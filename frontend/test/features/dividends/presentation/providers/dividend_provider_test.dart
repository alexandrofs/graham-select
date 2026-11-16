import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/dividends/domain/entities/dividend_announcement.dart';
import 'package:frontend/src/features/dividends/domain/entities/relevant_fact.dart';
import 'package:frontend/src/features/dividends/domain/repositories/dividend_repository.dart';
import 'package:frontend/src/features/dividends/presentation/providers/dividend_provider.dart';

class FakeDividendRepository implements DividendRepository {
  List<DividendAnnouncement> scheduleToReturn = [];
  List<RelevantFact> factsToReturn = [];
  bool shouldThrow = false;
  bool lastOnlyMyTickers = false;
  String? lastTicker;

  @override
  Future<List<DividendAnnouncement>> getDividendSchedule({
    String? ticker,
    DateTime? startDate,
    DateTime? endDate,
    String? status,
    bool onlyMyTickers = false,
  }) async {
    if (shouldThrow) throw Exception('Network error');
    lastOnlyMyTickers = onlyMyTickers;
    lastTicker = ticker;
    return scheduleToReturn;
  }

  @override
  Future<List<RelevantFact>> getRelevantFacts({
    String? ticker,
    int limit = 20,
    bool onlyMyTickers = false,
  }) async {
    if (shouldThrow) throw Exception('Network error');
    return factsToReturn;
  }
}

void main() {
  late FakeDividendRepository repository;
  late DividendProvider provider;

  setUp(() {
    repository = FakeDividendRepository();
    provider = DividendProvider(repository: repository);
  });

  group('DividendProvider Tests', () {
    test('Deve inicializar com valores padrão', () {
      expect(provider.isLoading, false);
      expect(provider.errorMessage, isNull);
      expect(provider.schedule, isEmpty);
      expect(provider.facts, isEmpty);
      expect(provider.onlyMyAssets, false);
      expect(provider.selectedTab, 0);
    });

    test('Deve carregar agenda de proventos e fatos relevantes com sucesso', () async {
      final now = DateTime.now();
      repository.scheduleToReturn = [
        DividendAnnouncement(
          id: 'div-1',
          ticker: 'PETR4',
          type: 'DIVIDENDO',
          rate: 1.25,
          netValue: 1.25,
          comDate: now,
          paymentDate: now.add(const Duration(days: 10)),
          source: 'BRAPI',
          status: 'APPROVED',
          userShares: 100,
          estimatedTotal: 125.0,
          estimatedNetTotal: 125.0,
        ),
      ];
      repository.factsToReturn = [
        RelevantFact(
          id: 'fact-1',
          ticker: 'VALE3',
          title: 'Programa de Recompra',
          category: 'FATO_RELEVANTE',
          publicationDate: now,
        ),
      ];

      await provider.loadData();

      expect(provider.isLoading, false);
      expect(provider.errorMessage, isNull);
      expect(provider.schedule.length, 1);
      expect(provider.facts.length, 1);
      expect(provider.totalEstimatedNext30Days, 125.0);
    });

    test('Deve alternar filtro de Meus Ativos e recarregar dados', () async {
      provider.toggleOnlyMyAssets();
      expect(provider.onlyMyAssets, true);
      expect(repository.lastOnlyMyTickers, true);
    });

    test('Deve tratar erro de carregamento atualizando errorMessage', () async {
      repository.shouldThrow = true;
      await provider.loadData();

      expect(provider.isLoading, false);
      expect(provider.errorMessage, contains('Network error'));
    });
  });
}
