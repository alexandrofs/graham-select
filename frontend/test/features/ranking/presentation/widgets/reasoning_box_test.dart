import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/ranking/domain/entities/graham_recommendation.dart';
import 'package:frontend/src/features/ranking/presentation/widgets/reasoning_box.dart';

void main() {
  Widget createWidgetUnderTest(GrahamRecommendation recommendation) {
    return MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: ReasoningBox(recommendation: recommendation),
          ),
        ),
      ),
    );
  }

  group('ReasoningBox Widget Tests', () {
    testWidgets('should display LPA, VPA, formula and margin of safety when epsUsed and bvpsUsed are present', (WidgetTester tester) async {
      const recommendation = GrahamRecommendation(
        ticker: 'PETR4',
        currentPrice: 30.00,
        intrinsicValue: 40.00,
        marginOfSafety: 0.33,
        currentAllocationPct: 5.00,
        targetAllocationPct: 20.00,
        allocationGap: 15.00,
        recommendationScore: 6.198,
        epsUsed: 4.52,
        bvpsUsed: 31.80,
      );

      await tester.pumpWidget(createWidgetUnderTest(recommendation));
      await tester.pump();

      // Verificar Ticker e Score
      expect(find.text('PETR4'), findsOneWidget);
      expect(find.text('Score: 6.20'), findsOneWidget);

      // Verificar LPA e VPA
      expect(find.text('LPA (Lucro Por Ação):'), findsOneWidget);
      expect(find.text('R\$ 4,52'), findsOneWidget);
      expect(find.text('VPA (Valor Patrimonial):'), findsOneWidget);
      expect(find.text('R\$ 31,80'), findsOneWidget);

      // Verificar fórmula de Graham e valor intrínseco
      expect(find.text('VI Graham = √(22,5 × LPA × VPA):'), findsOneWidget);
      expect(find.text('R\$ 40,00'), findsOneWidget);

      // Verificar preço atual e margem de segurança
      expect(find.text('Preço Atual:'), findsOneWidget);
      expect(find.text('R\$ 30,00'), findsOneWidget);
      expect(find.text('+33,00%'), findsOneWidget);
    });

    testWidgets('should display "Dado não disponível" when epsUsed and bvpsUsed are null', (WidgetTester tester) async {
      const recommendation = GrahamRecommendation(
        ticker: 'PETR4',
        currentPrice: 30.00,
        intrinsicValue: 40.00,
        marginOfSafety: 0.33,
        currentAllocationPct: 5.00,
        targetAllocationPct: 20.00,
        allocationGap: 15.00,
        recommendationScore: 6.198,
        epsUsed: null,
        bvpsUsed: null,
      );

      await tester.pumpWidget(createWidgetUnderTest(recommendation));
      await tester.pump();

      // Verificar Ticker e Score
      expect(find.text('PETR4'), findsOneWidget);

      // Verificar mensagem de dados indisponíveis
      expect(find.text('Dados de LPA/VPA não disponíveis'), findsOneWidget);

      // Ainda deve exibir preço e margem de segurança
      expect(find.text('Valor atual de mercado:'), findsOneWidget);
      expect(find.text('R\$ 30,00'), findsOneWidget);
      expect(find.text('+33,00%'), findsOneWidget);
    });

    testWidgets('should display "Sem meta de alocação definida" when targetAllocationPct is 0', (WidgetTester tester) async {
      const recommendation = GrahamRecommendation(
        ticker: 'PETR4',
        currentPrice: 30.00,
        intrinsicValue: 40.00,
        marginOfSafety: 0.33,
        currentAllocationPct: 0.00,
        targetAllocationPct: 0.00,
        allocationGap: 0.00,
        recommendationScore: 6.198,
      );

      await tester.pumpWidget(createWidgetUnderTest(recommendation));
      await tester.pump();

      // Verificar mensagem de alocação sem meta
      expect(find.text('Sem meta de alocação definida para este ativo'), findsOneWidget);
      expect(find.byType(LinearProgressIndicator), findsNothing);
    });

    testWidgets('should display marginOfSafety > 0 with correct badge style (Amber Gold)', (WidgetTester tester) async {
      const recommendation = GrahamRecommendation(
        ticker: 'PETR4',
        currentPrice: 30.00,
        intrinsicValue: 40.00,
        marginOfSafety: 0.33, // Margem positiva
        currentAllocationPct: 5.00,
        targetAllocationPct: 20.00,
        allocationGap: 15.00,
        recommendationScore: 6.198,
      );

      await tester.pumpWidget(createWidgetUnderTest(recommendation));
      await tester.pump();

      // Achar o texto da margem
      final textFinder = find.text('+33,00%');
      expect(textFinder, findsOneWidget);

      // Validar a cor do texto do widget
      final textWidget = tester.widget<Text>(textFinder);
      expect(textWidget.style?.color, ReasoningBox.amberGold);
    });

    testWidgets('should display marginOfSafety <= 0 with correct badge style (Red)', (WidgetTester tester) async {
      const recommendation = GrahamRecommendation(
        ticker: 'PETR4',
        currentPrice: 45.00,
        intrinsicValue: 40.00,
        marginOfSafety: -0.1111, // Margem negativa
        currentAllocationPct: 5.00,
        targetAllocationPct: 20.00,
        allocationGap: 15.00,
        recommendationScore: 1.20,
      );

      await tester.pumpWidget(createWidgetUnderTest(recommendation));
      await tester.pump();

      // Achar o texto da margem
      final textFinder = find.text('-11,11%');
      expect(textFinder, findsOneWidget);

      // Validar a cor do texto do widget
      final textWidget = tester.widget<Text>(textFinder);
      expect(textWidget.style?.color, ReasoningBox.errorRed);
    });
  });
}
