---
name: kerno-intergenerational-design-system
description: 'Design System Intergeracional do Kerno ("O núcleo inteligente do seu dinheiro") — clareza patrimonial inspirada no Nubank, Light-First respirável com Dark Mode nativo, acessibilidade WCAG 2.1 AA/AAA, tipografia escalável 1.0x–2.0x com numerais tabulares e conformidade visual CVM 19/20.'
status: final
created: 2026-11-21
updated: 2026-11-21
sources:
  - docs/bmad/planning-artifacts/prds/prd-graham-select-2026-11-21/prd.md
  - docs/bmad/planning-artifacts/briefs/brief-graham-select-2026-11-21/brief.md
colors:
  surface-base: '#F8FAFC'
  surface-elevated: '#FFFFFF'
  surface-subtle: '#F1F5F9'
  surface-overlay: '#0F172A'
  surface-base-dark: '#0B0F19'
  surface-elevated-dark: '#111827'
  surface-subtle-dark: '#1E293B'
  text-primary: '#0F172A'
  text-secondary: '#334155'
  text-muted: '#475569'
  text-on-primary: '#FFFFFF'
  text-primary-dark: '#F8FAFC'
  text-secondary-dark: '#CBD5E1'
  text-muted-dark: '#94A3B8'
  brand-primary: '#4F46E5'
  brand-primary-hover: '#4338CA'
  brand-primary-deep: '#3730A3'
  brand-accent-soft: '#EEF2FF'
  brand-primary-dark: '#818CF8'
  brand-accent-soft-dark: '#1E1B4B'
  semantic-emerald: '#065F46'
  semantic-emerald-surface: '#ECFDF5'
  semantic-emerald-border: '#A7F3D0'
  semantic-emerald-dark: '#6EE7B7'
  semantic-emerald-surface-dark: '#064E3B'
  semantic-amber: '#92400E'
  semantic-amber-surface: '#FFFBEB'
  semantic-amber-border: '#FDE68A'
  semantic-amber-dark: '#FBBF24'
  semantic-amber-surface-dark: '#451A03'
  semantic-crimson: '#991B1B'
  semantic-crimson-surface: '#FEF2F2'
  semantic-crimson-border: '#FECACA'
  semantic-crimson-dark: '#FCA5A5'
  semantic-crimson-surface-dark: '#450A0A'
  founder-gold: '#92400E'
  founder-gold-surface: '#FEF3C7'
  founder-gold-border: '#F59E0B'
  founder-gold-dark: '#FDE68A'
  founder-gold-surface-dark: '#78350F'
  border-subtle: '#E2E8F0'
  border-strong: '#94A3B8'
  border-focus: '#4F46E5'
  border-subtle-dark: '#1E293B'
  border-strong-dark: '#64748B'
  border-focus-dark: '#818CF8'
typography:
  display-wealth:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: '36px'
    fontWeight: '700'
    lineHeight: '1.15'
    letterSpacing: '-0.02em'
    fontVariantNumeric: 'tabular-nums'
  heading-1:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: '28px'
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: '-0.015em'
    fontVariantNumeric: 'tabular-nums'
  heading-2:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: '22px'
    fontWeight: '600'
    lineHeight: '1.25'
    letterSpacing: '-0.01em'
    fontVariantNumeric: 'tabular-nums'
  heading-3:
    fontFamily: 'Plus Jakarta Sans'
    fontSize: '18px'
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: '-0.005em'
    fontVariantNumeric: 'tabular-nums'
  metric-hero:
    fontFamily: 'Inter'
    fontSize: '32px'
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: '-0.015em'
    fontVariantNumeric: 'tabular-nums'
  metric-card:
    fontFamily: 'Inter'
    fontSize: '20px'
    fontWeight: '600'
    lineHeight: '1.25'
    letterSpacing: '-0.01em'
    fontVariantNumeric: 'tabular-nums'
  ticker-badge:
    fontFamily: 'Inter'
    fontSize: '14px'
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: '0.04em'
    fontVariantNumeric: 'tabular-nums'
  body-lg:
    fontFamily: 'Inter'
    fontSize: '18px'
    fontWeight: '400'
    lineHeight: '1.55'
    letterSpacing: '0em'
    fontVariantNumeric: 'tabular-nums'
  body-base:
    fontFamily: 'Inter'
    fontSize: '16px'
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: '0em'
    fontVariantNumeric: 'tabular-nums'
  body-sm:
    fontFamily: 'Inter'
    fontSize: '14px'
    fontWeight: '500'
    lineHeight: '1.45'
    letterSpacing: '0.005em'
    fontVariantNumeric: 'tabular-nums'
  caption:
    fontFamily: 'Inter'
    fontSize: '12px'
    fontWeight: '500'
    lineHeight: '1.4'
    letterSpacing: '0.01em'
    fontVariantNumeric: 'tabular-nums'
rounded:
  none: '0px'
  sm: '6px'
  md: '10px'
  lg: '16px'
  xl: '24px'
  2xl: '32px'
  full: '9999px'
spacing:
  '1': '4px'
  '2': '8px'
  '3': '12px'
  '4': '16px'
  '5': '20px'
  '6': '24px'
  '8': '32px'
  '10': '40px'
  '12': '48px'
  card-padding-mobile: '16px'
  card-padding-desktop: '24px'
  screen-margin-mobile: '16px'
  screen-margin-desktop: '32px'
  section-gap: '24px'
  touch-target-min: '48px'
