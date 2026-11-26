import 'package:flutter/material.dart';
import 'package:frontend/src/core/theme/kerno_tokens.dart';

/// Texto financeiro acessível com alinhamento tabular obrigatório (`tabular-nums`)
/// e proibição estrutural de truncamento (`TextOverflow.ellipsis`) para valores
/// monetários, tickers e percentuais sob `textScaler` de `1.0x` a `2.0x`.
class KernoMetricText extends StatelessWidget {
  const KernoMetricText(
    this.data, {
    super.key,
    this.style,
    this.color,
    this.textAlign,
    this.semanticsLabel,
  });

  final String data;
  final TextStyle? style;
  final Color? color;
  final TextAlign? textAlign;
  final String? semanticsLabel;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final baseStyle = style ?? KernoTypography.metricCard(color: tokens.textPrimary);
    final features = <FontFeature>{
      ...?baseStyle.fontFeatures,
      const FontFeature.tabularFigures(),
    }.toList(growable: false);
    final effectiveStyle = baseStyle.copyWith(
      color: color ?? baseStyle.color ?? tokens.textPrimary,
      overflow: TextOverflow.visible,
      fontFeatures: features,
    );

    return Text(
      data,
      style: effectiveStyle,
      textAlign: textAlign,
      softWrap: true,
      overflow: TextOverflow.visible,
      semanticsLabel: semanticsLabel,
    );
  }
}

/// Linha de métrica responsiva que comuta automaticamente de layout horizontal
/// ([Row]) para empilhamento vertical ([Column]) quando `MediaQuery.textScalerOf(context)`
/// atinge ou supera [reflowThreshold] (padrão `1.3x`) ou quando a largura
/// disponível é ilimitada, prevenindo `RenderFlex` overflow.
class ResponsiveMetricRow extends StatelessWidget {
  const ResponsiveMetricRow({
    super.key,
    required this.leading,
    required this.trailing,
    this.horizontalSpacing = KernoSpacing.s3,
    this.verticalSpacing = KernoSpacing.s2,
    this.reflowThreshold = 1.3,
    this.mainAxisAlignment = MainAxisAlignment.spaceBetween,
    this.rowCrossAxisAlignment = CrossAxisAlignment.center,
    this.columnCrossAxisAlignment = CrossAxisAlignment.start,
  });

  final Widget leading;
  final Widget trailing;
  final double horizontalSpacing;
  final double verticalSpacing;
  final double reflowThreshold;
  final MainAxisAlignment mainAxisAlignment;
  final CrossAxisAlignment rowCrossAxisAlignment;
  final CrossAxisAlignment columnCrossAxisAlignment;

  @override
  Widget build(BuildContext context) {
    final textScale = MediaQuery.textScalerOf(context).scale(1.0);
    return LayoutBuilder(
      builder: (context, constraints) {
        if (textScale >= reflowThreshold || !constraints.hasBoundedWidth) {
          return Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: columnCrossAxisAlignment,
            children: <Widget>[
              leading,
              SizedBox(height: verticalSpacing),
              trailing,
            ],
          );
        }

        return Row(
          mainAxisAlignment: mainAxisAlignment,
          crossAxisAlignment: rowCrossAxisAlignment,
          children: <Widget>[
            Flexible(child: leading),
            SizedBox(width: horizontalSpacing),
            Flexible(child: trailing),
          ],
        );
      },
    );
  }
}

/// Tons semânticos suportados pela Regra da Tríade (Cor + Ícone + Rótulo).
enum TriadStatusTone {
  emerald,
  amber,
  crimson,
  founder,
  brand,
}

/// Selo semântico daltônico-seguro que combina obrigatoriamente cor semântica,
/// ícone explícito e rótulo textual sem truncamento (`DESIGN.md`).
class TriadStatusBadge extends StatelessWidget {
  const TriadStatusBadge({
    super.key,
    required this.label,
    required this.icon,
    required this.tone,
    this.semanticsLabel,
  });

  final String label;
  final IconData icon;
  final TriadStatusTone tone;
  final String? semanticsLabel;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final (Color foreground, Color background, Color border) = switch (tone) {
      TriadStatusTone.emerald => (
          tokens.emerald,
          tokens.emeraldSurface,
          tokens.emeraldBorder,
        ),
      TriadStatusTone.amber => (
          tokens.amber,
          tokens.amberSurface,
          tokens.amberBorder,
        ),
      TriadStatusTone.crimson => (
          tokens.crimson,
          tokens.crimsonSurface,
          tokens.crimsonBorder,
        ),
      TriadStatusTone.founder => (
          tokens.founderGold,
          tokens.founderGoldSurface,
          tokens.founderGoldBorder,
        ),
      TriadStatusTone.brand => (
          tokens.brandPrimary,
          tokens.brandAccentSoft,
          tokens.brandPrimary,
        ),
    };

    return Semantics(
      label: semanticsLabel ?? label,
      excludeSemantics: true,
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: KernoSpacing.s3,
          vertical: KernoSpacing.s1,
        ),
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(KernoRadius.full),
          border: Border.all(color: border, width: 1.0),
        ),
        child: LayoutBuilder(
          builder: (context, constraints) {
            final labelWidget = Text(
              label,
              softWrap: true,
              overflow: TextOverflow.visible,
              style: KernoTypography.caption(color: foreground).copyWith(
                fontWeight: FontWeight.w600,
              ),
            );
            return Row(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: <Widget>[
                Icon(icon, size: 14.0, color: foreground),
                const SizedBox(width: KernoSpacing.s1),
                if (constraints.hasBoundedWidth)
                  Flexible(child: labelWidget)
                else
                  labelWidget,
              ],
            );
          },
        ),
      ),
    );
  }
}
