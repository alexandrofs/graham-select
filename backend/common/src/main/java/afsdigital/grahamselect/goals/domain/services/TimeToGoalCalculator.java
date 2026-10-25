package afsdigital.grahamselect.goals.domain.services;

import afsdigital.grahamselect.goals.domain.entities.GoalProjection;
import afsdigital.grahamselect.goals.domain.entities.GoalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

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
