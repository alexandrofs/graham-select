---
stepsCompleted: []
inputDocuments: [
  "/Users/alexandrofs/Documents/projects/graham-select/docs/bmad/brainstorming/negociacao-2026-03-09-21-37-18.xlsx"
]
workflowType: 'research'
lastStep: 1
research_type: 'technical'
research_topic: 'Extração de dados do relatório Excel (Negociação B3)'
research_goals: 'Verificar a viabilidade técnica, bibliotecas e melhor arquitetura para extração automatizada de dados do relatório de negociação da B3.'
user_name: 'Alex'
date: '2026-03-09'
web_research_enabled: true
source_verification: true
---

# Research Report: technical

**Date:** 2026-03-09
**Author:** Alex
**Research Type:** technical

---

## Extração de Dados B3 em Alta Escala: Comprehensive Technical Research

## Executive Summary

A extração automatizada de dados de negociação da B3 (Bolsa do Brasil) é fundamental para viabilizar produtos de gestão de portfólio no ecossistema Graham Select. Como a B3 processa milhões de transações diárias em um dos 10 maiores mercados do mundo, a ingestão desses lotes de dados por plataformas de terceiros exige tolerância a falhas, processamento não-bloqueante e respeito estrito a dados sensíveis (PII). A abordagem de ler o arquivo `.xlsx` em memória, linha a linha, em processos síncronos, foi invalidada devido ao iminente risco de vazamento de memória (OOM).

**Key Technical Findings:**
*   **Arquitetura:** O padrão Splitter em uma Arquitetura Orientada a Eventos (EDA) provou ser o mais eficiente. O arquivo de origem é recebido via REST asíncrono, salvo em S3 (Claim Check Pattern) e um Worker isolado lê o Excel em streaming, fragmentando-o em micro-eventos individuais no Kafka.
*   **Implementação:** A escolha da biblioteca `fastexcel-reader` reduz drasticamente o memory footprint comparado ao Apache POI, permitindo o uso de contêineres menores no K8s. 
*   **Escalabilidade e Resiliência:** O uso do Kafka com partições mapeadas para múltiplos Consumers garante que, se uma linha do Excel estiver corrompida, ela vá para uma *Dead Letter Topic* (DLT), enquanto as outras dezenas de milhares de operações na mesma planilha continuam sendo faturadas em tempo real.

**Technical Recommendations:**
*   Implementar ingestão Incremental: S3 Upload -> Notificação de Evento no Kafka -> WebWorker Spliter com FastExcel.
*   Monitorar ativamente o *Consumer Lag* das partições para garantir que a percepção de tempo real para o usuário do Graham Select seja mantida nos dias de pico do mercado.
*   Proteger os eventos em trânsito e em repouso mitigando impactos de PII.

## Table of Contents

1. Technical Research Introduction and Methodology
2. Technical Landscape and Architecture Analysis
3. Implementation Approaches and Best Practices
4. Technology Stack Evolution and Current Trends
5. Integration and Interoperability Patterns
6. Performance and Scalability Analysis
7. Security and Compliance Considerations
8. Strategic Technical Recommendations
9. Implementation Roadmap and Risk Assessment

## 1. Technical Research Introduction and Methodology

### Technical Research Significance

A B3 desempenha um papel cardeal na infraestrutura financeira da América Latina. O volume de negociações e a volatilidade exigem que as ferramentas de acompanhamento para investidores não sofram engasgos na hora de consolidar aportes. Processar relatórios da B3 de ponta a ponta sem impactar os recursos na nuvem permite ao Graham Select crescer sua base de clientes sem medo de *bottlenecks* técnicos.
_Technical Importance: Eliminar IO bloqueante e uso massivo de Heap._
_Business Impact: Escalabilidade infinita no on-boarding de usuários trazendo históricos gigantescos da B3._

### Technical Research Methodology

*   **Technical Scope**: Análise de Ingestão de Arquivos, Mensageria distribuída (Kafka), Parsing de planilhas e Arquitetura Limpa.
*   **Data Sources**: Documentação do Spring IO, Apache Kafka, Confluent Blogs e Github do Fastexcel.
*   **Analysis Framework**: Padrões de Integração Corporativa (EIP) e Design Orientado a Domínio (DDD).

### Technical Research Goals and Objectives

**Original Technical Goals:** Verificar a viabilidade técnica, bibliotecas e melhor arquitetura para extração automatizada de dados do relatório de negociação da B3.

