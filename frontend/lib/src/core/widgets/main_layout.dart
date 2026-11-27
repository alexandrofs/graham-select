import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../features/founders/presentation/widgets/commercial_plan_badge.dart';
import '../../features/profile/presentation/providers/profile_provider.dart';
import '../config/feature_flags.dart';
import '../theme/kerno_tokens.dart';
import 'skip_to_content_link.dart';

class _NavItem {
  final String route;
  final String label;
  final IconData icon;
  final IconData selectedIcon;
  final Key? key;
  final int? branchIndex;

  const _NavItem({
    required this.route,
    required this.label,
    required this.icon,
    required this.selectedIcon,
    this.key,
    this.branchIndex,
  });
}

class _NavGroup {
  final String title;
  final List<_NavItem> items;

  const _NavGroup({
    required this.title,
    required this.items,
  });
}

List<_NavItem> _buildPrimaryItems() {
  final items = <_NavItem>[
    const _NavItem(
      route: '/dashboard',
      label: 'Carteira',
      icon: Icons.account_balance_wallet_outlined,
      selectedIcon: Icons.account_balance_wallet,
      branchIndex: 0,
    ),
    const _NavItem(
      route: '/gps-aporte',
      label: 'GPS de Aporte',
      icon: Icons.explore_outlined,
      selectedIcon: Icons.explore,
      branchIndex: 1,
    ),
    const _NavItem(
      route: '/kerno-ai',
      label: 'Kerno AI',
      icon: Icons.auto_awesome_outlined,
      selectedIcon: Icons.auto_awesome,
      branchIndex: 2,
    ),
    const _NavItem(
      route: '/allocation-strategy',
      label: 'Diagrama',
      icon: Icons.pie_chart_outline_rounded,
      selectedIcon: Icons.pie_chart_rounded,
      branchIndex: 3,
    ),
  ];

  if (FeatureFlags.enableExpensesModule) {
    items.add(
      const _NavItem(
        route: '/expenses',
        label: 'Gastos',
        icon: Icons.receipt_outlined,
        selectedIcon: Icons.receipt,
        branchIndex: 4,
      ),
    );
  }

  return items;
}

class MainLayout extends StatefulWidget {
  final Widget? child;
  final StatefulNavigationShell? navigationShell;

  const MainLayout({
    super.key,
    this.child,
    this.navigationShell,
  }) : assert(
         child != null || navigationShell != null,
         'MainLayout requer child ou navigationShell.',
       );

  @override
  State<MainLayout> createState() => _MainLayoutState();
}

class _MainLayoutState extends State<MainLayout> {
  static const double _mobileBreakpoint = 600.0;
  static const double _desktopBreakpoint = 1024.0;

  bool _profileRequestScheduled = false;
  late final FocusNode _mainContentFocusNode;

  @override
  void initState() {
    super.initState();
    _mainContentFocusNode = FocusNode(debugLabel: 'MainContent');
  }

  @override
  void dispose() {
    _mainContentFocusNode.dispose();
    super.dispose();
  }

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

  void _navigateToRoute(BuildContext context, _NavItem item) {
    final shell = widget.navigationShell;
    if (shell != null && item.branchIndex != null) {
      shell.goBranch(
        item.branchIndex!,
        initialLocation: item.branchIndex == shell.currentIndex,
      );
      return;
    }
    context.go(item.route);
  }

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final width = MediaQuery.sizeOf(context).width;
    final isMobile = width < _mobileBreakpoint;
    final isTablet = width >= _mobileBreakpoint && width < _desktopBreakpoint;
    final isDesktop = width >= _desktopBreakpoint;
    final primaryItems = _buildPrimaryItems();
    final Widget? content = widget.navigationShell ?? widget.child;

    final mainColumn = Column(
      children: <Widget>[
        _TopBar(isCompact: isMobile),
        Expanded(
          child: Focus(
            focusNode: _mainContentFocusNode,
            skipTraversal: true,
            child: content ?? const SizedBox.shrink(),
          ),
        ),
      ],
    );

