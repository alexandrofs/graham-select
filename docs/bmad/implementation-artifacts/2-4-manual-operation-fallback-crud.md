# Story 2.4: Cadastro Manual de Operação (Fallback)

Status: done

## Story

**As a** investidor,
**I want** cadastrar uma compra ou venda manualmente,
**So that** eu possa registrar operações que não estão na planilha da B3 (ex: Tesouro Direto antigo ou ajustes).

## Acceptance Criteria

1. **Cenário: Cadastro de Nova Operação Manual**
   - [x] **Given** que o usuário acessa o formulário de "Nova Operação" (`ManualOperationEntry`)
   - [x] **When** preenche Ativo (Ticker), Tipo (Compra/Venda), Quantidade, Valor Total Pago (incluindo taxas) e Data
   - [x] **Then** o sistema valida que os campos obrigatórios estão preenchidos e são positivos
   - [x] **And** salva a operação na tabela de `trades` associada ao `userId` do investidor
   - [x] **And** registra uma entrada na Trilha de Auditoria (`ManualTradeAudit`) contendo o autor, timestamp e dados da operação (FR17)
   - [x] **And** dispara o recálculo imediato da posição (emite evento `valuation-requested` para o Kafka)

2. **Cenário: Validação de Dados**
   - [x] **Given** o formulário de entrada manual
   - [x] **When** o usuário tenta salvar com dados inválidos (ex: quantidade negativa ou ticker inexistente)
   - [x] **Then** exibe erro RFC 7807 (Backend) ou feedback inline no Flutter (Frontend) impedindo o envio

3. **Cenário: Responsividade UX**
   - [x] **Given** que o usuário clica em "Salvar"
   - [x] **Then** a resposta visual de sucesso e o redirecionamento/limpeza do formulário deve ocorrer em `< 300ms` (NFR2)

## Tasks / Subtasks

### Backend (Java 21 / Spring Boot 3.4.13)
- [x] **Task 1: Modelagem da Trilha de Auditoria**
  - [x] Subtask 1.1: Criar migration Liquibase para a tabela `manual_trade_audits` (id, trade_id, user_id, action_type, payload_json, created_at).
  - [x] Subtask 1.2: Criar entidade `ManualTradeAudit` e `ManualTradeAuditPort` no módulo `common`.
- [x] **Task 2: Lógica de Negócio (Use Case)**
  - [x] Subtask 2.1: Criar `CreateManualTradeUseCase` no módulo `common`.
  - [x] Subtask 2.2: Implementar a persistência da `Trade` e a criação do registro de auditoria em uma única transação (`@Transactional`).
  - [x] Subtask 2.3: Implementar a emissão do evento `valuation-requested` para o Kafka após o commit.
- [x] **Task 3: Infraestrutura e API**
  - [x] Subtask 3.1: Criar `TradeManualController` no módulo `api` com o endpoint `POST /api/v1/trades/manual`.
  - [x] Subtask 3.2: Implementar mapeamento JPA para `ManualTradeAuditEntity`.
- [x] **Task 6: Refatoração de Segurança e Integridade (AI Follow-up)**
  - [x] Adicionar campo `side` (COMPRA/VENDA) à `Trade` e Banco de Dados.
  - [x] Garantir transacionalidade no Use Case.

### Frontend (Flutter / Material 3)
- [x] **Task 4: Componente de Entrada Manual**
  - [x] Subtask 4.1: Implementar o widget `ManualOperationEntry` seguindo o UX Design Spec (Autocomplete de Ticker, Toggles de Tipo, Máscara Monetária).
  - [x] Subtask 4.2: Integrar com o Provider da feature de `portfolio` para disparar a requisição à API passando o `side`.
- [x] **Task 5: Feedback e Navegação**
  - [x] Subtask 5.1: Implementar Skeleton screens e animações de sucesso (fade/slide 300ms).


## Developer Context

### Architecture Compliance
- **Isolamento de Dados:** Filtre SEMPRE por `user_id` extraído do JWT para qualquer operação de escrita ou auditoria.
- **Trilha de Auditoria:** O log de auditoria deve ser imutável. Não deve haver endpoints de deleção ou edição para a tabela `manual_trade_audits`.
- **Event-Driven:** O recálculo de posição deve ser disparado via Kafka, não sincronamente na API, para manter a resiliência (valuation-service pode estar ocupado).

### Library / Framework Requirements
- **Java 21:** Use `Records` para os DTOs de entrada (`ManualTradeRequest`).
- **Spring Boot 3.4.13:** Use `ProblemDetail` para erros de validação.
- **Flutter:** Siga o padrão feature-first (`lib/src/features/portfolio/presentation/widgets/manual_operation_entry.dart`).

### Previous Story Intelligence
- **Learn from Story 2.3:** A lógica de deduplicação (Ticker + Data + Qtd + Preço) também deve ser aplicada ao cadastro manual para evitar cliques duplos.
- **Timezone:** Use `ZoneOffset.UTC` para todas as datas de trades.

### Git Intelligence
- O projeto usa Conventional Commits. Ex: `feat(api): add manual trade endpoint with audit trail`.

## Project Context Reference
- **Stack:** Java 21, Spring Boot 3.4.13, Flutter, Kafka, MySQL.
- **Architecture:** Clean Architecture (domain ← application ← infrastructure).
- **UX Spec:** Componente `ManualOperationEntry` com paleta Navy Blue e Emerald.

---
**Status Final:** Ultimate context engine analysis completed - comprehensive developer guide created.
