package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.dto.AssetTagsAssignmentDto;
import afsdigital.grahamselect.common.tags.application.dto.TagSummaryDto;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetAssetTagsUseCase {

    private final PortfolioTagPort portfolioTagPort;
    private final AssetTagPort assetTagPort;

    public GetAssetTagsUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        this.portfolioTagPort = portfolioTagPort;
        this.assetTagPort = assetTagPort;
    }

    public AssetTagsAssignmentDto execute(String userId, String rawTicker) {
        String validUserId = TagInputValidator.validateUserId(userId);
        String normalizedTicker = TagInputValidator.validateAndNormalizeTicker(rawTicker);

        List<AssetTag> assetTags = assetTagPort.findAllByUserIdAndTicker(validUserId, normalizedTicker);
        if (assetTags.isEmpty()) {
            return new AssetTagsAssignmentDto(normalizedTicker, List.of());
        }

        Set<UUID> tagIds = assetTags.stream()
                .map(AssetTag::getTagId)
                .collect(Collectors.toSet());

        Map<UUID, PortfolioTag> tagsById = portfolioTagPort.findAllByUserIdAndIdIn(validUserId, tagIds).stream()
                .collect(Collectors.toMap(PortfolioTag::getId, Function.identity(), (a, b) -> a));

        List<TagSummaryDto> summaries = tagIds.stream()
                .map(tagsById::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PortfolioTag::getName, String.CASE_INSENSITIVE_ORDER))
                .map(tag -> new TagSummaryDto(tag.getId(), tag.getName(), tag.getColor()))
                .toList();

        return new AssetTagsAssignmentDto(normalizedTicker, summaries);
    }
}
