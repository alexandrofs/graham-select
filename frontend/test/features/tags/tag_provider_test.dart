import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/tags/domain/entities/portfolio_tag.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';

class FakeTagRepository implements TagRepository {
  List<PortfolioTag> storedTags = [];
  Exception? errorToThrow;
  int _counter = 0;

  @override
  Future<List<PortfolioTag>> fetchTags() async {
    if (errorToThrow != null) throw errorToThrow!;
    return List<PortfolioTag>.from(storedTags);
  }

  @override
  Future<List<PortfolioTag>> fetchAssetTags(String ticker) async {
    if (errorToThrow != null) throw errorToThrow!;
    final normalized = ticker.trim().toUpperCase();
    return storedTags.where((t) => t.tickers.contains(normalized)).toList();
  }

  @override
  Future<PortfolioTag> createTag({required String name, String? color}) async {
    if (errorToThrow != null) throw errorToThrow!;
    _counter++;
    final tag = PortfolioTag(
      id: 'tag-$_counter',
      name: name.trim(),
      color: (color ?? '#6366F1').toUpperCase(),
      tickers: const [],
      assetCount: 0,
    );
    storedTags.add(tag);
    return tag;
  }

  @override
  Future<PortfolioTag> updateTag({
    required String tagId,
    required String name,
    String? color,
  }) async {
    if (errorToThrow != null) throw errorToThrow!;
    final idx = storedTags.indexWhere((t) => t.id == tagId);
    final existing = storedTags[idx];
    final updated = existing.copyWith(
      name: name.trim(),
      color: (color ?? existing.color).toUpperCase(),
    );
    storedTags[idx] = updated;
    return updated;
  }

  @override
  Future<void> deleteTag(String tagId) async {
    if (errorToThrow != null) throw errorToThrow!;
    storedTags.removeWhere((t) => t.id == tagId);
  }

  @override
  Future<List<PortfolioTag>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) async {
    if (errorToThrow != null) throw errorToThrow!;
    final normalized = ticker.trim().toUpperCase();
    final idSet = tagIds.toSet();
    final updatedList = <PortfolioTag>[];
    final result = <PortfolioTag>[];
    for (final tag in storedTags) {
      final tickers = tag.tickers.toSet();
      if (idSet.contains(tag.id)) {
        tickers.add(normalized);
      } else {
        tickers.remove(normalized);
      }
      final next = tag.copyWith(tickers: tickers.toList()..sort());
      updatedList.add(next);
      if (idSet.contains(tag.id)) {
        result.add(next);
      }
    }
    storedTags = updatedList;
    return result;
  }

  @override
  Future<void> assignAssetToTag({required String tagId, required String ticker}) async {}

  @override
  Future<void> unassignAssetFromTag({required String tagId, required String ticker}) async {}
}

