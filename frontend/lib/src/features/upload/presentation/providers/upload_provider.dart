import 'package:flutter/foundation.dart';
import 'package:file_picker/file_picker.dart';
import 'dart:async';
import '../../domain/entities/app_file.dart';
import '../../domain/entities/upload_processing_status.dart';
import '../../domain/usecases/get_b3_upload_status_usecase.dart';
import '../../domain/usecases/upload_file_usecase.dart';
import '../../domain/usecases/upload_b3_file_usecase.dart';

enum UploadState { idle, picking, validating, uploading, success, error }

class UploadProvider extends ChangeNotifier {
  final UploadFileUseCase uploadFileUseCase;
  final UploadB3FileUseCase uploadB3FileUseCase;
  final GetB3UploadStatusUseCase getB3UploadStatusUseCase;

  UploadState _state = UploadState.idle;
  AppFile? _selectedFile;
  String? _errorMessage;
  String? _successMessage;
  UploadProcessingStatus? _processingStatus;
  Timer? _pollingTimer;

  UploadProvider({
    required this.uploadFileUseCase,
    required this.uploadB3FileUseCase,
    required this.getB3UploadStatusUseCase,
  });

  UploadState get state => _state;
  AppFile? get selectedFile => _selectedFile;
  String? get errorMessage => _errorMessage;
  String? get successMessage => _successMessage;
  UploadProcessingStatus? get processingStatus => _processingStatus;
  String get progressMessage =>
      _processingStatus?.message ?? 'Enviando para processamento...';

  bool get hasFile => _selectedFile != null;
  bool get canUpload => hasFile && _state != UploadState.uploading;

  Future<void> pickFile() async {
    _setState(UploadState.picking);
    _clearMessages();

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['xlsx'],
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

  Future<void> uploadB3File() async {
    if (_selectedFile == null) return;

    _stopPolling();
    _setState(UploadState.uploading);
    _clearMessages();
    _processingStatus = null;

    try {
      final result = await uploadB3FileUseCase(_selectedFile!);

      if (result.success && result.correlationId != null) {
        await _pollUploadStatus(result.correlationId!);
        _pollingTimer = Timer.periodic(
          const Duration(seconds: 2),
          (_) => _pollUploadStatus(result.correlationId!),
        );
      } else if (result.success) {
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
    _stopPolling();
    _selectedFile = null;
    _processingStatus = null;
    _clearMessages();
    _setState(UploadState.idle);
  }

  void setFile(AppFile file) {
    _selectedFile = file;
    _clearMessages();
    _setState(UploadState.idle);
  }

  Future<void> _pollUploadStatus(String correlationId) async {
    try {
      final status = await getB3UploadStatusUseCase(correlationId);
      _processingStatus = status;

      if (status.isFailed) {
        _errorMessage = status.message;
        _stopPolling();
        _setState(UploadState.error);
        return;
      }

      if (status.isCompleted) {
        _successMessage = status.message;
        _stopPolling();
        _setState(UploadState.success);
        return;
      }

      notifyListeners();
    } catch (e) {
      _errorMessage = 'Erro ao acompanhar processamento: ${e.toString()}';
      _stopPolling();
      _setState(UploadState.error);
    }
  }

  void _stopPolling() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
  }

  void _setState(UploadState newState) {
    _state = newState;
    notifyListeners();
  }

  void _clearMessages() {
    _errorMessage = null;
    _successMessage = null;
  }

  @override
  void dispose() {
    _stopPolling();
    super.dispose();
  }
}
