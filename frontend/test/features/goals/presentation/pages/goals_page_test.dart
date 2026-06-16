import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import 'package:frontend/src/features/goals/domain/entities/financial_goal.dart';
import 'package:frontend/src/features/goals/domain/repositories/goals_repository.dart';
import 'package:frontend/src/features/goals/presentation/providers/goals_provider.dart';
import 'package:frontend/src/features/goals/presentation/pages/goals_page.dart';
import 'package:frontend/src/features/goals/presentation/widgets/goal_summary_card.dart';
import 'package:frontend/src/features/portfolio/domain/entities/portfolio_summary.dart';
import 'package:frontend/src/features/portfolio/domain/repositories/portfolio_repository.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/domain/entities/trade.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/portfolio/domain/entities/monthly_evolution.dart';

class ManualMockGoalsRepository implements GoalsRepository {
  FinancialGoal? goal;
  bool shouldThrowError = false;
  int createGoalCallCount = 0;
  int updateGoalCallCount = 0;

  @override
  Future<FinancialGoal?> fetchGoal() async {
    if (shouldThrowError) {
      throw Exception('Erro de rede');
    }
    return goal;
  }

  @override
  Future<FinancialGoal> createGoal(FinancialGoal newGoal) async {
    createGoalCallCount++;
    if (shouldThrowError) {
      throw Exception('Erro ao criar');
    }
    goal = newGoal.copyWith(id: 'generated-id-123');
    return goal!;
  }

  @override
  Future<FinancialGoal> updateGoal(String id, FinancialGoal updatedGoal) async {
    updateGoalCallCount++;
    if (shouldThrowError) {
      throw Exception('Erro ao atualizar');
    }
    goal = updatedGoal;
    return goal!;
  }
}

class ManualMockPortfolioRepository implements PortfolioRepository {
  PortfolioSummary summary = PortfolioSummary(
    totalEquity: 150000.0,
    grossYieldPercentage: 12.5,
    accumulatedDividends: 5000.0,
    monthlyProjection: 800.0,
  );

  @override
  Future<PortfolioSummary> getSummary() async {
    return summary;
  }

  @override
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<({List<CustodyPosition> positions, DateTime? metaPriceUpdatedAt})> getCustodyPositions() async {
    return (positions: <CustodyPosition>[], metaPriceUpdatedAt: null);
  }

  @override
  Future<PortfolioEvolution> getPortfolioEvolution() async {
    throw UnimplementedError();
  }
}

