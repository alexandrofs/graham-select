package afsdigital.grahamselect.common.ai.domain.model;

/**
 * Closed 4-block structure for Kerno AI portfolio diagnostics.
 */
public record AiDiagnosticBlocks(
        String resumoExecutivo,
        String pontosFortes,
        String alertasDeRiscoEConcentracao,
        String racionalDoPlanoDeAporte
) {
    public boolean hasAllNonBlankBlocks() {
        return isNotBlank(resumoExecutivo)
                && isNotBlank(pontosFortes)
                && isNotBlank(alertasDeRiscoEConcentracao)
                && isNotBlank(racionalDoPlanoDeAporte);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
