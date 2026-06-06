---
baseline_commit: 8d96e833af23c5fb1327b89ba858582e217918ce
---
# Story 4.1: Integração de Market Data (Cotações & Indicadores)

Status: done

## Story

As a sistema (backend),
I want buscar cotações atualizadas e indicadores fundamentalistas (P/L, DY, P/VP) dos ativos em custódia via API externa (Brapi),
so that o cálculo do Filtro de Graham nas histórias subsequentes (4.3) use dados de mercado confiáveis e atualizados.

## Acceptance Criteria

1. **Given** que o sistema possui ativos em custódia (tabela `trade_transaction`)
   **When** o scheduler agendado do `market-data-service` executa o sync (uma vez ao dia fora do horário de pregão)
   **Then** consulta a API Brapi (`GET /api/quote/{tickers}?modules=summaryProfile,financialData&token={token}`) para todos os tickers únicos em custódia
   **And** atualiza a cotação (`price`) e a data (`price_date`) na tabela `stock_price` para cada ticker encontrado

2. **Given** que a API Brapi retorna dados fundamentalistas (P/L, DY, P/VP, VPA, LPA)
   **When** o sync processa a resposta
   **Then** persiste os indicadores fundamentalistas novos/atualizados no `FinancialDataEvent` consumido pelo `valuation-service` via tópico Kafka `financial-data`
   **And** armazena em cache Spring (Caffeine) com TTL de 24h por ticker

3. **Given** que a API Brapi falha (timeout, 5xx, rate limit)
   **When** o sync tenta buscar as cotações
   **Then** usa a cotação mais recente em cache (Graceful Degradation — NFR8)
   **And** registra log de nível WARN com o ticker e o motivo da falha
   **And** NÃO lança exceção para outros tickers — processa os que conseguir

4. **Given** que o ticker não existe na Brapi (IPO novo, erro de digitação)
   **When** a resposta retorna status de ticker inválido
   **Then** registra log de nível WARN com o ticker desconhecido e pula o ativo
   **And** NÃO interrompe o processamento dos demais tickers

5. **Given** que o sync concluiu com sucesso
   **When** o scheduler finaliza a execução
   **Then** registra log de nível INFO com total de tickers processados, atualizados com sucesso e falhos

## Tasks / Subtasks

### Backend — Módulo `common` (Domain + Application — POJOs puros, sem Spring)

- [x] **Task 1: Criar Port `MarketDataPort` em `common`** (AC: 1, 2)
  - [x] Criar interface `MarketDataPort` em `common/src/main/java/afsdigital/grahamselect/valuation/application/repository/MarketDataPort.java`
  - [x] Método `List<MarketDataResult> fetchMarketData(List<String> tickers)`
  - [x] Criar record `MarketDataResult` em `common/.../valuation/application/dto/` com campos: `ticker`, `price` (BigDecimal), `dividendYield` (Double), `priceEarnings` (Double), `priceToBook` (Double), `earningsPerShare` (Double), `bookValuePerShare` (Double)
  - [x] **SEM** importações Spring nesta camada (POJO puro)

- [x] **Task 2: Criar Use Case `SyncMarketDataUseCase` em `common`** (AC: 1, 2, 3, 4, 5)
  - [x] Criar `SyncMarketDataUseCase` em `common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/SyncMarketDataUseCase.java` como POJO (sem `@Component`)
  - [x] Método `execute()` — sem parâmetros; obtém tickers via `CustodyTickerPort`
  - [x] Injeta `MarketDataPort`, `CustodyTickerPort`, `MarketDataEventPublisherPort`
  - [x] Para cada ticker com resultado bem-sucedido: chama `MarketDataEventPublisherPort.publish(FinancialDataEvent)` (montando o evento com os dados obtidos)
  - [x] Lógica de resiliência: try-catch por ticker individual (AC: 3, 4); log WARN em falha; continua para próximo
  - [x] Ao finalizar: log INFO com contagem (AC: 5)
  - [x] Lombok `@RequiredArgsConstructor` + `@Slf4j`

