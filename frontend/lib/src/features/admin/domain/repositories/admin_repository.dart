import '../entities/admin_observability.dart';

abstract class AdminRepository {
  Future<AdminObservability> getObservabilityMetrics();
}
