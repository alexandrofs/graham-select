---
baseline_commit: 94edb6214f7b2ebb616f7ef7275eb3067404fb52
---
# Story 6.3: Gestão de Ativos e Tickers (Mestre de Dados)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** administrador,  
**I want** cadastrar novos tickers ou corrigir informações de classes de ativos,  
**So that** o sistema reconheça novos papéis que entrem no mercado (IPOs).

---

## Acceptance Criteria

1. **AC1 - Gestão de Cadastro de Tickers e Classes de Ativos (Mestre Global) (FR19):**
   - **Given** que o usuário autenticado possui o papel de administrador (`ROLE_ADMIN` no Spring Security via claim do JWT ou persistido em `users.role = 'ADMIN'`)
   - **When** ele acessa a tela de Gestão de Tickers (`/admin/tickers` no frontend) ou aciona a API REST sob `/api/v1/admin/tickers`
   - **Then** o sistema permite:
     1. **Listar Tickers:** Listagem completa de ativos cadastrados no dicionário mestre, com suporte a filtro textual por ticker/nome (`?query=...`) e filtro por classe de ativo (`?assetClass=...`), retornando os campos: `id`, `ticker`, `name`, `assetClass`, `sector`, `createdAt`, `updatedAt`;
     2. **Cadastrar Novo Ticker:** Endpoint `POST /api/v1/admin/tickers` recebendo `ticker` (2 a 10 caracteres, alfanumérico, sanitizado em `UPPERCASE`), `name` (nome corporativo/descritivo), `assetClass` (obrigatório: `Ações`, `FIIs`, `BDRs`, `ETFs`, `Renda Fixa`, `Outros`) e `sector` (opcional);
     3. **Editar / Corrigir Ticker:** Endpoint `PUT /api/v1/admin/tickers/{id}` permitindo alterar o nome, a classe de ativo correspondente e o setor;
     4. **Remover Ticker:** Endpoint `DELETE /api/v1/admin/tickers/{id}` para exclusão lógica ou física quando o ativo não possuir vínculos de integridade referencial com cotações ou transações.
   - **And** em caso de tentativa de cadastro de um ticker já existente, o backend retorna status HTTP `409 Conflict` (ou `400 Bad Request`) em formato RFC 7807 (`application/problem+json`).

2. **AC2 - Atualização do Dicionário Global e Reconhecimento Automático de Ativos / IPOs (FR19):**
   - **Given** que um novo ativo foi lançado na B3 (ex: IPO de um novo papel ou ativo ainda não negociado por nenhum usuário)
   - **When** o administrador cadastra o Ticker e a Classe correspondente através do painel
   - **Then** o sistema atualiza o dicionário de ativos global (persistindo na tabela `company` com as colunas `asset_class`, `sector`, `created_at` e `updated_at`)
   - **And** a classe cadastrada explicitamente no mestre de dados prevalece sobre a heurística regex estática (`inferAssetClass`), corrigindo discrepâncias (como ETFs terminados em `11` sendo classificados como `ETFs` ou `Ações` em vez de `FIIs`) no cálculo de custódia (`GetCustodyPositionsUseCase`) e recomendações Graham (`GenerateGrahamRecommendationsUseCase`)
   - **And** o `CustodyTickerPort` / `CustodyTickerRepositoryImpl` passa a incluir os tickers cadastrados no mestre de dados no conjunto de ativos sincronizáveis, permitindo que o `SyncMarketDataUseCase` obtenha cotações e indicadores de novos papéis (IPOs) mesmo antes do primeiro trade de usuários.

3. **AC3 - Invalidação e Limpeza de Cache de Market Data:**
   - **Given** que um ticker foi cadastrado ou teve sua classe/informação alterada pelo administrador
   - **When** a operação de gravação ou atualização é finalizada com sucesso
   - **Then** o sistema invalida seletivamente a entrada daquele ticker no cache de Market Data (`cacheManager.getCache("market-data").evict(ticker)`)
   - **And** disponibiliza um endpoint de limpeza global `POST /api/v1/admin/tickers/cache/clear` (acionável via botão na UI "Limpar Cache de Market Data") que executa `cache.clear()`, forçando o recarregamento imediato das cotações na próxima consulta à API da Brapi
   - **And** registra logs estruturados em nível `INFO` informando o evento de limpeza/evicção de cache.

