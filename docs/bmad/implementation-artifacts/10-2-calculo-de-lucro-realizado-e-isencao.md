---
story_id: "10.2"
story_key: "10-2-calculo-de-lucro-realizado-e-isencao"
baseline_commit: "b3eb35c5275efec1eecda4f64748296bf441e8fe"
---

# Story 10.2: Cálculo de Lucro Realizado e Isenção

Status: done

## Story

Como investidor pessoa física do Graham-Select,
Eu quero ter a apuração mensal automática dos meus lucros e prejuízos realizados na alienação de ativos, o acompanhamento do limite de isenção de R$ 20.000,00 para ações e a apuração do DARF com código 6015,
Para que eu saiba exatamente quando devo pagar tributos à Receita Federal, como compensar prejuízos acumulados e declare corretamente meus ganhos de capital em renda variável sem cometer erros ou cair na malha fina.

## Escopo e Limites (Boundaries)

- **O que ESTÁ incluído nesta história (Story 10.2):**
  1. **Motor de Cálculo Mensal de Ganhos de Capital e Prejuízos no módulo `backend/common`:**
     - **Replay Histórico Multi-Ano Contínuo:** Carregamento de todas as transações do usuário/perfil (`TradePort.findAllByUserId`) desde a primeira operação registrada até 31/12 do ano-calendário consultado.
       - **AVISO CRÍTICO DE CARRYOVER:** NUNCA filtrar transações apenas pelo ano consultado. O cálculo do mês de janeiro de um ano depende obrigatoriamente do saldo de prejuízos acumulados ao final do ano anterior (`31/12/(ano - 1)`) e de eventuais saldos de DARF acumulados menores que R$ 10,00 remanescentes do mês de dezembro anterior.
       - Ordenação determinística das operações: `tradeDate ASC`, `side: COMPRA -> 0, VENDA -> 1`, `createdAt ASC` (ou ID caso nulo).
     - **Rastreamento Contínuo de Preço Médio Ponderado por Ativo:**
       - Normalização obrigatória de fracionários via `TaxTickerUtils.stripFractionalSuffix` (`PETR4F` $\rightarrow$ `PETR4`, `BOVA11F` $\rightarrow$ `BOVA11`).
       - Compras (`COMPRA`): $\text{novoCustoTotal} = \text{custoTotalAnterior} + (\text{qtdCompra} \times \text{precoCompra})$ e $\text{novoPrecoMedio} = \frac{\text{novoCustoTotal}}{\text{novaQtdTotal}}$.
       - Vendas (`VENDA`): reduzem a quantidade mantendo o preço médio unitário vigente inalterado; $\text{novoCustoTotal} = \text{qtdRestante} \times \text{precoMedioVigente}$. Se a quantidade for zerada, custo total e preço médio tornam-se `0.00`.
       - Salvaguarda: se `qtyVendida > qtdCustodia` por inconsistência em extrato importado, limitar o custo ao saldo disponível e registrar warning em log, prevenindo saldos negativos ou divisões por zero.
     - **Apuração de Resultado por Alienação:**
       - $\text{valorAlienacao} = \text{quantidadeVendida} \times \text{precoVenda}$
       - $\text{custoAquisicao} = \text{quantidadeVendida} \times \text{precoMedioVigente}$
       - $\text{resultadoOperacao} = \text{valorAlienacao} - \text{custoAquisicao}$
     - **Agrupamento Mensal (`YearMonth`) e Segregação Estrita das Cestas Fiscais (`TaxCategory`):**
       1. **Cesta de Operações Comuns (Ações, ETFs de Índice e BDRs):**
          - **Ações (`SWING_TRADE_EQUITY`):**
            - Soma total das alienações de ações no mês ($\text{totalAlienacaoAcoes}$).
            - **Regra de Ouro dos R$ 20.000,00 (Art. 59 da IN RFB 1.585/2015):**
              - O limite de R$ 20.000,00 aplica-se estritamente ao somatório do **VALOR TOTAL DAS VENDAS (Alienações)** de Ações no mês (e não ao lucro).
              - Alienações de FIIs, Fiagros, ETFs e BDRs **NÃO** entram na soma dos R$ 20.000,00.
              - **Se $\text{totalAlienacaoAcoes} \le \text{R\$ } 20.000,00$:**
                - Se o resultado líquido de ações for positivo, classifica-se como **Lucro Isento** (`exemptProfit`). Não gera imposto.
                - **REGRA CRÍTICA DE CARRYOVER:** Lucros isentos **NÃO abatem nem consomem** o saldo de prejuízos acumulados anteriores da cesta comum.
                - Se o resultado líquido de ações for negativo (prejuízo), este prejuízo **ACUMULA NORMALMENTE** para a cesta de operações comuns (`accumulatedLossCommon`), podendo compensar lucros tributáveis futuros.
              - **Se $\text{totalAlienacaoAcoes} > \text{R\$ } 20.000,00$:**
                - O resultado líquido positivo é **Lucro Tributável** de ações.
                - Se o resultado for negativo (prejuízo), adiciona-se ao saldo de prejuízos acumulados da cesta comum.
          - **ETFs de Índice de Mercado (`ETF`) e BDRs (`BDR`):**
            - **NÃO gozam de isenção de R$ 20.000,00** (qualquer ganho líquido é tributável).
            - Alíquota de **15%** sobre o ganho líquido.
            - Os resultados integram a **Cesta de Operações Comuns**: seus lucros podem ser compensados com prejuízos de ações/BDRs/ETFs, e seus prejuízos abatem lucros futuros dessa mesma cesta.
          - **Consolidação Mensal da Cesta Comum:**
            - $\text{lucroTributavelLiquidoComum} = \text{lucroTribAcoes} + \text{resultadoEtf} + \text{resultadoBdr}$.
            - Se positivo: compensa com `accumulatedLossCommon` anterior:
              $$\text{compensado} = \min(\text{lucroTributavelLiquidoComum}, \text{accumulatedLossCommon})$$
              $$\text{baseTributavelComum} = \text{lucroTributavelLiquidoComum} - \text{compensado}$$
              $$\text{accumulatedLossCommon} = \text{accumulatedLossCommon} - \text{compensado}$$
              $$\text{impostoDevidoComum} = \text{baseTributavelComum} \times 0.15$$
            - Se negativo: $\text{accumulatedLossCommon} = \text{accumulatedLossCommon} + |\text{resultadoNegativoComum}|$, com $\text{impostoDevidoComum} = 0.00$.
       2. **Cesta de Fundos Imobiliários e Fiagros (`FII_FIAGRO`):**
          - **NÃO gozam de isenção de R$ 20.000,00.**
          - Alíquota fixa de **20%** sobre o ganho líquido.
          - **SEGREGAÇÃO ESTRITA INEGOCIÁVEL:** Prejuízos de FII só compensam lucros de FII. Jamais abatem lucros de ações/ETFs/BDRs, e lucros de FII jamais são compensados por prejuízos da cesta comum.
          - Se o resultado de FII no mês for positivo:
            $$\text{compensadoFii} = \min(\text{lucroFii}, \text{accumulatedLossFii})$$
            $$\text{baseTributavelFii} = \text{lucroFii} - \text{compensadoFii}$$
            $$\text{accumulatedLossFii} = \text{accumulatedLossFii} - \text{compensadoFii}$$
            $$\text{impostoDevidoFii} = \text{baseTributavelFii} \times 0.20$$
          - Se o resultado de FII no mês for negativo:
            $$\text{accumulatedLossFii} = \text{accumulatedLossFii} + |\text{prejuizoFii}|$$, com $\text{impostoDevidoFii} = 0.00$.
     - **Regras da Guia DARF (Código 6015, Vencimento e Limite Mínimo de R$ 10,00):**
       - $\text{impostoDevidoMes} = \text{impostoDevidoComum} + \text{impostoDevidoFii}$.
       - $\text{totalARecolher} = \text{impostoDevidoMes} + \text{darfAcumuladoAnteriorMenor10}$.
       - **Regra do Art. 68 da Lei nº 9.430/1996:**
         - Se $\text{totalARecolher} > 0.00$ e $\text{totalARecolher} < 10.00$:
           - DARF não pode ser recolhido no mês corrente.
           - Status: `ACCUMULATED_BELOW_MINIMUM`.
           - $\text{darfAcumuladoAnteriorMenor10} = \text{totalARecolher}$ transportado para o mês seguinte.
           - Valor da guia do mês: `0.00`.
         - Se $\text{totalARecolher} \ge 10.00$:
           - Status: `DUE`.
           - Código da Receita: `6015`.
           - Valor principal do DARF: $\text{totalARecolher}$.
           - $\text{darfAcumuladoAnteriorMenor10} = 0.00$.
           - Data de vencimento: **Último dia útil do mês subsequente** ao mês de competência da operação, calculado via `DarfDueDateCalculator` considerando fins de semana e feriados nacionais fixos.
         - Se $\text{impostoDevidoMes} == 0.00$ e $\text{darfAcumuladoAnteriorMenor10} == 0.00$:
           - Se houve prejuízo no mês: Status `LOSS`.
           - Se houve apenas lucro isento: Status `EXEMPT`.
           - Sem operações de venda: Status `NO_TAX_DUE`.
  2. **Modelagem de Domínio e Casos de Uso no módulo `backend/common`:**
     - Enum `TaxCategory` (`SWING_TRADE_EQUITY`, `FII_FIAGRO`, `ETF`, `BDR`) com helper `TaxCategory.fromTaxAssetGroup(TaxAssetGroup group)`.
     - Enum `DarfStatus` (`NO_TAX_DUE`, `EXEMPT`, `LOSS`, `ACCUMULATED_BELOW_MINIMUM`, `DUE`).
     - Entidade utilitária `TaxTickerUtils` contendo o método estático `stripFractionalSuffix(String ticker)` reutilizável em todo o sistema.
     - Entidade utilitária `DarfDueDateCalculator` contendo o cálculo do último dia útil do mês seguinte com catálogo de feriados nacionais fixos.
     - Entidades de Domínio:
       - `MonthlyTradeOperation` (record com ticker, tradeDate, side, quantity, price, averagePrice, totalSaleValue, netProfitOrLoss, category).
       - `MonthlyTaxCalculation` (competência `YearMonth`, totalAlienacaoAcoes, isentoAcoes, lucroIsento, lucroTributavelComum, prejuizoCompensadoComum, saldoPrejuizoComum, impostoDevidoComum, lucroTributavelFii, prejuizoCompensadoFii, saldoPrejuizoFii, impostoDevidoFii, impostoDevidoMes, darfStatus, darfValor, darfVencimento, operacoes).
       - `AnnualTaxSummary` (ano, lista dos 12 meses, totalImpostoAno, totalAlienadoAno, totalLucroIsentoAno, totalLucroTributavelAno, saldoPrejuizoComumFinal, saldoPrejuizoFiiFinal).
     - DTOs em `afsdigital.grahamselect.common.tax.application.dto`:
       - `MonthlyTradeOperationDTO`
       - `MonthlyTaxCalculationDTO`
       - `AnnualTaxSummaryDTO`
     - Caso de Uso Desacoplado:
       - `GetTaxMonthlyCapitalGainsUseCase`: gerador da apuração completa com replay contínuo, carryforward mensal e suporte multitenant.
  3. **Isolamento Multi-Tenancy e Suporte a Family Holding:**
     - `userId` mandatoriamente extraído do token JWT autenticado (`jwt.getSubject()`).
     - Parâmetro opcional `profileId`:
       - Se nulo/vazio: processa apenas trades do titular (`trade.getProfileId() == null`).
       - Se informado: valida plano Premium via `PremiumFeatureAccessGuard.canAccessPremiumFeatures()` e pertencimento ativo via `FamilyProfilePort.findActiveByIdAndMasterUserId(...)`. Retorna `403 Forbidden` se não for Premium e `404 Not Found` se o perfil for inválido ou não pertencer ao usuário. Processa apenas trades vinculados ao `profileId`.
  4. **Camada Web e Endpoints REST no módulo `backend/api`:**
     - `GET /api/v1/tax/monthly-capital-gains?year={year}&profileId={profileId}`: retorna JSON com o `AnnualTaxSummaryDTO`.
     - `GET /api/v1/tax/monthly-capital-gains/export?year={year}&profileId={profileId}`: retorna arquivo CSV formatado:
       - UTF-8 com BOM (`\uFEFF`), delimitador `;`, números com vírgula decimal brasileira.
       - Sanitização contra CSV Injection (prefixo `'` se iniciar por `=`, `+`, `-`, `@`).
       - Header: `Content-Disposition: attachment; filename="apuracao_mensal_ganhos_capital_{ano}.csv"`.
       - Duas seções claras: (1) Demonstrativo Consolidado dos 12 Meses; (2) Detalhamento de Alienações do Ano.
     - Registro do bean em `TaxConfiguration.java`.
     - Atualização da especificação OpenAPI em `backend/api/src/main/resources/openapi.yaml`.
  5. **Camada Frontend Flutter na feature `tax`:**
     - Entidades de domínio e modelos:
       - `monthly_trade_operation.dart`, `monthly_tax_calculation.dart`, `annual_tax_summary.dart`.
     - Extensão de `TaxRemoteDataSource` e `TaxRepository` com métodos:
       - `Future<AnnualTaxSummary> getMonthlyCapitalGains({int? year, String? profileId})`
       - `Future<String> exportMonthlyCapitalGainsCsv({int? year, String? profileId})`
     - Atualização do `TaxProvider`:
       - Controle de aba ativa (`enum TaxTab { bensEDireitos, apuracaoMensal }`).
       - Carregamento de `AnnualTaxSummary` com sincronização automática de ano e perfil familiar selecionados entre ambas as abas.
       - Método `exportMonthlyCsv()`.
     - Refatoração de `tax_declaration_page.dart`:
       - `DefaultTabController` no topo com abas:
         - Aba 1: "Bens e Direitos (Informe Anual)" (preserva layout e widgets existentes).
         - Aba 2: "Apuração Mensal & DARF" (novo painel).
     - Componentes visuais em `presentation/widgets/`:
       - `monthly_summary_card.dart`: cards de resumo anual (Total Imposto Devido no Ano, Total Alienado, Saldo de Prejuízos a Compensar Comum e FII).
       - `monthly_tax_card.dart`: card expansível de cada mês contendo:
         - Barra de progresso do limite de R$ 20.000,00 de ações (verde $\le 20k$, amarelo/vermelho $> 20k$).
         - Indicador visual de lucro (verde) ou prejuízo (vermelho) do mês.
         - Card da Guia DARF (código 6015, vencimento formatado `dd/MM/yyyy`, valor e status com badges).
         - Botão "Copiar Dados do DARF" que copia texto formatado para o clipboard.
         - Tabela expansível de alienações realizadas no mês (Data, Ticker, Tipo, Quantidade, Preço Venda, Custo Médio, Lucro/Prejuízo).
  6. **Garantia de Qualidade e Blindagem de Testes Existentes:**
     - Atualização de `MockTaxRepository` em `frontend/test/features/tax/presentation/tax_declaration_page_test.dart` e `tax_provider_test.dart` para implementar os novos métodos da interface.
     - Atualização de `TaxDeclarationControllerTest.java` com mock do novo caso de uso.
     - Logs `@Slf4j` em nível `INFO` nas entradas e `ERROR` com stack trace em exceções conforme `AGENTS.md`.