components:
  wealth-hero-card:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    accent: '{colors.brand-primary}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    radius: '{rounded.xl}'
    paddingMobile: '{spacing.card-padding-mobile}'
    paddingDesktop: '{spacing.card-padding-desktop}'
    valueFont: '{typography.metric-hero}'
  macro-class-distribution-card:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    radius: '{rounded.lg}'
    paddingMobile: '{spacing.card-padding-mobile}'
    paddingDesktop: '{spacing.card-padding-desktop}'
    barTrack: '{colors.surface-subtle}'
    barTrackDark: '{colors.surface-subtle-dark}'
    barFillPrimary: '{colors.brand-primary}'
    barFillTarget: '{colors.text-primary}'
  contribution-gps-card:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    highlightBorder: '{colors.brand-primary}'
    radius: '{rounded.lg}'
    paddingMobile: '{spacing.card-padding-mobile}'
    paddingDesktop: '{spacing.card-padding-desktop}'
    amountFont: '{typography.metric-card}'
    tickerFont: '{typography.ticker-badge}'
  math-transparency-accordion:
    background: '{colors.surface-subtle}'
    backgroundDark: '{colors.surface-subtle-dark}'
    foreground: '{colors.text-secondary}'
    foregroundDark: '{colors.text-secondary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    radius: '{rounded.md}'
    padding: '{spacing.4}'
    metricFont: '{typography.body-sm}'
  risk-ceiling-saturation-card:
    background: '{colors.semantic-amber-surface}'
    backgroundDark: '{colors.semantic-amber-surface-dark}'
    foreground: '{colors.semantic-amber}'
    foregroundDark: '{colors.semantic-amber-dark}'
    border: '{colors.semantic-amber-border}'
    radius: '{rounded.lg}'
    padding: '{spacing.card-padding-mobile}'
  cerrado-template-selector-card:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    selectedBackground: '{colors.brand-accent-soft}'
    selectedBackgroundDark: '{colors.brand-accent-soft-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    selectedBorder: '{colors.brand-primary}'
    radius: '{rounded.lg}'
    padding: '{spacing.card-padding-mobile}'
  cerrado-quality-card:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    radius: '{rounded.lg}'
    paddingMobile: '{spacing.card-padding-mobile}'
    paddingDesktop: '{spacing.card-padding-desktop}'
    sliderTrackActive: '{colors.brand-primary}'
    sliderTrackQuarantine: '{colors.semantic-amber}'
    thumbSize: '{spacing.touch-target-min}'
  kerno-ai-diagnostic-card:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    headerSurface: '{colors.brand-accent-soft}'
    headerSurfaceDark: '{colors.brand-accent-soft-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    radius: '{rounded.lg}'
    paddingMobile: '{spacing.card-padding-mobile}'
    paddingDesktop: '{spacing.card-padding-desktop}'
    bodyFont: '{typography.body-base}'
  b3-ingestion-crud-sheet:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    radiusTop: '{rounded.xl}'
    padding: '{spacing.card-padding-desktop}'
  button-primary:
    background: '{colors.brand-primary}'
    backgroundHover: '{colors.brand-primary-hover}'
    backgroundActive: '{colors.brand-primary-deep}'
    foreground: '{colors.text-on-primary}'
    radius: '{rounded.md}'
    minHeight: '{spacing.touch-target-min}'
    paddingHorizontal: '{spacing.6}'
    font: '{typography.body-base}'
  button-secondary:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    backgroundHover: '{colors.surface-subtle}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-strong}'
    borderDark: '{colors.border-strong-dark}'
    radius: '{rounded.md}'
    minHeight: '{spacing.touch-target-min}'
    paddingHorizontal: '{spacing.5}'
    font: '{typography.body-base}'
  input-currency:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-strong}'
    borderDark: '{colors.border-strong-dark}'
    focusBorder: '{colors.border-focus}'
    focusBorderDark: '{colors.border-focus-dark}'
    radius: '{rounded.md}'
    minHeight: '{spacing.12}'
    paddingHorizontal: '{spacing.4}'
    valueFont: '{typography.metric-hero}'
  badge-graham-bonus:
    background: '{colors.semantic-emerald-surface}'
    backgroundDark: '{colors.semantic-emerald-surface-dark}'
    foreground: '{colors.semantic-emerald}'
    foregroundDark: '{colors.semantic-emerald-dark}'
    border: '{colors.semantic-emerald-border}'
    radius: '{rounded.full}'
    paddingHorizontal: '{spacing.3}'
    paddingVertical: '{spacing.1}'
    font: '{typography.caption}'
  badge-quarantine:
    background: '{colors.semantic-amber-surface}'
    backgroundDark: '{colors.semantic-amber-surface-dark}'
    foreground: '{colors.semantic-amber}'
    foregroundDark: '{colors.semantic-amber-dark}'
    border: '{colors.semantic-amber-border}'
    radius: '{rounded.full}'
    paddingHorizontal: '{spacing.3}'
    paddingVertical: '{spacing.1}'
    font: '{typography.caption}'
  badge-founder:
    background: '{colors.founder-gold-surface}'
    backgroundDark: '{colors.founder-gold-surface-dark}'
    foreground: '{colors.founder-gold}'
    foregroundDark: '{colors.founder-gold-dark}'
    border: '{colors.founder-gold-border}'
    radius: '{rounded.full}'
    paddingHorizontal: '{spacing.3}'
    paddingVertical: '{spacing.1}'
    font: '{typography.caption}'
  privacy-toggle:
    background: '{colors.surface-subtle}'
    backgroundActive: '{colors.brand-accent-soft}'
    foreground: '{colors.text-primary}'
    foregroundActive: '{colors.brand-primary-deep}'
    radius: '{rounded.full}'
    size: '{spacing.touch-target-min}'
  multi-cpf-selector:
    background: '{colors.surface-subtle}'
    foreground: '{colors.text-primary}'
    border: '{colors.border-subtle}'
    radius: '{rounded.full}'
    minHeight: '{spacing.touch-target-min}'
    paddingHorizontal: '{spacing.4}'
    font: '{typography.body-sm}'
  cvm-regulatory-footer:
    background: '{colors.surface-subtle}'
    backgroundDark: '{colors.surface-subtle-dark}'
    foreground: '{colors.text-secondary}'
    foregroundDark: '{colors.text-secondary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    radius: '{rounded.md}'
    padding: '{spacing.3}'
    font: '{typography.caption}'
  bottom-nav-bar:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    activeForeground: '{colors.brand-primary}'
    inactiveForeground: '{colors.text-muted}'
    borderTop: '{colors.border-subtle}'
    borderTopDark: '{colors.border-subtle-dark}'
    itemMinSize: '{spacing.touch-target-min}'
---

# Kerno — Design System Intergeracional (`DESIGN.md`)

> Contrato de identidade visual do **Kerno** (*"O núcleo inteligente do seu dinheiro"*). Este documento governa *como o produto se parece* nas superfícies Mobile Nativo (iOS e Android via Flutter), Web App Responsivo (Flutter Web SPA) e Landing Page Pública (`https://kerno.afsdigital.com.br`). O documento irmão `EXPERIENCE.md` governa *como o produto funciona* e referencia os tokens deste frontmatter via `{path.to.token}`. Em caso de conflito com qualquer mockup, wireframe ou arquivo importado, `DESIGN.md` e `EXPERIENCE.md` prevalecem como fonte única da verdade.

## Brand & Style

O **Kerno** representa o amadurecimento definitivo do antigo *Graham Select*. Enquanto a primeira geração do produto carregava a estética de um *screener* quantitativo voltado a planilheiros de ações, o **Kerno** nasce como o núcleo calmo, lúcido e preparado para Inteligência Artificial (*AI-ready*) da vida patrimonial pessoal e familiar no Brasil.

A postura estética do **Design System Intergeracional** resolve uma tensão histórica das plataformas de investimento: terminais financeiros tradicionais (como home brokers densos e planilhas multiculunas) intimidam o investidor jovem acostumado à fluidez de fintechs modernas e, simultaneamente, penalizam o investidor maduro (40+ anos, o *"CFO da Família"*) com tipografia diminuta, baixo contraste e poluição visual. Inspirado na clareza tátil, nos espaços generosos de respiro e na hierarquia direta do Nubank, o **Kerno** substitui a ansiedade do mercado financeiro por **calma analítica e transparência matemática**.

Os três pilares visuais da marca **Kerno** são:

1. **Clareza Intergeracional Sem Ruído (*Calm Wealth Ergonomics*):** Uma tela nunca deve parecer um painel de controle de avião. Cada superfície destaca um único número protagonista por cartão, cercado por respiro generoso (`{spacing.card-padding-mobile}` / `{spacing.card-padding-desktop}`), contraste impecável (WCAG 2.1 AA como piso universal e AAA `>= 7:1` para saldos patrimoniais primários) e elasticidade total para fontes ampliadas do sistema operacional (`1.0x` a `2.0x`) sem jamais truncar um único centavo ou ticker.
2. **Autoridade Matemática Sem Imperativos Regulatórios (Conformidade CVM 19/20):** A personalidade visual do **Kerno** transmite precisão de engenharia financeira — não euforia especulativa. Não existem cores de cassino, contadores de *streak*, setas piscantes de *day trade* ou botões imperativos de *"Compre Agora"*. O visual apresenta distâncias matemáticas em relação à meta configurada pelo próprio usuário no **Diagrama do Cerrado** e descontos fundamentalistas calculados pela **Margem de Segurança de Graham**, sempre ancorados por badges semânticos sóbrios e blocos de explicabilidade transparente.
3. **Neutralidade Modular Pronta para IA e Finanças Pessoais:** A identidade centrada no Roxo-Índigo Kerno (`{colors.brand-primary}`) com superfícies claras respiráveis (*Light-First*) e suporte nativo ao modo escuro (*Dark Mode*) sustenta tanto a gestão de patrimônio consolidado (**Dashboard Patrimonial**, **GPS de Aporte**, **Diagrama do Cerrado** e **Kerno AI (Diagnóstico de Carteira)**) quanto a expansão modular futura para o Controle de Gastos Pessoais e o Copiloto Conversacional sem ruptura estética.

