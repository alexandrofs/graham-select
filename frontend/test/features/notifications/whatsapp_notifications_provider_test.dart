import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/notifications/data/datasources/whatsapp_notifications_remote_data_source.dart';
import 'package:frontend/src/features/notifications/data/repositories/whatsapp_notifications_repository.dart';
import 'package:frontend/src/features/notifications/domain/models/whatsapp_notification_models.dart';
import 'package:frontend/src/features/notifications/presentation/providers/whatsapp_notifications_provider.dart';
import 'package:frontend/src/core/api/api_client.dart';

class FakeWhatsappNotificationsRepository
    extends WhatsappNotificationsRepository {
  WhatsappPreferencesModel storedPrefs = const WhatsappPreferencesModel(
    phoneNumber: '',
    optedIn: false,
    notifyDividends: true,
    notifyRebalanceTriggers: true,
    notifyGoalMilestones: true,
    rebalanceDriftThresholdPct: 5.0,
  );
  List<WhatsappNotificationLogModel> storedHistory = [];
  bool shouldThrowOnSave = false;

  FakeWhatsappNotificationsRepository()
      : super(
          remoteDataSource: WhatsappNotificationsRemoteDataSource(
            apiClient: ApiClient(baseUrl: 'http://localhost:8080'),
          ),
        );

  @override
  Future<WhatsappPreferencesModel> getPreferences() async => storedPrefs;

  @override
  Future<WhatsappPreferencesModel> updatePreferences(
      WhatsappPreferencesModel model) async {
    if (shouldThrowOnSave) {
      throw Exception('Número de WhatsApp inválido.');
    }
    storedPrefs = model;
    return storedPrefs;
  }

  @override
  Future<WhatsappTestAlertResultModel> sendTestAlert() async {
    final result = WhatsappTestAlertResultModel(
      status: 'SENT',
      provider: 'MOCK',
      providerMessageId: 'mock-wamid-1',
      messagePreview: 'Teste OK',
      sentAt: DateTime.now().toUtc(),
    );
    storedHistory = [
      WhatsappNotificationLogModel(
        id: '1',
        eventType: 'TEST',
        messagePreview: 'Teste OK',
        provider: 'MOCK',
        status: 'SENT',
        sentAt: result.sentAt,
      ),
    ];
    return result;
  }

  @override
  Future<List<WhatsappNotificationLogModel>> getHistory({int limit = 20}) async =>
      storedHistory;
}

void main() {
  test('loadAll loads preferences and history', () async {
    final repo = FakeWhatsappNotificationsRepository();
    final provider = WhatsappNotificationsProvider(repository: repo);

    await provider.loadAll();

    expect(provider.isLoading, isFalse);
    expect(provider.preferences, isNotNull);
    expect(provider.preferences!.optedIn, isFalse);
    expect(provider.history, isEmpty);
  });

  test('savePreferences updates preferences and handles errors', () async {
    final repo = FakeWhatsappNotificationsRepository();
    final provider = WhatsappNotificationsProvider(repository: repo);

    const updated = WhatsappPreferencesModel(
      phoneNumber: '+5511999998888',
      optedIn: true,
      notifyDividends: true,
      notifyRebalanceTriggers: false,
      notifyGoalMilestones: true,
      rebalanceDriftThresholdPct: 10.0,
    );

    final ok = await provider.savePreferences(updated);
    expect(ok, isTrue);
    expect(provider.preferences!.phoneNumber, '+5511999998888');
    expect(provider.preferences!.optedIn, isTrue);

    repo.shouldThrowOnSave = true;
    final fail = await provider.savePreferences(updated);
    expect(fail, isFalse);
    expect(provider.errorMessage, 'Número de WhatsApp inválido.');
  });

  test('sendTestAlert updates history on success', () async {
    final repo = FakeWhatsappNotificationsRepository();
    final provider = WhatsappNotificationsProvider(repository: repo);

    final result = await provider.sendTestAlert();

    expect(result, isNotNull);
    expect(result!.status, 'SENT');
    expect(provider.history.length, 1);
    expect(provider.history.first.eventType, 'TEST');
  });
}