    final body = isDesktop
        ? FocusTraversalGroup(
            policy: OrderedTraversalPolicy(),
            child: Stack(
              children: <Widget>[
                Row(
                  children: <Widget>[
                    FocusTraversalOrder(
                      order: const NumericFocusOrder(1),
                      child: _DesktopSidebar(
                        onSelectItem: (item) =>
                            _navigateToRoute(context, item),
                      ),
                    ),
                    Expanded(
                      child: FocusTraversalOrder(
                        order: const NumericFocusOrder(2),
                        child: mainColumn,
                      ),
                    ),
                  ],
                ),
                Positioned(
                  top: KernoSpacing.s2,
                  left: KernoSpacing.s2,
                  child: FocusTraversalOrder(
                    order: const NumericFocusOrder(0),
                    child: SkipToContentLink(
                      contentFocusNode: _mainContentFocusNode,
                    ),
                  ),
                ),
              ],
            ),
          )
        : Row(
            children: <Widget>[
              if (isTablet)
                _TabletNavigationRail(
                  items: primaryItems,
                  navigationShell: widget.navigationShell,
                  onSelectItem: (item) => _navigateToRoute(context, item),
                ),
              Expanded(child: mainColumn),
            ],
          );

    return Scaffold(
      backgroundColor: tokens.surfaceBase,
      body: body,
      bottomNavigationBar: isMobile
          ? _BottomNavBar(
              items: primaryItems,
              navigationShell: widget.navigationShell,
              onSelectItem: (item) => _navigateToRoute(context, item),
            )
          : null,
    );
  }
}

int _resolvePrimaryIndex(
  String location,
  List<_NavItem> items,
  StatefulNavigationShell? shell,
) {
  if (location.startsWith('/gps-aporte') ||
      location.startsWith('/graham-recommendations')) {
    final idx = items.indexWhere((i) => i.route == '/gps-aporte');
    if (idx >= 0) return idx;
  }
  for (var i = 0; i < items.length; i++) {
    if (location.startsWith(items[i].route)) {
      return i;
    }
  }
  if (shell != null && shell.currentIndex < items.length) {
    return shell.currentIndex;
  }
  return 0;
}

class _TabletNavigationRail extends StatelessWidget {
  final List<_NavItem> items;
  final StatefulNavigationShell? navigationShell;
  final ValueChanged<_NavItem> onSelectItem;

  const _TabletNavigationRail({
    required this.items,
    required this.navigationShell,
    required this.onSelectItem,
  });

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final location = GoRouterState.of(context).matchedLocation;
    final selectedIndex = _resolvePrimaryIndex(location, items, navigationShell);
    final textScale = MediaQuery.textScalerOf(context).scale(1.0);

    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight),
            child: IntrinsicHeight(
              child: NavigationRail(
                minWidth: 72,
                backgroundColor: tokens.surfaceElevated,
                indicatorColor: tokens.brandAccentSoft,
                selectedIndex: selectedIndex,
                labelType: textScale > 1.35
                    ? NavigationRailLabelType.none
                    : NavigationRailLabelType.selected,
                onDestinationSelected: (index) => onSelectItem(items[index]),
                leading: Padding(
                  padding: const EdgeInsets.symmetric(vertical: KernoSpacing.s4),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(KernoRadius.sm),
                    child: Image.asset(
                      'assets/images/logo.png',
                      width: 32,
                      height: 32,
                      excludeFromSemantics: true,
                    ),
                  ),
                ),
                destinations: items
                    .map(
                      (item) => NavigationRailDestination(
                        icon: Tooltip(
                          message: item.label,
                          child: Icon(item.icon, color: tokens.textSecondary),
                        ),
                        selectedIcon: Tooltip(
                          message: item.label,
                          child: Icon(
                            item.selectedIcon,
                            color: tokens.brandPrimary,
                          ),
                        ),
                        label: Text(
                          item.label,
                          style: KernoTypography.caption(
                            color: tokens.textPrimary,
                          ),
                        ),
                      ),
                    )
                    .toList(growable: false),
              ),
            ),
          ),
        );
      },
    );
  }
}

class _DesktopSidebar extends StatelessWidget {
  final ValueChanged<_NavItem> onSelectItem;

  const _DesktopSidebar({required this.onSelectItem});

