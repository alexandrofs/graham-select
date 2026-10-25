import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/admin/data/models/admin_observability_model.dart';

void main() {
  group('AdminObservabilityModel', () {
    test('should parse JSON envelope with data field accurately', () {
      final json = {
        'data': {
          'userMetrics': {
            'activeUsers24h': 18,
            'totalUsers': 120,
            'trialUsers': 40,
            'freeUsers': 60,
            'premiumUsers': 20,
          },
          'uploadMetrics': {
            'totalUploads': 50,
            'successCount': 42,
            'partialCount': 5,
            'errorCount': 2,
            'processingCount': 1,
            'errorRatePercentage': 4.00,
          },
          'grahamLatencyMetrics': {
            'averageLatencyMs': 155.50,
            'p95LatencyMs': 340.00,
            'slaTargetMs': 2000,
            'withinSla': true,
            'latencyHistory': [
              {'label': '-5m', 'latencyMs': 150.0},
              {'label': 'Agora', 'latencyMs': 155.5},
            ],
          },
          'recentUploads': [
            {
              'anonymizedUserId': 'usr_***9f8a',
              'sanitizedFileName': 'extrato_b3_***.xlsx',
              'status': 'SUCCESS',
              'processedLines': 35,
              'errorLines': 0,
              'maskedPortfolioValue': '***',
              'uploadTimestamp': '2026-06-13T12:00:00Z',
            },
          ],
          'generatedAt': '2026-06-13T12:05:00Z',
        },
      };

      final model = AdminObservabilityModel.fromJson(json);

      expect(model.userMetrics.activeUsers24h, 18);
      expect(model.userMetrics.totalUsers, 120);
      expect(model.userMetrics.premiumUsers, 20);
      expect(model.uploadMetrics.totalUploads, 50);
      expect(model.uploadMetrics.successCount, 42);
      expect(model.uploadMetrics.errorRatePercentage, 4.0);
      expect(model.grahamLatencyMetrics.averageLatencyMs, 155.5);
      expect(model.grahamLatencyMetrics.slaTargetMs, 2000);
      expect(model.grahamLatencyMetrics.withinSla, isTrue);
      expect(model.grahamLatencyMetrics.latencyHistory.length, 2);
      expect(model.recentUploads.length, 1);
      expect(model.recentUploads.first.anonymizedUserId, 'usr_***9f8a');
      expect(model.recentUploads.first.sanitizedFileName, 'extrato_b3_***.xlsx');
      expect(model.recentUploads.first.maskedPortfolioValue, '***');
    });
  });
}
