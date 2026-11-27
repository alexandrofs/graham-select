import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/security/privacy_provider.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/portfolio/presentation/widgets/macro_class_distribution_card.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

void main() {
  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
  });

  final samplePositions = <CustodyPosition>[
    const CustodyPosition(
      ticker: 'ITUB4',
      quantity: 100,
      averagePrice: 30,
      currentPrice: 35,
      marketValue: 4000,
      gainLossPercentage: 10,
      priceSource: 'LIVE',
      assetClass: 'Ações',
    ),
    const CustodyPosition(
      ticker: 'HGLG11',
      quantity: 10,
      averagePrice: 150,
      currentPrice: 160,
      marketValue: 2000,
      gainLossPercentage: 5,
      priceSource: 'LIVE',
      assetClass: 'FII',
    ),
    const CustodyPosition(
      ticker: 'TESOURO SELIC',
      quantity: 1,
      averagePrice: 2000,
      currentPrice: 2000,
      marketValue: 2000,
      gainLossPercentage: 2,
      priceSource: 'LIVE',
      assetClass: 'Tesouro Direto',
    ),
    const CustodyPosition(
      ticker: 'IVVB11',
      quantity: 5,
      averagePrice: 200,
      currentPrice: 200,
      marketValue: 1000,
      gainLossPercentage: 12,
      priceSource: 'LIVE',
      assetClass: 'ETF',
    ),
    const CustodyPosition(
      ticker: 'BTC',
      quantity: 1,
      averagePrice: 1000,
      currentPrice: 1000,
      marketValue: 1000,
      gainLossPercentage: 20,
      priceSource: 'LIVE',
      assetClass: 'Cripto',
    ),
  ];

  testWidgets('oculta card quando lista de posições está vazia', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: const Scaffold(
          body: MacroClassDistributionCard(positions: []),
        ),
      ),
    );

    expect(find.byKey(const Key('macro_class_distribution_card')), findsNothing);
  });

  testWidgets('agrupa nas 5 Macro Classes sem scroll horizontal e expande subclasses', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: Scaffold(
          body: SingleChildScrollView(
            child: MacroClassDistributionCard(
              positions: samplePositions,
              targetPercentages: const {'Ações BR': 35.0},
            ),
          ),
        ),
      ),
    );

    expect(find.text('Ações BR'), findsOneWidget);
    expect(find.text('FIIs'), findsOneWidget);
    expect(find.text('Renda Fixa / Tesouro'), findsOneWidget);
    expect(find.text('Internacional'), findsOneWidget);
    expect(find.text('Criptoativos'), findsOneWidget);
    expect(find.text('40,0%'), findsOneWidget);
    expect(find.text('Meta 35,0%'), findsOneWidget);

    // Expande subclasses de Internacional
    await tester.tap(find.byKey(const Key('macro_class_tile_internacional')));
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('macro_subclass_chip_internacional_ETF')),
      findsOneWidget,
    );
  });

  testWidgets('Modo Privacidade oculta R\$ e mantém percentuais visíveis', (
    tester,
  ) async {
    final privacyProvider = PrivacyProvider(initialPrivacyMode: true);

    await tester.pumpWidget(
      ChangeNotifierProvider<PrivacyProvider>.value(
        value: privacyProvider,
        child: MaterialApp(
          theme: AppTheme.lightTheme,
          home: Scaffold(
            body: SingleChildScrollView(
              child: MacroClassDistributionCard(positions: samplePositions),
            ),
          ),
        ),
      ),
    );

    expect(find.text(PrivacyProvider.maskedCurrencyValue), findsNWidgets(5));
    expect(find.textContaining('4.000,00'), findsNothing);
    expect(find.text('40,0%'), findsOneWidget);
  });

  testWidgets('mapeia ETF Renda Fixa corretamente, ignora Meta NaN e tolera largura estreita (< 3dp)', (
    tester,
  ) async {
    const etfRf = CustodyPosition(
      ticker: 'IMAB11',
      quantity: 10,
      averagePrice: 100,
      currentPrice: 100,
      marketValue: 1000,
      gainLossPercentage: 4,
      priceSource: 'LIVE',
      assetClass: 'ETF Renda Fixa',
    );

    expect(
      MacroAssetClass.fromRawAssetClass('ETF Renda Fixa'),
      MacroAssetClass.rendaFixa,
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: const Scaffold(
          body: SizedBox(
            width: 2.0,
            child: SingleChildScrollView(
              child: MacroClassDistributionCard(
                positions: [etfRf],
                targetPercentages: {'Renda Fixa / Tesouro': double.nan},
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.textContaining('Meta'), findsNothing);
  });
}
