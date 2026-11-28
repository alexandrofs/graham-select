package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListManualTradesUseCaseTest {

    @Mock
    private TradePort tradePort;

    private ListManualTradesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListManualTradesUseCase(tradePort);
    }

    @Test
    void shouldReturnTradesSortedByTradeDateDescending() {
        Trade older = Trade.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .ticker("PETR4")
                .side("COMPRA")
                .tradeDate(LocalDate.of(2026, 1, 10))
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("30.00"))
                .broker("Manual")
                .createdAt(LocalDateTime.of(2026, 1, 10, 10, 0))
                .build();

        Trade newer = Trade.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .ticker("IVVB11")
                .side("COMPRA")
                .tradeDate(LocalDate.of(2026, 2, 15))
                .quantity(new BigDecimal("5"))
                .price(new BigDecimal("300.00"))
                .broker("Manual")
                .assetClass("Internacional")
                .createdAt(LocalDateTime.of(2026, 2, 15, 10, 0))
                .build();

        when(tradePort.findAllByUserId("user-1")).thenReturn(List.of(older, newer));

        List<Trade> result = useCase.execute("user-1");

        assertEquals(2, result.size());
        assertEquals("IVVB11", result.get(0).getTicker());
        assertEquals("PETR4", result.get(1).getTicker());
    }
}