4. **AC4 - Controle de Acesso Baseado em Papel (`ROLE_ADMIN`) e Segurança:**
   - **Given** um usuário comum autenticado (`role: USER`) que tente invocar qualquer endpoint sob `/api/v1/admin/tickers/**`
   - **When** a requisição é interceptada pelo Spring Security (`@RequireAdmin` / `AdminAccessGuard`)
   - **Then** o backend rejeita a chamada com status HTTP `403 Forbidden` no formato padrão RFC 7807 (`application/problem+json`, `type: "urn:problem-type:access-denied"`)
   - **And** requisições não autenticadas retornam HTTP `401 Unauthorized`
   - **And** caso um usuário não-admin acesse `/admin/tickers` no Flutter Web, a tela exibe o estado amigável de **"Acesso Restrito a Administradores"** (`Key('admin_tickers_access_denied_state')`) com botão para retornar ao `/admin/dashboard` ou `/dashboard`.

5. **AC5 - Interface Administrativa Flutter Web (`/admin/tickers`):**
   - **Given** que o administrador está logado na aplicação Flutter Web
   - **When** ele navega para `/admin/tickers` (a partir do botão de atalho no `/admin/dashboard` `Key('admin_nav_tickers_button')`)
   - **Then** a interface apresenta:
     1. **Barra Superior / Ações:** Título "Gestão de Ativos e Tickers (Mestre de Dados)", botão "Novo Ticker" (`Key('admin_tickers_create_button')`), botão "Limpar Cache" (`Key('admin_tickers_clear_cache_button')`), botão "Atualizar" (`Key('admin_tickers_refresh_button')`) e botão de voltar (`Key('admin_tickers_back_button')`);
     2. **Filtros e Busca:** Campo de busca textual por ticker/nome (`Key('admin_tickers_search_field')`) e Dropdown/Chips de filtro por classe (`Key('admin_tickers_class_filter')`: Todos, Ações, FIIs, BDRs, ETFs, Renda Fixa, Outros);
     3. **Tabela / Lista de Tickers (`Key('admin_tickers_table')`):** Exibição em tabela responsiva (ou cartões em telas compactas) com colunas: Ticker, Nome, Classe de Ativo (badge colorido de destaque por categoria), Setor e Ações (Editar `Key('admin_ticker_edit_button_${id}')`, Limpar Cache `Key('admin_ticker_evict_cache_button_${id}')`);
     4. **Modal / Diálogo de Cadastro e Edição (`Key('admin_ticker_form_dialog')`):** Formulário modal contendo campos para Ticker (`Key('admin_ticker_input_ticker')`), Nome (`Key('admin_ticker_input_name')`), Classe (`Key('admin_ticker_input_class')`) e Setor (`Key('admin_ticker_input_sector')`), com botões Salvar (`Key('admin_ticker_save_button')`) e Cancelar (`Key('admin_ticker_cancel_button')`);
     5. **Estados de Carregamento e Vazio:** Skeleton de carregamento (`Key('admin_tickers_skeleton')`) e estado visual quando nenhum ticker for encontrado (`Key('admin_tickers_empty_state')`);
     6. **Feedback Visual:** Exibição de SnackBars de sucesso ao cadastrar, editar ticker ou limpar o cache de Market Data.

6. **AC6 - Logs Estruturados, Padrão UTC e Qualidade de Código (`AGENTS.md` & `project-context.md`):**
   - **Given** as operações de gestão de tickers e limpeza de cache
   - **When** qualquer endpoint da API é acionado ou método de serviço é executado
   - **Then** o sistema registra logs estruturados em nível `INFO` nas entradas contendo os identificadores contextuais relevantes (principal, ticker, classe) e logs em nível `ERROR` com stack trace completo em blocos `catch`
   - **And** todas as datas e horários utilizam estritamente `ZoneOffset.UTC`
   - **And** o contrato OpenAPI 3.0 é devidamente atualizado em `backend/api/src/main/resources/openapi.yaml`
   - **And** todos os testes unitários, testes de integração e análise estática (`mvn clean test` com JDK 21/22 no backend, `flutter analyze` e `flutter test` no frontend) passam com 100% de sucesso e zero warnings.

---

## Tasks / Subtasks

### Backend — Módulo `common` (Persistência, Liquibase, Entidade Company e Enriquecimento de Classes)

