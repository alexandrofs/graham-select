---
title: 'Circuit Breaker e Validação de Integridade no Motor de Valuation Graham'
type: 'enhancement'
created: '2026-06-13T16:46:00-03:00'
status: 'ready-for-dev'
context:
  - '{project-root}/docs/bmad/project-context.md'
  - '{project-root}/docs/bmad/planning-artifacts/architecture.md'
  - '{project-root}/docs/bmad/implementation-artifacts/4-3-graham-valuation-engine.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O `GenerateGrahamRecommendationsUseCase` atualmente inclui todos os ativos do Top 20 que tenham `marginOfSafety > 0` sem validar a integridade dos dados fundamentalistas que embasam esse cálculo. Isso cria três riscos concretos:

1. **Dados desatualizados (Stale Data):** Indicadores com `updatedAt` muito antigos (ex: empresa sem dados publicados há semanas) geram recomendações embasadas em dados defasados, sem qualquer aviso ao usuário.
2. **Outlier de preço / Falso-Positivo:** Um dado corrompido pode produzir um `marginOfSafety` absurdamente alto (ex: 900%), fazendo o motor recomendar compra massiva de um ativo cujo dado é inválido — o risco de Falso-Positivo descrito na seção "Risk Mitigations" do PRD.
3. **Falha total silenciosa:** Se o `valuation-service` abortar a geração por exceção não tratada, o Kafka consumer engole o erro sem publicar `valuation-failed`, e o frontend nunca é notificado (o delay hardcoded de 3s do `GrahamRecommendationProvider` simplesmente exibe dados antigos sem contexto).

**Approach:**
- Adicionar validação de `updatedAt` no `GenerateGrahamRecommendationsUseCase`: ativos com `intrinsicValueUpdatedAt` superior a **7 dias** são excluídos do ranking e registrados em `excludedTickers`.
- Adicionar circuit breaker de outlier: ativos com `marginOfSafety > 80%` são suspensos da recomendação com motivo `OUTLIER_PRICE` e log ERROR.
- Enriquecer o `ValuationCompletedEvent` com a lista `excludedTickers` (motivo por ativo) para rastreabilidade.
- Capturar exceções fatais no `ValuationRequestedConsumerService` e publicar um evento `valuation-failed` no Kafka para que o módulo `api` notifique o frontend via SSE (D16).
- No frontend, eliminar o delay hardcoded de 3 segundos substituindo-o por escuta do evento SSE `VALUATION_COMPLETED` (ou `VALUATION_FAILED`) já emitido pela Story 3.5.

## Boundaries & Constraints

**Always:**
- Usar `ZoneOffset.UTC` em todas as comparações de data (`LocalDate.now(ZoneOffset.UTC)`).
- Log WARN para exclusões por stale data; log ERROR para circuit breaker de outlier e falhas fatais.
- Manter todos os testes existentes passando (`mvn clean test` e `flutter analyze` sem regressões).
- O campo `excludedTickers` no evento é opcional — se vazio, omitir no JSON (Jackson `NON_NULL` / `NON_EMPTY`).
- O threshold de stale data (7 dias) e outlier (80%) devem ser constantes nomeadas — nunca magic numbers.

**Ask First:**
- Se a tabela `company_intrinsic_value` não tiver coluna `updated_at` ou equivalente, consultar o dev antes de criar uma migration — pode ser que o timestamp esteja em outra tabela.

**Never:**
- Não fazer chamada síncrona entre módulos `api` e `valuation-service`.
- Não alterar o schema da tabela `graham_recommendations` — os dados de `excludedTickers` trafegam apenas via evento Kafka, não são persistidos.
- Não remover o polling de fallback do frontend — mantê-lo como fallback se SSE não estiver conectado.

## I/O & Edge-Case Matrix

