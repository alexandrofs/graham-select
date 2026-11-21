import 'package:dio/dio.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/notifications/domain/models/whatsapp_notification_models.dart';

class WhatsappNotificationsRemoteDataSource {
  final ApiClient apiClient;

  WhatsappNotificationsRemoteDataSource({required this.apiClient});

  Future<WhatsappPreferencesModel> getPreferences() async {
    try {
      final response =
          await apiClient.get('/api/v1/notifications/whatsapp/preferences');
      return WhatsappPreferencesModel.fromJson(_requireJsonMap(response.data));
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao carregar preferências WhatsApp.'));
    }
  }

  Future<WhatsappPreferencesModel> updatePreferences(
      WhatsappPreferencesModel model) async {
    try {
      final response = await apiClient.put(
        '/api/v1/notifications/whatsapp/preferences',
        data: model.toUpdateJson(),
      );
      return WhatsappPreferencesModel.fromJson(_requireJsonMap(response.data));
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao salvar preferências WhatsApp.'));
    }
  }

  Future<WhatsappTestAlertResultModel> sendTestAlert() async {
    try {
      final response =
          await apiClient.post('/api/v1/notifications/whatsapp/test');
      return WhatsappTestAlertResultModel.fromJson(
          _requireJsonMap(response.data));
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao enviar alerta de teste.'));
    }
  }

  Future<List<WhatsappNotificationLogModel>> getHistory({int limit = 20}) async {
    try {
      final response = await apiClient.get(
        '/api/v1/notifications/whatsapp/history',
        queryParameters: {'limit': limit},
      );
      if (response.data is! List) {
        throw Exception('Resposta inválida do servidor ao carregar histórico.');
      }
      final list = response.data as List<dynamic>;
      return list
          .map((item) => WhatsappNotificationLogModel.fromJson(_requireJsonMap(item)))
          .toList();
    } on DioException catch (e) {
      throw Exception(_extractErrorMessage(e, 'Erro ao carregar histórico de alertas.'));
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
