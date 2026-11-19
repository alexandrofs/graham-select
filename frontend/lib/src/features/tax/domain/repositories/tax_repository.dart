import '../entities/tax_declaration_report.dart';

abstract class TaxRepository {
  Future<TaxDeclarationReport> getBensEDireitos({int? year, String? profileId});
  Future<List<int>> getAvailableYears({String? profileId});
  Future<String> exportBensEDireitosCsv({int? year, String? profileId});
}
