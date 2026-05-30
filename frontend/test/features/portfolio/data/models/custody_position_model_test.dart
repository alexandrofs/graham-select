import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/portfolio/data/models/custody_position_model.dart';

void main() {
  group('CustodyPositionModel.fromJson', () {
    // Cenário principal: backend serializa BigDecimal como String (ToStringSerializer)
    test('deve deserializar campos BigDecimal enviados como String pelo backend', () {
      final json = {
        'ticker': 'PETR4',
        'quantity': '12.00',          // String — padrão do backend Java
        'averagePrice': '31.00',
        'currentPrice': '34.10',
        'marketValue': '409.20',
        'gainLossPercentage': '10.00',
        'priceSource': 'LIVE',
        'priceUpdatedAt': '2026-05-30T14:00:00Z',
      };

      final model = CustodyPositionModel.fromJson(json);

      expect(model.ticker, equals('PETR4'));
      expect(model.quantity, closeTo(12.00, 0.001));
      expect(model.averagePrice, closeTo(31.00, 0.001));
      expect(model.currentPrice, closeTo(34.10, 0.001));
      expect(model.marketValue, closeTo(409.20, 0.001));
      expect(model.gainLossPercentage, closeTo(10.00, 0.001));
      expect(model.priceSource, equals('LIVE'));
      expect(model.priceUpdatedAt, isNotNull);
      expect(model.priceUpdatedAt!.isUtc, isTrue);
    });

    // Retrocompatibilidade: campos numéricos diretos (caso improvável mas defensivo)
    test('deve aceitar campos numéricos diretos sem quebrar', () {
      final json = {
        'ticker': 'VALE3',
        'quantity': 10.0,
        'averagePrice': 80.00,
        'currentPrice': 85.00,
        'marketValue': 850.00,
        'gainLossPercentage': 6.25,
        'priceSource': 'CACHE',
        'priceUpdatedAt': null,
      };

      final model = CustodyPositionModel.fromJson(json);

      expect(model.ticker, equals('VALE3'));
      expect(model.quantity, closeTo(10.0, 0.001));
      expect(model.priceSource, equals('CACHE'));
      expect(model.priceUpdatedAt, isNull);
    });

    test('deve retornar priceUpdatedAt null quando campo é null no JSON', () {
      final json = {
        'ticker': 'ITUB4',
        'quantity': '10.00',
        'averagePrice': '28.50',
        'currentPrice': '28.50',
        'marketValue': '285.00',
        'gainLossPercentage': '0.00',
        'priceSource': 'CACHE',
        'priceUpdatedAt': null,
      };

      final model = CustodyPositionModel.fromJson(json);

      expect(model.priceUpdatedAt, isNull);
      expect(model.priceSource, equals('CACHE'));
    });

    test('deve parsear priceUpdatedAt como DateTime UTC válido', () {
      final json = {
        'ticker': 'BBAS3',
        'quantity': '5.00',
        'averagePrice': '50.00',
        'currentPrice': '55.00',
        'marketValue': '275.00',
        'gainLossPercentage': '10.00',
        'priceSource': 'LIVE',
        'priceUpdatedAt': '2026-05-30T17:00:00Z',
      };

      final model = CustodyPositionModel.fromJson(json);

      expect(model.priceUpdatedAt, equals(DateTime.utc(2026, 5, 30, 17, 0, 0)));
    });

    test('deve lidar com gainLossPercentage negativo (posição no prejuízo)', () {
      final json = {
        'ticker': 'MGLU3',
        'quantity': '100.00',
        'averagePrice': '15.00',
        'currentPrice': '8.50',
        'marketValue': '850.00',
        'gainLossPercentage': '-43.33',
        'priceSource': 'LIVE',
        'priceUpdatedAt': '2026-05-30T14:00:00Z',
      };

      final model = CustodyPositionModel.fromJson(json);

      expect(model.gainLossPercentage, closeTo(-43.33, 0.001));
      expect(model.currentPrice, lessThan(model.averagePrice));
    });
  });
}
