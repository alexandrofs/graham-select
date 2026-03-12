---
stepsCompleted:
  - step-01-init
  - step-02-discovery
  - step-02b-vision
  - step-02c-executive-summary
  - step-03-success
  - step-04-journeys
  - step-05-domain
  - step-06-innovation
  - step-07-project-type
  - step-08-scoping
  - step-09-functional
  - step-10-nonfunctional
inputDocuments:
  - docs/bmad/planning-artifacts/product-brief-graham-select-2026-03-09.md
  - docs/bmad/planning-artifacts/research/domain-gestao_carteiras_investimentos-research-2026-03-09.md
  - docs/bmad/planning-artifacts/research/market-aplicativos_de_gestao_de_carteira_de_investimentos-research-2026-03-09.md
  - docs/bmad/planning-artifacts/research/technical-extracao_dados_b3-research-2026-03-09.md
  - docs/bmad/brainstorming/brainstorming-session-2026-03-08-21-48-37.md
  - docs/bmad/project-context.md
  - docs/deploy.md
  - docs/visao-produto.md
  - docs/landing-page-proposta.md
documentCounts:
  briefCount: 1
  researchCount: 3
  brainstormingCount: 1
  projectDocsCount: 4
classification:
  projectType: web_app
  domain: fintech
  complexity: high
  projectContext: brownfield
workflowType: 'prd'
---

# Product Requirements Document - graham-select

**Author:** Alex
**Date:** 2026-03-10

## Executive Summary

O Graham Select é uma plataforma projetada para eliminar a fricção na construção de patrimônio. Ele resolve a complexidade e a dispersão do acompanhamento de investimentos consolidando carteiras pulverizadas em múltiplas corretoras e titularidades. O produto projeta claramente o futuro financeiro do investidor, indicando o tempo restante para o alcance das metas financeiras com base na performance real.

### Core Value Differentiator

O diferencial central reside na união de Inteligência Artificial com análise de dados fundamentalistas para indicar **instantaneamente onde o próximo aporte deve ser alocado**. O app elimina a paralisia da análise e a "fricção técnica" através de:
- **Zero Atrito de Ingestão:** Arquitetura focada na extração impecável de dados brutos da B3, tolerando edge-cases como desdobramentos ou erros de corretora sem travar a experiência.
- **Recomendação Prescritiva (IA):** Sugestões de aporte automatizadas (Filtro de Graham), balizadas por regras de alocação estritas (Tenant) e passíveis de auditoria visual (Explainable AI).
- **Consolidação Familiar (Multi-Account):** Capacidade exclusiva de agrupar o patrimônio cruzado da família, isolando ao mesmo tempo a rentabilidade exata e sugestões por CPF/Tags de Carteira de forma instantânea.
- **Projeção de Futuro:** Evolução clara do "Tempo de Vida até a Meta" baseada no yield atual em meses/anos.

## Project Classification

- **Project Type:** Web App / SaaS
- **Domain:** Fintech (Gestão de Investimentos)
- **Complexity:** High (Dados financeiros, integração B3, segurança de dados, inteligência de aportes)
- **Project Context:** Brownfield (Integração com infraestrutura, arquitetura e documentação pré-existentes)

## Success Criteria

### User Success
O usuário alcançará o sucesso quando:
- Conseguir inserir 5 anos de transações subindo apenas o arquivo da B3, sem incidentes pesados ou frustrações no onboarding.
- Estabelecer uma recorrência semanal passiva, abrindo o app com fluidez (menos de 5 minutos, garantido pela agilidade da plataforma) para consultar seu portfólio consolidado.
- Os inputs/correções manuais em cima da base importada representarem menos de 5% do volume total de dados inseridos, provando a eficácia e confiabilidade da integração automatizada.

