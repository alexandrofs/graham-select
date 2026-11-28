---
title: 'Sanitizador LGPD de PII, Normalização Canônica e Validador Pré-Payload'
type: 'feature'
created: '2026-11-28'
baseline_commit: '38f05953ae39e18a5bd721d187140a0daf09e77e'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/docs/bmad/implementation-artifacts/epic-5-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Ainda não existe uma barreira determinística em `backend/common` que remova dados pessoais (PII) e valores monetários absolutos em `R$`, normalize ativos manuais em rótulos canônicos anônimos e calcule o hash de estado da carteira antes de qualquer envio futuro ao Vertex AI (Story 5.3).

**Approach:** Implementar no módulo puro `backend/common` (`afsdigital.grahamselect.common.ai`) os modelos de snapshot bruto/sanitizado, o catálogo fechado de allowlists (B3, Internacional, Tesouro Direto e Criptoativos), o `PiiSanitizer`, o `PortfolioStateHashCalculator` (SHA-256 canônico) e o `AiPayloadValidator` (bloqueio de PII, moedas e teto de 4.000 tokens sobre o JSON serializado), além do wiring Spring em `AiConfiguration` (`backend/api`).

## Boundaries & Constraints

**Always:**
- Usar `BigDecimal` com `RoundingMode.HALF_EVEN` em todos os percentuais, pesos e quantidades normalizadas (escala fixa de 4 casas decimais no hash); nunca usar `double`/`float` em cálculos financeiros.
- Garantir `0 bytes` de PII (nome, CPF, e-mail, telefone, IDs nominais de conta, nomes de corretoras) e `0` valores monetários absolutos (`R$`, `BRL`, preços unitários, saldos ou quantidade absoluta de cotas) no `SanitizedPortfolioSnapshot`.
- Validar tickers contra allowlists oficiais: B3 (`^[A-Z][A-Z0-9]{3}[0-9]{1,2}$` + catálogo curado, removendo `F` apenas de `^[A-Z][A-Z0-9]{3}[0-9]{1,2}F$`), Internacional (`^[A-Z0-9.-]{1,10}$` + catálogo curado), Tesouro Direto (preservando ano quando válido, ex.: `TESOURO_SELIC_2029`) e Criptoativos (normalizando `BTC-BRL`/`BTC-USD` → `BTC`).
- Substituir qualquer descrição livre ou ticker fora das allowlists (incluindo tentativas literais `*_MANUAL_*`) por rótulos canônicos sequenciais por `MacroClass` (`<MACRO_CLASS>_MANUAL_<N>`), ordenados por chave composta determinística e descartando a descrição original.
- Agregar posições equivalentes pelo `canonicalTicker` antes de calcular `portfolio_state_hash` (SHA-256 hex lowercase de 64 chars sobre `ticker_canonico:quantidade_4_casas:quarantine` ordenado + metas de `MacroClass` + notas de qualidade `1–10`).
- Validar o JSON serializado antes do envio com `AiPayloadValidator` (`<= 4.000` tokens estimados e varredura defensiva de CPF, CNPJ, e-mail, telefone e moedas sem falsos positivos em datas ISO ou hashes SHA-256), lançando `AiPayloadValidationException` com `log.error` sem PII.

**Never:**
- Expor `quantity` absoluta, `userId`, `cpf`, `email`, `phone`, `brokerName` ou valores em `R$` no `SanitizedPortfolioSnapshot`.
- Chamar Vertex AI/LLM, criar endpoints REST ou acoplar `backend/common` a repositórios JPA nesta história.
- Usar `String.hashCode()` ou fallback silencioso em caso de erro de SHA-256.
- Logar valores de PII ou descrições brutas rejeitadas em mensagens de log.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Carteira com B3 + Exterior + Tesouro + Cripto válidos | `B3SA3`, `PETR4F`, `AAPL`, `TESOURO_SELIC_2029`, `BTC-BRL` + nome/CPF/e-mail/saldo `R$` | Normaliza (`PETR4`, `TESOURO_SELIC_2029`, `BTC`), agrega duplicados, mantém apenas `weightPercent`, `CerradoSubClass`, nota, HHI e flags; remove 100% de PII e `R$`/`quantity` | N/A |
| Ativos manuais com texto livre ou PII embutida | `"CDB Banco Inter CPF 123.456.789-00 João R$ 50.000"` em `RENDA_FIXA` | Descarta descrição; atribui `RENDA_FIXA_MANUAL_1`, `RENDA_FIXA_MANUAL_2` em ordem determinística | N/A |
| Determinismo do `portfolio_state_hash` | Mesma carteira com posições em ordens distintas no input | Produz exatamente o mesmo SHA-256 de 64 caracteres (independe de escala `10.0` vs `10.0000`) | N/A |
| Alteração de meta, nota ou quarentena | Mesmas quantidades/tickers, mas `targetAcoesBr`, `qualityGrade` ou `quarantineCorporateEvent` alterado | Produz SHA-256 diferente para invalidar cache | N/A |
| Payload excede orçamento de 4.000 tokens | JSON serializado acima de 4.000 tokens estimados | `AiPayloadValidator` rejeita antes de qualquer saída de rede | Lança `AiPayloadValidationException` (`log.error` sem PII) |
| Vazamento residual de PII ou moeda no JSON | JSON contendo CPF (com/sem pontuação), CNPJ, e-mail, telefone ou `R$`/`US$`/`BRL` | `AiPayloadValidator` bloqueia o envio sem falsos positivos em datas ISO ou hash hex | Lança `AiPayloadValidationException` (`log.error` sem PII) |

