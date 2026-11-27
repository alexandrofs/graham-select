import 'package:flutter/material.dart';
import 'package:frontend/src/core/theme/kerno_accessibility.dart';
import 'package:frontend/src/core/theme/kerno_tokens.dart';

/// Stub modular desacoplado para o módulo de Gastos (`/expenses`, Ciclo 2.1).
///
/// Mantido isolado de `contribution_gps` e `portfolio` e protegido pela
/// feature flag `ENABLE_EXPENSES_MODULE`.
class ExpensesPage extends StatelessWidget {
  const ExpensesPage({super.key});

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
                  label: 'Módulo de Gastos • Ciclo 2.1',
                  icon: Icons.account_balance_wallet_outlined,
                  tone: TriadStatusTone.emerald,
                ),
                const SizedBox(height: KernoSpacing.s4),
                Text(
                  'Controle de Fluxo e Orçamento',
                  style: KernoTypography.heading2(color: tokens.textPrimary),
                ),
                const SizedBox(height: KernoSpacing.s3),
                Text(
                  'Superfície modular preparada para o acompanhamento de '
                  'despesas e capacidade de aporte mensal.',
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
