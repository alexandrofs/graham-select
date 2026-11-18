import '../../domain/entities/open_finance_connection.dart';
import '../../domain/entities/open_finance_institution.dart';
import '../../domain/entities/open_finance_sync_result.dart';
import '../../domain/repositories/open_finance_repository.dart';
import '../datasources/open_finance_remote_data_source.dart';

class OpenFinanceRepositoryImpl implements OpenFinanceRepository {
  final OpenFinanceRemoteDataSource remoteDataSource;

  OpenFinanceRepositoryImpl({required this.remoteDataSource});

  @override
  Future<List<OpenFinanceInstitution>> getInstitutions() =>
      remoteDataSource.getInstitutions();

  @override
  Future<List<OpenFinanceConnection>> getConnections() =>
      remoteDataSource.getConnections();

  @override
  Future<Map<String, dynamic>> initiateConnection({
    required String institutionId,
    String? profileId,
    String? redirectUri,
  }) =>
      remoteDataSource.initiateConnection(
        institutionId: institutionId,
        profileId: profileId,
        redirectUri: redirectUri,
      );

  @override
  Future<OpenFinanceConnection> completeConsentCallback({
    required String connectionId,
    required String code,
    required String state,
    String? redirectUri,
  }) =>
      remoteDataSource.completeConsentCallback(
        connectionId: connectionId,
        code: code,
        state: state,
        redirectUri: redirectUri,
      );

  @override
  Future<OpenFinanceSyncResult> syncConnection(String connectionId) =>
      remoteDataSource.syncConnection(connectionId);

  @override
  Future<void> revokeConnection(String connectionId) =>
      remoteDataSource.revokeConnection(connectionId);
}
