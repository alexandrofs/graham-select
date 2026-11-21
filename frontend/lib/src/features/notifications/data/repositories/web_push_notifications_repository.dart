import 'package:frontend/src/features/notifications/data/datasources/web_push_notifications_remote_data_source.dart';
import 'package:frontend/src/features/notifications/domain/models/web_push_notification_models.dart';

class WebPushNotificationsRepository {
  final WebPushNotificationsRemoteDataSource remoteDataSource;

  WebPushNotificationsRepository({required this.remoteDataSource});

  Future<WebPushVapidKeyModel> getVapidPublicKey() =>
      remoteDataSource.getVapidPublicKey();

  Future<WebPushPreferencesModel> getPreferences() =>
      remoteDataSource.getPreferences();

  Future<WebPushPreferencesModel> updatePreferences(
          WebPushPreferencesModel model) =>
      remoteDataSource.updatePreferences(model);

  Future<List<WebPushSubscriptionModel>> getSubscriptions() =>
      remoteDataSource.getSubscriptions();

  Future<WebPushSubscriptionModel> registerSubscription({
    required String endpoint,
    required String p256dhKey,
    required String authKey,
    String? userAgent,
  }) =>
      remoteDataSource.registerSubscription(
        endpoint: endpoint,
        p256dhKey: p256dhKey,
        authKey: authKey,
        userAgent: userAgent,
      );

  Future<void> deleteSubscription(String id) =>
      remoteDataSource.deleteSubscription(id);

  Future<WebPushTestAlertResultModel> sendTestAlert() =>
      remoteDataSource.sendTestAlert();

  Future<List<WebPushNotificationLogModel>> getHistory({int limit = 20}) =>
      remoteDataSource.getHistory(limit: limit);
}
