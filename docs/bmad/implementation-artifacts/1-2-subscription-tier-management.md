# História 1.2: Gerenciamento de Nível de Assinatura

Status: ready-for-dev

## História

Como usuário da plataforma,
Eu quero ver meu nível de assinatura atual (Trial/Free/Premium),
Para que eu saiba quais funcionalidades estão disponíveis para mim e quando meu período de teste (trial) expira.

## Critérios de Aceite

1. **Atualização da Entidade User:** A tabela `users` e a entidade `User` devem ser atualizadas para incluir:
    - `tier` (Enum: TRIAL, FREE, PREMIUM) [ ]
    - `trial_ends_at` (LocalDateTime) [ ]
2. **Nível Padrão (Default):** Novos usuários (provisionados via Google OAuth2) devem receber o nível `TRIAL` por padrão. [ ]
3. **Duração do Trial:** O campo `trial_ends_at` deve ser definido para 30 dias a partir da data de criação da conta. [ ]
4. **API de Perfil:** Um novo endpoint `GET /api/v1/users/me` deve retornar o perfil do usuário atual, incluindo o nível de assinatura (tier) e os dias restantes de trial. [ ]
5. **Lógica de Expiração do Trial:** Um mecanismo (ex: uma verificação simples durante o login ou um job agendado) deve ser capaz de identificar trials expirados. Para esta história, o foco é no modelo de dados e no retorno da API. [ ]
6. **Testes:** Testes de integração devem verificar se novos usuários recebem 30 dias de trial e se a API retorna o tier corretamente. [ ]

## Tarefas / Subtarefas

- [ ] Backend: Modelo & Persistência
  - [ ] Atualizar entidade `User` com `tier` e `trialEndsAt`
  - [ ] Criar changelog do Liquibase para adicionar as colunas na tabela `users`
  - [ ] Atualizar `UserRepository` se necessário
- [ ] Backend: Regra de Negócio (Provisionamento)
  - [ ] Modificar `OAuth2UserProvisioningConverter` para definir o tier padrão e a data de fim do trial
- [ ] Backend: Camada de API
  - [ ] Criar DTO `UserProfileResponse`
  - [ ] Criar `UserController` com o endpoint `GET /me`
- [ ] Backend: Verificação
  - [ ] Implementar `UserTierManagementIT` para verificar o tier padrão e o período de trial
  - [ ] Verificar a resposta da API para o usuário autenticado

## Notas de Desenvolvimento

- **Tratamento de Enum:** Usar `@Enumerated(EnumType.STRING)` para o campo tier.
- **Contexto de Segurança:** Usar o `SecurityContext` populado na História 1.1 para identificar o usuário atual.
- **Fuso Horário:** Usar `UTC` para todas as operações de data e hora.
