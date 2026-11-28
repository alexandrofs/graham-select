package afsdigital.grahamselect.api.cerrado.web;

import afsdigital.grahamselect.common.cerrado.application.service.CerradoPreferencesService;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoPreferences;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/cerrado")
@RequiredArgsConstructor
public class CerradoController {

    private final CerradoPreferencesService cerradoPreferencesService;

    @GetMapping("/templates")
    public ResponseEntity<CerradoTemplatesResponse> getTemplates(Authentication authentication) {
        Optional<Long> userIdOpt = extractAuthenticatedUserId(authentication);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = userIdOpt.get();
        log.info("Received GET /api/v1/cerrado/templates userId={}", userId);
        CerradoPreferencesService.TemplatesCatalogResult catalog = cerradoPreferencesService.getTemplates();
        List<CerradoTemplateItemResponse> templates = catalog.templates().stream()
                .map(CerradoTemplateItemResponse::from)
                .toList();
        return ResponseEntity.ok(new CerradoTemplatesResponse(
                catalog.defaultNeutralGrade(),
                catalog.cvmDisclaimer(),
                templates
        ));
    }

    @GetMapping("/preferences")
    @Transactional(readOnly = true)
    public ResponseEntity<CerradoPreferencesResponse> getPreferences(Authentication authentication) {
        Optional<Long> userIdOpt = extractAuthenticatedUserId(authentication);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = userIdOpt.get();
        log.info("Received GET /api/v1/cerrado/preferences userId={}", userId);
        CerradoPreferences preferences = cerradoPreferencesService.getPreferences(userId);
        return ResponseEntity.ok(CerradoPreferencesResponse.from(preferences));
    }

    @PutMapping("/preferences")
    @Transactional
    public ResponseEntity<CerradoPreferencesResponse> savePreferences(
            Authentication authentication,
            @RequestBody UpdateCerradoPreferencesRequest request
    ) {
        Optional<Long> userIdOpt = extractAuthenticatedUserId(authentication);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = userIdOpt.get();
        log.info(
                "Received PUT /api/v1/cerrado/preferences userId={}, acoesBr={}, fiis={}, rendaFixa={}, internacional={}, cripto={}, grahamMultiplier={}, grahamFilterEnabled={}",
                userId,
                request.targetAcoesBr(),
                request.targetFiis(),
                request.targetRendaFixa(),
                request.targetInternacional(),
                request.targetCripto(),
                request.grahamMultiplier(),
                request.grahamFilterEnabled()
        );
        CerradoPreferences saved = cerradoPreferencesService.savePreferences(
                userId,
                request.targetAcoesBr(),
                request.targetFiis(),
                request.targetRendaFixa(),
                request.targetInternacional(),
                request.targetCripto(),
                request.grahamMultiplier(),
                request.grahamFilterEnabled()
        );
        log.info(
                "Saved Cerrado preferences userId={}, preferencesId={}, updatedAt={}",
                userId,
                saved.getId(),
                saved.getUpdatedAt()
        );
        return ResponseEntity.ok(CerradoPreferencesResponse.from(saved));
    }

    private Optional<Long> extractAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(authentication.getName()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public record UpdateCerradoPreferencesRequest(
            BigDecimal targetAcoesBr,
            BigDecimal targetFiis,
            BigDecimal targetRendaFixa,
            BigDecimal targetInternacional,
            BigDecimal targetCripto,
            BigDecimal grahamMultiplier,
            Boolean grahamFilterEnabled
    ) {
    }

    public record CerradoTemplatesResponse(
            int defaultNeutralGrade,
            String cvmDisclaimer,
            List<CerradoTemplateItemResponse> templates
    ) {
    }

    public record CerradoTemplateItemResponse(
            String code,
            String name,
            String description,
            BigDecimal targetAcoesBr,
            BigDecimal targetFiis,
            BigDecimal targetRendaFixa,
            BigDecimal targetInternacional,
            BigDecimal targetCripto
    ) {
        public static CerradoTemplateItemResponse from(CerradoTemplate template) {
            return new CerradoTemplateItemResponse(
                    template.code(),
                    template.name(),
                    template.description(),
                    template.targetAcoesBr(),
                    template.targetFiis(),
                    template.targetRendaFixa(),
                    template.targetInternacional(),
                    template.targetCripto()
            );
        }
    }

    public record CerradoPreferencesResponse(
            Long id,
            Long userId,
            BigDecimal targetAcoesBr,
            BigDecimal targetFiis,
            BigDecimal targetRendaFixa,
            BigDecimal targetInternacional,
            BigDecimal targetCripto,
            BigDecimal grahamMultiplier,
            boolean grahamFilterEnabled,
            OffsetDateTime updatedAt
    ) {
        public static CerradoPreferencesResponse from(CerradoPreferences preferences) {
            OffsetDateTime updatedAtUtc = preferences.getUpdatedAt() != null
                    ? preferences.getUpdatedAt().atOffset(ZoneOffset.UTC)
                    : null;
            return new CerradoPreferencesResponse(
                    preferences.getId(),
                    preferences.getUserId(),
                    preferences.getTargetAcoesBr(),
                    preferences.getTargetFiis(),
                    preferences.getTargetRendaFixa(),
                    preferences.getTargetInternacional(),
                    preferences.getTargetCripto(),
                    preferences.getGrahamMultiplier(),
                    preferences.isGrahamFilterEnabled(),
                    updatedAtUtc
            );
        }
    }
}
