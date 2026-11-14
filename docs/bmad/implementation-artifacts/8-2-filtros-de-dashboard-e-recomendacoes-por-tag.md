---
baseline_commit: c64a900d3c91ed1a024ca7a95e6f6e49fa9b11f0
---

# Story 8.2: Filtros de Dashboard e Recomendações por Tag

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como investidor multi-estratégia,
Eu quero filtrar o Dashboard, a Evolução, a Estratégia de Alocação e o Motor de Recomendações de Graham por uma Tag específica,
Para que eu tome decisões de aporte precisas para cada "sub-carteira" isoladamente.

## Escopo e Limites (Boundaries)

- **O que ESTÁ incluído nesta história (Story 8.2):**
  1. Seletor unificado de lente analítica (`AnalyticalLensTabs`) no topo com a opção padrão `Visão Geral` (limpa o filtro de tag) e as Tags do usuário com indicador de cor/contagem de ativos, sincronizado via `TagProvider.selectedFilterTagId` entre Dashboard, Evolução (`/evolution`), Estratégia de Alocação (`/allocation-strategy`) e Recomendações de Graham (`/graham-recommendations`).
  2. Badge visual explícito `"Visualizando: [Nome da Tag]"` sempre que uma lente de Tag estiver ativa, evitando confusão entre sub-carteira e carteira global.
  3. Recálculo real (backend + frontend) de KPIs do Dashboard (`Patrimônio Total`, `Rendimento Bruto`, `Dividendos Acumulados`, `Projeção Mensal`), fatias do gráfico de alocação (`AssetAllocationDonutChart`) e linhas da tabela (`FinancialDataTable`) considerando apenas os ativos associados à Tag selecionada, com animação crossfade de `250ms` (`NFR2`).
  4. Aviso discreto no Dashboard quando houver ativos em custódia sem nenhuma tag associada e uma Tag estiver selecionada (`"X ativos sem tag não estão sendo exibidos"`), com ação direta para associá-los.
  5. Filtragem por `?tagId=` no histórico de Evolução (`GET /api/v1/portfolios/evolution?tagId=...`), recalculando aportes (`COMPRA`) e proventos (`DIVIDENDO`) dos últimos 12 meses somente dos ativos da Tag selecionada.
  6. Suporte a metas de alocação isoladas por Tag em `allocation_goals` e `AllocationStrategyController` (`GET/POST /api/v1/allocation-strategy?tagId=...`, `@RequirePremium`), sem sobrescrever metas globais nem metas de outras Tags.
  7. Suporte a escopo por Tag no Motor de Recomendação de Graham (`graham_recommendations`, `PortfolioSnapshotPort`, `GenerateGrahamRecommendationsUseCase`, `ValuationRequestedEvent`/`ValuationCompletedEvent`/`ValuationFailedEvent`, `GrahamRecommendationsController`, `valuation-service`), calculando saldo/alocação atual apenas sobre os ativos da Tag e usando exclusivamente as metas da própria Tag (quando definidas), completando o recálculo em `< 2s` (`NFR3`).
- **O que NÃO está incluído (fora de escopo desta história):**
  - Criação/edição/exclusão/associação básica de Tags e tela `/tags` (já entregues na Story 8.1).
  - Filtro de metas financeiras (`financial_goals`, `GoalSummaryCard`, `MotivationalClockCard`, `WhatIfSimulatorCard`, `GoalHealthChecklistCard`) por Tag — permanece no nível da conta global para não confundir projeções de patrimônio total com sub-carteiras.
  - Combinação booleana múltipla simultânea (`Tag A AND/OR Tag B`) ou troca de `profileId` familiar — o filtro desta história é por uma Tag ativa única (`UUID? tagId`).

## Acceptance Criteria

1. **Seletor de Lente Analítica (`AnalyticalLensTabs`) no topo (`FR15`, UX Notion-style Views):**
   - **Dado** que o usuário possui Tags criadas (`TagProvider.tags` não vazio) ou está visualizando sua carteira
   - **Quando** acessa o Dashboard (`/`), Evolução (`/evolution`), Estratégia de Alocação (`/allocation-strategy`) ou Recomendações de Graham (`/graham-recommendations`)
   - **Então** um componente `AnalyticalLensTabs` é exibido no topo da área de conteúdo contendo `Visão Geral` (padrão quando `selectedFilterTagId == null`) seguida das Tags disponíveis com suas cores e contagem de ativos (além de atalho discreto para gerenciar tags `/tags`)
   - **E** ao selecionar uma Tag em qualquer uma dessas telas, o estado `TagProvider.selectedFilterTagId` permanece sincronizado entre navegações até que o usuário clique em `Visão Geral` (ou faça logout/troca de conta).

2. **Indicador Visual Explícito de Contexto Filtrado:**
   - **Dado** que uma Tag está selecionada (`selectedFilterTag != null`)
   - **Quando** o usuário visualiza o Dashboard, Evolução, Estratégia de Alocação ou Recomendações de Graham
   - **Então** um badge/pill visual claramente legível `"Visualizando: [Nome da Tag]"` é exibido junto ao cabeçalho/conteúdo da tela com a cor da Tag e ação rápida para voltar à `Visão Geral`, evitando confusão com a visão global da carteira.

3. **Recálculo de KPIs, Gráfico Donut e Custódia no Dashboard com Crossfade (`NFR2`):**
   - **Dado** que o usuário seleciona uma Tag no seletor do Dashboard
   - **Quando** o filtro é aplicado
   - **Então** o frontend consulta `GET /api/v1/portfolios/summary?tagId={tagId}` e `GET /api/v1/portfolios/custody?tagId={tagId}` (mantendo validação de ownership `userId` no backend e retornando `404` se `tagId` inexistente ou de outro usuário)
   - **E** os KPIs (`Patrimônio Total`, `Rendimento Bruto`, `Dividendos Acumulados`, `Projeção Mensal`) são recalculados considerando somente os trades/posições dos tickers pertencentes àquela Tag
   - **E** o `AssetAllocationDonutChart` exibe somente as proporções por classe calculadas sobre os ativos pertencentes àquela Tag (preservando a composição com filtro de fatia por classe quando aplicável, e limpando seleção de classe inexistente na nova tag)
   - **E** a tabela `FinancialDataTable` exibe apenas os ativos em custódia pertencentes àquela Tag
   - **E** a transição visual dos blocos (KPIs, gráfico Donut e tabela) utiliza animação crossfade suave de `250ms` (`AnimatedSwitcher(duration: Duration(milliseconds: 250))`) sem travamentos (`< 300ms` local após retorno dos dados).

4. **Aviso Discreto de Ativos sem Tag no Dashboard:**
   - **Dado** que o usuário possui ativos em custódia sem nenhuma tag associada e uma Tag específica está selecionada no Dashboard
   - **Quando** visualiza o Dashboard
   - **Então** um aviso discreto é exibido informando exatamente quantos ativos em custódia não possuem nenhuma tag (singular/plural coerente: `"1 ativo sem tag não está sendo exibido"` ou `"{X} ativos sem tag não estão sendo exibidos"`)
   - **E** oferece link/botão direto (ex.: `"Associar agora"`) que abre seletor rápido dos ativos sem tag (reutilizando `AssetTagSelectorSheet` ou direcionando/destacando os ativos para associação imediata sem perder o contexto), atualizando a contagem reativamente após associar
   - **E** quando todos os ativos em custódia possuem ao menos 1 tag (ou quando `Visão Geral` está ativa), esse aviso não é exibido.

5. **Histórico de Evolução (Aportes e Proventos) Filtrado por Tag:**
   - **Dado** que uma Tag está selecionada
   - **Quando** o usuário visualiza ou alterna a Tag na tela de Evolução (`/evolution`)
   - **Então** `GET /api/v1/portfolios/evolution?tagId={tagId}` filtra os trades `COMPRA` (aportes) e `DIVIDENDO` (proventos) apenas para os tickers vinculados àquela Tag (retornando os 12 meses consecutivos em UTC com zeros para meses sem movimentação dessa Tag)
   - **E** o gráfico de barras mensais e o badge `"Visualizando: [Nome da Tag]"` são atualizados imediatamente após mudança da lente analítica.