## Colors

A paleta do **Design System Intergeracional** é construída sob a filosofia **Light-First Respirável com Paridade Nativa de Dark Mode**. Em ambientes diurnos ou de leitura analítica prolongada, os fundos claros de altíssima luminância (`{colors.surface-base}` e `{colors.surface-elevated}`) maximizam a abertura pupilar e a nitidez para usuários com presbiopia (40+ anos). À noite ou por preferência do sistema operacional, os tokens `*-dark` entregam profundidade em tons de ardósia noturna sem o cansaço visual do preto puro (`#000000`) que causa *halation* (astigmatismo luminoso) em telas OLED.

### Superfícies e Tinta Tipográfica (WCAG AA & AAA)

- **Canvas Base (`{colors.surface-base}` `#F8FAFC` / `{colors.surface-base-dark}` `#0B0F19`):** O pano de fundo da aplicação. Um branco-gelo levemente acinzentado no modo claro que permite que os cartões brancos puros se destaquem sem depender de sombras pesadas.
- **Superfície Elevada (`{colors.surface-elevated}` `#FFFFFF` / `{colors.surface-elevated-dark}` `#111827`):** Reservada para cartões principais (`{components.wealth-hero-card}`, `{components.contribution-gps-card}`, `{components.kerno-ai-diagnostic-card}`, `{components.cerrado-quality-card}`), barra de navegação inferior e modais.
- **Superfície Sutil (`{colors.surface-subtle}` `#F1F5F9` / `{colors.surface-subtle-dark}` `#1E293B`):** Usada para áreas expansíveis de explicabilidade matemática (`{components.math-transparency-accordion}`), trilhas de barras de progresso, rodapés regulatórios CVM e estados inativos.
- **Tinta Primária (`{colors.text-primary}` `#0F172A` / `{colors.text-primary-dark}` `#F8FAFC`):** Aplicada obrigatoriamente a todos os saldos patrimoniais em `R$`, títulos, tickers e valores de aporte. Sobre `{colors.surface-elevated}` (`#FFFFFF`), o tom `#0F172A` entrega razão de contraste de **`17.4:1`** — superando com folga o requisito **WCAG 2.1 AAA (`>= 7:1`)** exigido para saldos patrimoniais primários (`FR-2`).
- **Tinta Secundária (`{colors.text-secondary}` `#334155` / `{colors.text-secondary-dark}` `#CBD5E1`):** Usada para subtítulos, textos analíticos do **Kerno AI (Diagnóstico de Carteira)** e explicações de cálculo. Entrega contraste de **`10.2:1`** (AAA) sobre fundo branco.
- **Tinta Atenuada (`{colors.text-muted}` `#475569` / `{colors.text-muted-dark}` `#94A3B8`):** Usada para metadados, carimbos de data/hora (`D-1`) e rótulos auxiliares. Mesmo o tom mais suave da paleta mantém contraste de **`7.5:1`** (AAA/AA) sobre branco, abolindo o clássico cinza ilegível de interfaces modernas.

### Identidade Kerno (Roxo-Índigo Institucional)

- **Kerno Indigo Primário (`{colors.brand-primary}` `#4F46E5` / `{colors.brand-primary-dark}` `#818CF8`):** A assinatura cromática do **Kerno**. Une a autoridade institucional do azul bancário à modernidade tecnológica das fintechs de nova geração. Aplicado em CTAs primários (`{components.button-primary}` com texto `{colors.text-on-primary}` `#FFFFFF`, contraste **`6.3:1`** AA), abas ativas, barras de progresso de **Macro Classes** e acentos do **Kerno AI**.
- **Kerno Indigo Profundo (`{colors.brand-primary-deep}` `#3730A3`):** Usado para estados pressionados, indicadores de meta no gráfico do **Diagrama do Cerrado** e textos de alto contraste sobre `{colors.brand-accent-soft}` (`#EEF2FF`, contraste **`9.4:1`** AAA).

### Semântica Financeira Daltônico-Segura (Regra da Tríade: Cor + Ícone + Rótulo)

Nenhuma cor semântica no **Kerno** pode transmitir significado sozinha (`FR-2`). Para garantir acessibilidade plena a portadores de protanopia, deuteranopia e tritanopia e leitura cristalina sob luz solar intensa por usuários 40+ (**WCAG 2.1 AAA `>= 7:1`** em todas as cifras e alertas):

- **Esmeralda de Margem e Aporte (`{colors.semantic-emerald}` `#065F46` sobre `{colors.surface-elevated}` `#FFFFFF` com **`7.68:1` AAA** e sobre `{colors.semantic-emerald-surface}` `#ECFDF5` com **`7.13:1` AAA**; Dark Mode `{colors.semantic-emerald-dark}` `#6EE7B7` sobre `{colors.semantic-emerald-surface-dark}` `#064E3B` com **`7.45:1` AAA**):** Representa exclusivamente **Margem de Segurança de Graham** positiva, elegibilidade ao **Multiplicador de Graham** (`1.30x`) e sugestões ativas de alocação no **GPS de Aporte**. Sempre acompanhado de ícone (`Sparkles` / `TrendingUp` / `CheckCircle`) e rótulo textual explícito (ex.: *"Oportunidade Graham + Cerrado"*).
- **Âmbar de Atenção e Quarentena (`{colors.semantic-amber}` `#92400E` sobre `{colors.semantic-amber-surface}` `#FFFBEB` com **`7.05:1` AAA**; Dark Mode `{colors.semantic-amber-dark}` `#FBBF24` sobre `{colors.semantic-amber-surface-dark}` `#451A03` com **`8.80:1` AAA**):** Representa estados de contenção protetiva: ativos em **Quarentena de Aporte** (**Nota de Qualidade (1–10)** `< 5`), ativos em verificação de anomalia de mercado (`QUARENTENA_EVENTO_CORPORATIVO` `> 40%`), avisos de cotação de fechamento (`D-1`) e o cartão de *"Saldo Excedente Protegido por Teto de Risco"*. Sempre acompanhado de ícone (`ShieldAlert` / `PauseCircle` / `AlertTriangle`) e rótulo explícito.
- **Carmesim de Erro e Bloqueio (`{colors.semantic-crimson}` `#991B1B` sobre `{colors.semantic-crimson-surface}` `#FEF2F2` com **`7.60:1` AAA**; Dark Mode `{colors.semantic-crimson-dark}` `#FCA5A5` sobre `{colors.semantic-crimson-surface-dark}` `#450A0A` com **`7.92:1` AAA**):** Reservado estritamente para erros de validação (como soma de **Macro Classes** diferente de `100.00%`), falhas críticas de upload de planilha da B3 e a confirmação final irreversível de exclusão de conta LGPD. **Nunca** utilizado para forçar ou sugerir venda de ativos em queda.
- **Ouro Founder (`{colors.founder-gold}` `#92400E` sobre `{colors.founder-gold-surface}` `#FEF3C7` com borda `{colors.founder-gold-border}` `#F59E0B`, contraste **`6.37:1` AA**; Dark Mode `{colors.founder-gold-dark}` `#FDE68A` sobre `{colors.founder-gold-surface-dark}` `#78350F`):** Reservado exclusivamente para o selo vitalício de **Membro Founder** no cabeçalho autenticado e nos destaques do **Programa Founders** na **Landing Page**.
- **Bordas de Componentes Não-Textuais (WCAG 1.4.11 `>= 3:1`):** `{colors.border-strong}` (`#94A3B8`, contraste **`3.02:1`** sobre `#FFFFFF`) e `{colors.border-strong-dark}` (`#64748B`, contraste **`3.75:1`** sobre `#111827`) delimitam campos de entrada (`{components.input-currency}`) e botões secundários (`{components.button-secondary}`), transitando em foco para `{colors.border-focus}` (`#4F46E5`) / `{colors.border-focus-dark}` (`#818CF8`).

