import '../entities/family_profile_entity.dart';

abstract class FamilyProfileRepository {
  Future<List<FamilyProfileEntity>> fetchProfiles();

  Future<FamilyProfileEntity> linkProfile({
    required String cpf,
    required String name,
    String? relationship,
  });

  Future<void> unlinkProfile(String profileId);
}

