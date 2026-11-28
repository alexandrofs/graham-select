import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/dashboard/presentation/pages/dashboard_page.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/portfolio/domain/entities/monthly_evolution.dart';
import 'package:frontend/src/features/portfolio/domain/entities/portfolio_summary.dart';
import 'package:frontend/src/features/portfolio/domain/entities/trade.dart';
import 'package:frontend/src/features/portfolio/domain/repositories/portfolio_repository.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/presentation/widgets/financial_data_table.dart';
import 'package:frontend/src/features/portfolio/presentation/widgets/portfolio_kpi_card.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';
import 'package:frontend/src/features/profile/presentation/pages/profile_page.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:frontend/src/features/tags/domain/entities/portfolio_tag.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/pages/tag_management_page.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';
import 'package:frontend/src/features/tags/presentation/widgets/dashboard_tag_filter_bar.dart';

class _FakePortfolioRepository implements PortfolioRepository {
  _FakePortfolioRepository(
    this.positions,
    this.summary, {
    _StatefulTagRepository? tagRepository,
  }) : _tagRepository = tagRepository;

  final List<CustodyPosition> positions;
  final PortfolioSummary summary;
  final _StatefulTagRepository? _tagRepository;

  @override
  Future<List<Trade>> getManualTrades() async => [];

  @override
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
    String? assetClass,
    String? profileId,
  }) =>
      throw UnimplementedError();

  @override
  Future<Trade> updateManualTrade({
    required String id,
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
    String? assetClass,
    String? profileId,
  }) =>
      throw UnimplementedError();

  @override
  Future<void> deleteManualTrade(String id) => throw UnimplementedError();

  List<String> _untaggedTickers() {
    if (_tagRepository == null) return const [];
    final tagged = _tagRepository._tags
        .expand((t) => t.tickers)
        .map((t) => t.trim().toUpperCase())
        .toSet();
    return positions
        .map((p) => p.ticker.trim().toUpperCase())
        .where((t) => !tagged.contains(t))
        .toList();
  }

  List<CustodyPosition> _filterPositions(String? tagId) {
    final normalized = tagId?.trim();
    if (normalized == null || normalized.isEmpty || _tagRepository == null) {
      return positions;
    }
    final matching = _tagRepository._tags.where((t) => t.id == normalized);
    if (matching.isEmpty) return const [];
    final allowed =
        matching.first.tickers.map((t) => t.trim().toUpperCase()).toSet();
    return positions
        .where((p) => allowed.contains(p.ticker.trim().toUpperCase()))
        .toList();
  }

  @override
  Future<({List<CustodyPosition> positions, DateTime? metaPriceUpdatedAt, int untaggedAssetsCount, List<String> untaggedTickers})>
      getCustodyPositions({String? tagId}) async {
    final untagged = _untaggedTickers();
    return (
      positions: _filterPositions(tagId),
      metaPriceUpdatedAt: DateTime.utc(2026, 11, 14, 15),
      untaggedAssetsCount: untagged.length,
      untaggedTickers: untagged,
    );
  }

  @override
  Future<PortfolioEvolution> getPortfolioEvolution({String? tagId}) =>
      throw UnimplementedError();

  @override
  Future<PortfolioSummary> getSummary({String? tagId}) async {
    final normalized = tagId?.trim();
    if (normalized == null || normalized.isEmpty || _tagRepository == null) {
      return summary;
    }
    final filtered = _filterPositions(normalized);
    final equity = filtered.fold<double>(0.0, (sum, p) => sum + p.marketValue);
    return PortfolioSummary(
      totalEquity: equity,
      grossYieldPercentage: filtered.isEmpty ? 0.0 : 16.67,
      accumulatedDividends: filtered.isEmpty ? 0.0 : 220.0,
      monthlyProjection: filtered.isEmpty ? 0.0 : 18.0,
    );
  }
}

class _FakeProfileRepository implements ProfileRepository {
  @override
  Future<UserProfile> getProfile() async {
    return UserProfile(
      id: 1,
      email: 'investidor@grahamselect.com',
      fullName: 'Investidor Free',
      tier: 'FREE',
      investorProfile: 'MODERATE',
    );
  }

