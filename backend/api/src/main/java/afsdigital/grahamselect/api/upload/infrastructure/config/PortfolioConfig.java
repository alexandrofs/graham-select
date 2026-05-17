package afsdigital.grahamselect.api.upload.infrastructure.config;

import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.usecase.SaveTradeUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortfolioConfig {

    @Bean
    public SaveTradeUseCase saveTradeUseCase(TradePort tradePort) {
        return new SaveTradeUseCase(tradePort);
    }
}
