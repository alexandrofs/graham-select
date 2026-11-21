import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/theme/app_theme.dart';
import '../providers/tag_provider.dart';
import 'asset_tag_selector_sheet.dart';

/// Mixin compartilhado pelas telas analíticas para manter o carregamento
/// inicial e recargas reativas sincronizados com [TagProvider.selectedFilterTagId]
/// (incluindo deep links e alternâncias rápidas entre lentes).
mixin TagLensSyncMixin<T extends StatefulWidget> on State<T> {
  TagProvider? _syncedTagProvider;
  String? _syncedLensTagId;
  bool _hasCompletedInitialLensLoad = false;

  String? get currentLensTagId => _syncedTagProvider?.selectedFilterTagId;

  @protected
  Future<void> onLensChanged(String? tagId);

  @protected
  void onLensPostFrameInit() {
    if (!mounted) return;
    final tagProvider =
        _syncedTagProvider ?? Provider.of<TagProvider?>(context, listen: false);
    if (tagProvider != null && _syncedTagProvider != tagProvider) {
      _syncedTagProvider?.removeListener(_handleTagLensChange);
      _syncedTagProvider = tagProvider;
      _syncedTagProvider?.addListener(_handleTagLensChange);
    }
    _syncedLensTagId = tagProvider?.selectedFilterTagId;
    _hasCompletedInitialLensLoad = true;
    if (tagProvider != null) {
      tagProvider.loadTags(silent: tagProvider.tags.isNotEmpty);
    }
    onLensChanged(_syncedLensTagId);
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => onLensPostFrameInit());
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final nextTagProvider = Provider.of<TagProvider?>(context);
    if (_syncedTagProvider != nextTagProvider) {
      _syncedTagProvider?.removeListener(_handleTagLensChange);
      _syncedTagProvider = nextTagProvider;
      _syncedTagProvider?.addListener(_handleTagLensChange);
      if (_hasCompletedInitialLensLoad &&
          _syncedTagProvider?.selectedFilterTagId != _syncedLensTagId) {
        _syncedLensTagId = _syncedTagProvider?.selectedFilterTagId;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            onLensChanged(_syncedLensTagId);
          }
        });
      }
    }
  }

  void _handleTagLensChange() {
    if (!mounted || !_hasCompletedInitialLensLoad) return;
    final currentTagId = _syncedTagProvider?.selectedFilterTagId;
    if (currentTagId != _syncedLensTagId) {
      _syncedLensTagId = currentTagId;
      onLensChanged(currentTagId);
    }
  }

  @override
  void dispose() {
    _syncedTagProvider?.removeListener(_handleTagLensChange);
    super.dispose();
  }
}

/// Seletor unificado de lente analítica no topo das telas de análise.
///
/// Exibe `Visão Geral` por padrão (`selectedFilterTagId == null`) seguida pelas
/// Tags configuradas com cor e contagem de ativos, sincronizando o estado via
/// [TagProvider.selectedFilterTagId].
class AnalyticalLensTabs extends StatelessWidget {
  const AnalyticalLensTabs({
    super.key,
    this.onTagSelected,
    this.showManageButton = true,
  });

  final ValueChanged<String?>? onTagSelected;
  final bool showManageButton;

  void _selectTag(TagProvider tagProvider, String? tagId) {
    tagProvider.selectFilterTag(tagId);
    onTagSelected?.call(tagId);
  }

