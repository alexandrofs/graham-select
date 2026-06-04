package afsdigital.grahamselect.api.valuation.infrastructure.spring;

import afsdigital.grahamselect.valuation.application.usecase.SyncMarketDataUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataScheduler {

    private final SyncMarketDataUseCase syncMarketDataUseCase;

    @Scheduled(cron = "${market-data.sync.cron:0 0 20 * * MON-FRI}")
    public void syncMarketData() {
        log.info("Starting market data sync at {}", OffsetDateTime.now(ZoneOffset.UTC));
        syncMarketDataUseCase.execute();
    }
}
