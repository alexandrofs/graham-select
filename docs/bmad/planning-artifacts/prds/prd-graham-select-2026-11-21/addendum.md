# Addendum: PRD Kerno (Ciclo 2.0)

Este adendo preserva especificações matemáticas, decisões arquiteturais e de transporte, matrizes de alternativas descartadas, diretrizes técnicas de Design System e contratos de infraestrutura que sustentam o PRD do **Kerno**, servindo de insumo direto para os documentos de **Arquitetura**, **Especificação de UX** e **Quebra de Épicos/Histórias**.

---

## 1. Especificação Matemática e Algorítmica: Diagrama do Cerrado + Graham

### 1.1 Estrutura Hierárquica de 3 Níveis e Templates Padrão
- **Templates Pré-configurados:**
  - *Perfil Equilibrado Cerrado (Default):* 40% Ações BR, 30% FIIs, 20% Renda Fixa / Tesouro, 10% Internacional.
  - *Perfil Conservador Patrimonial:* 20% Ações BR, 25% FIIs, 45% Renda Fixa / Tesouro, 10% Internacional.
  - *Perfil Acumulação Arrojada:* 50% Ações BR, 25% FIIs, 10% Renda Fixa / Tesouro, 15% Internacional.
  - Para blindar o enquadramento regulatório da plataforma como calculadora parametrizada pelo usuário (Resoluções CVM 19 e 20), o sistema inicializa todos os ativos importados ou cadastrados com uma **Nota de Qualidade (1–10)** neutra padrão igual a `6` (livre de curadoria subjetiva/editorial), totalmente editável pelo usuário via slider ou assistente de 5 critérios objetivos.
- **Hierarquia de Pesos:**
  1. **Nível 1 (Macro Classes):** Meta percentual da carteira ($\text{Meta}_{\text{Classe}}\%$), onde $\sum \text{Meta}_{\text{Classe}}\% = 100\%$.
  2. **Nível 2 (Subclasses / Segmentos):** Agrupador analítico dentro da Macro Classe (ex.: Ações Defensivas/Dividendos vs. Crescimento; FIIs de Tijolo vs. Papel) derivado automaticamente do cadastro setorial do ativo para visualização de exposição no **Dashboard Patrimonial** e no **Kerno AI**, sem impor trava percentual intermediária no cálculo do **GPS de Aporte** no MVP 2.0.
  3. **Nível 3 (Ativo Individual e Nota 1–10):** Cada ativo $i$ pertencente à Macro Classe $C$ recebe uma nota $N_i \in [1, 10]$ atribuída via controle direto (slider) ou assistente opcional de **5 critérios objetivos** (Governança, Lucro Consistente em 5 anos, Endividamento Controlado, Vantagem Competitiva/Perenidade e Histórico de Proventos/Crescimento, valendo até 2 pontos cada).

### 1.2 Mapeamento da Nota (1–10) para Meta Proporcional, Teto Máximo de Risco e Redistribuição Intra-Classe
- **Peso Proporcional Alvo dentro da Macro Classe ($W_{i,\text{bruto}}$):**
  *(Nota: apenas ativos elegíveis com $N_i \ge 5$ e fora de `QUARENTENA_EVENTO_CORPORATIVO` compõem o denominador de distribuição de novos aportes da Macro Classe $C$).*
  $$\text{Meta}_{i,\text{bruta}}\% = \left( \frac{N_i}{\sum_{j \in C, N_j \ge 5} N_j} \right) \times \text{Meta}_{C}\%$$
