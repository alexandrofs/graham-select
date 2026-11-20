import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';

class TaxRemoteDataSource {
  final ApiClient apiClient;

  TaxRemoteDataSource({required this.apiClient});

  Future<Map<String, dynamic>> getBensEDireitos({int? year, String? profileId}) async {
    final queryParams = <String, dynamic>{};
    if (year != null) {
      queryParams['year'] = year;
    }
    if (profileId != null && profileId.trim().isNotEmpty) {
      queryParams['profileId'] = profileId.trim();
    }

    final response = await apiClient.get(
      '/api/v1/tax/bens-e-direitos',
      queryParameters: queryParams,
    );

    if (response.statusCode == 200) {
      return response.data as Map<String, dynamic>;
    } else {
      throw Exception('Falha ao carregar relatório de Bens e Direitos: ${response.statusCode}');
    }
  }

  Future<List<int>> getAvailableYears({String? profileId}) async {
    final queryParams = <String, dynamic>{};
    if (profileId != null && profileId.trim().isNotEmpty) {
      queryParams['profileId'] = profileId.trim();
    }

    final response = await apiClient.get(
      '/api/v1/tax/available-years',
      queryParameters: queryParams,
    );

    if (response.statusCode == 200) {
      final data = response.data as Map<String, dynamic>;
      final rawYears = data['years'] as List<dynamic>? ?? [];
      return rawYears.map((y) => (y as num).toInt()).toList();
    } else {
      throw Exception('Falha ao carregar anos disponíveis: ${response.statusCode}');
    }
  }

  Future<String> exportBensEDireitosCsv({int? year, String? profileId}) async {
    final queryParams = <String, dynamic>{};
    if (year != null) {
      queryParams['year'] = year;
    }
    if (profileId != null && profileId.trim().isNotEmpty) {
      queryParams['profileId'] = profileId.trim();
    }

    final response = await apiClient.dio.get<String>(
      '/api/v1/tax/bens-e-direitos/export',
      queryParameters: queryParams,
      options: Options(responseType: ResponseType.plain),
    );

    if (response.statusCode == 200 && response.data != null) {
      return response.data!;
    } else {
      throw Exception('Falha ao exportar CSV de Bens e Direitos: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getMonthlyCapitalGains({int? year, String? profileId}) async {
    final queryParams = <String, dynamic>{};
    if (year != null) {
      queryParams['year'] = year;
    }
    if (profileId != null && profileId.trim().isNotEmpty) {
      queryParams['profileId'] = profileId.trim();
    }

    final response = await apiClient.get(
      '/api/v1/tax/monthly-capital-gains',
      queryParameters: queryParams,
    );

    if (response.statusCode == 200) {
      return response.data as Map<String, dynamic>;
    } else {
      throw Exception('Falha ao carregar apuração mensal de ganhos de capital: ${response.statusCode}');
    }
  }

  Future<String> exportMonthlyCapitalGainsCsv({int? year, String? profileId}) async {
    final queryParams = <String, dynamic>{};
    if (year != null) {
      queryParams['year'] = year;
    }
    if (profileId != null && profileId.trim().isNotEmpty) {
      queryParams['profileId'] = profileId.trim();
    }

    final response = await apiClient.dio.get<String>(
      '/api/v1/tax/monthly-capital-gains/export',
      queryParameters: queryParams,
      options: Options(responseType: ResponseType.plain),
    );

    if (response.statusCode == 200 && response.data != null) {
      return response.data!;
    } else {
      throw Exception('Falha ao exportar CSV de apuração mensal: ${response.statusCode}');
    }
  }
}
