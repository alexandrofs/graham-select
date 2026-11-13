import '../../domain/entities/admin_ticker.dart';

class AdminTickerModel extends AdminTicker {
  const AdminTickerModel({
    required super.id,
    required super.ticker,
    required super.name,
    required super.assetClass,
    super.sector,
    super.createdAt,
    super.updatedAt,
  });

  factory AdminTickerModel.fromJson(Map<String, dynamic> json) {
    DateTime? parseNullableUtc(dynamic value) {
      if (value == null) return null;
      if (value is DateTime) return value.toUtc();
      return DateTime.tryParse(value.toString())?.toUtc();
    }

    final rawClass = json['assetClass']?.toString().trim() ?? '';

    return AdminTickerModel(
      id: json['id']?.toString() ?? '',
      ticker: (json['ticker']?.toString() ?? '').trim().toUpperCase(),
      name: json['name']?.toString() ?? '',
      assetClass: rawClass.isEmpty ? 'Ações' : rawClass,
      sector: json['sector']?.toString(),
      createdAt: parseNullableUtc(json['createdAt']),
      updatedAt: parseNullableUtc(json['updatedAt']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'ticker': ticker,
      'name': name,
      'assetClass': assetClass,
      if (sector != null) 'sector': sector,
      if (createdAt != null) 'createdAt': createdAt!.toIso8601String(),
      if (updatedAt != null) 'updatedAt': updatedAt!.toIso8601String(),
    };
  }

  static List<AdminTickerModel> fromJsonList(dynamic data) {
    if (data == null) return [];
    if (data is Map && data.containsKey('data')) {
      return fromJsonList(data['data']);
    }
    if (data is List) {
      return data
          .whereType<Map>()
          .map((item) => AdminTickerModel.fromJson(Map<String, dynamic>.from(item)))
          .toList();
    }
    return [];
  }
}
