import '../../domain/entities/portfolio_summary.dart';

class PortfolioSummaryModel extends PortfolioSummary {
  PortfolioSummaryModel({
    required super.totalEquity,
    required super.grossYieldPercentage,
    required super.accumulatedDividends,
    required super.monthlyProjection,
  });

  factory PortfolioSummaryModel.fromJson(Map<String, dynamic> json) {
    return PortfolioSummaryModel(
      totalEquity: (json['totalEquity'] as num).toDouble(),
      grossYieldPercentage: (json['grossYieldPercentage'] as num).toDouble(),
      accumulatedDividends: (json['accumulatedDividends'] as num).toDouble(),
      monthlyProjection: (json['monthlyProjection'] as num).toDouble(),
    );
  }
}
