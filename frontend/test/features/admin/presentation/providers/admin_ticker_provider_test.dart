import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/admin/data/datasources/admin_remote_datasource.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_ticker.dart';
import 'package:frontend/src/features/admin/domain/repositories/admin_repository.dart';
import 'package:frontend/src/features/admin/presentation/providers/admin_ticker_provider.dart';

class _MockAdminTickerRepository implements AdminRepository {
  List<AdminTicker> tickersResult = [];
  Object? errorToThrow;
  bool clearCacheCalled = false;
  String? lastEvictedTicker;
  String? lastDeletedId;

  @override
  Future<List<AdminTicker>> getTickers({String? query, String? assetClass}) async {
    if (errorToThrow != null) throw errorToThrow!;
    return tickersResult;
  }

  @override
  Future<AdminTicker> createTicker({
    required String ticker,
    required String name,
    required String assetClass,
    String? sector,
  }) async {
    if (errorToThrow != null) throw errorToThrow!;
    return AdminTicker(
      id: 'new-id',
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
    if (errorToThrow != null) throw errorToThrow!;
    return AdminTicker(
      id: id,
      ticker: 'BOVA11',
      name: name,
      assetClass: assetClass,
      sector: sector,
    );
  }

  @override
  Future<void> deleteTicker(String id) async {
    if (errorToThrow != null) throw errorToThrow!;
    lastDeletedId = id;
  }

  @override
  Future<void> clearMarketDataCache() async {
    if (errorToThrow != null) throw errorToThrow!;
    clearCacheCalled = true;
  }

  @override
  Future<void> evictTickerCache(String ticker) async {
    if (errorToThrow != null) throw errorToThrow!;
    lastEvictedTicker = ticker;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late _MockAdminTickerRepository mockRepo;
  late AdminTickerProvider provider;

  const sample1 = AdminTicker(
    id: '1',
    ticker: 'BOVA11',
    name: 'iShares Ibovespa ETF',
    assetClass: 'ETFs',
    sector: 'Índices',
  );

  const sample2 = AdminTicker(
    id: '2',
    ticker: 'PETR4',
    name: 'Petrobras PN',
    assetClass: 'Ações',
    sector: 'Petróleo',
  );

  setUp(() {
    mockRepo = _MockAdminTickerRepository();
    provider = AdminTickerProvider(repository: mockRepo);
  });

  group('AdminTickerProvider', () {
    test('loadTickers carrega lista de ativos com sucesso', () async {
      mockRepo.tickersResult = [sample1, sample2];

      await provider.loadTickers();

      expect(provider.isLoading, isFalse);
      expect(provider.isAccessDenied, isFalse);
      expect(provider.tickers.length, 2);
      expect(provider.filteredTickers.length, 2);
    });

    test('loadTickers trata AdminAccessDeniedException ativando isAccessDenied', () async {
      mockRepo.errorToThrow = const AdminAccessDeniedException();

      await provider.loadTickers();

      expect(provider.isAccessDenied, isTrue);
      expect(provider.errorMessage, contains('Acesso restrito'));
    });

    test('setSearchQuery filtra tickers localmente por ticker ou nome', () async {
      mockRepo.tickersResult = [sample1, sample2];
      await provider.loadTickers();

      provider.setSearchQuery('bova');
      expect(provider.filteredTickers.length, 1);
      expect(provider.filteredTickers.first.ticker, 'BOVA11');

      provider.setSearchQuery('Petrobras');
      expect(provider.filteredTickers.length, 1);
      expect(provider.filteredTickers.first.ticker, 'PETR4');
    });

    test('createTicker adiciona novo ativo e ordena por ticker', () async {
      mockRepo.tickersResult = [sample2];
      await provider.loadTickers();

      final success = await provider.createTicker(
        ticker: 'abev3',
        name: 'Ambev S.A.',
        assetClass: 'Ações',
        sector: 'Bebidas',
      );

      expect(success, isTrue);
      expect(provider.tickers.length, 2);
      expect(provider.tickers.first.ticker, 'ABEV3');
    });

    test('updateTicker atualiza item existente na lista', () async {
      mockRepo.tickersResult = [sample1];
      await provider.loadTickers();

      final success = await provider.updateTicker(
        id: '1',
        name: 'iShares Ibovespa Atualizado',
        assetClass: 'ETFs',
        sector: 'Renda Variável',
      );

      expect(success, isTrue);
      expect(provider.tickers.first.name, 'iShares Ibovespa Atualizado');
    });

    test('clearCache e evictTicker acionam o repositório com sucesso', () async {
      final cleared = await provider.clearCache();
      expect(cleared, isTrue);
      expect(mockRepo.clearCacheCalled, isTrue);

      final evicted = await provider.evictTicker('PETR4');
      expect(evicted, isTrue);
      expect(mockRepo.lastEvictedTicker, 'PETR4');
    });

    test('deleteTicker remove ativo da lista com sucesso', () async {
      mockRepo.tickersResult = [sample1, sample2];
      await provider.loadTickers();

      final deleted = await provider.deleteTicker('1');

      expect(deleted, isTrue);
      expect(mockRepo.lastDeletedId, '1');
      expect(provider.tickers.length, 1);
      expect(provider.tickers.first.id, '2');
    });

    test('setAssetClassFilter recarrega sem enviar query textual para não truncar lista após limpar busca', () async {
      mockRepo.tickersResult = [sample1, sample2];
      await provider.loadTickers();

      provider.setSearchQuery('BOVA');
      expect(provider.filteredTickers.length, 1);

      await provider.setAssetClassFilter('Todos');
      provider.setSearchQuery('');
      expect(provider.filteredTickers.length, 2);
    });
  });
}
