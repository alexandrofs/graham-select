import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/goals/domain/entities/goal_health_assessment.dart';
import 'package:frontend/src/features/goals/domain/services/time_to_goal_calculator.dart';

void main() {
  final referenceDate = DateTime.utc(2026, 6, 15, 12, 0);

  group('TimeToGoalCalculator Tests', () {
    test('should calculate months, years and progress for PATRIMONY_TARGET with positive yield', () {
      final projection = TimeToGoalCalculator.calculate(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 1000000.0,
        currentEquity: 150000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 10.5,
        referenceDate: referenceDate,
      );

      expect(projection.isAchieved, isFalse);
      expect(projection.isUnreachable, isFalse);
      expect(projection.targetValue, 1000000.0);
      expect(projection.currentEquity, 150000.0);
      expect(projection.progressPercentage, 15.0);
      expect(projection.totalMonths, greaterThan(0));
      expect(projection.years, projection.totalMonths ~/ 12);
      expect(projection.months, projection.totalMonths % 12);
      expect(projection.targetDate, isNotNull);
    });

    test('should convert MONTHLY_INCOME_TARGET using annualYieldPercentage when positive', () {
      // R$ 5,000/month with 10% a.a. yield -> FV = (5000 * 12) / 0.10 = 600,000
      final projection = TimeToGoalCalculator.calculate(
        goalType: 'MONTHLY_INCOME_TARGET',
        targetValue: 5000.0,
        currentEquity: 60000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 10.0,
        referenceDate: referenceDate,
      );

      expect(projection.targetValue, 600000.0);
      expect(projection.progressPercentage, 10.0);
      expect(projection.isAchieved, isFalse);
      expect(projection.isUnreachable, isFalse);
      expect(projection.totalMonths, greaterThan(0));
    });

    test('should convert MONTHLY_INCOME_TARGET using default 6% yield when annualYieldPercentage <= 0', () {
      // R$ 3,000/month with 0% yield -> FV = (3000 * 12) / 0.06 = 600,000
      final projection = TimeToGoalCalculator.calculate(
        goalType: 'MONTHLY_INCOME_TARGET',
        targetValue: 3000.0,
        currentEquity: 300000.0,
        monthlyContribution: 5000.0,
        annualYieldPercentage: 0.0,
        referenceDate: referenceDate,
      );

      expect(projection.targetValue, 600000.0);
      expect(projection.progressPercentage, 50.0);
      expect(projection.totalMonths, 60);
      expect(projection.years, 5);
      expect(projection.months, 0);
      expect(projection.isAchieved, isFalse);
      expect(projection.isUnreachable, isFalse);
    });

    test('should return achieved state when currentEquity >= targetValue', () {
      final projection = TimeToGoalCalculator.calculate(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        currentEquity: 550000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 8.0,
        referenceDate: referenceDate,
      );

      expect(projection.isAchieved, isTrue);
      expect(projection.isUnreachable, isFalse);
      expect(projection.totalMonths, 0);
      expect(projection.years, 0);
      expect(projection.months, 0);
      expect(projection.progressPercentage, 100.0);
      expect(projection.targetDate, referenceDate);
    });

    test('should return unreachable state when yield <= 0, contribution <= 0 and equity < target', () {
      final projection = TimeToGoalCalculator.calculate(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        currentEquity: 100000.0,
        monthlyContribution: 0.0,
        annualYieldPercentage: 0.0,
        referenceDate: referenceDate,
      );

      expect(projection.isAchieved, isFalse);
      expect(projection.isUnreachable, isTrue);
      expect(projection.totalMonths, -1);
      expect(projection.years, -1);
      expect(projection.months, -1);
      expect(projection.targetDate, isNull);
      expect(projection.progressPercentage, 20.0);
    });

    test('should simulate positive, negative and zero monthsDifference and generate evolution curves', () {
      final positiveSim = TimeToGoalCalculator.simulate(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 1000000.0,
        currentEquity: 150000.0,
        baselineMonthlyContribution: 2000.0,
        baselineAnnualYieldPercentage: 10.5,
        simulatedMonthlyContribution: 3500.0,
        simulatedAnnualYieldPercentage: 12.0,
        referenceDate: referenceDate,
      );

      expect(positiveSim.monthsDifference, greaterThan(0));
      expect(
        positiveSim.monthsDifference,
        positiveSim.baselineProjection.totalMonths - positiveSim.simulatedProjection.totalMonths,
      );
      expect(positiveSim.baselineCurve, isNotEmpty);
      expect(positiveSim.simulatedCurve, isNotEmpty);
      expect(positiveSim.baselineCurve.first.monthIndex, 0);
      expect(positiveSim.baselineCurve.first.equity, 150000.0);
      expect(positiveSim.simulatedCurve.last.equity, greaterThan(positiveSim.simulatedCurve.first.equity));

      final negativeSim = TimeToGoalCalculator.simulate(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 1000000.0,
        currentEquity: 150000.0,
        baselineMonthlyContribution: 3500.0,
        baselineAnnualYieldPercentage: 12.0,
        simulatedMonthlyContribution: 2000.0,
        simulatedAnnualYieldPercentage: 10.5,
        referenceDate: referenceDate,
      );

      expect(negativeSim.monthsDifference, lessThan(0));

      final neutralSim = TimeToGoalCalculator.simulate(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 1000000.0,
        currentEquity: 150000.0,
        baselineMonthlyContribution: 2000.0,
        baselineAnnualYieldPercentage: 10.5,
        simulatedMonthlyContribution: 2000.0,
        simulatedAnnualYieldPercentage: 10.5,
        referenceDate: referenceDate,
      );

      expect(neutralSim.monthsDifference, 0);
    });

    test('should keep fixed targetValue in simulate when goalType is MONTHLY_INCOME_TARGET', () {
      final sim = TimeToGoalCalculator.simulate(
        goalType: 'MONTHLY_INCOME_TARGET',
        targetValue: 5000.0,
        currentEquity: 60000.0,
        baselineMonthlyContribution: 2000.0,
        baselineAnnualYieldPercentage: 10.0,
        simulatedMonthlyContribution: 2000.0,
        simulatedAnnualYieldPercentage: 15.0,
        referenceDate: referenceDate,
      );

      expect(sim.baselineProjection.targetValue, 600000.0);
      expect(sim.simulatedProjection.targetValue, 600000.0);
      expect(sim.monthsDifference, greaterThan(0));
    });

    test('should evaluate health as offTrack when projected months exceed estimatedYears * 12', () {
      final assessment = TimeToGoalCalculator.evaluateHealth(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 1000000.0,
        estimatedYears: 10,
        currentEquity: 150000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 10.5,
        referenceDate: referenceDate,
      );

      expect(assessment.status, GoalHealthStatus.offTrack);
      expect(assessment.isOnTrack, isFalse);
      expect(assessment.estimatedTargetMonths, 120);
      expect(assessment.projectedTotalMonths, 136);
      expect(assessment.monthsDelay, 16);
      expect(assessment.additionalMonthlyContributionNeeded, greaterThan(0));
      expect(assessment.requiredAnnualYieldPercentage, greaterThan(10.5));
      expect(assessment.checklistItems.length, 3);
      expect(assessment.checklistItems.every((item) => !item.isHealthy), isTrue);
      expect(assessment.actionSuggestions.length, 2);
      expect(
        assessment.actionSuggestions.first,
        startsWith('Aumente seu aporte em R\$ '),
      );
      expect(
        assessment.actionSuggestions.last,
        startsWith('Revise seu yield alvo para '),
      );

      // Applying requiredMonthlyContribution must bring projection <= 120 months
      final adjustedProj = TimeToGoalCalculator.calculate(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 1000000.0,
        currentEquity: 150000.0,
        monthlyContribution: assessment.requiredMonthlyContribution,
        annualYieldPercentage: 10.5,
        referenceDate: referenceDate,
      );
      expect(adjustedProj.isUnreachable, isFalse);
      expect(adjustedProj.totalMonths, lessThanOrEqualTo(120));
    });

    test('should evaluate health as onTrack when projected months <= estimatedYears * 12', () {
      final assessment = TimeToGoalCalculator.evaluateHealth(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 1000000.0,
        estimatedYears: 15,
        currentEquity: 150000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 10.5,
        referenceDate: referenceDate,
      );

      expect(assessment.status, GoalHealthStatus.onTrack);
      expect(assessment.isOnTrack, isTrue);
      expect(assessment.estimatedTargetMonths, 180);
      expect(assessment.monthsDelay, 0);
      expect(assessment.additionalMonthlyContributionNeeded, 0.0);
      expect(assessment.checklistItems.every((item) => item.isHealthy), isTrue);
      expect(
        assessment.actionSuggestions.first,
        'Mantenha a consistência dos seus aportes mensais para seguir no trilho.',
      );
    });

    test('should evaluate health as achieved when currentEquity >= targetValue', () {
      final assessment = TimeToGoalCalculator.evaluateHealth(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        estimatedYears: 10,
        currentEquity: 550000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 10.0,
        referenceDate: referenceDate,
      );

      expect(assessment.status, GoalHealthStatus.achieved);
      expect(assessment.isOnTrack, isTrue);
      expect(assessment.projectedTotalMonths, 0);
      expect(assessment.monthsDelay, 0);
      expect(assessment.checklistItems.every((item) => item.isHealthy), isTrue);
      expect(
        assessment.actionSuggestions.first,
        'Parabéns! Sua meta financeira já foi atingida.',
      );
    });

    test('should evaluate health as offTrack when goal is unreachable', () {
      final assessment = TimeToGoalCalculator.evaluateHealth(
        goalType: 'PATRIMONY_TARGET',
        targetValue: 240000.0,
        estimatedYears: 10,
        currentEquity: 0.0,
        monthlyContribution: 0.0,
        annualYieldPercentage: 0.0,
        referenceDate: referenceDate,
      );

      expect(assessment.status, GoalHealthStatus.offTrack);
      expect(assessment.isOnTrack, isFalse);
      expect(assessment.projectedTotalMonths, -1);
      expect(assessment.monthsDelay, -1);
      expect(assessment.additionalMonthlyContributionNeeded, 2000.0);
      expect(assessment.requiredMonthlyContribution, 2000.0);
      expect(assessment.requiredAnnualYieldPercentage, 12.0);
    });
  });
}
