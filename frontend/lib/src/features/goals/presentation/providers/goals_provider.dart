import 'package:flutter/foundation.dart';
import '../../domain/entities/financial_goal.dart';
import '../../domain/entities/goal_projection.dart';
import '../../domain/repositories/goals_repository.dart';
import '../../domain/services/time_to_goal_calculator.dart';

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

  GoalProjection? _projection;
  GoalProjection? get projection => _projection;

  Future<void> loadGoal() async {
    _status = GoalsStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      _currentGoal = await repository.fetchGoal();
      if (_currentGoal != null) {
        await loadProjection();
      } else {
        _projection = null;
      }
      _status = GoalsStatus.success;
    } catch (e) {
      _status = GoalsStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  Future<void> loadProjection({double? currentEquity, double? grossYieldPercentage}) async {
    try {
      final remoteProjection = await repository.fetchProjection();
      if (remoteProjection != null) {
        _projection = remoteProjection;
      } else if (_currentGoal != null) {
        _projection = TimeToGoalCalculator.calculate(
          goalType: _currentGoal!.goalType,
          targetValue: _currentGoal!.targetValue,
          currentEquity: currentEquity ?? _projection?.currentEquity ?? 0.0,
          monthlyContribution: _currentGoal!.monthlyContribution,
          annualYieldPercentage: grossYieldPercentage ?? _projection?.annualYieldPercentage ?? 0.0,
        );
      } else {
        _projection = null;
      }
      notifyListeners();
    } catch (_) {
      if (_currentGoal != null) {
        _projection = TimeToGoalCalculator.calculate(
          goalType: _currentGoal!.goalType,
          targetValue: _currentGoal!.targetValue,
          currentEquity: currentEquity ?? _projection?.currentEquity ?? 0.0,
          monthlyContribution: _currentGoal!.monthlyContribution,
          annualYieldPercentage: grossYieldPercentage ?? _projection?.annualYieldPercentage ?? 0.0,
        );
        notifyListeners();
      }
    }
  }

  void recalculateLocalProjection({
    required double currentEquity,
    required double grossYieldPercentage,
  }) {
    if (_currentGoal == null) {
      _projection = null;
      notifyListeners();
      return;
    }
    _projection = TimeToGoalCalculator.calculate(
      goalType: _currentGoal!.goalType,
      targetValue: _currentGoal!.targetValue,
      currentEquity: currentEquity,
      monthlyContribution: _currentGoal!.monthlyContribution,
      annualYieldPercentage: grossYieldPercentage,
    );
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
      await loadProjection();
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
