import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../family_holding/presentation/providers/family_profile_provider.dart';
import '../../domain/entities/open_finance_institution.dart';
import '../providers/open_finance_provider.dart';
import 'open_finance_connection_card.dart';
import 'open_finance_institution_dialog.dart';

class OpenFinanceTab extends StatefulWidget {
  const OpenFinanceTab({super.key});

  @override
  State<OpenFinanceTab> createState() => _OpenFinanceTabState();
}

class _OpenFinanceTabState extends State<OpenFinanceTab> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final ofProvider = context.read<OpenFinanceProvider>();
      if (ofProvider.state == OpenFinanceState.initial) {
        ofProvider.fetchConnections();
        ofProvider.fetchInstitutions();
      }
    });
  }

  Future<void> _handleConnectInstitution(
    BuildContext context,
    OpenFinanceProvider ofProvider,
  ) async {
    if (ofProvider.institutions.isEmpty) {
      await ofProvider.fetchInstitutions();
    }

    if (!context.mounted) return;

    showDialog<void>(
      context: context,
      builder: (dialogCtx) {
        return OpenFinanceInstitutionDialog(
          institutions: ofProvider.institutions,
          onSelected: (OpenFinanceInstitution inst) async {
            Navigator.of(dialogCtx).pop();
            final consentUrl = await ofProvider.initiateConnection(inst.id);
            if (!context.mounted) return;
            if (consentUrl != null && consentUrl.isNotEmpty) {
              final uri = Uri.parse(consentUrl);
              final launched = await launchUrl(uri, mode: LaunchMode.externalApplication);
              if (!launched && context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Falha ao abrir navegador para autorização Open Finance.'),
                    backgroundColor: Colors.redAccent,
                  ),
                );
              }
            } else if (context.mounted && ofProvider.errorMessage != null) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(ofProvider.errorMessage!),
                  backgroundColor: Colors.redAccent,
                ),
              );
            }
          },
        );
      },
    );
  }

  Future<void> _handleSync(
    BuildContext context,
    OpenFinanceProvider ofProvider,
    String connectionId,
  ) async {
    final result = await ofProvider.syncConnection(connectionId);
    if (!context.mounted) return;

    if (result != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Sincronização concluída: ${result.tradesCreated} trades novos, ${result.tradesDuplicated} duplicados.',
          ),
          backgroundColor: AppTheme.accentColor,
        ),
      );
    } else if (ofProvider.errorMessage != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(ofProvider.errorMessage!),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  Future<void> _handleDisconnect(
    BuildContext context,
    OpenFinanceProvider ofProvider,
    String connectionId,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surfaceColor,
        title: const Text('Desconectar Instituição', style: TextStyle(color: Colors.white)),
        content: const Text(
          'Deseja revogar o consentimento desta conta bancária/corretora? As credenciais cifradas serão excluídas.',
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Desconectar'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      final success = await ofProvider.revokeConnection(connectionId);
      if (!context.mounted) return;
      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Conexão revogada com sucesso.'),
            backgroundColor: AppTheme.accentColor,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    FamilyProfileProvider? familyProvider;
    try {
      familyProvider = context.watch<FamilyProfileProvider>();
    } catch (_) {
      familyProvider = null;
    }

    final profiles = familyProvider?.profiles ?? const [];

    return Consumer<OpenFinanceProvider>(
      builder: (context, provider, child) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Security badge header
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppTheme.surfaceColor,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.accentColor.withValues(alpha: 0.3)),
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: AppTheme.accentColor.withValues(alpha: 0.15),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.shield_outlined,
                      color: AppTheme.accentColor,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Sincronização Contínua via Open Finance',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                            fontSize: 15,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Conecte suas contas com segurança bancária (padrão Bacen OpenID FAPI) e criptografia AES-256 de ponta a ponta.',
                          style: TextStyle(
                            color: AppTheme.textColor.withValues(alpha: 0.7),
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Family profile selector (if family profiles exist)
            if (profiles.isNotEmpty) ...[
              DropdownButtonFormField<String?>(
                key: const Key('open_finance_profile_selector'),
                initialValue: provider.selectedProfileId,
                decoration: const InputDecoration(
                  labelText: 'Vincular a Perfil Familiar (Opcional - Premium)',
                  border: OutlineInputBorder(),
                ),
                dropdownColor: AppTheme.surfaceColor,
                items: [
                  const DropdownMenuItem<String?>(
                    value: null,
                    child: Text('Conta Titular (Minha Carteira)'),
                  ),
                  ...profiles.map(
                    (profile) => DropdownMenuItem<String?>(
                      value: profile.id,
                      child: Text('${profile.name} (${profile.cpfMasked})'),
                    ),
                  ),
                ],
                onChanged: (value) => provider.setSelectedProfileId(value),
              ),
              const SizedBox(height: 16),
            ],

            // Action button to connect institution
            ElevatedButton.icon(
              key: const Key('open_finance_connect_button'),
              onPressed: () => _handleConnectInstitution(context, provider),
              icon: const Icon(Icons.add_link),
              label: const Text('Conectar Nova Instituição'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
                backgroundColor: AppTheme.accentColor,
                foregroundColor: Colors.black,
              ),
            ),
            const SizedBox(height: 24),

            // Active connections section
            Text(
              'Instituições Conectadas',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
            ),
            const SizedBox(height: 12),

            if (provider.state == OpenFinanceState.loading && provider.connections.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32.0),
                  child: CircularProgressIndicator(),
                ),
              )
            else if (provider.connections.isEmpty)
              Container(
                key: const Key('open_finance_empty_state'),
                padding: const EdgeInsets.all(32),
                decoration: BoxDecoration(
                  color: AppTheme.surfaceColor,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.white12),
                ),
                child: Column(
                  children: [
                    Icon(
                      Icons.account_balance_outlined,
                      size: 48,
                      color: AppTheme.textColor.withValues(alpha: 0.3),
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Nenhuma instituição conectada ainda.',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: Colors.white70,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Clique em "Conectar Nova Instituição" para autorizar a sincronização de sua corretora.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 12,
                        color: AppTheme.textColor.withValues(alpha: 0.6),
                      ),
                    ),
                  ],
                ),
              )
            else
              ListView.builder(
                key: const Key('open_finance_connections_list'),
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: provider.connections.length,
                itemBuilder: (context, index) {
                  final connection = provider.connections[index];
                  final isSyncing = provider.isConnectionSyncing(connection.id);
                  return OpenFinanceConnectionCard(
                    connection: connection,
                    isSyncing: isSyncing,
                    onSync: () => _handleSync(context, provider, connection.id),
                    onDisconnect: () => _handleDisconnect(context, provider, connection.id),
                  );
                },
              ),
          ],
        );
      },
    );
  }
}
