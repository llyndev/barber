
# Barber SaaS API

API backend para um sistema SaaS de gestão de barbearias, desenvolvido com Java e Spring Boot. Permite que múltiplas barbearias utilizem a mesma plataforma, cada uma com seus próprios usuários, serviços, regras e dados isolados (multi-tenant).

---

## Descrição

Este projeto evoluiu para um SaaS completo para gestão de barbearias. Cada barbearia pode se cadastrar na plataforma, configurar seus próprios serviços, horários, equipe e regras de negócio, tudo de forma isolada e segura. O sistema permite que múltiplos negócios operem simultaneamente, com dados segregados e recursos avançados para administração, agendamento, controle financeiro e comunicação.

---


## Funcionalidades SaaS

* **Multi-Tenant:** Cada barbearia possui seu próprio ambiente, dados e configurações isoladas.
* **Onboarding de Barbearias:** Cadastro e configuração inicial facilitada para novos negócios.
* **Autenticação e Autorização:** Login seguro com JWT, controle de acesso por roles (ADMIN, BARBER, CLIENT) e contexto de negócio.
* **Gestão de Usuários:** Cadastro e gerenciamento de clientes, barbeiros e administradores por barbearia.
* **Gestão de Serviços:** CRUD de serviços personalizados por barbearia (corte, barba, combos, etc.), com duração e preço.
* **Horários de Funcionamento:**
  * Configuração flexível de horários semanais e regras de exceção (feriados, eventos).
* **Agendamentos:**
  * Clientes agendam, cancelam e visualizam horários.
  * Barbeiros e administradores gerenciam agendamentos e disponibilidade.
  * Verificação de disponibilidade em tempo real.
* **Consulta de Horários Disponíveis:** Endpoint para consulta dinâmica de horários livres por barbeiro, serviço e data.
* **Gestão de Negócios:** Cada barbearia pode gerenciar sua equipe, serviços, clientes e regras sem interferir em outros negócios.
* **Recursos SaaS:**
  * Isolamento de dados por barbearia
  * Suporte a múltiplos negócios
  * Expansível para integrações (pagamentos, notificações, etc.)

---


## Tecnologias Utilizadas

* **Java 21**
* **Spring Boot 3**
* **Spring Security**
* **Spring Data JPA (Hibernate)**
* **PostgreSQL**
* **Maven**
* **Lombok**
* **JWT (JSON Web Tokens)**
* **Multi-Tenancy (Contexto de Negócio)**

---


## Endpoints da API

Principais endpoints para integração e uso da plataforma SaaS:

---


### Autenticação e Onboarding

* `POST /auth/register` - Registrar um novo usuário (cliente, barbeiro ou admin).
* `POST /auth/login` - Autentica usuário e retorna token JWT.
* `POST /auth/me` - Retorna informações do usuário autenticado.
* `POST /business/register` - Cadastro de nova barbearia na plataforma (onboarding SaaS).

---


### Usuários

* `GET /users` - Lista usuários da barbearia (Admin).
* `GET /users/{id}` - Busca usuário por ID (Admin).
* `GET /users/barbers` - Lista barbeiros da barbearia.
* `PUT /users/{id}` - Atualiza informações de usuário (Admin).
* `PATCH /users/{id}/role` - Atualiza role de usuário (Admin).
* `DELETE /users/{id}` - Remove usuário (Admin).

---


### Serviços da Barbearia

* `GET /barber-services` - Lista serviços da barbearia.
* `GET /barber-services/{id}` - Busca serviço por ID.
* `POST /barber-services` - Cria novo serviço (Admin).
* `PUT /barber-services/{id}` - Atualiza serviço (Admin).
* `DELETE /barber-services/{id}` - Remove serviço (Admin).

---


### Horários de Funcionamento

* `GET /opening-hours/weekly-schedule` - Retorna horários semanais da barbearia.
* `PUT /opening-hours/weekly-schedule` - Cria/atualiza horários semanais (Admin).
* `GET /opening-hours/specific-date` - Retorna regras de exceção (feriados/eventos).
* `POST /opening-hours/specific-date` - Cria regra de exceção (Admin).
* `PUT /opening-hours/specific-date/{id}` - Atualiza regra de exceção (Admin).
* `DELETE /opening-hours/specific-date/{id}` - Remove regra de exceção (Admin).

---


### Agendamentos

* `GET /scheduling` - Lista agendamentos da barbearia.
* `GET /scheduling/{id}` - Busca agendamento por ID.
* `GET /scheduling/per-customer` - Lista agendamentos do cliente autenticado.
* `GET /scheduling/per-barber` - Lista agendamentos do barbeiro autenticado.
* `GET /scheduling/per-day` - Lista agendamentos por data.
* `GET /scheduling/avaiable-times` - Consulta horários disponíveis para agendamento.
* `POST /scheduling` - Cria novo agendamento.
* `DELETE /scheduling/{id}` - Cancela agendamento (Cliente).
* `POST /scheduling/barber/{id}` - Cancela agendamento (Barbeiro).
* `PUT /scheduling/barber/completed/{id}` - Marca agendamento como concluído.
* `POST /scheduling/barber/add-service/{id}` - Adiciona serviço a agendamento existente.

---


## Como Executar o Projeto (Desenvolvimento)

1. Clone o repositório:
  ```bash
  git clone https://github.com/llyndev/barber.git
  ```
2. **Configure o banco de dados:**
   * Crie um banco de dados PostgreSQL.
   * Atualize as configurações de conexão no arquivo `src/main/resources/application.properties`:
      ```properties
      spring.datasource.url=jdbc:postgresql://localhost:5432/seu_banco_de_dados
      spring.datasource.username=seu_usuario
      spring.datasource.password=sua_senha
      ```
3. **Execute a aplicação:**
  * Execute pela IDE (IntelliJ/Eclipse) ou via Maven:
  ```bash
  ./mvnw spring-boot:run
  ```
4. A API estará disponível em `http://localhost:8080`.

## Onboarding de Nova Barbearia (SaaS)

1. Acesse o endpoint `/business/register` para cadastrar uma nova barbearia.
2. Configure os serviços, equipe e horários pelo painel administrativo (frontend).
3. Cada barbearia terá seu próprio ambiente, usuários e dados isolados.

---

Este projeto está em evolução contínua para oferecer cada vez mais recursos SaaS para barbearias. Sugestões e contribuições são bem-vindas!