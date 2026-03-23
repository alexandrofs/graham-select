import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:dio/dio.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';

import 'profile_repository_test.mocks.dart';

@GenerateMocks([ApiClient])
void main() {
  late ProfileRepository repository;
  late MockApiClient mockApiClient;

  setUp(() {
    mockApiClient = MockApiClient();
    repository = ProfileRepository(mockApiClient);
  });

  group('ProfileRepository', () {
    test('should return UserProfile when API call is successful', () async {
      // Arrange
      final jsonResponse = {
        'id': 1,
        'email': 'user@example.com',
        'fullName': 'Test User',
        'tier': 'TRIAL',
        'trialEndsAt': '2026-04-22T20:24:51Z',
        'daysRemaining': 30,
      };
      
      final response = Response(
        data: jsonResponse,
        statusCode: 200,
        requestOptions: RequestOptions(path: '/api/v1/users/me'),
      );

      when(mockApiClient.get('/api/v1/users/me')).thenAnswer((_) async => response);

      // Act
      final result = await repository.getProfile();

      // Assert
      expect(result.id, 1);
      expect(result.tier, 'TRIAL');
      verify(mockApiClient.get('/api/v1/users/me')).called(1);
    });

    test('should throw exception when API call returns error code', () async {
      // Arrange
      final response = Response(
        data: 'Not Found',
        statusCode: 404,
        requestOptions: RequestOptions(path: '/api/v1/users/me'),
      );

      when(mockApiClient.get('/api/v1/users/me')).thenAnswer((_) async => response);

      // Act & Assert
      expect(() => repository.getProfile(), throwsException);
    });

    test('should throw exception when API call fails', () async {
      // Arrange
      when(mockApiClient.get('/api/v1/users/me')).thenThrow(DioException(
        requestOptions: RequestOptions(path: '/api/v1/users/me'),
        error: 'Network error',
      ));

      // Act & Assert
      expect(() => repository.getProfile(), throwsException);
    });
  });
}
