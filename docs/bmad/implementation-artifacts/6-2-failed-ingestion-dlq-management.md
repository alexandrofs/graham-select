---
baseline_commit: c10845ada41200ad6458eefe74414f03487de4c6
---
# Story 6.2: Reprocessamento de Importações Falhas (DLQ UI)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** administrador,  
**I want** visualizar os arquivos que falharam no processamento e disparar o reprocessamento manual,  
**So that** o usuário não precise fazer o upload novamente em caso de erro transiente no servidor.

---

## Acceptance Criteria

1. **AC1 - Listagem de Falhas de Ingestão na DLQ UI (`/admin/dlq` e API `GET /api/v1/admin/dlq`) (FR27):**
   - **Given** que existem mensagens na Dead Letter Queue (`trade-extracted-dlq`) do Kafka que foram consumidas e persistidas com status `PENDING`
   - **When** o administrador acessa a lista de "Falhas de Ingestão" (pela rota `/admin/dlq` no frontend ou endpoint `GET /api/v1/admin/dlq`)
   - **Then** o sistema lista cada item de falha contendo:
     1. **Arquivo:** Nome do arquivo de origem (`fileName`);
     2. **Erro Reportado:** Motivo/descrição do erro capturado (`errorMessage` / `reason`);
     3. **Timestamp:** Data e hora da falha registrada em UTC (`ZoneOffset.UTC`);
     4. **Linha & Correlação:** Número da linha da planilha (`lineNumber`) e `correlationId` para rastreabilidade;
     5. **Identificador Mascarado:** Usuário anonimizado no formato `usr_***xxxx` (via SHA-256 do `userId`, conforme NFR5 e Story 6.1);
   - **And** a listagem é ordenada de forma decrescente pela data de criação (`createdAt DESC`)
   - **And** se não houver falhas pendentes, a interface exibe um estado visual vazio elegante (`Key('admin_dlq_empty_state')`, "Nenhuma falha de ingestão pendente").

2. **AC2 - Reprocessamento Manual Individual com Republicação no Tópico Original (FR27):**
   - **Given** um item de falha com status `PENDING` na lista de Dead Letter Queue
   - **When** o administrador clica no botão "Reprocessar" (`Key('admin_dlq_reprocess_button_${id}')` no frontend chamando `POST /api/v1/admin/dlq/{id}/reprocess` no backend)
   - **Then** o sistema:
     1. Recupera o payload original (`originalPayload`);
     2. Valida e reconstrói o evento de domínio (`TradeExtractedEvent`) gerando um `eventId` no formato UUID válido (`UUID.randomUUID().toString()`) para evitar erros no `UUID.fromString(event.eventId())` do consumer consumidor;
     3. Republíca o evento no tópico original do Kafka (`TopicConstants.TRADE_EXTRACTED_TOPIC` = `"trade-extracted"`) com a chave de particionamento `userId + ":" + ticker` para nova tentativa de persistência na carteira e consolidação de patrimônio;
   - **And** se a republicação e reprocessamento forem bem-sucedidos, atualiza o status do registro no banco para `RESOLVED` (com timestamp `resolvedAt` em `ZoneOffset.UTC`)
   - **And** remove o item imediatamente da lista de falhas ativas exibida na interface do administrador, exibindo um SnackBar verde de confirmação ("Item reprocessado com sucesso!")
   - **And** caso o item já esteja com status `RESOLVED`, retorna resposta idempotente informando que já foi resolvido sem duplicar processamento
   - **And** caso o `id` não exista, retorna status HTTP `404 Not Found` no formato RFC 7807 (`application/problem+json`).

3. **AC3 - Reprocessamento em Lote (Batch Reprocess) de Falhas (FR27):**
   - **Given** múltiplos itens pendentes na DLQ selecionados pelo administrador via checkboxes (ou através do botão "Reprocessar Todos", enviando lista vazia ou nula)
   - **When** o administrador clica em "Reprocessar Selecionados" (`Key('admin_dlq_reprocess_batch_button')` chamando `POST /api/v1/admin/dlq/reprocess-batch`)
   - **Then** o sistema executa o reprocessamento de cada item individualmente em lote com isolamento de falhas (a falha de um registro não interrompe os demais)
   - **And** contabiliza e retorna um resumo consolidado (`totalProcessed`, `successCount`, `failureCount`, `details`) no envelope padrão `{ "data": { ... } }`
   - **And** remove todos os itens resolvidos com sucesso da lista de falhas ativas na interface, atualizando os contadores visuais e mantendo apenas os itens que ainda requererem intervenção.

4. **AC4 - Tratamento Resiliente de Erro Persistente / Dados Inválidos:**
   - **Given** um item cujo payload continue inválido (ex.: formato numérico corrompido, ticker ausente) ou ocorra um erro irrecuperável no reprocessamento
   - **When** o reprocessamento individual ou em lote é executado
   - **Then** o sistema aplica parser resiliente de datas (suportando `dd/MM/yyyy` e `yyyy-MM-dd`) e números (removendo `R$`, espaços e tratando vírgulas)
   - **And** se o erro persistir, o sistema não quebra a execução em lote, atualiza o status do item para `FAILED` registrando a mensagem de erro atualizada, mantendo o histórico de auditoria
   - **And** a interface exibe feedback contextual de erro para orientar o administrador sobre o motivo da rejeição.

5. **AC5 - Controle de Acesso Baseado em Papel (`ROLE_ADMIN`) e Anonimização:**
   - **Given** um usuário comum autenticado (`role: USER`) que tente invocar qualquer endpoint sob `/api/v1/admin/dlq/**`
   - **When** a requisição atinge o Spring Security / `AdminAccessGuard` (`@RequireAdmin`)
   - **Then** o backend rejeita a chamada com status HTTP `403 Forbidden` no formato padrão RFC 7807 (`application/problem+json`, `type: "urn:problem-type:access-denied"`)
   - **And** requisições não autenticadas retornam HTTP `401 Unauthorized`
   - **And** caso um usuário não-admin acesse `/admin/dlq` no Flutter Web, a tela exibe o estado amigável de **"Acesso Restrito a Administradores"** (`Key('admin_dlq_access_denied_state')`) com botão para retornar ao `/dashboard`
   - **And** identificadores de usuários são preservados com isolamento ou anonimizados na visualização (`usr_***xxxx` via SHA-256 do `userId`).

