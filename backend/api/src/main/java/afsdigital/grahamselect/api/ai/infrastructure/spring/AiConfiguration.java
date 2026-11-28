package afsdigital.grahamselect.api.ai.infrastructure.spring;

import afsdigital.grahamselect.api.ai.infrastructure.config.AiFinOpsConfigurationProperties;
import afsdigital.grahamselect.common.ai.application.port.AiDiagnosticCachePort;
import afsdigital.grahamselect.common.ai.application.port.AiUsageCounterPort;
import afsdigital.grahamselect.common.ai.application.service.AiDiagnosticCacheService;
import afsdigital.grahamselect.common.ai.application.service.AiFinOpsRateLimitService;
import afsdigital.grahamselect.common.ai.domain.model.AiFinOpsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiFinOpsConfigurationProperties.class)
public class AiConfiguration {

    @Bean
    public AiFinOpsProperties aiFinOpsProperties(AiFinOpsConfigurationProperties config) {
        return new AiFinOpsProperties(
                config.getDailyRateLimit(),
                config.getMonthlyRateLimit(),
                config.getCacheTtlHours()
        );
    }

    @Bean
    public AiDiagnosticCacheService aiDiagnosticCacheService(
            AiDiagnosticCachePort cachePort,
            AiFinOpsProperties properties
    ) {
        return new AiDiagnosticCacheService(cachePort, properties);
    }

    @Bean
    public AiFinOpsRateLimitService aiFinOpsRateLimitService(
            AiUsageCounterPort usageCounterPort,
            AiDiagnosticCachePort cachePort,
            AiFinOpsProperties properties
    ) {
        return new AiFinOpsRateLimitService(usageCounterPort, cachePort, properties);
    }
}
