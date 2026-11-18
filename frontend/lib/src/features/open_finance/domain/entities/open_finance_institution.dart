class OpenFinanceInstitution {
  final String id;
  final String name;
  final String logoUrl;
  final bool enabled;
  final String description;

  const OpenFinanceInstitution({
    required this.id,
    required this.name,
    required this.logoUrl,
    required this.enabled,
    required this.description,
  });

  factory OpenFinanceInstitution.fromJson(Map<String, dynamic> json) {
    return OpenFinanceInstitution(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      logoUrl: json['logoUrl'] as String? ?? '',
      enabled: json['enabled'] as bool? ?? true,
      description: json['description'] as String? ?? '',
    );
  }
}