### Business Success
O negócio será considerado um sucesso se atingir:
- Uma taxa de crescimento de usuários ativos de 50% ao mês.
- Um custo de infraestrutura por usuário em trajetória decrescente, garantindo a viabilidade financeira e a escalabilidade do modelo SaaS (e das chamadas de IA/Cloud).

### Technical Success
Os requisitos inegociáveis de engenharia para validar a solução são:
- Retorno da carga e consolidação de dados massivos da B3 (historicamente densos em eventos corporativos) em um tempo bruto menor do que 5 minutos para a visualização completa do usuário.
- Estabilidade nas integrações para não gerar quebras de rentabilidade que minam a confiança do usuário no cálculo da IA.

### Measurable Outcomes
- Tempo de carregamento/processamento da B3 < 5 minutos.
- Correções manuais pós-B3 < 5%.
- Crescimento mensal de MAU (Monthly Active Users) >= 50%.
- Custo/MAU: Decrescente mês a mês.

## Product Scope

### MVP - Minimum Viable Product
O MVP focará estritamente em provar a proposta de valor central:
- Importação completa do histórico B3 (upload de arquivo manual por enquanto).
- Motor de consolidação visual das rentabilidades passadas (incluindo tratamento inicial de múltiplos CPFs).
- Sugestão "De cara" das empresas/aportes suportada pela união de cotações com algoritmos de qualidade (IA/Fundamentalismo).
- Interface de acompanhamento da evolução patrimonial em direção à meta (tempo/valor faltante).

### Growth Features (Post-MVP)
Funcionalidades competitivas a serem inseridas após validação inicial:
- Sincronização Open Finance direta com corretoras.
- Leitura Computacional via imagem (OCR de aplicativos).
- Expansão das recomendações via WhatsApp, notificações push.

### Vision (Future)
- **Copiloto Financeiro Ativo:** Motor de IA conversacional via WhatsApp para insights comportamentais baseados em fundamentos, atuando como o conselheiro que evita a sabotagem.

## User Journeys

### 1. Thiago: A "Epifania" do Aporte (Primary User - Core Success Path)
**Situação:** Recebe o salário e tem 40 minutos de trabalho manual adiado em planilhas para calcular o próximo aporte de rebalanceamento.
**A Jornada:** Sobe o PDF/XLS da B3. Em < 5 minutos o sistema carrega seus 4 anos de histórico. A tela exibe a ação tática: "Você está 5% abaixo da meta em FIIs. Pelo filtro de Graham, o ativo X é a melhor alocação hoje para o saldo de R$ 2.000".
**Clímax:** Ele checa a premissa matemática no app (transparência), valida a lógica e executa a ordem em sua corretora, liberando seu domingo.

### 2. Mariana: A Recuperação Rápida (Primary User - Edge Case Recovery)
**Situação:** Vê uma queda irreal de 40% em ITUB4 no seu gráfico por conta de um evento corporativo não computado pela bolsa.
**A Jornada:** Clica no ativo afetado. O sistema exibe um alerta inteligente sobre a possível anomalia e guia Mariana para uma tela de "Correções CRUD", sugerindo a inserção do evento ou operação faltante.
**Clímax:** Os cálculos de rentabilidade estilizam instantaneamente. O gráfico de "anos até a meta" é corrigido. A confiança na integridade da plataforma é reforçada.

### 3. Roberto: A Visão da "Holding Familiar" (Primary User - Multi-Account)
**Situação:** O chefe de família gerencia seu patrimônio próprio e da esposa em corretoras separadas, mas eles pensam no dinheiro como um montante único com destinos diferentes.
**A Jornada:** Acessa a "Holding". A visão global exibe o total agregado, mas um simples clique nos filtros de perfil (Tag/CPF) recalcula todo o dashboard, focando exclusivamente na rentabilidade dos "Dividendos da Esposa" sem misturar aportes.
**Clímax:** Exibe para a esposa o resultado tático da conta dela no celular, sem a fricção de planilhas fracionadas.

