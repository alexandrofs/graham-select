import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/goals/domain/entities/financial_goal.dart';
import 'package:frontend/src/features/goals/presentation/widgets/goal_health_checklist_card.dart';

void main() {
  group('GoalHealthChecklistCard Widget Tests', () {
    testWidgets('should render offTrack state with Warning #EF4444 badge and action suggestions', (tester) async {
      // Target: 120,000 in 5 years (60 months) at 0% yield.
      // Current equity: 0, monthlyContribution: 1,800 -> required is 2,000 -> delta = R$ 200
      const goal = FinancialGoal(
        id: 'goal-1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 120000.0,
        monthlyContribution: 1800.0,
        estimatedYears: 5,
      );

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: GoalHealthChecklistCard(
              goal: goal,
              currentEquity: 0.0,
              annualYieldPercentage: 0.0,
            ),
          ),
        ),
      );

      expect(find.text('Checklist de Saúde da Meta'), findsOneWidget);
      expect(find.byKey(const Key('goal_health_status_badge')), findsOneWidget);
      expect(find.text('Atenção: Fora do Trilho'), findsOneWidget);
      expect(find.byKey(const Key('goal_health_warning_banner')), findsOneWidget);

      final badgeText = tester.widget<Text>(find.text('Atenção: Fora do Trilho'));
      expect(badgeText.style?.color, const Color(0xFFEF4444));

      expect(find.text('Aumente seu aporte em R\$ 200,00 para voltar ao trilho'), findsOneWidget);
      expect(find.text('Aumente seu aporte em R\$ 200 para voltar ao trilho'), findsOneWidget);
      expect(find.textContaining('Revise seu yield alvo'), findsWidgets);
    });

    testWidgets('should render onTrack state with Emerald #10B981 badge and no warning banner', (tester) async {
      const goal = FinancialGoal(
        id: 'goal-2',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 100000.0,
        monthlyContribution: 2500.0,
        estimatedYears: 10,
      );

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: GoalHealthChecklistCard(
              goal: goal,
              currentEquity: 20000.0,
              annualYieldPercentage: 10.0,
            ),
          ),
        ),
      );

      expect(find.byKey(const Key('goal_health_status_badge')), findsOneWidget);
      expect(find.text('No Trilho'), findsOneWidget);
      expect(find.byKey(const Key('goal_health_warning_banner')), findsNothing);
      expect(
        find.text('Mantenha a consistência dos seus aportes mensais para seguir no trilho.'),
        findsOneWidget,
      );

      final badgeText = tester.widget<Text>(find.text('No Trilho'));
      expect(badgeText.style?.color, const Color(0xFF10B981));
    });

    testWidgets('should render achieved state when currentEquity >= targetValue', (tester) async {
      const goal = FinancialGoal(
        id: 'goal-3',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 100000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 10,
      );

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: GoalHealthChecklistCard(
              goal: goal,
              currentEquity: 120000.0,
              annualYieldPercentage: 10.0,
            ),
          ),
        ),
      );

      expect(find.byKey(const Key('goal_health_status_badge')), findsOneWidget);
      expect(find.text('Meta Concluída'), findsOneWidget);
      expect(find.byKey(const Key('goal_health_warning_banner')), findsNothing);
      expect(find.text('Parabéns! Sua meta financeira já foi atingida.'), findsOneWidget);
    });
  });
}
