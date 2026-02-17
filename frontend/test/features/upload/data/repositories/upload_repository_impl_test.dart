import 'dart:typed_data';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:frontend/src/features/upload/domain/entities/app_file.dart';
import 'package:frontend/src/features/upload/data/datasources/upload_remote_data_source.dart';
import 'package:frontend/src/features/upload/data/repositories/upload_repository_impl.dart';
import 'package:frontend/src/features/upload/data/models/upload_response_model.dart';

import 'upload_repository_impl_test.mocks.dart';

@GenerateMocks([UploadRemoteDataSource])
void main() {
  late UploadRepositoryImpl repository;
  late MockUploadRemoteDataSource mockDataSource;

  setUp(() {
    mockDataSource = MockUploadRemoteDataSource();
    repository = UploadRepositoryImpl(mockDataSource);
  });

  group('UploadRepositoryImpl', () {
    final testAppFile = AppFile(
      name: 'test.csv',
      bytes: Uint8List.fromList([1, 2, 3]),
      size: 3,
    );

    test('should return success result when data source succeeds', () async {
      // Arrange
      const responseModel = UploadResponseModel(
        message: 'Upload completed successfully',
        statusCode: 200,
      );
      when(
        mockDataSource.uploadFile(testAppFile),
      ).thenAnswer((_) async => responseModel);

      // Act
      final result = await repository.uploadFile(testAppFile);

      // Assert
      expect(result.success, true);
      expect(result.message, 'Upload completed successfully');
      expect(result.statusCode, 200);
      verify(mockDataSource.uploadFile(testAppFile)).called(1);
    });

    test('should return error result when data source returns 400', () async {
      // Arrange
      const responseModel = UploadResponseModel(
        message: 'Invalid file format',
        statusCode: 400,
      );
      when(
        mockDataSource.uploadFile(testAppFile),
      ).thenAnswer((_) async => responseModel);

      // Act
      final result = await repository.uploadFile(testAppFile);

      // Assert
      expect(result.success, false);
      expect(result.message, 'Invalid file format');
      expect(result.statusCode, 400);
    });

    test('should return error result when data source returns 500', () async {
      // Arrange
      const responseModel = UploadResponseModel(
        message: 'Internal server error',
        statusCode: 500,
      );
      when(
        mockDataSource.uploadFile(testAppFile),
      ).thenAnswer((_) async => responseModel);

      // Act
      final result = await repository.uploadFile(testAppFile);

      // Assert
      expect(result.success, false);
      expect(result.message, 'Internal server error');
      expect(result.statusCode, 500);
    });

    test('should handle data source exceptions', () async {
      // Arrange
      when(
        mockDataSource.uploadFile(testAppFile),
      ).thenThrow(Exception('Network error'));

      // Act
      final result = await repository.uploadFile(testAppFile);

      // Assert
      expect(result.success, false);
      expect(result.message, contains('Erro ao conectar com o servidor'));
      expect(result.message, contains('Network error'));
      expect(result.statusCode, null);
    });
  });
}
