package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SaveTradeUseCaseTest {

    @Mock
    private TradePort tradePort;

    @InjectMocks
    private SaveTradeUseCase saveTradeUseCase;

    private Trade trade;

    @BeforeEach
    void setUp() {
        trade = Trade.builder()
                .userId("user-123")
                .ticker("PETR4")
                .side("COMPRA")
                .tradeDate(LocalDate.now())
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("30.00"))
                .broker("Corretora X")
                .build();
    }

    @Test
    void shouldSaveTradeWhenNotDuplicate() {
        // Given
        when(tradePort.exists(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(tradePort.save(any())).thenReturn(trade);

        // When
        Trade result = saveTradeUseCase.execute(trade);

        // Then
        assertNotNull(result);
        verify(tradePort, times(1)).save(trade);
    }

    @Test
    void shouldNotSaveTradeWhenDuplicate() {
        // Given
        when(tradePort.exists(
                trade.getUserId(),
                trade.getProfileId(),
                trade.getTicker(),
                trade.getTradeDate(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getBroker(),
                trade.getSide()
        )).thenReturn(true);

        // When
        Trade result = saveTradeUseCase.execute(trade);

        // Then
        assertNull(result);
        verify(tradePort, never()).save(any());
    }

    @Test
    void shouldCheckDuplicateUsingProfileIdWhenPresent() {
        trade.setProfileId("profile-1");
        when(tradePort.exists(
                "user-123",
                "profile-1",
                trade.getTicker(),
                trade.getTradeDate(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getBroker(),
                trade.getSide()
        )).thenReturn(false);
        when(tradePort.save(trade)).thenReturn(trade);

        Trade result = saveTradeUseCase.execute(trade);

        assertNotNull(result);
        verify(tradePort).exists("user-123", "profile-1", trade.getTicker(), trade.getTradeDate(), trade.getQuantity(), trade.getPrice(), trade.getBroker(), trade.getSide());
    }
}
