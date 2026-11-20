import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/monthly_tax_calculation.dart';

class MonthlyTaxCard extends StatefulWidget {
  final MonthlyTaxCalculation month;

  const MonthlyTaxCard({super.key, required this.month});

  @override
  State<MonthlyTaxCard> createState() => _MonthlyTaxCardState();
}

class _MonthlyTaxCardState extends State<MonthlyTaxCard> {
  bool _isExpanded = false;

  static const double _twentyThousand = 20000.0;
  static const List<String> _monthNames = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  @override
  Widget build(BuildContext context) {
    final currencyFormatter = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final monthName = (widget.month.month >= 1 && widget.month.month <= 12)
        ? _monthNames[widget.month.month - 1]
        : 'Mês ${widget.month.month}';

    final hasOperations = widget.month.operations.isNotEmpty;
    final totalEquitySales = widget.month.totalEquitySales;
    final netResultMonth = widget.month.operations.fold<double>(0.0, (acc, op) => acc + op.netProfitOrLoss);
    final progress = (totalEquitySales / _twentyThousand).clamp(0.0, 1.0);
    final isExempt = widget.month.isEquityExempt;
    final progressColor = isExempt ? AppTheme.accentColor : Colors.amberAccent;

    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Row: Month Name, Net Result and Status Badge
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(
                      hasOperations ? Icons.calendar_month : Icons.calendar_today_outlined,
                      color: hasOperations ? AppTheme.primaryColor : Colors.white38,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '$monthName/${widget.month.year}',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                  ],
                ),
                Row(
                  children: [
                    if (hasOperations) ...[
                      _buildNetResultChip(netResultMonth, currencyFormatter),
                      const SizedBox(width: 8),
                    ],
                    _buildStatusBadge(widget.month.darfStatus),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Progress Bar for R$ 20.000,00 equity exemption (only when equity sales exist)
            if (totalEquitySales > 0) ...[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      'Alienação de Ações: ${currencyFormatter.format(totalEquitySales)} / R\$ 20.000,00',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.white70),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    isExempt ? 'Isento' : 'Tributável (>20k)',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: progressColor,
                        ),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: progress,
                  minHeight: 6,
                  backgroundColor: Colors.white.withValues(alpha: 0.1),
                  valueColor: AlwaysStoppedAnimation<Color>(progressColor),
                ),
              ),
              const SizedBox(height: 12),
            ],

            // DARF Box (if applicable)
            if (widget.month.darfStatus == DarfStatus.due) ...[
              _buildDarfCard(context, currencyFormatter),
              const SizedBox(height: 12),
            ] else if (widget.month.darfStatus == DarfStatus.accumulatedBelowMinimum) ...[
              _buildAccumulatedDarfNotice(currencyFormatter),
              const SizedBox(height: 12),
            ],

            // Operations Expandable Section
            if (hasOperations) ...[
              InkWell(
                onTap: () => setState(() => _isExpanded = !_isExpanded),
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4.0),
                  child: Row(
                    children: [
                      Icon(
                        _isExpanded ? Icons.keyboard_arrow_up : Icons.keyboard_arrow_down,
                        size: 20,
                        color: AppTheme.primaryColor,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        _isExpanded
                            ? 'Ocultar alienações do mês (${widget.month.operations.length})'
                            : 'Ver alienações do mês (${widget.month.operations.length})',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: AppTheme.primaryColor,
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                    ],
                  ),
                ),
              ),
              if (_isExpanded) ...[
                const SizedBox(height: 8),
                _buildOperationsTable(context, currencyFormatter),
              ],
            ] else ...[
              Text(
                'Sem operações de alienação registradas neste mês.',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.white38),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildNetResultChip(double netResult, NumberFormat currencyFormatter) {
    final isProfit = netResult > 0;
    final isLoss = netResult < 0;
    final color = isProfit
        ? AppTheme.accentColor
        : (isLoss ? Colors.redAccent : Colors.white60);
    final prefix = isProfit ? '+' : '';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withValues(alpha: 0.3)),
      ),
      child: Text(
        '$prefix${currencyFormatter.format(netResult)}',
        style: TextStyle(
          color: color,
          fontSize: 11,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildStatusBadge(DarfStatus status) {
    Color bg;
    Color fg;
    switch (status) {
      case DarfStatus.due:
        bg = Colors.red.withValues(alpha: 0.2);
        fg = Colors.redAccent;
        break;
      case DarfStatus.accumulatedBelowMinimum:
        bg = Colors.amber.withValues(alpha: 0.2);
        fg = Colors.amberAccent;
        break;
      case DarfStatus.exempt:
        bg = AppTheme.accentColor.withValues(alpha: 0.2);
        fg = AppTheme.accentColor;
        break;
      case DarfStatus.loss:
        bg = Colors.blueGrey.withValues(alpha: 0.2);
        fg = Colors.lightBlueAccent;
        break;
      case DarfStatus.noTaxDue:
        bg = Colors.white.withValues(alpha: 0.08);
        fg = Colors.white60;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        status.label,
        style: TextStyle(
          color: fg,
          fontSize: 12,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildDarfCard(BuildContext context, NumberFormat currencyFormatter) {
    final dateFormat = DateFormat('dd/MM/yyyy');
    final dueDateStr = widget.month.darfDueDate != null
        ? dateFormat.format(widget.month.darfDueDate!)
        : '-';

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.redAccent.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.redAccent.withValues(alpha: 0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    const Icon(Icons.payment_outlined, color: Colors.redAccent, size: 20),
                    const SizedBox(width: 8),
                    Flexible(
                      child: Text(
                        'Guia DARF - Código 6015',
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.redAccent,
                            ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              FilledButton.tonalIcon(
                onPressed: () => _copyDarfToClipboard(context),
                icon: const Icon(Icons.copy, size: 16),
                label: const Text('Copiar Dados do DARF'),
                style: FilledButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  visualDensity: VisualDensity.compact,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Text(
                'Valor: ${currencyFormatter.format(widget.month.darfAmount)}',
                style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
              ),
              const SizedBox(width: 24),
              Text(
                'Vencimento: $dueDateStr',
                style: const TextStyle(color: Colors.white70),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAccumulatedDarfNotice(NumberFormat currencyFormatter) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.amber.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.amber.withValues(alpha: 0.2)),
      ),
      child: Row(
        children: [
          const Icon(Icons.info_outline, color: Colors.amberAccent, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'Imposto gerado (${currencyFormatter.format(widget.month.accumulatedTaxUnder10)}) é inferior ao limite de R\$ 10,00 e foi acumulado para os meses seguintes.',
              style: const TextStyle(fontSize: 12, color: Colors.white70),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOperationsTable(BuildContext context, NumberFormat currencyFormatter) {
    final dateFormat = DateFormat('dd/MM/yyyy');

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: DataTable(
        headingRowHeight: 36,
        dataRowMinHeight: 32,
        dataRowMaxHeight: 40,
        horizontalMargin: 12,
        columnSpacing: 16,
        columns: const [
          DataColumn(label: Text('Data')),
          DataColumn(label: Text('Ticker')),
          DataColumn(label: Text('Tipo')),
          DataColumn(label: Text('Qtd')),
          DataColumn(label: Text('Preço Venda')),
          DataColumn(label: Text('Custo Médio')),
          DataColumn(label: Text('Total Venda')),
          DataColumn(label: Text('Lucro/Prejuízo')),
        ],
        rows: widget.month.operations.map((op) {
          final isProfit = op.netProfitOrLoss >= 0;
          final resultColor = isProfit ? AppTheme.accentColor : Colors.redAccent;

          return DataRow(cells: [
            DataCell(Text(dateFormat.format(op.tradeDate), style: const TextStyle(fontSize: 12))),
            DataCell(Text(op.ticker, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12))),
            DataCell(Text(op.side, style: const TextStyle(fontSize: 12))),
            DataCell(Text(
              op.quantity % 1 == 0 ? op.quantity.toInt().toString() : op.quantity.toString(),
              style: const TextStyle(fontSize: 12),
            )),
            DataCell(Text(currencyFormatter.format(op.price), style: const TextStyle(fontSize: 12))),
            DataCell(Text(currencyFormatter.format(op.averagePrice), style: const TextStyle(fontSize: 12))),
            DataCell(Text(currencyFormatter.format(op.totalSaleValue), style: const TextStyle(fontSize: 12))),
            DataCell(Text(
              '${isProfit ? '+' : ''}${currencyFormatter.format(op.netProfitOrLoss)}',
              style: TextStyle(fontWeight: FontWeight.bold, color: resultColor, fontSize: 12),
            )),
          ]);
        }).toList(),
      ),
    );
  }

  void _copyDarfToClipboard(BuildContext context) {
    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: '');
    final dateFormat = DateFormat('dd/MM/yyyy');
    final compFormat = DateFormat('MM/yyyy');

    final dueDateStr = widget.month.darfDueDate != null
        ? dateFormat.format(widget.month.darfDueDate!)
        : '-';

    final text = '''
=== GUIA DARF - CÓDIGO 6015 ===
Período de Apuração: ${compFormat.format(widget.month.monthDate)}
Código da Receita: 6015
Data de Vencimento: $dueDateStr
Valor do Principal: R\$ ${currencyFormat.format(widget.month.darfAmount)}
Observação: Recolhimento de IRPF sobre Ganhos Líquidos em Operações em Bolsa de Valores.
''';

    Clipboard.setData(ClipboardData(text: text));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Dados da Guia DARF copiados para a área de transferência!'),
        backgroundColor: AppTheme.accentColor,
      ),
    );
  }
}