- **O que NÃO está incluído (fora de escopo desta história):**
  - Pagamento automático ou integração bancária direta de débito de DARF.
  - Day Trade com alíquota segregada de 20% (operações com compra e venda no mesmo pregão são tratadas pela regra geral de swing trade com compras ordenadas antes de vendas).
  - Operações de opções, futuros e derivativos complexos.

## Acceptance Criteria

1. **Replay Histórico Multi-Ano e Preço Médio Ponderado:**
   - **Dado** o histórico de transações de um usuário cadastrado
   - **Quando** o caso de uso `GetTaxMonthlyCapitalGainsUseCase` executa para o ano $Y$
   - **Então** carrega todas as operações de compra e venda desde a primeira data até `31/12/Y`
   - **E** ordena cronologicamente por `tradeDate ASC`, `COMPRA -> 0, VENDA -> 1`, `createdAt ASC`
   - **E** normaliza tickers com `TaxTickerUtils.stripFractionalSuffix`
   - **E** calcula para cada venda: $\text{valorAlienacao} = \text{qtd} \times \text{precoVenda}$, $\text{custoTotal} = \text{qtd} \times \text{precoMedioVigente}$, $\text{resultado} = \text{valorAlienacao} - \text{custoTotal}$
   - **E** atualiza a quantidade em custódia e o custo total remanescente sem alterar o preço médio unitário vigente.

