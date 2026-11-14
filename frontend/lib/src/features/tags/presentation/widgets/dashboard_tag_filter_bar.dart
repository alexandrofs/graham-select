import 'package:flutter/material.dart';

import 'analytical_lens_tabs.dart';

/// Barra de filtro por tags customizadas exibida acima da tabela de ativos
/// no Dashboard. Delega integralmente para [AnalyticalLensTabs], preservando
/// a chave raiz `dashboard_tag_filter_bar`.
class DashboardTagFilterBar extends StatelessWidget {
  const DashboardTagFilterBar({
    super.key,
    this.onTagSelected,
  });

  final ValueChanged<String?>? onTagSelected;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      key: const Key('dashboard_tag_filter_bar'),
      width: double.infinity,
      child: AnalyticalLensTabs(onTagSelected: onTagSelected),
    );
  }
}
