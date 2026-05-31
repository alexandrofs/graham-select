# Story 3.5: Atualização em Tempo Real (Event-Driven UI)

Status: in-progress

## Story

As a usuário,
I want que meu dashboard se atualize automaticamente quando um novo upload ou operação manual for concluído,
so that eu não precise dar "refresh" na página manualmente.

## Acceptance Criteria

1. **Given** que o usuário está com o Dashboard aberto
2. **When** o Kafka processa um evento `trade-extracted` (Epic 2)
3. **Then** o backend envia uma notificação push/socket para o frontend
4. **And** o Dashboard recarrega apenas os componentes de dados afetados com uma animação de fade (250ms)

## Tasks / Subtasks

### Backend (Spring Boot — módulo `api`)

- [ ] Criar endpoint SSE (Server-Sent Events) `GET /api/v1/notifications/stream` no módulo `api` (AC: 3)
  - [ ] Criar a interface Port `NotificationPort` em `common/src/.../portfolio/application/repository/`
  - [ ] Criar o Use Case `SendPortfolioUpdateNotificationUseCase` em `common/src/.../portfolio/application/usecase/` como POJO puro (sem Spring)
  - [ ] Criar o adapter SSE `SseNotificationAdapter` em `api/src/.../portfolio/infrastructure/sse/` implementando `NotificationPort` usando `SseEmitter`
  - [ ] Criar o controller REST `NotificationController` em `api/src/.../portfolio/web/` com o endpoint `/api/v1/notifications/stream`
  - [ ] Registrar o `SseNotificationAdapter` no `PortfolioSummaryConfiguration` (ou criar nova `@Configuration` dedicada)

- [ ] Integrar o envio de notificação no `KafkaTradeExtractedConsumer` existente (AC: 2)
  - [ ] Após `saveTradeUseCase.execute(trade)` com sucesso (trade não nulo), chamar `sendPortfolioUpdateNotificationUseCase.execute(event.userId())`
  - [ ] A notificação deve conter: `userId`, `event: "PORTFOLIO_UPDATED"`, `timestamp`

- [ ] Adicionar permissão no `SecurityConfig` para o novo endpoint SSE (AC: 3)
  - [ ] Garantir que `/api/v1/notifications/stream` requer autenticação JWT
  - [ ] Configurar CORS no `WebConfig` para aceitar o header `Accept: text/event-stream`

- [ ] Escrever testes unitários (AC: 3)
  - [ ] `SendPortfolioUpdateNotificationUseCaseTest` validando que o Port é chamado corretamente
  - [ ] `NotificationControllerTest` (mock MVC) validando o endpoint SSE
  - [ ] Atualizar `KafkaTradeExtractedConsumerTest` para verificar que a notificação é enviada após salvar o trade

- [ ] Executar `mvn clean test` no módulo `api` (AC: 3)

### Frontend (Flutter)

- [ ] Criar o `NotificationService` em `lib/src/features/portfolio/data/datasources/notification_service.dart` (AC: 3)
  - [ ] Conectar ao endpoint SSE via `Dio` ou `http` package para streaming
  - [ ] Implementar `Stream<PortfolioNotificationEvent>` que emite quando o evento `PORTFOLIO_UPDATED` chega
  - [ ] Gerenciar reconexão automática com backoff exponencial em caso de falha

- [ ] Criar a entidade de domínio `PortfolioNotificationEvent` em `lib/src/features/portfolio/domain/entities/` (AC: 3)
  - [ ] Campos: `userId`, `event` (String), `timestamp` (DateTime)

- [ ] Integrar o `NotificationService` no `PortfolioProvider` (AC: 4)
  - [ ] Adicionar método `startListeningForUpdates()` que inicia a escuta do stream SSE
  - [ ] Quando um evento `PORTFOLIO_UPDATED` chegar, chamar `loadSummary()` e `loadCustodyPositions()` em paralelo
  - [ ] Adicionar método `stopListeningForUpdates()` para cancelar o stream na destruição do Provider
  - [ ] Adicionar estado `_isReceivingLiveUpdates` (bool) e getter para exibir indicador visual

- [ ] Adicionar indicador visual de "atualização ao vivo" no `DashboardPage` (AC: 4)
  - [ ] Exibir um chip/badge sutil mostrando "● Ao vivo" (ponto verde pulsante) quando SSE conectado
  - [ ] Animar os componentes afetados (KPI Cards, tabela de custódia) com fade de 250ms ao receber atualização
  - [ ] Usar `AnimatedOpacity` ou `AnimatedSwitcher` com duração de 250ms

- [ ] Registrar o `NotificationService` no container de DI (ex: `main.dart` ou provider setup) (AC: 3)

- [ ] Rodar `flutter analyze` e garantir zero warnings/erros (AC: 4)

