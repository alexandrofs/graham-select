import '../../domain/entities/portfolio_tag.dart';

class PortfolioTagModel extends PortfolioTag {
  static const String defaultColor = '#6366F1';
  static final RegExp _hexColorRegex = RegExp(r'^#[0-9A-Fa-f]{6}$');
  static final RegExp _tzSuffixRegex = RegExp(r'(Z|[+-]\d{2}:?\d{2})$', caseSensitive: false);

  const PortfolioTagModel({
    required super.id,
    required super.name,
    required super.color,
    super.tickers = const [],
    super.assetCount = 0,
    super.createdAt,
    super.updatedAt,
  });

  factory PortfolioTagModel.fromJson(Map<String, dynamic> json) {
    final id = json['id']?.toString().trim() ?? '';
    final name = json['name']?.toString().trim() ?? '';
    if (id.isEmpty || name.isEmpty) {
      throw const FormatException('Tag payload must include non-empty id and name.');
    }

    final rawColor = json['color']?.toString().trim();
    final color = (rawColor != null && _hexColorRegex.hasMatch(rawColor))
        ? rawColor.toUpperCase()
        : defaultColor;

    final rawTickers = json['tickers'];
    final tickers = rawTickers is List
        ? List<String>.unmodifiable(
            rawTickers
                .map((e) => e?.toString().trim().toUpperCase() ?? '')
                .where((e) => e.isNotEmpty),
          )
        : const <String>[];

    final rawCount = json['assetCount'];
    final assetCount = rawCount is num ? rawCount.toInt() : tickers.length;

    return PortfolioTagModel(
      id: id,
      name: name,
      color: color,
      tickers: tickers,
      assetCount: assetCount,
      createdAt: _parseUtcTimestamp(json['createdAt']),
      updatedAt: _parseUtcTimestamp(json['updatedAt']),
    );
  }

  static DateTime? _parseUtcTimestamp(Object? value) {
    final trimmed = value?.toString().trim() ?? '';
    if (trimmed.isEmpty) {
      return null;
    }
    final normalized = _tzSuffixRegex.hasMatch(trimmed) ? trimmed : '${trimmed}Z';
    return (DateTime.tryParse(normalized) ?? DateTime.tryParse(trimmed))?.toUtc();
  }
}