- [x] **Task 1: Migração Liquibase `27-add-asset-class-to-company.yaml` e Evolução da Entidade `Company`** (AC: #1, #2, #6)
  - [x] 1.1 Criar changelog Liquibase `backend/common/src/main/resources/db/changelog/27-add-asset-class-to-company.yaml`:
    - Adicionar colunas na tabela `company`:
      - `asset_class`: `VARCHAR(50)`, `nullable: true`, `defaultValue: 'Ações'`
      - `sector`: `VARCHAR(100)`, `nullable: true`
      - `created_at`: `DATETIME`, `nullable: true`
      - `updated_at`: `DATETIME`, `nullable: true`
    - Adicionar índice `idx_company_asset_class` na coluna `asset_class`
    - Rollback explícito: `dropColumn` para as colunas adicionadas
  - [x] 1.2 Registrar `27-add-asset-class-to-company.yaml` no final de `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (após `26-create-dlq-messages-table.yaml`).
  - [x] 1.3 Atualizar entidade de domínio `Company` (`backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/Company.java`):
    - Adicionar campos `name`, `assetClass`, `sector`, `createdAt`, `updatedAt` mantendo construtores legados `Company(String ticker)` e `Company(String id, String ticker)` para 100% de retrocompatibilidade com testes e código legado.
  - [x] 1.4 Atualizar `CompanyRepository` (`backend/common/src/main/java/afsdigital/grahamselect/common/domain/repository/CompanyRepository.java`):
    - Adicionar assinaturas:
      - `List<Company> findAll();`
      - `Optional<Company> findById(String id);`
      - `void deleteById(String id);`
      - `boolean existsByTicker(String ticker);`
  - [x] 1.5 Atualizar `GetCustodyPositionsUseCase` (`backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java`):
    - No mapeamento da posição, verificar se o `Company` retornado possui `assetClass` definido e não vazio; caso positivo, usar `company.getAssetClass()`; caso contrário, aplicar fallback para `inferAssetClass(ticker)`.
    - Garantir que nenhum teste existente de custódia quebre.

---

### Backend — Módulo `api` (Persistência JPA, Limpeza de Cache, Endpoints REST e OpenAPI)

- [x] **Task 2: Atualizar Entidade JPA `CompanyEntity` e `CompanyJpaRepository` no Módulo `api`** (AC: #1, #2, #3)
  - [x] 2.1 Atualizar `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/CompanyEntity.java`:
    - Adicionar mapeamentos JPA:
      ```java
      @Column(name = "asset_class", length = 50)
      private String assetClass;

      @Column(name = "sector", length = 100)
      private String sector;

      @Column(name = "created_at")
      private LocalDateTime createdAt;

      @Column(name = "updated_at")
      private LocalDateTime updatedAt;
      ```
    - Adicionar `@PrePersist` e `@PreUpdate` definindo `createdAt` e `updatedAt` com `LocalDateTime.now(ZoneOffset.UTC)`.
  - [x] 2.2 Atualizar `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/CompanyJpaRepository.java`:
    - Adicionar métodos:
      - `boolean existsByTickerIgnoreCase(String ticker);`
      - `Optional<CompanyEntity> findByTickerIgnoreCase(String ticker);`
      - `List<CompanyEntity> findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(String tickerPart, String namePart);`
      - `List<CompanyEntity> findByAssetClassIgnoreCase(String assetClass);`
  - [x] 2.3 Atualizar `CustodyTickerRepositoryImpl` (`backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/CustodyTickerRepositoryImpl.java`):
    - Injetar `CompanyJpaRepository` e combinar os tickers ativos de custódia com todos os tickers presentes no dicionário mestre de `CompanyEntity`, eliminando duplicatas e garantindo que novos ativos cadastrados (IPOs) entrem no pool de sync de cotações.

- [x] **Task 3: Implementar `AdminTickerService` com Gerenciamento de Cache** (AC: #1, #2, #3, #4, #6)
  - [x] 3.1 Criar DTOs em `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/`:
    - `AdminTickerDto(String id, String ticker, String name, String assetClass, String sector, OffsetDateTime createdAt, OffsetDateTime updatedAt)`
    - `CreateAdminTickerRequest(String ticker, String name, String assetClass, String sector)`
    - `UpdateAdminTickerRequest(String name, String assetClass, String sector)`
    - `CacheEvictionResultDto(String message, OffsetDateTime timestamp)`
  - [x] 3.2 Criar serviço `AdminTickerService` em `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminTickerService.java`:
    - Injetar `CompanyJpaRepository` e `CacheManager`.
    - **Método `listTickers(String query, String assetClass)`:**
      - Se filtros estiverem presentes, aplicar busca combinada; senão listar todos ordenados por `ticker ASC`.
      - Mapear para `AdminTickerDto` convertendo timestamps para `OffsetDateTime` em `ZoneOffset.UTC`.
    - **Método `createTicker(CreateAdminTickerRequest request)`:**
      - Validar se `request.ticker()` não é nulo/em branco e se já existe no banco (`existsByTickerIgnoreCase`). Se já existir, lançar `DuplicateResourceException` ("Ticker já cadastrado no dicionário mestre.").
      - Sanitizar ticker: `request.ticker().trim().toUpperCase()`.
      - Normalizar classe (padrão `Ações` caso nulo/em branco).
      - Salvar nova `CompanyEntity` com UUID gerado e timestamps em UTC.
      - Evict seletivo no cache de Market Data: `evictMarketDataCache(sanitizedTicker)`.
      - Retornar `AdminTickerDto`.
    - **Método `updateTicker(String id, UpdateAdminTickerRequest request)`:**
      - Buscar entidade por `id`. Lançar `ResourceNotFoundException` caso não encontrada.
      - Atualizar `name`, `assetClass`, `sector` e `updatedAt`.
      - Salvar entidade e executar evict no cache para o ticker correspondente.
      - Retornar `AdminTickerDto`.
    - **Método `deleteTicker(String id)`:**
      - Buscar entidade por `id`. Lançar `ResourceNotFoundException` caso não encontrada.
      - Executar `companyJpaRepository.delete(entity)` e evict no cache de cotação.
    - **Método `clearMarketDataCache()`:**
      - Obter cache `"market-data"` via `cacheManager.getCache("market-data")`. Se não nulo, executar `cache.clear()`.
      - Logar `log.info("Market data cache cleared by administrator")`.
      - Retornar `new CacheEvictionResultDto("Cache de Market Data limpo com sucesso.", OffsetDateTime.now(ZoneOffset.UTC))`.
    - **Método `evictMarketDataCache(String ticker)`:**
      - Obter cache `"market-data"` e executar `cache.evict(ticker)` e `cache.evict(ticker.toUpperCase())`.
      - Logar `log.info("Evicted ticker {} from market-data cache", ticker)`.

- [x] **Task 4: Implementar `AdminTickerController` e Atualizar OpenAPI** (AC: #1, #3, #4, #6)
  - [x] 4.1 Criar `AdminTickerController` em `backend/api/src/main/java/afsdigital/grahamselect/api/admin/web/AdminTickerController.java`:
    - Anotado com `@RestController`, `@RequestMapping("/api/v1/admin/tickers")`, `@RequiredArgsConstructor`, `@Slf4j`.
    - Proteger com `@RequireAdmin` nos métodos ou na classe.
    - Endpoints:
      - `GET /api/v1/admin/tickers` (parâmetros opcionais: `query`, `assetClass`) -> Retorna `ResponseEntity<Map<String, Object>>` com envelope `{ "data": List<AdminTickerDto> }`.
      - `POST /api/v1/admin/tickers` -> Retorna HTTP 201 com `{ "data": AdminTickerDto }`.
      - `PUT /api/v1/admin/tickers/{id}` -> Retorna HTTP 200 com `{ "data": AdminTickerDto }`.
      - `DELETE /api/v1/admin/tickers/{id}` -> Retorna HTTP 204 No Content.
      - `POST /api/v1/admin/tickers/cache/clear` -> Retorna HTTP 200 com `{ "data": CacheEvictionResultDto }`.
      - `POST /api/v1/admin/tickers/{ticker}/cache/evict` -> Retorna HTTP 200 com `{ "data": CacheEvictionResultDto }`.
  - [x] 4.2 Atualizar `backend/api/src/main/resources/openapi.yaml`:
    - Documentar tag `Admin - Tickers` e as rotas `/admin/tickers`, `/admin/tickers/{id}`, `/admin/tickers/cache/clear`, `/admin/tickers/{ticker}/cache/evict` com schemas de requisição e resposta.
  - [x] 4.3 Implementar testes de integração e unitários:
    - `AdminTickerServiceTest`: testes cobrindo CRUD, validação de duplicidade e chamadas ao cacheManager.
    - `AdminTickerControllerIT`: testes simulando requisições com `ROLE_ADMIN` (sucesso 200/201/204), `ROLE_USER` (403 Forbidden) e anônimo (401 Unauthorized).

---

### Frontend — Flutter Web (Camada de Dados, Provider, Telas e Testes de Widget)

- [x] **Task 5: Modelos, Datasource e Repositório de Tickers no Módulo Admin** (AC: #1, #4, #5)
  - [x] 5.1 Criar entidade de domínio `AdminTicker` em `frontend/lib/src/features/admin/domain/entities/admin_ticker.dart`:
    - Campos: `id`, `ticker`, `name`, `assetClass`, `sector`, `createdAt`, `updatedAt`.
  - [x] 5.2 Criar modelo `AdminTickerModel` em `frontend/lib/src/features/admin/data/models/admin_ticker_model.dart`:
    - Métodos `fromJson(Map<String, dynamic> json)`, `toJson()`, `fromJsonList(dynamic json)`.
  - [x] 5.3 Atualizar `AdminRemoteDatasource` (`frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart`):
    - Adicionar métodos:
      - `Future<List<AdminTickerModel>> getTickers({String? query, String? assetClass})`
      - `Future<AdminTickerModel> createTicker({required String ticker, required String name, required String assetClass, String? sector})`
      - `Future<AdminTickerModel> updateTicker({required String id, required String name, required String assetClass, String? sector})`
      - `Future<void> deleteTicker(String id)`
      - `Future<void> clearMarketDataCache()`
      - `Future<void> evictTickerCache(String ticker)`
    - Tratar erros HTTP 403 lançando `AdminAccessDeniedException`.
  - [x] 5.4 Atualizar `AdminRepository` e `AdminRepositoryImpl`:
    - Declarar e implementar os métodos correspondentes convertendo models em entities.

- [x] **Task 6: Implementar `AdminTickerProvider` e Gestão de Estado** (AC: #1, #3, #4, #5)
  - [x] 6.1 Criar `AdminTickerProvider` em `frontend/lib/src/features/admin/presentation/providers/admin_ticker_provider.dart`:
    - Estados: `List<AdminTicker> tickers`, `bool isLoading`, `bool isActionLoading`, `String? errorMessage`, `bool isAccessDenied`, `String selectedAssetClassFilter`, `String searchQuery`.
    - Métodos:
      - `loadTickers()`: busca da API e filtra por query/classe localmente ou via query params.
      - `setSearchQuery(String query)` e `setAssetClassFilter(String assetClass)`.
      - `createTicker(...)`, `updateTicker(...)`, `deleteTicker(...)`.
      - `clearCache()` e `evictTicker(String ticker)`.

- [x] **Task 7: Telas, Diálogo de Edição/Criação e Navegação** (AC: #1, #4, #5)
  - [x] 7.1 Criar diálogo `AdminTickerFormDialog` em `frontend/lib/src/features/admin/presentation/widgets/admin_ticker_form_dialog.dart`:
    - Suporte a modo criação e modo edição.
    - Campos: Ticker (`Key('admin_ticker_input_ticker')`), Nome (`Key('admin_ticker_input_name')`), Classe Dropdown (`Key('admin_ticker_input_class')`), Setor (`Key('admin_ticker_input_sector')`).
    - Validação de campos obrigatórios e botões Salvar (`Key('admin_ticker_save_button')`) e Cancelar (`Key('admin_ticker_cancel_button')`).
  - [x] 7.2 Criar página `AdminTickersPage` em `frontend/lib/src/features/admin/presentation/pages/admin_tickers_page.dart`:
    - Componentes:
      - `_AdminTickersSkeleton` (`Key('admin_tickers_skeleton')`)
      - `_AdminTickersAccessDeniedView` (`Key('admin_tickers_access_denied_state')`)
      - `_AdminTickersEmptyState` (`Key('admin_tickers_empty_state')`)
      - Barra de ferramentas com busca (`Key('admin_tickers_search_field')`), filtro por classe (`Key('admin_tickers_class_filter')`), botão novo ticker (`Key('admin_tickers_create_button')`), botão limpar cache (`Key('admin_tickers_clear_cache_button')`), botão atualizar (`Key('admin_tickers_refresh_button')`) e botão voltar (`Key('admin_tickers_back_button')`).
      - Tabela responsiva de tickers (`Key('admin_tickers_table')`) com badges temáticos por classe (Ações, FIIs, BDRs, ETFs, Renda Fixa, Outros) e botões de ação para editar (`Key('admin_ticker_edit_button_${id}')`) e evict cache (`Key('admin_ticker_evict_cache_button_${id}')`).
  - [x] 7.3 Atualizar navegação e injeção de dependências:
    - Em `frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart`, adicionar botão "Gestão de Tickers" (`Key('admin_nav_tickers_button')`) navegando para `/admin/tickers`.
    - Em `frontend/lib/main.dart`, registrar rota `/admin/tickers` no `GoRouter` e prover `AdminTickerProvider` no `MultiProvider`.

- [x] **Task 8: Testes Automatizados no Frontend e Validação Geral** (AC: #5, #6)
  - [x] 8.1 Criar testes de unidade e widget:
    - `admin_ticker_model_test.dart`
    - `admin_ticker_provider_test.dart`
    - `admin_tickers_page_test.dart`
    - `admin_dashboard_page_test.dart` (validar presença do botão de navegação para tickers)
  - [x] 8.2 Executar validação estática e regressão:
    - Backend: `./mvnw clean test` em todos os módulos (`common`, `api`, `valuation-service`).
    - Frontend: `flutter analyze` e `flutter test`.

### Review Findings

- [x] [Review][Decision] Valor padrão `defaultValue: 'Ações'` na migration `27-add-asset-class-to-company.yaml` vs fallback `inferAssetClass` — A migration preenche `asset_class = 'Ações'` em todos os registros já existentes na tabela `company`, fazendo com que `GetCustodyPositionsUseCase` nunca acione o fallback `inferAssetClass(ticker)` para FIIs, BDRs e outros ativos legados já persistidos no banco.
- [x] [Review][Decision] Exposição do botão de exclusão de ticker na UI (`AdminTickersPage`) — O AC1 item 4 prevê a remoção de ticker e toda a pilha (`AdminRemoteDatasource.deleteTicker`, `AdminRepository.deleteTicker`, `AdminTickerProvider.deleteTicker`) foi implementada no frontend, mas a tabela em `AdminTickersPage` seguiu o AC5 item 3 e expôs apenas os botões de Editar e Invalidar Cache.
- [x] [Review][Patch] Priorizar `assetClass` do dicionário mestre sobre a heurística `inferAssetClass` em `GenerateGrahamRecommendationsUseCase` conforme exigido pelo AC2 [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:117]
- [x] [Review][Patch] Tratar vínculos de integridade referencial (`trades` e `DataIntegrityViolationException` de `stock_price`/`company_intrinsic_value`) em `AdminTickerService.deleteTicker` e `createTicker` retornando HTTP 409 RFC 7807 em vez de HTTP 500 [backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminTickerService.java:140]
- [x] [Review][Patch] Adicionar validação alfanumérica para `ticker`, limites `@Size` nos campos e validação de `assetClass` contra a lista permitida nos DTOs, `AdminTickerService` e `AdminTickerFormDialog` [backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/CreateAdminTickerRequest.java:6]
- [x] [Review][Patch] Implementar persistência real em `CompanyJpaAdapter.save(Company)`, guardas contra `null`/`blank` em `findById`/`deleteById` e remover métodos não utilizados em `CompanyJpaRepository` [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/CompanyJpaAdapter.java:33]
- [x] [Review][Patch] Corrigir tipo de ID (`String` vs `UUID`) e mapear todos os campos de `Company` (`name`, `assetClass`, `sector`, `createdAt`, `updatedAt`) em `CompanyRepositoryImpl` e `CompanyEntity` do `valuation-service` [backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/CompanyRepositoryImpl.java:20]
- [x] [Review][Patch] Suportar fallback de ticker fracionário (sufixo `F`, ex.: `BOVA11F` -> `BOVA11`) no lookup de `Company` em `GetCustodyPositionsUseCase` (com merge seguro no `toMap`) e na evicção de cache em `AdminTickerService.evictMarketDataCache` [backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java:78]
- [x] [Review][Patch] Escapar caracteres curinga (`%` e `_`) na busca `LIKE` de `CompanyJpaRepository.searchTickers` e usar projeção de tickers em `CustodyTickerRepositoryImpl` em vez de `findAll()` completo [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/CompanyJpaRepository.java:27]
- [x] [Review][Patch] Incluir stack trace no `log.error` de `handleIllegalArgument`, adicionar handler para `MethodArgumentNotValidException` em `GlobalExceptionHandler` e evitar triplicação de stack trace nos logs de erro 4xx [backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java:64]
- [x] [Review][Patch] Permitir limpar o campo opcional `sector` na edição de ticker (`PUT /api/v1/admin/tickers/{id}`), usar cast seguro de `Map` e codificar o ticker com `Uri.encodeComponent` em `AdminRemoteDatasource` [frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart:273]
- [x] [Review][Patch] Sincronizar filtro textual (`setSearchQuery`) com `loadTickers()` em `AdminTickerProvider` para não truncar a lista em memória ao limpar a busca e proteger contra race conditions em `loadTickers()` [frontend/lib/src/features/admin/presentation/providers/admin_ticker_provider.dart:39]
- [x] [Review][Patch] Exibir a mensagem de erro real (`provider.errorMessage` / RFC 7807 `detail`) em `AdminTickerFormDialog`, tratar `assetClass` em branco no `AdminTickerModel.fromJson` e preservar `assetClass` fora da lista padrão ao abrir edição [frontend/lib/src/features/admin/presentation/widgets/admin_ticker_form_dialog.dart:48]
- [x] [Review][Patch] Exibir estado de erro com botão de retry quando `loadTickers()` falhar em `AdminTickersPage` e desabilitar ações concorrentes na tabela durante `isActionLoading` [frontend/lib/src/features/admin/presentation/pages/admin_tickers_page.dart:160]

---

## Dev Notes

- **Conformidade Arquitetural Spring Boot Multi-Módulo:**
  - O Liquibase e a interface de repositório `CompanyRepository` residem no módulo `common`.
  - A entidade JPA `CompanyEntity` utilizada na API e o serviço `AdminTickerService` / `AdminTickerController` residem no módulo `api`.
  - A entidade `CompanyEntity` no módulo `valuation-service` não necessita mapear as novas colunas caso não as utilize diretamente, pois o JPA ignora colunas sobressalentes da tabela.
- **Cache Eviction Strategy:**
  - O bean `cacheManager` configurado em `MarketDataServiceConfiguration` utiliza Caffeine com o cache nomeado `"market-data"`.
  - Toda mutação em um ticker deve executar `cache.evict(ticker)` para garantir que cotações antigas em memória não continuem sendo servidas.
  - A ação de "Limpar Cache" executa `cache.clear()`, útil em ambientes onde cotações da B3 precisem ser recarregadas em massa após horário de fechamento.
- **Prevalência da Classe Cadastrada:**
  - A inferência regex em `GetCustodyPositionsUseCase` (`inferAssetClass`) servirá estritamente como fallback quando o ativo não possuir `assetClass` cadastrado na base de dados.
  - Isso soluciona o problema comum de mercado onde ETFs com código final `11` (ex: `BOVA11`, `SMAL11`, `IVVB11`) eram incorretamente tratados como `FIIs`.
- **Compatibilidade Retroativa:**
  - A migração `27-add-asset-class-to-company.yaml` adiciona as colunas com valor padrão (`defaultValue: 'Ações'`) e `nullable: true`, garantindo que os dados e testes existentes não sofram quebra de integridade.

### Project Structure Notes

- Backend Changes:
  - `backend/common/src/main/resources/db/changelog/27-add-asset-class-to-company.yaml` (NEW)
  - `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (UPDATE)
  - `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/Company.java` (UPDATE)
  - `backend/common/src/main/java/afsdigital/grahamselect/common/domain/repository/CompanyRepository.java` (UPDATE)
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java` (UPDATE)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/CompanyEntity.java` (UPDATE)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/CompanyJpaRepository.java` (UPDATE)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/CustodyTickerRepositoryImpl.java` (UPDATE)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/AdminTickerDto.java` (NEW)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/CreateAdminTickerRequest.java` (NEW)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/UpdateAdminTickerRequest.java` (NEW)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/CacheEvictionResultDto.java` (NEW)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminTickerService.java` (NEW)
  - `backend/api/src/main/java/afsdigital/grahamselect/api/admin/web/AdminTickerController.java` (NEW)
  - `backend/api/src/main/resources/openapi.yaml` (UPDATE)
- Frontend Changes:
  - `frontend/lib/src/features/admin/domain/entities/admin_ticker.dart` (NEW)
  - `frontend/lib/src/features/admin/data/models/admin_ticker_model.dart` (NEW)
  - `frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart` (UPDATE)
  - `frontend/lib/src/features/admin/data/repositories/admin_repository_impl.dart` (UPDATE)
  - `frontend/lib/src/features/admin/domain/repositories/admin_repository.dart` (UPDATE)
  - `frontend/lib/src/features/admin/presentation/providers/admin_ticker_provider.dart` (NEW)
  - `frontend/lib/src/features/admin/presentation/widgets/admin_ticker_form_dialog.dart` (NEW)
  - `frontend/lib/src/features/admin/presentation/pages/admin_tickers_page.dart` (NEW)
  - `frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart` (UPDATE)
  - `frontend/lib/main.dart` (UPDATE)

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 6.3: Gestão de Ativos e Tickers (Mestre de Dados)]
- [Source: docs/bmad/planning-artifacts/prd.md#4. Market Data & AI Engine (The Brain) - FR19]
- [Source: docs/bmad/planning-artifacts/sprint-plan.md#Task 6.3.1: CRUD simples via painel de admins para o dicionário global de Tickers]
- [Source: docs/bmad/implementation-artifacts/6-1-admin-observability-dashboard.md]
- [Source: docs/bmad/implementation-artifacts/6-2-failed-ingestion-dlq-management.md]
- [Source: docs/bmad/project-context.md]

---

## Dev Agent Record

### Agent Model Used

Gemini 4 Argon (Medium)

### Debug Log References

- Análise estática de classes de ativos e cotações de mercado via Caffeine Cache em `BrapiMarketDataAdapter`.
- Levantamento de integrações de `Company` e `inferAssetClass` em `GetCustodyPositionsUseCase`.
- Análise de segurança e RBAC utilizando `@RequireAdmin` e `AdminAccessGuard`.
- Execução completa dos testes do backend (`common`, `api-service`, `valuation-service`) com JDK 22 e frontend (`flutter analyze` + 200 testes de unidade e widget).

### Completion Notes List

- Implementada migração Liquibase `27-add-asset-class-to-company.yaml` adicionando colunas `asset_class`, `sector`, `created_at` e `updated_at` na tabela `company` e evolução de `Company`, `CompanyRepository` e `GetCustodyPositionsUseCase` priorizando a classe persistida no dicionário mestre sobre a heurística regex.
- Implementados `AdminTickerService` e `AdminTickerController` sob `/api/v1/admin/tickers` com proteção `@RequireAdmin`, invalidação seletiva e limpeza global do cache `"market-data"`, logs estruturados (`INFO`/`ERROR`), timestamps em `ZoneOffset.UTC` e documentação no `openapi.yaml`.
- Atualizado `CustodyTickerRepositoryImpl` para combinar os tickers em custódia com os tickers do dicionário mestre (`company`), permitindo sincronização de cotações de novos ativos/IPOs.
- Implementada a tela administrativa Flutter Web `AdminTickersPage` (`/admin/tickers`), `AdminTickerFormDialog`, `AdminTickerProvider`, `AdminTickerModel`, rotas no `GoRouter` e botão de acesso rápido no `AdminDashboardPage`.
- Todos os testes unitários, de integração e de widget foram executados e aprovados com zero warnings no `flutter analyze`.

### File List

- `backend/common/src/main/resources/db/changelog/27-add-asset-class-to-company.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/Company.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/repository/CompanyRepository.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCaseTest.java`
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/CompanyRepositoryImpl.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/CustodyTickerRepositoryImpl.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/CompanyJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/CompanyEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/CompanyJpaRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/MarketDataServiceConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/AdminTickerDto.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/CreateAdminTickerRequest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/UpdateAdminTickerRequest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/dto/CacheEvictionResultDto.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminTickerService.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/exceptions/DuplicateResourceException.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/exceptions/ResourceNotFoundException.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/web/AdminTickerController.java`
- `backend/api/src/main/resources/openapi.yaml`
- `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/CustodyTickerRepositoryImplTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/admin/application/service/AdminTickerServiceTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/admin/web/AdminTickerControllerIT.java`
- `frontend/lib/main.dart`
- `frontend/lib/src/features/admin/domain/entities/admin_ticker.dart`
- `frontend/lib/src/features/admin/domain/repositories/admin_repository.dart`
- `frontend/lib/src/features/admin/data/models/admin_ticker_model.dart`
- `frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart`
- `frontend/lib/src/features/admin/data/repositories/admin_repository_impl.dart`
- `frontend/lib/src/features/admin/presentation/providers/admin_ticker_provider.dart`
- `frontend/lib/src/features/admin/presentation/widgets/admin_ticker_form_dialog.dart`
- `frontend/lib/src/features/admin/presentation/pages/admin_tickers_page.dart`
- `frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart`
- `frontend/test/features/admin/data/models/admin_ticker_model_test.dart`
- `frontend/test/features/admin/presentation/providers/admin_ticker_provider_test.dart`
- `frontend/test/features/admin/presentation/pages/admin_tickers_page_test.dart`
- `frontend/test/features/admin/presentation/pages/admin_dashboard_page_test.dart`
- `frontend/test/features/admin/presentation/pages/admin_dlq_page_test.dart`
- `frontend/test/features/admin/presentation/providers/admin_dlq_provider_test.dart`
- `frontend/test/features/admin/presentation/providers/admin_observability_provider_test.dart`
- `docs/bmad/implementation-artifacts/6-3-master-data-ticker-management.md`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`

### Change Log

- **2026-11-11:** Implementação completa da Story 6.3 (Gestão de Ativos e Tickers - Mestre de Dados) no backend (`common`, `api`, `valuation-service`) e frontend Flutter Web (`/admin/tickers`), incluindo invalidação seletiva/global de cache de Market Data e suíte completa de testes automatizados.
