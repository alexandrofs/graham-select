import '../../domain/entities/app_file.dart';
import '../../domain/entities/upload_processing_status.dart';
import '../../domain/entities/upload_result.dart';
import '../../domain/repositories/upload_repository.dart';
import '../datasources/upload_remote_data_source.dart';
import '../../../auth/data/auth_repository.dart';

class UploadRepositoryImpl implements UploadRepository {
  final UploadRemoteDataSource remoteDataSource;
  final AuthRepository authRepository;

  const UploadRepositoryImpl(this.remoteDataSource, this.authRepository);

  @override
  Future<UploadResult> uploadFile(AppFile file) async {
    try {
      final token = await authRepository.getPersistedToken();
      final responseModel = await remoteDataSource.uploadFile(file, token: token);
      return responseModel.toEntity();
    } catch (e) {
      return UploadResult(
        success: false,
        message: 'Erro ao conectar com o servidor: ${e.toString()}',
        statusCode: null,
      );
    }
  }

  @override
  Future<UploadResult> uploadB3File(AppFile file) async {
    try {
      final token = await authRepository.getPersistedToken();
      final responseModel = await remoteDataSource.uploadB3File(file, token: token);
      return responseModel.toEntity();
    } catch (e) {
      return UploadResult(
        success: false,
        message: 'Erro ao conectar com o servidor: ${e.toString()}',
        statusCode: null,
      );
    }
  }

  @override
  Future<UploadProcessingStatus> fetchB3UploadStatus(String correlationId) async {
    final token = await authRepository.getPersistedToken();
    final responseModel = await remoteDataSource.fetchB3UploadStatus(correlationId, token: token);
    return responseModel.toEntity();
  }
}
