enum GoalType { patrimonyTarget, monthlyIncomeTarget }

class FinancialGoal {
  final String? id;
  final String goalType; // 'PATRIMONY_TARGET' | 'MONTHLY_INCOME_TARGET'
  final double targetValue;
  final double monthlyContribution;
  final int estimatedYears;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const FinancialGoal({
    this.id,
    required this.goalType,
    required this.targetValue,
    required this.monthlyContribution,
    required this.estimatedYears,
    this.createdAt,
    this.updatedAt,
  });

  FinancialGoal copyWith({
    String? id,
    String? goalType,
    double? targetValue,
    double? monthlyContribution,
    int? estimatedYears,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return FinancialGoal(
      id: id ?? this.id,
      goalType: goalType ?? this.goalType,
      targetValue: targetValue ?? this.targetValue,
      monthlyContribution: monthlyContribution ?? this.monthlyContribution,
      estimatedYears: estimatedYears ?? this.estimatedYears,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
