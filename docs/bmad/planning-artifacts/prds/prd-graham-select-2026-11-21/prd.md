---
title: "Kerno (ex-Graham Select 2.0)"
status: final
created: 2026-11-21
updated: 2026-11-21
author: "Alex"
---

# PRD: Kerno
*O núcleo inteligente do seu dinheiro — Rebranding, Design System Intergeracional, Diagrama do Cerrado + Graham, Diagnóstico de Carteira com IA e MVP Mobile.*

## 0. Document Purpose

Este Documento de Requisitos de Produto (PRD) define o escopo funcional, os requisitos não-funcionais, as regras de conformidade (CVM 19/20 e LGPD) e os critérios de sucesso comercial do **Kerno** (evolução e rebranding oficial do *Graham Select* em seu Ciclo 2.0). Ele se destina à liderança de produto (PM), engenharia (Backend, Frontend/Mobile Flutter e IA) e responsáveis pelos fluxos subsequentes de **Especificação de UX**, **Arquitetura** e **Quebra de Épicos e Histórias**. O vocabulário do documento é ancorado de forma estrita na seção **§3. Glossary**, as funcionalidades agrupam Requisitos Funcionais numerados globalmente (`FR-1` a `FR-17`) com rastreabilidade direta para Jornadas do Usuário (`UJ-1` a `UJ-5`) e Métricas de Sucesso (`SM-1` a `SM-6`, acompanhadas das contra-métricas `SM-C1` a `SM-C3`), e todas as decisões e premissas confirmadas estão consolidadas no índice **§12. Confirmed Decisions & Assumptions Index**. Este PRD constrói sobre a fundação *brownfield* já existente da plataforma (ingestão assíncrona de planilhas da B3, motor fundamentalista de Benjamin Graham e consolidação multi-CPF documentados no [prd.md legado](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/prd.md)) e formaliza a visão do [Product Brief do Kerno](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/briefs/brief-graham-select-2026-11-21/brief.md). Especificações matemáticas detalhadas, arquitetura de prompt do LLM, matrizes de decisão de marca e contratos técnicos de implementação residem no [addendum.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/prds/prd-graham-select-2026-11-21/addendum.md).

---

## 1. Vision

O **Kerno** nasce da maturidade do antigo *Graham Select*. Enquanto a primeira geração resolveu a dor de consolidar o histórico caótico da B3 e filtrar ações descontadas pela fórmula clássica de Benjamin Graham, o investidor de longo prazo continuava enfrentando três barreiras críticas no dia do aporte mensal: a **paralisia de alocação** entre diferentes classes de ativos sem precisar vender posições vencedoras (evitando giro patrimonial e geração de DARF), o **atrito visual** de interfaces financeiras densas que afastam tanto jovens acostumados a fintechs modernas quanto investidores maduros (40+) que exigem legibilidade cristalina, e a **ausência de uma experiência móvel de bolso** apoiada por Inteligência Artificial que traduza números frios em clareza imediata.

O **Kerno** ("o núcleo inteligente do seu dinheiro") resolve esse tripé ao unir o filtro fundamentalista da **Margem de Segurança de Graham** à disciplina de gestão de risco e alocação por novos aportes do **Diagrama do Cerrado**, potencializados pelo **Diagnóstico de Carteira do Kerno AI**. Em menos de 30 segundos pelo celular (iOS ou Android) ou na Web, o investidor visualiza seu **Dashboard Patrimonial** consolidado sobre um **Design System Intergeracional** inspirado na simplicidade do Nubank, digita o valor disponível no mês no **GPS de Aporte** e recebe a distribuição matemática exata de onde alocar — acompanhada de uma análise executiva em linguagem natural gerada por IA que explica a concentração de risco, a aderência às metas e o racional matemático da carteira sem jamais alucinar números ou cruzar a fronteira regulatória da CVM.

Mais do que uma atualização funcional, o **Kerno** estabelece a fundação arquitetural e de marca para se tornar o sistema operacional financeiro pessoal e familiar no Brasil: começando pela excelência em patrimônio, aportes inteligentes e análise de carteira por IA neste lançamento, com arquitetura modular já preparada para conectar o controle inteligente de gastos pessoais e o copiloto conversacional nos próximos ciclos.

---

## 2. Target User

### 2.1 Jobs To Be Done (JTBD)

- **Funcional (Alocação sem Giro Patrimonial):** *"Quando recebo meu salário ou proventos no mês, quero informar o valor disponível e descobrir exatamente em quais classes e ativos aportar para reequilibrar meu risco e aproveitar descontos fundamentalistas, sem precisar vender ativos vencedores nem pagar imposto desnecessário."*
- **Funcional (Compreensão Assistida por IA):** *"Quando olho para minha carteira consolidada, quero ler um diagnóstico claro em português simples que me aponte riscos de concentração setorial, ativos que perderam qualidade e o motivo exato das sugestões de aporte do mês, sem precisar cruzar dezenas de indicadores sozinho."*
- **Contextual / Mobilidade:** *"Quando estou com o aplicativo da corretora aberto no celular, quero consultar meu patrimônio e meu plano de aporte em menos de 30 segundos com desbloqueio biométrico e modo de privacidade, sem depender de abrir planilhas no computador."*
- **Emocional / Intergeracional:** *"Quero sentir tranquilidade, controle e confiança ao gerir o patrimônio da minha família em uma interface limpa, com letras legíveis e sem poluição visual, que me proteja do efeito manada do mercado."*

### 2.2 Non-Users (v1)

- **Day Traders e Especuladores de Curto Prazo:** Investidores que buscam gráficos *intraday*, análise técnica (*candlesticks*), alavancagem ou giro rápido de carteira.
- **Usuários em Busca de Execução Automática de Ordens (*Robo-Advisor* Custodiante):** Investidores que desejam que o aplicativo envie ordens de compra e venda diretamente para o livro de ofertas da B3.
- **Investidores Exclusivos de Criptoativos DeFi Não-Custodiados:** Carteiras *on-chain* descentralizadas fora do escopo das Macro Classes patrimoniais tradicionais.

### 2.3 Key User Journeys

- **UJ-1. Roberto (CFO da Família, 52 anos) decide e valida o aporte do mês pelo celular em 25 segundos.**
  - **Persona + contexto:** Roberto gerencia o patrimônio da família, usa fonte ampliada no iOS (`1.3x`) e acaba de receber uma transferência na conta da corretora.
  - **Entry state:** Aplicativo Mobile do **Kerno** instalado no iPhone, sessão previamente vinculada via Google Sign-In/OAuth2.
  - **Path:**
    1. Roberto toca no ícone do **Kerno** e autentica instantaneamente via FaceID.
    2. O **Dashboard Patrimonial** carrega com tipografia ampliada perfeita (sem textos cortados), exibindo o patrimônio consolidado e o gráfico das **Macro Classes**; ele toca na aba **GPS de Aporte**.
    3. Ele digita `R$ 5.000,00` no campo de aporte disponível e toca em *"Calcular Aporte"*.
    4. O sistema processa o **Diagrama do Cerrado** cruzado com a **Margem de Segurança de Graham** e apresenta 3 cartões limpos indicando os valores exatos em `R$` e a quantidade inteira de ações/cotas a adquirir, com o selo *"Bônus Graham"* na ação descontada.
  - **Climax:** Roberto abre o cartão expansível *"Por que estes ativos?"* e vê a matemática transparente: a classe Ações BR estava 4,2% abaixo da meta e o ativo recomendado possui Nota 9 no Diagrama e 28% de Margem de Segurança de Graham.
  - **Resolution:** Em menos de 30 segundos, Roberto alterna para o app da corretora e executa as compras com total convicção e zero vendas.
  - **Edge case:** Caso o mercado esteja fechado ou a API de cotações externas esteja indisponível, o **GPS de Aporte** calcula as quantidades com base na última cotação de fechamento (`D-1`) e exibe um aviso discreto *"Cotações de fechamento (D-1)"* sem bloquear o fluxo.
  - **Capability mapping:** O sistema deve autenticar via biometria (`→ FR-11`), renderizar o dashboard com escala tipográfica nativa (`→ FR-2`, `→ FR-12`), calcular o aporte pelo Diagrama do Cerrado + Graham (`→ FR-6`, `→ FR-7`) e explicar matematicamente o cálculo (`→ FR-8`).

- **UJ-2. Lucas (Investidor Digital Native, 27 anos) configura seu Diagrama do Cerrado sem fricção de tela em branco.**
  - **Persona + contexto:** Lucas acabou de criar conta pela nova **Landing Page** do **Kerno** e importou sua planilha da B3, mas nunca montou uma estratégia formal de pesos por risco.
  - **Entry state:** Autenticado no Web App ou Mobile logo após a conclusão da ingestão do arquivo da B3.
  - **Path:**
    1. Lucas acessa a seção **Diagrama do Cerrado** e o sistema oferece 3 **Templates de Alocação** pré-configurados (*Equilibrado Cerrado*, *Conservador Patrimonial*, *Acumulação Arrojada*).
    2. Ele seleciona *Equilibrado Cerrado* (40% Ações BR, 30% FIIs, 20% Renda Fixa, 10% Internacional) e vê seus ativos da B3 já inicializados com a **Nota de Qualidade (1–10)** neutra padrão (`6`) — acompanhada do assistente opcional de 5 critérios objetivos contábeis — como ponto de partida livre de viés editorial.
    3. Ele utiliza o assistente objetivo e o slider para ajustar a nota de uma ação específica de `6` para `9` por conhecer bem o setor e reduz uma empresa endividada para nota `4`.
    4. O sistema recalcula instantaneamente a meta percentual proporcional de cada ativo, aplica o **Teto Máximo de Risco** e move a ação de nota `4` para o status de **Quarentena de Aporte**.
  - **Climax:** Lucas vê imediatamente o mapa visual da sua carteira comparando *"Onde estou hoje"* versus *"Minha meta ideal no Diagrama"*, sem ter preenchido dezenas de planilhas do zero.
  - **Resolution:** Sua estratégia fica salva e pronta para alimentar o **GPS de Aporte** e o **Diagnóstico de Carteira com IA**.
  - **Edge case:** Se Lucas tentar salvar metas de **Macro Classes** cuja soma seja diferente de `100%` (ex.: `95%` ou `110%`), o sistema bloqueia a gravação e destaca visualmente a diferença exata que falta distribuir.
  - **Capability mapping:** O sistema deve fornecer templates editáveis e notas base (`→ FR-4`), gerenciar a hierarquia de 3 níveis com travas de teto de risco e quarentena (`→ FR-5`) e validar a soma de 100% das metas (`→ FR-4`).

