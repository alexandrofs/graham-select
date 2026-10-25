import 'goal_projection.dart';

class ProjectionCurvePoint {
  final int monthIndex;
  final double equity;

  const ProjectionCurvePoint({
    required this.monthIndex,
    required this.equity,
  });
}

class GoalSimulationResult {
  final GoalProjection baselineProjection;
  final GoalProjection simulatedProjection;
  final int monthsDifference;
  final List<ProjectionCurvePoint> baselineCurve;
  final List<ProjectionCurvePoint> simulatedCurve;

  const GoalSimulationResult({
    required this.baselineProjection,
    required this.simulatedProjection,
    required this.monthsDifference,
    required this.baselineCurve,
    required this.simulatedCurve,
  });
}
