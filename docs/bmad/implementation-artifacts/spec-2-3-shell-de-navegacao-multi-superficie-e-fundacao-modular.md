---
title: 'Story 2.3: Shell de Navegação Multi-Superfície e Fundação Modular com Feature Flags'
type: 'feature'
created: '2026-11-27'
status: 'done'
baseline_commit: '734026d09a394c0d4430534d725589d74a110cf9'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O `MainLayout` atual em `frontend/lib/src/core/widgets/main_layout.dart` usa um breakpoint único (`< 900`), exibe 10 itens amontoados na barra inferior mobile e na barra lateral, não possui `NavigationRail` para tablet, `SkipToContentLink` para acessibilidade por teclado, `LargeContentPresenter` para escalas tipográficas altas, nem suporte a *feature flags* modulares, enquanto `FinancialDataTable<T>` ainda força `SingleChildScrollView(scrollDirection: Axis.horizontal)` no mobile.

**Approach:** Refatorar `MainLayout` e o `GoRouter` em `main.dart` para adotar `StatefulShellRoute.indexedStack` com 3 superfícies responsivas (`< 600dp` `BottomNavigationBar` com 4 abas primárias, `600dp–1024dp` `NavigationRail` de `72dp`, `>= 1024dp` `Sidebar` de `240dp–260dp` agrupando todas as 18 rotas legadas + `/kerno-ai` e `SkipToContentLink`), introduzir `FeatureFlags` (`ENABLE_EXPENSES_MODULE = false`) espelhada em backend/Helm/CI com módulo `features/expenses` desacoplado, e tornar `FinancialDataTable<T>` responsivo sem scroll horizontal no mobile.

## Boundaries & Constraints