  List<_NavGroup> _buildGroups({required bool isAdmin}) {
    return <_NavGroup>[
      _NavGroup(
        title: 'Patrimônio',
        items: <_NavItem>[
          const _NavItem(
            route: '/dashboard',
            label: 'Carteira',
            icon: Icons.account_balance_wallet_outlined,
            selectedIcon: Icons.account_balance_wallet,
            branchIndex: 0,
          ),
          const _NavItem(
            route: '/evolution',
            label: 'Evolução',
            icon: Icons.show_chart_rounded,
            selectedIcon: Icons.show_chart_rounded,
          ),
          const _NavItem(
            route: '/dividends',
            label: 'Proventos',
            icon: Icons.calendar_month_outlined,
            selectedIcon: Icons.calendar_month,
            key: Key('nav_dividends_button'),
          ),
          const _NavItem(
            route: '/tax',
            label: 'Bens e Direitos',
            icon: Icons.receipt_long_outlined,
            selectedIcon: Icons.receipt_long,
            key: Key('nav_tax_button'),
          ),
          if (FeatureFlags.enableExpensesModule)
            const _NavItem(
              route: '/expenses',
              label: 'Gastos',
              icon: Icons.receipt_outlined,
              selectedIcon: Icons.receipt,
              branchIndex: 4,
            ),
        ],
      ),
      const _NavGroup(
        title: 'Decisão & IA',
        items: <_NavItem>[
          _NavItem(
            route: '/gps-aporte',
            label: 'GPS de Aporte',
            icon: Icons.explore_outlined,
            selectedIcon: Icons.explore,
            branchIndex: 1,
          ),
          _NavItem(
            route: '/kerno-ai',
            label: 'Kerno AI',
            icon: Icons.auto_awesome_outlined,
            selectedIcon: Icons.auto_awesome,
            branchIndex: 2,
          ),
          _NavItem(
            route: '/allocation-strategy',
            label: 'Diagrama',
            icon: Icons.pie_chart_outline_rounded,
            selectedIcon: Icons.pie_chart_rounded,
            branchIndex: 3,
          ),
          _NavItem(
            route: '/ranking',
            label: 'Ranking',
            icon: Icons.star_outline,
            selectedIcon: Icons.star,
          ),
        ],
      ),
      const _NavGroup(
        title: 'Planejamento & Dados',
        items: <_NavItem>[
          _NavItem(
            route: '/metas',
            label: 'Metas',
            icon: Icons.flag_outlined,
            selectedIcon: Icons.flag,
          ),
          _NavItem(
            route: '/upload',
            label: 'Importação',
            icon: Icons.cloud_upload_outlined,
            selectedIcon: Icons.cloud_upload,
          ),
          _NavItem(
            route: '/tags',
            label: 'Tags',
            icon: Icons.label_outline,
            selectedIcon: Icons.label,
          ),
        ],
      ),
      const _NavGroup(
        title: 'Conta & Alertas',
        items: <_NavItem>[
          _NavItem(
            route: '/profile',
            label: 'Meu Perfil',
            icon: Icons.person_outline,
            selectedIcon: Icons.person,
          ),
          _NavItem(
            route: '/suitability',
            label: 'Perfil de Investidor',
            icon: Icons.tune_outlined,
            selectedIcon: Icons.tune,
          ),
          _NavItem(
            route: '/family-profiles',
            label: 'Holding Familiar',
            icon: Icons.family_restroom_outlined,
            selectedIcon: Icons.family_restroom,
          ),
          _NavItem(
            route: '/notifications/whatsapp',
            label: 'Alertas WhatsApp',
            icon: Icons.chat_bubble_outline,
            selectedIcon: Icons.chat_bubble,
          ),
          _NavItem(
            route: '/notifications/web-push',
            label: 'Alertas Web Push',
            icon: Icons.notifications_active_outlined,
            selectedIcon: Icons.notifications_active,
          ),
        ],
      ),
      if (isAdmin)
        const _NavGroup(
          title: 'Administração',
          items: <_NavItem>[
            _NavItem(
              route: '/admin/dashboard',
              label: 'Painel Admin',
              icon: Icons.admin_panel_settings_outlined,
              selectedIcon: Icons.admin_panel_settings,
            ),
            _NavItem(
              route: '/admin/dlq',
              label: 'Filas DLQ',
              icon: Icons.warning_amber_outlined,
              selectedIcon: Icons.warning_amber,
            ),
            _NavItem(
              route: '/admin/tickers',
              label: 'Tickers',
              icon: Icons.analytics_outlined,
              selectedIcon: Icons.analytics,
            ),
          ],
        ),
    ];
  }

