import 'dart:async';
import 'package:flutter/foundation.dart';
import '../../../../core/security/encrypted_portfolio_cache.dart';
import '../../data/datasources/notification_service.dart';
import '../../domain/entities/portfolio_summary.dart';
import '../../domain/entities/custody_position.dart';
import '../../domain/entities/monthly_evolution.dart';
import '../../domain/repositories/portfolio_repository.dart';

enum PortfolioStatus { initial, loading, success, error }
enum TradeStatus { initial, loading, success, error }

class PortfolioProvider extends ChangeNotifier {
  final PortfolioRepository repository;
  final NotificationService? notificationService;
  final EncryptedPortfolioCache? cache;
  final String? Function()? userIdProvider;

  PortfolioProvider(
    this.repository, [
    this.notificationService,
    this.cache,
    this.userIdProvider,
  ]);

  PortfolioStatus _status = PortfolioStatus.initial;
  PortfolioStatus get status => _status;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  // --- Estados independentes para criação de trade manual ---
  TradeStatus _tradeStatus = TradeStatus.initial;
  TradeStatus get tradeStatus => _tradeStatus;

  String? _tradeError;
  String? get tradeError => _tradeError;

  PortfolioSummary? _summary;
  PortfolioSummary? get summary => _summary;

  PortfolioSummary? _globalSummary;
  PortfolioSummary? get globalSummary => _globalSummary;

  String? _activeTagId;
  String? get activeTagId => _activeTagId;

  bool _isOfflineCache = false;
  bool get isOfflineCache => _isOfflineCache;

  DateTime? _lastSyncedAt;
  DateTime? get lastSyncedAt => _lastSyncedAt;

  int _summaryGeneration = 0;
  int _custodyGeneration = 0;
  int _evolutionGeneration = 0;

  String? _normalizeTagId(String? tagId) {
    final trimmed = tagId?.trim();
    return (trimmed == null || trimmed.isEmpty) ? null : trimmed;
  }

