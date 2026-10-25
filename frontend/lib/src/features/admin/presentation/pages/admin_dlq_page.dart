import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/admin_dlq_message.dart';
import '../providers/admin_dlq_provider.dart';

class AdminDlqPage extends StatefulWidget {
  const AdminDlqPage({super.key});

  @override
  State<AdminDlqPage> createState() => _AdminDlqPageState();
}

class _AdminDlqPageState extends State<AdminDlqPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final provider = Provider.of<AdminDlqProvider?>(context, listen: false);
      if (provider != null && provider.status == AdminDlqState.initial) {
        provider.loadFailures();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<AdminDlqProvider>(
      builder: (context, provider, _) {
        switch (provider.status) {
          case AdminDlqState.initial:
          case AdminDlqState.loading:
            return const _AdminDlqSkeleton();
          case AdminDlqState.accessDenied:
            return const _AdminDlqAccessDeniedView();
          case AdminDlqState.error:
            return _AdminDlqErrorView(
              message: provider.errorMessage ?? 'Ocorreu um erro ao carregar as falhas da DLQ.',
              onRetry: provider.loadFailures,
            );
          case AdminDlqState.loaded:
            return _AdminDlqContent(provider: provider);
        }
      },
    );
  }
}

class _AdminDlqSkeleton extends StatelessWidget {
  const _AdminDlqSkeleton() : super(key: const Key('admin_dlq_skeleton'));

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildBox(width: 320, height: 32),
          const SizedBox(height: 12),
          _buildBox(width: 480, height: 16),
          const SizedBox(height: 24),
          _buildBox(width: double.infinity, height: 48),
          const SizedBox(height: 16),
          _buildBox(width: double.infinity, height: 80),
          const SizedBox(height: 12),
          _buildBox(width: double.infinity, height: 80),
          const SizedBox(height: 12),
          _buildBox(width: double.infinity, height: 80),
        ],
      ),
    );
  }

  Widget _buildBox({required double width, required double height}) {
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(8),
      ),
    );
  }
}

class _AdminDlqAccessDeniedView extends StatelessWidget {
  const _AdminDlqAccessDeniedView() : super(key: const Key('admin_dlq_access_denied_state'));

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520),
          child: Card(
            color: AppTheme.surfaceColor,
            elevation: 4,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(32),
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
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Você não possui privilégios de administrador para acessar a Dead Letter Queue (DLQ).',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.white.withValues(alpha: 0.75),
                    ),
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(minimumSize: const Size(0, 44)),
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

class _AdminDlqErrorView extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _AdminDlqErrorView({
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: Color(0xFFEF4444), size: 48),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(minimumSize: const Size(0, 44)),
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Tentar Novamente'),
            ),
          ],
        ),
      ),
    );
  }
}

