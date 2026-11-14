package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.GrahamRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrahamRecommendationJpaRepository extends JpaRepository<GrahamRecommendationEntity, String> {
    void deleteByUserId(String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GrahamRecommendationEntity g WHERE g.userId = :userId AND g.scopeKey = :scopeKey")
    void deleteByUserIdAndScopeKey(@Param("userId") String userId, @Param("scopeKey") String scopeKey);

    List<GrahamRecommendationEntity> findByUserIdOrderByRecommendationScoreDesc(String userId);
    List<GrahamRecommendationEntity> findByUserIdAndScopeKeyOrderByRecommendationScoreDesc(String userId, String scopeKey);
    List<GrahamRecommendationEntity> findAllByOrderByRecommendationScoreDesc();

    @Query(value = "SELECT COUNT(*) FROM portfolio_tags WHERE id = :tagId AND user_id = :userId", nativeQuery = true)
    long countOwnedTag(@Param("tagId") String tagId, @Param("userId") String userId);
}
