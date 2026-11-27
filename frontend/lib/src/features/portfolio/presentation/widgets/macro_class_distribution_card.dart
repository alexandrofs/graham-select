import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../../core/security/privacy_provider.dart';
import '../../../../core/theme/kerno_accessibility.dart';
import '../../../../core/theme/kerno_tokens.dart';
import '../../domain/entities/custody_position.dart';

/// As 5 Macro Classes oficiais do Kerno + fallback `Outros`.
enum MacroAssetClass {
  acoesBr('Ações BR', Color(0xFF059669)),
  fiis('FIIs', Color(0xFF4F46E5)),
  rendaFixa('Renda Fixa / Tesouro', Color(0xFFD97706)),
  internacional('Internacional', Color(0xFF7C3AED)),
  criptoativos('Criptoativos', Color(0xFF0284C7)),
  outros('Outros', Color(0xFF64748B));

  const MacroAssetClass(this.label, this.accentColor);

  final String label;
  final Color accentColor;

  /// Normaliza strings legadas de [CustodyPosition.assetClass] para uma das
  /// 5 Macro Classes oficiais (ou [outros] quando não reconhecida).
  static MacroAssetClass fromRawAssetClass(String raw) {
    final normalized = raw.trim().toLowerCase();
    if (normalized.isEmpty) return MacroAssetClass.outros;

    if (normalized.contains('cripto') ||
        normalized.contains('crypto') ||
        normalized.contains('btc') ||
        normalized.contains('eth')) {
      return MacroAssetClass.criptoativos;
    }
    if (normalized.contains('renda fixa') ||
        normalized.contains('tesouro') ||
        normalized.contains('cdb') ||
        normalized.contains('lci') ||
        normalized.contains('lca') ||
        normalized.contains('debênture') ||
        normalized.contains('debenture')) {
      return MacroAssetClass.rendaFixa;
    }
    if (normalized.contains('fii') || normalized.contains('imobili')) {
      return MacroAssetClass.fiis;
    }
    if (normalized.contains('ação') ||
        normalized.contains('acoes') ||
        normalized.contains('ações') ||
        normalized.contains('acao')) {
      return MacroAssetClass.acoesBr;
    }
    if (normalized.contains('bdr') ||
        normalized.contains('internacional') ||
        normalized.contains('exterior') ||
        normalized.contains('stock') ||
        normalized.contains('reit') ||
        normalized.contains('etf')) {
      return MacroAssetClass.internacional;
    }
    return MacroAssetClass.outros;
  }
}

class _MacroBucket {
  final MacroAssetClass macroClass;
  double totalValue = 0.0;
  final Map<String, double> subclasses = <String, double>{};

  _MacroBucket(this.macroClass);
}

/// Card de distribuição patrimonial por 5 Macro Classes com barras horizontais
/// duplas (`% Atual` e marcador opcional `% Meta`), chips expansíveis de
/// Subclasses sem rolagem horizontal no mobile e máscara de Modo Privacidade.
class MacroClassDistributionCard extends StatefulWidget {
  final List<CustodyPosition> positions;
  final Map<String, double>? targetPercentages;

  const MacroClassDistributionCard({
    super.key,
    required this.positions,
    this.targetPercentages,
  });

  @override
  State<MacroClassDistributionCard> createState() =>
      _MacroClassDistributionCardState();
}

