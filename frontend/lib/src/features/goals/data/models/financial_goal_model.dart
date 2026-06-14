import '../../domain/entities/financial_goal.dart';

class FinancialGoalModel {
  final String? id;
  final String goalType;
  final double targetValue;
  final double monthlyContribution;
  final int estimatedYears;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const FinancialGoalModel({
    this.id,
    required this.goalType,
    required this.targetValue,
    required this.monthlyContribution,
    required this.estimatedYears,
    this.createdAt,
    this.updatedAt,
  });

  factory FinancialGoalModel.fromJson(Map<String, dynamic> json) {
    return FinancialGoalModel(
      id: json['id'] as String?,
      goalType: json['goalType'] as String? ?? 'PATRIMONY_TARGET',
      targetValue: (json['targetValue'] as num?)?.toDouble() ?? 0.0,
      monthlyContribution: (json['monthlyContribution'] as num?)?.toDouble() ?? 0.0,
      estimatedYears: json['estimatedYears'] as int? ?? 0,
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt'] as String) : null,
      updatedAt: json['updatedAt'] != null ? DateTime.parse(json['updatedAt'] as String) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      'goalType': goalType,
      'targetValue': targetValue,
      'monthlyContribution': monthlyContribution,
      'estimatedYears': estimatedYears,
      if (createdAt != null) 'createdAt': createdAt!.toIso8601String(),
      if (updatedAt != null) 'updatedAt': updatedAt!.toIso8601String(),
    };
  }

  FinancialGoal toEntity() {
    return FinancialGoal(
      id: id,
      goalType: goalType,
      targetValue: targetValue,
      monthlyContribution: monthlyContribution,
      estimatedYears: estimatedYears,
      createdAt: createdAt,
      updatedAt: updatedAt,
    );
  }

  factory FinancialGoalModel.fromEntity(FinancialGoal entity) {
    return FinancialGoalModel(
      id: entity.id,
      goalType: entity.goalType,
      targetValue: entity.targetValue,
      monthlyContribution: entity.monthlyContribution,
      estimatedYears: entity.estimatedYears,
      createdAt: entity.createdAt,
      updatedAt: entity.updatedAt,
    );
  }
}
