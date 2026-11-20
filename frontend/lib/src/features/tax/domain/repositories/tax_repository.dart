import '../entities/annual_tax_summary.dart';
import '../entities/tax_declaration_report.dart';

abstract class TaxRepository {
  Future<TaxDeclarationReport> getBensEDireitos({int? year, String? profileId});
  Future<List<int>> getAvailableYears({String? profileId});
  Future<String> exportBensEDireitosCsv({int? year, String? profileId});
  Future<AnnualTaxSummary> getMonthlyCapitalGains({int? year, String? profileId});
  Future<String> exportMonthlyCapitalGainsCsv({int? year, String? profileId});
}
