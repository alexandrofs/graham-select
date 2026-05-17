import '../../domain/entities/upload_result.dart';

class UploadResponseModel {
  final String message;
  final int statusCode;
  final String? correlationId;

  const UploadResponseModel({
    required this.message,
    required this.statusCode,
    this.correlationId,
  });

  factory UploadResponseModel.fromJson(
    Map<String, dynamic> json,
    int statusCode,
  ) {
    return UploadResponseModel(
      message: json['message'] as String? ?? 'Upload completed',
      statusCode: statusCode,
      correlationId: json['correlationId'] as String?,
    );
  }

  UploadResult toEntity() {
    return UploadResult(
      success: statusCode >= 200 && statusCode < 300,
      message: message,
      statusCode: statusCode,
      correlationId: correlationId,
    );
  }
}
