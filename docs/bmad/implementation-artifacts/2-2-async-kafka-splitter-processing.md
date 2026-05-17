# Story 2.2: Processamento Assíncrono e Splitter (Worker)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a sistema,
I want processar o arquivo Excel em segundo plano, fragmentando-o em operações individuais,
So that grandes volumes de dados não bloqueiem a interface e sejam tolerantes a falhas.

## Acceptance Criteria

1. **Scenario: Processamento e Fragmentação de Arquivo (Splitter Pattern)**
   - **Given** um evento `file-uploaded` recebido do Kafka
   - **When** o consumer processa o evento e lê o arquivo do storage (usando streaming via FastExcel)
   - **Then** fragmenta o arquivo em eventos `trade-extracted` individuais (um evento para cada linha de operação)
   - **And** publica os eventos no tópico `trade-extracted`
   - **And** atualiza o status geral da importação no banco de dados.

2. **Scenario: Tratamento de Falhas (DLQ)**
   - **Given** uma linha corrompida ou inválida no arquivo Excel
   - **When** o parser tenta extrair os dados da linha
   - **Then** o erro é capturado de forma isolada
   - **And** envia um evento para o tópico `trade-extracted-dlq` contendo o payload original e o motivo do erro
   - **And** o processamento continua para as próximas linhas do arquivo sem interrupção.

## Tasks / Subtasks

- [x] Task 1: Domain & Application (Módulo `common`)
  - [x] Subtask 1.1: Criar classe de evento `TradeExtractedEvent` (e `TradeExtractionFailedEvent` se necessário).
  - [x] Subtask 1.2: Criar port `TradeExtractionEventPort` para o envio dos eventos de extração.
  - [x] Subtask 1.3: Criar UseCase `ProcessB3FileUseCase` responsável pela lógica de splitter do arquivo.
- [x] Task 2: Infraestrutura Kafka (Módulo `api`)
  - [x] Subtask 2.1: Implementar o consumer `KafkaB3FileUploadedConsumer` para escutar o tópico `file-uploaded`.
  - [x] Subtask 2.2: Implementar o producer para o tópico `trade-extracted` e `trade-extracted-dlq`.
- [x] Task 3: Parsing do Excel (Módulo `api`)
  - [x] Subtask 3.1: Utilizar FastExcel para ler o arquivo recebido via streaming, parsear as linhas e invocar a porta de publicação do evento por linha.
- [x] Task 4: UI/Frontend
  - [x] Subtask 4.1: Mostrar o progresso do upload/status de processamento na interface (polling ou websocket mock se o websocket não estiver na arquitetura ainda - MVP sugere polling).

## Dev Notes

### Architecture Compliance
- **Isolamento e Tolerância:** O parser deve usar Streaming API (FastExcel) para evitar `OutOfMemoryError` (OOM) e atender ao NFR1 (processamento em < 5 min para arquivos grandes) e NFR7 (ingestão elástica).
- **Módulos:** O consumer do evento `file-uploaded` e o splitter devem ficar no módulo `api` (o qual tem acesso ao storage file local). A lógica abstrata de orquestração deve estar no `common` via UseCases.
- **Kafka Topics:**
  - Consome: `file-uploaded`
  - Produz: `trade-extracted`, `trade-extracted-dlq`
- **Data e Fuso Horário:** Lembre-se da regra de utilizar estritamente `ZoneOffset.UTC` para o parseamento de datas do Excel.
- **Isolamento de Dados (LGPD):** O evento propagado para o tópico `trade-extracted` DEVE carregar o `userId` em todas as mensagens individuais. Nenhuma informação pessoal direta além do ID deve ser passada.

### Library / Framework Requirements
- **Java 21:** Priorize Records para criação dos DTOs dos Eventos.
- **FastExcel:** O uso desta biblioteca para leitura orientada a eventos (streaming) é obrigatório, conforme especificado na arquitetura.
- **Spring Kafka:** Configure o consumer com controle manual ou transacional se necessário, mas garanta que exceptions nas linhas não falhem todo o arquivo (Try/Catch interno no loop).

