import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

class PortfolioKpiCard extends StatelessWidget {
  final String title;
  final double value;
  final bool isPercentage;
  final bool isCurrency;
  final bool isLoading;
  final Color? valueColor;

  const PortfolioKpiCard({
    super.key,
    required this.title,
    required this.value,
    this.isPercentage = false,
    this.isCurrency = true,
    this.isLoading = false,
    this.valueColor,
  });

  @override
  Widget build(BuildContext context) {
    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final percentFormat = NumberFormat.decimalPattern('pt_BR');

    String formattedValue;
    if (isPercentage) {
      // De acordo com o code review, usar percentPattern seria melhor, mas o valor vem como 21.67 e não 0.2167
      formattedValue = '${percentFormat.format(value)}%';
    } else if (isCurrency) {
      formattedValue = currencyFormat.format(value);
    } else {
      formattedValue = percentFormat.format(value);
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    color: Colors.white70,
                  ),
            ),
            const SizedBox(height: 8),
            if (isLoading)
              _buildLoadingIndicator()
            else
              Text(
                formattedValue,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      color: valueColor ?? Colors.white,
                      fontWeight: FontWeight.bold,
                      fontFamily: 'JetBrains Mono',
                    ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildLoadingIndicator() {
    return Container(
      height: 24,
      width: 120,
      decoration: BoxDecoration(
        color: Colors.white10,
        borderRadius: BorderRadius.circular(4),
      ),
      child: const LinearProgressIndicator(
        backgroundColor: Colors.transparent,
        valueColor: AlwaysStoppedAnimation<Color>(Colors.white10),
      ),
    );
  }
}
