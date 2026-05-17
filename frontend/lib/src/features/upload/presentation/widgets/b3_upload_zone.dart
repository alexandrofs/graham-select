import 'package:flutter/material.dart';
import 'package:desktop_drop/desktop_drop.dart';
import 'package:provider/provider.dart';
import '../providers/upload_provider.dart';
import '../../domain/entities/app_file.dart';

class B3UploadZone extends StatefulWidget {
  const B3UploadZone({super.key});

  @override
  State<B3UploadZone> createState() => _B3UploadZoneState();
}

class _B3UploadZoneState extends State<B3UploadZone> {
  bool _dragging = false;

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<UploadProvider>();

    return DropTarget(
      onDragDone: (detail) async {
        if (detail.files.isNotEmpty) {
          final file = detail.files.first;
          final bytes = await file.readAsBytes();
          provider.setFile(AppFile(
            name: file.name,
            bytes: bytes,
            size: bytes.length,
          ));
        }
      },
      onDragEntered: (detail) => setState(() => _dragging = true),
      onDragExited: (detail) => setState(() => _dragging = false),
      child: MouseRegion(
        cursor: SystemMouseCursors.click,
        child: GestureDetector(
          onTap: provider.pickFile,
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            padding: const EdgeInsets.all(40),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: _dragging || provider.hasFile
                    ? Theme.of(context).colorScheme.primary
                    : Theme.of(context).colorScheme.outline.withValues(alpha: 0.3),
                width: 2,
                style: BorderStyle.solid,
              ),
              color: _dragging
                  ? Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.1)
                  : Colors.transparent,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  provider.hasFile ? Icons.insert_drive_file_outlined : Icons.cloud_upload_outlined,
                  size: 64,
                  color: provider.hasFile
                      ? Theme.of(context).colorScheme.primary
                      : Theme.of(context).colorScheme.onSurfaceVariant,
                ),
                const SizedBox(height: 16),
                Text(
                  provider.hasFile
                      ? provider.selectedFile!.name
                      : 'Arraste sua planilha da B3 aqui',
                  style: Theme.of(context).textTheme.titleMedium,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  provider.hasFile
                      ? '${(provider.selectedFile!.size / 1024).toStringAsFixed(1)} KB'
                      : 'ou clique para selecionar (.xlsx)',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                if (provider.state == UploadState.uploading) ...[
                  const SizedBox(height: 24),
                  const LinearProgressIndicator(),
                  const SizedBox(height: 8),
                  const Text('Enviando para processamento...'),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
