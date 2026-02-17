import 'dart:typed_data';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:frontend/src/features/upload/domain/entities/app_file.dart';
import 'package:frontend/src/features/upload/domain/entities/upload_result.dart';
import 'package:frontend/src/features/upload/domain/repositories/upload_repository.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_file_usecase.dart';

import 'upload_file_usecase_test.mocks.dart';

@GenerateMocks([UploadRepository])
void main() {
  late UploadFileUseCase useCase;
  late MockUploadRepository mockRepository;

  setUp(() {
    mockRepository = MockUploadRepository();
    useCase = UploadFileUseCase(mockRepository);
  });

  group('UploadFileUseCase', () {
    final testBytes = Uint8List.fromList([1, 2, 3]);

    test('should return error for invalid extension', () async {
      // Arrange
      final file = AppFile(
        name: 'test.txt',
        bytes: testBytes,
        size: testBytes.length,
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, false);
      expect(
        result.message,
        'Formato de arquivo não suportado. Envie arquivos CSV, XLS ou XLSX.',
      );
      verifyNever(mockRepository.uploadFile(any));
    });

    test('should return error for empty file', () async {
      // Arrange
      final file = AppFile(
        name: 'test.csv',
        bytes: Uint8List(0),
        size: 0,
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, false);
      expect(result.message, 'O arquivo está vazio');
      verifyNever(mockRepository.uploadFile(any));
    });

    test('should return error for file larger than 10MB', () async {
      // Arrange
      const fileSize = 11 * 1024 * 1024; // 11 MB
      final file = AppFile(
        name: 'large.csv',
        bytes: testBytes, // real bytes don't need to be 11MB for the test logic
        size: fileSize,
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, false);
      expect(result.message, contains('Arquivo muito grande'));
      expect(result.message, contains('11.00 MB'));
      verifyNever(mockRepository.uploadFile(any));
    });

    test('should call repository when file is valid CSV', () async {
      // Arrange
      final file = AppFile(
        name: 'valid.csv',
        bytes: testBytes,
        size: testBytes.length,
      );
      
      when(mockRepository.uploadFile(file)).thenAnswer(
        (_) async => const UploadResult(
          success: true,
          message: 'Upload successful',
          statusCode: 200,
        ),
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, true);
      expect(result.message, 'Upload successful');
      verify(mockRepository.uploadFile(file)).called(1);
    });

    test('should call repository when file is valid XLS', () async {
      // Arrange
      final file = AppFile(
        name: 'valid.xls',
        bytes: testBytes,
        size: testBytes.length,
      );
      
      when(mockRepository.uploadFile(file)).thenAnswer(
        (_) async => const UploadResult(
          success: true,
          message: 'Upload successful',
          statusCode: 200,
        ),
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, true);
      verify(mockRepository.uploadFile(file)).called(1);
    });

    test('should call repository when file is valid XLSX', () async {
      // Arrange
      final file = AppFile(
        name: 'valid.xlsx',
        bytes: testBytes,
        size: testBytes.length,
      );
      
      when(mockRepository.uploadFile(file)).thenAnswer(
        (_) async => const UploadResult(
          success: true,
          message: 'Upload successful',
          statusCode: 200,
        ),
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, true);
      verify(mockRepository.uploadFile(file)).called(1);
    });

    test('should handle repository errors', () async {
      // Arrange
      final file = AppFile(
        name: 'valid.csv',
        bytes: testBytes,
        size: testBytes.length,
      );
      
      when(mockRepository.uploadFile(file)).thenAnswer(
        (_) async => const UploadResult(
          success: false,
          message: 'Server error',
          statusCode: 500,
        ),
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, false);
      expect(result.message, 'Server error');
      expect(result.statusCode, 500);
    });
  });
}
