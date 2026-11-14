import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/tags/data/models/portfolio_tag_model.dart';

void main() {
  group('PortfolioTagModel', () {
    test('fromJson aplica fallbacks resilientes quando campos opcionais estão ausentes', () {
      final model = PortfolioTagModel.fromJson({
        'id': 'tag-1',
        'name': 'Aposentadoria',
        'color': null,
      });

      expect(model.id, 'tag-1');
      expect(model.name, 'Aposentadoria');
      expect(model.color, '#6366F1');
      expect(model.tickers, isEmpty);
      expect(model.assetCount, 0);
      expect(model.createdAt, isNull);
      expect(model.updatedAt, isNull);
    });

    test('fromJson normaliza tickers e faz parse de assetCount e datas', () {
      final model = PortfolioTagModel.fromJson({
        'id': 'tag-2',
        'name': 'Renda Fixa',
        'color': '#10b981',
        'tickers': [' petr4 ', 'cdb banco x'],
        'createdAt': '2026-11-13T12:00:00Z',
        'updatedAt': '2026-11-13T13:30:00Z',
      });

      expect(model.color, '#10B981');
      expect(model.tickers, ['PETR4', 'CDB BANCO X']);
      expect(model.assetCount, 2);
      expect(model.createdAt, DateTime.utc(2026, 11, 13, 12, 0, 0));
      expect(model.updatedAt, DateTime.utc(2026, 11, 13, 13, 30, 0));
    });

    test('fromJson usa fallback #6366F1 quando cor é inválida', () {
      final model = PortfolioTagModel.fromJson({
        'id': 'tag-3',
        'name': 'Dividendos',
        'color': 'invalid',
        'assetCount': 3,
      });

      expect(model.color, '#6366F1');
      expect(model.assetCount, 3);
    });

    test('fromJson interpreta LocalDateTime sem sufixo Z como UTC e normaliza offset', () {
      final model = PortfolioTagModel.fromJson({
        'id': 'tag-4',
        'name': 'Internacional',
        'createdAt': '2026-11-13T12:00:00',
        'updatedAt': '2026-11-13T09:00:00-03:00',
      });

      expect(model.createdAt, DateTime.utc(2026, 11, 13, 12, 0, 0));
      expect(model.createdAt?.isUtc, isTrue);
      expect(model.updatedAt, DateTime.utc(2026, 11, 13, 12, 0, 0));
      expect(model.updatedAt?.isUtc, isTrue);
    });

    test('fromJson lança FormatException quando id ou name está ausente/vazio', () {
      expect(
        () => PortfolioTagModel.fromJson({'id': '', 'name': 'Valida'}),
        throwsFormatException,
      );
      expect(
        () => PortfolioTagModel.fromJson({'id': 'tag-5', 'name': '   '}),
        throwsFormatException,
      );
    });
  });
}
