---
status: final
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
inputDocuments:
  - docs/bmad/planning-artifacts/prds/prd-graham-select-2026-11-21/prd.md
  - docs/bmad/planning-artifacts/architecture/architecture-graham-select-2026-11-21/ARCHITECTURE-SPINE.md
  - docs/bmad/planning-artifacts/architecture/architecture-graham-select-2026-11-21/solution-design.md
  - docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/DESIGN.md
  - docs/bmad/planning-artifacts/ux-designs/ux-graham-select-2026-11-21/EXPERIENCE.md
---

# Kerno (ex-Graham Select 2.0) - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Kerno (ex-Graham Select 2.0), decomposing the requirements from the PRD, UX Design if it exists, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: Identidade de Marca Unificada Kerno — O sistema deve exibir o nome Kerno, logotipo, paleta cromática institucional, favicon, ícones de aplicativo (iOS/Android), títulos de página, metadados SEO/PWA e textos institucionais atualizados em 100% das telas públicas e autenticadas, operando diretamente sob o domínio oficial `kerno.afsdigital.com.br`, preservando identificadores internos de infraestrutura para evitar recriação destrutiva no Kubernetes.
FR2: Design System Intergeracional & Acessibilidade Visual — A interface Web e Mobile deve aplicar os tokens do Design System Intergeracional, garantindo contraste mínimo WCAG 2.1 AA (4.5:1 para texto normal e 7:1 AAA para saldos primários), tipografia com numerais tabulares (`tabular-nums`), áreas de toque mínimas de 48x48dp, suporte nativo ao escalonamento de fonte do SO (`textScaler` até 2.0x sem truncamento/ellipsis) e cartões estruturados empilhados sem scroll horizontal móvel.
FR3: Preservação e Adaptação Visual dos Fluxos Brownfield (Ingestão B3, CRUD Multi-Classe e Multi-CPF) — Os fluxos existentes de importação assíncrona da B3 (com descarte imediato do arquivo bruto com PII), edição manual de posições/proventos nas 5 Macro Classes (Ações BR, FIIs, Renda Fixa / Tesouro, Internacional e Criptoativos) e filtro de CPFs (Holding Familiar com consentimento LGPD expresso) devem operar integralmente sob o Design System Intergeracional e identidade Kerno com 100% dos testes de regressão aprovados.
FR4: Onboarding com Templates de Alocação e Metas de Macro Classes — O investidor autenticado pode escolher explicitamente (sem pré-seleção automática) um Template de Alocação pré-configurado (*Modelo Equilibrado Cerrado*, *Modelo Moderado Patrimonial*, *Modelo Acumulação Arrojada*) ou definir manualmente as metas percentuais das 5 Macro Classes (soma estrita obrigatória de 100.00% validada visualmente e via HTTP 422), com inicialização das Notas de Qualidade neutras padrão iguais a 6 (sem viés editorial CVM).
FR5: Gestão de Notas de Qualidade (1–10), Teto Máximo de Risco e Quarentena de Aporte — O investidor pode atribuir ou editar a Nota de Qualidade (1–10) de cada ativo via slider direto ou assistente opcional de autoavaliação com 5 critérios objetivos (Governança, Lucro 5 anos, Dívida, Vantagem Competitiva, Proventos), calculando a meta percentual proporcional dentro da Macro Classe em `BigDecimal`, aplicando o Teto Máximo de Risco (8–10: 10%; 5–7: 6%; 1–4: quarentena 0% de novos aportes) com isenção restrita a títulos soberanos e ETFs de índices amplos, redistribuindo excedentes de teto intra-classe.
FR6: Cálculo do GPS de Aporte com Regra de Transbordamento (Spillover) — Quando o investidor informa um valor disponível (`R$ X > 0`), o sistema calcula de forma stateless o patrimônio projetado, determina as defasagens relativas de Macro Classes e ativos, e distribui matematicamente o aporte priorizando maiores defasagens sem sugerir venda de posições, resolvendo empates por tie-breaker determinístico (Nota -> Margem Graham -> Déficit monetário -> Ticker A-Z), quantizando cotas inteiras in-loop e fazendo o troco/excedente transbordar para os próximos ativos elegíveis.
FR7: Integração do Multiplicador de Graham no GPS de Aporte — Durante a priorização de Ações BR, o motor do GPS de Aporte cruza os ativos elegíveis no Diagrama com o cálculo da Margem de Segurança de Graham ($VI = \sqrt{22.5 \times LPA \times VPA}$) e aplica o Multiplicador de Graham configurável (padrão 1.30x) sobre o score de defasagem das ações com margem positiva (Cotação < VI), exibindo o selo *"Elegível com Margem de Segurança (Graham)"* sem jamais ultrapassar o Teto Máximo de Risco.
FR8: Transparência Matemática e Explicabilidade do Aporte (Conformidade CVM) — Cada simulação gerada pelo GPS de Aporte apresenta um painel expansível (*"Por que estes ativos?"*) detalhando % Atual Pré-Aporte, % Meta Efetiva, % Projetada Pós-Aporte, Nota, Teto de Risco, e dados de Graham (LPA, VPA, VI, Margem), acompanhado de disclaimer de conformidade CVM permanente informando que se trata de simulação matemática automatizada baseada nas metas do próprio investidor.
FR9: Geração de Diagnóstico Estruturado da Carteira via Kerno AI — O investidor pode solicitar sob demanda diagnóstico completo integrado ao Google Cloud Vertex AI / Gemini Enterprise (modelo pinado `gemini-2.5-flash` com Zero Data Retention) via porta agnóstica `AiPortfolioAnalyzerPort`, gerando 4 seções estruturadas (Resumo Executivo, Pontos Fortes, Alertas de Risco/Concentração, Racional do Plano de Aporte), com validador determinístico de ancoragem numérica e de entidades, cache de 24h por `portfolio_state_hash`, rate limiting (5/dia e 30/mês por usuário Pro/Founder) e degradação graciosa com fallback quantitativo local em caso de erro ou timeout > 15s.
FR10: Sanitização LGPD de PII, ZDR e Guardrails Regulatórios CVM no Kerno AI — Toda requisição ao LLM passa pelo Sanitizador LGPD de IA que remove 100% dos dados pessoais e valores absolutos em R$, valida tickers contra allowlists da B3/Exterior e tabelas fechadas de Tesouro/Cripto, convertendo ativos manuais em rótulos canônicos (`RENDA_FIXA_MANUAL_1`), além de aplicar guardrail léxico pós-geração com bloqueio de modos imperativos/exortativos de compra/venda da CVM, ativando fallback determinístico imediato sem retry síncrono.
FR11: Autenticação Mobile OAuth2/Google com Persistência Segura e Desbloqueio Biométrico — Aplicativo Mobile (iOS e Android) autentica via Google Sign-In / OAuth2 + JWT, armazena tokens exclusivamente via `flutter_secure_storage` (Keychain / Keystore), exige desbloqueio biométrico nativo via `local_auth` na abertura ou após 2 minutos de inatividade em background, com suporte a fallback de credencial do SO e cache offline criptografado do Dashboard e Diagrama.
FR12: Dashboard Patrimonial Mobile com Modo Privacidade e Proteção de Snapshot em Background — O Dashboard Patrimonial apresenta patrimônio consolidado, rentabilidade e gráfico de Macro Classes atual vs. meta, com toggle de Modo Privacidade no cabeçalho mascarando valores monetários (`R$ ••••••`) persistido localmente, e proteção nativa de snapshot (`FLAG_SECURE` / overlay de máscara) ao entrar em background para evitar exposição no App Switcher do SO.
FR13: Arquitetura de Navegação Modular com Feature Flags (Pronta para Controle de Gastos) — Navegação principal (`BottomNavigationBar` no Mobile e Sidebar na Web) estruturada com 4 áreas no lançamento (Carteira, GPS de Aporte, Kerno AI, Diagrama) e slot pré-mapeado para Controle de Gastos (`features/expenses`) sob feature flag `enable_expenses_module = false`, com contrato de entrada do GPS de Aporte desacoplado para aceitar tanto valor manual quanto saldo livre mensal futuro.
FR14: Landing Page Kerno de Alta Conversão com Destaque do Programa Founders e Isolamento SEO — Landing Page pública servida em HTML estático/SSR leve com First Contentful Paint < 1.2s sob a marca Kerno, com seção de destaque do Programa Founders (primeiras 250 contas com acesso Pro vitalício), demonstração visual do GPS, Diagrama e Kerno AI, disclaimers CVM no rodapé e isolamento SEO via `robots.txt` bloqueando rotas autenticadas.
FR15: Elegibilidade ao Status de Membro Founder (Acesso Vitalício) e Camadas Comerciais (Free vs. Pro com Trial de 90 Dias) — Concessão atômica de `plan_tier = FOUNDER_LIFETIME` com selo Founder e acesso gratuito vitalício a todos os recursos Pro para os primeiros cadastros enquanto `founder_slots_remaining > 0` (lote padrão de 250 vagas), concedendo PLG Trial de 90 dias para cadastros posteriores, com transição não-destrutiva para Free (Resumo Executivo determinístico, sem chamadas ao LLM e sem perda de dados cadastrados).
FR16: Expurgo Integral LGPD das Entidades do Diagrama do Cerrado e Kerno AI (Direito ao Esquecimento) — Quando o usuário solicita exclusão de conta ou exclusão granular de CPF secundário, o `UserDataPurgeAdapter` cancela jobs assíncronos e realiza hard-delete atômico em cascata de todas as tabelas (Macro Classes, Notas de Qualidade, Snapshots/Diagnósticos de IA, caches e contadores), validado obrigatoriamente no teste de integração `AccountDeletionIT`.
FR17: Proteção contra Anomalias de Cotações (Circuit Breaker de Dados de Mercado) — Validação de variações diárias > 40% não ajustadas em renda variável B3 e Exterior, ativando o estado `QUARENTENA_EVENTO_CORPORATIVO`, congelando a última cotação válida para patrimônio, suspendendo simulações de aporte no ticker no GPS e oferecendo fluxo de resolução manual para o usuário confirmar ou retomar cotação.