2. **Regra de Ouro de Isenção dos R$ 20.000,00 para Ações (Swing Trade):**
   - **Dado** as operações de alienação de ações (`SWING_TRADE_EQUITY`) em um mês de competência
   - **Quando** o somatório do **valor total das vendas** de ações no mês for $\le \text{R\$ } 20.000,00$
   - **Então** se o resultado líquido de ações for positivo, é classificado como `exemptProfit` (lucro isento) com imposto `R$ 0,00`
   - **E** este lucro isento **NÃO reduz nem consome** o saldo de prejuízos acumulados anteriores da cesta comum
   - **E** se o resultado líquido de ações for negativo (prejuízo), este prejuízo **acumula integralmente** no saldo de prejuízos da cesta comum (`accumulatedLossCommon`) para compensação futura
   - **Quando** o somatório do valor total das vendas de ações no mês for $> \text{R\$ } 20.000,00$
   - **Então** o resultado positivo é classificado como `taxableProfit` sujeito à alíquota de 15% e compensação com prejuízos anteriores.

3. **Tributação Segregada de FIIs e Fiagros:**
   - **Dado** operações de alienação de cotas de FII ou Fiagro
   - **Quando** o resultado do mês for apurado
   - **Então** o limite de R$ 20.000,00 **NÃO se aplica**
   - **E** o ganho líquido é tributado à alíquota de **20%**
   - **E** prejuízos acumulados de FII **somente** compensam lucros de FII, sem qualquer comunicação com ações, ETFs ou BDRs.

