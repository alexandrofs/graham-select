import 'package:flutter/material.dart';
import '../../../../core/theme/app_theme.dart';

class TierComparisonCard extends StatelessWidget {
  const TierComparisonCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Diferenciais da Assinatura',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 24),
            _buildTierColumn(
              context,
              'GRATUITO',
              'Acesso básico ao ranking diário. Ideal para quem está começando.',
              [
                'Visualização do Top 20',
                'Ranking atualizado 1x/dia',
                'Dados fundamentalistas básicos',
              ],
              const Color(0xFF64748B),
            ),
            const Divider(height: 48),
            _buildTierColumn(
              context,
              'TRIAL (30 dias)',
              'Experimente todo o poder do Graham Select sem custo.',
              [
                'Acesso Full ao Ranking',
                'Atualização em tempo real (3x/dia)',
                'Filtros avançados de Graham',
                'Exportação de dados',
              ],
              const Color(0xFFD97706),
            ),
            const Divider(height: 48),
            _buildTierColumn(
              context,
              'PREMIUM',
              'O cérebro insubstituível para sua gestão de patrimônio.',
              [
                'Tudo do Trial +',
                'Suporte prioritário',
                'Análise setorial detalhada',
                'Alertas de Margem de Segurança',
              ],
              const Color(0xFF10B981),
              isHighlighted: true,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTierColumn(
    BuildContext context,
    String title,
    String description,
    List<String> features,
    Color color, {
    bool isHighlighted = false,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: color,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 12),
            Text(
              title,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                color: color,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Text(
          description,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: AppTheme.textColor.withValues(alpha: 0.7),
          ),
        ),
        const SizedBox(height: 16),
        ...features.map((feature) => Padding(
          padding: const EdgeInsets.only(bottom: 8.0),
          child: Row(
            children: [
              const Icon(Icons.check_circle_outline, size: 16, color: AppTheme.accentColor),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  feature,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            ],
          ),
        )),
      ],
    );
  }
}
