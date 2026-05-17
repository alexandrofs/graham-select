class UploadProcessingStatus {
  final String correlationId;
  final String fileName;
  final String status;
  final int processedRows;
  final int successfulRows;
  final int failedRows;
  final int duplicatedRows;
  final String message;

  const UploadProcessingStatus({
    required this.correlationId,
    required this.fileName,
    required this.status,
    required this.processedRows,
    required this.successfulRows,
    required this.failedRows,
    required this.duplicatedRows,
    required this.message,
  });

  bool get isCompleted =>
      status == 'COMPLETED' || status == 'COMPLETED_WITH_ERRORS';

  bool get isFailed => status == 'FAILED';
}
