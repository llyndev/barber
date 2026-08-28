# TODO — Auditoria Técnica do BarberCuttz API

> Documento de diagnóstico gerado a partir da leitura completa do código (167 arquivos Java, configs, Docker, migrations).
> **Nenhuma linha de código foi alterada.** Isto é um mapa de correção, não um patch.
>
> Formato de cada item: **Onde** → **O que está errado** → **Por que é problema** → **Alternativas** → **Recomendação**.

---

## Veredito em 60 segundos

O projeto tem uma base boa: modularização por domínio (`modules/*`), separação controller/service/repository/mapper/dto, DTOs como `record`, exceções tipadas com `@ControllerAdvice`, multi-tenant por slug. Isso já é mais maduro que a média.

O problema não é a arquitetura macro — é que **o isolamento multi-tenant e a autorização não são garantidos por nenhum mecanismo único**. Eles são reimplementados à mão, de três formas diferentes, em cada método de cada service. Onde alguém esqueceu, existe um vazamento entre barbearias. E existem vários pontos onde alguém esqueceu.

Os três erros estruturais que geram a maior parte do resto:

1. **Autorização espalhada em 3 camadas concorrentes** (`ContextFilter`, `businessService.validate*BySlug()` no controller, `checkOwnerManagerPermission()` no service) — sem uma ser a fonte da verdade.
2. **Ausência de schema versionado** (Flyway vazio + `ddl-auto=update`) — o banco de produção hoje é definido por "o que o Hibernate achou de fazer".
3. **Zero testes** — o que torna qualquer refatoração das duas anteriores um ato de fé.

Contagem: **9 itens P0** (segurança / vazamento de dados), **11 P1** (bugs de corretude), **12 P2** (arquitetura), **10 P3** (infra/qualidade).

---

# P0 — Crítico (vazamento de dados / segurança)

Estes permitem que um usuário leia ou altere dados de **outra barbearia**, ou expõem credenciais. Trate como incidente, não como backlog.

---

### P0.1 — Segredos de produção reais no `application.properties`

**Onde:** `src/main/resources/application.properties`

```
spring.mail.password=xsmtpsib-386590efb30b03...   ← senha SMTP Brevo real
google.auth.client-secret=GOCSPX-5E44Um4I2_...    ← client secret OAuth real
jwt.secret=${JWT_SECRET:ZlK9s7D3pQx4...}          ← fallback que funciona em prod
```

**O que está errado:** o arquivo está no `.gitignore` (bom, verifiquei — nunca foi commitado), mas continua em disco com credenciais reais em texto puro. E o `Dockerfile` faz `COPY src ./src` **sem `.dockerignore`** — ou seja, esses segredos são assados dentro da imagem Docker. Qualquer um com acesso à imagem tem sua conta Brevo e seu OAuth client.

**Por que é problema:** o `jwt.secret` com fallback é o pior dos três. Se a env var `JWT_SECRET` não for setada em produção (fácil de acontecer — o `docker-compose.yml` depende de um `.env` que pode não existir), a aplicação **sobe normalmente** usando a chave que está aqui em texto. Qualquer pessoa que leia este arquivo forja um JWT de `PLATFORM_ADMIN`. Falha silenciosa é pior que falha ruidosa.

**Onde você errou:** você tentou resolver isso com `${VAR:default}` para "facilitar o dev local". A intenção é certa, o mecanismo é errado: default de conveniência e segredo de produção não podem morar no mesmo lugar.

**Alternativas:**

| | Abordagem | Prós | Contras |
|---|---|---|---|
| **A** | `application.properties` só com placeholders **sem default** + `application-dev.properties` commitado com valores fake + `.env` local | Simples, zero dependência nova. App **quebra no boot** se faltar segredo em prod | Precisa disciplina de perfis |
| **B** | Vault / AWS Secrets Manager / Doppler | Rotação, auditoria | Overkill para o estágio atual |
| **C** | Variáveis de ambiente puras + `spring-boot-docker-compose` | Idiomático | Ainda precisa de A para o fail-fast |

**Recomendação: A.** Concretamente:
- `jwt.secret=${JWT_SECRET}` — **sem default**. Se faltar, o app não sobe. É exatamente o que você quer.
- Criar `application.properties.example` commitado, com valores fake, para onboarding.
- Criar `.dockerignore` com `src/main/resources/application.properties`.
- **Rotacionar as três credenciais agora** — elas estiveram em disco em uma máquina de dev e numa imagem Docker; considere-as comprometidas.

---

### P0.2 — Estoque, preço de custo e histórico de movimentação são públicos

**Onde:** `SecurityConfig.java:60` + `InventoryService.java:40` e `:97`

```java
.requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").permitAll()   // linha 60
...
.requestMatchers("/api/v1/inventory/**").authenticated()               // linha 77 — nunca alcançada p/ GET
```

**O que está errado:** as regras do Spring Security são avaliadas **em ordem, primeira que casar vence**. A linha 60 libera todo GET de inventário; a linha 77 é código morto para GET. E `listProducts()` / `listMovements()` não fazem nenhuma verificação de membership — recebem o `slug` e devolvem tudo.

**Por que é problema:** `ProductResponse` inclui **`costPrice`**. Ou seja:

```
GET /api/v1/inventory/barbearia-do-joao          → estoque + preço de custo do concorrente
GET /api/v1/inventory/barbearia-do-joao/history  → quem movimentou o quê, quando
```

Sem token. Margem de lucro de todos os seus clientes é pública. Num SaaS B2B isso é cláusula de rescisão de contrato.

**Onde você errou:** duas coisas se combinaram. (1) Você provavelmente liberou o GET durante o desenvolvimento do frontend e não reverteu. (2) O DTO de resposta é único para todos os consumidores — não existe distinção entre "o que o dono vê" e "o que um visitante vê".

**Alternativas:**
- **A —** Remover a linha 60 e adicionar `validateOwnerOrManagerBySlug()` em `listProducts`/`listMovements`. Rápido.
- **B —** Além de A, separar `ProductResponse` (interno, com `costPrice`) de `ProductPublicResponse` (catálogo, sem custo), caso exista mesmo um caso de uso público.

**Recomendação: A agora, B se houver vitrine pública de produtos.** Regra geral que resolve a classe inteira: **nunca coloque `costPrice`, `commissionPercentage` ou e-mail em um DTO que qualquer endpoint público possa retornar.** O DTO deve ser seguro por construção, não seguro por qual rota o usa.

---

### P0.3 — Módulo de Orders sem nenhuma verificação de tenant (IDOR total)

**Onde:** `OrderService.java` — `getOrder:252`, `addItem:125`, `removeItem:168`, `checkout:187`, `getOrderByBusiness:316`

```java
public OrderResponse getOrder(Long orderId) {
     Order order = orderRepository.findById(orderId)   // ← sem businessId
            .orElseThrow(...);
     return toResponse(order);
}
```

**O que está errado:** todo o módulo de pedidos ignora o tenant. `Order` até tem `businessId`, mas **nenhuma query o utiliza**. Compare com `SchedulingRepository`, que faz certo com `findByIdAndBusinessId`.

**Por que é problema:** qualquer usuário autenticado (inclusive um cliente comum, que só precisa se cadastrar) consegue:

```
GET  /orders/1..N                      → faturamento item a item de qualquer barbearia
GET  /orders/business/{qualquer-slug}  → histórico financeiro completo (nem valida membership)
POST /orders/{id}/checkout             → fecha o caixa de outra barbearia, dá baixa no estoque dela
POST /orders/{id}/items                → injeta itens no pedido de outro
```

