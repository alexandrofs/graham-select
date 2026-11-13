import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/admin/data/models/admin_ticker_model.dart';

void main() {
  group('AdminTickerModel', () {
    test('deve deserializar corretamente a partir de JSON válido', () {
      final json = {
        'id': 'comp-1',
        'ticker': '  bova11  ',
        'name': 'iShares Ibovespa Fundo de Índice',
        'assetClass': 'ETFs',
        'sector': 'Índices',
        'createdAt': '2026-06-13T10:00:00.000Z',
        'updatedAt': '2026-06-13T11:30:00.000Z',
      };

      final model = AdminTickerModel.fromJson(json);

      expect(model.id, 'comp-1');
      expect(model.ticker, 'BOVA11');
      expect(model.name, 'iShares Ibovespa Fundo de Índice');
      expect(model.assetClass, 'ETFs');
      expect(model.sector, 'Índices');
      expect(model.createdAt, isNotNull);
      expect(model.createdAt!.isUtc, isTrue);
      expect(model.updatedAt, isNotNull);
      expect(model.updatedAt!.isUtc, isTrue);
    });

    test('deve serializar para JSON mantendo campos esperados', () {
      final model = AdminTickerModel(
        id: 'comp-2',
        ticker: 'PETR4',
        name: 'Petróleo Brasileiro S.A.',
        assetClass: 'Ações',
        sector: 'Petróleo e Gás',
        createdAt: DateTime.utc(2026, 6, 13, 10, 0),
        updatedAt: DateTime.utc(2026, 6, 13, 12, 0),
      );

      final json = model.toJson();

      expect(json['id'], 'comp-2');
      expect(json['ticker'], 'PETR4');
      expect(json['name'], 'Petróleo Brasileiro S.A.');
      expect(json['assetClass'], 'Ações');
      expect(json['sector'], 'Petróleo e Gás');
      expect(json['createdAt'], isNotNull);
      expect(json['updatedAt'], isNotNull);
    });

    test('deve deserializar lista a partir de envelope { "data": [...] }', () {
      final envelope = {
        'data': [
          {
            'id': '1',
            'ticker': 'VALE3',
            'name': 'Vale S.A.',
            'assetClass': 'Ações',
          },
          {
            'id': '2',
            'ticker': 'HGLG11',
            'name': 'CSHG Logística FII',
            'assetClass': 'FIIs',
          },
        ],
      };

      final list = AdminTickerModel.fromJsonList(envelope);

      expect(list.length, 2);
      expect(list[0].ticker, 'VALE3');
      expect(list[1].ticker, 'HGLG11');
    });

    test('deve aplicar fallback Ações quando assetClass vier vazio ou nulo', () {
      final model = AdminTickerModel.fromJson({
        'id': '3',
        'ticker': 'ITUB4',
        'name': 'Itaú',
        'assetClass': '   ',
      });

      expect(model.assetClass, 'Ações');
    });
  });
}
