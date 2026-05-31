package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioSummaryDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.domain.repository.StockPricePort;
import afsdigital.grahamselect.common.domain.entities.StockPrice;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
