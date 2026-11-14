import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/ranking/data/datasources/graham_recommendation_remote_data_source.dart';
import 'package:http/http.dart' as http;

class MockHttpClient extends http.BaseClient {
  final Future<http.Response> Function(http.BaseRequest request) mockHandler;

  MockHttpClient(this.mockHandler);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final response = await mockHandler(request);
    final bytes = response.bodyBytes;
    return http.StreamedResponse(
      Stream.value(bytes),
      response.statusCode,
      contentLength: bytes.length,
      headers: response.headers,
      request: request,
    );
  }
}

void main() {
  const baseUrl = 'http://localhost:8080/api/v1';

  group('GrahamRecommendationRemoteDataSource', () {
    test('deve incluir query param tagId em getRecommendations e triggerCalculation quando fornecido', () async {
      final urls = <String>[];
      final client = MockHttpClient((request) async {
        urls.add(request.url.toString());
        if (request.method == 'GET') {
          return http.Response(jsonEncode([
            {
              'ticker': 'VALE3',
              'currentPrice': 70.0,
              'intrinsicValue': 90.0,
              'marginOfSafety': 0.28,
              'currentAllocationPct': 5.0,
              'targetAllocationPct': 20.0,
              'allocationGap': 15.0,
              'recommendationScore': 4.2,
            }
          ]), 200);
        }
        return http.Response('', 202);
      });

      final ds = GrahamRecommendationRemoteDataSource(client: client, baseUrl: baseUrl);
      final list = await ds.getRecommendations(token: 'jwt', tagId: 'tag-77');
      await ds.triggerCalculation(token: 'jwt', tagId: 'tag-77');

      expect(urls, [
        '$baseUrl/graham-recommendations?tagId=tag-77',
        '$baseUrl/graham-recommendations/trigger?tagId=tag-77',
      ]);
      expect(list.single.ticker, 'VALE3');
    });

    test('não deve incluir query param tagId quando nulo', () async {
      final urls = <String>[];
      final client = MockHttpClient((request) async {
        urls.add(request.url.toString());
        return http.Response(request.method == 'GET' ? '[]' : '', request.method == 'GET' ? 200 : 202);
      });

      final ds = GrahamRecommendationRemoteDataSource(client: client, baseUrl: baseUrl);
      await ds.getRecommendations(token: 'jwt');
      await ds.triggerCalculation(token: 'jwt');

      expect(urls, [
        '$baseUrl/graham-recommendations',
        '$baseUrl/graham-recommendations/trigger',
      ]);
    });
  });
}
