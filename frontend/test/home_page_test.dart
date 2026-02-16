import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';

void main() {
  testWidgets('HomePage should display Graham Select title', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const GrahamSelectApp());

    // Verify that the title is displayed
    expect(find.text('Graham Select'), findsOneWidget);
    
    // Verify that the subtitle is displayed
    expect(find.text('Sua inteligência financeira para investimentos de valor.'), findsOneWidget);
    
    // Verify that the buttons are present
    expect(find.text('Ver Ranking de Empresas'), findsOneWidget);
    expect(find.text('Fazer Upload de Dados'), findsOneWidget);
  });

  testWidgets('Navigation buttons should be tappable', (WidgetTester tester) async {
    await tester.pumpWidget(const GrahamSelectApp());

    // Find the "Ver Ranking" button
    final rankingButton = find.text('Ver Ranking de Empresas');
    expect(rankingButton, findsOneWidget);

    // Tap the button
    await tester.tap(rankingButton);
    await tester.pumpAndSettle();

    // Verify navigation occurred (placeholder page should be visible)
    expect(find.text('Ranking Page - Em breve'), findsOneWidget);
  });
}
