import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../goals/domain/entities/goal_projection.dart';
import '../../../goals/domain/services/time_to_goal_calculator.dart';
import '../../../goals/presentation/providers/goals_provider.dart';
import '../../../goals/presentation/widgets/goal_health_checklist_card.dart';
import '../../../goals/presentation/widgets/motivational_clock_card.dart';
import '../../../profile/presentation/providers/profile_provider.dart';
import '../../../portfolio/presentation/widgets/manual_operation_entry.dart';
import '../../../portfolio/presentation/providers/portfolio_provider.dart';
import '../../../portfolio/presentation/widgets/portfolio_kpi_card.dart';
import '../../../portfolio/presentation/widgets/financial_data_table.dart';
import '../../../portfolio/presentation/widgets/asset_allocation_donut_chart.dart';
import '../../../tags/presentation/providers/tag_provider.dart';
import '../../../tags/presentation/widgets/analytical_lens_tabs.dart';
import '../../../tags/presentation/widgets/dashboard_tag_filter_bar.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage>
    with TagLensSyncMixin<DashboardPage> {
  bool _initialAccountLoaded = false;

  @override
  Future<void> onLensChanged(String? tagId) async {
    if (!mounted) return;
    final portfolioProvider = context.read<PortfolioProvider>();
    final goalsProvider = Provider.of<GoalsProvider?>(context, listen: false);
    final isInitialLoad = !_initialAccountLoaded;
    _initialAccountLoaded = true;

    if (isInitialLoad) {
      context.read<ProfileProvider>().fetchProfile();
    }
    portfolioProvider.loadCustodyPositions(tagId: tagId);
    await Future.wait([
      portfolioProvider.loadSummary(tagId: tagId),
      if (tagId != null && portfolioProvider.globalSummary == null)
        portfolioProvider.refreshGlobalSummary(),
      if (isInitialLoad && goalsProvider != null) goalsProvider.loadGoal(),
    ]);
    if (!mounted) return;
    final goalSummary = portfolioProvider.globalSummary ?? portfolioProvider.summary;
    if (isInitialLoad &&
        goalsProvider != null &&
        goalsProvider.currentGoal != null &&
        goalSummary != null) {
      goalsProvider.recalculateLocalProjection(
        currentEquity: goalSummary.totalEquity,
        grossYieldPercentage: goalSummary.grossYieldPercentage,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final goalsProvider = Provider.of<GoalsProvider?>(context);
    final tagProvider = Provider.of<TagProvider?>(context);
    return Scaffold(
      backgroundColor: Colors.transparent,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          showModalBottomSheet(
            context: context,
            isScrollControlled: true,
            backgroundColor: Colors.transparent,
            builder: (context) => const ManualOperationEntry(),
          );
        },
        icon: const Icon(Icons.add),
        label: const Text('Nova Operação'),
        backgroundColor: AppTheme.accentColor,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Consumer<PortfolioProvider>(
          builder: (context, provider, child) {
            final currentGoal = goalsProvider?.currentGoal;
            final goalSummary = provider.globalSummary ?? provider.summary;
            GoalProjection? effectiveProjection = goalsProvider?.projection;
            if (currentGoal != null && effectiveProjection == null) {
              effectiveProjection = TimeToGoalCalculator.calculate(
                goalType: currentGoal.goalType,
                targetValue: currentGoal.targetValue,
                currentEquity: goalSummary?.totalEquity ?? 0.0,
                monthlyContribution: currentGoal.monthlyContribution,
                annualYieldPercentage: goalSummary?.grossYieldPercentage ?? 0.0,
              );
            }

            final activeTag = tagProvider?.selectedFilterTag;
            final effectiveUntaggedTickers = tagProvider == null
                ? provider.untaggedTickers
                : provider.untaggedTickers
                    .where((t) => tagProvider.tagsForTicker(t).isEmpty)
                    .toList();
            final untaggedCount = provider.untaggedTickers.isNotEmpty
                ? effectiveUntaggedTickers.length
                : provider.untaggedAssetsCount;

            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Bem-vindo ao seu Dashboard',
                            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Acompanhe seu patrimônio e veja recomendações de aporte.',
                            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                  color: AppTheme.textColor.withValues(alpha: 0.7),
                                ),
                          ),
                        ],
                      ),
                    ),
                    if (provider.isReceivingLiveUpdates)
                      const Padding(
                        padding: EdgeInsets.only(left: 16.0),
                        child: LiveUpdatesBadge(),
                      ),
                  ],
                ),
                if (tagProvider != null) ...[
                  const SizedBox(height: 20),
                  const DashboardTagFilterBar(),
                  if (activeTag != null) ...[
                    const SizedBox(height: 12),
                    const ActiveTagLensBadge(),
                  ],
                  if (activeTag != null && untaggedCount > 0) ...[
                    const SizedBox(height: 12),
                    _UntaggedAssetsBanner(
                      count: untaggedCount,
                      onAssociatePressed: () {
                        if (effectiveUntaggedTickers.isEmpty) return;
                        UntaggedAssetsQuickSheet.show(
                          context,
                          untaggedTickers: effectiveUntaggedTickers,
                          tagProvider: tagProvider,
                          onTagsUpdated: () async {
                            final activeTagId = tagProvider.selectedFilterTagId;
                            await provider.refreshGlobalSummary();
                            await Future.wait([
                              provider.loadSummary(tagId: activeTagId),
                              provider.loadCustodyPositions(tagId: activeTagId),
                            ]);
                          },
                        );
                      },
                    ),
                  ],
                ],
                const SizedBox(height: 24),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: _buildKpiSection(context, provider, tagProvider),
                ),
                if (goalsProvider != null) ...[
                  const SizedBox(height: 32),
                  MotivationalClockCard(
                    goal: currentGoal,
                    projection: effectiveProjection,
                  ),
                  if (currentGoal != null) ...[
                    const SizedBox(height: 24),
                    GoalHealthChecklistCard(
                      goal: currentGoal,
                      currentEquity: goalSummary?.totalEquity ?? 0.0,
                      annualYieldPercentage: goalSummary?.grossYieldPercentage ?? 0.0,
                    ),
                  ],
                ],
                const SizedBox(height: 32),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: AssetAllocationDonutChart(
                    key: ValueKey(
                      'donut_${provider.activeTagId}_${provider.custodyStatus}_${provider.rawCustodyPositions.length}_${provider.rawCustodyPositions.fold<double>(0, (sum, p) => sum + p.marketValue)}',
                    ),
                  ),
                ),
                const SizedBox(height: 32),
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: FinancialDataTable(
                    key: ValueKey(
                      'table_${provider.activeTagId}_${provider.custodyStatus}_${provider.rawCustodyPositions.length}',
                    ),
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildKpiSection(
    BuildContext context,
    PortfolioProvider provider,
    TagProvider? tagProvider,
  ) {
    if (provider.status == PortfolioStatus.error) {
      return Container(
        key: const ValueKey('error'),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.redAccent.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.redAccent.withValues(alpha: 0.3)),
        ),
        child: Row(
          children: [
            const Icon(Icons.error_outline, color: Colors.redAccent),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                'Erro ao carregar resumo: ${provider.errorMessage ?? "Erro desconhecido"}',
                style: const TextStyle(color: Colors.redAccent),
              ),
            ),
            TextButton(
              onPressed: () => provider.loadSummary(
                tagId: tagProvider?.selectedFilterTagId,
              ),
              child: const Text('Tentar novamente'),
            ),
          ],
        ),
      );
    }

    final summary = provider.summary;
    final isLoading = provider.status == PortfolioStatus.loading;

    return GridView.count(
      key: ValueKey(
        summary != null
            ? 'kpi_${provider.activeTagId}_${summary.totalEquity}_${summary.grossYieldPercentage}_${summary.accumulatedDividends}'
            : 'loading_${provider.activeTagId}',
      ),
      crossAxisCount: MediaQuery.of(context).size.width > 1200 ? 4 : (MediaQuery.of(context).size.width > 800 ? 2 : 1),
      crossAxisSpacing: 16,
      mainAxisSpacing: 16,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      childAspectRatio: 2.5,
      children: [
        PortfolioKpiCard(
          title: 'Patrimônio Total',
          value: summary?.totalEquity ?? 0,
          isLoading: isLoading,
        ),
        PortfolioKpiCard(
          title: 'Rendimento Bruto',
          value: summary?.grossYieldPercentage ?? 0,
          isPercentage: true,
          isCurrency: false,
          isLoading: isLoading,
          valueColor: (summary?.grossYieldPercentage ?? 0) >= 0 ? const Color(0xFF10B981) : Colors.redAccent,
        ),
        PortfolioKpiCard(
          title: 'Dividendos Acumulados',
          value: summary?.accumulatedDividends ?? 0,
          isLoading: isLoading,
          valueColor: const Color(0xFFF59E0B),
        ),
        PortfolioKpiCard(
          title: 'Projeção Mensal',
          value: summary?.monthlyProjection ?? 0,
          isLoading: isLoading,
        ),
      ],
    );
  }
}

