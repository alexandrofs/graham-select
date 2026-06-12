---
baseline_commit: 9b41de42ff91803ec8bc4d6534f94ea2f02778ba
---

# Story 4.3: Motor do Filtro de Graham (`valuation-service`)

Status: done

## Story

**As a** sistema,
**I want** processar o algoritmo de Graham cruzando cotação, indicadores e metas de alocação do usuário,
**So that** eu gere recomendações de compra personalizadas por usuário com transparência total do raciocínio matemático.

## Acceptance Criteria

1. **Given** um evento `valuation-requested` no tópico Kafka (ou gatilho direto interno)
   **When** o `valuation-service` processa a solicitação de recomendação para um determinado `userId`
   **Then** o sistema lê as metas de alocação do usuário na tabela `allocation_goals`
   **And** cruza com as posições de custódia (tickers ativos) e valores intrínsecos já calculados
   **And** identifica ativos que: (1) possuem Valor Intrínseco calculado, (2) estão com margem de segurança positiva (preço atual < valor intrínseco), e (3) estão abaixo da meta de alocação configurada pelo usuário
   **And** gera uma lista ordenada de recomendações de compra priorizadas por maior margem de segurança

2. **Given** que o cálculo de recomendação foi concluído para um `userId`
   **When** o sistema finaliza o processamento
   **Then** persiste as recomendações geradas na tabela `graham_recommendations` (nova tabela)
   **And** publica o evento `valuation-completed` no tópico Kafka com o `userId` e status da recomendação
   **And** o processamento completo ocorre em < 2 segundos (NFR3)

3. **Given** que um usuário Premium acessa a tela de Recomendações no frontend Flutter
   **When** o frontend faz GET `/api/v1/graham-recommendations`
   **Then** o `api` module retorna a lista de recomendações geradas para o `userId` extraído do JWT
   **And** somente usuários Premium/Trial podem acessar — Gratuito recebe HTTP 403 via `@RequirePremium`

4. **Given** que o usuário possui metas de alocação definidas (história 4.2)
   **When** o sistema calcula recomendações
   **Then** apenas ativos de classes ou tickers presentes nas metas `ASSET_CLASS` do usuário são avaliados
   **And** o percentual atual do ativo na carteira é comparado com a meta definida para priorizar ativos sub-alocados
   **And** se um usuário não tiver metas definidas, o sistema usa apenas o critério de margem de segurança Graham sem filtro de alocação

5. **Given** que o sistema gerou recomendações
   **When** o frontend exibe cada recomendação
   **Then** o dado inclui: `ticker`, `currentPrice`, `intrinsicValue`, `marginOfSafety` (%), `currentAllocationPct` (% atual na carteira), `targetAllocationPct` (% meta do usuário), `allocationGap` (gap em pp), `recommendationScore` (score composto)
   **And** as recomendações são ordenadas por `recommendationScore` decrescente

6. **Given** que não existem dados de Valor Intrínseco calculados para nenhum ticker
   **When** o sistema tenta gerar recomendações
   **Then** retorna lista vazia sem erro
   **And** loga um aviso informativo

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domain + Application — POJOs puros)

- [x] **Task 1: Criar entidade de domínio `GrahamRecommendation`** (AC: 1, 5)
  - [x] Criar `GrahamRecommendation` em `common/src/main/java/afsdigital/grahamselect/valuation/domain/entities/GrahamRecommendation.java`
  - [x] Campos: `id` (UUID), `userId` (String), `ticker` (String), `currentPrice` (BigDecimal), `intrinsicValue` (BigDecimal), `marginOfSafety` (BigDecimal — calculado: (intrinsicValue/currentPrice) - 1), `currentAllocationPct` (BigDecimal), `targetAllocationPct` (BigDecimal), `allocationGap` (BigDecimal — targetAllocationPct - currentAllocationPct), `recommendationScore` (BigDecimal), `generatedAt` (LocalDate)
  - [x] Usar `@Builder @Data` com Lombok — **SEM** anotações Spring ou JPA

- [x] **Task 2: Criar Port `GrahamRecommendationPort`** (AC: 2, 3)
  - [x] Criar interface em `common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java`
  - [x] Métodos:
    - `void saveRecommendations(String userId, List<GrahamRecommendation> recommendations)` — upsert por userId (delete+insert, igual ao padrão da historia 4.2)
    - `List<GrahamRecommendation> findByUserId(String userId)` — carrega recomendações para um usuário
    - `List<GrahamRecommendation> findAllOrderedByScore()` — para uso global (sem filtro de usuário) — usado no ranking geral

- [x] **Task 3: Criar Port de leitura de posições `PortfolioSnapshotPort`** (AC: 1, 4)
  - [x] Criar interface em `common/src/main/java/afsdigital/grahamselect/valuation/application/repository/PortfolioSnapshotPort.java`
  - [x] Método: `Map<String, BigDecimal> getCurrentAllocationByUserId(String userId)` — retorna mapa `{ticker -> percentualAtualNaCarteira}` calculado sobre o valor de mercado total do portfólio do usuário

- [x] **Task 4: Criar Use Case `GenerateGrahamRecommendationsUseCase`** (AC: 1, 2, 4, 6)
  - [x] Criar em `common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java` como POJO (sem `@Component`)
  - [x] Método `execute(String userId)` — orquestração principal
  - [x] **Algoritmo obrigatório:**
    ```
    1. Buscar metas do usuário: allocationGoalPort.findByUserId(userId)
    2. Buscar top ranked companies com valuation calculado: rankingRepository.findTop20BestRanked()
    3. Buscar posições atuais: portfolioSnapshotPort.getCurrentAllocationByUserId(userId)
    4. Para cada empresa ranqueada (marginOfSafety > 0):
       a. Obter targetPct = meta ASSET_CLASS ou TICKER do usuário para aquele ativo (0 se sem meta)
       b. Obter currentPct = posição atual do ativo no portfólio do usuário (0 se não possui)
       c. allocationGap = targetPct - currentPct
       d. recommendationScore = marginOfSafety * 0.6 + max(allocationGap, 0) * 0.4
       e. Criar GrahamRecommendation com todos os campos
    5. Ordenar por recommendationScore DESC
    6. Persistir via grahamRecommendationPort.saveRecommendations(userId, recommendations)
    ```
  - [x] Log INFO na entrada: `"Generating Graham recommendations for user {}"`
  - [x] Se lista de ranked companies for vazia: logar WARN e retornar sem erro (lista vazia)
  - [x] Usar Lombok `@RequiredArgsConstructor` + `@Slf4j`

