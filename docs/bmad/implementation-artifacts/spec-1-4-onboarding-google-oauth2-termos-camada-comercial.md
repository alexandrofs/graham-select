---
title: 'Story 1.4: Onboarding com Google OAuth2, Aceite de Termos e Concessão de Camada Comercial'
type: 'feature'
created: '2026-11-24'
status: 'done'
route: 'dispatch'
baseline_commit: '247beb834de500ee0601b024d8a2ae135b23372d'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/bmad/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A tela `/login` ainda exibe texto estático sem links clicáveis para `/terms` e `/privacy`, o backend não registra `terms_accepted_at` no provisionamento OAuth2, e a primeira tela autenticada (`MainLayout` / `_TopBar`) não carrega nem exibe o selo exclusivo `Founder` (`FOUNDER_LIFETIME`) ou a pílula `Trial Pro: X dias restantes` (`PRO_TRIAL` de 90 dias).

**Approach:** Atualizar o fluxo de login (`/login`) com botão *"Continuar com Google"* e aviso claro com links diretos para `/terms` e `/privacy`, adicionar `terms_accepted_at` (migration Liquibase idempotente + `User` + `OAuth2UserProvisioningConverter` + `openapi.yaml` + `UserController`), e carregar/exibir o selo comercial (`Founder` / `Trial Pro: X dias restantes`) na barra superior do shell autenticado (`MainLayout`) e no perfil (`UserProfile`).

## Boundaries & Constraints

**Always:**
- Na tela `/login` (`LoginScreen`), exibir o botão *"Continuar com Google"* acompanhado de texto claro e links diretos clicáveis para `/terms` e `/privacy` (abertos via `url_launcher`, com contraste WCAG AA/AAA, alvo de toque acessível e suporte a `TextScaler` `1.0x–2.0x` sem truncamento/overflow).
- Após autenticação bem-sucedida no `LoginScreen`, navegar diretamente para `/dashboard` (evitando redirecionamento redundante por `/`, que no Nginx serve `landing.html`).
- Criar migration Liquibase incremental e idempotente (`38-add-user-terms-accepted-at.yaml` com `preConditions` `onFail: MARK_RAN`, registrada em `db.changelog-master.yaml`) adicionando `terms_accepted_at DATETIME NULL` na tabela `users`.
- Registrar `termsAcceptedAt = LocalDateTime.now(ZoneOffset.UTC)` em `OAuth2UserProvisioningConverter` na criação de novos usuários (e preencher se nulo em logins subsequentes caso ainda não registrado), expondo `termsAcceptedAt` em `openapi.yaml` (`UserProfile`), `UserController` e `UserProfile` (Dart).
- No shell autenticado (`MainLayout`), garantir o carregamento inicial de `ProfileProvider.fetchProfile()` quando `state == ProfileState.idle && profile == null` e exibir o componente `CommercialPlanBadge` (em `features/founders`) no `_TopBar`:
  - `FOUNDER_LIFETIME`: selo Ouro Founder *"Founder"* (fundo `#FEF3C7`, texto `#92400E`, borda `#F59E0B`, ícone + rótulo textual).
  - `PRO_TRIAL`: pílula *"Trial Pro: X dias restantes"* (ex.: *"Trial Pro: 90 dias restantes"*).
  - `FREE`: pílula *"Gratuito"*.
- Manter linguagem estritamente descritiva e matemática (CVM 19/20) e preservar armazenamento de credenciais exclusivamente via `flutter_secure_storage`.

