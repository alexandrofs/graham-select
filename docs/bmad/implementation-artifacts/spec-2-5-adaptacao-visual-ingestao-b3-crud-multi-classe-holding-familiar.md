---
title: 'Story 2.5: Adaptação Visual da Ingestão B3 e CRUD Manual Multi-Classe'
type: 'feature'
created: '2026-11-27'
status: 'done'
baseline_commit: '6c31404719fc4efb8b6115f76487eb717240784a'
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
- [x] `backend/common/src/main/resources/db/changelog/39-add-asset-class-to-trades.yaml` e `db.changelog-master.yaml` -- Criar migration idempotente com `preConditions` para `trades.asset_class` -- Suportar classificação manual sem quebrar dados existentes.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3FileUploadedConsumer.java` -- Garantir `Files.deleteIfExists(filePath)` no `finally` após o parsing e falha terminal -- Cumprir descarte imediato de PII bruto.
- [x] `backend/common/` e `backend/api/` (`TradeManualController.java`, `TradeEntity.java`, `TradeJpaAdapter.java`, `JpaTradeRepository.java`, use cases de `portfolio`) -- Implementar `GET`/`PUT /{id}`/`DELETE /{id}` em `/api/v1/trades/manual` (ownership `404`, `409` duplicidade, auditoria, `ValuationRequestedEvent`, `side` `COMPRA`/`VENDA`), passar `profileId` em `tradePort.exists` e atualizar `GetCustodyPositionsUseCase` (precedência + ETFs `11` + `Internacional`/`Criptoativos`) -- Completar suporte de API ao CRUD Multi-Classe.
- [x] `backend/common/src/test/` e `backend/api/src/test/` -- Adicionar testes unitários, `TradeManualControllerIT` e verificação de descarte em `KafkaB3WorkerIT` -- Garantir cobertura completa e zero regressão.
- [x] `frontend/lib/src/features/portfolio/`, `upload_provider.dart` e `dashboard_page.dart` -- Criar `B3IngestionCrudSheet` (Upload `.xlsx`/`.pdf` + CRUD Manual 5 Macro Classes), estender `PortfolioRemoteDataSource`/`PortfolioRepository`/`PortfolioProvider` com `GET`/`PUT`/`DELETE` e integrar CTA `"Atualizar Carteira / + Ativo"` + `_EmptyStateWidget` -- Entregar fluxo unificado no Design System.
- [x] `frontend/test/` -- Criar/atualizar testes de widget e provider para `B3IngestionCrudSheet`, `DashboardPage`, `FinancialDataTable` e `PortfolioProvider`, executando `flutter analyze` e `flutter test` -- Validar todos os cenários da Matriz I/O.

**Acceptance Criteria:**
- Given usuário autenticado no `DashboardPage`, when aciona `"Atualizar Carteira / + Ativo"` ou o estado Zero-Blank-Screen, then `B3IngestionCrudSheet` abre em nível único permitindo upload B3 (`.xlsx`/`.pdf`) com descarte do arquivo bruto no backend e CRUD manual completo nas 5 Macro Classes canônicas em `BRL`.

## Implementation Notes

### File List
- `backend/common/src/main/resources/db/changelog/39-add-asset-class-to-trades.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3FileUploadedConsumer.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/domain/entities/Trade.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/repository/TradePort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/CreateManualTradeUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/ListManualTradesUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/UpdateManualTradeUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/DeleteManualTradeUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/ManualTradeUseCaseSupport.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/TradeManualController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/entities/TradeEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/TradeJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/repositories/JpaTradeRepository.java`
- `frontend/lib/src/features/portfolio/presentation/widgets/b3_ingestion_crud_sheet.dart`
- `frontend/lib/src/features/portfolio/presentation/widgets/manual_operation_entry.dart`
- `frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart`
- `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart`
- `frontend/lib/src/features/portfolio/data/datasources/portfolio_remote_data_source.dart`
- `frontend/lib/src/features/portfolio/data/repositories/portfolio_repository_impl.dart`
- `frontend/lib/src/features/portfolio/domain/repositories/portfolio_repository.dart`
- `frontend/lib/src/features/portfolio/data/models/trade_model.dart`
- `frontend/lib/src/features/upload/presentation/providers/upload_provider.dart`
- `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`

## Spec Change Log

## Review Triage Log

| Layer | Location | Verdict | Route | Evidence |
|-------|----------|---------|-------|----------|
| blind-hunter, edge-case-hunter | `b3_ingestion_crud_sheet.dart` (`_startEditing`, `_submitManualTrade`) | medium | patch | `_startEditing` não preserva `trade.profileId`, sobrescrevendo `profileId` com `null` no `PUT` (seletor multi-CPF segue fora de escopo). |
| blind-hunter | `b3_ingestion_crud_sheet.dart` (`_buildManualCrudTab`) | medium | patch | Quando `manualTradesError != null` e `manualTradesLoading == false`, a UI cai no fallback `'Nenhum lançamento encontrado.'` sem exibir erro. |
| edge-case-hunter | `b3_ingestion_crud_sheet.dart` (`deleteManualTrade`) | medium | patch | Excluir o lançamento atualmente em `_editingTradeId` mantém o formulário em modo edição, causando `PUT` `404` ao salvar. |
| blind-hunter, edge-case-hunter | `b3_ingestion_crud_sheet.dart` (`_startEditing` price) | medium | patch | `trade.price.toStringAsFixed(2)` arredonda preços cripto/fracionários (`< 0.005` vira `0,00`). |
| edge-case-hunter | `b3_ingestion_crud_sheet.dart` (`_selectedSide`) | medium | patch | `SegmentedButton` lança `AssertionError` se `trade.side` não for `COMPRA` ou `VENDA`. |
| edge-case-hunter | `b3_ingestion_crud_sheet.dart` (`_pickDate`) | low | patch | `showDatePicker` exige `firstDate <= initialDate <= lastDate`; clamping evita `AssertionError` em datas antigas/futuras. |
| blind-hunter, edge-case-hunter, verification-gap | `portfolio_provider.dart` (`createManualTrade`/`updateManualTrade`/`deleteManualTrade`) | medium | patch | Limpar `_manualTrades = []` antes da chamada ao repositório apaga a lista da UI em falhas como `409 Conflict`. |
| blind-hunter, edge-case-hunter | `TradeManualController.java` (`ManualTradeRequest`) | medium | patch | Faltam `@Size(max = 20)` em `ticker`, `@Size(max = 100)` em `broker` e `@Size(max = 50)` em `assetClass` para retornar `400` em vez de `500`. |
| blind-hunter | `GetCustodyPositionsUseCase.java` (`tradeAssetClasses`) | medium | patch | `tradeAssetClasses.put` depende da ordem arbitrária do repositório em vez de ordenar por `tradeDate`/`createdAt`. |
| blind-hunter | `GetCustodyPositionsUseCase.java` (`inferAssetClass` ETFs `BOVA11`/`SMAL11`) | medium | defer | ETFs de ações BR terminados em `11` (`BOVA11`, `SMAL11`) caírem em `FIIs` é comportamento pré-existente fora dos tickers `IVVB11`/`HASH11` desta entrega. |
| blind-hunter | `GetCustodyPositionsUseCase.java` vs `kCanonicalMacroClasses` (`Ações` vs `Ações BR`) | false | reject | `MacroClassDistributionCard` e `GetCustodyPositionsUseCaseTest` já esperam/normalizam `"Ações"` e `"Renda Fixa"` para compatibilidade brownfield. |
| blind-hunter | `b3_ingestion_crud_sheet.dart` (confirmação de exclusão) | low | reject | Spec não exige modal de confirmação na exclusão; adicionar diálogo aumentaria complexidade sem defeito funcional. |
| blind-hunter | `UpdateManualTradeUseCase.java`, `DeleteManualTradeUseCase.java` (`@SneakyThrows` / evento em `@Transactional`) | low | defer | Segue exatamente o padrão pré-existente de `CreateManualTradeUseCase.java`. |
| blind-hunter | `KafkaB3FileUploadedConsumer.java` (`catch`/`finally`) | false | reject | Não relançar `IOException` após `failProcessing` é restrição explícita da spec; `getAndSet(0)` no `catch` já zera contadores antes do `finally` e `filePath` é sempre não-nulo. |
| blind-hunter | `spec-2-5` / `sprint-status.yaml` (`in-review` / `File List`) | false | reject | `File List` e sincronização final para `done` são executados na etapa `step-05-present`. |
| verification-gap | `TradeManualControllerIT.java` (`PUT` `409` com/sem `profileId`) | medium | patch | Falta teste de integração para `PUT /api/v1/trades/manual/{id}` retornando `409 Conflict` com e sem `profileId`. |
| verification-gap | `KafkaB3WorkerIT.java` / `KafkaB3FileUploadedConsumer` (`IOException` -> `FAILED` + descarte) | medium | patch | Falta teste cobrindo falha terminal/`IOException` no consumidor garantindo status `FAILED` e `Files.notExists(filePath)`. |
| verification-gap | `portfolio_remote_data_source_test.dart` (`getManualTrades`/`create`/`update`/`delete`) | medium | patch | Faltam testes unitários dos novos métodos HTTP e `TradeModel.fromJson` (`assetClass`/`profileId`). |
| verification-gap | `portfolio_provider_test.dart` (`loadManualTrades`/`update`/`delete`/refresh) | medium | patch | Faltam testes unitários de `loadManualTrades`, `updateManualTrade`, `deleteManualTrade` e `_refreshAfterManualMutation`. |
| verification-gap | `b3_ingestion_crud_sheet_test.dart` (`Cotas inteiras`, upload-success, edição/exclusão) | medium | patch | Faltam testes de widget para validação de cotas inteiras, refresh pós-upload e fluxos de edição/exclusão. |

## Design Notes

- **Classes canônicas e `ManualTradeRequest`:** Usar `"Ações BR"`, `"FIIs"`, `"Renda Fixa / Tesouro"`, `"Internacional"`, `"Criptoativos"`. `side` aceita `"COMPRA"`/`"VENDA"` (default `"COMPRA"` na UI de posição direta); `broker` opcional na UI (default `"Manual"`).
- **Renda Fixa & Criptoativos no CRUD Manual:** Para Renda Fixa / Tesouro e Cripto direto (`BTC`/`ETH`), permitir entrada contínua em `BRL` (`quantity = 1, price = valorBrl` ou fração até 4 casas decimais), enquanto Ações BR, FIIs e ETFs B3 (`IVVB11`, `HASH11`) validam cotas inteiras.

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: 0 erros/falhas
- `cd backend && mvn test -pl api -Dtest=PortfolioControllerIT,B3UploadControllerIT,TradeManualControllerIT,KafkaB3WorkerIT` -- expected: 100% passando
- `cd frontend && flutter analyze` -- expected: `No issues found!`
- `cd frontend && flutter test` -- expected: 100% dos testes passando
