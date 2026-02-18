import '../../domain/entities/ranked_company.dart';

class RankedCompanyModel extends RankedCompany {
  const RankedCompanyModel({
    required super.symbol,
    required super.name,
    required super.intrinsicValue,
    required super.currentPrice,
    required super.marginOfSafety,
  });

  factory RankedCompanyModel.fromJson(Map<String, dynamic> json) {
    return RankedCompanyModel(
      symbol: json['symbol'] ?? '',
      name: json['name'] ?? '',
      intrinsicValue: (json['intrinsicValue'] as num?)?.toDouble() ?? 0.0,
      currentPrice: (json['currentPrice'] as num?)?.toDouble() ?? 0.0,
      marginOfSafety: (json['marginOfSafety'] as num?)?.toDouble() ?? 0.0,
    );
  }
}
