import 'monthly_trade_operation.dart';

enum DarfStatus {
  noTaxDue,
  exempt,
  loss,
  accumulatedBelowMinimum,
  due;

  static DarfStatus fromString(String? status) {
    switch (status?.toUpperCase()) {
      case 'EXEMPT':
        return DarfStatus.exempt;
      case 'LOSS':
        return DarfStatus.loss;
      case 'ACCUMULATED_BELOW_MINIMUM':
        return DarfStatus.accumulatedBelowMinimum;
      case 'DUE':
        return DarfStatus.due;
      case 'NO_TAX_DUE':
      default:
        return DarfStatus.noTaxDue;
    }
  }

  String get label {
    switch (this) {
      case DarfStatus.exempt:
        return 'Isento';
      case DarfStatus.loss:
        return 'Prejuízo a Compensar';
      case DarfStatus.accumulatedBelowMinimum:
        return 'Acumulado (< R\$ 10)';
      case DarfStatus.due:
        return 'DARF a Pagar';
      case DarfStatus.noTaxDue:
        return 'Sem Imposto Devido';
    }
  }
}

class MonthlyTaxCalculation {
  final int month;
  final int year;
  final double totalEquitySales;
  final bool isEquityExempt;
  final double exemptProfit;
  final double commonTaxableProfit;
  final double commonLossCompensated;
  final double commonLossCarryforward;
  final double commonTaxDue;
  final double fiiTaxableProfit;
  final double fiiLossCompensated;
  final double fiiLossCarryforward;
  final double fiiTaxDue;
  final double totalTaxDue;
  final double accumulatedTaxUnder10;
  final DarfStatus darfStatus;
  final double darfAmount;
  final DateTime? darfDueDate;
  final List<MonthlyTradeOperation> operations;

  const MonthlyTaxCalculation({
    required this.month,
    required this.year,
    required this.totalEquitySales,
    required this.isEquityExempt,
    required this.exemptProfit,
    required this.commonTaxableProfit,
    required this.commonLossCompensated,
    required this.commonLossCarryforward,
    required this.commonTaxDue,
    required this.fiiTaxableProfit,
    required this.fiiLossCompensated,
    required this.fiiLossCarryforward,
    required this.fiiTaxDue,
    required this.totalTaxDue,
    required this.accumulatedTaxUnder10,
    required this.darfStatus,
    required this.darfAmount,
    this.darfDueDate,
    required this.operations,
  });

  factory MonthlyTaxCalculation.fromJson(Map<String, dynamic> json) {
    final rawOps = json['operations'] as List<dynamic>? ?? [];
    return MonthlyTaxCalculation(
      month: json['month'] as int? ?? 1,
      year: json['year'] as int? ?? DateTime.now().year,
      totalEquitySales: double.tryParse(json['totalEquitySales']?.toString() ?? '0') ?? 0.0,
      isEquityExempt: json['isEquityExempt'] as bool? ?? true,
      exemptProfit: double.tryParse(json['exemptProfit']?.toString() ?? '0') ?? 0.0,
      commonTaxableProfit: double.tryParse(json['commonTaxableProfit']?.toString() ?? '0') ?? 0.0,
      commonLossCompensated: double.tryParse(json['commonLossCompensated']?.toString() ?? '0') ?? 0.0,
      commonLossCarryforward: double.tryParse(json['commonLossCarryforward']?.toString() ?? '0') ?? 0.0,
      commonTaxDue: double.tryParse(json['commonTaxDue']?.toString() ?? '0') ?? 0.0,
      fiiTaxableProfit: double.tryParse(json['fiiTaxableProfit']?.toString() ?? '0') ?? 0.0,
      fiiLossCompensated: double.tryParse(json['fiiLossCompensated']?.toString() ?? '0') ?? 0.0,
      fiiLossCarryforward: double.tryParse(json['fiiLossCarryforward']?.toString() ?? '0') ?? 0.0,
      fiiTaxDue: double.tryParse(json['fiiTaxDue']?.toString() ?? '0') ?? 0.0,
      totalTaxDue: double.tryParse(json['totalTaxDue']?.toString() ?? '0') ?? 0.0,
      accumulatedTaxUnder10: double.tryParse(json['accumulatedTaxUnder10']?.toString() ?? '0') ?? 0.0,
      darfStatus: DarfStatus.fromString(json['darfStatus']?.toString()),
      darfAmount: double.tryParse(json['darfAmount']?.toString() ?? '0') ?? 0.0,
      darfDueDate: json['darfDueDate'] != null ? DateTime.tryParse(json['darfDueDate'].toString()) : null,
      operations: rawOps
          .map((op) => MonthlyTradeOperation.fromJson(Map<String, dynamic>.from(op as Map)))
          .toList(),
    );
  }

  DateTime get monthDate => DateTime(year, month);
}
