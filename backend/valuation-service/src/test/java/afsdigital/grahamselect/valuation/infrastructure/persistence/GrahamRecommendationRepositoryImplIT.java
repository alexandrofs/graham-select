package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GrahamRecommendationRepositoryImplIT extends BaseRepositoryIT {

    @Autowired
    private GrahamRecommendationJpaRepository grahamRecommendationJpaRepository;

    @Autowired
    private GrahamRecommendationRepositoryImpl grahamRecommendationRepository;

    @AfterEach
    void tearDown() {
        grahamRecommendationJpaRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveRecommendations() {
        String userId = "user-123";
        GrahamRecommendation rec1 = GrahamRecommendation.builder()
                .ticker("PETR4")
                .currentPrice(new BigDecimal("30.00"))
                .intrinsicValue(new BigDecimal("40.00"))
                .marginOfSafety(new BigDecimal("0.33"))
                .currentAllocationPct(new BigDecimal("5.00"))
                .targetAllocationPct(new BigDecimal("20.00"))
                .allocationGap(new BigDecimal("15.00"))
                .recommendationScore(new BigDecimal("0.2580"))
                .generatedAt(LocalDate.now())
                .build();

        GrahamRecommendation rec2 = GrahamRecommendation.builder()
                .ticker("VALE3")
                .currentPrice(new BigDecimal("70.00"))
                .intrinsicValue(new BigDecimal("80.00"))
                .marginOfSafety(new BigDecimal("0.14"))
                .currentAllocationPct(new BigDecimal("8.00"))
                .targetAllocationPct(new BigDecimal("10.00"))
                .allocationGap(new BigDecimal("2.00"))
                .recommendationScore(new BigDecimal("0.0920"))
                .generatedAt(LocalDate.now())
                .build();

        grahamRecommendationRepository.saveRecommendations(userId, List.of(rec1, rec2));

        List<GrahamRecommendationEntity> entities = grahamRecommendationJpaRepository.findAll();
        assertThat(entities).hasSize(2);

        List<GrahamRecommendation> retrieved = grahamRecommendationRepository.findByUserId(userId);
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).getTicker()).isEqualTo("PETR4"); // High score first
        assertThat(retrieved.get(1).getTicker()).isEqualTo("VALE3");

        // Test delete on save recommendations with empty list
        grahamRecommendationRepository.saveRecommendations(userId, List.of());
        assertThat(grahamRecommendationJpaRepository.findAll()).isEmpty();
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackages = "afsdigital.grahamselect.valuation.infrastructure.persistence.jpa")
    @EntityScan(basePackages = "afsdigital.grahamselect.valuation.infrastructure.persistence.jpa")
    static class Config {
        @Bean
        public GrahamRecommendationRepositoryImpl grahamRecommendationRepository(GrahamRecommendationJpaRepository repo) {
            return new GrahamRecommendationRepositoryImpl(repo);
        }
    }
}
