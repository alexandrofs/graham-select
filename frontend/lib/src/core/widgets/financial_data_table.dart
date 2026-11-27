import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:frontend/src/core/theme/kerno_accessibility.dart';
import 'package:frontend/src/core/theme/kerno_tokens.dart';

/// Tabela financeira responsiva que renderiza `ListView` de cartões verticais
/// empilhados em viewports `< 600dp` (eliminando scroll horizontal no mobile)
/// e `DataTable` tabular em viewports `>= 600dp`.
class FinancialDataTable<T> extends StatelessWidget {
  static const double mobileBreakpoint = 600.0;

  final List<T> items;
  final List<DataColumn> columns;
  final DataRow Function(T item) rowBuilder;
  final String? emptyMessage;
  final String? regulatoryFooter;

  const FinancialDataTable({
    super.key,
    required this.items,
    required this.columns,
    required this.rowBuilder,
    this.emptyMessage,
    this.regulatoryFooter,
  });

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    if (items.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(KernoSpacing.s6),
          child: Text(
            emptyMessage ?? 'Nenhum dado encontrado',
            textAlign: TextAlign.center,
            style: KernoTypography.bodyBase(color: tokens.textSecondary),
          ),
        ),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final isMobile = constraints.maxWidth < mobileBreakpoint;
        if (isMobile) {
          return _buildMobileCardList(context, tokens);
        }
        return _buildDesktopTable(
          context,
          tokens,
          hasBoundedWidth: constraints.hasBoundedWidth,
        );
      },
    );
  }

  Widget _buildMobileCardList(BuildContext context, KernoThemeData tokens) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        ListView.separated(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: items.length,
          separatorBuilder: (_, _) => const SizedBox(height: KernoSpacing.s3),
          itemBuilder: (context, index) {
            final row = rowBuilder(items[index]);
            final fieldCount = math.min(columns.length, row.cells.length);
            final VoidCallback? onCardTap = row.onSelectChanged != null
                ? () => row.onSelectChanged!(!row.selected)
                : null;

            final cardContent = Container(
              padding: const EdgeInsets.all(KernoSpacing.cardPaddingMobile),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(KernoRadius.lg),
                border: Border.all(color: tokens.borderSubtle),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisSize: MainAxisSize.min,
                children: List<Widget>.generate(fieldCount, (cellIndex) {
                  final cell = row.cells[cellIndex];
                  final columnLabel = DefaultTextStyle.merge(
                    style: KernoTypography.bodySm(color: tokens.textSecondary),
                    child: columns[cellIndex].label,
                  );
                  Widget cellValue = DefaultTextStyle.merge(
                    style: KernoTypography.bodyBase(
                      color: tokens.textPrimary,
                    ).copyWith(fontFeatures: KernoTypography.tabularFeatures),
                    child: cell.child,
                  );
                  if (cell.onTap != null) {
                    cellValue = InkWell(
                      borderRadius: BorderRadius.circular(KernoRadius.sm),
                      onTap: cell.onTap,
                      child: cellValue,
                    );
                  }

                  return Padding(
                    padding: EdgeInsets.only(
                      bottom: cellIndex < fieldCount - 1 ? KernoSpacing.s2 : 0,
                    ),
                    child: ResponsiveMetricRow(
                      leading: columnLabel,
                      trailing: cellValue,
                    ),
                  );
                }),
              ),
            );

            return Material(
              color: tokens.surfaceElevated,
              borderRadius: BorderRadius.circular(KernoRadius.lg),
              child: (onCardTap != null || row.onLongPress != null)
                  ? InkWell(
                      borderRadius: BorderRadius.circular(KernoRadius.lg),
                      onTap: onCardTap,
                      onLongPress: row.onLongPress,
                      child: cardContent,
                    )
                  : cardContent,
            );
          },
        ),
        if (regulatoryFooter != null) _buildFooter(tokens),
      ],
    );
  }

  Widget _buildDesktopTable(
    BuildContext context,
    KernoThemeData tokens, {
    required bool hasBoundedWidth,
  }) {
    return Column(
      crossAxisAlignment: hasBoundedWidth
          ? CrossAxisAlignment.stretch
          : CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Container(
          decoration: BoxDecoration(
            color: tokens.surfaceElevated,
            borderRadius: BorderRadius.circular(KernoRadius.lg),
            border: Border.all(color: tokens.borderSubtle),
          ),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: DataTable(
              headingTextStyle: KernoTypography.bodySm(
                color: tokens.textSecondary,
              ).copyWith(fontWeight: FontWeight.w600),
              dataTextStyle: KernoTypography.bodyBase(
                color: tokens.textPrimary,
              ).copyWith(fontFeatures: KernoTypography.tabularFeatures),
              columns: columns,
              rows: items.map((item) => rowBuilder(item)).toList(growable: false),
            ),
          ),
        ),
        if (regulatoryFooter != null) _buildFooter(tokens),
      ],
    );
  }

  Widget _buildFooter(KernoThemeData tokens) {
    return Padding(
      padding: const EdgeInsets.only(top: KernoSpacing.s3),
      child: Text(
        regulatoryFooter!,
        style: KernoTypography.caption(color: tokens.textMuted),
      ),
    );
  }
}
