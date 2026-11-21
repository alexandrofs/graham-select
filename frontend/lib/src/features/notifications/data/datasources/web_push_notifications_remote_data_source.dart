import 'package:dio/dio.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/notifications/domain/models/web_push_notification_models.dart';

class WebPushNotificationsRemoteDataSource {
  final ApiClient apiClient;

  WebPushNotificationsRemoteDataSource({required this.apiClient});

  Future<WebPushVapidKeyModel> getVapidPublicKey() async {
    try {
      final response =
          await apiClient.get('/api/v1/notifications/web-push/vapid-public-key');
      return WebPushVapidKeyModel.fromJson(_requireJsonMap(response.data));
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao obter chave pública VAPID.'));
    }
  }

  Future<WebPushPreferencesModel> getPreferences() async {
    try {
      final response =
          await apiClient.get('/api/v1/notifications/web-push/preferences');
      return WebPushPreferencesModel.fromJson(_requireJsonMap(response.data));
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao carregar preferências Web Push.'));
    }
  }

  Future<WebPushPreferencesModel> updatePreferences(
      WebPushPreferencesModel model) async {
    try {
      final response = await apiClient.put(
        '/api/v1/notifications/web-push/preferences',
        data: model.toUpdateJson(),
      );
      return WebPushPreferencesModel.fromJson(_requireJsonMap(response.data));
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao salvar preferências Web Push.'));
    }
  }

  Future<List<WebPushSubscriptionModel>> getSubscriptions() async {
    try {
      final response =
          await apiClient.get('/api/v1/notifications/web-push/subscriptions');
      if (response.data is! List) {
        throw Exception('Resposta inválida do servidor ao carregar assinaturas.');
      }
      final list = response.data as List<dynamic>;
      return list
          .map((item) => WebPushSubscriptionModel.fromJson(_requireJsonMap(item)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao carregar assinaturas de navegadores.'));
    }
  }

  Future<WebPushSubscriptionModel> registerSubscription({
    required String endpoint,
    required String p256dhKey,
    required String authKey,
    String? userAgent,
  }) async {
    try {
      final response = await apiClient.post(
        '/api/v1/notifications/web-push/subscriptions',
        data: {
          'endpoint': endpoint,
          'p256dhKey': p256dhKey,
          'authKey': authKey,
          '?userAgent': userAgent,
        },
      );
      return WebPushSubscriptionModel.fromJson(_requireJsonMap(response.data));
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao registrar assinatura de navegador.'));
    }
  }

  Future<void> deleteSubscription(String id) async {
    try {
      await apiClient.delete('/api/v1/notifications/web-push/subscriptions/$id');
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao remover assinatura de navegador.'));
    }
  }

  Future<WebPushTestAlertResultModel> sendTestAlert() async {
    try {
      final response =
          await apiClient.post('/api/v1/notifications/web-push/test');
      return WebPushTestAlertResultModel.fromJson(
          _requireJsonMap(response.data));
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao enviar alerta de teste Web Push.'));
    }
  }

  Future<List<WebPushNotificationLogModel>> getHistory({int limit = 20}) async {
    try {
      final response = await apiClient.get(
        '/api/v1/notifications/web-push/history',
        queryParameters: {'limit': limit},
      );
      if (response.data is! List) {
        throw Exception('Resposta inválida do servidor ao carregar histórico.');
      }
      final list = response.data as List<dynamic>;
      return list
          .map((item) =>
              WebPushNotificationLogModel.fromJson(_requireJsonMap(item)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao carregar histórico de Web Push.'));
    }
  }

  Map<String, dynamic> _requireJsonMap(dynamic data) {
    if (data is! Map) {
      throw Exception('Resposta inválida do servidor.');
    }
    return Map<String, dynamic>.from(data);
  }

  String _extractErrorMessage(DioException e, String fallback) {
    final data = e.response?.data;
    if (data is Map && data['detail'] != null) {
      return data['detail'].toString();
    }
    return fallback;
  }
}
