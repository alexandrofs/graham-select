package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.dto.TagDetailsDto;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.DuplicateTagException;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
public class CreateTagUseCase {

    private final PortfolioTagPort portfolioTagPort;

    public CreateTagUseCase(PortfolioTagPort portfolioTagPort) {
        this.portfolioTagPort = portfolioTagPort;
    }

    public TagDetailsDto execute(String userId, String rawName, String rawColor) {
        String validUserId = TagInputValidator.validateUserId(userId);
        String validName = TagInputValidator.validateAndNormalizeName(rawName);
        String normalizedName = TagInputValidator.toNormalizedNameKey(validName);
        String validColor = TagInputValidator.validateAndNormalizeColor(rawColor);

        if (portfolioTagPort.findByUserIdAndNormalizedName(validUserId, normalizedName).isPresent()) {
            DuplicateTagException ex = new DuplicateTagException("Já existe uma tag com o nome \"" + validName + "\".");
            log.error("Duplicate tag on create for userId={}, normalizedName={}: {}", validUserId, normalizedName, ex.getMessage(), ex);
            throw ex;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PortfolioTag tag = PortfolioTag.builder()
                .id(UUID.randomUUID())
                .userId(validUserId)
                .name(validName)
                .normalizedName(normalizedName)
                .color(validColor)
                .createdAt(now)
                .updatedAt(null)
                .build();

        PortfolioTag created = portfolioTagPort.create(tag);
        return TagDetailsMapper.toDetailsDto(created, List.of());
    }
}