- **Trava de Teto Máximo de Exposição de Risco ($\text{Teto}_{\text{Risco}}(N_i)$):**
  Para impedir concentração excessiva em emissores individuais quando uma classe possui poucos ativos cadastrados, a meta efetiva de ativos de **risco idiossincrático / emissor único** (*Ações BR*, *FIIs*, *Stocks/REITs individuais* e *Crédito Privado*) na carteira total é limitada pela faixa de nota:
  - **Faixa Alta Qualidade ($N_i \in [8, 10]$):** Teto máximo de **10%** do patrimônio total consolidado.
  - **Faixa Qualidade Moderada ($N_i \in [5, 7]$):** Teto máximo de **6%** do patrimônio total consolidado.
  - **Faixa Quarentena / Risco Elevado ($N_i \in [1, 4]$):** Teto de novos aportes igual a **0%** (bloqueado para novos aportes; a posição existente é mantida em quarentena sem forçar venda).
  - **Isenção para Soberanos e ETFs Amplos:** Títulos públicos soberanos (*Tesouro Direto* na classe *Renda Fixa / Tesouro*) e ETFs de índices amplos diversificados (ex.: `IVVB11`, `WRLD11`, `BOVA11`) são isentos da trava de emissor único (`6%`/`10%`), tendo como teto o próprio limite da **Macro Classe** ($\text{Teto}_{\text{Risco}}(i) = \text{Meta}_C\%$).
  $$\text{Meta}_{i,\text{efetiva}}\% = \min\left(\text{Meta}_{i,\text{bruta}}\%, \text{Teto}_{\text{Risco}}(N_i)\right)$$
- **Redistribuição Intra-Classe de Excedente de Teto:**
  Quando o **Teto Máximo de Risco** limitar a meta bruta de um ou mais ativos ($\text{Meta}_{i,\text{bruta}}\% > \text{Teto}_{\text{Risco}}(N_i)$), o percentual excedente ($\sum (\text{Meta}_{i,\text{bruta}}\% - \text{Teto}_{\text{Risco}}(N_i))$) é redistribuído iterativamente de forma proporcional a $N_j$ entre os demais ativos elegíveis não-saturados da mesma **Macro Classe** $C$ até que $\sum_{i \in C} \text{Meta}_{i,\text{efetiva}}\% = \text{Meta}_C\%$ (ou até que todos os ativos elegíveis da classe atinjam seus respectivos tetos).

### 1.3 Cálculo de Defasagem, Multiplicador de Graham e Algoritmo de Transbordamento (*Spillover*)
- **Entrada:** Valor disponível para aporte no mês ($X > 0$), operando com aritmética decimal estrita (`BigDecimal` / `NUMERIC(19,4)`) em moeda nacional (`BRL` / `R$`).
- **Patrimônio Projetado Pós-Aporte ($P_{\text{proj}}$):**
  $$P_{\text{proj}} = P_{\text{atual}} + X$$
- **Defasagem Monetária ($\Delta_i$ em R$) e Defasagem Percentual Relativa ($G_i$):**
  $$\text{Alvo}_{i}(\text{R\$}) = P_{\text{proj}} \times \text{Meta}_{i,\text{efetiva}}\%$$
  $$\Delta_i = \text{Alvo}_{i}(\text{R\$}) - V_{\text{atual}, i}$$
  $$G_i = \frac{\text{Meta}_{i,\text{efetiva}}\% - \text{Participação}_{\text{proj}, i}\%}{\text{Meta}_{i,\text{efetiva}}\%}$$
  *(Onde $\text{Participação}_{\text{proj}, i}\% = \frac{V_{\text{atual}, i}}{P_{\text{proj}}}$ é utilizada internamente para o cálculo do déficit pós-aporte, enquanto a UI do painel de transparência `FR-8` exibe explicitamente ao usuário a `% Atual Pré-Aporte` $= \frac{V_{\text{atual}, i}}{P_{\text{atual}}}$, a `% Meta Efetiva` $= \text{Meta}_{i,\text{efetiva}}\%$ e a `% Projetada Pós-Aporte` $= \frac{V_{\text{atual}, i} + \text{Aporte}_i}{P_{\text{proj}}}$).*
- **Filtro de Elegibilidade Buy & Hold:**
  - Qualquer classe ou ativo com $\Delta_i \le 0$ (já acima ou igual à meta/teto), com $N_i < 5$ (**Quarentena de Aporte**) ou em estado `QUARENTENA_EVENTO_CORPORATIVO` (`FR-17`) recebe alocação zero ($\text{Aporte}_i = 0$). Nenhuma venda é sugerida.
