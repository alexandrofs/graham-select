import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/admin/data/models/admin_dlq_message_model.dart';

void main() {
  group('AdminDlqMessageModel', () {
    test('deve deserializar corretamente a partir de JSON com valores válidos', () {
      final json = {
        'id': 1,
        'eventId': 'ev-123',
        'correlationId': 'corr-456',
        'anonymizedUserId': 'usr_***a1b2',
        'fileName': 'extrato_petr4.xlsx',
        'lineNumber': 15,
        'errorMessage': 'Erro de formato',
        'originalTopic': 'trade-extracted',
        'status': 'PENDING',
        'timestamp': '2026-06-13T12:00:00.000Z',
        'resolvedAt': null,
      };

      final model = AdminDlqMessageModel.fromJson(json);

      expect(model.id, 1);
      expect(model.eventId, 'ev-123');
      expect(model.correlationId, 'corr-456');
      expect(model.anonymizedUserId, 'usr_***a1b2');
      expect(model.fileName, 'extrato_petr4.xlsx');
      expect(model.lineNumber, 15);
      expect(model.errorMessage, 'Erro de formato');
      expect(model.originalTopic, 'trade-extracted');
      expect(model.status, 'PENDING');
      expect(model.isPending, isTrue);
      expect(model.isResolved, isFalse);
      expect(model.timestamp.isUtc, isTrue);
      expect(model.resolvedAt, isNull);
    });

    test('deve serializar para JSON mantendo formato esperado', () {
      final model = AdminDlqMessageModel(
        id: 2,
        eventId: 'ev-999',
        correlationId: 'corr-888',
        anonymizedUserId: 'usr_***c3d4',
        fileName: 'extrato.xlsx',
        lineNumber: 5,
        errorMessage: 'Preço inválido',
        originalTopic: 'trade-extracted',
        status: 'RESOLVED',
        timestamp: DateTime.utc(2026, 6, 13, 10, 0),
        resolvedAt: DateTime.utc(2026, 6, 13, 10, 30),
      );

      final json = model.toJson();

      expect(json['id'], 2);
      expect(json['status'], 'RESOLVED');
      expect(json['resolvedAt'], isNotNull);
    });

    test('deve deserializar lista a partir de envelope { "data": [...] }', () {
      final envelope = {
        'data': [
          {
            'id': 10,
            'eventId': 'ev-10',
            'correlationId': 'corr-10',
            'anonymizedUserId': 'usr_***1111',
            'fileName': 'extrato1.xlsx',
            'errorMessage': 'Erro 1',
            'originalTopic': 'trade-extracted',
            'status': 'PENDING',
            'timestamp': '2026-06-13T12:00:00.000Z',
          },
          {
            'id': 20,
            'eventId': 'ev-20',
            'correlationId': 'corr-20',
            'anonymizedUserId': 'usr_***2222',
            'fileName': 'extrato2.xlsx',
            'errorMessage': 'Erro 2',
            'originalTopic': 'trade-extracted',
            'status': 'PENDING',
            'timestamp': '2026-06-13T12:05:00.000Z',
          }
        ]
      };

      final list = AdminDlqMessageModel.fromJsonList(envelope);
      expect(list.length, 2);
      expect(list[0].id, 10);
      expect(list[1].id, 20);
    });
  });
}
