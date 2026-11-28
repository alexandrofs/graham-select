import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';

const landingPath = path.resolve(process.cwd(), 'frontend/web/landing.html');
const landingHtml = fs.readFileSync(landingPath, 'utf8');

function extractInlineScript(html) {
  const matches = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)];
  assert.ok(matches.length > 0, 'Expected at least one <script> block in landing.html');
  return matches[matches.length - 1][1];
}

function createElement(initialText = '') {
  const classes = new Set();
  return {
    textContent: initialText,
    classList: {
      add: (cls) => classes.add(cls),
      remove: (cls) => classes.delete(cls),
      contains: (cls) => classes.has(cls),
    },
  };
}

async function runLandingScript(fetchImpl) {
  const elements = {
    'founders-badge': createElement(),
    'founders-title': createElement(),
    'founders-desc': createElement(),
    'founders-slots-label': createElement(),
    'founders-slots-value': createElement(),
    'founders-cta': createElement(),
    'hero-primary-cta': createElement(),
    'founders-card': createElement(),
  };

  let fetchedUrl = null;
  const context = vm.createContext({
    document: {
      getElementById: (id) => elements[id] ?? null,
    },
    fetch: async (url, options) => {
      fetchedUrl = url;
      return fetchImpl(url, options);
    },
    AbortController,
    setTimeout,
    clearTimeout,
    Number,
  });

  const script = extractInlineScript(landingHtml);
  vm.runInContext(script, context);

  // Flush microtask queue so Promise chains (.then/.catch/.finally) settle.
  await new Promise((resolve) => setTimeout(resolve, 10));

  return { elements, fetchedUrl };
}

test('active founders lot updates badge, counter, and CTAs', async () => {
  const { elements, fetchedUrl } = await runLandingScript(async () => ({
    ok: true,
    status: 200,
    json: async () => ({ remainingSlots: 42, totalSlots: 250, isLoteActive: true }),
  }));

  assert.equal(fetchedUrl, '/api/v1/founders/slots');
  assert.equal(elements['founders-card'].classList.contains('is-trial'), false);
  assert.equal(elements['founders-slots-label'].textContent, 'Vagas Founders restantes');
  assert.equal(elements['founders-slots-value'].textContent, '42/250');
  assert.equal(elements['founders-cta'].textContent, 'Garantir Minha Vaga de Founder Gratuitamente');
  assert.equal(elements['hero-primary-cta'].textContent, 'Garantir Minha Vaga de Founder Gratuitamente');
});

test('sold-out founders lot transitions to 90-day Pro trial fallback', async () => {
  const { elements } = await runLandingScript(async () => ({
    ok: true,
    status: 200,
    json: async () => ({ remainingSlots: 0, totalSlots: 250, isLoteActive: true }),
  }));

  assert.equal(elements['founders-card'].classList.contains('is-trial'), true);
  assert.equal(elements['founders-badge'].textContent, 'Trial Pro de 90 dias gratuitos');
  assert.equal(elements['founders-slots-label'].textContent, 'Oferta disponível');
  assert.equal(elements['founders-slots-value'].textContent, '90 dias Pro grátis');
  assert.equal(elements['founders-cta'].textContent, 'Começar Trial Pro de 90 dias gratuitamente');
  assert.equal(elements['hero-primary-cta'].textContent, 'Começar Trial Pro de 90 dias gratuitamente');
});

test('inactive founders lot transitions to 90-day Pro trial fallback even with remainingSlots > 0', async () => {
  const { elements } = await runLandingScript(async () => ({
    ok: true,
    status: 200,
    json: async () => ({ remainingSlots: 15, totalSlots: 250, isLoteActive: false }),
  }));

  assert.equal(elements['founders-card'].classList.contains('is-trial'), true);
  assert.equal(elements['founders-slots-value'].textContent, '90 dias Pro grátis');
});

test('HTTP 500 error triggers error fallback state', async () => {
  const { elements } = await runLandingScript(async () => ({
    ok: false,
    status: 500,
    json: async () => ({}),
  }));

  assert.equal(elements['founders-slots-label'].textContent, 'Status do lote');
  assert.equal(elements['founders-slots-value'].textContent, 'Disponibilidade no login');
});

test('network failure triggers error fallback state', async () => {
  const { elements } = await runLandingScript(async () => {
    throw new Error('Network down');
  });

  assert.equal(elements['founders-slots-label'].textContent, 'Status do lote');
  assert.equal(elements['founders-slots-value'].textContent, 'Disponibilidade no login');
});

test('invalid JSON payload triggers error fallback state', async () => {
  const { elements } = await runLandingScript(async () => ({
    ok: true,
    status: 200,
    json: async () => ({ remainingSlots: null }),
  }));

  assert.equal(elements['founders-slots-label'].textContent, 'Status do lote');
  assert.equal(elements['founders-slots-value'].textContent, 'Disponibilidade no login');
});
