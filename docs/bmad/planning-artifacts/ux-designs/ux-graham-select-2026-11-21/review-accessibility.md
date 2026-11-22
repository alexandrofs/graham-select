# Accessibility Review (WCAG 2.1 AA) — graham-select (Kerno)

> **Auditor Responsável:** Especialista Revisor de Acessibilidade (Accessibility Reviewer — WCAG 2.1 AA & Mobile Ergonomics)  
> **Artefatos Auditados:**  
> - `DESIGN.md`: [docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/DESIGN.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/DESIGN.md)  
> - `EXPERIENCE.md`: [docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/EXPERIENCE.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/EXPERIENCE.md)  
> **Data da Auditoria:** 21 de Novembro de 2026  
> **Padrões de Referência:** WCAG 2.1 Nível AA (com extensões AAA selecionadas), WCAG 2.2 (Target Size e Focus Appearance), Boas Práticas de Acessibilidade Mobile Flutter (iOS VoiceOver e Android TalkBack).

---

## Overall verdict

O par de especificações de UX do **Kerno** (`DESIGN.md` e `EXPERIENCE.md`) apresenta um nível de maturidade e rigor de acessibilidade **excepcionalmente elevado**, muito acima da média do setor financeiro e de fintechs. Destacam-se como pontos de referência de classe mundial:

1. **Inovação em Privacidade Acessível:** A proteção semântica no **Modo Privacidade**, que instrui a árvore `Semantics` do Flutter a silenciar os valores monetários em sintetizadores de voz para evitar vazamento sonoro em ambientes públicos (transporte público), anunciando apenas percentuais e metas relativas.
2. **Independência Cromática Rigorosa:** A institucionalização da **Regra da Tríade** (**Cor + Ícone Distinto + Rótulo Textual Explícito**) para 100% dos status patrimoniais (oportunidades de Graham, Quarentena de Aporte, tetos de risco e travas de alocação), eliminando qualquer dependência do binômio verde/vermelho para usuários daltônicos (protanopia, deuteranopia e tritanopia).
3. **Ergonomia e Elasticidade Tipográfica:** A proibição inegociável de truncamento financeiro (`TextOverflow.ellipsis`, `maxLines: 1`) e de tabelas horizontais em smartphones (`< 600dp`), garantindo empilhamento em cartões verticais e elasticidade de `1.0x` a `2.0x` no `Dynamic Type` do sistema operacional.

Contudo, a auditoria rigorosa identificou **2 problemas críticos** e **3 de alta prioridade** que exigem correções pontuais para assegurar conformidade formal e prática inquestionável com o padrão **WCAG 2.1 AA**:
- **Erro de cálculo no contraste não-textual (WCAG 1.4.11):** O token `border-strong` (`#94A3B8`) sobre `#FFFFFF` atinge apenas **`2.56:1`** (e não `3.02:1` como afirmado no texto), ficando abaixo do limiar obrigatório de `3.0:1` para contornos de inputs e botões secundários;
- **Tokens Dark Mode ausentes no frontmatter:** Componentes de navegação como `bottom-nav-bar` não declaram variantes dark para primeiro plano ativo/inativo, o que provocaria razão de contraste inadequada (`2.82:1` e `2.34:1`) caso mapeados sem fallback correto;
- **Riscos de overflow no Dynamic Type 2.0x:** Cabeçalho global (`_TopBar`) e barra de navegação inferior (`_BottomNavBar`) contêm nós horizontais fixos que podem sofrer `RenderFlex overflow` em escala `2.0x` em smartphones compactos;
- **Falta de Bypass Blocks (WCAG 2.4.1) no Flutter Web:** Ausência de atalho de teclado ("Skip to Main Content") para saltar a sidebar de 18 rotas;
- **Lacuna de semântica para `CustomPainter`:** Barras de progresso e gráficos de evolução não possuem contrato explícito de nó `Semantics`.

**Veredicto Final:** **Aprovado com Ressalvas Pontuais (Correções Mandatórias Pré-Implementação)**. A arquitetura é sólida e os ajustes necessários são pontuais e de baixo esforço.

---

## Findings by Severity

### Critical (2)

