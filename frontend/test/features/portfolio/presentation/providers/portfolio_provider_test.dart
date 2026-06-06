import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/portfolio/domain/entities/portfolio_summary.dart';
import 'package:frontend/src/features/portfolio/domain/entities/monthly_evolution.dart';
import 'package:frontend/src/features/portfolio/domain/repositories/portfolio_repository.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/domain/entities/trade.dart';

class MockPortfolioRepository implements PortfolioRepository {
  bool shouldThrow = false;
  List<CustodyPosition> positionsToReturn = [];
  DateTime? metaPriceUpdatedAtToReturn;
  PortfolioSummary? summaryToReturn;
  PortfolioEvolution? evolutionToReturn;

  @override
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
  }) async {
    if (shouldThrow) {
      throw Exception('Failed to create manual trade');
    }
    return Trade(
      id: '123',
      userId: 'user-123',
      ticker: ticker,
      side: side,
      tradeDate: tradeDate,
      quantity: quantity,
      price: price,
      broker: broker,
      createdAt: DateTime.now(),
    );
  }

  @override
  Future<PortfolioSummary> getSummary() async {
    if (shouldThrow) {
      throw Exception('Erro ao buscar resumo');
    }
    if (summaryToReturn != null) {
      return summaryToReturn!;
    }
    return PortfolioSummary(
      totalEquity: 10000.0,
      grossYieldPercentage: 15.0,
      accumulatedDividends: 1500.0,
      monthlyProjection: 200.0,
    );
  }

  @override
  Future<({List<CustodyPosition> positions, DateTime? metaPriceUpdatedAt})>
      getCustodyPositions() async {
    if (shouldThrow) {
      throw Exception('Erro ao buscar custódia');
    }
    return (positions: positionsToReturn, metaPriceUpdatedAt: metaPriceUpdatedAtToReturn);
  }

  @override
  Future<PortfolioEvolution> getPortfolioEvolution() async {
    if (shouldThrow) {
      throw Exception('Erro ao buscar evolução');
    }
    if (evolutionToReturn != null) {
      return evolutionToReturn!;
    }
    return const PortfolioEvolution(monthlyData: []);
  }
}

