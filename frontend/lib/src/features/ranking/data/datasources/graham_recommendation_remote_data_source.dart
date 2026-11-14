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

  Uri _buildUri(String pathSuffix, {String? tagId}) {
    final base = Uri.parse('$baseUrl$pathSuffix');
    final normalized = tagId?.trim();
    if (normalized == null || normalized.isEmpty) {
      return base;
    }
    return base.replace(queryParameters: {
      ...base.queryParameters,
      'tagId': normalized,
    });
  }

  Future<List<GrahamRecommendationModel>> getRecommendations({
    String? token,
    String? tagId,
  }) async {
    final http.Response response;
    try {
      response = await client.get(
        _buildUri('/graham-recommendations', tagId: tagId),
        headers: {
          'Content-Type': 'application/json',
          if (token != null) 'Authorization': 'Bearer $token',
        },
      );
    } catch (e) {
      throw Exception('Erro de conexão ou ao processar dados: $e');
    }

    if (response.statusCode == 200) {
      final List<dynamic> jsonList = jsonDecode(response.body);
      return jsonList.map((json) => GrahamRecommendationModel.fromJson(json)).toList();
    } else if (response.statusCode == 403) {
      throw Exception('Funcionalidade exclusiva para usuários Premium.');
    } else if (response.statusCode == 404) {
      throw Exception('Tag selecionada não encontrada.');
    } else {
      throw Exception('Falha ao obter recomendações de Graham. Status: ${response.statusCode}');
    }
  }

  Future<void> triggerCalculation({
    String? token,
    String? tagId,
  }) async {
    final http.Response response;
    try {
      response = await client.post(
        _buildUri('/graham-recommendations/trigger', tagId: tagId),
        headers: {
          'Content-Type': 'application/json',
          if (token != null) 'Authorization': 'Bearer $token',
        },
      );
    } catch (e) {
      throw Exception('Erro de conexão ao disparar cálculo: $e');
    }

    if (response.statusCode == 202) {
      return;
    } else if (response.statusCode == 403) {
      throw Exception('Funcionalidade exclusiva para usuários Premium.');
    } else if (response.statusCode == 404) {
      throw Exception('Tag selecionada não encontrada.');
    } else if (response.statusCode == 429) {
      throw Exception('Aguarde alguns segundos antes de solicitar um novo recálculo para esta visão.');
    } else {
      throw Exception('Falha ao solicitar cálculo. Status: ${response.statusCode}');
    }
  }
}
