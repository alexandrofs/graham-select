# Sprint Plan 1: Onboarding & Ingestão Base

**Sprint Number:** 01
**Duration:** 2 weeks
**Sprint Goal:** Estabelecer o fluxo de autenticação, provisionamento de conta e o pipeline de ingestão de dados da B3 com processamento assíncrono.

## 🎯 Objetivo do Sprint
O objetivo deste sprint é permitir que o usuário faça login via Google, tenha sua conta provisionada automaticamente e consiga realizar o primeiro upload do relatório de negociação da B3, vendo o processamento ocorrer em segundo plano.

## 📋 Backlog do Sprint

### Épico 1: Auth & Tenant Provisioning
*   **Story 1.1: Google OAuth2 & Auto-Provisioning**
    *   *Task 1.1.1:* Implementar login com Google OAuth2 (Spring Security).
    *   *Task 1.1.2:* Implementar lógica de criação automática de Tenant/Perfil no primeiro login.
*   **Story 1.2: LGPD - Expurgo de Dados**
    *   *Task 1.2.1:* Criar endpoint de solicitação de deleção de conta.
    *   *Task 1.2.2:* Implementar job de expurgo de dados (Hard Delete).

### Épico 2: Ingestão de Dados (B3 & Manual)
*   **Story 2.1: Upload de Relatório B3 (Excel)**
    *   *Task 2.1.1:* Criar endpoint REST para upload de arquivos `.xlsx`.
    *   *Task 2.1.2:* Integrar com Storage (Local/MinIO/S3) para persistência do arquivo.
    *   *Task 2.1.3:* Publicar evento `file-uploaded` no Kafka.
*   **Story 2.2: Processamento Assíncrono (Worker/Splitter)**
    *   *Task 2.2.1:* Criar worker consumer do evento `file-uploaded`.
    *   *Task 2.2.2:* Implementar parsing de Excel via FastExcel (Streaming).
    *   *Task 2.2.3:* Implementar Splitter Pattern disparando eventos `trade-extracted`.
*   **Story 2.4: Cadastro Manual de Operação**
    *   *Task 2.4.1:* Implementar formulário e API para inserção manual de ativos.

## 🛠️ Metas Técnicas e NFRs
*   **NFR1 (Escalabilidade):** O pipeline deve suportar a fragmentação de um arquivo de 5.000 linhas em menos de 30 segundos.
*   **NFR4 (Segurança):** Validar que o isolamento de dados por Tenant está funcionando via RLS no banco de dados.

## 🚀 Definição de Pronto (DoP)
- [ ] Código revisado e mergeado.
- [ ] Testes unitários com > 80% de cobertura.
- [ ] Testes de integração (Container do Kafka e DB) passando.
- [ ] Documentação de API atualizada (Swagger/OpenAPI).
