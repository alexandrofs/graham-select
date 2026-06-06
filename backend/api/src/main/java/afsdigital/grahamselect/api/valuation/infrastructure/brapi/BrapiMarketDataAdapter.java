package afsdigital.grahamselect.api.valuation.infrastructure.brapi;

import afsdigital.grahamselect.valuation.application.dto.MarketDataResult;
import afsdigital.grahamselect.valuation.application.repository.MarketDataPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class BrapiMarketDataAdapter implements MarketDataPort {

    private final RestClient brapiRestClient;
    private final CacheManager cacheManager;
    private final String token;

    // Construtor explícito para injeção via @Bean manual, evitando falha de injeção de campo @Value
    public BrapiMarketDataAdapter(RestClient brapiRestClient, CacheManager cacheManager, String token) {
        this.brapiRestClient = brapiRestClient;
        this.cacheManager = cacheManager;
        this.token = token;
    }

    @Override
    public List<MarketDataResult> fetchMarketData(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        // Sanitização: filtrar tickers nulos ou vazios, remover espaços em branco e colocar em caixa alta
        List<String> cleanTickers = tickers.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .map(t -> t.trim().toUpperCase())
                .distinct()
                .toList();

        if (cleanTickers.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Fetching market data from Brapi for tickers: {}", cleanTickers);

        // Mapeamento: ticker base -> lista de tickers originais correspondentes (ex: BBSE3 -> [BBSE3F, BBSE3])
        // LinkedHashMap é utilizado para preservar a ordem de inserção do Collectors.groupingBy
        Map<String, List<String>> baseToOriginalMap = cleanTickers.stream()
                .collect(Collectors.groupingBy(
                        this::getBaseTicker,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<String> tickersToSend = new ArrayList<>(baseToOriginalMap.keySet());
        List<MarketDataResult> results = new ArrayList<>();
        Cache cache = cacheManager.getCache("market-data");

        // Loteamento (Batching): particionar a lista de tickers base em blocos de 20 para evitar erro 414
        int batchSize = 20;
        for (int i = 0; i < tickersToSend.size(); i += batchSize) {
            List<String> batch = tickersToSend.subList(i, Math.min(i + batchSize, tickersToSend.size()));
            String tickersParam = String.join(",", batch);

            try {
                Optional<BrapiQuoteResponse> responseOpt = fetchFromApi(tickersParam);
                if (responseOpt.isPresent()) {
                    processQuoteResponse(responseOpt.get(), baseToOriginalMap, cache, results);
                }
            } catch (BrapiApiException e) {
                log.error("Batch API call failed for tickers: {}. Error: {}", batch, e.getMessage(), e);

                // Retentar individualmente apenas para erros de cliente (como 400 Bad Request ou 404 Not Found),
                // que indicam possíveis tickers inválidos no lote, e se houver mais de 1 ticker no lote.
                boolean shouldRetryIndividually = (e.getStatusCode() == 400 || e.getStatusCode() == 404) && batch.size() > 1;

                if (shouldRetryIndividually) {
                    log.warn("Retrying individually to isolate invalid tickers: {}", batch);
                    for (String individualTicker : batch) {
                        try {
                            Optional<BrapiQuoteResponse> individualOpt = fetchFromApi(individualTicker);
                            if (individualOpt.isPresent()) {
                                processQuoteResponse(individualOpt.get(), baseToOriginalMap, cache, results);
                            }
                        } catch (Exception ex) {
                            log.error("Individual API call also failed for ticker: {}. Error: {}", individualTicker, ex.getMessage(), ex);
                        }
                    }
                } else {
                    log.warn("Individual retry skipped for batch: {}. Status code: {}", batch, e.getStatusCode());
                }
            }
        }

        // Criar um Set dos tickers processados com sucesso para otimização O(N)
        Set<String> processedTickers = results.stream()
                .map(r -> r.ticker().toUpperCase())
                .collect(Collectors.toSet());

        // Verificar tickers ausentes (usando cache para fallback individual)
        for (String ticker : cleanTickers) {
            if (!processedTickers.contains(ticker)) {
                log.warn("Ticker {} not found in Brapi. Skipping.", ticker);
                if (cache != null) {
                    MarketDataResult cached = cache.get(ticker, MarketDataResult.class);
                    if (cached != null) {
                        log.info("Using cached market data for missing ticker: {}", ticker);
                        results.add(cached);
                    }
                }
            }
        }


        return results;
    }

    private String getBaseTicker(String ticker) {
        if (ticker != null && ticker.length() >= 5 && ticker.endsWith("F")) {
            char charBeforeF = ticker.charAt(ticker.length() - 2);
            if (Character.isDigit(charBeforeF)) {
                return ticker.substring(0, ticker.length() - 1);
            }
        }
        return ticker;
    }

    private void processQuoteResponse(
            BrapiQuoteResponse response,
            Map<String, List<String>> baseToOriginalMap,
            Cache cache,
            List<MarketDataResult> results
    ) {
        if (response.results() == null) {
            return;
        }
        for (BrapiResult res : response.results()) {
            if (res == null || res.symbol() == null) {
                continue;
            }

            // Validação de preço: ignorar se for nulo, NaN ou infinito
            Double rawPrice = res.regularMarketPrice();
            if (rawPrice == null || Double.isNaN(rawPrice) || Double.isInfinite(rawPrice)) {
                log.warn("Ticker {} has invalid or null price data: {}. Skipping.", res.symbol(), rawPrice);
                continue;
            }

            Double eps = (res.defaultKeyStatistics() != null)
                    ? res.defaultKeyStatistics().earningsPerShare() : null;
            Double bvs = (res.defaultKeyStatistics() != null)
                    ? res.defaultKeyStatistics().bookValue() : null;

            // Encontrar os tickers originais que mapearam para este ticker base, sanitizando o símbolo retornado
            String cleanSymbol = cleanApiResponseSymbol(res.symbol());
            List<String> originalTickers = baseToOriginalMap.getOrDefault(cleanSymbol, List.of(res.symbol()));

            for (String originalTicker : originalTickers) {
                MarketDataResult result = new MarketDataResult(
                        originalTicker,
                        BigDecimal.valueOf(rawPrice),
                        res.dividendYield(),
                        res.priceEarnings(),
                        res.priceToBook(),
                        eps,
                        bvs
                );

                if (cache != null) {
                    cache.put(originalTicker, result);
                }
                results.add(result);
            }
        }
    }

    private String cleanApiResponseSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        String clean = symbol.trim().toUpperCase();
        if (clean.endsWith(".SA")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean;
    }

    private Optional<BrapiQuoteResponse> fetchFromApi(String tickers) {
        try {
            BrapiQuoteResponse response = brapiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote/{tickers}")
                            .queryParam("modules", "summaryProfile,financialData,defaultKeyStatistics")
                            .queryParam("token", token)
                            .build(tickers))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseEntity) -> {
                        HttpStatusCode status = responseEntity.getStatusCode();
                        throw new BrapiApiException("Brapi API returned error status: " + status, status.value());
                    })
                    .body(BrapiQuoteResponse.class);
            return Optional.ofNullable(response);
        } catch (BrapiApiException e) {
            throw e;
        } catch (Exception e) {
            throw new BrapiApiException("Brapi API communication failed for tickers: " + tickers, e);
        }
    }

    private static class BrapiApiException extends RuntimeException {
        private final int statusCode;

        public BrapiApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public BrapiApiException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = 0;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}

record BrapiQuoteResponse(List<BrapiResult> results) {}
record BrapiResult(
        String symbol,
        Double regularMarketPrice,
        Double dividendYield,
        Double priceEarnings,
        Double priceToBook,
        BrapiKeyStats defaultKeyStatistics
) {}
record BrapiKeyStats(Double earningsPerShare, Double bookValue) {}
