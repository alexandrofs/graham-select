---
title: 'Story 3.4: Interface do Diagrama do Cerrado: Seletor de Templates e Trava de Metas'
type: 'feature'
created: '2026-11-28'
status: 'done'
baseline_commit: 'e15f4b0d6763516cfdb6ad4514d127c43ea36194'
route: 'dispatch'
review_loop_iteration: 0
context:
  - 'c:\Users\afssi\projects\graham-select\docs\bmad\implementation-artifacts\epic-3-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O backend já expõe os templates oficiais e o endpoint de preferências do Diagrama do Cerrado (`Story 3.3`), mas a Aba 4 (`Diagrama`, rota `/allocation-strategy`) ainda exibe a tela legada de alocação de 4 classes sem seletor de templates, sem as 5 Macro Classes do Cerrado e sem a trava visual de soma exata em `100.00%`.

**Approach:** Criar o módulo feature-first `frontend/lib/src/features/cerrado_diagram/` (`data`, `domain`, `presentation`) integrado a `GET /api/v1/cerrado/templates`, `GET /api/v1/cerrado/preferences` e `PUT /api/v1/cerrado/preferences` via `ApiClient`, expondo o `CerradoTemplateSelectorCard` (3 perfis sem pré-seleção automática + disclaimer CVM), a trava visual de soma `100.00%` com aritmética exata em basis points (escala 2) e conectando a rota `/allocation-strategy` em `main.dart` à nova `CerradoDiagramPage`.

## Boundaries & Constraints

**Always:**
- Carregar os 3 templates oficiais, `defaultNeutralGrade` (`6`) e `cvmDisclaimer` dinamicamente de `GET /api/v1/cerrado/templates`, sem pré-selecionar nenhum template automaticamente no onboarding (`GET /api/v1/cerrado/preferences` retornando `404`).
- Exibir exatamente as 5 Macro Classes: *Ações BR* (`targetAcoesBr`), *FIIs* (`targetFiis`), *Renda Fixa / Tesouro* (`targetRendaFixa`), *Internacional* (`targetInternacional`) e *Criptoativos* (`targetCripto`), todas em `[0.00, 100.00]` com até 2 casas decimais.
- Calcular a soma e a diferença (`Faltam X,XX%` / `Excesso de X,XX%`) em inteiros de basis points (`10000 bps == 100.00%`), normalizando `,` para `.`, tratando campo vazio `""` como `0 bps` e rejeitando entradas não numéricas, negativas, `> 100.00` ou com `> 2` casas decimais; desabilitar o botão Salvar enquanto `totalBps != 10000`, houver campo inválido ou requisição em andamento.
- Serializar os 5 `target*` em `PUT /api/v1/cerrado/preferences` diretamente a partir de basis points com 2 casas decimais (`double.parse((bps / 100).toStringAsFixed(2))`) e aceitar `num`/`String` no `fromJson`, evitando rejeição `422` por escala `> 2` no `BigDecimal` do backend.
- Preservar `grahamMultiplier` e `grahamFilterEnabled` existentes ao salvar (`PUT /api/v1/cerrado/preferences`), ou omiti-los quando não houver preferências prévias (deixando o backend aplicar `1.30` / `true`).
- Expor `reset()` em `CerradoDiagramProvider` e conectá-lo a `authStateChanges` e `onSignOut` em `frontend/lib/main.dart`, evitando vazamento de estado entre sessões de usuários.
- Usar tokens `KernoThemeData.of(context)` / `KernoColors` / `KernoSpacing` / `KernoRadius` / `KernoTypography` (nunca `Colors.green`/`red`/`amber` hardcoded), `FontFeature.tabularFigures()` (`KernoMetricText`) em todos os percentuais, alvo de toque mínimo `48dp`, `TriadStatusBadge` (`TriadStatusTone.emerald` / `TriadStatusTone.crimson`) nos estados da trava, e reflow vertical para `textScaler` até `2.0x` sem `TextOverflow.ellipsis` em cifras.
- Preservar `allocationStrategyBuilder` em `createAppRouter` (`main.dart`) e o item `'Diagrama'` (`branchIndex: 3`, `/allocation-strategy`) em `MainLayout`.

