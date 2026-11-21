---
baseline_commit: 4d6627b3d35953def9851ffdd17886eff1de4343
---
# Story 11.2: Web Push para Zona de Compra (Graham)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como um investidor (usuário autenticado do Graham-Select),
Eu quero ativar notificações Web Push no meu navegador e configurar minha margem de segurança mínima para receber alertas proativos quando o preço de um ativo entrar na Zona de Compra de Graham,
Para que eu possa aproveitar oportunidades de aporte com desconto sem precisar monitorar o mercado manualmente.

## Acceptance Criteria

1. **Persistência e Imutabilidade Liquibase (`36-create-web-push-notifications-tables.yaml`):**
   - **Dado** o banco de dados gerenciado pelo Liquibase
   - **Quando** a aplicação inicializa e executa a nova migration `36-create-web-push-notifications-tables.yaml` (registrada em `db.changelog-master.yaml`, sem alterar nenhuma migration `01` a `35`)
   - **Então** é criada a tabela `web_push_preferences` com colunas: `id` (`VARCHAR(36)` PK), `user_id` (`VARCHAR(255)` NOT NULL UNIQUE), `opted_in` (`BOOLEAN` NOT NULL DEFAULT FALSE), `opted_in_at` (`TIMESTAMP` NULL), `min_margin_of_safety_pct` (`DECIMAL(5,2)` NOT NULL DEFAULT 20.00), `only_custody_tickers` (`BOOLEAN` NOT NULL DEFAULT TRUE), `created_at` (`TIMESTAMP` NOT NULL), `updated_at` (`TIMESTAMP` NOT NULL)
   - **E** é criada a tabela `web_push_subscriptions` com colunas: `id` (`VARCHAR(36)` PK), `user_id` (`VARCHAR(255)` NOT NULL), `endpoint` (`VARCHAR(1000)` NOT NULL), `endpoint_hash` (`VARCHAR(64)` NOT NULL UNIQUE), `p256dh_key` (`VARCHAR(255)` NOT NULL), `auth_key` (`VARCHAR(255)` NOT NULL), `user_agent` (`VARCHAR(500)` NULL), `active` (`BOOLEAN` NOT NULL DEFAULT TRUE), `last_used_at` (`TIMESTAMP` NULL), `created_at` (`TIMESTAMP` NOT NULL), `updated_at` (`TIMESTAMP` NOT NULL), além de índice `idx_web_push_sub_user_active` em `(user_id, active)`
   - **E** é criada a tabela `web_push_notification_logs` com colunas: `id` (`VARCHAR(36)` PK), `user_id` (`VARCHAR(255)` NOT NULL), `subscription_id` (`VARCHAR(36)` NULL), `ticker` (`VARCHAR(20)` NULL), `event_type` (`VARCHAR(50)` NOT NULL), `dedup_key` (`VARCHAR(255)` NOT NULL UNIQUE), `title` (`VARCHAR(150)` NOT NULL), `message_preview` (`VARCHAR(500)` NOT NULL), `provider` (`VARCHAR(50)` NOT NULL), `http_status` (`INT` NULL), `status` (`VARCHAR(30)` NOT NULL), `error_message` (`TEXT` NULL), `sent_at` (`TIMESTAMP` NOT NULL), além de índice `idx_web_push_logs_user_sent` em `(user_id, sent_at)`.

2. **Gestão de Preferências, Assinaturas Push e Chave Pública VAPID (`/api/v1/notifications/web-push/*`):**
   - **Dado** um investidor autenticado com token JWT válido (`userId = jwt.getSubject()`)
   - **Quando** consulta `GET /api/v1/notifications/web-push/vapid-public-key`
   - **Então** recebe `{ "publicKey": "...", "provider": "mock|vapid" }`
   - **Quando** consulta `GET /api/v1/notifications/web-push/preferences`
   - **Então** recebe suas preferências atuais (ou defaults seguros com `optedIn = false`, `minMarginOfSafetyPct = 20.00`, `onlyCustodyTickers = true` caso ainda não configurado)
   - **Quando** envia `PUT /api/v1/notifications/web-push/preferences`
   - **Então** o sistema aplica `clamp` em `minMarginOfSafetyPct` entre `0.00` e `90.00`, registra `optedInAt` em `ZoneOffset.UTC` quando `optedIn` passa a `true` (limpando ou preservando conforme revogação sem erro), e persiste com `updatedAt` em `ZoneOffset.UTC`
   - **Quando** envia `POST /api/v1/notifications/web-push/subscriptions` com `endpoint`, `p256dhKey`, `authKey` e `userAgent` opcional
   - **Então** valida que `endpoint` (URL `https://` ou `http://localhost` / `http://127.0.0.1` para desenvolvimento local), `p256dhKey` e `authKey` são não vazios (retornando `400 Bad Request` para inválidos), realiza upsert idempotente por `endpoint_hash` (SHA-256 de `userId + "|" + endpoint`) ativando a assinatura (`active = true`)
   - **E** `GET /api/v1/notifications/web-push/subscriptions` lista as assinaturas do `userId` autenticado sem expor `p256dhKey` ou `authKey`
   - **E** `DELETE /api/v1/notifications/web-push/subscriptions/{id}` remove/desativa apenas assinaturas pertencentes ao `userId` autenticado (retornando `404` se inexistente ou de outro usuário).

