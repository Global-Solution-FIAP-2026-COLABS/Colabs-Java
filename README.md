# Colabs — Plataforma de Rede Solidária (Global Solution FIAP)

---
Integrantes:
- Guilherme Santos Fonseca, RM 564232.
- Gustavo Araujo da Silva, RM 566526
- Anthony de Souza Henriques, RM 566188

API RESTful desenvolvida em **Java 17** e **Spring Boot 3.4.3** para conectar cidadãos doadores a pontos de coleta comunitários, utilizando **Spring AI (OpenAI)** para triagem automática de urgência das doações e **RabbitMQ** para publicação e consumo assíncrono de eventos de ciclo de vida.

---

## Sumário

* [Visão Geral e Problema Resolvido](#visão-geral-e-problema-resolvido)
* [Arquitetura e Tecnologias](#arquitetura-e-tecnologias)
* [Estrutura de Pacotes](#estrutura-de-pacotes)
* [Modelagem do Banco de Dados (Flyway)](#modelagem-do-banco-de-dados-flyway)
* [Segurança, Papéis e Controle de Posse](#segurança-papéis-e-controle-de-posse)
* [Inteligência Artificial (Spring AI)](#inteligência-artificial-spring-ai)
* [Mensageria Assíncrona (RabbitMQ)](#mensageria-assíncrona-rabbitmq)
* [Como Executar](#como-executar)
* [Credenciais de Teste (Data Seeder)](#credenciais-de-teste-data-seeder)
* [Endpoints da API](#endpoints-da-api)
* [Testes Automatizados](#testes-automatizados)
* [Requisito 5 — Arquitetura Distribuída](#requisito-5--arquitetura-distribuída)
* [Deploy e Vídeo Pitch](#deploy-e-vídeo-pitch)
* [Declaração de Uso de IA](#declaração-de-uso-de-ia)

---

## Visão Geral e Problema Resolvido

Em cenários de assistência social e resposta a emergências comunitárias, pontos de coleta recebem doações sem classificação prévia de urgência, gerando gargalos operacionais na triagem e falta de rastreabilidade entre doadores e centros de distribuição.

O **Colabs** resolve esse problema oferecendo uma plataforma centralizada onde:
1. **Cidadãos (`ROLE_CITIZEN`)** localizam pontos de coleta ativos por bairro e registram intenções de doação.
2. **Spring AI** analisa automaticamente o título, descrição e quantidade da doação no momento do cadastro, atribuindo um nível de urgência (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), sugerindo a categoria mais aderente e gerando uma justificativa técnica.
3. **RabbitMQ** publica eventos assíncronos (`donation.created`, `donation.reviewed`, `donation.cancelled`) desacoplando o fluxo transacional do processamento de notificações e auditoria.
4. **Gestores de Pontos (`ROLE_POINT_MANAGER`)** revisam, aprovam, rejeitam ou confirmam a entrega (`APPROVED`, `REJECTED`, `DELIVERED`) exclusivamente das doações destinadas ao seu ponto de coleta.

---

## Arquitetura e Tecnologias

| Camada / Recurso | Tecnologia | Versão / Detalhes |
| :--- | :--- | :--- |
| **Linguagem & Runtime** | Java | 17 |
| **Framework Principal** | Spring Boot | 3.4.3 (`Web`, `Data JPA`, `Validation`, `Security`, `Cache`, `Actuator`, `AMQP`) |
| **Banco de Dados** | Oracle Database FIAP | `oracle.fiap.com.br:1521:orcl` + Driver `ojdbc11` |
| **Versionamento de Schema** | Flyway Migration | `V1__create_tables.sql` (`ddl-auto=validate`) |
| **Autenticação & Autorização** | Spring Security + JJWT | `0.12.6` (Stateless JWT + `@EnableMethodSecurity`) |
| **Inteligência Artificial** | Spring AI (OpenAI) | `1.0.0-M6` (`gpt-4o-mini` com fallback determinístico) |
| **Mensageria Assíncrona** | RabbitMQ (`spring-amqp`) | `TopicExchange` (`colabs.donations.exchange`) + JSON Converter |
| **Cache em Memória** | Spring Cache | `@Cacheable` em catálogos + `@EvictCatalogCaches` |
| **Documentação Interativa** | SpringDoc OpenAPI / Swagger | `2.8.5` (`/swagger-ui.html` com esquema `bearerAuth`) |
| **Testes Unitários** | JUnit 5 + Mockito + Jakarta Validation | 27 testes cobrindo DTOs, `CurrentUser`, `AccountService` e `DonationService` |
| **Containerização** | Docker & Docker Compose | Multi-stage build Maven/Temurin 17 + RabbitMQ Management |

---

## Estrutura de Pacotes

```text
src/
├── main/
│   ├── java/br/com/fiap/colabs/
│   │   ├── ColabsApplication.java
│   │   ├── config/
│   │   │   ├── DataSeederConfig.java
│   │   │   ├── EvictCatalogCaches.java
│   │   │   ├── OpenApiConfig.java
│   │   │   └── RabbitMQConfig.java
│   │   ├── controllers/
│   │   │   ├── AuthController.java
│   │   │   ├── CategoryController.java
│   │   │   ├── CitizenController.java
│   │   │   ├── CollectionPointController.java
│   │   │   └── DonationController.java
│   │   ├── dtos/
│   │   │   ├── RegisterRequest.java / AccountResponse.java
│   │   │   ├── AiTriageResult.java
│   │   │   ├── CategoryRequest.java / CategoryResponse.java
│   │   │   ├── CitizenRequest.java / CitizenResponse.java
│   │   │   ├── CollectionPointRequest.java / CollectionPointResponse.java
│   │   │   ├── DonationCreateRequest.java / DonationReviewRequest.java / DonationResponse.java
│   │   │   ├── ErrorResponse.java
│   │   │   └── LoginRequest.java / LoginResponse.java
│   │   ├── enums/
│   │   │   ├── AccountType.java
│   │   │   ├── DonationStatus.java
│   │   │   ├── Roles.java
│   │   │   └── UrgencyLevel.java
│   │   ├── events/
│   │   │   ├── DonationEvent.java
│   │   │   ├── DonationEventConsumer.java
│   │   │   └── DonationEventPublisher.java
│   │   ├── exceptions/
│   │   │   ├── BusinessConflictException.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── ResourceNotFoundException.java
│   │   ├── models/
│   │   │   ├── Account.java
│   │   │   ├── Category.java
│   │   │   ├── Citizen.java
│   │   │   ├── CollectionPoint.java
│   │   │   └── Donation.java
│   │   ├── repositories/
│   │   │   ├── AccountRepository.java
│   │   │   ├── CategoryRepository.java
│   │   │   ├── CitizenRepository.java
│   │   │   ├── CollectionPointRepository.java
│   │   │   └── DonationRepository.java
│   │   ├── security/
│   │   │   ├── AccountUserDetails.java
│   │   │   ├── AuthService.java
│   │   │   ├── CurrentUser.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── TokenService.java
│   │   └── services/
│   │       ├── AccountService.java
│   │       ├── CategoryService.java
│   │       ├── CitizenService.java
│   │       ├── CollectionPointService.java
│   │       ├── DonationService.java
│   │       └── DonationTriageAiService.java
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__create_tables.sql
└── test/
    └── java/br/com/fiap/colabs/
        ├── dtos/RequestValidationTest.java
        ├── security/CurrentUserTest.java
        └── services/
            ├── AccountServiceTest.java
            └── DonationServiceTest.java


---

## Segurança, Papéis e Controle de Posse

A API é **stateless**: o login devolve um JWT (HS256, validade de 24h) que deve ser enviado no header `Authorization: Bearer <token>`. O token carrega `role`, `accountId`, `citizenId` e `collectionPointId`.

| Papel | O que pode fazer |
| :--- | :--- |
| `ROLE_CITIZEN` | Gerenciar o próprio perfil, cadastrar doações, ver e cancelar (se `PENDING`) as próprias doações |
| `ROLE_POINT_MANAGER` | Ver e revisar (`APPROVED`, `REJECTED`, `DELIVERED`) as doações do **seu** ponto de coleta e atualizar esse ponto |
| `ROLE_ADMIN` | Acesso total: CRUD de categorias, pontos, cidadãos e visão de todas as doações |

Endpoints são protegidos por `@PreAuthorize` e, além do papel, a posse do recurso é validada em `CurrentUser` (um cidadão não acessa dados de outro; um gestor só mexe no próprio ponto). Requisições sem token válido recebem `401`; sem permissão, `403`. Respostas de erro seguem o padrão `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`, `fieldErrors`).

---

## Inteligência Artificial (Spring AI)

A funcionalidade de IA está **dentro do fluxo de doação**, não é um chat solto:

1. O cidadão chama `POST /donations` com título, descrição, quantidade, ponto e categoria.
2. `DonationTriageAiService` monta um prompt e chama o `ChatClient` (OpenAI `gpt-4o-mini`, temperatura 0.2), pedindo saída estruturada mapeada para o record `AiTriageResult` (`urgency`, `suggestedCategory`, `triageReason`).
3. O resultado é salvo junto da doação (`urgency`, `aiSuggestedCategory`, `aiTriageReason`) e o gestor do ponto passa a ver as doações já classificadas por urgência (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
4. **Fallback:** se não houver `OPENAI_API_KEY` válida (ou se a chamada falhar), uma triagem determinística por palavras-chave é aplicada, e a doação nunca deixa de ser registrada.

**Valor para o cidadão:** itens essenciais (água, medicamentos, itens de emergência) são priorizados automaticamente, reduzindo o tempo até chegarem a quem precisa.

---

## Mensageria Assíncrona (RabbitMQ)

| Item | Valor |
| :--- | :--- |
| Exchange (topic) | `colabs.donations.exchange` |
| Fila durável | `colabs.donations.events.queue` |
| Binding | `donation.#` |
| Routing keys publicadas | `donation.created`, `donation.reviewed`, `donation.cancelled` |
| Payload | `DonationEvent` em JSON (tipo, id, título, status, urgência, cidadão, ponto, data) |

`DonationService` publica o evento após persistir a doação (`DonationEventPublisher`) e `DonationEventConsumer` (`@RabbitListener`) o consome de forma assíncrona. Se o broker estiver indisponível, a falha é registrada em log e a requisição HTTP não é interrompida.

> O listener vem com `spring.rabbitmq.listener.simple.auto-startup=false` no `application.properties`. No Docker Compose ele é ligado via `SPRING_RABBITMQ_LISTENER_SIMPLE_AUTO_STARTUP=true`. Para rodar localmente com consumo ativo, defina essa variável como `true`.

---

## Requisito 5 — Arquitetura Distribuída

**Opção escolhida: A — RabbitMQ**, com evento publicado e consumido de forma assíncrona (detalhes na seção anterior).

---

## Como Executar

### Pré-requisitos

* Java 17 e Maven (ou use o `./mvnw` incluso)
* Docker e Docker Compose (para o RabbitMQ e para subir a API em container)
* Acesso ao Oracle da FIAP (`oracle.fiap.com.br:1521:orcl`) com usuário e senha próprios (VPN/rede FIAP, se necessário)
* Chave da OpenAI (opcional: sem ela a triagem usa o fallback determinístico)

### Variáveis de ambiente

| Variável | Obrigatória | Descrição |
| :--- | :---: | :--- |
| `DB_USERNAME` | sim | Usuário do Oracle |
| `DB_PASSWORD` | sim | Senha do Oracle |
| `DB_URL` | não | Padrão: `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl` |
| `JWT_SECRET` | sim | Segredo de assinatura do JWT (use 32 caracteres ou mais) |
| `OPENAI_API_KEY` | não | Se ausente ou `sk-placeholder...`, usa o fallback |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | não | Padrão: `localhost` / `5672` |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | não | Padrão: `guest` / `guest` |

### Opção 1 — Docker Compose (API + RabbitMQ)

Crie um arquivo `.env` na raiz:

```env
ORACLE_USER=seu_rm
ORACLE_PASSWORD=sua_senha
JWT_SECRET=troque-por-um-segredo-com-32-caracteres-ou-mais
OPENAI_API_KEY=sk-...   # opcional
```

Depois:

```bash
docker compose up --build
```

* API: http://localhost:8080
* Swagger: http://localhost:8080/swagger-ui.html
* RabbitMQ Management: http://localhost:15672 (`guest` / `guest`)

### Opção 2 — Local (RabbitMQ no Docker, API via Maven)

```bash
docker compose up -d rabbitmq

export DB_USERNAME=seu_rm
export DB_PASSWORD=sua_senha
export JWT_SECRET=troque-por-um-segredo-com-32-caracteres-ou-mais
export OPENAI_API_KEY=sk-...                                  # opcional
export SPRING_RABBITMQ_LISTENER_SIMPLE_AUTO_STARTUP=true      # para o consumer processar eventos

./mvnw spring-boot:run
```

O Flyway cria as tabelas automaticamente na primeira execução (`V1__create_tables.sql`).

---

## Credenciais de Teste (Data Seeder)

Na primeira execução (quando ainda não existe nenhuma conta), o `DataSeederConfig` cadastra 5 categorias, 1 ponto de coleta ("Ponto Solidário Centro"), 1 cidadão e 3 contas de teste. Para desativar o seed: `app.seed.enabled=false`.

| Papel | E-mail | Senha |
| :--- | :--- | :--- |
| `ROLE_ADMIN` | `admin@colabs.com` | `Admin@123` |
| `ROLE_POINT_MANAGER` (Ponto Solidário Centro) | `gestor@colabs.com` | `Gestor@123` |
| `ROLE_CITIZEN` (Maria Silva) | `cidadao@colabs.com` | `Cidadao@123` |

> Contas `ADMIN` só existem via seed; o endpoint público de cadastro cria apenas cidadãos e pontos de coleta.

---

## Endpoints da API

Documentação interativa completa em `/swagger-ui.html` (use **Authorize** com o token obtido no login).

### Autenticação (público)

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| POST | `/auth/register` | Cadastro único: cria a conta e o perfil (cidadão ou ponto de coleta) em uma só requisição |
| POST | `/auth/login` | Autenticar e receber o JWT |

#### Cadastro único (`POST /auth/register`)

O usuário escolhe **Sou cidadão** ou **Sou ponto de coleta** e envia tudo em um único POST. A conta e o perfil são criados na mesma transação (se algo falhar, nada é salvo). O papel é definido pelo `type`; contas `ADMIN` não podem ser criadas por este endpoint.

Cidadão (`type: "CITIZEN"`, papel `ROLE_CITIZEN`):

```json
{
  "email": "maria@email.com",
  "password": "senha123",
  "type": "CITIZEN",
  "citizen": {
    "name": "Maria Silva",
    "cpf": "529.982.247-25",
    "phone": "11987654321",
    "neighborhood": "Centro"
  }
}
```

Ponto de coleta (`type: "COLLECTION_POINT"`, papel `ROLE_POINT_MANAGER`):

```json
{
  "email": "contato@pontosolidario.org",
  "password": "senha123",
  "type": "COLLECTION_POINT",
  "collectionPoint": {
    "name": "Ponto Solidário Centro",
    "cnpj": "11.222.333/0001-81",
    "address": "Rua das Flores, 100",
    "neighborhood": "Centro",
    "phone": "1133334444"
  }
}
```

Envie apenas o bloco correspondente ao `type` (o outro bloco deve ficar ausente). A resposta (`201`) traz `id`, `email`, `role`, `citizenId` e `collectionPointId`. Erros: `400` (validação ou bloco ausente/extra) e `409` (e-mail, CPF ou CNPJ já cadastrado).

### Categorias

| Método | Rota | Acesso |
| :--- | :--- | :--- |
| GET | `/categories`, `/categories/{id}` | Público |
| POST / PUT / DELETE | `/categories`, `/categories/{id}` | ADMIN |

### Pontos de coleta

| Método | Rota                        | Acesso |
| :--- |:----------------------------| :--- |
| GET |  `/collection-points/{id}`  | Público |
| POST | `/collection-points`        | ADMIN |
| PUT | `/collection-points/{id}`   | POINT_MANAGER (do próprio ponto) ou ADMIN |
| DELETE | `/collection-points/{id}`   | ADMIN |

### Cidadãos

| Método | Rota | Acesso |
| :--- | :--- | :--- |
| GET | `/citizens` | ADMIN |
| GET / PUT | `/citizens/{id}` | CITIZEN (próprio perfil) ou ADMIN |
| POST | `/citizens` | CITIZEN ou ADMIN |
| DELETE | `/citizens/{id}` | ADMIN |

### Doações

| Método | Rota | Acesso |
| :--- | :--- | :--- |
| GET | `/donations?status=` | Autenticado (filtrado pelo papel; filtro `status` aplicado para ADMIN) |
| GET | `/donations/{id}` | Autor, gestor do ponto ou ADMIN |
| POST | `/donations` | CITIZEN ou ADMIN (aciona triagem por IA e evento RabbitMQ) |
| PUT | `/donations/{id}/review` | POINT_MANAGER (do ponto) ou ADMIN |
| DELETE | `/donations/{id}` | Cancela doação `PENDING` (autor ou ADMIN) |

Todas as listagens aceitam paginação: `?page=0&size=10&sort=campo,asc`.

### Exemplo rápido

```bash
# 1) Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<email>","password":"<senha>"}'

# 2) Criar doação (usa o token retornado)
curl -X POST http://localhost:8080/donations \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"title":"Cestas básicas","description":"20 cestas básicas para famílias afetadas","quantity":20,"citizenId":1,"collectionPointId":1,"categoryId":1}'
```

---

## Testes Automatizados

```bash
./mvnw test
```

> Os 27 testes unitários (`RequestValidationTest`, `CurrentUserTest`, `AccountServiceTest`, `DonationServiceTest`) não precisam de banco nem de broker. `ColabsApplicationTests` carrega o contexto completo e exige as variáveis de ambiente e o banco acessíveis.

---

## Integração com o App Mobile

O app da disciplina de Mobile consome esta mesma API. A URL base e o fluxo (login, listagem de pontos, criação de doação, acompanhamento de status) estão descritos no repositório do app: **[PREENCHER link do repositório Mobile]**.

---

## Deploy e Vídeo Pitch

* Deploy: **[PREENCHER link do deploy, ou "sem deploy; execução local conforme acima"]**
* Vídeo pitch (até 5 min): **[PREENCHER link do vídeo]**

---

## Declaração de Uso de IA

**[PREENCHER/CONFERIR]** Ferramentas de IA generativa foram usadas como apoio ao desenvolvimento (ex.: sugestões de código, revisão e redação da documentação). **[LISTAR ferramentas e para quê foram usadas]**. Toda a lógica foi revisada, testada e é compreendida pelos integrantes do grupo, que respondem pelo projeto.

Dentro da própria solução, o recurso de IA generativa é a triagem de urgência das doações via **Spring AI (OpenAI `gpt-4o-mini`)**, descrita na seção [Inteligência Artificial](#inteligência-artificial-spring-ai).