## Typography

A tipografia do **Kerno** combina a personalidade contemporânea e acolhedora da **Plus Jakarta Sans** nos títulos com a precisão geométrica implacável da **Inter** para leitura corrida e dados financeiros tabulares.

### Papéis Tipográficos e Hierarquia

1. **Plus Jakarta Sans (`{typography.display-wealth}`, `{typography.heading-1}`, `{typography.heading-2}`, `{typography.heading-3}`):** Utilizada nos cabeçalhos de seção, nos títulos da **Landing Page** (*"Do caos das planilhas ao núcleo inteligente das suas finanças"*) e nos nomes das 4 áreas principais (**Dashboard Patrimonial**, **GPS de Aporte**, **Kerno AI**, **Diagrama do Cerrado**). Suas terminações abertas mantêm excelente discernimento mesmo para leitores com baixa acuidade visual.
2. **Inter com Numerais Tabulares Obrigatórios (`{typography.metric-hero}`, `{typography.metric-card}`, `{typography.ticker-badge}`, `{typography.body-lg}`, `{typography.body-base}`, `{typography.body-sm}`, `{typography.caption}`):** Todo valor monetário (`R$`), quantidade de cotas, ticker (`^[A-Z]{4}[0-9]{1,2}$`), **Nota de Qualidade (1–10)** e percentual (`%`) aplica obrigatoriamente `font-variant-numeric: tabular-nums` na Web e `FontFeature.tabularFigures()` no Flutter nativo (`FR-2`). Isso garante que colunas e cartões empilhados alinhem casas decimais verticalmente ao milímetro, permitindo comparação instantânea entre `% Atual Pré-Aporte`, `% Meta Efetiva` e `% Projetada Pós-Aporte`.

### Regras Inegociáveis de Escalonamento (`Dynamic Type` `1.0x` a `2.0x`)

- **Honra Nativa ao `textScaler`:** Todos os tokens de tamanho de fonte listados no frontmatter representam a base em escala `1.0x` e multiplicam linearmente pelo `MediaQuery.textScalerOf(context)` do iOS e Android (ou zoom de acessibilidade do navegador) no intervalo de `1.0x` até `2.0x` (`FR-2`).
- **Proibição Absoluta de Truncamento Financeiro:** É estritamente proibido aplicar `TextOverflow.ellipsis` (`...`), `maxLines: 1` com corte ou containers de altura fixa (`height: 80`) em widgets que exibam saldos em `R$`, tickers de ativos, percentuais de alocação ou valores de aporte. Quando o usuário opera em escala `1.3x` (como o protagonista Roberto em `UJ-1`) ou `2.0x`, os layouts horizontais de métricas convertem-se automaticamente via `Wrap` ou `Column` em empilhamento vertical, e o cartão expande sua altura livremente.

## Layout & Spacing

O sistema espacial do **Kerno** baseia-se em uma grade de `4px`/`8px` projetada para eliminar a claustrofobia visual das ferramentas financeiras legadas.

### Ritmo Espacial e Áreas de Toque

- **Respiro Interno de Cartões:** Cartões principais utilizam `{spacing.card-padding-mobile}` (`16px`) em telas móveis e `{spacing.card-padding-desktop}` (`24px`) em telas desktop/tablet, com separação vertical entre grupos de `{spacing.section-gap}` (`24px`) e `{spacing.4}` (`16px`) entre cartões da mesma lista.
- **Margens de Tela:** `{spacing.screen-margin-mobile}` (`16px`) nas laterais do smartphone e `{spacing.screen-margin-desktop}` (`32px`) em viewports amplas, com largura máxima de conteúdo centralizado em `1200px` no **Dashboard Patrimonial** Web e `840px` em fluxos focados de leitura/configuração (**GPS de Aporte** e **Kerno AI**).
- **Piso Ergonômico de Toque (`{spacing.touch-target-min}` `48px`):** Todo elemento interativo — botões, abas da `{components.bottom-nav-bar}`, ícone do `{components.privacy-toggle}`, seletor `{components.multi-cpf-selector}`, cabeçalho do acordeão *"Por que estes ativos?"*, botões 👍/👎 de avaliação do **Kerno AI** e o *thumb* do slider da **Nota de Qualidade (1–10)** — possui área física de toque mínima de `48x48dp` (`FR-2`), prevenindo toques acidentais por usuários maduros ou em movimento no transporte público (`UJ-4`).

### Breakpoints Responsivos e Banimento de Tabelas Móveis

| Faixa de Viewport | Superfície Alvo | Comportamento Estrutural de Layout |
|---|---|---|
| **Mobile (`< 600dp`)** | App Nativo iOS/Android & Web Mobile | Coluna única (`100%` da largura útil). Navegação via `{components.bottom-nav-bar}` fixa na base. **Zero tabelas com rolagem horizontal (`FR-2`):** todos os ativos, notas e sugestões de aporte renderizam como cartões verticais estruturados. Ações secundárias (Upload B3, CRUD Manual Multi-Classe) abrem em Bottom Sheet modal de nível único. |
| **Tablet / Web Compacto (`600dp – 1024dp`)** | iPad / Tablets Android / Janela Web Média | Navegação transita para `NavigationRail` lateral recolhida (`72dp`). Grid de 2 colunas para cartões de resumo do **Dashboard Patrimonial** e para os 4 blocos do **Kerno AI (Diagnóstico de Carteira)**. |
| **Desktop SPA & Landing Page (`> 1024dp`)** | Flutter Web App & Vitrine Pública | Sidebar de navegação expandida (`240dp`) no Web App autenticado. Grid de até 12 colunas: no **Dashboard Patrimonial**, o `{components.wealth-hero-card}` e o `{components.macro-class-distribution-card}` ocupam o topo lado a lado (proporção `5:7`), enquanto o **Diagrama do Cerrado** e o **GPS de Aporte** exibem a lista de cartões de ativos à esquerda e o painel de resumo matemático fixo (*sticky*) à direita. |

→ **Referências visuais de composição em `mockups/`** (os contratos `DESIGN.md` e `EXPERIENCE.md` prevalecem em caso de conflito):
- [mockups/key-dashboard-patrimonial.html](mockups/key-dashboard-patrimonial.html) — Ilustra o layout Mobile `< 600dp` de coluna única, o `{components.wealth-hero-card}` com contraste `17.85:1` AAA, as barras horizontais do `{components.macro-class-distribution-card}`, o estado mascarado `R$ ••••••` do **Modo Privacidade** e o alerta âmbar de evento corporativo (`FR-17`).
- [mockups/key-gps-aporte.html](mockups/key-gps-aporte.html) — Ilustra o ritmo vertical do `{components.input-currency}`, o empilhamento dos cartões `{components.contribution-gps-card}` com quantização de cotas inteiras, o painel `{components.math-transparency-accordion}` aberto e o `{components.risk-ceiling-saturation-card}`.
- [mockups/key-kerno-ai-diagnostico.html](mockups/key-kerno-ai-diagnostico.html) — Ilustra a anatomia dos 4 cartões `{components.kerno-ai-diagnostic-card}` separando cabeçalhos locais em `R$` da narrativa do LLM em `%`, além do estado de degradação graciosa determinística.
- [mockups/key-diagrama-cerrado.html](mockups/key-diagrama-cerrado.html) — Ilustra o `{components.cerrado-template-selector-card}`, o `{components.cerrado-quality-card}` com *thumb* de `48x48dp` e Assistente de 5 Critérios Objetivos, a trava carmesim de soma `!= 100.00%` e o estado âmbar de **Quarentena de Aporte**.
- [mockups/key-web-landing-founders.html](mockups/key-web-landing-founders.html) — Ilustra a reestilização completa das rotas públicas do Frontend Web (`HomePage` `/`, `LoginScreen` `/login` e `DocsScreen` `/docs`) sob a marca **Kerno** com o banner Ouro Founder do **Programa Founders** (`FR-1`, `FR-14`, `FR-15`).
- [mockups/key-web-desktop-restyling.html](mockups/key-web-desktop-restyling.html) — Ilustra a reestilização de **100% das seções autenticadas existentes no Frontend (`MainLayout`, `Sidebar` e todas as 18 rotas de `main.dart`)**: Dashboard, Evolução (`/evolution`), GPS de Aporte (`/graham-recommendations`), Kerno AI (`/kerno-ai`), Diagrama do Cerrado (`/allocation-strategy`), Ranking Graham com `FinancialDataTable` (`/ranking`), Proventos (`/dividends`), Metas (`/metas`), Bens e Direitos/IRPF (`/tax`), Importação B3/Open Finance (`/upload`), Tags (`/tags`), Holding Familiar (`/family-profiles`), Perfil/Suitability/Notificações (`/profile`, `/suitability`, `/notifications/*`) e Painel Admin (`/admin/*`).