3. **Gateway Plugável de Web Push (Mock Hermético + VAPID RFC 8291/8292 Nativo JDK 21):**
   - **Dado** a porta `WebPushGatewayPort` no módulo `common`
   - **Quando** `webpush.provider=mock` (padrão local/CI)
   - **Então** `MockWebPushGatewayAdapter` simula o envio sem chamadas HTTP externas, registra log `INFO` estruturado (mascarando o `endpoint`, sem expor `p256dhKey`/`authKey`) e retorna `WebPushSendResult` com `success=true`, `httpStatus=201` e `provider="MOCK"`
   - **Quando** `webpush.provider=vapid`
   - **Então** `VapidWebPushGatewayAdapter` assina o JWT VAPID (ES256 / `SHA256withECDSA`) e criptografa o payload JSON no padrão RFC 8291 (`aes128gcm` via ECDH P-256 + HKDF-SHA256 + AES-128-GCM com APIs padrão JDK 21, sem dependências incompatíveis com GraalVM Native Image), enviando via `RestClient` para o `endpoint` da assinatura com headers `Authorization: vapid t=..., k=...`, `TTL: 86400` e `Content-Encoding: aes128gcm`
   - **E** se o push service responder HTTP `404` ou `410` (Gone), o retorno sinaliza assinatura expirada (`expired = true`) para que o caso de uso marque automaticamente `active = false` na tabela `web_push_subscriptions`
   - **E** falhas HTTP/criptográficas são registradas com log `ERROR` (incluindo stack trace) sem derrubar o fluxo principal.

4. **Idempotência, Envio de Teste e Histórico (`POST /test` e `GET /history`):**
   - **Dado** um usuário com `optedIn = true` e pelo menos uma assinatura `active = true`
   - **Quando** dispara `POST /api/v1/notifications/web-push/test`
   - **Então** envia um alerta Web Push de teste para suas assinaturas ativas e grava o resultado em `web_push_notification_logs`
   - **E** caso `optedIn == false` ou não haja nenhuma assinatura ativa, retorna `400 Bad Request`
   - **E** `GET /api/v1/notifications/web-push/history` retorna os últimos registros do `userId` autenticado ordenados por `sentAt` decrescente.

5. **Gatilho Automático de Zona de Compra de Graham (`EvaluateGrahamBuyZoneAlertsUseCase` + `WebPushAlertScheduler`):**
   - **Dado** o agendador `WebPushAlertScheduler` (`@Scheduled` + `@SchedulerLock` na tabela `shedlock`) configurado para `webpush.alerts.cron` (`0 15 20 * * MON-FRI`, `America/Sao_Paulo`, executando após `MarketDataScheduler` das `20:00`)
   - **Quando** `EvaluateGrahamBuyZoneAlertsUseCase` avalia os usuários com `optedIn = true` e assinaturas ativas
   - **Então** consulta `RankingRepository.findTop20BestRanked()`, aplicando obrigatoriamente os mesmos guardrails de integridade do `GenerateGrahamRecommendationsUseCase`:
     - Descarta ativos com `intrinsicValueUpdatedAt == null` ou idade superior a `ValuationCompletedEvent.STALE_DATA_THRESHOLD_DAYS` (`STALE_DATA`)
     - Descarta ativos com `marginOfSafety == null` ou `<= 0` ou `> ValuationCompletedEvent.OUTLIER_MARGIN_THRESHOLD` (`OUTLIER_PRICE` circuit breaker)
     - Valida `currentPrice > 0`, `intrinsicValue > 0` antes de calcular percentual `marginOfSafety * 100`
   - **E** se `onlyCustodyTickers == true`, filtra apenas os tickers que o usuário possui em custódia (via `GetCustodyPositionsUseCase.execute(userId)`)
   - **E** seleciona os ativos cuja margem de segurança percentual (`marginOfSafety * 100`) seja `>= minMarginOfSafetyPct` do usuário
   - **E** para cada ativo em Zona de Compra e cada assinatura ativa do usuário, envia Web Push com título (ex: `"Zona de Compra Graham: {TICKER}"`), corpo com preço atual, preço justo de Graham e margem de segurança (%), e URL de destino `/graham-recommendations`, garantindo idempotência por `dedupKey = graham-buy-zone:{userId}:{ticker}:{yyyy-MM-dd}:{subscriptionId}` (ciclo em `America/Sao_Paulo`, ignorando reenvio apenas se status anterior for `SENT`).

