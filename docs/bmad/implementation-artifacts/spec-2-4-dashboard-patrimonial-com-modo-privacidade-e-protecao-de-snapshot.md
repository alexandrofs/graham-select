---
title: 'Story 2.4: Dashboard Patrimonial com Modo Privacidade e Proteção de Snapshot em Background'
type: 'feature'
created: '2026-11-27'
status: 'done'
baseline_commit: '67bc7bb1680da9cab754abae1ed62f20b658750b'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O `DashboardPage` atual exibe valores monetários em `R$` sem opção de ocultação em ambientes públicos, não apresenta o `WealthHeroCard` com aviso de atraso B3/D-1 nem o `MacroClassDistributionCard` das 5 Macro Classes sem scroll horizontal no mobile, e o `BiometricLockGate` não mascara a tela em `AppLifecycleState.inactive` nem ativa `FLAG_SECURE`/snapshot mask no *App Switcher* de Android/iOS.

**Approach:** Introduzir `PrivacyProvider` persistido localmente e `PrivacyToggleButton` no `_TopBar` global para mascarar instantaneamente valores em `R$` (`R$ ••••••`) mantendo percentuais e gráficos visíveis, evoluir o `DashboardPage` com `WealthHeroCard` e `MacroClassDistributionCard` usando tokens do Design System Intergeracional, e adicionar proteção de snapshot em background via overlay Flutter em `BiometricLockGate` (`inactive`/`paused`/`hidden`) integrado a `FLAG_SECURE` (Android) e máscara nativa (iOS).

## Boundaries & Constraints

**Always:**
- Usar tokens do Design System Intergeracional (`KernoThemeData.of(context)`, `KernoColors`, `KernoSpacing`, `KernoRadius`, `KernoTypography` com `FontFeature.tabularFigures()`), garantindo contraste WCAG AA/AAA, alvos de toque `>= 48x48dp` e zero `TextOverflow.ellipsis` em saldos, tickers ou percentuais.
- No **Modo Privacidade**, substituir todos os valores monetários em `R$` por `R$ ••••••` sem *layout shift*, manter percentuais (`%`) e gráficos visíveis, bloquear leitura de valores em `R$` via `Semantics` e persistir a preferência localmente em `FlutterSecureStorage`.
- Agrupar posições no `MacroClassDistributionCard` nas **5 Macro Classes** oficiais (*Ações BR*, *FIIs*, *Renda Fixa / Tesouro*, *Internacional*, *Criptoativos*) com chips expansíveis de Subclasses e barra dupla (`% Atual` + marcador opcional `% Meta`), sem rolagem horizontal no mobile.
- Ao transicionar para `AppLifecycleState.inactive`, `AppLifecycleState.paused` ou `AppLifecycleState.hidden` em dispositivos móveis autenticados, exibir imediatamente overlay opaco de proteção de snapshot no `BiometricLockGate` e acionar `FLAG_SECURE` no Android e camada de proteção no iOS.
- Preservar `BiometricLockGate` (regra de `> 2 minutos` via `BiometricAuthService`), `EncryptedPortfolioCache`, guardas de tag/offline de `PortfolioProvider` e compatibilidade com `GrahamSelectApp` e testes existentes.

**Never:**
- Alterar endpoints REST do backend (`/api/v1/portfolios/*`), formato/chave de `EncryptedPortfolioCache` ou cálculos numéricos de `PortfolioProvider`/`PortfolioKpiCard`.
- Adicionar dependências nativas de terceiros em `pubspec.yaml` que exijam CocoaPods/Gradle externos para `FLAG_SECURE` ou snapshot mask (usar `MethodChannel` nativo + `WidgetsBindingObserver`).
- Usar linguagem imperativa de recomendação de compra/venda (CVM 19/20) ou implementar persistência/API de metas do Diagrama do Cerrado (Épico 3).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Toggle Modo Privacidade | Toque em `PrivacyToggleButton` | `isPrivacyMode` alterna em `< 300ms`, todos os valores em `R$` viram `R$ ••••••` mantendo `%` e gráficos, `Semantics` oculta `R$` e estado é salvo | Falha de leitura/escrita no storage mantém estado em memória sem lançar exceção |
| Persistência Privacidade | Reinício do app com `kerno_privacy_mode='true'` | `PrivacyProvider` carrega `isPrivacyMode=true` na inicialização antes de exibir saldos | Storage vazio/corrompido assume `false` |
| WealthHeroCard + Delay B3 | `PortfolioSummary` carregado + `custodyMetaPriceUpdatedAt` / `hasCachePrice` | Exibe patrimônio consolidado (`32px`), rentabilidade e aviso discreto de delay 15 min B3 ou fechamento D-1 / cache | Summary nulo ou erro exibe skeleton/alerta existente sem quebrar layout |
| MacroClassDistributionCard | `rawCustodyPositions` com múltiplas classes | Agrupa nas 5 Macro Classes com barras horizontais sem scroll horizontal e chips expansíveis de Subclasses | Lista vazia oculta card (`SizedBox.shrink()`) |
| Proteção de Snapshot Mobile | `AppLifecycleState.inactive` / `paused` / `hidden` (Android/iOS autenticado) | Overlay opaco cobre a árvore imediatamente no `BiometricLockGate` e canal nativo ativa `FLAG_SECURE` / snapshot mask | Em Web ou `MissingPluginException` em testes, falha silenciosamente sem crash |

