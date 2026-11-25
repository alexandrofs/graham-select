---
title: 'Story 1.2: Infraestrutura de Controle Transacional do Lote de Membros Founders'
type: 'feature'
created: '2026-11-24'
status: 'done'
baseline_commit: '33f388760fe585730dca6098606a0f3f0dbc0aef'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/bmad/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O sistema ainda provisiona novos usuários OAuth2 com trial fixo de 30 dias (`SubscriptionTier.TRIAL`) e não possui controle persistente e atômico do lote inicial de 250 vagas de Membros Founders (`tb_founder_quota`), nem os campos `commercial_plan` e `trial_expires_at` na tabela `users` ou o endpoint público `GET /api/v1/founders/slots`.

**Approach:** Criar migration incremental e idempotente do Liquibase adicionando `tb_founder_quota` e as colunas `commercial_plan` e `trial_expires_at` em `users`, implementar o serviço transacional de decremento condicional atômico (`WHERE founder_slots_remaining > 0`) integrado ao provisionamento OAuth2 (`FOUNDER_LIFETIME` vitalício vs. `PRO_TRIAL` de 90 dias com fallback não-destrutivo para `FREE`), e expor `GET /api/v1/founders/slots` com paridade de configuração em `application.yml`, Helm e CI/CD.

## Boundaries & Constraints

**Always:**
- Criar nova migration Liquibase incremental e estritamente idempotente (`preConditions` com `onFail: MARK_RAN`) em `backend/common/src/main/resources/db/changelog/37-create-founder-quota-and-add-user-commercial-plan.yaml`, registrada em `db.changelog-master.yaml`: criar `tb_founder_quota` (`id BIGINT PK`, `total_slots INT NOT NULL DEFAULT 250`, `founder_slots_remaining INT NOT NULL DEFAULT 250`, `updated_at DATETIME NOT NULL`) com registro singleton `id = 1`, e adicionar `commercial_plan VARCHAR(30) NOT NULL DEFAULT 'PRO_TRIAL'` e `trial_expires_at DATETIME NULL` na tabela real `users` (com backfill consistente a partir de `tier`/`trial_ends_at`).
- Decrementar a cota de forma atômica no banco via `UPDATE tb_founder_quota SET founder_slots_remaining = founder_slots_remaining - 1, updated_at = :now WHERE id = 1 AND founder_slots_remaining > 0` apenas na criação de novos usuários (com `email` válido): se `affectedRows == 1`, conceder `commercial_plan = FOUNDER_LIFETIME` (`tier = PREMIUM`, `trial_expires_at = null`, `trial_ends_at = null`); se `affectedRows == 0`, conceder `commercial_plan = PRO_TRIAL` (`tier = TRIAL`, `trial_expires_at = now(UTC) + 90 dias`, `trial_ends_at = trial_expires_at`).
- Configurar o teto inicial de vagas via propriedade `app.founders.total-slots: ${FOUNDER_TOTAL_SLOTS:250}` (e `app.founders.trial-days: ${FOUNDER_PRO_TRIAL_DAYS:90}`), reconciliando `total_slots` e `founder_slots_remaining` (descontando os slots já consumidos e aplicando `Math.max(0, ...)`) e espelhando `FOUNDER_TOTAL_SLOTS` em `application.yml`, `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml` e `.github/workflows/deploy.yml`.
- Expor `GET /api/v1/founders/slots` como endpoint público em `SecurityConfig` (`permitAll()`), retornando HTTP 200 `{ "totalSlots": 250, "remainingSlots": X, "isLoteActive": true/false }` (`isLoteActive = remainingSlots > 0`), com log `INFO` de entrada sem PII e log `ERROR` com stack trace em exceções.
- Manter compatibilidade retroativa com `SubscriptionTier` (`PREMIUM`, `TRIAL`, `FREE`) e `trialEndsAt` em `User`, `SubscriptionTierService`, `PremiumFeatureAccessGuard` e `UserController` (`/api/v1/users/me`), garantindo que `FOUNDER_LIFETIME` tenha acesso Pro vitalício e `PRO_TRIAL` expirado sofra downgrade lazy não-destrutivo para `FREE` (`commercial_plan = FREE` e `tier = FREE`).
- Usar `ZoneOffset.UTC` explicitamente em todas as operações de data/hora e atualizar `docs/workspace.dsl` com o componente de controle de cota Founders.

