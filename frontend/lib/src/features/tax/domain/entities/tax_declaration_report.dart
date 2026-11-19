import 'tax_declaration_item.dart';

class TaxDeclarationReport {
  final int year;
  final int previousYear;
  final double previousYearTotal;
  final double currentYearTotal;
  final double netVariation;
  final double percentageVariation;
  final Map<String, double> subtotalsByGroup;
  final List<TaxDeclarationItem> items;

  const TaxDeclarationReport({
    required this.year,
    required this.previousYear,
    required this.previousYearTotal,
    required this.currentYearTotal,
    required this.netVariation,
    required this.percentageVariation,
    this.subtotalsByGroup = const {},
    required this.items,
  });

  factory TaxDeclarationReport.fromJson(Map<String, dynamic> json) {
    final rawItems = json['items'] as List<dynamic>? ?? [];
    final rawSubtotals = json['subtotalsByGroup'] as Map<String, dynamic>? ?? {};
    final subtotals = rawSubtotals.map(
      (key, value) => MapEntry(key, double.tryParse(value?.toString() ?? '0') ?? 0.0),
    );
    return TaxDeclarationReport(
      year: json['year'] as int? ?? DateTime.now().year - 1,
      previousYear: json['previousYear'] as int? ?? (DateTime.now().year - 2),
      previousYearTotal: double.tryParse(json['previousYearTotal']?.toString() ?? '0') ?? 0.0,
      currentYearTotal: double.tryParse(json['currentYearTotal']?.toString() ?? '0') ?? 0.0,
      netVariation: double.tryParse(json['netVariation']?.toString() ?? '0') ?? 0.0,
      percentageVariation: double.tryParse(json['percentageVariation']?.toString() ?? '0') ?? 0.0,
      subtotalsByGroup: subtotals,
      items: rawItems
          .map((item) => TaxDeclarationItem.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}
