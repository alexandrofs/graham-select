package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository("apiGrahamRecommendationJpaRepository")
public interface GrahamRecommendationJpaRepository extends JpaRepository<GrahamRecommendationEntity, UUID> {
    List<GrahamRecommendationEntity> findByUserIdOrderByRecommendationScoreDesc(String userId);
}
