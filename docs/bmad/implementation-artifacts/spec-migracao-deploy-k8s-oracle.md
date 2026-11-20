---
title: 'Migração do Deploy para Kubernetes OKE (Oracle Cloud) via Helm e GitHub Actions'
type: 'chore'
created: '2026-11-20T20:30:00-03:00'
baseline_commit: '26207ca6fba35f156a177212c192abe91e6c308c'
status: 'done'
context:
  - 'docs/bmad/planning-artifacts/architecture.md'
  - 'AGENTS.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O deploy atual do Graham Select depende de instâncias no Render e pipelines manuais/incompletos, sem paridade operacional com o cluster Kubernetes OKE (Oracle Cloud) já utilizado com sucesso pelo projeto SGCE.

**Approach:** Criar os charts Helm padronizados para o Graham Select (API, Valuation Service, Frontend Flutter NGINX e Ingress TLS Let's Encrypt), conectar ao Kafka hospedado em seu próprio namespace dedicado (`kafka`) via DNS de serviço do cluster (`kafka.kafka.svc.cluster.local:9092`), implementar os artefatos de conteinerização do frontend e estruturar o pipeline GitHub Actions (`deploy.yml`) idêntico ao padrão do SGCE com build multi-arch (ARM64) e deploy contínuo no OKE.

## Boundaries & Constraints

**Always:**
- Seguir o padrão arquitetural de microsserviços do SGCE (Helm 3, cert-manager com `letsencrypt-prod`, Ingress NGINX, GHCR com imagens multi-arch / linux/arm64).
- O broker Kafka deve ficar isolado em um namespace dedicado chamado `kafka`, acessível pelos microsserviços do Graham Select via DNS interno (`kafka.kafka.svc.cluster.local:9092` ou configurável via values).
- Configurar probes HTTP (`livenessProbe` e `readinessProbe`) para todos os pods com graceful fallback.
- Isolar todas as configurações sensíveis via Kubernetes Secrets (`DB_PASSWORD`, `GOOGLE_CLIENT_SECRET`, `BRAPI_TOKEN`, etc.).
- Garantir compatibilidade com os nós ARM64 do cluster OKE (Ampere A1).

**Ask First:**
- Definição do subdomínio exato no Ingress (ex: `graham.afsdigital.com.br` vs `grahamselect.afsdigital.com.br`).
- Se os manifests do Kafka no namespace dedicado `kafka` devem ser incluídos neste repositório em uma pasta `helm/kafka` ou sub-template dedicado.

**Never:**
- Não commitar credenciais ou kubeconfigs no repositório.
- Não alterar migrations Liquibase existentes (respeitar imutabilidade do banco).
- Não quebrar os scripts de execução local (`docker-compose.yml` e `start-app.sh`).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Helm Lint & Dry-run | `helm lint helm/graham-select` | 0 erros e templates renderizados com sucesso | Falha no CI bloqueia build/push |
| Conexão Cross-Namespace | API/Valuation conecta a `kafka.kafka.svc.cluster.local:9092` | Produtor e consumidor Kafka comunicam-se entre namespaces no cluster | Reconexão automática com backoff caso o broker Kafka esteja inicializando |
| Push na branch `main` | Commit mergeado na main | Workflow compila backend/frontend, publica no GHCR e aplica helm upgrade no OKE | Rollback automático se o rollout falhar via `--wait --timeout 10m` |
| Roteamento Ingress | Requisição para `/api/*` e `/` | Roteia `/api` para o backend-api e `/` para o frontend SPA | Nginx retorna index.html em rotas desconhecidas (SPA) |

</frozen-after-approval>

## Code Map

- `helm/graham-select/Chart.yaml` -- Metadados do Chart Helm do Graham Select
- `helm/graham-select/values.yaml` -- Valores padrão e parametrizações (DB MDS, Kafka em `kafka.kafka.svc.cluster.local:9092`, Frontend, API, Valuation)
- `helm/graham-select/templates/_helpers.tpl` -- Nomes padronizados, labels e templates compartilhados
- `helm/graham-select/templates/ingress.yaml` -- Regras de Ingress NGINX e TLS Cert-Manager
- `helm/graham-select/templates/configmap.yaml` -- Variáveis de ambiente não-sensíveis dos serviços (apontando para o Kafka no namespace `kafka`)
- `helm/graham-select/templates/secret.yaml` -- Declaração dos secrets Opaque do cluster
- `helm/graham-select/templates/frontend-deployment.yaml` -- Deployment do Frontend Flutter Web
- `helm/graham-select/templates/frontend-service.yaml` -- Service ClusterIP do Frontend
- `helm/graham-select/templates/backend-api-deployment.yaml` -- Deployment do microsserviço de API Spring Boot
- `helm/graham-select/templates/backend-api-service.yaml` -- Service ClusterIP da API
- `helm/graham-select/templates/backend-valuation-deployment.yaml` -- Deployment do microsserviço de Valuation
- `helm/kafka/` -- Chart/manifestos opcionais para provisionamento do Kafka KRaft no namespace `kafka`
- `frontend/Dockerfile` -- Dockerfile NGINX Alpine para servir o bundle Flutter Web compilado
- `frontend/nginx.conf` -- Configuração NGINX com SPA fallback, compressão gzip e endpoint `/healthz`
- `.github/workflows/deploy.yml` -- Workflow CI/CD completo com validação, build multi-arch ARM64, push GHCR e deploy Helm no OKE

## Tasks & Acceptance

**Execution:**
- [x] `frontend/nginx.conf` -- Criar configuração SPA com gzip e `/healthz` -- Essencial para probes e roteamento Flutter Web
- [x] `frontend/Dockerfile` -- Criar Dockerfile de produção NGINX Alpine -- Padronizar imagem leve para o container
- [x] `helm/graham-select/Chart.yaml` -- Criar manifesto do Helm Chart -- Identificação do pacote no Helm 3
- [x] `helm/graham-select/values.yaml` -- Configurar valores padrão para Oracle OKE com Kafka configurado para `kafka.kafka.svc.cluster.local:9092` -- Centralizar parametrização cross-namespace
- [x] `helm/graham-select/templates/_helpers.tpl` -- Criar templates de labels e nomes canônicos -- Padronização de recursos k8s
- [x] `helm/graham-select/templates/configmap.yaml` & `secret.yaml` -- Configurar injeção de envs e secrets da API e Valuation -- Observabilidade e segurança
- [x] `helm/kafka/` -- Criar manifests Helm dedicados para provisionar o Kafka KRaft no namespace dedicado `kafka` -- Garantir ambiente Kafka desacoplado
- [x] `helm/graham-select/templates/backend-api-deployment.yaml` & `backend-api-service.yaml` -- Configurar Deployment e Service da API -- Servir tráfego HTTP e SSE
- [x] `helm/graham-select/templates/backend-valuation-deployment.yaml` -- Configurar Deployment do Valuation Service -- Processar valuations assíncronas do Kafka
- [x] `helm/graham-select/templates/frontend-deployment.yaml` & `frontend-service.yaml` -- Configurar Deployment e Service do Frontend -- Disponibilizar UI do sistema
- [x] `helm/graham-select/templates/ingress.yaml` -- Configurar Ingress com cert-manager e roteamento unificado -- Acesso público seguro HTTPS
- [x] `.github/workflows/deploy.yml` -- Criar pipeline automatizado de validação, build ARM64, push e deploy Helm com garantia do namespace `kafka` -- Automação contínua idêntica ao SGCE
- [x] `.github/workflows/deploy-backend.yml` -- Desativar ou substituir o pipeline legado do Render -- Evitar conflito de deploys

**Acceptance Criteria:**
- Given o namespace dedicado `kafka`, when o Kafka estiver em execução, then a API e o Valuation Service no namespace `graham-select` devem conectar com sucesso através do endpoint `kafka.kafka.svc.cluster.local:9092`.
- Given o chart `helm/graham-select`, when executado `helm lint helm/graham-select` e `helm template helm/graham-select`, then a validação deve passar sem nenhum erro de sintaxe.
- Given o workflow `.github/workflows/deploy.yml`, when acionado em commit na branch `main` ou via `workflow_dispatch`, then deve validar testes, compilar imagens Docker para arquitetura `linux/arm64`, publicá-las no GHCR e realizar o rollout no cluster OKE da Oracle.

## Design Notes

- **Namespace Dedicado para Kafka:** O Apache Kafka opera isolado no namespace `kafka`. A comunicação intra-cluster com o namespace `graham-select` ocorre via Fully Qualified Domain Name (FQDN) interno do Kubernetes CoreDNS: `kafka.kafka.svc.cluster.local:9092`.
- **Arquitetura ARM64:** O cluster OKE da Oracle utiliza instâncias Ampere A1 (`aarch64`). O backend Spring Boot e o frontend NGINX devem ser construídos especificamente para `linux/arm64` no pipeline do GitHub Actions.
- **Ingress & TLS:** Utiliza a Ingress Controller NGINX e Cert-Manager `letsencrypt-prod` do SGCE, aproveitando o DNS wildcard `*.afsdigital.com.br` já provisionado no OCI DNS.

## Verification

**Commands:**
- `helm lint helm/graham-select` -- expected: `1 chart(s) linted, 0 chart(s) failed`
- `helm template graham-select helm/graham-select --debug > /dev/null` -- expected: `exit code 0`
- `flutter analyze` na pasta `frontend` -- expected: `No issues found!`
- `mvn test-compile` na pasta `backend` -- expected: `BUILD SUCCESS`

## Suggested Review Order

**Automação de CI/CD e Pipeline OKE**

- Pipeline GitHub Actions principal com validação, build multi-arch ARM64 e deploy Helm no OKE.
  [`deploy.yml:1`](../../../.github/workflows/deploy.yml#L1)

- Desativação do deploy antigo no Render para evitar conflitos operacionais.
  [`deploy-backend.yml.disabled:1`](../../../.github/workflows/deploy-backend.yml.disabled#L1)

**Helm Charts da Aplicação (Graham Select)**

- Parametrizações de recursos, banco OCI MDS, Kafka cross-namespace e Ingress Let's Encrypt.
  [`values.yaml:1`](../../../helm/graham-select/values.yaml#L1)

- Roteamento HTTP/HTTPS com Ingress NGINX e TLS Cert-Manager para subdomínio Graham.
  [`ingress.yaml:1`](../../../helm/graham-select/templates/ingress.yaml#L1)

- Injeção das variáveis de ambiente e FQDN do Kafka para API e Valuation.
  [`configmap.yaml:1`](../../../helm/graham-select/templates/configmap.yaml#L1)

- Deployment e Service do microsserviço de API Spring Boot com health probes.
  [`backend-api-deployment.yaml:1`](../../../helm/graham-select/templates/backend-api-deployment.yaml#L1)

- Deployment do microsserviço assíncrono de Valuation consumidor de tópicos Kafka.
  [`backend-valuation-deployment.yaml:1`](../../../helm/graham-select/templates/backend-valuation-deployment.yaml#L1)

- Deployment e Service do Frontend Flutter Web servido via container NGINX.
  [`frontend-deployment.yaml:1`](../../../helm/graham-select/templates/frontend-deployment.yaml#L1)

**Namespace Dedicado e Broker Kafka**

- Manifestos do Kafka KRaft isolado no namespace dedicado `kafka`.
  [`deployment.yaml:1`](../../../helm/kafka/templates/deployment.yaml#L1)

**Conteinerização do Frontend Flutter**

- Configuração de SPA, compressão gzip e health check `/healthz` para o Flutter Web.
  [`nginx.conf:1`](../../../frontend/nginx.conf#L1)

- Dockerfile de produção NGINX Alpine leve para servir os artefatos compilados da web.
  [`Dockerfile:1`](../../../frontend/Dockerfile#L1)

