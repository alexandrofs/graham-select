package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;

import java.util.List;
import java.util.UUID;

public interface GrahamRecommendationReadPort {
    List<GrahamRecommendation> findByUserId(String userId);

    default List<GrahamRecommendation> findByUserId(String userId, UUID tagId) {
        return findByUserId(userId);
    }
}