6. **Interface Flutter Web, Service Worker e Integração no Perfil:**
   - **Dado** o usuário autenticado no frontend Flutter
   - **Quando** acessa `Meu Perfil` (`/profile`) e toca no novo card **"Alertas Web Push (Graham)"** (rota `/notifications/web-push` via `context.push`, preservando os índices de navegação de `main_layout.dart`)
   - **Então** visualiza:
     - Status de permissão do navegador (`granted` / `denied` / `default` / `unsupported`) e botão **"Ativar Notificações neste Navegador"** (que solicita permissão via Push API, registra `frontend/web/push-sw.js`, obtém a chave pública VAPID via API e envia a `PushSubscription` ao backend)
     - Switch de consentimento LGPD (`optedIn`), slider/campo de **Margem de Segurança Mínima (`0%` a `90%`)**, toggle **"Apenas ativos em custódia"**, botão **"Salvar Preferências"**
     - Lista de navegadores/assinaturas registradas com botão **"Remover"**
     - Botão **"Enviar Teste"** com feedback via `SnackBar` e lista de histórico recente de notificações
   - **E** o Service Worker `frontend/web/push-sw.js` trata eventos `push` exibindo notificação nativa (`self.registration.showNotification`) e `notificationclick` buscando janelas abertas com `clients.matchAll({ type: 'window', includeUncontrolled: true })` para focar ou abrir a rota `/graham-recommendations`.

## Tasks / Subtasks

