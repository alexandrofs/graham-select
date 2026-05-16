---
stepsCompleted: ['step-01-detect-mode', 'step-02-load-context', 'step-03-risk-and-testability', 'step-04-coverage-plan', 'step-05-generate-output']
lastStep: 'step-05-generate-output'
lastSaved: '2026-05-16'
workflowType: 'testarch-test-design'
inputDocuments:
  - docs/bmad/planning-artifacts/prd.md
  - docs/bmad/planning-artifacts/architecture.md
---

# Test Design for Architecture: graham-select

**Purpose:** Preocupações arquiteturais, lacunas de testabilidade e requisitos de NFR para revisão pelas equipes de Arquitetura e Desenvolvimento.

**Date:** 2026-05-16
**Author:** BMad TEA Agent
**Status:** Architecture Review Pending
**Project:** graham-select
**PRD Reference:** docs/bmad/planning-artifacts/prd.md
**ADR Reference:** docs/bmad/planning-artifacts/architecture.md

---

## Executive Summary

**Scope:** Fundação de testes para autenticação, ingestão de dados (Kafka/B3), motor de recomendação Graham e conformidade LGPD.

**Business Context:**
- **Impacto:** Gestão de patrimônio financeiro e recomendações de investimento.
- **Problema:** Falta de validação E2E em ambiente real e riscos de integridade de dados/fuso horário.

**Architecture:**
- **Key Decision 1:** Java 21 + Spring Boot 3.4 Multi-module.
- **Key Decision 2:** Event-Driven Architecture com Apache Kafka para ingestão.
- **Key Decision 3:** Flutter mobile-first com Clean Architecture.

**Risk Summary:**
- **Total de riscos:** 5
- **Alta prioridade (≥6):** 2 riscos (Parser B3 e Market Data)
- **Esforço de teste:** ~75–125 horas (~2–4 semanas para 1 QA)

---

## Quick Guide

### 🚨 BLOCKERS - Time deve Decidir

1. **B-001: API de Test Data Seeding** - Necessário para injetar estados de carteira complexos sem depender de fluxos lentos de upload (Dono: Backend).
2. **B-002: Mock de Market Data** - Necessário para testes determinísticos do motor Graham (Dono: Backend).

### ⚠️ HIGH PRIORITY - Time deve Validar

1. **R-001: Resiliência do Parser B3** - Recomendamos suíte de integração com Testcontainers usando samples reais (Dono: Charlie).
2. **R-005: Obsolecência de Dados** - Recomendamos implementação de circuit breakers e indicadores de cache na UI (Dono: Charlie).

### 📋 INFO ONLY - Soluções Providas

1. **Estratégia**: Pirâmide de testes focada em Unit (Negócio) e E2E (Jornadas Críticas).
2. **Ferramentas**: Playwright (E2E/API), JUnit 5, Testcontainers.
3. **Qualidade**: Pass rate P0/P1 de 100%.

---

## For Architects and Devs - Open Topics 👷

### Risk Assessment

| Risk ID | Category | Description | Prob | Imp | Score | Mitigation | Owner |
|---|---|---|---|---|---|---|---|
| **R-001** | **DATA** | Inconsistência no Preço Médio por falha no parser B3 | 2 | 3 | **6** | Suite de integração com samples reais | Charlie |
| **R-005** | **BUS** | Recomendação baseada em dados obsoletos | 2 | 3 | **6** | Circuit breaker + Cache indicators | Charlie |
| R-002 | **SEC** | Vazamento de dados (Falha RLS) | 1 | 3 | 3 | Testes cross-tenant mandatórios | Alex |

---

### Testability Concerns and Architectural Gaps

**🚨 ACTIONABLE CONCERNS**

| Concern | Impact | What Architecture Must Provide | Owner |
|---|---|---|---|
| **Sem API de Seeding** | Lentidão nos testes E2E | Endpoint POST /api/test/seed (dev only) | Backend |
| **Async Observability** | Testes E2E instáveis | Correlation IDs e Polling robusto | Backend |

---

### Assumptions and Dependencies

1. **Assumption:** O ambiente de testes terá paridade com produção em termos de configuração de Kafka e MySQL.
2. **Dependency:** Disponibilidade de credenciais de teste para Google OAuth2 (Sandbox).

**End of Architecture Document**
