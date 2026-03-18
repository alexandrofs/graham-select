# Story 1.1-F: Implementação da UI de Login com Google (Frontend)

## Status: Pendente

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
- [ ] Configurar Firebase no projeto Flutter (android/ios/web).
- [ ] Adicionar dependências no `pubspec.yaml`.
- [ ] Criar `AuthRepository` e `AuthService`.
- [ ] Implementar UI da LoginScreen.
- [ ] Configurar interceptor HTTP para incluir JWT nas requisições.
- [ ] Atualizar `GoRouter` para gerenciar o estado de autenticação.