6. **Estratégia de Alocação Escopada por Tag (`@RequirePremium`):**
   - **Dado** um usuário `PREMIUM` com uma Tag selecionada
   - **Quando** acessa ou alterna a Tag em `/allocation-strategy`
   - **Então** `GET /api/v1/allocation-strategy?tagId={tagId}` retorna apenas as metas (`ASSET_CLASS` e `TICKER`) configuradas para aquela Tag (retornando lista vazia `[]` caso nenhuma meta própria tenha sido salva ainda — sem misturar metas globais)
   - **E** ao salvar via `POST /api/v1/allocation-strategy?tagId={tagId}` (mantendo validação de soma de `ASSET_CLASS` igual a `100%` e unicidade por escopo), apenas as metas daquela Tag são substituídas em `allocation_goals`, preservando integralmente as metas da `Visão Geral` (`scope_key = 'GLOBAL'`) e de outras Tags
   - **E** usuários `FREE` continuam recebendo `403 Forbidden` (`urn:problem-type:upgrade-required`) tanto com quanto sem `tagId`, enquanto `tagId` inválido/de outro usuário retorna `404 Not Found` para `PREMIUM`.

7. **Motor de Recomendação de Graham com Lente por Tag (`FR15`, `NFR3` `< 2s`):**
   - **Dado** um usuário `PREMIUM` com uma Tag selecionada em Recomendações de Graham (`/graham-recommendations`)
   - **Quando** visualiza ou aciona o recálculo (`POST /api/v1/graham-recommendations/trigger?tagId={tagId}`)
   - **Então** o controller valida a posse da `tagId` (`404` se inexistente/outro usuário), aplica cooldown isolado por `(userId, scopeKey)` para não bloquear alternância entre Tags e `Visão Geral`, e publica `ValuationRequestedEvent(..., tagId)` no Kafka
   - **E** o `valuation-service` calcula o snapshot atual (`PortfolioSnapshotPort.getCurrentAllocationByUserIdAndTagId`) considerando exclusivamente os ativos pertencentes àquela Tag (onde a soma do patrimônio daqueles ativos forma `100%` da sub-carteira da Tag)
   - **E** `GenerateGrahamRecommendationsUseCase` utiliza exclusivamente as metas de alocação configuradas para aquela Tag (`AllocationGoalPort.findByUserId(userId, tagId)`, se definidas; caso nenhuma meta própria exista para a Tag, executa no modo sem metas sem herdar metas globais)
   - **E** persiste as recomendações em `graham_recommendations` com `tag_id` e `scope_key` próprios (sem apagar recomendações globais nem de outras Tags), publica `ValuationCompletedEvent(..., tagId)` com correlação por `tagId` no SSE do frontend, e o recálculo é concluído em `< 2s` (`NFR3`)
   - **E** `GET /api/v1/graham-recommendations?tagId={tagId}` retorna as recomendações persistidas somente daquele escopo (ou lista vazia se ainda não calculadas para aquela Tag), permitindo que o usuário visualize ou recalcule sem afetar a `Visão Geral`.

8. **Restauração Instantânea para `Visão Geral`:**
   - **Dado** que uma Tag está ativa em qualquer tela suportada
   - **Quando** o usuário seleciona `Visão Geral` no `AnalyticalLensTabs` (ou limpa o filtro pelo badge/botão)
   - **Então** `TagProvider.selectFilterTag(null)` limpa o filtro globalmente, os endpoints voltam a ser consultados sem `tagId` (escopo completo/`GLOBAL`) e os valores totais da carteira, metas globais e recomendações globais são restaurados com transição suave.

9. **Isolamento Multi-Tenant, Temporização UTC, Integridade de Banco e Observabilidade (`AGENTS.md`):**
   - **Dado** requisições nas APIs alteradas e processamento de mensagens Kafka
   - **Então** todo `userId` é extraído exclusivamente do `Jwt` autenticado (`jwt.getSubject()`), toda validação de `tagId` confirma pertencimento ao `userId` (`404` caso contrário), toda exclusão de Tag continua removendo em cascata metas/recomendações associadas sem erro de FK, e operações em Kafka lidam graciosamente caso a Tag seja deletada durante processamento em voo
   - **E** todas as novas datas/timestamps usam `ZoneOffset.UTC`
   - **E** todas as entradas de endpoint e consumidores Kafka adicionam logs `INFO` com contexto sanitizado (`userId`, `tagId`), e todas as capturas/lançamentos de exceção logam em nível `ERROR` com stack trace.

## Tasks / Subtasks

