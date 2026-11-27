import 'package:local_auth/local_auth.dart';

/// Serviço centralizado de autenticação biométrica nativa e controle de
/// timeout de inatividade em background.
class BiometricAuthService {
  static const Duration defaultInactivityTimeout = Duration(minutes: 2);

  final LocalAuthentication _localAuth;
  final Duration inactivityTimeout;
  final DateTime Function() _now;

  DateTime? _backgroundedAt;
  bool _skipNextInitialLock = false;

  BiometricAuthService({
    LocalAuthentication? localAuth,
    this.inactivityTimeout = defaultInactivityTimeout,
    DateTime Function()? now,
  })  : _localAuth = localAuth ?? LocalAuthentication(),
        _now = now ?? (() => DateTime.now().toUtc());

  DateTime? get backgroundedAt => _backgroundedAt;

  /// Registra o instante em que o aplicativo entrou em background.
  /// Ignora eventos repetidos (`paused`/`hidden`) enquanto já houver marcador ativo.
  void markBackgrounded() {
    _backgroundedAt ??= _now();
  }

  /// Marca que o usuário acabou de concluir um login interativo e não deve
  /// ser bloqueado imediatamente na transição para a rota autenticada.
  void markInteractiveLoginSuccess() {
    _skipNextInitialLock = true;
    clearBackgroundTimestamp();
  }

  /// Consome a flag de bypass único pós-login interativo.
  bool consumeSkipNextInitialLock() {
    final skip = _skipNextInitialLock;
    _skipNextInitialLock = false;
    return skip;
  }

  /// Limpa o marcador de background após desbloqueio ou encerramento de sessão.
  void clearBackgroundTimestamp() {
    _backgroundedAt = null;
  }

  /// Retorna `true` se o tempo em background exceder [inactivityTimeout]
  /// (> 120s) ou se o relógio do dispositivo retroceder (`duration < 0`).
  bool shouldRequireReauth() {
    final backgroundedAt = _backgroundedAt;
    if (backgroundedAt == null) return false;

    final elapsed = _now().difference(backgroundedAt);
    if (elapsed.isNegative) {
      return true;
    }
    return elapsed > inactivityTimeout;
  }

  /// Verifica se o dispositivo suporta biometria ou credencial nativa do SO.
  Future<bool> isBiometricOrDeviceAuthSupported() async {
    try {
      final canCheckBiometrics = await _localAuth.canCheckBiometrics;
      final isDeviceSupported = await _localAuth.isDeviceSupported();
      return canCheckBiometrics || isDeviceSupported;
    } catch (_) {
      return false;
    }
  }

  /// Aciona o prompt nativo de biometria com fallback para PIN/senha do SO
  /// (`biometricOnly: false`). Retorna `false` em cancelamento, erro ou
  /// indisponibilidade.
  Future<bool> authenticate({
    String localizedReason = 'Desbloqueie o Kerno para acessar seu patrimônio',
  }) async {
    try {
      final supported = await isBiometricOrDeviceAuthSupported();
      if (!supported) {
        return false;
      }

      final authenticated = await _localAuth.authenticate(
        localizedReason: localizedReason,
        options: const AuthenticationOptions(
          biometricOnly: false,
          stickyAuth: true,
          useErrorDialogs: true,
        ),
      );
      if (authenticated) {
        clearBackgroundTimestamp();
      }
      return authenticated;
    } catch (_) {
      return false;
    }
  }
}
