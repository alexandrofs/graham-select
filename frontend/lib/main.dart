import 'package:flutter/material.dart';
import 'dart:async';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:http/http.dart' as http;
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'src/core/theme/app_theme.dart';
import 'src/core/api/api_client.dart';
import 'src/features/home/presentation/pages/home_page.dart';
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
import 'src/features/portfolio/presentation/providers/portfolio_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
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

  const GrahamSelectApp({
    super.key,
    this.authRepository,
    this.apiClient,
    this.storage,
  });

  @override
  State<GrahamSelectApp> createState() => _GrahamSelectAppState();
}

class _GrahamSelectAppState extends State<GrahamSelectApp> {
  late final http.Client httpClient;
  late final FlutterSecureStorage effectiveStorage;
  late final ApiClient effectiveApiClient;
  late final AuthRepository effectiveAuthRepository;

  late final UploadRemoteDataSource uploadRemoteDataSource;
  late final UploadRepositoryImpl uploadRepository;
  late final UploadFileUseCase uploadUseCase;
  late final UploadB3FileUseCase uploadB3UseCase;
  late final GetB3UploadStatusUseCase getB3UploadStatusUseCase;

  late final RankingRemoteDataSource rankingRemoteDataSource;
  late final RankingRepositoryImpl rankingRepository;
  late final GetRankingUseCase getRankingUseCase;

  late final ProfileRepository profileRepository;

  late final PortfolioRemoteDataSource portfolioRemoteDataSource;
  late final PortfolioRepositoryImpl portfolioRepository;

  late final AuthStateListenable authListenable;
  late final GoRouter router;

  @override
  void initState() {
    super.initState();
    // Shared dependencies
    httpClient = http.Client();
    const baseUrl = 'https://graham-select-api.render.com';
    effectiveStorage = widget.storage ?? const FlutterSecureStorage();
    effectiveApiClient = widget.apiClient ?? ApiClient(baseUrl: baseUrl, storage: effectiveStorage);

    // Auth Feature DI
    effectiveAuthRepository = widget.authRepository ?? 
        AuthRepository(apiClient: effectiveApiClient, storage: effectiveStorage);

    // Upload Feature DI
    uploadRemoteDataSource = UploadRemoteDataSource(client: httpClient);
    uploadRepository = UploadRepositoryImpl(uploadRemoteDataSource);
    uploadUseCase = UploadFileUseCase(uploadRepository);
    uploadB3UseCase = UploadB3FileUseCase(uploadRepository);
    getB3UploadStatusUseCase = GetB3UploadStatusUseCase(uploadRepository);

    // Ranking Feature DI
    rankingRemoteDataSource = RankingRemoteDataSource(client: httpClient);
    rankingRepository = RankingRepositoryImpl(rankingRemoteDataSource);
    getRankingUseCase = GetRankingUseCase(rankingRepository);

    // Profile Feature DI
    profileRepository = ProfileRepository(effectiveApiClient);

    // Portfolio Feature DI
    portfolioRemoteDataSource = PortfolioRemoteDataSource(client: httpClient);
    portfolioRepository = PortfolioRepositoryImpl(portfolioRemoteDataSource, effectiveAuthRepository);

    authListenable = AuthStateListenable(effectiveAuthRepository);

    router = GoRouter(
      initialLocation: '/',
      refreshListenable: authListenable,
      redirect: (context, state) async {
        final firebaseUser = effectiveAuthRepository.currentUser;
        // O JWT token agora é carregado da memória em vez de fazer IO toda vez
        final jwtToken = await effectiveAuthRepository.getPersistedToken();
        
        // Consideramos logado se tivermos tanto o usuário Firebase quanto o JWT do backend
        final isLoggedIn = firebaseUser != null && jwtToken != null;
        final isLoggingIn = state.matchedLocation == '/login';

        if (!isLoggedIn) {
          return isLoggingIn ? null : '/login';
        }

        if (isLoggingIn) {
          return '/';
        }

        return null;
      },
      routes: [
        GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
        GoRoute(path: '/', builder: (context, state) => const HomePage()),
        GoRoute(path: '/upload', builder: (context, state) => const UploadPage()),
        GoRoute(path: '/ranking', builder: (context, state) => const RankingPage()),
        GoRoute(path: '/docs', builder: (context, state) => const DocsScreen()),
        GoRoute(path: '/profile', builder: (context, state) => const ProfilePage()),
        GoRoute(path: '/suitability', builder: (context, state) => const InvestorProfilePage()),
      ],
    );
  }

  @override
  void dispose() {
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
        ChangeNotifierProvider(
          create: (_) => UploadProvider(
            uploadFileUseCase: uploadUseCase,
            uploadB3FileUseCase: uploadB3UseCase,
            getB3UploadStatusUseCase: getB3UploadStatusUseCase,
          ),
        ),
        ChangeNotifierProvider(create: (_) => RankingProvider(getRankingUseCase)),
        ChangeNotifierProvider(create: (_) => ProfileProvider(profileRepository)),
        ChangeNotifierProvider(create: (_) => PortfolioProvider(portfolioRepository)),
      ],
      child: MaterialApp.router(
        title: 'Graham Select',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.darkTheme,
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
