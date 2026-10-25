import 'package:flutter/foundation.dart';
import '../../data/datasources/admin_remote_datasource.dart';
import '../../domain/entities/admin_dlq_message.dart';
import '../../domain/repositories/admin_repository.dart';

enum AdminDlqState { initial, loading, loaded, accessDenied, error }

class AdminDlqProvider extends ChangeNotifier {
  final AdminRepository repository;

  AdminDlqState _status = AdminDlqState.initial;
  List<AdminDlqMessage> _failures = [];
  final Set<int> _selectedIds = {};
  final Set<int> _reprocessingItemIds = {};
  String? _errorMessage;
  bool _disposed = false;

  AdminDlqProvider({required this.repository});

  AdminDlqState get status => _status;
  List<AdminDlqMessage> get failures => List.unmodifiable(_failures);
  Set<int> get selectedIds => Set.unmodifiable(_selectedIds);
  Set<int> get reprocessingItemIds => Set.unmodifiable(_reprocessingItemIds);
  String? get errorMessage => _errorMessage;

  bool get isAllSelected => _failures.isNotEmpty && _selectedIds.length == _failures.length;
  int get selectedCount => _selectedIds.length;
  bool get hasSelection => _selectedIds.isNotEmpty;

  bool isSelected(int id) => _selectedIds.contains(id);
  bool isItemReprocessing(int id) => _reprocessingItemIds.contains(id);

  Future<void> loadFailures({String status = 'PENDING'}) async {
    _status = AdminDlqState.loading;
    _errorMessage = null;
    if (!_disposed) notifyListeners();

    try {
      final list = await repository.getDlqFailures(status: status);
      _failures = List.of(list);
      _selectedIds.removeWhere((id) => !_failures.any((f) => f.id == id));
      _status = AdminDlqState.loaded;
    } on AdminAccessDeniedException {
      _status = AdminDlqState.accessDenied;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
    } catch (e) {
      _status = AdminDlqState.error;
      _errorMessage = e.toString();
    } finally {
      if (!_disposed) notifyListeners();
    }
  }

  void toggleSelection(int id) {
    if (_selectedIds.contains(id)) {
      _selectedIds.remove(id);
    } else {
      _selectedIds.add(id);
    }
    if (!_disposed) notifyListeners();
  }

  void selectAll() {
    if (isAllSelected) {
      _selectedIds.clear();
    } else {
      _selectedIds.addAll(_failures.map((f) => f.id));
    }
    if (!_disposed) notifyListeners();
  }

  void clearSelection() {
    _selectedIds.clear();
    if (!_disposed) notifyListeners();
  }

  Future<bool> reprocessItem(int id) async {
    if (_reprocessingItemIds.contains(id)) return false;

    _reprocessingItemIds.add(id);
    if (!_disposed) notifyListeners();

    try {
      await repository.reprocessDlqMessage(id);
      _failures.removeWhere((f) => f.id == id);
      _selectedIds.remove(id);
      return true;
    } on AdminAccessDeniedException {
      _status = AdminDlqState.accessDenied;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst(RegExp(r'^Exception:\s*'), '');
      return false;
    } finally {
      _reprocessingItemIds.remove(id);
      if (!_disposed) notifyListeners();
    }
  }

  Future<bool> reprocessSelected() async {
    if (_selectedIds.isEmpty || _reprocessingItemIds.isNotEmpty) return false;

    final idsToReprocess = _selectedIds.toList();
    _reprocessingItemIds.addAll(idsToReprocess);
    if (!_disposed) notifyListeners();

    try {
      final result = await repository.reprocessBatchDlqMessages(idsToReprocess);
      
      final details = result['details'];
      int resolvedCount = 0;
      if (details is List) {
        for (final item in details) {
          if (item is Map && (item['status'] == 'RESOLVED' || item['status'] == 'resolved')) {
            final id = item['id'];
            if (id is num) {
              _failures.removeWhere((f) => f.id == id.toInt());
              _selectedIds.remove(id.toInt());
              resolvedCount++;
            }
          }
        }
      } else {
        _failures.removeWhere((f) => idsToReprocess.contains(f.id));
        _selectedIds.clear();
        resolvedCount = idsToReprocess.length;
      }

      final failureCount = result['failureCount'] as num? ?? 0;
      if (failureCount > 0 && resolvedCount == 0) {
        _errorMessage = 'Todas as falhas selecionadas falharam no reprocessamento.';
        return false;
      }
      return true;
    } on AdminAccessDeniedException {
      _status = AdminDlqState.accessDenied;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst(RegExp(r'^Exception:\s*'), '');
      return false;
    } finally {
      _reprocessingItemIds.removeAll(idsToReprocess);
      if (!_disposed) notifyListeners();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
