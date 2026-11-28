package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;

import java.util.List;
import java.util.UUID;

public interface GrahamRecommendationWritePort {
    void saveRecommendations(String userId, List<GrahamRecommendation> recommendations);
    List<GrahamRecommendation> findAllOrderedByScore();

    default void saveRecommendations(String userId, UUID tagId, List<GrahamRecommendation> recommendations) {
        saveRecommendations(userId, recommendations);
    }
}
