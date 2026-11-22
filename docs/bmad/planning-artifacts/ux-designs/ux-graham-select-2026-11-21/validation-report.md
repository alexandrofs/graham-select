# Validation Report — graham-select (Kerno)

- **DESIGN.md:** `docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/DESIGN.md`
- **EXPERIENCE.md:** `docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/EXPERIENCE.md`
- **Run at:** 2026-11-21T18:22:00-03:00

## Overall verdict

A auditoria integrada das especificações de UX do projeto **Kerno** (`DESIGN.md` e `EXPERIENCE.md`) atesta uma maturidade de engenharia e coesão contratual de padrão **STRONG** (forte), com conformidade mecânica total em fluxos, tokens, componentes, estados e referências visuais para os consumidores downstream (Arquitetura Técnica, Decomposição de Épicos e Implementação Flutter/Spring Boot).

Paralelamente, as lentes especializadas de **Acessibilidade (WCAG 2.1 AA)** e **Regulatório & Compliance (CVM e LGPD)** confirmaram inovações de destaque no ecossistema de fintechs — como o silenciamento semântico de cifras monetárias no Modo Privacidade no VoiceOver/TalkBack, a institucionalização da Regra da Tríade (Cor + Ícone + Rótulo) para daltônicos e a blindagem de 0 PII no Sanitizador LGPD de IA. Foram detectados **4 apontamentos Críticos** e **6 de Alta severidade** concentrados estritamente em ajustes finos pré-implementação: correção de cálculo de contraste no token `border-strong` (de 2.56:1 para >= 3.0:1), declaração de propriedades de primeiro plano dark no frontmatter, obrigatoriedade de disclaimer CVM no Ranking Graham (`/ranking`), consentimento explícito aos Termos e Privacidade no onboarding OAuth2 de 1 clique e salvaguarda de overflow no Dynamic Type 2.0x.

## Category verdicts

- Flow coverage — **strong**
- Token completeness — **strong**
- Component coverage — **strong**
- State coverage — **strong**
- Visual reference coverage — **strong**
- Bloat & overspecification — **strong**
- Inheritance discipline — **strong**
- Shape fit — **strong**
- Accessibility Review (WCAG 2.1 AA) — **adequate** *(Aprovado com Ressalvas Pontuais)*
- Regulatory & Compliance Review (CVM & LGPD) — **adequate** *(Aprovado com Ressalvas Pontuais)*

---

## Findings by severity

### Critical (4)

- **[Accessibility — WCAG 1.4.11] Falha no Contraste Não-Textual no token `border-strong` (`#94A3B8`) sobre `#FFFFFF`** (`DESIGN.md` § L52, L279, L291, L405)  
  *Descrição:* A documentação afirma que `{colors.border-strong}` entrega contraste de 3.02:1, mas o cálculo matemático exato de luminância sobre `#FFFFFF` é de **2.56:1**, falhando no piso mínimo de 3.0:1 exigido pela WCAG 2.1 SC 1.4.11 para bordas de campos de entrada (`input-currency`) e botões secundários (`button-secondary`).  
  *Fix:* Atualizar o token no frontmatter e texto para `#64748B` (Slate 500, contraste **3.98:1** sobre `#FFFFFF`).

- **[Accessibility — WCAG 1.4.3] Ausência de tokens Dark Mode nos componentes `bottom-nav-bar`, `multi-cpf-selector` e `privacy-toggle`** (`DESIGN.md` § L328–342, L353–360)  
  *Descrição:* Os nós desses componentes no frontmatter definem apenas propriedades light. Caso consumidos sem variantes dark declaradas sobre `{colors.surface-elevated-dark}` (`#111827`), a razão de contraste cai para **2.82:1** e **2.34:1**, violando o critério WCAG 1.4.3.  
  *Fix:* Declarar explicitamente as chaves dark nos nós dos componentes (`activeForegroundDark: '{colors.brand-primary-dark}'`, `inactiveForegroundDark: '{colors.text-muted-dark}'`, `backgroundDark`, etc.).

- **[Regulatory — CVM 20/2021] Ausência de Disclaimer Regulatório CVM na Tela de Ranking Graham (`/ranking`) e no widget `FinancialDataTable`** (`EXPERIENCE.md` § L92, L124–128; `DESIGN.md` § L523–526, L538–544)  
  *Descrição:* A tela de `/ranking` ordena ações com base na Margem Graham. A Resolução CVM nº 20/2021 exige aviso inequívoco de que rankings sem disclaimer podem ser interpretados como relatórios de análise ou carteiras recomendadas, expondo a plataforma a sanções por exercício não registrado de análise de valores mobiliários.  
  *Fix:* Tornar obrigatória a presença do componente `{components.cvm-regulatory-footer}` no topo e base de `/ranking`: *"Aviso Regulatório (CVM 20/2021): O Ranking Graham é uma ferramenta de triagem quantitativa estritamente baseada em dados públicos passados das demonstrações contábeis. Não constitui carteira recomendada ou indicação de compra."*

