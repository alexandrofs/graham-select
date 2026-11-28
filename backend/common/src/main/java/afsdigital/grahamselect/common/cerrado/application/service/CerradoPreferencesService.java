package afsdigital.grahamselect.common.cerrado.application.service;

import afsdigital.grahamselect.common.ai.application.service.AiDiagnosticCacheService;
import afsdigital.grahamselect.common.cerrado.application.repository.CerradoPreferencesPort;
import afsdigital.grahamselect.common.cerrado.application.service.exceptions.CerradoPreferencesNotFoundException;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoPreferences;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoTemplate;
import afsdigital.grahamselect.common.cerrado.domain.service.CerradoAllocationEngine;
import afsdigital.grahamselect.common.cerrado.domain.service.CerradoTemplateCatalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servico de aplicacao para consulta de templates de onboarding e gestao das
 * preferencias estrategicas do Diagrama do Cerrado por usuario.
 */
public class CerradoPreferencesService {

    private static final int OUTPUT_SCALE = 2;

    private final CerradoPreferencesPort preferencesPort;
    private final CerradoAllocationEngine allocationEngine;
    private final AiDiagnosticCacheService aiDiagnosticCacheService;
    private final CerradoTemplateCatalog templateCatalog;

    public CerradoPreferencesService(
            CerradoPreferencesPort preferencesPort,
            CerradoAllocationEngine allocationEngine,
            AiDiagnosticCacheService aiDiagnosticCacheService,
            CerradoTemplateCatalog templateCatalog
    ) {
        this.preferencesPort = Objects.requireNonNull(preferencesPort, "preferencesPort must not be null");
        this.allocationEngine = Objects.requireNonNull(allocationEngine, "allocationEngine must not be null");
        this.aiDiagnosticCacheService = Objects.requireNonNull(
                aiDiagnosticCacheService,
                "aiDiagnosticCacheService must not be null"
        );
        this.templateCatalog = Objects.requireNonNull(templateCatalog, "templateCatalog must not be null");
    }

    public TemplatesCatalogResult getTemplates() {
        return new TemplatesCatalogResult(
                templateCatalog.getDefaultNeutralGrade(),
                templateCatalog.getCvmDisclaimer(),
                templateCatalog.getTemplates()
        );
    }

    public CerradoPreferences getPreferences(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return preferencesPort.findByUserId(userId)
                .orElseThrow(() -> new CerradoPreferencesNotFoundException(
                        "Preferencias do Diagrama do Cerrado nao encontradas para o usuario."
                ));
    }

    public CerradoPreferences savePreferences(
            Long userId,
            BigDecimal targetAcoesBr,
            BigDecimal targetFiis,
            BigDecimal targetRendaFixa,
            BigDecimal targetInternacional,
            BigDecimal targetCripto,
            BigDecimal grahamMultiplier,
            Boolean grahamFilterEnabled
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        Optional<CerradoPreferences> existingOpt = preferencesPort.findByUserId(userId);

        BigDecimal effectiveGrahamMultiplier = grahamMultiplier != null
                ? grahamMultiplier
                : existingOpt.map(CerradoPreferences::getGrahamMultiplier)
                        .orElse(CerradoPreferences.DEFAULT_GRAHAM_MULTIPLIER);

        boolean effectiveGrahamFilterEnabled = grahamFilterEnabled != null
                ? grahamFilterEnabled
                : existingOpt.map(CerradoPreferences::isGrahamFilterEnabled)
                        .orElse(true);

        allocationEngine.validateTargetValues(
                targetAcoesBr,
                targetFiis,
                targetRendaFixa,
                targetInternacional,
                targetCripto,
                effectiveGrahamMultiplier
        );

        CerradoPreferences toSave = CerradoPreferences.builder()
                .id(existingOpt.map(CerradoPreferences::getId).orElse(null))
                .userId(userId)
                .targetAcoesBr(targetAcoesBr.setScale(OUTPUT_SCALE, RoundingMode.HALF_EVEN))
                .targetFiis(targetFiis.setScale(OUTPUT_SCALE, RoundingMode.HALF_EVEN))
                .targetRendaFixa(targetRendaFixa.setScale(OUTPUT_SCALE, RoundingMode.HALF_EVEN))
                .targetInternacional(targetInternacional.setScale(OUTPUT_SCALE, RoundingMode.HALF_EVEN))
                .targetCripto(targetCripto.setScale(OUTPUT_SCALE, RoundingMode.HALF_EVEN))
                .grahamMultiplier(effectiveGrahamMultiplier.setScale(OUTPUT_SCALE, RoundingMode.HALF_EVEN))
                .grahamFilterEnabled(effectiveGrahamFilterEnabled)
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        CerradoPreferences saved = preferencesPort.save(toSave);
        aiDiagnosticCacheService.invalidateByUserId(userId);
        return saved;
    }

    public record TemplatesCatalogResult(
            int defaultNeutralGrade,
            String cvmDisclaimer,
            List<CerradoTemplate> templates
    ) {
    }
}