### 4. Alex: A Estabilidade do Motor (System Admin/Operations)
**Situação:** Atualização silenciosa de layout de exportação por parte da B3 afeta as chaves de leitura, quebrando os robôs de usuários novatos.
**A Jornada:** O Painel Admin alerta sobre um pico de falhas de ingestão XML. Alex checa logs limpos isolados no Sentry.
**Clímax:** Aplica hotfix em horas e manda o backend reprocessar proativamente a fila de arquivos falhos (Dead-letter), recuperando o parse sem os usuários notarem o erro fatal.

## Domain-Specific Requirements

### Compliance & Regulatory
- **Lei Geral de Proteção de Dados (LGPD):** O sistema processa históricos financeiros e CPFs, exigindo consentimento explícito, política forte de retenção e "direito ao esquecimento" (exclusão definitiva de conta).
- **Adequação CVM:** Avaliação rigorosa sobre interface/IA. O Graham Select automatiza a matemática dos fundamentos públicos, não emitindo relatórios de casa de análise e não exercendo consultoria. Os resultados da IA são "filtros de lógica configurada pelo usuário". Disclaimers visíveis em todas as telas de aporte são obrigatórios.
- **Preparação Open Finance (Fase 2):** Planejamento da arquitetura técnica atual para os padrões exigidos pelo Bacen (ex: mTLS) para o futuro consumo de APIs das corretoras.

### Technical Constraints
- **Multi-tenant Data Isolation (Isolamento de Dados Seguro):** A arquitetura deve garantir que, no nível do banco de dados (ex: uso de Row Level Security - RLS no Postgres ou chaves de partição estritas), seja impossível que um bug de código na aplicação vaze ou exponha os saldos, transações ou posições de um usuário (Tenant A) para outro usuário não autorizado (Tenant B).
- **Segurança de Dados e Privacidade:** Criptografia forçada de informações sensíveis (saldos, aportes) em repouso e em trânsito.
- **Precisão Matemática:** Uso intransigente de precisão decimal alta/exata (`Decimal`/`Numeric` types, sem uso de ponto flutuante via `Float`/`Double` que causam problemas de arredondamento) na consolidação de juros compostos e dividendos fracionados de 10 anos.
- **Auditabilidade e Trilhas de Auditoria:** Qualquer inserção ou exclusão feita manualmente pelo usuário gera um log imutável, fundamental para rastrear divergências e provar falhas sistêmicas.

### Integration Requirements
- **Resiliência do Parser da B3:** A extração do TXT/XML B3 é o calcanhar de aquiles estrutural. Se o arquivo da B3 mudar a formatação em operações raras, o sistema não deve falhar a extração das operações padrão.
- **Market Data Confiável e Tempo Real:** A inteligência da alocação instantânea requer integração robusta a provedores de cotações e fundamentos (Balanços, P/L, P/VP, DY), com fallbacks de cache em caso de indisponibilidade da API primária.

### Risk Mitigations
- **Risco de Falso-Positivo na IA:** Se o dado fundamentalista da API terceira vier errado (ex: um desdobramento de cotação não computado ainda gera um preçoi/lucro ilusório), a IA mandaria o usuário alocar errado. *Mitigação:* Implementar checagem de integridade (circuit breaker) e validação de outliers nas cotações antes que a engine gere a recomendação.
- **Vazamento de Visibilidade Familiar:** No caso do perfil que visualiza a conta da esposa/filhos sob o mesmo "Guarda-Chuva". *Mitigação:* Controles de permissão estritos baseados em identificadores de sub-contas e aceite de compartilhamento interno para garantir o acesso restrito apenas aos vinculados autoritativos.

## Innovation & Novel Patterns

