import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:provider/provider.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:frontend/src/features/profile/presentation/pages/profile_page.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';

// Import generated mocks from the widget test location
import '../test/features/profile/presentation/pages/profile_page_test.mocks.dart';

@GenerateMocks([ProfileRepository])
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  late MockProfileRepository mockRepository;
  late ProfileProvider profileProvider;

  setUp(() {
    mockRepository = MockProfileRepository();
    profileProvider = ProfileProvider(mockRepository);
  });

  Widget createWidgetUnderTest() {
    return MaterialApp(
      home: ChangeNotifierProvider<ProfileProvider>.value(
        value: profileProvider,
        child: const ProfilePage(),
      ),
    );
  }

  group('Profile E2E Tests', () {
    testWidgets('verify profile page shows PREMIUM tier correctly', (tester) async {
      // Arrange
      final profile = UserProfile(
        id: 1,
        email: 'premium@example.com',
        fullName: 'Premium User',
        tier: 'PREMIUM',
        daysRemaining: 0,
      );
      when(mockRepository.getProfile()).thenAnswer((_) async => profile);

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('Premium User'), findsOneWidget);
      expect(find.text('PREMIUM'), findsOneWidget);
      // Premium should not show "dias restantes" or show "Assinatura Ativa"
      // Based on Story 1.2-F, we check the badge.
    });

    testWidgets('verify profile page shows TRIAL with days remaining', (tester) async {
      // Arrange
      final profile = UserProfile(
        id: 2,
        email: 'trial@example.com',
        fullName: 'Trial User',
        tier: 'TRIAL',
        daysRemaining: 25,
      );
      when(mockRepository.getProfile()).thenAnswer((_) async => profile);

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('TRIAL'), findsOneWidget);
      expect(find.textContaining('25 dias'), findsOneWidget);
    });
  });
}
