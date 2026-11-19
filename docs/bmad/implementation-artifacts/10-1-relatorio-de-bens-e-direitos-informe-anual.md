---
story_id: "10.1"
story_key: "10-1-relatorio-de-bens-e-direitos-informe-anual"
baseline_commit: "1d49faa1024eb5b8e71de50fa1a64d48c441ac79"
---

# Story 10.1: Relatório de Bens e Direitos (Informe Anual)

Status: review

## Story

Como investidor pessoa física do Graham-Select,
Eu quero gerar um relatório anual consolidado de "Bens e Direitos" com a posição de custódia e custo de aquisição em 31/12 de um ano-calendário selecionado,
Para que eu possa preencher de forma ágil, precisa e em conformidade com as regras da Receita Federal do Brasil a ficha de "Bens e Direitos" da minha Declaração de Ajuste Anual do IRPF (ou dos meus dependentes na Holding Familiar).

## Escopo e Limites (Boundaries)

- **O que ESTÁ incluído nesta história (Story 10.1):**
  1. Criação do script de migração Liquibase `backend/common/src/main/resources/db/changelog/34-add-cnpj-to-company.yaml` e sua inclusão em `db.changelog-master.yaml`:
     - Adição da coluna `cnpj` (`VARCHAR(20)`) na tabela `company`, com índice de busca `idx_company_cnpj` e respectivo bloco de rollback.
     - Carga inicial (seed/update) dos CNPJs conhecidos das principais empresas já semeadas no banco de dados (ex: `PETR4: 33.000.167/0001-01`, `VALE3: 33.592.510/0001-54`, `ITUB4: 60.872.504/0001-23`, `BBDC4: 60.746.948/0001-12`, `BBAS3: 00.000.000/0001-91`).
     - Atualização das entidades `Company` (`backend/common`) e `CompanyEntity` (`backend/api`) para mapear o campo `cnpj`.
  2. Implementação do motor de cálculo de custo médio e snapshot patrimonial em 31/12 no módulo `backend/common`:
     - Normalização mandatória de tickers via `stripFractionalSuffix` (remover 'F' terminal de ativos da B3 de 5-6 caracteres, ex: `PETR4F` $\rightarrow$ `PETR4`, `BOVA11F` $\rightarrow$ `BOVA11`) antes de qualquer agrupamento, garantindo consolidação em um único ativo.
     - Replay cronológico determinístico das transações de compras (`COMPRA`) e vendas (`VENDA`) até 31/12 do ano anterior (`ano - 1`) e até 31/12 do ano selecionado (`ano`), ordenadas por `tradeDate ASC`, depois por `createdAt ASC` (ou ID).
     - Aplicação estrita das regras da Receita Federal (IN RFB 1.585/2015 e atualizações): o valor patrimonial a declarar é **SEMPRE o Custo de Aquisição** (Preço Médio Ponderado pelas compras $\times$ Quantidade em custódia), e **NUNCA** o valor de mercado/cotação atual.
     - Regra de Venda: a venda reduz a quantidade e o custo total proporcionalmente ($\text{custoTotal} = \text{quantidadeRestante} \times \text{precoMedioVigente}$), mantendo inalterado o preço médio ponderado anterior.
     - Regra de Liquidação Total: se a quantidade for zerada, o custo total passa a ser `R$ 0,00` e o preço médio `R$ 0,00`.
     - Regra de Nova Compra: nova compra adiciona quantidade e valor ao custo total existente ($\text{novoCustoTotal} = \text{custoTotalAnterior} + (\text{qtdCompra} \times \text{precoCompra})$) e recalcula $\text{novoPrecoMedio} = \frac{\text{novoCustoTotal}}{\text{novaQuantidadeTotal}}$.
     - Proteção contra divisão por zero: se a quantidade for zero ou negativa, preço médio e custo total devem ser `BigDecimal.ZERO`.
     - Regra de Ativos Alienados no Ano: se um ativo possuía quantidade $> 0$ em 31/12 do ano anterior e foi totalmente alienado durante o ano (quantidade em 31/12 do ano $= 0$), ele **DEVE** constar no relatório anual com "Situação em 31/12 do ano anterior" preenchida e "Situação em 31/12 do ano atual" igual a `R$ 0,00`, conforme exigência da Receita Federal para comprovação de alienação.
     - Ativos com quantidade e custo zero em ambas as datas (31/12 anterior e 31/12 atual) não aparecem no relatório de Bens e Direitos (pertencem à apuração de ganho de capital/DARF na Story 10.2).
     - Ordenação determinística do relatório: os itens consolidados devem ser ordenados primariamente pelo código do Grupo DIRPF (`groupCode ASC`) e secundariamente pelo ticker (`ticker ASC`).
  3. Classificação Tributária Automática (Grupos e Códigos DIRPF):
     - Mapeamento determinístico de Grupo e Código da Receita Federal:
       - **Ações:** Grupo 03 (Participações Societárias), Código 01 (Ações inclusive as listadas em bolsa).
       - **FIIs:** Grupo 07 (Fundos), Código 03 (Fundos de Investimento Imobiliário).
       - **Fiagros:** Grupo 07 (Fundos), Código 04 (Fundos de Investimento nas Cadeias Produtivas Agroindustriais).
       - **BDRs:** Grupo 04 (Aplicações e Investimentos), Código 04 (Ativos negociados no exterior) ou Grupo 03 Código 04.
       - **ETFs de Índice:** Grupo 07 (Fundos), Código 09 (Demais fundos de índice de mercado).
     - Heurística robusta de fallback quando `Company.assetClass` for nulo: derivação automática pelo ticker normalizado (ex: finais 11 identificados como FII/ETF, 3/4/5/6 como Ações, 31/32/33/34/35 como BDRs).
  4. Geração Padronizada do Texto de Discriminação para DIRPF:
     - Geração automática e padronizada do texto para o campo "Discriminação":
       - Ativo com posição mantida em 31/12:
         `"{quantidade} {tipo_ativo} de {razao_social} ({ticker}), CNPJ: {cnpj}. {texto_custodia}. Custo total de aquisição em 31/12/{ano}: R$ {custo_total} (Preço médio de aquisição: R$ {preco_medio})."`
       - Ativo totalmente alienado no ano:
         `"Posição de {quantidade_anterior} {tipo_ativo} de {razao_social} ({ticker}), CNPJ: {cnpj}, {texto_custodia}, totalmente alienada durante o ano-calendário de {ano}. Custo total de aquisição em 31/12/{ano}: R$ 0,00."`
     - Regra determinística para `{texto_custodia}`:
       - 1 corretora: `"Custodiado na corretora {corretora}"`
       - 2+ corretoras distintas: `"Custodiado nas corretoras: {corretora1}, {corretora2}"` (em ordem alfabética)
       - Nenhuma corretora informada/nula: `"Custodiado em corretora de valores"`
     - Tratamento para CNPJ não cadastrado: `"CNPJ não informado"`.
  5. Camada de Domínio, DTOs e Casos de Uso no módulo `backend/common`:
     - Entidades / Records: `TaxAssetDeclarationItem`, `TaxAssetDeclarationReport`, `TaxAssetGroup`.
     - DTOs com tipagem estrita e escala monetária/quantidade: `TaxAssetDeclarationReportDTO`, `TaxAssetDeclarationItemDTO`, `TaxAvailableYearsDTO`.
     - Casos de uso desacoplados (POJO puro, Command Pattern com `execute(...)`):
       - `GetTaxAssetDeclarationReportUseCase`: gera o relatório completo de bens e direitos para o ano e usuário/perfil informados, calculando totais consolidados e subtotais por grupo, com busca de empresas otimizada (lookup em lote ou cache em memória).
       - `GetTaxAvailableYearsUseCase`: lista os anos-calendário disponíveis para seleção com base nas datas das transações do usuário.
  6. Isolamento Multi-Tenancy e Suporte a Family Holding:
     - O `userId` é mandatoriamente extraído do token JWT autenticado via `SecurityContextHolder`.
     - Parâmetro opcional `profileId`: se informado, validação prévia de assinatura Premium via `PremiumFeatureAccessGuard.canAccessPremiumFeatures()` e pertencimento à conta matriz via `FamilyProfilePort.findActiveByIdAndMasterUserId(...)`.
     - Se `profileId` não for informado, processa estritamente os trades da conta matriz do titular (`trade.getProfileId() == null`).
     - Se `profileId` for informado, processa estritamente os trades associados àquele dependente (`trade.getProfileId().equals(profileId)`).
  7. Camada Web e Endpoints REST no módulo `backend/api`:
     - `GET /api/v1/tax/bens-e-direitos`: retorna o relatório estruturado em JSON para o ano-calendário especificado (`year`, default ano anterior) e perfil opcional (`profileId`).
     - `GET /api/v1/tax/bens-e-direitos/export`: exporta o relatório em formato CSV com:
       - Codificação UTF-8 com BOM (`\uFEFF`) para compatibilidade nativa com Excel em português.
       - Delimitador ponto e vírgula (`;`).
       - Formatação numérica brasileira (vírgula decimal) e texto de discriminação protegido por aspas duplas escapadas.
       - Cabeçalho: `Grupo;Código;Localização;CNPJ;Ticker;Ativo;Quantidade;Preço Médio;Situação 31/12/{Ano-1};Situação 31/12/{Ano};Discriminação`
     - `GET /api/v1/tax/available-years`: retorna os anos em que há movimentação registrada para preenchimento dos filtros de UI.
     - Atualização do contrato OpenAPI 3.0 em `backend/api/src/main/resources/openapi.yaml` com tag `Tax` e configuração do `maven-antrun-plugin` em `backend/api/pom.xml` para exclusão de stubs gerados (`TaxApi.java`, etc.).
  8. Camada Frontend Flutter:
     - Criação da feature `frontend/lib/src/features/tax`:
       - `data/datasources/tax_remote_data_source.dart` (usando `ApiClient`)
       - `data/repositories/tax_repository_impl.dart`
       - `domain/entities/tax_declaration_report.dart` e `tax_declaration_item.dart`
       - `domain/repositories/tax_repository.dart`
       - `presentation/providers/tax_provider.dart` (gerenciamento de estado, seleção de ano, seleção de dependente/perfil, feedback de carregamento).
       - `presentation/pages/tax_declaration_page.dart`
       - `presentation/widgets/tax_summary_card.dart` e `presentation/widgets/tax_item_card.dart`.
     - Botão "Copiar Discriminação" com feedback imediato via `Clipboard.setData` e `SnackBar` amigável.
     - Botão "Exportar CSV" para download do informe com suporte transparente a Web e Mobile (disparo de streaming de bytes ou abertura via `url_launcher`/blob).
     - Registro da rota `/tax` no `GoRouter` e item no menu lateral (`_Sidebar`) e barra inferior (`_BottomNavBar`) em `main_layout.dart`.
  9. Conformidade Estrita com `AGENTS.md` e `project-context.md`:
     - Logs `@Slf4j` em nível `INFO` nas entradas de controllers e use cases com parâmetros contextuais.
     - Tratamento robusto de exceções com captura e logs em nível `ERROR` acompanhados da stack trace completa (`log.error("...", e)`).
     - Uso exclusivo de `BigDecimal` com escala e arredondamento explícito (`RoundingMode.HALF_EVEN` / `HALF_UP`) para todos os valores numéricos de dinheiro e quantidade.
     - Testes unitários herméticos no backend (`TaxUseCasesTest`) e testes de widget/provider no Flutter com 0 erros/warnings.

