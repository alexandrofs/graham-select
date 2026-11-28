package afsdigital.grahamselect.api.upload.infrastructure.config;

import afsdigital.grahamselect.common.portfolio.application.repository.ManualTradeAuditPort;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import afsdigital.grahamselect.common.portfolio.application.usecase.CreateManualTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.DeleteManualTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.ListManualTradesUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.SaveTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.UpdateManualTradeUseCase;
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

    @Bean
    public ListManualTradesUseCase listManualTradesUseCase(TradePort tradePort) {
        return new ListManualTradesUseCase(tradePort);
    }

    @Bean
    public UpdateManualTradeUseCase updateManualTradeUseCase(
            TradePort tradePort,
            ManualTradeAuditPort auditPort,
            ValuationEventPort valuationEventPort,
            ObjectMapper objectMapper
    ) {
        return new UpdateManualTradeUseCase(tradePort, auditPort, valuationEventPort, objectMapper);
    }

    @Bean
    public DeleteManualTradeUseCase deleteManualTradeUseCase(
            TradePort tradePort,
            ManualTradeAuditPort auditPort,
            ValuationEventPort valuationEventPort,
            ObjectMapper objectMapper
    ) {
        return new DeleteManualTradeUseCase(tradePort, auditPort, valuationEventPort, objectMapper);
    }
}
