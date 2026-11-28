# Epic 5 Context: Diagnóstico de Carteira Assistido por IA (Kerno AI) & Conformidade LGPD

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Entregar o diagnóstico executivo de carteira do Kerno AI em linguagem natural estruturada (4 blocos analíticos) com garantia estrita de privacidade (zero dados pessoais ou valores monetários absolutos enviados ao LLM, sob contrato corporativo com Zero Data Retention), validação determinística pós-geração contra alucinações e imperativos regulatórios da CVM, além da conformidade integral com o Direito ao Esquecimento da LGPD por meio de expurgo atômico em cascata na exclusão de conta ou de CPF secundário.

## Stories

- Story 5.1: Modelo de Dados de Cache Analítico, Controle FinOps e Migrations de IA
- Story 5.2: Sanitizador LGPD de PII, Normalização Canônica e Validador Pré-Payload
- Story 5.3: Adaptador Vertex AI Gemini Enterprise ZDR e Validação Determinística Pós-Geração
- Story 5.4: Interface do Kerno AI: Cartões de Diagnóstico, Feedback 👍/👎 e Fallback Gracioso
- Story 5.5: Expurgo em Cascata LGPD (Direito ao Esquecimento) e Teste de Integração AccountDeletionIT

## Requirements & Constraints

- **Privacidade e Zero Data Retention (LGPD):** Nenhuma informação pessoal identificável (nome, CPF, e-mail, telefone, IDs nominais de conta) nem valores monetários absolutos em `R$` podem trafegar para o provedor externo de IA. O payload enviado deve conter apenas pesos percentuais, notas e tickers públicos validados contra allowlists (B3/Exterior) ou rótulos anônimos canônicos para ativos manuais (ex.: `RENDA_FIXA_MANUAL_1`, `CRIPTO_MANUAL_1`).
- **Conformidade Regulatória CVM:** A saída da IA tem caráter estritamente analítico-educativo. Um validador léxico pós-geração deve bloquear expressões imperativas ou exortativas de compra/venda ou garantia de retorno (via regex com fronteiras de palavra), abortando imediatamente a resposta do LLM sem retry síncrono e acionando o fallback determinístico local.
- **Ancoragem Numérica:** Todos os indicadores quantitativos (HHI de concentração, desvios de meta, Margem de Graham, plano de aporte) são calculados previamente pelo backend em `BigDecimal` (`RoundingMode.HALF_EVEN`). O validador pós-geração deve garantir que nenhum ticker ou número foi inventado pelo LLM (tolerância máxima de arredondamento de ±0,1 p.p. em relação ao snapshot de entrada).
- **Latência, Cache e FinOps:** Leituras em cache devem responder em `< 400ms` (P95); novas gerações via LLM em `< 10s` (P95), com timeout rígido de `15s` disparando fallback determinístico sem debitar a cota do usuário. O rate limit para contas Pro/Founder é de 5 gerações reais por dia e 30 por mês (retornando HTTP 429 com último diagnóstico cacheado ao exceder); contas na camada Free pós-trial recebem apenas o bloco Resumo Executivo via template determinístico local (`0` chamadas ao LLM).
- **Direito ao Esquecimento:** A exclusão definitiva de conta (ou remoção granular de CPF secundário na Holding Familiar) deve cancelar jobs assíncronos em execução no Kafka e executar hard-delete atômico em cascata de todas as entidades legadas e novas, sem deixar registros órfãos.

## Technical Decisions