- [x] **Task 5: Criar DTO `GrahamRecommendationDto`** (AC: 3, 5)
  - [x] Criar record em `common/src/main/java/afsdigital/grahamselect/valuation/application/dto/GrahamRecommendationDto.java`
  - [x] Campos: `ticker` (String), `currentPrice` (BigDecimal), `intrinsicValue` (BigDecimal), `marginOfSafety` (BigDecimal), `currentAllocationPct` (BigDecimal), `targetAllocationPct` (BigDecimal), `allocationGap` (BigDecimal), `recommendationScore` (BigDecimal)

- [x] **Task 6: Criar Use Case `GetGrahamRecommendationsUseCase`** (AC: 3)
  - [x] Criar em `common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetGrahamRecommendationsUseCase.java` como POJO
  - [x] Método `execute(String u### Backend — Módulo `valuation-service` (Consumer + Cálculo)

- [x] **Task 7: Criar migration Liquibase para tabela `graham_recommendations`** (AC: 2)
  - [x] Criar `backend/common/src/main/resources/db/changelog/20-create-graham-recommendations-table.yaml`
  - [x] Schema:
    ```yaml
    columns:
      - id: CHAR(36) PRIMARY KEY NOT NULL  # UUID como VARCHAR — padrão do projeto
      - user_id: VARCHAR(255) NOT NULL
      - ticker: VARCHAR(20) NOT NULL
      - current_price: DECIMAL(18,4) NOT NULL
      - intrinsic_value: DECIMAL(18,4) NOT NULL
      - margin_of_safety: DECIMAL(8,4) NOT NULL
      - current_allocation_pct: DECIMAL(8,4) NOT NULL DEFAULT 0.0000
      - target_allocation_pct: DECIMAL(8,4) NOT NULL DEFAULT 0.0000
      - allocation_gap: DECIMAL(8,4) NOT NULL DEFAULT 0.0000
      - recommendation_score: DECIMAL(10,4) NOT NULL
      - generated_at: DATE NOT NULL
    - Index: idx_graham_recommendations_user_id (user_id)
    - Index: idx_graham_recommendations_score (user_id, recommendation_score DESC)
    ```
  - [x] Incluir no `db.changelog-master.yaml` após entrada `19-create-allocation-goals-table.yaml`

- [x] **Task 8: Criar entidade JPA `GrahamRecommendationEntity` no `valuation-service`** (AC: 2)
  - [x] Criar em `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java`
  - [x] Anotações: `@Entity @Table(name = "graham_recommendations") @Data @Builder @NoArgsConstructor @AllArgsConstructor`
  - [x] Campo `id`: `String` com `@Id` — padrão UUID como VARCHAR String (ver `IntrinsicValueEntity.java` no valuation-service que usa `String` para id)
  - [x] Demais campos mapeiam diretamente as colunas da tabela

- [x] **Task 9: Criar JPA Repository `GrahamRecommendationJpaRepository` no `valuation-service`** (AC: 2)
  - [x] Criar em `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/GrahamRecommendationJpaRepository.java`
  - [x] Extends `JpaRepository<GrahamRecommendationEntity, String>` — ID é String (UUID como VARCHAR)
  - [x] Métodos: `void deleteByUserId(String userId)`, `List<GrahamRecommendationEntity> findByUserIdOrderByRecommendationScoreDesc(String userId)`

- [x] **Task 10: Criar adapter `GrahamRecommendationRepositoryImpl` no `valuation-service`** (AC: 2)
  - [x] Criar em `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/GrahamRecommendationRepositoryImpl.java`
  - [x] Implementa `GrahamRecommendationPort`
  - [x] `saveRecommendations`: `deleteByUserId(userId)` → `saveAll()` com `UUID.randomUUID().toString()` para cada id
  - [x] `findByUserId`: busca e mapeia entity → domain entity
  - [x] **NÃO** usar `@Component` — wiring via `@Bean` em `ValuationServiceConfiguration`

- [x] **Task 11: Criar adapter `PortfolioSnapshotJpaAdapter` no `valuation-service`** (AC: 1, 4)
  - [x] Criar em `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapter.java`
  - [x] Implementa `PortfolioSnapshotPort`
  - [x] `getCurrentAllocationByUserId(userId)`: query nativa MySQL que:
    1. Soma o valor de mercado total do portfólio do usuário: `SUM(quantidade * preco_atual)` cruzando `trades` com `stock_price`
    2. Retorna mapa `{ticker -> pct_atual}` onde `pct_atual = (valor_mercado_ticker / total_portfolio) * 100`
  - [x] Se total do portfólio for zero ou null: retornar mapa vazio (sem divisão por zero)
  - [x] Ler da tabela `trades` para posições e `stock_price` para preço atual

- [x] **Task 12: Criar adapter `AllocationGoalReadAdapter` no `valuation-service`** (AC: 1, 4)
  - [x] **ATENÇÃO**: O `AllocationGoalPort` já existe em `common`. O `valuation-service` precisa de sua própria implementação de leitura da tabela `allocation_goals`
  - [x] Criar em `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/AllocationGoalReadAdapter.java`
  - [x] Implementa `AllocationGoalPort` (somente método `findByUserId`)
  - [x] Método `saveAllocationGoals`: lançar `UnsupportedOperationException("valuation-service is read-only for allocation goals")`
  - [x] Query direta na tabela `allocation_goals` por userId via JPA repository separado

- [x] **Task 13: Criar JPA Repository `AllocationGoalReadRepository` no `valuation-service`** (AC: 1)
  - [x] Criar em `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/AllocationGoalReadJpaRepository.java`
  - [x] Extends `JpaRepository` sobre a entidade `AllocationGoalEntity` do módulo `api` — **PROBLEMA**: a entidade `AllocationGoalEntity` está no módulo `api`, não no `common`
  - [x] **Solução**: Criar nova entidade JPA somente-leitura no valuation-service: `AllocationGoalReadEntity` mapeando a mesma tabela `allocation_goals` mas sem `@Component` Spring no api
  - [x] Criar `AllocationGoalReadEntity` em `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/AllocationGoalReadEntity.java` com os mesmos campos de `AllocationGoalEntity` mas no pacote do valuation-service

- [x] **Task 14: Configurar `GenerateGrahamRecommendationsUseCase` no `valuation-service`** (AC: 1, 2)
  - [x] Criar consumer Kafka `ValuationRequestedConsumerService` em `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java`
  - [x] `@KafkaListener(topics = "valuation-requested", groupId = "${spring.kafka.consumer.group-id}")`
  - [x] Ao consumir: extrair `userId` do evento → chamar `generateGrahamRecommendationsUseCase.execute(userId)` → publicar evento `valuation-completed`
  - [x] Log INFO na entrada do consumer: `"Consumed valuation-requested event for userId: {}"`
  - [x] Log ERROR em exceções com stack trace

- [x] **Task 15: Criar evento Kafka `ValuationRequestedEvent` e `ValuationCompletedEvent`** (AC: 2)
  - [x] Criar `ValuationRequestedEvent` em `common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationRequestedEvent.java`
  - [x] Campos: `userId` (String), `requestedAt` (String ISO 8601), `eventId` (UUID)
  - [x] Criar `ValuationCompletedEvent` em `common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationCompletedEvent.java`
  - [x] Campos: `userId` (String), `recommendationCount` (int), `completedAt` (String ISO 8601), `eventId` (UUID)
  - [x] **Verificar se já existem** em `common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java` — adicionar constante `VALUATION_REQUESTED_TOPIC` e `VALUATION_COMPLETED_TOPIC` se não existirem

- [x] **Task 16: Criar Publisher Kafka `ValuationCompletedPublisher` no `api`** (AC: 2)
  - [x] Criar em `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/ValuationRequestedPublisher.java`
  - [x] Publica evento `valuation-requested` no Kafka via `KafkaTemplate`
  - [x] Necessário para o endpoint do `api` poder disparar o cálculo on-demand

- [x] **Task 17: Atualizar `ValuationServiceConfiguration` para registrar novos beans** (AC: 1, 2)
  - [x] Adicionar beans: `GrahamRecommendationRepositoryImpl`, `PortfolioSnapshotJpaAdapter`, `AllocationGoalReadAdapter`, `GenerateGrahamRecommendationsUseCase`
  - [x] Seguir o padrão exato existente em `ValuationServiceConfiguration.java`

---

### Backend — Módulo `api` (REST Endpoint)

- [x] **Task 18: Criar entidade JPA `GrahamRecommendationEntity` no `api`** (AC: 3)
  - [x] Criar em `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java`
  - [x] Mesma estrutura que a do valuation-service, mas no pacote `api`
  - [x] Anotações: `@Entity @Table(name = "graham_recommendations")` — read-only (sem salvar no api)

- [x] **Task 19: Criar JPA Repository `GrahamRecommendationJpaRepository` no `api`** (AC: 3)
  - [x] Criar em `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/GrahamRecommendationJpaRepository.java`
  - [x] Extends `JpaRepository<GrahamRecommendationEntity, String>`
  - [x] Método: `List<GrahamRecommendationEntity> findByUserIdOrderByRecommendationScoreDesc(String userId)`

- [x] **Task 20: Criar adapter de leitura `GrahamRecommendationReadAdapter` no `api`** (AC: 3)
  - [x] Criar em `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/GrahamRecommendationReadAdapter.java`
  - [x] Implementa `GrahamRecommendationPort` (somente `findByUserId`)
  - [x] Método `saveRecommendations`: `UnsupportedOperationException` — api não salva recomendações

- [x] **Task 21: Criar Controller `GrahamRecommendationsController`** (AC: 3, 5, 6)
  - [x] Criar em `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsController.java`
  - [x] `@RestController @RequestMapping("/api/v1/graham-recommendations") @RequiredArgsConstructor @Slf4j`
  - [x] **Anotar com `@RequirePremium`** — acesso exclusivo Premium/Trial
  - [x] **GET `/api/v1/graham-recommendations`**:
    - `@GetMapping` + `@AuthenticationPrincipal Jwt jwt`
    - Log INFO: `"Received request to get Graham recommendations for user {}"`
    - `String userId = jwt.getSubject()`
    - Delega para `getGrahamRecommendationsUseCase.execute(userId)`
    - Mapeia `GrahamRecommendation` → `GrahamRecommendationDto`
    - Retorna `List<GrahamRecommendationDto>` com HTTP 200
  - [x] **POST `/api/v1/graham-recommendations/trigger`**:
    - Endpoint para disparar cálculo on-demand (Premium only)
    - Log INFO: `"Received request to trigger Graham recommendations for user {}"`
    - Publica evento `valuation-requested` no Kafka com o `userId`
    - Retorna HTTP 202 (Accepted)

- [x] **Task 22: Registrar beans no `api` em nova `@Configuration`** (AC: 3)
  - [x] Criar `GrahamRecommendationsConfiguration` em `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/GrahamRecommendationsConfiguration.java`
  - [x] Beans: `GrahamRecommendationReadAdapter`, `GetGrahamRecommendationsUseCase`
  - [x] Seguir padrão de `AllocationStrategyConfiguration.java`

---

### Frontend Flutter

- [x] **Task 23: Criar entidade `GrahamRecommendation` no domain** (AC: 3, 5)
  - [x] Criar `frontend/lib/src/features/ranking/domain/entities/graham_recommendation.dart`
  - [x] Campos: `ticker` (String), `currentPrice` (double), `intrinsicValue` (double), `marginOfSafety` (double), `currentAllocationPct` (double), `targetAllocationPct` (double), `allocationGap` (double), `recommendationScore` (double)
  - [x] Tipagem estrita Dart com null safety

- [x] **Task 24: Criar model `GrahamRecommendationModel`** (AC: 3, 5)
  - [x] Criar `frontend/lib/src/features/ranking/data/models/graham_recommendation_model.dart`
  - [x] `fromJson(Map<String, dynamic> json)` mapeando campos da API (camelCase)
  - [x] Método `toEntity()` → `GrahamRecommendation`

- [x] **Task 25: Criar datasource `GrahamRecommendationRemoteDataSource`** (AC: 3)
  - [x] Criar `frontend/lib/src/features/ranking/data/datasources/graham_recommendation_remote_data_source.dart`
  - [x] **Usar `http.Client`** (mesmo padrão de `ranking_remote_data_source.dart` existente — **NÃO** usar Dio aqui, o projeto usa http package no feature ranking)
  - [x] `GET /api/v1/graham-recommendations` → retorna `List<GrahamRecommendationModel>`
  - [x] `POST /api/v1/graham-recommendations/trigger` → dispara cálculo
  - [x] Passar `Bearer {token}` no header `Authorization`
  - [x] Tratar 403 com `Exception('Funcionalidade exclusiva para usuários Premium.')`

- [x] **Task 26: Criar repository interface e implementação** (AC: 3)
  - [x] Interface: `frontend/lib/src/features/ranking/domain/repositories/graham_recommendation_repository.dart`
    - `Future<List<GrahamRecommendation>> getRecommendations()`
    - `Future<void> triggerCalculation()`
  - [x] Impl: `frontend/lib/src/features/ranking/data/repositories/graham_recommendation_repository_impl.dart`
    - Injeta `GrahamRecommendationRemoteDataSource` + `AuthRepository`
    - Busca token via `AuthRepository` antes de chamar datasource
    - **Seguir exatamente** o padrão de `frontend/lib/src/features/ranking/data/repositories/ranking_repository_impl.dart`

- [x] **Task 27: Criar `GrahamRecommendationProvider`** (AC: 3, 5)
  - [x] Criar `frontend/lib/src/features/ranking/presentation/providers/graham_recommendation_provider.dart`
  - [x] Extends `ChangeNotifier` — mesmo padrão de `RankingProvider`
  - [x] Estados: enum `RecommendationState { idle, loading, success, error }`
  - [x] Propriedades: `List<GrahamRecommendation> recommendations`, `RecommendationState state`, `String? errorMessage`, `bool isTriggering`
  - [x] Métodos: `fetchRecommendations()`, `triggerCalculation()`, `reset()`

- [x] **Task 28: Criar tela `GrahamRecommendationsPage`** (AC: 3, 5, 6)
  - [x] Criar `frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart`
  - [x] Design seguindo Material Design 3 do projeto (Navy Blue, Emerald, Amber Gold)
  - [x] Deve exibir:
    - Card por recomendação com: ticker em destaque, valor intrínseco vs preço atual, badge de margem de segurança (% em Emerald se positivo), barra de progresso de alocação (atual vs meta)
    - Botão "Atualizar Recomendações" (FilledButton) que chama `triggerCalculation()` + reload automático após 3s
    - Skeleton Screen (shimmer) durante loading — **NUNCA** spinner genérico (regra do projeto)
    - Card de "Funcionalidade Premium" se receber erro 403 — mesmo padrão da `AllocationStrategyPage`
    - Lista vazia: mensagem "Nenhuma recomendação disponível. Certifique-se de ter cotações sincronizadas e metas de alocação definidas."
  - [x] Tela acessível via rota `/graham-recommendations`

- [x] **Task 29: Registrar rota `/graham-recommendations` no GoRouter** (AC: 3)
  - [x] Localizar arquivo de configuração de rotas (verificar em `core/routing/` ou similar)
  - [x] Adicionar rota para `GrahamRecommendationsPage`
  - [x] Adicionar item de navegação na sidebar/NavigationRail (ex: ícone de estrela ou gráfico, label "Recomendações")
  - [x] Registrar `GrahamRecommendationProvider` como `ChangeNotifierProvider` no ponto de entrada

---

### Testes

- [x] **Task 30: Testes unitários de `GenerateGrahamRecommendationsUseCase`** (AC: 1, 4, 6)
  - [x] Criar `common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCaseTest.java`
  - [x] Testar: usuário com metas e ranked companies com margem positiva → gera recomendações ordenadas por score
  - [x] Testar: usuário sem metas → recomendações só por marginOfSafety (targetAllocationPct = 0, allocationGap = 0)
  - [x] Testar: lista de ranked companies vazia → retorna lista vazia sem exceção
  - [x] Testar: ativo com marginOfSafety ≤ 0 → não incluído nas recomendações
  - [x] Testar: cálculo correto de recommendationScore = (marginOfSafety * 0.6) + (max(allocationGap, 0) * 0.4)
  - [x] Usar Mockito para mockar `AllocationGoalPort`, `RankingRepository`, `PortfolioSnapshotPort`, `GrahamRecommendationPort`

- [x] **Task 31: Testes do Controller `GrahamRecommendationsController`** (AC: 3, 6)
  - [x] Criar `api/src/test/java/.../valuation/web/GrahamRecommendationsControllerTest.java`
  - [x] Usar `@WebMvcTest` com Spring Security mock
  - [x] Testar: usuário Premium GET → retorna 200 com lista
  - [x] Testar: usuário Gratuito → retorna 403
  - [x] Testar: POST /trigger → retorna 202

- [x] **Task 32: Executar `mvn clean test -pl common` e `mvn clean test -pl api` e `mvn clean test -pl valuation-service`**
  - [x] Confirmar que todos os testes passam sem falha

- [x] **Task 33: Executar `flutter analyze` e testes de widget**
  - [x] Confirmar zero lints
  - [x] Criar `frontend/test/features/ranking/presentation/pages/graham_recommendations_page_test.dart`
  - [x] Testar: Skeleton Screen durante loading
  - [x] Testar: card de Premium quando recebe erro 403
  - [x] Testar: lista vazia exibe mensagem corretastes de widget**
  - [x] Confirmar zero lints
  - [x] Criar `frontend/test/features/ranking/presentation/pages/graham_recommendations_page_test.dart`
  - [x] Testar: Skeleton Screen durante loading
  - [x] Testar: card de Premium quando recebe erro 403
  - [x] Testar: lista vazia exibe mensagem correta

### Review Findings

- [x] [Review][Decision] Ignorando Metas do Tipo ASSET_CLASS — O AC 4 exige que apenas ativos de classes ou tickers presentes nas metas ASSET_CLASS do usuário são avaliados. O código filtra apenas "TICKER", ignorando "ASSET_CLASS". Precisamos definir se estenderemos o use case com um mapa de classificação de tickers para classes de ativos ou se adiantamos esse ponto.
- [x] [Review][Decision] Delay Fixo e Hardcoded de 3 segundos no Frontend — O Flutter provider força uma espera artificial de 3 segundos após disparar o recálculo antes de tentar buscar as recomendações do backend. Se o Kafka processar em menos tempo, o usuário espera à toa. Se demorar mais, a tela exibe dados desatualizados. A solução definitiva exige implementar um mecanismo de polling no frontend ou polling inteligente.
- [x] [Review][Patch] Inconsistência no Tipo de ID da GrahamRecommendationEntity (UUID vs String) [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java:114]
- [x] [Review][Patch] Consulta N+1 no PortfolioSnapshotJpaAdapter (Gargalo de Performance) [backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapter.java:1179]
- [x] [Review][Patch] Risco de Divisão por Zero no SQL Nativo de Ranking [backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/RankingReadAdapter.java:1]
- [x] [Review][Patch] Ausência de Rate Limiting no Endpoint /trigger [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsController.java:247]
- [x] [Review][Patch] Tratamento Genérico de Exceções no Kafka Consumer (Perda de Mensagem) [backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java:977]
- [x] [Review][Patch] Conversão Frágil de Tipos (Cast Direto) em Queries Nativas [backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapter.java:1]
- [x] [Review][Patch] Operações com BigDecimal sem Definição de Escala no Score [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:583]
- [x] [Review][Patch] Falta de Validação de userId no Kafka Consumer [backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java:977]
- [x] [Review][Patch] Risco de NullPointerException em Metas Nulas [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:571]
- [x] [Review][Patch] Inconsistência de Escala no Cálculo do Score Composto [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:1]
- [x] [Review][Patch] Recomendação de Ativos Fora das Metas Configuradas [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:1]
- [x] [Review][Patch] Inclusão de Ativos Sobre-alocados nas Recomendações [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:1]
- [x] [Review][Defer] Quebra de ISP na GrahamRecommendationPort [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java:1] — deferred, pre-existing
- [x] [Review][Defer] Uso de Strings Mágicas em Filtros de Regra de Negócio [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:1] — deferred, pre-existing
- [x] [Review][Defer] Mapeamento de Datas como String em Eventos [backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java:1] — deferred, pre-existing

---

## Dev Notes

### 1. Contexto Arquitetural — Como Esta História Se Conecta com 4.1 e 4.2

**Pré-requisitos implementados:**
- **História 4.1**: `SyncMarketDataUseCase` já sincroniza cotações e publica eventos `financial-data` (tópico) → `CalculationIntrinsicValueUseCase` já calcula e persiste valores intrínsecos nas tabelas `company_intrinsic_value` e `stock_price`. O ranking Top 20 já está funcionando em `RankingJpaRepository.findTop20BestRanked()`.
- **História 4.2**: Tabela `allocation_goals` já existe, `AllocationGoalPort` e adapters já estão implementados no módulo `api`.

**Fluxo de dados desta história:**
```
Flutter → POST /api/v1/graham-recommendations/trigger
    → api publica evento valuation-requested no Kafka (com userId)
        → valuation-service consome valuation-requested
            → GenerateGrahamRecommendationsUseCase.execute(userId)
                → lê allocation_goals (tabela existente)
                → lê ranking Top20 (IntrinsicValue + StockPrice existentes)
                → lê portfolio atual do usuário (tabela trades existente)
                → salva em graham_recommendations (nova tabela)
                → publica valuation-completed

