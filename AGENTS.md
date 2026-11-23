<!-- bmad:context -->
<!-- Verified 2026-11-22 against 2678253. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

## graham-select

Plataforma de gestão de carteiras e valuation (em transição de marca pública para Kerno). Backend multi-módulo em Java 21 / Spring Boot 3.4 (`backend/common`, `backend/api`, `backend/valuation-service`) com MySQL, Liquibase e Kafka; frontend Flutter/Dart (`frontend/`); produção em Kubernetes OKE via Helm (`helm/`). Planejamento ativo (Kerno 2.0) fica em `docs/bmad/planning-artifacts/` (subpastas `*-2026-11-21`), acompanhamento em `docs/bmad/implementation-artifacts/sprint-status.yaml` e diagrama C4 em `docs/workspace.dsl`.

## Política

- Abra sempre uma branch atualizada a partir da `main` (ou base designada) antes de implementar (`git checkout -b feature/<nome> main`); nunca trabalhe direto na `main`.
- Use Conventional Commits (`feat:`, `fix:`, `chore:`) e mantenha histórias coesas (~20–25 arquivos como alerta para fatiar).
- Em modo autônomo/YOLO, execute apenas tarefas de baixo risco ou documentação; mudanças em autenticação, ingestão, cálculos financeiros, CORS, portas, banco ou produção exigem aprovação humana e validação E2E antes do merge na `main`.
- Não marque uma história como `done` sem marcar todas as tarefas `[x]` e preencher a `File List` com os arquivos criados/alterados; ao criar histórias, cruze cenários de exceção/circuit breaker de `docs/bmad/planning-artifacts/epics.md` e valide contratos de API contra os controllers existentes.
- Nunca altere migrations Liquibase já aplicadas em `backend/common/src/main/resources/db/changelog/`; toda mudança de esquema exige nova migration incremental e idempotente (com `preConditions` ou `validCheckSum` quando pertinente).
- Sempre que uma nova tabela ou entidade armazenar dados de usuário/PII (LGPD / FR5), atualize `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/persistence/UserDataPurgeAdapter.java` e cubra a tabela em `backend/api/src/test/java/afsdigital/grahamselect/api/user/AccountDeletionIT.java`.
- Sempre que adicionar variáveis de ambiente ou propriedades em `backend/api/src/main/resources/application.yml`, `backend/valuation-service/src/main/resources/application.yml` ou no frontend, reflita-as em `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `helm/graham-select/templates/secret.yaml` e `.github/workflows/deploy.yml`.
- Nunca crie Dockerfiles manuais para serviços do `backend/`; empacote o backend via Spring Boot Cloud Native Buildpacks (`frontend/Dockerfile` é o único Dockerfile da aplicação).
- Nunca renomeie identificadores internos legados (`helm/graham-select/`, recursos Kubernetes, banco `grahamselect`, pacotes `afsdigital.grahamselect.*`) durante o rebranding para Kerno; altere apenas superfícies e metadados públicos.
- Registre log `INFO` com contexto (sem PII/tokens) em toda entrada de endpoint REST e consumer Kafka; ao lançar ou capturar exceções, registre em `ERROR` com stack trace e nunca use `catch` vazio.
- Configure origens CORS exclusivamente via `${app.security.cors.allowed-origins}` em `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfig.java`, sem `*` com `allowCredentials(true)`; não replique o padrão legado de `WebConfig.java`.
- Em todo upload de arquivo, sanitize o nome com `Paths.get(filename).getFileName()` para impedir path traversal; preserve suporte a preflight `OPTIONS` em endpoints SSE (`/api/v1/notifications/stream`).
- Atualize `docs/workspace.dsl` sempre que adicionar ou alterar componentes/containers arquiteturais.
- Quando o usuário pedir para abrir um PR, crie-o de forma autônoma via `gh` ou via `POST https://api.github.com/repos/{owner}/{repo}/pulls` (UTF-8, Bearer token); se `gh` não estiver no PATH no Windows, obtenha o token com ``"protocol=https`nhost=github.com`n`n" | git credential fill`` em vez de devolver link manual.

## Onde estão as coisas

- Backend Maven multi-módulo: `backend/` (`common`, `api`, `valuation-service`); migrations em `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`.
- Contrato OpenAPI gerado em build: `backend/api/src/main/resources/openapi.yaml` (o `maven-antrun-plugin` em `backend/api/pom.xml` remove stubs de `Admin`, `Dividends`, `OpenFinance` e `Tax`, que usam controllers manuais).
- Frontend Flutter: `frontend/lib/src/features/<feature>/{data,domain,presentation}`, roteamento/providers em `frontend/lib/main.dart`, cliente HTTP padrão em `frontend/lib/src/core/api/api_client.dart`.
- Infraestrutura de produção (OKE): `helm/graham-select/`, `helm/kafka/` e `.github/workflows/deploy.yml`; infra local em `docker-compose.yml` e `start-app.sh`.
- Vetores compartilhados de paridade Java × Dart: `backend/common/src/test/resources/goals/time-to-goal-parity-vectors.json`.
- Planejamento Kerno 2.0 (arquitetura, PRD, UX): subpastas `*-2026-11-21` em `docs/bmad/planning-artifacts/` (os `.md` soltos na raiz de `planning-artifacts/` são da geração 1.0).
- Ignore os `.spec.ts` em `tests/` ao rodar testes locais — são scaffolds Playwright sem `package.json` nem runner configurado (apenas `tests/api/user-tier-api-test.sh` é executável avulso).

