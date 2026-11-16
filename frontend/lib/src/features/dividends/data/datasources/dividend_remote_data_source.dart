import '../../../../core/api/api_client.dart';
import '../models/dividend_announcement_model.dart';
import '../models/relevant_fact_model.dart';

class DividendRemoteDataSource {
  final ApiClient apiClient;

  DividendRemoteDataSource({required this.apiClient});

  Future<List<DividendAnnouncementModel>> getDividendSchedule({
    String? ticker,
    DateTime? startDate,
    DateTime? endDate,
    String? status,
    bool onlyMyTickers = false,
  }) async {
    final queryParameters = <String, dynamic>{
      'onlyMyTickers': onlyMyTickers,
    };
    if (ticker != null && ticker.trim().isNotEmpty) {
      queryParameters['ticker'] = ticker.trim().toUpperCase();
    }
    if (startDate != null) {
      queryParameters['startDate'] = startDate.toIso8601String().split('T').first;
    }
    if (endDate != null) {
      queryParameters['endDate'] = endDate.toIso8601String().split('T').first;
    }
    if (status != null && status.trim().isNotEmpty) {
      queryParameters['status'] = status.trim().toUpperCase();
    }

    final response = await apiClient.get(
      '/api/v1/dividends/schedule',
      queryParameters: queryParameters,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = response.data is List ? response.data as List<dynamic> : [];
      return data
          .map((item) => DividendAnnouncementModel.fromJson(item as Map<String, dynamic>))
          .toList();
    } else {
      throw Exception('Falha ao carregar agenda de proventos: ${response.statusCode}');
    }
  }

  Future<List<RelevantFactModel>> getRelevantFacts({
    String? ticker,
    int limit = 20,
    bool onlyMyTickers = false,
  }) async {
    final queryParameters = <String, dynamic>{
      'limit': limit,
      'onlyMyTickers': onlyMyTickers,
    };
    if (ticker != null && ticker.trim().isNotEmpty) {
      queryParameters['ticker'] = ticker.trim().toUpperCase();
    }

    final response = await apiClient.get(
      '/api/v1/dividends/facts',
      queryParameters: queryParameters,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = response.data is List ? response.data as List<dynamic> : [];
      return data
          .map((item) => RelevantFactModel.fromJson(item as Map<String, dynamic>))
          .toList();
    } else {
      throw Exception('Falha ao carregar fatos relevantes: ${response.statusCode}');
    }
  }
}
