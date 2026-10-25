import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../domain/entities/goal_simulation_result.dart';

class GoalProjectionChart extends StatelessWidget {
  final List<ProjectionCurvePoint> baselineCurve;
  final List<ProjectionCurvePoint> simulatedCurve;
  final double targetValue;

  static const Color kNavyBlue = Color(0xFF1B2A4A);
  static const Color kEmerald = Color(0xFF10B981);
  static const Color kAmberGold = Color(0xFFF59E0B);

  const GoalProjectionChart({
    super.key,
    required this.baselineCurve,
    required this.simulatedCurve,
    required this.targetValue,
  });

  @override
  Widget build(BuildContext context) {
    final int maxMonths = _resolveMaxMonths();
    final String horizonLabel = _formatHorizon(maxMonths);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 16,
          runSpacing: 8,
          children: [
            _buildLegendItem(
              context,
              label: 'Trajetória Atual',
              color: kNavyBlue.withAlpha((0.55 * 255).round()),
              isDashed: false,
            ),
            _buildLegendItem(
              context,
              label: 'Cenário Simulado',
              color: kEmerald,
              isDashed: false,
            ),
            _buildLegendItem(
              context,
              label: 'Meta Alvo',
              color: kAmberGold,
              isDashed: true,
            ),
          ],
        ),
        const SizedBox(height: 12),
        SizedBox(
          height: 180,
          width: double.infinity,
          child: CustomPaint(
            painter: _GoalProjectionChartPainter(
              baselineCurve: baselineCurve,
              simulatedCurve: simulatedCurve,
              targetValue: targetValue,
            ),
          ),
        ),
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Hoje',
              style: GoogleFonts.jetBrainsMono(
                textStyle: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: kNavyBlue.withAlpha((0.7 * 255).round()),
                      fontWeight: FontWeight.w600,
                    ),
              ),
            ),
            Text(
              horizonLabel,
              style: GoogleFonts.jetBrainsMono(
                textStyle: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: kNavyBlue.withAlpha((0.7 * 255).round()),
                      fontWeight: FontWeight.w600,
                    ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  int _resolveMaxMonths() {
    int maxM = 0;
    if (baselineCurve.isNotEmpty) {
      maxM = math.max(maxM, baselineCurve.last.monthIndex);
    }
    if (simulatedCurve.isNotEmpty) {
      maxM = math.max(maxM, simulatedCurve.last.monthIndex);
    }
    return maxM > 0 ? maxM : 120;
  }

  String _formatHorizon(int totalMonths) {
    final int years = totalMonths ~/ 12;
    final int months = totalMonths % 12;
    if (years > 0 && months > 0) {
      return 'Horizonte: ${years}a ${months}m';
    } else if (years > 0) {
      return 'Horizonte: $years anos';
    }
    return 'Horizonte: $months meses';
  }

  Widget _buildLegendItem(
    BuildContext context, {
    required String label,
    required Color color,
    required bool isDashed,
  }) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 16,
          height: 4,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(2),
          ),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: kNavyBlue.withAlpha((0.85 * 255).round()),
                fontWeight: FontWeight.w600,
              ),
        ),
      ],
    );
  }
}

class _GoalProjectionChartPainter extends CustomPainter {
  final List<ProjectionCurvePoint> baselineCurve;
  final List<ProjectionCurvePoint> simulatedCurve;
  final double targetValue;

  static const Color kNavyBlue = Color(0xFF1B2A4A);
  static const Color kEmerald = Color(0xFF10B981);
  static const Color kAmberGold = Color(0xFFF59E0B);

