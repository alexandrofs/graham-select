---
title: 'Story 2.5: Adaptação Visual da Ingestão B3 e CRUD Manual Multi-Classe'
type: 'feature'
created: '2026-11-27'
status: 'ready-for-dev'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O `DashboardPage` atual possui apenas um FAB `"Nova Operação"` com formulário legado sem suporte às 5 Macro Classes, edição/exclusão manual ou upload B3 em modal; `KafkaB3FileUploadedConsumer` não descarta o arquivo bruto com PII após o parsing; `TradeManualController` expõe apenas `POST /api/v1/trades/manual` sem `assetClass` nem `GET`/`PUT`/`DELETE`; e `GetCustodyPositionsUseCase` classifica ETFs terminados em `11` (`IVVB11`, `HASH11`) como `FIIs` e não suporta `Internacional`/`Criptoativos`.

**Approach:** Entregar o `B3IngestionCrudSheet` responsivo (Bottom Sheet no Mobile / Drawer `480px` na Web) acionado pelo CTA `"Atualizar Carteira / + Ativo"` e pelo estado Zero-Blank-Screen do `DashboardPage`, com abas de Upload B3 (`.xlsx`/`.pdf` SINACOR) e CRUD Manual nas 5 Macro Classes em `BRL`; e no backend descartar o arquivo bruto no `finally` do consumidor Kafka, implementar CRUD completo (`GET`/`POST`/`PUT`/`DELETE`) em `/api/v1/trades/manual` com `assetClass`/`profileId` opcionais e corrigir a precedência/inferência de classes em `GetCustodyPositionsUseCase`.

## Boundaries & Constraints

**Always:**
- Usar tokens do Design System Intergeracional (`KernoThemeData.of(context)`, `KernoColors`, `KernoSpacing`, `KernoRadius`, `KernoTypography` com `FontFeature.tabularFigures()`), contraste WCAG AA/AAA, alvos `>= 48x48dp` e zero `TextOverflow.ellipsis` em saldos, tickers ou percentuais.
- Integrar `PrivacyProvider` em valores monetários em `R$` exibidos no sheet/CRUD mantendo percentuais visíveis.
- Manter `@AuthenticationPrincipal Jwt jwt` (`jwt.getSubject()`) em `TradeManualController` para compatibilidade brownfield com `trades` e `GetCustodyPositionsUseCase`; manter `assetClass` e `profileId` opcionais em `ManualTradeRequest` (validando ownership de `profileId` via `FamilyProfilePort` quando informado) e `http.Client` em `PortfolioRemoteDataSource`.
- Descartar o arquivo bruto B3/SINACOR em disco (`Files.deleteIfExists`) no `finally` de `KafkaB3FileUploadedConsumer` após `failProcessing`/conclusão (sem relançar `IOException` após falha terminal, evitando retry sobre arquivo removido).
- Usar migration Liquibase incremental e idempotente (`preConditions`) e timestamps em `ZoneOffset.UTC`.