- **UJ-3. Mariana (Investidora Analítica, 38 anos) solicita um Diagnóstico de Carteira com IA (*Kerno AI*) para auditar riscos ocultos.**
  - **Persona + contexto:** Mariana acumulou 18 ativos ao longo de 4 anos e suspeita que sua carteira está concentrada demais em poucos setores, mas não quer perder horas montando tabelas dinâmicas.
  - **Entry state:** Autenticada na aba **Kerno AI** (Web ou Mobile), com carteira consolidada e **Diagrama do Cerrado** ativos.
  - **Path:**
    1. Mariana toca em *"Gerar Diagnóstico da Carteira com IA"*.
    2. O backend consolida os indicadores determinísticos da carteira, remove 100% dos dados pessoais/CPF via camada de sanitização LGPD e solicita a síntese qualitativa ao motor do **Kerno AI**.
    3. Em poucos segundos, a tela exibe o diagnóstico estruturado em 4 cartões: **Resumo Executivo**, **Pontos Fortes**, **Alertas de Risco e Concentração** e **Racional do Plano de Aporte**.
  - **Climax:** Mariana descobre no cartão de alertas que, embora tenha 10 ações diferentes, 62% da sua classe de Ações BR está concentrada no setor elétrico/financeiro e que 2 ativos em **Quarentena de Aporte** (nota < 5) ainda representam 11% do seu patrimônio total.
  - **Resolution:** Mariana compreende exatamente como os próximos aportes mensais no **GPS de Aporte** diluirão essa concentração de forma orgânica ao longo dos próximos meses, amparada pelo disclaimer educativo de conformidade CVM no rodapé.
  - **Edge case:** Se o provedor externo de LLM sofrer timeout ou indisponibilidade, o sistema exibe instantaneamente os cartões de diagnóstico quantitativo determinístico (concentração setorial, desvios do Diagrama e ativos descontados por Graham) com um aviso de que o resumo narrativo por IA está temporariamente em fila, sem tela de erro bloqueante.
  - **Capability mapping:** O sistema deve gerar diagnóstico estruturado por IA ancorado em dados determinísticos (`→ FR-9`), anonimizar PII antes do envio ao LLM e aplicar guardrails CVM (`→ FR-10`), além de degradar graciosamente em falha do provedor (`→ FR-9`).

- **UJ-4. Thiago (Investidor no Transporte Público, 31 anos) consulta seu patrimônio no celular com Modo Privacidade.**
  - **Persona + contexto:** Thiago está no metrô no dia do pagamento e quer conferir a distribuição percentual das suas **Macro Classes** sem expor seu patrimônio em reais para passageiros ao lado.
  - **Entry state:** App Mobile aberto com **Modo Privacidade** ativado na sessão anterior.
  - **Path:**
    1. Thiago desbloqueia o app via impressão digital.
    2. O **Dashboard Patrimonial** abre com todos os valores monetários em `R$` mascarados (`R$ ••••••`), mantendo visíveis os gráficos e percentuais de alocação (`36% Ações BR / Meta 40%`).
    3. Ele navega até a barra inferior (**Carteira**, **GPS de Aporte**, **Kerno AI**, **Diagrama do Cerrado**) e confere os tickers prioritários para aporte em percentual.
  - **Climax:** Thiago consulta toda a saúde relativa da carteira em ambiente público com zero exposição de patrimônio líquido.
  - **Resolution:** Ele fecha o app com segurança; o estado do **Modo Privacidade** permanece persistido para o próximo acesso.
  - **Capability mapping:** O sistema deve persistir o toggle de Modo Privacidade no Dashboard Mobile/Web (`→ FR-12`) e oferecer navegação modular clara (`→ FR-13`).

- **UJ-5. Camila (Engenheira de Software, 34 anos) descobre o Kerno pela nova Landing Page e converte como Membro Founder com acesso vitalício.**
  - **Persona + contexto:** Camila investe há 3 anos, está frustrada com planilhas de rebalanceamento quebradas e pesquisa sobre alocação pelo Diagrama do Cerrado até chegar à **Landing Page** do **Kerno** (`https://kerno.afsdigital.com.br`).
  - **Entry state:** Visitante anônima acessando a URL pública em navegador mobile ou desktop durante a janela de lançamento.
  - **Path:**
    1. Camila lê a proposta de valor sob a nova identidade **Kerno** (*"Do caos das planilhas ao núcleo inteligente das suas finanças"*).
    2. Visualiza o destaque do **Programa Founders**: os primeiros investidores que adotarem e utilizarem a plataforma recebem o selo exclusivo de **Membro Founder**, tratamento prioritário e **acesso Pro gratuito vitalício (*Lifetime Free Access*)**.
    3. Clica no CTA principal *"Garantir Minha Vaga de Founder Gratuitamente"* e autentica com 1 clique via Google Sign-In.
  - **Climax:** Em menos de 1 minuto após aterrissar na página, Camila já está dentro do onboarding guiado com seu selo **Membro Founder** ativo, **Template de Alocação** pronto para uso e acesso vitalício garantido ao utilizar a aplicação.
  - **Resolution:** Conta criada e vinculada à coorte **Membro Founder** (ou ao trial PLG de 90 dias para cadastros posteriores ao encerramento do lote de Founders).
  - **Capability mapping:** O sistema deve apresentar a nova identidade Kerno (`→ FR-1`), exibir a oferta do Programa Founders na Landing Page (`→ FR-14`) e conceder o status de **Membro Founder** com acesso gratuito vitalício aos primeiros usuários ativos (`→ FR-15`).

---

## 3. Glossary

*Os termos abaixo possuem significado único e obrigatório em todo o PRD, na Especificação de UX, na Arquitetura e nas Histórias de Usuário. O uso de sinônimos não listados aqui é considerado violação de disciplina do documento.*

- **Kerno** — Nome e marca oficial da plataforma (anteriormente *Graham Select*), posicionada como ecossistema inteligente de patrimônio e finanças pessoais preparado para IA.
- **Design System Intergeracional** — Sistema visual unificado (Web e Mobile) do **Kerno**, inspirado na clareza do Nubank, caracterizado por contraste mínimo WCAG 2.1 AA/AAA, suporte a escalonamento tipográfico nativo (`Dynamic Type` / `textScaler`), numerais tabulares e ausência de tabelas densas em telas móveis.
- **Diagrama do Cerrado** — Metodologia de gestão de risco e alocação patrimonial do tipo *Buy & Hold* estruturada em 3 níveis (**Macro Classes**, **Subclasses** e **Nota de Qualidade** por ativo) que direciona exclusivamente novos aportes mensais sem venda de ativos vencedores.
- **Macro Classe** — Nível 1 da hierarquia do **Diagrama do Cerrado**, representando as grandes categorias patrimoniais (ex.: *Ações BR*, *FIIs*, *Renda Fixa / Tesouro*, *Internacional*) cuja soma de metas percentuais deve totalizar exatamente `100%`.
- **Subclasse** — Nível 2 da hierarquia do **Diagrama do Cerrado**, representando segmentos estratégicos dentro de uma **Macro Classe** (ex.: *Setores Defensivos* vs. *Crescimento* em Ações BR; *Tijolo* vs. *Papel* em FIIs), utilizados no MVP 2.0 como agrupadores analíticos de exposição no **Dashboard Patrimonial** e no diagnóstico do **Kerno AI (Diagnóstico de Carteira)**.
- **Nota de Qualidade (1–10)** — Pontuação inteira de `1` a `10` atribuída a um ativo individual (Nível 3 do **Diagrama do Cerrado**) com base em critérios fundamentalistas e de governança, que determina o peso proporcional do ativo dentro de sua **Macro Classe** e sua faixa de **Teto Máximo de Risco**.
- **Template de Alocação** — Perfil pré-configurado e editável do **Diagrama do Cerrado** (contendo distribuição sugerida de **Macro Classes** e inicialização neutra de **Notas de Qualidade (1–10)** iguais a `6` para os ativos, sem juízo de valor editorial) oferecido no onboarding para eliminar a fricção de tela em branco.
- **Teto Máximo de Risco** — Limite percentual máximo que um único ativo de risco idiossincrático/emissor único (*Ações BR*, *FIIs*, *Stocks/REITs individuais* e *Crédito Privado*) pode representar no patrimônio total consolidado, determinado pela faixa da sua **Nota de Qualidade (1–10)** (`8–10`: teto máximo de `10%`; `5–7`: teto de `6%`; `1–4`: teto de `0%` para novos aportes). Títulos públicos soberanos (*Tesouro Direto*) e ETFs de índices amplos diversificados são isentos do teto individual de emissor único, sendo limitados apenas pela meta da própria **Macro Classe**.
- **Quarentena de Aporte** — Estado automático atribuído a qualquer ativo com **Nota de Qualidade (1–10)** inferior a `5` (ou seja, de `1` a `4`) ou sob alerta de evento corporativo não sincronizado, no qual o ativo permanece visível no patrimônio consolidado, mas fica bloqueado para receber novos recursos no **GPS de Aporte**.
- **Margem de Segurança de Graham** — Desconto percentual entre a cotação atual de uma ação e seu Valor Intrínseco calculado pela fórmula de Benjamin Graham ($VI = \sqrt{22.5 \times \text{LPA} \times \text{VPA}}$, exigindo $LPA > 0$ e $VPA > 0$).
- **Multiplicador de Graham** — Fator de bônus (`1.30x`) aplicado ao score de priorização de aporte de ativos da **Macro Classe** *Ações BR* que sejam elegíveis no **Diagrama do Cerrado** e simultaneamente apresentem **Margem de Segurança de Graham** positiva (limiar padrão de margem mínima igual a `0%`, ou seja, $\text{Cotação Atual} < VI$), sem jamais ultrapassar o **Teto Máximo de Risco**.
- **GPS de Aporte** — Motor e interface de cálculo de alocação mensal onde o usuário informa o capital disponível (`R$ X`) e recebe a distribuição recomendada por classe e ativo aplicando a **Regra de Transbordamento**.
- **Regra de Transbordamento (*Spillover*)** — Comportamento algorítmico do **GPS de Aporte** no qual, sempre que o valor do aporte mensal exceder a defasagem ou atingir o **Teto Máximo de Risco** do ativo mais prioritário, o saldo remanescente transborda automaticamente para o próximo ativo ou **Macro Classe** elegível na fila de prioridade.
- **Kerno AI (Diagnóstico de Carteira)** — Módulo de Inteligência Artificial que consome exclusivamente os indicadores determinísticos pré-calculados da carteira (após sanitização de PII pelo **Sanitizador LGPD de IA**) para gerar um diagnóstico estruturado em linguagem natural contendo Resumo Executivo, Pontos Fortes, Alertas de Risco/Concentração e Racional do Plano de Aporte.
- **Sanitizador LGPD de IA** — Camada de segurança de backend que remove 100% dos dados pessoais identificáveis (nome, CPF, e-mail, telefone e IDs de conta) antes de qualquer chamada externa ao provedor de modelo de linguagem (LLM) do **Kerno AI (Diagnóstico de Carteira)**.
- **Dashboard Patrimonial** — Tela principal autenticada (Web e Mobile) que apresenta o patrimônio líquido consolidado, a evolução/rentabilidade e a distribuição atual versus meta das **Macro Classes**.
- **Modo Privacidade** — Controle visual global no cabeçalho do **Dashboard Patrimonial** que oculta todos os valores monetários em `R$` (`••••••`), preservando a visualização de percentuais e gráficos.
- **Membro Founder** — Categoria especial de usuário atribuída aos primeiros investidores que se cadastrarem e utilizarem ativamente o **Kerno** no lançamento, garantindo selo exclusivo no perfil, tratamento prioritário e acesso gratuito vitalício (*Lifetime Free Access*) a todos os recursos da camada Pro.