- **O que NÃO está incluído (fora de escopo desta história):**
  - Apuração mensal de lucros/prejuízos, cálculo de isenção de R$ 20k em ações e geração de DARF de renda variável (escopo exclusivo da **Story 10.2**).
  - Informes de rendimentos de dividendos/JCP pagos e retidos na fonte (Fichas "Rendimentos Isentos e Não Tributáveis" e "Rendimentos Sujeitos à Tributação Exclusiva/Definitiva") — previsto como evolução ou story subsequente.
  - Integração direta com a API do programa IRPF da Receita Federal (RFB não disponibiliza API pública aberta de envio de declarações para pessoas físicas).

## Acceptance Criteria

1. **Migração de Banco de Dados com Coluna CNPJ e Seed em Company (`NFR4`, `NFR6`):**
   - **Dado** o processo de inicialização do banco de dados com Liquibase
   - **Quando** o changeset `34-add-cnpj-to-company.yaml` for executado
   - **Então** a coluna `cnpj` (`VARCHAR(20)`) deve ser adicionada à tabela `company` permitindo valores nulos
   - **E** um índice `idx_company_cnpj` deve ser criado na tabela `company`
   - **E** o changeset deve popular os CNPJs das principais empresas cadastradas no sistema (`PETR4`, `VALE3`, `ITUB4`, `BBDC4`, `BBAS3`)
   - **E** o changeset deve possuir bloco de `rollback` válido que reverta as alterações
   - **E** as classes `Company` (módulo `common`) e `CompanyEntity` (módulo `api`) devem refletir o campo `cnpj`.

