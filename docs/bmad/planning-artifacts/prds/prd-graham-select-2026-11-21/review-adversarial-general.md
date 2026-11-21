# Adversarial Review — Kerno (ex-Graham Select 2.0)

## Perspective
Revisor cético e focado em estresse de arquitetura, brechas regulatórias (CVM 19/20), ataques de custos em IA (FinOps), anomalias matemáticas em arredondamento e quantização de cotas, concorrência e condições de corrida em banco de dados, e riscos de aprovação nas lojas móveis (App Store / Google Play).

## Executive Verdict
O documento demonstra robustez excepcional contra os principais vetores de falha técnica, econômica e regulatória. As vulnerabilidades identificadas na primeira iteração (como o risco de concorrência no teto de 250 vagas de Founders, estouro de teto de risco por troco residual de cotas inteiras, evasões lexicais de recomendação e rejeição por IAP de terceiros nas lojas de aplicativos) foram integralmente sanadas com soluções estruturais e testáveis. Restam apenas 2 observações de severidade baixa para monitoramento durante o desenvolvimento.

---

## Stress Analysis & Findings

### 1. Concorrência no Programa Founders e Segurança Transacional
- **Análise:** O teto de 250 vagas do lote inicial de **Membros Founders** (`FR-15`) é decrementado de forma atômica no banco de dados (`founder_slots_remaining`), impedindo condições de corrida onde múltiplos cadastros simultâneos pudessem inflar o lote além do limite contratado.
- **Veredito:** Blindado.

### 2. Aritmética de Alocação e Quantização In-Loop (GPS de Aporte)
- **Análise:** Na formulação anterior, a quantização de ações inteiras fora do loop iterativo gerava distorções onde o saldo não alocado violava o déficit proporcional ou deixava troco desnecessário. A versão atual executa a quantização inteira *dentro* do loop iterativo de alocação (FR-6 / Addendum §1.3), deduzindo apenas o capital consumido pelas cotas e fazendo o troco transbordar deterministicamente para o próximo ativo elegível. Em empates de score $S_i$, o tie-breaker em 4 etapas elimina qualquer indeterminação na ordenação.
- **Veredito:** Blindado.

### 3. FinOps e Vetores de Abuso na Análise de Carteira com IA
- **Análise:** A rota de geração de diagnóstico por IA poderia ser alvo de negação de serviço econômica caso um usuário alterasse minimamente um ativo repetidamente para disparar chamadas externas ao LLM. A mitigação adotada é tripla:
  1. Rate limiting rígido de 5 requisições/dia e 30/mês por `user_id` na camada Pro/Founder.
  2. Cache de 24h ancorado no SHA-256 do estado da carteira (`portfolio_state_hash`).
  3. Para usuários na camada Free pós-trial de 90 dias, o Resumo Executivo é gerado deterministicamente pelo backend (`0` chamadas ao LLM).
- **Veredito:** Blindado.

### 4. Risco de Rejeição nas Lojas Móveis por Mecanismo de Pagamento Externo
- **Análise:** As diretrizes da Apple App Store (Seção 3.1.1) e Google Play proíbem que aplicativos ofereçam funcionalidades digitais pagas redirecionando para checkouts web externos sem oferecer In-App Purchase (IAP). A decisão de diferir o gateway de faturamento para o Ciclo 2.1 e exibir apenas uma tela informativa / lista de espera para contas Free pós-trial (sem botão ou link para checkout externo) neutraliza integralmente o risco de rejeição durante a revisão do aplicativo para publicação nas lojas.
- **Veredito:** Blindado.

### 5. Resolução de Anomalias de Mercado (Circuit Breaker)
- **Análise:** O estado de quarentena por anomalia de cotação (`QUARENTENA_EVENTO_CORPORATIVO` em FR-17) protege o GPS de Aporte contra variações distorcidas por desdobramentos/grupamentos não processados pela fonte de cotação. A inclusão do fluxo de resolução manual pelo usuário através de endpoint dedicado garante que o investidor não fique travado caso a fonte externa atrase a atualização dos dados.
- **Veredito:** Blindado.

---

## Detailed Findings

- **[low]** **Monitoramento de saturação do lote de Founders na Landing Page** (§4.5 / FR-14, FR-15) — Quando o saldo `founder_slots_remaining` atingir 0, a Landing Page deve atualizar seu layout de forma limpa para comunicar o encerramento do lote e a transição para o Trial de 90 dias sem causar inconsistências de expectativa nos visitantes que estiverem com a página aberta em cache local. *Fix:* Assegurar na implementação do frontend que o endpoint da Landing Page consulte o status do lote ou utilize revalidação curta (ex.: cache SSR com revalidação de 60s).
- **[low]** **Comportamento em carteiras com apenas 1 ativo elegível por classe** (§4.2 / FR-5, Addendum §1.2) — No caso raro em que uma Macro Classe contenha apenas 1 ativo cadastrado e sua meta bruta exceda o Teto Máximo de Risco (ex.: 10%), não há outros ativos na mesma classe para absorver o excedente de redistribuição intra-classe. O PRD já prevê que a alocação é limitada pelo teto e o saldo remanescente transborda para a próxima classe (`unallocatedRiskProtectedBalance`), mas vale cobrir este cenário específico na suíte de testes unitários de borda. *Fix:* Incluir caso de teste unitário explícito para classe com ativo único saturando o teto.
