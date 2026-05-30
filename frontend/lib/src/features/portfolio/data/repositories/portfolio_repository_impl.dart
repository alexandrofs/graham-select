import '../../domain/entities/portfolio_summary.dart';
import '../../domain/entities/trade.dart';
import '../../domain/entities/custody_position.dart';
import '../../domain/entities/monthly_evolution.dart';
import '../../domain/repositories/portfolio_repository.dart';
import '../datasources/portfolio_remote_data_source.dart';
import '../../../auth/data/auth_repository.dart';

class PortfolioRepositoryImpl implements PortfolioRepository {
  final PortfolioRemoteDataSource remoteDataSource;
  final AuthRepository authRepository;

  PortfolioRepositoryImpl(this.remoteDataSource, this.authRepository);

  @override
  Future<PortfolioSummary> getSummary() async {
    final token = await authRepository.getPersistedToken();
    return remoteDataSource.getSummary(token: token);
  }

  @override
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
  }) async {
    final token = await authRepository.getPersistedToken();
    return remoteDataSource.createManualTrade(
      ticker: ticker,
      side: side,
      tradeDate: tradeDate,
      quantity: quantity,
      price: price,
      broker: broker,
      token: token,
    );
  }

  @override
  Future<({List<CustodyPosition> positions, DateTime? metaPriceUpdatedAt})>
      getCustodyPositions() async {
    final token = await authRepository.getPersistedToken();
    return remoteDataSource.getCustodyPositions(token: token);
  }

  @override
  Future<PortfolioEvolution> getPortfolioEvolution() async {
    final token = await authRepository.getPersistedToken();
    return remoteDataSource.getPortfolioEvolution(token: token);
  }
}
