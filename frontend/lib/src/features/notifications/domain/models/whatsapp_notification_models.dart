class WhatsappPreferencesModel {
  final String phoneNumber;
  final bool optedIn;
  final DateTime? optedInAt;
  final bool notifyDividends;
  final bool notifyRebalanceTriggers;
  final bool notifyGoalMilestones;
  final double rebalanceDriftThresholdPct;

  const WhatsappPreferencesModel({
    required this.phoneNumber,
    required this.optedIn,
    this.optedInAt,
    required this.notifyDividends,
    required this.notifyRebalanceTriggers,
    required this.notifyGoalMilestones,
    required this.rebalanceDriftThresholdPct,
  });

  factory WhatsappPreferencesModel.fromJson(Map<String, dynamic> json) {
    return WhatsappPreferencesModel(
      phoneNumber: (json['phoneNumber'] as String?) ?? '',
      optedIn: (json['optedIn'] as bool?) ?? false,
      optedInAt: json['optedInAt'] != null
          ? DateTime.tryParse(json['optedInAt'].toString())
          : null,
      notifyDividends: (json['notifyDividends'] as bool?) ?? true,
      notifyRebalanceTriggers: (json['notifyRebalanceTriggers'] as bool?) ?? true,
      notifyGoalMilestones: (json['notifyGoalMilestones'] as bool?) ?? true,
      rebalanceDriftThresholdPct:
          (json['rebalanceDriftThresholdPct'] as num?)?.toDouble() ?? 5.0,
    );
  }

  Map<String, dynamic> toUpdateJson() {
    return {
      'phoneNumber': phoneNumber,
      'optedIn': optedIn,
      'notifyDividends': notifyDividends,
      'notifyRebalanceTriggers': notifyRebalanceTriggers,
      'notifyGoalMilestones': notifyGoalMilestones,
      'rebalanceDriftThresholdPct': rebalanceDriftThresholdPct,
    };
  }
}

class WhatsappNotificationLogModel {
  final String id;
  final String eventType;
  final String messagePreview;
  final String provider;
  final String status;
  final String? errorMessage;
  final DateTime sentAt;

  const WhatsappNotificationLogModel({
    required this.id,
    required this.eventType,
    required this.messagePreview,
    required this.provider,
    required this.status,
    this.errorMessage,
    required this.sentAt,
  });

  factory WhatsappNotificationLogModel.fromJson(Map<String, dynamic> json) {
    return WhatsappNotificationLogModel(
      id: (json['id'] ?? '').toString(),
      eventType: (json['eventType'] as String?) ?? '',
      messagePreview: (json['messagePreview'] as String?) ?? '',
      provider: (json['provider'] as String?) ?? '',
      status: (json['status'] as String?) ?? '',
      errorMessage: json['errorMessage'] as String?,
      sentAt: DateTime.tryParse((json['sentAt'] ?? '').toString()) ??
          DateTime.now().toUtc(),
    );
  }
}

class WhatsappTestAlertResultModel {
  final String status;
  final String provider;
  final String? providerMessageId;
  final String messagePreview;
  final DateTime sentAt;

  const WhatsappTestAlertResultModel({
    required this.status,
    required this.provider,
    this.providerMessageId,
    required this.messagePreview,
    required this.sentAt,
  });

  factory WhatsappTestAlertResultModel.fromJson(Map<String, dynamic> json) {
    return WhatsappTestAlertResultModel(
      status: (json['status'] as String?) ?? '',
      provider: (json['provider'] as String?) ?? '',
      providerMessageId: json['providerMessageId'] as String?,
      messagePreview: (json['messagePreview'] as String?) ?? '',
      sentAt: DateTime.tryParse((json['sentAt'] ?? '').toString()) ??
          DateTime.now().toUtc(),
    );
  }
}
