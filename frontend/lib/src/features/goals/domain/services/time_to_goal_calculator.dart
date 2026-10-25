import 'dart:math' as math;
import '../entities/goal_projection.dart';
import '../entities/goal_simulation_result.dart';

class TimeToGoalCalculator {
  static const double defaultAnnualYieldRate = 0.06;
  static const int maxSupportedMonths = 12000;

  static GoalProjection calculate({
    String goalType = 'PATRIMONY_TARGET',
    required double targetValue,
    required double currentEquity,
    required double monthlyContribution,
    required double annualYieldPercentage,
    DateTime? referenceDate,
  }) {
    final DateTime refDate = (referenceDate ?? DateTime.now()).toUtc();
    if (!targetValue.isFinite ||
        !currentEquity.isFinite ||
        !monthlyContribution.isFinite ||
        !annualYieldPercentage.isFinite) {
      return const GoalProjection(
        targetValue: 0.0,
        currentEquity: 0.0,
        monthlyContribution: 0.0,
        annualYieldPercentage: 0.0,
        totalMonths: -1,
        years: -1,
        months: -1,
        targetDate: null,
        progressPercentage: 0.0,
        isAchieved: false,
        isUnreachable: true,
      );
    }
    final double pv = currentEquity < 0 ? 0.0 : currentEquity;
    final double pmt = monthlyContribution < 0 ? 0.0 : monthlyContribution;
    final double annualYieldPct = annualYieldPercentage;

    double effectiveTargetValue;
    if (goalType == 'MONTHLY_INCOME_TARGET') {
      final double annualYieldRate = annualYieldPct > 0
          ? (annualYieldPct / 100.0)
          : defaultAnnualYieldRate;
      effectiveTargetValue = ((targetValue * 12.0) / annualYieldRate * 100.0).round() / 100.0;
    } else {
      effectiveTargetValue = (targetValue * 100.0).round() / 100.0;
    }

    double progressPercentage;
    if (effectiveTargetValue <= 0) {
      progressPercentage = 100.0;
    } else {
      final double rawProgress = (pv / effectiveTargetValue) * 100.0;
      progressPercentage = ((rawProgress.clamp(0.0, 100.0)) * 100.0).round() / 100.0;
    }

    // Case 1: PV >= FV (Goal achieved)
    if (pv >= effectiveTargetValue) {
      return GoalProjection(
        targetValue: effectiveTargetValue,
        currentEquity: pv,
        monthlyContribution: pmt,
        annualYieldPercentage: annualYieldPct,
        totalMonths: 0,
        years: 0,
        months: 0,
        targetDate: refDate,
        progressPercentage: 100.0,
        isAchieved: true,
        isUnreachable: false,
      );
    }

    final double r = annualYieldPct / (12.0 * 100.0);
    double rawMonths;

    // Case 2: r > 0 and PV * r + PMT > 0
    if (r > 0 && (pv * r + pmt) > 0 && math.log(1.0 + r) > 0) {
      final double numerator = math.log((effectiveTargetValue * r + pmt) / (pv * r + pmt));
      final double denominator = math.log(1.0 + r);
      rawMonths = numerator / denominator;
    }
    // Case 3: r <= 0 and PMT > 0
    else if (r <= 0 && pmt > 0) {
      rawMonths = (effectiveTargetValue - pv) / pmt;
    }
    // Case 4: Unreachable
    else {
      return GoalProjection(
        targetValue: effectiveTargetValue,
        currentEquity: pv,
        monthlyContribution: pmt,
        annualYieldPercentage: annualYieldPct,
        totalMonths: -1,
        years: -1,
        months: -1,
        targetDate: null,
        progressPercentage: progressPercentage,
        isAchieved: false,
        isUnreachable: true,
      );
    }

    if (!rawMonths.isFinite || rawMonths <= 0 || rawMonths.ceil() > maxSupportedMonths) {
      return GoalProjection(
        targetValue: effectiveTargetValue,
        currentEquity: pv,
        monthlyContribution: pmt,
        annualYieldPercentage: annualYieldPct,
        totalMonths: -1,
        years: -1,
        months: -1,
        targetDate: null,
        progressPercentage: progressPercentage,
        isAchieved: false,
        isUnreachable: true,
      );
    }

    final int totalMonths = rawMonths.ceil();

    final int years = totalMonths ~/ 12;
    final int months = totalMonths % 12;
    final DateTime targetDate = _addMonthsUtc(refDate, totalMonths);

    return GoalProjection(
      targetValue: effectiveTargetValue,
      currentEquity: pv,
      monthlyContribution: pmt,
      annualYieldPercentage: annualYieldPct,
      totalMonths: totalMonths,
      years: years,
      months: months,
      targetDate: targetDate,
      progressPercentage: progressPercentage,
      isAchieved: false,
      isUnreachable: false,
    );
  }

