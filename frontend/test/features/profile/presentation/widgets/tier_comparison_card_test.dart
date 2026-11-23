import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/profile/presentation/widgets/tier_comparison_card.dart';

void main() {
  testWidgets('TierComparisonCard exibe copy Kerno sem marca legada', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(child: TierComparisonCard()),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('poder do Kerno'), findsOneWidget);
    expect(
      find.text('O núcleo inteligente para sua gestão de patrimônio.'),
      findsOneWidget,
    );
    expect(find.textContaining('Graham Select'), findsNothing);
    expect(find.textContaining('cérebro insubstituível'), findsNothing);
  });
}
