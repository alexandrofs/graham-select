# História 1.5: Perfil do Investidor (KYC/Suitability)

Status: pronto-para-desenvolvimento

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

- [ ] Backend: Atualização do Banco de Dados
  - [ ] Criar changelog Liquibase para adicionar coluna `investor_profile` na tabela `users`.
- [ ] Backend: Domínio e Infraestrutura
  - [ ] Atualizar entidade `User` com o novo campo.
  - [ ] Criar DTO `UpdateProfileRequest`.
  - [ ] Implementar `UserService.updateProfile`.
  - [ ] Criar endpoint no `UserController`.
- [ ] Frontend: Implementação da Interface (UI)
  - [ ] Criar tela de Suitability/KYC.
  - [ ] Implementar lógica de navegação após o questionário.
  - [ ] Adicionar verificação no Dashboard para exibir o prompt de perfil.
- [ ] Verificação
  - [ ] Teste de integração: verificar se o perfil é salvo corretamente via API.
  - [ ] Teste manual no simulador Flutter.

## Notas de Desenvolvimento

- **Suitability:** Por enquanto, o questionário pode ser estático (ex: 3-5 perguntas de múltipla escolha).
- **Enums:** Usar Enums no Java para garantir a integridade dos dados.
- **UX:** O banner no dashboard deve ser não-intrusivo, mas visível.