4. **Tributação de ETFs de Índice e BDRs:**
   - **Dado** operações de alienação de ETFs ou BDRs
   - **Quando** o resultado do mês for apurado
   - **Então** a isenção de R$ 20.000,00 **NÃO se aplica**
   - **E** o ganho líquido é tributado à alíquota de **15%**
   - **E** os lucros e prejuízos integram a **Cesta de Operações Comuns**, compensando com ações e BDRs.

5. **Compensação Contínua de Prejuízos Acumulados (Loss Carryforward):**
   - **Dado** um saldo de prejuízos acumulados de meses ou anos anteriores
   - **Quando** ocorrer lucro tributável no mês corrente
   - **Então** a compensação ocorre até o limite do lucro tributável do mês na respectiva cesta
   - **E** a base de cálculo é reduzida pelo montante compensado ($\text{base} = \max(0, \text{lucro} - \text{prejuizoAcumulado})$)
   - **E** o saldo residual de prejuízo é transferido para o mês seguinte.

6. **Regras da Guia DARF (Código 6015, Vencimento e Limite de R$ 10,00):**
   - **Dado** o imposto devido consolidado no mês ($\text{impostoDevidoComum} + \text{impostoDevidoFii}$)
   - **Quando** o total acumulado a recolher for $> \text{R\$ } 0,00$ e $< \text{R\$ } 10,00$
   - **Então** o DARF não é emitido (`darfStatus = ACCUMULATED_BELOW_MINIMUM`) e o valor é somado ao saldo acumulado para os meses seguintes
   - **Quando** o valor acumulado atingir ou ultrapassar $\text{R\$ } 10,00$
   - **Então** a guia DARF é liberada (`darfStatus = DUE`) com código `6015`
   - **E** o vencimento é fixado no **último dia útil do mês subsequente** ao mês de competência, considerando fins de semana e feriados nacionais fixos via `DarfDueDateCalculator`
   - **E** o saldo acumulado de DARF $< 10$ é zerado após a emissão.

7. **Isolamento Multi-Tenancy e Holding Familiar:**
   - **Dado** uma chamada aos endpoints `/api/v1/tax/monthly-capital-gains` ou `/export`
   - **Quando** `profileId` não for informado, processa apenas trades com `profileId == null`
   - **Quando** `profileId` for informado, valida acesso Premium via `PremiumFeatureAccessGuard` (retornando 403 se não for Premium) e vínculo via `FamilyProfilePort` (retornando 404 se não for encontrado/ativo) e processa apenas os trades do perfil solicitado.

8. **Exportação de Relatório Mensal em CSV Sanitizado:**
   - **Dado** a chamada a `GET /api/v1/tax/monthly-capital-gains/export`
   - **Então** retorna arquivo CSV com codificação UTF-8 BOM (`\uFEFF`), delimitador `;`, números com vírgula decimal brasileira e proteção contra injeção de fórmulas (prefixando `'` em campos que começam com `=`, `+`, `-`, `@`).

9. **Frontend: Abas, Apuração Mensal Responsiva e Cópia de DARF:**
   - **Dado** o usuário na tela `/tax`
   - **Então** visualiza uma barra de abas (`TabBar`) com "Bens e Direitos (Informe Anual)" e "Apuração Mensal & DARF"
   - **E** a seleção de Ano e Perfil Familiar é sincronizada entre as abas
   - **E** na aba de Apuração Mensal:
     - Visualiza cards de resumo anual com Imposto Total, Volume Alienado e Prejuízos Acumulados (Comum e FII)
     - Visualiza os cards dos 12 meses do ano selecionado
     - Para meses com vendas, exibe barra de progresso dos R$ 20k de ações, lucro/prejuízo, card do DARF com código 6015, vencimento e botão "Copiar Dados do DARF"
     - Expansão de tabela com as vendas do mês
     - Botão funcional "Exportar Apuração Mensal (CSV)".