  bool _isRouteSelected(String location, String route) {
    if (route == '/gps-aporte' &&
        (location.startsWith('/gps-aporte') ||
            location.startsWith('/graham-recommendations'))) {
      return true;
    }
    return location.startsWith(route);
  }

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final location = GoRouterState.of(context).matchedLocation;
    final profile = Provider.of<ProfileProvider?>(context)?.profile;
    final isAdmin = profile?.role == 'ADMIN';
    final groups = _buildGroups(isAdmin: isAdmin);

    return Container(
      width: 248,
      decoration: BoxDecoration(
        color: tokens.surfaceElevated,
        border: Border(
          right: BorderSide(color: tokens.borderSubtle),
        ),
      ),
      child: Column(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: KernoSpacing.s5,
              vertical: KernoSpacing.s5,
            ),
            child: Row(
              children: <Widget>[
                ClipRRect(
                  borderRadius: BorderRadius.circular(KernoRadius.sm),
                  child: Image.asset(
                    'assets/images/logo.png',
                    width: 32,
                    height: 32,
                    excludeFromSemantics: true,
                  ),
                ),
                const SizedBox(width: KernoSpacing.s3),
                Flexible(
                  child: Text(
                    'Kerno',
                    style: KernoTypography.heading2(color: tokens.textPrimary),
                  ),
                ),
              ],
            ),
          ),
          Divider(height: 1, color: tokens.borderSubtle),
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(
                horizontal: KernoSpacing.s3,
                vertical: KernoSpacing.s3,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: groups.map((group) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: KernoSpacing.s4),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: KernoSpacing.s3,
                            vertical: KernoSpacing.s1,
                          ),
                          child: Text(
                            group.title.toUpperCase(),
                            style: KernoTypography.caption(
                              color: tokens.textMuted,
                            ).copyWith(fontWeight: FontWeight.w700),
                          ),
                        ),
                        ...group.items.map((item) {
                          final selected =
                              _isRouteSelected(location, item.route);
                          return Padding(
                            padding: const EdgeInsets.only(top: KernoSpacing.s1),
                            child: Semantics(
                              selected: selected,
                              button: true,
                              child: Material(
                                color: Colors.transparent,
                                child: InkWell(
                                  borderRadius:
                                      BorderRadius.circular(KernoRadius.md),
                                  onTap: () => onSelectItem(item),
                                  child: Ink(
                                    decoration: BoxDecoration(
                                      color: selected
                                          ? tokens.brandAccentSoft
                                          : Colors.transparent,
                                      borderRadius:
                                          BorderRadius.circular(KernoRadius.md),
                                    ),
                                    child: Container(
                                      constraints: const BoxConstraints(
                                        minHeight: KernoSpacing.touchTargetMin,
                                      ),
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: KernoSpacing.s3,
                                        vertical: KernoSpacing.s2,
                                      ),
                                      child: Row(
                                        children: <Widget>[
                                          Icon(
                                            selected
                                                ? item.selectedIcon
                                                : item.icon,
                                            key: item.key,
                                            size: 20,
                                            color: selected
                                                ? tokens.brandPrimary
                                                : tokens.textSecondary,
                                          ),
                                          const SizedBox(width: KernoSpacing.s3),
                                          Expanded(
                                            child: Text(
                                              item.label,
                                              style: KernoTypography.bodySm(
                                                color: selected
                                                    ? tokens.brandPrimary
                                                    : tokens.textPrimary,
                                              ).copyWith(
                                                fontWeight: selected
                                                    ? FontWeight.w600
                                                    : FontWeight.w500,
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          );
                        }),
                      ],
                    ),
                  );
                }).toList(growable: false),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _BottomNavBar extends StatelessWidget {
  final List<_NavItem> items;
  final StatefulNavigationShell? navigationShell;
  final ValueChanged<_NavItem> onSelectItem;

  const _BottomNavBar({
    required this.items,
    required this.navigationShell,
    required this.onSelectItem,
  });

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final location = GoRouterState.of(context).matchedLocation;
    final selectedIndex = _resolvePrimaryIndex(location, items, navigationShell);
    final textScale = MediaQuery.textScalerOf(context).scale(1.0);
    final useLargeContentPresenter = textScale > 1.35;

    return Container(
      decoration: BoxDecoration(
        color: tokens.surfaceElevated,
        border: Border(
          top: BorderSide(color: tokens.borderSubtle),
        ),
      ),
      child: BottomNavigationBar(
        type: BottomNavigationBarType.fixed,
        backgroundColor: tokens.surfaceElevated,
        selectedItemColor: tokens.brandPrimary,
        unselectedItemColor: tokens.textSecondary,
        currentIndex: selectedIndex,
        showSelectedLabels: !useLargeContentPresenter,
        showUnselectedLabels: !useLargeContentPresenter,
        onTap: (index) => onSelectItem(items[index]),
        items: items.map((item) {
          final iconWidget = _LargeContentNavIcon(
            icon: item.icon,
            selectedIcon: item.selectedIcon,
            label: item.label,
            enableLongPressLabel: useLargeContentPresenter,
          );
          return BottomNavigationBarItem(
            icon: iconWidget,
            activeIcon: _LargeContentNavIcon(
              icon: item.selectedIcon,
              selectedIcon: item.selectedIcon,
              label: item.label,
              enableLongPressLabel: useLargeContentPresenter,
              isSelected: true,
            ),
            label: item.label,
            tooltip: item.label,
          );
        }).toList(growable: false),
      ),
    );
  }
}

/// Presenter acessível para escalas tipográficas elevadas (`textScaler > 1.35`),
/// garantindo alvo mínimo de `48x48dp`.
class _LargeContentNavIcon extends StatelessWidget {
  final IconData icon;
  final IconData selectedIcon;
  final String label;
  final bool enableLongPressLabel;
  final bool isSelected;

  const _LargeContentNavIcon({
    required this.icon,
    required this.selectedIcon,
    required this.label,
    required this.enableLongPressLabel,
    this.isSelected = false,
  });

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: const BoxConstraints(
        minWidth: KernoSpacing.touchTargetMin,
        minHeight: KernoSpacing.touchTargetMin,
      ),
      child: Center(
        child: Icon(isSelected ? selectedIcon : icon),
      ),
    );
  }
}

class _TopBar extends StatelessWidget {
  final bool isCompact;

  const _TopBar({this.isCompact = false});

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final profileProvider = Provider.of<ProfileProvider?>(context);
    final profile = profileProvider?.profile;
    final isAdmin = profile?.role == 'ADMIN';
    final textScale = MediaQuery.textScalerOf(context).scale(1.0);
    final simplifyTopBar = isCompact && textScale > 1.35;

    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: isCompact ? KernoSpacing.s3 : KernoSpacing.s6,
        vertical: simplifyTopBar ? KernoSpacing.s2 : KernoSpacing.s3,
      ),
      decoration: BoxDecoration(
        color: tokens.surfaceElevated,
        border: Border(
          bottom: BorderSide(color: tokens.borderSubtle),
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: SizedBox(
          width: double.infinity,
          child: Wrap(
            alignment: WrapAlignment.end,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: KernoSpacing.s2,
            runSpacing: KernoSpacing.s2,
            children: <Widget>[
              if (profile != null) CommercialPlanBadge(profile: profile),
              if (isAdmin)
                TextButton.icon(
                  key: const Key('admin_dashboard_nav_button'),
                  onPressed: () => context.go('/admin/dashboard'),
                  icon: Icon(
                    Icons.admin_panel_settings_outlined,
                    size: 18,
                    color: tokens.emerald,
                  ),
                  label: Text(
                    'Painel Admin',
                    style: KernoTypography.bodySm(
                      color: tokens.emerald,
                    ).copyWith(fontWeight: FontWeight.w600),
                  ),
                ),
              if (!simplifyTopBar)
                IconButton(
                  onPressed: () => context.go('/notifications/web-push'),
                  tooltip: 'Notificações',
                  icon: Icon(
                    Icons.notifications_outlined,
                    color: tokens.textPrimary,
                  ),
                ),
              IconButton(
                key: const Key('topbar_profile_button'),
                onPressed: () => context.go('/profile'),
                tooltip: 'Meu Perfil',
                icon: CircleAvatar(
                  radius: 18,
                  backgroundColor: tokens.brandPrimary,
                  child: Icon(
                    Icons.person,
                    size: 20,
                    color: tokens.textOnPrimary,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
