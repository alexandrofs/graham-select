---
title: 'Story 3.3: API REST para Gestão de Metas de Macro Classes e Templates de Onboarding'
type: 'feature'
created: '2026-11-28'
status: 'done'
baseline_commit: '846aafdc38b189b9c8336c1d1ad262ffce1f40a2'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '/Users/alexandrofs/projects/graham-select/docs/bmad/implementation-artifacts/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O modelo de dados (Story 3.1) e o motor de domínio do Diagrama do Cerrado (Story 3.2) já existem no backend, mas ainda não há endpoints REST para que o frontend consulte os 3 templates oficiais de onboarding ou persista e consulte as metas das 5 Macro Classes do usuário autenticado.

**Approach:** Expor os serviços de aplicação de templates e preferências do Cerrado em `backend/common`, disponibilizar os endpoints REST autenticados `GET /api/v1/cerrado/templates`, `GET /api/v1/cerrado/preferences` e `PUT /api/v1/cerrado/preferences` via `@RestController` manual em `backend/api` (validando soma exata de `100.00%` com HTTP `422`, registrando log estruturado e invalidando o cache de IA em `tb_ai_diagnostic_cache` na mesma transação), e cobrir o fluxo completo em `CerradoPreferencesIT`.

## Boundaries & Constraints

**Always:**
- Retornar em `GET /api/v1/cerrado/templates` os 3 perfis oficiais sem pré-seleção automática, com `defaultNeutralGrade = 6` e `cvmDisclaimer` explícito:
  - `EQUILIBRADO_CERRADO` (*Modelo Equilibrado Cerrado*): `targetAcoesBr: 39.00`, `targetFiis: 30.00`, `targetRendaFixa: 20.00`, `targetInternacional: 10.00`, `targetCripto: 1.00`.
  - `MODERADO_PATRIMONIAL` (*Modelo Moderado Patrimonial*): `targetAcoesBr: 20.00`, `targetFiis: 25.00`, `targetRendaFixa: 45.00`, `targetInternacional: 10.00`, `targetCripto: 0.00`.
  - `ACUMULACAO_ARROJADA` (*Modelo Acumulação Arrojada*): `targetAcoesBr: 45.00`, `targetFiis: 25.00`, `targetRendaFixa: 10.00`, `targetInternacional: 15.00`, `targetCripto: 5.00`.
- Obter o `userId` autenticado exclusivamente via `Authentication.getName()` (ID numérico `users.id`) com parsing seguro (retornando HTTP `401` se ausente ou não numérico) e isolar leitura/gravação por `userId`.
- Validar os campos brutos das 5 metas e `grahamMultiplier` (`BigDecimal`, escala `<= 2`, metas em `[0.00, 100.00]`, soma `== 100.00%`, `grahamMultiplier` em `(0.00, 99.99]`) via `CerradoAllocationEngine` **antes** de construir `CerradoPreferences`, evitando que `@Valid`/`IllegalArgumentException` convertam erros de domínio em HTTP `400` em vez de `422`.
- Em `PUT /api/v1/cerrado/preferences`, tratar `grahamMultiplier` e `grahamFilterEnabled` como opcionais: se omitidos/`null`, preservar os valores já persistidos do usuário (ou usar `1.30` / `true` no primeiro cadastro).
- Executar upsert idempotente em `tb_cerrado_preferences` (preservando `id` existente e `ZoneOffset.UTC`), invalidar `tb_ai_diagnostic_cache` via `AiDiagnosticCacheService.invalidateByUserId(userId)` sob uma única transação `@Transactional` e registrar log `INFO` estruturado sem PII/tokens.
- Retornar HTTP `404` (`ProblemDetail` via `CerradoPreferencesNotFoundException`) em `GET /api/v1/cerrado/preferences` quando o usuário não possuir preferências salvas.

