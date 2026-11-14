import 'package:flutter/foundation.dart';
import '../../domain/entities/allocation_goal.dart';
import '../../domain/repositories/allocation_repository.dart';

enum AllocationStatus { initial, loading, success, error }

class AllocationProvider extends ChangeNotifier {
  final AllocationRepository repository;

  AllocationProvider(this.repository);

  AllocationStatus _status = AllocationStatus.initial;
  AllocationStatus get status => _status;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  List<AllocationGoal> _goals = [];
  List<AllocationGoal> get goals => _goals;

  String? _activeTagId;
  String? get activeTagId => _activeTagId;

  int _loadGeneration = 0;

  String? _normalizeTagId(String? tagId) {
    final trimmed = tagId?.trim();
    if (trimmed == null || trimmed.isEmpty) return null;
    return trimmed;
  }

  double get classGoalSum {
    return _goals
        .where((g) => g.goalType == 'ASSET_CLASS')
        .fold(0.0, (sum, g) => sum + g.targetPercentage);
  }

  Future<void> loadGoals({String? tagId}) async {
    final normalizedTagId = _normalizeTagId(tagId);
    final generation = ++_loadGeneration;
    if (_activeTagId != normalizedTagId) {
      _goals = [];
    }
    _activeTagId = normalizedTagId;
    _status = AllocationStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final loaded = await repository.getGoals(tagId: normalizedTagId);
      if (generation != _loadGeneration) return;
      _goals = loaded;
      _status = AllocationStatus.success;
    } catch (e) {
      if (generation != _loadGeneration) return;
      _status = AllocationStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  Future<void> saveGoals(List<AllocationGoal> goalsToSave, {String? tagId}) async {
    final targetTagId = _normalizeTagId(tagId);
    final generation = ++_loadGeneration;
    _activeTagId = targetTagId;
    _status = AllocationStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      await repository.saveGoals(goalsToSave, tagId: targetTagId);
      if (generation != _loadGeneration) return;
      _goals = List.from(goalsToSave);
      _status = AllocationStatus.success;
    } catch (e) {
      if (generation != _loadGeneration) return;
      _status = AllocationStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  void resetStatus() {
    _status = AllocationStatus.initial;
    _errorMessage = null;
    notifyListeners();
  }
}
