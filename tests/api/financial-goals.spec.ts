import { test, expect } from '@playwright/test';

/*
 * Provider Scrutiny Evidence:
 * - Handler: backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java
 * - Response type: FinancialGoalDto (backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/FinancialGoalDto.java)
 * - Status:
 *   - POST /api/v1/financial-goals: 201 Created (Authorized), 400 Bad Request (Invalid Data), 401 Unauthorized (No Token)
 *   - PUT /api/v1/financial-goals/{id}: 200 OK (Authorized), 403 Forbidden (Non-owner / NotFound), 400 Bad Request (Invalid Data), 401 Unauthorized (No Token)
 *   - GET /api/v1/financial-goals: 200 OK (Authorized), 401 Unauthorized (No Token)
 * - Fields:
 *   - id: string (UUID)
 *   - goalType: string ("PATRIMONY_TARGET" | "MONTHLY_INCOME_TARGET")
 *   - targetValue: number (BigDecimal)
 *   - monthlyContribution: number (BigDecimal)
 *   - estimatedYears: number
 *   - createdAt: string
 *   - updatedAt: string
 */

test.describe('Financial Goals API', () => {
  const USER_TOKEN = 'mock-premium-jwt-token';

  test.describe('POST /api/v1/financial-goals', () => {
    test('[P0] deve criar uma meta de patrimonio com sucesso', async ({ request }) => {
      const response = await request.post('/api/v1/financial-goals', {
        headers: {
          'Authorization': `Bearer ${USER_TOKEN}`
        },
        data: {
          goalType: 'PATRIMONY_TARGET',
          targetValue: 500000.00,
          monthlyContribution: 2000.00,
          estimatedYears: 15
        }
      });

      expect(response.status()).toBe(201);
      const body = await response.json();
      expect(body).toHaveProperty('data');
      const goal = body.data;
      expect(goal).toHaveProperty('id');
      expect(goal.goalType).toBe('PATRIMONY_TARGET');
      expect(goal.targetValue).toBe(500000.00);
      expect(goal.monthlyContribution).toBe(2000.00);
      expect(goal.estimatedYears).toBe(15);
    });

    test('[P1] deve retornar 400 Bad Request se os inputs forem invalidos', async ({ request }) => {
      const response = await request.post('/api/v1/financial-goals', {
        headers: {
          'Authorization': `Bearer ${USER_TOKEN}`
        },
        data: {
          goalType: 'PATRIMONY_TARGET',
          targetValue: -100.00,
          monthlyContribution: 0.00,
          estimatedYears: 60
        }
      });

      expect(response.status()).toBe(400);
    });

    test('[P1] deve retornar 401 Unauthorized se nao houver autenticacao', async ({ request }) => {
      const response = await request.post('/api/v1/financial-goals', {
        data: {
          goalType: 'PATRIMONY_TARGET',
          targetValue: 500000.00,
          monthlyContribution: 2000.00,
          estimatedYears: 15
        }
      });
      expect(response.status()).toBe(401);
    });
  });

  test.describe('GET /api/v1/financial-goals', () => {
    test('[P0] deve retornar a meta em uma lista se ela existir', async ({ request }) => {
      const response = await request.get('/api/v1/financial-goals', {
        headers: {
          'Authorization': `Bearer ${USER_TOKEN}`
        }
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body).toHaveProperty('data');
      expect(Array.isArray(body.data)).toBe(true);
    });
  });
});
