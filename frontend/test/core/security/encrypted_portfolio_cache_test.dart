import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/security/encrypted_portfolio_cache.dart';
import 'package:frontend/src/features/portfolio/data/models/custody_position_model.dart';
import 'package:frontend/src/features/portfolio/data/models/portfolio_summary_model.dart';

class _InMemorySecureStorage extends Fake implements FlutterSecureStorage {
  final Map<String, String> store = {};

  @override
  Future<void> write({
    required String key,
    required String? value,
    IOSOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    MacOsOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    if (value == null) {
      store.remove(key);
    } else {
      store[key] = value;
    }
  }

  @override
  Future<String?> read({
    required String key,
    IOSOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    MacOsOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    return store[key];
  }

  @override
  Future<void> delete({
    required String key,
    IOSOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    MacOsOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    store.remove(key);
  }

  @override
  Future<Map<String, String>> readAll({
    IOSOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    MacOsOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    return Map<String, String>.from(store);
  }
}

void main() {
  late _InMemorySecureStorage storage;
  late EncryptedPortfolioCache cache;
  final fixedNow = DateTime.utc(2026, 11, 27, 15, 30, 0);

  setUp(() {
    storage = _InMemorySecureStorage();
    cache = EncryptedPortfolioCache(storage: storage, now: () => fixedNow);
  });

  group('EncryptedPortfolioCache', () {
    test('salva e carrega snapshot de summary e custody por userId e tagId', () async {
      final summary = PortfolioSummaryModel(
        totalEquity: 150000.0,
        grossYieldPercentage: 12.5,
        accumulatedDividends: 3200.0,
        monthlyProjection: 450.0,
      );
      final positions = [
        CustodyPositionModel(
          ticker: 'PETR4',
          quantity: 100,
          averagePrice: 30,
          currentPrice: 35,
          marketValue: 3500,
          gainLossPercentage: 16.67,
          priceSource: 'LIVE',
          priceUpdatedAt: DateTime.utc(2026, 11, 27, 15, 0),
          assetClass: 'Ações',
        ),
      ];

      await cache.saveSummary(summary: summary, userId: 'u1', tagId: 'tag-1');
      await cache.saveCustody(
        positions: positions,
        metaPriceUpdatedAt: DateTime.utc(2026, 11, 27, 15, 0),
        untaggedAssetsCount: 1,
        untaggedTickers: const ['VALE3'],
        userId: 'u1',
        tagId: 'tag-1',
      );

      final loaded = await cache.loadSnapshot(userId: 'u1', tagId: 'tag-1');
      expect(loaded, isNotNull);
      expect(loaded!.summary?.totalEquity, equals(150000.0));
      expect(loaded.positions?.length, equals(1));
      expect(loaded.positions?.first.ticker, equals('PETR4'));
      expect(loaded.untaggedAssetsCount, equals(1));
      expect(loaded.untaggedTickers, equals(['VALE3']));
      expect(loaded.lastSyncedAt, equals(fixedNow));

      // Outro escopo permanece vazio
      final other = await cache.loadSnapshot(userId: 'u1', tagId: 'tag-2');
      expect(other, isNull);
    });

    test('clearAll remove todos os snapshots sem apagar outras chaves', () async {
      storage.store['jwt_token'] = 'keep-me-for-separate-test';
      await cache.saveSummary(
        summary: PortfolioSummaryModel(
          totalEquity: 1000,
          grossYieldPercentage: 5,
          accumulatedDividends: 10,
          monthlyProjection: 20,
        ),
        userId: 'u1',
      );

      await cache.clearAll();

      expect(await cache.loadSnapshot(userId: 'u1'), isNull);
      expect(storage.store.containsKey('jwt_token'), isTrue);
    });

    test('retorna null com segurança quando JSON armazenado está corrompido', () async {
      storage.store['kerno_portfolio_snapshot_default_global'] = '{invalid-json';

      final loaded = await cache.loadSnapshot();
      expect(loaded, isNull);
    });

    test('serializa chamadas concorrentes de saveSummary e saveCustody sem perder dados', () async {
      final summary = PortfolioSummaryModel(
        totalEquity: 99000.0,
        grossYieldPercentage: 9.5,
        accumulatedDividends: 1200.0,
        monthlyProjection: 300.0,
      );
      final positions = [
        CustodyPositionModel(
          ticker: 'WEGE3',
          quantity: 10,
          averagePrice: 40,
          currentPrice: 50,
          marketValue: 500,
          gainLossPercentage: 25,
          priceSource: 'LIVE',
          assetClass: 'Ações',
        ),
      ];

      await Future.wait([
        cache.saveSummary(summary: summary, userId: 'u1', tagId: 't1'),
        cache.saveCustody(positions: positions, userId: 'u1', tagId: 't1'),
      ]);

      final loaded = await cache.loadSnapshot(userId: 'u1', tagId: 't1');
      expect(loaded, isNotNull);
      expect(loaded!.summary?.totalEquity, equals(99000.0));
      expect(loaded.positions?.length, equals(1));
      expect(loaded.positions?.first.ticker, equals('WEGE3'));
    });

    test('evita colisão de chave quando userId ou tagId contêm underscore', () async {
      final s1 = PortfolioSummaryModel(
        totalEquity: 111.0,
        grossYieldPercentage: 1,
        accumulatedDividends: 1,
        monthlyProjection: 1,
      );
      final s2 = PortfolioSummaryModel(
        totalEquity: 222.0,
        grossYieldPercentage: 2,
        accumulatedDividends: 2,
        monthlyProjection: 2,
      );

      await cache.saveSummary(summary: s1, userId: 'a_b', tagId: 'c');
      await cache.saveSummary(summary: s2, userId: 'a', tagId: 'b_c');

      final loaded1 = await cache.loadSnapshot(userId: 'a_b', tagId: 'c');
      final loaded2 = await cache.loadSnapshot(userId: 'a', tagId: 'b_c');
      expect(loaded1?.summary?.totalEquity, equals(111.0));
      expect(loaded2?.summary?.totalEquity, equals(222.0));
    });
  });
}
