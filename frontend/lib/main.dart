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
      builder: (context, state) =>
          const Scaffold(body: Center(child: Text('Ranking Page - Em breve'))),
    ),
  ],
);

class GrahamSelectApp extends StatelessWidget {
  const GrahamSelectApp({super.key});

  @override
  Widget build(BuildContext context) {
    // Dependency injection
    final httpClient = http.Client();
    final remoteDataSource = UploadRemoteDataSource(client: httpClient);
    final repository = UploadRepositoryImpl(remoteDataSource);
    final uploadUseCase = UploadFileUseCase(repository);

    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => UploadProvider(uploadUseCase)),
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
