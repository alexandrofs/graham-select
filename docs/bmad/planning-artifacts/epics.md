  - step-03-create-stories (All Epics 1-10)
inputDocuments:
  - docs/bmad/planning-artifacts/prd.md
  - docs/bmad/planning-artifacts/architecture.md
  - docs/bmad/planning-artifacts/ux-design-specification.md
---

# graham-select - Epic Breakdown

## Overview

Este documento fornece a decomposição completa de épicos e histórias para o graham-select, transformando os requisitos do PRD, UX Design e Architecture em histórias implementáveis.

## Requirements Inventory

### Functional Requirements

- **FR1:** Usuários podem criar uma conta na plataforma utilizando e-mail ou provedores sociais (Google/Apple).
- **FR2:** Usuários podem visualizar e alterar seu nível atual de assinatura (Gratuito ou Premium).
- **FR3:** O Sistema deve restringir o acesso à recomendação baseada em Inteligência Artificial para contas no tier Gratuito (após o Trial expirar).
- **FR4:** Usuários Premium podem vincular e visualizar múltiplos perfis/CPFs (Holding Familiar) sob uma mesma conta matriz.
- **FR5:** O Sistema deve permitir a exclusão definitiva da conta e expurgo total dos dados financeiros atrelados ao CPF (Direito ao Esquecimento LGPD).
- **FR6:** Usuários podem realizar o upload manual de arquivos extraídos da Área Logada da B3 (PDF, XLS ou XML).
- **FR7:** O Sistema deve extrair, estruturar e salvar as operações históricas contidas no arquivo em background (Assíncrono).
- **FR8:** O Sistema deve notificar o usuário sobre o sucesso ou falha do processamento do arquivo importado.
- **FR9 (New/Post-MVP):** Usuários poderão realizar o upload das Notas de Corretagem padrão (SINACOR) em arquivo PDF para alimentar as operações sem depender exclusivamente dos relatórios da bolsa.
- **FR10 (New/Post-MVP):** O Sistema permitirá a integração, cadastro manual ou parse de dados oriundos de corretoras internacionais (Nyse/Nasdaq) e carteiras/plataformas de criptoativos, viabilizando a recomendação de alocação de "All-in-One Global Portfolio".
- **FR11 (Post-MVP):** Usuários poderão conectar suas contas via Open Finance para sincronização automática total de custódia nacional e proventos.
- **FR12:** Usuários podem visualizar a custódia atualizada do seu portfólio (posições ativas).
- **FR13:** Usuários podem visualizar o histórico de proventos recebidos e aportes realizados.
- **FR14:** Usuários podem criar "Tags de Carteira" personalizadas (ex: Aposentadoria, Hold, Dividendos) e associar ativos a essas tags (como "Progressive Profiling" em etapas pós-cadastro).
- **FR15:** O Sistema deve permitir a visualização de consolidações, rentabilidade e recomendação de IA filtradas especificamente por essas Tags de Carteira.
- **FR16:** Usuários podem inserir, editar ou deletar (CRUD) operações de ativos manualmente.
- **FR17:** O Sistema deve manter uma trilha de auditoria imutável (log) de toda criação, alteração ou deleção manual.
- **FR18:** O Sistema deve acessar via integração (API) as cotações financeiras atualizadas (end-of-day ou real-time) dos ativos custodiados.
- **FR19:** O Sistema deve acessar e armazenar atualizações dos indicadores fundamentalistas das empresas acompanhadas (P/L, DY, P/VP, etc.).
- **FR20:** Usuários Premium podem definir uma "Meta Percentual" de alocação por classe de ativos e por ativo específico.
- **FR21:** O Sistema deve cruzar a carteira atual do usuário, as metas definidas e o Filtro Fundamentalista (Filtro de Graham) para gerar recomendações primárias de compra com o capital disponível (Isolado e configurado via backend em Engine dedicada).
- **FR22:** O Sistema deve apresentar abertamente o racional matemático por trás da recomendação de ativo gerada pela IA (Explainable AI - Fallback).
- **FR23:** Usuários podem configurar um valor financeiro alvo como "Meta de Independência Financeira" ou "Renda Passiva Mensal Alvo".
- **FR24:** O Sistema deve projetar e visualizar o tempo de vida restante (anos/meses) para atingir a meta financeira com base na taxa de rentabilidade e aportes atuais.
- **FR25:** (Post-MVP) O Sistema enviará alertas acionáveis via WhatsApp contendo resumos do recebimento de proventos e gatilhos de rebalanceamento.
- **FR26:** Administradores do sistema podem visualizar o volume e as taxas de falha (tracing) das importações de arquivos B3 de forma anonimizada.
- **FR27:** Administradores podem disparar o reprocessamento em lote (fila manual) de arquivos que falharam no parser após a subida de um hotfix corretivo.

### NonFunctional Requirements

