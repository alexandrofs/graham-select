# Story 3.4: Visualização de Alocação por Classe (Gráfico de Rosca)

Status: done

## Story

As a investidor,
I want visualizar o balanceamento da minha carteira entre as diferentes classes de ativos (Ações, FIIs, Tesouro, etc.),
so that eu possa verificar se a minha diversificação está de acordo com a minha estratégia.

## Acceptance Criteria

1. **Given** que o usuário possui ativos de diferentes classes na carteira
2. **When** ele acessa o dashboard de Portfólio
3. **Then** o sistema exibe um gráfico de rosca (Donut Chart) com a distribuição percentual por classe
4. **And** permite clicar em uma classe para detalhar os ativos dentro dela
5. **And** exibe a legenda com o valor financeiro total por categoria

## Tasks / Subtasks

### Backend (Spring Boot)
- [x] Adicionar campo `assetClass` no `CustodyPositionDTO` do módulo `common` (AC: 3)
- [x] Implementar a inferência de classe de ativos baseada no ticker no `GetCustodyPositionsUseCase` (AC: 3)
  - Inferir "Ações" para tickers terminados em 3, 4, 5, 6, 8, etc.
  - Inferir "FIIs" para tickers terminados em 11 (ex: HGLG11, MXRF11)
  - Inferir "BDRs" para tickers terminados em 34
  - Inferir "Renda Fixa" para Tesouro ou Renda Fixa
  - Inferir "Outros" como fallback
- [x] Atualizar os testes unitários do `GetCustodyPositionsUseCaseTest` para contemplar o novo campo e regras (AC: 3)
- [x] Executar build completa e testes do backend (`mvn clean compile && mvn test`)

### Frontend (Flutter)
- [x] Adicionar campo `assetClass` no `CustodyPosition` em `lib/src/features/portfolio/domain/entities/custody_position.dart` (AC: 3)
- [x] Atualizar o mapeamento no `CustodyPositionModel` ou similar para decodificar `assetClass` da API (AC: 3)
- [x] Criar o componente `AssetAllocationDonutChart` em `lib/src/features/portfolio/presentation/widgets/` (AC: 3, 5)
  - Utilizar uma biblioteca de gráficos robusta e integrada no Flutter ou desenhar um Donut Chart nativo usando CustomPainter para controle total e sem dependências extras, ou utilizar `fl_chart` se estiver instalado.
  - Exibir a rosca com cores elegantes combinando com o Design System (Navy Blue, Emerald Green, Amber Gold, etc.).
  - Adicionar interação de hover/clique para expandir ou filtrar a tabela de ativos pela classe selecionada (AC: 4).
  - Renderizar legenda com o valor financeiro total por classe e percentual (AC: 5).
- [x] Integrar o gráfico de rosca no `DashboardPage` logo abaixo dos KPIs e acima da tabela de custódia (AC: 2).
- [x] Permitir filtrar a tabela de custódia ao clicar em uma classe do gráfico, exibindo um botão de limpar filtro (AC: 4).
- [x] Garantir que o gráfico seja responsivo e tenha transições e animações fluidas (< 300ms) (NFR2).
- [x] Rodar `flutter analyze` e garantir conformidade com as regras estáticas de análise.

## Dev Notes

- **Aparência Donut Chart:**
  - Animação de entrada do gráfico suave (escala/rotação).
  - Hover / seleção com destaque sutil da fatia selecionada.
  - Legenda alinhada ou abaixo dependendo do tamanho da tela.
- **Interação:**
  - Clicar na fatia de "Ações" deve filtrar a tabela `FinancialDataTable` para exibir apenas os ativos do tipo "Ações".
  - Exibir um indicador visual sutil (ex: chip "Filtrado por: Ações [Limpar]") para retornar ao estado geral.

## References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 3.4]
- [Source: docs/bmad/project-context.md#Critical Implementation Rules]

## Dev Agent Record

### Agent Model Used
Gemini 3.5 Flash (High)
