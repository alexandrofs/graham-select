# Validation Report — Kerno (ex-Graham Select 2.0)

- **PRD:** `docs/bmad/planning-artifacts/prds/prd-graham-select-2026-11-21/prd.md`
- **Addendum:** `docs/bmad/planning-artifacts/prds/prd-graham-select-2026-11-21/addendum.md`
- **Rubric:** `.agent/skills/bmad-prd/assets/prd-validation-checklist.md`
- **Run at:** 2026-11-21T15:23:00-03:00
- **Grade:** Excellent (7/7 Rubric Dimensions `strong`; All 17 Findings — 3 Rubric + 14 Adversarial Red-Team — Autofixed & Verified in `prd.md` and `addendum.md`)

## Overall verdict

**Veredito Pós-Remediação (100% Blindado / Decision-Ready):** Todos os **17 apontamentos** levantados na validação (os 3 refinamentos de baixa severidade da Rubrica Estrutural e os 14 apontamentos de *stress-test* do Revisor Adversarial, incluindo os 3 críticos e 6 altos) foram integralmente incorporados ao `prd.md` e ao `addendum.md`.

**Veredito da Rubrica Estrutural (7/7 Strong):** O PRD do **Kerno (Ciclo 2.0)** e seu `addendum.md` constituem uma especificação de produto madura, coesa e pronta para execução (*decision-ready* / `status: final`), articulando com precisão a transição *brownfield* do legado *Graham Select* para um ecossistema Web e Mobile apoiado pelo **Diagrama do Cerrado**, **Margem de Segurança de Graham** e **Kerno AI (Diagnóstico de Carteira)**. Todas as questões abertas e premissas críticas de negócio (`A-1` a `A-7`) foram deliberadas e travadas com critérios de aceite testáveis, restando apenas três refinamentos pontuais de baixa severidade na rubrica (gatilho exato de concessão do status **Membro Founder**, alinhamento da variável de margem mínima no **Multiplicador de Graham** e teto residual da **Macro Classe** na fórmula de *Spillover* quando há ativos em **Quarentena de Aporte**).

**Stress-Test do Revisor Adversarial (Red Team FinTech / CVM / Matemática / FinOps):** A revisão adversarial complementar levantou 14 pontos de atenção de borda (`3 critical`, `6 high`, `4 medium`, `1 low`) para blindar o produto antes ou durante a quebra de histórias: (1) o risco regulatório CVM 19/20 caso o "catálogo base versionado" traga notas qualitativas editoriais pré-julgadas em vez de notas neutras (`6`) ou puramente quantitativas; (2) a aplicação do **Teto Máximo de Risco** (`6%` / `10%`) sobre títulos soberanos do *Tesouro Direto* e ETFs amplos na classe *Internacional*, que travaria carteiras conservadoras ou com 1 único ETF antes de atingir a meta da **Macro Classe**; e (3) a proteção de *Unit Economics* (FinOps) no cruzamento entre `FOUNDER_LIFETIME`, a geração do **Resumo Executivo** na camada Free e a invalidação diária do `portfolio_state_hash`.

## Dimension verdicts
- Decision-readiness — strong
- Substance over theater — strong
- Strategic coherence — strong
- Done-ness clarity — strong
- Scope honesty — strong
- Downstream usability — strong
- Shape fit — strong

## Findings by severity

### Critical (3)

**[Adversarial Review]** — Contradição Regulatória CVM 19/20 no "Catálogo Base Versionado" de Notas de Qualidade 1–10 (§4.2 `FR-4`, §8, Addendum §1.1)
Em `§8. Compliance`, o PRD sustenta a tese jurídica de que o Kerno é apenas uma *"calculadora matemática parametrizada pelo próprio usuário"* que não emite juízo de valor sobre ativos. Contudo, `FR-4` e `Addendum §1.1` determinam que o sistema *"pré-popula notas base de governança e fundamentos (1 a 10) para os principais ativos da B3 a partir de um catálogo base versionado no banco de dados"*. Atribuir editorialmente notas de governança e qualidade (`1` a `10`) a valores mobiliários específicos da B3 — determinando quais entram em **Quarentena de Aporte** ou recebem teto de `10%` — pode configurar Análise/Consultoria de Valores Mobiliários (Resoluções CVM 19 e 20).
Fix: Eliminar curadoria editorial/subjetiva no catálogo base: todas as notas iniciais pré-populadas devem ser uniformemente neutras (`Nota 6` para todos os ativos importados até que o próprio investidor avalie pelo slider ou checklist de 5 critérios) ou pré-calculadas por fórmula quantitativa 100% objetiva baseada em indicadores contábeis públicos com *opt-in* explícito do usuário.

