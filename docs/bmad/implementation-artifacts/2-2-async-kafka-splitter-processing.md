# Story 2.2: Processamento Assíncrono e Splitter (Worker)

Status: ready-for-dev

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

- [ ] Task 1: Domain & Application (Módulo `common`)
  - [ ] Subtask 1.1: Criar classe de evento `TradeExtractedEvent` (e `TradeExtractionFailedEvent` se necessário).
  - [ ] Subtask 1.2: Criar port `TradeExtractionEventPort` para o envio dos eventos de extração.
  - [ ] Subtask 1.3: Criar UseCase `ProcessB3FileUseCase` responsável pela lógica de splitter do arquivo.
- [ ] Task 2: Infraestrutura Kafka (Módulo `api`)
  - [ ] Subtask 2.1: Implementar o consumer `KafkaB3FileUploadedConsumer` para escutar o tópico `file-uploaded`.
  - [ ] Subtask 2.2: Implementar o producer para o tópico `trade-extracted` e `trade-extracted-dlq`.
- [ ] Task 3: Parsing do Excel (Módulo `api`)
  - [ ] Subtask 3.1: Utilizar FastExcel para ler o arquivo recebido via streaming, parsear as linhas e invocar a porta de publicação do evento por linha.
- [ ] Task 4: UI/Frontend
  - [ ] Subtask 4.1: Mostrar o progresso do upload/status de processamento na interface (polling ou websocket mock se o websocket não estiver na arquitetura ainda - MVP sugere polling).

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

### File List
(Será preenchido pelo agente dev-story)