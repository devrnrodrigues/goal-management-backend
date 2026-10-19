# Goal Management Backend

API RESTful desenvolvida para gestao de metas e tarefas com controle de acesso contextual por meta, historico de atividades e autenticacao segura.

## Tecnologias Utilizadas

* Java 21 LTS
* Spring Boot 4.1.1
* Spring Data JPA / Hibernate
* Spring Security com JWT (Auth0 java-jwt)
* PostgreSQL
* Flyway Migration
* Lombok
* Jakarta Bean Validation

## Arquitetura do Projeto

O projeto adota a arquitetura Package by Feature, organizando o codigo por dominios de negocio:

* user: gestao de entidades de usuarios e dados de perfil
* auth: cadastro de usuarios, login com BCrypt e emissao de tokens JWT
* goal: regras de metas, vinculacao de membros e controle de permissoes contextuais
* board: fluxo de colunas do quadro de tarefas com criacao automatica das colunas padrao
* task: ciclo de vida das tarefas, movimentacao e recalculo de progresso da meta
* activity: auditoria e historico cronologico das acoes realizadas
* common: classes base auditaveis, tratamento global de excecoes e filtros de seguranca

## Controle de Acesso Contextual

O sistema nao utiliza roles globais para usuarios. As permissoes sao definidas exclusivamente no escopo de cada meta:

* OWNER: proprietario da meta com acesso total, inclusao e remocao de membros e exclusao da meta
* ADMIN: permissao para criar e editar tarefas, colunas e atualizar dados da meta
* VIEWER: permissao restrita para visualizacao e consulta

## Como Executar o Projeto

### Pre-requisitos

* Java 21 JDK instalado
* Docker e Docker Compose (para o banco de dados PostgreSQL)
* Maven 3.9+ ou o wrapper incluido no projeto

### Passo a Passo

1. Iniciar o container do PostgreSQL na porta 5432:

```bash
docker run --name librarydb -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres
```

2. Criar o banco de dados da aplicacao:

```bash
docker exec -i librarydb psql -U postgres -c "CREATE DATABASE goal_management;"
```

3. Configurar as variaveis da aplicacao copiando o arquivo de exemplo:

```bash
cp src/main/resources/application.example.yaml src/main/resources/application.yaml
```

4. Executar a aplicacao via Maven:

```bash
./mvnw spring-boot:run
```

A API estara disponivel em `http://localhost:8080`.

## Testes e Colecao do Postman

O projeto inclui o arquivo `Goal_Management_API.postman_collection.json` na raiz, contendo todos os endpoints mapeados com scripts automatizados para gerenciamento de tokens, criacao de metas, tarefas e validacao de cenarios de erro.
