import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/open_finance/domain/entities/open_finance_connection.dart';
import 'package:frontend/src/features/open_finance/domain/entities/open_finance_institution.dart';
import 'package:frontend/src/features/open_finance/domain/entities/open_finance_sync_result.dart';
import 'package:frontend/src/features/open_finance/domain/repositories/open_finance_repository.dart';
import 'package:frontend/src/features/open_finance/presentation/providers/open_finance_provider.dart';
import 'package:frontend/src/features/open_finance/presentation/widgets/open_finance_tab.dart';
import 'package:provider/provider.dart';

class MockOpenFinanceRepository implements OpenFinanceRepository {
  List<OpenFinanceConnection> connections = [];
  List<OpenFinanceInstitution> institutions = [];

  @override
  Future<List<OpenFinanceConnection>> getConnections() async => connections;

  @override
  Future<List<OpenFinanceInstitution>> getInstitutions() async => institutions;

  @override
  Future<Map<String, dynamic>> initiateConnection({
    required String institutionId,
    String? profileId,
    String? redirectUri,
  }) async => {
        'connectionId': 'conn-1',
        'consentUrl': 'https://auth.xp.com.br',
      };

  @override
  Future<OpenFinanceConnection> completeConsentCallback({
    required String connectionId,
    required String code,
    required String state,
    String? redirectUri,
  }) async =>
      const OpenFinanceConnection(
        id: 'conn-1',
        userId: 'user-1',
        institutionId: 'XP_INVESTIMENTOS',
        institutionName: 'XP Investimentos',
        status: 'ACTIVE',
      );

  @override
  Future<OpenFinanceSyncResult> syncConnection(String connectionId) async =>
      const OpenFinanceSyncResult(
        connectionId: 'conn-1',
        status: 'SUCCESS',
        positionsFetched: 1,
        transactionsFetched: 2,
        tradesCreated: 2,
        tradesDuplicated: 0,
        message: 'OK',
      );

  @override
  Future<void> revokeConnection(String connectionId) async {}
}

void main() {
  late MockOpenFinanceRepository repository;
  late OpenFinanceProvider provider;

  setUp(() {
    repository = MockOpenFinanceRepository();
    provider = OpenFinanceProvider(repository: repository);
  });

  Widget createWidgetUnderTest() {
    return MaterialApp(
      home: Scaffold(
        body: ChangeNotifierProvider<OpenFinanceProvider>.value(
          value: provider,
          child: const SingleChildScrollView(child: OpenFinanceTab()),
        ),
      ),
    );
  }

  testWidgets('Should render security header and connect button', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    expect(find.text('Sincronização Contínua via Open Finance'), findsOneWidget);
    expect(find.byKey(const Key('open_finance_connect_button')), findsOneWidget);
    expect(find.byKey(const Key('open_finance_empty_state')), findsOneWidget);
  });

  testWidgets('Should display connected institutions list and cards', (tester) async {
    repository.connections = [
      const OpenFinanceConnection(
        id: 'conn-123',
        userId: 'user-1',
        institutionId: 'XP_INVESTIMENTOS',
        institutionName: 'XP Investimentos',
        status: 'ACTIVE',
      ),
    ];

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('open_finance_connections_list')), findsOneWidget);
    expect(find.text('XP Investimentos'), findsOneWidget);
    expect(find.text('Ativo'), findsOneWidget);
    expect(find.byKey(const Key('open_finance_sync_button_conn-123')), findsOneWidget);
    expect(find.byKey(const Key('open_finance_disconnect_button_conn-123')), findsOneWidget);
  });

  testWidgets('Clicking connect button should open institution selector dialog', (tester) async {
    repository.institutions = [
      const OpenFinanceInstitution(
        id: 'XP_INVESTIMENTOS',
        name: 'XP Investimentos',
        logoUrl: 'http://logo',
        enabled: true,
        description: 'OpenID FAPI',
      ),
      const OpenFinanceInstitution(
        id: 'BTG_PACTUAL',
        name: 'BTG Pactual',
        logoUrl: 'http://logo',
        enabled: true,
        description: 'OpenID FAPI',
      ),
    ];

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('open_finance_connect_button')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('open_finance_institution_dialog')), findsOneWidget);
    expect(find.text('Selecionar Instituição'), findsOneWidget);
    expect(find.text('BTG Pactual'), findsOneWidget);
  });
}
