# Deferred Work

Este arquivo registra itens de dívida técnica ou melhorias adiadas durante as revisões de código e sprints do projeto.

## Deferred from: code review of 4-1-market-data-indicator-integration (2026-06-06)

- Ausência de bloqueio distribuído no Scheduler (ShedLock) [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java] — O scheduler usa @Scheduled local sem ShedLock. Em cluster multi-instância, rodará em paralelo e duplicará requisições.
