import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/security/privacy_provider.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/portfolio/domain/entities/monthly_evolution.dart';
import 'package:frontend/src/features/portfolio/domain/entities/portfolio_summary.dart';
import 'package:frontend/src/features/portfolio/domain/entities/trade.dart';
import 'package:frontend/src/features/portfolio/domain/repositories/portfolio_repository.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/presentation/widgets/asset_allocation_donut_chart.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

class _StubPortfolioRepository implements PortfolioRepository {
  final List<CustodyPosition> positions;

  _StubPortfolioRepository(this.positions);

  @override
  Future<({List<CustodyPosition> positions, DateTime? metaPriceUpdatedAt, int untaggedAssetsCount, List<String> untaggedTickers})>
      getCustodyPositions({String? tagId}) async => (
            positions: positions,
            metaPriceUpdatedAt: null,
            untaggedAssetsCount: 0,
            untaggedTickers: const <String>[],
          );

  @override
  Future<PortfolioSummary> getSummary({String? tagId}) async =>
      throw UnimplementedError();

  @override
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
  }) async =>
      throw UnimplementedError();

  @override
  Future<PortfolioEvolution> getPortfolioEvolution({String? tagId}) async =>
      throw UnimplementedError();
}

void main() {
  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
  });

  testWidgets('AssetAllocationDonutChart mascara R\$ e mantém percentuais no Modo Privacidade', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    try {
      final repo = _StubPortfolioRepository(const [
        CustodyPosition(
          ticker: 'ITUB4',
          quantity: 100,
          averagePrice: 30,
          currentPrice: 40,
          marketValue: 4000,
          gainLossPercentage: 10,
          priceSource: 'LIVE',
          assetClass: 'Ações',
        ),
        CustodyPosition(
          ticker: 'HGLG11',
          quantity: 10,
          averagePrice: 100,
          currentPrice: 100,
          marketValue: 1000,
          gainLossPercentage: 5,
          priceSource: 'LIVE',
          assetClass: 'FIIs',
        ),
      ]);

      final portfolioProvider = PortfolioProvider(repo);
      await portfolioProvider.loadCustodyPositions();
      final privacyProvider = PrivacyProvider(initialPrivacyMode: true);

      await tester.pumpWidget(
        MultiProvider(
          providers: [
            ChangeNotifierProvider<PortfolioProvider>.value(value: portfolioProvider),
            ChangeNotifierProvider<PrivacyProvider>.value(value: privacyProvider),
          ],
          child: const MaterialApp(
            home: Scaffold(
              body: SingleChildScrollView(child: AssetAllocationDonutChart()),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Centro do donut + 2 fatias na legenda
      expect(find.text(PrivacyProvider.maskedCurrencyValue), findsNWidgets(3));
      expect(find.textContaining('5.000,00'), findsNothing);
      expect(find.textContaining('80,0%'), findsOneWidget);
      expect(find.textContaining('20,0%'), findsOneWidget);
      expect(
        find.bySemanticsLabel(PrivacyProvider.maskedSemanticsLabel),
        findsNWidgets(3),
      );
    } finally {
      semantics.dispose();
    }
  });
}