## Dev Notes

### Tecnologia de Notificação: SSE (Server-Sent Events)

**Decisão**: Usar **SSE (Server-Sent Events)** em vez de WebSockets.

**Racional**:
- O Spring Boot MVC (WebMVC, não WebFlux) tem suporte nativo a SSE via `SseEmitter` — sem dependências extras
- A comunicação é **unidirecional** (servidor → cliente), que é tudo que precisamos para este caso de uso
- WebSockets exigiria adicionar `spring-boot-starter-websocket` e configurar `WebSocketConfig` — overhead desnecessário
- SSE funciona perfeitamente com HTTP/2 (que Flutter Web pode usar) e é mais simples de testar
- Padrão mais leve e adequado para este MVP
- O `flutter_client_sse` package (ou implementação manual com `Dio` streaming) suporta SSE no Flutter

**Alternativa considerada e rejeitada**: Polling HTTP (implementado no `UploadProvider`). Adequado para status de upload (operação de curta duração), mas ineficiente para manter dashboard atualizado continuamente.

### Backend — Estrutura de Pastas Esperada

```
backend/
├── common/
│   └── src/main/java/afsdigital/grahamselect/
│       └── portfolio/
│           ├── application/
│           │   ├── usecase/
│           │   │   └── SendPortfolioUpdateNotificationUseCase.java  ← NOVO (POJO)
│           │   └── repository/
│           │       └── NotificationPort.java  ← NOVO (interface)
└── api/
    └── src/main/java/afsdigital/grahamselect/api/
        └── portfolio/
            ├── web/
            │   └── NotificationController.java  ← NOVO
            └── infrastructure/
                ├── sse/
                │   └── SseNotificationAdapter.java  ← NOVO
                └── spring/
                    └── PortfolioSummaryConfiguration.java  ← MODIFICAR (adicionar bean)
```

### Backend — Padrão de Implementação SSE

```java
// NotificationPort.java (common/portfolio/application/repository/) — POJO, sem Spring
public interface NotificationPort {
    void sendPortfolioUpdate(String userId);
}

// SendPortfolioUpdateNotificationUseCase.java — POJO puro (sem Spring)
@RequiredArgsConstructor
@Slf4j
public class SendPortfolioUpdateNotificationUseCase {
    private final NotificationPort notificationPort;
    
    public void execute(String userId) {
        log.info("Sending portfolio update notification for user: {}", userId);
        notificationPort.sendPortfolioUpdate(userId);
    }
}

// SseNotificationAdapter.java (api/portfolio/infrastructure/sse/)
@Component
@Slf4j
public class SseNotificationAdapter implements NotificationPort {
    private final Map<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    
    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(0L); // sem timeout
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));
        return emitter;
    }
    
    @Override
    public void sendPortfolioUpdate(String userId) {
        List<SseEmitter> emitters = userEmitters.getOrDefault(userId, List.of());
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("PORTFOLIO_UPDATED")
                    .data(Map.of("userId", userId, "timestamp", OffsetDateTime.now()))
                );
            } catch (IOException e) {
                log.warn("Failed to send SSE event to user {}: {}", userId, e.getMessage());
                removeEmitter(userId, emitter);
            }
        });
    }
    
    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) emitters.remove(emitter);
    }
}
```

### Backend — Integração com KafkaTradeExtractedConsumer

```java
// KafkaTradeExtractedConsumer.java — MODIFICAR
@KafkaListener(topics = TopicConstants.TRADE_EXTRACTED_TOPIC, groupId = "api-group")
public void consume(String payload) throws JsonProcessingException {
    // ... código existente ...
    try {
        Trade savedTrade = saveTradeUseCase.execute(trade);
        if (savedTrade == null) {
            // ... código existente para duplicatas ...
        } else {
            log.info("New trade saved: {} for user {} at {}", ...);
            // NOVO: Notificar frontend
            sendPortfolioUpdateNotificationUseCase.execute(event.userId());
        }
    } catch (...) {
        // ... código existente ...
    }
}
```

### Frontend — Estrutura de Pastas Esperada

```
frontend/lib/src/features/portfolio/
├── domain/
│   └── entities/
│       └── portfolio_notification_event.dart  ← NOVO
├── data/
│   └── datasources/
│       └── notification_service.dart  ← NOVO
└── presentation/
    ├── providers/
    │   └── portfolio_provider.dart  ← MODIFICAR
    └── pages/
        └── dashboard_page.dart  ← MODIFICAR (indicador visual)
```

### Frontend — Implementação SSE com Dio

