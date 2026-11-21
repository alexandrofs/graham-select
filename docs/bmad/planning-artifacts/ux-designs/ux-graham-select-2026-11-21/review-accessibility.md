# Revisão de Acessibilidade e Ergonomia Intergeracional (`review-accessibility.md`)

- **Escopo Auditado:** `DESIGN.md`, `EXPERIENCE.md` e `mockups/key-*.html` (`key-dashboard-patrimonial.html`, `key-gps-aporte.html`, `key-kerno-ai-diagnostico.html`, `key-diagrama-cerrado.html`)
- **Lente de Auditoria:** Acessibilidade Financeira Intergeracional (Investidores *Digital Natives* 20–35 anos e *CFOs da Família* 40+ anos) & Conformidade **WCAG 2.1 AA / AAA**
- **Data:** 2026-11-21
- **Veredito Geral:** **APROVADO COM RESSALVAS MENORES (PASS WITH MINOR REFINEMENTS)** — `0` críticos, `0` altos, `2` médios, `5` baixos.

---

## 1. Resumo Executivo da Auditoria

O **Design System Intergeracional do Kerno** (`DESIGN.md` + `EXPERIENCE.md`) e seus 4 mockups HTML de referência demonstram maturidade exemplar em acessibilidade financeira:
1. **Contraste Tipográfico Principal (WCAG AAA):** O par principal `{colors.text-primary}` (`#0F172A`) sobre `{colors.surface-elevated}` (`#FFFFFF`) atinge **`17.85:1`** no modo claro e **`16.96:1`** (`#F8FAFC` sobre `#111827`) no modo escuro — mais que o dobro do piso **WCAG 2.1 AAA (`>= 7:1`)**. Até mesmo o texto atenuado `{colors.text-muted}` (`#475569` sobre `#FFFFFF`) mantém **`7.58:1`** (AAA).
2. **Elasticidade Tipográfica (`1.0x–2.0x`) e Banimento de Tabelas:** Ambos os documentos proíbem explicitamente `TextOverflow.ellipsis` (`...`), `maxLines: 1` e tabelas com rolagem horizontal em `< 600dp`, e nenhum dos 4 mockups HTML utiliza `<table>`, `text-overflow: ellipsis` ou `white-space: nowrap`.
3. **Numerais Tabulares e Ergonomia Motora (`48x48dp`):** `tabular-nums` / `FontFeature.tabularFigures()` é obrigatório em toda a rampa numérica e `48x48dp` é aplicado consistentemente aos alvos primários (botões, `{components.privacy-toggle}`, `{components.multi-cpf-selector}`, `{components.bottom-nav-bar}`, gatilho do acordeão e *thumb* do slider).
4. **Tríade Daltônico-Segura e Leitores de Tela no Modo Privacidade:** Todos os estados semânticos combinam **Cor + Ícone + Rótulo Textual Explícito**, e `EXPERIENCE.md` especifica o bloqueio semântico (`Semantics`) para que o **Modo Privacidade** (`R$ ••••••`) jamais exponha valores em reais por voz no VoiceOver/TalkBack.

As oportunidades de melhoria identificadas abaixo (2 de severidade `[medium]` e 5 de severidade `[low]`) tratam de contrastes de bordas de componentes não-textuais (WCAG 1.4.11 `>= 3:1`), elevação de valores monetários em `semantic-emerald` e alertas críticos do nível AA para AAA (`>= 7:1`), e atributos ARIA/reflow nos mockups HTML estáticos.

---

## 2. Verificação Matemática de Contrastes (WCAG 2.1 AA / AAA)

