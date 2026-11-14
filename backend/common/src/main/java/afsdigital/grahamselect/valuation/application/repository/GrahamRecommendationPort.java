package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;

import java.util.List;
import java.util.UUID;

public interface GrahamRecommendationPort {
    void saveRecommendations(String userId, List<GrahamRecommendation> recommendations);
    List<GrahamRecommendation> findByUserId(String userId);
    List<GrahamRecommendation> findAllOrderedByScore();

    default void saveRecommendations(String userId, UUID tagId, List<GrahamRecommendation> recommendations) {
        saveRecommendations(userId, recommendations);
    }

    default List<GrahamRecommendation> findByUserId(String userId, UUID tagId) {
        return findByUserId(userId);
    }
}
