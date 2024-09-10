package afsdigital.grahamselect.api.upload.infrastructure.spring;

import afsdigital.grahamselect.upload.application.service.FinancialDataPublisherService;
import afsdigital.grahamselect.upload.application.usecase.UploadFinancialDataUseCase;
import afsdigital.grahamselect.upload.domain.repository.FinancialDataReader;
import afsdigital.grahamselect.upload.domain.repository.FinancialEventDataProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UploadServiceConfiguration {

    @Bean
    public UploadFinancialDataUseCase uploadFinancialDataUseCase(FinancialDataReader financialDataReader, FinancialEventDataProducer eventDataProducer) {
        return new UploadFinancialDataUseCase(financialDataReader, new FinancialDataPublisherService(eventDataProducer));
    }

}