- **[critical] Falha no Critério de Contraste Não-Textual (WCAG 1.4.11) no token `border-strong` (`#94A3B8`) sobre `#FFFFFF`** (`DESIGN.md` linhas 52, 279, 291, 405).  
  *Descrição:* A documentação afirma em `DESIGN.md` (linha 405) que `{colors.border-strong}` (`#94A3B8`) entrega razão de contraste de `3.02:1` sobre `#FFFFFF`. O cálculo matemático exato de luminância relativa resulta em **`2.56:1`**. Como esse token é utilizado para delimitar as bordas de campos de entrada (`{components.input-currency}`) e botões secundários (`{components.button-secondary}`), ele falha diretamente no critério de sucesso **WCAG 2.1 SC 1.4.11 (Non-text Contrast)**, que exige no mínimo `3.0:1` para componentes de interface e estados visuais.  
  *Impacto:* Usuários com baixa acuidade visual, idosos (40+) com perda de sensibilidade ao contraste e usuários utilizando o aplicativo sob luz solar direta não conseguirão discernir os limites físicos dos campos de digitação e botões secundários em fundo branco.  
  *Fix:* Atualizar o token `border-strong` no frontmatter e texto para `#768696` (contraste `3.24:1` sobre `#FFFFFF`) ou adotar diretamente o tom ardósia `#64748B` (Slate 500, contraste **`3.98:1`** sobre `#FFFFFF`), garantindo conformidade matemática robusta com WCAG 1.4.11.

- **[critical] Ausência de tokens Dark Mode nos componentes `bottom-nav-bar`, `multi-cpf-selector` e `privacy-toggle` no frontmatter de `DESIGN.md`** (`DESIGN.md` linhas 328-342, 353-360).  
  *Descrição:* O frontmatter de componentes em `DESIGN.md` define tokens sem paridade de modo escuro para componentes estruturais. Especificamente na `{components.bottom-nav-bar}`, declaram-se apenas `activeForeground: '{colors.brand-primary}'` (`#4F46E5`) e `inactiveForeground: '{colors.text-muted}'` (`#475569`). Se esses tokens forem consumidos diretamente pelo tema escuro sobre `{colors.surface-elevated-dark}` (`#111827`), a razão de contraste cai para **`2.82:1`** (ícone ativo) e **`2.34:1`** (ícone inativo), violando gravemente o critério **WCAG 1.4.3 (Contraste Mínimo 4.5:1 / 3:1)**. Omissões similares ocorrem em `privacy-toggle` e `multi-cpf-selector`.  
  *Impacto:* No modo escuro, os ícones e rótulos da barra de navegação inferior e dos botões de controle de privacidade tornam-se quase ilegíveis para qualquer usuário, com contraste abaixo de 3:1.  
  *Fix:* Declarar explicitamente as chaves dark no frontmatter de `DESIGN.md`:
  ```yaml
  bottom-nav-bar:
    background: '{colors.surface-elevated}'
    backgroundDark: '{colors.surface-elevated-dark}'
    activeForeground: '{colors.brand-primary}'
    activeForegroundDark: '{colors.brand-primary-dark}' # #818CF8 (5.95:1 sobre #111827)
    inactiveForeground: '{colors.text-muted}'
    inactiveForegroundDark: '{colors.text-muted-dark}' # #94A3B8 (6.92:1 sobre #111827)
    borderTop: '{colors.border-subtle}'
    borderTopDark: '{colors.border-subtle-dark}'
    itemMinSize: '{spacing.touch-target-min}'
  privacy-toggle:
    background: '{colors.surface-subtle}'
    backgroundDark: '{colors.surface-subtle-dark}'
    backgroundActive: '{colors.brand-accent-soft}'
    backgroundActiveDark: '{colors.brand-accent-soft-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    foregroundActive: '{colors.brand-primary-deep}'
    foregroundActiveDark: '{colors.brand-primary-dark}'
    radius: '{rounded.full}'
    size: '{spacing.touch-target-min}'
  multi-cpf-selector:
    background: '{colors.surface-subtle}'
    backgroundDark: '{colors.surface-subtle-dark}'
    foreground: '{colors.text-primary}'
    foregroundDark: '{colors.text-primary-dark}'
    border: '{colors.border-subtle}'
    borderDark: '{colors.border-subtle-dark}'
    radius: '{rounded.full}'
    minHeight: '{spacing.touch-target-min}'
    paddingHorizontal: '{spacing.4}'
    font: '{typography.body-sm}'
  ```

