import 'package:flutter/material.dart';
import 'package:frontend/src/core/theme/kerno_accessibility.dart';
import 'package:frontend/src/core/theme/kerno_tokens.dart';

/// Superfície placeholder do módulo Kerno AI (`/kerno-ai`).
///
/// O motor conversacional completo será implementado no Épico 5.
class KernoAiPage extends StatelessWidget {
  const KernoAiPage({super.key});

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(KernoSpacing.cardPaddingDesktop),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 720),
          child: Container(
            padding: const EdgeInsets.all(KernoSpacing.cardPaddingDesktop),
            decoration: BoxDecoration(
              color: tokens.surfaceElevated,
              borderRadius: BorderRadius.circular(KernoRadius.lg),
              border: Border.all(color: tokens.borderSubtle),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                const TriadStatusBadge(
                  label: 'Kerno AI • Em breve',
                  icon: Icons.auto_awesome_outlined,
                  tone: TriadStatusTone.brand,
                ),
                const SizedBox(height: KernoSpacing.s4),
                Text(
                  'Copiloto Inteligente de Patrimônio',
                  style: KernoTypography.heading2(color: tokens.textPrimary),
                ),
                const SizedBox(height: KernoSpacing.s3),
                Text(
                  'Aqui você poderá conversar com o Kerno AI para analisar a '
                  'saúde da sua carteira, simular novos aportes com margem de '
                  'segurança e esclarecer dúvidas tributárias em linguagem simples.',
                  style: KernoTypography.bodyBase(color: tokens.textSecondary),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
