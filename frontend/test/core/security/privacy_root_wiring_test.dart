import 'dart:io';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';
import 'package:frontend/main.dart';
import 'package:frontend/src/core/api/api_client.dart';
import 'package:frontend/src/core/security/privacy_provider.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';

class _FakeAuthRepository extends Mock implements AuthRepository {
  @override
  User? get currentUser => null;

  @override
  Future<String?> getPersistedToken() async => null;

  @override
  Stream<User?> get authStateChanges => const Stream.empty();
}

class _FakeApiClient extends Mock implements ApiClient {}

class _PreloadedSecureStorage extends Fake implements FlutterSecureStorage {
  final Map<String, String?> _data;

  _PreloadedSecureStorage(this._data);

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
    _data[key] = value;
  }
}

void main() {
  testWidgets(
    'GrahamSelectApp executa PrivacyProvider.load() no initState e aplica estado persistido',
    (tester) async {
      final storage = _PreloadedSecureStorage({
        PrivacyProvider.storageKey: 'true',
      });

      await tester.pumpWidget(
        GrahamSelectApp(
          authRepository: _FakeAuthRepository(),
          apiClient: _FakeApiClient(),
          storage: storage,
        ),
      );
      await tester.pumpAndSettle(const Duration(milliseconds: 300));

      final materialAppContext = tester.element(find.byType(MaterialApp));
      final privacyProvider = Provider.of<PrivacyProvider>(
        materialAppContext,
        listen: false,
      );

      expect(privacyProvider.isLoaded, isTrue);
      expect(privacyProvider.isPrivacyMode, isTrue);
    },
  );

  test('push-sw.js referencia badge monocromático transparente e asset existe', () {
    final swContent = File('web/push-sw.js').readAsStringSync();
    expect(swContent, contains("badge: '/icons/badge-monochrome.png'"));
    expect(File('web/icons/badge-monochrome.png').existsSync(), isTrue);
    expect(
      File('android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml').existsSync(),
      isTrue,
    );
  });
}
