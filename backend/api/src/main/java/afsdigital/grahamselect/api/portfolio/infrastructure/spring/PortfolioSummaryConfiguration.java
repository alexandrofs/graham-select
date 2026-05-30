package afsdigital.grahamselect.api.portfolio.infrastructure.spring;

import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioEvolutionUseCase;
import afsdigital.grahamselect.valuation.application.repository.CompanyRepository;
import afsdigital.grahamselect.valuation.application.repository.StockPricePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortfolioSummaryConfiguration {

    @Bean
    public GetPortfolioSummaryUseCase getPortfolioSummaryUseCase(
            TradePort tradePort,
            CompanyRepository companyRepository,
            StockPricePort stockPricePort
    ) {
        return new GetPortfolioSummaryUseCase(tradePort, companyRepository, stockPricePort);
    }

    @Bean
    public GetCustodyPositionsUseCase getCustodyPositionsUseCase(
            TradePort tradePort,
            CompanyRepository companyRepository,
            StockPricePort stockPricePort
    ) {
        return new GetCustodyPositionsUseCase(tradePort, companyRepository, stockPricePort);
    }

    @Bean
    public GetPortfolioEvolutionUseCase getPortfolioEvolutionUseCase(
            TradePort tradePort
    ) {
        return new GetPortfolioEvolutionUseCase(tradePort);
    }
}
