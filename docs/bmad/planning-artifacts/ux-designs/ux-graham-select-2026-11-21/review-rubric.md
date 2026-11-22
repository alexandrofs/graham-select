# Spine Pair Review — graham-select

## Overall verdict
O par de contratos de especificação de UX (`DESIGN.md` e `EXPERIENCE.md`) atinge a classificação máxima **strong** (forte), constituindo um contrato de interface robusto, detalhado e imediatamente acionável para os consumidores downstream (Arquitetura Técnica, Decomposição de Épicos/Histórias e Engenharia Flutter/Spring Boot). Todas as 5 jornadas do usuário (`UJ-1` a `UJ-5`) e os 17 requisitos funcionais (`FR-1` a `FR-17`) possuem cobertura integral com protagonistas nomeados, passos numerados, clímax e tratamentos de falha, enquanto 100% das 178 referências a tokens (`{path.to.token}`), dos 19 componentes de design system e dos 6 mockups HTML de alta fidelidade resolvem perfeitamente sem referências órfãs. Os 3 apontamentos remanescentes são estritamente de severidade `low` (polimento de tokens escuros secundários e leve redundância de fórmulas algébricas de backend).

## 1. Flow coverage — strong
Foram extraídas todas as 5 jornadas de usuário (`UJ-1` a `UJ-5`) e os 17 requisitos funcionais (`FR-1` a `FR-17`) do PRD e do Brief upstream (`prd.md` e `brief.md`) e cruzados contra a Arquitetura de Informação e a seção `## Key Flows` de `EXPERIENCE.md`:
- **UJ-1 (Roberto, CFO da Família, 52 anos):** Coberta em `EXPERIENCE.md` (`L231–L251`) com protagonista nomeado, 7 passos numerados, clímax no Passo 6 (expansão do `{components.math-transparency-accordion}` revelando desvio de `4,2%` e Margem Graham de `28%`), caminhos de borda/falha detalhados (cotações `D-1` e `QUARENTENA_EVENTO_CORPORATIVO` de `FR-17`) e extensão de fechamento para a superfície `S-09` (`{components.b3-ingestion-crud-sheet}`).
- **UJ-2 (Lucas, Investidor Digital Native, 27 anos):** Coberta em `EXPERIENCE.md` (`L253–L271`) com protagonista nomeado, 7 passos numerados, clímax no Passo 6 (comparação visual `% Atual` vs. `% Meta Efetiva`), bloqueio imediato com feedback carmesim para somas `!= 100.00%` (`FR-4`) e ativação da **Quarentena de Aporte** (`FR-5`).
- **UJ-3 (Mariana, Investidora Analítica, 38 anos):** Coberta em `EXPERIENCE.md` (`L273–L293`) com protagonista nomeado, 6 passos numerados, clímax no Passo 4 (identificação de `62,0%` de concentração em Subclasse e `11,0%` em quarentena), degradação determinística instantânea em timeout `> 15s` / bloqueio léxico CVM (`FR-9`, `FR-10`) e extensão de auditoria de privacidade/Direito ao Esquecimento na superfície `S-10`.
- **UJ-4 (Thiago, Investidor no Transporte Público, 31 anos):** Coberta em `EXPERIENCE.md` (`L295–L306`) com protagonista nomeado, 5 passos numerados, clímax no Passo 4 (auditoria de prioridades em `%` com zero exposição visual ou sonora de cifras em reais via `{components.privacy-toggle}`) e tratamento de borda/falha explícito (falha na leitura de impressão digital por trepidação do metrô com fallback para PIN do SO e bloqueio biométrico após `> 2 min` de inatividade conforme `FR-11`).
- **UJ-5 (Camila, Engenheira de Software, 34 anos):** Coberta em `EXPERIENCE.md` (`L308–L321`) com protagonista nomeado, 5 passos numerados, clímax no Passo 4 (conversão em `< 1 min` via Google OAuth2 com ativação do `{components.badge-founder}`) e caso de borda comercial para coortes pós-Founders (esgotamento de `founder_slots_remaining`, ativação do Trial Pro de 90 dias e downgrade não-destrutivo para `FREE` conforme `FR-15`).
- **Fechamento Integral de Superfícies:** Todas as 10 superfícies da Arquitetura de Informação (`S-01` a `S-10`) aterrissam formalmente em pelo menos uma jornada na matriz de fechamento (`EXPERIENCE.md L47–L58`).

