import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/widgets/main_layout.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

class _StubProfileRepository implements ProfileRepository {
  final Future<UserProfile> Function() onGetProfile;

  _StubProfileRepository(this.onGetProfile);

  @override
  Future<UserProfile> getProfile() => onGetProfile();

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Widget _buildApp(ProfileRepository repo, {double textScale = 1.0}) {
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
            builder: (_, _) => const SizedBox(),
          ),
        ],
      ),
    ],
  );

  return ChangeNotifierProvider<ProfileProvider>(
    create: (_) => ProfileProvider(repo),
    child: MaterialApp.router(routerConfig: router),
  );
}

void main() {
  testWidgets(
      'MainLayout carrega perfil automaticamente e exibe selo Founder na TopBar',
      (tester) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repo = _StubProfileRepository(
      () async => UserProfile(
        id: 1,
        email: 'founder@kerno.afsdigital.com.br',
        fullName: 'Founder User',
        tier: 'PREMIUM',
        commercialPlan: 'FOUNDER_LIFETIME',
        termsAcceptedAt: DateTime.utc(2026, 11, 24, 12, 0),
      ),
    );

    await tester.pumpWidget(_buildApp(repo));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('commercial_plan_badge')), findsOneWidget);
    expect(find.text('Founder'), findsOneWidget);
  });

  testWidgets('MainLayout exibe Trial Pro: 90 dias restantes na TopBar', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repo = _StubProfileRepository(
      () async => UserProfile(
        id: 2,
        email: 'trial@kerno.afsdigital.com.br',
        fullName: 'Trial User',
        tier: 'TRIAL',
        commercialPlan: 'PRO_TRIAL',
        daysRemaining: 90,
      ),
    );

    await tester.pumpWidget(_buildApp(repo, textScale: 2.0));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Trial Pro: 90 dias restantes'), findsOneWidget);
  });

  testWidgets('MainLayout omite badge silenciosamente se fetchProfile falhar', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repo = _StubProfileRepository(
      () async => throw Exception('Falha de rede'),
    );

    await tester.pumpWidget(_buildApp(repo));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byKey(const Key('commercial_plan_badge')), findsNothing);
  });

  testWidgets(
      'MainLayout renderiza sem overflow em viewport estreita (360x640) com ADMIN + PRO_TRIAL + TextScaler 2.0',
      (tester) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repo = _StubProfileRepository(
      () async => UserProfile(
        id: 3,
        email: 'admin@kerno.afsdigital.com.br',
        fullName: 'Admin Trial',
        tier: 'TRIAL',
        commercialPlan: 'PRO_TRIAL',
        daysRemaining: 90,
        role: 'ADMIN',
      ),
    );

    await tester.pumpWidget(_buildApp(repo, textScale: 2.0));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byKey(const Key('commercial_plan_badge')), findsOneWidget);
    expect(find.byKey(const Key('admin_dashboard_nav_button')), findsOneWidget);
  });
}
