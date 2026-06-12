import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/graham_recommendation_model.dart';

class GrahamRecommendationRemoteDataSource {
  final http.Client client;
  final String baseUrl;

  const GrahamRecommendationRemoteDataSource({
    required this.client,
    this.baseUrl = const String.fromEnvironment('API_BASE_URL', defaultValue: 'http://localhost:8080/api/v1'),
  });

  Future<List<GrahamRecommendationModel>> getRecommendations({String? token}) async {
    try {
      final response = await client.get(
        Uri.parse('$baseUrl/graham-recommendations'),
        headers: {
          'Content-Type': 'application/json',
          if (token != null) 'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 200) {
        final List<dynamic> jsonList = jsonDecode(response.body);
        return jsonList.map((json) => GrahamRecommendationModel.fromJson(json)).toList();
      } else if (response.statusCode == 403) {
        throw Exception('Funcionalidade exclusiva para usuários Premium.');
      } else {
        throw Exception('Falha ao obter recomendações de Graham. Status: ${response.statusCode}');
      }
    } catch (e) {
      if (e.toString().contains('Premium')) {
        rethrow;
      }
      throw Exception('Erro de conexão ou ao processar dados: $e');
    }
  }

  Future<void> triggerCalculation({String? token}) async {
    try {
      final response = await client.post(
        Uri.parse('$baseUrl/graham-recommendations/trigger'),
        headers: {
          'Content-Type': 'application/json',
          if (token != null) 'Authorization': 'Bearer $token',
        },
      );

      if (response.statusCode == 202) {
        return;
      } else if (response.statusCode == 403) {
        throw Exception('Funcionalidade exclusiva para usuários Premium.');
      } else {
        throw Exception('Falha ao solicitar cálculo. Status: ${response.statusCode}');
      }
    } catch (e) {
      if (e.toString().contains('Premium')) {
        rethrow;
      }
      throw Exception('Erro de conexão ao disparar cálculo: $e');
    }
  }
}