- [x] **Task 3: Criar Port `CustodyTickerPort` em `common`** (AC: 1)
  - [x] Criar interface `CustodyTickerPort` em `common/src/main/java/afsdigital/grahamselect/valuation/application/repository/CustodyTickerPort.java`
  - [x] Método `List<String> findDistinctActiveTickers()` — retorna tickers únicos de `trade_transaction` onde há posição aberta

- [x] **Task 4: Criar Port `MarketDataEventPublisherPort` em `common`** (AC: 2)
  - [x] Criar interface `MarketDataEventPublisherPort` em `common/src/main/java/afsdigital/grahamselect/valuation/application/repository/MarketDataEventPublisherPort.java`
  - [x] Método `void publish(FinancialDataEvent event)` — publica no tópico `financial-data`

---

### Backend — Módulo `api` (Infrastructure + Scheduler)

- [x] **Task 5: Implementar adapter Brapi `BrapiMarketDataAdapter`** (AC: 1, 2, 3, 4)
  - [x] Criar `BrapiMarketDataAdapter` em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java`
  - [x] Implementa `MarketDataPort`
  - [x] Usar `RestClient` (Spring Boot 3.2+) — **NÃO** adicionar OpenFeign (evitar dependência extra)
  - [x] URL base: `https://brapi.dev/api`
  - [x] Endpoint: `GET /quote/{tickers}?modules=summaryProfile,financialData&token={token}`
  - [x] Passar múltiplos tickers numa única requisição separados por vírgula (ex: `PETR4,VALE3,ITUB4`)
  - [x] Token via `@Value("${brapi.token}")` — variável de ambiente `BRAPI_TOKEN`
  - [x] Timeout de conexão: 5s / leitura: 10s
  - [x] Cache com `@Cacheable(value = "market-data", key = "#ticker")` por ticker (AC: 3)
  - [x] Em caso de falha HTTP 4xx/5xx: retornar `Optional.empty()` e logar WARN

- [x] **Task 6: Implementar `CustodyTickerRepositoryImpl`** (AC: 1)
  - [x] Criar `CustodyTickerRepositoryImpl` em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/CustodyTickerRepositoryImpl.java`
  - [x] Implementa `CustodyTickerPort`
  - [x] Injeta o `TradeJpaRepository` existente em `api/src/.../portfolio/infrastructure/persistence/jpa/repository/`
  - [x] Query JPQL: `SELECT DISTINCT t.ticker FROM TradeEntity t` (tabela real: `trades`; entidade: `TradeEntity`)
  - [x] Campos reais do `TradeEntity`: `id` (UUID), `userId` (String), `ticker` (String, max 20), `side` (String — `"BUY"`/`"SELL"`), `tradeDate`, `quantity`, `price`, `broker`, `createdAt`
  - [x] **NÃO** usar `trade_transaction` — essa tabela não existe; a tabela correta é `trades`

- [x] **Task 7: Implementar `KafkaMarketDataEventPublisher`** (AC: 2)
  - [x] Criar `KafkaMarketDataEventPublisher` em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/KafkaMarketDataEventPublisher.java`
  - [x] Implementa `MarketDataEventPublisherPort`
  - [x] Injeta `KafkaTemplate<FinancialDataKey, FinancialDataEvent>`
  - [x] Publica no tópico `TopicConstants.FINANCIAL_DATA_TOPIC` (`"financial-data"`)
  - [x] Chave: `FinancialDataKey.builder().ticker(event.getTicker()).build()`
  - [x] Log INFO antes de publicar: `"Publishing market data event for ticker: {}"` (rastreabilidade de entrada Kafka)

- [x] **Task 8: Criar Scheduler `MarketDataScheduler`** (AC: 1, 5)
  - [x] Criar `MarketDataScheduler` em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java`
  - [x] Anotar com `@Component` e `@Slf4j`
  - [x] Método `syncMarketData()` anotado com `@Scheduled(cron = "${market-data.sync.cron:0 0 20 * * MON-FRI}")`
  - [x] Cron padrão: 20h (horário de Brasília = 23h UTC), segunda a sexta — após fechamento do pregão
  - [x] Log INFO na entrada do scheduler: `"Starting market data sync at {}"` (rastreabilidade obrigatória)
  - [x] Delega para `syncMarketDataUseCase.execute()`

- [x] **Task 9: Registrar todos os beans em `@Configuration`** (AC: 1, 2)
  - [x] Criar `MarketDataServiceConfiguration` em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataServiceConfiguration.java`
  - [x] Beans: `BrapiMarketDataAdapter`, `CustodyTickerRepositoryImpl`, `KafkaMarketDataEventPublisher`, `SyncMarketDataUseCase`
  - [x] Registrar cache `"market-data"` no `CacheManager` de Caffeine (TTL 24h) — verificar se `CaffeineCacheManager` já existe no projeto; se sim, apenas adicionar o cache name; se não, criar o bean
  - [x] Habilitar `@EnableScheduling` na configuração ou na classe principal `ApiServiceApplication`

