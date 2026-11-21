import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/features/notifications/domain/models/whatsapp_notification_models.dart';
import 'package:frontend/src/features/notifications/presentation/pages/whatsapp_notifications_page.dart';
import 'package:frontend/src/features/notifications/presentation/providers/whatsapp_notifications_provider.dart';

import 'whatsapp_notifications_provider_test.dart';

void main() {
  testWidgets('WhatsappNotificationsPage renders form, saves and sends test',
      (WidgetTester tester) async {
    final repo = FakeWhatsappNotificationsRepository();
    repo.storedPrefs = const WhatsappPreferencesModel(
      phoneNumber: '+5511999998888',
      optedIn: true,
      notifyDividends: true,
      notifyRebalanceTriggers: true,
      notifyGoalMilestones: true,
      rebalanceDriftThresholdPct: 5.0,
    );
    final provider = WhatsappNotificationsProvider(repository: repo);

    await tester.pumpWidget(
      MaterialApp(
        home: ChangeNotifierProvider<WhatsappNotificationsProvider>.value(
          value: provider,
          child: const WhatsappNotificationsPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Alertas via WhatsApp'), findsOneWidget);
    expect(find.byKey(const Key('whatsapp_phone_field')), findsOneWidget);
    expect(find.byKey(const Key('whatsapp_opt_in_switch')), findsOneWidget);
    expect(find.text('Salvar Preferências'), findsOneWidget);
    expect(find.text('Enviar Teste'), findsOneWidget);

    await tester.ensureVisible(find.byKey(const Key('whatsapp_test_button')));
    await tester.tap(find.byKey(const Key('whatsapp_test_button')));
    await tester.pumpAndSettle();

    expect(find.text('Teste enviado via MOCK!'), findsOneWidget);
    expect(find.text('TEST'), findsOneWidget);

    await tester.enterText(
        find.byKey(const Key('whatsapp_phone_field')), '+5511988887777');
    await tester.pump();
    final ElevatedButton saveBtn =
        tester.widget(find.byKey(const Key('whatsapp_save_button')));
    expect(saveBtn.onPressed, isNotNull);
    final OutlinedButton testBtnDisabled =
        tester.widget(find.byKey(const Key('whatsapp_test_button')));
    expect(testBtnDisabled.onPressed, isNull);

    await tester.ensureVisible(find.byKey(const Key('whatsapp_save_button')));
    await tester.tap(find.byKey(const Key('whatsapp_save_button')));
    await tester.pumpAndSettle();

    expect(find.text('Preferências de alertas WhatsApp salvas com sucesso!'),
        findsOneWidget);
    final OutlinedButton testBtnReenabled =
        tester.widget(find.byKey(const Key('whatsapp_test_button')));
    expect(testBtnReenabled.onPressed, isNotNull);
  });
}