- **Score de Priorização Combinado ($S_i$) com Multiplicador de Benjamin Graham:**
  - Para ativos da classe **Ações BR** que possuem **Margem de Segurança de Graham** positiva (limiar padrão de `Margem de Segurança Mínima` $= 0\%$, ou seja, $\text{Preço Atual} < VI$, onde $VI = \sqrt{22.5 \times \text{LPA} \times \text{VPA}}$ com $LPA > 0$ e $VPA > 0$), aplica-se um multiplicador de prioridade confirmado $M_{\text{Graham}} = 1.30$ (`+30%` de bônus no score de priorização):
  $$S_i = G_i \times \begin{cases} 1.30 & \text{se Ação BR elegível com } \text{Preço Atual} < VI \\ 1.00 & \text{caso contrário} \end{cases}$$
- **Alocação Iterativa com Transbordamento (*Spillover*) e Quantização In-Loop:**
  1. Ordenam-se as Macro Classes com déficit agregado $\Delta_{C} = \max(0, P_{\text{proj}} \times \text{Meta}_C\% - V_{\text{atual}, C}) > 0$ pela maior defasagem percentual relativa $G_C$, mantendo o controle do teto residual da classe $\Delta_{C,\text{rem}} = \Delta_C$.
  2. Dentro da Macro Classe prioritária, ordenam-se os ativos elegíveis ($\Delta_i > 0$ e $N_i \ge 5$) em ordem decrescente do Score $S_i$. Em caso de empate no Score $S_i$, aplica-se a regra determinística de desempate (*Tie-Breaker*): `1º Maior Nota de Qualidade (Ni) -> 2º Maior Margem de Segurança de Graham (%) -> 3º Maior déficit monetário absoluto (Delta_i) -> 4º Ordem alfabética crescente do Ticker (A-Z)`.
  3. Calcula-se o montante bruto alocável ao ativo $i$ limitado pelo déficit individual, pelo teto residual da **Macro Classe**, pelo espaço até o **Teto Máximo de Risco** e pelo saldo remanescente do aporte:
     $$A_{i,\text{bruto}} = \min\left(\Delta_i, \Delta_{C,\text{rem}}, \text{Espaço até Teto de Risco}_i, X_{\text{rem}}\right)$$
     - Para ativos negociados em bolsa (*Ações BR*, *FIIs*, *ETFs e BDRs*), aplica-se a quantização por cotas inteiras **dentro** do loop:
       $$\text{Qtd}_i = \left\lfloor \frac{A_{i,\text{bruto}}}{\text{Cotação}_i} \right\rfloor, \quad \text{Aporte}_i = \text{Qtd}_i \times \text{Cotação}_i$$
     - Para posições de *Renda Fixa / Tesouro* e ativos manuais não-cotados em bolsa, a alocação opera em valor financeiro contínuo ($\text{Aporte}_i = A_{i,\text{bruto}}$).
     - Atualiza-se imediatamente $X_{\text{rem}} \leftarrow X_{\text{rem}} - \text{Aporte}_i$ e $\Delta_{C,\text{rem}} \leftarrow \Delta_{C,\text{rem}} - \text{Aporte}_i$, fazendo com que o troco residual não consumido por cotas inteiras transborde para o próximo ativo elegível da fila.
  4. Caso sobre saldo ($X_{\text{rem}} > 0$), o valor transborda sucessivamente para os próximos ativos elegíveis da classe (enquanto $\Delta_{C,\text{rem}} > 0$) e, esgotada a classe, para a próxima Macro Classe mais defasada, até que $X_{\text{rem}} = 0$ (ou inferior ao menor preço unitário de cota elegível) ou que todos os ativos elegíveis da carteira atinjam seu respectivo **Teto Máximo de Risco**. Caso ocorra saturação global de todos os tetos com $X_{\text{rem}} > 0$, o saldo protegido é retornado no campo `unallocatedRiskProtectedBalance` e exibido no card *"Saldo Excedente Protegido por Teto de Risco"*.

