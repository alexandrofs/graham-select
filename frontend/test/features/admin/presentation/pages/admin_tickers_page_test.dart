import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/admin/data/datasources/admin_remote_datasource.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_ticker.dart';
import 'package:frontend/src/features/admin/domain/repositories/admin_repository.dart';
import 'package:frontend/src/features/admin/presentation/pages/admin_tickers_page.dart';
import 'package:frontend/src/features/admin/presentation/providers/admin_ticker_provider.dart';
import 'package:provider/provider.dart';

class _ControllableAdminTickerRepository implements AdminRepository {
  Completer<List<AdminTicker>>? completer;
  List<AdminTicker>? immediateResult;
  Object? immediateError;
  bool clearCacheCalled = false;
  String? evictedTicker;
  String? deletedId;
  Object? actionError;

  @override
  Future<List<AdminTicker>> getTickers({String? query, String? assetClass}) {
    if (completer != null) {
      return completer!.future;
    }
    if (immediateError != null) {
      return Future.error(immediateError!);
    }
    return Future.value(immediateResult ?? []);
  }

  @override
  Future<AdminTicker> createTicker({
    required String ticker,
    required String name,
    required String assetClass,
    String? sector,
  }) async {
    if (actionError != null) {
      throw actionError!;
    }
    return AdminTicker(
      id: 'created-id',
      ticker: ticker,
      name: name,
      assetClass: assetClass,
      sector: sector,
    );
  }

  @override
  Future<AdminTicker> updateTicker({
    required String id,
    required String name,
    required String assetClass,
    String? sector,
  }) async {
    return AdminTicker(
      id: id,
      ticker: 'BOVA11',
      name: name,
      assetClass: assetClass,
      sector: sector,
    );
  }

  @override
  Future<void> clearMarketDataCache() async {
    clearCacheCalled = true;
  }

  @override
  Future<void> evictTickerCache(String ticker) async {
    evictedTicker = ticker;
  }