**[Adversarial Review]** — Falha Matemática do Teto Máximo de Risco (`6%` / `10%`) em Renda Fixa, ETFs Internacionais e Carteiras Enxutas (§4.2 `FR-5`, `FR-6`, Addendum §1.2)
`FR-5` e `Addendum §1.2` impõem globalmente o **Teto Máximo de Risco** por ativo sobre o patrimônio total (`10%` para notas `8–10` e `6%` para notas `5–7`). Em um perfil *Conservador Patrimonial* (`45% Renda Fixa / Tesouro`) onde o investidor possui apenas 1 ou 2 títulos (ex.: `Tesouro Selic` e `Tesouro IPCA+`), ou na classe *Internacional* (`10%–15%`) onde aloca via um único ETF amplo (`IVVB11` ou `WRLD11` com nota fallback `6`), a soma dos tetos individuais será inferior à meta da **Macro Classe**, disparando permanentemente a *"Trava de Saturação Global de Tetos de Risco"* (`FR-6`).
Fix: Restringir a trava de **Teto Máximo de Risco** individual (`6%` / `10%`) a ativos de risco idiossincrático/emissor único (*Ações BR*, *FIIs*, *Stocks/REITs individuais* e *Crédito Privado*), isentando (ou limitando ao teto da própria **Macro Classe**) títulos públicos soberanos (*Tesouro Direto*) e ETFs de índices amplos diversificados, além de redistribuir o percentual excedente entre os demais ativos não-saturados da mesma classe.

**[Adversarial Review]** — Passivo Financeiro Perpétuo (*Unit Economics* / FinOps) entre `FOUNDER_LIFETIME`, Camada Free e Custos do Kerno AI (§4.3 `FR-9`, §4.5 `FR-15`, §10 `SM-C2`)
Há três riscos econômicos na modelagem do **Programa Founders** e do **Kerno AI**: (1) `FR-15` concede acesso Pro gratuito vitalício (`FOUNDER_LIFETIME`) sem teto numérico fixo no texto do requisito e sem cláusula de inatividade; (2) na camada **Free** pós-trial de 90 dias, o usuário continua tendo acesso ao cartão **Resumo Executivo** do **Kerno AI**, que consome a mesma chamada de 4 blocos ao Gemini; e (3) o `portfolio_state_hash` inclui as notas `1–10` e a data `YYYY-MM-DD`, permitindo invalidação de cache a cada ajuste de slider.
Fix: (a) Fixar teto padrão do lote Founder (ex.: `100` ou `250` contas) condicionado à conclusão do onboarding (importação B3 ou >= 3 ativos); (b) na camada **Free** pós-trial, servir o **Resumo Executivo** via template determinístico do backend (zero chamadas ao Gemini) ou limitar a 1 geração/mês; e (c) aplicar o rate limit de `5 chamadas/dia` diretamente por `user_id`, independentemente de mutações no `portfolio_state_hash`.

### High (6)

**[Adversarial Review]** — Concentração Monolítica de Aporte no Algoritmo *Greedy Waterfall* do GPS de Aporte (§2.3 `UJ-1`, §4.2 `FR-6`, Addendum §1.3)
O algoritmo em `Addendum §1.3` aloca no ativo líder (1º da fila) o valor $\min(\Delta_i, X_{\text{rem}})$. Em uma carteira de médio porte (ex.: `R$ 250.000,00`), um único ativo com `4,2%` de defasagem possui déficit $\Delta_1 = \text{R\$ } 10.500,00$. Quando Roberto (`UJ-1`) digita `R$ 5.000,00`, 100% do aporte vai para um único ativo, contradizendo `UJ-1` passo 4 (que exibe 3 cartões de ativos).
Fix: Distribuir o aporte destinado à classe prioritária proporcionalmente ao déficit ponderado pelo Score entre os *top-K* ativos elegíveis mais defasados da classe (ex.: até 3 a 4 ativos por aporte), aplicando o *spillover* quando qualquer um atingir seu $\Delta_i$ ou **Teto Máximo de Risco**.

