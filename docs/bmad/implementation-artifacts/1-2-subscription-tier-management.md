# História 1.2: Gerenciamento de Nível de Assinatura

Status: done

## História

Como usuário da plataforma,
Eu quero ver meu nível de assinatura atual (Trial/Free/Premium),
Para que eu saiba quais funcionalidades estão disponíveis para mim e quando meu período de teste (trial) expira.

## Critérios de Aceite

1. **Atualização da Entidade User:** A tabela `users` e a entidade `User` devem ser atualizadas para incluir:
    - `tier` (Enum: TRIAL, FREE, PREMIUM) [x]
    - `trial_ends_at` (LocalDateTime) [x]
2. **Nível Padrão (Default):** Novos usuários (provisionados via Google OAuth2) devem receber o nível `TRIAL` por padrão. [x]
3. **Duração do Trial:** O campo `trial_ends_at` deve ser definido para 30 dias a partir da data de criação da conta. [x]
4. **API de Perfil:** Um novo endpoint `GET /api/v1/users/me` deve retornar o perfil do usuário atual, incluindo o nível de assinatura (tier) e os dias restantes de trial. [x]
5. **Lógica de Expiração do Trial:** Um mecanismo (ex: uma verificação simples durante o login ou um job agendado) deve ser capaz de identificar trials expirados. Para esta história, o foco é no modelo de dados e no retorno da API. [x]
6. **Testes:** Testes de integração devem verificar se novos usuários recebem 30 dias de trial e se a API retorna o tier corretamente. [x]

## Tarefas / Subtarefas

- [x] Backend: Modelo & Persistência
  - [x] Atualizar entidade `User` com `tier` e `trialEndsAt`
  - [x] Criar changelog do Liquibase para adicionar as colunas na tabela `users`
  - [x] Atualizar `UserRepository` se necessário
- [x] Backend: Regra de Negócio (Provisionamento)
  - [x] Modificar `OAuth2UserProvisioningConverter` para definir o tier padrão e a data de fim do trial
- [x] Backend: Camada de API
  - [x] Criar DTO `UserProfileResponse` (Usando OpenAPI Generator)
  - [x] Criar `UserController` com o endpoint `GET /me`
- [x] Backend: Verificação
  - [x] Implementar `UserTierManagementIT` para verificar o tier padrão e o período de trial
  - [x] Verificar a resposta da API para o usuário autenticado
- [x] Frontend: Implementação do Perfil
  - [x] Criar `UserProfile` model e parse de JSON
  - [x] Implementar `ProfileRepository` para chamar endpoint `/me`
  - [x] Criar `ProfilePage` com exibição de tier e dias restantes
  - [x] Implementar testes de widget para `ProfilePage`
- [x] **Post-Review Fixes** (AI)
  - [x] Lógica de expiração dinâmica no `UserController`
  - [x] Teste de borda para trial expirado
  - [x] Sincronização de status e documentação de arquivos frontend

## Notas de Desenvolvimento

- **Tratamento de Enum:** Usar `@Enumerated(EnumType.STRING)` para o campo tier.
- **Contexto de Segurança:** Usar o `SecurityContext` populado na História 1.1 para identificar o usuário atual.
- **Fuso Horário:** Usar `UTC` para todas as operações de data e hora.

## Dev Agent Record

### Agent Model Used

Antigravity (Claude 3.5 Sonnet)

### File List

- [User.java](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java)
- [SubscriptionTier.java](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/SubscriptionTier.java)
- [UserController.java](file:///c:/Users/afssi/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java)
- [OAuth2UserProvisioningConverter.java](file:///c:/Users/afssi/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java)
- [UserTierManagementIT.java](file:///c:/Users/afssi/projects/graham-select/backend/api/src/test/java/afsdigital/grahamselect/api/auth/UserTierManagementIT.java)
- [08-add-user-subscription-fields.yaml](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/resources/db/changelog/08-add-user-subscription-fields.yaml)
- [db.changelog-master.yaml](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/resources/db/changelog/db.changelog-master.yaml)
- [openapi.yaml](file:///c:/Users/afssi/projects/graham-select/backend/api/src/main/resources/openapi.yaml)
- [profile_page.dart](file:///c:/Users/afssi/projects/graham-select/frontend/lib/src/features/profile/presentation/pages/profile_page.dart)
- [user_profile_model.dart](file:///c:/Users/afssi/projects/graham-select/frontend/lib/src/features/profile/data/models/user_profile_model.dart)
- [profile_repository.dart](file:///c:/Users/afssi/projects/graham-select/frontend/lib/src/features/profile/data/repositories/profile_repository.dart)
- [profile_page_test.dart](file:///c:/Users/afssi/projects/graham-select/frontend/test/features/profile/presentation/pages/profile_page_test.dart)
