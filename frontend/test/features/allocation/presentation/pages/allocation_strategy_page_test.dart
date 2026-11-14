import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import 'package:frontend/src/features/allocation/domain/entities/allocation_goal.dart';
import 'package:frontend/src/features/allocation/domain/repositories/allocation_repository.dart';
import 'package:frontend/src/features/allocation/presentation/pages/allocation_strategy_page.dart';
import 'package:frontend/src/features/allocation/presentation/providers/allocation_provider.dart';
import 'package:frontend/src/features/tags/domain/entities/portfolio_tag.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';

class _AllocationTagRepository implements TagRepository {
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
          tickers: ['PETR4'],
          assetCount: 1,
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

class ManualMockAllocationRepository implements AllocationRepository {
  List<AllocationGoal> goals = [];
  final Map<String?, List<AllocationGoal>> goalsByScope = {};
  final List<String?> loadedTags = [];
  final List<String?> savedTags = [];
  bool shouldThrowPremiumError = false;
  bool shouldThrowNetworkError = false;
  Completer<List<AllocationGoal>>? getGoalsCompleter;

  @override
  Future<List<AllocationGoal>> getGoals({String? tagId}) async {
    loadedTags.add(tagId);
    if (getGoalsCompleter != null) {
      return getGoalsCompleter!.future;
    }
    if (shouldThrowPremiumError) {
      throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade do seu plano.');
    }
    if (shouldThrowNetworkError) {
      throw Exception('Network error');
    }
    return goalsByScope[tagId] ?? goals;
  }

  @override
  Future<void> saveGoals(List<AllocationGoal> goals, {String? tagId}) async {
    savedTags.add(tagId);
    goalsByScope[tagId] = List<AllocationGoal>.from(goals);
    if (tagId == null) {
      this.goals = goals;
    }
  }
}

void main() {
  late ManualMockAllocationRepository mockRepository;
  late AllocationProvider allocationProvider;

  setUp(() {
    mockRepository = ManualMockAllocationRepository();
    allocationProvider = AllocationProvider(mockRepository);
  });

  Widget createWidgetUnderTest({TagProvider? tagProvider}) {
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
      builder: (context, child) => MultiProvider(
        providers: [
          ChangeNotifierProvider<AllocationProvider>.value(value: allocationProvider),
          if (tagProvider != null)
            ChangeNotifierProvider<TagProvider>.value(value: tagProvider),
        ],
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

    testWidgets('should resync controllers cleanly when switching between Visão Geral and Tag without keeping _initialized stale', (tester) async {
      mockRepository.goalsByScope[null] = const [
        AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'ACOES', targetPercentage: 60.0),
        AllocationGoal(goalType: 'ASSET_CLASS', targetKey: 'FIIS', targetPercentage: 40.0),
      ];
      mockRepository.goalsByScope['tag-1'] = const [];

      final tagProvider = TagProvider(repository: _AllocationTagRepository());
      await tagProvider.loadTags();

      await tester.pumpWidget(createWidgetUnderTest(tagProvider: tagProvider));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(find.byKey(const Key('analytical_lens_tabs')), findsOneWidget);
      expect(find.textContaining('Estratégia de classes equilibrada!'), findsOneWidget);

      await tester.ensureVisible(find.byKey(const Key('tag_filter_tag-1')));
      await tester.tap(find.byKey(const Key('tag_filter_tag-1')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(find.text('Visualizando: Dividendos'), findsOneWidget);
      expect(find.textContaining('0% alocados em classes'), findsOneWidget);
      expect(mockRepository.loadedTags, [null, 'tag-1']);

      final acoesField = find.byWidgetPredicate(
        (w) => w is TextField && w.decoration?.labelText == 'Ações (ex: ACOES)',
      );
      await tester.enterText(acoesField, '100');
      await tester.pump();
      expect(find.textContaining('Estratégia de classes equilibrada!'), findsOneWidget);

      final saveBtn = find.byType(ElevatedButton);
      await tester.ensureVisible(saveBtn);
      await tester.tap(saveBtn);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(mockRepository.savedTags, ['tag-1']);
      expect(mockRepository.goalsByScope[null]!.length, 2);
      expect(mockRepository.goalsByScope['tag-1']!.single.targetPercentage, 100.0);
    });
  });
}
