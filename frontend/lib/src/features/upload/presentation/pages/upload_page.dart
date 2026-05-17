import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../providers/upload_provider.dart';
import '../widgets/b3_upload_zone.dart';
import '../widgets/upload_progress_widget.dart';

class UploadPage extends StatelessWidget {
  const UploadPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Upload de Planilha B3'),
        backgroundColor: AppTheme.backgroundColor,
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [AppTheme.backgroundColor, AppTheme.surfaceColor],
          ),
        ),
        child: SafeArea(
          child: Consumer<UploadProvider>(
            builder: (context, provider, child) {
              return SingleChildScrollView(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Header
                    Text(
                      'Importar Planilha da B3',
                      style: Theme.of(context).textTheme.headlineSmall
                          ?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Faça o upload do seu relatório de negociação extraído do portal do investidor B3 (.xlsx).',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppTheme.textColor.withValues(alpha: 0.7),
                      ),
                    ),
                    const SizedBox(height: 32),

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
                                style: Theme.of(context).textTheme.bodyMedium
                                    ?.copyWith(color: AppTheme.accentColor),
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
                                style: Theme.of(context).textTheme.bodyMedium
                                    ?.copyWith(color: Colors.red),
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
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}
