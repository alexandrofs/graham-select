---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
lastStep: 8
status: 'complete'
completedAt: '2026-03-12'
inputDocuments:
  - docs/bmad/planning-artifacts/prd.md
  - docs/bmad/planning-artifacts/prd-validation-report.md
  - docs/bmad/planning-artifacts/product-brief-graham-select-2026-03-09.md
  - docs/bmad/planning-artifacts/ux-design-specification.md
  - docs/bmad/planning-artifacts/research/domain-gestao_carteiras_investimentos-research-2026-03-09.md
  - docs/bmad/planning-artifacts/research/market-aplicativos_de_gestao_de_carteira_de_investimentos-research-2026-03-09.md
  - docs/bmad/planning-artifacts/research/technical-extracao_dados_b3-research-2026-03-09.md
  - docs/bmad/project-context.md
  - docs/visao-produto.md
  - docs/workspace.dsl
workflowType: 'architecture'
project_name: 'graham-select'
user_name: 'Alex'
date: '2026-03-12'
---

# Architecture Decision Document

_Este documento é construído colaborativamente através de descoberta passo-a-passo. Seções são adicionadas conforme trabalhamos em cada decisão arquitetural juntos._

## Análise de Contexto do Projeto

### Visão Geral de Requisitos

**Requisitos Funcionais:**

O Graham Select é uma aplicação de gestão de portfólio de investimentos baseada na metodologia de Benjamin Graham, com foco no mercado brasileiro (B3). Os requisitos funcionais principais se organizam em 4 áreas:

1. **Ingestão de Dados** — Upload de arquivos Excel da B3 (até 50MB), parsing assíncrono via padrão Splitter, entrada manual de ativos como alternativa
2. **Consolidação de Portfólio** — Visão unificada multi-corretora, cálculo de preço médio, tracking de posições abertas/fechadas
3. **Análise & Ranking** — Ranking de empresas usando 7 critérios de Graham, cálculos de valuation (P/L, P/VP, dividend yield), sugestões com transparência de raciocínio
4. **Experiência do Usuário** — Dashboard com indicadores consolidados, feedback progressivo durante processamento, design responsivo mobile-first

**Requisitos Não-Funcionais (Drivers Arquiteturais):**

| Categoria | Requisito | Impacto Arquitetural |
|---|---|---|
| Performance | Upload até 50MB, processamento < 30s/1000 transações | Processamento assíncrono via Kafka, streaming de Excel |
| Segurança | TLS 1.3, JWT, criptografia em repouso | mTLS para Kafka, sem PII direto em mensagens |
| Escalabilidade | 10.000 usuários simultâneos | Kafka particionado, workers horizontais |
| Resiliência | Retry automático, idempotência | Dead-letter queues, circuit breakers |
| Compliance | LGPD, dados financeiros sensíveis | Anonimização, controle de acesso, auditoria |
| Acessibilidade | WCAG 2.1 AA | Contraste, navegação por teclado, screen readers |

**Escala & Complexidade:**

- **Domínio primário:** Full-Stack (Mobile + Backend + Event Processing)
- **Nível de complexidade:** Média-Alta
- **Componentes arquiteturais estimados:** ~8-10
- **Features em tempo real:** Parcial (feedback de processamento, não live data streaming)
- **Multi-tenancy:** Dados isolados por usuário
- **Compliance regulatório:** LGPD, dados financeiros

### Restrições Técnicas & Dependências

**Stack Definida (project-context.md):**

- **Frontend:** Flutter SDK com Provider + GoRouter
- **Backend:** Java 21, Spring Boot 3.4.13
- **Mensageria:** Apache Kafka (comunicação assíncrona obrigatória entre api e valuation-service)
- **Banco de Dados:** MySQL
- **Infraestrutura:** Docker, Docker Compose
- **Documentação C4:** Structurizr DSL

**Dependências Externas:**

- Arquivos Excel da B3 (formato pode variar entre versões)
- FastExcel para parsing de baixo consumo de memória
- S3/MinIO para armazenamento de arquivos

**Regras de Implementação:**

- Módulos Spring Boot separados por domínio
- CORS habilitado para frontend
- Conventional Commits para versionamento
- Testes unitários obrigatórios