  @override
  Future<UserProfile> updateInvestorProfile(String profile) async {
    return UserProfile(
      id: 1,
      email: 'investidor@grahamselect.com',
      fullName: 'Investidor Free',
      tier: 'FREE',
      investorProfile: profile,
    );
  }
}

class _StatefulTagRepository implements TagRepository {
  _StatefulTagRepository(List<PortfolioTag> initial)
      : _tags = List<PortfolioTag>.from(initial);

  final List<PortfolioTag> _tags;
  int _seq = 1;

  @override
  Future<List<PortfolioTag>> fetchTags() async => List<PortfolioTag>.from(_tags);

  @override
  Future<PortfolioTag> createTag({required String name, String? color}) async {
    final trimmed = name.trim();
    if (_tags.any((t) => t.name.toLowerCase() == trimmed.toLowerCase())) {
      throw Exception('Já existe uma tag com o nome "$trimmed".');
    }
    final created = PortfolioTag(
      id: 'tag-inline-${_seq++}',
      name: trimmed,
      color: color ?? '#6366F1',
      tickers: const [],
      assetCount: 0,
    );
    _tags.add(created);
    return created;
  }

  @override
  Future<PortfolioTag> updateTag({
    required String tagId,
    required String name,
    String? color,
  }) async {
    final index = _tags.indexWhere((t) => t.id == tagId);
    final updated = _tags[index].copyWith(
      name: name.trim(),
      color: color ?? _tags[index].color,
    );
    _tags[index] = updated;
    return updated;
  }

  @override
  Future<void> deleteTag(String tagId) async {
    _tags.removeWhere((t) => t.id == tagId);
  }

  @override
  Future<List<PortfolioTag>> fetchAssetTags(String ticker) async {
    final upper = ticker.trim().toUpperCase();
    return _tags.where((t) => t.tickers.contains(upper)).toList();
  }

  @override
  Future<List<PortfolioTag>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) async {
    final upper = ticker.trim().toUpperCase();
    final desired = tagIds.toSet();
    for (var i = 0; i < _tags.length; i++) {
      final t = _tags[i];
      final tickers = List<String>.from(t.tickers);
      if (desired.contains(t.id)) {
        if (!tickers.contains(upper)) tickers.add(upper);
      } else {
        tickers.remove(upper);
      }
      _tags[i] = t.copyWith(tickers: tickers, assetCount: tickers.length);
    }
    return fetchAssetTags(upper);
  }

  @override
  Future<void> assignAssetToTag({
    required String tagId,
    required String ticker,
  }) async {
    final upper = ticker.trim().toUpperCase();
    final index = _tags.indexWhere((t) => t.id == tagId);
    if (index == -1) return;
    final t = _tags[index];
    final tickers = List<String>.from(t.tickers);
    if (!tickers.contains(upper)) {
      tickers.add(upper);
      _tags[index] = t.copyWith(tickers: tickers, assetCount: tickers.length);
    }
  }

  @override
  Future<void> unassignAssetFromTag({
    required String tagId,
    required String ticker,
  }) async {
    final upper = ticker.trim().toUpperCase();
    final index = _tags.indexWhere((t) => t.id == tagId);
    if (index == -1) return;
    final t = _tags[index];
    final tickers = List<String>.from(t.tickers)..remove(upper);
    _tags[index] = t.copyWith(tickers: tickers, assetCount: tickers.length);
  }
}

List<CustodyPosition> _samplePositions() => const [
      CustodyPosition(
        ticker: 'PETR4',
        quantity: 100,
        averagePrice: 30,
        currentPrice: 35,
        marketValue: 3500,
        gainLossPercentage: 16.67,
        priceSource: 'LIVE',
        assetClass: 'STOCK',
      ),
      CustodyPosition(
        ticker: 'VALE3',
        quantity: 50,
        averagePrice: 60,
        currentPrice: 65,
        marketValue: 3250,
        gainLossPercentage: 8.33,
        priceSource: 'LIVE',
        assetClass: 'STOCK',
      ),
      CustodyPosition(
        ticker: 'CDB BANCO X',
        quantity: 1,
        averagePrice: 1000,
        currentPrice: 1100,
        marketValue: 1100,
        gainLossPercentage: 10.0,
        priceSource: 'CACHE',
        assetClass: 'FIXED_INCOME',
      ),
    ];

