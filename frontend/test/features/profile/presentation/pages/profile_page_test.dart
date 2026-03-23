import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
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
  });
}
