import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/allocation/data/datasources/allocation_remote_data_source.dart';
import 'package:frontend/src/features/allocation/data/models/allocation_goal_model.dart';

@GenerateNiceMocks([MockSpec<ApiClient>()])
import 'allocation_remote_data_source_test.mocks.dart';

void main() {
  late MockApiClient mockApiClient;
  late AllocationRemoteDataSource dataSource;

  setUp(() {
    mockApiClient = MockApiClient();
    dataSource = AllocationRemoteDataSource(apiClient: mockApiClient);
  });

  group('AllocationRemoteDataSource - getGoals', () {
    test('getGoals should return list of models on status 200', () async {
      final responseData = [
        {'goalType': 'ASSET_CLASS', 'targetKey': 'ACOES', 'targetPercentage': 50.0},
      ];
      when(mockApiClient.get(any)).thenAnswer((_) async => Response(
            requestOptions: RequestOptions(path: ''),
            data: responseData,
            statusCode: 200,
          ));

      final result = await dataSource.getGoals();
      expect(result, isA<List<AllocationGoalModel>>());
      expect(result.first.targetKey, 'ACOES');
    });

    test('getGoals should throw Premium exception on status 403', () async {
      when(mockApiClient.get(any)).thenThrow(DioException(
        requestOptions: RequestOptions(path: ''),
        response: Response(
          requestOptions: RequestOptions(path: ''),
          statusCode: 403,
        ),
      ));

      expect(
        () => dataSource.getGoals(),
        throwsA(predicate((e) =>
            e is Exception &&
            e.toString().contains('exclusiva para usuários Premium'))),
      );
    });

    test('getGoals should handle HTML error graciosamente (NFR error parsing patch)', () async {
      when(mockApiClient.get(any)).thenThrow(DioException(
        requestOptions: RequestOptions(path: ''),
        response: Response(
          requestOptions: RequestOptions(path: ''),
          data: '<html>500 Internal Server Error</html>',
          statusCode: 500,
        ),
      ));

      expect(
        () => dataSource.getGoals(),
        throwsA(predicate((e) =>
            e is Exception &&
            e.toString().contains('Erro de conexão com o servidor'))),
      );
    });

    test('getGoals should throw Exception when response status code is not 200', () async {
      when(mockApiClient.get(any)).thenAnswer((_) async => Response(
            requestOptions: RequestOptions(path: ''),
            statusCode: 500,
          ));

      expect(
        () => dataSource.getGoals(),
        throwsA(predicate((e) =>
            e is Exception &&
            e.toString().contains('Erro ao buscar metas de alocação: 500'))),
      );
    });
  });

  group('AllocationRemoteDataSource - saveGoals', () {
    final goals = [
      const AllocationGoalModel(goalType: 'ASSET_CLASS', targetKey: 'ACOES', targetPercentage: 100.0),
    ];

    test('saveGoals should complete successfully on status 204', () async {
      when(mockApiClient.post(any, data: anyNamed('data'))).thenAnswer((_) async => Response(
            requestOptions: RequestOptions(path: ''),
            statusCode: 204,
          ));

      await expectLater(dataSource.saveGoals(goals), completes);
    });

    test('saveGoals should complete successfully on status 200', () async {
      when(mockApiClient.post(any, data: anyNamed('data'))).thenAnswer((_) async => Response(
            requestOptions: RequestOptions(path: ''),
            statusCode: 200,
          ));

      await expectLater(dataSource.saveGoals(goals), completes);
    });

    test('saveGoals should throw Exception when response status code is not 200 or 204', () async {
      when(mockApiClient.post(any, data: anyNamed('data'))).thenAnswer((_) async => Response(
            requestOptions: RequestOptions(path: ''),
            statusCode: 400,
          ));

      expect(
        () => dataSource.saveGoals(goals),
        throwsA(predicate((e) =>
            e is Exception &&
            e.toString().contains('Erro ao salvar metas de alocação: 400'))),
      );
    });

    test('saveGoals should throw Premium exception on status 403', () async {
      when(mockApiClient.post(any, data: anyNamed('data'))).thenThrow(DioException(
        requestOptions: RequestOptions(path: ''),
        response: Response(
          requestOptions: RequestOptions(path: ''),
          statusCode: 403,
        ),
      ));

      expect(
        () => dataSource.saveGoals(goals),
        throwsA(predicate((e) =>
            e is Exception &&
            e.toString().contains('exclusiva para usuários Premium'))),
      );
    });

    test('saveGoals should throw general error on other DioException', () async {
      when(mockApiClient.post(any, data: anyNamed('data'))).thenThrow(DioException(
        requestOptions: RequestOptions(path: ''),
        response: Response(
          requestOptions: RequestOptions(path: ''),
          statusCode: 500,
        ),
      ));

      expect(
        () => dataSource.saveGoals(goals),
        throwsA(predicate((e) =>
            e is Exception &&
            e.toString().contains('Erro ao salvar metas de alocação.'))),
      );
    });
  });
}
