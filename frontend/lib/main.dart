import 'package:flutter/material.dart';
import 'dart:async';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:http/http.dart' as http;
import 'package:firebase_core/firebase_core.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'src/core/theme/app_theme.dart';
import 'src/core/config/feature_flags.dart';
import 'src/core/api/api_client.dart';
import 'src/core/security/biometric_auth_service.dart';
import 'src/core/security/biometric_lock_gate.dart';
import 'src/core/security/encrypted_portfolio_cache.dart';
import 'src/core/security/privacy_provider.dart';
import 'src/features/home/presentation/pages/home_page.dart';
import 'src/features/dashboard/presentation/pages/dashboard_page.dart';
import 'src/core/widgets/main_layout.dart';
import 'src/features/upload/presentation/pages/upload_page.dart';
import 'src/features/upload/presentation/providers/upload_provider.dart';
import 'src/features/upload/domain/usecases/get_b3_upload_status_usecase.dart';
import 'src/features/upload/domain/usecases/upload_file_usecase.dart';
import 'src/features/upload/domain/usecases/upload_b3_file_usecase.dart';
import 'src/features/upload/data/repositories/upload_repository_impl.dart';
import 'src/features/upload/data/datasources/upload_remote_data_source.dart';


// Auth Feature Imports
import 'src/features/auth/data/auth_repository.dart';
import 'src/features/auth/presentation/login_screen.dart';

// Ranking Feature Imports
import 'src/features/ranking/data/datasources/ranking_remote_data_source.dart';
import 'src/features/ranking/data/repositories/ranking_repository_impl.dart';
import 'src/features/ranking/domain/usecases/get_ranking_usecase.dart';
import 'src/features/ranking/presentation/providers/ranking_provider.dart';
import 'src/features/ranking/presentation/pages/ranking_page.dart';
import 'src/features/ranking/data/datasources/graham_recommendation_remote_data_source.dart';
import 'src/features/ranking/data/repositories/graham_recommendation_repository_impl.dart';
import 'src/features/ranking/presentation/providers/graham_recommendation_provider.dart';
import 'src/features/ranking/presentation/pages/graham_recommendations_page.dart';

// Docs Feature Imports
import 'src/features/docs/presentation/pages/docs_screen.dart';

// Profile Feature Imports
import 'src/features/profile/data/repositories/profile_repository.dart';
import 'src/features/profile/presentation/providers/profile_provider.dart';
import 'src/features/profile/presentation/pages/profile_page.dart';
import 'src/features/profile/presentation/pages/investor_profile_page.dart';

// Portfolio Feature Imports
import 'src/features/portfolio/data/datasources/portfolio_remote_data_source.dart';
import 'src/features/portfolio/data/repositories/portfolio_repository_impl.dart';
import 'src/features/portfolio/data/datasources/notification_service.dart';
import 'src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'src/features/portfolio/presentation/pages/evolution_page.dart';

// Allocation Feature Imports
import 'src/features/allocation/data/datasources/allocation_remote_data_source.dart';
import 'src/features/allocation/data/repositories/allocation_repository_impl.dart';
import 'src/features/allocation/presentation/providers/allocation_provider.dart';

// Cerrado Diagram Feature Imports
import 'src/features/cerrado_diagram/data/datasources/cerrado_diagram_remote_data_source.dart';
import 'src/features/cerrado_diagram/data/repositories/cerrado_diagram_repository_impl.dart';
import 'src/features/cerrado_diagram/presentation/pages/cerrado_diagram_page.dart';
import 'src/features/cerrado_diagram/presentation/providers/cerrado_diagram_provider.dart';

// Goals Feature Imports
import 'src/features/goals/data/datasources/goals_remote_datasource.dart';
import 'src/features/goals/data/repositories/goals_repository_impl.dart';
import 'src/features/goals/presentation/providers/goals_provider.dart';
import 'src/features/goals/presentation/pages/goals_page.dart';

