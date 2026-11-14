package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.MonthlyEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPortfolioEvolutionUseCaseTest {

    @Mock
    private TradePort tradePort;

    @Mock
    private PortfolioTagPort portfolioTagPort;

    @Mock
    private AssetTagPort assetTagPort;

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

    @Test
    void shouldFilterEvolutionTradesByTagInUtcTwelveMonthWindow() {
        String userId = "user-1";
        UUID tagId = UUID.randomUUID();
        GetPortfolioEvolutionUseCase tagAware = new GetPortfolioEvolutionUseCase(
                tradePort, portfolioTagPort, assetTagPort
        );

        when(portfolioTagPort.findByIdAndUserId(tagId, userId))
                .thenReturn(Optional.of(PortfolioTag.builder().id(tagId).userId(userId).name("FIIs").build()));
        when(assetTagPort.findAllByUserIdAndTagId(userId, tagId))
                .thenReturn(List.of(AssetTag.builder().tagId(tagId).userId(userId).ticker("HGLG11").build()));

        LocalDate utcNow = LocalDate.now(ZoneOffset.UTC);
        LocalDate twoMonthsAgo = utcNow.minusMonths(2);

        Trade taggedBuy = Trade.builder()
                .userId(userId).ticker("HGLG11").side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10")).price(new BigDecimal("160.00"))
                .tradeDate(twoMonthsAgo).build();
        Trade taggedDiv = Trade.builder()
                .userId(userId).ticker("HGLG11").side(TradeSide.DIVIDENDO.name())
                .quantity(new BigDecimal("10")).price(new BigDecimal("1.10"))
                .tradeDate(twoMonthsAgo).build();
        Trade untaggedBuy = Trade.builder()
                .userId(userId).ticker("PETR4").side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("50")).price(new BigDecimal("35.00"))
                .tradeDate(twoMonthsAgo).build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(taggedBuy, taggedDiv, untaggedBuy));

        PortfolioEvolutionDTO evolution = tagAware.execute(userId, tagId);

        assertEquals(12, evolution.monthlyData().size());
        String targetMonth = twoMonthsAgo.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        for (MonthlyEvolutionDTO dto : evolution.monthlyData()) {
            if (dto.month().equals(targetMonth)) {
                assertEquals(new BigDecimal("1600.00"), dto.totalContributions());
                assertEquals(new BigDecimal("11.00"), dto.totalDividends());
            } else {
                assertEquals(BigDecimal.ZERO, dto.totalContributions());
                assertEquals(BigDecimal.ZERO, dto.totalDividends());
            }
        }
    }

    @Test
    void shouldReturnZeroEvolutionWhenTagHasNoAssets() {
        String userId = "user-1";
        UUID tagId = UUID.randomUUID();
        GetPortfolioEvolutionUseCase tagAware = new GetPortfolioEvolutionUseCase(
                tradePort, portfolioTagPort, assetTagPort
        );

        when(portfolioTagPort.findByIdAndUserId(tagId, userId))
                .thenReturn(Optional.of(PortfolioTag.builder().id(tagId).userId(userId).name("Vazia").build()));
        when(assetTagPort.findAllByUserIdAndTagId(userId, tagId)).thenReturn(List.of());
        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(
                Trade.builder().userId(userId).ticker("PETR4").side(TradeSide.COMPRA.name())
                        .quantity(BigDecimal.TEN).price(BigDecimal.TEN)
                        .tradeDate(LocalDate.now(ZoneOffset.UTC)).build()
        ));

        PortfolioEvolutionDTO evolution = tagAware.execute(userId, tagId);

        assertEquals(12, evolution.monthlyData().size());
        for (MonthlyEvolutionDTO dto : evolution.monthlyData()) {
            assertEquals(BigDecimal.ZERO, dto.totalContributions());
            assertEquals(BigDecimal.ZERO, dto.totalDividends());
        }
    }

    @Test
    void shouldThrowTagNotFoundWhenEvolutionRequestedForUnknownTag() {
        String userId = "user-1";
        UUID tagId = UUID.randomUUID();
        GetPortfolioEvolutionUseCase tagAware = new GetPortfolioEvolutionUseCase(
                tradePort, portfolioTagPort, assetTagPort
        );

        when(portfolioTagPort.findByIdAndUserId(tagId, userId)).thenReturn(Optional.empty());

        assertThrows(TagNotFoundException.class, () -> tagAware.execute(userId, tagId));
    }
}