### Findings
- Nenhum apontamento bloqueante. A ausência de subbloco de falha em `UJ-4` identificada em revisões preliminares foi completamente sanada com o tratamento de fallback para PIN e revalidação de `2 minutos` (`FR-11`).

## 2. Token completeness — strong
Foram extraídos e validados todos os tokens declarados no frontmatter YAML de `DESIGN.md` (`L10–L361`) e todas as referências no padrão `{path.to.token}` no corpo de `DESIGN.md` (136 referências únicas) e `EXPERIENCE.md` (42 referências únicas):
- **Cores (`colors`):** 46 tokens definidos com códigos hexadecimais de 6 dígitos (`#RRGGBB`), com pares completos Light/Dark (`*-dark`) cobrindo superfícies, tipografia, identidade Roxo-Índigo, semântica financeira (esmeralda, âmbar, carmesim), Ouro Founder e bordas.
- **Auditoria de Contraste:** Relações de contraste calculadas e documentadas expressamente para todas as combinações críticas: `17.4:1` AAA para `{colors.text-primary}` sobre `{colors.surface-elevated}`, `10.2:1` AAA para `{colors.text-secondary}`, `7.5:1` AAA para `{colors.text-muted}`, `6.3:1` AA para `{components.button-primary}`, `7.68:1` / `7.13:1` AAA para esmeralda (Light) e `7.45:1` AAA (Dark), `7.05:1` AAA para âmbar (Light) e `8.80:1` AAA (Dark), `7.60:1` AAA para carmesim (Light) e `7.92:1` AAA (Dark), `6.37:1` AA para Ouro Founder, e `>= 3.0:1` para bordas não-textuais (WCAG 1.4.11).
- **Tipografia (`typography`):** 11 papéis tipográficos estruturados (Plus Jakarta Sans para Display/Headings e Inter para corpo e dados). O atributo `fontVariantNumeric: 'tabular-nums'` está ativado em todos os papéis de dados e leitura corrida (`FR-2`), com regras de honra ao `textScaler` (`1.0x` a `2.0x`) e banimento estrito de `TextOverflow.ellipsis`.
- **Raios (`rounded`) e Espaçamentos (`spacing`):** 7 tokens de curvatura e 15 tokens de espaçamento na escala 4px/8px, incluindo o token ergonômico `{spacing.touch-target-min}: '48px'`.
- **Resolução de Tokens:** 100% das referências `{colors.*}`, `{typography.*}`, `{rounded.*}`, `{spacing.*}` e `{components.*}` resolvem diretamente no frontmatter YAML de `DESIGN.md`.

### Findings
- **[low]** O token `{colors.semantic-crimson-border}` (`#FECACA`) está definido no frontmatter YAML de `DESIGN.md` (`L44`), mas não é referenciado nominalmente na prosa descritiva de `## Colors` nem nos objetos de componentes (embora seu par esmeralda `{colors.semantic-emerald-border}` e âmbar `{colors.semantic-amber-border}` sejam referenciados em badges) (`DESIGN.md L44, L403`). *Fix:* Adicionar uma menção a `{colors.semantic-crimson-border}` como borda sutil de cartões de erro ou validação na seção `## Colors`.
- **[low]** Certos componentes de badge (`badge-graham-bonus`, `badge-quarantine`, `badge-founder`) e o `bottom-nav-bar` no frontmatter YAML de `DESIGN.md` (`L304–L360`) omitem chaves explícitas `borderDark` ou `foregroundDark` no nível do objeto, recorrendo às regras globais de mapeamento Dark Mode especificadas em `## Colors` e `## Elevation & Depth` (`DESIGN.md L304–L360`). *Fix:* Opcionalmente espelhar as chaves `borderDark` e `foregroundDark` diretamente nos nós YAML desses componentes para simplificar a geração declarativa de temas Flutter.

