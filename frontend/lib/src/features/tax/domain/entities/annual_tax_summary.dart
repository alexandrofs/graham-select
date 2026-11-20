import 'monthly_tax_calculation.dart';

class AnnualTaxSummary {
  final int year;
  final List<MonthlyTaxCalculation> monthlyCalculations;
  final double totalTaxDueYear;
  final double totalSalesYear;
  final double totalExemptProfitYear;
  final double totalTaxableProfitYear;
  final double finalCommonLossCarryforward;
  final double finalFiiLossCarryforward;

  const AnnualTaxSummary({
    required this.year,
    required this.monthlyCalculations,
    required this.totalTaxDueYear,
    required this.totalSalesYear,
    required this.totalExemptProfitYear,
    required this.totalTaxableProfitYear,
    required this.finalCommonLossCarryforward,
    required this.finalFiiLossCarryforward,
  });

  factory AnnualTaxSummary.fromJson(Map<String, dynamic> json) {
    final rawMonths = json['monthlyCalculations'] as List<dynamic>? ?? [];
    return AnnualTaxSummary(
      year: json['year'] as int? ?? DateTime.now().year,
      monthlyCalculations: rawMonths
          .map((m) => MonthlyTaxCalculation.fromJson(Map<String, dynamic>.from(m as Map)))
          .toList(),
      totalTaxDueYear: double.tryParse(json['totalTaxDueYear']?.toString() ?? '0') ?? 0.0,
      totalSalesYear: double.tryParse(json['totalSalesYear']?.toString() ?? '0') ?? 0.0,
      totalExemptProfitYear: double.tryParse(json['totalExemptProfitYear']?.toString() ?? '0') ?? 0.0,
      totalTaxableProfitYear: double.tryParse(json['totalTaxableProfitYear']?.toString() ?? '0') ?? 0.0,
      finalCommonLossCarryforward: double.tryParse(json['finalCommonLossCarryforward']?.toString() ?? '0') ?? 0.0,
      finalFiiLossCarryforward: double.tryParse(json['finalFiiLossCarryforward']?.toString() ?? '0') ?? 0.0,
    );
  }

  factory AnnualTaxSummary.empty(int year) {
    return AnnualTaxSummary(
      year: year,
      monthlyCalculations: const [],
      totalTaxDueYear: 0.0,
      totalSalesYear: 0.0,
      totalExemptProfitYear: 0.0,
      totalTaxableProfitYear: 0.0,
      finalCommonLossCarryforward: 0.0,
      finalFiiLossCarryforward: 0.0,
    );
  }
}
