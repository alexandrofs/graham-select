package afsdigital.grahamselect.api.valuation.infrastructure.spring;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.brapi.BrapiMarketDataAdapter;
import afsdigital.grahamselect.api.valuation.infrastructure.kafka.KafkaMarketDataEventPublisher;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.CustodyTickerRepositoryImpl;
import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import afsdigital.grahamselect.valuation.application.repository.MarketDataEventPublisherPort;
import afsdigital.grahamselect.valuation.application.repository.MarketDataPort;
import afsdigital.grahamselect.valuation.application.usecase.SyncMarketDataUseCase;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class MarketDataServiceConfiguration {

    @Value("${brapi.token}")
    private String token;

    @Bean
    public RestClient brapiRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        return RestClient.builder()
                .baseUrl("https://brapi.dev/api")
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofDays(1)));
        cacheManager.setCacheNames(List.of("market-data"));
        return cacheManager;
    }

    @Bean
    public CustodyTickerPort custodyTickerPort(JpaTradeRepository jpaTradeRepository) {
        return new CustodyTickerRepositoryImpl(jpaTradeRepository);
    }

    @Bean
    public MarketDataPort marketDataPort(RestClient brapiRestClient, CacheManager cacheManager) {
        return new BrapiMarketDataAdapter(brapiRestClient, cacheManager, token);
    }

    @Bean
    public MarketDataEventPublisherPort marketDataEventPublisherPort(
            KafkaTemplate<FinancialDataKey, FinancialDataEvent> kafkaTemplate
    ) {
        return new KafkaMarketDataEventPublisher(kafkaTemplate);
    }

    @Bean
    public SyncMarketDataUseCase syncMarketDataUseCase(
            MarketDataPort marketDataPort,
            CustodyTickerPort custodyTickerPort,
            MarketDataEventPublisherPort marketDataEventPublisherPort
    ) {
        return new SyncMarketDataUseCase(marketDataPort, custodyTickerPort, marketDataEventPublisherPort);
    }
}
