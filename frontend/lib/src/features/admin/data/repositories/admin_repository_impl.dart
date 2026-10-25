import '../../domain/entities/admin_dlq_message.dart';
import '../../domain/entities/admin_observability.dart';
import '../../domain/repositories/admin_repository.dart';
import '../datasources/admin_remote_datasource.dart';

class AdminRepositoryImpl implements AdminRepository {
  final AdminRemoteDatasource remoteDatasource;

  AdminRepositoryImpl({required this.remoteDatasource});

  @override
  Future<AdminObservability> getObservabilityMetrics() {
    return remoteDatasource.fetchObservabilityMetrics();
  }

  @override
  Future<List<AdminDlqMessage>> getDlqFailures({String status = 'PENDING'}) {
    return remoteDatasource.getDlqFailures(status: status);
  }

  @override
  Future<void> reprocessDlqMessage(int id) {
    return remoteDatasource.reprocessDlqMessage(id);
  }

  @override
  Future<Map<String, dynamic>> reprocessBatchDlqMessages(List<int> ids) {
    return remoteDatasource.reprocessBatchDlqMessages(ids);
  }
}
