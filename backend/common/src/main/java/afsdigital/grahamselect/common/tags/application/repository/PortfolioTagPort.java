package afsdigital.grahamselect.common.tags.application.repository;

import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioTagPort {

    PortfolioTag create(PortfolioTag tag);

    PortfolioTag update(PortfolioTag tag);

    List<PortfolioTag> findAllByUserId(String userId);

    List<PortfolioTag> findAllByUserIdAndIdIn(String userId, Collection<UUID> ids);

    Optional<PortfolioTag> findByIdAndUserId(UUID id, String userId);

    Optional<PortfolioTag> findByUserIdAndNormalizedName(String userId, String normalizedName);

    void deleteByIdAndUserId(UUID id, String userId);
}
