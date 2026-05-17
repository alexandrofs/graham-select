import 'package:flutter/material.dart';

class FinancialDataTable<T> extends StatelessWidget {
  final List<T> items;
  final List<DataColumn> columns;
  final DataRow Function(T item) rowBuilder;
  final String? emptyMessage;

  const FinancialDataTable({
    super.key,
    required this.items,
    required this.columns,
    required this.rowBuilder,
    this.emptyMessage,
  });

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return Center(
        child: Text(emptyMessage ?? 'Nenhum dado encontrado'),
      );
    }

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: DataTable(
        columns: columns,
        rows: items.map((item) => rowBuilder(item)).toList(),
      ),
    );
  }
}
