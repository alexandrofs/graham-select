import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../../core/api/api_client.dart';
import '../../domain/entities/cerrado_macro_class.dart';
import '../models/cerrado_models.dart';

class CerradoDiagramRemoteDataSource {
  const CerradoDiagramRemoteDataSource({required this.apiClient});

  final ApiClient apiClient;

  static const String _templatesPath = '/api/v1/cerrado/templates';
  static const String _preferencesPath = '/api/v1/cerrado/preferences';

  Future<CerradoTemplatesCatalogModel> fetchTemplates() async {
    debugPrint('[CerradoDiagramRemoteDataSource] Requesting GET $_templatesPath');
    try {
      final response = await apiClient.get(_templatesPath);
      if (response.statusCode == 200) {
        final body = _asStringMap(
          response.data,
          'Resposta inválida ao carregar templates do Diagrama do Cerrado.',
        );
        return CerradoTemplatesCatalogModel.fromJson(body);
      }
      throw Exception(
        'Erro ao carregar templates do Diagrama do Cerrado: ${response.statusCode}',
      );
    } on DioException catch (e, stackTrace) {
      debugPrint(
        '[CerradoDiagramRemoteDataSource] ERROR on GET $_templatesPath: $e\n$stackTrace',
      );
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint(
        '[CerradoDiagramRemoteDataSource] ERROR unexpected on fetchTemplates: $e\n$stackTrace',
      );
      rethrow;
    }
  }

  Future<CerradoPreferencesModel?> fetchPreferences() async {
    debugPrint(
      '[CerradoDiagramRemoteDataSource] Requesting GET $_preferencesPath',
    );
    try {
      final response = await apiClient.get(_preferencesPath);
      if (response.statusCode == 404) {
        return null;
      }
      if (response.statusCode == 200) {
        final body = _asStringMap(
          response.data,
          'Resposta inválida ao carregar preferências do Diagrama do Cerrado.',
        );
        return CerradoPreferencesModel.fromJson(body);
      }
      throw Exception(
        'Erro ao carregar preferências do Diagrama do Cerrado: ${response.statusCode}',
      );
    } on DioException catch (e, stackTrace) {
      if (e.response?.statusCode == 404) {
        return null;
      }
      debugPrint(
        '[CerradoDiagramRemoteDataSource] ERROR on GET $_preferencesPath: $e\n$stackTrace',
      );
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint(
        '[CerradoDiagramRemoteDataSource] ERROR unexpected on fetchPreferences: $e\n$stackTrace',
      );
      rethrow;
    }
  }

  Future<CerradoPreferencesModel> savePreferences({
    required Map<CerradoMacroClass, int> targetsBps,
    double? grahamMultiplier,
    bool? grahamFilterEnabled,
  }) async {
    debugPrint(
      '[CerradoDiagramRemoteDataSource] Requesting PUT $_preferencesPath',
    );
    try {
      final payload = CerradoPreferencesModel.toUpdatePayload(
        targetsBps: targetsBps,
        grahamMultiplier: grahamMultiplier,
        grahamFilterEnabled: grahamFilterEnabled,
      );
      final response = await apiClient.put(_preferencesPath, data: payload);
      if (response.statusCode == 200) {
        final body = _asStringMap(
          response.data,
          'Resposta inválida ao salvar preferências do Diagrama do Cerrado.',
        );
        return CerradoPreferencesModel.fromJson(body);
      }
      throw Exception(
        'Erro ao salvar preferências do Diagrama do Cerrado: ${response.statusCode}',
      );
    } on DioException catch (e, stackTrace) {
      debugPrint(
        '[CerradoDiagramRemoteDataSource] ERROR on PUT $_preferencesPath: $e\n$stackTrace',
      );
      throw _mapDioException(e);
    } catch (e, stackTrace) {
      debugPrint(
        '[CerradoDiagramRemoteDataSource] ERROR unexpected on savePreferences: $e\n$stackTrace',
      );
      rethrow;
    }
  }

  Map<String, dynamic> _asStringMap(Object? raw, String errorMessage) {
    if (raw is Map) {
      return Map<String, dynamic>.from(raw);
    }
    throw Exception(errorMessage);
  }

  Exception _mapDioException(DioException e) {
    final responseData = e.response?.data;
    Map<String, dynamic>? parsedMap;

    if (responseData is Map) {
      parsedMap = Map<String, dynamic>.from(responseData);
    } else if (responseData is String && responseData.trim().isNotEmpty) {
      try {
        final decoded = jsonDecode(responseData);
        if (decoded is Map) {
          parsedMap = Map<String, dynamic>.from(decoded);
        }
      } catch (parseError, stackTrace) {
        debugPrint(
          '[CerradoDiagramRemoteDataSource] WARN failed to decode error body: $parseError\n$stackTrace',
        );
        return Exception(responseData.trim());
      }
    }

    if (parsedMap != null) {
      for (final key in const ['detail', 'message', 'title']) {
        final candidate = parsedMap[key]?.toString().trim();
        if (candidate != null && candidate.isNotEmpty) {
          return Exception(candidate);
        }
      }
    }

    return Exception('Erro de comunicação com o servidor.');
  }
}
