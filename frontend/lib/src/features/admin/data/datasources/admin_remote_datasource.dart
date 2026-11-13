import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../../../../core/api/api_client.dart';
import '../models/admin_dlq_message_model.dart';
import '../models/admin_observability_model.dart';
import '../models/admin_ticker_model.dart';

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

  Future<List<AdminTickerModel>> getTickers({String? query, String? assetClass}) async {
    final queryParams = <String, dynamic>{};
    if (query != null && query.trim().isNotEmpty) {
      queryParams['query'] = query.trim();
    }
    if (assetClass != null && assetClass.trim().isNotEmpty && assetClass != 'Todos') {
      queryParams['assetClass'] = assetClass.trim();
    }

    final queryStr = queryParams.entries.map((e) => '${e.key}=${Uri.encodeComponent(e.value.toString())}').join('&');
    final path = queryStr.isNotEmpty ? '/api/v1/admin/tickers?$queryStr' : '/api/v1/admin/tickers';
    debugPrint('[AdminRemoteDatasource] Requesting GET $path');

    try {
      final response = await apiClient.get(path);
      if (response.statusCode == 200) {
        return AdminTickerModel.fromJsonList(response.data);
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao carregar tickers mestre: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on GET $path: $e\n$stackTrace');
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
      throw Exception(detail ?? 'Erro de conexão ao buscar tickers.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on getTickers: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<AdminTickerModel> createTicker({
    required String ticker,
    required String name,
    required String assetClass,
    String? sector,
  }) async {
    const path = '/api/v1/admin/tickers';
    debugPrint('[AdminRemoteDatasource] Requesting POST $path for $ticker');

    try {
      final response = await apiClient.post(
        path,
        data: {
          'ticker': ticker,
          'name': name,
          'assetClass': assetClass,
          if (sector != null && sector.trim().isNotEmpty) 'sector': sector.trim(),
        },
      );
      if (response.statusCode == 201 && response.data is Map && response.data['data'] is Map) {
        final data = Map<String, dynamic>.from(response.data['data'] as Map);
        return AdminTickerModel.fromJson(data);
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao cadastrar ticker: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on POST $path: $e\n$stackTrace');
      if (e.response?.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro ao cadastrar ticker.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on createTicker: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<AdminTickerModel> updateTicker({
    required String id,
    required String name,
    required String assetClass,
    String? sector,
  }) async {
    final path = '/api/v1/admin/tickers/$id';
    debugPrint('[AdminRemoteDatasource] Requesting PUT $path');

    try {
      final response = await apiClient.put(
        path,
        data: {
          'name': name,
          'assetClass': assetClass,
          'sector': (sector != null && sector.trim().isNotEmpty) ? sector.trim() : null,
        },
      );
      if (response.statusCode == 200 && response.data is Map && response.data['data'] is Map) {
        final data = Map<String, dynamic>.from(response.data['data'] as Map);
        return AdminTickerModel.fromJson(data);
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao atualizar ticker: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on PUT $path: $e\n$stackTrace');
      if (e.response?.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro ao atualizar ticker.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on updateTicker: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<void> deleteTicker(String id) async {
    final path = '/api/v1/admin/tickers/$id';
    debugPrint('[AdminRemoteDatasource] Requesting DELETE $path');

    try {
      final response = await apiClient.delete(path);
      if (response.statusCode == 204 || response.statusCode == 200) {
        return;
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao remover ticker: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on DELETE $path: $e\n$stackTrace');
      if (e.response?.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro ao remover ticker.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on deleteTicker: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<void> clearMarketDataCache() async {
    const path = '/api/v1/admin/tickers/cache/clear';
    debugPrint('[AdminRemoteDatasource] Requesting POST $path');

    try {
      final response = await apiClient.post(path);
      if (response.statusCode == 200) {
        return;
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao limpar cache: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on POST $path: $e\n$stackTrace');
      if (e.response?.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro ao limpar cache de Market Data.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on clearMarketDataCache: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<void> evictTickerCache(String ticker) async {
    final cleanTicker = ticker.trim().toUpperCase();
    final encodedTicker = Uri.encodeComponent(cleanTicker);
    final path = '/api/v1/admin/tickers/$encodedTicker/cache/evict';
    debugPrint('[AdminRemoteDatasource] Requesting POST $path');

    try {
      final response = await apiClient.post(path);
      if (response.statusCode == 200) {
        return;
      }
      if (response.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      throw Exception('Erro ao invalidar cache do ticker $cleanTicker: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR on POST $path: $e\n$stackTrace');
      if (e.response?.statusCode == 403) {
        throw const AdminAccessDeniedException();
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro ao invalidar cache do ticker $cleanTicker.');
    } catch (e, stackTrace) {
      debugPrint('[AdminRemoteDatasource] ERROR unexpected on evictTickerCache: $e\n$stackTrace');
      rethrow;
    }
  }
}
