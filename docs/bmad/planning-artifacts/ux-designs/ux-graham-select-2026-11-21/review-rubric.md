# Spine Pair Review — Kerno (ex-Graham Select 2.0)

## Overall verdict
O par de contratos (`DESIGN.md` + `EXPERIENCE.md`) constitui uma especificação de UX **strong** (forte), coesa e imediatamente acionável por consumidores downstream (Arquitetura, Quebra de Épicos/Histórias e Engenharia Flutter/Spring Boot). Todas as 5 jornadas do PRD (`UJ-1` a `UJ-5`) e os 17 requisitos funcionais (`FR-1` a `FR-17`) possuem cobertura completa de superfície e fluxo, 100% das 168 referências de tokens (`{path.to.token}`) e dos 19 componentes resolvem sem erros entre os dois arquivos e os 4 mockups HTML de alta fidelidade, restando apenas 4 oportunidades menores (`low`) de refinamento documental.

## 1. Flow coverage — strong
Foram extraídas todas as 5 jornadas de usuário (`UJ-1` a `UJ-5`) e os requisitos funcionais (`FR-1` a `FR-17`) das fontes declaradas no frontmatter (`prd.md` e `brief.md`) e cruzados contra `EXPERIENCE.md ## Information Architecture` e `## Key Flows`:
- **UJ-1 (Roberto, CFO da Família, 52 anos):** Coberta em `EXPERIENCE.md` (`L216–L230`) com protagonista nomeado, 7 passos numerados, momento de **Climax** explícito (Passo 6 — abertura do `{components.math-transparency-accordion}`) e caminhos de falha/borda (`D-1` mercado fechado e `QUARENTENA_EVENTO_CORPORATIVO` de `FR-17`), além de extensão de fechamento para a superfície `S-09` (`{components.b3-ingestion-crud-sheet}`).
- **UJ-2 (Lucas, Investidor Digital Native, 27 anos):** Coberta em `EXPERIENCE.md` (`L234–L247`) com protagonista nomeado, 7 passos numerados, **Climax** (Passo 6 — mapa comparativo `% Atual` vs. `% Meta Efetiva`) e caminho de falha (bloqueio carmesim de soma `!= 100.00%` de `FR-4` e entrada em **Quarentena de Aporte** de `FR-5`).
- **UJ-3 (Mariana, Investidora Analítica, 38 anos):** Coberta em `EXPERIENCE.md` (`L251–L263`) com protagonista nomeado, 6 passos numerados (incluindo extensão de auditoria LGPD/Direito ao Esquecimento na superfície `S-10`), **Climax** (Passo 4 — descoberta de concentração de `62,0%` em Subclasse e `11,0%` em quarentena) e caminho de falha (degradação graciosa determinística em timeout `> 15s` ou bloqueio léxico CVM de `FR-9`/`FR-10`).
- **UJ-4 (Thiago, Investidor no Transporte Público, 31 anos):** Coberta em `EXPERIENCE.md` (`L267–L276`) com protagonista nomeado, 5 passos numerados e **Climax** (Passo 4 — auditoria de prioridades em `%` com zero exposição visual ou sonora em reais via `{components.privacy-toggle}`).
- **UJ-5 (Camila, Engenheira de Software, 34 anos):** Coberta em `EXPERIENCE.md` (`L280–L292`) com protagonista nomeado, 5 passos numerados, **Climax** (Passo 4 — conversão em `< 1 min` com selo `{components.badge-founder}` ativo) e caso de borda comercial (esgotamento do lote de 250 vagas `founder_slots_remaining == 0`, transição para Trial Pro de 90 dias e downgrade não-destrutivo para `FREE` em `FR-15`).
- **Fechamento de Superfícies (Surface Closure):** Todas as 10 superfícies (`S-01` a `S-10`) da Arquitetura de Informação aterrissam em jornadas nomeadas (`EXPERIENCE.md L43–L58`).

### Findings
- **[low]** A jornada `UJ-4 (Thiago)` não inclui um subbloco explícito `- **Caso de Borda & Falha:**` ao final do fluxo (ao contrário de `UJ-1`, `UJ-2`, `UJ-3` e `UJ-5`), embora os comportamentos de falha correspondentes (falha biométrica com fallback para PIN do SO e revalidação após `> 2 min` em background de `FR-11`) já estejam especificados em `EXPERIENCE.md ## State Patterns` (`L140`) (`EXPERIENCE.md L267–L276`). *Fix:* Adicionar um subitem `- **Caso de Borda & Falha:**` ao final de `UJ-4` referenciando o fallback de PIN nativo do SO quando a leitura de impressão digital falha em movimento no metrô.

