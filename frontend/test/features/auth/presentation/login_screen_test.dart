import 'dart:async';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';
import 'package:frontend/src/features/auth/presentation/login_screen.dart';
import 'package:go_router/go_router.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';

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

  Widget createWidgetUnderTest({
    Future<bool> Function(Uri uri)? onLaunchUrl,
  }) {
    final router = GoRouter(
      initialLocation: '/login',
      routes: [
        GoRoute(
          path: '/login',
          builder: (context, state) => Provider<AuthRepository>.value(
            value: mockAuthRepository,
            child: LoginScreen(onLaunchUrl: onLaunchUrl),
          ),
        ),
        GoRoute(
          path: '/dashboard',
          builder: (context, state) => const Scaffold(body: Text('Dashboard')),
        ),
      ],
    );

    return MaterialApp.router(
      routerConfig: router,
    );
  }

  testWidgets('Deve exibir o título "Kerno" e o logotipo oficial', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());

    expect(find.text('Kerno'), findsOneWidget);
    expect(find.textContaining('Graham Select'), findsNothing);
    expect(
      find.byWidgetPredicate(
        (widget) =>
            widget is Image &&
            widget.image is AssetImage &&
            (widget.image as AssetImage).assetName == 'assets/images/logo.png',
      ),
      findsOneWidget,
    );
  });

  testWidgets('Deve exibir o subtítulo descritivo', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());

    expect(
      find.text('O núcleo inteligente do seu dinheiro.'),
      findsOneWidget,
    );
  });

  testWidgets('Deve exibir botão "Continuar com Google"', (tester) async {
    await tester.pumpWidget(createWidgetUnderTest());

    expect(find.text('Continuar com Google'), findsOneWidget);
    expect(find.byType(ElevatedButton), findsOneWidget);
  });

  testWidgets('Deve exibir links clicáveis para Termos de Uso e Política de Privacidade',
      (tester) async {
    final launchedUris = <Uri>[];
    await tester.pumpWidget(
      createWidgetUnderTest(
        onLaunchUrl: (uri) async {
          launchedUris.add(uri);
          return true;
        },
      ),
    );

    expect(find.byKey(const Key('login_terms_link')), findsOneWidget);
    expect(find.byKey(const Key('login_privacy_link')), findsOneWidget);

    await tester.tap(find.byKey(const Key('login_terms_link')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('login_privacy_link')));
    await tester.pumpAndSettle();

    expect(launchedUris.map((u) => u.path), ['/terms', '/privacy']);
    expect(find.byType(SnackBar), findsNothing);
  });

  testWidgets('Deve exibir SnackBar informativo quando launchUrl falhar sem bloquear login',
      (tester) async {
    await tester.pumpWidget(
      createWidgetUnderTest(
        onLaunchUrl: (_) async => false,
      ),
    );

    await tester.tap(find.byKey(const Key('login_terms_link')));
    await tester.pumpAndSettle();

    expect(find.byType(SnackBar), findsOneWidget);
    expect(find.text('Continuar com Google'), findsOneWidget);
  });

  testWidgets('Deve exibir SnackBar informativo quando onLaunchUrl lançar exceção',
      (tester) async {
    await tester.pumpWidget(
      createWidgetUnderTest(
        onLaunchUrl: (_) async => throw Exception('Erro ao abrir URL'),
      ),
    );

    await tester.tap(find.byKey(const Key('login_privacy_link')));
    await tester.pumpAndSettle();

    expect(find.byType(SnackBar), findsOneWidget);
    expect(find.text('Continuar com Google'), findsOneWidget);
  });

  testWidgets('Tap no botão deve chamar signInWithGoogle() e navegar para /dashboard',
      (tester) async {
    final mockCredential = MockUserCredential();
    when(mockAuthRepository.signInWithGoogle())
        .thenAnswer((_) async => mockCredential);

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.tap(find.text('Continuar com Google'));
    await tester.pumpAndSettle();

    verify(mockAuthRepository.signInWithGoogle()).called(1);
    expect(find.text('Dashboard'), findsOneWidget);
    expect(find.byType(SnackBar), findsNothing);
  });

  testWidgets('Deve mostrar CircularProgressIndicator durante loading', (tester) async {
    final completer = Completer<UserCredential>();
    when(mockAuthRepository.signInWithGoogle())
        .thenAnswer((_) => completer.future);

    await tester.pumpWidget(createWidgetUnderTest());

    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(find.text('Continuar com Google'), findsOneWidget);

    await tester.tap(find.text('Continuar com Google'));
    await tester.pump();

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(find.text('Continuar com Google'), findsNothing);

    completer.complete(MockUserCredential());
    await tester.pumpAndSettle();
  });

  testWidgets('Deve mostrar SnackBar de erro quando login falha', (tester) async {
    when(mockAuthRepository.signInWithGoogle())
        .thenThrow(Exception('Erro de rede'));

    await tester.pumpWidget(createWidgetUnderTest());
    await tester.tap(find.text('Continuar com Google'));
    await tester.pumpAndSettle();

    expect(find.byType(SnackBar), findsOneWidget);
    expect(find.textContaining('Falha ao entrar com Google'), findsOneWidget);
  });

  testWidgets(
      'Deve renderizar sem overflow em viewport curto (400x340) com TextScaler 2.0',
      (tester) async {
    tester.view.physicalSize = const Size(400, 340);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final router = GoRouter(
      initialLocation: '/login',
      routes: [
        GoRoute(
          path: '/login',
          builder: (context, state) => MediaQuery(
            data: MediaQuery.of(context).copyWith(
              textScaler: const TextScaler.linear(2.0),
            ),
            child: Provider<AuthRepository>.value(
              value: mockAuthRepository,
              child: const LoginScreen(),
            ),
          ),
        ),
      ],
    );

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Kerno'), findsOneWidget);
    expect(find.text('Continuar com Google'), findsOneWidget);
    expect(find.text('Termos de Uso'), findsOneWidget);
    expect(find.text('Política de Privacidade'), findsOneWidget);
  });
}