- [x] **Task 10: Adicionar configurações em `application.properties`** (AC: 1, 3)
  - [x] `brapi.token=${BRAPI_TOKEN:}` — obrigatório via variável de ambiente
  - [x] `market-data.sync.cron=0 0 20 * * MON-FRI` — configurável via env
  - [x] `spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=86400s` — 24h TTL

---

### Testes

- [x] **Task 11: Testes unitários do Use Case** (AC: 1, 2, 3, 4, 5)
  - [x] Criar `SyncMarketDataUseCaseTest` em `common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/SyncMarketDataUseCaseTest.java`
  - [x] Testar: sync bem-sucedido (todos os tickers processados)
  - [x] Testar: falha de API para 1 ticker → continua para os demais (AC: 3)
  - [x] Testar: ticker inválido → WARN logado, sem interrupção (AC: 4)
  - [x] Testar: log INFO de conclusão com contagens corretas (AC: 5)
  - [x] Usar Mockito para mockar `MarketDataPort`, `CustodyTickerPort`, `MarketDataEventPublisherPort`

- [x] **Task 12: Testes de integração do adapter Brapi** (AC: 1, 3)
  - [x] Criar `BrapiMarketDataAdapterTest` em `api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java`
  - [x] Usar `WireMock` (já disponível via Testcontainers) ou `MockRestServiceServer` para simular respostas Brapi
  - [x] Testar: resposta 200 com dados completos → `MarketDataResult` mapeado corretamente
  - [x] Testar: resposta 5xx → retorna `Optional.empty()`, WARN logado
  - [x] Testar: resposta com ticker inválido na lista → WARN logado, skipped

- [x] **Task 13: Executar `mvn clean test` nos módulos `common` e `api`**
  - [x] Confirmar que `mvn clean test -pl common` passa sem falhas
  - [x] Confirmar que `mvn clean test -pl api` passa sem falhas

---

### Review Findings

- [x] [Review][Decision] Distorção de Data (Data Futura) em fuso UTC próximo à meia-noite — O uso de LocalDate.now(ZoneOffset.UTC) em execuções de scheduler no fim do dia de Brasília pode registrar cotações com a data do dia seguinte. Devemos alinhar se usamos o fuso local America/Sao_Paulo para datas civis de pregão ou se mantemos UTC conforme regra geral.
- [x] [Review][Decision] Query simplificada buscando ativos sem saldo em custódia (Zerados) — A query SELECT DISTINCT t.ticker FROM TradeEntity t traz ativos que o usuário já vendeu completamente. Isso conflita com a especificação original (AC 1: ativos em custódia onde há posição aberta). Devemos alinhar se mantemos a query simplificada da Task 6 ou se a aprimoramos para filtrar apenas ativos com saldo de custódia positivo.
- [x] [Review][Decision] Nível de log para falhas em API de terceiros (WARN vs ERROR) — Conflito entre a orientação da história (log WARN para evitar alarmar monitoramento com instabilidade de API externa) e o padrão do AGENTS.md (log ERROR em qualquer captura de exceção).
- [x] [Review][Patch] Injeção de @Value em bean construído de forma manual [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataServiceConfiguration.java]
- [x] [Review][Patch] Timezone indefinido no cron do Scheduler [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java]
- [x] [Review][Patch] Tratamento ineficiente de erros no RestClient [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java]
- [x] [Review][Patch] Falta de loteamento (batching) na URL da API Brapi [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java]
- [x] [Review][Patch] Envio assíncrono Kafka mascarando falhas de sincronização [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/KafkaMarketDataEventPublisher.java]
- [x] [Review][Patch] Complexidade quadrática no pós-processamento do cache [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java]
- [x] [Review][Patch] Validação de preços inválidos (NaN/Infinity) [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java]
- [x] [Review][Patch] Sanitização da lista de tickers de entrada [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java]
- [x] [Review][Defer] Ausência de bloqueio distribuído no Scheduler (ShedLock) [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java] — deferred, pre-existing