// Admin Feature Imports
import 'src/features/admin/data/datasources/admin_remote_datasource.dart';
import 'src/features/admin/data/repositories/admin_repository_impl.dart';
import 'src/features/admin/presentation/providers/admin_dlq_provider.dart';
import 'src/features/admin/presentation/providers/admin_observability_provider.dart';
import 'src/features/admin/presentation/providers/admin_ticker_provider.dart';
import 'src/features/admin/presentation/pages/admin_dashboard_page.dart';
import 'src/features/admin/presentation/pages/admin_dlq_page.dart';
import 'src/features/admin/presentation/pages/admin_tickers_page.dart';

// Family Holding Feature Imports
import 'src/features/family_holding/data/datasources/family_profile_remote_datasource.dart';
import 'src/features/family_holding/data/repositories/family_profile_repository_impl.dart';
import 'src/features/family_holding/presentation/providers/family_profile_provider.dart';
import 'src/features/family_holding/presentation/pages/family_profiles_page.dart';

// Tags Feature Imports
import 'src/features/tags/data/datasources/tag_remote_datasource.dart';
import 'src/features/tags/data/repositories/tag_repository_impl.dart';
import 'src/features/tags/presentation/providers/tag_provider.dart';
import 'src/features/tags/presentation/pages/tag_management_page.dart';

// Dividends Feature Imports
import 'src/features/dividends/data/datasources/dividend_remote_data_source.dart';
import 'src/features/dividends/data/repositories/dividend_repository_impl.dart';
import 'src/features/dividends/presentation/providers/dividend_provider.dart';
import 'src/features/dividends/presentation/pages/dividends_page.dart';

// Open Finance Feature Imports
import 'src/features/open_finance/data/datasources/open_finance_remote_data_source.dart';
import 'src/features/open_finance/data/repositories/open_finance_repository_impl.dart';
import 'src/features/open_finance/presentation/providers/open_finance_provider.dart';

// Tax Feature Imports
import 'src/features/tax/data/datasources/tax_remote_data_source.dart';
import 'src/features/tax/data/repositories/tax_repository_impl.dart';
import 'src/features/tax/presentation/providers/tax_provider.dart';
import 'src/features/tax/presentation/pages/tax_declaration_page.dart';

// WhatsApp Notifications Feature Imports
import 'src/features/notifications/data/datasources/whatsapp_notifications_remote_data_source.dart';
import 'src/features/notifications/data/repositories/whatsapp_notifications_repository.dart';
import 'src/features/notifications/presentation/providers/whatsapp_notifications_provider.dart';
import 'src/features/notifications/presentation/pages/whatsapp_notifications_page.dart';

// Web Push Notifications Feature Imports
import 'src/features/notifications/data/datasources/web_push_notifications_remote_data_source.dart';
import 'src/features/notifications/data/repositories/web_push_notifications_repository.dart';
import 'src/features/notifications/data/services/web_push_browser_client.dart';
import 'src/features/notifications/presentation/providers/web_push_notifications_provider.dart';
import 'src/features/notifications/presentation/pages/web_push_notifications_page.dart';

// Kerno AI & Expenses Modular Stubs
import 'src/features/ai_analysis/presentation/pages/kerno_ai_page.dart';
import 'src/features/expenses/presentation/pages/expenses_page.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('pt_BR', null);
  
  try {
    // No Flutter Web sem firebase_options.dart, o Firebase.initializeApp() falha
    // se não passarmos as opções explicitamente. Agora pegamos essas opções via
    // variáveis de compilação (--dart-define) para que a troca de token no Firebase Auth funcione.
    await Firebase.initializeApp(
      options: FirebaseOptions(
        apiKey: const String.fromEnvironment('FIREBASE_API_KEY', defaultValue: 'dummy-api-key'),
        authDomain: const String.fromEnvironment('FIREBASE_AUTH_DOMAIN', defaultValue: 'dummy-auth-domain'),
        projectId: const String.fromEnvironment('FIREBASE_PROJECT_ID', defaultValue: 'dummy-project-id'),
        storageBucket: const String.fromEnvironment('FIREBASE_STORAGE_BUCKET', defaultValue: 'dummy-storage-bucket'),
        messagingSenderId: const String.fromEnvironment('FIREBASE_MESSAGING_SENDER_ID', defaultValue: 'dummy-sender-id'),
        appId: const String.fromEnvironment('FIREBASE_APP_ID', defaultValue: 'dummy-app-id'),
        measurementId: const String.fromEnvironment('FIREBASE_MEASUREMENT_ID', defaultValue: 'dummy-measurement-id'),
      ),
    );
    debugPrint('Firebase inicializado com sucesso');
  } catch (e) {
    debugPrint('Erro não fatal na inicialização do Firebase: $e');
  }
  
  runApp(const GrahamSelectApp());
}

