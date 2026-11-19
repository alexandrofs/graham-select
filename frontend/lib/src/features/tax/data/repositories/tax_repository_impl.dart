import '../../domain/entities/tax_declaration_report.dart';
import '../../domain/repositories/tax_repository.dart';
import '../datasources/tax_remote_data_source.dart';

class TaxRepositoryImpl implements TaxRepository {
  final TaxRemoteDataSource remoteDataSource;

  TaxRepositoryImpl({required this.remoteDataSource});

  @override
  Future<TaxDeclarationReport> getBensEDireitos({int? year, String? profileId}) async {
    final json = await remoteDataSource.getBensEDireitos(year: year, profileId: profileId);
    return TaxDeclarationReport.fromJson(json);
  }

  @override
  Future<List<int>> getAvailableYears({String? profileId}) {
    return remoteDataSource.getAvailableYears(profileId: profileId);
  }

  @override
  Future<String> exportBensEDireitosCsv({int? year, String? profileId}) {
    return remoteDataSource.exportBensEDireitosCsv(year: year, profileId: profileId);
  }
}
