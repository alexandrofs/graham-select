package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.domain.repository.StockPricePort;
import afsdigital.grahamselect.common.domain.entities.StockPrice;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPortfolioSummaryUseCaseTest {

    @Mock
    private TradePort tradePort;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StockPricePort stockPricePort;

    @Mock
    private PortfolioTagPort portfolioTagPort;

    @Mock
    private AssetTagPort assetTagPort;

    private GetPortfolioSummaryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPortfolioSummaryUseCase(tradePort, companyRepository, stockPricePort);
    }

    @Test
    void shouldCalculateSummaryCorrectly() {
        String userId = "user-1";
        String ticker = "PETR4";
        String companyId = "comp-1";

        Trade trade1 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("30.00"))
                .build();

        Trade trade2 = Trade.builder()
                .userId(userId)
                .ticker(ticker)
                .side(TradeSide.DIVIDENDO.name())
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("1.50"))
                .build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(trade1, trade2));
        
        Company company = new Company(companyId, ticker);
        when(companyRepository.findByTicker(ticker)).thenReturn(company);
        
        StockPrice stockPrice = StockPrice.builder()
                .companyId(companyId)
                .price(new BigDecimal("35.00"))
                .date(LocalDate.now())
                .build();
        when(stockPricePort.findLatestByCompanyIds(anyList())).thenReturn(Map.of(companyId, stockPrice));

        PortfolioSummaryDTO summary = useCase.execute(userId);

        assertEquals(new BigDecimal("350.00"), summary.totalEquity());
        assertEquals(new BigDecimal("15.00"), summary.accumulatedDividends());
        assertEquals(new BigDecimal("21.6700"), summary.grossYieldPercentage().setScale(4, RoundingMode.HALF_UP));
    }

    @Test
    void shouldFilterSummaryByTagIncludingDividendsAndFractionalTickers() {
        String userId = "user-1";
        UUID tagId = UUID.randomUUID();
        GetPortfolioSummaryUseCase tagAware = new GetPortfolioSummaryUseCase(
                tradePort, companyRepository, stockPricePort, portfolioTagPort, assetTagPort
        );

        when(portfolioTagPort.findByIdAndUserId(tagId, userId))
                .thenReturn(Optional.of(PortfolioTag.builder().id(tagId).userId(userId).name("Dividendos").build()));
        when(assetTagPort.findAllByUserIdAndTagId(userId, tagId))
                .thenReturn(List.of(AssetTag.builder().tagId(tagId).userId(userId).ticker("PETR4").build()));

        Trade petrBuy = Trade.builder()
                .userId(userId).ticker("PETR4").side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("10")).price(new BigDecimal("30.00")).build();
        Trade petrDiv = Trade.builder()
                .userId(userId).ticker("PETR4").side(TradeSide.DIVIDENDO.name())
                .quantity(new BigDecimal("10")).price(new BigDecimal("1.50")).build();
        Trade valeBuy = Trade.builder()
                .userId(userId).ticker("VALE3").side(TradeSide.COMPRA.name())
                .quantity(new BigDecimal("20")).price(new BigDecimal("60.00")).build();
        Trade valeDiv = Trade.builder()
                .userId(userId).ticker("VALE3").side(TradeSide.DIVIDENDO.name())
                .quantity(new BigDecimal("20")).price(new BigDecimal("2.00")).build();

        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(petrBuy, petrDiv, valeBuy, valeDiv));
        when(companyRepository.findByTicker("PETR4")).thenReturn(new Company("comp-1", "PETR4"));
        when(stockPricePort.findLatestByCompanyIds(List.of("comp-1"))).thenReturn(Map.of(
                "comp-1", StockPrice.builder().companyId("comp-1").price(new BigDecimal("35.00")).date(LocalDate.now()).build()
        ));

        PortfolioSummaryDTO summary = tagAware.execute(userId, tagId);

        assertEquals(new BigDecimal("350.00"), summary.totalEquity());
        assertEquals(new BigDecimal("15.00"), summary.accumulatedDividends());
        assertEquals(new BigDecimal("21.6700"), summary.grossYieldPercentage().setScale(4, RoundingMode.HALF_UP));
    }

    @Test
    void shouldReturnZeroSummaryWhenTagHasNoAssociatedAssets() {
        String userId = "user-1";
        UUID tagId = UUID.randomUUID();
        GetPortfolioSummaryUseCase tagAware = new GetPortfolioSummaryUseCase(
                tradePort, companyRepository, stockPricePort, portfolioTagPort, assetTagPort
        );

        when(portfolioTagPort.findByIdAndUserId(tagId, userId))
                .thenReturn(Optional.of(PortfolioTag.builder().id(tagId).userId(userId).name("Vazia").build()));
        when(assetTagPort.findAllByUserIdAndTagId(userId, tagId)).thenReturn(List.of());
        when(tradePort.findAllByUserId(userId)).thenReturn(List.of(
                Trade.builder().userId(userId).ticker("PETR4").side(TradeSide.COMPRA.name())
                        .quantity(new BigDecimal("10")).price(new BigDecimal("30.00")).build()
        ));
        when(stockPricePort.findLatestByCompanyIds(List.of())).thenReturn(Map.of());

        PortfolioSummaryDTO summary = tagAware.execute(userId, tagId);

        assertEquals(BigDecimal.ZERO, summary.totalEquity());
        assertEquals(BigDecimal.ZERO, summary.accumulatedDividends());
        assertEquals(BigDecimal.ZERO, summary.grossYieldPercentage());
    }

    @Test
    void shouldThrowTagNotFoundWhenSummaryRequestedForUnknownOrForeignTag() {
        String userId = "user-1";
        UUID foreignTagId = UUID.randomUUID();
        GetPortfolioSummaryUseCase tagAware = new GetPortfolioSummaryUseCase(
                tradePort, companyRepository, stockPricePort, portfolioTagPort, assetTagPort
        );

        when(portfolioTagPort.findByIdAndUserId(foreignTagId, userId)).thenReturn(Optional.empty());

        assertThrows(TagNotFoundException.class, () -> tagAware.execute(userId, foreignTagId));
    }
}
