---
title: 'Story 1.1: Identidade Visual Institucional Kerno e Migração de Metadados Públicos'
type: 'feature'
created: '2026-11-22'
status: 'done'
baseline_commit: '79362d838797d8b3c7b61a307d4693ffd7f2e196'
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
- [x] `frontend/web/index.html`, `frontend/web/manifest.json`, `frontend/web/push-sw.js` -- Atualizar `<html lang="pt-BR">`, `<title>`, meta tags OG/Twitter/canonical (`https://kerno.afsdigital.com.br/`), cores `#4F46E5`/`#F8FAFC`, nome `Kerno` e comentário SW -- Cumprir metadados SEO/PWA.
- [x] `frontend/assets/images/{logo.svg,logo.png}`, `frontend/web/{favicon.png,icons/*,og-image.png}`, `frontend/android/app/src/main/res/mipmap-*`, `frontend/ios/Runner/Assets.xcassets/AppIcon.appiconset/*`, `frontend/macos/Runner/Assets.xcassets/AppIcon.appiconset/*` -- Criar SVG geométrico do núcleo Kerno (`#4F46E5`/`#FFFFFF`) e derivar todos os PNGs exatos substituindo logo legado e ícones Flutter -- Unificar identidade visual Web e Mobile.
- [x] `frontend/android/app/src/main/AndroidManifest.xml`, `frontend/ios/Runner/Info.plist`, `frontend/macos/Runner/Configs/AppInfo.xcconfig`, `frontend/pubspec.yaml`, `frontend/README.md`, `frontend/Dockerfile` -- Definir rótulo nativo `Kerno` e atualizar descrições -- Exibir marca oficial nos sistemas operacionais.
- [x] `frontend/lib/main.dart`, `frontend/lib/src/core/widgets/main_layout.dart`, `frontend/lib/src/features/home/presentation/pages/home_page.dart`, `frontend/lib/src/features/auth/presentation/login_screen.dart`, `frontend/lib/src/features/docs/presentation/pages/docs_screen.dart`, `frontend/lib/src/features/profile/presentation/widgets/tier_comparison_card.dart` -- Substituir `"Graham Select"` por `"Kerno"`, atualizar rodapé para `kerno@afsdigital.com.br` sem telefone fictício e alinhar taglines -- Remover marca legada mantendo referências a Benjamin Graham.
- [x] `backend/api/src/main/resources/application.yml`, `backend/api/src/main/resources/openapi.yaml`, `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsConfiguration.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java`, `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/{EvaluateRebalanceAndGoalAlertsUseCase,NotifyDividendAlertsUseCase,SendTestWhatsappAlertUseCase}.java` -- Atualizar CORS (`https://kerno.afsdigital.com.br`), `KernoCrawler/1.0`, `mailto:kerno@afsdigital.com.br`, OpenAPI (`Kerno API`, `kerno@afsdigital.com.br`) e mensagens WhatsApp/Web Push para `"Kerno"` -- Garantir marca unificada em API e notificações.
- [x] `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `helm/graham-select/Chart.yaml`, `.github/workflows/deploy.yml` -- Aplicar corte direto para `kerno.afsdigital.com.br` (`ingress.host`, `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`, `API_BASE_URL`) e `WEBPUSH_VAPID_SUBJECT` (`mailto:kerno@afsdigital.com.br`) -- Manter paridade Helm/CI-CD preservando identificadores K8s.
- [x] `frontend/test/{home_page_test.dart,features/auth/presentation/login_screen_test.dart}`, `backend/api/src/test/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapterTest.java`, `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/{EvaluateRebalanceAndGoalAlertsUseCaseTest,NotifyDividendAlertsUseCaseTest,SendTestWhatsappAlertUseCaseTest}.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfigCorsTest.java` -- Atualizar asserções `"Kerno"`, testar fallback `title == null`, prefixos WhatsApp e origem CORS `https://kerno.afsdigital.com.br` -- Cobrir matriz I/O e regressões.

