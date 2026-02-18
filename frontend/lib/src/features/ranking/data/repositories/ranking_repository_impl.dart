import '../../domain/entities/ranked_company.dart';
import '../../domain/repositories/ranking_repository.dart';
import '../datasources/ranking_remote_data_source.dart';

class RankingRepositoryImpl implements RankingRepository {
  final RankingRemoteDataSource remoteDataSource;

  RankingRepositoryImpl(this.remoteDataSource);

  @override
  Future<List<RankedCompany>> getRankedCompanies() async {
    return await remoteDataSource.getRankedCompanies();
  }
}