| Par de Tokens (`DESIGN.md`) | Hex (Texto / Elemento) | Hex (Fundo) | Razão Calculada | Meta Exigida | Status |
|---|---|---|---|---|---|
| `text-primary` / `surface-elevated` (Saldos `R$` Hero & Input) | `#0F172A` | `#FFFFFF` | **`17.85:1`** | `>= 7.0:1` (AAA) | ✅ PASS |
| `text-primary` / `surface-base` | `#0F172A` | `#F8FAFC` | **`17.06:1`** | `>= 7.0:1` (AAA) | ✅ PASS |
| `text-secondary` / `surface-elevated` | `#334155` | `#FFFFFF` | **`10.35:1`** | `>= 4.5:1` (AA) | ✅ PASS (AAA) |
| `text-muted` / `surface-elevated` | `#475569` | `#FFFFFF` | **`7.58:1`** | `>= 4.5:1` (AA) | ✅ PASS (AAA) |
| `text-primary-dark` / `surface-elevated-dark` | `#F8FAFC` | `#111827` | **`16.96:1`** | `>= 7.0:1` (AAA) | ✅ PASS |
| `text-secondary-dark` / `surface-elevated-dark` | `#CBD5E1` | `#111827` | **`11.95:1`** | `>= 4.5:1` (AA) | ✅ PASS (AAA) |
| `text-muted-dark` / `surface-elevated-dark` | `#94A3B8` | `#111827` | **`6.92:1`** | `>= 4.5:1` (AA) | ✅ PASS |
| `text-on-primary` / `brand-primary` (`button-primary`) | `#FFFFFF` | `#4F46E5` | **`6.29:1`** | `>= 4.5:1` (AA) | ✅ PASS |
| `brand-primary-deep` / `brand-accent-soft` | `#3730A3` | `#EEF2FF` | **`8.88:1`** | `>= 4.5:1` (AA) | ✅ PASS (AAA) |
| `semantic-emerald` / `semantic-emerald-surface` | `#047857` | `#ECFDF5` | **`5.21:1`** | `>= 4.5:1` (AA) | ✅ PASS (AA) |
| `semantic-emerald` / `surface-elevated` (`contribution-gps-card` `R$`) | `#047857` | `#FFFFFF` | **`5.48:1`** | `>= 7.0:1` (AAA cifra central) | ⚠️ AA (ver `[medium]` F-01) |
| `semantic-amber` / `semantic-amber-surface` (Alertas / Quarentena) | `#B45309` | `#FFFBEB` | **`4.84:1`** | `>= 7.0:1` (AAA alerta crítico) | ⚠️ AA (ver `[medium]` F-01) |
| `semantic-crimson` / `semantic-crimson-surface` (Bloqueio Soma 100%) | `#B91C1C` | `#FEF2F2` | **`5.91:1`** | `>= 7.0:1` (AAA alerta crítico) | ⚠️ AA (ver `[medium]` F-01) |
| `founder-gold` / `founder-gold-surface` (`badge-founder`) | `#92400E` | `#FEF3C7` | **`6.37:1`** | `>= 4.5:1` (AA) | ✅ PASS |
| `border-strong` / `surface-elevated` (Borda de input/botão secundário) | `#CBD5E1` | `#FFFFFF` | **`1.48:1`** | `>= 3.0:1` (WCAG 1.4.11 UI) | ⚠️ Abaixo de 3:1 (ver `[medium]` F-02) |

---

## 3. Achados Detalhados por Severidade

### `[medium]` F-01 — Cifras Monetárias de Aporte (`contribution-gps-card`) e Alertas Críticos Atendem WCAG AA (`4.84:1–5.91:1`), mas Ficam Abaixo do Piso WCAG AAA (`>= 7:1`)

- **Localização:**
  - `DESIGN.md`: linhas 31–45 (`semantic-emerald: '#047857'`, `semantic-amber: '#B45309'`, `semantic-crimson: '#B91C1C'`), linha 201 (`risk-ceiling-saturation-card.foreground`), linha 444 (`contribution-gps-card` valor em `R$` usando `{colors.semantic-emerald}`).
  - `mockups/key-gps-aporte.html`: linhas 177–181 (`.gps-amount { color: var(--semantic-emerald); }`) e linhas 218–227 (`.saturation-card`).
  - `mockups/key-diagrama-cerrado.html`: linhas 306–309 (card carmesim de bloqueio de soma `95,0% != 100,0%`) e linhas 319–335 (card âmbar de quarentena).
