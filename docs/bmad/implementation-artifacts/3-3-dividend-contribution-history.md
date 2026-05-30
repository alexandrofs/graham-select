---
stepsCompleted: ['create-story', 'dev-story', 'code-review']
lastStep: 'code-review'
lastSaved: '2026-05-30'
---

# Story 3.3: Histórico de Proventos e Aportes

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an investidor,
I want visualizar graficamente quanto recebi de dividendos e quanto aportei por mês,
so that eu visualize a evolução do meu efeito "bola de neve".

## Acceptance Criteria

1. **Dado** que o usuário acessa a visão de "Evolução" no frontend Flutter
2. **Quando** o sistema filtra as operações do usuário por tipo `DIVIDENDO` e `COMPRA` nos últimos 12 meses
3. **Então** plota um gráfico de barras comparativo por mês exibindo o total de Compras (Aportes) e o total de Dividendos (Proventos) para cada um dos últimos 12 meses
4. **E** garante que todos os 12 meses sejam exibidos sequencialmente de forma contínua, mesmo aqueles que possuam valor zerado para ambos os totais
5. **E** permite alternar a visualização do gráfico de barras para uma visão de tabela detalhada contendo a listagem analítica dos proventos e aportes com filtros de data (data de início e fim)
6. **E** garante o isolamento estrito de dados por usuário (Tenant Isolation) utilizando o token JWT autenticado nas requisições HTTP REST.

## Tasks / Subtasks

- [x] **Task 1: Backend - Criação do Endpoint de Evolução (AC: 2, 3, 4, 6)**
  - [x] Criar os DTOs `PortfolioEvolutionDTO` e `MonthlyEvolutionDTO` no módulo `common`
  - [x] Criar a interface de caso de uso `GetPortfolioEvolutionUseCase` e sua implementação no módulo `common`
  - [x] Implementar a lógica para buscar todas as transações do usuário via `TradePort`, filtrar `COMPRA` e `DIVIDENDO` dos últimos 12 meses e agrupar por mês preenchendo meses sem atividade com valores zerados
  - [x] Expor o endpoint HTTP GET `/api/v1/portfolios/evolution` no `PortfolioController` no módulo `api` sob autenticação JWT
  - [x] Escrever testes unitários e de integração abrangentes para o UseCase e Controller

- [x] **Task 2: Frontend - Camada de Dados e Estado do Flutter (AC: 1, 2, 3, 5)**
  - [x] Criar o modelo `MonthlyEvolutionModel` estendendo a entidade correspondente no Flutter
  - [x] Adicionar a chamada de API `getPortfolioEvolution` no `PortfolioRemoteDataSource` e no `PortfolioRepositoryImpl`
  - [x] Adicionar o estado de evolução (`evolutionStatus`, `evolutionData`, `evolutionError`) e o método `loadEvolutionData` no `PortfolioProvider` com suporte a loading skeleton, sucesso e tratamento de erros

- [x] **Task 3: Frontend - UI e Gráficos da Tela de Evolução (AC: 1, 3, 5)**
  - [x] Criar a página `EvolutionPage` em `frontend/lib/src/features/portfolio/presentation/pages/evolution_page.dart`
  - [x] Desenvolver o widget de gráfico de barras mensal utilizando um layout customizado em Flutter ou widgets de desenho responsivos
  - [x] Implementar a alternância amigável via Tabs ou Botões entre a visão de gráfico e a tabela analítica detalhada
  - [x] Implementar filtros de data (data inicial e final) com validação e recarregamento dos dados na tabela
  - [x] Integrar a navegação da `EvolutionPage` na barra lateral ou abas do aplicativo via `go_router`

## Dev Notes

### Backend Architecture
- **DTOs:**
  - `MonthlyEvolutionDTO` deve conter os campos `String month` (formato "yyyy-MM"), `BigDecimal contributions` e `BigDecimal dividends`.