2. **Cálculo Determinístico de Custo Médio e Posições em 31/12 (Regras da Receita Federal):**
   - **Dado** o histórico de transações de um usuário (compras e vendas)
   - **Quando** o caso de uso `GetTaxAssetDeclarationReportUseCase` calcula a posição para o ano-calendário $Y$
   - **Então** aplica `stripFractionalSuffix` a cada ticker, agrupando `XXXX3F` com `XXXX3` e `XXXX11F` com `XXXX11`
   - **E** considera apenas as transações com `tradeDate <= 31/12/Y`, ordenadas cronologicamente por `tradeDate ASC`, depois por `createdAt ASC`
   - **E** para cada ativo:
     - Uma operação de `COMPRA` incrementa a quantidade e adiciona `quantidade * preco` ao custo total de aquisição, recalculando o preço médio ponderado: $\text{Preço Médio} = \frac{\text{Custo Total}}{\text{Quantidade}}$
     - Uma operação de `VENDA` diminui a quantidade e subtrai do custo total o valor de `quantidade_vendida * preco_medio_vigente`, mantendo o preço médio ponderado unitário inalterado
     - Se a quantidade for zerada, o custo total passa a ser `0.00` e o preço médio `0.00`
     - Divisões por zero são estritamente prevenidas
   - **E** o saldo de "Situação em 31/12/(Y-1)" reflete a posição acumulada até 31/12 do ano anterior
   - **E** o saldo de "Situação em 31/12/Y" reflete a posição acumulada até 31/12 do ano selecionado
   - **E** o valor informado é **SEMPRE o Custo de Aquisição** (e NUNCA a cotação de mercado do ativo)
   - **E** a lista resultante de itens é ordenada por `groupCode ASC, ticker ASC`.

3. **Inclusão Obrigatória de Ativos Alienados Durante o Ano-Base:**
   - **Dado** que o investidor possuía ações de um ativo em 31/12/(Y-1) ($\text{quantidade} > 0$)
   - **E** realizou a venda total desse ativo durante o ano $Y$ ($\text{quantidade em 31/12/Y} = 0$)
   - **Quando** o relatório anual de Bens e Direitos para o ano $Y$ é gerado
   - **Então** o item desse ativo **DEVE** constar no relatório anual
   - **E** o campo "Situação em 31/12/(Y-1)" deve exibir o custo de aquisição acumulado anterior
   - **E** o campo "Situação em 31/12/Y" deve exibir exatamente `0.00`
   - **E** a discriminação deve indicar que a posição foi alienada/zerada durante o ano-base.

4. **Classificação Tributária (Grupo e Código DIRPF) e Discriminação Padronizada:**
   - **Dado** um ativo listado no relatório
   - **Quando** suas informações fiscais são montadas
   - **Então** o sistema atribui:
     - Ações: Grupo 03, Código 01
     - Fundos Imobiliários (FII): Grupo 07, Código 03
     - Fiagros: Grupo 07, Código 04
     - BDRs: Grupo 04, Código 04 (ou Grupo 03, Código 04)
     - ETFs: Grupo 07, Código 09
   - **E** o texto de "Discriminação" gerado deve conter:
     - Quantidade de cotas/ações em 31/12
     - Tipo de ativo, nome da empresa ou fundo, e ticker entre parênteses
     - CNPJ do emissor (ou "CNPJ não informado" se não cadastrado)
     - Detalhamento de custódia segundo a regra determinística (1 corretora, 2+ corretoras ordenadas alfabeticamente ou fallback)
     - Custo total de aquisição em 31/12 do ano-base e o Preço Médio ponderado calculado
   - **E** a API deve retornar o texto limpo e pronto para preenchimento na DIRPF.

