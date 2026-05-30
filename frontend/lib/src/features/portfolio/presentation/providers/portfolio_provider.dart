import 'package:flutter/foundation.dart';
import '../../domain/entities/portfolio_summary.dart';
import '../../domain/entities/custody_position.dart';
import '../../domain/repositories/portfolio_repository.dart';

enum PortfolioStatus { initial, loading, success, error }

class PortfolioProvider extends ChangeNotifier {
  final PortfolioRepository repository;

  PortfolioProvider(this.repository);

  PortfolioStatus _status = PortfolioStatus.initial;
  PortfolioStatus get status => _status;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

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
    _status = PortfolioStatus.loading;
    _errorMessage = null;
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
      _status = PortfolioStatus.success;
    } catch (e) {
      _status = PortfolioStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }

  void resetStatus() {
    _status = PortfolioStatus.initial;
    _errorMessage = null;
    notifyListeners();
  }

  // --- Estados de Custódia ---
  List<CustodyPosition> _custodyPositions = [];
  
  List<CustodyPosition> get custodyPositions {
    final sorted = List<CustodyPosition>.from(_custodyPositions);
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

  String _sortColumn = 'ticker';
  String get sortColumn => _sortColumn;

  bool _sortAscending = true;
  bool get sortAscending => _sortAscending;

  Future<void> loadCustodyPositions() async {
    _custodyStatus = PortfolioStatus.loading;
    _custodyError = null;
    notifyListeners();

    try {
      _custodyPositions = await repository.getCustodyPositions();
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
}
