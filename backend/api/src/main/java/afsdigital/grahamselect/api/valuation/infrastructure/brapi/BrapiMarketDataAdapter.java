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
import java.util.List;
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

        // Sanitização: filtrar tickers nulos ou vazios e remover espaços em branco
        List<String> cleanTickers = tickers.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .map(String::trim)
                .toList();

        if (cleanTickers.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Fetching market data from Brapi for tickers: {}", cleanTickers);

        List<MarketDataResult> results = new ArrayList<>();
        Cache cache = cacheManager.getCache("market-data");

        // Loteamento (Batching): particionar a lista de tickers em blocos de 20 para evitar erro 414
        int batchSize = 20;
        for (int i = 0; i < cleanTickers.size(); i += batchSize) {
            List<String> batch = cleanTickers.subList(i, Math.min(i + batchSize, cleanTickers.size()));
            String tickersParam = String.join(",", batch);

            Optional<BrapiQuoteResponse> responseOpt = fetchFromApi(tickersParam);

            if (responseOpt.isPresent()) {
                BrapiQuoteResponse response = responseOpt.get();
                if (response.results() != null) {
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

                        Double eps = (res.defaultKeyStatistics() != null && res.defaultKeyStatistics().earningsPerShare() != null)
                                ? res.defaultKeyStatistics().earningsPerShare().raw() : null;
                        Double bvs = (res.defaultKeyStatistics() != null && res.defaultKeyStatistics().bookValue() != null)
                                ? res.defaultKeyStatistics().bookValue().raw() : null;

                        MarketDataResult result = new MarketDataResult(
                                res.symbol(),
                                BigDecimal.valueOf(rawPrice),
                                res.dividendYield(),
                                res.priceEarnings(),
                                res.priceToBook(),
                                eps,
                                bvs
                        );

                        if (cache != null) {
                            cache.put(res.symbol(), result);
                        }
                        results.add(result);
                    }
                }
            }
        }

        // Criar um Set dos tickers processados com sucesso para otimização O(N)
        Set<String> processedTickers = results.stream()
                .map(r -> r.ticker().toUpperCase())
                .collect(Collectors.toSet());

        // Check for missing tickers
        for (String ticker : cleanTickers) {
            if (!processedTickers.contains(ticker.toUpperCase())) {
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

        // Se todos os lotes falharam completamente ou a lista retornada estiver vazia, tentar fallback total no cache
        if (results.isEmpty() && cache != null) {
            log.warn("Brapi API failed or returned empty results. Falling back to cached data for tickers: {}", cleanTickers);
            for (String ticker : cleanTickers) {
                MarketDataResult cached = cache.get(ticker, MarketDataResult.class);
                if (cached != null) {
                    log.info("Using cached market data for ticker: {}", ticker);
                    results.add(cached);
                } else {
                    log.warn("No cached data found for ticker: {}", ticker);
                }
            }
        }

        return results;
    }

    private Optional<BrapiQuoteResponse> fetchFromApi(String tickers) {
        try {
            BrapiQuoteResponse response = brapiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote/{tickers}")
                            .queryParam("modules", "summaryProfile,financialData")
                            .queryParam("token", token)
                            .build(tickers))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, responseEntity) -> {
                        // Lança exceção para evitar que o fluxo prossiga tentando desserializar corpo de erro
                        throw new RuntimeException("Brapi API returned error status: " + responseEntity.getStatusCode());
                    })
                    .body(BrapiQuoteResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.warn("Brapi API communication failed for tickers {}. Reason: {}", tickers, e.getMessage());
            return Optional.empty();
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
record BrapiKeyStats(BrapiRaw earningsPerShare, BrapiRaw bookValue) {}
record BrapiRaw(Double raw) {}
