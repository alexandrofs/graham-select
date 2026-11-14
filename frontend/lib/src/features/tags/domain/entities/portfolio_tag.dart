class PortfolioTag {
  final String id;
  final String name;
  final String color;
  final List<String> tickers;
  final int assetCount;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const PortfolioTag({
    required this.id,
    required this.name,
    required this.color,
    this.tickers = const [],
    this.assetCount = 0,
    this.createdAt,
    this.updatedAt,
  });

  PortfolioTag copyWith({
    String? id,
    String? name,
    String? color,
    List<String>? tickers,
    int? assetCount,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    final nextTickers = tickers ?? this.tickers;
    return PortfolioTag(
      id: id ?? this.id,
      name: name ?? this.name,
      color: color ?? this.color,
      tickers: nextTickers,
      assetCount: assetCount ?? (tickers != null ? nextTickers.length : this.assetCount),
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
