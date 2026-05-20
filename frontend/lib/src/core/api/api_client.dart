import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Contrato mínimo necessário para obter o token — evita dependência circular
/// com AuthRepository completo.
abstract class TokenProvider {
  Future<String?> getPersistedToken();
}

class ApiClient {
  final Dio dio;
  final FlutterSecureStorage _storage;
  TokenProvider? _tokenProvider;

  ApiClient({
    required String baseUrl,
    Dio? dio,
    FlutterSecureStorage? storage,
  })  : dio = dio ?? Dio(BaseOptions(baseUrl: baseUrl)),
        _storage = storage ?? const FlutterSecureStorage() {
    _initInterceptors();
  }

  /// Injeta o provider de token após a construção (evita dependência circular)
  void setTokenProvider(TokenProvider provider) {
    _tokenProvider = provider;
  }

  void _initInterceptors() {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          String? token;

          // Prefere o provider (com auto-refresh) se disponível
          if (_tokenProvider != null) {
            token = await _tokenProvider!.getPersistedToken();
          } else {
            // Fallback direto ao storage
            token = await _storage.read(key: 'jwt_token');
          }

          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }

          return handler.next(options);
        },
        onError: (DioException e, handler) {
          if (e.response?.statusCode == 401) {
            // Futuro: Lógica para forçar re-login ou notificar o usuário
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

  Future<Response> patch(String path, {dynamic data}) => dio.patch(path, data: data);

  Future<Response> put(String path, {dynamic data}) => dio.put(path, data: data);

  Future<Response> delete(String path, {dynamic data}) => dio.delete(path, data: data);
}
