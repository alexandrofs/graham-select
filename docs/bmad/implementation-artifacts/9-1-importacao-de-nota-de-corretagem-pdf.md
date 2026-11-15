---
baseline_commit: 7b5b58a101c18c7573d7b411f752db9c244420e1
story_id: "9.1"
story_key: "9-1-importacao-de-nota-de-corretagem-pdf"
---

# Story 9.1: Importação de Nota de Corretagem (PDF)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como investidor pessoa física,
Eu quero fazer o upload das minhas Notas de Corretagem em formato PDF (padrão SINACOR),
Para que minhas operações de compra e venda de ativos sejam extraídas e consolidadas automaticamente no Graham-Select sem depender exclusivamente dos relatórios da área logada da B3.

## Escopo e Limites (Boundaries)

- **O que ESTÁ incluído nesta história (Story 9.1):**
  1. Adição da biblioteca Apache PDFBox 3.x (`org.apache.pdfbox:pdfbox:3.0.4`) no módulo `backend/common/pom.xml` para leitura e extração de texto estruturado de PDFs.
  2. Implementação do parser desacoplado de notas de corretagem padrão SINACOR (`SinacorPdfTradeParser`) com o método canônico `parse(InputStream, Consumer<B3TradeRow>, Consumer<B3TradeRowFailure>)`, capaz de identificar cabeçalhos (Corretora, Data do Pregão, Nr. Nota), linhas de negócios (Praça, Operação C/V mapeada para `COMPRA`/`VENDA`, Mercado, Ticker, Quantidade, Preço unitário, Valor da Operação) e normalização de tickers fracionários de ações (`PETR4F` -> `PETR4`).
  3. Suporte robusto a notas multipáginas (notas com múltiplos pregões ou operações distribuídas em 2 ou mais páginas).
  4. Endpoint REST seguro `POST /api/v1/upload/brokerage-note` no módulo `api` (e/ou suporte dual em `POST /api/v1/upload/b3`), com validação de extensão `.pdf`, tamanho máximo (15MB), leitura segura de bytes para validação de cabeçalho sem exaustão de `InputStream`, proteção contra Path Traversal, extração de `userId` via token JWT (`jwt.getSubject()`), suporte opcional a `profileId` (Family Holding) e resposta HTTP `202 Accepted` contendo `correlationId`.
  5. Integração com a infraestrutura existente de ingestão assíncrona: delegação direta para `UploadB3FileUseCase.execute(...)` para armazenamento no `B3FileStoragePort`, criação do status `RECEIVED` em `B3ImportStatusPort` e publicação do evento `FileUploadedEvent` no tópico Kafka `file-uploaded`.
  6. Roteamento inteligente no consumidor Kafka (`KafkaB3FileUploadedConsumer`): inspeciona o arquivo e aciona `SinacorPdfTradeParser` quando a extensão for `.pdf` ou `FastExcelB3TradeRowParser` quando for `.xlsx`.
  7. Observabilidade estrita no consumidor Kafka: inclusão de `@Slf4j`, registro de log em nível `INFO` ao receber mensagens do tópico e registro de exceções capturadas em nível `ERROR` com mensagem e stack trace completo (`AGENTS.md`).
  8. Conversão de cada linha extraída para `B3TradeRow` com `side` padronizado como `"COMPRA"` ou `"VENDA"`, despachando para o pipeline existente via `ProcessB3FileUseCase.handleExtractedTrade(event, tradeRow)` para deduplicação automática (`KafkaTradeExtractedConsumer`), persistência na tabela `trades` e recálculo de custódia.
  9. Atualização contínua do rastreador de status de importação (`b3_import_status`) com contagem de linhas processadas, sucessos, duplicadas e falhas com consulta via `GET /api/v1/upload/b3/status/{correlationId}`.
  10. No frontend Flutter (`UploadPage`, `UploadProvider` e `B3UploadZone`): expansão do seletor e área de arrastar-e-soltar para aceitar tanto arquivos `.xlsx` quanto `.pdf` (Notas de Corretagem SINACOR), feedback de progresso, status detalhado e tratamento de erros amigável.