- **[Regulatory — LGPD Arts. 7 e 8] Ausência de Consentimento Explícito e Links de Termos e Privacidade no Onboarding OAuth2 de 1 clique** (`EXPERIENCE.md` § L32–33, L79–85, L316; `DESIGN.md` § L541)  
  *Descrição:* A jornada UJ-5 descreve cadastro via CTA de 1 clique pelo Google OAuth2 sem exibir previamente aceite dos Termos de Uso e Política de Privacidade, expondo a base de dados a autuações da ANPD por falta de transparência e consentimento viciado. As rotas `/terms` e `/privacy` também não constam na tabela do `GoRouter`.  
  *Fix:* Criar as rotas públicas `/terms` e `/privacy` no `GoRouter` e inserir texto legal mandatório junto ao botão OAuth: *"Ao continuar, você concorda com os [Termos de Uso](/terms) e confirma ciência da [Política de Privacidade](/privacy)"*, persistindo o aceite com timestamp.

---

### High (6)

- **[Accessibility — WCAG 1.4.4] Risco de `RenderFlex overflow` no Header Global (`_TopBar`) e `_BottomNavBar` sob Dynamic Type `2.0x`** (`DESIGN.md` § L353–360, L416–420, L520–527; `EXPERIENCE.md` § L182–185)  
  *Descrição:* Em smartphones compactos (360dp de largura) com Dynamic Type configurado em 2.0x, rótulos de navegação e botões em linha única sofrem quebra de linha com estouro vertical ou corte de texto.  
  *Fix:* Sob `textScaler > 1.35x`, a `_BottomNavBar` deve comutar para ícones com `LargeContentPresenter` (toque longo expande balão acessível) e a `_TopBar` deve mover badges secundários para o menu do avatar/perfil.

- **[Accessibility — WCAG 2.4.1] Ausência de Link de Atalho "Pular para o Conteúdo Principal" (Skip to Main Content) no Flutter Web** (`EXPERIENCE.md` § L77–101, L175–176, L209–216)  
  *Descrição:* No Desktop Web SPA, a barra lateral possui 18 rotas; usuários de teclado ou leitores de tela precisam pressionar dezenas de vezes a tecla `Tab` a cada navegação para chegar ao conteúdo da página.  
  *Fix:* Incluir na especificação de `MainLayout` o widget `SkipToContentLink` (`FocusableActionDetector`), acessível no primeiro foco de `Tab` para saltar diretamente ao container principal.

- **[Accessibility — WCAG 1.1.1 / 4.1.2] Falta de Contrato Semântico para Barras de Progresso e Gráficos (`CustomPainter`)** (`DESIGN.md` § L472–475; `EXPERIENCE.md` § L186–187)  
  *Descrição:* As barras horizontais de distribuição de Macro Classes e curvas de rentabilidade em `EvolutionPage` utilizam Canvas/`CustomPainter` sem diretriz explícita de nós `Semantics` para leitores de tela.  
  *Fix:* Definir em `EXPERIENCE.md.Accessibility Floor` que todo `CustomPainter` de distribuição deve implementar `semanticsBuilder` ou ser envolvido por widget `Semantics` descrevendo percentuais e valores comparativos.

- **[Regulatory — CVM 20/2021] Uso do Substantivo "Oportunidade" no Selo do Multiplicador de Graham (`badge-graham-bonus`)** (`DESIGN.md` § L401, L478, L516; `EXPERIENCE.md` § L118, L189)  
  *Descrição:* O selo semântico renderiza o texto *"Oportunidade Graham + Cerrado"*. O termo possui carga promocional e indutiva de compra, conflitante com o posicionamento algorítmico neutro exigido pela CVM.  
  *Fix:* Substituir por nomenclatura puramente matemática: *"Margem Graham Positiva"*, *"Fator Graham Elegível (1.30x)"* ou *"Desconto Contábil Graham"*.

- **[Regulatory — B3 Market Data & CDC] Ausência de Aviso de Defasagem de Cotações Intradiárias (Delay de 15 Minutos da B3) Durante o Pregão** (`DESIGN.md` § L390, L469, L486; `EXPERIENCE.md` § L135, L162, L256)  
  *Descrição:* A especificação cobre cotações de fechamento (D-1), mas omite aviso durante o pregão aberto de que cotações de varejo operam com atraso de no mínimo 15 minutos em relação ao book da B3.  
  *Fix:* Especificar carimbo de data/hora acompanhado do status: *"Cotações B3 com atraso de 15 min (Última atualização às HH:mm:ss)"* durante o pregão.

- **[Regulatory — CVM] Exposição da Palavra "Recommendations" em Rota e Alias Técnico (`/graham-recommendations`)** (`EXPERIENCE.md` § L70, L89; `DESIGN.md` § L445, L543)  
  *Descrição:* A menção da palavra em inglês "recommendations" colide diretamente com a tese regulatória perante a CVM de que a plataforma não emite recomendações individuais.  
  *Fix:* Renomear rota canônica para `/gps-aporte` ou `/alocacao-mensal`, mantendo redirect interno de rede sem exposição pública em links, breadcrumbs ou telemetria.

