import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:http/http.dart' as http;
import 'src/core/theme/app_theme.dart';
import 'src/features/home/presentation/pages/home_page.dart';
import 'src/features/upload/presentation/pages/upload_page.dart';
import 'src/features/upload/presentation/providers/upload_provider.dart';
import 'src/features/upload/domain/usecases/upload_file_usecase.dart';
import 'src/features/upload/data/repositories/upload_repository_impl.dart';
import 'src/features/upload/data/datasources/upload_remote_data_source.dart';

// Ranking Feature Imports
import 'src/features/ranking/data/datasources/ranking_remote_data_source.dart';
import 'src/features/ranking/data/repositories/ranking_repository_impl.dart';
import 'src/features/ranking/domain/usecases/get_ranking_usecase.dart';
import 'src/features/ranking/presentation/providers/ranking_provider.dart';
import 'src/features/ranking/presentation/pages/ranking_page.dart';

void main() {
  runApp(const GrahamSelectApp());
}

final _router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(path: '/', builder: (context, state) => const HomePage()),
    GoRoute(path: '/upload', builder: (context, state) => const UploadPage()),
    GoRoute(
      path: '/ranking',
      builder: (context, state) => const RankingPage(),
    ),
  ],
);

class GrahamSelectApp extends StatelessWidget {
  const GrahamSelectApp({super.key});

  @override
  Widget build(BuildContext context) {
    // Shared dependencies
    final httpClient = http.Client();

    // Upload Feature DI
    final uploadRemoteDataSource = UploadRemoteDataSource(client: httpClient);
    final uploadRepository = UploadRepositoryImpl(uploadRemoteDataSource);
    final uploadUseCase = UploadFileUseCase(uploadRepository);

    // Ranking Feature DI
    final rankingRemoteDataSource = RankingRemoteDataSource(client: httpClient);
    final rankingRepository = RankingRepositoryImpl(rankingRemoteDataSource);
    final getRankingUseCase = GetRankingUseCase(rankingRepository);

    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => UploadProvider(uploadUseCase)),
        ChangeNotifierProvider(create: (_) => RankingProvider(getRankingUseCase)),
      ],
      child: MaterialApp.router(
        title: 'Graham Select',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.darkTheme,
        routerConfig: _router,
      ),
    );
  }
}
