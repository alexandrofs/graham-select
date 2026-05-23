import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../profile/presentation/providers/profile_provider.dart';
import '../../../portfolio/presentation/widgets/manual_operation_entry.dart';
import '../../../portfolio/presentation/providers/portfolio_provider.dart';
import '../../../portfolio/presentation/widgets/portfolio_kpi_card.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<ProfileProvider>().fetchProfile();
      context.read<PortfolioProvider>().loadSummary();
    });
  }

  @override
  Widget build(BuildContext context) {
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
            const SizedBox(height: 32),
            _buildKpiSection(context),
            const SizedBox(height: 32),
            // Outras seções do Epic 3 virão aqui
          ],
        ),
      ),
    );
  }

  Widget _buildKpiSection(BuildContext context) {
    return Consumer<PortfolioProvider>(
      builder: (context, provider, child) {
        if (provider.status == PortfolioStatus.error) {
          return Container(
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
                  onPressed: () => provider.loadSummary(),
                  child: const Text('Tentar novamente'),
                ),
              ],
            ),
          );
        }

        final summary = provider.summary;
        final isLoading = provider.status == PortfolioStatus.loading;

        return GridView.count(
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
      },
    );
  }
}
