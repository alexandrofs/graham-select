package afsdigital.grahamselect.goals.domain.services;

import afsdigital.grahamselect.goals.domain.entities.GoalHealthAssessment;
import afsdigital.grahamselect.goals.domain.entities.GoalHealthStatus;
import afsdigital.grahamselect.goals.domain.entities.GoalProjection;
import afsdigital.grahamselect.goals.domain.entities.GoalSimulationResult;
import afsdigital.grahamselect.goals.domain.entities.GoalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

public class TimeToGoalCalculator {

    private static final BigDecimal DEFAULT_ANNUAL_YIELD_RATE = new BigDecimal("0.06");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final int MAX_SUPPORTED_MONTHS = 12000;

    public GoalProjection calculate(
            BigDecimal targetValue,
            BigDecimal currentEquity,
            BigDecimal monthlyContribution,
            BigDecimal annualYieldPercentage,
            OffsetDateTime referenceDate
    ) {
        return calculate(
                GoalType.PATRIMONY_TARGET,
                targetValue,
                currentEquity,
                monthlyContribution,
                annualYieldPercentage,
                referenceDate
        );
    }

    public GoalProjection calculate(
            GoalType goalType,
            BigDecimal rawTargetValue,
            BigDecimal currentEquity,
            BigDecimal monthlyContribution,
            BigDecimal annualYieldPercentage,
            OffsetDateTime referenceDate
    ) {
        BigDecimal safeRawTarget = rawTargetValue != null ? rawTargetValue : BigDecimal.ZERO;
        BigDecimal pv = (currentEquity != null && currentEquity.compareTo(BigDecimal.ZERO) > 0)
                ? currentEquity
                : BigDecimal.ZERO;
        BigDecimal pmt = (monthlyContribution != null && monthlyContribution.compareTo(BigDecimal.ZERO) > 0)
                ? monthlyContribution
                : BigDecimal.ZERO;
        BigDecimal annualYieldPct = annualYieldPercentage != null ? annualYieldPercentage : BigDecimal.ZERO;

        BigDecimal effectiveTargetValue;
        if (goalType == GoalType.MONTHLY_INCOME_TARGET) {
            BigDecimal annualYieldRate = annualYieldPct.compareTo(BigDecimal.ZERO) > 0
                    ? annualYieldPct.divide(HUNDRED, 8, RoundingMode.HALF_UP)
                    : DEFAULT_ANNUAL_YIELD_RATE;
            effectiveTargetValue = safeRawTarget.multiply(TWELVE)
                    .divide(annualYieldRate, 2, RoundingMode.HALF_UP);
        } else {
            effectiveTargetValue = safeRawTarget.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal progressPercentage;
        if (effectiveTargetValue.compareTo(BigDecimal.ZERO) <= 0) {
            progressPercentage = HUNDRED;
        } else {
            BigDecimal rawProgress = pv.multiply(HUNDRED)
                    .divide(effectiveTargetValue, 2, RoundingMode.HALF_UP);
            if (rawProgress.compareTo(HUNDRED) > 0) {
                progressPercentage = HUNDRED;
            } else if (rawProgress.compareTo(BigDecimal.ZERO) < 0) {
                progressPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                progressPercentage = rawProgress;
            }
        }

        // Case 1: PV >= FV (Goal achieved)
        if (pv.compareTo(effectiveTargetValue) >= 0) {
            return GoalProjection.builder()
                    .targetValue(effectiveTargetValue)
                    .currentEquity(pv)
                    .monthlyContribution(pmt)
                    .annualYieldPercentage(annualYieldPct)
                    .totalMonths(0)
                    .years(0)
                    .months(0)
                    .targetDate(referenceDate)
                    .progressPercentage(HUNDRED)
                    .isAchieved(true)
                    .isUnreachable(false)
                    .build();
        }

        double fvDouble = effectiveTargetValue.doubleValue();
        double pvDouble = pv.doubleValue();
        double pmtDouble = pmt.doubleValue();
        double r = annualYieldPct.doubleValue() / (12.0 * 100.0);

        double rawMonths;

        // Case 2: r > 0 and PV * r + PMT > 0
        if (r > 0 && (pvDouble * r + pmtDouble) > 0 && Math.log(1.0 + r) > 0) {
            double numerator = Math.log((fvDouble * r + pmtDouble) / (pvDouble * r + pmtDouble));
            double denominator = Math.log(1.0 + r);
            rawMonths = Math.ceil(numerator / denominator);
        }
        // Case 3: r <= 0 and PMT > 0
        else if (r <= 0 && pmtDouble > 0) {
            rawMonths = Math.ceil((fvDouble - pvDouble) / pmtDouble);
        }
        // Case 4: Unreachable
        else {
            return buildUnreachable(effectiveTargetValue, pv, pmt, annualYieldPct, progressPercentage);
        }

        if (!Double.isFinite(rawMonths) || rawMonths <= 0 || rawMonths > MAX_SUPPORTED_MONTHS) {
            return buildUnreachable(effectiveTargetValue, pv, pmt, annualYieldPct, progressPercentage);
        }

        int totalMonths = (int) rawMonths;

        int years = totalMonths / 12;
        int months = totalMonths % 12;
        OffsetDateTime targetDate = referenceDate != null ? referenceDate.plusMonths(totalMonths) : null;

        return GoalProjection.builder()
                .targetValue(effectiveTargetValue)
                .currentEquity(pv)
                .monthlyContribution(pmt)
                .annualYieldPercentage(annualYieldPct)
                .totalMonths(totalMonths)
                .years(years)
                .months(months)
                .targetDate(targetDate)
                .progressPercentage(progressPercentage)
                .isAchieved(false)
                .isUnreachable(false)
                .build();
    }

    public GoalSimulationResult simulate(
            GoalType goalType,
            BigDecimal rawTargetValue,
            BigDecimal currentEquity,
            BigDecimal baselineMonthlyContribution,
            BigDecimal baselineAnnualYieldPercentage,
            BigDecimal simulatedMonthlyContribution,
            BigDecimal simulatedAnnualYieldPercentage,
            OffsetDateTime referenceDate
    ) {
        GoalProjection baselineProjection = calculate(
                goalType,
                rawTargetValue,
                currentEquity,
                baselineMonthlyContribution,
                baselineAnnualYieldPercentage,
                referenceDate
        );

        GoalProjection simulatedProjection = calculate(
                GoalType.PATRIMONY_TARGET,
                baselineProjection.getTargetValue(),
                currentEquity,
                simulatedMonthlyContribution,
                simulatedAnnualYieldPercentage,
                referenceDate
        );

        int monthsDifference = 0;
        if (!baselineProjection.isUnreachable() && !simulatedProjection.isUnreachable()) {
            monthsDifference = baselineProjection.getTotalMonths() - simulatedProjection.getTotalMonths();
        }

        return GoalSimulationResult.builder()
                .baselineProjection(baselineProjection)
                .simulatedProjection(simulatedProjection)
                .monthsDifference(monthsDifference)
                .build();
    }

    public GoalHealthAssessment evaluateHealth(
            GoalType goalType,
            BigDecimal rawTargetValue,
            int estimatedYears,
            BigDecimal currentEquity,
            BigDecimal monthlyContribution,
            BigDecimal annualYieldPercentage,
            OffsetDateTime referenceDate
    ) {
        BigDecimal pv = (currentEquity != null && currentEquity.compareTo(BigDecimal.ZERO) > 0)
                ? currentEquity
                : BigDecimal.ZERO;
        BigDecimal pmt = (monthlyContribution != null && monthlyContribution.compareTo(BigDecimal.ZERO) > 0)
                ? monthlyContribution
                : BigDecimal.ZERO;
        BigDecimal annualYieldPct = annualYieldPercentage != null ? annualYieldPercentage : BigDecimal.ZERO;

        GoalProjection projection = calculate(
                goalType,
                rawTargetValue,
                pv,
                pmt,
                annualYieldPct,
                referenceDate
        );

        int estimatedTargetMonths = Math.max(1, estimatedYears) * 12;

        // Case 1: Achieved
        if (projection.isAchieved()) {
            BigDecimal zeroScaled = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return GoalHealthAssessment.builder()
                    .status(GoalHealthStatus.ACHIEVED)
                    .isOnTrack(true)
                    .estimatedTargetMonths(estimatedTargetMonths)
                    .projectedTotalMonths(0)
                    .monthsDelay(0)
                    .additionalMonthlyContributionNeeded(zeroScaled)
                    .requiredMonthlyContribution(zeroScaled)
                    .requiredAnnualYieldPercentage(zeroScaled)
                    .actionSuggestions(List.of("Parabéns! Sua meta financeira já foi atingida."))
                    .build();
        }

        // Case 2: On Track
        if (!projection.isUnreachable() && projection.getTotalMonths() <= estimatedTargetMonths) {
            return GoalHealthAssessment.builder()
                    .status(GoalHealthStatus.ON_TRACK)
                    .isOnTrack(true)
                    .estimatedTargetMonths(estimatedTargetMonths)
                    .projectedTotalMonths(projection.getTotalMonths())
                    .monthsDelay(0)
                    .additionalMonthlyContributionNeeded(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .requiredMonthlyContribution(pmt.setScale(2, RoundingMode.HALF_UP))
                    .requiredAnnualYieldPercentage(annualYieldPct.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                    .actionSuggestions(List.of("Mantenha a consistência dos seus aportes mensais para seguir no trilho."))
                    .build();
        }

        // Case 3: Off Track
        int monthsDelay = projection.isUnreachable()
                ? -1
                : (projection.getTotalMonths() - estimatedTargetMonths);

        double fvDouble = projection.getTargetValue().doubleValue();
        double pvDouble = pv.doubleValue();
        double pmtDouble = pmt.doubleValue();
        double r = annualYieldPct.doubleValue() / (12.0 * 100.0);

        double pmtReqDouble;
        if (r > 0) {
            double compoundFactor = Math.pow(1.0 + r, estimatedTargetMonths);
            if (!Double.isFinite(compoundFactor) || (pvDouble > 0 && pvDouble * compoundFactor >= fvDouble)) {
                pmtReqDouble = 0.0;
            } else {
                double pvTerm = pvDouble > 0 ? pvDouble * compoundFactor : 0.0;
                pmtReqDouble = ((fvDouble - pvTerm) * r) / (compoundFactor - 1.0);
            }
        } else {
            pmtReqDouble = (fvDouble - pvDouble) / estimatedTargetMonths;
        }
        if (!Double.isFinite(pmtReqDouble) || pmtReqDouble < 0.0) {
            pmtReqDouble = 0.0;
        }

        double rawDelta = (pmtReqDouble - pmtDouble) - 1e-6;
        double deltaPmtDouble = Double.isFinite(rawDelta) ? Math.max(1.0, Math.ceil(rawDelta)) : 1.0;
        BigDecimal additionalMonthlyContributionNeeded = BigDecimal.valueOf(deltaPmtDouble)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal requiredMonthlyContribution = pmt.add(additionalMonthlyContributionNeeded)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal requiredAnnualYieldPercentage = calculateRequiredAnnualYieldPercentage(
                fvDouble,
                pvDouble,
                pmtDouble,
                estimatedTargetMonths
        );

        DecimalFormatSymbols ptBrSymbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"));
        DecimalFormat numberFormatter = new DecimalFormat("#,##0.00", ptBrSymbols);

        String suggestion1 = String.format(
                "Aumente seu aporte em R$ %s para voltar ao trilho",
                numberFormatter.format(additionalMonthlyContributionNeeded)
        );
        String suggestion2 = String.format(
                "Revise seu yield alvo para %s%% a.a. para atingir a meta no prazo estimado",
                numberFormatter.format(requiredAnnualYieldPercentage)
        );

        return GoalHealthAssessment.builder()
                .status(GoalHealthStatus.OFF_TRACK)
                .isOnTrack(false)
                .estimatedTargetMonths(estimatedTargetMonths)
                .projectedTotalMonths(projection.getTotalMonths())
                .monthsDelay(monthsDelay)
                .additionalMonthlyContributionNeeded(additionalMonthlyContributionNeeded)
                .requiredMonthlyContribution(requiredMonthlyContribution)
                .requiredAnnualYieldPercentage(requiredAnnualYieldPercentage)
                .actionSuggestions(List.of(suggestion1, suggestion2))
                .build();
    }

    private BigDecimal calculateRequiredAnnualYieldPercentage(
            double fvDouble,
            double pvDouble,
            double pmtDouble,
            int estimatedTargetMonths
    ) {
        if (pvDouble + pmtDouble * estimatedTargetMonths >= fvDouble) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (pvDouble <= 0.0 && pmtDouble <= 0.0) {
            return BigDecimal.valueOf(12.00).setScale(2, RoundingMode.HALF_UP);
        }

        double low = 0.0;
        double high = 200.0;
        for (int i = 0; i < 60; i++) {
            double mid = (low + high) / 2.0;
            double rMid = mid / 1200.0;
            double accumulated;
            if (rMid > 0) {
                double factor = Math.pow(1.0 + rMid, estimatedTargetMonths);
                if (!Double.isFinite(factor)) {
                    accumulated = (pvDouble > 0.0 || pmtDouble > 0.0) ? Double.POSITIVE_INFINITY : 0.0;
                } else {
                    double pvTerm = pvDouble > 0.0 ? pvDouble * factor : 0.0;
                    accumulated = pvTerm + pmtDouble * ((factor - 1.0) / rMid);
                }
            } else {
                accumulated = pvDouble + pmtDouble * estimatedTargetMonths;
            }
            if (accumulated >= fvDouble) {
                high = mid;
            } else {
                low = mid;
            }
        }

        return BigDecimal.valueOf(high).setScale(2, RoundingMode.CEILING);
    }

    private GoalProjection buildUnreachable(
            BigDecimal effectiveTargetValue,
            BigDecimal pv,
            BigDecimal pmt,
            BigDecimal annualYieldPct,
            BigDecimal progressPercentage
    ) {
        return GoalProjection.builder()
                .targetValue(effectiveTargetValue)
                .currentEquity(pv)
                .monthlyContribution(pmt)
                .annualYieldPercentage(annualYieldPct)
                .totalMonths(-1)
                .years(-1)
                .months(-1)
                .targetDate(null)
                .progressPercentage(progressPercentage)
                .isAchieved(false)
                .isUnreachable(true)
                .build();
    }
}
