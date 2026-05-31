---
stepsCompleted: ['create-story', 'dev-story', 'code-review']
lastStep: 'code-review'
lastSaved: '2026-05-31'
---

# Story 3.4: Visualização de Alocação por Classe (Gráfico de Rosca)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an investidor,
I want visualizar o balanceamento da minha carteira entre as diferentes classes de ativos (Ações, FIIs, Tesouro, etc.),
so that eu possa verificar se a minha diversificação está de acordo com a minha estratégia.

## Acceptance Criteria

1. **Dado** que o usuário possui ativos de diferentes classes na carteira
2. **Quando** ele acessa o dashboard de Portfólio
3. **Então** o sistema exibe um gráfico de rosca (Donut Chart) com a distribuição percentual por classe
4. **E** permite clicar em uma classe para filtrar os ativos da tabela de custódia pela classe selecionada
5. **E** exibe a legenda com o valor financeiro total e percentual por categoria
6. **E** exibe um botão "Limpar filtro" para retornar à visão geral quando um filtro estiver ativo

## Tasks / Subtasks

### Backend (Spring Boot — módulo `common`)

- [x] **Task 1: Campo `assetClass` no `CustodyPositionDTO` (AC: 3)**
  - [x] Adicionar campo `String assetClass` como último parâmetro do record `CustodyPositionDTO`
  - [x] Localização: `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/dto/CustodyPositionDTO.java`

- [x] **Task 2: Método `inferAssetClass(String ticker)` no `GetCustodyPositionsUseCase` (AC: 3)**
  - [x] Adicionar método private `inferAssetClass(String ticker)` com as seguintes regras:
    - Remove sufixo `F` de mercado fracionário (ex: `PETR4F` → `PETR4`) antes de inferir
    - `TESOURO`, `CDB`, `LCI`, `LCA`, `DIRETO`, `DEBENTURE` → `"Renda Fixa"`
    - Ticker com 6 chars terminando em `11` → `"FIIs"` (ex: `HGLG11`, `MXRF11`)
    - Ticker terminando em `34` → `"BDRs"` (ex: `TSLA34`, `AAPL34`)
    - Ticker com 5 chars onde o 5º é dígito → `"Ações"` (ex: `PETR4`, `ITUB4`)
    - Fallback → `"Outros"`
  - [x] Chamar `inferAssetClass(ticker)` na construção do `CustodyPositionDTO` no método `execute(String userId)`
  - [x] Localização: `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java`

- [x] **Task 3: Testes do `GetCustodyPositionsUseCaseTest` (AC: 3)**
  - [x] Adicionar testes para a inferência de `assetClass` nos cenários de sucesso
  - [x] Testar todos os tipos: `"Ações"`, `"FIIs"`, `"BDRs"`, `"Renda Fixa"`, `"Outros"`
  - [x] Executar build + testes: `mvn clean compile && mvn test`
  - [x] Localização: `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCaseTest.java`

### Frontend (Flutter — feature `portfolio`)

- [x] **Task 4: Campo `assetClass` na entidade e model Flutter (AC: 3)**
  - [x] Adicionar campo `String assetClass` à entidade `CustodyPosition`
    - Localização: `frontend/lib/src/features/portfolio/domain/entities/custody_position.dart`
  - [x] Adicionar campo `assetClass` ao `CustodyPositionModel` e atualizar `fromJson`
    - Localização: `frontend/lib/src/features/portfolio/data/models/custody_position_model.dart`

- [x] **Task 5: `AssetAllocationDonutChart` widget (AC: 3, 4, 5)**
  - [x] Criar widget `AssetAllocationDonutChart` como `StatefulWidget` com `SingleTickerProviderStateMixin`
  - [x] Implementar gráfico de rosca via `CustomPainter` sem dependências externas (sem `fl_chart`)
  - [x] Usar `AnimationController` com duração de 800ms e curva `Curves.easeOutQuart` para animação de entrada
  - [x] Paleta de cores por classe (vinculada ao Design System):
    - `"Ações"` → `Color(0xFF10B981)` (Emerald Green)
    - `"FIIs"` → `Color(0xFF1E3A8A)` (Navy Blue)
    - `"Renda Fixa"` → `Color(0xFFF59E0B)` (Amber Gold)
    - `"BDRs"` → `Color(0xFF8B5CF6)` (Premium Purple)
    - `"Outros"` → `Color(0xFF64748B)` (Slate Grey)
  - [x] Interação de tap/seleção de fatia: destacar fatia selecionada e notificar o Provider
  - [x] Legenda com valor financeiro total e percentual por classe (abaixo do gráfico)
  - [x] Localização: `frontend/lib/src/features/portfolio/presentation/widgets/asset_allocation_donut_chart.dart`