- **NFR1 (Tempo de Parse):** O processamento assíncrono do arquivo bruto da B3 (PDF/XLS/XML) e a consolidação inicial da carteira no banco de dados deve ocorrer em até `< 5 minutos` (P95) para arquivos extensos.
- **NFR2 (Responsividade UX):** Como o front-end é construído utilizando renderização com base nativa (Flutter), as transições visuais (Dashboard, Portfólio, Sugestões) não devem exceder `< 300ms` via requisições REST/GraphQL, garantindo a sensação de fluidez ("app-like").
- **NFR3 (Recomendação IA):** O cálculo da recomendação de aportes, que cruza saldos (Tenant), API de Fundamentos e as Tags do Usuário, deve responder em `< 2 segundos`.
- **NFR4 (Isolamento de Tenant - LGPD):** Nenhuma query ou extração do backend deve ter arquitetura suscetível ao vazamento acidental de histórico de ordens entre IDs (Implementação estrutural de Multi-tenancy lógica via Row-Level Security no DB).
- **NFR5 (Data Sanitization & GDPR):** A solicitação de fechamento da conta deve invocar em menos de 24 horas um expurgo efetivo (Hard Delete) dos espelhos de transações B3 e recomendações.
- **NFR6 (Criptografia):** Elementos sensíveis (PIIs, saldos reais, e posteriormente tokens Open Finance) devem habitar banco cifrado em repouso (AES-256) e transitar criptografados (TLS 1.3).
- **NFR7 (Ingestão Elástica):** A arquitetura do parser e do *Engine Core* da IA precisará tolerar e isolar cargas em filas elásticas (bus/workers) caso o volume de upload simultâneo ocorra todo mês nos "dias mundiais de aporte" (ex: dia útil 05 ou dia 10 de recebimento de P/L). A fila pesada não pode quebrar o app visual do NFR2.
- **NFR8 (Graceful Degradation):** Em caso de queda momentânea da API terceira de Cotações ou do módulo da B3 parceira, o aplicativo deve continuar operável. O front-end exibirá as posições com um indicador tático de "Cotação em Cache - Desatualizado" no último fechamento (D-1/D-X), permitindo navegação sem telas "em branco".

### Additional Requirements

**Arquitetura:**
- Stack: Java 21, Spring Boot 3.4.13, Flutter, Apache Kafka, MySQL
- Projeto multi-module Maven (api, valuation-service, common)
- Clean Architecture com regra de dependência (domain ← application ← infrastructure)
- Google Sign-In via Firebase Auth (frontend Flutter + backend validação JWT)
- 5 tópicos Kafka: `file-uploaded`, `trade-extracted`, `trade-extracted-dlq`, `valuation-requested`, `valuation-completed`
- Liquibase para database migrations
- RFC 7807 ProblemDetail para error handling
- REST com versionamento por path `/api/v1/`
- CI/CD via GitHub Actions
- Docker Compose para deploy MVP
- Observabilidade: Spring Boot Actuator + Micrometer
- Cache: Spring Cache + Caffeine (in-memory, TTL 24h para rankings)
- Rate limiting: Bucket4j
- Use Cases como POJOs com método `execute()` — sem Spring annotations

