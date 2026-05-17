---
stepsCompleted: ['step-01-preflight-and-context', 'step-02-identify-targets', 'step-03-generate-tests']
lastStep: 'step-03-generate-tests'
lastSaved: '2026-05-17T17:15:00Z'
inputDocuments:
  - docs/bmad/planning-artifacts/traceability-matrix.md
  - backend/api/src/main/resources/openapi.yaml
  - docs/bmad/implementation-artifacts/
---

# Automação de Testes - Graham Select

## Passo 3: Test Generation Results

### Resumo da Execução (Adaptive Mode)
- **Modo Utilizado**: `subagents` (Parallel Execution).
- **Stack**: `fullstack`.
- **Status**: Sucesso ✅.

### Testes de API (Subagente API)
- **Arquivo**: `backend/api/src/test/java/afsdigital/grahamselect/api/ApiAutomationIT.java`
- **Cobertura**:
  - `GET /api/v1/ranked-companies`: Validação de restrição de Tier (FREE: 403, PREMIUM: 200).
  - `POST /api/v1/upload-financial-data`: Validação de multipart upload e tratamento de erros.
  - **Diferencial**: Lógica de polling implementada para validar processamento assíncrono.

### Testes E2E Flutter (Subagente E2E)
- **Arquivos**:
  - `frontend/integration_test/upload_flow_test.dart`
  - `frontend/integration_test/profile_e2e_test.dart`
- **Cobertura**:
  - Fluxo completo de Upload B3 -> Processamento -> Verificação no Dashboard.
  - Onboarding de Perfil do Investidor (Suitability).

### Testes de Backend/Kafka (Subagente Backend)
- **Arquivos**:
  - `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaTradeExtractedConsumerIT.java`
  - `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/messaging/KafkaTradeExtractionDlqIT.java`
- **Cobertura**:
  - **Race Condition (P0)**: Validação empírica de mensagens simultâneas para o mesmo trade.
  - **DLQ Flow (P1)**: Garantia de que falhas de extração são enviadas para a fila de erro.
- **Fixtures Utilizadas**: `EmbeddedKafkaBroker`, `Awaitility`.

### Métricas de Performance
- **Modo**: Subagent (Parallel).
- **Geração API**: ~3 min.
- **Geração E2E**: ~5 min.
- **Geração Backend**: ~4 min.
- **Total de Testes Gerados**: 5 suítes de teste de alta prioridade.
