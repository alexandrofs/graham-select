import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../../../../core/api/api_client.dart';
import '../models/admin_dlq_message_model.dart';
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

  Future<List<AdminDlqMessageModel>> getDlqFailures({String status = 'PENDING'}) async {
    debugPrint('[AdminRemoteDatasource] Requesting GET /api/v1/admin/dlq?status=$status');
    try {
      final response = await apiClient.get('/api/v1/admin/dlq?status=$status');
      if (response.statusCode == 200) {
        return AdminDlqMessageModel.fromJsonList(response.data);
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao carregar falhas da DLQ: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on GET /api/v1/admin/dlq: $e\n$stackTrace');
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
      throw Exception(detail ?? 'Erro de conexão com o servidor ao buscar falhas da DLQ.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on getDlqFailures: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<void> reprocessDlqMessage(int id) async {
    debugPrint('[AdminRemoteDatasource] Requesting POST /api/v1/admin/dlq/$id/reprocess');
    try {
      final response = await apiClient.post('/api/v1/admin/dlq/$id/reprocess');
      if (response.statusCode == 200) {
        if (response.data is Map && response.data['data'] is Map) {
          final data = response.data['data'] as Map;
          final status = data['status']?.toString().toUpperCase();
          if (status == 'FAILED') {
            final msg = data['message']?.toString() ?? 'Falha ao reprocessar mensagem.';
            throw Exception(msg);
          }
        }
        return;
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao reprocessar mensagem: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on POST /api/v1/admin/dlq/$id/reprocess: $e\n$stackTrace');
      if (e.response?.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro ao reprocessar mensagem DLQ.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on reprocessDlqMessage: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> reprocessBatchDlqMessages(List<int> ids) async {
    debugPrint('[AdminRemoteDatasource] Requesting POST /api/v1/admin/dlq/reprocess-batch with ${ids.length} items');
    try {
      final response = await apiClient.post(
        '/api/v1/admin/dlq/reprocess-batch',
        data: {'ids': ids},
      );
      if (response.statusCode == 200 && response.data is Map<String, dynamic>) {
        final data = response.data as Map<String, dynamic>;
        if (data.containsKey('data') && data['data'] is Map<String, dynamic>) {
          return data['data'] as Map<String, dynamic>;
        }
        return data;
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao reprocessar em lote: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on POST /api/v1/admin/dlq/reprocess-batch: $e\n$stackTrace');
      if (e.response?.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro ao reprocessar em lote.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on reprocessBatchDlqMessages: $e\n$stackTrace');
      rethrow;
    }
  }
}
