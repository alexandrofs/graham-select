---
title: 'Correção de Erros no Log da API (UUID no MySQL e Brapi Adapters)'
type: 'bugfix'
created: '2026-06-06T10:15:00-03:00'
status: 'done'
baseline_commit: '0172d32bdbb99e042f32d50d07dfc76b10746252'
context:
  - '{project-root}/docs/bmad/project-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O banco de dados MySQL local falha ao persistir UUIDs no formato BINARY(16) gerado pelo Liquibase sem indicação JDBC para VARCHAR (gerando erro SQL 1366 na inserção de auditorias em `manual_trade_audits`). Além disso, a chamada em lote para a Brapi API falha com HTTP 400 se houver tickers fracionários (com terminação "F") no lote, descartando as atualizações de todos os outros ativos.

**Approach:** Atualizar o tipo de dados no banco local e na entidade `ManualTradeAuditEntity` de `UUID` (padrão) para `CHAR(36)` por meio de um novo changeset do Liquibase e da anotação `@JdbcTypeCode(SqlTypes.VARCHAR)`. No `BrapiMarketDataAdapter`, mapear tickers fracionários (terminação "F") para tickers base para consulta à API Brapi, de modo que a Brapi retorne as informações da ação comum, e então mapear o resultado de volta para o ticker fracionário original. Em caso de erro 400 em lote, tentar obter dados individualmente para isolar e ignorar apenas os tickers problemáticos. Adicionar a migration 17 em falta ao `db.changelog-master.yaml` para evitar a ausência da tabela `ingestion_audits`.

## Boundaries & Constraints

**Always:** As datas devem usar UTC de forma consistente (ZoneOffset.UTC). Devem ser mantidos todos os testes e a análise estática passando com sucesso. Devem ser registrados logs em nível INFO nas entradas de API e em nível ERROR em caso de exceções nos adaptadores de banco de dados e APIs externas.

**Ask First:** Nenhuma decisão que exija aprovação direta.

**Never:** Não realizar chamadas síncronas entre os módulos `api` e `valuation-service`. Não alterar a migração 15 original (já aplicada); criar uma nova migração corretiva.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Persistência de Auditoria de Trade | `ManualTradeAuditEntity` com `id` e `tradeId` como UUID válidos | Registro salvo como `CHAR(36)` no MySQL sem erro SQL 1366 | Lançar erro apropriado se falhar |
| Consulta à Brapi com Tickers Fracionários | Tickers `["BBSE3F", "PETR4"]` | Chamada à Brapi por `["BBSE3", "PETR4"]` com resultados associados a `BBSE3F` e `PETR4` | Fallback para cache individual se falhar |
| Lote da Brapi falha com 400 | Tickers `["INVALIDO", "PETR4"]` | Retentativa individual: `INVALIDO` falha e gera log, `PETR4` obtém dados com sucesso | Log de erro e fallback para cache do ticker inválido |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` -- Contém as migrações ativas e sua ordem de execução.
- `backend/common/src/main/resources/db/changelog/18-fix-manual-trade-audits-uuid-columns.yaml` -- Nova migração do Liquibase para alterar colunas `id` e `trade_id` de `manual_trade_audits` para `CHAR(36)`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/entities/ManualTradeAuditEntity.java` -- Entidade JPA contendo a definição e mapeamento das colunas com `@JdbcTypeCode(SqlTypes.VARCHAR)`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java` -- Adaptador da Brapi que realiza o mapeamento de tickers fracionários e a recuperação individual resiliente em caso de falha de lote.
- `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java` -- Testes unitários para validar a lógica de recuperação de dados e tratamento de fracionários e falhas de lote.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` -- Adicionar migrações 17 (se ausente) e 18 ao arquivo do Liquibase master -- Garante a execução correta na inicialização.
- [x] `backend/common/src/main/resources/db/changelog/18-fix-manual-trade-audits-uuid-columns.yaml` -- Criar changeset do Liquibase para alterar o tipo de `id` e `trade_id` em `manual_trade_audits` para `CHAR(36)` -- Compatibilidade MySQL.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/entities/ManualTradeAuditEntity.java` -- Adicionar anotação `@JdbcTypeCode(SqlTypes.VARCHAR)` aos campos `id` e `tradeId` -- Garante consistência de persistência UUID.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java` -- Implementar mapeamento de tickers com final 'F' e lógica de retentativa individual se o lote retornar erro -- Resiliência e correção de chamadas à Brapi.
- [x] `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java` -- Adicionar testes unitários para o mapeamento de fracionários e retentativa individual -- Cobertura de novos fluxos de erro e edge cases.

**Acceptance Criteria:**
- Given a manual trade audit creation trigger, when the database is MySQL, then the audit record is saved in `manual_trade_audits` with `CHAR(36)` columns without SQL Error 1366.
- Given a request for market data containing fractional tickers, when querying via the Brapi adapter, then the API is queried with base tickers and the results are mapped back to fractional symbols.
- Given a batch request that returns HTTP 400, when querying via the Brapi adapter, then individual fallbacks are triggered and valid tickers in the batch are still successfully updated.

## Verification

**Commands:**
- `mvn clean test -pl backend/api,backend/common` -- expected: Sucesso em todos os testes do backend (compilação e execução).

## Suggested Review Order

**Banco de Dados & Persistência**

- Mapeamento JPA para UUID na entidade ManualTradeAuditEntity
  [`ManualTradeAuditEntity.java:23`](../../../backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/entities/ManualTradeAuditEntity.java#L23)

- Criação da nova migration para ajustar colunas id/trade_id de UUID para CHAR(36) no MySQL
  [`18-fix-manual-trade-audits-uuid-columns.yaml:1`](../../../backend/common/src/main/resources/db/changelog/18-fix-manual-trade-audits-uuid-columns.yaml#L1)

- Inclusão das migrações 17 e 18 no changelog master
  [`db.changelog-master.yaml:31`](../../../backend/common/src/main/resources/db/changelog/db.changelog-master.yaml#L31)

**Integração de Market Data (Brapi)**

- Lógica de mapeamento de tickers fracionários e retentativas individuais em falhas de lote
  [`BrapiMarketDataAdapter.java:34`](../../../backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java#L34)

**Validação de Testes**

- Cobertura de testes unitários para fracionários e retentativas individuais
  [`BrapiMarketDataAdapterTest.java:210`](../../../backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java#L210)
