class UserProfile {
  final int id;
  final String email;
  final String fullName;
  final String tier; // TRIAL, FREE, PREMIUM
  final String? commercialPlan; // FOUNDER_LIFETIME, PRO_TRIAL, FREE
  final String? investorProfile; // CONSERVATIVE, MODERATE, AGGRESSIVE
  final DateTime? trialEndsAt;
  final DateTime? trialExpiresAt;
  final DateTime? termsAcceptedAt;
  final int? daysRemaining;
  final String role; // USER, ADMIN

  UserProfile({
    required this.id,
    required this.email,
    required this.fullName,
    required this.tier,
    this.commercialPlan,
    this.investorProfile,
    this.trialEndsAt,
    this.trialExpiresAt,
    this.termsAcceptedAt,
    this.daysRemaining,
    this.role = 'USER',
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'],
      email: json['email'] ?? '',
      fullName: json['fullName'] ?? '',
      tier: json['tier'] ?? 'TRIAL',
      commercialPlan: json['commercialPlan'] as String?,
      investorProfile: json['investorProfile'],
      trialEndsAt: json['trialEndsAt'] != null
          ? DateTime.parse(json['trialEndsAt'])
          : null,
      trialExpiresAt: json['trialExpiresAt'] != null
          ? DateTime.parse(json['trialExpiresAt'])
          : null,
      termsAcceptedAt: json['termsAcceptedAt'] != null
          ? DateTime.parse(json['termsAcceptedAt'])
          : null,
      daysRemaining: json['daysRemaining'],
      role: json['role'] as String? ?? 'USER',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'fullName': fullName,
      'tier': tier,
      'commercialPlan': commercialPlan,
      'investorProfile': investorProfile,
      'trialEndsAt': trialEndsAt?.toIso8601String(),
      'trialExpiresAt': trialExpiresAt?.toIso8601String(),
      'termsAcceptedAt': termsAcceptedAt?.toIso8601String(),
      'daysRemaining': daysRemaining,
      'role': role,
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
          commercialPlan == other.commercialPlan &&
          investorProfile == other.investorProfile &&
          trialEndsAt == other.trialEndsAt &&
          trialExpiresAt == other.trialExpiresAt &&
          termsAcceptedAt == other.termsAcceptedAt &&
          daysRemaining == other.daysRemaining &&
          role == other.role;

  @override
  int get hashCode =>
      id.hashCode ^
      email.hashCode ^
      fullName.hashCode ^
      tier.hashCode ^
      commercialPlan.hashCode ^
      investorProfile.hashCode ^
      trialEndsAt.hashCode ^
      trialExpiresAt.hashCode ^
      termsAcceptedAt.hashCode ^
      daysRemaining.hashCode ^
      role.hashCode;
}
