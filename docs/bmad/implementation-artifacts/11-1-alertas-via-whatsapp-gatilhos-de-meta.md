---
baseline_commit: 55c87b8b836894ffd3a786fa8cb37ef89da6c9db
---
# Story 11.1: Alertas via WhatsApp (Gatilhos de Meta)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como um investidor (usuário autenticado do Graham-Select),
Eu quero configurar meu número de WhatsApp com consentimento explícito (LGPD) e receber alertas acionáveis sobre gatilhos de rebalanceamento, marcos de meta financeira e anúncios/pagamentos de proventos,
Para que eu possa acompanhar minha carteira proativamente e tomar decisões rápidas de aporte ou ajuste.

## Acceptance Criteria

1. **Persistência e Imutabilidade Liquibase (`35-create-whatsapp-notifications-tables.yaml`):**
   - **Dado** o banco de dados gerenciado pelo Liquibase
   - **Quando** a aplicação inicializa e executa a nova migration `35-create-whatsapp-notifications-tables.yaml` (registrada em `db.changelog-master.yaml`, sem alterar nenhuma migration `01` a `34`)
   - **Então** é criada a tabela `whatsapp_notification_preferences` com colunas: `id` (`VARCHAR(36)` PK), `user_id` (`VARCHAR(255)` NOT NULL UNIQUE), `phone_number` (`VARCHAR(20)` NOT NULL), `opted_in` (`BOOLEAN` NOT NULL DEFAULT FALSE), `opted_in_at` (`TIMESTAMP` NULL), `notify_dividends` (`BOOLEAN` NOT NULL DEFAULT TRUE), `notify_rebalance_triggers` (`BOOLEAN` NOT NULL DEFAULT TRUE), `notify_goal_milestones` (`BOOLEAN` NOT NULL DEFAULT TRUE), `rebalance_drift_threshold_pct` (`DECIMAL(5,2)` NOT NULL DEFAULT 5.00), `created_at` (`TIMESTAMP` NOT NULL), `updated_at` (`TIMESTAMP` NOT NULL)
   - **E** é criada a tabela `whatsapp_notification_logs` com colunas: `id` (`VARCHAR(36)` PK), `user_id` (`VARCHAR(255)` NOT NULL), `event_type` (`VARCHAR(50)` NOT NULL), `dedup_key` (`VARCHAR(255)` NOT NULL UNIQUE), `message_preview` (`VARCHAR(500)` NOT NULL), `provider` (`VARCHAR(50)` NOT NULL), `provider_message_id` (`VARCHAR(128)` NULL), `status` (`VARCHAR(30)` NOT NULL), `error_message` (`TEXT` NULL), `sent_at` (`TIMESTAMP` NOT NULL), além de índice em `(user_id, sent_at)`.

2. **Gestão de Preferências, Validação E.164 e Opt-in LGPD (`GET/PUT /api/v1/notifications/whatsapp/preferences`):**
   - **Dado** um investidor autenticado com token JWT válido (`userId = jwt.getSubject()`)
   - **Quando** consulta `GET /api/v1/notifications/whatsapp/preferences`
   - **Então** recebe suas preferências atuais (ou defaults seguros com `optedIn = false` caso ainda não configurado)
   - **Quando** envia `PUT /api/v1/notifications/whatsapp/preferences` com `phoneNumber`, `optedIn`, flags de canal (`notifyDividends`, `notifyRebalanceTriggers`, `notifyGoalMilestones`) e `rebalanceDriftThresholdPct`
   - **Então** o sistema valida o formato E.164 do telefone (`^\+[1-9]\d{9,14}$`), aplica `clamp` em `rebalanceDriftThresholdPct` entre `1.00` e `50.00`, registra `optedInAt` em `ZoneOffset.UTC` quando `optedIn` passa a `true`, e retorna `400 Bad Request` (RFC 7807 `ProblemDetail`) para número inválido
   - **E** o telefone completo nunca é exposto em texto claro nos logs `INFO` (utilizar mascaramento, ex: `+5511****9999`).

