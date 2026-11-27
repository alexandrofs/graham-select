# Epic 2 Context: Design System Intergeracional, Experiência Mobile & Dashboard Patrimonial

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Entregar a base visual unificada (**Design System Intergeracional**), a experiência móvel segura (iOS/Android) e o **Dashboard Patrimonial** consolidado do **Kerno** em todas as superfícies autenticadas (Mobile e Web). Este épico elimina a poluição visual e as tabelas horizontais legadas, garante legibilidade universal para investidores de todas as faixas etárias (contraste WCAG AA/AAA e escala nativa até `2.0x`), protege os dados financeiros no dia a dia com autenticação biométrica, cache offline criptografado, **Modo Privacidade** (`R$ ••••••`) e máscara no *App Switcher* do sistema operacional, organiza a navegação modular pronta para futuras extensões (como o módulo de Gastos) e adapta os fluxos existentes de ingestão B3, cadastro manual multi-classe e Holding Familiar Multi-CPF sem regressão analítica.

## Stories

- Story 2.1: Fundação do Design System Intergeracional (Tokens, Tipografia e Acessibilidade)
- Story 2.2: Autenticação Mobile Segura, Biometria Nativa e Cache Offline Criptografado
- Story 2.3: Shell de Navegação Multi-Superfície e Fundação Modular com Feature Flags
- Story 2.4: Dashboard Patrimonial com Modo Privacidade e Proteção de Snapshot em Background
- Story 2.5: Adaptação Visual da Ingestão B3, CRUD Manual Multi-Classe e Holding Familiar Multi-CPF

## Requirements & Constraints

- **Acessibilidade visual e escalonamento tipográfico:** Garantir contraste mínimo de `4.5:1` (WCAG AA) em textos corridos e `7.0:1` (WCAG AAA) em saldos patrimoniais e alertas principais, áreas de toque mínimas de `48x48dp` em todos os controles interativos, numerais tabulares obrigatórios (`tabular-nums` / `FontFeature.tabularFigures()`) em valores monetários, quantidades, tickers e percentuais, e suporte nativo a `MediaQuery.textScalerOf(context)` de `1.0x` a `2.0x` sem truncamento (`TextOverflow.ellipsis` proibido em saldos, tickers ou percentuais).
- **Segurança mobile e modo offline:** Armazenar `access_token` e `refresh_token` exclusivamente em `flutter_secure_storage` (iOS Keychain / Android Keystore EncryptedSharedPreferences). Exigir desbloqueio biométrico nativo (`local_auth`) na abertura do app e ao retornar do background após mais de `2 minutos` de inatividade, com fallback para PIN/credencial do SO. Em rede instável ou offline, renderizar após biometria aprovada o último snapshot válido do Dashboard a partir de cache local criptografado, exibindo o timestamp da última sincronização. Limpar integralmente tokens e caches locais no logout ou exclusão de conta.
- **Privacidade visual instantânea e proteção de background:** O **Modo Privacidade** substitui valores em `R$` por `R$ ••••••` em todas as telas em `< 300ms` (`60fps`) sem *layout shift*, mantendo percentuais e gráficos visíveis e persistindo a preferência localmente. Ao transicionar para `AppLifecycleState.inactive` ou `AppLifecycleState.paused`, ativar imediatamente `FLAG_SECURE` no Android e overlay de máscara no iOS para impedir captura de miniatura no *App Switcher*.
- **Latência e fluidez:** Carregamento do Dashboard Patrimonial autenticado em `< 1.5s` (P95) no Mobile nativo e `< 2.5s` em *cold start* no Web; transições de tela e toggle de privacidade a `60fps`.
- **Preservação brownfield e LGPD Multi-CPF:** Manter 100% dos testes de regressão dos módulos de cálculo de posição e ingestão assíncrona B3 via Kafka (com descarte imediato do arquivo bruto com PII após parsing). O cadastro manual deve cobrir as 5 Macro Classes (*Ações BR*, *FIIs*, *Renda Fixa / Tesouro*, *Internacional*, *Criptoativos*) em `BRL`. O vínculo de CPF secundário ou de dependente menor exige aceite explícito de consentimento/representação legal (LGPD Art. 14).
- **Engenharia e paridade:** Manter zero erros/warnings em `flutter analyze`; refletir novas variáveis ou *feature flags* em `application.yml`, Helm (`values.yaml`, `configmap.yaml`, `secret.yaml`) e `.github/workflows/deploy.yml`; usar UTC ISO-8601 em datas/timestamps e propagar `X-Correlation-ID`.

## Technical Decisions

