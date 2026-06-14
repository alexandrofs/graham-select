import '../../domain/entities/financial_goal.dart';
import '../../domain/repositories/goals_repository.dart';
import '../datasources/goals_remote_datasource.dart';
import '../models/financial_goal_model.dart';

class GoalsRepositoryImpl implements GoalsRepository {
  final GoalsRemoteDatasource remoteDatasource;

  GoalsRepositoryImpl({required this.remoteDatasource});

  @override
  Future<FinancialGoal?> fetchGoal() async {
    final model = await remoteDatasource.fetchGoal();
    return model?.toEntity();
  }

  @override
  Future<FinancialGoal> createGoal(FinancialGoal goal) async {
    final model = FinancialGoalModel.fromEntity(goal);
    final savedModel = await remoteDatasource.createGoal(model.toJson());
    return savedModel.toEntity();
  }

  @override
  Future<FinancialGoal> updateGoal(String id, FinancialGoal goal) async {
    final model = FinancialGoalModel.fromEntity(goal);
    final savedModel = await remoteDatasource.updateGoal(id, model.toJson());
    return savedModel.toEntity();
  }
}
