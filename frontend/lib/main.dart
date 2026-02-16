import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'src/core/theme/app_theme.dart';
import 'src/features/home/presentation/pages/home_page.dart';

void main() {
  runApp(const GrahamSelectApp());
}

final _router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const HomePage(),
    ),
    GoRoute(
      path: '/upload',
      builder: (context, state) => const Scaffold(
        body: Center(child: Text('Upload Page - Em breve')),
      ),
    ),
    GoRoute(
      path: '/ranking',
      builder: (context, state) => const Scaffold(
        body: Center(child: Text('Ranking Page - Em breve')),
      ),
    ),
  ],
);

class GrahamSelectApp extends StatelessWidget {
  const GrahamSelectApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Graham Select',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      routerConfig: _router,
    );
  }
}
