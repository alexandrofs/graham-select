import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/core/widgets/financial_data_table.dart';

class _SampleRow {
  final String ticker;
  final String value;

  const _SampleRow(this.ticker, this.value);
}

Widget _buildTableApp({
  required List<_SampleRow> items,
  String? emptyMessage,
  String? regulatoryFooter,
  ValueChanged<bool?>? onSelectChanged,
  VoidCallback? onLongPress,
  VoidCallback? onCellTap,
}) {
  return MaterialApp(
    theme: AppTheme.lightTheme,
    home: Scaffold(
      body: SingleChildScrollView(
        child: FinancialDataTable<_SampleRow>(
          items: items,
          emptyMessage: emptyMessage,
          regulatoryFooter: regulatoryFooter,
          columns: const [
            DataColumn(label: Text('Ativo')),
            DataColumn(label: Text('Valor')),
          ],
          rowBuilder: (item) => DataRow(
            onSelectChanged: onSelectChanged,
            onLongPress: onLongPress,
            cells: [
              DataCell(Text(item.ticker), onTap: onCellTap),
              DataCell(Text(item.value)),
            ],
          ),
        ),
      ),
    ),
  );
}

void main() {
  testWidgets(
      'FinancialDataTable renderiza ListView de cartões sem scroll horizontal e preserva callbacks em < 600dp',
      (tester) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    var selectedCalls = 0;
    var longPressCalls = 0;
    var cellTapCalls = 0;

    await tester.pumpWidget(
      _buildTableApp(
        items: const [_SampleRow('PETR4', 'R\$ 3.500,00')],
        regulatoryFooter: 'Dados ilustrativos',
        onSelectChanged: (_) => selectedCalls++,
        onLongPress: () => longPressCalls++,
        onCellTap: () => cellTapCalls++,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(ListView), findsOneWidget);
    expect(find.byType(DataTable), findsNothing);
    expect(
      find.byWidgetPredicate(
        (w) =>
            w is SingleChildScrollView &&
            w.scrollDirection == Axis.horizontal,
      ),
      findsNothing,
    );

    await tester.tap(find.text('PETR4'));
    expect(cellTapCalls, 1);

    await tester.tap(find.text('R\$ 3.500,00'));
    expect(selectedCalls, 1);

    await tester.longPress(find.text('R\$ 3.500,00'));
    expect(longPressCalls, 1);
    expect(find.text('Dados ilustrativos'), findsOneWidget);
  });

  testWidgets(
      'FinancialDataTable renderiza DataTable tabular com rodapé em >= 600dp e suporta largura ilimitada',
      (tester) async {
    tester.view.physicalSize = const Size(800, 600);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: Scaffold(
          body: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: FinancialDataTable<_SampleRow>(
              items: const [_SampleRow('VALE3', 'R\$ 4.200,00')],
              regulatoryFooter: 'Cotações B3',
              columns: const [
                DataColumn(label: Text('Ativo')),
                DataColumn(label: Text('Valor')),
              ],
              rowBuilder: (item) => DataRow(
                cells: [
                  DataCell(Text(item.ticker)),
                  DataCell(Text(item.value)),
                ],
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(DataTable), findsOneWidget);
    expect(find.text('VALE3'), findsOneWidget);
    expect(find.text('Cotações B3'), findsOneWidget);
  });

  testWidgets('FinancialDataTable exibe emptyMessage centralizado quando vazio',
      (tester) async {
    await tester.pumpWidget(
      _buildTableApp(
        items: const [],
        emptyMessage: 'Sem registros disponíveis',
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Sem registros disponíveis'), findsOneWidget);
    expect(find.byType(DataTable), findsNothing);
    expect(find.byType(ListView), findsNothing);
  });
}