## Como rodar e verificar

- Runtime exigido pelos manifestos e CI: Java 21 (`backend/pom.xml`), Dart `^3.11.0` (`frontend/pubspec.yaml`), Flutter `3.41.1` (`.github/workflows/deploy.yml`).
- Não existe `./mvnw`; rode `mvn` a partir de `backend/`.
- Após implementar ou corrigir review, rode obrigatoriamente a suíte completa e análise estática com zero erros/warnings: `mvn clean test` em `backend/`, e `flutter analyze` + `flutter test` em `frontend/` (novas regras de negócio e widgets exigem testes unitários/widget cobrindo casos de borda).
- `mvn clean test` e `mvn verify` hoje pulam os 30 `*IT.java` (sem `maven-failsafe-plugin` nos POMs); execute-os explicitamente quando tocar persistência, API ou LGPD: `mvn test -pl api -Dtest=*IT` (H2, sem Docker; ex.: `-Dtest=AccountDeletionIT`) e `mvn test -pl valuation-service -Dtest=*IT` (exige Docker/Testcontainers).
- Antes de gerar imagem Buildpack (`mvn -B spring-boot:build-image -pl api -am` ou `-pl valuation-service -am`), rode `mvn clean install -DskipTests` em `backend/` para instalar `common` no repositório local.
- Ao alterar `helm/`, valide como o CI: `helm lint helm/kafka`, `helm lint helm/graham-select` e `helm template test-graham helm/graham-select --debug`.

## Convenções fora do padrão

- Siga Clean Code, SOLID e Clean Architecture pragmática: domínio, use cases e ports ficam em `backend/common` (entidades/repositórios JPA compartilhados são permitidos em `common`), enquanto controllers, adapters específicos e wiring `@Configuration` ficam em `backend/api` e `backend/valuation-service`.
- Nunca faça chamadas HTTP síncronas entre `api` e `valuation-service`; comunique-os exclusivamente via tópicos Apache Kafka (`valuation-service` não expõe endpoints de usuário, apenas `/actuator/health` para probes).
- Em todas as operações de data/hora no backend, use `ZoneOffset.UTC` explicitamente (`OffsetDateTime.now(ZoneOffset.UTC)` ou `LocalDateTime.now(ZoneOffset.UTC)`), exceto onde o calendário de pregão B3 exigir `America/Sao_Paulo`.
- Em novos endpoints autenticados, obtenha o usuário exclusivamente via `Authentication.getName()` (ID numérico único `users.id` preenchido por `OAuth2UserProvisioningConverter`), propagando-o em todas as queries e eventos; controllers legados usam `jwt.getSubject()` (`google_sub`), por isso `UserDataPurgeAdapter` expurga pelos dois IDs.
- No frontend, use `provider` para estado, `go_router` para rotas e `ApiClient` (`dio`) em novas features em vez do `http.Client` legado.

## Armadilhas conhecidas

- Cálculos financeiros (Java e Dart): valide entradas com `Double.isFinite` / `.isFinite`, aplique `clamp` em percentuais (0–100) e valores negativos de patrimônio/aporte, evite `0 * Infinity` e divisão por zero, e converta `NaN`/`Infinity` em estado explícito (ex.: `isUnreachable`) antes da UI/JSON.
- Lógica duplicada entre Java e Dart (ex.: `TimeToGoalCalculator`): corrija sempre ambas as implementações e adicione o caso em `backend/common/src/test/resources/goals/time-to-goal-parity-vectors.json` (lido diretamente por `frontend/test/features/goals/domain/services/time_to_goal_calculator_parity_test.dart`).
- Entidades JPA com `UUID` no MySQL/Liquibase: force persistência textual (`CHAR(36)`/`VARCHAR`), pois o mapeamento binário padrão já quebrou queries e migrations em produção.
- Esquemas nos planos Kerno 2.0 (`ARCHITECTURE-SPINE.md`, `epic-1-context.md`) citam nomes ilustrativos como `tb_user`; confira sempre os nomes reais sem prefixo (`users`, `trades`, `financial_goals`) nos changelogs Liquibase antes de escrever SQL ou migrations.
- Garanta migrations Liquibase estritamente idempotentes (`preConditions`/locks seguros), pois tanto `backend/api` quanto `backend/valuation-service` executam o mesmo `db.changelog-master.yaml` contra o mesmo banco MySQL.

<!-- /bmad:context -->
