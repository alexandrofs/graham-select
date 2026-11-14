package afsdigital.grahamselect.api.family.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.RequirePremium;
import afsdigital.grahamselect.common.family.application.usecase.LinkFamilyProfileUseCase;
import afsdigital.grahamselect.common.family.application.usecase.ListFamilyProfilesUseCase;
import afsdigital.grahamselect.common.family.application.usecase.UnlinkFamilyProfileUseCase;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/family-profiles")
@RequirePremium
@RequiredArgsConstructor
public class FamilyProfileController {

    private final LinkFamilyProfileUseCase linkFamilyProfileUseCase;
    private final ListFamilyProfilesUseCase listFamilyProfilesUseCase;
    private final UnlinkFamilyProfileUseCase unlinkFamilyProfileUseCase;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listProfiles(@AuthenticationPrincipal Jwt jwt) {
        String masterUserId = jwt.getSubject();
        log.info("Listing family profiles for masterUserId={}", masterUserId);
        try {
            List<FamilyProfileResponse> profiles = listFamilyProfilesUseCase.execute(masterUserId).stream()
                    .map(FamilyProfileResponse::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "data", profiles,
                    "meta", Map.of("total", profiles.size())
            ));
        } catch (Exception e) {
            log.error("Error listing family profiles for masterUserId={}: {}", masterUserId, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> linkProfile(
            @Valid @RequestBody CreateFamilyProfileRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String masterUserId = jwt.getSubject();
        log.info("Linking new family profile for masterUserId={}, relationship={}", masterUserId, request.relationship());
        try {
            FamilyProfile created = linkFamilyProfileUseCase.execute(
                    masterUserId,
                    request.cpf(),
                    request.name(),
                    request.relationship()
            );
            FamilyProfileResponse response = FamilyProfileResponse.from(created);
            log.info("Family profile linked successfully: masterUserId={}, profileId={}", masterUserId, response.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", response));
        } catch (Exception e) {
            log.error("Error linking family profile for masterUserId={}: {}", masterUserId, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> unlinkProfile(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String masterUserId = jwt.getSubject();
        log.info("Unlinking family profile: masterUserId={}, profileId={}", masterUserId, profileId);
        try {
            unlinkFamilyProfileUseCase.execute(profileId, masterUserId);
            log.info("Family profile unlinked successfully: masterUserId={}, profileId={}", masterUserId, profileId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error unlinking family profile: masterUserId={}, profileId={}, error={}", masterUserId, profileId, e.getMessage(), e);
            throw e;
        }
    }
}

