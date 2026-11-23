import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/widgets/main_layout.dart';
import 'package:frontend/src/features/profile/data/models/user_profile_model.dart';
import 'package:frontend/src/features/profile/data/repositories/profile_repository.dart';
import 'package:frontend/src/features/profile/presentation/providers/profile_provider.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

class _StubProfileRepository implements ProfileRepository {
  @override
  Future<UserProfile> getProfile() async => UserProfile(
        id: 1,
        email: 'user@kerno.afsdigital.com.br',
        fullName: 'User',
        tier: 'FREE',
        role: 'USER',
      );

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  testWidgets('MainLayout exibe logotipo e marca Kerno no menu lateral', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final router = GoRouter(
      initialLocation: '/dashboard',
      routes: [
        ShellRoute(
          builder: (context, state, child) => MainLayout(child: child),
          routes: [
            GoRoute(
              path: '/dashboard',
              builder: (_, _) => const SizedBox(),
            ),
          ],
        ),
      ],
    );

    await tester.pumpWidget(
      ChangeNotifierProvider<ProfileProvider>(
        create: (_) => ProfileProvider(_StubProfileRepository()),
        child: MaterialApp.router(routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Kerno'), findsOneWidget);
    expect(
      find.byWidgetPredicate(
        (widget) =>
            widget is Image &&
            widget.image is AssetImage &&
            (widget.image as AssetImage).assetName == 'assets/images/logo.png',
      ),
      findsOneWidget,
    );
    expect(find.textContaining('Graham'), findsNothing);
  });
}