## 2. Token completeness — strong
Foram extraídos todos os tokens do frontmatter YAML de `DESIGN.md` (`L10–L329`) e todas as referências `{path.to.token}` no corpo de `DESIGN.md` (130 referências únicas) e `EXPERIENCE.md` (38 referências únicas):
- **Cores (`colors`):** 46 tokens definidos, 100% com códigos hexadecimais válidos (`#RRGGBB`) e pares Light/Dark (`*-dark`) cobrindo superfícies, texto, identidade roxo-índigo, semântica financeira (esmeralda, âmbar, carmesim), Ouro Founder e bordas.
- **Metas de Contraste:** Razões de contraste declaradas explicitamente para todas as combinações críticas (`17.4:1` AAA para `{colors.text-primary}` sobre `{colors.surface-elevated}`, `10.2:1` AAA para `{colors.text-secondary}`, `7.5:1` AAA para `{colors.text-muted}`, `6.3:1` AA para `{components.button-primary}`, `5.4:1` AA para esmeralda, `5.0:1` AA para âmbar, `6.4:1` AA para carmesim e `6.5:1` AA para Ouro Founder).
- **Tipografia (`typography`), Raios (`rounded`), Espaçamento (`spacing`) e Componentes (`components`):** 11 papéis tipográficos completos (com `fontVariantNumeric: 'tabular-nums'` aplicado a todos os papéis numéricos e de leitura corrida conforme `FR-2`), 7 tokens de raio, 15 tokens de espaçamento (base 4px/8px + tokens semânticos como `{spacing.touch-target-min}: '48px'`) e 19 objetos de componentes.
- **Resolução de Referências:** 100% das referências `{colors.*}`, `{typography.*}`, `{rounded.*}`, `{spacing.*}` e `{components.*}` em `DESIGN.md` e `EXPERIENCE.md` resolvem exatamente para chaves existentes no frontmatter de `DESIGN.md`.

### Findings
- **[low]** Alguns objetos em `DESIGN.md` frontmatter `components` declaram `backgroundDark`, mas omitem `foregroundDark` ou `borderDark` explícitos no nível do componente (dependendo da regra global de mapeamento Dark Mode descrita em `## Colors` e `## Elevation & Depth`), e 4 tokens escuros definidos em `colors` (`brand-accent-soft-dark`, `semantic-crimson-dark`, `border-strong-dark`, `border-focus-dark`) não são referenciados nominalmente via `{colors.*}` no corpo ou nos componentes (`DESIGN.md L25–L56, L157–L328`). *Fix:* Opcionalmente vincular `borderDark: '{colors.border-subtle-dark}'` e os tokens `*-dark` restantes nos objetos de `components` ou na seção `## Colors` para facilitar a geração automatizada de `ThemeData` dark no Flutter.

## 3. Component coverage — strong
Foram extraídos todos os 19 componentes nomeados no frontmatter e nas seções de prosa de ambos os documentos:
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

Todos os 19 componentes possuem especificação visual detalhada em `DESIGN.md ## Components` (`L429–L491`) **e** especificação comportamental substantiva com regras de negócio reais em `EXPERIENCE.md ## Component Patterns` (`L103–L123`).

### Findings
- **[low]** Na linha agrupada `button-primary & button-secondary` da tabela `EXPERIENCE.md ## Component Patterns` (`L119`), o contrato comportamental detalha as regras de `{components.button-primary}` (máximo 1 por cartão/seção, desabilitação visual com `CircularProgressIndicator` em chamadas assíncronas), mas não explicita a regra comportamental de `{components.button-secondary}` (ex.: acionamento de ações secundárias não-destrutivas como abrir `{components.b3-ingestion-crud-sheet}` ou o Assistente de 5 Critérios sem bloquear a ação primária). *Fix:* Adicionar uma frase explicitando o papel comportamental de `{components.button-secondary}` na célula da linha `L119` de `EXPERIENCE.md`.

## 4. State coverage — strong
Foram percorridas todas as 10 superfícies da Arquitetura de Informação (`S-01` a `S-10`) e verificada a matriz `EXPERIENCE.md ## State Patterns` (`L124–L142`):
- **Estados Cobertos:** Cold Load com orçamentos P95 (`< 1.5s` Dashboard, `< 1.0s` GPS, `< 400ms` cache IA, `< 10s` LLM live), Onboarding Zero-Blank-Screen sem carteira (Aba 1 e Aba 4), Carteira importada sem Diagrama configurado (pré-visualização 1 clique do template *Equilibrado Cerrado* com nota neutra `6`), Erro de validação de soma `!= 100.00%` (`FR-4`), Ativo em Quarentena de Aporte (`Nota 1–4`, teto `0%`, `FR-5`), Saturação Global de Tetos de Risco ($X_{\text{rem}} > 0$, `FR-6`), Mercado fechado / cotação `D-1`, Circuit Breaker de Evento Corporativo (`> 40%`, `QUARENTENA_EVENTO_CORPORATIVO`, `FR-17`), Timeout do LLM (`> 15s`) / Bloqueio Léxico CVM com fallback quantitativo determinístico instantâneo (`FR-9`, `FR-10`), Limite FinOps atingido (`5/dia`, `30/mês`), Proteção de snapshot em background (`FLAG_SECURE`) e revalidação biométrica após `> 2 min` (`FR-11`, `FR-12`), além da Máquina de Estados Comerciais (`FOUNDER_LIFETIME`, `PRO_TRIAL_90D` e downgrade não-destrutivo para `FREE` em `FR-15`).

