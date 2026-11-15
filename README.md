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
| **Testes Unitários** | JUnit 5 + Mockito + Jakarta Validation | 8 testes cobrindo DTOs, `CurrentUser` e `DonationService` |
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
│   │   │   ├── AccountRequest.java / AccountResponse.java
│   │   │   ├── AiTriageResult.java
│   │   │   ├── CategoryRequest.java / CategoryResponse.java
│   │   │   ├── CitizenRequest.java / CitizenResponse.java
│   │   │   ├── CollectionPointRequest.java / CollectionPointResponse.java
│   │   │   ├── DonationCreateRequest.java / DonationReviewRequest.java / DonationResponse.java
│   │   │   ├── ErrorResponse.java
│   │   │   └── LoginRequest.java / LoginResponse.java
│   │   ├── enums/
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
        └── services/DonationServiceTest.java