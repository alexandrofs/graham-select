class RelevantFact {
  final String id;
  final String ticker;
  final String title;
  final String? summary;
  final String? sourceUrl;
  final String category;
  final DateTime publicationDate;

  const RelevantFact({
    required this.id,
    required this.ticker,
    required this.title,
    this.summary,
    this.sourceUrl,
    required this.category,
    required this.publicationDate,
  });
}
