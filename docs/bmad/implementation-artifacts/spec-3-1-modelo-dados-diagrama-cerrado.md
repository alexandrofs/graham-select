---
title: 'Story 3.1: Modelo de Dados Relacional e Migrations para o Diagrama do Cerrado'
type: 'feature'
created: '2026-11-27'
status: 'done'
baseline_commit: '809e725bc003361aea7c490c32cbdeda969a8837'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '/Users/alexandrofs/projects/graham-select/docs/bmad/implementation-artifacts/epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O sistema Kerno ainda não possui persistência relacional para a estratégia de alocação do Diagrama do Cerrado (metas percentuais das 5 Macro Classes, multiplicador de Graham e notas de qualidade por ativo). Sem essas tabelas e entidades dedicadas, os motores de cálculo (Story 3.2), APIs (Story 3.3), GPS de Aporte (Épico 4) e Kerno AI (Épico 5) não conseguem salvar ou isolar configurações por usuário com precisão decimal.

**Approach:** Criar a migration Liquibase incremental e idempotente `39-create-cerrado-tables.yaml` definindo `tb_cerrado_preferences` e `tb_cerrado_asset_grade` com integridade referencial para `users(id)`, implementar os enums/modelos de domínio e portas em `backend/common`, os adaptadores/entidades JPA em `backend/api`, e integrar ambas as tabelas ao expurgo LGPD (`UserDataPurgeAdapter` e `AccountDeletionIT`).

## Boundaries & Constraints

**Always:**
- Usar exclusivamente `BigDecimal` (zero `float`/`double`) no domínio e entidades JPA, e colunas `DECIMAL(5,2)` / `DECIMAL(4,2)` no banco.
- Criar migration YAML incremental (`39-create-cerrado-tables.yaml`) com **dois `changeSet`s separados** (um por tabela), cada um com `preConditions` (`onFail: MARK_RAN`, `not tableExists`) e bloco `rollback`, sem alterar migrations já aplicadas (`01` a `38`).
- Garantir isolamento estrito por `user_id` (`BIGINT` FK -> `users(id)` com `onDelete: CASCADE`) com unicidade por usuário em `tb_cerrado_preferences` (`uq_cerrado_preferences_user`) e por `(user_id, ticker)` em `tb_cerrado_asset_grade` (`uq_cerrado_asset_grade_user_ticker`).
- Usar `@PrePersist` e `@PreUpdate` com `LocalDateTime.now(ZoneOffset.UTC)` para `updated_at` nas entidades JPA (evitando `@LastModifiedDate`, pois `@EnableJpaAuditing` no projeto não define `DateTimeProvider` UTC).
- Incluir `tb_cerrado_asset_grade` e `tb_cerrado_preferences` no `UserDataPurgeAdapter` (usando `Long userId`) e cobrir em `AccountDeletionIT` (incluindo limpeza no `cleanUp()`).