---

## 4. Features

### 4.1 Rebranding Oficial & Design System Intergeracional (Web e Mobile)

**Description:** Esta funcionalidade executa a transição completa da identidade legada (*Graham Select*) para a marca **Kerno** em todas as superfícies (Web App, Mobile iOS/Android, comunicações transacionais e metadados de lojas de aplicativos) e implanta o **Design System Intergeracional**. Inspirado na clareza e fluidez do Nubank, o novo sistema visual elimina tabelas financeiras claustrofóbicas em telas móveis, adota cartões respiráveis com alto contraste (WCAG 2.1 AA/AAA), numerais tabulares e escalonamento tipográfico nativo para atender com a mesma excelência tanto o investidor jovem quanto o investidor maduro (40+). Realiza `UJ-1`, `UJ-4` e `UJ-5`.

**Functional Requirements:**

#### FR-1: Identidade de Marca Unificada Kerno
O sistema deve exibir o nome **Kerno**, logotipo, paleta cromática institucional, favicon, ícones de aplicativo (iOS/Android), títulos de página, metadados SEO/PWA e textos institucionais atualizados em 100% das telas públicas e autenticadas, operando diretamente sob o domínio oficial `kerno.afsdigital.com.br`. Realiza `UJ-1`, `UJ-5`.

**Consequences (testable):**
- Nenhuma tela, cabeçalho HTTP/HTML, manifesto PWA, título de aplicativo Android (`AndroidManifest.xml`), título iOS (`Info.plist`) ou template de notificação **visível ao usuário final** exibe o nome legado *"Graham Select"* como nome da plataforma (referências a *"Benjamin Graham"* permanecem exclusivamente como nome técnico da metodologia de **Margem de Segurança de Graham**, e identificadores internos de infraestrutura legada — como o diretório de chart `helm/graham-select/` e recursos internos de banco/cluster — são preservados para evitar *downtime* ou recriação destrutiva no Kubernetes).
- Os metadados de título, URLs canônicas (`https://kerno.afsdigital.com.br`) e OpenGraph da aplicação web pública refletem *"Kerno — O núcleo inteligente do seu dinheiro"*.

#### FR-2: Design System Intergeracional & Acessibilidade Visual
A interface Web e Mobile deve aplicar os tokens do **Design System Intergeracional**, garantindo contraste mínimo WCAG 2.1 AA (`4.5:1` para texto normal e `7:1` AAA para saldos patrimoniais primários), tipografia com numerais tabulares (`tabular-nums`), áreas de toque mínimas de `48x48dp` e suporte nativo ao escalonamento de fonte do sistema operacional (`textScaler` até `2.0x`). Realiza `UJ-1`, `UJ-4`.

**Consequences (testable):**
- Quando o fator de escala de texto do sistema operacional (`MediaQuery.textScalerOf(context)`) é configurado entre `1.0x` e `2.0x`, nenhum valor monetário (`R$`), ticker de ativo ou percentual sofre truncamento com reticências (`...`) ou sobreposição de linha; os cartões expandem verticalmente de forma responsiva.
- Em viewports móveis (`largura < 600dp`), listagens de ativos e sugestões de aporte são renderizadas como cartões estruturados empilhados, sem exigir rolagem horizontal de tabelas.
- Todos os indicadores semânticos (lucro/desconto em verde, alerta/quarentena em âmbar, erro em vermelho) são acompanhados de ícone e rótulo textual explícito para acessibilidade a daltônicos.

#### FR-3: Preservação e Adaptação Visual dos Fluxos Brownfield (Ingestão B3, CRUD Multi-Classe e Multi-CPF)
Os fluxos existentes de importação assíncrona de arquivos da B3, edição manual de posições/proventos (CRUD) e filtro consolidado/individual de CPFs (Holding Familiar) devem operar integralmente sob o **Design System Intergeracional** e a identidade **Kerno**, permitindo o cadastro e edição manual de ativos vinculados a qualquer uma das 4 **Macro Classes** (*Ações BR*, *FIIs*, *Renda Fixa / Tesouro* e *Internacional*). Realiza `UJ-1`, `UJ-2`.

**Consequences (testable):**
- O usuário consegue importar planilhas da B3, cadastrar ou editar manualmente posições nas 4 **Macro Classes** (incluindo títulos de *Renda Fixa / Tesouro* e ativos *Internacionais* não oriundos do arquivo B3) e alternar a visualização entre CPFs da família utilizando os novos componentes visuais do **Kerno**, com 100% dos testes de regressão existentes aprovados.
- No MVP 2.0, todas as posições manuais de *Renda Fixa / Tesouro* e *Internacional* são registradas e consolidadas em moeda nacional (`BRL` / `R$`), onde ativos da classe *Internacional* negociados na B3 (BDRs e ETFs como `IVVB11`) utilizam cotação automática em `BRL` com quantização inteira de cotas, enquanto posições manuais externas e títulos de *Renda Fixa / Tesouro* operam por saldo financeiro contínuo em `R$` atualizado pelo usuário.

---

### 4.2 Carteira & Rebalanceamento pelo Diagrama do Cerrado + Graham

**Description:** Motor estratégico de alocação patrimonial *Buy & Hold* que substitui a indecisão mensal por um processo matemático transparente. No primeiro acesso à funcionalidade, o investidor escolhe um **Template de Alocação** pré-configurado (com metas para **Macro Classes** e **Notas de Qualidade (1–10)** neutras iniciais iguais a `6` para evitar qualquer viés de recomendação editorial CVM), podendo personalizar livremente pesos e notas individuais via slider ou assistente de 5 critérios objetivos. No dia de investir, o usuário informa o aporte disponível (`R$ X`) no **GPS de Aporte** e o sistema calcula a distribuição ideal priorizando a maior defasagem percentual relativa, respeitando o **Teto Máximo de Risco** de cada nota, bloqueando ativos em **Quarentena de Aporte**, aplicando a **Regra de Transbordamento** e concedendo o bônus do **Multiplicador de Graham** às ações descontadas — sem jamais sugerir a venda de ativos vencedores. Realiza `UJ-1` e `UJ-2`.

**Functional Requirements:**

#### FR-4: Onboarding com Templates de Alocação e Metas de Macro Classes
O investidor autenticado pode selecionar um **Template de Alocação** pré-configurado (*Equilibrado Cerrado*, *Conservador Patrimonial* ou *Acumulação Arrojada*) ou definir manualmente as metas percentuais das **Macro Classes** (*Ações BR*, *FIIs*, *Renda Fixa / Tesouro*, *Internacional*), enquanto as **Subclasses** (*Defensivas vs. Crescimento*; *Tijolo vs. Papel*) são classificadas automaticamente a partir do cadastro setorial do ativo para visualização analítica de exposição. Realiza `UJ-2`.

**Consequences (testable):**
- Ao selecionar um **Template de Alocação**, o sistema preenche automaticamente as metas das **Macro Classes** somando `100%` e inicializa todos os ativos da carteira com uma **Nota de Qualidade (1–10)** neutra padrão igual a `6` (isentando a plataforma de curadoria subjetiva/editorial sob as Resoluções CVM 19 e 20), permitindo que o próprio investidor ajuste ou calcule suas notas individuais a qualquer momento via slider ou assistente de 5 critérios objetivos (`FR-5`).
- O sistema valida a soma das **Macro Classes** em tempo real e rejeita (HTTP `422 Unprocessable Entity` na API e bloqueio visual no botão salvar) qualquer configuração cuja soma percentual seja diferente de `100.00%`.

