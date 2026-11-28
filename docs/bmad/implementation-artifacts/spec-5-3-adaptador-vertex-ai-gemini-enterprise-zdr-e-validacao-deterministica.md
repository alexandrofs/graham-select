---
title: 'Adaptador Vertex AI Gemini Enterprise ZDR e Validação Determinística Pós-Geração'
type: 'feature'
created: '2026-11-28'
baseline_commit: '846aafdc38b189b9c8336c1d1ad262ffce1f40a2'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/docs/bmad/implementation-artifacts/epic-5-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O sistema já possui cache/FinOps (Story 5.1) e sanitização LGPD pré-payload (Story 5.2), mas ainda não dispõe da porta `AiPortfolioAnalyzerPort`, do adaptador corporativo Google Cloud Vertex AI com Zero Data Retention (ZDR), da validação determinística pós-geração (ancoragem numérica e filtro léxico CVM 19/20) nem do fallback quantitativo local em caso de falha ou timeout.

**Approach:** Definir em `backend/common` a porta `AiPortfolioAnalyzerPort`, os modelos de diagnóstico em 4 blocos, o `PostGenerationValidator` e o `DeterministicPortfolioFallbackGenerator`; implementar em `backend/valuation-service` o `VertexAiGeminiAdapter` via API REST do Vertex AI (`RestClient` + `google-auth-library-oauth2-http`, `gemini-2.5-flash`, `temperature = 0.1`, timeout `15s`); e espelhar variáveis/credenciais em `application.yml`, `helm/graham-select/`, `.github/workflows/deploy.yml` e `docs/workspace.dsl`.

## Boundaries & Constraints

**Always:**
- Serializar `SanitizedPortfolioSnapshot` via `ObjectMapper` e validar com `AiPayloadValidator.validate(...)` antes de qualquer chamada HTTP ao Vertex AI.
- Conectar-se exclusivamente ao endpoint corporativo Vertex AI (`https://{location}-aiplatform.googleapis.com/v1/projects/{projectId}/locations/{location}/publishers/google/models/{modelName}:generateContent`) com `gemini-2.5-flash`, `temperature = 0.1`, `responseMimeType = "application/json"` e JSON Schema fechado nos 4 blocos (`resumoExecutivo`, `pontosFortes`, `alertasDeRiscoEConcentracao`, `racionalDoPlanoDeAporte`).
- Aplicar timeout rígido de `15s` (connect + read) na chamada HTTP ao Vertex AI e usar `GcpAccessTokenProvider` (baseado em `GoogleCredentials.getApplicationDefault()`).
- Validar pós-geração no `PostGenerationValidator`: (1) 4 blocos não vazios; (2) zero tickers inventados (excluindo siglas de mercado como `HHI`, `CVM`, `B3`, `P/L`, `P/VP`, `DY`, `CDI`, `IPCA`, `SELIC`, `ETF`, `FII`, `BDR`, `LGPD`, `ZDR`); (3) todo número citado (normalizando `,` → `.`, ignorando dígitos dentro de tickers do snapshot e escala `/10` de nota) ancorado em valores do snapshot com tolerância `±0.1` (`±0.01` para `hhiIndex`); (4) filtro léxico CVM 19/20 via regex `\b` case-insensitive bloqueando termos imperativos (`comprar?`, `compre`, `vender?`, `venda`, `lucro\s+garantido`, `retorno\s+garantido`, `rentabilidade\s+garantida`, `recomendo\s+(a\s+)?(compra|venda)`).
- Abortar imediatamente sem retry síncrono em violação CVM, alucinação, resposta incompleta/malformada, erro do provedor, falha de credencial OAuth2 ou timeout `> 15s`, registrando `ERROR` com stack trace (ou `WARN` em rejeição semântica sem PII) e acionando `DeterministicPortfolioFallbackGenerator` sem debitar cota FinOps.
- Espelhar `AI_VERTEX_PROJECT_ID`, `AI_VERTEX_LOCATION` (`us-central1`), `AI_VERTEX_MODEL_NAME` (`gemini-2.5-flash`) e secret opcional `GCP_VERTEX_CREDENTIALS_JSON` (`GOOGLE_APPLICATION_CREDENTIALS`) em `valuation-service/application.yml`, `helm/graham-select/` e `.github/workflows/deploy.yml`, validando com `helm lint` e `helm template`.
- Atualizar `docs/workspace.dsl` conectando o container `dataProcessor` (`valuation-service`) aos novos componentes de IA e ao sistema externo Vertex AI (ZDR).

