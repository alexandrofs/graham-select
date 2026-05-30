import 'package:flutter_test/flutter_test.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';
import 'package:frontend/src/features/portfolio/data/datasources/portfolio_remote_data_source.dart';
import 'package:frontend/src/features/portfolio/data/models/custody_position_model.dart';
import 'package:frontend/src/features/portfolio/data/models/monthly_evolution_model.dart';
import 'package:frontend/src/features/portfolio/data/repositories/portfolio_repository_impl.dart';
import 'package:http/http.dart' as http;

class MockAuthRepository implements AuthRepository {
  String? persistedToken = 'token-123';

  @override
  Future<String?> getPersistedToken() async => persistedToken;

  Future<bool> hasPersistedToken() async => persistedToken != null;

  Future<void> persistToken(String token) async {}

  Future<void> clearPersistedToken() async {}

  @override
  Stream<User?> get authStateChanges => throw UnimplementedError();

  @override
  User? get currentUser => null;

  @override
  Future<UserCredential?> signInWithGoogle() async => null;

  @override
  Future<void> signOut() async {}
}

class FakeHttpClient extends http.BaseClient {
  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    throw UnimplementedError();
  }
}

class MockPortfolioRemoteDataSource extends PortfolioRemoteDataSource {
  ({List<CustodyPositionModel> positions, DateTime? metaPriceUpdatedAt})? responseToReturn;
  PortfolioEvolutionModel? evolutionResponseToReturn;
  String? capturedToken;

  MockPortfolioRemoteDataSource() : super(client: FakeHttpClient());

  @override
  Future<({List<CustodyPositionModel> positions, DateTime? metaPriceUpdatedAt})>
      getCustodyPositions({String? token}) async {
    capturedToken = token;
    if (responseToReturn != null) {
      return responseToReturn!;
    }
    throw UnimplementedError();
  }

  @override
  Future<PortfolioEvolutionModel> getPortfolioEvolution({String? token}) async {
    capturedToken = token;
    if (evolutionResponseToReturn != null) {
      return evolutionResponseToReturn!;
    }
    throw UnimplementedError();
  }
}

void main() {
  group('PortfolioRepositoryImpl', () {
    late MockAuthRepository mockAuthRepository;
    late MockPortfolioRemoteDataSource mockRemoteDataSource;
    late PortfolioRepositoryImpl repository;

    setUp(() {
      mockAuthRepository = MockAuthRepository();
      mockRemoteDataSource = MockPortfolioRemoteDataSource();
      repository = PortfolioRepositoryImpl(mockRemoteDataSource, mockAuthRepository);
    });

    test('deve obter o token do AuthRepository e repassar ao RemoteDataSource', () async {
      final tDate = DateTime.utc(2026, 5, 30, 18, 0, 0);
      final tResponse = (
        positions: [
          const CustodyPositionModel(
            ticker: 'PETR4',
            quantity: 10.0,
            averagePrice: 30.0,
            currentPrice: 35.0,
            marketValue: 350.0,
            gainLossPercentage: 16.67,
            priceSource: 'LIVE',
          )
        ],
        metaPriceUpdatedAt: tDate,
      );

      mockRemoteDataSource.responseToReturn = tResponse;
      mockAuthRepository.persistedToken = 'token-123';

      final result = await repository.getCustodyPositions();

      expect(mockRemoteDataSource.capturedToken, equals('token-123'));
      expect(result.positions.length, equals(1));
      expect(result.positions[0].ticker, equals('PETR4'));
      expect(result.metaPriceUpdatedAt, equals(tDate));
    });
  });
}
