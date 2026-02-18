class RankedCompany {
  final String symbol;
  final String name;
  final double intrinsicValue;
  final double currentPrice;
  final double marginOfSafety;

  const RankedCompany({
    required this.symbol,
    required this.name,
    required this.intrinsicValue,
    required this.currentPrice,
    required this.marginOfSafety,
  });
}
