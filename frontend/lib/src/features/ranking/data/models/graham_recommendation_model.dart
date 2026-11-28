import '../../domain/entities/graham_recommendation.dart';

double _parseDouble(dynamic value, [double defaultValue = 0.0]) {
  if (value == null) return defaultValue;
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? defaultValue;
  return defaultValue;
}

double? _parseNullableDouble(dynamic value) {
  if (value == null) return null;
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value);
  return null;
}

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
      ticker: json['ticker']?.toString() ?? '',
      currentPrice: _parseDouble(json['currentPrice']),
      intrinsicValue: _parseDouble(json['intrinsicValue']),
      marginOfSafety: _parseDouble(json['marginOfSafety']),
      currentAllocationPct: _parseDouble(json['currentAllocationPct']),
      targetAllocationPct: _parseDouble(json['targetAllocationPct']),
      allocationGap: _parseDouble(json['allocationGap']),
      recommendationScore: _parseDouble(json['recommendationScore']),
      epsUsed: _parseNullableDouble(json['epsUsed']),
      bvpsUsed: _parseNullableDouble(json['bvpsUsed']),
    );
  }
}