5. **Isolamento de Tenant, Restrição de Tier e Holding Familiar (`FR4`, `NFR4`, `NFR6`):**
   - **Dado** uma requisição para emissão do relatório de bens e direitos
   - **Quando** `userId` é extraído do token JWT autenticado
   - **Então** o relatório só computa operações pertencentes a esse `userId`
   - **E** se o parâmetro `profileId` for fornecido:
     - O sistema valida que o usuário possui assinatura Premium ativa via `PremiumFeatureAccessGuard.canAccessPremiumFeatures()`
     - Caso não seja Premium, retorna HTTP `403 Forbidden`
     - O sistema valida que `profileId` é um UUID válido, existe, está ativo e pertence ao `userId` autenticado via `FamilyProfilePort.findActiveByIdAndMasterUserId(...)`
     - Caso o perfil seja inválido ou não pertença ao usuário, retorna HTTP `404 Not Found`
     - As posições computadas refletem estritamente as operações vinculadas a esse `profileId`.

6. **Endpoints da API REST e Exportação CSV (Excel Brasil):**
   - **Dado** um usuário autenticado
   - **Quando** chama `GET /api/v1/tax/bens-e-direitos?year=2025`
   - **Então** responde com HTTP `200 OK` contendo o payload JSON com metadados do relatório (ano, totais 31/12 anterior e atual, variação patrimonial, lista de itens agrupados ordenados por `groupCode` e `ticker`)
   - **Quando** chama `GET /api/v1/tax/bens-e-direitos/export?year=2025`
   - **Então** responde com HTTP `200 OK`, `Content-Type: text/csv; charset=UTF-8`, header `Content-Disposition: attachment; filename="informe_bens_direitos_2025.csv"`
   - **E** o arquivo CSV inicia com o prefixo UTF-8 BOM (`\uFEFF`), utiliza `;` como delimitador e formata decimais com vírgula para compatibilidade nativa com Excel no Brasil
   - **E** o arquivo CSV contém as colunas: `Grupo;Código;Localização;CNPJ;Ticker;Ativo;Quantidade;Preço Médio;Situação 31/12/{Ano-1};Situação 31/12/{Ano};Discriminação`
   - **Quando** chama `GET /api/v1/tax/available-years`
   - **Então** retorna a lista ordenada decrescente dos anos em que há operações (ex: `[2025, 2024, 2023]`).

7. **Interface Frontend Flutter Fluida e Acessível (`NFR2`, `WCAG`):**
   - **Dado** o usuário autenticado na tela `/tax` ("Informe de Bens e Direitos")
   - **Quando** a tela é carregada
   - **Então** exibe um seletor dropdown com os anos disponíveis (default ano anterior)
   - **E** se o usuário possuir perfis na Holding Familiar, exibe seletor de perfil (Titular vs Dependentes)
   - **E** exibe cartões de resumo patrimonial no topo com Total em 31/12 do ano anterior, Total em 31/12 do ano atual e Variação Patrimonial R$ e %
   - **E** exibe a listagem dos ativos agrupados ou categorizados por Grupo (Ações, FIIs, BDRs, etc.)
   - **E** cada item possui botão de ação rápida "Copiar Discriminação" que transfere o texto para a área de transferência do dispositivo e exibe `SnackBar` de confirmação
   - **E** possui botão "Exportar CSV" que efetua o download do arquivo com suporte Web e Mobile
   - **E** em caso de carteira vazia para o ano, exibe estado vazio amigável ("Nenhum bem ou direito registrado para o ano {ano}").

## Tasks / Subtasks

- [x] Task 1: Migração de Banco de Dados e Atualização de Entidades (AC: 1)
  - [x] Criar changeset Liquibase `backend/common/src/main/resources/db/changelog/34-add-cnpj-to-company.yaml` com:
    - [x] Adição da coluna `cnpj VARCHAR(20)` e índice `idx_company_cnpj`.
    - [x] Updates de seed para popular CNPJs das empresas padrão (`PETR4`, `VALE3`, `ITUB4`, `BBDC4`, `BBAS3`).
    - [x] Bloco de rollback válido.
  - [x] Registrar changeset 34 no `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`.
  - [x] Atualizar entidade de domínio `Company` em `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/Company.java` para incluir `cnpj`.
  - [x] Atualizar entidade JPA `CompanyEntity` em `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/CompanyEntity.java`.
  - [x] Atualizar `CompanyJpaAdapter` para persistir e mapear o campo `cnpj`.