```dart
// notification_service.dart
class NotificationService {
  final Dio _dio;
  StreamController<PortfolioNotificationEvent>? _controller;
  
  NotificationService(this._dio);
  
  Stream<PortfolioNotificationEvent> get notificationStream {
    _controller ??= StreamController<PortfolioNotificationEvent>.broadcast();
    return _controller!.stream;
  }
  
  Future<void> connect() async {
    try {
      final response = await _dio.get<ResponseBody>(
        '/api/v1/notifications/stream',
        options: Options(
          responseType: ResponseType.stream,
          headers: {'Accept': 'text/event-stream'},
        ),
      );
      response.data?.stream.listen(
        (data) {
          final text = String.fromCharCodes(data);
          if (text.contains('PORTFOLIO_UPDATED')) {
            _controller?.add(PortfolioNotificationEvent(
              event: 'PORTFOLIO_UPDATED',
              timestamp: DateTime.now(),
            ));
          }
        },
        onError: (_) => _reconnect(),
        onDone: () => _reconnect(),
      );
    } catch (_) {
      _reconnect();
    }
  }
  
  // Reconexão com backoff exponencial (2s, 4s, 8s... max 30s)
  void _reconnect() {
    Future.delayed(const Duration(seconds: 5), connect);
  }
  
  void dispose() {
    _controller?.close();
    _controller = null;
  }
}
```

### Frontend — Animação de Fade 250ms

```dart
// DashboardPage — usar AnimatedSwitcher para o fade
AnimatedSwitcher(
  duration: const Duration(milliseconds: 250),
  child: _buildKpiSection(context), // chave muda quando os dados mudam
)
```

### Regras de Arquitetura Obrigatórias

1. **Clean Architecture**: `NotificationPort` (interface) fica em `common/application/repository/` — **nunca** em `infrastructure/`
2. **Use Case é POJO**: `SendPortfolioUpdateNotificationUseCase` sem `@Component`, wired via `@Configuration`
3. **Thread Safety**: `SseNotificationAdapter` usa `ConcurrentHashMap` + `CopyOnWriteArrayList` pois vários threads Kafka podem chamar simultaneamente
4. **JWT obrigatório**: O endpoint SSE `/api/v1/notifications/stream` deve estar protegido pelo `SecurityConfig` igual aos demais endpoints de portfolio
5. **Isolamento de usuário**: O `NotificationPort.sendPortfolioUpdate(userId)` DEVE filtrar por userId — cada usuário recebe apenas suas próprias notificações
6. **Sem import Spring em common**: `NotificationPort` e `SendPortfolioUpdateNotificationUseCase` são POJOs sem anotações Spring

### Referência: Padrão de Tópico Kafka Existente

```
TopicConstants.TRADE_EXTRACTED_TOPIC = "trade-extracted"
```
O `KafkaTradeExtractedConsumer` já processa este tópico no grupo `api-group`. Não é necessário criar novo consumidor — apenas adicionar a chamada de notificação após salvar o trade.

### Dependências Flutter — Nenhuma nova necessária

O `Dio` (^5.8.0) já está no `pubspec.yaml` e suporta streaming via `ResponseType.stream`. **Não adicionar** o package `flutter_client_sse` — implementar o cliente SSE diretamente com `Dio` para evitar dependências extras e manter consistência com o padrão já estabelecido no projeto (`api_client.dart`).

### Referência: Injeção de Dependências Backend

O padrão do projeto usa `@Configuration` classes para wiring:

```java
// Nova @Configuration ou modificar PortfolioSummaryConfiguration.java
@Bean
public SendPortfolioUpdateNotificationUseCase sendPortfolioUpdateNotificationUseCase(
        NotificationPort notificationPort) {
    return new SendPortfolioUpdateNotificationUseCase(notificationPort);
}
```

### Project Structure Notes

- Alinhamento com a estrutura Clean Architecture definida em `docs/bmad/planning-artifacts/architecture.md#D12`
- O `SseNotificationAdapter` é um adapter de infrastructure, similar ao `KafkaTradeExtractionEventPublisher` existente
- O `NotificationController` segue o padrão dos outros controllers em `api/portfolio/web/` (ex: `PortfolioController`, `TradeManualController`)
- Não há conflito com a funcionalidade de polling do `UploadProvider` (que usa `Timer.periodic` para status de upload — caso de uso diferente)

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 3.5]
- [Source: docs/bmad/planning-artifacts/architecture.md#D9 Kafka Topics]
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean Architecture]
- [Source: docs/bmad/planning-artifacts/architecture.md#D12 Estrutura Flutter]
- [Source: backend/common/.../TopicConstants.java]
- [Source: backend/api/.../KafkaTradeExtractedConsumer.java]
- [Source: frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart]
- [Source: frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart]
- [Source: frontend/pubspec.yaml — Dio ^5.8.0 já disponível]

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.6 (Thinking)

### Debug Log References

### Completion Notes List

### File List