class GrahamSelectApp extends StatefulWidget {
  final AuthRepository? authRepository;
  final ApiClient? apiClient;
  final FlutterSecureStorage? storage;
  final BiometricAuthService? biometricAuthService;
  final EncryptedPortfolioCache? portfolioCache;
  final PrivacyProvider? privacyProvider;

  const GrahamSelectApp({
    super.key,
    this.authRepository,
    this.apiClient,
    this.storage,
    this.biometricAuthService,
    this.portfolioCache,
    this.privacyProvider,
  });

  @override
  State<GrahamSelectApp> createState() => _GrahamSelectAppState();
}

class _GrahamSelectAppState extends State<GrahamSelectApp> {
  late final http.Client httpClient;
  late final FlutterSecureStorage effectiveStorage;
  late final ApiClient effectiveApiClient;
  late final BiometricAuthService effectiveBiometricAuthService;
  late final EncryptedPortfolioCache effectivePortfolioCache;
  late final PrivacyProvider effectivePrivacyProvider;
  late final AuthRepository effectiveAuthRepository;

  late final UploadRemoteDataSource uploadRemoteDataSource;
  late final UploadRepositoryImpl uploadRepository;
  late final UploadFileUseCase uploadUseCase;
  late final UploadB3FileUseCase uploadB3UseCase;
  late final GetB3UploadStatusUseCase getB3UploadStatusUseCase;

  late final RankingRemoteDataSource rankingRemoteDataSource;
  late final RankingRepositoryImpl rankingRepository;
  late final GetRankingUseCase getRankingUseCase;
  late final GrahamRecommendationRemoteDataSource grahamRecommendationRemoteDataSource;
  late final GrahamRecommendationRepositoryImpl grahamRecommendationRepository;

  late final ProfileRepository profileRepository;
  late final ProfileProvider profileProvider;
  StreamSubscription<dynamic>? _profileAuthSubscription;
  String? _activeProfileUserId;

  late final PortfolioRemoteDataSource portfolioRemoteDataSource;
  late final PortfolioRepositoryImpl portfolioRepository;

  late final AllocationRemoteDataSource allocationRemoteDataSource;
  late final AllocationRepositoryImpl allocationRepository;

  late final CerradoDiagramRemoteDataSource cerradoDiagramRemoteDataSource;
  late final CerradoDiagramRepositoryImpl cerradoDiagramRepository;
  late final CerradoDiagramProvider cerradoDiagramProvider;
  StreamSubscription<dynamic>? _cerradoAuthSubscription;
  String? _activeCerradoUserId;

  late final GoalsRemoteDatasource goalsRemoteDatasource;
  late final GoalsRepositoryImpl goalsRepository;

  late final AdminRemoteDatasource adminRemoteDatasource;
  late final AdminRepositoryImpl adminRepository;

  late final FamilyProfileRemoteDataSource familyProfileRemoteDataSource;
  late final FamilyProfileRepositoryImpl familyProfileRepository;

  late final TagRemoteDataSource tagRemoteDataSource;
  late final TagRepositoryImpl tagRepository;
  late final TagProvider tagProvider;
  StreamSubscription<dynamic>? _tagAuthSubscription;
  String? _activeTagUserId;

  late final DividendRemoteDataSource dividendRemoteDataSource;
  late final DividendRepositoryImpl dividendRepository;

  late final OpenFinanceRemoteDataSource openFinanceRemoteDataSource;
  late final OpenFinanceRepositoryImpl openFinanceRepository;

  late final TaxRemoteDataSource taxRemoteDataSource;
  late final TaxRepositoryImpl taxRepository;

  late final WhatsappNotificationsRemoteDataSource whatsappNotificationsRemoteDataSource;
  late final WhatsappNotificationsRepository whatsappNotificationsRepository;