PortfolioSummary _sampleSummary() => PortfolioSummary(
      totalEquity: 7850,
      grossYieldPercentage: 12.5,
      accumulatedDividends: 420,
      monthlyProjection: 35,
    );

Widget _buildRouterApp({
  required String initialLocation,
  required PortfolioProvider portfolioProvider,
  required ProfileProvider profileProvider,
  required TagProvider tagProvider,
}) {
  final router = GoRouter(
    initialLocation: initialLocation,
    routes: [
      GoRoute(
        path: '/dashboard',
        builder: (context, state) => const DashboardPage(),
      ),
      GoRoute(
        path: '/profile',
        builder: (context, state) => const ProfilePage(),
      ),
      GoRoute(
        path: '/tags',
        builder: (context, state) => const TagManagementPage(),
      ),
    ],
  );

  return MultiProvider(
    providers: [
      ChangeNotifierProvider<PortfolioProvider>.value(value: portfolioProvider),
      ChangeNotifierProvider<ProfileProvider>.value(value: profileProvider),
      ChangeNotifierProvider<TagProvider>.value(value: tagProvider),
    ],
    child: MaterialApp.router(
      theme: AppTheme.darkTheme,
      routerConfig: router,
    ),
  );
}

void main() {
  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
  });

  group('Tags Dashboard, Sheet & Profile Integration Tests', () {
    testWidgets(
      'associa múltiplas tags, cria tag inline no bottom sheet e filtra custódia/KPIs por tag via backend preservando aviso de ativos sem tag',
      (tester) async {
        tester.view.physicalSize = const Size(1440, 1400);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final tagRepo = _StatefulTagRepository(const [
          PortfolioTag(
            id: 'tag-div',
            name: 'Dividendos',
            color: '#6366F1',
            tickers: ['PETR4'],
            assetCount: 1,
          ),
          PortfolioTag(
            id: 'tag-com',
            name: 'Commodities',
            color: '#10B981',
            tickers: ['VALE3'],
            assetCount: 1,
          ),
          PortfolioTag(
            id: 'tag-vazia',
            name: 'Vazia',
            color: '#F59E0B',
            tickers: [],
            assetCount: 0,
          ),
        ]);
        final portfolioRepo = _FakePortfolioRepository(
          _samplePositions(),
          _sampleSummary(),
          tagRepository: tagRepo,
        );
        final portfolioProvider = PortfolioProvider(portfolioRepo);
        final profileProvider = ProfileProvider(_FakeProfileRepository());
        final tagProvider = TagProvider(repository: tagRepo);

        await tester.pumpWidget(
          _buildRouterApp(
            initialLocation: '/dashboard',
            portfolioProvider: portfolioProvider,
            profileProvider: profileProvider,
            tagProvider: tagProvider,
          ),
        );
        await tester.pumpAndSettle();

        // Barra de lente analítica + todas as posições e KPI global visíveis inicialmente
        expect(find.byKey(const Key('dashboard_tag_filter_bar')), findsOneWidget);
        expect(find.byKey(const Key('active_tag_lens_badge')), findsNothing);
        expect(find.byKey(const Key('untagged_assets_banner')), findsNothing);
        expect(
          find.descendant(
            of: find.byType(PortfolioKpiCard),
            matching: find.textContaining('7.850,00'),
          ),
          findsOneWidget,
        );
        expect(find.text('PETR4'), findsOneWidget);
        expect(find.text('VALE3'), findsOneWidget);
        expect(find.text('CDB BANCO X'), findsOneWidget);
        expect(find.text('Sem tag'), findsOneWidget);

        // Abre seletor de tags para PETR4
        final managePetr4 = find.byKey(const Key('manage_asset_tags_PETR4'));
        await tester.ensureVisible(managePetr4);
        await tester.tap(managePetr4);
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('asset_tag_selector_sheet')), findsOneWidget);

        // Marca a tag existente "Commodities" e cria "Bancos" inline
        await tester.tap(find.byKey(const Key('asset_tag_checkbox_tag-com')));
        await tester.enterText(
          find.byKey(const Key('new_tag_inline_input')),
          'Bancos',
        );
        await tester.tap(find.byKey(const Key('new_tag_inline_submit')));
        await tester.pumpAndSettle();

        await tester.tap(find.byKey(const Key('save_asset_tags_button')));
        await tester.pumpAndSettle();

        // Nova tag aparece imediatamente na barra de filtro e múltiplas tags aparecem na linha de PETR4
        expect(
          find.descendant(
            of: find.byType(DashboardTagFilterBar),
            matching: find.text('Bancos'),
          ),
          findsOneWidget,
        );
        expect(
          find.descendant(
            of: find.byType(FinancialDataTable),
            matching: find.text('Bancos'),
          ),
          findsOneWidget,
        );
        expect(find.text('Tags de PETR4 atualizadas com sucesso.'), findsOneWidget);

        // Filtra pela tag "Dividendos": backend recalcula KPIs (3.500,00) + exibe badge e aviso de ativo sem tag
        await tester.ensureVisible(find.byKey(const Key('tag_filter_tag-div')));
        await tester.pumpAndSettle();
        await tester.tap(find.byKey(const Key('tag_filter_tag-div')));
        await tester.pumpAndSettle();

        expect(find.text('Visualizando: Dividendos'), findsOneWidget);
        expect(find.text('PETR4'), findsOneWidget);
        expect(find.text('VALE3'), findsNothing);
        expect(find.text('CDB BANCO X'), findsNothing);
        expect(find.text('Patrimônio Total'), findsOneWidget);
        expect(
          find.descendant(
            of: find.byType(PortfolioKpiCard),
            matching: find.textContaining('3.500,00'),
          ),
          findsOneWidget,
        );
        expect(
          find.descendant(
            of: find.byType(PortfolioKpiCard),
            matching: find.textContaining('7.850,00'),
          ),
          findsNothing,
        );
        expect(find.text('1 ativo sem tag não está sendo exibido'), findsOneWidget);

        // Abre seletor rápido de ativos sem tag a partir do aviso discreto
        await tester.tap(find.byKey(const Key('associate_untagged_assets_button')));
        await tester.pumpAndSettle();
        expect(find.byKey(const Key('untagged_assets_quick_sheet')), findsOneWidget);
        expect(find.byKey(const Key('untagged_asset_item_CDB BANCO X')), findsOneWidget);
        await tester.tap(find.byTooltip('Fechar'));
        await tester.pumpAndSettle();

        // Filtra por tag sem ativos ("Vazia"): mostra empty state contextual + Limpar filtro
        await tester.tap(find.byKey(const Key('tag_filter_tag-vazia')));
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('filtered_tags_empty_state')), findsOneWidget);
        expect(
          find.text('Nenhum ativo associado à tag selecionada'),
          findsOneWidget,
        );

        await tester.tap(find.byKey(const Key('clear_tag_filter_button')));
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('active_tag_lens_badge')), findsNothing);
        expect(
          find.descendant(
            of: find.byType(PortfolioKpiCard),
            matching: find.textContaining('7.850,00'),
          ),
          findsOneWidget,
        );
        expect(find.text('PETR4'), findsOneWidget);
        expect(find.text('VALE3'), findsOneWidget);
        expect(find.text('CDB BANCO X'), findsOneWidget);

        // Navega pelo botão "Gerenciar Tags" do Dashboard para /tags
        await tester.ensureVisible(
          find.byKey(const Key('dashboard_manage_tags_button')),
        );
        await tester.tap(find.byKey(const Key('dashboard_manage_tags_button')));
        await tester.pumpAndSettle();

        expect(find.text('Gerenciar Tags'), findsOneWidget);
      },
    );

    testWidgets(
      'FinancialDataTable exibe badge +N quando ativo possui mais de 3 tags',
      (tester) async {
        tester.view.physicalSize = const Size(1440, 900);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final portfolioProvider = PortfolioProvider(
          _FakePortfolioRepository(
            [_samplePositions().first],
            _sampleSummary(),
          ),
        );
        await portfolioProvider.loadCustodyPositions();

        final tagProvider = TagProvider(
          repository: _StatefulTagRepository(const [
            PortfolioTag(id: 't1', name: 'A1', color: '#6366F1', tickers: ['PETR4'], assetCount: 1),
            PortfolioTag(id: 't2', name: 'A2', color: '#10B981', tickers: ['PETR4'], assetCount: 1),
            PortfolioTag(id: 't3', name: 'A3', color: '#8B5CF6', tickers: ['PETR4'], assetCount: 1),
            PortfolioTag(id: 't4', name: 'A4', color: '#F59E0B', tickers: ['PETR4'], assetCount: 1),
          ]),
        );
        await tagProvider.loadTags();

        await tester.pumpWidget(
          MultiProvider(
            providers: [
              ChangeNotifierProvider<PortfolioProvider>.value(value: portfolioProvider),
              ChangeNotifierProvider<TagProvider>.value(value: tagProvider),
            ],
            child: MaterialApp(
              theme: AppTheme.darkTheme,
              home: const Scaffold(body: FinancialDataTable()),
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.text('A1'), findsOneWidget);
        expect(find.text('A2'), findsOneWidget);
        expect(find.text('A3'), findsOneWidget);
        expect(find.text('A4'), findsNothing);
        expect(find.text('+1'), findsOneWidget);
      },
    );

    testWidgets(
      'ProfilePage navega para /tags ao clicar em Tags da Carteira',
      (tester) async {
        tester.view.physicalSize = const Size(1024, 900);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final portfolioProvider = PortfolioProvider(
          _FakePortfolioRepository(_samplePositions(), _sampleSummary()),
        );
        final profileProvider = ProfileProvider(_FakeProfileRepository());
        final tagProvider = TagProvider(
          repository: _StatefulTagRepository(const []),
        );

        await tester.pumpWidget(
          _buildRouterApp(
            initialLocation: '/profile',
            portfolioProvider: portfolioProvider,
            profileProvider: profileProvider,
            tagProvider: tagProvider,
          ),
        );
        await tester.pumpAndSettle();

        final tagsButton = find.byKey(const Key('profile_tags_management_button'));
        await tester.ensureVisible(tagsButton);
        await tester.tap(tagsButton);
        await tester.pumpAndSettle();

        expect(find.text('Gerenciar Tags'), findsOneWidget);
        expect(find.byKey(const Key('tags_empty_state')), findsOneWidget);
      },
    );

    testWidgets(
      'fechar seletor após criação inline cria tag na barra de filtro sem vinculá-la ao ativo',
      (tester) async {
        tester.view.physicalSize = const Size(1440, 1400);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        final portfolioRepo =
            _FakePortfolioRepository(_samplePositions(), _sampleSummary());
        final portfolioProvider = PortfolioProvider(portfolioRepo);
        final profileProvider = ProfileProvider(_FakeProfileRepository());
        final tagProvider = TagProvider(
          repository: _StatefulTagRepository(const []),
        );

        await tester.pumpWidget(
          _buildRouterApp(
            initialLocation: '/dashboard',
            portfolioProvider: portfolioProvider,
            profileProvider: profileProvider,
            tagProvider: tagProvider,
          ),
        );
        await tester.pumpAndSettle();

        final managePetr4 = find.byKey(const Key('manage_asset_tags_PETR4'));
        await tester.ensureVisible(managePetr4);
        await tester.tap(managePetr4);
        await tester.pumpAndSettle();

        await tester.enterText(
          find.byKey(const Key('new_tag_inline_input')),
          'Bancos',
        );
        await tester.tap(find.byKey(const Key('new_tag_inline_submit')));
        await tester.pumpAndSettle();

        // Fecha o seletor sem salvar a associação
        await tester.tap(find.byIcon(Icons.close));
        await tester.pumpAndSettle();

        expect(
          find.descendant(
            of: find.byType(DashboardTagFilterBar),
            matching: find.text('Bancos'),
          ),
          findsOneWidget,
        );
        expect(
          find.descendant(
            of: find.byType(FinancialDataTable),
            matching: find.text('Bancos'),
          ),
          findsNothing,
        );
        expect(tagProvider.tagsForTicker('PETR4'), isEmpty);
      },
    );
  });
}