## Elevation & Depth

O **Kerno** rejeita tanto o *flat design* sem fronteiras (onde usuários 40+ não distinguem o que é clicável do que é fundo) quanto o excesso de sombras dramáticas. A hierarquia de profundidade opera por **Camadas Tonais + Bordas Estruturais Nítidas**:

- **No Modo Claro (*Light-First*):** Todo cartão elevado (`{colors.surface-elevated}` `#FFFFFF`) sobre o canvas `{colors.surface-base}` (`#F8FAFC`) é delimitado por uma borda estrutural contínua de `1px solid` em `{colors.border-subtle}` (`#E2E8F0`), combinada a uma sombra ambiental de baixíssima difusão (`0 1px 3px rgba(15, 23, 42, 0.04), 0 1px 2px rgba(15, 23, 42, 0.02)`). Quando um cartão recebe foco ou representa a prioridade matemática nº 1 de alocação do **GPS de Aporte**, sua borda transita para `1.5px solid` em `{colors.brand-primary}` (`#4F46E5`).
- **No Modo Escuro (*Dark Mode*):** Sombras escuras são desativadas. A profundidade é comunicada pela progressão luminosa das superfícies (`{colors.surface-base-dark}` `#0B0F19` → `{colors.surface-elevated-dark}` `#111827` → `{colors.surface-subtle-dark}` `#1E293B`) ancoradas por bordas de `1px solid` em `{colors.border-subtle-dark}` (`#1E293B`).
- **Superfícies Modais (Bottom Sheets e Drawers):** Elevam-se sobre um *scrim* translúcido derivado de `{colors.surface-overlay}` (`rgba(15, 23, 42, 0.48)`) com *backdrop-blur* suave (`4px`) na Web, mantendo o foco absoluto na tarefa ativa. Pilhas modais têm profundidade máxima estrita de **1 nível** (nunca se abre um modal sobre outro modal).

## Shapes

A geometria do **Kerno** equilibra a simpatia humana das fintechs de consumo com a sobriedade de um gestor patrimonial:

- **Cartões Protagonistas e Bottom Sheets (`{rounded.xl}` `24px`):** Aplicado ao `{components.wealth-hero-card}` no topo do **Dashboard Patrimonial** e às bordas superiores do `{components.b3-ingestion-crud-sheet}`, criando um ponto focal acolhedor inspirado na ergonomia do Nubank.
- **Cartões Analíticos e Operacionais (`{rounded.lg}` `16px`):** Padrão universal para os cartões de ativos do **GPS de Aporte** (`{components.contribution-gps-card}`), cartões de avaliação de **Nota de Qualidade (1–10)** (`{components.cerrado-quality-card}`) e os 4 cartões do **Kerno AI (Diagnóstico de Carteira)** (`{components.kerno-ai-diagnostic-card}`).
- **Botões, Inputs e Acordeões Internos (`{rounded.md}` `10px`):** Aplicado a `{components.button-primary}`, `{components.button-secondary}`, `{components.input-currency}` e `{components.math-transparency-accordion}` aninhados dentro de cartões maiores, garantindo harmonia concêntrica de raios (`16px` externo → `10px` interno).
- **Badges Semânticos e Controles de Cabeçalho (`{rounded.full}` `9999px`):** Formato de pílula reservado estritamente para selos de estado (`{components.badge-graham-bonus}`, `{components.badge-quarantine}`, `{components.badge-founder}`), para o seletor `{components.multi-cpf-selector}` e para o botão `{components.privacy-toggle}`.

## Components

Cada componente abaixo possui seu par comportamental documentado na seção `## Component Patterns` de `EXPERIENCE.md`. Os nomes são idênticos em ambos os documentos. As composições visuais de referência encontram-se em [mockups/key-dashboard-patrimonial.html](mockups/key-dashboard-patrimonial.html), [mockups/key-gps-aporte.html](mockups/key-gps-aporte.html), [mockups/key-kerno-ai-diagnostico.html](mockups/key-kerno-ai-diagnostico.html), [mockups/key-diagrama-cerrado.html](mockups/key-diagrama-cerrado.html), [mockups/key-web-landing-founders.html](mockups/key-web-landing-founders.html) e [mockups/key-web-desktop-restyling.html](mockups/key-web-desktop-restyling.html).

### 1. `wealth-hero-card` (Cartão Principal do Dashboard Patrimonial)
- **Anatomia Visual:** Superfície `{components.wealth-hero-card.background}` (`{colors.surface-elevated}`) com raio `{components.wealth-hero-card.radius}` (`{rounded.xl}`), borda `{components.wealth-hero-card.border}` e padding `{components.wealth-hero-card.paddingMobile}` / `{components.wealth-hero-card.paddingDesktop}`. No topo, exibe o rótulo *"Patrimônio Líquido Consolidado"* em `{typography.body-sm}` (`{colors.text-secondary}`), ladeado pelo controle `{components.privacy-toggle}` e pelo badge de camada comercial (`{components.badge-founder}` para **Membro Founder** ou pill informativo *"Trial Pro: XX dias restantes"*). No centro, o saldo total em `R$` renderiza em `{components.wealth-hero-card.valueFont}` (`{typography.metric-hero}`, `32px`, `tabular-nums`, cor `{colors.text-primary}` com contraste `17.4:1` AAA). Na base, pílulas resumem a rentabilidade acumulada e a data da última sincronização (`D-1` quando mercado fechado).
- **Estado Modo Privacidade & Escala `2.0x`:** Quando o **Modo Privacidade** está ativo, o valor em `R$` é substituído instantaneamente (`< 300ms`) pelo bloco mascarado `R$ ••••••` na mesma altura de linha de `{typography.metric-hero}` (zero *layout shift*). Em escala de fonte `2.0x`, o cabeçalho do cartão quebra o badge e o saldo em linhas verticais empilhadas sem qualquer truncamento.

