import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../models/financial_goal_model.dart';

class GoalsRemoteDatasource {
  final ApiClient apiClient;

  GoalsRemoteDatasource({required this.apiClient});

  Future<FinancialGoalModel?> fetchGoal() async {
    try {
      final response = await apiClient.get('/api/v1/financial-goals');
      if (response.statusCode == 200) {
        final Map<String, dynamic> responseData = response.data as Map<String, dynamic>;
        final dynamic data = responseData['data'];
        if (data == null || (data is List && data.isEmpty)) {
          return null;
        }
        if (data is List) {
          return FinancialGoalModel.fromJson(data.first as Map<String, dynamic>);
        }
        return FinancialGoalModel.fromJson(data as Map<String, dynamic>);
      } else {
        throw Exception('Erro ao buscar metas financeiras: ${response.statusCode}');
      }
    } on DioException catch (e) {
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro de conexão com o servidor.');
    }
  }

  Future<FinancialGoalModel> createGoal(Map<String, dynamic> payload) async {
    try {
      final response = await apiClient.post('/api/v1/financial-goals', data: payload);
      if (response.statusCode == 201) {
        final Map<String, dynamic> responseData = response.data as Map<String, dynamic>;
        final Map<String, dynamic> data = responseData['data'] as Map<String, dynamic>;
        return FinancialGoalModel.fromJson(data);
      } else {
        throw Exception('Erro ao criar meta financeira: ${response.statusCode}');
      }
    } on DioException catch (e) {
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro de conexão com o servidor.');
    }
  }

  Future<FinancialGoalModel> updateGoal(String id, Map<String, dynamic> payload) async {
    try {
      final response = await apiClient.put('/api/v1/financial-goals/$id', data: payload);
      if (response.statusCode == 200) {
        final Map<String, dynamic> responseData = response.data as Map<String, dynamic>;
        final Map<String, dynamic> data = responseData['data'] as Map<String, dynamic>;
        return FinancialGoalModel.fromJson(data);
      } else {
        throw Exception('Erro ao atualizar meta financeira: ${response.statusCode}');
      }
    } on DioException catch (e) {
      String? detail;
      final responseData = e.response?.data;
      if (responseData is Map) {
        detail = responseData['detail']?.toString();
      }
      throw Exception(detail ?? 'Erro de conexão com o servidor.');
    }
  }
}
