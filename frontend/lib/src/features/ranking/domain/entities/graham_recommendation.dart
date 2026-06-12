class GrahamRecommendation {
  final String ticker;
  final double currentPrice;
  final double intrinsicValue;
  final double marginOfSafety;
  final double currentAllocationPct;
  final double targetAllocationPct;
  final double allocationGap;
  final double recommendationScore;

  const GrahamRecommendation({
    required this.ticker,
    required this.currentPrice,
    required this.intrinsicValue,
    required this.marginOfSafety,
    required this.currentAllocationPct,
    required this.targetAllocationPct,
    required this.allocationGap,
    required this.recommendationScore,
  });
}