</frozen-after-approval>

## Code Map

- `frontend/lib/src/core/security/privacy_provider.dart` -- Criar `PrivacyProvider` (`ChangeNotifier`) com `isPrivacyMode`, `toggle()`, `load()`, `formatCurrencyOrMask(...)` e persistência em `FlutterSecureStorage` (`kerno_privacy_mode`).
- `frontend/lib/src/core/security/snapshot_protection_service.dart` -- Criar `SnapshotProtectionService` encapsulando `MethodChannel('kerno/snapshot_protection')` (`enableProtection`, `disableProtection`) com tratamento seguro de `MissingPluginException` / Web.
- `frontend/lib/src/core/security/biometric_lock_gate.dart` (L11–253) -- Observar também `AppLifecycleState.inactive`, exibir overlay de privacidade (`snapshot_privacy_overlay`) enquanto `inactive`/`paused`/`hidden` e acionar `SnapshotProtectionService` quando autenticado em mobile.
- `frontend/android/app/src/main/kotlin/com/example/frontend/MainActivity.kt` (L1–5) -- Registrar `MethodChannel('kerno/snapshot_protection')` aplicando/limpando `WindowManager.LayoutParams.FLAG_SECURE`.
- `frontend/ios/Runner/AppDelegate.swift` (L1–16) e `frontend/ios/Runner/SceneDelegate.swift` (L1–6) -- Adicionar cobertura de snapshot no `applicationWillResignActive`/`sceneWillResignActive` e remoção no `DidBecomeActive`.
- `frontend/lib/src/core/widgets/main_layout.dart` (L749–830) -- Adicionar `PrivacyToggleButton` (`Key('privacy_toggle_button')`, `48x48dp`) no `_TopBar`, visível inclusive quando `simplifyTopBar` estiver ativo.
- `frontend/lib/src/features/portfolio/presentation/widgets/wealth_hero_card.dart` -- Criar `WealthHeroCard` (`rounded.xl`, saldo AAA, rentabilidade, badge comercial opcional, aviso de delay 15 min B3 / fechamento D-1 e suporte a `PrivacyProvider`).
- `frontend/lib/src/features/portfolio/presentation/widgets/macro_class_distribution_card.dart` -- Criar `MacroClassDistributionCard` (`rounded.lg`) mapeando `assetClass` para as 5 Macro Classes com barras duplas (`% Atual` vs `% Meta` opcional), chips expansíveis de Subclasses e suporte a `PrivacyProvider`.
- `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart` (L81–299) -- Integrar `WealthHeroCard` e `MacroClassDistributionCard` no `DashboardPage`, preservando `TagLensSyncMixin`, `PortfolioKpiCard`, `MotivationalClockCard`, `AssetAllocationDonutChart` e `FinancialDataTable`.
- `frontend/lib/src/features/portfolio/presentation/widgets/portfolio_kpi_card.dart` (L4–81), `asset_allocation_donut_chart.dart` (L53–100) e `financial_data_table.dart` (L66–220) -- Aplicar máscara `R$ ••••••` e `Semantics` quando `PrivacyProvider.isPrivacyMode` for `true` (tolerando ausência do provider em testes isolados).
- `frontend/lib/main.dart` (L153–440) -- Registrar `PrivacyProvider` no `MultiProvider` raiz (permitindo injeção opcional em `GrahamSelectApp` para testes) e inicializar carga da preferência.

## Tasks & Acceptance

