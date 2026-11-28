package afsdigital.grahamselect.common.cerrado.domain.service;

import afsdigital.grahamselect.common.cerrado.domain.exceptions.CerradoValidationException;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAssetAllocation;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAssetInput;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoAllocationResult;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoMacroClassSummary;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoPreferences;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Motor de dominio puro, stateless e deterministico para calculo de metas
 * estrategicas do Diagrama do Cerrado, aplicando quarentena de aporte,
 * teto maximo de risco, isencoes e redistribuicao iterativa de excedentes.
 */
public class CerradoAllocationEngine {

    private static final int INTERMEDIATE_SCALE = 4;
    private static final int OUTPUT_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private static final BigDecimal ZERO_2 = BigDecimal.ZERO.setScale(OUTPUT_SCALE, ROUNDING);
    private static final BigDecimal ZERO_4 = BigDecimal.ZERO.setScale(INTERMEDIATE_SCALE, ROUNDING);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");
    private static final BigDecimal MAX_GRAHAM_MULTIPLIER = new BigDecimal("99.99");

    private static final BigDecimal CEILING_HIGH_GRADE = new BigDecimal("10.00");
    private static final BigDecimal CEILING_MID_GRADE = new BigDecimal("6.00");
    private static final int HIGH_GRADE_THRESHOLD = 8;
    private static final int QUARANTINE_MAX_GRADE = 4;
    private static final int MIN_GRADE = 1;
    private static final int MAX_GRADE = 10;

    /**
     * Calcula a alocacao efetiva para todos os ativos informados com base nas
     * preferencias estrategicas das 5 Macro Classes.
     */
    public CerradoAllocationResult calculate(CerradoPreferences preferences, List<CerradoAssetInput> assets) {
        validatePreferences(preferences);
        List<NormalizedAsset> normalizedAssets = validateAndNormalizeAssets(assets);

        Map<MacroClass, List<NormalizedAsset>> byMacroClass = new EnumMap<>(MacroClass.class);
        for (MacroClass mc : MacroClass.values()) {
            byMacroClass.put(mc, new ArrayList<>());
        }
        for (NormalizedAsset asset : normalizedAssets) {
            byMacroClass.get(asset.macroClass()).add(asset);
        }

        Map<MacroClass, CerradoMacroClassSummary> summaries = new EnumMap<>(MacroClass.class);
        List<CerradoAssetAllocation> allAllocations = new ArrayList<>();
        BigDecimal totalAllocated = ZERO_2;
        BigDecimal totalResidual = ZERO_2;

        for (MacroClass mc : MacroClass.values()) {
            List<NormalizedAsset> classAssets = byMacroClass.get(mc);
            classAssets.sort(Comparator.comparing(NormalizedAsset::ticker));

            BigDecimal metaC = targetFor(preferences, mc).setScale(OUTPUT_SCALE, ROUNDING);
            CerradoMacroClassSummary summary = calculateMacroClass(mc, metaC, classAssets);

            summaries.put(mc, summary);
            allAllocations.addAll(summary.assets());
            totalAllocated = totalAllocated.add(summary.allocatedPct());
            totalResidual = totalResidual.add(summary.unallocatedResidualPct());
        }

        return CerradoAllocationResult.builder()
                .grahamMultiplier(preferences.getGrahamMultiplier())
                .grahamFilterEnabled(preferences.isGrahamFilterEnabled())
                .macroClasses(summaries)
                .allocations(allAllocations)
                .totalAllocatedPct(totalAllocated.setScale(OUTPUT_SCALE, ROUNDING))
                .totalUnallocatedResidualPct(totalResidual.setScale(OUTPUT_SCALE, ROUNDING))
                .build();
    }

