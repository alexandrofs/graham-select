import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../providers/portfolio_provider.dart';
import '../../domain/entities/custody_position.dart';
import '../../../tags/presentation/providers/tag_provider.dart';
import '../../../tags/presentation/widgets/asset_tag_selector_sheet.dart';

/// Rota de upload centralizada — evita string mágica espalhada no código
const _kUploadRoute = '/upload';

class FinancialDataTable extends StatelessWidget {
  const FinancialDataTable({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<PortfolioProvider>();
    final tagProvider = Provider.of<TagProvider?>(context);

    switch (provider.custodyStatus) {
      case PortfolioStatus.initial:
        return const SizedBox.shrink();
      case PortfolioStatus.loading:
        return const _SkeletonTableWidget();
      case PortfolioStatus.error:
        return _AnomalyAlert(
          message: provider.custodyError ?? 'Erro desconhecido ao carregar custódia',
          onRetry: () => provider.loadCustodyPositions(),
        );
      case PortfolioStatus.success:
        if (provider.custodyPositions.isEmpty) {
          return const _EmptyStateWidget();
        }
        final selectedFilterTag = tagProvider?.selectedFilterTag;
        final allowedTickers = selectedFilterTag == null
            ? null
            : tagProvider!.filteredTickersForSelectedTag;
        final filteredPositions = allowedTickers == null
            ? provider.custodyPositions
            : provider.custodyPositions
                .where(
                  (pos) =>
                      allowedTickers.contains(pos.ticker.trim().toUpperCase()),
                )
                .toList();
        if (filteredPositions.isEmpty && selectedFilterTag != null) {
          return _FilteredEmptyStateWidget(
            tagName: selectedFilterTag.name,
            onClearFilter: () => tagProvider!.selectFilterTag(null),
          );
        }
        return _buildTableContent(
          context,
          provider,
          filteredPositions,
          tagProvider,
        );
    }
  }

  Widget _buildTableContent(
    BuildContext context,
    PortfolioProvider provider,
    List<CustodyPosition> filteredPositions,
    TagProvider? tagProvider,
  ) {
    final currencyFormatter = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final percentFormatter = NumberFormat('+##0.00%;-##0.00%', 'pt_BR');
    final quantityFormatter = NumberFormat('#,##0', 'pt_BR');
    final timeFormatter = DateFormat('HH:mm', 'pt_BR');

    final positions = filteredPositions;
    final isAscending = provider.sortAscending;
    final sortColumn = provider.sortColumn;

    // Usa o timestamp autoritativo do backend (meta.priceUpdatedAt) em vez de calcular localmente
    final latestUpdate = provider.custodyMetaPriceUpdatedAt;
    final hasCachePrice = positions.any((e) => e.priceSource == 'CACHE');

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 16),
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Sua Custódia',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        fontFamily: 'DM Sans',
                      ),
                ),
                if (latestUpdate != null)
                  Row(
                    children: [
                      if (hasCachePrice) ...[
                        const Icon(
                          Icons.warning_amber_rounded,
                          color: Color(0xFFF59E0B),
                          size: 16,
                        ),
                        const SizedBox(width: 4),
                        const Text(
                          'Preço em Cache',
                          style: TextStyle(
                            color: Color(0xFFF59E0B),
                            fontSize: 12,
                            fontFamily: 'Inter',
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ] else ...[
                        const Icon(
                          Icons.check_circle_outline_rounded,
                          color: Color(0xFF10B981),
                          size: 16,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          'Atualizado em ${timeFormatter.format(latestUpdate.toLocal())}',
                          style: const TextStyle(
                            color: Colors.white70,
                            fontSize: 12,
                            fontFamily: 'Inter',
                          ),
                        ),
                      ],
                    ],
                  ),
              ],
            ),
            const SizedBox(height: 16),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Theme(
                data: Theme.of(context).copyWith(
                  dividerColor: Colors.white10,
                ),
                child: DataTable(
                  columnSpacing: 24,
                  horizontalMargin: 8,
                  headingRowHeight: 44,
                  dataRowMinHeight: 48,
                  dataRowMaxHeight: tagProvider != null ? double.infinity : 52,
                  sortColumnIndex: _getColumnIndex(sortColumn),
                  sortAscending: isAscending,
                  columns: [
                    _buildHeader(context, provider, 'ticker', 'Ticker', isNumeric: false),
                    _buildHeader(context, provider, 'quantity', 'Qtd', isNumeric: true),
                    _buildHeader(context, provider, 'averagePrice', 'Preço Médio', isNumeric: true),
                    _buildHeader(context, provider, 'currentPrice', 'Cotação Atual', isNumeric: true),
                    _buildHeader(context, provider, 'marketValue', 'Valor de Mercado', isNumeric: true),
                    _buildHeader(context, provider, 'gainLossPercentage', 'Ganho/Perda %', isNumeric: true),
                    if (tagProvider != null)
                      DataColumn(
                        label: Semantics(
                          header: true,
                          label: 'Tags do ativo',
                          child: const Text(
                            'Tags',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontFamily: 'DM Sans',
                              color: Colors.white70,
                            ),
                          ),
                        ),
                      ),
                  ],
                  rows: positions.map((pos) {
                    final isPositive = pos.gainLossPercentage > 0;
                    final isNegative = pos.gainLossPercentage < 0;

                    final Color semanticColor = isPositive
                        ? const Color(0xFF10B981)
                        : isNegative
                            ? const Color(0xFFEF4444)
                            : Colors.white;

                    return DataRow(
                      cells: [
                        DataCell(
                          Semantics(
                            label: 'Ticker ${pos.ticker}',
                            child: Text(
                              pos.ticker,
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontFamily: 'DM Sans',
                              ),
                            ),
                          ),
                        ),
                        DataCell(
                          Semantics(
                            label: 'Quantidade ${quantityFormatter.format(pos.quantity)}',
                            child: Text(
                              quantityFormatter.format(pos.quantity),
                              style: const TextStyle(fontFamily: 'JetBrains Mono'),
                            ),
                          ),
                        ),
                        DataCell(
                          Semantics(
                            label: 'Preço Médio ${currencyFormatter.format(pos.averagePrice)}',
                            child: Text(
                              currencyFormatter.format(pos.averagePrice),
                              style: const TextStyle(fontFamily: 'JetBrains Mono'),
                            ),
                          ),
                        ),
                        DataCell(
                          Semantics(
                            label: 'Cotação Atual ${currencyFormatter.format(pos.currentPrice)}',
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  currencyFormatter.format(pos.currentPrice),
                                  style: const TextStyle(fontFamily: 'JetBrains Mono'),
                                ),
                                if (pos.priceSource == 'CACHE') ...[
                                  const SizedBox(width: 4),
                                  const Icon(
                                    Icons.warning_amber_rounded,
                                    color: Color(0xFFF59E0B),
                                    size: 14,
                                  ),
                                ],
                              ],
                            ),
                          ),
                        ),
                        DataCell(
                          Semantics(
                            label: 'Valor de Mercado ${currencyFormatter.format(pos.marketValue)}',
                            child: Text(
                              currencyFormatter.format(pos.marketValue),
                              style: const TextStyle(
                                fontFamily: 'JetBrains Mono',
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ),
                        DataCell(
                          Semantics(
                            label: 'Variação percentual ${percentFormatter.format(pos.gainLossPercentage / 100)}',
                            child: Text(
                              percentFormatter.format(pos.gainLossPercentage / 100),
                              style: TextStyle(
                                fontFamily: 'JetBrains Mono',
                                color: semanticColor,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ),
                        if (tagProvider != null)
                          _buildTagsCell(context, tagProvider, pos.ticker),
                      ],
                    );
                  }).toList(),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  DataColumn _buildHeader(
    BuildContext context,
    PortfolioProvider provider,
    String columnId,
    String label, {
    required bool isNumeric,
  }) {
    final isSelected = provider.sortColumn == columnId;
    return DataColumn(
      numeric: isNumeric,
      onSort: (columnIndex, ascending) => provider.sortBy(columnId),
      label: Semantics(
        header: true,
        label: 'Ordenar por $label',
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              label,
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontFamily: 'DM Sans',
                color: isSelected ? Colors.white : Colors.white70,
              ),
            ),
          ],
        ),
      ),
    );
  }

  int _getColumnIndex(String columnId) {
    switch (columnId) {
      case 'ticker':
        return 0;
      case 'quantity':
        return 1;
      case 'averagePrice':
        return 2;
      case 'currentPrice':
        return 3;
      case 'marketValue':
        return 4;
      case 'gainLossPercentage':
        return 5;
      default:
        return 0;
    }
  }

  DataCell _buildTagsCell(
    BuildContext context,
    TagProvider tagProvider,
    String ticker,
  ) {
    final assetTags = tagProvider.tagsForTicker(ticker);
    final visibleTags = assetTags.take(3).toList();
    final overflowCount = assetTags.length - visibleTags.length;

    return DataCell(
      Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (visibleTags.isNotEmpty)
              Flexible(
                fit: FlexFit.loose,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 220),
                  child: Wrap(
                    spacing: 4,
                    runSpacing: 4,
                    children: [
                      ...visibleTags.map((tag) {
                        final color = TagProvider.parseColor(tag.color);
                        return Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: color.withValues(alpha: 0.16),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: color.withValues(alpha: 0.6),
                            ),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Container(
                                width: 7,
                                height: 7,
                                decoration: BoxDecoration(
                                  color: color,
                                  shape: BoxShape.circle,
                                ),
                              ),
                              const SizedBox(width: 5),
                              Flexible(
                                fit: FlexFit.loose,
                                child: Text(
                                  tag.name,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontSize: 11,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        );
                      }),
                      if (overflowCount > 0)
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 7,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: 0.08),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: Colors.white24),
                          ),
                          child: Text(
                            '+$overflowCount',
                            style: const TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.w600,
                              color: Colors.white70,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              )
            else
              const Text(
                'Sem tag',
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.white38,
                  fontFamily: 'Inter',
                ),
              ),
            const SizedBox(width: 4),
            Semantics(
              label: 'Gerenciar tags de $ticker',
              child: IconButton(
                key: Key('manage_asset_tags_$ticker'),
                tooltip: 'Gerenciar tags',
                icon: Icon(
                  visibleTags.isEmpty
                      ? Icons.add_circle_outline
                      : Icons.edit_outlined,
                  size: 18,
                  color: Colors.white70,
                ),
                onPressed: () {
                  showModalBottomSheet<bool>(
                    context: context,
                    isScrollControlled: true,
                    backgroundColor: Colors.transparent,
                    builder: (_) => ChangeNotifierProvider<TagProvider>.value(
                      value: tagProvider,
                      child: AssetTagSelectorSheet(ticker: ticker),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SkeletonTableWidget extends StatefulWidget {
  const _SkeletonTableWidget();

  @override
  State<_SkeletonTableWidget> createState() => _SkeletonTableWidgetState();
}

class _SkeletonTableWidgetState extends State<_SkeletonTableWidget>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();
    _animation = Tween<double>(begin: -1.5, end: 2.5).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Widget _shimmerBlock({double width = 60, double height = 16}) {
    return AnimatedBuilder(
      animation: _animation,
      builder: (context, _) {
        // Converte o valor da animação (-1.5 a 2.5) para opacidade (0.05 a 0.20)
        // usando uma curva senoidal para criar o efeito de pulso shimmer
        final t = ((_animation.value + 1.5) / 4.0).clamp(0.0, 1.0);
        final opacity = 0.05 + 0.15 * (1 - (2 * t - 1).abs());
        return Container(
          width: width,
          height: height,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(4),
            color: Colors.white.withValues(alpha: opacity),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 16),
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _shimmerBlock(width: 150, height: 24),
            const SizedBox(height: 24),
            // Header skeleton
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: List.generate(
                6,
                (index) => _shimmerBlock(width: 60, height: 20),
              ),
            ),
            const Divider(color: Colors.white10, height: 24),
            // Rows skeleton — 5 linhas imitando layout da tabela
            Column(
              children: List.generate(
                5,
                (rowIndex) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12.0),
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: List.generate(
                        6,
                        (colIndex) => Padding(
                          padding: const EdgeInsets.only(right: 24),
                          child: _shimmerBlock(
                            width: colIndex == 0 ? 50 : 65,
                            height: 16,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}



class _EmptyStateWidget extends StatelessWidget {
  const _EmptyStateWidget();

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 40.0, horizontal: 24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.folder_open_outlined,
              size: 64,
              color: Colors.white30,
            ),
            const SizedBox(height: 16),
            Text(
              'Nenhum ativo encontrado',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                    fontFamily: 'DM Sans',
                  ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Sua carteira de investimentos está vazia no momento.\nFaça upload do seu extrato de operações para ver sua custódia detalhada.',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.white54,
                fontSize: 14,
                fontFamily: 'Inter',
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () => context.go(_kUploadRoute),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF1B2A4A),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              icon: const Icon(Icons.upload_file),
              label: const Text(
                'Fazer upload do arquivo B3 para começar',
                style: TextStyle(fontFamily: 'DM Sans', fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AnomalyAlert extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _AnomalyAlert({
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFEF4444).withValues(alpha: 0.1),
        border: Border.all(color: const Color(0xFFEF4444).withValues(alpha: 0.3)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.error_outline_rounded,
            color: Color(0xFFEF4444),
            size: 24,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'Ocorreu uma anomalia',
                  style: TextStyle(
                    color: Color(0xFFEF4444),
                    fontWeight: FontWeight.bold,
                    fontFamily: 'DM Sans',
                    fontSize: 16,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  message,
                  style: const TextStyle(
                    color: Colors.white70,
                    fontFamily: 'Inter',
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 12),
                TextButton.icon(
                  onPressed: onRetry,
                  style: TextButton.styleFrom(
                    foregroundColor: const Color(0xFFEF4444),
                    padding: EdgeInsets.zero,
                  ),
                  icon: const Icon(Icons.refresh_rounded, size: 18),
                  label: const Text(
                    'Tentar novamente',
                    style: TextStyle(fontFamily: 'DM Sans', fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _FilteredEmptyStateWidget extends StatelessWidget {
  const _FilteredEmptyStateWidget({
    required this.tagName,
    required this.onClearFilter,
  });

  final String tagName;
  final VoidCallback onClearFilter;

  @override
  Widget build(BuildContext context) {
    return Card(
      key: const Key('filtered_tags_empty_state'),
      margin: const EdgeInsets.symmetric(vertical: 16),
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 36, horizontal: 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.filter_list_off_rounded,
              size: 40,
              color: Color(0xFF6366F1),
            ),
            const SizedBox(height: 12),
            const Text(
              'Nenhum ativo associado à tag selecionada',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'DM Sans',
                fontWeight: FontWeight.bold,
                fontSize: 16,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'A tag "$tagName" ainda não possui ativos da sua custódia vinculados.',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Inter',
                fontSize: 13,
                color: Colors.white70,
              ),
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              key: const Key('clear_tag_filter_button'),
              style: OutlinedButton.styleFrom(
                minimumSize: const Size(48, 48),
                padding: const EdgeInsets.symmetric(horizontal: 16),
              ),
              onPressed: onClearFilter,
              icon: const Icon(Icons.clear_all, size: 18),
              label: const Text('Limpar filtro'),
            ),
          ],
        ),
      ),
    );
  }
}
