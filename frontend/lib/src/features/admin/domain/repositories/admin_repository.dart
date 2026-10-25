import '../entities/admin_dlq_message.dart';
import '../entities/admin_observability.dart';

abstract class AdminRepository {
  Future<AdminObservability> getObservabilityMetrics();
  Future<List<AdminDlqMessage>> getDlqFailures({String status = 'PENDING'});
  Future<void> reprocessDlqMessage(int id);
  Future<Map<String, dynamic>> reprocessBatchDlqMessages(List<int> ids);
}