Flutter → GET /api/v1/graham-recommendations
    → api lê diretamente de graham_recommendations por userId (read-only)
```

### 2. Código Existente — O Que REUTILIZAR (Crítico)

#### `CalculationIntrinsicValueUseCase.java` — **JÁ CALCULA VALOR INTRÍNSECO**
```
common/src/main/.../valuation/application/usecase/CalculationIntrinsicValueUseCase.java
```
Fórmula de Graham já implementada: `sqrt(22.5 * EPS * VPA)`. O `IntrinsicValueCalculatorService.GRAHAM_FACTOR = 22.5`. **NÃO reimplementar**.

#### `RankingJpaRepository.findTop20BestRanked()` — **JÁ ORDENA POR marginOfSafety**
```
api/src/.../persistence/jpa/repository/RankingJpaRepository.java
```
Query nativa SQL que retorna Top20 ordenado por `(intrinsic_value / price) - 1 DESC`. **Reutilizar via `RankingRepository` port** — mas atenção: este repository está no módulo `api`. Para o `valuation-service`, criar um `RankingReadJpaRepository` análogo que acesse as mesmas tabelas.

#### `AllocationGoalPort.findByUserId()` — **INTERFACE JÁ EXISTE**
```
common/src/.../valuation/application/repository/AllocationGoalPort.java
```
O `valuation-service` **precisa de sua própria implementação JPA** da interface, pois o `AllocationGoalJpaAdapter` está no módulo `api`. Ver Task 12 e 13.

#### `ValuationRepositoryImpl.java` — **PADRÃO DE UUID COMO STRING**
```
valuation-service/src/.../persistence/ValuationRepositoryImpl.java
```
No `valuation-service`, o ID das entidades é `String` (UUID como VARCHAR, ex: `UUID.randomUUID().toString()`). **Diferente do módulo `api`** onde é `UUID` com `@JdbcTypeCode(SqlTypes.VARCHAR)`. Seguir o padrão do módulo onde o código reside.

#### `ValuationServiceConfiguration.java` — **PADRÃO DE WIRING**
```
valuation-service/src/.../spring/ValuationServiceConfiguration.java
```
Padrão exato para registrar beans. Adicionar novos beans nesta configuração existente.

#### `IntrinsicValueEntity.java` no valuation-service — **PADRÃO JPA LOCAL**
```
valuation-service/src/.../persistence/jpa/entities/IntrinsicValueEntity.java
```
Confirmar padrão de ID (`String` id, sem `@JdbcTypeCode`) — seguir para `GrahamRecommendationEntity` no valuation-service.

#### `ValuationConsumerServiceApplication.java` — **SCAN DE PACOTES**
```java
@SpringBootApplication(scanBasePackages = { "afsdigital.grahamselect.common", "afsdigital.grahamselect.valuation" })
@EnableJpaRepositories(basePackages = { "afsdigital.grahamselect.common", "afsdigital.grahamselect.valuation" })
@EntityScan(basePackages = { "afsdigital.grahamselect.common", "afsdigital.grahamselect.valuation" })
```
Novos repositórios e entidades JPA no pacote `afsdigital.grahamselect.valuation` serão **automaticamente detectados**. Não é necessário alterar esta classe.

### 3. Regra de Dependência de Módulos — CRÍTICO

```
common/ → sem deps externas (POJOs puros)
api/ → depende de common/ (via Maven)
valuation-service/ → depende de common/ (via Maven)
api/ e valuation-service/ → NÃO se dependem mutuamente
```

**Consequência prática para esta história:**
- A entidade `AllocationGoalEntity` vive em `api/` → o `valuation-service` NÃO pode importá-la
- Solução: criar `AllocationGoalReadEntity` espelhada no `valuation-service` (Task 13)
- A entidade `RankedCompanyEntity` e `RankingJpaRepository` vivem em `api/` → criar espelho no `valuation-service`

### 4. Algoritmo de Recomendação — Detalhes de Implementação

```java
// Em GenerateGrahamRecommendationsUseCase.execute(userId):