### 2. `macro-class-distribution-card` (Gráfico e Mapa de Macro Classes e Subclasses)
- **Anatomia Visual:** Cartão `{components.macro-class-distribution-card.background}` com raio `{components.macro-class-distribution-card.radius}` (`{rounded.lg}`). Apresenta as 4 **Macro Classes** (*Ações BR*, *FIIs*, *Renda Fixa / Tesouro*, *Internacional*). Para cada classe, exibe uma barra horizontal dupla sobre trilha `{components.macro-class-distribution-card.barTrack}`: a barra principal em `{components.macro-class-distribution-card.barFillPrimary}` mostra o `% Atual` e um marcador vertical nítido em `{components.macro-class-distribution-card.barFillTarget}` (`{colors.text-primary}` `#0F172A` com halo branco lateral de `1.5px` para garantir contraste não-textual `>= 3:1` WCAG 1.4.11 mesmo quando a barra ultrapassa a meta) indica a `% Meta` do **Diagrama do Cerrado**. Ao lado (ou abaixo em `2.0x`), numerais em `{typography.body-base}` (`tabular-nums`) mostram `36,0% / Meta 40,0%` e o valor correspondente em `R$` (oculto em **Modo Privacidade**).
- **Agrupadores Analíticos de Subclasse:** Ao expandir uma **Macro Classe**, chips em `{colors.surface-subtle}` exibem a exposição por **Subclasse** (*Setores Defensivos* vs. *Crescimento* em Ações BR; *Tijolo* vs. *Papel* em FIIs) (`FR-4`, `FR-5`).

### 3. `contribution-gps-card` (Cartão de Sugestão de Aporte por Ativo/Classe)
- **Anatomia Visual:** Cartão `{components.contribution-gps-card.background}` com raio `{components.contribution-gps-card.radius}` (`{rounded.lg}`) e borda `{components.contribution-gps-card.border}` (ou `{components.contribution-gps-card.highlightBorder}` para o 1º ativo da fila de prioridade). Divide-se em três zonas verticais que se empilham perfeitamente até `2.0x`:
  1. *Cabeçalho do Ativo:* Ticker em `{components.contribution-gps-card.tickerFont}` (`{typography.ticker-badge}`) dentro de um chip `{colors.surface-subtle}`, nome da empresa/título em `{typography.body-sm}`, tag da **Macro Classe** e, quando elegível ao **Multiplicador de Graham** (`1.30x`), o selo `{components.badge-graham-bonus}` (*"Oportunidade Graham + Cerrado"* ou *"Bônus Graham"*).
  2. *Bloco de Alocação Calculada:* Valor exato a aportar em `R$` renderizado em `{components.contribution-gps-card.amountFont}` (`{typography.metric-card}`, `20px`, `{colors.semantic-emerald}`), acompanhado da quantidade inteira de cotas/ações (`ex.: 142 ações × R$ 24,50`) para ativos negociados em bolsa (*Ações BR*, *FIIs*, *ETFs e BDRs*) ou indicação de *"Aporte contínuo em R$"* para títulos manuais de *Renda Fixa / Tesouro* (`FR-6`).
  3. *Gatilho de Explicabilidade:* Botão de largura total com altura mínima `{spacing.touch-target-min}` (`48px`) rotulado *"Por que estes ativos?"* que expande o `{components.math-transparency-accordion}`.

### 4. `math-transparency-accordion` (Painel Expansível *"Por que estes ativos?"*)
- **Anatomia Visual:** Container `{components.math-transparency-accordion.background}` (`{colors.surface-subtle}`) com raio `{components.math-transparency-accordion.radius}` (`{rounded.md}`) e padding `{components.math-transparency-accordion.padding}` (`{spacing.4}`). Exibe em grade limpa de 2 colunas (convertida para 1 coluna em escala `>= 1.5x`) os indicadores determinísticos de `FR-8` em `{components.math-transparency-accordion.metricFont}` (`tabular-nums`):
  - `% Atual Pré-Aporte` vs. `% Meta Efetiva` vs. `% Projetada Pós-Aporte` (da **Macro Classe** e do ativo);
  - **Nota de Qualidade (1–10)** atual e **Teto Máximo de Risco** aplicável (`10%`, `6%` ou *"Isento — Soberano/ETF Amplo"*);
  - Para *Ações BR*: `Valor Intrínseco (VI)` calculado por Graham, `Cotação Atual` (ou `D-1`) e `% de Margem de Segurança de Graham` + indicação do bônus `1.30x`.

### 5. `risk-ceiling-saturation-card` (Alerta de Saldo Excedente Protegido por Teto de Risco)
- **Anatomia Visual:** Cartão de destaque semântico `{components.risk-ceiling-saturation-card.background}` (`{colors.semantic-amber-surface}`) com borda `{components.risk-ceiling-saturation-card.border}` e texto `{components.risk-ceiling-saturation-card.foreground}`, acompanhado de ícone `ShieldAlert`. Exibido exclusivamente quando todos os ativos elegíveis da carteira atingem seu **Teto Máximo de Risco** e resta saldo não alocado ($X_{\text{rem}} > 0$, `FR-6`), destacando o título *"Saldo Excedente Protegido por Teto de Risco (R$ X_rem)"* em `{typography.heading-3}` e instruindo o cadastro de novo ativo ou alocação em Reserva/Renda Fixa.

### 6. `cerrado-template-selector-card` (Seletor de Templates de Alocação no Onboarding)
- **Anatomia Visual:** Trio de cartões selecionáveis com superfície `{components.cerrado-template-selector-card.background}` (transitando para `{components.cerrado-template-selector-card.selectedBackground}` e borda de `2px` `{components.cerrado-template-selector-card.selectedBorder}` quando ativo). Apresenta os 3 **Templates de Alocação** oficiais (*Equilibrado Cerrado*, *Conservador Patrimonial* e *Acumulação Arrojada*), exibindo uma barra visual das 4 **Macro Classes** somando `100%` e o selo informativo *"Notas iniciais neutras = 6 (Editáveis)"* (`FR-4`).

### 7. `cerrado-quality-card` (Cartão de Ativo e Nota de Qualidade 1–10 no Diagrama do Cerrado)
- **Anatomia Visual:** Superfície `{components.cerrado-quality-card.background}` com raio `{components.cerrado-quality-card.radius}` (`{rounded.lg}`). No topo, exibe ticker, nome, **Macro Classe**, **Subclasse** e a meta efetiva calculada (`% Meta Efetiva`) ao lado do **Teto Máximo de Risco** (`Teto 10%` para notas `8–10`, `Teto 6%` para notas `5–7`, `Teto 0%` para notas `1–4`, ou `Teto da Classe` para Tesouro Direto e ETFs amplos como `IVVB11`/`BOVA11`). No centro, apresenta o slider interativo de `1` a `10` com *thumb* de `{components.cerrado-quality-card.thumbSize}` (`48px`) e trilha `{components.cerrado-quality-card.sliderTrackActive}` (que muda instantaneamente para `{components.cerrado-quality-card.sliderTrackQuarantine}` e exibe o `{components.badge-quarantine}` *"Quarentena de Aporte"* quando a nota cai entre `1` e `4`). Abaixo do slider, um botão secundário discreto abre o **Assistente de 5 Critérios Objetivos** (5 linhas interativas com altura mínima de toque `{spacing.touch-target-min}` `48px` e seletores `0 / 1 / 2 pontos` cada: Governança, Lucro Consistente 5a, Endividamento Controlado, Vantagem Competitiva/Perenidade e Histórico de Proventos/Crescimento). No modo Free pós-trial, o slider assume aparência *read-only* com ícone de cadeado sutil e CTA não-destrutivo de upgrade (`FR-15`).

