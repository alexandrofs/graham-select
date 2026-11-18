import '../../../../core/api/api_client.dart';
import '../../domain/entities/open_finance_connection.dart';
import '../../domain/entities/open_finance_institution.dart';
import '../../domain/entities/open_finance_sync_result.dart';

abstract class OpenFinanceRemoteDataSource {
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

class OpenFinanceRemoteDataSourceImpl implements OpenFinanceRemoteDataSource {
  final ApiClient apiClient;

  OpenFinanceRemoteDataSourceImpl({required this.apiClient});

  @override
  Future<List<OpenFinanceInstitution>> getInstitutions() async {
    final response = await apiClient.get('/api/v1/open-finance/institutions');
    final List<dynamic> data = response.data as List<dynamic>;
    return data
        .map((json) => OpenFinanceInstitution.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<List<OpenFinanceConnection>> getConnections() async {
    final response = await apiClient.get('/api/v1/open-finance/connections');
    final List<dynamic> data = response.data as List<dynamic>;
    return data
        .map((json) => OpenFinanceConnection.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<Map<String, dynamic>> initiateConnection({
    required String institutionId,
    String? profileId,
    String? redirectUri,
  }) async {
    final Map<String, dynamic> body = {
      'institutionId': institutionId,
    };
    if (profileId != null) {
      body['profileId'] = profileId;
    }
    final path = redirectUri != null
        ? '/api/v1/open-finance/connections?redirectUri=${Uri.encodeComponent(redirectUri)}'
        : '/api/v1/open-finance/connections';
    final response = await apiClient.post(path, data: body);
    return response.data as Map<String, dynamic>;
  }

  @override
  Future<OpenFinanceConnection> completeConsentCallback({
    required String connectionId,
    required String code,
    required String state,
    String? redirectUri,
  }) async {
    final Map<String, dynamic> body = {
      'connectionId': connectionId,
      'code': code,
      'state': state,
    };
    final path = redirectUri != null
        ? '/api/v1/open-finance/connections/callback?redirectUri=${Uri.encodeComponent(redirectUri)}'
        : '/api/v1/open-finance/connections/callback';
    final response = await apiClient.post(path, data: body);
    return OpenFinanceConnection.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<OpenFinanceSyncResult> syncConnection(String connectionId) async {
    final response = await apiClient.post('/api/v1/open-finance/connections/$connectionId/sync');
    return OpenFinanceSyncResult.fromJson(response.data as Map<String, dynamic>);
  }

  @override
  Future<void> revokeConnection(String connectionId) async {
    await apiClient.delete('/api/v1/open-finance/connections/$connectionId');
  }
}