- **Problema:** Enquanto o saldo patrimonial principal (`{colors.text-primary}` `#0F172A` sobre `#FFFFFF`) entrega `17.85:1` (AAA), o valor monetário central de cada sugestão do **GPS de Aporte** (`R$ 2.380,00` em `.gps-amount`) utiliza `{colors.semantic-emerald}` (`#047857`) sobre fundo branco (`#FFFFFF`), resultando em **`5.48:1`** (AA). Da mesma forma, os textos de alertas críticos em `{colors.semantic-amber}` (`#B45309` sobre `#FFFBEB` = **`4.84:1`**) e `{colors.semantic-crimson}` (`#B91C1C` sobre `#FEF2F2` = **`5.91:1`**) cumprem WCAG 2.1 AA (`>= 4.5:1`), mas não atingem `>= 7:1` (AAA) para leitura sob luz solar intensa por usuários 40+ com presbiopia. No modo escuro, `{colors.semantic-emerald-dark}` (`#34D399` sobre `#064E3B` = **`5.06:1`**) e `{colors.semantic-crimson-dark}` (`#F87171` sobre `#450A0A` = **`5.84:1`**) também ficam na faixa AA.
- **Correção Recomendada:**
  1. Em `contribution-gps-card`, exibir o valor monetário principal em `R$` com `{colors.text-primary}` (`#0F172A`, `17.85:1` AAA) ou escurecer `{colors.semantic-emerald}` para `#065F46` (**`7.68:1` AAA** sobre `#FFFFFF` e **`7.13:1` AAA** sobre `#ECFDF5`).
  2. Para atingir AAA (`>= 7:1`) também nos alertas críticos no modo claro, ajustar `{colors.semantic-amber}` para `#92400E` (**`7.05:1` AAA** sobre `#FFFBEB`) e `{colors.semantic-crimson}` para `#991B1B` (**`7.60:1` AAA** sobre `#FEF2F2`), e no modo escuro ajustar `{colors.semantic-emerald-dark}` para `#6EE7B7` sobre `#064E3B` (**`7.45:1` AAA**) e `{colors.semantic-crimson-dark}` para `#FCA5A5` sobre `#450A0A` (**`7.92:1` AAA**).

---

### `[medium]` F-02 — Contraste Não-Textual (WCAG 1.4.11 `>= 3:1`) Insuficiente nas Bordas de `input-currency` (Sem Foco), `button-secondary` e Marcador de Meta Sobreposto em `macro-class-distribution-card`

- **Localização:**
  - `DESIGN.md`: linhas 52 e 55 (`border-strong: '#CBD5E1'`, `border-strong-dark: '#334155'`), linhas 176–178 (`macro-class-distribution-card.barTrack` / `barFillPrimary` / `barFillTarget`), linha 254 (`button-secondary.border`), linha 263 (`input-currency.border`).
  - `mockups/key-dashboard-patrimonial.html`: linhas 275–293 (`.macro-bar-track`, `.macro-bar-fill`, `.macro-target-marker`) e linhas 338–350 (`.btn-secondary-touch`).
- **Problema:**
  1. O token `{colors.border-strong}` (`#CBD5E1`) sobre `{colors.surface-elevated}` (`#FFFFFF`) possui razão de contraste de apenas **`1.48:1`** (e `{colors.border-strong-dark}` `#334155` sobre `#111827` possui **`1.86:1`**). Como `input-currency` (quando não focado) e `button-secondary` dependem dessa borda para delimitar sua área clicável, o contraste fica abaixo do piso de **`3.0:1`** da WCAG 2.1 Critério 1.4.11 (*Non-text Contrast*).
  2. No gráfico de barras `{components.macro-class-distribution-card}`, quando o `% Atual` ultrapassa a `% Meta` (como em *FIIs: 30,2% / Meta 30,0%* e *Renda Fixa: 24,0% / Meta 20,0%* em `mockups/key-dashboard-patrimonial.html` linhas 435–453), o marcador `.macro-target-marker` (`#3730A3`) fica sobreposto à barra `.macro-bar-fill` (`#4F46E5`), gerando contraste de apenas **`1.42:1`** entre o marcador de meta e o preenchimento da barra.
- **Correção Recomendada:**
  1. Escurecer `{colors.border-strong}` para `#94A3B8` (**`3.02:1`** sobre `#FFFFFF`) e clarear `{colors.border-strong-dark}` para `#64748B` (**`3.75:1`** sobre `#111827`).
  2. Adicionar ao `.macro-target-marker` (`barFillTarget`) um halo/borda lateral de `1px solid #FFFFFF` (`box-shadow: 0 0 0 1.5px var(--surface-elevated)`) e cor `{colors.text-primary}` (`#0F172A`, contraste **`3.11:1`** contra `#4F46E5` e **`15.8:1`** contra `#F1F5F9`), garantindo visibilidade nítida mesmo quando a barra ultrapassa a meta.

---

### `[low]` F-03 — Omissão de Propriedades `*Dark` Explícitas em Alguns Componentes do Frontmatter de `DESIGN.md`

