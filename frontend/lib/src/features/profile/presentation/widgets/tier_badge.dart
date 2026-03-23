import 'package:flutter/material.dart';

class TierBadge extends StatelessWidget {
  final String tier;

  const TierBadge({super.key, required this.tier});

  @override
  Widget build(BuildContext context) {
    Color backgroundColor;
    Color textColor = Colors.white;
    String label = tier;

    switch (tier.toUpperCase()) {
      case 'PREMIUM':
        backgroundColor = const Color(0xFF10B981); // Emerald
        label = 'PREMIUM';
        break;
      case 'TRIAL':
        backgroundColor = const Color(0xFFD97706); // Amber Gold
        label = 'TRIAL';
        break;
      case 'FREE':
      default:
        backgroundColor = const Color(0xFF64748B); // Slate
        label = 'GRATUITO';
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(999),
        boxShadow: [
          BoxShadow(
            color: backgroundColor.withValues(alpha: 0.3),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Text(
        label,
        style: TextStyle(
          color: textColor,
          fontSize: 12,
          fontWeight: FontWeight.bold,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}
