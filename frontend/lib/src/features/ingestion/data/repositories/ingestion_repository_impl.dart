import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../../domain/entities/ingestion_audit.dart';
import '../../domain/repositories/ingestion_repository.dart';

class IngestionRepositoryImpl implements IngestionRepository {
  final ApiClient apiClient;

  IngestionRepositoryImpl(this.apiClient);

  @override
  Future<List<IngestionAudit>> getHistory() async {
    try {
      final response = await apiClient.get('/api/v1/ingestion/history');
      final List<dynamic> data = response.data;
      return data.map((json) => IngestionAudit.fromJson(json)).toList();
    } on DioException catch (e) {
      throw Exception('Failed to load ingestion history: ${e.message}');
    }
  }
}
