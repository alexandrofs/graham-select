import '../../domain/entities/graham_recommendation.dart';
import '../../domain/repositories/graham_recommendation_repository.dart';
import '../datasources/graham_recommendation_remote_data_source.dart';
import '../../../auth/data/auth_repository.dart';

class GrahamRecommendationRepositoryImpl implements GrahamRecommendationRepository {
  final GrahamRecommendationRemoteDataSource remoteDataSource;
  final AuthRepository authRepository;

  GrahamRecommendationRepositoryImpl({
    required this.remoteDataSource,
    required this.authRepository,
  });

  @override
  Future<List<GrahamRecommendation>> getRecommendations() async {
    final token = await authRepository.getPersistedToken();
    return await remoteDataSource.getRecommendations(token: token);
  }

  @override
  Future<void> triggerCalculation() async {
    final token = await authRepository.getPersistedToken();
    await remoteDataSource.triggerCalculation(token: token);
  }
}
