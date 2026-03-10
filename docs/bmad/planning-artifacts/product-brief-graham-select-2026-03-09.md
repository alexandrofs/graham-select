---
stepsCompleted: [1, 2, 3, 4, 5]
inputDocuments:
  - "docs/bmad/project-context.md"
  - "docs/visao-produto.md"
  - "docs/bmad/brainstorming/brainstorming-session-2026-03-08-21-48-37.md"
  - "docs/landing-page-proposta.md"
  - "docs/bmad/planning-artifacts/research/market-aplicativos_de_gestao_de_carteira_de_investimentos-research-2026-03-09.md"
  - "docs/bmad/planning-artifacts/research/domain-gestao_carteiras_investimentos-research-2026-03-09.md"
  - "docs/bmad/planning-artifacts/research/technical-extracao_dados_b3-research-2026-03-09.md"
  - "docs/deploy.md"
date: 2026-03-09
author: Alex
---

# Product Brief: graham-select

<!-- Content will be appended sequentially through collaborative workflow steps -->

## Executive Summary

O Graham Select é uma plataforma de gestão de carteiras de investimentos focada em empoderar tanto investidores iniciantes quanto experientes a tomarem decisões de aporte rápidas, assertivas e fundamentadas. O aplicativo consolida o histórico de ativos de múltiplas corretoras com altíssima velocidade e acurácia computacional, removendo a dolorosa fricção da atualização manual de planilhas. Combinado à metodologia comprovada de Benjamin Graham para classificar e exibir as ações mais descontadas do mercado, o Graham Select transforma a experiência do usuário: elimina o "efeito manada", dá clareza total ao portfólio em tempo real e faz com que o investidor tome o verdadeiro gosto por construir patrimônio.

---

## Core Vision

### Problem Statement

Investidores que possuem ativos pulverizados enfrentam extrema dificuldade em ter uma visão consolidada, precisa e instantânea de suas carteiras, além da dor de registrar eventos corporativos (como dividendos e desdobramentos) manualmente. A dependência de planilhas torna o acompanhamento exaustivo e lento, enquanto a incapacidade de visualizar a distribuição do patrimônio gera paralisia ou decisões de aporte puramente emocionais.

### Problem Impact

Sem a visão total de risco e retorno aliada à falta de agilidade, os investidores acabam realizando aportes mal alocados, seguindo notícias passageiras ("efeito manada") ao invés de análises de valor. Na prática, quem tem muitos ativos perde dinheiro e tempo com a lentidão das ferramentas, e os iniciantes se frustram com a complexidade, perdendo o apreço por continuar investindo. 

### Why Existing Solutions Fall Short

Soluções de mercado atualmente estabelecidas, como o Kinvo, são limitadas por lentidão no carregamento e falta de atualização das cotações e eventos automáticos em tempo real – minando a confiança na hora da tomada de decisão. Na ponta oposta, as planilhas customizadas resolvem a exatidão matemática, mas demandam um esforço repetitivo intolerável. O mercado carece de uma solução que una a velocidade instantânea a um acompanhamento de valor prático.

### Proposed Solution

Um aplicativo ultrarrápido, seguro e fácil de usar, construído sob uma Arquitetura Orientada a Eventos (Kafka) que separa dados de mercado compartilhados (eventos corporativos) dos dados privados. O Graham Select recebe o arquivo da bolsa B3 ou dados Open Finance e centraliza instantaneamente tudo, calculando ganhos, perdas e proventos sem esforço humano. A cereja do bolo é a aplicação da teoria rigorosa de Graham, expondo as "20 empresas mais baratas da bolsa" e sugerindo aportes com embasamento técnico em tempo real, com visualizações fluidas de progresso.

### Key Differentiators

