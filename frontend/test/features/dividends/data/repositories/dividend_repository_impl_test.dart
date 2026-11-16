import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/dividends/data/datasources/dividend_remote_data_source.dart';
import 'package:frontend/src/features/dividends/data/models/dividend_announcement_model.dart';
import 'package:frontend/src/features/dividends/data/models/relevant_fact_model.dart';
import 'package:frontend/src/features/dividends/data/repositories/dividend_repository_impl.dart';

class FakeDividendRemoteDataSource implements DividendRemoteDataSource {
  List<DividendAnnouncementModel> mockSchedule = [];
  List<RelevantFactModel> mockFacts = [];

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);

  @override
  Future<List<DividendAnnouncementModel>> getDividendSchedule({
    String? ticker,
    DateTime? startDate,
    DateTime? endDate,
    String? status,
    bool onlyMyTickers = false,
  }) async {
    return mockSchedule;
  }

  @override
  Future<List<RelevantFactModel>> getRelevantFacts({
    String? ticker,
    int limit = 20,
    bool onlyMyTickers = false,
  }) async {
    return mockFacts;
  }
}

void main() {
  late FakeDividendRemoteDataSource dataSource;
  late DividendRepositoryImpl repository;

  setUp(() {
    dataSource = FakeDividendRemoteDataSource();
    repository = DividendRepositoryImpl(remoteDataSource: dataSource);
  });

  test('Deve delegar getDividendSchedule para remoteDataSource', () async {
    final now = DateTime.now();
    dataSource.mockSchedule = [
      DividendAnnouncementModel(
        id: 'div-1',
        ticker: 'PETR4',
        type: 'DIVIDENDO',
        rate: 1.5,
        comDate: now,
        source: 'CVM',
        status: 'APPROVED',
      ),
    ];

    final result = await repository.getDividendSchedule(ticker: 'PETR4');
    expect(result.length, 1);
    expect(result.first.ticker, 'PETR4');
  });

  test('Deve delegar getRelevantFacts para remoteDataSource', () async {
    final now = DateTime.now();
    dataSource.mockFacts = [
      RelevantFactModel(
        id: 'fact-1',
        ticker: 'VALE3',
        title: 'Fato Relevante',
        category: 'FATO_RELEVANTE',
        publicationDate: now,
      ),
    ];

    final result = await repository.getRelevantFacts(limit: 5);
    expect(result.length, 1);
    expect(result.first.ticker, 'VALE3');
  });
}
