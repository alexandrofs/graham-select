package afsdigital.grahamselect.common.ai.application.service;

import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticBlocks;
import afsdigital.grahamselect.common.ai.domain.model.MacroClassAllocationSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedAssetPosition;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostGenerationValidatorTest {

    private final PostGenerationValidator validator = new PostGenerationValidator();

    private SanitizedPortfolioSnapshot sampleSnapshot() {
        return new SanitizedPortfolioSnapshot(
                List.of(
                        new SanitizedAssetPosition(
                                "PETR4",
                                MacroClass.ACOES_BR,
                                CerradoSubClass.EQUITY_DEFENSIVE,
                                new BigDecimal("32.10"),
                                8,
                                false,
                                new BigDecimal("4.50"),
                                new BigDecimal("1.10"),
                                new BigDecimal("12.40"),
                                new BigDecimal("35.00")
                        ),
                        new SanitizedAssetPosition(
                                "RENDA_FIXA_MANUAL_1",
                                MacroClass.RENDA_FIXA,
                                CerradoSubClass.TREASURY_POST_FIXED,
                                new BigDecimal("67.90"),
                                null,
                                false,
                                null,
                                null,
                                null,
                                null
                        )
                ),
                List.of(
                        new MacroClassAllocationSnapshot(
                                MacroClass.ACOES_BR,
                                new BigDecimal("32.10"),
                                new BigDecimal("40.00"),
                                new BigDecimal("7.90")
                        ),
                        new MacroClassAllocationSnapshot(
                                MacroClass.RENDA_FIXA,
                                new BigDecimal("67.90"),
                                new BigDecimal("60.00"),
                                new BigDecimal("-7.90")
                        )
                ),
                new BigDecimal("0.2500")
        );
    }

    @Test
    void shouldAcceptValidBlocksWithNormalizedCommasMarketAcronymsAndGradeScale() {
        AiDiagnosticBlocks blocks = new AiDiagnosticBlocks(
                "Na B3, a carteira possui PETR4 com 32,1% e RENDA_FIXA_MANUAL_1 com 67,9%, além de HHI de 0,2500 sob contrato ZDR e LGPD.",
                "PETR4 apresenta nota 8/10, P/L de 4,5, P/VP de 1,1 e DY de 12,4%, superando referências como CDI, IPCA e SELIC.",
                "A classe RENDA_FIXA (FII, ETF, BDR ausentes) está em 67,9% frente ao alvo de 60,0%, com desvio de -7,9% conforme análise educativa CVM.",
                "A macroclasse ACOES_BR está em 32,1% ante meta de 40,0%, com desvio de 7,9% para equilibrar o portfólio."
        );

        PostGenerationValidator.ValidationResult result = validator.validate(blocks, sampleSnapshot());

        assertThat(result.valid()).isTrue();
        assertThat(result.reasonCode()).isNull();
    }

    @Test
    void shouldRejectBlankOrIncompleteBlocks() {
        AiDiagnosticBlocks blocks = new AiDiagnosticBlocks(
                "Resumo válido com PETR4 em 32,1%.",
                "   ",
                "Alerta válido.",
                "Racional válido."
        );

        PostGenerationValidator.ValidationResult result = validator.validate(blocks, sampleSnapshot());

        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("INCOMPLETE_BLOCKS");
    }

    @Test
    void shouldRejectInventedTickerNotPresentInSnapshot() {
        AiDiagnosticBlocks blocks = new AiDiagnosticBlocks(
                "Carteira com PETR4 em 32,1% e exposição adicional a MGLU3.",
                "PETR4 mantém nota 8/10.",
                "Concentração HHI de 0,25.",
                "Desvio de 7,9% em ACOES_BR."
        );

        PostGenerationValidator.ValidationResult result = validator.validate(blocks, sampleSnapshot());

        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("HALLUCINATED_TICKER");
    }

    @Test
    void shouldRejectUnanchoredPercentageOutsideTolerance() {
        AiDiagnosticBlocks blocks = new AiDiagnosticBlocks(
                "PETR4 representa 45,0% da carteira.",
                "Nota 8/10 para PETR4.",
                "HHI em 0,25.",
                "Desvio de 7,9%."
        );

        PostGenerationValidator.ValidationResult result = validator.validate(blocks, sampleSnapshot());

        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("HALLUCINATED_NUMBER");
    }

    @Test
    void shouldRejectHhiOutsideTightTolerance() {
        AiDiagnosticBlocks blocks = new AiDiagnosticBlocks(
                "PETR4 representa 32,1% da carteira.",
                "Nota 8/10 para PETR4.",
                "HHI em 0,30.",
                "Desvio de 7,9%."
        );

        PostGenerationValidator.ValidationResult result = validator.validate(blocks, sampleSnapshot());

        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("HALLUCINATED_NUMBER");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Recomendo comprar PETR4 agora.",
            "Compre PETR4 com 32,1%.",
            "Adquira PETR4 para sua carteira.",
            "Recomendamos manter ou ajustar PETR4.",
            "Recomendo a venda de RENDA_FIXA_MANUAL_1.",
            "Vender PETR4 imediatamente.",
            "Este ativo oferece lucro garantido.",
            "Este ativo oferece ganho garantido.",
            "Retorno garantido de 32,1%.",
            "Rentabilidade garantida no longo prazo."
    })
    void shouldRejectCvmImperativeOrGuaranteedReturnTerms(String offensiveBlock) {
        AiDiagnosticBlocks blocks = new AiDiagnosticBlocks(
                offensiveBlock,
                "PETR4 tem nota 8/10.",
                "HHI em 0,25.",
                "Desvio de 7,9%."
        );

        PostGenerationValidator.ValidationResult result = validator.validate(blocks, sampleSnapshot());

        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("CVM_REGULATORY_VIOLATION");
    }

    @Test
    void shouldRejectLowercaseInventedTicker() {
        AiDiagnosticBlocks blocks = new AiDiagnosticBlocks(
                "Carteira com PETR4 em 32,1% e menção a mglu3.",
                "PETR4 mantém nota 8/10.",
                "Concentração HHI de 0,25.",
                "Desvio de 7,9% em ACOES_BR."
        );

        PostGenerationValidator.ValidationResult result = validator.validate(blocks, sampleSnapshot());

        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("HALLUCINATED_TICKER");
    }

    @Test
    void shouldAcceptRangeNumbersAndAdditionalFinancialAcronyms() {
        AiDiagnosticBlocks blocks = new AiDiagnosticBlocks(
                "No KERNO, a carteira apresenta faixa de 32,1-67,9% com indicadores EV, EBIT, CAGR, WACC e ESG.",
                "PETR4 tem nota 8/10 segundo critérios II e III.",
                "Concentração HHI em 0,25.",
                "Desvio de 7,9% em ACOES_BR."
        );

        PostGenerationValidator.ValidationResult result = validator.validate(blocks, sampleSnapshot());

        assertThat(result.valid()).isTrue();
    }
}
