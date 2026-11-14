---
baseline_commit: 810b4897270b9e94ec9e7789e81e3c67e3394669
---

# Story 7.1: Vínculo de Novo CPF à Conta Matriz

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** usuário Premium e gestor familiar,
**I want** vincular o CPF de um dependente à minha conta matriz,
**So that** eu possa gerenciar os investimentos da família centralizadamente e importar extratos B3 específicos para cada perfil.

---

## Acceptance Criteria

1. **AC1 - Controle de Acesso Premium (`@RequirePremium`):**
   - **Given** um usuário autenticado com plano `FREE` (ou `TRIAL` expirado)
   - **When** ele tenta listar, criar ou remover perfis familiares em `/api/v1/family-profiles/**` ou enviar um upload B3 informando `profileId`
   - **Then** o backend rejeita a requisição com HTTP `403 Forbidden` no formato RFC 7807 (`application/problem+json`) via `PremiumFeatureAccessGuard` / `PremiumFeatureAccessDeniedHandler`
   - **And** requisições não autenticadas retornam HTTP `401 Unauthorized`.

2. **AC2 - Vínculo de Novo CPF como Sub-Tenant Lógico (`POST /api/v1/family-profiles`):**
   - **Given** um usuário autenticado com plano `PREMIUM` ou `TRIAL` ativo
   - **When** ele envia `POST /api/v1/family-profiles` com `cpf` (string numérica ou formatada com 11 dígitos válidos), `name` (obrigatório, 2 a 120 caracteres) e `relationship` (opcional, até 50 caracteres, ex.: `"Cônjuge"`, `"Filho(a)"`)
   - **Then** o sistema sanitiza o CPF (apenas 11 dígitos), valida os dígitos verificadores (rejeitando sequências repetidas como `11111111111` ou CPFs inválidos com HTTP `400 Bad Request` RFC 7807)
   - **And** verifica se o CPF já está vinculado à mesma conta matriz (`master_user_id = jwt.getSubject()`), retornando HTTP `409 Conflict` RFC 7807 em caso de duplicidade
   - **And** persiste o sub-tenant lógico na tabela `family_profiles` com `id` (UUID `CHAR(36)`), `master_user_id`, `cpf_digits`, `name`, `relationship`, `is_active = true`, `created_at` (UTC)
   - **And** retorna HTTP `201 Created` com `{ "data": { "id", "name", "cpfMasked", "relationship", "active", "createdAt" } }`, onde `cpfMasked` mascara o CPF no padrão `***.XXX.XXX-**` (sem nunca expor o CPF completo em logs).

3. **AC3 - Listagem e Remoção de Perfis Vinculados (`GET` e `DELETE /api/v1/family-profiles`):**
   - **Given** um usuário Premium autenticado
   - **When** ele chama `GET /api/v1/family-profiles`
   - **Then** o sistema retorna HTTP `200 OK` com `{ "data": [...], "meta": { "total": N } }` contendo apenas os perfis ativos pertencentes ao `master_user_id` do usuário autenticado (`jwt.getSubject()`), ordenados por `created_at` ascendente
   - **When** ele chama `DELETE /api/v1/family-profiles/{profileId}`
   - **Then** o sistema valida que o perfil pertence ao `master_user_id` autenticado (retornando `404 Not Found` se não existir ou pertencer a outro usuário) e realiza a desativação/remoção segura do vínculo, retornando `204 No Content`.

4. **AC4 - Upload de Extratos B3 Específicos por Perfil (`POST /api/v1/upload/b3` + Kafka):**
   - **Given** um usuário com ao menos um perfil familiar vinculado (ou conta titular)
   - **When** ele realiza upload em `POST /api/v1/upload/b3` informando o parâmetro opcional `profileId` (via multipart form field)
   - **Then** se `profileId` for informado:
     1. O controller valida acesso Premium (`PremiumFeatureAccessGuard`) e valida que `profileId` existe, está ativo e pertence ao `masterUserId` (`jwt.getSubject()`), retornando `404`/`403` em caso contrário;
     2. O `profileId` é persistido em `b3_import_status.profile_id` e propagado nos eventos Kafka `FileUploadedEvent` (`file-uploaded`), `TradeExtractedEvent` (`trade-extracted`) e `TradeExtractionFailedEvent` (`trade-extracted-dlq`);
     3. O `KafkaTradeExtractedConsumer` persiste os trades na tabela `trades` com `user_id = event.userId()` (ID mestre) e `profile_id = event.profileId()`, permitindo que o dashboard consolidado continue exibindo o patrimônio total da família e diferenciando a origem por sub-tenant;
     4. A deduplicação de trades (`TradePort.exists`) considera `(userId, profileId, ticker, tradeDate, quantity, price, broker, side)` para não colidir operações idênticas realizadas em CPFs diferentes;
   - **And** se `profileId` não for informado (ou vazio), o upload continua funcionando normalmente para a conta titular (`profile_id = null`), mantendo 100% de retrocompatibilidade.