  late final WebPushNotificationsRemoteDataSource webPushNotificationsRemoteDataSource;
  late final WebPushNotificationsRepository webPushNotificationsRepository;
  late final WebPushBrowserClient webPushBrowserClient;

  late final AuthStateListenable authListenable;
  late final GoRouter router;

  @override
  void initState() {
    super.initState();
    // Shared dependencies
    httpClient = http.Client();
    // baseUrl para datasources que usam http.Client diretamente (inclui /api/v1)
    const datasourceBaseUrl = String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: 'http://localhost:8080/api/v1',
    );
    // baseUrl para ApiClient (Dio) - sem /api/v1 pois os repos já incluem o prefixo nos paths
    final apiClientBaseUrl = datasourceBaseUrl.replaceFirst('/api/v1', '');
    effectiveStorage = widget.storage ?? EncryptedPortfolioCache.defaultSecureStorage;
    effectiveBiometricAuthService = widget.biometricAuthService ?? BiometricAuthService();
    effectivePortfolioCache = widget.portfolioCache ?? EncryptedPortfolioCache(storage: effectiveStorage);
    effectivePrivacyProvider = widget.privacyProvider ?? PrivacyProvider(storage: effectiveStorage);
    if (widget.privacyProvider == null) {
      unawaited(effectivePrivacyProvider.load());
    }
    effectiveApiClient = widget.apiClient ?? ApiClient(baseUrl: apiClientBaseUrl, storage: effectiveStorage);

    // Auth Feature DI
    effectiveAuthRepository = widget.authRepository ?? 
        AuthRepository(
          apiClient: effectiveApiClient,
          storage: effectiveStorage,
          portfolioCache: effectivePortfolioCache,
          biometricAuthService: effectiveBiometricAuthService,
          onClearCache: () async => effectiveBiometricAuthService.clearBackgroundTimestamp(),
        );

    // Conecta o AuthRepository ao ApiClient para auto-refresh de token
    effectiveApiClient.setTokenProvider(effectiveAuthRepository);
    uploadRemoteDataSource = UploadRemoteDataSource(client: httpClient, baseUrl: datasourceBaseUrl);
    uploadRepository = UploadRepositoryImpl(uploadRemoteDataSource, effectiveAuthRepository);
    uploadUseCase = UploadFileUseCase(uploadRepository);
    uploadB3UseCase = UploadB3FileUseCase(uploadRepository);
    getB3UploadStatusUseCase = GetB3UploadStatusUseCase(uploadRepository);

    // Ranking Feature DI
    rankingRemoteDataSource = RankingRemoteDataSource(client: httpClient, baseUrl: datasourceBaseUrl);
    rankingRepository = RankingRepositoryImpl(rankingRemoteDataSource, effectiveAuthRepository);
    getRankingUseCase = GetRankingUseCase(rankingRepository);

    // Graham Recommendations DI
    grahamRecommendationRemoteDataSource = GrahamRecommendationRemoteDataSource(client: httpClient, baseUrl: datasourceBaseUrl);
    grahamRecommendationRepository = GrahamRecommendationRepositoryImpl(
      remoteDataSource: grahamRecommendationRemoteDataSource,
      authRepository: effectiveAuthRepository,
    );

    // Profile Feature DI
    profileRepository = ProfileRepository(effectiveApiClient);
    profileProvider = ProfileProvider(profileRepository);
    _activeProfileUserId = effectiveAuthRepository.currentUser?.uid;
    _profileAuthSubscription = effectiveAuthRepository.authStateChanges.listen((user) {
      final nextUserId = user?.uid;
      if (nextUserId == null ||
          (_activeProfileUserId != null && _activeProfileUserId != nextUserId)) {
        profileProvider.reset();
      }
      _activeProfileUserId = nextUserId;
    });

    // Portfolio Feature DI
    portfolioRemoteDataSource = PortfolioRemoteDataSource(client: httpClient, baseUrl: datasourceBaseUrl);
    portfolioRepository = PortfolioRepositoryImpl(portfolioRemoteDataSource, effectiveAuthRepository);

