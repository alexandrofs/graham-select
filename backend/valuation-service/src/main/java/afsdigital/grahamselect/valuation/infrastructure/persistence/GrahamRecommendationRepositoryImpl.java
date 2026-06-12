package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class GrahamRecommendationRepositoryImpl implements GrahamRecommendationPort {

    private final GrahamRecommendationJpaRepository repository;

    @Override
    @Transactional
    public void saveRecommendations(String userId, List<GrahamRecommendation> recommendations) {
        repository.deleteByUserId(userId);
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }

        List<GrahamRecommendationEntity> entities = recommendations.stream()
                .map(r -> GrahamRecommendationEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .ticker(r.getTicker())
                        .currentPrice(r.getCurrentPrice())
                        .intrinsicValue(r.getIntrinsicValue())
                        .marginOfSafety(r.getMarginOfSafety())
                        .currentAllocationPct(r.getCurrentAllocationPct())
                        .targetAllocationPct(r.getTargetAllocationPct())
                        .allocationGap(r.getAllocationGap())
                        .recommendationScore(r.getRecommendationScore())
                        .generatedAt(r.getGeneratedAt())
                        .build())
                .toList();

        repository.saveAll(entities);
    }

    @Override
    public List<GrahamRecommendation> findByUserId(String userId) {
        List<GrahamRecommendationEntity> entities = repository.findByUserIdOrderByRecommendationScoreDesc(userId);
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<GrahamRecommendation> findAllOrderedByScore() {
        List<GrahamRecommendationEntity> entities = repository.findAllByOrderByRecommendationScoreDesc();
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    private GrahamRecommendation toDomain(GrahamRecommendationEntity entity) {
        return GrahamRecommendation.builder()
                .id(UUID.fromString(entity.getId()))
                .userId(entity.getUserId())
                .ticker(entity.getTicker())
                .currentPrice(entity.getCurrentPrice())
                .intrinsicValue(entity.getIntrinsicValue())
                .marginOfSafety(entity.getMarginOfSafety())
                .currentAllocationPct(entity.getCurrentAllocationPct())
                .targetAllocationPct(entity.getTargetAllocationPct())
                .allocationGap(entity.getAllocationGap())
                .recommendationScore(entity.getRecommendationScore())
                .generatedAt(entity.getGeneratedAt())
                .build();
    }
}