---

### High (3)

- **[high] Risco de `RenderFlex overflow` no Header Global (`_TopBar`) e na `_BottomNavBar` sob Dynamic Type `2.0x`** (`DESIGN.md` linhas 353-360, 416-420, 520-527; `EXPERIENCE.md` linhas 182-185).  
  *Descrição:* Embora `DESIGN.md` e `EXPERIENCE.md` proíbam o corte de texto financeiro nos cartões (convertendo `Row` em `Column`), componentes de navegação persistentes possuem restrições espaciais severas. Em um smartphone de 360dp de largura:
  1. A `_BottomNavBar` divide a largura em 4 slots (~90dp por aba). Em escala `2.0x`, uma fonte de 12px/14px atinge 24px/28px; rótulos como *"GPS de Aporte"* (13 caracteres) atingem mais de 170dp de largura horizontal, causando quebra de linha com corte vertical ou erro de overflow na altura padrão (56–64dp);
  2. A `_TopBar` agrupa Logo + Seletor Multi-CPF + Privacy Toggle + Founder Badge + Notificações. Em linha única em tela móvel com fonte ampliada, esse conjunto fatalmente colapsará.  
  *Impacto:* Quebra visual de interface, sobreposição de ícones ou travamento de layout em modo de acessibilidade visual ampliada.  
  *Fix:*  
  1. Especificar em `EXPERIENCE.md` que quando `MediaQuery.textScalerOf(context).scale(1) > 1.35x`, a `_BottomNavBar` comute para modo "ícone proeminente com rótulo sob demanda" utilizando o `LargeContentPresenter` nativo (toque longo abre balão ampliado com ícone e rótulo, padrão do iOS/Android Accessibility) ou adote layout de barra expansível verticalmente;  
  2. No Header Mobile (`_TopBar`), estipular que em escalas `> 1.3x`, o badge Founder e atalhos secundários migrem automaticamente para dentro do menu do Avatar/Perfil, mantendo na barra apenas Logo, Seletor Multi-CPF e Privacy Toggle.

- **[high] Ausência de Link de Atalho "Pular para o Conteúdo Principal" (Skip to Main Content) no Flutter Web (WCAG 2.4.1 Level A)** (`EXPERIENCE.md` linhas 77-101, 175-176, 209-216).  
  *Descrição:* Na versão Desktop Web SPA (`> 1024dp`), o layout `MainLayout` possui uma Sidebar expandida com 18 rotas organizadas em 5 grupos. Atualmente, não há especificação de mecanismo de desvio de blocos repetitivos (*Bypass Blocks*). Ao navegar via teclado (`Tab`), o usuário com deficiência motora ou visual precisa pressionar a tecla `Tab` dezenas de vezes a cada troca de página para ultrapassar os links da barra lateral antes de atingir o formulário ou cartão de ativos da página.  
  *Impacto:* Fadiga física severa e lentidão de navegação para usuários que operam exclusivamente via teclado, switches assistivos ou leitores de tela.  
  *Fix:* Adicionar na especificação de `MainLayout` um widget `FocusableActionDetector` de primeiro foco (`SkipToContentLink`), oculto visualmente e exibido no topo esquerdo ao receber foco via `Tab`, permitindo saltar diretamente para a âncora do conteúdo principal (`FocusScope` da página ativa).

- **[high] Falta de Contrato Semântico para Barras de Progresso e Gráficos de Canvas Customizados (`CustomPainter`) (WCAG 1.1.1, 4.1.2)** (`DESIGN.md` linhas 472-475; `EXPERIENCE.md` linhas 186-187).  
  *Descrição:* As barras horizontais de distribuição de Macro Classes (`macro-class-distribution-card`) e as curvas de rentabilidade em `EvolutionPage` utilizam componentes desenhados sob medida via Canvas/`CustomPainter`. O documento `EXPERIENCE.md` detalha brilhantemente a semântica de valores em Modo Privacidade e sliders, mas omite a diretriz para a árvore semântica dos gráficos. Sem `Semantics` associado, leitores de tela interpretam a área do gráfico como um elemento vazio/mudo.  
  *Impacto:* Usuários totalmente cegos utilizando VoiceOver ou TalkBack não conseguem ouvir a proporção das Macro Classes nem a distância em relação à meta.  
  *Fix:* Definir em `EXPERIENCE.md.Accessibility Floor` que todo `CustomPainter` de distribuição ou gráfico DEVE implementar `semanticsBuilder` ou ser envolvido por um widget `Semantics` explícito:
  ```dart
  Semantics(
    container: true,
    label: 'Distribuição Macro Classe Ações BR: 36,0% alocado atualmente contra meta de 40,0%. Defasagem de 4,0% abaixo da meta ideal.',
    child: MacroClassProgressBar(...),
  )
  ```

