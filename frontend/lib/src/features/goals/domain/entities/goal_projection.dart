class GoalProjection {
  final double targetValue;
  final double currentEquity;
  final double monthlyContribution;
  final double annualYieldPercentage;
  final int totalMonths;
  final int years;
  final int months;
  final DateTime? targetDate;
  final double progressPercentage;
  final bool isAchieved;
  final bool isUnreachable;

  const GoalProjection({
    required this.targetValue,
    required this.currentEquity,
    required this.monthlyContribution,
    required this.annualYieldPercentage,
    required this.totalMonths,
    required this.years,
    required this.months,
    this.targetDate,
    required this.progressPercentage,
    required this.isAchieved,
    required this.isUnreachable,
  });
}
