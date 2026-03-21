import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';
import 'package:frontend/src/features/auth/presentation/login_screen.dart';

@GenerateNiceMocks([
  MockSpec<AuthRepository>(),
  MockSpec<UserCredential>(),
])
import 'login_screen_test.mocks.dart';

void main() {
  late MockAuthRepository mockAuthRepository;

  setUp(() {
    mockAuthRepository = MockAuthRepository();
  });

  Widget createWidgetUnderTest() {
    final router = GoRouter(
      initialLocation: '/login',
      routes: [
        GoRoute(
          path: '/login',
          builder: (context, state) => Provider<AuthRepository>.value(
            value: mockAuthRepository,
            child: const LoginScreen(),
          ),
        ),
        GoRoute(
          path: '/',
          builder: (context, state) => const Scaffold(body: Text('Home')),
        ),
      ],
    );

    return MaterialApp.router(
      routerConfig: router,
    );
  }

  // ==========================================
  // Cenário 1: Renderização inicial
  // ==========================================

  testWidgets('Deve exibir o título "Graham Select"', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());

    expect(find.text('Graham Select'), findsOneWidget);
  });

  testWidgets('Deve exibir o subtítulo descritivo', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());

    expect(
      find.text('O cérebro insubstituível do seu patrimônio familiar.'),
      findsOneWidget,
    );
  });

  // ==========================================
  // Cenário 2: Botão CTA presente
  // ==========================================

  testWidgets('Deve exibir botão "Entrar com Google"', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());

    expect(find.text('Entrar com Google'), findsOneWidget);
    expect(find.byType(ElevatedButton), findsOneWidget);
  });

  // ==========================================
  // Cenário 3: Texto de termos
  // ==========================================

  testWidgets('Deve exibir texto de termos de uso', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());

    expect(
      find.textContaining('Termos de Uso'),
      findsOneWidget,
    );
  });

  // ==========================================
  // Cenário 4: Tap no botão chama signInWithGoogle
  // ==========================================

  testWidgets('Tap no botão deve chamar signInWithGoogle()', (tester) async {
    final mockCredential = MockUserCredential();
    when(mockAuthRepository.signInWithGoogle())
        .thenAnswer((_) async => mockCredential);

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.tap(find.text('Entrar com Google'));
    await tester.pumpAndSettle();

    verify(mockAuthRepository.signInWithGoogle()).called(1);
  });

  // ==========================================
  // Cenário 5: Loading spinner durante autenticação
  // ==========================================

  testWidgets('Deve mostrar CircularProgressIndicator durante loading', (tester) async {
    // Simula um signIn que nunca completa (usando Completer)
    final completer = Completer<UserCredential>();
    when(mockAuthRepository.signInWithGoogle())
        .thenAnswer((_) => completer.future);

    await tester.pumpWidget(createWidgetUnderTest());

    // Verificar que não está em loading inicialmente
    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(find.text('Entrar com Google'), findsOneWidget);

    // Toca no botão
    await tester.tap(find.text('Entrar com Google'));
    // pump sem settle para capturar o estado intermediário
    await tester.pump();

    // Agora deve estar em loading
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(find.text('Entrar com Google'), findsNothing);
    
    // Completa o futuro para não deixar lixo
    completer.complete(MockUserCredential());
  });

  // ==========================================
  // Cenário 6: SnackBar de erro quando login falha
  // ==========================================

  testWidgets('Deve mostrar SnackBar de erro quando login falha', (tester) async {
    when(mockAuthRepository.signInWithGoogle())
        .thenThrow(Exception('Erro de rede'));

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.tap(find.text('Entrar com Google'));
    await tester.pumpAndSettle();

    expect(find.byType(SnackBar), findsOneWidget);
    expect(find.textContaining('Falha ao entrar com Google'), findsOneWidget);
  });

  // ==========================================
  // Cenário 7: Login com sucesso (retorna UserCredential não-nulo)
  // ==========================================

  testWidgets('Login com sucesso deve chamar signInWithGoogle sem erro', (tester) async {
    final mockCredential = MockUserCredential();
    when(mockAuthRepository.signInWithGoogle())
        .thenAnswer((_) async => mockCredential);

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.tap(find.text('Entrar com Google'));
    await tester.pumpAndSettle();

    verify(mockAuthRepository.signInWithGoogle()).called(1);
    // Após login bem-sucedido, o botão deve voltar (não mais loading)
    // e nenhum SnackBar de erro deve aparecer
    expect(find.byType(SnackBar), findsNothing);
  });
}
