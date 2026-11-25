import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/founders/presentation/widgets/commercial_plan_badge.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';

void main() {
  testWidgets('Exibe selo Ouro Founder para FOUNDER_LIFETIME', (tester) async {
    final profile = UserProfile(
      id: 1,
      email: 'founder@kerno.afsdigital.com.br',
      fullName: 'Founder',
      tier: 'PREMIUM',
      commercialPlan: 'FOUNDER_LIFETIME',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: CommercialPlanBadge(profile: profile),
        ),
      ),
    );

    expect(find.text('Founder'), findsOneWidget);
    expect(find.byIcon(Icons.workspace_premium), findsOneWidget);

    final container = tester.widget<Container>(
      find.byKey(const Key('commercial_plan_badge')),
    );
    final decoration = container.decoration! as BoxDecoration;
    expect(decoration.color, const Color(0xFFFEF3C7));
    expect((decoration.border! as Border).top.color, const Color(0xFFF59E0B));

    final text = tester.widget<Text>(find.text('Founder'));
    expect(text.overflow, isNull);
    expect(text.style?.color, const Color(0xFF92400E));
  });

  testWidgets('Exibe pílula Trial Pro com dias restantes para PRO_TRIAL', (
    tester,
  ) async {
    final profile = UserProfile(
      id: 2,
      email: 'trial@kerno.afsdigital.com.br',
      fullName: 'Trial',
      tier: 'TRIAL',
      commercialPlan: 'PRO_TRIAL',
      daysRemaining: 90,
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: CommercialPlanBadge(profile: profile),
        ),
      ),
    );

    expect(find.text('Trial Pro: 90 dias restantes'), findsOneWidget);
    expect(find.byIcon(Icons.timer_outlined), findsOneWidget);
  });

  testWidgets('Exibe pílula Gratuito para FREE sem overflow em TextScaler 2.0', (
    tester,
  ) async {
    final profile = UserProfile(
      id: 3,
      email: 'free@kerno.afsdigital.com.br',
      fullName: 'Free',
      tier: 'FREE',
      commercialPlan: 'FREE',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(textScaler: TextScaler.linear(2.0)),
          child: Scaffold(
            body: CommercialPlanBadge(profile: profile),
          ),
        ),
      ),
    );

    expect(tester.takeException(), isNull);
    expect(find.text('Gratuito'), findsOneWidget);
    expect(find.byIcon(Icons.person_outline), findsOneWidget);
  });

  testWidgets('Exibe Gratuito quando PRO_TRIAL expirou (tier == FREE)', (
    tester,
  ) async {
    final profile = UserProfile(
      id: 4,
      email: 'expired@kerno.afsdigital.com.br',
      fullName: 'Expired',
      tier: 'FREE',
      commercialPlan: 'PRO_TRIAL',
      daysRemaining: 0,
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: CommercialPlanBadge(profile: profile)),
      ),
    );

    expect(find.text('Gratuito'), findsOneWidget);
  });

  testWidgets('Usa singular para 1 dia restante e faz clamp >= 0 para daysRemaining negativo', (
    tester,
  ) async {
    final oneDayProfile = UserProfile(
      id: 5,
      email: 'oneday@kerno.afsdigital.com.br',
      fullName: 'One Day',
      tier: 'TRIAL',
      commercialPlan: 'PRO_TRIAL',
      daysRemaining: 1,
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: CommercialPlanBadge(profile: oneDayProfile)),
      ),
    );
    expect(find.text('Trial Pro: 1 dia restante'), findsOneWidget);

    final negativeProfile = UserProfile(
      id: 6,
      email: 'neg@kerno.afsdigital.com.br',
      fullName: 'Negative',
      tier: 'TRIAL',
      commercialPlan: 'PRO_TRIAL',
      daysRemaining: -5,
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: CommercialPlanBadge(profile: negativeProfile)),
      ),
    );
    expect(find.text('Trial Pro: 0 dias restantes'), findsOneWidget);
  });

  testWidgets('Faz fallback para trialExpiresAt quando daysRemaining é nulo e para tier quando commercialPlan é vazio', (
    tester,
  ) async {
    final expiryFallback = UserProfile(
      id: 7,
      email: 'fallback@kerno.afsdigital.com.br',
      fullName: 'Fallback',
      tier: 'TRIAL',
      commercialPlan: '',
      trialExpiresAt: DateTime.now().toUtc().add(const Duration(days: 10, hours: 1)),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: CommercialPlanBadge(profile: expiryFallback)),
      ),
    );
    expect(find.text('Trial Pro: 10 dias restantes'), findsOneWidget);
  });
}