---

## 2. Arquitetura e Guardrails da Análise de Carteira com IA (*Kerno AI*)

### 2.1 Pipeline Determinístico-Primeiro (*Deterministic-First AI Grounding*)
Para impedir alucinações numéricas de LLMs em finanças pessoais:
1. **Camada Determinística (Backend Java/Spring Boot):** Calcula previamente todos os indicadores exatos em `BigDecimal`: distribuição atual vs. meta do Diagrama do Cerrado, índice de concentração Herfindahl-Hirschman (HHI) ou top-5 ativos, exposição setorial, múltiplos fundamentalistas (P/L, P/VP, Dividend Yield, Valor Intrínseco de Graham e Margem de Segurança) e o plano de aporte calculado.
2. **Camada de Sanitização e Anonimização LGPD (`PiiSanitizer`):** Remove 100% dos identificadores pessoais (nome completo, CPF, e-mail, telefone, IDs internos de conta e números de documentos) e omite valores monetários absolutos em `R$` antes de montar o payload enviado ao provedor de LLM (**Google Gemini**, integrado via porta agnóstica `AiPortfolioAnalyzerPort`). Para blindar contra vazamento de PII e *prompt injection* via nomes livres de ativos manuais (`FR-3`), todos os identificadores de ativos são validados contra uma *allowlist* regex de tickers públicos da B3 (`^[A-Z]{4}[0-9]{1,2}$`) e títulos padronizados do Tesouro Direto; qualquer identificador fora da *allowlist* é substituído por rótulos canônicos anonimizados (`RENDA_FIXA_MANUAL_1`, `INTERNACIONAL_MANUAL_1`). O LLM recebe e responde utilizando apenas esses identificadores canônicos, notas de risco e percentuais (`%`), enquanto os valores em `R$` são exibidos exclusivamente nos badges e cabeçalhos determinísticos adjacentes renderizados localmente pela UI.
3. **Prompt Estruturado e Schema de Saída (JSON Schema):** O LLM (**Google Gemini**) gera exclusivamente a interpretação qualitativa estruturada em 4 blocos:
   - `resumoExecutivo`: Síntese clara e empática da saúde atual da carteira (3 a 4 frases, linguagem acessível tanto para jovens quanto para o público 40+). *(Nota FinOps: para usuários na camada Free pós-trial de 90 dias, este bloco é servido via template determinístico do backend sem chamada externa ao Gemini).*
   - `pontosFortes`: Até 3 destaques positivos baseados nos dados (ex.: diversificação entre classes, desconto fundamentalista).
   - `alertasDeRiscoEConcentracao`: Até 3 desvios críticos (ex.: sobre-exposição a um único setor, ativos em quarentena com nota < 5 acima do teto, ou classe muito defasada).
   - `racionalDoPlanoDeAporte`: Explicação didática do porquê o GPS de Aporte priorizou determinados ativos no mês (combinando distância da meta no Diagrama do Cerrado + Margem de Segurança de Graham).
4. **Validador Pós-Geração e Guardrails CVM (Resolução CVM 20):**
   - Filtro léxico pós-geração (baseado em radicais morfológicos em português como `compr[aeo]|vend[aeo]|adquir[ai]|alien[ae]|garantid[oa]|recomenda`) e instrução de sistema que proíbem linguagem imperativa de ordem de compra/venda ou promessa de rentabilidade futura. Caso qualquer um dos 4 blocos JSON viole o validador, o sistema **não realiza retry no LLM**: aciona imediatamente o fallback determinístico de `FR-9`, registra log `WARN/ERROR` de auditoria regulatória e não debita a cota diária do usuário.
   - Inclusão obrigatória de metadados de auditoria (`timestamp`, versão do modelo, hash do snapshot da carteira e disclaimer regulatório).
