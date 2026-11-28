import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';
import 'package:frontend/src/core/config/feature_flags.dart';
import 'package:frontend/src/core/security/biometric_auth_service.dart';
import 'package:frontend/src/core/security/biometric_lock_gate.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/ai_analysis/presentation/pages/kerno_ai_page.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_macro_class.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_preferences.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_template.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/repositories/cerrado_diagram_repository.dart';
import 'package:frontend/src/features/cerrado_diagram/presentation/pages/cerrado_diagram_page.dart';
import 'package:frontend/src/features/cerrado_diagram/presentation/providers/cerrado_diagram_provider.dart';
import 'package:frontend/src/features/expenses/presentation/pages/expenses_page.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:frontend/src/features/tags/domain/repositories/tag_repository.dart';
import 'package:frontend/src/features/tags/presentation/providers/tag_provider.dart';
import 'package:provider/provider.dart';

class _FakeUser extends Fake implements User {
  @override
  String get uid => 'user-123';
}

class _AuthenticatedFakeAuthRepository extends Fake implements AuthRepository {
  final _user = _FakeUser();

  @override
  User? get currentUser => _user;

  @override
  Stream<User?> get authStateChanges => const Stream.empty();

  @override
  Future<String?> getPersistedToken() async => 'fake-jwt-token';

  @override
  Future<void> signOut() async {}
}

class _StubProfileRepository implements ProfileRepository {
  @override
  Future<UserProfile> getProfile() async => UserProfile(
        id: 1,
        email: 'user@kerno.afsdigital.com.br',
        fullName: 'User',
        tier: 'FREE',
        role: 'USER',
      );

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _StubTagRepository extends Fake implements TagRepository {}

class _StubCerradoDiagramRepository implements CerradoDiagramRepository {
  @override
  Future<CerradoTemplatesCatalog> getTemplates() async =>
      const CerradoTemplatesCatalog(
        defaultNeutralGrade: 6,
        cvmDisclaimer: 'Aviso CVM.',
        templates: <CerradoTemplate>[
          CerradoTemplate(
            code: 'EQUILIBRADO_CERRADO',
            name: 'Modelo Equilibrado Cerrado',
            description: 'Balanceado',
            targetAcoesBrBps: 3000,
            targetFiisBps: 2500,
            targetRendaFixaBps: 2400,
            targetInternacionalBps: 2000,
            targetCriptoBps: 100,
          ),
        ],
      );

  @override
  Future<CerradoPreferences?> getPreferences() async => null;