---

## Dev Notes

### 1. Contexto Arquitetural — Por que esta história é a base do Épico 4

Esta história implementa o **pipeline de ingestão de dados de mercado** que alimenta o `valuation-service` com cotações e indicadores fundamentalistas. Sem esses dados, as histórias 4.2 (Metas de Alocação), 4.3 (Motor Graham) e 4.4 (ReasoningBox) não têm base de cálculo.

**Fluxo de dados desta história:**
```
Scheduler (api) → BrapiMarketDataAdapter → Brapi API
                                         ↓
                                  FinancialDataEvent
                                         ↓
              KafkaMarketDataEventPublisher → tópico "financial-data"
                                         ↓
              FinancialDataConsumerService (valuation-service) [EXISTENTE]
                                         ↓
              CalculationIntrinsicValueUseCase [EXISTENTE] → DB (intrinsic_value, stock_price)
```

**Atenção:** O `FinancialDataConsumerService` e o `CalculationIntrinsicValueUseCase` **já existem** no `valuation-service`. Esta história apenas provê os dados de entrada via Kafka — o consumer existente processará automaticamente.

---

### 2. Análise do Código Existente — O Que NÃO Alterar

#### `FinancialDataConsumerService.java` (valuation-service) — **NÃO MODIFICAR**
```
backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/
  infrastructure/kafka/FinancialDataConsumerService.java
```
Já consome o tópico `"financial-data"` e delega para `CalculationIntrinsicValueUseCase`. Funciona como está.

#### `CalculationIntrinsicValueUseCase.java` (common) — **NÃO MODIFICAR**
```
backend/common/src/main/java/afsdigital/grahamselect/valuation/application/
  usecase/CalculationIntrinsicValueUseCase.java
```
Já persiste `IntrinsicValue` e `StockPrice` a partir do `FinancialDataEvent`.

#### `FinancialDataEvent.java` — **NÃO MODIFICAR, apenas USAR**
```
backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/FinancialDataEvent.java
```
Campos relevantes que o `BrapiMarketDataAdapter` deve preencher:
- `ticker` (String) — obrigatório
- `price` (Double) — cotação atual
- `dividendYield` (Double) — DY da Brapi
- `priceEarnings` (Double) — P/L
- `priceToBook` (Double) — P/VP
- `earningsPerShare` (Double) — LPA (VPA)
- `bookValuePerShare` (Double) — VPA
- `resultDate` (LocalDate) — usar `LocalDate.now()` no momento do sync

#### `TopicConstants.java` — **NÃO MODIFICAR, usar constante existente**
```java
TopicConstants.FINANCIAL_DATA_TOPIC = "financial-data"
```

#### `FinancialDataKey` / `FinancialDataKeySerializer` / `FinancialDataKeyDeserializer` — **USAR EXISTENTES**
```
backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/
  FinancialDataKey.java / FinancialDataKeySerializer.java / FinancialDataKeyDeserializer.java
```

---

### 3. Estrutura de Arquivos Esperada (NOVOS arquivos)

```
backend/
├── common/
│   └── src/main/java/afsdigital/grahamselect/
│       └── valuation/
│           └── application/
│               ├── dto/
│               │   └── MarketDataResult.java           ← NOVO (record)
│               ├── repository/
│               │   ├── MarketDataPort.java              ← NOVO (interface)
│               │   ├── CustodyTickerPort.java           ← NOVO (interface)
│               │   └── MarketDataEventPublisherPort.java ← NOVO (interface)
│               └── usecase/
│                   └── SyncMarketDataUseCase.java       ← NOVO (POJO)
│
└── api/
    └── src/main/java/afsdigital/grahamselect/api/
        └── valuation/
            ├── infrastructure/
            │   ├── brapi/
            │   │   └── BrapiMarketDataAdapter.java      ← NOVO
            │   ├── kafka/
            │   │   └── KafkaMarketDataEventPublisher.java ← NOVO
            │   ├── persistence/
            │   │   └── CustodyTickerRepositoryImpl.java  ← NOVO
            │   └── spring/
            │       ├── MarketDataServiceConfiguration.java ← NOVO
            │       └── MarketDataScheduler.java           ← NOVO
```