### Innovation Differentiation
- **Decision Engine Prescritiva Integrada:** Diferente de consolidadores tradicionais voltados ao "retrovisor analítico", o Graham Select atua ativamente cruzando a meta percentual do investidor com métricas fundamentalistas de mercado em tempo real (Filtro de Graham) para ditar **como agir hoje**.
- **Time-to-Goal (Gamificação):** Traduz métricas complexas de taxa de juros e yield em um relógio motivacional compreensível que aponta "quantos anos de vida faltam para a meta financeira real", estimulando depósitos consistentes.

### Market Risks & Fallback Mechanisms
- **Descolamento de Recomendação:** Risco da IA recomendar falsas barganhas por ruídos nos balanços importados.
- **Fallback Transparente (Explainable AI):** Imprescindível exibir o racional matemático simplificado e transparente ("Caixa de Cálculo") do porquê o sistema elegeu aquele ativo como aporte do dia, devolvendo a agência ao investidor e evitando a "Trust Box" cega.

## Project-Type Specific Requirements (Web App / SaaS)

### Project-Type Overview
O Graham Select é abordado sob um modelo SaaS B2C/B2B (Software as a Service) com entrega via Web App *Cross-platform*. Ele provê uma experiência de gerenciamento de patrimônio baseada em multi-tenant cloud, oferecendo tiers de acesso baseados em funcionalidades premium de recomendação algorítmica.

### Technical Architecture Considerations

#### 1. Frontend Framework: Flutter (Cross-Platform)
- O cliente será renderizado utilizando o framework **Flutter**. Isso garante uma interface de usuário extremamente reativa, com renderização gráfica consistente em múltiplos navegadores, e estrutura nativamente preparada para expansão futura rápida via empacotamento em aplicativos móveis (iOS/Android).
- **Performance Targets:** Como será um app focado no motor Flutter, a métrica de sucesso de "carregamento em < 5 minutos" com fluidez "app-like" nas transições de dashboards sem reload de tela deve ser a regra primária do client.

#### 2. SEO e Acessibilidade (Browser Matrix & Strategy)
- **Ações de SEO:** O aplicativo `logado` do Graham Select **não** possuirá metadados de otimização de busca, protegendo as configurações privadas de rotas por razões de performance e segurança.
- **Ecossistema:** Todo o tráfego orgânico e aderência aos requisitos de indexação (Google bots, metatags, Core Web Vitals) serão direcionados com exclusividade para o website externo estático/público (Landing Page de marketing e conteúdo).

#### 3. SaaS Subscription Tiers (Tenant Model e Acessos)
- A arquitetura do banco de dados (Tenants) será projetada suportando diferenciações (flags) no perfil do usuário, baseada no esquema **Freemium com Limitações Funcionais**:
  - **Tier Gratuito (Basic):** Acesso à ferramenta visual, upload simplificado de carteiras e painel de acompanhamento passivo de investimentos, sem ativação do motor de Inteligência Artificial ou uso do recomendador de IA+Fundamentos (Copiloto/Premium insights).
  - **Tier Premium (Pro):** Acesso total, incluindo inteligência IA sobreposta às cotações, e gestão do holding familiar (múltiplos CPFs).
  - **Período de Experiência (Trial):** Para tracionar conversão, o motor autorizador (Auth/Billing) suportará a mecânica de liberação (Trial) de 30 a 60 dias das "regras Premium" aos novos entrantes (conversão PLG - Product-led Growth).

## Project Scoping & Phased Development

### MVP Strategy & Philosophy
**MVP Approach:** *Problem-Solving & Experience MVP.* O objetivo primário da Fase 1 é validar a redução radical de atrito. O usuário deve conseguir importar seus dados da B3 de anos de histórico e receber sua primeira recomendação acionável em menos de 5 minutos, provando o valor do "Fim da Paralisia da Análise".
**Resource Requirements:** Equipe técnica com proficiência forte e obrigatória em Flutter (Front) e manipulação assíncrona/filas de dados pesados + Integração de APIs terceiras de mercado (Back-end).