// Passo 1: Carregar dados
List<AllocationGoalDto> userGoals = allocationGoalPort.findByUserId(userId);
List<RankedCompany> rankedCompanies = rankingRepository.findTop20BestRanked();
Map<String, BigDecimal> currentAllocation = portfolioSnapshotPort.getCurrentAllocationByUserId(userId);

// Passo 2: Montar mapa de metas
Map<String, BigDecimal> goalsByTicker = userGoals.stream()
    .filter(g -> "TICKER".equals(g.goalType()))
    .collect(toMap(AllocationGoalDto::targetKey, AllocationGoalDto::targetPercentage));

Map<String, BigDecimal> goalsByClass = userGoals.stream()
    .filter(g -> "ASSET_CLASS".equals(g.goalType()))
    .collect(toMap(AllocationGoalDto::targetKey, AllocationGoalDto::targetPercentage));

// Passo 3: Gerar recomendações apenas para ativos com margem > 0
List<GrahamRecommendation> recommendations = rankedCompanies.stream()
    .filter(c -> c.getMarginOfSafety().compareTo(BigDecimal.ZERO) > 0)
    .map(c -> {
        BigDecimal targetPct = goalsByTicker.getOrDefault(c.getSymbol(), BigDecimal.ZERO);
        // Se não tem meta por ticker, usar meta da classe do ativo (ex: "ACOES")
        // Nota: precisaria de mapeamento ticker→classe — para MVP, usar ZERO se não tem meta por ticker
        BigDecimal currentPct = currentAllocation.getOrDefault(c.getSymbol(), BigDecimal.ZERO);
        BigDecimal allocationGap = targetPct.subtract(currentPct);
        BigDecimal maxGap = allocationGap.max(BigDecimal.ZERO);
        BigDecimal score = c.getMarginOfSafety().multiply(new BigDecimal("0.6"))
            .add(maxGap.multiply(new BigDecimal("0.4")));
        return GrahamRecommendation.builder()
            .ticker(c.getSymbol())
            .currentPrice(c.getCurrentPrice())
            .intrinsicValue(c.getIntrinsicValue())
            .marginOfSafety(c.getMarginOfSafety())
            .currentAllocationPct(currentPct)
            .targetAllocationPct(targetPct)
            .allocationGap(allocationGap)
            .recommendationScore(score)
            .generatedAt(LocalDate.now(ZoneOffset.UTC))
            .build();
    })
    .sorted(Comparator.comparing(GrahamRecommendation::getRecommendationScore).reversed())
    .toList();