#### FR-5: Gestão de Notas de Qualidade (1–10), Teto Máximo de Risco e Quarentena de Aporte
O investidor pode atribuir ou editar a **Nota de Qualidade (1–10)** de cada ativo da sua carteira — tanto via controle direto (slider `1–10`) quanto por meio de um assistente opcional de **5 critérios objetivos** (Governança, Lucro Consistente em 5 anos, Endividamento Controlado, Vantagem Competitiva/Perenidade e Histórico de Proventos/Crescimento, valendo até 2 pontos cada) — e o sistema deve calcular automaticamente a meta percentual proporcional do ativo diretamente dentro de sua **Macro Classe** (mantendo as **Subclasses** como agrupadores analíticos de exposição), aplicando o **Teto Máximo de Risco** e o status de **Quarentena de Aporte**. Realiza `UJ-2`.

**Consequences (testable):**
- A meta bruta do ativo $i$ na **Macro Classe** $C$ é calculada exatamente como $\left(\frac{\text{Nota}_i}{\sum \text{Notas elegíveis de } C}\right) \times \text{Meta}_C\%$ utilizando aritmética decimal estrita (`BigDecimal`).
- Para ativos de risco idiossincrático / emissor único (*Ações BR*, *FIIs*, *Stocks/REITs individuais* e *Crédito Privado*), o sistema limita a meta efetiva do ativo na carteira total pelo **Teto Máximo de Risco** correspondente à sua nota: teto máximo de `10%` do patrimônio total para notas `8` a `10`; teto máximo de `6%` para notas `5` a `7`. Títulos públicos soberanos (*Tesouro Direto* na classe *Renda Fixa / Tesouro*) e ETFs de índices amplos diversificados (ex.: `IVVB11`, `WRLD11`, `BOVA11`) são isentos da trava de emissor único (`6%`/`10%`), sendo limitados apenas pela meta da própria **Macro Classe** ($\text{Meta}_C\%$).
- **Redistribuição Intra-Classe de Excedente de Teto:** Quando o **Teto Máximo de Risco** limitar a meta bruta de um ativo ($\text{Meta}_{i,\text{bruta}}\% > \text{Teto}_{\text{Risco}}(N_i)$), o percentual excedente ($\text{Meta}_{i,\text{bruta}}\% - \text{Teto}_{\text{Risco}}(N_i)$) é redistribuído iterativamente de forma proporcional à nota entre os demais ativos elegíveis não-saturados da mesma **Macro Classe** até que $\sum \text{Meta}_{i,\text{efetiva}}\% = \text{Meta}_C\%$ (ou até que todos os ativos da classe atinjam seus respectivos tetos).
- Quando um ativo recebe **Nota de Qualidade (1–10)** entre `1` e `4`, o sistema marca o ativo com o selo **Quarentena de Aporte**, define seu teto de novos aportes como `0%` e exclui sua nota do denominador ($\sum \text{Notas elegíveis de } C$) de distribuição de novos aportes daquela **Macro Classe**, sem remover o valor atual do ativo do cômputo do patrimônio total da carteira.

#### FR-6: Cálculo do GPS de Aporte com Regra de Transbordamento (*Spillover*)
Quando o investidor informa um valor monetário disponível (`R$ X > 0`) no **GPS de Aporte**, o sistema calcula o patrimônio projetado ($\text{Patrimônio Atual} + X$), determina a defasagem percentual relativa de cada **Macro Classe** e ativo elegível, e distribui o valor do aporte priorizando as maiores defasagens sem sugerir venda de posições existentes. Realiza `UJ-1`.

**Consequences (testable):**
- Classes ou ativos cuja posição atual já seja igual ou superior à meta projetada ($\Delta_i \le 0$) ou que estejam em **Quarentena de Aporte** recebem `R$ 0,00` de sugestão de aporte.
- Dentro de cada **Macro Classe** com déficit agregado ($\Delta_C > 0$), o aporte alocado nunca ultrapassa o teto residual da própria classe ($\Delta_{C,\text{rem}}$) nem o déficit/teto individual de cada ativo ($\min(\Delta_i, \Delta_{C,\text{rem}}, \text{Espaço até Teto de Risco}_i, X_{\text{rem}})$), distribuindo o recurso entre os ativos elegíveis mais defasados da classe e aplicando a **Regra de Transbordamento** (*Spillover*) sucessivamente para os próximos ativos elegíveis e para a próxima **Macro Classe** mais defasada.
- **Critério Determinístico de Desempate (*Tie-Breaker*):** Caso dois ou mais ativos elegíveis apresentem exatamente o mesmo score de priorização ($S_i$), a ordem de prioridade é resolvida deterministicamente por: `1º Maior Nota de Qualidade (1–10) -> 2º Maior Margem de Segurança de Graham (%) -> 3º Maior déficit monetário absoluto (Delta_i) -> 4º Ordem alfabética crescente do Ticker (A-Z)`.
- **Quantização de Cotas Inteiras Dentro do Loop de Transbordamento:** Para ativos negociados em bolsa (*Ações BR*, *FIIs*, *ETFs e BDRs*), o cálculo da quantidade inteira de ações/cotas ($\lfloor \text{Valor Alocável} / \text{Cotação Atual}_i \rfloor$) ocorre **dentro** do passo iterativo de alocação: apenas o valor financeiro efetivamente consumido pelas cotas inteiras ($\text{Qtd Inteira}_i \times \text{Cotação Atual}_i$) é deduzido de $X_{\text{rem}}$, fazendo com que qualquer troco residual transborde imediatamente para o próximo ativo elegível da fila (enquanto posições de *Renda Fixa / Tesouro* e ativos manuais não-cotados em bolsa recebem sugestão contínua em `R$`, sem divisão por cotas).
- **Trava de Saturação Global de Tetos de Risco:** Caso todos os ativos elegíveis da carteira atinjam seu respectivo **Teto Máximo de Risco** e ainda reste saldo de aporte não alocado ($X_{\text{rem}} > 0$), a API (`unallocatedRiskProtectedBalance`) e a UI exibem o saldo remanescente em um cartão explícito *"Saldo Excedente Protegido por Teto de Risco (R$ X_rem)"*, orientando o investidor a cadastrar um novo ativo na **Macro Classe** correspondente ou alocar o excedente em Reserva/Renda Fixa, sem jamais violar o **Teto Máximo de Risco** de nenhum ativo.

#### FR-7: Integração do Multiplicador de Graham no GPS de Aporte
Durante a priorização dos ativos dentro da **Macro Classe** *Ações BR*, o motor do **GPS de Aporte** deve cruzar os ativos elegíveis no **Diagrama do Cerrado** com o cálculo da **Margem de Segurança de Graham** e aplicar o **Multiplicador de Graham** aos papéis descontados. Realiza `UJ-1`.

**Consequences (testable):**
- Toda ação elegível (Nota $\ge 5$ e $\Delta_i > 0$) que possua $LPA > 0$, $VPA > 0$ e **Margem de Segurança de Graham** positiva (limiar padrão de margem mínima igual a `0%`, isto é, $\text{Cotação Atual} < VI$, onde $VI = \sqrt{22.5 \times \text{LPA} \times \text{VPA}}$) recebe o **Multiplicador de Graham** de `1.30x` (`+30%`) sobre seu score de defasagem percentual relativa dentro da classe *Ações BR*, além de exibir o selo visual *"Oportunidade Graham + Cerrado"*.
- O bônus do **Multiplicador de Graham** altera apenas a ordem/prioridade de recebimento do aporte entre ações elegíveis, nunca permitindo que o ativo ultrapasse seu **Teto Máximo de Risco**.

#### FR-8: Transparência Matemática e Explicabilidade do Aporte (Conformidade CVM)
Cada sugestão gerada pelo **GPS de Aporte** deve apresentar um painel expansível (*"Por que estes ativos?"*) detalhando os números determinísticos que originaram o cálculo, acompanhado do disclaimer regulatório obrigatório. Realiza `UJ-1`.

**Consequences (testable):**
- O painel expansível exibe de forma padronizada e sem ambiguidade: `% Atual Pré-Aporte` ($V_{\text{atual}, i} / P_{\text{atual}}$), `% Meta Efetiva` ($\text{Meta}_{i,\text{efetiva}}\%$) e `% Projetada Pós-Aporte` ($(V_{\text{atual}, i} + \text{Aporte}_i) / P_{\text{proj}}$) tanto para a **Macro Classe** quanto para o ativo, além da `Nota de Qualidade (1–10)`, do `Teto Máximo de Risco` aplicável e, para Ações BR, do `Valor Intrínseco` e da `% de Margem de Segurança de Graham`.
- A tela do **GPS de Aporte** renderiza de forma visível e permanente o aviso de conformidade CVM informando que a distribuição é um cálculo matemático automatizado baseado exclusivamente nas metas configuradas pelo próprio usuário e em indicadores públicos, não constituindo recomendação personalizada de consultoria de valores mobiliários.

**Out of Scope:**
- Sugestão de venda de ativos para rebalanceamento (o rebalanceamento ocorre estritamente via novos aportes).
- Envio automático de ordens de compra para corretoras.

---

### 4.3 Análise de Carteira com Inteligência Artificial (*Kerno AI*)

**Description:** Funcionalidade de inteligência analítica que transforma os dados consolidados da carteira, os pesos do **Diagrama do Cerrado** e os múltiplos da **Margem de Segurança de Graham** em um diagnóstico executivo claro, empático e acionável em linguagem natural. Projetado sob a arquitetura *Deterministic-First*, o backend calcula primeiro todas as métricas exatas, remove 100% dos identificadores pessoais pelo **Sanitizador LGPD de IA** e utiliza o LLM exclusivamente para sintetizar e explicar o diagnóstico em 4 blocos padronizados — garantindo zero alucinação aritmética, total privacidade de dados e conformidade estrita com as Resoluções CVM 19 e 20. Realiza `UJ-3`.

**Functional Requirements:**

#### FR-9: Geração de Diagnóstico Estruturado da Carteira via Kerno AI
O investidor pode solicitar sob demanda ao abrir ou acionar o módulo **Kerno AI (Diagnóstico de Carteira)** um diagnóstico completo integrado ao provedor **Google Gemini** através de uma porta agnóstica de domínio (`AiPortfolioAnalyzerPort`) e estruturado em quatro seções: **Resumo Executivo**, **Pontos Fortes**, **Alertas de Risco e Concentração** e **Racional do Plano de Aporte**. Realiza `UJ-3`.

