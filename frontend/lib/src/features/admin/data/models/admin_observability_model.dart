import '../../domain/entities/admin_observability.dart';

class AdminObservabilityModel extends AdminObservability {
  const AdminObservabilityModel({
    required super.userMetrics,
    required super.uploadMetrics,
    required super.grahamLatencyMetrics,
    required super.recentUploads,
    required super.generatedAt,
  });

  factory AdminObservabilityModel.fromJson(Map<String, dynamic> json) {
    final Map<String, dynamic> root = json['data'] is Map<String, dynamic>
        ? json['data'] as Map<String, dynamic>
        : json;

    final userJson = (root['userMetrics'] as Map<String, dynamic>?) ?? const {};
    final uploadJson = (root['uploadMetrics'] as Map<String, dynamic>?) ?? const {};
    final latencyJson = (root['grahamLatencyMetrics'] as Map<String, dynamic>?) ?? const {};
    final recentJson = (root['recentUploads'] as List<dynamic>?) ?? const [];

    return AdminObservabilityModel(
      userMetrics: AdminUserMetrics(
        activeUsers24h: _parseInt(userJson['activeUsers24h']),
        totalUsers: _parseInt(userJson['totalUsers']),
        trialUsers: _parseInt(userJson['trialUsers']),
        freeUsers: _parseInt(userJson['freeUsers']),
        premiumUsers: _parseInt(userJson['premiumUsers']),
      ),
      uploadMetrics: AdminUploadMetrics(
        totalUploads: _parseInt(uploadJson['totalUploads']),
        successCount: _parseInt(uploadJson['successCount']),
        partialCount: _parseInt(uploadJson['partialCount']),
        errorCount: _parseInt(uploadJson['errorCount']),
        processingCount: _parseInt(uploadJson['processingCount']),
        errorRatePercentage: _parseDouble(uploadJson['errorRatePercentage']),
      ),
      grahamLatencyMetrics: AdminGrahamLatencyMetrics(
        averageLatencyMs: _parseDouble(latencyJson['averageLatencyMs']),
        p95LatencyMs: _parseDouble(latencyJson['p95LatencyMs']),
        slaTargetMs: _parseInt(latencyJson['slaTargetMs'], fallback: 2000),
        withinSla: latencyJson['withinSla'] as bool? ?? true,
        latencyHistory: ((latencyJson['latencyHistory'] as List<dynamic>?) ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(
              (point) => AdminLatencyPoint(
                label: point['label']?.toString() ?? '',
                latencyMs: _parseDouble(point['latencyMs']),
              ),
            )
            .toList(),
      ),
      recentUploads: recentJson
          .whereType<Map<String, dynamic>>()
          .map(
            (trace) => AnonymizedUploadTrace(
              anonymizedUserId: trace['anonymizedUserId']?.toString() ?? 'usr_***0000',
              sanitizedFileName: trace['sanitizedFileName']?.toString() ?? 'extrato_b3_***.xlsx',
              status: trace['status']?.toString() ?? 'PROCESSING',
              processedLines: _parseInt(trace['processedLines']),
              errorLines: _parseInt(trace['errorLines']),
              maskedPortfolioValue: trace['maskedPortfolioValue']?.toString() ?? '***',
              uploadTimestamp: _parseDate(trace['uploadTimestamp']),
            ),
          )
          .toList(),
      generatedAt: _parseDate(root['generatedAt']),
    );
  }

  static int _parseInt(dynamic value, {int fallback = 0}) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    if (value is String) return int.tryParse(value) ?? fallback;
    return fallback;
  }

  static double _parseDouble(dynamic value, {double fallback = 0.0}) {
    double parsed = fallback;
    if (value is num) {
      parsed = value.toDouble();
    } else if (value is String) {
      parsed = double.tryParse(value) ?? fallback;
    }
    return parsed.isFinite ? parsed : fallback;
  }

  static DateTime _parseDate(dynamic value) {
    if (value is String && value.isNotEmpty) {
      final parsed = DateTime.tryParse(value);
      if (parsed != null) return parsed.toUtc();
    }
    return DateTime.now().toUtc();
  }
}
