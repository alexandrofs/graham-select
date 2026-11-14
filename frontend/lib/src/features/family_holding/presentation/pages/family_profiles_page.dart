import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../providers/family_profile_provider.dart';

class FamilyProfilesPage extends StatefulWidget {
  const FamilyProfilesPage({super.key});

  @override
  State<FamilyProfilesPage> createState() => _FamilyProfilesPageState();
}

class _FamilyProfilesPageState extends State<FamilyProfilesPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        context.read<FamilyProfileProvider>().fetchProfiles();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Consumer<FamilyProfileProvider>(
        builder: (context, provider, _) {
          return SingleChildScrollView(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Holding Familiar (Multi-CPF)',
                            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Vincule os CPFs dos seus dependentes para importar extratos B3 e consolidar a carteira da família.',
                            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  color: AppTheme.textColor.withValues(alpha: 0.7),
                                ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 16),
                    if (provider.state != FamilyProfileState.premiumRequired)
                      ElevatedButton.icon(
                        key: const Key('family_profile_add_button'),
                        onPressed: provider.isSubmitting
                            ? null
                            : () => _openAddProfileDialog(context, provider),
                        icon: const Icon(Icons.person_add_alt_1),
                        label: const Text('Vincular Novo CPF'),
                      ),
                  ],
                ),
                const SizedBox(height: 24),
                _buildBody(context, provider),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildBody(BuildContext context, FamilyProfileProvider provider) {
    switch (provider.state) {
      case FamilyProfileState.initial:
      case FamilyProfileState.loading:
        return const Center(
          child: Padding(
            padding: EdgeInsets.all(48.0),
            child: CircularProgressIndicator(),
          ),
        );
      case FamilyProfileState.premiumRequired:
        return Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: const Color(0xFFD97706).withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFFD97706).withValues(alpha: 0.4)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(
                children: [
                  Icon(Icons.workspace_premium, color: Color(0xFFD97706), size: 28),
                  SizedBox(width: 12),
                  Text(
                    'Recurso Exclusivo Premium',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFFD97706),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                provider.errorMessage ??
                    'A gestão de Holding Familiar (Multi-CPF) é exclusiva para assinantes Premium.',
                style: const TextStyle(color: Colors.white70),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () => context.push('/profile'),
                child: const Text('Ver Planos Premium'),
              ),
            ],
          ),
        );
      case FamilyProfileState.error:
        return Container(
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: Colors.redAccent.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.redAccent.withValues(alpha: 0.4)),
          ),
          child: Column(
            children: [
              Text(
                provider.errorMessage ?? 'Erro ao carregar perfis familiares.',
                style: const TextStyle(color: Colors.redAccent),
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: () => provider.fetchProfiles(),
                icon: const Icon(Icons.refresh),
                label: const Text('Tentar Novamente'),
              ),
            ],
          ),
        );
      case FamilyProfileState.loaded:
        if (provider.profiles.isEmpty) {
          return Container(
            key: const Key('family_profiles_empty_state'),
            padding: const EdgeInsets.all(32),
            decoration: BoxDecoration(
              color: AppTheme.surfaceColor,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
            ),
            child: Column(
              children: [
                Icon(
                  Icons.family_restroom,
                  size: 48,
                  color: AppTheme.primaryColor.withValues(alpha: 0.8),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Nenhum CPF dependente vinculado ainda',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Clique em "Vincular Novo CPF" para cadastrar os perfis da sua família.',
                  style: TextStyle(color: Colors.white70),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          );
        }

        return Column(
          children: provider.profiles.map((profile) {
            return Card(
              color: AppTheme.surfaceColor,
              margin: const EdgeInsets.only(bottom: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
                side: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
              ),
              child: ListTile(
                contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                leading: CircleAvatar(
                  backgroundColor: AppTheme.primaryColor.withValues(alpha: 0.2),
                  child: const Icon(Icons.person_outline, color: AppTheme.primaryColor),
                ),
                title: Text(
                  profile.name,
                  style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
                ),
                subtitle: Text(
                  profile.relationship != null && profile.relationship!.isNotEmpty
                      ? 'CPF: ${profile.cpfMasked} • ${profile.relationship}'
                      : 'CPF: ${profile.cpfMasked}',
                  style: const TextStyle(color: Colors.white70),
                ),
                trailing: IconButton(
                  key: Key('family_profile_delete_button_${profile.id}'),
                  icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                  tooltip: 'Desvincular perfil',
                  onPressed: provider.isSubmitting
                      ? null
                      : () => _confirmUnlink(context, provider, profile.id, profile.name),
                ),
              ),
            );
          }).toList(),
        );
    }
  }

  Future<void> _openAddProfileDialog(
    BuildContext context,
    FamilyProfileProvider provider,
  ) async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => _AddProfileDialog(
        provider: provider,
        parentContext: context,
      ),
    );
  }

  Future<void> _confirmUnlink(
    BuildContext context,
    FamilyProfileProvider provider,
    String profileId,
    String name,
  ) async {
    final success = await provider.unlinkProfile(profileId);
    if (!context.mounted) return;
    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Perfil "$name" desvinculado com sucesso.')),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(provider.errorMessage ?? 'Erro ao desvincular perfil.'),
          backgroundColor: Colors.redAccent,
        ),
      );
    }
  }
}

