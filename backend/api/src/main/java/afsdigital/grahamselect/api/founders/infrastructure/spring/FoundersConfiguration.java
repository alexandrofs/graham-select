package afsdigital.grahamselect.api.founders.infrastructure.spring;

import afsdigital.grahamselect.api.founders.infrastructure.persistence.JdbcFounderQuotaRepositoryAdapter;
import afsdigital.grahamselect.common.founders.application.port.FounderQuotaRepositoryPort;
import afsdigital.grahamselect.common.founders.application.service.FounderQuotaService;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FoundersConfiguration {

    @Bean
    public FounderQuotaService founderQuotaService(
            FounderQuotaRepositoryPort repositoryPort,
            @Value("${app.founders.trial-days:90}") int trialDays
    ) {
        return new FounderQuotaService(repositoryPort, trialDays);
    }

    @Bean
    public SmartInitializingSingleton founderQuotaInitializer(JdbcFounderQuotaRepositoryAdapter adapter) {
        return adapter::reconcileConfiguredTotalSlots;
    }
}
