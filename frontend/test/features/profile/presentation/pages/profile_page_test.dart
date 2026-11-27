import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import 'package:frontend/src/features/auth/data/auth_repository.dart';
import 'package:frontend/src/features/profile/presentation/pages/profile_page.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';

import 'profile_page_test.mocks.dart';

@GenerateMocks([ProfileRepository])
void main() {
  late MockProfileRepository mockRepository;
  late ProfileProvider profileProvider;

  setUp(() {
    mockRepository = MockProfileRepository();
    profileProvider = ProfileProvider(mockRepository);
  });

  Widget createWidgetUnderTest() {
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (context, state) => const ProfilePage(),
        ),
      ],
    );

    return MaterialApp.router(
      routerConfig: router,
      builder: (context, child) => ChangeNotifierProvider<ProfileProvider>.value(
        value: profileProvider,
        child: child,
      ),
    );
  }

  group('ProfilePage Widget Tests', () {
    testWidgets('should display loading indicator when state is loading', (tester) async {
      // Arrange
      final completer = Completer<UserProfile>();
      when(mockRepository.getProfile()).thenAnswer((_) => completer.future);

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      // Pump to trigger post-frame callback and then another to see the Loading state
      await tester.pump(); 
      await tester.pump(); 

      // Assert
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      
      // Cleanup
      completer.complete(UserProfile(id: 1, email: '', fullName: '', tier: 'FREE'));
      await tester.pumpAndSettle();
    });

    testWidgets('should display profile data when state is success', (tester) async {
      // Arrange
      final profile = UserProfile(
        id: 1,
        email: 'test@example.com',
        fullName: 'Test User',
        tier: 'TRIAL',
        daysRemaining: 15,
      );
      when(mockRepository.getProfile()).thenAnswer((_) async => profile);

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump(); // trigger initState callback
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('Test User'), findsOneWidget);
      expect(find.text('test@example.com'), findsOneWidget);
      expect(find.text('TRIAL'), findsOneWidget);
    });

    testWidgets('should display error message when state is error', (tester) async {
      // Arrange
      when(mockRepository.getProfile()).thenThrow(Exception('Network error'));

      // Act
      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump(); // trigger initState callback
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('Ocorreu um erro ao carregar seu perfil'), findsOneWidget);
      expect(find.textContaining('Network error'), findsOneWidget);
    });

    testWidgets('should display FOUNDER badge for FOUNDER_LIFETIME', (tester) async {
      final profile = UserProfile(
        id: 2,
        email: 'founder@example.com',
        fullName: 'Founder User',
        tier: 'PREMIUM',
        commercialPlan: 'FOUNDER_LIFETIME',
      );
      when(mockRepository.getProfile()).thenAnswer((_) async => profile);

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('FOUNDER'), findsOneWidget);
    });

    testWidgets('should display TRIAL PRO badge and trial banner for PRO_TRIAL', (tester) async {
      final profile = UserProfile(
        id: 3,
        email: 'trial@example.com',
        fullName: 'Trial Pro User',
        tier: 'TRIAL',
        commercialPlan: 'PRO_TRIAL',
        daysRemaining: 90,
      );
      when(mockRepository.getProfile()).thenAnswer((_) async => profile);

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('TRIAL PRO'), findsOneWidget);
      expect(find.text('Trial Expira em 90 dias'), findsOneWidget);
    });

    testWidgets('should fallback to tier when commercialPlan is empty', (tester) async {
      final profile = UserProfile(
        id: 4,
        email: 'fallback@example.com',
        fullName: 'Fallback User',
        tier: 'TRIAL',
        commercialPlan: '',
        daysRemaining: 10,
      );
      when(mockRepository.getProfile()).thenAnswer((_) async => profile);

      await tester.pumpWidget(createWidgetUnderTest());
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.text('TRIAL'), findsOneWidget);
    });

    test('ProfileProvider.reset clears profile, error and returns to idle', () async {
      final profile = UserProfile(
        id: 5,
        email: 'reset@example.com',
        fullName: 'Reset User',
        tier: 'PREMIUM',
        commercialPlan: 'FOUNDER_LIFETIME',
      );
      when(mockRepository.getProfile()).thenAnswer((_) async => profile);

      await profileProvider.fetchProfile();
      expect(profileProvider.state, ProfileState.success);
      expect(profileProvider.profile, isNotNull);

      profileProvider.reset();
      expect(profileProvider.state, ProfileState.idle);
      expect(profileProvider.profile, isNull);
      expect(profileProvider.errorMessage, isNull);
    });

    testWidgets('tapping profile_logout_button signs out, resets providers and navigates to /login', (tester) async {
      final fakeAuthRepo = _FakeAuthRepository();
      final profile = UserProfile(
        id: 6,
        email: 'logout@example.com',
        fullName: 'Logout User',
        tier: 'FREE',
      );
      when(mockRepository.getProfile()).thenAnswer((_) async => profile);

      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (context, state) => const ProfilePage(),
          ),
          GoRoute(
            path: '/login',
            builder: (context, state) => const Scaffold(body: Text('LOGIN_SCREEN')),
          ),
        ],
      );

      await tester.pumpWidget(
        MultiProvider(
          providers: [
            Provider<AuthRepository>.value(value: fakeAuthRepo),
            ChangeNotifierProvider<ProfileProvider>.value(value: profileProvider),
          ],
          child: MaterialApp.router(routerConfig: router),
        ),
      );
      await tester.pump();
      await tester.pumpAndSettle();

      final logoutFinder = find.byKey(const Key('profile_logout_button'));
      await tester.ensureVisible(logoutFinder);
      await tester.tap(logoutFinder);
      await tester.pumpAndSettle();

      expect(fakeAuthRepo.signOutCalled, isTrue);
      expect(profileProvider.state, ProfileState.idle);
      expect(profileProvider.profile, isNull);
      expect(find.text('LOGIN_SCREEN'), findsOneWidget);
    });
  });
}

class _FakeAuthRepository extends Fake implements AuthRepository {
  bool signOutCalled = false;

  @override
  Future<void> signOut() async {
    signOutCalled = true;
  }
}
