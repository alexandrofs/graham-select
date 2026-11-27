import 'package:flutter/foundation.dart'
    show TargetPlatform, defaultTargetPlatform, kIsWeb;
import 'package:flutter/services.dart';

/// Encapsula o canal nativo (`kerno/snapshot_protection`) responsável por
/// ativar `FLAG_SECURE` no Android e a cobertura de proteção de snapshot no iOS.
///
/// Em Web ou ambientes de teste sem plugin registrado ([MissingPluginException]),
/// falha silenciosamente sem propagar exceções.
class SnapshotProtectionService {
  static const String channelName = 'kerno/snapshot_protection';

  final MethodChannel _channel;
  final bool? _isWebOverride;
  final TargetPlatform? _platformOverride;

  SnapshotProtectionService({
    MethodChannel? channel,
    bool? isWebOverride,
    TargetPlatform? platformOverride,
  })  : _channel = channel ?? const MethodChannel(channelName),
        _isWebOverride = isWebOverride,
        _platformOverride = platformOverride;

  bool get _isMobilePlatform {
    if (_isWebOverride == true) return false;
    if (_isWebOverride == null && kIsWeb) return false;
    final platform = _platformOverride ?? defaultTargetPlatform;
    return platform == TargetPlatform.android || platform == TargetPlatform.iOS;
  }

  Future<void> enableProtection() async {
    if (!_isMobilePlatform) return;
    try {
      await _channel.invokeMethod<void>('enableProtection');
    } on MissingPluginException {
      // Ambiente de teste ou plataforma sem handler nativo registrado.
    } on PlatformException {
      // Falha silenciosa para não interromper o fluxo de ciclo de vida.
    }
  }

  Future<void> disableProtection() async {
    if (!_isMobilePlatform) return;
    try {
      await _channel.invokeMethod<void>('disableProtection');
    } on MissingPluginException {
      // Ambiente de teste ou plataforma sem handler nativo registrado.
    } on PlatformException {
      // Falha silenciosa para não interromper o fluxo de ciclo de vida.
    }
  }
}