### MVP Feature Set (Phase 1)
### MVP Feature Set (Phase 1)
- Importação completa do histórico B3 (upload de arquivo).
- Motor de consolidação visual das rentabilidades passadas (Dashboard).
- Sistema de Recomendação de Aporte (Algoritmo Graham/IA).
- Visão visual gamificada do tempo até a meta financeira.

*(Ver mapa completo das capacidades desta fase no módulo "Functional Requirements" - Seções 1, 4, 5 e 6).*

### Post-MVP Features (Growth & Expansion)
- Visão Holding Familiar Multi-CPF (Phase 2).
- Tags de Carteira customizáveis - Progressive Profiling (Phase 2).
- Parser em arquivo PDF da nota de corretagem (Phase 2).
- Sincronização direta via Open Finance (Phase 3).
- Visão Global com corretoras internacionais e Cripto (Phase 3).
- Execução de ordens diretamente no app (Vision).

## Functional Requirements

### 1. Account & Subscription Management
- **FR1:** Usuários podem criar uma conta na plataforma utilizando e-mail ou provedores sociais (Google/Apple).
- **FR2:** Usuários podem visualizar e alterar seu nível atual de assinatura (Gratuito ou Premium).
- **FR3:** O Sistema deve restringir o acesso à recomendação baseada em Inteligência Artificial para contas no tier Gratuito (após o Trial expirar).
- **FR4:** Usuários Premium podem vincular e visualizar múltiplos perfis/CPFs (Holding Familiar) sob uma mesma conta matriz.
- **FR5:** O Sistema deve permitir a exclusão definitiva da conta e expurgo total dos dados financeiros atrelados ao CPF (Direito ao Esquecimento LGPD).

### 2. Data Ingestion & Integration (A B3 & Beyond)
- **FR6:** Usuários podem realizar o upload manual de arquivos extraídos da Área Logada da B3 (PDF, XLS ou XML).
- **FR7:** O Sistema deve extrair, estruturar e salvar as operações históricas contidas no arquivo em background (Assíncrono).
- **FR8:** O Sistema deve notificar o usuário sobre o sucesso ou falha do processamento do arquivo importado.
- **FR9 (New/Post-MVP):** Usuários poderão realizar o upload das Notas de Corretagem padrão (SINACOR) em arquivo PDF para alimentar as operações sem depender exclusivamente dos relatórios da bolsa.
- **FR10 (New/Post-MVP):** O Sistema permitirá a integração, cadastro manual ou parse de dados oriundos de corretoras internacionais (Nyse/Nasdaq) e carteiras/plataformas de criptoativos, viabilizando a recomendação de alocação de "All-in-One Global Portfolio".
- **FR11 (Post-MVP):** Usuários poderão conectar suas contas via Open Finance para sincronização automática total de custódia nacional e proventos.

### 3. Portfolio Management & Taxonomia (Tags)
- **FR12:** Usuários podem visualizar a custódia atualizada do seu portfólio (posições ativas).
- **FR13:** Usuários podem visualizar o histórico de proventos recebidos e aportes realizados.
- **FR14:** Usuários podem criar "Tags de Carteira" personalizadas (ex: Aposentadoria, Hold, Dividendos) e associar ativos a essas tags (como "Progressive Profiling" em etapas pós-cadastro).
- **FR15:** O Sistema deve permitir a visualização de consolidações, rentabilidade e recomendação de IA filtradas especificamente por essas Tags de Carteira.
- **FR16:** Usuários podem inserir, editar ou deletar (CRUD) operações de ativos manualmente.
- **FR17:** O Sistema deve manter uma trilha de auditoria imutável (log) de toda criação, alteração ou deleção manual.