---

### 4. Integração com Brapi — Detalhes Técnicos

#### Endpoint e Resposta

```
GET https://brapi.dev/api/quote/PETR4,VALE3,ITUB4?modules=summaryProfile,financialData&token=SEU_TOKEN
```

**Resposta JSON (estrutura relevante):**
```json
{
  "results": [
    {
      "symbol": "PETR4",
      "regularMarketPrice": 38.45,
      "dividendYield": 8.2,
      "priceEarnings": 5.1,
      "priceToBook": 1.8,
      "defaultKeyStatistics": {
        "earningsPerShare": { "raw": 7.54 },
        "bookValue": { "raw": 21.36 }
      }
    }
  ]
}
```

> ⚠️ **Atenção**: A estrutura exata da resposta Brapi pode variar. O adapter deve tratar valores nulos com segurança (`Optional.ofNullable(...).orElse(null)`) e nunca lançar NPE.

#### DTO de Mapeamento (sugestão)
```java
// BrapiQuoteResponse.java (record interno no adapter ou package brapi/)
record BrapiQuoteResponse(List<BrapiResult> results) {}
record BrapiResult(
    String symbol,
    Double regularMarketPrice,
    Double dividendYield,
    Double priceEarnings,
    Double priceToBook,
    BrapiKeyStats defaultKeyStatistics
) {}
record BrapiKeyStats(BrapiRaw earningsPerShare, BrapiRaw bookValue) {}
record BrapiRaw(Double raw) {}
```

#### Configuração do `RestClient`
```java
// No @Configuration:
@Bean
public RestClient brapiRestClient() {
    return RestClient.builder()
        .baseUrl("https://brapi.dev/api")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .requestFactory(new SimpleClientHttpRequestFactory()) // com timeouts via config
        .build();
}
```

#### Limite da Brapi (Plano Gratuito)
- 15.000 requisições/mês
- Estratégia: agrupar todos os tickers em uma única requisição (máximo 50 tickers por chamada)
- Com o agendamento diário (weekdays only), o uso esperado é < 500 requisições/mês

---

### 5. Regras de Clean Architecture — Obrigatórias

1. `MarketDataPort`, `CustodyTickerPort`, `MarketDataEventPublisherPort` → `common/valuation/application/repository/` (interfaces, sem Spring)
2. `SyncMarketDataUseCase` → `common/valuation/application/usecase/` (POJO, sem Spring, sem `@Component`)
3. `BrapiMarketDataAdapter`, `KafkaMarketDataEventPublisher`, `CustodyTickerRepositoryImpl` → `api/valuation/infrastructure/` (implementações com Spring)
4. `MarketDataScheduler` → `api/valuation/infrastructure/spring/` (componente Spring)
5. `MarketDataServiceConfiguration` → `api/valuation/infrastructure/spring/` (wiring com `@Bean`)
6. **NUNCA** importar `org.springframework.*` em classes dentro de `common/application/usecase/` ou `common/application/repository/`

---

### 6. Regras de Logging Obrigatórias (AGENTS.md)

Todo ponto de entrada e saída **deve** ser logado:

```java
// MarketDataScheduler — entrada do scheduler (INFO)
log.info("Starting market data sync at {}", OffsetDateTime.now(ZoneOffset.UTC));

// SyncMarketDataUseCase — falha por ticker (WARN, não ERROR — falha esperada)
log.warn("Failed to fetch market data for ticker {}. Reason: {}", ticker, e.getMessage());

// SyncMarketDataUseCase — ticker inválido (WARN)
log.warn("Ticker {} not found in Brapi. Skipping.", ticker);

// SyncMarketDataUseCase — conclusão (INFO)
log.info("Market data sync completed. Processed: {}, Success: {}, Failed: {}", total, success, failed);

// KafkaMarketDataEventPublisher — entrada do publish (INFO — rastreabilidade Kafka obrigatória)
log.info("Publishing market data event for ticker: {} at {}", event.getTicker(), OffsetDateTime.now(ZoneOffset.UTC));

// BrapiMarketDataAdapter — entrada (INFO)
log.info("Fetching market data from Brapi for tickers: {}", tickers);

// BrapiMarketDataAdapter — falha HTTP (WARN)
log.warn("Brapi API error for tickers {}. Status: {}", tickers, statusCode);
```