- **O que NÃO está incluído (fora de escopo desta história):**
  - Web Crawler automatizado de notas ou faturas (previsto na Story 9.2).
  - Conexão direta via Open Finance (previsto na Story 9.3).
  - Reconhecimento ótico de caracteres (OCR) para PDFs escaneados como imagem/foto (esta história foca em PDFs textuais nativos gerados digitalmente pelas corretoras).
  - Alocação automática de taxas operacionais da nota (corretagem, emolumentos) nos preços médios — o parser extrai o preço unitário e quantidade de cada trade conforme o contrato existente de `TradeExtractedEvent`.

## Acceptance Criteria

1. **Upload de Nota de Corretagem SINACOR válida via REST (`FR9`, `NFR1`):**
   - **Dado** que o usuário está autenticado com token JWT válido
   - **Quando** envia uma requisição `POST /api/v1/upload/brokerage-note` (multipart/form-data) contendo um arquivo `.pdf` válido de nota de corretagem no formato SINACOR
   - **Então** o sistema valida que o arquivo possui extensão `.pdf`, não está vazio e não ultrapassa 15MB
   - **E** valida que os primeiros bytes/página contêm marcadores essenciais de nota SINACOR (`"NOTA DE CORRETAGEM"` ou `"NOTA DE NEGOCIAÇÃO"`, acompanhado de `"Data pregão"` ou `"Nr. nota"`), utilizando streams independentes para preservar o conteúdo do arquivo
   - **E** delega para `UploadB3FileUseCase.execute(...)`, que salva o arquivo com sanitização de nome contra Path Traversal, cria o status `RECEIVED` em `b3_import_status` e publica `FileUploadedEvent` no tópico `file-uploaded`
   - **E** responde com status HTTP `202 Accepted` contendo JSON com mensagem, `correlationId` gerado (UUID) e status `RECEIVED`.

2. **Validação e Rejeição de Arquivos Inválidos ou Maliciosos (`RFC 7807`):**
   - **Dado** que o usuário tenta enviar um arquivo com extensão diferente de `.pdf` (ou arquivo vazio) para `/api/v1/upload/brokerage-note`
   - **Quando** a API recebe a requisição
   - **Então** responde com HTTP `400 Bad Request` informando: `"Apenas arquivos .pdf de notas de corretagem (padrão SINACOR) são suportados neste endpoint."`
   - **Dado** que o usuário envia um arquivo `.pdf` que não é uma nota de corretagem (ex: fatura de cartão, documento em branco ou relatório genérico sem marcadores SINACOR)
   - **Quando** o validador inspeciona a primeira página
   - **Então** responde com HTTP `400 Bad Request` informando: `"Arquivo PDF inválido: Não foi possível identificar o padrão de Nota de Corretagem SINACOR."`
   - **Dado** um nome de arquivo malicioso contendo sequências de escape como `../../etc/passwd`
   - **Quando** o arquivo é recebido
   - **Então** o sistema descarta qualquer caminho relativo, normaliza o nome e impede gravação fora do diretório configurado de uploads.

3. **Extração Textual e Parsing Robusto de Negócios SINACOR:**
   - **Dado** um arquivo PDF SINACOR recebido pelo consumidor assíncrono no Kafka
   - **Quando** o `SinacorPdfTradeParser` processa o conteúdo via `parse(InputStream, Consumer<B3TradeRow>, Consumer<B3TradeRowFailure>)`
   - **Então** extrai a Data do Pregão (convertendo formato `dd/MM/yyyy` para `LocalDate` em UTC) e o nome da Corretora identificada no cabeçalho
   - **E** identifica cada linha de negociação da tabela contendo:
     - Tipo de Operação: `C` mapeado obrigatoriamente para `"COMPRA"`, `V` mapeado obrigatoriamente para `"VENDA"` (em conformidade com o enum `TradeSide`)
     - Especificação do Título / Ticker: extrai o código do ativo (ex: `PETR4`, `VALE3`, `HGLG11`, `XPML11`, `AAPL34`)
     - Tratamento Fracionário: se o ticker for de ação tradicional com sufixo 'F' (padrão `[A-Z]{4}[0-9]{1,2}F`), converte para o lote padrão removendo o 'F' (ex: `PETR4F` -> `PETR4`, `VALE3F` -> `VALE3`), sem alterar tickers que naturalmente contenham a letra F
     - Quantidade: número inteiro ou decimal positivo
     - Preço Unitário: valor monetário positivo em Reais (parseando formato brasileiro `1.234,56` para `BigDecimal`)
   - **E** converte cada linha válida em um `B3TradeRow` e despacha para `processB3FileUseCase.handleExtractedTrade(event, tradeRow)`
   - **E** se uma linha individual estiver ilegível ou corrompida, emite `handleExtractionFailure(event, failure)` sem abortar o processamento das demais linhas válidas da nota.

