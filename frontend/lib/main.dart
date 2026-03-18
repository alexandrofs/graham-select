import 'package:flutter/material.dart';
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
import 'src/features/upload/domain/usecases/upload_file_usecase.dart';
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

class GrahamSelectApp extends StatelessWidget {
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
  Widget build(BuildContext context) {
    // Shared dependencies
    final httpClient = http.Client();
    const baseUrl = 'https://graham-select-api.render.com';
    final effectiveStorage = storage ?? const FlutterSecureStorage();
    final effectiveApiClient = apiClient ?? ApiClient(baseUrl: baseUrl, storage: effectiveStorage);

    // Auth Feature DI
    final effectiveAuthRepository = authRepository ?? 
        AuthRepository(apiClient: effectiveApiClient, storage: effectiveStorage);

    // Upload Feature DI
    final uploadRemoteDataSource = UploadRemoteDataSource(client: httpClient);
    final uploadRepository = UploadRepositoryImpl(uploadRemoteDataSource);
    final uploadUseCase = UploadFileUseCase(uploadRepository);

    // Ranking Feature DI
    final rankingRemoteDataSource = RankingRemoteDataSource(client: httpClient);
    final rankingRepository = RankingRepositoryImpl(rankingRemoteDataSource);
    final getRankingUseCase = GetRankingUseCase(rankingRepository);

    final router = GoRouter(
      initialLocation: '/',
      refreshListenable: AuthStateListenable(effectiveAuthRepository),
      redirect: (context, state) async {
        final firebaseUser = effectiveAuthRepository.currentUser;
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
      ],
    );

    return MultiProvider(
      providers: [
        Provider.value(value: effectiveAuthRepository),
        Provider.value(value: effectiveApiClient),
        ChangeNotifierProvider(create: (_) => UploadProvider(uploadUseCase)),
        ChangeNotifierProvider(create: (_) => RankingProvider(getRankingUseCase)),
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
  AuthStateListenable(AuthRepository repository) {
    repository.authStateChanges.listen((_) => notifyListeners());
  }
}
