import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:frontend/src/features/goals/domain/entities/financial_goal.dart';
import 'package:frontend/src/features/goals/domain/entities/goal_projection.dart';
import 'package:frontend/src/features/goals/presentation/widgets/motivational_clock_card.dart';

void main() {
  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
    MotivationalClockCard.isTesting = true;
  });

  tearDownAll(() {
    MotivationalClockCard.isTesting = false;
  });

  const sampleGoal = FinancialGoal(
    id: 'goal-1',
    goalType: 'PATRIMONY_TARGET',
    targetValue: 1000000.0,
    monthlyContribution: 2000.0,
    estimatedYears: 15,
  );

  Widget wrapWidget(Widget child) {
    return MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: child,
        ),
      ),
    );
  }

  group('MotivationalClockCard Widget Tests', () {
    testWidgets('should render normal state with years, months, forecast date and progress bar', (tester) async {
      final projection = GoalProjection(
        targetValue: 1000000.0,
        currentEquity: 150000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 10.5,
        totalMonths: 142,
        years: 11,
        months: 10,
        targetDate: DateTime.utc(2038, 10, 15),
        progressPercentage: 15.0,
        isAchieved: false,
        isUnreachable: false,
      );

      await tester.pumpWidget(
        wrapWidget(
          MotivationalClockCard(
            goal: sampleGoal,
            projection: projection,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Tempo Restante para Independência'), findsOneWidget);
      expect(find.text('11 anos e 10 meses'), findsOneWidget);
      expect(find.text('15,0% alcançado'), findsOneWidget);
      expect(find.textContaining('Previsão: Outubro de 2038'), findsOneWidget);
      expect(find.text('Em Progresso'), findsOneWidget);
    });

    testWidgets('should render achieved state when isAchieved is true', (tester) async {
      final projection = GoalProjection(
        targetValue: 500000.0,
        currentEquity: 550000.0,
        monthlyContribution: 2000.0,
        annualYieldPercentage: 9.0,
        totalMonths: 0,
        years: 0,
        months: 0,
        targetDate: DateTime.utc(2026, 6, 15),
        progressPercentage: 100.0,
        isAchieved: true,
        isUnreachable: false,
      );

      await tester.pumpWidget(
        wrapWidget(
          MotivationalClockCard(
            goal: sampleGoal,
            projection: projection,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('0 anos e 0 meses'), findsOneWidget);
      expect(find.text('🎉 Parabéns! Você atingiu sua independência financeira!'), findsOneWidget);
      expect(find.text('100,0% alcançado'), findsOneWidget);
      expect(find.text('Meta Atingida 🎉'), findsOneWidget);
    });

    testWidgets('should render unreachable state when isUnreachable is true', (tester) async {
      const projection = GoalProjection(
        targetValue: 500000.0,
        currentEquity: 100000.0,
        monthlyContribution: 0.0,
        annualYieldPercentage: 0.0,
        totalMonths: -1,
        years: -1,
        months: -1,
        targetDate: null,
        progressPercentage: 20.0,
        isAchieved: false,
        isUnreachable: true,
      );

      await tester.pumpWidget(
        wrapWidget(
          const MotivationalClockCard(
            goal: sampleGoal,
            projection: projection,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.text('Defina um aporte mensal ou adicione ativos com rendimento para calcular a projeção.'),
        findsOneWidget,
      );
      expect(find.text('Ação Necessária'), findsOneWidget);
    });

    testWidgets('should render empty state with CTA button when goal is null', (tester) async {
      bool ctaClicked = false;

      await tester.pumpWidget(
        wrapWidget(
          MotivationalClockCard(
            goal: null,
            projection: null,
            onConfigureGoal: () {
              ctaClicked = true;
            },
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.text('Defina sua Meta Financeira para desbloquear o Relógio Time-to-Goal'),
        findsOneWidget,
      );
      expect(find.text('Configurar Minha Meta'), findsOneWidget);

      await tester.tap(find.text('Configurar Minha Meta'));
      await tester.pumpAndSettle();

      expect(ctaClicked, isTrue);
    });
  });
}