### Preocupações Transversais Identificadas

1. **Observabilidade** — Rastreamento end-to-end do fluxo upload → Kafka → processamento → DB, métricas de saúde dos consumers
2. **Tratamento de Erros** — Dead-letter queues para mensagens falhas, retry com backoff exponencial, idempotência na ingestão de trades
3. **Segurança de Dados** — LGPD compliance, criptografia em trânsito e repouso, sem PII direto em mensagens Kafka (usar IDs internos)
4. **Autenticação/Autorização** — JWT em todas as camadas da API, mTLS para comunicação Kafka
5. **Validação de Dados** — Parsing resiliente de Excel (formato B3 muda entre versões), validação de domínio nos eventos Kafka

## Avaliação de Fundação Tecnológica

### Domínio Tecnológico Primário

**Full-Stack (Mobile + Backend + Event Processing)** — baseado na análise de requisitos do projeto.

### Preferências Técnicas Existentes

O projeto possui stack técnica completamente definida no `project-context.md`, não sendo necessário avaliar starter templates genéricos.

### Fundação Selecionada: Stack Customizada Existente

**Racional da Seleção:**

A fundação tecnológica não parte de um starter template genérico porque:
1. Stack já definida e validada pelo time
2. Código existente com estrutura Maven multi-module funcional
3. Expertise alinhada com as tecnologias escolhidas
4. Requisitos de Event-Driven Architecture requerem configuração customizada

**Backend — Spring Boot 3.4.13 Multi-Module (Maven):**

| Aspecto | Decisão |
|---|---|
| Linguagem & Runtime | Java 21 (records, sealed classes, pattern matching) |
| Framework | Spring Boot 3.4.13 (WebMVC + Spring Kafka) |
| Build | Maven multi-module (api, valuation-service, shared/common) |
| Testes | JUnit 5 + Mockito + Testcontainers (Kafka, MySQL) |
| Validação | Bean Validation (Jakarta Validation) |
| Documentação API | SpringDoc OpenAPI (Swagger) |
| Processamento Excel | FastExcel (low-memory streaming) |
| Storage | S3/MinIO para arquivos, MySQL para dados estruturados |

**Frontend — Flutter SDK:**

| Aspecto | Decisão |
|---|---|
| Framework | Flutter (mobile-first, multiplataforma) |
| State Management | Provider |
| Navegação | GoRouter |
| Design System | Material Design 3 customizado (Navy Blue, Emerald Green, Amber Gold) |
| HTTP Client | Dio ou http package |

**Infraestrutura:**

| Aspecto | Decisão |
|---|---|
| Mensageria | Apache Kafka (topics particionados por UserID+Ativo) |
| Banco de Dados | MySQL |
| Containers | Docker Compose (dev), Docker (prod) |
| CI/CD | GitHub Actions |

## Decisões Arquiteturais Core

### Prioridade das Decisões

**Decisões Críticas (Bloqueiam Implementação):**
D1 (Modelagem), D2 (Migrations), D4 (Auth), D7 (API Design), D9 (Kafka Topics), D12 (Estrutura Flutter)

**Decisões Importantes (Moldam Arquitetura):**
D3 (Cache), D5 (Autorização), D6 (Proteção API), D8 (Error Handling), D10 (HTTP Client), D13 (CI/CD), D14 (Observabilidade), D16 (Event-Driven UI)

**Decisões Adiáveis (Pós-MVP):**
D11 (Offline avançado), D15 (Deploy produção escalável)

### Arquitetura de Dados

**D1 - Modelagem de Dados:** Schema normalizado com views/queries otimizadas para leitura
- Transações do usuário em tabelas normalizadas
- Dados fundamentalistas de empresas em tabelas separadas
- Ranking de Graham calculado em batch e resultado armazenado (não on-the-fly)
- Rationale: Queries de ranking são batch semanais, não real-time

**D2 - Strategy de Migrations:** Liquibase (mantido do projeto existente)
- Já configurado em ambos os módulos (api, valuation-service)
- Integrado com native-image e Testcontainers
- Rationale: Já funcional, sem razão para migrar