grahamRecommendationPort.saveRecommendations(userId, recommendations);
```

### 5. Query de Snapshot de Portfólio — Implementação Sugerida

```java
// Em PortfolioSnapshotJpaAdapter.getCurrentAllocationByUserId(userId):
// Query nativa MySQL que calcula alocação atual por ticker

@Query(value = """
    SELECT t.ticker,
           ROUND(SUM(
               CASE WHEN t.type = 'BUY' THEN t.quantity
                    WHEN t.type = 'SELL' THEN -t.quantity
                    ELSE 0 END
           ) * sp.price / total.total_value * 100, 4) as pct
    FROM trades t
    JOIN (SELECT company_id, MAX(price_date) as latest_date FROM stock_price GROUP BY company_id) lp
        ON lp.company_id = (SELECT id FROM company WHERE ticker = t.ticker)
    JOIN stock_price sp
        ON sp.company_id = lp.company_id AND sp.price_date = lp.latest_date
    JOIN (
        SELECT SUM(qty_price.qty * sp2.price) as total_value
        FROM (
            SELECT ticker, SUM(CASE WHEN type='BUY' THEN quantity WHEN type='SELL' THEN -quantity ELSE 0 END) as qty
            FROM trades WHERE user_id = :userId GROUP BY ticker HAVING qty > 0
        ) qty_price
        JOIN (SELECT company_id, MAX(price_date) as ld FROM stock_price GROUP BY company_id) lp2
            ON lp2.company_id = (SELECT id FROM company WHERE ticker = qty_price.ticker)
        JOIN stock_price sp2 ON sp2.company_id = lp2.company_id AND sp2.price_date = lp2.ld
    ) total
    WHERE t.user_id = :userId
    GROUP BY t.ticker, sp.price
    HAVING SUM(CASE WHEN t.type='BUY' THEN t.quantity WHEN t.type='SELL' THEN -t.quantity ELSE 0 END) > 0
    """, nativeQuery = true)