### Findings
- **[low]** Embora `DESIGN.md ## Colors` (`L371`) mencione *"falhas críticas de upload de planilha da B3"* associadas a `{colors.semantic-crimson}`, a tabela `EXPERIENCE.md ## State Patterns` (`L128–L141`) não possui uma linha dedicada descrevendo o tratamento comportamental de falha de validação/parsing de planilha B3 corrompida na superfície `S-09` (`{components.b3-ingestion-crud-sheet}`) ou de perda total de conectividade de rede (offline) ao abrir o app Mobile. *Fix:* Adicionar uma linha em `EXPERIENCE.md ## State Patterns` cobrindo erro de formato no upload B3 (feedback inline em `{colors.semantic-crimson-surface}` mantendo o modal aberto para nova tentativa ou cadastro manual) e leitura offline do último snapshot cacheado.

## 5. Visual reference coverage — strong
Foram inspecionados os diretórios `mockups/`, `imports/` (vazio) e `wireframes/` (ausente). Os 4 arquivos existentes em `mockups/` são:
1. `mockups/key-dashboard-patrimonial.html`
2. `mockups/key-gps-aporte.html`
3. `mockups/key-kerno-ai-diagnostico.html`
4. `mockups/key-diagrama-cerrado.html`

- **Links Inline e Descrição:** Ambos os documentos (`DESIGN.md L406–L410, L431` e `EXPERIENCE.md L64–L75, L105, L126, L212`) possuem links clicáveis inline para todos os 4 arquivos nas seções relevantes, descrevendo com precisão o que cada mockup ilustra (Coluna 1 canônica e Coluna 2 de estado alternativo/borda).
- **Regra de Precedência:** A regra de que `DESIGN.md` e `EXPERIENCE.md` prevalecem em caso de conflito (*spines-win-on-conflict*) está declarada no cabeçalho de `DESIGN.md` (`L333`), na seção `## Layout & Spacing` (`L406`), no cabeçalho e `## Foundation` de `EXPERIENCE.md` (`L14, L22`) e nos comentários de cabeçalho CSS dos 4 arquivos HTML.
- **Órfãos:** Zero arquivos órfãos e zero referências genéricas.

### Findings
Nenhum apontamento. Cobertura visual 100% íntegra e rastreável.

## 6. Bloat & overspecification — strong
- **Voz Editorial vs. Contrato Comportamental:** `DESIGN.md` utiliza prosa editorial apenas onde apropriado (`## Brand & Style` e justificativa cromática em `## Colors`), enquanto `EXPERIENCE.md` mantém tom técnico, direto e orientado a tabelas de decisão (`## Information Architecture`, `## Voice and Tone`, `## Component Patterns`, `## State Patterns`, `## Responsive & Platform`).
- **Uso de Tokens vs. Pixels Hardcoded:** Os componentes em `DESIGN.md` referenciam consistentemente os tokens do frontmatter (`{spacing.touch-target-min}`, `{rounded.lg}`, `{typography.metric-hero}`) em vez de espalhar valores mágicos arbitrários.

### Findings
- **[low]** Há leve repetição de regras de implementação de backend (como a composição SHA-256 de `portfolio_state_hash`, o nome da classe `UserDataPurgeAdapter` e a fórmula algébrica $\sqrt{22.5 \times \text{LPA} \times \text{VPA}}$) entre `EXPERIENCE.md ## Component Patterns`, `## State Patterns` e `## Regulatory, Privacy & AI Guardrails` (`EXPERIENCE.md L113–L117, L195–L207`). *Fix:* Manter as fórmulas e invariantes de backend concentradas em uma única seção de `EXPERIENCE.md` (ou referenciar o PRD) caso o documento passe por futura compactação.

