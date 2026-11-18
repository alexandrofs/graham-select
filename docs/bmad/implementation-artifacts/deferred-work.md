# Deferred Work

Este arquivo registra itens de dívida técnica ou melhorias adiadas durante as revisões de código e sprints do projeto.

## Deferred from: code review of 4-1-market-data-indicator-integration (2026-06-06)

(Nenhum item pendente)

## Deferred from: code review of 4-2-premium-allocation-strategy-config.md (2026-06-08)

(Nenhum item pendente)

## Deferred from: code review of 4-3-graham-valuation-engine.md (2026-06-12)

- Quebra de ISP na GrahamRecommendationPort [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java:1] — A interface unifica leitura e escrita, obrigando adapters de leitura a lançar UnsupportedOperationException nos métodos de escrita.
- Uso de Strings Mágicas em Filtros de Regra de Negócio [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:1] — Utiliza a String "TICKER" hardcoded no usecase em vez de um enum de tipo de meta.
- Mapeamento de Datas como String em Eventos [backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java:1] — Os eventos do Kafka de recomendação usam String para trafegar timestamps de solicitação e conclusão em vez de tipos temporais nativos, conforme instruído na especificação.

## Deferred from: code review of 4-4-explainable-ai-reasoning-box.md (2026-06-13)

- Risco de Quebra em Tempo de Execução na Desserialização do JSON [frontend/lib/src/features/ranking/data/models/graham_recommendation_model.dart:584] — O cast direto do JSON num? as double? pode falhar se o Java serializar como String.
- Rolagem Conflitante no Bottom Sheet [frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart:1] — Conflito potencial de gestos entre o SingleChildScrollView interno e a folha deslizável nativa.

## Deferred from: code review of 5-1-wealth-income-goal-setup.md (2026-06-13)

(Nenhum item pendente)

## Deferred from: code review of 6-1-admin-observability-dashboard.md (2026-10-25)

- Validação de `sub` e `email` nulos na conversão OAuth2 [backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java:34] — Comportamento pré-existente da Story 1.1 na extração de claims do JWT.
- Recuperação de concorrência dentro da mesma transação em `OAuth2UserProvisioningConverter.convert()` [backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java:72] — Bloco catch pré-existente da Story 1.1 dentro de método anotado com `@Transactional`.

## Deferred from: code review of 7-1-vinculo-de-novo-cpf-a-conta-matriz (2026-11-13)
- Implementar `Persistable<UUID>` em `FamilyProfileEntity` (`backend/api/src/main/java/afsdigital/grahamselect/api/family/infrastructure/persistence/jpa/entities/FamilyProfileEntity.java`) para evitar `SELECT` prévio antes de `INSERT` em `FamilyProfileJpaAdapter.save()`.

## Deferred from: code review of 9-2-web-crawler-de-proventos-e-fatos-relevantes.md (2026-11-15)

- Endpoints Ideais e Protocolo Rad/ENET da CVM [backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/CvmOfficialCrawlerAdapter.java:467] — A extração avançada de formulários e autenticação na sessão do sistema CVM Rad/ENET requer engenharia reversa de protocolo governamental além do escopo da história 9.2, onde Brapi opera como fonte primária robusta.
- Consulta de Agenda com Expressão COALESCE Sem Índice Funcional e Sem Paginação [backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/jpa/repositories/DividendAnnouncementJpaRepository.java:1131] — Em grande escala de dados, `COALESCE(paymentDate, comDate)` impede uso de índices B-Tree padrão e ausência de `Pageable` pode onerar memória.

## Deferred from: code review of 9-3-conector-open-finance.md (2026-11-17)

- Integração com Agregador Real de Produção vs Mock Sandbox [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/OpenFinanceConnectorAdapter.java:180] — A integração com agregadores reais (Pluggy, Celcoin ou APIs Bacen FAPI) em modo não-mock requer credenciamento formal e contratação de parceiro de Open Finance, mantendo-se o MockOpenFinanceConnectorAdapter como driver ativo de testes/sandbox.
- Reconciliação direta de custódia vs derivação por transações de trades [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java:88] — No Graham-Select a custódia consolidada é derivada reativamente do razão de transações de compra/venda pelo valuation-service. A reconciliação forçada de saldo estático fica diferida como evolução de auditoria de custódia.
- Processamento sequencial em thread única de conexões ativas no Scheduler em alta escala [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/spring/OpenFinanceSyncScheduler.java:36] — Para a escala atual o agendador sequencial atende com folga dentro da janela do ShedLock. Paginação com pool de workers concorrentes fica adiada para cenário de milhares de conexões ativas.
