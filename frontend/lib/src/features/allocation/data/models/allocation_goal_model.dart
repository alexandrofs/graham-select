import '../../domain/entities/allocation_goal.dart';

class AllocationGoalModel extends AllocationGoal {
  const AllocationGoalModel({
    required super.goalType,
    required super.targetKey,
    required super.targetPercentage,
  });

  factory AllocationGoalModel.fromJson(Map<String, dynamic> json) {
    return AllocationGoalModel(
      goalType: json['goalType'] as String,
      targetKey: json['targetKey'] as String,
      targetPercentage: (json['targetPercentage'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'goalType': goalType,
      'targetKey': targetKey,
      'targetPercentage': targetPercentage,
    };
  }
}