- [x] **Task 1: Schema Liquibase (Migration 31) e Entidades JPA escopadas por Tag** (AC: 6, 7, 9)
  - [x] 1.1 Criar `backend/common/src/main/resources/db/changelog/31-add-tag-scope-to-allocation-goals-and-graham-recommendations.yaml` e registrá-la em `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (NUNCA modificar migrations `01`–`30` devido ao `LiquibaseChecksumValidationTest`).
  - [x] 1.2 Em `allocation_goals`: adicionar coluna `tag_id` (`CHAR(36)`, nullable: true) e `scope_key` (`VARCHAR(36)`, defaultValue `'GLOBAL'`, nullable: false); criar índice `idx_allocation_goals_user_scope (user_id, scope_key)`, substituir constraint única `uq_allocation_goals_user_type_key` por `uq_allocation_goals_user_scope_type_key (user_id, scope_key, goal_type, target_key)` (garantindo unicidade em MySQL `8.0` e H2 `MODE=MySQL` mesmo quando `tag_id IS NULL`), e adicionar FK `fk_allocation_goals_tag_id` -> `portfolio_tags(id)` com `onDelete: CASCADE` / `onUpdate: RESTRICT`.
  - [x] 1.3 Em `graham_recommendations`: adicionar coluna `tag_id` (`CHAR(36)`, nullable: true) e `scope_key` (`VARCHAR(36)`, defaultValue `'GLOBAL'`, nullable: false); criar índice `idx_graham_rec_user_scope (user_id, scope_key)` e FK `fk_graham_rec_tag_id` -> `portfolio_tags(id)` com `onDelete: CASCADE` / `onUpdate: RESTRICT`.
  - [x] 1.4 Atualizar as entidades JPA em ambos os módulos (`api` e `valuation-service`) mantendo alinhamento exato de tipos (`@JdbcTypeCode(SqlTypes.VARCHAR)` com `UUID tagId` no `api`, `String tagId` com `length = 36` no `valuation-service`, e `String scopeKey` com `nullable = false, length = 36` em ambos):
    - `backend/api/.../AllocationGoalEntity.java` e `backend/api/.../GrahamRecommendationEntity.java`
    - `backend/valuation-service/.../AllocationGoalReadEntity.java` e `backend/valuation-service/.../GrahamRecommendationEntity.java`

- [x] **Task 2: Portfolio & Evolução filtrados por Tag no `common` e `api`** (AC: 3, 4, 5, 9)
  - [x] 2.1 Injetar `PortfolioTagPort` e `AssetTagPort` nos casos de uso `GetPortfolioSummaryUseCase`, `GetCustodyPositionsUseCase` e `GetPortfolioEvolutionUseCase` (em `backend/common/.../common/portfolio/application/usecase/`), mantendo os construtores legados delegando com `null` para preservar 100% de compatibilidade com unit tests existentes, e configurando os beans em `PortfolioSummaryConfiguration.java`.
  - [x] 2.2 Extrair helper reutilizável `PortfolioTagFilterSupport` no pacote `common.portfolio.application.usecase` (zero dependências Spring/JPA) que: (a) retorne `null` quando `tagId == null`; (b) valide `portfolioTagPort.findByIdAndUserId(tagId, userId)` lançando `TagNotFoundException` se ausente; (c) colete `Set<String>` de tickers normalizados (`trim().toUpperCase(Locale.ROOT)`) de `assetTagPort.findAllByUserIdAndTagId(userId, tagId)`; e (d) filtre a lista de trades por ticker normalizado (preservando equivalência com sufixo fracionário `F` conforme o padrão do `GetCustodyPositionsUseCase`).
  - [x] 2.3 Implementar sobrecargas `execute(String userId, UUID tagId)` nos três casos de uso (`GetPortfolioSummaryUseCase`, `GetCustodyPositionsUseCase`, `GetPortfolioEvolutionUseCase`) reutilizando integralmente o cálculo já existente (sem duplicar fórmulas), corrigindo `GetPortfolioEvolutionUseCase` para usar `LocalDate.now(ZoneOffset.UTC)` e retornando valores zerados / listas vazias / 12 meses zerados quando a Tag não possuir ativos associados. Em `GetCustodyPositionsUseCase`, expor também método/resultado auxiliar para obter os tickers ativos globais sem tag (comparando posições net > 0 contra `assetTagPort.findAllByUserId(userId)`) sem duplicar chamadas de preço/empresa.
  - [x] 2.4 Atualizar `PortfolioController` (`/api/v1/portfolios/summary`, `/custody`, `/evolution`) para aceitar `@RequestParam(required = false) UUID tagId` com validação de `jwt.getSubject()`, logs `INFO` (`userId`, `tagId`) e tratamento de erro com log `ERROR` + stack trace. **Atenção ao contrato de resposta:** manter `GET /summary` retornando `PortfolioSummaryDTO` direto e `GET /evolution` retornando `PortfolioEvolutionDTO` direto (sem wrapper), e em `GET /custody` preservar `{ "data": [...], "meta": { ... } }` acrescentando em `meta` `"untaggedAssetsCount"` (`int`) e `"untaggedTickers"` (`List<String>`).

- [x] **Task 3: Estratégia de Alocação e Motor de Graham escopados por Tag (`common`, `api`, `valuation-service`)** (AC: 6, 7, 9)
  - [x] 3.1 Criar utilitário POJO `ScopeKey` (ou `AllocationScope` em `backend/common/.../valuation/domain`) normalizando `UUID tagId -> "GLOBAL"` quando nulo ou `tagId.toString()` quando presente, evitando strings mágicas espalhadas.
  - [x] 3.2 Atualizar `AllocationGoalPort`, `SaveAllocationGoalsUseCase`, `GetAllocationGoalsUseCase` e `AllocationStrategyConfiguration` para suportar `(String userId, UUID tagId)` (com sobrecargas delegando `tagId = null` para compatibilidade). Validar propriedade da `tagId` no `api` via `PortfolioTagPort` (lançando `TagNotFoundException` se inválido).
  - [x] 3.3 Atualizar `AllocationGoalJpaRepository` e `AllocationGoalJpaAdapter` (`api`), bem como `AllocationGoalReadJpaRepository` e `AllocationGoalReadAdapter` (`valuation-service`), para consultar/deletar exclusivamente por `(userId, scopeKey)` sem nunca apagar metas de outros escopos — utilizando `@Modifying(clearAutomatically = true, flushAutomatically = true)` na query JPQL de exclusão por escopo (e mantendo `findByUserId` para compatibilidade de testes). Atualizar `AllocationStrategyController` para receber `@RequestParam(required = false) UUID tagId` em `GET` e `POST` com logs `INFO`/`ERROR`.
  - [x] 3.4 Atualizar eventos Kafka `ValuationRequestedEvent`, `ValuationCompletedEvent` e `ValuationFailedEvent` adicionando campo opcional `UUID tagId` com construtores canônicos delegando `null` para manter retrocompatibilidade total com deserialização JSON e testes existentes.
  - [x] 3.5 Atualizar `PortfolioSnapshotPort` e `PortfolioSnapshotJpaAdapter` (`valuation-service`) adicionando `getCurrentAllocationByUserIdAndTagId(String userId, UUID tagId)`, compartilhando a lógica de cálculo de preços/percentuais sem duplicação SQL e filtrando `trades` com subconsulta `AND EXISTS (SELECT 1 FROM asset_tags ag WHERE ag.user_id = t.user_id AND ag.tag_id = :tagId AND UPPER(TRIM(ag.ticker)) = UPPER(TRIM(t.ticker)))` (evitando o alias `at`, palavra reservada SQL, e evitando duplicação de linhas).
  - [x] 3.6 Atualizar `GrahamRecommendationPort`, `GenerateGrahamRecommendationsUseCase`, `GetGrahamRecommendationsUseCase`, `GrahamRecommendationsConfiguration`, `GrahamRecommendationReadAdapter` (`api`), `GrahamRecommendationRepositoryImpl` (`valuation-service`), `ValuationServiceConfiguration`, `ValuationRequestedConsumerService`, `ValuationRequestedPublisher` e `GrahamRecommendationsController`:
    - `GrahamRecommendationsController`: aceitar `@RequestParam(required = false) UUID tagId` em `GET` e `POST /trigger`; validar `portfolioTagPort.findByIdAndUserId(tagId, userId)` antes de publicar no Kafka; chavear cooldown como `userId + ":" + scopeKey` (`10s`); adicionar logs `INFO`/`ERROR`.
    - `ValuationRequestedConsumerService`: logar `INFO` com `userId` e `tagId`; acionar `generateGrahamRecommendationsUseCase.execute(userId)` quando `tagId == null` ou `execute(userId, tagId)` quando presente (mantendo retrocompatibilidade dos mocks em `ValuationRequestedConsumerServiceTest`); publicar `ValuationCompletedEvent`/`ValuationFailedEvent` preservando `tagId` e logando exceções em `ERROR`.
    - `GrahamRecommendationRepositoryImpl` (`valuation-service`): manter o construtor único recebendo `GrahamRecommendationJpaRepository` (para compatibilidade com `GrahamRecommendationRepositoryImplIT.Config`), deletar e salvar apenas por `(userId, scopeKey)` com `@Modifying(clearAutomatically = true, flushAutomatically = true)` (preservando `deleteByUserId` / `findByUserIdOrderByRecommendationScoreDesc` quando usado no caminho legado ou atualizando os testes correspondentes), e verificar via `@Query` nativa em `GrahamRecommendationJpaRepository` se a `tagId` (quando não nula) ainda existe em `portfolio_tags` antes do `saveAll` para evitar falha de FK caso a Tag tenha sido excluída concorrentemente durante trânsito no Kafka.

- [x] **Task 4: Camada Data/Domain/Provider no Frontend para Escopo por Tag** (AC: 1, 3, 4, 5, 6, 7, 8)
  - [x] 4.1 Atualizar `PortfolioRemoteDataSource`, `PortfolioRepository`, `PortfolioRepositoryImpl` e `PortfolioProvider` para aceitar `{String? tagId}` em `getSummary`, `getCustodyPositions` (incluindo leitura de `meta['untaggedAssetsCount']` e `meta['untaggedTickers']`) e `getPortfolioEvolution`. No `PortfolioProvider`:
    - Manter `_globalSummary` (`get globalSummary`) e `_globalCustodyTickers` sempre sincronizados com a visão global (populando quando `tagId == null`, buscando baseline global silenciosamente se a tela abrir já filtrada por Tag, e atualizando ambos em eventos SSE `PORTFOLIO_UPDATED`) para que as Metas Financeiras permaneçam globais e a contagem dinâmica de ativos sem tag reflita alterações locais via `TagProvider`.
    - Preservar `_activeTagId` nos reloads acionados por SSE `PORTFOLIO_UPDATED` (não sobrescrever a lente filtrada com dados globais) e aplicar contador de geração por operação (`loadSummary`, `loadCustodyPositions`, `loadEvolutionData`) descartando respostas HTTP atrasadas de uma lente anterior.
  - [x] 4.2 Atualizar `AllocationRemoteDataSource`, `AllocationRepository`, `AllocationRepositoryImpl` e `AllocationProvider` para aceitar `{String? tagId}` em `getGoals` e `saveGoals`, preservando o `tagId` ativo em estado e descartando respostas obsoletas via geração de requisição. **Atenção:** anexar `?tagId=...` no path via `Uri` (em vez de passar `queryParameters:` no `ApiClient.get/post`) preserva compatibilidade direta com `ApiClient.post` e com os mocks `MockApiClient` existentes sem exigir alteração da assinatura de `ApiClient`.
  - [x] 4.3 Atualizar `GrahamRecommendationRemoteDataSource`, `GrahamRecommendationRepository`, `GrahamRecommendationRepositoryImpl` e `GrahamRecommendationProvider` para aceitar `{String? tagId}` em `fetchRecommendations` e `triggerCalculation(userId, {String? tagId})`. No listener SSE dentro de `GrahamRecommendationProvider.triggerCalculation`, filtrar `VALUATION_COMPLETED`/`VALUATION_FAILED` tanto por `event.userId` quanto pela correspondência exata de `event.payload?['tagId']` com o `tagId` disparado, chamando `fetchRecommendations(tagId: tagId)` na conclusão/falha e evitando que resposta de um escopo sobrescreva outro se o usuário alternar de Tag durante o cálculo.

- [x] **Task 5: Componente `AnalyticalLensTabs`, Badge Contextual e Aviso de Ativos sem Tag nas Telas** (AC: 1, 2, 3, 4, 5, 6, 7, 8)
  - [x] 5.1 Criar `frontend/lib/src/features/tags/presentation/widgets/analytical_lens_tabs.dart` implementando `AnalyticalLensTabs` (exibe `Visão Geral` com `Key('tag_filter_all')`, chips das tags com dot colorido + nome + contador com `Key('tag_filter_${tag.id}')`, atalho `"Gerenciar Tags"` preservando `Key('dashboard_manage_tags_button')`, semântica acessível e touch target mínimo `48x48dp`) e `ActiveTagLensBadge` (`"Visualizando: [Nome da Tag]"` com cor da tag + botão voltar à `Visão Geral`), além de modal/sheet rápido dos tickers sem tag que abre `AssetTagSelectorSheet`. Atualizar `DashboardTagFilterBar` para delegar ao `AnalyticalLensTabs` preservando `Key`s e compatibilidade dos testes existentes.
  - [x] 5.2 Integrar `AnalyticalLensTabs`, `ActiveTagLensBadge`, `AnimatedSwitcher(duration: Duration(milliseconds: 250))` (keyed por lente ativa + resumo/custódia) e aviso discreto de ativos sem tag (`"1 ativo sem tag não está sendo exibido"` / `"{X} ativos sem tag não estão sendo exibidos"` + `"Associar agora"`) na `DashboardPage`:
    - Manter `MotivationalClockCard`, `GoalHealthChecklistCard`, `TimeToGoalCalculator.calculate` e `goalsProvider.recalculateLocalProjection` alimentados exclusivamente por `provider.globalSummary ?? provider.summary` (nunca pelo resumo sub-filtrado da Tag).
    - Sincronizar mudanças de `TagProvider.selectedFilterTagId` (e também de `TagProvider` após editar tags inline pela tabela ou pelo sheet de associação rápida) recarregando `loadSummary(tagId: ...)` e `loadCustodyPositions(tagId: ...)`, invalidando `selectedAssetClassFilter` apenas quando a nova lente não possuir aquela classe, e preservando `tagId` nos botões de retry de KPIs e da tabela.
    - Corrigir a ordem do estado vazio em `FinancialDataTable`: avaliar `selectedFilterTag != null` (`_FilteredEmptyStateWidget` com `Key('filtered_tags_empty_state')` e `Key('clear_tag_filter_button')`) **antes** de `_EmptyStateWidget` (`"Fazer upload do arquivo B3"`), pois com filtro server-side `provider.custodyPositions` retorna vazio para tags sem ativos.
  - [x] 5.3 Integrar `AnalyticalLensTabs`, `ActiveTagLensBadge` e reação automática ao `TagProvider.selectedFilterTagId` em `EvolutionPage`, `AllocationStrategyPage` e `GrahamRecommendationsPage`:
    - Garantir que todas chamem `tagProvider?.loadTags(silent: tagProvider.tags.isNotEmpty)` no `initState` (caso acessadas diretamente sem passar pelo Dashboard), inicializem já usando `tagProvider?.selectedFilterTagId`, preservem `tagId` nos botões retry/refresh e suportem `TagProvider == null` nos testes isolados (`Provider.of<TagProvider?>(context)`).
    - Em `AllocationStrategyPage`: remover a trava definitiva de `_initialized` (ou reinicializar controladores sempre que a lente/resposta da API mudar, limpando campos e entradas de ticker quando a tag retornar `[]`), manter tolerância de ponto flutuante `(_classSum - 100).abs() > 0.01`, e preservar `tagId` tanto em `saveGoals(..., tagId: ...)` quanto no reload pós-salvamento.
    - Em `GrahamRecommendationsPage`: acionar `fetchRecommendations(tagId: ...)` e `triggerCalculation(userId, tagId: ...)` escopados pela Tag ativa e exibir estado vazio contextual quando a Tag ainda não tiver cálculo próprio.

- [x] **Task 6: Testes Automatizados Backend & Frontend e Validação Completa** (AC: 1–9)
  - [x] 6.1 **Testes Unitários Backend (`common`):** expandir `GetPortfolioSummaryUseCaseTest`, `GetCustodyPositionsUseCaseTest`, `GetPortfolioEvolutionUseCaseTest`, `SaveAllocationGoalsUseCaseTest`, `GetAllocationGoalsUseCaseTest` e `GenerateGrahamRecommendationsUseCaseTest` cobrindo cenários com `tagId` válido, `tagId` sem ativos, `tagId` inexistente/de outro usuário (`TagNotFoundException`), uso exclusivo de metas da Tag sem vazar metas globais, cálculo de `untaggedTickers` e cálculo de gap apenas sobre o saldo da Tag.
  - [x] 6.2 **Testes Unitários & Integração Backend (`api` e `valuation-service`):**
    - `PortfolioControllerTest` & `PortfolioControllerIT`: testar `/summary`, `/custody` (incluindo `meta.untaggedAssetsCount` / `meta.untaggedTickers`) e `/evolution` com `?tagId=` e erro `404` para `tagId` de outro usuário.
    - `AllocationStrategyIT`: testar metas isoladas por `tagId` vs `GLOBAL`, restrição `403` para usuário `FREE` e `404` para `tagId` alheio.
    - `GrahamRecommendationsIT` & `ValuationCompletedConsumerTest`: testar `GET` e `POST /trigger` com `?tagId=`, cooldown isolado por escopo, validação de `tagId` alheio (`404`), e preservação de recomendações globais + por tag; testar `TagControllerIT` removendo tag que possui `allocation_goals` e `graham_recommendations` sem erro de FK.
    - `PortfolioSnapshotJpaAdapterTest` & `PortfolioSnapshotJpaAdapterIT` (MySQL Testcontainers): validar `getCurrentAllocationByUserIdAndTagId` com soma `100%` restrita aos ativos da Tag e isolamento por `user_id`.
    - `GrahamRecommendationRepositoryImplTest`, `GrahamRecommendationRepositoryImplIT` & `ValuationRequestedConsumerServiceTest`: testar persistência/deleção escopada por `(userId, scopeKey)`, `tagId` nos eventos Kafka, e resiliência quando a Tag é removida antes da conclusão no Kafka.
    - Verificar `LiquibaseChecksumValidationTest`, `ApiServiceContextTest` e `ValuationConsumerServiceContextTest`.
  - [x] 6.3 **Testes Frontend (`flutter test` e `flutter analyze`):**
    - Atualizar implementações fake de repositórios/datasources e regenerar mocks (`dart run build_runner build --delete-conflicting-outputs` para `dashboard_page_test.mocks.dart`) em todos os arquivos afetados listados nas Dev Notes.
    - Criar `frontend/test/features/tags/analytical_lens_tabs_test.dart` e expandir testes de `DashboardPage` (KPIs, Donut, Tabela, crossfade 250ms, Metas Financeiras preservadas globais, aviso `"X ativos sem tag não estão sendo exibidos"` + associação inline, volta à `Visão Geral`), `FinancialDataTable` (empty state com filtro server-side), `EvolutionPage`, `AllocationStrategyPage` (troca de lente atualizando inputs/limpando metas vazias, `PREMIUM` e paywall `FREE`), `GrahamRecommendationsPage` (filtro + correlação SSE por `tagId`) e `tags_dashboard_integration_test.dart` ponta a ponta.
    - Executar `mvn clean test` (módulos `common`, `api`, `valuation-service`), `flutter analyze` e `flutter test` na árvore completa antes de concluir.

### Review Findings

- [x] [Review][Audit] Clean review — todas as camadas passaram (Blind Hunter, Edge Case Hunter, Acceptance Auditor).
  - Blind Hunter: Validações multi-tenant consistentes (`Jwt.getSubject()`), consultas nativas parametrizadas com aliases seguros, integridade referencial com `ON DELETE CASCADE` e resiliência a exclusão concorrente no Kafka verificada.
  - Edge Case Hunter: Tratamento de tags vazias sem ativos, sufocamento/mapeamento de sufixo fracionário B3 ('F'), sincronização reativa com contador de geração em chamadas assíncronas do frontend e tolerância flutuante de 100% de classe.
  - Acceptance Auditor: Todos os 9 Critérios de Aceitação cumpridos com suite de testes completa (253 testes frontend, 21 suites backend cobrindo common, api e valuation-service, LiquibaseChecksumValidationTest íntegro).

## Dev Notes

### Estado Atual do Codebase vs. Mudanças Necessárias

1. **O que a Story 8.1 já entregou e deve ser reutilizado:**
   - Tabelas `portfolio_tags` e `asset_tags` (`backend/common/src/main/resources/db/changelog/30-create-portfolio-tags-and-asset-tags.yaml`), entidades JPA com `Persistable<UUID>` (`PortfolioTagEntity`, `AssetTagEntity`) e ports `PortfolioTagPort` / `AssetTagPort` já configurados em `api`.
   - `TagController` (`/api/v1/tags/**`) já acessível para usuários `FREE` e `PREMIUM`.
   - `TagProvider` (`frontend/lib/src/features/tags/presentation/providers/tag_provider.dart`) registrado como singleton no `MultiProvider` de `main.dart` (portanto já compartilhado entre todas as rotas do `ShellRoute`: `/dashboard`, `/evolution`, `/allocation-strategy`, `/graham-recommendations`) já gerencia `selectedFilterTagId`, `selectedFilterTag`, `filteredTickersForSelectedTag` e `selectFilterTag(String? tagId)` + `reset()`.
   - `DashboardTagFilterBar` (`frontend/lib/src/features/tags/presentation/widgets/dashboard_tag_filter_bar.dart`) e `AssetTagSelectorSheet` (`frontend/lib/src/features/tags/presentation/widgets/asset_tag_selector_sheet.dart`) já existem e permitem associação inline.
   - `FinancialDataTable` (`frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart`) já lê `Provider.of<TagProvider?>(context)` com segurança quando nulo e filtra linhas por `selectedFilterTag` + coluna `'Tags'`.

2. **Regras Críticas de Banco de Dados (Liquibase + H2 MODE=MySQL + MySQL 8.0):**
   - **Nunca alterar migrations `01`–`30`:** `LiquibaseChecksumValidationTest` valida checksums históricos. Crie a migration `31-...yaml` e inclua em `db.changelog-master.yaml`.
   - **Armadilha de `NULL` em `UNIQUE` no MySQL:** Em MySQL `8.0`, `UNIQUE (user_id, tag_id, goal_type, target_key)` **não** impede duplicatas quando `tag_id IS NULL` (`NULL != NULL`). Por isso, introduza a coluna `scope_key VARCHAR(36) NOT NULL DEFAULT 'GLOBAL'` com valor `'GLOBAL'` quando global e `tagId.toString()` quando escopada por Tag, criando a constraint única em `(user_id, scope_key, goal_type, target_key)`.
   - **Ordem segura para `dropUniqueConstraint` e delete JPA:** Como feito em `10-fix-deletion-request-unique-constraint.yaml`, crie o novo índice (`idx_allocation_goals_user_scope`) antes de dropar `uq_allocation_goals_user_type_key`, compatível tanto com H2 (`MODE=MySQL`) nos ITs de `api` quanto com MySQL `8.0.28+` (Testcontainers) em `valuation-service`. Em `AllocationGoalJpaRepository` e `GrahamRecommendationJpaRepository`, anote exclusões JPQL escopadas com `@Modifying(clearAutomatically = true, flushAutomatically = true)` para evitar `ConstraintViolationException` / cache stale do `EntityManager` antes do `saveAll` na mesma transação.
   - **Consistência de UUID (`CHAR(36)`), alias SQL e `@Builder.Default` para `scopeKey`:** `api` mapeia UUIDs com `@JdbcTypeCode(SqlTypes.VARCHAR)` (`@Column(length = 36)`), enquanto `valuation-service` mapeia como `String` (`@Column(name = "...", length = 36, columnDefinition = "CHAR(36)")`). Passe `tagId.toString()` em queries nativas (`PortfolioSnapshotJpaAdapter`) usando alias `ag` + `EXISTS (...)` (nunca use `at` como alias SQL, evitando conflito com palavra reservada). **Crítico:** Em todas as entidades JPA alteradas (`AllocationGoalEntity`, `AllocationGoalReadEntity` e ambas `GrahamRecommendationEntity`), anote `@Builder.Default private String scopeKey = "GLOBAL";` para que builders em testes existentes (`GrahamRecommendationsIT`, `GrahamRecommendationRepositoryImplTest`) não falhem com `PropertyValueException` de campo not-null.
   - **`onDelete: CASCADE` com `portfolio_tags`:** Como `DeleteTagUseCase` executa `DELETE FROM PortfolioTagEntity` diretamente via JPQL, adicionar FK `fk_allocation_goals_tag_id` e `fk_graham_rec_tag_id` com `onDelete: CASCADE` garante limpeza automática sem quebrar exclusão de tags que possuam metas/recomendações.

3. **Arquitetura Backend, Isolamento `common` e Multi-Module Parity (`api` vs `valuation-service`):**
   - **Zero dependências Spring/JPA/Validation no domínio/usecases de `common`:** Mantenha `GetPortfolioSummaryUseCase`, `GetCustodyPositionsUseCase`, `GetPortfolioEvolutionUseCase`, `SaveAllocationGoalsUseCase`, `GetAllocationGoalsUseCase` e `GenerateGrahamRecommendationsUseCase` como POJOs puros. Use injeção via construtor em `@Configuration` (`PortfolioSummaryConfiguration`, `AllocationStrategyConfiguration`, `GrahamRecommendationsConfiguration`, `ValuationServiceConfiguration`).
   - **Nenhuma chamada HTTP síncrona entre `api` e `valuation-service`:** Toda comunicação continua via Kafka (`TopicConstants.VALUATION_REQUESTED_TOPIC`, `VALUATION_COMPLETED_TOPIC`, `VALUATION_FAILED_TOPIC`). Ambas as aplicações acessam o mesmo schema MySQL gerenciado pelo Liquibase em `common`.
   - **Paridade de JPA e Entidades em `valuation-service`:** `ValuationConsumerServiceApplication` faz `@EntityScan`, `@EnableJpaRepositories` e component scan apenas em `afsdigital.grahamselect.common` e `afsdigital.grahamselect.valuation` (NÃO enxerga classes/beans de `afsdigital.grahamselect.api`!). Portanto:
     - Mantenha as 4 dependências originais do construtor de `GenerateGrahamRecommendationsUseCase` (`AllocationGoalPort`, `RankingRepository`, `PortfolioSnapshotPort`, `GrahamRecommendationPort`) sem injetar `PortfolioTagPort` dentro dele (evitando exigir bean novo no `ValuationServiceConfiguration` ou quebrar `@InjectMocks` em `GenerateGrahamRecommendationsUseCaseTest`).
     - Mantenha também o construtor de 1 argumento de `GrahamRecommendationRepositoryImpl(GrahamRecommendationJpaRepository)` adicionando a consulta nativa `countOwnedTag(tagId, userId)` diretamente em `GrahamRecommendationJpaRepository` (`valuation-service`) para não quebrar `GrahamRecommendationRepositoryImplIT.Config` nem `GrahamRecommendationRepositoryImplTest`.
     - `PortfolioSnapshotJpaAdapter` consulta `trades`, `companies`, `stock_price` e agora `asset_tags` via SQL nativo; atualize `AllocationGoalReadEntity` / `AllocationGoalReadJpaRepository` / `AllocationGoalReadAdapter` e `GrahamRecommendationEntity` / `GrahamRecommendationJpaRepository` / `GrahamRecommendationRepositoryImpl` dentro de `valuation-service` em espelho exato com `api`.
   - **Regra de `@RequirePremium`, Envelopes REST e Escopo Multi-Tenant:**
     - `TagController` (`/api/v1/tags/**`) e `PortfolioController` (`/api/v1/portfolios/**`) continuam acessíveis a `FREE` e `PREMIUM`.
     - Preserve integralmente os envelopes JSON existentes em `PortfolioController`: `GET /summary` retorna `PortfolioSummaryDTO` direto, `GET /evolution` retorna `PortfolioEvolutionDTO` direto e apenas `GET /custody` utiliza `{ "data": ..., "meta": ... }`. Em `PortfolioControllerTest` (`BaseRepositoryIT` com `@MockBean` dos use cases), adicione stubs/cenários também para as sobrecargas `(userId, tagId)`.
     - `AllocationStrategyController` e `GrahamRecommendationsController` possuem `@RequirePremium` no nível de classe — mantenha esse controle inalterado.
     - Em **todos** os controllers/use cases que aceitam `UUID tagId` opcional, valide `portfolioTagPort.findByIdAndUserId(tagId, userId)` antes de operar (ou publicar no Kafka), lançando `TagNotFoundException` (`404 Not Found` no `GlobalExceptionHandler`) se a tag não existir ou pertencer a outro usuário.
   - **Nota sobre KPIs no `PortfolioSummaryDTO`:** `PortfolioSummaryDTO` expõe atualmente `(totalEquity, grossYieldPercentage, accumulatedDividends, monthlyProjection)`, que alimentam os 4 cards da `DashboardPage` (`Patrimônio Total`, `Rendimento Bruto`, `Dividendos Acumulados`, `Projeção Mensal`). Todos os 4 devem ser recalculados exclusivamente a partir dos trades cujos tickers pertençam à Tag filtrada.
   - **Nota sobre `GrahamRecommendationPort` ISP e Constantes (`epic-6-retro-2026-11-13.md`):** Preserve assinaturas existentes delegando `tagId = null` para não quebrar callers/mocks (inclusive `GrahamRecommendationRepositoryImplTest` e `ValuationRequestedConsumerServiceTest` quando `tagId == null`) e evite duplicar strings mágicas `"ASSET_CLASS"` / `"TICKER"` no código novo.

4. **Observabilidade e Temporização (`AGENTS.md` + `project-context.md`):**
   - Todo novo endpoint/parâmetro (`tagId`) em `PortfolioController`, `AllocationStrategyController` e `GrahamRecommendationsController` deve registrar log `INFO` na entrada (com `userId` e `tagId` sanitizados, sem tokens/PII) e log `ERROR` com stack trace ao lançar ou capturar exceções (`TagNotFoundException`, erros inesperados).
   - `ValuationRequestedPublisher`, `ValuationRequestedConsumerService` e `ValuationCompletedConsumer` devem logar `userId` e `tagId` em `INFO` e exceções com stack trace em `ERROR`.
   - Todas as datas (`LocalDate.now(ZoneOffset.UTC)`, `LocalDateTime.now(ZoneOffset.UTC)`, `OffsetDateTime.now(ZoneOffset.UTC)`) devem usar explicitamente `ZoneOffset.UTC` (incluindo `GetPortfolioEvolutionUseCase`, atualmente sem `ZoneOffset.UTC`).

5. **Frontend: Reatividade, Performance (`NFR2`, `NFR3`), Layout e Compatibilidade com Testes:**
   - **Substituição / Evolução de `DashboardTagFilterBar` para `AnalyticalLensTabs`:** O épico especifica `AnalyticalLensTabs` com `Visão Geral` (padrão) no topo. Faça `DashboardTagFilterBar` delegar para `AnalyticalLensTabs` preservando os `Key`s `'tag_filter_all'`, `'tag_filter_<id>'` e `'dashboard_manage_tags_button'` exigidos pelos testes existentes. Em `tags_dashboard_integration_test.dart` (linha ~359), atualize o cenário para validar o recálculo real dos KPIs por Tag (e o empty state com filtragem no backend).
   - **Preservação Global de Metas Financeiras na `DashboardPage`:** `DashboardPage` hoje injeta `provider.summary` diretamente em `goalsProvider.recalculateLocalProjection(...)`, `TimeToGoalCalculator.calculate(...)` e `GoalHealthChecklistCard(...)`. Armazene `_globalSummary` no `PortfolioProvider` e aponte esses widgets de Meta exclusivamente para `provider.globalSummary ?? provider.summary`, mantendo o fallback para `provider.summary` quando testes isolados stubbarem apenas `summary`.
   - **Empty State em `FinancialDataTable` com filtro no backend:** Como `GET /custody?tagId=...` já retorna `[]` quando a Tag não tem ativos, `FinancialDataTable` deve verificar `selectedFilterTag != null` (`_FilteredEmptyStateWidget` com `Key('filtered_tags_empty_state')` e `Key('clear_tag_filter_button')`) **antes** de cair no `_EmptyStateWidget` (`"Fazer upload do arquivo B3"`). Ao salvar alterações de tags pelo `AssetTagSelectorSheet` na `DashboardPage`, se houver Tag ativa, recarregue resumo e custódia para refletir na hora inclusões/remoções da lente atual.
   - **SSE, Retries, Troca Rápida e Bootstrap de Tags nas 4 Telas:** Em `PortfolioProvider`, preserve o `tagId` ativo dentro do listener SSE `PORTFOLIO_UPDATED` (recarregando também o baseline global em paralelo) e proteja todos os providers (`PortfolioProvider`, `AllocationProvider`, `GrahamRecommendationProvider`) com contador de geração contra respostas fora de ordem ao trocar de Tag rapidamente. As 4 páginas (`DashboardPage`, `EvolutionPage`, `AllocationStrategyPage`, `GrahamRecommendationsPage`) devem chamar `tagProvider?.loadTags(silent: tagProvider.tags.isNotEmpty)` no `initState` para deep links e propagar `tagId` em todos os botões de retry. Em `AllocationStrategyPage`, remova o bloqueio definitivo de `_initialized` para que os `TextEditingController`s sejam atualizados/limpos sempre que a lente ou resposta mudar.
   - **Aviso de Ativos sem Tag (`"X ativos sem tag não estão sendo exibidos"`):** Não calcule esse número sobre a lista filtrada de `/custody?tagId=...`, pois ela contém apenas os ativos da Tag selecionada! Armazene no `PortfolioProvider` a lista global de tickers em custódia (obtida quando `tagId == null` ou recebida em `meta['untaggedAssetsCount']` / `meta['untaggedTickers']` do `/custody?tagId=...`), combinando dinamicamente com `TagProvider.tagsForTicker(ticker).isEmpty` para refletir instantaneamente associações feitas na própria tela. Ao clicar em `"Associar agora"`, abra um bottom sheet/diálogo listando os tickers sem tag (permitindo tocar em qualquer ticker para abrir `AssetTagSelectorSheet` diretamente).
   - **Armadilha de Mockito em `AllocationRemoteDataSource` & `ApiClient`:** `allocation_remote_data_source_test.dart` usa `when(mockApiClient.get(any))` (que falha em Mockito se o parâmetro nomeado `queryParameters:` for passado!) e `ApiClient.post` não tem `queryParameters` na assinatura atual (cujo mock é gerado em `home_page_test.mocks.dart`, `auth_repository_test.mocks.dart`, `profile_repository_test.mocks.dart` e `allocation_remote_data_source_test.mocks.dart`). Solução segura: construa o path com query string via `Uri` quando `tagId != null` (`'/api/v1/allocation-strategy?tagId=${Uri.encodeQueryComponent(tagId)}'`) — funciona perfeitamente em HTTP real e mantém 100% compatível com todos os mocks `ApiClient` existentes!
   - **Armadilha de Interfaces Fake e Mocks em `frontend/test/`:** Adicionar parâmetros opcionais `{String? tagId}` nas interfaces abstratas `PortfolioRepository`, `AllocationRepository` e `GrahamRecommendationRepository` (e em `PortfolioRemoteDataSource`) exige adicionar `{String? tagId}` nas assinaturas de `@override` das seguintes classes fake existentes nos testes (e rodar `dart run build_runner build --delete-conflicting-outputs` para regenerar `dashboard_page_test.mocks.dart` quando a assinatura de `PortfolioProvider` mudar):
     - `frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart` (`MockPortfolioProvider` gerado em `dashboard_page_test.mocks.dart`)
     - `frontend/test/features/tags/tags_dashboard_integration_test.dart` (`_FakePortfolioRepository`)
     - `frontend/test/features/portfolio/data/repositories/portfolio_repository_impl_test.dart` (`MockPortfolioRemoteDataSource extends PortfolioRemoteDataSource`)
     - `frontend/test/features/portfolio/data/datasources/portfolio_remote_data_source_test.dart` (testes de URL `?tagId=` e `meta.untaggedTickers`)
     - `frontend/test/features/portfolio/presentation/providers/portfolio_provider_test.dart` (`MockPortfolioRepository`)
     - `frontend/test/features/portfolio/presentation/pages/evolution_page_test.dart` (`MockPortfolioRepository`)
     - `frontend/test/features/portfolio/presentation/widgets/financial_data_table_test.dart` (`MockPortfolioRepository`)
     - `frontend/test/features/goals/presentation/pages/goals_page_test.dart` (`ManualMockPortfolioRepository`)
     - `frontend/test/features/allocation/presentation/providers/allocation_provider_test.dart` (`MockAllocationRepository`)
     - `frontend/test/features/allocation/presentation/pages/allocation_strategy_page_test.dart` (`ManualMockAllocationRepository`)
     - `frontend/test/features/ranking/presentation/pages/graham_recommendations_page_test.dart` (`ManualMockGrahamRecommendationRepository`)
     - `frontend/test/features/ranking/presentation/pages/graham_recommendations_page_flow_test.dart` (`FlowMockGrahamRecommendationRepository` e `FlowMockGrahamRecommendationProvider`)
   - **`TagProvider` opcional nos widgets (`Provider.of<TagProvider?>(context)`):** Preserve o padrão da Story 8.1 usando `Provider.of<TagProvider?>(context)` (e nunca `context.watch<TagProvider>()` sem nullability) nas 4 páginas (`DashboardPage`, `EvolutionPage`, `AllocationStrategyPage`, `GrahamRecommendationsPage`) para não quebrar testes de widget que montam páginas sem `TagProvider`.
   - **`AppTheme.elevatedButtonTheme` e UI Kit:** Nunca coloque `ElevatedButton` dentro de `Row` sem `Expanded`/largura fixa (`minimumSize: Size(double.infinity, 56)` causa erro de layout). Use `ChoiceChip`/`SegmentedButton`/`FilterChip`, `FilledButton` (máximo 1 por tela/diálogo), `OutlinedButton` ou `TextButton`, respeitando `minHeight: 48` de área clicável e contraste WCAG AA.
   - **CORS em API Web:** `SecurityConfig.java` permite apenas `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` (não use `PATCH`).

### Project Structure Notes

- **Arquivos Backend a CRIAR:**
  - `backend/common/src/main/resources/db/changelog/31-add-tag-scope-to-allocation-goals-and-graham-recommendations.yaml`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/PortfolioTagFilterSupport.java` (helper POJO compartilhado entre Summary/Custody/Evolution para resolver/filtrar tickers de uma Tag)
  - `backend/common/src/main/java/afsdigital/grahamselect/valuation/domain/AllocationScope.java` (normalização `"GLOBAL"` vs `tagId.toString()`, opcional se encapsulado no pacote `valuation`)
- **Arquivos Backend a ATUALIZAR:**
  - `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioSummaryUseCase.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioEvolutionUseCase.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationRequestedEvent.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationCompletedEvent.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationFailedEvent.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/AllocationGoalPort.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/PortfolioSnapshotPort.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetAllocationGoalsUseCase.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/SaveAllocationGoalsUseCase.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetGrahamRecommendationsUseCase.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/PortfolioController.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/spring/PortfolioSummaryConfiguration.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/AllocationStrategyController.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsController.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/AllocationStrategyConfiguration.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/GrahamRecommendationsConfiguration.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/AllocationGoalEntity.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repositories/AllocationGoalJpaRepository.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/GrahamRecommendationJpaRepository.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/GrahamRecommendationReadAdapter.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/ValuationRequestedPublisher.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/ValuationCompletedConsumer.java`
  - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/AllocationGoalReadEntity.java`
  - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/AllocationGoalReadJpaRepository.java`
  - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/AllocationGoalReadAdapter.java`
  - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java`
  - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/GrahamRecommendationJpaRepository.java`
  - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/GrahamRecommendationRepositoryImpl.java`
  - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapter.java`
  - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java`
- **Arquivos Frontend a CRIAR:**
  - `frontend/lib/src/features/tags/presentation/widgets/analytical_lens_tabs.dart` (contendo `AnalyticalLensTabs`, `ActiveTagLensBadge` e modal/sheet de associação rápida de ativos sem tag)
  - `frontend/test/features/tags/analytical_lens_tabs_test.dart`
- **Arquivos Frontend a ATUALIZAR:**
  - `frontend/lib/src/features/tags/presentation/widgets/dashboard_tag_filter_bar.dart`
  - `frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart`
  - `frontend/lib/src/features/portfolio/data/datasources/portfolio_remote_data_source.dart`
  - `frontend/lib/src/features/portfolio/domain/repositories/portfolio_repository.dart`
  - `frontend/lib/src/features/portfolio/data/repositories/portfolio_repository_impl.dart`
  - `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart`
  - `frontend/lib/src/features/allocation/data/datasources/allocation_remote_data_source.dart`
  - `frontend/lib/src/features/allocation/domain/repositories/allocation_repository.dart`
  - `frontend/lib/src/features/allocation/data/repositories/allocation_repository_impl.dart`
  - `frontend/lib/src/features/allocation/presentation/providers/allocation_provider.dart`
  - `frontend/lib/src/features/ranking/data/datasources/graham_recommendation_remote_data_source.dart`
  - `frontend/lib/src/features/ranking/domain/repositories/graham_recommendation_repository.dart`
  - `frontend/lib/src/features/ranking/data/repositories/graham_recommendation_repository_impl.dart`
  - `frontend/lib/src/features/ranking/presentation/providers/graham_recommendation_provider.dart`
  - `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`
  - `frontend/lib/src/features/portfolio/presentation/pages/evolution_page.dart`
  - `frontend/lib/src/features/allocation/presentation/pages/allocation_strategy_page.dart`
  - `frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart`
  - Suites de testes em `backend/*/src/test/...` e `frontend/test/...` listadas na Task 6 e nas Dev Notes.

### References

- Requisitos do Épico 8 / Story 8.2: [epics.md](file:///Users/alexandrofs/projects/graham-select/docs/bmad/planning-artifacts/epics.md#L776-L811)
- PRD (`FR15`, `NFR2`, `NFR3`): [prd.md](file:///Users/alexandrofs/projects/graham-select/docs/bmad/planning-artifacts/prd.md)
- Arquitetura & Padrões (`POJO`, Kafka, UTC, RFC 7807): [architecture.md](file:///Users/alexandrofs/projects/graham-select/docs/bmad/planning-artifacts/architecture.md), [project-context.md](file:///Users/alexandrofs/projects/graham-select/docs/bmad/project-context.md)
- Especificação UX (`AnalyticalLensTabs`, crossfade 250ms, Notion-style views): [ux-design-specification.md](file:///Users/alexandrofs/projects/graham-select/docs/bmad/planning-artifacts/ux-design-specification.md)
- História anterior (Story 8.1 — base de tags): [8-1-criacao-e-associacao-de-tags.md](file:///Users/alexandrofs/projects/graham-select/docs/bmad/implementation-artifacts/8-1-criacao-e-associacao-de-tags.md)
- Aprendizados de Retrospectiva e Dívida Técnica: [epic-6-retro-2026-11-13.md](file:///Users/alexandrofs/projects/graham-select/docs/bmad/implementation-artifacts/epic-6-retro-2026-11-13.md), [deferred-work.md](file:///Users/alexandrofs/projects/graham-select/docs/bmad/implementation-artifacts/deferred-work.md)
- Controllers & Use Cases existentes: [PortfolioController.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/PortfolioController.java), [AllocationStrategyController.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/AllocationStrategyController.java), [GrahamRecommendationsController.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsController.java), [GenerateGrahamRecommendationsUseCase.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java), [PortfolioSnapshotJpaAdapter.java](file:///Users/alexandrofs/projects/graham-select/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapter.java)
- Migrations de referência: [10-fix-deletion-request-unique-constraint.yaml](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/resources/db/changelog/10-fix-deletion-request-unique-constraint.yaml), [19-create-allocation-goals-table.yaml](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/resources/db/changelog/19-create-allocation-goals-table.yaml), [20-create-graham-recommendations-table.yaml](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/resources/db/changelog/20-create-graham-recommendations-table.yaml), [30-create-portfolio-tags-and-asset-tags.yaml](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/resources/db/changelog/30-create-portfolio-tags-and-asset-tags.yaml)
- Frontend Providers & Páginas: [tag_provider.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/tags/presentation/providers/tag_provider.dart), [dashboard_page.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart), [evolution_page.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/portfolio/presentation/pages/evolution_page.dart), [allocation_strategy_page.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/allocation/presentation/pages/allocation_strategy_page.dart), [graham_recommendations_page.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart)

## Dev Agent Record

### Agent Model Used

Antigravity Agent

### Debug Log References

- `JAVA_HOME=/Users/alexandrofs/Library/Java/JavaVirtualMachines/openjdk-22.0.1/Contents/Home mvn -f backend/pom.xml clean test` (`BUILD SUCCESS`: `common` 138/138, `api-service` 81/81, `valuation-service` 21/21; 0 failures, 0 errors, 0 skipped)
- `cd frontend && flutter analyze && flutter test` (`No issues found!`, `253` tests passed, `All tests passed!`)

### Completion Notes List

- Implementada migration Liquibase `31-add-tag-scope-to-allocation-goals-and-graham-recommendations.yaml` com colunas `tag_id` e `scope_key` (`GLOBAL` vs `tagId`), constraints únicas em `(user_id, scope_key, goal_type, target_key)` e FKs `ON DELETE CASCADE` para `portfolio_tags`.
- Adicionado filtro opcional por `tagId` nos casos de uso POJO e endpoints `/api/v1/portfolios/{summary,custody,evolution}`, `/api/v1/allocation-strategy` e `/api/v1/graham-recommendations{/trigger}`, preservando validação multi-tenant (`404 TagNotFoundException`), cooldown isolado por `(userId, scopeKey)`, timestamps `ZoneOffset.UTC`, eventos Kafka com `tagId` e snapshot de alocação restrito à Tag no `valuation-service`.
- Adicionados logs `INFO` nas entradas de API/consumidores Kafka com contexto sanitizado e logs `ERROR` com stack trace nas capturas/lançamentos de exceção (`AGENTS.md`).
- Implementados `AnalyticalLensTabs`, `ActiveTagLensBadge`, `UntaggedAssetsQuickSheet` e `TagLensSyncMixin`, integrando lentes analíticas por Tag em `DashboardPage`, `EvolutionPage`, `AllocationStrategyPage` e `GrahamRecommendationsPage`, preservando `MotivationalClockCard`, `GoalHealthChecklistCard`, `TimeToGoalCalculator` e `recalculateLocalProjection` no resumo global (`globalSummary ?? summary`) e evitando overflows em viewports compactos no gráfico de evolução e nos estados vazio/erro de recomendações.
- Validada a suíte completa após a edição final: `mvn -f backend/pom.xml clean test` (240 testes backend passando: 138 em `common`, 81 em `api-service`, 21 em `valuation-service`), `flutter analyze` (0 issues) e `flutter test` (253/253 testes frontend passando).

### File List

**Backend — Criados:**
- `backend/common/src/main/resources/db/changelog/31-add-tag-scope-to-allocation-goals-and-graham-recommendations.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/PortfolioTagFilterSupport.java`
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/domain/AllocationScope.java`

