import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import 'package:frontend/src/features/ranking/domain/entities/graham_recommendation.dart';
import 'package:frontend/src/features/ranking/domain/repositories/graham_recommendation_repository.dart';
import 'package:frontend/src/features/ranking/presentation/pages/graham_recommendations_page.dart';
import 'package:frontend/src/features/ranking/presentation/providers/graham_recommendation_provider.dart';

class FlowMockGrahamRecommendationRepository implements GrahamRecommendationRepository {
  List<GrahamRecommendation> initialRecommendations = [];
  List<GrahamRecommendation> updatedRecommendations = [];
  int getRecommendationsCallCount = 0;
  bool shouldThrowPremiumError = false;
  bool shouldThrowNetworkError = false;
  bool triggerCalled = false;

  @override
  Future<List<GrahamRecommendation>> getRecommendations() async {
    getRecommendationsCallCount++;
    if (shouldThrowPremiumError) {
      throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade.');
    }
    if (shouldThrowNetworkError) {
      throw Exception('Network error');
    }
    if (getRecommendationsCallCount > 1 && updatedRecommendations.isNotEmpty) {
      return updatedRecommendations;
    }
    return initialRecommendations;
  }

  @override
  Future<void> triggerCalculation() async {
    triggerCalled = true;
    return;
  }
}

void main() {
  late FlowMockGrahamRecommendationRepository mockRepository;
  late GrahamRecommendationProvider recommendationProvider;

  setUp(() {
    mockRepository = FlowMockGrahamRecommendationRepository();
    recommendationProvider = GrahamRecommendationProvider(repository: mockRepository);
  });

  Widget createWidgetUnderTest() {
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (context, state) => const GrahamRecommendationsPage(),
        ),
      ],
    );

    return MaterialApp.router(
      routerConfig: router,
      builder: (context, child) => ChangeNotifierProvider<GrahamRecommendationProvider>.value(
        value: recommendationProvider,
        child: child,
      ),
    );
  }

  group('GrahamRecommendationsPage E2E Flow Tests', () {
    testWidgets('[P0] should run polling when trigger is tapped and stop when list changes', (tester) async {
      mockRepository.initialRecommendations = [
        const GrahamRecommendation(
          ticker: 'PETR4',
          currentPrice: 30.0,
          intrinsicValue: 40.0,
          marginOfSafety: 0.33,
          currentAllocationPct: 5.0,
          targetAllocationPct: 20.0,
          allocationGap: 15.0,
          recommendationScore: 6.198,
        )
      ];
      mockRepository.updatedRecommendations = [
        const GrahamRecommendation(
          ticker: 'PETR4',
          currentPrice: 30.0,
          intrinsicValue: 40.0,
          marginOfSafety: 0.33,
          currentAllocationPct: 5.0,
          targetAllocationPct: 20.0,
          allocationGap: 15.0,
          recommendationScore: 6.198,
        ),
        const GrahamRecommendation(
          ticker: 'VALE3',
          currentPrice: 60.0,
          intrinsicValue: 90.0,
          marginOfSafety: 0.50,
          currentAllocationPct: 10.0,
          targetAllocationPct: 30.0,
          allocationGap: 20.0,
          recommendationScore: 8.300,
        )
      ];

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('PETR4'), findsOneWidget);
      expect(find.text('VALE3'), findsNothing);

      final buttonFinder = find.byType(FilledButton);
      expect(buttonFinder, findsOneWidget);
      await tester.tap(buttonFinder);
      await tester.pump();

      expect(find.byType(CircularProgressIndicator), findsAtLeastNWidgets(1));

      // Avançar o tempo virtual para simular o polling.
      // O provider faz loops de delay de 1s.
      await tester.pump(const Duration(seconds: 1));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('VALE3'), findsOneWidget);
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });

    testWidgets('[P1] should redirect to profile page when upgrade button is clicked in premium paywall', (tester) async {
      mockRepository.shouldThrowPremiumError = true;

      String currentPath = '/';

      Widget createWidgetUnderTestWithTraditionalRoutes() {
        return MaterialApp(
          initialRoute: '/',
          routes: {
            '/': (context) => ChangeNotifierProvider<GrahamRecommendationProvider>.value(
                  value: recommendationProvider,
                  child: const GrahamRecommendationsPage(),
                ),
            '/profile': (context) {
              currentPath = '/profile';
              return const Scaffold(body: Text('Profile Page Dummy'));
            },
          },
        );
      }

      await tester.pumpWidget(createWidgetUnderTestWithTraditionalRoutes());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Motor de Recomendação Graham (Premium)'), findsOneWidget);

      final upgradeButtonFinder = find.widgetWithText(ElevatedButton, 'Fazer Upgrade na Página de Perfil');
      expect(upgradeButtonFinder, findsOneWidget);

      // Rolar até o botão para garantir visibilidade antes de clicar
      await tester.ensureVisible(upgradeButtonFinder);
      await tester.pump(const Duration(milliseconds: 100));

      await tester.tap(upgradeButtonFinder);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(currentPath, equals('/profile'));
      expect(find.text('Profile Page Dummy'), findsOneWidget);
    });

    testWidgets('[P1] should display error screen and recover when reload button is tapped', (tester) async {
      mockRepository.shouldThrowNetworkError = true;

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Falha ao carregar recomendações:'), findsOneWidget);
      expect(find.text('Exception: Network error'), findsOneWidget);

      final reloadButtonFinder = find.widgetWithText(ElevatedButton, 'Recarregar');
      expect(reloadButtonFinder, findsOneWidget);

      mockRepository.shouldThrowNetworkError = false;
      mockRepository.initialRecommendations = [
        const GrahamRecommendation(
          ticker: 'PETR4',
          currentPrice: 30.0,
          intrinsicValue: 40.0,
          marginOfSafety: 0.33,
          currentAllocationPct: 5.0,
          targetAllocationPct: 20.0,
          allocationGap: 15.0,
          recommendationScore: 6.198,
        )
      ];

      await tester.tap(reloadButtonFinder);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Falha ao carregar recomendações:'), findsNothing);
      expect(find.text('PETR4'), findsOneWidget);
    });
  });
}