- [x] **Task 1: Migration Liquibase (AC: #1)**
  - [x] 1.1 Criar `backend/common/src/main/resources/db/changelog/36-create-web-push-notifications-tables.yaml` com as tabelas `web_push_preferences`, `web_push_subscriptions` (com `endpoint_hash` `UNIQUE` e índice `idx_web_push_sub_user_active`) e `web_push_notification_logs` (com `dedup_key` `UNIQUE` e índice `idx_web_push_logs_user_sent`), além de bloco `rollback` explícito compatível com MySQL/TiDB.
  - [x] 1.2 Registrar `36-create-web-push-notifications-tables.yaml` no final de `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (sem alterar migrations `01`–`35`).

- [x] **Task 2: Camada de Domínio, Portas, DTOs e Casos de Uso no módulo `common` (AC: #2, #3, #4, #5)**
  - [x] 2.1 Criar entidades de domínio em `afsdigital.grahamselect.common.notifications.domain.entities`: `WebPushPreference`, `WebPushSubscription`, `WebPushNotificationLog`, `WebPushAlertType` (`GRAHAM_BUY_ZONE`, `TEST_ALERT`), `WebPushDeliveryStatus` (`SENT`, `FAILED`, `EXPIRED`), `WebPushSendResult(boolean success, boolean expired, Integer httpStatus, String provider, String errorMessage)`.
  - [x] 2.2 Criar portas em `afsdigital.grahamselect.common.notifications.application.repository`: `WebPushPreferencePort`, `WebPushSubscriptionPort`, `WebPushNotificationLogPort`, `WebPushGatewayPort`.
  - [x] 2.3 Criar DTOs em `afsdigital.grahamselect.common.notifications.application.dto`:
    - `WebPushVapidKeyDto(String publicKey, String provider)`
    - `WebPushPreferenceDto(boolean optedIn, OffsetDateTime optedInAt, BigDecimal minMarginOfSafetyPct, boolean onlyCustodyTickers)`
    - `UpdateWebPushPreferenceRequest(boolean optedIn, BigDecimal minMarginOfSafetyPct, boolean onlyCustodyTickers)`
    - `RegisterWebPushSubscriptionRequest(String endpoint, String p256dhKey, String authKey, String userAgent)`
    - `WebPushSubscriptionDto(UUID id, String endpointPreview, String userAgent, boolean active, OffsetDateTime lastUsedAt, OffsetDateTime createdAt)`
    - `WebPushNotificationLogDto(UUID id, String ticker, String eventType, String title, String messagePreview, String provider, Integer httpStatus, String status, String errorMessage, OffsetDateTime sentAt)`
    - `WebPushTestAlertResultDto(int sentCount, int failedCount, String provider, String messagePreview, OffsetDateTime sentAt)`
  - [x] 2.4 Implementar casos de uso de preferências, assinaturas e histórico:
    - `GetWebPushPreferencesUseCase`
    - `UpdateWebPushPreferencesUseCase` (aplicar `clamp` de `0.00` a `90.00` em `minMarginOfSafetyPct`, registrar `optedInAt` em `ZoneOffset.UTC`)
    - `RegisterWebPushSubscriptionUseCase` (validar URL `https://` ou `http://localhost` / `http://127.0.0.1`, validar chaves não vazias, gerar `endpointHash` SHA-256 de `userId + "|" + endpoint`, upsert mantendo ownership do `userId`)
    - `ListWebPushSubscriptionsUseCase` e `DeleteWebPushSubscriptionUseCase` (validar ownership por `userId`, lançando `WebPushSubscriptionNotFoundException` em `common.notifications.application.exceptions` caso inexistente ou de outro usuário)
    - `GetWebPushNotificationHistoryUseCase`
  - [x] 2.5 Implementar `SendWebPushAlertUseCase` (idempotência por `dedupKey` bounded a 255 chars, pular apenas se `status == SENT`, reusar `id` em retentativa de `FAILED`, desativar assinatura se `result.expired()` == `true`, registrar logs `INFO` com endpoint mascarado e `ERROR` com stack trace em falhas) e `SendTestWebPushAlertUseCase` (lançando `IllegalArgumentException` caso `optedIn == false` ou sem assinaturas ativas).
  - [x] 2.6 Implementar `EvaluateGrahamBuyZoneAlertsUseCase` reutilizando `RankingRepository.findTop20BestRanked()`, `GetCustodyPositionsUseCase.execute(userId)`, `WebPushPreferencePort.findAllOptedIn()` e `WebPushSubscriptionPort.findActiveByUserId(userId)`. Normalizar tickers com `.trim().toUpperCase()` (considerando custódia apenas com `quantity > 0`), converter `marginOfSafety` decimal (`0.00`–`0.80`) para percentual via `multiply(BigDecimal.valueOf(100))` e aplicar integralmente a regra de Robustez Numérica (`project-context.md`) e os guardrails de `STALE_DATA_THRESHOLD_DAYS` e `OUTLIER_MARGIN_THRESHOLD` de `ValuationCompletedEvent`.

- [x] **Task 3: Adaptadores JPA, Gateway VAPID, Scheduler e REST API no módulo `api` (AC: #2, #3, #4, #5)**
  - [x] 3.1 Criar entidades JPA, repositórios Spring Data e adaptadores em `afsdigital.grahamselect.api.notifications.infrastructure.persistence`: `WebPushPreferenceJpaAdapter`, `WebPushSubscriptionJpaAdapter`, `WebPushNotificationLogJpaAdapter` (aplicando truncamento preventivo em `userAgent` `<= 500`, `dedupKey` `<= 255` e `messagePreview` `<= 500` antes da persistência).
  - [x] 3.2 Criar `MockWebPushGatewayAdapter` (`webpush.provider=mock`, default) e `VapidWebPushGatewayAdapter` (`webpush.provider=vapid`) usando exclusivamente APIs criptográficas padrão do JDK 21 (`ECGenParameterSpec("secp256r1")`, `ECDH`, `SHA256withECDSAinP1363Format` para saída IEEE P1363 64-byte `r || s`, `HmacSHA256`, `AES/GCM/NoPadding`) + `RestClient` com timeouts configurados. No JWT VAPID (RFC 8292), definir `aud` estritamente como a origem do `endpoint` (`scheme://host[:port]`) e `exp` `<= 24h` (ex.: `+12h`). Validar defensivamente `public-key`, `private-key` e `subject` não vazios quando `provider=vapid`, tratando HTTP `404`/`410` como `expired=true` e outros erros com log `ERROR` e stack trace.
  - [x] 3.3 Criar `WebPushNotificationsConfiguration` (registrando beans de use cases e configurando `webpush.*` em `backend/api/src/main/resources/application.yml` no mesmo padrão do bloco `whatsapp.*`: `webpush.provider`, `webpush.alerts.cron`, `webpush.vapid.{subject,public-key,private-key}`, `webpush.http.{connect-timeout-ms,read-timeout-ms}`). Para `GET /vapid-public-key`, retornar a chave pública configurada (ou placeholder Base64URL determinístico/configurável no modo `mock` quando `vapid.public-key` estiver vazio).
  - [x] 3.4 Criar `WebPushAlertScheduler` com `@Scheduled(cron = "${webpush.alerts.cron:0 15 20 * * MON-FRI}", zone = "America/Sao_Paulo")` e `@SchedulerLock(name = "WebPushAlertScheduler_evaluateBuyZone", lockAtLeastFor = "1m", lockAtMostFor = "15m")`.
  - [x] 3.5 Criar `WebPushNotificationsController` em `/api/v1/notifications/web-push` (`GET /vapid-public-key`, `GET /preferences`, `PUT /preferences`, `GET /subscriptions`, `POST /subscriptions`, `DELETE /subscriptions/{id}`, `POST /test`, `GET /history`), extraindo `userId` estritamente de `@AuthenticationPrincipal Jwt jwt` (`jwt.getSubject()`), com logs `INFO` na entrada de cada endpoint e `ERROR` com stack trace em exceções. Registrar handler para `WebPushSubscriptionNotFoundException` em `GlobalExceptionHandler.java` retornando RFC 7807 `ProblemDetail` com HTTP `404 Not Found`.

- [x] **Task 4: Frontend Flutter Web, Service Worker e UI (AC: #6)**
  - [x] 4.1 Criar `frontend/web/push-sw.js` para tratar eventos `push` (exibindo notificação com título, corpo e ícone `icons/Icon-192.png`) e `notificationclick` (usando `clients.matchAll({ type: 'window', includeUncontrolled: true })` para focar aba existente ou abrir `/graham-recommendations` via `clients.openWindow`).
  - [x] 4.2 Criar abstração `WebPushBrowserClient` em `frontend/lib/src/features/notifications/data/services/` com import condicional (`web_push_browser_client_stub.dart` para VM/testes e `web_push_browser_client_web.dart` usando `dart:js_interop` para navegador Web: checar permissão, registrar `push-sw.js` com escopo dedicado `/push/`, executar `pushManager.subscribe` com VAPID `applicationServerKey` Base64URL).
  - [x] 4.3 Criar models (`web_push_notification_models.dart`), datasource (`web_push_notifications_remote_data_source.dart` usando `ApiClient`), repository (`web_push_notifications_repository.dart`), provider (`web_push_notifications_provider.dart`) e página (`web_push_notifications_page.dart`) em `frontend/lib/src/features/notifications/`.
  - [x] 4.4 Registrar `WebPushNotificationsProvider` e rota `/notifications/web-push` em `frontend/lib/main.dart`, e adicionar card **"Alertas Web Push (Graham)"** em `frontend/lib/src/features/profile/presentation/pages/profile_page.dart`.

- [x] **Task 5: Testes Automatizados, Análise Estática e Documentação C4 (AC: #1-#6)**
  - [x] 5.1 Backend: criar testes unitários para `UpdateWebPushPreferencesUseCase`, `RegisterWebPushSubscriptionUseCase`, `SendWebPushAlertUseCase`, `EvaluateGrahamBuyZoneAlertsUseCase`, `VapidWebPushGatewayAdapter` (criptografia RFC 8291, JWT ES256 P1363 e tratamento 404/410) e `WebPushNotificationsControllerTest` (seguindo o padrão de `WhatsappNotificationsControllerTest` para cobrir `200`/`400`/`401`/`404`), e executar `mvn test` em todo o reactor backend.
  - [x] 5.2 Frontend: criar testes de provider e widget (`web_push_notifications_provider_test.dart`, `web_push_notifications_page_test.dart`), executar `flutter test` e `flutter analyze` (0 erros e 0 warnings).
  - [x] 5.3 Atualizar `docs/workspace.dsl` documentando o componente/serviço de Notificações Web Push e a integração com Browser Push Services (W3C Web Push / VAPID).

### Review Findings

- [x] [Review][Patch] Ajustar teto de margem de segurança para 90.00% em EvaluateGrahamBuyZoneAlertsUseCase [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateGrahamBuyZoneAlertsUseCase.java:68]
- [x] [Review][Patch] Typo '?userAgent' no mapa de payload de registro de assinatura Web Push [frontend/lib/src/features/notifications/data/datasources/web_push_notifications_remote_data_source.dart:72]
- [x] [Review][Patch] Validação de Host em endpoints HTTPS e extração de Origin [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/RegisterWebPushSubscriptionUseCase.java:90]
- [x] [Review][Patch] Validação de userId não nulo/em branco e tratamento de status EXPIRED na idempotência [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/SendWebPushAlertUseCase.java:36]
- [x] [Review][Patch] Truncamento defensivo de errorMessage em logs de notificação para prevenir overflow em push gateways [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/SendWebPushAlertUseCase.java:112]
- [x] [Review][Patch] Sincronização do estado local do formulário após salvar preferências e no retry de erro [frontend/lib/src/features/notifications/presentation/pages/web_push_notifications_page.dart:63]
- [x] [Review][Patch] Desabilitar botão de teste Web Push se houver alterações não salvas pendentes [frontend/lib/src/features/notifications/presentation/pages/web_push_notifications_page.dart:550]
- [x] [Review][Patch] Alinhar log de lançamento de exceção para nível ERROR em exclusão de assinatura não pertencente ao usuário [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/DeleteWebPushSubscriptionUseCase.java:33]
- [x] [Review][Defer] Processamento assíncrono desacoplado para disparo em massa de Web Push na avaliação diária [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateGrahamBuyZoneAlertsUseCase.java:82] — deferred, pre-existing
- [x] [Review][Defer] Caching de token JWT VAPID assinado por origin para redução de overhead criptográfico em lote [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java:80] — deferred, pre-existing
- [x] [Review][Defer] Validação antecipada do par de chaves VAPID no startup do adapter [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java:45] — deferred, pre-existing
- [x] [Review][Defer] Desativação de assinatura por endpoint físico em caso de troca de usuário no mesmo browser compartilhado [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/RegisterWebPushSubscriptionUseCase.java:40] — deferred, pre-existing
- [x] [Review][Defer] Suporte a atributo tag para agrupamento no Service Worker push-sw.js [frontend/web/push-sw.js:20] — deferred, pre-existing

## Dev Notes

### Contexto Arquitetural e Guardrails Críticos

1. **Identificador de Usuário (`userId`) e Tratamento de Erros (`404`):**
   - Seguir exatamente o padrão de `WhatsappNotificationsController` (Story 11.1): extrair `userId` via `@AuthenticationPrincipal Jwt jwt` -> `jwt.getSubject()`, rejeitando `jwt == null` ou `subject` vazio com `ResponseStatusException(HttpStatus.UNAUTHORIZED, ...)`.
   - Todas as queries de preferências, assinaturas e logs devem filtrar estritamente por `userId`.
   - `GlobalExceptionHandler` não mapeia `NoSuchElementException`; portanto, `DeleteWebPushSubscriptionUseCase` deve lançar `WebPushSubscriptionNotFoundException` (definida em `common.notifications.application.exceptions`) com handler dedicado em `GlobalExceptionHandler.java` retornando `ProblemDetail` `404 Not Found`.

2. **Compatibilidade GraalVM Native Image e Criptografia VAPID/RFC 8291 (`backend/api/pom.xml`):**
   - O módulo `api` possui perfil `native` (`native-maven-plugin`). Portanto, **não adicionar** bibliotecas Java Web Push que dependam de BouncyCastle ou reflexão pesada sem metadata de native-image.
   - A especificação RFC 8291 (`aes128gcm`) + RFC 8292 (VAPID ES256) deve ser implementada exclusivamente com pacotes padrão do JDK 21 (`java.security.*`, `javax.crypto.*`) + `RestClient`.
   - Para o JWT ES256 (RFC 8292), usar `SHA256withECDSAinP1363Format` (evitando ASN.1 DER que falha em push services), definir `aud` como a origem do `endpoint` (`scheme://host[:port]`) e `exp` `<= 24h`. Validar defensivamente chaves e `subject` não vazios quando `webpush.provider=vapid`.

3. **Imutabilidade de Migrations Liquibase (`AGENTS.md`):**
   - Nunca alterar migrations `01` a `35`. A nova migration deve ser `36-create-web-push-notifications-tables.yaml`.
   - Em MySQL 8.0 / TiDB, colunas `VARCHAR(1000)` podem exceder limites de índice `UNIQUE`. Utilize `endpoint_hash` (`VARCHAR(64)` NOT NULL UNIQUE) com SHA-256 de `userId + "|" + endpoint` para garantir unicidade segura.

4. **Guardrails de Valuation & Robustez Numérica (`project-context.md`):**
   - Em `EvaluateGrahamBuyZoneAlertsUseCase`, **reutilizar** `RankingRepository.findTop20BestRanked()` e `GetCustodyPositionsUseCase.execute(userId)` — não duplicar queries SQL de valuation nem recalcular custódia via `TradePort`.
   - Aplicar os mesmos filtros de segurança de `GenerateGrahamRecommendationsUseCase`:
     - Rejeitar `intrinsicValueUpdatedAt == null` ou `ChronoUnit.DAYS.between(updatedAt, now) > ValuationCompletedEvent.STALE_DATA_THRESHOLD_DAYS`.
     - Rejeitar `marginOfSafety == null`, `<= 0` ou `> ValuationCompletedEvent.OUTLIER_MARGIN_THRESHOLD` (Circuit Breaker contra dados corrompidos).
     - Validar `currentPrice > 0` e `intrinsicValue > 0` antes de qualquer cálculo e aplicar `clamp` em `minMarginOfSafetyPct` (`0.00` a `90.00`).

5. **Lições Aprendidas da Story 11.1 (`11-1-alertas-via-whatsapp-gatilhos-de-meta.md`):**
   - **Idempotência sem bloquear retentativas:** em `SendWebPushAlertUseCase`, pular envio apenas quando `existing.getStatus() == WebPushDeliveryStatus.SENT`; se `FAILED`, permitir retentativa reutilizando o mesmo registro (`id`).
   - **Truncamento preventivo:** limitar `dedupKey` a `<= 255` caracteres, `messagePreview` a `<= 500` caracteres e `userAgent` a `<= 500` caracteres antes da persistência JPA.
   - **Frontend Provider/Page:** aguardar todas as futures em `loadAll()`, não mascarar sucesso de `sendTestAlert()` caso `getHistory()` falhe, sincronizar formulário após `savePreferences()`, oferecer botão Retry no estado de erro inicial e checar tipo de `response.data` no `RemoteDataSource`.

6. **Service Worker e Imports Condicionais no Flutter Web:**
   - Flutter Web já registra `flutter_service_worker.js` na raiz `/`. Registrar `push-sw.js` com escopo isolado (`/push/`) evita conflito de ciclo de vida com o service worker do Flutter; no evento `notificationclick`, usar `clients.matchAll({ type: 'window', includeUncontrolled: true })` para localizar janelas da aplicação controladas pelo `/` antes de chamar `clients.openWindow('/graham-recommendations')`.
   - Todo código que usa `dart:js_interop` / APIs do navegador deve ficar isolado atrás de import condicional (`if (dart.library.js_interop)`) para que `flutter test` (VM) e `flutter analyze` executem sem erros.

7. **Observabilidade e Manipulação Temporal (`AGENTS.md` / `project-context.md`):**
   - Todas as datas/horas persistidas ou manipuladas em Java devem usar `ZoneOffset.UTC` (`OffsetDateTime.now(ZoneOffset.UTC)`), exceto o agrupamento diário da chave de idempotência do ciclo B3 (`LocalDate.now(ZoneId.of("America/Sao_Paulo"))`).
   - Registrar log `INFO` na entrada de todos os endpoints REST, no `WebPushAlertScheduler` e nos use cases; nunca expor `p256dhKey`, `authKey` ou endpoint completo em logs `INFO`; registrar `ERROR` com stack trace (`log.error("...", e)`) em todo `catch`.

### Project Structure Notes

- **Backend `common`:** `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/{domain/entities,application/{dto,repository,usecase}}`
- **Backend `api`:** `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/{infrastructure/{persistence,gateway,spring},web}`
- **Liquibase:** `backend/common/src/main/resources/db/changelog/36-create-web-push-notifications-tables.yaml`
- **Frontend:** `frontend/web/push-sw.js` e `frontend/lib/src/features/notifications/{data/{datasources,repositories,services},domain/models,presentation/{pages,providers}}`

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Epic 11: Notificações Multicanal]
- [Source: docs/bmad/implementation-artifacts/11-1-alertas-via-whatsapp-gatilhos-de-meta.md]
- [Source: docs/bmad/project-context.md#Critical Implementation Rules]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java]
- [Source: backend/api/src/main/java/afsdigital/grahamselect/api/notifications/web/WhatsappNotificationsController.java]
- [Source: backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java]
- [Source: frontend/lib/src/features/profile/presentation/pages/profile_page.dart]
- [Source: frontend/lib/main.dart]

## Dev Agent Record

### Agent Model Used

Gemini 3.8 Flash (Medium)

### Debug Log References

- **Módulo `common` e ordem de compilação do Maven:** Após adicionar entidades, portas e use cases em `common`, executou-se `mvn install -pl common -DskipTests` para disponibilizar os artefatos compilados no repositório `.m2` local antes de compilar o módulo `api`.
- **Criptografia Web Push compatível com GraalVM Native Image (JDK 21 pura):** Implementou-se `WebPushCryptoUtils` e `VapidWebPushGatewayAdapter` utilizando exclusivamente primitivas padrão do Java 21 (`Signature.getInstance("SHA256withECDSAinP1363Format")`, `KeyAgreement.getInstance("ECDH")`, `Cipher.getInstance("AES/GCM/NoPadding")` com HKDF RFC 5869 customizado), gerando diretamente assinaturas IEEE P1363 (64 bytes `r || s`) exigidas pela RFC 8292 sem dependência do BouncyCastle ou ASN.1 DER.
- **Dart 3 JS Interop e compilação condicional:** Uso de `import 'dart:js_interop_unsafe';` além de `dart:js_interop` em `web_push_browser_client_web.dart` para acesso dinâmico a `has`, `getProperty` e `callMethod` na Web, com fallback em `web_push_browser_client_stub.dart` garantindo zero erros no `flutter analyze` e compatibilidade total com os testes em VM do `flutter test`.
- **Enfileiramento de SnackBars no Flutter Test:** Adicionado `ScaffoldMessenger.of(context).hideCurrentSnackBar()` antes de invocar `showSnackBar` em `WebPushNotificationsPage`, eliminando acúmulo de widgets de mensagem que causava asserção duplicada no teste de widget.

### Completion Notes List

- **Task 1 (Liquibase):** Migration incremental `36-create-web-push-notifications-tables.yaml` criada com `web_push_preferences`, `web_push_subscriptions` (com `endpoint_hash` único de 64 caracteres e índice `idx_web_push_sub_user_active`) e `web_push_notification_logs` (com `dedup_key` único de 255 caracteres e índice `idx_web_push_logs_user_sent`), registrada em `db.changelog-master.yaml` preservando a imutabilidade das migrations 01 a 35.
- **Task 2 (Domínio & Use Cases no `common`):** Entidades imutáveis `WebPushPreference`, `WebPushSubscription`, `WebPushNotificationLog`, `WebPushAlertType`, `WebPushDeliveryStatus`, `WebPushSendResult`; portas `WebPushPreferencePort`, `WebPushSubscriptionPort`, `WebPushNotificationLogPort`, `WebPushGatewayPort`; DTOs completos de preferências, requisições, assinaturas e logs; use cases com clamp de margem (`0%` a `90%`), validação de ownership e endpoints, expiração automática de assinaturas em HTTP 404/410, idempotência por `dedupKey`, mascaração em logs (`WebPushMaskingUtils`) e guarda contra dados obsoletos/outliers em `EvaluateGrahamBuyZoneAlertsUseCase`.
- **Task 3 (Adapters JPA, Gateways, Scheduler & REST no `api`):** Adaptadores JPA com truncamento preventivo; gateway `MockWebPushGatewayAdapter` e `VapidWebPushGatewayAdapter` nativo; Spring configuration com propriedades `webpush.*`; agendador `WebPushAlertScheduler` com ShedLock e cron B3 (`20:15 America/Sao_Paulo`); controller `WebPushNotificationsController` mapeando 8 endpoints sob `/api/v1/notifications/web-push` com extração estrita de `userId` via token JWT e mapeamento de `WebPushSubscriptionNotFoundException` (404) no `GlobalExceptionHandler`.
- **Task 4 (Frontend Flutter Web, Service Worker & UI):** Service worker `frontend/web/push-sw.js` com tratamento dos eventos `push` e `notificationclick` para focar/abrir `/graham-recommendations`; cliente condicional `WebPushBrowserClient` via `dart:js_interop`; models, datasource, repository, provider com carregamento assíncrono seguro e página de preferências/assinaturas/histórico `WebPushNotificationsPage`; rota `/notifications/web-push` no `main.dart` e card de atalho em `ProfilePage`.
- **Task 5 (Testes & Documentação C4):** 25 testes unitários em `common`, 13 testes em `api` (incluindo testes de crypto VAPID RFC 8291/8292 e controller MockMvc), 10 testes de widget e provider no frontend Flutter; 295/295 testes de frontend verdes; zero warnings no `flutter analyze`; `docs/workspace.dsl` atualizado com `browserPushServices` e `webPushNotificationsService`.

### File List

- `backend/common/src/main/resources/db/changelog/36-create-web-push-notifications-tables.yaml` (Novo)
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (Modificado)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WebPushAlertType.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WebPushDeliveryStatus.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WebPushSendResult.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WebPushPreference.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WebPushSubscription.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/domain/entities/WebPushNotificationLog.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/repository/WebPushPreferencePort.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/repository/WebPushSubscriptionPort.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/repository/WebPushNotificationLogPort.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/repository/WebPushGatewayPort.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/exceptions/WebPushSubscriptionNotFoundException.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/WebPushVapidKeyDto.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/WebPushPreferenceDto.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/UpdateWebPushPreferenceRequest.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/RegisterWebPushSubscriptionRequest.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/WebPushSubscriptionDto.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/WebPushNotificationLogDto.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/dto/WebPushTestAlertResultDto.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/WebPushMaskingUtils.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/GetWebPushPreferencesUseCase.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/UpdateWebPushPreferencesUseCase.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/RegisterWebPushSubscriptionUseCase.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/ListWebPushSubscriptionsUseCase.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/DeleteWebPushSubscriptionUseCase.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/GetWebPushNotificationHistoryUseCase.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/SendWebPushAlertUseCase.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/SendTestWebPushAlertUseCase.java` (Novo)
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateGrahamBuyZoneAlertsUseCase.java` (Novo)
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/UpdateWebPushPreferencesUseCaseTest.java` (Novo)
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/RegisterWebPushSubscriptionUseCaseTest.java` (Novo)
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/SendWebPushAlertUseCaseTest.java` (Novo)
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateGrahamBuyZoneAlertsUseCaseTest.java` (Novo)
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/DeleteWebPushSubscriptionUseCaseTest.java` (Novo)
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/SendTestWebPushAlertUseCaseTest.java` (Novo)
- `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/WebPushMaskingUtilsTest.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WebPushPreferenceEntity.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/SpringDataWebPushPreferenceRepository.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WebPushPreferenceJpaAdapter.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WebPushSubscriptionEntity.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/SpringDataWebPushSubscriptionRepository.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WebPushSubscriptionJpaAdapter.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WebPushNotificationLogEntity.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/SpringDataWebPushNotificationLogRepository.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/persistence/WebPushNotificationLogJpaAdapter.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/MockWebPushGatewayAdapter.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/WebPushCryptoUtils.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/spring/WebPushNotificationsConfiguration.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/spring/WebPushAlertScheduler.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/web/WebPushNotificationsController.java` (Novo)
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java` (Modificado)
- `backend/api/src/main/resources/application.yml` (Modificado)
- `backend/api/src/test/java/afsdigital/grahamselect/api/notifications/web/WebPushNotificationsControllerTest.java` (Novo)
- `backend/api/src/test/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/WebPushCryptoUtilsTest.java` (Novo)
- `backend/api/src/test/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapterTest.java` (Novo)
- `frontend/web/push-sw.js` (Novo)
- `frontend/lib/src/features/notifications/data/services/web_push_browser_client.dart` (Novo)
- `frontend/lib/src/features/notifications/data/services/web_push_browser_client_stub.dart` (Novo)
- `frontend/lib/src/features/notifications/data/services/web_push_browser_client_web.dart` (Novo)
- `frontend/lib/src/features/notifications/domain/models/web_push_notification_models.dart` (Novo)
- `frontend/lib/src/features/notifications/data/datasources/web_push_notifications_remote_data_source.dart` (Novo)
- `frontend/lib/src/features/notifications/data/repositories/web_push_notifications_repository.dart` (Novo)
- `frontend/lib/src/features/notifications/presentation/providers/web_push_notifications_provider.dart` (Novo)
- `frontend/lib/src/features/notifications/presentation/pages/web_push_notifications_page.dart` (Novo)
- `frontend/lib/src/features/profile/presentation/pages/profile_page.dart` (Modificado)
- `frontend/lib/main.dart` (Modificado)
- `frontend/test/features/notifications/web_push_notifications_provider_test.dart` (Novo)
- `frontend/test/features/notifications/web_push_notifications_page_test.dart` (Novo)
- `docs/workspace.dsl` (Modificado)
- `docs/bmad/implementation-artifacts/11-2-web-push-para-zona-de-compra-graham.md` (Modificado)
- `docs/bmad/implementation-artifacts/sprint-status.yaml` (Modificado)