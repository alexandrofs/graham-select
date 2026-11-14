package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.dto.TagDetailsDto;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ListTagsUseCase {

    private final PortfolioTagPort portfolioTagPort;
    private final AssetTagPort assetTagPort;

    public ListTagsUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        this.portfolioTagPort = portfolioTagPort;
        this.assetTagPort = assetTagPort;
    }

    public List<TagDetailsDto> execute(String userId) {
        String validUserId = TagInputValidator.validateUserId(userId);
        List<PortfolioTag> tags = portfolioTagPort.findAllByUserId(validUserId);
        Map<UUID, List<AssetTag>> assetsByTagId = assetTagPort.findAllByUserId(validUserId).stream()
                .collect(Collectors.groupingBy(AssetTag::getTagId));

        return tags.stream()
                .sorted(Comparator
                        .comparing(PortfolioTag::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PortfolioTag::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(tag -> TagDetailsMapper.toDetailsDto(
                        tag,
                        assetsByTagId.getOrDefault(tag.getId(), List.of())
                ))
                .toList();
    }
}
