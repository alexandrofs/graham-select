# Story 2.3: Deduplicação de Operações

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a investidor,
I want que o sistema identifique e ignore operações que eu já importei anteriormente,
So that meu saldo e preço médio não fiquem duplicados e incorretos.

## Acceptance Criteria

1. **Scenario: Deduplicação de operações via arquivo B3**
   - **Given** uma operação extraída do Excel via Kafka
   - **When** o sistema verifica se já existe uma operação idêntica cadastrada (mesmo `Ticker`, `Data`, `Quantidade`, `Preço` e `Corretora` para o `userId`)
   - **Then** ignora a duplicata no banco de dados e registra apenas as novas operações
   - **And** inclui o total de "novas operações" vs "duplicadas" no painel final de processamento

2. **Scenario: Feedback de Duplicadas**
   - **Given** o fim do processamento do arquivo
   - **When** existem itens duplicados
   - **Then** informa ao usuário (ex: "10 novas operações inseridas, 5 operações ignoradas por duplicidade").

## Tasks / Subtasks

- [x] Task 1: Adicionar restrições de unicidade ou lógica de verificação
  - [x] Subtask 1.1: Criar migration Liquibase adicionando indexação para busca rápida ou `UNIQUE` constraint usando (`user_id`, `ticker`, `trade_date`, `quantity`, `price`).
- [x] Task 2: Lógica de Deduplicação no Caso de Uso
  - [x] Subtask 2.1: Modificar o `SaveTradeUseCase` para verificar duplicatas antes da inserção.
  - [x] Subtask 2.2: Ajustar Consumer do `trade-extracted` para contabilizar duplicadas vs sucesso.
- [x] Task 3: Informar Total de Novas vs Duplicadas
  - [x] Subtask 3.1: Emitir evento ou atualizar status da importação contendo contagem.
- [x] Task 4: UI/Frontend
  - [x] Subtask 4.1: Mostrar no dashboard/histórico o sumário da importação (X novas, Y ignoradas).

## Dev Notes

### Architecture Compliance
- **Isolamento de Dados:** Filtre SEMPRE por `user_id` na verificação de duplicidade (NFR4).
- **Tratamento Assíncrono:** As validações ocorrem em background consumindo do Kafka. Evite longos bloqueios de IO usando queries otimizadas (`EXISTS`).
- **Idempotência:** A deduplicação é a base da idempotência do sistema.

### Library / Framework Requirements
- **Java 21:** Use `Records` para DTOs.
- **Kafka:** O consumer deve tratar exceções de unicidade caso ocorra race condition e commitar o offset para não travar a partição.
- **Liquibase:** Alterações de schema no `db.changelog`.

### Previous Story Intelligence
- No arquivo 2.1 e 1.1, foi fixado o bug de fuso horário. Use sempre `ZoneOffset.UTC` para a `Data` da operação.
- O payload de Kafka contém o `correlationId` para rastreio e `userId`.

### Git Intelligence
- O commit recente focou em upload UI e Produtor Kafka (`B3UploadZone`, `UploadB3FileUseCase`). A deduplicação atua como o core logic do Kafka *Consumer* que processa `trade-extracted`.

## Dev Agent Record

### Agent Model Used
Gemini 2.0 Flash

### File List
- `backend/common/src/main/resources/db/changelog/13-create-trades-table.yaml`
- `backend/common/src/main/resources/db/changelog/14-add-duplicated-rows-to-import-status.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/domain/entities/Trade.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/repository/TradePort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/SaveTradeUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/SaveTradeUseCaseTest.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/model/B3ImportStatus.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/repository/B3ImportStatusPort.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/entities/TradeEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/repositories/JpaTradeRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/TradeJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaTradeExtractedConsumer.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/persistence/B3ImportStatusRepositoryAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/persistence/jpa/entities/B3ImportStatusEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3ImportStatusView.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3UploadController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/config/PortfolioConfig.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/TradeDeduplicationIT.java`
- `frontend/lib/src/features/upload/domain/entities/upload_processing_status.dart`
- `frontend/lib/src/features/upload/data/models/upload_status_response_model.dart`
- `frontend/lib/src/features/upload/presentation/pages/upload_page.dart`
