---
title: 'TEA Test Design → BMAD Handoff Document'
version: '1.0'
workflowType: 'testarch-test-design-handoff'
generatedBy: 'TEA Master Test Architect'
generatedAt: '2026-05-16'
projectName: 'graham-select'
---

# TEA → BMAD Integration Handoff

## TEA Artifacts Inventory

| Artifact | Path | BMAD Integration Point |
|---|---|---|
| Test Design Architecture | docs/bmad/test-artifacts/test-design-architecture.md | Epic quality requirements |
| Test Design QA | docs/bmad/test-artifacts/test-design-qa.md | Story acceptance criteria |

## Epic-Level Integration Guidance

### Quality Gates
- **Épico 1:** 100% Pass em testes de RLS e Auth.
- **Épico 2:** 100% Pass em testes de Parser (sucesso e falha/DLQ).
- **Épico 4:** Validação de precisão do Motor Graham via Unit Tests (Deltas < 0.001).

## Story-Level Integration Guidance

### P0 Test Scenarios → Story Acceptance Criteria
- **Story 1.1:** Deve garantir isolamento de tenant via RLS no DB.
- **Story 2.1:** Deve validar cabeçalhos do Excel antes de emitir evento Kafka.
- **Story 2.2:** Deve mover linhas corrompidas para DLQ sem interromper o processamento.

## Risk-to-Story Mapping

| Risk ID | Category | P×I | Recommended Story/Epic | Test Level |
|---|---|---|---|---|
| R-001 | DATA | 6 | Epic 2 (Ingestão) | E2E / API |
| R-005 | BUS | 6 | Epic 4 (Ranking) | Unit / API |
| R-002 | SEC | 3 | Epic 1 (Auth) | API Integration |
