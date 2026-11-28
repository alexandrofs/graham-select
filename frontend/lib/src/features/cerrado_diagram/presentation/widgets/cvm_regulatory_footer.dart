import 'package:flutter/material.dart';

import '../../../../core/theme/kerno_tokens.dart';

class CvmRegulatoryFooter extends StatelessWidget {
  const CvmRegulatoryFooter({
    super.key,
    required this.disclaimer,
  });

  final String disclaimer;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    return Container(
      padding: const EdgeInsets.all(KernoSpacing.s4),
      decoration: BoxDecoration(
        color: tokens.surfaceSubtle,
        borderRadius: BorderRadius.circular(KernoRadius.md),
        border: Border.all(color: tokens.borderSubtle),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Icon(
            Icons.info_outline,
            size: 18.0,
            color: tokens.textSecondary,
          ),
          const SizedBox(width: KernoSpacing.s3),
          Expanded(
            child: Text(
              disclaimer,
              softWrap: true,
              style: KernoTypography.caption(color: tokens.textSecondary),
            ),
          ),
        ],
      ),
    );
  }
}
