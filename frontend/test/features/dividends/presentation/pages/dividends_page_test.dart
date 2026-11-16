import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/dividends/domain/entities/dividend_announcement.dart';
import 'package:frontend/src/features/dividends/domain/entities/relevant_fact.dart';
import 'package:frontend/src/features/dividends/domain/repositories/dividend_repository.dart';
import 'package:frontend/src/features/dividends/presentation/pages/dividends_page.dart';
import 'package:frontend/src/features/dividends/presentation/providers/dividend_provider.dart';
import 'package:provider/provider.dart';

class MockDividendRepository implements DividendRepository {
  List<DividendAnnouncement> scheduleToReturn = [];
  List<RelevantFact> factsToReturn = [];
  bool isLoading = false;

  @override
  Future<List<DividendAnnouncement>> getDividendSchedule({
    String? ticker,
    DateTime? startDate,
    DateTime? endDate,
    String? status,
    bool onlyMyTickers = false,
  }) async {
    return scheduleToReturn;
  }

  @override
  Future<List<RelevantFact>> getRelevantFacts({
    String? ticker,
    int limit = 20,
    bool onlyMyTickers = false,
  }) async {
    return factsToReturn;
  }
}

void main() {
  late MockDividendRepository repository;

  setUp(() {
    repository = MockDividendRepository();
  });

  Widget createWidgetUnderTest() {
    return MaterialApp(
      home: ChangeNotifierProvider(
        create: (_) => DividendProvider(repository: repository),
        child: const DividendsPage(),
      ),
    );
  }

  testWidgets('Deve renderizar cabeçalho, card de previsão e lista de proventos', (tester) async {
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

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    expect(find.text('Agenda de Proventos & Fatos Relevantes'), findsOneWidget);
    expect(find.byKey(const Key('dividends_estimated_total_card')), findsOneWidget);
    expect(find.byKey(const Key('dividends_only_my_assets_toggle')), findsOneWidget);
    expect(find.byKey(const Key('dividends_refresh_button')), findsOneWidget);
    expect(find.byKey(const Key('dividends_schedule_list')), findsOneWidget);
    expect(find.text('PETR4'), findsOneWidget);
    expect(find.text('R\$ 125,00'), findsOneWidget);
  });

  testWidgets('Deve alternar para aba de fatos relevantes e renderizar lista de fatos', (tester) async {
    final now = DateTime.now();
    repository.factsToReturn = [
      RelevantFact(
        id: 'fact-1',
        ticker: 'VALE3',
        title: 'Programa de Recompra de Ações',
        category: 'FATO_RELEVANTE',
        publicationDate: now,
        sourceUrl: 'https://cvm.gov.br/doc/123',
      ),
    ];

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    // Clicar na aba Fatos Relevantes
    await tester.tap(find.text('Fatos Relevantes'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('relevant_facts_list')), findsOneWidget);
    expect(find.text('VALE3'), findsOneWidget);
    expect(find.text('Programa de Recompra de Ações'), findsOneWidget);
    expect(find.text('Ver documento oficial'), findsOneWidget);

    await tester.tap(find.text('Ver documento oficial'));
    await tester.pumpAndSettle();
    expect(find.text('VALE3 - Documento Oficial'), findsOneWidget);
    expect(find.text('https://cvm.gov.br/doc/123'), findsOneWidget);
    await tester.tap(find.text('Fechar'));
    await tester.pumpAndSettle();
  });

  testWidgets('Deve exibir empty state quando não houver proventos', (tester) async {
    repository.scheduleToReturn = [];

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('dividends_empty_state')), findsOneWidget);
    expect(find.text('Nenhum provento programado encontrado no momento.'), findsOneWidget);
  });
}