**Acceptance Criteria:**
- Given um visitante acessando qualquer rota Web pública ou autenticada sob `kerno.afsdigital.com.br`, when a página é renderizada, then título HTML, favicons, meta tags OpenGraph e UI visível exibem *"Kerno — O núcleo inteligente do seu dinheiro"* e marca **Kerno**, sem menção à plataforma *"Graham Select"*.
- Given os artefatos Android/iOS/macOS e o chart `helm/graham-select/`, when inspecionados, then rótulos/ícones nativos exibem **Kerno** enquanto diretório Helm, seletores/recursos Kubernetes, schema `grahamselect` e referências à metodologia de Benjamin Graham permanecem intactos.

### Review Findings

- [x] [Review][Patch] Validação de URL de fallback em notificações Web Push [`backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java:90`]
- [x] [Review][Patch] Declaração explícita de purpose nos ícones padrão do manifest PWA e ajuste de apple-touch-icon [`frontend/web/manifest.json:11-22`, `frontend/web/index.html:47`]
- [x] [Review][Patch] Adicionar cabeçalho de seção da Story 1.1 em deferred-work.md [`docs/bmad/implementation-artifacts/deferred-work.md:68`]
- [x] [Review][Defer] Auditar permissão incondicional de loopback CORS em SecurityConfig sob profile prod [`backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfig.java:56-60`] — deferred: pre-existing, já documentado em deferred-work.md
- [x] [Review][Defer] Espelhar parâmetro CRAWLER_USER_AGENT no chart Helm [`helm/graham-select/values.yaml`] — deferred: pre-existing, default em código funciona sem override
- [x] [Review][Defer] Ícone full-color referenciado como badge na notificação Web Push [`frontend/web/push-sw.js:16`] — deferred: pre-existing, criação de asset monocromático fora do escopo da Story 1.1
- [x] [Review][Defer] Configuração de ícone adaptativo Android (mipmap-anydpi-v26/ic_launcher.xml) [`frontend/android/app/src/main/res/`] — deferred: pre-existing, escopo da Story 1.1 cobriu todas as densidades de raster PNG
- [x] [Review][Defer] Workflow legado frontend-ci.yml mantendo referências ao Render [`.github/workflows/frontend-ci.yml:114-121`] — deferred: pre-existing, fora do escopo do deploy OKE
- [x] [Review][Defer] Reuso de assets/icons/google_logo.png no botão Google da LoginScreen [`frontend/lib/src/features/auth/presentation/login_screen.dart:1062`] — deferred: deferido explicitamente para Story 1.4
- [x] [Review][Defer] Execução de helm lint e helm template em CI [`helm/graham-select/`] — deferred: ambiente local sem binário helm, validado via YAML e delegado ao CI

#### Rejected
- Status inconsistente entre spec (done) e sprint-status (review): rejeitado por regra de triagem BMAD (o fix editaria a própria spec sob revisão).
- Notificação push com 'Zona de Compra Graham' em push-sw.js:12: falso-positivo (metodologia de Benjamin Graham expressamente preservada por restrição da spec).
- Ausência de logo no _TopBar mobile em main_layout.dart: falso-positivo (redesenho multi-superfície mobile pertence expressamente à Story 2.3).
- SecurityConfigCorsTest estendendo BaseRepositoryIT: baixa severidade (preferência de arquitetura de teste, sem defeito funcional).
- Falta de cobertura para origens múltiplas separadas por vírgula em SecurityConfigCorsTest: baixa severidade (melhoria de teste secundária, origem oficial 100% coberta).

## Implementation Notes

