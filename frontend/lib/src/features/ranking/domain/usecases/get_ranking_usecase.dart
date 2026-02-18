import '../entities/ranked_company.dart';
import '../repositories/ranking_repository.dart';

class GetRankingUseCase {
  final RankingRepository repository;

  GetRankingUseCase(this.repository);

  Future<List<RankedCompany>> call() {
    return repository.getRankedCompanies();
  }
}
