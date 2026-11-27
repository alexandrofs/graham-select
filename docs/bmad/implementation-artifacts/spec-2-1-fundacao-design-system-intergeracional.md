---
title: 'Story 2.1: Fundação do Design System Intergeracional (Tokens, Tipografia e Acessibilidade)'
type: 'feature'
created: '2026-11-25'
status: 'done'
baseline_commit: '7f170831c23988584c961a7586b241e5d076354d'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O `AppTheme` atual em `frontend/lib/src/core/theme/app_theme.dart` expõe apenas um tema escuro legado com `DM Sans`, sem tokens semânticos Light-First/Dark Mode, sem garantia de contraste WCAG AA/AAA, sem `tabular-nums` padronizado, sem alvos de toque mínimos de `48x48dp` em todos os controles e sem suporte estrutural a `textScaler` até `2.0x`.

**Approach:** Criar a fundação do Design System Intergeracional do Kerno (`KernoColors`, `KernoTypography`, `KernoSpacing`, `KernoRadius`, `KernoThemeExtension`), expor `AppTheme.lightTheme` (padrão Light-First) e `AppTheme.darkTheme` com `Plus Jakarta Sans` + `Inter` (`FontFeature.tabularFigures()`), conectar `lightTheme`/`darkTheme`/`ThemeMode.system` em `main.dart` e prover primitivos reutilizáveis de acessibilidade (`KernoMetricText`, `ResponsiveMetricRow`, `TriadStatusBadge`).

## Boundaries & Constraints

**Always:**
- Seguir exatamente os tokens hexadecimais, escalas tipográficas (`Plus Jakarta Sans` para títulos/display; `Inter` com `FontFeature.tabularFigures()` para métricas/corpo), raios e espaçamentos (grid `4dp`/`8dp`) definidos em `DESIGN.md`.
- Garantir contraste mínimo `>= 4.5:1` (WCAG AA) em textos normais, `>= 7.0:1` (WCAG AAA) em saldos patrimoniais e alertas principais, e `>= 3.0:1` (WCAG 1.4.11) em bordas de inputs/botões (`#64748B`).
- Garantir `minimumSize: Size(48, 48)` nos temas de botões/controles interativos e suporte nativo a `MediaQuery.textScalerOf(context)` de `1.0x` a `2.0x` sem `TextOverflow.ellipsis` em saldos, tickers ou percentuais.
- Preservar as constantes estáticas legadas de `AppTheme` (`primaryColor`, `secondaryColor`, `accentColor`, `backgroundColor`, `surfaceColor`, `textColor`) para manter 100% de compatibilidade de compilação com as telas existentes.

**Never:**
- Alterar ou remover rotas, providers de negócio ou o comportamento de `GrahamSelectApp` em `main.dart` além da fiação de `theme`, `darkTheme` e `themeMode`.
- Implementar telas de feature específicas (Dashboard, Shell de Navegação, Sheet B3/CRUD — cobertos pelas Stories 2.3–2.5).
- Usar `TextOverflow.ellipsis` ou alturas fixas em componentes/helpers tipográficos destinados a valores monetários, tickers ou percentuais.
- Introduzir dependências externas novas no `pubspec.yaml` sem necessidade (reutilizar `google_fonts: ^6.2.1` já instalado).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Contraste Light & Dark | Pares texto/superfície primários, secundários, semânticos e `borderStrong` | Texto normal `>= 4.5:1`; saldos/alertas principais `>= 7.0:1`; bordas fortes `>= 3.0:1` | Falha imediata em teste unitário de contraste WCAG |
| Escala extrema de texto | `MediaQuery` com `textScaler` `1.0x`, `1.5x` e `2.0x` | `KernoMetricText` e `ResponsiveMetricRow` expandem verticalmente sem overflow nem `...`; `Row` comuta para `Column` em `>= 1.3x` | Erro de `RenderFlex` overflow falha o widget test |
| Alinhamento tabular | `KernoTypography` (`metricHero`, `metricCard`, `tickerBadge`, `bodyBase`, `bodySm`, `caption`) | Todos incluem `FontFeature.tabularFigures()` em `fontFeatures` | Asserção unitária falha se `tabularFigures` ausente |
| Alvos de toque | `ElevatedButton`, `OutlinedButton`, `TextButton`, `IconButton` sob `AppTheme` | Área renderizada `>= 48x48dp` por padrão | Asserção de `tester.getSize` falha se `< 48dp` |
| Tema padrão e Dark | `AppTheme.lightTheme` e `AppTheme.darkTheme` | `Brightness.light` (`#F8FAFC` / `#FFFFFF`) e `Brightness.dark` (`#0B0F19` / `#111827`) com `KernoThemeExtension` anexada | Fallback seguro via `KernoThemeExtension.of(context)` se extensão ausente |

</frozen-after-approval>

## Code Map

