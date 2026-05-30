package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.MonthlyEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPortfolioEvolutionUseCaseTest {

    @Mock
    private TradePort tradePort;

    private GetPortfolioEvolutionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPortfolioEvolutionUseCase(tradePort);
    }

    @Test
    void shouldReturnTwelveConsecutiveMonthsEvenIfEmpty() {
        String userId = "user-1";
        when(tradePort.findAllByUserId(userId)).thenReturn(List.of());

        PortfolioEvolutionDTO evolution = useCase.execute(userId);

        assertNotNull(evolution);
        assertNotNull(evolution.monthlyData());
        assertEquals(12, evolution.monthlyData().size());

        // Todos devem ser ZERO
        for (MonthlyEvolutionDTO dto : evolution.monthlyData()) {
            assertEquals(BigDecimal.ZERO, dto.totalContributions());
            assertEquals(BigDecimal.ZERO, dto.totalDividends());
        }
    }

    @Test
    void shouldAggregateContributionsAndDividendsCorrectly() {
        String userId = "user-1";
        LocalDate now = LocalDate.now();
        LocalDate threeMonthsAgo = now.minusMonths(3);
        LocalDate fiveMonthsAgo = now.minusMonths(5);
        LocalDate thirteenMonthsAgo = now.minusMonths(13);

        Trade comp1 = Trade.builder()
                .userId(userId)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("150.00"))
                .tradeDate(threeMonthsAgo)
                .build(); // Contribuição de 1500.00 a 3 meses atrás

        Trade comp2 = Trade.builder()
                .userId(userId)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("5"))
                .price(new BigDecimal("100.00"))
                .tradeDate(threeMonthsAgo)
                .build(); // Mais 500.00 de contribuição a 3 meses atrás (Total = 2000.00)

        Trade div1 = Trade.builder()
                .userId(userId)
                .side(TradeSide.DIVIDENDO.name())
                .quantity(new BigDecimal("1"))
                .price(new BigDecimal("300.00"))
                .tradeDate(fiveMonthsAgo)
                .build(); // Dividendo de 300.00 a 5 meses atrás

        Trade oldTrade = Trade.builder()
                .userId(userId)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("10.00"))
                .tradeDate(thirteenMonthsAgo)
                .build(); // Fora da janela de 12 meses

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(comp1, comp2, div1, oldTrade));

        PortfolioEvolutionDTO evolution = useCase.execute(userId);

        assertNotNull(evolution);
        assertEquals(12, evolution.monthlyData().size());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        String keyThreeMonthsAgo = threeMonthsAgo.format(formatter);
        String keyFiveMonthsAgo = fiveMonthsAgo.format(formatter);

        for (MonthlyEvolutionDTO dto : evolution.monthlyData()) {
            if (dto.month().equals(keyThreeMonthsAgo)) {
                assertEquals(new BigDecimal("2000.00"), dto.totalContributions());
                assertEquals(BigDecimal.ZERO, dto.totalDividends());
            } else if (dto.month().equals(keyFiveMonthsAgo)) {
                assertEquals(BigDecimal.ZERO, dto.totalContributions());
                assertEquals(new BigDecimal("300.00"), dto.totalDividends());
            } else {
                assertEquals(BigDecimal.ZERO, dto.totalContributions());
                assertEquals(BigDecimal.ZERO, dto.totalDividends());
            }
        }
    }
}