- [x] Task 2: Modelagem de Domínio e Casos de Uso no Módulo Common (AC: 2, 3, 4, 5)
  - [x] Criar pacote `afsdigital.grahamselect.common.tax.domain.entities` com as classes/records:
    - [x] `TaxAssetGroup` (enum com código, grupo e descrição da Receita Federal)
    - [x] `TaxAssetDeclarationItem` (dados completos do item: ticker, nome, cnpj, grupo, código, quantidade, preço médio, situação anterior, situação atual, discriminação)
    - [x] `TaxAssetDeclarationReport` (ano, total anterior, total atual, variação absoluta e percentual, lista de itens)
  - [x] Criar pacote `afsdigital.grahamselect.common.tax.application.dto` com:
    - [x] `TaxAssetDeclarationItemDTO` (tipos explícitos `BigDecimal` para valores e quantidades)
    - [x] `TaxAssetDeclarationReportDTO`
    - [x] `TaxAvailableYearsDTO`
  - [x] Implementar `GetTaxAssetDeclarationReportUseCase`:
    - [x] Normalização mandatória de tickers com `stripFractionalSuffix`.
    - [x] Replay cronológico das transações (`tradeDate ASC, createdAt ASC`) segundo a IN RFB 1.585/2015.
    - [x] Cálculo estrito de custo de aquisição e preço médio ponderado com proteção contra divisão por zero.
    - [x] Inclusão de ativos zerados durante o ano-base com situação atual `0.00`.
    - [x] Classificação tributária de grupo/código e geração determinística do texto de discriminação com tratamento de corretoras.
    - [x] Ordenação dos itens por `groupCode ASC, ticker ASC`.
    - [x] Filtro por `profileId` com validação de pertencimento e suporte a Multi-CPF.
    - [x] Otimização de busca de `Company` em cache de execução.
  - [x] Implementar `GetTaxAvailableYearsUseCase`:
    - [x] Extração dos anos distintos presentes nas operações do usuário.
  - [x] Implementar testes unitários herméticos para os use cases em `backend/common/src/test/java/afsdigital/grahamselect/common/tax/application/usecase/TaxUseCasesTest.java`.

- [x] Task 3: Camada de API, Endpoints REST e OpenAPI (AC: 5, 6)
  - [x] Criar `TaxDeclarationController` em `backend/api/src/main/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationController.java`:
    - [x] `GET /api/v1/tax/bens-e-direitos`
    - [x] `GET /api/v1/tax/bens-e-direitos/export` (gerador CSV com UTF-8 BOM, delimitador `;` e decimais formatados)
    - [x] `GET /api/v1/tax/available-years`
    - [x] Injeção de `PremiumFeatureAccessGuard` para validação de `profileId`.
    - [x] Logs estruturados com `@Slf4j` em nível `INFO` nas entradas e `ERROR` com stack trace nas exceções.
  - [x] Criar `TaxConfiguration` em `backend/api/src/main/java/afsdigital/grahamselect/api/tax/infrastructure/spring/TaxConfiguration.java` para wiring dos use cases como beans Spring.
  - [x] Atualizar especificação OpenAPI 3.0 em `backend/api/src/main/resources/openapi.yaml` adicionando a tag `Tax` e os 3 novos endpoints.
  - [x] Atualizar `backend/api/pom.xml` no plugin `maven-antrun-plugin` para deletar `TaxApi.java`, `TaxApiController.java` e `TaxApiDelegate.java`.
  - [x] Implementar testes de integração para o controller em `backend/api/src/test/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationControllerTest.java`.

- [x] Task 4: Camada Frontend Flutter (AC: 7)
  - [x] Criar pasta `frontend/lib/src/features/tax`:
    - [x] `domain/entities/tax_declaration_report.dart` e `tax_declaration_item.dart`
    - [x] `domain/repositories/tax_repository.dart`
    - [x] `data/datasources/tax_remote_data_source.dart` (usando `ApiClient`)
    - [x] `data/repositories/tax_repository_impl.dart`
    - [x] `presentation/providers/tax_provider.dart` (carregamento, seleção de ano, filtro de perfil familiar, cópia de discriminação, exportação CSV)
    - [x] `presentation/widgets/tax_summary_card.dart`
    - [x] `presentation/widgets/tax_item_card.dart` (com botão de cópia de discriminação com um clique)
    - [x] `presentation/pages/tax_declaration_page.dart`
  - [x] Registrar `TaxRemoteDataSource`, `TaxRepository` e `TaxProvider` em `frontend/lib/main.dart`.
  - [x] Registrar rota `/tax` no `GoRouter` dentro do `ShellRoute`.
  - [x] Adicionar item de navegação no menu lateral (`_Sidebar`) e na barra inferior (`_BottomNavBar`) em `frontend/lib/src/core/widgets/main_layout.dart`.
  - [x] Implementar testes unitários e de widget em `frontend/test/features/tax/presentation/tax_provider_test.dart` e `tax_declaration_page_test.dart`.

- [x] Task 5: Validação End-to-End, Análise Estática e Garantia de Qualidade
  - [x] Executar `mvn clean test` no backend garantindo 100% de sucesso.
  - [x] Executar `flutter analyze` e `flutter test` no frontend garantindo 0 erros e 0 warnings.
  - [x] Validar conformidade total com as regras de `AGENTS.md`.

## Dev Notes

### Arquitetura e Decisões Técnicas

- **Cálculo de Preço Médio e Custo Total (IN RFB 1.585/2015):**
  - **AVISO CRÍTICO PARA O DEV AGENT:** Não copie a fórmula de preço médio de `GetCustodyPositionsUseCase`! Aquele caso de uso apenas acumula compras globais. Para o cálculo fiscal de IRPF, você DEVE seguir rigorosamente a regra da Receita Federal:
    1. Venda parcial: reduz a quantidade e abate o custo total proporcionalmente ao preço médio vigente antes da venda ($\text{custoTotal} = \text{quantidadeRestante} \times \text{precoMedioVigente}$). O preço médio unitário **permanece inalterado**.
    2. Venda total ($\text{quantidade} = 0$): zera o `custoTotal` (`0.00`) e o `precoMedio` (`0.00`).
    3. Nova compra: soma a quantidade e adiciona `quantidade * preco` ao custo total acumulado. Recalcula o preço médio unitário:
       $$\text{Preço Médio} = \frac{\text{Custo Total}}{\text{Quantidade}}$$
    4. Divisão por zero: sempre verificar `if (quantidade.compareTo(BigDecimal.ZERO) <= 0)` e definir `precoMedio = BigDecimal.ZERO`.

