import '../../domain/entities/admin_dlq_message.dart';

class AdminDlqMessageModel extends AdminDlqMessage {
  const AdminDlqMessageModel({
    required super.id,
    required super.eventId,
    required super.correlationId,
    required super.anonymizedUserId,
    required super.fileName,
    super.lineNumber,
    required super.errorMessage,
    required super.originalTopic,
    required super.status,
    required super.timestamp,
    super.resolvedAt,
  });

  factory AdminDlqMessageModel.fromJson(Map<String, dynamic> json) {
    DateTime parseUtc(dynamic value) {
      if (value == null) return DateTime.now().toUtc();
      if (value is DateTime) return value.toUtc();
      return DateTime.tryParse(value.toString())?.toUtc() ?? DateTime.now().toUtc();
    }

    DateTime? parseNullableUtc(dynamic value) {
      if (value == null) return null;
      if (value is DateTime) return value.toUtc();
      return DateTime.tryParse(value.toString())?.toUtc();
    }

    int parseId(dynamic value) {
      if (value is num) return value.toInt();
      return int.tryParse(value?.toString() ?? '0') ?? 0;
    }

    int? parseNullableInt(dynamic value) {
      if (value == null) return null;
      if (value is num) return value.toInt();
      return int.tryParse(value.toString());
    }

    return AdminDlqMessageModel(
      id: parseId(json['id']),
      eventId: json['eventId']?.toString() ?? '',
      correlationId: json['correlationId']?.toString() ?? '',
      anonymizedUserId: json['anonymizedUserId']?.toString() ?? 'usr_***0000',
      fileName: json['fileName']?.toString() ?? 'arquivo_desconhecido',
      lineNumber: parseNullableInt(json['lineNumber']),
      errorMessage: json['errorMessage']?.toString() ?? '',
      originalTopic: json['originalTopic']?.toString() ?? 'trade-extracted',
      status: json['status']?.toString() ?? 'PENDING',
      timestamp: parseUtc(json['timestamp']),
      resolvedAt: parseNullableUtc(json['resolvedAt']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'eventId': eventId,
      'correlationId': correlationId,
      'anonymizedUserId': anonymizedUserId,
      'fileName': fileName,
      'lineNumber': lineNumber,
      'errorMessage': errorMessage,
      'originalTopic': originalTopic,
      'status': status,
      'timestamp': timestamp.toIso8601String(),
      'resolvedAt': resolvedAt?.toIso8601String(),
    };
  }

  static List<AdminDlqMessageModel> fromJsonList(dynamic data) {
    if (data == null) return [];
    if (data is Map && data.containsKey('data')) {
      return fromJsonList(data['data']);
    }
    if (data is List) {
      return data
          .whereType<Map>()
          .map((item) => AdminDlqMessageModel.fromJson(Map<String, dynamic>.from(item)))
          .toList();
    }
    return [];
  }
}
