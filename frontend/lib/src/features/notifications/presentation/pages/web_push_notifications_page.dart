import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/notifications/domain/models/web_push_notification_models.dart';
import 'package:frontend/src/features/notifications/presentation/providers/web_push_notifications_provider.dart';
import 'package:intl/intl.dart';

class WebPushNotificationsPage extends StatefulWidget {
  const WebPushNotificationsPage({super.key});

  @override
  State<WebPushNotificationsPage> createState() =>
      _WebPushNotificationsPageState();
}

class _WebPushNotificationsPageState extends State<WebPushNotificationsPage> {
  bool _optedIn = false;
  double _minMarginOfSafety = 20.0;
  bool _onlyCustodyTickers = true;
  bool _initializedFromProvider = false;
  bool _hasUnsavedChanges = false;
  bool _isSyncingForm = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await _loadInitialData();
    });
  }

  Future<void> _loadInitialData() async {
    final provider = context.read<WebPushNotificationsProvider>();
    await provider.loadAll();
    if (mounted && provider.preferences != null && !_initializedFromProvider) {
      _syncForm(provider.preferences!);
    }
  }

  void _syncForm(WebPushPreferencesModel prefs) {
    _isSyncingForm = true;
    setState(() {
      _optedIn = prefs.optedIn;
      _minMarginOfSafety = prefs.minMarginOfSafetyPct.clamp(0.0, 90.0);
      _onlyCustodyTickers = prefs.onlyCustodyTickers;
      _initializedFromProvider = true;
      _hasUnsavedChanges = false;
    });
    _isSyncingForm = false;
  }

  Future<void> _onSave() async {
    final provider = context.read<WebPushNotificationsProvider>();
    final model = WebPushPreferencesModel(
      optedIn: _optedIn,
      minMarginOfSafetyPct: _minMarginOfSafety,
      onlyCustodyTickers: _onlyCustodyTickers,
    );

    final success = await provider.savePreferences(model);
    if (!mounted) return;

    if (success) {
      if (provider.preferences != null) {
        _syncForm(provider.preferences!);
      } else {
        setState(() => _hasUnsavedChanges = false);
      }
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Preferências de Web Push salvas com sucesso!'),
          backgroundColor: Colors.green,
        ),
      );
    } else {
      ScaffoldMessenger.of(context).hideCurrentSnackBar();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.errorMessage ?? 'Erro ao salvar preferências.'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  Future<void> _onRegisterBrowser() async {
    final provider = context.read<WebPushNotificationsProvider>();
    final success = await provider.registerCurrentBrowser();
    if (!mounted) return;

    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Navegador registrado com sucesso para receber alertas!'),
          backgroundColor: Colors.green,
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.errorMessage ??
              'Não foi possível registrar as notificações neste navegador.'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  Future<void> _onSendTestAlert() async {
    final provider = context.read<WebPushNotificationsProvider>();
    final result = await provider.sendTestAlert();
    if (!mounted) return;

    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    if (result != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
              'Alerta de teste enviado! (Enviados: ${result.sentCount}, Falhas: ${result.failedCount})'),
          backgroundColor: Colors.green,
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.errorMessage ?? 'Falha ao enviar alerta de teste.'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  Future<void> _onDeleteSubscription(String id) async {
    final provider = context.read<WebPushNotificationsProvider>();
    final success = await provider.deleteSubscription(id);
    if (!mounted) return;

    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Assinatura removida com sucesso.'),
          backgroundColor: Colors.green,
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.errorMessage ?? 'Erro ao remover assinatura.'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<WebPushNotificationsProvider>();

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        title: const Text('Alertas Web Push (Graham)'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: provider.isLoading
          ? const Center(child: CircularProgressIndicator())
          : provider.errorMessage != null && provider.preferences == null
              ? _buildErrorState(provider)
              : _buildContent(context, provider),
    );
  }

  Widget _buildErrorState(WebPushNotificationsProvider provider) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 60, color: Colors.redAccent),
            const SizedBox(height: 16),
            Text(
              'Erro ao carregar configurações de Web Push',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              provider.errorMessage ?? 'Erro desconhecido',
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.white70),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () async {
                await provider.loadAll();
                if (mounted && provider.preferences != null) {
                  _syncForm(provider.preferences!);
                }
              },
              icon: const Icon(Icons.refresh),
              label: const Text('Tentar Novamente'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(
      BuildContext context, WebPushNotificationsProvider provider) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildBrowserStatusCard(provider),
          const SizedBox(height: 24),
          _buildPreferencesCard(provider),
          const SizedBox(height: 24),
          _buildSubscriptionsCard(provider),
          const SizedBox(height: 24),
          _buildTestAlertCard(provider),
          const SizedBox(height: 24),
          _buildHistoryCard(provider),
        ],
      ),
    );
  }

  Widget _buildBrowserStatusCard(WebPushNotificationsProvider provider) {
    final perm = provider.permissionState;
    Color badgeColor;
    String badgeLabel;
    IconData badgeIcon;

    switch (perm) {
      case 'granted':
        badgeColor = Colors.green;
        badgeLabel = 'Permissão Concedida';
        badgeIcon = Icons.check_circle_outline;
        break;
      case 'denied':
        badgeColor = Colors.redAccent;
        badgeLabel = 'Bloqueado no Navegador';
        badgeIcon = Icons.block;
        break;
      case 'default':
        badgeColor = Colors.amber;
        badgeLabel = 'Pendente de Permissão';
        badgeIcon = Icons.help_outline;
        break;
      default:
        badgeColor = Colors.grey;
        badgeLabel = 'Não Suportado / Não Web';
        badgeIcon = Icons.info_outline;
    }

    final isUnsupported = perm == 'unsupported';
    final hasActiveSubscriptions = provider.subscriptions.any((s) => s.active);

    return Card(
      color: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Status do Navegador',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                Chip(
                  avatar: Icon(badgeIcon, size: 16, color: badgeColor),
                  label: Text(
                    badgeLabel,
                    style: TextStyle(color: badgeColor, fontSize: 12),
                  ),
                  backgroundColor: badgeColor.withValues(alpha: 0.15),
                  side: BorderSide(color: badgeColor.withValues(alpha: 0.3)),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Text(
              'Para receber alertas instantâneos de Zona de Compra de Graham quando os preços atingirem a margem desejada, registre este dispositivo.',
              style: TextStyle(color: Colors.white70, fontSize: 14),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                key: const Key('btn_activate_web_push'),
                onPressed: (isUnsupported || provider.isRegistering)
                    ? null
                    : _onRegisterBrowser,
                icon: provider.isRegistering
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.notifications_active),
                label: Text(provider.isRegistering
                    ? 'Ativando...'
                    : hasActiveSubscriptions && perm == 'granted'
                        ? 'Atualizar Assinatura deste Navegador'
                        : 'Ativar Notificações neste Navegador'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryColor,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPreferencesCard(WebPushNotificationsProvider provider) {
    return Card(
      color: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Critérios de Alerta (Zona de Compra Graham)',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            SwitchListTile.adaptive(
              key: const Key('switch_web_push_opt_in'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Receber Alertas Web Push'),
              subtitle: const Text(
                'Consentimento explícito (LGPD) para disparar notificações proativas.',
                style: TextStyle(color: Colors.white70, fontSize: 13),
              ),
              value: _optedIn,
              activeColor: AppTheme.primaryColor,
              onChanged: (val) {
                setState(() {
                  _optedIn = val;
                  if (!_isSyncingForm) _hasUnsavedChanges = true;
                });
              },
            ),
            const Divider(color: Colors.white12, height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Margem de Segurança Mínima',
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                ),
                Text(
                  '${_minMarginOfSafety.toStringAsFixed(0)}%',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.primaryColor,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            const Text(
              'Apenas ativos com desconto igual ou superior a esta margem acionarão alertas.',
              style: TextStyle(color: Colors.white70, fontSize: 13),
            ),
            Slider(
              key: const Key('slider_min_margin_of_safety'),
              value: _minMarginOfSafety,
              min: 0.0,
              max: 90.0,
              divisions: 18,
              label: '${_minMarginOfSafety.toStringAsFixed(0)}%',
              activeColor: AppTheme.primaryColor,
              onChanged: _optedIn
                  ? (val) {
                      setState(() {
                        _minMarginOfSafety = val;
                        if (!_isSyncingForm) _hasUnsavedChanges = true;
                      });
                    }
                  : null,
            ),
            const Divider(color: Colors.white12, height: 24),
            SwitchListTile.adaptive(
              key: const Key('switch_only_custody_tickers'),
              contentPadding: EdgeInsets.zero,
              title: const Text('Apenas ativos em custódia'),
              subtitle: const Text(
                'Filtra notificações apenas para empresas que você já possui na carteira.',
                style: TextStyle(color: Colors.white70, fontSize: 13),
              ),
              value: _onlyCustodyTickers,
              activeColor: AppTheme.primaryColor,
              onChanged: _optedIn
                  ? (val) {
                      setState(() {
                        _onlyCustodyTickers = val;
                        if (!_isSyncingForm) _hasUnsavedChanges = true;
                      });
                    }
                  : null,
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                key: const Key('btn_save_web_push_preferences'),
                onPressed: (provider.isSaving || !_hasUnsavedChanges)
                    ? null
                    : _onSave,
                icon: provider.isSaving
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.save),
                label: Text(provider.isSaving
                    ? 'Salvando...'
                    : _hasUnsavedChanges
                        ? 'Salvar Alterações'
                        : 'Preferências Salvas'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryColor,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSubscriptionsCard(WebPushNotificationsProvider provider) {
    final subs = provider.subscriptions;

    return Card(
      color: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Navegadores Registrados',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                Text(
                  '${subs.length}',
                  style: const TextStyle(color: Colors.white70),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (subs.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 16.0),
                child: Text(
                  'Nenhum navegador registrado no momento. Clique no botão de ativação acima para conectar este dispositivo.',
                  style: TextStyle(color: Colors.white54, fontSize: 13),
                ),
              )
            else
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: subs.length,
                separatorBuilder: (context, index) =>
                    const Divider(color: Colors.white12),
                itemBuilder: (context, index) {
                  final s = subs[index];
                  final isDeleting = provider.deletingSubscriptionId == s.id;

                  return ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: CircleAvatar(
                      backgroundColor:
                          AppTheme.primaryColor.withValues(alpha: 0.2),
                      child: const Icon(Icons.devices,
                          color: AppTheme.primaryColor, size: 20),
                    ),
                    title: Text(
                      s.endpointPreview,
                      style: const TextStyle(
                          fontSize: 14, fontWeight: FontWeight.w600),
                    ),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (s.userAgent != null && s.userAgent!.isNotEmpty)
                          Text(
                            s.userAgent!,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                                fontSize: 12, color: Colors.white54),
                          ),
                        Text(
                          'Registrado em: ${_formatDate(s.createdAt)}',
                          style: const TextStyle(
                              fontSize: 11, color: Colors.white38),
                        ),
                      ],
                    ),
                    trailing: isDeleting
                        ? const SizedBox(
                            width: 24,
                            height: 24,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : IconButton(
                            icon: const Icon(Icons.delete_outline,
                                color: Colors.redAccent),
                            tooltip: 'Remover assinatura',
                            onPressed: () => _onDeleteSubscription(s.id),
                          ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildTestAlertCard(WebPushNotificationsProvider provider) {
    final isPersistedOptedIn = provider.preferences?.optedIn ?? false;
    final canTest = _optedIn &&
        isPersistedOptedIn &&
        !_hasUnsavedChanges &&
        provider.subscriptions.any((s) => s.active);

    return Card(
      color: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Disparo de Teste',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'Envie uma notificação de validação para os seus navegadores ativos cadastrados.',
              style: TextStyle(color: Colors.white70, fontSize: 13),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                key: const Key('btn_send_web_push_test'),
                onPressed:
                    (provider.isTesting || !canTest) ? null : _onSendTestAlert,
                icon: provider.isTesting
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.send),
                label: Text(provider.isTesting
                    ? 'Enviando...'
                    : 'Enviar Notificação de Teste'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppTheme.primaryColor,
                  side: const BorderSide(color: AppTheme.primaryColor),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
              ),
            ),
            if (_hasUnsavedChanges) ...[
              const SizedBox(height: 8),
              const Text(
                'Nota: Salve as alterações acima antes de disparar o teste.',
                style: TextStyle(color: Colors.amberAccent, fontSize: 12),
              ),
            ] else if (!canTest) ...[
              const SizedBox(height: 8),
              const Text(
                'Nota: É necessário habilitar o consentimento e possuir pelo menos um navegador ativo para disparar o teste.',
                style: TextStyle(color: Colors.white38, fontSize: 12),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildHistoryCard(WebPushNotificationsProvider provider) {
    final history = provider.history;

    return Card(
      color: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Histórico Recente de Notificações',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            if (history.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 16.0),
                child: Text(
                  'Nenhuma notificação registrada até o momento.',
                  style: TextStyle(color: Colors.white54, fontSize: 13),
                ),
              )
            else
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: history.length,
                separatorBuilder: (context, index) =>
                    const Divider(color: Colors.white12),
                itemBuilder: (context, index) {
                  final log = history[index];
                  final isSuccess = log.status == 'SENT';
                  final statusColor = isSuccess
                      ? Colors.green
                      : (log.status == 'EXPIRED'
                          ? Colors.amber
                          : Colors.redAccent);

                  return ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: CircleAvatar(
                      backgroundColor: statusColor.withValues(alpha: 0.15),
                      child: Icon(
                        isSuccess ? Icons.check : Icons.warning_amber_rounded,
                        color: statusColor,
                        size: 20,
                      ),
                    ),
                    title: Row(
                      children: [
                        if (log.ticker != null && log.ticker!.isNotEmpty) ...[
                          Chip(
                            label: Text(
                              log.ticker!,
                              style: const TextStyle(
                                  fontSize: 11, fontWeight: FontWeight.bold),
                            ),
                            padding: EdgeInsets.zero,
                            materialTapTargetSize:
                                MaterialTapTargetSize.shrinkWrap,
                          ),
                          const SizedBox(width: 8),
                        ],
                        Expanded(
                          child: Text(
                            log.title,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                                fontSize: 14, fontWeight: FontWeight.w600),
                          ),
                        ),
                      ],
                    ),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 4),
                        Text(
                          log.messagePreview,
                          style: const TextStyle(
                              color: Colors.white70, fontSize: 13),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${_formatDate(log.sentAt)} • Status: ${log.status} • Provedor: ${log.provider}',
                          style: const TextStyle(
                              color: Colors.white38, fontSize: 11),
                        ),
                      ],
                    ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }

  String _formatDate(DateTime dt) {
    return DateFormat('dd/MM/yyyy HH:mm').format(dt.toLocal());
  }
}
