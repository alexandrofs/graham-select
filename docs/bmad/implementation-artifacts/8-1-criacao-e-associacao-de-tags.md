---
baseline_commit: c1c33ee7cc8e52017e38407bef6827debb770df9
---
# Story 8.1: Criação e Associação de Tags

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** investidor,
**I want** criar etiquetas como "Aposentadoria" ou "Longo Prazo" e associar aos meus ativos,
**so that** eu veja a rentabilidade separada por estratégia.

### Contexto e Fronteira de Escopo (Story 8.1 vs. Story 8.2)
- Esta história implementa a **fundação de domínio, persistência, API REST e UX de criação, edição, exclusão e associação múltipla de Tags** por usuário e ativo (FR14), além da **atualização imediata dos filtros do Dashboard** para incluir/refletir as tags criadas, renomeadas ou excluídas.
- **Fora do escopo desta história (pertence à Story 8.2 — FR15, NFR2, NFR3):** recálculo completo de KPIs consolidados (`Patrimônio Total`, `Rendimento`, `DY`), filtro de histórico de proventos/evolução (`EvolutionPage`), recálculo do Motor de Graham (`GrahamRecommendation`) e meta percentual por Tag, badge `"Visualizando: [Nome da Tag]"` completo no seletor de Lentes (`AnalyticalLensTabs`) e aviso `"X ativos sem tag não estão sendo exibidos"`. Nesta Story 8.1, a barra de filtros de Tags no Dashboard exibe `"Todas"` + chips das tags do usuário (com filtragem visual opcional da tabela de custódia `FinancialDataTable` por tag selecionada, mantendo os KPIs globais e recomendações intactos até a Story 8.2).
- **Acesso (Tier):** Conforme PRD (FR14/FR15) e Épicos, a funcionalidade de Tags é aberta a qualquer investidor autenticado — **não** aplicar `@RequirePremium` nos endpoints ou telas de Tags.

## Acceptance Criteria

1. **AC 1 — Criação e Associação Rápida de Tag na Tela de Ativos (`FinancialDataTable`)**
   - **Given** que o usuário autenticado está no Dashboard visualizando sua lista de ativos em custódia (`FinancialDataTable`)
   - **When** ele seleciona um ativo (ex.: `PETR4`) para gerenciar tags, cria uma nova Tag (ex.: `"Aposentadoria"`) e/ou seleciona Tags existentes para associar ao ativo
   - **Then** o sistema persiste a tag e a relação ativo-tag-usuário no banco de dados (isolado por `userId` extraído de `jwt.getSubject()`)
   - **And** exibe as tags vinculadas na linha do respectivo ativo na `FinancialDataTable`
   - **And** atualiza imediatamente os filtros de tags exibidos no Dashboard para incluir a nova tag criada sem exigir recarregamento manual da página
   - **And** fornece feedback visual imediato (SnackBar ou confirmação inline em `< 300ms` na UI após resposta da API).

2. **AC 2 — Associação Múltipla (M:N) de Tags por Ativo**
   - **Given** que o usuário possui múltiplas tags cadastradas (ex.: `"Aposentadoria"` e `"Longo Prazo"`)
   - **When** ele associa o mesmo ativo (ex.: `WEGE3`) a duas ou mais tags simultaneamente (ou desmarca uma delas preservando as demais)
   - **Then** o sistema persiste todas as associações válidas na relação `asset_tags` sem duplicar vínculos para o mesmo `(userId, tagId, ticker)`
   - **And** permite consultar todos os ativos vinculados a cada tag e todas as tags associadas a um ativo (inclusive via `GET /api/v1/tags` e `GET /api/v1/tags/assets/{ticker}`), mantendo o relacionamento M:N consistente após recarregar a página.

3. **AC 3 — Tela de Gerência de Tags (`/tags`): Listagem, Criação, Renomeação e Exclusão Segura**
   - **Given** que o usuário acessa a tela de gerência de Tags (`/tags` a partir do Perfil em `ProfilePage` ou pelo atalho `"Gerenciar Tags"` na barra de filtros do Dashboard)
   - **When** ele cria uma nova tag, renomeia uma tag existente (ex.: `"Aposentadoria"` para `"Previdência"`) ou exclui uma tag
   - **Then** ao **renomear**, o novo nome (e opcionalmente cor) reflete imediatamente em todos os ativos vinculados a essa tag e nos filtros do Dashboard
   - **And** ao **excluir**, a tag e suas associações em `asset_tags` são removidas (desassociando todos os ativos vinculados), **sem excluir ou alterar** nenhum ativo, posição de custódia ou registro na tabela `trades`
   - **And** os filtros do Dashboard e chips na `FinancialDataTable` são atualizados imediatamente após renomeação ou exclusão.