### 8. `kerno-ai-diagnostic-card` (Conjunto de 4 Cartões do Kerno AI — Diagnóstico de Carteira)
- **Anatomia Visual:** Quatro cartões estruturados com superfície `{components.kerno-ai-diagnostic-card.background}`, cabeçalho acentuado em `{components.kerno-ai-diagnostic-card.headerSurface}` (`{colors.brand-accent-soft}`) e raio `{components.kerno-ai-diagnostic-card.radius}` (`{rounded.lg}`), correspondentes aos 4 blocos de `FR-9`:
  1. **Resumo Executivo** (visível em todas as camadas comerciais; gerado via template determinístico na camada Free pós-trial e via Google Gemini na camada Pro/Founder);
  2. **Pontos Fortes** (com selos esmeralda de diversificação e descontos de Graham);
  3. **Alertas de Risco e Concentração** (com barras de exposição setorial/Subclasse e destaque âmbar para ativos em **Quarentena de Aporte** ou acima do **Teto Máximo de Risco**);
  4. **Racional do Plano de Aporte** (conectando o diagnóstico diretamente ao **GPS de Aporte**).
- **Isolamento Visual LGPD (`FR-10`):** Os valores monetários em `R$` aparecem **exclusivamente** em chips/cabeçalhos determinísticos renderizados localmente pelo app Flutter (respeitando o **Modo Privacidade**), enquanto o corpo narrativo vindo do LLM em `{components.kerno-ai-diagnostic-card.bodyFont}` exibe apenas percentuais (`%`), notas e tickers públicos ou rótulos canônicos traduzidos localmente. Na base do diagnóstico, renderizam os botões de feedback (*"Este diagnóstico foi útil?"* 👍 / 👎 com área `48x48dp`), o carimbo de data/hora do snapshot + contador de cota (`X/5 gerações diárias`) e o `{components.cvm-regulatory-footer}`.

### 9. `b3-ingestion-crud-sheet` (Bottom Sheet / Modal Drawer de Importação B3 e CRUD Manual Multi-Classe)
- **Anatomia Visual:** Superfície modal `{components.b3-ingestion-crud-sheet.background}` com cantos superiores `{components.b3-ingestion-crud-sheet.radiusTop}` (`{rounded.xl}`) no Mobile (ou Drawer lateral de `480px` na Web). Agrupa duas abas internas claras: *Importar Planilha B3* (área de *dropzone*/seleção de arquivo `.xlsx`/`.csv` com barra de progresso assíncrona) e *Posição Manual Multi-Classe* (seletor das 4 **Macro Classes** — *Ações BR*, *FIIs*, *Renda Fixa / Tesouro* e *Internacional* —, seletor de CPF familiar vinculado, campo de ticker/identificador e input monetário em `BRL`/`R$`, `FR-3`).

### 10. `button-primary` & `button-secondary` (Botões de Ação)
- **Anatomia Visual:** `{components.button-primary}` utiliza preenchimento sólido `{colors.brand-primary}` (`#4F46E5`), texto `{colors.text-on-primary}` (`#FFFFFF`) em `{typography.body-base}` (`fontWeight: 600`), raio `{rounded.md}` (`10px`) e altura mínima `{spacing.touch-target-min}` (`48px`). Estados `hover` (`{colors.brand-primary-hover}`) e `active` (`{colors.brand-primary-deep}`) oferecem resposta tátil imediata. `{components.button-secondary}` utiliza fundo `{colors.surface-elevated}`, borda `{colors.border-strong}` (`1.5px`) e texto `{colors.text-primary}`.

### 11. `input-currency` (Campo Monetário de Aporte `R$ X`)
- **Anatomia Visual:** Campo protagonista do **GPS de Aporte** com superfície `{components.input-currency.background}`, borda `{components.input-currency.border}` (transitando para `2px solid` `{components.input-currency.focusBorder}` em foco), raio `{components.input-currency.radius}` (`{rounded.md}`) e altura mínima `{components.input-currency.minHeight}` (`48px`, expandindo com a fonte). O prefixo fixo `R$` em `{colors.text-muted}` precede o valor digitado em `{components.input-currency.valueFont}` (`{typography.metric-hero}`, `tabular-nums`). Abaixo do campo, reserva-se um slot de chip de preenchimento rápido preparado por contrato (`FR-13`) para o futuro botão *"Usar Saldo Livre do Mês"* do Ciclo 2.1 (Controle de Gastos).

### 12. `badge-graham-bonus`, `badge-quarantine` & `badge-founder` (Tríade de Selos Semânticos)
- **Anatomia Visual:** Pílulas com raio `{rounded.full}`, fonte `{typography.caption}` (`12px`, `fontWeight: 600`) e borda de `1px` para garantir nitidez tanto no modo claro quanto no escuro:
  - `{components.badge-graham-bonus}`: Fundo `{colors.semantic-emerald-surface}`, texto `{colors.semantic-emerald}`, ícone `Sparkles` + texto *"Oportunidade Graham + Cerrado"* (ou *"Bônus Graham +30%"*).
  - `{components.badge-quarantine}`: Fundo `{colors.semantic-amber-surface}`, texto `{colors.semantic-amber}`, ícone `PauseCircle` (para Nota `< 5`: *"Quarentena de Aporte — Nota < 5"*) ou `AlertTriangle` (para `FR-17`: *"Cotação em verificação de evento corporativo"*).
  - `{components.badge-founder}`: Fundo `{colors.founder-gold-surface}`, texto `{colors.founder-gold}`, borda `{colors.founder-gold-border}`, ícone `Crown` + texto *"Membro Founder — Vitalício"*.

### 13. `privacy-toggle` & `multi-cpf-selector` (Controles Globais de Cabeçalho)
- **Anatomia Visual:** Posicionados na barra superior global. O `{components.privacy-toggle}` é um botão circular de `{spacing.touch-target-min}` (`48x48dp`) com fundo `{colors.surface-subtle}` (ou `{colors.brand-accent-soft}` quando o **Modo Privacidade** está ativo) e ícone de olho aberto/fechado. O `{components.multi-cpf-selector}` é uma pílula de altura `48dp` que exibe *"Consolidado Família (N CPFs)"* ou o CPF individual selecionado, abrindo o seletor de Holding Familiar (`FR-3`, `FR-15`).

### 14. `cvm-regulatory-footer` & `bottom-nav-bar` (Rodapé Regulatório e Navegação Modular)
- **Anatomia Visual:**
  - `{components.cvm-regulatory-footer}`: Bloco permanente em `{colors.surface-subtle}` com borda `{colors.border-subtle}`, ícone `Info` e texto legal em `{typography.caption}` (`{colors.text-secondary}`) afixado na base do **GPS de Aporte** (`FR-8`) e do **Kerno AI (Diagnóstico de Carteira)** (`FR-10`), atestando a natureza de calculadora matemática parametrizada pelo usuário sob as Resoluções CVM 19 e 20.
  - `{components.bottom-nav-bar}`: Barra inferior fixa no Mobile (`< 600dp`) com superfície `{colors.surface-elevated}` e borda superior `{colors.border-subtle}`. Corrige o problema ergonômico legado de `MainLayout._BottomNavBar` (que espremia 10 destinos na barra inferior móvel) ao exibir as 4 abas primárias do MVP (**Carteira**, **GPS de Aporte**, **Kerno AI**, **Diagrama**) com área mínima de toque `{spacing.touch-target-min}` (`48x48dp`) por item, ícone + rótulo sempre visíveis (ícone ativo em `{colors.brand-primary}`) e arquitetura elástica pronta para acomodar o 5º item (**Gastos**) quando a *feature flag* `enable_expenses_module` for ativada (`FR-13`).

