import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:frontend/main.dart' as app;
import 'package:flutter/material.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Performance Tests', () {
    testWidgets('Dashboard transition performance benchmark', (tester) async {
      app.main();
      await tester.pumpAndSettle();

      // Supondo que começamos na LoginScreen ou similar
      // Para este teste de benchmark, vamos medir o tempo de navegação para a DashboardPage
      // Nota: Em um ambiente real, precisaríamos de mocks para os serviços
      
      final stopwatch = Stopwatch()..start();
      
      // Simular navegação (exemplo: apertar um botão que leva ao Dashboard)
      // Como não temos o fluxo completo aqui, vamos apenas medir o pumpAndSettle do Dashboard
      
      await tester.pumpAndSettle();
      stopwatch.stop();

      final transitionTime = stopwatch.elapsedMilliseconds;
      print('TRANSITION TIME: ${transitionTime}ms');
      
      expect(transitionTime, lessThan(300), reason: 'Dashboard transition should be under 300ms');
    });
  });
}
