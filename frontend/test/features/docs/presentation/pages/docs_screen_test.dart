import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/docs/presentation/pages/docs_screen.dart';

void main() {
  testWidgets('DocsScreen exibe marca Kerno mantendo metodologia Benjamin Graham', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: DocsScreen()));
    await tester.pumpAndSettle();

    expect(find.textContaining('Bem-vindo ao Kerno.'), findsOneWidget);
    expect(find.textContaining('Benjamin Graham'), findsOneWidget);
    expect(find.textContaining('Graham Select'), findsNothing);
  });
}
