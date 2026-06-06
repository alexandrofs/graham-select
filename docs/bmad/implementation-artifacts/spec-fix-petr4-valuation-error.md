---
title: 'Correção do Valuation e Ingestão da PETR4'
type: 'bugfix'
created: '2026-06-06T11:29:58-03:00'
status: 'done'
baseline_commit: 'bd33daa884892023767d427a4726e3b011318561'
context:
  - '{project-root}/docs/bmad/project-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A atualização de cotações da PETR4 falha no `valuation-service` com `NullPointerException` (unboxing null Double na linha 42 de `CalculationIntrinsicValueUseCase`) porque o `api-service` não solicita o módulo `defaultKeyStatistics` da API Brapi, enviando dados de LPA e VPA nulos. Isso interrompe o processamento e reverte (via rollback) o cadastro da PETR4 no banco de dados.

**Approach:** Atualizar o `BrapiMarketDataAdapter` para solicitar o módulo `defaultKeyStatistics` da API da Brapi. Corrigir o `CalculationIntrinsicValueUseCase` para tratar de forma segura o valor intrínseco nulo, permitindo a persistência da empresa e do `StockPrice` mesmo quando o valor intrínseco não puder ser calculado (LPA/VPA ausentes).

## Boundaries & Constraints

**Always:** Seguir as regras de datas UTC de forma consistente (usando `ZoneOffset.UTC`). Manter todos os testes passando com sucesso. Usar logs apropriados (INFO nas entradas e ERROR em exceções). Criar branch `feature/fix-petr4-valuation-error` antes de implementar.

**Ask First:** Nenhuma decisão que exija aprovação direta.

**Never:** Não realizar chamadas síncronas entre os módulos `api` e `valuation-service` (comunicação exclusiva via Kafka).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Happy Path | Chamada à Brapi para buscar dados da PETR4 | Retorno contendo `defaultKeyStatistics` preenchido com LPA e VPA | O valuation calcula o valor intrínseco com sucesso, salvando a empresa, o valuation e a cotação no banco. |
| Valuation Nulo | Evento recebido com LPA ou VPA nulos ou negativos | O cálculo do valor intrínseco é ignorado (retorna nulo) | A empresa e a cotação são persistidas no banco, e o registro do valuation não é criado (ou não é lançado NPE). |

</frozen-after-approval>

## Code Map

- `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java` -- Adaptador da Brapi para buscar as cotações e indicadores, onde o parâmetro de módulos de query HTTP é configurado.
- `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/CalculationIntrinsicValueUseCase.java` -- Use case responsável por calcular o valor intrínseco e orquestrar a persistência da empresa, cotação e valuation.
- `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java` -- Classe de testes unitários do adaptador da Brapi.

## Tasks & Acceptance

**Execution:**
- [x] `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java` -- Incluir `defaultKeyStatistics` na query string de módulos -- Garante o retorno dos dados de LPA/VPA.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/CalculationIntrinsicValueUseCase.java` -- Adicionar verificação de nulidade em `calculatedIntrinsicValue` antes de chamar `BigDecimal.valueOf(...)` e persistir `IntrinsicValue` apenas se o cálculo foi realizado com sucesso -- Evita NullPointerException e rollback transacional na atualização do preço de mercado.
- [x] `backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java` -- Atualizar os mocks das chamadas HTTP para incluir `defaultKeyStatistics` nos parâmetros de consulta -- Evita quebras de testes devido à alteração dos parâmetros da query string.

**Acceptance Criteria:**
- Given a company with missing valuation data (LPA or VPA), when processing its financial data event, then the company is persisted and its stock price is updated, but its intrinsic value calculation is skipped without raising a NullPointerException.
- Given a market data sync execution, when querying the Brapi API, then the request URL includes `defaultKeyStatistics` in the `modules` parameter.

## Verification

**Commands:**
- `mvn clean test -pl backend/common,backend/api` -- expected: Todos os testes executados com sucesso.

## Suggested Review Order

**Integração com API Brapi**

- Adicionado o módulo defaultKeyStatistics para obter os dados de LPA/VPA.
  [`BrapiMarketDataAdapter.java:198`](../../../backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java#L198)

**Resiliência e Tratamento de Nulos**

- Adicionado tratamento defensivo para valor intrínseco nulo na criação do objeto.
  [`CalculationIntrinsicValueUseCase.java:39`](../../../backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/CalculationIntrinsicValueUseCase.java#L39)

- Condicionada a gravação de valuation apenas se o valor for não nulo, gravando a cotação.
  [`ValuationRepositoryImpl.java:28`](../../../backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/ValuationRepositoryImpl.java#L28)

**Testes de Integração**

- Atualização das URLs de mock nos testes da API da Brapi.
  [`BrapiMarketDataAdapterTest.java:63`](../../../backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java#L63)

