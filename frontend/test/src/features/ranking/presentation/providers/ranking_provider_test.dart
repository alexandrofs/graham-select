
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/ranking/presentation/providers/ranking_provider.dart';
import 'package:frontend/src/features/ranking/domain/usecases/get_ranking_usecase.dart';
import 'package:frontend/src/features/ranking/domain/entities/ranked_company.dart';
import 'package:frontend/src/features/ranking/domain/repositories/ranking_repository.dart';

// Manual Mock
class ManualMockGetRankingUseCase implements GetRankingUseCase {
  List<RankedCompany>? _response;
  Exception? _error;
  int callCount = 0;

  @override
  RankingRepository get repository => throw UnimplementedError();

  void mockSuccess(List<RankedCompany> companies) {
    _response = companies;
    _error = null;
  }

  void mockFailure(Exception error) {
    _error = error;
    _response = null;
  }

  @override
  Future<List<RankedCompany>> call() async {
    callCount++;
    // Simulate network delay
    await Future.delayed(Duration(milliseconds: 10));
    if (_error != null) throw _error!;
    return _response!;
  }
}

void main() {
  late RankingProvider provider;
  late ManualMockGetRankingUseCase mockGetRankingUseCase;

  setUp(() {
    mockGetRankingUseCase = ManualMockGetRankingUseCase();
    provider = RankingProvider(mockGetRankingUseCase);
  });

  group('RankingProvider', () {
    test('initial state should be idle', () {
      expect(provider.state, RankingState.idle);
      expect(provider.companies, isEmpty);
      expect(provider.errorMessage, isNull);
    });

    test('should change state to loading and then success when fetchRanking is successful', () async {
      // Arrange
      final tCompanies = [
        RankedCompany(
          symbol: 'AAPL',
          name: 'Apple Inc.',
          currentPrice: 150.0,
          intrinsicValue: 200.0,
          marginOfSafety: 0.25,
        )
      ];
      mockGetRankingUseCase.mockSuccess(tCompanies);

      // Act
      final future = provider.fetchRanking();

      // Assert loading state
      expect(provider.state, RankingState.loading);
      
      await future;

      // Assert success state
      expect(provider.state, RankingState.success);
      expect(provider.companies, tCompanies);
      expect(provider.errorMessage, isNull);
      expect(mockGetRankingUseCase.callCount, 1);
    });

    test('should change state to loading and then error when fetchRanking fails', () async {
      // Arrange
      final tError = Exception('Failed to fetch data');
      mockGetRankingUseCase.mockFailure(tError);

      // Act
      final future = provider.fetchRanking();
      
      expect(provider.state, RankingState.loading);

      await future;

      // Assert error state
      expect(provider.state, RankingState.error);
      expect(provider.companies, isEmpty);
      expect(provider.errorMessage, contains('Failed to fetch data'));
      expect(mockGetRankingUseCase.callCount, 1);
    });
  });
}
