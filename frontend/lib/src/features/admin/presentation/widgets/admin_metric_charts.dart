import 'package:flutter/material.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/admin_observability.dart';

class AdminActiveUsersCard extends StatelessWidget {
  final AdminUserMetrics metrics;

  const AdminActiveUsersCard({
    super.key,
    required this.metrics,
  });

  @override
  Widget build(BuildContext context) {
    final total = metrics.totalUsers > 0 ? metrics.totalUsers : 1;
    return Card(
      key: const Key('admin_active_users_card'),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: AppTheme.primaryColor.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.people_alt_outlined,
                    color: AppTheme.primaryColor,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Usuários Ativos (24h)',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '${metrics.activeUsers24h}',
                  style: const TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(width: 8),
                Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Text(
                    'de ${metrics.totalUsers} cadastrados',
                    style: TextStyle(
                      fontSize: 13,
                      color: Colors.white.withValues(alpha: 0.7),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            _TierBarRow(
              label: 'PREMIUM',
              count: metrics.premiumUsers,
              fraction: (metrics.premiumUsers / total).clamp(0.0, 1.0),
              color: const Color(0xFF10B981),
            ),
            const SizedBox(height: 10),
            _TierBarRow(
              label: 'TRIAL',
              count: metrics.trialUsers,
              fraction: (metrics.trialUsers / total).clamp(0.0, 1.0),
              color: const Color(0xFF0D9488),
            ),
            const SizedBox(height: 10),
            _TierBarRow(
              label: 'FREE',
              count: metrics.freeUsers,
              fraction: (metrics.freeUsers / total).clamp(0.0, 1.0),
              color: const Color(0xFF6366F1),
            ),
          ],
        ),
      ),
    );
  }
}

class _TierBarRow extends StatelessWidget {
  final String label;
  final int count;
  final double fraction;
  final Color color;