    // Allocation Feature DI
    allocationRemoteDataSource = AllocationRemoteDataSource(apiClient: effectiveApiClient);
    allocationRepository = AllocationRepositoryImpl(allocationRemoteDataSource, effectiveAuthRepository);

    // Cerrado Diagram Feature DI
    cerradoDiagramRemoteDataSource = CerradoDiagramRemoteDataSource(apiClient: effectiveApiClient);
    cerradoDiagramRepository = CerradoDiagramRepositoryImpl(remoteDataSource: cerradoDiagramRemoteDataSource);
    cerradoDiagramProvider = CerradoDiagramProvider(repository: cerradoDiagramRepository);
    _activeCerradoUserId = effectiveAuthRepository.currentUser?.uid;
    _cerradoAuthSubscription = effectiveAuthRepository.authStateChanges.listen((user) {
      final nextUserId = user?.uid;
      if (nextUserId == null ||
          (_activeCerradoUserId != null && _activeCerradoUserId != nextUserId)) {
        cerradoDiagramProvider.reset();
      }
      _activeCerradoUserId = nextUserId;
    });

    // Goals Feature DI
    goalsRemoteDatasource = GoalsRemoteDatasource(apiClient: effectiveApiClient);
    goalsRepository = GoalsRepositoryImpl(remoteDatasource: goalsRemoteDatasource);

    // Admin Feature DI
    adminRemoteDatasource = AdminRemoteDatasource(apiClient: effectiveApiClient);
    adminRepository = AdminRepositoryImpl(remoteDatasource: adminRemoteDatasource);

    // Family Holding Feature DI
    familyProfileRemoteDataSource = FamilyProfileRemoteDataSource(apiClient: effectiveApiClient);
    familyProfileRepository = FamilyProfileRepositoryImpl(remoteDataSource: familyProfileRemoteDataSource);

    // Tags Feature DI
    tagRemoteDataSource = TagRemoteDataSource(apiClient: effectiveApiClient);
    tagRepository = TagRepositoryImpl(remoteDataSource: tagRemoteDataSource);
    tagProvider = TagProvider(repository: tagRepository);
    _activeTagUserId = effectiveAuthRepository.currentUser?.uid;
    _tagAuthSubscription = effectiveAuthRepository.authStateChanges.listen((user) {
      final nextUserId = user?.uid;
      if (nextUserId == null ||
          (_activeTagUserId != null && _activeTagUserId != nextUserId)) {
        tagProvider.reset();
      }
      _activeTagUserId = nextUserId;
    });

    // Dividends Feature DI
    dividendRemoteDataSource = DividendRemoteDataSource(apiClient: effectiveApiClient);
    dividendRepository = DividendRepositoryImpl(remoteDataSource: dividendRemoteDataSource);

    // Open Finance Feature DI
    openFinanceRemoteDataSource = OpenFinanceRemoteDataSourceImpl(apiClient: effectiveApiClient);
    openFinanceRepository = OpenFinanceRepositoryImpl(remoteDataSource: openFinanceRemoteDataSource);

    // Tax Feature DI
    taxRemoteDataSource = TaxRemoteDataSource(apiClient: effectiveApiClient);
    taxRepository = TaxRepositoryImpl(remoteDataSource: taxRemoteDataSource);

    // WhatsApp Notifications Feature DI
    whatsappNotificationsRemoteDataSource = WhatsappNotificationsRemoteDataSource(apiClient: effectiveApiClient);
    whatsappNotificationsRepository = WhatsappNotificationsRepository(remoteDataSource: whatsappNotificationsRemoteDataSource);

    // Web Push Notifications Feature DI
    webPushNotificationsRemoteDataSource = WebPushNotificationsRemoteDataSource(apiClient: effectiveApiClient);
    webPushNotificationsRepository = WebPushNotificationsRepository(remoteDataSource: webPushNotificationsRemoteDataSource);
    webPushBrowserClient = WebPushBrowserClient();

    authListenable = AuthStateListenable(effectiveAuthRepository);

