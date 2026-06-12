import { test, expect } from '@playwright/test';

/*
 * Provider Scrutiny Evidence:
 * - Consumer: ValuationRequestedConsumerService (backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/kafka/ValuationRequestedConsumerService.java)
 * - Tópico Consumido: valuation-requested
 * - Evento Recebido: ValuationRequestedEvent (userId: String, timestamp: String, eventId: UUID)
 * - Tópico Publicado: valuation-completed
 * - Evento Publicado: ValuationCompletedEvent (userId: String, recommendationCount: int, timestamp: String, eventId: UUID)
 * - Validações do Consumer:
 *   - Se event for null, ignora com log warning.
 *   - Se userId for null ou vazio, loga erro e interrompe.
 *   - Se houver exceção no processamento do caso de uso, captura o erro, loga no nível ERROR e relança como RuntimeException (para gestão de retentativas no Kafka).
 */

test.describe('Graham Recommendations Kafka Messages Integration', () => {
  const KAFKA_BOOTSTRAP_SERVERS = process.env.KAFKA_BOOTSTRAP_SERVERS || 'localhost:9092';
  const VALUATION_REQUESTED_TOPIC = 'valuation-requested';
  const VALUATION_COMPLETED_TOPIC = 'valuation-completed';

  test('[P0] deve processar evento valuation-requested com sucesso e publicar valuation-completed', async ({ request }) => {
    const testUserId = 'premium-user-test-id';
    const eventPayload = {
      userId: testUserId,
      timestamp: new Date().toISOString(),
      eventId: '123e4567-e89b-12d3-a456-426614174000'
    };

    const publishResponse = await request.post('/api/test/kafka/publish', {
      data: {
        topic: KAFKA_BOOTSTRAP_SERVERS,
        key: testUserId,
        value: eventPayload
      }
    });
    expect(publishResponse.status()).toBe(200);

    const consumeResponse = await request.get(`/api/test/kafka/consume?topic=${VALUATION_COMPLETED_TOPIC}&key=${testUserId}`);
    expect(consumeResponse.status()).toBe(200);
    
    const completedEvents = await consumeResponse.json();
    expect(completedEvents.length).toBeGreaterThan(0);
    
    const completedEvent = completedEvents[0];
    expect(completedEvent).toMatchObject({
      userId: testUserId,
      recommendationCount: expect.any(Number),
      timestamp: expect.any(String),
      eventId: expect.any(String)
    });
  });

  test('[P1] deve ignorar mensagens com userId nulo ou vazio e registrar log de erro', async ({ request }) => {
    const invalidPayload = {
      userId: '',
      timestamp: new Date().toISOString(),
      eventId: '123e4567-e89b-12d3-a456-426614174001'
    };

    const publishResponse = await request.post('/api/test/kafka/publish', {
      data: {
        topic: KAFKA_BOOTSTRAP_SERVERS,
        key: 'invalid-key',
        value: invalidPayload
      }
    });
    expect(publishResponse.status()).toBe(200);

    const consumeResponse = await request.get(`/api/test/kafka/consume?topic=${VALUATION_COMPLETED_TOPIC}&key=invalid-key`);
    expect(consumeResponse.status()).toBe(200);
    
    const completedEvents = await consumeResponse.json();
    expect(completedEvents.length).toBe(0);
  });

  test('[P2] deve propagar exceção de processamento ao contêiner Kafka caso o caso de uso falhe', async ({ request }) => {
    const errorUserId = 'error-trigger-user-id';
    const eventPayload = {
      userId: errorUserId,
      timestamp: new Date().toISOString(),
      eventId: '123e4567-e89b-12d3-a456-426614174002'
    };

    const publishResponse = await request.post('/api/test/kafka/publish', {
      data: {
        topic: VALUATION_REQUESTED_TOPIC,
        key: errorUserId,
        value: eventPayload
      }
    });
    expect(publishResponse.status()).toBe(200);

    const logsResponse = await request.get(`/api/test/logs?level=ERROR&message=Failed to process valuation requested event`);
    expect(logsResponse.status()).toBe(200);
    const logs = await logsResponse.json();
    expect(logs.length).toBeGreaterThan(0);
  });
});