**[Adversarial Review]** — Perda de Liquidez por Truncamento de Cotas Inteiras Fora do Loop de Transbordamento (§4.2 `FR-6`, Addendum §1.3 Passo 5)
Em `Addendum §1.3`, a divisão inteira por cotação ocorre apenas no Passo 5, após o encerramento do loop financeiro. Se o loop alocar valores menores que 1 cota ou gerar sobras parciais em múltiplos ativos, o truncamento pós-loop resultará em trocos residuais fragmentados que somados comprariam cotas inteiras dos ativos seguintes na fila.
Fix: Mover a quantização por cotação ($\lfloor \text{Valor} / \text{Cotação}_i \rfloor \times \text{Cotação}_i$) para dentro do passo iterativo de alocação (Passo 3) para ativos de bolsa, fazendo o troco residual transbordar imediatamente para o próximo ativo elegível.

**[Adversarial Review]** — Ponto Cego de Câmbio (FX) e Quantização nas Macro Classes *Internacional* e *Renda Fixa / Tesouro* (§4.1 `FR-3`, §4.2 `FR-6`, §7.2 `NFR-4`)
`FR-3` exige cadastro manual de ativos de *Renda Fixa / Tesouro* e *Internacional*, mas o PRD não define a moeda/conversão cambial (USD/BRL) para ativos no exterior, a atualização de saldo de Renda Fixa manual nem se *Renda Fixa / Tesouro* opera sem divisão por cotas inteiras no **GPS de Aporte**.
Fix: Especificar que no MVP 2.0 posições de *Renda Fixa / Tesouro* recebem sugestão puramente em valor financeiro (`R$` contínuo, sem cotas inteiras) e que ativos da classe *Internacional* são registrados em `BRL` (BDRs/ETFs na B3 com cotas inteiras, ou saldo manual em `BRL`).

**[Adversarial Review]** — Bypass do *Circuit Breaker* de Anomalias (`FR-17`) no Dia Seguinte (`D+1`) e Distorção do Valor Patrimonial (§4.6 `FR-17`)
No segundo pregão após um desdobramento/grupamento (`D+1`), a variação diária volta a ser normal (< 40%), o que desarmaria um *circuit breaker* baseado apenas na variação diária do pregão. Além disso, suspender apenas o **Multiplicador de Graham** não evita que uma queda artificial de -90% no preço unitário distorça o saldo atual da posição no **Diagrama do Cerrado**.
Fix: Manter o estado de alerta ativo até sincronização/confirmação da posição pelo usuário e, enquanto ativo, congelar a última cotação válida pré-anomalia ou suspender novas sugestões de aporte no ticker no **GPS de Aporte**.

**[Adversarial Review]** — Vetor de Vazamento de PII e *Prompt Injection* via Nomes de Ativos Cadastrados Manualmente (§4.1 `FR-3`, §4.3 `FR-10`, §10 `SM-C1`)
Se o nome de um ativo cadastrado manualmente em `FR-3` aceitar texto livre (ex.: número de conta/CPF ou instrução maliciosa de prompt injection), esse texto poderia ser enviado no payload JSON ao Google Gemini, violando `SM-C1` (0 bytes de PII) e os guardrails CVM 20.
Fix: Determinar em `FR-10` que o **Sanitizador LGPD de IA** substitua qualquer identificador manual fora da *allowlist* regex de tickers B3/Tesouro (`^[A-Z]{4}[0-9]{1,2}$`) por rótulos canônicos anonimizados (ex.: `RENDA_FIXA_MANUAL_1`, `INTERNACIONAL_MANUAL_1`).

