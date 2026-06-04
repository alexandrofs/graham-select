package afsdigital.grahamselect.api.valuation.infrastructure.brapi;

import afsdigital.grahamselect.valuation.application.dto.MarketDataResult;
import afsdigital.grahamselect.valuation.application.repository.MarketDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class BrapiMarketDataAdapter implements MarketDataPort {

    private final RestClient brapiRestClient;
    private final CacheManager cacheManager;

    @Value("${brapi.token}")
    private String token;

    @Override
    public List<MarketDataResult> fetchMarketData(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Fetching market data from Brapi for tickers: {}", tickers);

        String tickersParam = String.join(",", tickers);
        Optional<BrapiQuoteResponse> responseOpt = fetchFromApi(tickersParam);

        List<MarketDataResult> results = new ArrayList<>();
        Cache cache = cacheManager.getCache("market-data");

        if (responseOpt.isPresent()) {
            BrapiQuoteResponse response = responseOpt.get();
            if (response.results() != null) {
                for (BrapiResult res : response.results()) {
                    if (res == null || res.symbol() == null) {
                        continue;
                    }
                    if (res.regularMarketPrice() == null) {
                        log.warn("Ticker {} not found in Brapi or has no price data. Skipping.", res.symbol());
                        continue;
                    }

                    Double eps = (res.defaultKeyStatistics() != null && res.defaultKeyStatistics().earningsPerShare() != null)
                            ? res.defaultKeyStatistics().earningsPerShare().raw() : null;
                    Double bvs = (res.defaultKeyStatistics() != null && res.defaultKeyStatistics().bookValue() != null)
                            ? res.defaultKeyStatistics().bookValue().raw() : null;

                    MarketDataResult result = new MarketDataResult(
                            res.symbol(),
                            BigDecimal.valueOf(res.regularMarketPrice()),
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

            // Check for missing tickers
            for (String ticker : tickers) {
                boolean found = results.stream().anyMatch(r -> r.ticker().equalsIgnoreCase(ticker));
                if (!found) {
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
        } else {
            // API failed -> Fallback to Cache (AC: 3)
            log.warn("Brapi API failed. Falling back to cached data for tickers: {}", tickers);
            if (cache != null) {
                for (String ticker : tickers) {
                    MarketDataResult cached = cache.get(ticker, MarketDataResult.class);
                    if (cached != null) {
                        log.info("Using cached market data for ticker: {}", ticker);
                        results.add(cached);
                    } else {
                        log.warn("No cached data found for ticker: {}", ticker);
                    }
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
                        log.warn("Brapi API error for tickers {}. Status: {}", tickers, responseEntity.getStatusCode());
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