3. **Gateway Plugável de WhatsApp (Mock Hermético + Meta Cloud API):**
   - **Dado** a porta `WhatsappGatewayPort` no módulo `common`
   - **Quando** `whatsapp.provider=mock` (padrão local/CI)
   - **Então** o adaptador `MockWhatsappGatewayAdapter` simula o envio sem chamadas HTTP externas, registra log `INFO` estruturado com `provider=MOCK` e retorna `providerMessageId` sintético
   - **Quando** `whatsapp.provider=meta`
   - **Então** o adaptador `MetaCloudWhatsappGatewayAdapter` utiliza `RestClient` para chamar `POST https://graph.facebook.com/v21.0/{phoneNumberId}/messages` com Bearer token configurado via `${WHATSAPP_META_ACCESS_TOKEN:}`, tratando falhas HTTP com log `ERROR` (incluindo stack trace) e retorno de `WhatsappSendResult` com `success=false` sem derrubar o fluxo principal.

4. **Idempotência, Auditoria e Envio de Teste (`POST /api/v1/notifications/whatsapp/test` e `GET /history`):**
   - **Dado** um usuário com `optedIn = true` e telefone válido configurado
   - **Quando** dispara `POST /api/v1/notifications/whatsapp/test`
   - **Então** envia uma mensagem de teste via `WhatsappGatewayPort` e registra o resultado em `whatsapp_notification_logs`
   - **E** caso o usuário ainda não tenha `optedIn = true` ou telefone configurado, `POST /api/v1/notifications/whatsapp/test` retorna `400 Bad Request` (RFC 7807 `ProblemDetail`)
   - **E** para disparos automáticos, `SendWhatsappAlertUseCase` verifica `existsByDedupKey(dedupKey)` antes de enviar, garantindo unicidade global por chave:
     - `DIVIDEND_ANNOUNCEMENT`: `dividend:{userId}:{ticker}:{comDate}:{type}`
     - `REBALANCE_TRIGGER`: `rebalance:{userId}:{goalType}:{targetKey}:{yyyy-MM-dd}` (data do ciclo em `America/Sao_Paulo`)
     - `GOAL_MILESTONE`: `goal:{goalId}:milestone:{pct}`
   - **E** `GET /api/v1/notifications/whatsapp/history` retorna os últimos registros do `userId` autenticado ordenados por `sentAt` decrescente.

5. **Gatilhos Automáticos de Proventos, Rebalanceamento e Metas Financeiras:**
   - **Dado** novos anúncios de proventos persistidos em `SyncCorporateEventsUseCase` (`!exists`)
   - **Quando** `NotifyDividendAlertsUseCase` é acionado para os novos proventos
   - **Então** identifica usuários com `optedIn = true` e `notifyDividends = true` que possuem o ticker em custódia (via `GetCustodyPositionsUseCase`), calcula o valor líquido estimado (`quantidade × netValue` em `BigDecimal`, validado contra valores não finitos/negativos) e envia alerta formatado com ticker, tipo, data COM/pagamento e valor estimado
   - **Dado** o agendador `WhatsappAlertScheduler` (`@Scheduled` + `@SchedulerLock` na tabela `shedlock`)
   - **Quando** executa no horário configurado (`whatsapp.alerts.cron: 0 0 9 * * MON-FRI`, `America/Sao_Paulo`)
   - **Então** `EvaluateRebalanceAndGoalAlertsUseCase` avalia para usuários `optedIn = true`:
     - **Rebalanceamento (`notifyRebalanceTriggers = true`):** desvio absoluto entre alocação atual (derivada de `GetCustodyPositionsUseCase`) e `AllocationGoalPort.findByUserId(userId)` superior a `rebalanceDriftThresholdPct` (com proteção contra divisão por zero quando `totalEquity <= 0`)
     - **Marcos de Meta (`notifyGoalMilestones = true`):** atingimento de marcos percentuais `(25, 50, 75, 100)` da `FinancialGoal` do usuário (com proteção `targetValue > 0`): para `GoalType.WEALTH`, `totalEquity / targetValue * 100`; para `GoalType.INCOME`, renda mensal estimada `((totalEquity * grossYieldPercentage / 100) / 12) / targetValue * 100` (via `GetPortfolioSummaryUseCase`), avaliando cada marco atingido e deduplicado individualmente por `goal:{goalId}:milestone:{pct}`.