**Never:**
- Alterar arquivos de backend, migrations Liquibase ou a assinatura pública de `createAppRouter`.
- Implementar edição de notas de ativos (`1–10`), slider `48dp` de qualidade ou assistente de 5 critérios (escopo da Story 3.5).
- Pré-selecionar um template ou emitir recomendação de investimento (conformidade CVM 19/2021 e 20/2021).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Onboarding inicial (`404`) | `GET /templates` -> `200`; `GET /preferences` -> `404` | Exibe 3 cards de template sem seleção, campos em `0,00%`, `TriadStatusBadge` carmesim (`Faltam 100,00%`) e `cvmDisclaimer` | `404` em `/preferences` tratado como estado inicial válido (sem erro bloqueante) |
| Seleção de template (1 clique) | Clique em card (ex.: `EQUILIBRADO_CERRADO`) | Destaca card (`brandPrimary` 2px + `brandAccentSoft`), preenche as 5 Macro Classes (`100,00%`), exibe badge *"Notas iniciais neutras = 6 (Editáveis)"* e habilita Salvar | Edição manual posterior limpa o destaque do template selecionado (`selectedTemplateCode = null`) |
| Soma != `100.00%` ou entrada inválida | Edição manual resultando em `98.00%`, `102.50%`, negativo, `> 100` ou `> 2` casas | `TriadStatusBadge` carmesim com diferença exata (`Faltam 2,00%` / `Excesso de 2,50%`) ou alerta de valor inválido; botão Salvar desabilitado | Bloqueio preventivo na UI (evita `422`) |
| Salvamento válido (`PUT`) | Soma `== 100.00%`, clique em Salvar | `PUT /api/v1/cerrado/preferences` retorna `200`, atualiza estado salvo, exibe feedback esmeralda de sucesso | Se API retornar `422` ou erro de rede, exibe mensagem do `ProblemDetail` (`detail`) mantendo valores editados |
| Preferências já salvas (`200`) | Usuário retorna à Aba 4 com preferências no backend | Preenche as 5 Macro Classes com os valores salvos, trava em estado esmeralda `100,00%` | Erro em `GET /templates` ou erro `5xx` em `GET /preferences` exibe estado de erro com botão *Tentar novamente* (`>= 48dp`) |

</frozen-after-approval>

## Code Map

- `frontend/lib/main.dart` -- Registrar `CerradoDiagramProvider` no `MultiProvider` (usando `effectiveApiClient`) e substituir `AllocationStrategyPage` por `CerradoDiagramPage` na rota `/allocation-strategy` (preservando `allocationStrategyBuilder`).
- `frontend/lib/src/core/api/api_client.dart` -- Cliente `Dio` autenticado; reutilizar `.get('/api/v1/cerrado/templates')`, `.get('/api/v1/cerrado/preferences')` e `.put('/api/v1/cerrado/preferences', data: ...)`.
- `frontend/lib/src/core/theme/kerno_tokens.dart` -- Tokens `KernoThemeData.of(context)`, `KernoColors`, `KernoSpacing`, `KernoRadius`, `KernoTypography` (tabular figures).
- `frontend/lib/src/core/theme/kerno_accessibility.dart` -- Reutilizar `KernoMetricText`, `ResponsiveMetricRow` e `TriadStatusBadge` para métricas e status da trava.
- `frontend/lib/src/features/allocation/presentation/pages/allocation_strategy_page.dart` -- Página legada (não alterar; manter testes legados verdes).
- `frontend/test/main_shell_navigation_test.dart` -- Teste de shell/rotas que usa `allocationStrategyBuilder`; garantir que continue passando.

## Tasks & Acceptance

