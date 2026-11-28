import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../domain/entities/tax_declaration_item.dart';
import '../../../../core/security/privacy_provider.dart';
import '../../../../core/theme/app_theme.dart';

class TaxItemCard extends StatelessWidget {
  final TaxDeclarationItem item;
  final int previousYear;
  final int reportYear;

  const TaxItemCard({
    super.key,
    required this.item,
    required this.previousYear,
    required this.reportYear,
  });

  @override
  Widget build(BuildContext context) {
    final isMasked = Provider.of<PrivacyProvider?>(context)?.isPrivacyMode ?? false;
    final currencyFormatter = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final quantityFormatter = NumberFormat('#,##0.####', 'pt_BR');
    String formatMoney(double value) =>
        isMasked ? PrivacyProvider.maskedCurrencyValue : currencyFormatter.format(value);

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 8.0),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Wrap(
                    spacing: 8,
                    runSpacing: 4,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppTheme.primaryColor.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(color: AppTheme.primaryColor.withValues(alpha: 0.3)),
                        ),
                        child: Text(
                          'Grupo ${item.groupCode} • Código ${item.itemCode}',
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: AppTheme.primaryColor,
                          ),
                        ),
                      ),
                      Text(
                        item.ticker,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.bold,
                            ),
                      ),
                      if (item.liquidated)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.orange.withValues(alpha: 0.2),
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(color: Colors.orange.withValues(alpha: 0.4)),
                          ),
                          child: const Text(
                            'Alienado no ano',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: Colors.orange,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                TextButton.icon(
                  key: Key('copy_discrimination_${item.ticker}'),
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: item.discrimination));
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text('Discriminação de ${item.ticker} copiada com sucesso!'),
                        backgroundColor: AppTheme.accentColor,
                        duration: const Duration(seconds: 2),
                      ),
                    );
                  },
                  icon: const Icon(Icons.copy_rounded, size: 16),
                  label: const Text('Copiar'),
                  style: TextButton.styleFrom(
                    foregroundColor: AppTheme.accentColor,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              item.companyName,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Colors.white70,
                  ),
            ),
            const SizedBox(height: 4),
            Text(
              'CNPJ: ${item.cnpj}',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Colors.white54,
                  ),
            ),
            const Divider(height: 24),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Situação em 31/12/$previousYear',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.white60),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        formatMoney(item.previousYearValue),
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Situação em 31/12/$reportYear',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.white60),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        formatMoney(item.currentYearValue),
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                          color: item.liquidated ? Colors.white60 : Colors.white,
                        ),
                      ),
                    ],
                  ),
                ),
                if (!item.liquidated) ...[
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Quantidade / PM',
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.white60),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '${quantityFormatter.format(item.quantity)} • ${formatMoney(item.averagePrice)}',
                          style: const TextStyle(fontSize: 13, color: Colors.white70),
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
            const SizedBox(height: 14),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.black.withValues(alpha: 0.25),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
              ),
              child: SelectableText(
                item.discrimination,
                style: const TextStyle(
                  fontSize: 12.5,
                  height: 1.4,
                  color: Colors.white70,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
