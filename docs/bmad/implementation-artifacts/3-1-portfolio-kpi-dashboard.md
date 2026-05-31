# Story 3.1: Dashboard KPI (Componente `PortfolioKpiCard`)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a investidor,
I want ver um resumo visual do meu patrimônio (Valor Total, Lucro/Prejuízo, Dividend Yield),
so that eu tenha uma visão rápida da saúde financeira da minha carteira.

## Acceptance Criteria

1. **Given** que o usuário está logado e possui operações cadastradas
2. **When** ele acessa a `HomeScreen`
3. **Then** o sistema calcula e exibe cards com: Patrimônio Total (R$), Rendimento Bruto (%), Dividendos Acumulados e Projeção Mensal
4. **And** usa Skeleton Screens durante o carregamento (NFR2)
5. **And** utiliza as cores Emerald (#10B981) para lucro/positivo e Navy Blue (#1B2A4A) para neutro conforme o Design System
6. **And** as transições entre visões ocorrem em < 300ms (NFR2)

## Tasks / Subtasks

### Backend (Spring Boot)
- [x] Criar DTO `PortfolioSummaryDTO` no módulo `common` (AC: 3)
- [x] Implementar `GetPortfolioSummaryUseCase` no módulo `common` (AC: 3)
  - [x] Calcular Patrimônio Total (Soma de Posições * Cotação Atual)
  - [x] Calcular Rendimento Bruto (%)
  - [x] Calcular Dividendos Acumulados
  - [x] Projetar Renda Mensal (Média 12m ou Yield médio)
- [x] Adicionar endpoint `GET /api/v1/portfolios/summary` no módulo `api` (AC: 1, 3)
  - [x] Garantir isolamento por `userId` extraído do JWT Google/Firebase
- [x] Implementar Testes Unitários para o Use Case e Teste de Integração para o Controller

### Frontend (Flutter)
- [x] Criar `PortfolioSummaryModel` em `lib/src/features/portfolio/data/models/` (AC: 3)
- [x] Implementar `PortfolioService` para chamar a API via Dio (AC: 3)
- [x] Criar `PortfolioProvider` para gerência de estado (AC: 3, 4)
  - [x] Gerenciar estados: `initial`, `loading`, `loaded`, `error`
- [x] Implementar componente `PortfolioKpiCard` em `lib/src/features/portfolio/presentation/widgets/` (AC: 3, 5)
  - [x] Aplicar Design System (Navy Blue, Emerald, Amber Gold)
  - [x] Implementar Skeleton Loading
- [x] Integrar cards na `HomeScreen` (AC: 2, 6)
- [x] Rodar `flutter analyze` e garantir conformidade com `analysis_options.yaml`

## Dev Notes

- **Fuso Horário:** Utilizar `ZoneOffset.UTC` para todos os cálculos de data no backend.
- **Segurança:** Isolamento mandatório por `userId` em todas as queries.
- **Performance:** Rendimento do Dashboard deve ser fluido (< 300ms). Utilizar Cache se necessário para cotações.
- **Design System:**
  - Navy Blue: `#1B2A4A`
  - Emerald Green: `#10B981`
  - Amber Gold: `#F59E0B`
  - Tipografia: DM Sans (headers) / Inter (body) / JetBrains Mono (valores)

### Project Structure Notes

- **Backend:** Seguir Clean Architecture (`domain` ← `application` ← `infrastructure`). Use Case como POJO com método `execute()`.
- **Frontend:** Feature-first structure em `lib/src/features/portfolio/`.

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 3.1]
- [Source: docs/bmad/planning-artifacts/architecture.md#D7 API Design]
- [Source: docs/bmad/project-context.md#Critical Implementation Rules]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#PortfolioKpiCard]

## Dev Agent Record

### Agent Model Used

Gemini 2.0 Flash (CLI YOLO Mode)

### Debug Log References

### Completion Notes List

- Análise completa do motor de contexto realizada - guia abrangente para o desenvolvedor criado.

### File List
- backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/dto/PortfolioSummaryDTO.java
- backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioSummaryUseCase.java
- backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioSummaryUseCaseTest.java
- backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/PortfolioController.java
- backend/api/src/test/java/afsdigital/grahamselect/api/portfolio/web/PortfolioControllerTest.java
- backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/spring/PortfolioSummaryConfiguration.java
- frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart
- frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart
- frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart
- frontend/test/features/portfolio/presentation/providers/portfolio_provider_test.dart
- docs/bmad/implementation-artifacts/3-1-portfolio-kpi-dashboard.md