4. **Suporte a Notas SINACOR Multipáginas:**
   - **Dado** uma nota de corretagem com múltiplos negócios distribuídos em 2 ou mais páginas
   - **Quando** o parser executa a leitura sequencial com `PDFTextStripper`
   - **Então** varre todas as páginas do documento, mantendo a Data do Pregão e Corretora do cabeçalho da nota
   - **E** consolida todas as operações de todas as páginas no mesmo fluxo assíncrono sob o mesmo `correlationId`.

5. **Deduplicação Inteligente e Persistência na Tabela `trades` (`FR9`, Reutilização da Story 2.3):**
   - **Dado** que as linhas de negociação são emitidas como `TradeExtractedEvent` no tópico `trade-extracted`
   - **Quando** o `KafkaTradeExtractedConsumer` consome cada operação
   - **Então** delega para `SaveTradeUseCase.execute(trade)`, que verifica `tradePort.exists(...)` por `(userId, profileId, ticker, tradeDate, quantity, price, broker, side)`
   - **E** ignora operações já existentes no banco de dados (`duplicatedRows++`)
   - **E** persiste novos trades na tabela `trades` (`successfulRows++`)
   - **E** ao final da nota, atualiza o status para `COMPLETED` com o sumário de processamento.

6. **Suporte a Family Holding (Perfil Familiar / `profileId`):**
   - **Dado** um usuário assinante `PREMIUM` que selecionou um `profileId` familiar antes do upload
   - **Quando** o upload do PDF é realizado
   - **Então** o sistema valida que o perfil pertence ao `userId` autenticado via `FamilyProfilePort`
   - **E** propaga o `profileId` no `FileUploadedEvent` e em todos os `TradeExtractedEvent` gerados, associando os trades extraídos da nota diretamente à sub-carteira familiar correspondente.

7. **Interface Frontend (Upload e Drag-and-Drop Expandidos):**
   - **Dado** que o usuário acessa a tela de Importação (`/upload`) no Flutter
   - **Quando** visualiza a zona de upload (`B3UploadZone`)
   - **Então** o componente exibe instruções claras de que tanto planilhas B3 (`.xlsx`) quanto Notas de Corretagem SINACOR (`.pdf`) são aceitas
   - **E** ao clicar ou arrastar um arquivo `.pdf`, o sistema aceita o arquivo, exibe seu nome e tamanho, e habilita o botão de confirmação
   - **E** ao confirmar, o frontend direciona para o upload, exibindo o `UploadProgressWidget` e acionando o polling de status em `/upload/b3/status/{correlationId}`
   - **E** ao concluir, exibe o resumo com linhas processadas, novas, duplicadas e falhas.

8. **Observabilidade, Logs Padronizados e Temporização UTC (`AGENTS.md`):**
   - **Dado** qualquer requisição ao endpoint de upload ou consumo de mensagens Kafka (`KafkaB3FileUploadedConsumer` e `KafkaTradeExtractedConsumer`)
   - **Então** toda entrada de API e processamento de tópico DEVE ser logada em nível `INFO` com dados de contexto sanitizados (`userId`, `profileId`, `fileName`, `correlationId`)
   - **E** qualquer exceção lançada ou capturada DEVE ser logada em nível `ERROR` com a mensagem descritiva e o stack trace completo (`log.error("...", e)`)
   - **E** todas as datas e timestamps de upload e processamento DEVEM utilizar obrigatoriamente `ZoneOffset.UTC`.

## Tasks / Subtasks

