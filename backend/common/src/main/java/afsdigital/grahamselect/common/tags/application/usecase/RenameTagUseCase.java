package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.dto.TagDetailsDto;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.DuplicateTagException;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class RenameTagUseCase {

    private final PortfolioTagPort portfolioTagPort;
    private final AssetTagPort assetTagPort;

    public RenameTagUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        this.portfolioTagPort = portfolioTagPort;
        this.assetTagPort = assetTagPort;
    }

    public TagDetailsDto execute(String userId, UUID tagId, String rawName, String rawColor) {
        String validUserId = TagInputValidator.validateUserId(userId);
        if (tagId == null) {
            IllegalArgumentException ex = new IllegalArgumentException("tagId é obrigatório.");
            log.error("Invalid tagId on rename for userId={}: {}", validUserId, ex.getMessage(), ex);
            throw ex;
        }

        PortfolioTag existing = portfolioTagPort.findByIdAndUserId(tagId, validUserId)
                .orElseThrow(() -> {
                    TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
                    log.error("Tag not found on rename userId={}, tagId={}: {}", validUserId, tagId, ex.getMessage(), ex);
                    return ex;
                });

        String validName = TagInputValidator.validateAndNormalizeName(rawName);
        String normalizedName = TagInputValidator.toNormalizedNameKey(validName);
        String resolvedColor = (rawColor == null || rawColor.isBlank())
                ? (existing.getColor() != null ? existing.getColor() : TagInputValidator.DEFAULT_COLOR)
                : TagInputValidator.validateAndNormalizeColor(rawColor);

        Optional<PortfolioTag> conflicting = portfolioTagPort.findByUserIdAndNormalizedName(validUserId, normalizedName);
        if (conflicting.isPresent() && !conflicting.get().getId().equals(tagId)) {
            DuplicateTagException ex = new DuplicateTagException("Já existe uma tag com o nome \"" + validName + "\".");
            log.error("Duplicate tag on rename userId={}, tagId={}, normalizedName={}: {}",
                    validUserId, tagId, normalizedName, ex.getMessage(), ex);
            throw ex;
        }

        existing.setName(validName);
        existing.setNormalizedName(normalizedName);
        existing.setColor(resolvedColor);
        existing.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        PortfolioTag updated = portfolioTagPort.update(existing);
        List<AssetTag> assetTags = assetTagPort.findAllByUserIdAndTagId(validUserId, tagId);
        return TagDetailsMapper.toDetailsDto(updated, assetTags);
    }
}
