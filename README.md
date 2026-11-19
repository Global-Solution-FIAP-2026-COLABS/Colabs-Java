# Colabs — Plataforma de Rede Solidária (Global Solution FIAP 2026/2)

---

**Integrantes:**
- Guilherme Santos Fonseca — RM 564232
- Gustavo Araujo da Silva — RM 566526
- Anthony de Souza Henriques — RM 566188

API RESTful desenvolvida em **Java 17** e **Spring Boot 3.4.3** para conectar cidadãos a pontos de coleta comunitários por meio de ofertas de doação e pedidos de ajuda, utilizando **Spring AI (OpenAI)** para triagem automática de urgência e **RabbitMQ** para publicação e consumo assíncrono de eventos de ciclo de vida.

---

## Sumário

- [Visão Geral e Problema Resolvido](#visão-geral-e-problema-resolvido)
- [Arquitetura e Tecnologias](#arquitetura-e-tecnologias)
- [Estrutura de Pacotes](#estrutura-de-pacotes)
- [Modelagem do Banco de Dados (Flyway)](#modelagem-do-banco-de-dados-flyway)
- [Segurança, Papéis e Controle de Posse](#segurança-papéis-e-controle-de-posse)
- [Inteligência Artificial (Spring AI)](#inteligência-artificial-spring-ai)
- [Mensageria Assíncrona (RabbitMQ)](#mensageria-assíncrona-rabbitmq)
- [Requisito 5 — Arquitetura Distribuída](#requisito-5--arquitetura-distribuída)
- [Como Executar](#como-executar)
- [Credenciais de Teste (Data Seeder)](#credenciais-de-teste-data-seeder)
- [Endpoints da API](#endpoints-da-api)
- [Testes Automatizados](#testes-automatizados)
- [Integração com o App Mobile](#integração-com-o-app-mobile)
- [Deploy e Vídeo Pitch](#deploy-e-vídeo-pitch)
- [Declaração de Uso de IA](#declaração-de-uso-de-ia)

---

## Visão Geral e Problema Resolvido

Em cenários de assistência social e resposta a emergências comunitárias, pontos de coleta recebem doações e demandas sem classificação prévia de urgência, gerando gargalos operacionais na triagem e falta de rastreabilidade entre cidadãos e centros de distribuição.

O **Colabs** resolve esse problema oferecendo uma plataforma CitizenTech onde:
1. **Cidadãos (`ROLE_CITIZEN`)** localizam pontos de coleta ativos e registram ofertas de doação ou pedidos de ajuda (`RecordType`) com listas detalhadas de itens (`DonationItem`).
2. **Spring AI** analisa automaticamente o título, descrição e itens da solicitação no momento do cadastro, atribuindo um nível de urgência (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), sugerindo a categoria mais aderente (`DonationCategory`) e gerando uma justificativa técnica.
3. **RabbitMQ** publica eventos assíncronos (`donation.created`, `donation.reviewed`, `donation.cancelled`, `donation.completed`) desacoplando o fluxo transacional do processamento de notificações e auditoria.
4. **Gestores de Pontos (`ROLE_POINT_MANAGER`)** revisam, aprovam ou rejeitam (`APPROVED`, `REJECTED`) exclusivamente os registros destinados ao seu ponto de coleta, enquanto o cidadão confirma a conclusão/entrega (`DELIVERED`).

---

## Arquitetura e Tecnologias

| Camada / Recurso | Tecnologia | Versão / Detalhes |
| :--- | :--- | :--- |
| **Linguagem & Runtime** | Java | 17 |
| **Framework Principal** | Spring Boot | 3.4.3 (`Web`, `Data JPA`, `Validation`, `Security`, `Cache`, `Actuator`, `AMQP`) |
| **Banco de Dados** | Oracle Database FIAP | `oracle.fiap.com.br:1521:orcl` + Driver `ojdbc11` |
| **Versionamento de Schema** | Flyway Migration | `V1` a `V6` (`ddl-auto=validate`) |
| **Autenticação & Autorização** | Spring Security + JJWT | `0.12.6` (Stateless JWT + `@EnableMethodSecurity`) |
| **Inteligência Artificial** | Spring AI (OpenAI) | `1.0.0-M6` (`gpt-4o-mini` com fallback determinístico) |
| **Mensageria Assíncrona** | RabbitMQ (`spring-amqp`) | `TopicExchange` (`colabs.donations.exchange`) + JSON Converter |
| **Cache em Memória** | Spring Cache | `@Cacheable` em consultas de pontos de coleta + `@EvictCatalogCaches` |
| **Documentação Interativa** | SpringDoc OpenAPI / Swagger | `2.8.5` (`/swagger-ui.html` com esquema `bearerAuth`) |
| **Testes Automatizados** | JUnit 5 + Mockito + Jakarta Validation | Testes unitários cobrindo DTOs, `CurrentUser`, `AccountService` e `DonationService` |
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
│   │   │   ├── CitizenController.java
│   │   │   ├── CollectionPointController.java
│   │   │   └── DonationController.java
│   │   ├── dtos/
│   │   │   ├── AccountRequest.java / AccountResponse.java
│   │   │   ├── AiTriageResult.java
│   │   │   ├── CitizenRequest.java / CitizenResponse.java
│   │   │   ├── CollectionPointRequest.java / CollectionPointResponse.java
│   │   │   ├── DonationCreateRequest.java / DonationReviewRequest.java / DonationResponse.java
│   │   │   ├── DonationItemRequest.java / DonationItemResponse.java
│   │   │   ├── ErrorResponse.java
│   │   │   ├── LoginRequest.java / LoginResponse.java
│   │   │   └── RegisterRequest.java
│   │   ├── enums/
│   │   │   ├── AccountType.java
│   │   │   ├── DonationCategory.java
│   │   │   ├── DonationStatus.java
│   │   │   ├── RecordType.java
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
│   │   │   ├── Citizen.java
│   │   │   ├── CollectionPoint.java
│   │   │   ├── Donation.java
│   │   │   └── DonationItem.java
│   │   ├── repositories/
│   │   │   ├── AccountRepository.java
│   │   │   ├── CitizenRepository.java
│   │   │   ├── CollectionPointRepository.java
│   │   │   └── DonationRepository.java
│   │   ├── security/
│   │   │   ├── AccountUserDetails.java
│   │   │   ├── AuthService.java
│   │   │   ├── CurrentUser.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── PasswordConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── TokenService.java
│   │   └── services/
│   │       ├── AccountService.java
│   │       ├── CitizenService.java
│   │       ├── CollectionPointService.java
│   │       ├── DonationService.java
│   │       └── DonationTriageAiService.java
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           ├── V1__create_tables.sql
│           ├── V2__support_requests_and_community_visibility.sql
│           ├── V3__remove_neighborhood_column.sql
│           ├── V4__replace_category_table_with_enum.sql
│           ├── V5__create_donation_items_table.sql
│           └── V6__ensure_donation_type_column.sql
└── test/
    └── java/br/com/fiap/colabs/
        ├── ColabsApplicationTests.java
        ├── dtos/RequestValidationTest.java
        ├── security/CurrentUserTest.java
        └── services/
            ├── AccountServiceTest.java
            └── DonationServiceTest.java
```

---

## Modelagem do Banco de Dados (Flyway)

O schema relacional é versionado automaticamente via Flyway e validado pelo Hibernate (`spring.jpa.hibernate.ddl-auto=validate`), composto por **5 entidades principais**:

- **`Account`**: credenciais de acesso, papel (`Roles`) e vínculo opcional com cidadão ou ponto de coleta.
- **`Citizen`**: dados cadastrais do cidadão (`1:1` com `Account`).
- **`CollectionPoint`**: dados institucionais e endereço do ponto de coleta (`1:1` com `Account`).
- **`Donation`**: registro central de doação ou pedido de ajuda (`N:1` com `Citizen` e `CollectionPoint`), armazenando categoria (`DonationCategory`), tipo (`RecordType`), status (`DonationStatus`) e os metadados da triagem por IA.
- **`DonationItem`**: itens detalhados pertencentes a cada registro (`N:1` com `Donation`, cascade e orphan removal).

---

## Segurança, Papéis e Controle de Posse

A API é **stateless**: o login devolve um JWT (HS256, validade de 24h) que deve ser enviado no header `Authorization: Bearer <token>`. O token carrega `role`, `accountId`, `citizenId` e `collectionPointId`.

| Papel | Permissões |
| :--- | :--- |
| `ROLE_CITIZEN` | Gerenciar o próprio perfil, cadastrar doações/pedidos de ajuda, listar e cancelar (enquanto `PENDING`) ou concluir (`DELIVERED`) seus registros |
| `ROLE_POINT_MANAGER` | Listar e revisar (`APPROVED`, `REJECTED`) os registros vinculados ao **seu** ponto de coleta e atualizar os dados desse ponto |
| `ROLE_ADMIN` | Acesso total: CRUD de pontos de coleta, cidadãos e visão completa de todas as doações |

Endpoints são protegidos por `@PreAuthorize` e a posse do recurso é validada em `CurrentUser` (um cidadão não altera dados de outro; um gestor atua apenas no próprio ponto). Requisições sem token válido recebem `401`; sem permissão, `403`. Respostas de erro seguem o padrão `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `path`, `fieldErrors`).

---

## Inteligência Artificial (Spring AI)

A funcionalidade de IA está **integrada diretamente ao fluxo transacional de cadastro**, não sendo um chat genérico isolado:

1. O cidadão envia `POST /donations` com título, descrição, tipo de registro, ponto de coleta e lista de itens.
2. `DonationTriageAiService` estrutura um prompt contextualizado e aciona o `ChatClient` (OpenAI `gpt-4o-mini`, temperatura `0.2`), solicitando saída estruturada mapeada para o record `AiTriageResult` (`urgency`, `suggestedCategory`, `triageReason`).
3. O resultado é persistido junto ao registro (`urgency`, `aiSuggestedCategory`, `aiTriageReason`) para que o gestor visualize a fila já priorizada (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
4. **Fallback resiliente:** caso não haja `OPENAI_API_KEY` configurada ou ocorra indisponibilidade externa, uma triagem determinística por palavras-chave assume automaticamente, garantindo que o cidadão nunca seja impedido de registrar sua solicitação.

**Valor para o cidadão:** itens críticos (água potável, medicamentos, insumos de emergência) recebem priorização imediata na triagem comunitária.

---

## Mensageria Assíncrona (RabbitMQ)

| Item | Configuração |
| :--- | :--- |
| **Exchange (Topic)** | `colabs.donations.exchange` |
| **Fila durável** | `colabs.donations.events.queue` |
| **Binding** | `donation.#` |
| **Routing keys publicadas** | `donation.created`, `donation.reviewed`, `donation.cancelled`, `donation.completed` |
| **Payload** | `DonationEvent` serializado em JSON (`Jackson2JsonMessageConverter`) |

`DonationService` publica o evento após persistir a operação (`DonationEventPublisher`) e `DonationEventConsumer` (`@RabbitListener`) o consome de forma assíncrona. Se o broker estiver indisponível, a falha é registrada em log sem interromper a resposta HTTP ao cliente.

> **Nota:** No `application.properties`, o listener inicia com `spring.rabbitmq.listener.simple.auto-startup=false`. No `docker-compose.yml` ele é ativado automaticamente via `SPRING_RABBITMQ_LISTENER_SIMPLE_AUTO_STARTUP=true`. Para rodar localmente com consumo ativo, exporte essa variável como `true`.

---

## Requisito 5 — Arquitetura Distribuída

**Opção escolhida: A — Mensageria Assíncrona com RabbitMQ**, implementando publicação e consumo desacoplado de eventos de domínio conforme detalhado na seção anterior.

---

## Como Executar

### Pré-requisitos

- Java 17 e Maven (ou use o wrapper `./mvnw` incluso)
- Docker e Docker Compose (para o RabbitMQ e execução em container)
- Acesso ao Oracle Database da FIAP (`oracle.fiap.com.br:1521:orcl`) com credenciais válidas (VPN/rede FIAP, se necessário)
- Chave da OpenAI (opcional: sem ela a triagem opera em modo fallback determinístico)

### Variáveis de ambiente

| Variável | Obrigatória | Descrição |
| :--- | :---: | :--- |
| `DB_USERNAME` | Sim | Usuário do Oracle Database |
| `DB_PASSWORD` | Sim | Senha do Oracle Database |
| `DB_URL` | Não | Padrão: `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl` |
| `JWT_SECRET` | Sim | Chave secreta para assinatura HS256 do JWT (mínimo 32 caracteres) |
| `OPENAI_API_KEY` | Não | Chave da API OpenAI (se omitida, ativa o fallback) |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | Não | Padrão: `localhost` / `5672` |
| `RABBITMQ_USER` / `RABBITMQ_PASS` | Não | Padrão: `guest` / `guest` |

### Opção 1 — Docker Compose (API + RabbitMQ)

Crie um arquivo `.env` na raiz do projeto:

```env
ORACLE_USER=seu_rm
ORACLE_PASSWORD=sua_senha
JWT_SECRET=troque-por-um-segredo-com-32-caracteres-ou-mais
OPENAI_API_KEY=sk-...
```

Execute:

```bash
docker compose up --build
```

- **API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **RabbitMQ Management:** http://localhost:15672 (`guest` / `guest`)

### Opção 2 — Execução Local (RabbitMQ via Docker + API via Maven)

```bash
docker compose up -d rabbitmq

export DB_USERNAME=seu_rm
export DB_PASSWORD=sua_senha
export JWT_SECRET=troque-por-um-segredo-com-32-caracteres-ou-mais
export OPENAI_API_KEY=sk-...
export SPRING_RABBITMQ_LISTENER_SIMPLE_AUTO_STARTUP=true

./mvnw spring-boot:run
```

As migrations do Flyway (`V1` a `V6`) são aplicadas automaticamente na inicialização.

---

## Credenciais de Teste (Data Seeder)

Na primeira execução (quando a tabela de contas estiver vazia), o `DataSeederConfig` popula automaticamente o ponto de coleta inicial, um cidadão e 3 contas de teste. Para desativar o seeder, defina `app.seed.enabled=false`.

| Papel | E-mail | Senha |
| :--- | :--- | :--- |
| `ROLE_ADMIN` | `admin@colabs.com` | `Admin@123` |
| `ROLE_POINT_MANAGER` (Ponto Solidário Centro) | `gestor@colabs.com` | `Gestor@123` |
| `ROLE_CITIZEN` (Maria Silva) | `cidadao@colabs.com` | `Cidadao@123` |

> Contas `ROLE_ADMIN` são criadas exclusivamente via seeder; o endpoint público de cadastro aceita apenas `CITIZEN` e `COLLECTION_POINT`.

---

## Endpoints da API

Documentação interativa completa em `/swagger-ui.html` (clique em **Authorize** e informe o token JWT).

### Autenticação (Público)

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `POST` | `/auth/register` | Cadastro transacional único de conta + perfil (`CITIZEN` ou `COLLECTION_POINT`) |
| `POST` | `/auth/login` | Autentica credenciais e retorna o token JWT |

#### Exemplos de Cadastro (`POST /auth/register`)

**Cidadão (`type: "CITIZEN"` $\rightarrow$ `ROLE_CITIZEN`):**

```json
{
  "email": "maria@email.com",
  "password": "Senha@123",
  "type": "CITIZEN",
  "citizen": {
    "name": "Maria Silva",
    "cpf": "529.982.247-25",
    "phone": "11987654321"
  }
}
```

**Ponto de Coleta (`type: "COLLECTION_POINT"` $\rightarrow$ `ROLE_POINT_MANAGER`):**

```json
{
  "email": "contato@pontosolidario.org",
  "password": "Senha@123",
  "type": "COLLECTION_POINT",
  "collectionPoint": {
    "name": "Ponto Solidário Centro",
    "cnpj": "11.222.333/0001-81",
    "address": "Rua das Flores, 100 - Centro",
    "phone": "1133334444"
  }
}
```

### Pontos de Coleta

| Método | Rota | Acesso |
| :--- | :--- | :--- |
| `GET` | `/collection-points` e `/collection-points/{id}` | Público (com cache) |
| `POST` | `/collection-points` | `ADMIN` |
| `PUT` | `/collection-points/{id}` | `POINT_MANAGER` (próprio ponto) ou `ADMIN` |
| `DELETE` | `/collection-points/{id}` | `ADMIN` |

### Cidadãos

| Método | Rota | Acesso |
| :--- | :--- | :--- |
| `GET` | `/citizens` | `ADMIN` |
| `GET` / `PUT` | `/citizens/{id}` | `CITIZEN` (próprio perfil) ou `ADMIN` |
| `POST` | `/citizens` | `CITIZEN` ou `ADMIN` |
| `DELETE` | `/citizens/{id}` | `ADMIN` |

### Doações e Pedidos de Ajuda

| Método | Rota | Acesso |
| :--- | :--- | :--- |
| `GET` | `/donations` e `/donations/my` | Autenticado (filtrado conforme papel e visibilidade comunitária) |
| `GET` | `/donations/{id}` | Autor, gestor do ponto ou `ADMIN` |
| `POST` | `/donations` | `CITIZEN` ou `ADMIN` (aciona triagem Spring AI e evento RabbitMQ) |
| `PUT` | `/donations/{id}/review` | `POINT_MANAGER` (do ponto) ou `ADMIN` (`APPROVED` / `REJECTED`) |
| `PUT` | `/donations/{id}/complete` | `CITIZEN` (autor) ou `ADMIN` (marca como `DELIVERED`) |
| `DELETE` | `/donations/{id}` | Cancela registro `PENDING` (autor ou `ADMIN`) |

Todas as listagens suportam paginação padrão do Spring Data (`?page=0&size=10&sort=id,desc`).

### Exemplo de Fluxo via cURL

```bash
# 1) Autenticação
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cidadao@colabs.com","password":"Cidadao@123"}'

# 2) Criar doação com itens (aciona Spring AI + RabbitMQ)
curl -X POST http://localhost:8080/donations \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Cestas básicas e água mineral",
    "description": "Itens urgentes para famílias afetadas pelas chuvas",
    "category": "FOOD",
    "recordType": "DONATION",
    "collectionPointId": 1,
    "items": [
      { "name": "Cesta básica completa", "quantity": 10, "unit": "un" },
      { "name": "Água mineral 5L", "quantity": 20, "unit": "galões" }
    ]
  }'
```

---

## Testes Automatizados

```bash
./mvnw test
```

Os testes unitários (`RequestValidationTest`, `CurrentUserTest`, `AccountServiceTest`, `DonationServiceTest`) executam de forma isolada com Mockito sem depender de banco de dados ou broker ativo. O teste de contexto (`ColabsApplicationTests`) valida o carregamento completo da aplicação quando as variáveis de ambiente e o Oracle estão acessíveis.

---

## Integração com o App Mobile

O aplicativo desenvolvido em React Native para a disciplina de **Mobile Application Development** consome esta API RESTful (autenticação JWT, listagem de pontos de coleta, cadastro e acompanhamento de doações).
- **Repositório Mobile:** **[PREENCHER link do repositório Mobile]**

---

## Deploy e Vídeo Pitch

- **Deploy em Nuvem:** **[PREENCHER link do deploy ou informar: "Execução local via Docker Compose / Maven conforme instruções acima"]**
- **Vídeo Pitch (até 5 min):** **[PREENCHER link público do YouTube ou Google Drive]**

---

## Declaração de Uso de IA

Em conformidade com as diretrizes da Global Solution FIAP 2026/2:
- **IA Generativa na Solução:** O sistema integra **Spring AI com OpenAI (`gpt-4o-mini`)** na classe `DonationTriageAiService` para realizar a classificação de urgência e sugestão de categoria das doações registradas pelos cidadãos, com fallback determinístico em caso de indisponibilidade.
- **IA Generativa no Desenvolvimento:** Ferramentas de IA generativa (ChatGPT / Gemini) foram utilizadas como apoio na revisão de arquitetura Spring Boot, depuração de mapeamentos JPA/Flyway, geração de casos de teste unitários e estruturação da documentação técnica (`README.md`). Todo o código gerado ou sugerido foi revisado, validado e testado pelos integrantes do grupo.
