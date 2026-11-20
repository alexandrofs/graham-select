import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/tax/domain/entities/annual_tax_summary.dart';
import 'package:frontend/src/features/tax/domain/entities/monthly_tax_calculation.dart';
import 'package:frontend/src/features/tax/domain/entities/monthly_trade_operation.dart';
import 'package:frontend/src/features/tax/domain/entities/tax_declaration_item.dart';
import 'package:frontend/src/features/tax/domain/entities/tax_declaration_report.dart';
import 'package:frontend/src/features/tax/domain/repositories/tax_repository.dart';
import 'package:frontend/src/features/tax/presentation/pages/tax_declaration_page.dart';
import 'package:frontend/src/features/tax/presentation/providers/tax_provider.dart';
import 'package:provider/provider.dart';

class MockTaxRepository implements TaxRepository {
  TaxDeclarationReport? reportToReturn;
  AnnualTaxSummary? monthlySummaryToReturn;
  List<int> availableYearsToReturn = [2025, 2024];
  String csvToReturn = 'csv content';
  String monthlyCsvToReturn = 'monthly csv content';

  @override
  Future<TaxDeclarationReport> getBensEDireitos({int? year, String? profileId}) async {
    return reportToReturn ??
        const TaxDeclarationReport(
          year: 2025,
          previousYear: 2024,
          previousYearTotal: 0,
          currentYearTotal: 0,
          netVariation: 0,
          percentageVariation: 0,
          items: [],
        );
  }

  @override
  Future<List<int>> getAvailableYears({String? profileId}) async {
    return availableYearsToReturn;
  }

  @override
  Future<String> exportBensEDireitosCsv({int? year, String? profileId}) async {
    return csvToReturn;
  }

  @override
  Future<AnnualTaxSummary> getMonthlyCapitalGains({int? year, String? profileId}) async {
    return monthlySummaryToReturn ?? AnnualTaxSummary.empty(year ?? 2025);
  }

  @override
  Future<String> exportMonthlyCapitalGainsCsv({int? year, String? profileId}) async {
    return monthlyCsvToReturn;
  }
}

