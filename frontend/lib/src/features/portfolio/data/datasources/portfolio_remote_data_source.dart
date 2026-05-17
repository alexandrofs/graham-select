import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/trade_model.dart';

class PortfolioRemoteDataSource {
  final http.Client client;
  final String baseUrl;

  const PortfolioRemoteDataSource({
    required this.client,
    this.baseUrl = const String.fromEnvironment('API_BASE_URL', defaultValue: 'http://localhost:8080/api/v1'),
  });

  Future<TradeModel> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
    String? token,
  }) async {
    final uri = Uri.parse('$baseUrl/trades/manual');
    final response = await client.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'ticker': ticker,
        'side': side,
        'tradeDate': tradeDate.toIso8601String().split('T')[0],
        'quantity': quantity,
        'price': price,
        'broker': broker,
      }),
    );

    if (response.statusCode == 201) {
      return TradeModel.fromJson(jsonDecode(response.body));
    } else if (response.statusCode == 409) {
      throw Exception('Operação duplicada detectada.');
    } else {
      throw Exception('Erro ao criar operação manual: ${response.statusCode}');
    }
  }
}
