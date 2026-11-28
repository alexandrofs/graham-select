---
title: 'Story 3.2: Motor de Domínio da Hierarquia do Cerrado, Aritmética de Metas e Redistribuição de Teto'
type: 'feature'
created: '2026-11-28'
status: 'done'
baseline_commit: '0d701baa0ca611172fc4dff378c90f99e811215c'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '/Users/alexandrofs/projects/graham-select/docs/bmad/implementation-artifacts/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O modelo relacional e as entidades do Diagrama do Cerrado já existem (Story 3.1), mas ainda falta o motor determinístico de domínio que valide a soma estrita de `100.00%` das 5 Macro Classes, aplique a hierarquia de subclasses, imponha a Quarentena de Aporte (notas `1–4` ou evento corporativo) e redistribua iterativamente excedentes do Teto Máximo de Risco sem deixar resíduos indevidos nem usar ponto flutuante.

**Approach:** Implementar o serviço de domínio puro e imutável `CerradoAllocationEngine` (e seus modelos de entrada/saída, validação de hierarquia `MacroClass`/`CerradoSubClass` e exceção `CerradoValidationException`) em `backend/common`, mapear `CerradoValidationException` para HTTP `422 Unprocessable Entity` no `GlobalExceptionHandler` em `backend/api`, e cobrir exaustivamente todos os cenários matemáticos e de borda com testes unitários JUnit 5.

## Boundaries & Constraints

**Always:**
- Usar estritamente `BigDecimal` com escala intermediária `4` (`RoundingMode.HALF_EVEN`) nos cálculos proporcionais e de redistribuição, e escala `2` (`RoundingMode.HALF_EVEN`) nas saídas percentuais consolidadas, com reconciliação determinística de centésimos (Largest Remainder / ajuste `±0.01%` por maior resto fracionário, maior nota e `ticker` A–Z, sem violar tetos) para garantir `soma(effectiveTargetPct) + unallocatedResidualPct == Meta_C%` e `unallocatedResidualPct >= 0.00`; proibido `float`/`double`.
- Validar em `CerradoPreferences` que as 5 metas das Macro Classes sejam não-negativas (`>= 0.00`), estejam entre `0.00` e `100.00` com escala `<= 2`, somem exatamente `100.00%` (`compareTo(new BigDecimal("100.00")) == 0`), e `grahamMultiplier > 0`, lançando `CerradoValidationException` caso contrário.
- Rejeitar lista de ativos `null` ou contendo elemento `null`; normalizar `ticker` (`trim().toUpperCase(Locale.ROOT)`) e rejeitar ticker em branco, duplicado (case-insensitive) ou `qualityGrade` fora de `1..10` com `CerradoValidationException`.
- Validar coerência entre `MacroClass` e `CerradoSubClass` (`OUTROS_MANUAL` permitido em qualquer `MacroClass`; demais subclasses restritas à sua respectiva `MacroClass`).
- Aplicar Quarentena de Aporte (`0.00%` e exclusão do denominador da classe) para qualquer ativo com `qualityGrade` entre `1` e `4` ou `quarantineCorporateEvent == true`.
- Validar `customRiskCeiling` quando não-nulo: aceitar apenas `(0.00, 100.00]` (senão lançar `CerradoValidationException`). Aplicar Teto Máximo de Risco sobre o patrimônio total para ativos de risco emissor único (notas `8–10`: `10.00%`; notas `5–7`: `6.00%`; ou `customRiskCeiling` quando informado). Isentar títulos públicos soberanos (`TREASURY_POST_FIXED`, `TREASURY_IPCA`, `TREASURY_PREFIXED`) e ETFs de índices amplos (`isDiversifiedBroadEtf == true` em `CerradoAssetInput`, atributo de catálogo não persistido em `CerradoAssetGrade`), limitando-os à `Meta_C%` (ou `customRiskCeiling`, se explicitamente informado).
- Redistribuir iterativamente o excedente de ativos saturados (limitando todos os saturados da rodada simultaneamente) de forma proporcional às notas entre os ativos elegíveis não-saturados da mesma Macro Classe até `surplus <= 0`, não houver elegíveis não-saturados ou o incremento distribuível em escala `4` ser `0` (reportando eventual resíduo em `unallocatedResidualPct` sem violar tetos).
- Ordenar saídas deterministicamente (`MacroClass` na ordem do enum; ativos por `ticker` A–Z) em coleções imutáveis.
- Mapear `CerradoValidationException` para HTTP `422 Unprocessable Entity` (RFC 7807 `ProblemDetail`) no `GlobalExceptionHandler` com `log.error(..., ex.getMessage(), ex)`.

