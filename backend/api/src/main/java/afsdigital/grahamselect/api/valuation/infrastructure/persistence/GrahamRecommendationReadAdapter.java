package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GrahamRecommendationReadAdapter implements GrahamRecommendationPort {

    private final GrahamRecommendationJpaRepository repository;

    @Override
    public void saveRecommendations(String userId, List<GrahamRecommendation> recommendations) {
        throw new UnsupportedOperationException("api module is read-only for Graham recommendations");
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
        throw new UnsupportedOperationException("api module does not support global ranking retrieval");
    }

    private GrahamRecommendation toDomain(GrahamRecommendationEntity entity) {
        return GrahamRecommendation.builder()
                .id(entity.getId())
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
                .epsUsed(entity.getEpsUsed())
                .bvpsUsed(entity.getBvpsUsed())
                .build();
    }
}
