import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/presentation/widgets/financial_data_table.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/features/portfolio/domain/repositories/portfolio_repository.dart';
import 'package:frontend/src/features/portfolio/domain/entities/trade.dart';
import 'package:frontend/src/features/portfolio/domain/entities/portfolio_summary.dart';
import 'package:frontend/src/features/portfolio/domain/entities/monthly_evolution.dart';
import 'package:intl/date_symbol_data_local.dart';

class MockPortfolioRepository implements PortfolioRepository {
  bool shouldThrow = false;
  List<CustodyPosition> positionsToReturn = [];
  DateTime? metaPriceUpdatedAtToReturn;

  @override
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
  }) async => throw UnimplementedError();

  @override
  Future<PortfolioSummary> getSummary() async => throw UnimplementedError();

  @override
  Future<({List<CustodyPosition> positions, DateTime? metaPriceUpdatedAt})>
      getCustodyPositions() async {
    if (shouldThrow) {
      throw Exception('Erro ao buscar custódia');
    }
    return (positions: positionsToReturn, metaPriceUpdatedAt: metaPriceUpdatedAtToReturn);
  }

  @override
  Future<PortfolioEvolution> getPortfolioEvolution() async => throw UnimplementedError();
}

void main() {
  setUpAll(() async {
    // Inicializa a formatação local em português para evitar LocaleDataException no intl
    await initializeDateFormatting('pt_BR', null);
  });

  late MockPortfolioRepository mockRepository;
  late PortfolioProvider provider;

  setUp(() {
    mockRepository = MockPortfolioRepository();
    provider = PortfolioProvider(mockRepository);
  });

  Widget buildTestableWidget() {
    return MaterialApp(
      home: Scaffold(
        body: ChangeNotifierProvider<PortfolioProvider>.value(
          value: provider,
          child: const FinancialDataTable(),
        ),
      ),
    );
  }

  testWidgets('não deve renderizar nada no estado initial', (tester) async {
    await tester.pumpWidget(buildTestableWidget());
    expect(find.byType(Card), findsNothing);
    expect(find.byType(DataTable), findsNothing);
  });

  testWidgets('deve renderizar skeleton/shimmer no estado loading', (tester) async {
    provider.loadCustodyPositions();

    await tester.pumpWidget(buildTestableWidget());
    await tester.pump();

    expect(find.byType(Card), findsOneWidget);
    expect(find.byType(DataTable), findsNothing);
  });

  testWidgets('deve renderizar alerta de anomalia no estado error', (tester) async {
    mockRepository.shouldThrow = true;
    await provider.loadCustodyPositions();

    await tester.pumpWidget(buildTestableWidget());

    expect(find.text('Ocorreu uma anomalia'), findsOneWidget);
    expect(find.text('Erro ao buscar custódia'), findsOneWidget);
    expect(find.text('Tentar novamente'), findsOneWidget);
  });

  testWidgets('deve renderizar empty state no estado success sem posições', (tester) async {
    mockRepository.positionsToReturn = [];
    await provider.loadCustodyPositions();

    await tester.pumpWidget(buildTestableWidget());

    expect(find.text('Nenhum ativo encontrado'), findsOneWidget);
    expect(find.textContaining('Sua carteira de investimentos está vazia'), findsOneWidget);
    expect(find.text('Fazer upload do arquivo B3 para começar'), findsOneWidget);
  });

  testWidgets('deve renderizar a tabela com dados no estado success com posições', (tester) async {
    // Configura viewport maior para a tabela larga não dar overflow
    tester.view.physicalSize = const Size(1200, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

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
      ),
    ];
    mockRepository.positionsToReturn = tPositions;
    mockRepository.metaPriceUpdatedAtToReturn = DateTime.utc(2026, 5, 30, 18, 0, 0);
    await provider.loadCustodyPositions();

    await tester.pumpWidget(buildTestableWidget());

    expect(find.text('Sua Custódia'), findsOneWidget);
    expect(find.byType(DataTable), findsOneWidget);
    expect(find.text('PETR4'), findsOneWidget);
    expect(find.text('100'), findsOneWidget);
    expect(find.textContaining('30,00'), findsOneWidget);
    expect(find.textContaining('35,00'), findsOneWidget);
    expect(find.textContaining('3.500,00'), findsOneWidget);
    expect(find.text('+16,67%'), findsOneWidget);
  });

  testWidgets('deve chamar sortBy ao clicar no header do Ticker', (tester) async {
    // Configura viewport maior para a tabela larga não dar overflow
    tester.view.physicalSize = const Size(1200, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final tPositions = [
      const CustodyPosition(
        ticker: 'VALE3',
        quantity: 50.0,
        averagePrice: 80.0,
        currentPrice: 75.0,
        marketValue: 3750.0,
        gainLossPercentage: -6.25,
        priceSource: 'CACHE',
      ),
      const CustodyPosition(
        ticker: 'PETR4',
        quantity: 100.0,
        averagePrice: 30.0,
        currentPrice: 35.0,
        marketValue: 3500.0,
        gainLossPercentage: 16.67,
        priceSource: 'LIVE',
      ),
    ];
    mockRepository.positionsToReturn = tPositions;
    await provider.loadCustodyPositions();

    await tester.pumpWidget(buildTestableWidget());

    await tester.tap(find.text('Ticker'));
    await tester.pumpAndSettle();

    expect(provider.sortColumn, equals('ticker'));
    expect(provider.sortAscending, isFalse);
  });
}