4. **AC 4 — Validações de Domínio, Normalização e Unicidade**
   - **Given** uma requisição de criação, renomeação ou associação de Tag
   - **When** o nome informado está vazio/em branco após `trim()` ou excede 50 caracteres (ou contém quebras de linha/controle), a cor hexadecimal opcional não segue o formato `#RRGGBB`, o ticker está vazio/inválido após `trim().toUpperCase(Locale.ROOT)` (exige de 1 a 20 caracteres compatíveis com tickers B3 e títulos de Renda Fixa no padrão `^[A-Z0-9][A-Z0-9 ._%+-]{0,19}$`, vedando `/`, `\`, `..` e caracteres de controle), a lista de `tagIds` é nula/inválida, ou o path `tagId`/JSON da requisição está malformado
   - **Then** o backend rejeita com `400 Bad Request` no padrão RFC 7807 (`ProblemDetail`)
   - **And** se o usuário tentar criar ou renomear uma tag para um nome já existente para o mesmo `userId` (comparação case-insensitive sobre `normalized_name = name.trim().toLowerCase(Locale.ROOT)`), o backend rejeita com `409 Conflict` (`DuplicateTagException` -> `ProblemDetail`)
   - **And** atualizar a cor ou capitalização de uma tag preservando sua própria identidade (`tagId`) é permitido sem falso conflito.

5. **AC 5 — Isolamento Multi-Tenant Estrito por `userId`**
   - **Given** dois usuários autenticados distintos (Usuário A e Usuário B, com `jwt.getSubject()` diferentes)
   - **When** ambos criam tags mesmo com nomes idênticos (ex.: `"Dividendos"`), ou o Usuário B tenta listar, renomear, excluir ou associar ativos usando o `tagId` pertencente ao Usuário A
   - **Then** cada usuário vê e filtra apenas suas próprias tags e associações de ativos
   - **And** operações de Usuário B sobre um `tagId` do Usuário A retornam `404 Not Found` (RFC 7807 `ProblemDetail`), sem vazar existência ou dados de tags de terceiros.

6. **AC 6 — Contrato de Filtro no Dashboard Preparatório (Story 8.1) & Compatibilidade**
   - **Given** o Dashboard (`DashboardPage`) com `TagProvider` montado no `MultiProvider`
   - **When** o usuário possui uma ou mais tags e visualiza o Dashboard
   - **Then** uma barra de filtros de tags exibe `"Todas"` e os chips das tags do usuário (atualizada automaticamente ao criar, renomear ou excluir tags em qualquer tela)
   - **And** ao clicar em um chip de tag na barra, a `FinancialDataTable` filtra em memória os ativos vinculados àquela tag (exibindo estado contextual `"Nenhum ativo associado à tag selecionada"` com `OutlinedButton` `"Limpar filtro"` se vazia, sem exibir erroneamente o `_EmptyStateWidget` de carteira vazia `/upload`)
   - **And** widgets existentes (`DashboardPage`, `FinancialDataTable`, `ProfilePage`) mantêm compatibilidade com testes unitários/widgets existentes (usando `Provider.of<TagProvider?>(context)` onde o provider é opcional em harnesses legados).

7. **AC 7 — Observabilidade, Padrões Temporais e Qualidade (`AGENTS.md` & `project-context.md`)**
   - **Given** qualquer operação nos endpoints `/api/v1/tags/**` e fluxos frontend
   - **Then** todos os timestamps de criação/atualização no backend são gerados explicitamente em UTC (`ZoneOffset.UTC`)
   - **And** todas as entradas de endpoint REST em `TagController` registram log em nível `INFO` com contexto (`userId`, `tagId`, `ticker` — nunca tokens/segredos) e exceções/erros capturados registram log em nível `ERROR` com stack trace
   - **And** a suíte completa de backend (`mvn clean test`) e frontend (`flutter analyze` e `flutter test`) executa e passa 100% após a implementação final.

## Tasks / Subtasks

- [x] **Task 1 — Migração Liquibase e Esquema MySQL (`portfolio_tags` e `asset_tags`)** (AC: 1, 2, 3, 4, 5)
  - [x] 1.1 Criar `backend/common/src/main/resources/db/changelog/30-create-portfolio-tags-and-asset-tags.yaml` definindo:
    - Tabela `portfolio_tags`: `id` `CHAR(36)` PK NOT NULL, `user_id` `VARCHAR(255)` NOT NULL, `name` `VARCHAR(50)` NOT NULL, `normalized_name` `VARCHAR(50)` NOT NULL, `color` `VARCHAR(7)` NULL, `created_at` `DATETIME` NOT NULL, `updated_at` `DATETIME` NULL; unique constraint `uq_portfolio_tags_user_normalized_name (user_id, normalized_name)`; index `idx_portfolio_tags_user_id (user_id)`.
    - Tabela `asset_tags`: `id` `CHAR(36)` PK NOT NULL, `user_id` `VARCHAR(255)` NOT NULL, `tag_id` `CHAR(36)` NOT NULL, `ticker` `VARCHAR(20)` NOT NULL, `created_at` `DATETIME` NOT NULL; FK `fk_asset_tags_tag_id` (`asset_tags.tag_id` -> `portfolio_tags.id`, `onDelete: CASCADE`); unique constraint `uq_asset_tags_user_tag_ticker (user_id, tag_id, ticker)`; index `idx_asset_tags_user_ticker (user_id, ticker)`; index `idx_asset_tags_tag_id (tag_id)`.
  - [x] 1.2 Incluir `db/changelog/30-create-portfolio-tags-and-asset-tags.yaml` no final de `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`, mantendo intactas as migrações `01`–`29`.

- [x] **Task 2 — Camada de Domínio e Casos de Uso no módulo `common` (POJOs Puros)** (AC: 1, 2, 3, 4, 5, 7)
  - [x] 2.1 Criar entidades de domínio puras (sem Spring/JPA) em `backend/common/src/main/java/afsdigital/grahamselect/common/tags/domain/entities/`:
    - `PortfolioTag` (`id`, `userId`, `name`, `normalizedName`, `color`, `createdAt`, `updatedAt`).
    - `AssetTag` (`id`, `userId`, `tagId`, `ticker`, `createdAt`).
  - [x] 2.2 Criar utilitário de validação/normalização puro `TagInputValidator` em `backend/common/src/main/java/afsdigital/grahamselect/common/tags/domain/` com métodos testáveis:
    - `validateAndNormalizeName(String rawName)` (trim, 1..50 chars, sem quebras de linha/controle).
    - `toNormalizedNameKey(String validName)` (`validName.trim().toLowerCase(Locale.ROOT)`).
    - `validateAndNormalizeColor(String rawColor)` (se nulo/em branco retorna `#6366F1`; valida regex `^#[0-9A-Fa-f]{6}$` em maiúsculo).
    - `validateAndNormalizeTicker(String rawTicker)` (trim, uppercase `Locale.ROOT`, rejeita nulo/em branco, `/`, `\`, `..` ou controle, e valida `^[A-Z0-9][A-Z0-9 ._%+-]{0,19}$` — suportando ações/FIIs/BDRs/ETFs e tickers de Renda Fixa com espaços ou hífens presentes em `trades.ticker`).
  - [x] 2.3 Criar exceções em `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/service/exceptions/`:
    - `TagNotFoundException`
    - `DuplicateTagException`
  - [x] 2.4 Criar DTOs de aplicação em `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/dto/`:
    - `TagDetailsDto(UUID id, String name, String color, List<String> tickers, int assetCount, LocalDateTime createdAt, LocalDateTime updatedAt)`
    - `AssetTagsAssignmentDto(String ticker, List<TagSummaryDto> tags)` com `TagSummaryDto(UUID id, String name, String color)`.
  - [x] 2.5 Criar interfaces de porta em `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/repository/`:
    - `PortfolioTagPort` (`create`, `update`, `findAllByUserId`, `findByIdAndUserId`, `findByUserIdAndNormalizedName`, `deleteByIdAndUserId`)
    - `AssetTagPort` (`save`, `saveAll`, `findAllByUserId`, `findAllByUserIdAndTagId`, `findAllByUserIdAndTicker`, `findByUserIdAndTagIdAndTicker`, `deleteByUserIdAndTagIdAndTicker`, `deleteAllByUserIdAndTagId`, `deleteAllByUserIdAndTicker`)
  - [x] 2.6 Criar Use Cases POJO (sem `@Service`) em `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/`:
    - `CreateTagUseCase` (valida `userId`, nome, cor, verifica unicidade por `normalizedName`, persiste via `portfolioTagPort.create(...)` com `UUID.randomUUID()` e `LocalDateTime.now(ZoneOffset.UTC)`).
    - `ListTagsUseCase` (lista todas as tags do usuário ordenadas alfabeticamente por nome case-insensitive, agregando `tickers` normalizados ordenados e `assetCount`).
    - `GetAssetTagsUseCase` (normaliza `ticker` e `userId`, consulta as tags associadas ao ativo e retorna `AssetTagsAssignmentDto` ordenado por nome da tag).
    - `RenameTagUseCase` (verifica posse `(tagId, userId)` -> `TagNotFoundException` se ausente; valida nome/cor; verifica conflito apenas contra outro `id != tagId` -> `DuplicateTagException`; atualiza via `portfolioTagPort.update(...)` com `updatedAt = LocalDateTime.now(ZoneOffset.UTC)` e retorna `TagDetailsDto` completo com `tickers` vinculados).
    - `DeleteTagUseCase` (verifica posse `(tagId, userId)` -> `TagNotFoundException` se ausente; remove associações por `(userId, tagId)` e exclui a tag por `(tagId, userId)` sem tocar `trades`).
    - `SetAssetTagsUseCase` (substitui de forma idempotente a lista de `tagIds` associada a um `ticker`: normaliza `ticker`, deduplica `tagIds`, valida antes de mutar que todas as tags pertencem ao `userId` -> `TagNotFoundException`, calcula diff removendo apenas vínculos obsoletos e inserindo apenas os novos em UTC para preservar `createdAt` dos mantidos, retornando `AssetTagsAssignmentDto`).
    - `AssignAssetToTagUseCase` e `UnassignAssetFromTagUseCase` (endpoints ergonômicos individuais `POST`/`DELETE /api/v1/tags/{tagId}/assets/{ticker}`, idempotentes após validar posse da tag e formato do ticker).
  - [x] 2.7 Escrever testes unitários puros em `backend/common/src/test/java/afsdigital/grahamselect/common/tags/` cobrindo normalização (incluindo tickers de Renda Fixa com espaços/hífens e rejeição de `/`, `\`, `..`), unicidade case-insensitive, consulta por ativo (`GetAssetTagsUseCase`), multi-tag, renomeação preservando própria identidade, exclusão segura e isolamento por `userId`.

- [x] **Task 3 — Infraestrutura JPA, Spring Configuration, Exception Handling e Controller REST (`api`)** (AC: 1, 2, 3, 4, 5, 7)
  - [x] 3.1 Criar entidades JPA em `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/entities/`:
    - `PortfolioTagEntity` e `AssetTagEntity` utilizando `@JdbcTypeCode(SqlTypes.VARCHAR)` com `length = 36` nos campos `UUID` (`id`, `tagId`) e implementando `Persistable<UUID>` com `@PostLoad` / `@PostPersist` marcando `isNew = false` (evitando `SELECT` prévio antes de `INSERT`, conforme débito registrado da Story 7.1):
      - `PortfolioTagEntity` controla `@Transient @Builder.Default private boolean isNew = false;` explicitamente no adapter (`create` instancia/marca `isNew = true` para `em.persist()` direto; `update` carrega a entidade gerenciada via `findByIdAndUserId` ou mantém `isNew = false` para `em.merge()` sem causar `EntityExistsException`).
      - `AssetTagEntity` (somente inserção/remoção) usa `@Transient @Builder.Default private boolean isNew = true;`.
  - [x] 3.2 Criar repositórios Spring Data JPA (`JpaPortfolioTagRepository`, `JpaAssetTagRepository`) e adapters (`PortfolioTagJpaAdapter`, `AssetTagJpaAdapter`) em `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/`:
    - Apenas `PortfolioTagJpaAdapter` traduz `DataIntegrityViolationException` (constraint de unicidade de nome por usuário) em `DuplicateTagException`, logando `log.error(..., ex)` antes de relançar.
    - `AssetTagJpaAdapter` utiliza métodos `@Transactional` e `@Modifying` (ou `delete` + `flush()`) para evitar conflitos de ordem `INSERT`/`DELETE` no Hibernate e nunca converte erros de integridade genéricos em `DuplicateTagException`.
  - [x] 3.3 Criar `TagsConfiguration` em `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/spring/` registrando `@Bean` para todos os Use Cases (`CreateTagUseCase`, `ListTagsUseCase`, `GetAssetTagsUseCase`, `RenameTagUseCase`, `DeleteTagUseCase`, `SetAssetTagsUseCase`, `AssignAssetToTagUseCase`, `UnassignAssetFromTagUseCase`).
  - [x] 3.4 Atualizar `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java` adicionando handlers RFC 7807 `ProblemDetail` + `log.error(..., ex)` para `DuplicateTagException` (`409 Conflict`), `TagNotFoundException` (`404 Not Found`), `MethodArgumentTypeMismatchException` (`400 Bad Request` — ex.: `UUID` inválido em `{tagId}`) e `HttpMessageNotReadableException` (`400 Bad Request` — JSON malformado).
  - [x] 3.5 Criar DTOs web com Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@NotNull`) — `CreateOrUpdateTagRequest`, `SetAssetTagsRequest` (`@NotNull @Size(max = 100) List<@NotNull UUID> tagIds`), `TagResponse`, `AssetTagsResponse` — e `TagController` (`@RequestMapping("/api/v1/tags")`) em `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/`:
    - `GET /api/v1/tags` -> `200 OK` `{ "data": List<TagResponse>, "meta": { "total": n } }`
    - `POST /api/v1/tags` -> `201 Created` `{ "data": TagResponse }`
    - `PUT /api/v1/tags/{tagId}` -> `200 OK` `{ "data": TagResponse }` (usar `PUT`, nunca `PATCH`, respeitando CORS de `SecurityConfig`)
    - `DELETE /api/v1/tags/{tagId}` -> `204 No Content` (`@Transactional` garantindo atomicidade com deleção de vínculos)
    - `GET /api/v1/tags/assets/{ticker}` -> `200 OK` `{ "data": AssetTagsResponse }`
    - `PUT /api/v1/tags/assets/{ticker}` -> `200 OK` `{ "data": AssetTagsResponse }` (`@Transactional` substituindo tags do ativo)
    - `POST /api/v1/tags/{tagId}/assets/{ticker}` -> `201 Created` `{ "data": { "tagId", "ticker" } }`
    - `DELETE /api/v1/tags/{tagId}/assets/{ticker}` -> `204 No Content`
    - Todos extraindo `userId = jwt.getSubject()`, registrando `log.info(...)` na entrada com contexto (`userId`, `tagId`, `ticker`, sem expor tokens) e delegando erros ao `GlobalExceptionHandler`.
  - [x] 3.6 Criar testes de integração `TagControllerIT` em `backend/api/src/test/java/afsdigital/grahamselect/api/tags/web/TagControllerIT.java` (estendendo `BaseRepositoryIT` com `@AutoConfigureMockMvc`, `@MockBean JwtDecoder`) cobrindo:
    - Autenticação e acesso: `401 Unauthorized` sem token e acesso completo (`200`/`201`) para usuário com `SubscriptionTier.FREE` (sem `@RequirePremium`).
    - Fluxo completo: criar tag (`201`), listar (`200`), duplicidade case-insensitive (`409`), validação de entrada inválida / `UUID` malformado / JSON inválido (`400`), renomear refletindo em ativos vinculados (`200`), associar múltiplas tags a um mesmo ticker (`PUT` e `POST`), consultar tags de um ativo via `GET /api/v1/tags/assets/{ticker}` (`200`), suportar ticker de Renda Fixa com espaços/hífens (ex.: `CDB BANCO X` / `LCI-2026`), desassociar 1 tag (`DELETE`), excluir tag garantindo que `trades` existentes do usuário permaneçam intocados (`204`), e isolamento estrito entre `user-a` e `user-b` (`404` cruzado + `200` com listas separadas).

- [x] **Task 4 — Camada Frontend de Tags (`features/tags` — Clean Architecture + Provider)** (AC: 1, 2, 3, 4, 6)
  - [x] 4.1 Criar modelos e entidades em `frontend/lib/src/features/tags/`:
    - `domain/entities/portfolio_tag.dart` (`id`, `name`, `color`, `tickers`, `assetCount`, `createdAt`, `updatedAt`).
    - `domain/repositories/tag_repository.dart` (interface abstrata: `fetchTags`, `fetchAssetTags`, `createTag`, `updateTag`, `deleteTag`, `setAssetTags`, `assignAssetToTag`, `unassignAssetFromTag`).
    - `data/models/portfolio_tag_model.dart` (`fromJson` resiliente com fallback de cor `#6366F1`, `tickers` vazio quando ausente, `assetCount` com fallback `tickers.length`, e `DateTime.tryParse`).
    - `data/datasources/tag_remote_datasource.dart` usando `ApiClient` (paths `/api/v1/tags...`), codificando obrigatoriamente o segmento `{ticker}` com `Uri.encodeComponent(ticker.trim().toUpperCase())` (compatível com tickers de Renda Fixa com espaços/hífens) e fazendo parse de `ProblemDetail` (`detail` / `message` em `DioException`) no padrão de `FamilyProfileRemoteDataSource`.
    - `data/repositories/tag_repository_impl.dart`.
  - [x] 4.2 Criar `TagProvider` (`ChangeNotifier`) em `frontend/lib/src/features/tags/presentation/providers/tag_provider.dart` gerenciando:
    - Estado de carregamento (`initial`, `loading`, `loaded`, `error`), `_isSubmitting`, `_errorMessage`.
    - Lista `tags` (`List<PortfolioTag>`), mapa derivado/getter `tagsForTicker(String ticker)` (normalizado em uppercase), `selectedFilterTagId` + `selectedFilterTag` + `selectFilterTag(String? tagId)` para o filtro no Dashboard, e `filteredTickersForSelectedTag`.
    - Helper puro e seguro para conversão hex `#RRGGBB` -> `Color` com fallback para `AppTheme.primaryColor`.
    - Ações reativas: `loadTags({bool silent = false})`, `createTag({required String name, String? color})`, `renameTag({required String tagId, required String name, String? color})`, `deleteTag(String tagId)` (limpando `selectedFilterTagId` se a tag excluída estava selecionada!), `setAssetTags({required String ticker, required List<String> tagIds})`, `createAndAssignTag({required String ticker, required String name, String? color})` (cria tag e inclui na associação atual do ativo em fluxo contínuo).
    - Garantir que todas as mutações atualizam a lista de tags em memória e chamam `notifyListeners()`, mantendo os filtros do Dashboard e chips na tabela sincronizados sem recarregar a página.

- [x] **Task 5 — Integração UI Flutter: Tabela de Ativos, Filtro do Dashboard, Tela de Gerência `/tags`, Perfil e Rotas** (AC: 1, 2, 3, 6)
  - [x] 5.1 Registrar `TagRemoteDataSource`, `TagRepositoryImpl`, `ChangeNotifierProvider<TagProvider>` e rota `GoRoute(path: '/tags', builder: (_, __) => const TagManagementPage())` dentro do `ShellRoute` em `frontend/lib/main.dart`.
  - [x] 5.2 Criar widget de diálogo/sheet `AssetTagSelectorSheet` (`StatefulWidget` com `dispose()` de `TextEditingController`s e bloqueio anti-duplo clique) em `frontend/lib/src/features/tags/presentation/widgets/asset_tag_selector_sheet.dart`:
    - Permite marcar/desmarcar múltiplas tags existentes (`CheckboxListTile` / `FilterChip` selecionáveis com cor e nome) para o `ticker` selecionado.
    - Inclui campo inline `"Nova tag (ex.: Aposentadoria)"` + botão compacto `"Criar"` (`OutlinedButton` ou `TextButton` com altura mínima acessível — **nunca** `ElevatedButton` dentro de `Row`, pois `AppTheme` define `minimumSize: Size(double.infinity, 56)`) para criar uma nova tag diretamente a partir da seleção do ativo, selecionando-a automaticamente para o ativo e atualizando os filtros do Dashboard.
    - CTA principal único em `FilledButton` (`Key('save_asset_tags_button')`), estado de loading/erro claro e Keys determinísticas (`asset_tag_selector_sheet`, `new_tag_inline_input`, `new_tag_inline_submit`, `save_asset_tags_button`).
  - [x] 5.3 Atualizar **exclusivamente** `frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart` (não editar `frontend/lib/src/core/widgets/financial_data_table.dart`, que é outro widget genérico):
    - Obter `TagProvider` opcionalmente via `Provider.of<TagProvider?>(context)` para preservar compatibilidade com `financial_data_table_test.dart`.
    - Quando `TagProvider` estiver presente e `tagProvider.selectedFilterTagId != null`, filtrar a lista `positions` exibida (já filtrada por classe quando aplicável pelo gráfico de rosca), casando `pos.ticker.trim().toUpperCase()` com os tickers vinculados à tag selecionada.
    - Se a custódia carregou (`provider.custodyPositions.isNotEmpty`), um filtro de tag estiver ativo e o resultado filtrado estiver vazio, exibir um card/estado vazio específico `"Nenhum ativo associado à tag selecionada"` com `OutlinedButton` `"Limpar filtro"` (`Key('clear_tag_filter_button')`), sem mostrar o `_EmptyStateWidget` de carteira vazia `/upload`.
    - Adicionar coluna `'Tags'` (não-ordenável, `index 6`) exibindo chips compactos das tags vinculadas a `pos.ticker` (ex.: até 2-3 chips + indicador `+N` se houver muitas) e um `IconButton` acionável (`Key('manage_asset_tags_${pos.ticker}')` com `Semantics` acessível e área mínima `48x48dp`) para abrir `AssetTagSelectorSheet`. Ajustar `dataRowMaxHeight: double.infinity` com padding vertical controlado para evitar `RenderFlex overflow` quando um ativo possuir múltiplas tags.
  - [x] 5.4 Atualizar `DashboardPage` (`frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`):
    - No `initState` (`addPostFrameCallback`), carregar `final tagProvider = Provider.of<TagProvider?>(context, listen: false); tagProvider?.loadTags(silent: tagProvider.tags.isNotEmpty);` (opcional, não quebra `dashboard_page_test.dart` e evita flicker ao retornar ao Dashboard).
    - Renderizar uma barra compacta de filtros de Tags (`DashboardTagFilterBar`, `Key('dashboard_tag_filter_bar')`) imediatamente acima da seção de `FinancialDataTable` quando `tagProvider != null` e o estado de tags estiver disponível, contendo: chip `"Todas"` (`Key('tag_filter_all')`), chips das tags do usuário (`Key('tag_filter_${tag.id}')` com cor e contagem opcional) que alternam `tagProvider.selectFilterTag(tag.id)`, e atalho discreto `TextButton` `"Gerenciar Tags"` (`Key('dashboard_manage_tags_button')`) navegando via `context.push('/tags')`.
  - [x] 5.5 Criar `TagManagementPage` em `frontend/lib/src/features/tags/presentation/pages/tag_management_page.dart`:
    - Lista todas as tags do usuário (estado loading/vazio/erro/lista com nome, indicador de cor e contagem `"X ativo(s) vinculado(s)"`).
    - Permite criar nova tag (no máximo 1 `FilledButton` como CTA principal na tela, com diálogo `StatefulWidget` gerenciando `TextEditingController` e `dispose()` + seletor de cores da paleta do tema), renomear tag via diálogo (`PUT`), e excluir tag com `OutlinedButton` em cor de erro abrindo confirmação explícita (`AlertDialog`) informando que os ativos vinculados apenas serão desassociados sem serem excluídos (`DELETE`).
    - Feedback imediato com `SnackBar` em português e acessibilidade (`Semantics`, tooltips, foco e botões com área de toque `>= 48x48dp`).
  - [x] 5.6 Atualizar `ProfilePage` (`frontend/lib/src/features/profile/presentation/pages/profile_page.dart`) adicionando um card de navegação para `/tags` (seguindo o mesmo padrão visual de `profile_family_holding_button`, usando `Key('profile_tags_management_button')`, título `'Tags da Carteira'`, subtítulo explicativo e navegação `context.push('/tags')`).

- [x] **Task 6 — Testes Frontend e Validação Ponta a Ponta** (AC: 1, 2, 3, 4, 6, 7)
  - [x] 6.1 Escrever testes unitários de `TagProvider` e `PortfolioTagModel` em `frontend/test/features/tags/` (criação, duplicidade/erro, renomeação refletida em `tagsForTicker`, exclusão limpando filtro ativo, associação múltipla a um ticker, serialização JSON).
  - [x] 6.2 Escrever testes de widget para `TagManagementPage` e `AssetTagSelectorSheet`, além de testes cobrindo a integração em `FinancialDataTable` e `DashboardPage` (criar/associar tag pelo ativo atualizando a barra de filtros, multi-tag exibido na linha, filtrar por tag com estado vazio contextual, atalho no `ProfilePage`).
  - [x] 6.3 Validar que todos os testes legados (`dashboard_page_test.dart`, `financial_data_table_test.dart`, `profile_page_test.dart`) continuam passando sem regressão.
  - [x] 6.4 Executar `mvn clean test` (módulos `common` e `api`), `flutter analyze` e `flutter test` após a última edição, confirmando 0 erros/falhas antes de concluir.

## Dev Notes

### Arquitetura Backend, Clean Architecture e Padrões a Seguir
- **Separação rigorosa `common` vs `api`:** Conforme `docs/bmad/planning-artifacts/architecture.md` e o precedente direto de Holding Familiar (Story 7.1 em `backend/common/src/main/java/afsdigital/grahamselect/common/family/`), todas as entidades de domínio (`PortfolioTag`, `AssetTag`), portas (`PortfolioTagPort`, `AssetTagPort`), validadores, exceções de negócio e Use Cases POJO devem ficar em `backend/common/src/main/java/afsdigital/grahamselect/common/tags/` **sem nenhuma importação de `org.springframework.*` ou `jakarta.persistence.*`**. O wiring via `@Bean` acontece em `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/spring/TagsConfiguration.java` (mesmo padrão de `FamilyProfileConfiguration.java`).
- **Identidade de Usuário (`userId`):** `OAuth2UserProvisioningConverter` define `authentication.getName()` com o `Long id` interno do usuário, mas **todos** os controllers de carteira/operações/holding (`PortfolioController`, `TradeManualController`, `B3UploadController`, `FamilyProfileController`, `FinancialGoalsController`) usam `jwt.getSubject()` (`String`, Google `sub`) como chave de isolamento em `trades.user_id` e demais tabelas de domínio. `TagController` **deve obrigatoriamente usar `jwt.getSubject()`** (`String`) em `portfolio_tags.user_id` e `asset_tags.user_id`.
- **Evitar `SELECT` prévio em `INSERT` (`Persistable<UUID>`):** Em `docs/bmad/implementation-artifacts/deferred-work.md` (item da Story 7.1), registrou-se que entidades JPA com `@Id UUID` preenchido manualmente na aplicação fazem o Spring Data JPA executar um `SELECT` antes do `INSERT` se não implementarem `Persistable<UUID>`. Portanto, `PortfolioTagEntity` e `AssetTagEntity` devem implementar `Persistable<UUID>` controlando um campo `@Transient private boolean isNew;` (com `@PostLoad` e `@PostPersist` definindo `isNew = false`, e `isNew()` retornando `this.isNew`). **Atenção:** `PortfolioTagEntity` deve inicializar `isNew = false` e marcar `isNew = true` exclusivamente no caminho `create(...)` do adapter (para `em.persist()` sem `SELECT`), mantendo `isNew = false` em atualizações (`RenameTagUseCase` -> `update(...)` na entidade gerenciada / `em.merge()`, evitando `EntityExistsException`). Já `AssetTagEntity` (somente inserção/remoção) pode inicializar `@Builder.Default private boolean isNew = true;`.
- **UUIDs em MySQL (`CHAR(36)`):** Seguir exatamente `FamilyProfileEntity.java`:
  ```java
  @Id
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(length = 36, nullable = false)
  private UUID id;
  ```
  E no Liquibase usar `CHAR(36)` para `id` e `tag_id`.
- **Liquibase Imutável:** A última migração registrada em `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` é `29-create-family-profiles-and-link-trades.yaml`. A nova migração deve ser a **30** (`30-create-portfolio-tags-and-asset-tags.yaml`), utilizando `addUniqueConstraint`, `createIndex` (`uq_`, `idx_`) e `addForeignKeyConstraint` explícito (`fk_asset_tags_tag_id` com `onDelete: CASCADE`, `onUpdate: RESTRICT`, compatível com H2 `MODE=MySQL` e MySQL 8.0.33). **Nunca** editar arquivos `01`–`29`, pois `LiquibaseChecksumValidationTest` valida os checksums históricos durante `mvn clean test`.
- **CORS e Verbos HTTP (`SecurityConfig.java`):** `SecurityConfig.corsConfigurationSource` permite apenas `["GET", "POST", "PUT", "DELETE", "OPTIONS"]` — **`PATCH` NÃO está habilitado no CORS**. Portanto, **usar `PUT`** para renomeação (`PUT /api/v1/tags/{tagId}`) e substituição das tags do ativo (`PUT /api/v1/tags/assets/{ticker}`), evitando alterações desnecessárias em `SecurityConfig.java`.
- **Normalização de Ticker vs Renda Fixa e URL Encoding:** Em `GetCustodyPositionsUseCase.java`, `CustodyPositionDTO.ticker` retorna o ticker exato consolidado das operações (`trades.ticker`, em caixa alta — podendo conter espaços ou hífens em títulos de Renda Fixa como `"TESOURO SELIC 2029"` ou `"LCI-2026"`). Padronize `ticker` nas rotas/payloads de tags com `trim().toUpperCase(Locale.ROOT)` e padrão `^[A-Z0-9][A-Z0-9 ._%+-]{0,19}$` (vedando `/`, `\`, `..` e controle) para casar 1:1 com `pos.ticker` da `FinancialDataTable` em todas as classes de ativos (ações, FIIs, BDRs, ETFs, fracionários e Renda Fixa). No frontend, `TagRemoteDataSource` deve codificar `{ticker}` com `Uri.encodeComponent(...)` (seguindo `AdminRemoteDatasource`).
- **Exclusão Atômica e Substituição de Vínculos sem Conflito JPA:**
  - Ao excluir uma tag (`DELETE /api/v1/tags/{tagId}`), chamar `assetTagPort.deleteAllByUserIdAndTagId(userId, tagId)` (além do `ON DELETE CASCADE` da FK `fk_asset_tags_tag_id`) dentro de fronteira `@Transactional` na camada web/adapter antes de remover a `portfolio_tags`. Nunca tocar em `trades` ou `companies`.
  - No `SetAssetTagsUseCase` (`PUT /api/v1/tags/assets/{ticker}`), evitar o antipadrão "delete all + insert all sem flush" que faz o Hibernate executar `INSERT` antes de `DELETE` e violar `uq_asset_tags_user_tag_ticker`. Preferir cálculo de **diff explícito** (buscar vínculos atuais de `(userId, ticker)`, remover apenas os `tagIds` não mais presentes via `deleteByUserIdAndTagIdAndTicker` ou `flush()`, e inserir apenas os novos `tagIds`), preservando inclusive o `createdAt` original dos vínculos mantidos.
  - Apenas `PortfolioTagJpaAdapter` deve traduzir `DataIntegrityViolationException` da constraint de unicidade de nome em `DuplicateTagException`; `AssetTagJpaAdapter` não deve mascarar outros erros de integridade como duplicidade de tag.
- **Conta & LGPD Purge (`EmptyUserDataPurgeAdapter`):** `EmptyUserDataPurgeAdapter` atualmente é um placeholder na codebase; portanto não requer alteração nesta história.
- **Timestamps em UTC:** Toda geração de `createdAt` / `updatedAt` nos Use Cases deve usar `LocalDateTime.now(ZoneOffset.UTC)` (nunca `LocalDateTime.now()` sem timezone).

### Contrato REST e Payloads Sugeridos (`/api/v1/tags`)
- Envelope alinhado ao `architecture.md` e `FinancialGoalsController`/`PortfolioController`:
  - `GET /api/v1/tags` -> `200 OK`:
    ```json
    {
      "data": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440000",
          "name": "Aposentadoria",
          "color": "#6366F1",
          "tickers": ["PETR4", "VALE3"],
          "assetCount": 2,
          "createdAt": "2026-11-13T12:00:00",
          "updatedAt": null
        }
      ],
      "meta": { "total": 1 }
    }
    ```
  - `POST /api/v1/tags` (`{ "name": "Aposentadoria", "color": "#10B981" }`) -> `201 Created`: `{ "data": { ... } }`
  - `PUT /api/v1/tags/{tagId}` (`{ "name": "Previdência", "color": "#8B5CF6" }`) -> `200 OK`: `{ "data": { ... } }` (`color` nulo mantém a cor existente).
  - `DELETE /api/v1/tags/{tagId}` -> `204 No Content`.
  - `GET /api/v1/tags/assets/{ticker}` -> `200 OK`: `{ "data": { "ticker": "PETR4", "tags": [ ... ] } }`.
  - `PUT /api/v1/tags/assets/{ticker}` (`{ "tagIds": ["uuid-1", "uuid-2"] }`) -> `200 OK`:
    ```json
    {
      "data": {
        "ticker": "PETR4",
        "tags": [
          { "id": "uuid-1", "name": "Aposentadoria", "color": "#6366F1" },
          { "id": "uuid-2", "name": "Longo Prazo", "color": "#10B981" }
        ]
      }
    }
    ```
  - `POST /api/v1/tags/{tagId}/assets/{ticker}` -> `201 Created`: `{ "data": { "tagId": "...", "ticker": "PETR4" } }`.
  - `DELETE /api/v1/tags/{tagId}/assets/{ticker}` -> `204 No Content`.
- **Erros RFC 7807 (`GlobalExceptionHandler.java`):**
  - `TagNotFoundException` -> `404 Not Found` (`title: "Tag Not Found"`)
  - `DuplicateTagException` -> `409 Conflict` (`title: "Duplicate Tag"`)
  - `MethodArgumentTypeMismatchException` -> `400 Bad Request` (`title: "Invalid Parameter"`, ex.: `UUID` malformado em `{tagId}`)
  - `HttpMessageNotReadableException` -> `400 Bad Request` (`title: "Malformed JSON Request"`)
  - `IllegalArgumentException` / `MethodArgumentNotValidException` -> já tratados com `400 Bad Request` (`ProblemDetail`).

### Arquitetura Frontend, UX e Compatibilidade de Testes
- **HTTP Client (`ApiClient`):** Em `frontend/lib/main.dart`, `effectiveApiClient` (`ApiClient`) recebe `baseUrl` **sem** `/api/v1` (linha 174), portanto `TagRemoteDataSource` (usando Dio via `ApiClient`, igual a `FamilyProfileRemoteDataSource` e `GoalsRemoteDatasource`) deve chamar paths iniciando com `/api/v1/tags` e codificar `{ticker}` com `Uri.encodeComponent(ticker.trim().toUpperCase())` (seguindo `AdminRemoteDatasource`).
- **Disambiguação de `FinancialDataTable`:** Existem dois widgets homônimos no projeto — alterar **exclusivamente** `frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart` (usado na custódia do Dashboard). Não modificar `frontend/lib/src/core/widgets/financial_data_table.dart`.
- **Providers Opcionais em Widgets Existentes:**
  - `dashboard_page_test.dart` monta `DashboardPage` apenas com `PortfolioProvider` e `ProfileProvider` (e `DashboardPage` já usa `Provider.of<GoalsProvider?>(context)` nas linhas 30 e 49 exatamente por isso!).
  - `financial_data_table_test.dart` monta `FinancialDataTable` apenas com `PortfolioProvider`.
  - **Obrigatório:** Em `DashboardPage` e `FinancialDataTable`, acessar `TagProvider` via `Provider.of<TagProvider?>(context)` (e `Provider.of<TagProvider?>(context, listen: false)` no `initState`), degradando graciosamente quando `tagProvider == null` para não quebrar nenhum teste legado.
- **Armadilha de Layout em `AppTheme.elevatedButtonTheme` e Ciclo de Vida de Controllers:**
  - `frontend/lib/src/core/theme/app_theme.dart` define `ElevatedButtonThemeData` com `minimumSize: const Size(double.infinity, 56)`. Por isso, **nunca** inserir `ElevatedButton` sem restrição de largura dentro de `Row`s ou barras horizontais (`AssetTagSelectorSheet` inline, `DashboardTagFilterBar` `"Gerenciar Tags"`, ações em linha ou `"Limpar filtro"`), pois causa `BoxConstraints forces an infinite width`. Utilizar `FilledButton` (no máximo 1 CTA primário por tela/diálogo conforme `ux-design-specification.md`), `OutlinedButton` (ações secundárias e botão de exclusão em cor de erro com diálogo de confirmação) ou `TextButton`.
  - Em diálogos/sheets com entrada de texto (`AssetTagSelectorSheet`, diálogos de criação/renomeação), isolar `TextEditingController` em `StatefulWidget` com `dispose()` próprio e guarda booleana contra duplo envio (`_isSubmitting`).
- **Overflow, Ordenação e Composição de Filtros em `FinancialDataTable`:**
  - `FinancialDataTable` usa `dataRowMinHeight: 48` e `dataRowMaxHeight: 52`, e `_getColumnIndex` retorna `0..5` para as 6 colunas atuais. Ao adicionar `'Tags'` como `7ª` coluna (`index 6`, sem `onSort`), o `sortColumnIndex` permanece seguro em `0..5`. Ajustar `dataRowMaxHeight: double.infinity` com padding vertical controlado (chips compactos em linha + badge `+N` + `IconButton`) para prevenir qualquer `RenderFlex overflowed` quando um ativo possuir múltiplas tags.
  - A filtragem por tag na `FinancialDataTable` compõe naturalmente sobre `provider.custodyPositions` (que já aplica `PortfolioProvider.selectedAssetClassFilter` vindo do gráfico de rosca), sem quebrar a interação existente do `AssetAllocationDonutChart`.
- **Estado Vazio com Filtro de Tag Ativo vs Carteira Vazia:**
  - Hoje `FinancialDataTable` verifica `if (provider.custodyPositions.isEmpty) return const _EmptyStateWidget();` (que orienta ir para `/upload`). Se o usuário tiver ativos na custódia (`provider.custodyPositions.isNotEmpty`), selecionar um chip de tag na barra de filtros do Dashboard e o resultado filtrado (por tag isolada ou combinada com classe) estiver vazio, **não** exibir `_EmptyStateWidget`. Mostrar um aviso claro `"Nenhum ativo associado à tag selecionada"` com `OutlinedButton` `"Limpar filtro"` (`Key('clear_tag_filter_button')`).
- **Padrão de Teste de Integração (`TagControllerIT`):**
  - Seguir exatamente `FamilyProfileControllerIT` (`extends BaseRepositoryIT`, `@AutoConfigureMockMvc`, `@MockBean JwtDecoder` com `header("Authorization", "Bearer ...")`), persistindo `User` com `SubscriptionTier.FREE` no setup para validar explicitamente que investidores `FREE` possuem acesso total às tags (sem `@RequirePremium`).
- **Tokens Visuais e Estilo Notion (`AppTheme` & `ux-design-specification.md`):**
  - Fundo `#0F172A`, cards `#1E293B` (`borderRadius: 16`, borda `Colors.white.withValues(alpha: 0.1)`), primária `#6366F1`, secundária `#8B5CF6`, acento/sucesso `#10B981`, headers em `DM Sans`, texto em `Inter`.
  - Paleta sugerida de cores pré-definidas no seletor da UI (todas com bom contraste no tema escuro): `#6366F1` (Indigo), `#10B981` (Emerald), `#8B5CF6` (Violet), `#F59E0B` (Amber), `#EC4899` (Pink), `#06B6D4` (Cyan).
  - Para manter coerência com o padrão visual de `ProfilePage` (onde fica o card `profile_family_holding_button` para `/family-profiles`), adicionar logo abaixo dele o card `profile_tags_management_button` apontando para `/tags`, além do atalho direto `"Gerenciar Tags"` no Dashboard. Não alterar os itens principais da `NavigationRail` / `_BottomNavBar` em `MainLayout`, mantendo a navegação principal enxuta e consistente com `FamilyProfilesPage` (`/family-profiles`).
  - **Visão C4 (`docs/workspace.dsl`):** Tags pertencem aos containers existentes (`API (Spring Boot)`, `Frontend (Flutter)` e `Database`), não havendo novos containers externos a registrar no modelo DSL.

### Lições Aprendidas (Retrospectiva Epic 6, Story 7.1 e Commits Recentes)
- **Commits recentes verificados (`git log -n 5`):**
  - `c1c33ee` — `feat(family-holding): implement story 7-1-vinculo-de-novo-cpf-a-conta-matriz (Closes #106)` (precedente mais próximo: Clean Architecture `common/family` + `api/family` + `CHAR(36)` Liquibase + `FamilyProfileControllerIT` + `frontend/lib/src/features/family_holding/` + card no `ProfilePage`).
  - `3f9e686` — `fix: resolve liquibase checksum mismatch and missing kafka consumer group-id`.
  - `ddb459e` — `feat(admin): implement master data ticker management and cache eviction (Story 6.3)`.
- **Cuidados herdados de `epic-6-retro-2026-11-13.md`, `deferred-work.md` e Story 7.1:**
  1. Nunca editar changelogs Liquibase já mergeados (`LiquibaseChecksumValidationTest`).
  2. Não introduzir propriedades `@Value` / `@ConfigurationProperties` obrigatórias que existam apenas em `@DynamicPropertySource` de testes — garantir defaults seguros no `application.yml` ou não exigir nova config externa para Tags.
  3. Toda nova funcionalidade backend deve ser verificada ponta a ponta na UI Flutter (não deixar funcionalidade apenas em API sem pontos de entrada na interface).
  4. Adotar `Persistable<UUID>` nas entidades JPA novas com UUID gerado em Java para evitar query `SELECT` extra antes do `INSERT`, diferenciando criação (`isNew = true`) de atualização (`isNew = false`) quando a entidade for mutável.
  5. Garantir `JAVA_HOME` com JDK >= 21 ao compilar os módulos `common` e `api` devido à flag `-proc:full`.

### Requisitos Transversais Obrigatórios (`AGENTS.md`)
- **Branch de trabalho:** utilizar branch atualizada a partir da `main` (`story/8-1-criacao-e-associacao-de-tags`).
- **Logs:** `INFO` em todas as entradas de endpoint REST em `TagController`; `ERROR` com stack trace nas capturas de exceção (`GlobalExceptionHandler`, adapters JPA e eventuais catch blocks).
- **Validação final:** rodar `mvn clean test` (backend `common` e `api`), `flutter analyze` e `flutter test` (frontend) após a última edição.

### Project Structure Notes

**Arquivos a CRIAR (`CREATE`):**
- `backend/common/src/main/resources/db/changelog/30-create-portfolio-tags-and-asset-tags.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/domain/TagInputValidator.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/domain/entities/PortfolioTag.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/domain/entities/AssetTag.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/dto/TagDetailsDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/dto/TagSummaryDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/dto/AssetTagsAssignmentDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/repository/PortfolioTagPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/repository/AssetTagPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/service/exceptions/TagNotFoundException.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/service/exceptions/DuplicateTagException.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/CreateTagUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/ListTagsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/GetAssetTagsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/RenameTagUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/DeleteTagUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/SetAssetTagsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/AssignAssetToTagUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/UnassignAssetFromTagUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/tags/...` (testes unitários de validador e use cases)
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/entities/PortfolioTagEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/entities/AssetTagEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/repositories/JpaPortfolioTagRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/repositories/JpaAssetTagRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/PortfolioTagJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/AssetTagJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/spring/TagsConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/CreateOrUpdateTagRequest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/SetAssetTagsRequest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/TagResponse.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/AssetTagsResponse.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/TagController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/tags/web/TagControllerIT.java`
- `frontend/lib/src/features/tags/domain/entities/portfolio_tag.dart`
- `frontend/lib/src/features/tags/domain/repositories/tag_repository.dart`
- `frontend/lib/src/features/tags/data/models/portfolio_tag_model.dart`
- `frontend/lib/src/features/tags/data/datasources/tag_remote_datasource.dart`
- `frontend/lib/src/features/tags/data/repositories/tag_repository_impl.dart`
- `frontend/lib/src/features/tags/presentation/providers/tag_provider.dart`
- `frontend/lib/src/features/tags/presentation/pages/tag_management_page.dart`
- `frontend/lib/src/features/tags/presentation/widgets/asset_tag_selector_sheet.dart`
- `frontend/lib/src/features/tags/presentation/widgets/dashboard_tag_filter_bar.dart`
- `frontend/test/features/tags/...` (testes unitários de model/provider e widget tests)

**Arquivos existentes a MODIFICAR (`UPDATE`):**
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` — incluir `30-create-portfolio-tags-and-asset-tags.yaml`.
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java` — mapear `TagNotFoundException` (404), `DuplicateTagException` (409), `MethodArgumentTypeMismatchException` (400) e `HttpMessageNotReadableException` (400) como `ProblemDetail`.
- `frontend/lib/main.dart` — registrar DI de `TagRemoteDataSource`/`TagRepositoryImpl`, `ChangeNotifierProvider<TagProvider>` e rota `/tags` no `ShellRoute`.
- `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart` — disparar `loadTags()` de forma opcional (`Provider.of<TagProvider?>(context, listen: false)`) e exibir `DashboardTagFilterBar`.
- `frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart` — exibir coluna `'Tags'`, chips de tags do ativo, ação de abrir `AssetTagSelectorSheet` e filtragem visual opcional quando uma tag estiver selecionada na barra de filtros (sem quebrar `financial_data_table_test.dart`).
- `frontend/lib/src/features/profile/presentation/pages/profile_page.dart` — adicionar card de acesso `'Tags da Carteira'` navegando para `/tags` (sem quebrar `profile_page_test.dart`).

### References

- Requisitos Epic 8, Story 8.1 e fronteira com Story 8.2: [Source: docs/bmad/planning-artifacts/epics.md#L756-L811]
- Requisito funcional FR14 / FR15 e Jornada 3: [Source: docs/bmad/planning-artifacts/prd.md]
- Padrões Arquiteturais (Clean Architecture, REST `{ data, meta }`, RFC 7807 `ProblemDetail`, MySQL `snake_case`): [Source: docs/bmad/planning-artifacts/architecture.md]
- Padrões UX Notion-style e Lentes Analíticas: [Source: docs/bmad/planning-artifacts/ux-design-specification.md#L197-L241] e [Source: docs/bmad/planning-artifacts/ux-design-specification.md#L635-L640]
- Contexto Técnico e Stack: [Source: docs/bmad/project-context.md]
- Lições Aprendidas e Liquibase/UI: [Source: docs/bmad/implementation-artifacts/epic-6-retro-2026-11-13.md]
- Dívida técnica `Persistable<UUID>`: [Source: docs/bmad/implementation-artifacts/deferred-work.md#L33-L34]
- Precedente de implementação Clean Architecture (`FamilyProfile`): [Source: docs/bmad/implementation-artifacts/7-1-vinculo-de-novo-cpf-a-conta-matriz.md] e [Source: backend/common/src/main/java/afsdigital/grahamselect/common/family/application/usecase/LinkFamilyProfileUseCase.java]
- Migração Liquibase mais recente (`29`): [Source: backend/common/src/main/resources/db/changelog/db.changelog-master.yaml#L55-L58]
- Segurança/CORS (sem `PATCH`): [Source: backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfig.java]
- Handlers globais `ProblemDetail`: [Source: backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java#L60-L147]
- Dashboard, Tabela de Custódia, Perfil e DI Flutter: [Source: frontend/lib/main.dart#L202-L331], [Source: frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart#L24-L150], [Source: frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart#L13-L230], [Source: frontend/lib/src/features/profile/presentation/pages/profile_page.dart#L215-L240]

## Dev Agent Record

### Agent Model Used

Antigravity AI Agent

### Debug Log References

- `JAVA_HOME=/Users/alexandrofs/Library/Java/JavaVirtualMachines/openjdk-22.0.1/Contents/Home mvn -f backend/pom.xml -pl common,api clean test`: `common` (123 testes: `TagUseCasesTest` 13, `TagInputValidatorTest` 7, 0 falhas, 0 erros) e `api-service` (78 testes unitários/WebMvc/Liquibase, 0 falhas, 0 erros), `BUILD SUCCESS`.
- `JAVA_HOME=/Users/alexandrofs/Library/Java/JavaVirtualMachines/openjdk-22.0.1/Contents/Home mvn -f backend/pom.xml -pl common,api test -Dtest=TagControllerIT -Dsurefire.failIfNoSpecifiedTests=false`: 6 testes de integração com MySQL/Testcontainers (`401`, tier `FREE`, duplicidade `409`, validação `400`, Renda Fixa `CDB BANCO X`/`LCI-2026`, padding + controle `\n`, idempotência, `tagIds: []` e `[null]`, preservação de `trades` após delete, isolamento multi-tenant `user-a`/`user-b`), 0 falhas, 0 erros, `BUILD SUCCESS`.
- Verificação POJO puro (`grep -rnE "org\.springframework|jakarta\.persistence|jakarta\.validation" backend/common/src/main/java/afsdigital/grahamselect/common/tags`): 0 ocorrências (`POJO_CHECK_OK`).
- `cd frontend && flutter analyze`: `No issues found!`.
- `cd frontend && flutter test`: 234 testes passaram (`All tests passed!`), incluindo `tag_remote_datasource_test.dart`, `portfolio_tag_model_test.dart`, `tag_provider_test.dart`, `tag_management_page_test.dart`, `tags_dashboard_integration_test.dart`, `home_page_test.dart`, `dashboard_page_test.dart`, `financial_data_table_test.dart` e `profile_page_test.dart`.

### Completion Notes List

- Migração Liquibase `30-create-portfolio-tags-and-asset-tags.yaml` criada com tabelas `portfolio_tags` e `asset_tags`, FK `fk_asset_tags_tag_id` (`onDelete: CASCADE`) e constraints de unicidade por usuário; `db.changelog-master.yaml` atualizado preservando migrações `01`–`29`.
- Camada de domínio e 8 Use Cases POJO criados em `common/tags` sem dependências Spring/JPA, com normalização de nomes case-insensitive, validação `#RRGGBB`, suporte a tickers B3 e Renda Fixa com espaços/hífens, timestamps em `ZoneOffset.UTC` e associação M:N idempotente por diff (`SetAssetTagsUseCase`).
- Camada `api/tags` criada com `Persistable<UUID>` (`PortfolioTagEntity` e `AssetTagEntity`), adapters JPA, `TagsConfiguration`, handlers RFC 7807 no `GlobalExceptionHandler` (`409`, `404`, `400`), `TagController` (`/api/v1/tags/**`) sem `@RequirePremium` (acesso `FREE`), `INFO` log nas entradas de API e `ERROR` log com stack trace nas exceções.
- Camada `frontend/lib/src/features/tags` criada seguindo Clean Architecture + `TagProvider`, com codificação `Uri.encodeComponent` no `{ticker}`, `DashboardTagFilterBar`, coluna `'Tags'` e `AssetTagSelectorSheet` (criação inline + multi-seleção) em `FinancialDataTable`, filtro visual em memória preservando KPIs globais, `TagManagementPage` (`/tags`) e card `'Tags da Carteira'` em `ProfilePage`.

### File List

**Arquivos criados:**
- `backend/common/src/main/resources/db/changelog/30-create-portfolio-tags-and-asset-tags.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/domain/entities/PortfolioTag.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/domain/entities/AssetTag.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/domain/TagInputValidator.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/service/exceptions/TagNotFoundException.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/service/exceptions/DuplicateTagException.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/dto/TagDetailsDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/dto/TagSummaryDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/dto/AssetTagsAssignmentDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/repository/PortfolioTagPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/repository/AssetTagPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/TagDetailsMapper.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/CreateTagUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/ListTagsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/GetAssetTagsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/RenameTagUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/DeleteTagUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/SetAssetTagsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/AssignAssetToTagUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/tags/application/usecase/UnassignAssetFromTagUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/tags/domain/TagInputValidatorTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/tags/application/usecase/TagUseCasesTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/entities/PortfolioTagEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/entities/AssetTagEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/repositories/JpaPortfolioTagRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/repositories/JpaAssetTagRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/PortfolioTagJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/persistence/jpa/AssetTagJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/infrastructure/spring/TagsConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/CreateOrUpdateTagRequest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/SetAssetTagsRequest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/TagResponse.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/AssetTagsResponse.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/tags/web/TagController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/tags/web/TagControllerIT.java`
- `frontend/lib/src/features/tags/domain/entities/portfolio_tag.dart`
- `frontend/lib/src/features/tags/domain/repositories/tag_repository.dart`
- `frontend/lib/src/features/tags/data/models/portfolio_tag_model.dart`
- `frontend/lib/src/features/tags/data/datasources/tag_remote_datasource.dart`
- `frontend/lib/src/features/tags/data/repositories/tag_repository_impl.dart`
- `frontend/lib/src/features/tags/presentation/providers/tag_provider.dart`
- `frontend/lib/src/features/tags/presentation/pages/tag_management_page.dart`
- `frontend/lib/src/features/tags/presentation/widgets/asset_tag_selector_sheet.dart`
- `frontend/lib/src/features/tags/presentation/widgets/dashboard_tag_filter_bar.dart`
- `frontend/test/features/tags/portfolio_tag_model_test.dart`
- `frontend/test/features/tags/tag_remote_datasource_test.dart`
- `frontend/test/features/tags/tag_provider_test.dart`
- `frontend/test/features/tags/tag_management_page_test.dart`
- `frontend/test/features/tags/tags_dashboard_integration_test.dart`

**Arquivos modificados:**
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java`
- `frontend/lib/main.dart`
- `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`
- `frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart`
- `frontend/lib/src/features/profile/presentation/pages/profile_page.dart`
- `docs/bmad/implementation-artifacts/8-1-criacao-e-associacao-de-tags.md`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`

### Review Findings

**Itens resolvidos nesta revisão (`Patch`):**
- [x] [Review][Patch] Logs `ERROR` com stack trace e limpeza Unicode no domínio/use cases (`TagInputValidator`, `CreateTagUseCase`, `RenameTagUseCase`, `DeleteTagUseCase`, `SetAssetTagsUseCase`, `AssignAssetToTagUseCase`, `UnassignAssetFromTagUseCase`) além da preservação de caracteres de controle em `CreateOrUpdateTagRequest`.
- [x] [Review][Patch] Limite `MAX_TAGS_PER_ASSET = 100` imposto em `SetAssetTagsUseCase` e `AssignAssetToTagUseCase`, remoção de consulta total em `GetAssetTagsUseCase`/`SetAssetTagsUseCase` via `PortfolioTagPort.findAllByUserIdAndIdIn` e deleção em lote via `AssetTagPort.deleteByUserIdAndTickerAndTagIdIn`.
- [x] [Review][Patch] Idempotência concorrente via `AssetTagPort.saveIfAbsent` e `AssetTagJpaAdapter.saveIfAbsent` (`TransactionTemplate(PROPAGATION_REQUIRES_NEW)`), além de distinção de `DuplicateTagException` por constraint em `PortfolioTagJpaAdapter.update`.
- [x] [Review][Patch] Validação de `sub` no `TagController.extractUserId(Jwt)`, sanitização CRLF nos logs de ticker (`sanitizeForLog`) e anotações `@Transactional` nos endpoints adequados.
- [x] [Review][Patch] Preservação de `assetCount` no `PortfolioTag.copyWith` quando `tickers` é omitido; validação de `id`/`name` não vazios e normalização UTC no `PortfolioTagModel.fromJson`.
- [x] [Review][Patch] Validação e `Uri.encodeComponent` de `tagId`/`ticker`, leitura defensiva de `Map`/`List` e tratamento ampliado de erros em `TagRemoteDataSource`, cobertos pelo novo `tag_remote_datasource_test.dart`.
- [x] [Review][Patch] Índice em memória `_tagsByTicker` (`tagsForTicker` em O(1)), guarda de concorrência `_loadGeneration`, `clearError()` e `reset()` acionado no `main.dart` ao trocar usuário ou encerrar sessão via `authStateChanges`.
- [x] [Review][Patch] Criação inline transacional (`createTag` sem auto-atribuição prematura ao cancelar) preservando seleções pendentes, estados `initial`/`loading`/`error` com retry, sincronização inicial segura, `PopScope`, `maxLength: 50` e limite de altura (`0.9 * height`) em `AssetTagSelectorSheet`.
- [x] [Review][Patch] Estado de erro com retry (`tag_filter_retry_button`) e truncamento com reticências em `DashboardTagFilterBar`; `TagState.initial`, `clearError()`, `PopScope`, confirmação `OutlinedButton` em cor de erro e `maxLength: 50` em `TagManagementPage`; otimização de filtro em `FinancialDataTable` e remoção de `Semantics(button: true)` duplicados com rótulos `tagColorLabel`.
- [x] [Review][Patch] Expansão das suítes de testes (`TagInputValidatorTest`, `TagUseCasesTest`, `TagControllerIT`, `portfolio_tag_model_test.dart`, `tag_provider_test.dart`, `tags_dashboard_integration_test.dart` e `tag_remote_datasource_test.dart`).

**Achados avaliados e descartados (por aderência à especificação e arquitetura):**
- Mantidos `toNormalizedNameKey` (`toLowerCase(Locale.ROOT)` sem remoção de acentos), `TICKER_PATTERN`, `@Size(max = 100)` em `SetAssetTagsRequest`, preservação da cor existente no `RenameTagUseCase` quando nula, padrão JPA `CHAR(36)` + `@JdbcTypeCode(SqlTypes.VARCHAR)` + `isNew = false`, e métodos `assignAssetToTag`/`unassignAssetFromTag` no `TagRepository` por corresponderem explicitamente aos critérios de aceite e decisões de design da história.

### Change Log

- **2026-11-14:** Implementação completa da Story 8.1 (Liquibase 30, domínio e casos de uso POJO em `common/tags`, infraestrutura JPA + REST `/api/v1/tags/**` em `api/tags`, e camada Flutter `features/tags` + integração Dashboard/Custódia/Perfil + suítes unitárias, widget e integração). Status atualizado para `review`.
- **2026-11-14:** Code review adversário concluído e todos os 10 grupos de correções (`[Review][Patch]`) aplicados e validados (`common`: 123 testes; `api-service`: 78 testes + 6 `TagControllerIT`; `flutter analyze` sem issues; `flutter test`: 234 testes aprovados). Status atualizado para `done`.