- [x] **Task 1: Dependência PDFBox e Modelo de Domínio** (AC: 1, 3, 8)
  - [x] 1.1 Adicionar dependência `org.apache.pdfbox:pdfbox:3.0.4` no `backend/common/pom.xml`.
  - [x] 1.2 Compilar `mvn clean test-compile -pl common` para garantir resolução limpa sem conflitos de dependências.
  - [x] 1.3 Assegurar que os registros de domínio `B3TradeRow` e `B3TradeRowFailure` em `afsdigital.grahamselect.common.upload.domain.model` atendam integralmente às necessidades de extração de notas SINACOR (sem duplicação de classes de modelo).

- [x] **Task 2: Motor de Parsing de PDF SINACOR (`SinacorPdfTradeParser`)** (AC: 2, 3, 4, 8)
  - [x] 2.1 Criar componente `SinacorPdfTradeParser` em `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java` padronizando a assinatura:
    `public void parse(InputStream inputStream, Consumer<B3TradeRow> successConsumer, Consumer<B3TradeRowFailure> failureConsumer) throws IOException`.
  - [x] 2.2 Implementar método de validação de cabeçalho `boolean isSinacorNote(InputStream is)` ou inspeção inicial de marcadores na primeira página.
  - [x] 2.3 Implementar a lógica de extração via `Loader.loadPDF(is)` e `PDFTextStripper` (PDFBox 3.x), com tratamento robusto linha por linha:
    - Regex para captura do cabeçalho da nota (Data do Pregão `dd/MM/yyyy`, Corretora).
    - Regex para captura da tabela de negócios: `(1-BOVESPA|1-BVMF|BOVESPA)\s+(C|V)\s+([A-Z\s]+?)\s+([A-Z0-9]{4,6}[F]?)\s+.*?\s+(\d+[\.,]?\d*)\s+(\d+[\.,]?\d*)\s+(\d+[\.,]?\d*)\s+(D|C)`.
    - Normalização do tipo de operação: mapear `C` -> `"COMPRA"` e `V` -> `"VENDA"`.
    - Normalização do ticker fracionário: se casado pelo padrão `^([A-Z]{4}\d{1,2})F$`, normalizar para `$1` (ex: `PETR4F` -> `PETR4`, `VALE3F` -> `VALE3`), preservando tickers normais que contenham F.
    - Normalização monetária brasileira: converter `1.500,25` para `BigDecimal` `1500.25`.
    - Normalização de data: converter `dd/MM/yyyy` para `LocalDate` com `ZoneOffset.UTC`.
  - [x] 2.4 Suporte completo a múltiplas páginas em documentos de múltiplos pregões ou dezenas de trades.
  - [x] 2.5 Criar testes unitários exaustivos em `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParserTest.java` com notas reais simuladas (XP, Clear, NuInvest, Inter), cobrindo compras (`COMPRA`), vendas (`VENDA`), fracionários, números formatados e notas multipáginas.

- [x] **Task 3: Controller e Roteamento de Upload de PDF** (AC: 1, 2, 6, 8)
  - [x] 3.1 Criar endpoint `POST /api/v1/upload/brokerage-note` em `BrokerageNoteUploadController.java` (ou integrar suporte a `.pdf` em `B3UploadController.java`).
  - [x] 3.2 Implementar validações estritas: arquivo não nulo, não vazio, tamanho <= 15MB, extensão `.pdf` e validação de marcadores SINACOR.
  - [x] 3.3 Obter bytes do arquivo (`file.getBytes()`) antes da validação de cabeçalho, garantindo que streams independentes (`ByteArrayInputStream`) sejam passados para a validação e para o use case de upload.
  - [x] 3.4 Aplicar sanitização contra Path Traversal no nome do arquivo e extração de `userId` via `jwt.getSubject()`.
  - [x] 3.5 Validar `profileId` se fornecido (mantendo regras de isolamento multi-tenant e verificação de perfil familiar ativo via `FamilyProfilePort`).
  - [x] 3.6 Delegar o upload diretamente para `UploadB3FileUseCase.execute(is, fileName, userId, profileId)`, retornando HTTP `202 Accepted` com `correlationId`.
  - [x] 3.7 Criar testes de integração MockMvc em `BrokerageNoteUploadControllerTest.java` testando cenários de sucesso (202), formato inválido (400), arquivo não SINACOR (400) e perfil não autorizado.

