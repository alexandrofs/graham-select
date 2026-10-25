import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/goals/domain/entities/financial_goal.dart';
import 'package:frontend/src/features/goals/presentation/widgets/goal_projection_chart.dart';
import 'package:frontend/src/features/goals/presentation/widgets/what_if_simulator_card.dart';

void main() {
  const defaultGoal = FinancialGoal(
    id: 'goal-1',
    goalType: 'PATRIMONY_TARGET',
    targetValue: 1000000.0,
    monthlyContribution: 2000.0,
    estimatedYears: 15,
  );

  Widget buildTestableWidget({
    FinancialGoal goal = defaultGoal,
    double currentEquity = 150000.0,
    double baselineYieldPercentage = 10.5,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: WhatIfSimulatorCard(
            goal: goal,
            currentEquity: currentEquity,
            baselineYieldPercentage: baselineYieldPercentage,
          ),
        ),
      ),
    );
  }

  group('WhatIfSimulatorCard Widget Tests', () {
    testWidgets('should render initial state with neutral guidance message and chart (AC 1 & AC 4)', (tester) async {
      await tester.pumpWidget(buildTestableWidget(baselineYieldPercentage: 10.55));
      await tester.pumpAndSettle();

      expect(find.text('Simulador de Sensibilidade ("E se?")'), findsOneWidget);
      expect(find.text('Resetar Simulação'), findsOneWidget);
      expect(
        find.text('Ajuste os controles de aporte ou rentabilidade para simular o impacto no seu tempo até a meta.'),
        findsOneWidget,
      );
      expect(find.byKey(const Key('what_if_contribution_slider')), findsOneWidget);
      expect(find.byKey(const Key('what_if_yield_slider')), findsOneWidget);
      expect(find.byType(GoalProjectionChart), findsOneWidget);
    });

    testWidgets('should recalculate and show saved months when contribution slider increases (AC 2)', (tester) async {
      await tester.pumpWidget(buildTestableWidget());
      await tester.pumpAndSettle();

      final sliderFinder = find.byKey(const Key('what_if_contribution_slider'));
      final Slider slider = tester.widget<Slider>(sliderFinder);
      slider.onChanged!(4500.0);
      await tester.pumpAndSettle();

      expect(find.textContaining('Isso encurtaria sua jornada em '), findsOneWidget);
      expect(find.textContaining(' meses!'), findsOneWidget);
    });

    testWidgets('should recalculate and show saved months when yield slider increases (AC 2)', (tester) async {
      await tester.pumpWidget(buildTestableWidget());
      await tester.pumpAndSettle();

      final sliderFinder = find.byKey(const Key('what_if_yield_slider'));
      final Slider slider = tester.widget<Slider>(sliderFinder);
      slider.onChanged!(18.0);
      await tester.pumpAndSettle();

      expect(find.textContaining('Isso encurtaria sua jornada em '), findsOneWidget);
      expect(find.textContaining(' meses!'), findsOneWidget);
    });

    testWidgets('should reset sliders to baseline when Resetar Simulação button is tapped', (tester) async {
      await tester.pumpWidget(buildTestableWidget());
      await tester.pumpAndSettle();

      final sliderFinder = find.byKey(const Key('what_if_contribution_slider'));
      tester.widget<Slider>(sliderFinder).onChanged!(5000.0);
      await tester.pumpAndSettle();

      expect(find.textContaining('Isso encurtaria sua jornada em '), findsOneWidget);

      await tester.tap(find.text('Resetar Simulação'));
      await tester.pumpAndSettle();

      expect(
        find.text('Ajuste os controles de aporte ou rentabilidade para simular o impacto no seu tempo até a meta.'),
        findsOneWidget,
      );
    });

    testWidgets('should show increased journey months warning when contribution decreases (AC 3)', (tester) async {
      await tester.pumpWidget(buildTestableWidget());
      await tester.pumpAndSettle();

      final sliderFinder = find.byKey(const Key('what_if_contribution_slider'));
      tester.widget<Slider>(sliderFinder).onChanged!(500.0);
      await tester.pumpAndSettle();

      expect(find.textContaining('Isso aumentaria sua jornada em '), findsOneWidget);
      expect(find.textContaining(' meses.'), findsOneWidget);
    });

    testWidgets('should handle edge cases: achieved goal, baseline unreachable becoming reachable, and simulated unreachable (AC 5)', (tester) async {
      // Scenario A: Goal already achieved (PV >= FV)
      await tester.pumpWidget(
        buildTestableWidget(
          currentEquity: 1200000.0,
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.text('🎉 Sua meta já foi atingida! Use os controles para simular a evolução do seu patrimônio.'),
        findsOneWidget,
      );

      // Scenario B: Baseline unreachable (0 contribution, 0 yield), simulated becomes reachable
      const zeroContributionGoal = FinancialGoal(
        id: 'goal-zero',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        monthlyContribution: 0.0,
        estimatedYears: 10,
      );

      await tester.pumpWidget(
        buildTestableWidget(
          goal: zeroContributionGoal,
          currentEquity: 100000.0,
          baselineYieldPercentage: 0.0,
        ),
      );
      await tester.pumpAndSettle();

      // Initially both baseline and simulated are unreachable (Scenario C applies when simulated is unreachable)
      expect(
        find.text('Neste cenário simulado, a meta não seria alcançada. Aumente o aporte ou a rentabilidade esperada.'),
        findsOneWidget,
      );

      // Now increase contribution so simulated becomes reachable while baseline remains unreachable (Scenario B)
      final sliderFinder = find.byKey(const Key('what_if_contribution_slider'));
      tester.widget<Slider>(sliderFinder).onChanged!(4000.0);
      await tester.pumpAndSettle();

      expect(
        find.textContaining('Com este cenário, sua meta passa a ser alcançável em '),
        findsOneWidget,
      );
    });
  });
}
