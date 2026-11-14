package afsdigital.grahamselect.common.tags.application.repository;

import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetTagPort {

    AssetTag save(AssetTag assetTag);

    boolean saveIfAbsent(AssetTag assetTag);

    List<AssetTag> saveAll(List<AssetTag> assetTags);

    List<AssetTag> findAllByUserId(String userId);

    List<AssetTag> findAllByUserIdAndTagId(String userId, UUID tagId);

    List<AssetTag> findAllByUserIdAndTicker(String userId, String ticker);

    Optional<AssetTag> findByUserIdAndTagIdAndTicker(String userId, UUID tagId, String ticker);

    void deleteByUserIdAndTagIdAndTicker(String userId, UUID tagId, String ticker);

    void deleteByUserIdAndTickerAndTagIdIn(String userId, String ticker, Collection<UUID> tagIds);

    void deleteAllByUserIdAndTagId(String userId, UUID tagId);

    void deleteAllByUserIdAndTicker(String userId, String ticker);
}
