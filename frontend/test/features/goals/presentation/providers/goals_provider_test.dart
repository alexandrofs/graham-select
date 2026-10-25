import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/goals/domain/entities/financial_goal.dart';
import 'package:frontend/src/features/goals/domain/entities/goal_projection.dart';
import 'package:frontend/src/features/goals/domain/repositories/goals_repository.dart';
import 'package:frontend/src/features/goals/presentation/providers/goals_provider.dart';

class FakeGoalsRepository implements GoalsRepository {
  FinancialGoal? goal;
  GoalProjection? projection;
  bool throwOnFetchProjection = false;

  @override
  Future<FinancialGoal?> fetchGoal() async => goal;

  @override
  Future<GoalProjection?> fetchProjection() async {
    if (throwOnFetchProjection) {
      throw Exception('Erro ao buscar projeção');
    }
    return projection;
  }

  @override
  Future<FinancialGoal> createGoal(FinancialGoal newGoal) async {
    goal = newGoal.copyWith(id: 'goal-1');
    return goal!;
  }

  @override
  Future<FinancialGoal> updateGoal(String id, FinancialGoal updatedGoal) async {
    goal = updatedGoal;
    return goal!;
  }
}

void main() {
  late FakeGoalsRepository repository;
  late GoalsProvider provider;

  setUp(() {
    repository = FakeGoalsRepository();
    provider = GoalsProvider(repository: repository);
  });

  group('GoalsProvider Projection Tests', () {
    test('should load remote projection when loadGoal finds a goal and API returns projection', () async {
      repository.goal = const FinancialGoal(
        id: 'goal-1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 15,
      );
      repository.projection = GoalProjection(
        targetValue: 500000.0,
        currentEquity: 100000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 10.0,
        totalMonths: 108,
        years: 9,
        months: 0,
        targetDate: DateTime.utc(2035, 6, 15),
        progressPercentage: 20.0,
        isAchieved: false,
        isUnreachable: false,
      );

      await provider.loadGoal();

      expect(provider.status, GoalsStatus.success);
      expect(provider.currentGoal, isNotNull);
      expect(provider.projection, isNotNull);
      expect(provider.projection!.years, 9);
      expect(provider.projection!.months, 0);
    });

    test('should fallback to local calculation when fetchProjection returns null', () async {
      repository.goal = const FinancialGoal(
        id: 'goal-1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 200000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 10,
      );
      repository.projection = null;

      await provider.loadGoal();

      expect(provider.projection, isNotNull);
      expect(provider.projection!.targetValue, 200000.0);
      expect(provider.projection!.totalMonths, 100);
    });

    test('recalculateLocalProjection should update projection synchronously', () async {
      repository.goal = const FinancialGoal(
        id: 'goal-1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 15,
      );
      await provider.loadGoal();

      provider.recalculateLocalProjection(
        currentEquity: 500000.0,
        grossYieldPercentage: 12.0,
      );

      expect(provider.projection, isNotNull);
      expect(provider.projection!.isAchieved, isTrue);
      expect(provider.projection!.progressPercentage, 100.0);
    });
  });
}
