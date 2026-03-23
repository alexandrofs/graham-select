class UserProfile {
  final int id;
  final String email;
  final String fullName;
  final String tier; // TRIAL, FREE, PREMIUM
  final DateTime? trialEndsAt;
  final int? daysRemaining;

  UserProfile({
    required this.id,
    required this.email,
    required this.fullName,
    required this.tier,
    this.trialEndsAt,
    this.daysRemaining,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'],
      email: json['email'] ?? '',
      fullName: json['fullName'] ?? '',
      tier: json['tier'] ?? 'TRIAL',
      trialEndsAt: json['trialEndsAt'] != null 
          ? DateTime.parse(json['trialEndsAt']) 
          : null,
      daysRemaining: json['daysRemaining'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'fullName': fullName,
      'tier': tier,
      'trialEndsAt': trialEndsAt?.toIso8601String(),
      'daysRemaining': daysRemaining,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is UserProfile &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          email == other.email &&
          fullName == other.fullName &&
          tier == other.tier &&
          trialEndsAt == other.trialEndsAt &&
          daysRemaining == other.daysRemaining;

  @override
  int get hashCode =>
      id.hashCode ^
      email.hashCode ^
      fullName.hashCode ^
      tier.hashCode ^
      trialEndsAt.hashCode ^
      daysRemaining.hashCode;
}
