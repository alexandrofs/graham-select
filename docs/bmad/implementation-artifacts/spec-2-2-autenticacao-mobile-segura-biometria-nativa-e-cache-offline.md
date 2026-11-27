---
title: 'Story 2.2: Autenticação Mobile Segura, Biometria Nativa e Cache Offline Criptografado'
type: 'feature'
created: '2026-11-27'
status: 'done'
baseline_commit: '59e2e9a62d95fdc3069c9722985d66f7c19f7dc9'
route: 'dispatch'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** No aplicativo móvel do Kerno, `AuthRepository` persiste apenas `jwt_token` sem suporte a `access_token`/`refresh_token` estruturados, não há gate biométrico nativo (`local_auth`) na abertura ou retorno após inatividade (> 2 min), não existe cache local criptografado para renderizar o último snapshot válido do Dashboard offline, e o botão de logout em `ProfilePage` está sem implementação (`TODO`).

**Approach:** Criar a camada `core/security/` com serviço de biometria nativa (`BiometricAuthService` via `local_auth` com fallback para credencial do SO e janela de 2 minutos), cofre de sessão (`SecureSessionStorage` sobre `flutter_secure_storage`) e cache criptografado de snapshot do Dashboard (`EncryptedPortfolioCache`), integrando-os ao ciclo de vida em `main.dart`, `AuthRepository`, `PortfolioProvider`, `DashboardPage` e `ProfilePage`, além das configurações nativas Android/iOS.

## Boundaries & Constraints

**Always:**
- Persistir `access_token`, `refresh_token` (e `jwt_token` legado) e o snapshot offline do Dashboard exclusivamente via `flutter_secure_storage` (iOS Keychain / Android EncryptedSharedPreferences), nunca em `SharedPreferences` ou texto puro.
- Exigir desbloqueio biométrico nativo (`local_auth`, `biometricOnly: false` para fallback de PIN/credencial do SO) na abertura com sessão ativa em mobile (`!kIsWeb`) e ao retornar do background após mais de `2 minutos` de inatividade, antes de exibir dados patrimoniais.
- Em falha de rede/offline no `DashboardPage`, após biometria aprovada, renderizar o último snapshot válido do cache criptografado para o escopo ativo (`tagId`), exibindo timestamp discreto da última sincronização em horário local formatado a partir de UTC ISO-8601.
- Ao acionar logout (`signOut`) ou exclusão de conta, limpar integralmente `flutter_secure_storage` e todos os caches locais, mantendo compatibilidade de assinatura com `MockAuthRepository` e rotas públicas (`/`, `/login`, `/docs`).
- Manter zero erros/warnings em `flutter analyze` e cobrir todos os cenários da Matriz I/O com testes unitários e de widget.

**Never:**
- Implementar `FLAG_SECURE` no Android, máscara de snapshot no *App Switcher* do iOS ou `PrivacyToggleButton` (escopo da Story 2.4).
- Alterar o shell de navegação (`MainLayout`), criar `BottomNavigationBar` ou alterar contratos/paths REST do backend (`/portfolios/summary`, `/portfolios/custody`, `/portfolios/evolution`).
- Exibir dados patrimoniais atrás ou durante o bloqueio biométrico, ou ignorar falha/cancelamento da biometria liberando acesso silencioso.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Persistência de tokens | Login OAuth2 bem-sucedido retorna `idToken`/`accessToken` e `refreshToken` | Tokens gravados em `flutter_secure_storage` e mantidos em memória | Erro de escrita propaga sem gravar em texto puro |
| Gate biométrico inicial e > 2 min | App mobile abre autenticado ou retorna de `paused` após `> 120s` | Exibe tela de bloqueio Kerno e aciona `local_auth` (`biometricOnly: false`) | Cancelamento/falha mantém bloqueio com botões "Tentar novamente" e "Sair" |
| Retorno rápido <= 2 min | App mobile retorna de `paused` com `<= 120s` decorridos | Não solicita nova biometria; mantém sessão desbloqueada | Se relógio retroceder (`duration < 0`), exige biometria por segurança |
| Fallback offline com snapshot | `getSummary`/`getCustodyPositions` falha por rede e existe snapshot válido | `PortfolioProvider` carrega snapshot do cache criptografado e marca `isOfflineCache = true` com `lastSyncedAt` | Sem snapshot em cache, exibe estado de erro amigável sem quebrar a tela |
| Logout / exclusão de conta | Usuário aciona "Sair da Conta" ou limpeza de sessão | Limpa tokens, cache criptografado, estado de `PortfolioProvider` e redireciona para `/login` | Falha parcial no Google/Firebase Sign-Out ainda limpa `flutter_secure_storage` |

