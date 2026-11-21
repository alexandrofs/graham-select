import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/notifications/domain/models/whatsapp_notification_models.dart';
import 'package:frontend/src/features/notifications/presentation/providers/whatsapp_notifications_provider.dart';

class WhatsappNotificationsPage extends StatefulWidget {
  const WhatsappNotificationsPage({super.key});

  @override
  State<WhatsappNotificationsPage> createState() =>
      _WhatsappNotificationsPageState();
}

class _WhatsappNotificationsPageState extends State<WhatsappNotificationsPage> {
  final _phoneController = TextEditingController();
  bool _optedIn = false;
  bool _notifyDividends = true;
  bool _notifyRebalanceTriggers = true;
  bool _notifyGoalMilestones = true;
  double _driftThreshold = 5.0;
  bool _initializedFromProvider = false;
  bool _hasUnsavedChanges = false;
  bool _isSyncingForm = false;

  @override
  void initState() {
    super.initState();
    _phoneController.addListener(() {
      if (!_isSyncingForm && !_hasUnsavedChanges && mounted) {
        setState(() => _hasUnsavedChanges = true);
      }
    });
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await _loadInitialData();
    });
  }

  Future<void> _loadInitialData() async {
    final provider = context.read<WhatsappNotificationsProvider>();
    await provider.loadAll();
    if (mounted && provider.preferences != null && !_initializedFromProvider) {
      _syncForm(provider.preferences!);
    }
  }

  void _syncForm(WhatsappPreferencesModel prefs) {
    _isSyncingForm = true;
    setState(() {
      _phoneController.text = prefs.phoneNumber;
      _optedIn = prefs.optedIn;
      _notifyDividends = prefs.notifyDividends;
      _notifyRebalanceTriggers = prefs.notifyRebalanceTriggers;
      _notifyGoalMilestones = prefs.notifyGoalMilestones;
      _driftThreshold = prefs.rebalanceDriftThresholdPct.clamp(1.0, 50.0);
      _initializedFromProvider = true;
      _hasUnsavedChanges = false;
    });
    _isSyncingForm = false;
  }

  @override
  void dispose() {
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _onSave() async {
    final provider = context.read<WhatsappNotificationsProvider>();
    final model = WhatsappPreferencesModel(
      phoneNumber: _phoneController.text.trim(),
      optedIn: _optedIn,
      notifyDividends: _notifyDividends,
      notifyRebalanceTriggers: _notifyRebalanceTriggers,
      notifyGoalMilestones: _notifyGoalMilestones,
      rebalanceDriftThresholdPct: _driftThreshold,
    );

    final ok = await provider.savePreferences(model);
    if (!mounted) return;
    if (ok) {
      if (provider.preferences != null) {
        _syncForm(provider.preferences!);
      }
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          const SnackBar(
            content: Text('Preferências de alertas WhatsApp salvas com sucesso!'),
            backgroundColor: Colors.green,
          ),
        );
    } else {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(provider.errorMessage ?? 'Erro ao salvar preferências.'),
            backgroundColor: Colors.redAccent,
          ),
        );
    }
  }

  Future<void> _onSendTest() async {
    final provider = context.read<WhatsappNotificationsProvider>();
    final result = await provider.sendTestAlert();
    if (!mounted) return;
    if (result != null && result.status == 'SENT') {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text('Teste enviado via ${result.provider}!'),
            backgroundColor: Colors.green,
          ),
        );
    } else {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(provider.errorMessage ??
                'Não foi possível enviar o alerta de teste.'),
            backgroundColor: Colors.redAccent,
          ),
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        title: const Text('Alertas via WhatsApp'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: Consumer<WhatsappNotificationsProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading && provider.preferences == null) {
            return const Center(child: CircularProgressIndicator());
          }

          return SingleChildScrollView(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (provider.errorMessage != null)
                  Container(
                    width: double.infinity,
                    margin: const EdgeInsets.only(bottom: 16),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.redAccent.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.redAccent),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            provider.errorMessage!,
                            style: const TextStyle(color: Colors.redAccent),
                          ),
                        ),
                        if (provider.preferences == null)
                          TextButton.icon(
                            key: const Key('whatsapp_retry_button'),
                            onPressed: _loadInitialData,
                            icon: const Icon(Icons.refresh, color: Colors.redAccent),
                            label: const Text(
                              'Tentar novamente',
                              style: TextStyle(color: Colors.redAccent),
                            ),
                          ),
                      ],
                    ),
                  ),
                Card(
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
                        Text(
                          'Configuração e Consentimento (LGPD)',
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 16),
                        TextField(
                          key: const Key('whatsapp_phone_field'),
                          controller: _phoneController,
                          keyboardType: TextInputType.phone,
                          decoration: const InputDecoration(
                            labelText: 'Telefone WhatsApp (formato E.164)',
                            hintText: '+5511999999999',
                            prefixIcon: Icon(Icons.phone_android),
                            border: OutlineInputBorder(),
                          ),
                        ),
                        const SizedBox(height: 12),
                        SwitchListTile(
                          key: const Key('whatsapp_opt_in_switch'),
                          contentPadding: EdgeInsets.zero,
                          title: const Text(
                            'Autorizo o envio de alertas via WhatsApp (LGPD)',
                            style: TextStyle(fontWeight: FontWeight.w600),
                          ),
                          subtitle: const Text(
                            'Você pode revogar seu consentimento a qualquer momento.',
                            style: TextStyle(color: Colors.white70, fontSize: 12),
                          ),
                          value: _optedIn,
                          onChanged: (val) => setState(() {
                            _optedIn = val;
                            _hasUnsavedChanges = true;
                          }),
                        ),
                        const Divider(height: 24),
                        Text(
                          'Canais de Alerta',
                          style: Theme.of(context)
                              .textTheme
                              .titleSmall
                              ?.copyWith(fontWeight: FontWeight.bold),
                        ),
                        SwitchListTile(
                          key: const Key('whatsapp_dividends_switch'),
                          contentPadding: EdgeInsets.zero,
                          title: const Text('Proventos (Dividendos e JCP)'),
                          value: _notifyDividends,
                          onChanged: (val) => setState(() {
                            _notifyDividends = val;
                            _hasUnsavedChanges = true;
                          }),
                        ),
                        SwitchListTile(
                          key: const Key('whatsapp_rebalance_switch'),
                          contentPadding: EdgeInsets.zero,
                          title: const Text('Gatilhos de Rebalanceamento'),
                          value: _notifyRebalanceTriggers,
                          onChanged: (val) => setState(() {
                            _notifyRebalanceTriggers = val;
                            _hasUnsavedChanges = true;
                          }),
                        ),
                        SwitchListTile(
                          key: const Key('whatsapp_goals_switch'),
                          contentPadding: EdgeInsets.zero,
                          title: const Text('Marcos de Meta Financeira (25%, 50%, 75%, 100%)'),
                          value: _notifyGoalMilestones,
                          onChanged: (val) => setState(() {
                            _notifyGoalMilestones = val;
                            _hasUnsavedChanges = true;
                          }),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text('Limiar de desvio de alocação'),
                            Text(
                              '${_driftThreshold.toStringAsFixed(0)}%',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                color: AppTheme.primaryColor,
                              ),
                            ),
                          ],
                        ),
                        Slider(
                          key: const Key('whatsapp_threshold_slider'),
                          min: 1,
                          max: 50,
                          divisions: 49,
                          value: _driftThreshold,
                          label: '${_driftThreshold.toStringAsFixed(0)}%',
                          onChanged: (val) => setState(() {
                            _driftThreshold = val.roundToDouble();
                            _hasUnsavedChanges = true;
                          }),
                        ),
                        const SizedBox(height: 16),
                        Wrap(
                          spacing: 12,
                          runSpacing: 12,
                          children: [
                            ElevatedButton.icon(
                              key: const Key('whatsapp_save_button'),
                              onPressed: provider.isSaving ? null : _onSave,
                              icon: provider.isSaving
                                  ? const SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(
                                          strokeWidth: 2),
                                    )
                                  : const Icon(Icons.save),
                              label: const Text('Salvar Preferências'),
                            ),
                            OutlinedButton.icon(
                              key: const Key('whatsapp_test_button'),
                              onPressed: (provider.isTesting || _hasUnsavedChanges)
                                  ? null
                                  : _onSendTest,
                              icon: provider.isTesting
                                  ? const SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(
                                          strokeWidth: 2),
                                    )
                                  : const Icon(Icons.send),
                              label: const Text('Enviar Teste'),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  'Histórico Recente de Envios',
                  style: Theme.of(context)
                      .textTheme
                      .titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                if (provider.history.isEmpty)
                  const Text(
                    'Nenhum alerta enviado até o momento.',
                    style: TextStyle(color: Colors.white70),
                  )
                else
                  ListView.separated(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: provider.history.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      final item = provider.history[index];
                      final isSent = item.status == 'SENT';
                      return Card(
                        color: AppTheme.surfaceColor,
                        child: ListTile(
                          leading: Icon(
                            isSent ? Icons.check_circle : Icons.error,
                            color: isSent ? Colors.green : Colors.redAccent,
                          ),
                          title: Text(item.eventType),
                          subtitle: Text(
                            item.messagePreview,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                          trailing: Text(
                            item.status,
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              color: isSent ? Colors.green : Colors.redAccent,
                            ),
                          ),
                        ),
                      );
                    },
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}