  _GoalProjectionChartPainter({
    required this.baselineCurve,
    required this.simulatedCurve,
    required this.targetValue,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (size.width <= 0 || size.height <= 0) return;

    final double safeTarget = (targetValue.isFinite && targetValue > 0) ? targetValue : 0.0;
    double maxEquity = safeTarget > 0 ? safeTarget * 1.1 : 100.0;
    int maxMonth = 1;

    for (final p in baselineCurve) {
      if (p.equity.isFinite && p.equity > maxEquity) maxEquity = p.equity;
      if (p.monthIndex > maxMonth) maxMonth = p.monthIndex;
    }
    for (final p in simulatedCurve) {
      if (p.equity.isFinite && p.equity > maxEquity) maxEquity = p.equity;
      if (p.monthIndex > maxMonth) maxMonth = p.monthIndex;
    }

    if (!maxEquity.isFinite || maxEquity <= 0) maxEquity = 100.0;

    // 1. Subtle horizontal grid lines
    final Paint gridPaint = Paint()
      ..color = kNavyBlue.withAlpha((0.08 * 255).round())
      ..strokeWidth = 1.0
      ..style = PaintingStyle.stroke;

    const int gridCount = 4;
    for (int i = 0; i <= gridCount; i++) {
      final double y = size.height * (i / gridCount);
      canvas.drawLine(Offset(0, y), Offset(size.width, y), gridPaint);
    }

    // 2. Target Value (FV) dashed horizontal line in kAmberGold
    if (safeTarget > 0 && safeTarget <= maxEquity) {
      final double targetY =
          size.height - ((safeTarget / maxEquity) * size.height).clamp(0.0, size.height);
      final Paint targetPaint = Paint()
        ..color = kAmberGold
        ..strokeWidth = 1.8
        ..style = PaintingStyle.stroke;

      const double dashWidth = 6.0;
      const double dashSpace = 4.0;
      double startX = 0.0;
      while (startX < size.width) {
        final double endX = math.min(startX + dashWidth, size.width);
        canvas.drawLine(Offset(startX, targetY), Offset(endX, targetY), targetPaint);
        startX += dashWidth + dashSpace;
      }
    }

    // 3. Baseline curve in kNavyBlue with alpha 0.55
    if (baselineCurve.length >= 2) {
      final Path baselinePath = _buildCurvePath(baselineCurve, size, maxMonth, maxEquity);
      final Paint baselinePaint = Paint()
        ..color = kNavyBlue.withAlpha((0.55 * 255).round())
        ..strokeWidth = 2.2
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..style = PaintingStyle.stroke;
      canvas.drawPath(baselinePath, baselinePaint);
    }

    // 4. Simulated curve in kEmerald (strokeWidth 3.0) with gradient fill below
    if (simulatedCurve.length >= 2) {
      final Path simulatedPath = _buildCurvePath(simulatedCurve, size, maxMonth, maxEquity);

      final Path fillPath = Path.from(simulatedPath)
        ..lineTo(size.width, size.height)
        ..lineTo(0, size.height)
        ..close();

      final Paint fillPaint = Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            kEmerald.withAlpha((0.28 * 255).round()),
            kEmerald.withAlpha((0.02 * 255).round()),
          ],
        ).createShader(Rect.fromLTWH(0, 0, size.width, size.height))
        ..style = PaintingStyle.fill;

      canvas.drawPath(fillPath, fillPaint);

      final Paint simulatedPaint = Paint()
        ..color = kEmerald
        ..strokeWidth = 3.0
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..style = PaintingStyle.stroke;

      canvas.drawPath(simulatedPath, simulatedPaint);
    }
  }

  Path _buildCurvePath(
    List<ProjectionCurvePoint> points,
    Size size,
    int maxMonth,
    double maxEquity,
  ) {
    final Path path = Path();
    for (int i = 0; i < points.length; i++) {
      final p = points[i];
      final double x = ((p.monthIndex / maxMonth) * size.width).clamp(0.0, size.width);
      final double y =
          size.height - ((p.equity / maxEquity) * size.height).clamp(0.0, size.height);
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    return path;
  }

  @override
  bool shouldRepaint(covariant _GoalProjectionChartPainter oldDelegate) {
    return oldDelegate.baselineCurve != baselineCurve ||
        oldDelegate.simulatedCurve != simulatedCurve ||
        oldDelegate.targetValue != targetValue;
  }
}
