import 'dart:typed_data';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:frontend/src/features/upload/domain/entities/app_file.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_file_usecase.dart';
import 'package:frontend/src/features/upload/domain/entities/upload_result.dart';
import 'package:frontend/src/features/upload/domain/repositories/upload_repository.dart';
import 'package:frontend/src/features/upload/presentation/providers/upload_provider.dart';

import 'upload_provider_test.mocks.dart';

// Create a simpler test by mocking the repository instead of the use case
@GenerateMocks([UploadRepository])
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
    final testBytes = Uint8List.fromList([1, 2, 3]);

    test('should show error for invalid file extension', () async {
      // Arrange
      final file = AppFile(
        name: 'file.txt',
        bytes: testBytes,
        size: testBytes.length,
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, false);
      expect(result.message, contains('Formato de arquivo não suportado'));
    });

    test('should show error for empty file', () async {
      // Arrange
      final file = AppFile(
        name: 'empty.csv',
        bytes: Uint8List(0),
        size: 0,
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, false);
      expect(result.message, 'O arquivo está vazio');
    });

    test('should show error for file larger than 10MB', () async {
      // Arrange
      final file = AppFile(
        name: 'large.csv',
        bytes: testBytes,
        size: 11 * 1024 * 1024, // 11 MB
      );

      // Act
      final result = await useCase(file);

      // Assert
      expect(result.success, false);
      expect(result.message, contains('Arquivo muito grande'));
    });

    test('should succeed with valid CSV file', () async {
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
    });
  });
}
