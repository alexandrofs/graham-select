class UploadResult {
  final bool success;
  final String message;
  final int? statusCode;
  final String? correlationId;

  const UploadResult({
    required this.success,
    required this.message,
    this.statusCode,
    this.correlationId,
  });

  @override
  String toString() {
    return 'UploadResult(success: $success, message: $message, statusCode: $statusCode, correlationId: $correlationId)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is UploadResult &&
        other.success == success &&
        other.message == message &&
        other.statusCode == statusCode &&
        other.correlationId == correlationId;
  }

  @override
  int get hashCode =>
      success.hashCode ^
      message.hashCode ^
      statusCode.hashCode ^
      correlationId.hashCode;
}
