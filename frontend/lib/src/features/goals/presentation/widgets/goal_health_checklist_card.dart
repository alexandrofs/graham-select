import 'package:flutter/material.dart';
import '../../domain/entities/financial_goal.dart';
import '../../domain/entities/goal_health_assessment.dart';
import '../../domain/services/time_to_goal_calculator.dart';

class GoalHealthChecklistCard extends StatelessWidget {
  final FinancialGoal goal;
  final double currentEquity;
  final double annualYieldPercentage;
  final GoalHealthAssessment? assessment;

  const GoalHealthChecklistCard({
    super.key,
    required this.goal,
    required this.currentEquity,
    required this.annualYieldPercentage,
    this.assessment,
  });

  static const Color kWarningRed = Color(0xFFEF4444);
  static const Color kEmerald = Color(0xFF10B981);
  static const Color kNavyBlue = Color(0xFF1B2A4A);

  @override
  Widget build(BuildContext context) {
    final GoalHealthAssessment effectiveAssessment = assessment ??
        TimeToGoalCalculator.evaluateHealth(
          goalType: goal.goalType,
          targetValue: goal.targetValue,
          estimatedYears: goal.estimatedYears,
          currentEquity: currentEquity,
          monthlyContribution: goal.monthlyContribution,
          annualYieldPercentage: annualYieldPercentage,
        );

    final bool isOffTrack = effectiveAssessment.status == GoalHealthStatus.offTrack;
    final bool isAchieved = effectiveAssessment.status == GoalHealthStatus.achieved;
    final Color statusColor = isOffTrack ? kWarningRed : kEmerald;
    final IconData badgeIcon = isOffTrack
        ? Icons.warning_amber_rounded
        : (isAchieved ? Icons.verified_rounded : Icons.check_circle_outline);
    final String badgeText = isOffTrack
        ? 'Atenção: Fora do Trilho'
        : (isAchieved ? 'Meta Concluída' : 'No Trilho');

    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: isOffTrack ? kWarningRed.withAlpha(90) : kEmerald.withAlpha(60),
          width: 1.5,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 12,
              runSpacing: 12,
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.fact_check_outlined,
                      color: isOffTrack ? kWarningRed : kNavyBlue,
                      size: 24,
                    ),
                    const SizedBox(width: 10),
                    Text(
                      'Checklist de Saúde da Meta',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: kNavyBlue,
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                  ],
                ),
                Container(
                  key: const Key('goal_health_status_badge'),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: statusColor.withAlpha(30),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: statusColor),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        badgeIcon,
                        color: statusColor,
                        size: 16,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        badgeText,
                        style: TextStyle(
                          color: statusColor,
                          fontWeight: FontWeight.w700,
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            ...effectiveAssessment.checklistItems.map(
              (item) => Padding(
                padding: const EdgeInsets.only(bottom: 12.0),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      item.isHealthy ? Icons.check_circle_rounded : Icons.error_outline_rounded,
                      color: item.isHealthy ? kEmerald : kWarningRed,
                      size: 20,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.title,
                            style: const TextStyle(
                              color: kNavyBlue,
                              fontWeight: FontWeight.w700,
                              fontSize: 14,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            item.description,
                            style: TextStyle(
                              color: kNavyBlue.withAlpha(200),
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 4),
            if (isOffTrack)
              Container(
                key: const Key('goal_health_warning_banner'),
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: kWarningRed.withAlpha(25),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: kWarningRed),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(
                          Icons.warning_amber_rounded,
                          color: kWarningRed,
                          size: 18,
                        ),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Ações recomendadas para voltar ao trilho:',
                            style: TextStyle(
                              color: kWarningRed,
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    ...effectiveAssessment.actionSuggestions.map(
                      (suggestion) => Padding(
                        padding: const EdgeInsets.only(bottom: 4.0),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              '• ',
                              style: TextStyle(
                                color: kWarningRed,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            Expanded(
                              child: Text(
                                suggestion,
                                style: const TextStyle(
                                  color: kWarningRed,
                                  fontWeight: FontWeight.w600,
                                  fontSize: 13,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              )
            else
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: kEmerald.withAlpha(25),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: kEmerald.withAlpha(100)),
                ),
                child: Row(
                  children: [
                    const Icon(
                      Icons.check_circle_outline,
                      color: kEmerald,
                      size: 18,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        effectiveAssessment.actionSuggestions.isNotEmpty
                            ? effectiveAssessment.actionSuggestions.first
                            : 'Mantenha a consistência dos seus aportes mensais para seguir no trilho.',
                        style: const TextStyle(
                          color: kEmerald,
                          fontWeight: FontWeight.w600,
                          fontSize: 13,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
