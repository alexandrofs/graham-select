---
title: "Reconciliation Report: Product Brief (2026-11-21) vs. DESIGN.md & EXPERIENCE.md"
source: "docs/bmad/planning-artifacts/briefs/brief-graham-select-2026-11-21/brief.md"
status: "reconciled"
date: "2026-11-21"
dropped_items: 0
---

# Relatório de Reconciliação de Insumos — `brief.md` (Product Brief: Kerno)

Este documento verifica a incorporação integral da visão estratégica, dos 5 problemas mapeados, dos 5 pilares de solução, dos perfis intergeracionais e da visão de longo prazo do [brief.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/briefs/brief-graham-select-2026-11-21/brief.md) nos contratos [DESIGN.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/DESIGN.md) e [EXPERIENCE.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/EXPERIENCE.md).

## 1. Mapeamento dos 5 Pilares Estratégicos do Product Brief

| Pilar do `brief.md` | Intenção Qualitativa no Brief | Onde Vive em `DESIGN.md` & `EXPERIENCE.md` | Status |
|---|---|---|---|
| **1. Rebranding Oficial para Kerno (Neutro & AI-Ready)** | Superar o posicionamento restritivo de *"Graham Select"*, adotando **Kerno** (*"o núcleo inteligente do seu dinheiro"*) pronto para unir investimentos, controle de gastos e IA sob uma marca única. | `DESIGN.md` (`## Brand & Style`, `## Colors` com Roxo-Índigo Kerno `#4F46E5`, `## Do's and Don'ts`) e `EXPERIENCE.md` (`## Foundation`, `## Information Architecture`, `## Key Flows` `UJ-5`). | **100% Coberto** |
| **2. Design System Intergeracional (Inspirado no Nubank)** | Resolver o atrito visual tanto para o **Investidor Jovem / Digital Native (20–35 anos)** quanto para o **"CFO" da Família (40+ anos)** com fundos limpos, contraste WCAG AA/AAA, tipografia generosa/escalável e zero ruído visual. | `DESIGN.md` (`## Brand & Style`, `## Colors` Light-First `#F8FAFC`/`#FFFFFF` com contraste `17.4:1` AAA, `## Typography` Plus Jakarta Sans + Inter `tabular-nums` escalável até `2.0x` sem `...`, `## Layout & Spacing` sem tabelas móveis). | **100% Coberto** |
| **3. Carteira & Rebalanceamento pelo Diagrama do Cerrado + Graham** | Eliminar a paralisia do aporte mensal e o giro patrimonial (sem vendas forçadas nem geração de DARF), unindo * templates* iniciais, notas de qualidade editáveis, tetos de risco e a margem de segurança de Benjamin Graham. | `DESIGN.md` (`## Components`: `contribution-gps-card`, `math-transparency-accordion`, `cerrado-template-selector-card`, `cerrado-quality-card`, `badge-graham-bonus`, `badge-quarantine`) e `EXPERIENCE.md` (`## Component Patterns`, `## Inspiration & Anti-patterns`, `## Key Flows` `UJ-1` e `UJ-2`). | **100% Coberto** |
| **4. MVP Mobile Enxuto, Seguro e Escalável (iOS & Android)** | Experiência de bolso para decisão em `< 30 segundos`: Login OAuth2 + biometria (FaceID/Digital), **Dashboard Patrimonial** com **Modo Privacidade**, **GPS de Aporte** e arquitetura modular pronta para Controle de Gastos e Copiloto de IA. | `DESIGN.md` (`## Components`: `wealth-hero-card`, `privacy-toggle`, `bottom-nav-bar`, `input-currency`) e `EXPERIENCE.md` (`## Information Architecture` Superfícies 2, 4, 5 e 8, `## Interaction Primitives`, `## Responsive & Platform`, `## Key Flows` `UJ-1` e `UJ-4`). | **100% Coberto** |
| **5. Nova Landing Page de Alta Conversão** | Storytelling direto (*"Do caos das planilhas ao núcleo inteligente das suas finanças"*), vitrine mobile, explicação visual Graham + Diagrama do Cerrado e CTA proeminente (ampliado no PRD com o **Programa Founders**). | `DESIGN.md` (`## Colors` Ouro Founder, `## Typography`, `## Layout & Spacing`) e `EXPERIENCE.md` (`## Information Architecture` Superfície 1, `## Responsive & Platform`, `## Key Flows` `UJ-5`). | **100% Coberto** |

## 2. Mapeamento dos Ganchos de Evolução Futura (Visão de 2–3 Anos)

- **Conexão Futura entre Controle de Gastos (Saídas) e GPS de Aporte (Entradas):**
  - Preservada estruturalmente no `{components.bottom-nav-bar}` (5º slot modular **Gastos** controlado pela *feature flag* `enable_expenses_module`) e no componente `{components.input-currency}` do **GPS de Aporte**, cujo contrato comportamental reserva o slot de chip de preenchimento rápido *"Usar Saldo Livre do Mês (R$ X)"* para quando o módulo de Gastos for ativado no Ciclo 2.1.
- **Fundação para Ferramentas e Copiloto de IA:**
  - Inaugurada no Ciclo 2.0 pelo módulo **Kerno AI (Diagnóstico de Carteira)** (`{components.kerno-ai-diagnostic-card}`) com arquitetura *Deterministic-First*, **Sanitizador LGPD de IA** e guardrails CVM 19/20, preparando a linguagem visual e a confiança do usuário para o futuro Copiloto Conversacional do Ciclo 2.2.

## 3. Ideias Qualitativas Descartadas

- **Nenhuma (`0` itens descartados).** Toda a visão qualitativa e ergonômica do Product Brief está refletida nos dois documentos de UX.
