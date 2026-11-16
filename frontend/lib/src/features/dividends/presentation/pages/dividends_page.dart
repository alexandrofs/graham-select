import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../providers/dividend_provider.dart';
import '../widgets/dividend_schedule_card.dart';
import '../widgets/dividend_summary_header.dart';
import '../widgets/relevant_fact_card.dart';

class DividendsPage extends StatefulWidget {
  const DividendsPage({super.key});

  @override
  State<DividendsPage> createState() => _DividendsPageState();
}

class _DividendsPageState extends State<DividendsPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _tabController.addListener(() {
      if (!_tabController.indexIsChanging) {
        context.read<DividendProvider>().setSelectedTab(_tabController.index);
      }
    });

    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<DividendProvider>().loadData();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Consumer<DividendProvider>(
        builder: (context, provider, _) {
          return SingleChildScrollView(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildHeader(context, provider),
                const SizedBox(height: 20),
                DividendSummaryHeader(
                  estimatedTotal: provider.totalEstimatedNext30Days,
                  hasShares: provider.schedule.any((d) => (d.userShares ?? 0) > 0),
                ),
                const SizedBox(height: 24),
                _buildTabBar(),
                const SizedBox(height: 16),
                if (provider.isLoading)
                  _buildSkeleton()
                else if (provider.errorMessage != null)
                  _buildErrorState(provider)
                else
                  _buildContent(provider),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildHeader(BuildContext context, DividendProvider provider) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Agenda de Proventos & Fatos Relevantes',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'Acompanhe datas COM, pagamentos previstos e comunicados de mercado',
                style: TextStyle(
                  fontSize: 13,
                  color: Colors.white.withValues(alpha: 0.7),
                ),
              ),
            ],
          ),
        ),
        Row(
          children: [
            FilterChip(
              key: const Key('dividends_only_my_assets_toggle'),
              label: const Text('Meus Ativos'),
              selected: provider.onlyMyAssets,
              onSelected: (_) => provider.toggleOnlyMyAssets(),
              selectedColor: AppTheme.primaryColor.withValues(alpha: 0.25),
              checkmarkColor: AppTheme.primaryColor,
              labelStyle: TextStyle(
                color: provider.onlyMyAssets ? AppTheme.primaryColor : Colors.white70,
                fontWeight: provider.onlyMyAssets ? FontWeight.bold : FontWeight.normal,
                fontSize: 12,
              ),
            ),
            const SizedBox(width: 8),
            IconButton(
              key: const Key('dividends_refresh_button'),
              icon: const Icon(Icons.refresh_rounded, color: Colors.white70),
              tooltip: 'Recarregar dados',
              onPressed: () => provider.loadData(),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildTabBar() {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(12),
      ),
      child: TabBar(
        controller: _tabController,
        indicatorColor: AppTheme.primaryColor,
        indicatorSize: TabBarIndicatorSize.tab,
        labelColor: Colors.white,
        unselectedLabelColor: Colors.white60,
        tabs: const [
          Tab(
            icon: Icon(Icons.calendar_month_rounded, size: 18),
            text: 'Agenda de Proventos',
          ),
          Tab(
            icon: Icon(Icons.article_outlined, size: 18),
            text: 'Fatos Relevantes',
          ),
        ],
      ),
    );
  }

  Widget _buildContent(DividendProvider provider) {
    if (_tabController.index == 0) {
      if (provider.schedule.isEmpty) {
        return _buildEmptyState('Nenhum provento programado encontrado no momento.');
      }
      return ListView.builder(
        key: const Key('dividends_schedule_list'),
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: provider.schedule.length,
        itemBuilder: (context, index) {
          final item = provider.schedule[index];
          return DividendScheduleCard(announcement: item);
        },
      );
    } else {
      if (provider.facts.isEmpty) {
        return _buildEmptyState('Nenhum fato relevante recente encontrado.');
      }
      return ListView.builder(
        key: const Key('relevant_facts_list'),
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: provider.facts.length,
        itemBuilder: (context, index) {
          final fact = provider.facts[index];
          return RelevantFactCard(fact: fact);
        },
      );
    }
  }

  Widget _buildSkeleton() {
    return Column(
      key: const Key('dividends_skeleton'),
      children: List.generate(4, (index) {
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                Container(
                  width: 60,
                  height: 24,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: 120,
                        height: 14,
                        color: Colors.white.withValues(alpha: 0.08),
                      ),
                      const SizedBox(height: 8),
                      Container(
                        width: 200,
                        height: 12,
                        color: Colors.white.withValues(alpha: 0.05),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      }),
    );
  }

  Widget _buildEmptyState(String message) {
    return Center(
      key: const Key('dividends_empty_state'),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 48.0),
        child: Column(
          children: [
            Icon(
              Icons.event_busy_rounded,
              size: 48,
              color: Colors.white.withValues(alpha: 0.3),
            ),
            const SizedBox(height: 12),
            Text(
              message,
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.6),
                fontSize: 14,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorState(DividendProvider provider) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 36.0),
        child: Column(
          children: [
            const Icon(Icons.error_outline_rounded, size: 40, color: Colors.redAccent),
            const SizedBox(height: 12),
            Text(
              provider.errorMessage ?? 'Ocorreu um erro ao carregar os dados.',
              style: const TextStyle(color: Colors.white70),
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: () => provider.loadData(),
              icon: const Icon(Icons.refresh_rounded, size: 16),
              label: const Text('Tentar novamente'),
            ),
          ],
        ),
      ),
    );
  }
}
