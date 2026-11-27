import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import '../../../core/api/api_client.dart';
import '../../../core/security/biometric_auth_service.dart';
import '../../../core/security/encrypted_portfolio_cache.dart';

class AuthRepository implements TokenProvider {
  static const String jwtTokenKey = 'jwt_token';
  static const String accessTokenKey = 'access_token';
  static const String refreshTokenKey = 'refresh_token';

  final FirebaseAuth _firebaseAuth;
  final GoogleSignIn _googleSignIn;
  final FlutterSecureStorage _storage;
  final EncryptedPortfolioCache? _portfolioCache;
  final BiometricAuthService? _biometricAuthService;
  final Future<void> Function()? _onClearCache;

  String? _cachedToken;
  String? _cachedAccessToken;
  String? _cachedRefreshToken;

  AuthRepository({
    required ApiClient apiClient,
    FirebaseAuth? firebaseAuth,
    GoogleSignIn? googleSignIn,
    FlutterSecureStorage? storage,
    EncryptedPortfolioCache? portfolioCache,
    BiometricAuthService? biometricAuthService,
    Future<void> Function()? onClearCache,
  })  : _firebaseAuth = firebaseAuth ?? FirebaseAuth.instance,
        _googleSignIn = googleSignIn ?? GoogleSignIn(
          clientId: const String.fromEnvironment('GOOGLE_CLIENT_ID', defaultValue: '').isEmpty 
              ? null 
              : const String.fromEnvironment('GOOGLE_CLIENT_ID'),
          serverClientId: kIsWeb || const String.fromEnvironment('GOOGLE_CLIENT_ID', defaultValue: '').isEmpty 
              ? null 
              : const String.fromEnvironment('GOOGLE_CLIENT_ID'),
        ),
        _storage = storage ?? EncryptedPortfolioCache.defaultSecureStorage,
        _portfolioCache = portfolioCache,
        _biometricAuthService = biometricAuthService,
        _onClearCache = onClearCache;

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
        await _storage.write(key: jwtTokenKey, value: idToken);
      }

      final String? accessToken = googleAuth.accessToken;
      if (accessToken != null && accessToken.isNotEmpty) {
        _cachedAccessToken = accessToken;
        await _storage.write(key: accessTokenKey, value: accessToken);
      }

      final String? refreshToken = userCredential.user?.refreshToken;
      if (refreshToken != null && refreshToken.isNotEmpty) {
        _cachedRefreshToken = refreshToken;
        await _storage.write(key: refreshTokenKey, value: refreshToken);
      }

      _biometricAuthService?.markInteractiveLoginSuccess();

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
    _cachedToken = await _storage.read(key: jwtTokenKey);
    return _cachedToken;
  }

  Future<String?> getAccessToken() async {
    if (_cachedAccessToken != null) return _cachedAccessToken;
    _cachedAccessToken = await _storage.read(key: accessTokenKey);
    return _cachedAccessToken;
  }

  Future<String?> getRefreshToken() async {
    if (_cachedRefreshToken != null) return _cachedRefreshToken;
    _cachedRefreshToken = await _storage.read(key: refreshTokenKey);
    return _cachedRefreshToken;
  }

  /// Remove tokens de sessão e caches locais criptografados.
  Future<void> clearSessionData() async {
    _cachedToken = null;
    _cachedAccessToken = null;
    _cachedRefreshToken = null;
    for (final key in const [jwtTokenKey, accessTokenKey, refreshTokenKey]) {
      try {
        await _storage.delete(key: key);
      } catch (_) {}
    }
    try {
      await _portfolioCache?.clearAll();
    } catch (_) {}
    try {
      if (_onClearCache != null) {
        await _onClearCache();
      }
    } catch (_) {}
  }

  Future<void> signOut() async {
    try {
      try {
        await _googleSignIn.signOut();
      } catch (_) {
        // Falha parcial no Google Sign-Out não deve impedir limpeza local
      }
      try {
        await _firebaseAuth.signOut();
      } catch (_) {
        // Falha parcial no Firebase Sign-Out não deve impedir limpeza local
      }
    } finally {
      await clearSessionData();
    }
  }
}
