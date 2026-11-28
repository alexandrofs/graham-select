import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/cerrado_diagram/data/datasources/cerrado_diagram_remote_data_source.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_macro_class.dart';

class _FakeApiClient extends Fake implements ApiClient {
  Future<Response> Function(String path)? onGet;
  Future<Response> Function(String path, dynamic data)? onPut;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) {
    return onGet!(path);
  }

  @override
  Future<Response> put(String path, {dynamic data}) {
    return onPut!(path, data);
  }
}

void main() {
  late _FakeApiClient apiClient;
  late CerradoDiagramRemoteDataSource dataSource;

  setUp(() {
    apiClient = _FakeApiClient();
    dataSource = CerradoDiagramRemoteDataSource(apiClient: apiClient);
  });

  test('fetchTemplates faz parse do catálogo oficial com num e String', () async {
    apiClient.onGet = (path) async {
      expect(path, '/api/v1/cerrado/templates');
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: <String, dynamic>{
          'defaultNeutralGrade': 6,
          'cvmDisclaimer': 'Material meramente informativo.',
          'templates': <Map<String, dynamic>>[
            <String, dynamic>{
              'code': 'EQUILIBRADO_CERRADO',
              'name': 'Modelo Equilibrado Cerrado',
              'description': 'Perfil balanceado',
              'targetAcoesBr': 30,
              'targetFiis': '25.00',
              'targetRendaFixa': 24.0,
              'targetInternacional': '20.00',
              'targetCripto': 1.0,
            },
          ],
        },
      );
    };

    final catalog = await dataSource.fetchTemplates();
    expect(catalog.defaultNeutralGrade, 6);
    expect(catalog.cvmDisclaimer, 'Material meramente informativo.');
    expect(catalog.templates, hasLength(1));
    expect(catalog.templates.first.targetAcoesBrBps, 3000);
    expect(catalog.templates.first.targetFiisBps, 2500);
    expect(catalog.templates.first.targetRendaFixaBps, 2400);
    expect(catalog.templates.first.targetInternacionalBps, 2000);
    expect(catalog.templates.first.targetCriptoBps, 100);
  });

  test('fetchPreferences retorna null quando API responde 404 via DioException', () async {
    apiClient.onGet = (path) async {
      throw DioException(
        requestOptions: RequestOptions(path: path),
        response: Response(
          requestOptions: RequestOptions(path: path),
          statusCode: 404,
          data: <String, dynamic>{'detail': 'Preferências não encontradas'},
        ),
        type: DioExceptionType.badResponse,
      );
    };

    final prefs = await dataSource.fetchPreferences();
    expect(prefs, isNull);
  });

  test('fetchPreferences propaga detail de ProblemDetail em erro 500', () async {
    apiClient.onGet = (path) async {
      throw DioException(
        requestOptions: RequestOptions(path: path),
        response: Response(
          requestOptions: RequestOptions(path: path),
          statusCode: 500,
          data: <String, dynamic>{'detail': 'Falha interna ao consultar preferências.'},
        ),
        type: DioExceptionType.badResponse,
      );
    };

    expect(
      () => dataSource.fetchPreferences(),
      throwsA(
        isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('Falha interna ao consultar preferências.'),
        ),
      ),
    );
  });

  test('savePreferences omite graham* quando nulos e envia em 2 casas decimais', () async {
    Map<String, dynamic>? capturedPayload;
    apiClient.onPut = (path, data) async {
      expect(path, '/api/v1/cerrado/preferences');
      capturedPayload = Map<String, dynamic>.from(data as Map);
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: <String, dynamic>{
          'id': 10,
          'userId': 99,
          'targetAcoesBr': 30.0,
          'targetFiis': 25.0,
          'targetRendaFixa': 24.0,
          'targetInternacional': 20.0,
          'targetCripto': 1.0,
          'grahamMultiplier': 1.30,
          'grahamFilterEnabled': true,
          'updatedAt': '2026-11-28T12:00:00Z',
        },
      );
    };

    final saved = await dataSource.savePreferences(
      targetsBps: const <CerradoMacroClass, int>{
        CerradoMacroClass.acoesBr: 3000,
        CerradoMacroClass.fiis: 2500,
        CerradoMacroClass.rendaFixa: 2400,
        CerradoMacroClass.internacional: 2000,
        CerradoMacroClass.cripto: 100,
      },
    );

    expect(capturedPayload, <String, dynamic>{
      'targetAcoesBr': 30.0,
      'targetFiis': 25.0,
      'targetRendaFixa': 24.0,
      'targetInternacional': 20.0,
      'targetCripto': 1.0,
    });
    expect(capturedPayload!.containsKey('grahamMultiplier'), isFalse);
    expect(capturedPayload!.containsKey('grahamFilterEnabled'), isFalse);
    expect(saved.grahamMultiplier, 1.30);
    expect(saved.grahamFilterEnabled, isTrue);
  });

  test('savePreferences preserva graham* quando informados e extrai detail em 422', () async {
    Map<String, dynamic>? capturedPayload;
    apiClient.onPut = (path, data) async {
      capturedPayload = Map<String, dynamic>.from(data as Map);
      throw DioException(
        requestOptions: RequestOptions(path: path),
        response: Response(
          requestOptions: RequestOptions(path: path),
          statusCode: 422,
          data: <String, dynamic>{'detail': 'Soma das metas deve ser exatamente 100.00%.'},
        ),
        type: DioExceptionType.badResponse,
      );
    };

    await expectLater(
      () => dataSource.savePreferences(
        targetsBps: const <CerradoMacroClass, int>{
          CerradoMacroClass.acoesBr: 3000,
          CerradoMacroClass.fiis: 2500,
          CerradoMacroClass.rendaFixa: 2400,
          CerradoMacroClass.internacional: 2000,
          CerradoMacroClass.cripto: 100,
        },
        grahamMultiplier: 1.50,
        grahamFilterEnabled: false,
      ),
      throwsA(
        isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('Soma das metas deve ser exatamente 100.00%.'),
        ),
      ),
    );

    expect(capturedPayload?['grahamMultiplier'], 1.5);
    expect(capturedPayload?['grahamFilterEnabled'], false);
  });
}
