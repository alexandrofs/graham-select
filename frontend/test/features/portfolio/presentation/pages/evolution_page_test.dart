import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/portfolio/domain/entities/portfolio_summary.dart';
import 'package:frontend/src/features/portfolio/domain/entities/monthly_evolution.dart';
import 'package:frontend/src/features/portfolio/domain/repositories/portfolio_repository.dart';
import 'package:frontend/src/features/portfolio/domain/entities/trade.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/presentation/pages/evolution_page.dart';

class MockPortfolioRepository implements PortfolioRepository {
  bool shouldThrow = false;
  PortfolioEvolution? evolutionToReturn;
  Completer<PortfolioEvolution>? evolutionCompleter;

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
      getCustodyPositions() async => throw UnimplementedError();

  @override
  Future<PortfolioEvolution> getPortfolioEvolution() async {
    if (shouldThrow) {
      throw Exception('Erro ao buscar evolução do portfólio');
    }
    if (evolutionCompleter != null) {
      return evolutionCompleter!.future;
    }
    if (evolutionToReturn != null) {
      return evolutionToReturn!;
    }
    return const PortfolioEvolution(monthlyData: []);
  }
}

void main() {
  late MockPortfolioRepository mockRepository;
  late PortfolioProvider provider;

  setUp(() {
    mockRepository = MockPortfolioRepository();
    provider = PortfolioProvider(mockRepository);
  });

  Widget buildTestableWidget() {
    return MaterialApp(
      home: ChangeNotifierProvider<PortfolioProvider>.value(
        value: provider,
        child: const EvolutionPage(),
      ),
    );
  }

  group('EvolutionPage Widget Tests', () {
    testWidgets('deve exibir indicador de progresso no estado loading', (tester) async {
      // Simula loading indefinido usando Completer que nunca se completa
      final completer = Completer<PortfolioEvolution>();
      mockRepository.evolutionCompleter = completer;
      
      await tester.pumpWidget(buildTestableWidget());
      await tester.pump(); // Dispara o post frame callback e inicia o carregamento

      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('deve exibir card de erro no estado error', (tester) async {
      mockRepository.shouldThrow = true;

      await tester.pumpWidget(buildTestableWidget());
      await tester.pump(); // Dispara o post frame callback
      await tester.pumpAndSettle(); // Espera a transição de estado terminar

      expect(find.text('Erro ao carregar dados'), findsOneWidget);
      expect(find.textContaining('Erro ao buscar evolução do portfólio'), findsOneWidget);
      expect(find.text('Tentar Novamente'), findsOneWidget);
    });

    testWidgets('deve exibir o gráfico de barras e legendas no estado success', (tester) async {
      final tEvolution = const PortfolioEvolution(
        monthlyData: [
          MonthlyEvolution(month: '2026-04', totalContributions: 1500.0, totalDividends: 50.0),
          MonthlyEvolution(month: '2026-05', totalContributions: 2000.0, totalDividends: 100.0),
        ],
      );
      mockRepository.evolutionToReturn = tEvolution;

      await tester.pumpWidget(buildTestableWidget());
      await tester.pump(); // Dispara o post frame callback
      await tester.pumpAndSettle();

      // Verifica elementos do cabeçalho e legenda
      expect(find.text('Evolução da Carteira'), findsOneWidget);
      expect(find.text('Aportes (Compras)'), findsOneWidget);
      expect(find.text('Dividendos (Proventos)'), findsOneWidget);

      // Rótulos do gráfico de barras (meses formatados)
      expect(find.text('Abr/26'), findsOneWidget);
      expect(find.text('Mai/26'), findsOneWidget);
    });

    testWidgets('deve alternar para visualização de tabela e aplicar filtros', (tester) async {
      final tEvolution = const PortfolioEvolution(
        monthlyData: [
          MonthlyEvolution(month: '2026-04', totalContributions: 1500.0, totalDividends: 50.0),
          MonthlyEvolution(month: '2026-05', totalContributions: 2000.0, totalDividends: 100.0),
        ],
      );
      mockRepository.evolutionToReturn = tEvolution;

      await tester.pumpWidget(buildTestableWidget());
      await tester.pump(); // Dispara o post frame callback
      await tester.pumpAndSettle();

      // Clica no botão Pílula de Tabela
      await tester.tap(find.text('Tabela'));
      await tester.pumpAndSettle();

      // Verifica se a tabela renderiza os headers e dados corretamente
      expect(find.text('Mês de Referência'), findsOneWidget);
      expect(find.text('Total Aportado'), findsOneWidget);
      expect(find.text('Proventos Recebidos'), findsOneWidget);

      // Dados da tabela completos
      expect(find.text('Abril de 2026'), findsOneWidget);
      expect(find.text('Maio de 2026'), findsOneWidget);
      expect(find.text('R\$ 1.500,00'), findsOneWidget);
      expect(find.text('R\$ 50,00'), findsOneWidget);
      expect(find.text('R\$ 2.000,00'), findsOneWidget);
      expect(find.text('R\$ 100,00'), findsOneWidget);
    });
  });
}
