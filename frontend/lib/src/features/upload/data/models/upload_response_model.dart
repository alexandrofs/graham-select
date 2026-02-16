import '../../domain/entities/upload_result.dart';

class UploadResponseModel {
  final String message;
  final int statusCode;

  const UploadResponseModel({required this.message, required this.statusCode});

  factory UploadResponseModel.fromJson(
    Map<String, dynamic> json,
    int statusCode,
  ) {
    return UploadResponseModel(
      message: json['message'] as String? ?? 'Upload completed',
      statusCode: statusCode,
    );
  }

  UploadResult toEntity() {
    return UploadResult(
      success: statusCode >= 200 && statusCode < 300,
      message: message,
      statusCode: statusCode,
    );
  }
}
