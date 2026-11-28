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
      expect(result.untaggedAssetsCount, equals(0));
      expect(result.untaggedTickers, isEmpty);
    });

    test('deve repassar query param tagId e extrair metadados de ativos sem tag na custódia', () async {
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
          'priceUpdatedAt': '2026-05-30T18:00:00Z',
          'untaggedAssetsCount': 2,
          'untaggedTickers': ['VALE3', 'ITUB4'],
        }
      };

      final mockClient = MockHttpClient((request) async {
        expect(request.url.toString(), equals('$tBaseUrl/portfolios/custody?tagId=tag-1'));
        return http.Response(jsonEncode(jsonResponse), 200, headers: {
          'content-type': 'application/json',
        });
      });

      final dataSource = PortfolioRemoteDataSource(client: mockClient, baseUrl: tBaseUrl);
      final result = await dataSource.getCustodyPositions(token: 'test-token', tagId: 'tag-1');

      expect(result.positions.length, equals(1));
      expect(result.untaggedAssetsCount, equals(2));
      expect(result.untaggedTickers, equals(['VALE3', 'ITUB4']));
    });

    test('deve enviar query param tagId em getSummary e getPortfolioEvolution', () async {
      final requestedUrls = <String>[];
      final mockClient = MockHttpClient((request) async {
        requestedUrls.add(request.url.toString());
        if (request.url.path.endsWith('/summary')) {
          return http.Response(jsonEncode({
            'totalEquity': 5000.0,
            'grossYieldPercentage': 10.0,
            'accumulatedDividends': 250.0,
            'monthlyProjection': 40.0,
          }), 200);
        }
        return http.Response(jsonEncode({
          'monthlyData': [
            {'month': '2026-05', 'totalContributions': 1000.0, 'totalDividends': 50.0},
          ],
        }), 200);
      });

      final dataSource = PortfolioRemoteDataSource(client: mockClient, baseUrl: tBaseUrl);
      final summary = await dataSource.getSummary(token: 't', tagId: 'tag-1');
      final evolution = await dataSource.getPortfolioEvolution(token: 't', tagId: 'tag-1');

      expect(requestedUrls, equals([
        '$tBaseUrl/portfolios/summary?tagId=tag-1',
        '$tBaseUrl/portfolios/evolution?tagId=tag-1',
      ]));
      expect(summary.totalEquity, equals(5000.0));
      expect(evolution.monthlyData.length, equals(1));
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

    test('getManualTrades retorna lista de TradeModel mapeando assetClass e profileId', () async {
      final mockClient = MockHttpClient((request) async {
        expect(request.method, equals('GET'));
        expect(request.url.toString(), equals('$tBaseUrl/trades/manual'));
        expect(request.headers['Authorization'], equals('Bearer tok'));
        return http.Response(
          jsonEncode([
            {
              'id': 'id-1',
              'userId': 'u-1',
              'profileId': 'prof-1',
              'ticker': 'IVVB11',
              'side': 'COMPRA',
              'tradeDate': '2026-05-10',
              'quantity': 10,
              'price': 310.5,
              'broker': 'Manual',
              'assetClass': 'Internacional',
              'createdAt': '2026-05-10T10:00:00Z',
            }
          ]),
          200,
        );
      });

      final dataSource = PortfolioRemoteDataSource(client: mockClient, baseUrl: tBaseUrl);
      final trades = await dataSource.getManualTrades(token: 'tok');

      expect(trades.length, equals(1));
      expect(trades[0].id, equals('id-1'));
      expect(trades[0].profileId, equals('prof-1'));
      expect(trades[0].assetClass, equals('Internacional'));
    });

    test('createManualTrade envia assetClass e profileId e trata 404 e 409', () async {
      final mockClient = MockHttpClient((request) async {
        expect(request.method, equals('POST'));
        expect(request.url.toString(), equals('$tBaseUrl/trades/manual'));
        final body = jsonDecode((request as http.Request).body) as Map<String, dynamic>;
        expect(body['assetClass'], equals('Criptoativos'));
        expect(body['profileId'], equals('prof-1'));
        return http.Response(
          jsonEncode({
            'id': 'id-2',
            'userId': 'u-1',
            'profileId': 'prof-1',
            'ticker': 'BTC',
            'side': 'COMPRA',
            'tradeDate': '2026-05-10',
            'quantity': 0.05,
            'price': 350000,
            'broker': 'Manual',
            'assetClass': 'Criptoativos',
            'createdAt': '2026-05-10T10:00:00Z',
          }),
          201,
        );
      });

      final dataSource = PortfolioRemoteDataSource(client: mockClient, baseUrl: tBaseUrl);
      final created = await dataSource.createManualTrade(
        ticker: 'BTC',
        side: 'COMPRA',
        tradeDate: DateTime(2026, 5, 10),
        quantity: 0.05,
        price: 350000,
        broker: 'Manual',
        assetClass: 'Criptoativos',
        profileId: 'prof-1',
        token: 'tok',
      );
      expect(created.assetClass, equals('Criptoativos'));

      final notFoundClient = MockHttpClient((_) async => http.Response('', 404));
      final notFoundDs = PortfolioRemoteDataSource(client: notFoundClient, baseUrl: tBaseUrl);
      expect(
        () => notFoundDs.createManualTrade(
          ticker: 'BTC',
          side: 'COMPRA',
          tradeDate: DateTime(2026, 5, 10),
          quantity: 1,
          price: 100,
          broker: 'Manual',
          profileId: 'missing',
        ),
        throwsException,
      );
    });

    test('updateManualTrade e deleteManualTrade enviam PUT e DELETE e tratam 404/409', () async {
      final putClient = MockHttpClient((request) async {
        expect(request.method, equals('PUT'));
        expect(request.url.toString(), equals('$tBaseUrl/trades/manual/id-1'));
        return http.Response(
          jsonEncode({
            'id': 'id-1',
            'userId': 'u-1',
            'ticker': 'PETR4',
            'side': 'COMPRA',
            'tradeDate': '2026-05-11',
            'quantity': 20,
            'price': 32,
            'broker': 'XP',
            'assetClass': 'Ações BR',
            'createdAt': '2026-05-10T10:00:00Z',
          }),
          200,
        );
      });

      final putDs = PortfolioRemoteDataSource(client: putClient, baseUrl: tBaseUrl);
      final updated = await putDs.updateManualTrade(
        id: 'id-1',
        ticker: 'PETR4',
        side: 'COMPRA',
        tradeDate: DateTime(2026, 5, 11),
        quantity: 20,
        price: 32,
        broker: 'XP',
        assetClass: 'Ações BR',
      );
      expect(updated.quantity, equals(20));

      final deleteClient = MockHttpClient((request) async {
        expect(request.method, equals('DELETE'));
        expect(request.url.toString(), equals('$tBaseUrl/trades/manual/id-1'));
        return http.Response('', 204);
      });
      final deleteDs = PortfolioRemoteDataSource(client: deleteClient, baseUrl: tBaseUrl);
      await deleteDs.deleteManualTrade(id: 'id-1');

      final conflictClient = MockHttpClient((_) async => http.Response('', 409));
      final conflictDs = PortfolioRemoteDataSource(client: conflictClient, baseUrl: tBaseUrl);
      expect(
        () => conflictDs.updateManualTrade(
          id: 'id-1',
          ticker: 'PETR4',
          side: 'COMPRA',
          tradeDate: DateTime(2026, 5, 11),
          quantity: 20,
          price: 32,
          broker: 'XP',
        ),
        throwsException,
      );
    });
  });
}
