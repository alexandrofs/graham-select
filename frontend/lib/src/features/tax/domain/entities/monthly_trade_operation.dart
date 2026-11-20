class MonthlyTradeOperation {
  final String ticker;
  final DateTime tradeDate;
  final String side;
  final double quantity;
  final double price;
  final double averagePrice;
  final double totalSaleValue;
  final double netProfitOrLoss;
  final String category;

  const MonthlyTradeOperation({
    required this.ticker,
    required this.tradeDate,
    required this.side,
    required this.quantity,
    required this.price,
    required this.averagePrice,
    required this.totalSaleValue,
    required this.netProfitOrLoss,
    required this.category,
  });

  factory MonthlyTradeOperation.fromJson(Map<String, dynamic> json) {
    return MonthlyTradeOperation(
      ticker: json['ticker'] as String? ?? '',
      tradeDate: DateTime.tryParse(json['tradeDate']?.toString() ?? '') ?? DateTime.now(),
      side: json['side'] as String? ?? 'VENDA',
      quantity: double.tryParse(json['quantity']?.toString() ?? '0') ?? 0.0,
      price: double.tryParse(json['price']?.toString() ?? '0') ?? 0.0,
      averagePrice: double.tryParse(json['averagePrice']?.toString() ?? '0') ?? 0.0,
      totalSaleValue: double.tryParse(json['totalSaleValue']?.toString() ?? '0') ?? 0.0,
      netProfitOrLoss: double.tryParse(json['netProfitOrLoss']?.toString() ?? '0') ?? 0.0,
      category: json['category'] as String? ?? 'SWING_TRADE_EQUITY',
    );
  }
}