  @override
  Widget build(BuildContext context) {
    final tagProvider = Provider.of<TagProvider?>(context);
    if (tagProvider == null) {
      return const SizedBox.shrink();
    }

    final selectedId = tagProvider.selectedFilterTagId;

    return Container(
      key: const Key('analytical_lens_tabs'),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(
                Icons.layers_outlined,
                size: 18,
                color: AppTheme.primaryColor,
              ),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Lente Analítica',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: AppTheme.textColor,
                    fontWeight: FontWeight.w600,
                    fontSize: 14,
                    fontFamily: 'DM Sans',
                  ),
                ),
              ),
              if (showManageButton)
                TextButton.icon(
                  key: const Key('dashboard_manage_tags_button'),
                  style: TextButton.styleFrom(
                    minimumSize: const Size(48, 48),
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                  ),
                  onPressed: () => context.push('/tags'),
                  icon: const Icon(Icons.tune, size: 18),
                  label: const Text('Gerenciar Tags'),
                ),
            ],
          ),
          const SizedBox(height: 8),
          if (tagProvider.state == TagState.error && tagProvider.tags.isEmpty)
            Row(
              children: [
                Icon(
                  Icons.error_outline,
                  size: 18,
                  color: Theme.of(context).colorScheme.error,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    tagProvider.errorMessage ?? 'Erro ao carregar tags.',
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.error,
                      fontSize: 13,
                    ),
                  ),
                ),
                TextButton.icon(
                  key: const Key('tag_filter_retry_button'),
                  style: TextButton.styleFrom(
                    minimumSize: const Size(48, 48),
                  ),
                  onPressed: () => tagProvider.loadTags(),
                  icon: const Icon(Icons.refresh, size: 18),
                  label: const Text('Tentar novamente'),
                ),
              ],
            )
          else
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  Semantics(
                    selected: selectedId == null,
                    label: 'Mostrar visão geral da carteira',
                    child: ChoiceChip(
                      key: const Key('tag_filter_all'),
                      label: const Text('Visão Geral'),
                      selected: selectedId == null,
                      materialTapTargetSize: MaterialTapTargetSize.padded,
                      onSelected: (_) => _selectTag(tagProvider, null),
                      selectedColor: AppTheme.primaryColor.withValues(alpha: 0.25),
                      side: BorderSide(
                        color: selectedId == null
                            ? AppTheme.primaryColor
                            : Colors.white.withValues(alpha: 0.14),
                      ),
                    ),
                  ),
                  ...tagProvider.tags.map((tag) {
                    final isSelected = selectedId == tag.id;
                    final tagColor = TagProvider.parseColor(tag.color);

                    return Padding(
                      padding: const EdgeInsets.only(left: 8),
                      child: Semantics(
                        selected: isSelected,
                        label: 'Filtrar por tag ${tag.name}, ${tag.assetCount} ativos',
                        child: ChoiceChip(
                          key: Key('tag_filter_${tag.id}'),
                          selected: isSelected,
                          materialTapTargetSize: MaterialTapTargetSize.padded,
                          onSelected: (_) {
                            _selectTag(tagProvider, isSelected ? null : tag.id);
                          },
                          selectedColor: tagColor.withValues(alpha: 0.22),
                          side: BorderSide(
                            color: isSelected
                                ? tagColor
                                : Colors.white.withValues(alpha: 0.14),
                          ),
                          label: ConstrainedBox(
                            constraints: const BoxConstraints(maxWidth: 220),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Container(
                                  width: 10,
                                  height: 10,
                                  decoration: BoxDecoration(
                                    color: tagColor,
                                    shape: BoxShape.circle,
                                  ),
                                ),
                                const SizedBox(width: 6),
                                Flexible(
                                  child: Text(
                                    tag.name,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                                const SizedBox(width: 6),
                                Container(
                                  key: Key('tag_lens_count_${tag.id}'),
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 6,
                                    vertical: 1,
                                  ),
                                  decoration: BoxDecoration(
                                    color: Colors.white.withValues(alpha: 0.10),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Text(
                                    '${tag.assetCount}',
                                    style: const TextStyle(
                                      fontSize: 11,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.white70,
                                      fontFamily: 'JetBrains Mono',
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    );
                  }),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

/// Badge explícito exibido quando uma lente de Tag está ativa no contexto.
class ActiveTagLensBadge extends StatelessWidget {
  const ActiveTagLensBadge({
    super.key,
    this.onClear,
  });

  final VoidCallback? onClear;

  @override
  Widget build(BuildContext context) {
    final tagProvider = Provider.of<TagProvider?>(context);
    final selectedTag = tagProvider?.selectedFilterTag;
    if (tagProvider == null || selectedTag == null) {
      return const SizedBox.shrink();
    }

    final tagColor = TagProvider.parseColor(selectedTag.color);

    return Container(
      key: const Key('active_tag_lens_badge'),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: tagColor.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: tagColor.withValues(alpha: 0.55)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 10,
            height: 10,
            decoration: BoxDecoration(
              color: tagColor,
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              'Visualizando: ${selectedTag.name}',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
                fontSize: 13,
                fontFamily: 'DM Sans',
              ),
            ),
          ),
          const SizedBox(width: 8),
          TextButton.icon(
            key: const Key('active_tag_lens_clear_button'),
            style: TextButton.styleFrom(
              minimumSize: const Size(48, 40),
              padding: const EdgeInsets.symmetric(horizontal: 8),
              foregroundColor: Colors.white,
            ),
            onPressed: () {
              tagProvider.selectFilterTag(null);
              onClear?.call();
            },
            icon: const Icon(Icons.close_rounded, size: 16),
            label: const Text(
              'Visão Geral',
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

/// Bottom sheet rápido para associar ativos da custódia que ainda não possuem
/// nenhuma Tag sem perder o contexto da lente ativa no Dashboard.
class UntaggedAssetsQuickSheet extends StatefulWidget {
  const UntaggedAssetsQuickSheet({
    super.key,
    required this.untaggedTickers,
    this.onTagsUpdated,
  });

  final List<String> untaggedTickers;
  final Future<void> Function()? onTagsUpdated;

  static Future<void> show(
    BuildContext context, {
    required List<String> untaggedTickers,
    required TagProvider tagProvider,
    Future<void> Function()? onTagsUpdated,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => ChangeNotifierProvider<TagProvider>.value(
        value: tagProvider,
        child: UntaggedAssetsQuickSheet(
          untaggedTickers: untaggedTickers,
          onTagsUpdated: onTagsUpdated,
        ),
      ),
    );
  }

  @override
  State<UntaggedAssetsQuickSheet> createState() => _UntaggedAssetsQuickSheetState();
}

class _UntaggedAssetsQuickSheetState extends State<UntaggedAssetsQuickSheet> {
  Future<void> _openAssetSelector(BuildContext context, String ticker) async {
    final tagProvider = context.read<TagProvider>();
    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => ChangeNotifierProvider<TagProvider>.value(
        value: tagProvider,
        child: AssetTagSelectorSheet(ticker: ticker),
      ),
    );
    if (saved == true && widget.onTagsUpdated != null) {
      await widget.onTagsUpdated!();
    }
  }

  @override
  Widget build(BuildContext context) {
    final tagProvider = context.watch<TagProvider>();
    final remaining = widget.untaggedTickers
        .map((t) => t.trim().toUpperCase())
        .where((t) => t.isNotEmpty && tagProvider.tagsForTicker(t).isEmpty)
        .toList();

    return SafeArea(
      child: Material(
        color: AppTheme.surfaceColor,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        child: Container(
          key: const Key('untagged_assets_quick_sheet'),
          constraints: BoxConstraints(
            maxHeight: MediaQuery.of(context).size.height * 0.75,
          ),
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Icon(
                  Icons.label_off_outlined,
                  color: Color(0xFFF59E0B),
                  size: 20,
                ),
                const SizedBox(width: 10),
                const Expanded(
                  child: Text(
                    'Ativos sem Tag',
                    style: TextStyle(
                      fontFamily: 'DM Sans',
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                      color: AppTheme.textColor,
                    ),
                  ),
                ),
                IconButton(
                  tooltip: 'Fechar',
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close_rounded, color: Colors.white70),
                ),
              ],
            ),
            const SizedBox(height: 6),
            const Text(
              'Toque em um ativo para associá-lo às suas tags sem sair da visão atual.',
              style: TextStyle(
                fontFamily: 'Inter',
                fontSize: 13,
                color: Colors.white70,
              ),
            ),
            const SizedBox(height: 16),
            if (remaining.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 20),
                child: Column(
                  children: [
                    const Icon(
                      Icons.check_circle_outline_rounded,
                      color: Color(0xFF10B981),
                      size: 40,
                    ),
                    const SizedBox(height: 10),
                    const Text(
                      'Todos os ativos listados já possuem tags associadas.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: 'Inter',
                        fontSize: 14,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 16),
                    FilledButton(
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text('Concluir'),
                    ),
                  ],
                ),
              )
            else
              Flexible(
                child: ListView.separated(
                  shrinkWrap: true,
                  itemCount: remaining.length,
                  separatorBuilder: (_, _) => const Divider(color: Colors.white12),
                  itemBuilder: (context, index) {
                    final ticker = remaining[index];
                    return ListTile(
                      key: Key('untagged_asset_item_$ticker'),
                      minVerticalPadding: 12,
                      leading: const Icon(
                        Icons.sell_outlined,
                        color: AppTheme.primaryColor,
                      ),
                      title: Text(
                        ticker,
                        style: const TextStyle(
                          fontFamily: 'JetBrains Mono',
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                      subtitle: const Text(
                        'Sem tag associada',
                        style: TextStyle(
                          fontFamily: 'Inter',
                          fontSize: 12,
                          color: Colors.white54,
                        ),
                      ),
                      trailing: const Icon(
                        Icons.add_circle_outline,
                        color: Colors.white70,
                      ),
                      onTap: () => _openAssetSelector(context, ticker),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
      ),
    );
  }
}
