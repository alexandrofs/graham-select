import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/tags/domain/entities/portfolio_tag.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/pages/tag_management_page.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';

class _InMemoryTagRepository implements TagRepository {
  _InMemoryTagRepository({List<PortfolioTag>? initialTags})
      : _tags = List<PortfolioTag>.from(initialTags ?? const []);

  final List<PortfolioTag> _tags;
  int _seq = 1;

  @override
  Future<List<PortfolioTag>> fetchTags() async {
    return List<PortfolioTag>.from(_tags);
  }

  @override
  Future<PortfolioTag> createTag({required String name, String? color}) async {
    final trimmed = name.trim();
    final exists = _tags.any(
      (t) => t.name.toLowerCase() == trimmed.toLowerCase(),
    );
    if (exists) {
      throw Exception('Já existe uma tag com o nome "$trimmed".');
    }
    final created = PortfolioTag(
      id: 'tag-new-${_seq++}',
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
    final trimmed = name.trim();
    final duplicate = _tags.any(
      (t) => t.id != tagId && t.name.toLowerCase() == trimmed.toLowerCase(),
    );
    if (duplicate) {
      throw Exception('Já existe uma tag com o nome "$trimmed".');
    }
    final index = _tags.indexWhere((t) => t.id == tagId);
    if (index == -1) {
      throw Exception('Tag não encontrada.');
    }
    final existing = _tags[index];
    final updated = existing.copyWith(
      name: trimmed,
      color: color ?? existing.color,
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

Widget _wrapWithApp(TagProvider provider) {
  return ChangeNotifierProvider<TagProvider>.value(
    value: provider,
    child: MaterialApp(
      theme: AppTheme.darkTheme,
      home: const TagManagementPage(),
    ),
  );
}

void main() {
  group('TagManagementPage Widget Tests', () {
    testWidgets(
      'exibe estado vazio inicialmente e permite criar nova tag via modal',
      (tester) async {
        final repo = _InMemoryTagRepository();
        final provider = TagProvider(repository: repo);

        await tester.pumpWidget(_wrapWithApp(provider));
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('tags_empty_state')), findsOneWidget);
        expect(find.text('Nenhuma tag cadastrada ainda'), findsOneWidget);

        await tester.tap(find.byKey(const Key('create_tag_button')));
        await tester.pumpAndSettle();

        await tester.enterText(
          find.byKey(const Key('tag_name_input')),
          'Dividendos',
        );
        await tester.tap(find.byKey(const Key('tag_color_option_#10B981')));
        await tester.tap(find.byKey(const Key('tag_dialog_save_button')));
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('tags_empty_state')), findsNothing);
        expect(find.text('Dividendos'), findsOneWidget);
        expect(find.text('0 ativo(s) vinculado(s)'), findsOneWidget);
        expect(
          find.text('Tag "Dividendos" criada com sucesso.'),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'permite renomear tag e exibe erro inline quando nome já existe',
      (tester) async {
        final repo = _InMemoryTagRepository(
          initialTags: const [
            PortfolioTag(
              id: 'tag-1',
              name: 'Dividendos',
              color: '#6366F1',
              tickers: ['PETR4'],
              assetCount: 1,
            ),
            PortfolioTag(
              id: 'tag-2',
              name: 'Crescimento',
              color: '#10B981',
              tickers: [],
              assetCount: 0,
            ),
          ],
        );
        final provider = TagProvider(repository: repo);

        await tester.pumpWidget(_wrapWithApp(provider));
        await tester.pumpAndSettle();

        expect(find.text('Dividendos'), findsOneWidget);
        expect(find.text('1 ativo(s) vinculado(s)'), findsOneWidget);

        await tester.tap(find.byKey(const Key('rename_tag_button_tag-1')));
        await tester.pumpAndSettle();

        await tester.enterText(
          find.byKey(const Key('tag_name_input')),
          'Crescimento',
        );
        await tester.tap(find.byKey(const Key('tag_dialog_save_button')));
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('tag_dialog_error')), findsOneWidget);
        expect(
          find.textContaining('Já existe uma tag com o nome "Crescimento"'),
          findsOneWidget,
        );

        await tester.enterText(
          find.byKey(const Key('tag_name_input')),
          'Renda Passiva',
        );
        await tester.tap(find.byKey(const Key('tag_dialog_save_button')));
        await tester.pumpAndSettle();

        expect(find.text('Renda Passiva'), findsOneWidget);
        expect(find.text('1 ativo(s) vinculado(s)'), findsOneWidget);
        expect(
          find.text('Tag atualizada para "Renda Passiva".'),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'exige confirmação explícita antes de excluir tag e mantém feedback claro',
      (tester) async {
        final repo = _InMemoryTagRepository(
          initialTags: const [
            PortfolioTag(
              id: 'tag-1',
              name: 'Dividendos',
              color: '#6366F1',
              tickers: ['PETR4'],
              assetCount: 1,
            ),
          ],
        );
        final provider = TagProvider(repository: repo);

        await tester.pumpWidget(_wrapWithApp(provider));
        await tester.pumpAndSettle();

        await tester.tap(find.byKey(const Key('delete_tag_button_tag-1')));
        await tester.pumpAndSettle();

        expect(
          find.textContaining('Suas operações e o histórico da carteira permanecerão intactos'),
          findsOneWidget,
        );

        // Cancela primeiro para garantir que não excluiu acidentalmente
        await tester.tap(find.byKey(const Key('confirm_delete_cancel_button')));
        await tester.pumpAndSettle();
        expect(find.text('Dividendos'), findsOneWidget);

        // Confirma exclusão
        await tester.tap(find.byKey(const Key('delete_tag_button_tag-1')));
        await tester.pumpAndSettle();
        await tester.tap(find.byKey(const Key('confirm_delete_confirm_button')));
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('tag_card_tag-1')), findsNothing);
        expect(find.byKey(const Key('tags_empty_state')), findsOneWidget);
        expect(
          find.text('Tag "Dividendos" excluída. Suas operações permaneceram intactas.'),
          findsOneWidget,
        );
      },
    );
  });
}
