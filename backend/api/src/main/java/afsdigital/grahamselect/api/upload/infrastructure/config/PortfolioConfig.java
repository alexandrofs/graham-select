package afsdigital.grahamselect.api.upload.infrastructure.config;

import afsdigital.grahamselect.common.portfolio.application.repository.ManualTradeAuditPort;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import afsdigital.grahamselect.common.portfolio.application.usecase.CreateManualTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.SaveTradeUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortfolioConfig {

    @Bean
    public SaveTradeUseCase saveTradeUseCase(TradePort tradePort) {
        return new SaveTradeUseCase(tradePort);
    }

    @Bean
    public CreateManualTradeUseCase createManualTradeUseCase(
            TradePort tradePort,
            ManualTradeAuditPort auditPort,
            ValuationEventPort valuationEventPort,
            ObjectMapper objectMapper
    ) {
        return new CreateManualTradeUseCase(tradePort, auditPort, valuationEventPort, objectMapper);
    }
}
