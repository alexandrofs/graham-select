import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/tags/domain/entities/portfolio_tag.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';
import 'package:frontend/src/features/tags/presentation/widgets/analytical_lens_tabs.dart';

class _FakeTagRepository implements TagRepository {
  _FakeTagRepository(List<PortfolioTag> initial, {this.shouldThrow = false})
      : _tags = List<PortfolioTag>.from(initial);

  final List<PortfolioTag> _tags;
  bool shouldThrow;

  @override
  Future<void> assignAssetToTag({
    required String tagId,
    required String ticker,
  }) async {}

  @override
  Future<PortfolioTag> createTag({required String name, String? color}) async {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteTag(String tagId) async {}

  @override
  Future<List<PortfolioTag>> fetchAssetTags(String ticker) async {
    final upper = ticker.trim().toUpperCase();
    return _tags.where((t) => t.tickers.contains(upper)).toList();
  }

  @override
  Future<List<PortfolioTag>> fetchTags() async {
    if (shouldThrow) throw Exception('Falha ao listar tags');
    return List<PortfolioTag>.from(_tags);
  }

  @override
  Future<PortfolioTag> updateTag({
    required String tagId,
    required String name,
    String? color,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<void> unassignAssetFromTag({
    required String tagId,
    required String ticker,
  }) async {}

  @override
  Future<List<PortfolioTag>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) async {
    final upper = ticker.trim().toUpperCase();
    final selected = tagIds.toSet();
    for (var i = 0; i < _tags.length; i++) {
      final t = _tags[i];
      final next = List<String>.from(t.tickers);
      if (selected.contains(t.id)) {
        if (!next.contains(upper)) next.add(upper);
      } else {
        next.remove(upper);
      }
      _tags[i] = t.copyWith(tickers: next, assetCount: next.length);
    }
    return fetchAssetTags(upper);
  }
}

void main() {
  group('AnalyticalLensTabs, ActiveTagLensBadge e UntaggedAssetsQuickSheet', () {
    testWidgets(
      'renderiza Visão Geral por padrão, mostra contagem nas tags e exibe ActiveTagLensBadge ao selecionar',
      (tester) async {
        final repo = _FakeTagRepository(const [
          PortfolioTag(
            id: 't1',
            name: 'Dividendos',
            color: '#10B981',
            tickers: ['PETR4', 'BBAS3'],
            assetCount: 2,
          ),
          PortfolioTag(
            id: 't2',
            name: 'Nome Muito Longo Para Testar Truncamento Em Tela Pequena',
            color: '#6366F1',
            tickers: ['VALE3'],
            assetCount: 1,
          ),
        ]);
        final tagProvider = TagProvider(repository: repo);
        await tagProvider.loadTags();

        final selectedCallbacks = <String?>[];
        final router = GoRouter(
          routes: [
            GoRoute(
              path: '/',
              builder: (_, _) => Scaffold(
                body: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      AnalyticalLensTabs(
                        onTagSelected: selectedCallbacks.add,
                      ),
                      const SizedBox(height: 8),
                      const ActiveTagLensBadge(),
                    ],
                  ),
                ),
              ),
            ),
            GoRoute(
              path: '/tags',
              builder: (_, _) => const Scaffold(body: Text('Tela de Tags')),
            ),
          ],
        );

        tester.view.physicalSize = const Size(340, 800);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        await tester.pumpWidget(
          ChangeNotifierProvider<TagProvider>.value(
            value: tagProvider,
            child: MaterialApp.router(
              theme: AppTheme.darkTheme,
              routerConfig: router,
            ),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('analytical_lens_tabs')), findsOneWidget);
        expect(find.text('Visão Geral'), findsOneWidget);
        expect(find.byKey(const Key('tag_lens_count_t1')), findsOneWidget);
        expect(find.text('2'), findsOneWidget);
        expect(find.byKey(const Key('active_tag_lens_badge')), findsNothing);

        await tester.ensureVisible(find.byKey(const Key('tag_filter_t1')));
        await tester.tap(find.byKey(const Key('tag_filter_t1')));
        await tester.pumpAndSettle();

        expect(tagProvider.selectedFilterTagId, 't1');
        expect(selectedCallbacks, ['t1']);
        expect(find.byKey(const Key('active_tag_lens_badge')), findsOneWidget);
        expect(find.text('Visualizando: Dividendos'), findsOneWidget);

        await tester.tap(find.byKey(const Key('active_tag_lens_clear_button')));
        await tester.pumpAndSettle();

        expect(tagProvider.selectedFilterTagId, isNull);
        expect(find.byKey(const Key('active_tag_lens_badge')), findsNothing);
      },
    );

    testWidgets('não falha quando TagProvider está ausente e exibe retry no erro', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.darkTheme,
          home: const Scaffold(
            body: Column(
              children: [
                AnalyticalLensTabs(),
                ActiveTagLensBadge(),
              ],
            ),
          ),
        ),
      );

      expect(find.byKey(const Key('analytical_lens_tabs')), findsNothing);
      expect(find.byKey(const Key('active_tag_lens_badge')), findsNothing);

      final repo = _FakeTagRepository(const [], shouldThrow: true);
      final tagProvider = TagProvider(repository: repo);
      await tagProvider.loadTags();

      await tester.pumpWidget(
        ChangeNotifierProvider<TagProvider>.value(
          value: tagProvider,
          child: MaterialApp(
            theme: AppTheme.darkTheme,
            home: const Scaffold(body: AnalyticalLensTabs()),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('tag_filter_retry_button')), findsOneWidget);
      repo.shouldThrow = false;
      await tester.tap(find.byKey(const Key('tag_filter_retry_button')));
      await tester.pumpAndSettle();

      expect(find.text('Visão Geral'), findsOneWidget);
    });

    testWidgets('UntaggedAssetsQuickSheet associa ticker sem tag e atualiza lista reativamente', (tester) async {
      final repo = _FakeTagRepository(const [
        PortfolioTag(
          id: 't1',
          name: 'Hold',
          color: '#6366F1',
          tickers: [],
          assetCount: 0,
        ),
      ]);
      final tagProvider = TagProvider(repository: repo);
      await tagProvider.loadTags();

      var updatedCount = 0;

      await tester.pumpWidget(
        ChangeNotifierProvider<TagProvider>.value(
          value: tagProvider,
          child: MaterialApp(
            theme: AppTheme.darkTheme,
            home: Scaffold(
              body: Builder(
                builder: (context) => TextButton(
                  onPressed: () {
                    UntaggedAssetsQuickSheet.show(
                      context,
                      untaggedTickers: const ['WEGE3'],
                      tagProvider: tagProvider,
                      onTagsUpdated: () async {
                        updatedCount++;
                      },
                    );
                  },
                  child: const Text('Abrir'),
                ),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Abrir'));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('untagged_assets_quick_sheet')), findsOneWidget);
      expect(find.byKey(const Key('untagged_asset_item_WEGE3')), findsOneWidget);

      await tester.tap(find.byKey(const Key('untagged_asset_item_WEGE3')));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('asset_tag_checkbox_t1')));
      await tester.tap(find.byKey(const Key('save_asset_tags_button')));
      await tester.pumpAndSettle();

      expect(updatedCount, 1);
      expect(
        find.text('Todos os ativos listados já possuem tags associadas.'),
        findsOneWidget,
      );
    });
  });
}