**[Adversarial Review]** — Indefinição de Máquina de Estados no *Downgrade* Pós-Trial de 90 Dias para Carteiras Multi-CPF (§4.1 `FR-3`, §4.5 `FR-15`)
`FR-15` bloqueia Multi-CPF e edição de notas na camada **Free** pós-trial de 90 dias, mas não especifica qual CPF permanece visível caso o usuário tenha importado múltiplos CPFs familiares durante o trial.
Fix: Definir em `FR-15` que nenhum dado é deletado no downgrade: o sistema mantém ativo o CPF titular (primeiro CPF cadastrado), pausa a consolidação dos CPFs secundários com um banner explicativo não-destrutivo e torna as **Notas de Qualidade (1–10)** somente-leitura.

### Medium (4)

**[Adversarial Review]** — Fragilidade do Guardrail Pós-Geração CVM 20 sem Ação de Fallback Definida (§4.3 `FR-10`, Addendum §2.1)
`FR-10` exige bloqueio pós-geração de expressões imperativas de compra/venda, mas não define o comportamento do sistema quando uma violação é detectada na resposta do Gemini.
Fix: Especificar que, em caso de violação léxica detectada em qualquer bloco JSON, o sistema não faz retry no LLM: aciona imediatamente o fallback determinístico de `FR-9`, registra log `WARN/ERROR` e não debita a cota diária do usuário.

**[Adversarial Review]** — Exposição Visual de Patrimônio no *App Switcher* do Sistema Operacional Mobile (§2.3 `UJ-1`, §4.4 `FR-11`, `FR-12`)
Ao alternar do Kerno para o app da corretora (`UJ-1`), o iOS e o Android capturam um snapshot da tela para o carrossel de apps recentes, expondo o patrimônio líquido mesmo antes ou depois do timeout biométrico de 2 minutos.
Fix: Adicionar em `FR-11`/`FR-12` a aplicação automática de máscara/blur nativo no snapshot do sistema operacional sempre que o app entrar em `AppLifecycleState.inactive` ou `paused`.

**[Adversarial Review]** — Distinção entre Identidade Visível e Identificadores Internos de Infraestrutura Legada no Rebranding (§4.1 `FR-1`, §11 Questão 3, Addendum §4)
`FR-1` exige ausência do nome legado em configurações enquanto `Addendum §4` referencia `helm/graham-select/`. Renomear diretórios internos de Helm/banco sem necessidade pode causar recriação destrutiva de recursos Kubernetes.
Fix: Explicitar em `FR-1` que a exigência de 100% **Kerno** aplica-se a toda a superfície visível ao usuário, metadados de lojas, títulos de documentos e domínio (`kerno.afsdigital.com.br`), preservando identificadores internos de infraestrutura legada (`helm/graham-select/`).

**[Adversarial Review]** — Inconsistência Visual na Defasagem Percentual entre Patrimônio Atual e Patrimônio Projetado (§4.2 `FR-6`, `FR-8`, Addendum §1.3)
`Addendum §1.3` calcula a participação sobre o patrimônio projetado pós-aporte ($P_{\text{proj}}$), enquanto `FR-8` exibe o desvio entre `% Atual` e `% Meta`. Em carteiras pequenas com aportes grandes, o denominador $P_{\text{proj}}$ reduz visualmente a participação atual exibida.
Fix: Exibir claramente na UI de `FR-8` a `% Atual Pré-Aporte`, a `% Meta Efetiva` e a `% Projetada Pós-Aporte`.

### Low (4)

**[Decision-readiness]** — Definição do evento exato de ativação para concessão do status Membro Founder (§2.3 `UJ-5`, §4.5 `FR-15`)
Em `UJ-5`, Camila recebe o selo **Membro Founder** ativo *"Em menos de 1 minuto após aterrissar na página"* logo após o login Google Sign-In, enquanto `FR-15` menciona *"Usuários que criarem conta e realizarem a ativação de uso no lote inicial de lançamento (controlado por flag/limite configurável de vagas do Programa Founders no backend)"*.
Fix: Explicitar na história de implementação de `FR-15` se a atribuição de `plan_tier = FOUNDER_LIFETIME` ocorre atomicamente no momento da criação da conta (`OAuth2 sign-up` enquanto `founder_slots_remaining > 0`, conforme `UJ-5`) ou após o primeiro marco de onboarding, definindo também o valor padrão inicial da flag de limite de vagas.