---

### Medium (4)

- **[medium] Alvos de toque interativos em Badges e Chips menores que `48x48dp` (WCAG 2.1 AAA 2.5.5 / WCAG 2.2 AA 2.5.8)** (`DESIGN.md` linhas 304-327, 514-522; `EXPERIENCE.md` linhas 146, 318).  
  *Descrição:* Na jornada `UJ-5` (passo 5), descreve-se que Camila "toca no selo `{components.badge-founder}` para abrir o resumo em Conta e Plano". Similarmente, chips de Subclasse em `macro-class-distribution-card` e tags em `DashboardTagFilterBar` são interativos. Os tokens de badge declaram `paddingVertical: '4px'` e `font: '12px'`, resultando em altura visual de apenas ~24-28dp. Se o widget não possuir área física invisível estendida, viola a recomendação de `48x48dp` para toque motor.  
  *Impacto:* Dificuldade de acionamento e toques acidentais para investidores com tremores essenciais, mobilidade reduzida ou em uso em movimento (metrô/ônibus).  
  *Fix:* Especificar formalmente que todo badge, pill ou chip com ação de clique/toque utilize `MaterialTapTargetSize.padded` ou envoltório `ConstrainedBox(constraints: BoxConstraints(minWidth: 48, minHeight: 48))` com `HitTestBehavior.translucent`, garantindo alvo de toque físico de 48dp mantendo a elegância visual do elemento.

- **[medium] Ausência de Distinção Não-Cromática para Múltiplas Curvas nos Gráficos de Evolução (`/evolution`) (WCAG 1.4.1 Use of Color)** (`DESIGN.md` linha 542; `EXPERIENCE.md` linha 87).  
  *Descrição:* Embora o Design System Kerno aplique com primor a regra de Cor + Ícone + Rótulo em badges e status, a especificação dos gráficos de evolução patrimonial (`/evolution`) menciona apenas a curva principal em `{colors.brand-primary}` (`#4F46E5`). Ao comparar a rentabilidade da carteira contra múltiplos benchmarks (CDI, Ibovespa, IPCA+), se a distinção ocorrer apenas por cores distintas (ex.: roxo, azul, verde), investidores monocromatas ou daltônicos não distinguirão a carteira dos índices.  
  *Impacto:* Confusão analítica para usuários com deficiência de percepção de cores ao avaliar a performance do portfólio.  
  *Fix:* Determinar que curvas comparativas em gráficos utilizem obrigatoriamente padrões de traço distintos (linha contínua grossa para Carteira, linha tracejada com traços longos para CDI, linha pontilhada para Ibovespa) além de rótulo flutuante (*direct labeling*) nos pontos de fechamento.

- **[medium] Espessura da Borda de Foco de Cartões (`1.5px`) abaixo do Limiar Recomendado (WCAG 2.4.11 Focus Appearance)** (`DESIGN.md` linha 451).  
  *Descrição:* `DESIGN.md` estabelece que quando um cartão recebe foco do teclado ou representa a prioridade nº 1 do GPS de Aporte, sua borda transita para `1.5px solid` em `{colors.brand-primary}` (`#4F46E5`). Contra o fundo claro `#F8FAFC`, uma borda de apenas 1.5px apresenta área de visibilidade marginal, ficando abaixo do limiar de 2px a 3px recomendado pela WCAG 2.2 para indicadores de foco.  
  *Impacto:* Dificuldade de rastreamento visual do foco para usuários de navegação por teclado sem leitor de tela.  
  *Fix:* Elevar a borda de foco para no mínimo `2px solid` ou aplicar contorno com respiro (*outline offset*): `outline: 2px solid {colors.brand-primary}; outline-offset: 2px;`.

