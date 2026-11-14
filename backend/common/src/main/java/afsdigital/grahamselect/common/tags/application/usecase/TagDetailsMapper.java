package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.dto.TagDetailsDto;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;

import java.util.List;
import java.util.Locale;

final class TagDetailsMapper {

    private TagDetailsMapper() {
    }

    static TagDetailsDto toDetailsDto(PortfolioTag tag, List<AssetTag> assetTags) {
        List<String> tickers = assetTags.stream()
                .map(AssetTag::getTicker)
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
        return new TagDetailsDto(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tickers,
                tickers.size(),
                tag.getCreatedAt(),
                tag.getUpdatedAt()
        );
    }
}
