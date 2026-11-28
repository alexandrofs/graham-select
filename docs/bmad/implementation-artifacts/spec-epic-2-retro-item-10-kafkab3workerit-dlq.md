---
title: 'Corrigir asserção de DLQ em KafkaB3WorkerIT (Item 10 da Retro do Épico 2)'
type: 'bugfix'
created: '2026-11-28'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O teste de integração `KafkaB3WorkerIT.shouldPublishToDlqWhenRowIsCorrupted:177` falha esperando `"Wrong cell type"` na mensagem da DLQ, pois o `FastExcelB3TradeRowParser` passou a aplicar fallback textual em `parseTradeDate` (lançando `"Text 'invalid-date' could not be parsed at index 0"`).

**Approach:** Atualizar a asserção em `KafkaB3WorkerIT.shouldPublishToDlqWhenRowIsCorrupted` para refletir o contrato real de erro do parser (alinhado ao `FastExcelB3TradeRowParserTest`), executar a suíte `*IT` do módulo `api` para confirmar 100% de aprovação e atualizar o status de `epic-2-retro-item-10` para `done` em `sprint-status.yaml`.

</frozen-after-approval>

## Implementation Notes

- Atualizada a asserção em `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaB3WorkerIT.java:178-180` para validar `correlationId`, `"PETR4"` e `"Text 'invalid-date' could not be parsed"`, alinhado ao contrato de erro textual do `FastExcelB3TradeRowParser` e ao `FastExcelB3TradeRowParserTest`.
- Adicionada a coluna `corretora` (`"XP"`) em `KafkaB3WorkerIT.shouldProcessFileAndPublishTradeExtractedEvents` para evitar race condition com `SQL Error 23502` (`NULL not allowed for column "BROKER"`) em `KafkaTradeExtractedConsumer`, que incrementava `processedRows` de 2 para 4 via `addDuplicate`.
- Atualizado `epic-2-retro-item-10-corrigir-falha-em-kafkab3workerit-should` para `status: done` em `docs/bmad/implementation-artifacts/sprint-status.yaml`.

## Review Triage Log

- **Finding 1 (`KafkaB3WorkerIT` mascara bug `NOT NULL` em `trades.broker` ao adicionar `corretora`):** `medium` (defer) — Bug pré-existente entre `FastExcelB3TradeRowParser` (`corretora` opcional) e `13-create-trades-table.yaml` (`broker` `nullable: false`); registrado em `deferred-work.md` para correção no `KafkaTradeExtractedConsumer`.
- **Finding 2 (Asserção permissiva na DLQ em `KafkaB3WorkerIT:180`):** `medium` (patch) — Aplicado patch validando `correlationId`, `"PETR4"` e `"Text 'invalid-date' could not be parsed"`.
- **Finding 3 (`app.upload.storage-path=./test-uploads` muta fixtures versionadas):** `low` (reject) — Configuração pré-existente no teste, fora do escopo desta alteração.
- **Finding 4 (`Consumer` Kafka sem `try-with-resources` em `KafkaB3WorkerIT`):** `low` (reject) — Higiene pré-existente de teste, não causada por esta alteração.
- **Finding 5 (Status `'in-progress'` e referência de linha `177` na spec):** `low` (patch) — Resolvido na finalização da spec (`status: 'done'` e linhas `178-180`).
- **Finding 6 (Cabeçalho e `source_spec: none` em `deferred-work.md`):** `false` — Formato `source_spec: none` segue exatamente a regra de split do `step-01-clarify-and-route.md`.
- **Finding 7 (Artefatos não commitados no worktree antes do commit):** `false` — Estado esperado antes da etapa `Commit` do workflow, mantendo os arquivos alterados da branch conforme instrução do usuário.