void main() {
  late MockTaxRepository repository;

  setUp(() {
    repository = MockTaxRepository();
  });

  Widget createWidgetUnderTest() {
    return MaterialApp(
      home: ChangeNotifierProvider(
        create: (_) => TaxProvider(repository: repository),
        child: const TaxDeclarationPage(),
      ),
    );
  }

  testWidgets('Deve renderizar cabeçalho e estado vazio quando não há itens', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    expect(find.text('Informe de Bens e Direitos'), findsOneWidget);
    expect(find.byKey(const Key('tax_empty_state')), findsOneWidget);
    expect(find.byKey(const Key('export_csv_button')), findsOneWidget);
  });

  testWidgets('Deve renderizar cartões de totais e item quando há ativos', (tester) async {
    repository.reportToReturn = const TaxDeclarationReport(
      year: 2025,
      previousYear: 2024,
      previousYearTotal: 7000.0,
      currentYearTotal: 5250.0,
      netVariation: -1750.0,
      percentageVariation: -25.0,
      items: [
        TaxDeclarationItem(
          ticker: 'PETR4',
          companyName: 'PETROLEO BRASILEIRO S.A. PETROBRAS',
          cnpj: '33.000.167/0001-01',
          groupCode: '03',
          itemCode: '01',
          groupDescription: 'Participações societárias',
          locationCode: '105',
          quantity: 150.0,
          averagePrice: 35.0,
          previousYearValue: 7000.0,
          currentYearValue: 5250.0,
          discrimination: r'150 ações de PETROLEO BRASILEIRO S.A. PETROBRAS (PETR4), CNPJ: 33.000.167/0001-01. Custodiado na corretora XP Investimentos. Custo total de aquisição em 31/12/2025: R$ 5.250,00 (Preço médio de aquisição: R$ 35,00).',
          liquidated: false,
        ),
      ],
    );

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    expect(find.text('Posição Patrimonial Consolidada'), findsOneWidget);
    expect(find.byKey(const Key('tax_group_header_03')), findsOneWidget);
    expect(find.text('PETR4'), findsOneWidget);
    expect(find.text('PETROLEO BRASILEIRO S.A. PETROBRAS'), findsOneWidget);
    expect(find.text('CNPJ: 33.000.167/0001-01'), findsOneWidget);
    expect(find.byKey(const Key('copy_discrimination_PETR4')), findsOneWidget);

    // Clica no botão de copiar discriminação
    await tester.ensureVisible(find.byKey(const Key('copy_discrimination_PETR4')));
    await tester.tap(find.byKey(const Key('copy_discrimination_PETR4')));
    await tester.pumpAndSettle();

    expect(find.text('Discriminação de PETR4 copiada com sucesso!'), findsOneWidget);
  });

  testWidgets('Deve alternar para aba de Apuração Mensal & DARF e renderizar cards de apuração', (tester) async {
    final marchCalc = MonthlyTaxCalculation(
      month: 3,
      year: 2025,
      totalEquitySales: 4000.0,
      isEquityExempt: true,
      exemptProfit: 1000.0,
      commonTaxableProfit: 0.0,
      commonLossCompensated: 0.0,
      commonLossCarryforward: 0.0,
      commonTaxDue: 0.0,
      fiiTaxableProfit: 0.0,
      fiiLossCompensated: 0.0,
      fiiLossCarryforward: 0.0,
      fiiTaxDue: 0.0,
      totalTaxDue: 0.0,
      accumulatedTaxUnder10: 0.0,
      darfStatus: DarfStatus.exempt,
      darfAmount: 0.0,
      darfDueDate: null,
      operations: [
        MonthlyTradeOperation(
          ticker: 'PETR4',
          tradeDate: DateTime(2025, 3, 15),
          side: 'VENDA',
          quantity: 100,
          price: 40.0,
          averagePrice: 30.0,
          totalSaleValue: 4000.0,
          netProfitOrLoss: 1000.0,
          category: 'SWING_TRADE_EQUITY',
        ),
      ],
    );

    final mayCalc = MonthlyTaxCalculation(
      month: 5,
      year: 2025,
      totalEquitySales: 30000.0,
      isEquityExempt: false,
      exemptProfit: 0.0,
      commonTaxableProfit: 10000.0,
      commonLossCompensated: 0.0,
      commonLossCarryforward: 0.0,
      commonTaxDue: 1500.0,
      fiiTaxableProfit: 0.0,
      fiiLossCompensated: 0.0,
      fiiLossCarryforward: 0.0,
      fiiTaxDue: 0.0,
      totalTaxDue: 1500.0,
      accumulatedTaxUnder10: 0.0,
      darfStatus: DarfStatus.due,
      darfAmount: 1500.0,
      darfDueDate: DateTime(2025, 6, 30),
      operations: [],
    );

    repository.monthlySummaryToReturn = AnnualTaxSummary(
      year: 2025,
      monthlyCalculations: [marchCalc, mayCalc],
      totalTaxDueYear: 1500.0,
      totalSalesYear: 34000.0,
      totalExemptProfitYear: 1000.0,
      totalTaxableProfitYear: 10000.0,
      finalCommonLossCarryforward: 0.0,
      finalFiiLossCarryforward: 0.0,
    );

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    // Clica na Tab 2
    expect(find.byKey(const Key('tab_apuracao_mensal')), findsOneWidget);
    await tester.tap(find.byKey(const Key('tab_apuracao_mensal')));
    await tester.pumpAndSettle();

    expect(find.text('Apuração Mensal & DARF'), findsNWidgets(2));
    expect(find.text('Resumo Anual de Apuração Fiscal (2025)'), findsOneWidget);
    expect(find.text('Total Imposto Devido'), findsOneWidget);
    expect(find.text('Volume Total Alienado'), findsOneWidget);
    expect(find.text('Março/2025'), findsOneWidget);
    expect(find.text('Maio/2025'), findsOneWidget);
    expect(find.text('DARF a Pagar'), findsOneWidget);
    expect(find.text('Guia DARF - Código 6015'), findsOneWidget);
    expect(find.text('Copiar Dados do DARF'), findsOneWidget);

    // Clica em Copiar Dados do DARF
    await tester.ensureVisible(find.text('Copiar Dados do DARF'));
    await tester.tap(find.text('Copiar Dados do DARF'));
    await tester.pumpAndSettle();
    expect(find.text('Dados da Guia DARF copiados para a área de transferência!'), findsOneWidget);
  });
}
