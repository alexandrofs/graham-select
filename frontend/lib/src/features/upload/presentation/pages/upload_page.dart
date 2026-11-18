import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../family_holding/presentation/providers/family_profile_provider.dart';
import '../../../open_finance/presentation/widgets/open_finance_tab.dart';
import '../providers/upload_provider.dart';
import '../widgets/b3_upload_zone.dart';
import '../widgets/upload_progress_widget.dart';

class UploadPage extends StatefulWidget {
  const UploadPage({super.key});

  @override
  State<UploadPage> createState() => _UploadPageState();
}

class _UploadPageState extends State<UploadPage> {
  int _selectedTab = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      try {
        final familyProvider = context.read<FamilyProfileProvider>();
        if (familyProvider.state == FamilyProfileState.initial) {
          familyProvider.fetchProfiles(silent: true);
        }
      } catch (_) {
        // Ignora caso FamilyProfileProvider não esteja registrado em testes isolados
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    FamilyProfileProvider? familyProvider;
    try {
      familyProvider = context.watch<FamilyProfileProvider>();
    } catch (_) {
      familyProvider = null;
    }

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Consumer<UploadProvider>(
        builder: (context, provider, child) {
          final profiles = familyProvider?.profiles ?? const [];
          return SingleChildScrollView(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Header
                Text(
                  'Importar Planilha da B3 e Notas de Corretagem',
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Faça o upload do seu relatório de negociação B3 (.xlsx) ou da sua Nota de Corretagem SINACOR (.pdf).',
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        color: AppTheme.textColor.withValues(alpha: 0.7),
                      ),
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  children: [
                    Chip(
                      avatar: const Icon(Icons.table_chart_outlined, size: 16),
                      label: const Text('.xlsx (Planilha B3)'),
                      backgroundColor: AppTheme.surfaceColor,
                    ),
                    Chip(
                      avatar: const Icon(Icons.picture_as_pdf_outlined, size: 16),
                      label: const Text('.pdf (Nota SINACOR)'),
                      backgroundColor: AppTheme.surfaceColor,
                    ),
                  ],
                ),
                const SizedBox(height: 24),

                // Seletor de modo de importação: Manual vs Open Finance
                SizedBox(
                  width: double.infinity,
                  child: SegmentedButton<int>(
                    segments: const [
                      ButtonSegment<int>(
                        value: 0,
                        label: Text('Upload Manual (.xlsx / .pdf)'),
                        icon: Icon(Icons.upload_file),
                      ),
                      ButtonSegment<int>(
                        value: 1,
                        label: Text('Open Finance (Automático)'),
                        icon: Icon(Icons.sync),
                      ),
                    ],
                    selected: {_selectedTab},
                    onSelectionChanged: (Set<int> newSelection) {
                      setState(() {
                        _selectedTab = newSelection.first;
                      });
                    },
                  ),
                ),
                const SizedBox(height: 24),

                if (_selectedTab == 1)
                  const OpenFinanceTab()
                else ...[
                if (profiles.isNotEmpty) ...[
                  Builder(
                    builder: (context) {
                      final hasSelected = provider.selectedProfileId != null &&
                          profiles.any((p) => p.id == provider.selectedProfileId);
                      if (provider.selectedProfileId != null && !hasSelected) {
                        WidgetsBinding.instance.addPostFrameCallback((_) {
                          if (mounted) {
                            provider.setSelectedProfileId(null);
                          }
                        });
                      }
                      final effectiveProfileId =
                          hasSelected ? provider.selectedProfileId : null;
                      return DropdownButtonFormField<String?>(
                        key: const Key('upload_profile_selector'),
                        initialValue: effectiveProfileId,
                        decoration: const InputDecoration(
                          labelText: 'Perfil / CPF de Destino',
                          border: OutlineInputBorder(),
                        ),
                        dropdownColor: AppTheme.surfaceColor,
                        items: [
                          const DropdownMenuItem<String?>(
                            value: null,
                            child: Text('Conta Titular (Minha Carteira)'),
                          ),
                          ...profiles.map(
                            (profile) => DropdownMenuItem<String?>(
                              value: profile.id,
                              child: Text('${profile.name} (${profile.cpfMasked})'),
                            ),
                          ),
                        ],
                        onChanged: provider.state == UploadState.uploading
                            ? null
                            : (value) => provider.setSelectedProfileId(value),
                      );
                    },
                  ),
                  const SizedBox(height: 24),
                ] else ...[
                  if (provider.selectedProfileId != null)
                    Builder(
                      builder: (context) {
                        WidgetsBinding.instance.addPostFrameCallback((_) {
                          if (mounted) {
                            provider.setSelectedProfileId(null);
                          }
                        });
                        return const SizedBox(height: 8);
                      },
                    )
                  else
                    const SizedBox(height: 8),
                ],

                // B3 Upload Zone (supports Drag and Drop and File Picker)
                const B3UploadZone(),

                const SizedBox(height: 24),

                // Upload Button
                if (provider.hasFile &&
                    provider.state != UploadState.uploading &&
                    provider.state != UploadState.success)
                  ElevatedButton(
                    onPressed: provider.canUpload
                        ? () => provider.uploadB3File()
                        : null,
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size(double.infinity, 56),
                    ),
                    child: const Text('Confirmar e Enviar Planilha'),
                  ),

                if (provider.state == UploadState.uploading) ...[
                  const SizedBox(height: 24),
                  UploadProgressWidget(message: provider.progressMessage),
                ],

                // Success Message
                if (provider.state == UploadState.success) ...[
                  const SizedBox(height: 32),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppTheme.accentColor.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: AppTheme.accentColor.withValues(alpha: 0.3),
                      ),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.check_circle,
                          color: AppTheme.accentColor,
                          size: 24,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            provider.successMessage ??
                                'Upload realizado com sucesso!',
                            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  color: AppTheme.accentColor,
                                ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (provider.processingStatus != null) ...[
                    const SizedBox(height: 12),
                    Text(
                      'Linhas processadas: ${provider.processingStatus!.processedRows} | '
                      'Novas: ${provider.processingStatus!.successfulRows} | '
                      'Duplicadas: ${provider.processingStatus!.duplicatedRows} | '
                      'Falhas: ${provider.processingStatus!.failedRows}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: AppTheme.textColor.withValues(alpha: 0.75),
                          ),
                    ),
                  ],
                  const SizedBox(height: 16),
                  OutlinedButton(
                    onPressed: () => provider.reset(),
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size(double.infinity, 56),
                      side: BorderSide(
                        color: AppTheme.primaryColor.withValues(alpha: 0.5),
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text('Enviar Outra Planilha'),
                  ),
                ],

                // Error Message
                if (provider.state == UploadState.error) ...[
                  const SizedBox(height: 32),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.red.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: Colors.red.withValues(alpha: 0.3),
                      ),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.error,
                          color: Colors.red,
                          size: 24,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            provider.errorMessage ?? 'Erro ao fazer upload',
                            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  color: Colors.red,
                                ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  OutlinedButton(
                    onPressed: () => provider.reset(),
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size(double.infinity, 56),
                      side: BorderSide(
                        color: AppTheme.primaryColor.withValues(alpha: 0.5),
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text('Tentar Novamente'),
                  ),
                ],
              ],
              ],
            ),
          );
        },
      ),
    );
  }
}
