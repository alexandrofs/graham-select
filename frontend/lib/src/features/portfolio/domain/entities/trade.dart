class Trade {
  final String id;
  final String userId;
  final String? profileId;
  final String ticker;
  final String side;
  final DateTime tradeDate;
  final double quantity;
  final double price;
  final String broker;
  final String? assetClass;
  final DateTime createdAt;

  Trade({
    required this.id,
    required this.userId,
    this.profileId,
    required this.ticker,
    required this.side,
    required this.tradeDate,
    required this.quantity,
    required this.price,
    required this.broker,
    this.assetClass,
    required this.createdAt,
  });
}