- **Zero Atrito ("B3 Time Machine" e Proventos Automáticos):** O usuário não precisa lançar notas ou dividendos pendentes manualmente. O sistema consolida o histórico B3 e cruza com uma base de eventos de mercado pré-armazenada, entregando o "Retorno Total" real sem fricção.
- **Percepção Instantânea e Confiabilidade de Aportes:** Atualizações verdadeiramente assíncronas em que a velocidade mecânica (backend otimizado) e a velocidade percebida (coreografia de UI) garantem uma ferramenta viva para decisão rápida.
- **Blindagem Contra Efeito Manada:** O método de Graham atua como um *guardrail* psicológico embutido, focando as decisões em matemática fria e fundamentos sempre que o investidor ameaçar seguir tendências de mercado.

---

## Target Users

### Primary Users

**Investidor Familiar Híbrido (O "CFO" da Casa)**
- **Contexto:** É o tomador de decisão financeira da família. Tem contas em diferentes corretoras (a sua, a da esposa/filhos) para não misturar dinheiros ou aproveitar isenções, mas a visão final é sempre fragmentada.
- **Dor Atual:** Gasta horas em planilhas tentando casar o patrimônio da família para entender onde estão super-alocados. Sentem ansiedade por não saberem a rentabilidade "real consolidada" do núcleo familiar.
- **Visão de Sucesso:** Arrastar os relatórios B3 da família inteira e o aplicativo criar "Tags/Visões Virtuais" (ex: "Viagem", "Aposentadoria") em segundos, descolando os ativos empilhados de sua conta de origem corretora original. O "Aha moment" (momento Uau) ocorre ao ver a pizza da família equilibrada e atualizada.

**Investidor Guiado por Fundamentos (O Iniciante Ambicioso)**
- **Contexto:** Começou agora e quer formar patrimônio. Sabe que deve comprar empresas descontadas ("Value Investing"), mas se perde nos múltiplos complexos e cai no "efeito manada" dos fóruns e influencers.
- **Dor Atual:** Não sabe *o que* aportar na segunda-feira sem passar horas lendo relatórios.
- **Visão de Sucesso:** Entrar no app e ver de maneira clara o ranking das "20 empresas mais baratas" sustentado pela IA, que traduz a teoria de Graham para o português do dia a dia, validando suas escolhas.

### Secondary Users

**O "Stakeholder Passivo" Familiar (ex: Cônjuges/Dependentes)**
- **Contexto:** Não opera o home broker, mas tem os recursos na reta e ansiedade ao ver notícias ruins na TV. 
- **Necessidade:** Poder visualizar de forma simples (ex: "Modo Calmaria" / resumos via WhatsApp) que o dinheiro da família, administrado pelo parceiro(a), está seguro e rendendo. Confia na "IA" como um selo de auditoria e validação neutra das decisões tomadas.

### User Journey

1. **Discovery & Onboarding (O Zero Atrito):** O usuário descobre o Graham Select frustrado com outra planilha quebrada. Em vez de adicionar ativos à mão, o sistema pede apenas que ele faça upload de um `.xlsx` da B3 dele e da esposa. Em menos de 5 segundos, *toda sua história financeira de anos ganha vida gráfica viva*. (A "B3 Time Machine").
2. **Core Usage (Rotina Semanal):** Semanalmente, ele não tem mais o trabalho de buscar informações de mercado. Ele entra para checar a saúde do patrimônio (agora categorizado por *Tags* e Células Familiares) e checar os "sugestores de aporte".
3. **Success Moment (A Decisão Guiada):** Com o dinheiro em caixa na conta da corretora, o usuário abre o Graham Select. A IA analisa sua carteira da família, indica onde há brecha de rebalanceamento, e a cruza com a lista das 20 Ações de Graham hoje: *"Recomendo injetar esses fundos em ABC4 para tapar o risco em Utilidade Pública."*
4. **Long-term Value:** O sistema se torna o "Cérebro Insubstituível", eliminando o medo no Bear Market e blindando o "CFO da casa" da fadiga de decisões.

---

## Success Metrics

O sucesso do Graham Select é focado primordialmente em Engajamento Ativo e Confiança Fiduciária. Diferente de plataformas em que "ficar horas lendo relatórios" é uma métrica de engajamento, nosso sucesso significa que o usuário toma uma decisão inteligente no menor tempo de sessão possível. O valor está no *insight*, não no tempo retido por tela.