void main() {
  late ManualMockGoalsRepository mockGoalsRepository;
  late ManualMockPortfolioRepository mockPortfolioRepository;
  late GoalsProvider goalsProvider;
  late PortfolioProvider portfolioProvider;

  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
    GoalsPage.isTesting = true;
  });

  setUp(() {
    mockGoalsRepository = ManualMockGoalsRepository();
    mockPortfolioRepository = ManualMockPortfolioRepository();
    goalsProvider = GoalsProvider(repository: mockGoalsRepository);
    portfolioProvider = PortfolioProvider(mockPortfolioRepository);
  });

  Widget createWidgetUnderTest() {
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (context, state) => const GoalsPage(),
        ),
      ],
    );

    return MaterialApp.router(
      routerConfig: router,
      builder: (context, child) => MultiProvider(
        providers: [
          ChangeNotifierProvider<GoalsProvider>.value(value: goalsProvider),
          ChangeNotifierProvider<PortfolioProvider>.value(value: portfolioProvider),
        ],
        child: child,
      ),
    );
  }

  group('GoalsPage Widget Tests', () {
    testWidgets('should show success snackbar and display summary card when saved successfully', (tester) async {
      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pumpAndSettle();

      // Enter valid values
      await tester.enterText(find.widgetWithText(TextFormField, 'Valor Alvo'), '20000000'); // R$ 200.000,00
      await tester.enterText(find.widgetWithText(TextFormField, 'Aporte Mensal Pretendido'), '100000'); // R$ 1.000,00
      await tester.enterText(find.widgetWithText(TextFormField, 'Prazo Estimado (em anos)'), '15');
      await tester.pump();

      await tester.tap(find.text('Salvar Meta'));
      await tester.pumpAndSettle();

      // Assert success SnackBar
      expect(find.text('Meta salva com sucesso ✅'), findsOneWidget);

      // Assert summary card is now displayed
      expect(find.byType(GoalSummaryCard), findsOneWidget);
      expect(find.text('R\$ 200.000,00'), findsNWidgets(2));

      // Aguarda o timer de 1s do SnackBar disparar e a animação de sumir terminar
      await tester.pump(const Duration(seconds: 2));
      await tester.pumpAndSettle();
    });

    testWidgets('should display empty form when there is no saved goal', (tester) async {
      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('Minhas Metas'), findsOneWidget);
      expect(find.text('Defina seu Objetivo Financeiro'), findsOneWidget);
      
      // Inputs should be empty
      expect(find.widgetWithText(TextFormField, 'Valor Alvo'), findsOneWidget);
      final valueField = tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Valor Alvo'));
      expect(valueField.controller?.text, isEmpty);

      final contributionField = tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Aporte Mensal Pretendido'));
      expect(contributionField.controller?.text, isEmpty);

      final yearsField = tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Prazo Estimado (em anos)'));
      expect(yearsField.controller?.text, isEmpty);

      // Goal summary card should not be displayed
      expect(find.byType(GoalSummaryCard), findsNothing);
    });

    testWidgets('should display prefilled form and summary card when goal exists', (tester) async {
      // Arrange
      mockGoalsRepository.goal = const FinancialGoal(
        id: 'existing-id',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 300000.0,
        monthlyContribution: 1500.0,
        estimatedYears: 10,
      );

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pumpAndSettle();

      // Assert prefilled values
      final valueField = tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Valor Alvo'));
      expect(valueField.controller?.text, 'R\$ 300.000,00');

      final contributionField = tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Aporte Mensal Pretendido'));
      expect(contributionField.controller?.text, 'R\$ 1.500,00');

      final yearsField = tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Prazo Estimado (em anos)'));
      expect(yearsField.controller?.text, '10');

      // Summary card should be visible
      expect(find.byType(GoalSummaryCard), findsOneWidget);
      expect(find.text('R\$ 300.000,00'), findsNWidgets(2)); // in input and in summary card
    });

    testWidgets('should show inline validation errors when fields are empty and submitted', (tester) async {
      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pumpAndSettle();

      // Tap Salvar Meta
      await tester.tap(find.text('Salvar Meta'));
      await tester.pump();

      // Assert error messages
      expect(find.text('Este campo é obrigatório'), findsNWidgets(3));
    });

    testWidgets('should show error when values are zero or negative', (tester) async {
      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pumpAndSettle();

      // Enter zero values
      await tester.enterText(find.widgetWithText(TextFormField, 'Valor Alvo'), '0');
      await tester.enterText(find.widgetWithText(TextFormField, 'Aporte Mensal Pretendido'), '0');
      await tester.enterText(find.widgetWithText(TextFormField, 'Prazo Estimado (em anos)'), '0');
      await tester.pump();

      await tester.tap(find.text('Salvar Meta'));
      await tester.pump();

      // Assert validation errors
      expect(find.text('O valor deve ser maior que R\$ 0,00'), findsNWidgets(2));
      expect(find.text('O prazo deve estar entre 1 e 50 anos'), findsOneWidget);

      // Desmonta a árvore para limpar o foco e os timers dos cursores dos TextFields
      await tester.pumpWidget(const SizedBox());
    });

    testWidgets('should prevent duplicate submissions when Save Button is tapped multiple times quickly', (tester) async {
      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pumpAndSettle();

      // Enter valid values
      await tester.enterText(find.widgetWithText(TextFormField, 'Valor Alvo'), '20000000');
      await tester.enterText(find.widgetWithText(TextFormField, 'Aporte Mensal Pretendido'), '100000');
      await tester.enterText(find.widgetWithText(TextFormField, 'Prazo Estimado (em anos)'), '15');
      await tester.pump();

      // Tap multiple times quickly
      await tester.tap(find.text('Salvar Meta'));
      await tester.tap(find.text('Salvar Meta'));
      await tester.pump(); // Start execution but don't settle yet

      // Verify that createGoal was only called once
      expect(mockGoalsRepository.createGoalCallCount, 1);

      await tester.pumpAndSettle();
      await tester.pumpWidget(const SizedBox());
    });
  });
}
