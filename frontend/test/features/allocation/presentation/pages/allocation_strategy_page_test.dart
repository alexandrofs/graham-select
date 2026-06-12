import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import 'package:frontend/src/features/allocation/domain/entities/allocation_goal.dart';
import 'package:frontend/src/features/allocation/domain/repositories/allocation_repository.dart';
import 'package:frontend/src/features/allocation/presentation/pages/allocation_strategy_page.dart';
import 'package:frontend/src/features/allocation/presentation/providers/allocation_provider.dart';

class ManualMockAllocationRepository implements AllocationRepository {
  List<AllocationGoal> goals = [];
  bool shouldThrowPremiumError = false;
  bool shouldThrowNetworkError = false;
  Completer<List<AllocationGoal>>? getGoalsCompleter;

  @override
  Future<List<AllocationGoal>> getGoals() async {
    if (getGoalsCompleter != null) {
      return getGoalsCompleter!.future;
    }
    if (shouldThrowPremiumError) {
      throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade do seu plano.');
    }
    if (shouldThrowNetworkError) {
      throw Exception('Network error');
    }
    return goals;
  }

  @override
  Future<void> saveGoals(List<AllocationGoal> goals) async {
    this.goals = goals;
  }
}

void main() {
  late ManualMockAllocationRepository mockRepository;
  late AllocationProvider allocationProvider;

  setUp(() {
    mockRepository = ManualMockAllocationRepository();
    allocationProvider = AllocationProvider(mockRepository);
  });

  Widget createWidgetUnderTest() {
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (context, state) => const AllocationStrategyPage(),
        ),
      ],
    );

    return MaterialApp.router(
      routerConfig: router,
      builder: (context, child) => ChangeNotifierProvider<AllocationProvider>.value(
        value: allocationProvider,
        child: child,
      ),
    );
  }

  group('AllocationStrategyPage Widget Tests', () {
    testWidgets('should display shimmer pulse during loading', (tester) async {
      // Arrange
      mockRepository.getGoalsCompleter = Completer<List<AllocationGoal>>();

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump(); // trigger initState post frame

      // Assert
      expect(find.byType(AnimatedBuilder), findsAtLeastNWidgets(1));

      // Cleanup
      mockRepository.getGoalsCompleter!.complete([]);
      await tester.pump();
    });

    testWidgets('should display premium paywall when user is not premium', (tester) async {
      // Arrange
      mockRepository.shouldThrowPremiumError = true;

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump(); // trigger initState post frame
      await tester.pump(const Duration(milliseconds: 100));

      // Assert
      expect(find.text('Configuração de Estratégia (Premium)'), findsOneWidget);
      expect(find.textContaining('Fazer Upgrade'), findsOneWidget);
    });

    testWidgets('should display class sum in real-time and enable/disable button', (tester) async {
      // Arrange
      mockRepository.goals = [
        const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'ACOES', targetPercentage: 50.00),
        const AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'FIIS', targetPercentage: 40.00),
      ];

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      // Assert initial loaded state (90% sum, button disabled)
      expect(find.textContaining('90% alocados'), findsOneWidget);
      
      final buttonFinder = find.byType(ElevatedButton);
      expect(buttonFinder, findsOneWidget);
      
      ElevatedButton btn = tester.widget<ElevatedButton>(buttonFinder);
      expect(btn.onPressed, isNull); // Disabled

      // Act: Type to make it 100%
      // Update Outros to 10%
      final outrosFieldFinder = find.byWidgetPredicate(
        (widget) => widget is TextField && widget.decoration?.labelText == 'Outros (ex: OUTROS)',
      );
      expect(outrosFieldFinder, findsOneWidget);

      await tester.enterText(outrosFieldFinder, '10');
      await tester.pump(); // update sum listener

      // Assert real-time sum becomes 100% and button gets enabled
      expect(find.textContaining('classes equilibrada'), findsOneWidget);
      
      btn = tester.widget<ElevatedButton>(buttonFinder);
      expect(btn.onPressed, isNotNull); // Enabled
    });
  });
}
