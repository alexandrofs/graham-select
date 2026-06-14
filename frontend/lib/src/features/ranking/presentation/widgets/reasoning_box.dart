import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../../domain/entities/graham_recommendation.dart';

class ReasoningBox extends StatelessWidget {
  final GrahamRecommendation recommendation;

  // Paleta de cores obrigatória
  static const Color emerald = Color(0xFF10B981);
  static const Color amberGold = Color(0xFFF59E0B);
  static const Color errorRed = Color(0xFFEF4444);

  const ReasoningBox({
    super.key,
    required this.recommendation,
  });

  String _formatCurrency(double value) {
    if (!value.isFinite) return 'R\$ N/A';
    return 'R\$ ${value.toStringAsFixed(2).replaceAll('.', ',')}';
  }

  String _formatPercentage(double value) {
    if (!value.isFinite) return 'N/A';
    return '${value.toStringAsFixed(2).replaceAll('.', ',')}%';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final hasMarketData = recommendation.epsUsed != null && recommendation.bvpsUsed != null;
    
    // Margem de Segurança formatada
    final mosPercent = recommendation.marginOfSafety * 100;
    final mosColor = recommendation.marginOfSafety > 0 ? amberGold : errorRed;
    final mosSign = recommendation.marginOfSafety > 0 ? '+' : '';

    // Lógica do Gap de Alocação
    final hasAllocationGoal = recommendation.targetAllocationPct > 0;
    final progress = hasAllocationGoal
        ? math.min(1.0, math.max(0.0, recommendation.currentAllocationPct / recommendation.targetAllocationPct))
        : 0.0;

    // Componentes do Score
    final mosScoreComponent = recommendation.marginOfSafety * 0.6;
    final gapNormalized = hasAllocationGoal
        ? math.max(0.0, recommendation.allocationGap) / 100.0
        : 0.0;
    final allocationScoreComponent = gapNormalized * 0.4;

    return Semantics(
      label: 'Painel de raciocínio de investimento para o ativo ${recommendation.ticker}',
      excludeSemantics: false,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Ticker + Score Badge
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                recommendation.ticker,
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: theme.colorScheme.primary,
                ),
              ),
              Semantics(
                label: 'Score de recomendação: ${recommendation.recommendationScore.toStringAsFixed(2)}',
                excludeSemantics: true,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primary.withAlpha((0.1 * 255).round()),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    'Score: ${recommendation.recommendationScore.toStringAsFixed(2)}',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: theme.colorScheme.primary,
                      fontFamily: 'JetBrainsMono',
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          const Divider(),

          // Seção: Análise de Valor de Mercado
          const SizedBox(height: 8),
          Text(
            '📊 Análise de Valor de Mercado',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: theme.colorScheme.primary,
            ),
          ),
          const SizedBox(height: 12),
          if (hasMarketData) ...[
            _buildStepRow(
              'Passo 1',
              'LPA (Lucro Por Ação):',
              _formatCurrency(recommendation.epsUsed!),
              theme,
            ),
            _buildDivider(theme),
            _buildStepRow(
              'Passo 2',
              'VPA (Valor Patrimonial por Ação):',
              _formatCurrency(recommendation.bvpsUsed!),
              theme,
            ),
            _buildDivider(theme),
            _buildStepRow(
              'Passo 3',
              'Valor Intrínseco Graham = √(22,5 × LPA × VPA) =',
              _formatCurrency(recommendation.intrinsicValue),
              theme,
              isFormula: true,
            ),
            _buildDivider(theme),
            _buildStepRow(
              'Passo 4',
              'Preço Atual:',
              _formatCurrency(recommendation.currentPrice),
              theme,
            ),
            _buildDivider(theme),
            _buildStepRow(
              'Passo 5',
              'Margem de Segurança: (Intrínseco/Preço - 1) =',
              '$mosSign${_formatPercentage(mosPercent)}',
              theme,
              valueColor: mosColor,
              isBadge: true,
            ),
          ] else ...[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: theme.colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                'Dados de LPA/VPA não disponíveis',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                  fontStyle: FontStyle.italic,
                ),
              ),
            ),
            const SizedBox(height: 12),
            _buildStepRow(
              'Preço Atual',
              'Valor atual de mercado:',
              _formatCurrency(recommendation.currentPrice),
              theme,
            ),
            _buildDivider(theme),
            _buildStepRow(
              'Margem de Segurança',
              'Margem de Segurança: (Intrínseco/Preço - 1) =',
              '$mosSign${_formatPercentage(mosPercent)}',
              theme,
              valueColor: mosColor,
              isBadge: true,
            ),
          ],
          const SizedBox(height: 12),
          Text(
            recommendation.marginOfSafety > 0
                ? 'Este ativo está ${_formatPercentage(mosPercent)} abaixo do valor intrínseco calculado pelo Filtro de Graham.'
                : 'Este ativo está ${_formatPercentage(mosPercent.abs())} acima do valor intrínseco calculado pelo Filtro de Graham.',
            style: theme.textTheme.bodyMedium,
          ),
          
          const SizedBox(height: 16),
          const Divider(),

          // Seção: Análise de Alocação
          const SizedBox(height: 8),
          Text(
            '📈 Análise de Alocação na Carteira',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: theme.colorScheme.primary,
            ),
          ),
          const SizedBox(height: 12),
          if (!hasAllocationGoal) ...[
            Text(
              'Sem meta de alocação definida para este ativo',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
                fontStyle: FontStyle.italic,
              ),
            ),
          ] else ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Semantics(
                  label: 'Alocação Atual: ${_formatPercentage(recommendation.currentAllocationPct)}',
                  excludeSemantics: true,
                  child: Text(
                    'Atual: ${_formatPercentage(recommendation.currentAllocationPct)}',
                    style: theme.textTheme.bodyMedium?.copyWith(fontFamily: 'JetBrainsMono'),
                  ),
                ),
                Semantics(
                  label: 'Meta de Alocação: ${_formatPercentage(recommendation.targetAllocationPct)}',
                  excludeSemantics: true,
                  child: Text(
                    'Meta: ${_formatPercentage(recommendation.targetAllocationPct)}',
                    style: theme.textTheme.bodyMedium?.copyWith(fontFamily: 'JetBrainsMono'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: progress,
                minHeight: 8,
                backgroundColor: theme.colorScheme.surfaceContainerHighest,
                valueColor: AlwaysStoppedAnimation<Color>(
                  progress >= 1.0 ? emerald : theme.colorScheme.primary,
                ),
              ),
            ),
            const SizedBox(height: 8),
            _buildAllocationGapText(recommendation.allocationGap, theme),
          ],

          const SizedBox(height: 16),
          const Divider(),

          // Seção: Score Composto
          const SizedBox(height: 8),
          Text(
            '🎯 Como é Calculado o Score',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: theme.colorScheme.primary,
            ),
          ),
          const SizedBox(height: 12),
          if (hasAllocationGoal) ...[
            _buildScoreRow(
              'Componente Graham (60%):',
              'marginOfSafety × 0,6',
              mosScoreComponent.toStringAsFixed(3),
              theme,
            ),
            const SizedBox(height: 6),
            _buildScoreRow(
              'Componente Alocação (40%):',
              'gap × 0,4',
              allocationScoreComponent.toStringAsFixed(3),
              theme,
            ),
          ] else ...[
            _buildScoreRow(
              'Componente Graham (100%):',
              'marginOfSafety × 0,6',
              mosScoreComponent.toStringAsFixed(3),
              theme,
            ),
          ],
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: theme.colorScheme.primaryContainer,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Score Final:',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: theme.colorScheme.onPrimaryContainer,
                  ),
                ),
                Text(
                  recommendation.recommendationScore.toStringAsFixed(2),
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: theme.colorScheme.onPrimaryContainer,
                    fontFamily: 'JetBrainsMono',
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Semantics(
            label: 'Nota explicativa: Quanto maior o score, maior a prioridade de aporte sugerida.',
            excludeSemantics: true,
            child: Text(
              'Quanto maior o score, maior a prioridade de aporte sugerida.',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.outline,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDivider(ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Divider(
        height: 1,
        thickness: 0.5,
        color: theme.colorScheme.outlineVariant.withAlpha((0.5 * 255).round()),
      ),
    );
  }

  Widget _buildStepRow(
    String step,
    String description,
    String value,
    ThemeData theme, {
    Color? valueColor,
    bool isFormula = false,
    bool isBadge = false,
  }) {
    final textStyle = TextStyle(
      fontFamily: 'JetBrainsMono',
      fontWeight: isFormula || isBadge ? FontWeight.bold : FontWeight.normal,
      color: valueColor ?? (isFormula ? theme.colorScheme.primary : theme.colorScheme.onSurface),
      fontFeatures: const [FontFeature.tabularFigures()],
    );

    Widget valueWidget = Semantics(
      label: '$description $value',
      excludeSemantics: true,
      child: Text(
        value,
        style: textStyle,
      ),
    );

    if (isBadge) {
      valueWidget = Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: (valueColor ?? amberGold).withAlpha((0.1 * 255).round()),
          borderRadius: BorderRadius.circular(4),
          border: Border.all(
            color: (valueColor ?? amberGold).withAlpha((0.3 * 255).round()),
          ),
        ),
        child: Text(
          value,
          style: textStyle.copyWith(fontSize: 13),
        ),
      );
    }

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 72,
          child: Text(
            step,
            style: theme.textTheme.bodyMedium?.copyWith(
              fontWeight: FontWeight.bold,
              color: theme.colorScheme.outline,
            ),
          ),
        ),
        Expanded(
          child: Text(
            description,
            style: theme.textTheme.bodyMedium,
          ),
        ),
        const SizedBox(width: 8),
        valueWidget,
      ],
    );
  }

  Widget _buildAllocationGapText(double gap, ThemeData theme) {
    if (gap.abs() < 0.05) {
      return Semantics(
        label: 'Meta atingida',
        excludeSemantics: true,
        child: Row(
          children: [
            const Icon(Icons.check_circle_outline, size: 14, color: emerald),
            const SizedBox(width: 4),
            Text(
              'Meta atingida ✓',
              style: theme.textTheme.bodySmall?.copyWith(color: emerald),
            ),
          ],
        ),
      );
    } else if (gap > 0) {
      return Semantics(
        label: 'Falta ${gap.toStringAsFixed(1)}% para atingir a meta',
        excludeSemantics: true,
        child: Text(
          'Falta ${gap.toStringAsFixed(1).replaceAll('.', ',')}% para atingir a meta',
          style: theme.textTheme.bodySmall?.copyWith(color: amberGold),
        ),
      );
    } else {
      final overAlloc = gap.abs();
      return Semantics(
        label: 'Sobre-alocado em ${overAlloc.toStringAsFixed(1)}%',
        excludeSemantics: true,
        child: Text(
          'Sobre-alocado em ${overAlloc.toStringAsFixed(1).replaceAll('.', ',')}%',
          style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.outline),
        ),
      );
    }
  }

  Widget _buildScoreRow(
    String label,
    String formula,
    String value,
    ThemeData theme,
  ) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold),
            ),
            Text(
              formula,
              style: theme.textTheme.labelSmall?.copyWith(color: theme.colorScheme.outline),
            ),
          ],
        ),
        Semantics(
          label: '$label calculado: $value',
          excludeSemantics: true,
          child: Text(
            value,
            style: const TextStyle(
              fontFamily: 'JetBrainsMono',
              fontFeatures: [FontFeature.tabularFigures()],
            ),
          ),
        ),
      ],
    );
  }
}
