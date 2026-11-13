import 'package:flutter/foundation.dart';
import '../../data/datasources/admin_remote_datasource.dart';
import '../../domain/entities/admin_ticker.dart';
import '../../domain/repositories/admin_repository.dart';

class AdminTickerProvider extends ChangeNotifier {
  final AdminRepository repository;

  bool _isLoading = false;
  bool _isActionLoading = false;
  String? _errorMessage;
  bool _isAccessDenied = false;
  String _selectedAssetClassFilter = 'Todos';
  String _searchQuery = '';
  List<AdminTicker> _tickers = [];
  int _loadRequestId = 0;
  bool _disposed = false;

  AdminTickerProvider({required this.repository});

  bool get isLoading => _isLoading;
  bool get isActionLoading => _isActionLoading;
  String? get errorMessage => _errorMessage;
  bool get isAccessDenied => _isAccessDenied;
  String get selectedAssetClassFilter => _selectedAssetClassFilter;
  String get searchQuery => _searchQuery;
  List<AdminTicker> get tickers => List.unmodifiable(_tickers);

  List<AdminTicker> get filteredTickers {
    return _tickers.where((item) {
      final matchesQuery = _searchQuery.isEmpty ||
          item.ticker.toLowerCase().contains(_searchQuery.toLowerCase()) ||
          item.name.toLowerCase().contains(_searchQuery.toLowerCase());
      final matchesClass = _selectedAssetClassFilter == 'Todos' ||
          item.assetClass.toLowerCase() == _selectedAssetClassFilter.toLowerCase();
      return matchesQuery && matchesClass;
    }).toList();
  }

  void setSearchQuery(String query) {
    _searchQuery = query.trim();
    notifyListeners();
  }

  Future<void> setAssetClassFilter(String assetClass) async {
    _selectedAssetClassFilter = assetClass;
    notifyListeners();
    await loadTickers();
  }

  Future<void> loadTickers() async {
    final requestId = ++_loadRequestId;
    _isLoading = true;
    _errorMessage = null;
    _isAccessDenied = false;
    if (!_disposed) notifyListeners();

    try {
      final result = await repository.getTickers(
        assetClass: _selectedAssetClassFilter != 'Todos' ? _selectedAssetClassFilter : null,
      );
      if (requestId != _loadRequestId) return;
      _tickers = List.of(result);
    } on AdminAccessDeniedException {
      if (requestId != _loadRequestId) return;
      _isAccessDenied = true;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
    } catch (e) {
      if (requestId != _loadRequestId) return;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    } finally {
      if (requestId == _loadRequestId) {
        _isLoading = false;
        if (!_disposed) notifyListeners();
      }
    }
  }

  Future<bool> createTicker({
    required String ticker,
    required String name,
    required String assetClass,
    String? sector,
  }) async {
    _isActionLoading = true;
    _errorMessage = null;
    if (!_disposed) notifyListeners();

    try {
      final created = await repository.createTicker(
        ticker: ticker.trim().toUpperCase(),
        name: name.trim(),
        assetClass: assetClass.trim(),
        sector: sector?.trim().isNotEmpty == true ? sector!.trim() : null,
      );
      _tickers.removeWhere((t) => t.id == created.id || t.ticker.toUpperCase() == created.ticker.toUpperCase());
      _tickers.add(created);
      _tickers.sort((a, b) => a.ticker.compareTo(b.ticker));
      return true;
    } on AdminAccessDeniedException {
      _isAccessDenied = true;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      _isActionLoading = false;
      if (!_disposed) notifyListeners();
    }
  }

  Future<bool> updateTicker({
    required String id,
    required String name,
    required String assetClass,
    String? sector,
  }) async {
    _isActionLoading = true;
    _errorMessage = null;
    if (!_disposed) notifyListeners();

    try {
      final updated = await repository.updateTicker(
        id: id,
        name: name.trim(),
        assetClass: assetClass.trim(),
        sector: sector?.trim().isNotEmpty == true ? sector!.trim() : null,
      );
      final index = _tickers.indexWhere((t) => t.id == id);
      if (index != -1) {
        _tickers[index] = updated;
      } else {
        _tickers.add(updated);
      }
      _tickers.sort((a, b) => a.ticker.compareTo(b.ticker));
      return true;
    } on AdminAccessDeniedException {
      _isAccessDenied = true;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      _isActionLoading = false;
      if (!_disposed) notifyListeners();
    }
  }

  Future<bool> deleteTicker(String id) async {
    _isActionLoading = true;
    _errorMessage = null;
    if (!_disposed) notifyListeners();

    try {
      await repository.deleteTicker(id);
      _tickers.removeWhere((t) => t.id == id);
      return true;
    } on AdminAccessDeniedException {
      _isAccessDenied = true;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      _isActionLoading = false;
      if (!_disposed) notifyListeners();
    }
  }

  Future<bool> clearCache() async {
    _isActionLoading = true;
    _errorMessage = null;
    if (!_disposed) notifyListeners();

    try {
      await repository.clearMarketDataCache();
      return true;
    } on AdminAccessDeniedException {
      _isAccessDenied = true;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      _isActionLoading = false;
      if (!_disposed) notifyListeners();
    }
  }

  Future<bool> evictTicker(String ticker) async {
    _isActionLoading = true;
    _errorMessage = null;
    if (!_disposed) notifyListeners();

    try {
      await repository.evictTickerCache(ticker);
      return true;
    } on AdminAccessDeniedException {
      _isAccessDenied = true;
      _errorMessage = 'Acesso restrito a administradores do sistema.';
      return false;
    } catch (e) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      return false;
    } finally {
      _isActionLoading = false;
      if (!_disposed) notifyListeners();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