**UX Design:**
- Onboarding bifurcado: Upload B3 ou Inserção Manual como caminhos equivalentes
- 7 componentes custom: `B3UploadZone`, `ManualOperationEntry`, `PortfolioKpiCard`, `FinancialDataTable`, `ReasoningBox`, `AnalyticalLensTabs`, `AnomalyAlert`
- Material Design 3 customizado com paleta Navy Blue (#1B2A4A), Emerald (#10B981), Amber Gold (#F59E0B)
- Desktop-first responsivo com breakpoints: Desktop (≥1024px), Tablet (768-1023px), Mobile (<768px)
- Acessibilidade WCAG 2.1 AA obrigatória (contraste, teclado, screen readers)
- Dark/Light mode nativo via `ColorScheme.fromSeed()`
- Skeleton screens obrigatórios (nunca spinner genérico)
- Tipografia: DM Sans (headers) + Inter (body/dados) + JetBrains Mono (valores financeiros)
- Sidebar colapsável (NavigationRail desktop) + NavigationBar (bottom mobile)
- Feature-first structure: `lib/src/features/{feature}/data|domain|presentation`
- Animações: fade+slide 300ms, crossfade 250ms, sidebar 200ms

### FR Coverage Map

| FR | Épico | Descrição |
|---|---|---|
| FR1 | Epic 1 | Criação de conta |
| FR2 | Epic 1 | Gestão de assinatura |
| FR3 | Epic 1 | Restrição por tier |
| FR4 | Epic 7 | Multi-CPF (Post-MVP) |
| FR5 | Epic 1 | Exclusão LGPD |
| FR6 | Epic 2 | Upload B3 |
| FR7 | Epic 2 | Processamento assíncrono |
| FR8 | Epic 2 | Notificação processamento |
| FR9 | Epic 9 | Notas corretagem (Post-MVP) |
| FR10 | Epic 9 | Internacional/Cripto (Post-MVP) |
| FR11 | Epic 9 | Open Finance (Post-MVP) |
| FR12 | Epic 3 | Custódia atualizada |
| FR13 | Epic 3 | Histórico proventos |
| FR14 | Epic 8 | Tags de Carteira (Post-MVP) |
| FR15 | Epic 8 | Filtros por Tags (Post-MVP) |
| FR16 | Epic 2 | CRUD operações manuais |
| FR17 | Epic 2 | Trilha de auditoria |
| FR18 | Epic 4 | Cotações API |
| FR19 | Epic 4 | Indicadores fundamentalistas |
| FR20 | Epic 4 | Meta percentual de alocação |
| FR21 | Epic 4 | Motor de recomendação Graham |
| FR22 | Epic 4 | Explainable AI |
| FR23 | Epic 5 | Meta financeira |
| FR24 | Epic 5 | Projeção tempo de vida |
| FR25 | Epic 10 | WhatsApp (Post-MVP) |
| FR26 | Epic 6 | Painel admin |
| FR27 | Epic 6 | Reprocessamento lote |

## Epic List

### Épicos MVP

#### Epic 1: Fundação & Autenticação
O usuário consegue criar conta, fazer login via Google e ter seu perfil provisionado automaticamente com segurança.

**FRs cobertos:** FR1, FR2, FR3, FR5
**NFRs impactados:** NFR4 (isolamento tenant), NFR6 (criptografia)
**Notas:** Google Sign-In via Firebase Auth. Auto-provisioning no primeiro acesso. Subscription tier logic (Gratuito/Premium/Trial) e exclusão LGPD.

---

#### Epic 2: Ingestão de Dados — Upload B3 & Entrada Manual
O usuário consegue alimentar sua carteira com dados reais — arrastando o arquivo da B3 ou digitando operações manualmente — e recebe feedback sobre o resultado.

**FRs cobertos:** FR6, FR7, FR8, FR16, FR17
**NFRs impactados:** NFR1 (processamento < 5min), NFR7 (ingestão elástica)
**Notas:** Padrão Splitter via Kafka. Onboarding bifurcado (Upload B3 / Manual). Componentes `B3UploadZone` e `ManualOperationEntry`. Trilha de auditoria para CRUD manual.

---

#### Epic 3: Portfólio & Dashboard Consolidado
O usuário visualiza todo seu patrimônio consolidado — posições ativas, proventos, aportes — com atualizações em tempo real e dashboard rico.

**FRs cobertos:** FR12, FR13
**NFRs impactados:** NFR2 (responsividade < 300ms), NFR8 (graceful degradation)
**Notas:** Componentes `PortfolioKpiCard`, `FinancialDataTable`. Skeleton screens. Trust indicators ("Atualizado em DD/MM").

---

#### Epic 4: Market Data & Motor de Recomendação (Filtro de Graham)
O usuário Premium recebe recomendações de aporte baseadas no Filtro de Graham com transparência total do raciocínio matemático.

**FRs cobertos:** FR18, FR19, FR20, FR21, FR22
**NFRs impactados:** NFR3 (recomendação < 2s), NFR8 (cache de cotações)
**Notas:** Integração API de cotações. Indicadores fundamentalistas. Motor de ranking via `valuation-service` + Kafka. Componente `ReasoningBox` (Explainable AI).

---

#### Epic 5: Metas & Projeção Financeira (Time-to-Goal)
O usuário configura suas metas de independência financeira e visualiza projeções de quanto tempo falta para atingi-las.

**FRs cobertos:** FR23, FR24
**Notas:** Gamificação via "relógio motivacional" de anos/meses restantes. Projeção baseada em yield atual e aportes.

---

#### Epic 6: Administração & Operações
Administradores podem monitorar a saúde do sistema e reprocessar importações falhas.

**FRs cobertos:** FR26, FR27
**Notas:** Painel admin com métricas anonimizadas. Reprocessamento via dead-letter queue do Kafka.

---

### Épicos Post-MVP

#### Epic 7: Holding Familiar (Multi-CPF)
Usuários Premium podem vincular múltiplos CPFs e alternar entre visões familiares/individuais.

**FRs cobertos:** FR4
**Notas:** "Lentes Analíticas" com filtro por Tag/CPF. Componente `AnalyticalLensTabs`.

---

#### Epic 10: Relatórios & Benchmarking
Funcionalidades de comparação de performance e relatórios fiscais (IR).
**FRs cobertos:** FR12, FR13

#### Epic 11: Notificações Multicanal
Alertas via Web Push e WhatsApp (Post-MVP).
**FRs cobertos:** FR25

---


---

## Detailed Stories

### Epic 1: Fundação & Autenticação
**Related PRD Requirements:** FR1, FR2, FR3, FR5
**NFRs impactados:** NFR4 (Disponibilidade), NFR5 (LGPD Expurge), NFR6 (Segurança/Logs)


**Goal:** O usuário consegue criar conta, fazer login via Google e ter seu perfil provisionado automaticamente com segurança.

#### Story 1.1: Registro e Login via Google Sign-In

**Related FRs/NFRs:** FR1

**As a** investidor pessoa física,
**I want** me cadastrar e fazer login usando minha conta Google,
**So that** eu acesse a plataforma de forma rápida e segura, sem precisar criar mais uma senha.

**Acceptance Criteria:**

- **Given** que o usuário acessa a tela de login pela primeira vez
- **When** ele clica em "Entrar com Google" e autoriza o acesso
- **Then** o sistema cria a conta com dados do perfil Google (nome, e-mail, foto)
- **And** provisiona automaticamente o tenant isolado (Row-Level Security)
- **And** define o tier como "Trial" com validade de 30 dias
- **And** garante que este log seja imutável (sem API de deleção/edição para o log)

- **Given** que o usuário já possui conta
- **When** ele clica em "Entrar com Google"
- **Then** o sistema valida o JWT Firebase e inicia a sessão
- **And** redireciona para o dashboard principal

- **Given** que o token JWT é inválido ou expirado
- **When** o sistema tenta validar a autenticação
- **Then** retorna erro RFC 7807 (401 Unauthorized)
- **And** redireciona para a tela de login

#### Story 1.2: Gestão de Tier de Assinatura

**Related FRs/NFRs:** FR2, FR3

**As a** usuário da plataforma,
**I want** visualizar meu nível atual de assinatura (Trial/Gratuito/Premium),
**So that** eu saiba quais funcionalidades estão disponíveis para mim.

**Acceptance Criteria:**

- **Given** que o usuário está autenticado
- **When** ele acessa a página de perfil/assinatura
- **Then** o sistema exibe o tier atual (Trial, Gratuito ou Premium)
- **And** se Trial, mostra os dias restantes até expiração
- **And** exibe um comparativo de funcionalidades por tier

- **Given** que o período de Trial de 30 dias expirou
- **When** o sistema verifica o tier do usuário
- **Then** atualiza automaticamente o tier para "Gratuito"
- **And** restringe acesso às funcionalidades Premium (ex: motor de recomendação)

#### Story 1.3: Restrição de Funcionalidades por Tier

**Related FRs/NFRs:** FR5

**As a** sistema,
**I want** controlar o acesso a funcionalidades Premium baseado no tier do usuário,
**So that** apenas assinantes Premium acessem recomendações de IA e metas de alocação.

**Acceptance Criteria:**

- **Given** que o usuário tem tier "Gratuito"
- **When** ele tenta acessar o motor de recomendação (FR21) ou metas de alocação (FR20)
- **Then** o sistema exibe uma mensagem explicando que a funcionalidade é Premium
- **And** oferece opção de upgrade

- **Given** que o usuário tem tier "Trial" dentro da validade
- **When** ele acessa funcionalidades Premium
- **Then** o sistema permite acesso normalmente
- **And** exibe badge "Trial — X dias restantes"

- **Given** que o usuário tem tier "Premium"
- **When** ele acessa qualquer funcionalidade
- **Then** o sistema permite acesso completo sem restrições

#### Story 1.4: Exclusão de Conta e Expurgo LGPD

**As a** usuário da plataforma,
**I want** poder excluir permanentemente minha conta e todos meus dados,
**So that** meu direito ao esquecimento (LGPD) seja respeitado.

**Acceptance Criteria:**

- **Given** que o usuário está autenticado
- **When** ele solicita exclusão de conta na página de configurações
- **Then** o sistema exige confirmação dupla (modal + digitação "EXCLUIR")
- **And** registra a solicitação com timestamp

- **Given** que a solicitação de exclusão foi confirmada
- **When** o job de expurgo é executado
- **Then** realiza Hard Delete de todas as operações, posições, metas e dados financeiros
- **And** remove todos os PIIs (nome, e-mail, CPF)
- **And** conclui o expurgo em menos de 24 horas (NFR5)
- **And** envia e-mail de confirmação antes de deletar os dados de autenticação

#### Story 1.5: Perfil do Investidor (KYC/Suitability)

**As a** investidor,
**I want** responder um questionário rápido sobre meus objetivos e tolerância a risco,
**So that** as recomendações do sistema sejam adequadas ao meu perfil.

**Acceptance Criteria:**
- **Given** que o usuário fez o primeiro login
- **When** ele acessa o dashboard inicial
- **Then** o sistema sugere o preenchimento do perfil de investidor
- **And** salva o perfil (Conservador, Moderado, Arrojado) para calibrar o motor de alocação (Epic 4).

### Epic 2: Ingestão de Dados (B3 & Manual)
**Related PRD Requirements:** FR16, FR17, FR26, FR6, FR7, FR8, NFR1, NFR7

**Goal:** Garantir que o usuário possa consolidar seu histórico de investimentos de forma automatizada (CDB/B3) ou manual, com processamento assíncrono e resiliente.

#### Story 2.1: Upload de Relatório de Negociação B3 (Excel)

**Related FRs/NFRs:** FR6

**As a** investidor,
**I want** fazer o upload da minha planilha de negociação da B3,
**So that** eu não precise cadastrar centenas de operações manualmente.

**Acceptance Criteria:**
- **Given** que o usuário está na tela de "Importação"
- **When** ele seleciona um arquivo `.xlsx` (Relatório oficial da B3)
- **Then** o sistema realiza o upload para o Storage (Claim Check Pattern)
- **And** valida os cabeçalhos do arquivo (Ticker, Data, Quantidade, Preço)
- **And** emite o evento `file-uploaded` no Kafka

#### Story 2.2: Processamento Assíncrono e Splitter (Worker)

**Related FRs/NFRs:** FR7, NFR1, NFR7

**As a** sistema,
**I want** processar o arquivo Excel em segundo plano, fragmentando-o em operações individuais,
**So that** grandes volumes de dados não bloqueiem a interface e sejam tolerantes a falhas.

**Acceptance Criteria:**
- **Given** um evento `file-uploaded` no Kafka
- **When** o `ingestion-service` consome o evento e lê o arquivo (Streaming via FastExcel)
- **Then** fragmenta o arquivo (Splitter Pattern) em eventos `trade-extracted` (um por linha)
- **And** envia linhas corrompidas para o tópico `trade-extracted-dlq` (DLQ)
- **And** o status da importação é atualizado via WebSocket/Polling

#### Story 2.3: Deduplicação de Operações

**Related FRs/NFRs:** FR8

**As a** investidor,
**I want** que o sistema identifique e ignore operações que eu já importei anteriormente,
**So that** meu saldo e preço médio não fiquem duplicados e incorretos.

**Acceptance Criteria:**
- **Given** uma operação extraída do Excel
- **When** o sistema verifica se já existe uma operação idêntica (Ticker + Data + Qtd + Preço + Corretora) para o usuário
- **Then** ignora a duplicata e registra apenas as novas operações
- **And** informa o total de "novas operações" vs "duplicadas" no final do processo

#### Story 2.4: Cadastro Manual de Operação (Fallback)

**Related FRs/NFRs:** FR16, FR17

**As a** investidor,
**I want** cadastrar uma compra ou venda manualmente,
**So that** eu possa registrar operações que não estão na planilha da B3 (ex: Tesouro Direto antigo ou ajustes).

**Acceptance Criteria:**
- **Given** que o usuário acessa o formulário de "Nova Operação"
- **When** preenche Ativo, Tipo (Compra/Venda), Quantidade, Preço Unitário e Taxas
- **Then** o sistema valida os dados e salva na tabela de operações
- **And** dispara o recálculo imediato da posição do ativo

#### Story 2.5: Trilha de Auditoria das Importações

**Related FRs/NFRs:** FR17, FR26

**As a** investidor,
**I want** ver um histórico de todos os arquivos que já importei,
**So that** eu saiba quando foi minha última atualização de dados.

**Acceptance Criteria:**
- **Given** que o usuário acessa a aba "Histórico de Importação"
- **When** o dashboard carrega
- **Then** lista Nome do Arquivo, Data de Upload, Status (Sucesso/Parcial/Erro) e Qtd. de Linhas Processadas

### Epic 3: Portfólio & Dashboard Consolidado
**Related PRD Requirements:** FR12, FR13, NFR2, NFR8

**Goal:** O usuário visualiza todo seu patrimônio consolidado — posições ativas, proventos, aportes — com atualizações em tempo real e dashboard rico.

#### Story 3.1: Dashboard KPI (Componente `PortfolioKpiCard`)

**Related FRs/NFRs:** FR12, NFR2

**As a** investidor,
**I want** ver um resumo visual do meu patrimônio (Valor Total, Lucro/Prejuízo, Dividend Yield),
**So that** eu tenha uma visão rápida da saúde financeira da minha carteira.

**Acceptance Criteria:**

- **Given** que o usuário está logado e possui operações cadastradas
- **When** ele acessa a `HomeScreen`
- **Then** o sistema calcula e exibe cards com: Patrimônio Total (R$), Rendimento Bruto (%), Dividendos Acumulados e Projeção Mensal
- **And** usa Skeleton Screens durante o carregamento (NFR2)
- **And** utiliza as cores Emerald (lucro) e Navy Blue (neutro) conforme o Design System
- **And** as transições entre visões ocorrem em < 300ms

#### Story 3.2: Tabela de Custódia Atualizada (Componente `FinancialDataTable`)

**Related FRs/NFRs:** FR12, NFR8

**As a** investidor,
**I want** ver a lista de todos os ativos que possuo hoje,
**So that** eu acompanhe o preço médio e a valorização de cada papel individualmente.

**Acceptance Criteria:**

- **Given** que o usuário acessa a aba "Portfólio"
- **When** o sistema consolida as operações manuais e de upload
- **Then** agrupa ativos por Ticker e exibe: Quantidade, Preço Médio, Cotação Atual (D-0 ou Cache), Valor de Mercado e % de Ganho/Perda
- **And** permite ordenação por qualquer coluna
- **And** exibe indicador "Atualizado em [Horário]" ou "Preço em Cache" (NFR8)

#### Story 3.3: Histórico de Proventos e Aportes

**Related FRs/NFRs:** FR13

**As a** investidor,
**I want** visualizar graficamente quanto recebi de dividendos e quanto aportei por mês,
**So that** eu visualize a evolução do meu efeito "bola de neve".

**Acceptance Criteria:**

- **Given** que o usuário acessa a visão de "Evolução"
- **When** o sistema filtra as operações por tipo `DIVIDENDO` e `COMPRA`
- **Then** plota um gráfico de barras comparativo por mês dos últimos 12 meses
- **And** permite alternar para visão de tabela detalhada com filtros de data

#### Story 3.4: Visualização de Alocação por Classe (Gráfico de Rosca)

**Related FRs/NFRs:** FR12

**As a** investidor,
**I want** visualizar o balanceamento da minha carteira entre as diferentes classes de ativos (Ações, FIIs, Tesouro, etc.),
**So that** eu possa verificar se a minha diversificação está de acordo com a minha estratégia.

**Acceptance Criteria:**

- **Given** que o usuário possui ativos de diferentes classes na carteira
- **When** ele acessa o dashboard de Portfólio
- **Then** o sistema exibe um gráfico de rosca (Donut Chart) com a distribuição percentual por classe
- **And** permite clicar em uma classe para detalhar os ativos dentro dela
- **And** exibe a legenda com o valor financeiro total por categoria

#### Story 3.5: Atualização em Tempo Real (Event-Driven UI)

**Related FRs/NFRs:** NFR2

**As a** usuário,
**I want** que meu dashboard se atualize automaticamente quando um novo upload ou operação manual for concluído,
**So that** eu não precise dar "refresh" na página manualmente.

**Acceptance Criteria:**

- **Given** que o usuário está com o Dashboard aberto
- **When** o Kafka processa um evento `trade-extracted` (Epic 2)
- **Then** o backend envia uma notificação push/socket
- **And** o Dashboard recarrega apenas os componentes de dados afetados com uma animação de fade (250ms)

### Epic 4: Market Data & Motor de Recomendação (Filtro de Graham)
**Related PRD Requirements:** FR18, FR19, FR20, FR21, FR22, NFR3, NFR8

**Goal:** O usuário Premium recebe recomendações de aporte baseadas no Filtro de Graham com transparência total do raciocínio matemático.

#### Story 4.1: Integração de Market Data (Cotações & Indicadores)

**Related FRs/NFRs:** FR18, FR19, NFR8

**As a** sistema (backend),
**I want** buscar cotações atualizadas e indicadores fundamentalistas (P/L, DY, P/VP),
**So that** o cálculo do Filtro de Graham use dados de mercado confiáveis.

**Acceptance Criteria:**

- **Given** que o sistema possui ativos em custódia
- **When** o `market-data-service` executa o sync agendado
- **Then** consulta APIs externas (ex: Yahoo Finance, Brapi) e atualiza cotação e indicadores
- **And** armazena em cache por 24h para indicadores (Spring Cache/Caffeine)
- **And** usa cotação em cache se a API externa falhar (Graceful Degradation - NFR8)

#### Story 4.2: Configuração de Metas de Alocação (Tier Premium)

**Related FRs/NFRs:** FR20

**As a** investidor Premium,
**I want** definir uma meta percentual de alocação para cada classe (ex: 50% Ações, 30% FIIs) e ativos específicos,
**So that** o sistema saiba onde eu quero chegar.

**Acceptance Criteria:**

- **Given** que o usuário é Premium
- **When** ele acessa "Configurações de Estratégia"
- **Then** o sistema permite atribuir % alvo para classes e ativos
- **And** valida se a soma dos percentuais fecha em 100%
- **And** salva o plano de alocação por usuário (isolamento RLS)

#### Story 4.3: Motor do Filtro de Graham (`valuation-service`)

**Related FRs/NFRs:** FR21, NFR3

**As a** sistema,
**I want** processar o algoritmo de Graham cruzando cotação, indicadores e metas do usuário,
**So that** eu gere sugestões de compra precisas.

**Acceptance Criteria:**

- **Given** um gatilho de cálculo de valuation
- **When** o `valuation-service` consome as cotações e indicadores
- **Then** aplica o Filtro de Graham (Valor Intrínseco vs Preço Atual)
- **And** identifica ativos que estão abaixo do Valor Intrínseco e abaixo da meta de alocação
- **And** gera a recomendação de aporte em < 2 segundos (NFR3)
- **And** publica o evento `valuation-completed` no Kafka

#### Story 4.4: Reasoning Box — Explainable AI (Componente `ReasoningBox`)

**Related FRs/NFRs:** FR22

**As a** investidor,
**I want** visualizar o passo-a-passo matemático da recomendação,
**So that** eu entenda por que o sistema está me sugerindo aquele aporte específico.

**Acceptance Criteria:**

- **Given** que o sistema gerou uma recomendação de compra
- **When** o usuário clica em "Por que comprar?"
- **Then** abre o componente `ReasoningBox` exibindo as variáveis utilizadas (Preço atual, Graham Formula, Alocação Ideal vs Atual)
- **And** destaca em Emerald Gold (#F59E0B) a vantagem competitiva encontrada

### Epic 5: Metas & Projeção Financeira (Time-to-Goal)
**Related PRD Requirements:** FR23, FR24, FR25

**Goal:** O usuário configura suas metas de independência financeira e visualiza projeções de quanto tempo falta para atingi-las (o famoso "Time-to-Goal").

#### Story 5.1: Configuração de Metas de Patrimônio e Renda

**Related FRs/NFRs:** FR23

**As a** investidor,
**I want** definir um objetivo financeiro (ex: R$ 1 Milhão ou R$ 5.000 de renda mensal),
**So that** o sistema calcule minha rota de progresso.

**Acceptance Criteria:**

- **Given** que o usuário acessa a tela de "Minhas Metas"
- **When** ele preenche o formulário (Valor Alvo, Prazo Estimado, Aporte Mensal Pretendido)
- **Then** o sistema valida os dados e salva no perfil do usuário
- **And** exibe um resumo: "Você precisa de R$ X para atingir sua meta."

#### Story 5.2: Relógio Motivacional Time-to-Goal (Componente UI Principal)

**Related FRs/NFRs:** FR24

**As a** investidor,
**I want** ver um cronômetro ou contagem regressiva de anos/meses para minha meta,
**So that** eu mantenha o foco no longo prazo.

**Acceptance Criteria:**

- **Given** que o usuário possui uma meta ativa e dados de portfólio
- **When** ele acessa o dashboard central
- **Then** o sistema calcula a projeção baseada no Yield Atual + Aporte Mensal + Juros Compostos
- **And** exibe o componente visual "Tempo Restante para Independência": [X anos, Y meses]
- **And** a interface usa uma animação de "tick" suave para reforçar a ideia de progressão temporal

#### Story 5.3: Simulador de Sensibilidade ("E se?")

**Related FRs/NFRs:** FR24

**As a** investidor,
**I want** simular como um aumento no aporte mensal ou na rentabilidade afetaria meu tempo para a meta,
**So that** eu possa ajustar meu estilo de vida ou estratégia.

**Acceptance Criteria:**

- **Given** que o usuário está na tela de projeção
- **When** ele move um slider de "Aporte Mensal" ou "Rendimento Esperado (%)"
- **Then** o sistema recalcula instantaneamente o Time-to-Goal e atualiza o gráfico de projeção
- **And** exibe a diferença: "Isso encurtaria sua jornada em Z meses!"

#### Story 5.4: Checklist de Saúde da Meta

**Related FRs/NFRs:** FR25

**As a** investidor,
**I want** receber alertas se meu ritmo atual de aportes ou rentabilidade estiver me afastando da meta,
**So that** eu não tenha surpresas negativas no futuro.

**Acceptance Criteria:**

- **Given** que a projeção atual ultrapassa o prazo estimado pelo usuário
- **When** o dashboard é carregado
- **Then** exibe um badge de atenção (Warning #EF4444)
- **And** sugere ações: "Aumente seu aporte em R$ 200 para voltar ao trilho" ou "Revise seu yield alvo".

### Epic 6: Administração & Operações (Saúde do Sistema)
**Related PRD Requirements:** FR19, FR26, FR27

**Goal:** Administradores podem monitorar a saúde do sistema e reprocessar importações falhas para garantir a integridade dos dados de todos os usuários.

#### Story 6.1: Painel de Monitoramento Admin

**Related FRs/NFRs:** FR26

**As a** administrador do sistema,
**I want** visualizar métricas agregadas de uso (número de usuários, volume de uploads, taxa de erro de parse),
**So that** eu identifique problemas técnicos ou gargalos de infraestrutura proativamente.

**Acceptance Criteria:**

- **Given** que o usuário possui a flag `role: ADMIN` no JWT
- **When** ele acessa `/admin/dashboard`
- **Then** o sistema exibe gráficos de: Usuários Ativos (24h), Sucesso vs Erro de Upload (Kafka) e Latência Média do Motor Graham
- **And** os dados financeiros sensíveis (portfólio individual) aparecem anonimizados

#### Story 6.2: Reprocessamento de Importações Falhas (DLQ UI)

**Related FRs/NFRs:** FR27

**As a** administrador,
**I want** visualizar os arquivos que falharam no processamento e disparar o reprocessamento manual,
**So that** o usuário não precise fazer o upload novamente em caso de erro transiente no servidor.

**Acceptance Criteria:**

- **Given** que existem mensagens na `trade-extracted-dlq` (Dead Letter Queue) do Kafka
- **When** o administrador acessa a lista de "Falhas de Ingestão"
- **Then** o sistema lista: Arquivo, Erro Reportado e Timestamp
- **When** o administrador seleciona um item e clica em "Reprocessar"
- **Then** o sistema repubblica a mensagem no tópico original para nova tentativa
- **And** remove da lista de falhas se o reprocessamento for bem-sucedido

#### Story 6.3: Gestão de Ativos e Tickers (Mestre de Dados)

**Related FRs/NFRs:** FR19

**As a** administrador,
**I want** cadastrar novos tickers ou corrigir informações de classes de ativos,
**So that** o sistema reconheça novos papéis que entrem no mercado (IPOs).

**Acceptance Criteria:**

- **Given** que um novo ativo foi lançado na B3 e não está no banco
- **When** o admin cadastra o Ticker e a Classe correspondente
- **Then** o sistema atualiza o dicionário de ativos global
- **And** limpa o cache de Market Data para forçar o carregamento das novas informações

### Epic 7: Holding Familiar (Multi-CPF)
**Related PRD Requirements:** FR4

**Goal:** Usuários Premium podem consolidar a visão de patrimônio de toda a família em um único dashboard.

#### Story 7.1: Vínculo de Novo CPF à Conta Matriz
**As a** usuário Premium e gestor familiar,
**I want** vincular o CPF de um dependente à minha conta,
**So that** eu possa gerenciar os investimentos da família centralizadamente.

**Acceptance Criteria:**
- **Given** que o usuário é Premium
- **When** ele informa um novo CPF e Nome para vínculo
- **Then** o sistema cria um sub-tenant lógico vinculado ao ID mestre
- **And** permite upload de arquivos B3 específicos para esse novo perfil.

### Epic 8: Tags de Carteira & Visões Personalizadas
**Related PRD Requirements:** FR14, FR15

**Goal:** Permitir que o usuário organize seus ativos em categorias lógicas personalizadas.

#### Story 8.1: Criação e Associação de Tags
**As a** investidor,
**I want** criar etiquetas como "Aposentadoria" ou "Longo Prazo" e associar aos meus ativos,
**So that** eu veja a rentabilidade separada por estratégia.

**Acceptance Criteria:**
- **Given** que o usuário está na tela de ativos
- **When** ele seleciona um ativo e cria/associa uma Tag
- **Then** o sistema persiste a relação ativo-tag-usuário
- **And** atualiza os filtros de dashboard para incluir essa nova tag.

### Epic 9: Expansão de Ingestão de Dados (Post-MVP)
**Related PRD Requirements:** FR9, FR10, FR11

**Goal:** Expandir a coleta de dados (Crawler, PDF e APIs externas).

#### Story 9.1: Importação de Nota de Corretagem (PDF)
**As a** investidor,
**I want** fazer upload de PDFs de notas de corretagem,
**So that** os dados sejam extraídos automaticamente via Vision/Parser.

#### Story 9.2: Web Crawler de Proventos e Fatos Relevantes
**As a** sistema,
**I want** buscar anúncios de dividendos nos canais oficiais,
**So that** a agenda de proventos esteja sempre atualizada.

#### Story 9.3: Conector Open Finance
**As a** usuário,
**I want** conectar minha conta de investimento diretamente,
**So that** eu não precise fazer uploads manuais.

### Epic 10: Relatórios & Benchmarking
**Related PRD Requirements:** FR12, FR13

**Goal:** Comparar performance com índices e gerar bases para IR.

#### Story 10.1: Relatório de Bens e Direitos (Informe Anual)
**As a** investidor,
**I want** gerar um relatório consolidado em 31/12,
**So that** eu possa preencher o meu IR.

#### Story 10.2: Cálculo de Lucro Realizado e Isenção
**As a** investidor,
**I want** saber quanto tive de lucro e se passei dos R$ 20k,
**So that** eu pague o DARF corretamente.

### Epic 11: Notificações Multicanal
**Related PRD Requirements:** FR25

**Goal:** Engajamento e alertas em tempo real.

#### Story 11.1: Alertas via WhatsApp (Gatilhos de Meta)
**As a** investidor,
**I want** receber zaps de rebalanceamento e proventos,
**So that** eu tome decisões rápidas.

#### Story 11.2: Web Push para Zona de Compra (Graham)
**As a** investidor,
**I want** notificação no navegador quando o preço bater o Graham,
**So that** eu aporte com desconto.
