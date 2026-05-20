import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:frontend/src/core/api/api_client.dart';
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

    test('signOut should delete token from storage and clear cache', () async {
      await authRepository.signOut();
      verify(mockStorage.delete(key: anyNamed('key'))).called(1);
    });

    test('signInWithGoogle should write idToken to storage and cache it', () async {
      final mockGoogleUser = MockGoogleSignInAccount();
      final mockGoogleAuth = MockGoogleSignInAuthentication();
      final mockUserCredential = MockUserCredential();
      final mockUser = MockUser();

      when(mockGoogleSignIn.signIn()).thenAnswer((_) async => mockGoogleUser);
      when(mockGoogleUser.authentication).thenAnswer((_) async => mockGoogleAuth);
      when(mockGoogleAuth.accessToken).thenReturn('access_token');
      when(mockGoogleAuth.idToken).thenReturn('id_token');
      
      when(mockFirebaseAuth.signInWithCredential(any))
          .thenAnswer((_) async => mockUserCredential);
      when(mockUserCredential.user).thenReturn(mockUser);
      when(mockUser.getIdToken(any)).thenAnswer((_) async => 'fake_id_token');

      await authRepository.signInWithGoogle();

      verify(mockStorage.write(key: anyNamed('key'), value: anyNamed('value'))).called(1);

      // Verify it's cached
      final cachedToken = await authRepository.getPersistedToken();
      expect(cachedToken, 'fake_id_token');
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
  });
}