- **Snippet de Referência para o Motor Fiscal:**
  ```java
  public class TaxReplayCalculator {
      public static class PositionState {
          private BigDecimal quantity = BigDecimal.ZERO;
          private BigDecimal totalCost = BigDecimal.ZERO;
          private BigDecimal averagePrice = BigDecimal.ZERO;
          private final Set<String> brokers = new TreeSet<>();

          public void processTrade(Trade trade) {
              BigDecimal qty = trade.getQuantity();
              BigDecimal price = trade.getPrice();
              if (trade.getBroker() != null && !trade.getBroker().isBlank()) {
                  brokers.add(trade.getBroker().trim());
              }

              if ("COMPRA".equalsIgnoreCase(trade.getSide())) {
                  quantity = quantity.add(qty);
                  totalCost = totalCost.add(qty.multiply(price));
                  if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                      averagePrice = totalCost.divide(quantity, 4, RoundingMode.HALF_UP);
                  }
              } else if ("VENDA".equalsIgnoreCase(trade.getSide())) {
                  quantity = quantity.subtract(qty);
                  if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                      quantity = BigDecimal.ZERO;
                      totalCost = BigDecimal.ZERO;
                      averagePrice = BigDecimal.ZERO;
                  } else {
                      // Custo proporcional restante com base no preço médio vigente
                      totalCost = quantity.multiply(averagePrice).setScale(2, RoundingMode.HALF_UP);
                  }
              }
          }
          // Getters...
      }
  }
  ```

- **Normalização de Ticker Fracionário (`stripFractionalSuffix`):**
  - Todo ticker que terminar com 'F' e possuir comprimento de 5 ou 6 caracteres (ex: `PETR4F`, `VALE3F`, `BOVA11F`) deve ser normalizado para seu ticker base (`PETR4`, `VALE3`, `BOVA11`) antes de indexar no mapa de posições.

- **Classificação de Ativos (Receita Federal):**
  - Código do País: Sempre `105 - Brasil` para ativos custodiados na B3.
  - Tabela de Grupos e Códigos DIRPF:
    - Ações ordinárias e preferenciais: Grupo 03, Código 01.
    - Fundos de Investimento Imobiliário: Grupo 07, Código 03.
    - Fundos Agroindustriais (Fiagro): Grupo 07, Código 04.
    - BDRs: Grupo 04, Código 04 (ou Grupo 03, Código 04).
    - ETFs (BOVA11, SMAL11, etc.): Grupo 07, Código 09.

- **Formatação de Discriminação:**
  - Regra determinística para o trecho de custódia:
    - 1 corretora: `"Custodiado na corretora XP Investimentos."`
    - 2+ corretoras: `"Custodiadas nas corretoras: BTG Pactual, XP Investimentos."` (ordenadas alfabeticamente).
    - Nula/vazia: `"Custodiado em corretora de valores."`
  - Se a posição continua ativa em 31/12/{ano}:
    `"150 ações de PETROLEO BRASILEIRO S.A. PETROBRAS (PETR4), CNPJ: 33.000.167/0001-01. Custodiado na corretora XP Investimentos. Custo total de aquisição em 31/12/2025: R$ 5.250,00 (Preço médio de aquisição: R$ 35,00)."`
  - Se a posição foi zerada em {ano}:
    `"Posição de 200 ações de PETROLEO BRASILEIRO S.A. PETROBRAS (PETR4), CNPJ: 33.000.167/0001-01, custodiado na corretora XP Investimentos, totalmente alienada durante o ano-calendário de 2025. Custo total de aquisição em 31/12/2025: R$ 0,00."`

- **Tratamento de Exceções e Observabilidade (`AGENTS.md`):**
  - Logs estruturados no controller:
    ```java
    log.info("Gerando relatório de Bens e Direitos: userId={}, year={}, profileId={}", userId, year, profileId);
    ```
  - Em blocos catch:
    ```java
    log.error("Erro ao gerar relatório de Bens e Direitos para userId={}, year={}: {}", userId, year, e.getMessage(), e);
    ```

### Estrutura de Arquivos

#### Novos Arquivos

```
backend/
├── common/
│   ├── src/main/java/afsdigital/grahamselect/common/tax/
│   │   ├── domain/entities/
│   │   │   ├── TaxAssetDeclarationItem.java
│   │   │   ├── TaxAssetDeclarationReport.java
│   │   │   └── TaxAssetGroup.java
│   │   ├── application/dto/
│   │   │   ├── TaxAssetDeclarationItemDTO.java
│   │   │   ├── TaxAssetDeclarationReportDTO.java
│   │   │   └── TaxAvailableYearsDTO.java
│   │   └── application/usecase/
│   │       ├── GetTaxAssetDeclarationReportUseCase.java
│   │       └── GetTaxAvailableYearsUseCase.java
│   ├── src/main/resources/db/changelog/
│   │   └── 34-add-cnpj-to-company.yaml
│   └── src/test/java/afsdigital/grahamselect/common/tax/
│       └── application/usecase/TaxUseCasesTest.java
└── api/
    ├── src/main/java/afsdigital/grahamselect/api/tax/
    │   ├── web/TaxDeclarationController.java
    │   └── infrastructure/spring/TaxConfiguration.java
    └── src/test/java/afsdigital/grahamselect/api/tax/
        └── web/TaxDeclarationControllerTest.java

frontend/
├── lib/src/features/tax/
│   ├── domain/
│   │   ├── entities/
│   │   │   ├── tax_declaration_report.dart
│   │   │   └── tax_declaration_item.dart
│   │   └── repositories/
│   │       └── tax_repository.dart
│   ├── data/
│   │   ├── datasources/
│   │   │   └── tax_remote_data_source.dart
│   │   └── repositories/
│   │       └── tax_repository_impl.dart
│   └── presentation/
│       ├── providers/
│       │   └── tax_provider.dart
│       ├── widgets/
│       │   ├── tax_summary_card.dart
│       │   └── tax_item_card.dart
│       └── pages/
│           └── tax_declaration_page.dart
└── test/features/tax/
    └── presentation/
        ├── tax_provider_test.dart
        └── tax_declaration_page_test.dart
```

