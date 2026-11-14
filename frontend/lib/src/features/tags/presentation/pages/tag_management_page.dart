import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/portfolio_tag.dart';
import '../providers/tag_provider.dart';

/// Tela dedicada para listar, criar, renomear e excluir tags customizadas
/// do usuário autenticado.
class TagManagementPage extends StatefulWidget {
  const TagManagementPage({super.key});

  @override
  State<TagManagementPage> createState() => _TagManagementPageState();
}

class _TagManagementPageState extends State<TagManagementPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final tagProvider = context.read<TagProvider>();
      tagProvider.loadTags(silent: tagProvider.tags.isNotEmpty);
    });
  }

  Future<void> _openCreateDialog(BuildContext context, TagProvider provider) async {
    provider.clearError();
    final messenger = ScaffoldMessenger.of(context);
    final createdName = await showDialog<String>(
      context: context,
      builder: (_) => _TagFormDialog(provider: provider),
    );
    if (!mounted || createdName == null) return;
    messenger.showSnackBar(
      SnackBar(content: Text('Tag "$createdName" criada com sucesso.')),
    );
  }

  Future<void> _openRenameDialog(
    BuildContext context,
    TagProvider provider,
    PortfolioTag tag,
  ) async {
    provider.clearError();
    final messenger = ScaffoldMessenger.of(context);
    final updatedName = await showDialog<String>(
      context: context,
      builder: (_) => _TagFormDialog(provider: provider, existingTag: tag),
    );
    if (!mounted || updatedName == null) return;
    messenger.showSnackBar(
      SnackBar(content: Text('Tag atualizada para "$updatedName".')),
    );
  }

  Future<void> _confirmDeleteTag(
    BuildContext context,
    TagProvider provider,
    PortfolioTag tag,
  ) async {
    final messenger = ScaffoldMessenger.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        final errorColor = Theme.of(dialogContext).colorScheme.error;
        return AlertDialog(
          backgroundColor: AppTheme.surfaceColor,
          title: Text('Excluir tag "${tag.name}"?'),
          content: const Text(
            'Os ativos vinculados serão apenas desvinculados desta tag. Suas operações e o histórico da carteira permanecerão intactos.',
          ),
          actions: [
            TextButton(
              key: const Key('confirm_delete_cancel_button'),
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Cancelar'),
            ),
            OutlinedButton(
              key: const Key('confirm_delete_confirm_button'),
              style: OutlinedButton.styleFrom(
                foregroundColor: errorColor,
                side: BorderSide(color: errorColor.withValues(alpha: 0.7)),
                minimumSize: const Size(48, 48),
              ),
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('Excluir Tag'),
            ),
          ],
        );
      },
    );

    if (confirmed != true || !mounted) return;

    final deleted = await provider.deleteTag(tag.id);
    if (!mounted) return;

    if (deleted) {
      messenger.showSnackBar(
        SnackBar(
          content: Text(
            'Tag "${tag.name}" excluída. Suas operações permaneceram intactas.',
          ),
        ),
      );
    } else {
      messenger.showSnackBar(
        SnackBar(
          content: Text(
            provider.errorMessage ?? 'Não foi possível excluir a tag.',
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final tagProvider = context.watch<TagProvider>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Gerenciar Tags'),
        backgroundColor: AppTheme.backgroundColor,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                const Expanded(
                  child: Text(
                    'Crie e organize tags para agrupar e filtrar seus ativos sem impactar seus lançamentos.',
                    style: TextStyle(color: Colors.white70, fontSize: 14),
                  ),
                ),
                const SizedBox(width: 12),
                FilledButton.icon(
                  key: const Key('create_tag_button'),
                  style: FilledButton.styleFrom(
                    minimumSize: const Size(140, 48),
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                  ),
                  onPressed: tagProvider.isSubmitting
                      ? null
                      : () => _openCreateDialog(context, tagProvider),
                  icon: const Icon(Icons.add),
                  label: const Text('Nova Tag'),
                ),
              ],
            ),
            const SizedBox(height: 20),
            Expanded(child: _buildBody(context, tagProvider)),
          ],
        ),
      ),
    );
  }

  Widget _buildBody(BuildContext context, TagProvider provider) {
    if ((provider.state == TagState.initial ||
            provider.state == TagState.loading) &&
        provider.tags.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (provider.state == TagState.error && provider.tags.isEmpty) {
      return Center(
        child: Card(
          color: AppTheme.surfaceColor,
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.error_outline,
                  color: Theme.of(context).colorScheme.error,
                  size: 40,
                ),
                const SizedBox(height: 12),
                Text(
                  provider.errorMessage ?? 'Erro ao carregar tags.',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: AppTheme.textColor),
                ),
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: () => provider.loadTags(),
                  icon: const Icon(Icons.refresh),
                  label: const Text('Tentar novamente'),
                ),
              ],
            ),
          ),
        ),
      );
    }

    if (provider.tags.isEmpty) {
      return Center(
        child: Card(
          key: const Key('tags_empty_state'),
          color: AppTheme.surfaceColor,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: BorderSide(color: Colors.white.withValues(alpha: 0.08)),
          ),
          child: const Padding(
            padding: EdgeInsets.all(28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.label_off_outlined,
                  size: 48,
                  color: AppTheme.primaryColor,
                ),
                SizedBox(height: 12),
                Text(
                  'Nenhuma tag cadastrada ainda',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.textColor,
                  ),
                ),
                SizedBox(height: 8),
                Text(
                  'Use o botão "Nova Tag" acima para começar a categorizar os ativos da sua carteira.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white70, fontSize: 13),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return ListView.separated(
      itemCount: provider.tags.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (context, index) {
        final tag = provider.tags[index];
        return _buildTagCard(context, provider, tag);
      },
    );
  }

  Widget _buildTagCard(
    BuildContext context,
    TagProvider provider,
    PortfolioTag tag,
  ) {
    final tagColor = TagProvider.parseColor(tag.color);
    final errorColor = Theme.of(context).colorScheme.error;

    return Card(
      key: Key('tag_card_${tag.id}'),
      color: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: Colors.white.withValues(alpha: 0.08)),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              width: 14,
              height: 14,
              decoration: BoxDecoration(
                color: tagColor,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    tag.name,
                    style: const TextStyle(
                      color: AppTheme.textColor,
                      fontWeight: FontWeight.bold,
                      fontSize: 15,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '${tag.assetCount} ativo(s) vinculado(s)',
                    style: const TextStyle(
                      color: Colors.white70,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Semantics(
              label: 'Renomear tag ${tag.name}',
              child: IconButton(
                key: Key('rename_tag_button_${tag.id}'),
                tooltip: 'Renomear tag',
                icon: const Icon(Icons.edit_outlined, color: Colors.white70),
                onPressed: provider.isSubmitting
                    ? null
                    : () => _openRenameDialog(context, provider, tag),
              ),
            ),
            const SizedBox(width: 4),
            Semantics(
              label: 'Excluir tag ${tag.name}',
              child: OutlinedButton.icon(
                key: Key('delete_tag_button_${tag.id}'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: errorColor,
                  side: BorderSide(color: errorColor.withValues(alpha: 0.6)),
                  minimumSize: const Size(48, 48),
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                ),
                onPressed: provider.isSubmitting
                    ? null
                    : () => _confirmDeleteTag(context, provider, tag),
                icon: const Icon(Icons.delete_outline, size: 18),
                label: const Text('Excluir'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TagFormDialog extends StatefulWidget {
  const _TagFormDialog({required this.provider, this.existingTag});

  final TagProvider provider;
  final PortfolioTag? existingTag;

  @override
  State<_TagFormDialog> createState() => _TagFormDialogState();
}

class _TagFormDialogState extends State<_TagFormDialog> {
  late final TextEditingController _nameController;
  late String _selectedColor;
  bool _isSubmitting = false;
  String? _errorMessage;

  bool get _isEditing => widget.existingTag != null;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(
      text: widget.existingTag?.name ?? '',
    );
    _selectedColor = widget.existingTag?.color ?? kTagColorPalette.first;
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isSubmitting) return;

    final rawName = _nameController.text.trim();
    if (rawName.isEmpty) {
      setState(() {
        _errorMessage = 'Informe o nome da tag.';
      });
      return;
    }

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    final navigator = Navigator.of(context);
    final result = _isEditing
        ? await widget.provider.renameTag(
            tagId: widget.existingTag!.id,
            name: rawName,
            color: _selectedColor,
          )
        : await widget.provider.createTag(
            name: rawName,
            color: _selectedColor,
          );

    if (!mounted) return;

    if (result != null) {
      navigator.pop(result.name);
      return;
    }

    setState(() {
      _isSubmitting = false;
      _errorMessage =
          widget.provider.errorMessage ?? 'Erro ao salvar a tag.';
    });
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_isSubmitting,
      child: AlertDialog(
        backgroundColor: AppTheme.surfaceColor,
        title: Text(_isEditing ? 'Renomear Tag' : 'Nova Tag'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(
                key: const Key('tag_name_input'),
                controller: _nameController,
                enabled: !_isSubmitting,
                maxLength: 50,
                autofocus: true,
                textInputAction: TextInputAction.done,
                onSubmitted: (_) => _submit(),
                decoration: const InputDecoration(
                  labelText: 'Nome da tag',
                  hintText: 'Ex.: Dividendos',
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                'Cor da tag',
                style: TextStyle(color: Colors.white70, fontSize: 13),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 4,
                runSpacing: 4,
                children: kTagColorPalette.map((hex) {
                  final color = TagProvider.parseColor(hex);
                  final isSelected = _selectedColor == hex;
                  return Semantics(
                    label: 'Cor ${tagColorLabel(hex)}',
                    button: true,
                    selected: isSelected,
                    child: InkWell(
                      key: Key('tag_color_option_$hex'),
                      borderRadius: BorderRadius.circular(24),
                      onTap: _isSubmitting
                          ? null
                          : () {
                              setState(() {
                                _selectedColor = hex;
                              });
                            },
                      child: SizedBox(
                        width: 48,
                        height: 48,
                        child: Center(
                          child: Container(
                            width: 28,
                            height: 28,
                            decoration: BoxDecoration(
                              color: color,
                              shape: BoxShape.circle,
                              border: isSelected
                                  ? Border.all(color: Colors.white, width: 2.5)
                                  : Border.all(color: Colors.white24, width: 1),
                            ),
                            child: isSelected
                                ? const Icon(
                                    Icons.check,
                                    size: 16,
                                    color: Colors.white,
                                  )
                                : null,
                          ),
                        ),
                      ),
                    ),
                  );
                }).toList(),
              ),
              if (_errorMessage != null) ...[
                const SizedBox(height: 12),
                Text(
                  _errorMessage!,
                  key: const Key('tag_dialog_error'),
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.error,
                    fontSize: 13,
                  ),
                ),
              ],
            ],
          ),
        ),
        actions: [
          TextButton(
            key: const Key('tag_dialog_cancel_button'),
            onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            key: const Key('tag_dialog_save_button'),
            onPressed: _isSubmitting ? null : _submit,
            child: _isSubmitting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : Text(_isEditing ? 'Salvar' : 'Criar Tag'),
          ),
        ],
      ),
    );
  }
}