- **File List:**
  - Web/PWA & Assets: `frontend/assets/images/logo.svg` (novo), `frontend/assets/images/logo.png`, `frontend/web/index.html`, `frontend/web/manifest.json`, `frontend/web/push-sw.js`, `frontend/web/favicon.png`, `frontend/web/og-image.png` (novo), `frontend/web/icons/Icon-{192,512}.png`, `frontend/web/icons/Icon-maskable-{192,512}.png`
  - Nativo (Android/iOS/macOS) & Pacote: `frontend/android/app/src/main/AndroidManifest.xml`, `frontend/android/app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png`, `frontend/ios/Runner/Info.plist`, `frontend/ios/Runner/Assets.xcassets/AppIcon.appiconset/*.png`, `frontend/macos/Runner/Configs/AppInfo.xcconfig`, `frontend/macos/Runner/Assets.xcassets/AppIcon.appiconset/*.png`, `frontend/macos/Runner.xcodeproj/project.pbxproj`, `frontend/macos/Runner.xcodeproj/xcshareddata/xcschemes/Runner.xcscheme`, `frontend/pubspec.yaml`, `frontend/README.md`, `frontend/Dockerfile`
  - Frontend Dart/Flutter: `frontend/lib/main.dart`, `frontend/lib/src/core/widgets/main_layout.dart`, `frontend/lib/src/features/home/presentation/pages/home_page.dart`, `frontend/lib/src/features/auth/presentation/login_screen.dart`, `frontend/lib/src/features/docs/presentation/pages/docs_screen.dart`, `frontend/lib/src/features/profile/presentation/widgets/tier_comparison_card.dart`
  - Backend: `backend/api/src/main/resources/application.yml`, `backend/api/src/main/resources/openapi.yaml`, `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsConfiguration.java`, `backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java`, `backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/{EvaluateRebalanceAndGoalAlertsUseCase,NotifyDividendAlertsUseCase,SendTestWhatsappAlertUseCase}.java`
  - Helm & CI/CD: `helm/graham-select/values.yaml`, `helm/graham-select/templates/configmap.yaml`, `helm/graham-select/Chart.yaml`, `.github/workflows/deploy.yml`
  - Testes: `frontend/test/home_page_test.dart`, `frontend/test/features/auth/presentation/login_screen_test.dart`, `frontend/test/kerno_branding_metadata_test.dart` (novo), `frontend/test/core/widgets/main_layout_test.dart` (novo), `frontend/test/features/docs/presentation/pages/docs_screen_test.dart` (novo), `frontend/test/features/profile/presentation/widgets/tier_comparison_card_test.dart` (novo), `backend/api/src/test/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapterTest.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsConfigurationTest.java` (novo), `backend/common/src/test/java/afsdigital/grahamselect/common/notifications/application/usecase/{EvaluateRebalanceAndGoalAlertsUseCaseTest,NotifyDividendAlertsUseCaseTest,SendTestWhatsappAlertUseCaseTest}.java`, `backend/api/src/test/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfigCorsTest.java` (novo)
- **Nota:** O CLI `helm` não está instalado localmente neste ambiente Windows (e o daemon Docker encontra-se inativo); a sintaxe dos arquivos YAML foi validada via `yaml.safe_load`, restando `helm lint`/`helm template` para execução no pipeline de CI ou ambiente Linux com Helm. Além disso, `DocsScreen._buildSectionCard` recebeu `Expanded` no título do card para corrigir overflow horizontal em viewports de `800px` identificado pelo teste de widget.

## Spec Change Log

## Review Triage Log

