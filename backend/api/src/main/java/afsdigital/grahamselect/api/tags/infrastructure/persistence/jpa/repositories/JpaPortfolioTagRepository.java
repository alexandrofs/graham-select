package afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.entities.PortfolioTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPortfolioTagRepository extends JpaRepository<PortfolioTagEntity, UUID> {

    List<PortfolioTagEntity> findAllByUserId(String userId);

    List<PortfolioTagEntity> findAllByUserIdAndIdIn(String userId, Collection<UUID> ids);

    Optional<PortfolioTagEntity> findByIdAndUserId(UUID id, String userId);

    Optional<PortfolioTagEntity> findByUserIdAndNormalizedName(String userId, String normalizedName);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PortfolioTagEntity t WHERE t.id = :id AND t.userId = :userId")
    void deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);
}
