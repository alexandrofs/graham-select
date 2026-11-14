import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/family_holding/data/datasources/family_profile_remote_datasource.dart';
import 'package:frontend/src/features/family_holding/domain/entities/family_profile_entity.dart';
import 'package:frontend/src/features/family_holding/domain/repositories/family_profile_repository.dart';
import 'package:frontend/src/features/family_holding/presentation/providers/family_profile_provider.dart';

class FakeFamilyProfileRepository implements FamilyProfileRepository {
  List<FamilyProfileEntity> profilesToReturn = [];
  Exception? errorToThrow;

  @override
  Future<List<FamilyProfileEntity>> fetchProfiles() async {
    if (errorToThrow != null) throw errorToThrow!;
    return profilesToReturn;
  }

  @override
  Future<FamilyProfileEntity> linkProfile({
    required String cpf,
    required String name,
    String? relationship,
  }) async {
    if (errorToThrow != null) throw errorToThrow!;
    return FamilyProfileEntity(
      id: 'prof-1',
      name: name,
      cpfMasked: '***.982.247-**',
      relationship: relationship,
      active: true,
    );
  }

  @override
  Future<void> unlinkProfile(String profileId) async {
    if (errorToThrow != null) throw errorToThrow!;
  }
}

void main() {
  group('FamilyProfileProvider', () {
    test('fetchProfiles carrega lista com sucesso', () async {
      final repo = FakeFamilyProfileRepository()
        ..profilesToReturn = [
          const FamilyProfileEntity(
            id: '1',
            name: 'Maria',
            cpfMasked: '***.982.247-**',
            relationship: 'Cônjuge',
            active: true,
          ),
        ];
      final provider = FamilyProfileProvider(repository: repo);

      await provider.fetchProfiles();

      expect(provider.state, FamilyProfileState.loaded);
      expect(provider.profiles.length, 1);
      expect(provider.profiles.first.name, 'Maria');
    });

    test('fetchProfiles define premiumRequired em PremiumRequiredException', () async {
      final repo = FakeFamilyProfileRepository()
        ..errorToThrow = const PremiumRequiredException('Recurso Premium');
      final provider = FamilyProfileProvider(repository: repo);

      await provider.fetchProfiles();

      expect(provider.state, FamilyProfileState.premiumRequired);
      expect(provider.errorMessage, 'Recurso Premium');
    });

    test('linkProfile adiciona perfil e unlinkProfile remove da lista', () async {
      final repo = FakeFamilyProfileRepository();
      final provider = FamilyProfileProvider(repository: repo);

      final linked = await provider.linkProfile(
        cpf: '52998224725',
        name: 'Maria Silva',
        relationship: 'Cônjuge',
      );
      expect(linked, isTrue);
      expect(provider.profiles.length, 1);

      final unlinked = await provider.unlinkProfile('prof-1');
      expect(unlinked, isTrue);
      expect(provider.profiles, isEmpty);
    });
  });
}

