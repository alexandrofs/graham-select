class PortfolioNotificationEvent {
  final String userId;
  final String event;
  final DateTime timestamp;

  const PortfolioNotificationEvent({
    required this.userId,
    required this.event,
    required this.timestamp,
  });

  factory PortfolioNotificationEvent.fromJson(Map<String, dynamic> json) {
    return PortfolioNotificationEvent(
      userId: json['userId'] as String? ?? '',
      event: json['event'] as String? ?? '',
      timestamp: json['timestamp'] != null
          ? DateTime.parse(json['timestamp'] as String)
          : DateTime.now(),
    );
  }
}
