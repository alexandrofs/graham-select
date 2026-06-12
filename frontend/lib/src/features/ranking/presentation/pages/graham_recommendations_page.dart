import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/graham_recommendation_provider.dart';
import '../../domain/entities/graham_recommendation.dart';

class GrahamRecommendationsPage extends StatefulWidget {
  const GrahamRecommendationsPage({super.key});

  @override
  State<GrahamRecommendationsPage> createState() => _GrahamRecommendationsPageState();
}

class _GrahamRecommendationsPageState extends State<GrahamRecommendationsPage>
    with SingleTickerProviderStateMixin {
  late final AnimationController _shimmerController;
  late final Animation<double> _shimmerAnimation;

  @override
  void initState() {
    super.initState();
    _shimmerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();
    _shimmerAnimation = Tween<double>(begin: -1.5, end: 2.5).animate(
      CurvedAnimation(parent: _shimmerController, curve: Curves.easeInOut),
    );

    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<GrahamRecommendationProvider>().fetchRecommendations();
    });
  }

  @override
  void dispose() {
    _shimmerController.dispose();
    super.dispose();
  }

  Widget _shimmerBlock({double width = 60, double height = 16}) {
    return AnimatedBuilder(
      animation: _shimmerAnimation,
      builder: (context, _) {
        final t = ((_shimmerAnimation.value + 1.5) / 4.0).clamp(0.0, 1.0);
        final opacity = 0.05 + 0.15 * (1 - (2 * t - 1).abs());
        return Container(
          width: width,
          height: height,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(8),
            color: Colors.white.withOpacity(opacity),
          ),
        );
      },
    );
  }

  Widget _buildSkeletonLoader() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _shimmerBlock(width: 300, height: 32),
          const SizedBox(height: 12),
          _shimmerBlock(width: double.infinity, height: 18),
          const SizedBox(height: 32),
          ...List.generate(3, (_) => Card(
                margin: const EdgeInsets.only(bottom: 16),
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          _shimmerBlock(width: 80, height: 24),
                          _shimmerBlock(width: 60, height: 24),
                        ],
                      ),
                      const SizedBox(height: 16),
                      _shimmerBlock(width: double.infinity, height: 14),
                      const SizedBox(height: 8),
                      _shimmerBlock(width: 150, height: 14),
                    ],
                  ),
                ),
              )),
        ],
      ),
    );
  }

  Widget _buildPremiumPaywall() {
    return Center(
      child: Container(
        constraints: const BoxConstraints(maxWidth: 600),
        margin: const EdgeInsets.all(24.0),
        child: Card(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(32.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.workspace_premium_outlined,
                    size: 64,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  'Motor de Recomendação Graham (Premium)',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 16),
                Text(
                  'O cruzamento do algoritmo de Graham com as suas metas de alocação e posições em carteira é uma funcionalidade exclusiva do plano Premium.',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                      ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                const Divider(),
                const SizedBox(height: 16),
                _buildBenefitRow(Icons.check_circle_outline, 'Lista de oportunidades de compra ordenadas por score'),
                _buildBenefitRow(Icons.check_circle_outline, 'Comparativo visual de preço de mercado vs valor intrínseco'),
                _buildBenefitRow(Icons.check_circle_outline, 'Mapeamento de alocação atual vs meta com gap de alocação'),
                const SizedBox(height: 32),
                ElevatedButton(
                  onPressed: () {
                    Navigator.of(context).pushNamed('/profile');
                  },
                  child: const Text('Fazer Upgrade na Página de Perfil'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBenefitRow(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        children: [
          Icon(icon, size: 20, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(fontSize: 14),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState(String message) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.red),
            const SizedBox(height: 16),
            Text(
              'Falha ao carregar recomendações:',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () => context.read<GrahamRecommendationProvider>().fetchRecommendations(),
              icon: const Icon(Icons.refresh),
              label: const Text('Recarregar'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(200, 56),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRecommendationCard(GrahamRecommendation rec) {
    final double margin = rec.marginOfSafety * 100;
    final bool isPositive = margin > 0;
    final double target = rec.targetAllocationPct;
    final double current = rec.currentAllocationPct;

    // Calcular progresso de alocação para a barra (de 0.0 a 1.0)
    double allocationProgress = 0.0;
    if (target > 0) {
      allocationProgress = (current / target).clamp(0.0, 1.0);
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Text(
                      rec.ticker,
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        'Score: ${rec.recommendationScore.toStringAsFixed(2)}',
                        style: TextStyle(
                          fontSize: 12,
                          color: Theme.of(context).colorScheme.primary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: isPositive ? Colors.green.withOpacity(0.1) : Colors.red.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    '${isPositive ? '+' : ''}${margin.toStringAsFixed(1)}% Margem',
                    style: TextStyle(
                      fontSize: 12,
                      color: isPositive ? Colors.green : Colors.red,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Preço Atual',
                        style: TextStyle(fontSize: 12, color: Colors.grey),
                      ),
                      Text(
                        'R\$ ${rec.currentPrice.toStringAsFixed(2)}',
                        style: const TextStyle(fontWeight: FontWeight.w500),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Valor Intrínseco',
                        style: TextStyle(fontSize: 12, color: Colors.grey),
                      ),
                      Text(
                        'R\$ ${rec.intrinsicValue.toStringAsFixed(2)}',
                        style: const TextStyle(fontWeight: FontWeight.w500),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Alocação na Carteira (Atual vs Meta)',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
                ),
                Text(
                  '${current.toStringAsFixed(1)}% / ${target.toStringAsFixed(1)}%',
                  style: const TextStyle(fontSize: 12),
                ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: allocationProgress,
              backgroundColor: Theme.of(context).colorScheme.surfaceVariant,
              valueColor: AlwaysStoppedAnimation<Color>(
                allocationProgress >= 1.0 ? Colors.green : Theme.of(context).colorScheme.primary,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  rec.allocationGap > 0
                      ? 'Falta ${rec.allocationGap.toStringAsFixed(1)}% para a meta'
                      : rec.allocationGap < 0
                          ? 'Sobre-alocado em ${(-rec.allocationGap).toStringAsFixed(1)}%'
                          : 'Meta atingida',
                  style: TextStyle(
                    fontSize: 11,
                    color: rec.allocationGap > 0 ? Theme.of(context).colorScheme.primary : Colors.grey,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.list_alt_outlined, size: 64, color: Colors.grey),
            const SizedBox(height: 16),
            Text(
              'Nenhuma recomendação disponível',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            const Text(
              'Certifique-se de ter cotações sincronizadas e metas de alocação definidas na página de alocação.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () => context.read<GrahamRecommendationProvider>().fetchRecommendations(),
              icon: const Icon(Icons.refresh),
              label: const Text('Recarregar'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(GrahamRecommendationProvider provider) {
    if (provider.recommendations.isEmpty) {
      return _buildEmptyState();
    }

    return ListView.builder(
      padding: const EdgeInsets.all(24),
      itemCount: provider.recommendations.length,
      itemBuilder: (context, index) {
        return _buildRecommendationCard(provider.recommendations[index]);
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('Motor do Filtro de Graham'),
        actions: [
          Consumer<GrahamRecommendationProvider>(
            builder: (context, provider, _) {
              if (provider.state == RecommendationState.loading || provider.isTriggering) {
                return const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 16.0),
                  child: SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                );
              }

              return IconButton(
                icon: const Icon(Icons.refresh),
                onPressed: () => provider.fetchRecommendations(),
              );
            },
          ),
        ],
      ),
      body: Consumer<GrahamRecommendationProvider>(
        builder: (context, provider, _) {
          if (provider.state == RecommendationState.loading) {
            return _buildSkeletonLoader();
          }

          if (provider.state == RecommendationState.error) {
            final err = provider.errorMessage ?? '';
            if (err.contains('403') || err.contains('Premium') || err.contains('Upgrade')) {
              return _buildPremiumPaywall();
            }
            return _buildErrorState(err);
          }

          return _buildContent(provider);
        },
      ),
      bottomNavigationBar: Consumer<GrahamRecommendationProvider>(
        builder: (context, provider, _) {
          // Só exibir o botão de trigger se não estiver em estado de erro 403
          final err = provider.errorMessage ?? '';
          if (err.contains('403') || err.contains('Premium') || err.contains('Upgrade')) {
            return const SizedBox.shrink();
          }

          return Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surface,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  blurRadius: 4,
                  offset: const Offset(0, -2),
                ),
              ],
            ),
            child: FilledButton(
              onPressed: provider.isTriggering
                  ? null
                  : () {
                      provider.triggerCalculation();
                    },
              style: FilledButton.styleFrom(
                minimumSize: const Size(double.infinity, 56),
              ),
              child: provider.isTriggering
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                      ),
                    )
                  : const Text('Atualizar Recomendações (Filtro de Graham)'),
            ),
          );
        },
      ),
    );
  }
}
