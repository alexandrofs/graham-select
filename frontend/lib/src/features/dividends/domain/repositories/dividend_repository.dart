import '../entities/dividend_announcement.dart';
import '../entities/relevant_fact.dart';

abstract class DividendRepository {
  Future<List<DividendAnnouncement>> getDividendSchedule({
    String? ticker,
    DateTime? startDate,
    DateTime? endDate,
    String? status,
    bool onlyMyTickers = false,
  });

  Future<List<RelevantFact>> getRelevantFacts({
    String? ticker,
    int limit = 20,
    bool onlyMyTickers = false,
  });
}
