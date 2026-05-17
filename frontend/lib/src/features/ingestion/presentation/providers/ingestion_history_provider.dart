import 'dart:async';
import 'package:flutter/material.dart';
import '../../domain/entities/ingestion_audit.dart';
import '../../domain/repositories/ingestion_repository.dart';

enum IngestionHistoryState { initial, loading, loaded, error }

class IngestionHistoryProvider extends ChangeNotifier {
  final IngestionRepository repository;
  Timer? _pollingTimer;

  IngestionHistoryProvider(this.repository);

  List<IngestionAudit> _history = [];
  List<IngestionAudit> get history => _history;

  IngestionHistoryState _state = IngestionHistoryState.initial;
  IngestionHistoryState get state => _state;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  Future<void> fetchHistory({bool isPolling = false}) async {
    if (!isPolling) {
      _state = IngestionHistoryState.loading;
      notifyListeners();
    }

    try {
      _history = await repository.getHistory();
      _state = IngestionHistoryState.loaded;
      _checkPolling();
    } catch (e) {
      if (!isPolling) {
        _errorMessage = e.toString();
        _state = IngestionHistoryState.error;
      }
    } finally {
      notifyListeners();
    }
  }

  void _checkPolling() {
    final hasProcessing = _history.any((a) => a.status == 'PROCESSANDO');
    if (hasProcessing && _pollingTimer == null) {
      _pollingTimer = Timer.periodic(const Duration(seconds: 5), (timer) {
        fetchHistory(isPolling: true);
      });
    } else if (!hasProcessing && _pollingTimer != null) {
      _pollingTimer?.cancel();
      _pollingTimer = null;
    }
  }

  @override
  void dispose() {
    _pollingTimer?.cancel();
    super.dispose();
  }
}
