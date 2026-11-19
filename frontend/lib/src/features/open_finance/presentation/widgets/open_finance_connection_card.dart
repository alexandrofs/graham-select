import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/open_finance_connection.dart';

class OpenFinanceConnectionCard extends StatelessWidget {
  final OpenFinanceConnection connection;
  final bool isSyncing;
  final VoidCallback onSync;
  final VoidCallback onDisconnect;

  const OpenFinanceConnectionCard({
    super.key,
    required this.connection,
    required this.isSyncing,
    required this.onSync,
    required this.onDisconnect,
  });

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('dd/MM/yyyy HH:mm');
    final lastSyncText = connection.lastSyncAt != null
        ? dateFormat.format(connection.lastSyncAt!.toLocal())
        : 'Nunca';

    Color statusColor;
    String statusLabel;

    switch (connection.status.toUpperCase()) {
      case 'ACTIVE':
        statusColor = AppTheme.accentColor;
        statusLabel = 'Ativo';
        break;
      case 'SYNCING':
        statusColor = Colors.blueAccent;
        statusLabel = 'Sincronizando';
        break;
      case 'EXPIRED':
        statusColor = Colors.orangeAccent;
        statusLabel = 'Expirado';
        break;
      case 'REVOKED':
        statusColor = Colors.grey;
        statusLabel = 'Revogado';
        break;
      case 'FAILED':
        statusColor = Colors.redAccent;
        statusLabel = 'Falha';
        break;
      default:
        statusColor = Colors.amber;
        statusLabel = 'Pendente';
    }

    return Card(
      color: AppTheme.surfaceColor,
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: AppTheme.textColor.withValues(alpha: 0.1),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: AppTheme.backgroundColor,
                  radius: 20,
                  child: (connection.institutionLogoUrl != null && connection.institutionLogoUrl!.isNotEmpty)
                      ? ClipOval(
                          child: Image.network(
                            connection.institutionLogoUrl!,
                            width: 40,
                            height: 40,
                            fit: BoxFit.cover,
                            errorBuilder: (context, error, stackTrace) => const Icon(
                              Icons.account_balance,
                              color: AppTheme.accentColor,
                              size: 20,
                            ),
                          ),
                        )
                      : const Icon(
                          Icons.account_balance,
                          color: AppTheme.accentColor,
                          size: 20,
                        ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        connection.institutionName,
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        connection.profileId != null
                            ? 'Sub-carteira Familiar'
                            : 'Conta Titular',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppTheme.textColor.withValues(alpha: 0.7),
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: statusColor.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: statusColor.withValues(alpha: 0.5)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: statusColor,
                        ),
                      ),
                      const SizedBox(width: 6),
                      Text(
                        statusLabel,
                        style: TextStyle(
                          color: statusColor,
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Divider(color: Colors.white12),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(
                      Icons.sync,
                      size: 14,
                      color: AppTheme.textColor.withValues(alpha: 0.6),
                    ),
                    const SizedBox(width: 4),
                    Text(
                      'Último sync: $lastSyncText',
                      style: TextStyle(
                        fontSize: 12,
                        color: AppTheme.textColor.withValues(alpha: 0.6),
                      ),
                    ),
                  ],
                ),
                Row(
                  children: [
                    TextButton.icon(
                      key: Key('open_finance_sync_button_${connection.id}'),
                      onPressed: isSyncing ? null : onSync,
                      icon: isSyncing
                          ? const SizedBox(
                              width: 14,
                              height: 14,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.refresh, size: 16),
                      label: Text(
                        isSyncing ? 'Sincronizando...' : 'Sincronizar Agora',
                        style: const TextStyle(fontSize: 12),
                      ),
                      style: TextButton.styleFrom(
                        foregroundColor: AppTheme.accentColor,
                        padding: const EdgeInsets.symmetric(horizontal: 8),
                      ),
                    ),
                    const SizedBox(width: 4),
                    IconButton(
                      key: Key('open_finance_disconnect_button_${connection.id}'),
                      onPressed: onDisconnect,
                      icon: const Icon(Icons.link_off, size: 18),
                      color: Colors.redAccent.withValues(alpha: 0.8),
                      tooltip: 'Desconectar',
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