void main() {
  group('TagProvider', () {
    test('loadTags ordena tags e parseColor faz fallback seguro', () async {
      final repo = FakeTagRepository()
        ..storedTags = [
          const PortfolioTag(id: '2', name: 'Zebra', color: '#10B981', tickers: ['PETR4']),
          const PortfolioTag(id: '1', name: 'Aposentadoria', color: '#8B5CF6', tickers: ['WEGE3']),
        ];
      final provider = TagProvider(repository: repo);

      await provider.loadTags();

      expect(provider.state, TagState.loaded);
      expect(provider.tags.map((t) => t.name), ['Aposentadoria', 'Zebra']);
      expect(TagProvider.parseColor('#10B981'), const Color(0xFF10B981));
      expect(TagProvider.parseColor('invalid'), AppTheme.primaryColor);
    });

    test('createTag e erro de duplicidade expõem errorMessage corretamente', () async {
      final repo = FakeTagRepository();
      final provider = TagProvider(repository: repo);

      final created = await provider.createTag(name: 'Aposentadoria', color: '#10B981');
      expect(created, isNotNull);
      expect(provider.tags.length, 1);

      repo.errorToThrow = Exception('Já existe uma tag com o nome "Aposentadoria".');
      final failed = await provider.createTag(name: 'Aposentadoria');
      expect(failed, isNull);
      expect(provider.errorMessage, 'Já existe uma tag com o nome "Aposentadoria".');
    });

    test('setAssetTags e createAndAssignTag mantêm M:N e tagsForTicker atualizados', () async {
      final repo = FakeTagRepository();
      final provider = TagProvider(repository: repo);

      final t1 = await provider.createTag(name: 'Longo Prazo');
      final ok = await provider.setAssetTags(ticker: 'wege3', tagIds: [t1!.id]);
      expect(ok, isTrue);

      final t2 = await provider.createAndAssignTag(
        ticker: 'WEGE3',
        name: 'Dividendos',
        color: '#10B981',
      );
      expect(t2, isNotNull);

      final wegeTags = provider.tagsForTicker('  wege3 ');
      expect(wegeTags.map((t) => t.name), ['Dividendos', 'Longo Prazo']);
    });

    test('renameTag reflete em tagsForTicker e deleteTag limpa filtro ativo', () async {
      final repo = FakeTagRepository();
      final provider = TagProvider(repository: repo);

      final created = await provider.createTag(name: 'Aposentadoria');
      await provider.setAssetTags(ticker: 'PETR4', tagIds: [created!.id]);
      provider.selectFilterTag(created.id);

      expect(provider.filteredTickersForSelectedTag, {'PETR4'});

      final renamed = await provider.renameTag(
        tagId: created.id,
        name: 'Previdência',
        color: '#F59E0B',
      );
      expect(renamed?.name, 'Previdência');
      expect(provider.tagsForTicker('PETR4').first.name, 'Previdência');

      final deleted = await provider.deleteTag(created.id);
      expect(deleted, isTrue);
      expect(provider.tags, isEmpty);
      expect(provider.selectedFilterTagId, isNull);
      expect(provider.tagsForTicker('PETR4'), isEmpty);
    });

    test('PortfolioTag.copyWith preserva assetCount quando tickers não muda', () {
      const original = PortfolioTag(
        id: 'tag-count',
        name: 'Dividendos',
        color: '#6366F1',
        tickers: [],
        assetCount: 5,
      );

      final renamed = original.copyWith(name: 'Renda');
      expect(renamed.assetCount, 5);

      final reassigned = original.copyWith(tickers: const ['PETR4', 'VALE3']);
      expect(reassigned.assetCount, 2);
    });

    test('reset limpa cache, filtro e erros e invalida loadTags em voo', () async {
      final completer = Completer<List<PortfolioTag>>();
      final provider = TagProvider(repository: _DelayedFetchTagRepository(completer.future));

      final loadFuture = provider.loadTags();
      expect(provider.state, TagState.loading);

      provider.reset();
      expect(provider.state, TagState.initial);

      completer.complete(const [
        PortfolioTag(id: 'stale', name: 'Antiga', color: '#6366F1', tickers: ['PETR4'], assetCount: 1),
      ]);
      await loadFuture;

      expect(provider.state, TagState.initial);
      expect(provider.tags, isEmpty);

      final syncedProvider = TagProvider(
        repository: FakeTagRepository()
          ..storedTags = [
            const PortfolioTag(id: 't1', name: 'Dividendos', color: '#6366F1', tickers: ['PETR4'], assetCount: 1),
          ],
      );
      await syncedProvider.loadTags();
      syncedProvider.selectFilterTag('t1');
      syncedProvider.reset();

      expect(syncedProvider.state, TagState.initial);
      expect(syncedProvider.tags, isEmpty);
      expect(syncedProvider.selectedFilterTagId, isNull);
      expect(syncedProvider.tagsForTicker('PETR4'), isEmpty);
      expect(syncedProvider.errorMessage, isNull);
    });

    test('createAndAssignTag propaga falha parcial de setAssetTags mantendo tag criada', () async {
      final repo = _SetFailsTagRepository();
      final provider = TagProvider(repository: repo);

      final result = await provider.createAndAssignTag(
        ticker: 'PETR4',
        name: 'Nova',
        color: '#10B981',
      );

      expect(result, isNull);
      expect(provider.tags.map((t) => t.name), ['Nova']);
      expect(provider.tagsForTicker('PETR4'), isEmpty);
      expect(provider.errorMessage, 'Falha ao associar');
    });
  });
}

class _DelayedFetchTagRepository extends FakeTagRepository {
  final Future<List<PortfolioTag>> delayedFuture;
  _DelayedFetchTagRepository(this.delayedFuture);

  @override
  Future<List<PortfolioTag>> fetchTags() => delayedFuture;
}

class _SetFailsTagRepository extends FakeTagRepository {
  @override
  Future<List<PortfolioTag>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) async {
    throw Exception('Falha ao associar');
  }
}
