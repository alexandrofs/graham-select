import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/admin_observability.dart';
import '../providers/admin_observability_provider.dart';
import '../widgets/admin_metric_charts.dart';

class AdminDashboardPage extends StatefulWidget {
  const AdminDashboardPage({super.key});

  @override
  State<AdminDashboardPage> createState() => _AdminDashboardPageState();
}

class _AdminDashboardPageState extends State<AdminDashboardPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final provider = Provider.of<AdminObservabilityProvider?>(
        context,
        listen: false,
      );
      if (provider != null &&
          provider.status == AdminObservabilityStatus.initial) {
        provider.loadMetrics();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<AdminObservabilityProvider>(
      builder: (context, provider, _) {
        switch (provider.status) {
          case AdminObservabilityStatus.initial:
          case AdminObservabilityStatus.loading:
            return const _AdminDashboardSkeleton();
          case AdminObservabilityStatus.accessDenied:
            return _AdminAccessDeniedView(
              message: provider.errorMessage ??
                  'Você não possui privilégios de administrador para visualizar este painel.',
            );
          case AdminObservabilityStatus.error:
            return _AdminErrorView(
              message: provider.errorMessage ??
                  'Não foi possível carregar as métricas de observabilidade.',
              onRetry: provider.loadMetrics,
            );
          case AdminObservabilityStatus.loaded:
            final metrics = provider.metrics;
            if (metrics == null) {
              return const _AdminDashboardSkeleton();
            }
            return _AdminDashboardContent(
              metrics: metrics,
              onRefresh: provider.loadMetrics,
            );
        }
      },
    );
  }
}

class _AdminDashboardSkeleton extends StatelessWidget {
  const _AdminDashboardSkeleton();

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      key: const Key('admin_dashboard_skeleton'),
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildPlaceholderBox(width: 280, height: 28),
          const SizedBox(height: 8),
          _buildPlaceholderBox(width: 420, height: 16),
          const SizedBox(height: 24),
          Wrap(
            spacing: 16,
            runSpacing: 16,
            children: [
              _buildPlaceholderBox(width: 340, height: 220),
              _buildPlaceholderBox(width: 340, height: 220),
              _buildPlaceholderBox(width: 340, height: 220),
            ],
          ),
          const SizedBox(height: 24),
          _buildPlaceholderBox(width: double.infinity, height: 260),
        ],
      ),
    );
  }

  Widget _buildPlaceholderBox({required double width, required double height}) {
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor.withValues(alpha: 0.7),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
      ),
    );
  }
}

class _AdminAccessDeniedView extends StatelessWidget {
  final String message;

  const _AdminAccessDeniedView({required this.message});

  @override
  Widget build(BuildContext context) {
    return Center(
      key: const Key('admin_access_denied_state'),
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(28),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: const Color(0xFFEF4444).withValues(alpha: 0.15),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.lock_person_outlined,
                      size: 40,
                      color: Color(0xFFEF4444),
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Acesso Restrito a Administradores',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    message,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.white.withValues(alpha: 0.75),
                    ),
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton.icon(
                    onPressed: () => context.go('/dashboard'),
                    icon: const Icon(Icons.arrow_back),
                    label: const Text('Voltar ao Dashboard'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _AdminErrorView extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _AdminErrorView({
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      key: const Key('admin_error_state'),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.error_outline,
              color: Color(0xFFEF4444),
              size: 48,
            ),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: 220,
              child: ElevatedButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh),
                label: const Text('Tentar Novamente'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AdminDashboardContent extends StatelessWidget {
  final AdminObservability metrics;
  final VoidCallback onRefresh;

  const _AdminDashboardContent({
    required this.metrics,
    required this.onRefresh,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Painel de Observabilidade Admin',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Métricas operacionais consolidadas e tracing anonimizado de importações B3.',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.white70,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                key: const Key('admin_refresh_button'),
                tooltip: 'Atualizar métricas',
                onPressed: onRefresh,
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          const SizedBox(height: 24),
          LayoutBuilder(
            builder: (context, constraints) {
              final isWide = constraints.maxWidth >= 980;
              if (isWide) {
                return Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: AdminActiveUsersCard(
                        metrics: metrics.userMetrics,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: AdminUploadMetricsCard(
                        metrics: metrics.uploadMetrics,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: AdminGrahamLatencyCard(
                        metrics: metrics.grahamLatencyMetrics,
                      ),
                    ),
                  ],
                );
              }
              return Column(
                children: [
                  AdminActiveUsersCard(metrics: metrics.userMetrics),
                  const SizedBox(height: 16),
                  AdminUploadMetricsCard(metrics: metrics.uploadMetrics),
                  const SizedBox(height: 16),
                  AdminGrahamLatencyCard(
                    metrics: metrics.grahamLatencyMetrics,
                  ),
                ],
              );
            },
          ),
          const SizedBox(height: 24),
          _AnonymizedUploadsCard(uploads: metrics.recentUploads),
        ],
      ),
    );
  }
}

class _AnonymizedUploadsCard extends StatelessWidget {
  final List<AnonymizedUploadTrace> uploads;

  const _AnonymizedUploadsCard({required this.uploads});

  @override
  Widget build(BuildContext context) {
    return Card(
      key: const Key('admin_anonymized_uploads_card'),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 12,
              runSpacing: 8,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                const Text(
                  'Importações Recentes Anonimizadas',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: const Color(0xFF0D9488).withValues(alpha: 0.18),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        Icons.verified_user_outlined,
                        size: 14,
                        color: Color(0xFF0D9488),
                      ),
                      SizedBox(width: 6),
                      Text(
                        'Dados Anonimizados (LGPD / NFR5)',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFF0D9488),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (uploads.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 20),
                child: Text(
                  'Nenhuma importação registrada até o momento.',
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.7),
                  ),
                ),
              )
            else
              Column(
                children: uploads.map((trace) {
                  return Container(
                    padding: const EdgeInsets.symmetric(
                      vertical: 12,
                      horizontal: 12,
                    ),
                    margin: const EdgeInsets.only(bottom: 8),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.03),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Wrap(
                      spacing: 16,
                      runSpacing: 8,
                      alignment: WrapAlignment.spaceBetween,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(
                              Icons.person_pin_outlined,
                              size: 18,
                              color: Colors.white70,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              trace.anonymizedUserId,
                              style: const TextStyle(
                                fontWeight: FontWeight.w600,
                                fontFamily: 'monospace',
                              ),
                            ),
                          ],
                        ),
                        Text(
                          trace.sanitizedFileName,
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.8),
                          ),
                        ),
                        _StatusBadge(status: trace.status),
                        Text(
                          'Linhas: ${trace.processedLines} ok / ${trace.errorLines} erros',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.white.withValues(alpha: 0.75),
                          ),
                        ),
                        Text(
                          'Patrimônio: ${trace.maskedPortfolioValue}',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.white.withValues(alpha: 0.65),
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              ),
          ],
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final String status;

  const _StatusBadge({required this.status});

  @override
  Widget build(BuildContext context) {
    Color color;
    switch (status.toUpperCase()) {
      case 'SUCCESS':
      case 'SUCESSO':
      case 'COMPLETED':
        color = const Color(0xFF10B981);
      case 'PARTIAL':
      case 'PARCIAL':
      case 'COMPLETED_WITH_ERRORS':
        color = const Color(0xFFF59E0B);
      case 'ERROR':
      case 'ERRO':
      case 'FAILED':
        color = const Color(0xFFEF4444);
      default:
        color = const Color(0xFF0D9488);
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.18),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        status,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.bold,
          color: color,
        ),
      ),
    );
  }
}