    private CerradoMacroClassSummary calculateMacroClass(
            MacroClass macroClass,
            BigDecimal metaC,
            List<NormalizedAsset> classAssets
    ) {
        if (classAssets.isEmpty()) {
            return CerradoMacroClassSummary.builder()
                    .macroClass(macroClass)
                    .targetPct(metaC)
                    .allocatedPct(ZERO_2)
                    .unallocatedResidualPct(metaC)
                    .totalAssets(0)
                    .eligibleAssets(0)
                    .quarantinedAssets(0)
                    .assets(List.of())
                    .build();
        }

        int n = classAssets.size();
        boolean[] quarantined = new boolean[n];
        boolean[] exempt = new boolean[n];
        BigDecimal[] riskCeilingOutput = new BigDecimal[n];
        BigDecimal[] effectiveCap4 = new BigDecimal[n];
        BigDecimal[] grossTarget4 = new BigDecimal[n];
        BigDecimal[] allocated4 = new BigDecimal[n];
        boolean[] saturated = new boolean[n];
        boolean[] ceilingApplied = new boolean[n];

        List<Integer> eligibleIndices = new ArrayList<>();
        long eligibleGradeSum = 0L;
        int quarantinedCount = 0;

        BigDecimal metaC4 = metaC.setScale(INTERMEDIATE_SCALE, ROUNDING);

        for (int i = 0; i < n; i++) {
            NormalizedAsset asset = classAssets.get(i);
            boolean isQuarantined = asset.qualityGrade() <= QUARANTINE_MAX_GRADE || asset.quarantineCorporateEvent();
            boolean isExempt = asset.subClass().isSovereignTreasury() || asset.isDiversifiedBroadEtf();
            quarantined[i] = isQuarantined;
            exempt[i] = isExempt;

            if (isQuarantined) {
                quarantinedCount++;
                grossTarget4[i] = ZERO_4;
                allocated4[i] = ZERO_4;
            } else {
                eligibleIndices.add(i);
                eligibleGradeSum += asset.qualityGrade();

                BigDecimal resolvedCeiling = resolveRiskCeiling(asset, isExempt);
                riskCeilingOutput[i] = resolvedCeiling != null ? resolvedCeiling.setScale(OUTPUT_SCALE, ROUNDING) : null;

                BigDecimal cap4 = resolvedCeiling != null
                        ? resolvedCeiling.setScale(INTERMEDIATE_SCALE, ROUNDING).min(metaC4)
                        : metaC4;
                effectiveCap4[i] = cap4;
            }
        }

        if (eligibleIndices.isEmpty() || metaC.compareTo(BigDecimal.ZERO) == 0) {
            List<CerradoAssetAllocation> allocations = buildZeroAllocations(classAssets, quarantined, exempt, riskCeilingOutput);
            return CerradoMacroClassSummary.builder()
                    .macroClass(macroClass)
                    .targetPct(metaC)
                    .allocatedPct(ZERO_2)
                    .unallocatedResidualPct(metaC)
                    .totalAssets(n)
                    .eligibleAssets(eligibleIndices.size())
                    .quarantinedAssets(quarantinedCount)
                    .assets(allocations)
                    .build();
        }

        BigDecimal totalGradeBd = BigDecimal.valueOf(eligibleGradeSum);
        BigDecimal surplus = ZERO_4;

        for (int idx : eligibleIndices) {
            int grade = classAssets.get(idx).qualityGrade();
            BigDecimal gross = metaC4.multiply(BigDecimal.valueOf(grade))
                    .divide(totalGradeBd, INTERMEDIATE_SCALE, ROUNDING);
            grossTarget4[idx] = gross;

            if (gross.compareTo(effectiveCap4[idx]) > 0) {
                surplus = surplus.add(gross.subtract(effectiveCap4[idx]));
                allocated4[idx] = effectiveCap4[idx];
                saturated[idx] = true;
                ceilingApplied[idx] = true;
            } else {
                allocated4[idx] = gross;
                if (gross.compareTo(effectiveCap4[idx]) == 0) {
                    saturated[idx] = true;
                }
            }
        }

        // Redistribuicao iterativa proporcional as notas entre ativos elegiveis nao-saturados
        while (surplus.compareTo(BigDecimal.ZERO) > 0) {
            List<Integer> unsaturated = new ArrayList<>();
            long unsaturatedGradeSum = 0L;

            for (int idx : eligibleIndices) {
                if (!saturated[idx] && allocated4[idx].compareTo(effectiveCap4[idx]) < 0) {
                    unsaturated.add(idx);
                    unsaturatedGradeSum += classAssets.get(idx).qualityGrade();
                }
            }

            if (unsaturated.isEmpty()) {
                break;
            }

            BigDecimal roundGradeSum = BigDecimal.valueOf(unsaturatedGradeSum);
            BigDecimal distributedInRound = ZERO_4;

            for (int idx : unsaturated) {
                BigDecimal share = surplus.multiply(BigDecimal.valueOf(classAssets.get(idx).qualityGrade()))
                        .divide(roundGradeSum, INTERMEDIATE_SCALE, ROUNDING);
                BigDecimal headroom = effectiveCap4[idx].subtract(allocated4[idx]);

                if (share.compareTo(headroom) >= 0) {
                    allocated4[idx] = effectiveCap4[idx];
                    saturated[idx] = true;
                    if (share.compareTo(headroom) > 0) {
                        ceilingApplied[idx] = true;
                    }
                    distributedInRound = distributedInRound.add(headroom);
                } else {
                    allocated4[idx] = allocated4[idx].add(share);
                    distributedInRound = distributedInRound.add(share);
                }
            }

            if (distributedInRound.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }

            surplus = surplus.subtract(distributedInRound);
        }

        // Reconciliacao deterministica de centesimos (Largest Remainder) para escala 2
        BigDecimal[] effective2 = reconcileToScale2(
                classAssets,
                eligibleIndices,
                allocated4,
                effectiveCap4,
                saturated,
                metaC
        );

        List<CerradoAssetAllocation> resultAssets = new ArrayList<>(n);
        BigDecimal allocatedSum = ZERO_2;

        for (int i = 0; i < n; i++) {
            NormalizedAsset asset = classAssets.get(i);
            BigDecimal effPct = quarantined[i] ? ZERO_2 : effective2[i];
            BigDecimal grossPct = quarantined[i] ? ZERO_2 : grossTarget4[i].setScale(OUTPUT_SCALE, ROUNDING);
            allocatedSum = allocatedSum.add(effPct);

            resultAssets.add(CerradoAssetAllocation.builder()
                    .ticker(asset.ticker())
                    .macroClass(asset.macroClass())
                    .subClass(asset.subClass())
                    .qualityGrade(asset.qualityGrade())
                    .quarantined(quarantined[i])
                    .quarantineCorporateEvent(asset.quarantineCorporateEvent())
                    .ceilingExempt(exempt[i])
                    .ceilingApplied(ceilingApplied[i])
                    .riskCeilingPct(riskCeilingOutput[i])
                    .grossTargetPct(grossPct)
                    .effectiveTargetPct(effPct)
                    .build());
        }

        BigDecimal residual = metaC.subtract(allocatedSum).setScale(OUTPUT_SCALE, ROUNDING);
        if (residual.compareTo(BigDecimal.ZERO) < 0) {
            residual = ZERO_2;
        }

        return CerradoMacroClassSummary.builder()
                .macroClass(macroClass)
                .targetPct(metaC)
                .allocatedPct(allocatedSum.setScale(OUTPUT_SCALE, ROUNDING))
                .unallocatedResidualPct(residual)
                .totalAssets(n)
                .eligibleAssets(eligibleIndices.size())
                .quarantinedAssets(quarantinedCount)
                .assets(resultAssets)
                .build();
    }

