---
title: 'Correção de Bugs no Top 20 Graham e Modal de Nova Operação'
type: 'bugfix'
created: '2026-06-06T16:15:00-03:00'
status: 'done'
baseline_commit: '472e4ecb8e14407f7502cd0e4d2f373371445117'
context:
  - '{project-root}/docs/bmad/project-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** 
1. Duplicação de registros de ativos no ranking "Top 20 Graham" devido a inserções de cotações redundantes na mesma data no banco e ausência de agrupamento/deduplicação na query nativa de ranking.
2. Fechamento automático imediato com mensagem de sucesso ao abrir o modal "Nova Operação" pela primeira vez, devido ao compartilhamento de status de sucesso do dashboard.
3. Impossibilidade de informar tickers customizados (não aceita o texto digitado se não for clicado no dropdown de sugestões do Autocomplete) e bloqueio da digitação de preços com centavos usando vírgula (separador decimal brasileiro).

**Approach:** 
1. Ajustar a query nativa do ranking para realizar o join sobre uma subquery agrupada de cotações, e alterar o serviço de valuation para buscar cotações existentes da mesma empresa no mesmo dia, atualizando o valor (garantindo que fique com o preço mais atualizado recebido) em vez de inserir novas linhas.
2. Criar estados e tratamentos de status específicos para trade manual no `PortfolioProvider` (`tradeStatus`), isolando-o dos estados gerais do dashboard.
3. Integrar o `_tickerController` no `Autocomplete` e atualizar a regex de formatação de preço do TextFormField para permitir vírgula, efetuando a normalização antes do parsing de double.

## Boundaries & Constraints

**Always:** 
- Seguir as regras de datas UTC de forma consistente (usando `ZoneOffset.UTC`).
- Manter todos os testes unitários e de integração existentes passando.
- Usar logs apropriados (INFO nas entradas de API e ERROR em exceções).
- Manter o padrão de arquitetura multi-módulo no backend e Provider no frontend.

**Ask First:** 
- Nenhuma decisão que requeira aprovação prévia foi identificada.

**Never:** 
- Nunca realizar chamadas síncronas entre os módulos `api` e `valuation-service`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Salvamento de cotação | Valuation com cotação existente na mesma data | Atualiza o registro existente de `stock_price` com o novo preço | Log de depuração ou aviso se necessário |
| Abertura do modal | Dashboard carregado com sucesso (`status = success`) | Modal abre vazio, no estado `initial` para o trade | Sem SnackBar de sucesso |
| Digitação manual de Ticker | Usuário digita "SANB11" e clica em salvar sem usar dropdown | Trade é enviado com ticker "SANB11" | Validação normal de ticker vazio |
| Preço com vírgula | Usuário digita "40,89" | Parser converte para "40.89" e persiste no banco | Validação normal de número inválido |

</frozen-after-approval>

## Code Map

- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/RankingJpaRepository.java` -- Interface contendo a query SQL nativa do ranking.
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/ValuationRepositoryImpl.java` -- Repositório onde as cotações de stock price são salvas na base.
- `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/StockPriceJpaRepository.java` -- Repositório JPA de cotações do valuation service.
- `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart` -- Gerenciador de estado do portfólio.
- `frontend/lib/src/features/portfolio/presentation/widgets/manual_operation_entry.dart` -- Widget do modal de nova operação.

## Tasks & Acceptance

**Execution:**
- [x] `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/repository/StockPriceJpaRepository.java` -- Adicionar a assinatura do método `Optional<StockPriceEntity> findFirstByCompanyIdAndPriceDate(String companyId, java.time.LocalDate priceDate)` -- Permite localizar registros de cotação duplicados/existentes com limite de 1 registro.
- [x] `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/ValuationRepositoryImpl.java` -- Modificar a gravação de `StockPriceEntity` para buscar se já existe cotação para o mesmo `companyId` e `priceDate`: se existir, atualizar o preço com o valor do novo preço recebido (garantindo o preço mais atualizado) e salvar; caso contrário, criar um novo registro com UUID aleatório -- Evita inserções duplicadas no banco para o mesmo dia e empresa.
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repository/RankingJpaRepository.java` -- Modificar a query nativa de ranking para dar JOIN em uma subquery deduplicada agrupando cotações por `company_id` e `price_date` (ex: `JOIN (SELECT company_id, price_date, MAX(price) as price FROM stock_price GROUP BY company_id, price_date) sp ON sp.company_id = lp.company_id AND sp.price_date = lp.latest_price_date`) -- Corrige o ranking para não duplicar linhas na listagem no frontend mesmo se houver dados históricos duplicados no banco.
- [x] `frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart` -- Adicionar estados independentes `_tradeStatus` e `_tradeError` com seus respectivos getters, e atualizar `createManualTrade` e `resetStatus` para utilizarem/resetarem esses estados separadamente -- Evita colisões de SnackBar e fechamento do modal ao abrir.
- [x] `frontend/lib/src/features/portfolio/presentation/widgets/manual_operation_entry.dart` -- Vincular `_tickerController` ao parâmetro `textEditingController` do `Autocomplete`, escutar `provider.tradeStatus` em vez de `provider.status`, atualizar os formatadores de campos numéricos de preço e quantidade para permitir vírgulas (`[0-9.,]`) e aplicar `.replaceAll(',', '.')` nas strings antes de executar o `double.parse(...)` -- Corrige os inputs de Ticker e de preço decimal, garantindo que o valor seja enviado corretamente.
- [x] `frontend/test/features/portfolio/presentation/providers/portfolio_provider_test.dart` -- Atualizar os testes unitários do provider para validar as mudanças do `tradeStatus` -- Garante a integridade dos testes de unidade do frontend.

**Acceptance Criteria:**
- Given a company with existing stock price for a date, when inserting another stock price for the same date, then the existing record is updated and no new record is inserted.
- Given stock price history with duplicate entries for the same date, when retrieving the top 20 best ranked companies, then each company is listed at most once.
- Given the manual trade modal is opened, when rendering the UI, then the modal remains open and does not display a success toast message unless a trade is actively saved.
- Given the user types a custom ticker manually in the trade modal, when saving the trade, then the typed ticker is successfully validated and submitted.
- Given the user types a price with decimals using a comma (e.g., "40,89"), when saving the trade, then the value is successfully parseable and submitted.

## Verification

**Commands:**
- `mvn clean test -pl backend/common,backend/api,backend/valuation-service` -- expected: Sucesso na execução de todos os testes no backend.
- `flutter test` -- expected: Execução bem sucedida dos testes no frontend (rodar dentro da pasta `frontend`).

**Manual checks (if no CLI):**
- Abrir a aplicação com `./start-app.sh`, abrir o modal "Nova Operação" e garantir que o modal não fecha de imediato.
- Inserir um ativo digitando manualmente e salvando (ex: `ITUB4` com preço `25.50` ou `25,50`).
- Validar no painel "Top 20 Graham" se os ativos listados não se duplicam na tabela.