### NonFunctional Requirements

NFR1: Velocidade do GPS de Aporte — O cálculo completo do GPS de Aporte (cruzando Diagrama do Cerrado, Regra de Transbordamento e Multiplicador de Graham) deve responder em < 1.0 segundo (P95) para carteiras de até 100 ativos.
NFR2: Fluidez de UI Web e Mobile — As transições de tela e ativação do Modo Privacidade no Flutter devem ocorrer a 60fps (< 300ms de resposta visual), e o carregamento do Dashboard Patrimonial autenticado deve concluir em < 1.5 segundo (P95) no Mobile nativo e < 2.5 segundos em cold-start no Web.
NFR3: Latência e Cache do Kerno AI — Leituras de diagnósticos cacheados do Kerno AI devem responder em < 400ms (P95); novas gerações via LLM devem retornar em < 10 segundos (P95), com timeout rígido e fallback determinístico em 15 segundos.
NFR4: Aritmética Decimal Estrita — 100% dos cálculos patrimoniais, pesos do Diagrama, Teto Máximo de Risco, Margem de Graham e distribuições do GPS devem utilizar tipos decimais exatos (`BigDecimal` com `RoundingMode.HALF_EVEN` no Java e `NUMERIC(19,4)` no PostgreSQL), sendo proibido uso de float/double.
NFR5: Isolamento Multi-Tenant e Criptografia — Todos os dados de carteira, metas do Diagrama e diagnósticos de IA devem respeitar isolamento estrito por `user_id` no banco, com criptografia TLS 1.3 em trânsito, AES-256 em repouso e armazenamento local restrito ao Keychain/Keystore via `flutter_secure_storage`.
NFR6: Rastreabilidade e Logs Obrigatórios — Toda entrada de requisição REST, consumo de filas Kafka e chamadas externas deve registrar log estruturado contextual (sem vazar PII/tokens); capturas e lançamentos de exceção devem ser logados no nível ERROR com stack trace completo.
NFR7: Imutabilidade de Migrations Liquibase — Nenhuma migration Liquibase existente pode ser alterada retroativamente; todas as novas tabelas e colunas devem ser criadas via novos changesets incrementais e idempotentes.
NFR8: Paridade Helm e CI/CD — Todas as novas variáveis de ambiente e propriedades (como chaves de IA, quotas de rate limit, feature flags) devem ser obrigatoriamente refletidas em `application.yml`, no chart Helm (`helm/graham-select/values.yaml`, `configmap.yaml`, `secret.yaml`) e em `.github/workflows/deploy.yml`, mantendo validação `*IT.java` e `flutter analyze` com zero erros/warnings.

### Additional Requirements

- Starter Template / Greenfield vs. Brownfield: Arquitetura brownfield construída diretamente sobre a base estável do Graham Select 1.0, preservando os fluxos assíncronos Kafka e o modelo relacional existente.
- Padrão Arquitetural Backend: Arquitetura Hexagonal (Ports & Adapters) em Java 21 / Spring Boot 3.4.13, isolando o núcleo matemático (`common/domain`) de adaptadores web e de persistência.
- Portas e Adaptadores de Domínio: Porta de IA `AiPortfolioAnalyzerPort` implementada pelo adaptador corporativo `VertexAiGeminiAdapter` (Google Cloud Vertex AI com ZDR e modelo fixo `gemini-2.5-flash`), `MarketDataPort` para cotações e `UserDataPurgeAdapter` para expurgo LGPD.
- Padrão Arquitetural Frontend: Flutter SDK ^3.11.0 / Dart 3.7+ estruturado no padrão Feature-First com Provider e GoRouter, gerando binários nativos ARM64 para iOS/Android e SPA Web.
- Persistência Relacional e Migrations: Criação incremental de tabelas `tb_cerrado_preferences`, `tb_cerrado_asset_grade`, `tb_ai_diagnostic_cache`, `tb_ai_usage_counter` e colunas de controle de Founders em `tb_user`, com índices compostos por `user_id`.
- Estratégia de Cache e Hash de Estado: Cache-aside com chave SHA-256 baseada no `portfolio_state_hash` (composto por tickers, quantidades, metas e notas) com TTL de 24 horas.
- Header HTTP de Rastreabilidade e Interceptor PII: Propagação obrigatória do cabeçalho `X-Correlation-ID` em todas as camadas e interceptor de segurança para sanitização de requisições.
- Concorrência Transacional de Founders: Decremento atômico de `founder_slots_remaining` com trava pessimista/otimista para garantir o teto exato de 250 contas vitalícias.

### UX Design Requirements