### 15. Contrato de Reestilização de `AppTheme`, `MainLayout`, `FinancialDataTable` e Todas as Seções Brownfield (`frontend/lib/`)
Para garantir que **100% das telas existentes no frontend Flutter** sejam reestilizadas sem deixar nenhuma página com a identidade antiga (*Graham Select*, fundo escuro exclusivo `#0F172A` ou fonte `DM Sans`):
- **`AppTheme` (`frontend/lib/src/core/theme/app_theme.dart`) e `main.dart`:**
  - `MaterialApp.router` atualiza `title: 'Kerno'`, define `theme: AppTheme.lightTheme` (superfícies `{colors.surface-base}` `#F8FAFC` e `{colors.surface-elevated}` `#FFFFFF`, texto `{colors.text-primary}` `#0F172A`, primária `{colors.brand-primary}` `#4F46E5`) como padrão Light-First e `darkTheme: AppTheme.darkTheme` (`{colors.surface-base-dark}` `#0B0F19`, `{colors.surface-elevated-dark}` `#111827`) com suporte a `themeMode`.
  - A família `GoogleFonts.dmSans` do código legado é substituída por `GoogleFonts.plusJakartaSans` nos papéis de título/display e `GoogleFonts.inter` com `FontFeature.tabularFigures()` em todo o `textTheme`.
- **`MainLayout` (`frontend/lib/src/core/widgets/main_layout.dart`):**
  - `_TopBar` exibe o logotipo **Kerno**, os filtros analíticos (`DashboardTagFilterBar` e `AnalyticalLensTabs` quando na visão de carteira), o seletor `{components.multi-cpf-selector}`, o botão `{components.privacy-toggle}`, o selo `{components.badge-founder}`, o botão de notificações e o atalho `Painel Admin` (`role == 'ADMIN'`).
  - `_Sidebar` (Desktop `> 1024dp`, largura `240dp–260dp`, e Tablet `600dp–1024dp` em `NavigationRail` recolhida) organiza todas as rotas existentes em 5 grupos visuais limpos (ver [mockups/key-web-desktop-restyling.html](mockups/key-web-desktop-restyling.html)).
  - `_BottomNavBar` (Mobile `< 600dp`) exibe exclusivamente as 4 abas nucleares (**Carteira**, **GPS de Aporte**, **Kerno AI**, **Diagrama**), enquanto as demais seções existentes permanecem a 1 toque de distância dentro de seus respectivos Hubs (Sub-abas em **Carteira** para *Evolução*, *Proventos*, *Metas*, *IRPF* e *Importação B3/Tags*; Sub-aba em **Diagrama** para *Ranking Graham*; e *Menu de Perfil* no Header Global para *Holding Familiar*, *Suitability*, *Notificações WhatsApp/Web Push*, *LGPD* e *Admin*).
- **`FinancialDataTable` (`frontend/lib/src/core/widgets/financial_data_table.dart`):**
  - Em viewports `>= 600dp` (usado em `RankingPage`, `DividendsPage`, `TaxDeclarationPage` e `AdminTickersPage`), renderiza cabeçalho em `{colors.surface-subtle}`, bordas `{colors.border-subtle}` e células numéricas alinhadas com `tabular-nums`.
  - Em viewports móveis (`< 600dp`), o widget comuta automaticamente via `LayoutBuilder` para uma `ListView` de cartões empilhados (`{colors.surface-elevated}`, raio `{rounded.lg}`), eliminando 100% do `SingleChildScrollView(scrollDirection: Axis.horizontal)` em smartphones (`FR-2`).
- **Páginas Brownfield Reestilizadas (`FR-1`, `FR-2`, `FR-3`):**
  - `HomePage` (`/`), `LoginScreen` (`/login`) e `DocsScreen` (`/docs`): Reestilizadas com a identidade **Kerno** e o **Programa Founders** (ver [mockups/key-web-landing-founders.html](mockups/key-web-landing-founders.html)).
  - `DashboardPage` (`/dashboard`), `EvolutionPage` (`/evolution`), `GoalsPage` (`/metas`), `DividendsPage` (`/dividends`) e `TaxDeclarationPage` (`/tax`): Recebem os cartões `{colors.surface-elevated}`, gráficos em `{colors.brand-primary}` / `{colors.semantic-emerald}` e **obedecem integralmente ao `{components.privacy-toggle}`** (mascarando qualquer cifra em `R$` como `R$ ••••••`).
  - `AllocationStrategyPage` (`/allocation-strategy`) e `GrahamRecommendationsPage` (`/graham-recommendations`): Evoluem visual e funcionalmente para o **Diagrama do Cerrado** e o **GPS de Aporte**, mantendo compatibilidade de rota no `GoRouter`.
  - `RankingPage` (`/ranking`), `UploadPage` (`/upload` com `B3UploadZone` e `OpenFinanceProvider`), `TagManagementPage` (`/tags` e `AssetTagSelectorSheet`), `FamilyProfilesPage` (`/family-profiles`), `ProfilePage` (`/profile`), `InvestorProfilePage` (`/suitability`), `WhatsappNotificationsPage` (`/notifications/whatsapp`), `WebPushNotificationsPage` (`/notifications/web-push`) e páginas de governança (`AdminDashboardPage` `/admin/dashboard`, `AdminDlqPage` `/admin/dlq`, `AdminTickersPage` `/admin/tickers`): Todas migradas para os tokens de superfície, tipografia tabular, bordas `>= 3:1` e botões de `48dp` deste `DESIGN.md` (ver [mockups/key-web-desktop-restyling.html](mockups/key-web-desktop-restyling.html)).

## Do's and Don'ts

| Do (Faça — Padrão Kerno) | Don't (Não Faça — Violação de Design System) |
|---|---|
| Aplicar `font-variant-numeric: tabular-nums` (`FontFeature.tabularFigures()`) em 100% dos valores em `R$`, quantidades, notas e percentuais (`%`). | Usar fontes proporcionais sem alinhamento tabular em listas financeiras ou desalinhar casas decimais entre cartões. |
| Permitir que todos os cartões expandam verticalmente (`Wrap` / `Column`) quando o `textScaler` do sistema operacional estiver entre `1.0x` e `2.0x`. | Aplicar `TextOverflow.ellipsis` (`...`), `maxLines: 1` com corte ou altura fixa em saldos patrimoniais, tickers ou sugestões de aporte (`FR-2`). |
| Empilhar ativos e sugestões de aporte em cartões estruturados de coluna única em viewports móveis (`< 600dp`). | Renderizar tabelas financeiras multicolunas com rolagem horizontal no smartphone (`FR-2`). |
| Combinar obrigatoriamente **Cor Semântica + Ícone + Rótulo Textual Explícito** em todo badge (`{components.badge-graham-bonus}`, `{components.badge-quarantine}`). | Usar apenas vermelho e verde puros para diferenciar estados financeiros, excluindo investidores daltônicos (`FR-2`). |
| Manter contraste mínimo **WCAG AAA (`>= 7:1`)** em saldos patrimoniais primários (`{colors.text-primary}` sobre `{colors.surface-elevated}`) e **AA (`>= 4.5:1`)** no restante. | Usar texto cinza claro de baixo contraste sobre fundo branco ou texto branco sobre fundos coloridos pastel sem verificação de luminância. |
| Exibir valores em `R$` exclusivamente em componentes determinísticos locais (mascarados como `R$ ••••••` no **Modo Privacidade**), mantendo o texto do LLM em `%`. | Injetar valores patrimoniais absolutos em `R$` ou dados pessoais (CPF/nome) dentro do corpo de texto gerado pelo LLM (`FR-10`). |
| Utilizar linguagem visual e textual descritiva, calma e matemática (*"Defasagem relativa de -4,2% em relação à sua meta"*), acompanhada do `{components.cvm-regulatory-footer}`. | Exibir cores de urgência especulativa, alertas de venda forçada ou botões imperativos proibidos pela CVM (*"Compre Agora"*, *"Venda Ação"*). |
| Preservar todas as mentionadas referências a *"Graham"* exclusivamente como nome técnico da fórmula (**Margem de Segurança de Graham** / **Multiplicador de Graham**). | Exibir o nome legado *"Graham Select"* em qualquer logotipo, cabeçalho, título de janela ou metadado visível ao usuário final (`FR-1`). |