**Never:**
- Alterar migrations Liquibase existentes (`01` a `36`) ou usar o nome ilustrativo `tb_user` no SQL/Liquibase em vez da tabela real `users`.
- Decrementar `tb_founder_quota` em logins subsequentes de usuários já cadastrados ou quando o JWT não contiver `email` válido (rollback/pré-validação obrigatória para não vazar vagas).
- Antecipar nesta história a Landing Page HTML/SSR e `robots.txt` (Story 1.3) ou a UI de consentimento e badges visuais no frontend Flutter (Story 1.4).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Consulta pública de vagas (lote ativo) | `GET /api/v1/founders/slots` sem JWT, `founder_slots_remaining = 250` | HTTP 200 `{ "totalSlots": 250, "remainingSlots": 250, "isLoteActive": true }` e log `INFO` sem PII | Exceções logadas em `ERROR` com stack trace |
| Consulta pública de vagas (lote esgotado) | `GET /api/v1/founders/slots` sem JWT, `founder_slots_remaining = 0` | HTTP 200 `{ "totalSlots": 250, "remainingSlots": 0, "isLoteActive": false }` | Exceções logadas em `ERROR` com stack trace |
| Novo cadastro OAuth2 com vaga disponível | 1º login JWT (`sub` novo, `email` válido) com `founder_slots_remaining > 0` | Decrementa `founder_slots_remaining` em 1; persiste `User` com `commercialPlan = FOUNDER_LIFETIME`, `tier = PREMIUM`, `trialExpiresAt = null` | Se `save` falhar, transação faz rollback do decremento |
| Novo cadastro OAuth2 após esgotamento | 1º login JWT (`sub` novo, `email` válido) com `founder_slots_remaining == 0` | Mantém `founder_slots_remaining = 0`; persiste `User` com `commercialPlan = PRO_TRIAL`, `tier = TRIAL`, `trialExpiresAt = now(UTC) + 90d` | N/A |
| Cadastros simultâneos concorrentes | `N` threads simultâneas disputando `K < N` vagas restantes | Exatamente `K` usuários recebem `FOUNDER_LIFETIME` e `N - K` recebem `PRO_TRIAL` (90 dias); saldo final `remainingSlots = 0` sem saldo negativo | Em colisão de mesmo `sub`, fallback recupera usuário existente sem perder vaga |
| Login recorrente ou JWT inválido sem e-mail | Usuário já existente ou JWT com `email == null` | Usuário existente mantém seu `commercialPlan` sem decrementar cota; JWT sem `email` falha antes do decremento | Lança exceção e preserva `founder_slots_remaining` intacto |
| Expiração do Trial Pro de 90 dias | Usuário `PRO_TRIAL` com `trialExpiresAt < now(UTC)` acessa `/api/v1/users/me` ou rota `@RequirePremium` | `SubscriptionTierService` faz downgrade lazy não-destrutivo para `commercialPlan = FREE` e `tier = FREE` | Rota `@RequirePremium` retorna HTTP 403 Problem JSON |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (`L71`) e `backend/common/src/main/resources/db/changelog/37-create-founder-quota-and-add-user-commercial-plan.yaml` (novo) -- Migration Liquibase idempotente criando `tb_founder_quota` (seed `id=1, total_slots=250, founder_slots_remaining=250`) e adicionando `commercial_plan` e `trial_expires_at` em `users`.
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/CommercialPlan.java` (novo) e `User.java` (`L45-75`) -- Enum `CommercialPlan` (`FOUNDER_LIFETIME`, `PRO_TRIAL`, `FREE`), mapeamento de `commercial_plan` e `trial_expires_at` em `User` e sincronização defensiva em `ensureDefaults()` para builders legados.
- `backend/common/src/main/java/afsdigital/grahamselect/common/founders/domain/model/FounderSlotsStatus.java` (novo), `backend/common/src/main/java/afsdigital/grahamselect/common/founders/application/port/FounderQuotaRepositoryPort.java` (novo) e `backend/common/src/main/java/afsdigital/grahamselect/common/founders/application/service/FounderQuotaService.java` (novo) -- Modelo, porta e serviço de domínio/aplicação para consulta (`getSlotsStatus`) e alocação atômica (`tryClaimFounderSlot`).
- `backend/api/src/main/java/afsdigital/grahamselect/api/founders/infrastructure/persistence/jpa/entities/FounderQuotaEntity.java` (novo), `FounderQuotaJpaRepository.java` (novo), `JdbcFounderQuotaRepositoryAdapter.java` (novo) e `backend/api/src/main/java/afsdigital/grahamselect/api/founders/infrastructure/spring/FoundersConfiguration.java` (novo) -- Persistência e `UPDATE` condicional atômico em `tb_founder_quota` com reconciliação do teto `app.founders.total-slots`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/founders/web/FoundersController.java` (novo), `FounderSlotsResponseDto.java` (novo) e `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfig.java` (`L34-40`) -- Endpoint público `GET /api/v1/founders/slots` liberado no Spring Security com logs estruturados.
- `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java` (`L33-81`), `backend/api/src/main/java/afsdigital/grahamselect/api/user/service/SubscriptionTierService.java` (`L19-34`) e `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java` (`L131-155`) -- Concessão atômica `FOUNDER_LIFETIME` vs. `PRO_TRIAL` (90 dias) no primeiro login OAuth2, downgrade lazy de `PRO_TRIAL` expirado para `FREE` e exposição de `commercialPlan`/`trialExpiresAt` no `UserProfile`.
- `backend/api/src/main/resources/openapi.yaml` (`L881-930`), `backend/api/pom.xml` (`L60-77`), `backend/api/src/main/resources/application.yml` (`L35-42`), `helm/graham-select/values.yaml` (`L25-43`), `helm/graham-select/templates/configmap.yaml` (`L20-31`), `.github/workflows/deploy.yml` (`L226-244`) e `docs/workspace.dsl` (`L33-75`) -- Contrato OpenAPI, paridade `FOUNDER_TOTAL_SLOTS` e diagrama C4.
- `backend/common/src/test/java/afsdigital/grahamselect/common/founders/application/service/FounderQuotaServiceTest.java` (novo), `backend/api/src/test/java/afsdigital/grahamselect/api/founders/web/FoundersControllerTest.java` (novo), `backend/api/src/test/java/afsdigital/grahamselect/api/founders/FounderQuotaConcurrencyIT.java` (novo), `backend/api/src/test/java/afsdigital/grahamselect/api/auth/UserTierManagementIT.java` e `backend/api/src/test/java/afsdigital/grahamselect/api/auth/UserAutoProvisioningExtendedIT.java` -- Testes unitários e de integração concorrente.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/resources/db/changelog/37-create-founder-quota-and-add-user-commercial-plan.yaml`, `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` -- Criar migration Liquibase idempotente para `tb_founder_quota` (seed singleton `250`) e colunas `commercial_plan` e `trial_expires_at` em `users` com backfill -- Estruturar esquema relacional do Programa Founders sem tocar migrations anteriores.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/CommercialPlan.java`, `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java`, `backend/common/src/main/java/afsdigital/grahamselect/common/founders/domain/model/FounderSlotsStatus.java`, `backend/common/src/main/java/afsdigital/grahamselect/common/founders/application/port/FounderQuotaRepositoryPort.java`, `backend/common/src/main/java/afsdigital/grahamselect/common/founders/application/service/FounderQuotaService.java` -- Modelar `CommercialPlan`, atualizar entidade `User` (com defaults compatíveis) e implementar `FounderQuotaService` -- Isolar regras de domínio de concessão `FOUNDER_LIFETIME` vs. `PRO_TRIAL` (90 dias).
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/founders/infrastructure/persistence/jpa/entities/FounderQuotaEntity.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/founders/infrastructure/persistence/jpa/repository/FounderQuotaJpaRepository.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/founders/infrastructure/persistence/JdbcFounderQuotaRepositoryAdapter.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/founders/infrastructure/spring/FoundersConfiguration.java` -- Implementar adaptador transacional com `UPDATE tb_founder_quota SET founder_slots_remaining = founder_slots_remaining - 1 WHERE id = 1 AND founder_slots_remaining > 0` e inicialização/reconciliação do teto configurável -- Garantir atomicidade sob concorrência.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/user/service/SubscriptionTierService.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java`, `backend/api/src/main/resources/openapi.yaml`, `backend/api/pom.xml` -- Integrar consumo atômico de vaga Founder no provisionamento OAuth2 (pré-validando `email` antes do decremento), suportar `CommercialPlan` e `trialExpiresAt` no `SubscriptionTierService` e `UserController`, e documentar contrato no `openapi.yaml` -- Conceder acesso vitalício aos Founders e trial de 90 dias aos excedentes.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/founders/web/FoundersController.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/founders/web/dto/FounderSlotsResponseDto.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfig.java` -- Expor `GET /api/v1/founders/slots` público em `SecurityConfig` com logs `INFO`/`ERROR` e payload `{ "totalSlots", "remainingSlots", "isLoteActive" }` -- Servir contador auditável em tempo real sem exigir autenticação.
- [x] `backend/api/src/main/resources/application.yml`, `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `.github/workflows/deploy.yml`, `docs/workspace.dsl` -- Adicionar `app.founders.total-slots` (`FOUNDER_TOTAL_SLOTS: 250`) e `app.founders.trial-days` (`90`) com paridade Helm/CI-CD e atualizar diagrama C4 -- Cumprir governança de configuração e documentação arquitetural.
- [x] `backend/common/src/test/java/afsdigital/grahamselect/common/founders/application/service/FounderQuotaServiceTest.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/founders/web/FoundersControllerTest.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/founders/FounderQuotaConcurrencyIT.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/auth/UserTierManagementIT.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/auth/UserAutoProvisioningExtendedIT.java` -- Cobrir matriz I/O com testes unitários (`mvn clean test`) e testes de integração (`*IT.java`) validando disputa concorrente multi-thread pelas últimas vagas Founders -- Comprovar ausência de race conditions e regressões.