#### Arquivos a Modificar (UPDATE)

```
backend/
├── common/src/main/resources/db/changelog/db.changelog-master.yaml (incluir 34-add-cnpj-to-company.yaml)
├── common/src/main/java/afsdigital/grahamselect/common/domain/entities/Company.java (adicionar campo cnpj)
├── api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/CompanyEntity.java (adicionar campo cnpj)
├── api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/CompanyJpaAdapter.java (mapear campo cnpj)
├── api/src/main/resources/openapi.yaml (adicionar tag Tax e endpoints /api/v1/tax/...)
└── api/pom.xml (adicionar exclusões antrun para TaxApi*.java)

frontend/
├── lib/main.dart (registrar Tax DI, Providers e rota /tax)
└── lib/src/core/widgets/main_layout.dart (adicionar destino de navegação 'IR / Bens e Direitos')
```

### Análise dos Arquivos Existentes Marcados como UPDATE

1. **`db.changelog-master.yaml`:**
   - *Estado Atual:* Registra os changelogs até `33-create-open-finance-connections-and-sync-logs.yaml`.
   - *Mudança:* Incluir `34-add-cnpj-to-company.yaml`.
   - *Preservar:* Ordem cronológica dos changelogs para integridade do Liquibase.

2. **`Company.java` & `CompanyEntity.java`:**
   - *Estado Atual:* Contêm `id`, `ticker`, `name`, `assetClass`, `sector`, `createdAt`, `updatedAt`.
   - *Mudança:* Adicionar campo imutável `cnpj` em `Company` e mapeamento JPA com `@Column(name = "cnpj", length = 20)` em `CompanyEntity`.
   - *Preservar:* Construtores existentes, annotations Lombok `@Builder`, `@Getter` e compatibilidade com os demais use cases (valuation, market data).

3. **`CompanyJpaAdapter.java`:**
   - *Estado Atual:* Mapeia entre `Company` e `CompanyEntity` nos métodos `save`, `findByTicker`, `findAll`, etc.
   - *Mudança:* Incluir o mapeamento de `cnpj` no método `toDomain` e `toEntity`.
   - *Preservar:* Todo o comportamento de busca e persistência existente.

4. **`openapi.yaml` & `pom.xml`:**
   - *Estado Atual:* OpenAPI define tags de Portfolio, Auth, Upload, Ranking, Goals, Admin, Family, Tags, Dividends, OpenFinance. O `pom.xml` deleta os stubs das classes com tag via Antrun.
   - *Mudança:* Adicionar paths `/api/v1/tax/bens-e-direitos`, `/api/v1/tax/bens-e-direitos/export`, `/api/v1/tax/available-years` sob tag `Tax`. Em `pom.xml`, adicionar a deleção de `TaxApi.java`, `TaxApiController.java` e `TaxApiDelegate.java`.
   - *Preservar:* Todas as outras definições do contrato.

5. **`main.dart` & `main_layout.dart`:**
   - *Estado Atual:* Instancia dependências de todos os módulos e configura o `GoRouter` e os botões da barra lateral/inferior.
   - *Mudança:* Adicionar inicialização do `TaxRemoteDataSource`, `TaxRepositoryImpl`, provider `TaxProvider`, rota `/tax` no `ShellRoute`, e botão de navegação `IR / Bens e Direitos` com ícone `Icons.receipt_long_outlined`.
   - *Preservar:* Todas as rotas e regras de autenticação existentes.

### Testing Standards Summary

- **Backend:**
  - `TaxUseCasesTest`: Testa todos os cenários de replay cronológico de operações:
    - Compra única e compras sucessivas (recálculo de preço médio ponderado).
    - Venda parcial (redução de quantidade sem alteração de preço médio unitário).
    - Venda total no ano-base (presença do item com situação anterior $> 0$ e situação atual $= 0.00$).
    - Compra e venda total no mesmo ano (não inclusão no relatório de Bens e Direitos).
    - Consolidação de fracionários (`PETR4F` + `PETR4` consolidados em `PETR4`).
    - Classificação correta de Grupo e Código da Receita Federal.
    - Discriminação gerada corretamente com todos os dados preenchidos e regras de corretora.
    - Isolamento por `profileId` (Holding Familiar) e rejeição de perfil de outro usuário.
  - `TaxDeclarationControllerTest`: Testa os endpoints REST via `@WebMvcTest` ou standalone mock:
    - Requisição sem parâmetro de ano assume ano anterior.
    - `profileId` com plano Free lança `403 Forbidden`.
    - `profileId` válido retorna `200 OK`.
    - Endpoint `/export` retorna CSV com UTF-8 BOM, delimitador `;` e conteúdo delimitado.
- **Frontend:**
  - `tax_provider_test.dart`: Testa mudança de ano, seleção de dependente/perfil, estados de loading, success e error.
  - `tax_declaration_page_test.dart`: Testa renderização da página, exibição dos cartões de totais, listagem de ativos, acionamento do botão "Copiar Discriminação" e disparo de exportação CSV.

### Project Structure Notes