**D3 - Caching:** Spring Cache com Caffeine (in-memory)
- TTL de 24h para rankings de Graham (recalculados semanalmente)
- Abstração Spring Cache permite migrar para Redis no futuro sem mudança de código
- Rationale: MVP não justifica overhead de Redis; Caffeine é trivial de testar

### Autenticação & Segurança

**D4 - Autenticação:** Google Sign-In via Firebase Auth
- Frontend: Flutter `google_sign_in` + `firebase_auth`
- Backend: Validação de Google ID Token via `spring-security-oauth2-resource-server`
- Auto-provisioning de usuário no primeiro acesso
- Sem endpoints `/auth/register`, `/auth/login`, `/auth/refresh`
- Refresh token gerenciado pelo Firebase SDK
- Rationale: Zero gestão de credentials, UX simplificada, sem backend de auth

**D5 - Autorização:** RBAC simples com Google Auth
- Identificação: `sub` claim do Google ID Token mapeado para `userId` interno
- Roles: `USER` (padrão) e `ADMIN` (configurado manualmente no banco)
- Isolamento de dados: Toda query filtra por `userId` extraído do token
- Spring Security: `oauth2ResourceServer` com JWT decoder do Google
- Rationale: MVP com tipo único de usuário, sem necessidade de ACL complexa

**D6 - Proteção de API:** Rate limiting com Bucket4j
- Actuator exposto apenas `/actuator/health` e `/actuator/info` sem autenticação
- Demais endpoints Actuator protegidos
- CORS já configurado no project-context
- Rationale: Proteção básica contra abuso sem complexidade

### API & Comunicação

**D7 - API Design:** REST com convenções padronizadas
- Versionamento por path: `/api/v1/`
- Formato de sucesso: `{ data: {...}, meta: {...} }`
- Naming: kebab-case para URLs, camelCase para campos JSON
- Rationale: Padrão maduro, bem suportado por Spring Boot

**D8 - Error Handling:** RFC 7807 Problem Details
- Nativo no Spring Framework 6 / Spring Boot 3.x
- Config: `spring.mvc.problemdetails.enabled=true`
- `ProblemDetail` class + `ErrorResponse` interface
- Rationale: Padrão HTTP standard, built-in no framework

**D9 - Tópicos Kafka:**

| Tópico | Chave de Partição | Propósito |
|---|---|---|
| `file-uploaded` | `userId` | Trigger de processamento de arquivo |
| `trade-extracted` | `userId:ticker` | Trades individuais extraídos |
| `trade-extracted-dlq` | — | Dead letter para falhas de extração |
| `valuation-requested` | `ticker` | Solicitação de cálculo de ranking |
| `valuation-completed` | `ticker` | Resultado do ranking |

### Arquitetura Frontend

**D10 - HTTP Client:** Dio
- Interceptors para JWT refresh automático (Firebase), logging, retry
- Upload com progress callback para arquivos B3
- Rationale: Mais robusto que http package para necessidades do projeto

**D11 - Cache Offline:** Mínimo para MVP
- SharedPreferences para token/preferences
- Dio cache interceptor para cache HTTP de rankings
- Sem SQLite/Hive — dados principais vêm do backend
- Rationale: Complexidade adiada; cache HTTP cobre casos básicos

**D12 - Estrutura Flutter:** Feature-first com shared core
```
lib/
  core/         # theme, routing, dio client, models comuns
  features/
    auth/       # login via Google
    upload/     # upload B3
    portfolio/  # dashboard, posições
    ranking/    # ranking Graham
```
- Cada feature: `screens/`, `widgets/`, `providers/`, `services/`
- Design tokens no `core/theme/` (Navy Blue, Emerald Green, Amber Gold)

### Infraestrutura & Deploy

**D13 - CI/CD:** GitHub Actions
- Workflow `build-test` on PR
- Workflow `deploy` on merge to main
- Rationale: Repo já no GitHub, zero infra adicional

**D14 - Observabilidade:** Spring Boot Actuator + Micrometer
- Logs estruturados com Logback JSON encoder
- Logs centralizados via Docker Compose logging para MVP
- Prometheus/Grafana quando escalar
- Rationale: Built-in no Spring Boot, evolução incremental

