import '../../domain/entities/relevant_fact.dart';

class RelevantFactModel extends RelevantFact {
  const RelevantFactModel({
    required super.id,
    required super.ticker,
    required super.title,
    super.summary,
    super.sourceUrl,
    required super.category,
    required super.publicationDate,
  });

  factory RelevantFactModel.fromJson(Map<String, dynamic> json) {
    return RelevantFactModel(
      id: json['id']?.toString() ?? '',
      ticker: json['ticker']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      summary: json['summary']?.toString(),
      sourceUrl: json['sourceUrl']?.toString(),
      category: json['category']?.toString() ?? 'FATO_RELEVANTE',
      publicationDate: _parseDate(json['publicationDate']) ?? DateTime.now(),
    );
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