class _AddProfileDialog extends StatefulWidget {
  final FamilyProfileProvider provider;
  final BuildContext parentContext;

  const _AddProfileDialog({
    required this.provider,
    required this.parentContext,
  });

  @override
  State<_AddProfileDialog> createState() => _AddProfileDialogState();
}

class _AddProfileDialogState extends State<_AddProfileDialog> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _cpfController = TextEditingController();
  final _relationshipController = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _nameController.dispose();
    _cpfController.dispose();
    _relationshipController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_submitting || !_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      final success = await widget.provider.linkProfile(
        cpf: _cpfController.text.trim(),
        name: _nameController.text.trim(),
        relationship: _relationshipController.text.trim().isEmpty
            ? null
            : _relationshipController.text.trim(),
      );
      if (!mounted) return;
      if (success) {
        Navigator.of(context).pop();
        if (widget.parentContext.mounted) {
          ScaffoldMessenger.of(widget.parentContext).showSnackBar(
            const SnackBar(content: Text('CPF vinculado com sucesso!')),
          );
        }
      } else {
        setState(() => _submitting = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(widget.provider.errorMessage ?? 'Erro ao vincular CPF.'),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        setState(() => _submitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppTheme.surfaceColor,
      title: const Text('Vincular Novo CPF'),
      content: Form(
        key: _formKey,
        child: SizedBox(
          width: 400,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                key: const Key('family_profile_name_input'),
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Nome completo',
                  hintText: 'Ex: Maria Silva',
                ),
                validator: (value) {
                  final trimmed = value?.trim() ?? '';
                  if (trimmed.length < 2 || trimmed.length > 120) {
                    return 'Informe um nome entre 2 e 120 caracteres.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                key: const Key('family_profile_cpf_input'),
                controller: _cpfController,
                keyboardType: TextInputType.number,
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp(r'[0-9.\-]')),
                ],
                decoration: const InputDecoration(
                  labelText: 'CPF',
                  hintText: '000.000.000-00',
                ),
                validator: (value) {
                  final digits = (value ?? '').replaceAll(RegExp(r'\D'), '');
                  if (digits.length != 11 || RegExp(r'^(\d)\1{10}$').hasMatch(digits)) {
                    return 'Informe um CPF válido com 11 dígitos.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                key: const Key('family_profile_relationship_input'),
                controller: _relationshipController,
                decoration: const InputDecoration(
                  labelText: 'Parentesco (opcional)',
                  hintText: 'Ex: Cônjuge, Filho(a)',
                ),
                validator: (value) {
                  if (value != null && value.trim().length > 50) {
                    return 'Parentesco deve ter no máximo 50 caracteres.';
                  }
                  return null;
                },
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _submitting ? null : () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        ElevatedButton(
          key: const Key('family_profile_save_button'),
          onPressed: _submitting ? null : _submit,
          child: _submitting
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Salvar'),
        ),
      ],
    );
  }
}

