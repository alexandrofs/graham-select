import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../../domain/entities/financial_goal.dart';
import '../../domain/entities/goal_projection.dart';
import '../../domain/services/time_to_goal_calculator.dart';
import '../pages/goals_page.dart';

class MotivationalClockCard extends StatefulWidget {
  static bool isTesting = false;

  final FinancialGoal? goal;
  final GoalProjection? projection;
  final VoidCallback? onConfigureGoal;

  const MotivationalClockCard({
    super.key,
    required this.goal,
    required this.projection,
    this.onConfigureGoal,
  });

  @override
  State<MotivationalClockCard> createState() => _MotivationalClockCardState();
}

class _MotivationalClockCardState extends State<MotivationalClockCard>
    with SingleTickerProviderStateMixin {
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  static const Color kNavyBlue = Color(0xFF1B2A4A);
  static const Color kEmerald = Color(0xFF10B981);
  static const Color kAmberGold = Color(0xFFF59E0B);
  static const Color kCardDarkBg = Color(0xFF111827);

  bool get _isTestEnvironment =>
      MotivationalClockCard.isTesting ||
      GoalsPage.isTesting ||
      WidgetsBinding.instance.runtimeType.toString().contains('Test');

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1000),
    );
    _pulseAnimation = Tween<double>(begin: 0.4, end: 1.0).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
    _syncPulseAnimation();
  }

  @override
  void didUpdateWidget(covariant MotivationalClockCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    _syncPulseAnimation();
  }

  void _syncPulseAnimation() {
    if (_isTestEnvironment) {
      if (_pulseController.isAnimating) {
        _pulseController.stop();
      }
      return;
    }

    final isAchieved = widget.projection?.isAchieved ?? false;
    final hasGoal = widget.goal != null && widget.projection != null;

    if (hasGoal && !isAchieved) {
      if (!_pulseController.isAnimating) {
        _pulseController.repeat(reverse: true);
      }
    } else {
      if (_pulseController.isAnimating) {
        _pulseController.stop();
      }
      _pulseController.value = 1.0;
    }
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.goal == null) {
      return _buildEmptyState(context);
    }

    final GoalProjection projection = widget.projection ??
        TimeToGoalCalculator.calculate(
          goalType: widget.goal!.goalType,
          targetValue: widget.goal!.targetValue,
          currentEquity: 0.0,
          monthlyContribution: widget.goal!.monthlyContribution,
          annualYieldPercentage: 0.0,
        );
    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final percentFormat = NumberFormat.decimalPattern('pt_BR');

    final String pvFormatted = currencyFormat.format(projection.currentEquity);
    final String fvFormatted = currencyFormat.format(projection.targetValue);
    final String pmtFormatted = currencyFormat.format(projection.monthlyContribution);
    final String yieldFormatted = percentFormat.format(projection.annualYieldPercentage);
    final String progressFormatted = projection.progressPercentage.toStringAsFixed(1).replaceAll('.', ',');

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: kNavyBlue.withAlpha((0.12 * 255).round()),
          width: 1,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildHeader(context, projection),
            const SizedBox(height: 20),
            _buildClockDisplay(context, projection),
            const SizedBox(height: 20),
            _buildProgressSection(
              context,
              projection: projection,
              progressFormatted: progressFormatted,
              pvFormatted: pvFormatted,
              fvFormatted: fvFormatted,
            ),
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 12),
            _buildFooterContext(
              context,
              projection: projection,
              pmtFormatted: pmtFormatted,
              yieldFormatted: yieldFormatted,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context) {
    return Card(
      elevation: 3,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: kAmberGold.withAlpha((0.35 * 255).round()),
          width: 1.5,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: kAmberGold.withAlpha((0.12 * 255).round()),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.timer_outlined,
                color: kAmberGold,
                size: 32,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Tempo Restante para Independência',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    color: kNavyBlue,
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              'Defina sua Meta Financeira para desbloquear o Relógio Time-to-Goal',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: kNavyBlue.withAlpha((0.75 * 255).round()),
                  ),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: () {
                if (widget.onConfigureGoal != null) {
                  widget.onConfigureGoal!();
                } else {
                  context.go('/metas');
                }
              },
              icon: const Icon(Icons.flag_outlined),
              label: const Text('Configurar Minha Meta'),
              style: FilledButton.styleFrom(
                backgroundColor: kEmerald,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context, GoalProjection projection) {
    final bool isAchieved = projection.isAchieved;
    final bool isUnreachable = projection.isUnreachable;

    String badgeLabel;
    Color badgeColor;
    if (isAchieved) {
      badgeLabel = 'Meta Atingida 🎉';
      badgeColor = kEmerald;
    } else if (isUnreachable) {
      badgeLabel = 'Ação Necessária';
      badgeColor = kAmberGold;
    } else {
      badgeLabel = 'Em Progresso';
      badgeColor = kAmberGold;
    }

    return Row(
      children: [
        AnimatedBuilder(
          animation: _pulseAnimation,
          builder: (context, child) {
            final glowAlpha = isAchieved ? 0.25 : (0.15 * _pulseAnimation.value);
            final iconColor = isAchieved ? kEmerald : kAmberGold;
            return Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: iconColor.withAlpha((glowAlpha * 255).round()),
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: iconColor.withAlpha(((glowAlpha * 0.8) * 255).round()),
                    blurRadius: 10 * _pulseAnimation.value,
                    spreadRadius: 2 * _pulseAnimation.value,
                  ),
                ],
              ),
              child: Icon(
                Icons.timer_outlined,
                color: iconColor,
                size: 28,
              ),
            );
          },
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            'Tempo Restante para Independência',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  color: kNavyBlue,
                  fontWeight: FontWeight.bold,
                ),
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration: BoxDecoration(
            color: badgeColor.withAlpha((0.15 * 255).round()),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: badgeColor.withAlpha((0.4 * 255).round()),
            ),
          ),
          child: Text(
            badgeLabel,
            style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  color: badgeColor,
                  fontWeight: FontWeight.bold,
                ),
          ),
        ),
      ],
    );
  }

  Widget _buildClockDisplay(BuildContext context, GoalProjection projection) {
    final bool isAchieved = projection.isAchieved;
    final bool isUnreachable = projection.isUnreachable;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 16),
      decoration: BoxDecoration(
        color: kCardDarkBg,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: (isAchieved ? kEmerald : kAmberGold).withAlpha((0.45 * 255).round()),
          width: 1.5,
        ),
      ),
      child: Column(
        children: [
          if (isUnreachable) ...[
            const Icon(
              Icons.hourglass_bottom_rounded,
              color: kAmberGold,
              size: 32,
            ),
            const SizedBox(height: 8),
            Text(
              'Defina um aporte mensal ou adicione ativos com rendimento para calcular a projeção.',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                    color: kAmberGold,
                    fontWeight: FontWeight.w600,
                  ),
            ),
          ] else ...[
            Text(
              '${projection.years} anos e ${projection.months} meses',
              textAlign: TextAlign.center,
              style: GoogleFonts.jetBrainsMono(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: isAchieved ? kEmerald : kAmberGold,
              ),
            ),
            if (isAchieved) ...[
              const SizedBox(height: 8),
              Text(
                '🎉 Parabéns! Você atingiu sua independência financeira!',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      color: kEmerald,
                      fontWeight: FontWeight.bold,
                    ),
              ),
            ],
          ],
        ],
      ),
    );
  }

  Widget _buildProgressSection(
    BuildContext context, {
    required GoalProjection projection,
    required String progressFormatted,
    required String pvFormatted,
    required String fvFormatted,
  }) {
    final double clampedFraction = (projection.progressPercentage / 100.0).clamp(0.0, 1.0);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              '$progressFormatted% alcançado',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: kNavyBlue,
                    fontWeight: FontWeight.bold,
                  ),
            ),
            Flexible(
              child: Text(
                '$pvFormatted de $fvFormatted',
                textAlign: TextAlign.end,
                style: GoogleFonts.jetBrainsMono(
                  textStyle: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: kNavyBlue.withAlpha((0.8 * 255).round()),
                        fontWeight: FontWeight.w600,
                      ),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Container(
            height: 10,
            width: double.infinity,
            color: kNavyBlue.withAlpha((0.1 * 255).round()),
            child: FractionallySizedBox(
              alignment: Alignment.centerLeft,
              widthFactor: clampedFraction,
              child: Container(
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    colors: [kEmerald, kAmberGold],
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFooterContext(
    BuildContext context, {
    required GoalProjection projection,
    required String pmtFormatted,
    required String yieldFormatted,
  }) {
    String forecastLabel;
    if (projection.isAchieved) {
      forecastLabel = 'Previsão: Meta Concluída';
    } else if (projection.isUnreachable || projection.targetDate == null) {
      forecastLabel = 'Previsão: Indeterminada';
    } else {
      final rawDate = DateFormat("MMMM 'de' yyyy", 'pt_BR').format(projection.targetDate!);
      final capitalizedDate = rawDate.isNotEmpty
          ? '${rawDate[0].toUpperCase()}${rawDate.substring(1)}'
          : rawDate;
      forecastLabel = 'Previsão: $capitalizedDate';
    }

    return Wrap(
      spacing: 12,
      runSpacing: 8,
      alignment: WrapAlignment.spaceBetween,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.calendar_month_outlined,
              size: 18,
              color: kNavyBlue.withAlpha((0.7 * 255).round()),
            ),
            const SizedBox(width: 6),
            Text(
              forecastLabel,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: kNavyBlue,
                    fontWeight: FontWeight.w600,
                  ),
            ),
          ],
        ),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            _buildContextChip(context, 'Aporte: $pmtFormatted/mês'),
            _buildContextChip(context, 'Yield: $yieldFormatted% a.a.'),
          ],
        ),
      ],
    );
  }

  Widget _buildContextChip(BuildContext context, String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: kNavyBlue.withAlpha((0.06 * 255).round()),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        style: GoogleFonts.jetBrainsMono(
          textStyle: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: kNavyBlue,
                fontWeight: FontWeight.w600,
              ),
        ),
      ),
    );
  }
}
