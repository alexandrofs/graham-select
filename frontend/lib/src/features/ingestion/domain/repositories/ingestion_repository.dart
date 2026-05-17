import '../entities/ingestion_audit.dart';

abstract class IngestionRepository {
  Future<List<IngestionAudit>> getHistory();
}
