import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../../core/security/privacy_provider.dart';
import '../../../../core/theme/kerno_accessibility.dart';
import '../../../../core/theme/kerno_tokens.dart';
import '../../../founders/presentation/widgets/commercial_plan_badge.dart';
import '../../../profile/data/models/user_profile_model.dart';
import '../../domain/entities/portfolio_summary.dart';

/// Card hero do Dashboard Patrimonial com saldo consolidado em destaque (`32px`),
/// rentabilidade daltônico-segura (`TriadStatusBadge`), aviso de delay B3/D-1 e
/// suporte nativo ao Modo Privacidade (`R$ ••••••`).
class WealthHeroCard extends StatelessWidget {
  final PortfolioSummary? summary;
  final bool isLoading;
  final DateTime? priceUpdatedAt;
  final bool hasCachePrice;
  final bool isOfflineCache;
  final UserProfile? profile;
  final String? activeTagName;

  const WealthHeroCard({
    super.key,
    required this.summary,
    this.isLoading = false,
    this.priceUpdatedAt,
    this.hasCachePrice = false,
    this.isOfflineCache = false,
    this.profile,
    this.activeTagName,
  });

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final privacyProvider = Provider.of<PrivacyProvider?>(context);
    final isPrivacyMode = privacyProvider?.isPrivacyMode ?? false;

    if (isLoading && summary == null) {
      return Container(
        key: const Key('wealth_hero_skeleton'),
        width: double.infinity,
        padding: const EdgeInsets.all(KernoSpacing.cardPaddingDesktop),
        decoration: BoxDecoration(
          color: tokens.surfaceElevated,
          borderRadius: BorderRadius.circular(KernoRadius.xl),
          border: Border.all(color: tokens.borderSubtle),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Container(
              height: 16,
              width: 180,
              decoration: BoxDecoration(
                color: tokens.surfaceSubtle,
                borderRadius: BorderRadius.circular(KernoRadius.sm),
              ),
            ),
            const SizedBox(height: KernoSpacing.s4),
            Container(
              height: 36,
              width: 240,
              decoration: BoxDecoration(
                color: tokens.surfaceSubtle,
                borderRadius: BorderRadius.circular(KernoRadius.sm),
              ),
            ),
            const SizedBox(height: KernoSpacing.s3),
            Container(
              height: 24,
              width: 150,
              decoration: BoxDecoration(
                color: tokens.surfaceSubtle,
                borderRadius: BorderRadius.circular(KernoRadius.full),
              ),
            ),
          ],
        ),
      );
    }

    final currentSummary = summary;
    if (currentSummary == null) {
      return const SizedBox.shrink();
    }

    final currencyFormat = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final percentFormat = NumberFormat('+##0.00;-##0.00', 'pt_BR');
    final safeTotalEquity =
        currentSummary.totalEquity.isFinite ? currentSummary.totalEquity : 0.0;
    final formattedEquity = isPrivacyMode
        ? PrivacyProvider.maskedCurrencyValue
        : currencyFormat.format(safeTotalEquity);
    final equitySemantics = isPrivacyMode
        ? PrivacyProvider.maskedSemanticsLabel
        : 'Patrimônio consolidado ${currencyFormat.format(safeTotalEquity)}';

    final yieldPct = currentSummary.grossYieldPercentage.isFinite
        ? currentSummary.grossYieldPercentage
        : 0.0;
    final isPositiveYield = yieldPct >= 0;
    final yieldLabel = '${percentFormat.format(yieldPct)}%';

    final delayText = _buildDelayNoticeText();
    final isWarningDelay = hasCachePrice || isOfflineCache;

    return Container(
      key: const Key('wealth_hero_card'),
      width: double.infinity,
      padding: const EdgeInsets.all(KernoSpacing.cardPaddingDesktop),
      decoration: BoxDecoration(
        color: tokens.surfaceElevated,
        borderRadius: BorderRadius.circular(KernoRadius.xl),
        border: Border.all(color: tokens.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          SizedBox(
            width: double.infinity,
            child: Wrap(
              spacing: KernoSpacing.s2,
              runSpacing: KernoSpacing.s2,
              crossAxisAlignment: WrapCrossAlignment.center,
              alignment: WrapAlignment.spaceBetween,
              children: <Widget>[
                Text(
                  activeTagName != null
                      ? 'Patrimônio · $activeTagName'
                      : 'Patrimônio Consolidado',
                  style: KernoTypography.bodySm(color: tokens.textSecondary)
                      .copyWith(fontWeight: FontWeight.w600),
                ),
                if (profile != null) CommercialPlanBadge(profile: profile!),
              ],
            ),
          ),
          const SizedBox(height: KernoSpacing.s3),
          Semantics(
            label: equitySemantics,
            excludeSemantics: isPrivacyMode,
            child: KernoMetricText(
              formattedEquity,
              key: const Key('wealth_hero_equity_value'),
              style: KernoTypography.metricHero(color: tokens.textPrimary),
            ),
          ),
          const SizedBox(height: KernoSpacing.s3),
          Wrap(
            spacing: KernoSpacing.s3,
            runSpacing: KernoSpacing.s2,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: <Widget>[
              TriadStatusBadge(
                label: 'Rentabilidade $yieldLabel',
                icon: isPositiveYield
                    ? Icons.trending_up_rounded
                    : Icons.trending_down_rounded,
                tone: isPositiveYield
                    ? TriadStatusTone.emerald
                    : TriadStatusTone.crimson,
                semanticsLabel: 'Rentabilidade bruta de $yieldLabel',
              ),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Icon(
                    isWarningDelay
                        ? Icons.warning_amber_rounded
                        : Icons.schedule_rounded,
                    size: 15,
                    color: isWarningDelay ? tokens.amber : tokens.textMuted,
                  ),
                  const SizedBox(width: KernoSpacing.s1),
                  Flexible(
                    child: Text(
                      delayText,
                      key: const Key('wealth_hero_delay_notice'),
                      softWrap: true,
                      style: KernoTypography.caption(
                        color: isWarningDelay ? tokens.amber : tokens.textMuted,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  String _buildDelayNoticeText() {
    if (isOfflineCache) {
      return 'Modo offline · Cotações de fechamento D-1 em cache';
    }
    if (hasCachePrice) {
      return 'Cotações em cache / fechamento D-1 B3';
    }
    if (priceUpdatedAt != null) {
      final timeStr = DateFormat('HH:mm', 'pt_BR').format(priceUpdatedAt!.toLocal());
      return 'Delay de até 15 min B3 · Atualizado às $timeStr';
    }
    return 'Cotações com delay de até 15 min B3 / fechamento D-1';
  }
}