---

### 7. Temporal — Regra Crítica (project-context.md)

```java
// ✅ CORRETO — usar ZoneOffset.UTC em todas as operações temporais
OffsetDateTime.now(ZoneOffset.UTC)
LocalDate.now(ZoneOffset.UTC)  // para resultDate do FinancialDataEvent

// ❌ ERRADO — nunca usar LocalDate.now() sem timezone
LocalDate.now()  // BUG — timezone pode variar entre ambientes
```

---

### 8. Padrão de Valores Monetários

```java
// ✅ CORRETO — BigDecimal para preços
BigDecimal price = BigDecimal.valueOf(brapiResult.regularMarketPrice());

// ❌ ERRADO — nunca float/double para monetários
double price = brapiResult.regularMarketPrice(); // viola regra 6 da arquitetura
```

> **Nota**: `FinancialDataEvent` usa `Double` para indicadores fundamentalistas (P/L, DY etc.) — isso é aceitável pois são ratios, não valores monetários. Apenas `price` deve ser tratado como `BigDecimal` na camada de persistência (`StockPriceEntity`).

---

### 9. Dívida Arquitetural do Épico 3 — Impacto nesta história

A retro do Épico 3 identificou acoplamento indevido onde `common` importava de `valuation`:
> "GetCustodyPositionsUseCase no módulo common importa diretamente CompanyRepository e StockPricePort do módulo valuation"

Esta história **NÃO deve replicar este padrão**. O `SyncMarketDataUseCase` vive em `common` e acessa apenas interfaces (`MarketDataPort`, `CustodyTickerPort`) que são implementadas em `api`. O `api` é quem chama o valuation via Kafka — nunca o contrário.

---

### 10. Dependências Maven — Verificar Antes de Adicionar

**NÃO adicionar** `spring-cloud-starter-openfeign` — usar `RestClient` (nativo Spring Boot 3.2+, sem dependências extras).

**Verificar se já existe no `api/pom.xml`:**
- `spring-boot-starter-cache` — necessário para `@Cacheable`
- `caffeine` — necessário para `CaffeineCacheManager`
- `spring-kafka` — já presente (usado no Epic 2/3)

