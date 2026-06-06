import 'dart:async';
import 'package:flutter/foundation.dart';
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

  PortfolioProvider(this.repository, [this.notificationService]);

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

  Future<void> loadSummary() async {
    _status = PortfolioStatus.loading;
    _errorMessage = null;
    notifyListeners();

    try {
      _summary = await repository.getSummary();
      _status = PortfolioStatus.success;
    } catch (e) {
      _status = PortfolioStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
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

  String _sortColumn = 'ticker';
  String get sortColumn => _sortColumn;

  bool _sortAscending = true;
  bool get sortAscending => _sortAscending;

  Future<void> loadCustodyPositions() async {
    _custodyStatus = PortfolioStatus.loading;
    _custodyError = null;
    notifyListeners();

    try {
      final result = await repository.getCustodyPositions();
      _custodyPositions = result.positions;
      _custodyMetaPriceUpdatedAt = result.metaPriceUpdatedAt;
      _custodyStatus = PortfolioStatus.success;
    } catch (e) {
      _custodyStatus = PortfolioStatus.error;
      _custodyError = e.toString().replaceFirst('Exception: ', '');
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

  Future<void> loadEvolutionData() async {
    _evolutionStatus = PortfolioStatus.loading;
    _evolutionError = null;
    notifyListeners();

    try {
      _evolutionData = await repository.getPortfolioEvolution();
      _evolutionStatus = PortfolioStatus.success;
    } catch (e) {
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
          loadSummary(),
          loadCustodyPositions(),
          loadEvolutionData(),
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

  @override
  void dispose() {
    _notificationSubscription?.cancel();
    super.dispose();
  }
}