## 3. Component coverage — strong
Foram extraídos e confrontados todos os 19 componentes de UI declarados no frontmatter de `DESIGN.md`:
1. `wealth-hero-card`
2. `macro-class-distribution-card`
3. `contribution-gps-card`
4. `math-transparency-accordion`
5. `risk-ceiling-saturation-card`
6. `cerrado-template-selector-card`
7. `cerrado-quality-card`
8. `kerno-ai-diagnostic-card`
9. `b3-ingestion-crud-sheet`
10. `button-primary`
11. `button-secondary`
12. `input-currency`
13. `badge-graham-bonus`
14. `badge-quarantine`
15. `badge-founder`
16. `privacy-toggle`
17. `multi-cpf-selector`
18. `cvm-regulatory-footer`
19. `bottom-nav-bar`

- **Cobertura em `DESIGN.md`:** Todos os 19 componentes possuem subseções dedicadas em `DESIGN.md ## Components` (`L448–L538`), detalhando raio, cores, contraste, fontes e hierarquia visual.
- **Cobertura em `EXPERIENCE.md`:** Todos os 19 componentes possuem linhas na tabela `EXPERIENCE.md ## Component Patterns` (`L103–L123`), com regras comportamentais substanciais (invariantes de layout, prevenção de duplo envio, debounce, mascaramento e acessibilidade). A regra comportamental de `{components.button-secondary}` foi devidamente integrada na linha de botões.

### Findings
- Nenhum apontamento. Cobertura de componentes 100% simétrica e completa.

## 4. State coverage — strong
A matriz de estados em `EXPERIENCE.md ## State Patterns` (`L125–L143`) percorre de forma exaustiva as 10 superfícies da Arquitetura de Informação (`S-01` a `S-10`) e contempla todos os estados de ciclo de vida essenciais:
- **Loading / Cold-Load:** Skeletons geométricos em `{colors.surface-subtle}` com raios correspondentes e metas rígidas de latência P95 (`< 1.5s` Dashboard, `< 1.0s` GPS para até 100 ativos, `< 400ms` cache IA, `< 10s` Gemini live) (`NFR-1` a `NFR-3`).
- **Empty / Onboarding (*Zero-Blank-Screen*):** Cartão de boas-vindas na Aba 1 com CTAs duplos (Upload B3 e Cadastro Manual) e exibição imediata dos 3 templates do Diagrama do Cerrado na Aba 4 (`FR-4`).
- **Guided Preview:** Estado de carteira importada sem metas salvas, aplicando preview com o template *Equilibrado Cerrado* e nota neutra 6.
- **Validation Errors:** Erro de soma de Macro Classes (`!= 100.00%`) com barra em `{colors.semantic-crimson-surface}` e bloqueio prévio do botão salvar (`FR-4`).
- **Quarantine & Protective Containment:** Slider com nota `< 5` disparando `{components.badge-quarantine}`, teto `0%` e exclusão do denominador (`FR-5`), além de isolamento visual no GPS de Aporte.
- **Risk Ceiling Saturation:** Cartão `{components.risk-ceiling-saturation-card}` exibindo saldo excedente protegido ($X_{\text{rem}} > 0$) quando todos os ativos atingem o teto (`FR-6`).
- **Stale / Market Closed:** Suporte a cotações de fechamento (`D-1`) com pílula informativa âmbar discreta sem bloquear decisões (`UJ-1`).
- **Corporate Action Circuit Breaker:** Variação abrupta `> 40%` congelando a cotação em `QUARENTENA_EVENTO_CORPORATIVO` com ação para o usuário validar cotas (`FR-17`).
- **LLM Timeout & Regulatory Fallback:** Degradação graciosa instantânea em caso de timeout `> 15s`, erro de provedor ou bloqueio léxico CVM, preenchendo os 4 cartões pelo Motor Quantitativo Determinístico sem tela de erro ou consumo de cota (`FR-9`, `FR-10`).
- **FinOps Quota Limits:** Exibição do último diagnóstico cacheado (`portfolio_state_hash`) com dados numéricos live ao atingir `5/dia` ou `30/mês`.
- **Upload B3 Errors & Offline Mode:** Alerta inline carmesim no modal `{components.b3-ingestion-crud-sheet}` indicando coluna inválida sem fechar o diálogo, e abertura do app Mobile offline renderizando o último snapshot local sincronizado.
- **Background Privacy & Timeout:** Máscara nativa `FLAG_SECURE` / overlay iOS no carrossel de apps recentes e exigência de biometria ao retornar após `> 2 minutos` (`FR-11`, `FR-12`).
- **Commercial Transitions:** Matriz completa de permissões entre `FOUNDER_LIFETIME`, `PRO_TRIAL_90D` e downgrade não-destrutivo para `FREE` (`FR-15`).

