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
import 'package:frontend/src/features/tags/domain/entities/portfolio_tag.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';

class _EvolutionTagRepository implements TagRepository {
  @override
  Future<void> assignAssetToTag({required String tagId, required String ticker}) async {}

  @override
  Future<PortfolioTag> createTag({required String name, String? color}) =>
      throw UnimplementedError();

  @override
  Future<void> deleteTag(String tagId) async {}

  @override
  Future<List<PortfolioTag>> fetchAssetTags(String ticker) async => const [];

  @override
  Future<List<PortfolioTag>> fetchTags() async => const [
        PortfolioTag(
          id: 'tag-div',
          name: 'Dividendos',
          color: '#10B981',
          tickers: ['PETR4'],
          assetCount: 1,
        ),
      ];

  @override
  Future<List<PortfolioTag>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) async =>
      const [];

  @override
  Future<void> unassignAssetFromTag({required String tagId, required String ticker}) async {}

  @override
  Future<PortfolioTag> updateTag({
    required String tagId,
    required String name,
    String? color,
  }) =>
      throw UnimplementedError();
}

class MockPortfolioRepository implements PortfolioRepository {
  bool shouldThrow = false;
  PortfolioEvolution? evolutionToReturn;
  Completer<PortfolioEvolution>? evolutionCompleter;
  final List<String?> requestedTags = [];
  Map<String?, PortfolioEvolution> evolutionByTag = {};

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
  }) async => throw UnimplementedError();

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
  }) async => throw UnimplementedError();

  @override
  Future<void> deleteManualTrade(String id) async => throw UnimplementedError();

  @override
  Future<PortfolioSummary> getSummary({String? tagId}) async => throw UnimplementedError();

  @override
  Future<({List<CustodyPosition> positions, DateTime? metaPriceUpdatedAt, int untaggedAssetsCount, List<String> untaggedTickers})>
      getCustodyPositions({String? tagId}) async => throw UnimplementedError();

  @override
  Future<PortfolioEvolution> getPortfolioEvolution({String? tagId}) async {
    requestedTags.add(tagId);
    if (shouldThrow) {
      throw Exception('Erro ao buscar evolução do portfólio');
    }
    if (evolutionCompleter != null) {
      return evolutionCompleter!.future;
    }
    if (evolutionByTag.containsKey(tagId)) {
      return evolutionByTag[tagId]!;
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

    testWidgets('deve filtrar histórico e exibir ActiveTagLensBadge ao alternar tag na lente analítica', (tester) async {
      mockRepository.evolutionByTag = {
        null: const PortfolioEvolution(
          monthlyData: [
            MonthlyEvolution(month: '2026-04', totalContributions: 1500.0, totalDividends: 50.0),
          ],
        ),
        'tag-div': const PortfolioEvolution(
          monthlyData: [
            MonthlyEvolution(month: '2026-05', totalContributions: 400.0, totalDividends: 90.0),
          ],
        ),
      };
      final tagProvider = TagProvider(repository: _EvolutionTagRepository());
      await tagProvider.loadTags();

      await tester.pumpWidget(
        MultiProvider(
          providers: [
            ChangeNotifierProvider<PortfolioProvider>.value(value: provider),
            ChangeNotifierProvider<TagProvider>.value(value: tagProvider),
          ],
          child: const MaterialApp(home: EvolutionPage()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('analytical_lens_tabs')), findsOneWidget);
      expect(find.text('Abr/26'), findsOneWidget);
      expect(find.byKey(const Key('active_tag_lens_badge')), findsNothing);

      await tester.tap(find.byKey(const Key('tag_filter_tag-div')));
      await tester.pumpAndSettle();

      expect(find.text('Visualizando: Dividendos'), findsOneWidget);
      expect(find.text('Mai/26'), findsOneWidget);
      expect(mockRepository.requestedTags, [null, 'tag-div']);

      await tester.tap(find.byKey(const Key('active_tag_lens_clear_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('active_tag_lens_badge')), findsNothing);
      expect(mockRepository.requestedTags, [null, 'tag-div', null]);
    });
  });
}
