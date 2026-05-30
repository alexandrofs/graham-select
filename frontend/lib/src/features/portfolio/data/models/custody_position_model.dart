import '../../domain/entities/custody_position.dart';

class CustodyPositionModel extends CustodyPosition {
  const CustodyPositionModel({
    required super.ticker,
    required super.quantity,
    required super.averagePrice,
    required super.currentPrice,
    required super.marketValue,
    required super.gainLossPercentage,
    required super.priceSource,
    super.priceUpdatedAt,
  });

  factory CustodyPositionModel.fromJson(Map<String, dynamic> json) {
    return CustodyPositionModel(
      ticker: json['ticker'] as String,
      quantity: (json['quantity'] as num).toDouble(),
      averagePrice: (json['averagePrice'] as num).toDouble(),
      currentPrice: (json['currentPrice'] as num).toDouble(),
      marketValue: (json['marketValue'] as num).toDouble(),
      gainLossPercentage: (json['gainLossPercentage'] as num).toDouble(),
      priceSource: json['priceSource'] as String,
      priceUpdatedAt: json['priceUpdatedAt'] != null
          ? DateTime.parse(json['priceUpdatedAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'ticker': ticker,
      'quantity': quantity,
      'averagePrice': averagePrice,
      'currentPrice': currentPrice,
      'marketValue': marketValue,
      'gainLossPercentage': gainLossPercentage,
      'priceSource': priceSource,
      'priceUpdatedAt': priceUpdatedAt?.toIso8601String(),
    };
  }
}
