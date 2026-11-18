import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/open_finance/domain/entities/open_finance_connection.dart';
import 'package:frontend/src/features/open_finance/domain/entities/open_finance_institution.dart';
import 'package:frontend/src/features/open_finance/domain/entities/open_finance_sync_result.dart';
import 'package:frontend/src/features/open_finance/domain/repositories/open_finance_repository.dart';
import 'package:frontend/src/features/open_finance/presentation/providers/open_finance_provider.dart';

class FakeOpenFinanceRepository implements OpenFinanceRepository {
  List<OpenFinanceConnection> mockConnections = [];
  List<OpenFinanceInstitution> mockInstitutions = [];
  bool syncCalled = false;
  bool revokeCalled = false;

  @override
  Future<List<OpenFinanceConnection>> getConnections() async {
    return mockConnections;
  }

  @override
  Future<List<OpenFinanceInstitution>> getInstitutions() async {
    return mockInstitutions;
  }

  @override
  Future<Map<String, dynamic>> initiateConnection({
    required String institutionId,
    String? profileId,
    String? redirectUri,
  }) async {
    return {
      'connectionId': 'conn-1',
      'consentUrl': 'https://auth.xp.com.br/consent?state=123',
      'state': 'state-123',
    };
  }

  @override
  Future<OpenFinanceConnection> completeConsentCallback({
    required String connectionId,
    required String code,
    required String state,
    String? redirectUri,
  }) async {
    return const OpenFinanceConnection(
      id: 'conn-1',
      userId: 'user-1',
      institutionId: 'XP_INVESTIMENTOS',
      institutionName: 'XP Investimentos',
      status: 'ACTIVE',
    );
  }

  @override
  Future<OpenFinanceSyncResult> syncConnection(String connectionId) async {
    syncCalled = true;
    return const OpenFinanceSyncResult(
      connectionId: 'conn-1',
      status: 'SUCCESS',
      positionsFetched: 2,
      transactionsFetched: 5,
      tradesCreated: 3,
      tradesDuplicated: 2,
      message: 'OK',
    );
  }

  @override
  Future<void> revokeConnection(String connectionId) async {
    revokeCalled = true;
  }
}

void main() {
  late FakeOpenFinanceRepository fakeRepository;
  late OpenFinanceProvider provider;

  setUp(() {
    fakeRepository = FakeOpenFinanceRepository();
    provider = OpenFinanceProvider(repository: fakeRepository);
  });

  test('fetchConnections should populate connections and set state to loaded', () async {
    fakeRepository.mockConnections = [
      const OpenFinanceConnection(
        id: 'conn-1',
        userId: 'user-1',
        institutionId: 'XP_INVESTIMENTOS',
        institutionName: 'XP Investimentos',
        status: 'ACTIVE',
      ),
    ];

    expect(provider.state, OpenFinanceState.initial);
    await provider.fetchConnections();

    expect(provider.state, OpenFinanceState.loaded);
    expect(provider.connections.length, 1);
    expect(provider.connections.first.institutionId, 'XP_INVESTIMENTOS');
  });

  test('fetchInstitutions should populate institutions list', () async {
    fakeRepository.mockInstitutions = [
      const OpenFinanceInstitution(
        id: 'XP_INVESTIMENTOS',
        name: 'XP Investimentos',
        logoUrl: 'http://logo',
        enabled: true,
        description: 'OpenID FAPI',
      ),
    ];

    await provider.fetchInstitutions();

    expect(provider.institutions.length, 1);
    expect(provider.institutions.first.name, 'XP Investimentos');
  });

  test('initiateConnection should return consentUrl and refresh connections', () async {
    final consentUrl = await provider.initiateConnection('XP_INVESTIMENTOS');

    expect(consentUrl, 'https://auth.xp.com.br/consent?state=123');
  });

  test('syncConnection should execute sync and update lastSyncResult', () async {
    final result = await provider.syncConnection('conn-1');

    expect(result, isNotNull);
    expect(result?.tradesCreated, 3);
    expect(fakeRepository.syncCalled, isTrue);
    expect(provider.lastSyncResult?.tradesCreated, 3);
  });

  test('revokeConnection should invoke repository revoke', () async {
    final success = await provider.revokeConnection('conn-1');

    expect(success, isTrue);
    expect(fakeRepository.revokeCalled, isTrue);
  });
}
