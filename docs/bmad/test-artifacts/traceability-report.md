---
stepsCompleted: ['step-01-load-context', 'step-02-discover-tests', 'step-03-map-criteria', 'step-04-analyze-gaps', 'step-05-gate-decision']
lastStep: 'step-05-gate-decision'
lastSaved: '2026-05-17T18:25:00Z'
---

# Traceability Matrix & Gate Decision - Story 1.3

**Story:** Restrição de Funcionalidades por Tier
**Date:** domingo, 17 de maio de 2026
**Evaluator:** TEA Agent

---

## Contexto e Critérios de Aceite Carregados

### Critérios de Aceite (Story 1.3)

1. **AC-1: Acesso Negado (Tier Gratuito e Trial Expirado)**
   - Bloquear requisição com 403 Forbidden ou 402 Payment Required.
   - Resposta RFC 7807 com `urn:problem-type:upgrade-required`.
   - Prioridade: P0

2. **AC-2: Acesso Permitido (Trial Válido)**
   - Permitir acesso se `trialEndsAt` > agora.
   - Prioridade: P0

3. **AC-3: Acesso Permitido (Premium)**
   - Acesso irrestrito para usuários Premium.
   - Prioridade: P0

4. **AC-4: Expiração Automática de Trial**
   - Bloquear acesso se `trialEndsAt` expirado.
   - Atualizar status do usuário para FREE (lazy update).
   - Prioridade: P1

## Passo 2: Descoberta e Catálogo de Testes

### Inventário de Testes Identificados

| ID do Teste | Arquivo | Nível | Foco / Describe |
|:---|:---|:---|:---|
| `1.3-API-001` | `FeatureRestrictionIT.java` | API | `shouldDenyFreeUsers` (AC-1) |
| `1.3-API-002` | `FeatureRestrictionIT.java` | API | `shouldAllowValidTrialUsers` (AC-2) |
| `1.3-API-003` | `FeatureRestrictionIT.java` | API | `shouldAllowPremiumUsers` (AC-3) |
| `1.3-API-004` | `FeatureRestrictionIT.java` | API | `shouldDenyExpiredTrialUsersAndDowngradeThem` (AC-4) |
| `1.3-E2E-001` | `profile_e2e_test.dart` | E2E | `verify profile page shows PREMIUM tier correctly` |
| `1.3-E2E-002` | `profile_e2e_test.dart` | E2E | `verify profile page shows TRIAL with days remaining` |

### Heurísticas de Cobertura (Coverage Heuristics)

#### Cobertura de Endpoints API
- **Endpoints Cobertos**:
  - `GET /api/v1/ranked-companies` (Full coverage: Free, Trial, Premium, Expired)
  - `POST /api/v1/upload-financial-data` (Partial: Basic restriction)
- **Endpoints com Gaps (Sem Testes de Restrição Diretos)**:
  - `POST /api/v1/allocation-goals` (FR20) - Mencionado na história mas não testado explicitamente.

#### Cobertura de Autenticação/Autorização
- **Caminhos Negativos (Negative-Path)**:
  - Usuário FREE -> 403 Forbidden (Validado ✅)
  - Usuário TRIAL Expirado -> 403 Forbidden + Lazy Downgrade (Validado ✅)
- **Gaps Identificados**:
  - Falta validação de status 402 Payment Required (mencionado como alternativa no AC-1).

## Passo 3: Matriz de Rastreabilidade

### Mapeamento Requisitos x Testes