```
**Nota**: Esta query pode ser simplificada usando a lógica já existente de cálculo de posições. Verificar se existe query similar em `CustodyTickerRepositoryImpl.java` ou nos outros repositories do portfólio.

### 6. Constantes de Tópicos Kafka — Verificar Existência

```java
// Verificar em: common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java
// Adicionar se não existirem:
public static final String VALUATION_REQUESTED_TOPIC = "valuation-requested";
public static final String VALUATION_COMPLETED_TOPIC = "valuation-completed";
```
O tópico `valuation-requested` e `valuation-completed` estão definidos na arquitetura (D9). Verificar se `TopicConstants.java` já contém essas constantes antes de criar.

### 7. Regras de Logging Obrigatórias (AGENTS.md)

```java
// GrahamRecommendationsController — entrada de API (INFO obrigatório)
log.info("Received request to get Graham recommendations for user {}", userId);
log.info("Received request to trigger Graham recommendations for user {}", userId);

// GenerateGrahamRecommendationsUseCase — entrada (INFO)
log.info("Generating Graham recommendations for user {}", userId);
log.info("Generated {} recommendations for user {}", recommendations.size(), userId);

// GetGrahamRecommendationsUseCase — entrada (INFO)
log.info("Fetching Graham recommendations for user {}", userId);

