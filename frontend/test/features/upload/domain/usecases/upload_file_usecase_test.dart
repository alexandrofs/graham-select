import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:frontend/src/features/upload/domain/entities/upload_result.dart';
import 'package:frontend/src/features/upload/domain/repositories/upload_repository.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_file_usecase.dart';

import 'upload_file_usecase_test.mocks.dart';

@GenerateMocks([UploadRepository, File])
void main() {
  late UploadFileUseCase useCase;
  late MockUploadRepository mockRepository;
  late MockFile mockFile;

  setUp(() {
    mockRepository = MockUploadRepository();
    useCase = UploadFileUseCase(mockRepository);
    mockFile = MockFile();
  });

  group('UploadFileUseCase', () {
    const testFilePath = '/path/to/test.csv';

    test('should return error when file does not exist', () async {
      // Arrange
      when(mockFile.path).thenReturn(testFilePath);
      when(mockFile.exists()).thenAnswer((_) async => false);

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, false);
      expect(result.message, 'Arquivo não encontrado');
      verifyNever(mockRepository.uploadFile(any));
    });

    test('should return error for invalid extension', () async {
      // Arrange
      when(mockFile.path).thenReturn('/path/to/test.txt');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => 1024);

      // Act
      final result = await useCase(mockFile);

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
      when(mockFile.path).thenReturn(testFilePath);
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => 0);

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, false);
      expect(result.message, 'O arquivo está vazio');
      verifyNever(mockRepository.uploadFile(any));
    });

    test('should return error for file larger than 10MB', () async {
      // Arrange
      const fileSize = 11 * 1024 * 1024; // 11 MB
      when(mockFile.path).thenReturn(testFilePath);
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => fileSize);

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, false);
      expect(result.message, contains('Arquivo muito grande'));
      expect(result.message, contains('11.00 MB'));
      verifyNever(mockRepository.uploadFile(any));
    });

    test('should call repository when file is valid CSV', () async {
      // Arrange
      const fileSize = 1024; // 1 KB
      when(mockFile.path).thenReturn('/path/to/valid.csv');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => fileSize);
      when(mockRepository.uploadFile(mockFile)).thenAnswer(
        (_) async => const UploadResult(
          success: true,
          message: 'Upload successful',
          statusCode: 200,
        ),
      );

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, true);
      expect(result.message, 'Upload successful');
      verify(mockRepository.uploadFile(mockFile)).called(1);
    });

    test('should call repository when file is valid XLS', () async {
      // Arrange
      const fileSize = 2048; // 2 KB
      when(mockFile.path).thenReturn('/path/to/valid.xls');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => fileSize);
      when(mockRepository.uploadFile(mockFile)).thenAnswer(
        (_) async => const UploadResult(
          success: true,
          message: 'Upload successful',
          statusCode: 200,
        ),
      );

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, true);
      verify(mockRepository.uploadFile(mockFile)).called(1);
    });

    test('should call repository when file is valid XLSX', () async {
      // Arrange
      const fileSize = 5 * 1024 * 1024; // 5 MB
      when(mockFile.path).thenReturn('/path/to/valid.xlsx');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => fileSize);
      when(mockRepository.uploadFile(mockFile)).thenAnswer(
        (_) async => const UploadResult(
          success: true,
          message: 'Upload successful',
          statusCode: 200,
        ),
      );

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, true);
      verify(mockRepository.uploadFile(mockFile)).called(1);
    });

    test('should handle repository errors', () async {
      // Arrange
      const fileSize = 1024;
      when(mockFile.path).thenReturn('/path/to/valid.csv');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => fileSize);
      when(mockRepository.uploadFile(mockFile)).thenAnswer(
        (_) async => const UploadResult(
          success: false,
          message: 'Server error',
          statusCode: 500,
        ),
      );

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, false);
      expect(result.message, 'Server error');
      expect(result.statusCode, 500);
    });
  });
}