### Previous Story Intelligence
- Na Story 2.1, o upload inicial já está funcionando e postando o evento `file-uploaded` no Kafka com `userId` e `correlationId`. O storage local foi configurado. Você precisará recuperar o arquivo com o mesmo identificador do evento.

## Dev Agent Record

### Agent Model Used
Gemini 2.0 Flash

### Completion Notes
- Worker assíncrono implementado com consumer Kafka para `file-uploaded`, parser streaming com FastExcel e publicação por linha em `trade-extracted`.
- Proteção contra Path Traversal implementada no consumer para validar caminhos de arquivos.
- Falhas por linha agora seguem para `trade-extracted-dlq` com payload original, motivo do erro e rastreabilidade por `correlationId`.
- Idempotência no processamento garantida: retentativas do Kafka não resetam o progresso já persistido.
- Status da importação foi persistido em banco com migration Liquibase, atualização incremental de progresso e endpoint HTTP para polling autenticado.
- Upload B3 agora retorna `correlationId`, e o frontend acompanha o processamento em tempo real com polling e mensagens progressivas até concluir.
- Testes validados no `common`, compilação do backend `api` e testes focados do frontend de upload.

### File List
- `backend/api/pom.xml`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3FileUploadedConsumer.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3UploadEventPublisher.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaTradeExtractionEventPublisher.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/FastExcelB3TradeRowParser.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/persistence/B3ImportStatusRepositoryAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/persistence/jpa/entities/B3ImportStatusEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/persistence/jpa/repository/B3ImportStatusJpaRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/spring/B3ProcessingConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3ImportStatusEnvelope.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3ImportStatusView.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3UploadAcceptedResponse.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3UploadController.java`
- `backend/api/src/main/resources/application-local.yml`
- `backend/api/src/main/resources/application.yml`
- `backend/api/src/test/java/afsdigital/grahamselect/api/upload/web/B3UploadControllerIT.java`
- `backend/api/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/repository/B3ImportStatusPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/repository/TradeExtractionEventPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/usecase/ProcessB3FileUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/usecase/UploadB3FileUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/events/FileUploadedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/events/TradeExtractedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/events/TradeExtractionFailedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/model/B3ImportStatus.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/model/B3ImportStatusState.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/model/B3TradeRow.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/model/B3TradeRowFailure.java`
- `backend/common/src/main/resources/db/changelog/12-create-b3-import-status-table.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/test/java/afsdigital/grahamselect/common/upload/application/usecase/ProcessB3FileUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/upload/application/usecase/UploadB3FileUseCaseTest.java`
- `backend/common/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- `frontend/lib/main.dart`
- `frontend/lib/src/features/upload/data/datasources/upload_remote_data_source.dart`
- `frontend/lib/src/features/upload/data/models/upload_response_model.dart`
- `frontend/lib/src/features/upload/data/models/upload_status_response_model.dart`
- `frontend/lib/src/features/upload/data/repositories/upload_repository_impl.dart`
- `frontend/lib/src/features/upload/domain/entities/upload_processing_status.dart`
- `frontend/lib/src/features/upload/domain/entities/upload_result.dart`
- `frontend/lib/src/features/upload/domain/repositories/upload_repository.dart`
- `frontend/lib/src/features/upload/domain/usecases/get_b3_upload_status_usecase.dart`
- `frontend/lib/src/features/upload/presentation/pages/upload_page.dart`
- `frontend/lib/src/features/upload/presentation/providers/upload_provider.dart`
- `frontend/lib/src/features/upload/presentation/widgets/b3_upload_zone.dart`
- `frontend/test/features/upload/presentation/pages/upload_page_test.dart`
- `frontend/test/features/upload/presentation/providers/upload_provider_test.dart`

## Change Log
- 2026-05-17: Implementado worker assíncrono de split do arquivo B3 com eventos Kafka por linha, DLQ por falha isolada, persistência de status da importação e polling de progresso no frontend.
- 2026-05-17: Aplicadas correções do Code Review: validação de Path Traversal, idempotência no processamento, refatoração do parser com Enums e melhoria na serialização Kafka.
- 2026-05-17: Validada a implementação via testes automatizados.
