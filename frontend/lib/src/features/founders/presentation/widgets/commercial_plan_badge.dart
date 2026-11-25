import 'package:flutter/material.dart';
import '../../../profile/data/models/user_profile_model.dart';

class CommercialPlanBadge extends StatelessWidget {
  final UserProfile profile;

  const CommercialPlanBadge({super.key, required this.profile});

  String _resolvePlan() {
    final plan = profile.commercialPlan?.toUpperCase();
    final tier = profile.tier.toUpperCase();
    if (plan == 'PRO_TRIAL' && tier == 'FREE') {
      return 'FREE';
    }
    if (plan != null && plan.isNotEmpty) {
      return plan;
    }
    switch (tier) {
      case 'PREMIUM':
        return 'FOUNDER_LIFETIME';
      case 'TRIAL':
        return 'PRO_TRIAL';
      default:
        return 'FREE';
    }
  }

  int _resolveDaysRemaining() {
    if (profile.daysRemaining != null) {
      return profile.daysRemaining! < 0 ? 0 : profile.daysRemaining!;
    }
    final expiry = profile.trialExpiresAt ?? profile.trialEndsAt;
    if (expiry == null) {
      return 0;
    }
    final diff = expiry.toUtc().difference(DateTime.now().toUtc()).inDays;
    return diff < 0 ? 0 : diff;
  }

  @override
  Widget build(BuildContext context) {
    final plan = _resolvePlan();
    final Color backgroundColor;
    final Color textColor;
    final Color borderColor;
    final IconData icon;
    final String label;

    switch (plan) {
      case 'FOUNDER_LIFETIME':
        backgroundColor = const Color(0xFFFEF3C7);
        textColor = const Color(0xFF92400E);
        borderColor = const Color(0xFFF59E0B);
        icon = Icons.workspace_premium;
        label = 'Founder';
        break;
      case 'PRO_TRIAL':
        final days = _resolveDaysRemaining();
        final unit = days == 1 ? 'dia restante' : 'dias restantes';
        backgroundColor = const Color(0xFFEEF2FF);
        textColor = const Color(0xFF3730A3);
        borderColor = const Color(0xFF6366F1);
        icon = Icons.timer_outlined;
        label = 'Trial Pro: $days $unit';
        break;
      case 'FREE':
      default:
        backgroundColor = const Color(0xFFF1F5F9);
        textColor = const Color(0xFF334155);
        borderColor = const Color(0xFFCBD5E1);
        icon = Icons.person_outline;
        label = 'Gratuito';
        break;
    }

    return Semantics(
      label: 'Plano comercial: $label',
      child: Container(
        key: const Key('commercial_plan_badge'),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: backgroundColor,
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: borderColor),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 16, color: textColor),
            const SizedBox(width: 6),
            Flexible(
              child: Text(
                label,
                softWrap: true,
                style: TextStyle(
                  color: textColor,
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