| Critério de Aceite | Prioridade | Status de Cobertura | Testes Associados | Sinais de Heurística |
|:---|:---:|:---:|:---|:---|
| **AC-1**: Acesso Negado (FREE/Expired) | P0 | FULL ✅ | `1.3-API-001`, `1.3-API-004` | Endpoint: Presente; Auth: Negativo ✅; Erro: RFC 7807 ✅ |
| **AC-2**: Acesso Permitido (Trial Válido) | P0 | FULL ✅ | `1.3-API-002`, `1.3-E2E-002` | Endpoint: Presente; Auth: Positivo ✅ |
| **AC-3**: Acesso Permitido (Premium) | P0 | FULL ✅ | `1.3-API-003`, `1.3-E2E-001` | Endpoint: Presente; Auth: Positivo ✅ |
| **AC-4**: Expiração Automática | P1 | FULL ✅ | `1.3-API-004` | Endpoint: Presente; Auth: Negativo ✅; Erro: Lazy Update ✅ |

### Validação da Lógica de Cobertura

- **Critérios P0/P1**: 100% dos critérios possuem ao menos um teste associado.
- **Defesa em Profundidade**: AC-2 e AC-3 possuem cobertura dupla (API e E2E), validando tanto a lógica de negócio quanto a representação visual (badges de Tier).
- **Caminhos de Erro**: AC-1 e AC-4 incluem testes de "acesso negado" e validação do formato RFC 7807.
- **Observação Crítica**: Embora o mecanismo de segurança seja genérico e testado no endpoint `/ranked-companies`, a falta de testes diretos em outros endpoints Premium (ex: `/allocation-goals`) sugere um risco de regressão se a anotação de segurança for omitida no código desses controllers.

## Passo 4: Análise de Gaps e Estatísticas

### 📊 Estatísticas de Cobertura
- **Total de Requisitos**: 4
- **Cobertura Total (Fully Covered)**: 4 (100%) ✅
- **Gaps Críticos (P0)**: 0
- **Gaps de Alta Prioridade (P1)**: 0

**Detalhamento por Prioridade:**
- **P0**: 3/3 (100%)
- **P1**: 1/1 (100%)

### 🔍 Blind Spots (Heurísticas)
- **Endpoints sem testes diretos**: `POST /api/v1/allocation-goals`
- **Gaps de Caminho Negativo**: AC-1 (Falta validação explícita de status 402)

### 📝 Recomendações de Rastreabilidade

1. **URGENTE (P0)**: Nenhuma ação imediata bloqueadora de PR.
2. **ALTA (P1)**: Adicionar teste de API para o endpoint `/allocation-goals` para garantir que a proteção `@RequirePremium` está aplicada corretamente em novos controllers.
3. **MÉDIA (P2)**: Implementar cenário de teste para status 402 no AC-1 para cobertura completa dos requisitos do PRD.

## Passo 5: Decisão de Gate Final

### 🚨 GATE DECISION: PASS ✅

**Rationale:** Todos os critérios de aceite P0 (Críticos) e P1 (Altos) possuem cobertura de teste completa (100%). Os testes de API e E2E garantem a integridade tanto da camada de segurança autoritativa quanto da experiência do usuário (UX).

### 📊 Avaliação dos Critérios de Gate

| Critério | Threshold | Atual | Status |
|:---|:---:|:---:|:---|
| **Cobertura P0** | 100% | 100% | MET ✅ |
| **Cobertura P1** | 90% | 100% | MET ✅ |
| **Cobertura Geral** | 80% | 100% | MET ✅ |

### ⚠️ Riscos Residuais e Blind Spots
- **Risco**: Falta de testes diretos no endpoint `/allocation-goals` (Mecanismo genérico validado em outro endpoint).
- **Risco**: Ausência de validação específica do status HTTP 402 (Mencionado no PRD como alternativa ao 403).

### 🎯 Próximos Passos
1. **Implantação**: Liberar a Story 1.3 para deploy em Staging/Produção.
2. **Melhoria Contínua**: Implementar as recomendações de P1 (teste para `/allocation-goals`) e P2 (teste para 402) conforme detalhado no Passo 4.
3. **Qualidade**: Executar `tea:test-review` para assegurar que os novos testes seguem os padrões de isolamento e determinismo do projeto.

---
**Workflow Concluído.** Rastreabilidade validada e Gate aprovado.