| ID / Camada | Local | Veredito | Rota | Evidência |
|---|---|---|---|---|
| VG-1 (`verification-gap`) | `frontend/test/home_page_test.dart:51` | `medium` | `patch` | Pré-verificado: `find.text('Graham Select')` exige igualdade exata e não detecta regressão da marca legada em frases multi-palavra do hero, depoimento, CTA e copyright. |
| VG-2 (`verification-gap`) | `frontend/test/{core/widgets/main_layout_test,features/auth/presentation/login_screen_test,home_page_test}.dart` | `medium` | `patch` | Pré-verificado: nenhum dos três testes de widget verifica a renderização do `Image.asset('assets/images/logo.png')` adicionado/substituído neste diff. |
| VG-3 (`verification-gap`) | `frontend/test/home_page_test.dart` / `kerno_branding_metadata_test.dart:189` | `medium` | `patch` | Pré-verificado: `MaterialApp.title` em runtime (`main.dart:436`) só é verificado por busca textual no arquivo-fonte, sem asserção no widget montado. |
| VG-4 (`verification-gap`) | `CorporateEventsConfiguration.java:31`, `application.yml:54` | `medium` | `patch` | Pré-verificado: `CvmOfficialCrawlerAdapterTest` injeta `"TestAgent/1.0"` manualmente e não testa o wiring Spring de `KernoCrawler/1.0` a partir de `application.yml` e do default `@Value`. |
| VG-5 (`verification-gap`) | `backend/api/src/main/resources/application.yml:86` | `medium` | `patch` | Pré-verificado: `VapidWebPushGatewayAdapterTest` instancia o adapter com string literal hardcoded e os testes Spring usam `webpush.provider=mock`, deixando o default `mailto:kerno@afsdigital.com.br` de `application.yml` sem teste de binding. |
| VG-6 (`verification-gap` Other) | `backend/api/uploads/*_nota_xp.pdf` | `high` | `patch` + `defer` | Confirmado em `backend/api/uploads/` (2 PDFs gerados por `BrokerageNoteUploadControllerTest` e incluídos no index por `git add -N .`): remover os PDFs e desindexar `backend/api/uploads/` (`patch`); diferir o isolamento pré-existente de `application-test.yml`/`.gitignore` (`defer`). |
| EC-1 (`edge-case-hunter`) | `VapidWebPushGatewayAdapter.java:90` | `low` | `patch` | Confirmado: `title != null` permite `""` ou espaços vazios no JSON, divergindo da checagem falsy em `push-sw.js:12` (que cairia no fallback `'Zona de Compra Graham'`). |
| EC-2 (`edge-case-hunter`) | `frontend/lib/src/features/auth/presentation/login_screen.dart:53-75` | `medium` | `patch` | Confirmado: o bloco do logo (`56px + 16px`) em `Column` sem `SingleChildScrollView` eleva a altura para ~362px e causa `RenderFlex` overflow em viewports `< 380px` ou escala `2.0x`. |
| EC-3 (`edge-case-hunter`) | `frontend/macos/Runner/Configs/AppInfo.xcconfig:8`, `project.pbxproj:67,134,220,391,405,419`, `Runner.xcscheme:18,34,69,86` | `high` | `patch` | Confirmado: `PRODUCT_NAME = Kerno` gera `Kerno.app`, mas `project.pbxproj` (`TEST_HOST` e `PBXFileReference`) e `Runner.xcscheme` (`BuildableName`) ainda apontam para `frontend.app`/`frontend`, quebrando `RunnerTests` e launch no Xcode macOS. |
| BH-1 (`blind-hunter`) | `backend/api/uploads/0f23ea7a-..._nota_xp.pdf`, `.gitignore`, `application-test.yml` | `high` | `patch` + `defer` | Mesma causa raiz de VG-6: limpar `backend/api/uploads/` do index/disco (`patch`) e diferir a configuração pré-existente de `app.upload.storage-path` em `application-test.yml`/`.gitignore` (`defer`). |
| BH-2 (`blind-hunter`) | `frontend/lib/src/core/widgets/main_layout.dart:13-33,260-310` | `low` | `defer` | Confirmado, porém pré-existente: `_TopBar` em mobile (`< 900px`) já não exibia marca antes desta história (sem vazamento de `"Graham Select"`); o shell multi-superfície mobile pertence à Story 2.3. |
| BH-3 (`blind-hunter`) | `frontend/web/og-image.png` | `low` | `patch` | Confirmado via medição de pixels: o texto `"kerno.afsdigital.com.br"` na pílula tem padding assimétrico (`32px` esq vs `41px` dir; `18px` topo vs `8px` base). Recentrando o texto na imagem (`patch`); rejeitada a criação de script gerador permanente no repositório (desnecessário ao build). |
| BH-4 (`blind-hunter`) | `frontend/web/index.html:47,50`, `frontend/web/icons/Icon-192.png` | `medium` | `patch` | Confirmado via inspeção de pixels: `Icon-192.png` passou de `RGB` opaco para `RGBA` com cantos `(0,0,0,0)`, o que gera cantos pretos em `apple-touch-icon` no iOS; apontar `apple-touch-icon` para `icons/Icon-maskable-192.png` (cantos `#4F46E5` opacos) corrige a regressão. |
| BH-5 (`blind-hunter`) | `frontend/android/app/src/main/res/`, `frontend/web/push-sw.js:16` | `low` | `defer` | Pré-existente: o template Android já não possuía `mipmap-anydpi-v26` e `push-sw.js:16` já usava `/icons/Icon-192.png` como `badge` antes desta história. |
| BH-6 (`blind-hunter`) | `VapidWebPushGatewayAdapter.java:90`, `SendTestWebPushAlertUseCase.java:45-47` | `low` / `false` | `patch` (blank title) + `reject` (`SendTestWebPushAlertUseCase`) | Parte `title.isBlank()` confirmada (mesma raiz de EC-1 → `patch` + testes blank `title` e `url == null`). Parte `SendTestWebPushAlertUseCase` é `false`: não contém `"Graham Select"` e refere-se a `"alertas de compra Graham"` (metodologia explicitamente preservada pela spec). |
| BH-7 (`blind-hunter`) | `frontend/assets/images/logo.svg`, `frontend/pubspec.yaml:78` | `low` | `reject` | `logo.svg` (~550 bytes) está no caminho exato exigido pelo bloco congelado (`frontend/assets/images/logo.svg`); impacto no bundle é desprezível e restringir `pubspec.yaml` para arquivos individuais cria atrito de manutenção. |
| BH-8 (`blind-hunter`) | `SecurityConfigCorsTest.java`, `SecurityConfig.java:56-60` | `false` (renomear `*IT`) / `medium` (`localhost:*`) | `reject` (`*IT`) + `defer` (`SecurityConfig`) | Renomear `SecurityConfigCorsTest` para `*IT` é `false` pois `mvn clean test` pula `*IT.java`, removendo a validação CORS da suíte padrão e da matriz I/O. Já os padrões incondicionais `http://localhost:*`/`127.0.0.1:*`/`10.0.2.2:*` em `SecurityConfig.java:56-60` sob `prod` são pré-existentes (`defer`). |
| BH-9 (`blind-hunter`) | `application.yml:54`, `CorporateEventsConfiguration.java:31`, `helm/`, `CvmOfficialCrawlerAdapter.java` | `medium` (teste) / `low` (duplicação/Helm) | `patch` (teste) + `defer` (Helm/duplicação) | Falta de teste de wiring de `KernoCrawler/1.0` é mesma raiz de VG-4 (`patch`). Variável `CRAWLER_USER_AGENT` e fallback duplicado em `@Value` já existiam antes da Story 1.1 (`defer`). |
| BH-10 (`blind-hunter`) | `login_screen.dart`, `main_layout.dart`, `home_page.dart` | `low` | `patch` (`excludeFromSemantics`) + `defer` (`google_logo.png`/link `mailto:`) | Os 3 `Image.asset('assets/images/logo.png')` adicionados neste diff ao lado de `Text('Kerno')` devem usar `excludeFromSemantics: true` (`patch`). `Icons.account_circle` no botão Google (Story 1.4) e texto simples no email do rodapé (Story 1.3) são pré-existentes (`defer`). |
| BH-11 (`blind-hunter`) | `sprint-status.yaml:39`, `helm/`, `.github/workflows/frontend-ci.yml` | `false` (`sprint-status`) / `maybe-false` (`helm lint`) / `medium` (`frontend-ci.yml`) | `reject` (`sprint-status`) + `defer` (`helm lint`, `frontend-ci.yml`) | `sprint-status.yaml` em `in-progress` durante revisão é comportamento esperado do workflow (transição ocorre no Step 5 → `false`). `helm lint`/`helm template` não pôde ser executado localmente sem `helm`/Docker (`maybe-false` → `defer`, validável no CI). `.github/workflows/frontend-ci.yml` com hook Render é pré-existente (`defer`). |
| BH-12 (`blind-hunter`) | `frontend/test/kerno_branding_metadata_test.dart:42-88` | `low` | `patch` | Confirmado: o teste novo não valida explicitamente os atributos `og:title`, `og:description`, `og:site_name` e `twitter:*` em `index.html` (`patch`). Varredura cruzada de `../backend/openapi.yaml` dentro de `frontend/test/` rejeitada para manter a suíte frontend autossuficiente. |

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: todos os testes backend aprovados (exit 0).
- `cd frontend && flutter analyze && flutter test` -- expected: zero warnings/erros no `analyze` e todos os testes aprovados.
- `helm lint helm/graham-select` -- expected: chart válido sem erros.

**Manual checks (if no CLI):**
- Verificar dimensões dos PNGs gerados (Web/Android/iOS/macOS) e `git diff` confirmando preservação dos identificadores internos K8s/Helm/Liquibase/Java.
