package afsdigital.grahamselect.api.valuation.infrastructure.spring;

import afsdigital.grahamselect.valuation.application.usecase.SyncMarketDataUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MarketDataSchedulerTest {

    @Mock
    private SyncMarketDataUseCase syncMarketDataUseCase;

    @InjectMocks
    private MarketDataScheduler marketDataScheduler;

    @Test
    void shouldCallUseCaseWhenScheduled() {
        // Act
        marketDataScheduler.syncMarketData();

        // Assert
        verify(syncMarketDataUseCase, times(1)).execute();
    }
}
