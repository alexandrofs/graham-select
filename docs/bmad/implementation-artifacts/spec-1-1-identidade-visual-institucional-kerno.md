---
title: 'Story 1.1: Identidade Visual Institucional Kerno e Migração de Metadados Públicos'
type: 'feature'
created: '2026-11-22'
status: 'ready-for-dev'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/bmad/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Todas as superfícies públicas, metadados Web/PWA, aplicativos nativos (Android/iOS/macOS), notificações e configs de domínio ainda exibem *"Graham Select"* (ou defaults `"frontend"` / ícones Flutter) e o host antigo, impedindo a transição institucional para a marca **Kerno** (*"Kerno — O núcleo inteligente do seu dinheiro"*) em `kerno.afsdigital.com.br`.

**Approach:** Substituir textos institucionais, logotipos, favicons, ícones nativos, títulos, metadados SEO/PWA/OpenGraph, templates de notificação ao usuário final e configs públicas de domínio para **Kerno**, mantendo referências técnicas à metodologia de Benjamin Graham e todos os identificadores internos de infraestrutura.

## Boundaries & Constraints

**Always:**
- Exibir *"Kerno — O núcleo inteligente do seu dinheiro"* no `<title>` HTML e meta tags OpenGraph/públicas sob URL canônica `https://kerno.afsdigital.com.br/`.
- Gerar logotipo vetorial SVG geométrico próprio do Kerno (`frontend/assets/images/logo.svg`, núcleo minimalista em `#4F46E5`/`#FFFFFF`) e derivar todos os PNGs exatos para Web (`favicon.png`, `Icon-192.png`, `Icon-512.png`, `Icon-maskable-192.png`, `Icon-maskable-512.png`, `og-image.png` `1200x630`), Android (`mipmap-*/ic_launcher.png`), iOS (`AppIcon.appiconset/*.png`), macOS (`AppIcon.appiconset/app_icon_*.png`) e `frontend/assets/images/logo.png`.
- Padronizar contato institucional público para `kerno@afsdigital.com.br` em `home_page.dart:775` (removendo o telefone fictício `+55 (11) 99999-9999`), `WEBPUSH_VAPID_SUBJECT` (`mailto:kerno@afsdigital.com.br` em `application.yml:86`, `values.yaml:42` e `configmap.yaml:38`) e `openapi.yaml:9`.
- Executar corte direto para `kerno.afsdigital.com.br` na Story 1.1: `ingress.host: "kerno.afsdigital.com.br"`, `FRONTEND_URL: "https://kerno.afsdigital.com.br"`, `CORS_ALLOWED_ORIGINS: "https://kerno.afsdigital.com.br"` em `values.yaml`, `app.security.cors.allowed-origins` default `http://localhost:3000,http://localhost:5000,https://kerno.afsdigital.com.br` em `application.yml` e `--dart-define=API_BASE_URL="https://kerno.afsdigital.com.br/api/v1"` em `deploy.yml:150`.
- Preservar referências à metodologia de Benjamin Graham (*"Benjamin Graham"*, *"Zona de Compra Graham"*, `/graham-recommendations`, `GRAHAM_BUY_ZONE`, `GRAHAM_FACTOR`).
- Preservar identificadores internos: `helm/graham-select/` (diretório, `Chart.yaml:name`, `_helpers.tpl`, nomes/seletores K8s, namespace `graham-select`, imagens GHCR `graham-select-*`, `tls.secretName`), banco `grahamselect`, migrations Liquibase, pacotes `afsdigital.grahamselect.*`, `pubspec.yaml:name: frontend`, `com.example.frontend` (`applicationId`/`PRODUCT_BUNDLE_IDENTIFIER`) e `issuer-uri` Firebase.
- Manter linguagem descritiva CVM 19/20 e paridade entre `application.yml`, `helm/graham-select/` e `.github/workflows/deploy.yml`.

**Never:**
- Alterar migrations Liquibase existentes ou renomear recursos K8s/banco.
- Antecipar nesta história: Landing Page estática/SSR e `robots.txt` (Story 1.3), aceite/badge Founder no `/login` (Story 1.4), redesign `AppTheme` Light-First (Story 2.1) ou refatoração das 4 abas do `MainLayout` (Story 2.3).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Metadados Web/PWA | `index.html` e `manifest.json` | `lang="pt-BR"`, `<title>`/`og:title` = `"Kerno — O núcleo inteligente do seu dinheiro"`, `og:url` = `https://kerno.afsdigital.com.br/`, `og:image` = `https://kerno.afsdigital.com.br/og-image.png`, `theme_color` = `#4F46E5`, `name` = `"Kerno"`, favicons/ícones Kerno | N/A |
| Apps Nativos | Android, iOS e macOS | Rótulos visíveis = `"Kerno"`; `ic_launcher` e `AppIcon` atualizados mantendo `com.example.frontend` | N/A |
| Telas Web/App | `HomePage`, `LoginScreen`, `DocsScreen`, `MainLayout`, `TierComparisonCard` | Exibem `"Kerno"` e `kerno@afsdigital.com.br` sem telefone fictício nem marca `"Graham Select"`; metodologia *"Benjamin Graham"* intacta | N/A |
| Notificações | Web Push (`title == null`) e WhatsApp | Web Push usa fallback `"Kerno"` e VAPID `mailto:kerno@afsdigital.com.br`; WhatsApp usa prefixo `"Kerno ..."` sem alterar `dedupKey` | Exceções mantêm log `ERROR` com stack trace |
| Origem CORS | Requisição com `Origin: https://kerno.afsdigital.com.br` | API retorna `Access-Control-Allow-Origin` correspondente com `Allow-Credentials: true` | Origem não listada é rejeitada |

