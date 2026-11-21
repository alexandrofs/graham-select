class WebPushVapidKeyModel {
  final String publicKey;
  final String provider;

  const WebPushVapidKeyModel({
    required this.publicKey,
    required this.provider,
  });

  factory WebPushVapidKeyModel.fromJson(Map<String, dynamic> json) {
    return WebPushVapidKeyModel(
      publicKey: (json['publicKey'] as String?) ?? '',
      provider: (json['provider'] as String?) ?? '',
    );
  }
}

class WebPushPreferencesModel {
  final bool optedIn;
  final DateTime? optedInAt;
  final double minMarginOfSafetyPct;
  final bool onlyCustodyTickers;

  const WebPushPreferencesModel({
    required this.optedIn,
    this.optedInAt,
    required this.minMarginOfSafetyPct,
    required this.onlyCustodyTickers,
  });

  factory WebPushPreferencesModel.fromJson(Map<String, dynamic> json) {
    return WebPushPreferencesModel(
      optedIn: (json['optedIn'] as bool?) ?? false,
      optedInAt: json['optedInAt'] != null
          ? DateTime.tryParse(json['optedInAt'].toString())
          : null,
      minMarginOfSafetyPct:
          (json['minMarginOfSafetyPct'] as num?)?.toDouble() ?? 20.0,
      onlyCustodyTickers: (json['onlyCustodyTickers'] as bool?) ?? true,
    );
  }

  Map<String, dynamic> toUpdateJson() {
    return {
      'optedIn': optedIn,
      'minMarginOfSafetyPct': minMarginOfSafetyPct,
      'onlyCustodyTickers': onlyCustodyTickers,
    };
  }

  WebPushPreferencesModel copyWith({
    bool? optedIn,
    DateTime? optedInAt,
    double? minMarginOfSafetyPct,
    bool? onlyCustodyTickers,
  }) {
    return WebPushPreferencesModel(
      optedIn: optedIn ?? this.optedIn,
      optedInAt: optedInAt ?? this.optedInAt,
      minMarginOfSafetyPct: minMarginOfSafetyPct ?? this.minMarginOfSafetyPct,
      onlyCustodyTickers: onlyCustodyTickers ?? this.onlyCustodyTickers,
    );
  }
}

class WebPushSubscriptionModel {
  final String id;
  final String endpointPreview;
  final String? userAgent;
  final bool active;
  final DateTime? lastUsedAt;
  final DateTime createdAt;

  const WebPushSubscriptionModel({
    required this.id,
    required this.endpointPreview,
    this.userAgent,
    required this.active,
    this.lastUsedAt,
    required this.createdAt,
  });

  factory WebPushSubscriptionModel.fromJson(Map<String, dynamic> json) {
    return WebPushSubscriptionModel(
      id: (json['id'] ?? '').toString(),
      endpointPreview: (json['endpointPreview'] as String?) ?? '',
      userAgent: json['userAgent'] as String?,
      active: (json['active'] as bool?) ?? true,
      lastUsedAt: json['lastUsedAt'] != null
          ? DateTime.tryParse(json['lastUsedAt'].toString())
          : null,
      createdAt: DateTime.tryParse((json['createdAt'] ?? '').toString()) ??
          DateTime.now().toUtc(),
    );
  }
}

class WebPushNotificationLogModel {
  final String id;
  final String? ticker;
  final String eventType;
  final String title;
  final String messagePreview;
  final String provider;
  final int? httpStatus;
  final String status;
  final String? errorMessage;
  final DateTime sentAt;

  const WebPushNotificationLogModel({
    required this.id,
    this.ticker,
    required this.eventType,
    required this.title,
    required this.messagePreview,
    required this.provider,
    this.httpStatus,
    required this.status,
    this.errorMessage,
    required this.sentAt,
  });

  factory WebPushNotificationLogModel.fromJson(Map<String, dynamic> json) {
    return WebPushNotificationLogModel(
      id: (json['id'] ?? '').toString(),
      ticker: json['ticker'] as String?,
      eventType: (json['eventType'] as String?) ?? '',
      title: (json['title'] as String?) ?? '',
      messagePreview: (json['messagePreview'] as String?) ?? '',
      provider: (json['provider'] as String?) ?? '',
      httpStatus: (json['httpStatus'] as num?)?.toInt(),
      status: (json['status'] as String?) ?? '',
      errorMessage: json['errorMessage'] as String?,
      sentAt: DateTime.tryParse((json['sentAt'] ?? '').toString()) ??
          DateTime.now().toUtc(),
    );
  }
}

class WebPushTestAlertResultModel {
  final int sentCount;
  final int failedCount;
  final String provider;
  final String messagePreview;
  final DateTime sentAt;

  const WebPushTestAlertResultModel({
    required this.sentCount,
    required this.failedCount,
    required this.provider,
    required this.messagePreview,
    required this.sentAt,
  });

  factory WebPushTestAlertResultModel.fromJson(Map<String, dynamic> json) {
    return WebPushTestAlertResultModel(
      sentCount: (json['sentCount'] as num?)?.toInt() ?? 0,
      failedCount: (json['failedCount'] as num?)?.toInt() ?? 0,
      provider: (json['provider'] as String?) ?? '',
      messagePreview: (json['messagePreview'] as String?) ?? '',
      sentAt: DateTime.tryParse((json['sentAt'] ?? '').toString()) ??
          DateTime.now().toUtc(),
    );
  }
}
