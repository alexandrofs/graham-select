import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/admin/data/datasources/admin_remote_datasource.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_observability.dart';
import 'package:frontend/src/features/admin/domain/repositories/admin_repository.dart';
import 'package:frontend/src/features/admin/presentation/providers/admin_observability_provider.dart';

class _FakeAdminRepository implements AdminRepository {
  AdminObservability? result;
  Object? error;

  @override
  Future<AdminObservability> getObservabilityMetrics() async {
    if (error != null) {
      throw error!;
    }
    return result!;
  }
}

void main() {
  late _FakeAdminRepository fakeRepository;
  late AdminObservabilityProvider provider;

  final sampleMetrics = AdminObservability(
    userMetrics: const AdminUserMetrics(
      activeUsers24h: 12,
      totalUsers: 50,
      trialUsers: 10,
      freeUsers: 25,
      premiumUsers: 15,
    ),
    uploadMetrics: const AdminUploadMetrics(
      totalUploads: 20,
      successCount: 18,
      partialCount: 1,
      errorCount: 1,
      processingCount: 0,
      errorRatePercentage: 5.0,
    ),
    grahamLatencyMetrics: const AdminGrahamLatencyMetrics(
      averageLatencyMs: 145.0,
      p95LatencyMs: 310.0,
      slaTargetMs: 2000,
      withinSla: true,
      latencyHistory: [
        AdminLatencyPoint(label: 'Agora', latencyMs: 145.0),
      ],
    ),
    recentUploads: [
      AnonymizedUploadTrace(
        anonymizedUserId: 'usr_***a1b2',
        sanitizedFileName: 'extrato_b3_***.xlsx',
        status: 'SUCCESS',
        processedLines: 20,
        errorLines: 0,
        maskedPortfolioValue: '***',
        uploadTimestamp: DateTime.utc(2026, 6, 13, 12, 0),
      ),
    ],
    generatedAt: DateTime.utc(2026, 6, 13, 12, 5),
  );

  setUp(() {
    fakeRepository = _FakeAdminRepository();
    provider = AdminObservabilityProvider(repository: fakeRepository);
  });

  test('transitions from initial -> loading -> loaded on success', () async {
    fakeRepository.result = sampleMetrics;
    expect(provider.status, AdminObservabilityStatus.initial);

    await provider.loadMetrics();

    expect(provider.status, AdminObservabilityStatus.loaded);
    expect(provider.metrics?.userMetrics.activeUsers24h, 12);
    expect(provider.errorMessage, isNull);
  });

  test('transitions to accessDenied on AdminAccessDeniedException (403)', () async {
    fakeRepository.error = const AdminAccessDeniedException(
      'Access restricted to system administrators.',
    );

    await provider.loadMetrics();

    expect(provider.status, AdminObservabilityStatus.accessDenied);
    expect(
      provider.errorMessage,
      contains('Access restricted to system administrators'),
    );
  });

  test('transitions to error on generic failure', () async {
    fakeRepository.error = Exception('Falha de rede');

    await provider.loadMetrics();

    expect(provider.status, AdminObservabilityStatus.error);
    expect(provider.errorMessage, contains('Falha de rede'));
  });
}
