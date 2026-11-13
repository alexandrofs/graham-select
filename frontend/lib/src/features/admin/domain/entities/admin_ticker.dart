class AdminTicker {
  final String id;
  final String ticker;
  final String name;
  final String assetClass;
  final String? sector;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const AdminTicker({
    required this.id,
    required this.ticker,
    required this.name,
    required this.assetClass,
    this.sector,
    this.createdAt,
    this.updatedAt,
  });
}
