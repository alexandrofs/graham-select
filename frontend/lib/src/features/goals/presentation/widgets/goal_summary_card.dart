import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import '../../domain/entities/financial_goal.dart';

class GoalSummaryCard extends StatelessWidget {
  final FinancialGoal goal;
  final double currentPatrimony;

  const GoalSummaryCard({
    super.key,
    required this.goal,
    required this.currentPatrimony,
  });

  @override
  Widget build(BuildContext context) {
    const Color kNavyBlue = Color(0xFF1B2A4A);
    const Color kEmerald = Color(0xFF10B981);
    const Color kAmberGold = Color(0xFFF59E0B);

    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final isPatrimony = goal.goalType == 'PATRIMONY_TARGET';

    final targetValueFormatted = currencyFormat.format(goal.targetValue);
    final contributionFormatted = currencyFormat.format(goal.monthlyContribution);

    final double remainingValue = goal.targetValue - currentPatrimony;
    final bool goalAchieved = remainingValue <= 0;

    String motivationalText;
    Color highlightColor;

    if (isPatrimony) {
      if (goalAchieved) {
        motivationalText = '🎉 Parabéns! Você já atingiu sua meta de patrimônio!';
        highlightColor = kEmerald;
      } else {
        final remainingFormatted = currencyFormat.format(remainingValue);
        motivationalText = 'Você precisa de $remainingFormatted para atingir sua meta.';
        highlightColor = kAmberGold;
      }
    } else {
      motivationalText = 'Meta: gerar $targetValueFormatted/mês de renda passiva.';
      highlightColor = kAmberGold;
    }

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: kNavyBlue.withAlpha((0.1 * 255).round()),
          width: 1,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: highlightColor.withAlpha((0.1 * 255).round()),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.flag,
                    color: highlightColor,
                    size: 28,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    isPatrimony ? 'Meta de Patrimônio Alvo' : 'Meta de Renda Mensal Alvo',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          color: kNavyBlue,
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            _buildInfoRow(
              context,
              'Valor Alvo:',
              targetValueFormatted,
            ),
            const SizedBox(height: 12),
            _buildInfoRow(
              context,
              'Aporte Mensal Pretendido:',
              contributionFormatted,
            ),
            const SizedBox(height: 12),
            _buildInfoRow(
              context,
              'Prazo Estimado:',
              '${goal.estimatedYears} anos',
            ),
            // Renda passiva mensal exibida na frase motivacional em destaque abaixo
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 16),
            Center(
              child: Text(
                motivationalText,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      color: highlightColor,
                      fontWeight: FontWeight.bold,
                    ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoRow(
    BuildContext context,
    String label,
    String value, {
    bool isLabelBold = false,
    bool isHighlight = false,
  }) {
    const Color kNavyBlue = Color(0xFF1B2A4A);
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: kNavyBlue.withAlpha((0.7 * 255).round()),
                fontWeight: isLabelBold ? FontWeight.bold : FontWeight.normal,
              ),
        ),
        Flexible(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: isHighlight
                ? Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: const Color(0xFF10B981),
                      fontWeight: FontWeight.bold,
                    )
                : GoogleFonts.jetBrainsMono(
                    textStyle: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: kNavyBlue,
                          fontWeight: FontWeight.bold,
                        ),
                  ),
          ),
        ),
      ],
    );
  }
}