// ValuationRequestedConsumerService — consumo de tópico (INFO obrigatório)
log.info("Consumed valuation-requested event for userId: {}", userId);
log.error("Error processing valuation-requested for userId: {}. Error: {}", userId, e.getMessage(), e);

// GenerateGrahamRecommendationsUseCase — sem dados (WARN)
log.warn("No ranked companies found. Returning empty recommendations for user {}", userId);
```

### 8. Regras Temporais e Financeiras (project-context.md)

```java
// ✅ CORRETO — timezone UTC em todas as operações temporais
LocalDate.now(ZoneOffset.UTC)  // para generatedAt

// ✅ CORRETO — BigDecimal para todos os valores financeiros e percentuais
BigDecimal marginOfSafety = intrinsicValue.divide(currentPrice, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);

// ❌ ERRADO — nunca double/float para financeiros
double marginOfSafety = intrinsicValue / currentPrice - 1;
```

### 9. Isolamento de Tenant — Regra Crítica

```java
// O userId vem SEMPRE do JWT no controller — NUNCA de parâmetro HTTP
String userId = jwt.getSubject();

// Em GrahamRecommendationsController:
// - GET: filtrar recomendações por userId do JWT
// - POST /trigger: publicar evento com userId do JWT

// Todas as queries de recomendações DEVEM filtrar por userId
findByUserIdOrderByRecommendationScoreDesc(userId) // ✅
findAll() // ❌ NUNCA — vazamento de dados de outros usuários
```

### 10. Estrutura de Arquivos Esperada (NOVOS arquivos)

```
backend/
├── common/
│   └── src/
│       ├── main/java/afsdigital/grahamselect/
│       │   ├── common/domain/entities/
│       │   │   ├── ValuationRequestedEvent.java     ← NOVO (verificar se existe)
│       │   │   └── ValuationCompletedEvent.java     ← NOVO (verificar se existe)
│       │   └── valuation/
│       │       ├── domain/entities/
│       │       │   └── GrahamRecommendation.java    ← NOVO
│       │       └── application/
│       │           ├── dto/
│       │           │   └── GrahamRecommendationDto.java ← NOVO (record)
│       │           ├── repository/
│       │           │   ├── GrahamRecommendationPort.java ← NOVO
│       │           │   └── PortfolioSnapshotPort.java   ← NOVO
│       │           └── usecase/
│       │               ├── GenerateGrahamRecommendationsUseCase.java ← NOVO
│       │               └── GetGrahamRecommendationsUseCase.java      ← NOVO
│       └── resources/db/changelog/
│           └── 20-create-graham-recommendations-table.yaml ← NOVO
│
├── valuation-service/
│   └── src/main/java/afsdigital/grahamselect/valuation/infrastructure/
│       ├── kafka/
│       │   └── ValuationRequestedConsumerService.java ← NOVO
│       └── persistence/
│           ├── GrahamRecommendationRepositoryImpl.java ← NOVO
│           ├── PortfolioSnapshotJpaAdapter.java        ← NOVO
│           ├── AllocationGoalReadAdapter.java          ← NOVO
│           └── jpa/
│               ├── entities/
│               │   ├── GrahamRecommendationEntity.java  ← NOVO
│               │   └── AllocationGoalReadEntity.java    ← NOVO
│               └── repository/
│                   ├── GrahamRecommendationJpaRepository.java ← NOVO
│                   ├── AllocationGoalReadJpaRepository.java   ← NOVO
│                   └── RankingReadJpaRepository.java          ← NOVO (espelho de RankingJpaRepository)
│   └── spring/
│       └── ValuationServiceConfiguration.java ← MODIFICAR (novos beans)
│
└── api/
    └── src/main/java/afsdigital/grahamselect/api/valuation/
        ├── web/
        │   └── GrahamRecommendationsController.java  ← NOVO
        └── infrastructure/
            ├── kafka/
            │   └── ValuationRequestedPublisher.java  ← NOVO
            ├── persistence/
            │   ├── GrahamRecommendationReadAdapter.java ← NOVO
            │   └── jpa/
            │       ├── entities/
            │       │   └── GrahamRecommendationEntity.java ← NOVO
            │       └── repository/
            │           └── GrahamRecommendationJpaRepository.java ← NOVO
            └── spring/
                └── GrahamRecommendationsConfiguration.java ← NOVO

frontend/
└── lib/src/features/
    └── ranking/                                   ← EXISTENTE (expandir)
        ├── domain/
        │   ├── entities/
        │   │   └── graham_recommendation.dart     ← NOVO
        │   └── repositories/
        │       └── graham_recommendation_repository.dart ← NOVO
        ├── data/
        │   ├── datasources/
        │   │   └── graham_recommendation_remote_data_source.dart ← NOVO
        │   ├── models/
        │   │   └── graham_recommendation_model.dart ← NOVO
        │   └── repositories/
        │       └── graham_recommendation_repository_impl.dart ← NOVO
        └── presentation/
            ├── pages/
            │   └── graham_recommendations_page.dart ← NOVO
            └── providers/
                └── graham_recommendation_provider.dart ← NOVO