| Cenário | Input / Estado | Saída Esperada | Tratamento de Erro |
|---------|---------------|----------------|-------------------|
| Ativo com `intrinsicValueUpdatedAt` > 7 dias | Top 20 contém MGLU3 com dado de 10 dias atrás | MGLU3 excluído; demais processados normalmente | Log WARN; `excludedTickers: [{ticker: "MGLU3", reason: "STALE_DATA", daysOld: 10}]` no evento |
| Ativo com `marginOfSafety > 80%` | XPTO3 calculado com desconto de 250% (dado corrompido) | XPTO3 bloqueado; demais processados normalmente | Log ERROR com ticker e valor; `excludedTickers: [{ticker: "XPTO3", reason: "OUTLIER_PRICE", value: "250%"}]` no evento |
| Ambas as condições no mesmo ativo | ABCD4 com dado stale E outlier | Excluir uma única vez; motivo = `STALE_DATA` (verificado primeiro) | Log WARN (stale tem precedência sobre outlier) |
| Todos os ativos excluídos | Top 20 inteiramente stale ou outlier | Lista de recomendações vazia publicada; `excludedTickers` com todos os ativos | Log WARN "All ranked companies excluded from recommendations for user {}" |
| Exceção fatal no use case | `NullPointerException` em `portfolioSnapshotPort` | Abortar cálculo; publicar `valuation-failed` no Kafka | Log ERROR com stack trace completo; evento `valuation-failed` com `{userId, errorCode: "INTERNAL_ERROR", message}` |
| SSE conectado no frontend | `VALUATION_COMPLETED` chega via SSE | Chamar `fetchRecommendations()` imediatamente; remover delay de 3s | — |
| SSE desconectado no frontend | Trigger disparado; SSE não conectado | Polling de fallback mantido (delay de 3s) como contingência | Banner de "SSE desconectado" não obrigatório — fallback silencioso |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationCompletedEvent.java` — Enriquecer com campo `excludedTickers` (lista de registros `{ticker, reason, detail}`) e constantes de threshold.
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationFailedEvent.java` — **NOVO**: evento publicado em caso de falha fatal do cálculo.
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java` — Adicionar constante `VALUATION_FAILED_TOPIC = "valuation-failed"` se não existir.
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java` — Adicionar validação de stale data e circuit breaker de outlier antes de gerar recomendações.
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java` — Capturar exceções fatais e publicar `valuation-failed`; enriquecer publicação de `valuation-completed` com `excludedTickers`.
- `backend/common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCaseTest.java` — Adicionar testes para os novos cenários de exclusão.
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/sse/SseNotificationAdapter.java` — Adicionar suporte ao evento `VALUATION_COMPLETED` e `VALUATION_FAILED` para notificação via SSE.
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/ValuationCompletedConsumer.java` — **NOVO**: consumer Kafka no módulo `api` que escuta `valuation-completed` e `valuation-failed` e repassa via SSE ao frontend.
- `frontend/lib/src/features/ranking/presentation/providers/graham_recommendation_provider.dart` — Substituir delay hardcoded de 3s por escuta do evento SSE `VALUATION_COMPLETED`; adicionar tratamento de `VALUATION_FAILED`.

## Tasks & Acceptance

**Execution:**

### Backend — Módulo `common`

- [ ] `ValuationCompletedEvent.java` — Adicionar campo `List<ExcludedTicker> excludedTickers` onde `ExcludedTicker` é um record com `{String ticker, String reason, String detail}`. Adicionar constantes `STALE_DATA_THRESHOLD_DAYS = 7` e `OUTLIER_MARGIN_THRESHOLD = new BigDecimal("0.80")` na classe ou em `ValuationConstants`.
- [ ] `ValuationFailedEvent.java` — Criar novo evento com campos: `userId` (String), `errorCode` (String), `message` (String), `failedAt` (String ISO 8601), `eventId` (UUID). Seguir padrão de `ValuationCompletedEvent.java`.
- [ ] `TopicConstants.java` — Verificar se `VALUATION_FAILED_TOPIC` existe; adicionar `"valuation-failed"` se ausente.
- [ ] `GenerateGrahamRecommendationsUseCase.java` — Antes de calcular o score de cada ativo:
  1. Verificar `company.getIntrinsicValueUpdatedAt()` — se `ChronoUnit.DAYS.between(updatedAt, LocalDate.now(ZoneOffset.UTC)) > STALE_DATA_THRESHOLD_DAYS`, adicionar à lista `excludedTickers` com motivo `STALE_DATA` e `log.warn(...)`. Pular o ativo.
  2. Calcular `marginOfSafety` provisoriamente — se `> OUTLIER_MARGIN_THRESHOLD`, adicionar à lista `excludedTickers` com motivo `OUTLIER_PRICE` e `log.error(...)`. Pular o ativo.
  3. Retornar as recomendações **e** a lista `excludedTickers` — alterar o método `execute` para retornar um record/DTO `GenerationResult { List<GrahamRecommendation> recommendations, List<ExcludedTicker> excludedTickers }` em vez de `void`.

### Backend — Módulo `valuation-service`

- [ ] `ValuationRequestedConsumerService.java` — Envolver o bloco de chamada ao use case em `try/catch(Exception e)`:
  - No `catch`: `log.error("[VALUATION] Fatal error processing valuation-requested for userId: {}. Error: {}", userId, e.getMessage(), e)` e publicar `ValuationFailedEvent` no tópico `valuation-failed`.
  - No caminho feliz: usar o `GenerationResult` retornado para construir `ValuationCompletedEvent` com `excludedTickers` preenchido.
- [ ] `ValuationServiceConfiguration.java` — Adicionar bean do novo publisher `ValuationFailedPublisher` se necessário.

### Backend — Módulo `api`

- [ ] `ValuationCompletedConsumer.java` — **NOVO**: `@KafkaListener` no módulo `api` escutando tópicos `valuation-completed` e `valuation-failed`. Ao consumir:
  - `valuation-completed`: chamar `sendPortfolioUpdateNotificationUseCase` com evento `VALUATION_COMPLETED` (e `excludedTickers` se não vazio) para notificar o frontend via SSE.
  - `valuation-failed`: chamar `sendPortfolioUpdateNotificationUseCase` com evento `VALUATION_FAILED` e a mensagem de erro.
  - Log INFO na entrada: `"Consumed valuation-completed event for userId: {}, recommendations: {}, excluded: {}"`.
- [ ] `NotificationPort.java` — Verificar se é necessário adicionar sobrecarga `sendValuationUpdate(String userId, String eventType, Object payload)` ou se o método existente `sendPortfolioUpdate(userId)` é suficiente para carregar o payload estendido. Adaptar conforme necessário sem quebrar Story 3.5.
- [ ] `SseNotificationAdapter.java` — Garantir que o evento SSE enviado ao Flutter inclui o campo `event` (ex: `VALUATION_COMPLETED` ou `VALUATION_FAILED`) e o payload JSON para que o frontend possa distinguir os tipos.

### Frontend

- [ ] `graham_recommendation_provider.dart` — No método `triggerCalculation()`:
  - Remover `await Future.delayed(const Duration(seconds: 3))`.
  - Registrar um listener único no stream SSE do `NotificationService` que aguarda evento com `event == 'VALUATION_COMPLETED'` ou `event == 'VALUATION_FAILED'` para o usuário atual.
  - Ao receber `VALUATION_COMPLETED`: chamar `fetchRecommendations()` automaticamente.
  - Ao receber `VALUATION_FAILED`: setar `state = RecommendationState.error` com `errorMessage` da mensagem do evento.
  - Manter o polling de fallback (delay de 3s) apenas se o SSE não estiver conectado (`!portfolioProvider.isReceivingLiveUpdates`).

### Testes

- [ ] `GenerateGrahamRecommendationsUseCaseTest.java` — Adicionar testes:
  - Ativo com `intrinsicValueUpdatedAt` de 10 dias atrás → excluído com motivo `STALE_DATA`
  - Ativo com `marginOfSafety = 0.95` → excluído com motivo `OUTLIER_PRICE`; log ERROR verificado com `verify(appender)`
  - Ativo com ambas as condições → excluído uma vez com motivo `STALE_DATA`
  - Todos os ativos excluídos → `GenerationResult.recommendations` vazio; `excludedTickers` com todos os ativos
- [ ] `ValuationRequestedConsumerServiceTest.java` — Adicionar teste: exceção no use case → evento `valuation-failed` publicado; log ERROR verificado.

**Acceptance Criteria:**

- Given um ativo do Top 20 com dado fundamentalista com `updatedAt` superior a 7 dias, when o `GenerateGrahamRecommendationsUseCase` é executado, then o ativo é excluído das recomendações com motivo `STALE_DATA` registrado no log e no `ValuationCompletedEvent.excludedTickers`.
- Given um ativo com `marginOfSafety` calculado superior a 80%, when o use case processa o ativo, then o circuit breaker é ativado, o ativo é excluído com motivo `OUTLIER_PRICE`, e um log ERROR é emitido.
- Given uma exceção fatal no `ValuationRequestedConsumerService`, when o processamento falha, then um evento `valuation-failed` é publicado no Kafka e o frontend recebe notificação via SSE com `VALUATION_FAILED`.
- Given que o usuário dispara o recálculo com SSE conectado, when o `valuation-completed` chega via SSE, then o `GrahamRecommendationProvider` busca as recomendações imediatamente (sem delay de 3s).

## Verification

**Commands:**
- `mvn clean test -pl backend/common` — esperado: todos os testes passando, incluindo novos cenários de circuit breaker.
- `mvn clean test -pl backend/valuation-service` — esperado: consumer tests passando com mock de `valuation-failed`.
- `mvn clean test -pl backend/api` — esperado: `ValuationCompletedConsumerTest` passando.
- `flutter analyze` — esperado: zero warnings/erros.
- `flutter test test/features/ranking/` — esperado: testes do provider atualizados passando sem delay hardcoded.

## Suggested Review Order

**Módulo `common` — Contratos e Use Case**

- Novos campos em `ValuationCompletedEvent` e novo `ValuationFailedEvent`
  [`ValuationCompletedEvent.java`](../../../backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationCompletedEvent.java)

- Lógica de circuit breaker e stale data no use case principal
  [`GenerateGrahamRecommendationsUseCase.java`](../../../backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java)

**Módulo `valuation-service` — Consumer Kafka**

- Captura de exceção fatal e publicação de `valuation-failed`
  [`ValuationRequestedConsumerService.java`](../../../backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java)

**Módulo `api` — Ponte Kafka → SSE**

- Novo consumer `ValuationCompletedConsumer` e integração com `SseNotificationAdapter`
  [`ValuationCompletedConsumer.java`](../../../backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/ValuationCompletedConsumer.java)

**Frontend — Eliminação do delay hardcoded**

- Substituição do `Future.delayed` por escuta SSE no provider
  [`graham_recommendation_provider.dart`](../../../frontend/lib/src/features/ranking/presentation/providers/graham_recommendation_provider.dart)

**Testes**

- Novos cenários de circuit breaker
  [`GenerateGrahamRecommendationsUseCaseTest.java`](../../../backend/common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCaseTest.java)