- A nova feature fiscal segue rigorosamente a estrutura modular:
  - Backend: `afsdigital.grahamselect.common.tax` e `afsdigital.grahamselect.api.tax`.
  - Frontend: `lib/src/features/tax`.
- Padrão uniforme com as últimas histórias implementadas no sistema (`9-1`, `9-2`, `9-3`, `8-1`, `8-2`).

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Epic 10: Relatórios & Benchmarking]
- [Source: docs/bmad/planning-artifacts/prd.md#Functional Requirements]
- [Source: docs/bmad/planning-artifacts/architecture.md#Architectural Decisions]
- [Source: AGENTS.md#Regras para AI Agents]
- [Instrução Normativa RFB nº 1.585/2015 e atualizações - Tributação de Mercados Financeiros e de Capitais]

## Dev Agent Record

### Agent Model Used

Gemini 3.8 Flash (Medium)

### Debug Log References

- Análise de branch e commits recentes (`1d49faa`, `e5cd209`).
- Verificação do estado do `sprint-status.yaml` e dos esquemas Liquibase existentes (índices 01 a 33).
- Validação das convenções de segurança (`PremiumFeatureAccessGuard`) e de controllers Spring Boot.
- Validação de regras tributárias da Receita Federal para Bens e Direitos (IN RFB 1.585/2015).

### Completion Notes List

- Ultimate context engine analysis completed - comprehensive developer guide created for Story 10.1.
- Executado ciclo TDD completo (Red-Green-Refactor) para motor de replay e apuração de Bens e Direitos (IN RFB 1.585/2015).
- Criado changeset Liquibase `34-add-cnpj-to-company.yaml` com rollback e seed de CNPJs de empresas padrão, registrado em `db.changelog-master.yaml`.
- Atualizadas entidades de domínio e persistência `Company` e `CompanyEntity` com campo `cnpj`.
- Implementados casos de uso `GetTaxAssetDeclarationReportUseCase` e `GetTaxAvailableYearsUseCase` no módulo `common`.
- Implementados endpoints REST (`/api/v1/tax/bens-e-direitos`, `/api/v1/tax/bens-e-direitos/export`, `/api/v1/tax/available-years`) com UTF-8 BOM, delimitador `;` e isolamento multi-tenant via `PremiumFeatureAccessGuard` no módulo `api`.
- Atualizada especificação OpenAPI 3.0 e exclusões Antrun no `pom.xml`.
- Implementada feature completa de IRPF no Flutter (`lib/src/features/tax`), com `TaxSummaryCard`, `TaxItemCard`, botão de cópia de discriminação com feedback via `SnackBar`, exportação CSV e rota `/tax` integrada no `main_layout.dart`.
- Validação completa: 100% de sucesso em `mvn test` em todos os módulos backend, 100% em `flutter test` (279 testes passando) e `flutter analyze` sem nenhum warning/error.

### File List

- `backend/common/src/main/resources/db/changelog/34-add-cnpj-to-company.yaml` (NEW)
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (UPDATE)
- `backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/Company.java` (UPDATE)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/TaxAssetGroup.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/TaxAssetDeclarationItem.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/entities/TaxAssetDeclarationReport.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/dto/TaxAssetDeclarationItemDTO.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/dto/TaxAssetDeclarationReportDTO.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/dto/TaxAvailableYearsDTO.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxAssetDeclarationReportUseCase.java` (NEW)
- `backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxAvailableYearsUseCase.java` (NEW)
- `backend/common/src/test/java/afsdigital/grahamselect/common/tax/application/usecase/TaxUseCasesTest.java` (NEW)
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/CompanyEntity.java` (UPDATE)
- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/CompanyJpaAdapter.java` (UPDATE)
- `backend/api/src/main/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationController.java` (NEW)
- `backend/api/src/main/java/afsdigital/grahamselect/api/tax/infrastructure/spring/TaxConfiguration.java` (NEW)
- `backend/api/src/main/resources/openapi.yaml` (UPDATE)
- `backend/api/pom.xml` (UPDATE)
- `backend/api/src/test/java/afsdigital/grahamselect/api/tax/web/TaxDeclarationControllerTest.java` (NEW)
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/CompanyEntity.java` (UPDATE)
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/CompanyRepositoryImpl.java` (UPDATE)
- `frontend/lib/src/features/tax/domain/entities/tax_declaration_item.dart` (NEW)
- `frontend/lib/src/features/tax/domain/entities/tax_declaration_report.dart` (NEW)
- `frontend/lib/src/features/tax/domain/repositories/tax_repository.dart` (NEW)
- `frontend/lib/src/features/tax/data/datasources/tax_remote_data_source.dart` (NEW)
- `frontend/lib/src/features/tax/data/repositories/tax_repository_impl.dart` (NEW)
- `frontend/lib/src/features/tax/presentation/providers/tax_provider.dart` (NEW)
- `frontend/lib/src/features/tax/presentation/widgets/tax_summary_card.dart` (NEW)
- `frontend/lib/src/features/tax/presentation/widgets/tax_item_card.dart` (NEW)
- `frontend/lib/src/features/tax/presentation/pages/tax_declaration_page.dart` (NEW)
- `frontend/lib/main.dart` (UPDATE)
- `frontend/lib/src/core/widgets/main_layout.dart` (UPDATE)
- `frontend/test/features/tax/presentation/tax_provider_test.dart` (NEW)
- `frontend/test/features/tax/presentation/tax_declaration_page_test.dart` (NEW)
- `docs/bmad/implementation-artifacts/10-1-relatorio-de-bens-e-direitos-informe-anual.md` (UPDATE)
- `docs/bmad/implementation-artifacts/sprint-status.yaml` (UPDATE)