### Findings
- Nenhum apontamento. Matriz de estados exemplar, cobrindo todos os cenários normais, anômalos e regulatórios.

## 5. Visual reference coverage — strong
Foram auditados os diretórios `mockups/`, `imports/` (vazio) e os vínculos em ambos os contratos:
- **Arquivos Físicos Presentes:** Existem 6 arquivos HTML autossuficientes em `mockups/`:
  1. `mockups/key-dashboard-patrimonial.html` (Mobile Dashboard & Modo Privacidade)
  2. `mockups/key-gps-aporte.html` (Mobile GPS de Aporte & Explicabilidade Matemática)
  3. `mockups/key-kerno-ai-diagnostico.html` (Mobile Diagnóstico IA & Degradação Graciosa)
  4. `mockups/key-diagrama-cerrado.html` (Mobile Diagrama do Cerrado, Trava 100% e Quarentena)
  5. `mockups/key-web-landing-founders.html` (Web Landing Page Pública & Programa Founders)
  6. `mockups/key-web-desktop-restyling.html` (Web Desktop Restyling de 100% das 18 rotas brownfield)
- **Links e Descrições Inline:** Ambos os documentos (`DESIGN.md L439–L445` e `EXPERIENCE.md L64–L75`) contêm links markdown clicáveis para todos os 6 arquivos, descrevendo com exatidão a superfície representada e os estados alternativos exibidos na 2ª coluna.
- **Regra de Precedência (*Spines Win on Conflict*):** Declarada de forma unívoca em `DESIGN.md` (`L365, L439`), `EXPERIENCE.md` (`L14, L22`) e nos comentários de cabeçalho CSS dos 6 arquivos HTML de mockup.
- **Zero Órfãos:** Todos os arquivos de mockups estão referenciados nos dois documentos, sem referências quebradas ou arquivos não inventariados.

### Findings
- Nenhum apontamento. Cobertura visual 100% documentada e rastreável.

## 6. Bloat & overspecification — strong
- **Disciplina de Tokens vs. Pixels Arbitrários:** O documento `DESIGN.md` utiliza consistentemente os tokens de espaçamento (`{spacing.card-padding-mobile}`, `{spacing.section-gap}`, `{spacing.touch-target-min}`), raios (`{rounded.lg}`, `{rounded.xl}`) e papéis tipográficos do frontmatter, evitando dimensões mágicas soltas.
- **Densidade de Informação:** `EXPERIENCE.md` privilegia tabelas analíticas para a Arquitetura de Informação, fechamento de superfícies, padrões de componentes, matriz de estados e regras responsivas, reservando prosa apenas para os fluxos narrativos e salvaguardas regulatórias.
- **Separação de Preocupações:** `DESIGN.md` foca estritamente em propriedades visuais (*como se parece*), enquanto `EXPERIENCE.md` estabelece a governança comportamental (*como funciona*).

### Findings
- **[low]** Há leve redundância de fórmulas e regras de engenharia de backend (ex.: menção à classe `UserDataPurgeAdapter`, composição SHA-256 de `portfolio_state_hash` e expressão $\sqrt{22.5 \times \text{LPA} \times \text{VPA}}$) repetidas em `## Component Patterns`, `## State Patterns` e `## Regulatory, Privacy & AI Guardrails` de `EXPERIENCE.md` (`L113–L117, L195–L207`). *Fix:* Em futuras rodadas de compactação, concentrar as fórmulas algébricas e detalhes de adapters de infraestrutura na seção regulatória ou remeter diretamente ao PRD.

