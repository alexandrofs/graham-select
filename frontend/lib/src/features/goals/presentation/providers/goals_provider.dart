import 'package:flutter/foundation.dart';
import '../../domain/entities/financial_goal.dart';
import '../../domain/repositories/goals_repository.dart';

enum GoalsStatus { initial, loading, success, error }

class GoalsProvider extends ChangeNotifier {
  final GoalsRepository repository;

  GoalsProvider({required this.repository});

  GoalsStatus _status = GoalsStatus.initial;
  GoalsStatus get status => _status;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  FinancialGoal? _currentGoal;
  FinancialGoal? get currentGoal => _currentGoal;

  Future<void> loadGoal() async {
    _status = GoalsStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      _currentGoal = await repository.fetchGoal();
      _status = GoalsStatus.success;
    } catch (e) {
      _status = GoalsStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  Future<void> saveGoal({
    required String goalType,
    required double targetValue,
    required double monthlyContribution,
    required int estimatedYears,
  }) async {
    _status = GoalsStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final goal = FinancialGoal(
        id: _currentGoal?.id,
        goalType: goalType,
        targetValue: targetValue,
        monthlyContribution: monthlyContribution,
        estimatedYears: estimatedYears,
      );

      final FinancialGoal savedGoal;
      if (_currentGoal?.id != null) {
        savedGoal = await repository.updateGoal(_currentGoal!.id!, goal);
      } else {
        savedGoal = await repository.createGoal(goal);
      }

      _currentGoal = savedGoal;
      _status = GoalsStatus.success;
    } catch (e) {
      _status = GoalsStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  void resetStatus() {
    _status = GoalsStatus.initial;
    _errorMessage = null;
    notifyListeners();
  }
}