</frozen-after-approval>

## Code Map

- `backend/common/src/main/java/afsdigital/grahamselect/common/ai/` -- Pacote alvo para modelos, `TickerAllowlistCatalog`, `PiiSanitizer`, `PortfolioStateHashCalculator` e `AiPayloadValidator`.
- `backend/common/src/main/java/afsdigital/grahamselect/common/cerrado/domain/model/MacroClass.java` e `CerradoSubClass.java` -- Enums canônicos de macro-classes e subclasses do Cerrado.
- `backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java` (L256–L326) -- Conjuntos existentes (`CRYPTO_TICKERS`, `INTERNATIONAL_ETF_TICKERS`, `BRAZILIAN_EQUITY_ETF_TICKERS`, `FIXED_INCOME_ETF_TICKERS`) para referência do catálogo.
- `backend/api/src/main/java/afsdigital/grahamselect/api/ai/infrastructure/spring/AiConfiguration.java` -- Registrar beans de `TickerAllowlistCatalog`, `PiiSanitizer`, `PortfolioStateHashCalculator` e `AiPayloadValidator`.

## Tasks & Acceptance

**Execution:**
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/ai/domain/model/` -- Criar `RawPortfolioSnapshot`, `RawPortfolioPosition` (com campos sensíveis apenas para entrada/descarte e `quantity` para hash) e `SanitizedPortfolioSnapshot`, `SanitizedAssetPosition` (`canonicalTicker`, `macroClass`, `CerradoSubClass`, `weightPercent`, `qualityGrade`, `quarantineCorporateEvent`, múltiplos relativos opcionais), `MacroClassAllocationSnapshot` (atual `%`, meta `%`, desvio `%`) e `hhiIndex` em `BigDecimal` (`HALF_EVEN`) -- Isolar fronteira segura sem `quantity` nem PII na saída.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/ai/domain/service/TickerAllowlistCatalog.java` -- Implementar catálogo puro e versionado (suportando extensão via `Set<String>` opcional no construtor) para B3 (`^[A-Z][A-Z0-9]{3}[0-9]{1,2}$` + strip de `F` apenas em fracionário B3), Internacional (`^[A-Z0-9.-]{1,10}$` + catálogo curado), Tesouro Direto (preservando ano quando válido) e Criptoativos (normalizando `*-BRL`/`*-USD`) -- Garantir padronização canônica.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/ai/application/service/PiiSanitizer.java` e `PortfolioStateHashCalculator.java` -- Implementar `PiiSanitizer` (descarta PII/`R$`/`quantity`, normaliza e agrega posições pelo `canonicalTicker`, numera `<MACRO_CLASS>_MANUAL_<N>` em ordem determinística) e `PortfolioStateHashCalculator` (SHA-256 hex 64 chars sobre posições agregadas com escala 4, metas e notas) -- Garantir anonimização LGPD e determinismo de cache.
- [x] `backend/common/src/main/java/afsdigital/grahamselect/common/ai/application/service/AiPayloadValidator.java`, `AiPayloadValidationException.java` e `AiConfiguration.java` -- Implementar `AiPayloadValidator` sobre `String serializedPayloadJson` (teto `<= 4.000` tokens via `ceil(utf8Bytes / 4.0)` e regexes precisas de PII/moeda) e registrar os beans em `AiConfiguration` -- Bloquear vazamentos antes da camada de rede.
- [x] `backend/common/src/test/java/afsdigital/grahamselect/common/ai/` -- Criar testes unitários (`TickerAllowlistCatalogTest`, `PiiSanitizerTest`, `PortfolioStateHashCalculatorTest`, `AiPayloadValidatorTest`) cobrindo toda a matriz I/O, agregação multi-CPF/fracionário, escala `BigDecimal`, ausência de falsos positivos e orçamento de 4.000 tokens -- Garantir 100% de cobertura da barreira LGPD.

**Acceptance Criteria:**
- Given um `RawPortfolioSnapshot` contendo nome, CPF, e-mail, telefone, corretora, valores em `R$`, quantidades absolutas e ativos manuais com texto livre, when `PiiSanitizer.sanitize(...)` executa, then o `SanitizedPortfolioSnapshot` resultante contém `0 bytes` de PII, `R$` ou `quantity` e todos os ativos fora das allowlists recebem rótulos canônicos sequenciais determinísticos.
- Given duas instâncias equivalentes de carteira com posições em ordens ou escalas `BigDecimal` distintas, when `PortfolioStateHashCalculator.calculate(...)` executa, then ambas produzem o mesmo SHA-256 de 64 caracteres, mudando apenas se ticker canônico, quantidade, meta, nota de qualidade ou quarentena mudar.
- Given um JSON serializado que viole o teto de 4.000 tokens ou contenha padrão residual de PII/moeda, when `AiPayloadValidator.validate(...)` executa, then lança `AiPayloadValidationException` antes de qualquer chamada externa.

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Finding | Layer | Verdict | Route | Evidence |
|---------|-------|---------|-------|----------|
| `MANUAL_POSITION_COMPARATOR` omite `quarantineCorporateEvent` e múltiplos | edge-case-hunter, blind-hunter | medium | patch | Confirmado em `CanonicalPositionAggregator.java:183-188`; incluir `quarantineCorporateEvent` e os 4 múltiplos no comparador para garantir determinismo total. |
| `MutableAggregate.merge` depende da ordem de entrada para `subClass` e múltiplos | edge-case-hunter, blind-hunter | medium | patch | Confirmado em `CanonicalPositionAggregator.java:322-333`; usar seleção determinística independente da ordem (ex.: menor valor / `subClass` ordenado). |
| `CanonicalPositionAggregator` agrupa apenas por `canonicalTicker` ignorando divergência de `MacroClass` | blind-hunter | low | patch | Confirmado em `CanonicalPositionAggregator.java:222`; agrupar por `(canonicalTicker, macroClass)` para evitar soma silenciosa em `MacroClass` errada. |
| `RawPortfolioPosition` / `RawPortfolioSnapshot` não validam `quantity < 0` ou `weightPercent` fora de `0..100` | edge-case-hunter, blind-hunter | medium | patch | Confirmado em `RawPortfolioPosition.java:619-624`; validar limites não-negativos e `weightPercent <= 100` nos construtores. |
| `TickerAllowlistCatalog.resolveCrypto` aceita `extraAllowedTickers` sem validar formato | blind-hunter | medium | patch | Confirmado em `TickerAllowlistCatalog.java:1100`; mover checagem de `extraAllowedTickers` para `resolveB3`/`resolveInternational` após regex ou validar formato em cripto. |
| `AiPayloadValidator` não bloqueia chaves sensíveis (`marketValueBrl`, `quantity`, `userId`, etc.), `USD`/`EUR` e `55` sem `+` | blind-hunter | medium | patch | Confirmado em `AiPayloadValidator.java:96-117`; adicionar regra de chaves brutas proibidas, `\bUSD\b`/`\bEUR\b`/`€` e `\+?55`. |
| `PiiSanitizer.resolveHhiIndex` usa `sanitizedPositions` já arredondados | blind-hunter | low | patch | Confirmado em `PiiSanitizer.java:437-454`; calcular HHI sobre `aggregated` antes do arredondamento para 2 casas. |
| `AiPayloadValidationException` fora do subpacote `.exceptions` | blind-hunter | low (rejected) | — | Cosmético; caminho definido na spec e sem impacto funcional. |
| Lacunas de teste: ordenação de manuais invertida, mudança de `quantity` no hash, `null`/UTF-8 multibyte/`Renda+`/`Educa+` | verification-gap, blind-hunter | medium | patch | Confirmado em `PiiSanitizerTest`, `PortfolioStateHashCalculatorTest`, `AiPayloadValidatorTest` e `TickerAllowlistCatalogTest`; adicionar os casos faltantes. |

## Design Notes

- **Estimativa determinística de tokens:** Usar `ceil(utf8Bytes / 4.0)` sobre o JSON serializado para validar o teto de `4.000` tokens sem acoplar tokenizador externo em `common`.
- **Chave composta de ordenação manual:** Ordenar ativos manuais por `(normalizedRawKey, subClass, qualityGrade, quantity, weightPercent)` antes de atribuir `1..N` por `MacroClass`.

## Verification

**Commands:**
- `cd backend && mvn clean test` -- expected: 0 erros/warnings, todos os testes unitários passando.
- `cd backend && mvn test -pl api -Dtest="AiCacheAndFinOpsIT,AccountDeletionIT"` -- expected: contexto Spring (`AiConfiguration`) e testes de integração passando sem erros.