**Never:**
- Criar endpoints REST (`/api/v1/cerrado/*`, escopo da Story 3.3), alterar migrations Liquibase ou acoplar `backend/common` a frameworks web/JPA.
- Permitir que a redistribuição ultrapasse o teto individual de outro ativo elegível ou a `Meta_C%` da Macro Classe.
- Forçar venda ou zerar posição patrimonial existente de ativo em quarentena (a quarentena restringe apenas a meta de novos aportes = `0.00%`).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Soma das Macro Classes != 100.00% ou escala > 2 ou `grahamMultiplier <= 0` | Metas somando `95.00%` / `105.00%`, valor negativo, `33.333` ou `grahamMultiplier = 0` | Rejeição imediata antes do cálculo de ativos | `CerradoValidationException` (HTTP `422`) |
| Subclasse incompatível, `ticker` inválido/duplicado ou `customRiskCeiling` fora de `(0, 100]` | `ACOES_BR` + `FII_BRICK`, `"petr4"` e `"PETR4"`, `assets == null`, `customRiskCeiling = 0` | Rejeição da entrada inválida | `CerradoValidationException` (HTTP `422`) |
| Cálculo proporcional simples e reconciliação de centésimos | `ACOES_BR = 20.00%` com 3 ativos nota `10` (ou `30.00%` com `10, 8, 6, 6`) | Soma exata `20.00%` (ex.: `6.67%`, `6.67%`, `6.66%`) / `10.00%, 8.00%, 6.00%, 6.00%` sem estourar `Meta_C%` | N/A |
| Saturação de teto + redistribuição iterativa | `ACOES_BR = 25.00%`, ativo A nota `10` (teto `10%`), B nota `7` (teto `6%`), C nota `9` (teto `10%`), D nota `8` (teto `10%`) | A saturado em `10%`, B saturado em `6%`; excedente redistribuído proporcionalmente entre C e D respeitando seus tetos `10%` | N/A |
| Saturação total da classe | `CRIPTO = 15.00%`, apenas 1 ativo não-isento nota `10` (teto `10.00%`) | Ativo recebe `10.00%` (`ceilingApplied = true`); `unallocatedResidualPct = 5.00%` na classe | N/A |
| Isenção de Tesouro Direto e ETF amplo | `RENDA_FIXA = 40.00%` com 1 ativo `TREASURY_IPCA` nota `8` ou `INTERNACIONAL = 20.00%` com `IVVB11` (`isDiversifiedBroadEtf = true`) | Recebe `40.00%` / `20.00%` sem corte no teto de `10%` | N/A |
| Quarentena por nota 1–4 ou evento corporativo | Ativo com `qualityGrade = 4` ou `quarantineCorporateEvent = true` na classe com outros ativos elegíveis | `effectiveTargetPct = 0.00%`, `quarantined = true`, excluído do denominador; classe soma `Meta_C%` nos demais | N/A |
| Todos os ativos da classe em quarentena ou classe vazia | `FIIS = 20.00%`, nenhum ativo elegível | Todos com `0.00%`; `unallocatedResidualPct = 20.00%` | N/A |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/model/MacroClass.java` -- Enum das 5 Macro Classes; reutilizar como chave de agregação e meta.
- `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/model/CerradoSubClass.java` -- Enum de subclasses; adicionar método `belongsTo(MacroClass)` para validação hierárquica.
- `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/model/CerradoPreferences.java` -- Preferências de metas das 5 Macro Classes; reutilizar como entrada do motor.
- `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/model/CerradoAssetGrade.java` -- Nota de qualidade, `customRiskCeiling` e `quarantineCorporateEvent`; manter `IllegalArgumentException` para não quebrar `CerradoDomainModelTest`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java` -- Handler global RFC 7807; adicionar `@ExceptionHandler(CerradoValidationException.class)` retornando `HttpStatus.UNPROCESSABLE_ENTITY`.
- `backend/common/src/test/java/afsdigital/grahamselect/common/cerrado/domain/model/CerradoDomainModelTest.java` -- Testes existentes dos modelos; manter intacto.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/model/CerradoSubClass.java` -- Mapear cada constante do enum à sua `MacroClass` (e tratar `OUTROS_MANUAL` como compatível com todas) via `belongsTo(MacroClass)` -- Garante integridade da hierarquia Nível 1 -> Nível 2.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/exceptions/CerradoValidationException.java` -- Criar exceção de domínio estendendo `RuntimeException` para violações de regra do Cerrado (soma != 100%, valores negativos/escala, `grahamMultiplier <= 0`, subclasse incompatível, `ticker` inválido/duplicado, `customRiskCeiling` inválido) -- Desacopla erro de validação do HTTP em `common`.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/service/CerradoAllocationEngine.java` e modelos I/O (`CerradoAssetInput`, `CerradoAssetAllocation`, `CerradoMacroClassSummary`, `CerradoAllocationResult`) -- Implementar motor stateless e determinístico: validar preferências e entradas, normalizar tickers, calcular quarentena, isenção (`TREASURY_*` ou `isDiversifiedBroadEtf`), tetos (`10%`/`6%`/`customRiskCeiling`), redistribuição iterativa com `BigDecimal` escala `4` `HALF_EVEN` e reconciliação de centésimos para escala `2` -- Cumpre integralmente o algoritmo e invariantes da história.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java` -- Adicionar handler para `CerradoValidationException` retornando `ProblemDetail` com status `422 Unprocessable Entity` e `log.error(..., ex.getMessage(), ex)` -- Garante resposta HTTP 422 exigida nos critérios de aceite.
- [x] `backend/common/src/test/java/afsdigital/grahamselect/common/cerrado/domain/service/CerradoAllocationEngineTest.java` e `backend/api/src/test/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandlerTest.java` -- Cobrir toda a matriz I/O e casos de borda (reconciliação de centésimos `HALF_EVEN`, redistribuição multi-iteração, saturação total, isenções, quarentena por nota e evento corporativo, teto customizado, duplicidade case-insensitive e HTTP 422) -- Valida precisão decimal e ausência de regressões.

