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
    required super.assetClass,
  });

  factory CustodyPositionModel.fromJson(Map<String, dynamic> json) {
    // Backend serializa BigDecimal como String via @JsonSerialize(ToStringSerializer)
    // Por isso usamos double.parse(toString()) para aceitar tanto String quanto num
    return CustodyPositionModel(
      ticker: json['ticker'] as String,
      quantity: double.parse(json['quantity'].toString()),
      averagePrice: double.parse(json['averagePrice'].toString()),
      currentPrice: double.parse(json['currentPrice'].toString()),
      marketValue: double.parse(json['marketValue'].toString()),
      gainLossPercentage: double.parse(json['gainLossPercentage'].toString()),
      priceSource: json['priceSource'] as String,
      priceUpdatedAt: json['priceUpdatedAt'] != null
          ? DateTime.parse(json['priceUpdatedAt'] as String)
          : null,
      assetClass: json['assetClass'] as String? ?? 'Outros',
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
      'assetClass': assetClass,
    };
  }
}