UX-DR1: Tokens de Cores e Temas do Design System Intergeracional — Implementação dos tokens Light-First (`#F8FAFC`, `#FFFFFF`, brand-primary `#4F46E5`) e Dark Mode nativo (`#0B0F19`, `#111827`, brand-primary-dark `#818CF8`), paleta semântica esmeralda/âmbar/carmesim e founder-gold.
UX-DR2: Rampa Tipográfica Escalonável e Numerais Tabulares — Definição das 11 escalas tipográficas (`display-wealth` a `caption`) com famílias Plus Jakarta Sans e Inter, configuração obrigatória de `fontVariantNumeric: 'tabular-nums'` e suporte a `textScaler` de 1.0x até 2.0x sem truncamento (`TextOverflow.ellipsis` proibido em cifras e tickers).
UX-DR3: Acessibilidade de Toque e Contraste WCAG — Garantia de áreas de toque mínimas de 48x48dp para todos os botões, chips e sliders, com contraste mínimo 4.5:1 para textos e 7.0:1 (WCAG AAA) para cifras patrimoniais e saldos principais.
UX-DR4: Componente Wealth Hero Card — Cartão elevado com contraste 17.85:1 AAA para patrimônio total, rentabilidade, suporte nativo ao Modo Privacidade (`R$ ••••••`), indicador discreto de cotações B3 (delay 15 min ou D-1) e reflow vertical sob fontes ampliadas.
UX-DR5: Componente Macro Class Distribution Card — Card analítico de exposição das 5 Macro Classes e Subclasses com barras de progresso duplo (atual vs. meta), sem rolagem horizontal em viewports móveis (`< 600dp`).
UX-DR6: Componente Contribution GPS Card — Cartão estruturado de sugestão de aporte por ativo destacando quantidade inteira de ações/cotas, valor alocado em R$, percentuais pré e pós-aporte e selo visual de bonificação de Graham.
UX-DR7: Componente Math Transparency Accordion — Acordeão expansível *"Por que estes ativos?"* detalhando % atual pré-aporte, % meta efetiva, % projetada pós-aporte, Nota de Qualidade, Teto de Risco, métricas de Graham (LPA, VPA, VI) e disclaimer regulatório CVM.
UX-DR8: Componente Risk Ceiling Saturation Card — Card semântico em âmbar informando com clareza o saldo excedente protegido por teto de risco (`unallocatedRiskProtectedBalance`), classes vazias e troco residual.
UX-DR9: Componente Cerrado Template Selector Card — Seletor visual dos 3 templates pré-configurados (*Equilibrado*, *Moderado*, *Acumulação*) com destaque de borda/fundo na seleção, inicialização de notas neutras `= 6` e trava de soma 100%.
UX-DR10: Componente Cerrado Quality Card — Card de ativo com slider de toque 48x48dp para Nota de Qualidade (1–10), acionador do assistente de autoavaliação com 5 critérios objetivos e badge de Quarentena de Aporte.
UX-DR11: Componente Kerno AI Diagnostic Card — Cartão estruturado para os 4 blocos analíticos (Resumo Executivo, Pontos Fortes, Alertas de Risco/Concentração, Racional do Plano de Aporte), com badges monetários locais determinísticos em R$ e narrativa do LLM em %, botões de feedback útil/não-útil (👍/👎) e indicador de cota diária FinOps.
UX-DR12: Componente B3 Ingestion CRUD Sheet — Bottom Sheet responsivo de nível único para upload assíncrono de planilhas B3 e formulário de cadastro/edição manual de ativos nas 5 Macro Classes em moeda nacional (BRL).
UX-DR13: Componentes de Entrada Monetária e Badges Semânticos — Implementação do widget `InputCurrency` (com tipografia `metric-hero`), `PrivacyToggleButton`, `BadgeGrahamBonus`, `BadgeQuarantine`, `BadgeFounder` e `CvmRegulatoryFooter` afixado nas telas de recomendação e ranking.
UX-DR14: Reestruturação da Navegação e Reestilização de 100% das 18 Rotas Legadas — Migração da navegação móvel para `BottomNavigationBar` de 4 abas (Carteira, GPS, Kerno AI, Diagrama) + 5º slot modular de gastos sob feature flag, sidebar desktop agrupada com `SkipToContentLink`, e reestilização completa de todas as 18 páginas existentes no `GoRouter` para o Design System Intergeracional.
UX-DR15: Proteção de Privacidade Mobile no App Switcher — Máscara automática de privacidade com `FLAG_SECURE` no Android e overlay nativo no iOS sempre que o aplicativo entrar em estado inativo/segundo plano (`AppLifecycleState.inactive`/`paused`).

### FR Coverage Map

FR1: Epic 1 - Identidade de Marca Unificada Kerno em todas as superfícies públicas e autenticadas
FR2: Epic 2 - Design System Intergeracional, acessibilidade visual e suporte a textScaler até 2.0x
FR3: Epic 2 - Preservação e adaptação dos fluxos B3, CRUD multi-classe e multi-CPF com consentimento LGPD
FR4: Epic 3 - Onboarding com Templates de Alocação e metas das Macro Classes (soma estrita 100%)
FR5: Epic 3 - Gestão de Notas de Qualidade (1–10), Teto Máximo de Risco e Quarentena de Aporte
FR6: Epic 4 - Cálculo stateless do GPS de Aporte com Regra de Transbordamento (Spillover)
FR7: Epic 4 - Integração do Multiplicador de Graham (1.30x) no GPS de Aporte
FR8: Epic 4 - Transparência matemática ("Por que estes ativos?") e conformidade regulatória CVM
FR9: Epic 5 - Diagnóstico estruturado da carteira via Kerno AI (Vertex AI Enterprise ZDR)
FR10: Epic 5 - Sanitização LGPD de PII, allowlists de tickers e guardrails regulatórios CVM pós-geração
FR11: Epic 2 - Autenticação Mobile Google OAuth2 + JWT, flutter_secure_storage e biometria nativa
FR12: Epic 2 - Dashboard Patrimonial com Modo Privacidade e proteção de snapshot no App Switcher
FR13: Epic 2 - Navegação modular com feature flags (slot desacoplado para Controle de Gastos)
FR14: Epic 1 - Landing Page Kerno de alta conversão, destaque do Programa Founders e isolamento SEO
FR15: Epic 1 - Concessão atômica do status Membro Founder e camadas comerciais (Free vs. Pro Trial 90d)
FR16: Epic 5 - Expurgo integral LGPD de dados e novas entidades (Direito ao Esquecimento / AccountDeletionIT)
FR17: Epic 4 - Proteção contra anomalias de cotações (Circuit Breaker de mercado e quarentena de evento)

## Epic List

### Epic 1: Rebranding Kerno, Vitrine Pública & Programa Founders
Novos usuários e visitantes conhecem a nova marca Kerno através da Landing Page de alta conversão (`kerno.afsdigital.com.br`), garantem sua vaga exclusiva de Membro Founder (com acesso Pro vitalício gratuito auditado atômica e transacionalmente) e realizam cadastro/login com 1 clique via Google OAuth2 com termos de conformidade e privacidade claros.
**FRs covered:** FR1, FR14, FR15.

### Epic 2: Design System Intergeracional, Experiência Mobile & Dashboard Patrimonial
Investidores acessam seu patrimônio consolidado no celular (iOS/Android) ou no navegador desktop com desbloqueio biométrico nativo (FaceID/Digital), tipografia de alto contraste escalonável até 2.0x sem truncamento, proteção total no dia a dia (Modo Privacidade `R$ ••••••` e Máscara no App Switcher do SO), navegação modular desacoplada e gestão contínua de posições multi-classe e multi-CPF (Holding Familiar).
**FRs covered:** FR2, FR3, FR11, FR12, FR13.

### Epic 3: Alocação Estratégica pelo Diagrama do Cerrado
O investidor estrutura sua estratégia patrimonial de longo prazo sem paralisia de tela em branco a partir de Templates de Alocação pré-configurados (Equilibrado, Moderado, Acumulação), define metas das 5 Macro Classes (Ações BR, FIIs, Renda Fixa / Tesouro, Internacional, Criptoativos) com validação estrita de soma 100%, e avalia a qualidade de cada ativo (Notas 1–10) com proteção automática por Teto Máximo de Risco e Quarentena de Aporte.
**FRs covered:** FR4, FR5.

