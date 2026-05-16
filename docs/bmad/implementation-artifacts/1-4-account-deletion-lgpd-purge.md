# Story 1.4: Exclusão de Conta e Expurgo LGPD

Status: in-progress

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário da plataforma,
I want poder excluir permanentemente minha conta e todos os meus dados,
so that meu direito ao esquecimento (LGPD Art. 18, VI) seja respeitado.

## Acceptance Criteria

1. **Solicitação de Exclusão (Frontend → Backend)**
   - **Given** que o usuário está autenticado
   - **When** ele acessa a página de configurações e solicita exclusão de conta
   - **Then** o sistema exige confirmação dupla (modal + digitação da palavra "EXCLUIR")
   - **And** registra a solicitação com timestamp em UTC na tabela `account_deletion_requests`
   - **And** retorna HTTP 202 Accepted com corpo `{ "data": { "requestId": "<uuid>", "status": "PENDING", "estimatedCompletionWithin": "24h" } }`

2. **Execução do Expurgo (Job Assíncrono)**
   - **Given** que a solicitação de exclusão foi confirmada (status = `PENDING`)
   - **When** o job de expurgo é executado (scheduled ou via Kafka event)
   - **Then** realiza Hard Delete em cascata de TODOS os dados do usuário, na ordem:
     1. Dados financeiros: todas as linhas em `trade_transactions` (futuro), `positions` (futuro), metas, valuation data vinculadas ao `user_id`
     2. PIIs: `email`, `full_name`, `google_sub` na tabela `users`
     3. Dados de autenticação: invalida a sessão e remove o vínculo
   - **And** atualiza o registro na tabela `account_deletion_requests` com `status = COMPLETED` e `completed_at` timestamp
   - **And** conclui o expurgo em menos de 24 horas (NFR5)
   - **And** envia e-mail de confirmação para o endereço salvo ANTES de deletar os dados de autenticação

3. **Pós-Expurgo (Relogin com mesmo Google ID)**
   - **Given** que o expurgo foi concluído e o usuário foi completamente removido
   - **When** alguém tenta fazer login com o mesmo Google ID (`google_sub`)
   - **Then** o sistema trata como novo registro (auto-provisioning normal, como na Story 1.1)
   - **And** cria um novo usuário com tier `TRIAL` e validade de 30 dias

4. **Cancelamento de Solicitação (Período de Graça)**
   - **Given** que o usuário solicitou exclusão mas o job ainda não processou
   - **When** ele faz login e cancela a solicitação
   - **Then** o sistema atualiza o status para `CANCELLED` e mantém a conta ativa

## Tasks / Subtasks

