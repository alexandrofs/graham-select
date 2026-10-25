import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/widgets/main_layout.dart';
import 'package:frontend/src/features/admin/data/datasources/admin_remote_datasource.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_dlq_message.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_observability.dart';
import 'package:frontend/src/features/admin/domain/repositories/admin_repository.dart';
import 'package:frontend/src/features/admin/presentation/pages/admin_dashboard_page.dart';
import 'package:frontend/src/features/admin/presentation/providers/admin_observability_provider.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

class _StubProfileProvider extends ChangeNotifier implements ProfileProvider {
  @override
  final UserProfile? profile;

  _StubProfileProvider(this.profile);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _ControllableAdminRepository implements AdminRepository {
  Completer<AdminObservability>? completer;
  AdminObservability? immediateResult;
  Object? immediateError;

  @override
  Future<AdminObservability> getObservabilityMetrics() {
    if (completer != null) {
      return completer!.future;
    }
    if (immediateError != null) {
      return Future.error(immediateError!);
    }
    return Future.value(immediateResult!);
  }

  @override
  Future<List<AdminDlqMessage>> getDlqFailures({String status = 'PENDING'}) async => [];

  @override
  Future<void> reprocessDlqMessage(int id) async {}

  @override
  Future<Map<String, dynamic>> reprocessBatchDlqMessages(List<int> ids) async => {};
}

void main() {
  final sampleMetrics = AdminObservability(
    userMetrics: const AdminUserMetrics(
      activeUsers24h: 24,
      totalUsers: 150,
      trialUsers: 30,
      freeUsers: 80,
      premiumUsers: 40,
    ),
    uploadMetrics: const AdminUploadMetrics(
      totalUploads: 40,
      successCount: 35,
      partialCount: 3,
      errorCount: 2,
      processingCount: 0,
      errorRatePercentage: 5.0,
    ),
    grahamLatencyMetrics: const AdminGrahamLatencyMetrics(
      averageLatencyMs: 145.0,
      p95LatencyMs: 320.0,
      slaTargetMs: 2000,
      withinSla: true,
      latencyHistory: [
        AdminLatencyPoint(label: '-10m', latencyMs: 150.0),
        AdminLatencyPoint(label: '-5m', latencyMs: 140.0),
        AdminLatencyPoint(label: 'Agora', latencyMs: 145.0),
      ],
    ),
    recentUploads: [
      AnonymizedUploadTrace(
        anonymizedUserId: 'usr_***9f8a',
        sanitizedFileName: 'extrato_b3_***.xlsx',
        status: 'SUCCESS',
        processedLines: 42,
        errorLines: 0,
        maskedPortfolioValue: '***',
        uploadTimestamp: DateTime.utc(2026, 6, 13, 14, 0),
      ),
    ],
    generatedAt: DateTime.utc(2026, 6, 13, 14, 5),
  );

  Widget buildTestWidget(AdminObservabilityProvider provider) {
    return ChangeNotifierProvider<AdminObservabilityProvider>.value(
      value: provider,
      child: const MaterialApp(
        home: Scaffold(
          body: AdminDashboardPage(),
        ),
      ),
    );
  }

  testWidgets('renders Skeleton Screen while loading', (tester) async {
    final repo = _ControllableAdminRepository()
      ..completer = Completer<AdminObservability>();
    final provider = AdminObservabilityProvider(repository: repo);

    await tester.pumpWidget(buildTestWidget(provider));
    await tester.pump();

    expect(find.byKey(const Key('admin_dashboard_skeleton')), findsOneWidget);
  });

  testWidgets(
      'renders all 3 required charts/metrics and anonymized upload table when loaded',
      (tester) async {
    final repo = _ControllableAdminRepository()
      ..immediateResult = sampleMetrics;
    final provider = AdminObservabilityProvider(repository: repo);

    await tester.pumpWidget(buildTestWidget(provider));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('admin_active_users_card')), findsOneWidget);
    expect(find.text('Usuários Ativos (24h)'), findsOneWidget);
    expect(find.text('24'), findsOneWidget);

    expect(find.byKey(const Key('admin_upload_metrics_card')), findsOneWidget);
    expect(find.text('Sucesso vs Erro de Upload (Kafka)'), findsOneWidget);
    expect(find.text('5.00% falha'), findsOneWidget);

    expect(find.byKey(const Key('admin_graham_latency_card')), findsOneWidget);
    expect(find.text('Latência Média do Motor Graham'), findsOneWidget);
    expect(find.text('145 ms'), findsOneWidget);

    expect(
      find.byKey(const Key('admin_anonymized_uploads_card')),
      findsOneWidget,
    );
    expect(find.text('Dados Anonimizados (LGPD / NFR5)'), findsOneWidget);
    expect(find.text('usr_***9f8a'), findsOneWidget);
    expect(find.text('extrato_b3_***.xlsx'), findsOneWidget);
    expect(find.text('Patrimônio: ***'), findsOneWidget);
  });

  testWidgets('renders Access Restricted state when status is accessDenied',
      (tester) async {
    final repo = _ControllableAdminRepository()
      ..immediateError = const AdminAccessDeniedException(
        'Acesso exclusivo para administradores.',
      );
    final provider = AdminObservabilityProvider(repository: repo);

    await tester.pumpWidget(buildTestWidget(provider));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('admin_access_denied_state')), findsOneWidget);
    expect(find.text('Acesso Restrito a Administradores'), findsOneWidget);
    expect(
      find.text('Acesso exclusivo para administradores.'),
      findsOneWidget,
    );
  });

  testWidgets(
      'MainLayout shows Painel Admin button only when ProfileProvider role is ADMIN',
      (tester) async {
    final adminProfile = _StubProfileProvider(
      UserProfile(
        id: 1,
        email: 'admin@graham.com',
        fullName: 'Admin',
        tier: 'PREMIUM',
        role: 'ADMIN',
      ),
    );
    final routerAdmin = GoRouter(
      initialLocation: '/dashboard',
      routes: [
        ShellRoute(
          builder: (context, state, child) => MainLayout(child: child),
          routes: [
            GoRoute(
              path: '/dashboard',
              builder: (_, _) => const SizedBox(),
            ),
          ],
        ),
      ],
    );

    await tester.pumpWidget(
      ChangeNotifierProvider<ProfileProvider>.value(
        value: adminProfile,
        child: MaterialApp.router(routerConfig: routerAdmin),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('admin_dashboard_nav_button')), findsOneWidget);

    final userProfile = _StubProfileProvider(
      UserProfile(
        id: 2,
        email: 'user@graham.com',
        fullName: 'User',
        tier: 'FREE',
        role: 'USER',
      ),
    );
    await tester.pumpWidget(
      ChangeNotifierProvider<ProfileProvider>.value(
        value: userProfile,
        child: MaterialApp.router(routerConfig: routerAdmin),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('admin_dashboard_nav_button')), findsNothing);
  });
}