### Epic 4: GPS de Aporte Inteligente com Bônus de Graham e Resiliência
No dia do aporte mensal, o investidor digita o valor disponível em R$ e recebe em menos de 1 segundo a distribuição matemática simulada exata de onde alocar — priorizando maiores defasagens sem sugerir venda de ativos, com cotas inteiras e transbordamento automático (spillover), bonificação de Graham (1.30x) para ações descontadas, painel de transparência matemática explicativa ("Por que estes ativos?") e proteção contra anomalias de mercado (circuit breaker).
**FRs covered:** FR6, FR7, FR8, FR17.

### Epic 5: Diagnóstico de Carteira Assistido por IA (Kerno AI) & Conformidade LGPD
O investidor obtém auditoria executiva de riscos, concentração e coerência da sua carteira em linguagem natural através do Kerno AI em 4 cartões objetivos, com garantia inegociável de privacidade de dados (zero PII no LLM sob contrato Google Cloud Vertex AI Enterprise ZDR), guardrail regulatório CVM (sem imperativos de compra/venda) e conformidade integral com a LGPD através do expurgo de dados em cascata (Direito ao Esquecimento).
**FRs covered:** FR9, FR10, FR16.

## Epic 1: Rebranding Kerno, Vitrine Pública & Programa Founders

Novos usuários e visitantes conhecem a nova marca Kerno através da Landing Page de alta conversão (`kerno.afsdigital.com.br`), entendem a proposta de valor (Cerrado + Graham + IA), garantem vaga de Membro Founder (com acesso Pro vitalício gratuito auditado de forma atômica em lote de 250 vagas) e realizam cadastro/login com 1 clique via Google OAuth2 com consentimento aos Termos de Uso e Política de Privacidade, sob isolamento SEO estrito.

### Story 1.1: Identidade Visual Institucional Kerno e Migração de Metadados Públicos

As a visitante ou investidor da plataforma,
I want visualizar a nova identidade de marca Kerno em 100% dos textos institucionais, logotipos, favicons e metadados públicos,
So that eu reconheça a marca oficial Kerno com clareza em todas as superfícies e canais de acesso.

**Acceptance Criteria:**

**Given** que um visitante acessa a aplicação web sob o domínio oficial `kerno.afsdigital.com.br`
**When** a página inicial ou qualquer tela pública é renderizada
**Then** o título HTML, favicons, meta tags OpenGraph e textos visíveis exibem *"Kerno — O núcleo inteligente do seu dinheiro"*, sem menção ao nome legado *"Graham Select"* (exceto quando em referência à fórmula de Benjamin Graham)
**And** identificadores internos de infraestrutura (como diretório de chart Helm `helm/graham-select/` e recursos de banco) permanecem intactos para evitar recriação destrutiva no Kubernetes.

### Story 1.2: Infraestrutura de Controle Transacional do Lote de Membros Founders

As a sistema Kerno,
I want gerenciar o lote inicial de vagas Founders com trava atômica transacional no banco de dados e expor um endpoint público de consulta de vagas,
So that exatamente os primeiros usuários cadastrados recebam o plano Pro vitalício gratuito (`FOUNDER_LIFETIME`) sem condições de corrida.

**Acceptance Criteria:**

**Given** uma nova migration incremental do Liquibase (`tb_user` e `tb_founder_quota`)
**When** aplicada no banco de dados
**Then** a tabela de controle de cota é criada com a coluna `founder_slots_remaining` inicializada com o teto configurável (padrão de 250 vagas) e `tb_user` passa a suportar os campos `commercial_plan` e `trial_expires_at`
**And** a requisição `GET /api/v1/founders/slots` retorna HTTP 200 com JSON `{ "totalSlots": 250, "remainingSlots": X, "isLoteActive": true/false }`, registrando log estruturado da entrada sem expor PII
**And** quando múltiplos cadastros ocorrem simultaneamente, a transação decrementa a cota com bloqueio atômico (`UPDATE tb_founder_quota SET founder_slots_remaining = founder_slots_remaining - 1 WHERE founder_slots_remaining > 0`), concedendo `commercial_plan = 'FOUNDER_LIFETIME'` apenas a quem obteve sucesso no decremento, enquanto os cadastros excedentes recebem automaticamente `commercial_plan = 'PRO_TRIAL'` com expiração em 90 dias.

### Story 1.3: Landing Page Pública de Alta Conversão com Destaque do Programa Founders

As a visitante interessado em alocação patrimonial,
I want acessar uma Landing Page rápida e responsiva que explique o método Kerno, mostre o contador de vagas de Membro Founder e isole áreas privadas do SEO,
So that eu compreenda o valor da plataforma em menos de 1 minuto e decida criar minha conta.

**Acceptance Criteria:**

**Given** um visitante acessando a rota pública raiz `/`
**When** a página é servida via HTML estático/SSR leve
**Then** o First Contentful Paint (FCP) conclui em `< 1.2s`, apresentando a proposta de valor, preview visual do GPS de Aporte, Diagrama do Cerrado e Kerno AI, e disclaimer CVM obrigatório no rodapé
**And** enquanto houver vagas disponíveis (`remainingSlots > 0`), a seção do Programa Founders exibe badge de destaque *"Seja um Membro Founder — Acesso Pro Vitalício Gratuito"* com CTA *"Garantir Minha Vaga de Founder Gratuitamente"*
**And** o arquivo `robots.txt` e meta tags garantem `index, follow` exclusivamente para as rotas públicas institucionais (`/`, `/terms`, `/privacy`, `/docs`), aplicando `noindex, nofollow` em todas as rotas autenticadas (`/app/*`, `/dashboard/*`, `/api/*`).

### Story 1.4: Onboarding com Google OAuth2, Aceite de Termos e Concessão de Camada Comercial

As a novo usuário que clicou no CTA da Landing Page ou acessou `/login`,
I want me autenticar via Google Sign-In com 1 clique, confirmando meu aceite aos Termos e Política de Privacidade,
So that minha conta seja criada instantaneamente com meu status de Membro Founder garantido.

**Acceptance Criteria:**

**Given** o card de login na tela `/login` e na Landing Page
**When** o usuário clica em *"Continuar com Google"*
**Then** a interface apresenta links diretos e texto claro informando que a autenticação implica no aceite aos Termos de Uso (`/terms`) e à Política de Privacidade (`/privacy`)
**And** ao validar o token OAuth2 do Google no backend, o usuário cadastrado com vaga Founder disponível recebe `commercial_plan = 'FOUNDER_LIFETIME'`, e a primeira tela autenticada exibe o badge exclusivo `{components.badge-founder}` (*"Founder"*)
**And** usuários cadastrados após o esgotamento do lote Founder recebem `commercial_plan = 'PRO_TRIAL'` com `trial_expires_at = now() + 90 dias` e pill visual informando *"Trial Pro: 90 dias restantes"*.

## Epic 2: Design System Intergeracional, Experiência Mobile & Dashboard Patrimonial

Investidores acessam seu patrimônio consolidado no celular (iOS/Android) ou no navegador desktop com desbloqueio biométrico nativo (FaceID/Digital), tipografia de alto contraste escalonável até 2.0x sem truncamento, proteção total no dia a dia (Modo Privacidade `R$ ••••••` e Máscara no App Switcher do SO), navegação modular desacoplada e gestão contínua de posições multi-classe e multi-CPF (Holding Familiar).

