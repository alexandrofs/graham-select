package afsdigital.grahamselect.api.dividends.infrastructure.spring;

import afsdigital.grahamselect.api.dividends.infrastructure.crawler.CvmOfficialCrawlerAdapter;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAlertPublisherPort;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAnnouncementPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactPort;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CorporateEventsConfigurationTest {

    private static final String EXPECTED_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 KernoCrawler/1.0";

    private ApplicationContextRunner baseContextRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(CorporateEventsConfiguration.class)
                .withBean("brapiRestClient", RestClient.class, () -> mock(RestClient.class))
                .withBean(CustodyTickerPort.class, () -> mock(CustodyTickerPort.class))
                .withBean(DividendAnnouncementPort.class, () -> mock(DividendAnnouncementPort.class))
                .withBean(RelevantFactPort.class, () -> mock(RelevantFactPort.class))
                .withBean(DividendAlertPublisherPort.class, () -> mock(DividendAlertPublisherPort.class))
                .withBean(GetCustodyPositionsUseCase.class, () -> mock(GetCustodyPositionsUseCase.class));
    }

    @Test
    void shouldInjectKernoCrawlerUserAgentFromApplicationYml() {
        baseContextRunner()
                .withInitializer(ctx -> {
                    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
                    try {
                        loader.load("application", new ClassPathResource("application.yml"))
                                .forEach(ps -> ctx.getEnvironment().getPropertySources().addLast(ps));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CvmOfficialCrawlerAdapter.class);
                    CvmOfficialCrawlerAdapter adapter = context.getBean(CvmOfficialCrawlerAdapter.class);
                    assertThat(ReflectionTestUtils.getField(adapter, "userAgent"))
                            .isEqualTo(EXPECTED_USER_AGENT);
                });
    }

    @Test
    void shouldFallbackToKernoCrawlerUserAgentDefaultWhenPropertyIsUnset() {
        baseContextRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CvmOfficialCrawlerAdapter.class);
                    CvmOfficialCrawlerAdapter adapter = context.getBean(CvmOfficialCrawlerAdapter.class);
                    assertThat(ReflectionTestUtils.getField(adapter, "userAgent"))
                            .isEqualTo(EXPECTED_USER_AGENT);
                });
    }
}