- [x] **Task 4: Consumidor Kafka e Integração com Pipeline de Trades** (AC: 3, 5, 8)
  - [x] 4.1 Adicionar anotação `@Slf4j` em `KafkaB3FileUploadedConsumer.java`, logando em nível `INFO` o início do processamento com `correlationId`, `userId` e `fileName`.
  - [x] 4.2 Injetar `SinacorPdfTradeParser` e atualizar o método de consumo para rotear pelo nome/extensão do arquivo:
    - Se terminar com `.xlsx`: delega para `FastExcelB3TradeRowParser`.
    - Se terminar com `.pdf`: delega para `SinacorPdfTradeParser`.
  - [x] 4.3 Garantir que qualquer exceção lançada no consumidor seja capturada e logada em nível `ERROR` com a mensagem e stack trace completo (`log.error("...", exception)`).
  - [x] 4.4 Para cada trade extraído pelo parser de PDF, despachar `B3TradeRow` para `processB3FileUseCase.handleExtractedTrade(event, tradeRow)` para emissão do `TradeExtractedEvent`.
  - [x] 4.5 Tratar falhas parciais de linha com `processB3FileUseCase.handleExtractionFailure(event, failure)` e atualizar progresso a cada lote de 100 linhas.
  - [x] 4.6 Garantir que o `KafkaTradeExtractedConsumer` existente processe os trades de PDF persistindo na tabela `trades` com deduplicação sem regressões.
  - [x] 4.7 Criar teste de integração Kafka E2E para o fluxo de PDF em `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaBrokerageNoteUploadIT.java`.

- [x] **Task 5: Frontend Flutter - Suporte a Notas de Corretagem em PDF** (AC: 7)
  - [x] 5.1 Atualizar `UploadRemoteDataSource` para incluir método `uploadBrokerageNoteFile(AppFile file, {String? token, String? profileId})` apontando para `/upload/brokerage-note` (ou permitir que `uploadB3File` manipule tanto `.xlsx` quanto `.pdf`).
  - [x] 5.2 Em `UploadRepository` e `UploadRepositoryImpl`, adicionar suporte a upload de notas de corretagem em PDF.
  - [x] 5.3 Atualizar `UploadProvider` (`frontend/lib/src/features/upload/presentation/providers/upload_provider.dart`):
    - Expandir `allowedExtensions` no `FilePicker` para `['xlsx', 'pdf']`.
    - Detectar extensão do arquivo selecionado e acionar o fluxo apropriado.
  - [x] 5.4 Atualizar `B3UploadZone` (`frontend/lib/src/features/upload/presentation/widgets/b3_upload_zone.dart`):
    - Atualizar textos de auxílio: `"Arraste sua planilha da B3 (.xlsx) ou Nota de Corretagem SINACOR (.pdf) aqui"`.
    - Tratar drag-and-drop de arquivos com extensão `.pdf` e `.xlsx`.
  - [x] 5.5 Atualizar `UploadPage` (`upload_page.dart`):
    - Atualizar títulos e subtítulos para refletir suporte a Planilhas B3 e Notas de Corretagem PDF.
    - Exibir chip/badge indicando formatos aceitos (`.xlsx`, `.pdf`).
  - [x] 5.6 Executar `flutter pub run build_runner build --delete-conflicting-outputs` para atualizar mocks do Mockito caso métodos de repositório/datasource sejam adicionados ou alterados.
  - [x] 5.7 Atualizar os testes existentes de frontend (`upload_page_test.dart`, `upload_provider_test.dart`, `b3_upload_zone_test.dart`) e adicionar testes específicos para upload de arquivo `.pdf`.

- [x] **Task 6: Validação Geral de Qualidade, Logs e Análise Estática** (AC: 8)
  - [x] 6.1 Rodar suíte completa de testes no backend: `mvn clean test` em todos os módulos (`common`, `api`, `valuation-service`).
  - [x] 6.2 Rodar `flutter test` e `flutter analyze` garantindo 0 erros e 0 warnings no frontend.
  - [x] 6.3 Validar que todos os novos logs de entrada de API, tópicos e tratamento de exceções com `ERROR` estão conformes com `AGENTS.md`.