  Future<void> loadSummary({String? tagId}) async {
    final normalizedTagId = _normalizeTagId(tagId);
    _activeTagId = normalizedTagId;
    final gen = ++_summaryGeneration;

    _status = PortfolioStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await repository.getSummary(tagId: normalizedTagId);
      if (gen != _summaryGeneration) return;
      _summary = result;
      if (normalizedTagId == null) {
        _globalSummary = result;
      }
      _status = PortfolioStatus.success;
      _isOfflineCache = false;
      _lastSyncedAt = DateTime.now().toUtc();
      try {
        await cache?.saveSummary(
          summary: result,
          userId: userIdProvider?.call(),
          tagId: normalizedTagId,
        );
      } catch (_) {
        // Erro no cache local não deve impedir exibição dos dados online
      }
    } catch (e) {
      if (gen != _summaryGeneration) return;
      final snapshot = await cache?.loadSnapshot(
        userId: userIdProvider?.call(),
        tagId: normalizedTagId,
      );
      if (gen != _summaryGeneration) return;
      if (snapshot?.summary != null) {
        _summary = snapshot!.summary;
        if (normalizedTagId == null) {
          _globalSummary = snapshot.summary;
        }
        _status = PortfolioStatus.success;
        _errorMessage = null;
        _isOfflineCache = true;
        _lastSyncedAt = snapshot.lastSyncedAt;
      } else {
        _status = PortfolioStatus.error;
        _isOfflineCache = false;
        _errorMessage = e.toString().replaceFirst('Exception: ', '');
      }
    }
    notifyListeners();
  }

  Future<void> refreshGlobalSummary() async {
    try {
      _globalSummary = await repository.getSummary();
      notifyListeners();
    } catch (_) {
      // Silencioso para não sobrescrever feedback visual do escopo ativo
    }
  }

  Future<void> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
  }) async {
    _tradeStatus = TradeStatus.loading;
    _tradeError = null;
    notifyListeners();

    try {
      await repository.createManualTrade(
        ticker: ticker,
        side: side,
        tradeDate: tradeDate,
        quantity: quantity,
        price: price,
        broker: broker,
      );
      _tradeStatus = TradeStatus.success;
    } catch (e) {
      _tradeStatus = TradeStatus.error;
      _tradeError = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  void resetStatus() {
    _status = PortfolioStatus.initial;
    _errorMessage = null;
    notifyListeners();
  }

  void resetTradeStatus() {
    _tradeStatus = TradeStatus.initial;
    _tradeError = null;
    notifyListeners();
  }

  // --- Estados de Custódia ---
  List<CustodyPosition> _custodyPositions = [];
  List<CustodyPosition> get rawCustodyPositions => _custodyPositions;
  
  String? _selectedAssetClassFilter;
  String? get selectedAssetClassFilter => _selectedAssetClassFilter;

  void selectAssetClassFilter(String? assetClass) {
    _selectedAssetClassFilter = assetClass;
    notifyListeners();
  }
  
  List<CustodyPosition> get custodyPositions {
    Iterable<CustodyPosition> filtered = _custodyPositions;
    if (_selectedAssetClassFilter != null) {
      filtered = filtered.where((p) => p.assetClass == _selectedAssetClassFilter);
    }
    final sorted = List<CustodyPosition>.from(filtered);
    if (_sortColumn.isNotEmpty) {
      sorted.sort((a, b) {
        dynamic valA = _getValueByColumn(a, _sortColumn);
        dynamic valB = _getValueByColumn(b, _sortColumn);
        
        int cmp = 0;
        if (valA is String && valB is String) {
          cmp = valA.compareTo(valB);
        } else if (valA is num && valB is num) {
          cmp = valA.compareTo(valB);
        } else if (valA is DateTime && valB is DateTime) {
          cmp = valA.compareTo(valB);
        } else if (valA == null && valB != null) {
          cmp = -1;
        } else if (valA != null && valB == null) {
          cmp = 1;
        }
        
        return _sortAscending ? cmp : -cmp;
      });
    }
    return sorted;
  }

  PortfolioStatus _custodyStatus = PortfolioStatus.initial;
  PortfolioStatus get custodyStatus => _custodyStatus;

  String? _custodyError;
  String? get custodyError => _custodyError;

  /// Timestamp autoritativo vindo do backend (meta.priceUpdatedAt).
  /// Null quando todas as posições são CACHE sem cotação real.
  DateTime? _custodyMetaPriceUpdatedAt;
  DateTime? get custodyMetaPriceUpdatedAt => _custodyMetaPriceUpdatedAt;

  int _untaggedAssetsCount = 0;
  int get untaggedAssetsCount => _untaggedAssetsCount;

  List<String> _untaggedTickers = const [];
  List<String> get untaggedTickers => List.unmodifiable(_untaggedTickers);

  String _sortColumn = 'ticker';
  String get sortColumn => _sortColumn;

  bool _sortAscending = true;
  bool get sortAscending => _sortAscending;

  Future<void> loadCustodyPositions({String? tagId}) async {
    final normalizedTagId = _normalizeTagId(tagId);
    _activeTagId = normalizedTagId;
    final gen = ++_custodyGeneration;

    _custodyStatus = PortfolioStatus.loading;
    _custodyError = null;
    notifyListeners();

    try {
      final result = await repository.getCustodyPositions(tagId: normalizedTagId);
      if (gen != _custodyGeneration) return;
      _custodyPositions = result.positions;
      _custodyMetaPriceUpdatedAt = result.metaPriceUpdatedAt;
      _untaggedAssetsCount = result.untaggedAssetsCount;
      _untaggedTickers = result.untaggedTickers;
      if (_selectedAssetClassFilter != null &&
          !_custodyPositions.any((p) => p.assetClass == _selectedAssetClassFilter)) {
        _selectedAssetClassFilter = null;
      }
      _custodyStatus = PortfolioStatus.success;
      _isOfflineCache = false;
      _lastSyncedAt = DateTime.now().toUtc();
      try {
        await cache?.saveCustody(
          positions: result.positions,
          metaPriceUpdatedAt: result.metaPriceUpdatedAt,
          untaggedAssetsCount: result.untaggedAssetsCount,
          untaggedTickers: result.untaggedTickers,
          userId: userIdProvider?.call(),
          tagId: normalizedTagId,
        );
      } catch (_) {
        // Silencioso para não bloquear UI online
      }
    } catch (e) {
      if (gen != _custodyGeneration) return;
      final snapshot = await cache?.loadSnapshot(
        userId: userIdProvider?.call(),
        tagId: normalizedTagId,
      );
      if (gen != _custodyGeneration) return;
      if (snapshot?.positions != null) {
        _custodyPositions = snapshot!.positions!;
        _custodyMetaPriceUpdatedAt = snapshot.metaPriceUpdatedAt;
        _untaggedAssetsCount = snapshot.untaggedAssetsCount;
        _untaggedTickers = snapshot.untaggedTickers;
        if (_selectedAssetClassFilter != null &&
            !_custodyPositions.any((p) => p.assetClass == _selectedAssetClassFilter)) {
          _selectedAssetClassFilter = null;
        }
        _custodyStatus = PortfolioStatus.success;
        _custodyError = null;
        _isOfflineCache = true;
        _lastSyncedAt = snapshot.lastSyncedAt;
      } else {
        _custodyStatus = PortfolioStatus.error;
        _isOfflineCache = false;
        _custodyError = e.toString().replaceFirst('Exception: ', '');
      }
    }
    notifyListeners();
  }

  void sortBy(String column) {
    if (_sortColumn == column) {
      _sortAscending = !_sortAscending;
    } else {
      _sortColumn = column;
      _sortAscending = true;
    }
    notifyListeners();
  }

  dynamic _getValueByColumn(CustodyPosition pos, String column) {
    switch (column) {
      case 'ticker':
        return pos.ticker;
      case 'quantity':
        return pos.quantity;
      case 'averagePrice':
        return pos.averagePrice;
      case 'currentPrice':
        return pos.currentPrice;
      case 'marketValue':
        return pos.marketValue;
      case 'gainLossPercentage':
        return pos.gainLossPercentage;
      case 'priceUpdatedAt':
        return pos.priceUpdatedAt;
      default:
        return null;
    }
  }

  // --- Estados de Evolução (Histórico Proventos e Aportes) ---
  PortfolioStatus _evolutionStatus = PortfolioStatus.initial;
  PortfolioStatus get evolutionStatus => _evolutionStatus;

  String? _evolutionError;
  String? get evolutionError => _evolutionError;

  PortfolioEvolution? _evolutionData;
  PortfolioEvolution? get evolutionData => _evolutionData;

  Future<void> loadEvolutionData({String? tagId}) async {
    final normalizedTagId = _normalizeTagId(tagId);
    _activeTagId = normalizedTagId;
    final gen = ++_evolutionGeneration;

    _evolutionStatus = PortfolioStatus.loading;
    _evolutionError = null;
    notifyListeners();

    try {
      final result = await repository.getPortfolioEvolution(tagId: normalizedTagId);
      if (gen != _evolutionGeneration) return;
      _evolutionData = result;
      _evolutionStatus = PortfolioStatus.success;
    } catch (e) {
      if (gen != _evolutionGeneration) return;
      _evolutionStatus = PortfolioStatus.error;
      _evolutionError = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  // --- Live Updates (SSE) ---
  StreamSubscription? _notificationSubscription;
  bool _isReceivingLiveUpdates = false;
  bool get isReceivingLiveUpdates => _isReceivingLiveUpdates;

  void startListeningForUpdates() {
    if (notificationService == null) return;
    
    _notificationSubscription?.cancel();
    notificationService!.connect();
    _isReceivingLiveUpdates = true;
    notifyListeners();

    _notificationSubscription = notificationService!.notificationStream.listen((event) {
      if (event.event == 'PORTFOLIO_UPDATED') {
        Future.wait([
          loadSummary(tagId: _activeTagId),
          if (_activeTagId != null) refreshGlobalSummary(),
          loadCustodyPositions(tagId: _activeTagId),
          loadEvolutionData(tagId: _activeTagId),
        ]);
      }
    });
  }

  void stopListeningForUpdates() {
    _notificationSubscription?.cancel();
    _notificationSubscription = null;
    notificationService?.dispose();
    _isReceivingLiveUpdates = false;
    notifyListeners();
  }

  /// Limpa todo o estado em memória do provider (ex.: no logout ou troca de conta).
  void reset() {
    _summaryGeneration++;
    _custodyGeneration++;
    _evolutionGeneration++;
    _summary = null;
    _globalSummary = null;
    _custodyPositions = [];
    _custodyMetaPriceUpdatedAt = null;
    _untaggedAssetsCount = 0;
    _untaggedTickers = const [];
    _evolutionData = null;
    _status = PortfolioStatus.initial;
    _custodyStatus = PortfolioStatus.initial;
    _evolutionStatus = PortfolioStatus.initial;
    _tradeStatus = TradeStatus.initial;
    _errorMessage = null;
    _custodyError = null;
    _evolutionError = null;
    _tradeError = null;
    _isOfflineCache = false;
    _lastSyncedAt = null;
    _activeTagId = null;
    _selectedAssetClassFilter = null;
    _notificationSubscription?.cancel();
    _notificationSubscription = null;
    _isReceivingLiveUpdates = false;
    notifyListeners();
  }

  @override
  void dispose() {
    _notificationSubscription?.cancel();
    super.dispose();
  }
}
