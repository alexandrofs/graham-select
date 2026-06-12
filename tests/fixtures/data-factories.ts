// Data Factories para geração de payloads de teste

export const createGrahamRecommendationDto = (overrides = {}) => ({
  ticker: 'PETR4',
  currentPrice: 30.00,
  intrinsicValue: 40.00,
  marginOfSafety: 0.33,
  currentAllocationPct: 5.00,
  targetAllocationPct: 20.00,
  allocationGap: 15.00,
  recommendationScore: 0.2580,
  ...overrides,
});

export const createValuationRequestedEvent = (overrides = {}) => ({
  userId: 'premium-user-test-id',
  timestamp: new Date().toISOString(),
  eventId: '123e4567-e89b-12d3-a456-426614174000',
  ...overrides,
});
