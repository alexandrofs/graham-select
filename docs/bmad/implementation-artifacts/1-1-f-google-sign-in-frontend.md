# Story 1.1-f: User Login and Auto-provisioning via Google Sign-In

Status: Done

## Descrição
Implementar a interface de usuário para login e o fluxo de autenticação via Google Sign-In no frontend Flutter, integrando com o backend já preparado na Story 1.1.

## Critérios de Aceitação
1. **Dependências**: Adicionar `firebase_auth` e `google_sign_in` ao `pubspec.yaml`.
2. **Feature Auth**: Criar a estrutura básica em `lib/src/features/auth`.
3. **Tela de Login**: Implementar tela de login conforme `ux-design-specification.md` (Botão "Entrar com Google").
4. **Integração Backend**: Enviar o ID Token obtido do Google para o endpoint `/api/auth/google` do backend e armazenar o JWT retornado.
5. **Estado de Autenticação**: Persistir o JWT e redirecionar para a Home após o login bem-sucedido.
6. **Proteção de Rotas**: Garantir que rotas protegidas redirecionem para Login se não houver JWT.

## Tarefas Técnicas
- [x] Configurar Firebase no projeto Flutter (android/ios/web).
- [x] Adicionar dependências no `pubspec.yaml`.
- [x] Criar / Atualizar `AuthRepository` com troca de token.
- [x] Implementar UI da LoginScreen.
- [x] Configurar interceptor HTTP para incluir JWT nas requisições.
- [x] Atualizar `GoRouter` para gerenciar o estado de autenticação.

## Dev Agent Record

### Agent Model Used

Antigravity (Claude 3.5 Sonnet)

### File List

- [1-1-f-google-sign-in-frontend.md](file:///Users/alexandrofs/Documents/projects/graham-select/docs/bmad/stories/1-1-f-google-sign-in-frontend.md)
- [main.dart](file:///Users/alexandrofs/Documents/projects/graham-select/frontend/lib/main.dart)
- [api_client.dart](file:///Users/alexandrofs/Documents/projects/graham-select/frontend/lib/src/core/api/api_client.dart)
- [auth_repository.dart](file:///Users/alexandrofs/Documents/projects/graham-select/frontend/lib/src/features/auth/data/auth_repository.dart)
- [Info.plist](file:///Users/alexandrofs/Documents/projects/graham-select/frontend/ios/Runner/Info.plist)
- [build.gradle.kts](file:///Users/alexandrofs/Documents/projects/graham-select/frontend/android/app/build.gradle.kts)
- [login_screen.dart](file:///Users/alexandrofs/Documents/projects/graham-select/frontend/lib/src/features/auth/presentation/login_screen.dart)
