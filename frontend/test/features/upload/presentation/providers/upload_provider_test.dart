import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_file_usecase.dart';
import 'package:frontend/src/features/upload/domain/entities/upload_result.dart';
import 'package:frontend/src/features/upload/domain/repositories/upload_repository.dart';
import 'package:frontend/src/features/upload/presentation/providers/upload_provider.dart';

import 'upload_provider_test.mocks.dart';

// Create a simpler test by mocking the repository instead of the use case
@GenerateMocks([UploadRepository, File])
void main() {
  late UploadProvider provider;
  late UploadFileUseCase useCase;
  late MockUploadRepository mockRepository;

  setUp(() {
    mockRepository = MockUploadRepository();
    useCase = UploadFileUseCase(mockRepository);
    provider = UploadProvider(useCase);
  });

  group('UploadProvider', () {
    test('initial state should be idle', () {
      expect(provider.state, UploadState.idle);
      expect(provider.selectedFile, null);
      expect(provider.errorMessage, null);
      expect(provider.successMessage, null);
      expect(provider.hasFile, false);
      expect(provider.canUpload, false);
    });

    test('reset should clear state and selected file', () {
      // Act
      provider.reset();

      // Assert
      expect(provider.state, UploadState.idle);
      expect(provider.selectedFile, null);
      expect(provider.errorMessage, null);
      expect(provider.successMessage, null);
    });

    test('uploadFile should do nothing when no file is selected', () async {
      // Arrange - no file selected
      expect(provider.selectedFile, null);

      // Act
      await provider.uploadFile();

      // Assert
      expect(provider.state, UploadState.idle);
      verifyNever(mockRepository.uploadFile(any));
    });
  });

  group('UploadProvider with file validation', () {
    test('should show error for file that does not exist', () async {
      // Arrange
      final mockFile = MockFile();
      when(mockFile.path).thenReturn('/path/to/nonexistent.csv');
      when(mockFile.exists()).thenAnswer((_) async => false);

      // Mock pickFile to set the file (simulating file selection)
      // Since we can't directly set selectedFile, we'll test validation through useCase
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, false);
      expect(result.message, 'Arquivo não encontrado');
    });

    test('should show error for invalid file extension', () async {
      // Arrange
      final mockFile = MockFile();
      when(mockFile.path).thenReturn('/path/to/file.txt');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => 1024);

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, false);
      expect(result.message, contains('Formato de arquivo não suportado'));
    });

    test('should show error for empty file', () async {
      // Arrange
      final mockFile = MockFile();
      when(mockFile.path).thenReturn('/path/to/empty.csv');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => 0);

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, false);
      expect(result.message, 'O arquivo está vazio');
    });

    test('should show error for file larger than 10MB', () async {
      // Arrange
      final mockFile = MockFile();
      when(mockFile.path).thenReturn('/path/to/large.csv');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(
        mockFile.length(),
      ).thenAnswer((_) async => 11 * 1024 * 1024); // 11 MB

      // Act
      final result = await useCase(mockFile);

      // Assert
      expect(result.success, false);
      expect(result.message, contains('Arquivo muito grande'));
    });

    test('should succeed with valid CSV file', () async {
      // Arrange
      final mockFile = MockFile();
      when(mockFile.path).thenReturn('/path/to/valid.csv');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => 1024);
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

    test('should handle repository errors', () async {
      // Arrange
      final mockFile = MockFile();
      when(mockFile.path).thenReturn('/path/to/valid.csv');
      when(mockFile.exists()).thenAnswer((_) async => true);
      when(mockFile.length()).thenAnswer((_) async => 1024);
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
    });
  });
}