### 4. Market Data & AI Engine (The Brain)
- **FR18:** O Sistema deve acessar via integração (API) as cotações financeiras atualizadas (end-of-day ou real-time) dos ativos custodiados.
- **FR19:** O Sistema deve acessar e armazenar atualizações dos indicadores fundamentalistas das empresas acompanhadas (P/L, DY, P/VP, etc.).
- **FR20:** Usuários Premium podem definir uma "Meta Percentual" de alocação por classe de ativos e por ativo específico.
- **FR21:** O Sistema deve cruzar a carteira atual do usuário, as metas definidas e o Filtro Fundamentalista (Filtro de Graham) para gerar recomendações primárias de compra com o capital disponível (Isolado e configurado via backend em Engine dedicada).
- **FR22:** O Sistema deve apresentar abertamente o racional matemático por trás da recomendação de ativo gerada pela IA (Explainable AI - Fallback).

### 5. Goals & Gamification 
- **FR23:** Usuários podem configurar um valor financeiro alvo como "Meta de Independência Financeira" ou "Renda Passiva Mensal Alvo".
- **FR24:** O Sistema deve projetar e visualizar o tempo de vida restante (anos/meses) para atingir a meta financeira com base na taxa de rentabilidade e aportes atuais.
- **FR25:** (Post-MVP) O Sistema enviará alertas acionáveis via WhatsApp contendo resumos do recebimento de proventos e gatilhos de rebalanceamento.

### 6. Admin & Operations
- **FR26:** Administradores do sistema podem visualizar o volume e as taxas de falha (tracing) das importações de arquivos B3 de forma anonimizada.
- **FR27:** Administradores podem disparar o reprocessamento em lote (fila manual) de arquivos que falharam no parser após a subida de um hotfix corretivo.

## Non-Functional Requirements

### Performance
Tolerância zero para lentidão. O principal atrito das velhas planilhas é a demora no input e atualização.
- **NFR1 (Tempo de Parse):** O processamento assíncrono do arquivo bruto da B3 (PDF/XLS/XML) e a consolidação inicial da carteira no banco de dados deve ocorrer em até `< 5 minutos` (P95) para arquivos extensos.
- **NFR2 (Responsividade UX):** Como o front-end é construído utilizando renderização com base nativa (Flutter), as transições visuais (Dashboard, Portfólio, Sugestões) não devem exceder `< 300ms` via requisições REST/GraphQL, garantindo a sensação de fluidez ("app-like").
- **NFR3 (Recomendação IA):** O cálculo da recomendação de aportes, que cruza saldos (Tenant), API de Fundamentos e as Tags do Usuário, deve responder em `< 2 segundos`.

### Security
Sendo um SaaS que trafega a custódia patrimonial do usuário e seu CPF.
- **NFR4 (Isolamento de Tenant - LGPD):** Nenhuma query ou extração do backend deve ter arquitetura suscetível ao vazamento acidental de histórico de ordens entre IDs (Implementação estrutural de Multi-tenancy lógica via Row-Level Security no DB).
- **NFR5 (Data Sanitization & GDPR):** A solicitação de fechamento da conta deve invocar em menos de 24 horas um expurgo efetivo (Hard Delete) dos espelhos de transações B3 e recomendações.
- **NFR6 (Criptografia):** Elementos sensíveis (PIIs, saldos reais, e posteriormente tokens Open Finance) devem habitar banco cifrado em repouso (AES-256) e transitar criptografados (TLS 1.3).

### Scalability
- **NFR7 (Ingestão Elástica):** A arquitetura do parser e do *Engine Core* da IA precisará tolerar e isolar cargas em filas elásticas (bus/workers) caso o volume de upload simultâneo ocorra todo mês nos "dias mundiais de aporte" (ex: dia útil 05 ou dia 10 de recebimento de P/L). A fila pesada não pode quebrar o app visual do NFR2.

### Reliability
- **NFR8 (Graceful Degradation):** Em caso de queda momentânea da API terceira de Cotações ou do módulo da B3 parceira, o aplicativo deve continuar operável. O front-end exibirá as posições com um indicador tático de "Cotação em Cache - Desatualizado" no último fechamento (D-1/D-X), permitindo navegação sem telas "em branco".
