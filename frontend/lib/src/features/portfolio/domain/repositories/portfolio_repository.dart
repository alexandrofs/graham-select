import '../entities/portfolio_summary.dart';
import '../entities/trade.dart';
import '../entities/custody_position.dart';
import '../entities/monthly_evolution.dart';

abstract class PortfolioRepository {
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
  });

  Future<PortfolioSummary> getSummary({String? tagId});
  Future<({
    List<CustodyPosition> positions,
    DateTime? metaPriceUpdatedAt,
    int untaggedAssetsCount,
    List<String> untaggedTickers,
  })> getCustodyPositions({String? tagId});
  Future<PortfolioEvolution> getPortfolioEvolution({String? tagId});
}
