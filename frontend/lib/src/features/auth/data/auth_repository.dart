import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/api/api_client.dart';

class AuthRepository {
  final FirebaseAuth _firebaseAuth;
  final GoogleSignIn _googleSignIn;
  final FlutterSecureStorage _storage;

  AuthRepository({
    required ApiClient apiClient,
    FirebaseAuth? firebaseAuth,
    GoogleSignIn? googleSignIn,
    FlutterSecureStorage? storage,
  })  : _firebaseAuth = firebaseAuth ?? FirebaseAuth.instance,
        _googleSignIn = googleSignIn ?? GoogleSignIn(
          clientId: const String.fromEnvironment('GOOGLE_CLIENT_ID', defaultValue: '').isEmpty 
              ? null 
              : const String.fromEnvironment('GOOGLE_CLIENT_ID'),
        ),
        _storage = storage ?? const FlutterSecureStorage();

  Stream<User?> get authStateChanges => _firebaseAuth.authStateChanges();

  User? get currentUser => _firebaseAuth.currentUser;

  Future<UserCredential?> signInWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await _googleSignIn.signIn();
      if (googleUser == null) return null;

      final GoogleSignInAuthentication googleAuth = await googleUser.authentication;
      final OAuthCredential credential = GoogleAuthProvider.credential(
        accessToken: googleAuth.accessToken,
        idToken: googleAuth.idToken,
      );

      final UserCredential userCredential = await _firebaseAuth.signInWithCredential(credential);
      
      // Obter o ID Token para enviar ao backend (Resource Server)
      final String? idToken = await userCredential.user?.getIdToken();
      if (idToken != null) {
        // Persistir o ID Token diretamente. O backend validará este token nas APIs.
        await _storage.write(key: 'jwt_token', value: idToken);
      }

      return userCredential;
    } catch (e) {
      // Ignorando print em prod
      rethrow;
    }
  }

  /// Verifica se existe um token JWT persistido
  Future<String?> getPersistedToken() async {
    return await _storage.read(key: 'jwt_token');
  }

  Future<void> signOut() async {
    await _googleSignIn.signOut();
    await _firebaseAuth.signOut();
    await _storage.delete(key: 'jwt_token');
  }
}