  static GoalSimulationResult simulate({
    String goalType = 'PATRIMONY_TARGET',
    required double targetValue,
    required double currentEquity,
    required double baselineMonthlyContribution,
    required double baselineAnnualYieldPercentage,
    required double simulatedMonthlyContribution,
    required double simulatedAnnualYieldPercentage,
    DateTime? referenceDate,
  }) {
    final GoalProjection baselineProjection = calculate(
      goalType: goalType,
      targetValue: targetValue,
      currentEquity: currentEquity,
      monthlyContribution: baselineMonthlyContribution,
      annualYieldPercentage: baselineAnnualYieldPercentage,
      referenceDate: referenceDate,
    );

    final GoalProjection simulatedProjection = calculate(
      goalType: 'PATRIMONY_TARGET',
      targetValue: baselineProjection.targetValue,
      currentEquity: currentEquity,
      monthlyContribution: simulatedMonthlyContribution,
      annualYieldPercentage: simulatedAnnualYieldPercentage,
      referenceDate: referenceDate,
    );

    int monthsDifference = 0;
    if (!baselineProjection.isUnreachable && !simulatedProjection.isUnreachable) {
      monthsDifference = baselineProjection.totalMonths - simulatedProjection.totalMonths;
    }

    int horizonMonths;
    if (!baselineProjection.isUnreachable && !simulatedProjection.isUnreachable) {
      final int maxMonths = math.max(baselineProjection.totalMonths, simulatedProjection.totalMonths);
      horizonMonths = maxMonths > 0 ? maxMonths : 120;
    } else if (!baselineProjection.isUnreachable) {
      horizonMonths = baselineProjection.totalMonths > 0 ? baselineProjection.totalMonths : 120;
    } else if (!simulatedProjection.isUnreachable) {
      horizonMonths = simulatedProjection.totalMonths > 0 ? simulatedProjection.totalMonths : 120;
    } else {
      horizonMonths = 120;
    }

    final double fv = baselineProjection.targetValue;
    final double pv = currentEquity.isFinite && currentEquity > 0 ? currentEquity : 0.0;
    final double maxChartEquity = math.max(fv * 1.25, pv * 1.5);

    final List<ProjectionCurvePoint> baselineCurve = generateEvolutionCurve(
      initialEquity: pv,
      monthlyContribution: baselineMonthlyContribution,
      annualYieldPercentage: baselineAnnualYieldPercentage,
      horizonMonths: horizonMonths,
      maxCapEquity: maxChartEquity,
    );

    final List<ProjectionCurvePoint> simulatedCurve = generateEvolutionCurve(
      initialEquity: pv,
      monthlyContribution: simulatedMonthlyContribution,
      annualYieldPercentage: simulatedAnnualYieldPercentage,
      horizonMonths: horizonMonths,
      maxCapEquity: maxChartEquity,
    );

    return GoalSimulationResult(
      baselineProjection: baselineProjection,
      simulatedProjection: simulatedProjection,
      monthsDifference: monthsDifference,
      baselineCurve: baselineCurve,
      simulatedCurve: simulatedCurve,
    );
  }

  static List<ProjectionCurvePoint> generateEvolutionCurve({
    required double initialEquity,
    required double monthlyContribution,
    required double annualYieldPercentage,
    required int horizonMonths,
    required double maxCapEquity,
  }) {
    final int safeHorizon = horizonMonths > 0 ? horizonMonths : 120;
    final double pv = initialEquity.isFinite && initialEquity > 0 ? initialEquity : 0.0;
    final double pmt = monthlyContribution.isFinite && monthlyContribution > 0 ? monthlyContribution : 0.0;
    final double r = annualYieldPercentage.isFinite ? (annualYieldPercentage / (12.0 * 100.0)) : 0.0;
    final double cap = (maxCapEquity.isFinite && maxCapEquity > 0) ? maxCapEquity : 1e12;

    const int maxSteps = 20;
    final int steps = math.min(safeHorizon, maxSteps);
    final List<ProjectionCurvePoint> points = [];

    for (int i = 0; i <= steps; i++) {
      final int m = ((i * safeHorizon) / steps).round();
      if (points.isNotEmpty && points.last.monthIndex == m) {
        continue;
      }

      double equityAtMonth;
      if (r > 0) {
        final double compoundFactor = math.pow(1.0 + r, m).toDouble();
        equityAtMonth = pv * compoundFactor + pmt * ((compoundFactor - 1.0) / r);
      } else {
        equityAtMonth = pv + pmt * m;
      }

      if (!equityAtMonth.isFinite || equityAtMonth < 0) {
        equityAtMonth = cap;
      } else if (equityAtMonth > cap) {
        equityAtMonth = cap;
      }

      points.add(
        ProjectionCurvePoint(
          monthIndex: m,
          equity: (equityAtMonth * 100.0).round() / 100.0,
        ),
      );
    }

    return points;
  }

  static DateTime _addMonthsUtc(DateTime date, int monthsToAdd) {
    final int totalMonths = (date.year * 12 + (date.month - 1)) + monthsToAdd;
    final int newYear = totalMonths ~/ 12;
    final int newMonth = (totalMonths % 12) + 1;
    final int maxDayInNewMonth = DateTime.utc(newYear, newMonth + 1, 0).day;
    final int newDay = math.min(date.day, maxDayInNewMonth);
    return DateTime.utc(
      newYear,
      newMonth,
      newDay,
      date.hour,
      date.minute,
      date.second,
      date.millisecond,
      date.microsecond,
    );
  }
}
