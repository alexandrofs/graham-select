import '../entities/financial_goal.dart';
import '../entities/goal_projection.dart';

abstract class GoalsRepository {
  Future<FinancialGoal?> fetchGoal();
  Future<GoalProjection?> fetchProjection();
  Future<FinancialGoal> createGoal(FinancialGoal goal);
  Future<FinancialGoal> updateGoal(String id, FinancialGoal goal);
}
