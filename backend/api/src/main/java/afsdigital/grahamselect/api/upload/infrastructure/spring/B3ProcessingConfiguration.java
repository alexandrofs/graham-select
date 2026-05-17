package afsdigital.grahamselect.api.upload.infrastructure.spring;

import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.application.repository.TradeExtractionEventPort;
import afsdigital.grahamselect.common.upload.application.usecase.ProcessB3FileUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class B3ProcessingConfiguration {

    @Bean
    public ProcessB3FileUseCase processB3FileUseCase(
            TradeExtractionEventPort tradeExtractionEventPort,
            B3ImportStatusPort importStatusPort
    ) {
        return new ProcessB3FileUseCase(tradeExtractionEventPort, importStatusPort);
    }
}
