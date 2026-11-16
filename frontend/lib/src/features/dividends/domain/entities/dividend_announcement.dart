class DividendAnnouncement {
  final String id;
  final String ticker;
  final String type;
  final double rate;
  final double? netValue;
  final DateTime? approvedDate;
  final DateTime comDate;
  final DateTime? exDate;
  final DateTime? paymentDate;
  final String source;
  final String status;
  final double? userShares;
  final double? estimatedTotal;
  final double? estimatedNetTotal;

  const DividendAnnouncement({
    required this.id,
    required this.ticker,
    required this.type,
    required this.rate,
    this.netValue,
    this.approvedDate,
    required this.comDate,
    this.exDate,
    this.paymentDate,
    required this.source,
    required this.status,
    this.userShares,
    this.estimatedTotal,
    this.estimatedNetTotal,
  });
}
