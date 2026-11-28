import 'package:flutter/material.dart';

import '../../../../core/theme/kerno_accessibility.dart';
import '../../../../core/theme/kerno_tokens.dart';
import '../../domain/entities/cerrado_macro_class.dart';
import '../../domain/services/cerrado_target_validator.dart';

class CerradoMacroAllocationCard extends StatefulWidget {
  const CerradoMacroAllocationCard({
    super.key,
    required this.rawInputs,
    required this.parsedBps,
    required this.validation,
    required this.inputSyncVersion,
    required this.canSave,
    required this.isSaving,
    required this.onChanged,
    required this.onSave,
    this.saveSuccessMessage,
    this.saveErrorMessage,
  });

  final Map<CerradoMacroClass, String> rawInputs;
  final Map<CerradoMacroClass, int?> parsedBps;
  final CerradoTargetValidationResult validation;
  final int inputSyncVersion;
  final bool canSave;
  final bool isSaving;
  final void Function(CerradoMacroClass macroClass, String value) onChanged;
  final VoidCallback onSave;
  final String? saveSuccessMessage;
  final String? saveErrorMessage;

  @override
  State<CerradoMacroAllocationCard> createState() =>
      _CerradoMacroAllocationCardState();
}

class _CerradoMacroAllocationCardState
    extends State<CerradoMacroAllocationCard> {
  late final Map<CerradoMacroClass, TextEditingController> _controllers;
  int _lastSyncedVersion = -1;

  @override
  void initState() {
    super.initState();
    _controllers = <CerradoMacroClass, TextEditingController>{
      for (final macroClass in CerradoMacroClass.values)
        macroClass: TextEditingController(
          text: widget.rawInputs[macroClass] ?? '0,00',
        ),
    };
    _lastSyncedVersion = widget.inputSyncVersion;
  }

  @override
  void didUpdateWidget(covariant CerradoMacroAllocationCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.inputSyncVersion != _lastSyncedVersion) {
      _lastSyncedVersion = widget.inputSyncVersion;
      for (final macroClass in CerradoMacroClass.values) {
        final nextText = widget.rawInputs[macroClass] ?? '0,00';
        final controller = _controllers[macroClass]!;
        if (controller.text != nextText) {
          controller.text = nextText;
        }
      }
    }
  }

  @override
  void dispose() {
    for (final controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final totalFormatted =
        '${CerradoTargetValidator.formatBpsPtBr(widget.validation.totalBps)}%';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(KernoSpacing.cardPaddingMobile),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Text(
              'Metas por Macro Classe',
              style: KernoTypography.heading3(color: tokens.textPrimary),
            ),
            const SizedBox(height: KernoSpacing.s1),
            Text(
              'Defina a distribuição exata da sua carteira entre as 5 Macro Classes do Diagrama do Cerrado (soma obrigatória de 100,00%).',
              style: KernoTypography.bodySm(color: tokens.textSecondary),
            ),
            const SizedBox(height: KernoSpacing.s4),
            for (final macroClass in CerradoMacroClass.values) ...<Widget>[
              _MacroInputRow(
                macroClass: macroClass,
                controller: _controllers[macroClass]!,
                enabled: !widget.isSaving,
                isInvalid: widget.parsedBps[macroClass] == null,
                onChanged: (value) => widget.onChanged(macroClass, value),
              ),
              const SizedBox(height: KernoSpacing.s3),
            ],
            const Divider(),
            const SizedBox(height: KernoSpacing.s3),
            ResponsiveMetricRow(
              leading: Text(
                'Total alocado',
                style: KernoTypography.bodyBase(
                  color: tokens.textPrimary,
                ).copyWith(fontWeight: FontWeight.w600),
              ),
              trailing: KernoMetricText(
                totalFormatted,
                style: KernoTypography.metricCard(
                  color: widget.validation.isValid
                      ? tokens.emerald
                      : tokens.crimson,
                ),
              ),
            ),
            const SizedBox(height: KernoSpacing.s3),
            Align(
              alignment: Alignment.centerLeft,
              child: Semantics(
                liveRegion: true,
                child: _buildLockBadge(),
              ),
            ),
            if (widget.saveSuccessMessage != null) ...<Widget>[
              const SizedBox(height: KernoSpacing.s3),
              _FeedbackBanner(
                message: widget.saveSuccessMessage!,
                icon: Icons.check_circle_outline,
                foreground: tokens.emerald,
                background: tokens.emeraldSurface,
                border: tokens.emeraldBorder,
              ),
            ],
            if (widget.saveErrorMessage != null) ...<Widget>[
              const SizedBox(height: KernoSpacing.s3),
              _FeedbackBanner(
                message: widget.saveErrorMessage!,
                icon: Icons.error_outline,
                foreground: tokens.crimson,
                background: tokens.crimsonSurface,
                border: tokens.crimsonBorder,
              ),
            ],
            const SizedBox(height: KernoSpacing.s4),
            ConstrainedBox(
              constraints: const BoxConstraints(
                minHeight: KernoSpacing.touchTargetMin,
              ),
              child: FilledButton.icon(
                onPressed: widget.canSave ? widget.onSave : null,
                style: FilledButton.styleFrom(
                  backgroundColor: widget.validation.isValid
                      ? tokens.emerald
                      : tokens.brandPrimary,
                  foregroundColor: tokens.textOnPrimary,
                  minimumSize: const Size(
                    double.infinity,
                    KernoSpacing.touchTargetMin,
                  ),
                ),
                icon: widget.isSaving
                    ? SizedBox(
                        width: 18.0,
                        height: 18.0,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.0,
                          color: tokens.textOnPrimary,
                        ),
                      )
                    : const Icon(Icons.save_outlined),
                label: Text(
                  widget.isSaving ? 'Salvando...' : 'Salvar Metas',
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLockBadge() {
    if (widget.validation.hasInvalidField) {
      return const TriadStatusBadge(
        label: 'Valor inválido — use até 2 casas decimais (0,00 a 100,00)',
        icon: Icons.error_outline,
        tone: TriadStatusTone.crimson,
      );
    }

    if (widget.validation.isValid) {
      return const TriadStatusBadge(
        label: 'Soma exata: 100,00%',
        icon: Icons.check_circle_outline,
        tone: TriadStatusTone.emerald,
      );
    }

    if (widget.validation.remainingBps > 0) {
      final missing = CerradoTargetValidator.formatBpsPtBr(
        widget.validation.remainingBps,
      );
      return TriadStatusBadge(
        label: 'Faltam $missing%',
        icon: Icons.warning_amber_rounded,
        tone: TriadStatusTone.crimson,
      );
    }

    final excess = CerradoTargetValidator.formatBpsPtBr(
      -widget.validation.remainingBps,
    );
    return TriadStatusBadge(
      label: 'Excesso de $excess%',
      icon: Icons.warning_amber_rounded,
      tone: TriadStatusTone.crimson,
    );
  }
}

class _MacroInputRow extends StatelessWidget {
  const _MacroInputRow({
    required this.macroClass,
    required this.controller,
    required this.enabled,
    required this.isInvalid,
    required this.onChanged,
  });

  final CerradoMacroClass macroClass;
  final TextEditingController controller;
  final bool enabled;
  final bool isInvalid;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    return ConstrainedBox(
      constraints: const BoxConstraints(
        minHeight: KernoSpacing.touchTargetMin,
      ),
      child: TextField(
        key: Key('cerrado_input_${macroClass.jsonKey}'),
        controller: controller,
        enabled: enabled,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        onChanged: onChanged,
        style: KernoTypography.bodyBase(color: tokens.textPrimary),
        decoration: InputDecoration(
          labelText: macroClass.label,
          suffixText: '%',
          errorText: isInvalid ? 'Entre 0,00 e 100,00 (máx. 2 decimais)' : null,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(KernoRadius.md),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(KernoRadius.md),
            borderSide: BorderSide(color: tokens.borderStrong),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(KernoRadius.md),
            borderSide: BorderSide(color: tokens.borderFocus, width: 2.0),
          ),
          errorBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(KernoRadius.md),
            borderSide: BorderSide(color: tokens.crimson, width: 1.5),
          ),
          focusedErrorBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(KernoRadius.md),
            borderSide: BorderSide(color: tokens.crimson, width: 2.0),
          ),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: KernoSpacing.s4,
            vertical: KernoSpacing.s3,
          ),
        ),
      ),
    );
  }
}

class _FeedbackBanner extends StatelessWidget {
  const _FeedbackBanner({
    required this.message,
    required this.icon,
    required this.foreground,
    required this.background,
    required this.border,
  });

  final String message;
  final IconData icon;
  final Color foreground;
  final Color background;
  final Color border;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(KernoSpacing.s3),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(KernoRadius.md),
        border: Border.all(color: border),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Icon(icon, size: 18.0, color: foreground),
          const SizedBox(width: KernoSpacing.s2),
          Expanded(
            child: Text(
              message,
              softWrap: true,
              style: KernoTypography.bodySm(color: foreground).copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
