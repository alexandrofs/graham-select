import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:frontend/src/core/security/privacy_provider.dart';
import 'package:frontend/src/features/dashboard/presentation/pages/dashboard_page.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/goals/domain/entities/financial_goal.dart';
import 'package:frontend/src/features/goals/domain/entities/goal_projection.dart';
import 'package:frontend/src/features/goals/domain/repositories/goals_repository.dart';
import 'package:frontend/src/features/goals/presentation/providers/goals_provider.dart';
import 'package:frontend/src/features/goals/presentation/widgets/goal_health_checklist_card.dart';
import 'package:frontend/src/features/goals/presentation/widgets/motivational_clock_card.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/data/models/portfolio_summary_model.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:frontend/src/features/tags/domain/entities/portfolio_tag.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';

import 'dashboard_page_test.mocks.dart';

class _StubTagRepository implements TagRepository {
  _StubTagRepository(this.tags);
  final List<PortfolioTag> tags;

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
  Future<List<PortfolioTag>> fetchTags() async => tags;

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

class _FakeGoalsRepository implements GoalsRepository {
  FinancialGoal? goal;
  GoalProjection? projection;

  @override
  Future<FinancialGoal?> fetchGoal() async => goal;

  @override
  Future<GoalProjection?> fetchProjection() async => projection;

  @override
  Future<FinancialGoal> createGoal(FinancialGoal goal) async => goal;

