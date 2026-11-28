package afsdigital.grahamselect.api.ai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for Kerno AI FinOps limits and cache TTL.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.finops")
public class AiFinOpsConfigurationProperties {

    private int dailyRateLimit = 5;
    private int monthlyRateLimit = 30;
    private int cacheTtlHours = 24;
}
