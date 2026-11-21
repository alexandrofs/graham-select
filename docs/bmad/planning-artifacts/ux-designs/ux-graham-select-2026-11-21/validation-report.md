# Validation Report — Kerno (ex-Graham Select 2.0)

- **DESIGN.md:** `docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/DESIGN.md`
- **EXPERIENCE.md:** `docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/EXPERIENCE.md`
- **Run at:** 2026-11-21T20:00:00Z

## Overall verdict

O par de contratos (`DESIGN.md` + `EXPERIENCE.md`) constitui uma especificação de UX **strong** (forte), coesa e imediatamente acionável por consumidores downstream (Arquitetura, Quebra de Épicos/Histórias e Engenharia Flutter/Spring Boot). Todas as 5 jornadas do PRD (`UJ-1` a `UJ-5`) e os 17 requisitos funcionais (`FR-1` a `FR-17`) possuem cobertura completa de superfície e fluxo, 100% das referências de tokens (`{path.to.token}`) e dos 19 componentes resolvem sem erros entre os dois arquivos e os 4 mockups HTML de alta fidelidade.

As duas lentes especializadas adicionais — **Acessibilidade Intergeracional & WCAG 2.1 AA/AAA** (`review-accessibility.md`) e **Conformidade Regulatória CVM 19/20 & Privacidade LGPD** (`review-regulatory.md`) — confirmaram `0` achados críticos e `0` achados altos, identificando 2 refinamentos médios de contraste (elevação de cifras e alertas semânticos para `>= 7:1` AAA e bordas não-textuais `border-strong` para `>= 3:1` WCAG 1.4.11) e 11 refinamentos baixos, todos integralmente resolvidos e aplicados nos contratos finais (`status: final`).

## Category verdicts
- Flow coverage — strong
- Token completeness — strong
- Component coverage — strong
- State coverage — strong
- Visual reference coverage — strong
- Bloat & overspecification — strong
- Inheritance discipline — strong
- Shape fit — strong

## Findings by severity

### Critical (0)
Nenhum achado crítico.

### High (0)
Nenhum achado alto.

### Medium (2)

**[review-accessibility]** — F-01: Cifras Monetárias de Aporte (`contribution-gps-card`) e Alertas Críticos Atendem WCAG AA (`4.84:1–5.91:1`), mas Ficam Abaixo do Piso WCAG AAA (`>= 7:1`) (`DESIGN.md § Colors`, `mockups/key-*.html`)
Enquanto o saldo patrimonial principal (`{colors.text-primary}` `#0F172A` sobre `#FFFFFF`) entrega `17.85:1` (AAA), os tokens `semantic-emerald` (`#047857`, `5.48:1`), `semantic-amber` (`#B45309`, `4.84:1`) e `semantic-crimson` (`#B91C1C`, `5.91:1`) estavam na faixa AA.
Fix: Ajustar `semantic-emerald` para `#065F46` (`7.68:1` AAA sobre `#FFFFFF` e `7.13:1` AAA sobre `#ECFDF5`), `semantic-amber` para `#92400E` (`7.05:1` AAA sobre `#FFFBEB`), `semantic-crimson` para `#991B1B` (`7.60:1` AAA sobre `#FEF2F2`), `semantic-emerald-dark` para `#6EE7B7` (`7.45:1` AAA sobre `#064E3B`) e `semantic-crimson-dark` para `#FCA5A5` (`7.92:1` AAA sobre `#450A0A`). *(Resolvido e aplicado)*

**[review-accessibility]** — F-02: Contraste Não-Textual (WCAG 1.4.11 `>= 3:1`) em `border-strong` e Marcador de Meta Sobreposto em `macro-class-distribution-card` (`DESIGN.md § Colors / § Components`, `mockups/key-dashboard-patrimonial.html`)
O token `border-strong` (`#CBD5E1` sobre `#FFFFFF` = `1.48:1`) ficava abaixo de `3.0:1` para delimitar inputs/botões secundários sem foco, e o marcador de meta `#3730A3` sobre barra preenchida `#4F46E5` tinha contraste de `1.42:1`.
Fix: Ajustar `border-strong` para `#94A3B8` (`3.02:1` sobre `#FFFFFF`) e `border-strong-dark` para `#64748B` (`3.75:1` sobre `#111827`), e aplicar `{colors.text-primary}` (`#0F172A`) com halo branco de `1.5px` ao marcador de meta `barFillTarget`. *(Resolvido e aplicado)*

### Low (11)

**[Flow coverage / review-rubric]** — Subbloco explícito de Caso de Borda & Falha em `UJ-4 (Thiago)` (`EXPERIENCE.md § Key Flows`)
A jornada `UJ-4` não incluía o subbloco `- **Caso de Borda & Falha:**` ao final do fluxo.
Fix: Adicionar o subbloco referenciando o fallback para PIN nativo do SO quando a leitura biométrica falha no metrô e a revalidação após `> 2 min` em background. *(Resolvido e aplicado)*