6. **AC6 - Logs Estruturados, Padrão UTC e Qualidade de Código (`AGENTS.md` & `project-context.md`):**
   - **Given** as operações da feature de DLQ
   - **When** qualquer API REST é invocada ou mensagem de tópico Kafka é consumida
   - **Then** o sistema registra logs de nível `INFO` nas entradas contendo os identificadores contextuais relevantes (principal, IDs, correlationId, tópicos) e logs de nível `ERROR` com stack trace completo em blocos `catch`
   - **And** todas as datas e horários utilizam estritamente `ZoneOffset.UTC`
   - **And** todos os testes unitários, testes de integração e análise estática (`mvn clean test` com JDK 21/22 no backend, `flutter analyze` e `flutter test` no frontend) passam com 100% de sucesso e zero warnings.

---

## Tasks / Subtasks

### Backend — Módulo `common` (Persistência, Liquibase e Entidades DLQ)

- [x] **Task 1: Migração Liquibase `26-create-dlq-messages-table.yaml`, Entidade `DlqMessageEntity` e Enum `DlqMessageStatus`** (AC: #1, #2, #3, #6)
  - [x] 1.1 Criar changelog Liquibase `backend/common/src/main/resources/db/changelog/26-create-dlq-messages-table.yaml`:
    - Tabela: `dlq_messages`
    - Colunas:
      - `id`: `BIGINT` auto-increment, `primaryKey: true`, `nullable: false`
      - `event_id`: `VARCHAR(36)`, `nullable: false`, `unique: true`
      - `correlation_id`: `VARCHAR(36)`, `nullable: false`
      - `user_id`: `VARCHAR(255)`, `nullable: false`
      - `file_name`: `VARCHAR(255)`, `nullable: false`
      - `line_number`: `BIGINT`, `nullable: true`
      - `error_message`: `VARCHAR(1000)`, `nullable: false`
      - `original_payload`: `TEXT` (ou `CLOB` cross-database MySQL/H2), `nullable: true` (JSON serializado do `Map<String, String>` recebido no `TradeExtractionFailedEvent`)
      - `original_topic`: `VARCHAR(100)`, `nullable: false`, `defaultValue: "trade-extracted"`
      - `status`: `VARCHAR(30)`, `nullable: false`, `defaultValue: "PENDING"`
      - `created_at`: `DATETIME`, `nullable: false`
      - `updated_at`: `DATETIME`, `nullable: false`
      - `resolved_at`: `DATETIME`, `nullable: true`
    - Índices:
      - `idx_dlq_messages_status_created` nas colunas `(status, created_at)`
      - `idx_dlq_messages_correlation` na coluna `correlation_id`
    - Rollback explícito:
      - `dropTable tableName="dlq_messages"`
  - [x] 1.2 Registrar `26-create-dlq-messages-table.yaml` no final de `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (após `25-add-role-to-users.yaml`).
  - [x] 1.3 Criar enum `DlqMessageStatus` em `backend/common/src/main/java/afsdigital/grahamselect/common/dlq/domain/entities/DlqMessageStatus.java`:
    - Valores: `PENDING`, `REPROCESSING`, `RESOLVED`, `FAILED`.
  - [x] 1.4 Criar entidade JPA `DlqMessageEntity` em `backend/common/src/main/java/afsdigital/grahamselect/common/dlq/infrastructure/persistence/jpa/entities/DlqMessageEntity.java`:
    - Anotações `@Entity`, `@Table(name = "dlq_messages")`, `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`.
    - Métodos auxiliares de ciclo de vida (`@PrePersist`, `@PreUpdate`) garantindo `created_at` e `updated_at` com `LocalDateTime.now(ZoneOffset.UTC)`.
  - [x] 1.5 Criar repositório `DlqMessageRepository` em `backend/common/src/main/java/afsdigital/grahamselect/common/dlq/infrastructure/persistence/jpa/repositories/DlqMessageRepository.java`:
    - Métodos:
      - `List<DlqMessageEntity> findByStatusOrderByCreatedAtDesc(DlqMessageStatus status);`
      - `List<DlqMessageEntity> findAllByOrderByCreatedAtDesc();`
      - `Optional<DlqMessageEntity> findByEventId(String eventId);`
      - `long countByStatus(DlqMessageStatus status);`
      - `List<DlqMessageEntity> findByIdIn(List<Long> ids);`

---

### Backend — Módulo `api` (Consumer Kafka DLQ, Serviço de Reprocessamento e Endpoints REST)

- [x] **Task 2: Implementar Consumer Kafka para `trade-extracted-dlq` (`KafkaTradeExtractionDlqConsumer`)** (AC: #1, #6)
  - [x] 2.1 Criar `KafkaTradeExtractionDlqConsumer` em `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/infrastructure/messaging/KafkaTradeExtractionDlqConsumer.java`:
    - Anotações `@Component`, `@RequiredArgsConstructor`, `@Slf4j`.
    - Método `@KafkaListener(topics = TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC, groupId = "${spring.kafka.consumer.group-id:api-dlq-consumer-group}")`:
      - Registrar `log.info("Recebida mensagem no tópico DLQ {}: {}", TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC, payload)`.
      - Fazer parse do payload com `ObjectMapper` para `TradeExtractionFailedEvent`.
      - **Idempotência:** Verificar se `dlqMessageRepository.findByEventId(event.eventId())` já existe; se sim, logar aviso e ignorar para evitar duplicidade.
      - Serializar `event.originalPayload()` para JSON string.
      - Construir e persistir `DlqMessageEntity` com `status = DlqMessageStatus.PENDING`, `createdAt` e `updatedAt` em `LocalDateTime.now(ZoneOffset.UTC)`.
      - Capturar exceções com bloco `try / catch`, registrando `log.error("Erro ao persistir mensagem DLQ: {}", ex.getMessage(), ex)`.

- [x] **Task 3: Reuso de Anonimização e Implementar `DlqManagementService`** (AC: #1, #2, #3, #4, #5, #6)
  - [x] 3.1 Criar ou extrair utilitário de anonimização `AdminAnonymizationUtils` em `backend/api/src/main/java/afsdigital/grahamselect/api/admin/util/AdminAnonymizationUtils.java`:
    - Método estático `public static String anonymizeUserId(String rawUserId)`: SHA-256 do `userId` gerando prefixo `usr_***` + primeiros 4 caracteres hexadecimais em lowercase (ex: `usr_***a1b2`).
    - Evita duplicar código entre `AdminObservabilityService` e `DlqManagementService`.
  - [x] 3.2 Criar DTOs em `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/`:
    - `DlqMessageDto(Long id, String eventId, String correlationId, String anonymizedUserId, String fileName, Long lineNumber, String errorMessage, String originalTopic, DlqMessageStatus status, OffsetDateTime timestamp, OffsetDateTime resolvedAt)` (record Java 21).
    - `DlqReprocessResultDto(Long id, DlqMessageStatus status, String message)` (record).
    - `DlqBatchReprocessRequest(List<Long> ids)` (record).
    - `DlqBatchReprocessResultDto(int totalProcessed, int successCount, int failureCount, List<DlqReprocessResultDto> details)` (record).
  - [x] 3.3 Criar `DlqManagementService` em `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java`:
    - Injetar `DlqMessageRepository`, `KafkaTemplate<String, Object>` e `ObjectMapper`.
    - **Método `getPendingFailures(DlqMessageStatus status)`:**
      - Se `status == null`, usar `DlqMessageStatus.PENDING`.
      - Consultar `dlqMessageRepository.findByStatusOrderByCreatedAtDesc(status)`.
      - Mapear cada entidade para `DlqMessageDto`, utilizando `AdminAnonymizationUtils.anonymizeUserId(entity.getUserId())` e convertendo datas para `OffsetDateTime` em `ZoneOffset.UTC`.
    - **Método `reprocess(Long id)`:**
      - Buscar entidade por `id`. Lançar `ResourceNotFoundException` caso não encontrada.
      - Se `entity.getStatus() == DlqMessageStatus.RESOLVED`, retornar `new DlqReprocessResultDto(id, DlqMessageStatus.RESOLVED, "Mensagem já resolvida anteriormente.")`.
      - Desserializar `entity.getOriginalPayload()` de volta para `Map<String, String>`. Se nulo ou vazio, lançar exceção de payload inválido.
      - **Reconstrução do Evento:** Extrair campos `"ticker"`, `"data"`, `"quantidade"`, `"preco"`, `"side"`, `"corretora"` do mapa com busca insensível a maiúsculas/minúsculas:
        - Converter data resiliente: tentar formato brasileiro `"dd/MM/yyyy"` e fallback para ISO `"yyyy-MM-dd"`.
        - Converter quantidade e preço: remover `"R$"`, espaços em branco, converter vírgula decimal para ponto e instanciar `new BigDecimal(...)` com escala controlada. Validar valores positivos (> 0).
        - Se o parse for bem-sucedido:
          - Construir `TradeExtractedEvent`:
            ```java
            TradeExtractedEvent tradeEvent = TradeExtractedEvent.builder()
                    .eventId(UUID.randomUUID().toString()) // Formato UUID válido obrigatório para o consumer!
                    .version("v1")
                    .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                    .userId(entity.getUserId())
                    .correlationId(entity.getCorrelationId())
                    .fileName(entity.getFileName())
                    .lineNumber(entity.getLineNumber() != null ? entity.getLineNumber() : 1L)
                    .ticker(ticker.toUpperCase().trim())
                    .tradeDate(parsedDate)
                    .side(side != null && !side.isBlank() ? side.toUpperCase().trim() : "COMPRA")
                    .quantity(parsedQuantity)
                    .price(parsedPrice)
                    .broker(broker)
                    .build();
            ```
          - Publicar no Kafka:
            `kafkaTemplate.send(TopicConstants.TRADE_EXTRACTED_TOPIC, tradeEvent.userId() + ":" + tradeEvent.ticker(), tradeEvent);`
          - Atualizar entidade: `entity.setStatus(DlqMessageStatus.RESOLVED); entity.setResolvedAt(LocalDateTime.now(ZoneOffset.UTC)); dlqMessageRepository.save(entity);`
          - Registrar `log.info("Mensagem DLQ id: {} reprocessada com sucesso e publicada no tópico {}", id, TopicConstants.TRADE_EXTRACTED_TOPIC)`.
          - Retornar `new DlqReprocessResultDto(id, DlqMessageStatus.RESOLVED, "Mensagem reprocessada com sucesso.")`.
        - Se o parse falhar:
          - Registrar `log.error("Falha ao reprocessar mensagem DLQ id: {}: {}", id, ex.getMessage(), ex)`.
          - Atualizar entidade: `entity.setStatus(DlqMessageStatus.FAILED); entity.setErrorMessage("Falha de reprocessamento: " + ex.getMessage()); dlqMessageRepository.save(entity);`
          - Retornar `new DlqReprocessResultDto(id, DlqMessageStatus.FAILED, ex.getMessage())`.
    - **Método `reprocessBatch(List<Long> ids)`:**
      - Se `ids == null || ids.isEmpty()`, buscar todos os IDs com status `PENDING` via repositório.
      - Executar `reprocess(id)` para cada um com isolamento de falha (try/catch individual), agregando em `List<DlqReprocessResultDto>`.
      - Computar `successCount` (status `RESOLVED`), `failureCount` (status `FAILED`) e `totalProcessed`.
      - Retornar `DlqBatchReprocessResultDto`.

- [x] **Task 4: Implementar `AdminDlqController` e Atualizar Contrato OpenAPI** (AC: #1, #2, #3, #5, #6)
  - [x] 4.1 Atualizar `backend/api/src/main/resources/openapi.yaml`:
    - Adicionar tags `Admin - DLQ` e endpoints:
      - `GET /api/v1/admin/dlq` (suportando query param opcional `status`, padrão `PENDING`)
      - `POST /api/v1/admin/dlq/{id}/reprocess`
      - `POST /api/v1/admin/dlq/reprocess-batch`
    - Adicionar schemas correspondentes com envelope `{ "data": ... }`, exemplos e respostas RFC 7807 (`401 Unauthorized`, `403 Forbidden`, `404 Not Found`).
  - [x] 4.2 Criar `AdminDlqController` em `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/web/AdminDlqController.java`:
    - Anotações `@RestController`, `@RequestMapping("/api/v1/admin/dlq")`, `@RequiredArgsConstructor`, `@Slf4j`, `@RequireAdmin`.
    - Endpoint `GET`:
      - Registrar `log.info("Requisição para listar falhas DLQ recebida do principal: {}", principal)`.
      - Retornar `ResponseEntity.ok(Map.of("data", dlqManagementService.getPendingFailures(status)))`.
    - Endpoint `POST /{id}/reprocess`:
      - Registrar `log.info("Requisição para reprocessar falha DLQ id: {} recebida do principal: {}", id, principal)`.
      - Retornar `ResponseEntity.ok(Map.of("data", dlqManagementService.reprocess(id)))`.
    - Endpoint `POST /reprocess-batch`:
      - Registrar `log.info("Requisição para reprocessamento em lote recebida do principal: {}", principal)`.
      - Retornar `ResponseEntity.ok(Map.of("data", dlqManagementService.reprocessBatch(request != null ? request.ids() : null)))`.

---

### Frontend Flutter — Feature `admin` (DLQ UI, Provider e Navegação)

- [x] **Task 5: Camada de Domínio, Dados e Datasource para DLQ** (AC: #1, #2, #3, #5)
  - [x] 5.1 Criar entidade `AdminDlqMessage` em `frontend/lib/src/features/admin/domain/entities/admin_dlq_message.dart`:
    - Campos: `id` (int), `eventId` (String), `correlationId` (String), `anonymizedUserId` (String), `fileName` (String), `lineNumber` (int?), `errorMessage` (String), `originalTopic` (String), `status` (String), `timestamp` (DateTime), `resolvedAt` (DateTime?).
  - [x] 5.2 Criar model `AdminDlqMessageModel` em `frontend/lib/src/features/admin/data/models/admin_dlq_message_model.dart`:
    - Construtor com `fromJson` e `toJson`, suportando envelope `{ "data": ... }` ou lista direta.
  - [x] 5.3 Atualizar `AdminRemoteDatasource` em `frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart`:
    - `Future<List<AdminDlqMessageModel>> getDlqFailures({String status = 'PENDING'});` (GET `/api/v1/admin/dlq?status=$status`)
    - `Future<void> reprocessDlqMessage(int id);` (POST `/api/v1/admin/dlq/$id/reprocess`)
    - `Future<Map<String, dynamic>> reprocessBatchDlqMessages(List<int> ids);` (POST `/api/v1/admin/dlq/reprocess-batch`)
    - Tratar status `403` lançando `AdminAccessDeniedException` já existente no datasource.
  - [x] 5.4 Atualizar `AdminRepository` (`frontend/lib/src/features/admin/domain/repositories/admin_repository.dart`) e `AdminRepositoryImpl` (`frontend/lib/src/features/admin/data/repositories/admin_repository_impl.dart`).

- [x] **Task 6: Implementar `AdminDlqProvider` com Gestão de Seleção e Proteção de Dispose** (AC: #1, #2, #3, #4, #5)
  - [x] 6.1 Criar `AdminDlqProvider` em `frontend/lib/src/features/admin/presentation/providers/admin_dlq_provider.dart`:
    - `enum AdminDlqState { initial, loading, loaded, reprocessing, accessDenied, error }`
    - Campos: `status`, `failures` (List<AdminDlqMessage>), `selectedIds` (Set<int>), `reprocessingItemIds` (Set<int>), `errorMessage`, `_disposed` (bool).
    - Getters: `isAllSelected`, `selectedCount`, `hasSelection`.
    - Métodos:
      - `Future<void> loadFailures()`: busca falhas da API e atualiza a lista com `status = AdminDlqState.loaded`.
      - `void toggleSelection(int id)` / `void selectAll()` / `void clearSelection()`.
      - `Future<bool> reprocessItem(int id)`:
        - Adiciona `id` em `reprocessingItemIds`, notifica listeners;
        - Chama datasource/repo; se sucesso, remove da lista local de `failures` e de `selectedIds`;
        - Remove `id` de `reprocessingItemIds` em bloco `finally`;
        - Guard: `if (_disposed) return; notifyListeners();`.
      - `Future<bool> reprocessSelected()`:
        - Adiciona todos os `selectedIds` a `reprocessingItemIds`, chama `reprocessBatchDlqMessages(selectedIds.toList())`;
        - Remove os itens bem-sucedidos da lista local de `failures`, limpa a seleção e atualiza o estado.
      - Sobrescrever `dispose()`: marcar `_disposed = true; super.dispose();`.

- [x] **Task 7: Implementar a Tela `AdminDlqPage` e Integrar no Dashboard e Rotas** (AC: #1, #2, #3, #4, #5)
  - [x] 7.1 Criar `AdminDlqPage` em `frontend/lib/src/features/admin/presentation/pages/admin_dlq_page.dart`:
    - **Header:** Título **"Falhas de Ingestão (Dead Letter Queue)"**, botão para voltar ao dashboard (`Key('admin_dlq_back_button')`) e botão de atualização (`Key('admin_dlq_refresh_button')`).
    - **Barra de Ações em Lote:** Botão **"Reprocessar Selecionados"** (`Key('admin_dlq_reprocess_batch_button')`), habilitado apenas quando houver itens selecionados (com contador de seleção).
    - **Estados de Tela:**
      - **Skeleton Screen (`Key('admin_dlq_skeleton')`):** Exibido durante `AdminDlqState.loading`.
      - **Acesso Negado (`Key('admin_dlq_access_denied_state')`):** Exibido em caso de `403 Forbidden` com botão para voltar ao dashboard.
      - **Estado Vazio (`Key('admin_dlq_empty_state')`):** Exibido quando `failures.isEmpty` ("Tudo limpo! Nenhuma falha de ingestão pendente na DLQ.").
      - **Tabela / Cards de Falhas:**
        - Checkbox mestre (`Key('admin_dlq_select_all_checkbox')`);
        - Checkbox de linha (`Key('admin_dlq_item_checkbox_${item.id}')`);
        - Coluna **Arquivo:** Ícone de planilha + `fileName`;
        - Coluna **Usuário:** ID anonimizado (`usr_***xxxx`);
        - Coluna **Erro Reportado:** Badge/Texto com destaque em vermelho/âmbar do erro;
        - Coluna **Linha:** Número da linha da planilha (`#${lineNumber}`);
        - Coluna **Timestamp:** Data formatada em UTC/Local;
        - Coluna **Ação:** Botão **"Reprocessar"** (`Key('admin_dlq_reprocess_button_${item.id}')`) com `CircularProgressIndicator` caso `reprocessingItemIds.contains(item.id)`.
    - **Feedbacks:** SnackBar verde (`AppTheme.primaryColor`) no sucesso e SnackBar vermelha no erro.
  - [x] 7.2 Integrar navegação em `frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart`:
    - Adicionar botão/atalho na barra superior do `AdminDashboardPage` ao lado de `admin_refresh_button`:
      ```dart
      ElevatedButton.icon(
        key: const Key('admin_nav_dlq_button'),
        onPressed: () => context.go('/admin/dlq'),
        icon: const Icon(Icons.sync_problem_outlined),
        label: const Text('Falhas de Ingestão (DLQ)'),
      )
      ```
  - [x] 7.3 Atualizar `frontend/lib/main.dart`:
    - Registrar `AdminDlqProvider` no `MultiProvider` injetando `adminRepository`.
    - Adicionar rota `GoRoute(path: '/admin/dlq', builder: (context, state) => const AdminDlqPage())` dentro do `ShellRoute`.

---

### Testes Automatizados e Validação Final

- [x] **Task 8: Testes Automatizados de Backend (Unitários e de Integração)** (AC: #1, #2, #3, #4, #5, #6)
  - [x] 8.1 Criar testes unitários em `backend/api/src/test/java/afsdigital/grahamselect/api/dlq/`:
    - `KafkaTradeExtractionDlqConsumerTest`: valida desserialização de `TradeExtractionFailedEvent` e persistência idempotente no repositório.
    - `DlqManagementServiceTest`:
      - Valida listagem com ordenação decrescente e anonimização SHA-256 (`usr_***xxxx`).
      - Valida reprocessamento com sucesso (reconstrução do evento, geração de UUID válido para `eventId`, publicação no Kafka e transição para `RESOLVED`).
      - Valida reprocessamento com erro (transição para `FAILED` e preservação do motivo).
      - Valida reprocessamento em lote com agregação correta de contadores.
  - [x] 8.2 Criar teste de integração `AdminDlqControllerIT` em `backend/api/src/test/java/afsdigital/grahamselect/api/dlq/web/AdminDlqControllerIT.java` (estendendo `BaseRepositoryIT` com `@AutoConfigureMockMvc`):
    - Configurar `@MockBean private KafkaTemplate<String, Object> kafkaTemplate;` para evitar tentativa de conexão com broker durante testes com `auto-startup: false`.
    - `shouldReturn200WithDlqFailuresWhenAdmin`
    - `shouldReprocessDlqMessageSuccessfullyWhenAdmin`
    - `shouldReprocessBatchDlqMessagesSuccessfullyWhenAdmin`
    - `shouldReturn403ForbiddenWhenRegularUser`
    - `shouldReturn401UnauthorizedWhenUnauthenticated`
    - `shouldReturn404NotFoundWhenDlqIdDoesNotExist`

- [x] **Task 9: Testes Automatizados de Frontend (Unitários e de Widget)** (AC: #1, #2, #3, #4, #5, #6)
  - [x] 9.1 Criar testes unitários em `frontend/test/features/admin/`:
    - `admin_dlq_message_model_test.dart`: valida deserialização JSON de objetos e envelopes.
    - `admin_dlq_provider_test.dart`: valida ciclo `initial -> loading -> loaded`, seleção múltipla, reprocessamento individual e em lote, tratamento de `accessDenied`, tratamento de erro e guard pós-dispose.
  - [x] 9.2 Criar testes de widget em `frontend/test/features/admin/presentation/pages/admin_dlq_page_test.dart`:
    - Valida exibição do Skeleton Screen durante carregamento (`Key('admin_dlq_skeleton')`).
    - Valida listagem de itens da DLQ com Arquivo, Erro Reportado e Timestamp.
    - Valida disparo de reprocessamento ao clicar no botão "Reprocessar" e remoção do item da tela.
    - Valida seleção múltipla e disparo do reprocessamento em lote.
    - Valida exibição do estado de "Acesso Restrito a Administradores" para usuários sem perfil `ADMIN` (`Key('admin_dlq_access_denied_state')`).
    - Valida exibição do estado vazio quando não há mensagens na DLQ (`Key('admin_dlq_empty_state')`).

- [x] **Task 10: Validação de Qualidade e Integridade (`AGENTS.md`)**
  - [x] 10.1 Executar suíte de testes de backend: `mvn clean test` no diretório `backend` (utilizando JDK 21/22) garantindo 0 falhas em todos os módulos.
  - [x] 10.2 Executar análise estática e testes de frontend: `flutter analyze` e `flutter test` no diretório `frontend` garantindo 0 erros e 0 warnings.

### Review Findings

- [x] [Review][Patch] Tratamento do resultado no frontend para evitar falso positivo quando reprocess individual/lote falhar no backend [`frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart:104`, `frontend/lib/src/features/admin/presentation/providers/admin_dlq_provider.dart:82`]
- [x] [Review][Patch] Despacho no Kafka com confirmação síncrona com timeout (`.get(5, TimeUnit.SECONDS)`) prevenindo perda silenciosa de eventos [`backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java:112`]
- [x] [Review][Patch] Resiliência no parser numérico (`parseNumber`) suportando tanto padrão brasileiro `1.234,56` quanto americano `1,234.56` [`backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java:220`]
- [x] [Review][Patch] Normalização de `side` para mapear "V" -> "VENDA" e "C" -> "COMPRA" garantindo conformidade com consultas da carteira [`backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java:90`]
- [x] [Review][Patch] Prevenção de concorrência e duplo clique no frontend (`_reprocessingItemIds` guard) [`frontend/lib/src/features/admin/presentation/providers/admin_dlq_provider.dart:77`]
- [x] [Review][Patch] Preservação do histórico e mensagem de erro original da DLQ ao registrar falha de reprocessamento [`backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java:124`]
- [x] [Review][Patch] Proteção contra chave nula em `payloadMap` (`entry.getKey() != null`) [`backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java:197`]
- [x] [Review][Patch] Limite de segurança (máximo 100 itens) no reprocessamento em lote sem IDs específicos prevenindo DoS [`backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java:140`]
- [x] [Review][Patch] Sanitização do log de consumo da DLQ para não expor PII/payload bruto em nível INFO [`backend/api/src/main/java/afsdigital/grahamselect/api/dlq/infrastructure/messaging/KafkaTradeExtractionDlqConsumer.java:30`]
- [x] [Review][Patch] Logs de nível ERROR adicionados nas exceções de negócio e parsing conforme AGENTS.md [`backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java`]
- [x] [Review][Patch] Compatibilidade de tipos no deserializador de lista no Flutter com `whereType<Map>()` [`frontend/lib/src/features/admin/data/models/admin_dlq_message_model.dart:78`]

---

## Dev Notes

### 1. Contexto Arquitetural e Decisões Técnicas
- **Épico 6:** Administração & Operações (Saúde do Sistema).
- **FR27 (PRD):** *"Administradores podem disparar o reprocessamento em lote (fila manual) de arquivos que falharam no parser após a subida de um hotfix corretivo."*
- **Decisão Arquitetural D9 (Tópicos Kafka):**
  - `TopicConstants.TRADE_EXTRACTED_DLQ_TOPIC` = `"trade-extracted-dlq"` (Dead Letter Queue para falhas de extração).
  - `TopicConstants.TRADE_EXTRACTED_TOPIC` = `"trade-extracted"` (Tópico de trades extraídos).
  - `TopicConstants.B3_UPLOAD_TOPIC` = `"file-uploaded"` (Tópico de trigger de arquivo).
- **Armazenamento Persistente de Mensagens DLQ:**
  - Tópicos Kafka possuem tempo de retenção finito e são imutáveis (não suportam deleção pontual nem consultas relacionais paginadas).
  - Portanto, o consumer `KafkaTradeExtractionDlqConsumer` persiste cada falha na tabela `dlq_messages`.
  - A remoção da lista de falhas ativas exigida pelo AC ocorre através da transição do status para `RESOLVED` (com filtro `status = 'PENDING'` nas consultas padrão da UI).
- **Consumo e Idempotência:**
  - O consumer `KafkaTradeExtractionDlqConsumer` verifica `dlqMessageRepository.findByEventId(event.eventId())`. Se já existir, ignora o evento para garantir idempotência contra re-entregas do Kafka.
- **UUID Obrigatório para `eventId` no Reprocessamento:**
  - No backend, `KafkaTradeExtractedConsumer` consome de `trade-extracted` e executa:
    `Trade.builder().id(UUID.fromString(event.eventId()))`.
  - Portanto, ao reconstruir `TradeExtractedEvent` em `DlqManagementService`, é **mandatório** gerar `UUID.randomUUID().toString()`. Se uma string não-UUID for enviada, o consumer quebrará com `IllegalArgumentException`.

### 2. Análise de Arquivos Existentes que Serão Modificados (UPDATE Files Analysis)

1. **`backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`**
   - *Estado atual:* Possui changelogs `01` a `25-add-role-to-users.yaml`.
   - *O que muda:* Adicionar no final a linha `- include: { file: db/changelog/26-create-dlq-messages-table.yaml }`.
   - *O que preservar:* Ordem estrita e integridade dos changelogs anteriores.

2. **`backend/api/src/main/resources/openapi.yaml`**
   - *Estado atual:* Possui endpoints de observabilidade admin (`/api/v1/admin/observability`) e schemas de perfil de usuário.
   - *O que muda:* Adicionar endpoints `/api/v1/admin/dlq`, `/api/v1/admin/dlq/{id}/reprocess` e `/api/v1/admin/dlq/reprocess-batch`.
   - *O que preservar:* Todos os schemas e paths existentes.

3. **`frontend/lib/src/features/admin/domain/repositories/admin_repository.dart` & `admin_repository_impl.dart`**
   - *Estado atual:* Contém apenas métodos de observabilidade (`getObservabilityMetrics`).
   - *O que muda:* Adição dos métodos `getDlqFailures`, `reprocessDlqMessage` e `reprocessBatchDlqMessages`.
   - *O que preservar:* Assinaturas e comportamento de observabilidade intactos.

4. **`frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart`**
   - *Estado atual:* Chama `GET /api/v1/admin/observability`.
   - *O que muda:* Implementação das chamadas HTTP para `/api/v1/admin/dlq/**` com tratamento de erro e `AdminAccessDeniedException`.
   - *O que preservar:* Método `fetchObservabilityMetrics` e injeção do `ApiClient`.

5. **`frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart`**
   - *Estado atual:* Exibe cards de usuários ativos, uploads, latência e tabela de uploads anonimizados.
   - *O que muda:* Inclusão de botão de navegação rápida para o painel de DLQ (`Key('admin_nav_dlq_button')`).
   - *O que preservar:* Todos os cards, skeleton e comportamento responsivo existentes.

6. **`frontend/lib/main.dart`**
   - *Estado atual:* Configura rotas no `ShellRoute` e providers no `MultiProvider`.
   - *O que muda:* Registro do `AdminDlqProvider` e da rota `/admin/dlq`.
   - *O que preservar:* Todas as outras rotas, providers e estrutura do `ShellRoute`.

---

### 3. Guardrails Mandatórios para o Desenvolvedor (`AGENTS.md` & `project-context.md`)

- **Branch de Desenvolvimento:** Sempre abrir branch atualizada antes de implementar:  
  `git checkout -b feature/6-2-failed-ingestion-dlq-management main`
- **Ambiente Java:** A compilação e execução de testes Maven requer OpenJDK 21 ou 22 (devido ao flag de compilação `-proc:full`). Configurar `JAVA_HOME=/Users/alexandrofs/Library/Java/JavaVirtualMachines/openjdk-22.0.1/Contents/Home` se necessário.
- **Datas e Horários em UTC:** Obrigatório usar `ZoneOffset.UTC` para todas as datas e horários tanto em Java (`OffsetDateTime.now(ZoneOffset.UTC)`) quanto em Dart (`DateTime.toUtc()`).
- **Valores Monetários e Numéricos:** Utilizar `BigDecimal` com escala controlada e `RoundingMode.HALF_UP`.
- **Logs Estruturados:** Registrar `log.info` em todas as entradas de endpoints e no consumo do tópico `trade-extracted-dlq`. Registrar `log.error` com stack trace em capturas de exceção.
- **Testes de Integração Backend:** `BaseRepositoryIT` desabilita auto-startup do listener Kafka (`spring.kafka.listener.auto-startup: false`). Em `AdminDlqControllerIT`, utilizar `@MockBean private KafkaTemplate<String, Object> kafkaTemplate;` para isolar a camada HTTP contra chamadas de rede Kafka não mockadas.
- **Flutter Theme e Deprecações:** Utilizar `AppTheme.surfaceColor`, `AppTheme.primaryColor` e `.withValues(alpha: ...)`. Não utilizar `.withOpacity(...)`.
- **Proteção Pós-Dispose no Flutter:** No `AdminDlqProvider`, assegurar verificação `if (_disposed) return;` antes de disparar `notifyListeners()` após conclusões assíncronas de chamadas HTTP.
- **Validação de Código:** Rodar `mvn clean test` no backend e `flutter analyze` + `flutter test` no frontend antes de submeter a história para code review.

---

### 4. Estrutura Completa de Arquivos — Novos e Modificados

```text
NOVOS ARQUIVOS (BACKEND):
backend/common/src/main/resources/db/changelog/26-create-dlq-messages-table.yaml
backend/common/src/main/java/afsdigital/grahamselect/common/dlq/domain/entities/DlqMessageStatus.java
backend/common/src/main/java/afsdigital/grahamselect/common/dlq/infrastructure/persistence/jpa/entities/DlqMessageEntity.java
backend/common/src/main/java/afsdigital/grahamselect/common/dlq/infrastructure/persistence/jpa/repositories/DlqMessageRepository.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/util/AdminAnonymizationUtils.java
backend/api/src/main/java/afsdigital/grahamselect/api/dlq/infrastructure/messaging/KafkaTradeExtractionDlqConsumer.java
backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/DlqMessageDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/DlqReprocessResultDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/DlqBatchReprocessRequest.java
backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/DlqBatchReprocessResultDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java
backend/api/src/main/java/afsdigital/grahamselect/api/dlq/web/AdminDlqController.java
backend/api/src/test/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementServiceTest.java
backend/api/src/test/java/afsdigital/grahamselect/api/dlq/infrastructure/messaging/KafkaTradeExtractionDlqConsumerTest.java
backend/api/src/test/java/afsdigital/grahamselect/api/dlq/web/AdminDlqControllerIT.java

MODIFICADOS (BACKEND):
backend/common/src/main/resources/db/changelog/db.changelog-master.yaml
backend/api/src/main/resources/openapi.yaml
backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminObservabilityService.java (para reuso do utilitário)

NOVOS ARQUIVOS (FRONTEND):
frontend/lib/src/features/admin/domain/entities/admin_dlq_message.dart
frontend/lib/src/features/admin/data/models/admin_dlq_message_model.dart
frontend/lib/src/features/admin/presentation/providers/admin_dlq_provider.dart
frontend/lib/src/features/admin/presentation/pages/admin_dlq_page.dart
frontend/test/features/admin/data/models/admin_dlq_message_model_test.dart
frontend/test/features/admin/presentation/providers/admin_dlq_provider_test.dart
frontend/test/features/admin/presentation/pages/admin_dlq_page_test.dart

MODIFICADOS (FRONTEND):
frontend/lib/src/features/admin/domain/repositories/admin_repository.dart
frontend/lib/src/features/admin/data/repositories/admin_repository_impl.dart
frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart
frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart
frontend/lib/main.dart
```

---

### Project Structure Notes

- Segue estritamente a Clean Architecture do projeto:
  - Backend: módulos `common` (domínio, entidades JPA e Liquibase) e `api` (web controllers, application services, messaging consumers/producers).
  - Frontend: `lib/src/features/admin/` organizado em `domain/`, `data/` e `presentation/`.
- Nenhum pacote externo novo é adicionado ao `pubspec.yaml` nem ao `pom.xml`.

---

### References

- [Source: `docs/bmad/planning-artifacts/epics.md` — Epic 6: Administração & Operações, Story 6.2: Reprocessamento de Importações Falhas (DLQ UI)]
- [Source: `docs/bmad/planning-artifacts/prd.md` — FR27, NFR3, NFR5]
- [Source: `docs/bmad/planning-artifacts/architecture.md` — Decisão D5 (RBAC), Decisão D9 (Tópicos Kafka e DLQ), Decisão D7 (API REST Envelope), Decisão D8 (RFC 7807), Decisão D12 (Estrutura Flutter)]
- [Source: `docs/bmad/planning-artifacts/ux-design-specification.md` — Paleta de Cores, Skeleton Screens e Feedback via SnackBar]
- [Source: `docs/bmad/project-context.md` — Regras de BigDecimal, ZoneOffset.UTC, camelCase, Provider e Testes]
- [Source: `AGENTS.md` — Regras de Branch, Validação Obrigatória e Logs Estruturados]
- [Source: `docs/bmad/implementation-artifacts/6-1-admin-observability-dashboard.md` — Padrões estabelecidos de RBAC e anonimização LGPD]

---

## Dev Agent Record

### Agent Model Used

Gemini 3.8 Flash (High)

### Debug Log References

### Completion Notes List

- Context engine quality review and validation completed (modo de validação bmad-create-story).
- Adicionada especificação estrita para geração de `UUID.randomUUID().toString()` no `eventId` do `TradeExtractedEvent` para prevenir `IllegalArgumentException` no consumer downstream `KafkaTradeExtractedConsumer`.
- Inserido utilitário de anonimização `AdminAnonymizationUtils` para evitar duplicidade de código com `AdminObservabilityService` mantendo LGPD `usr_***xxxx`.
- Adicionado bloco explícito de `rollback` (`dropTable`) no changelog Liquibase `26-create-dlq-messages-table.yaml` e compatibilidade cruzada MySQL/H2 (`CLOB`/`TEXT`).
- Incluídos guardrails para `AdminDlqControllerIT` (`@MockBean KafkaTemplate`) e compatibilidade do JDK 21/22 com Maven.
- Detalhados todos os Keys de UI para testes de widget, proteção pós-dispose no `AdminDlqProvider`, e botões de navegação bidirecional entre `/admin/dashboard` e `/admin/dlq`.
- Implementação completa do backend concluída:
  - Liquibase changelog 26 e entidade `DlqMessageEntity` com `DlqMessageRepository` no módulo `common`.
  - Consumer Kafka `KafkaTradeExtractionDlqConsumer` para tópico `trade-extracted-dlq` com garantia de idempotência e auditoria de erros.
  - Serviço `DlqManagementService` com parser resiliente para datas (`dd/MM/yyyy` e ISO `yyyy-MM-dd`) e números (tratamento de moeda `R$`, vírgulas e pontos), reconstrução de evento com UUID válido e reprocessamento individual e em lote.
  - Controller `AdminDlqController` protegido por `@RequireAdmin`, registro de logs de entrada e nível ERROR em exceções, envelope REST `{ "data": ... }` e RFC 7807 para 401, 403 e 404 (`DlqMessageNotFoundException`).
  - Atualização do contrato `openapi.yaml`.
- Implementação completa do frontend concluída:
  - Entidade `AdminDlqMessage` e Model `AdminDlqMessageModel` com suporte a datas em UTC.
  - Métodos adicionados no `AdminRemoteDatasource`, `AdminRepository` e `AdminRepositoryImpl` para comunicação HTTP com `/api/v1/admin/dlq/**`.
  - State management com `AdminDlqProvider` cobrindo seleção individual, seleção total, reprocessamento individual e em lote com feedback e proteção pós-dispose.
  - Tela `AdminDlqPage` responsiva com skeleton loading, empty state, access denied amigável e botões de reprocessamento com indicador de loading.
  - Integração do botão de atalho no `AdminDashboardPage` e rotas no `main.dart`.
- Qualidade e testes 100% aprovados:
  - Backend: todos os 18 testes de backend passaram via `mvn test` (incluindo testes unitários de consumer, serviço e testes de integração de controller).
  - Frontend: todos os 186 testes passaram via `flutter test` e `flutter analyze` finalizou com zero erros e zero warnings.

### File List

- `backend/common/src/main/resources/db/changelog/26-create-dlq-messages-table.yaml` (new)
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (modified)
- `backend/common/src/main/java/afsdigital/grahamselect/common/dlq/domain/entities/DlqMessageStatus.java` (new)
- `backend/common/src/main/java/afsdigital/grahamselect/common/dlq/infrastructure/persistence/jpa/entities/DlqMessageEntity.java` (new)
- `backend/common/src/main/java/afsdigital/grahamselect/common/dlq/infrastructure/persistence/jpa/repositories/DlqMessageRepository.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/util/AdminAnonymizationUtils.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminObservabilityService.java` (modified)
- `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/infrastructure/messaging/KafkaTradeExtractionDlqConsumer.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/DlqMessageDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/DlqReprocessResultDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/DlqBatchReprocessRequest.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/dto/DlqBatchReprocessResultDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/exceptions/DlqMessageNotFoundException.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementService.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/dlq/web/AdminDlqController.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java` (modified)
- `backend/api/src/main/resources/openapi.yaml` (modified)
- `backend/api/pom.xml` (modified)
- `backend/api/src/test/java/afsdigital/grahamselect/api/dlq/infrastructure/messaging/KafkaTradeExtractionDlqConsumerTest.java` (new)
- `backend/api/src/test/java/afsdigital/grahamselect/api/dlq/application/service/DlqManagementServiceTest.java` (new)
- `backend/api/src/test/java/afsdigital/grahamselect/api/dlq/web/AdminDlqControllerIT.java` (new)
- `frontend/lib/src/features/admin/domain/entities/admin_dlq_message.dart` (new)
- `frontend/lib/src/features/admin/data/models/admin_dlq_message_model.dart` (new)
- `frontend/lib/src/features/admin/domain/repositories/admin_repository.dart` (modified)
- `frontend/lib/src/features/admin/data/repositories/admin_repository_impl.dart` (modified)
- `frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart` (modified)
- `frontend/lib/src/features/admin/presentation/providers/admin_dlq_provider.dart` (new)
- `frontend/lib/src/features/admin/presentation/pages/admin_dlq_page.dart` (new)
- `frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart` (modified)
- `frontend/lib/main.dart` (modified)
- `frontend/test/features/admin/data/models/admin_dlq_message_model_test.dart` (new)
- `frontend/test/features/admin/presentation/providers/admin_dlq_provider_test.dart` (new)
- `frontend/test/features/admin/presentation/pages/admin_dlq_page_test.dart` (new)
- `frontend/test/features/admin/presentation/providers/admin_observability_provider_test.dart` (modified)
- `frontend/test/features/admin/presentation/pages/admin_dashboard_page_test.dart` (modified)
- `docs/bmad/implementation-artifacts/sprint-status.yaml` (modified)
- `docs/bmad/implementation-artifacts/6-2-failed-ingestion-dlq-management.md` (modified)