  @override
  Future<void> deleteTicker(String id) async {
    if (actionError != null) {
      throw actionError!;
    }
    deletedId = id;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  const sampleItem = AdminTicker(
    id: 'comp-100',
    ticker: 'BOVA11',
    name: 'iShares Ibovespa Fundo de Índice',
    assetClass: 'ETFs',
    sector: 'Índices',
  );

  Widget createWidgetUnderTest(AdminTickerProvider provider) {
    return MaterialApp(
      theme: AppTheme.darkTheme,
      home: ChangeNotifierProvider<AdminTickerProvider>.value(
        value: provider,
        child: const Scaffold(body: AdminTickersPage()),
      ),
    );
  }

  group('AdminTickersPage Widget Tests', () {
    testWidgets('exibe Skeleton durante o carregamento inicial', (tester) async {
      final repo = _ControllableAdminTickerRepository()
        ..completer = Completer<List<AdminTicker>>();
      final provider = AdminTickerProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pump();

      expect(find.byKey(const Key('admin_tickers_skeleton')), findsOneWidget);
    });

    testWidgets('exibe estado de Acesso Restrito em caso de 403 Forbidden', (tester) async {
      final repo = _ControllableAdminTickerRepository()
        ..immediateError = const AdminAccessDeniedException();
      final provider = AdminTickerProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_tickers_access_denied_state')), findsOneWidget);
      expect(find.text('Acesso Restrito a Administradores'), findsOneWidget);
    });

    testWidgets('exibe estado vazio quando nenhum ticker é retornado', (tester) async {
      final repo = _ControllableAdminTickerRepository()..immediateResult = [];
      final provider = AdminTickerProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_tickers_empty_state')), findsOneWidget);
      expect(find.text('Nenhum ticker encontrado'), findsOneWidget);
    });

    testWidgets('exibe tabela de tickers e permite limpar cache global e por ativo', (tester) async {
      final repo = _ControllableAdminTickerRepository()
        ..immediateResult = [sampleItem];
      final provider = AdminTickerProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_tickers_table')), findsOneWidget);
      expect(find.text('BOVA11'), findsOneWidget);
      expect(find.text('iShares Ibovespa Fundo de Índice'), findsOneWidget);

      final clearCacheBtn = find.byKey(const Key('admin_tickers_clear_cache_button'));
      await tester.tap(clearCacheBtn);
      await tester.pumpAndSettle();

      expect(repo.clearCacheCalled, isTrue);
      expect(find.text('Cache de Market Data limpo com sucesso!'), findsOneWidget);

      ScaffoldMessenger.of(tester.element(find.byType(AdminTickersPage))).hideCurrentSnackBar();
      await tester.pumpAndSettle();

      final evictBtn = find.byKey(const Key('admin_ticker_evict_cache_button_comp-100'));
      await tester.ensureVisible(evictBtn);
      await tester.tap(evictBtn);
      await tester.pumpAndSettle();

      expect(repo.evictedTicker, 'BOVA11');
    });

    testWidgets('abre modal de Novo Ticker e cadastra ativo com sucesso', (tester) async {
      final repo = _ControllableAdminTickerRepository()
        ..immediateResult = [sampleItem];
      final provider = AdminTickerProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('admin_tickers_create_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_ticker_form_dialog')), findsOneWidget);

      await tester.enterText(find.byKey(const Key('admin_ticker_input_ticker')), 'WEGE3');
      await tester.enterText(find.byKey(const Key('admin_ticker_input_name')), 'WEG S.A.');
      await tester.enterText(find.byKey(const Key('admin_ticker_input_sector')), 'Bens Industriais');

      await tester.tap(find.byKey(const Key('admin_ticker_save_button')));
      await tester.pumpAndSettle();

      expect(find.text('WEGE3'), findsOneWidget);
      expect(find.text('WEG S.A.'), findsOneWidget);
    });

    testWidgets('exibe estado de erro com botão de retry quando carregamento falha', (tester) async {
      final repo = _ControllableAdminTickerRepository()
        ..immediateError = Exception('Falha de rede simulada');
      final provider = AdminTickerProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_tickers_error_state')), findsOneWidget);
      expect(find.text('Falha de rede simulada'), findsOneWidget);

      repo.immediateError = null;
      repo.immediateResult = [sampleItem];
      await tester.tap(find.byKey(const Key('admin_tickers_retry_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_tickers_table')), findsOneWidget);
      expect(find.text('BOVA11'), findsOneWidget);
    });

    testWidgets('permite excluir ticker mediante diálogo de confirmação', (tester) async {
      final repo = _ControllableAdminTickerRepository()
        ..immediateResult = [sampleItem];
      final provider = AdminTickerProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      final deleteBtn = find.byKey(const Key('admin_ticker_delete_button_comp-100'));
      await tester.ensureVisible(deleteBtn);
      await tester.tap(deleteBtn);
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_ticker_delete_confirm_dialog')), findsOneWidget);

      await tester.tap(find.byKey(const Key('admin_ticker_delete_confirm_button')));
      await tester.pumpAndSettle();

      expect(repo.deletedId, 'comp-100');
      expect(find.text('Ticker BOVA11 removido com sucesso!'), findsOneWidget);
    });

    testWidgets('exibe erro detalhado do backend no modal quando criação falha', (tester) async {
      final repo = _ControllableAdminTickerRepository()
        ..immediateResult = [sampleItem]
        ..actionError = Exception('Ticker já cadastrado no dicionário mestre: BOVA11');
      final provider = AdminTickerProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('admin_tickers_create_button')));
      await tester.pumpAndSettle();

      await tester.enterText(find.byKey(const Key('admin_ticker_input_ticker')), 'BOVA11');
      await tester.enterText(find.byKey(const Key('admin_ticker_input_name')), 'BOVA11');

      await tester.tap(find.byKey(const Key('admin_ticker_save_button')));
      await tester.pumpAndSettle();

      expect(find.text('Ticker já cadastrado no dicionário mestre: BOVA11'), findsOneWidget);
    });
  });
}
