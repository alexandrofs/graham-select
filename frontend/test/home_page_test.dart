import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';

void main() {
  testWidgets('HomePage should display Graham Select title', (
    WidgetTester tester,
  ) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const GrahamSelectApp());

    // Verify that the title is displayed
    expect(find.text('Graham Select'), findsOneWidget);

    // Verify that the hero copy is displayed
    expect(
      find.text('Invista com a metodologia de Benjamin Graham'),
      findsOneWidget,
    );

    // Verify that the buttons are present
    expect(find.text('Ver ranking agora'), findsOneWidget);
    expect(find.text('Entender metodologia'), findsOneWidget);
  });

  testWidgets('Navigation buttons should be tappable', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const GrahamSelectApp());

    // Find the "Ver ranking agora" button
    final rankingButton = find.text('Ver ranking agora');
    expect(rankingButton, findsOneWidget);

    // Scroll if needed and tap
    await tester.ensureVisible(rankingButton);
    await tester.tap(rankingButton);
    await tester.pumpAndSettle();

    // Verify navigation occurred (ranking page should be visible)
    expect(find.text('Top 20 Graham'), findsOneWidget);
  });
}
