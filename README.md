<div align="center">

# Goal Management API

API RESTful robusta desenvolvida em Java 21 e Spring Boot 4 para gerenciamento estratégico de metas e tarefas, com persistência relacional em PostgreSQL, migrações versionadas via Flyway e autenticação stateless com Spring Security, JWT e OAuth 2.0 Google.

[![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migration-CC0200?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

<br />
<br />

<a href="https://github.com/devrnrodrigues/goal-management-frontend">
  <img src="https://img.shields.io/badge/VER_FRONTEND-2563EB?style=for-the-badge&logo=react&logoColor=white" alt="Ver Frontend" />
</a>

</div>

---

## Tecnologias

| Tecnologia | Finalidade |
| --- | --- |
| **Java 21 (LTS)** | Linguagem de programação principal com recursos modernos de linguagem e tipos estritos |
| **Spring Boot 4.1.1** | Framework corporativo base para simplificação de dependências, injeção e ciclo de vida |
| **Spring Data JPA / Hibernate** | Camada de abstração e mapeamento objeto-relacional (ORM) com persistência em PostgreSQL |
| **Spring Security** | Framework de segurança corporativa e autorização para controle de endpoints e sessão |
| **Auth0 java-jwt (4.4.0)** | Emissão, decodificação e validação criptográfica de tokens JWT (HMAC256) |
| **PostgreSQL** | Sistema gerenciador de banco de dados relacional voltado à consistência ACID |
| **Flyway Migration** | Controle e automação de versionamento evolutivo do esquema do banco de dados |
| **Jakarta Bean Validation** | Validação sintática e declarativa de contratos de entrada nos DTOs |
| **Lombok** | Produtividade no código eliminando boilerplate (getters, setters, builders, construtores) |
| **Maven** | Gerenciador de dependências, ciclo de compilação e empacotamento da aplicação |

---

## Funcionalidades

### Autenticação & Sessão (`auth`)
- Cadastro de novos usuários com hashing seguro de senha.
- Login com credenciais locais (e-mail e senha) e emissão de Access Token e Refresh Token.
- Autenticação social via Google Identity Services (OAuth 2.0).
- Rotação de Refresh Tokens com persistência e mecanismo de revogação/logout.

### Gestão de Metas (`goal`)
- Criação, consulta paginada, detalhamento, atualização e exclusão de metas.
- Categorização de objetivos e controle de datas de início e prazo final.
- Associação de membros à meta com papéis de acesso específicos (`OWNER`, `ADMIN`, `VIEWER`).

### Quadro Kanban & Colunas (`board`)
- Criação automática das colunas padrão de fluxo ao cadastrar uma meta.
- Adição, edição, exclusão e reordenação dinâmica de colunas do quadro.

### Gestão de Tarefas (`task`)
- Cadastro, atualização e remoção de tarefas vinculadas a uma coluna da meta.
- Movimentação de tarefas entre colunas e posições com recálculo do progresso global da meta.

### Auditoria & Linha do Tempo (`activity`)
- Registro cronológico e rastreabilidade de eventos executados (criação de metas, movimentação de tarefas, atualizações de colunas).

### Painel Geral (`dashboard`)
- Agregação de métricas consolidadas: total de metas, metas em andamento, metas concluídas e taxas de progresso.

### Calendário & Relatórios (`calendar` / `report`)
- Consultas estruturadas de metas organizadas por janelas temporais de entrega e relatórios de rendimento.

### Usuários & Perfil (`user`)
- Consulta e atualização do perfil do usuário autenticado e busca de usuários para convites em metas.

---

## Segurança

- **Hashing Criptográfico de Senhas**: Utilização do algoritmo `BCryptPasswordEncoder` com custo de iterações configurado para armazenamento seguro de credenciais.
- **Autenticação Stateless com JWT**: Emissão de tokens de acesso assinados digitalmente via HMAC256 com tempo de expiração curto (15 minutos).
- **Rotação de Refresh Tokens**: Emissão de refresh tokens associados ao usuário, com validação de ciclo de vida e invalidação no logout.
- **Controle de Acesso Contextual (RBAC / ACL por Meta)**: O sistema não adota papéis globais genéricos para os usuários. As permissões são calculadas estritamente no contexto de cada meta:
  - `OWNER`: Proprietário da meta com privilégios totais (inclusão/remoção de membros e exclusão).
  - `ADMIN`: Permissão para criar, atualizar e mover colunas, tarefas e dados operacionais da meta.
  - `VIEWER`: Acesso somente-leitura aos dados da meta, quadro e tarefas associadas.
- **Transações Seguras**: Operações críticas de escrita utilizam `@Transactional` para garantir atomicidade e isolamento de dados.

---

## Validações

- **Validações Declarativas com Jakarta Bean Validation**: Todos os DTOs de entrada (`@RequestBody`) são validados com anotações padrão (`@NotNull`, `@NotBlank`, `@Size`, `@Email`), acompanhadas de mensagens em português (PT-BR).
- **Consistência Temporal de Metas**: Regras de negócio garantindo que prazos finais não sejam anteriores à data de início.
- **Validação de Permissões de Domínio**: Interceptação antes de alterações de estado para certificar se o usuário possui papel suficiente na meta correspondente.
- **Integridade Referencial**: Validação de existência de entidades associadas (usuários, metas, colunas) impedindo dados órfãos.

---

## Tratamento de Erros

A API centraliza o tratamento de exceções através da classe `@RestControllerAdvice` (`GlobalExceptionHandler`), padronizando as respostas HTTP em um formato JSON previsível:

```json
{
  "status": 422,
  "message": "Erro de validação nos campos informados.",
  "errors": {
    "title": "O título da meta é obrigatório.",
    "email": "Formato de e-mail inválido."
  }
}
```

### Mapeamento de Status HTTP

| Exceção | Código HTTP | Cenário de Ocorrência |
| --- | --- | --- |
| `ResourceNotFoundException` | **404 Not Found** | Identificador de meta, tarefa, coluna ou usuário não localizado |
| `BusinessException` | **400 Bad Request** | Violação de regra de negócio (ex: data limite anterior ao início) |
| `UnauthorizedException` | **401 Unauthorized** | Token ausente, inválido, expirado ou credenciais incorretas |
| `AccessForbiddenException` | **403 Forbidden** | Usuário sem nível de permissão suficiente para a meta |
| `MethodArgumentNotValidException` | **422 Unprocessable Entity** | Falha de validação nos campos do DTO de entrada |
| `Exception` | **500 Internal Server Error** | Falha inesperada não mapeada com log de erro |

---

## Arquitetura

O projeto adota a arquitetura **Package by Feature**, onde cada domínio de negócio organiza suas próprias entidades, repositórios, serviços, controladores e DTOs, mantendo alto desacoplamento e isolamento de responsabilidades.

### Estrutura de Pastas

```text
src/main/java/com/devrenanrodrigues/goalmanagementapi/
├── activity/                   # Histórico de atividades e auditoria cronológica
│   ├── dto/                    # Records de resposta de auditoria
│   ├── Activity.java           # Entidade de registro de atividade
│   ├── ActivityController.java # Endpoints de consulta de timeline
│   ├── ActivityRepository.java # Operações de consulta paginada
│   └── ActivityService.java    # Lógica de gravação e busca de eventos
├── auth/                       # Autenticação local, Google OAuth e tokens
│   ├── dto/                    # DTOs de login, registro, Google e refresh
│   ├── AuthController.java     # Endpoints públicos de autenticação
│   ├── AuthService.java        # Lógica de login, hash e emissão de tokens
│   ├── RefreshToken.java       # Entidade de persistência do refresh token
│   └── RefreshTokenRepository.java # Consulta e invalidação de tokens
├── board/                      # Gestão de colunas do quadro Kanban
│   ├── dto/                    # DTOs de criação, reordenação e resposta
│   ├── BoardColumn.java        # Entidade da coluna do quadro
│   ├── BoardColumnController.java # Endpoints de colunas e reordenação
│   ├── BoardColumnRepository.java # Consultas ordenadas por posição
│   └── BoardColumnService.java # Regras de criação e ordenação
├── calendar/                   # Agrupamento temporal de metas e prazos
├── common/                     # Componentes transversais compartilhados
│   ├── entity/                 # BaseAuditableEntity (createdAt, updatedAt)
│   ├── exception/              # GlobalExceptionHandler e exceções customizadas
│   └── security/               # SecurityConfig, JwtTokenService, JwtFilter
├── dashboard/                  # Métricas e indicadores consolidados
├── goal/                       # Domínio central de metas e participantes
│   ├── dto/                    # DTOs de metas, membros e permissões
│   ├── enums/                  # GoalCategory, GoalMemberRole
│   ├── Goal.java               # Entidade da meta
│   ├── GoalMember.java         # Entidade de membro com chave composta
│   ├── GoalMemberId.java       # Chave primária composta (goal_id, user_id)
│   ├── GoalController.java     # Endpoints CRUD de metas e membros
│   ├── GoalRepository.java     # Consultas otimizadas
│   └── GoalService.java        # Regras de negócio e controle de acesso
├── report/                     # Relatórios analíticos de progresso
├── task/                       # Tarefas operacionais vinculadas às metas
│   ├── dto/                    # DTOs de criação, movimentação e resposta
│   ├── Task.java               # Entidade de tarefa com ordenação
│   ├── TaskController.java     # Endpoints de tarefas e movimentação
│   ├── TaskRepository.java     # Consultas de tarefas por coluna
│   └── TaskService.java        # Lógica de movimentação e recálculo
└── user/                       # Gestão de usuários e perfis
```

### Destaques de Engenharia & Persistência

- **Identificadores Únicos em UUID**: Todas as entidades primárias expostas utilizam chaves `UUID` nativas, prevenindo enumeração sequencial.
- **Chave Composta Relacional**: A entidade `GoalMember` adota `@EmbeddedId` com `GoalMemberId` (`goal_id` + `user_id`), garantindo unicidade de relacionamento sem duplicidade.
- **Auditoria Nativa (`BaseAuditableEntity`)**: Campos `createdAt` e `updatedAt` gerenciados com listeners JPA.
- **Prevenção de Problemas N+1**: Consultas de relacionamentos críticos implementadas com joins explícitos e projeções para otimização de queries no PostgreSQL.
- **Evolução de Esquema com Flyway**: Migrações SQL versionadas e rastreáveis na pasta `db/migration`.

### Rotas da API

<details>
<summary><strong>Autenticação & Sessão (<code>/api/auth</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Público | Cadastro de nova conta de usuário |
| `POST` | `/api/auth/login` | Público | Autenticação com e-mail e senha |
| `POST` | `/api/auth/google` | Público | Autenticação social via Google Identity |
| `POST` | `/api/auth/refresh` | Público | Renovação de tokens de acesso com refresh token |
| `POST` | `/api/auth/logout` | Público | Revogação de refresh token e logout |

</details>

<details>
<summary><strong>Metas & Membros (<code>/api/goals</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/goals` | Autenticado | Cadastro de nova meta |
| `GET` | `/api/goals` | Autenticado | Listagem paginada de metas do usuário |
| `GET` | `/api/goals/{id}` | Autenticado | Detalhamento de meta por UUID |
| `PUT` | `/api/goals/{id}` | Autenticado (`OWNER`/`ADMIN`) | Atualização dos dados da meta |
| `DELETE` | `/api/goals/{id}` | Autenticado (`OWNER`) | Exclusão definitiva da meta |
| `POST` | `/api/goals/{id}/members` | Autenticado (`OWNER`) | Inclusão de membro na meta |
| `GET` | `/api/goals/{id}/members` | Autenticado | Listagem de membros vinculados |
| `PATCH` | `/api/goals/{id}/members/{userId}` | Autenticado (`OWNER`) | Atualização do papel do membro (`ADMIN`/`VIEWER`) |
| `DELETE` | `/api/goals/{id}/members/{userId}` | Autenticado (`OWNER`) | Desvinculação de membro da meta |

</details>

<details>
<summary><strong>Quadro Kanban & Colunas (<code>/api/goals/{goalId}/columns</code> & <code>/api/columns</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/goals/{goalId}/columns` | Autenticado | Listagem das colunas do quadro Kanban |
| `POST` | `/api/goals/{goalId}/columns` | Autenticado (`OWNER`/`ADMIN`) | Criação de nova coluna no quadro |
| `PUT` | `/api/columns/{columnId}` | Autenticado (`OWNER`/`ADMIN`) | Edição de título da coluna |
| `DELETE` | `/api/columns/{columnId}` | Autenticado (`OWNER`/`ADMIN`) | Exclusão de coluna |
| `PUT` | `/api/goals/{goalId}/columns/reorder` | Autenticado (`OWNER`/`ADMIN`) | Reordenação da sequência de colunas |

</details>

<details>
<summary><strong>Tarefas (<code>/api/goals/{goalId}/tasks</code> & <code>/api/tasks</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/goals/{goalId}/tasks` | Autenticado | Listagem de tarefas da meta |
| `POST` | `/api/goals/{goalId}/tasks` | Autenticado (`OWNER`/`ADMIN`) | Criação de nova tarefa vinculada a uma coluna |
| `PUT` | `/api/tasks/{taskId}` | Autenticado (`OWNER`/`ADMIN`) | Edição de dados da tarefa |
| `DELETE` | `/api/tasks/{taskId}` | Autenticado (`OWNER`/`ADMIN`) | Remoção de tarefa |
| `PATCH` | `/api/tasks/{taskId}/move` | Autenticado (`OWNER`/`ADMIN`) | Movimentação de tarefa entre colunas e posições |

</details>

<details>
<summary><strong>Métricas & Painel Geral (<code>/api/dashboard</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/dashboard` | Autenticado | Métricas e contadores consolidados do usuário |

</details>

<details>
<summary><strong>Calendário de Entregas (<code>/api/calendar</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/calendar` | Autenticado | Visualização cronológica de prazos de metas |

</details>

<details>
<summary><strong>Relatórios Analíticos (<code>/api/reports</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/reports` | Autenticado | Resumo estatístico de rendimento e produtividade |

</details>

<details>
<summary><strong>Histórico & Auditoria (<code>/api/activities</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/activities` | Autenticado | Linha do tempo e histórico de auditoria de ações |

</details>

<details>
<summary><strong>Usuários & Perfil (<code>/api/users</code>)</strong></summary>

<br />

| Método | Endpoint | Acesso | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | Autenticado | Obtenção do perfil do usuário autenticado |
| `PUT` | `/api/users/me` | Autenticado | Atualização dos dados do perfil |
| `GET` | `/api/users/search` | Autenticado | Busca de usuários por nome ou e-mail para convites |

</details>

---

## Documentação da API

- **Coleção Postman Oficial**: O projeto disponibiliza o arquivo `Goal_Management_API.postman_collection.json` na raiz do repositório, contendo todos os endpoints mapeados com scripts automatizados de autenticação, persistência de variáveis e cenários de validação.
- **Contingência**: A tabela de rotas documentada na seção de **Arquitetura** reflete o catálogo de endpoints ativos.
- **Swagger / OpenAPI**:
  > [!NOTE]
  > **Status: Planejamento**  
  > A integração da documentação interativa via Springdoc OpenAPI / Swagger UI está em fase de planejamento para versões futuras da API.

---

## Configuração de Ambiente

As configurações sensíveis e dinâmicas da aplicação são carregadas via variáveis de ambiente com valores padrão de desenvolvimento definidos no `application.yaml`:

| Variável | Descrição | Exemplo |
| --- | --- | --- |
| `DB_URL` | URL de conexão JDBC com o PostgreSQL | `jdbc:postgresql://localhost:5432/goal_management` |
| `DB_USERNAME` | Usuário do banco de dados | `postgres` |
| `DB_PASSWORD` | Senha do banco de dados | `postgres` |
| `JWT_SECRET` | Chave secreta para assinatura criptográfica dos tokens JWT | `sua_chave_secreta_jwt_de_pelo_menos_256_bits` |
| `GOOGLE_CLIENT_ID` | Client ID registrado no Google Cloud Console para OAuth 2.0 | `914972024799-xxxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Client Secret registrado no Google Cloud Console | `GOCSPX-xxxx` |

---

## Testes

> [!NOTE]
> **Status: Planejamento**  
> A suíte automatizada de testes encontra-se em fase de planejamento arquitetural (com teste base de carregamento de contexto Spring Boot implementado). O escopo futuro contemplará:
> - **Testes Unitários**: Regras de negócio e use cases isolados nos Services utilizando JUnit 5 e Mockito.
> - **Testes de Integração**: Validação de fluxos de ponta a ponta dos Controllers via `MockMvc` com segurança simulada.
> - **Testes de Persistência**: Verificação de queries customizadas e constraints relacionais via `@DataJpaTest` com banco de dados em container.

---

## Como Rodar o Projeto

### Pré-requisitos
- **Java 21 JDK** instalado e configurado no PATH
- **Docker** e Docker Compose (para inicialização do PostgreSQL)
- **Maven 3.9+** (ou o wrapper `./mvnw` incluído no projeto)

### Passo a Passo

1. **Clonar o repositório**:
   ```bash
   git clone https://github.com/devrnrodrigues/goal-management-backend.git
   ```

2. **Acessar o diretório**:
   ```bash
   cd goal-management-backend
   ```

3. **Subir a instância do PostgreSQL via Docker**:
   ```bash
   docker run --name goal-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16-alpine
   ```

4. **Criar o banco de dados da aplicação**:
   ```bash
   docker exec -i goal-postgres psql -U postgres -c "CREATE DATABASE goal_management;"
   ```

5. **Configurar as variáveis de ambiente**:
   ```bash
   cp src/main/resources/application.example.yaml src/main/resources/application.yaml
   ```
   *Edite o arquivo `src/main/resources/application.yaml` ajustando suas credenciais conforme necessário.*

6. **Executar a aplicação via Maven**:
   ```bash
   ./mvnw spring-boot:run
   ```
   *Em terminais Windows PowerShell, execute:*
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   *A API estará operacional em `http://localhost:8080` e as migrações do Flyway serão executadas automaticamente.*

7. **Compilar e gerar o pacote de produção**:
   ```bash
   ./mvnw clean package -DskipTests
   ```

---

## Licença

Este projeto está licenciado sob os termos da [Licença MIT](https://opensource.org/licenses/MIT).
