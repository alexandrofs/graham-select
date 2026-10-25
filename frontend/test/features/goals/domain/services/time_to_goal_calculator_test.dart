import 'package:flutter_test/flutter_test.dart';
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
  });
}