## Tasks / Subtasks

- [x] Task 1: Modelagem de Domínio e Casos de Uso no Módulo `backend/common` (AC: #1, #2, #3, #4, #5, #6, #7)
  - [x] Criar classe utilitária `TaxTickerUtils` com método público `stripFractionalSuffix(String ticker)` e refatorar `GetTaxAssetDeclarationReportUseCase` para utilizá-la.
  - [x] Criar enum `TaxCategory` (`SWING_TRADE_EQUITY`, `FII_FIAGRO`, `ETF`, `BDR`) com mapeamento a partir de `TaxAssetGroup`.
  - [x] Criar enum `DarfStatus` (`NO_TAX_DUE`, `EXEMPT`, `LOSS`, `ACCUMULATED_BELOW_MINIMUM`, `DUE`).
  - [x] Criar classe utilitária `DarfDueDateCalculator` para calcular o último dia útil do mês subsequente, tratando finais de semana e feriados nacionais fixos (Ano Novo, Tiradentes, Dia do Trabalho, Independência, N. Sra Aparecida, Finados, Proclamação da República, Consciência Negra e Natal).
  - [x] Criar records de domínio:
    - [x] `MonthlyTradeOperation` (ticker, tradeDate, side, quantity, price, averagePrice, totalSaleValue, netProfitOrLoss, category).
    - [x] `MonthlyTaxCalculation` (month, year, totalEquitySales, isEquityExempt, exemptProfit, commonTaxableProfit, commonLossCompensated, commonLossCarryforward, commonTaxDue, fiiTaxableProfit, fiiLossCompensated, fiiLossCarryforward, fiiTaxDue, totalTaxDue, accumulatedTaxUnder10, darfStatus, darfAmount, darfDueDate, operations).
    - [x] `AnnualTaxSummary` (year, monthlyCalculations, totalTaxDueYear, totalSalesYear, totalExemptProfitYear, totalTaxableProfitYear, finalCommonLossCarryforward, finalFiiLossCarryforward).
  - [x] Criar DTOs correspondentes em `afsdigital.grahamselect.common.tax.application.dto` com tipos estritos (`BigDecimal` com escala explícita, sem `double`).
  - [x] Implementar o caso de uso `GetTaxMonthlyCapitalGainsUseCase`:
    - [x] Carregamento de transações multi-ano desde o início do histórico até `31/12` do ano solicitado.
    - [x] Replay cronológico determinístico (`tradeDate ASC, COMPRA -> 0, VENDA -> 1, createdAt ASC`).
    - [x] Manutenção contínua de preço médio por ativo.
    - [x] Agrupamento mensal (`YearMonth`) com apuração segregada da Cesta Comum e da Cesta FII.
    - [x] Aplicação da regra de R$ 20k em ações (lucro isento não consome prejuízos acumulados anteriores; prejuízo em mês $\le 20k$ acumula normalmente).
    - [x] Cálculo da guia DARF (código 6015, limite de R$ 10,00 e vencimento no último dia útil).
    - [x] Suporte a multitenant e Holding Familiar via `userId` e `profileId`.
  - [x] Criar suíte de testes unitários herméticos em `TaxMonthlyCapitalGainsTest.java` cobrindo a matriz completa de cenários:
    - [x] Cenário A: Alienação de ações $\le \text{R\$ } 20.000,00$ com lucro $\rightarrow$ Lucro Isento, imposto zero.
    - [x] Cenário B: Alienação de ações $\le \text{R\$ } 20.000,00$ com lucro quando há prejuízo anterior acumulado $\rightarrow$ Lucro Isento, prejuízo acumulado anterior NÃO é consumido.
    - [x] Cenário C: Alienação de ações $\le \text{R\$ } 20.000,00$ com prejuízo $\rightarrow$ Prejuízo acumula na cesta comum.
    - [x] Cenário D: Alienação de ações $> \text{R\$ } 20.000,00$ com lucro tributável à alíquota de 15%.
    - [x] Cenário E: Compensação de prejuízo anterior da cesta comum sobre lucro tributável de ações/ETFs/BDRs.
    - [x] Cenário F: Alienação de FII com lucro tributado a 20%, sem isenção de R$ 20k.
    - [x] Cenário G: Prejuízo de FII NÃO compensa lucro de ações, e prejuízo de ações NÃO compensa FII.
    - [x] Cenário H: Imposto apurado $< \text{R\$ } 10,00$ acumula para o mês seguinte; mês seguinte atinge $\ge 10,00$ e gera DARF consolidado.
    - [x] Cenário I: Carryover de prejuízo e saldo de DARF atravessando a virada de ano-calendário.
    - [x] Cenário J: Cálculo correto do vencimento do DARF em meses com feriado ou fim de semana no último dia.

- [x] Task 2: Camada Web e Endpoints REST no Módulo `backend/api` (AC: #7, #8)
  - [x] Atualizar `TaxDeclarationController`:
    - [x] `GET /api/v1/tax/monthly-capital-gains` (com parâmetros `year` e `profileId`).
    - [x] `GET /api/v1/tax/monthly-capital-gains/export` (geração de CSV sanitizado com UTF-8 BOM, delimitador `;` e cabeçalhos claros).
    - [x] Injeção de `GetTaxMonthlyCapitalGainsUseCase`.
    - [x] Logs `@Slf4j` em nível `INFO` nas entradas e `ERROR` com stack trace em exceções.
  - [x] Atualizar `TaxConfiguration.java` para registrar o novo caso de uso como Spring Bean.
  - [x] Atualizar `openapi.yaml` com as novas operações e schemas de resposta.
  - [x] Atualizar `TaxDeclarationControllerTest.java` adicionando mock para `GetTaxMonthlyCapitalGainsUseCase` e testes para os novos endpoints (incluindo validação de segurança e exportação CSV).

- [x] Task 3: Camada Frontend Flutter na Feature `tax` (AC: #9)
  - [x] Criar entidades e modelos em `frontend/lib/src/features/tax/domain/entities/`:
    - [x] `monthly_trade_operation.dart`
    - [x] `monthly_tax_calculation.dart`
    - [x] `annual_tax_summary.dart`
  - [x] Atualizar `TaxRemoteDataSource` e `TaxRepository` com métodos `getMonthlyCapitalGains` e `exportMonthlyCapitalGainsCsv`.
  - [x] Atualizar `TaxRepositoryImpl`.
  - [x] Atualizar mocks de teste no Flutter (`MockTaxRepository` em `tax_declaration_page_test.dart` e `tax_provider_test.dart`) implementando os novos métodos.
  - [x] Atualizar `TaxProvider`:
    - [x] Adicionar controle de aba ativa (`enum TaxTab { bensEDireitos, apuracaoMensal }`).
    - [x] Adicionar carregamento de `AnnualTaxSummary` (`fetchMonthlyCapitalGains`).
    - [x] Adicionar exportação de CSV (`exportMonthlyCsv`).
    - [x] Manter sincronização de ano e perfil selecionado entre ambas as abas.
  - [x] Criar widgets em `presentation/widgets/`:
    - [x] `monthly_summary_card.dart`: cards consolidados com imposto devido, volume alienado e prejuízos acumulados.
    - [x] `monthly_tax_card.dart`: card mensal com barra de progresso de R$ 20k, card da Guia DARF (código 6015, vencimento, valor, botão "Copiar Dados do DARF") e tabela expansível de vendas do mês.
  - [x] Refatorar `tax_declaration_page.dart` para incluir `TabBar` e alternar entre "Bens e Direitos" e "Apuração Mensal & DARF".
  - [x] Implementar testes unitários e de widget em `frontend/test/features/tax/` com cobertura completa.

- [x] Task 4: Validação Estática, Testes Automatizados e Documentação (AC: #1-#9)
  - [x] Executar `./mvnw clean test` no backend garantindo 100% de sucesso.
  - [x] Executar `flutter analyze` e `flutter test` no frontend com zero erros e zero warnings.
  - [x] Atualizar o status em `sprint-status.yaml` e sincronizar com `_bmad-output/`.

### Review Findings

- [x] [Review][Patch] Adicionar 31/12 como feriado bancário sem expediente no DarfDueDateCalculator [backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/utils/DarfDueDateCalculator.java:21]
- [x] [Review][Defer] Calendário dinâmico de feriados bancários móveis para vencimento de DARF — deferred, opção 2 escolhida para futuro serviço centralizado de calendário
- [x] [Review][Patch] Sanitização contra Injeção de Fórmulas (CSV Injection) em valores negativos [backend/api/src/main/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationController.java:158]
- [x] [Review][Patch] Race condition no estado de loading compartilhado no TaxProvider [frontend/lib/src/features/tax/presentation/providers/tax_provider.dart:43-46]
- [x] [Review][Patch] Indicador visual de lucro ou prejuízo consolidado do mês no MonthlyTaxCard [frontend/lib/src/features/tax/presentation/widgets/monthly_tax_card.dart:2588-2696]
- [x] [Review][Patch] Definição de Schemas e Componentes de Resposta no OpenAPI [backend/api/src/main/resources/openapi.yaml:814-849]
- [x] [Review][Patch] Validação defensiva do parâmetro 'year' no TaxDeclarationController [backend/api/src/main/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationController.java:43]
- [x] [Review][Patch] Otimização de consultas repetidas a Company no GetTaxMonthlyCapitalGainsUseCase [backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxMonthlyCapitalGainsUseCase.java:85]
- [x] [Review][Patch] Tratamento de profileId vazio/em branco em transações de usuário master [backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxMonthlyCapitalGainsUseCase.java:53-55]
- [x] [Review][Patch] Status de DARF em meses sem operações e sem DARF pendente [backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxMonthlyCapitalGainsUseCase.java:180-182]
- [x] [Review][Patch] Guarda contra preços ou quantidades negativas em operações de compra [backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxMonthlyCapitalGainsUseCase.java:277-279]
- [x] [Review][Patch] Parsing e casting defensivo de JSON no frontend Dart [frontend/lib/src/features/tax/domain/entities/monthly_trade_operation.dart:27]
- [x] [Review][Patch] Exibição de casas decimais em quantidades fracionárias na tabela de operações [frontend/lib/src/features/tax/presentation/widgets/monthly_tax_card.dart:321]
- [x] [Review][Defer] Mecanismo de download de arquivos CSV no Flutter Web via launchUrl(dataURI) [frontend/lib/src/features/tax/presentation/providers/tax_provider.dart:140] — deferred, pre-existing (padrão herdado de Bens e Direitos da Story 10.1)

## Dev Notes

### Arquitetura e Decisões Técnicas

- **Cálculo de Preço Médio e Motor de Carryover Mensal:**
  - Seguir estritamente a IN RFB 1.585/2015.
  - As transações devem ser ordenadas por `tradeDate ASC`, `side: COMPRA -> 0, VENDA -> 1`, `createdAt ASC`.
  - **Snippet de Referência para o Motor Fiscal Mensal:**
    ```java
    public class TaxMonthlyEngine {
        public static class AssetPosition {
            private BigDecimal quantity = BigDecimal.ZERO;
            private BigDecimal totalCost = BigDecimal.ZERO;
            private BigDecimal averagePrice = BigDecimal.ZERO;

            public void buy(BigDecimal qty, BigDecimal price) {
                quantity = quantity.add(qty);
                totalCost = totalCost.add(qty.multiply(price));
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    averagePrice = totalCost.divide(quantity, 4, RoundingMode.HALF_UP);
                }
            }

            public MonthlyTradeOperation sell(Trade trade, TaxCategory category) {
                BigDecimal qty = trade.getQuantity();
                BigDecimal salePrice = trade.getPrice();
                BigDecimal saleValue = qty.multiply(salePrice).setScale(2, RoundingMode.HALF_UP);
                BigDecimal cost = qty.multiply(averagePrice).setScale(2, RoundingMode.HALF_UP);
                BigDecimal result = saleValue.subtract(cost);

                quantity = quantity.subtract(qty);
                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    quantity = BigDecimal.ZERO;
                    totalCost = BigDecimal.ZERO;
                    averagePrice = BigDecimal.ZERO;
                } else {
                    totalCost = quantity.multiply(averagePrice).setScale(2, RoundingMode.HALF_UP);
                }

                return new MonthlyTradeOperation(
                    trade.getTicker(),
                    trade.getTradeDate(),
                    trade.getSide(),
                    qty,
                    salePrice,
                    averagePrice,
                    saleValue,
                    result,
                    category
                );
            }
        }
    }
    ```

- **Calculadora de Vencimento do DARF (`DarfDueDateCalculator`):**
  - O vencimento do DARF código 6015 é o último dia útil do mês subsequente à operação.
  - Se o último dia do mês for sábado ou domingo, recuar para a sexta-feira.
  - Se for feriado nacional bancário/federal, recuar para o dia útil anterior. Feriados fixos considerados:
    - 01/01 (Ano Novo)
    - 21/04 (Tiradentes)
    - 01/05 (Dia do Trabalho)
    - 07/09 (Independência)
    - 12/10 (Nossa Sra Aparecida)
    - 02/11 (Finados)
    - 15/11 (Proclamação da República)
    - 20/11 (Dia da Consciência Negra - Lei Federal 14.759/2023)
    - 25/12 (Natal)

- **Sanitização de CSV Contra Injeção de Fórmulas:**
  ```java
  private String escapeCsvField(String value) {
      if (value == null) return "";
      String sanitized = value;
      if (sanitized.startsWith("=") || sanitized.startsWith("+") || sanitized.startsWith("-") || sanitized.startsWith("@")) {
          sanitized = "'" + sanitized;
      }
      if (sanitized.contains(";") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r")) {
          return "\"" + sanitized.replace("\"", "\"\"") + "\"";
      }
      return sanitized;
  }
  ```

- **Formatador do DARF para Área de Transferência (Frontend):**
  ```dart
  String formatDarfClipboardText(MonthlyTaxCalculation month) {
    return '''
  === GUIA DARF - CÓDIGO 6015 ===
  Período de Apuração: ${DateFormat('MM/yyyy').format(month.monthDate)}
  Código da Receita: 6015
  Data de Vencimento: ${DateFormat('dd/MM/yyyy').format(month.darfDueDate!)}
  Valor do Principal: R\$ ${NumberFormat.currency(locale: 'pt_BR', symbol: '').format(month.darfAmount)}
  Observação: Recolhimento de IRPF sobre Ganhos Líquidos em Operações em Bolsa de Valores.
  ''';
  }
  ```

- **Prevenção de Quebras de Testes Existentes:**
  - `MockTaxRepository` nos testes do Flutter deve implementar:
    ```dart
    @override
    Future<AnnualTaxSummary> getMonthlyCapitalGains({int? year, String? profileId}) async {
      return AnnualTaxSummary.empty(year ?? 2025);
    }

    @override
    Future<String> exportMonthlyCapitalGainsCsv({int? year, String? profileId}) async {
      return 'csv content';
    }
    ```
  - `TaxDeclarationControllerTest.java` deve incluir:
    ```java
    @Mock
    private GetTaxMonthlyCapitalGainsUseCase getTaxMonthlyCapitalGainsUseCase;
    ```

### Project Structure Notes

- Backend:
  - `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/`:
    - `TaxCategory.java`
    - `DarfStatus.java`
    - `MonthlyTradeOperation.java`
    - `MonthlyTaxCalculation.java`
    - `AnnualTaxSummary.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/utils/`:
    - `TaxTickerUtils.java`
    - `DarfDueDateCalculator.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/dto/`:
    - `MonthlyTradeOperationDTO.java`
    - `MonthlyTaxCalculationDTO.java`
    - `AnnualTaxSummaryDTO.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/`:
    - `GetTaxMonthlyCapitalGainsUseCase.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/tax/web/`:
    - `TaxDeclarationController.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/tax/infrastructure/spring/`:
    - `TaxConfiguration.java`
- Frontend:
  - `frontend/lib/src/features/tax/domain/entities/`:
    - `monthly_trade_operation.dart`
    - `monthly_tax_calculation.dart`
    - `annual_tax_summary.dart`
  - `frontend/lib/src/features/tax/presentation/widgets/`:
    - `monthly_tax_card.dart`
    - `monthly_summary_card.dart`

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 10.2: Cálculo de Lucro Realizado e Isenção]
- [Source: docs/bmad/implementation-artifacts/10-1-relatorio-de-bens-e-direitos-informe-anual.md]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/TaxAssetGroup.java]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxAssetDeclarationReportUseCase.java]
- [Source: backend/api/src/main/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationController.java]
- [Source: frontend/lib/src/features/tax/presentation/pages/tax_declaration_page.dart]
- [Source: docs/bmad/project-context.md]
- [Source: AGENTS.md]

## Dev Agent Record

### Agent Model Used

Gemini 3.8 Flash (Medium)

### Debug Log References

### Completion Notes List

- Implementação completa do motor fiscal mensal multi-ano de apuração de ganhos de capital e DARF (Código 6015) em `backend/common`:
  - Replay histórico contínuo desde a primeira operação até 31/12 do ano consultado, preservando saldos acumulados de prejuízos e DARF < R$ 10 através das viradas de ano.
  - Segregação estrita das cestas fiscais: Cesta Comum (Ações com regra de R$ 20.000,00, ETFs a 15%, BDRs a 15%) e Cesta Imobiliária (FIIs e Fiagros a 20%).
  - Regra de ouro da isenção de ações: lucros isentos em vendas <= R$ 20.000,00 NÃO consomem prejuízos acumulados anteriores da cesta comum. Prejuízos em vendas <= R$ 20.000,00 acumulam normalmente.
  - Calculadora de vencimento do DARF (`DarfDueDateCalculator`) no último dia útil do mês subsequente, considerando finais de semana e feriados nacionais fixos.
  - Suporte completo a Multi-Tenancy e perfis da Family Holding.
- Endpoints REST em `backend/api`:
  - `GET /api/v1/tax/monthly-capital-gains`
  - `GET /api/v1/tax/monthly-capital-gains/export` com UTF-8 BOM, delimitador `;` e sanitização contra injeção de fórmulas CSV.
  - Atualização da especificação OpenAPI em `openapi.yaml`.
- Frontend Flutter:
  - Criação de entidades e modelos de apuração mensal.
  - Extensão de `TaxRemoteDataSource`, `TaxRepository` e `TaxProvider` com controle de abas e sincronização de filtros.
  - Criação de `MonthlySummaryCard` e `MonthlyTaxCard` com barra de progresso dos R$ 20k, card da Guia DARF e botão de cópia formatada para o clipboard.
  - Refatoração de `tax_declaration_page.dart` com `TabBar`.
- Validações e testes:
  - 10 cenários de testes unitários herméticos em `TaxMonthlyCapitalGainsTest.java` cobrindo cenários de A a J com 100% de sucesso.
  - 10 testes unitários em `TaxDeclarationControllerTest.java` com 100% de sucesso.
  - Suíte completa do backend reactor Maven passando 100% de sucesso (`common`, `api-service`, `valuation-service`).
  - `flutter analyze` com zero erros e zero warnings.
  - 284 testes unitários e de widget no Flutter passando com 100% de sucesso.

### Change Log

- 2026-11-20: Implementação da História 10.2: Cálculo de Lucro Realizado e Isenção (Backend, Frontend e Testes).

### File List

- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/TaxCategory.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/DarfStatus.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/MonthlyTradeOperation.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/MonthlyTaxCalculation.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/AnnualTaxSummary.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/utils/DarfDueDateCalculator.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/utils/TaxTickerUtils.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/dto/MonthlyTradeOperationDTO.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/dto/MonthlyTaxCalculationDTO.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/dto/AnnualTaxSummaryDTO.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxMonthlyCapitalGainsUseCase.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxAssetDeclarationReportUseCase.java` (UPDATE)
- `backend/common/src/test/java/afsdigital/grahamselect/common/tax/application/usecase/TaxMonthlyCapitalGainsTest.java` (NEW)
- `backend/api/src/main/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationController.java` (UPDATE)
- `backend/api/src/main/java/afsdigital/grahamselect/api/tax/infrastructure/spring/TaxConfiguration.java` (UPDATE)
- `backend/api/src/main/resources/openapi.yaml` (UPDATE)
- `backend/api/src/test/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationControllerTest.java` (UPDATE)
- `frontend/lib/src/features/tax/domain/entities/monthly_trade_operation.dart` (NEW)
- `frontend/lib/src/features/tax/domain/entities/monthly_tax_calculation.dart` (NEW)
- `frontend/lib/src/features/tax/domain/entities/annual_tax_summary.dart` (NEW)
- `frontend/lib/src/features/tax/domain/repositories/tax_repository.dart` (UPDATE)
- `frontend/lib/src/features/tax/data/datasources/tax_remote_data_source.dart` (UPDATE)
- `frontend/lib/src/features/tax/data/repositories/tax_repository_impl.dart` (UPDATE)
- `frontend/lib/src/features/tax/presentation/providers/tax_provider.dart` (UPDATE)
- `frontend/lib/src/features/tax/presentation/widgets/monthly_tax_card.dart` (NEW)
- `frontend/lib/src/features/tax/presentation/widgets/monthly_summary_card.dart` (NEW)
- `frontend/lib/src/features/tax/presentation/pages/tax_declaration_page.dart` (UPDATE)
- `frontend/test/features/tax/presentation/tax_provider_test.dart` (UPDATE)
- `frontend/test/features/tax/presentation/tax_declaration_page_test.dart` (UPDATE)
- `docs/bmad/implementation-artifacts/10-2-calculo-de-lucro-realizado-e-isencao.md` (UPDATE)
- `docs/bmad/implementation-artifacts/sprint-status.yaml` (UPDATE)
