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

  testWidgets('HomePage should display Graham Select title', (
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

    // Verify that the title is displayed
    // Como agora temos o título no Desktop e Mobile, podem aparecer 1 ou 2 widgets dependendo do tamanho da tela do teste
    expect(find.text('Graham Select'), findsAtLeast(1));

    // Verify that the hero copy is displayed
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
    expect(find.text('Entrar com Google'), findsOneWidget);
  });
}