**Execution:**
- [x] `frontend/lib/src/core/security/privacy_provider.dart` e `frontend/lib/src/core/security/snapshot_protection_service.dart` -- Implementar `PrivacyProvider` persistido em `FlutterSecureStorage` e `SnapshotProtectionService` via `MethodChannel` -- Centralizar estado de privacidade e canal nativo de proteção.
- [x] `frontend/android/app/src/main/kotlin/com/example/frontend/MainActivity.kt`, `frontend/ios/Runner/AppDelegate.swift` e `frontend/ios/Runner/SceneDelegate.swift` -- Implementar `FLAG_SECURE` no Android e snapshot mask no iOS -- Impedir captura de miniatura no *App Switcher*.
- [x] `frontend/lib/src/core/security/biometric_lock_gate.dart` -- Adicionar overlay imediato em `AppLifecycleState.inactive`/`paused`/`hidden` e integração com `SnapshotProtectionService` mantendo regra de `2 minutos` -- Proteger visualmente antes da captura do SO.
- [x] `frontend/lib/src/core/widgets/main_layout.dart` e `frontend/lib/main.dart` -- Expor `PrivacyToggleButton` (`48x48dp`) no `_TopBar` e prover `PrivacyProvider` no `MultiProvider` raiz -- Permitir alternância instantânea em todas as rotas autenticadas.
- [x] `frontend/lib/src/features/portfolio/presentation/widgets/wealth_hero_card.dart` e `macro_class_distribution_card.dart` -- Criar `WealthHeroCard` (com aviso de delay B3/D-1) e `MacroClassDistributionCard` (5 Macro Classes + Subclasses expansíveis) -- Entregar hierarquia visual do Dashboard Patrimonial.
- [x] `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`, `portfolio_kpi_card.dart`, `asset_allocation_donut_chart.dart` e `financial_data_table.dart` -- Integrar novos cards e mascaramento `R$ ••••••` com `Semantics` em todos os valores monetários -- Garantir consistência visual e acessibilidade.
- [x] `frontend/test/` -- Criar/atualizar testes unitários e de widget para `PrivacyProvider`, `BiometricLockGate` (`inactive`/snapshot mask), `PrivacyToggleButton`, `WealthHeroCard`, `MacroClassDistributionCard` e `DashboardPage`, e executar `flutter analyze` e `flutter test` -- Cobrir todos os cenários da Matriz I/O sem regressões.