</frozen-after-approval>

## Code Map

- `frontend/web/index.html` (`L2,21,26,32`), `frontend/web/manifest.json` (`L2-8`), `frontend/web/push-sw.js` (`L1,12`) -- Metadados HTML/PWA/OG e comentário SW; manter `base href`, `'Zona de Compra Graham'` e `/graham-recommendations`.
- `frontend/web/{favicon.png,icons/*.png,og-image.png}`, `frontend/assets/images/{logo.svg,logo.png}` -- Ícones Flutter e logo `GRAHAM SELECT` substituídos por SVG geométrico Kerno + PNGs derivados; reusar `assets/icons/google_logo.png`.
- `frontend/android/app/src/main/AndroidManifest.xml` (`L3`), `frontend/android/app/src/main/res/mipmap-*`, `frontend/ios/Runner/Info.plist` (`L22-31`), `frontend/ios/Runner/Assets.xcassets/AppIcon.appiconset/*`, `frontend/macos/Runner/Configs/AppInfo.xcconfig` (`L8,14`), `frontend/macos/Runner/Assets.xcassets/AppIcon.appiconset/*` -- Rótulo `Kerno` e ícones; manter `com.example.frontend` e `CFBundleURLSchemes`.
- `frontend/lib/main.dart` (`L436`), `frontend/lib/src/core/widgets/main_layout.dart` (`L91-100`), `frontend/lib/src/features/home/presentation/pages/home_page.dart` (`L167,300,639,727,770,775`), `frontend/lib/src/features/auth/presentation/login_screen.dart` (`L63,72`), `frontend/lib/src/features/docs/presentation/pages/docs_screen.dart` (`L130`), `frontend/lib/src/features/profile/presentation/widgets/tier_comparison_card.dart` (`L37,50`), `frontend/pubspec.yaml` (`L2`), `frontend/README.md`, `frontend/Dockerfile` (`L2`) -- Textos e títulos de marca + contato `kerno@afsdigital.com.br`; manter `AppTheme` e referências a Benjamin Graham.
- `backend/api/src/main/resources/application.yml` (`L40,54,86`), `backend/api/src/main/resources/openapi.yaml` (`L3-15`), `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsConfiguration.java` (`L31`), `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java` (`L90`) -- CORS (`https://kerno.afsdigital.com.br`), `KernoCrawler/1.0`, `mailto:kerno@afsdigital.com.br`, OpenAPI e fallback Web Push; reusar `SecurityConfig.corsConfigurationSource` (`L51-69`); manter `issuer-uri`.
- `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/{EvaluateRebalanceAndGoalAlertsUseCase,NotifyDividendAlertsUseCase,SendTestWhatsappAlertUseCase}.java` -- Prefixos WhatsApp `"Graham-Select"` → `"Kerno"`; manter `EvaluateGrahamBuyZoneAlertsUseCase`, `SendTestWebPushAlertUseCase` e URLs mockadas `assets.grahamselect.com`.
- `helm/graham-select/{values.yaml,templates/configmap.yaml,Chart.yaml}`, `.github/workflows/deploy.yml` (`L1,150,205,221-222`) -- Paridade CORS/`FRONTEND_URL`/`ingress.host`/`API_BASE_URL` (`kerno.afsdigital.com.br`) e `WEBPUSH_VAPID_SUBJECT` (`mailto:kerno@afsdigital.com.br`) preservando todos os nomes/seletores K8s e `grahamselect`.
- `frontend/test/{home_page_test.dart,features/auth/presentation/login_screen_test.dart}`, `backend/api/src/test/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapterTest.java`, `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/{EvaluateRebalanceAndGoalAlertsUseCaseTest,NotifyDividendAlertsUseCaseTest,SendTestWhatsappAlertUseCaseTest}.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfigCorsTest.java` (novo) -- Cobertura de UI, notificações e CORS.

## Tasks & Acceptance

