package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.valuation.application.dto.MarketDataResult;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import afsdigital.grahamselect.valuation.application.repository.MarketDataEventPublisherPort;
import afsdigital.grahamselect.valuation.application.repository.MarketDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SyncMarketDataUseCase {

    private final MarketDataPort marketDataPort;
    private final CustodyTickerPort custodyTickerPort;
    private final MarketDataEventPublisherPort marketDataEventPublisherPort;

    public void execute() {
        List<String> tickers = custodyTickerPort.findDistinctActiveTickers();
        if (tickers == null || tickers.isEmpty()) {
            log.info("No active tickers found in custody. Skipping sync.");
            log.info("Market data sync completed. Processed: 0, Success: 0, Failed: 0");
            return;
        }

        int total = tickers.size();
        int success = 0;
        int failed = 0;

        List<MarketDataResult> results;
        try {
            results = marketDataPort.fetchMarketData(tickers);
        } catch (Exception e) {
            log.warn("Failed to fetch market data batch. Reason: {}", e.getMessage());
            results = List.of();
        }

        for (String ticker : tickers) {
            try {
                MarketDataResult result = null;
                if (results != null) {
                    result = results.stream()
                            .filter(r -> r.ticker().equalsIgnoreCase(ticker))
                            .findFirst()
                            .orElse(null);
                }

                if (result == null || result.price() == null) {
                    log.warn("Failed to fetch market data for ticker {}. Reason: Not found in Brapi results or price is null", ticker);
                    failed++;
                    continue;
                }

                FinancialDataEvent event = FinancialDataEvent.builder()
                        .ticker(result.ticker())
                        .price(result.price().doubleValue())
                        .dividendYield(result.dividendYield())
                        .priceEarnings(result.priceEarnings())
                        .priceToBook(result.priceToBook())
                        .earningsPerShare(result.earningsPerShare())
                        .bookValuePerShare(result.bookValuePerShare())
                        .resultDate(LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")))
                        .build();

                marketDataEventPublisherPort.publish(event);
                success++;
            } catch (Exception e) {
                log.warn("Failed to fetch market data for ticker {}. Reason: {}", ticker, e.getMessage());
                failed++;
            }
        }

        log.info("Market data sync completed. Processed: {}, Success: {}, Failed: {}", total, success, failed);
    }
}
