import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/dashboard/presentation/pages/dashboard_page.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/data/models/portfolio_summary_model.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';

import 'dashboard_page_test.mocks.dart';

@GenerateMocks([PortfolioProvider, ProfileProvider])
void main() {
  late MockPortfolioProvider mockPortfolioProvider;
  late MockProfileProvider mockProfileProvider;

  setUp(() {
    mockPortfolioProvider = MockPortfolioProvider();
    mockProfileProvider = MockProfileProvider();

    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.initial);
    when(mockPortfolioProvider.summary).thenReturn(null);
    when(mockPortfolioProvider.errorMessage).thenReturn(null);
    
    // Stubs da custódia para evitar MissingStubError
    when(mockPortfolioProvider.custodyStatus).thenReturn(PortfolioStatus.initial);
    when(mockPortfolioProvider.custodyPositions).thenReturn([]);
    when(mockPortfolioProvider.rawCustodyPositions).thenReturn([]);
    when(mockPortfolioProvider.selectedAssetClassFilter).thenReturn(null);
    when(mockPortfolioProvider.custodyError).thenReturn(null);
    when(mockPortfolioProvider.loadCustodyPositions()).thenAnswer((_) async {});
  });

  Widget createWidgetUnderTest() {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<PortfolioProvider>.value(value: mockPortfolioProvider),
        ChangeNotifierProvider<ProfileProvider>.value(value: mockProfileProvider),
      ],
      child: const MaterialApp(home: DashboardPage()),
    );
  }

  testWidgets('should render all 4 KPI cards when summary is loaded', (tester) async {
    final summary = PortfolioSummaryModel(
      totalEquity: 50000.0,
      grossYieldPercentage: 12.5,
      accumulatedDividends: 1200.0,
      monthlyProjection: 450.0,
    );

    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.success);
    when(mockPortfolioProvider.summary).thenReturn(summary);

    await tester.pumpWidget(createWidgetUnderTest());

    expect(find.text('Patrimônio Total'), findsOneWidget);
    expect(find.textContaining('50.000,00'), findsOneWidget);
    expect(find.text('Rendimento Bruto'), findsOneWidget);
    expect(find.text('12,5%'), findsOneWidget);
    expect(find.text('Dividendos Acumulados'), findsOneWidget);
    expect(find.textContaining('1.200,00'), findsOneWidget);
    expect(find.text('Projeção Mensal'), findsOneWidget);
    expect(find.textContaining('450,00'), findsOneWidget);
  });

  testWidgets('should show error state and retry button', (tester) async {
    when(mockPortfolioProvider.status).thenReturn(PortfolioStatus.error);
    when(mockPortfolioProvider.errorMessage).thenReturn('Connection Timeout');

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pump(); // Aguarda o PostFrameCallback do initState

    expect(find.textContaining('Erro ao carregar resumo: Connection Timeout'), findsOneWidget);
    expect(find.text('Tentar novamente'), findsOneWidget);

    await tester.tap(find.text('Tentar novamente'));
    // É chamado uma vez no initState e outra no clique do botão
    verify(mockPortfolioProvider.loadSummary()).called(2);
  });
}
