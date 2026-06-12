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

  double get classGoalSum {
    return _goals
        .where((g) => g.goalType == 'ASSET_CLASS')
        .fold(0.0, (sum, g) => sum + g.targetPercentage);
  }

  Future<void> loadGoals() async {
    _status = AllocationStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      _goals = await repository.getGoals();
      _status = AllocationStatus.success;
    } catch (e) {
      _status = AllocationStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  Future<void> saveGoals(List<AllocationGoal> goalsToSave) async {
    _status = AllocationStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      await repository.saveGoals(goalsToSave);
      _goals = List.from(goalsToSave);
      _status = AllocationStatus.success;
    } catch (e) {
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
