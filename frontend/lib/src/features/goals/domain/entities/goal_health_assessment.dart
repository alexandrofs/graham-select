enum GoalHealthStatus { onTrack, offTrack, achieved }

class GoalHealthChecklistItem {
  final String title;
  final String description;
  final bool isHealthy;

  const GoalHealthChecklistItem({
    required this.title,
    required this.description,
    required this.isHealthy,
  });
}

class GoalHealthAssessment {
  final GoalHealthStatus status;
  final bool isOnTrack;
  final int estimatedTargetMonths;
  final int projectedTotalMonths;
  final int monthsDelay;
  final double additionalMonthlyContributionNeeded;
  final double requiredMonthlyContribution;
  final double requiredAnnualYieldPercentage;
  final List<GoalHealthChecklistItem> checklistItems;
  final List<String> actionSuggestions;

  const GoalHealthAssessment({
    required this.status,
    required this.isOnTrack,
    required this.estimatedTargetMonths,
    required this.projectedTotalMonths,
    required this.monthsDelay,
    required this.additionalMonthlyContributionNeeded,
    required this.requiredMonthlyContribution,
    required this.requiredAnnualYieldPercentage,
    required this.checklistItems,
    required this.actionSuggestions,
  });
}