**Always:**
- Usar tokens do Design System Intergeracional (`KernoThemeData.of(context)`, `KernoColors`, `KernoSpacing`, `KernoRadius`, `KernoTypography` com `FontFeature.tabularFigures()`), garantindo alvos de toque `>= 48x48dp`, contraste WCAG AA/AAA em light/dark mode e zero `TextOverflow.ellipsis` em valores monetários/percentuais.
- No Mobile (`< 600dp`), exibir exatamente **4 abas primárias** (**Carteira** `/dashboard`, **GPS de Aporte** `/gps-aporte`, **Kerno AI** `/kerno-ai`, **Diagrama** `/allocation-strategy`) e, quando `MediaQuery.textScalerOf(context) > 1.35`, ativar `LargeContentPresenter` na barra inferior e simplificar o `_TopBar` para evitar overflow.
- Manter todas as **18 rotas autenticadas existentes** funcionando (mantendo `/graham-recommendations` como alias/redirecionamento para `/gps-aporte`), preservar os `Key`s existentes (`admin_dashboard_nav_button`, `commercial_plan_badge`), preservar `BiometricLockGate` e os parâmetros de injeção de `GrahamSelectApp`.
- Ocultar o 5º módulo (**Gastos**, `features/expenses` / `/expenses`) quando `ENABLE_EXPENSES_MODULE` for `false` e espelhar a variável em `application.yml`, `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `.github/workflows/deploy.yml` e `start-app.sh`.
- Eliminar `SingleChildScrollView(scrollDirection: Axis.horizontal)` em `core/widgets/financial_data_table.dart` para viewports `< 600dp`, convertendo para `ListView` de cartões empilhados.

**Never:**
- Quebrar rotas públicas (`/`, `/login`, `/docs`), fluxos de autenticação/biometria (Story 2.2) ou constantes estáticas legadas de `AppTheme`.
- Acoplar `features/expenses` a `features/contribution_gps` ou `features/portfolio`, nem alterar endpoints REST ou identificadores internos legados (`graham-select`).
- Implementar o conteúdo completo do Dashboard Patrimonial (Story 2.4), o sheet de ingestão B3/CRUD (Story 2.5) ou o motor real do Kerno AI (Épico 5).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Navegação Mobile | Viewport `< 600dp`, `ENABLE_EXPENSES_MODULE=false` | `BottomNavigationBar` com 4 abas (Carteira, GPS de Aporte, Kerno AI, Diagrama) e `48dp` min | Rota `/expenses` redireciona para `/dashboard` quando flag `false` |
| Navegação Tablet | Viewport `600dp <= w < 1024dp` | `NavigationRail` (`72dp`) + `_TopBar` global | Sem overflow ao alternar orientação |
| Navegação Desktop | Viewport `>= 1024dp` | `Sidebar` (`240–260dp`) com 5 grupos (Grupo 5 Admin só para `role=='ADMIN'`) e `SkipToContentLink` no 1º `Tab` | Grupo Admin oculto se `profile == null` ou `role != 'ADMIN'` |
| Escala tipográfica alta | Viewport `360dp` e `textScaler > 1.35` | `_BottomNavBar` em `LargeContentPresenter` (long-press exibe rótulo) e `_TopBar` compacta | Zero `RenderFlex overflow` até `textScaler = 2.0` |
| Tabela responsiva | `FinancialDataTable<T>` em `< 600dp` vs `>= 600dp` | `< 600dp` renderiza `ListView` de cartões verticais; `>= 600dp` renderiza `DataTable` tabular | Lista vazia exibe `emptyMessage` centralizado sem erro |

</frozen-after-approval>

## Code Map

- `frontend/lib/src/core/config/feature_flags.dart` -- Criar classe `FeatureFlags` com `enableExpensesModule` (`const bool.fromEnvironment('ENABLE_EXPENSES_MODULE', defaultValue: false)`) e suporte a override em testes.
- `frontend/lib/src/core/widgets/skip_to_content_link.dart` -- Criar `SkipToContentLink` com `FocusableActionDetector` (WCAG 2.4.1) visível ao foco de teclado para saltar ao `FocusNode` do conteúdo principal.
- `frontend/lib/src/core/widgets/main_layout.dart` (L8–349) -- Refatorar para 3 breakpoints (`< 600`, `600–1024`, `>= 1024`), 4 abas mobile (ou 5 se `enableExpensesModule`), `NavigationRail` tablet, `Sidebar` agrupada desktop, `LargeContentPresenter` e `_TopBar` responsiva.
- `frontend/lib/src/core/widgets/financial_data_table.dart` (L1–33) -- Atualizar `FinancialDataTable<T>` com `LayoutBuilder` (`< 600dp` -> `ListView` de cartões verticais; `>= 600dp` -> `DataTable` estilizado + rodapé regulatório opcional).
- `frontend/lib/src/features/ai_analysis/presentation/pages/kerno_ai_page.dart` -- Criar página placeholder Kerno AI (`/kerno-ai`) usando tokens do Design System.
- `frontend/lib/src/features/expenses/presentation/pages/expenses_page.dart` -- Criar stub modular desacoplado para o Ciclo 2.1 (`/expenses`).
- `frontend/lib/main.dart` (L148–412) -- Adotar `StatefulShellRoute.indexedStack` mantendo `BiometricLockGate`, adicionar `/gps-aporte` (com redirect de `/graham-recommendations`), `/kerno-ai` e `/expenses` protegido por `FeatureFlags`, preservando todas as 18 rotas existentes.
- `backend/api/src/main/resources/application.yml` (L40–45), `helm/graham-select/values.yaml` (L25–46), `helm/graham-select/templates/configmap.yaml` (L39–41), `.github/workflows/deploy.yml` (L150–160), `start-app.sh` (L89–98) -- Espelhar `ENABLE_EXPENSES_MODULE` (`false`).

## Tasks & Acceptance

**Execution:**
- [x] `frontend/lib/src/core/config/feature_flags.dart` e `frontend/lib/src/core/widgets/skip_to_content_link.dart` -- Implementar `FeatureFlags` (`enableExpensesModule`) e `SkipToContentLink` com `FocusableActionDetector` -- Centralizar flags modulares e cumprir WCAG 2.4.1 no desktop.
- [x] `frontend/lib/src/features/ai_analysis/presentation/pages/kerno_ai_page.dart` e `frontend/lib/src/features/expenses/presentation/pages/expenses_page.dart` -- Criar stubs modulares de `/kerno-ai` e `/expenses` -- Garantir navegação nas 4 abas primárias e isolamento do 5º módulo.
- [x] `frontend/lib/src/core/widgets/main_layout.dart` -- Refatorar `MainLayout` com breakpoints `< 600dp` / `600–1024dp` / `>= 1024dp`, 4 abas mobile, `LargeContentPresenter` (`textScaler > 1.35`), `NavigationRail`, `Sidebar` com 5 grupos lógicos e `SkipToContentLink` -- Entregar shell multi-superfície acessível.
- [x] `frontend/lib/src/core/widgets/financial_data_table.dart` -- Tornar `FinancialDataTable<T>` responsivo via `LayoutBuilder` (cartões verticais em `< 600dp`, `DataTable` tabular em `>= 600dp`) -- Eliminar scroll horizontal no mobile.
- [x] `frontend/lib/main.dart` -- Migrar `ShellRoute` para `StatefulShellRoute.indexedStack` preservando `BiometricLockGate`, 18 rotas legadas, `/gps-aporte` e guarda da flag `enableExpensesModule` -- Preservar estado/scroll a 60fps e compatibilidade brownfield.
- [x] `backend/api/src/main/resources/application.yml`, `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `.github/workflows/deploy.yml`, `start-app.sh` -- Espelhar `ENABLE_EXPENSES_MODULE=false` -- Cumprir paridade de configuração exigida pelo projeto.
- [x] `frontend/test/core/widgets/` e `frontend/test/` -- Adicionar/atualizar testes unitários e de widget cobrindo todos os cenários da Matriz I/O (breakpoints, 4 abas vs flag ativa, `SkipToContentLink`, `LargeContentPresenter` em `2.0x`, `FinancialDataTable` mobile/desktop) e validar `flutter analyze` / `flutter test` / `helm lint` -- Garantir zero regressões.

