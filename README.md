# Barbearia API

API de backend para um sistema de agendamento de barbearia, desenvolvida com Java e Spring Boot.

---

## Frontend (barberfront)

A interface de usuário para esta API foi desenvolvida como um projeto separado, utilizando [**React, Typescript**].

**Repositório do Frontend:** [Barber - Frontend React](https://github.com/llyndev/barberfront)

---

## Descrição

Este projeto é uma API RESTful projetada para gerenciar os agendamentos de uma barbearia. Ele permite que os clientes agendem horários com barbeiros, e que os adiministradores gerenciem os serviços, usuários e horários de funcionamento.

---

## Funcionalidades

* **Autenticação e Autorização:** Sistema de login seguro com JWT (JSON Web Tokens) e controle de acesso baseado em roles (ADMIN, BARBER, CLIENT).
* **Gerenciamento de Usuários:** Cadastro de novos clientes e gerenciamento de todos os usuários pelo adiministrador, incluidno a atribuição de roles.
* **Serviços da Barbearia:** CRUD completo para os serviços oferecidos, como corte de cabelo e barba, com duração e preço.
* **Horários de Funcionamento:** 
  * Configuração de horários de funcionamento recorrentes para cada dia da semana.
  * Criação de regras de exceção para datas específicas (feriados, eventos, etc.).
* **Agendamentos:**
  * Os clientes podem agendar, cancelar, e visualizar seus horários.
  * Os barbeiros podem visualizar e cancelar os agendamentos feitos com eles.
  * Verificação de disponibilidade de horários em tempo real.
* **Consulta de Horários Disponíveis:** Endpoint para consultar os horários livres com base no barbeiro, serviços e data desejada.

---

## Tecnologias Utilizadas

* **Java 21**
* **Spring Boot 3**
* **Spring Security:** Autenticação e autorização.
* **Spring Data JPA (Hibernate):** Persistência de dados.
* **PostgreSQL:** Banco de dados relacional.
* **Maven:** Gerenciamento de dependências e build.
* **Lombok:** Redução de boilerplate code.
* **JWT (JSON Web Tokens):** Para autenticação segura.

---

## Endpoints da API

A seguir então os principais endpoints da API:

---

### Autenticação

* `POST auth/register` - Registrar um novo usuário.
* `POST auth/login` - Autentica um usuário e retorna um token JWT.
* `POST auth/me` - Retorna as informações do usuário autenticado.'

---

### Usuários

* `GET /users` - Lista todos os usuários (Admin).
* `GET /users/{id}` - Busca um usuário por ID (Admin).
* `GET /users/barbers` - Lista todos os usuarios com role BARBER.
* `PUT /users/{id}` - Atualiza as informações de um usuário (Admin).
* `PATCH /users/{id}/role` - Atualiza a role de um usuário (Admin).
* `DELETE /users/{id}` - Remove um usuário (Admin).

---

### Serviços da Barbearia

* `GET /barber-services` - Lista todos os serviços.
* `GET /barber-services/{id}` - Busca um serviço por ID.
* `POST /barber-services` - Cria um novo serviço (Admin).
* `PUT /barber-services/{id}` - Atualiza um serviço (Admin).
* `DELETE /barber-services/{id}` - Remove um serviço (Admin).

---

### Horários de Funcionamento

* `GET /opening-hours/weekly-schedule` - Retorna a configuração dos horários da semana.
* `PUT /opening-hours/weekly-schedule` - Cria ou Atualiza a configuração dos horários da semana (Admin).
* `GET /opening-hours/specific-date` - Retorna as regras de exceção para datas específicas.
* `POST /opening-hours/specific-date` - Cria uma nova regra de exceção (Admin).
* `PUT /opening-hours/specific-date/{id}` - Atualiza uma regra de exceção (Admin).
* `DELETE /opening-hours/specific-date/{id}` - Remove uma regra de exceção (Admin).

---

### Agendamentos

* `GET /scheduling` - Lista todos os agendamentos.
* `GET /scheduling/{id}` - Busca um agendamento por ID.
* `GET /scheduling/per-customer` - Lista os agendamentos do cliente autenticado.
* `GET /scheduling/per-barber` - Lista os agendamentos do barbeiro autenticado.
* `GET /scheduling/per-day` - Lista os agendamentos de uma data específica.
* `GET /scheduling/avaiable-times` - Consulta os horários disponíveis para agendamento.
* `POST /scheduling` - Cria um novo agendamento.
* `DELETE /scheduling/{id}` - Cancela um agendamento (Cliente).
* `POST /scheduling/barber/{id}` - Cancela um agendamento (Barbeiro).
* `PUT /scheduling/barber/completed/{id}` - Marca um agendamento como concluído.
* `POST /scheduling/barber/add-service/{id}` - Adiciona um serviço a um agendamento existente.

---

## Como Executar o Projeto

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
   * Você pode executar a aplicação diretamente pela sua IDE (como IntelliJ ou Eclipse) ou usando Maven:
   ```bash
    ./mvnw spring-boot:run
   ```
4. A API estará disponível em `http://localhost:8080`.