import '../entities/app_file.dart';
import '../entities/upload_processing_status.dart';
import '../entities/upload_result.dart';

abstract class UploadRepository {
  Future<UploadResult> uploadFile(AppFile file);
  Future<UploadResult> uploadB3File(AppFile file);
  Future<UploadProcessingStatus> fetchB3UploadStatus(String correlationId);
}
