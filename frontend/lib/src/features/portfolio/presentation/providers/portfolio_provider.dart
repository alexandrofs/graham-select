import 'package:flutter/foundation.dart';
import '../../domain/entities/portfolio_summary.dart';
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
}