**D15 - Deploy Produção:** Docker Compose em VM para MVP
- `docker-compose.prod.yml` com env vars via `.env`
- Multi-stage Dockerfile para builds otimizados
- Flutter web servido via Nginx
- Migração futura para ECS/EKS ou Cloud Run se necessário
- Rationale: Simples, barato, suficiente para validação de produto

**D16 - Event-Driven UI (Push Backend → Flutter):** Server-Sent Events (SSE) via Spring
- **Protocolo:** SSE (`text/event-stream`) usando `SseEmitter` do Spring MVC
- **Endpoint:** `GET /api/v1/events/stream` — autenticado via JWT, mantém conexão aberta por usuário
- **Fluxo:** Kafka consumer (`api` module) recebe `trade-extracted` → publica no `SseEmitter` do usuário correspondente
- **Eventos publicados:**
  - `upload-progress` — progresso do processamento do arquivo B3 (Stories 2.1, 2.2)
  - `upload-completed` — conclusão com resumo (novas operações, duplicatas) (Story 2.3, FR8)
  - `portfolio-updated` — sinal de recarga do dashboard após processamento (Story 3.5)
- **Reconexão automática:** O cliente Flutter (via Dio + EventSource polyfill ou `http` package com stream) deve implementar retry automático com backoff de 5s ao detectar desconexão
- **Isolamento por usuário:** Um `SseEmitter` por sessão autenticada, mapeado por `userId` em `ConcurrentHashMap` no Spring. Sem vazamento entre tenants.
- **Timeout:** Emitter com timeout de 10 minutos (`SseEmitter(10 * 60 * 1000L)`); Flutter reconecta automaticamente
- **Fallback:** Se SSE não disponível (rede corporativa bloqueando streaming), Flutter faz polling do endpoint `GET /api/v1/imports/{fileId}/status` a cada 3s até status terminal
- **Não substituir Kafka:** SSE é apenas a "última milha" de notificação frontend. O processamento real continua via Kafka (D9). SSE lê o resultado final e propaga ao cliente.
- Rationale: SSE é unidirecional (server → client), sem overhead de WebSocket bidirecional. Suficiente para os casos de uso de notificação de progresso e atualização de dashboard. Suportado nativamente pelo Spring MVC sem dependência adicional.

### Análise de Impacto das Decisões

**Sequência de Implementação:**
1. D2 (Migrations) + D1 (Schema) → Base de dados
2. D4 (Google Auth) + D5 (Autorização) → Segurança
3. D7 (REST) + D8 (Error Handling) → API foundation
4. D9 (Kafka Topics) → Event infrastructure
5. D16 (SSE Event-Driven UI) → Push de eventos para o Flutter
6. D12 (Flutter structure) + D10 (Dio) → Frontend foundation
7. D13 (GitHub Actions) → CI/CD

**Dependências entre Decisões:**
- D4 (Google Auth) → D5 (Autorização depende do modelo de auth)
- D9 (Kafka Topics) → D1 (Schema precisa refletir eventos)
- D9 (Kafka Topics) → D16 (SSE consome resultados do Kafka)
- D12 (Flutter structure) → D10 (Dio configurado no core/)
- D12 (Flutter structure) → D16 (cliente SSE configurado no core/api/)
- D3 (Cache) → D1 (Cache de rankings depende do schema)

## Padrões de Implementação & Regras de Consistência

### Clean Architecture — Padrão do Projeto

O projeto segue Clean Architecture com separação em camadas rigorosa. O módulo `common` (Maven) contém domain entities, ports (interfaces), use cases e application services — compartilhados entre `api` e `valuation-service`.

**Regra de Dependência:**
```
domain/ → Nenhuma dependência externa (puro Java)
     ↑
application/ → Depende de domain/ (e Lombok)
     ↑
infrastructure/ → Depende de application/ + frameworks (Spring, JPA, Kafka)
     ↑
web/ → Depende de application/ (invoca Use Cases)
```

**Estrutura de Packages por Feature:**