**Execution:**
- [ ] `frontend/web/index.html`, `frontend/web/manifest.json`, `frontend/web/push-sw.js` -- Atualizar `<html lang="pt-BR">`, `<title>`, meta tags OG/Twitter/canonical (`https://kerno.afsdigital.com.br/`), cores `#4F46E5`/`#F8FAFC`, nome `Kerno` e comentário SW -- Cumprir metadados SEO/PWA.
- [ ] `frontend/assets/images/{logo.svg,logo.png}`, `frontend/web/{favicon.png,icons/*,og-image.png}`, `frontend/android/app/src/main/res/mipmap-*`, `frontend/ios/Runner/Assets.xcassets/AppIcon.appiconset/*`, `frontend/macos/Runner/Assets.xcassets/AppIcon.appiconset/*` -- Criar SVG geométrico do núcleo Kerno (`#4F46E5`/`#FFFFFF`) e derivar todos os PNGs exatos substituindo logo legado e ícones Flutter -- Unificar identidade visual Web e Mobile.
- [ ] `frontend/android/app/src/main/AndroidManifest.xml`, `frontend/ios/Runner/Info.plist`, `frontend/macos/Runner/Configs/AppInfo.xcconfig`, `frontend/pubspec.yaml`, `frontend/README.md`, `frontend/Dockerfile` -- Definir rótulo nativo `Kerno` e atualizar descrições -- Exibir marca oficial nos sistemas operacionais.
- [ ] `frontend/lib/main.dart`, `frontend/lib/src/core/widgets/main_layout.dart`, `frontend/lib/src/features/home/presentation/pages/home_page.dart`, `frontend/lib/src/features/auth/presentation/login_screen.dart`, `frontend/lib/src/features/docs/presentation/pages/docs_screen.dart`, `frontend/lib/src/features/profile/presentation/widgets/tier_comparison_card.dart` -- Substituir `"Graham Select"` por `"Kerno"`, atualizar rodapé para `kerno@afsdigital.com.br` sem telefone fictício e alinhar taglines -- Remover marca legada mantendo referências a Benjamin Graham.
- [ ] `backend/api/src/main/resources/application.yml`, `backend/api/src/main/resources/openapi.yaml`, `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsConfiguration.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java`, `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/{EvaluateRebalanceAndGoalAlertsUseCase,NotifyDividendAlertsUseCase,SendTestWhatsappAlertUseCase}.java` -- Atualizar CORS (`https://kerno.afsdigital.com.br`), `KernoCrawler/1.0`, `mailto:kerno@afsdigital.com.br`, OpenAPI (`Kerno API`, `kerno@afsdigital.com.br`) e mensagens WhatsApp/Web Push para `"Kerno"` -- Garantir marca unificada em API e notificações.
- [ ] `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `helm/graham-select/Chart.yaml`, `.github/workflows/deploy.yml` -- Aplicar corte direto para `kerno.afsdigital.com.br` (`ingress.host`, `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`, `API_BASE_URL`) e `WEBPUSH_VAPID_SUBJECT` (`mailto:kerno@afsdigital.com.br`) -- Manter paridade Helm/CI-CD preservando identificadores K8s.
- [ ] `frontend/test/{home_page_test.dart,features/auth/presentation/login_screen_test.dart}`, `backend/api/src/test/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapterTest.java`, `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/{EvaluateRebalanceAndGoalAlertsUseCaseTest,NotifyDividendAlertsUseCaseTest,SendTestWhatsappAlertUseCaseTest}.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfigCorsTest.java` -- Atualizar asserções `"Kerno"`, testar fallback `title == null`, prefixos WhatsApp e origem CORS `https://kerno.afsdigital.com.br` -- Cobrir matriz I/O e regressões.

**Acceptance Criteria:**
- Given um visitante acessando qualquer rota Web pública ou autenticada sob `kerno.afsdigital.com.br`, when a página é renderizada, then título HTML, favicons, meta tags OpenGraph e UI visível exibem *"Kerno — O núcleo inteligente do seu dinheiro"* e marca **Kerno**, sem menção à plataforma *"Graham Select"*.
- Given os artefatos Android/iOS/macOS e o chart `helm/graham-select/`, when inspecionados, then rótulos/ícones nativos exibem **Kerno** enquanto diretório Helm, seletores/recursos Kubernetes, schema `grahamselect` e referências à metodologia de Benjamin Graham permanecem intactos.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: todos os testes backend aprovados (exit 0).
- `cd frontend && flutter analyze && flutter test` -- expected: zero warnings/erros no `analyze` e todos os testes aprovados.
- `helm lint helm/graham-select` -- expected: chart válido sem erros.

**Manual checks (if no CLI):**
- Verificar dimensões dos PNGs gerados (Web/Android/iOS/macOS) e `git diff` confirmando preservação dos identificadores internos K8s/Helm/Liquibase/Java.
