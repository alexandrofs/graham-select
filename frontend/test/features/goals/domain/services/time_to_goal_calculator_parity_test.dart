import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/goals/domain/services/time_to_goal_calculator.dart';

/// Paridade Java x Dart: usa os mesmos vetores do backend
/// (backend/common/src/test/resources/goals/time-to-goal-parity-vectors.json).
void main() {
  test('TimeToGoalCalculator matches shared parity vectors', () {
    final file = File('../backend/common/src/test/resources/goals/time-to-goal-parity-vectors.json');
    expect(file.existsSync(), isTrue, reason: 'vetores de paridade nao encontrados');
    final root = jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
    final ref = DateTime.utc(2026, 6, 15, 12, 0);

    for (final raw in root['vectors'] as List<dynamic>) {
      final v = raw as Map<String, dynamic>;
      final name = v['name'] as String;
      final e = v['expected'] as Map<String, dynamic>;
      final p = TimeToGoalCalculator.calculate(
        goalType: v['goalType'] as String,
        targetValue: (v['targetValue'] as num).toDouble(),
        currentEquity: (v['currentEquity'] as num).toDouble(),
        monthlyContribution: (v['monthlyContribution'] as num).toDouble(),
        annualYieldPercentage: (v['annualYieldPercentage'] as num).toDouble(),
        referenceDate: ref,
      );
      expect(p.isAchieved, e['isAchieved'], reason: '$name isAchieved');
      expect(p.isUnreachable, e['isUnreachable'], reason: '$name isUnreachable');
      expect(p.totalMonths, e['totalMonths'], reason: '$name totalMonths');
      expect(p.years, e['years'], reason: '$name years');
      expect(p.months, e['months'], reason: '$name months');
      expect(p.progressPercentage, closeTo((e['progressPercentage'] as num).toDouble(), 0.005), reason: '$name progress');
      expect(p.targetValue, closeTo((e['targetValue'] as num).toDouble(), 0.005), reason: '$name targetValue');
    }
  });
}
