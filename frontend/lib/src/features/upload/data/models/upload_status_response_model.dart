import '../../domain/entities/upload_processing_status.dart';

class UploadStatusResponseModel {
  final String correlationId;
  final String fileName;
  final String status;
  final int processedRows;
  final int successfulRows;
  final int failedRows;
  final String message;

  const UploadStatusResponseModel({
    required this.correlationId,
    required this.fileName,
    required this.status,
    required this.processedRows,
    required this.successfulRows,
    required this.failedRows,
    required this.message,
  });

  factory UploadStatusResponseModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>? ?? json;
    return UploadStatusResponseModel(
      correlationId: data['correlationId'] as String? ?? '',
      fileName: data['fileName'] as String? ?? '',
      status: data['status'] as String? ?? 'RECEIVED',
      processedRows: (data['processedRows'] as num?)?.toInt() ?? 0,
      successfulRows: (data['successfulRows'] as num?)?.toInt() ?? 0,
      failedRows: (data['failedRows'] as num?)?.toInt() ?? 0,
      message: data['message'] as String? ?? 'Aguardando processamento.',
    );
  }

  UploadProcessingStatus toEntity() {
    return UploadProcessingStatus(
      correlationId: correlationId,
      fileName: fileName,
      status: status,
      processedRows: processedRows,
      successfulRows: successfulRows,
      failedRows: failedRows,
      message: message,
    );
  }
}
