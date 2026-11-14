import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:frontend/src/features/family_holding/data/datasources/family_profile_remote_datasource.dart';
import 'package:frontend/src/features/family_holding/domain/entities/family_profile_entity.dart';
import 'package:frontend/src/features/family_holding/domain/repositories/family_profile_repository.dart';
import 'package:frontend/src/features/family_holding/presentation/pages/family_profiles_page.dart';
import 'package:frontend/src/features/family_holding/presentation/providers/family_profile_provider.dart';

class MockFamilyRepo implements FamilyProfileRepository {
  List<FamilyProfileEntity> items = [];
  bool throwPremium = false;

  @override
  Future<List<FamilyProfileEntity>> fetchProfiles() async {
    if (throwPremium) {
      throw const PremiumRequiredException('Exclusivo Premium');
    }
    return items;
  }

  @override
  Future<FamilyProfileEntity> linkProfile({
    required String cpf,
    required String name,
    String? relationship,
  }) async {
    final created = FamilyProfileEntity(
      id: 'new-id',
      name: name,
      cpfMasked: '***.982.247-**',
      relationship: relationship,
      active: true,
    );
    items = [...items, created];
    return created;
  }

  @override
  Future<void> unlinkProfile(String profileId) async {
    items = items.where((e) => e.id != profileId).toList();
  }
}

void main() {
  Widget wrapWithProvider(FamilyProfileProvider provider) {
    return MaterialApp(
      home: ChangeNotifierProvider<FamilyProfileProvider>.value(
        value: provider,
        child: const FamilyProfilesPage(),
      ),
    );
  }

  testWidgets('exibe estado vazio e permite vincular e desvincular CPF', (tester) async {
    final repo = MockFamilyRepo();
    final provider = FamilyProfileProvider(repository: repo);

    await tester.pumpWidget(wrapWithProvider(provider));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('family_profiles_empty_state')), findsOneWidget);

    await tester.tap(find.byKey(const Key('family_profile_add_button')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('family_profile_name_input')), 'Maria Silva');
    await tester.enterText(find.byKey(const Key('family_profile_cpf_input')), '52998224725');
    await tester.enterText(find.byKey(const Key('family_profile_relationship_input')), 'Cônjuge');
    await tester.tap(find.byKey(const Key('family_profile_save_button')));
    await tester.pumpAndSettle();

    expect(find.text('Maria Silva'), findsOneWidget);
    expect(find.byKey(const Key('family_profile_delete_button_new-id')), findsOneWidget);

    await tester.tap(find.byKey(const Key('family_profile_delete_button_new-id')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('family_profiles_empty_state')), findsOneWidget);
  });

  testWidgets('exibe banner Premium quando usuário é FREE (403)', (tester) async {
    final repo = MockFamilyRepo()..throwPremium = true;
    final provider = FamilyProfileProvider(repository: repo);

    await tester.pumpWidget(wrapWithProvider(provider));
    await tester.pumpAndSettle();

    expect(find.text('Recurso Exclusivo Premium'), findsOneWidget);
    expect(find.byKey(const Key('family_profile_add_button')), findsNothing);
  });
}

