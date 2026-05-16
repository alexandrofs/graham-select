import '../../domain/entities/app_file.dart';
import '../../domain/entities/upload_result.dart';
import '../../domain/repositories/upload_repository.dart';
import '../datasources/upload_remote_data_source.dart';

class UploadRepositoryImpl implements UploadRepository {
  final UploadRemoteDataSource remoteDataSource;

  const UploadRepositoryImpl(this.remoteDataSource);

  @override
  Future<UploadResult> uploadFile(AppFile file) async {
    try {
      final responseModel = await remoteDataSource.uploadFile(file);
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
      final responseModel = await remoteDataSource.uploadB3File(file);
      return responseModel.toEntity();
    } catch (e) {
      return UploadResult(
        success: false,
        message: 'Erro ao conectar com o servidor: ${e.toString()}',
        statusCode: null,
      );
    }
  }
}