**Consequences (testable):**
- O payload enviado ao LLM (**Google Gemini**) é construído exclusivamente a partir do snapshot determinístico pré-calculado pelo backend em `BigDecimal` (distribuição por **Macro Classes** e **Subclasses**, concentração por ativo e setor, ativos em **Quarentena de Aporte**, ativos acima do **Teto Máximo de Risco** e descontos pela **Margem de Segurança de Graham**). O LLM é instruído via JSON Schema estrito a não realizar contas próprias nem inventar indicadores ausentes no snapshot.
- O diagnóstico é armazenado em cache (*cache-aside*) vinculado ao hash determinístico de estado da carteira (`portfolio_state_hash`, composto pelo SHA-256 de: lista ordenada de `ticker_canonico:quantidade_arredondada` + metas das **Macro Classes** + **Notas de Qualidade (1–10)**) com TTL de 24 horas para leitura instantânea. O *rate limiting* FinOps é aplicado diretamente por `user_id` no contador do backend (independentemente de mutações no `portfolio_state_hash`), permitindo até `5 gerações/regenerações de IA por dia` (e teto de `30 por mês`) por usuário na camada Pro/Founder.
- **Degradação Graciosa (*Graceful Degradation*):** Caso o provedor externo de LLM retorne erro ou exceda o timeout de `15 segundos`, a interface renderiza imediatamente o painel de diagnóstico quantitativo determinístico (gráficos e alertas baseados em regras de concentração setorial, desvios do **Diagrama do Cerrado** e oportunidades de Graham) com uma mensagem informativa não-bloqueante.

#### FR-10: Sanitização LGPD de PII e Guardrails Regulatórios CVM no Kerno AI
Toda interação entre o backend do **Kerno** e o provedor externo de LLM deve passar obrigatoriamente pelo **Sanitizador LGPD de IA** antes do envio e pelo validador de conformidade regulatória CVM antes da exibição ao usuário. Realiza `UJ-3`.

**Consequences (testable):**
- **Zero PII, Sanitização de Ativos Manuais e Isolamento Monetário no Payload de IA:** Testes automatizados de integração verificam que o JSON enviado ao provedor de LLM não contém nome do usuário, CPF, e-mail, telefone, `user_id` nominal, números de conta/corretora nem valores patrimoniais absolutos em `R$`. Para impedir vazamento de PII ou *prompt injection* via nomes livres de ativos cadastrados manualmente (`FR-3`), o **Sanitizador LGPD de IA** valida todos os identificadores contra uma *allowlist* estrita de tickers públicos da B3 (`^[A-Z]{4}[0-9]{1,2}$`) e títulos padronizados do Tesouro Direto, substituindo qualquer texto livre fora da *allowlist* por rótulos canônicos anonimizados (`RENDA_FIXA_MANUAL_1`, `INTERNACIONAL_MANUAL_1`). O texto gerado pelo LLM opera estritamente com esses identificadores canônicos, notas de risco e percentuais (`%`), enquanto os valores monetários em `R$` são exibidos exclusivamente nos badges e cabeçalhos determinísticos adjacentes renderizados pelo próprio aplicativo (respeitando o **Modo Privacidade**).
- **Guardrail Léxico e Regulatório Pós-Geração (CVM 19/20):** O prompt de sistema e o validador léxico pós-geração (baseado em radicais morfológicos em português como `compr[aeo]|vend[aeo]|adquir[ai]|alien[ae]|garantid[oa]|recomenda`) bloqueiam expressões imperativas de recomendação ou promessa de ganho (ex.: *"Compre agora"*, *"Venda suas ações"*, *"Lucro garantido"*). Em caso de violação detectada em qualquer um dos 4 blocos JSON retornados pelo LLM, o sistema **não realiza retry síncrono no LLM**: ele aciona imediatamente o fallback determinístico de `FR-9`, registra log `WARN/ERROR` de auditoria regulatória e não debita a chamada da cota diária do usuário.
- Todo diagnóstico do **Kerno AI (Diagnóstico de Carteira)** exibe a data/hora do snapshot analisado e o disclaimer legal de que a síntese é gerada por Inteligência Artificial para fins informativos e educacionais com base nos parâmetros do usuário, não configurando relatório de análise CNPI nem consultoria CVM.
- Todos os registros persistidos de diagnósticos do **Kerno AI (Diagnóstico de Carteira)** são integralmente removidos pelo `UserDataPurgeAdapter` quando o usuário solicita a exclusão de sua conta (Direito ao Esquecimento / LGPD).

**Out of Scope:**
- Chat conversacional aberto de perguntas livres sobre o mercado ou sobre despesas pessoais (reservado para o ciclo futuro do Copiloto Conversacional).

---

### 4.4 Experiência Mobile Nativa (iOS & Android) & Fundação Modular

**Description:** Aplicativo móvel nativo compilado via Flutter para iOS e Android que coloca o **Kerno** no bolso do investidor no momento exato da decisão de aporte. O MVP Mobile entrega autenticação rápida com persistência segura e desbloqueio biométrico, o **Dashboard Patrimonial** com **Modo Privacidade**, o acesso completo ao **Diagrama do Cerrado**, ao **GPS de Aporte** e ao **Kerno AI (Diagnóstico de Carteira)**, estruturado sobre uma arquitetura de navegação modular controlada por *feature flags* já pronta para receber o futuro módulo de Controle de Gastos sem refatoração. Realiza `UJ-1`, `UJ-3` e `UJ-4`.

**Functional Requirements:**

#### FR-11: Autenticação Mobile OAuth2/Google com Persistência Segura e Desbloqueio Biométrico
O aplicativo Mobile (iOS e Android) deve autenticar o usuário reutilizando o provedor Google Sign-In / OAuth2 + JWT do sistema, persistindo as credenciais de sessão em armazenamento criptografado nativo e exigindo validação biométrica (FaceID, TouchID ou Biometria Android) ao abrir o app ou retomar de inatividade. Realiza `UJ-1`, `UJ-4`.

**Consequences (testable):**
- Os tokens JWT (`access_token` e `refresh_token`) são armazenados exclusivamente via `flutter_secure_storage` (Keychain no iOS e EncryptedSharedPreferences/Keystore no Android), nunca em `SharedPreferences` texto puro.
- Ao abrir o aplicativo com sessão válida ou ao retornar do segundo plano após mais de `2 minutos` de inatividade, o app solicita autenticação biométrica nativa via `local_auth` antes de exibir qualquer dado do **Dashboard Patrimonial**.
- Caso o dispositivo não possua biometria cadastrada ou a biometria falhe repetidamente, o sistema oferece fallback seguro para PIN/credencial do sistema operacional ou reautenticação OAuth2.

#### FR-12: Dashboard Patrimonial Mobile com Modo Privacidade e Proteção de Snapshot em Background
O investidor autenticado no Mobile (e na Web) visualiza imediatamente no **Dashboard Patrimonial** seu patrimônio líquido consolidado, rentabilidade e a distribuição atual comparada às metas das **Macro Classes** do **Diagrama do Cerrado**, contando com um botão de **Modo Privacidade** no cabeçalho e proteção automática contra exposição visual no seletor de aplicativos do sistema operacional (*App Switcher*). Realiza `UJ-1`, `UJ-4`.

**Consequences (testable):**
- Ao tocar no ícone de **Modo Privacidade** (olho aberto/fechado), todos os valores monetários em `R$` na tela (patrimônio total, valores por classe e valores de ativos) são instantaneamente substituídos por máscara visual (`R$ ••••••`), mantendo visíveis os percentuais de alocação (`%`) e gráficos de proporção.
- A preferência do **Modo Privacidade** (ativado/desativado) é persistida localmente no dispositivo entre sessões.
- Sempre que o ciclo de vida do aplicativo Mobile entrar em estado inativo ou segundo plano (`AppLifecycleState.inactive` ou `AppLifecycleState.paused`, como ao alternar para o aplicativo da corretora), a interface aplica automaticamente overlay/máscara nativa de privacidade (`FLAG_SECURE` / snapshot mask no Android e iOS), impedindo que saldos em `R$` fiquem visíveis na miniatura do carrossel de aplicativos recentes do sistema operacional.

#### FR-13: Arquitetura de Navegação Modular com Feature Flags (Pronta para Controle de Gastos)
O aplicativo Mobile e Web deve estruturar sua navegação principal (`BottomNavigationBar` no Mobile e barra lateral/superior na Web) e seus módulos de código de forma desacoplada, habilitando no lançamento as áreas **Carteira**, **GPS de Aporte**, **Kerno AI** e **Diagrama**, e mantendo os módulos de **Controle de Gastos** (`features/expenses`) e **Copiloto Conversacional** (`features/ai_copilot`) pré-mapeados sob *feature flags*. Realiza `UJ-1`, `UJ-4`.

**Consequences (testable):**
- A árvore de rotas e a barra de navegação leem as *feature flags* de configuração sem exigir refatoração das rotas existentes quando a flag `enable_expenses_module` for ativada em ciclos futuros.
- O contrato de entrada do **GPS de Aporte** (`R$ X`) possui interface de domínio desacoplada capaz de receber tanto o valor digitado manualmente pelo usuário quanto, futuramente, o saldo líquido mensal ($\text{Entradas} - \text{Saídas}$) calculado pelo módulo de Controle de Gastos.

---

### 4.5 Nova Landing Page de Alta Conversão, Programa Founders & Camadas Comerciais

**Description:** Refatoração completa da vitrine pública de aquisição sob a marca **Kerno**, substituindo o antigo posicionamento de *screener* quantitativo por uma narrativa direta e aspiracional (*"Do caos das planilhas ao núcleo inteligente das suas finanças"*). A página destaca a experiência Mobile + Web, explica visualmente o poder combinado do **Diagrama do Cerrado**, da **Margem de Segurança de Graham** e do **Kerno AI (Diagnóstico de Carteira)**, e apresenta como grande alavanca de aquisição inicial o **Programa Founders**: os primeiros usuários que começarem a utilizar a aplicação são reconhecidos como **Membros Founders**, recebendo tratamento especial e **acesso gratuito vitalício (*Lifetime Free Access*)** aos recursos completos da plataforma (com transição automática para o modelo Freemium + Trial PLG de 90 dias para coortes posteriores ao encerramento do lote de Founders). Realiza `UJ-5`.

