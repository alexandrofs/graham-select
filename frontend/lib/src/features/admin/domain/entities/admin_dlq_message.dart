class AdminDlqMessage {
  final int id;
  final String eventId;
  final String correlationId;
  final String anonymizedUserId;
  final String fileName;
  final int? lineNumber;
  final String errorMessage;
  final String originalTopic;
  final String status;
  final DateTime timestamp;
  final DateTime? resolvedAt;

  const AdminDlqMessage({
    required this.id,
    required this.eventId,
    required this.correlationId,
    required this.anonymizedUserId,
    required this.fileName,
    this.lineNumber,
    required this.errorMessage,
    required this.originalTopic,
    required this.status,
    required this.timestamp,
    this.resolvedAt,
  });

  bool get isPending => status == 'PENDING';
  bool get isResolved => status == 'RESOLVED';
  bool get isFailed => status == 'FAILED';
}
