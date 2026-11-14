package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.domain.AllocationScope;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository.GrahamRecommendationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class GrahamRecommendationRepositoryImpl implements GrahamRecommendationPort {

    private final GrahamRecommendationJpaRepository repository;

    @Override
    @Transactional
    public void saveRecommendations(String userId, List<GrahamRecommendation> recommendations) {
        saveRecommendations(userId, null, recommendations);
    }

    @Override
    @Transactional
    public void saveRecommendations(String userId, UUID tagId, List<GrahamRecommendation> recommendations) {
        String scopeKey = AllocationScope.fromTagId(tagId);
        repository.deleteByUserIdAndScopeKey(userId, scopeKey);
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }
        if (tagId != null && repository.countOwnedTag(tagId.toString(), userId) == 0) {
            log.warn("Skipping recommendation persistence because tag no longer exists for user={}, tagId={}",
                    userId, tagId);
            return;
        }

        String tagIdStr = tagId != null ? tagId.toString() : null;
        List<GrahamRecommendationEntity> entities = recommendations.stream()
                .map(r -> GrahamRecommendationEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .tagId(tagIdStr)
                        .scopeKey(scopeKey)
                        .ticker(r.getTicker())
                        .currentPrice(r.getCurrentPrice())
                        .intrinsicValue(r.getIntrinsicValue())
                        .marginOfSafety(r.getMarginOfSafety())
                        .currentAllocationPct(r.getCurrentAllocationPct())
                        .targetAllocationPct(r.getTargetAllocationPct())
                        .allocationGap(r.getAllocationGap())
                        .recommendationScore(r.getRecommendationScore())
                        .generatedAt(r.getGeneratedAt())
                        .epsUsed(r.getEpsUsed())
                        .bvpsUsed(r.getBvpsUsed())
                        .build())
                .toList();

        repository.saveAll(entities);
    }

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
                .epsUsed(entity.getEpsUsed())
                .bvpsUsed(entity.getBvpsUsed())
                .build();
    }
}