- **[medium] Falta de Padronização na Pronúncia Semântica de Valores Monetários em Modo Normal** (`EXPERIENCE.md` linhas 186-187).  
  *Descrição:* `EXPERIENCE.md` define de forma inovadora o comportamento do sintetizador quando o Modo Privacidade está ativo. Porém, em modo normal, sintetizadores de tela lendo strings brutas como `"R$ 1.245.890,00"` ou `"142 ações × R$ 24,50"` frequentemente pronunciam pontuações de forma truncada ("erre cifrão um ponto dois...").  
  *Impacto:* Experiência auditiva desagradável e lentidão para compreender saldos e quantidades de ativos sugeridos.  
  *Fix:* Recomendar no contrato de componentes o uso de extensão de semântica de moeda no Flutter:
  ```dart
  Semantics(
    label: 'Patrimônio total: um milhão, duzentos e quarenta e cinco mil, oitocentos e noventa reais',
    child: Text('R$ 1.245.890,00'),
  )
  ```

---

### Low (3)

- **[low] Omissão de `borderDark` no cartão `risk-ceiling-saturation-card` e nos Badges no frontmatter** (`DESIGN.md` linhas 210-216, 303, 313, 323).  
  *Descrição:* No frontmatter, `risk-ceiling-saturation-card`, `badge-graham-bonus` e `badge-quarantine` possuem propriedades `backgroundDark` e `foregroundDark`, mas mantêm `border` apontando para o token de borda claro (ex.: `semantic-amber-border` `#FDE68A`).  
  *Impacto:* Inconsistência cosmética menor no tema escuro, sem impacto severo de contraste.  
  *Fix:* Incluir tokens `borderDark` com variantes escurecidas ou correspondentes no frontmatter.

- **[low] Ausência de Anúncio de Conclusão de Cálculo Assíncrono via `SemanticsService.announce` (WCAG 4.1.3)** (`EXPERIENCE.md` linhas 137-140, 252).  
  *Descrição:* Quando o investidor aciona *"Calcular Aporte"* no GPS de Aporte, o cálculo conclui em < 1.0s e atualiza os cartões dinamicamente. Um usuário cego não recebe feedback sonoro imediato de que a lista de compras foi montada.  
  *Impacto:* Usuário precisa explorar a tela manualmente para verificar se o cálculo foi concluído.  
  *Fix:* Disparar chamada de acessibilidade pós-cálculo:
  ```dart
  SemanticsService.announce(
    'Cálculo concluído. 3 ativos priorizados para o aporte de ${formatCurrency(amount)}.',
    TextDirection.ltr,
  );
  ```

- **[low] Especificação de Comportamento em Orientação Paisagem (Landscape) no Mobile (WCAG 1.3.4)** (`EXPERIENCE.md` linha 20).  
  *Descrição:* O documento não detalha o comportamento do layout mobile quando o dispositivo é girado para o modo horizontal (Landscape).  
  *Impacto:* Se o app não adaptar a coluna vertical em landscape, diálogos e modais de Bottom Sheet podem sofrer corte vertical com o teclado virtual aberto.  
  *Fix:* Declarar explicitamente que telas móveis suportam orientação horizontal com rolagem livre e envoltório `SafeArea` adaptado.

---

## Checklist WCAG 2.1 AA

