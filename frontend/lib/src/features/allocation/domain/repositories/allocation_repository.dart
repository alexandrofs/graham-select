import '../entities/allocation_goal.dart';

abstract class AllocationRepository {
  Future<List<AllocationGoal>> getGoals({String? tagId});
  Future<void> saveGoals(List<AllocationGoal> goals, {String? tagId});
}