**Functional Requirements:**

#### FR-14: Landing Page Kerno de Alta Conversão com Destaque do Programa Founders e Isolamento SEO
O sistema deve servir uma nova **Landing Page** pública otimizada para conversão e SEO sob a marca **Kerno**, apresentando proposta de valor clara, seção de destaque para o **Programa Founders** (comunicando que os primeiros usuários ativos serão considerados **Membros Founders**, terão tratamento especial e acesso gratuito vitalício), demonstração visual do **GPS de Aporte**, do **Diagrama do Cerrado** e do **Kerno AI (Diagnóstico de Carteira)**, vitrine Mobile (iOS/Android) e chamadas para ação (CTAs) diretas para autenticação Google/OAuth2. Realiza `UJ-5`.

**Consequences (testable):**
- A **Landing Page** exibe seção dedicada e banner/badge de destaque do **Programa Founders**, comunicando claramente os dois benefícios exclusivos dos primeiros adotantes: status de **Membro Founder** com tratamento especial e **acesso gratuito vitalício** para os primeiros usuários que começarem a utilizar a aplicação.
- A **Landing Page** carrega com *First Contentful Paint (FCP)* `< 1.2s`, layout 100% responsivo (Mobile e Desktop) e contraste aprovado no **Design System Intergeracional**.
- Apenas as rotas públicas institucionais são indexáveis por mecanismos de busca (`robots.txt` e meta tags `index, follow`); todas as rotas autenticadas do aplicativo (`/app/*`, `/dashboard/*`, `/api/*`) bloqueiam estritamente a indexação (`noindex, nofollow`).

#### FR-15: Elegibilidade ao Status de Membro Founder (Acesso Vitalício) e Camadas Comerciais (Free vs. Pro com Trial de 90 Dias)
O sistema deve identificar e marcar os primeiros usuários que se cadastrarem e iniciarem o uso ativo da aplicação durante o lote inicial de lançamento como **Membros Founders** (`plan_tier = FOUNDER_LIFETIME`), concedendo-lhes acesso gratuito vitalício a 100% das funcionalidades Pro, além de suportar o modelo comercial SaaS Freemium (com *PLG Trial* de **90 dias**) para os usuários que ingressarem após o encerramento do lote de Founders. Realiza `UJ-2`, `UJ-3`, `UJ-5`.

**Consequences (testable):**
- **Concessão e Governança do Lote de Membro Founder:** Enquanto o contador configurável de vagas no backend possuir saldo disponível (`founder_slots_remaining > 0`, com limite padrão inicial de `250 vagas` configurável via variável de ambiente), o usuário que criar conta via Google Sign-In/OAuth2 recebe atomicamente no momento do cadastro o atributo `plan_tier = FOUNDER_LIFETIME`, exibe o badge exclusivo *"Founder"* na interface autenticada e possui acesso vitalício isento de cobrança a todos os recursos da camada **Pro** (condicionado à ativação da carteira em até 30 dias após o cadastro para evitar reserva inativa por bots).
- Para novos usuários cadastrados após o esgotamento do lote de **Membros Founders** (`founder_slots_remaining == 0`), o sistema concede automaticamente `90 dias` de degustação (*PLG Trial*) da camada **Pro** sem exigência de cartão de crédito no cadastro.
- **Permissões Exatas e Proteção FinOps na Camada Free (aplicável apenas a usuários não-Founders após o término dos 90 dias de trial):**
  - *Liberado no Free:* Importação de planilhas da B3, cadastro manual de posições, visualização completa do **Dashboard Patrimonial** (incluindo gráfico de **Macro Classes** atual vs. meta do **Diagrama do Cerrado**), simulação de defasagem no nível de **Macro Classes** no **GPS de Aporte** e leitura do cartão **Resumo Executivo** do **Kerno AI (Diagnóstico de Carteira)** — sendo este **Resumo Executivo** na camada Free gerado via motor determinístico parametrizado do backend (`0` chamadas externas ao LLM Google Gemini para contas Free pós-trial, protegendo a contra-métrica `SM-C2`).
  - *Exclusivo Pro / Founder (exibe CTA/Paywall de Upgrade quando acionado por usuário Free pós-trial):* Edição individual de **Notas de Qualidade (1–10)**, distribuição detalhada por ticker individual no **GPS de Aporte** com **Multiplicador de Graham**, geração por LLM e leitura dos 3 cartões analíticos detalhados do **Kerno AI** (*Pontos Fortes*, *Alertas de Risco e Concentração* e *Racional do Plano de Aporte*) e consolidação Multi-CPF (Holding Familiar).
  - *Máquina de Estados no Downgrade (Pro Trial 90 Dias → Free):* Nenhum dado cadastrado durante o trial é excluído ao expirar os 90 dias; as **Notas de Qualidade (1–10)** personalizadas durante o trial permanecem ativas em modo somente-leitura (*read-only*), e, caso o usuário tenha cadastrado múltiplos CPFs familiares, o sistema mantém ativo o CPF titular (primeiro CPF cadastrado) e pausa a consolidação dos CPFs secundários exibindo um banner não-destrutivo de upgrade.

---

### 4.6 Governança de Dados, Conformidade LGPD e Resiliência Operacional

**Description:** Requisitos funcionais estruturais que garantem o cumprimento integral da Lei Geral de Proteção de Dados (Direito ao Esquecimento / Art. 18 LGPD) sobre todas as novas entidades do Ciclo 2.0, além da integridade dos dados de mercado que alimentam o **Diagrama do Cerrado** e a **Margem de Segurança de Graham**. Realiza `UJ-1`, `UJ-2`, `UJ-3`.

**Functional Requirements:**

#### FR-16: Expurgo Integral LGPD das Entidades do Diagrama do Cerrado e Kerno AI (Direito ao Esquecimento)
Quando o usuário solicita a exclusão definitiva de sua conta, o sistema deve expurgar de forma atômica e irreversível (hard-delete) todos os registros vinculados ao usuário, incluindo obrigatoriamente todas as novas tabelas do Ciclo 2.0: configurações de **Macro Classes** e **Subclasses**, **Notas de Qualidade (1–10)** por ativo, históricos de simulação do **GPS de Aporte** e snapshots/diagnósticos do **Kerno AI (Diagnóstico de Carteira)**. Realiza `UJ-1`, `UJ-2`, `UJ-3`.

**Consequences (testable):**
- O adaptador `UserDataPurgeAdapter` remove 100% dos registros das novas entidades do usuário na mesma transação ou fluxo determinístico de expurgo, sem deixar registros órfãos nem violar constraints de chave estrangeira.
- O teste de integração automatizado `AccountDeletionIT` valida explicitamente a criação e a remoção completa dos dados do **Diagrama do Cerrado**, do **GPS de Aporte** e do **Kerno AI (Diagnóstico de Carteira)**.

#### FR-17: Proteção contra Anomalias de Cotações (*Circuit Breaker* de Dados de Mercado)
O motor que alimenta a **Margem de Segurança de Graham** e o **GPS de Aporte** deve validar variações diárias extremas de preços ou múltiplos oriundas de provedores externos antes de aplicá-las no cálculo de recomendação. Realiza `UJ-1`.

**Consequences (testable):**
- Se um ativo apresentar variação diária anômala não ajustada (queda ou salto abrupto `> 40%` em um único pregão) ou divergência estrutural abrupta nos múltiplos implícitos ($P/L$ ou $P/VP$ variando `> 40%` sem novo balanço publicado, típico de grupamento/desdobramento ainda não sincronizado na fonte externa ou na quantidade da carteira), o sistema ativa o estado persistente `QUARENTENA_EVENTO_CORPORATIVO` para o ticker (que permanece ativo nos pregões seguintes `D+1..D+N` até sincronização ou confirmação da posição pelo usuário), congela a última cotação válida pré-anomalia para fins de cômputo de saldo patrimonial, suspende novas sugestões de aporte naquele ticker no **GPS de Aporte** e sinaliza *"Cotação em verificação de evento corporativo"*.

---

## 5. Non-Goals (Explicit)

Para proteger o foco de engenharia, a velocidade de lançamento comercial e o enquadramento regulatório do **Kerno** no Ciclo 2.0, o produto **NÃO** fará o seguinte neste lançamento:

- **Não fará recomendação de venda de ativos (*Zero Forced Selling*):** O **Diagrama do Cerrado** e o **GPS de Aporte** operam exclusivamente direcionando o dinheiro novo (`R$ X`). O sistema jamais emitirá ordens ou sugestões de venda de posições para rebalancear a carteira.
- **Não executará ordens em corretoras (*No Broker Execution*):** O **Kerno** é o cérebro analítico e consolidador; a execução da compra das ações, FIIs ou títulos permanece 100% no home broker ou aplicativo da corretora de preferência do usuário.
- **Não entregará o módulo funcional de Controle de Gastos Pessoais neste MVP:** Embora a marca **Kerno** e a arquitetura modular de navegação (`FR-13`) já nasçam prontas para essa expansão, o lançamento funcional de categorização de despesas, cartões de crédito e orçamento doméstico fica explicitamente para o próximo ciclo.
- **Não entregará um Chatbot Conversacional Livre de IA neste MVP:** A entrega de IA neste ciclo é o **Kerno AI (Diagnóstico de Carteira)** estruturado, determinístico e auditável (`FR-9`, `FR-10`). Um chat aberto de perguntas e respostas livres (*Copiloto Conversacional*) será construído em ciclo futuro sobre esta base.
- **Não atuará como Consultoria de Investimentos CVM nem Casa de Research CNPI:** O **Kerno** não emite carteiras recomendadas editoriais nem opiniões subjetivas de analistas humanos; toda priorização decorre matematicamente das metas e notas configuradas pelo próprio usuário cruzadas com fórmulas fundamentalistas públicas.