O `checkout` é o pior: é **escrita destrutiva cross-tenant**. Ele muda status de agendamento, dá baixa em estoque e grava `SchedulingAdditionalValue` — tudo em outra barbearia.

**Onde você errou:** este módulo parece ter sido escrito depois dos outros e não herdou o padrão. Sintoma disso: `Order.businessId` é um `Long` solto em vez de `@ManyToOne Business` como no resto do domínio. Quando o tenant é só um número, é fácil esquecer de filtrar por ele; quando é uma relação, o esquecimento fica visível.

**Alternativas:**
- **A —** Adicionar `findByIdAndBusinessId` em `OrderRepository` e checagem de role em todos os 5 métodos. Consistente com o resto do projeto, mas é mais um lugar para esquecer no futuro.
- **B —** Hibernate `@Filter` / `@TenantId` (Hibernate 6 suporta `@TenantId` nativamente) — o filtro de tenant vira automático no nível do ORM.
- **C —** Row-Level Security no PostgreSQL — o banco recusa, mesmo se a aplicação errar.

**Recomendação: A imediatamente** (é o fogo), **B como direção** (ver P2.1). B é o que impede o próximo módulo de repetir isto.

---

### P0.4 — `ContextFilter` deixa passar quem não é membro (falta um `return`)

**Onde:** `tenant/ContextFilter.java:57-62`

```java
if (membership == null) {
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is not a member of this business");
}   // ← faltou return

BusinessContext.setBusinessId(business.getId().toString());
BusinessContext.setBusinessRole(membership.getRole().name());   // ← NPE garantido
```

**O que está errado:** `sendError()` **não interrompe a execução** — só marca a resposta. O código continua e chama `membership.getRole()` em `null`.

**Por que é problema:** duas consequências, e a segunda é a grave.
1. `NullPointerException` → o `@ExceptionHandler(Exception.class)` transforma em **500**, quando deveria ser 403. Você perde a semântica.
2. Pior: o `BusinessContext.setBusinessId()` da linha 61 **nunca executa** para não-membros — mas também nunca executa para o caminho de erro corretamente, e o filtro cai no `finally` que limpa o contexto. O resultado é que a checagem de membership que você escreveu em `ac556b1` ("verifica se o usuario pertence a business") **não protege nada** — ela explode antes de proteger. Todo endpoint que depende só do `ContextFilter` está confiando em uma checagem que sempre lança NPE em vez de negar.

**Onde você errou:** `sendError` parece terminal (o nome sugere isso) mas não é. É uma armadilha clássica da Servlet API. Foi um commit recente e bem-intencionado que introduziu isso.

**Alternativas:**
- **A —** Adicionar `return;` depois do `sendError`. Corrige em 1 linha.
- **B —** Não escrever a resposta no filtro; lançar `AccessDeniedException` e deixar o `AuthenticationEntryPoint`/`AccessDeniedHandler` do Spring Security formatar. Resposta de erro consistente com o resto da API (hoje o `sendError` devolve HTML do Tomcat, não o seu JSON `{"error": ...}`).

**Recomendação: B.** Hoje um 403 desse filtro devolve página HTML do Tomcat e um 403 do `GlobalExceptionHandler` devolve JSON. O frontend precisa tratar dois formatos. Unifique.

---

### P0.5 — `startAppointment` não valida a barbearia do agendamento

**Onde:** `SchedulingService.java:528`

```java
public void startAppointment(Long schedulingId) {
    Scheduling scheduling = schedulingRepository.findById(schedulingId)   // ← sem businessId
            .orElseThrow(...);
    scheduling.setStates(AppointmentStatus.IN_PROGRESS);
    ...
    orderService.createOrder(new CreateOrderRequest(scheduling.getId(), null, null, null));
}
```

**O que está errado:** o controller valida que você é OWNER/MANAGER/BARBER **da barbearia do header** (`validateOwnerOrManagerOrBarberBySlug`), mas o service busca o agendamento **por ID puro**. Não há ligação entre as duas coisas.

**Por que é problema:** sou barbeiro legítimo da minha barbearia. Mando meu slug no header (passo na validação) e o ID de um agendamento da barbearia vizinha. Resultado: inicio o atendimento dela e crio um pedido no caixa dela. Todos os outros métodos da classe usam `findByIdAndBusinessId` — este é a exceção. Foi esquecimento, não decisão.

**Recomendação:** usar `findByIdAndBusinessId(schedulingId, getBusinessIdFromContext())` como os vizinhos. E ver P2.1 — o padrão certo é que seja impossível escrever `findById` sem tenant.

---

### P0.6 — Endpoint aberto de envio de e-mail (relay de spam)

**Onde:** `modules/email/controller/MailTextController.java`

```java
@GetMapping    // GET /text-mail?to=qualquer@email.com
public String test(@RequestParam String to) { ... sender.send(msg); }
```

**O que está errado:** endpoint de teste esquecido em produção. Cai no `anyRequest().authenticated()`, então exige login — mas qualquer pessoa faz `POST /register` e vira usuário autenticado em 2 segundos.

**Por que é problema:** é um **open relay** usando sua conta Brevo. Um script simples queima sua cota, e depois disso seu domínio `barbercuttz.me` entra em blacklist de spam. Aí seus e-mails de recuperação de senha param de chegar. O dano não é o e-mail enviado — é a reputação do domínio, que leva semanas para recuperar.

**Recomendação:** **deletar o arquivo.** Se precisar de smoke test de e-mail, faça um teste de integração ou um endpoint sob `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` num profile `dev`. Vale um grep geral por outros endpoints de teste esquecidos.

---

### P0.7 — Enumeração de usuários no "esqueci minha senha"

**Onde:** `PasswordResetTokenService.java:38-39` vs `AuthController.java:48`

```java
// Service:
if (!userRepository.existsByEmail(email)) {
    throw new ResourceNotFoundException("The email address does not exist.");   // → 404
}

// Controller (nunca alcançado quando não existe):
return ResponseEntity.ok().body("If the email address exists, we will send a recovery link.");
```

**O que está errado:** você escreveu a mensagem genérica correta no controller — e depois adicionou no service uma exceção que a torna inútil. As duas se contradizem, e o service ganha.

**Por que é problema:** `404` = e-mail não cadastrado, `200` = cadastrado. Um atacante enumera sua base inteira de clientes com uma wordlist. Depois usa essa lista para credential stuffing ou phishing direcionado ("olá, sua barbearia X..."). O endpoint é `permitAll` e não tem rate limit, então dá para varrer rápido.

**Onde você errou:** o commit `c0f203d` ("add email existence check for password reset") foi provavelmente motivado por um pedido do frontend ("preciso saber se o e-mail existe para mostrar erro"). É uma requisição de UX que **deve ser recusada** — a resposta correta para o frontend é sempre mostrar "se existir, enviamos".

**Recomendação:** remover o `existsByEmail`. Sempre `200` com mensagem genérica. Adicionalmente: rate limit por IP e por e-mail (ver P2.9), e guardar **hash** do token no banco em vez do token em claro (hoje um dump/SQL injection do banco entrega resets prontos).

---

### P0.8 — Upload permite XSS armazenado (extensão não validada)

**Onde:** `FileStorageService.java:35-49` + `SecurityConfig.java:53/66` + `WebConfig`

