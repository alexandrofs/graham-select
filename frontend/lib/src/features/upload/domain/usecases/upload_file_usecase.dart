import '../entities/app_file.dart';
import '../entities/upload_result.dart';
import '../repositories/upload_repository.dart';

class UploadFileUseCase {
  final UploadRepository repository;

  // Validação de extensões permitidas
  static const List<String> allowedExtensions = ['.csv', '.xls', '.xlsx'];

  // Tamanho máximo: 10 MB
  static const int maxFileSize = 10 * 1024 * 1024; // 10 MB em bytes

  const UploadFileUseCase(this.repository);

  Future<UploadResult> call(AppFile file) async {
    // Validar se o arquivo tem dados
    if (file.bytes.isEmpty) {
      return const UploadResult(
        success: false,
        message: 'O arquivo está vazio',
      );
    }

    // Validar extensão
    final extension = _getFileExtension(file.name).toLowerCase();
    if (!allowedExtensions.contains(extension)) {
      return UploadResult(
        success: false,
        message:
            'Formato de arquivo não suportado. Envie arquivos CSV, XLS ou XLSX.',
      );
    }

    // Validar tamanho
    final fileSize = file.size;
    if (fileSize > maxFileSize) {
      final fileSizeMB = (fileSize / (1024 * 1024)).toStringAsFixed(2);
      return UploadResult(
        success: false,
        message:
            'Arquivo muito grande ($fileSizeMB MB). Tamanho máximo: 10 MB.',
      );
    }

    // Se passou em todas as validações, delegar para o repositório
    return await repository.uploadFile(file);
  }

  String _getFileExtension(String path) {
    final dotIndex = path.lastIndexOf('.');
    if (dotIndex == -1 || dotIndex == path.length - 1) {
      return '';
    }
    return path.substring(dotIndex);
  }
}