    router = createAppRouter(
      authRepository: effectiveAuthRepository,
      authListenable: authListenable,
      biometricAuthService: effectiveBiometricAuthService,
      profileProvider: profileProvider,
      tagProvider: tagProvider,
      cerradoDiagramProvider: cerradoDiagramProvider,
    );
  }

  @override
  void dispose() {
    if (widget.privacyProvider == null) {
      effectivePrivacyProvider.dispose();
    }
    _profileAuthSubscription?.cancel();
    profileProvider.dispose();
    _tagAuthSubscription?.cancel();
    tagProvider.dispose();
    _cerradoAuthSubscription?.cancel();
    cerradoDiagramProvider.dispose();
    authListenable.dispose();
    router.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider.value(value: effectiveAuthRepository),
        Provider.value(value: effectiveApiClient),
        ChangeNotifierProvider<PrivacyProvider>.value(value: effectivePrivacyProvider),
        ChangeNotifierProvider(
          create: (_) => UploadProvider(
            uploadFileUseCase: uploadUseCase,
            uploadB3FileUseCase: uploadB3UseCase,
            getB3UploadStatusUseCase: getB3UploadStatusUseCase,
          ),
        ),
        ChangeNotifierProvider(create: (_) => RankingProvider(getRankingUseCase)),
        Provider<NotificationService>(
          create: (_) => NotificationService(effectiveApiClient),
          dispose: (_, service) => service.dispose(),
        ),
        ChangeNotifierProvider(
          create: (context) => GrahamRecommendationProvider(
            repository: grahamRecommendationRepository,
            notificationService: context.read<NotificationService>(),
          ),
        ),
        ChangeNotifierProvider<ProfileProvider>.value(value: profileProvider),
        ChangeNotifierProvider(create: (_) => FamilyProfileProvider(repository: familyProfileRepository)),
        ChangeNotifierProvider<TagProvider>.value(value: tagProvider),
        ChangeNotifierProvider(create: (_) => AllocationProvider(allocationRepository)),
        ChangeNotifierProvider<CerradoDiagramProvider>.value(value: cerradoDiagramProvider),
        ChangeNotifierProvider(create: (_) => GoalsProvider(repository: goalsRepository)),
        ChangeNotifierProvider(create: (_) => DividendProvider(repository: dividendRepository)),
        ChangeNotifierProvider(create: (_) => OpenFinanceProvider(repository: openFinanceRepository)),
        ChangeNotifierProvider(create: (_) => TaxProvider(repository: taxRepository)),
        ChangeNotifierProvider(create: (_) => WhatsappNotificationsProvider(repository: whatsappNotificationsRepository)),
        ChangeNotifierProvider(
          create: (_) => WebPushNotificationsProvider(
            repository: webPushNotificationsRepository,
            browserClient: webPushBrowserClient,
          ),
        ),
        ChangeNotifierProvider(create: (_) => AdminObservabilityProvider(repository: adminRepository)),
        ChangeNotifierProvider(create: (_) => AdminDlqProvider(repository: adminRepository)),
        ChangeNotifierProvider(create: (_) => AdminTickerProvider(repository: adminRepository)),
        ChangeNotifierProvider(
          create: (context) {
            final provider = PortfolioProvider(
              portfolioRepository, 
              context.read<NotificationService>(),
              effectivePortfolioCache,
              () => effectiveAuthRepository.currentUser?.uid,
            );
            provider.startListeningForUpdates();
            return provider;
          },
        ),
      ],
      child: MaterialApp.router(
        title: 'Kerno — O núcleo inteligente do seu dinheiro',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        darkTheme: AppTheme.darkTheme,
        themeMode: ThemeMode.system,
        routerConfig: router,
      ),
    );
  }
}

/// Helper para notificar o GoRouter sobre mudanças no estado de auth
class AuthStateListenable extends ChangeNotifier {
  late final StreamSubscription _subscription;

  AuthStateListenable(AuthRepository repository) {
    _subscription = repository.authStateChanges.listen((_) => notifyListeners());
  }

  @override
  void dispose() {
    _subscription.cancel();
    super.dispose();
  }
}

