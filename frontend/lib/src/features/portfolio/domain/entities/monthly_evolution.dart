class MonthlyEvolution {
  final String month;
  final double totalContributions;
  final double totalDividends;

  const MonthlyEvolution({
    required this.month,
    required this.totalContributions,
    required this.totalDividends,
  });
}

class PortfolioEvolution {
  final List<MonthlyEvolution> monthlyData;

  const PortfolioEvolution({
    required this.monthlyData,
  });
}