**Execution:**
- [x] `frontend/lib/src/features/cerrado_diagram/domain/` -- Criar entidades (`CerradoTemplate`, `CerradoTemplatesCatalog`, `CerradoPreferences`, `CerradoMacroClass`), repositório abstrato e utilitário de validação/conversão em basis points (`CerradoTargetValidator`) -- Centraliza regra determinística de soma `100.00%` sem ponto flutuante.
- [x] `frontend/lib/src/features/cerrado_diagram/data/` -- Criar models JSON (`fromJson`/`toJson` alinhados a `CerradoController`), `CerradoDiagramRemoteDataSource` (tratando `404` em `/preferences` como `null` e parseando `ProblemDetail.detail` em `422`/erros) e `CerradoDiagramRepositoryImpl` -- Integra com os 3 endpoints REST da Story 3.3.
- [x] `frontend/lib/src/features/cerrado_diagram/presentation/providers/cerrado_diagram_provider.dart` -- Implementar `CerradoDiagramProvider` (`ChangeNotifier`) com `load()` (controle por `generation` contra respostas obsoletas), `selectTemplate()`, edição das 5 Macro Classes (limpando `selectedTemplateCode` ao editar manualmente), cálculo em bps, `savePreferences()` e `reset()` -- Orquestra estado reativo da tela e isolamento entre sessões.
- [x] `frontend/lib/src/features/cerrado_diagram/presentation/widgets/` e `pages/cerrado_diagram_page.dart` -- Criar `CerradoTemplateSelectorCard`, `CerradoMacroAllocationCard` (trava visual esmeralda/carmesim via `TriadStatusBadge` + botão Salvar `>= 48dp`), `CvmRegulatoryFooter` e `CerradoDiagramPage` (disparando `load()` pós-frame em `initState`), e conectar em `frontend/lib/main.dart` (incluindo `reset()` em `authStateChanges` e `onSignOut`) -- Entrega interface acessível, responsiva e isolada por sessão na Aba 4.
- [x] `frontend/test/features/cerrado_diagram/` -- Criar testes unitários (`cerrado_target_validator_test.dart`, `cerrado_diagram_remote_data_source_test.dart`, `cerrado_diagram_provider_test.dart`) e de widget (`cerrado_diagram_page_test.dart`) cobrindo toda a matriz I/O, `textScaler: 2.0`, `48dp` e temas claro/escuro -- Garante regressão zero e conformidade UX.