**Acceptance Criteria:**
- Given o app em viewport mobile (`< 600dp`) com `ENABLE_EXPENSES_MODULE=false`, when o usuário navega entre as abas primárias, then o estado/scroll é preservado via `StatefulShellRoute.indexedStack` entre exatamente 4 abas (Carteira, GPS de Aporte, Kerno AI, Diagrama).
- Given o app em Desktop Web (`>= 1024dp`), when o usuário pressiona `Tab` pela primeira vez, then `SkipToContentLink` recebe foco e move o foco para o conteúdo principal, com todas as 18 rotas legadas acessíveis nos grupos da `Sidebar`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Origem | Achado | Veredito | Evidência / Rota |
|--------|--------|----------|------------------|
| verification-gap / blind-hunter | `main_shell_navigation_test.dart` testa `_buildShellRouter()` duplicado em vez do roteador real de `main.dart` | `high` (`patch`) | Confirmado em `main_shell_navigation_test.dart:27-101` e `main.dart:376-451`; extrair factory `createKernoRouter` em `main.dart` ou testar `GrahamSelectApp` autenticado no teste. |
| edge-case-hunter / blind-hunter | `FinancialDataTable<T>` ignora `onSelectChanged`/`onLongPress`/`DataCell.onTap` nos cartões mobile (`< 600dp`) | `medium` (`patch`) | Confirmado em `financial_data_table.dart:368-421`; adicionar `InkWell` para interações de linha/célula nos cartões mobile. |
| blind-hunter | `FinancialDataTable<T>` usa `shrinkWrap: true` e `NeverScrollableScrollPhysics()` no mobile | `false` | Intencional para compatibilidade com callers dentro de `SingleChildScrollView`/`Column` (ex.: `UploadPage` e `financial_data_table_responsive_test.dart`). |
| edge-case-hunter | `_buildDesktopTable` usa `CrossAxisAlignment.stretch` mesmo com largura ilimitada | `low` (`patch`) | Confirmado em `financial_data_table.dart:423-451`; usar `constraints.hasBoundedWidth ? CrossAxisAlignment.stretch : CrossAxisAlignment.start`. |
| edge-case-hunter | `SkipToContentLink` oculta o link em `onShowFocusHighlight(false)` mesmo mantendo foco de teclado | `medium` (`patch`) | Confirmado em `skip_to_content_link.dart:51-60`; controlar `_isFocused` apenas via `onFocusChange`. |
| edge-case-hunter | `MainLayout` pode lançar `TypeError` em release se `child` e `navigationShell` forem nulos | `low` (`patch`) | Confirmado em `main_layout.dart:164`; retornar `SizedBox.shrink()` se ambos forem nulos. |
| blind-hunter | `SkipToContentLink` em `Column` causa layout shift no desktop ao focar | `medium` (`patch`) | Confirmado em `main_layout.dart:184-205`; posicionar `SkipToContentLink` em `Stack`/`Positioned` sobre o topo sem deslocar o layout. |
| blind-hunter | `_TopBar` tem avatar sem ação e sino no-op, e fica vazio em `simplifyTopBar` quando `profile == null` | `medium` (`patch`) | Confirmado em `main_layout.dart:396-477`; conectar avatar a `/profile` (mantendo-o visível em `simplifyTopBar`) e sino a `/notifications/web-push`. |
| blind-hunter | `_LargeContentNavIcon` duplica `Tooltip` e `Semantics` já fornecidos por `BottomNavigationBarItem` | `low` (`patch`) | Confirmado em `main_layout.dart:340-380`; remover `Tooltip` e `Semantics` duplicados mantendo `ConstrainedBox(48x48)`. |
| blind-hunter | `_TabletNavigationRail` sem `Tooltip` em `selectedIcon` e sem scroll em viewports baixos | `low` (`patch`) | Confirmado em `main_layout.dart:240-260`; adicionar `Tooltip` em `selectedIcon` e envolver `NavigationRail` em `SingleChildScrollView` + `IntrinsicHeight`. |
| blind-hunter | `_DesktopSidebar` usa `Container` sobre `InkWell` (oculta splash) e omite `Semantics(selected, button)` | `low` (`patch`) | Confirmado em `main_layout.dart:290-335`; usar `Ink` e `Semantics(selected: selected, button: true)`. |
| blind-hunter | Guarda `/expenses` em `main.dart` usa `state.matchedLocation` em vez de `state.uri.path` | `medium` (`patch`) | Confirmado em `main.dart:371`; checar `state.uri.path.startsWith('/expenses')` para cobrir sub-rotas. |
| blind-hunter | `sprint-status.yaml` em `in-progress` e `app.features.enable-expenses-module` sem consumer backend | `false` | `sprint-status.yaml` é atualizado para `done` na etapa final (`step-05`), e a propriedade no `application.yml` é exigida pela regra de paridade em `AGENTS.md`. |

## Verification

**Commands:**
- `cd frontend && flutter analyze` -- expected: `No issues found!`
- `cd frontend && flutter test` -- expected: 100% dos testes passando sem regressões
- `helm lint helm/graham-select && helm template test-graham helm/graham-select --debug` -- expected: validação sem erros
