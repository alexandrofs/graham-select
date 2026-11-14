package afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.entities.AssetTagEntity;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaAssetTagRepository;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaPortfolioTagRepository;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetTagJpaAdapter implements AssetTagPort {

    private final JpaAssetTagRepository repository;
    private final JpaPortfolioTagRepository portfolioTagRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public AssetTag save(AssetTag assetTag) {
        try {
            AssetTagEntity saved = repository.saveAndFlush(toEntity(assetTag));
            return toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity error saving asset tag tagId={} ticker={} userId={}: {}",
                    assetTag.getTagId(), assetTag.getTicker(), assetTag.getUserId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public boolean saveIfAbsent(AssetTag assetTag) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            txTemplate.executeWithoutResult(status -> repository.saveAndFlush(toEntity(assetTag)));
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity error saving asset tag idempotently tagId={} ticker={} userId={}: {}",
                    assetTag.getTagId(), assetTag.getTicker(), assetTag.getUserId(), ex.getMessage(), ex);
            if (repository.findByUserIdAndTagIdAndTicker(
                    assetTag.getUserId(), assetTag.getTagId(), assetTag.getTicker()).isPresent()) {
                return false;
            }
            if (portfolioTagRepository.findByIdAndUserId(assetTag.getTagId(), assetTag.getUserId()).isEmpty()) {
                TagNotFoundException notFound = new TagNotFoundException("Tag não encontrada.");
                log.error("Tag deleted concurrently during assign tagId={} userId={}: {}",
                        assetTag.getTagId(), assetTag.getUserId(), notFound.getMessage(), notFound);
                throw notFound;
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public List<AssetTag> saveAll(List<AssetTag> assetTags) {
        try {
            List<AssetTagEntity> entities = assetTags.stream().map(this::toEntity).toList();
            List<AssetTagEntity> saved = repository.saveAllAndFlush(entities);
            return saved.stream().map(this::toDomain).toList();
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity error batch-saving {} asset tags: {}",
                    assetTags.size(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public List<AssetTag> findAllByUserId(String userId) {
        return repository.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AssetTag> findAllByUserIdAndTagId(String userId, UUID tagId) {
        return repository.findAllByUserIdAndTagId(userId, tagId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AssetTag> findAllByUserIdAndTicker(String userId, String ticker) {
        return repository.findAllByUserIdAndTicker(userId, ticker).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<AssetTag> findByUserIdAndTagIdAndTicker(String userId, UUID tagId, String ticker) {
        return repository.findByUserIdAndTagIdAndTicker(userId, tagId, ticker)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndTagIdAndTicker(String userId, UUID tagId, String ticker) {
        repository.deleteByUserIdAndTagIdAndTicker(userId, tagId, ticker);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndTickerAndTagIdIn(String userId, String ticker, Collection<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        repository.deleteByUserIdAndTickerAndTagIdIn(userId, ticker, tagIds);
    }

    @Override
    @Transactional
    public void deleteAllByUserIdAndTagId(String userId, UUID tagId) {
        repository.deleteAllByUserIdAndTagId(userId, tagId);
    }

    @Override
    @Transactional
    public void deleteAllByUserIdAndTicker(String userId, String ticker) {
        repository.deleteAllByUserIdAndTicker(userId, ticker);
    }

    private AssetTagEntity toEntity(AssetTag assetTag) {
        return AssetTagEntity.builder()
                .id(assetTag.getId())
                .userId(assetTag.getUserId())
                .tagId(assetTag.getTagId())
                .ticker(assetTag.getTicker())
                .createdAt(assetTag.getCreatedAt())
                .isNew(true)
                .build();
    }

    private AssetTag toDomain(AssetTagEntity entity) {
        return AssetTag.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tagId(entity.getTagId())
                .ticker(entity.getTicker())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
