import '../../domain/entities/dividend_announcement.dart';

class DividendAnnouncementModel extends DividendAnnouncement {
  const DividendAnnouncementModel({
    required super.id,
    required super.ticker,
    required super.type,
    required super.rate,
    super.netValue,
    super.approvedDate,
    required super.comDate,
    super.exDate,
    super.paymentDate,
    required super.source,
    required super.status,
    super.userShares,
    super.estimatedTotal,
    super.estimatedNetTotal,
  });

  factory DividendAnnouncementModel.fromJson(Map<String, dynamic> json) {
    return DividendAnnouncementModel(
      id: json['id']?.toString() ?? '',
      ticker: json['ticker']?.toString() ?? '',
      type: json['type']?.toString() ?? 'DIVIDENDO',
      rate: _parseDouble(json['rate']),
      netValue: json['netValue'] != null ? _parseDouble(json['netValue']) : null,
      approvedDate: _parseDate(json['approvedDate']),
      comDate: _parseDate(json['comDate']) ?? DateTime.now(),
      exDate: _parseDate(json['exDate']),
      paymentDate: _parseDate(json['paymentDate']),
      source: json['source']?.toString() ?? 'DESCONHECIDO',
      status: json['status']?.toString() ?? 'APPROVED',
      userShares: json['userShares'] != null ? _parseDouble(json['userShares']) : null,
      estimatedTotal: json['estimatedTotal'] != null ? _parseDouble(json['estimatedTotal']) : null,
      estimatedNetTotal: json['estimatedNetTotal'] != null ? _parseDouble(json['estimatedNetTotal']) : null,
    );
  }

  static double _parseDouble(dynamic value) {
    if (value == null) return 0.0;
    if (value is num) return value.toDouble();
    return double.tryParse(value.toString()) ?? 0.0;
  }

  static DateTime? _parseDate(dynamic value) {
    if (value == null) return null;
    try {
      return DateTime.parse(value.toString());
    } catch (_) {
      return null;
    }
  }
}
