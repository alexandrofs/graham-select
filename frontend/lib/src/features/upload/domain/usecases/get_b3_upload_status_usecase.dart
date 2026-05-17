import '../entities/upload_processing_status.dart';
import '../repositories/upload_repository.dart';

class GetB3UploadStatusUseCase {
  final UploadRepository repository;

  GetB3UploadStatusUseCase(this.repository);

  Future<UploadProcessingStatus> call(String correlationId) {
    return repository.fetchB3UploadStatus(correlationId);
  }
}