## 7. Inheritance discipline — strong
- **Integridade das Fontes Upstream:** Os caminhos referenciados nos frontmatters (`docs/bmad/planning-artifacts/prds/prd-graham-select-2026-11-21/prd.md` e `docs/bmad/planning-artifacts/briefs/brief-graham-select-2026-11-21/brief.md`) existem e estão com status sincronizado.
- **Fidelidade às Jornadas e Requisitos:** Os nomes e papéis dos 5 protagonistas (*Roberto*, *Lucas*, *Mariana*, *Thiago*, *Camila*), os 17 requisitos funcionais (`FR-1` a `FR-17`), os 4 requisitos não-funcionais (`NFR-1` a `NFR-4`) e as métricas de sucesso (`SM-1` a `SM-6`) preservam alinhamento literal com o PRD.
- **Aderência ao Glossário Canônico (`prd.md §3`):** Todos os 18 termos proprietários do ecossistema (**Kerno**, **Design System Intergeracional**, **Diagrama do Cerrado**, **Macro Classe**, **Subclasse**, **Nota de Qualidade (1–10)**, **Template de Alocação**, **Teto Máximo de Risco**, **Quarentena de Aporte**, **Margem de Segurança de Graham**, **Multiplicador de Graham**, **GPS de Aporte**, **Regra de Transbordamento**, **Kerno AI (Diagnóstico de Carteira)**, **Sanitizador LGPD de IA**, **Dashboard Patrimonial**, **Modo Privacidade**, **Membro Founder**) são empregados com precisão terminológica idêntica nos dois documentos e nos mockups.
- **Simetria de Nomenclatura:** Os 19 identificadores de componentes coincidem caractere por caractere entre o frontmatter de `DESIGN.md`, o corpo de `DESIGN.md` e a tabela de padrões de `EXPERIENCE.md`.

### Findings
- Nenhum apontamento. Disciplina de herança exemplar em toda a cadeia de planejamento.

## 8. Shape fit — strong
- **Ordem Canônica em `DESIGN.md`:** O corpo do documento segue com rigor a ordem canônica exigida pelo padrão `bmad-ux`:
  1. `## Brand & Style` (`L367`)
  2. `## Colors` (`L379`)
  3. `## Typography` (`L407`)
  4. `## Layout & Spacing` (`L423`)
  5. `## Elevation & Depth` (`L449`)
  6. `## Shapes` (`L457`)
  7. `## Components` (`L466`)
  8. `## Do's and Don'ts` (`L540`)
- **Seções Mandatórias em `EXPERIENCE.md`:** Contém todas as 8 seções obrigatórias (`## Foundation`, `## Information Architecture`, `## Voice and Tone`, `## Component Patterns`, `## State Patterns`, `## Interaction Primitives`, `## Accessibility Floor`, `## Key Flows`).
- **Seções Condicionais Justificadas:** Contém e justifica plenamente as seções condicionais `## Inspiration & Anti-patterns` (referências estéticas e banimento de anti-padrões especulativos), `## Responsive & Platform` (estratégia multi-superfície Mobile Nativo, Tablet e Desktop Web SPA) e `## Regulatory, Privacy & AI Guardrails` (blindagem jurídica CVM 19/20, Sanitizador LGPD de IA e expurgo de dados).

### Findings
- Nenhum apontamento. Estrutura e organização 100% aderentes à especificação `bmad-ux`.

## Mechanical notes
- **Frontmatter YAML:** Válido, bem-formado e completo em ambos os arquivos (`DESIGN.md` com 11 chaves de primeiro nível; `EXPERIENCE.md` com 6 chaves de primeiro nível).
- **Consistência de Nomenclatura:** 19/19 componentes adotam convenção kebab-case rigorosa e idêntica em ambos os documentos.
- **Resolução de Referências Cruzadas:** 100% das 136 referências `{path.to.token}` em `DESIGN.md` e das 42 em `EXPERIENCE.md` resolvem com exatidão no frontmatter YAML de `DESIGN.md`. Todos os links relativos para `mockups/*.html` são válidos e resolvem no disco.
- **Sintaxe Mermaid:** Nenhum bloco Mermaid utilizado; a arquitetura de informação, fluxos de fechamento e matrizes de estado foram modeladas através de tabelas Markdown canônicas, eliminando riscos de falhas de renderização em visualizadores Markdown.
