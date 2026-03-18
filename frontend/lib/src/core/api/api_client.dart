import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  final Dio dio;
  final FlutterSecureStorage _storage;

  ApiClient({
    required String baseUrl,
    Dio? dio,
    FlutterSecureStorage? storage,
  })  : dio = dio ?? Dio(BaseOptions(baseUrl: baseUrl)),
        _storage = storage ?? const FlutterSecureStorage() {
    _initInterceptors();
  }

  void _initInterceptors() {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // Recupera o token do storage seguro
          final token = await _storage.read(key: 'jwt_token');
          
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          
          return handler.next(options);
        },
        onError: (DioException e, handler) {
          if (e.response?.statusCode == 401) {
            // Futuro: Lógica para refresh token ou deslogar o usuário
            print('Sessão expirada ou não autorizada (401)');
          }
          return handler.next(e);
        },
      ),
    );
  }

  // Atalhos para métodos comuns
  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) =>
      dio.get(path, queryParameters: queryParameters);

  Future<Response> post(String path, {dynamic data}) => dio.post(path, data: data);

  Future<Response> put(String path, {dynamic data}) => dio.put(path, data: data);

  Future<Response> delete(String path, {dynamic data}) => dio.delete(path, data: data);
}
