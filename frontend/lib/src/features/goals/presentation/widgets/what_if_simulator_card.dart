import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../../domain/entities/financial_goal.dart';
import '../../domain/entities/goal_projection.dart';
import '../../domain/entities/goal_simulation_result.dart';
import '../../domain/services/time_to_goal_calculator.dart';
import 'goal_projection_chart.dart';

class WhatIfSimulatorCard extends StatefulWidget {
  final FinancialGoal goal;
  final double currentEquity;
  final double baselineYieldPercentage;

  const WhatIfSimulatorCard({
    super.key,
    required this.goal,
    required this.currentEquity,
    required this.baselineYieldPercentage,
  });

  @override
  State<WhatIfSimulatorCard> createState() => _WhatIfSimulatorCardState();
}

class _WhatIfSimulatorCardState extends State<WhatIfSimulatorCard> {
  static const Color kNavyBlue = Color(0xFF1B2A4A);
  static const Color kEmerald = Color(0xFF10B981);
  static const Color kAmberGold = Color(0xFFF59E0B);

  late double _simulatedContribution;
  late double _simulatedYield;

  @override
  void initState() {
    super.initState();
    _resetValues();
  }

  @override
  void didUpdateWidget(covariant WhatIfSimulatorCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.goal != widget.goal ||
        oldWidget.baselineYieldPercentage != widget.baselineYieldPercentage) {
      _resetValues();
    }
  }

  void _resetValues() {
    _simulatedContribution = widget.goal.monthlyContribution.isFinite
        ? math.max(0.0, widget.goal.monthlyContribution)
        : 0.0;
    _simulatedYield = widget.baselineYieldPercentage.isFinite
        ? math.max(0.0, widget.baselineYieldPercentage)
        : 0.0;
  }

  @override
  Widget build(BuildContext context) {
    final double safeBaseContribution = widget.goal.monthlyContribution.isFinite
        ? math.max(0.0, widget.goal.monthlyContribution)
        : 0.0;
    final double safeBaseYield = widget.baselineYieldPercentage.isFinite
        ? math.max(0.0, widget.baselineYieldPercentage)
        : 0.0;

    final double maxContribution = math.max(safeBaseContribution * 3.0, 10000.0);
    final double maxYield = math.max(30.0, safeBaseYield * 1.5);

    final double clampedContribution = _simulatedContribution.clamp(0.0, maxContribution);
    final double clampedYield = _simulatedYield.clamp(0.0, maxYield);

    final GoalSimulationResult simulationResult = TimeToGoalCalculator.simulate(
      goalType: widget.goal.goalType,
      targetValue: widget.goal.targetValue,
      currentEquity: widget.currentEquity,
      baselineMonthlyContribution: safeBaseContribution,
      baselineAnnualYieldPercentage: safeBaseYield,
      simulatedMonthlyContribution: clampedContribution,
      simulatedAnnualYieldPercentage: clampedYield,
    );

    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final String formattedSimulatedContribution =
        'Simulado: ${currencyFormat.format(clampedContribution)}/mês';
    final String formattedSimulatedYield =
        'Simulado: ${clampedYield.toStringAsFixed(1).replaceAll('.', ',')}% a.a.';

    return Card(
      elevation: 3,
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
            _buildHeader(context),
            const SizedBox(height: 16),
            _buildImpactBanner(context, simulationResult),
            const SizedBox(height: 16),
            _buildComparisonPanel(context, simulationResult),
            const SizedBox(height: 20),
            _buildContributionSlider(
              context,
              value: clampedContribution,
              max: maxContribution,
              formattedLabel: formattedSimulatedContribution,
            ),
            const SizedBox(height: 12),
            _buildYieldSlider(
              context,
              value: clampedYield,
              max: maxYield,
              formattedLabel: formattedSimulatedYield,
            ),
            const SizedBox(height: 20),
            const Divider(),
            const SizedBox(height: 12),
            GoalProjectionChart(
              baselineCurve: simulationResult.baselineCurve,
              simulatedCurve: simulationResult.simulatedCurve,
              targetValue: simulationResult.baselineProjection.targetValue,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Wrap(
      alignment: WrapAlignment.spaceBetween,
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: 12,
      runSpacing: 8,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: kAmberGold.withAlpha((0.15 * 255).round()),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.tune_rounded,
                color: kAmberGold,
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            Flexible(
              child: Text(
                'Simulador de Sensibilidade ("E se?")',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      color: kNavyBlue,
                      fontWeight: FontWeight.bold,
                    ),
              ),
            ),
          ],
        ),
        TextButton.icon(
          onPressed: () {
            setState(() {
              _resetValues();
            });
          },
          icon: const Icon(Icons.refresh_rounded, size: 18),
          label: const Text('Resetar Simulação'),
          style: TextButton.styleFrom(
            foregroundColor: kNavyBlue,
          ),
        ),
      ],
    );
  }

  Widget _buildImpactBanner(BuildContext context, GoalSimulationResult result) {
    final GoalProjection baseline = result.baselineProjection;
    final GoalProjection simulated = result.simulatedProjection;

    Color bannerColor;
    IconData bannerIcon;
    String bannerText;

    // Cenário A: Meta já atingida (PV >= FV)
    if (baseline.isAchieved) {
      bannerColor = kEmerald;
      bannerIcon = Icons.celebration_outlined;
      bannerText =
          '🎉 Sua meta já foi atingida! Use os controles para simular a evolução do seu patrimônio.';
    }
    // Cenário C: Cenário simulado inalcançável
    else if (simulated.isUnreachable) {
      bannerColor = kAmberGold;
      bannerIcon = Icons.warning_amber_rounded;
      bannerText =
          'Neste cenário simulado, a meta não seria alcançada. Aumente o aporte ou a rentabilidade esperada.';
    }
    // Cenário B: Cenário atual inalcançável, mas cenário simulado alcançável
    else if (baseline.isUnreachable && !simulated.isUnreachable) {
      bannerColor = kEmerald;
      bannerIcon = Icons.rocket_launch_outlined;
      bannerText =
          'Com este cenário, sua meta passa a ser alcançável em ${simulated.years} anos e ${simulated.months} meses!';
    }
    // AC 2: Cenário mais favorável (reduz tempo)
    else if (result.monthsDifference > 0) {
      bannerColor = kEmerald;
      bannerIcon = Icons.rocket_launch_outlined;
      bannerText = 'Isso encurtaria sua jornada em ${result.monthsDifference} meses!';
    }
    // AC 3: Cenário menos favorável (aumenta tempo)
    else if (result.monthsDifference < 0) {
      bannerColor = kAmberGold;
      bannerIcon = Icons.schedule_outlined;
      bannerText = 'Isso aumentaria sua jornada em ${result.monthsDifference.abs()} meses.';
    }
    // AC 4: Mesmo número de meses
    else {
      bannerColor = kNavyBlue;
      bannerIcon = Icons.info_outline_rounded;
      bannerText =
          'Ajuste os controles de aporte ou rentabilidade para simular o impacto no seu tempo até a meta.';
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: bannerColor.withAlpha(25),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: bannerColor.withAlpha((0.35 * 255).round()),
        ),
      ),
      child: Row(
        children: [
          Icon(
            bannerIcon,
            color: bannerColor,
            size: 22,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              bannerText,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: bannerColor,
                    fontWeight: FontWeight.w600,
                  ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildComparisonPanel(BuildContext context, GoalSimulationResult result) {
    final GoalProjection baseline = result.baselineProjection;
    final GoalProjection simulated = result.simulatedProjection;

    final String baselineTimeLabel = baseline.isUnreachable
        ? 'Inalcançável'
        : '${baseline.years} anos e ${baseline.months} meses';
    final String simulatedTimeLabel = simulated.isUnreachable
        ? 'Inalcançável'
        : '${simulated.years} anos e ${simulated.months} meses';

    return Row(
      children: [
        Expanded(
          child: _buildTimeCard(
            context,
            title: 'Ritmo Atual',
            timeLabel: baselineTimeLabel,
            accentColor: kNavyBlue,
            isHighlighted: false,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildTimeCard(
            context,
            title: 'Cenário Simulado',
            timeLabel: simulatedTimeLabel,
            accentColor: simulated.isUnreachable ? kAmberGold : kEmerald,
            isHighlighted: true,
          ),
        ),
      ],
    );
  }

  Widget _buildTimeCard(
    BuildContext context, {
    required String title,
    required String timeLabel,
    required Color accentColor,
    required bool isHighlighted,
  }) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: accentColor.withAlpha(isHighlighted ? 20 : 12),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: accentColor.withAlpha(isHighlighted ? 90 : 40),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: Theme.of(context).textTheme.labelMedium?.copyWith(
                  color: kNavyBlue.withAlpha((0.75 * 255).round()),
                  fontWeight: FontWeight.w600,
                ),
          ),
          const SizedBox(height: 6),
          Text(
            timeLabel,
            style: GoogleFonts.jetBrainsMono(
              fontSize: 15,
              fontWeight: FontWeight.bold,
              color: accentColor,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContributionSlider(
    BuildContext context, {
    required double value,
    required double max,
    required String formattedLabel,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Aporte Mensal',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: kNavyBlue,
                    fontWeight: FontWeight.w600,
                  ),
            ),
            Flexible(
              child: Text(
                formattedLabel,
                textAlign: TextAlign.end,
                style: GoogleFonts.jetBrainsMono(
                  textStyle: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: kEmerald,
                        fontWeight: FontWeight.bold,
                      ),
                ),
              ),
            ),
          ],
        ),
        Slider(
          key: const Key('what_if_contribution_slider'),
          value: value.clamp(0.0, max),
          min: 0.0,
          max: max,
          activeColor: kEmerald,
          inactiveColor: kNavyBlue.withAlpha((0.15 * 255).round()),
          onChanged: (newValue) {
            setState(() {
              _simulatedContribution = newValue.roundToDouble();
            });
          },
        ),
      ],
    );
  }

  Widget _buildYieldSlider(
    BuildContext context, {
    required double value,
    required double max,
    required String formattedLabel,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Rendimento Esperado (%)',
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: kNavyBlue,
                    fontWeight: FontWeight.w600,
                  ),
            ),
            Flexible(
              child: Text(
                formattedLabel,
                textAlign: TextAlign.end,
                style: GoogleFonts.jetBrainsMono(
                  textStyle: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: kAmberGold,
                        fontWeight: FontWeight.bold,
                      ),
                ),
              ),
            ),
          ],
        ),
        Slider(
          key: const Key('what_if_yield_slider'),
          value: value.clamp(0.0, max),
          min: 0.0,
          max: max,
          activeColor: kAmberGold,
          inactiveColor: kNavyBlue.withAlpha((0.15 * 255).round()),
          onChanged: (newValue) {
            setState(() {
              _simulatedYield = (newValue * 10).round() / 10.0;
            });
          },
        ),
      ],
    );
  }
}