5. **AC5 - Interface Flutter Web para Holding Familiar e Upload por Perfil:**
   - **Given** que o usuário está autenticado no Flutter Web
   - **When** ele acessa a seção "Holding Familiar (Multi-CPF)" a partir da `ProfilePage` (`Key('profile_family_holding_button')`) ou navega para `/family-profiles`
   - **Then** a `FamilyProfilesPage` exibe:
     - Lista de perfis vinculados (Nome, CPF mascarado, Parentesco, botão remover `Key('family_profile_delete_button_${id}')`) e estado vazio amigável (`Key('family_profiles_empty_state')`)
     - Botão "Vincular Novo CPF" (`Key('family_profile_add_button')`) abrindo modal/formulário com Nome (`Key('family_profile_name_input')`), CPF (`Key('family_profile_cpf_input')`), Parentesco (`Key('family_profile_relationship_input')`) e Salvar (`Key('family_profile_save_button')`)
     - Tratamento amigável para usuários `FREE` (`403 Forbidden`) exibindo banner/estado de recurso exclusivo Premium sem quebrar a tela
   - **And** na `UploadPage`, quando o usuário possui perfis vinculados, é exibido o seletor "Perfil / CPF de Destino" (`Key('upload_profile_selector')`) com opção padrão `"Conta Titular (Minha Carteira)"` (`null`) e os perfis vinculados, enviando `profileId` no upload multipart quando selecionado.

6. **AC6 - Logs Estruturados, Padrão UTC, Migrations e Qualidade (`AGENTS.md` & `project-context.md`):**
   - **Given** todas as operações de backend e frontend desta história
   - **When** endpoints REST ou consumers Kafka são executados
   - **Then** há logs `INFO` nas entradas com `userId` e `profileId` (nunca logar CPF completo) e logs `ERROR` com stack trace nos blocos `catch`
   - **And** todas as datas/horas utilizam `ZoneOffset.UTC`
   - **And** a migration Liquibase `29-create-family-profiles-and-link-trades.yaml` é criada como nova migration sem alterar changelogs anteriores
   - **And** `mvn clean test` (backend) e `flutter analyze` + `flutter test` (frontend) passam 100%.

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domínio, Portas, Casos de Uso e Liquibase)

