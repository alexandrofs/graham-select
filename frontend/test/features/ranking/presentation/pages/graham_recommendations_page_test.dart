import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:frontend/src/features/auth/data/auth_repository.dart';
import 'package:frontend/src/features/ranking/domain/entities/graham_recommendation.dart';
import 'package:frontend/src/features/ranking/data/models/graham_recommendation_model.dart';
import 'package:frontend/src/features/ranking/domain/repositories/graham_recommendation_repository.dart';
import 'package:frontend/src/features/ranking/presentation/pages/graham_recommendations_page.dart';
import 'package:frontend/src/features/ranking/presentation/providers/graham_recommendation_provider.dart';
import 'package:frontend/src/features/ranking/presentation/widgets/reasoning_box.dart';
import 'package:frontend/src/features/tags/domain/entities/portfolio_tag.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';

class _FakeUser extends Fake implements firebase_auth.User {
  @override
  String get uid => 'user-1';
}

class _FakeAuthRepository extends Fake implements AuthRepository {
  @override
  firebase_auth.User? get currentUser => _FakeUser();
}

class _RankingTagRepository implements TagRepository {
  @override
  Future<void> assignAssetToTag({required String tagId, required String ticker}) async {}

  @override
  Future<PortfolioTag> createTag({required String name, String? color}) =>
      throw UnimplementedError();

  @override
  Future<void> deleteTag(String tagId) async {}

  @override
  Future<List<PortfolioTag>> fetchAssetTags(String ticker) async => const [];

  @override
  Future<List<PortfolioTag>> fetchTags() async => const [
        PortfolioTag(
          id: 'tag-1',
          name: 'Dividendos',
          color: '#10B981',
          tickers: ['BBAS3'],
          assetCount: 1,
        ),
        PortfolioTag(
          id: 'tag-empty',
          name: 'SemMetas',
          color: '#F59E0B',
          tickers: [],
          assetCount: 0,
        ),
      ];

  @override
  Future<List<PortfolioTag>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) async =>
      const [];

  @override
  Future<void> unassignAssetFromTag({required String tagId, required String ticker}) async {}

  @override
  Future<PortfolioTag> updateTag({
    required String tagId,
    required String name,
    String? color,
  }) =>
      throw UnimplementedError();
}

class ManualMockGrahamRecommendationRepository implements GrahamRecommendationRepository {
  List<GrahamRecommendation> recommendations = [];
  final Map<String?, List<GrahamRecommendation>> recommendationsByScope = {};
  final List<String?> fetchedTags = [];
  final List<String?> triggeredTags = [];
  bool shouldThrowPremiumError = false;
  bool shouldThrowNetworkError = false;
  Completer<List<GrahamRecommendation>>? getRecommendationsCompleter;

  @override
  Future<List<GrahamRecommendation>> getRecommendations({String? tagId}) async {
    fetchedTags.add(tagId);
    if (getRecommendationsCompleter != null) {
      return getRecommendationsCompleter!.future;
    }
    if (shouldThrowPremiumError) {
      throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade.');
    }
    if (shouldThrowNetworkError) {
      throw Exception('Network error');
    }
    return recommendationsByScope[tagId] ?? recommendations;
  }

  @override
  Future<void> triggerCalculation({String? tagId}) async {
    triggeredTags.add(tagId);
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

  Widget createWidgetUnderTest({
    TagProvider? tagProvider,
    AuthRepository? authRepository,
  }) {
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
      builder: (context, child) => MultiProvider(
        providers: [
          ChangeNotifierProvider<GrahamRecommendationProvider>.value(
            value: recommendationProvider,
          ),
          if (tagProvider != null)
            ChangeNotifierProvider<TagProvider>.value(value: tagProvider),
          if (authRepository != null)
            Provider<AuthRepository>.value(value: authRepository),
        ],
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

    testWidgets('should switch analytical lens tabs, pass tagId on fetch/trigger, and display scoped empty state', (tester) async {
      mockRepository.recommendationsByScope[null] = [
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
      ];
      mockRepository.recommendationsByScope['tag-1'] = [
        const GrahamRecommendation(
          ticker: 'BBAS3',
          currentPrice: 25.0,
          intrinsicValue: 42.0,
          marginOfSafety: 0.68,
          currentAllocationPct: 10.0,
          targetAllocationPct: 50.0,
          allocationGap: 40.0,
          recommendationScore: 8.5,
        ),
      ];
      mockRepository.recommendationsByScope['tag-empty'] = const [];

      final tagProvider = TagProvider(repository: _RankingTagRepository());
      await tagProvider.loadTags();

      await tester.pumpWidget(
        createWidgetUnderTest(
          tagProvider: tagProvider,
          authRepository: _FakeAuthRepository(),
        ),
      );
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(find.byKey(const Key('analytical_lens_tabs')), findsOneWidget);
      expect(find.text('PETR4'), findsOneWidget);

      await tester.ensureVisible(find.byKey(const Key('tag_filter_tag-1')));
      await tester.tap(find.byKey(const Key('tag_filter_tag-1')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(find.text('Visualizando: Dividendos'), findsOneWidget);
      expect(find.text('BBAS3'), findsOneWidget);
      expect(find.text('PETR4'), findsNothing);

      // Dispara recálculo na lente tag-1 (inclui espera de fallback de 3s no widget)
      final triggerBtn = find.text('Atualizar Recomendações (Filtro de Graham)');
      await tester.ensureVisible(triggerBtn);
      await tester.tap(triggerBtn);
      await tester.pump();
      await tester.pump(const Duration(seconds: 4));
      await tester.pump(const Duration(milliseconds: 300));

      expect(mockRepository.triggeredTags, ['tag-1']);

      // Seleciona tag sem recomendações e valida mensagem contextual
      await tester.ensureVisible(find.byKey(const Key('tag_filter_tag-empty')));
      await tester.tap(find.byKey(const Key('tag_filter_tag-empty')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(
        find.textContaining('Nenhuma recomendação disponível para "SemMetas"'),
        findsOneWidget,
      );
      expect(mockRepository.fetchedTags.contains('tag-empty'), isTrue);
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
