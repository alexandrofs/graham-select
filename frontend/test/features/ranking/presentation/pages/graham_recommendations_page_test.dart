import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import 'package:frontend/src/features/ranking/domain/entities/graham_recommendation.dart';
import 'package:frontend/src/features/ranking/data/models/graham_recommendation_model.dart';
import 'package:frontend/src/features/ranking/domain/repositories/graham_recommendation_repository.dart';
import 'package:frontend/src/features/ranking/presentation/pages/graham_recommendations_page.dart';
import 'package:frontend/src/features/ranking/presentation/providers/graham_recommendation_provider.dart';
import 'package:frontend/src/features/ranking/presentation/widgets/reasoning_box.dart';

class ManualMockGrahamRecommendationRepository implements GrahamRecommendationRepository {
  List<GrahamRecommendation> recommendations = [];
  bool shouldThrowPremiumError = false;
  bool shouldThrowNetworkError = false;
  Completer<List<GrahamRecommendation>>? getRecommendationsCompleter;

  @override
  Future<List<GrahamRecommendation>> getRecommendations() async {
    if (getRecommendationsCompleter != null) {
      return getRecommendationsCompleter!.future;
    }
    if (shouldThrowPremiumError) {
      throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade.');
    }
    if (shouldThrowNetworkError) {
      throw Exception('Network error');
    }
    return recommendations;
  }

  @override
  Future<void> triggerCalculation() async {
    return;
  }
}

void main() {
  late ManualMockGrahamRecommendationRepository mockRepository;
  late GrahamRecommendationProvider recommendationProvider;

  setUp(() {
    mockRepository = ManualMockGrahamRecommendationRepository();
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

  group('GrahamRecommendationsPage Widget Tests', () {
    testWidgets('should display shimmer pulse during loading', (tester) async {
      mockRepository.getRecommendationsCompleter = Completer<List<GrahamRecommendation>>();

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();

      expect(find.byType(AnimatedBuilder), findsAtLeastNWidgets(1));

      mockRepository.getRecommendationsCompleter!.complete([]);
      await tester.pump();
    });

    testWidgets('should display premium paywall when user is not premium', (tester) async {
      mockRepository.shouldThrowPremiumError = true;

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Motor de Recomendação Graham (Premium)'), findsOneWidget);
      expect(find.textContaining('Fazer Upgrade'), findsOneWidget);
    });

    testWidgets('should display empty state when list is empty', (tester) async {
      mockRepository.recommendations = [];

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Nenhuma recomendação disponível'), findsOneWidget);
      expect(find.textContaining('Certifique-se de ter cotações sincronizadas'), findsOneWidget);
    });

    testWidgets('should display recommendations list when success', (tester) async {
      mockRepository.recommendations = [
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

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('PETR4'), findsOneWidget);
      expect(find.text('Score: 6.20'), findsOneWidget);
      expect(find.text('+33.0% Margem'), findsOneWidget);
      expect(find.text('R\$ 30.00'), findsOneWidget);
      expect(find.text('R\$ 40.00'), findsOneWidget);
    });

    testWidgets('should open reasoning box modal when clicking "Por que comprar?"', (tester) async {
      mockRepository.recommendations = [
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

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('Por que comprar?'), findsOneWidget);

      await tester.tap(find.text('Por que comprar?'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      // O widget ReasoningBox deve estar visível
      expect(find.byType(ReasoningBox), findsOneWidget);
    });
  });

  group('GrahamRecommendationModel Tests', () {
    test('should populate epsUsed and bvpsUsed from JSON when available', () {
      final json = {
        'ticker': 'PETR4',
        'currentPrice': 30.0,
        'intrinsicValue': 40.0,
        'marginOfSafety': 0.33,
        'currentAllocationPct': 5.0,
        'targetAllocationPct': 20.0,
        'allocationGap': 15.0,
        'recommendationScore': 6.198,
        'epsUsed': 4.52,
        'bvpsUsed': 31.80,
      };

      final model = GrahamRecommendationModel.fromJson(json);

      expect(model.ticker, 'PETR4');
      expect(model.epsUsed, 4.52);
      expect(model.bvpsUsed, 31.80);
    });

    test('should set epsUsed and bvpsUsed to null when absent in JSON', () {
      final json = {
        'ticker': 'PETR4',
        'currentPrice': 30.0,
        'intrinsicValue': 40.0,
        'marginOfSafety': 0.33,
        'currentAllocationPct': 5.0,
        'targetAllocationPct': 20.0,
        'allocationGap': 15.0,
        'recommendationScore': 6.198,
      };

      final model = GrahamRecommendationModel.fromJson(json);

      expect(model.ticker, 'PETR4');
      expect(model.epsUsed, isNull);
      expect(model.bvpsUsed, isNull);
    });
  });
}