Se `spring-boot-starter-cache` ou `caffeine` não estiverem presentes, adicionar ao `api/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

---

### 11. Isolamento Multi-tenant — Tickers vs Dados de Usuário

O scheduler de Market Data é **agnóstico a usuário** — ele sincroniza dados de mercado globais (preços de ativos), não dados de carteira individual. A query `CustodyTickerPort.findDistinctActiveTickers()` retorna todos os tickers distintos do banco **sem filtro de userId** — isso é correto e intencional.

O isolamento por userId só é necessário nas histórias 4.2 e 4.3 (metas e recomendações individuais).

> **⚠️ Nome real da tabela**: A tabela de operações se chama **`trades`** (entidade `TradeEntity`), localizada em `api/src/.../portfolio/infrastructure/persistence/jpa/entities/TradeEntity.java`. O nome `trade_transaction` **não existe** no schema atual.

---

### 12. Checklist de Segurança — BRAPI_TOKEN

- O token Brapi deve estar em variável de ambiente `BRAPI_TOKEN`, **nunca** hardcoded
- Adicionar ao `.env.local` (desenvolvimento): `BRAPI_TOKEN=seu_token_aqui`
- Adicionar ao `docker-compose.yml` como env var passada ao container `api`
- **NUNCA** logar o token (`log.info("Token: {}", token)` → PROIBIDO)

---

### Project Structure Notes

- **Pacotes backend**: `afsdigital.grahamselect.api.{feature}.{camada}` conforme architecture.md#D12
- **Naming conventions**: `{Verbo}{Substantivo}UseCase`, `{Feature}Repository`, `{Feature}Adapter` — conforme architecture.md#Convenções
- **Tabela `stock_price`**: já existente (criada nas histórias anteriores); `StockPriceEntity` e `StockPriceJpaRepository` já implementados no `valuation-service`
- **Tabela `trades`** (entidade `TradeEntity`): já existente no `api` desde o Épico 2 — usar `TradeJpaRepository` em `api/.../portfolio/infrastructure/persistence/jpa/repository/` para query de tickers distintos
- **`ValuationServiceConfiguration.java`**: NÃO modificar — é do `valuation-service`, que é independente
- **Scheduler**: habilitar `@EnableScheduling` na `ApiServiceApplication.java` se não estiver habilitado
- **Varredura de componentes**: verificar se o package `afsdigital.grahamselect.api.valuation` está coberto pelo `@SpringBootApplication` scan da `ApiServiceApplication`

---

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 4.1]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 4: Market Data & Motor de Recomendação]
- [Source: docs/bmad/planning-artifacts/architecture.md#D9 Tópicos Kafka]
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean Architecture — Padrão do Projeto]
- [Source: docs/bmad/planning-artifacts/architecture.md#D3 Caching]
- [Source: docs/bmad/planning-artifacts/architecture.md#Convenções de Use Cases]
- [Source: docs/bmad/planning-artifacts/architecture.md#Padrões de Naming]
- [Source: docs/bmad/project-context.md#Critical Implementation Rules]
- [Source: docs/bmad/project-context.md#Security Checklist]
- [Source: docs/bmad/implementation-artifacts/epic-3-retro-2026-05-31.md#Dívida Técnica]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/FinancialDataEvent.java]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/CalculationIntrinsicValueUseCase.java]
- [Source: backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/FinancialDataConsumerService.java]
- [Source: backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/StockPriceEntity.java]
- [Source: backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/spring/ValuationServiceConfiguration.java]
- [Source: API Brapi — https://brapi.dev/api/quote/{ticker}?modules=summaryProfile,financialData&token={token}]
- [Source: AGENTS.md — Regras de logging obrigatório]

---

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash

### Debug Log References

N/A

### Completion Notes List

- Implementado `MarketDataPort`, `CustodyTickerPort` e `MarketDataEventPublisherPort` no módulo `common`
- Implementado `SyncMarketDataUseCase` seguindo Clean Architecture, sem dependências Spring
- Criado record `MarketDataResult` para transporte de dados fundamentalistas e cotação
- Adicionado suporte a cache Caffeine em `api/pom.xml`
- Implementado `BrapiMarketDataAdapter` consumindo a API Brapi com RestClient, com timeout de conexão (5s) e leitura (10s) e tratamento de erros 4xx/5xx com fallback automático no cache
- Implementado `CustodyTickerRepositoryImpl` buscando tickers ativos via JPQL do `JpaTradeRepository`
- Implementado `KafkaMarketDataEventPublisher` publicando no tópico `financial-data` usando serialização Jackson
- Criado `MarketDataScheduler` rodando às 20h nos dias de semana (segunda a sexta) para sincronizar dados de mercado de todos os tickers em custódia
- Adicionado testes unitários para o Use Case `SyncMarketDataUseCaseTest`
- Adicionado testes de integração/unitários com `MockRestServiceServer` para o adapter Brapi `BrapiMarketDataAdapterTest`
- Rodado todos os testes do projeto e verificado 100% de sucesso

### File List

- backend/api/pom.xml
- backend/common/src/main/java/afsdigital/grahamselect/valuation/application/dto/MarketDataResult.java
- backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/MarketDataPort.java
- backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/CustodyTickerPort.java
- backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/MarketDataEventPublisherPort.java
- backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/SyncMarketDataUseCase.java
- backend/common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/SyncMarketDataUseCaseTest.java
- backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java
- backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/repositories/JpaTradeRepository.java
- backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/CustodyTickerRepositoryImpl.java
- backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/KafkaMarketDataEventPublisher.java
- backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java
- backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataServiceConfiguration.java
- backend/api/src/main/resources/application.yml
- backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java