class _MacroClassDistributionCardState
    extends State<MacroClassDistributionCard> {
  final Set<MacroAssetClass> _expandedClasses = <MacroAssetClass>{};

  @override
  Widget build(BuildContext context) {
    if (widget.positions.isEmpty) {
      return const SizedBox.shrink();
    }

    final Map<MacroAssetClass, _MacroBucket> buckets =
        <MacroAssetClass, _MacroBucket>{};
    double totalMarketValue = 0.0;

    for (final pos in widget.positions) {
      if (!pos.marketValue.isFinite || pos.marketValue <= 0) continue;
      final macro = MacroAssetClass.fromRawAssetClass(pos.assetClass);
      final bucket = buckets.putIfAbsent(macro, () => _MacroBucket(macro));
      bucket.totalValue += pos.marketValue;
      final subclassLabel =
          pos.assetClass.trim().isEmpty ? macro.label : pos.assetClass.trim();
      bucket.subclasses[subclassLabel] =
          (bucket.subclasses[subclassLabel] ?? 0.0) + pos.marketValue;
      totalMarketValue += pos.marketValue;
    }

    if (totalMarketValue <= 0 || buckets.isEmpty) {
      return const SizedBox.shrink();
    }

    final tokens = KernoThemeData.of(context);
    final privacyProvider = Provider.of<PrivacyProvider?>(context);
    final isPrivacyMode = privacyProvider?.isPrivacyMode ?? false;
    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final percentFormat = NumberFormat('#0.0', 'pt_BR');

    const officialOrder = <MacroAssetClass>[
      MacroAssetClass.acoesBr,
      MacroAssetClass.fiis,
      MacroAssetClass.rendaFixa,
      MacroAssetClass.internacional,
      MacroAssetClass.criptoativos,
      MacroAssetClass.outros,
    ];

    final visibleBuckets = officialOrder
        .where((macro) =>
            macro != MacroAssetClass.outros || buckets.containsKey(macro))
        .map((macro) => buckets[macro] ?? _MacroBucket(macro))
        .toList(growable: false);

    return Container(
      key: const Key('macro_class_distribution_card'),
      width: double.infinity,
      padding: const EdgeInsets.all(KernoSpacing.cardPaddingDesktop),
      decoration: BoxDecoration(
        color: tokens.surfaceElevated,
        borderRadius: BorderRadius.circular(KernoRadius.lg),
        border: Border.all(color: tokens.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            'Distribuição por Macro Classes',
            style: KernoTypography.heading3(color: tokens.textPrimary),
          ),
          const SizedBox(height: KernoSpacing.s1),
          Text(
            'Toque em uma classe para expandir subclasses sem sair da tela.',
            style: KernoTypography.bodySm(color: tokens.textSecondary),
          ),
          const SizedBox(height: KernoSpacing.s4),
          ...visibleBuckets.map((bucket) {
            final actualPct =
                ((bucket.totalValue / totalMarketValue) * 100.0).clamp(0.0, 100.0);
            final rawTarget = widget.targetPercentages?[bucket.macroClass.label];
            final targetPct =
                (rawTarget != null && rawTarget.isFinite) ? rawTarget : null;
            final isExpanded = _expandedClasses.contains(bucket.macroClass);
            final hasSubclasses = bucket.subclasses.isNotEmpty;

            final formattedValue = isPrivacyMode
                ? PrivacyProvider.maskedCurrencyValue
                : currencyFormat.format(bucket.totalValue);
            final formattedPct = '${percentFormat.format(actualPct)}%';

            return Padding(
              padding: const EdgeInsets.only(bottom: KernoSpacing.s3),
              child: InkWell(
                key: Key('macro_class_tile_${bucket.macroClass.name}'),
                borderRadius: BorderRadius.circular(KernoRadius.md),
                onTap: hasSubclasses
                    ? () {
                        setState(() {
                          if (isExpanded) {
                            _expandedClasses.remove(bucket.macroClass);
                          } else {
                            _expandedClasses.add(bucket.macroClass);
                          }
                        });
                      }
                    : null,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(
                    minHeight: KernoSpacing.touchTargetMin,
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: KernoSpacing.s2,
                      vertical: KernoSpacing.s2,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        ResponsiveMetricRow(
                          leading: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              Container(
                                width: 10,
                                height: 10,
                                decoration: BoxDecoration(
                                  color: bucket.macroClass.accentColor,
                                  shape: BoxShape.circle,
                                ),
                              ),
                              const SizedBox(width: KernoSpacing.s2),
                              Flexible(
                                child: Text(
                                  bucket.macroClass.label,
                                  softWrap: true,
                                  style: KernoTypography.bodyBase(
                                    color: tokens.textPrimary,
                                  ).copyWith(fontWeight: FontWeight.w600),
                                ),
                              ),
                              if (hasSubclasses) ...[
                                const SizedBox(width: KernoSpacing.s1),
                                Icon(
                                  isExpanded
                                      ? Icons.expand_less_rounded
                                      : Icons.expand_more_rounded,
                                  size: 18,
                                  color: tokens.textMuted,
                                ),
                              ],
                            ],
                          ),
                          trailing: Wrap(
                            spacing: KernoSpacing.s2,
                            crossAxisAlignment: WrapCrossAlignment.center,
                            children: <Widget>[
                              Semantics(
                                label: isPrivacyMode
                                    ? PrivacyProvider.maskedSemanticsLabel
                                    : '${bucket.macroClass.label}: ${currencyFormat.format(bucket.totalValue)}',
                                excludeSemantics: isPrivacyMode,
                                child: KernoMetricText(
                                  formattedValue,
                                  style: KernoTypography.bodySm(
                                    color: tokens.textSecondary,
                                  ),
                                ),
                              ),
                              KernoMetricText(
                                formattedPct,
                                style: KernoTypography.bodySm(
                                  color: tokens.textPrimary,
                                ).copyWith(fontWeight: FontWeight.w700),
                              ),
                              if (targetPct != null)
                                Text(
                                  'Meta ${percentFormat.format(targetPct.clamp(0.0, 100.0))}%',
                                  style: KernoTypography.caption(
                                    color: tokens.textMuted,
                                  ),
                                ),
                            ],
                          ),
                        ),
                        const SizedBox(height: KernoSpacing.s2),
                        _DualAllocationBar(
                          actualFraction: (actualPct / 100.0).clamp(0.0, 1.0),
                          targetFraction: targetPct == null
                              ? null
                              : (targetPct / 100.0).clamp(0.0, 1.0),
                          barColor: bucket.macroClass.accentColor,
                          trackColor: tokens.surfaceSubtle,
                          markerColor: tokens.textPrimary,
                        ),
                        if (isExpanded && hasSubclasses) ...[
                          const SizedBox(height: KernoSpacing.s3),
                          Wrap(
                            spacing: KernoSpacing.s2,
                            runSpacing: KernoSpacing.s2,
                            children: bucket.subclasses.entries.map((entry) {
                              final subPct = ((entry.value / totalMarketValue) *
                                      100.0)
                                  .clamp(0.0, 100.0);
                              final subValueStr = isPrivacyMode
                                  ? PrivacyProvider.maskedCurrencyValue
                                  : currencyFormat.format(entry.value);
                              return Semantics(
                                label: isPrivacyMode
                                    ? '${entry.key} ${percentFormat.format(subPct)}%'
                                    : '${entry.key} $subValueStr (${percentFormat.format(subPct)}%)',
                                excludeSemantics: isPrivacyMode,
                                child: Container(
                                  key: Key(
                                    'macro_subclass_chip_${bucket.macroClass.name}_${entry.key}',
                                  ),
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: KernoSpacing.s3,
                                    vertical: KernoSpacing.s2,
                                  ),
                                  decoration: BoxDecoration(
                                    color: tokens.surfaceSubtle,
                                    borderRadius: BorderRadius.circular(
                                      KernoRadius.full,
                                    ),
                                    border: Border.all(
                                      color: tokens.borderSubtle,
                                    ),
                                  ),
                                  child: Text(
                                    '${entry.key} · ${percentFormat.format(subPct)}% · $subValueStr',
                                    softWrap: true,
                                    style: KernoTypography.caption(
                                      color: tokens.textSecondary,
                                    ),
                                  ),
                                ),
                              );
                            }).toList(growable: false),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              ),
            );
          }),
        ],
      ),
    );
  }
}

