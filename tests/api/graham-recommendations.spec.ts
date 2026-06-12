import { test, expect } from '@playwright/test';

/*
 * Provider Scrutiny Evidence:
 * - Handler: backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsController.java
 * - Response type: GrahamRecommendationDto (backend/common/src/main/java/afsdigital/grahamselect/valuation/application/dto/GrahamRecommendationDto.java)
 * - Status:
 *   - GET /api/v1/graham-recommendations: 200 OK (Premium), 403 Forbidden (Non-Premium), 401 Unauthorized (No Token)
 *   - POST /api/v1/graham-recommendations/trigger: 202 Accepted (Premium), 429 Too Many Requests (Cooldown), 403 Forbidden (Non-Premium), 401 Unauthorized (No Token)
 * - Fields:
 *   - ticker: string (e.g. "PETR4")
 *   - currentPrice: number (BigDecimal)
 *   - intrinsicValue: number (BigDecimal)
 *   - marginOfSafety: number (BigDecimal)
 *   - currentAllocationPct: number (BigDecimal)
 *   - targetAllocationPct: number (BigDecimal)
 *   - allocationGap: number (BigDecimal)
 *   - recommendationScore: number (BigDecimal)
 */

test.describe('Graham Recommendations API', () => {
  const PREMIUM_TOKEN = 'mock-premium-jwt-token';
  const BASIC_TOKEN = 'mock-basic-jwt-token';

  test.describe('GET /api/v1/graham-recommendations', () => {
    test('[P0] deve retornar 200 OK com a lista de recomendações para usuário Premium', async ({ request }) => {
      const response = await request.get('/api/v1/graham-recommendations', {
        headers: {
          'Authorization': `Bearer ${PREMIUM_TOKEN}`
        }
      });

      expect(response.status()).toBe(200);
      const recommendations = await response.json();
      expect(Array.isArray(recommendations)).toBe(true);

      if (recommendations.length > 0) {
        const item = recommendations[0];
        expect(item).toHaveProperty('ticker');
        expect(item).toHaveProperty('currentPrice');
        expect(item).toHaveProperty('intrinsicValue');
        expect(item).toHaveProperty('marginOfSafety');
        expect(item).toHaveProperty('currentAllocationPct');
        expect(item).toHaveProperty('targetAllocationPct');
        expect(item).toHaveProperty('allocationGap');
        expect(item).toHaveProperty('recommendationScore');
        
        expect(typeof item.ticker).toBe('string');
        expect(typeof item.currentPrice).toBe('number');
        expect(typeof item.intrinsicValue).toBe('number');
        expect(typeof item.marginOfSafety).toBe('number');
        expect(typeof item.currentAllocationPct).toBe('number');
        expect(typeof item.targetAllocationPct).toBe('number');
        expect(typeof item.allocationGap).toBe('number');
        expect(typeof item.recommendationScore).toBe('number');
      }
    });

    test('[P1] deve retornar 403 Forbidden quando usuário básico/gratuito tentar acessar', async ({ request }) => {
      const response = await request.get('/api/v1/graham-recommendations', {
        headers: {
          'Authorization': `Bearer ${BASIC_TOKEN}`
        }
      });

      expect(response.status()).toBe(403);
    });

    test('[P1] deve retornar 401 Unauthorized quando não for fornecido token de autenticação', async ({ request }) => {
      const response = await request.get('/api/v1/graham-recommendations');
      expect(response.status()).toBe(401);
    });
  });

  test.describe('POST /api/v1/graham-recommendations/trigger', () => {
    test('[P0] deve retornar 202 Accepted quando usuário Premium disparar cálculo', async ({ request }) => {
      const response = await request.post('/api/v1/graham-recommendations/trigger', {
        headers: {
          'Authorization': `Bearer ${PREMIUM_TOKEN}`
        }
      });

      expect(response.status()).toBe(202);
    });

    test('[P0] deve impor cooldown de 10s e retornar 429 Too Many Requests ao disparar rapidamente', async ({ request }) => {
      // Primeira requisição - deve ser aceita
      const firstResponse = await request.post('/api/v1/graham-recommendations/trigger', {
        headers: {
          'Authorization': `Bearer ${PREMIUM_TOKEN}`
        }
      });
      expect(firstResponse.status()).toBe(202);

      // Segunda requisição imediata - deve falhar com 429
      const secondResponse = await request.post('/api/v1/graham-recommendations/trigger', {
        headers: {
          'Authorization': `Bearer ${PREMIUM_TOKEN}`
        }
      });
      expect(secondResponse.status()).toBe(429);
    });

    test('[P1] deve retornar 403 Forbidden quando usuário básico tentar disparar cálculo', async ({ request }) => {
      const response = await request.post('/api/v1/graham-recommendations/trigger', {
        headers: {
          'Authorization': `Bearer ${BASIC_TOKEN}`
        }
      });

      expect(response.status()).toBe(403);
    });

    test('[P1] deve retornar 401 Unauthorized ao disparar cálculo sem token', async ({ request }) => {
      const response = await request.post('/api/v1/graham-recommendations/trigger');
      expect(response.status()).toBe(401);
    });
  });
});