</frozen-after-approval>

## Code Map

- `frontend/pubspec.yaml` (L30–48) -- Adicionar dependência `local_auth` mantendo `flutter_secure_storage: ^9.2.2`.
- `frontend/android/app/src/main/AndroidManifest.xml` (L1–45) -- Adicionar permissão `android.permission.USE_BIOMETRIC`.
- `frontend/android/app/src/main/kotlin/com/example/frontend/MainActivity.kt` (L1–5) -- Migrar de `FlutterActivity` para `FlutterFragmentActivity` exigido por `local_auth`.
- `frontend/ios/Runner/Info.plist` (L1–83) -- Adicionar chave `NSFaceIDUsageDescription`.
- `frontend/lib/src/core/security/biometric_auth_service.dart` -- Criar serviço de autenticação biométrica (`local_auth`) e controle de timeout de background (`> 2 minutos`, com `DateTime Function()` injetável para testes).
- `frontend/lib/src/core/security/encrypted_portfolio_cache.dart` -- Criar repositório de cache local criptografado sobre `FlutterSecureStorage` para snapshot de `PortfolioSummaryModel` + `CustodyPositionModel` + `lastSyncedAt` (UTC ISO-8601) por `userId`/`tagId`, com `clearAll()`.
- `frontend/lib/src/core/security/biometric_lock_gate.dart` -- Criar wrapper de UI (`WidgetsBindingObserver`) que bloqueia rotas autenticadas em mobile até desbloqueio biométrico, sem afetar `/`, `/login`, `/docs` ou `kIsWeb`.
- `frontend/lib/src/features/auth/data/auth_repository.dart` (L7–97) -- Persistir `access_token`, `refresh_token` e `jwt_token` via `FlutterSecureStorage`, injetar callback/serviço de limpeza de cache opcional no `signOut()` e manter compatibilidade de construtor para testes existentes.
- `frontend/lib/src/features/portfolio/data/models/portfolio_summary_model.dart` (L3–19) -- Adicionar `toJson()` para serialização do snapshot offline.
- `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart` (L12–120) -- Integrar `EncryptedPortfolioCache` opcional (grava snapshot no sucesso; faz fallback no erro de rede expondo `isOfflineCache` e `lastSyncedAt`; limpa no `reset()`).
- `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart` (L61–100) -- Exibir banner/aviso discreto com `lastSyncedAt` formatado quando `provider.isOfflineCache` for `true`.
- `frontend/lib/src/features/profile/presentation/pages/profile_page.dart` (L333–345) -- Conectar botão "Sair da Conta" a `AuthRepository.signOut()` + limpeza de caches/providers.
- `frontend/lib/main.dart` (L225–456) -- Instanciar `BiometricAuthService` e `EncryptedPortfolioCache`, injetar no `AuthRepository`/`PortfolioProvider` e envolver o conteúdo autenticado com `BiometricLockGate`.

## Tasks & Acceptance

