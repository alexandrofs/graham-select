import '../entities/admin_dlq_message.dart';
import '../entities/admin_observability.dart';
import '../entities/admin_ticker.dart';

abstract class AdminRepository {
  Future<AdminObservability> getObservabilityMetrics();
  Future<List<AdminDlqMessage>> getDlqFailures({String status = 'PENDING'});
  Future<void> reprocessDlqMessage(int id);
  Future<Map<String, dynamic>> reprocessBatchDlqMessages(List<int> ids);

  Future<List<AdminTicker>> getTickers({String? query, String? assetClass});
  Future<AdminTicker> createTicker({
    required String ticker,
    required String name,
    required String assetClass,
    String? sector,
  });
  Future<AdminTicker> updateTicker({
    required String id,
    required String name,
    required String assetClass,
    String? sector,
  });
  Future<void> deleteTicker(String id);
  Future<void> clearMarketDataCache();
  Future<void> evictTickerCache(String ticker);
}