**Acceptance Criteria:**
- Given a migration `37-create-founder-quota-and-add-user-commercial-plan.yaml` aplicada, when o banco é inspecionado, then `tb_founder_quota` existe com `founder_slots_remaining = 250` e `users` possui as colunas `commercial_plan` e `trial_expires_at`.
- Given o endpoint público `GET /api/v1/founders/slots`, when chamado sem autenticação JWT, then retorna HTTP 200 com `{ "totalSlots": 250, "remainingSlots": X, "isLoteActive": true/false }` e registra log `INFO` estruturado sem PII.
- Given múltiplos cadastros OAuth2 simultâneos disputando vagas limitadas em `tb_founder_quota`, when processados concorrentemente, then o decremento atômico concede `commercial_plan = 'FOUNDER_LIFETIME'` exatamente até zerar `founder_slots_remaining` e atribui `commercial_plan = 'PRO_TRIAL'` com expiração em 90 dias aos demais.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Layer | Location | Verdict | Route | Evidence |
|---|---|---|---|---|
| edge-case-hunter | `JdbcFounderQuotaRepositoryAdapter.java:41-46` | `low` | `patch` | `getStatus()` fallback não aplica `Math.max(0, configuredTotalSlots)` em `totalSlots` quando a linha 1 não existe. |
| edge-case-hunter | `JdbcFounderQuotaRepositoryAdapter.java:50-57` | `false` | `reject` | `FounderQuotaService.tryClaimFounderSlot()` sempre passa `LocalDateTime.now(ZoneOffset.UTC)`; nenhum caller alcança `null`. |
| edge-case-hunter | `JdbcFounderQuotaRepositoryAdapter.java:60-87` | `medium` | `patch` | `reconcileConfiguredTotalSlots()` faz `findById` + `save` sem lock/SQL atômico, sujeito a lost update ou colisão de PK em réplicas simultâneas. |
| edge-case-hunter | `FounderQuotaService.java:27-29` | `false` | `reject` | `OAuth2UserProvisioningConverter` sempre passa `LocalDateTime.now(ZoneOffset.UTC)` e `trialDays` tem default `90`. |
| edge-case-hunter | `UserController.java:149-158` | `medium` | `patch` | Usuário `FOUNDER_LIFETIME` com `trialEndsAt` legado não-nulo expõe `trialEndsAt`/`trialExpiresAt` no `UserProfile`. |
| edge-case-hunter | `User.java:98-103` | `low` | `patch` | `ensureDefaults()` não limpa `trialEndsAt`/`trialExpiresAt` quando `commercialPlan == FOUNDER_LIFETIME`. |
| blind-hunter | `JdbcFounderQuotaRepositoryAdapter.java` | `medium` | `patch` | `Timestamp.valueOf(nowUtc)` usa o timezone padrão da JVM em vez de UTC; passar `LocalDateTime` direto no JDBC evita deslocamento de horário. |
| blind-hunter | `JdbcFounderQuotaRepositoryAdapter.java` | `medium` | `patch` | Mesma causa raiz de `reconcileConfiguredTotalSlots()` sem lock/SQL atômico (agrupado com edge-case-hunter). |
| blind-hunter | `OAuth2UserProvisioningConverter.java` | `medium` | `patch` | `IllegalArgumentException` para `email` ausente não é `AuthenticationException`, podendo gerar HTTP 500 em vez de 401 no `BearerTokenAuthenticationFilter`. |
| blind-hunter | `OAuth2UserProvisioningConverter.java` | `false` | `reject` | `userRepository.save()` já é `@Transactional` no Spring Data JPA; fallback `catch` é comportamento pré-existente. |
| blind-hunter | `SubscriptionTierService.java`, `UserController.java` | `medium` | `patch` | `mapToUserProfile()` deixa `daysRemaining` como `null` para `FOUNDER_LIFETIME` (sem expiração) em vez de `0`, e `resolveEffectiveTier()` não reconcilia `commercialPlan` divergente. |
| blind-hunter | `37-create-founder-quota-and-add-user-commercial-plan.yaml` | `medium` | `patch` | Backfill promove `PREMIUM` existentes a `FOUNDER_LIFETIME` sem descontar esses usuários de `founder_slots_remaining` e omite bloco `rollback`. |
| blind-hunter | `.github/workflows/deploy.yml`, `FounderQuotaService.java`, `JdbcFounderQuotaRepositoryAdapter.java` | `low` | `patch` | `FOUNDER_PRO_TRIAL_DAYS` ausente no `--set-string` do `deploy.yml` (embora tenha default `"90"` no Helm). |
| blind-hunter | `openapi.yaml`, `FoundersController.java` | `false` | `reject` | Controller manual com exclusão de `FoundersApi*` no `pom.xml` é intencional conforme spec (padrão de `Dividends`/`OpenFinance`). |
| blind-hunter | `UserAutoProvisioningIT.java`, `FoundersControllerTest.java`, `FounderQuotaConcurrencyIT.java` | `low` | `patch` | `ExecutorService` em `FounderQuotaConcurrencyIT` deve ser encerrado em bloco `finally`. |
| verification-gap | `.github/workflows/deploy.yml`, `*IT.java` | `medium` | `defer` | Lacuna pré-existente do repositório (`mvn clean test` pula `*IT.java` sem Failsafe); adicionar `*Test.java` para lógica unitária nova e registrar `*IT` no CI em `deferred-work.md`. |
| verification-gap | `JdbcFounderQuotaRepositoryAdapter.java:58-86` | `medium` | `patch` | `reconcileConfiguredTotalSlots()` (branches `entity == null`, `safeTotalSlots != totalSlots` e clamp `Math.max(0, ...)`) sem cobertura de teste. |

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: todos os testes unitários dos módulos `common`, `api` e `valuation-service` aprovados (exit 0).
- `cd backend && mvn test -pl api -Dtest=FounderQuotaConcurrencyIT,UserTierManagementIT,UserAutoProvisioningIT,UserAutoProvisioningExtendedIT,AuthEndpointE2EIT,FeatureRestrictionIT,UserProfileIT,AccountDeletionIT` -- expected: todos os testes de integração H2/Liquibase aprovados sem condição de corrida (exit 0).
- `cd frontend && flutter analyze && flutter test` -- expected: zero warnings/erros e suíte frontend íntegra.