    private BigDecimal[] reconcileToScale2(
            List<NormalizedAsset> classAssets,
            List<Integer> eligibleIndices,
            BigDecimal[] allocated4,
            BigDecimal[] effectiveCap4,
            boolean[] saturated,
            BigDecimal metaC
    ) {
        int n = classAssets.size();
        BigDecimal[] result2 = new BigDecimal[n];
        BigDecimal[] remainder4 = new BigDecimal[n];

        BigDecimal rawSum4 = ZERO_4;
        BigDecimal floorSum2 = ZERO_2;
        boolean allSaturated = true;

        for (int idx : eligibleIndices) {
            rawSum4 = rawSum4.add(allocated4[idx]);
            BigDecimal floor = allocated4[idx].setScale(OUTPUT_SCALE, RoundingMode.DOWN);
            result2[idx] = floor;
            remainder4[idx] = allocated4[idx].subtract(floor);
            floorSum2 = floorSum2.add(floor);
            if (!saturated[idx]) {
                allSaturated = false;
            }
        }

        BigDecimal targetTotal2 = allSaturated
                ? rawSum4.setScale(OUTPUT_SCALE, ROUNDING).min(metaC)
                : metaC;

        int centsDiff = targetTotal2.subtract(floorSum2).movePointRight(OUTPUT_SCALE).intValue();

        if (centsDiff > 0) {
            List<Integer> candidates = new ArrayList<>(eligibleIndices);
            candidates.sort(
                    Comparator.<Integer, BigDecimal>comparing(idx -> remainder4[idx]).reversed()
                            .thenComparing(Comparator.<Integer>comparingInt(idx -> classAssets.get(idx).qualityGrade()).reversed())
                            .thenComparing(idx -> classAssets.get(idx).ticker())
            );

            for (int idx : candidates) {
                if (centsDiff == 0) {
                    break;
                }
                BigDecimal bumped = result2[idx].add(ONE_CENT);
                if (bumped.setScale(INTERMEDIATE_SCALE, ROUNDING).compareTo(effectiveCap4[idx]) <= 0) {
                    result2[idx] = bumped;
                    centsDiff--;
                }
            }
        } else if (centsDiff < 0) {
            List<Integer> candidates = new ArrayList<>(eligibleIndices);
            candidates.sort(
                    Comparator.<Integer, BigDecimal>comparing(idx -> remainder4[idx])
                            .thenComparingInt(idx -> classAssets.get(idx).qualityGrade())
                            .thenComparing(Comparator.<Integer, String>comparing(idx -> classAssets.get(idx).ticker()).reversed())
            );

            for (int idx : candidates) {
                if (centsDiff == 0) {
                    break;
                }
                if (result2[idx].compareTo(ONE_CENT) >= 0) {
                    result2[idx] = result2[idx].subtract(ONE_CENT);
                    centsDiff++;
                }
            }
        }

        return result2;
    }

