class IngestionAudit {
  final String fileName;
  final DateTime uploadDate;
  final String status;
  final int successfulLines;
  final int errorLines;

  IngestionAudit({
    required this.fileName,
    required this.uploadDate,
    required this.status,
    required this.successfulLines,
    required this.errorLines,
  });

  factory IngestionAudit.fromJson(Map<String, dynamic> json) {
    return IngestionAudit(
      fileName: json['fileName'],
      uploadDate: DateTime.parse(json['uploadDate']),
      status: json['status'],
      successfulLines: json['successfulLines'],
      errorLines: json['errorLines'],
    );
  }
}
