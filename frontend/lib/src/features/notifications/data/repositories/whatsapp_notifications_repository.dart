import 'package:frontend/src/features/notifications/data/datasources/whatsapp_notifications_remote_data_source.dart';
import 'package:frontend/src/features/notifications/domain/models/whatsapp_notification_models.dart';

class WhatsappNotificationsRepository {
  final WhatsappNotificationsRemoteDataSource remoteDataSource;

  WhatsappNotificationsRepository({required this.remoteDataSource});

  Future<WhatsappPreferencesModel> getPreferences() =>
      remoteDataSource.getPreferences();

  Future<WhatsappPreferencesModel> updatePreferences(
          WhatsappPreferencesModel model) =>
      remoteDataSource.updatePreferences(model);

  Future<WhatsappTestAlertResultModel> sendTestAlert() =>
      remoteDataSource.sendTestAlert();

  Future<List<WhatsappNotificationLogModel>> getHistory({int limit = 20}) =>
      remoteDataSource.getHistory(limit: limit);
}
