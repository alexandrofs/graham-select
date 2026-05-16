---
stepsCompleted: []
lastStep: ''
lastSaved: ''
workflowType: 'testarch-test-design'
inputDocuments: []
---

# Test Design for QA: graham-select

**Purpose:** Receita de execução de testes para o time de QA.

**Date:** 2026-05-16
**Author:** BMad TEA Agent
**Status:** Draft
**Project:** graham-select

**Related:** Ver [test-design-architecture.md](./test-design-architecture.md) para preocupações de testabilidade.

---

## Executive Summary

**Scope:** Cobertura de testes para jornadas de Auth, Ingestão, Dashboard e Graham Engine.

**Risk Summary:**
- Riscos Totais: 5 (2 de Alta Prioridade)
- Categorias Críticas: DATA, BUS

**Coverage Summary:**
- P0 tests: ~15 (Caminhos críticos, segurança)
- P1 tests: ~10 (Features importantes, integração)
- P2 tests: ~8 (Casos de borda)
- **Total**: ~33 testes (~3–5 semanas com 1 QA)

---

## Dependencies & Test Blockers

### Backend/Architecture Dependencies (Pre-Implementation)
1. **API de Seeding** - Backend - Sprint 1 (Épico 2)
   - Necessário para injetar dados de carteira.
   - Bloqueia testes E2E do Dashboard.

### QA Infrastructure Setup
1. **Test Data Factories** - QA
   - Factory para User e TradeTransaction com Faker.
2. **Ambiente de Testes**
   - Local: Docker Compose com Kafka/MySQL.

---

## Test Coverage Plan

**P0/P1/P2/P3 = prioridade e risco, NÃO tempo de execução.**

### P0 (Crítico)
| Test ID | Requirement | Test Level | Risk Link | Notes |
|---|---|---|---|---|
| **P0-001** | Login Google | E2E | R-002 | Valida criação de perfil e RLS |
| **P0-002** | Upload B3 | E2E | R-001 | Valida fluxo completo até dashboard |
| **P0-003** | Expurgo LGPD | API/Int | R-002 | Valida integridade referencial no delete |
| **P0-004** | Fórmula Graham | Unit | R-005 | Valida cálculos matemáticos (BigDecimal) |

### P1 (Alto)
| Test ID | Requirement | Test Level | Risk Link | Notes |
|---|---|---|---|---|
| **P1-001** | Deduplicação | API/Int | R-001 | Evita trades duplicados no mesmo arquivo |
| **P1-002** | Expiração Trial | API | R-005 | Valida transição de tier automática |

---

## Execution Strategy

### Every PR: Playwright Tests (~10-15 min)
- Todos os testes funcionais P0, P1 e P2 (API e E2E Smoke).
- Paralelizado em 4 shards.

### Nightly: Performance & Regression
- Testes de carga com datasets reais (k6).
- Regressão visual Flutter.

---

## QA Effort Estimate

| Priority | Count | Effort Range |
|---|---|---|
| P0 | ~15 | ~3–5 semanas |
| P1 | ~10 | ~2–3 semanas |
| **Total** | ~33 | **~5–8 semanas** |

---

## Appendix A: Code Examples & Tagging

```typescript
import { test } from '@seontechnologies/playwright-utils/api-request/fixtures';
import { expect } from '@playwright/test';
import { faker } from '@faker-js/faker';

test('@P0 @Auth user login auto-provisioning', async ({ apiRequest }) => {
  const email = faker.internet.email();
  const { status, body } = await apiRequest({
    method: 'POST',
    path: '/api/v1/auth/provision',
    body: { email, name: 'Test User' },
  });
  expect(status).toBe(201);
});
```
