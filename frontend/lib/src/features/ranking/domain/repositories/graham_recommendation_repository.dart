import '../entities/graham_recommendation.dart';

abstract class GrahamRecommendationRepository {
  Future<List<GrahamRecommendation>> getRecommendations();
  Future<void> triggerCalculation();
}
