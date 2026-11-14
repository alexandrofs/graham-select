class FamilyProfileEntity {
  final String id;
  final String name;
  final String cpfMasked;
  final String? relationship;
  final bool active;
  final DateTime? createdAt;

  const FamilyProfileEntity({
    required this.id,
    required this.name,
    required this.cpfMasked,
    this.relationship,
    required this.active,
    this.createdAt,
  });
}

