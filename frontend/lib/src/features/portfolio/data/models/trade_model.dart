import '../../domain/entities/trade.dart';

class TradeModel extends Trade {
  TradeModel({
    required super.id,
    required super.userId,
    super.profileId,
    required super.ticker,
    required super.side,
    required super.tradeDate,
    required super.quantity,
    required super.price,
    required super.broker,
    super.assetClass,
    required super.createdAt,
  });

  factory TradeModel.fromJson(Map<String, dynamic> json) {
    return TradeModel(
      id: json['id'] as String,
      userId: json['userId'] as String,
      profileId: json['profileId'] as String?,
      ticker: json['ticker'] as String,
      side: json['side'] as String,
      tradeDate: DateTime.parse(json['tradeDate'] as String),
      quantity: (json['quantity'] as num).toDouble(),
      price: (json['price'] as num).toDouble(),
      broker: json['broker'] as String,
      assetClass: json['assetClass'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
