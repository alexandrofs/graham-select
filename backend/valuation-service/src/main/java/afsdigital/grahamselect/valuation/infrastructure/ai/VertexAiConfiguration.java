package afsdigital.grahamselect.valuation.infrastructure.ai;

import afsdigital.grahamselect.common.ai.application.port.AiPortfolioAnalyzerPort;
import afsdigital.grahamselect.common.ai.application.service.AiPayloadValidator;
import afsdigital.grahamselect.common.ai.application.service.DeterministicPortfolioFallbackGenerator;
import afsdigital.grahamselect.common.ai.application.service.PostGenerationValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring configuration for Kerno AI Vertex AI Enterprise ZDR adapter and
 * deterministic validation/fallback components in {@code valuation-service}.
 */
@Configuration
@EnableConfigurationProperties(VertexAiConfigurationProperties.class)
public class VertexAiConfiguration {

    @Bean
    public AiPayloadValidator aiPayloadValidator() {
        return new AiPayloadValidator();
    }

    @Bean
    public PostGenerationValidator postGenerationValidator() {
        return new PostGenerationValidator();
    }

    @Bean
    public DeterministicPortfolioFallbackGenerator deterministicPortfolioFallbackGenerator() {
        return new DeterministicPortfolioFallbackGenerator();
    }

    @Bean
    public GcpAccessTokenProvider gcpAccessTokenProvider() {
        return new GcpAccessTokenProvider();
    }

    @Bean
    public RestClient vertexAiRestClient(VertexAiConfigurationProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = properties.getTimeout() != null ? properties.getTimeout() : Duration.ofSeconds(15);
        int timeoutMs = (int) Math.min(Integer.MAX_VALUE, Math.max(1_000L, timeout.toMillis()));
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public AiPortfolioAnalyzerPort aiPortfolioAnalyzerPort(
            RestClient vertexAiRestClient,
            ObjectMapper objectMapper,
            VertexAiConfigurationProperties properties,
            GcpAccessTokenProvider gcpAccessTokenProvider,
            AiPayloadValidator aiPayloadValidator,
            PostGenerationValidator postGenerationValidator,
            DeterministicPortfolioFallbackGenerator deterministicPortfolioFallbackGenerator
    ) {
        return new VertexAiGeminiAdapter(
                vertexAiRestClient,
                objectMapper,
                properties,
                gcpAccessTokenProvider,
                aiPayloadValidator,
                postGenerationValidator,
                deterministicPortfolioFallbackGenerator
        );
    }
}
