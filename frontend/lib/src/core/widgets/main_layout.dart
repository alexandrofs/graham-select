import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../theme/app_theme.dart';

class MainLayout extends StatelessWidget {
  final Widget child;

  const MainLayout({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final isMobile = size.width < 900;

    return Scaffold(
      body: Row(
        children: [
          if (!isMobile) const _Sidebar(),
          Expanded(
            child: Column(
              children: [
                const _TopBar(),
                Expanded(child: child),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: isMobile ? const _BottomNavBar() : null,
    );
  }
}

class _Sidebar extends StatelessWidget {
  const _Sidebar();

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).matchedLocation;

    int getSelectedIndex() {
      if (location.startsWith('/dashboard')) return 0;
      if (location.startsWith('/evolution')) return 1;
      if (location.startsWith('/ranking')) return 2;
      if (location.startsWith('/upload')) return 3;
      if (location.startsWith('/profile')) return 4;
      return 0;
    }

    return NavigationRail(
      backgroundColor: AppTheme.surfaceColor,
      extended: true,
      minExtendedWidth: 200,
      selectedIndex: getSelectedIndex(),
      onDestinationSelected: (index) {
        switch (index) {
          case 0:
            context.go('/dashboard');
          case 1:
            context.go('/evolution');
          case 2:
            context.go('/ranking');
          case 3:
            context.go('/upload');
          case 4:
            context.go('/profile');
        }
      },
      leading: Padding(
        padding: const EdgeInsets.symmetric(vertical: 24.0),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.bar_chart_rounded, color: AppTheme.accentColor, size: 32),
            const SizedBox(width: 12),
            const Text(
              'Graham\nSelect',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 18,
                height: 1.1,
              ),
            ),
          ],
        ),
      ),
      destinations: const [
        NavigationRailDestination(
          icon: Icon(Icons.dashboard_outlined),
          selectedIcon: Icon(Icons.dashboard),
          label: Text('Dashboard'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.show_chart_rounded),
          selectedIcon: Icon(Icons.show_chart_rounded),
          label: Text('Evolução'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.star_outline),
          selectedIcon: Icon(Icons.star),
          label: Text('Ranking'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.cloud_upload_outlined),
          selectedIcon: Icon(Icons.cloud_upload),
          label: Text('Importação'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.person_outline),
          selectedIcon: Icon(Icons.person),
          label: Text('Meu Perfil'),
        ),
      ],
    );
  }
}

class _BottomNavBar extends StatelessWidget {
  const _BottomNavBar();

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).matchedLocation;

    int getSelectedIndex() {
      if (location.startsWith('/dashboard')) return 0;
      if (location.startsWith('/evolution')) return 1;
      if (location.startsWith('/ranking')) return 2;
      if (location.startsWith('/upload')) return 3;
      if (location.startsWith('/profile')) return 4;
      return 0;
    }

    return NavigationBar(
      selectedIndex: getSelectedIndex(),
      onDestinationSelected: (index) {
        switch (index) {
          case 0:
            context.go('/dashboard');
          case 1:
            context.go('/evolution');
          case 2:
            context.go('/ranking');
          case 3:
            context.go('/upload');
          case 4:
            context.go('/profile');
        }
      },
      destinations: const [
        NavigationDestination(
          icon: Icon(Icons.dashboard_outlined),
          label: 'Início',
        ),
        NavigationDestination(
          icon: Icon(Icons.show_chart_rounded),
          label: 'Evolução',
        ),
        NavigationDestination(
          icon: Icon(Icons.star_outline),
          label: 'Ranking',
        ),
        NavigationDestination(
          icon: Icon(Icons.cloud_upload_outlined),
          label: 'Upload',
        ),
        NavigationDestination(
          icon: Icon(Icons.person_outline),
          label: 'Perfil',
        ),
      ],
    );
  }
}

class _TopBar extends StatelessWidget {
  const _TopBar();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
      decoration: BoxDecoration(
        color: AppTheme.backgroundColor,
        border: Border(
          bottom: BorderSide(color: Colors.white.withValues(alpha: 0.05)),
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: Row(
          children: [
            const Spacer(),
            IconButton(
              onPressed: () {},
              icon: const Icon(Icons.notifications_outlined),
            ),
            const SizedBox(width: 8),
            const CircleAvatar(
              radius: 16,
              backgroundColor: AppTheme.primaryColor,
              child: Icon(Icons.person, size: 20, color: Colors.white),
            ),
          ],
        ),
      ),
    );
  }
}