6. **Interface Flutter para Configuração e Histórico de Alertas WhatsApp:**
   - **Dado** o usuário autenticado no frontend Flutter
   - **Quando** acessa `Meu Perfil` (`/profile`) e toca no novo card **"Alertas via WhatsApp"** (rota `/notifications/whatsapp` via `context.push`, preservando os índices de navegação de `main_layout.dart`)
   - **Então** visualiza o formulário com campo de telefone E.164, switch de consentimento LGPD (`optedIn`), toggles por tipo de alerta (`Proventos`, `Rebalanceamento`, `Marcos de Meta`), controle de limiar de desvio (`1%` a `50%`), botão **"Salvar Preferências"**, botão **"Enviar Teste"** com feedback via `SnackBar` e lista do histórico recente de envios.

## Tasks / Subtasks

- [x] **Task 1: Migration Liquibase (AC: #1)**
  - [x] 1.1 Criar `backend/common/src/main/resources/db/changelog/35-create-whatsapp-notifications-tables.yaml` com as tabelas `whatsapp_notification_preferences` e `whatsapp_notification_logs`, constraints `UNIQUE` (`user_id` e `dedup_key`), índice `idx_whatsapp_logs_user_sent` em `(user_id, sent_at)` e bloco `rollback` explícito compatível com MySQL/TiDB.
  - [x] 1.2 Incluir o novo arquivo no final de `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`.

- [x] **Task 2: Camada de Domínio, Portas e Casos de Uso no módulo `common` (AC: #2, #3, #4, #5)**
  - [x] 2.1 Criar entidades de domínio em `afsdigital.grahamselect.common.notifications.domain.entities`: `WhatsappNotificationPreference`, `WhatsappNotificationLog`, `WhatsappAlertType`, `WhatsappDeliveryStatus`, `WhatsappSendResult`.
  - [x] 2.2 Criar portas em `afsdigital.grahamselect.common.notifications.application.repository`: `WhatsappPreferencePort`, `WhatsappNotificationLogPort`, `WhatsappGatewayPort`.
  - [x] 2.3 Criar DTOs em `afsdigital.grahamselect.common.notifications.application.dto`:
    - `WhatsappPreferenceDto(phoneNumber, optedIn, optedInAt, notifyDividends, notifyRebalanceTriggers, notifyGoalMilestones, rebalanceDriftThresholdPct)`
    - `UpdateWhatsappPreferenceRequest(phoneNumber, optedIn, notifyDividends, notifyRebalanceTriggers, notifyGoalMilestones, rebalanceDriftThresholdPct)`
    - `WhatsappNotificationLogDto(id, eventType, messagePreview, provider, status, errorMessage, sentAt)`
    - `WhatsappTestAlertResultDto(status, provider, providerMessageId, messagePreview, sentAt)`
  - [x] 2.4 Implementar `GetWhatsappPreferencesUseCase`, `UpdateWhatsappPreferencesUseCase` (validação E.164, `clamp` de `1.00` a `50.00` no limiar de rebalanceamento, `ZoneOffset.UTC` para `optedInAt`) e `GetWhatsappNotificationHistoryUseCase`.
  - [x] 2.5 Implementar `SendWhatsappAlertUseCase` (checagem de opt-in, idempotência via `dedupKey`, mascaramento de telefone em logs `INFO`, registro de logs `ERROR` com stack trace em falhas) e `SendTestWhatsappAlertUseCase` (lançando `IllegalArgumentException` caso `optedIn == false` ou telefone não configurado).
  - [x] 2.6 Implementar `NotifyDividendAlertsUseCase` e `EvaluateRebalanceAndGoalAlertsUseCase` reutilizando `GetCustodyPositionsUseCase`, `GetPortfolioSummaryUseCase`, `AllocationGoalPort` e `FinancialGoalRepository` (cálculos com `BigDecimal`, checagem de denominador `> 0` antes de qualquer divisão para `totalEquity` e `targetValue` em `PATRIMONY_TARGET` e `MONTHLY_INCOME_TARGET` — regra de Robustez Numérica do `project-context.md`). Integrar gancho opcional em `SyncCorporateEventsUseCase` para novos proventos (`!exists`), mantendo construtor retrocompatível de 5 argumentos para não quebrar testes existentes.

- [x] **Task 3: Adaptadores, Scheduler e REST API no módulo `api` (AC: #2, #3, #4, #5)**
  - [x] 3.1 Criar entidades JPA, repositórios Spring Data e adaptadores (`WhatsappPreferenceJpaAdapter`, `WhatsappNotificationLogJpaAdapter`).
  - [x] 3.2 Criar `MockWhatsappGatewayAdapter` (`whatsapp.provider=mock`, default) e `MetaCloudWhatsappGatewayAdapter` (`whatsapp.provider=meta`, usando `RestClient` com timeouts configurados, payload `messaging_product: "whatsapp"`, `to`, `type: "text"`, `text.body`, e validação defensiva caso `phoneNumberId` ou `accessToken` estejam vazios).
  - [x] 3.3 Criar `WhatsappNotificationsConfiguration` registrando os beans de use cases (incluindo atualização do bean `SyncCorporateEventsUseCase` em `CorporateEventsConfiguration`) e configurando propriedades `whatsapp.*` em `backend/api/src/main/resources/application.yml`.
  - [x] 3.4 Criar `WhatsappAlertScheduler` com `@Scheduled(cron = "${whatsapp.alerts.cron:0 0 9 * * MON-FRI}", zone = "America/Sao_Paulo")` e `@SchedulerLock(name = "WhatsappAlertScheduler_evaluateAlerts", lockAtLeastFor = "1m", lockAtMostFor = "15m")`.
  - [x] 3.5 Criar `WhatsappNotificationsController` em `/api/v1/notifications/whatsapp` (`GET /preferences`, `PUT /preferences`, `POST /test`, `GET /history`) extraindo `userId` estritamente de `@AuthenticationPrincipal Jwt jwt` (`jwt.getSubject()`), com logs `INFO` na entrada de cada endpoint e `ERROR` com stack trace em exceções.

- [x] **Task 4: Interface Frontend Flutter (AC: #6)**
  - [x] 4.1 Criar feature `frontend/lib/src/features/notifications/` seguindo a estrutura `data/{datasources,repositories}`, `domain/models`, `presentation/{pages,providers}`:
    - `WhatsappPreferencesModel` e `WhatsappNotificationLogModel` (alinhados aos campos JSON dos DTOs da Task 2.3)
    - `WhatsappNotificationsRemoteDataSource` (chamando `/api/v1/notifications/whatsapp/*` via `ApiClient` (`Dio`: `get`, `put`, `post`) e extraindo `detail` de `DioException` quando houver `ProblemDetail`)
    - `WhatsappNotificationsProvider` (`ChangeNotifier` com estados loading/saving/testing/error)
    - `WhatsappNotificationsPage` (formulário E.164, opt-in LGPD, toggles, limiar de desvio, botão de teste, histórico).
  - [x] 4.2 Registrar `WhatsappNotificationsProvider` e rota `/notifications/whatsapp` em `frontend/lib/main.dart`.
  - [x] 4.3 Adicionar card **"Alertas via WhatsApp"** em `frontend/lib/src/features/profile/presentation/pages/profile_page.dart` navegando via `context.push('/notifications/whatsapp')`.

- [x] **Task 5: Testes Automatizados, Análise Estática e Documentação C4 (AC: #1-#6)**
  - [x] 5.1 Backend: criar testes unitários para os use cases (`UpdateWhatsappPreferencesUseCaseTest`, `SendWhatsappAlertUseCaseTest`, `EvaluateRebalanceAndGoalAlertsUseCaseTest`, `NotifyDividendAlertsUseCaseTest`) e testes de controller/mock (`WhatsappNotificationsControllerTest`), e executar `mvn test` no reactor completo.
  - [x] 5.2 Frontend: criar testes de provider e de widget (`whatsapp_notifications_provider_test.dart`, `whatsapp_notifications_page_test.dart`), executar `flutter test` e `flutter analyze` (0 erros e 0 warnings).
  - [x] 5.3 Atualizar `docs/workspace.dsl` documentando o componente/serviço de Notificações WhatsApp e a integração externa com gateway WhatsApp.

### Review Findings

- [x] [Review][Decision] Burst de múltiplos marcos de meta (25%, 50%, 75%, 100%) no mesmo ciclo — **Decisão (1A):** manter o envio individual de cada marco atingido conforme AC #5 (dismissed).
- [x] [Review][Patch] Tornar assíncrono o disparo de alertas de proventos em `SyncCorporateEventsUseCase` (decisão 2B) [backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/usecase/SyncCorporateEventsUseCase.java:135]
- [x] [Review][Patch] Falha transitória no gateway bloqueia retentativas permanentemente e condição de corrida TOCTOU em `dedupKey` [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/SendWhatsappAlertUseCase.java:32]
- [x] [Review][Patch] Ausência de limite/truncamento para `dedupKey` (<= 255) e `providerMessageId` (<= 128) antes da persistência [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WhatsappNotificationLogJpaAdapter.java:36]
- [x] [Review][Patch] Impossibilidade de revogar consentimento (`optedIn = false`) limpando o telefone por validação E.164 incondicional [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/UpdateWhatsappPreferencesUseCase.java:34]
- [x] [Review][Patch] Fallback silencioso para `equityByAssetClass` quando `AllocationGoalDto.goalType()` é desconhecido [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateRebalanceAndGoalAlertsUseCase.java:105]
- [x] [Review][Patch] Null-checks ausentes para `type` e `comDate` e envio de alerta para provento com `netValue == 0` [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/NotifyDividendAlertsUseCase.java:53]
- [x] [Review][Patch] Null-check ausente para retorno de `preferencePort.findAllOptedIn()` [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateRebalanceAndGoalAlertsUseCase.java:39]
- [x] [Review][Patch] Validação de telefone vazio, checagem de `messages[0].id` em HTTP 200 e sanitização de `errorMessage` no gateway Meta [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/MetaCloudWhatsappGatewayAdapter.java:34]
- [x] [Review][Patch] Null-check ausente para `@AuthenticationPrincipal Jwt jwt` nos endpoints REST [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/web/WhatsappNotificationsController.java:31]
- [x] [Review][Patch] Mascaramento insuficiente para números E.164 curtos (exposição de 9 de 11 caracteres) [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/WhatsappMaskingUtils.java:17]
- [x] [Review][Patch] `WhatsappNotificationsProvider`: `historyFuture` não aguardada em `loadAll()` e erro em `getHistory()` mascarando sucesso de `sendTestAlert()` [frontend/lib/src/features/notifications/presentation/providers/whatsapp_notifications_provider.dart:24]
- [x] [Review][Patch] `WhatsappNotificationsPage` e `RemoteDataSource`: falta `_syncForm` pós-save, botão Retry no erro inicial, bloqueio de teste com alterações não salvas e checagem de tipo em `response.data` [frontend/lib/src/features/notifications/presentation/pages/whatsapp_notifications_page.dart:78]
- [x] [Review][Patch] Lacunas de cobertura de testes automatizados (Meta gateway, scheduler, use cases restantes, fluxos 400/401 no controller e cenários de save/erro na página Flutter) [backend/api/src/test/java/afsdigital/grahamselect/api/notifications/web/WhatsappNotificationsControllerTest.java:1]

## Dev Notes

### Contexto Arquitetural e Guardrails Críticos

1. **Identificador de Usuário (`userId`):**
   - Os controllers de domínio recentes (`FinancialGoalsController`, `DividendController`, `TaxController`) usam `@AuthenticationPrincipal Jwt jwt` e extraem `String userId = jwt.getSubject();`.
   - `User.id` é `Long`, mas `financial_goals.user_id`, `trades.user_id`, `allocation_goals.user_id` armazenam `String` (`googleSub` / `jwt.getSubject()`). Use `String userId` (`jwt.getSubject()`) nas tabelas `whatsapp_notification_preferences` e `whatsapp_notification_logs` para manter compatibilidade direta com custódia, metas e proventos.
2. **Controllers Manuais vs. OpenAPI Generator:**
   - Conforme `backend/api/pom.xml`, controllers de domínio mais recentes (`Admin`, `Dividends`, `OpenFinance`, `Tax`, `FinancialGoals`) são `@RestController`s Spring manuais. Implemente `WhatsappNotificationsController` diretamente como `@RestController` em `afsdigital.grahamselect.api.notifications.web`.
3. **Imutabilidade de Migrations Liquibase (`AGENTS.md`):**
   - Nunca edite arquivos `01`–`34` em `backend/common/src/main/resources/db/changelog/`. A próxima migration é estritamente a `35-create-whatsapp-notifications-tables.yaml`.
4. **Robustez Numérica (`project-context.md`):**
   - Em `EvaluateRebalanceAndGoalAlertsUseCase` e `NotifyDividendAlertsUseCase`, use `BigDecimal` com escala e `RoundingMode.HALF_UP`, valide `totalEquity.compareTo(BigDecimal.ZERO) > 0` antes de calcular percentuais de desvio ou progresso de meta, e aplique `clamp` em `rebalanceDriftThresholdPct` (`1.00` a `50.00`).
5. **Segurança & LGPD (`project-context.md`):**
   - Não enviar mensagens sem `optedIn == true`.
   - Mascarar `phoneNumber` em todos os logs (`INFO`/`WARN`/`ERROR`), exibindo apenas prefixo + últimos 4 dígitos.
   - Registrar log `INFO` na entrada de todos os endpoints REST, no `WhatsappAlertScheduler` e nos use cases de disparo; registrar `ERROR` com stack trace (`log.error("...", e)`) em todo bloco `catch`.
   - Usar `ZoneOffset.UTC` (`OffsetDateTime.now(ZoneOffset.UTC)` / `LocalDateTime.now(ZoneOffset.UTC)`) em todas as manipulações temporais.
6. **Navegação Flutter (`main_layout.dart` e `profile_page.dart`):**
   - Não alterar os índices do `_Sidebar` / `_BottomNavBar` em `main_layout.dart`. Expor o acesso à tela `/notifications/whatsapp` via `ListTile` em `ProfilePage` usando `context.push('/notifications/whatsapp')`, exatamente como já é feito para `/family-profiles` e `/tags`.
7. **Reuso de Domínio Existente e Agendamento:**
   - Reutilizar `GetCustodyPositionsUseCase.execute(userId)` (`CustodyPositionDTO`: `ticker`, `quantity`, `marketValue`, `assetClass`), `GetPortfolioSummaryUseCase.execute(userId)` (`PortfolioSummaryDTO`: `totalEquity`, `grossYieldPercentage`), `AllocationGoalPort.findByUserId(userId)` (`AllocationGoalDto`: `goalType`, `targetKey`, `targetPercentage`) e `FinancialGoalRepository.findByUserId(userId)`. Não recalcular custódia diretamente via `TradePort`.
   - `ApiServiceApplication` já possui `@EnableScheduling` e a tabela `shedlock` já existe (migration `24`); não duplicar essas configurações.
   - Em `SyncCorporateEventsUseCase`, manter o construtor original de 5 parâmetros delegando para o novo construtor de 6 parâmetros com `notifyDividendAlertsUseCase = null`, preservando compatibilidade com testes unitários existentes.

### Project Structure Notes

- **Backend `common`:** `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/{domain/entities,application/{dto,repository,usecase}}`
- **Backend `api`:** `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/{infrastructure/{persistence,gateway,spring},web}`
- **Liquibase:** `backend/common/src/main/resources/db/changelog/35-create-whatsapp-notifications-tables.yaml`
- **Frontend:** `frontend/lib/src/features/notifications/{data/{datasources,repositories},domain/models,presentation/{pages,providers}}`

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Epic 11: Notificações Multicanal]
- [Source: docs/bmad/planning-artifacts/prd.md#FR25]
- [Source: docs/bmad/implementation-artifacts/epic-10-retro-2026-11-20.md#Visão do Próximo Épico & Dependências]
- [Source: docs/bmad/project-context.md#Critical Implementation Rules]
- [Source: backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsScheduler.java]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/usecase/SyncCorporateEventsUseCase.java]
- [Source: frontend/lib/src/features/profile/presentation/pages/profile_page.dart]

## Dev Agent Record

### Agent Model Used

Gemini 4 Hydrogen (Medium)

### Debug Log References

- `GoalType` real no domínio é `PATRIMONY_TARGET` e `MONTHLY_INCOME_TARGET` (ajustado em `EvaluateRebalanceAndGoalAlertsUseCase`).
- Reactor backend completo (`mvn test`) executado e aprovado em todos os módulos (`common`, `api-service`, `valuation-service`).
- `flutter analyze` (0 issues) e `flutter test` completo (289/289 testes) executados e aprovados.

### Completion Notes List

- Migration Liquibase `35-create-whatsapp-notifications-tables.yaml` criada com rollback explícito e registrada no `db.changelog-master.yaml`.
- Domínio, portas, DTOs e 7 casos de uso implementados em `common`, com validação E.164, clamp `1.00`–`50.00`, `ZoneOffset.UTC`, mascaramento de telefone, idempotência via `dedup_key` e gancho retrocompatível em `SyncCorporateEventsUseCase`.
- Adaptadores JPA, gateway plugável (`MockWhatsappGatewayAdapter` e `MetaCloudWhatsappGatewayAdapter`), `WhatsappAlertScheduler` (ShedLock + cron `America/Sao_Paulo`), `WhatsappNotificationsConfiguration` e `WhatsappNotificationsController` implementados em `api`.
- Feature Flutter `notifications` criada com models, datasource (`ApiClient`), repository, `WhatsappNotificationsProvider`, `WhatsappNotificationsPage`, rota `/notifications/whatsapp` em `main.dart` e card na `ProfilePage`.
- Diagrama C4 atualizado em `docs/workspace.dsl`.

### File List

- `backend/common/src/main/resources/db/changelog/35-create-whatsapp-notifications-tables.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WhatsappAlertType.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WhatsappDeliveryStatus.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WhatsappSendResult.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WhatsappNotificationPreference.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WhatsappNotificationLog.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/repository/WhatsappPreferencePort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/repository/WhatsappNotificationLogPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/repository/WhatsappGatewayPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/WhatsappPreferenceDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/UpdateWhatsappPreferenceRequest.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/WhatsappNotificationLogDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/WhatsappTestAlertResultDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/WhatsappMaskingUtils.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/GetWhatsappPreferencesUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/UpdateWhatsappPreferencesUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/GetWhatsappNotificationHistoryUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/SendWhatsappAlertUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/SendTestWhatsappAlertUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/NotifyDividendAlertsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateRebalanceAndGoalAlertsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/usecase/SyncCorporateEventsUseCase.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WhatsappNotificationPreferenceEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WhatsappNotificationLogEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/SpringDataWhatsappPreferenceRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/SpringDataWhatsappNotificationLogRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WhatsappPreferenceJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WhatsappNotificationLogJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/MockWhatsappGatewayAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/MetaCloudWhatsappGatewayAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/spring/WhatsappNotificationsConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/spring/WhatsappAlertScheduler.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/web/WhatsappNotificationsController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsConfiguration.java`
- `backend/api/src/main/resources/application.yml`
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/UpdateWhatsappPreferencesUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/SendWhatsappAlertUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateRebalanceAndGoalAlertsUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/NotifyDividendAlertsUseCaseTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/notifications/web/WhatsappNotificationsControllerTest.java`
- `frontend/lib/src/features/notifications/domain/models/whatsapp_notification_models.dart`
- `frontend/lib/src/features/notifications/data/datasources/whatsapp_notifications_remote_data_source.dart`
- `frontend/lib/src/features/notifications/data/repositories/whatsapp_notifications_repository.dart`
- `frontend/lib/src/features/notifications/presentation/providers/whatsapp_notifications_provider.dart`
- `frontend/lib/src/features/notifications/presentation/pages/whatsapp_notifications_page.dart`
- `frontend/lib/main.dart`
- `frontend/lib/src/features/profile/presentation/pages/profile_page.dart`
- `frontend/test/features/notifications/whatsapp_notifications_provider_test.dart`
- `frontend/test/features/notifications/whatsapp_notifications_page_test.dart`
- `docs/workspace.dsl`

### Change Log

- **2026-11-20:** Implementação completa da Story 11.1 (Alertas via WhatsApp — Gatilhos de Meta): migration Liquibase `35`, domínio/casos de uso em `common`, adaptadores/scheduler/REST API em `api`, tela/provider Flutter e atualização do C4 (`docs/workspace.dsl`).
