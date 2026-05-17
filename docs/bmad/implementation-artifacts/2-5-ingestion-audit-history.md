# Story 2.5: Ingestion Audit History

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** investidor,
**I want** ver um histórico de todos os arquivos que já importei,
**so that** eu saiba quando foi minha última atualização de dados e o status do processamento.

## Acceptance Criteria

1. **Cenário: Visualização do Histórico de Importação**
   - [x] **Given** que o usuário acessa a aba "Histórico de Importação" no Dashboard.
   - [x] **When** a tela é carregada.
   - [x] **Then** o sistema exibe uma lista (tabela) contendo:
     - Nome do Arquivo.
     - Data e Hora do Upload (formatado conforme locale do usuário).
     - Status da Importação: `PROCESSANDO`, `SUCESSO`, `PARCIAL` (quando há linhas na DLQ) ou `ERRO`.
     - Quantidade de Linhas Processadas com Sucesso.
     - Quantidade de Linhas com Erro (se houver).
   - [x] **And** permite a ordenação por Data de Upload (decrescente por padrão).

2. **Cenário: Atualização de Status via WebSocket/Polling**
   - [x] **Given** que uma importação está em andamento.
   - [x] **When** o status muda no backend (ex: de `PROCESSANDO` para `SUCESSO`).
   - [x] **Then** a interface do usuário deve refletir a mudança sem necessidade de refresh manual (NFR2/NFR8). (Nota: Implementado via Polling inteligente no Provider de 5 em 5 segundos quando há itens em processamento)

3. **Cenário: Isolamento de Dados (Multitenancy)**
   - [x] **Given** dois usuários distintos (A e B).
   - [x] **When** o usuário A consulta seu histórico.
   - [x] **Then** ele NUNCA deve ver os arquivos importados pelo usuário B (NFR4).

## Tasks / Subtasks

### Backend (Java 21 / Spring Boot 3.4.13)
- [x] **Task 1: Modelagem da Entidade IngestionAudit**
  - [x] Subtask 1.1: Criar migration Liquibase para a tabela `ingestion_audits`.
  - [x] Subtask 1.2: Criar entidade `IngestionAudit` no módulo `common`.
  - [x] Subtask 1.3: Garantir que a tabela seja imutável (apenas INSERT e UPDATE de status/counts).
- [x] **Task 2: Lógica de Negócio (Use Case)**
  - [x] Subtask 2.1: Criar `GetIngestionAuditHistoryUseCase` no módulo `common`.
  - [x] Subtask 2.2: Implementar filtragem obrigatória por `userId`.
- [x] **Task 3: Integração com o Fluxo de Ingestão Existente**
  - [x] Subtask 3.1: Atualizar o `IngestionService` para criar um registro em `ingestion_audits`.
  - [x] Subtask 3.2: Atualizar os contadores (`processed_lines`, `error_lines`) e o `status` final.
- [x] **Task 4: API Rest**
  - [x] Subtask 4.1: Criar `IngestionAuditController` no módulo `api` com o endpoint `GET /api/v1/ingestion/history`.

### Frontend (Flutter / Material 3)
- [x] **Task 5: UI de Histórico de Importação**
  - [x] Subtask 5.1: Implementar o componente `IngestionHistoryTable` utilizando `FinancialDataTable`.
  - [x] Subtask 5.2: Adicionar indicadores visuais de status.
- [x] **Task 6: Integração com Estado (Provider)**
  - [x] Subtask 6.1: Criar `IngestionHistoryProvider` e integrar com o serviço de API.
  - [x] Subtask 6.2: Implementar Skeleton screens durante o carregamento inicial.

### Review Follow-ups (AI)
- [x] **[AI-Review][CRITICAL]** Implementar Polling no `IngestionHistoryProvider` para satisfazer AC 2.
- [x] **[AI-Review][HIGH]** Adicionar `correlationId` à entidade e fluxo de auditoria para evitar race conditions.
- [x] **[AI-Review][MEDIUM]** Implementar Skeleton Screens reais em `history_page.dart`.
- [x] **[AI-Review][LOW]** Corrigir uso de `@Data` em entidades JPA.

## Dev Notes

- **Architecture Compliance:** Seguir a regra de dependência `domain` ← `application` ← `infrastructure`.
- **Security:** O `userId` deve ser extraído do JWT.
- **Polling:** Implementado polling de 5s no frontend enquanto houver itens `PROCESSANDO`.
- **Integrity:** `correlationId` usado para vincular eventos de progresso ao registro de auditoria correto.

## Dev Agent Record

### Agent Model Used

Gemini CLI (YOLO Mode)

### Debug Log References

- Migração 17 atualizada com `correlation_id`.
- `IngestionAuditControllerIT` validado após correções de review.
- `flutter analyze` passou sem erros.

### Completion Notes List

- Implementação completa com correções de review adversarial aplicadas.
- Polling automático no frontend satisfaz AC 2.
- Unicidade garantida via `correlationId`.

### File List

- `backend/common/pom.xml`
- `backend/common/src/main/resources/db/changelog/17-create-ingestion-audits-table.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/ingestion/infrastructure/persistence/jpa/entities/IngestionAudit.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/ingestion/infrastructure/persistence/jpa/repositories/IngestionAuditRepository.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/ingestion/application/usecase/GetIngestionAuditHistoryUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/ingestion/application/service/IngestionService.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/usecase/ProcessB3FileUseCase.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/spring/B3ProcessingConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/ingestion/web/IngestionAuditController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/ingestion/web/IngestionAuditControllerIT.java`
- `frontend/pubspec.yaml`
- `frontend/lib/main.dart`
- `frontend/lib/src/core/widgets/financial_data_table.dart`
- `frontend/lib/src/features/ingestion/domain/entities/ingestion_audit.dart`
- `frontend/lib/src/features/ingestion/domain/repositories/ingestion_repository.dart`
- `frontend/lib/src/features/ingestion/data/repositories/ingestion_repository_impl.dart`
- `frontend/lib/src/features/ingestion/presentation/providers/ingestion_history_provider.dart`
- `frontend/lib/src/features/ingestion/presentation/widgets/ingestion_history_table.dart`
- `frontend/lib/src/features/ingestion/presentation/pages/history_page.dart`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`