class _UntaggedAssetsBanner extends StatelessWidget {
  const _UntaggedAssetsBanner({
    required this.count,
    required this.onAssociatePressed,
  });

  final int count;
  final VoidCallback onAssociatePressed;

  @override
  Widget build(BuildContext context) {
    final message = count == 1
        ? '1 ativo sem tag não está sendo exibido'
        : '$count ativos sem tag não estão sendo exibidos';

    return Container(
      key: const Key('untagged_assets_banner'),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFF59E0B).withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFFF59E0B).withValues(alpha: 0.35),
        ),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.info_outline_rounded,
            size: 18,
            color: Color(0xFFF59E0B),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontFamily: 'Inter',
                fontSize: 13,
                color: Colors.white,
              ),
            ),
          ),
          TextButton(
            key: const Key('associate_untagged_assets_button'),
            style: TextButton.styleFrom(
              minimumSize: const Size(48, 40),
              foregroundColor: const Color(0xFFFBBF24),
            ),
            onPressed: onAssociatePressed,
            child: const Text(
              'Associar agora',
              style: TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

class LiveUpdatesBadge extends StatefulWidget {
  const LiveUpdatesBadge({super.key});

  @override
  State<LiveUpdatesBadge> createState() => _LiveUpdatesBadgeState();
}

class _LiveUpdatesBadgeState extends State<LiveUpdatesBadge> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 1),
    )..repeat(reverse: true);
    
    _animation = Tween<double>(begin: 0.3, end: 1.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFF10B981).withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: const Color(0xFF10B981).withValues(alpha: 0.3),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          AnimatedBuilder(
            animation: _animation,
            builder: (context, child) {
              return Opacity(
                opacity: _animation.value,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: Color(0xFF10B981),
                    shape: BoxShape.circle,
                  ),
                ),
              );
            },
          ),
          const SizedBox(width: 6),
          const Text(
            'Ao vivo',
            style: TextStyle(
              color: Color(0xFF10B981),
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
