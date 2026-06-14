import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:frontend/src/features/goals/domain/entities/financial_goal.dart';
import 'package:frontend/src/features/goals/presentation/widgets/goal_summary_card.dart';

void main() {
  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
  });

  Widget createWidgetUnderTest({
    required FinancialGoal goal,
    required double currentPatrimony,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: GoalSummaryCard(
          goal: goal,
          currentPatrimony: currentPatrimony,
        ),
      ),
    );
  }

  group('GoalSummaryCard Widget Tests', () {
    testWidgets('should display target, contribution, and estimated years correctly', (tester) async {
      // Arrange
      const goal = FinancialGoal(
        id: '1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 15,
      );

      // Act
      await tester.pumpWidget(createWidgetUnderTest(
        goal: goal,
        currentPatrimony: 100000.0,
      ));

      // Assert
      expect(find.text('Meta de Patrimônio Alvo'), findsOneWidget);
      expect(find.text('R\$ 500.000,00'), findsOneWidget); // pt_BR format with non-breaking space
      expect(find.text('R\$ 2.000,00'), findsOneWidget);
      expect(find.text('15 anos'), findsOneWidget);
    });

    testWidgets('should show motivational message with remaining value when goal is not achieved', (tester) async {
      // Arrange
      const goal = FinancialGoal(
        id: '1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 15,
      );

      // Act
      await tester.pumpWidget(createWidgetUnderTest(
        goal: goal,
        currentPatrimony: 150000.0,
      ));

      // Assert
      // remaining = 500,000 - 150,000 = 350,000
      expect(find.textContaining('Você precisa de R\$ 350.000,00 para atingir sua meta.'), findsOneWidget);
    });

    testWidgets('should show congratulatory message when goal is achieved', (tester) async {
      // Arrange
      const goal = FinancialGoal(
        id: '1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 15,
      );

      // Act
      await tester.pumpWidget(createWidgetUnderTest(
        goal: goal,
        currentPatrimony: 600000.0,
      ));

      // Assert
      expect(find.textContaining('🎉 Parabéns! Você já atingiu sua meta de patrimônio!'), findsOneWidget);
    });

    testWidgets('should show passive income text when goal type is MONTHLY_INCOME_TARGET', (tester) async {
      // Arrange
      const goal = FinancialGoal(
        id: '1',
        goalType: 'MONTHLY_INCOME_TARGET',
        targetValue: 5000.0,
        monthlyContribution: 1000.0,
        estimatedYears: 10,
      );

      // Act
      await tester.pumpWidget(createWidgetUnderTest(
        goal: goal,
        currentPatrimony: 2000.0,
      ));

      // Assert
      expect(find.text('Meta de Renda Mensal Alvo'), findsOneWidget);
      expect(find.text('Meta: gerar R\$ 5.000,00/mês de renda passiva.'), findsOneWidget);
    });
  });
}