/// Constrói o [GoRouter] de produção com [StatefulShellRoute.indexedStack].
GoRouter createAppRouter({
  required AuthRepository authRepository,
  required Listenable authListenable,
  required BiometricAuthService biometricAuthService,
  required ProfileProvider profileProvider,
  required TagProvider tagProvider,
  CerradoDiagramProvider? cerradoDiagramProvider,
  WidgetBuilder? dashboardBuilder,
  WidgetBuilder? gpsAporteBuilder,
  WidgetBuilder? allocationStrategyBuilder,
}) {
  return GoRouter(
    initialLocation: '/',
    refreshListenable: authListenable,
    redirect: (context, state) async {
      final firebaseUser = authRepository.currentUser;
      final jwtToken = await authRepository.getPersistedToken();

      final isLoggedIn = firebaseUser != null && jwtToken != null;
      final isLoggingIn = state.matchedLocation == '/login';
      final isAtLanding = state.matchedLocation == '/';
      final isDocs = state.matchedLocation == '/docs';

      if (!isLoggedIn) {
        if (isLoggingIn || isAtLanding || isDocs) return null;
        return '/login';
      }

      if (isLoggingIn || (isAtLanding && isLoggedIn)) {
        return '/dashboard';
      }

      if (!FeatureFlags.enableExpensesModule &&
          state.uri.path.startsWith('/expenses')) {
        return '/dashboard';
      }

      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(path: '/', builder: (context, state) => const HomePage()),
      GoRoute(path: '/docs', builder: (context, state) => const DocsScreen()),
      if (!FeatureFlags.enableExpensesModule)
        GoRoute(
          path: '/expenses',
          redirect: (context, state) => '/dashboard',
        ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) => BiometricLockGate(
          biometricAuthService: biometricAuthService,
          onSignOut: () async {
            final portfolioProvider = Provider.of<PortfolioProvider?>(context, listen: false);
            await authRepository.signOut();
            portfolioProvider?.reset();
            profileProvider.reset();
            tagProvider.reset();
            cerradoDiagramProvider?.reset();
          },
          child: MainLayout(navigationShell: navigationShell),
        ),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/dashboard',
                builder: (context, state) =>
                    dashboardBuilder?.call(context) ?? const DashboardPage(),
              ),
              GoRoute(path: '/evolution', builder: (context, state) => const EvolutionPage()),
              GoRoute(path: '/upload', builder: (context, state) => const UploadPage()),
              GoRoute(path: '/ranking', builder: (context, state) => const RankingPage()),
              GoRoute(path: '/profile', builder: (context, state) => const ProfilePage()),
              GoRoute(path: '/notifications/whatsapp', builder: (context, state) => const WhatsappNotificationsPage()),
              GoRoute(path: '/notifications/web-push', builder: (context, state) => const WebPushNotificationsPage()),
              GoRoute(path: '/family-profiles', builder: (context, state) => const FamilyProfilesPage()),
              GoRoute(path: '/tags', builder: (context, state) => const TagManagementPage()),
              GoRoute(path: '/suitability', builder: (context, state) => const InvestorProfilePage()),
              GoRoute(path: '/metas', builder: (context, state) => const GoalsPage()),
              GoRoute(path: '/dividends', builder: (context, state) => const DividendsPage()),
              GoRoute(path: '/tax', builder: (context, state) => const TaxDeclarationPage()),
              GoRoute(path: '/admin/dashboard', builder: (context, state) => const AdminDashboardPage()),
              GoRoute(path: '/admin/dlq', builder: (context, state) => const AdminDlqPage()),
              GoRoute(path: '/admin/tickers', builder: (context, state) => const AdminTickersPage()),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/gps-aporte',
                builder: (context, state) =>
                    gpsAporteBuilder?.call(context) ?? const GrahamRecommendationsPage(),
              ),
              GoRoute(
                path: '/graham-recommendations',
                redirect: (context, state) => '/gps-aporte',
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(path: '/kerno-ai', builder: (context, state) => const KernoAiPage()),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/allocation-strategy',
                builder: (context, state) =>
                    allocationStrategyBuilder?.call(context) ?? const CerradoDiagramPage(),
              ),
            ],
          ),
          if (FeatureFlags.enableExpensesModule)
            StatefulShellBranch(
              routes: [
                GoRoute(path: '/expenses', builder: (context, state) => const ExpensesPage()),
              ],
            ),
        ],
      ),
    ],
  );
}
