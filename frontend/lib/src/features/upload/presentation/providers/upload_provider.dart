import 'package:flutter/foundation.dart';
import 'package:file_picker/file_picker.dart';
import '../../domain/entities/app_file.dart';
import '../../domain/usecases/upload_file_usecase.dart';

enum UploadState { idle, picking, validating, uploading, success, error }

class UploadProvider extends ChangeNotifier {
  final UploadFileUseCase uploadFileUseCase;

  UploadState _state = UploadState.idle;
  AppFile? _selectedFile;
  String? _errorMessage;
  String? _successMessage;

  UploadProvider(this.uploadFileUseCase);

  UploadState get state => _state;
  AppFile? get selectedFile => _selectedFile;
  String? get errorMessage => _errorMessage;
  String? get successMessage => _successMessage;

  bool get hasFile => _selectedFile != null;
  bool get canUpload => hasFile && _state != UploadState.uploading;

  Future<void> pickFile() async {
    _setState(UploadState.picking);
    _clearMessages();

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['csv', 'xls', 'xlsx'],
        allowMultiple: false,
        withData: true, // Importante para Web!
      );

      if (result != null && result.files.isNotEmpty) {
        final file = result.files.first;
        if (file.bytes != null) {
          _selectedFile = AppFile(
            name: file.name,
            bytes: file.bytes!,
            size: file.size,
          );
          _setState(UploadState.idle);
        } else {
          _errorMessage = 'Não foi possível ler os dados do arquivo.';
          _setState(UploadState.error);
        }
      } else {
        // User canceled
        _setState(UploadState.idle);
      }
    } catch (e) {
      _errorMessage = 'Erro ao selecionar arquivo: ${e.toString()}';
      _setState(UploadState.error);
    }
  }

  Future<void> uploadFile() async {
    if (_selectedFile == null) return;

    _setState(UploadState.uploading);
    _clearMessages();

    try {
      final result = await uploadFileUseCase(_selectedFile!);

      if (result.success) {
        _successMessage = result.message;
        _setState(UploadState.success);
      } else {
        _errorMessage = result.message;
        _setState(UploadState.error);
      }
    } catch (e) {
      _errorMessage = 'Erro inesperado: ${e.toString()}';
      _setState(UploadState.error);
    }
  }

  void reset() {
    _selectedFile = null;
    _clearMessages();
    _setState(UploadState.idle);
  }

  void _setState(UploadState newState) {
    _state = newState;
    notifyListeners();
  }

  void _clearMessages() {
    _errorMessage = null;
    _successMessage = null;
  }
}
