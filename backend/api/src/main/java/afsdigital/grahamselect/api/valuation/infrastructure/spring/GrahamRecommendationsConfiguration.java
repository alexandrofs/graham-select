package afsdigital.grahamselect.api.valuation.infrastructure.spring;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.GrahamRecommendationReadAdapter;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.application.usecase.GetGrahamRecommendationsUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrahamRecommendationsConfiguration {

    @Bean("apiGrahamRecommendationPort")
    public GrahamRecommendationPort grahamRecommendationPort(
            @Qualifier("apiGrahamRecommendationJpaRepository") GrahamRecommendationJpaRepository repository) {
        return new GrahamRecommendationReadAdapter(repository);
    }

    @Bean
    public GetGrahamRecommendationsUseCase getGrahamRecommendationsUseCase(
            @Qualifier("apiGrahamRecommendationPort") GrahamRecommendationPort port) {
        return new GetGrahamRecommendationsUseCase(port);
    }
}