```java
String contentType = file.getContentType();               // ← vem do cliente, forjável
if (!ALLOWED_CONTENT_TYPES.contains(contentType)) throw ...;

fileExtension = originalFileNames.substring(originalFileNames.lastIndexOf("."));  // ← qualquer coisa
String uniqueFileName = UUID.randomUUID() + fileExtension;
```

**O que está errado:** o `Content-Type` é um header enviado pelo cliente — trivial de forjar com `curl`. A extensão vem do nome original sem allowlist. Os arquivos são servidos em `/uploads/**` com `permitAll`, **na mesma origem da API**.

**Por que é problema:** `POST` de um arquivo chamado `payload.html` com `Content-Type: image/png` passa na validação e é salvo como `<uuid>.html`. O Spring serve com `text/html`. Qualquer vítima que abrir esse link executa JS **na origem da sua API** — e como o CORS está com `allowCredentials(true)`, isso lê o token da vítima. O mesmo vale para `.svg` (SVG executa script).

**Onde você errou:** você validou o rótulo (`Content-Type`) em vez do conteúdo, e não validou a extensão de jeito nenhum. Regra: **valide o que você controla (extensão que você escolhe), não o que o cliente afirma.**

**Alternativas:**
- **A —** Ignorar a extensão original: derive-a do tipo detectado (`jpeg→.jpg`, `png→.png`, `webp→.webp`). Valide os *magic bytes* do arquivo (Apache Tika ou `ImageIO.read()` != null).
- **B —** Servir uploads de um domínio/bucket separado (S3/R2/Cloudinary), com `Content-Disposition` e `Content-Type` fixos. Resolve XSS **e** o P3.3 (uploads em disco local).

**Recomendação: A agora, B junto com a migração de storage.** Adicione também `X-Content-Type-Options: nosniff`.

---

### P0.9 — Dados pessoais de barbeiros expostos publicamente

**Onde:** `SecurityConfig.java:58` + `UserResponse`

```java
.requestMatchers(HttpMethod.GET, "/users/barbers").permitAll()
```

`UserResponse` devolve `email`, `telephone`, `plantType`, `platformRole`, `userBusinesses`.

**Por que é problema:** o frontend só precisa de **nome + foto + id** para montar a tela "escolha seu barbeiro". Você está publicando e-mail e telefone pessoal de todos os funcionários de todas as barbearias, sem autenticação. Isso é exposição de dado pessoal sob LGPD, com titular identificável e finalidade não declarada.

**Recomendação:** criar `BarberPublicResponse(id, name, profileImage)` para a rota pública. Já existe um `BarberResponse` em `modules/business/dto/response/` — verifique se ele serve. Mesmo princípio de P0.2: **o DTO define a fronteira, não a rota.**

---

# P1 — Bugs de corretude

Não são brechas de segurança, mas produzem comportamento errado ou perda de dados.

---

### P1.1 — Flyway está vazio e `ddl-auto=update` está ligado

