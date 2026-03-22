# Sprint Plan

Este documento consolida o planejamento de Sprints para o MVP do projeto **graham-select**.

---

## 🏃 Sprint 1: Autenticação & Ingestão de Dados Base

**Sprint Number:** 01
**Duration:** 2 weeks
**Sprint Goal:** Estabelecer o fluxo de autenticação, provisionamento de conta e o pipeline de ingestão de dados da B3 com processamento assíncrono.

### 🎯 Objetivo do Sprint
O objetivo deste sprint é permitir que o usuário faça login via Google, tenha sua conta provisionada automaticamente e consiga realizar o primeiro upload do relatório de negociação da B3, vendo o processamento ocorrer em segundo plano.

### 📋 Backlog do Sprint

#### Épico 1: Fundação & Autenticação
*   **Story 1.1: Registro e Login via Google Sign-In**
    *   *Task 1.1.1:* Implementar login com Google OAuth2 (Spring Security).
    *   *Task 1.1.2:* Implementar lógica de criação automática de Tenant/Perfil no primeiro login.
*   **Story 1.2: Gestão de Tier de Assinatura**
    *   *Task 1.2.1:* Implementar modelagem e visualização do tier atual (Gratuito/Premium/Trial).
*   **Story 1.3: Restrição de Funcionalidades por Tier**
    *   *Task 1.3.1:* Ajustar UI para refletir limites de features do plano.
*   **Story 1.4: Exclusão de Conta e Expurgo LGPD**
    *   *Task 1.4.1:* Criar endpoint de solicitação de deleção de conta (Hard Delete).
*   **Story 1.5: Perfil do Investidor (KYC/Suitability)**
    *   *Task 1.5.1:* Implementar questionário de onboarding e salvar perfil de risco.

#### Épico 2: Ingestão de Dados (B3 & Manual)
*   **Story 2.1: Upload de Relatório B3 (Excel)**
    *   *Task 2.1.1:* Criar endpoint REST para upload de arquivos `.xlsx` e UI associada (`B3UploadZone`).
    *   *Task 2.1.2:* Publicar evento `file-uploaded` no Kafka.
*   **Story 2.2: Processamento Assíncrono e Splitter (Worker)**
    *   *Task 2.2.1:* Criar worker consumer do evento `file-uploaded`.
    *   *Task 2.2.2:* Implementar parsing de Excel via FastExcel e disparar eventos `trade-extracted`.
*   **Story 2.3: Deduplicação de Operações**
    *   *Task 2.3.1:* Implementar controle de deduplicação idempotente (hash de Ticker+Data+Qtd+Preco+Corretora).
*   **Story 2.4: Cadastro Manual de Operação (Fallback)**
    *   *Task 2.4.1:* Implementar formulário UI (`ManualOperationEntry`) e API para inserção manual de ativos.
*   **Story 2.5: Trilha de Auditoria das Importações**
    *   *Task 2.5.1:* Desenvolver view e controle de estado do Histórico de Importações.

### 🛠️ Metas Técnicas e NFRs
*   **NFR1 (Escalabilidade):** O pipeline deve suportar a fragmentação de um arquivo de 5.000 linhas em menos de 30 segundos.
*   **NFR4 (Segurança):** Validar que o isolamento de dados por Tenant está funcionando via RLS no banco de dados.

### 🚀 Definição de Pronto (DoP)
- [ ] Código revisado e mergeado.
- [ ] Integração com Firebase Auth (Google Sign-In) e Kafka validada localmente via Docker.
- [ ] Testes unitários com > 80% de cobertura nos extractors.

---

## 🏃 Sprint 2: Portfólio, Market Data e Motor de Graham

**Sprint Number:** 02
**Duration:** 2 weeks
**Sprint Goal:** Consolidar e exibir o dashboard de custódia e integrar a obtenção de cotações para gerar e exibir os cálculos de recomendação do Filtro de Graham.

### 🎯 Objetivo do Sprint
O usuário conseguirá ver seus investimentos consolidados na interface principal. Usuários Premium poderão receber e analisar o motivo matemático de recomendações de compra baseadas na fórmula de Graham.

### 📋 Backlog do Sprint

#### Épico 3: Portfólio & Dashboard Consolidado
*   **Story 3.1: Dashboard KPI**
    *   *Task 3.1.1:* Desenvolver componente UI `PortfolioKpiCard`.
    *   *Task 3.1.2:* Criar endpoint de totalizadores (Patrimônio Total, Yield).
*   **Story 3.2: Tabela de Custódia Atualizada**
    *   *Task 3.2.1:* Desenvolver tabela `FinancialDataTable` consolidando posição atual.