**Execution:**
- [x] `frontend/pubspec.yaml`, `AndroidManifest.xml`, `MainActivity.kt`, `Info.plist` -- Adicionar `local_auth` e configurar permissões/FragmentActivity nativas -- Habilitar biometria nativa iOS/Android.
- [x] `frontend/lib/src/core/security/biometric_auth_service.dart` -- Implementar `BiometricAuthService` com `local_auth` (`biometricOnly: false`) e regra de inatividade `> 2 minutos` -- Centralizar política de desbloqueio e facilitar mocks.
- [x] `frontend/lib/src/core/security/encrypted_portfolio_cache.dart` e `portfolio_summary_model.dart` -- Implementar `toJson()` em `PortfolioSummaryModel` e `EncryptedPortfolioCache` sobre `FlutterSecureStorage` -- Persistir e expurgar snapshot do Dashboard criptografado.
- [x] `frontend/lib/src/features/auth/data/auth_repository.dart` e `profile_page.dart` -- Persistir `access_token`/`refresh_token` no `FlutterSecureStorage`, limpar tokens e caches no `signOut()`/`clearSessionData()` e ligar botão "Sair da Conta" -- Garantir cofre seguro e wipe completo.
- [x] `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart` e `dashboard_page.dart` -- Salvar snapshot no sucesso, ativar fallback offline em falha de rede e exibir timestamp discreto da última sincronização -- Garantir continuidade offline após biometria.
- [x] `frontend/lib/src/core/security/biometric_lock_gate.dart` e `frontend/lib/main.dart` -- Integrar `BiometricLockGate` com `WidgetsBindingObserver` e DI em `GrahamSelectApp` sem quebrar rotas públicas ou testes de roteador -- Proteger dados patrimoniais na abertura e retorno do background.
- [x] `frontend/test/core/security/` e `frontend/test/` -- Adicionar testes unitários e de widget cobrindo todos os cenários da Matriz I/O e atualizar mocks afetados (`auth_repository_test.dart`, `portfolio_repository_impl_test.dart`, `portfolio_provider_test.dart`) -- Validar regressão zero e conformidade completa.