5. **Estratégia de Cache e Rate Limiting (Controle de Custos FinOps):**
   - O diagnóstico gerado é persistido e cacheado (*cache-aside*) por hash determinístico do estado da carteira (`portfolio_state_hash`, calculado via SHA-256 sobre: lista ordenada de `ticker_canonico:quantidade_arredondada` + metas de **Macro Classes** + **Notas de Qualidade (1–10)**) com TTL de 24h. O *rate limiting* é controlado diretamente por contador por `user_id` no backend (independente de mutações no hash), com limite de até `5 gerações/regenerações de IA por dia` (e `30 por mês`) por usuário Pro/Founder e teto FinOps de `<= R$ 0,50 / usuário ativo / mês` (`<= 4.000 tokens` totais por requisição).

---

## 3. Diretrizes Técnicas de Design System Intergeracional (Web & Mobile)

- **Paleta e Contraste (Inspiração Nubank / Acessibilidade WCAG 2.1 AA+):**
  - Contraste mínimo de **4.5:1** para texto corrido e **7:1 (AAA)** para valores monetários principais e alertas críticos.
  - Superfícies limpas com espaçamento respirável (`padding` mínimo de `16dp`/`24dp`), evitando tabelas financeiras densas em viewports móveis.
  - Semântica cromática consistente e daltônico-segura (acompanhada de ícones e rótulos textuais, nunca apenas cor): Roxo/Azul Institucional Kerno para ações primárias, Verde Esmeralda para margem de segurança positiva / aportes, Âmbar para atenção/quarentena, Neutros de alto contraste para leitura longa.
- **Tipografia Escalável e Numerais Tabulares:**
  - Família tipográfica `Plus Jakarta Sans` ou `Inter` com `font-variant-numeric: tabular-nums` habilitado em todos os campos monetários e percentuais.
  - Suporte integral a `MediaQuery.textScalerOf(context)` no Flutter até `2.0x` sem truncamento (`TextOverflow.ellipsis` proibido em valores patrimoniais e tickers; os cards devem expandir verticalmente em layout responsivo).
  - Áreas de toque (*touch targets*) mínimas de **48x48dp** para conforto motor de usuários 40+.
- **Modo Privacidade (*Discrete Mode*) e Proteção de Snapshot em Background:**
  - Toggle global no cabeçalho do **Dashboard Patrimonial** (persistido localmente no dispositivo) que mascara instantaneamente saldos em `R$` com `••••••`, mantendo visíveis apenas os percentuais de alocação se o usuário desejar consultar a carteira em ambientes públicos.
  - Sempre que o aplicativo Mobile entrar em `AppLifecycleState.inactive` ou `AppLifecycleState.paused`, aplica-se automaticamente overlay/máscara nativa (`FLAG_SECURE` no Android / snapshot overlay no iOS) para ocultar valores financeiros na miniatura do seletor de aplicativos recentes do sistema operacional (*App Switcher*).

---

## 4. Âncoras Arquiteturais Mobile, Modularidade e Governança Técnica

- **Autenticação e Segurança de Sessão Mobile:**
  - Reuso do fluxo Google Sign-In / OAuth2 + JWT do backend atual.
  - Armazenamento de `access_token` e `refresh_token` exclusivamente via `flutter_secure_storage` (Keychain no iOS / EncryptedSharedPreferences + Keystore no Android).
  - **Gate biométrico local** via `local_auth` (FaceID / TouchID / Biometria Android) ao abrir o app ou retomar do background após timeout confirmado de 2 minutos de inatividade.