### Review Findings
- [x] [Review][Decision] Decisão sobre duplicação arquitetural entre B3UploadController e BrokerageNoteUploadController — Resolvido: Opção 1 (Restringir PDFs exclusivamente ao BrokerageNoteUploadController /api/v1/upload/brokerage-note e manter B3UploadController apenas para .xlsx).
- [x] [Review][Patch] Restringir B3UploadController exclusivamente a .xlsx removendo suporte a PDF duplicado [backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3UploadController.java]
- [x] [Review][Patch] Corrigir fallback silencioso de data para LocalDate.now() em caso de falha de parsing ou header ausente [backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java:233]
- [x] [Review][Patch] Suportar tickers alfanuméricos com dígitos como B3SA3 e fracionário B3SA3F no TICKER_PATTERN [backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java:41]
- [x] [Review][Patch] Prevenir corrupção de valores em parseBigDecimal para formatos decimais [backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java:216]
- [x] [Review][Patch] Suportar extração de data e corretora por página em documentos com múltiplos pregões [backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java:108]
- [x] [Review][Patch] Adequar capturas de exceção para nível ERROR com stack trace completo conforme AGENTS.md [backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java:85,229]
- [x] [Review][Patch] Refinar regex de extração de corretora para ignorar linhas de negócios e metadados [backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java:235]
- [x] [Review][Patch] Corrigir reset prematuro do failureCounter no KafkaB3FileUploadedConsumer [backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3FileUploadedConsumer.java:75]
- [x] [Review][Patch] Adicionar feedback visual (SnackBar) ao soltar arquivo de extensão não suportada no B3UploadZone [frontend/lib/src/features/upload/presentation/widgets/b3_upload_zone.dart:25]
- [x] [Review][Patch] Adicionar testes no BrokerageNoteUploadControllerTest para profileId inválido e não pertencente ao usuário [backend/api/src/test/java/afsdigital/grahamselect/api/upload/web/BrokerageNoteUploadControllerTest.java]
- [x] [Review][Patch] Suportar prefixos variados de câmara (ex: \d+-BOVESPA) e falhar se PDF SINACOR contiver zero trades [backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java:31,144]
- [x] [Review][Patch] Sanitizar espaços em branco em nomes de arquivo na verificação de extensão em UploadB3FileUseCase [frontend/lib/src/features/upload/domain/usecases/upload_b3_file_usecase.dart:11]

## Dev Notes

### Arquitetura e Padrões Obrigatórios

1. **Multi-Módulo Spring Boot:**
   - A dependência do Apache PDFBox deve residir no módulo `common` (`backend/common/pom.xml`), permitindo que modelos e componentes de parsing sejam compartilhados.
   - O controlador REST `BrokerageNoteUploadController` reside no módulo `api` (`backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/`).
   - O módulo `valuation-service` não necessita de alterações diretas nesta história.

2. **Formato SINACOR e Lógica do Parser:**
   - No formato padrão SINACOR adotado por corretoras brasileiras:
     - Cabeçalho contém `"NOTA DE CORRETAGEM"`, nome da corretora e data do pregão (`dd/MM/yyyy`).
     - Linhas da tabela de negócios seguem uma estrutura tabular consistente:
       - Praça: geralmente `1-BOVESPA` ou `1-BVMF`.
       - Operação: `C` ou `V`. **Atenção Mandatória:** Deve ser mapeado para `"COMPRA"` ou `"VENDA"`. O uso de `"BUY"` ou `"SELL"` quebrará as regras de cálculo de custódia e queries SQL (`WHERE side = 'COMPRA'`).
       - Mercado: `VISTA`, `FRACIONARIO`, etc.
       - Ticker: código do ativo negociado (ex: `PETR4`, `VALE3`, `WEGE3`, `HGLG11`, `XPML11`, `AAPL34`).
       - Ticker Fracionário: no mercado fracionário, notas frequentemente trazem o ativo com terminação 'F' (ex: `PETR4F`, `VALE3F`, `BBDC4F`). O parser deve normalizar eliminando o sufixo 'F' apenas quando casar com o padrão de ações tradicionais (`^([A-Z]{4}\d{1,2})F$`), convertendo para o ticker original sem o 'F'.
       - Quantidade: número de cotas ou ações.
       - Preço: preço unitário em R$ (formatação brasileira `1.234,56` convertida para `BigDecimal`).
     - Exemplo de linha textual típica gerada por PDFBox de uma nota SINACOR:
       `1-BOVESPA C VISTA PETR4 PETROBRAS PN N2 100 38,50 3.850,00 D`
       ou no fracionário:
       `1-BOVESPA C FRACIONARIO PETR4F PETROBRAS PN N2 15 38,50 577,50 D`
   - O parser deve ser tolerante a variações menores de espaçamento entre colunas e quebras de linha entre ativos.

