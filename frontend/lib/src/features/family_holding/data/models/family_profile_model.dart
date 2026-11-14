import '../../domain/entities/family_profile_entity.dart';

class FamilyProfileModel extends FamilyProfileEntity {
  const FamilyProfileModel({
    required super.id,
    required super.name,
    required super.cpfMasked,
    super.relationship,
    required super.active,
    super.createdAt,
  });

  factory FamilyProfileModel.fromJson(Map<String, dynamic> json) {
    return FamilyProfileModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      cpfMasked: json['cpfMasked']?.toString() ?? '***.***.***-**',
      relationship: json['relationship']?.toString(),
      active: json['active'] as bool? ?? true,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
    );
  }
}