---

## 6. MVP Scope (Ciclo Comercial 2.0 — Kerno)

### 6.1 In Scope

- **Rebranding Completo para Kerno:** Nova marca, logotipo, paleta, textos institucionais e metadados no Web, Mobile (iOS/Android) e **Landing Page** (`FR-1`).
- **Design System Intergeracional:** Interface redesenhada com inspiração no Nubank, contraste WCAG 2.1 AA/AAA, suporte a `textScaler` até `2.0x` sem truncamento, numerais tabulares e cartões responsivos (`FR-2`, `FR-3`).
- **Carteira e Rebalanceamento pelo Diagrama do Cerrado + Graham:** **Templates de Alocação** pré-configurados, hierarquia de 3 níveis (**Macro Classes**, **Subclasses**, **Nota de Qualidade (1–10)**), **Teto Máximo de Risco**, **Quarentena de Aporte**, **GPS de Aporte** com **Regra de Transbordamento**, **Multiplicador de Graham** e caixa de transparência matemática (`FR-4` a `FR-8`).
- **Análise de Carteira com IA (*Kerno AI*):** Diagnóstico estruturado em 4 blocos (Resumo Executivo, Pontos Fortes, Alertas de Risco/Concentração e Racional do Plano de Aporte) ancorado em dados determinísticos, com **Sanitizador LGPD de IA** (zero PII no LLM), cache inteligente, degradação graciosa e guardrails CVM (`FR-9`, `FR-10`).
- **Aplicativo Mobile Nativo (iOS & Android via Flutter):** Login Google/OAuth2 com `flutter_secure_storage` e desbloqueio biométrico (FaceID/Digital), **Dashboard Patrimonial** com **Modo Privacidade**, acesso ao **GPS de Aporte** e **Kerno AI**, e barra de navegação modular controlada por *feature flags* (`FR-11` a `FR-13`).
- **Nova Landing Page, Programa Founders (Acesso Vitalício) & Estrutura Comercial Freemium/Pro:** Página pública de alta conversão sob a marca **Kerno** destacando que os primeiros usuários ativos serão **Membros Founders** com tratamento especial e acesso gratuito vitalício, além de isolamento SEO e suporte a trial PLG de 90 dias para coortes seguintes (`FR-14`, `FR-15`).
- **Conformidade LGPD e Resiliência de Dados:** Atualização obrigatória do `UserDataPurgeAdapter` + `AccountDeletionIT` para todas as novas tabelas e *circuit breaker* de anomalias de cotação (`FR-16`, `FR-17`).

### 6.2 Out of Scope for MVP (Próximos Ciclos)

- **Módulo Funcional de Controle de Gastos e Fluxo de Caixa Pessoal (Ciclo 2.1):** Apenas o contrato arquitetural, os pacotes isolados (`features/expenses`) e o slot de navegação controlado por *feature flag* entram agora. `[NOTE FOR PM: Assim que o módulo de Gastos entrar no Ciclo 2.1, seu saldo livre mensal (Entradas - Saídas) alimentará automaticamente o campo R$ X do GPS de Aporte.]`
- **Copiloto Conversacional de IA / Chat Financeiro Aberto (Ciclo 2.2):** Diferido para garantir primeiro a maturidade regulatória, o controle de custos de tokens (FinOps) e a confiança no **Kerno AI (Diagnóstico de Carteira)** estruturado.
- **Sincronização Direta via Open Finance / Pluggy com Corretoras (Ciclo 3.0):** Mantida a ingestão assíncrona via arquivo oficial da B3 e edição assistida para preservar margem operacional e independência de agregadores pagos no lançamento.
- **Leitura OCR de Notas de Corretagem SINACOR em PDF e Alertas Proativos no WhatsApp (Ciclo 3.0):** Diferidos do backlog legado em favor da experiência nativa Mobile iOS/Android e do **GPS de Aporte**.

---

## 7. Cross-Cutting NFRs (Requisitos Não-Funcionais)

### 7.1 Performance e Latência
- **NFR-1 (Velocidade do GPS de Aporte):** O cálculo completo do **GPS de Aporte** (cruzando **Diagrama do Cerrado**, **Regra de Transbordamento** e **Multiplicador de Graham**) deve responder em `< 1.0 segundo` (P95) para carteiras de até 100 ativos.
- **NFR-2 (Fluidez de UI Web e Mobile):** As transições de tela e a ativação do **Modo Privacidade** no aplicativo Flutter (Web, iOS e Android) devem ocorrer a `60fps` (`< 300ms` de tempo de resposta visual), e o carregamento do **Dashboard Patrimonial** autenticado deve concluir em `< 1.5 segundo` (P95).
- **NFR-3 (Latência e Cache do Kerno AI):** Leituras de diagnósticos cacheados do **Kerno AI (Diagnóstico de Carteira)** devem responder em `< 400ms` (P95); novas gerações via LLM devem retornar em `< 10 segundos` (P95), com timeout rígido e fallback determinístico em `15 segundos`.

### 7.2 Segurança, Precisão Financeira e Isolamento Multi-Tenant
- **NFR-4 (Aritmética Decimal Estrita):** 100% dos cálculos patrimoniais, pesos do **Diagrama do Cerrado**, **Teto Máximo de Risco**, **Margem de Segurança de Graham** e distribuições do **GPS de Aporte** devem utilizar tipos decimais exatos (`BigDecimal` no Java e `NUMERIC(19,4)` ou superior no PostgreSQL). O uso de `float` ou `double` para valores monetários ou percentuais de alocação é estritamente proibido.
- **NFR-5 (Isolamento Multi-Tenant e Criptografia):** Todos os dados de carteira, metas do **Diagrama do Cerrado** e diagnósticos do **Kerno AI** devem respeitar isolamento estrito por `user_id` no banco de dados, com criptografia `TLS 1.3` em trânsito, `AES-256` em repouso e armazenamento local mobile restrito ao Keychain/Keystore via `flutter_secure_storage`.

### 7.3 Observabilidade, Migrações e Paridade de Implantação (Regras de Engenharia)
- **NFR-6 (Rastreabilidade e Logs Obrigatórios):** Toda entrada de requisição em endpoints REST (incluindo cálculos do **Diagrama do Cerrado**, **GPS de Aporte** e **Kerno AI**), consumo de filas/tópicos e chamadas externas deve registrar log estruturado com contexto relevante (sem vazar PII/tokens). Toda captura ou lançamento de exceção deve ser obrigatoriamente logada no nível `ERROR` com o respectivo *stack trace*.
- **NFR-7 (Imutabilidade de Migrations Liquibase):** Nenhuma migration Liquibase existente pode ser alterada retroativamente. Todas as novas tabelas e colunas do **Diagrama do Cerrado** e do **Kerno AI** devem ser criadas via novos changesets incrementais e idempotentes.
- **NFR-8 (Paridade Helm e CI/CD):** Todas as novas propriedades e variáveis de ambiente introduzidas (como configurações do provedor de IA do **Kerno AI**, limites de rate-limit e *feature flags* modulares) devem ser obrigatoriamente refletidas em `application.yml`, no chart Helm (`helm/graham-select/values.yaml`, `configmap.yaml` e `secret.yaml`) e na pipeline `.github/workflows/deploy.yml`, mantendo o sufixo `-it` nos testes de integração do backend e validação estática limpa (`flutter analyze` com zero erros/warnings).

---

## 8. Compliance, Privacy (LGPD) & Guardrails Regulatórios (CVM)

- **Enquadramento Regulatório CVM (Resoluções CVM nº 19 e nº 20):**
  - O **Kerno** opera juridicamente como uma **ferramenta tecnológica de consolidação e calculadora matemática parametrizada pelo próprio usuário**.
  - Nem o **GPS de Aporte** nem o **Kerno AI (Diagnóstico de Carteira)** escolhem ativos de forma autônoma ou discricionária: o motor apenas aplica a fórmula pública de Benjamin Graham e distribui matematicamente o aporte conforme as metas de **Macro Classes** e **Notas de Qualidade (1–10)** definidas ou confirmadas pelo próprio investidor.
  - Todas as telas de sugestão de aporte e diagnóstico de IA incluem disclaimer regulatório explícito e linguagem descritiva não-imperativa (`FR-8`, `FR-10`).
- **Privacidade e Proteção de Dados Pessoais (LGPD — Lei nº 13.709/2018):**
  - **Minimização e Anonimização na IA:** O **Sanitizador LGPD de IA** garante que nenhum dado pessoal identificável (nome, CPF, e-mail, telefone) saia da fronteira do backend do **Kerno** em direção a provedores externos de LLM (`FR-10`).
  - **Direito ao Esquecimento (Art. 18, VI):** O expurgo determinístico via `UserDataPurgeAdapter` cobre 100% das entidades legadas e das novas entidades do Ciclo 2.0, com validação contínua no `AccountDeletionIT` (`FR-16`).
- **Guardrails de Custos de IA (FinOps):**
  - Cache por hash de estado da carteira (`portfolio_state_hash`), invalidação apenas em mudanças materiais de posição/metas e *rate limiting* por usuário impedem explosão de custos de API de LLM e ataques de negação de serviço econômica.

---

## 9. Platform & Form-Factor Strategy

| Superfície | Tecnologia / Alvo | Papel no Ecossistema Kerno (Ciclo 2.0) |
| :--- | :--- | :--- |
| **Mobile Nativo (iOS & Android)** | Flutter (compilação nativa ARM64 para App Store e Google Play) | Superfície primária de bolso para o dia do aporte: Login com FaceID/Biometria, **Dashboard Patrimonial** com **Modo Privacidade**, **GPS de Aporte**, **Kerno AI (Diagnóstico de Carteira)** e consulta/ajuste do **Diagrama do Cerrado**. |
| **Web App Responsivo** | Flutter Web / SPA Autenticada | Experiência completa em telas grandes e médias para onboarding inicial, upload de planilhas históricas da B3, configuração detalhada de **Notas de Qualidade (1–10)**, visão Multi-CPF e leitura expandida do **Kerno AI**. |
| **Landing Page Pública** | Web Responsiva Otimizada para SEO & Performance | Vitrine de aquisição de alta conversão sob a marca **Kerno**, storytelling do produto, demonstração visual do método e entrada direta para autenticação OAuth2. |

