import '../../domain/entities/graham_recommendation.dart';

class GrahamRecommendationModel extends GrahamRecommendation {
  const GrahamRecommendationModel({
    required super.ticker,
    required super.currentPrice,
    required super.intrinsicValue,
    required super.marginOfSafety,
    required super.currentAllocationPct,
    required super.targetAllocationPct,
    required super.allocationGap,
    required super.recommendationScore,
    super.epsUsed,
    super.bvpsUsed,
  });

  factory GrahamRecommendationModel.fromJson(Map<String, dynamic> json) {
    return GrahamRecommendationModel(
      ticker: json['ticker'] ?? '',
      currentPrice: (json['currentPrice'] as num?)?.toDouble() ?? 0.0,
      intrinsicValue: (json['intrinsicValue'] as num?)?.toDouble() ?? 0.0,
      marginOfSafety: (json['marginOfSafety'] as num?)?.toDouble() ?? 0.0,
      currentAllocationPct: (json['currentAllocationPct'] as num?)?.toDouble() ?? 0.0,
      targetAllocationPct: (json['targetAllocationPct'] as num?)?.toDouble() ?? 0.0,
      allocationGap: (json['allocationGap'] as num?)?.toDouble() ?? 0.0,
      recommendationScore: (json['recommendationScore'] as num?)?.toDouble() ?? 0.0,
      epsUsed: (json['epsUsed'] as num?)?.toDouble(),
      bvpsUsed: (json['bvpsUsed'] as num?)?.toDouble(),
    );
  }
}