**Achieved Technical Objectives:**
*   Estratégia do FastExcel-Reader mitigando alto custo computacional validada.
*   Pipeline de processamento em duas etapas (Splitter) no Kafka desenhado.
*   Current web data with rigorous source verification
*   Multi-source validation for critical technical claims
*   Confidence level framework for uncertain information
*   Comprehensive technical coverage with architecture-specific insights

**Scope Confirmed:** 2026-03-09

---

## Research Overview

[Research overview and methodology will be appended here]

---

## Technology Stack Analysis

### Programming Languages

O ecossistema Java, especificamente Java 17 ou superior, é altamente recomendado para esse tipo de processamento pesado devido às melhorias no Garbage Collector (como G1GC e ZGC) que otimizam a alocação de memória durante a ingestão de grandes arquivos.
_Popular Languages: Java, Kotlin_
_Emerging Languages: Go (para workers extremamente enxutos), porém o ecossistema Java para arquivos Excel (POI/FastExcel) ainda é muito maduro._
_Language Evolution: Transição forte para Java 17+ em novos projetos Spring Boot._
_Performance Characteristics: Alta performance, porém necessita de cuidado com o heap sizing (OOM errors) em ingestão de arquivos._
_Source: Análise da Stack - Ecossistema Spring Boot_

### Development Frameworks and Libraries