- **Pipeline Deterministic-First e Arquitetura Hexagonal:** O caso de uso consolida o snapshot determinístico, consulta cache-aside por `portfolio_state_hash` (SHA-256 com TTL de 24h), sanitiza via `PiiSanitizer` e aciona a porta agnóstica `AiPortfolioAnalyzerPort`, implementada por `VertexAiGeminiAdapter` no `valuation-service`.
- **Provedor e Parâmetros de IA:** Integração exclusiva com Google Cloud Vertex AI sob contrato Enterprise ZDR, modelo pinado `gemini-2.5-flash`, temperatura `0.1` e saída estruturada em JSON Schema com 4 blocos (`resumoExecutivo`, `pontosFortes`, `alertasDeRiscoEConcentracao`, `racionalDoPlanoDeAporte`).
- **Persistência e Migrations Liquibase:** Migrations existentes são imutáveis; criar novos changesets incrementais e idempotentes (compartilhados entre `api` e `valuation-service`) para `tb_ai_diagnostic_cache` e `tb_ai_usage_counter`. Ao referenciar tabelas legadas no SQL ou no expurgo, conferir os nomes reais sem prefixo `tb_` nos changelogs Liquibase. Entidades com `UUID` devem usar persistência textual (`CHAR(36)`/`VARCHAR`).
- **Expurgo LGPD e Testes de Integração:** Atualizar `UserDataPurgeAdapter` (que expurga tanto por `users.id` numérico quanto por `google_sub` legado) incluindo as novas tabelas de IA e do Diagrama do Cerrado, além de limpar chaves de rate limit e caches em memória. Validar obrigatoriamente via `mvn test -pl api -Dtest=AccountDeletionIT` (suíte `*IT` não roda no `mvn clean test` padrão).
- **Paridade de Infraestrutura e Observabilidade:** Refletir todas as novas variáveis (`AI_VERTEX_PROJECT_ID`, `AI_VERTEX_LOCATION`, `AI_VERTEX_MODEL_NAME`, quotas diária/mensal) em `application.yml`, `helm/graham-select/` (`values.yaml`, `configmap.yaml`, `secret.yaml`) e `.github/workflows/deploy.yml`. Propagar `X-Correlation-ID` e registrar logs estruturados (`INFO` em entradas sem PII/tokens; `WARN`/`ERROR` com stack trace em falhas/violações).

## UX & Interaction Patterns

- **Aba Kerno AI (`/kerno-ai`):** Renderiza os 4 cartões `KernoAiDiagnosticCard` (*Resumo Executivo*, *Pontos Fortes*, *Alertas de Risco e Concentração*, *Racional do Plano de Aporte*). Valores monetários em `R$` aparecem apenas em chips/cabeçalhos determinísticos locais (respeitando o Modo Privacidade `R$ ••••••`), enquanto o corpo narrativo da IA exibe exclusivamente percentuais, notas e tickers.
- **Transparência, Feedback e Cota:** Na primeira utilização, exibir aviso claro sobre o processamento percentual anonimizado sob ZDR. Incluir carimbo de data/hora do snapshot, contador de cota diária (`X/5`), botões de avaliação útil/não-útil (👍/👎, área mínima `48x48dp`) sem comentário obrigatório, e rodapé permanente `CvmRegulatoryFooter`.
- **Acessibilidade e Degradação Graciosa:** Suportar `textScaler` até `2.0x` com reflow vertical (proibido `TextOverflow.ellipsis` em cifras e tickers) e numerais tabulares. Em timeout (`> 15s`), violação CVM ou conta Free, exibir o painel determinístico local com aviso não-bloqueante.
- **Exclusão de Conta (`/profile`):** Fluxo no hub de Conta & Privacidade com confirmação explícita e irreversível antes de acionar o expurgo LGPD.

## Cross-Story Dependencies

- **Entre histórias do Épico 5:** Story 5.1 (tabelas de cache/uso e rate limit) e Story 5.2 (`PiiSanitizer` e allowlists) são pré-requisitos para a Story 5.3 (`VertexAiGeminiAdapter` e validadores); Story 5.4 consome o endpoint `/api/v1/ai/diagnostic` e métricas de feedback entregues até a 5.3; Story 5.5 fecha o ciclo garantindo que as tabelas da 5.1 (e dos épicos anteriores) sejam purgadas no `UserDataPurgeAdapter` e verificadas no `AccountDeletionIT`.
- **Com outros épicos:** O snapshot de entrada do Kerno AI depende das posições multi-classe/multi-CPF (Épico 2), das metas e notas do Diagrama do Cerrado (Épico 3) e do plano de aporte calculado pelo GPS com Margem de Graham (Épico 4), além de respeitar o tier comercial (`FOUNDER_LIFETIME`, `PRO_TRIAL`, `FREE`) definido no Épico 1.