### Business Objectives

- **Adoção e Aquisição:** Conquistar e solidificar uma base inicial forte de investidores via integração de planilhas B3 (Zero Atrito) e organicamente escalar os acessos móveis quando o aplicativo for publicado.
- **Validação de "Advisory" Sistêmico:** Provar que a inteligência artificial do sistema em parceria com o modelo do Benjamin Graham gera confiança suficiente para sobrepujar a intuição (ou efeito manada) do usuário.

### Key Performance Indicators (KPIs)

- **Taxa de Aceite de Aportes (Conversion to Action):** Qual a % de vezes que o sistema indica as "20 empresas mais baratas" e o usuário reporta que aceitou a sugestão ou efetivamente realizou aquele aporte sugerido. (Essa é a North Star Metric).
- **Tempo de Uso Efetivo vs. Atrito:** Monitorar a agilidade – um baixo tempo de uso por sessão (sinalizando atrito quase nulo, o usuário entra, pega a informação em segundos e sai para executar na corretora) mas aliado à estabilidade assíncrona.
- **Frequência de Iterações (Stickiness):** Volume de interações semanais (DAU/MAU – Daily/Monthly Active Users), focando em trazer o usuário ao menos 1x por semana antes do seu ciclo natural de aportes (geralmente sexta ou fim do mês).
- **Métricas de Store (Crescimento Futuro):** Ao lançar as versões Mobile (iOS/Android), perseguir Nota média do App > 4.6 (indicando acurácia nos bugs e UI satisfatória) e curva de Downloads acelerada baseada em retenção.

---

## MVP Scope

### Core Features (V1)
O Minimum Viable Product (MVP) focará estritamente na resolução do "Atrito de Inserção de Dados" e na "Consolidação de Visualização":
- **Carregamento 'B3 Time Machine':** Importação via upload de arquivo `.xlsx`/`.csv` extraído da área do investidor da B3.
- **Parser Resiliente:** Processamento assíncrono dos dados em infraestrutura robusta (Kafka/Spring Boot) capaz de mapear compras e consolidar a posição média dos ativos.
- **Painel de Consolidação:** A "Mesa de Comando" Web/Mobile que unifica todo o histórico importado em uma visualização "limpa", somando diferentes papéis por contas/CPFs.
- **CRUD e Conciliação Manual:** Interface de ajustes na qual o usuário pode adicionar, modificar ou remover (excluir um ativo) transações pontuais para cobrir lacunas em caso de erros nos dados da B3 ou operações descentralizadas.

### Out of Scope for MVP (V2+)
Funcionalidades que foram intencionalmente adiadas para garantir lançamento rápido:
- Open Finance nativo via APIs de corretoras.
- Motor de IA ativo tipo "Copiloto Financeiro" com processamento de linguagem natural no WhatsApp.
- Base central de market data cobrindo todos os eventos corporativos retroativos globais (dividendos, desdobramentos de mercado não registrados na nota da B3).
- Importação via Leitura Computacional de tela (OCR de aplicativo de banco).

### MVP Success Criteria
Saberemos que o MVP atingiu o sucesso (Product-Market Fit inicial) se:
- O usuário for capaz de inserir 5 anos de transações subindo apenas o arquivo da B3, sem incidentes pesados no onboarding.
- Houver recorrência de abertura do app semanal para consulta passiva (verificação do portfólio consolidado).
- Os inputs manuais do usuário em cima de uma base gerada pela B3 caírem para menos de 5% de correção do total importado.

### Future Vision
A longo prazo (2 a 3 anos), o Graham Select evoluirá de um visualizador ágil B3 para um "Cérebro Insubstituível do Patrimônio Familiar". Terá integração passiva e direta via Open Finance com algoritmos de ML aplicando o filtro de Graham diário não apenas para alertar as mais baratas, mas enviando alertas no momento exato em que uma tese fundamentalista num dado ativo corra perigo. Ele será o conselheiro que evita a sabotagem comportamental.
