import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../../../../core/api/api_client.dart';
import '../models/admin_observability_model.dart';

class AdminAccessDeniedException implements Exception {
  final String message;

  const AdminAccessDeniedException([
    this.message = 'Acesso restrito a administradores do sistema.',
  ]);

  @override
  String toString() => message;
}

class AdminRemoteDatasource {
  final ApiClient apiClient;

  AdminRemoteDatasource({required this.apiClient});

  Future<AdminObservabilityModel> fetchObservabilityMetrics() async {
    debugPrint('[AdminRemoteDatasource] Requesting GET /api/v1/admin/observability');
    try {
      final response = await apiClient.get('/api/v1/admin/observability');
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        return AdminObservabilityModel.fromJson(
          response.data as Map<String, dynamic>,
        );
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception(
        'Erro ao carregar métricas de observabilidade: ${response.statusCode}',
      );
    } on DioException catch (e, stackTrace) {
      debugPrint(
        '[AdminRemoteDatasource] ERROR on GET /api/v1/admin/observability: $e\n$stackTrace',
      );
      if (e.response?.statusCode == 403) {
        String? detail;
        final responseData = e.response?.data;
        if (responseData is Map) {
          detail = responseData['detail']?.toString();
        }
        throw AdminAccessDeniedException(
          detail ?? 'Acesso restrito a administradores do sistema.',
        );
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro de conexão com o servidor.');
    } catch (e, stackTrace) {
      debugPrint(
        '[AdminRemoteDatasource] ERROR unexpected: $e\n$stackTrace',
      );
      rethrow;
    }
  }
}