### Story 2.1: Fundação do Design System Intergeracional (Tokens, Tipografia e Acessibilidade)

As a investidor utilizando o Kerno no celular ou na Web,
I want visualizar interfaces com contraste elevado (WCAG AA/AAA), numerais tabulares alinhados e tipografia que respeita a escala de acessibilidade do meu sistema operacional sem cortar palavras,
So that eu leia meus dados financeiros com conforto e precisão, independentemente da minha faixa etária ou iluminação ambiente.

**Acceptance Criteria:**

**Given** os tokens de cores definidos em `DESIGN.md`
**When** o aplicativo é inicializado no tema Light (padrão) ou Dark
**Then** a superfície aplica contraste mínimo de `4.5:1` para textos corridos e `7.0:1` (WCAG AAA) para saldos patrimoniais, com áreas de toque interativas de no mínimo `48x48dp`
**And** quando um dispositivo móvel opera com escalonamento de texto nativo configurado entre `1.0x` e `2.0x` (`MediaQuery.textScalerOf(context)`), nenhum valor monetário, ticker ou percentual sofre truncamento com reticências (`...`) ou sobreposição de linha, expandindo verticalmente com reflow natural
**And** todos os valores numéricos exibidos em colunas e listas utilizam obrigatoriamente a propriedade `fontVariantNumeric: 'tabular-nums'` para alinhamento vertical exato de dígitos.

### Story 2.2: Autenticação Mobile Segura, Biometria Nativa e Cache Offline Criptografado

As a investidor utilizando o aplicativo móvel do Kerno,
I want salvar minha sessão com segurança no cofre de chaves do aparelho e desbloquear o aplicativo via biometria (FaceID ou impressão digital),
So that eu acesse minha carteira em menos de 5 segundos sem digitar senhas e sem expor minhas credenciais em repouso.

**Acceptance Criteria:**

**Given** a autenticação Google OAuth2 no app mobile (iOS/Android)
**When** os tokens de sessão (`access_token`, `refresh_token`) são recebidos
**Then** são persistidos exclusivamente através de `flutter_secure_storage` (iOS Keychain e Android EncryptedSharedPreferences/Keystore), nunca em texto puro
**And** ao abrir o aplicativo ou retornar do background após mais de `2 minutos` de inatividade, o gate biométrico nativo (`local_auth`) é acionado compulsoriamente antes de exibir qualquer dado patrimonial, oferecendo fallback para o PIN/credencial do sistema operacional
**And** em situações sem conexão de rede ou com sinal intermitente, após a biometria aprovada o aplicativo renderiza o último snapshot válido do Dashboard a partir de cache local criptografado, exibindo timestamp discreto da última sincronização
**And** ao acionar logout ou confirmar exclusão de conta, o `flutter_secure_storage` e todos os caches locais do dispositivo são limpos integralmente.

### Story 2.3: Shell de Navegação Multi-Superfície e Fundação Modular com Feature Flags

As a investidor navegando pelo Kerno,
I want uma navegação simples com 4 abas claras no celular e barra lateral agrupada no computador, com arquitetura preparada para novos módulos,
So that eu alterne rapidamente entre Carteira, GPS de Aporte, Kerno AI e Diagrama sem excesso de cliques.

**Acceptance Criteria:**

**Given** a interface Mobile (`largura < 600dp`)
**When** a barra de navegação inferior (`BottomNavigationBar`) é renderizada
**Then** exibe exatamente 4 abas primárias (**Carteira**, **GPS de Aporte**, **Kerno AI**, **Diagrama**) com ícones e rótulos acessíveis de `48dp`, eliminando a sobrecarga de 10 ícones legados da versão anterior
**And** quando a feature flag `enable_expenses_module` está desativada (`false`), o 5º slot modular (Gastos / `features/expenses`) permanece oculto na UI, mantendo a árvore de rotas desacoplada e pronta para ativação no Ciclo 2.1
**And** na interface Desktop Web (`largura >= 1024dp`), a `Sidebar` exibe agrupamentos lógicos de navegação com `SkipToContentLink` para acessibilidade de teclado, com todas as 18 rotas legadas reestilizadas operando sob o novo tema.

### Story 2.4: Dashboard Patrimonial com Modo Privacidade e Proteção de Snapshot em Background

As a investidor consultando meu patrimônio em ambientes públicos,
I want ocultar todos os valores monetários em R$ com um toque e ter a garantia de que a miniatura do app no celular não revela meu saldo,
So that pessoas próximas não vejam meu patrimônio financeiro no metrô ou no seletor de aplicativos.

**Acceptance Criteria:**

**Given** o `DashboardPage` autenticado
**When** renderizado
**Then** apresenta o componente `WealthHeroCard` com patrimônio líquido consolidado e rentabilidade, acompanhado de aviso discreto de delay de 15 min B3 ou fechamento D-1, e o `MacroClassDistributionCard` com barras de progresso duplo para as 5 Macro Classes e Subclasses sem rolagem horizontal no mobile
**And** ao tocar no botão de cabeçalho `PrivacyToggleButton` (ícone de olho), todos os saldos e valores monetários em `R$` são substituídos instantaneamente por `R$ ••••••`, mantendo percentuais (`%`) e gráficos visíveis, com o estado persistido localmente para os próximos acessos
**And** sempre que o aplicativo móvel entrar em estado inativo ou segundo plano (`AppLifecycleState.inactive` ou `AppLifecycleState.paused`), o app ativa automaticamente proteção visual (`FLAG_SECURE` no Android e snapshot mask no iOS), impedindo que saldos ou telas fiquem visíveis no *App Switcher* do SO.

### Story 2.5: Adaptação Visual da Ingestão B3, CRUD Manual Multi-Classe e Holding Familiar Multi-CPF

As a investidor com ativos na B3, títulos de renda fixa, investimentos internacionais e criptoativos na família,
I want importar planilhas da B3 e cadastrar ativos manualmente nas 5 Macro Classes com filtro de Holding Familiar sob o novo design,
So that eu mantenha meu patrimônio completo consolidado sem perder as funcionalidades existentes.

**Acceptance Criteria:**

**Given** o botão *"Atualizar Carteira / + Ativo"* no Dashboard
**When** acionado pelo usuário
**Then** abre o Bottom Sheet responsivo `B3IngestionCrudSheet` de nível único, permitindo upload assíncrono de planilhas B3 (descartando o arquivo bruto com PII imediatamente após o parsing) e cadastro manual de posições nas 5 Macro Classes (*Ações BR*, *FIIs*, *Renda Fixa / Tesouro*, *Internacional* e *Criptoativos*) em `BRL`
**And** ao vincular um CPF secundário ou de dependente menor na visão Holding Familiar, o sistema exige aceite de declaração de representação legal e consentimento conforme LGPD (Art. 14), permitindo alternar a visualização da carteira entre o consolidado familiar e cada CPF individual
**And** 100% dos testes de regressão dos módulos legados de cálculo de posição passam sem quebra de comportamento analítico anterior.

## Epic 3: Alocação Estratégica pelo Diagrama do Cerrado

O investidor estrutura sua estratégia patrimonial de longo prazo sem paralisia de tela em branco a partir de Templates de Alocação pré-configurados (Equilibrado, Moderado, Acumulação), define metas das 5 Macro Classes (Ações BR, FIIs, Renda Fixa / Tesouro, Internacional, Criptoativos) com validação estrita de soma 100%, e avalia a qualidade de cada ativo (Notas 1–10) com proteção automática por Teto Máximo de Risco e Quarentena de Aporte.

