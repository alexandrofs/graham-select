import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/ranking/data/models/graham_recommendation_model.dart';

void main() {
  group('GrahamRecommendationModel', () {
    test('fromJson deve desserializar corretamente com números do tipo double e int', () {
      final json = {
        'ticker': 'PETR4',
        'currentPrice': 35,
        'intrinsicValue': 42.5,
        'marginOfSafety': 21.4,
        'currentAllocationPct': 10,
        'targetAllocationPct': 15,
        'allocationGap': 5.0,
        'recommendationScore': 85,
        'epsUsed': 7,
        'bvpsUsed': 25.8,
      };

      final model = GrahamRecommendationModel.fromJson(json);

      expect(model.ticker, 'PETR4');
      expect(model.currentPrice, 35.0);
      expect(model.intrinsicValue, 42.5);
      expect(model.marginOfSafety, 21.4);
      expect(model.currentAllocationPct, 10.0);
      expect(model.targetAllocationPct, 15.0);
      expect(model.allocationGap, 5.0);
      expect(model.recommendationScore, 85.0);
      expect(model.epsUsed, 7.0);
      expect(model.bvpsUsed, 25.8);
    });

    test('fromJson deve desserializar com segurança quando números chegam como String', () {
      final json = {
        'ticker': 'VALE3',
        'currentPrice': '65.20',
        'intrinsicValue': '80.00',
        'marginOfSafety': '22.7',
        'currentAllocationPct': '5.5',
        'targetAllocationPct': '10.0',
        'allocationGap': '4.5',
        'recommendationScore': '90.2',
        'epsUsed': '8.5',
        'bvpsUsed': '45.1',
      };

      final model = GrahamRecommendationModel.fromJson(json);

      expect(model.ticker, 'VALE3');
      expect(model.currentPrice, 65.20);
      expect(model.intrinsicValue, 80.0);
      expect(model.marginOfSafety, 22.7);
      expect(model.currentAllocationPct, 5.5);
      expect(model.targetAllocationPct, 10.0);
      expect(model.allocationGap, 4.5);
      expect(model.recommendationScore, 90.2);
      expect(model.epsUsed, 8.5);
      expect(model.bvpsUsed, 45.1);
    });

    test('fromJson deve adotar valores default e nulos seguros quando campos estão ausentes ou inválidos', () {
      final json = <String, dynamic>{
        'currentPrice': 'invalid_number',
      };

      final model = GrahamRecommendationModel.fromJson(json);

      expect(model.ticker, '');
      expect(model.currentPrice, 0.0);
      expect(model.intrinsicValue, 0.0);
      expect(model.marginOfSafety, 0.0);
      expect(model.currentAllocationPct, 0.0);
      expect(model.targetAllocationPct, 0.0);
      expect(model.allocationGap, 0.0);
      expect(model.recommendationScore, 0.0);
      expect(model.epsUsed, isNull);
      expect(model.bvpsUsed, isNull);
    });
  });
}