**Acceptance Criteria:**
- Given o `DashboardPage` autenticado, when renderizado, then exibe `WealthHeroCard` com patrimônio consolidado, rentabilidade e aviso de delay 15 min B3/D-1, além de `MacroClassDistributionCard` para as 5 Macro Classes sem scroll horizontal no mobile.
- Given o usuário toca em `PrivacyToggleButton`, when o Modo Privacidade é ativado, then todos os valores em `R$` passam a `R$ ••••••` instantaneamente (sem *layout shift*, com `Semantics` ocultando valores em `R$`), percentuais/gráficos permanecem visíveis e o estado persiste localmente.
- Given o app móvel autenticado transiciona para `AppLifecycleState.inactive` ou `paused`, when o SO prepara o snapshot do *App Switcher*, then o overlay opaco de proteção e a flag nativa (`FLAG_SECURE` / snapshot mask) são ativados imediatamente.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Origem | Achado | Veredito | Evidência / Rota |
|--------|--------|----------|------------------|
| blind-hunter | Flash inicial de privacidade no `main.dart` (`unawaited(load())`) | `low` (`defer`) | `main.dart:257-259`; `BiometricLockGate` bloqueia mobile no init e `DashboardPage` exibe skeleton durante carga; otimização de preload no `main()` adiada. |
| blind-hunter / edge-case-hunter | Race condition em `PrivacyProvider.load()` após `toggle()`/`setPrivacyMode()` ou `dispose()` | `high` (`patch`) | Confirmado em `privacy_provider.dart:552-591`; adicionar controle de `_epoch` e `_disposed` em `PrivacyProvider`. |
| blind-hunter / verification-gap | `MotivationalClockCard` mascara `R$` visualmente sem `Semantics(excludeSemantics: isPrivacyMode)` | `medium` (`patch`) | Confirmado em `motivational_clock_card.dart:107-120, 390-392, 474`; envolver textos mascarados em `Semantics`. |
| blind-hunter | `PrivacyToggleButton` global não mascara `R$` em rotas fora do Dashboard (`goals`, `tax`) | `medium` (`defer`) | `goal_summary_card.dart`, `what_if_simulator_card.dart`, `tax_declaration_page.dart` estão fora do escopo do Dashboard/Portfólio da Story 2.4; registrado em `deferred-work.md`. |
| blind-hunter | `Wrap(alignment: WrapAlignment.spaceBetween)` no `WealthHeroCard` sem `SizedBox(width: double.infinity)` | `low` (`patch`) | Confirmado em `wealth_hero_card.dart:1568-1583`; envolver `Wrap` em `SizedBox(width: double.infinity)`. |
| blind-hunter / edge-case-hunter | `WealthHeroCard` formata `totalEquity` e `grossYieldPercentage` sem `.isFinite` | `medium` (`patch`) | Confirmado em `wealth_hero_card.dart:1542-1551`; guardar com `.isFinite` antes de formatar/comparar. |
| blind-hunter | `MacroAssetClass.fromRawAssetClass` classifica `'ETF Renda Fixa'`/`'ETF Ações BR'` como `Internacional` | `medium` (`patch`) | Confirmado em `macro_class_distribution_card.dart:1007-1044`; avaliar `'renda fixa'`/`'tesouro'`/`'ações'`/`'fii'` antes de `'etf'`. |
| blind-hunter | `BiometricLockGate.didUpdateWidget` chama `_syncNativeSnapshotProtection()` em todo rebuild | `low` (`patch`) | Confirmado em `biometric_lock_gate.dart:74-76`; chamar apenas quando `isAuthenticated` ou `isWebOverride` mudar. |
| blind-hunter | `AppDelegate.swift` usa `application.windows` e overlay sem branding | `low` (`defer`) | `SceneDelegate.swift` já cobre `UIWindowScene` no iOS 15+; validar visualmente em build iOS. |
| blind-hunter | Tarefas da spec não marcadas como `[x]` no diff inicial | `false` | Tarefas já marcadas `[x]` após verificação do diff na etapa 3 antes da revisão. |
| edge-case-hunter | `MacroClassDistributionCard` não valida `.isFinite` em `targetPercentages` | `medium` (`patch`) | Confirmado em `macro_class_distribution_card.dart:1149, 1242-1257`; ignorar `targetPct` não finito. |
| edge-case-hunter | `_DualAllocationBar` lança `ArgumentError` em `clamp(0.0, width - 3.0)` se `width < 3.0` | `medium` (`patch`) | Confirmado em `macro_class_distribution_card.dart:1342-1344`; limitar `maxLeft = (width - 3.0).clamp(0.0, width)`. |
| verification-gap | `FinancialDataTable` com `PrivacyProvider` sem cobertura de teste | `high` (`patch`) | Confirmado em `financial_data_table_test.dart`; adicionar teste com `PrivacyProvider(initialPrivacyMode: true)`. |
| verification-gap | `AssetAllocationDonutChart` com `PrivacyProvider` sem cobertura de teste | `high` (`patch`) | Confirmado; adicionar teste de widget com posições e `PrivacyProvider(initialPrivacyMode: true)`. |
| verification-gap | `MotivationalClockCard` com `PrivacyProvider` sem cobertura de teste | `medium` (`patch`) | Confirmado em `motivational_clock_card_test.dart`; adicionar teste com `PrivacyProvider(initialPrivacyMode: true)`. |
| verification-gap | `DashboardPage` com `MacroClassDistributionCard` (`rawCustodyPositions` não vazio) sem teste | `medium` (`patch`) | Confirmado em `dashboard_page_test.dart`; adicionar teste verificando `MacroClassDistributionCard`. |
| verification-gap | `BiometricLockGate` sem teste de `disableProtection` no sign-out (`isAuthenticated: false`) e `dispose()` | `medium` (`patch`) | Confirmado em `biometric_lock_gate_test.dart`; adicionar asserções de `disableProtection`. |
| verification-gap | `GrahamSelectApp` sem teste integrado do `PrivacyProvider` na raiz | `medium` (`defer`) | Requer harness completo autenticado; registrado em `deferred-work.md` para E2E. |

## Design Notes

- **Compatibilidade com testes isolados:** Widgets que leem `PrivacyProvider` (`PortfolioKpiCard`, `AssetAllocationDonutChart`, `FinancialDataTable`, `_TopBar`) devem usar `Provider.of<PrivacyProvider?>(context)` (nullable) com fallback `isPrivacyMode = false` para não quebrar testes de widget existentes que montam esses componentes sem o provider raiz.
- **Mapeamento de Macro Classes no `MacroClassDistributionCard`:** Normalizar strings legadas de `CustodyPosition.assetClass` (`Ações`/`Ação` -> `Ações BR`, `FIIs`/`FII` -> `FIIs`, `Renda Fixa`/`Tesouro` -> `Renda Fixa / Tesouro`, `BDRs`/`Internacional`/`ETF` -> `Internacional`, `Cripto`/`Criptoativos` -> `Criptoativos`, demais -> `Outros` ou subclasse correspondente).

## Verification

**Commands:**
- `cd frontend && flutter analyze` -- expected: `No issues found!`
- `cd frontend && flutter test` -- expected: 100% dos testes passando sem regressões