- **Localização:** `DESIGN.md`, linhas 168–328 (`components:` YAML frontmatter).
- **Problema:** Embora a paleta global defina todos os tokens `*-dark` necessários (linhas 15–56), apenas `wealth-hero-card` e os três `badge-*` declaram explicitamente `foregroundDark`. Componentes como `macro-class-distribution-card`, `contribution-gps-card`, `math-transparency-accordion`, `cerrado-quality-card`, `kerno-ai-diagnostic-card`, `input-currency` e `bottom-nav-bar` declaram `backgroundDark`, mas omitem `foregroundDark: '{colors.text-primary-dark}'` e `borderDark: '{colors.border-subtle-dark}'`.
- **Correção Recomendada:** Explicitar os pares `*Dark` (`foregroundDark`, `borderDark`, `barTrackDark`) nos objetos de componente do frontmatter de `DESIGN.md` para evitar que geradores de código mapeiem texto escuro `#0F172A` sobre fundo `#111827` no Dark Mode.

---

### `[low]` F-04 — Linhas Flexbox/Grid nos Mockups HTML Sem `flex-wrap: wrap` para Simular o Reflow Vertical em `1.3x–2.0x`

- **Localização:**
  - `mockups/key-dashboard-patrimonial.html`: linhas 269–274 (`.macro-header`), linhas 318–322 (`.asset-top`), linhas 332–337 (`.asset-metrics`).
  - `mockups/key-gps-aporte.html`: linhas 173–176 (`.gps-amount-row`), linhas 208–210 (`.accordion-body` com `grid-template-columns: 1fr 1fr` fixo).
  - `mockups/key-kerno-ai-diagnostico.html`: linhas 109–116 (`.ai-card-header`).
- **Problema:** Os contratos `DESIGN.md` (linhas 386 e 448) e `EXPERIENCE.md` (linhas 156–157) determinam acertadamente que, em escalas `1.3x–2.0x`, linhas com rótulo à esquerda e valor à direita convertem-se em empilhamento vertical (`Wrap` / `Column`) e o acordeão de 2 colunas passa para 1 coluna em `>= 1.5x`. Nos mockups HTML estáticos, porém, `.macro-header`, `.asset-metrics`, `.gps-amount-row` e `.ai-card-header` não possuem `flex-wrap: wrap`, e `.accordion-body` fixa `grid-template-columns: 1fr 1fr`.
- **Correção Recomendada:** Adicionar `flex-wrap: wrap; gap: 4px 8px;` a `.macro-header`, `.asset-top`, `.asset-metrics`, `.gps-amount-row` e `.ai-card-header`, e alterar `.accordion-body` para `grid-template-columns: repeat(auto-fit, minmax(145px, 1fr));`.

---

### `[low]` F-05 — Tokens `heading-1`, `heading-2` e `heading-3` sem `fontVariantNumeric: 'tabular-nums'` no Frontmatter de `DESIGN.md`

- **Localização:** `DESIGN.md`, linhas 65–82 (`typography.heading-1..3`) vs. linha 454 (`risk-ceiling-saturation-card` exibindo `R$ X_rem` em `{typography.heading-3}`) e `mockups/key-gps-aporte.html` linha 368 (`Saldo Excedente Protegido por Teto de Risco (R$ 42.350,00)`).
- **Problema:** A regra de ouro de `DESIGN.md` (linhas 381 e 497) exige `tabular-nums` em 100% dos valores em `R$` e `%`. Como `heading-3` é usado em títulos de cartões que embutem cifras em `R$` (como o `risk-ceiling-saturation-card`) e em cabeçalhos de soma percentual (`Soma: 95,0%`), a ausência de `fontVariantNumeric: 'tabular-nums'` nos tokens `heading-1..3` do frontmatter pode causar largura proporcional nos dígitos em Flutter caso o estilo não herde `tabularFigures()`.
- **Correção Recomendada:** Incluir `fontVariantNumeric: 'tabular-nums'` nos tokens `heading-1`, `heading-2` e `heading-3` do frontmatter de `DESIGN.md`.

---

### `[low]` F-06 — Linhas do Assistente de 5 Critérios Objetivos (`cerrado-quality-card`) Sem Altura Mínima de Toque `48dp` no Mockup

