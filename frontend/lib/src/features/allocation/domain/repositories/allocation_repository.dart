import '../entities/allocation_goal.dart';

abstract class AllocationRepository {
  Future<List<AllocationGoal>> getGoals();
  Future<void> saveGoals(List<AllocationGoal> goals);
}