- `frontend/lib/src/core/theme/app_theme.dart` (L1–63) -- Atualizar para expor `lightTheme` e `darkTheme` usando `KernoColors`, `KernoTypography` (`Plus Jakarta Sans` + `Inter`), `KernoSpacing`, `KernoRadius` e `KernoThemeExtension`, preservando as constantes estáticas legadas (L5–10).
- `frontend/lib/src/core/theme/kerno_tokens.dart` -- Criar tokens imutáveis `KernoColors`, `KernoSpacing`, `KernoRadius`, `KernoTypography` (com `FontFeature.tabularFigures()`) e `ThemeExtension<KernoThemeData>` (`KernoThemeData.light` / `KernoThemeData.dark`).
- `frontend/lib/src/core/theme/kerno_accessibility.dart` -- Criar utilitários/widgets reutilizáveis: `KernoMetricText` (sem `ellipsis`, `tabular-nums`), `ResponsiveMetricRow` (reflow horizontal -> vertical em `textScaler >= 1.3x`) e `TriadStatusBadge` (Regra da Tríade: cor + ícone + rótulo textual explícito).
- `frontend/lib/main.dart` (L451–456) -- Atualizar `MaterialApp.router` para usar `theme: AppTheme.lightTheme`, `darkTheme: AppTheme.darkTheme` e `themeMode: ThemeMode.system`.
- `frontend/pubspec.yaml` (L38) -- Já possui `google_fonts: ^6.2.1` (não alterar dependências).
- `frontend/test/core/theme/` -- Criar suíte de testes unitários e de widgets (contraste WCAG, `tabular-nums`, `48x48dp`, `textScaler` `1.0x–2.0x` e `ThemeMode`).

## Tasks & Acceptance

**Execution:**
- [x] `frontend/lib/src/core/theme/kerno_tokens.dart` -- Criar `KernoColors`, `KernoSpacing`, `KernoRadius`, `KernoTypography` (com `Plus Jakarta Sans`, `Inter` e `FontFeature.tabularFigures()`) e `KernoThemeData` (`ThemeExtension`) para Light e Dark -- Centralizar tokens de `DESIGN.md` em fonte única tipada.
- [x] `frontend/lib/src/core/theme/kerno_accessibility.dart` -- Criar `KernoMetricText`, `ResponsiveMetricRow` (reflow em `textScaler >= 1.3x`) e `TriadStatusBadge` (cor + ícone + texto) -- Garantir acessibilidade intergeracional reutilizável nas Stories 2.3–2.5.
- [x] `frontend/lib/src/core/theme/app_theme.dart` -- Implementar `AppTheme.lightTheme` e `AppTheme.darkTheme` com `minimumSize: Size(48, 48)`, `KernoThemeData` e tipografia `Plus Jakarta Sans`/`Inter`, mantendo constantes legadas -- Aplicar Design System sem quebrar telas legadas.
- [x] `frontend/lib/main.dart` -- Configurar `theme: AppTheme.lightTheme`, `darkTheme: AppTheme.darkTheme` e `themeMode: ThemeMode.system` em `MaterialApp.router` (L451–456) -- Ativar Light-First com suporte nativo a Dark Mode.
- [x] `frontend/test/core/theme/kerno_design_system_test.dart` -- Adicionar testes de contraste WCAG (AA/AAA/1.4.11), `tabularFigures`, `48x48dp`, `textScaler` `1.0x`/`1.5x`/`2.0x` sem truncamento e renderização Light/Dark -- Validar todos os cenários da Matriz I/O e ACs.