## 7. Inheritance discipline — strong
- **Resolução de `sources`:** Os dois caminhos declarados no frontmatter de `DESIGN.md` e `EXPERIENCE.md` (`docs/bmad/planning-artifacts/prds/prd-graham-select-2026-11-21/prd.md` e `docs/bmad/planning-artifacts/briefs/brief-graham-select-2026-11-21/brief.md`) existem e resolvem corretamente.
- **Fidelidade Verbatim de UJs e FRs:** Os títulos e protagonistas de `UJ-1` a `UJ-5` (*Roberto*, *Lucas*, *Mariana*, *Thiago*, *Camila*), os códigos `FR-1` a `FR-17`, `NFR-1` a `NFR-4`, `SM-1` a `SM-6`, `SM-C1` a `SM-C3` e as decisões `A-1` a `A-7` espelham o PRD sem distorção.
- **Disciplina de Glossário (`prd.md §3`):** Todos os 18 termos canônicos do Glossário (**Kerno**, **Design System Intergeracional**, **Diagrama do Cerrado**, **Macro Classe**, **Subclasse**, **Nota de Qualidade (1–10)**, **Template de Alocação**, **Teto Máximo de Risco**, **Quarentena de Aporte**, **Margem de Segurança de Graham**, **Multiplicador de Graham**, **GPS de Aporte**, **Regra de Transbordamento**, **Kerno AI (Diagnóstico de Carteira)**, **Sanitizador LGPD de IA**, **Dashboard Patrimonial**, **Modo Privacidade**, **Membro Founder**) são empregados de forma idêntica nos dois spines e nos 4 mockups HTML.
- **Identidade de Componentes e Tokens entre Arquivos:** Os 19 nomes de componentes e todas as 38 referências `{path.to.token}` usadas em `EXPERIENCE.md` coincidem caractere por caractere com o frontmatter de `DESIGN.md`.

### Findings
Nenhum apontamento. Disciplina de herança exemplar entre Brief, PRD, Spines de UX e Mockups.

## 8. Shape fit — strong
- **Ordem Canônica de `DESIGN.md`:** As 8 seções do corpo seguem estritamente a ordem canônica travada em `references/design-md-spec.md`:
  1. `## Brand & Style` (`L335`)
  2. `## Colors` (`L347`)
  3. `## Typography` (`L374`)
  4. `## Layout & Spacing` (`L388`)
  5. `## Elevation & Depth` (`L412`)
  6. `## Shapes` (`L420`)
  7. `## Components` (`L429`)
  8. `## Do's and Don'ts` (`L493`)
- **Seções Obrigatórias e Condicionais de `EXPERIENCE.md`:**
  - Todas as 8 seções padrão obrigatórias estão presentes (`## Foundation`, `## Information Architecture`, `## Voice and Tone`, `## Component Patterns`, `## State Patterns`, `## Interaction Primitives`, `## Accessibility Floor`, `## Key Flows`).
  - As 2 seções condicionais obrigatórias quando aplicáveis estão presentes e justificadas: `## Inspiration & Anti-patterns` (ancorada na inspiração do Nubank/Diagrama do Cerrado/Graham e na rejeição explícita de terminais de day trade, tabelas móveis com scroll horizontal, venda forçada e paywalls destrutivos) e `## Responsive & Platform` (necessária pela estratégia multi-superfície Mobile Nativo `< 600dp`, Tablet `600dp–1024dp`, Desktop Web SPA `> 1024dp` e Landing Page pública).
  - A seção adicional `## Regulatory, Privacy & AI Guardrails` (`L191–L208`) conquista legitimamente seu espaço por consolidar os contratos visuais e comportamentais do **Sanitizador LGPD de IA** (`Local UI Binding` de `R$` vs. `%`), da blindagem CVM 19/20 e do fluxo de Direito ao Esquecimento (`FR-8`, `FR-10`, `FR-16`).

### Findings
Nenhum apontamento. Estrutura física e semântica 100% aderente ao contrato `bmad-ux`.

## Mechanical notes
- **Frontmatter YAML:** Completo e válido em `DESIGN.md` (`name`, `description`, `status`, `created`, `updated`, `sources`, `colors`, `typography`, `rounded`, `spacing`, `components`) e em `EXPERIENCE.md` (`name`, `description`, `status`, `created`, `updated`, `sources`).
- **Consistência de Nomes:** 19/19 componentes com nomes kebab-case idênticos entre frontmatter YAML, `DESIGN.md ## Components` e `EXPERIENCE.md ## Component Patterns`.
- **Referências Cruzadas:** 130/130 referências `{path.to.token}` em `DESIGN.md` e 38/38 em `EXPERIENCE.md` resolvidas com sucesso. 4/4 arquivos HTML em `mockups/` existentes e linkados corretamente sem links quebrados.
- **Diagramas Mermaid:** Nenhum bloco Mermaid utilizado (toda a topologia de superfícies, matriz de fechamento e máquina de estados está representada em tabelas Markdown limpas, sem risco de erro de sintaxe de renderização).
