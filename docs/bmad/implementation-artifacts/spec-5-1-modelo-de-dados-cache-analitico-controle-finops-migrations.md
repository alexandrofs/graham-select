---
title: 'Modelo de Dados de Cache Analítico, Controle FinOps e Migrations de IA'
type: 'feature'
created: '2026-11-27'
baseline_commit: '13ec5611ec2045236e766d68563b6e778229bfa4'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/docs/bmad/implementation-artifacts/epic-5-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O Kerno AI ainda não possui persistência para cache de diagnósticos por hash de carteira nem controle transacional de cota de uso (FinOps), expondo futuras integrações com LLM a latência alta em leituras repetidas e a risco de abuso de custo.

**Approach:** Criar a migration Liquibase incremental e idempotente (`39-create-ai-cache-and-usage-tables.yaml`) para `tb_ai_diagnostic_cache` e `tb_ai_usage_counter`, os modelos/portas/serviços de domínio em `backend/common`, os adaptadores de persistência atômica e handler `ProblemDetail` (HTTP 429) no `GlobalExceptionHandler` em `backend/api`, a paridade no Helm/CI e o expurgo LGPD em `UserDataPurgeAdapter`.

## Boundaries & Constraints

**Always:**
- Usar `ZoneOffset.UTC` em todas as datas/horas (`LocalDateTime.now(ZoneOffset.UTC)` e `LocalDate.now(ZoneOffset.UTC)`).
- Tornar a migration `39` estritamente idempotente com `preConditions` separadas por `changeSet` (`onFail: MARK_RAN`, `not: tableExists`), `autoIncrement: true` nos IDs `BIGINT`, constraints `nullable: false`, nomes explícitos de FKs/índices e bloco `rollback`, compatível com MySQL e H2 (`MODE=MySQL`).
- Garantir incremento atômico da cota FinOps na camada de persistência (evitando read-modify-write concorrente) e calcular o consumo mensal via `SUM(daily_call_count)` no mês UTC corrente.
- Espelhar `AI_DAILY_RATE_LIMIT: "5"`, `AI_MONTHLY_RATE_LIMIT: "30"` e `AI_CACHE_TTL_HOURS: "24"` como strings em `application.yml`, `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml` e `--set-string` em `.github/workflows/deploy.yml`.
- Incluir `tb_ai_diagnostic_cache` e `tb_ai_usage_counter` no `UserDataPurgeAdapter` (via sobrecarga `deleteQuietly(String, String, Long)` fora do loop de `String`) e no `@BeforeEach cleanUp()` + teste do `AccountDeletionIT`.
- Mapear `AiRateLimitExceededException` no `GlobalExceptionHandler` existente retornando `ProblemDetail` (`HttpStatus.TOO_MANY_REQUESTS`), propriedade `latestDiagnostic` e `log.error` com stack trace.

