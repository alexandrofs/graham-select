import '../../domain/entities/monthly_evolution.dart';

class MonthlyEvolutionModel extends MonthlyEvolution {
  const MonthlyEvolutionModel({
    required super.month,
    required super.totalContributions,
    required super.totalDividends,
  });

  factory MonthlyEvolutionModel.fromJson(Map<String, dynamic> json) {
    return MonthlyEvolutionModel(
      month: json['month'] as String,
      totalContributions: (json['totalContributions'] as num).toDouble(),
      totalDividends: (json['totalDividends'] as num).toDouble(),
    );
  }
}

class PortfolioEvolutionModel extends PortfolioEvolution {
  const PortfolioEvolutionModel({
    required super.monthlyData,
  });

  factory PortfolioEvolutionModel.fromJson(Map<String, dynamic> json) {
    final list = (json['monthlyData'] as List<dynamic>?) ?? [];
    final parsed = list
        .map((item) => MonthlyEvolutionModel.fromJson(item as Map<String, dynamic>))
        .toList();

    return PortfolioEvolutionModel(monthlyData: parsed);
  }
}