- **Domínio Oficial Web e Preservação de Infraestrutura Interna:** Migração direta da superfície pública/web para `kerno.afsdigital.com.br` sem necessidade de redirecionamento legado, preservando identificadores internos de infraestrutura legada (como o caminho do chart `helm/graham-select/` e recursos internos do cluster Kubernetes) para evitar recriação destrutiva.
- **Modularidade Frontend e Backend (Preparação para Gastos e IA Conversacional):**
  - No Flutter: pacotes/módulos de feature desacoplados (`features/portfolio`, `features/cerrado_diagram`, `features/contribution_gps`, `features/ai_analysis`, além dos módulos esqueleto `features/expenses` e `features/ai_copilot` controlados por Feature Flags).
  - `BottomNavigationBar` dinâmica: exibe no MVP as abas **Carteira**, **GPS de Aporte**, **Kerno AI** e **Diagrama do Cerrado**, com contrato de rota preparado para injetar a aba **Gastos** em ciclos futuros sem refatorar a árvore de navegação.
  - Hook de integração futura: o campo de aporte mensal `R$ X` do GPS de Aporte aceita tanto input manual quanto, futuramente, o payload automático de `Saldo Livre Mensal = Entradas - Saídas` vindo do módulo de Controle de Gastos.
- **Governança de Engenharia (Regras Críticas do Repositório `AGENTS.md`):**
  - **Expurgo LGPD (`UserDataPurgeAdapter` & `AccountDeletionIT`):** Todas as novas tabelas criadas para o Ciclo 2.0 (ex.: configurações e metas do Diagrama do Cerrado, notas 1–10 por ativo do usuário, histórico de simulações de aporte e snapshots/diagnósticos gerados pelo *Kerno AI*) DEVEM ser integradas ao `UserDataPurgeAdapter` e verificadas no teste de integração `AccountDeletionIT`.
  - **Imutabilidade de Migrations Liquibase:** Novas tabelas e colunas do Diagrama do Cerrado e do Kerno AI devem ser adicionadas exclusivamente via novos changesets incrementais e idempotentes.
  - **Observabilidade Obrigatória:** Logs contextuais em todas as entradas de API REST e workers assíncronos; logs `ERROR` com stack trace em todas as capturas/lançamentos de exceções (incluindo falhas ou timeouts na chamada ao provedor de LLM).
  - **Paridade Helm e CI/CD:** Novas variáveis de ambiente (ex.: chaves/endpoints do provedor de IA, timeouts, feature flags e limites de rate limit do Kerno AI) devem ser mapeadas em `application.yml`, `helm/graham-select/values.yaml`, `configmap.yaml`, `secret.yaml` e `.github/workflows/deploy.yml`.

---

## 5. Racional de Rebranding e Matriz de Alternativas Descartadas

| Candidato | Avaliação Estratégica | Veredito |
| :--- | :--- | :--- |
| **Kerno** *(Escolhido)* | Do esperanto/raiz semântica para "núcleo/essência". Curto (5 letras, 2 sílabas), neutro (não restringe o produto à bolsa ou a uma única fórmula), pronúncia imediata para todas as idades, livre de colisão relevante no mercado financeiro BR e excelente sonoridade para extensões de IA (*Kerno AI*, *Kerno Insights*). | **Aprovado (Marca Oficial 2.0)** |
| **Aura** | Forte apelo para assistente de IA, porém descartado após levantamento de mercado demonstrar alta saturação no Brasil em finanças e IA (*Aura Financeira*, *Aura Lembretes* no WhatsApp, *GetAura*, *Aura Finance* na Play Store), gerando alto risco de confusão de marca, disputa de SEO e colisão no INPI. | Descartado |
| **Aloca / Cerrado Invest** | Comunicam bem a etapa de rebalanceamento, mas prendem a marca apenas a investimentos, inviabilizando a expansão planejada para controle de gastos pessoais e orçamento familiar. | Descartado |
| **Graham Select** *(Legado)* | Excelente para o MVP 1.0 focado em *value investing* clássico, mas restritivo para um ecossistema amplo de patrimônio, multi-classes de ativos, controle de gastos e IA. | Substituído por **Kerno** (mantendo *Graham* como nome do motor fundamentalista interno de ações) |
| **Lumina / Plano / Nexus / Valora / Synap** | Avaliados na fase de ideação; descartados por menor diferenciação fonética ou excesso de uso genérico em SaaS B2B. | Descartados |
