import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../domain/entities/ingestion_audit.dart';
import '../../../../core/widgets/financial_data_table.dart';

class IngestionHistoryTable extends StatelessWidget {
  final List<IngestionAudit> audits;

  const IngestionHistoryTable({super.key, required this.audits});

  @override
  Widget build(BuildContext context) {
    return FinancialDataTable<IngestionAudit>(
      items: audits,
      emptyMessage: 'Nenhum histórico de importação encontrado',
      columns: const [
        DataColumn(label: Text('Nome do Arquivo')),
        DataColumn(label: Text('Data/Hora')),
        DataColumn(label: Text('Status')),
        DataColumn(label: Text('Sucesso')),
        DataColumn(label: Text('Erro')),
      ],
      rowBuilder: (audit) {
        return DataRow(cells: [
          DataCell(Text(audit.fileName)),
          DataCell(Text(DateFormat('dd/MM/yyyy HH:mm').format(audit.uploadDate))),
          DataCell(_buildStatusBadge(audit.status)),
          DataCell(Text(audit.successfulLines.toString())),
          DataCell(Text(audit.errorLines.toString())),
        ]);
      },
    );
  }

  Widget _buildStatusBadge(String status) {
    Color color;
    switch (status) {
      case 'SUCESSO':
        color = Colors.green;
        break;
      case 'PARCIAL':
      case 'PROCESSANDO':
        color = Colors.amber;
        break;
      case 'ERRO':
        color = Colors.red;
        break;
      default:
        color = Colors.grey;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color),
      ),
      child: Text(
        status,
        style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12),
      ),
    );
  }
}
