import '../entities/graham_recommendation.dart';

abstract class GrahamRecommendationRepository {
  Future<List<GrahamRecommendation>> getRecommendations({String? tagId});
  Future<void> triggerCalculation({String? tagId});
}
