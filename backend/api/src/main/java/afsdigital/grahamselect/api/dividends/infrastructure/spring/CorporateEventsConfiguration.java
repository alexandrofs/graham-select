package afsdigital.grahamselect.api.dividends.infrastructure.spring;

import afsdigital.grahamselect.api.dividends.infrastructure.crawler.BrapiCorporateEventsAdapter;
import afsdigital.grahamselect.api.dividends.infrastructure.crawler.CompositeCorporateEventsCrawler;
import afsdigital.grahamselect.api.dividends.infrastructure.crawler.CvmOfficialCrawlerAdapter;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAlertPublisherPort;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAnnouncementPort;
import afsdigital.grahamselect.common.dividends.application.repository.DividendCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactPort;
import afsdigital.grahamselect.common.dividends.application.usecase.GetDividendScheduleUseCase;
import afsdigital.grahamselect.common.dividends.application.usecase.GetRelevantFactsUseCase;
import afsdigital.grahamselect.common.dividends.application.usecase.SyncCorporateEventsUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;

@Configuration
public class CorporateEventsConfiguration {

    @Value("${brapi.token:}")
    private String brapiToken;

    @Value("${crawler.user-agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 KernoCrawler/1.0}")
    private String userAgent;

    @Value("${crawler.timeout-ms:10000}")
    private int timeoutMs;

    @Bean(name = "dividendAlertExecutor")
    public Executor dividendAlertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("dividend-alerts-");
        executor.initialize();
        return executor;
    }

    @Bean
    public BrapiCorporateEventsAdapter brapiCorporateEventsAdapter(RestClient brapiRestClient) {
        return new BrapiCorporateEventsAdapter(brapiRestClient, brapiToken);
    }

    @Bean
    public CvmOfficialCrawlerAdapter cvmOfficialCrawlerAdapter() {
        return new CvmOfficialCrawlerAdapter(userAgent, timeoutMs);
    }

    @Bean
    @Primary
    public CompositeCorporateEventsCrawler compositeCorporateEventsCrawler(
            BrapiCorporateEventsAdapter brapiAdapter,
            CvmOfficialCrawlerAdapter cvmAdapter
    ) {
        return new CompositeCorporateEventsCrawler(brapiAdapter, cvmAdapter, cvmAdapter);
    }

    @Bean
    public SyncCorporateEventsUseCase syncCorporateEventsUseCase(
            CustodyTickerPort custodyTickerPort,
            CompositeCorporateEventsCrawler compositeCrawler,
            DividendAnnouncementPort dividendAnnouncementPort,
            RelevantFactPort relevantFactPort,
            DividendAlertPublisherPort dividendAlertPublisherPort
    ) {
        return new SyncCorporateEventsUseCase(
                custodyTickerPort,
                compositeCrawler,
                compositeCrawler,
                dividendAnnouncementPort,
                relevantFactPort,
                dividendAlertPublisherPort
        );
    }

    @Bean
    public GetDividendScheduleUseCase getDividendScheduleUseCase(
            DividendAnnouncementPort dividendAnnouncementPort,
            GetCustodyPositionsUseCase getCustodyPositionsUseCase
    ) {
        return new GetDividendScheduleUseCase(dividendAnnouncementPort, getCustodyPositionsUseCase);
    }

    @Bean
    public GetRelevantFactsUseCase getRelevantFactsUseCase(
            RelevantFactPort relevantFactPort,
            GetCustodyPositionsUseCase getCustodyPositionsUseCase
    ) {
        return new GetRelevantFactsUseCase(relevantFactPort, getCustodyPositionsUseCase);
    }
}
