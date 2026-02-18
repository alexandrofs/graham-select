import '../entities/ranked_company.dart';

abstract class RankingRepository {
  Future<List<RankedCompany>> getRankedCompanies();
}
