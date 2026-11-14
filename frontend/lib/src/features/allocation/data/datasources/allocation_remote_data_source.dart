import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../models/allocation_goal_model.dart';

class AllocationRemoteDataSource {
  final ApiClient apiClient;

  AllocationRemoteDataSource({required this.apiClient});

  String _scopedPath(String? tagId) {
    final normalized = tagId?.trim();
    if (normalized == null || normalized.isEmpty) {
      return '/api/v1/allocation-strategy';
    }
    return '/api/v1/allocation-strategy?tagId=${Uri.encodeQueryComponent(normalized)}';
  }

  Future<List<AllocationGoalModel>> getGoals({String? tagId}) async {
    try {
      final response = await apiClient.get(_scopedPath(tagId));
      if (response.statusCode == 200) {
        final List<dynamic> data = response.data as List<dynamic>;
        return data.map((json) => AllocationGoalModel.fromJson(json as Map<String, dynamic>)).toList();
      } else {
        throw Exception('Erro ao buscar metas de alocação: ${response.statusCode}');
      }
    } on DioException catch (e) {
      if (e.response?.statusCode == 403) {
        throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade do seu plano.');
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro de conexão com o servidor.');
    }
  }

  Future<void> saveGoals(List<AllocationGoalModel> goals, {String? tagId}) async {
    try {
      final data = goals.map((g) => g.toJson()).toList();
      final response = await apiClient.post(_scopedPath(tagId), data: data);
      if (response.statusCode != 200 && response.statusCode != 204) {
        throw Exception('Erro ao salvar metas de alocação: ${response.statusCode}');
      }
    } on DioException catch (e) {
      if (e.response?.statusCode == 403) {
        throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade do seu plano.');
      }
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro ao salvar metas de alocação.');
    }
  }
}
