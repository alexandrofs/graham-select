import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../models/family_profile_model.dart';

class PremiumRequiredException implements Exception {
  final String message;

  const PremiumRequiredException(this.message);

  @override
  String toString() => message;
}

class FamilyProfileRemoteDataSource {
  final ApiClient apiClient;

  const FamilyProfileRemoteDataSource({required this.apiClient});

  Future<List<FamilyProfileModel>> fetchProfiles() async {
    try {
      final response = await apiClient.get('/api/v1/family-profiles');
      if (response.statusCode == 200) {
        final rawBody = response.data;
        if (rawBody is! Map<String, dynamic>) {
          throw Exception('Resposta inválida ao listar perfis familiares.');
        }
        final List<dynamic> dataList = rawBody['data'] as List<dynamic>? ?? [];
        return dataList
            .map((e) => FamilyProfileModel.fromJson(e as Map<String, dynamic>))
            .toList();
      }
      throw Exception('Erro ao listar perfis familiares: ${response.statusCode}');
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Future<FamilyProfileModel> linkProfile({
    required String cpf,
    required String name,
    String? relationship,
  }) async {
    try {
      final payload = <String, dynamic>{
        'cpf': cpf,
        'name': name,
        if (relationship != null && relationship.trim().isNotEmpty)
          'relationship': relationship.trim(),
      };
      final response = await apiClient.post('/api/v1/family-profiles', data: payload);
      if (response.statusCode == 201 || response.statusCode == 200) {
        final rawBody = response.data;
        if (rawBody is! Map<String, dynamic> || rawBody['data'] is! Map<String, dynamic>) {
          throw Exception('Resposta inválida ao vincular CPF.');
        }
        return FamilyProfileModel.fromJson(rawBody['data'] as Map<String, dynamic>);
      }
      throw Exception('Erro ao vincular CPF: ${response.statusCode}');
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Future<void> unlinkProfile(String profileId) async {
    try {
      final response = await apiClient.delete('/api/v1/family-profiles/$profileId');
      if (response.statusCode == 204 || response.statusCode == 200) {
        return;
      }
      throw Exception('Erro ao desvincular perfil: ${response.statusCode}');
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Exception _mapDioException(DioException e) {
    final statusCode = e.response?.statusCode;
    final responseData = e.response?.data;
    String? detail;
    if (responseData is Map) {
      detail = responseData['detail']?.toString() ?? responseData['message']?.toString();
    }
    if (statusCode == 403) {
      return PremiumRequiredException(
        detail ?? 'Holding Familiar é um recurso exclusivo para assinantes Premium.',
      );
    }
    return Exception(detail ?? 'Erro de comunicação com o servidor.');
  }
}

