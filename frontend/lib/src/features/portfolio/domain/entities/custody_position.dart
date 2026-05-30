class CustodyPosition {
  final String ticker;
  final double quantity;
  final double averagePrice;
  final double currentPrice;
  final double marketValue;
  final double gainLossPercentage;
  final String priceSource;
  final DateTime? priceUpdatedAt;

  const CustodyPosition({
    required this.ticker,
    required this.quantity,
    required this.averagePrice,
    required this.currentPrice,
    required this.marketValue,
    required this.gainLossPercentage,
    required this.priceSource,
    this.priceUpdatedAt,
  });
}
