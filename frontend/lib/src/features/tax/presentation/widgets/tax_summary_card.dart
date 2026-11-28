import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../domain/entities/tax_declaration_report.dart';
import '../../../../core/security/privacy_provider.dart';
import '../../../../core/theme/app_theme.dart';

class TaxSummaryCard extends StatelessWidget {
  final TaxDeclarationReport report;

  const TaxSummaryCard({super.key, required this.report});

  @override
  Widget build(BuildContext context) {
    final isMasked = Provider.of<PrivacyProvider?>(context)?.isPrivacyMode ?? false;
    final currencyFormatter = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    String formatMoney(double value) =>
        isMasked ? PrivacyProvider.maskedCurrencyValue : currencyFormatter.format(value);
    final isPositive = report.netVariation >= 0;
    final variationColor = isPositive ? AppTheme.accentColor : Colors.redAccent;
    final variationText = isMasked
        ? PrivacyProvider.maskedCurrencyValue
        : '${isPositive ? '+' : ''}${currencyFormatter.format(report.netVariation)} (${report.percentageVariation.toStringAsFixed(2)}%)';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.account_balance_outlined, color: AppTheme.primaryColor, size: 24),
                const SizedBox(width: 8),
                Text(
                  'Posição Patrimonial Consolidada',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            LayoutBuilder(
              builder: (context, constraints) {
                final isNarrow = constraints.maxWidth < 600;
                if (isNarrow) {
                  return Column(
                    children: [
                      _buildMetric(
                        context,
                        'Situação em 31/12/${report.previousYear}',
                        formatMoney(report.previousYearTotal),
                        Colors.white70,
                      ),
                      const SizedBox(height: 12),
                      _buildMetric(
                        context,
                        'Situação em 31/12/${report.year}',
                        formatMoney(report.currentYearTotal),
                        AppTheme.textColor,
                      ),
                      const SizedBox(height: 12),
                      _buildMetric(
                        context,
                        'Variação Patrimonial',
                        variationText,
                        variationColor,
                      ),
                    ],
                  );
                }

                return Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: _buildMetric(
                        context,
                        'Situação em 31/12/${report.previousYear}',
                        formatMoney(report.previousYearTotal),
                        Colors.white70,
                      ),
                    ),
                    Container(
                      width: 1,
                      height: 48,
                      color: Colors.white.withValues(alpha: 0.1),
                    ),
                    Expanded(
                      child: _buildMetric(
                        context,
                        'Situação em 31/12/${report.year}',
                        formatMoney(report.currentYearTotal),
                        AppTheme.textColor,
                      ),
                    ),
                    Container(
                      width: 1,
                      height: 48,
                      color: Colors.white.withValues(alpha: 0.1),
                    ),
                    Expanded(
                      child: _buildMetric(
                        context,
                        'Variação Patrimonial',
                        variationText,
                        variationColor,
                      ),
                    ),
                  ],
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetric(BuildContext context, String label, String value, Color valueColor) {
    final isMasked = value == PrivacyProvider.maskedCurrencyValue;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Colors.white60,
                ),
          ),
          const SizedBox(height: 4),
          Semantics(
            label: isMasked ? '$label: ${PrivacyProvider.maskedSemanticsLabel}' : '$label: $value',
            child: Text(
              value,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: valueColor,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}
