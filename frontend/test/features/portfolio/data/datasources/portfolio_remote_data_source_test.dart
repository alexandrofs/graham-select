import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/portfolio/data/datasources/portfolio_remote_data_source.dart';
import 'package:http/http.dart' as http;

class MockHttpClient extends http.BaseClient {
  final Future<http.Response> Function(http.BaseRequest request) mockHandler;

  MockHttpClient(this.mockHandler);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final response = await mockHandler(request);
    final bodyBytes = response.bodyBytes;
    return http.StreamedResponse(
      Stream.value(bodyBytes),
      response.statusCode,
      contentLength: bodyBytes.length,
      headers: response.headers,
      request: request,
    );
  }
}

void main() {
  group('PortfolioRemoteDataSource', () {
    const tBaseUrl = 'http://localhost:8080/api/v1';

    test('deve retornar a lista de posições e o metadado priceUpdatedAt com status 200', () async {
      final jsonResponse = {
        'data': [
          {
            'ticker': 'PETR4',
            'quantity': '100.00',
            'averagePrice': '30.00',
            'currentPrice': '35.00',
            'marketValue': '3500.00',
            'gainLossPercentage': '16.67',
            'priceSource': 'LIVE',
            'priceUpdatedAt': '2026-05-30T18:00:00Z',
          }
        ],
        'meta': {
          'priceUpdatedAt': '2026-05-30T18:00:00Z'
        }
      };

      final mockClient = MockHttpClient((request) async {
        expect(request.url.toString(), equals('$tBaseUrl/portfolios/custody'));
        expect(request.method, equals('GET'));
        expect(request.headers['Authorization'], equals('Bearer test-token'));
        return http.Response(jsonEncode(jsonResponse), 200, headers: {
          'content-type': 'application/json',
        });
      });

      final dataSource = PortfolioRemoteDataSource(client: mockClient, baseUrl: tBaseUrl);

      final result = await dataSource.getCustodyPositions(token: 'test-token');

      expect(result.positions.length, equals(1));
      expect(result.positions[0].ticker, equals('PETR4'));
      expect(result.metaPriceUpdatedAt, equals(DateTime.parse('2026-05-30T18:00:00Z')));
    });

    test('deve lançar Exception se o statusCode for diferente de 200', () async {
      final mockClient = MockHttpClient((request) async {
        return http.Response('Internal Server Error', 500);
      });

      final dataSource = PortfolioRemoteDataSource(client: mockClient, baseUrl: tBaseUrl);

      expect(
        () => dataSource.getCustodyPositions(token: 'test-token'),
        throwsException,
      );
    });
  });
}
