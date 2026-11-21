import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/notifications/domain/models/web_push_notification_models.dart';
import 'package:frontend/src/features/notifications/presentation/pages/web_push_notifications_page.dart';
import 'package:frontend/src/features/notifications/presentation/providers/web_push_notifications_provider.dart';
import 'package:provider/provider.dart';

import 'web_push_notifications_provider_test.dart';

void main() {
  testWidgets('WebPushNotificationsPage renders status, toggles, saves and triggers test',
      (WidgetTester tester) async {
    final repo = FakeWebPushNotificationsRepository();
    repo.storedPrefs = const WebPushPreferencesModel(
      optedIn: true,
      minMarginOfSafetyPct: 25.0,
      onlyCustodyTickers: true,
    );
    repo.storedSubscriptions = [
      WebPushSubscriptionModel(
        id: 'sub-test-1',
        endpointPreview: 'https://fcm.googleapis.com/.../****',
        userAgent: 'Test Agent',
        active: true,
        createdAt: DateTime.now().toUtc(),
      ),
    ];

    final client = FakeWebPushBrowserClient();
    final provider = WebPushNotificationsProvider(
      repository: repo,
      browserClient: client,
    );

    await tester.pumpWidget(
      MaterialApp(
        home: ChangeNotifierProvider<WebPushNotificationsProvider>.value(
          value: provider,
          child: const WebPushNotificationsPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Alertas Web Push (Graham)'), findsOneWidget);
    expect(find.byKey(const Key('btn_activate_web_push')), findsOneWidget);
    expect(find.byKey(const Key('switch_web_push_opt_in')), findsOneWidget);
    expect(find.byKey(const Key('slider_min_margin_of_safety')), findsOneWidget);
    expect(find.byKey(const Key('switch_only_custody_tickers')), findsOneWidget);
    expect(find.byKey(const Key('btn_send_web_push_test')), findsOneWidget);

    // Test send test alert
    await tester.ensureVisible(find.byKey(const Key('btn_send_web_push_test')));
    await tester.tap(find.byKey(const Key('btn_send_web_push_test')));
    await tester.pumpAndSettle();

    expect(find.text('Alerta de teste enviado! (Enviados: 1, Falhas: 0)'),
        findsOneWidget);

    // Toggle custody switch and save
    await tester.ensureVisible(find.byKey(const Key('switch_only_custody_tickers')));
    await tester.tap(find.byKey(const Key('switch_only_custody_tickers')));
    await tester.pump();

    final ElevatedButton saveBtn =
        tester.widget(find.byKey(const Key('btn_save_web_push_preferences')));
    expect(saveBtn.onPressed, isNotNull);

    await tester.ensureVisible(find.byKey(const Key('btn_save_web_push_preferences')));
    await tester.tap(find.byKey(const Key('btn_save_web_push_preferences')));
    await tester.pumpAndSettle();

    expect(find.text('Preferências de Web Push salvas com sucesso!'),
        findsOneWidget);
  });
}