**[Token completeness / review-rubric & review-accessibility F-03]** — Propriedades `*Dark` explícitas nos objetos de componentes (`DESIGN.md frontmatter`)
Alguns objetos em `components` declaravam `backgroundDark`, mas omitiam `foregroundDark` ou `borderDark`.
Fix: Vincular `foregroundDark` e `borderDark` nos componentes do frontmatter e referenciar todos os tokens `*-dark` na seção `## Colors`. *(Resolvido e aplicado)*

**[Component coverage / review-rubric]** — Papel comportamental explícito de `button-secondary` (`EXPERIENCE.md § Component Patterns`)
A célula agrupada `button-primary & button-secondary` não explicitava o contrato comportamental de `button-secondary`.
Fix: Explicitar que `{components.button-secondary}` aciona fluxos auxiliares não-destrutivos (como abrir `{components.b3-ingestion-crud-sheet}` ou o Assistente de 5 Critérios) sem bloquear a ação primária. *(Resolvido e aplicado)*

**[State coverage / review-rubric]** — Linha para falha de upload de planilha B3 e abertura offline (`EXPERIENCE.md § State Patterns`)
Faltava uma linha específica na tabela `## State Patterns` para falha de validação/parsing de planilha B3 e leitura offline.
Fix: Adicionar linha dedicada em `## State Patterns` cobrindo erro de formato de arquivo B3 em `{colors.semantic-crimson-surface}` e leitura offline do último snapshot cacheado. *(Resolvido e aplicado)*

**[Bloat & overspecification / review-rubric]** — Consolidação de detalhes internos de backend (`EXPERIENCE.md § Component Patterns / § Regulatory, Privacy & AI Guardrails`)
Leve repetição de detalhes internos (`portfolio_state_hash`, `UserDataPurgeAdapter`) entre seções.
Fix: Concentrar a mecânica técnica na seção `## Regulatory, Privacy & AI Guardrails` e manter as células de tabela enxutas. *(Resolvido e aplicado)*

**[review-accessibility]** — F-04: Adição de `flex-wrap: wrap` e grid auto-fit nos mockups HTML para reflow até `2.0x` (`mockups/key-*.html`)
Linhas flex nos mockups estáticos não declaravam `flex-wrap: wrap`.
Fix: Adicionar `flex-wrap: wrap; gap: 4px 8px;` e `grid-template-columns: repeat(auto-fit, minmax(145px, 1fr))` nos 4 mockups HTML. *(Resolvido e aplicado)*

**[review-accessibility]** — F-05: Inclusão de `fontVariantNumeric: 'tabular-nums'` em `heading-1..3` (`DESIGN.md frontmatter`)
Os tokens `heading-1`, `heading-2` e `heading-3` não declaravam `fontVariantNumeric: 'tabular-nums'` para títulos que contêm valores em `R$` ou `%`.
Fix: Incluir `fontVariantNumeric: 'tabular-nums'` em `heading-1`, `heading-2` e `heading-3`. *(Resolvido e aplicado)*

**[review-accessibility]** — F-06: Altura mínima de toque `48dp` nas linhas interativas do Assistente de 5 Critérios Objetivos (`DESIGN.md § Components`, `mockups/key-diagrama-cerrado.html`)
As linhas `.criteria-item` no mockup estavam compactas (`~18px`).
Fix: Especificar e estilizar `.criteria-item` com `min-height: 48px` e controles de pontuação `0 / 1 / 2 pts` acessíveis ao toque. *(Resolvido e aplicado)*

**[review-accessibility]** — F-07: Atributos `aria-label` em todos os `R$ ••••••` e `role="slider"` nos sliders (`mockups/key-dashboard-patrimonial.html`, `mockups/key-diagrama-cerrado.html`)
Apenas o saldo principal possuía `aria-label` no mockup do Modo Privacidade, e os thumbs de slider não declaravam `role="slider"`.
Fix: Adicionar `aria-label="Valor monetário oculto pelo Modo Privacidade"` em todas as ocorrências de `R$ ••••••` e `role="slider" aria-valuemin="1" aria-valuemax="10" aria-valuenow="..."` nos sliders. *(Resolvido e aplicado)*

**[review-regulatory]** — F-REG-01: Substituição de "recomendação prioritária" na prosa técnica (`DESIGN.md § Elevation & Depth`)
A linha 416 de `DESIGN.md` usava o termo *"recomendação prioritária"*.
Fix: Substituir por *"prioridade matemática nº 1 de alocação do GPS de Aporte"*. *(Resolvido e aplicado)*

**[review-regulatory]** — F-REG-02: Explicitação do binding local de `RENDA_FIXA_MANUAL_1` no mockup de IA (`mockups/key-kerno-ai-diagnostico.html`)
Clarificar no HTML que o código canônico retornado pelo Gemini é traduzido localmente pela UI Flutter.
Fix: Adicionar `title` explicativo e notação `RENDA_FIXA_MANUAL_1 → CDB Banco X (Local UI)`. *(Resolvido e aplicado)*

## Reviewer files
- `review-rubric.md`
- `review-accessibility.md`
- `review-regulatory.md`
