package afsdigital.grahamselect.api.tags.web;

import afsdigital.grahamselect.common.tags.application.dto.AssetTagsAssignmentDto;
import afsdigital.grahamselect.common.tags.application.dto.TagDetailsDto;
import afsdigital.grahamselect.common.tags.application.usecase.AssignAssetToTagUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.CreateTagUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.DeleteTagUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.GetAssetTagsUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.ListTagsUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.RenameTagUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.SetAssetTagsUseCase;
import afsdigital.grahamselect.common.tags.application.usecase.UnassignAssetFromTagUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final CreateTagUseCase createTagUseCase;
    private final ListTagsUseCase listTagsUseCase;
    private final GetAssetTagsUseCase getAssetTagsUseCase;
    private final RenameTagUseCase renameTagUseCase;
    private final DeleteTagUseCase deleteTagUseCase;
    private final SetAssetTagsUseCase setAssetTagsUseCase;
    private final AssignAssetToTagUseCase assignAssetToTagUseCase;
    private final UnassignAssetFromTagUseCase unassignAssetFromTagUseCase;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> listTags(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);
        log.info("Listing tags for userId={}", userId);
        try {
            List<TagResponse> tags = listTagsUseCase.execute(userId).stream()
                    .map(TagResponse::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "data", tags,
                    "meta", Map.of("total", tags.size())
            ));
        } catch (Exception ex) {
            log.error("Error listing tags for userId={}: {}", userId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createTag(
            @Valid @RequestBody CreateOrUpdateTagRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = extractUserId(jwt);
        log.info("Creating tag for userId={}", userId);
        try {
            TagDetailsDto created = createTagUseCase.execute(userId, request.name(), request.color());
            TagResponse response = TagResponse.from(created);
            log.info("Tag created successfully: userId={}, tagId={}", userId, response.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", response));
        } catch (Exception ex) {
            log.error("Error creating tag for userId={}: {}", userId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @PutMapping("/{tagId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> renameTag(
            @PathVariable UUID tagId,
            @Valid @RequestBody CreateOrUpdateTagRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = extractUserId(jwt);
        log.info("Updating tag for userId={}, tagId={}", userId, tagId);
        try {
            TagDetailsDto updated = renameTagUseCase.execute(userId, tagId, request.name(), request.color());
            TagResponse response = TagResponse.from(updated);
            log.info("Tag updated successfully: userId={}, tagId={}", userId, tagId);
            return ResponseEntity.ok(Map.of("data", response));
        } catch (Exception ex) {
            log.error("Error updating tag userId={}, tagId={}: {}", userId, tagId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @DeleteMapping("/{tagId}")
    @Transactional
    public ResponseEntity<Void> deleteTag(
            @PathVariable UUID tagId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = extractUserId(jwt);
        log.info("Deleting tag for userId={}, tagId={}", userId, tagId);
        try {
            deleteTagUseCase.execute(userId, tagId);
            log.info("Tag deleted successfully: userId={}, tagId={}", userId, tagId);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error deleting tag userId={}, tagId={}: {}", userId, tagId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @GetMapping("/assets/{ticker}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAssetTags(
            @PathVariable String ticker,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = extractUserId(jwt);
        String safeTicker = sanitizeForLog(ticker);
        log.info("Fetching tags for asset userId={}, ticker={}", userId, safeTicker);
        try {
            AssetTagsAssignmentDto dto = getAssetTagsUseCase.execute(userId, ticker);
            return ResponseEntity.ok(Map.of("data", AssetTagsResponse.from(dto)));
        } catch (Exception ex) {
            log.error("Error fetching tags for asset userId={}, ticker={}: {}", userId, safeTicker, ex.getMessage(), ex);
            throw ex;
        }
    }

    @PutMapping("/assets/{ticker}")
    @Transactional
    public ResponseEntity<Map<String, Object>> setAssetTags(
            @PathVariable String ticker,
            @Valid @RequestBody SetAssetTagsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = extractUserId(jwt);
        String safeTicker = sanitizeForLog(ticker);
        log.info("Setting tags for asset userId={}, ticker={}", userId, safeTicker);
        try {
            AssetTagsAssignmentDto dto = setAssetTagsUseCase.execute(userId, ticker, request.tagIds());
            log.info("Asset tags updated successfully: userId={}, ticker={}, tagCount={}",
                    userId, dto.ticker(), dto.tags().size());
            return ResponseEntity.ok(Map.of("data", AssetTagsResponse.from(dto)));
        } catch (Exception ex) {
            log.error("Error setting tags for asset userId={}, ticker={}: {}", userId, safeTicker, ex.getMessage(), ex);
            throw ex;
        }
    }

    @PostMapping("/{tagId}/assets/{ticker}")
    public ResponseEntity<Map<String, Object>> assignAssetToTag(
            @PathVariable UUID tagId,
            @PathVariable String ticker,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = extractUserId(jwt);
        String safeTicker = sanitizeForLog(ticker);
        log.info("Assigning asset to tag userId={}, tagId={}, ticker={}", userId, tagId, safeTicker);
        try {
            String normalizedTicker = assignAssetToTagUseCase.execute(userId, tagId, ticker);
            log.info("Asset assigned to tag: userId={}, tagId={}, ticker={}", userId, tagId, normalizedTicker);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "data", Map.of(
                            "tagId", tagId,
                            "ticker", normalizedTicker
                    )
            ));
        } catch (Exception ex) {
            log.error("Error assigning asset to tag userId={}, tagId={}, ticker={}: {}",
                    userId, tagId, safeTicker, ex.getMessage(), ex);
            throw ex;
        }
    }

    @DeleteMapping("/{tagId}/assets/{ticker}")
    @Transactional
    public ResponseEntity<Void> unassignAssetFromTag(
            @PathVariable UUID tagId,
            @PathVariable String ticker,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = extractUserId(jwt);
        String safeTicker = sanitizeForLog(ticker);
        log.info("Unassigning asset from tag userId={}, tagId={}, ticker={}", userId, tagId, safeTicker);
        try {
            unassignAssetFromTagUseCase.execute(userId, tagId, ticker);
            log.info("Asset unassigned from tag: userId={}, tagId={}, ticker={}", userId, tagId, safeTicker);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("Error unassigning asset from tag userId={}, tagId={}, ticker={}: {}",
                    userId, tagId, safeTicker, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            IllegalArgumentException ex = new IllegalArgumentException("Usuário autenticado não identificado.");
            log.error("Missing authenticated JWT subject: {}", ex.getMessage(), ex);
            throw ex;
        }
        return jwt.getSubject();
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return "<null>";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            sb.append(Character.isISOControl(ch) ? '_' : ch);
        }
        return sb.toString();
    }
}