3. **Apache PDFBox 3.x (Migração/Uso Moderno):**
   - Na versão 3.x do PDFBox, utiliza-se `Loader.loadPDF(byte[])` ou `Loader.loadPDF(InputStream)` em vez do antigo construtor `PDDocument.load(...)` da versão 2.x.
   - Use `PDFTextStripper` para extração de texto estruturado página a página.

4. **Reutilização da Infraestrutura Existente:**
   - Não reinventar o fluxo de upload: reutilizar diretamente `UploadB3FileUseCase.execute(is, fileName, userId, profileId)`. Ele encapsula gravação no storage, geração de correlationId, criação de status e publicação no Kafka.
   - Não reinventar rastreamento de status: reutilizar `B3ImportStatusPort` e tabela `b3_import_status` (com os estados `RECEIVED`, `PROCESSING`, `COMPLETED`, `FAILED`).
   - Não reinventar deduplicação: os eventos emitidos como `TradeExtractedEvent` já são tratados com validação de duplicidade por `SaveTradeUseCase.execute(trade)` e gravados na tabela `trades`.

5. **Regras de Negócio Transversais (`AGENTS.md` e `project-context.md`):**
   - **UTC:** Todas as datas e timestamps de operações devem ser normalizados para `ZoneOffset.UTC`.
   - **Multitenancy & JWT:** O `userId` deve ser extraído do `jwt.getSubject()` de forma estrita.
   - **Path Traversal:** Validar e sanitizar nomes de arquivos contra qualquer sequência `../` ou separadores de caminho inválidos.
   - **Logs:** Registrar logs `INFO` em todas as entradas de API e consumo de mensagens Kafka, e logs de nível `ERROR` em todos os lançamentos e capturas de exceções, incluindo a mensagem e stack trace completo (`log.error("...", e)`).

### Project Structure Notes

- **Backend:**
  - `backend/common/pom.xml`: Adicionar dependência `pdfbox`.
  - `backend/common/src/main/java/afsdigital/grahamselect/common/upload/domain/model/`: `B3TradeRow`, `B3TradeRowFailure`.
  - `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java`: Implementação do parser PDFBox com a mesma assinatura do `FastExcelB3TradeRowParser`.
  - `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/BrokerageNoteUploadController.java`: Endpoint REST para upload de PDFs.
  - `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3FileUploadedConsumer.java`: Consumidor Kafka roteador com observabilidade e `@Slf4j`.
- **Frontend:**
  - `frontend/lib/src/features/upload/data/datasources/upload_remote_data_source.dart`: Chamada HTTP multipart.
  - `frontend/lib/src/features/upload/presentation/providers/upload_provider.dart`: Gerenciamento de estado e seleção de arquivo `.pdf`.
  - `frontend/lib/src/features/upload/presentation/widgets/b3_upload_zone.dart`: Área visual de drag-and-drop e seleção com suporte a `.pdf`.
  - `frontend/lib/src/features/upload/presentation/pages/upload_page.dart`: Página de importação com cópia e badges atualizados.

### References

