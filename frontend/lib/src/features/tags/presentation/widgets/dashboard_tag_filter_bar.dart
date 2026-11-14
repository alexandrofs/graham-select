import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/theme/app_theme.dart';
import '../providers/tag_provider.dart';

/// Barra de filtro por tags customizadas exibida acima da tabela de ativos
/// no Dashboard.
class DashboardTagFilterBar extends StatelessWidget {
  const DashboardTagFilterBar({super.key});

  @override
  Widget build(BuildContext context) {
    final tagProvider = context.watch<TagProvider>();
    final selectedId = tagProvider.selectedFilterTagId;

    return Container(
      key: const Key('dashboard_tag_filter_bar'),
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
                Icons.label_outline,
                size: 18,
                color: AppTheme.primaryColor,
              ),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Filtrar ativos por Tag',
                  style: TextStyle(
                    color: AppTheme.textColor,
                    fontWeight: FontWeight.w600,
                    fontSize: 14,
                  ),
                ),
              ),
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
            LayoutBuilder(
              builder: (context, constraints) {
                final maxLabelWidth =
                    (constraints.maxWidth - 48).clamp(80.0, 280.0).toDouble();
                return Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    Semantics(
                      label: 'Mostrar todos os ativos',
                      child: ChoiceChip(
                        key: const Key('tag_filter_all'),
                        label: const Text('Todas'),
                        selected: selectedId == null,
                        onSelected: (_) => tagProvider.selectFilterTag(null),
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

                      return Semantics(
                        label: 'Filtrar por tag ${tag.name}',
                        child: ChoiceChip(
                          key: Key('tag_filter_${tag.id}'),
                          selected: isSelected,
                          onSelected: (_) {
                            tagProvider.selectFilterTag(
                              isSelected ? null : tag.id,
                            );
                          },
                          selectedColor: tagColor.withValues(alpha: 0.22),
                          side: BorderSide(
                            color: isSelected
                                ? tagColor
                                : Colors.white.withValues(alpha: 0.14),
                          ),
                          label: ConstrainedBox(
                            constraints: BoxConstraints(maxWidth: maxLabelWidth),
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
                              ],
                            ),
                          ),
                        ),
                      );
                    }),
                  ],
                );
              },
            ),
        ],
      ),
    );
  }
}