| Camada | Package | Responsabilidade | Dependências |
|---|---|---|---|
| Domain | `{feature}.domain.entities` | Value Objects, Entities puras | Nenhuma |
| Domain | `{feature}.domain.repository` | Port interfaces | Domain entities |
| Application | `{feature}.application.usecase` | Use Cases (1 por operação) | Domain, ports |
| Application | `{feature}.application.service` | Orquestração | Domain, ports |
| Application | `{feature}.application.repository` | Port interfaces (DB) | Domain entities |
| Application | `{feature}.application.dto` | DTOs de entrada/saída | Nenhuma |
| Infrastructure | `{feature}.infrastructure.persistence` | Adapters JPA | Application ports |
| Infrastructure | `{feature}.infrastructure.kafka` | Adapters Kafka | Application ports |
| Infrastructure | `{feature}.infrastructure.spring` | DI Configs (`@Bean`) | Tudo (wiring) |
| Web | `{feature}.web` | REST Controllers | Application Use Cases/DTOs |

**Convenções de Use Cases:**
- Naming: `{Verbo}{Substantivo}UseCase` → `UploadFinancialDataUseCase`
- Método único: `execute(...)` — Command Pattern
- Sem anotações Spring (POJO puro, wired via `@Configuration`)
- `@RequiredArgsConstructor` + `@Slf4j` (Lombok)

**Convenções de Infrastructure:**
- `{Entidade}RepositoryImpl` → adapter que implementa port
- `{Feature}ServiceConfiguration` → `@Configuration` que cria beans
- JPA entities em `infrastructure/persistence/jpa/entities/`
- JPA repositories em `infrastructure/persistence/jpa/repository/`

### Padrões de Naming

**Banco de Dados (MySQL):**
- Tabelas: `snake_case` plural → `users`, `trade_transactions`, `company_rankings`
- Colunas: `snake_case` → `user_id`, `created_at`, `ticker_symbol`
- Foreign keys: `fk_{origem}_{destino}` | Índices: `idx_{tabela}_{colunas}`
- Constraints unique: `uq_{tabela}_{coluna}`

**API REST:**
- Endpoints: `kebab-case` plural → `/api/v1/trade-transactions`
- Path params: `{camelCase}` → `/api/v1/portfolios/{portfolioId}`
- Query params: `camelCase` → `?pageSize=20&sortBy=ticker`

**Código Java:**
- Classes: `PascalCase` | Métodos/variáveis: `camelCase`
- Packages: `afsdigital.grahamselect.{módulo}.{camada}.{feature}`
- Constantes: `UPPER_SNAKE_CASE`

**Código Dart (Flutter):**
- Classes: `PascalCase` | Arquivos: `snake_case.dart`
- Variáveis/métodos: `camelCase` | Constantes: `kCamelCase`

**Kafka:**
- Tópicos: `kebab-case` → `file-uploaded`, `trade-extracted-dlq`
- Consumer groups: `{módulo}-group` → `api-group`, `valuation-service-group`
- Eventos (classes): `PascalCase` → `FileUploadedEvent`

### Padrões de Formato

**API Responses:**
- Sucesso (item): `{ "data": { ... } }`
- Sucesso (lista): `{ "data": [...], "meta": { "page": 1, "pageSize": 20, "total": N } }`
- Erro: RFC 7807 ProblemDetail (built-in Spring Boot 3.x)

**Dados:**
- JSON fields: `camelCase`
- Datas: ISO 8601 `"2026-03-12T08:00:00Z"`
- Monetários: `BigDecimal` (Java) → `String` (JSON)
- Nulls: Omitidos (Jackson `NON_NULL`)

### Padrões de Comunicação

**Eventos Kafka — Payload obrigatório:**
- `eventId` (UUID), `timestamp` (ISO 8601), `userId`, `version` ("v1")
- Serialização: JSON com Jackson
- Sem PII direto — usar `userId` interno

**Estado Flutter (Provider):**
- Um Provider por feature
- Estados: `initial`, `loading`, `loaded`, `error`
- Imutabilidade via getters

### Padrões de Processo

**Error Handling Backend:**
- Exceções de domínio: `{Feature}Exception`
- Global handler: `@ControllerAdvice` com `ProblemDetail`
- Kafka: DLQ após 3 retries com backoff exponencial

