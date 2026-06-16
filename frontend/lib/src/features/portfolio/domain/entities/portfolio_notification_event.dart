class PortfolioNotificationEvent {
  final String userId;
  final String event;
  final DateTime timestamp;
  final Map<String, dynamic>? payload;

  const PortfolioNotificationEvent({
    required this.userId,
    required this.event,
    required this.timestamp,
    this.payload,
  });

  factory PortfolioNotificationEvent.fromJson(Map<String, dynamic> json) {
    return PortfolioNotificationEvent(
      userId: json['userId'] as String? ?? '',
      event: json['event'] as String? ?? '',
      timestamp: json['timestamp'] != null
          ? DateTime.parse(json['timestamp'] as String)
          : DateTime.now(),
      payload: json,
    );
  }
}
