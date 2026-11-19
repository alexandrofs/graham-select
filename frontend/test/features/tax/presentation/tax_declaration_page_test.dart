import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/tax/domain/entities/tax_declaration_item.dart';
import 'package:frontend/src/features/tax/domain/entities/tax_declaration_report.dart';
import 'package:frontend/src/features/tax/domain/repositories/tax_repository.dart';
import 'package:frontend/src/features/tax/presentation/pages/tax_declaration_page.dart';
import 'package:frontend/src/features/tax/presentation/providers/tax_provider.dart';
import 'package:provider/provider.dart';

class MockTaxRepository implements TaxRepository {
  TaxDeclarationReport? reportToReturn;
  List<int> availableYearsToReturn = [2025, 2024];
  String csvToReturn = 'csv content';

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
    expect(find.text('PETR4'), findsOneWidget);
    expect(find.text('PETROLEO BRASILEIRO S.A. PETROBRAS'), findsOneWidget);
    expect(find.text('CNPJ: 33.000.167/0001-01'), findsOneWidget);
    expect(find.byKey(const Key('copy_discrimination_PETR4')), findsOneWidget);

    // Clica no botão de copiar discriminação
    await tester.tap(find.byKey(const Key('copy_discrimination_PETR4')));
    await tester.pumpAndSettle();

    expect(find.text('Discriminação de PETR4 copiada com sucesso!'), findsOneWidget);
  });
}