**Never:**
- Alterar migrations Liquibase existentes ou criar endpoints de notas de ativos (`/api/v1/cerrado/grades`, escopo da Story 3.5).
- Usar `float`/`double`, `jwt.getSubject()`, ou retornar sugestões personalizadas / pré-seleção automática de template.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Consulta de templates | `GET /api/v1/cerrado/templates` autenticado | HTTP `200` (`templates[]`, `defaultNeutralGrade: 6`, `cvmDisclaimer`) sem pré-seleção | `401` se não autenticado |
| Salvamento válido (insert e update) | `PUT /api/v1/cerrado/preferences` com 5 metas somando `100.00%` (com ou sem `grahamMultiplier`/`grahamFilterEnabled`) | HTTP `200` com preferências salvas (`updatedAt` UTC), valores Graham preservados se omitidos no update, e cache de IA do usuário removido | `401` se não autenticado |
| Soma != `100.00%`, negativo, nulo, escala > 2 ou `grahamMultiplier` fora de `(0, 99.99]` | `PUT /api/v1/cerrado/preferences` com soma `98.00%`, campo `null`, `33.333` ou `grahamMultiplier = 100.00` | Nenhuma persistência; cache de IA intacto | HTTP `422` (`ProblemDetail` via `CerradoValidationException`) |
| Consulta de preferências | `GET /api/v1/cerrado/preferences` com ou sem registro salvo | HTTP `200` com preferências do usuário autenticado | HTTP `404` (`ProblemDetail`) quando ausente |
| Isolamento multi-tenant | Usuário B salva/consulta após Usuário A ter preferências e cache de IA | Preferências e cache de IA do Usuário A permanecem intactos; Usuário B vê apenas seus dados | N/A |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/service/CerradoAllocationEngine.java` -- Expor validação pública de metas + `grahamMultiplier` (`(0.00, 99.99]`, escala `<= 2`) para reuso sem duplicação.
- `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/application/repository/CerradoPreferencesPort.java` -- Port existente (`findByUserId`, `save`, `deleteByUserId`); reutilizar.
- `backend/api/src/main/java/afsdigital/grahamselect/api/cerrado/infrastructure/persistence/CerradoPreferencesPersistenceAdapter.java` -- Ajustar `save()` para buscar `id` existente por `userId` quando `domain.getId() == null`, evitando violação de `UNIQUE(user_id)` em updates.
- `backend/common/src/main/java/afsdigital/grahamselect/common/ai/application/service/AiDiagnosticCacheService.java` -- Adicionar `invalidateByUserId(Long userId)` delegando para `AiDiagnosticCachePort.deleteByUserId(userId)`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java` -- Já trata `CerradoValidationException` (`422`); adicionar handler `404` para `CerradoPreferencesNotFoundException`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java` -- Referência para parsing seguro de `Authentication.getName()` (`Long`).
- `backend/api/src/test/java/afsdigital/grahamselect/api/user/UserProfileIT.java` -- Referência para testes `*IT` com `BaseRepositoryIT`, `@AutoConfigureMockMvc` e `@MockBean JwtDecoder`.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/service/CerradoAllocationEngine.java` e `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/model/CerradoTemplate.java` -- Expor `validateTargetValues(...)` / `validatePreferences(...)` com checagem de escala `<= 2` e teto `99.99` para `grahamMultiplier`, e criar `CerradoTemplate` + `CerradoTemplateCatalog` -- Centraliza validações e templates no domínio puro.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/ai/application/service/AiDiagnosticCacheService.java`, `CerradoPreferencesNotFoundException.java` e `CerradoPreferencesService.java` -- Adicionar `invalidateByUserId(Long)` em `AiDiagnosticCacheService` e criar `CerradoPreferencesService` (validar valores brutos antes de construir `CerradoPreferences`, preservar configurações Graham no update quando omitidas, salvar, invalidar cache de IA, consultar preferências e listar templates) -- Orquestra caso de uso com invalidação de cache.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/cerrado/infrastructure/persistence/CerradoPreferencesPersistenceAdapter.java` e `GlobalExceptionHandler.java` -- Garantir upsert por `userId` em `save()` e mapear `CerradoPreferencesNotFoundException` para HTTP `404` (`ProblemDetail`) -- Evita erro de constraint única e padroniza respostas RFC 7807.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/cerrado/infrastructure/spring/CerradoConfiguration.java` e `backend/api/src/main/java/afsdigital/grahamselect/api/cerrado/web/CerradoController.java` (+ DTOs) -- Registrar beans Spring, expor `GET /api/v1/cerrado/templates`, `GET /api/v1/cerrado/preferences` e `PUT /api/v1/cerrado/preferences` (`@Transactional` no `PUT` cobrindo persistência + invalidação de cache) como `@RestController` manual com `Authentication.getName()` seguro (`401`) e log `INFO` sem PII -- Entrega contrato REST seguro e atômico.
- [x] `backend/common/src/test/java/afsdigital/grahamselect/common/cerrado/application/service/CerradoPreferencesServiceTest.java` e `backend/api/src/test/java/afsdigital/grahamselect/api/cerrado/CerradoPreferencesIT.java` -- Cobrir toda a matriz I/O: templates, insert/update preservando Graham, soma `!= 100.00%` / `null` / escala > 2 / `grahamMultiplier` inválido (`422`), `404`, `401`, isolamento por `userId` e invalidação real de `tb_ai_diagnostic_cache` -- Valida contrato e invariantes.

**Acceptance Criteria:**
- Given um usuário autenticado consultando `GET /api/v1/cerrado/templates`, when a requisição é processada, then retorna HTTP `200` com os 3 perfis oficiais somando `100.00%`, `defaultNeutralGrade = 6` e disclaimer CVM sem pré-seleção.
- Given um usuário autenticado submetendo `PUT /api/v1/cerrado/preferences` com metas válidas somando `100.00%`, when persistido, then a configuração é salva isolada por `user_id` em `tb_cerrado_preferences`, o cache de IA do usuário em `tb_ai_diagnostic_cache` é invalidado na mesma transação e um log `INFO` estruturado é registrado.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Layer | Location | Claim | Verdict | Route / Disposition | Evidence |
|---|---|---|---|---|---|
| edge-case-hunter | `CerradoPreferencesPersistenceAdapter.java:29-33` | Concorrência em primeiro save simultâneo pode disparar `DataIntegrityViolationException` | `low` | Rejected | Colisão simultânea de primeiro onboarding do mesmo usuário é rara e retry em `@Transactional` exige `REQUIRES_NEW`/SQL nativo (complexidade desproporcional). |
| verification-gap | `CerradoPreferencesPersistenceAdapter.java:29-32` | Branch de upsert por `userId` quando `id == null` sem cobertura de teste | `medium` | `patch` | Coberto por `CerradoPersistenceIT.shouldUpsertPreferencesByUserIdWhenIdIsNull`. |
| verification-gap | `CerradoAllocationEngine.java:447-449` | Rejeição de `grahamMultiplier` com escala > 2 (`1.333`) sem teste | `medium` | `patch` | Coberto em `CerradoPreferencesServiceTest` e `CerradoPreferencesIT`. |
| blind-hunter | `CerradoController.java:145` | `@RequestBody(required = false)` retorna `422` em vez de `400` para body vazio | `low` | `patch` | Removido `(required = false)` e adicionado teste `400` para JSON vazio/malformado em `CerradoPreferencesIT`. |
| blind-hunter | `CerradoPreferencesPersistenceAdapter.java` | Risco de race no insert simultâneo e falta de verificação de `userId` quando `id != null` | `false` | Rejected | Race já tratado acima; `CerradoPreferencesPort` só é chamado por `CerradoPreferencesService` com `userId` autenticado e seu próprio `id`. |
| blind-hunter | `CerradoPersistenceIT.java:91-115` | Mudança para `preferencesJpaRepository.saveAndFlush` mascara upsert e falta teste do adapter | `false` | Rejected | O teste valida a constraint `UNIQUE(user_id)` no banco; o caminho de upsert do adapter foi coberto via `verification-gap` #1. |
| blind-hunter | `CerradoController.java` vs `CerradoPreferencesService.java` | `@Transactional` no controller em vez do service em `common` | `false` | Rejected | `common` é desacoplado de Spring por arquitetura (`AGENTS.md`); `JpaAiDiagnosticCacheAdapter` e `CerradoPreferencesPersistenceAdapter` já têm `@Transactional`, e `CerradoController.savePreferences` unifica a transação HTTP. |
| blind-hunter | `CerradoController.java:116-185` | `try/catch` duplica log `ERROR` já emitido pelo `GlobalExceptionHandler` | `low` | `patch` | Removidos os `try/catch` redundantes em `CerradoController`; `GlobalExceptionHandler` registra cada exceção uma única vez. |
| blind-hunter | `AiDiagnosticCacheService.java:87-92` | `invalidateByUserId` e guarda `userId == null` sem teste unitário | `low` | `patch` | Adicionado `shouldInvalidateCacheByUserIdAndIgnoreNull` em `AiDiagnosticCacheServiceTest`. |
| blind-hunter | `CerradoPreferencesServiceTest.java` / `CerradoPreferencesIT.java` | Lacunas em bordas (`null` userId, negativos, `>100`, `1.333`, `401` no `PUT`, `400` JSON inválido) | `low` | `patch` | Casos de borda adicionados em `CerradoPreferencesServiceTest` e `CerradoPreferencesIT`. |
| blind-hunter | `CerradoTemplateCatalog.java` | Mistura de português com e sem acentuação em `CVM_DISCLAIMER` e descrições | `low` | `patch` | Acentuação corrigida de forma consistente em `CerradoTemplateCatalog`. |
| blind-hunter | `UpdateCerradoPreferencesRequest` | Semântica de omissão/`null` para `grahamMultiplier`/`grahamFilterEnabled` | `false` | Rejected | Comportamento explícito na spec e controller manual fora de `openapi.yaml`. |
| blind-hunter | `sprint-status.yaml` / `spec-3-3` | `sprint-status.yaml` em `in-progress` e seções da spec vazias | `false` | Rejected | `sprint-status.yaml` é atualizado no `step-05-present` e `Review Triage Log` preenchido no `step-04-review`. |

## Design Notes

Contratos JSON principais (controller manual, fora do `openapi.yaml` legado):
```json
// GET /api/v1/cerrado/templates
{
  "defaultNeutralGrade": 6,
  "cvmDisclaimer": "...",
  "templates": [
    {
      "code": "EQUILIBRADO_CERRADO",
      "name": "Modelo Equilibrado Cerrado",
      "description": "...",
      "targetAcoesBr": 39.00, "targetFiis": 30.00, "targetRendaFixa": 20.00,
      "targetInternacional": 10.00, "targetCripto": 1.00
    }
  ]
}
// PUT /api/v1/cerrado/preferences (grahamMultiplier e grahamFilterEnabled opcionais)
{
  "targetAcoesBr": 39.00, "targetFiis": 30.00, "targetRendaFixa": 20.00,
  "targetInternacional": 10.00, "targetCripto": 1.00
}
```

## Verification

**Commands:**
- `mvn clean test` (em `backend/`) -- expected: 100% dos testes unitários aprovados, zero erros.
- `mvn test -pl api -Dtest=CerradoPreferencesIT,CerradoPersistenceIT,AiCacheAndFinOpsIT` (em `backend/`) -- expected: 100% dos testes de integração H2 aprovados.