**Acceptance Criteria:**
- Given o usuário navegando para a Aba 4 (`Diagrama`, `/allocation-strategy`), when a página carrega em `textScaler` `1.0x` ou `2.0x` (Light/Dark), then nenhum número percentual sofre truncamento (`TextOverflow.ellipsis`), todos os controles interativos respeitam `48dp` e o `cvmDisclaimer` permanece visível.
- Given um salvamento válido concluído na `CerradoDiagramPage`, when o usuário recarrega a tela, then as metas persistidas retornam de `GET /api/v1/cerrado/preferences` com a trava em estado esmeralda `100,00%`.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Layer | Location | Claim | Verdict | Route / Disposition | Evidence |
|---|---|---|---|---|---|
| edge-case-hunter | `cerrado_diagram_provider.dart:1106-1125` | `reset()` durante `savePreferences()` vaza resposta obsoleta para a próxima sessão | `medium` | `patch` | `_loadGeneration` não é verificado após `await repository.savePreferences(...)`. |
| edge-case-hunter | `cerrado_macro_allocation_card.dart:1436-1440` | `TextField` editável durante `isSaving == true` pode exibir banner de sucesso para valor não salvo | `low` | `patch` | `TextField` não define `enabled: !widget.isSaving`. |
| edge-case-hunter | `cerrado_target_validator.dart:690-705` | Inteiro gigante pode sofrer overflow em `whole * 100` | `low` | `patch` | Falta guarda `if (whole < 0 \|\| whole > 100) return null;` antes de multiplicar. |
| edge-case-hunter | `cerrado_target_validator.dart:713-715` | `parseApiValueToBps` lança `UnsupportedError` em `NaN`/`Infinity` | `medium` | `patch` | Falta `value.isFinite` no branch `value is num`. |
| edge-case-hunter | `cerrado_diagram_provider.dart:1059-1124` | `notifyListeners()` após `dispose()` lança `FlutterError` | `low` | `patch` | Provider não rastreia `_disposed` antes de notificar pós-`await`. |
| edge-case-hunter | `frontend/lib/main.dart:82` | Parâmetro `cerradoDiagramProvider` altera assinatura de `createAppRouter` | `false` | Rejected | Parâmetro nomeado opcional preserva todos os callers existentes e é exigido pelo `onSignOut` da spec. |
| blind-hunter | `cerrado_diagram_provider.dart` | `savePreferences()` sem guarda de `_loadGeneration` contra `reset()` | `medium` | `patch` | Mesma causa raiz do item 1 do `edge-case-hunter`. |
| blind-hunter | `cerrado_diagram_provider.dart:1112` | `savePreferences()` não sincroniza `_rawInputs` com `saved.targetsBps` | `low` | `patch` | Entrada `'25'` permanece sem formatação `'25,00'` após salvar. |
| blind-hunter | `cerrado_diagram_provider.dart:1065` | `load(silent: true)` sobrescreve `_status = error` | `low` | `patch` | Adicionar guarda `if (!silent)` ao definir estado de erro. |
| blind-hunter | `cerrado_target_validator.dart:713-724` | `parseApiValueToBps` sem `isFinite` e `fallbackDouble` aceita negativos | `medium` | `patch` | `fallbackDouble` faz clamp de negativos para `0` em vez de lançar `FormatException`. |
| blind-hunter | `cerrado_models.dart` / `cerrado_diagram_page.dart` | `grahamMultiplier.isFinite` ausente em `toUpdatePayload` e card vazio se `templates` vazio | `low` | `patch` | Adicionar guarda `isFinite` e `catalog.templates.isNotEmpty`. |
| blind-hunter | `cerrado_diagram_remote_data_source.dart:245` | `catch (_)` silencioso ao decodificar erro não-JSON | `low` | `patch` | Registrar `debugPrint` no `catch` conforme política de logs. |
| blind-hunter | `cerrado_macro_allocation_card.dart:1446-1460` | `_MacroInputRow` sem `focusedErrorBorder` | `low` | `patch` | Campo focado com erro perde borda carmesim. |
| blind-hunter | `cerrado_macro_allocation_card.dart` / `cerrado_diagram_page.dart` | Falta `Semantics(liveRegion: true)` na trava; `reset()` em `IndexedStack` | `low` | `patch` | `onSignOut` navega para `/login` desmontando a aba (`false` para spinner preso); `liveRegion: true` adicionado na trava. |
| blind-hunter | `frontend/lib/main.dart` | `AllocationProvider` legado mantido no `MultiProvider` | `false` | Rejected | Mantido intencionalmente pelo Code Map para preservar módulo e testes legados. |
| blind-hunter | `cerrado_diagram_provider.dart` | Falta `hasUnsavedChanges` para desabilitar Salvar sem edições | `false` | Rejected | Matriz I/O da spec exige Salvar habilitado sempre que soma == `100,00%`. |
| blind-hunter | `cerrado_preferences.dart` / `cerrado_template.dart` | Ausência de `operator ==` e `hashCode` | `low` | Rejected | Sem caller comparando instâncias por igualdade; evita boilerplate desnecessário. |
| blind-hunter | `frontend/test/features/cerrado_diagram/` | Lacunas de teste (`NaN`/negativos API, race `save`/`reset`, retry, erro inline) | `medium` | `patch` | Casos adicionais cobrem todas as bordas verificadas. |
| verification-gap | `frontend/lib/main.dart:605-609` | Rota padrão `/allocation-strategy` e DI de `CerradoDiagramProvider` sem teste via `createAppRouter` | `medium` | `patch` | Pré-verificado pelo `verification-gap`; adicionar teste em `main_shell_navigation_test.dart`. |
| verification-gap | `cerrado_macro_allocation_card.dart:58-70` | Sync de `TextEditingController`s em `didUpdateWidget` após seleção de template não verificado | `medium` | `patch` | Pré-verificado pelo `verification-gap`; asserção nos 5 `TextField`s em `cerrado_diagram_page_test.dart`. |
| verification-gap | `cerrado_diagram_provider_test.dart:106-150` | Teste `savePreferences...` só testa caminho de sucesso e não testa erro | `medium` | `patch` | Confirmado no diff; adicionar cenário de erro preservando inputs. |

## Design Notes

Contratos JSON (`CerradoController.java`):
- `GET /api/v1/cerrado/templates` -> `{ defaultNeutralGrade: int, cvmDisclaimer: String, templates: [{ code, name, description, targetAcoesBr, targetFiis, targetRendaFixa, targetInternacional, targetCripto }] }`
- `GET /api/v1/cerrado/preferences` / `PUT /api/v1/cerrado/preferences` -> `{ id: int, userId: int, targetAcoesBr, targetFiis, targetRendaFixa, targetInternacional, targetCripto, grahamMultiplier, grahamFilterEnabled, updatedAt: String (ISO-8601) }`

## Verification

**Commands:**
- `flutter analyze` (em `frontend/`) -- expected: zero issues.
- `flutter test` (em `frontend/`) -- expected: 100% dos testes unitários e de widget aprovados, incluindo `test/features/cerrado_diagram/` e `test/main_shell_navigation_test.dart`.
