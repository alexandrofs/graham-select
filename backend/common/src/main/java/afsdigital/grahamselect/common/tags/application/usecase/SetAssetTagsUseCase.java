package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.dto.AssetTagsAssignmentDto;
import afsdigital.grahamselect.common.tags.application.dto.TagSummaryDto;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class SetAssetTagsUseCase {

    private final PortfolioTagPort portfolioTagPort;
    private final AssetTagPort assetTagPort;

    public SetAssetTagsUseCase(PortfolioTagPort portfolioTagPort, AssetTagPort assetTagPort) {
        this.portfolioTagPort = portfolioTagPort;
        this.assetTagPort = assetTagPort;
    }

    public AssetTagsAssignmentDto execute(String userId, String rawTicker, List<UUID> rawTagIds) {
        String validUserId = TagInputValidator.validateUserId(userId);
        String normalizedTicker = TagInputValidator.validateAndNormalizeTicker(rawTicker);

        if (rawTagIds == null) {
            IllegalArgumentException ex = new IllegalArgumentException("Lista de tagIds é obrigatória.");
            log.error("Null tagIds list for userId={}, ticker={}: {}", validUserId, normalizedTicker, ex.getMessage(), ex);
            throw ex;
        }
        if (rawTagIds.stream().anyMatch(Objects::isNull)) {
            IllegalArgumentException ex = new IllegalArgumentException("Nenhum tagId pode ser nulo.");
            log.error("Null tagId element for userId={}, ticker={}: {}", validUserId, normalizedTicker, ex.getMessage(), ex);
            throw ex;
        }

        List<UUID> desiredTagIds = rawTagIds.stream().distinct().toList();
        if (desiredTagIds.size() > TagInputValidator.MAX_TAGS_PER_ASSET) {
            IllegalArgumentException ex = new IllegalArgumentException("Número máximo de tags por ativo excedido (máximo: 100).");
            log.error("Max tags per asset exceeded for userId={}, ticker={}, count={}: {}",
                    validUserId, normalizedTicker, desiredTagIds.size(), ex.getMessage(), ex);
            throw ex;
        }

        Map<UUID, PortfolioTag> userTagsById = desiredTagIds.isEmpty()
                ? Map.of()
                : portfolioTagPort.findAllByUserIdAndIdIn(validUserId, desiredTagIds).stream()
                        .collect(Collectors.toMap(PortfolioTag::getId, Function.identity(), (a, b) -> a));

        for (UUID tagId : desiredTagIds) {
            if (!userTagsById.containsKey(tagId)) {
                TagNotFoundException ex = new TagNotFoundException("Tag não encontrada: " + tagId);
                log.error("Tag not found in setAssetTags userId={}, ticker={}, tagId={}: {}",
                        validUserId, normalizedTicker, tagId, ex.getMessage(), ex);
                throw ex;
            }
        }

        List<AssetTag> currentAssignments = assetTagPort.findAllByUserIdAndTicker(validUserId, normalizedTicker);
        Set<UUID> desiredSet = new LinkedHashSet<>(desiredTagIds);
        Set<UUID> existingSet = currentAssignments.stream()
                .map(AssetTag::getTagId)
                .collect(Collectors.toSet());

        List<UUID> removedTagIds = currentAssignments.stream()
                .map(AssetTag::getTagId)
                .filter(tagId -> !desiredSet.contains(tagId))
                .distinct()
                .toList();
        if (!removedTagIds.isEmpty()) {
            assetTagPort.deleteByUserIdAndTickerAndTagIdIn(validUserId, normalizedTicker, removedTagIds);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<AssetTag> toCreate = new ArrayList<>();
        for (UUID tagId : desiredTagIds) {
            if (!existingSet.contains(tagId)) {
                toCreate.add(AssetTag.builder()
                        .id(UUID.randomUUID())
                        .userId(validUserId)
                        .tagId(tagId)
                        .ticker(normalizedTicker)
                        .createdAt(now)
                        .build());
            }
        }
        if (!toCreate.isEmpty()) {
            assetTagPort.saveAll(toCreate);
        }

        List<TagSummaryDto> summaries = desiredTagIds.stream()
                .map(userTagsById::get)
                .sorted(Comparator.comparing(PortfolioTag::getName, String.CASE_INSENSITIVE_ORDER))
                .map(tag -> new TagSummaryDto(tag.getId(), tag.getName(), tag.getColor()))
                .toList();

        return new AssetTagsAssignmentDto(normalizedTicker, summaries);
    }
}
