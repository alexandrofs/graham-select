package afsdigital.grahamselect.api.valuation.infrastructure.spring;

import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.application.usecase.GetRankedCompaniesUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RankingServiceConfiguration {

    @Bean
    public GetRankedCompaniesUseCase getRankedCompaniesUseCase(RankingRepository rankingRepository) {
        return new GetRankedCompaniesUseCase(rankingRepository);
    }
}