**Never:**
- Alterar migrations Liquibase existentes (`01` a `37`) ou duplicar lógica de alocação de vagas Founders já implementada na Story 1.2 (`FounderQuotaService`).
- Exibir `"Graham Select"` como nome da plataforma ou usar `TextOverflow.ellipsis` em rótulos comerciais/financeiros.
- Bloquear o login caso `/terms` ou `/privacy` não possa ser aberto em ambiente de teste sem navegador (tratar falha de `launchUrl` graciosamente com `SnackBar`).
- Introduzir gateway de pagamento, checkout ou IAP neste ciclo.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Links legais no `/login` | Usuário toca em `Termos de Uso` ou `Política de Privacidade` no `LoginScreen` | Abre `/terms` ou `/privacy` via `url_launcher` mantendo formulário de login intacto | Se `launchUrl` retornar `false` ou lançar erro, exibe `SnackBar` informativo sem travar a tela |
| Login OAuth2 com vaga Founder disponível | 1º login via Google com `founder_slots_remaining > 0` | Backend cria `User` com `commercialPlan = FOUNDER_LIFETIME`, `termsAcceptedAt = now(UTC)`; `_TopBar` exibe badge Ouro Founder *"Founder"* | Falha no `signInWithGoogle()` exibe `SnackBar` de erro e restaura botão |
| Login OAuth2 após esgotamento do lote | 1º login via Google com `founder_slots_remaining == 0` | Backend cria `User` com `commercialPlan = PRO_TRIAL`, `trialExpiresAt = now(UTC) + 90d`, `termsAcceptedAt = now(UTC)`; `_TopBar` exibe *"Trial Pro: 90 dias restantes"* (ou `daysRemaining` atual) | N/A |
| Login recorrente com `termsAcceptedAt` nulo | Usuário legado existente realiza login JWT | `OAuth2UserProvisioningConverter` preenche `termsAcceptedAt = now(UTC)` preservando `commercialPlan` e `founder_slots_remaining` | N/A |
| Usuário em `FREE` após expiração | `UserProfile` com `commercialPlan = FREE` | `_TopBar` exibe pílula *"Gratuito"* sem erro ou truncamento | Se `fetchProfile()` falhar, `_TopBar` omite o badge silenciosamente sem quebrar a navegação |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (`L72-73`) e `38-add-user-terms-accepted-at.yaml` (novo) -- Migration idempotente adicionando `terms_accepted_at` em `users`.
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java` (`L50-75`) -- Campo `termsAcceptedAt` (`terms_accepted_at`) e getter/setter.
- `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java` (`L70-115`) -- Persistir `termsAcceptedAt = LocalDateTime.now(ZoneOffset.UTC)` em `provisionNewUser` e preencher em `updateExistingIfNeeded` se nulo.
- `backend/api/src/main/resources/openapi.yaml` (`L910-937`) e `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java` (`L131-168`) -- Adicionar `termsAcceptedAt` (`date-time`) no schema `UserProfile` e mapeamento UTC.
- `frontend/lib/src/features/auth/presentation/login_screen.dart` (`L16-150`) -- Botão *"Continuar com Google"*, texto de consentimento com links clicáveis para `/terms` e `/privacy` via `url_launcher`, e navegação para `/dashboard`.
- `frontend/lib/src/features/profile/data/models/user_profile_model.dart` (`L1-74`) -- Campos `commercialPlan`, `trialExpiresAt` e `termsAcceptedAt` com `fromJson`/`toJson`/igualdade.
- `frontend/lib/src/features/founders/presentation/widgets/commercial_plan_badge.dart` (novo) -- Selo Ouro Founder (*"Founder"*), pílula *"Trial Pro: X dias restantes"* e *"Gratuito"* com tríade acessível.
- `frontend/lib/src/core/widgets/main_layout.dart` (`L7-34`, `L261-316`) -- Disparar `ProfileProvider.fetchProfile()` quando `idle` e exibir `CommercialPlanBadge` no `_TopBar`.
- `frontend/lib/src/features/profile/presentation/widgets/tier_badge.dart` e `tier_comparison_card.dart` -- Alinhar exibição para `FOUNDER_LIFETIME` e Trial Pro de `90 dias`.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/resources/db/changelog/38-add-user-terms-accepted-at.yaml`, `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`, `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java` -- Adicionar coluna idempotente `terms_accepted_at` em `users` e mapear `termsAcceptedAt` na entidade `User` -- Persistir timestamp UTC de aceite aos Termos/Privacidade.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java`, `backend/api/src/main/resources/openapi.yaml`, `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java` -- Registrar `termsAcceptedAt` no provisionamento/atualização OAuth2 e expor no `UserProfile` OpenAPI -- Garantir auditabilidade do aceite sem fricção adicional.
- [x] `frontend/lib/src/features/auth/presentation/login_screen.dart` -- Atualizar botão para *"Continuar com Google"*, adicionar links clicáveis acessíveis para `/terms` e `/privacy` via `url_launcher` e navegar para `/dashboard` após login -- Cumprir transparência CVM/LGPD no onboarding.
- [x] `frontend/lib/src/features/profile/data/models/user_profile_model.dart`, `frontend/lib/src/features/founders/presentation/widgets/commercial_plan_badge.dart`, `frontend/lib/src/core/widgets/main_layout.dart`, `frontend/lib/src/features/profile/presentation/widgets/tier_badge.dart`, `frontend/lib/src/features/profile/presentation/widgets/tier_comparison_card.dart` -- Parsear `commercialPlan`/`trialExpiresAt`/`termsAcceptedAt`, criar `CommercialPlanBadge`, carregar perfil no `MainLayout` e exibir o badge na `_TopBar` -- Exibir status *"Founder"* ou *"Trial Pro: X dias restantes"* na primeira tela autenticada.
- [x] `backend/api/src/test/java/afsdigital/grahamselect/api/auth/UserAutoProvisioningExtendedIT.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/user/UserProfileIT.java`, `frontend/test/features/auth/presentation/login_screen_test.dart`, `frontend/test/features/founders/presentation/widgets/commercial_plan_badge_test.dart`, `frontend/test/core/widgets/main_layout_commercial_badge_test.dart` -- Testar persistência/exposição de `termsAcceptedAt`, links `/terms` e `/privacy`, acessibilidade (`TextScaler 2.0x`) e renderização de `Founder` / `Trial Pro: 90 dias restantes` no `_TopBar` -- Cobrir matriz I/O e evitar regressões.

**Acceptance Criteria:**
- Given um usuário acessando `/login`, when visualiza o card de autenticação e clica em *"Continuar com Google"*, then existem links diretos para `/terms` e `/privacy` com aviso claro de aceite e o fluxo OAuth2 autentica e redireciona para `/dashboard`.
- Given um usuário recém-cadastrado com vaga disponível (`FOUNDER_LIFETIME`) ou após esgotamento (`PRO_TRIAL` 90 dias), when acessa a primeira tela autenticada, then o backend persiste `terms_accepted_at` em UTC e a `_TopBar` exibe imediatamente o selo Ouro *"Founder"* ou a pílula *"Trial Pro: 90 dias restantes"*.

## Implementation Notes

### File List
- `backend/common/src/main/resources/db/changelog/38-add-user-terms-accepted-at.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java`
- `backend/api/src/main/resources/openapi.yaml`
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/auth/UserAutoProvisioningExtendedIT.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/user/UserProfileIT.java`
- `frontend/lib/src/features/auth/presentation/login_screen.dart`
- `frontend/lib/src/features/profile/data/models/user_profile_model.dart`
- `frontend/lib/src/features/founders/presentation/widgets/commercial_plan_badge.dart`
- `frontend/lib/src/core/widgets/main_layout.dart`
- `frontend/lib/src/features/profile/presentation/pages/profile_page.dart`
- `frontend/lib/src/features/profile/presentation/widgets/tier_badge.dart`
- `frontend/lib/src/features/profile/presentation/widgets/tier_comparison_card.dart`
- `frontend/test/features/auth/presentation/login_screen_test.dart`
- `frontend/test/features/founders/presentation/widgets/commercial_plan_badge_test.dart`
- `frontend/test/core/widgets/main_layout_commercial_badge_test.dart`
- `frontend/test/features/profile/data/models/user_profile_model_test.dart`
- `frontend/test/features/profile/presentation/pages/profile_page_test.dart`
- `frontend/test/features/admin/presentation/pages/admin_dashboard_page_test.dart`
- `frontend/test/home_page_test.dart`
- `frontend/test/main_router_public_docs_test.dart`
- `docs/bmad/implementation-artifacts/spec-1-4-onboarding-google-oauth2-termos-camada-comercial.md`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`
- `docs/bmad/implementation-artifacts/deferred-work.md`

## Spec Change Log

## Review Triage Log

| Camada | Localização | Achado | Veredito | Rota | Evidência |
|---|---|---|---|---|---|
| `edge-case-hunter` | `frontend/lib/src/features/founders/presentation/widgets/commercial_plan_badge.dart:9-47` | `PRO_TRIAL` expirado (`tier == FREE`) ou `daysRemaining < 0` exibe badge `Trial Pro` | `medium` | `patch` | `_resolvePlan()` não verifica `tier == 'FREE'` quando `commercialPlan == 'PRO_TRIAL'` e não faz clamp `>= 0` em `daysRemaining`. |
| `edge-case-hunter` | `frontend/lib/src/features/profile/presentation/pages/profile_page.dart:169` | `commercialPlan == ""` ignora fallback `?? profile.tier` e exibe `GRATUITO` | `low` | `patch` | `profile.commercialPlan ?? profile.tier` repassa string vazia ao `TierBadge`; usar `isNotEmpty`. |
| `blind-hunter` / `verification-gap` | `backend/api/uploads/1de150d3-02a8-4cbf-b33d-de5fabf7776a_nota_xp.pdf` | Artefato de teste residual incluído no diff | `medium` | `patch` | Arquivo PDF gerado por teste de integração foi removido do working tree. |
| `blind-hunter` | `frontend/lib/main.dart`, `frontend/lib/src/core/widgets/main_layout.dart` | `ProfileProvider` global não é resetado em logout/troca de usuário | `medium` | `defer` | `ProfileProvider` já era instanciado no `MultiProvider` raiz sem listener de auth antes desta story; comportamento de não re-tentar após erro no `_TopBar` atende à matriz I/O. |
| `blind-hunter` | `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java:80-83` | Backfill silencioso de `termsAcceptedAt` sem log `INFO` | `low` | `patch` | `updateExistingIfNeeded` preenche `termsAcceptedAt` quando nulo conforme spec, mas sem `log.info` de auditoria. |
| `blind-hunter` | `frontend/lib/src/features/auth/presentation/login_screen.dart:43` | `launchUrl` sem `LaunchMode.externalApplication` pode abrir na mesma aba SPA | `low` | `patch` | Passar `mode: LaunchMode.externalApplication` garante abertura externa para `/terms` e `/privacy`. |
| `blind-hunter` | `frontend/lib/src/features/auth/presentation/login_screen.dart:45-62` | Bloco `SnackBar` duplicado em `_openLegalDocument` | `low` | `patch` | `if (!launched)` e `catch (_)` exibem o mesmo `SnackBar`; unificar fluxo. |
| `blind-hunter` | `frontend/lib/src/features/founders/presentation/widgets/commercial_plan_badge.dart:36` | Pluralização `1 dias restantes` e fallback para `trialExpiresAt` | `low` | `patch` | Ajustar para singular quando `days == 1` e derivar de `trialExpiresAt` quando `daysRemaining` for nulo. |
| `blind-hunter` | `frontend/lib/src/features/profile/presentation/pages/profile_page.dart:112` | `isTrial` verifica apenas `profile.tier` sem `commercialPlan` | `low` | `patch` | Verificar `profile.tier.toUpperCase() == 'TRIAL' || profile.commercialPlan?.toUpperCase() == 'PRO_TRIAL'`. |
| `blind-hunter` | `frontend/test/core/widgets/main_layout_commercial_badge_test.dart` | Falta cobertura de viewport estreita (`360x640`) + `ADMIN` + `TextScaler 2.0` | `low` | `patch` | Adicionar caso de teste em `main_layout_commercial_badge_test.dart`. |
| `blind-hunter` | `backend/api/src/test/java/afsdigital/grahamselect/api/user/UserProfileIT.java` | `/api/v1/users/me` testado apenas com `FREE` | `low` | `patch` | Adicionar asserções para `PRO_TRIAL` e `FOUNDER_LIFETIME`. |
| `blind-hunter` / `verification-gap` | `frontend/test/features/profile/presentation/pages/profile_page_test.dart`, `login_screen_test.dart` | Falta teste de `TierBadge` para `FOUNDER_LIFETIME`/`PRO_TRIAL` e exceção em `onLaunchUrl` | `medium` | `patch` | Adicionar testes de widget para `FOUNDER_LIFETIME`, `PRO_TRIAL` e exceção em `onLaunchUrl`. |
| `verification-gap` | `frontend/test/features/founders/presentation/widgets/commercial_plan_badge_test.dart` | Fallback de `commercialPlan` nulo/vazio para `tier` sem teste | `medium` | `patch` | Adicionar casos com `commercialPlan: null` e `''` no `commercial_plan_badge_test.dart`. |

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: todos os testes unitários dos módulos `common`, `api` e `valuation-service` aprovados (exit 0).
- `cd backend && mvn test -pl api -Dtest=UserAutoProvisioningExtendedIT,UserProfileIT,AccountDeletionIT` -- expected: testes de integração H2/Liquibase aprovados (exit 0).
- `cd frontend && flutter analyze && flutter test` -- expected: zero warnings/erros e todos os testes aprovados.
