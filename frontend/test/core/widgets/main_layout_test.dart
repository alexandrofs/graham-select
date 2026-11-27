import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/config/feature_flags.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/core/widgets/main_layout.dart';
import 'package:frontend/src/core/widgets/skip_to_content_link.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

class _StubProfileRepository implements ProfileRepository {
  final String role;

  _StubProfileRepository({this.role = 'USER'});

  @override
  Future<UserProfile> getProfile() async => UserProfile(
        id: 1,
        email: 'user@kerno.afsdigital.com.br',
        fullName: 'User',
        tier: 'FREE',
        role: role,
      );

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Widget _buildRouterApp({
  required ProfileRepository repo,
  double textScale = 1.0,
  Widget? dashboardChild,
}) {
  final router = GoRouter(
    initialLocation: '/dashboard',
    routes: [
      ShellRoute(
        builder: (context, state, child) => MediaQuery(
          data: MediaQuery.of(context).copyWith(
            textScaler: TextScaler.linear(textScale),
          ),
          child: MainLayout(child: child),
        ),
        routes: [
          GoRoute(
            path: '/dashboard',
            builder: (_, _) => dashboardChild ?? const SizedBox(),
          ),
          GoRoute(
            path: '/gps-aporte',
            builder: (_, _) => const SizedBox(),
          ),
          GoRoute(
            path: '/kerno-ai',
            builder: (_, _) => const SizedBox(),
          ),
          GoRoute(
            path: '/allocation-strategy',
            builder: (_, _) => const SizedBox(),
          ),
          GoRoute(
            path: '/expenses',
            builder: (_, _) => const SizedBox(),
          ),
        ],
      ),
    ],
  );

  return ChangeNotifierProvider<ProfileProvider>(
    key: ValueKey(repo),
    create: (_) => ProfileProvider(repo),
    child: MaterialApp.router(
      theme: AppTheme.lightTheme,
      routerConfig: router,
    ),
  );
}

void main() {
  tearDown(FeatureFlags.resetForTest);

  testWidgets('MainLayout exibe logotipo e marca Kerno no menu lateral', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(_buildRouterApp(repo: _StubProfileRepository()));
    await tester.pumpAndSettle();

    expect(find.text('Kerno'), findsOneWidget);
    expect(
      find.byWidgetPredicate(
        (widget) =>
            widget is Image &&
            widget.image is AssetImage &&
            (widget.image as AssetImage).assetName == 'assets/images/logo.png',
      ),
      findsOneWidget,
    );
    expect(find.textContaining('Graham'), findsNothing);
    expect(find.byKey(const Key('nav_dividends_button')), findsOneWidget);
    expect(find.byKey(const Key('nav_tax_button')), findsOneWidget);
  });

  testWidgets(
      'Mobile (< 600dp) com ENABLE_EXPENSES_MODULE=false exibe BottomNavigationBar com 4 abas',
      (tester) async {
    FeatureFlags.setEnableExpensesModuleForTest(false);
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(_buildRouterApp(repo: _StubProfileRepository()));
    await tester.pumpAndSettle();

    final bottomNav = tester.widget<BottomNavigationBar>(
      find.byType(BottomNavigationBar),
    );
    expect(bottomNav.items.length, 4);
    expect(
      bottomNav.items.map((item) => item.label).toList(),
      ['Carteira', 'GPS de Aporte', 'Kerno AI', 'Diagrama'],
    );
    expect(find.byType(NavigationRail), findsNothing);
  });

  testWidgets(
      'Mobile (< 600dp) com ENABLE_EXPENSES_MODULE=true exibe 5 abas incluindo Gastos',
      (tester) async {
    FeatureFlags.setEnableExpensesModuleForTest(true);
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(_buildRouterApp(repo: _StubProfileRepository()));
    await tester.pumpAndSettle();

    final bottomNav = tester.widget<BottomNavigationBar>(
      find.byType(BottomNavigationBar),
    );
    expect(bottomNav.items.length, 5);
    expect(bottomNav.items.last.label, 'Gastos');
  });

  testWidgets(
      'Tablet (600dp <= w < 1024dp) renderiza NavigationRail de 72dp sem BottomNavigationBar',
      (tester) async {
    tester.view.physicalSize = const Size(800, 1024);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(_buildRouterApp(repo: _StubProfileRepository()));
    await tester.pumpAndSettle();

    final rail = tester.widget<NavigationRail>(find.byType(NavigationRail));
    expect(rail.minWidth, 72);
    expect(find.byType(BottomNavigationBar), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
      'Desktop (>= 1024dp) oculta grupo Admin para USER e exibe para ADMIN',
      (tester) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      _buildRouterApp(repo: _StubProfileRepository(role: 'USER')),
    );
    await tester.pumpAndSettle();
    expect(find.text('ADMINISTRAÇÃO'), findsNothing);

    await tester.pumpWidget(
      _buildRouterApp(repo: _StubProfileRepository(role: 'ADMIN')),
    );
    await tester.pumpAndSettle();
    expect(find.text('ADMINISTRAÇÃO'), findsOneWidget);
  });

  testWidgets(
      'Desktop (>= 1024dp) foca SkipToContentLink no primeiro Tab e salta para o conteúdo',
      (tester) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final router = GoRouter(
      initialLocation: '/dashboard',
      routes: [
        GoRoute(
          path: '/dashboard',
          builder: (_, _) => MainLayout(
            child: ElevatedButton(
              key: const Key('main_content_button'),
              onPressed: () {},
              child: const Text('Ação Principal'),
            ),
          ),
        ),
      ],
    );

    await tester.pumpWidget(
      ChangeNotifierProvider<ProfileProvider>(
        create: (_) => ProfileProvider(_StubProfileRepository()),
        child: MaterialApp.router(
          theme: AppTheme.lightTheme,
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    final sidebarTopBefore = tester.getTopLeft(find.text('Kerno')).dy;

    await tester.sendKeyEvent(LogicalKeyboardKey.tab);
    await tester.pumpAndSettle();

    expect(find.byType(SkipToContentLink), findsOneWidget);
    expect(find.text('Pular para o conteúdo principal'), findsOneWidget);
    expect(tester.getTopLeft(find.text('Kerno')).dy, equals(sidebarTopBefore));

    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.pumpAndSettle();

    final contentFocus = Focus.of(
      tester.element(find.byKey(const Key('main_content_button'))),
    );
    expect(contentFocus.hasFocus, isTrue);
  });

  testWidgets(
      'Escala tipográfica alta (360dp, textScaler 2.0) ativa LargeContentPresenter sem overflow',
      (tester) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      _buildRouterApp(
        repo: _StubProfileRepository(),
        textScale: 2.0,
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byKey(const Key('topbar_profile_button')), findsOneWidget);
    final bottomNav = tester.widget<BottomNavigationBar>(
      find.byType(BottomNavigationBar),
    );
    expect(bottomNav.showSelectedLabels, isFalse);
    expect(bottomNav.showUnselectedLabels, isFalse);

    await tester.longPress(find.byIcon(Icons.explore_outlined));
    await tester.pumpAndSettle();
    expect(find.text('GPS de Aporte'), findsWidgets);
  });
}