- **TradeSide Enum:**
  - O enum `TradeSide` já suporta os valores `COMPRA`, `VENDA` e `DIVIDENDO` em [TradeSide.java](file:///Users/alexandrofs/Documents/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/domain/entities/TradeSide.java#L3-L8).
- **UseCase Logic:**
  - Buscar todos os trades usando `TradePort.findAllByUserId(userId)`.
  - Obter a data atual e filtrar as transações com `tradeDate` compreendidas entre `currentDate.minusMonths(11).withDayOfMonth(1)` e `currentDate`.
  - Agrupar por mês e somar os valores das compras (`side == COMPRA` => `price * quantity`) e proventos (`side == DIVIDENDO` => `price * quantity` ou apenas a representation direta de valor em price). *Observação:* Na nossa modelagem, para proventos/dividendos, a quantidade pode ser 1 e o preço representa o valor recebido, ou quantidade x preço. A fórmula de soma deve cobrir multiplicando de forma genérica.
  - Preencher deterministicamente os meses que não tiveram movimentação na carteira para que a resposta possua exatamente 12 registros de meses consecutivos.

### Frontend Architecture
- **Page Layout:**
  - Criar `EvolutionPage` como um `StatelessWidget` ou `StatefulWidget` consumindo o `PortfolioProvider`.
  - Utilizar transições suaves e estados de loading amigáveis (Skeletons / Shimmer) idênticos ao padrão do dashboard.
- **Gráficos:**
  - Desenhar o gráfico de barras comparativo utilizando barras reativas estilizadas em CSS/Material (por exemplo, colunas coloridas lado-a-lado usando `Row` e `Flexible` com contornos suaves, ou através de bibliotecas nativas de desenho) mantendo a paleta de cores moderna e escura da aplicação (tons de azul marinho profundo, verde esmeralda para dividendos e azul ciano para aportes).

### References
- [Sprint Plan: Story 3.3](file:///Users/alexandrofs/Documents/projects/graham-select/docs/bmad/planning-artifacts/sprint-plan.md#L73-L74)
- [TradeSide Enum](file:///Users/alexandrofs/Documents/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/domain/entities/TradeSide.java)
- [PortfolioController](file:///Users/alexandrofs/Documents/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/PortfolioController.java)

## Dev Agent Record

### Agent Model Used
Gemini 1.5 Pro (Antigravity Code Agent)

### Debug Log References
N/A

### Completion Notes List
- **Backend Clean Architecture:** DTOs e UseCase implementados no módulo `common`. Bean registrado com sucesso em `PortfolioSummaryConfiguration`. Endpoint seguro GET `/api/v1/portfolios/evolution` exposto sob autenticação JWT em `PortfolioController`.
- **Frontend Clean Architecture:** Entidades e modelos implementados no Flutter. RemoteDataSource e Repository adaptados. Estado reativo implementado no `PortfolioProvider` com ciclo de vida completo (loading, success, error).
- **Premium UI & UX:** Tela `EvolutionPage` desenvolvida com gráfico de barras reativo puro (com altura mínima limite de 4 pixels para legibilidade de proventos pequenos) e tabela analítica robusta com DatePickers e suporte de scroll responsivo contra overflows de layout.
- **Suite de Testes:** Testes unitários do UseCase e Controller criados no backend. Testes unitários do Provider e de Widget da página inteira no frontend passando com 100% de sucesso.

### File List
- **Criados:**
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/dto/MonthlyEvolutionDTO.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/dto/PortfolioEvolutionDTO.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioEvolutionUseCase.java`
  - `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioEvolutionUseCaseTest.java`
  - `frontend/lib/src/features/portfolio/domain/entities/monthly_evolution.dart`
  - `frontend/lib/src/features/portfolio/data/models/monthly_evolution_model.dart`
  - `frontend/lib/src/features/portfolio/presentation/pages/evolution_page.dart`
  - `frontend/test/features/portfolio/presentation/pages/evolution_page_test.dart`
- **Modificados:**
  - `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/spring/PortfolioSummaryConfiguration.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/PortfolioController.java`
  - `backend/api/src/test/java/afsdigital/grahamselect/api/portfolio/web/PortfolioControllerTest.java`
  - `frontend/lib/src/core/widgets/main_layout.dart`
  - `frontend/lib/main.dart`
  - `frontend/lib/src/features/portfolio/domain/repositories/portfolio_repository.dart`
  - `frontend/lib/src/features/portfolio/data/repositories/portfolio_repository_impl.dart`
  - `frontend/lib/src/features/portfolio/data/datasources/portfolio_remote_data_source.dart`
  - `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart`
  - `frontend/test/features/portfolio/presentation/providers/portfolio_provider_test.dart`
  - `frontend/test/features/portfolio/data/repositories/portfolio_repository_impl_test.dart`
  - `frontend/test/features/portfolio/presentation/widgets/financial_data_table_test.dart`
  - `docs/bmad/implementation-artifacts/sprint-status.yaml`
