package afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.entities.AssetTagEntity;
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
public interface JpaAssetTagRepository extends JpaRepository<AssetTagEntity, UUID> {

    List<AssetTagEntity> findAllByUserId(String userId);

    List<AssetTagEntity> findAllByUserIdAndTagId(String userId, UUID tagId);

    List<AssetTagEntity> findAllByUserIdAndTicker(String userId, String ticker);

    Optional<AssetTagEntity> findByUserIdAndTagIdAndTicker(String userId, UUID tagId, String ticker);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AssetTagEntity a WHERE a.userId = :userId AND a.tagId = :tagId AND a.ticker = :ticker")
    void deleteByUserIdAndTagIdAndTicker(
            @Param("userId") String userId,
            @Param("tagId") UUID tagId,
            @Param("ticker") String ticker
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AssetTagEntity a WHERE a.userId = :userId AND a.ticker = :ticker AND a.tagId IN :tagIds")
    void deleteByUserIdAndTickerAndTagIdIn(
            @Param("userId") String userId,
            @Param("ticker") String ticker,
            @Param("tagIds") Collection<UUID> tagIds
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AssetTagEntity a WHERE a.userId = :userId AND a.tagId = :tagId")
    void deleteAllByUserIdAndTagId(@Param("userId") String userId, @Param("tagId") UUID tagId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AssetTagEntity a WHERE a.userId = :userId AND a.ticker = :ticker")
    void deleteAllByUserIdAndTicker(@Param("userId") String userId, @Param("ticker") String ticker);
}
