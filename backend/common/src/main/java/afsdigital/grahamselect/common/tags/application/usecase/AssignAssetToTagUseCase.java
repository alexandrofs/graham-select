package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
public class AssignAssetToTagUseCase {

    private final PortfolioTagPort portfolioTagPort;
    private final AssetTagPort assetTagPort;

    public AssignAssetToTagUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        this.portfolioTagPort = portfolioTagPort;
        this.assetTagPort = assetTagPort;
    }

    public String execute(String userId, UUID tagId, String rawTicker) {
        String validUserId = TagInputValidator.validateUserId(userId);
        if (tagId == null) {
            IllegalArgumentException ex = new IllegalArgumentException("tagId é obrigatório.");
            log.error("Invalid tagId on assign for userId={}: {}", validUserId, ex.getMessage(), ex);
            throw ex;
        }
        String normalizedTicker = TagInputValidator.validateAndNormalizeTicker(rawTicker);

        portfolioTagPort.findByIdAndUserId(tagId, validUserId)
                .orElseThrow(() -> {
                    TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
                    log.error("Tag not found on assign userId={}, tagId={}, ticker={}: {}",
                            validUserId, tagId, normalizedTicker, ex.getMessage(), ex);
                    return ex;
                });

        if (assetTagPort.findByUserIdAndTagIdAndTicker(validUserId, tagId, normalizedTicker).isPresent()) {
            return normalizedTicker;
        }

        int currentCount = assetTagPort.findAllByUserIdAndTicker(validUserId, normalizedTicker).size();
        if (currentCount >= TagInputValidator.MAX_TAGS_PER_ASSET) {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "Número máximo de tags por ativo excedido (máximo: 100).");
            log.error("Max tags per asset exceeded on assign userId={}, ticker={}, count={}: {}",
                    validUserId, normalizedTicker, currentCount, ex.getMessage(), ex);
            throw ex;
        }

        assetTagPort.saveIfAbsent(AssetTag.builder()
                .id(UUID.randomUUID())
                .userId(validUserId)
                .tagId(tagId)
                .ticker(normalizedTicker)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());
        return normalizedTicker;
    }
}