  @override
  Future<CerradoPreferences> savePreferences({
    required Map<CerradoMacroClass, int> targetsBps,
    double? grahamMultiplier,
    bool? grahamFilterEnabled,
  }) {
    throw UnimplementedError();
  }
}

void main() {
  tearDown(FeatureFlags.resetForTest);

  testWidgets(
      'Router de produção preserva scroll entre 4 abas, redireciona /graham-recommendations e bloqueia /expenses',
      (tester) async {
    FeatureFlags.setEnableExpensesModuleForTest(false);
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final authRepo = _AuthenticatedFakeAuthRepository();
    final authListenable = AuthStateListenable(authRepo);
    final profileProvider = ProfileProvider(_StubProfileRepository());
    final tagProvider = TagProvider(repository: _StubTagRepository());
    addTearDown(authListenable.dispose);
    addTearDown(profileProvider.dispose);
    addTearDown(tagProvider.dispose);

    final router = createAppRouter(
      authRepository: authRepo,
      authListenable: authListenable,
      biometricAuthService: BiometricAuthService()..markInteractiveLoginSuccess(),
      profileProvider: profileProvider,
      tagProvider: tagProvider,
      dashboardBuilder: (_) => ListView.builder(
        key: const Key('dashboard_scroll'),
        itemCount: 30,
        itemBuilder: (_, i) => ListTile(title: Text('Ativo $i')),
      ),
      gpsAporteBuilder: (_) => const Center(child: Text('GPS Aporte View')),
      allocationStrategyBuilder: (_) => const Center(child: Text('Diagrama View')),
    );

    await tester.pumpWidget(
      ChangeNotifierProvider<ProfileProvider>.value(
        value: profileProvider,
        child: MaterialApp.router(
          theme: AppTheme.lightTheme,
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(router.routeInformationProvider.value.uri.path, '/dashboard');

    // Rola a lista da aba Carteira
    await tester.drag(find.byKey(const Key('dashboard_scroll')), const Offset(0, -300));
    await tester.pumpAndSettle();
    final scrollBefore = Scrollable.of(
      tester.element(find.text('Ativo 10')),
    ).position.pixels;
    expect(scrollBefore, greaterThan(0));

    // Navega para Kerno AI pela aba inferior
    await tester.tap(find.text('Kerno AI'));
    await tester.pumpAndSettle();
    expect(find.byType(KernoAiPage), findsOneWidget);

    // Volta para Carteira e confirma preservação do scroll
    await tester.tap(find.text('Carteira'));
    await tester.pumpAndSettle();
    final scrollAfter = Scrollable.of(
      tester.element(find.text('Ativo 10')),
    ).position.pixels;
    expect(scrollAfter, equals(scrollBefore));

    // Alias legado /graham-recommendations redireciona para /gps-aporte
    router.go('/graham-recommendations');
    await tester.pumpAndSettle();
    expect(find.text('GPS Aporte View'), findsOneWidget);
    expect(router.routeInformationProvider.value.uri.path, '/gps-aporte');

    // /expenses?tab=1 redireciona para /dashboard quando flag false
    router.go('/expenses?tab=1');
    await tester.pumpAndSettle();
    expect(router.routeInformationProvider.value.uri.path, '/dashboard');
  });

  testWidgets('Router de produção exibe /expenses quando flag true', (tester) async {
    FeatureFlags.setEnableExpensesModuleForTest(true);
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final authRepo = _AuthenticatedFakeAuthRepository();
    final authListenable = AuthStateListenable(authRepo);
    final profileProvider = ProfileProvider(_StubProfileRepository());
    final tagProvider = TagProvider(repository: _StubTagRepository());
    addTearDown(authListenable.dispose);
    addTearDown(profileProvider.dispose);
    addTearDown(tagProvider.dispose);

    final router = createAppRouter(
      authRepository: authRepo,
      authListenable: authListenable,
      biometricAuthService: BiometricAuthService()..markInteractiveLoginSuccess(),
      profileProvider: profileProvider,
      tagProvider: tagProvider,
      dashboardBuilder: (_) => const SizedBox(),
      gpsAporteBuilder: (_) => const SizedBox(),
      allocationStrategyBuilder: (_) => const SizedBox(),
    );

    await tester.pumpWidget(
      ChangeNotifierProvider<ProfileProvider>.value(
        value: profileProvider,
        child: MaterialApp.router(
          theme: AppTheme.lightTheme,
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Gastos'));
    await tester.pumpAndSettle();
    expect(find.byType(ExpensesPage), findsOneWidget);
  });

  testWidgets(
      'Rota /allocation-strategy renderiza CerradoDiagramPage por padrão e onSignOut reseta CerradoDiagramProvider',
      (tester) async {
    FeatureFlags.setEnableExpensesModuleForTest(false);
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final authRepo = _AuthenticatedFakeAuthRepository();
    final authListenable = AuthStateListenable(authRepo);
    final profileProvider = ProfileProvider(_StubProfileRepository());
    final tagProvider = TagProvider(repository: _StubTagRepository());
    final cerradoProvider = CerradoDiagramProvider(
      repository: _StubCerradoDiagramRepository(),
    );
    addTearDown(authListenable.dispose);
    addTearDown(profileProvider.dispose);
    addTearDown(tagProvider.dispose);
    addTearDown(cerradoProvider.dispose);

    final router = createAppRouter(
      authRepository: authRepo,
      authListenable: authListenable,
      biometricAuthService: BiometricAuthService()..markInteractiveLoginSuccess(),
      profileProvider: profileProvider,
      tagProvider: tagProvider,
      cerradoDiagramProvider: cerradoProvider,
      dashboardBuilder: (_) => const SizedBox(),
      gpsAporteBuilder: (_) => const SizedBox(),
    );

    await tester.pumpWidget(
      MultiProvider(
        providers: [
          ChangeNotifierProvider<ProfileProvider>.value(value: profileProvider),
          ChangeNotifierProvider<CerradoDiagramProvider>.value(
            value: cerradoProvider,
          ),
        ],
        child: MaterialApp.router(
          theme: AppTheme.lightTheme,
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Diagrama'));
    await tester.pumpAndSettle();
    expect(find.byType(CerradoDiagramPage), findsOneWidget);
    expect(cerradoProvider.status, CerradoDiagramStatus.loaded);

    final gate = tester.widget<BiometricLockGate>(find.byType(BiometricLockGate));
    await gate.onSignOut();
    expect(cerradoProvider.status, CerradoDiagramStatus.initial);
  });
}
