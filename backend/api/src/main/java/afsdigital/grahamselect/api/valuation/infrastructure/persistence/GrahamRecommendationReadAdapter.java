package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationReadPort;
import afsdigital.grahamselect.valuation.domain.AllocationScope;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class GrahamRecommendationReadAdapter implements GrahamRecommendationReadPort {

    private final GrahamRecommendationJpaRepository repository;

    @Override
    public List<GrahamRecommendation> findByUserId(String userId) {
        return findByUserId(userId, null);
    }

    @Override
    public List<GrahamRecommendation> findByUserId(String userId, UUID tagId) {
        String scopeKey = AllocationScope.fromTagId(tagId);
        List<GrahamRecommendationEntity> entities = repository.findByUserIdAndScopeKeyOrderByRecommendationScoreDesc(userId, scopeKey);
        return entities.stream()
                .map(this::toDomain)
                .toList();
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
