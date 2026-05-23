import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/portfolio/presentation/widgets/portfolio_kpi_card.dart';

void main() {
  group('PortfolioKpiCard', () {
    testWidgets('should render title and formatted currency value', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: PortfolioKpiCard(
              title: 'Patrimônio Total',
              value: 1250.50,
            ),
          ),
        ),
      );

      expect(find.text('Patrimônio Total'), findsOneWidget);
      expect(find.textContaining('1.250,50'), findsOneWidget);
    });

    testWidgets('should render percentage value', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: PortfolioKpiCard(
              title: 'Rendimento',
              value: 15.5,
              isPercentage: true,
              isCurrency: false,
            ),
          ),
        ),
      );

      expect(find.text('15,5%'), findsOneWidget);
    });

    testWidgets('should show loading indicator when isLoading is true', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: PortfolioKpiCard(
              title: 'Loading Test',
              value: 100,
              isLoading: true,
            ),
          ),
        ),
      );

      expect(find.byType(LinearProgressIndicator), findsOneWidget);
      expect(find.textContaining('100,00'), findsNothing);
    });

    testWidgets('should apply explicit valueColor', (tester) async {
      const customColor = Color(0xFF10B981);
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: PortfolioKpiCard(
              title: 'Color Test',
              value: 100.0,
              valueColor: customColor,
            ),
          ),
        ),
      );

      final textWidget = tester.widget<Text>(find.textContaining('100,00'));
      expect(textWidget.style?.color, customColor);
    });
  });
}
