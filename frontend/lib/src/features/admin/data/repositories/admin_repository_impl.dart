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
}
