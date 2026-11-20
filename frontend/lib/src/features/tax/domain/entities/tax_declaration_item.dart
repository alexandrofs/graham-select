class TaxDeclarationItem {
  final String ticker;
  final String companyName;
  final String cnpj;
  final String groupCode;
  final String itemCode;
  final String groupDescription;
  final String locationCode;
  final double quantity;
  final double averagePrice;
  final double previousYearValue;
  final double currentYearValue;
  final String discrimination;
  final bool liquidated;

  const TaxDeclarationItem({
    required this.ticker,
    required this.companyName,
    required this.cnpj,
    required this.groupCode,
    required this.itemCode,
    required this.groupDescription,
    required this.locationCode,
    required this.quantity,
    required this.averagePrice,
    required this.previousYearValue,
    required this.currentYearValue,
    required this.discrimination,
    required this.liquidated,
  });

  factory TaxDeclarationItem.fromJson(Map<String, dynamic> json) {
    return TaxDeclarationItem(
      ticker: json['ticker'] as String? ?? '',
      companyName: json['companyName'] as String? ?? '',
      cnpj: json['cnpj'] as String? ?? 'CNPJ não informado',
      groupCode: json['groupCode'] as String? ?? '',
      itemCode: json['itemCode'] as String? ?? '',
      groupDescription: json['groupDescription'] as String? ?? '',
      locationCode: json['locationCode'] as String? ?? '105',
      quantity: double.tryParse(json['quantity']?.toString() ?? '0') ?? 0.0,
      averagePrice: double.tryParse(json['averagePrice']?.toString() ?? '0') ?? 0.0,
      previousYearValue: double.tryParse(json['previousYearValue']?.toString() ?? '0') ?? 0.0,
      currentYearValue: double.tryParse(json['currentYearValue']?.toString() ?? '0') ?? 0.0,
      discrimination: json['discrimination'] as String? ?? '',
      liquidated: json['liquidated'] as bool? ?? false,
    );
  }
}
