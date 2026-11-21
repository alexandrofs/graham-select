import 'package:flutter/material.dart';
import 'package:frontend/src/features/notifications/data/repositories/web_push_notifications_repository.dart';
import 'package:frontend/src/features/notifications/data/services/web_push_browser_client.dart';
import 'package:frontend/src/features/notifications/domain/models/web_push_notification_models.dart';

class WebPushNotificationsProvider extends ChangeNotifier {
  final WebPushNotificationsRepository repository;
  final WebPushBrowserClient browserClient;

  WebPushNotificationsProvider({
    required this.repository,
    required this.browserClient,
  });

  bool _isLoading = false;
  bool _isSaving = false;
  bool _isTesting = false;
  bool _isRegistering = false;
  String? _deletingSubscriptionId;
  String? _errorMessage;
  String _permissionState = 'unsupported';
  WebPushPreferencesModel? _preferences;
  List<WebPushSubscriptionModel> _subscriptions = [];
  List<WebPushNotificationLogModel> _history = [];

  bool get isLoading => _isLoading;
  bool get isSaving => _isSaving;
  bool get isTesting => _isTesting;
  bool get isRegistering => _isRegistering;
  String? get deletingSubscriptionId => _deletingSubscriptionId;
  String? get errorMessage => _errorMessage;
  String get permissionState => _permissionState;
  WebPushPreferencesModel? get preferences => _preferences;
  List<WebPushSubscriptionModel> get subscriptions => _subscriptions;
  List<WebPushNotificationLogModel> get history => _history;

  Future<void> loadAll() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final results = await Future.wait([
        repository.getPreferences(),
        repository.getSubscriptions(),
        repository.getHistory(),
        browserClient.getPermissionState(),
      ]);

      _preferences = results[0] as WebPushPreferencesModel;
      _subscriptions = results[1] as List<WebPushSubscriptionModel>;
      _history = results[2] as List<WebPushNotificationLogModel>;
      _permissionState = results[3] as String;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> savePreferences(WebPushPreferencesModel model) async {
    _isSaving = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _preferences = await repository.updatePreferences(model);
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      _isSaving = false;
      notifyListeners();
    }
  }

  Future<bool> registerCurrentBrowser() async {
    _isRegistering = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final vapid = await repository.getVapidPublicKey();
      final payload = await browserClient.subscribeToPush(vapid.publicKey);
      _permissionState = await browserClient.getPermissionState();

      if (payload == null) {
        if (_permissionState == 'denied') {
          _errorMessage =
              'Permissão de notificação negada no navegador. Habilite nas configurações do site.';
        } else {
          _errorMessage =
              'Não foi possível ativar notificações neste navegador ou ambiente.';
        }
        return false;
      }

      await repository.registerSubscription(
        endpoint: payload.endpoint,
        p256dhKey: payload.p256dhKey,
        authKey: payload.authKey,
        userAgent: payload.userAgent,
      );

      _subscriptions = await repository.getSubscriptions();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      _isRegistering = false;
      notifyListeners();
    }
  }

  Future<bool> deleteSubscription(String id) async {
    _deletingSubscriptionId = id;
    _errorMessage = null;
    notifyListeners();

    try {
      await repository.deleteSubscription(id);
      _subscriptions.removeWhere((s) => s.id == id);
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      _deletingSubscriptionId = null;
      notifyListeners();
    }
  }

  Future<WebPushTestAlertResultModel?> sendTestAlert() async {
    _isTesting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await repository.sendTestAlert();
      try {
        _history = await repository.getHistory();
      } catch (_) {
        // Falha no histórico não invalida sucesso do envio de teste
      }
      return result;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return null;
    } finally {
      _isTesting = false;
      notifyListeners();
    }
  }
}
