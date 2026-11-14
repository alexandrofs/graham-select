import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/portfolio_tag.dart';
import '../providers/tag_provider.dart';

/// Bottom sheet modal para vincular/desvincular tags a um ativo específico
/// e permitir criação rápida de nova tag inline.
class AssetTagSelectorSheet extends StatefulWidget {
  const AssetTagSelectorSheet({super.key, required this.ticker});

  final String ticker;

  @override
  State<AssetTagSelectorSheet> createState() => _AssetTagSelectorSheetState();
}

class _AssetTagSelectorSheetState extends State<AssetTagSelectorSheet> {
  late final TextEditingController _newTagController;
  late Set<String> _selectedTagIds;
  late bool _didSyncInitialSelection;
  bool _hasUserEditedSelection = false;
  String _selectedColor = kTagColorPalette.first;
  bool _isSaving = false;
  bool _isCreatingInline = false;
  String? _inlineError;

  String get _normalizedTicker => widget.ticker.trim().toUpperCase();

  @override
  void initState() {
    super.initState();
    _newTagController = TextEditingController();
    final tagProvider = context.read<TagProvider>();
    _selectedTagIds = tagProvider
        .tagsForTicker(_normalizedTicker)
        .map((tag) => tag.id)
        .toSet();
    _didSyncInitialSelection =
        tagProvider.state == TagState.loaded || tagProvider.tags.isNotEmpty;
    if (tagProvider.state == TagState.initial) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && tagProvider.state == TagState.initial) {
          tagProvider.loadTags();
        }
      });
    }
  }

  @override
  void dispose() {
    _newTagController.dispose();
    super.dispose();
  }

  Future<void> _handleCreateInlineTag() async {
    if (_isCreatingInline || _isSaving) return;

    final rawName = _newTagController.text.trim();
    if (rawName.isEmpty) {
      setState(() {
        _inlineError = 'Informe o nome da tag.';
      });
      return;
    }

    setState(() {
      _isCreatingInline = true;
      _inlineError = null;
    });

    final tagProvider = context.read<TagProvider>();
    final created = await tagProvider.createTag(
      name: rawName,
      color: _selectedColor,
    );

    if (!mounted) return;

    setState(() {
      _isCreatingInline = false;
      if (created != null) {
        _selectedTagIds.add(created.id);
        _hasUserEditedSelection = true;
        _didSyncInitialSelection = true;
        _newTagController.clear();
      } else {
        _inlineError =
            tagProvider.errorMessage ?? 'Não foi possível criar a tag.';
      }
    });
  }

  Future<void> _handleSaveTags() async {
    if (_isSaving || _isCreatingInline) return;

    final tagProvider = context.read<TagProvider>();
    final isLoadingInitialTags =
        (tagProvider.state == TagState.initial ||
            tagProvider.state == TagState.loading) &&
        tagProvider.tags.isEmpty;
    if (isLoadingInitialTags ||
        (tagProvider.state == TagState.error && tagProvider.tags.isEmpty)) {
      return;
    }

    setState(() {
      _isSaving = true;
      _inlineError = null;
    });

    final navigator = Navigator.of(context);
    final messenger = ScaffoldMessenger.of(context);
    final existingIds = tagProvider.tags.map((tag) => tag.id).toSet();
    final validSelectedIds = _selectedTagIds
        .where(existingIds.contains)
        .toList();

    final success = await tagProvider.setAssetTags(
      ticker: _normalizedTicker,
      tagIds: validSelectedIds,
    );

    if (!mounted) return;

    setState(() {
      _isSaving = false;
    });

    if (success) {
      navigator.pop(true);
      messenger.showSnackBar(
        SnackBar(
          content: Text(
            'Tags de $_normalizedTicker atualizadas com sucesso.',
          ),
        ),
      );
    } else {
      setState(() {
        _inlineError =
            tagProvider.errorMessage ?? 'Erro ao salvar as tags do ativo.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final tagProvider = context.watch<TagProvider>();
    if (!_didSyncInitialSelection && tagProvider.state == TagState.loaded) {
      _didSyncInitialSelection = true;
      if (!_hasUserEditedSelection) {
        _selectedTagIds = tagProvider
            .tagsForTicker(_normalizedTicker)
            .map((tag) => tag.id)
            .toSet();
      }
    }

    final isLoadingInitialTags =
        (tagProvider.state == TagState.initial ||
            tagProvider.state == TagState.loading) &&
        tagProvider.tags.isEmpty;
    final hasLoadError =
        tagProvider.state == TagState.error && tagProvider.tags.isEmpty;
    final isBusy = _isSaving || _isCreatingInline || tagProvider.isSubmitting;
    final isSaveDisabled = isBusy || isLoadingInitialTags || hasLoadError;
    final mediaQuery = MediaQuery.of(context);
    final bottomInset = mediaQuery.viewInsets.bottom;
    final maxSheetHeight = mediaQuery.size.height * 0.9;

    return PopScope(
      canPop: !isBusy,
      child: SafeArea(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: maxSheetHeight),
          child: Container(
            key: const Key('asset_tag_selector_sheet'),
            decoration: const BoxDecoration(
              color: AppTheme.surfaceColor,
              borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
            ),
            padding: EdgeInsets.fromLTRB(20, 12, 20, 20 + bottomInset),
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Center(
                    child: Container(
                      width: 40,
                      height: 4,
                      decoration: BoxDecoration(
                        color: Colors.white24,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      const Icon(
                        Icons.label_outline,
                        color: AppTheme.primaryColor,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'Tags para $_normalizedTicker',
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(
                                fontWeight: FontWeight.bold,
                                color: AppTheme.textColor,
                              ),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close, color: Colors.white70),
                        tooltip: 'Fechar seletor de tags',
                        onPressed:
                            isBusy ? null : () => Navigator.of(context).pop(),
                      ),
                    ],
                  ),
                  if (_inlineError != null) ...[
                    const SizedBox(height: 8),
                    Container(
                      key: const Key('asset_tag_selector_error'),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Theme.of(
                          context,
                        ).colorScheme.error.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: Theme.of(
                            context,
                          ).colorScheme.error.withValues(alpha: 0.6),
                        ),
                      ),
                      child: Row(
                        children: [
                          Icon(
                            Icons.error_outline,
                            color: Theme.of(context).colorScheme.error,
                            size: 20,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              _inlineError!,
                              style: TextStyle(
                                color: Theme.of(context).colorScheme.error,
                                fontSize: 13,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 12),
                  if (isLoadingInitialTags)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          ),
                          SizedBox(width: 10),
                          Text(
                            'Carregando tags...',
                            style: TextStyle(color: Colors.white70, fontSize: 13),
                          ),
                        ],
                      ),
                    )
                  else if (hasLoadError)
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppTheme.backgroundColor.withValues(alpha: 0.6),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: Theme.of(
                            context,
                          ).colorScheme.error.withValues(alpha: 0.5),
                        ),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              tagProvider.errorMessage ??
                                  'Não foi possível carregar suas tags.',
                              style: const TextStyle(
                                color: Colors.white70,
                                fontSize: 13,
                              ),
                            ),
                          ),
                          TextButton.icon(
                            key: const Key('asset_tag_selector_retry_button'),
                            onPressed: isBusy ? null : () => tagProvider.loadTags(),
                            icon: const Icon(Icons.refresh, size: 18),
                            label: const Text('Tentar novamente'),
                          ),
                        ],
                      ),
                    )
                  else if (tagProvider.tags.isEmpty)
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppTheme.backgroundColor.withValues(alpha: 0.6),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.08),
                        ),
                      ),
                      child: const Text(
                        'Você ainda não possui tags. Crie uma abaixo para organizar sua carteira.',
                        style: TextStyle(color: Colors.white70, fontSize: 13),
                      ),
                    )
                  else
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxHeight: 240),
                      child: ListView.builder(
                        shrinkWrap: true,
                        itemCount: tagProvider.tags.length,
                        itemBuilder: (context, index) {
                          final tag = tagProvider.tags[index];
                          return _buildTagCheckboxTile(tag, isBusy);
                        },
                      ),
                    ),
                  const SizedBox(height: 16),
                  const Divider(color: Colors.white12),
                  const SizedBox(height: 12),
                  Text(
                    'Nova tag rápida',
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: Colors.white70,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Expanded(
                        child: TextField(
                          key: const Key('new_tag_inline_input'),
                          controller: _newTagController,
                          enabled: !isBusy,
                          maxLength: 50,
                          textInputAction: TextInputAction.done,
                          onSubmitted: (_) => _handleCreateInlineTag(),
                          decoration: const InputDecoration(
                            labelText: 'Nova tag',
                            hintText: 'Ex.: Dividendos',
                            counterText: '',
                            isDense: true,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      OutlinedButton.icon(
                        key: const Key('new_tag_inline_submit'),
                        style: OutlinedButton.styleFrom(
                          minimumSize: const Size(48, 48),
                          padding: const EdgeInsets.symmetric(horizontal: 14),
                        ),
                        onPressed: isBusy ? null : _handleCreateInlineTag,
                        icon: _isCreatingInline
                            ? const SizedBox(
                                width: 16,
                                height: 16,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : const Icon(Icons.add, size: 18),
                        label: const Text('Criar'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
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
                          key: Key('inline_tag_color_$hex'),
                          borderRadius: BorderRadius.circular(24),
                          onTap: isBusy
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
                                      ? Border.all(
                                          color: Colors.white,
                                          width: 2.5,
                                        )
                                      : Border.all(
                                          color: Colors.white24,
                                          width: 1,
                                        ),
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
                  const SizedBox(height: 16),
                  FilledButton(
                    key: const Key('save_asset_tags_button'),
                    style: FilledButton.styleFrom(
                      minimumSize: const Size(double.infinity, 48),
                    ),
                    onPressed: isSaveDisabled ? null : _handleSaveTags,
                    child: _isSaving
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Text('Salvar Tags'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTagCheckboxTile(PortfolioTag tag, bool isBusy) {
    final tagColor = TagProvider.parseColor(tag.color);
    final isSelected = _selectedTagIds.contains(tag.id);

    return CheckboxListTile(
      key: Key('asset_tag_checkbox_${tag.id}'),
      contentPadding: EdgeInsets.zero,
      dense: true,
      value: isSelected,
      onChanged: isBusy
          ? null
          : (checked) {
              setState(() {
                _hasUserEditedSelection = true;
                if (checked ?? false) {
                  _selectedTagIds.add(tag.id);
                } else {
                  _selectedTagIds.remove(tag.id);
                }
              });
            },
      title: Row(
        children: [
          Container(
            width: 12,
            height: 12,
            decoration: BoxDecoration(
              color: tagColor,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              tag.name,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: AppTheme.textColor,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
      subtitle: Padding(
        padding: const EdgeInsets.only(left: 22),
        child: Text(
          '${tag.assetCount} ativo(s)',
          style: const TextStyle(color: Colors.white54, fontSize: 12),
        ),
      ),
    );
  }
}