**Acceptance Criteria:**
- Given um `CerradoAllocationEngine` stateless e imutável em `backend/common`, when executado com `CerradoPreferences` válidas e lista de ativos, then todas as operações usam `BigDecimal` (`HALF_EVEN`), nenhum ativo não-isento excede seu Teto Máximo de Risco e a soma das metas efetivas + `unallocatedResidualPct` em cada Macro Classe iguala `Meta_C%` exatamente em escala `2`.
- Given uma `CerradoValidationException` lançada pelo domínio, when interceptada pelo `GlobalExceptionHandler` na camada `api`, then a resposta HTTP retorna status `422 Unprocessable Entity` no formato RFC 7807 `ProblemDetail`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Layer | Location | Claim | Verdict | Route / Disposition | Evidence |
|---|---|---|---|---|---|
| edge-case-hunter | `CerradoAssetInput.java:25-35` | `fromGrade(null, ...)` throws NPE instead of `CerradoValidationException` | `low` | `patch` | `fromGrade` dereferencia `grade` sem checagem de nulidade; guarda de 1 linha resolve. |
| edge-case-hunter | `CerradoAllocationEngine.java:475-481` | `customRiskCeiling` with > 2 decimal places rounds `riskCeilingPct` / misaligns cap | `medium` | `patch` | `validateAndNormalizeAssets` valida apenas `(0, 100]` sem checar `scale() <= 2`, diferentemente de `validateTarget`. |
| blind-hunter | `CerradoAllocationEngine.java:785-791` | Ausência de validação de escala decimal para `customRiskCeiling` | `medium` | `patch` | Mesma causa raiz do achado de `edge-case-hunter`; corrigido no mesmo patch. |
| blind-hunter | `CerradoAllocationEngine.java:621-658` | Loop de `reconcileToScale2` não garante absorção completa de `centsDiff` | `false` | Rejected | Com `metaC` e `customRiskCeiling` em escala `<= 2`, todo ativo não-saturado tem folga `>= 0.01` para `+0.01`, e `floorSum2 > targetTotal2 >= 0` garante ativos `>= 0.01` para `-0.01`. |
| blind-hunter | `CerradoAllocationEngine.java:437,449-456` | Contradição entre `ceilingExempt = true` e `ceilingApplied = true` com `customRiskCeiling` | `false` | Rejected | Comportamento explícito das Boundaries: isenção aplica-se à trava automática, mas `customRiskCeiling` informado pelo usuário prevalece. |
| blind-hunter | `CerradoAssetInput.java:199-209` | `fromGrade(null, ...)` lança NPE sem `CerradoValidationException` | `low` | `patch` | Mesma causa raiz do achado de `edge-case-hunter`; corrigido no mesmo patch. |
| blind-hunter | `CerradoAllocationEngine.java:723-732` | Branches `value == null` e `grahamMultiplier == null` inatingíveis via `CerradoPreferences` | `false` | Rejected | Guards defensivos inofensivos contra mocks/subclasses de `CerradoPreferences`. |
| blind-hunter | `CerradoAllocationEngine.java:641-658` | Branch `centsDiff < 0` sem cobertura em `CerradoAllocationEngineTest` | `low` | Rejected | `floorSum2 <= sum(allocated4)`, tornando `centsDiff < 0` inatingível em carteiras reais (exigiria >200 ativos na mesma classe arredondando `+0.00005` simultaneamente). |
| blind-hunter | `CerradoAllocationEngineTest.java` | Lacunas em testes de borda (`Meta_C == 0`, `List.of()`, `fromGrade`, `share == headroom`, nulls/negativos) | `low` | `patch` | Adicionar casos de borda faltantes em `CerradoAllocationEngineTest`. |
| blind-hunter | `CerradoAllocationEngine.java:723-726` | Ausência de limite superior/escala para `grahamMultiplier` | `false` | Rejected | Spec exige apenas `grahamMultiplier > 0`; limites de UI/DB pertencem às Stories 3.1/3.3/4.2. |
| blind-hunter | `CerradoAllocationEngine.java:360` | Falta Javadoc sobre papel passivo de `grahamMultiplier` e `grahamFilterEnabled` | `false` | Rejected | Cosmético sem impacto funcional ou de manutenção. |
| blind-hunter | `sprint-status.yaml` / `spec-3-2` | Status `in-progress` no `sprint-status.yaml` e `Verification` sem saída real | `false` | Rejected | Transição final de `sprint-status.yaml` e registro de verificação ocorrem no `step-05-present`. |
| verification-gap | `CerradoAllocationEngineTest.java:186-416` | Classe vazia (`allocatedPct == 0.00`, `unallocatedResidualPct == metaC`) e totais globais separados sem asserção | `medium` | `patch` | Mutação de troca entre `allocatedPct` e `unallocatedResidualPct` no ramo vazio passaria nos 9 testes atuais. |

## Verification

**Commands:**
- `mvn clean test` (em `backend/`) -- expected: 100% dos testes unitários (incluindo `CerradoAllocationEngineTest` e `GlobalExceptionHandlerTest`) aprovados, zero erros.