*   **Story 3.3: Histórico de Proventos e Aportes**
    *   *Task 3.3.1:* Criar gráficos mensais consolidando fluxo de entrada.
*   **Story 3.4: Visualização de Alocação por Classe**
    *   *Task 3.4.1:* Gráfico Donut na UI com agrupamento por classe do ativo.
*   **Story 3.5: Atualização em Tempo Real (Event-Driven UI)**
    *   *Task 3.5.1:* Implementar push/socket reativo para `trade-extracted`.
*   **Story 3.6: Comparativo de Performance (Benchmarking)**
    *   *Task 3.6.1:* Integrar séries históricas de IBOV/CDI e sobrepor no gráfico de rentabilidade.

#### Épico 4: Market Data & Motor de Recomendação
*   **Story 4.1: Integração de Market Data**
    *   *Task 4.1.1:* Integração worker/cron para buscar dados Brapi/Yahoo Finance.
    *   *Task 4.1.2:* Implementar fallback de cotações e cache via Caffeine (24h).
*   **Story 4.2: Configuração de Metas de Alocação**
    *   *Task 4.2.1:* Criar tela de configuração e salvar alocações alvo percentual/ativo.
*   **Story 4.3: Motor do Filtro de Graham**
    *   *Task 4.3.1:* Implementar algoritmo `valuation-service` em sub-módulo gerando recomendações.
*   **Story 4.4: Reasoning Box (Explainable AI)**
    *   *Task 4.4.1:* Componente UI `ReasoningBox` com passo-a-passo matemático.

### 🛠️ Metas Técnicas e NFRs
*   **NFR3 (Recomendação):** Motor de cálculo deve executar para todos os ativos em < 2 segundos.
*   **NFR8 (Resiliência):** Garantir que a falha na API parceira acione os dados em cache D-1.

### 🚀 Definição de Pronto (DoP)
- [ ] Integrações externas com fallback testadas via WireMock/Mocks.
- [ ] Dashboards renderizando posições e refletindo Skeletons enquanto carregam.
- [ ] Lógica de alocação de metas validando total em 100%.

---

## 🏃 Sprint 3: Metas Financeiras e Observabilidade

**Sprint Number:** 03
**Duration:** 2 weeks
**Sprint Goal:** Permitir definir metas de longo prazo com o gerador "Time-to-Goal" ("Relógio Motivacional") e construir a área administrativa básica e de resiliência.

### 🎯 Objetivo do Sprint
O usuário deverá ter sua experiência gamificada compreendendo quanto falta para atingir a meta financeira. Os administradores também ganham visibilidade das falhas para possibilitar manutenções operacionais.

### 📋 Backlog do Sprint

#### Épico 5: Metas & Projeção Financeira (Time-to-Goal)
*   **Story 5.1: Configuração de Metas de Patrimônio e Renda**
    *   *Task 5.1.1:* Formulário e persitência das metas de valor alvo e previsões.
*   **Story 5.2: Relógio Motivacional Time-to-Goal**
    *   *Task 5.2.1:* Desenvolver a UI com de projeção (Juros Compostos + Aporte Mensal) e animações.
*   **Story 5.3: Simulador de Sensibilidade ("E se?")**
    *   *Task 5.3.1:* Adicionar sliders reativos para recalcular projeção live-sync na UI.
*   **Story 5.4: Checklist de Saúde da Meta**
    *   *Task 5.4.1:* Implementar heurística de alerta caso o ritmo atual atrase os planos.

#### Épico 6: Administração & Operações (Saúde do Sistema)
*   **Story 6.1: Painel de Monitoramento Admin**
    *   *Task 6.1.1:* Visão de admin consolidada recuperando métricas baseadas no Actuator/DB.
*   **Story 6.2: Reprocessamento de Importações Falhas (DLQ UI)**
    *   *Task 6.2.1:* Frontend listando DLQ `trade-extracted-dlq` com botão de republishing.
*   **Story 6.3: Gestão de Ativos e Tickers (Mestre de Dados)**
    *   *Task 6.3.1:* CRUD simples via painel de admins para o dicionário global de Tickers.

### 🛠️ Metas Técnicas e NFRs
*   **NFR2 (Responsividade):** Slider de sensibilidade recalculando e redesenhando gráficos em menos de 300ms.
*   Acessibilidade e Componentização da UI final validada (Contrastes, Temas).

### 🚀 Definição de Pronto (DoP)
- [ ] Testes End-to-End da lógica de juros compostos garantidos.
- [ ] Permissão de admin rigidamente testada com falhas para usuários comuns.
- [ ] Reprocessamento consumindo do Kafka DLQ sem quebra do fluxo natural.
