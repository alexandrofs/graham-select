# Reviewer Role: Acceptance Auditor

Você tem acesso de leitura ao projeto e aos documentos de especificação. Analise o diff a seguir frente às regras e critérios de aceitação do arquivo de especificação `docs/bmad/implementation-artifacts/spec-fix-petr4-valuation-error.md`.

Valide se:
- Todos os critérios de aceitação (ACs) da especificação foram satisfeitos de forma estrita.
- Nenhuma regra crítica ou restrição do `docs/bmad/project-context.md` foi violada.
- Não existem desvios das diretrizes arquiteturais definidas.

## Diff a ser analisado:

```diff
diff --git a/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java b/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java
index 68a84a8..e3f6e65 100644
--- b/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java
+++ b/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapter.java
@@ -198,7 +198,7 @@ public class BrapiMarketDataAdapter implements MarketDataPort {
             BrapiQuoteResponse response = brapiRestClient.get()
                     .uri(uriBuilder -> uriBuilder
                             .path("/quote/{tickers}")
-                            .queryParam("modules", "summaryProfile,financialData")
+                            .queryParam("modules", "summaryProfile,financialData,defaultKeyStatistics")
                             .queryParam("token", token)
                             .build(tickers))
                     .retrieve()
diff --git a/backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java b/backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java
index c6bdebe..955bfdb 100644
--- b/backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java
+++ b/backend/api/src/test/java/afsdigital/grahamselect/api/valuation/infrastructure/brapi/BrapiMarketDataAdapterTest.java
@@ -60,7 +60,7 @@ public class BrapiMarketDataAdapterTest {
                 }
                 """;
 
-        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData&token=test-token"))
+        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData,defaultKeyStatistics&token=test-token"))
                 .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
 
         // Act
@@ -84,7 +84,7 @@ public class BrapiMarketDataAdapterTest {
     @Test
     public void shouldFallbackToCacheWhenApiFails() {
         // Arrange
-        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData&token=test-token"))
+        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData,defaultKeyStatistics&token=test-token"))
                 .andRespond(withServerError());
 
         MarketDataResult cachedResult = new MarketDataResult(
@@ -121,7 +121,7 @@ public class BrapiMarketDataAdapterTest {
                 }
                 """;
 
-        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData&token=test-token"))
+        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData,defaultKeyStatistics&token=test-token"))
                 .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
 
         // Act
@@ -154,7 +154,7 @@ public class BrapiMarketDataAdapterTest {
                 }
                 """;
 
-        mockServer.expect(requestTo("https://brapi.dev/api/quote/BBSE3?modules=summaryProfile,financialData&token=test-token"))
+        mockServer.expect(requestTo("https://brapi.dev/api/quote/BBSE3?modules=summaryProfile,financialData,defaultKeyStatistics&token=test-token"))
                 .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
 
         // Act
@@ -192,13 +192,13 @@ public class BrapiMarketDataAdapterTest {
                 }
                 """;
 
-        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4%2CVALE3?modules=summaryProfile,financialData&token=test-token"))
+        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4%2CVALE3?modules=summaryProfile,financialData,defaultKeyStatistics&token=test-token"))
                 .andRespond(withBadRequest());
 
-        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData&token=test-token"))
+        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData,defaultKeyStatistics&token=test-token"))
                 .andRespond(withSuccess(petr4Response, MediaType.APPLICATION_JSON));
 
-        mockServer.expect(requestTo("https://brapi.dev/api/quote/VALE3?modules=summaryProfile,financialData&token=test-token"))
+        mockServer.expect(requestTo("https://brapi.dev/api/quote/VALE3?modules=summaryProfile,financialData,defaultKeyStatistics&token=test-token"))
                 .andRespond(withServerError());
 
         // Act
diff --git a/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/CalculationIntrinsicValueUseCase.java b/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/CalculationIntrinsicValueUseCase.java
index a388c7c..f00a60d 100644
--- a/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/CalculationIntrinsicValueUseCase.java
+++ b/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/CalculationIntrinsicValueUseCase.java
@@ -36,10 +36,14 @@ public class CalculationIntrinsicValueUseCase {
 
         Company company = companyLookupService.findOrCreateCompany(financialDataEvent.getTicker());
 
+        BigDecimal finalIntrinsicValue = calculatedIntrinsicValue != null
+                ? BigDecimal.valueOf(calculatedIntrinsicValue).round(new MathContext(2))
+                : null;
+
         IntrinsicValue intrinsicValue = IntrinsicValue.builder()
                 .calculationDate(financialDataEvent.getResultDate())
                 .companyId(company.getId())
-                .value(BigDecimal.valueOf(calculatedIntrinsicValue).round(new MathContext(2)))
+                .value(finalIntrinsicValue)
                 .build();
 
         StockPrice stockPrice = StockPrice.builder()
diff --git a/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/ValuationRepositoryImpl.java b/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/ValuationRepositoryImpl.java
index 32f7935..c3e3ab7 100644
--- a/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/ValuationRepositoryImpl.java
+++ b/backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/ValuationRepositoryImpl.java
@@ -25,26 +25,31 @@ public class ValuationRepositoryImpl implements ValuationRepository {
     @Override
     @Transactional
     public void saveValuationData(IntrinsicValue intrinsicValue, StockPrice stockPrice) {
-        try {
-            Optional<IntrinsicValueEntity> existingIntrinsicValue = intrinsicValueJpaRepository
-                    .findByCompanyIdAndCalculationDate(intrinsicValue.getCompanyId(), intrinsicValue.getCalculationDate());
+        if (intrinsicValue != null && intrinsicValue.getValue() != null) {
+            try {
+                Optional<IntrinsicValueEntity> existingIntrinsicValue = intrinsicValueJpaRepository
+                        .findByCompanyIdAndCalculationDate(intrinsicValue.getCompanyId(), intrinsicValue.getCalculationDate());
 
-            IntrinsicValueEntity intrinsicValueEntity;
-            if (existingIntrinsicValue.isPresent()) {
-                intrinsicValueEntity = existingIntrinsicValue.get();
-                intrinsicValueEntity.setIntrinsicValue(intrinsicValue.getValue());
-            } else {
-                intrinsicValueEntity = IntrinsicValueEntity.builder()
-                        .id(UUID.randomUUID().toString())
-                        .companyId(intrinsicValue.getCompanyId())
-                        .calculationDate(intrinsicValue.getCalculationDate())
-                        .intrinsicValue(intrinsicValue.getValue())
-                        .build();
+                IntrinsicValueEntity intrinsicValueEntity;
+                if (existingIntrinsicValue.isPresent()) {
+                    intrinsicValueEntity = existingIntrinsicValue.get();
+                    intrinsicValueEntity.setIntrinsicValue(intrinsicValue.getValue());
+                } else {
+                    intrinsicValueEntity = IntrinsicValueEntity.builder()
+                            .id(UUID.randomUUID().toString())
+                            .companyId(intrinsicValue.getCompanyId())
+                            .calculationDate(intrinsicValue.getCalculationDate())
+                            .intrinsicValue(intrinsicValue.getValue())
+                            .build();
+                }
+                intrinsicValueJpaRepository.save(intrinsicValueEntity);
+            } catch (DataIntegrityViolationException e) {
+                log.warn("Concurrent insert detected for company {} on date {}: {}", 
+                         intrinsicValue.getCompanyId(), intrinsicValue.getCalculationDate(), e.getMessage());
             }
-            intrinsicValueJpaRepository.save(intrinsicValueEntity);
-        } catch (DataIntegrityViolationException e) {
-            log.warn("Concurrent insert detected for company {} on date {}: {}", 
-                     intrinsicValue.getCompanyId(), intrinsicValue.getCalculationDate(), e.getMessage());
+        } else {
+            log.info("Intrinsic value is null or empty. Skipping intrinsic value persistence for company {} on date {}.", 
+                     stockPrice.getCompanyId(), stockPrice.getDate());
         }
 
         StockPriceEntity stockPriceEntity = StockPriceEntity.builder()
```