  @override
  Future<FinancialGoal> updateGoal(String id, FinancialGoal goal) async => goal;
}

@GenerateNiceMocks([
  MockSpec<PortfolioProvider>(),
  MockSpec<ProfileProvider>(),
])
void main() {
  late MockPortfolioProvider mockPortfolioProvider;
  late MockProfileProvider mockProfileProvider;

  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
  });

  setUp(() {
    mockPortfolioProvider = MockPortfolioProvider();
    mockProfileProvider = MockProfileProvider();

    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.initial);
    when(mockPortfolioProvider.summary).thenReturn(null);
    when(mockPortfolioProvider.errorMessage).thenReturn(null);
    
    // Stubs da custódia para evitar MissingStubError
    when(mockPortfolioProvider.custodyStatus).thenReturn(PortfolioStatus.initial);
    when(mockPortfolioProvider.custodyPositions).thenReturn([]);
    when(mockPortfolioProvider.rawCustodyPositions).thenReturn([]);
    when(mockPortfolioProvider.selectedAssetClassFilter).thenReturn(null);
    when(mockPortfolioProvider.custodyError).thenReturn(null);
    when(mockPortfolioProvider.loadCustodyPositions()).thenAnswer((_) async {});
    when(mockPortfolioProvider.loadSummary()).thenAnswer((_) async {});
    when(mockPortfolioProvider.isReceivingLiveUpdates).thenReturn(false);
  });

  Widget createWidgetUnderTest() {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<PortfolioProvider>.value(value: mockPortfolioProvider),
        ChangeNotifierProvider<ProfileProvider>.value(value: mockProfileProvider),
      ],
      child: const MaterialApp(home: DashboardPage()),
    );
  }

  testWidgets('should render all 4 KPI cards when summary is loaded', (tester) async {
    final summary = PortfolioSummaryModel(
      totalEquity: 50000.0,
      grossYieldPercentage: 12.5,
      accumulatedDividends: 1200.0,
      monthlyProjection: 450.0,
    );

    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.success);
    when(mockPortfolioProvider.summary).thenReturn(summary);

    await tester.pumpWidget(createWidgetUnderTest());

    expect(find.byKey(const Key('wealth_hero_card')), findsOneWidget);
    expect(find.text('Patrimônio Total'), findsOneWidget);
    expect(find.textContaining('50.000,00'), findsNWidgets(2));
    expect(find.text('Rendimento Bruto'), findsOneWidget);
    expect(find.text('12,5%'), findsOneWidget);
    expect(find.text('Dividendos Acumulados'), findsOneWidget);
    expect(find.textContaining('1.200,00'), findsOneWidget);
    expect(find.text('Projeção Mensal'), findsOneWidget);
    expect(find.textContaining('450,00'), findsOneWidget);
  });

  testWidgets('should show error state and retry button', (tester) async {
    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.error);
    when(mockPortfolioProvider.errorMessage).thenReturn('Connection Timeout');

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pump(); // Aguarda o PostFrameCallback do initState

    expect(find.textContaining('Erro ao carregar resumo: Connection Timeout'), findsOneWidget);
    expect(find.text('Tentar novamente'), findsOneWidget);

    await tester.tap(find.text('Tentar novamente'));
    // É chamado uma vez no initState e outra no clique do botão
    verify(mockPortfolioProvider.loadSummary()).called(2);
  });

  testWidgets('should render MotivationalClockCard in empty state when GoalsProvider has no goal', (tester) async {
    final goalsProvider = GoalsProvider(repository: _FakeGoalsRepository());

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<PortfolioProvider>.value(value: mockPortfolioProvider),
          ChangeNotifierProvider<ProfileProvider>.value(value: mockProfileProvider),
          ChangeNotifierProvider<GoalsProvider>.value(value: goalsProvider),
        ],
        child: const MaterialApp(home: DashboardPage()),
      ),
    );
    await tester.pump();

    expect(find.byType(MotivationalClockCard), findsOneWidget);
    expect(find.byType(GoalHealthChecklistCard), findsNothing);
    expect(find.text('Defina sua Meta Financeira para desbloquear o Relógio Time-to-Goal'), findsOneWidget);
  });

  testWidgets('should render GoalHealthChecklistCard when GoalsProvider has an active goal', (tester) async {
    final fakeRepo = _FakeGoalsRepository()
      ..goal = const FinancialGoal(
        id: 'goal-1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 10,
      );
    final goalsProvider = GoalsProvider(repository: fakeRepo);

    final summary = PortfolioSummaryModel(
      totalEquity: 100000.0,
      grossYieldPercentage: 10.0,
      accumulatedDividends: 1200.0,
      monthlyProjection: 450.0,
    );
    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.success);
    when(mockPortfolioProvider.summary).thenReturn(summary);

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<PortfolioProvider>.value(value: mockPortfolioProvider),
          ChangeNotifierProvider<ProfileProvider>.value(value: mockProfileProvider),
          ChangeNotifierProvider<GoalsProvider>.value(value: goalsProvider),
        ],
        child: const MaterialApp(home: DashboardPage()),
      ),
    );
    await tester.pump();
    await tester.pump();

    expect(find.byType(MotivationalClockCard), findsOneWidget);
    expect(find.byType(GoalHealthChecklistCard), findsOneWidget);
    expect(find.text('Checklist de Saúde da Meta'), findsOneWidget);
  });

  testWidgets('should keep GoalHealthChecklistCard on globalSummary and show ActiveTagLensBadge plus untagged banner', (tester) async {
    final fakeRepo = _FakeGoalsRepository()
      ..goal = const FinancialGoal(
        id: 'goal-1',
        goalType: 'PATRIMONY_TARGET',
        targetValue: 500000.0,
        monthlyContribution: 2000.0,
        estimatedYears: 10,
      );
    final goalsProvider = GoalsProvider(repository: fakeRepo);

    final tagSummary = PortfolioSummaryModel(
      totalEquity: 25000.0,
      grossYieldPercentage: 8.0,
      accumulatedDividends: 300.0,
      monthlyProjection: 120.0,
    );
    final globalSummary = PortfolioSummaryModel(
      totalEquity: 100000.0,
      grossYieldPercentage: 10.0,
      accumulatedDividends: 1200.0,
      monthlyProjection: 450.0,
    );
    final tagProvider = TagProvider(
      repository: _StubTagRepository(const [
        PortfolioTag(
          id: 'tag-1',
          name: 'Dividendos',
          color: '#10B981',
          tickers: ['PETR4'],
          assetCount: 1,
        ),
      ]),
    );
    await tagProvider.loadTags();
    tagProvider.selectFilterTag('tag-1');

    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.success);
    when(mockPortfolioProvider.summary).thenReturn(tagSummary);
    when(mockPortfolioProvider.globalSummary).thenReturn(globalSummary);
    when(mockPortfolioProvider.activeTagId).thenReturn('tag-1');
    when(mockPortfolioProvider.untaggedAssetsCount).thenReturn(2);
    when(mockPortfolioProvider.untaggedTickers).thenReturn(const ['VALE3', 'WEGE3']);

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<PortfolioProvider>.value(value: mockPortfolioProvider),
          ChangeNotifierProvider<ProfileProvider>.value(value: mockProfileProvider),
          ChangeNotifierProvider<GoalsProvider>.value(value: goalsProvider),
          ChangeNotifierProvider<TagProvider>.value(value: tagProvider),
        ],
        child: const MaterialApp(home: DashboardPage()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Visualizando: Dividendos'), findsOneWidget);
    expect(find.text('2 ativos sem tag não estão sendo exibidos'), findsOneWidget);
    expect(find.textContaining('25.000,00'), findsNWidgets(2));

    final checklist = tester.widget<GoalHealthChecklistCard>(
      find.byType(GoalHealthChecklistCard),
    );
    expect(checklist.currentEquity, 100000.0);
    expect(checklist.annualYieldPercentage, 10.0);
    verify(mockPortfolioProvider.loadSummary(tagId: 'tag-1')).called(1);
    verify(mockPortfolioProvider.loadCustodyPositions(tagId: 'tag-1')).called(1);
  });

  testWidgets('exibe offline_cache_banner com timestamp local quando isOfflineCache é true', (tester) async {
    final syncedAtUtc = DateTime.utc(2026, 11, 27, 15, 30);
    final summary = PortfolioSummaryModel(
      totalEquity: 50000.0,
      grossYieldPercentage: 11.0,
      accumulatedDividends: 1500.0,
      monthlyProjection: 300.0,
    );

    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.success);
    when(mockPortfolioProvider.summary).thenReturn(summary);
    when(mockPortfolioProvider.isOfflineCache).thenReturn(true);
    when(mockPortfolioProvider.lastSyncedAt).thenReturn(syncedAtUtc);

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<PortfolioProvider>.value(value: mockPortfolioProvider),
          ChangeNotifierProvider<ProfileProvider>.value(value: mockProfileProvider),
        ],
        child: const MaterialApp(home: DashboardPage()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('offline_cache_banner')), findsOneWidget);
    expect(find.textContaining('Modo offline · Última sincronização em'), findsOneWidget);
  });

  testWidgets('Modo Privacidade mascara valores em R\$ no DashboardPage mantendo percentuais', (
    tester,
  ) async {
    final privacyProvider = PrivacyProvider(initialPrivacyMode: true);
    final summary = PortfolioSummaryModel(
      totalEquity: 75000.0,
      grossYieldPercentage: 15.0,
      accumulatedDividends: 2000.0,
      monthlyProjection: 500.0,
    );

    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.success);
    when(mockPortfolioProvider.summary).thenReturn(summary);

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<PortfolioProvider>.value(value: mockPortfolioProvider),
          ChangeNotifierProvider<ProfileProvider>.value(value: mockProfileProvider),
          ChangeNotifierProvider<PrivacyProvider>.value(value: privacyProvider),
        ],
        child: const MaterialApp(home: DashboardPage()),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('75.000,00'), findsNothing);
    expect(find.text(PrivacyProvider.maskedCurrencyValue), findsWidgets);
    expect(find.textContaining('15,00%'), findsOneWidget);
  });

  testWidgets('renderiza MacroClassDistributionCard quando rawCustodyPositions não está vazia', (
    tester,
  ) async {
    when(mockPortfolioProvider.rawCustodyPositions).thenReturn(const [
      CustodyPosition(
        ticker: 'ITUB4',
        quantity: 100,
        averagePrice: 30,
        currentPrice: 35,
        marketValue: 3500,
        gainLossPercentage: 10,
        priceSource: 'LIVE',
        assetClass: 'Ações',
      ),
    ]);

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('macro_class_distribution_card')), findsOneWidget);
    expect(find.text('Ações BR'), findsOneWidget);
  });
}