### Story 3.1: Modelo de Dados Relacional e Migrations para o Diagrama do Cerrado

As a sistema Kerno,
I want persistir as preferências de alocação de Macro Classes e as Notas de Qualidade de cada ativo em tabelas relacionais dedicadas,
So that as configurações estratégicas de cada usuário sejam salvas com isolamento estrito e precisão decimal.

**Acceptance Criteria:**

**Given** uma nova migration incremental do Liquibase
**When** executada no banco de dados
**Then** cria as tabelas `tb_cerrado_preferences` (com colunas `user_id`, metas percentuais das 5 Macro Classes em `NUMERIC(5,2)`, multiplicador de Graham e flags) e `tb_cerrado_asset_grade` (com `user_id`, `ticker`, `macro_class`, `sub_class`, `quality_grade` de 1 a 10, `custom_risk_ceiling` e `quarantine_corporate_event`) com índices por `user_id` e integridade referencial
**And** qualquer gravação de percentuais de metas ou tetos utiliza obrigatoriamente colunas `NUMERIC(19,4)` ou `NUMERIC(5,2)`, sendo proibido o uso de tipos de ponto flutuante (`float`/`double`).

### Story 3.2: Motor de Domínio da Hierarquia do Cerrado, Aritmética de Metas e Redistribuição de Teto

As a investidor que definiu suas metas e notas de qualidade,
I want que o motor de cálculo determine a meta percentual proporcional de cada ativo na carteira aplicando o Teto Máximo de Risco e a Quarentena,
So that nenhum ativo ultrapasse o limite prudencial de risco sem deixar resíduos de alocação na classe.

**Acceptance Criteria:**

**Given** as metas das 5 Macro Classes informadas pelo usuário
**When** a soma percentual for diferente de `100.00%` (ex.: `95%` ou `105%`)
**Then** o serviço de domínio rejeita a operação com erro de validação (HTTP `422 Unprocessable Entity`), impedindo inconsistências na carteira
**And** a meta bruta de cada ativo $i$ na Macro Classe $C$ é calculada como $\left(\frac{\text{Nota}_i}{\sum \text{Notas elegíveis de } C}\right) \times \text{Meta}_C\%$ utilizando estritamente `BigDecimal` com escala 4 e arredondamento `HALF_EVEN`
**And** para ativos de risco emissor único com nota entre 8–10 (teto 10%) ou 5–7 (teto 6%), caso sua meta bruta exceda o teto permitido, o teto é aplicado e o excedente é redistribuído iterativamente de forma proporcional à nota entre os demais ativos elegíveis não-saturados da mesma Macro Classe até que a soma das metas iguale $\text{Meta}_C\%$
**And** títulos públicos soberanos (*Tesouro Direto*) e ETFs de índices amplos (`is_diversified_broad_etf = true`, ex.: `IVVB11`, `WRLD11`, `BOVA11`) são isentos da trava de emissor único, limitando-se apenas à meta total da própria Macro Classe
**And** qualquer ativo com Nota de Qualidade entre 1 e 4 recebe compulsoriamente status de **Quarentena de Aporte** (teto de novos aportes = `0%`) e sua nota é excluída do denominador da classe, mantendo seu valor patrimonial existente intacto.

### Story 3.3: API REST para Gestão de Metas de Macro Classes e Templates de Onboarding

As a investidor configurando o Diagrama do Cerrado pelo Web ou Mobile,
I want consultar os templates pré-configurados e salvar minhas metas de alocação através de endpoints seguros,
So that minha estratégia esteja disponível imediatamente em qualquer dispositivo.

**Acceptance Criteria:**

**Given** o endpoint `GET /api/v1/cerrado/templates`
**When** consultado
**Then** retorna os 3 perfis oficiais (*Modelo Equilibrado Cerrado* com 1% Cripto, *Modelo Moderado Patrimonial* com 0% Cripto e 45% Renda Fixa, *Modelo Acumulação Arrojada* com 5% Cripto) contendo as metas de Macro Classes e a nota neutra padrão `6` para os ativos, acompanhados de disclaimer CVM
**And** ao submeter `PUT /api/v1/cerrado/preferences` com as metas das 5 Macro Classes somando exatamente 100.00%, persiste a configuração associada ao `user_id` autenticado, registrando log estruturado da operação e invalidando o cache de IA existente (`portfolio_state_hash`)
**And** 100% dos testes da suíte de integração `CerradoPreferencesIT` passam via Maven Failsafe, validando cenários de persistência, validação de soma 100% e isolamento por `user_id`.

### Story 3.4: Interface do Diagrama do Cerrado: Seletor de Templates e Trava de Metas

As a investidor acessando a Aba 4 (**Diagrama**),
I want escolher um template de alocação com 1 clique ou ajustar as metas das 5 Macro Classes com feedback visual em tempo real da soma de 100%,
So that eu configure minha estratégia sem erros matemáticos.

**Acceptance Criteria:**

**Given** a tela `/allocation-strategy` (Aba 4 Diagrama)
**When** o usuário é recém-cadastrado
**Then** apresenta os 3 cartões `CerradoTemplateSelectorCard` sem pré-seleção automática e com nota neutra inicial `6`, exibindo disclaimer regulatório de ausência de suitability
**And** ao editar manualmente os percentuais das 5 Macro Classes (*Ações BR*, *FIIs*, *Renda Fixa / Tesouro*, *Internacional*, *Criptoativos*), se a soma for diferente de `100.00%` (ex.: `98,0%`), a interface exibe badge carmesim informando a diferença exata faltante (*"Faltam 2,00% para completar 100%"*) e desabilita visualmente o botão salvar
**And** quando a soma atinge exatamente `100.00%`, o botão salvar é habilitado com destaque esmeralda e confirmação de gravação.

### Story 3.5: Gestão Visual de Notas de Qualidade, Slider 48dp, Assistente de 5 Critérios e Quarentena

As a investidor avaliando os ativos da minha carteira,
I want ajustar a Nota de Qualidade (1–10) de cada ativo via slider direto de toque ou através de um assistente objetivo de 5 critérios,
So that o sistema defina os pesos relativos e coloque em quarentena ativos que perderam qualidade fundamentalista.

**Acceptance Criteria:**

**Given** o cartão de ativo `CerradoQualityCard`
**When** renderizado
**Then** apresenta um slider interativo com polegar de toque de `48x48dp` permitindo selecionar notas inteiras de `1 a 10`, exibindo instantaneamente o Teto Máximo de Risco correspondente
**And** ao acionar o botão *"Assistente de Autoavaliação"*, abre modal com os 5 critérios objetivos (*Governança*, *Lucro 5 anos*, *Endividamento*, *Vantagem Competitiva*, *Histórico de Proventos*) com pontuação de `0`, `1` ou `2` cada, somando automaticamente e atribuindo a nota resultante (1–10) sem auditoria da plataforma
**And** ao atribuir uma nota entre `1 e 4`, o cartão exibe o badge visual `{components.badge-quarantine}` (*"Em Quarentena de Aporte — Novos Aportes = 0%"*), com trilha do slider destacada em âmbar
**And** ao salvar as notas via `PUT /api/v1/cerrado/grades`, as notas são persistidas no backend e recalculam as metas efetivas de todos os ativos da carteira.

## Epic 4: GPS de Aporte Inteligente com Bônus de Graham e Resiliência

No dia do aporte mensal, o investidor digita o valor disponível em R$ e recebe em menos de 1 segundo a distribuição matemática simulada exata de onde alocar — priorizando maiores defasagens sem sugerir venda de ativos, com cotas inteiras e transbordamento automático (spillover), bonificação de Graham (1.30x) para ações descontadas, painel de transparência matemática explicativa ("Por que estes ativos?") e proteção contra anomalias de mercado (circuit breaker).

