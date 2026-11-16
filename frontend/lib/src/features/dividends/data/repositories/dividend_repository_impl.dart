import '../../domain/entities/dividend_announcement.dart';
import '../../domain/entities/relevant_fact.dart';
import '../../domain/repositories/dividend_repository.dart';
import '../datasources/dividend_remote_data_source.dart';

class DividendRepositoryImpl implements DividendRepository {
  final DividendRemoteDataSource remoteDataSource;

  DividendRepositoryImpl({required this.remoteDataSource});

  @override
  Future<List<DividendAnnouncement>> getDividendSchedule({
    String? ticker,
    DateTime? startDate,
    DateTime? endDate,
    String? status,
    bool onlyMyTickers = false,
  }) {
    return remoteDataSource.getDividendSchedule(
      ticker: ticker,
      startDate: startDate,
      endDate: endDate,
      status: status,
      onlyMyTickers: onlyMyTickers,
    );
  }

  @override
  Future<List<RelevantFact>> getRelevantFacts({
    String? ticker,
    int limit = 20,
    bool onlyMyTickers = false,
  }) {
    return remoteDataSource.getRelevantFacts(
      ticker: ticker,
      limit: limit,
      onlyMyTickers: onlyMyTickers,
    );
  }
}
