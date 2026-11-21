import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/notifications/data/datasources/web_push_notifications_remote_data_source.dart';
import 'package:frontend/src/features/notifications/data/repositories/web_push_notifications_repository.dart';
import 'package:frontend/src/features/notifications/data/services/web_push_browser_client.dart';
import 'package:frontend/src/features/notifications/domain/models/web_push_notification_models.dart';
import 'package:frontend/src/features/notifications/presentation/providers/web_push_notifications_provider.dart';

class FakeWebPushBrowserClient implements WebPushBrowserClient {
  String permission = 'granted';
  bool shouldReturnNullPayload = false;

  @override
  bool get isSupported => true;

  @override
  Future<String> getPermissionState() async => permission;

  @override
  Future<WebPushSubscriptionPayload?> subscribeToPush(
      String vapidPublicKeyBase64Url) async {
    if (shouldReturnNullPayload) return null;
    return const WebPushSubscriptionPayload(
      endpoint: 'https://fcm.googleapis.com/fcm/send/fake-sub-token',
      p256dhKey: 'fake_p256dh',
      authKey: 'fake_auth',
      userAgent: 'Flutter Test Agent',
    );
  }
}

class FakeWebPushNotificationsRepository extends WebPushNotificationsRepository {
  WebPushPreferencesModel storedPrefs = const WebPushPreferencesModel(
    optedIn: false,
    minMarginOfSafetyPct: 20.0,
    onlyCustodyTickers: true,
  );
  List<WebPushSubscriptionModel> storedSubscriptions = [];
  List<WebPushNotificationLogModel> storedHistory = [];
  bool shouldThrowOnSave = false;

  FakeWebPushNotificationsRepository()
      : super(
          remoteDataSource: WebPushNotificationsRemoteDataSource(
            apiClient: ApiClient(baseUrl: 'http://localhost:8080'),
          ),
        );

  @override
  Future<WebPushVapidKeyModel> getVapidPublicKey() async =>
      const WebPushVapidKeyModel(publicKey: 'fake-vapid-key', provider: 'MOCK');

  @override
  Future<WebPushPreferencesModel> getPreferences() async => storedPrefs;

  @override
  Future<WebPushPreferencesModel> updatePreferences(
      WebPushPreferencesModel model) async {
    if (shouldThrowOnSave) {
      throw Exception('Margem de segurança inválida.');
    }
    storedPrefs = model;
    return storedPrefs;
  }

  @override
  Future<List<WebPushSubscriptionModel>> getSubscriptions() async =>
      storedSubscriptions;

  @override
  Future<WebPushSubscriptionModel> registerSubscription({
    required String endpoint,
    required String p256dhKey,
    required String authKey,
    String? userAgent,
  }) async {
    final sub = WebPushSubscriptionModel(
      id: 'sub-1',
      endpointPreview: 'https://fcm.googleapis.com/.../****',
      userAgent: userAgent,
      active: true,
      lastUsedAt: null,
      createdAt: DateTime.now().toUtc(),
    );
    storedSubscriptions = [sub];
    return sub;
  }

  @override
  Future<void> deleteSubscription(String id) async {
    storedSubscriptions.removeWhere((s) => s.id == id);
  }

  @override
  Future<WebPushTestAlertResultModel> sendTestAlert() async {
    final result = WebPushTestAlertResultModel(
      sentCount: 1,
      failedCount: 0,
      provider: 'MOCK',
      messagePreview: 'Alerta Teste',
      sentAt: DateTime.now().toUtc(),
    );
    storedHistory = [
      WebPushNotificationLogModel(
        id: 'log-1',
        ticker: null,
        eventType: 'TEST_ALERT',
        title: 'Teste',
        messagePreview: 'Alerta Teste',
        provider: 'MOCK',
        httpStatus: 201,
        status: 'SENT',
        errorMessage: null,
        sentAt: result.sentAt,
      )
    ];
    return result;
  }

  @override
  Future<List<WebPushNotificationLogModel>> getHistory({int limit = 20}) async =>
      storedHistory;
}

void main() {
  test('loadAll loads preferences, subscriptions, history and permission state',
      () async {
    final repo = FakeWebPushNotificationsRepository();
    final client = FakeWebPushBrowserClient();
    final provider = WebPushNotificationsProvider(
      repository: repo,
      browserClient: client,
    );

    await provider.loadAll();

    expect(provider.isLoading, isFalse);
    expect(provider.preferences, isNotNull);
    expect(provider.preferences!.optedIn, isFalse);
    expect(provider.permissionState, 'granted');
    expect(provider.subscriptions, isEmpty);
    expect(provider.history, isEmpty);
  });

  test('savePreferences updates preferences and handles errors', () async {
    final repo = FakeWebPushNotificationsRepository();
    final client = FakeWebPushBrowserClient();
    final provider = WebPushNotificationsProvider(
      repository: repo,
      browserClient: client,
    );

    const updated = WebPushPreferencesModel(
      optedIn: true,
      minMarginOfSafetyPct: 30.0,
      onlyCustodyTickers: false,
    );

    final ok = await provider.savePreferences(updated);
    expect(ok, isTrue);
    expect(provider.preferences!.optedIn, isTrue);
    expect(provider.preferences!.minMarginOfSafetyPct, 30.0);

    repo.shouldThrowOnSave = true;
    final fail = await provider.savePreferences(updated);
    expect(fail, isFalse);
    expect(provider.errorMessage, 'Margem de segurança inválida.');
  });

  test('registerCurrentBrowser registers subscription and refreshes list',
      () async {
    final repo = FakeWebPushNotificationsRepository();
    final client = FakeWebPushBrowserClient();
    final provider = WebPushNotificationsProvider(
      repository: repo,
      browserClient: client,
    );

    final ok = await provider.registerCurrentBrowser();
    expect(ok, isTrue);
    expect(provider.subscriptions.length, 1);
    expect(provider.subscriptions.first.id, 'sub-1');
  });

  test('deleteSubscription removes subscription from list', () async {
    final repo = FakeWebPushNotificationsRepository();
    final client = FakeWebPushBrowserClient();
    final provider = WebPushNotificationsProvider(
      repository: repo,
      browserClient: client,
    );

    await provider.registerCurrentBrowser();
    expect(provider.subscriptions.length, 1);

    final ok = await provider.deleteSubscription('sub-1');
    expect(ok, isTrue);
    expect(provider.subscriptions, isEmpty);
  });

  test('sendTestAlert triggers test and updates history', () async {
    final repo = FakeWebPushNotificationsRepository();
    final client = FakeWebPushBrowserClient();
    final provider = WebPushNotificationsProvider(
      repository: repo,
      browserClient: client,
    );

    final result = await provider.sendTestAlert();
    expect(result, isNotNull);
    expect(result!.sentCount, 1);
    expect(provider.history.length, 1);
    expect(provider.history.first.eventType, 'TEST_ALERT');
  });
}
