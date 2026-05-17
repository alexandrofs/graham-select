import '../../domain/entities/trade.dart';

class TradeModel extends Trade {
  TradeModel({
    required super.id,
    required super.userId,
    required super.ticker,
    required super.side,
    required super.tradeDate,
    required super.quantity,
    required super.price,
    required super.broker,
    required super.createdAt,
  });

  factory TradeModel.fromJson(Map<String, dynamic> json) {
    return TradeModel(
      id: json['id'],
      userId: json['userId'],
      ticker: json['ticker'],
      side: json['side'],
      tradeDate: DateTime.parse(json['tradeDate']),
      quantity: (json['quantity'] as num).toDouble(),
      price: (json['price'] as num).toDouble(),
      broker: json['broker'],
      createdAt: DateTime.parse(json['createdAt']),
    );
  }
}
