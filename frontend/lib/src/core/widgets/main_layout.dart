import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../features/founders/presentation/widgets/commercial_plan_badge.dart';
import '../../features/profile/presentation/providers/profile_provider.dart';
import '../theme/app_theme.dart';

class MainLayout extends StatefulWidget {
  final Widget child;

  const MainLayout({super.key, required this.child});

  @override
  State<MainLayout> createState() => _MainLayoutState();
}

class _MainLayoutState extends State<MainLayout> {
  bool _profileRequestScheduled = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_profileRequestScheduled) return;
    final profileProvider = Provider.of<ProfileProvider?>(context, listen: false);
    if (profileProvider != null &&
        profileProvider.state == ProfileState.idle &&
        profileProvider.profile == null) {
      _profileRequestScheduled = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _profileRequestScheduled = false;
        if (!mounted) return;
        if (profileProvider.state == ProfileState.idle &&
            profileProvider.profile == null) {
          profileProvider.fetchProfile();
        }
      });
    }
  }

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
                Expanded(child: widget.child),
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
      if (location.startsWith('/allocation-strategy')) return 3;
      if (location.startsWith('/graham-recommendations')) return 4;
      if (location.startsWith('/upload')) return 5;
      if (location.startsWith('/metas')) return 6;
      if (location.startsWith('/dividends')) return 7;
      if (location.startsWith('/tax')) return 8;
      if (location.startsWith('/profile')) return 9;
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
            context.go('/allocation-strategy');
          case 4:
            context.go('/graham-recommendations');
          case 5:
            context.go('/upload');
          case 6:
            context.go('/metas');
          case 7:
            context.go('/dividends');
          case 8:
            context.go('/tax');
          case 9:
            context.go('/profile');
        }
      },
      leading: Padding(
        padding: const EdgeInsets.symmetric(vertical: 24.0),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Image.asset(
                'assets/images/logo.png',
                width: 32,
                height: 32,
                excludeFromSemantics: true,
              ),
            ),
            const SizedBox(width: 12),
            const Text(
              'Kerno',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 20,
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
          icon: Icon(Icons.settings_suggest_outlined),
          selectedIcon: Icon(Icons.settings_suggest),
          label: Text('Estratégia'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.recommend_outlined),
          selectedIcon: Icon(Icons.recommend),
          label: Text('Recomendações'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.cloud_upload_outlined),
          selectedIcon: Icon(Icons.cloud_upload),
          label: Text('Importação'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.flag_outlined),
          selectedIcon: Icon(Icons.flag),
          label: Text('Metas'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.calendar_month_outlined, key: Key('nav_dividends_button')),
          selectedIcon: Icon(Icons.calendar_month),
          label: Text('Proventos'),
        ),
        NavigationRailDestination(
          icon: Icon(Icons.receipt_long_outlined, key: Key('nav_tax_button')),
          selectedIcon: Icon(Icons.receipt_long),
          label: Text('Bens e Direitos'),
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
      if (location.startsWith('/allocation-strategy')) return 3;
      if (location.startsWith('/graham-recommendations')) return 4;
      if (location.startsWith('/upload')) return 5;
      if (location.startsWith('/metas')) return 6;
      if (location.startsWith('/dividends')) return 7;
      if (location.startsWith('/tax')) return 8;
      if (location.startsWith('/profile')) return 9;
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
            context.go('/allocation-strategy');
          case 4:
            context.go('/graham-recommendations');
          case 5:
            context.go('/upload');
          case 6:
            context.go('/metas');
          case 7:
            context.go('/dividends');
          case 8:
            context.go('/tax');
          case 9:
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
          icon: Icon(Icons.settings_suggest_outlined),
          label: 'Estratégia',
        ),
        NavigationDestination(
          icon: Icon(Icons.recommend_outlined),
          label: 'Aportes',
        ),
        NavigationDestination(
          icon: Icon(Icons.cloud_upload_outlined),
          label: 'Upload',
        ),
        NavigationDestination(
          icon: Icon(Icons.flag_outlined),
          label: 'Metas',
        ),
        NavigationDestination(
          icon: Icon(Icons.calendar_month_outlined),
          label: 'Proventos',
        ),
        NavigationDestination(
          icon: Icon(Icons.receipt_long_outlined),
          label: 'IRPF',
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
    final profileProvider = Provider.of<ProfileProvider?>(context);
    final profile = profileProvider?.profile;
    final isAdmin = profile?.role == 'ADMIN';

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
        child: SizedBox(
          width: double.infinity,
          child: Wrap(
            alignment: WrapAlignment.end,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 8,
            children: [
              if (profile != null) CommercialPlanBadge(profile: profile),
              if (isAdmin)
                TextButton.icon(
                  key: const Key('admin_dashboard_nav_button'),
                  onPressed: () => context.go('/admin/dashboard'),
                  icon: const Icon(
                    Icons.admin_panel_settings_outlined,
                    size: 18,
                    color: AppTheme.accentColor,
                  ),
                  label: const Text(
                    'Painel Admin',
                    style: TextStyle(
                      color: AppTheme.accentColor,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              IconButton(
                onPressed: () {},
                icon: const Icon(Icons.notifications_outlined),
              ),
              const CircleAvatar(
                radius: 16,
                backgroundColor: AppTheme.primaryColor,
                child: Icon(Icons.person, size: 20, color: Colors.white),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
