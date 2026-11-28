import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/security/biometric_auth_service.dart';
import 'package:frontend/src/core/security/biometric_lock_gate.dart';
import 'package:frontend/src/core/security/snapshot_protection_service.dart';
import 'package:mockito/mockito.dart';

import 'biometric_auth_service_test.mocks.dart';

void main() {
  late MockLocalAuthentication mockLocalAuth;
  late DateTime currentTime;
  late BiometricAuthService biometricService;

  setUp(() {
    mockLocalAuth = MockLocalAuthentication();
    currentTime = DateTime.utc(2026, 11, 27, 12, 0, 0);
    biometricService = BiometricAuthService(
      localAuth: mockLocalAuth,
      now: () => currentTime,
    );
    when(mockLocalAuth.canCheckBiometrics).thenAnswer((_) async => true);
    when(mockLocalAuth.isDeviceSupported()).thenAnswer((_) async => true);
  });

  group('BiometricLockGate', () {
    testWidgets('bloqueia conteúdo na abertura em mobile e libera após biometria aprovada', (tester) async {
      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenAnswer((_) async => true);

      await tester.pumpWidget(
        MaterialApp(
          home: BiometricLockGate(
            biometricAuthService: biometricService,
            isWebOverride: false,
            onSignOut: () async {},
            child: const Text('DADOS_PATRIMONIAIS_SENSIVEIS'),
          ),
        ),
      );

      // Antes da conclusão do frame de desbloqueio, dados não são renderizados
      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsNothing);
      expect(find.byKey(const Key('biometric_lock_screen')), findsOneWidget);

      await tester.pumpAndSettle();

      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsOneWidget);
      expect(find.byKey(const Key('biometric_lock_screen')), findsNothing);
    });

    testWidgets('mantém bloqueio com botões Tentar novamente e Sair quando biometria falha', (tester) async {
      var authCalls = 0;
      var signOutCalled = false;
      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenAnswer((_) async {
        authCalls++;
        return authCalls >= 2;
      });

      await tester.pumpWidget(
        MaterialApp(
          home: BiometricLockGate(
            biometricAuthService: biometricService,
            isWebOverride: false,
            onSignOut: () async {
              signOutCalled = true;
            },
            child: const Text('DADOS_PATRIMONIAIS_SENSIVEIS'),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsNothing);
      expect(find.byKey(const Key('biometric_lock_screen')), findsOneWidget);
      expect(find.byKey(const Key('biometric_retry_button')), findsOneWidget);
      expect(find.byKey(const Key('biometric_logout_button')), findsOneWidget);

      // Tenta novamente e aprova
      await tester.tap(find.byKey(const Key('biometric_retry_button')));
      await tester.pumpAndSettle();

      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsOneWidget);
      expect(signOutCalled, isFalse);
    });

    testWidgets('botão Sair aciona onSignOut quando bloqueado', (tester) async {
      var signOutCalled = false;
      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenAnswer((_) async => false);

      await tester.pumpWidget(
        MaterialApp(
          home: BiometricLockGate(
            biometricAuthService: biometricService,
            isWebOverride: false,
            onSignOut: () async {
              signOutCalled = true;
            },
            child: const Text('DADOS_PATRIMONIAIS_SENSIVEIS'),
          ),
        ),
      );

      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('biometric_logout_button')));
      await tester.pumpAndSettle();

      expect(signOutCalled, isTrue);
    });

    testWidgets('re-bloqueia após > 2 min em background e não re-bloqueia em <= 2 min', (tester) async {
      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenAnswer((_) async => true);

      await tester.pumpWidget(
        MaterialApp(
          home: BiometricLockGate(
            biometricAuthService: biometricService,
            isWebOverride: false,
            onSignOut: () async {},
            child: const Text('DADOS_PATRIMONIAIS_SENSIVEIS'),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsOneWidget);

      // Retorno rápido (<= 2 min)
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      currentTime = currentTime.add(const Duration(seconds: 120));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pumpAndSettle();

      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsOneWidget);
      verify(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).called(1);

      // Retorno após > 2 min — usa Completer para observar estado bloqueado antes da aprovação
      final reauthCompleter = Completer<bool>();
      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenAnswer((_) => reauthCompleter.future);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      currentTime = currentTime.add(const Duration(seconds: 121));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump();

      expect(find.byKey(const Key('biometric_lock_screen')), findsOneWidget);
      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsNothing);

      reauthCompleter.complete(true);
      await tester.pumpAndSettle();
      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsOneWidget);
    });

    testWidgets('em kIsWeb (isWebOverride: true) não exibe gate biométrico', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: BiometricLockGate(
            biometricAuthService: biometricService,
            isWebOverride: true,
            onSignOut: () async {},
            child: const Text('DADOS_PATRIMONIAIS_SENSIVEIS'),
          ),
        ),
      );

      expect(find.text('DADOS_PATRIMONIAIS_SENSIVEIS'), findsOneWidget);
      expect(find.byKey(const Key('biometric_lock_screen')), findsNothing);
    });

    testWidgets(
        'exibe overlay de proteção imediatamente em AppLifecycleState.inactive e aciona canal nativo',
        (tester) async {
      final recordedCalls = <String>[];
      const channel = MethodChannel(SnapshotProtectionService.channelName);
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        channel,
        (call) async {
          recordedCalls.add(call.method);
          return null;
        },
      );
      addTearDown(
        () => tester.binding.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null),
      );

      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenAnswer((_) async => true);

      final snapshotService = SnapshotProtectionService(
        platformOverride: TargetPlatform.android,
      );

      await tester.pumpWidget(
        MaterialApp(
          home: BiometricLockGate(
            biometricAuthService: biometricService,
            snapshotProtectionService: snapshotService,
            isWebOverride: false,
            onSignOut: () async {},
            child: const Text('DADOS_PATRIMONIAIS_SENSIVEIS'),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(recordedCalls, contains('enableProtection'));
      expect(find.byKey(const Key('snapshot_privacy_overlay')), findsNothing);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
      await tester.pump();

      expect(find.byKey(const Key('snapshot_privacy_overlay')), findsOneWidget);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump();

      expect(find.byKey(const Key('snapshot_privacy_overlay')), findsNothing);
    });

    testWidgets('falha silenciosamente quando canal nativo não está registrado (MissingPluginException)',
        (tester) async {
      const channel = MethodChannel(SnapshotProtectionService.channelName);
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        channel,
        (call) => throw MissingPluginException('not registered'),
      );
      addTearDown(
        () => tester.binding.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null),
      );

      final snapshotService = SnapshotProtectionService(
        platformOverride: TargetPlatform.android,
      );

      await expectLater(snapshotService.enableProtection(), completes);
      await expectLater(snapshotService.disableProtection(), completes);
    });

    testWidgets('aciona disableProtection quando isAuthenticated muda para false e no dispose()', (
      tester,
    ) async {
      final recordedCalls = <String>[];
      const channel = MethodChannel(SnapshotProtectionService.channelName);
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        channel,
        (call) async {
          recordedCalls.add(call.method);
          return null;
        },
      );
      addTearDown(
        () => tester.binding.defaultBinaryMessenger
            .setMockMethodCallHandler(channel, null),
      );

      when(
        mockLocalAuth.authenticate(
          localizedReason: anyNamed('localizedReason'),
          options: anyNamed('options'),
        ),
      ).thenAnswer((_) async => true);

      final snapshotService = SnapshotProtectionService(
        platformOverride: TargetPlatform.android,
      );

      await tester.pumpWidget(
        MaterialApp(
          home: BiometricLockGate(
            biometricAuthService: biometricService,
            snapshotProtectionService: snapshotService,
            isWebOverride: false,
            isAuthenticated: true,
            onSignOut: () async {},
            child: const Text('DADOS'),
          ),
        ),
      );
      await tester.pumpAndSettle();

      recordedCalls.clear();
      await tester.pumpWidget(
        MaterialApp(
          home: BiometricLockGate(
            biometricAuthService: biometricService,
            snapshotProtectionService: snapshotService,
            isWebOverride: false,
            isAuthenticated: false,
            onSignOut: () async {},
            child: const Text('DADOS'),
          ),
        ),
      );
      expect(recordedCalls, contains('disableProtection'));

      recordedCalls.clear();
      await tester.pumpWidget(const MaterialApp(home: SizedBox()));
      expect(recordedCalls, contains('disableProtection'));
    });
  });
}
