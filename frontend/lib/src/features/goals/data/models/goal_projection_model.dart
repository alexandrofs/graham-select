import '../../domain/entities/goal_projection.dart';

class GoalProjectionModel {
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

  const GoalProjectionModel({
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

  factory GoalProjectionModel.fromJson(Map<String, dynamic> json) {
    return GoalProjectionModel(
      targetValue: (json['targetValue'] as num?)?.toDouble() ?? 0.0,
      currentEquity: (json['currentEquity'] as num?)?.toDouble() ?? 0.0,
      monthlyContribution: (json['monthlyContribution'] as num?)?.toDouble() ?? 0.0,
      annualYieldPercentage: (json['annualYieldPercentage'] as num?)?.toDouble() ?? 0.0,
      totalMonths: (json['totalMonths'] as num?)?.toInt() ?? 0,
      years: (json['years'] as num?)?.toInt() ?? 0,
      months: (json['months'] as num?)?.toInt() ?? 0,
      targetDate: json['targetDate'] is String ? DateTime.tryParse(json['targetDate'] as String) : null,
      progressPercentage: (json['progressPercentage'] as num?)?.toDouble() ?? 0.0,
      isAchieved: json['isAchieved'] as bool? ?? false,
      isUnreachable: json['isUnreachable'] as bool? ?? false,
    );
  }

  GoalProjection toEntity() {
    return GoalProjection(
      targetValue: targetValue,
      currentEquity: currentEquity,
      monthlyContribution: monthlyContribution,
      annualYieldPercentage: annualYieldPercentage,
      totalMonths: totalMonths,
      years: years,
      months: months,
      targetDate: targetDate,
      progressPercentage: progressPercentage,
      isAchieved: isAchieved,
      isUnreachable: isUnreachable,
    );
  }
}
