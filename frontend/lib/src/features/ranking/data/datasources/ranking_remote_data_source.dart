import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/ranked_company_model.dart';

class RankingRemoteDataSource {
  final http.Client client;
  final String baseUrl;

  const RankingRemoteDataSource({
    required this.client,
    this.baseUrl = 'http://localhost:8080/api/v1',
  });

  Future<List<RankedCompanyModel>> getRankedCompanies() async {
    try {
      final response = await client.get(
        Uri.parse('$baseUrl/ranked-companies'),
        headers: {'Content-Type': 'application/json'},
      );

      if (response.statusCode == 200) {
        final List<dynamic> jsonList = jsonDecode(response.body);
        return jsonList.map((json) => RankedCompanyModel.fromJson(json)).toList();
      } else {
        throw Exception('Falha ao carregar ranking. Status: ${response.statusCode}');
      }
    } catch (e) {
       throw Exception('Erro de conexão ou ao processar dados: $e');
    }
  }
}