**Backend — Modificados:**
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationRequestedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationCompletedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/ValuationFailedEvent.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioSummaryUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioEvolutionUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/AllocationGoalPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/PortfolioSnapshotPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetAllocationGoalsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/SaveAllocationGoalsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetGrahamRecommendationsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/spring/PortfolioSummaryConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/PortfolioController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/ValuationCompletedConsumer.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/GrahamRecommendationReadAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/AllocationGoalEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repositories/AllocationGoalJpaRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/GrahamRecommendationJpaRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/AllocationStrategyConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/GrahamRecommendationsConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/AllocationStrategyController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsController.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/AllocationGoalReadAdapter.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/GrahamRecommendationRepositoryImpl.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapter.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/AllocationGoalReadEntity.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/AllocationGoalReadJpaRepository.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/GrahamRecommendationJpaRepository.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioEvolutionUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioSummaryUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/GetAllocationGoalsUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/valuation/application/usecase/SaveAllocationGoalsUseCaseTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/portfolio/web/PortfolioControllerIT.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/portfolio/web/PortfolioControllerTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/tags/web/TagControllerIT.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/kafka/ValuationCompletedConsumerTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/web/AllocationStrategyIT.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsIT.java`
- `backend/valuation-service/src/test/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerServiceTest.java`
- `backend/valuation-service/src/test/java/afsdigital/grahamselect/valuation/infrastructure/persistence/GrahamRecommendationRepositoryImplIT.java`
- `backend/valuation-service/src/test/java/afsdigital/grahamselect/valuation/infrastructure/persistence/GrahamRecommendationRepositoryImplTest.java`
- `backend/valuation-service/src/test/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapterIT.java`
- `backend/valuation-service/src/test/java/afsdigital/grahamselect/valuation/infrastructure/persistence/PortfolioSnapshotJpaAdapterTest.java`

**Frontend — Criados:**
- `frontend/lib/src/features/tags/presentation/widgets/analytical_lens_tabs.dart`
- `frontend/test/features/ranking/data/datasources/graham_recommendation_remote_data_source_test.dart`
- `frontend/test/features/ranking/presentation/providers/graham_recommendation_provider_test.dart`
- `frontend/test/features/tags/analytical_lens_tabs_test.dart`

**Frontend — Modificados:**
- `frontend/lib/src/features/allocation/data/datasources/allocation_remote_data_source.dart`
- `frontend/lib/src/features/allocation/data/repositories/allocation_repository_impl.dart`
- `frontend/lib/src/features/allocation/domain/repositories/allocation_repository.dart`
- `frontend/lib/src/features/allocation/presentation/pages/allocation_strategy_page.dart`
- `frontend/lib/src/features/allocation/presentation/providers/allocation_provider.dart`
- `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`
- `frontend/lib/src/features/portfolio/data/datasources/portfolio_remote_data_source.dart`
- `frontend/lib/src/features/portfolio/data/repositories/portfolio_repository_impl.dart`
- `frontend/lib/src/features/portfolio/domain/repositories/portfolio_repository.dart`
- `frontend/lib/src/features/portfolio/presentation/pages/evolution_page.dart`
- `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart`
- `frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart`
- `frontend/lib/src/features/ranking/data/datasources/graham_recommendation_remote_data_source.dart`
- `frontend/lib/src/features/ranking/data/repositories/graham_recommendation_repository_impl.dart`
- `frontend/lib/src/features/ranking/domain/repositories/graham_recommendation_repository.dart`
- `frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart`
- `frontend/lib/src/features/ranking/presentation/providers/graham_recommendation_provider.dart`
- `frontend/lib/src/features/tags/presentation/widgets/dashboard_tag_filter_bar.dart`
- `frontend/test/features/allocation/data/datasources/allocation_remote_data_source_test.dart`
- `frontend/test/features/allocation/presentation/pages/allocation_strategy_page_test.dart`
- `frontend/test/features/allocation/presentation/providers/allocation_provider_test.dart`
- `frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart`
- `frontend/test/features/dashboard/presentation/pages/dashboard_page_test.mocks.dart`
- `frontend/test/features/goals/presentation/pages/goals_page_test.dart`
- `frontend/test/features/portfolio/data/datasources/portfolio_remote_data_source_test.dart`
- `frontend/test/features/portfolio/data/repositories/portfolio_repository_impl_test.dart`
- `frontend/test/features/portfolio/presentation/pages/evolution_page_test.dart`
- `frontend/test/features/portfolio/presentation/providers/portfolio_provider_test.dart`
- `frontend/test/features/portfolio/presentation/widgets/financial_data_table_test.dart`
- `frontend/test/features/ranking/presentation/pages/graham_recommendations_page_flow_test.dart`
- `frontend/test/features/ranking/presentation/pages/graham_recommendations_page_test.dart`
- `frontend/test/features/tags/tags_dashboard_integration_test.dart`

**Documentação:**
- `docs/bmad/implementation-artifacts/8-2-filtros-de-dashboard-e-recomendacoes-por-tag.md`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`
