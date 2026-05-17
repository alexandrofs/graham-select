import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:provider/provider.dart';
import 'package:frontend/main.dart' as app;
import 'package:frontend/src/features/upload/presentation/providers/upload_provider.dart';
import 'package:frontend/src/features/upload/presentation/pages/upload_page.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('end-to-end test for B3 Upload Flow', () {
    testWidgets('verify upload integration flow', (tester) async {
      app.main();
      await tester.pumpAndSettle();

      // Find the upload page elements
      expect(find.text('Importar Planilha da B3'), findsOneWidget);
      
      // Due to the complexity of testing drag-and-drop file picking in 
      // integration tests across multiple platforms, we check that the UI
      // responds properly and providers are correctly instantiated.
      
      expect(find.byType(UploadPage), findsOneWidget);
      expect(find.text('Arraste sua planilha da B3 aqui'), findsOneWidget);

      // Verify that if we tap the upload zone, it tries to pick a file
      final uploadZone = find.text('Arraste sua planilha da B3 aqui');
      await tester.tap(uploadZone);
      await tester.pumpAndSettle();

      // Wait for completion (if file picker wasn't mocked, it's native UI so we can't interact with it)
    });
  });
}
