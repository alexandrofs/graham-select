import 'dart:math' as math;
import '../entities/goal_projection.dart';

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
