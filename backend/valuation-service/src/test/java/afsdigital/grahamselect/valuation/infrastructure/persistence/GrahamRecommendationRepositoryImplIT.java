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
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class GrahamRecommendationRepositoryImplIT extends BaseRepositoryIT {

    @Autowired
    private GrahamRecommendationJpaRepository grahamRecommendationJpaRepository;

    @Autowired
    private GrahamRecommendationRepositoryImpl grahamRecommendationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        grahamRecommendationJpaRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM portfolio_tags");
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
                .epsUsed(new BigDecimal("4.5200"))
                .bvpsUsed(new BigDecimal("31.8000"))
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
        
        // Find entities.get(0) which is PETR4
        GrahamRecommendationEntity petrEntity = entities.stream()
                .filter(e -> "PETR4".equals(e.getTicker()))
                .findFirst()
                .orElseThrow();
        assertThat(petrEntity.getEpsUsed()).isEqualByComparingTo(new BigDecimal("4.5200"));
        assertThat(petrEntity.getBvpsUsed()).isEqualByComparingTo(new BigDecimal("31.8000"));

        List<GrahamRecommendation> retrieved = grahamRecommendationRepository.findByUserId(userId);
        assertThat(retrieved).hasSize(2);
        assertThat(retrieved.get(0).getTicker()).isEqualTo("PETR4"); // High score first
        assertThat(retrieved.get(0).getEpsUsed()).isEqualByComparingTo(new BigDecimal("4.5200"));
        assertThat(retrieved.get(0).getBvpsUsed()).isEqualByComparingTo(new BigDecimal("31.8000"));
        
        assertThat(retrieved.get(1).getTicker()).isEqualTo("VALE3");
        assertThat(retrieved.get(1).getEpsUsed()).isNull();
        assertThat(retrieved.get(1).getBvpsUsed()).isNull();

        // Test delete on save recommendations with empty list
        grahamRecommendationRepository.saveRecommendations(userId, List.of());
        assertThat(grahamRecommendationJpaRepository.findAll()).isEmpty();
    }

    @Test
    void shouldIsolateRecommendationsByTagScopeAndSkipMissingTagWithoutFkFailure() {
        String userId = "user-scoped-it";
        UUID tagId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO portfolio_tags (id, user_id, name, normalized_name, created_at) VALUES (?, ?, ?, ?, ?)",
                tagId.toString(), userId, "Aposentadoria", "aposentadoria", LocalDateTime.now(ZoneOffset.UTC)
        );

        GrahamRecommendation globalRec = GrahamRecommendation.builder()
                .ticker("PETR4")
                .currentPrice(new BigDecimal("30.00"))
                .intrinsicValue(new BigDecimal("40.00"))
                .marginOfSafety(new BigDecimal("0.33"))
                .currentAllocationPct(new BigDecimal("5.00"))
                .targetAllocationPct(new BigDecimal("20.00"))
                .allocationGap(new BigDecimal("15.00"))
                .recommendationScore(new BigDecimal("0.2580"))
                .generatedAt(LocalDate.now(ZoneOffset.UTC))
                .build();

        GrahamRecommendation tagRec = GrahamRecommendation.builder()
                .ticker("VALE3")
                .currentPrice(new BigDecimal("70.00"))
                .intrinsicValue(new BigDecimal("80.00"))
                .marginOfSafety(new BigDecimal("0.14"))
                .currentAllocationPct(new BigDecimal("8.00"))
                .targetAllocationPct(new BigDecimal("10.00"))
                .allocationGap(new BigDecimal("2.00"))
                .recommendationScore(new BigDecimal("0.1920"))
                .generatedAt(LocalDate.now(ZoneOffset.UTC))
                .build();

        grahamRecommendationRepository.saveRecommendations(userId, null, List.of(globalRec));
        grahamRecommendationRepository.saveRecommendations(userId, tagId, List.of(tagRec));

        assertThat(grahamRecommendationRepository.findByUserId(userId))
                .extracting(GrahamRecommendation::getTicker)
                .containsExactly("PETR4");
        assertThat(grahamRecommendationRepository.findByUserId(userId, tagId))
                .extracting(GrahamRecommendation::getTicker)
                .containsExactly("VALE3");

        // Deleting tag-scoped recommendations must preserve global scope
        grahamRecommendationRepository.saveRecommendations(userId, tagId, List.of());
        assertThat(grahamRecommendationRepository.findByUserId(userId, tagId)).isEmpty();
        assertThat(grahamRecommendationRepository.findByUserId(userId)).hasSize(1);

        // Deleted/missing tagId should skip cleanly without DataIntegrityViolationException
        grahamRecommendationRepository.saveRecommendations(userId, UUID.randomUUID(), List.of(tagRec));
        assertThat(grahamRecommendationRepository.findByUserId(userId)).hasSize(1);
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