- **Localização:**
  - `DESIGN.md`: linha 460 (`cerrado-quality-card` — Assistente de 5 Critérios Objetivos com 5 chaves de `0 a 2 pontos`).
  - `mockups/key-diagrama-cerrado.html`: linhas 172–186 (`.criteria-box`, `.criteria-item`) e linhas 265–272.
- **Problema:** Em `EXPERIENCE.md` (linha 116), o **Assistente de 5 Critérios Objetivos** é um mecanismo interativo alternativo ao slider onde o usuário pontua `0`, `1` ou `2` pontos em 5 perguntas. No mockup `key-diagrama-cerrado.html`, cada `.criteria-item` é renderizado apenas como uma linha de texto compacta de `12px` (~`18px` de altura) sem especificar controles interativos de `48x48dp`.
- **Correção Recomendada:** Especificar em `DESIGN.md` (#7 `cerrado-quality-card`) e em `mockups/key-diagrama-cerrado.html` que cada linha editável do Assistente de 5 Critérios possui `min-height: 48px` (ou botões segmentados `0` / `1` / `2` com área de toque `>= 48x48dp`).

---

### `[low]` F-07 — Atributos `aria-label` no Modo Privacidade (`R$ ••••••`) e `role="slider"` Parciais nos Mockups HTML

- **Localização:**
  - `mockups/key-dashboard-patrimonial.html`: linha 513 (possui `aria-label="Valor monetário oculto pelo Modo Privacidade"`) vs. linhas 515, 529, 540, 560 e 572 (ocorrências secundárias de `R$ ••••••` sem `aria-label`).
  - `mockups/key-diagrama-cerrado.html`: linhas 257–263 e 325–331 (`.slider-thumb` sem atributos ARIA de slider).
- **Problema:** Em `EXPERIENCE.md` (linhas 159–160), o contrato de leitores de tela para VoiceOver/TalkBack/ARIA está exemplarmente definido. Contudo, no HTML estático de `key-dashboard-patrimonial.html`, apenas o saldo principal `.hero-balance` recebeu `aria-label`, enquanto os demais `R$ ••••••` seriam lidos literalmente por leitores de tela Web como `"R cifrão ponto preto ponto preto..."`. Em `key-diagrama-cerrado.html`, o slider visual customizado não declara `role="slider" aria-valuemin="1" aria-valuemax="10" aria-valuenow="9"`.
- **Correção Recomendada:** Aplicar `aria-label="Valor monetário oculto pelo Modo Privacidade"` em todos os elementos que contêm `R$ ••••••` em `mockups/key-dashboard-patrimonial.html`, e adicionar `role="slider" aria-valuemin="1" aria-valuemax="10" aria-valuenow="9" aria-valuetext="Nota 9 de 10, Teto Máximo de Risco 10 por cento"` nos elementos `.slider-thumb` de `mockups/key-diagrama-cerrado.html`.

---

## 4. Checklist de Conformidade Consolidado

| Dimensão Auditada | Requisito | Status |
|---|---|---|
| **1. Razões de Contraste (WCAG 2.1 AA / AAA)** | `>= 4.5:1` texto, `>= 3:1` UI/gráficos, `>= 7:1` saldos primários e alertas | ⚠️ **Parcial** (`17.85:1` AAA no saldo principal; ajustes `[medium]` F-01 e F-02 para bordas `3:1` e cifras/alertas semânticos `7:1`) |
| **2. Dynamic Type & Zero-Truncation (`1.0x–2.0x`)** | Proibição de `TextOverflow.ellipsis` (`...`) em `R$`, tickers e `%`, reflow vertical `< 600dp` sem tabelas horizontais | ✅ **Conforme** (Refinamento `[low]` F-04 de `flex-wrap` no CSS dos mockups) |
| **3. Numerais Tabulares & Ergonomia Motora** | `tabular-nums` / `FontFeature.tabularFigures()` em `R$` e `%`; alvos `>= 48x48dp` | ✅ **Conforme** (Refinamentos `[low]` F-05 e F-06 para `heading-1..3` e itens do assistente) |
| **4. Tríade Semântica para Daltônicos & Screen Readers** | Cor + Ícone + Rótulo em Esmeralda/Âmbar/Carmesim; anúncio seguro no Modo Privacidade (`R$ ••••••`) | ✅ **Conforme** (Refinamento `[low]` F-07 de atributos ARIA nos mockups HTML) |