### Story 4.1: Motor Stateless do GPS de Aporte com Regra de Transbordamento (Spillover)

As a investidor informando meu capital disponível no mês,
I want que o sistema calcule a distribuição ideal de novos aportes priorizando as maiores defasagens sem sugerir a venda de nenhuma posição existente,
So that minha carteira reequilibre seu risco progressivamente sem giro patrimonial nem geração de impostos.

**Acceptance Criteria:**

**Given** um valor de aporte disponível $X > 0$, o snapshot da custódia e as preferências do Diagrama do Cerrado
**When** o motor `ContributionGpsSimulator` executa o cálculo em memória
**Then** a operação é estritamente *stateless* (sem persistência em banco), calculando a defasagem relativa de cada classe e ativo contra o patrimônio projetado ($P_{\text{atual}} + X$)
**And** quando múltiplos ativos apresentam o mesmo score de defasagem, aplica o desempate determinístico rigoroso: `1º Maior Nota (1–10) -> 2º Maior Margem de Segurança de Graham -> 3º Maior déficit monetário absoluto -> 4º Ordem alfabética do Ticker (A-Z)`
**And** para ativos cotados em bolsa (*Ações BR*, *FIIs*, *ETFs*, *BDRs*), quantiza a quantidade inteira de cotas ($\lfloor \text{Valor} / \text{Cotação} \rfloor$) *in-loop* e deduz apenas o valor consumido pelas cotas inteiras em `BigDecimal`, fazendo qualquer saldo residual transbordar (*spillover*) imediatamente para o próximo ativo ou Macro Classe elegível
**And** caso todos os ativos elegíveis atinjam seus respectivos tetos de risco ou restem apenas classes sem ativos cadastrados, encerra com saldo não alocado categorizado explicitamente em: `unallocatedRiskProtectedBalance`, `emptyClassUnallocatedBalance` e `residualChangeBalance`.

### Story 4.2: Integração da Margem de Segurança de Graham e Multiplicador de Prioridade

As a investidor que segue a filosofia de Benjamin Graham na classe Ações BR,
I want que ações com margem de segurança positiva recebam bônus na prioridade de aporte dentro da classe,
So that eu aproveite descontos fundamentalistas do mercado sem ultrapassar o teto prudencial de risco.

**Acceptance Criteria:**

**Given** uma ação brasileira elegível no Diagrama (Nota $\ge 5$ e déficit $> 0$) com $LPA > 0$ e $VPA > 0$
**When** sua cotação atual for inferior ao Valor Intrínseco de Graham ($VI = \sqrt{22.5 \times LPA \times VPA}$)
**Then** o sistema marca o papel com a flag de Margem de Segurança positiva e aplica o **Multiplicador de Graham** parametrizável (padrão `1.30x`) sobre o score de defasagem da ação
**And** o bônus altera a ordem de priorização dentro da classe *Ações BR*, mas a alocação máxima permanece estritamente limitada pelo Teto Máximo de Risco da Nota de Qualidade da ação
**And** para ações com $LPA \le 0$ ou $VPA \le 0$, o cálculo de Graham é registrado explicitamente como `"N/A (LPA/VPA <= 0)"`, mantendo a priorização padrão baseada no Diagrama sem bônus multiplicador.

### Story 4.3: Circuit Breaker de Cotações de Mercado e Quarentena de Eventos Corporativos

As a sistema de inteligência de investimentos Kerno,
I want identificar oscilações extremas e anomalias de preços em ativos de renda variável,
So that o investidor não receba simulações de aporte distorcidas por eventos corporativos pendentes de ajuste pela B3.

**Acceptance Criteria:**

**Given** uma variação diária de cotação não ajustada `> 40%` em renda variável (excluindo Criptoativos)
**When** detectada pelo ingestor de cotações de mercado
**Then** o ticker é colocado no estado persistente `QUARENTENA_EVENTO_CORPORATIVO`, congela a última cotação válida pré-anomalia para fins patrimoniais e suspende novas simulações de aporte para aquele ativo no GPS
**And** ao acessar o detalhe do ativo ou alerta na interface e acionar *"Confirmar posição atual / Retomar cotação"*, a API `POST /api/v1/market/tickers/{ticker}/override-quarantine` encerra o estado de quarentena e reabilita o ativo para simulação de aporte.

### Story 4.4: API REST do GPS de Aporte com Explicabilidade Matemática e Auditoria CVM

As a investidor submetendo uma simulação pelo celular ou Web,
I want receber a distribuição de aportes em menos de 1 segundo acompanhada de todos os números que justificam a simulação,
So that eu compreenda a matemática por trás de cada centavo sugerido sob conformidade CVM.

**Acceptance Criteria:**

**Given** o endpoint `POST /api/v1/gps/simulate`
**When** chamado com o valor de aporte disponível (`R$ X`)
**Then** o cálculo completo responde em `< 1.0 segundo` (P95) para carteiras de até 100 ativos, conforme validado na suíte de testes de integração `ContributionGpsIT`
**And** a resposta JSON contém para cada ativo: `% Atual Pré-Aporte`, `% Meta Efetiva`, `% Projetada Pós-Aporte`, `Quantidade de Cotas Inteiras`, `Valor Alocado em R$`, `Nota de Qualidade`, `Teto de Risco`, métricas de Graham (`LPA`, `VPA`, `VI`, `% Margem`), saldo não alocado protegido e texto de disclaimer CVM informando que se trata de simulação matemática automatizada
**And** para usuários na camada Free pós-trial (`FR-15`), a resposta agrega os valores exclusivamente no nível de Macro Classes, com indicador visual de upgrade para detalhamento por ativo.

### Story 4.5: Interface do GPS de Aporte: Calculadora, Cotas Inteiras e Acordeão de Transparência

As a investidor na Aba 2 (**GPS de Aporte**),
I want digitar o valor em R$ do meu aporte mensal e ver os cartões limpos com as quantidades de ações a comprar e a justificativa matemática detalhada,
So that eu execute minhas ordens no app da corretora em menos de 30 segundos.

**Acceptance Criteria:**

**Given** a tela `/gps-aporte` (Aba 2)
**When** o investidor digita o valor disponível no componente `InputCurrency` e clica em *"Calcular Aporte"*
**Then** a tela renderiza os cartões `ContributionGpsCard` em lista empilhada destacando o ticker, quantidade inteira de ações/cotas e valor em R$, exibindo o selo `{components.badge-graham-bonus}` (*"Margem Graham Positiva"*) nos papéis bonificados
**And** o acordeão `MathTransparencyAccordion` (*"Por que estes ativos?"*), quando expandido pelo usuário, exibe a comparação percentual pré vs. pós-aporte, a meta do Diagrama e os múltiplos fundamentalistas de Graham de forma transparente e em numerais tabulares
**And** em situações em que reste saldo protegido por teto de risco, a interface renderiza o `RiskCeilingSaturationCard` em âmbar explicando o motivo pelo qual aquele saldo não foi distribuído
**And** o rodapé da tela exibe permanentemente o `CvmRegulatoryFooter` informando a natureza estritamente matemática e não-recomendatória da ferramenta.

## Epic 5: Diagnóstico de Carteira Assistido por IA (Kerno AI) & Conformidade LGPD