class _DualAllocationBar extends StatelessWidget {
  final double actualFraction;
  final double? targetFraction;
  final Color barColor;
  final Color trackColor;
  final Color markerColor;

  const _DualAllocationBar({
    required this.actualFraction,
    required this.targetFraction,
    required this.barColor,
    required this.trackColor,
    required this.markerColor,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth.isFinite ? constraints.maxWidth : 200.0;
        final fillWidth = (width * actualFraction).clamp(0.0, width);
        final maxLeft = (width - 3.0).clamp(0.0, width);
        final markerLeft = targetFraction == null
            ? null
            : ((width * targetFraction!) - 1.5).clamp(0.0, maxLeft);

        return SizedBox(
          height: 12,
          width: double.infinity,
          child: Stack(
            alignment: Alignment.centerLeft,
            children: <Widget>[
              Container(
                height: 8,
                width: width,
                decoration: BoxDecoration(
                  color: trackColor,
                  borderRadius: BorderRadius.circular(KernoRadius.full),
                ),
              ),
              Container(
                height: 8,
                width: fillWidth,
                decoration: BoxDecoration(
                  color: barColor,
                  borderRadius: BorderRadius.circular(KernoRadius.full),
                ),
              ),
              if (markerLeft != null)
                Positioned(
                  left: markerLeft,
                  top: 0,
                  bottom: 0,
                  child: Container(
                    width: 3,
                    decoration: BoxDecoration(
                      color: markerColor,
                      borderRadius: BorderRadius.circular(KernoRadius.sm),
                    ),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}
