import 'package:flutter/material.dart';
import 'package:frontend/src/features/notifications/data/repositories/whatsapp_notifications_repository.dart';
import 'package:frontend/src/features/notifications/domain/models/whatsapp_notification_models.dart';

class WhatsappNotificationsProvider extends ChangeNotifier {
  final WhatsappNotificationsRepository repository;

  WhatsappNotificationsProvider({required this.repository});

  bool _isLoading = false;
  bool _isSaving = false;
  bool _isTesting = false;
  String? _errorMessage;
  WhatsappPreferencesModel? _preferences;
  List<WhatsappNotificationLogModel> _history = [];

  bool get isLoading => _isLoading;
  bool get isSaving => _isSaving;
  bool get isTesting => _isTesting;
  String? get errorMessage => _errorMessage;
  WhatsappPreferencesModel? get preferences => _preferences;
  List<WhatsappNotificationLogModel> get history => _history;

  Future<void> loadAll() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final results = await Future.wait([
        repository.getPreferences(),
        repository.getHistory(),
      ]);
      _preferences = results[0] as WhatsappPreferencesModel;
      _history = results[1] as List<WhatsappNotificationLogModel>;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> savePreferences(WhatsappPreferencesModel model) async {
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

  Future<WhatsappTestAlertResultModel?> sendTestAlert() async {
    _isTesting = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await repository.sendTestAlert();
      try {
        _history = await repository.getHistory();
      } catch (_) {
        // Falha ao atualizar histórico não invalida o envio de teste concluído
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