**Error Handling Flutter:**
- Try-catch em services (nunca em widgets)
- Provider expõe `errorMessage`
- Snackbar para recuperáveis, tela de erro para fatais

### Regras Obrigatórias para Agentes de IA

1. **SEMPRE** respeitar a regra de dependência de camadas (domain ← application ← infrastructure)
2. **NUNCA** importar frameworks (Spring, JPA) em classes de domain/ ou application/usecase/
3. **SEMPRE** criar Use Case como POJO com método `execute()`
4. **NUNCA** expor exceções internas na API — usar ProblemDetail
5. **SEMPRE** incluir `eventId` e `timestamp` em eventos Kafka
6. **NUNCA** usar `float`/`double` para valores monetários
7. **SEMPRE** filtrar por `userId` em queries — isolamento mandatório
8. **NUNCA** logar dados sensíveis
9. **SEMPRE** criar teste unitário para nova lógica de negócio

## Estrutura do Projeto & Boundaries

### Estrutura Completa

```
graham-select/
├── docker-compose.yml                  # Kafka, Zookeeper, MySQL, services
├── start-app.sh                        # Script de inicialização
├── .github/workflows/
│   ├── build.yml                       # CI: build + test on PR
│   ├── deploy-backend.yml              # CD: deploy backend
│   └── frontend-ci.yml                 # CI: Flutter checks
│
├── backend/                            # Multi-module Maven
│   ├── pom.xml                         # Parent POM
│   ├── common/                         # Domain + Application (shared)
│   │   └── src/main/java/afsdigital/grahamselect/
│   │       ├── common/domain/entities/          # Entities compartilhadas
│   │       ├── {feature}/domain/repository/     # Port interfaces
│   │       └── {feature}/application/
│   │           ├── usecase/                     # Use Cases (POJOs)
│   │           ├── service/                     # Services de orquestração
│   │           ├── repository/                  # Port interfaces (DB)
│   │           ├── dto/                         # DTOs
│   │           └── service/exceptions/          # Exceções de domínio
│   ├── api/                            # REST API + Infrastructure
│   │   └── src/main/java/afsdigital/grahamselect/api/
│   │       ├── config/                          # WebConfig, SecurityConfig
│   │       └── {feature}/
│   │           ├── web/                         # Controllers/Delegates
│   │           └── infrastructure/
│   │               ├── spring/                  # @Configuration wiring
│   │               ├── kafka/                   # Kafka producers
│   │               ├── file/                    # File I/O adapters
│   │               └── persistence/jpa/         # JPA entities + repos
│   └── valuation-service/              # Kafka Consumer + Cálculos
│       └── src/main/java/afsdigital/grahamselect/valuation/
│           └── infrastructure/
│               ├── spring/                      # Configs
│               ├── kafka/                       # Consumers
│               └── persistence/jpa/             # Entities + repos
│
└── frontend/                           # Flutter (Clean Architecture)
    ├── pubspec.yaml
    └── lib/src/
        ├── core/
        │   ├── api/                             # Dio client config
        │   ├── theme/                           # Design tokens
        │   └── utils/
        └── features/
            └── {feature}/
                ├── data/
                │   ├── datasources/              # Remote data sources
                │   ├── models/                   # Data models (JSON)
                │   └── repositories/             # Repository implementations
                ├── domain/
                │   ├── entities/                 # Domain entities
                │   ├── repositories/             # Repository interfaces
                │   └── usecases/                 # Use cases
                └── presentation/
                    ├── pages/                    # Screens
                    ├── providers/                # State management
                    └── widgets/                  # UI components
```

### Boundaries Arquiteturais

**Módulos Maven — Boundary por Deploy:**

| Módulo | Responsabilidade | Comunicação |
|---|---|---|
| `common` | Domain + Application (POJOs) | Dependência Maven |
| `api` | REST API + Infra producers | HTTP (REST) + Kafka producer |
| `valuation-service` | Consumer + cálculos | Kafka consumer + MySQL |

**Fluxo de Dados Principal:**
```
Flutter ──HTTP──► api ──Kafka──► valuation-service ──MySQL──► Rankings
Flutter ──HTTP──► api ──MySQL──► Query Rankings
```

