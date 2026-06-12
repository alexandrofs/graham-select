class AllocationGoal {
  final String goalType; // 'ASSET_CLASS' | 'TICKER'
  final String targetKey;
  final double targetPercentage;

  const AllocationGoal({
    required this.goalType,
    required this.targetKey,
    required this.targetPercentage,
  });
}
