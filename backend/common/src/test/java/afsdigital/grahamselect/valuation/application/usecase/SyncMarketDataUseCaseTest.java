package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.valuation.application.dto.MarketDataResult;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import afsdigital.grahamselect.valuation.application.repository.MarketDataEventPublisherPort;
import afsdigital.grahamselect.valuation.application.repository.MarketDataPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SyncMarketDataUseCaseTest {

    @Mock
    private MarketDataPort marketDataPort;

    @Mock
    private CustodyTickerPort custodyTickerPort;

    @Mock
    private MarketDataEventPublisherPort marketDataEventPublisherPort;

    @InjectMocks
    private SyncMarketDataUseCase syncMarketDataUseCase;

    @Test
    public void shouldSyncAllTickersSuccessfully() {
        // Arrange
        List<String> tickers = List.of("PETR4", "VALE3");
        when(custodyTickerPort.findDistinctActiveTickers()).thenReturn(tickers);

        List<MarketDataResult> results = List.of(
                new MarketDataResult("PETR4", BigDecimal.valueOf(35.50), 8.5, 5.2, 1.2, 6.8, 29.5),
                new MarketDataResult("VALE3", BigDecimal.valueOf(70.20), 6.2, 4.8, 1.5, 14.6, 46.8)
        );
        when(marketDataPort.fetchMarketData(tickers)).thenReturn(results);

        // Act
        syncMarketDataUseCase.execute();

        // Assert
        ArgumentCaptor<FinancialDataEvent> captor = ArgumentCaptor.forClass(FinancialDataEvent.class);
        verify(marketDataEventPublisherPort, times(2)).publish(captor.capture());

        List<FinancialDataEvent> publishedEvents = captor.getAllValues();
        assertEquals(2, publishedEvents.size());

        FinancialDataEvent event1 = publishedEvents.stream()
                .filter(e -> e.getTicker().equals("PETR4"))
                .findFirst().orElse(null);
        assertEquals(35.50, event1.getPrice());
        assertEquals(8.5, event1.getDividendYield());
        assertEquals(5.2, event1.getPriceEarnings());
        assertEquals(1.2, event1.getPriceToBook());

        FinancialDataEvent event2 = publishedEvents.stream()
                .filter(e -> e.getTicker().equals("VALE3"))
                .findFirst().orElse(null);
        assertEquals(70.20, event2.getPrice());
        assertEquals(6.2, event2.getDividendYield());
        assertEquals(4.8, event2.getPriceEarnings());
        assertEquals(1.5, event2.getPriceToBook());
    }

    @Test
    public void shouldContinueProcessingWhenOneTickerFails() {
        // Arrange
        List<String> tickers = List.of("PETR4", "VALE3");
        when(custodyTickerPort.findDistinctActiveTickers()).thenReturn(tickers);

        // Fetch returns only PETR4, simulating missing or failed/skipped VALE3
        List<MarketDataResult> results = List.of(
                new MarketDataResult("PETR4", BigDecimal.valueOf(35.50), 8.5, 5.2, 1.2, 6.8, 29.5)
        );
        when(marketDataPort.fetchMarketData(tickers)).thenReturn(results);

        // Act
        syncMarketDataUseCase.execute();

        // Assert
        ArgumentCaptor<FinancialDataEvent> captor = ArgumentCaptor.forClass(FinancialDataEvent.class);
        verify(marketDataEventPublisherPort, times(1)).publish(captor.capture());

        List<FinancialDataEvent> publishedEvents = captor.getAllValues();
        assertEquals(1, publishedEvents.size());
        assertEquals("PETR4", publishedEvents.get(0).getTicker());
    }

    @Test
    public void shouldSkipTickerWhenPriceIsNull() {
        // Arrange
        List<String> tickers = List.of("PETR4");
        when(custodyTickerPort.findDistinctActiveTickers()).thenReturn(tickers);

        List<MarketDataResult> results = List.of(
                new MarketDataResult("PETR4", null, 8.5, 5.2, 1.2, 6.8, 29.5)
        );
        when(marketDataPort.fetchMarketData(tickers)).thenReturn(results);

        // Act
        syncMarketDataUseCase.execute();

        // Assert
        verify(marketDataEventPublisherPort, never()).publish(any());
    }

    @Test
    public void shouldHandleEmptyTickersListGracefully() {
        // Arrange
        when(custodyTickerPort.findDistinctActiveTickers()).thenReturn(List.of());

        // Act
        syncMarketDataUseCase.execute();

        // Assert
        verify(marketDataPort, never()).fetchMarketData(any());
        verify(marketDataEventPublisherPort, never()).publish(any());
    }
}
