import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';
import 'package:frontend/src/features/docs/presentation/pages/docs_screen.dart';
import 'package:go_router/go_router.dart';

class _FakeAuthRepository extends Fake implements AuthRepository {
  @override
  User? get currentUser => null;

  @override
  Stream<User?> get authStateChanges => const Stream.empty();

  @override
  Future<String?> getPersistedToken() async => null;
}

class _FakeApiClient extends Fake implements ApiClient {
  @override
  void setTokenProvider(TokenProvider provider) {}
}

class _FakeSecureStorage extends Fake implements FlutterSecureStorage {}

void main() {
  testWidgets('Visitante não autenticado acessa /docs sem redirecionar para /login', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      GrahamSelectApp(
        authRepository: _FakeAuthRepository(),
        apiClient: _FakeApiClient(),
        storage: _FakeSecureStorage(),
      ),
    );
    await tester.pumpAndSettle();

    final materialApp = tester.widget<MaterialApp>(find.byType(MaterialApp));
    final router = materialApp.routerConfig as GoRouter;

    router.go('/docs');
    await tester.pumpAndSettle();

    expect(find.byType(DocsScreen), findsOneWidget);
    expect(find.text('Documentação'), findsOneWidget);

    // Tapping back with an empty navigation stack should redirect safely to /login
    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();
    expect(find.text('Continuar com Google'), findsOneWidget);

    // Rotas autenticadas continuam exigindo login
    router.go('/dashboard');
    await tester.pumpAndSettle();
    expect(find.text('Continuar com Google'), findsOneWidget);
  });
}