- [x] **Task 6: Filtro integrado via `PortfolioProvider` (AC: 4, 6)**
  - [x] Adicionar estado `String? selectedAssetClass` ao `PortfolioProvider`
  - [x] Adicionar método `filterByAssetClass(String? assetClass)` que notifica listeners
  - [x] Getter `filteredPositions` que aplica filtro quando `selectedAssetClass != null`
  - [x] Botão "Limpar filtro" exibido no `DashboardPage` quando filtro ativo
  - [x] Localização: `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart`

- [x] **Task 7: Integração no `DashboardPage` (AC: 2, 4)**
  - [x] Adicionar `AssetAllocationDonutChart()` logo abaixo dos KPIs e acima da tabela de custódia
  - [x] Exibir chip/indicador "Filtrado por: [Classe] [Limpar]" quando filtro ativo
  - [x] Localização: `frontend/lib/src/features/portfolio/presentation/pages/dashboard_page.dart`

- [x] **Task 8: Atualizar testes Flutter (AC: 3, 4)**
  - [x] Atualizar `dashboard_page_test.dart` e `dashboard_page_test.mocks.dart`
  - [x] Atualizar `portfolio_provider_test.dart`
  - [x] Atualizar `financial_data_table_test.dart`
  - [x] Atualizar `portfolio_repository_impl_test.dart`
  - [x] Rodar `flutter analyze` e garantir zero erros

## Dev Notes

### Backend — Lógica de Inferência de Classe (`inferAssetClass`)

A inferência é feita no `GetCustodyPositionsUseCase` porque o campo `assetClass` não existe na base de dados — é calculado em tempo de execução a partir do padrão do ticker. Regras aplicadas **nesta ordem**:

```java
private String inferAssetClass(String ticker) {
    if (ticker == null || ticker.isBlank()) return "Outros";
    String t = ticker.trim().toUpperCase();
    // Remove sufixo 'F' do mercado fracionário (ex: PETR4F → PETR4)
    if (t.endsWith("F") && t.length() == 6 && Character.isDigit(t.charAt(4))) {
        t = t.substring(0, 5);
    }
    if (t.startsWith("TESOURO") || t.startsWith("CDB") || t.startsWith("LCI")
            || t.startsWith("LCA") || t.contains("DIRETO") || t.contains("DEBENTURE")) {
        return "Renda Fixa";
    }
    if (t.endsWith("11") && t.length() == 6) return "FIIs";
    if (t.matches("^[A-Z]{4}[34568]$") || t.matches("^[A-Z]{4}11$") || t.matches("^[A-Z]{4}34$")) {
        if (t.endsWith("11")) return "FIIs";
        if (t.endsWith("34")) return "BDRs";
        return "Ações";
    }
    if (t.length() == 5 && Character.isDigit(t.charAt(4))) return "Ações";
    return "Outros";
}
```

**IMPORTANTE:** O código de regex na verificação de BDRs e Ações (`^[A-Z]{4}[34568]$`) precisa ser validado com cuidado — o `t.endsWith("11")` dentro do bloco de regex é **dead code** pois o regex `[34568]` nunca matcheia `11`.

### Frontend — CustomPainter sem fl_chart

O gráfico foi implementado com `CustomPainter` nativo do Flutter para evitar dependências externas. O `AnimatedBuilder` envolve o `CustomPaint` e repassa o valor do `_animation` ao painter.

**Padrão de animação:**
```dart
_animationController = AnimationController(
  vsync: this,
  duration: const Duration(milliseconds: 800),
);
_animation = CurvedAnimation(parent: _animationController, curve: Curves.easeOutQuart);
_animationController.forward();
```

**Interação de toque:** O widget usa `GestureDetector` com `onTapUp` para capturar o ponto de toque e calcular em qual fatia do gráfico o usuário clicou usando trigonometria (`atan2`).

### Frontend — Filtro no Provider

```dart
// Em PortfolioProvider
String? _selectedAssetClass;

void filterByAssetClass(String? assetClass) {
  _selectedAssetClass = assetClass;
  notifyListeners();
}

List<CustodyPosition> get filteredPositions {
  if (_selectedAssetClass == null) return _custodyPositions;
  return _custodyPositions.where((p) => p.assetClass == _selectedAssetClass).toList();
}
```

### Lições Aprendidas da Implementação Real (Commits ba7a936 e 301210a)

