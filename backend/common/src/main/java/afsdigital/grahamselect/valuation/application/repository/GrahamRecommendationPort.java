package afsdigital.grahamselect.valuation.application.repository;

import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;

import java.util.List;

public interface GrahamRecommendationPort {
    void saveRecommendations(String userId, List<GrahamRecommendation> recommendations);
    List<GrahamRecommendation> findByUserId(String userId);
    List<GrahamRecommendation> findAllOrderedByScore();
}