---

### Medium (8)

- **[Accessibility — WCAG 2.5.5 / 2.5.8]** Alvos de toque visuais de badges e chips menores que 48x48dp sem preenchimento explícito (`EXPERIENCE.md` § L178–181). *Fix:* Aplicar `MaterialTapTargetSize.padded` ou `BoxConstraints(minWidth: 48, minHeight: 48)` na área de clique.
- **[Accessibility — WCAG 1.4.1]** Falta de padrões de traço distintos (linhas tracejadas/pontilhadas) para curvas comparativas em gráficos (`DESIGN.md` § L396–403). *Fix:* Utilizar traço contínuo para a carteira e tracejado para benchmarks (IBOV/CDI).
- **[Accessibility — WCAG 2.4.11]** Espessura de 1.5px na borda de foco de cartões interativos abaixo dos 2px recomendados (`DESIGN.md` § L455). *Fix:* Ajustar indicador de foco para 2px de espessura.
- **[Accessibility — TTS]** Ausência de padronização na pronúncia semântica de valores monetários em modo normal para sintetizadores de voz (`EXPERIENCE.md` § L180–185). *Fix:* Especificar rótulo `Semantics(label: "X reais e Y centavos")` em modo normal.
- **[Regulatory — Engenharia Financeira]** Omissão de regra visual e aritmética para empresas com LPA ou VPA negativos na fórmula de Graham (`DESIGN.md` § L486–487; `EXPERIENCE.md` § L139, L253). *Fix:* Exibir badge informativo *"Fórmula de Graham Inaplicável (Empresa com Prejuízo ou PL Negativo)"* e fixar multiplicador em 1.00x.
- **[Regulatory — LGPD / Marco Civil]** Promessa de expurgo total sem menção à retenção legal obrigatória de registros de conexão por 6 meses pelo Marco Civil da Internet (`EXPERIENCE.md` § L231–235). *Fix:* Incluir na microcópia do modal de exclusão a ressalva do art. 15 da Lei 12.965/2014.
- **[Regulatory — LGPD Art. 14]** Falta de declaração de representação/guarda legal para dados de dependentes menores de idade no seletor Multi-CPF (`EXPERIENCE.md` § L36, L97, L243–250). *Fix:* Inserir checkbox de declaração de guarda legal ao cadastrar CPF de menor.
- **[Regulatory — LGPD Art. 18, V]** Ausência de opção de portabilidade de dados (download JSON/CSV) antes do expurgo definitivo (`EXPERIENCE.md` § L231–235). *Fix:* Disponibilizar botão *"Exportar Meus Dados (JSON/CSV)"* no painel de conta.

---

### Low (7)

- **[Rubric — Token]** Token `{colors.semantic-crimson-border}` definido no YAML mas não citado nominalmente na prosa de `## Colors` (`DESIGN.md` § L44, L403). *Fix:* Adicionar menção textual como borda sutil de alertas carmesim.
- **[Rubric — Token]** Chaves `borderDark` e `foregroundDark` ausentes nos nós YAML de badges (`DESIGN.md` § L304–360). *Fix:* Espelhar chaves dark nos nós para geração declarativa de temas Flutter.
- **[Rubric — Bloat]** Leve redundância de fórmulas algébricas e adapters entre seções de `EXPERIENCE.md` (`EXPERIENCE.md` § L113–117, L195–207). *Fix:* Concentrar detalhes de infraestrutura na seção regulatória.
- **[Accessibility]** Omissão cosmética de `borderDark` em `risk-ceiling-saturation-card` (`DESIGN.md` § L253). *Fix:* Adicionar propriedade no frontmatter YAML.
- **[Accessibility]** Ausência de anúncio auditivo assíncrono via `SemanticsService.announce` ao concluir o cálculo do GPS de Aporte (`EXPERIENCE.md` § L188). *Fix:* Disparar anúncio de voz na finalização do cálculo.
- **[Regulatory]** Expansão de termos proibidos indiretos no guardrail léxico do Kerno AI (`EXPERIENCE.md` § L110, L229). *Fix:* Enriquecer regex com termos como `sobreponder|subponder|imperdivel|lucro certo`.
- **[Regulatory]** Transparência e consumo auditável em tempo real do contador de vagas do Programa Founders (`EXPERIENCE.md` § L32, L315, L320). *Fix:* Vincular contador da landing page ao endpoint `/api/v1/founders/slots`.

---

## Reviewer files

- [review-rubric.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/review-rubric.md) — Rubric Walker (Pass 1 Cobertura Mecânica & Pass 2 Julgamento)
- [review-accessibility.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/review-accessibility.md) — Auditoria WCAG 2.1 AA & Ergonomia Mobile
- [review-regulatory.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/review-regulatory.md) — Auditoria Regulatória CVM (Res. 19/20) & Privacidade LGPD
