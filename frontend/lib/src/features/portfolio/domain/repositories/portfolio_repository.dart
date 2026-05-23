import '../entities/portfolio_summary.dart';
import '../entities/trade.dart';

abstract class PortfolioRepository {
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
  });

  Future<PortfolioSummary> getSummary();
}
