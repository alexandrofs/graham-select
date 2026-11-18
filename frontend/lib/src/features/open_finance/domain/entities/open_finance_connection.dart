class OpenFinanceConnection {
  final String id;
  final String userId;
  final String? profileId;
  final String institutionId;
  final String institutionName;
  final String? institutionLogoUrl;
  final String status; // PENDING_CONSENT, ACTIVE, SYNCING, EXPIRED, REVOKED, FAILED
  final DateTime? consentExpiresAt;
  final DateTime? lastSyncAt;
  final String? lastSyncStatus;
  final String? errorMessage;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const OpenFinanceConnection({
    required this.id,
    required this.userId,
    this.profileId,
    required this.institutionId,
    required this.institutionName,
    this.institutionLogoUrl,
    required this.status,
    this.consentExpiresAt,
    this.lastSyncAt,
    this.lastSyncStatus,
    this.errorMessage,
    this.createdAt,
    this.updatedAt,
  });

  factory OpenFinanceConnection.fromJson(Map<String, dynamic> json) {
    return OpenFinanceConnection(
      id: json['id'] as String? ?? '',
      userId: json['userId'] as String? ?? '',
      profileId: json['profileId'] as String?,
      institutionId: json['institutionId'] as String? ?? '',
      institutionName: json['institutionName'] as String? ?? '',
      institutionLogoUrl: json['institutionLogoUrl'] as String?,
      status: json['status'] as String? ?? 'PENDING_CONSENT',
      consentExpiresAt: json['consentExpiresAt'] != null
          ? DateTime.tryParse(json['consentExpiresAt'] as String)
          : null,
      lastSyncAt: json['lastSyncAt'] != null
          ? DateTime.tryParse(json['lastSyncAt'] as String)
          : null,
      lastSyncStatus: json['lastSyncStatus'] as String?,
      errorMessage: json['errorMessage'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String)
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'] as String)
          : null,
    );
  }
}
