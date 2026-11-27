import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/core/security/biometric_auth_service.dart';
import 'package:frontend/src/core/security/encrypted_portfolio_cache.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';

@GenerateNiceMocks([
  MockSpec<ApiClient>(),
  MockSpec<FlutterSecureStorage>(),
  MockSpec<FirebaseAuth>(),
  MockSpec<GoogleSignIn>(),
  MockSpec<GoogleSignInAccount>(),
  MockSpec<GoogleSignInAuthentication>(),
  MockSpec<UserCredential>(),
  MockSpec<User>(),
  MockSpec<EncryptedPortfolioCache>(),
  MockSpec<BiometricAuthService>(),
])
import 'auth_repository_test.mocks.dart';

void main() {
  late AuthRepository authRepository;
  late MockApiClient mockApiClient;
  late MockFlutterSecureStorage mockStorage;
  late MockFirebaseAuth mockFirebaseAuth;
  late MockGoogleSignIn mockGoogleSignIn;

  setUp(() {
    mockApiClient = MockApiClient();
    mockStorage = MockFlutterSecureStorage();
    mockFirebaseAuth = MockFirebaseAuth();
    mockGoogleSignIn = MockGoogleSignIn();
    authRepository = AuthRepository(
      apiClient: mockApiClient,
      storage: mockStorage,
      firebaseAuth: mockFirebaseAuth,
      googleSignIn: mockGoogleSignIn,
    );
  });

  group('AuthRepository JWT Persistence Tests', () {
    test('getPersistedToken should read from storage then hit cache', () async {
      when(mockStorage.read(key: anyNamed('key'))).thenAnswer((_) async => 'mock_token');

      final token1 = await authRepository.getPersistedToken();
      expect(token1, 'mock_token');
      verify(mockStorage.read(key: 'jwt_token')).called(1);

      // Now it should be cached
      final token2 = await authRepository.getPersistedToken();
      expect(token2, 'mock_token');
      // Should not call storage again
      verifyNever(mockStorage.read(key: anyNamed('key')));
    });

    test('signOut should delete all tokens from storage and clear cache even on partial provider failure', () async {
      when(mockGoogleSignIn.signOut()).thenThrow(Exception('Google signOut failed'));
      when(mockFirebaseAuth.signOut()).thenThrow(Exception('Firebase signOut failed'));

      await authRepository.signOut();

      verify(mockStorage.delete(key: AuthRepository.jwtTokenKey)).called(1);
      verify(mockStorage.delete(key: AuthRepository.accessTokenKey)).called(1);
      verify(mockStorage.delete(key: AuthRepository.refreshTokenKey)).called(1);
    });

    test('signInWithGoogle should write idToken, accessToken and refreshToken to storage and cache them', () async {
      final mockGoogleUser = MockGoogleSignInAccount();
      final mockGoogleAuth = MockGoogleSignInAuthentication();
      final mockUserCredential = MockUserCredential();
      final mockUser = MockUser();

      when(mockGoogleSignIn.signIn()).thenAnswer((_) async => mockGoogleUser);
      when(mockGoogleUser.authentication).thenAnswer((_) async => mockGoogleAuth);
      when(mockGoogleAuth.accessToken).thenReturn('access_token_123');
      when(mockGoogleAuth.idToken).thenReturn('id_token');
      
      when(mockFirebaseAuth.signInWithCredential(any))
          .thenAnswer((_) async => mockUserCredential);
      when(mockUserCredential.user).thenReturn(mockUser);
      when(mockUser.getIdToken(any)).thenAnswer((_) async => 'fake_id_token');
      when(mockUser.refreshToken).thenReturn('refresh_token_123');

      await authRepository.signInWithGoogle();

      verify(mockStorage.write(key: AuthRepository.jwtTokenKey, value: 'fake_id_token')).called(1);
      verify(mockStorage.write(key: AuthRepository.accessTokenKey, value: 'access_token_123')).called(1);
      verify(mockStorage.write(key: AuthRepository.refreshTokenKey, value: 'refresh_token_123')).called(1);

      // Verify it's cached
      final cachedToken = await authRepository.getPersistedToken();
      expect(cachedToken, 'fake_id_token');
      expect(await authRepository.getAccessToken(), 'access_token_123');
      expect(await authRepository.getRefreshToken(), 'refresh_token_123');
      verifyNever(mockStorage.read(key: anyNamed('key')));
    });

    test('signInWithGoogle should short-circuit if googleUser is null', () async {
      when(mockGoogleSignIn.signIn()).thenAnswer((_) async => null);
      final result = await authRepository.signInWithGoogle();
      expect(result, isNull);
    });

    test('signInWithGoogle should rethrow exception from FirebaseAuth', () async {
      final mockGoogleUser = MockGoogleSignInAccount();
      final mockGoogleAuth = MockGoogleSignInAuthentication();

      when(mockGoogleSignIn.signIn()).thenAnswer((_) async => mockGoogleUser);
      when(mockGoogleUser.authentication).thenAnswer((_) async => mockGoogleAuth);
      when(mockGoogleAuth.accessToken).thenReturn('access_token');
      when(mockGoogleAuth.idToken).thenReturn('id_token');
      
      when(mockFirebaseAuth.signInWithCredential(any))
          .thenThrow(FirebaseAuthException(code: 'ERROR', message: 'Test Error'));

      expect(() => authRepository.signInWithGoogle(), throwsA(isA<FirebaseAuthException>()));
    });

    test('signOut should clear portfolioCache and biometric timestamp even if storage delete throws', () async {
      final mockCache = MockEncryptedPortfolioCache();
      final mockBiometric = MockBiometricAuthService();
      var onClearCacheCalled = false;
      final repoWithCache = AuthRepository(
        apiClient: mockApiClient,
        storage: mockStorage,
        firebaseAuth: mockFirebaseAuth,
        googleSignIn: mockGoogleSignIn,
        portfolioCache: mockCache,
        biometricAuthService: mockBiometric,
        onClearCache: () async {
          onClearCacheCalled = true;
          mockBiometric.clearBackgroundTimestamp();
        },
      );

      when(mockStorage.delete(key: AuthRepository.jwtTokenKey))
          .thenThrow(Exception('storage delete error'));

      await repoWithCache.signOut();

      verify(mockStorage.delete(key: AuthRepository.accessTokenKey)).called(1);
      verify(mockStorage.delete(key: AuthRepository.refreshTokenKey)).called(1);
      verify(mockCache.clearAll()).called(1);
      verify(mockBiometric.clearBackgroundTimestamp()).called(1);
      expect(onClearCacheCalled, isTrue);
    });
  });
}