    private List<CerradoAssetAllocation> buildZeroAllocations(
            List<NormalizedAsset> classAssets,
            boolean[] quarantined,
            boolean[] exempt,
            BigDecimal[] riskCeilingOutput
    ) {
        List<CerradoAssetAllocation> result = new ArrayList<>(classAssets.size());
        for (int i = 0; i < classAssets.size(); i++) {
            NormalizedAsset asset = classAssets.get(i);
            result.add(CerradoAssetAllocation.builder()
                    .ticker(asset.ticker())
                    .macroClass(asset.macroClass())
                    .subClass(asset.subClass())
                    .qualityGrade(asset.qualityGrade())
                    .quarantined(quarantined[i])
                    .quarantineCorporateEvent(asset.quarantineCorporateEvent())
                    .ceilingExempt(exempt[i])
                    .ceilingApplied(false)
                    .riskCeilingPct(riskCeilingOutput[i])
                    .grossTargetPct(ZERO_2)
                    .effectiveTargetPct(ZERO_2)
                    .build());
        }
        return result;
    }

    private BigDecimal resolveRiskCeiling(NormalizedAsset asset, boolean isExempt) {
        if (asset.customRiskCeiling() != null) {
            return asset.customRiskCeiling();
        }
        if (isExempt) {
            return null;
        }
        return asset.qualityGrade() >= HIGH_GRADE_THRESHOLD ? CEILING_HIGH_GRADE : CEILING_MID_GRADE;
    }

    /**
     * Valida todas as preferencias do Cerrado, garantindo metas das 5 Macro Classes
     * com soma exata de 100.00% e multiplicador de Graham valido.
     */
    public void validatePreferences(CerradoPreferences preferences) {
        if (preferences == null) {
            throw new CerradoValidationException("Preferencias do Cerrado nao podem ser nulas.");
        }

        validateTargetValues(
                preferences.getTargetAcoesBr(),
                preferences.getTargetFiis(),
                preferences.getTargetRendaFixa(),
                preferences.getTargetInternacional(),
                preferences.getTargetCripto(),
                preferences.getGrahamMultiplier()
        );
    }

    /**
     * Valida os valores brutos das metas das 5 Macro Classes e o multiplicador de Graham
     * antes da construcao da entidade de dominio.
     */
    public void validateTargetValues(
            BigDecimal targetAcoesBr,
            BigDecimal targetFiis,
            BigDecimal targetRendaFixa,
            BigDecimal targetInternacional,
            BigDecimal targetCripto,
            BigDecimal grahamMultiplier
    ) {
        validateTarget(targetAcoesBr, "targetAcoesBr");
        validateTarget(targetFiis, "targetFiis");
        validateTarget(targetRendaFixa, "targetRendaFixa");
        validateTarget(targetInternacional, "targetInternacional");
        validateTarget(targetCripto, "targetCripto");

        BigDecimal sum = targetAcoesBr
                .add(targetFiis)
                .add(targetRendaFixa)
                .add(targetInternacional)
                .add(targetCripto);

        if (sum.compareTo(ONE_HUNDRED) != 0) {
            throw new CerradoValidationException(
                    "A soma das metas das 5 Macro Classes deve ser exatamente 100.00%, mas foi "
                            + sum.toPlainString() + "%."
            );
        }

        if (grahamMultiplier == null
                || grahamMultiplier.compareTo(BigDecimal.ZERO) <= 0
                || grahamMultiplier.compareTo(MAX_GRAHAM_MULTIPLIER) > 0) {
            throw new CerradoValidationException(
                    "grahamMultiplier deve estar no intervalo (0.00, 99.99]."
            );
        }
        if (grahamMultiplier.stripTrailingZeros().scale() > OUTPUT_SCALE) {
            throw new CerradoValidationException("grahamMultiplier deve ter no maximo 2 casas decimais.");
        }
    }

