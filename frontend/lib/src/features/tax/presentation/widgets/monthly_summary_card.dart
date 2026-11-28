import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../../core/security/privacy_provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/annual_tax_summary.dart';

class MonthlySummaryCard extends StatelessWidget {
  final AnnualTaxSummary summary;

  const MonthlySummaryCard({super.key, required this.summary});

  @override
  Widget build(BuildContext context) {
    final isMasked = Provider.of<PrivacyProvider?>(context)?.isPrivacyMode ?? false;
    final currencyFormatter = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    String formatMoney(double value) =>
        isMasked ? PrivacyProvider.maskedCurrencyValue : currencyFormatter.format(value);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.receipt_long_outlined, color: AppTheme.primaryColor, size: 24),
                const SizedBox(width: 8),
                Text(
                  'Resumo Anual de Apuração Fiscal (${summary.year})',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            LayoutBuilder(
              builder: (context, constraints) {
                final isNarrow = constraints.maxWidth < 750;
                if (isNarrow) {
                  return Column(
                    children: [
                      _buildMetric(
                        context,
                        'Total Imposto Devido',
                        formatMoney(summary.totalTaxDueYear),
                        summary.totalTaxDueYear > 0 ? Colors.amberAccent : AppTheme.accentColor,
                      ),
                      const SizedBox(height: 12),
                      _buildMetric(
                        context,
                        'Volume Total Alienado',
                        formatMoney(summary.totalSalesYear),
                        AppTheme.textColor,
                      ),
                      const SizedBox(height: 12),
                      _buildMetric(
                        context,
                        'Lucro Isento de Ações',
                        formatMoney(summary.totalExemptProfitYear),
                        AppTheme.accentColor,
                      ),
                      const SizedBox(height: 12),
                      _buildMetric(
                        context,
                        'Lucro Tributável Consolidado',
                        formatMoney(summary.totalTaxableProfitYear),
                        summary.totalTaxableProfitYear > 0 ? Colors.orangeAccent : AppTheme.textColor,
                      ),
                      const SizedBox(height: 12),
                      _buildMetric(
                        context,
                        'Prejuízo a Compensar (Comum)',
                        formatMoney(summary.finalCommonLossCarryforward),
                        summary.finalCommonLossCarryforward > 0 ? Colors.redAccent : Colors.white70,
                      ),
                      const SizedBox(height: 12),
                      _buildMetric(
                        context,
                        'Prejuízo a Compensar (FII/Fiagro)',
                        formatMoney(summary.finalFiiLossCarryforward),
                        summary.finalFiiLossCarryforward > 0 ? Colors.redAccent : Colors.white70,
                      ),
                    ],
                  );
                }

                return Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: _buildMetric(
                            context,
                            'Total Imposto Devido',
                            formatMoney(summary.totalTaxDueYear),
                            summary.totalTaxDueYear > 0 ? Colors.amberAccent : AppTheme.accentColor,
                          ),
                        ),
                        Container(width: 1, height: 48, color: Colors.white.withValues(alpha: 0.1)),
                        Expanded(
                          child: _buildMetric(
                            context,
                            'Volume Total Alienado',
                            formatMoney(summary.totalSalesYear),
                            AppTheme.textColor,
                          ),
                        ),
                        Container(width: 1, height: 48, color: Colors.white.withValues(alpha: 0.1)),
                        Expanded(
                          child: _buildMetric(
                            context,
                            'Lucro Isento de Ações',
                            formatMoney(summary.totalExemptProfitYear),
                            AppTheme.accentColor,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Divider(color: Colors.white.withValues(alpha: 0.08)),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: _buildMetric(
                            context,
                            'Lucro Tributável Consolidado',
                            formatMoney(summary.totalTaxableProfitYear),
                            summary.totalTaxableProfitYear > 0 ? Colors.orangeAccent : AppTheme.textColor,
                          ),
                        ),
                        Container(width: 1, height: 48, color: Colors.white.withValues(alpha: 0.1)),
                        Expanded(
                          child: _buildMetric(
                            context,
                            'Prejuízo a Compensar (Comum)',
                            formatMoney(summary.finalCommonLossCarryforward),
                            summary.finalCommonLossCarryforward > 0 ? Colors.redAccent : Colors.white70,
                          ),
                        ),
                        Container(width: 1, height: 48, color: Colors.white.withValues(alpha: 0.1)),
                        Expanded(
                          child: _buildMetric(
                            context,
                            'Prejuízo a Compensar (FII/Fiagro)',
                            formatMoney(summary.finalFiiLossCarryforward),
                            summary.finalFiiLossCarryforward > 0 ? Colors.redAccent : Colors.white70,
                          ),
                        ),
                      ],
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
