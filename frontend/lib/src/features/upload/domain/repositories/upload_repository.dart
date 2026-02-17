import '../entities/app_file.dart';
import '../entities/upload_result.dart';

abstract class UploadRepository {
  Future<UploadResult> uploadFile(AppFile file);
}
