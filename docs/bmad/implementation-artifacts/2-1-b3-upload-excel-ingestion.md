# Story 2.1: Upload de Relatório de Negociação B3 (Excel)

Status: ready-for-dev
Epic: 2 (Ingestão de Dados)

## Story

As a investidor pessoa física,
I want fazer o upload da minha planilha de negociação da B3 (.xlsx),
So that eu não precise cadastrar centenas de operações manualmente e tenha minha carteira consolidada rapidamente.

## Acceptance Criteria

### Funcional (BDD)

- **Scenario: Upload de arquivo válido com sucesso**
    - **Given** que o usuário está autenticado e na tela de "Importação"
    - **When** ele arrasta um arquivo `.xlsx` válido (formato padrão B3) para a zona de drop
    - **Then** o sistema valida que o arquivo possui os cabeçalhos obrigatórios (Ticker, Data, Quantidade, Preço)
    - **And** salva o arquivo bruto no storage (Simulado via volume Docker para MVP)
    - **And** emite o evento `file-uploaded` no tópico Kafka correspondente
    - **And** exibe feedback visual de "Processando..." com animação progressiva (UX)

- **Scenario: Upload de arquivo com formato inválido**
    - **Given** que o usuário tenta subir um arquivo `.pdf` ou `.csv`
    - **When** o sistema detecta a extensão incorreta
    - **Then** exibe mensagem de erro RFC 7807 (400 Bad Request)
    - **And** informa: "Apenas arquivos .xlsx (Excel) da B3 são suportados no momento."

- **Scenario: Upload de arquivo sem cabeçalhos obrigatórios**
    - **Given** um arquivo `.xlsx` sem a coluna "Ticker" ou "Quantidade"
    - **When** o parser inicial valida o cabeçalho
    - **Then** interrompe o upload e exibe alerta: "Arquivo inválido: Colunas obrigatórias não encontradas."

### Técnico & Qualidade (Guardrails)

- **UTC Mandatory:** A data de recebimento do arquivo e qualquer timestamp gerado deve obrigatoriamente usar `ZoneOffset.UTC`.
- **Kafka Integration:** Deve incluir um `correlationId` no payload do evento `file-uploaded` para rastreamento E2E.
- **Multi-tenancy:** O evento Kafka DEVE conter o `userId` extraído do contexto de segurança (JWT).
- **Performance:** O upload e validação de cabeçalho inicial deve responder em `< 2 segundos` antes de delegar para o worker assíncrono.

## Developer Context

### Architecture Compliance
- **Backend:** Implementar no módulo `api` (package `upload`).
- **Clean Architecture:** Criar `UploadB3FileUseCase` como POJO no módulo `common`.
- **Parser:** Usar **FastExcel** para leitura de baixo consumo de memória (streaming).
- **Storage:** Para o MVP, salvar no diretório configurado no `application.yaml` (storage local).

### File Structure Requirements
- **Backend:**
    - `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3UploadController.java`
    - `backend/common/src/main/java/afsdigital/grahamselect/common/upload/application/usecase/UploadB3FileUseCase.java`
- **Frontend:**
    - `frontend/lib/src/features/upload/presentation/widgets/b3_upload_zone.dart`
    - `frontend/lib/src/features/upload/presentation/providers/upload_provider.dart`

### Testing Requirements
- **Unit:** Testar `UploadB3FileUseCase` com mocks de Kafka e Storage.
- **Integration:** Testar o endpoint REST via `MockMvc` validando o payload do Kafka (use `EmbeddedKafka` ou Testcontainers).
- **E2E:** Criar cenário de teste no Flutter (`integration_test`) para simular o drag-and-drop e a mensagem de sucesso.

### Previous Story Intelligence (Epic 1)
- **UTC Fix:** Lembre-se que em 1.1 corrigimos o bug de fuso horário. Use sempre `LocalDateTime.now(ZoneOffset.UTC)`.
- **JWT Auth:** O `userId` deve ser extraído via `JwtAuthenticationToken` conforme implementado em 1.1.

## Completed Status
- **Status:** ready-for-dev
- **Note:** Ultimate context engine analysis completed - comprehensive developer guide created incorporating strategic test design (TEA).
