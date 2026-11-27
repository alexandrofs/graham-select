import 'dart:async';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/security/privacy_provider.dart';

class _FakeSecureStorage extends Fake implements FlutterSecureStorage {
  final Map<String, String?> _data = <String, String?>{};
  bool throwOnRead = false;
  bool throwOnWrite = false;
  Completer<String?>? pendingRead;

  @override
  Future<String?> read({
    required String key,
    AppleOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    AppleOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    if (pendingRead != null) {
      return pendingRead!.future;
    }
    if (throwOnRead) {
      throw Exception('read error');
    }
    return _data[key];
  }

  @override
  Future<void> write({
    required String key,
    required String? value,
    AppleOptions? iOptions,
    AndroidOptions? aOptions,
    LinuxOptions? lOptions,
    WebOptions? webOptions,
    AppleOptions? mOptions,
    WindowsOptions? wOptions,
  }) async {
    if (throwOnWrite) {
      throw Exception('write error');
    }
    _data[key] = value;
  }
}

void main() {
  group('PrivacyProvider', () {
    test('assume false por padrão quando storage está vazio ou corrompido', () async {
      final storage = _FakeSecureStorage();
      final provider = PrivacyProvider(storage: storage);

      await provider.load();
      expect(provider.isPrivacyMode, isFalse);
      expect(provider.isLoaded, isTrue);

      storage._data[PrivacyProvider.storageKey] = 'invalid_value';
      await provider.load();
      expect(provider.isPrivacyMode, isFalse);
    });

    test('carrega isPrivacyMode=true quando kerno_privacy_mode="true"', () async {
      final storage = _FakeSecureStorage();
      storage._data[PrivacyProvider.storageKey] = 'true';
      final provider = PrivacyProvider(storage: storage);

      await provider.load();
      expect(provider.isPrivacyMode, isTrue);
    });

    test('tolera falha de leitura e escrita no storage sem lançar exceção', () async {
      final storage = _FakeSecureStorage()
        ..throwOnRead = true
        ..throwOnWrite = true;
      final provider = PrivacyProvider(storage: storage);

      await provider.load();
      expect(provider.isPrivacyMode, isFalse);

      await provider.toggle();
      expect(provider.isPrivacyMode, isTrue);
    });

    test('toggle persiste preferência e formatCurrencyOrMask mascara valores em R\$', () async {
      final storage = _FakeSecureStorage();
      final provider = PrivacyProvider(storage: storage);

      expect(provider.formatCurrencyOrMask(() => 'R\$ 1.000,00'), 'R\$ 1.000,00');
      expect(
        provider.semanticsForCurrency(visibleLabel: 'Saldo R\$ 1.000,00'),
        'Saldo R\$ 1.000,00',
      );

      await provider.toggle();

      expect(provider.isPrivacyMode, isTrue);
      expect(storage._data[PrivacyProvider.storageKey], 'true');
      expect(
        provider.formatCurrencyOrMask(() => 'R\$ 1.000,00'),
        PrivacyProvider.maskedCurrencyValue,
      );
      expect(
        provider.semanticsForCurrency(visibleLabel: 'Saldo R\$ 1.000,00'),
        PrivacyProvider.maskedSemanticsLabel,
      );
    });

    test('load() concorrente não sobrescreve toggle() nem notifica após dispose()', () async {
      final storage = _FakeSecureStorage();
      final completer = Completer<String?>();
      storage.pendingRead = completer;

      final provider = PrivacyProvider(storage: storage);
      final loadFuture = provider.load();

      // Usuário alterna antes da leitura inicial concluir
      await provider.toggle();
      expect(provider.isPrivacyMode, isTrue);

      // Leitura antiga retorna 'false', mas não deve sobrescrever
      completer.complete('false');
      await loadFuture;
      expect(provider.isPrivacyMode, isTrue);

      // Dispose antes de outra leitura pendente concluir não deve lançar erro
      final completer2 = Completer<String?>();
      storage.pendingRead = completer2;
      final provider2 = PrivacyProvider(storage: storage);
      final loadFuture2 = provider2.load();
      provider2.dispose();
      completer2.complete('true');
      await expectLater(loadFuture2, completes);
    });
  });
}