    private void validateTarget(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new CerradoValidationException(fieldName + " nao pode ser nulo.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new CerradoValidationException(fieldName + " deve estar entre 0.00 e 100.00.");
        }
        if (value.stripTrailingZeros().scale() > OUTPUT_SCALE) {
            throw new CerradoValidationException(fieldName + " deve ter no maximo 2 casas decimais.");
        }
    }

    private List<NormalizedAsset> validateAndNormalizeAssets(List<CerradoAssetInput> assets) {
        if (assets == null) {
            throw new CerradoValidationException("Lista de ativos nao pode ser nula.");
        }

        List<NormalizedAsset> normalized = new ArrayList<>(assets.size());
        Set<String> seenTickers = new HashSet<>();

        for (CerradoAssetInput asset : assets) {
            if (asset == null) {
                throw new CerradoValidationException("Elemento da lista de ativos nao pode ser nulo.");
            }
            if (asset.ticker() == null || asset.ticker().isBlank()) {
                throw new CerradoValidationException("Ticker do ativo nao pode ser vazio ou em branco.");
            }

            String ticker = asset.ticker().trim().toUpperCase(Locale.ROOT);
            if (!seenTickers.add(ticker)) {
                throw new CerradoValidationException("Ticker duplicado na carteira: " + ticker);
            }

            MacroClass macroClass = asset.macroClass();
            if (macroClass == null) {
                throw new CerradoValidationException("MacroClass nao pode ser nula para o ativo " + ticker);
            }

            CerradoSubClass subClass = asset.subClass();
            if (subClass == null) {
                throw new CerradoValidationException("CerradoSubClass nao pode ser nula para o ativo " + ticker);
            }

            if (!subClass.belongsTo(macroClass)) {
                throw new CerradoValidationException(
                        "Subclasse " + subClass + " nao pertence a MacroClass " + macroClass + " para o ativo " + ticker
                );
            }

            int grade = asset.qualityGrade();
            if (grade < MIN_GRADE || grade > MAX_GRADE) {
                throw new CerradoValidationException(
                        "qualityGrade deve estar entre 1 e 10 para o ativo " + ticker + " (recebido: " + grade + ")"
                );
            }

            BigDecimal customCeiling = asset.customRiskCeiling();
            if (customCeiling != null) {
                if (customCeiling.compareTo(BigDecimal.ZERO) <= 0 || customCeiling.compareTo(ONE_HUNDRED) > 0) {
                    throw new CerradoValidationException(
                            "customRiskCeiling deve estar no intervalo (0.00, 100.00] para o ativo " + ticker
                    );
                }
                if (customCeiling.stripTrailingZeros().scale() > OUTPUT_SCALE) {
                    throw new CerradoValidationException(
                            "customRiskCeiling deve ter no maximo 2 casas decimais para o ativo " + ticker
                    );
                }
            }

            normalized.add(new NormalizedAsset(
                    ticker,
                    macroClass,
                    subClass,
                    grade,
                    customCeiling,
                    asset.quarantineCorporateEvent(),
                    asset.isDiversifiedBroadEtf()
            ));
        }

        return normalized;
    }

    private BigDecimal targetFor(CerradoPreferences preferences, MacroClass macroClass) {
        return switch (macroClass) {
            case ACOES_BR -> preferences.getTargetAcoesBr();
            case FIIS -> preferences.getTargetFiis();
            case RENDA_FIXA -> preferences.getTargetRendaFixa();
            case INTERNACIONAL -> preferences.getTargetInternacional();
            case CRIPTO -> preferences.getTargetCripto();
        };
    }

    private record NormalizedAsset(
            String ticker,
            MacroClass macroClass,
            CerradoSubClass subClass,
            int qualityGrade,
            BigDecimal customRiskCeiling,
            boolean quarantineCorporateEvent,
            boolean isDiversifiedBroadEtf
    ) {
    }
}
