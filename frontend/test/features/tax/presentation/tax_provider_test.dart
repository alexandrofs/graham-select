import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/tax/domain/entities/annual_tax_summary.dart';
import 'package:frontend/src/features/tax/domain/entities/tax_declaration_item.dart';
import 'package:frontend/src/features/tax/domain/entities/tax_declaration_report.dart';
import 'package:frontend/src/features/tax/domain/repositories/tax_repository.dart';
import 'package:frontend/src/features/tax/presentation/providers/tax_provider.dart';

class FakeTaxRepository implements TaxRepository {
  TaxDeclarationReport? reportToReturn;
  AnnualTaxSummary? monthlySummaryToReturn;
  List<int> availableYearsToReturn = [];
  String csvToReturn = 'csv content';
  String monthlyCsvToReturn = 'monthly csv content';
  bool shouldThrow = false;
  int? lastYear;
  String? lastProfileId;

  @override
  Future<TaxDeclarationReport> getBensEDireitos({int? year, String? profileId}) async {
    if (shouldThrow) throw Exception('Network error');
    lastYear = year;
    lastProfileId = profileId;
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
    if (shouldThrow) throw Exception('Network error');
    lastProfileId = profileId;
    return availableYearsToReturn;
  }

  @override
  Future<String> exportBensEDireitosCsv({int? year, String? profileId}) async {
    if (shouldThrow) throw Exception('Network error');
    lastYear = year;
    lastProfileId = profileId;
    return csvToReturn;
  }

  @override
  Future<AnnualTaxSummary> getMonthlyCapitalGains({int? year, String? profileId}) async {
    if (shouldThrow) throw Exception('Network error');
    lastYear = year;
    lastProfileId = profileId;
    return monthlySummaryToReturn ?? AnnualTaxSummary.empty(year ?? 2025);
  }

  @override
  Future<String> exportMonthlyCapitalGainsCsv({int? year, String? profileId}) async {
    if (shouldThrow) throw Exception('Network error');
    lastYear = year;
    lastProfileId = profileId;
    return monthlyCsvToReturn;
  }
}

void main() {
  late FakeTaxRepository repository;
  late TaxProvider provider;

  setUp(() {
    repository = FakeTaxRepository();
    provider = TaxProvider(repository: repository);
  });

  group('TaxProvider Tests', () {
    test('Deve inicializar com valores padrão', () {
      expect(provider.isLoading, false);
      expect(provider.isExporting, false);
      expect(provider.errorMessage, isNull);
      expect(provider.report, isNull);
      expect(provider.availableYears, isEmpty);
      expect(provider.selectedYear, isNull);
      expect(provider.selectedProfileId, isNull);
    });

    test('Deve carregar anos disponíveis e relatório com sucesso na inicialização', () async {
      repository.availableYearsToReturn = [2025, 2024, 2023];
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
            companyName: 'PETROBRAS',
            cnpj: '33.000.167/0001-01',
            groupCode: '03',
            itemCode: '01',
            groupDescription: 'Participações societárias',
            locationCode: '105',
            quantity: 150.0,
            averagePrice: 35.0,
            previousYearValue: 7000.0,
            currentYearValue: 5250.0,
            discrimination: '150 ações de PETROBRAS (PETR4)...',
            liquidated: false,
          ),
        ],
      );

      await provider.init();

      expect(provider.isLoading, false);
      expect(provider.errorMessage, isNull);
      expect(provider.availableYears, [2025, 2024, 2023]);
      expect(provider.selectedYear, 2025);
      expect(provider.report, isNotNull);
      expect(provider.report!.items.length, 1);
      expect(provider.report!.items.first.ticker, 'PETR4');
    });

    test('Deve alternar de ano e recarregar relatório', () async {
      repository.availableYearsToReturn = [2025, 2024];
      await provider.init();

      await provider.setYear(2024);

      expect(provider.selectedYear, 2024);
      expect(repository.lastYear, 2024);
    });

    test('Deve alternar perfil familiar e recarregar relatório', () async {
      await provider.setProfileId('profile-dep-1');

      expect(provider.selectedProfileId, 'profile-dep-1');
      expect(repository.lastProfileId, 'profile-dep-1');
    });

    test('Deve registrar mensagem de erro quando o relatório falhar', () async {
      repository.shouldThrow = true;

      await provider.loadReport();

      expect(provider.isLoading, false);
      expect(provider.errorMessage, isNotNull);
      expect(provider.errorMessage, contains('Falha ao carregar o informe de Bens e Direitos'));
    });

    test('Falha no exportCsv não deve sobrescrever errorMessage do relatório', () async {
      repository.shouldThrow = true;
      final result = await provider.exportCsv();
      expect(result, isNull);
      expect(provider.isExporting, false);
      expect(provider.errorMessage, isNull);
    });

    test('Deve alternar aba ativa', () {
      expect(provider.activeTab, TaxTab.bensEDireitos);
      provider.setActiveTab(TaxTab.apuracaoMensal);
      expect(provider.activeTab, TaxTab.apuracaoMensal);
    });

    test('Deve carregar apuração mensal com sucesso', () async {
      repository.monthlySummaryToReturn = const AnnualTaxSummary(
        year: 2025,
        monthlyCalculations: [],
        totalTaxDueYear: 500.0,
        totalSalesYear: 10000.0,
        totalExemptProfitYear: 1000.0,
        totalTaxableProfitYear: 2500.0,
        finalCommonLossCarryforward: 0.0,
        finalFiiLossCarryforward: 0.0,
      );

      await provider.loadMonthlyCapitalGains();

      expect(provider.isLoading, false);
      expect(provider.errorMessage, isNull);
      expect(provider.monthlySummary, isNotNull);
      expect(provider.monthlySummary!.totalTaxDueYear, 500.0);
    });

    test('Falha no exportMonthlyCsv não deve quebrar estado', () async {
      repository.shouldThrow = true;
      final result = await provider.exportMonthlyCsv();
      expect(result, isNull);
      expect(provider.isExporting, false);
    });
  });
}