**Acceptance Criteria:**
- Given os tokens de cores de `DESIGN.md`, when o app é inicializado em Light (padrão) ou Dark, then `AppTheme.lightTheme` e `AppTheme.darkTheme` aplicam as superfícies, cores de marca e semânticas com contraste WCAG AA/AAA e alvos de toque `>= 48x48dp`.
- Given um dispositivo com `MediaQuery.textScalerOf(context)` entre `1.0x` e `2.0x`, when `KernoMetricText` e `ResponsiveMetricRow` renderizam saldos, tickers e percentuais, then não ocorre truncamento (`...`) nem overflow, expandindo verticalmente com reflow natural.
- Given estilos numéricos em `KernoTypography`, when inspecionados ou renderizados, then todos possuem `FontFeature.tabularFigures()` ativo para alinhamento vertical exato.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Origem | Achado | Veredito | Evidência / Rota |
|--------|--------|----------|------------------|
| blind-hunter / edge-case-hunter | `KernoThemeData.dark.textOnPrimary` (`#FFFFFF`) sobre `brandPrimary` (`#818CF8`) gera ~3.06:1 (< 4.5:1) | `medium` (`patch`) | Confirmado em `kerno_tokens.dart:763-764`; ajustar `textOnPrimary: KernoColors.textPrimary` em `KernoThemeData.dark` e usar `tokens.textOnPrimary` em `app_theme.dart`. |
| blind-hunter | `ColorScheme` usa `secondaryColor` legado (`#8B5CF6`) com `onSecondary: #FFFFFF` (~3.4:1 < 4.5:1) | `medium` (`patch`) | Confirmado em `app_theme.dart:55,58,68,71`; substituir `secondary` por `KernoColors.brandPrimaryDeep` (Light) / `KernoColors.brandPrimaryDark` (Dark) para garantir `>= 4.5:1`. |
| blind-hunter | Badges dark reutilizam bordas pastéis de light mode | `false` | `DESIGN.md` não define tokens de borda semântica dark separados e os pastéis sobre superfícies escuras mantêm alto contraste legível. |
| blind-hunter | `TriadStatusBadge` anuncia o rótulo duas vezes em leitores de tela | `medium` (`patch`) | Confirmado em `kerno_accessibility.dart:384-414` (`Semantics` sem `excludeSemantics: true`); adicionar `excludeSemantics: true`. |
| blind-hunter / edge-case-hunter | `KernoMetricText` não bloqueia `TextOverflow.ellipsis` vindo de `style` e duplica `fontFeatures` | `medium` (`patch`) | Confirmado em `kerno_accessibility.dart:257-272`; forçar `overflow: TextOverflow.visible` em `effectiveStyle` e `Text`, deduplicando `fontFeatures`. |
| blind-hunter | `ResponsiveMetricRow` não faz reflow por largura estreita em `textScale < 1.3` | `low` (rejeitado) | `Flexible` com `softWrap: true` acomoda linhas limitadas; o risco real de largura ilimitada é tratado no achado de `edge-case-hunter`. |
| blind-hunter | `KernoThemeData` sem `operator ==` e `hashCode` | `low` (rejeitado) | `ThemeExtension` não exige igualdade por valor e apenas instâncias `const` (`light`/`dark`) são comparadas nos temas atuais. |
| blind-hunter / edge-case-hunter | Asserções de contraste em `kerno_design_system_test.dart` abaixo de 7.0:1 para 3 pares de badge | `false` | Os hexadecimais literais de `DESIGN.md` medem `6.84:1` (`semanticAmber` sobre `semanticAmberSurface`) e `6.38:1` (dark emerald/crimson sobre superfícies tintadas), acima de AA (`4.5:1`), enquanto saldos e alertas primários atingem `>= 7.0:1` AAA. |
| blind-hunter / verification-gap | `FilledButton` (`48x48dp`) omitido no loop de testes de alvo de toque | `medium` (`patch`) | Confirmado em `kerno_design_system_test.dart:1190-1195`; incluir `FilledButton` no teste. |
| blind-hunter | `AppTheme` não configura `appBarTheme`, `dialogTheme`, `bottomSheetTheme`, `chipTheme`, `snackBarTheme` | `low` (rejeitado) | Escopo de superfícies pertence às Stories 2.3–2.5 e o Material 3 já herda `ColorScheme` e `TextTheme` do `AppTheme`. |
| edge-case-hunter | `ResponsiveMetricRow` com `Flexible` falha em pai horizontal sem largura limitada (`unbounded width`) | `medium` (`patch`) | Confirmado em `kerno_accessibility.dart:316-324`; usar `LayoutBuilder` e fazer fallback para `Column` quando `!constraints.hasBoundedWidth`. |
| edge-case-hunter | `TriadStatusBadge` com `Flexible` dentro de `Row` falha em pai horizontal sem largura limitada | `medium` (`patch`) | Confirmado em `kerno_accessibility.dart:396-412`; usar `LayoutBuilder` para aplicar `Flexible` somente quando `constraints.hasBoundedWidth`. |
| verification-gap | Fiação `GrahamSelectApp` (`lightTheme`, `darkTheme`, `ThemeMode.system`) em `main.dart:454-456` sem teste dedicado | `medium` (`patch`) | Adicionar teste de widget verificando `MaterialApp` de `GrahamSelectApp` (ou `home_page_test.dart`) para `lightTheme`, `darkTheme` e `ThemeMode.system`. |
| verification-gap | `KernoThemeData.lerp` sem teste unitário dedicado | `low` (`defer`) | Interpolação campo a campo via `Color.lerp`; registrado em `deferred-work.md`. |

## Design Notes

- **KernoThemeData (`ThemeExtension`):** Em vez de acoplar widgets apenas a `ColorScheme` genérico do Material 3, `KernoThemeData` expõe tokens semânticos específicos (`surfaceBase`, `surfaceElevated`, `surfaceSubtle`, `textPrimary`, `textSecondary`, `textMuted`, `emerald*`, `amber*`, `crimson*`, `founder*`, `borderSubtle`, `borderStrong`) com interpolação suave (`lerp`) e helper `KernoThemeData.of(context)` com fallback seguro para `KernoThemeData.light`.
- **Compatibilidade legada:** `AppTheme.primaryColor`, `secondaryColor`, `accentColor`, `backgroundColor`, `surfaceColor` e `textColor` permanecem inalterados como `@Deprecated` ou constantes preservadas para evitar quebra de compilação em módulos que serão migrados nas Stories 2.3–2.5.

## Verification

**Commands:**
- `cd frontend && flutter analyze` -- expected: `No issues found!` (0 erros e 0 warnings)
- `cd frontend && flutter test test/core/theme/kerno_design_system_test.dart` -- expected: todos os testes de contraste, tipografia, toque `48dp` e `textScaler` passam
- `cd frontend && flutter test` -- expected: suíte completa passa sem regressões
