import 'package:flutter/foundation.dart';
import '../../data/datasources/admin_remote_datasource.dart';
import '../../domain/entities/admin_observability.dart';
import '../../domain/repositories/admin_repository.dart';

enum AdminObservabilityStatus {
  initial,
  loading,
  loaded,
  accessDenied,
  error,
}

class AdminObservabilityProvider extends ChangeNotifier {
  final AdminRepository repository;

  AdminObservabilityProvider({required this.repository});

  AdminObservabilityStatus _status = AdminObservabilityStatus.initial;
  AdminObservabilityStatus get status => _status;

  AdminObservability? _metrics;
  AdminObservability? get metrics => _metrics;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  bool _disposed = false;

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }

  void _safeNotifyListeners() {
    if (!_disposed) {
      notifyListeners();
    }
  }

  Future<void> loadMetrics() async {
    _status = AdminObservabilityStatus.loading;
    _errorMessage = null;
    _safeNotifyListeners();

    try {
      _metrics = await repository.getObservabilityMetrics();
      _status = AdminObservabilityStatus.loaded;
    } on AdminAccessDeniedException catch (e, stackTrace) {
      debugPrint(
        '[AdminObservabilityProvider] ERROR access denied: $e\n$stackTrace',
      );
      _status = AdminObservabilityStatus.accessDenied;
      _errorMessage = e.message;
    } catch (e, stackTrace) {
      debugPrint(
        '[AdminObservabilityProvider] ERROR loading metrics: $e\n$stackTrace',
      );
      _status = AdminObservabilityStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    } finally {
      _safeNotifyListeners();
    }
  }
}