**Never:**
- Alterar migrations Liquibase existentes (`01` a `38`).
- Criar um novo `@RestControllerAdvice` paralelo ao `GlobalExceptionHandler`.
- Chamar Vertex AI/LLM ou implementar `PiiSanitizer` nesta história (escopo das histórias 5.2 e 5.3).
- Debitar cota de uso em cache hit ou para `CommercialPlan.FREE` (`0` chamadas LLM).
- Alterar identificadores internos legados (`graham-select`, `grahamselect`, `afsdigital.grahamselect.*`).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Cache hit válido | `(userId, portfolioStateHash, modelVersion, promptVersion)` com `expires_at > now(UTC)` | Retorna `AiDiagnosticCacheEntry` sem debitar cota | N/A |
| Cache expirado ou miss | Chave ausente, versão distinta ou `expires_at <= now(UTC)` | Retorna `Optional.empty()` para disparo de nova análise | N/A |
| Consumo dentro da cota | `FOUNDER_LIFETIME` ou `PRO_TRIAL`, `daily < 5` e `monthly < 30` | Incrementa `daily_call_count` atomicamente em `usage_date` UTC | N/A |
| Limite diário/mensal atingido | `daily >= 5` ou `monthly >= 30` (incluindo concorrência) | Lança `AiRateLimitExceededException` anexando último cache | `GlobalExceptionHandler` retorna `ProblemDetail` HTTP `429` |
| Conta Free pós-trial | `CommercialPlan.FREE` | Não debita cota; sinaliza domínio (`AiFreeTierNotAllowedException`) para fallback local na Story 5.3 | Não expor HTTP 500 na 5.1 |
| Expurgo LGPD | `purgeAllUserData(userId)` | Remove todos os registros do usuário nas duas novas tabelas | Log `ERROR` com stack trace se falhar |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` -- Registrar `39-create-ai-cache-and-usage-tables.yaml` após a `38`. Não alterar `01`–`38`.
- `backend/common/src/main/resources/db/changelog/37-create-founder-quota-and-add-user-commercial-plan.yaml` -- Referência de `preConditions` (`onFail: MARK_RAN`), `BIGINT` FK para `users.id` e `rollback`.
- `backend/common/src/main/java/afsdigital/grahamselect/common/founders/` -- Referência de organização hexagonal (`domain/model`, `application/port`, `application/service`).
- `backend/api/src/main/java/afsdigital/grahamselect/api/founders/infrastructure/persistence/JdbcFounderQuotaRepositoryAdapter.java` -- Referência de `UPDATE` atômico via `NamedParameterJdbcTemplate`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java` -- Handler centralizado com `@ControllerAdvice` e `ProblemDetail`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/persistence/UserDataPurgeAdapter.java` -- Adicionar `deleteQuietly(String, String, Long)` e purgar as duas tabelas.
- `backend/api/src/test/java/afsdigital/grahamselect/api/user/AccountDeletionIT.java` -- Atualizar `@BeforeEach cleanUp()` e `shouldPerformHardDeleteDuringPurge()`.
- `backend/api/src/main/resources/application.yml`, `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `.github/workflows/deploy.yml` -- Paridade de `AI_DAILY_RATE_LIMIT`, `AI_MONTHLY_RATE_LIMIT` e `AI_CACHE_TTL_HOURS`.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/resources/db/changelog/39-create-ai-cache-and-usage-tables.yaml` e `db.changelog-master.yaml` -- Criar `tb_ai_diagnostic_cache` (`id` BIGINT `autoIncrement: true`, `user_id` FK `users.id` `fk_ai_cache_user`, `portfolio_state_hash` VARCHAR(64), `model_version` VARCHAR(64), `prompt_version` VARCHAR(32), `json_payload` TEXT, `created_at` DATETIME, `expires_at` DATETIME, índices `idx_ai_cache_lookup(user_id, portfolio_state_hash, model_version, prompt_version)` e `idx_ai_cache_user_expires(user_id, expires_at)`) e `tb_ai_usage_counter` (`id` BIGINT `autoIncrement: true`, `user_id` FK `users.id` `fk_ai_usage_user`, `usage_date` DATE, `daily_call_count` INT, `updated_at` DATETIME, unique `uq_ai_usage_user_date(user_id, usage_date)`) com `nullable: false`, `preConditions` separadas e `rollback` -- Persistência idempotente compatível com MySQL, H2 e `valuation-service`.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/ai/` -- Criar modelos (`AiDiagnosticCacheEntry`, `AiUsageStatus`, `AiFinOpsProperties`), exceções (`AiRateLimitExceededException`, `AiFreeTierNotAllowedException`), portas (`AiDiagnosticCachePort`, `AiUsageCounterPort`) e serviços (`AiDiagnosticCacheService`, `AiFinOpsRateLimitService`) cobrindo TTL 24h, lookup por hash+versões, cálculo de cota diária/mensal e bloqueio `FREE` -- Isolar regras FinOps e cache no módulo de domínio.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/ai/` e `GlobalExceptionHandler.java` -- Implementar entidades/repositórios JPA para `tb_ai_diagnostic_cache`, adaptador atômico (`NamedParameterJdbcTemplate`) para `tb_ai_usage_counter` (`UPDATE ... WHERE daily_call_count < :limit` + tratamento de `DataIntegrityViolationException` no insert inicial + `SUM(daily_call_count)` no mês UTC), `AiFinOpsConfigurationProperties` e `@ExceptionHandler(AiRateLimitExceededException.class)` no `GlobalExceptionHandler` (`ProblemDetail` 429 + `latestDiagnostic`) -- Garantir atomicidade FinOps e padrão RFC 7807.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/persistence/UserDataPurgeAdapter.java` e `AccountDeletionIT.java` -- Adicionar sobrecarga `deleteQuietly(String, String, Long)`, purgar as duas tabelas, limpar no `@BeforeEach cleanUp()` e validar contagem `0` no `AccountDeletionIT` -- Conformidade LGPD obrigatória.
- [x] `backend/api/src/main/resources/application.yml`, `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `.github/workflows/deploy.yml` -- Adicionar `AI_DAILY_RATE_LIMIT: "5"`, `AI_MONTHLY_RATE_LIMIT: "30"`, `AI_CACHE_TTL_HOURS: "24"` (e `--set-string` no workflow) -- Paridade entre código, Helm e CI.
- [x] `backend/common/src/test/.../ai/` e `backend/api/src/test/.../ai/` -- Criar testes unitários (cota 5/dia, 30/mês, virada de dia/mês UTC, `FREE` tier, cache hit/expiração/versão) e `*IT` (H2 + Liquibase, concorrência FinOps, `ProblemDetail` 429 e latência `< 400ms`) -- Cobrir matriz I/O e critérios de aceitação.

**Acceptance Criteria:**
- Given a migration `39-create-ai-cache-and-usage-tables.yaml` aplicada em banco limpo ou já migrado, when o contexto Spring Boot (`api` e `AccountDeletionIT`) inicializa com Liquibase, then as tabelas `tb_ai_diagnostic_cache` e `tb_ai_usage_counter` existem com índices, unique constraint e FKs válidos sem erros de idempotência.
- Given um diagnóstico salvo em `tb_ai_diagnostic_cache` com `expires_at` 24h à frente, when `AiDiagnosticCacheService` consulta o mesmo `(userId, portfolioStateHash, modelVersion, promptVersion)`, then retorna o payload em `< 400ms` sem incrementar `tb_ai_usage_counter`.
- Given um usuário Pro/Founder que já consumiu 5 chamadas no dia UTC atual (ou 30 no mês UTC atual), when solicita nova geração, then `AiFinOpsRateLimitService` rejeita com `AiRateLimitExceededException` contendo o último diagnóstico cacheado e o `GlobalExceptionHandler` responde `ProblemDetail` HTTP `429`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Finding | Layer | Verdict | Route | Evidence |
|---------|-------|---------|-------|----------|
| `JdbcAiUsageCounterAdapter.ensureRowExists` captura `DataIntegrityViolationException` amplo | edge-case-hunter | medium | patch | Confirmado em `JdbcAiUsageCounterAdapter.java:183-187`; violação de FK `users.id` seria engolida e viraria HTTP 429; trocar para `DuplicateKeyException`. |
| `ATOMIC_INCREMENT_SQL` usa subquery não bloqueante sob `REPEATABLE READ` (concorrência no limite mensal 29/30) | edge-case-hunter, blind-hunter | high | patch | Confirmado em `JdbcAiUsageCounterAdapter.java:75-92`; leitura não bloqueante em InnoDB permite duas transações concorrentes passarem de 29 para 31; adicionar `SELECT ... FOR UPDATE` nas linhas do mês antes do `UPDATE`. |
| `AiFinOpsRateLimitService.consumeQuotaOrThrow` lê `LocalDate.now(clock)` e `LocalDateTime.now(clock)` separados | blind-hunter | low | patch | Confirmado em `AiFinOpsRateLimitService.java:1045-1046`; derivar `todayUtc = nowUtc.toLocalDate()` a partir de uma única leitura. |
| `AiCacheAndFinOpsIT` não testa subquery mensal no dia 1º UTC e não assere `getMonthlyCallCount` / `getMonthlyCallsUsed()` | verification-gap, blind-hunter | medium | patch | Confirmado em `AiCacheAndFinOpsIT.java:653-674` e `JdbcAiUsageCounterAdapter.java:100-106`; no dia 1º do mês o teste cai no limite diário (`5`) e nunca valida `getMonthlyCallCount` nem `ex.getMonthlyCallsUsed()`. |
| `GlobalExceptionHandler` sem handler para `AiFreeTierNotAllowedException` | blind-hunter | medium | patch | Confirmado em `GlobalExceptionHandler.java`; spec exige não expor HTTP 500 na 5.1; adicionar `@ExceptionHandler` retornando `ProblemDetail` 403. |
| `GlobalExceptionHandler.handleAiRateLimitExceeded` expõe `userId` em `latestDiagnostic` e falta teste com `null` | blind-hunter | low | patch | Confirmado em `GlobalExceptionHandler.java:431` e `GlobalExceptionHandlerAiTest.java`; omitir `userId` do payload serializado e cobrir `latestDiagnostic == null`. |
| `tb_ai_diagnostic_cache.json_payload` usa `TEXT` (64 KB) sem validação | blind-hunter | low (rejected) | — | Design Notes da spec escolheu explicitamente `TEXT` para compatibilidade MySQL + H2; diagnósticos estruturados são pequenos (< 10 KB). |
| `tb_ai_diagnostic_cache` sem job de limpeza de expirados nem unique na chave de lookup | blind-hunter | low (rejected) | — | Job de expurgo periódico está fora do escopo da 5.1; `findFirst...OrderByCreatedAtDesc` trata múltiplas linhas sem erro. |
| Métodos `deleteByUserId` sem uso e sem teste nos ports/adapters | blind-hunter | low | patch | Confirmado em `JpaAiDiagnosticCacheAdapter` e `JdbcAiUsageCounterAdapter`; adicionar cobertura em `AiCacheAndFinOpsIT`. |
| Lacunas de validação em `AiFinOpsProperties` (`monthly >= daily`) e testes triviais | blind-hunter | low (rejected) | — | Validações `>= 0` e `> 0` já existem; exigir `monthly >= daily` restringiria configs de teste sem ganho real. |
| Deriva de numeração `39` vs `40` na spec | blind-hunter | false (rejected) | — | `39-create-cerrado-tables.yaml` já existia na `main`; mudança para `40` é obrigatória pela regra de imutabilidade de migrations; regra de revisão proíbe editar a spec para corrigir achado. |
| Risco de flakiness em `elapsedMs < 400L` na primeira query JPA fria | blind-hunter | low | patch | Confirmado em `AiCacheAndFinOpsIT.java:570-581`; fazer warmup antes da medição mantendo a asserção `< 400ms`. |

## Design Notes

- **Atomicidade FinOps e agregação mensal:** O contador usa `unique(user_id, usage_date)` com incremento atômico via JDBC (`UPDATE ... WHERE daily_call_count < :dailyLimit`) e soma mensal `SUM(daily_call_count)` no intervalo `[primeiroDiaDoMesUTC, hojeUTC]`, eliminando drift de coluna `monthly_call_count` redundante e race conditions.
- **Compatibilidade MySQL + H2:** Usar `TEXT` em `json_payload` e `VARCHAR(64)` para `portfolio_state_hash`.

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: 0 erros/warnings, todos os testes unitários passando.
- `cd backend && mvn test -pl api -Dtest=*IT` -- expected: todos os `*IT` (incluindo `AccountDeletionIT` e novos `Ai*IT`) passando com H2 + Liquibase.
- `helm lint helm/graham-select && helm template test-graham helm/graham-select --debug >/dev/null` -- expected: lint e renderização Helm sem erros.