O investidor obtém auditoria executiva de riscos, concentração e coerência da sua carteira em linguagem natural através do Kerno AI em 4 cartões objetivos, com garantia inegociável de privacidade de dados (zero PII no LLM sob contrato Google Cloud Vertex AI Enterprise ZDR), guardrail regulatório CVM (sem imperativos de compra/venda) e conformidade integral com a LGPD através do expurgo de dados em cascata (Direito ao Esquecimento).

### Story 5.1: Modelo de Dados de Cache Analítico, Controle FinOps e Migrations de IA

As a sistema Kerno,
I want armazenar diagnósticos gerados por IA vinculados ao hash de estado da carteira e controlar o consumo de chamadas por usuário,
So that leituras repetidas sejam instantâneas (< 400ms) e os custos de inferência de LLM permaneçam blindados contra abusos (FinOps).

**Acceptance Criteria:**

**Given** uma nova migration incremental do Liquibase
**When** executada no banco de dados
**Then** cria as tabelas `tb_ai_diagnostic_cache` (com `portfolio_state_hash` SHA-256, `model_version`, `prompt_version`, `json_payload` e `expires_at` com TTL de 24 horas) e `tb_ai_usage_counter` (com contadores de chamadas diárias e mensais por `user_id`)
**And** ao solicitar diagnóstico para uma carteira cujo `portfolio_state_hash` já exista em cache válido, a resposta é retornada em `< 400ms` (P95) sem chamar o provedor externo de LLM e sem debitar a cota de uso do usuário
**And** quando um usuário atinge o limite de 5 gerações reais diárias no plano Pro/Founder e tenta uma 6ª análise sem mudança material de carteira, a API retorna HTTP 429 com mensagem orientativa de uso justo, servindo o último diagnóstico cacheado.

### Story 5.2: Sanitizador LGPD de PII, Normalização Canônica e Validador Pré-Payload

As a encarregado de privacidade (DPO) e investidor do Kerno,
I want garantir que nenhum dado pessoal identificável ou valor monetário absoluto em R$ seja enviado ao modelo externo de linguagem,
So that a integração de IA respeite integralmente os Artigos 9º, 18 e 33 da LGPD.

**Acceptance Criteria:**

**Given** o snapshot determinístico da carteira gerado pelo backend
**When** processado pela camada `PiiSanitizer`
**Then** 100% dos dados pessoais (nome, CPF, e-mail, telefone, IDs nominais de conta) e valores monetários absolutos em `R$` são removidos, sendo enviado ao LLM exatamente `0 bytes` de PII
**And** para ativos cadastrados manualmente pelo usuário com descrições livres, o sanitizador valida contra as allowlists oficiais (tickers B3 e ativos internacionais), substituindo qualquer texto livre fora das allowlists por rótulos anônimos canônicos (`RENDA_FIXA_MANUAL_1`, `CRIPTO_MANUAL_1`)
**And** 100% dos testes da suíte automatizada do sanitizador confirmam a eliminação de dados sensíveis antes de qualquer saída de rede.

### Story 5.3: Adaptador Vertex AI Gemini Enterprise ZDR e Validação Determinística Pós-Geração

As a sistema de inteligência financeira Kerno,
I want gerar a síntese qualitativa da carteira via Google Cloud Vertex AI com cláusula de Zero Data Retention, validando a integridade numérica e os guardrails da CVM antes de exibir o resultado,
So that o investidor receba uma análise executiva clara com zero alucinação aritmética e total conformidade regulatória.

**Acceptance Criteria:**

**Given** a porta agnóstica de domínio `AiPortfolioAnalyzerPort` implementada pelo `VertexAiGeminiAdapter`
**When** acionada para inferência
**Then** conecta-se exclusivamente ao endpoint empresarial do Google Cloud Vertex AI sob contrato corporativo com Zero Data Retention (ZDR) utilizando o modelo fixo `gemini-2.5-flash` com temperatura `0.1`
**And** o retorno JSON estruturado em 4 blocos (*Resumo Executivo*, *Pontos Fortes*, *Alertas de Risco e Concentração*, *Racional do Plano de Aporte*) é validado deterministicamente garantindo que zero tickers ou números foram inventados (tolerância máxima de arredondamento de ±0,1 p.p. contra o snapshot de entrada)
**And** o validador léxico regulatório CVM 19/20 bloqueia qualquer sentença que contenha termos imperativos de compra ou venda (`\b(comprar?|vender?|lucro garantido)\b`), abortando imediatamente o uso da resposta do LLM sem retry síncrono
**And** em caso de falha de validação, erro do provedor ou timeout `> 15 segundos`, o sistema ativa imediatamente o fallback quantitativo determinístico local, registrando log de auditoria `WARN/ERROR` com stack trace e sem debitar a cota diária do usuário.

### Story 5.4: Interface do Kerno AI: Cartões de Diagnóstico, Feedback 👍/👎 e Fallback Gracioso

As a investidor na Aba 3 (**Kerno AI**),
I want visualizar meu diagnóstico estruturado em 4 cartões legíveis combinando valores em R$ locais com a síntese em %, avaliar a utilidade do relatório e ter acesso instantâneo a dados mesmo em caso de instabilidade da IA,
So that eu identifique concentrações de risco e pontos de melhoria na minha carteira com total clareza.

**Acceptance Criteria:**

**Given** a tela `/kerno-ai` (Aba 3)
**When** o diagnóstico é renderizado
**Then** exibe os 4 cartões `KernoAiDiagnosticCard` (*Resumo Executivo*, *Pontos Fortes*, *Alertas de Risco e Concentração*, *Racional do Plano de Aporte*), combinando badges locais determinísticos em R$ com a narrativa percentual gerada pela IA, respeitando o Modo Privacidade
**And** na primeira utilização do Kerno AI pelo usuário, exibe aviso transparente informando que a estrutura percentual anonimizada é processada por IA corporativa sob contrato de proteção e ZDR (Art. 9º LGPD)
**And** ao clicar nos botões de avaliação útil/não-útil (👍/👎), registra a métrica de satisfação do usuário (`SM-5`) sem exigir comentários obrigatórios
**And** em situações de timeout da IA externa ou em contas na camada Free pós-trial, renderiza o painel determinístico local com mensagem informativa não-bloqueante, garantindo continuidade de uso.

### Story 5.5: Expurgo em Cascata LGPD (Direito ao Esquecimento) e Teste de Integração AccountDeletionIT

As a titular de dados pessoais utilizando o Kerno,
I want solicitar a exclusão definitiva da minha conta (ou a remoção individual de um CPF secundário),
So that todos os meus dados cadastrais, posições, metas do Diagrama do Cerrado e diagnósticos de IA sejam expurgados de forma irreversível conforme a LGPD.

**Acceptance Criteria:**

**Given** a confirmação de exclusão definitiva de conta no Hub de Perfil (`/profile`)
**When** o `UserDataPurgeAdapter` é acionado no backend
**Then** cancela imediatamente quaisquer jobs assíncronos Kafka em processamento do usuário e executa hard-delete atômico em cascata de todas as tabelas: `tb_user`, `tb_user_cpf`, posições de custódia, `tb_cerrado_preferences`, `tb_cerrado_asset_grade`, `tb_ai_diagnostic_cache` e `tb_ai_usage_counter`, além de limpar chaves de rate limit e caches em memória
**And** ao submeter a remoção granular de um CPF secundário da Holding Familiar pelo endpoint dedicado, remove atômica e exclusivamente as posições e o vínculo do respectivo CPF, preservando intactos o titular e os demais membros
**And** 100% dos testes da suíte `AccountDeletionIT` passam via Maven Failsafe, validando a exclusão completa, o cancelamento de concorrência com workers assíncronos e a ausência absoluta de registros órfãos em todas as tabelas do Ciclo 2.0.
