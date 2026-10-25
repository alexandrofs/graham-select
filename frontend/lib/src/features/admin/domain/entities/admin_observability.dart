class AdminObservability {
  final AdminUserMetrics userMetrics;
  final AdminUploadMetrics uploadMetrics;
  final AdminGrahamLatencyMetrics grahamLatencyMetrics;
  final List<AnonymizedUploadTrace> recentUploads;
  final DateTime generatedAt;

  const AdminObservability({
    required this.userMetrics,
    required this.uploadMetrics,
    required this.grahamLatencyMetrics,
    required this.recentUploads,
    required this.generatedAt,
  });
}

class AdminUserMetrics {
  final int activeUsers24h;
  final int totalUsers;
  final int trialUsers;
  final int freeUsers;
  final int premiumUsers;

  const AdminUserMetrics({
    required this.activeUsers24h,
    required this.totalUsers,
    required this.trialUsers,
    required this.freeUsers,
    required this.premiumUsers,
  });
}

class AdminUploadMetrics {
  final int totalUploads;
  final int successCount;
  final int partialCount;
  final int errorCount;
  final int processingCount;
  final double errorRatePercentage;

  const AdminUploadMetrics({
    required this.totalUploads,
    required this.successCount,
    required this.partialCount,
    required this.errorCount,
    required this.processingCount,
    required this.errorRatePercentage,
  });
}

class AdminGrahamLatencyMetrics {
  final double averageLatencyMs;
  final double p95LatencyMs;
  final int slaTargetMs;
  final bool withinSla;
  final List<AdminLatencyPoint> latencyHistory;

  const AdminGrahamLatencyMetrics({
    required this.averageLatencyMs,
    required this.p95LatencyMs,
    required this.slaTargetMs,
    required this.withinSla,
    required this.latencyHistory,
  });
}

class AdminLatencyPoint {
  final String label;
  final double latencyMs;

  const AdminLatencyPoint({
    required this.label,
    required this.latencyMs,
  });
}

class AnonymizedUploadTrace {
  final String anonymizedUserId;
  final String sanitizedFileName;
  final String status;
  final int processedLines;
  final int errorLines;
  final String maskedPortfolioValue;
  final DateTime uploadTimestamp;

  const AnonymizedUploadTrace({
    required this.anonymizedUserId,
    required this.sanitizedFileName,
    required this.status,
    required this.processedLines,
    required this.errorLines,
    required this.maskedPortfolioValue,
    required this.uploadTimestamp,
  });
}