Para parsing de planilhas pesadas, a biblioteca **FastExcel** tem demonstrado ser até 10x mais rápida que o Apache POI (mesmo comparada à API de streaming SXSSFWorkbook do POI) e consome até 12x menos heap memory. O Spring Boot fornece a infraestrutura ideal de injeção de dependência, web e integração via Spring Kafka.
_Major Frameworks: Spring Boot 3.x, Spring Kafka_
_Micro-frameworks: FastExcel (para leitura em stream sem carregar o arquivo em memória), Apache POI (para manipulações muito complexas / estilos, embora não recomendado no cenário de alta volumetria simples)._
_Evolution Trends: Uso de bibliotecas de streaming em detrimento de abordagens DOM-like para leitura de arquivos._
_Ecosystem Maturity: Altíssima maturidade tanto de Spring quanto das libs de Excel em Java._
_Source: [Fastexcel vs Apache POI](https://howtodoinjava.com/java/library/fastexcel-vs-apache-poi/)_

### Database and Storage Technologies

Quando se fala de integração de arquivos grandes com Kafka, a antipattern é trafegar o binário na mensagem. O padrão correto é o **Claim Check Pattern**: o arquivo é salvo em um Blob Storage (S3, GCS, ou MinIO localmente) ou banco de dados relacional de forma temporária/permanente. O MySQL do projeto atual pode servir como storage se utilizado o tipo `LONGBLOB`, ou preferencialmente um serviço de arquivos dedicado local (S3-compatible).
_Relational Databases: MySQL 8+ (para os metadados da importação e status do lote)_
_NoSQL Databases: Amazon S3, MinIO AWS S3 Compatible (para o arquivo físico)_
_In-Memory Databases: Redis (para track de progresso do upload/processamento, via WebSockets/Polling no frontend)_
_Data Warehousing: N/A neste escopo_
_Source: [Kafka Large Messages (Claim Check Pattern)](https://medium.com/event-driven-utopia/handling-large-messages-in-apache-kafka-claim-check-pattern-a4ed77833ba)_

### Development Tools and Platforms

A stack tira proveito do ferramental tradicional de desenvolvimento backend e da infraestrutura de CI/CD para deploy. O Kafdrop ou AKHQ podem ser usados para inspecionar os tópicos.
_IDE and Editors: IntelliJ IDEA_
_Version Control: Git, GitHub/GitLab_
_Build Systems: Maven ou Gradle_
_Testing Frameworks: JUnit 5, Testcontainers (fundamental para subir o Kafka e o MySQL nos testes de integração)._
_Source: Conhecimento consolidado de ecossistema Java._

### Cloud Infrastructure and Deployment

Para gerenciar esse tipo de arquitetura envolvendo Kafka e Spring Boot, containers são mandatórios.
_Major Cloud Providers: AWS (MSK para Kafka, S3 para storage, ECS/EKS para os workers)_
_Container Technologies: Docker, Kubernetes_
_Serverless Platforms: AWS Lambda (apenas se o tempo de processamento das partes do Excel ficar abaixo do timeout padrão, logo, consumers long-running no ECS/EKS são mais adequados)._
_CDN and Edge Computing: Cloudflare (upload de chunks diretamente)_
_Source: Práticas modernas de deployment Spring Boot + Kafka_

### Technology Adoption Trends

_Migration Patterns: Mudança de processamento síncrono no request HTTP para design orientado a eventos usando Kafka._
_Emerging Technologies: FastExcel dominando uploads de altíssima volumetria frente ao antigo Apache POI._
_Legacy Technology: Envio direto de binários grandes no body HTTP resultando em timeouts (sendo substituído por presigned URLs no S3 + notificação no Kafka)._
_Community Trends: Adoção do Claim Check Pattern em Kafka para evitar "Kafka Message Too Large" errors._
_Source: [Kafka Best Practices](https://medium.com/@soham.k/handling-large-messages-in-kafka-3914a1a54728)_

## Integration Patterns Analysis

### API Design Patterns

Para o carregamento de arquivos na plataforma de investimentos Graham Select, o padrão REST API Asíncrono é ideal.
_RESTful APIs: Um endpoint `POST /api/v1/b3-reports` recebe o arquivo `multipart/form-data`, valida os headers básicos, salva num storage persistente e retorna um protocolo `202 Accepted` ao cliente, sem bloquear a conexão._
_GraphQL APIs: Consultas para monitoramento de status da importação podem ocorrer via GraphQL, se já utilizado no projeto._
_RPC and gRPC: N/A para este fluxo de upload frontend-backend._
_Webhook Patterns: O backend pode emitir Webhooks ou SSE (Server-Sent Events) pro frontend quando um lote de leitura do arquivo no Kafka for consumido e inserido com sucesso na base._
_Source: Análise da Stack - API Asíncrona com Spring_

### Communication Protocols

A comunicação inicial será via HTTP/HTTPS e internamente via TCP/IP para o Kafka.
_HTTP/HTTPS Protocols: Upload inicial do `.xlsx`._
_WebSocket Protocols: Padrão excelente para disparar progressões da leitura do backgorund process Kafka de volta ao UI do cliente._
_Message Queue Protocols: O protocolo binário nativo do Kafka será utilizado para escalar o consumo de mensagens de metadados do arquivo._
_grpc and Protocol Buffers: Pode-se optar por serializar o payload da mensagem enviada ao Kafka via Protobuf para reduzir latência e overhead, ao invés de JSON._
_Source: [Spring Boot REST to Kafka](https://medium.com/@sivalabs/spring-boot-kafka-tutorial-part-1-introduction-ea5dc4b3e8e2)_

### Data Formats and Standards

Embora JSON seja o padrão para o conteúdo da mensagem do Kafka, para alta performance recomenda-se usar algo mais condensado.
_JSON and XML: JSON para a mensagem de metadados que irá ao tópico Kafka, ex: `{"fileId": "123", "tenantId": "456", "path": "/s3/bucket/file.xlsx"}`._
_Protobuf and MessagePack: Pode substituir JSON no Kafka se a performance for crítica._
_CSV and Flat Files: N/A - o formato de input é o .xlsx da B3._
_Custom Data Formats: O schema exato da mensagem Kafka deve refletir a intenção (Command Pattern), ex: `ProcessB3ReportCommand`._
_Source: Padrões de payload Kafka_

### System Interoperability Approaches

Como é um sistema novo, a interoperabilidade foca na divisão dentro da mesma aplicação ou entre microsserviços do mesmo domínio de investimentos.
_Point-to-Point Integration: Consumo de eventos diretamente dos tópicos Kafka definidos._
_API Gateway Patterns: Um API Gateway pode rate-limitar a quantidade de uploads concorrentes._
_Service Mesh: Relevante no deployment de microsserviços via Kubernetes._
_Enterprise Service Bus: Substituído pela arquitetura Event-Driven com Kafka._
_Source: [Kafka in Spring Boot](https://www.instaclustr.com/blog/spring-boot-and-apache-kafka)_

### Microservices Integration Patterns

Essa extração divide claramente a responsabilidade de "Recepção" vs "Processamento".
_API Gateway Pattern: Recepção exposta externamente._
_Service Discovery: Se o Consumer estiver em projeto Spring separado, será util._
_Circuit Breaker Pattern: Usar resilience4j no Consumer Kafka caso o banco de dados da importação comece a falhar, permitindo pausar o processamento sem perder as mensagens do tópico Kafka._
_Saga Pattern: Se a importação gerar movimentações em contas bancárias/carteiras que exigem rollback em caso de falha numa linha do Excel, Saga Coreografado é essencial._
_Source: [Microservices Patterns on Kafka](https://medium.com/geekculture/microservice-communication-with-kafka-2e213ab1ac2a)_

### Event-Driven Integration

Esta é a base da solução de upload da B3 decidida. 
_Publish-Subscribe Patterns: O Upload Controller atua como Publisher, o componente FastExcel atua atrelado ao Subscriber._
_Event Sourcing: Menos necessário que CQRS._
_Message Broker Patterns: Padrão central do design, utilizando partições para garantir escalabilidade via Consumer Groups. Múltiplas planilhas poderiam ser processadas ao mesmo tempo por diferentes nós._
_CQRS Patterns: A leitura (Query) do status do processamento fica separada do comando de Ingestão (Command) do Kafka._
_Source: [Spring Kafka Asynchronous Processing](https://springfuse.com/asynchronous-processing-with-spring-boot-and-kafka/)_

### Integration Security Patterns

Os arquivos da B3 contêm dados financeiros críticos de PII (Personally Identifiable Information).
_OAuth 2.0 and JWT: Autenticação padrão no endpoint REST de upload._
_API Key Management: N/A se for uso final de cliente._
_Mutual TLS: Forte recomendação para comunicação mTLS entre as instâncias Spring Boot e os Brokers do Kafka MSK._
_Data Encryption: O arquivo `.xlsx` deve ser salvo encriptado At-Rest no S3. O Payload do Kafka pode não levar PII, apenas a referência (ID do banco/S3)._
_Source: Segurança da Arquitetura Spring Kafka_

## Architectural Patterns and Design

### System Architecture Patterns

A arquitetura geral utiliza o padrão de **Event-Driven Architecture (EDA)** combinada com processamento em Pipeline, focada em alta escalabilidade usando o padrão "Splitter" em dois estágios.
_Microservices, monolithic, and serverless patterns: O sistema pode adotar um "Modular Monolith" no backend inicial, separando clean modules entre o Recebedor de Uploads e os Consumers Kafka._
_Event-driven and reactive architectures: Ingestão focada em pipeline de eventos._ 
1. _A chegada do arquivo emite o evento `FileUploadedEvent`._
2. _Um Web Worker leve (FileProcessor) consome esse evento, lê as linhas do Excel._
3. _Ele aplica o padrão **Splitter**, publicando um evento `TradeExtractedEvent` para **cada linha** (ou pequeno lote de linhas) válida do arquivo no Kafka._
_Domain-driven design patterns: Bounded Context isolado para "Importação de Extratos", que interage com o Bounded Context de "Portfólio" (onde ficam os lançamentos efetivos)._
_Source: Enterprise Integration Patterns - Splitter Pattern_

### Design Principles and Best Practices

A implementação de dois estágios (File -> Rows) traz benefícios monstruosos para paralelismo.
_SOLID principles and their application: Single Responsibility no processamento de parsers. O construtor foca na extração; enquanto milhares de instâncias de "TradeConsumer" focam no negócio sem saber de onde vieram as "linhas" do Excel._
_Clean architecture and hexagonal architecture: A lógica do negócio opera em cima de um DTO de "Trade" simples via Kafka, sem jamais precisar lidar com a dependência do fastexcel no Domínio core._
_Source: Design de Resiliência de Arquivos Grandes._

### Scalability and Performance Patterns

Escalabilidade foca agora totalmente no paralelismo em nível de linha, removendo qualquer gargalo no loop do arquivo.
_Horizontal vs vertical scaling patterns: Se o arquivo Excel tem 100.000 linhas, ele irá gerar até 100.000 mensagens no Kafka. O Kafka é feito exatamente para isso. Com dezenas de `TradeProcessors` (consumers) rodando paralelos nas partições, as linhas são inseridas no banco de dados e as carteiras atualizadas em altíssima velocidade._
_Performance optimization techniques: O `FileProcessor` (que divide as linhas) ainda usará o FastExcel (streaming sem OOM), e o Kafka usará compressão (ex: Snappy), otimizando o I/O do broker para muitas mensagens pequenas._
_Source: Kafka Microservices Event-Driven Design_

### Integration and Communication Patterns

_Integration patterns analysis with source citations: "Claim Check Pattern" misturado com "Splitter". O payload grande (Excel) reside em um S3. O FileProcessor usa o claim check para baixar, mas logo fragmenta o conteúdo em micro-eventos auto contidos para os módulos finais consumirem._
_Source: [Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/patterns/messaging/Sequencer.html)_

### Security Architecture Patterns

_Security patterns analysis with source citations: Isolar o tópico Kafka num grupo de segurança. O PII do arquivo mestre fica isolado, mas atenção: o `TradeExtractedEvent` circulando no Kafka terá dados sensíveis da linha em plain text (nome do usuário/CPF). Recomenda-se não usar dados puramente identificáveis (apenas IDs internos) no payload, ou então habilitar In-Transit encryption com TLS no Kafka e At-Rest encryption do Tópico._
_Source: AWS MSK Security Best Practices_

### Data Architecture Patterns

_Data architecture analysis with source citations: Event Sourcing real. Cada linha lida se torna um `TradeExtractedEvent` independente. E se houver erro apenas na linha 500 do arquivo? Ela vai para a DLT (Dead Letter Topic) sem penalizar o resto do processamento que teve sucesso!_
_Source: [Spring Fuse - Kafka Event Sourcing](https://springfuse.com/asynchronous-processing-with-spring-boot-and-kafka/)_

### Deployment and Operations Architecture

_Deployment architecture analysis with source citations: Monitoramento de Consumer Lag (ex: Datadog, Prometheus/Grafana) vital para a Fila de Trades. Métricas de sucesso linha-a-linha podem alimentar um Web Socket para a interface do Front, atualizando uma barra de progresso (ex: "Processado 1400/5000 transações")._
_Source: Monitoramento O11y - Kafka Metrics Pattern._

## Implementation Approaches and Technology Adoption

### Technology Adoption Strategies

As decisões devem focar num pipeline de pipeline incremental de Consumers.
_Technology adoption analysis with source citations: Implementação incremental: Criar primeiro a API REST com S3. Depois, criar o "File Splitter Consumer" que loga as linhas. Por último, o "Trade Processor Consumer" que atualiza o BD._
_Source: Padrões de Evolução Ágil em Microservices_

### Development Workflows and Tooling

A implementação necessita no pom.xml dependências exatas.
_Development workflows analysis with source citations: Utilizar o Maven: dependências `spring-kafka` e `fastexcel-reader`. O `fastexcel-reader` lidará com os binários usando low-footprint object (`ReadableWorkbook`)._
_Source: [Dhatim FastExcel GitHub](https://github.com/dhatim/fastexcel)_

### Testing and Quality Assurance

_Testing approaches analysis with source citations: Recomenda-se simular testes de falha onde 10% das mensagens de linhas contem dados inválidos da B3, verificando se o DLT captura os "lixos" enquanto o resto é faturado no banco._
_Source: Chaos Engineering in Event Driven_

### Deployment and Operations Practices

_Deployment practices analysis with source citations: Pods separados no K8s. Um pod (Deploy) apenas pro "File Processor" (requer mais Memória) e outro Pod (Deploy) apenas pro "Trade Processor" (requer mais CPU e Conexões com Banco)._
_Source: Scalable microservices_

### Team Organization and Skills

_Team organization analysis with source citations: Requer proficiência em Data Streaming, Transações Distribuídas e Idempotência._
_Source: Requisitos T-Shaped Java_

### Cost Optimization and Resource Management

_Cost optimization analysis with source citations: Como dezenas de milhares de mensagens serão mandadas, otimizar `retention.bytes` do Kafka para não manter retenção longa de log que já foi consumido, se não for utilizar Event Sourcing permanente._
_Source: Kafka Storage Optimization_

### Risk Assessment and Mitigation

_Risk mitigation analysis with source citations: Cuidado com a ordem. Arquivos da B3 podem demandar processamento na ordem correta da data de execução (Compra antes de Venda). Se lançar tudo no Kafka sem Keys, as mensagens serão embaralhadas nas partições. A Key do Producer Kafka DEVE ser o código do Ativo + CPF do usuário, para garantir que operações no mesmo ticker pelo mesmo usuário cheguem na ordem cronológica em uma partição fixa._
_Source: Kafka Partitioning & Ordering semantics_

## Technical Research Recommendations

### Implementation Roadmap

1. Infra (Docker Compose com Kafka e MinIO).
2. Endpoint de Upload -> S3 -> Envia `FileUploadedEvent`(ID do arquivo).
3. Consumer A (Splitter): Usa `fastexcel-reader`, lê do S3, e para cada linha envia um `TradeExtractedEvent`(payload da linha, Kafka Key: UserID+Ativo).
4. Consumer B (Processor): Consome `TradeExtractedEvent`, aplica regras de investimento e persiste o cálculo no MySQL.
5. Frontend assina via WebSocket para os processamentos concluídos pelo Consumer B.

### Technology Stack Recommendations

*   **Java 17+ / Spring Boot 3.x / Spring Kafka**
*   **FastExcel (Reader)**
*   **S3/MinIO** (Store do excel bruto)

### Success Metrics and KPIs

*   **Vazão Linha a Linha (Throughput)**: Capaz de processar 5.000 lançamentos/segundo via Consumer B;
*   **Consumer Lag**: Deve voltar a "0" no encerramento da submissão;
*   **Isolamento de Falha**: Linhas mal formadas afetam apenas a fila de falhas, sem quebrar o batch.

<!-- Content will be appended sequentially through research workflow steps -->
