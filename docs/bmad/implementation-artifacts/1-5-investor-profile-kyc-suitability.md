# História 1.5: Perfil do Investidor (KYC/Suitability)

Status: done

## História

**Como um** investidor,
**Eu quero** responder um questionário rápido sobre meus objetivos e tolerância a risco,
**Para que** as recomendações do sistema sejam adequadas ao meu perfil.

## Critérios de Aceitação

1. **Backend - Perfil do Investidor:** O sistema deve suportar os perfis: `CONSERVATIVE` (Conservador), `MODERATE` (Moderado) e `AGGRESSIVE` (Arrojado).
2. **Backend - Persistência:** A tabela `users` deve ser atualizada para incluir a coluna `investor_profile`.
3. **Backend - API:** Deve existir um endpoint `PATCH /api/v1/users/profile` para atualizar o perfil do usuário logado.
4. **Frontend - Questionário:** Deve ser implementada uma tela em Flutter com perguntas simples para determinar o perfil.
5. **Frontend - Dashboard:** O sistema deve detectar se o usuário ainda não possui perfil e exibir um banner/modal sugerindo o preenchimento.
6. **Integração:** O perfil salvo deve estar disponível para o motor de recomendação (Épico 4).

## Tarefas / Subtarefas

- [x] Backend: Atualização do Banco de Dados
  - [x] Criar changelog Liquibase para adicionar coluna `investor_profile` na tabela `users`.
- [x] Backend: Domínio e Infraestrutura
  - [x] Atualizar entidade `User` com o novo campo.
  - [x] Criar DTO `UpdateProfileRequest`.
  - [x] Implementar `UserService.updateProfile`.
  - [x] Criar endpoint no `UserController`.
- [x] Frontend: Implementação da Interface (UI)
  - [x] Criar tela de Suitability/KYC.
  - [x] Implementar lógica de navegação após o questionário.
  - [x] Adicionar verificação no Dashboard para exibir o prompt de perfil.
- [x] Verificação
  - [x] Teste de integração: verificar se o perfil é salvo corretamente via API.
  - [x] Teste manual no simulador Flutter.

## Dev Agent Record

### Agent Model Used

gemini-2.0-flash

### Debug Log References

- Backend tests passed: `UserProfileIT` (JUnit 5 + Testcontainers).
- OpenAPI models generated successfully.

### Completion Notes List

- Adicionado campo `investor_profile` à tabela `users` via Liquibase.
- Criado enum `InvestorProfile` no backend (domínio) e exposto via API.
- Implementado endpoint `PATCH /api/v1/users/profile`.
- Criada nova tela `InvestorProfilePage` no Flutter para o questionário de Suitability.
- Adicionado banner informativo no `HomePage` que redireciona para o questionário caso o perfil não esteja definido.
- Modelo `UserProfile` no frontend atualizado para suportar o novo campo.

### Review Fixes (AI)
- **Architectural Fix**: Criado `UpdateInvestorProfileUseCase` no pacote `application.usecase` para manter a Clean Architecture e removida a dependência direta de persistência do `UserController`.
- **Flutter Code Style**: Removida constante global `bold` em `investor_profile_page.dart`.

### File List

- `backend/common/src/main/resources/db/changelog/11-add-user-investor-profile.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/InvestorProfile.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/application/usecase/UpdateInvestorProfileUseCase.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/infrastructure/spring/UserProfileConfiguration.java`
- `backend/api/src/main/resources/openapi.yaml`
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/user/UserProfileIT.java`
- `frontend/lib/src/features/profile/data/models/user_profile_model.dart`
- `frontend/lib/src/features/profile/data/repositories/profile_repository.dart`
- `frontend/lib/src/features/profile/presentation/providers/profile_provider.dart`
- `frontend/lib/src/features/profile/presentation/pages/investor_profile_page.dart`
- `frontend/lib/src/features/home/presentation/pages/home_page.dart`
- `frontend/lib/main.dart`

## Notas de Desenvolvimento

- **Suitability:** Por enquanto, o questionário pode ser estático (ex: 3-5 perguntas de múltipla escolha).
- **Enums:** Usar Enums no Java para garantir a integridade dos dados.
- **UX:** O banner no dashboard deve ser não-intrusivo, mas visível.
