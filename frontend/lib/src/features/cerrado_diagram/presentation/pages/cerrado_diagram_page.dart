import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../../core/theme/kerno_accessibility.dart';
import '../../../../core/theme/kerno_tokens.dart';
import '../providers/cerrado_diagram_provider.dart';
import '../widgets/cerrado_macro_allocation_card.dart';
import '../widgets/cerrado_template_selector_card.dart';
import '../widgets/cvm_regulatory_footer.dart';

class CerradoDiagramPage extends StatefulWidget {
  const CerradoDiagramPage({super.key});

  @override
  State<CerradoDiagramPage> createState() => _CerradoDiagramPageState();
}

class _CerradoDiagramPageState extends State<CerradoDiagramPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final provider = context.read<CerradoDiagramProvider>();
      if (provider.status == CerradoDiagramStatus.initial) {
        provider.load();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    return Scaffold(
      backgroundColor: tokens.surfaceBase,
      body: SafeArea(
        child: Consumer<CerradoDiagramProvider>(
          builder: (context, provider, _) {
            return switch (provider.status) {
              CerradoDiagramStatus.initial ||
              CerradoDiagramStatus.loading =>
                const Center(
                  child: CircularProgressIndicator(),
                ),
              CerradoDiagramStatus.error => _ErrorState(
                  message:
                      provider.errorMessage ??
                      'Não foi possível carregar o Diagrama do Cerrado.',
                  onRetry: () => provider.load(),
                ),
              CerradoDiagramStatus.loaded => _LoadedBody(provider: provider),
            };
          },
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({
    required this.message,
    required this.onRetry,
  });

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(KernoSpacing.screenMarginMobile),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 480),
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(KernoSpacing.cardPaddingMobile),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  const Align(
                    alignment: Alignment.centerLeft,
                    child: TriadStatusBadge(
                      label: 'Falha ao carregar',
                      icon: Icons.error_outline,
                      tone: TriadStatusTone.crimson,
                    ),
                  ),
                  const SizedBox(height: KernoSpacing.s3),
                  Text(
                    message,
                    style: KernoTypography.bodyBase(color: tokens.textPrimary),
                  ),
                  const SizedBox(height: KernoSpacing.s4),
                  ConstrainedBox(
                    constraints: const BoxConstraints(
                      minHeight: KernoSpacing.touchTargetMin,
                    ),
                    child: FilledButton.icon(
                      onPressed: onRetry,
                      icon: const Icon(Icons.refresh),
                      label: const Text('Tentar novamente'),
                    ),
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

class _LoadedBody extends StatelessWidget {
  const _LoadedBody({required this.provider});

  final CerradoDiagramProvider provider;

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);
    final catalog = provider.catalog;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(KernoSpacing.screenMarginMobile),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 760),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Text(
                'Diagrama do Cerrado',
                style: KernoTypography.heading1(color: tokens.textPrimary),
              ),
              const SizedBox(height: KernoSpacing.s1),
              Text(
                'Configure suas metas estratégicas nas 5 Macro Classes com soma exata de 100,00%.',
                style: KernoTypography.bodyBase(color: tokens.textSecondary),
              ),
              const SizedBox(height: KernoSpacing.sectionGap),
              if (catalog != null && catalog.templates.isNotEmpty) ...<Widget>[
                CerradoTemplateSelectorCard(
                  catalog: catalog,
                  selectedTemplateCode: provider.selectedTemplateCode,
                  onSelectTemplate: provider.selectTemplate,
                ),
                const SizedBox(height: KernoSpacing.sectionGap),
              ],
              CerradoMacroAllocationCard(
                rawInputs: provider.rawInputs,
                parsedBps: provider.parsedBps,
                validation: provider.validation,
                inputSyncVersion: provider.inputSyncVersion,
                canSave: provider.canSave,
                isSaving: provider.isSaving,
                onChanged: provider.updateTargetInput,
                onSave: () => provider.savePreferences(),
                saveSuccessMessage: provider.saveSuccessMessage,
                saveErrorMessage: provider.saveErrorMessage,
              ),
              if (catalog != null &&
                  catalog.cvmDisclaimer.trim().isNotEmpty) ...<Widget>[
                const SizedBox(height: KernoSpacing.sectionGap),
                CvmRegulatoryFooter(disclaimer: catalog.cvmDisclaimer),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
