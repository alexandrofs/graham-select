import '../entities/app_file.dart';
import '../entities/upload_result.dart';
import '../repositories/upload_repository.dart';

class UploadB3FileUseCase {
  final UploadRepository repository;

  UploadB3FileUseCase(this.repository);

  Future<UploadResult> call(AppFile file) {
    return repository.uploadB3File(file);
  }
}