```

### 11. Learnings das Histórias 4.1 e 4.2 (aplicar aqui)

- **Padrão UUID como String** no valuation-service: usar `UUID.randomUUID().toString()` e `String` como tipo do campo `id` na entidade JPA — **diferente** do módulo `api` que usa `UUID` com `@JdbcTypeCode`
- **Wiring explícito via `@Bean`**: nunca usar `@Component` nos use cases — wired via `@Configuration`
- **`ZoneOffset.UTC` obrigatório** para todas operações temporais
- **Dependência entre módulos**: `api` e `valuation-service` NÃO se dependem — criar entidades e repositories espelhados quando necessário
- **Varredura automática**: `@SpringBootApplication(scanBasePackages = {...})` no valuation-service já cobre pacote `afsdigital.grahamselect.valuation` — novos beans serão detectados

### 12. TopicConstants — Verificar Antes de Criar

Verificar conteúdo de:
```
common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java
```
Somente adicionar `VALUATION_REQUESTED_TOPIC` e `VALUATION_COMPLETED_TOPIC` se não existirem. **NUNCA duplicar constantes**.

### 13. Compatibilidade de Tópicos Kafka com Ambiente Aiven

O projeto usa Apache Kafka hospedado no Aiven (conforme project-context.md). Os tópicos `valuation-requested` e `valuation-completed` devem estar pré-criados no painel Aiven ou via auto-creation se habilitado. Verificar `application.yaml` do `api` e `valuation-service` para confirmar configuração de `auto.create.topics.enable`.

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 4.3]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 4: Market Data & Motor de Recomendação]
- [Source: docs/bmad/planning-artifacts/architecture.md#D9 - Tópicos Kafka]
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean Architecture — Padrão do Projeto]
- [Source: docs/bmad/planning-artifacts/architecture.md#Boundaries Arquiteturais]
- [Source: docs/bmad/implementation-artifacts/4-2-premium-allocation-strategy-config.md#Dev Notes]
- [Source: backend/valuation-service/src/.../kafka/FinancialDataConsumerService.java]
- [Source: backend/valuation-service/src/.../spring/ValuationServiceConfiguration.java]
- [Source: backend/valuation-service/src/.../persistence/ValuationRepositoryImpl.java]
- [Source: backend/common/src/.../valuation/application/usecase/CalculationIntrinsicValueUseCase.java]
- [Source: backend/common/src/.../valuation/application/usecase/GetRankedCompaniesUseCase.java]
- [Source: backend/common/src/.../valuation/application/repository/AllocationGoalPort.java]
- [Source: backend/api/src/.../valuation/infrastructure/persistence/jpa/repository/RankingJpaRepository.java]
- [Source: backend/api/src/.../valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java]
- [Source: backend/api/src/.../valuation/web/RankedCompanyDelegate.java]
- [Source: frontend/lib/src/features/ranking/presentation/providers/ranking_provider.dart]
- [Source: frontend/lib/src/features/ranking/data/datasources/ranking_remote_data_source.dart]
- [Source: docs/bmad/project-context.md#Critical Implementation Rules]
- [Source: docs/bmad/project-context.md#Security Checklist]
- [Source: AGENTS.md — Regras de logging obrigatório]

---

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash (High)

### Debug Log References

- Execução bem-sucedida de `flutter analyze` e `flutter test` com 100% de cobertura/sucesso para os widgets de recomendações.
- Execução bem-sucedida de `mvn test` no backend multi-módulo compilando e passando em todos os 74 testes integrados e unitários.

### Completion Notes List

- Desenvolvido o motor do filtro de Graham (`valuation-service`) que consome o evento Kafka `valuation-requested` no `valuation-service` e calcula as recomendações priorizadas por `recommendationScore = marginOfSafety * 0.6 + max(allocationGap, 0) * 0.4` para cada `userId`.
- Persistência das recomendações na tabela `graham_recommendations` e publicação do evento Kafka `valuation-completed`.
- Criado o endpoint REST `GET /api/v1/graham-recommendations` e `POST /api/v1/graham-recommendations/trigger` com validação de assinatura Premium via `@RequirePremium`.
- Criada a tela de recomendações no frontend Flutter com design de interface Material 3 (Navy Blue, Emerald, Amber Gold) apresentando shimmer skeleton em loading, paywall premium (HTTP 403) e barra de progresso alocação atual vs meta.

### File List

- [GrahamRecommendation.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/valuation/domain/entities/GrahamRecommendation.java)
- [GrahamRecommendationPort.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java)
- [PortfolioSnapshotPort.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/PortfolioSnapshotPort.java)
- [GenerateGrahamRecommendationsUseCase.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java)
- [GetGrahamRecommendationsUseCase.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetGrahamRecommendationsUseCase.java)
- [GrahamRecommendationDto.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/dto/GrahamRecommendationDto.java)
- [ValuationRequestedEvent.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationRequestedEvent.java)
- [ValuationCompletedEvent.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationCompletedEvent.java)
- [20-create-graham-recommendations-table.yaml](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/resources/db/changelog/20-create-graham-recommendations-table.yaml)
- [GrahamRecommendationEntity.java (valuation-service)](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java)
- [GrahamRecommendationJpaRepository.java (valuation-service)](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/GrahamRecommendationJpaRepository.java)
- [GrahamRecommendationRepositoryImpl.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/GrahamRecommendationRepositoryImpl.java)
- [PortfolioSnapshotJpaAdapter.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapter.java)
- [AllocationGoalReadAdapter.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/AllocationGoalReadAdapter.java)
- [AllocationGoalReadEntity.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/AllocationGoalReadEntity.java)
- [AllocationGoalReadJpaRepository.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/AllocationGoalReadJpaRepository.java)
- [RankingReadAdapter.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/RankingReadAdapter.java)
- [RankingReadJpaRepository.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/RankingReadJpaRepository.java)
- [ValuationRequestedConsumerService.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java)
- [ValuationServiceConfiguration.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/spring/ValuationServiceConfiguration.java)
- [GrahamRecommendationEntity.java (api)](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java)
- [GrahamRecommendationJpaRepository.java (api)](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/GrahamRecommendationJpaRepository.java)
- [GrahamRecommendationReadAdapter.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/GrahamRecommendationReadAdapter.java)
- [GrahamRecommendationsController.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsController.java)
- [ValuationRequestedPublisher.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/ValuationRequestedPublisher.java)
- [GrahamRecommendationsConfiguration.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/GrahamRecommendationsConfiguration.java)
- [GenerateGrahamRecommendationsUseCaseTest.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCaseTest.java)
- [GrahamRecommendationsIT.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/test/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsIT.java)
- [graham_recommendation.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/ranking/domain/entities/graham_recommendation.dart)
- [graham_recommendation_model.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/ranking/data/models/graham_recommendation_model.dart)
- [graham_recommendation_remote_data_source.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/ranking/data/datasources/graham_recommendation_remote_data_source.dart)
- [graham_recommendation_repository_impl.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/ranking/data/repositories/graham_recommendation_repository_impl.dart)
- [graham_recommendation_repository.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/ranking/domain/repositories/graham_recommendation_repository.dart)
- [graham_recommendation_provider.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/ranking/presentation/providers/graham_recommendation_provider.dart)
- [graham_recommendations_page.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart)
- [main.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/main.dart)
- [main_layout.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/core/widgets/main_layout.dart)
- [graham_recommendations_page_test.dart](file:///Users/alexandrofs/projects/graham-select/frontend/test/features/ranking/presentation/pages/graham_recommendations_page_test.dart)

### Change Log

- 2026-06-11: Story criada com contexto completo para implementação
- 2026-06-12: Implementação concluída com sucesso. Mudança de status para review. Todos os testes unitários e de integração (frontend/backend) passando.