  const _TierBarRow({
    required this.label,
    required this.count,
    required this.fraction,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Colors.white.withValues(alpha: 0.85),
              ),
            ),
            Text(
              '$count',
              style: TextStyle(
                fontSize: 12,
                color: Colors.white.withValues(alpha: 0.75),
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: Container(
            height: 8,
            width: double.infinity,
            color: Colors.white.withValues(alpha: 0.08),
            alignment: Alignment.centerLeft,
            child: FractionallySizedBox(
              widthFactor: fraction > 0 ? fraction.clamp(0.05, 1.0) : 0.0,
              child: Container(
                height: 8,
                color: color,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class AdminUploadMetricsCard extends StatelessWidget {
  final AdminUploadMetrics metrics;

  const AdminUploadMetricsCard({
    super.key,
    required this.metrics,
  });

  @override
  Widget build(BuildContext context) {
    final total = metrics.totalUploads > 0 ? metrics.totalUploads : 1;
    final errorRateText =
        '${metrics.errorRatePercentage.toStringAsFixed(2)}% falha';

    return Card(
      key: const Key('admin_upload_metrics_card'),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: const Color(0xFF0D9488).withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.cloud_sync_outlined,
                    color: Color(0xFF0D9488),
                    size: 22,
                  ),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Sucesso vs Erro de Upload (Kafka)',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: (metrics.errorRatePercentage > 10
                            ? const Color(0xFFEF4444)
                            : const Color(0xFF10B981))
                        .withValues(alpha: 0.18),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    errorRateText,
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: metrics.errorRatePercentage > 10
                          ? const Color(0xFFEF4444)
                          : const Color(0xFF10B981),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '${metrics.totalUploads}',
                  style: const TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(width: 8),
                Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Text(
                    'importações B3 deduplicadas',
                    style: TextStyle(
                      fontSize: 13,
                      color: Colors.white.withValues(alpha: 0.7),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: SizedBox(
                height: 14,
                child: Row(
                  children: [
                    if (metrics.successCount > 0)
                      Expanded(
                        flex: metrics.successCount,
                        child: Container(color: const Color(0xFF10B981)),
                      ),
                    if (metrics.partialCount > 0)
                      Expanded(
                        flex: metrics.partialCount,
                        child: Container(color: const Color(0xFFF59E0B)),
                      ),
                    if (metrics.errorCount > 0)
                      Expanded(
                        flex: metrics.errorCount,
                        child: Container(color: const Color(0xFFEF4444)),
                      ),
                    if (metrics.processingCount > 0)
                      Expanded(
                        flex: metrics.processingCount,
                        child: Container(color: const Color(0xFF0D9488)),
                      ),
                    if (metrics.totalUploads == 0)
                      Expanded(
                        child: Container(
                          color: Colors.white.withValues(alpha: 0.1),
                        ),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 16,
              runSpacing: 8,
              children: [
                _UploadLegendItem(
                  label: 'Sucesso',
                  count: metrics.successCount,
                  percentage: (metrics.successCount * 100 / total),
                  color: const Color(0xFF10B981),
                ),
                _UploadLegendItem(
                  label: 'Parcial',
                  count: metrics.partialCount,
                  percentage: (metrics.partialCount * 100 / total),
                  color: const Color(0xFFF59E0B),
                ),
                _UploadLegendItem(
                  label: 'Erro',
                  count: metrics.errorCount,
                  percentage: (metrics.errorCount * 100 / total),
                  color: const Color(0xFFEF4444),
                ),
                _UploadLegendItem(
                  label: 'Processando',
                  count: metrics.processingCount,
                  percentage: (metrics.processingCount * 100 / total),
                  color: const Color(0xFF0D9488),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _UploadLegendItem extends StatelessWidget {
  final String label;
  final int count;
  final double percentage;
  final Color color;

  const _UploadLegendItem({
    required this.label,
    required this.count,
    required this.percentage,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 10,
          height: 10,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 6),
        Text(
          '$label: $count (${percentage.toStringAsFixed(0)}%)',
          style: TextStyle(
            fontSize: 12,
            color: Colors.white.withValues(alpha: 0.8),
          ),
        ),
      ],
    );
  }
}

class AdminGrahamLatencyCard extends StatelessWidget {
  final AdminGrahamLatencyMetrics metrics;

  const AdminGrahamLatencyCard({
    super.key,
    required this.metrics,
  });

  @override
  Widget build(BuildContext context) {
    final slaBadgeText = metrics.withinSla
        ? 'SLA OK (< ${metrics.slaTargetMs}ms)'
        : 'Acima do SLA';
    final slaColor =
        metrics.withinSla ? const Color(0xFF10B981) : const Color(0xFFEF4444);

    final maxPoint = metrics.latencyHistory.isEmpty
        ? metrics.slaTargetMs.toDouble()
        : metrics.latencyHistory
            .map((e) => e.latencyMs)
            .reduce((a, b) => a > b ? a : b);
    final chartMax =
        (maxPoint > metrics.slaTargetMs ? maxPoint : metrics.slaTargetMs)
            .clamp(1.0, 100000.0);

    return Card(
      key: const Key('admin_graham_latency_card'),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: AppTheme.secondaryColor.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.speed_outlined,
                    color: AppTheme.secondaryColor,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Latência Média do Motor Graham',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: slaColor.withValues(alpha: 0.18),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    slaBadgeText,
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: slaColor,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${metrics.averageLatencyMs.toStringAsFixed(0)} ms',
                        style: const TextStyle(
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                      Text(
                        'Média atual',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.7),
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${metrics.p95LatencyMs.toStringAsFixed(0)} ms',
                        style: const TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFF0D9488),
                        ),
                      ),
                      Text(
                        'P95 (Meta < ${metrics.slaTargetMs} ms)',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.7),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            SizedBox(
              height: 90,
              child: metrics.latencyHistory.isEmpty
                  ? Center(
                      child: Text(
                        'Sem histórico recente de latência',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.6),
                        ),
                      ),
                    )
                  : Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: metrics.latencyHistory.map((point) {
                  final heightFactor =
                      (point.latencyMs / chartMax).clamp(0.08, 1.0);
                  final isAboveSla = point.latencyMs > metrics.slaTargetMs;
                  return Expanded(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Text(
                            '${point.latencyMs.toStringAsFixed(0)}ms',
                            style: TextStyle(
                              fontSize: 10,
                              color: Colors.white.withValues(alpha: 0.65),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Expanded(
                            child: Align(
                              alignment: Alignment.bottomCenter,
                              child: FractionallySizedBox(
                                heightFactor: heightFactor,
                                widthFactor: 0.7,
                                child: Container(
                                  decoration: BoxDecoration(
                                    color: isAboveSla
                                        ? const Color(0xFFEF4444)
                                        : const Color(0xFF0D9488),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            point.label,
                            style: TextStyle(
                              fontSize: 10,
                              color: Colors.white.withValues(alpha: 0.7),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