**Acceptance Criteria:**
- Given o app mobile com sessão ativa no `FlutterSecureStorage`, when o usuário abre o app ou retorna do background após mais de 2 minutos, then `BiometricLockGate` impede a visualização de qualquer dado patrimonial até aprovação biométrica ou de credencial do SO.
- Given o dispositivo sem conexão após desbloqueio biométrico, when o `DashboardPage` tenta carregar a carteira e encontra snapshot válido no `EncryptedPortfolioCache`, then renderiza o último patrimônio conhecido com indicação discreta da data/hora da última sincronização.
- Given o acionamento de logout ou limpeza de conta, when `signOut()` conclui, then `jwt_token`, `access_token`, `refresh_token` e todos os snapshots do `EncryptedPortfolioCache` são removidos do dispositivo.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Origem | Achado | Veredito | Evidência / Rota |
|--------|--------|----------|------------------|
| blind-hunter / edge-case-hunter | Lockout permanente em desktop ou dispositivo sem suporte biométrico/PIN (`BiometricLockGate` checa apenas `!_isWeb`) | `medium` (`patch`) | Confirmado em `biometric_lock_gate.dart:44`; restringir gate automático a plataformas móveis (`TargetPlatform.android` / `TargetPlatform.iOS`) mantendo `isWebOverride` para testes. |
| blind-hunter / edge-case-hunter | Race condition de read-modify-write concorrente entre `saveSummary` e `saveCustody` em `EncryptedPortfolioCache` | `high` (`patch`) | Confirmado em `encrypted_portfolio_cache.dart:109-144` e `dashboard_page.dart:41-43`; serializar escritas com fila assíncrona (`_synchronized`). |
| blind-hunter | `FlutterSecureStorage` instanciado sem `AndroidOptions(encryptedSharedPreferences: true)` | `medium` (`patch`) | Confirmado em `main.dart:245`, `auth_repository.dart:39` e `encrypted_portfolio_cache.dart:73`; configurar `AndroidOptions(encryptedSharedPreferences: true)`. |
| blind-hunter / edge-case-hunter | `_isOfflineCache` e `_lastSyncedAt` ficam obsoletos entre `loadSummary` e `loadCustodyPositions` | `medium` (`patch`) | Confirmado em `portfolio_provider.dart:77-105,248-295`; resetar `_isOfflineCache = false` no sucesso online de custódia e erro sem cache, e usar `snapshot.lastSyncedAt` no fallback. |
| blind-hunter | `context.mounted` pós-`await signOut()` em `main.dart` pode pular `PortfolioProvider.reset()` | `high` (`patch`) | Confirmado em `main.dart:380-387`; capturar `PortfolioProvider` antes do `await effectiveAuthRepository.signOut()`. |
| blind-hunter | Prompt biométrico dispara imediatamente após login fresco via `/login` -> `/dashboard` | `medium` (`patch`) | Confirmado em `main.dart:377` e `biometric_lock_gate.dart:44-49`; registrar login interativo recente em `BiometricAuthService` para não re-solicitar biometria imediatamente após OAuth. |
| blind-hunter / edge-case-hunter | `BiometricLockGate` não sincroniza `isAuthenticated` em `didUpdateWidget` | `low` (`patch`) | Confirmado em `biometric_lock_gate.dart:32-50`; implementar `didUpdateWidget` para atualizar `_isLocked`. |
| blind-hunter / edge-case-hunter | Chave `_buildKey` em `EncryptedPortfolioCache` pode colidir se `userId`/`tagId` contiver `_` | `low` (`patch`) | Confirmado em `encrypted_portfolio_cache.dart:76-82`; codificar segmentos com `Uri.encodeComponent`. |
| blind-hunter / edge-case-hunter | `AuthRepository.clearSessionData()` executa deletes sequenciais sem isolamento de falha | `medium` (`patch`) | Confirmado em `auth_repository.dart:130-141`; isolar cada etapa com `try/catch` para garantir limpeza máxima. |
| blind-hunter | `access_token` e `refresh_token` gravados mas não consumidos por `ApiClient` | `false` | Requisito explícito da Story 2.2 (AC1/AC4) persistir `access_token`, `refresh_token` e `jwt_token` no `FlutterSecureStorage` e removê-los no `signOut()`. |
| blind-hunter | Cache offline omite `loadEvolution()` e faltam testes de widget de banner/logout | `false` (parcial) | `loadEvolution()` pertence à `EvolutionPage` fora do snapshot do Dashboard; cobertura de widget tests tratada via `verification-gap`. |
| blind-hunter | Fake `_InMemoryStorage` duplicado em 2 arquivos de teste e `.mocks.dart` ausente no diff | `false` | Fakes locais por arquivo seguem regra do projeto; `.mocks.dart` foi omitido apenas do filtro do diff e existe no workspace. |
| edge-case-hunter | Múltiplos eventos `paused`/`hidden` sobrescrevem `backgroundedAt` | `low` (`patch`) | Confirmado em `biometric_lock_gate.dart:62-66`; só chamar `markBackgrounded()` se `backgroundedAt == null`. |
| edge-case-hunter | Duplo clique em "Sair" no `BiometricLockGate` sem trava `_isSigningOut` | `low` (`patch`) | Confirmado em `biometric_lock_gate.dart:99-104`; adicionar trava `_isSigningOut` com `try/finally`. |
| verification-gap | `DashboardPage` sem teste de widget para `_OfflineCacheBanner` (`offline_cache_banner`) | `medium` (`patch`) | Confirmado em `dashboard_page_test.dart`; adicionar teste de widget verificando exibição e ocultação do banner. |
| verification-gap | `ProfilePage` sem teste de widget para `profile_logout_button` | `medium` (`patch`) | Confirmado em `profile_page_test.dart`; adicionar teste de widget verificando `signOut()`, resets e navegação `/login`. |
| verification-gap | `AuthRepository.signOut()` sem teste verificando `portfolioCache.clearAll()` e `onClearCache()` | `medium` (`patch`) | Confirmado em `auth_repository_test.dart`; adicionar teste unitário validando limpeza de cache e timestamp no `signOut()`. |

## Design Notes

- **Criptografia de cache sem nova dependência pesada:** Reutilizar `FlutterSecureStorage` (que já usa Keychain no iOS e `EncryptedSharedPreferences`/Keystore no Android) para o `EncryptedPortfolioCache` evita adicionar engines extras (`hive`/`sqflite_sqlcipher`) e reaproveita a injeção `GrahamSelectApp(storage: ...)` nos testes.
- **Compatibilidade com testes de roteador e mocks:** Injetar `BiometricAuthService?` e `EncryptedPortfolioCache?` com defaults opcionais em `GrahamSelectApp` e `AuthRepository` preserva todos os testes existentes (`main_router_public_docs_test.dart`, `portfolio_repository_impl_test.dart`) sem quebra de assinatura.

## Verification

**Commands:**
- `cd frontend && flutter pub get` -- expected: resolve `local_auth` sem conflitos
- `cd frontend && flutter analyze` -- expected: `No issues found!`
- `cd frontend && flutter test` -- expected: 100% dos testes passam sem regressões