- PRD: [docs/bmad/planning-artifacts/prd.md#2-data-ingestion--integration-a-b3--beyond](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/prd.md) (Requisito FR9)
- Epics: [docs/bmad/planning-artifacts/epics.md#epic-9-expansao-de-ingestao-de-dados-post-mvp](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/epics.md)
- Arquitetura: [docs/bmad/planning-artifacts/architecture.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/architecture.md)
- Regras Transversais de Agentes: [AGENTS.md](file:///c:/Users/afssi/projects/graham-select/AGENTS.md)
- Contexto do Projeto: [docs/bmad/project-context.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/project-context.md)
- Implementação de Upload Excel B3: [docs/bmad/implementation-artifacts/2-1-b3-upload-excel-ingestion.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/implementation-artifacts/2-1-b3-upload-excel-ingestion.md)
- Splitter Kafka e Deduplicação: [docs/bmad/implementation-artifacts/2-2-async-kafka-splitter-processing.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/implementation-artifacts/2-2-async-kafka-splitter-processing.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.8 Flash (Medium)

### Debug Log References

- Análise estática e compilação do backend Maven
- Testes unitários com JUnit 5 e Mockito
- Testes com Testcontainers MySQL e Kafka
- `flutter test` e `flutter analyze`

### Completion Notes List

- Adicionada dependência Apache PDFBox 3.0.4 em `backend/common/pom.xml`.
- Implementado `SinacorPdfTradeParser` com extração precisa de cabeçalhos SINACOR (XP, Clear, NuInvest, etc.), data de pregão em UTC, corretora, linhas de negociação, conversão padronizada de `C`/`V` para `COMPRA`/`VENDA`, normalização fracionária (`PETR4F` -> `PETR4`), suporte a números formatados brasileiros e suporte nativo a notas multipáginas.
- Implementado `BrokerageNoteUploadController` (`POST /api/v1/upload/brokerage-note`) e atualizado `B3UploadController` (`POST /api/v1/upload/b3`) para suporte dual `.xlsx` e `.pdf`, incluindo validações rigorosas de tipo, tamanho (15MB), marcadores SINACOR via streams independentes de bytes, sanitização contra Path Traversal, validação opcional de perfil familiar (`profileId`) e resposta `202 Accepted` com `correlationId`.
- Atualizado consumidor Kafka `KafkaB3FileUploadedConsumer` com anotação `@Slf4j`, logs de entrada `INFO` com dados de contexto sanitizados (`userId`, `profileId`, `fileName`, `correlationId`), logs `ERROR` com mensagem e stack trace completo (`AGENTS.md`), e roteamento automático baseado na extensão (`.pdf` via `SinacorPdfTradeParser` e `.xlsx` via `FastExcelB3TradeRowParser`).
- Atualizado frontend Flutter: `UploadRemoteDataSource`, `UploadRepository`, `UploadRepositoryImpl`, `UploadB3FileUseCase`, `UploadProvider`, `B3UploadZone` e `UploadPage` com suporte completo a arquivos `.pdf` e `.xlsx`, textos instrutivos, badges de formato aceito e drag-and-drop.
- Validação estática `flutter analyze` finalizada com 0 erros e 0 warnings.
- Validação de testes no frontend: 255/255 testes passaram com sucesso (`flutter test`).
- Validação completa de testes no backend: `mvn clean test -f backend/pom.xml` bem-sucedido em todos os módulos (`common`, `api-service`, `valuation-service`).

### File List

- `backend/common/pom.xml`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParser.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/BrokerageNoteUploadController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/web/B3UploadController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3FileUploadedConsumer.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/parser/SinacorPdfTradeParserTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/upload/web/BrokerageNoteUploadControllerTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaBrokerageNoteUploadIT.java`
- `frontend/lib/src/features/upload/data/datasources/upload_remote_data_source.dart`
- `frontend/lib/src/features/upload/domain/repositories/upload_repository.dart`
- `frontend/lib/src/features/upload/data/repositories/upload_repository_impl.dart`
- `frontend/lib/src/features/upload/domain/usecases/upload_b3_file_usecase.dart`
- `frontend/lib/src/features/upload/presentation/providers/upload_provider.dart`
- `frontend/lib/src/features/upload/presentation/widgets/b3_upload_zone.dart`
- `frontend/lib/src/features/upload/presentation/pages/upload_page.dart`
- `frontend/test/features/upload/presentation/pages/upload_page_test.dart`
- `frontend/test/features/upload/data/repositories/upload_repository_impl_test.dart`
- `frontend/test/features/upload/presentation/providers/upload_provider_test.dart`
- `frontend/test/features/upload/upload_page_profile_selector_test.dart`
- `docs/bmad/implementation-artifacts/9-1-importacao-de-nota-de-corretagem-pdf.md`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`