| Critério de Sucesso WCAG 2.1 | Nível | Status | Notas da Auditoria |
|---|---|---|---|
| **1.1.1 Non-text Content** | A | **Partial** | Badges e ícones possuem rótulos claros. Requer adicionar nós `Semantics` nas barras e gráficos com `CustomPainter` (Finding #5). |
| **1.2.1 – 1.2.9 Time-based Media** | A / AA | **Compliant** | Não aplicável (a plataforma não utiliza áudio/vídeo obrigatórios). |
| **1.3.1 Info and Relationships** | A | **Partial** | Hierarquia tipográfica excelente (`heading-1` a `heading-3`). Tabelas em desktop precisam de cabeçalhos semânticos (`FinancialDataTable`); Mobile usa cartões limpos. |
| **1.3.2 Meaningful Sequence** | A | **Compliant** | Ordem de leitura lógica preservada em colunas empilhadas no Mobile e na navegação de abas. |
| **1.3.3 Sensory Characteristics** | A | **Compliant** | Nenhuma instrução depende de forma geométrica, posição física isolada ou som. |
| **1.3.4 Orientation** | AA | **Compliant** | Sem bloqueio artificial de orientação; requer registrar formalmente suporte à rolagem em Landscape (Finding #12). |
| **1.3.5 Identify Input Purpose** | AA | **Compliant** | Campos de entrada financeira e login configurados para tipos de entrada padronizados. |
| **1.4.1 Use of Color** | A | **Partial** | **Excelente** na Regra da Tríade de badges e quarentena. Requer inclusão de padrões de traço tracejados/pontilhados em gráficos de evolução comparativa (Finding #7). |
| **1.4.2 Audio Control** | A | **Compliant** | Nenhum áudio reproduzido automaticamente. |
| **1.4.3 Contrast (Minimum)** | AA | **Compliant\*** | Textos primários, secundários e atenuados atingem de `6.29:1` a `17.85:1` (AAA). \*Nota: corrigindo a omissão de tokens dark em `bottom-nav-bar` (Finding #2). |
| **1.4.4 Resize Text** | AA | **Partial** | Cartões e valores em `R$` escalam até 200% sem reticências ou cortes. Header e barra inferior necessitam de estratégia contra overflow (Finding #3). |
| **1.4.5 Images of Text** | AA | **Compliant** | Nenhum texto renderizado como imagem bitmap. Todos os títulos e números utilizam tipografia nativa vetorial. |
| **1.4.10 Reflow** | AA | **Compliant** | Banimento estrito de tabelas horizontais em viewports `< 600dp`. Coluna única contínua com quebra vertical limpa. |
| **1.4.11 Non-text Contrast** | AA | **Non-Compliant** | Token `border-strong` (`#94A3B8`) sobre `#FFFFFF` atinge apenas **`2.56:1`** contra a meta mínima de `3.0:1` (Finding #1). |
| **1.4.12 Text Spacing** | AA | **Compliant** | Rampa tipográfica baseada em `Inter` e `Plus Jakarta Sans` com `lineHeight` e espaçamentos elásticos compatíveis. |
| **1.4.13 Content on Hover or Focus** | AA | **Compliant** | Tooltips e acordeões expansíveis podem ser fechados sem mover o ponteiro (`Esc` e toque). |
| **2.1.1 Keyboard** | A | **Compliant** | Sliders com setas `←`/`→`, acordeões com `Enter`/`Espaço` e modais com `Esc` totalmente operáveis por teclado. |
| **2.1.2 No Keyboard Trap** | A | **Compliant** | Modais e Bottom Sheets de nível único com foco fechado e tecla `Esc` de liberação imediata. |
| **2.1.4 Character Key Shortcuts** | A | **Compliant** | Não existem atalhos de tecla única não modificada que interfiram com tecnologias assistivas. |
| **2.2.1 Timing Adjustable** | A | **Compliant** | Sem relógios regressivos ou timeouts que causem perda de dados; retorno após 2 minutos em background exige biometria sem apagar o estado do aporte. |
| **2.2.2 Pause, Stop, Hide** | A | **Compliant** | Zero carrosséis automáticos, tickers piscantes ou elementos em movimento não controlados pelo usuário. |
| **2.3.1 Three Flashes or Below Threshold** | A | **Compliant** | Zero flashes ou cintilações em toda a interface. |
| **2.4.1 Bypass Blocks** | A | **Non-Compliant** | Falta mecanismo de atalho para saltar o menu lateral no Flutter Web (Finding #4). |
| **2.4.2 Page Titled** | A | **Compliant** | Rotas do `GoRouter` declaram títulos informativos com a marca Kerno. |
| **2.4.3 Focus Order** | A | **Compliant** | Sequência lógica de tabulação seguindo o fluxo de cima para baixo e esquerda para a direita. |
| **2.4.4 Link Purpose (In Context)** | A | **Compliant** | Textos de ação descritivos (*"Calcular Aporte"*, *"Por que estes ativos?"*, *"Salvar Estratégia"*). |
| **2.4.5 Multiple Ways** | AA | **Compliant** | Acesso através de BottomNav, Sidebar agrupada, Hubs de Carteira e atalhos de cabeçalho. |
| **2.4.6 Headings and Labels** | AA | **Compliant** | Seções bem delimitadas por cabeçalhos hierárquicos e campos monetários com prefixo explícito `R$`. |
| **2.4.7 Focus Visible** | AA | **Partial** | Indicadores de foco visíveis em todos os controles interativos; espessura de 1.5px em cartões deve ser elevada para 2px (Finding #8). |
| **2.5.1 Pointer Gestures** | A | **Compliant** | Banimento explícito de gestos ocultos sem botão equivalente visível (ex.: sem *swipe-to-delete* sem botão explícito). |
| **2.5.2 Pointer Cancellation** | A | **Compliant** | Disparo de eventos no evento *up* com suporte a cancelamento de arrasto. |
| **2.5.3 Label in Name** | A | **Compliant** | O nome acessível de botões e cartões corresponde exatamente ao texto visualizado. |
| **2.5.4 Motion Actuation** | A | **Compliant** | Nenhuma funcionalidade depende de agitar ou inclinar o dispositivo móvel. |
| **2.5.5 / 2.5.8 Target Size** | AAA / AA | **Partial** | Piso de `48x48dp` respeitado na quase totalidade dos botões e sliders. Requer assegurar área invisível estendida em badges e tags clicáveis (Finding #6). |
| **3.1.1 Language of Page** | A | **Compliant** | Interface declarada integralmente em `pt-BR`. |
| **3.2.1 On Focus** | A | **Compliant** | O ato de receber foco não dispara navegações ou submissões de dados não solicitadas. |
| **3.2.2 On Input** | A | **Compliant** | Ajuste no slider da Nota de Qualidade recalcula metas em tela localmente sem submeter formulário. |
| **3.2.3 Consistent Navigation** | AA | **Compliant** | Posicionamento estritamente consistente de navegação no Mobile (`BottomNavBar`) e Desktop (`Sidebar`). |
| **3.2.4 Consistent Identification** | AA | **Compliant** | Ícones de olho (Modo Privacidade), cadeado (Quarentena), estrela (Graham) mantêm mesmo significado em todo o sistema. |
| **3.3.1 Error Identification** | A | **Compliant** | Identificação em carmesim com texto explicativo e ícone de alerta em erros de soma e falhas de importação. |
| **3.3.2 Labels or Instructions** | A | **Compliant** | Instruções contextuais e disclaimers regulatórios CVM 19/20 permanentes. |
| **3.3.3 Error Suggestion** | AA | **Compliant** | A interface instrui exatamente como sanar o erro (ex.: *"A soma está em 95,0%. Faltam +5,0% para fechar 100,0%"*). |
| **3.3.4 Error Prevention (Financial/Legal)** | AA | **Compliant** | Confirmação irreversível explícita no expurgo de conta LGPD; travas que impedem compra de ativos em quarentena ou violação de tetos. |
| **4.1.1 Parsing** | A | **Compliant** | Estrutura limpa de árvore de widgets Flutter sem conflitos de tags ou duplicação de identificadores. |
| **4.1.2 Name, Role, Value** | A | **Partial** | Sliders e acordeões expõem valores corretamente. Necessário estender a componentes gráficos via `Semantics` (Finding #5). |
| **4.1.3 Status Messages** | AA | **Partial** | Requer chamada a `SemanticsService.announce` ao término do cálculo do GPS de Aporte (Finding #11). |

---

## Recomendações Prioritárias para o Time de Engenharia e UX

1. **Sprint Imediato (Correções de Tokens e Contraste):**
   - Alterar `{colors.border-strong}` para `#64748B` (Slate 500) em `DESIGN.md`;
   - Inserir as definições `activeForegroundDark` e `inactiveForegroundDark` em `bottom-nav-bar` e completar os pares dark de `privacy-toggle` e `multi-cpf-selector`.
2. **Sprint de Componentes Base (Acessibilidade de Navegação):**
   - Implementar o componente `SkipToContentLink` em `MainLayout` para a versão Web;
   - Adicionar o comportamento de `LargeContentPresenter` na `_BottomNavBar` para telas sob `textScaler > 1.3x`.
3. **Sprint de Refinamento Assistivo (Semântica e Leitores de Tela):**
   - Criar wrapper reutilizável `SemanticsChartContainer` para barras de progresso e gráficos de linha com descrição verbal em português;
   - Garantir área de toque invisível expandida (`48x48dp`) em badges e chips interativos através de `BoxConstraints(minWidth: 48, minHeight: 48)`.
