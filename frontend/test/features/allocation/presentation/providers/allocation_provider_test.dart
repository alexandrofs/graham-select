import 'dart:async';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/allocation/domain/entities/allocation_goal.dart';
import 'package:frontend/src/features/allocation/domain/repositories/allocation_repository.dart';
import 'package:frontend/src/features/allocation/presentation/providers/allocation_provider.dart';

class MockAllocationRepository implements AllocationRepository {
  List<AllocationGoal> goals = [];
  final Map<String?, List<AllocationGoal>> scopedGoals = {};
  Future<List<AllocationGoal>> Function(String? tagId)? getGoalsHandler;
  String? lastSavedTagId;
  bool shouldFail = false;
  bool shouldPremiumFail = false;

  @override
  Future<List<AllocationGoal>> getGoals({String? tagId}) async {
    if (getGoalsHandler != null) {
      return getGoalsHandler!(tagId);
    }
    if (shouldPremiumFail) {
      throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade do seu plano.');
    }
    if (shouldFail) {
      throw Exception('Fetch error');
    }
    return scopedGoals[tagId] ?? goals;
  }

  @override
  Future<void> saveGoals(List<AllocationGoal> goals, {String? tagId}) async {
    if (shouldFail) {
      throw Exception('Save error');
    }
    lastSavedTagId = tagId;
    this.goals = goals;
    scopedGoals[tagId] = List.from(goals);
  }
}

void main() {
  late MockAllocationRepository mockRepo;
  late AllocationProvider provider;

  setUp(() {
    mockRepo = MockAllocationRepository();
    provider = AllocationProvider(mockRepo);
  });

  test('should initialize with initial status and empty goals', () {
    expect(provider.status, AllocationStatus.initial);
    expect(provider.goals, isEmpty);
    expect(provider.errorMessage, isNull);
  });

  test('should load goals successfully and set status to success', () async {
    final expectedGoals = [
      const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'ACOES', targetPercentage: 50.0),
      const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'FIIS', targetPercentage: 50.0),
    ];
    mockRepo.goals = expectedGoals;

    final future = provider.loadGoals();
    expect(provider.status, AllocationStatus.loading);

    await future;
    expect(provider.status, AllocationStatus.success);
    expect(provider.goals, expectedGoals);
    expect(provider.classGoalSum, 100.0);
  });

  test('should handle error when loading goals fails', () async {
    mockRepo.shouldFail = true;

    await provider.loadGoals();
    expect(provider.status, AllocationStatus.error);
    expect(provider.errorMessage, 'Fetch error');
  });

  test('should save goals successfully and set status to success', () async {
    final newGoals = [
      const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'ACOES', targetPercentage: 100.0),
    ];

    await provider.saveGoals(newGoals);
    expect(provider.status, AllocationStatus.success);
    expect(mockRepo.goals, newGoals);
  });

  test('should handle error when saving goals fails', () async {
    mockRepo.shouldFail = true;
    final newGoals = [
      const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'ACOES', targetPercentage: 100.0),
    ];

    await provider.saveGoals(newGoals);
    expect(provider.status, AllocationStatus.error);
    expect(provider.errorMessage, 'Save error');
  });

  test('should isolate goals per tagId and ignore stale concurrent load responses', () async {
    final slowTagA = Completer<List<AllocationGoal>>();
    mockRepo.getGoalsHandler = (tagId) {
      if (tagId == 'tag-a') return slowTagA.future;
      return Future.value([
        const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'FIIS', targetPercentage: 100.0),
      ]);
    };

    final futureA = provider.loadGoals(tagId: 'tag-a');
    final futureB = provider.loadGoals(tagId: 'tag-b');

    await futureB;
    expect(provider.activeTagId, 'tag-b');
    expect(provider.goals.single.targetKey, 'FIIS');

    slowTagA.complete([
      const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'ACOES', targetPercentage: 100.0),
    ]);
    await futureA;

    expect(provider.activeTagId, 'tag-b');
    expect(provider.goals.single.targetKey, 'FIIS');

    await provider.saveGoals([
      const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'RENDA_FIXA', targetPercentage: 100.0),
    ], tagId: 'tag-b');
    expect(mockRepo.lastSavedTagId, 'tag-b');
  });
}
