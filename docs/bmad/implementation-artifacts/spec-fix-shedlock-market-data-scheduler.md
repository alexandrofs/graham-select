---
title: 'Fix ShedLock on MarketDataScheduler'
type: 'bugfix'
created: '2026-06-13'
status: 'done'
context: []
baseline_commit: '5363679f506e97b16db9e8a6613b030061db8f07'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O scheduler `MarketDataScheduler` roda usando `@Scheduled` local sem ShedLock. Em ambientes multi-instância (produção), ele rodará concorrentemente em todas as instâncias, duplicando requisições a APIs externas e operações de banco de dados.

**Approach:** Adicionar o ShedLock ao projeto Spring Boot configurado com o `JdbcTemplateLockProvider` no banco MySQL, criar a tabela `shedlock` via migração do Liquibase (incluindo suporte a rollback) e decorar o método do scheduler com `@SchedulerLock`.

## Boundaries & Constraints

**Always:**
- Utilizar a tabela `shedlock` padrão para controle do lock.
- Garantir suporte a rollback na migração do Liquibase para dropar a tabela `shedlock` caso necessário.
- Configurar o default lock time de forma segura (ex: `defaultLockAtMostFor = "10m"`).

**Ask First:**
- N/A

**Never:**
- Não usar locks baseados em memória local para sincronização multi-instância.

</frozen-after-approval>

## Code Map

- [pom.xml](file:///Users/alexandrofs/projects/graham-select/backend/api/pom.xml) -- Arquivo de dependências da API para adicionar as bibliotecas do ShedLock.
- [db.changelog-master.yaml](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/resources/db/changelog/db.changelog-master.yaml) -- Changelog mestre do Liquibase para registrar a migração de criação da tabela de lock.
- [24-create-shedlock-table.yaml](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/resources/db/changelog/24-create-shedlock-table.yaml) -- [NEW] Migração Liquibase para criar a tabela `shedlock`.
- [ShedLockConfiguration.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/ShedLockConfiguration.java) -- [NEW] Classe de configuração para registrar o `LockProvider` e habilitar o ShedLock no Spring.
- [MarketDataScheduler.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java) -- Classe do scheduler para decorar o método `syncMarketData` com `@SchedulerLock`.

## Tasks & Acceptance

**Execution:**
- [x] `backend/api/pom.xml` -- Adicionar dependências `net.javacrumbs.shedlock:shedlock-spring:5.13.0` e `net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.13.0` -- Prover suporte a ShedLock na API.
- [x] `backend/common/src/main/resources/db/changelog/24-create-shedlock-table.yaml` -- Criar arquivo com a definição da tabela `shedlock` e sua query de rollback -- Garantir a persistência dos locks distribuídos.
- [x] `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` -- Registrar o arquivo de changelog `24-create-shedlock-table.yaml` no master -- Incluir a migração no ciclo do Liquibase.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/ShedLockConfiguration.java` -- Criar classe com `@Configuration` e `@EnableSchedulerLock` instanciando o `LockProvider` -- Configurar e habilitar ShedLock na aplicação.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataScheduler.java` -- Adicionar `@SchedulerLock(name = "MarketDataScheduler_syncMarketData", lockAtLeastFor = "1m", lockAtMostFor = "10m")` ao método `syncMarketData` -- Garantir exclusão mútua distribuída na execução do scheduler.

**Acceptance Criteria:**
- Given a MySQL database, when the application starts up, then the `shedlock` table must be created successfully via Liquibase.
- Given the `MarketDataScheduler`, when `syncMarketData` executes, then the lock is acquired, and a row for the lock is inserted/updated in the `shedlock` table.
- Given a rollback command, when Liquibase rollback is executed, then the `shedlock` table is dropped successfully.

## Verification

**Commands:**
- `mvn clean test` na pasta `backend` -- expected: compilação com sucesso e todos os testes unitários passando.