**Never:**
- Implementar o motor de cálculo/redistribuição de metas (`CerradoAllocationEngine`, escopo da Story 3.2) ou endpoints REST (escopo da Story 3.3).
- Usar `float`/`double` em campos ou cálculos percentuais.
- Renomear identificadores internos legados (`afsdigital.grahamselect.*`, `users`).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Persistência de preferências válidas | `user_id` existente, 5 metas em `BigDecimal` (`NUMERIC(5,2)`), `graham_multiplier = 1.30`, `graham_filter_enabled = true` | Registro salvo em `tb_cerrado_preferences` com precisão exata de 2 casas decimais | N/A |
| Unicidade de preferências por usuário | Segundo registro em `tb_cerrado_preferences` para o mesmo `user_id` | Rejeição pela constraint `uq_cerrado_preferences_user` | `DataIntegrityViolationException` |
| Nota de qualidade fora do intervalo 1–10 | `quality_grade = 0` ou `11` em `CerradoAssetGrade` / entidade | Rejeição na validação de domínio/bean validation antes ou na persistência | `IllegalArgumentException` / `ConstraintViolationException` |
| Unicidade de ativo por usuário | Segundo registro em `tb_cerrado_asset_grade` para mesmo `(user_id, ticker)` | Rejeição pela constraint `uq_cerrado_asset_grade_user_ticker` | `DataIntegrityViolationException` |
| Expurgo LGPD de conta | Usuário com registros em ambas as tabelas solicita exclusão e job de expurgo executa | Registros em `tb_cerrado_asset_grade` e `tb_cerrado_preferences` removidos (`count == 0`) | Log `ERROR` com stack trace e relançamento se falhar |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` -- Master changelog Liquibase; registrar `39-create-cerrado-tables.yaml` ao final.
- `backend/common/src/main/resources/db/changelog/37-create-founder-quota-and-add-user-commercial-plan.yaml` -- Referência de padrão recente (`author: bmad-dev`, `preConditions` `MARK_RAN`, `rollback`).
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java` -- Entidade `users` com PK `Long id`; não alterar.
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/AllocationGoalEntity.java` -- Referência de entidade JPA com `BigDecimal` em `backend/api`; não alterar.
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/persistence/UserDataPurgeAdapter.java` -- Adaptador de expurgo LGPD; adicionar remoção por `Long userId` para as duas novas tabelas.
- `backend/api/src/test/java/afsdigital/grahamselect/api/user/AccountDeletionIT.java` -- Teste de integração LGPD; limpar e validar expurgo das duas novas tabelas.
- `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/BaseRepositoryIT.java` -- Classe base H2 para testes `*IT` em `backend/api`.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/resources/db/changelog/39-create-cerrado-tables.yaml` e `db.changelog-master.yaml` -- Criar 2 `changeSet`s idempotentes (`id` PK `BIGINT autoIncrement: true`): (1) `tb_cerrado_preferences` (`user_id` `BIGINT` FK `onDelete: CASCADE` + `uq_cerrado_preferences_user`, `target_acoes_br`, `target_fiis`, `target_renda_fixa`, `target_internacional`, `target_cripto` `DECIMAL(5,2) NOT NULL`, `graham_multiplier` `DECIMAL(4,2) NOT NULL` default `1.30`, `graham_filter_enabled` `BOOLEAN NOT NULL` default `true`, `updated_at` `DATETIME NOT NULL`); (2) `tb_cerrado_asset_grade` (`user_id` `BIGINT` FK `onDelete: CASCADE` + `idx_cerrado_asset_grade_user_id`, `ticker` `VARCHAR(50) NOT NULL`, `macro_class` `VARCHAR(30) NOT NULL`, `sub_class` `VARCHAR(50) NOT NULL`, `quality_grade` `INT NOT NULL` default `6`, `custom_risk_ceiling` `DECIMAL(5,2)` nullable, `quarantine_corporate_event` `BOOLEAN NOT NULL` default `false`, `updated_at` `DATETIME NOT NULL`, `uq_cerrado_asset_grade_user_ticker` em `(user_id, ticker)`) -- Atende schema relacional, precisão decimal e idempotência Liquibase.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/...` e `application/repository/...` -- Criar enums `MacroClass` (`ACOES_BR`, `FIIS`, `RENDA_FIXA`, `INTERNACIONAL`, `CRIPTO`) e `CerradoSubClass` (`EQUITY_DEFENSIVE`, `EQUITY_GROWTH`, `FII_BRICK`, `FII_PAPER`, `FII_FOF`, `TREASURY_POST_FIXED`, `TREASURY_IPCA`, `TREASURY_PREFIXED`, `FIXED_INCOME_PRIVATE`, `INTERNATIONAL_EQUITY`, `INTERNATIONAL_ETF`, `CRYPTO_MAJOR`, `OUTROS_MANUAL`), modelos de domínio `CerradoPreferences` e `CerradoAssetGrade` (lançando `IllegalArgumentException` para `qualityGrade` fora de `1..10` ou `BigDecimal` nulo), e portas `CerradoPreferencesPort` / `CerradoAssetGradePort` -- Separa domínio/portas em `common` conforme Clean Architecture.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/cerrado/infrastructure/persistence/...` -- Criar entidades JPA `CerradoPreferencesEntity` e `CerradoAssetGradeEntity` (`@Enumerated(EnumType.STRING)`, `@Min(1) @Max(10)` em `qualityGrade`, `@PrePersist`/`@PreUpdate` com `LocalDateTime.now(ZoneOffset.UTC)`), repositórios Spring Data JPA e adaptadores implementando as portas de `common` -- Isola infraestrutura JPA em `api`.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/persistence/UserDataPurgeAdapter.java` e `backend/api/src/test/java/afsdigital/grahamselect/api/user/AccountDeletionIT.java` -- Adicionar deleção de `tb_cerrado_asset_grade` e `tb_cerrado_preferences` por `Long userId`, limpar em `AccountDeletionIT.cleanUp()` e validar expurgo em `shouldPerformHardDeleteDuringPurge()` -- Cumpre política obrigatória LGPD/FR5.
- [x] `backend/common/src/test/...` e `backend/api/src/test/java/afsdigital/grahamselect/api/cerrado/CerradoPersistenceIT.java` -- Criar testes unitários de domínio (validação de nota 1–10, defaults, `BigDecimal`) e `*IT` (com `@AfterEach` limpando dados) validando migration, precisão decimal, unicidade, isolamento por `user_id` e `ON DELETE CASCADE` -- Garante cobertura da matriz I/O e critérios da história.