class _AdminDlqEmptyState extends StatelessWidget {
  const _AdminDlqEmptyState() : super(key: const Key('admin_dlq_empty_state'));

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: const Color(0xFF10B981).withValues(alpha: 0.15),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.check_circle_outline,
                size: 56,
                color: Color(0xFF10B981),
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              'Nenhuma falha de ingestão pendente',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              'Tudo limpo! Nenhuma falha de ingestão pendente na DLQ.',
              style: TextStyle(
                fontSize: 14,
                color: Colors.white.withValues(alpha: 0.70),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AdminDlqContent extends StatelessWidget {
  final AdminDlqProvider provider;

  const _AdminDlqContent({required this.provider});

  @override
  Widget build(BuildContext context) {
    final failures = provider.failures;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Row(
            children: [
              IconButton(
                key: const Key('admin_dlq_back_button'),
                icon: const Icon(Icons.arrow_back),
                tooltip: 'Voltar ao Dashboard',
                onPressed: () => context.go('/admin/dashboard'),
              ),
              const SizedBox(width: 8),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Falhas de Ingestão (Dead Letter Queue)',
                      style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Monitore mensagens rejeitadas pelo parser de extratos e execute o reprocessamento manual ou em lote.',
                      style: TextStyle(fontSize: 13, color: Colors.grey),
                    ),
                  ],
                ),
              ),
              IconButton(
                key: const Key('admin_dlq_refresh_button'),
                icon: const Icon(Icons.refresh),
                tooltip: 'Atualizar',
                onPressed: provider.loadFailures,
              ),
            ],
          ),
          const SizedBox(height: 24),

          // Action Bar & Selection
          if (failures.isNotEmpty)
            Card(
              color: AppTheme.surfaceColor,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Row(
                  children: [
                    Checkbox(
                      key: const Key('admin_dlq_select_all_checkbox'),
                      value: provider.isAllSelected,
                      onChanged: (_) => provider.selectAll(),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        provider.hasSelection
                            ? '${provider.selectedCount} selecionados'
                            : 'Selecionar todos (${failures.length})',
                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton.icon(
                      key: const Key('admin_dlq_reprocess_batch_button'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryColor,
                        foregroundColor: Colors.white,
                        minimumSize: const Size(0, 40),
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                      ),
                      onPressed: provider.hasSelection
                          ? () async {
                              final success = await provider.reprocessSelected();
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: Text(
                                      success
                                          ? 'Itens selecionados reprocessados com sucesso!'
                                          : (provider.errorMessage ?? 'Falha ao reprocessar lote.'),
                                    ),
                                    backgroundColor: success ? const Color(0xFF10B981) : const Color(0xFFEF4444),
                                  ),
                                );
                              }
                            }
                          : null,
                      icon: const Icon(Icons.replay, size: 18),
                      label: Text(
                        provider.hasSelection
                            ? 'Reprocessar Selecionados (${provider.selectedCount})'
                            : 'Reprocessar Selecionados',
                      ),
                    ),
                  ],
                ),
              ),
            ),

          const SizedBox(height: 16),

          if (failures.isEmpty)
            const SizedBox(
              height: 340,
              child: _AdminDlqEmptyState(),
            )
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: failures.length,
              separatorBuilder: (context, index) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final item = failures[index];
                final isSelected = provider.isSelected(item.id);
                final isReprocessing = provider.isItemReprocessing(item.id);

                return _DlqFailureCard(
                  item: item,
                  isSelected: isSelected,
                  isReprocessing: isReprocessing,
                  onToggleSelection: () => provider.toggleSelection(item.id),
                  onReprocess: () async {
                    final success = await provider.reprocessItem(item.id);
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(
                            success
                                ? 'Item reprocessado com sucesso!'
                                : (provider.errorMessage ?? 'Falha ao reprocessar item.'),
                          ),
                          backgroundColor: success ? const Color(0xFF10B981) : const Color(0xFFEF4444),
                        ),
                      );
                    }
                  },
                );
              },
            ),
        ],
      ),
    );
  }
}

class _DlqFailureCard extends StatelessWidget {
  final AdminDlqMessage item;
  final bool isSelected;
  final bool isReprocessing;
  final VoidCallback onToggleSelection;
  final VoidCallback onReprocess;

  const _DlqFailureCard({
    required this.item,
    required this.isSelected,
    required this.isReprocessing,
    required this.onToggleSelection,
    required this.onReprocess,
  });

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('dd/MM/yyyy HH:mm:ss');
    final formattedDate = dateFormat.format(item.timestamp.toLocal());

    return Card(
      color: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: isSelected
              ? AppTheme.primaryColor.withValues(alpha: 0.6)
              : Colors.white.withValues(alpha: 0.08),
          width: isSelected ? 1.5 : 1,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Checkbox(
              key: Key('admin_dlq_item_checkbox_${item.id}'),
              value: isSelected,
              onChanged: (_) => onToggleSelection(),
            ),
            const SizedBox(width: 8),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFFEF4444).withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(
                Icons.table_chart_outlined,
                color: Color(0xFFEF4444),
                size: 24,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          item.fileName,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.08),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          item.anonymizedUserId,
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: Colors.tealAccent,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0xFFEF4444).withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: const Color(0xFFEF4444).withValues(alpha: 0.25),
                      ),
                    ),
                    child: Text(
                      item.errorMessage,
                      style: const TextStyle(
                        fontSize: 13,
                        color: Color(0xFFFCA5A5),
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 16,
                    runSpacing: 4,
                    children: [
                      Text(
                        'Linha: #${item.lineNumber ?? "-"}',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.65),
                        ),
                      ),
                      Text(
                        'Horário (Local): $formattedDate',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.65),
                        ),
                      ),
                      Text(
                        'Correlação: ${item.correlationId}',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.45),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 16),
            isReprocessing
                ? const SizedBox(
                    width: 32,
                    height: 32,
                    child: Center(
                      child: CircularProgressIndicator(strokeWidth: 2.5),
                    ),
                  )
                : OutlinedButton.icon(
                    key: Key('admin_dlq_reprocess_button_${item.id}'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppTheme.primaryColor,
                      side: BorderSide(color: AppTheme.primaryColor.withValues(alpha: 0.6)),
                    ),
                    onPressed: onReprocess,
                    icon: const Icon(Icons.replay, size: 16),
                    label: const Text('Reprocessar'),
                  ),
          ],
        ),
      ),
    );
  }
}
