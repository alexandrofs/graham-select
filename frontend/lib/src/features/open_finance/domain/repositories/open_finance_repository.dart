import '../entities/open_finance_connection.dart';
import '../entities/open_finance_institution.dart';
import '../entities/open_finance_sync_result.dart';

abstract class OpenFinanceRepository {
  Future<List<OpenFinanceInstitution>> getInstitutions();
  Future<List<OpenFinanceConnection>> getConnections();
  Future<Map<String, dynamic>> initiateConnection({
    required String institutionId,
    String? profileId,
    String? redirectUri,
  });
  Future<OpenFinanceConnection> completeConsentCallback({
    required String connectionId,
    required String code,
    required String state,
    String? redirectUri,
  });
  Future<OpenFinanceSyncResult> syncConnection(String connectionId);
  Future<void> revokeConnection(String connectionId);
}
