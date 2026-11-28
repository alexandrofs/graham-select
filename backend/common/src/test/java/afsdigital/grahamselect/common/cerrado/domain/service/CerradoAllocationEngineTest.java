package afsdigital.grahamselect.common.cerrado.domain.service;

import afsdigital.grahamselect.common.cerrado.domain.exceptions.CerradoValidationException;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAllocationResult;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAssetAllocation;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAssetGrade;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAssetInput;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoMacroClassSummary;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoPreferences;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CerradoAllocationEngineTest {

    private final CerradoAllocationEngine engine = new CerradoAllocationEngine();

    private static CerradoPreferences.CerradoPreferencesBuilder basePreferences() {
        return CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(new BigDecimal("25.00"))
                .targetFiis(new BigDecimal("20.00"))
                .targetRendaFixa(new BigDecimal("30.00"))
                .targetInternacional(new BigDecimal("20.00"))
                .targetCripto(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("CerradoSubClass belongsTo should enforce Level 1 -> Level 2 hierarchy and allow OUTROS_MANUAL anywhere")
    void shouldValidateSubClassHierarchy() {
        assertThat(CerradoSubClass.EQUITY_DEFENSIVE.belongsTo(MacroClass.ACOES_BR)).isTrue();
        assertThat(CerradoSubClass.EQUITY_GROWTH.belongsTo(MacroClass.ACOES_BR)).isTrue();
        assertThat(CerradoSubClass.EQUITY_DEFENSIVE.belongsTo(MacroClass.FIIS)).isFalse();

        assertThat(CerradoSubClass.FII_BRICK.belongsTo(MacroClass.FIIS)).isTrue();
        assertThat(CerradoSubClass.FII_PAPER.belongsTo(MacroClass.FIIS)).isTrue();
        assertThat(CerradoSubClass.FII_FOF.belongsTo(MacroClass.FIIS)).isTrue();
        assertThat(CerradoSubClass.FII_BRICK.belongsTo(MacroClass.ACOES_BR)).isFalse();

        assertThat(CerradoSubClass.TREASURY_POST_FIXED.belongsTo(MacroClass.RENDA_FIXA)).isTrue();
        assertThat(CerradoSubClass.TREASURY_IPCA.belongsTo(MacroClass.RENDA_FIXA)).isTrue();
        assertThat(CerradoSubClass.TREASURY_PREFIXED.belongsTo(MacroClass.RENDA_FIXA)).isTrue();
        assertThat(CerradoSubClass.FIXED_INCOME_PRIVATE.belongsTo(MacroClass.RENDA_FIXA)).isTrue();
        assertThat(CerradoSubClass.TREASURY_IPCA.belongsTo(MacroClass.INTERNACIONAL)).isFalse();

        assertThat(CerradoSubClass.INTERNATIONAL_EQUITY.belongsTo(MacroClass.INTERNACIONAL)).isTrue();
        assertThat(CerradoSubClass.INTERNATIONAL_ETF.belongsTo(MacroClass.INTERNACIONAL)).isTrue();
        assertThat(CerradoSubClass.INTERNATIONAL_ETF.belongsTo(MacroClass.CRIPTO)).isFalse();

        assertThat(CerradoSubClass.CRYPTO_MAJOR.belongsTo(MacroClass.CRIPTO)).isTrue();
        assertThat(CerradoSubClass.CRYPTO_MAJOR.belongsTo(MacroClass.ACOES_BR)).isFalse();

        for (MacroClass mc : MacroClass.values()) {
            assertThat(CerradoSubClass.OUTROS_MANUAL.belongsTo(mc)).isTrue();
        }
        assertThat(CerradoSubClass.OUTROS_MANUAL.belongsTo(null)).isFalse();
    }

    @Test
    @DisplayName("Should reject preferences when sum != 100%, negative, scale > 2, or grahamMultiplier <= 0")
    void shouldRejectInvalidPreferences() {
        assertThatThrownBy(() -> engine.calculate(null, List.of()))
                .isInstanceOf(CerradoValidationException.class);

        CerradoPreferences under100 = basePreferences().targetCripto(new BigDecimal("0.00")).build();
        assertThatThrownBy(() -> engine.calculate(under100, List.of()))
                .isInstanceOf(CerradoValidationException.class);

        CerradoPreferences over100 = basePreferences().targetCripto(new BigDecimal("10.00")).build();
        assertThatThrownBy(() -> engine.calculate(over100, List.of()))
                .isInstanceOf(CerradoValidationException.class);

        CerradoPreferences negative = basePreferences()
                .targetCripto(new BigDecimal("-5.00"))
                .targetAcoesBr(new BigDecimal("35.00"))
                .build();
        assertThatThrownBy(() -> engine.calculate(negative, List.of()))
                .isInstanceOf(CerradoValidationException.class);

        CerradoPreferences tooManyDecimals = basePreferences()
                .targetAcoesBr(new BigDecimal("33.333"))
                .targetFiis(new BigDecimal("16.667"))
                .targetRendaFixa(new BigDecimal("25.00"))
                .targetInternacional(new BigDecimal("20.00"))
                .targetCripto(new BigDecimal("5.00"))
                .build();
        assertThatThrownBy(() -> engine.calculate(tooManyDecimals, List.of()))
                .isInstanceOf(CerradoValidationException.class);

        CerradoPreferences zeroMultiplier = basePreferences()
                .grahamMultiplier(BigDecimal.ZERO)
                .build();
        assertThatThrownBy(() -> engine.calculate(zeroMultiplier, List.of()))
                .isInstanceOf(CerradoValidationException.class);

        CerradoPreferences negativeMultiplier = basePreferences()
                .grahamMultiplier(new BigDecimal("-1.00"))
                .build();
        assertThatThrownBy(() -> engine.calculate(negativeMultiplier, List.of()))
                .isInstanceOf(CerradoValidationException.class);
    }

    @Test
    @DisplayName("Should reject invalid assets: null list, null element, blank/duplicate ticker, incompatible subclass, invalid grade or ceiling")
    void shouldRejectInvalidAssetInputs() {
        CerradoPreferences prefs = basePreferences().build();

        assertThatThrownBy(() -> engine.calculate(prefs, null))
                .isInstanceOf(CerradoValidationException.class);

        assertThatThrownBy(() -> engine.calculate(prefs, Arrays.asList((CerradoAssetInput) null)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput blankTicker = CerradoAssetInput.builder()
                .ticker("   ")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(8)
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(blankTicker)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput dup1 = CerradoAssetInput.builder()
                .ticker("petr4")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(8)
                .build();
        CerradoAssetInput dup2 = CerradoAssetInput.builder()
                .ticker(" PETR4 ")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(9)
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(dup1, dup2)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput incompatibleSubClass = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.FII_BRICK)
                .qualityGrade(9)
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(incompatibleSubClass)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput invalidGrade = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .qualityGrade(11)
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(invalidGrade)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput zeroCeiling = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .qualityGrade(9)
                .customRiskCeiling(BigDecimal.ZERO)
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(zeroCeiling)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput overCeiling = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .qualityGrade(9)
                .customRiskCeiling(new BigDecimal("100.01"))
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(overCeiling)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput negativeCeiling = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .qualityGrade(9)
                .customRiskCeiling(new BigDecimal("-1.00"))
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(negativeCeiling)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput tooManyCeilingDecimals = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .qualityGrade(9)
                .customRiskCeiling(new BigDecimal("0.004"))
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(tooManyCeilingDecimals)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput nullMacroClass = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(null)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .qualityGrade(9)
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(nullMacroClass)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput nullSubClass = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(null)
                .qualityGrade(9)
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(nullSubClass)))
                .isInstanceOf(CerradoValidationException.class);

        CerradoAssetInput zeroGrade = CerradoAssetInput.builder()
                .ticker("WEGE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_GROWTH)
                .qualityGrade(0)
                .build();
        assertThatThrownBy(() -> engine.calculate(prefs, List.of(zeroGrade)))
                .isInstanceOf(CerradoValidationException.class);

        assertThatThrownBy(() -> CerradoAssetInput.fromGrade(null, false))
                .isInstanceOf(CerradoValidationException.class);
    }

    @Test
    @DisplayName("Should calculate proportional allocation and reconcile centesimos deterministically without exceeding Meta_C")
    void shouldCalculateProportionalAndReconcileCentesimos() {
        CerradoPreferences prefs = CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(new BigDecimal("20.00"))
                .targetFiis(new BigDecimal("30.00"))
                .targetRendaFixa(new BigDecimal("50.00"))
                .targetInternacional(new BigDecimal("0.00"))
                .targetCripto(new BigDecimal("0.00"))
                .build();

        List<CerradoAssetInput> assets = List.of(
                asset("wege3", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_GROWTH, 10),
                asset("bbas3", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_DEFENSIVE, 10),
                asset("itub4", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_DEFENSIVE, 10),
                asset("hglg11", MacroClass.FIIS, CerradoSubClass.FII_BRICK, 10),
                asset("knri11", MacroClass.FIIS, CerradoSubClass.FII_BRICK, 8),
                asset("mxrf11", MacroClass.FIIS, CerradoSubClass.FII_PAPER, 6),
                asset("bcff11", MacroClass.FIIS, CerradoSubClass.FII_FOF, 6)
        );

        CerradoAllocationResult result = engine.calculate(prefs, assets);

        CerradoMacroClassSummary acoes = result.macroClasses().get(MacroClass.ACOES_BR);
        assertThat(acoes.allocatedPct()).isEqualByComparingTo("20.00");
        assertThat(acoes.unallocatedResidualPct()).isEqualByComparingTo("0.00");
        assertThat(acoes.assets()).extracting(CerradoAssetAllocation::ticker)
                .containsExactly("BBAS3", "ITUB4", "WEGE3");
        assertThat(acoes.assets()).extracting(CerradoAssetAllocation::effectiveTargetPct)
                .containsExactly(new BigDecimal("6.67"), new BigDecimal("6.67"), new BigDecimal("6.66"));

        CerradoMacroClassSummary fiis = result.macroClasses().get(MacroClass.FIIS);
        assertThat(fiis.allocatedPct()).isEqualByComparingTo("30.00");
        assertThat(fiis.unallocatedResidualPct()).isEqualByComparingTo("0.00");
        assertThat(fiis.assets()).extracting(CerradoAssetAllocation::ticker)
                .containsExactly("BCFF11", "HGLG11", "KNRI11", "MXRF11");
        assertThat(fiis.assets()).extracting(CerradoAssetAllocation::effectiveTargetPct)
                .containsExactly(new BigDecimal("6.00"), new BigDecimal("10.00"), new BigDecimal("8.00"), new BigDecimal("6.00"));
    }

    @Test
    @DisplayName("Should iteratively redistribute ceiling surplus across unsaturated assets without exceeding individual caps")
    void shouldRedistributeSurplusIterativelyAcrossMultipleRounds() {
        CerradoPreferences prefs = CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(new BigDecimal("35.00"))
                .targetFiis(new BigDecimal("0.00"))
                .targetRendaFixa(new BigDecimal("65.00"))
                .targetInternacional(new BigDecimal("0.00"))
                .targetCripto(new BigDecimal("0.00"))
                .build();

        List<CerradoAssetInput> assets = List.of(
                asset("A1", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_GROWTH, 10),
                asset("B2", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_DEFENSIVE, 7),
                asset("C3", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_GROWTH, 9),
                asset("D4", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_DEFENSIVE, 8)
        );

        CerradoAllocationResult result = engine.calculate(prefs, assets);
        CerradoMacroClassSummary acoes = result.macroClasses().get(MacroClass.ACOES_BR);

        assertThat(acoes.allocatedPct()).isEqualByComparingTo("35.00");
        assertThat(acoes.unallocatedResidualPct()).isEqualByComparingTo("0.00");

        // A1 (teto 10%), B2 (teto 6%), C3 (satura na rodada 1 em 10%), D4 (recebe o restante = 9%)
        assertThat(acoes.assets()).extracting(CerradoAssetAllocation::effectiveTargetPct)
                .containsExactly(
                        new BigDecimal("10.00"),
                        new BigDecimal("6.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("9.00")
                );
        assertThat(acoes.assets().get(0).ceilingApplied()).isTrue();
        assertThat(acoes.assets().get(1).ceilingApplied()).isTrue();
        assertThat(acoes.assets().get(2).ceilingApplied()).isTrue();
        assertThat(acoes.assets().get(3).ceilingApplied()).isFalse();
    }

    @Test
    @DisplayName("Should report unallocatedResidualPct when all eligible assets saturate at their ceilings")
    void shouldReportUnallocatedResidualWhenClassSaturates() {
        CerradoPreferences prefs = CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(new BigDecimal("25.00"))
                .targetFiis(new BigDecimal("20.00"))
                .targetRendaFixa(new BigDecimal("25.00"))
                .targetInternacional(new BigDecimal("15.00"))
                .targetCripto(new BigDecimal("15.00"))
                .build();

        List<CerradoAssetInput> assets = List.of(
                asset("BTC", MacroClass.CRIPTO, CerradoSubClass.CRYPTO_MAJOR, 10)
        );

        CerradoAllocationResult result = engine.calculate(prefs, assets);
        CerradoMacroClassSummary cripto = result.macroClasses().get(MacroClass.CRIPTO);

        assertThat(cripto.allocatedPct()).isEqualByComparingTo("10.00");
        assertThat(cripto.unallocatedResidualPct()).isEqualByComparingTo("5.00");
        assertThat(cripto.assets().get(0).ceilingApplied()).isTrue();
        assertThat(cripto.assets().get(0).grossTargetPct()).isEqualByComparingTo("15.00");
        assertThat(cripto.assets().get(0).effectiveTargetPct()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Should exempt sovereign treasury and diversified broad ETFs from individual 10%/6% cap")
    void shouldExemptSovereignTreasuryAndBroadEtfs() {
        CerradoPreferences prefs = CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(new BigDecimal("20.00"))
                .targetFiis(new BigDecimal("20.00"))
                .targetRendaFixa(new BigDecimal("40.00"))
                .targetInternacional(new BigDecimal("20.00"))
                .targetCripto(new BigDecimal("0.00"))
                .build();

        CerradoAssetInput tesouro = asset("TESOURO_IPCA_2035", MacroClass.RENDA_FIXA, CerradoSubClass.TREASURY_IPCA, 8);
        CerradoAssetInput ivvb11 = CerradoAssetInput.builder()
                .ticker("IVVB11")
                .macroClass(MacroClass.INTERNACIONAL)
                .subClass(CerradoSubClass.INTERNATIONAL_ETF)
                .qualityGrade(8)
                .isDiversifiedBroadEtf(true)
                .build();

        CerradoAllocationResult result = engine.calculate(prefs, List.of(tesouro, ivvb11));

        CerradoMacroClassSummary rf = result.macroClasses().get(MacroClass.RENDA_FIXA);
        assertThat(rf.allocatedPct()).isEqualByComparingTo("40.00");
        assertThat(rf.unallocatedResidualPct()).isEqualByComparingTo("0.00");
        assertThat(rf.assets().get(0).ceilingExempt()).isTrue();
        assertThat(rf.assets().get(0).ceilingApplied()).isFalse();

        CerradoMacroClassSummary intl = result.macroClasses().get(MacroClass.INTERNACIONAL);
        assertThat(intl.allocatedPct()).isEqualByComparingTo("20.00");
        assertThat(intl.unallocatedResidualPct()).isEqualByComparingTo("0.00");
        assertThat(intl.assets().get(0).ceilingExempt()).isTrue();
        assertThat(intl.assets().get(0).ceilingApplied()).isFalse();
    }

    @Test
    @DisplayName("Should quarantine assets with grade 1-4 or corporate event and exclude them from denominator")
    void shouldQuarantineLowGradeAndCorporateEventAssets() {
        CerradoPreferences prefs = CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(new BigDecimal("16.00"))
                .targetFiis(new BigDecimal("20.00"))
                .targetRendaFixa(new BigDecimal("64.00"))
                .targetInternacional(new BigDecimal("0.00"))
                .targetCripto(new BigDecimal("0.00"))
                .build();

        CerradoAssetInput lowGrade = asset("OIBR3", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_GROWTH, 4);
        CerradoAssetInput corpEvent = CerradoAssetInput.builder()
                .ticker("AMER3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.OUTROS_MANUAL)
                .qualityGrade(8)
                .quarantineCorporateEvent(true)
                .build();
        CerradoAssetInput healthy1 = asset("ITUB4", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_DEFENSIVE, 8);
        CerradoAssetInput healthy2 = asset("WEGE3", MacroClass.ACOES_BR, CerradoSubClass.EQUITY_GROWTH, 8);

        CerradoAssetInput fiiQuarantined = asset("FIIQ11", MacroClass.FIIS, CerradoSubClass.FII_PAPER, 2);

        CerradoAllocationResult result = engine.calculate(
                prefs,
                List.of(lowGrade, corpEvent, healthy1, healthy2, fiiQuarantined)
        );

        CerradoMacroClassSummary acoes = result.macroClasses().get(MacroClass.ACOES_BR);
        assertThat(acoes.allocatedPct()).isEqualByComparingTo("16.00");
        assertThat(acoes.unallocatedResidualPct()).isEqualByComparingTo("0.00");
        assertThat(acoes.eligibleAssets()).isEqualTo(2);
        assertThat(acoes.quarantinedAssets()).isEqualTo(2);

        // Ordem A-Z: AMER3 (0%), ITUB4 (8%), OIBR3 (0%), WEGE3 (8%)
        assertThat(acoes.assets()).extracting(CerradoAssetAllocation::ticker)
                .containsExactly("AMER3", "ITUB4", "OIBR3", "WEGE3");
        assertThat(acoes.assets()).extracting(CerradoAssetAllocation::effectiveTargetPct)
                .containsExactly(
                        new BigDecimal("0.00"),
                        new BigDecimal("8.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("8.00")
                );
        assertThat(acoes.assets().get(0).quarantined()).isTrue();
        assertThat(acoes.assets().get(2).quarantined()).isTrue();

        // Todos em quarentena -> 0.00% alocado e 20.00% residual
        CerradoMacroClassSummary fiis = result.macroClasses().get(MacroClass.FIIS);
        assertThat(fiis.allocatedPct()).isEqualByComparingTo("0.00");
        assertThat(fiis.unallocatedResidualPct()).isEqualByComparingTo("20.00");
        assertThat(fiis.assets().get(0).quarantined()).isTrue();
    }

    @Test
    @DisplayName("Should apply customRiskCeiling when explicitly provided and return immutable collections")
    void shouldApplyCustomRiskCeilingAndReturnImmutableCollections() {
        CerradoPreferences prefs = CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(new BigDecimal("10.00"))
                .targetFiis(new BigDecimal("0.00"))
                .targetRendaFixa(new BigDecimal("90.00"))
                .targetInternacional(new BigDecimal("0.00"))
                .targetCripto(new BigDecimal("0.00"))
                .build();

        CerradoAssetInput customCapped = CerradoAssetInput.builder()
                .ticker("VALE3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(10)
                .customRiskCeiling(new BigDecimal("4.50"))
                .build();

        CerradoAllocationResult result = engine.calculate(prefs, List.of(customCapped));
        CerradoMacroClassSummary acoes = result.macroClasses().get(MacroClass.ACOES_BR);

        assertThat(acoes.allocatedPct()).isEqualByComparingTo("4.50");
        assertThat(acoes.unallocatedResidualPct()).isEqualByComparingTo("5.50");
        assertThat(acoes.assets().get(0).riskCeilingPct()).isEqualByComparingTo("4.50");
        assertThat(acoes.assets().get(0).ceilingApplied()).isTrue();

        assertThat(result.totalAllocatedPct()).isEqualByComparingTo("4.50");
        assertThat(result.totalUnallocatedResidualPct()).isEqualByComparingTo("95.50");
        assertThat(result.totalAllocatedPct().add(result.totalUnallocatedResidualPct()))
                .isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> result.allocations().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.macroClasses().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should handle empty asset list, empty macro class, zero Meta_C with eligible assets, fromGrade, and share == headroom")
    void shouldHandleEmptyClassesZeroMetaFromGradeAndExactHeadroomMatch() {
        CerradoPreferences prefs = CerradoPreferences.builder()
                .userId(1L)
                .targetAcoesBr(new BigDecimal("16.00"))
                .targetFiis(new BigDecimal("20.00"))
                .targetRendaFixa(new BigDecimal("64.00"))
                .targetInternacional(new BigDecimal("0.00"))
                .targetCripto(new BigDecimal("0.00"))
                .build();

        // Carteira vazia -> todas as classes ficam com 0.00 alocado e residual igual a targetPct
        CerradoAllocationResult emptyResult = engine.calculate(prefs, List.of());
        assertThat(emptyResult.totalAllocatedPct()).isEqualByComparingTo("0.00");
        assertThat(emptyResult.totalUnallocatedResidualPct()).isEqualByComparingTo("100.00");
        CerradoMacroClassSummary emptyFiis = emptyResult.macroClasses().get(MacroClass.FIIS);
        assertThat(emptyFiis.allocatedPct()).isEqualByComparingTo("0.00");
        assertThat(emptyFiis.unallocatedResidualPct()).isEqualByComparingTo("20.00");
        assertThat(emptyFiis.totalAssets()).isZero();

        // Meta_C == 0.00% com ativo elegivel + share == headroom (ceilingApplied == false) + fromGrade valido
        CerradoAssetGrade gradeEntity = CerradoAssetGrade.builder()
                .userId(1L)
                .ticker("IVVB11")
                .macroClass(MacroClass.INTERNACIONAL)
                .subClass(CerradoSubClass.INTERNATIONAL_ETF)
                .qualityGrade(9)
                .build();
        CerradoAssetInput broadEtfZeroMeta = CerradoAssetInput.fromGrade(gradeEntity, true);

        CerradoAssetInput a = CerradoAssetInput.builder()
                .ticker("ABEV3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(10)
                .customRiskCeiling(new BigDecimal("8.00"))
                .build();
        CerradoAssetInput b = CerradoAssetInput.builder()
                .ticker("BBAS3")
                .macroClass(MacroClass.ACOES_BR)
                .subClass(CerradoSubClass.EQUITY_DEFENSIVE)
                .qualityGrade(6)
                .customRiskCeiling(new BigDecimal("8.00"))
                .build();

        CerradoAllocationResult result = engine.calculate(prefs, List.of(a, b, broadEtfZeroMeta));

        CerradoMacroClassSummary acoes = result.macroClasses().get(MacroClass.ACOES_BR);
        assertThat(acoes.allocatedPct()).isEqualByComparingTo("16.00");
        assertThat(acoes.unallocatedResidualPct()).isEqualByComparingTo("0.00");
        assertThat(acoes.assets().get(0).ticker()).isEqualTo("ABEV3");
        assertThat(acoes.assets().get(0).effectiveTargetPct()).isEqualByComparingTo("8.00");
        assertThat(acoes.assets().get(0).ceilingApplied()).isTrue();
        assertThat(acoes.assets().get(1).ticker()).isEqualTo("BBAS3");
        assertThat(acoes.assets().get(1).effectiveTargetPct()).isEqualByComparingTo("8.00");
        assertThat(acoes.assets().get(1).ceilingApplied()).isFalse();

        CerradoMacroClassSummary intl = result.macroClasses().get(MacroClass.INTERNACIONAL);
        assertThat(intl.allocatedPct()).isEqualByComparingTo("0.00");
        assertThat(intl.unallocatedResidualPct()).isEqualByComparingTo("0.00");
        assertThat(intl.assets().get(0).effectiveTargetPct()).isEqualByComparingTo("0.00");
        assertThat(intl.assets().get(0).ceilingExempt()).isTrue();

        assertThat(result.totalAllocatedPct()).isEqualByComparingTo("16.00");
        assertThat(result.totalUnallocatedResidualPct()).isEqualByComparingTo("84.00");
    }

    private static CerradoAssetInput asset(String ticker, MacroClass mc, CerradoSubClass sc, int grade) {
        return CerradoAssetInput.builder()
                .ticker(ticker)
                .macroClass(mc)
                .subClass(sc)
                .qualityGrade(grade)
                .build();
    }
}