**Never:**
- Alterar migrations Liquibase já aplicadas (`01` a `38`), alterar `DECIMAL(19,4)` em `trades`, ou renomear identificadores internos legados (`grahamselect`, `trades`).
- Usar linguagem imperativa de recomendação de compra/venda (CVM 19/20) ou ponto flutuante (`float`/`double`) em cálculos monetários no backend (usar `BigDecimal`).
- Implementar nesta entrega o seletor global Multi-CPF (`MultiCpfSelector`), consentimento LGPD Art. 14 de `family_profiles` (adiados para `deferred-work.md`) ou regras do Diagrama do Cerrado (Épico 3).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Abertura do `B3IngestionCrudSheet` | Toque em `"Atualizar Carteira / + Ativo"` no Dashboard ou CTA do estado vazio | Abre Bottom Sheet (Mobile) ou Drawer `480px` (Web, fecha com `Esc`) em nível único com 2 abas | N/A |
| Upload B3/SINACOR + Descarte PII | `.xlsx` (`POST /api/v1/upload/b3`) ou `.pdf` (`POST /api/v1/upload/brokerage-note`); `.csv` não suportado | Polling exibe progresso; consumidor processa trades e remove arquivo físico do disco no `finally` | Erro exibe banner `KernoThemeData.of(context).crimsonSurface` inline e remove arquivo no `finally` |
| CRUD Manual Multi-Classe | `Ações BR`, `FIIs`, `Renda Fixa / Tesouro`, `Internacional` ou `Criptoativos` (`BRL` contínuo via `quantity=1, price=valorBrl` ou fração 4 casas para RF/Cripto direto; cotas inteiras para bolsa/ETFs B3) | `GET`/`POST`/`PUT`/`DELETE /api/v1/trades/manual` persiste `assetClass` opcional, audita, publica `ValuationRequestedEvent` e atualiza Dashboard | Ownership inválido retorna `404`; duplicidade no `PUT` retorna `409 Conflict`; erro inline sem fechar o sheet |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/resources/db/changelog/39-add-asset-class-to-trades.yaml` e `db.changelog-master.yaml` -- Migration idempotente adicionando coluna opcional `asset_class VARCHAR(50)` em `trades`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3FileUploadedConsumer.java` (L79–110) -- Descartar arquivo bruto em `finally` com `Files.deleteIfExists(filePath)` sem relançar `IOException` após `failProcessing`.
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/` (`Trade.java`, `TradePort.java`, `CreateManualTradeUseCase.java`, `ListManualTradesUseCase.java`, `UpdateManualTradeUseCase.java`, `DeleteManualTradeUseCase.java`, `GetCustodyPositionsUseCase.java`, `ManualTradeAuditPort.java`, `ValuationEventPort.java`) e `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/` (`TradeManualController.java`, `TradeEntity.java`, `TradeJpaAdapter.java`, `JpaTradeRepository.java`) -- CRUD completo (`/api/v1/trades/manual` e `/{id}`), passar `profileId` em `tradePort.exists`, precedência `trade.assetClass > company.assetClass > inferAssetClass` e classificação de ETFs `11` (`IVVB11`/`HASH11`) + `Internacional`/`Criptoativos`.
- `frontend/lib/src/features/portfolio/` (`presentation/widgets/b3_ingestion_crud_sheet.dart`, `manual_operation_entry.dart`, `financial_data_table.dart`, `presentation/providers/portfolio_provider.dart`, `data/datasources/portfolio_remote_data_source.dart`, `data/repositories/portfolio_repository_impl.dart`, `domain/repositories/portfolio_repository.dart`, `data/models/trade_model.dart`), `frontend/lib/src/features/upload/presentation/providers/upload_provider.dart` e `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart` -- `B3IngestionCrudSheet` (Upload B3 + CRUD Manual 5 Macro Classes), substituir FAB por CTA `"Atualizar Carteira / + Ativo"` e atualizar `_EmptyStateWidget` para abrir o sheet.

## Tasks & Acceptance

**Execution:**
- [ ] `backend/common/src/main/resources/db/changelog/39-add-asset-class-to-trades.yaml` e `db.changelog-master.yaml` -- Criar migration idempotente com `preConditions` para `trades.asset_class` -- Suportar classificação manual sem quebrar dados existentes.
- [ ] `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3FileUploadedConsumer.java` -- Garantir `Files.deleteIfExists(filePath)` no `finally` após o parsing e falha terminal -- Cumprir descarte imediato de PII bruto.
- [ ] `backend/common/` e `backend/api/` (`TradeManualController.java`, `TradeEntity.java`, `TradeJpaAdapter.java`, `JpaTradeRepository.java`, use cases de `portfolio`) -- Implementar `GET`/`PUT /{id}`/`DELETE /{id}` em `/api/v1/trades/manual` (ownership `404`, `409` duplicidade, auditoria, `ValuationRequestedEvent`, `side` `COMPRA`/`VENDA`), passar `profileId` em `tradePort.exists` e atualizar `GetCustodyPositionsUseCase` (precedência + ETFs `11` + `Internacional`/`Criptoativos`) -- Completar suporte de API ao CRUD Multi-Classe.
- [ ] `backend/common/src/test/` e `backend/api/src/test/` -- Adicionar testes unitários, `TradeManualControllerIT` e verificação de descarte em `KafkaB3WorkerIT` -- Garantir cobertura completa e zero regressão.
- [ ] `frontend/lib/src/features/portfolio/`, `upload_provider.dart` e `dashboard_page.dart` -- Criar `B3IngestionCrudSheet` (Upload `.xlsx`/`.pdf` + CRUD Manual 5 Macro Classes), estender `PortfolioRemoteDataSource`/`PortfolioRepository`/`PortfolioProvider` com `GET`/`PUT`/`DELETE` e integrar CTA `"Atualizar Carteira / + Ativo"` + `_EmptyStateWidget` -- Entregar fluxo unificado no Design System.
- [ ] `frontend/test/` -- Criar/atualizar testes de widget e provider para `B3IngestionCrudSheet`, `DashboardPage`, `FinancialDataTable` e `PortfolioProvider`, executando `flutter analyze` e `flutter test` -- Validar todos os cenários da Matriz I/O.

**Acceptance Criteria:**
- Given usuário autenticado no `DashboardPage`, when aciona `"Atualizar Carteira / + Ativo"` ou o estado Zero-Blank-Screen, then `B3IngestionCrudSheet` abre em nível único permitindo upload B3 (`.xlsx`/`.pdf`) com descarte do arquivo bruto no backend e CRUD manual completo nas 5 Macro Classes canônicas em `BRL`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Design Notes

- **Classes canônicas e `ManualTradeRequest`:** Usar `"Ações BR"`, `"FIIs"`, `"Renda Fixa / Tesouro"`, `"Internacional"`, `"Criptoativos"`. `side` aceita `"COMPRA"`/`"VENDA"` (default `"COMPRA"` na UI de posição direta); `broker` opcional na UI (default `"Manual"`).
- **Renda Fixa & Criptoativos no CRUD Manual:** Para Renda Fixa / Tesouro e Cripto direto (`BTC`/`ETH`), permitir entrada contínua em `BRL` (`quantity = 1, price = valorBrl` ou fração até 4 casas decimais), enquanto Ações BR, FIIs e ETFs B3 (`IVVB11`, `HASH11`) validam cotas inteiras.

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: 0 erros/falhas
- `cd backend && mvn test -pl api -Dtest=PortfolioControllerIT,B3UploadControllerIT,TradeManualControllerIT,KafkaB3WorkerIT` -- expected: 100% passando
- `cd frontend && flutter analyze` -- expected: `No issues found!`
- `cd frontend && flutter test` -- expected: 100% dos testes passando
