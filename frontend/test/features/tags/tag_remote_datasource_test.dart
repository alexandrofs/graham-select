import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/tags/data/datasources/tag_remote_datasource.dart';

class _FakeApiClient extends ApiClient {
  _FakeApiClient() : super(baseUrl: 'http://localhost:8080');

  String? lastMethod;
  String? lastPath;
  dynamic lastData;
  Future<Response<dynamic>> Function(String method, String path, dynamic data)?
      handler;

  @override
  Future<Response<dynamic>> get(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    lastMethod = 'GET';
    lastPath = path;
    lastData = queryParameters;
    return handler!('GET', path, queryParameters);
  }

  @override
  Future<Response<dynamic>> post(String path, {dynamic data}) async {
    lastMethod = 'POST';
    lastPath = path;
    lastData = data;
    return handler!('POST', path, data);
  }

  @override
  Future<Response<dynamic>> put(String path, {dynamic data}) async {
    lastMethod = 'PUT';
    lastPath = path;
    lastData = data;
    return handler!('PUT', path, data);
  }

  @override
  Future<Response<dynamic>> delete(String path, {dynamic data}) async {
    lastMethod = 'DELETE';
    lastPath = path;
    lastData = data;
    return handler!('DELETE', path, data);
  }
}

void main() {
  late _FakeApiClient fakeApiClient;
  late TagRemoteDataSource dataSource;

  setUp(() {
    fakeApiClient = _FakeApiClient();
    dataSource = TagRemoteDataSource(apiClient: fakeApiClient);
  });

  group('TagRemoteDataSource', () {
    test('fetchTags converte mapas dinâmicos e datas UTC sem Z', () async {
      fakeApiClient.handler = (method, path, _) async => Response(
            requestOptions: RequestOptions(path: path),
            statusCode: 200,
            data: <Object?, Object?>{
              'data': <Object?>[
                <Object?, Object?>{
                  'id': 'tag-1',
                  'name': 'Dividendos',
                  'color': '#10b981',
                  'tickers': [' petr4 '],
                  'createdAt': '2026-11-13T12:00:00',
                },
              ],
            },
          );

      final result = await dataSource.fetchTags();

      expect(fakeApiClient.lastPath, '/api/v1/tags');
      expect(result, hasLength(1));
      expect(result.first.color, '#10B981');
      expect(result.first.tickers, ['PETR4']);
      expect(result.first.createdAt, DateTime.utc(2026, 11, 13, 12, 0, 0));
    });

    test('codifica ticker e tagId na URL e rejeita parâmetros em branco', () async {
      fakeApiClient.handler = (method, path, _) async => Response(
            requestOptions: RequestOptions(path: path),
            statusCode: 200,
            data: {
              'data': {'ticker': 'CDB BANCO X', 'tags': []},
            },
          );

      await dataSource.fetchAssetTags(' cdb banco x ');
      expect(fakeApiClient.lastPath, '/api/v1/tags/assets/CDB%20BANCO%20X');

      fakeApiClient.handler = (method, path, _) async => Response(
            requestOptions: RequestOptions(path: path),
            statusCode: 201,
            data: null,
          );
      await dataSource.assignAssetToTag(tagId: 'a/b?c', ticker: 'petr4');
      expect(fakeApiClient.lastPath, '/api/v1/tags/a%2Fb%3Fc/assets/PETR4');

      expect(() => dataSource.deleteTag('   '), throwsException);
      expect(() => dataSource.fetchAssetTags('  '), throwsException);
    });

    test('rejeita payloads inválidos e extrai mensagens de DioException', () async {
      fakeApiClient.handler = (method, path, _) async => Response(
            requestOptions: RequestOptions(path: path),
            statusCode: 200,
            data: {'data': 'not-a-list'},
          );

      await expectLater(dataSource.fetchTags(), throwsException);

      fakeApiClient.handler = (method, path, _) {
        throw DioException(
          requestOptions: RequestOptions(path: path),
          response: Response(
            requestOptions: RequestOptions(path: path),
            statusCode: 409,
            data: {'detail': 'Já existe uma tag com o nome "Dividendos".'},
          ),
        );
      };

      await expectLater(
        dataSource.createTag(name: 'Dividendos'),
        throwsA(
          predicate(
            (e) =>
                e is Exception &&
                e.toString().contains('Já existe uma tag com o nome "Dividendos".'),
          ),
        ),
      );

      fakeApiClient.handler = (method, path, _) {
        throw DioException(
          requestOptions: RequestOptions(path: path),
          response: Response(
            requestOptions: RequestOptions(path: path),
            statusCode: 502,
            data: {'detail': '   '},
          ),
        );
      };

      await expectLater(
        dataSource.fetchTags(),
        throwsA(
          predicate(
            (e) =>
                e is Exception &&
                e.toString().contains('Erro de comunicação com o servidor.'),
          ),
        ),
      );
    });
  });
}
