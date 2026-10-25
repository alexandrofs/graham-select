import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/features/admin/data/datasources/admin_remote_datasource.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_dlq_message.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_observability.dart';
import 'package:frontend/src/features/admin/domain/repositories/admin_repository.dart';
import 'package:frontend/src/features/admin/presentation/pages/admin_dlq_page.dart';
import 'package:frontend/src/features/admin/presentation/providers/admin_dlq_provider.dart';
import 'package:provider/provider.dart';

class _ControllableAdminRepository implements AdminRepository {
  Completer<List<AdminDlqMessage>>? dlqCompleter;
  List<AdminDlqMessage>? immediateDlqResult;
  Object? immediateDlqError;

  int? reprocessedId;
  List<int>? batchReprocessedIds;

  @override
  Future<AdminObservability> getObservabilityMetrics() async {
    throw UnimplementedError();
  }

  @override
  Future<List<AdminDlqMessage>> getDlqFailures({String status = 'PENDING'}) {
    if (dlqCompleter != null) {
      return dlqCompleter!.future;
    }
    if (immediateDlqError != null) {
      return Future.error(immediateDlqError!);
    }
    return Future.value(immediateDlqResult ?? []);
  }

  @override
  Future<void> reprocessDlqMessage(int id) async {
    reprocessedId = id;
  }

  @override
  Future<Map<String, dynamic>> reprocessBatchDlqMessages(List<int> ids) async {
    batchReprocessedIds = ids;
    return {
      'totalProcessed': ids.length,
      'successCount': ids.length,
      'failureCount': 0,
      'details': ids.map((id) => {'id': id, 'status': 'RESOLVED'}).toList(),
    };
  }
}

void main() {
  final sampleItem = AdminDlqMessage(
    id: 101,
    eventId: 'ev-101',
    correlationId: 'corr-101',
    anonymizedUserId: 'usr_***abcd',
    fileName: 'extrato_janeiro.xlsx',
    lineNumber: 12,
    errorMessage: 'Formato numérico inválido na linha 12',
    originalTopic: 'trade-extracted',
    status: 'PENDING',
    timestamp: DateTime.utc(2026, 1, 15, 14, 30),
  );

  Widget createWidgetUnderTest(AdminDlqProvider provider) {
    return MaterialApp(
      theme: AppTheme.darkTheme,
      home: ChangeNotifierProvider<AdminDlqProvider>.value(
        value: provider,
        child: const Scaffold(body: AdminDlqPage()),
      ),
    );
  }

  group('AdminDlqPage Widget Tests', () {
    testWidgets('exibe Skeleton Screen durante o carregamento inicial', (tester) async {
      final repo = _ControllableAdminRepository();
      repo.dlqCompleter = Completer<List<AdminDlqMessage>>();
      final provider = AdminDlqProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pump();

      expect(find.byKey(const Key('admin_dlq_skeleton')), findsOneWidget);
    });

    testWidgets('exibe estado de Acesso Restrito em caso de 403 Forbidden', (tester) async {
      final repo = _ControllableAdminRepository();
      repo.immediateDlqError = const AdminAccessDeniedException();
      final provider = AdminDlqProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_dlq_access_denied_state')), findsOneWidget);
      expect(find.text('Acesso Restrito a Administradores'), findsOneWidget);
      expect(find.text('Voltar ao Dashboard'), findsOneWidget);
    });

    testWidgets('exibe estado vazio quando não há mensagens na DLQ', (tester) async {
      final repo = _ControllableAdminRepository();
      repo.immediateDlqResult = [];
      final provider = AdminDlqProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('admin_dlq_empty_state')), findsOneWidget);
      expect(find.text('Nenhuma falha de ingestão pendente'), findsOneWidget);
    });

    testWidgets('exibe lista de falhas com Arquivo, Erro Reportado e Usuário', (tester) async {
      final repo = _ControllableAdminRepository();
      repo.immediateDlqResult = [sampleItem];
      final provider = AdminDlqProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      expect(find.text('extrato_janeiro.xlsx'), findsOneWidget);
      expect(find.text('usr_***abcd'), findsOneWidget);
      expect(find.text('Formato numérico inválido na linha 12'), findsOneWidget);
      expect(find.byKey(const Key('admin_dlq_reprocess_button_101')), findsOneWidget);
    });

    testWidgets('dispara reprocessamento individual e remove item da tela', (tester) async {
      final repo = _ControllableAdminRepository();
      repo.immediateDlqResult = [sampleItem];
      final provider = AdminDlqProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      final reprocessButton = find.byKey(const Key('admin_dlq_reprocess_button_101'));
      expect(reprocessButton, findsOneWidget);

      await tester.tap(reprocessButton);
      await tester.pumpAndSettle();

      expect(repo.reprocessedId, 101);
      expect(find.text('Item reprocessado com sucesso!'), findsOneWidget);
      expect(find.byKey(const Key('admin_dlq_empty_state')), findsOneWidget);
    });

    testWidgets('dispara reprocessamento em lote ao selecionar itens', (tester) async {
      final repo = _ControllableAdminRepository();
      repo.immediateDlqResult = [sampleItem];
      final provider = AdminDlqProvider(repository: repo);

      await tester.pumpWidget(createWidgetUnderTest(provider));
      await tester.pumpAndSettle();

      final checkbox = find.byKey(const Key('admin_dlq_item_checkbox_101'));
      expect(checkbox, findsOneWidget);

      await tester.tap(checkbox);
      await tester.pumpAndSettle();

      final batchButton = find.byKey(const Key('admin_dlq_reprocess_batch_button'));
      expect(batchButton, findsOneWidget);

      await tester.tap(batchButton);
      await tester.pumpAndSettle();

      expect(repo.batchReprocessedIds, contains(101));
      expect(find.text('Itens selecionados reprocessados com sucesso!'), findsOneWidget);
      expect(find.byKey(const Key('admin_dlq_empty_state')), findsOneWidget);
    });
  });
}
