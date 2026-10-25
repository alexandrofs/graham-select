import 'dart:math' as math;
import 'package:intl/intl.dart';
import '../entities/goal_health_assessment.dart';
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

  static GoalHealthAssessment evaluateHealth({
    String goalType = 'PATRIMONY_TARGET',
    required double targetValue,
    required int estimatedYears,
    required double currentEquity,
    required double monthlyContribution,
    required double annualYieldPercentage,
    DateTime? referenceDate,
  }) {
    final int safeEstimatedYears = math.max(1, estimatedYears);
    final int estimatedTargetMonths = safeEstimatedYears * 12;

    if (!targetValue.isFinite ||
        !currentEquity.isFinite ||
        !monthlyContribution.isFinite ||
        !annualYieldPercentage.isFinite) {
      return GoalHealthAssessment(
        status: GoalHealthStatus.offTrack,
        isOnTrack: false,
        estimatedTargetMonths: estimatedTargetMonths,
        projectedTotalMonths: -1,
        monthsDelay: -1,
        additionalMonthlyContributionNeeded: 1.0,
        requiredMonthlyContribution: 1.0,
        requiredAnnualYieldPercentage: 12.0,
        checklistItems: const [
          GoalHealthChecklistItem(
            title: 'Prazo Estimado',
            description: 'Não foi possível projetar o prazo com os dados atuais.',
            isHealthy: false,
          ),
          GoalHealthChecklistItem(
            title: 'Ritmo de Aportes Mensais',
            description: 'Aumente seu aporte em R\$ 1 para voltar ao trilho',
            isHealthy: false,
          ),
          GoalHealthChecklistItem(
            title: 'Rentabilidade / Yield Alvo',
            description: 'Revise seu yield alvo para 12,00% a.a.',
            isHealthy: false,
          ),
        ],
        actionSuggestions: const [
          'Aumente seu aporte em R\$ 1,00 para voltar ao trilho',
          'Revise seu yield alvo para 12,00% a.a. para atingir a meta no prazo estimado',
        ],
      );
    }

    final double pv = currentEquity < 0 ? 0.0 : currentEquity;
    final double pmt = monthlyContribution < 0 ? 0.0 : monthlyContribution;
    final double annualYieldPct = annualYieldPercentage;

    final GoalProjection projection = calculate(
      goalType: goalType,
      targetValue: targetValue,
      currentEquity: pv,
      monthlyContribution: pmt,
      annualYieldPercentage: annualYieldPct,
      referenceDate: referenceDate,
    );

    final NumberFormat decimalFormat = NumberFormat('#,##0.00', 'pt_BR');
    final NumberFormat intFormat = NumberFormat('#,##0', 'pt_BR');

    if (projection.isAchieved) {
      return GoalHealthAssessment(
        status: GoalHealthStatus.achieved,
        isOnTrack: true,
        estimatedTargetMonths: estimatedTargetMonths,
        projectedTotalMonths: 0,
        monthsDelay: 0,
        additionalMonthlyContributionNeeded: 0.0,
        requiredMonthlyContribution: 0.0,
        requiredAnnualYieldPercentage: 0.0,
        checklistItems: [
          GoalHealthChecklistItem(
            title: 'Prazo Estimado',
            description: 'Meta concluída antes do prazo estimado de $safeEstimatedYears anos.',
            isHealthy: true,
          ),
          const GoalHealthChecklistItem(
            title: 'Ritmo de Aportes Mensais',
            description: 'Patrimônio atual já atingiu ou superou o objetivo definido.',
            isHealthy: true,
          ),
          const GoalHealthChecklistItem(
            title: 'Rentabilidade / Yield Alvo',
            description: 'Rentabilidade acumulada suficiente para sustentação da meta.',
            isHealthy: true,
          ),
        ],
        actionSuggestions: const [
          'Parabéns! Sua meta financeira já foi atingida.',
        ],
      );
    }

    if (!projection.isUnreachable && projection.totalMonths <= estimatedTargetMonths) {
      final double safeYield = math.max(0.0, (annualYieldPct * 100.0).round() / 100.0);
      final double safePmt = (pmt * 100.0).round() / 100.0;
      return GoalHealthAssessment(
        status: GoalHealthStatus.onTrack,
        isOnTrack: true,
        estimatedTargetMonths: estimatedTargetMonths,
        projectedTotalMonths: projection.totalMonths,
        monthsDelay: 0,
        additionalMonthlyContributionNeeded: 0.0,
        requiredMonthlyContribution: safePmt,
        requiredAnnualYieldPercentage: safeYield,
        checklistItems: [
          GoalHealthChecklistItem(
            title: 'Prazo Estimado',
            description:
                'Projeção de ${projection.years} anos e ${projection.months} meses (dentro do prazo de $safeEstimatedYears anos).',
            isHealthy: true,
          ),
          GoalHealthChecklistItem(
            title: 'Ritmo de Aportes Mensais',
            description:
                'Aporte mensal atual de R\$ ${decimalFormat.format(safePmt)} está adequado ao prazo.',
            isHealthy: true,
          ),
          GoalHealthChecklistItem(
            title: 'Rentabilidade / Yield Alvo',
            description:
                'Rentabilidade atual de ${decimalFormat.format(safeYield)}% a.a. sustenta o objetivo no prazo.',
            isHealthy: true,
          ),
        ],
        actionSuggestions: const [
          'Mantenha a consistência dos seus aportes mensais para seguir no trilho.',
        ],
      );
    }

    final int monthsDelay = projection.isUnreachable
        ? -1
        : (projection.totalMonths - estimatedTargetMonths);

    final double fv = projection.targetValue;
    final double r = annualYieldPct / (12.0 * 100.0);

    double pmtReq;
    if (r > 0) {
      final double compoundFactor = math.pow(1.0 + r, estimatedTargetMonths).toDouble();
      if (!compoundFactor.isFinite || (pv > 0 && pv * compoundFactor >= fv)) {
        pmtReq = 0.0;
      } else {
        final double pvTerm = pv > 0 ? pv * compoundFactor : 0.0;
        pmtReq = ((fv - pvTerm) * r) / (compoundFactor - 1.0);
      }
    } else {
      pmtReq = (fv - pv) / estimatedTargetMonths;
    }
    if (!pmtReq.isFinite || pmtReq < 0) {
      pmtReq = 0.0;
    }

    final double rawDelta = (pmtReq - pmt) - 1e-6;
    final double additionalNeeded = rawDelta.isFinite
        ? math.max(1.0, rawDelta.ceilToDouble())
        : 1.0;
    final double requiredMonthlyContribution =
        ((pmt + additionalNeeded) * 100.0).round() / 100.0;

    final double requiredAnnualYield = _calculateRequiredAnnualYield(
      fv: fv,
      pv: pv,
      pmt: pmt,
      estimatedTargetMonths: estimatedTargetMonths,
    );

    final String formattedDeltaDecimal = decimalFormat.format(additionalNeeded);
    final String formattedDeltaInt = intFormat.format(additionalNeeded.round());
    final String formattedYield = decimalFormat.format(requiredAnnualYield);

    final String suggestion1 =
        'Aumente seu aporte em R\$ $formattedDeltaDecimal para voltar ao trilho';
    final String suggestion2 =
        'Revise seu yield alvo para $formattedYield% a.a. para atingir a meta no prazo estimado';

    final String plazoDesc = projection.isUnreachable
        ? 'No ritmo atual, a meta não será atingida dentro do prazo estimado de $safeEstimatedYears anos.'
        : 'Projeção atual de ${projection.years} anos e ${projection.months} meses ultrapassa o prazo de $safeEstimatedYears anos em $monthsDelay meses.';

    return GoalHealthAssessment(
      status: GoalHealthStatus.offTrack,
      isOnTrack: false,
      estimatedTargetMonths: estimatedTargetMonths,
      projectedTotalMonths: projection.totalMonths,
      monthsDelay: monthsDelay,
      additionalMonthlyContributionNeeded: additionalNeeded,
      requiredMonthlyContribution: requiredMonthlyContribution,
      requiredAnnualYieldPercentage: requiredAnnualYield,
      checklistItems: [
        GoalHealthChecklistItem(
          title: 'Prazo Estimado',
          description: plazoDesc,
          isHealthy: false,
        ),
        GoalHealthChecklistItem(
          title: 'Ritmo de Aportes Mensais',
          description: 'Aumente seu aporte em R\$ $formattedDeltaInt para voltar ao trilho',
          isHealthy: false,
        ),
        GoalHealthChecklistItem(
          title: 'Rentabilidade / Yield Alvo',
          description:
              'Revise seu yield alvo para $formattedYield% a.a. mantendo os aportes atuais.',
          isHealthy: false,
        ),
      ],
      actionSuggestions: [suggestion1, suggestion2],
    );
  }

  static double _calculateRequiredAnnualYield({
    required double fv,
    required double pv,
    required double pmt,
    required int estimatedTargetMonths,
  }) {
    if (pv + pmt * estimatedTargetMonths >= fv) {
      return 0.0;
    }
    if (pv <= 0.0 && pmt <= 0.0) {
      return 12.0;
    }

    double low = 0.0;
    double high = 200.0;
    for (int i = 0; i < 60; i++) {
      final double mid = (low + high) / 2.0;
      final double rMid = mid / 1200.0;
      double accumulated;
      if (rMid > 0) {
        final double factor = math.pow(1.0 + rMid, estimatedTargetMonths).toDouble();
        if (!factor.isFinite) {
          accumulated = (pv > 0.0 || pmt > 0.0) ? double.infinity : 0.0;
        } else {
          final double pvTerm = pv > 0.0 ? pv * factor : 0.0;
          accumulated = pvTerm + pmt * ((factor - 1.0) / rMid);
        }
      } else {
        accumulated = pv + pmt * estimatedTargetMonths;
      }
      if (accumulated >= fv) {
        high = mid;
      } else {
        low = mid;
      }
    }

    return (high * 100.0).ceil() / 100.0;
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