### Mapeamento Features → Estrutura

| Feature | Backend Package | Flutter Feature | Status |
|---|---|---|---|
| Upload B3 | `upload/` | `features/upload/` | ✅ Existente |
| Ranking Graham | `valuation/` | `features/ranking/` | ✅ Existente |
| Auth Google | `config/` (Security) | `features/auth/` | 🆕 A criar |
| Portfolio | `portfolio/` | `features/portfolio/` | 🆕 A criar |
| Transações | `trade/` | `features/portfolio/` | 🆕 A criar |

### Integrações Externas

| Serviço | Propósito | Ponto de Integração |
|---|---|---|
| Google Sign-In / Firebase Auth | Autenticação | Flutter SDK + Backend token validation |
| B3 (CEI) | Upload de relatórios | File upload via API REST |
| MySQL | Persistência | JPA via Spring Data |
| Apache Kafka | Eventos assíncronos | Spring Kafka |

## Validação da Arquitetura

### Validação de Coerência — ✅ APROVADA

**Compatibilidade de Decisões:**
- Java 21 + Spring Boot 3.4.13: suporte completo a virtual threads, records, ProblemDetail
- Liquibase + MySQL: validado com Testcontainers no projeto existente
- Google Auth + `spring-security-oauth2-resource-server`: integração nativa
- Kafka + Spring Kafka: consumer/producer com serialização Jackson
- Flutter Clean Architecture + Dio + Provider: stack consistente

**Consistência de Padrões:**
- Clean Architecture aplicada igualmente em backend Java e frontend Dart
- Naming conventions não conflitam entre camadas
- Use Cases como POJOs em ambos os lados

### Validação de Cobertura de Requisitos — ✅ APROVADA

| Requisito | Decisão | Status |
|---|---|---|
| Upload Excel B3 (até 50MB) | D9 (Kafka Splitter) | ✅ |
| Consolidação multi-corretora | D1 (Schema normalizado) | ✅ |
| Ranking Graham (7 critérios) | D3 (Cache), D1 (batch) | ✅ |
| Dashboard mobile-first | D12 (Flutter), D10 (Dio) | ✅ |
| Autenticação segura | D4 (Google Auth) | ✅ |
| Isolamento de dados | D5 (userId filter) | ✅ |
| Performance < 30s/1000 txns | D9 (Kafka particionado) | ✅ |
| Resiliência (retry, DLQ) | D9 (DLQ), padrões processo | ✅ |
| Observabilidade | D14 (Actuator + Micrometer) | ✅ |
| LGPD | D5 (isolamento), sem PII Kafka | ✅ |
| WCAG 2.1 AA | UX spec definido | ✅ |

### Validação de Prontidão — ✅ APROVADA

- [x] 16/16 decisões documentadas (D1–D16)
- [x] Padrões de naming completos (DB, API, Java, Dart, Kafka)
- [x] Clean Architecture mapeada com tabela de camadas
- [x] Boundaries entre módulos Maven definidos
- [x] 5 features mapeadas para packages/directories
- [x] 9 regras mandatórias para agentes de IA
- [x] Protocolo de Event-Driven UI definido (D16 — SSE)

### Gap Analysis

**Sem gaps críticos.** Gaps menores (não bloqueiam MVP):
1. S3/MinIO para storage — volume Docker local no MVP
2. Prometheus/Grafana — Actuator cobre MVP
3. ECS/EKS deployment — Docker Compose VM suficiente

### Avaliação Final

**Status:** PRONTA PARA IMPLEMENTAÇÃO 🟢

**Nível de Confiança:** ALTO

**Pontos Fortes:**
- Arquitetura já parcialmente implementada e validada
- Clean Architecture consistente em backend E frontend
- Decisões pragmáticas para MVP com path de evolução claro

**Prioridade de Implementação:**
1. D4/D5 (Google Auth) → Foundation de segurança
2. D1/D2 (Schema + Migrations) → Novas tabelas para portfolio/trades
3. Nova feature `auth/` → Flutter + Backend
4. Nova feature `portfolio/` → Dashboard consolidado
