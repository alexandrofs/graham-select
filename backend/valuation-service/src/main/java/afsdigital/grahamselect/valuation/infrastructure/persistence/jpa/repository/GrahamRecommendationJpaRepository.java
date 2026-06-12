package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrahamRecommendationJpaRepository extends JpaRepository<GrahamRecommendationEntity, String> {
    void deleteByUserId(String userId);
    List<GrahamRecommendationEntity> findByUserIdOrderByRecommendationScoreDesc(String userId);
    List<GrahamRecommendationEntity> findAllByOrderByRecommendationScoreDesc();
}