---

## 10. Success Metrics

*Cada Métrica de Sucesso (SM) valida requisitos funcionais específicos. As Contra-Métricas (SM-C) estabelecem limites inegociáveis para impedir otimizações perversas.*

**Primary**
- **SM-1 (Adoção do Diagrama do Cerrado & GPS de Aporte):** Pelo menos **60% dos usuários ativos** possuem metas do **Diagrama do Cerrado** configuradas e realizam simulações/aportes recorrentes no **GPS de Aporte** nos primeiros 60 dias após o lançamento. Valida `FR-4`, `FR-5`, `FR-6`, `FR-7`. Realiza `UJ-1`, `UJ-2`.
- **SM-2 (Velocidade de Decisão / Time-to-Action):** Tempo mediano entre a abertura do aplicativo (desbloqueio biométrico) e a visualização da recomendação completa no **GPS de Aporte** **inferior a 30 segundos**. Valida `FR-6`, `FR-11`, `FR-12`, `NFR-1`, `NFR-2`. Realiza `UJ-1`.
- **SM-3 (Adoção Mobile Pré-Aporte):** Pelo menos **65% das sessões semanais** de consulta patrimonial e simulação de aporte originadas no aplicativo Mobile (iOS/Android) nos primeiros 60 dias pós-lançamento. Valida `FR-11`, `FR-12`, `FR-13`. Realiza `UJ-1`, `UJ-4`.
- **SM-4 (Conversão da Nova Landing Page Kerno):** Aumento mínimo de **25% na taxa de conversão** de visitantes únicos para contas criadas em comparação à landing page legada do *Graham Select*. Valida `FR-1`, `FR-14`. Realiza `UJ-5`.

**Secondary**
- **SM-5 (Engajamento e Utilidade do Kerno AI):** Pelo menos **50% dos usuários ativos com carteira importada** geram ou consultam o **Kerno AI (Diagnóstico de Carteira)** no primeiro mês, com índice de avaliação positiva (*"Este diagnóstico foi útil?"* 👍) **superior a 80%**. Valida `FR-9`, `FR-10`. Realiza `UJ-3`.
- **SM-6 (Aprovação de Usabilidade Intergeracional):** Índice de satisfação visual e clareza de leitura **superior a 85%** em pesquisas in-app segmentadas tanto na faixa etária `< 35 anos` quanto na faixa `40+ anos`. Valida `FR-2`, `FR-3`. Realiza `UJ-1`, `UJ-4`.

**Counter-metrics (do not optimize)**
- **SM-C1 (Zero Alucinação Numérica e Zero Vazamento de PII na IA):** Taxa de divergência entre números citados pelo **Kerno AI (Diagnóstico de Carteira)** e os cálculos determinísticos do backend deve ser **`0.0%`**, e o volume de PII enviado ao provedor externo de LLM deve ser **`0 bytes`**. Contrapesa `SM-5` (proíbe sacrificar precisão matemática ou privacidade LGPD para deixar o texto da IA mais criativo ou rápido).
- **SM-C2 (Estabilidade de Custo de IA por Usuário Ativo — FinOps):** O custo médio mensal de inferência de LLM (**Google Gemini**) por usuário ativo deve permanecer **`<= R$ 0,50 / usuário ativo / mês`** (ou limite máximo de `4.000 tokens` totais por requisição, controlado por cache de hash da carteira por 24h e teto de 5 regenerações manuais/dia). Contrapesa `SM-5`.
- **SM-C3 (Taxa de Correção Manual Pós-Importação B3):** O volume de edições manuais corretivas de posição após importação da B3 deve permanecer **`< 5%`**, garantindo que a evolução visual para o **Kerno** não degrade a precisão do motor de ingestão legado. Contrapesa `SM-1` e `SM-4`.

---

## 11. Open Questions

*Todas as questões abertas da fase de descoberta foram deliberadas e resolvidas com o Product Owner (Alex):*

1. **Checklist Guiado da Nota de Qualidade (1–10) — [RESOLVIDA]:** Confirmada a inclusão de um assistente opcional com 5 critérios objetivos (Governança, Lucro Consistente em 5 anos, Endividamento Controlado, Vantagem Competitiva/Perenidade e Histórico de Proventos/Crescimento, valendo até 2 pontos cada), além do slider direto `1–10` (`FR-5`).
2. **Provedor Default do Modelo de Linguagem (LLM) para o Kerno AI — [RESOLVIDA]:** Confirmado o **Google Gemini** como provedor padrão de produção, integrado via porta agnóstica `AiPortfolioAnalyzerPort` no backend Spring Boot (`FR-9`).
3. **Domínio Oficial Web — [RESOLVIDA]:** Migração direta para `kerno.afsdigital.com.br` sem necessidade de manter redirecionamento do domínio legado (`FR-1`).

---

## 12. Confirmed Decisions & Assumptions Index

*Todas as premissas inicialmente levantadas no modo Fast Path foram revisadas, ajustadas e confirmadas pelo Product Owner:*

- **A-1 [CONFIRMADA COM AJUSTE] (§4.2 / FR-5 e Addendum §1.2 — Faixas de Teto Máximo de Risco por Nota e Isenção de Soberanos/ETFs Amplos):** Para ativos de risco idiossincrático/emissor único (*Ações BR*, *FIIs*, *Stocks/REITs individuais* e *Crédito Privado*), ativos com **Nota de Qualidade (1–10)** entre `8 e 10` possuem **Teto Máximo de Risco** de **`10%`** do patrimônio total; notas entre `5 e 7` possuem teto de **`6%`**; e notas entre `1 e 4` entram em **Quarentena de Aporte** (teto `0%` para novos aportes). Títulos públicos soberanos (*Tesouro Direto*) e ETFs de índices amplos diversificados são isentos do teto individual de `6%`/`10%`, sendo limitados pela meta da própria **Macro Classe**, com redistribuição intra-classe de qualquer excedente de teto entre os demais ativos elegíveis não-saturados.
- **A-2 [CONFIRMADA] (§4.2 / FR-7 e Addendum §1.3 — Fator e Limiar do Multiplicador de Graham):** Ações elegíveis no **Diagrama do Cerrado** que também apresentem **Margem de Segurança de Graham** positiva (limiar padrão de margem mínima igual a `0%`, ou seja, $\text{Cotação Atual} < VI$) recebem bônus multiplicador de `1.30x` (`+30%`) no score de priorização de aporte dentro da classe *Ações BR*, respeitando estritamente o **Teto Máximo de Risco** de `10%` (ou `6%`).
- **A-3 [CONFIRMADA] (§4.3 / FR-9, §10 / SM-C2 e Addendum §2.1 — Cache e Rate Limit por Usuário no Kerno AI):** Cache automático de 24h vinculado ao hash de estado da carteira (`portfolio_state_hash`) e limite de uso justo aplicado diretamente por `user_id` de até `5 gerações/regenerações de IA por dia` (e `30 por mês`) na camada Pro/Founder, enquanto na camada Free pós-trial o **Resumo Executivo** é servido via template determinístico do backend (`0` chamadas externas ao LLM).
- **A-4 [CONFIRMADA] (§4.4 / FR-11, FR-12 e Addendum §4 — Timeout Biométrico Mobile e Proteção de Snapshot em Background):** O aplicativo Mobile exige revalidação biométrica (FaceID/Digital) ao abrir o app ou ao retornar do background após mais de `2 minutos` de inatividade, além de aplicar máscara nativa automática de privacidade no snapshot do *App Switcher* (`AppLifecycleState.inactive` / `paused`).
- **A-5 [CONFIRMADA COM BLINDAGEM CVM] (§4.2 / FR-4 — Nota Inicial Neutra nos Templates de Alocação):** Todos os ativos importados ou cadastrados recebem inicialmente uma **Nota de Qualidade (1–10)** neutra padrão igual a `6` (eliminando qualquer curadoria subjetiva/editorial sob as Resoluções CVM 19 e 20), permitindo funcionamento imediato do **Diagrama do Cerrado** e ajuste pelo investidor via slider ou assistente de 5 critérios objetivos.
- **A-6 [CONFIRMADA] (§4.3 / FR-10 — Normalização Percentual e Sanitização de Ativos Manuais no Prompt do LLM):** O **Sanitizador LGPD de IA** envia ao provedor externo de LLM (**Google Gemini**) apenas os pesos percentuais (`% atual`, `% meta`, `% desvio`), notas e múltiplos públicos, substituindo qualquer nome livre de ativo manual fora da *allowlist* regex de tickers da B3/Tesouro por rótulos canônicos (`RENDA_FIXA_MANUAL_1`, `INTERNACIONAL_MANUAL_1`); valores monetários absolutos em `R$` são exibidos exclusivamente nos badges e cartões determinísticos adjacentes da UI local.
- **A-7 [CONFIRMADA COM AJUSTE] (§4.5 / FR-14, FR-15 — Programa Founders com Acesso Vitalício + Trial PLG de 90 Dias e Downgrade Não-Destrutivo):** Enquanto houver vagas disponíveis no lote inicial configurável (`founder_slots_remaining > 0`, padrão inicial de `250 vagas`), os primeiros usuários recebem atomicamente no cadastro o status de **Membro Founder** (`FOUNDER_LIFETIME`, condicionado à ativação da carteira em até 30 dias) com selo exclusivo e **acesso gratuito vitalício (*Lifetime Free Access*)** a todos os recursos Pro. Para cadastros realizados após o esgotamento do lote de Founders, aplica-se o período de degustação automática (*PLG Trial*) de **`90 dias`** da camada Pro, seguido da transição não-destrutiva para a camada Free básica (mantendo notas em modo somente-leitura e o CPF titular ativo enquanto pausa CPFs secundários).
