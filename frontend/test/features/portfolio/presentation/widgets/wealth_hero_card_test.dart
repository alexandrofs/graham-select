import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/security/privacy_provider.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/portfolio/data/models/portfolio_summary_model.dart';
import 'package:frontend/src/features/portfolio/presentation/widgets/wealth_hero_card.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

void main() {
  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
  });

  final sampleSummary = PortfolioSummaryModel(
    totalEquity: 125430.55,
    grossYieldPercentage: 14.25,
    accumulatedDividends: 3200.0,
    monthlyProjection: 850.0,
  );

  testWidgets('exibe patrimônio consolidado, rentabilidade e aviso de delay 15 min B3', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: Scaffold(
          body: WealthHeroCard(
            summary: sampleSummary,
            priceUpdatedAt: DateTime(2026, 11, 27, 15, 30),
          ),
        ),
      ),
    );

    expect(find.byKey(const Key('wealth_hero_card')), findsOneWidget);
    expect(find.textContaining('125.430,55'), findsOneWidget);
    expect(find.textContaining('+14,25%'), findsOneWidget);
    expect(find.byKey(const Key('wealth_hero_delay_notice')), findsOneWidget);
    expect(find.textContaining('Delay de até 15 min B3'), findsOneWidget);
  });

  testWidgets('exibe aviso de fechamento D-1 / cache quando hasCachePrice=true', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: Scaffold(
          body: WealthHeroCard(
            summary: sampleSummary,
            hasCachePrice: true,
          ),
        ),
      ),
    );

    expect(find.textContaining('fechamento D-1'), findsOneWidget);
  });

  testWidgets('mascara R\$ com R\$ •••••• e oculta Semantics quando PrivacyMode ativo', (
    tester,
  ) async {
    final privacyProvider = PrivacyProvider(initialPrivacyMode: true);

    await tester.pumpWidget(
      ChangeNotifierProvider<PrivacyProvider>.value(
        value: privacyProvider,
        child: MaterialApp(
          theme: AppTheme.lightTheme,
          home: Scaffold(
            body: WealthHeroCard(summary: sampleSummary),
          ),
        ),
      ),
    );

    expect(find.text(PrivacyProvider.maskedCurrencyValue), findsOneWidget);
    expect(find.textContaining('125.430,55'), findsNothing);
    // Percentual permanece visível no Modo Privacidade
    expect(find.textContaining('+14,25%'), findsOneWidget);
    expect(
      find.bySemanticsLabel(PrivacyProvider.maskedSemanticsLabel),
      findsOneWidget,
    );
  });

  testWidgets('exibe skeleton quando isLoading=true e summary=null', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: const Scaffold(
          body: WealthHeroCard(summary: null, isLoading: true),
        ),
      ),
    );

    expect(find.byKey(const Key('wealth_hero_skeleton')), findsOneWidget);
  });

  testWidgets('trata totalEquity e grossYieldPercentage NaN com fallback seguro 0.0', (
    tester,
  ) async {
    final nanSummary = PortfolioSummaryModel(
      totalEquity: double.nan,
      grossYieldPercentage: double.nan,
      accumulatedDividends: 0,
      monthlyProjection: 0,
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: Scaffold(
          body: WealthHeroCard(summary: nanSummary),
        ),
      ),
    );

    expect(tester.takeException(), isNull);
    expect(find.textContaining('0,00'), findsWidgets);
  });
}
