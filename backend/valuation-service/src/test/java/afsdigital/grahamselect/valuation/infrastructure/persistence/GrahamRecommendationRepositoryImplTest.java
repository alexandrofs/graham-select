package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrahamRecommendationRepositoryImplTest {

    @Mock
    private GrahamRecommendationJpaRepository repository;

    @InjectMocks
    private GrahamRecommendationRepositoryImpl recommendationRepository;

    @Test
    void shouldSaveRecommendationsSuccessfully() {
        String userId = "user-123";
        GrahamRecommendation rec = GrahamRecommendation.builder()
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

        recommendationRepository.saveRecommendations(userId, List.of(rec));

        verify(repository, times(1)).deleteByUserId(userId);
        
        ArgumentCaptor<List<GrahamRecommendationEntity>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository, times(1)).saveAll(listCaptor.capture());
        
        List<GrahamRecommendationEntity> savedList = listCaptor.getValue();
        assertThat(savedList).hasSize(1);
        GrahamRecommendationEntity entity = savedList.get(0);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getTicker()).isEqualTo("PETR4");
        assertThat(entity.getCurrentPrice()).isEqualByComparingTo("30.00");
    }

    @Test
    void shouldDeleteRecommendationsWhenEmptyListProvided() {
        String userId = "user-123";
        recommendationRepository.saveRecommendations(userId, List.of());

        verify(repository, times(1)).deleteByUserId(userId);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void shouldFindRecommendationsByUserId() {
        String userId = "user-123";
        GrahamRecommendationEntity entity = GrahamRecommendationEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
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

        when(repository.findByUserIdOrderByRecommendationScoreDesc(userId)).thenReturn(List.of(entity));

        List<GrahamRecommendation> result = recommendationRepository.findByUserId(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTicker()).isEqualTo("VALE3");
        assertThat(result.get(0).getRecommendationScore()).isEqualByComparingTo("0.0920");
    }

    @Test
    void shouldFindAllOrderedByScore() {
        GrahamRecommendationEntity entity = GrahamRecommendationEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId("user-123")
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

        when(repository.findAllByOrderByRecommendationScoreDesc()).thenReturn(List.of(entity));

        List<GrahamRecommendation> result = recommendationRepository.findAllOrderedByScore();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTicker()).isEqualTo("VALE3");
    }
}
