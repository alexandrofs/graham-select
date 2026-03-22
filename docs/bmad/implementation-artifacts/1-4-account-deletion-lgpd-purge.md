# Story 1.4: Exclusão de Conta e Expurgo LGPD

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário da plataforma,
I want poder excluir permanentemente minha conta e todos meus dados,
so that meu direito ao esquecimento (LGPD) seja respeitado.

## Acceptance Criteria

1. **Given** que o usuário está autenticado
   **When** ele solicita exclusão de conta na página de configurações
   **Then** o sistema exige confirmação dupla (modal + digitação "EXCLUIR")
   **And** registra a solicitação com timestamp

2. **Given** que a solicitação de exclusão foi confirmada
   **When** o job de expurgo é executado
   **Then** realiza Hard Delete de todas as operações, posições, metas e dados financeiros
   **And** remove todos os PIIs (nome, e-mail, CPF)
   **And** conclui o expurgo em menos de 24 horas (NFR5)
   **And** envia e-mail de confirmação antes de deletar os dados de autenticação

3. **Given** que o expurgo foi concluído
   **When** alguém tenta logar com o mesmo Google ID
   **Then** o sistema trata como novo registro (conta inexistente)

## Tasks / Subtasks

- [ ] Task 1: Interface de Solicitação de Exclusão (Frontend)
  - [ ] Criar seção de exclusão de conta na página de configurações (Settings).
  - [ ] Implementar Modal de 'Double Confirmation' exigindo digitação da palavra "EXCLUIR".
- [ ] Task 2: API de Registro de Exclusão (Backend)
  - [ ] Criar endpoint autenticado para receber a confirmação de exclusão.
  - [ ] Registrar a solicitação no banco de dados com timestamp e `status = PENDING`.
- [ ] Task 3: Job de Expurgo LGPD (Worker/Cron)
  - [ ] Configurar job assíncrono ou rotina agendada (desacoplada da requisição do front) para processar as contas pendentes de exclusão.
  - [ ] Realizar cascata de "Hard Delete" nas tabelas de portfólios, carteiras, ativos, transações e objetivos atrelados ao usuário (NFR4/NFR5).
  - [ ] Remover ou anonimizar irreversivelmente os dados de PII (nome, email, CPF).
  - [ ] Disparar e-mail informando que a conta foi deletada com sucesso.
  - [ ] Chamar API do Supabase Auth (admin) para excluir o usuário do repositório de autenticação (liberando o Google ID).

## Dev Notes

- **LGPD Compliance:** A LGPD exige remoção integral dos PIIs. Se a arquitetura financeira depender de soft deletes para auditoria interna agregada, é fundamental que haja scrub dos vínculos pessoais. Contudo, o PRD/Epic exige "Hard Delete" dos dados financeiros atrelados ao CPF para esta funcionalidade. Siga a regra de Hard Delete conforme NFR5 e FR5.
- **Isolamento e Segurança (NFR4):** O Tenant Isolation via Row-Level Security (RLS) no banco continuará garantindo que a deleção de um tenant não interfira em outro. O job não deverá bypassar o RLS se executado no contexto restrito, mas caso seja via Admin Job, deve garantir filtro cirúrgico usando a chave Tenant / User ID.
- **Desempenho (NFR5):** O SLA é de 24 horas para garantir expurgo. O flow ideal aceita o comando do usuário e devolve confirmação de "Seu pedido está em processamento", ativando um background worker (ou enfileirando via Kafka).
- **Sem PII no Kafka:** Conforma a Arquitetura (A.3), se usar Kafka, trafegue apenas o ID interno na mensagem `UserDeletedEvent`, e não informações restritas.

### Project Structure Notes

- Alignment with unified project structure (paths, modules, naming):
  - Frontend: `apps/web/src/app/(dashboard)/settings` (ou similar)
  - Backend API: `apps/api/src/modules/users/` ou `apps/worker/`

### References

- Epic 1: Gestão de Usuário e Autenticação B2C [docs/bmad/planning-artifacts/epics.md#Story-1.4]
- FR5, NFR4, NFR5: Documento de Requisitos (PRD) [docs/bmad/planning-artifacts/prd.md]
- Arquitetura e Compliance: LGPD, Segurança de Dados (Item 3) [docs/bmad/planning-artifacts/architecture.md]

## Dev Agent Record

### Agent Model Used

Antigravity Build-Module-Workflow BMM-Create-Story

### Debug Log References
n/a

### Completion Notes List
Ultimate context engine analysis completed - comprehensive developer guide created

### File List
n/a
