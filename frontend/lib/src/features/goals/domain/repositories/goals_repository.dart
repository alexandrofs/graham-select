import '../entities/financial_goal.dart';

abstract class GoalsRepository {
  Future<FinancialGoal?> fetchGoal();
  Future<FinancialGoal> createGoal(FinancialGoal goal);
  Future<FinancialGoal> updateGoal(String id, FinancialGoal goal);
}