- [x] **Task 1: Modelagem de Dados — Tabela `account_deletion_requests` (AC: #1, #2, #4)**
  - [x] Criar migration Liquibase `09-create-account-deletion-requests-table.yaml`
  - [x] Colunas: `id` (BIGINT PK AUTO_INCREMENT), `user_id` (BIGINT FK → users.id), `status` (VARCHAR(20): PENDING/COMPLETED/CANCELLED/FAILED), `requested_at` (DATETIME NOT NULL), `completed_at` (DATETIME NULL), `failure_reason` (TEXT NULL)
  - [x] Constraint: `uq_deletion_requests_user_pending` — apenas 1 pedido PENDING por user
  - [x] Registrar no `db.changelog-master.yaml`

- [x] **Task 2: Domain & Application Layer — Use Cases no módulo `common` (AC: #1, #2, #4)**
  - [x] Criar entity `AccountDeletionRequest` em `common/user/domain/entities/`
  - [x] Criar enum `DeletionStatus` (PENDING, COMPLETED, CANCELLED, FAILED) em `common/user/domain/entities/`
  - [x] Criar port interface `AccountDeletionRequestRepository` em `common/user/application/repository/`
  - [x] Criar `RequestAccountDeletionUseCase` (POJO com `execute()`) em `common/user/application/usecase/`
  - [x] Criar `ExecuteAccountPurgeUseCase` (POJO com `execute()`) em `common/user/application/usecase/`
  - [x] Criar `CancelAccountDeletionUseCase` (POJO com `execute()`) em `common/user/application/usecase/`
  - [x] Criar exceções: `DeletionAlreadyPendingException`, `DeletionNotFoundException` em `common/user/application/service/exceptions/`

- [x] **Task 3: Infrastructure Layer — Persistência JPA no módulo `api` (AC: #1, #2)**
  - [x] Criar `AccountDeletionRequestEntity` em `api/user/infrastructure/persistence/jpa/entities/`
  - [x] Criar `AccountDeletionRequestJpaRepository` em `api/user/infrastructure/persistence/jpa/repository/`
  - [x] Criar `AccountDeletionRequestRepositoryImpl` (adapter que implementa port) em `api/user/infrastructure/persistence/`
  - [x] Criar `AccountDeletionConfiguration` (@Configuration que cria beans dos Use Cases) em `api/user/infrastructure/spring/`

- [x] **Task 4: API REST — Endpoints de Exclusão (AC: #1, #4)**
  - [x] Adicionar endpoints ao `openapi.yaml`:
    - `DELETE /users/me` → Solicita exclusão (retorna 202)
    - `POST /users/me/cancel-deletion` → Cancela solicitação pendente (retorna 200)
  - [x] Criar `AccountDeletionController` (implements delegate gerado pelo OpenAPI) em `api/user/web/` (implementado no `UserController` para manter coesão da tag User)
  - [x] Endpoint `DELETE /users/me`: extrai `userId` do SecurityContext, invoca `RequestAccountDeletionUseCase`
  - [x] Endpoint `POST /users/me/cancel-deletion`: invoca `CancelAccountDeletionUseCase`
  - [x] Ambos endpoints NÃO precisam de `@RequirePremium` — qualquer tier pode excluir sua conta

- [x] **Task 5: Job de Expurgo Assíncrono (AC: #2, #3)**
  - [x] Criar `AccountPurgeScheduler` em `api/user/infrastructure/spring/`
  - [x] Usar `@Scheduled(fixedRate = 3600000)` (a cada 1h) para buscar requests PENDING
  - [x] Para cada request PENDING:
    1. Carregar o User completo
    2. Salvar e-mail para envio de confirmação pós-exclusão (Placeholder)
    3. Deletar TODOS os dados financeiros vinculados ao `user_id` (queries diretas por FK)
    4. Deletar o registro do `users` (cascata JPA ou delete manual)
    5. Atualizar `account_deletion_requests` para COMPLETED
    6. Enviar e-mail de confirmação (placeholder — pode ser log no MVP)
  - [x] Em caso de erro: marcar request como FAILED com `failure_reason`
  - [x] Logging estruturado: NUNCA logar dados sensíveis (e-mail, nome), apenas `userId` e `requestId`

- [x] **Task 6: Testes (AC: #1, #2, #3, #4)**
  - [x] Testes unitários para cada Use Case (JUnit 5 + Mockito):
    - `RequestAccountDeletionUseCaseTest`: cenários de sucesso, duplicata PENDING, user inexistente
    - `ExecuteAccountPurgeUseCaseTest`: cenário de sucesso com cascata, cenário de falha parcial
    - `CancelAccountDeletionUseCaseTest`: cenário de sucesso, request não encontrada, já completada
  - [x] Testes de integração (`AccountDeletionIT`) com Testcontainers (MySQL):
    - Fluxo completo: criar user → solicitar exclusão → executar purge → verificar dados removidos
    - Verificar cancelamento de solicitação pendente
    - Rodar o job manualmente e verificar Hard Delete do usuário e status COMPLETED no request
  - [x] Verificar que o endpoint NÃO exige `@RequirePremium`

## Dev Notes

### Contexto Crítico de Negócio

- **LGPD (Lei 13.709/2018 — Art. 18, VI):** O direito ao esquecimento exige remoção integral dos dados pessoais. O PRD/Epic especifica **Hard Delete** (não soft delete) dos dados financeiros atrelados ao usuário.
- **NFR5:** SLA de 24 horas para completar o expurgo após confirmação do usuário.
- **NFR4 (Isolamento Tenant):** O job de purge DEVE filtrar EXCLUSIVAMENTE pelo `user_id` para garantir que apenas os dados daquele tenant sejam removidos. NUNCA fazer DELETE sem cláusula WHERE filtrando por user.

### Padrões Arquiteturais Obrigatórios

- **Clean Architecture:** Use Cases são POJOs com método `execute()`, sem anotações Spring. Wiring via `@Configuration`.
- **Naming:**
  - Package: `afsdigital.grahamselect.common.user.application.usecase` (Use Cases no `common`)
  - Package: `afsdigital.grahamselect.api.user.infrastructure.persistence` (JPA adapters no `api`)
  - Package: `afsdigital.grahamselect.api.user.web` (Controllers no `api`)
- **Error Handling:** RFC 7807 ProblemDetail via `@ControllerAdvice` global. Config: `spring.mvc.problemdetails.enabled=true`.
- **API Response Format:**
  - Sucesso: `{ "data": { ... } }`
  - Erro: RFC 7807 ProblemDetail nativo Spring Boot 3.x
- **Datas:** Todas em UTC (`LocalDateTime` com `ZoneOffset.UTC` ou `OffsetDateTime`). A Story 1.3 teve bug de comparação de datas sem UTC — EVITAR.
- **Sem PII no Kafka:** Se precisar emitir evento de deleção, trafegar APENAS `userId` (ID interno), NUNCA e-mail/nome/CPF.

### Tabelas Afetadas pelo Purge (Inventário Atual)

No estado atual do banco, as tabelas com dados vinculados a `user_id` são:
1. `users` — Tabela principal do usuário (PIIs: `email`, `full_name`, `google_sub`)
2. `account_deletion_requests` — Nova tabela (esta story)

**Tabelas futuras (sem FK por enquanto, mas o código deve ser extensível):**
- `trade_transactions` (Epic 2)
- `portfolio_positions` (Epic 3)
- `allocation_goals` (Epic 4-5)
- `audit_logs` (Epic 2)

**Estratégia:** O `ExecuteAccountPurgeUseCase` deve encapsular a lógica de purge de modo que, à medida que novas tabelas forem adicionadas, basta adicionar mais `deleteBy*` chamadas ao use case. Criar um port interface `UserDataPurgePort` com método `purgeAllUserData(Long userId)` que cada módulo implementará.

### Inteligência da Story 1.3 (Anterior)

- **Padrão de SecurityContext:** A extração do `userId` via `SecurityContextHolder.getContext().getAuthentication().getName()` já está estabelecida no `UserController`. O userId é um `Long` parseado da claim `name` do token JWT (após auto-provisioning pelo `OAuth2UserProvisioningConverter`).
- **Bug corrigido na 1.3:** Comparação de datas com `LocalDateTime.now()` sem UTC — o `SubscriptionTierService` usava `LocalDateTime.now()` ao invés de `LocalDateTime.now(ZoneOffset.UTC)`. Nesta story, usar `LocalDateTime.now(ZoneOffset.UTC)` para todas as comparações temporais.
- **Padrão `@RequirePremium`:** Criado na Story 1.3 para proteger endpoints Premium. Os endpoints desta story (DELETE /users/me, POST /cancel-deletion) NÃO devem usar `@RequirePremium` — qualquer tier pode excluir sua conta.
- **PremiumFeatureAccessDeniedHandler:** Já configurado no `SecurityConfig`. Não precisa alterar.

### Referências de Código Existente

| Componente | Caminho |
|---|---|
| User entity | `backend/common/src/main/java/.../common/user/domain/entities/User.java` |
| SubscriptionTier enum | `backend/common/src/main/java/.../common/user/domain/entities/SubscriptionTier.java` |
| UserRepository | `backend/common/src/main/java/.../common/user/infrastructure/persistence/UserRepository.java` |
| UserController | `backend/api/src/main/java/.../api/user/web/UserController.java` |
| SecurityConfig | `backend/api/src/main/java/.../api/auth/infrastructure/security/SecurityConfig.java` |
| OAuth2Converter | `backend/api/src/main/java/.../api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java` |
| SubscriptionTierService | `backend/api/src/main/java/.../api/user/service/SubscriptionTierService.java` |
| OpenAPI spec | `backend/api/src/main/resources/openapi.yaml` |
| Liquibase master | `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` |
| Last migration | `08-add-user-subscription-fields.yaml` → próxima deve ser `09-*` |

### Convenções de Teste

- **Unit Tests:** JUnit 5 + Mockito + `@RequiredArgsConstructor`. Use Cases testados isoladamente com mocks dos ports.
- **Integration Tests:** `@SpringBootTest` + Testcontainers (MySQL container real). Padrão usado em `FeatureRestrictionIT` e `RankedCompanyIT`.
- **Naming:** `*Test.java` (unitário), `*IT.java` (integração).

### Anti-Patterns a Evitar

1. ❌ **NÃO** usar soft delete (flag `deleted=true`) — o PRD exige Hard Delete para compliance LGPD
2. ❌ **NÃO** deletar dados diretamente no endpoint síncrono — o expurgo DEVE ser assíncrono (job)
3. ❌ **NÃO** logar e-mail, nome ou CPF no log do purge — apenas `userId` e `requestId`
4. ❌ **NÃO** enviar dados sensíveis via Kafka — apenas `userId` interno
5. ❌ **NÃO** usar `float`/`double` para qualquer dado financeiro — usar `BigDecimal`
6. ❌ **NÃO** fazer DELETE sem WHERE filtrando por `user_id`
7. ❌ **NÃO** importar frameworks Spring nas classes de domain/application/usecase

### Project Structure Notes

- Todos os novos arquivos seguem o padrão feature-first do projeto:
  - Use Cases / Domain: `backend/common/src/main/java/afsdigital/grahamselect/common/user/`
  - Infrastructure JPA: `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/persistence/`
  - Controllers: `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/`
  - Config: `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/spring/`
  - Migrations: `backend/common/src/main/resources/db/changelog/`
- Frontend: **NÃO incluído nesta story** (será criada story frontend separada se necessário)

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story-1.4] — Acceptance Criteria e user story original
- [Source: docs/bmad/planning-artifacts/prd.md#FR5] — FR5: Exclusão definitiva da conta
- [Source: docs/bmad/planning-artifacts/prd.md#NFR5] — NFR5: Expurgo em < 24h
- [Source: docs/bmad/planning-artifacts/prd.md#NFR4] — NFR4: Isolamento de tenant
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean-Architecture] — Padrão de camadas
- [Source: docs/bmad/planning-artifacts/architecture.md#D8] — RFC 7807 Error Handling
- [Source: docs/bmad/implementation-artifacts/1-3-feature-restriction-by-tier.md] — Learnings da story anterior

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

- Implementado fluxo completo de expurgo LGPD seguindo Clean Architecture.
- Adicionada migration Liquibase para solicitações de exclusão.
- Implementados Use Cases, Ports e Adapters para isolamento de domínio.
- Criado job agendado (@Scheduled) para processamento assíncrono.
- Resolvido problema de integridade referencial desconectando a solicitação do usuário antes da deleção física.
- Cobertura de testes unitários e integração garantida.


### File List
java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/spring/AccountPurgeScheduler.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/user/AccountDeletionIT.java`

