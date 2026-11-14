import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/portfolio_summary_model.dart';
import '../models/trade_model.dart';
import '../models/custody_position_model.dart';
import '../models/monthly_evolution_model.dart';

class PortfolioRemoteDataSource {
  final http.Client client;
  final String baseUrl;

  const PortfolioRemoteDataSource({
    required this.client,
    this.baseUrl = const String.fromEnvironment('API_BASE_URL', defaultValue: 'http://localhost:8080/api/v1'),
  });

  Uri _buildScopedUri(String path, {String? tagId}) {
    final normalizedTagId = tagId?.trim();
    final base = Uri.parse('$baseUrl$path');
    if (normalizedTagId == null || normalizedTagId.isEmpty) {
      return base;
    }
    return base.replace(queryParameters: {
      ...base.queryParameters,
      'tagId': normalizedTagId,
    });
  }

  Future<PortfolioSummaryModel> getSummary({String? token, String? tagId}) async {
    final uri = _buildScopedUri('/portfolios/summary', tagId: tagId);
    final response = await client.get(
      uri,
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode == 200) {
      return PortfolioSummaryModel.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Erro ao buscar resumo do portfólio: ${response.statusCode}');
    }
  }

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

  /// Retorna as posições de custódia, o priceUpdatedAt autoritativo do backend e metadados de ativos sem tag.
  /// O campo [metaPriceUpdatedAt] pode ser null quando todas as posições são CACHE sem data real.
  Future<({
    List<CustodyPositionModel> positions,
    DateTime? metaPriceUpdatedAt,
    int untaggedAssetsCount,
    List<String> untaggedTickers,
  })> getCustodyPositions({String? token, String? tagId}) async {
    final uri = _buildScopedUri('/portfolios/custody', tagId: tagId);
    final response = await client.get(
      uri,
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> body = jsonDecode(response.body);
      final List<dynamic> data = body['data'];
      final Map<String, dynamic> meta = (body['meta'] as Map<String, dynamic>?) ?? const {};

      final positions = data.map((json) => CustodyPositionModel.fromJson(json)).toList();
      final metaUpdatedAt = meta['priceUpdatedAt'] != null
          ? DateTime.parse(meta['priceUpdatedAt'] as String)
          : null;
      final untaggedAssetsCount = (meta['untaggedAssetsCount'] as num?)?.toInt() ?? 0;
      final untaggedRaw = meta['untaggedTickers'] as List<dynamic>?;
      final untaggedTickers = untaggedRaw
              ?.map((e) => e.toString().trim().toUpperCase())
              .where((e) => e.isNotEmpty)
              .toList() ??
          const <String>[];

      return (
        positions: positions,
        metaPriceUpdatedAt: metaUpdatedAt,
        untaggedAssetsCount: untaggedAssetsCount,
        untaggedTickers: untaggedTickers,
      );
    } else {
      throw Exception('Erro ao buscar posições de custódia: ${response.statusCode}');
    }
  }

  Future<PortfolioEvolutionModel> getPortfolioEvolution({String? token, String? tagId}) async {
    final uri = _buildScopedUri('/portfolios/evolution', tagId: tagId);
    final response = await client.get(
      uri,
      headers: {
        'Content-Type': 'application/json',
        if (token != null) 'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode == 200) {
      return PortfolioEvolutionModel.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Erro ao buscar evolução do portfólio: ${response.statusCode}');
    }
  }
}
