import '../../domain/entities/admin_dlq_message.dart';
import '../../domain/entities/admin_observability.dart';
import '../../domain/entities/admin_ticker.dart';
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

  @override
  Future<List<AdminTicker>> getTickers({String? query, String? assetClass}) async {
    final models = await remoteDatasource.getTickers(query: query, assetClass: assetClass);
    return models;
  }

  @override
  Future<AdminTicker> createTicker({
    required String ticker,
    required String name,
    required String assetClass,
    String? sector,
  }) {
    return remoteDatasource.createTicker(
      ticker: ticker,
      name: name,
      assetClass: assetClass,
      sector: sector,
    );
  }

  @override
  Future<AdminTicker> updateTicker({
    required String id,
    required String name,
    required String assetClass,
    String? sector,
  }) {
    return remoteDatasource.updateTicker(
      id: id,
      name: name,
      assetClass: assetClass,
      sector: sector,
    );
  }

  @override
  Future<void> deleteTicker(String id) {
    return remoteDatasource.deleteTicker(id);
  }

  @override
  Future<void> clearMarketDataCache() {
    return remoteDatasource.clearMarketDataCache();
  }

  @override
  Future<void> evictTickerCache(String ticker) {
    return remoteDatasource.evictTickerCache(ticker);
  }
}
