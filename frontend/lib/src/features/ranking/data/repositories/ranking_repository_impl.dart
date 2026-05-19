import '../../domain/entities/ranked_company.dart';
import '../../domain/repositories/ranking_repository.dart';
import '../datasources/ranking_remote_data_source.dart';
import '../../../auth/data/auth_repository.dart';

class RankingRepositoryImpl implements RankingRepository {
  final RankingRemoteDataSource remoteDataSource;
  final AuthRepository authRepository;

  RankingRepositoryImpl(this.remoteDataSource, this.authRepository);

  @override
  Future<List<RankedCompany>> getRankedCompanies() async {
    final token = await authRepository.getPersistedToken();
    return await remoteDataSource.getRankedCompanies(token: token);
  }
}
