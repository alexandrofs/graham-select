package afsdigital.grahamselect.api.valuation.infrastructure.spring;

import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.application.service.RankCompaniesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RankingServiceConfiguration {

    @Bean
    public RankCompaniesService rankCompaniesService(RankingRepository rankingRepository) {
        return new RankCompaniesService(rankingRepository);
    }
}
