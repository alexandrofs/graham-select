import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/admin/data/datasources/admin_remote_datasource.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_dlq_message.dart';
import 'package:frontend/src/features/admin/domain/entities/admin_observability.dart';
import 'package:frontend/src/features/admin/domain/repositories/admin_repository.dart';
import 'package:frontend/src/features/admin/presentation/providers/admin_dlq_provider.dart';

class _MockAdminRepository implements AdminRepository {
  List<AdminDlqMessage> dlqFailuresResult = [];
  Object? dlqFailuresError;

  int? lastReprocessedId;
  Object? reprocessError;

  List<int>? lastBatchIds;
  Map<String, dynamic> batchResult = {};
  Object? batchError;

  @override
  Future<AdminObservability> getObservabilityMetrics() async {
    throw UnimplementedError();
  }

  @override
  Future<List<AdminDlqMessage>> getDlqFailures({String status = 'PENDING'}) async {
    if (dlqFailuresError != null) throw dlqFailuresError!;
    return dlqFailuresResult;
  }

  @override
  Future<void> reprocessDlqMessage(int id) async {
    lastReprocessedId = id;
    if (reprocessError != null) throw reprocessError!;
  }

  @override
  Future<Map<String, dynamic>> reprocessBatchDlqMessages(List<int> ids) async {
    lastBatchIds = ids;
    if (batchError != null) throw batchError!;
    return batchResult;
  }
}

void main() {
  late _MockAdminRepository mockRepo;
  late AdminDlqProvider provider;

  final sampleItem1 = AdminDlqMessage(
    id: 1,
    eventId: 'ev-1',
    correlationId: 'corr-1',
    anonymizedUserId: 'usr_***1111',
    fileName: 'extrato1.xlsx',
    lineNumber: 10,
    errorMessage: 'Erro 1',
    originalTopic: 'trade-extracted',
    status: 'PENDING',
    timestamp: DateTime.utc(2026, 6, 13, 10, 0),
  );

  final sampleItem2 = AdminDlqMessage(
    id: 2,
    eventId: 'ev-2',
    correlationId: 'corr-2',
    anonymizedUserId: 'usr_***2222',
    fileName: 'extrato2.xlsx',
    lineNumber: 20,
    errorMessage: 'Erro 2',
    originalTopic: 'trade-extracted',
    status: 'PENDING',
    timestamp: DateTime.utc(2026, 6, 13, 10, 5),
  );

  setUp(() {
    mockRepo = _MockAdminRepository();
    provider = AdminDlqProvider(repository: mockRepo);
  });

  group('AdminDlqProvider', () {
    test('inicia com status initial e listas vazias', () {
      expect(provider.status, AdminDlqState.initial);
      expect(provider.failures, isEmpty);
      expect(provider.selectedIds, isEmpty);
      expect(provider.hasSelection, isFalse);
    });

    test('carrega falhas com sucesso transicionando para loaded', () async {
      mockRepo.dlqFailuresResult = [sampleItem1, sampleItem2];

      await provider.loadFailures();

      expect(provider.status, AdminDlqState.loaded);
      expect(provider.failures.length, 2);
      expect(provider.errorMessage, isNull);
    });

    test('trata AdminAccessDeniedException definindo status accessDenied', () async {
      mockRepo.dlqFailuresError = const AdminAccessDeniedException();

      await provider.loadFailures();

      expect(provider.status, AdminDlqState.accessDenied);
      expect(provider.errorMessage, contains('Acesso restrito'));
    });

    test('trata erro genérico definindo status error', () async {
      mockRepo.dlqFailuresError = Exception('Falha de rede');

      await provider.loadFailures();

      expect(provider.status, AdminDlqState.error);
      expect(provider.errorMessage, contains('Falha de rede'));
    });

    test('gerencia seleção individual e coletiva corretamente', () async {
      mockRepo.dlqFailuresResult = [sampleItem1, sampleItem2];
      await provider.loadFailures();

      provider.toggleSelection(1);
      expect(provider.isSelected(1), isTrue);
      expect(provider.selectedCount, 1);
      expect(provider.isAllSelected, isFalse);

      provider.selectAll();
      expect(provider.isAllSelected, isTrue);
      expect(provider.selectedCount, 2);

      provider.selectAll();
      expect(provider.hasSelection, isFalse);
      expect(provider.selectedCount, 0);
    });

    test('reprocessItem executa com sucesso e remove item da lista', () async {
      mockRepo.dlqFailuresResult = [sampleItem1, sampleItem2];
      await provider.loadFailures();

      final success = await provider.reprocessItem(1);

      expect(success, isTrue);
      expect(mockRepo.lastReprocessedId, 1);
      expect(provider.failures.any((f) => f.id == 1), isFalse);
      expect(provider.failures.length, 1);
    });

    test('reprocessSelected executa em lote e remove itens resolvidos', () async {
      mockRepo.dlqFailuresResult = [sampleItem1, sampleItem2];
      await provider.loadFailures();

      provider.selectAll();
      mockRepo.batchResult = {
        'totalProcessed': 2,
        'successCount': 2,
        'failureCount': 0,
        'details': [
          {'id': 1, 'status': 'RESOLVED'},
          {'id': 2, 'status': 'RESOLVED'},
        ],
      };

      final success = await provider.reprocessSelected();

      expect(success, isTrue);
      expect(mockRepo.lastBatchIds, containsAll([1, 2]));
      expect(provider.failures, isEmpty);
      expect(provider.selectedIds, isEmpty);
    });

    test('não lança exceção ao invocar métodos após dispose', () async {
      mockRepo.dlqFailuresResult = [sampleItem1];
      provider.dispose();

      // Não deve quebrar
      await provider.loadFailures();
      expect(true, isTrue);
    });
  });
}
