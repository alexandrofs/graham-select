import '../../domain/entities/allocation_goal.dart';
import '../../domain/repositories/allocation_repository.dart';
import '../datasources/allocation_remote_data_source.dart';
import '../../../auth/data/auth_repository.dart';
import '../models/allocation_goal_model.dart';

class AllocationRepositoryImpl implements AllocationRepository {
  final AllocationRemoteDataSource remoteDataSource;
  final AuthRepository authRepository;

  AllocationRepositoryImpl(this.remoteDataSource, this.authRepository);

  @override
  Future<List<AllocationGoal>> getGoals() async {
    return remoteDataSource.getGoals();
  }

  @override
  Future<void> saveGoals(List<AllocationGoal> goals) async {
    final models = goals.map((g) => AllocationGoalModel(
      goalType: g.goalType,
      targetKey: g.targetKey,
      targetPercentage: g.targetPercentage,
    )).toList();
    return remoteDataSource.saveGoals(models);
  }
}
