class OpenFinanceSyncResult {
  final String connectionId;
  final String status;
  final int positionsFetched;
  final int transactionsFetched;
  final int tradesCreated;
  final int tradesDuplicated;
  final String message;
  final DateTime? syncedAt;

  const OpenFinanceSyncResult({
    required this.connectionId,
    required this.status,
    required this.positionsFetched,
    required this.transactionsFetched,
    required this.tradesCreated,
    required this.tradesDuplicated,
    required this.message,
    this.syncedAt,
  });

  factory OpenFinanceSyncResult.fromJson(Map<String, dynamic> json) {
    return OpenFinanceSyncResult(
      connectionId: json['connectionId'] as String? ?? '',
      status: json['status'] as String? ?? '',
      positionsFetched: json['positionsFetched'] as int? ?? 0,
      transactionsFetched: json['transactionsFetched'] as int? ?? 0,
      tradesCreated: json['tradesCreated'] as int? ?? 0,
      tradesDuplicated: json['tradesDuplicated'] as int? ?? 0,
      message: json['message'] as String? ?? '',
      syncedAt: json['syncedAt'] != null
          ? DateTime.tryParse(json['syncedAt'] as String)
          : null,
    );
  }
}