void main() {
  group('PortfolioProvider', () {
    late MockPortfolioRepository mockRepository;
    late PortfolioProvider provider;

    setUp(() {
      mockRepository = MockPortfolioRepository();
      provider = PortfolioProvider(mockRepository);
    });

    test('deve ter estado inicial correto', () {
      expect(provider.custodyStatus, equals(PortfolioStatus.initial));
      expect(provider.custodyPositions, isEmpty);
      expect(provider.custodyError, isNull);
      expect(provider.custodyMetaPriceUpdatedAt, isNull);
    });

    group('loadCustodyPositions', () {
      final tPositions = [
        const CustodyPosition(
          ticker: 'PETR4',
          quantity: 100.0,
          averagePrice: 30.0,
          currentPrice: 35.0,
          marketValue: 3500.0,
          gainLossPercentage: 16.67,
          priceSource: 'LIVE',
          priceUpdatedAt: null,
          assetClass: 'Ações',
        ),
        const CustodyPosition(
          ticker: 'VALE3',
          quantity: 50.0,
          averagePrice: 80.0,
          currentPrice: 75.0,
          marketValue: 3750.0,
          gainLossPercentage: -6.25,
          priceSource: 'CACHE',
          priceUpdatedAt: null,
          assetClass: 'Ações',
        ),
      ];
      final tDate = DateTime.utc(2026, 5, 30, 18, 0, 0);

      test('deve carregar posições de custódia com sucesso', () async {
        mockRepository.positionsToReturn = tPositions;
        mockRepository.metaPriceUpdatedAtToReturn = tDate;

        final future = provider.loadCustodyPositions();

        expect(provider.custodyStatus, equals(PortfolioStatus.loading));

        await future;

        expect(provider.custodyStatus, equals(PortfolioStatus.success));
        expect(provider.custodyPositions.length, equals(2));
        expect(provider.custodyMetaPriceUpdatedAt, equals(tDate));
        expect(provider.custodyError, isNull);
      });

      test('deve atualizar estado para erro se a chamada falhar', () async {
        mockRepository.shouldThrow = true;

        await provider.loadCustodyPositions();

        expect(provider.custodyStatus, equals(PortfolioStatus.error));
        expect(provider.custodyPositions, isEmpty);
        expect(provider.custodyError, equals('Erro ao buscar custódia'));
      });
    });

    group('sortBy', () {
      final tPositions = [
        const CustodyPosition(
          ticker: 'VALE3',
          quantity: 50.0,
          averagePrice: 80.0,
          currentPrice: 75.0,
          marketValue: 3750.0,
          gainLossPercentage: -6.25,
          priceSource: 'CACHE',
          assetClass: 'Ações',
        ),
        const CustodyPosition(
          ticker: 'PETR4',
          quantity: 100.0,
          averagePrice: 30.0,
          currentPrice: 35.0,
          marketValue: 3500.0,
          gainLossPercentage: 16.67,
          priceSource: 'LIVE',
          assetClass: 'Ações',
        ),
      ];

      setUp(() async {
        mockRepository.positionsToReturn = tPositions;
        await provider.loadCustodyPositions();
      });

      test('deve ordenar por ticker de forma ascendente por padrão', () {
        expect(provider.sortColumn, equals('ticker'));
        expect(provider.sortAscending, isTrue);

        final sorted = provider.custodyPositions;
        expect(sorted[0].ticker, equals('PETR4'));
        expect(sorted[1].ticker, equals('VALE3'));
      });

      test('deve inverter ordenação ao chamar sortBy na mesma coluna', () {
        provider.sortBy('ticker');

        expect(provider.sortColumn, equals('ticker'));
        expect(provider.sortAscending, isFalse);

        final sorted = provider.custodyPositions;
        expect(sorted[0].ticker, equals('VALE3'));
        expect(sorted[1].ticker, equals('PETR4'));
      });

      test('deve alterar a coluna de ordenação com asc=true por padrão', () {
        provider.sortBy('marketValue');

        expect(provider.sortColumn, equals('marketValue'));
        expect(provider.sortAscending, isTrue);

        final sorted = provider.custodyPositions;
        expect(sorted[0].ticker, equals('PETR4'));
        expect(sorted[1].ticker, equals('VALE3'));
      });
    });

    test('resetStatus deve limpar mensagens de erro e voltar para o estado initial', () async {
      mockRepository.shouldThrow = true;
      await provider.loadSummary();

      expect(provider.status, equals(PortfolioStatus.error));
      expect(provider.errorMessage, equals('Erro ao buscar resumo'));

      provider.resetStatus();

      expect(provider.status, equals(PortfolioStatus.initial));
      expect(provider.errorMessage, isNull);
    });

    group('createManualTrade — tradeStatus independente', () {
      test('estado inicial do tradeStatus deve ser initial', () {
        expect(provider.tradeStatus, equals(TradeStatus.initial));
        expect(provider.tradeError, isNull);
      });

      test('deve atualizar tradeStatus para success após salvar com sucesso', () async {
        final future = provider.createManualTrade(
          ticker: 'PETR4',
          side: 'BUY',
          tradeDate: DateTime(2026, 6, 6),
          quantity: 10,
          price: 40.89,
          broker: 'XP',
        );

        expect(provider.tradeStatus, equals(TradeStatus.loading));

        await future;

        expect(provider.tradeStatus, equals(TradeStatus.success));
        expect(provider.tradeError, isNull);
      });

      test('deve atualizar tradeStatus para error quando o repositório lança exceção', () async {
        mockRepository.shouldThrow = true;

        await provider.createManualTrade(
          ticker: 'PETR4',
          side: 'BUY',
          tradeDate: DateTime(2026, 6, 6),
          quantity: 10,
          price: 40.89,
          broker: 'XP',
        );

        expect(provider.tradeStatus, equals(TradeStatus.error));
        expect(provider.tradeError, equals('Failed to create manual trade'));
      });

      test('resetTradeStatus deve limpar tradeStatus e tradeError', () async {
        mockRepository.shouldThrow = true;
        await provider.createManualTrade(
          ticker: 'PETR4',
          side: 'BUY',
          tradeDate: DateTime(2026, 6, 6),
          quantity: 10,
          price: 40.89,
          broker: 'XP',
        );

        expect(provider.tradeStatus, equals(TradeStatus.error));

        provider.resetTradeStatus();

        expect(provider.tradeStatus, equals(TradeStatus.initial));
        expect(provider.tradeError, isNull);
      });

      test('tradeStatus não deve impactar o status global do dashboard', () async {
        // Carrega o dashboard com sucesso primeiro
        await provider.loadSummary();
        expect(provider.status, equals(PortfolioStatus.success));

        // Salva um trade com sucesso
        await provider.createManualTrade(
          ticker: 'VALE3',
          side: 'BUY',
          tradeDate: DateTime(2026, 6, 6),
          quantity: 5,
          price: 65.00,
          broker: 'Clear',
        );

        // Trade status atualiza, mas status do dashboard permanece intacto
        expect(provider.tradeStatus, equals(TradeStatus.success));
        expect(provider.status, equals(PortfolioStatus.success));
      });
    });


    group('loadEvolutionData', () {
      test('deve carregar os dados de evolução com sucesso', () async {
        const tEvolution = PortfolioEvolution(
          monthlyData: [
            MonthlyEvolution(month: '2026-04', totalContributions: 1000.0, totalDividends: 50.0),
            MonthlyEvolution(month: '2026-05', totalContributions: 2000.0, totalDividends: 100.0),
          ],
        );

        mockRepository.evolutionToReturn = tEvolution;
        expect(provider.evolutionStatus, equals(PortfolioStatus.initial));

        final future = provider.loadEvolutionData();
        expect(provider.evolutionStatus, equals(PortfolioStatus.loading));

        await future;

        expect(provider.evolutionStatus, equals(PortfolioStatus.success));
        expect(provider.evolutionData, equals(tEvolution));
        expect(provider.evolutionError, isNull);
      });

      test('deve definir o estado de erro se falhar no carregamento', () async {
        mockRepository.shouldThrow = true;

        await provider.loadEvolutionData();

        expect(provider.evolutionStatus, equals(PortfolioStatus.error));
        expect(provider.evolutionData, isNull);
        expect(provider.evolutionError, equals('Erro ao buscar evolução'));
      });
    });

    group('Filtro por classe de ativos', () {
      final tPositions = [
        const CustodyPosition(
          ticker: 'PETR4',
          quantity: 100.0,
          averagePrice: 30.0,
          currentPrice: 35.0,
          marketValue: 3500.0,
          gainLossPercentage: 16.67,
          priceSource: 'LIVE',
          assetClass: 'Ações',
        ),
        const CustodyPosition(
          ticker: 'HGLG11',
          quantity: 10.0,
          averagePrice: 160.0,
          currentPrice: 165.0,
          marketValue: 1650.0,
          gainLossPercentage: 3.125,
          priceSource: 'LIVE',
          assetClass: 'FIIs',
        ),
      ];

      setUp(() async {
        mockRepository.positionsToReturn = tPositions;
        await provider.loadCustodyPositions();
      });

      test('deve retornar todas as posições sem filtro por padrão', () {
        expect(provider.selectedAssetClassFilter, isNull);
        expect(provider.custodyPositions.length, equals(2));
      });

      test('deve filtrar posições quando selecionado uma classe de ativos', () {
        provider.selectAssetClassFilter('FIIs');

        expect(provider.selectedAssetClassFilter, equals('FIIs'));
        expect(provider.custodyPositions.length, equals(1));
        expect(provider.custodyPositions[0].ticker, equals('HGLG11'));
      });

      test('deve retornar todas as posições ao limpar o filtro', () {
        provider.selectAssetClassFilter('FIIs');
        provider.selectAssetClassFilter(null);

        expect(provider.selectedAssetClassFilter, isNull);
        expect(provider.custodyPositions.length, equals(2));
      });
    });
  });
}
