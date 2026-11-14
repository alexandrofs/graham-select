package afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa;

import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.entities.PortfolioTagEntity;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaPortfolioTagRepository;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.DuplicateTagException;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioTagJpaAdapter implements PortfolioTagPort {

    private final JpaPortfolioTagRepository repository;

    @Override
    @Transactional
    public PortfolioTag create(PortfolioTag tag) {
        PortfolioTagEntity entity = PortfolioTagEntity.builder()
                .id(tag.getId())
                .userId(tag.getUserId())
                .name(tag.getName())
                .normalizedName(tag.getNormalizedName())
                .color(tag.getColor())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .isNew(true)
                .build();

        try {
            return toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            log.error("Constraint violation creating tag name={} userId={}: {}",
                    tag.getName(), tag.getUserId(), ex.getMessage(), ex);
            if (isDuplicateNameViolation(ex)) {
                throw new DuplicateTagException("Já existe uma tag com o nome \"" + tag.getName() + "\".");
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public PortfolioTag update(PortfolioTag tag) {
        PortfolioTagEntity entity = repository.findByIdAndUserId(tag.getId(), tag.getUserId())
                .orElseThrow(() -> {
                    TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
                    log.error("Tag not found on update tagId={} userId={}: {}",
                            tag.getId(), tag.getUserId(), ex.getMessage(), ex);
                    return ex;
                });

        entity.setName(tag.getName());
        entity.setNormalizedName(tag.getNormalizedName());
        entity.setColor(tag.getColor());
        entity.setUpdatedAt(tag.getUpdatedAt());

        try {
            return toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            log.error("Constraint violation updating tag tagId={} userId={}: {}",
                    tag.getId(), tag.getUserId(), ex.getMessage(), ex);
            if (isDuplicateNameViolation(ex)) {
                throw new DuplicateTagException("Já existe uma tag com o nome \"" + tag.getName() + "\".");
            }
            throw ex;
        }
    }

    @Override
    public List<PortfolioTag> findAllByUserId(String userId) {
        return repository.findAllByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PortfolioTag> findAllByUserIdAndIdIn(String userId, Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllByUserIdAndIdIn(userId, ids).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<PortfolioTag> findByIdAndUserId(UUID id, String userId) {
        return repository.findByIdAndUserId(id, userId)
                .map(this::toDomain);
    }

    @Override
    public Optional<PortfolioTag> findByUserIdAndNormalizedName(String userId, String normalizedName) {
        return repository.findByUserIdAndNormalizedName(userId, normalizedName)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteByIdAndUserId(UUID id, String userId) {
        repository.deleteByIdAndUserId(id, userId);
    }

    private boolean isDuplicateNameViolation(DataIntegrityViolationException ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof ConstraintViolationException cve) {
                String constraintName = cve.getConstraintName();
                if (constraintName != null
                        && constraintName.toLowerCase(Locale.ROOT).contains("uq_portfolio_tags_user_normalized_name")) {
                    return true;
                }
            }
            String message = cursor.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("uq_portfolio_tags_user_normalized_name")
                        || lower.contains("duplicate entry")
                        || lower.contains("unique index")
                        || lower.contains("unique constraint")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private PortfolioTag toDomain(PortfolioTagEntity entity) {
        return PortfolioTag.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .normalizedName(entity.getNormalizedName())
                .color(entity.getColor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
