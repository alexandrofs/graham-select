import 'package:flutter/material.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/dividend_announcement.dart';

class DividendScheduleCard extends StatelessWidget {
  final DividendAnnouncement announcement;

  const DividendScheduleCard({super.key, required this.announcement});

  @override
  Widget build(BuildContext context) {
    final typeColor = _getTypeColor(announcement.type);
    final formattedComDate = _formatDate(announcement.comDate);
    final formattedPayDate = announcement.paymentDate != null
        ? _formatDate(announcement.paymentDate!)
        : 'A definir';

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Text(
                      announcement.ticker,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: typeColor.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: typeColor.withValues(alpha: 0.4)),
                      ),
                      child: Text(
                        announcement.type,
                        style: TextStyle(
                          color: typeColor,
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
                Text(
                  'R\$ ${announcement.rate.toStringAsFixed(4).replaceAll('.', ',')} / cota',
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                _buildInfoCol('Data COM', formattedComDate),
                const SizedBox(width: 24),
                _buildInfoCol('Pagamento', formattedPayDate),
                const Spacer(),
                if (announcement.userShares != null && announcement.userShares! > 0)
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        '${announcement.userShares!.toStringAsFixed(0)} cotas',
                        style: const TextStyle(color: Colors.white60, fontSize: 12),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        'Total: R\$ ${(announcement.estimatedNetTotal ?? announcement.estimatedTotal ?? 0.0).toStringAsFixed(2).replaceAll('.', ',')}',
                        style: const TextStyle(
                          color: AppTheme.accentColor,
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoCol(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(color: Colors.white54, fontSize: 11),
        ),
        const SizedBox(height: 2),
        Text(
          value,
          style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w500),
        ),
      ],
    );
  }

  Color _getTypeColor(String type) {
    switch (type.toUpperCase()) {
      case 'JCP':
        return Colors.amber;
      case 'RENDIMENTO':
        return Colors.cyan;
      case 'BONIFICACAO':
      case 'DESDOBRAMENTO':
        return Colors.purpleAccent;
      case 'DIVIDENDO':
      default:
        return AppTheme.accentColor;
    }
  }

  String _formatDate(DateTime date) {
    final d = date.day.toString().padLeft(2, '0');
    final m = date.month.toString().padLeft(2, '0');
    final y = date.year.toString();
    return '$d/$m/$y';
  }
}
