# Deferred Work

Este arquivo registra itens de dívida técnica ou melhorias adiadas durante as revisões de código e sprints do projeto.

## Deferred from: code review of 4-1-market-data-indicator-integration (2026-06-06)

- Ausência de bloqueio distribuído no Scheduler (ShedLock) [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java] — O scheduler usa @Scheduled local sem ShedLock. Em cluster multi-instância, rodará em paralelo e duplicará requisições.

## Deferred from: code review of 4-2-premium-allocation-strategy-config.md (2026-06-08)

- Risco de Race Condition no Fluxo de Delete+Insert do JPA Adapter [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java:62] — A abordagem de deletar todas as metas de um usuário e depois inserir as novas pode sofrer com condições de corrida se duas transações concorrentes executarem para o mesmo userId simultaneamente.

## Deferred from: code review of 4-3-graham-valuation-engine.md (2026-06-12)

- Quebra de ISP na GrahamRecommendationPort [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java:1] — A interface unifica leitura e escrita, obrigando adapters de leitura a lançar UnsupportedOperationException nos métodos de escrita.
- Uso de Strings Mágicas em Filtros de Regra de Negócio [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:1] — Utiliza a String "TICKER" hardcoded no usecase em vez de um enum de tipo de meta.
- Mapeamento de Datas como String em Eventos [backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java:1] — Os eventos do Kafka de recomendação usam String para trafegar timestamps de solicitação e conclusão em vez de tipos temporais nativos, conforme instruído na especificação.
