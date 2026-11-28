package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticResult;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticSource;
import afsdigital.grahamselect.common.ai.domain.model.MacroClassAllocationSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedAssetPosition;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicPortfolioFallbackGeneratorTest {

    private final DeterministicPortfolioFallbackGenerator generator = new DeterministicPortfolioFallbackGenerator();

    @Test
    void shouldGenerateFourValidBlocksForEmptySnapshot() {
        SanitizedPortfolioSnapshot empty = new SanitizedPortfolioSnapshot(List.of(), List.of(), BigDecimal.ZERO);

        AiDiagnosticResult result = generator.generate(empty, "EMPTY_SNAPSHOT");

        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("EMPTY_SNAPSHOT");
        assertThat(result.blocks().hasAllNonBlankBlocks()).isTrue();
    }

    @Test
    void shouldHandleNullOptionalFieldsQuarantineAndNegativeDeviationsWithoutCurrencyOrPii() {
        SanitizedPortfolioSnapshot snapshot = new SanitizedPortfolioSnapshot(
                List.of(
                        new SanitizedAssetPosition(
                                "PETR4",
                                MacroClass.ACOES_BR,
                                null,
                                new BigDecimal("65.00"),
                                null,
                                true,
                                null,
                                null,
                                null,
                                null
                        ),
                        new SanitizedAssetPosition(
                                "RENDA_FIXA_MANUAL_1",
                                MacroClass.RENDA_FIXA,
                                null,
                                new BigDecimal("35.00"),
                                9,
                                false,
                                null,
                                null,
                                null,
                                new BigDecimal("15.00")
                        )
                ),
                List.of(
                        new MacroClassAllocationSnapshot(
                                MacroClass.ACOES_BR,
                                new BigDecimal("65.00"),
                                new BigDecimal("40.00"),
                                new BigDecimal("25.00")
                        ),
                        new MacroClassAllocationSnapshot(
                                MacroClass.RENDA_FIXA,
                                new BigDecimal("35.00"),
                                new BigDecimal("60.00"),
                                new BigDecimal("-25.00")
                        )
                ),
                new BigDecimal("0.5450")
        );

        AiDiagnosticResult result = generator.generate(snapshot, "VERTEX_API_ERROR");

        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.blocks().hasAllNonBlankBlocks()).isTrue();
        assertThat(result.blocks().alertasDeRiscoEConcentracao()).contains("PETR4", "quarentena", "ACOES_BR");
        assertThat(result.blocks().racionalDoPlanoDeAporte()).contains("RENDA_FIXA");

        String allText = result.blocks().resumoExecutivo()
                + result.blocks().pontosFortes()
                + result.blocks().alertasDeRiscoEConcentracao()
                + result.blocks().racionalDoPlanoDeAporte();
        assertThat(allText).doesNotContain("R$");
    }
}