**[Done-ness clarity]** — Limiar da margem mínima na elegibilidade ao Multiplicador de Graham (§3 `Glossary`, §4.2 `FR-7`, `Addendum §1.3`)
O Glossário define o **Multiplicador de Graham** para ações que *"apresentem Margem de Segurança de Graham positiva"* (isto é, $\text{Cotação Atual} < VI$), mas `FR-7` e o `Addendum §1.3` mencionam *"cotação atual inferior ou igual ao Valor Intrínseco de Graham ajustado pela margem de segurança mínima"* ($\text{Preço Atual} \le VI \times (1 - \text{Margem de Segurança Mínima})$) sem explicitar o valor percentual dessa margem mínima.
Fix: Explicitar o valor numérico padrão de `Margem de Segurança Mínima` (`0%` por padrão, bastando margem positiva conforme o Glossário) na especificação da história de `FR-7`.

**[Done-ness clarity]** — Teto residual da Macro Classe ($\Delta_C$) na Regra de Transbordamento com ativos em Quarentena de Aporte (§4.2 `FR-5`, `FR-6`, `Addendum §1.2–1.3`)
Se a soma dos déficits individuais dos ativos elegíveis ($\sum \Delta_i$) superar o déficit global remanescente da **Macro Classe** ($\Delta_{C,\text{rem}}$) devido à presença de ativos em **Quarentena de Aporte**, o passo 3 do algoritmo no `Addendum §1.3` poderia alocar na classe um valor superior a $\Delta_C$ antes de transbordar para a próxima **Macro Classe**.
Fix: Incluir $\Delta_{C,\text{rem}}$ no limitador de alocação do passo 3: $\min(\Delta_i, \Delta_{C,\text{rem}}, X_{\text{rem}})$.

**[Adversarial Review]** — Ausência de Critério de Desempate Determinístico (*Tie-Breaker*) no Ordenamento do GPS de Aporte (§4.2 `FR-6`, §4.3 `FR-9`, Addendum §1.3)
Ativos com mesma **Nota de Qualidade (1–10)** e mesmo saldo inicial (ex.: `R$ 0,00`) terão exatamente o mesmo score $S_i$, dependendo da ordem física do banco de dados caso não haja critério de desempate.
Fix: Definir em `Addendum §1.3` a chave secundária determinística de desempate: `1º Maior Nota de Qualidade -> 2º Maior Margem de Segurança de Graham (%) -> 3º Maior déficit monetário absoluto -> 4º Ordem alfabética do Ticker (A-Z)`.

## Mechanical notes
- **Glossary drift:** Aderência excelente aos 18 termos de **§3. Glossary**. Apenas na barra de navegação inferior citada em `UJ-4` (linha 89) e `Addendum §4` (linha 97) aparecem os rótulos curtos de UI **Aporte (GPS)** e **Diagrama** em vez dos nomes completos **GPS de Aporte** e **Diagrama do Cerrado** (compreensível por limite de espaço em `BottomNavigationBar`, já uniformizado em `FR-13`).
- **ID continuity & cross-references:** Sequências 100% contínuas, únicas e sem colisões: `UJ-1` a `UJ-5`, `FR-1` a `FR-17`, `NFR-1` a `NFR-8`, `SM-1` a `SM-6`, `SM-C1` a `SM-C3` e `A-1` a `A-7`. Todas as referências cruzadas entre seções e entre `prd.md` e `addendum.md` resolvem corretamente.
- **Assumptions Index roundtrip:** Todas as 7 decisões e premissas listadas em **§12. Confirmed Decisions & Assumptions Index** (`A-1` a `A-7`) apontam com precisão para as seções correspondentes do PRD e do `addendum.md`.
- **UJ protagonist naming:** Todas as 5 jornadas (`UJ-1` a `UJ-5`) possuem protagonistas nomeados com idade, perfil e contexto inline (`Roberto, 52 anos`; `Lucas, 27 anos`; `Mariana, 38 anos`; `Thiago, 31 anos`; `Camila, 34 anos`), seguidos de `Entry state`, `Path`, `Climax`, `Resolution`, `Edge case` e `Capability mapping`.
- **Required sections present:** Todas as seções exigidas para um PRD *chain-top* de produto B2C regulado (`§0` a `§12` + `addendum.md`) estão presentes e completas.

## Reviewer files
- `review-rubric.md`
- `review-adversarial-general.md`
