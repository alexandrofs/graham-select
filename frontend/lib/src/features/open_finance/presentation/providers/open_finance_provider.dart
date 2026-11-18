import 'package:flutter/foundation.dart';
import '../../domain/entities/open_finance_connection.dart';
import '../../domain/entities/open_finance_institution.dart';
import '../../domain/entities/open_finance_sync_result.dart';
import '../../domain/repositories/open_finance_repository.dart';

enum OpenFinanceState { initial, loading, loaded, error }

class OpenFinanceProvider extends ChangeNotifier {
  final OpenFinanceRepository repository;

  OpenFinanceProvider({required this.repository});

  OpenFinanceState _state = OpenFinanceState.initial;
  OpenFinanceState get state => _state;

  List<OpenFinanceConnection> _connections = [];
  List<OpenFinanceConnection> get connections => _connections;

  List<OpenFinanceInstitution> _institutions = [];
  List<OpenFinanceInstitution> get institutions => _institutions;

  final Set<String> _syncingConnectionIds = {};
  bool isConnectionSyncing(String id) => _syncingConnectionIds.contains(id);

  String? _selectedProfileId;
  String? get selectedProfileId => _selectedProfileId;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  OpenFinanceSyncResult? _lastSyncResult;
  OpenFinanceSyncResult? get lastSyncResult => _lastSyncResult;

  void setSelectedProfileId(String? profileId) {
    _selectedProfileId = profileId;
    notifyListeners();
  }

  Future<void> fetchConnections({bool silent = false}) async {
    if (!silent) {
      _state = OpenFinanceState.loading;
      _errorMessage = null;
      notifyListeners();
    }

    try {
      _connections = await repository.getConnections();
      _state = OpenFinanceState.loaded;
      _errorMessage = null;
    } catch (e) {
      _errorMessage = 'Erro ao carregar conexões Open Finance: $e';
      _state = OpenFinanceState.error;
    } finally {
      notifyListeners();
    }
  }

  Future<void> fetchInstitutions() async {
    try {
      _institutions = await repository.getInstitutions();
      notifyListeners();
    } catch (e) {
      _errorMessage = 'Erro ao carregar instituições: $e';
      notifyListeners();
    }
  }

  Future<String?> initiateConnection(String institutionId, {String? redirectUri}) async {
    try {
      _errorMessage = null;
      notifyListeners();

      final result = await repository.initiateConnection(
        institutionId: institutionId,
        profileId: _selectedProfileId,
        redirectUri: redirectUri,
      );

      final consentUrl = result['consentUrl'] as String?;
      await fetchConnections(silent: true);
      return consentUrl;
    } catch (e) {
      _errorMessage = 'Erro ao iniciar conexão: $e';
      notifyListeners();
      return null;
    }
  }

  Future<OpenFinanceConnection?> completeConsentCallback({
    required String connectionId,
    required String code,
    required String state,
    String? redirectUri,
  }) async {
    try {
      _errorMessage = null;
      notifyListeners();

      final connection = await repository.completeConsentCallback(
        connectionId: connectionId,
        code: code,
        state: state,
        redirectUri: redirectUri,
      );
      await fetchConnections(silent: true);
      return connection;
    } catch (e) {
      _errorMessage = 'Erro ao concluir autorização de consentimento: $e';
      notifyListeners();
      return null;
    }
  }

  Future<OpenFinanceSyncResult?> syncConnection(String connectionId) async {
    _syncingConnectionIds.add(connectionId);
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await repository.syncConnection(connectionId);
      _lastSyncResult = result;
      await fetchConnections(silent: true);
      return result;
    } catch (e) {
      _errorMessage = 'Erro ao sincronizar conexão: $e';
      await fetchConnections(silent: true);
      return null;
    } finally {
      _syncingConnectionIds.remove(connectionId);
      notifyListeners();
    }
  }

  Future<bool> revokeConnection(String connectionId) async {
    try {
      _errorMessage = null;
      notifyListeners();

      await repository.revokeConnection(connectionId);
      await fetchConnections(silent: true);
      return true;
    } catch (e) {
      _errorMessage = 'Erro ao desconectar instituição: $e';
      notifyListeners();
      return false;
    }
  }

  void clearMessages() {
    _errorMessage = null;
    _lastSyncResult = null;
    notifyListeners();
  }
}