1. **Code Review detectou:** Ausência de testes unitários explícitos para `inferAssetClass` — foram adicionados no commit `301210a` no `GetCustodyPositionsUseCaseTest`
2. **Dead Code:** A regex `^[A-Z]{4}11$` dentro do bloco que já verifica `t.endsWith("11") && t.length() == 6` é redundante — FIIs com 6 chars já são capturados antes desse bloco
3. **CustomPainter vs fl_chart:** A decisão de usar `CustomPainter` nativo foi correta pois elimina dependência externa; o resultado visual é equivalente com controle total sobre animações

### Estrutura de Arquivos Alterados

**Backend:**
```
backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/
  application/dto/CustodyPositionDTO.java           ← + campo assetClass
  application/usecase/GetCustodyPositionsUseCase.java ← + inferAssetClass()
  src/test/.../GetCustodyPositionsUseCaseTest.java   ← + testes assetClass
```

**Frontend:**
```
frontend/lib/src/features/portfolio/
  domain/entities/custody_position.dart             ← + campo assetClass
  data/models/custody_position_model.dart           ← + fromJson assetClass
  presentation/
    widgets/asset_allocation_donut_chart.dart       ← NOVO (418 linhas)
    pages/dashboard_page.dart                       ← + integração donut chart
    providers/portfolio_provider.dart               ← + filtro por classe
  test/...                                          ← mocks e testes atualizados
```

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 3.4]
- [Source: docs/bmad/planning-artifacts/architecture.md#D12 Estrutura Flutter]
- [Source: docs/bmad/planning-artifacts/architecture.md#Regras Obrigatórias para Agentes de IA]
- [Source: docs/bmad/project-context.md#Critical Implementation Rules]
- [Commit implementação: ba7a936 — feat(3.4): implementa visualizacao de alocacao por classe com grafico de rosca]
- [Commit correções code review: 301210a — refactor(3.4): corrige apontamentos do code review]
- [GetCustodyPositionsUseCase.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java)
- [CustodyPositionDTO.java](file:///Users/alexandrofs/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/dto/CustodyPositionDTO.java)
- [AssetAllocationDonutChart.dart](file:///Users/alexandrofs/projects/graham-select/frontend/lib/src/features/portfolio/presentation/widgets/asset_allocation_donut_chart.dart)

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.6 (Antigravity — Create Story Workflow)

### Debug Log References

N/A — Story recriada com contexto enriquecido a partir do código real implementado

### Completion Notes List

- **Backend:** Campo `assetClass` adicionado ao `CustodyPositionDTO` como record Java. Método `inferAssetClass(String ticker)` implementado no `GetCustodyPositionsUseCase` com lógica de 5 categorias: Ações, FIIs, BDRs, Renda Fixa e Outros. Testes unitários adicionados após code review.
- **Backend Refactoring (2026-05-31):** Refatorada a lógica de inferência de classes no UseCase para remover redundâncias de regex e código morto. Novo teste unitário exaustivo `shouldInferAssetClassForAllCategoriesCorrectly` adicionado em `GetCustodyPositionsUseCaseTest` com 100% de sucesso.
- **Frontend:** Widget `AssetAllocationDonutChart` criado (418 linhas) usando `CustomPainter` nativo sem dependências externas. Animação de entrada suave com `Curves.easeOutQuart`. Interação de tap para filtrar a tabela de custódia implementada via `PortfolioProvider`. Paleta de cores alinhada ao Design System do projeto.
- **Testes:** `flutter analyze` passou com zero erros no frontend. `mvn test` e `flutter test` (80 testes) executados com sucesso total, sem regressões.

### File List

- **Criados:**
  - `frontend/lib/src/features/portfolio/presentation/widgets/asset_allocation_donut_chart.dart`

- **Modificados:**
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/dto/CustodyPositionDTO.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java`
  - `backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCaseTest.java`
  - `frontend/lib/src/features/portfolio/domain/entities/custody_position.dart`
  - `frontend/lib/src/features/portfolio/data/models/custody_position_model.dart`
  - `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart`
  - `frontend/lib/src/features/portfolio/presentation/pages/dashboard_page.dart`
  - `frontend/test/features/portfolio/presentation/pages/dashboard_page_test.dart`
  - `frontend/test/features/portfolio/presentation/pages/dashboard_page_test.mocks.dart`
  - `frontend/test/features/portfolio/data/repositories/portfolio_repository_impl_test.dart`
  - `frontend/test/features/portfolio/presentation/providers/portfolio_provider_test.dart`
  - `frontend/test/features/portfolio/presentation/widgets/financial_data_table_test.dart`
  - `docs/bmad/implementation-artifacts/sprint-status.yaml`
