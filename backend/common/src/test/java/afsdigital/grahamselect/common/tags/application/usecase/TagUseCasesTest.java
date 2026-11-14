package afsdigital.grahamselect.common.tags.application.usecase;

import afsdigital.grahamselect.common.tags.application.dto.AssetTagsAssignmentDto;
import afsdigital.grahamselect.common.tags.application.dto.TagDetailsDto;
import afsdigital.grahamselect.common.tags.application.dto.TagSummaryDto;
import afsdigital.grahamselect.common.tags.application.repository.AssetTagPort;
import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.DuplicateTagException;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.common.tags.domain.TagInputValidator;
import afsdigital.grahamselect.common.tags.domain.entities.AssetTag;
import afsdigital.grahamselect.common.tags.domain.entities.PortfolioTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagUseCasesTest {

    @Mock
    private PortfolioTagPort portfolioTagPort;

    @Mock
    private AssetTagPort assetTagPort;

    private CreateTagUseCase createTagUseCase;
    private ListTagsUseCase listTagsUseCase;
    private GetAssetTagsUseCase getAssetTagsUseCase;
    private RenameTagUseCase renameTagUseCase;
    private DeleteTagUseCase deleteTagUseCase;
    private SetAssetTagsUseCase setAssetTagsUseCase;
    private AssignAssetToTagUseCase assignAssetToTagUseCase;
    private UnassignAssetFromTagUseCase unassignAssetFromTagUseCase;

    @BeforeEach
    void setUp() {
        createTagUseCase = new CreateTagUseCase(portfolioTagPort);
        listTagsUseCase = new ListTagsUseCase(portfolioTagPort, assetTagPort);
        getAssetTagsUseCase = new GetAssetTagsUseCase(portfolioTagPort, assetTagPort);
        renameTagUseCase = new RenameTagUseCase(portfolioTagPort, assetTagPort);
        deleteTagUseCase = new DeleteTagUseCase(portfolioTagPort, assetTagPort);
        setAssetTagsUseCase = new SetAssetTagsUseCase(portfolioTagPort, assetTagPort);
        assignAssetToTagUseCase = new AssignAssetToTagUseCase(portfolioTagPort, assetTagPort);
        unassignAssetFromTagUseCase = new UnassignAssetFromTagUseCase(portfolioTagPort, assetTagPort);
    }

    @Test
    void shouldCreateTagWithNormalizedNameAndDefaultColor() {
        when(portfolioTagPort.findByUserIdAndNormalizedName("user-a", "aposentadoria"))
                .thenReturn(Optional.empty());
        when(portfolioTagPort.create(any(PortfolioTag.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TagDetailsDto created = createTagUseCase.execute("user-a", "  Aposentadoria  ", null);

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Aposentadoria");
        assertThat(created.color()).isEqualTo("#6366F1");
        assertThat(created.tickers()).isEmpty();
        assertThat(created.assetCount()).isZero();
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNull();
    }

    @Test
    void shouldRejectDuplicateTagCaseInsensitiveForSameUser() {
        PortfolioTag existing = PortfolioTag.builder()
                .id(UUID.randomUUID())
                .userId("user-a")
                .name("Dividendos")
                .normalizedName("dividendos")
                .color("#10B981")
                .build();

        when(portfolioTagPort.findByUserIdAndNormalizedName("user-a", "dividendos"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> createTagUseCase.execute("user-a", "dividendos", "#6366F1"))
                .isInstanceOf(DuplicateTagException.class);
        verify(portfolioTagPort, never()).create(any());
    }

    @Test
    void shouldListTagsSortedAlphabeticallyWithAggregatedTickers() {
        UUID tag1Id = UUID.randomUUID();
        UUID tag2Id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        PortfolioTag tagZ = PortfolioTag.builder()
                .id(tag1Id)
                .userId("user-a")
                .name("Renda Fixa")
                .normalizedName("renda fixa")
                .color("#F59E0B")
                .createdAt(now)
                .build();
        PortfolioTag tagA = PortfolioTag.builder()
                .id(tag2Id)
                .userId("user-a")
                .name("aposentadoria")
                .normalizedName("aposentadoria")
                .color("#6366F1")
                .createdAt(now)
                .build();

        when(portfolioTagPort.findAllByUserId("user-a")).thenReturn(List.of(tagZ, tagA));
        when(assetTagPort.findAllByUserId("user-a")).thenReturn(List.of(
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(tag2Id).ticker("WEGE3").build(),
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(tag2Id).ticker("PETR4").build(),
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(tag1Id).ticker("CDB BANCO X").build()
        ));

        List<TagDetailsDto> result = listTagsUseCase.execute("user-a");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("aposentadoria");
        assertThat(result.get(0).tickers()).containsExactly("PETR4", "WEGE3");
        assertThat(result.get(0).assetCount()).isEqualTo(2);

        assertThat(result.get(1).name()).isEqualTo("Renda Fixa");
        assertThat(result.get(1).tickers()).containsExactly("CDB BANCO X");
        assertThat(result.get(1).assetCount()).isEqualTo(1);
    }

    @Test
    void shouldGetAssetTagsSortedAndNormalized() {
        UUID tag1Id = UUID.randomUUID();
        UUID tag2Id = UUID.randomUUID();

        when(assetTagPort.findAllByUserIdAndTicker("user-a", "CDB BANCO X")).thenReturn(List.of(
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(tag1Id).ticker("CDB BANCO X").build(),
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(tag2Id).ticker("CDB BANCO X").build()
        ));
        when(portfolioTagPort.findAllByUserIdAndIdIn(eq("user-a"), anyCollection())).thenReturn(List.of(
                PortfolioTag.builder().id(tag1Id).userId("user-a").name("Reserva").color("#10B981").build(),
                PortfolioTag.builder().id(tag2Id).userId("user-a").name("Aposentadoria").color("#6366F1").build()
        ));

        AssetTagsAssignmentDto dto = getAssetTagsUseCase.execute("user-a", "  cdb banco x  ");

        assertThat(dto.ticker()).isEqualTo("CDB BANCO X");
        assertThat(dto.tags()).extracting(TagSummaryDto::name)
                .containsExactly("Aposentadoria", "Reserva");
    }

    @Test
    void shouldRenameTagAllowingOwnCapitalizationAndColorChangeWithoutFalseConflict() {
        UUID tagId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC).minusDays(1);
        PortfolioTag existing = PortfolioTag.builder()
                .id(tagId)
                .userId("user-a")
                .name("aposentadoria")
                .normalizedName("aposentadoria")
                .color("#6366F1")
                .createdAt(createdAt)
                .build();

        when(portfolioTagPort.findByIdAndUserId(tagId, "user-a")).thenReturn(Optional.of(existing));
        when(portfolioTagPort.findByUserIdAndNormalizedName("user-a", "aposentadoria"))
                .thenReturn(Optional.of(existing));
        when(portfolioTagPort.update(any(PortfolioTag.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assetTagPort.findAllByUserIdAndTagId("user-a", tagId)).thenReturn(List.of(
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(tagId).ticker("PETR4").build()
        ));

        TagDetailsDto updated = renameTagUseCase.execute("user-a", tagId, "Aposentadoria", "#10b981");

        assertThat(updated.name()).isEqualTo("Aposentadoria");
        assertThat(updated.color()).isEqualTo("#10B981");
        assertThat(updated.updatedAt()).isNotNull();
        assertThat(updated.tickers()).containsExactly("PETR4");
        assertThat(updated.assetCount()).isEqualTo(1);
    }

    @Test
    void shouldPreserveExistingColorOnRenameWhenColorIsNull() {
        UUID tagId = UUID.randomUUID();
        PortfolioTag existing = PortfolioTag.builder()
                .id(tagId)
                .userId("user-a")
                .name("Aposentadoria")
                .normalizedName("aposentadoria")
                .color("#8B5CF6")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        when(portfolioTagPort.findByIdAndUserId(tagId, "user-a")).thenReturn(Optional.of(existing));
        when(portfolioTagPort.findByUserIdAndNormalizedName("user-a", "previdência"))
                .thenReturn(Optional.empty());
        when(portfolioTagPort.update(any(PortfolioTag.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assetTagPort.findAllByUserIdAndTagId("user-a", tagId)).thenReturn(List.of());

        TagDetailsDto updated = renameTagUseCase.execute("user-a", tagId, "Previdência", null);

        assertThat(updated.name()).isEqualTo("Previdência");
        assertThat(updated.color()).isEqualTo("#8B5CF6");
    }

    @Test
    void shouldRejectRenameConflictWithAnotherTagAndMissingTag() {
        UUID tagId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        PortfolioTag existing = PortfolioTag.builder()
                .id(tagId)
                .userId("user-a")
                .name("Aposentadoria")
                .normalizedName("aposentadoria")
                .color("#6366F1")
                .build();
        PortfolioTag conflicting = PortfolioTag.builder()
                .id(otherId)
                .userId("user-a")
                .name("Longo Prazo")
                .normalizedName("longo prazo")
                .color("#10B981")
                .build();

        when(portfolioTagPort.findByIdAndUserId(tagId, "user-a")).thenReturn(Optional.of(existing));
        when(portfolioTagPort.findByUserIdAndNormalizedName("user-a", "longo prazo"))
                .thenReturn(Optional.of(conflicting));

        assertThatThrownBy(() -> renameTagUseCase.execute("user-a", tagId, "Longo Prazo", null))
                .isInstanceOf(DuplicateTagException.class);

        when(portfolioTagPort.findByIdAndUserId(otherId, "user-b")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> renameTagUseCase.execute("user-b", otherId, "Qualquer", null))
                .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void shouldDeleteTagAndItsAssetAssociationsOnlyAfterOwnershipCheck() {
        UUID tagId = UUID.randomUUID();
        PortfolioTag existing = PortfolioTag.builder().id(tagId).userId("user-a").name("Temp").build();

        when(portfolioTagPort.findByIdAndUserId(tagId, "user-a")).thenReturn(Optional.of(existing));

        deleteTagUseCase.execute("user-a", tagId);

        verify(assetTagPort).deleteAllByUserIdAndTagId("user-a", tagId);
        verify(portfolioTagPort).deleteByIdAndUserId(tagId, "user-a");

        when(portfolioTagPort.findByIdAndUserId(tagId, "user-b")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> deleteTagUseCase.execute("user-b", tagId))
                .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void shouldSetAssetTagsUsingDiffAndValidateOwnershipBeforeMutating() {
        UUID keepId = UUID.randomUUID();
        UUID removeId = UUID.randomUUID();
        UUID addId = UUID.randomUUID();
        UUID foreignId = UUID.randomUUID();

        PortfolioTag keepTag = PortfolioTag.builder().id(keepId).userId("user-a").name("Aposentadoria").color("#6366F1").build();
        PortfolioTag addTag = PortfolioTag.builder().id(addId).userId("user-a").name("Longo Prazo").color("#10B981").build();

        when(portfolioTagPort.findAllByUserIdAndIdIn("user-a", List.of(keepId, foreignId)))
                .thenReturn(List.of(keepTag));

        // Fails before any mutation if a foreign/unknown tagId is included
        assertThatThrownBy(() -> setAssetTagsUseCase.execute("user-a", "WEGE3", List.of(keepId, foreignId)))
                .isInstanceOf(TagNotFoundException.class);
        verify(assetTagPort, never()).deleteByUserIdAndTickerAndTagIdIn(any(), any(), anyCollection());
        verify(assetTagPort, never()).saveAll(anyList());

        when(portfolioTagPort.findAllByUserIdAndIdIn("user-a", List.of(keepId, addId)))
                .thenReturn(List.of(keepTag, addTag));
        when(assetTagPort.findAllByUserIdAndTicker("user-a", "WEGE3")).thenReturn(List.of(
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(keepId).ticker("WEGE3").build(),
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(removeId).ticker("WEGE3").build()
        ));
        when(assetTagPort.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // Duplicate tagId in request is deduplicated cleanly
        AssetTagsAssignmentDto result = setAssetTagsUseCase.execute("user-a", "wege3", List.of(keepId, addId, keepId));

        assertThat(result.ticker()).isEqualTo("WEGE3");
        assertThat(result.tags()).extracting(TagSummaryDto::id).containsExactly(keepId, addId);

        verify(assetTagPort).deleteByUserIdAndTickerAndTagIdIn("user-a", "WEGE3", List.of(removeId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssetTag>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(assetTagPort).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(1);
        assertThat(savedCaptor.getValue().get(0).getTagId()).isEqualTo(addId);
        assertThat(savedCaptor.getValue().get(0).getTicker()).isEqualTo("WEGE3");
    }

    @Test
    void shouldClearAllAssetTagsWhenSetWithEmptyList() {
        UUID existingTagId = UUID.randomUUID();
        when(assetTagPort.findAllByUserIdAndTicker("user-a", "PETR4")).thenReturn(List.of(
                AssetTag.builder().id(UUID.randomUUID()).userId("user-a").tagId(existingTagId).ticker("PETR4").build()
        ));

        AssetTagsAssignmentDto result = setAssetTagsUseCase.execute("user-a", "petr4", List.of());

        assertThat(result.ticker()).isEqualTo("PETR4");
        assertThat(result.tags()).isEmpty();
        verify(portfolioTagPort, never()).findAllByUserIdAndIdIn(any(), anyCollection());
        verify(assetTagPort).deleteByUserIdAndTickerAndTagIdIn("user-a", "PETR4", List.of(existingTagId));
        verify(assetTagPort, never()).saveAll(anyList());
    }

    @Test
    void shouldRejectNullTagIdsListOrNullElementsOrExceedingLimitInSetAssetTags() {
        assertThatThrownBy(() -> setAssetTagsUseCase.execute("user-a", "PETR4", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> setAssetTagsUseCase.execute("user-a", "PETR4", Arrays.asList(UUID.randomUUID(), null)))
                .isInstanceOf(IllegalArgumentException.class);

        List<UUID> overLimit = IntStream.range(0, TagInputValidator.MAX_TAGS_PER_ASSET + 1)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        assertThatThrownBy(() -> setAssetTagsUseCase.execute("user-a", "PETR4", overLimit))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAssignAndUnassignAssetToTagIdempotently() {
        UUID tagId = UUID.randomUUID();
        PortfolioTag tag = PortfolioTag.builder().id(tagId).userId("user-a").name("Dividendos").build();
        when(portfolioTagPort.findByIdAndUserId(tagId, "user-a")).thenReturn(Optional.of(tag));

        // First assign: creates association
        when(assetTagPort.findByUserIdAndTagIdAndTicker("user-a", tagId, "LCI-2026"))
                .thenReturn(Optional.empty());
        when(assetTagPort.findAllByUserIdAndTicker("user-a", "LCI-2026"))
                .thenReturn(List.of());
        assignAssetToTagUseCase.execute("user-a", tagId, "lci-2026");
        verify(assetTagPort).saveIfAbsent(any(AssetTag.class));

        // Second assign: idempotent no-op
        clearInvocations(assetTagPort);
        when(assetTagPort.findByUserIdAndTagIdAndTicker("user-a", tagId, "LCI-2026"))
                .thenReturn(Optional.of(AssetTag.builder().id(UUID.randomUUID()).build()));
        assignAssetToTagUseCase.execute("user-a", tagId, "LCI-2026");
        verify(assetTagPort, never()).saveIfAbsent(any(AssetTag.class));

        // Unassign
        unassignAssetFromTagUseCase.execute("user-a", tagId, "LCI-2026");
        verify(assetTagPort).deleteByUserIdAndTagIdAndTicker("user-a", tagId, "LCI-2026");
    }

    @Test
    void shouldRejectAssignAndUnassignWhenTagMissingOrNullOrLimitReached() {
        UUID tagId = UUID.randomUUID();

        assertThatThrownBy(() -> assignAssetToTagUseCase.execute("user-a", null, "PETR4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> unassignAssetFromTagUseCase.execute("user-a", null, "PETR4"))
                .isInstanceOf(IllegalArgumentException.class);

        when(portfolioTagPort.findByIdAndUserId(tagId, "user-a")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> assignAssetToTagUseCase.execute("user-a", tagId, "PETR4"))
                .isInstanceOf(TagNotFoundException.class);
        assertThatThrownBy(() -> unassignAssetFromTagUseCase.execute("user-a", tagId, "PETR4"))
                .isInstanceOf(TagNotFoundException.class);

        PortfolioTag tag = PortfolioTag.builder().id(tagId).userId("user-a").name("Dividendos").build();
        when(portfolioTagPort.findByIdAndUserId(tagId, "user-a")).thenReturn(Optional.of(tag));
        when(assetTagPort.findByUserIdAndTagIdAndTicker("user-a", tagId, "PETR4"))
                .thenReturn(Optional.empty());
        List<AssetTag> maxedTags = IntStream.range(0, TagInputValidator.MAX_TAGS_PER_ASSET)
                .mapToObj(i -> AssetTag.builder().id(UUID.randomUUID()).build())
                .toList();
        when(assetTagPort.findAllByUserIdAndTicker("user-a", "PETR4")).thenReturn(maxedTags);

        assertThatThrownBy(() -> assignAssetToTagUseCase.execute("user-a", tagId, "PETR4"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(assetTagPort, never()).saveIfAbsent(any());
    }
}
