package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class DeleteTagUseCase {

    private final PortfolioTagPort portfolioTagPort;
    private final AssetTagPort assetTagPort;

    public DeleteTagUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        this.portfolioTagPort = portfolioTagPort;
        this.assetTagPort = assetTagPort;
    }

    public void execute(String userId, UUID tagId) {
        String validUserId = TagInputValidator.validateUserId(userId);
        if (tagId == null) {
            IllegalArgumentException ex = new IllegalArgumentException("tagId é obrigatório.");
            log.error("Invalid tagId on delete for userId={}: {}", validUserId, ex.getMessage(), ex);
            throw ex;
        }

        portfolioTagPort.findByIdAndUserId(tagId, validUserId)
                .orElseThrow(() -> {
                    TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
                    log.error("Tag not found on delete userId={}, tagId={}: {}", validUserId, tagId, ex.getMessage(), ex);
                    return ex;
                });

        assetTagPort.deleteAllByUserIdAndTagId(validUserId, tagId);
        portfolioTagPort.deleteByIdAndUserId(tagId, validUserId);
    }
}
