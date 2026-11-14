package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class UnassignAssetFromTagUseCase {

    private final PortfolioTagPort portfolioTagPort;
    private final AssetTagPort assetTagPort;

    public UnassignAssetFromTagUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        this.portfolioTagPort = portfolioTagPort;
        this.assetTagPort = assetTagPort;
    }

    public void execute(String userId, UUID tagId, String rawTicker) {
        String validUserId = TagInputValidator.validateUserId(userId);
        if (tagId == null) {
            IllegalArgumentException ex = new IllegalArgumentException("tagId é obrigatório.");
            log.error("Invalid tagId on unassign for userId={}: {}", validUserId, ex.getMessage(), ex);
            throw ex;
        }
        String normalizedTicker = TagInputValidator.validateAndNormalizeTicker(rawTicker);

        portfolioTagPort.findByIdAndUserId(tagId, validUserId)
                .orElseThrow(() -> {
                    TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
                    log.error("Tag not found on unassign userId={}, tagId={}, ticker={}: {}",
                            validUserId, tagId, normalizedTicker, ex.getMessage(), ex);
                    return ex;
                });

        assetTagPort.deleteByUserIdAndTagIdAndTicker(validUserId, tagId, normalizedTicker);
    }
}