**Never:**
- Fazer chamadas HTTP síncronas entre `backend/api` e `backend/valuation-service` (comunicação inter-serviços exclusiva via Kafka).
- Usar a API pública Gemini Developer (`generativelanguage.googleapis.com`) ou permitir retry síncrono após falha/violação CVM.
- Alterar migrations Liquibase existentes (`01`–`41`) ou expor PII/`R$` em logs ou prompts.
- Debitar `AiFinOpsRateLimitService` quando o resultado vier de fallback determinístico ou cache.
- Criar eventos Kafka ou endpoints REST (`/api/v1/ai/diagnostic`, `/feedback`) nesta história (decisão: escopo focado no núcleo + infra; orquestração Kafka/REST fica para a Story 5.4).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Inferência Vertex AI válida | Snapshot com `PETR4`, `RENDA_FIXA_MANUAL_1`, `32.1%`, `HHI 0.2500` + resposta JSON pt-BR (`32,1%`) dentro de `±0.1` (`±0.01` HHI) e sem termos CVM | Retorna `AiDiagnosticResult` com `source = LLM`, 4 blocos validados e metadados | N/A |
| Alucinação de ticker ou número | Resposta cita `MGLU3` ausente, `45.0%` vs `32.1%`, ou `HHI 0.30` vs `0.25` | `PostGenerationValidator` rejeita; aciona fallback determinístico | Log `WARN` sem PII; `source = DETERMINISTIC_FALLBACK`; sem débito de cota |
| Violação léxica CVM 19/20 | Resposta contém `"recomendo comprar PETR4"` ou `"lucro garantido"` | Bloqueia via regex `\b`, sem retry síncrono; retorna fallback determinístico | Log `WARN` com motivo CVM; `source = DETERMINISTIC_FALLBACK`; sem débito de cota |
| Resposta HTTP 200 inválida | `candidates` vazio, `finishReason != STOP`, JSON truncado ou bloco em branco | Rejeita parse/estrutura; aciona fallback determinístico sem retry | Log `ERROR` com stack trace (sem PII); `source = DETERMINISTIC_FALLBACK` |
| Timeout `> 15s`, HTTP 503 ou falha OAuth2 | Vertex AI excede `15s`, retorna `503` ou `GoogleCredentials` lança `IOException` | Interrompe chamada; ativa `DeterministicPortfolioFallbackGenerator` | Log `ERROR` com stack trace (sem PII); `source = DETERMINISTIC_FALLBACK` |
| Config ausente, payload bloqueado ou snapshot vazio | `AI_VERTEX_PROJECT_ID` vazio, `AiPayloadValidationException` ou `positions` vazio / campos opcionais `null` | Sem chamada de rede em config/payload inválido; fallback trata `positions` vazio, `null`s, quarentena e desvios negativos | Log `ERROR` em falha; `source = DETERMINISTIC_FALLBACK` |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/java/afsdigital/grahamselect/common/ai/` -- Reutilizar `SanitizedPortfolioSnapshot`, `AiPayloadValidator`, `AiDiagnosticCacheService`, `AiFinOpsRateLimitService`; adicionar `domain/model/{AiDiagnosticBlocks,AiDiagnosticResult,AiDiagnosticSource}.java`, `application/port/AiPortfolioAnalyzerPort.java` e `application/service/{PostGenerationValidator,DeterministicPortfolioFallbackGenerator}.java`.
- `backend/pom.xml` e `backend/valuation-service/pom.xml` -- Gerenciar versão explícita de `com.google.auth:google-auth-library-oauth2-http` no `<dependencyManagement>` do parent e adicioná-la ao `valuation-service`.
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/ai/` -- Criar `VertexAiConfigurationProperties.java`, `GcpAccessTokenProvider.java`, `VertexAiGeminiAdapter.java` (`RestClient` timeout `15s`, `ObjectMapper` + `AiPayloadValidator`) e `VertexAiConfiguration.java` (registra `AiPayloadValidator`, `PostGenerationValidator`, `DeterministicPortfolioFallbackGenerator` e `AiPortfolioAnalyzerPort`). `AiConfiguration` (`api`) permanece inalterada até a Story 5.4.
- `backend/valuation-service/src/main/resources/application.yml` -- Mapear `app.ai.vertex.*` (`AI_VERTEX_PROJECT_ID`, `AI_VERTEX_LOCATION`, `AI_VERTEX_MODEL_NAME`).
- `helm/graham-select/` (`values.yaml`, `templates/secret.yaml`, `templates/backend-valuation-deployment.yaml`) e `.github/workflows/deploy.yml` -- Adicionar `valuation.env.AI_VERTEX_*`, secret opcional `gcpVertexCredentialsJson` montado em `GOOGLE_APPLICATION_CREDENTIALS` para OKE e variáveis no workflow de deploy.
- `docs/workspace.dsl` -- Adicionar componentes de IA no container `dataProcessor` e sistema externo `Vertex AI (Enterprise ZDR)`.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/ai/` -- Criar `AiDiagnosticBlocks`, `AiDiagnosticSource`, `AiDiagnosticResult`, `AiPortfolioAnalyzerPort`, `PostGenerationValidator` e `DeterministicPortfolioFallbackGenerator` (tratando snapshot vazio, campos `null`, quarentena e desvios negativos) -- Garantir contrato agnóstico e validação determinística pura.
- [x] `backend/pom.xml`, `backend/valuation-service/pom.xml`, `application.yml` e `infrastructure/ai/` -- Configurar `google-auth-library-oauth2-http`, `app.ai.vertex.*`, `GcpAccessTokenProvider`, `VertexAiGeminiAdapter` e `VertexAiConfiguration` -- Integrar Vertex AI Enterprise ZDR com timeout `15s` e fallback sem retry.
- [x] `helm/graham-select/`, `.github/workflows/deploy.yml` e `docs/workspace.dsl` -- Mapear `AI_VERTEX_*`, secret `gcpVertexCredentialsJson` em `backend-valuation-deployment.yaml`, deploy workflow e atualizar `dataProcessor` no C4 -- Garantir deploy OKE e documentação.
- [x] `backend/common/src/test/java/afsdigital/grahamselect/common/ai/` e `backend/valuation-service/src/test/java/afsdigital/grahamselect/valuation/infrastructure/ai/` -- Criar `PostGenerationValidatorTest`, `DeterministicPortfolioFallbackGeneratorTest` e `VertexAiGeminiAdapterTest` (`MockRestServiceServer` cobrindo sucesso, timeout, HTTP 503, JSON inválido, falha OAuth2, CVM e alucinação) -- Cobrir 100% da matriz I/O.

**Acceptance Criteria:**
- Given `AiPortfolioAnalyzerPort` implementado por `VertexAiGeminiAdapter`, when acionado com um `SanitizedPortfolioSnapshot`, then serializa e valida via `AiPayloadValidator`, autentica via `GcpAccessTokenProvider`, chama exclusivamente o endpoint corporativo Vertex AI (`gemini-2.5-flash`, `temperature = 0.1`, `responseMimeType = application/json`) com timeout `15s` e retorna os 4 blocos validados.
- Given resposta do LLM com termo CVM, ticker inexistente, número fora de tolerância (`±0.1` / `±0.01` HHI), JSON inválido, falha OAuth2, erro HTTP ou timeout `> 15s`, when processada, then aborta sem retry síncrono e retorna o diagnóstico do `DeterministicPortfolioFallbackGenerator` sem debitar cota.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Finding | Layer | Verdict | Route | Evidence |
|---------|-------|---------|-------|----------|
| Contrato `deviationPercent` invertido em `DeterministicPortfolioFallbackGenerator` vs `PiiSanitizer.java:75` | verification-gap | high | patch | Confirmado em `PiiSanitizer.java:75` (`current.subtract(target)`): `deviation > 0` é sobrealocação e `deviation < 0` é déficit; corrigir `buildAlertas`, `buildRacionalAporte` e `DeterministicPortfolioFallbackGeneratorTest`. |
| `VertexAiConfiguration.java:933` não trata `timeout == null` nem overflow `int` | edge-case-hunter, blind-hunter | medium | patch | Confirmado em `VertexAiConfiguration.java:933`; aplicar fallback `15s` se `null` e `Math.min(Integer.MAX_VALUE, ...)`. |
| `PostGenerationValidator` ignora tickers em minúsculas (ex.: `mglu3`) e falha em faixas `30-40%` | edge-case-hunter | medium | patch | Confirmado em `PostGenerationValidator.java:276-282`; suportar case-insensitive em tickers com dígitos/prefixos e ajustar `NUMBER_PATTERN` para hífens entre números. |
| `PostGenerationValidator` rejeita siglas financeiras comuns (`KERNO`, `EV`, `CAGR`, `WACC`, `EBIT`, `ESG`, `II`, `III`) e omite termos CVM (`recomendamos`, `adquira`, `ganho garantido`) | blind-hunter | medium | patch | Confirmado em `PostGenerationValidator.java:271-278,405-424`; ampliar `EXCLUDED_ACRONYMS` e `CVM_IMPERATIVE_PATTERN` (`\bvenda\b` mantido por exigência da spec). |
| `PostGenerationValidator` rejeita `"4 blocos"`/`"CVM 19"` em prosa | blind-hunter | low (rejected) | — | Ancoragem estrita em números do snapshot é requisito da spec; `SYSTEM_INSTRUCTION` instrui o modelo a citar apenas valores do snapshot. |
| `VertexAiGeminiAdapter.java:1244-1252` lê apenas `parts.get(0)` ignorando part `thought` | edge-case-hunter | medium | patch | Confirmado em `VertexAiGeminiAdapter.java:1244-1252`; iterar `parts` ignorando `thought == true`. |
| `VertexAiGeminiAdapter.buildRequestBody` sem `maxOutputTokens` e `thinkingConfig` | blind-hunter | medium | patch | Confirmado em `VertexAiGeminiAdapter.java:1219-1223`; definir `maxOutputTokens: 2048` e `thinkingConfig.thinkingBudget: 0` para evitar estouro do timeout de `15s`. |
| `GcpAccessTokenProvider.getAccessToken()` recria `GoogleCredentials` a cada chamada | blind-hunter | medium | patch | Confirmado em `GcpAccessTokenProvider.java:28-36`; cachear instância `scoped` com `volatile`/`synchronized` para preservar cache interno de token. |
| `VertexAiConfigurationProperties.buildGenerateContentUrl()` gera `global-aiplatform` para `location=global` | blind-hunter | low | patch | Confirmado em `VertexAiConfigurationProperties.java:56-62`; tratar `location.equalsIgnoreCase("global")`. |
| `VertexAiConfiguration.vertexAiRestClient` sem `@Qualifier` | blind-hunter | low (rejected) | — | Bean já nomeado `vertexAiRestClient` e único `RestClient` no `valuation-service`. |
| `--set-string` JSON em `deploy.yml` e `application.yml` com `timeout`/`temperature` fixos | blind-hunter | low (rejected) | — | Padrão do chart Helm do repositório; `timeout=15s` e `temperature=0.1` são fixados pela spec. |
| Falta de testes para `PAYLOAD_VALIDATION_FAILED`, `candidates` vazio/4xx em `VertexAiGeminiAdapterTest`, `GcpAccessTokenProviderTest` e `VertexAiConfigurationPropertiesTest` | verification-gap, blind-hunter | medium | patch | Confirmado via `grep`; adicionar `GcpAccessTokenProviderTest`, `VertexAiConfigurationPropertiesTest` e novos casos em `VertexAiGeminiAdapterTest`. |
| `sprint-status.yaml` em `in-progress` e `## Implementation Notes` vazio | blind-hunter | false | — | Atualizados na etapa final (`step-05-present`) após conclusão do review. |

## Design Notes

- **Cliente REST vs SDK gRPC no `valuation-service`:** `RestClient` com `SimpleClientHttpRequestFactory` (`15s` connect/read timeout) + `google-auth-library-oauth2-http` (isolado em `GcpAccessTokenProvider`) evita conflitos de gRPC/Netty no profile GraalVM `native` e viabiliza testes determinísticos via `MockRestServiceServer`.

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: 0 erros/warnings, todos os testes unitários passando.
- `helm lint helm/graham-select && helm template test-graham helm/graham-select --debug > /dev/null` -- expected: 0 erros de sintaxe/template.
