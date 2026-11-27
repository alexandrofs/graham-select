import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/security/biometric_auth_service.dart';
import 'package:local_auth/local_auth.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';

@GenerateNiceMocks([MockSpec<LocalAuthentication>()])
import 'biometric_auth_service_test.mocks.dart';

void main() {
  late MockLocalAuthentication mockLocalAuth;
  late DateTime currentTime;
  late BiometricAuthService service;

  setUp(() {
    mockLocalAuth = MockLocalAuthentication();
    currentTime = DateTime.utc(2026, 11, 27, 12, 0, 0);
    service = BiometricAuthService(
      localAuth: mockLocalAuth,
      now: () => currentTime,
    );
  });

  group('BiometricAuthService', () {
    test('authenticate deve usar biometricOnly: false e retornar true no sucesso', () async {
      when(mockLocalAuth.canCheckBiometrics).thenAnswer((_) async => true);
      when(mockLocalAuth.isDeviceSupported()).thenAnswer((_) async => true);
      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenAnswer((_) async => true);

      final result = await service.authenticate();

      expect(result, isTrue);
      final captured = verify(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: captureAnyNamed('options'),
        ),
      ).captured;
      final options = captured.single as AuthenticationOptions;
      expect(options.biometricOnly, isFalse);
    });

    test('authenticate deve retornar false quando dispositivo não suporta autenticação', () async {
      when(mockLocalAuth.canCheckBiometrics).thenAnswer((_) async => false);
      when(mockLocalAuth.isDeviceSupported()).thenAnswer((_) async => false);

      final result = await service.authenticate();

      expect(result, isFalse);
      verifyNever(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      );
    });

    test('authenticate deve retornar false em PlatformException ou cancelamento', () async {
      when(mockLocalAuth.canCheckBiometrics).thenAnswer((_) async => true);
      when(mockLocalAuth.isDeviceSupported()).thenAnswer((_) async => true);
      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenThrow(PlatformException(code: 'NotAvailable'));

      final result = await service.authenticate();

      expect(result, isFalse);
    });

    test('shouldRequireReauth retorna false para <= 120s e true para > 120s', () {
      service.markBackgrounded();

      currentTime = currentTime.add(const Duration(seconds: 120));
      expect(service.shouldRequireReauth(), isFalse);

      currentTime = currentTime.add(const Duration(seconds: 1));
      expect(service.shouldRequireReauth(), isTrue);
    });

    test('shouldRequireReauth retorna true se o relógio retroceder (duration < 0)', () {
      service.markBackgrounded();
      currentTime = currentTime.subtract(const Duration(seconds: 5));

      expect(service.shouldRequireReauth(), isTrue);
    });
  });
}
