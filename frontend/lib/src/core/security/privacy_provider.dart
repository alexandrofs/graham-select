import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Gerencia o estado global do Modo Privacidade do Kerno, persistindo a
/// preferência localmente em [FlutterSecureStorage] sob a chave
/// `kerno_privacy_mode` e fornecendo formatação/mascaramento determinístico
/// para valores monetários em `R$`.
class PrivacyProvider extends ChangeNotifier {
  static const String storageKey = 'kerno_privacy_mode';
  static const String maskedCurrencyValue = 'R\$ ••••••';
  static const String maskedSemanticsLabel = 'Valor monetário oculto';

  final FlutterSecureStorage _storage;

  bool _isPrivacyMode;
  bool _isLoaded = false;
  int _epoch = 0;
  bool _disposed = false;

  PrivacyProvider({
    FlutterSecureStorage? storage,
    bool initialPrivacyMode = false,
  })  : _storage = storage ?? const FlutterSecureStorage(),
        _isPrivacyMode = initialPrivacyMode;

  bool get isPrivacyMode => _isPrivacyMode;
  bool get isLoaded => _isLoaded;

  @override
  void notifyListeners() {
    if (!_disposed) {
      super.notifyListeners();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }

  /// Carrega a preferência persistida em [FlutterSecureStorage].
  /// Em caso de storage vazio, corrompido ou erro de leitura, assume `false`
  /// sem lançar exceção.
  Future<void> load() async {
    final loadEpoch = _epoch;
    try {
      final raw = await _storage.read(key: storageKey);
      if (_disposed || loadEpoch != _epoch) return;
      final parsed = raw?.trim().toLowerCase() == 'true';
      final changed = parsed != _isPrivacyMode || !_isLoaded;
      _isPrivacyMode = parsed;
      _isLoaded = true;
      if (changed) {
        notifyListeners();
      }
    } catch (_) {
      if (_disposed || loadEpoch != _epoch) return;
      _isPrivacyMode = false;
      _isLoaded = true;
      notifyListeners();
    }
  }

  /// Alterna instantaneamente o Modo Privacidade em memória e persiste em
  /// background no storage seguro (tolerando falhas de I/O).
  Future<void> toggle() async {
    await setPrivacyMode(!_isPrivacyMode);
  }

  /// Define explicitamente o estado do Modo Privacidade.
  Future<void> setPrivacyMode(bool enabled) async {
    _epoch++;
    if (_isPrivacyMode == enabled && _isLoaded) {
      return;
    }
    _isPrivacyMode = enabled;
    _isLoaded = true;
    notifyListeners();
    try {
      await _storage.write(
        key: storageKey,
        value: enabled ? 'true' : 'false',
      );
    } catch (_) {
      // Falha de escrita no storage mantém o estado em memória sem lançar exceção.
    }
  }

  /// Retorna [maskedCurrencyValue] quando o Modo Privacidade estiver ativo,
  /// ou o resultado de [formatVisible] caso contrário.
  String formatCurrencyOrMask(String Function() formatVisible) {
    if (_isPrivacyMode) {
      return maskedCurrencyValue;
    }
    return formatVisible();
  }

  /// Retorna um rótulo acessível seguro quando o Modo Privacidade está ativo,
  /// impedindo que leitores de tela verbalizem cifras em `R$`.
  String semanticsForCurrency({
    required String visibleLabel,
    String? hiddenLabel,
  }) {
    if (_isPrivacyMode) {
      return hiddenLabel ?? maskedSemanticsLabel;
    }
    return visibleLabel;
  }
}