- **Arquitetura cliente Feature-First (Flutter ^3.11.0 / Dart 3.7+):** Organizar `frontend/lib/` com `core/theme` (tokens do Design System), `core/network` (cliente HTTP `Dio` com interceptor `X-Correlation-ID`), `core/security` (`local_auth` e `flutter_secure_storage`) e módulos isolados em `features/` (`portfolio`, `cerrado_diagram`, `contribution_gps`, `ai_analysis`, `founders`, `expenses`). Usar `provider` para estado e `go_router` (`StatefulShellRoute` / `IndexedStack`) para navegação a `60fps`.
- **Navegação modular com Feature Flags:** No Mobile (`< 600dp`), usar `BottomNavigationBar` com exatamente 4 abas primárias (**Carteira**, **GPS de Aporte**, **Kerno AI**, **Diagrama**), reservando o 5º slot (**Gastos**, `features/expenses`) oculto sob `enable_expenses_module = false`. No Tablet (`600dp–1024dp`), usar `NavigationRail` (`72dp`). No Desktop Web (`>= 1024dp`), usar `Sidebar` (`240dp–260dp`) com agrupamentos lógicos das 18 rotas existentes reestilizadas e `SkipToContentLink` (WCAG 2.4.1) no primeiro foco de `Tab`.
- **Eliminação de tabelas horizontais no Mobile:** O componente `FinancialDataTable` deve comutar automaticamente via `LayoutBuilder` para uma `ListView` de cartões verticais empilhados em viewports `< 600dp`, eliminando `SingleChildScrollView(scrollDirection: Axis.horizontal)`.
- **Precisão numérica e datas:** Cálculos no backend em `BigDecimal` (`RoundingMode.HALF_EVEN`); formatação no Dart via `intl` (`NumberFormat.currency(locale: 'pt_BR', symbol: 'R$')`). Exibir aviso discreto de cotações B3 com atraso de `15 min` em pregão aberto ou fechamento `D-1` fora do pregão.
- **Compatibilidade comercial:** Em contas `FREE` pós-trial com múltiplos CPFs cadastrados, manter o CPF titular ativo e exibir badge *"Pausado no Free — Upgrade Pro"* nos CPFs secundários sem apagar dados.

## UX & Interaction Patterns

- **Paleta Light-First com Dark Mode nativo:** Canvas `#F8FAFC` (Dark `#0B0F19`), superfície elevada `#FFFFFF` (Dark `#111827`), superfície sutil `#F1F5F9` (Dark `#1E293B`), tinta primária `#0F172A` (`17.4:1` AAA), marca primária Kerno Indigo `#4F46E5` (Dark `#818CF8`), e bordas estruturais `1px solid #E2E8F0` (Dark `#1E293B`) e `>= 3:1` em inputs/botões (`#64748B`). Aplicar a **Regra da Tríade** (cor + ícone + rótulo textual explícito) em todos os estados semânticos (Esmeralda `#065F46`, Âmbar `#92400E`, Carmesim `#991B1B`, Ouro Founder `#92400E`/`#FEF3C7`).
- **Tipografia e elasticidade (`1.0x`–`2.0x`):** `Plus Jakarta Sans` para títulos/display e `Inter` com `tabular-nums` para métricas e corpo. Em `textScaler >= 1.3x`, linhas horizontais (`Row`) de métricas convertem-se automaticamente para empilhamento vertical (`Column`/`Wrap`). Acima de `1.35x` em telas compactas (`360dp`), a barra inferior adota `LargeContentPresenter` ao toque longo e o cabeçalho global preserva apenas Logo, seletor Multi-CPF e botão de Privacidade, movendo itens secundários para o menu de perfil.
- **Componentes e superfícies principais:**
  - `wealth-hero-card` (`rounded.xl` `24px`): saldo consolidado (`32px`, `17.4:1` AAA), rentabilidade, status comercial, delay B3/D-1 e suporte instantâneo a `R$ ••••••`.
  - `macro-class-distribution-card` (`rounded.lg` `16px`): barras horizontais duplas (`% Atual` vs. marcador `% Meta` com halo branco `1.5px`) para as 5 Macro Classes e chips expansíveis de Subclasses.
  - `b3-ingestion-crud-sheet`: modal de profundidade máxima **1 nível** (Bottom Sheet no Mobile, Drawer `480px` na Web) com abas para upload assíncrono B3 (`.xlsx`/`.csv`) e CRUD manual multi-classe (cotas inteiras para ativos de bolsa/ETFs como `IVVB11`/`HASH11`, saldo contínuo em `BRL` para Renda Fixa e cripto direto como `BTC`/`ETH`).
- **Leitores de tela e linguagem:** No **Modo Privacidade**, `Semantics` bloqueia a leitura do valor em `R$` e anuncia apenas percentuais/alocação. Gráficos `CustomPainter` expõem `semanticsBuilder` e padrões distintos de traço (sólido vs. tracejado/pontilhado). Manter linguagem descritiva e matemática, vedando termos imperativos de compra/venda (CVM 19/20).

## Cross-Story Dependencies

- **Story 2.1 (Design System)** é pré-requisito direto para as Stories 2.3, 2.4 e 2.5, definindo tokens de cor, tipografia tabular, `AppTheme`, regras de `textScaler` e alvos de `48x48dp`.
- **Story 2.2 (Auth Mobile & Biometria)** e **Story 2.3 (Shell de Navegação)** estruturam o fluxo de entrada e o container (`MainLayout`, `GoRouter`, `privacy-toggle`, `multi-cpf-selector`) onde o Dashboard (Story 2.4) e o sheet de ingestão/CRUD (Story 2.5) operam.
- **Dependências externas ao épico:** Consome a identidade **Kerno**, rotas públicas e o selo `badge-founder` / `commercial_plan` entregues no **Epic 1**; fornece a base visual, as 4 abas principais e o contrato de entrada modular para o **Diagrama do Cerrado** (**Epic 3**), **GPS de Aporte** (**Epic 4**) e **Kerno AI** (**Epic 5**). O vínculo Multi-CPF (Story 2.5) deve permanecer compatível com o expurgo em cascata (`UserDataPurgeAdapter` / `AccountDeletionIT`) do **Epic 5**.