**Onde:** `application.properties` + `src/main/resources/db/migration/` (diretório sem arquivos)

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=false   # ← desliga a única proteção do Flyway
spring.jpa.hibernate.ddl-auto=update      # ← Hibernate manda no schema
```

**O que aconteceu:** o histórico do git mostra que existiram 8 migrations, com **versões duplicadas**:

```
V2__add_business_id_to_barber_service.sql   ⟷  V2__Update_Scheduling_Status_Check.sql
V3__add_business_id_to_opening_hours.sql    ⟷  V3__Add_Commission_To_UserBusiness.sql
V4__add_business_id_to_scheduling.sql       ⟷  V4__Create_Scheduling_AdditionalValue.sql
```

Flyway aborta com `Found more than one migration with version 2`. A saída adotada foi apagar tudo: hoje o diretório está vazio e as últimas 3 estão deletadas no working tree.

**Por que é problema:** com Flyway vazio e `ddl-auto=update`, o schema de produção é **o que o Hibernate deduziu ao subir**. Consequências concretas, não teóricas:
- `update` **nunca remove nem altera** coluna. Toda coluna renomeada continua lá, com dados velhos.
- Não existe rollback, nem histórico, nem revisão em PR.
- `validate-on-migrate=false` significa que, quando você recolocar migrations, o Flyway aceitará qualquer divergência em silêncio.
- Dois ambientes que subiram em commits diferentes têm schemas **diferentes**, e você não tem como saber.
- Constraints que só existem em SQL (o `CHECK` de status da `V2`) simplesmente não existem mais.

**Onde você errou:** o erro raiz foi rodar Flyway **e** `ddl-auto=update` juntos. São duas fontes de verdade para a mesma coisa; quando divergem, você apaga a que reclama. O `baseline-on-migrate=true` + `validate-on-migrate=false` são o sintoma de estar lutando contra a ferramenta em vez de usá-la.

**Alternativas:**

| | Abordagem | Quando faz sentido |
|---|---|---|
| **A** | Flyway como única fonte: gerar `V1__baseline.sql` via `pg_dump --schema-only` do banco atual, `ddl-auto=none`(ou `validate`), `validate-on-migrate=true` | **Recomendado.** É o caminho de qualquer app que vai para produção |
| **B** | Só `ddl-auto=update`, sem Flyway | Aceitável em protótipo. Não é o caso aqui — já tem dados de clientes |
| **C** | Liquibase | Equivalente; sem motivo para trocar |

**Recomendação: A.** Passo a passo:
1. `pg_dump --schema-only` do banco de produção → `V1__baseline.sql`.
2. `spring.flyway.baseline-version=1`, `baseline-on-migrate=true` (só nesta vez), `validate-on-migrate=true`.
3. `spring.jpa.hibernate.ddl-auto=validate` — o app passa a **recusar subir** se entidade e schema divergirem. É exatamente o alarme que falta hoje.
4. Convenção de nomes daqui em diante: `V{n}__{snake_case}.sql`, `n` sequencial, um por PR. A duplicação de V2/V3/V4 veio de dois branches paralelos — use timestamp (`V20260823_1400__...`) se trabalhar em paralelo.

---

### P1.2 — Falha no Google Calendar aborta a criação do agendamento

**Onde:** `SchedulingService.java:197`

```java
Scheduling saved = schedulingRepository.save(sched);
googleCalenderService.syncSchedulingCreated(saved);   // ← sem try/catch
return saved;
```

Compare com `cancelClient`, `endService`, `addService` — todos envolvem a chamada em `try/catch` e apenas logam. Só a **criação**, o fluxo mais importante, ficou desprotegida.

**Por que é problema:** `syncSchedulingCreated` é `@Async`, mas está sendo chamado **de dentro da mesma classe? Não** — é outro bean, então o proxy funciona e a chamada é assíncrona. O problema é outro e mais sutil: o método é `@Async` **e** `@Transactional`, e recebe a entidade `Scheduling` gerenciada. Ela atravessa a fronteira de thread e chega **detached** na nova transação. Qualquer acesso a `scheduling.getBarberService()` (lazy) em `buildEventPayload` pode lançar `LazyInitializationException`, e o `schedulingRepository.save(scheduling)` em outra thread pode sobrescrever alterações concorrentes.

Além disso, se o `@Async` fosse desligado ou o pool estivesse cheio, uma indisponibilidade do Google derrubaria a criação de agendamentos da plataforma inteira.

**Recomendação:**
- Nunca passe entidade JPA para método `@Async` — passe o **ID** e recarregue dentro do método.
- Envolva em `try/catch` como os irmãos, ou melhor: dispare via `@TransactionalEventListener(phase = AFTER_COMMIT)`. Assim o sync só roda depois do commit e nunca pode afetar a transação de negócio.
- Configure um `TaskExecutor` explícito — o default do `@EnableAsync` é `SimpleAsyncTaskExecutor`, que **cria uma thread nova por chamada, sem limite**. Sob carga isso derruba a JVM.

---

### P1.3 — Barbeiro não consegue finalizar atendimento (validação em AND)

**Onde:** `SchedulingController.java:162-168`

```java
if (businessSlug != null && !businessSlug.isBlank()) {
    businessService.validateBarberBySlug(businessSlug, ...);          // exige ser BARBER
}
if (businessSlug != null && !businessSlug.isBlank()) {
    businessService.validateOwnerOrManagerBySlug(businessSlug, ...);  // exige ser OWNER/MANAGER
}
```

**O que está errado:** ambas lançam exceção se falharem, então o efeito é **AND**: só passa quem é BARBER **e** OWNER/MANAGER simultaneamente. A intenção óbvia era OR.

**Por que é problema:** o endpoint `PUT /scheduling/barber/completed/{id}` — finalizar atendimento, o fluxo mais usado do dia a dia — está **quebrado para todo mundo**, exceto na combinação rara de alguém que tenha dois vínculos. Note que o `endService` do service já faz a checagem OR corretamente (`isManagerOrOwner || isAssignedBarber`). Ou seja: a lógica certa existe uma camada abaixo, e o controller a invalida.

**Onde você errou:** isso é sintoma direto do problema estrutural — a autorização está duplicada em duas camadas, e as duas discordam. Ver P2.1.

**Recomendação:** remover os dois blocos do controller. O `endService` já valida corretamente. **Uma checagem, num lugar só.**

---

### P1.4 — `endService` não é transacional

**Onde:** `SchedulingService.java:258`

```java
public Scheduling endService(...) {   // ← sem @Transactional
    ...
    inventoryService.registerMovement(...);   // este SIM é @Transactional → commita sozinho
    ...
    scheduling.setStates(COMPLETED);
    schedulingRepository.save(scheduling);
}
```

**Por que é problema:** o método faz N baixas de estoque (cada uma commitando em sua própria transação) e depois grava o agendamento. Se falhar no meio — produto inexistente na 3ª iteração, estoque insuficiente, queda de conexão — as duas primeiras baixas **já foram commitadas** e o agendamento não foi finalizado. Estoque some sem contrapartida, e não há como saber quantos itens foram descontados. É perda de dado silenciosa, do tipo que só aparece na conferência de inventário no fim do mês.

**Recomendação:** `@Transactional` no método. Reveja a classe inteira — `getByBarberId`, `getByDateTime`, `startAppointment` também escrevem/leem sem anotação consistente. Considere `@Transactional(readOnly = true)` na classe e `@Transactional` nos métodos de escrita.

---

### P1.5 — Duplo caminho para finalizar atendimento → estoque baixado duas vezes

**Onde:** `SchedulingService.endService` e `OrderService.checkout`

Ambos fazem, independentemente: setar status `COMPLETED`, gravar `paymentMethod`, dar baixa em estoque com `StockMovementType.EXIT`.

**Por que é problema:** são dois fluxos concorrentes para a mesma regra de negócio, sem guarda entre eles. Se o operador usa `startAppointment` (que cria um Order) e depois `checkout`, e **também** chama `completed/{id}` com `productsUsed`, o estoque é debitado duas vezes. Nada impede isso — `checkout` não verifica se o agendamento já está `COMPLETED`, e `endService` não verifica se existe Order pago.

**Onde você errou:** o módulo `orders` foi adicionado depois como um "caixa/comanda", mas o fluxo antigo (`endService`) não foi aposentado. Ficaram os dois vivos.

**Recomendação:** decida qual é o fluxo canônico e **remova o outro**. Pela modelagem, `Order` é o modelo mais completo (itens, produtos, adicionais por barbeiro). Sugestão: `endService` passa a apenas fechar o agendamento; toda baixa de estoque e todo dinheiro passam pelo `Order`. Documente essa decisão — é o tipo de coisa que volta a divergir em 3 meses.

---

### P1.6 — `Scheduling` tem dois campos de valor adicional

**Onde:** `Scheduling.java`

```java
private BigDecimal additionalValue;                        // usado por endService
private List<SchedulingAdditionalValue> additionalValues;  // usado por checkout
```

Um é valor único, o outro é lista por barbeiro. `OrderService.createOrder` lê o singular; `OrderService.checkout` grava o plural. `SchedulingMapper` devolve os dois. O consumidor não tem como saber qual é a verdade.

**Recomendação:** manter apenas `additionalValues` (a lista — modela comissão por barbeiro, que é o caso real de uma barbearia). Migrar os dados do singular e removê-lo. Consequência do mesmo problema de P1.5.

---

### P1.7 — `IllegalAccessError` em vez de exceção

**Onde:** `BusinessService.java:179`, `:196`, `:213`

```java
throw new IllegalAccessError("Acesso negado");
```

**O que está errado:** `IllegalAccessError` é um `java.lang.Error`, não `Exception`. Ele sinaliza corrupção de bytecode da JVM, não regra de negócio. Você quis `IllegalAccessException` (checked, também errada aqui) — provavelmente um autocomplete aceito rápido demais.

**Por que é problema:** `@ExceptionHandler(Exception.class)` **não captura `Error`**. A exceção escapa do Spring inteiro, o Tomcat devolve uma página HTML de erro 500, e o frontend recebe algo que não sabe parsear. Além disso, um `Error` propagando pode deixar o `finally` do `ContextFilter` em estado imprevisível.

**Recomendação:** trocar pela mesma exceção usada nos outros ramos do mesmo método (`SecurityException`, que já tem handler → 403). Melhor ainda: criar `ForbiddenException` própria e **parar de usar `SecurityException` do JDK**, que é semanticamente sobre SecurityManager.

---

### P1.8 — Construtor de `PasswordResetToken` com corpo vazio

**Onde:** `PasswordResetToken.java`

```java
public PasswordResetToken(String token, AppUser user) {
}   // ← não atribui nada
```

Recebe dois parâmetros e ignora ambos. Hoje não é usado (o service usa setters), então é uma bomba não armada: o primeiro `new PasswordResetToken(token, user)` cria um objeto com tudo `null` e `expirationDate` nulo — e `resetPassword` faz `prt.getExpirationDate().isBefore(...)` → NPE. **Deletar o construtor.**

---

### P1.9 — Corrida na criação de agendamento (double booking)

**Onde:** `SchedulingService.ensureAvailableOrThrow` + `Scheduling` (sem constraint)

A verificação de conflito é um `SELECT` seguido de um `INSERT`, sem lock e sem constraint no banco. Duas requisições simultâneas para o mesmo horário passam ambas na validação e ambas inserem.

**Por que é problema:** não é hipotético — é o caso mais comum de bug em sistema de agendamento. Dois clientes clicando no mesmo slot no mesmo segundo (o que acontece em horário de pico) resulta em overbooking, e quem descobre é o barbeiro com dois clientes na porta.

**Alternativas:**
- **A —** `SELECT ... FOR UPDATE` na agenda do barbeiro no dia (`@Lock(PESSIMISTIC_WRITE)`). Simples, correto, serializa por barbeiro/dia.
- **B —** Constraint de exclusão no PostgreSQL com `tstzrange` + `EXCLUDE USING gist` — o banco garante. Elegante e à prova de qualquer bug de aplicação.
- **C —** `@Version` (lock otimista) — não resolve, porque as duas linhas são novas, não conflitantes.

**Recomendação: B**, com A como fallback se o range type complicar. Independente disso, adicione índice em `scheduling(business_id, barber_id, scheduling_date_time)` — hoje não existe nenhum, e `findByBarber_IdAndDateTimeBetween` roda em toda consulta de disponibilidade.

---

### P1.10 — Fusos horários ignorados

**Onde:** todo o domínio de agendamento usa `LocalDateTime` / `LocalTime` / `LocalDate.now()`

`Business.timezone` **existe como campo** e só é lido em um lugar: `GoogleCalenderService.resolveTimezone`. Todo o resto — validação de horário de funcionamento, `getAvailableSlots`, `getBusinessStatus`, comparações `isAfter(LocalTime.now())` — usa o fuso **do servidor**.

**Por que é problema:** você é um SaaS multi-tenant. No momento em que uma barbearia do Acre (UTC−5) usar a plataforma hospedada em UTC, a agenda dela abre e fecha 3 horas fora. E `getBusinessStatus()` dirá "aberto" quando está fechada. Note que o Google Calendar já recebe o timezone certo — então a mesma reserva aparece em um horário no seu app e em outro no calendário do barbeiro.

**Recomendação:** guardar instantes em `Instant`/`timestamptz` e converter para o `ZoneId` do business na borda (controller/mapper). Regra: **`LocalDateTime` só existe em DTO; o banco guarda `Instant`.** Corrigir isso depois de ter dados em produção é caro — faça antes de escalar.

---

### P1.11 — `Dockerfile` referencia um jar que não existe

**Onde:** `Dockerfile`

```dockerfile
COPY --from=build /app/target/app.jar app.jar
```

O `pom.xml` não define `<finalName>`, então o artefato gerado é `barbearia-0.0.1-SNAPSHOT.jar`. **O build da imagem falha.** Some isso ao `SPRING_PROFILES_ACTIVE=prod` sem nenhum `application-prod.properties` no projeto, e o deploy containerizado nunca foi exercitado com sucesso.

**Recomendação:** `<finalName>app</finalName>` no pom, ou `COPY --from=build /app/target/*.jar app.jar`. Criar `application-prod.properties` com `ddl-auto=validate`, `show-sql=false` e logging em `INFO` (hoje o base tem `DEBUG` no Spring Security e `TRACE` no binder do Hibernate — em produção isso **loga valores de parâmetros de query**, incluindo hashes de senha e tokens, além de ser lentíssimo).

---

# P2 — Arquitetura e consistência

Não quebram nada hoje. São o que faz cada feature nova custar mais que a anterior.

---

### P2.1 — Autorização em três camadas concorrentes (o problema central)

Hoje coexistem três mecanismos:

| Mecanismo | Onde vive | Como identifica o tenant |
|---|---|---|
| `ContextFilter` + `BusinessContext` (ThreadLocal) | filtro HTTP | header `X-Business-Slug` |
| `businessService.validate*BySlug(slug, userId)` | chamado no **controller** | path variable ou header |
| `checkOwnerManagerPermission()` | privado, duplicado em **4 services** | lê a ThreadLocal |

Consequências observáveis, todas já catalogadas acima: P0.3 (orders não usa nenhum), P0.5 (controller valida um business, service busca outro), P1.3 (duas camadas discordam e viram AND), e o método `getBusinessIdFromContext()` copiado literalmente em `SchedulingService`, `InvitationService`, `UserBusinessService`, `BarberServiceService`, `OrderService`, `OpeningHoursService`.

**Onde você errou:** cada mecanismo foi adicionado quando o anterior não cobria um caso, sem remover o anterior. É acúmulo, não desenho. O sintoma diagnóstico é o `getBusinessIdFromContext()` duplicado 6 vezes — quando você copia a mesma função para o sexto arquivo, ela deveria ter virado infraestrutura no segundo.

**Alternativas:**

| | Abordagem | Prós | Contras |
|---|---|---|---|
| **A** | Manter ThreadLocal, mas: um `TenantService` injetável (não estático), regras via `@PreAuthorize("@tenantGuard.isOwnerOrManager()")` | Incremental, cabe em sprints | ThreadLocal continua invisível na assinatura |
| **B** | Tenant explícito: `BusinessContext` vira parâmetro tipado passado do controller ao service | Explícito, testável sem mock de ThreadLocal | Refatoração ampla |
| **C** | Hibernate `@TenantId` (nativo no Hibernate 6) + `CurrentTenantIdentifierResolver` | O ORM filtra sozinho; **impossível** esquecer | Casos legítimos cross-tenant (admin) exigem escape hatch |

**Recomendação: A + C.** C elimina a classe inteira de bugs P0.3/P0.5 na raiz — nenhum `findById` volta linha de outro tenant, nem por acidente. A resolve a autorização por papel de forma declarativa e testável. Faça C primeiro (é o que para o sangramento) e A depois.

Ordem sugerida: escrever testes de isolamento **antes** (P3.1) → aplicar `@TenantId` → remover as checagens manuais uma a uma, com os testes verdes.

---

### P2.2 — Duas modelagens para a mesma coisa: `UserBusiness` vs `Membership`

`Membership` (`business/model/Membership.java`) tem os mesmos campos que `UserBusiness`, com um enum `Role` **diferente** (`OWNER, ADMIN, BARBER, CLIENT` vs `OWNER, MANAGER, BARBER, VIEWER`). Tem `MembershipRepository`. **Nada no código usa.** É uma tabela criada pelo `ddl-auto=update` que nunca recebeu uma linha.

**Recomendação:** deletar `Membership`, `MembershipRepository` e a tabela. Enquanto existirem duas modelagens, alguém eventualmente usa a errada.

---

### P2.3 — Rotas duplicadas fazendo a mesma coisa

`SchedulingController` tem três endpoints idênticos:

```java
GET /scheduling                  → validateOwnerOrManagerBySlug(header) → findAllByBusinessId
GET /scheduling/business         → idem, mesmo corpo, com comentários
GET /scheduling/business/{slug}  → idem, slug via path
```

Idem em `BarberServiceService`: `createService(BarberService)` e `save(BarberServiceRequest, UserDetails)` fazem a mesma coisa (a primeira recebendo **entidade** direto, o que é um risco de mass assignment). `OrderService` tem um `removeItem(Long)` package-private com corpo vazio.

**Recomendação:** manter uma variante de cada, deletar as outras. Escolha uma convenção — sugiro **tenant sempre via header `X-Business-Slug`**, nunca via path — e aplique em toda a API. Hoje `/scheduling/business/{slug}` e o header competem, e alguns endpoints aceitam os dois (com `required = false`, o que significa que a validação é **opcional** — ver P0.5).

---

### P2.4 — Filtragem e busca feitas em memória

```java
// BusinessService.getAll / searchBusinesses
businessRepository.findAll().stream().filter(b -> ...).toList();

// InvitationService:89
invitationRepository.findAll().stream().filter(...).count();
```

Carrega a tabela inteira do banco para filtrar em Java. Com 50 barbearias funciona; com 5.000 a listagem pública derruba a aplicação — e ela é `permitAll`, então é um vetor de DoS trivial.

**Recomendação:** mover para query (`@Query` com `ILIKE`, ou Specification). Adicionar **paginação** — existe até um `PageResponse` DTO em `business/dto/response/` que aparentemente nunca foi usado. `GET /business`, `GET /scheduling`, `GET /orders/business/{slug}` e as listagens de inventário devem todas ser paginadas.

---

### P2.5 — N+1 em praticamente toda listagem

- `AppUser.userBusinesses` é `FetchType.EAGER` → **toda autenticação** carrega todos os vínculos do usuário, em toda requisição.
- `SchedulingMapper.toResponse` acessa `barberService` (lazy `@ManyToMany`), `productsUsed`, `additionalValues`, `user`, `barber`, `business` → uma listagem de 50 agendamentos dispara centenas de queries.
- `OrderService.toResponse` faz `userRepository.findById` **duas vezes por pedido** (cliente + profissional) e ainda um `schedulingRepository.findById`. `getOrderByBusiness` com 100 pedidos = ~300 queries.

**Recomendação:** `EAGER` → `LAZY` em `AppUser.userBusinesses`. Usar `@EntityGraph` ou `join fetch` nas queries de listagem. Em `OrderService`, desnormalizar (guardar o nome no `Order` no momento da criação — o que já é feito parcialmente com `clientName`) ou buscar os usuários em lote. Ligue `spring.jpa.properties.hibernate.generate_statistics=true` em dev para ver o tamanho real do problema.

---

### P2.6 — DTOs sem validação

11 DTOs de request não têm nenhuma anotação `jakarta.validation`, incluindo os mais sensíveis:

`AuthRequest`, `ProductRequest`, `MovementRequest`, `CreateOrderRequest`, `UserRequest`, `BarberServiceRequest`, `LeadRequest`, `CompleteRegistrationRequest`, `UpdateRoleRequest`, `PromoteToOwnerRequest`, `ReasonRequest`.

E vários controllers recebem `@RequestBody` **sem `@Valid`** (`InventoryController` inteiro, `RegisterController.completeRegistration`, `UserController.update`).

Consequências reais: preço negativo em produto, quantidade zero em movimentação, `reason` de 1MB, e a validação de senha só existe no `PasswordResetTokenRequest` (o `RegisterRequest` valida tamanho — os outros não).

**Recomendação:** `@NotNull/@NotBlank/@Positive/@Email/@Size` nos records + `@Valid` em todos os `@RequestBody`. Custo baixo, elimina uma faixa inteira de bugs. Bônus: o `GlobalExceptionHandler` já formata `MethodArgumentNotValidException` bonitinho — a infraestrutura está pronta, só falta usar.

---

### P2.7 — Dependências JWT em três versões incompatíveis

**Onde:** `pom.xml`

```xml
<artifactId>jjwt</artifactId>       <version>0.9.1</version>   <!-- API legada, pré-modular -->
<artifactId>jjwt-impl</artifactId>  <version>0.11.2</version>
<artifactId>jjwt-gson</artifactId>  <version>0.11.5</version>
```

`jjwt` 0.9.1 é o **artefato monolítico antigo**, que traz suas próprias classes `io.jsonwebtoken.*` conflitando com as de `jjwt-api` que o `jjwt-impl` 0.11.x espera. Que funcione hoje é acidente de ordem de classpath. Além disso, 0.9.1 tem CVEs conhecidos e depende de Jackson por reflexão.

**Recomendação:** trocar os três por `jjwt-api` + `jjwt-impl` + `jjwt-jackson`, todos na **mesma versão** (0.12.x). A API mudou (`Jwts.parserBuilder()` → `Jwts.parser()`, `setSubject` → `subject`), então `JwtUtil` precisa de ajuste — é meia hora de trabalho num único arquivo.

Aproveite para revisar `JwtUtil`: sem claim `iss`/`aud`, sem `jti`, sem refresh token, expiração de 12h fixa e **nenhuma forma de revogar**. Se um token vazar, ele é válido por 12 horas e você não pode fazer nada. Ver P3.6.

---

### P2.8 — CORS permissivo demais

**Onde:** `CorsConfig.java`

```java
config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","HEAD","TRACE","CONNECT","PATCH"));
config.setAllowCredentials(true);
```

`TRACE` e `CONNECT` não têm razão de existir numa API REST (`TRACE` historicamente habilita Cross-Site Tracing). Origens hardcoded no código — deveriam vir de property por ambiente. E `http://barbercuttz.netlify.app` está em **HTTP puro** na lista.

**Recomendação:** métodos = os que você realmente usa. Origens via `${app.cors.allowed-origins}`. Remover a origem `http://`.

---

### P2.9 — Sem rate limiting em lugar nenhum

`POST /auth/login`, `POST /auth/forgot-password`, `POST /register`, `POST /leads` são todos `permitAll` e sem limite. Isso viabiliza brute force de senha, enumeração (P0.7), flood da caixa de e-mail de qualquer usuário e poluição da tabela de leads.

**Recomendação:** Bucket4j (in-memory para começar; Redis quando escalar) num filtro antes do `JwtFilter`. Comece com o mínimo: 5 tentativas de login / 15 min por IP+email, 3 forgot-password / hora por e-mail. Adicione lockout progressivo de conta — `AppUser` já tem o campo `blocked` disponível.

---

### P2.10 — Tokens OAuth do Google em texto puro + state em memória

`GoogleCalendarConnection` guarda `accessToken` e `refreshToken` sem criptografia. O **refresh token não expira** — quem lê a tabela tem acesso permanente ao Google Calendar de todos os seus barbeiros.

`GoogleCalendarOAuthStateService` guarda o state num `ConcurrentHashMap` estático em memória: quebra com mais de uma instância (o callback pode chegar noutro nó) e se perde a cada restart.

O `redirectUri` vem do cliente sem allowlist local — você depende inteiramente da configuração do Google Console para não virar vetor de roubo de código de autorização.

**Recomendação:** criptografar os tokens em repouso (`@Convert` com AES-GCM e chave em env, ou `pgcrypto`). Mover o state para Redis ou para a tabela com TTL. Validar `redirectUri` contra allowlist da aplicação.

---

### P2.11 — `AppUser` acumulando responsabilidades de billing

```java
private String plantType;                       // typo de "planType", e é String, não o enum PlanType
private boolean isBusinessCreator;
private LocalDate dateExpirationAccount;
```

Plano e vencimento são atributos de **assinatura**, não de usuário. Já causam código frágil: `PlanType.valueOf(creator.getPlantType())` dentro de `try/catch(Exception)` em `BusinessService.create` e `InvitationService` — ou seja, o próprio código admite que o campo pode conter lixo.

Além disso, o limite do plano é verificado na **criação do convite** mas não no **aceite** (`InvitationService.acceptInvitation`): convide 10 barbeiros com plano SOLO, todos aceitam depois, e você tem 10 barbeiros num plano de 1.

**Recomendação:** extrair `Subscription` (owner, plano como **enum**, início, fim, status). Validar limite no aceite, não só no convite. Corrigir `plantType` → `planType` (com migration).

---

### P2.12 — Inconsistências menores que somam

- **Idiomas misturados:** mensagens de erro em PT e EN no mesmo service (`"Agendamento não encontrado"` / `"Scheduling not found"`), comentários em ambos. Escolha um — sugiro **mensagens de API em PT-BR** (é o público) e **código/comentários em EN**.
- **`System.out.println`** em `FileStorageService:36` e `:73` em vez de logger. O projeto já usa `@Slf4j` em outros lugares.
- **`throw new RuntimeException`** genérico em `AuthService.getMe`, `SchedulingService.cancelClient`, `FileStorageController` (2x), `PasswordResetTokenService.resetPassword`. Todos caem no handler genérico → **500** para o que deveria ser 401/403/404.
- **`ResponseStatusException` vs exceções próprias:** `UserService` usa a primeira, o resto usa as segundas. Padronize nas suas.
- **`SchedulingMapper` é `@Component` com métodos `static`** — a anotação não faz nada. Ou vira bean injetável, ou vira classe utilitária `final` sem anotação.
- **Erros de digitação em mensagens visíveis:** `"Business ID not foun"`, `"Invitaiton expired"`, `"User alredy exists"`, `"hasAcess"`, `"Barber shops don't have owners."` (deveria ser "não tem dono").
- **`Business.amenities`** é `@Column private List<String>` sem `@ElementCollection` — funciona por acidente (Hibernate 6 mapeia para `text[]` no Postgres). Torne explícito antes que uma atualização de versão quebre.
- **Link hardcoded:** `LeadController:37` gera `http://localhost:3000/invite/token=...` — que vai para produção assim. (E falta um `?` antes de `token`.)

---

# P3 — Infra, qualidade e processo

---

### P3.1 — Zero testes

`BarbeariaApplicationTests.contextLoads()` é o único teste do repositório. Nenhum teste unitário, de integração ou de segurança.

**Por que isso é o item mais caro da lista:** todas as correções P0/P1 acima envolvem mexer em autorização e transações. Sem testes, cada correção tem chance real de introduzir uma regressão pior que o bug original — e você não vai saber até um cliente reclamar.

**Recomendação — nesta ordem:**
1. **Testes de isolamento multi-tenant primeiro.** Um `@SpringBootTest` com Testcontainers (Postgres real): crie duas barbearias, um usuário em cada, e verifique que o usuário A recebe 403/404 em **todo** endpoint da barbearia B. Este teste sozinho teria pego P0.3, P0.5 e P0.4. Escreva-o **antes** de corrigi-los.
2. Testes unitários de `SchedulingService.getAvailableSlots` / `ensureAvailableOrThrow` — é a lógica mais complexa do sistema (aritmética de slots, sobreposição, borda de fechamento) e a mais fácil de quebrar sem perceber.
3. `@WebMvcTest` para as regras do `SecurityConfig` — a ordem das regras é sutil (P0.2 é exatamente um bug de ordenação) e um teste por rota pública documenta a intenção.

Meta realista: 60% de cobertura em `service/` e `security/`. Não persiga 100%.

---

### P3.2 — Sem CI

Nenhum `.github/workflows`. Sem build automatizado, sem verificação de que o projeto compila num ambiente limpo — e, dado o P1.11, o build Docker está quebrado sem ninguém saber.

**Recomendação:** GitHub Actions com `mvn verify` + build da imagem Docker em todo PR. Adicione `dependency-check` ou Dependabot (P2.7 é exatamente o que um scanner de dependências pegaria sozinho).

---

### P3.3 — Uploads em disco local do container

`file.upload-dir=./uploads`, servido por `WebConfig` como recurso estático, com bind mount no compose.

Isso impede: rodar mais de uma instância (cada uma com seus arquivos), deploy em plataformas com filesystem efêmero (Railway, Fly, Heroku, Cloud Run — os arquivos somem no restart) e qualquer CDN na frente. Também é o que torna P0.8 explorável, já que os arquivos são servidos da mesma origem da API.

**Recomendação:** S3 / Cloudflare R2 / Cloudinary. Resolve durabilidade, escala horizontal, XSS de origem e ainda tira carga de I/O da aplicação.

---

### P3.4 — Observabilidade inexistente

Sem Actuator, sem health check, sem métricas, sem trace id nos logs. Em produção, quando um cliente disser "o agendamento não salvou", você não tem como descobrir qual requisição foi.

**Recomendação:** `spring-boot-starter-actuator` com `/health` e `/metrics` protegidos; `HEALTHCHECK` no Dockerfile e no compose; MDC com trace id + businessId + userId em todo log. Configurar logging estruturado (JSON) se for para um agregador.

---

### P3.5 — Logging perigoso em produção

```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
spring.jpa.show-sql=${SHOW_SQL:true}
```

`BasicBinder=TRACE` loga **os valores dos parâmetros** de toda query — inclui hash de senha, tokens de reset e tokens OAuth do Google. Em produção isso é (a) vazamento de dados sensíveis para o arquivo de log e (b) lentidão severa.

Some a isso `SchedulingService:131`, que faz `log.info("... Request: {}", request)` com o DTO inteiro.

**Recomendação:** perfil `prod` com tudo em `INFO`/`WARN` e `show-sql=false`. Nunca logar objetos de request inteiros.

---

### P3.6 — Ciclo de vida do token sem revogação

JWT stateless de 12h, sem refresh token, sem blacklist, sem `jti`. Não existe logout real — o token continua válido até expirar. Trocar a senha não invalida sessões. Bloquear um usuário (`blocked=true`) só faz efeito no próximo login, porque `JwtFilter` recarrega o `UserDetails` mas... na verdade **recarrega sim** via `loadUserByUsername`, então `isAccountNonLocked` é reavaliado — bom. Mas o custo é uma query ao banco por requisição.

**Recomendação:** access token curto (15 min) + refresh token persistido e revogável. Isso resolve logout, revogação e reduz a janela de um token vazado — sem perder o stateless no caminho quente.

---

### P3.7 — `README` descreve um sistema diferente do que existe

O README fala em "roles (ADMIN, BARBER, CLIENT)" — os enums reais são `PlatformRole{CLIENT, BUSINESS_OWNER, PLATFORM_ADMIN}` e `BusinessRole{OWNER, MANAGER, BARBER, VIEWER}`. Não menciona o header `X-Business-Slug`, que é **obrigatório** para praticamente toda a API. Não tem instruções de setup que funcionem (o `application.properties` está gitignored e não há `.example`).

Um desenvolvedor novo (ou você daqui a 6 meses) não consegue subir o projeto a partir deste repositório.

**Recomendação:** seção "Como rodar" testada num clone limpo. Documentar o contrato do header de tenant. Documentar as roles reais e a matriz de permissões (que hoje não existe escrita em lugar nenhum — está espalhada em 6 services).

---

### P3.8 — Sem soft delete onde importa

`BarberServiceService.delete` faz `repository.delete()` físico — mas a entidade tem campo `active`. Como `scheduling_services` referencia o serviço, deletar um serviço já usado ou lança violação de FK ou (pior, se houver cascade) apaga histórico de agendamento. `UserService.delete` faz `deleteById` num usuário que é referenciado por agendamentos, orders, movimentações de estoque.

**Recomendação:** soft delete (`active=false`) em `BarberService`, `AppUser` e `Business`. Delete físico apenas onde não há histórico.

---

### P3.9 — Sem auditoria

Nenhuma entidade tem `createdAt`/`updatedAt`/`createdBy` consistentemente — só `Order` (`@CreationTimestamp`) e `Product`/`Business` (com `LocalDateTime.now()` no campo, o que grava a hora de instanciação do objeto, não a do INSERT).

Num sistema que movimenta dinheiro e estoque, "quem mudou o preço", "quem cancelou o agendamento" e "quem ajustou o estoque" são perguntas que **vão** ser feitas.

**Recomendação:** `@EnableJpaAuditing` + `@CreatedDate`/`@LastModifiedDate`/`@CreatedBy` numa `@MappedSuperclass` base. Barato agora, impossível de reconstruir depois.

---

### P3.10 — `target/` versionado e IDE files

`target/classes` e `target/generated-sources` existem no diretório. O `.gitignore` cobre `target/`, então não estão commitados — mas `.idea/` e `.vscode/` estão listados no ignore **e presentes** no working tree, o que sugere que já foram versionados em algum momento. Vale um `git log --all --name-only -- .idea/` para confirmar e limpar se necessário.

---

# Novas implementações sugeridas

Ordenadas por relação valor/esforço, considerando que é um SaaS B2B cobrando por plano.

### 1. Notificações (o buraco mais visível para o cliente final)
Hoje o único e-mail que o sistema envia é reset de senha. Não existe:
- confirmação de agendamento para o cliente;
- lembrete 24h/1h antes (o maior redutor de no-show que existe — e no-show é a dor #1 de barbearia);
- aviso de cancelamento;
- **e-mail de convite** — hoje o convidado precisa adivinhar que deve entrar no sistema e olhar `/my-invitations`. O `InvitationService` cria um `token` que **nunca é enviado a ninguém**. A feature está pela metade.

Implementar como `@TransactionalEventListener(AFTER_COMMIT)` + fila (comece com `@Async` e um `ThreadPoolTaskExecutor` configurado; migre para SQS/Rabbit quando doer). WhatsApp (Meta Cloud API / Twilio) tem taxa de abertura muito maior que e-mail no Brasil e seria um diferencial real.

### 2. Dashboard financeiro
Você já tem os dados: `Order`, `Expenses`, `commissionPercentage`, `SchedulingAdditionalValue`, `PaymentMethod`. Falta agregá-los:
- faturamento por período / barbeiro / forma de pagamento;
- **comissão calculada por barbeiro** — o campo existe em `UserBusiness` e **nada no código o usa**. É uma feature 80% pronta;
- despesas vs receita, ticket médio, taxa de no-show;
- lucro por produto (você guarda `costPrice` — é só usar).

Este é o item que justifica upgrade de plano.

### 3. Endpoint público de agendamento (sem cadastro)
Hoje `POST /scheduling` exige autenticação e sempre associa a um `AppUser`. Mas `Scheduling.clientName` existe justamente para cliente sem cadastro — só que apenas staff consegue usá-lo. A fricção de "crie uma conta para cortar o cabelo" custa conversão. Permitir agendamento com nome + telefone, com confirmação por código SMS/WhatsApp.

### 4. Fila de espera / lista de encaixe
Quando não há slot, capturar o interesse e notificar em cancelamento. Aproveita a infra de notificação do item 1 e aumenta ocupação diretamente.

### 5. Billing de verdade
Hoje o plano é um `String` no `AppUser` e a renovação é `POST /users/{id}/renew` feito **manualmente por um PLATFORM_ADMIN**. Não escala além de dezenas de clientes. Integrar Stripe ou Asaas (melhor para PIX/boleto no Brasil), com webhook atualizando a `Subscription` de P2.11. Junto disso: job diário que desativa barbearia com plano vencido — hoje `planExpirationDate` é gravado e **nunca verificado depois da criação**.

### 6. Bloqueio de agenda do barbeiro
`OpeningHours` já suporta regra por barbeiro (`barber_id`) e `SPECIFIC_DATE`. Falta o caso "almoço", "folga da tarde", "férias de 15 dias" — hoje só dá para configurar dia inteiro. Adicionar intervalos de bloqueio dentro do dia.

### 7. Programa de fidelidade
"A cada 10 cortes, 1 grátis." Barato de implementar em cima do histórico de `Scheduling`/`Order` que você já tem, e é forte argumento comercial para o dono da barbearia.

---

# Ordem de execução sugerida

**Sprint 0 — Contenção (1–2 dias)**
Só o que para o sangramento, sem refatorar nada:
- [ ] Rotacionar as 3 credenciais expostas (P0.1)
- [ ] Remover `MailTextController` (P0.6)
- [ ] Remover `permitAll` do GET de inventário (P0.2)
- [ ] Adicionar `return` / lançar exceção no `ContextFilter` (P0.4)
- [ ] Filtrar por `businessId` em todos os métodos de `OrderService` (P0.3)
- [ ] `findByIdAndBusinessId` em `startAppointment` (P0.5)
- [ ] Remover `existsByEmail` do forgot-password (P0.7)
- [ ] Corrigir a validação AND do `completed/{id}` (P1.3)
- [ ] `jwt.secret` sem default + `.dockerignore` (P0.1)

**Sprint 1 — Fundação (1 semana)**
- [ ] Testes de isolamento multi-tenant com Testcontainers (P3.1) — **antes** de tudo abaixo
- [ ] CI no GitHub Actions (P3.2)
- [ ] Baseline Flyway + `ddl-auto=validate` + `application-prod.properties` (P1.1, P3.5)
- [ ] Corrigir `Dockerfile` (P1.11)
- [ ] Validação de extensão em upload (P0.8) e DTO público de barbeiro (P0.9)

**Sprint 2 — Corretude (1 semana)**
- [ ] `@Transactional` em `endService` (P1.4)
- [ ] Unificar fluxo de finalização; remover o duplicado (P1.5, P1.6)
- [ ] Sync do Google via evento AFTER_COMMIT, passando ID (P1.2)
- [ ] `IllegalAccessError` → exceção (P1.7); construtor vazio (P1.8)
- [ ] Lock/constraint contra double booking + índices (P1.9)
- [ ] Bean Validation em todos os DTOs (P2.6)

**Sprint 3 — Arquitetura (2 semanas)**
- [ ] `@TenantId` do Hibernate + remoção das checagens manuais (P2.1)
- [ ] Deletar `Membership`, rotas e métodos duplicados (P2.2, P2.3)
- [ ] Paginação e queries no banco (P2.4); N+1 (P2.5)
- [ ] Unificar dependências JWT (P2.7)
- [ ] Rate limiting (P2.9)
- [ ] Auditoria JPA (P3.9)

**Sprint 4+ — Timezone e produto**
- [ ] Migração para `Instant`/`timestamptz` (P1.10) — antes de crescer a base
- [ ] Extrair `Subscription` (P2.11)
- [ ] Notificações (novo #1) → Dashboard financeiro (novo #2) → Billing (novo #5)

---

## Uma observação sobre o padrão de erro

Se houver uma lição única a tirar daqui: **quase todo P0 desta lista é "esqueci de filtrar por tenant em um método"**. Não é falta de conhecimento — os métodos vizinhos fazem certo. É que o design atual exige lembrar, toda vez, em todo método novo, em todo módulo novo. Isso tem taxa de acerto humana, e a taxa de acerto humana em tarefa repetitiva não é 100%.

A correção durável não é revisar os 167 arquivos procurando esquecimentos. É **tornar o esquecimento impossível**: `@TenantId` no ORM, DTOs que nunca carregam dado sensível, testes que falham quando o vazamento aparece. Corrija os P0 hoje porque eles estão abertos — mas invista no Sprint 3, porque é ele que impede a lista de se reconstituir sozinha.
