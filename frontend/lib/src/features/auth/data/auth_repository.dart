import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import '../../../core/api/api_client.dart';

class AuthRepository implements TokenProvider {
  final FirebaseAuth _firebaseAuth;
  final GoogleSignIn _googleSignIn;
  final FlutterSecureStorage _storage;
  String? _cachedToken;

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
          serverClientId: kIsWeb || const String.fromEnvironment('GOOGLE_CLIENT_ID', defaultValue: '').isEmpty 
              ? null 
              : const String.fromEnvironment('GOOGLE_CLIENT_ID'),
        ),
        _storage = storage ?? const FlutterSecureStorage();

  Stream<User?> get authStateChanges => _firebaseAuth.authStateChanges();

  User? get currentUser => _firebaseAuth.currentUser;

  Future<UserCredential?> signInWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = kIsWeb 
          ? await _googleSignIn.signInSilently() ?? await _googleSignIn.signIn()
          : await _googleSignIn.signIn();
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
        _cachedToken = idToken;
        // Persistir o ID Token diretamente. O backend validará este token nas APIs.
        await _storage.write(key: 'jwt_token', value: idToken);
      }

      return userCredential;
    } catch (e) {
      // Ignorando print em prod
      rethrow;
    }
  }

  @override
  /// Retorna um token JWT válido, renovando-o automaticamente se necessário.
  /// Tokens do Firebase expiram em 1 hora — sempre busca um token fresco do Firebase
  /// quando o usuário está logado, usando o storage apenas como fallback.
  Future<String?> getPersistedToken() async {
    // 1. Verifica cache em memória primeiro
    if (_cachedToken != null) return _cachedToken;

    // 2. Se há usuário Firebase logado, busca sempre um token fresco (auto-renova se expirado)
    final currentFirebaseUser = _firebaseAuth.currentUser;
    if (currentFirebaseUser != null) {
      try {
        final freshToken = await currentFirebaseUser.getIdToken(false);
        if (freshToken != null) {
          _cachedToken = freshToken;
          await _storage.write(key: 'jwt_token', value: freshToken);
          return freshToken;
        }
      } catch (_) {
        // Em caso de falha, cai no fallback do storage
      }
    }

    // 3. Fallback: lê do storage (usuário pode ter feito reload da página)
    _cachedToken = await _storage.read(key: 'jwt_token');
    return _cachedToken;
  }

  Future<void> signOut() async {
    await _googleSignIn.signOut();
    await _firebaseAuth.signOut();
    _cachedToken = null;
    await _storage.delete(key: 'jwt_token');
  }
}