- [x] **Task 1: Migration Liquibase `29-create-family-profiles-and-link-trades.yaml`** (AC: #2, #4, #6)
  - [x] 1.1 Criar `backend/common/src/main/resources/db/changelog/29-create-family-profiles-and-link-trades.yaml`:
    - Criar tabela `family_profiles`:
      - `id` (`CHAR(36)`, PK, NOT NULL)
      - `master_user_id` (`VARCHAR(255)`, NOT NULL)
      - `cpf_digits` (`VARCHAR(11)`, NOT NULL)
      - `name` (`VARCHAR(120)`, NOT NULL)
      - `relationship` (`VARCHAR(50)`, NULLABLE)
      - `is_active` (`BOOLEAN`, NOT NULL, default `true`)
      - `created_at` (`DATETIME`, NOT NULL)
      - `updated_at` (`DATETIME`, NULLABLE)
    - Adicionar `uniqueConstraint` `uq_family_profiles_master_cpf` em `(master_user_id, cpf_digits)` e índice `idx_family_profiles_master_user` em `(master_user_id)`.
    - Adicionar coluna `profile_id` (`CHAR(36)`, NULLABLE) na tabela `b3_import_status` com índice `idx_b3_import_status_profile`.
    - Adicionar coluna `profile_id` (`CHAR(36)`, NULLABLE) na tabela `trades` com índice `idx_trades_user_profile` em `(user_id, profile_id)`.
    - Atualizar a unique constraint `uq_trade_deduplication` na tabela `trades` (`dropUniqueConstraint` + `addUniqueConstraint`) para `(user_id, profile_id, ticker, trade_date, quantity, price, broker, side)`, evitando que operações idênticas em CPFs diferentes colidam no banco.
  - [x] 1.2 Registrar `db/changelog/29-create-family-profiles-and-link-trades.yaml` ao final de `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (sem modificar nenhum arquivo de migration anterior).

- [x] **Task 2: Domínio e Casos de Uso de `family` no módulo `common`** (AC: #2, #3, #6)
  - [x] 2.1 Criar entidade de domínio pura `FamilyProfile` em `backend/common/src/main/java/afsdigital/grahamselect/common/family/domain/entities/FamilyProfile.java` (POJO sem dependências Spring/JPA) e utilitário/value-object de validação e mascaramento de CPF (`CpfValidator` com cálculo dos 2 dígitos verificadores + máscara `***.XXX.XXX-**`).
  - [x] 2.2 Criar porta `FamilyProfilePort` em `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/repository/FamilyProfilePort.java`:
    - `save(FamilyProfile)`
    - `findActiveByMasterUserId(String masterUserId)`
    - `findActiveByIdAndMasterUserId(UUID id, String masterUserId)`
    - `findByMasterUserIdAndCpfDigits(String masterUserId, String cpfDigits)` (inclui inativos para suportar reativação segura sem violar `uq_family_profiles_master_cpf`)
  - [x] 2.3 Criar exceções de domínio em `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/service/exceptions/`:
    - `InvalidCpfException` (mapeada para `400 Bad Request`)
    - `DuplicateFamilyProfileException` (mapeada para `409 Conflict`)
    - `FamilyProfileNotFoundException` (mapeada para `404 Not Found`)
  - [x] 2.4 Criar Use Cases POJOs com método `execute(...)` em `common/family/application/usecase/`:
    - `LinkFamilyProfileUseCase` (se `findByMasterUserIdAndCpfDigits` encontrar perfil ativo → lança `DuplicateFamilyProfileException`; se encontrar perfil inativo → reativa `isActive = true` atualizando `name`, `relationship` e `updatedAt` UTC)
    - `ListFamilyProfilesUseCase`
    - `UnlinkFamilyProfileUseCase` (marca `isActive = false` e `updatedAt = LocalDateTime.now(ZoneOffset.UTC)`).
  - [x] 2.5 Criar testes unitários JUnit 5 para `CpfValidator` e os 3 Use Cases em `backend/common/src/test/...`.

- [x] **Task 3: Evolução dos Eventos Kafka, Entidades e Use Cases de Upload/Trade no `common`** (AC: #4, #6)
  - [x] 3.1 Adicionar campo `String profileId` (opcional/nullable) em `FileUploadedEvent`, `TradeExtractedEvent` e `TradeExtractionFailedEvent`.
  - [x] 3.2 Adicionar campo `String profileId` em `Trade` (`common/portfolio/domain/entities/Trade.java`) e `B3ImportStatus` (`common/upload/domain/model/B3ImportStatus.java`).
  - [x] 3.3 Atualizar `UploadB3FileUseCase` e `B3ImportStatusPort` com overload aceitando `String profileId` (mantendo assinatura antiga delegando com `null`).
  - [x] 3.4 Atualizar `ProcessB3FileUseCase` para propagar `event.profileId()` para `TradeExtractedEvent` e `TradeExtractionFailedEvent`.
  - [x] 3.5 Atualizar `TradePort` com `exists(String userId, String profileId, String ticker, LocalDate tradeDate, BigDecimal quantity, BigDecimal price, String broker, String side)` (mantendo overload sem `profileId` delegando com `null`) e atualizar `SaveTradeUseCase` para invocar `tradePort.exists` passando `trade.getProfileId()`.

### Backend — Módulo `api` (Persistência JPA, REST Controllers, Segurança e Consumers)

- [x] **Task 4: Persistência JPA e Wiring de `family` no módulo `api`** (AC: #2, #3)
  - [x] 4.1 Criar `FamilyProfileEntity` (`@Entity`, `@Table(name = "family_profiles")`, com `@JdbcTypeCode(SqlTypes.VARCHAR)` no campo `UUID id` para compatibilidade `CHAR(36)` no MySQL), `JpaFamilyProfileRepository` e `FamilyProfileJpaAdapter` em `api/family/infrastructure/persistence/jpa/`.
  - [x] 4.2 Criar `FamilyProfileConfiguration` em `api/family/infrastructure/spring/` registrando os beans dos Use Cases.
  - [x] 4.3 Atualizar `B3ImportStatusEntity`, `B3ImportStatusRepositoryAdapter`, `TradeEntity`, `JpaTradeRepository` e `TradeJpaAdapter` para mapear `profile_id` e suportar a checagem de duplicidade considerando `profileId` (`profileId == null` → `profileId IS NULL`, senão `profileId = :profileId`).
  - [x] 4.4 Atualizar `KafkaTradeExtractedConsumer` para propagar `event.profileId()` ao construir `Trade`.

- [x] **Task 5: `FamilyProfileController`, Atualização de `B3UploadController` e `GlobalExceptionHandler`** (AC: #1, #2, #3, #4, #6)
  - [x] 5.1 Criar `FamilyProfileController` em `api/family/web/FamilyProfileController.java` anotado com `@RestController`, `@RequestMapping("/api/v1/family-profiles")` e `@RequirePremium`:
    - `GET /api/v1/family-profiles` → lista perfis ativos de `jwt.getSubject()` com `{ "data": [...], "meta": { "total": N } }`
    - `POST /api/v1/family-profiles` → recebe `CreateFamilyProfileRequest(cpf, name, relationship)` com `@Valid`, retorna `201 Created` com `{ "data": FamilyProfileResponse }`
    - `DELETE /api/v1/family-profiles/{profileId}` → desvincula perfil de `jwt.getSubject()`, retorna `204 No Content`
    - Logs `INFO` nas entradas e `ERROR` nos `catch` (sem logar CPF completo).
  - [x] 5.2 Atualizar `B3UploadController`:
    - Aceitar `@RequestParam(value = "profileId", required = false) String profileId` em `POST /api/v1/upload/b3` e adicionar log `INFO` na entrada com `userId` e `profileId`
    - Se `profileId` não for nulo/branco: validar acesso Premium via `PremiumFeatureAccessGuard.canAccessPremiumFeatures()`, validar formato UUID (lançando `FamilyProfileNotFoundException` se inválido) e validar que o perfil existe e pertence a `jwt.getSubject()` via `FamilyProfilePort` (lançando `FamilyProfileNotFoundException` se não encontrado)
    - Incluir `profileId` em `B3ImportStatusView` no `GET /api/v1/upload/b3/status/{correlationId}`.
  - [x] 5.3 Registrar handlers em `GlobalExceptionHandler` para `InvalidCpfException` (`400`), `DuplicateFamilyProfileException` (`409`) e `FamilyProfileNotFoundException` (`404`) retornando `ProblemDetail` (RFC 7807).
  - [x] 5.4 Criar testes de integração (`*IT`) cobrindo `FamilyProfileController` (FREE -> `403`, PREMIUM/TRIAL -> `201`/`200`/`204`, CPF inválido -> `400`, CPF duplicado -> `409`, isolamento entre usuários -> `404`) e `B3UploadController` com `profileId`.

### Frontend — Flutter (Clean Architecture + Provider + GoRouter)

- [x] **Task 6: Feature `family_holding` e Integração em `ProfilePage` e `GoRouter`** (AC: #5, #6)
  - [x] 6.1 Criar camada `domain/` e `data/` em `frontend/lib/src/features/family_holding/`:
    - Entidade `FamilyProfileEntity` (`id`, `name`, `cpfMasked`, `relationship`, `active`, `createdAt`)
    - `FamilyProfileModel` (`fromJson`)
    - `FamilyProfileRemoteDataSource` usando `ApiClient` (Dio, paths `/api/v1/family-profiles`), tratando `DioException` com status `403` via exceção específica `PremiumRequiredException` e parseando `detail` de `ProblemDetail` RFC 7807
    - `FamilyProfileRepository`
  - [x] 6.2 Criar `FamilyProfileProvider` em `presentation/providers/family_profile_provider.dart` com estados `initial`, `loading`, `loaded`, `error`, `premiumRequired` e métodos `fetchProfiles()`, `linkProfile(...)`, `unlinkProfile(...)`.
  - [x] 6.3 Criar `FamilyProfilesPage` em `presentation/pages/family_profiles_page.dart` com todas as `Key`s definidas no AC5, máscara/validação básica de CPF no formulário, banner de upgrade quando `premiumRequired`, e SnackBars de feedback.
  - [x] 6.4 Registrar `FamilyProfileProvider` no `MultiProvider` e adicionar rota `/family-profiles` no `GoRouter` em `frontend/lib/main.dart`, além de adicionar botão/card de acesso na `ProfilePage` (`Key('profile_family_holding_button')`) navegando via `context.push('/family-profiles')`.

- [x] **Task 7: Seletor de Perfil/CPF na `UploadPage`** (AC: #4, #5)
  - [x] 7.1 Atualizar `UploadRemoteDataSource.uploadB3File` para aceitar `{String? token, String? profileId}` e anexar `request.fields['profileId'] = profileId` quando não nulo/vazio.
  - [x] 7.2 Atualizar `UploadRepository`, `UploadB3FileUseCase` e `UploadProvider` para suportar `selectedProfileId`.
  - [x] 7.3 Converter `UploadPage` para `StatefulWidget` (para carregar `FamilyProfileProvider.fetchProfiles()` silenciosamente no `initState` se ainda estiver `initial`, ignorando `premiumRequired` para usuários `FREE`) e exibir `DropdownButtonFormField<String?>` (`Key('upload_profile_selector')`) quando houver perfis vinculados.
  - [x] 7.4 Criar testes de widget e provider para `FamilyProfilesPage`, `FamilyProfileProvider` e `UploadPage` com seletor de perfil; rodar `flutter analyze` e `flutter test`.

### Review Findings

- [x] [Review][Patch] Excluir `cpfDigits` do `toString()` em `FamilyProfile` e substituir `@Data` por `@Getter/@Setter/@ToString(exclude = "cpfDigits")` em `FamilyProfileEntity` [backend/common/.../FamilyProfile.java:11, backend/api/.../FamilyProfileEntity.java:19]
- [x] [Review][Patch] Adicionar `@Size(max = 20)` no campo `cpf` de `CreateFamilyProfileRequest` [backend/api/.../CreateFamilyProfileRequest.java:8]
- [x] [Review][Patch] Tratar `DataIntegrityViolationException` em `FamilyProfileJpaAdapter.save` convertendo para `DuplicateFamilyProfileException` (409 Conflict) [backend/api/.../FamilyProfileJpaAdapter.java:33]
- [x] [Review][Patch] Normalizar `profileId` em branco para `null` em `TradeJpaAdapter.save` [backend/api/.../TradeJpaAdapter.java:25]
- [x] [Review][Patch] Sincronizar `UploadProvider.selectedProfileId` quando o perfil selecionado é removido na `UploadPage` [frontend/lib/src/features/upload/presentation/pages/upload_page.dart:69]
- [x] [Review][Patch] Refatorar modal de vínculo em `FamilyProfilesPage` para `_AddProfileDialog` (`StatefulWidget`) garantindo `dispose()` dos controllers, bloqueio de duplo clique no botão Salvar e rejeição client-side de CPFs com dígitos repetidos [frontend/lib/src/features/family_holding/presentation/pages/family_profiles_page.dart:228]
- [x] [Review][Patch] Validar tipo de `response.data` antes do cast em `FamilyProfileRemoteDataSource` [frontend/lib/src/features/family_holding/data/datasources/family_profile_remote_datasource.dart:23]
- [x] [Review][Patch] Ampliar `FamilyProfileControllerIT` para cobrir `DELETE` 403 (FREE) e `profileId` UUID inválido (404) no upload B3 [backend/api/.../FamilyProfileControllerIT.java:208]
- [x] [Review][Defer] Implementar `Persistable<UUID>` em `FamilyProfileEntity` para evitar `SELECT` antes de `INSERT` no `save()` [backend/api/.../FamilyProfileEntity.java:23] — deferred, otimização de baixa prioridade

---

## Dev Notes

### Arquitetura Crítica de Identidade e Sub-Tenant Lógico (NÃO ERRAR)

1. **Dualidade `user.id` vs `jwt.getSubject()` no codebase atual:**
   - Em `OAuth2UserProvisioningConverter.java` (`backend/api/.../auth/infrastructure/security/OAuth2UserProvisioningConverter.java:71`), o `JwtAuthenticationToken` é construído com `principalName = user.getId().toString()` (`Long`).
   - `PremiumFeatureAccessGuard` e `UserController` usam `SecurityContextHolder.getContext().getAuthentication().getName()` (`Long userId`).
   - **PORÉM**, `B3UploadController`, `PortfolioController`, `TradeManualController`, `NotificationController`, `AllocationStrategyController`, `trades.user_id`, `b3_import_status.user_id`, `FileUploadedEvent.userId()` e `TradeExtractedEvent.userId()` usam **`jwt.getSubject()`** (`String`, Google `sub`)!
   - **Decisão desta Story:** `family_profiles.master_user_id` **DEVE** armazenar `jwt.getSubject()` (`String`) para manter consistência direta com `trades.user_id`, `b3_import_status.user_id` e todos os eventos Kafka! E `@RequirePremium` (`PremiumFeatureAccessGuard`) continua funcionando normalmente via `SecurityContextHolder` porque lê `authentication.getName()` (`user.getId()`).

2. **Retrocompatibilidade e Deduplicação de `profile_id`:**
   - Todos os trades e importações existentes pertencem à conta titular e têm `profile_id = NULL`.
   - A tabela `trades` possui a constraint `uq_trade_deduplication` (`16-add-side-to-trades.yaml`). A migration `29` **DEVE** recriá-la incluindo `profile_id` para que operações idênticas em CPFs diferentes não disparem `DataIntegrityViolationException` no MySQL. Como `NULL != NULL` em índices `UNIQUE` no MySQL, a checagem lógica em `SaveTradeUseCase` via `TradePort.exists(userId, profileId, ...)` permanece essencial para evitar duplicatas na conta titular (`profile_id IS NULL`).
   - `B3ImportStatus` é um `record` Java sem `@Builder` — ao adicionar `String profileId`, atualizar `B3ImportStatusRepositoryAdapter.toDomain(...)` e `B3ImportStatusView`.
   - Eventos Kafka `FileUploadedEvent`, `TradeExtractedEvent` e `TradeExtractionFailedEvent` podem ter `profileId = null` (inclusive mensagens antigas na DLQ!). Não exigir `@NotNull` na desserialização Jackson.
   - `PortfolioController` (`findAllByUserId(jwt.getSubject())`) já consolida automaticamente todos os trades do `userId` (titular + dependentes), mantendo o comportamento de visão consolidada da Holding Familiar.

3. **Liquibase & Configurações (Lições da Retrospectiva Epic 6):**
   - **NUNCA** editar changelogs Liquibase antigos (`01` a `28`); criar apenas `29-create-family-profiles-and-link-trades.yaml` e incluir em `db.changelog-master.yaml`.
   - Atenção ao `application.yml`: não depender de `@DynamicPropertySource` de teste para propriedades obrigatórias.
   - **LGPD:** Nunca logar `cpfDigits` em `INFO`/`DEBUG`/`ERROR`. Logar apenas `masterUserId`, `profileId` e, se necessário, `cpfMasked`.

4. **Padrões Obrigatórios:**
   - Java 21, `ZoneOffset.UTC` em todas as datas (`LocalDateTime.now(ZoneOffset.UTC)`, `OffsetDateTime.now(ZoneOffset.UTC)`).
   - Clean Architecture estrita: classes em `common/.../domain/` e `common/.../application/usecase/` são POJOs sem annotations Spring/JPA.
   - Responses REST: `{ "data": ... }` / `{ "data": [...], "meta": { ... } }`, erros via `ProblemDetail` (RFC 7807).
   - Branching: abrir branch `feature/7-1-vinculo-de-novo-cpf-a-conta-matriz` a partir da `main` atualizada antes de implementar.

### Arquivos Principais a Criar / Modificar

- **Criar (Backend):**
  - `backend/common/src/main/resources/db/changelog/29-create-family-profiles-and-link-trades.yaml`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/family/...` (`FamilyProfile`, `CpfValidator`, `FamilyProfilePort`, Use Cases e exceções)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/family/...` (`FamilyProfileEntity`, `JpaFamilyProfileRepository`, `FamilyProfileJpaAdapter`, `FamilyProfileConfiguration`, `FamilyProfileController`, DTOs)
- **Modificar (Backend):**
  - `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/events/{FileUploadedEvent,TradeExtractedEvent,TradeExtractionFailedEvent}.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/usecase/{UploadB3FileUseCase,ProcessB3FileUseCase}.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/repository/B3ImportStatusPort.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/domain/entities/Trade.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/repository/TradePort.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/SaveTradeUseCase.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/model/B3ImportStatus.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/{B3UploadController,B3ImportStatusView}.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/persistence/{B3ImportStatusRepositoryAdapter,jpa/entities/B3ImportStatusEntity}.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaTradeExtractedConsumer.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/{TradeJpaAdapter,entities/TradeEntity,repositories/JpaTradeRepository}.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java`
- **Criar (Frontend):**
  - `frontend/lib/src/features/family_holding/...` (clean architecture completa + `FamilyProfilesPage` + `FamilyProfileProvider`)
- **Modificar (Frontend):**
  - `frontend/lib/main.dart` (registrar `FamilyProfileProvider` e rota `/family-profiles`)
  - `frontend/lib/src/features/profile/presentation/pages/profile_page.dart` (link/botão para `/family-profiles`)
  - `frontend/lib/src/features/upload/{data/datasources/upload_remote_data_source.dart,data/repositories/upload_repository_impl.dart,domain/repositories/upload_repository.dart,domain/usecases/upload_b3_file_usecase.dart,presentation/providers/upload_provider.dart,presentation/pages/upload_page.dart}`

### References

- [Source: docs/bmad/planning-artifacts/epics.md#L733-L748] (Epic 7 / Story 7.1)
- [Source: docs/bmad/planning-artifacts/prd.md#L207] (FR4: Holding Familiar Multi-CPF)
- [Source: docs/bmad/planning-artifacts/architecture.md#L304-L421] (Clean Architecture, Naming, Kafka, REST, Error Handling)
- [Source: docs/bmad/implementation-artifacts/epic-6-retro-2026-11-13.md#L63-L69] (Preparação técnica para Epic 7 e lições de Liquibase)
- [Source: docs/bmad/project-context.md] (Java 21, ZoneOffset.UTC, Flutter Provider/GoRouter, Security Checklist)

---

## Dev Agent Record

### Agent Model Used

Gemini 2.5 Pro (Antigravity Dev Agent)

### Debug Log References

- Compilação do módulo `common` e `api-service` exige JDK >= 21 no `JAVA_HOME` devido à flag `-proc:full` do `maven-compiler-plugin` (executado com `openjdk-22.0.1`), mantendo compatibilidade de bytecode com `<release>17</release>`.
- Mocks do `mockito` no Flutter regenerados via `flutter pub run build_runner build --delete-conflicting-outputs` após adicionar o parâmetro opcional `{String? profileId}` em `UploadRemoteDataSource.uploadB3File`, `UploadRepository.uploadB3File` e `UploadB3FileUseCase.call`.

### Completion Notes List

- Migration Liquibase `29-create-family-profiles-and-link-trades.yaml` criada e registrada em `db.changelog-master.yaml`, criando `family_profiles`, adicionando `profile_id` em `b3_import_status` e `trades` e atualizando `uq_trade_deduplication`.
- Domínio `family` no módulo `common` implementado com `FamilyProfile`, `CpfValidator` (cálculo de dígitos verificadores + máscara LGPD `***.XXX.XXX-**`), `FamilyProfilePort`, exceções e Use Cases (`LinkFamilyProfileUseCase`, `ListFamilyProfilesUseCase`, `UnlinkFamilyProfileUseCase`).
- Eventos Kafka (`FileUploadedEvent`, `TradeExtractedEvent`, `TradeExtractionFailedEvent`), `Trade`, `B3ImportStatus`, `UploadB3FileUseCase`, `ProcessB3FileUseCase`, `TradePort` e `SaveTradeUseCase` evoluídos para propagar `profileId` mantendo retrocompatibilidade.
- Persistência JPA, `FamilyProfileController` (`@RequirePremium`, logs `INFO`/`ERROR` sem expor CPF), atualização do `B3UploadController` e `GlobalExceptionHandler` implementados no módulo `api`.
- Feature Flutter `family_holding` (`FamilyProfileEntity`, `FamilyProfileModel`, `FamilyProfileRemoteDataSource`, `FamilyProfileRepositoryImpl`, `FamilyProfileProvider`, `FamilyProfilesPage`), integração na `ProfilePage`/`GoRouter` e seletor `Key('upload_profile_selector')` na `UploadPage` implementados.
- Suítes completas validadas: `mvn clean test` (todos os módulos backend passando) e `flutter analyze` (0 issues) + `flutter test` (212 testes passando).

### File List

- `backend/common/src/main/resources/db/changelog/29-create-family-profiles-and-link-trades.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/domain/entities/FamilyProfile.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/domain/CpfValidator.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/repository/FamilyProfilePort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/service/exceptions/InvalidCpfException.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/service/exceptions/DuplicateFamilyProfileException.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/service/exceptions/FamilyProfileNotFoundException.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/usecase/LinkFamilyProfileUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/usecase/ListFamilyProfilesUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/family/application/usecase/UnlinkFamilyProfileUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/events/FileUploadedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/events/TradeExtractedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/events/TradeExtractionFailedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/model/B3ImportStatus.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/repository/B3ImportStatusPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/usecase/UploadB3FileUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/usecase/ProcessB3FileUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/domain/entities/Trade.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/repository/TradePort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/SaveTradeUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/family/domain/CpfValidatorTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/family/application/usecase/FamilyProfileUseCasesTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/upload/application/usecase/UploadB3FileUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/SaveTradeUseCaseTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/family/infrastructure/persistence/jpa/entities/FamilyProfileEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/family/infrastructure/persistence/jpa/repositories/JpaFamilyProfileRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/family/infrastructure/persistence/jpa/FamilyProfileJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/family/infrastructure/spring/FamilyProfileConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/family/web/CreateFamilyProfileRequest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/family/web/FamilyProfileResponse.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/family/web/FamilyProfileController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/persistence/jpa/entities/B3ImportStatusEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/persistence/B3ImportStatusRepositoryAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaTradeExtractedConsumer.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3ImportStatusView.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3UploadController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/entities/TradeEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/repositories/JpaTradeRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/TradeJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/family/web/FamilyProfileControllerIT.java`
- `frontend/lib/main.dart`
- `frontend/lib/src/features/profile/presentation/pages/profile_page.dart`
- `frontend/lib/src/features/family_holding/domain/entities/family_profile_entity.dart`
- `frontend/lib/src/features/family_holding/domain/repositories/family_profile_repository.dart`
- `frontend/lib/src/features/family_holding/data/models/family_profile_model.dart`
- `frontend/lib/src/features/family_holding/data/datasources/family_profile_remote_datasource.dart`
- `frontend/lib/src/features/family_holding/data/repositories/family_profile_repository_impl.dart`
- `frontend/lib/src/features/family_holding/presentation/providers/family_profile_provider.dart`
- `frontend/lib/src/features/family_holding/presentation/pages/family_profiles_page.dart`
- `frontend/lib/src/features/upload/data/datasources/upload_remote_data_source.dart`
- `frontend/lib/src/features/upload/data/repositories/upload_repository_impl.dart`
- `frontend/lib/src/features/upload/domain/repositories/upload_repository.dart`
- `frontend/lib/src/features/upload/domain/usecases/upload_b3_file_usecase.dart`
- `frontend/lib/src/features/upload/presentation/providers/upload_provider.dart`
- `frontend/lib/src/features/upload/presentation/pages/upload_page.dart`
- `frontend/test/features/family_holding/family_profile_provider_test.dart`
- `frontend/test/features/family_holding/family_profiles_page_test.dart`
- `frontend/test/features/upload/upload_page_profile_selector_test.dart`
- `frontend/test/features/upload/data/repositories/upload_repository_impl_test.mocks.dart`
- `frontend/test/features/upload/domain/usecases/upload_file_usecase_test.mocks.dart`
- `frontend/test/features/upload/presentation/providers/upload_provider_test.mocks.dart`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`
- `docs/bmad/implementation-artifacts/7-1-vinculo-de-novo-cpf-a-conta-matriz.md`

### Change Log

- **2026-11-13:** Implementação completa da Story 7.1 (Backend `common`/`api` + Migration 29 + Frontend `family_holding` e seletor na `UploadPage`), com todos os testes (`mvn clean test`, `flutter analyze`, `flutter test`) passando.
