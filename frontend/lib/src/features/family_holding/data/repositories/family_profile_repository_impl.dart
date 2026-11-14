import '../../domain/entities/family_profile_entity.dart';
import '../../domain/repositories/family_profile_repository.dart';
import '../datasources/family_profile_remote_datasource.dart';

class FamilyProfileRepositoryImpl implements FamilyProfileRepository {
  final FamilyProfileRemoteDataSource remoteDataSource;

  const FamilyProfileRepositoryImpl({required this.remoteDataSource});

  @override
  Future<List<FamilyProfileEntity>> fetchProfiles() {
    return remoteDataSource.fetchProfiles();
  }

  @override
  Future<FamilyProfileEntity> linkProfile({
    required String cpf,
    required String name,
    String? relationship,
  }) {
    return remoteDataSource.linkProfile(
      cpf: cpf,
      name: name,
      relationship: relationship,
    );
  }

  @override
  Future<void> unlinkProfile(String profileId) {
    return remoteDataSource.unlinkProfile(profileId);
  }
}