**Acceptance Criteria:**
- Given a migration `39-create-cerrado-tables.yaml` aplicada via Liquibase em banco limpo ou já migrado, when o contexto Spring Boot sobe, then `tb_cerrado_preferences` e `tb_cerrado_asset_grade` existem com FKs para `users(id)`, índices em `user_id` e colunas `DECIMAL` sem uso de ponto flutuante.
- Given o fluxo de exclusão de conta em `AccountDeletionIT`, when `purgeScheduler.processPendingPurges()` executa, then todos os registros do usuário em `tb_cerrado_asset_grade` e `tb_cerrado_preferences` são removidos permanentemente.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Layer | Location | Claim | Verdict | Route / Disposition | Evidence |
|---|---|---|---|---|---|
| edge-case-hunter | `CerradoPreferences.java:819-833` | Negative, >100, or non-100%-sum targets or non-positive/overflowing `grahamMultiplier` | `low` | Rejected | Nenhum chamador na Story 3.1 envia entrada externa não confiável; soma 100% e validações de negócio pertencem às Stories 3.2/3.3. |
| edge-case-hunter | `CerradoAssetGrade.java:740-762` | `ticker` > 50 chars or negative/overflowing `customRiskCeiling` | `low` | Rejected | Nenhum chamador na Story 3.1 envia entrada não validada; banco impõe `VARCHAR(50)`/`DECIMAL(5,2)` e regras de negócio pertencem às Stories 3.2/3.3. |
| edge-case-hunter | `CerradoAssetGrade.java:743-748` | `subClass` does not belong to `macroClass` | `low` | Rejected | Regras de hierarquia entre `MacroClass` e `CerradoSubClass` são escopo da Story 3.2 (`CerradoAllocationEngine`); nenhum chamador na Story 3.1 envia pares inválidos. |
| edge-case-hunter | `CerradoAssetGradePersistenceAdapter.java:43-46` | `save(null)` causes unguarded NPE in `toEntity` | `false` | Rejected | Nenhum chamador passa `null` para `save()`, e `toEntity` falha ruidosamente caso ocorra. |
| edge-case-hunter | `CerradoPreferencesPersistenceAdapter.java:115-118` | `save(null)` causes unguarded NPE in `toEntity` | `false` | Rejected | Nenhum chamador passa `null` para `save()`, e `toEntity` falha ruidosamente caso ocorra. |
| edge-case-hunter | `CerradoAssetGradeEntity.java:203-221` | Direct JPA entity persistence bypasses domain invariants | `low` | Rejected | Entidade é acessada via `CerradoAssetGradePersistenceAdapter` (`CerradoAssetGradePort`); nenhum chamador persiste a entidade diretamente com dados inválidos. |
| blind-hunter | `CerradoPreferences.java` / `CerradoPreferencesEntity.java` | Falta validação `< 0`, `> 100`, `grahamMultiplier <= 0` e Bean Validation | `low` | Rejected | Nenhum chamador na Story 3.1 envia entrada externa; regras de negócio e DTOs de entrada pertencem às Stories 3.2/3.3. |
| blind-hunter | `CerradoAssetGrade.java` / `CerradoAssetGradeEntity.java` | `ticker` sem `trim`/uppercase e limite 50; `customRiskCeiling` sem range; sem coerência macro/sub | `low` | Rejected | Normalização e regras de hierarquia são escopo das Stories 3.2/3.3; nenhum chamador na Story 3.1 envia valores inválidos. |
| blind-hunter | `39-create-cerrado-tables.yaml` | Faltam constraints `CHECK` no banco (`quality_grade`, percentuais) | `low` | Rejected | Nenhum escritor SQL direto existe na Story 3.1; migration segue exatamente o schema especificado na história. |
| blind-hunter | `39-create-cerrado-tables.yaml` | Omite coluna `created_at` | `false` | Rejected | A especificação da Story 3.1 define intencionalmente apenas `updated_at`, sem requisito de `created_at`. |
| blind-hunter | `CerradoAssetGradeEntity.java` / `CerradoPreferencesEntity.java` | Uso de `@Data` do Lombok em entidades JPA | `low` | Rejected | Os adaptadores convertem imediatamente entidades em modelos de domínio imutáveis (`toDomain`/`toEntity`) e nunca comparam entidades em coleções `Set`. |
| blind-hunter | `CerradoAssetGradeJpaRepository.java` / `CerradoPreferencesJpaRepository.java` | `deleteByUserId` faz `SELECT` + deletes individuais e falta teste na porta | `low` | Rejected | `deleteByUserId` não está em caminho crítico na Story 3.1 (expurgo LGPD usa `JdbcTemplate` direto); `ON DELETE CASCADE` e expurgo estão cobertos em `*IT`. |
| blind-hunter | `CerradoPersistenceIT.java` | Faltam testes de `@PreUpdate`, consultas vazias e `customRiskCeiling` | `low` | Rejected | `customRiskCeiling` já é testado nas linhas 498 e 516 (`8.50`); `@PrePersist` já valida `updatedAt` e consultas vazias delegam ao Spring Data padrão. |
| blind-hunter | `AccountDeletionIT.java` | `processPendingPurges()` deleta `users` com `ON DELETE CASCADE`, não isolando `UserDataPurgeAdapter` | `false` | Rejected | `UserDataPurgeAdapter` executa `DELETE` explícito em ambas as tabelas com `Long userId` antes da remoção do usuário, seguindo o padrão de todas as tabelas em `AccountDeletionIT`. |
| blind-hunter | `sprint-status.yaml` / `spec-3-1-modelo-dados-diagrama-cerrado.md` | Status `in-progress` no diff e `Implementation Notes` vazio | `false` | Rejected | O diff foi capturado antes da transição para `in-review` e `done` ocorre no passo final (`step-05-present`). |

## Design Notes

- Em `UserDataPurgeAdapter`, tabelas legadas usam `user_id` `VARCHAR` (`String.valueOf(userId)` e `googleSub`). Como `tb_cerrado_preferences.user_id` e `tb_cerrado_asset_grade.user_id` são `BIGINT` (FK para `users.id`), o expurgo dessas duas tabelas deve passar `Long userId` diretamente ao `JdbcTemplate` (fora do loop de `googleSub` string) para evitar erro de conversão de tipo em H2/MySQL.

## Verification

**Commands:**
- `mvn clean test` (em `backend/`) -- expected: 100% dos testes unitários aprovados, zero erros.
- `mvn test -pl api -Dtest=CerradoPersistenceIT,AccountDeletionIT` (em `backend/`) -- expected: Liquibase aplica migration 39 no H2 e todos os testes de integração passam.
