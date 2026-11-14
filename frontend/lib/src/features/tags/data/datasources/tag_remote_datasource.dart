import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../../../../core/api/api_client.dart';
import '../models/portfolio_tag_model.dart';

class TagRemoteDataSource {
  final ApiClient apiClient;

  const TagRemoteDataSource({required this.apiClient});

  Future<List<PortfolioTagModel>> fetchTags() async {
    const path = '/api/v1/tags';
    debugPrint('[TagRemoteDataSource] Requesting GET $path');
    try {
      final response = await apiClient.get(path);
      if (response.statusCode == 200) {
        final body = _asStringMap(response.data, 'Resposta inválida ao listar tags.');
        return _parseTagList(body['data'], 'Resposta inválida ao listar tags.');
      }
      throw Exception('Erro ao listar tags: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR on GET $path: $e\n$stackTrace');
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR unexpected on fetchTags: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<List<PortfolioTagModel>> fetchAssetTags(String ticker) async {
    final encodedTicker = _encodeTicker(ticker);
    final path = '/api/v1/tags/assets/$encodedTicker';
    debugPrint('[TagRemoteDataSource] Requesting GET $path');
    try {
      final response = await apiClient.get(path);
      if (response.statusCode == 200) {
        final body = _asStringMap(response.data, 'Resposta inválida ao buscar tags do ativo.');
        final data = _asStringMap(body['data'], 'Resposta inválida ao buscar tags do ativo.');
        return _parseTagList(data['tags'], 'Resposta inválida ao buscar tags do ativo.');
      }
      throw Exception('Erro ao buscar tags do ativo: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR on GET $path: $e\n$stackTrace');
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR unexpected on fetchAssetTags: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<PortfolioTagModel> createTag({required String name, String? color}) async {
    const path = '/api/v1/tags';
    debugPrint('[TagRemoteDataSource] Requesting POST $path');
    try {
      final payload = <String, dynamic>{
        'name': name.trim(),
        if (color != null && color.trim().isNotEmpty) 'color': color.trim(),
      };
      final response = await apiClient.post(path, data: payload);
      if (response.statusCode == 201 || response.statusCode == 200) {
        final body = _asStringMap(response.data, 'Resposta inválida ao criar tag.');
        final data = _asStringMap(body['data'], 'Resposta inválida ao criar tag.');
        return PortfolioTagModel.fromJson(data);
      }
      throw Exception('Erro ao criar tag: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR on POST $path: $e\n$stackTrace');
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR unexpected on createTag: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<PortfolioTagModel> updateTag({
    required String tagId,
    required String name,
    String? color,
  }) async {
    final encodedTagId = _encodeTagId(tagId);
    final path = '/api/v1/tags/$encodedTagId';
    debugPrint('[TagRemoteDataSource] Requesting PUT $path');
    try {
      final payload = <String, dynamic>{
        'name': name.trim(),
        if (color != null && color.trim().isNotEmpty) 'color': color.trim(),
      };
      final response = await apiClient.put(path, data: payload);
      if (response.statusCode == 200) {
        final body = _asStringMap(response.data, 'Resposta inválida ao atualizar tag.');
        final data = _asStringMap(body['data'], 'Resposta inválida ao atualizar tag.');
        return PortfolioTagModel.fromJson(data);
      }
      throw Exception('Erro ao atualizar tag: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR on PUT $path: $e\n$stackTrace');
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR unexpected on updateTag: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<void> deleteTag(String tagId) async {
    final encodedTagId = _encodeTagId(tagId);
    final path = '/api/v1/tags/$encodedTagId';
    debugPrint('[TagRemoteDataSource] Requesting DELETE $path');
    try {
      final response = await apiClient.delete(path);
      if (response.statusCode == 204 || response.statusCode == 200) {
        return;
      }
      throw Exception('Erro ao excluir tag: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR on DELETE $path: $e\n$stackTrace');
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR unexpected on deleteTag: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<List<PortfolioTagModel>> setAssetTags({
    required String ticker,
    required List<String> tagIds,
  }) async {
    final encodedTicker = _encodeTicker(ticker);
    final path = '/api/v1/tags/assets/$encodedTicker';
    debugPrint('[TagRemoteDataSource] Requesting PUT $path');
    try {
      final response = await apiClient.put(path, data: {'tagIds': tagIds});
      if (response.statusCode == 200) {
        final body = _asStringMap(response.data, 'Resposta inválida ao associar tags ao ativo.');
        final data = _asStringMap(body['data'], 'Resposta inválida ao associar tags ao ativo.');
        return _parseTagList(data['tags'], 'Resposta inválida ao associar tags ao ativo.');
      }
      throw Exception('Erro ao associar tags ao ativo: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR on PUT $path: $e\n$stackTrace');
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR unexpected on setAssetTags: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<void> assignAssetToTag({
    required String tagId,
    required String ticker,
  }) async {
    final encodedTagId = _encodeTagId(tagId);
    final encodedTicker = _encodeTicker(ticker);
    final path = '/api/v1/tags/$encodedTagId/assets/$encodedTicker';
    debugPrint('[TagRemoteDataSource] Requesting POST $path');
    try {
      final response = await apiClient.post(path);
      if (response.statusCode == 201 || response.statusCode == 200) {
        return;
      }
      throw Exception('Erro ao associar ativo à tag: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR on POST $path: $e\n$stackTrace');
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR unexpected on assignAssetToTag: $e\n$stackTrace');
      rethrow;
    }
  }

  Future<void> unassignAssetFromTag({
    required String tagId,
    required String ticker,
  }) async {
    final encodedTagId = _encodeTagId(tagId);
    final encodedTicker = _encodeTicker(ticker);
    final path = '/api/v1/tags/$encodedTagId/assets/$encodedTicker';
    debugPrint('[TagRemoteDataSource] Requesting DELETE $path');
    try {
      final response = await apiClient.delete(path);
      if (response.statusCode == 204 || response.statusCode == 200) {
        return;
      }
      throw Exception('Erro ao desassociar ativo da tag: ${response.statusCode}');
    } on DioException catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR on DELETE $path: $e\n$stackTrace');
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint('[TagRemoteDataSource] ERROR unexpected on unassignAssetFromTag: $e\n$stackTrace');
      rethrow;
    }
  }

  String _encodeTagId(String tagId) {
    final trimmed = tagId.trim();
    if (trimmed.isEmpty) {
      final error = Exception('Identificador da tag é obrigatório.');
      debugPrint('[TagRemoteDataSource] ERROR invalid tagId: $error');
      throw error;
    }
    return Uri.encodeComponent(trimmed);
  }

  String _encodeTicker(String ticker) {
    final normalized = ticker.trim().toUpperCase();
    if (normalized.isEmpty) {
      final error = Exception('Ticker do ativo é obrigatório.');
      debugPrint('[TagRemoteDataSource] ERROR invalid ticker: $error');
      throw error;
    }
    return Uri.encodeComponent(normalized);
  }

  Map<String, dynamic> _asStringMap(Object? raw, String errorMessage) {
    if (raw is! Map) {
      throw Exception(errorMessage);
    }
    return Map<String, dynamic>.from(raw);
  }

  List<PortfolioTagModel> _parseTagList(Object? rawList, String errorMessage) {
    if (rawList is! List) {
      throw Exception(errorMessage);
    }
    return rawList
        .whereType<Map>()
        .map((e) => PortfolioTagModel.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  Exception _mapDioException(DioException e) {
    final responseData = e.response?.data;
    String? detail;
    if (responseData is Map) {
      for (final key in const ['detail', 'message', 'title']) {
        final candidate = responseData[key]?.toString().trim();
        if (candidate != null && candidate.isNotEmpty) {
          detail = candidate;
          break;
        }
      }
    } else if (responseData is String && responseData.trim().isNotEmpty) {
      detail = responseData.trim();
    }
    return Exception(detail ?? 'Erro de comunicação com o servidor.');
  }
}
