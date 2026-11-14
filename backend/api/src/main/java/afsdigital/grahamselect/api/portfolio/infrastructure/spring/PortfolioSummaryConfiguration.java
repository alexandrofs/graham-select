package afsdigital.grahamselect.api.portfolio.infrastructure.spring;

import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioEvolutionUseCase;
import afsdigital.grahamselect.common.domain.repository.CompanyRepository;
import afsdigital.grahamselect.common.domain.repository.StockPricePort;
import afsdigital.grahamselect.common.portfolio.application.repository.NotificationPort;
import afsdigital.grahamselect.common.portfolio.application.usecase.SendPortfolioUpdateNotificationUseCase;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortfolioSummaryConfiguration {

    @Bean
    public SendPortfolioUpdateNotificationUseCase sendPortfolioUpdateNotificationUseCase(
            NotificationPort notificationPort
    ) {
        return new SendPortfolioUpdateNotificationUseCase(notificationPort);
    }

    @Bean
    public GetPortfolioSummaryUseCase getPortfolioSummaryUseCase(
            TradePort tradePort,
            CompanyRepository companyRepository,
            StockPricePort stockPricePort,
            PortfolioTagPort portfolioTagPort,
            AssetTagPort assetTagPort
    ) {
        return new GetPortfolioSummaryUseCase(
                tradePort,
                companyRepository,
                stockPricePort,
                portfolioTagPort,
                assetTagPort
        );
    }

    @Bean
    public GetCustodyPositionsUseCase getCustodyPositionsUseCase(
            TradePort tradePort,
            CompanyRepository companyRepository,
            StockPricePort stockPricePort,
            PortfolioTagPort portfolioTagPort,
            AssetTagPort assetTagPort
    ) {
        return new GetCustodyPositionsUseCase(
                tradePort,
                companyRepository,
                stockPricePort,
                portfolioTagPort,
                assetTagPort
        );
    }

    @Bean
    public GetPortfolioEvolutionUseCase getPortfolioEvolutionUseCase(
            TradePort tradePort,
            PortfolioTagPort portfolioTagPort,
            AssetTagPort assetTagPort
    ) {
        return new GetPortfolioEvolutionUseCase(tradePort, portfolioTagPort, assetTagPort);
    }
}
