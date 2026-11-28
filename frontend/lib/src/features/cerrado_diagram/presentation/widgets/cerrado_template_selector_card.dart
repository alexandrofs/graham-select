import 'package:flutter/material.dart';

import '../../../../core/theme/kerno_accessibility.dart';
import '../../../../core/theme/kerno_tokens.dart';
import '../../domain/entities/cerrado_macro_class.dart';
import '../../domain/entities/cerrado_template.dart';
import '../../domain/services/cerrado_target_validator.dart';

class CerradoTemplateSelectorCard extends StatelessWidget {
  const CerradoTemplateSelectorCard({
    super.key,
    required this.catalog,
    required this.selectedTemplateCode,
    required this.onSelectTemplate,
  });

  final CerradoTemplatesCatalog catalog;
  final String? selectedTemplateCode;
  final ValueChanged<CerradoTemplate> onSelectTemplate;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(KernoSpacing.cardPaddingMobile),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text(
              'Templates de Onboarding',
              style: KernoTypography.heading3(color: tokens.textPrimary),
            ),
            const SizedBox(height: KernoSpacing.s1),
            Text(
              'Escolha um ponto de partida opcional ou preencha suas metas manualmente abaixo.',
              style: KernoTypography.bodySm(color: tokens.textSecondary),
            ),
            if (selectedTemplateCode != null) ...<Widget>[
              const SizedBox(height: KernoSpacing.s3),
              TriadStatusBadge(
                label:
                    'Notas iniciais neutras = ${catalog.defaultNeutralGrade} (Editáveis)',
                icon: Icons.tune_rounded,
                tone: TriadStatusTone.brand,
              ),
            ],
            const SizedBox(height: KernoSpacing.s4),
            Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                for (int i = 0; i < catalog.templates.length; i++) ...<Widget>[
                  if (i > 0) const SizedBox(height: KernoSpacing.s3),
                  _TemplateOptionCard(
                    template: catalog.templates[i],
                    isSelected:
                        catalog.templates[i].code == selectedTemplateCode,
                    onTap: () => onSelectTemplate(catalog.templates[i]),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _TemplateOptionCard extends StatelessWidget {
  const _TemplateOptionCard({
    required this.template,
    required this.isSelected,
    required this.onTap,
  });

  final CerradoTemplate template;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    return Semantics(
      button: true,
      selected: isSelected,
      label: '${template.name}. ${template.description}',
      child: ConstrainedBox(
        constraints: const BoxConstraints(
          minHeight: KernoSpacing.touchTargetMin,
        ),
        child: Material(
          color: isSelected ? tokens.brandAccentSoft : tokens.surfaceSubtle,
          borderRadius: BorderRadius.circular(KernoRadius.md),
          child: InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(KernoRadius.md),
            child: Container(
              padding: const EdgeInsets.all(KernoSpacing.s4),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(KernoRadius.md),
                border: Border.all(
                  color: isSelected ? tokens.brandPrimary : tokens.borderSubtle,
                  width: isSelected ? 2.0 : 1.0,
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  ResponsiveMetricRow(
                    leading: Text(
                      template.name,
                      style: KernoTypography.bodyBase(
                        color: tokens.textPrimary,
                      ).copyWith(fontWeight: FontWeight.w600),
                    ),
                    trailing: isSelected
                        ? TriadStatusBadge(
                            label: 'Selecionado',
                            icon: Icons.check_circle_outline,
                            tone: TriadStatusTone.brand,
                          )
                        : Text(
                            'Aplicar',
                            style: KernoTypography.bodySm(
                              color: tokens.brandPrimary,
                            ).copyWith(fontWeight: FontWeight.w600),
                          ),
                  ),
                  const SizedBox(height: KernoSpacing.s1),
                  Text(
                    template.description,
                    style: KernoTypography.bodySm(color: tokens.textSecondary),
                  ),
                  const SizedBox(height: KernoSpacing.s3),
                  Wrap(
                    spacing: KernoSpacing.s3,
                    runSpacing: KernoSpacing.s2,
                    children: <Widget>[
                      for (final macroClass in CerradoMacroClass.values)
                        _MacroChip(
                          label: macroClass.label,
                          bps: template.bpsFor(macroClass),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _MacroChip extends StatelessWidget {
  const _MacroChip({
    required this.label,
    required this.bps,
  });

  final String label;
  final int bps;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final formatted = '${CerradoTargetValidator.formatBpsPtBr(bps)}%';

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: KernoSpacing.s2,
        vertical: KernoSpacing.s1,
      ),
      decoration: BoxDecoration(
        color: tokens.surfaceElevated,
        borderRadius: BorderRadius.circular(KernoRadius.sm),
        border: Border.all(color: tokens.borderSubtle),
      ),
      child: Wrap(
        spacing: KernoSpacing.s1,
        crossAxisAlignment: WrapCrossAlignment.center,
        children: <Widget>[
          Text(
            '$label:',
            style: KernoTypography.caption(color: tokens.textSecondary),
          ),
          KernoMetricText(
            formatted,
            style: KernoTypography.caption(
              color: tokens.textPrimary,
            ).copyWith(fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}
