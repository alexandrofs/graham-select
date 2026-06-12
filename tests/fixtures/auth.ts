import { test as base } from '@playwright/test';

// Fixture para autenticação nos testes de API
export const test = base.extend({
  premiumAuthToken: async ({}, use) => {
    // Retorna o token mockado de usuário Premium
    const token = 'mock-premium-jwt-token';
    await use(token);
  },

  basicAuthToken: async ({}, use) => {
    // Retorna o token mockado de usuário básico/gratuito
    const token = 'mock-basic-jwt-token';
    await use(token);
  },
});

export { expect } from '@playwright/test';
