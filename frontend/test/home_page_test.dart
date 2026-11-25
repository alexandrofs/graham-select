import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:mockito/annotations.dart';
import 'package:frontend/main.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:firebase_auth/firebase_auth.dart';

@GenerateNiceMocks([
  MockSpec<AuthRepository>(),
  MockSpec<ApiClient>(),
  MockSpec<FlutterSecureStorage>(),
  MockSpec<User>(),
])
import 'home_page_test.mocks.dart';

void main() {
  late MockAuthRepository mockAuthRepository;
  late MockApiClient mockApiClient;
  late MockFlutterSecureStorage mockStorage;

  setUp(() {
    mockAuthRepository = MockAuthRepository();
    mockApiClient = MockApiClient();
    mockStorage = MockFlutterSecureStorage();

    // Simular estado DESLOGADO para testar a Landing Page (HomePage)
    // Sem isso, o GoRouter redireciona para /dashboard
    when(mockAuthRepository.currentUser).thenReturn(null);
    when(mockAuthRepository.getPersistedToken()).thenAnswer((_) async => null);
    // Simular mudanças de estado vazias
    when(mockAuthRepository.authStateChanges).thenAnswer((_) => const Stream.empty());
  });

  testWidgets('HomePage should display Kerno title and contact without legacy brand', (
    WidgetTester tester,
  ) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(
      GrahamSelectApp(
        authRepository: mockAuthRepository,
        apiClient: mockApiClient,
        storage: mockStorage,
      ),
    );
    await tester.pumpAndSettle(const Duration(milliseconds: 500));

    // Verify runtime MaterialApp title and Kerno branding without legacy brand
    final materialApp = tester.widget<MaterialApp>(find.byType(MaterialApp));
    expect(
      materialApp.title,
      equals('Kerno — O núcleo inteligente do seu dinheiro'),
    );
    expect(
      find.byWidgetPredicate(
        (widget) =>
            widget is Image &&
            widget.image is AssetImage &&
            (widget.image as AssetImage).assetName == 'assets/images/logo.png',
      ),
      findsOneWidget,
    );
    expect(find.text('Kerno'), findsAtLeast(1));
    expect(find.textContaining('Graham Select'), findsNothing);
    expect(
      find.text(
        'O Kerno coleta, filtra e classifica dados financeiros em segundos para entregar um ranking quantitativo e auditável da bolsa.',
      ),
      findsOneWidget,
    );
    expect(
      find.text(
        '“Uso o Kerno para filtrar ideias antes de escrever meus relatórios.”',
      ),
      findsOneWidget,
    );
    expect(find.text('Pronto para testar o Kerno?'), findsOneWidget);
    expect(
      find.text('© ${DateTime.now().year} Kerno. Todos os direitos reservados.'),
      findsOneWidget,
    );
    expect(find.text('kerno@afsdigital.com.br'), findsOneWidget);
    expect(find.textContaining('99999-9999'), findsNothing);

    // Verify that the hero copy preserves Benjamin Graham methodology
    expect(
      find.text('Invista com a metodologia de Benjamin Graham'),
      findsOneWidget,
    );

    // Verify that the buttons are present
    expect(find.text('Ver ranking agora'), findsOneWidget);
    expect(find.text('Entender metodologia'), findsOneWidget);
  });

  testWidgets('Navigation buttons should be tappable', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      GrahamSelectApp(
        authRepository: mockAuthRepository,
        apiClient: mockApiClient,
        storage: mockStorage,
      ),
    );
    await tester.pumpAndSettle();

    // Find the "Ver ranking agora" button
    final rankingButton = find.text('Ver ranking agora');
    expect(rankingButton, findsOneWidget);

    // Scroll if needed and tap
    await tester.ensureVisible(rankingButton);
    await tester.tap(rankingButton);
    await tester.pumpAndSettle();

    // Verify navigation occurred (login page should be visible because we are logged out)
    expect(find.text('Continuar com Google'), findsOneWidget);
  });
}
