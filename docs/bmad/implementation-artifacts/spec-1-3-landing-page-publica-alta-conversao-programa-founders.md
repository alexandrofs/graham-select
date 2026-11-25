---
title: 'Story 1.3: Landing Page Pública de Alta Conversão com Destaque do Programa Founders'
type: 'feature'
created: '2026-11-24'
status: 'done'
baseline_commit: '6696ad11b283d176f977b610d3990ec41cec6f31'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/bmad/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A rota pública raiz `/` ainda carrega o bundle completo do Flutter Web (`CanvasKit`), prejudicando o *First Contentful Paint* (`FCP < 1.2s`), não possui página dedicada para `/terms` e `/privacy`, não expõe o contador dinâmico de vagas do Programa Founders (`/api/v1/founders/slots`) nem isola rotas privadas (`/app/*`, `/dashboard/*`, `/api/*`) via `robots.txt` e diretivas SEO.

**Approach:** Servir `/`, `/terms` e `/privacy` como páginas HTML estáticas leves no Nginx (desacopladas do bundle Flutter Web) com a narrativa Kerno, previews visuais do GPS de Aporte, Diagrama do Cerrado e Kerno AI, contador dinâmico do Programa Founders e disclaimer CVM 19/20, além de configurar `robots.txt`, cabeçalhos `X-Robots-Tag` e liberar `/docs` sem autenticação no `GoRouter`.

## Boundaries & Constraints

**Always:**
- Servir `/` (`landing.html`), `/terms` (`terms.html`) e `/privacy` (`privacy.html`) como HTML/CSS/JS estático leve sem dependências externas bloqueantes (fontes de sistema com fallback seguro + CSS inline/leve), garantindo `FCP < 1.2s`, layout responsivo Mobile/Desktop e contraste WCAG 2.1 AA/AAA (paleta Kerno Light-First `#F8FAFC`, `#FFFFFF`, `#0F172A`, `#4F46E5`, Ouro Founder `#92400E`/`#FEF3C7`/`#F59E0B`).
- Exibir na Landing Page (`/`): narrativa *"Do caos das planilhas ao núcleo inteligente das suas finanças"*, previews visuais do **GPS de Aporte** (simulação em 25s, Margem de Graham + bônus `1.30x`), **Diagrama do Cerrado** e **Kerno AI**, além do disclaimer regulatório CVM 19/20 no rodapé e junto às demonstrações (linguagem estritamente descritiva/matemática, sem imperativos de compra/venda).
- Consultar `GET /api/v1/founders/slots` de forma assíncrona (sem bloquear renderização inicial): quando `remainingSlots > 0` (`isLoteActive: true`), exibir selo *"Seja um Membro Founder — Acesso Pro Vitalício Gratuito"*, contador de vagas restantes e CTA *"Garantir Minha Vaga de Founder Gratuitamente"* apontando para `/login`; quando esgotado (`remainingSlots === 0`), transicionar transparentemente para a oferta de **Trial Pro de 90 dias** sem cartão de crédito.
- Configurar `robots.txt` e meta tags / `X-Robots-Tag` garantindo `index, follow` exclusivamente em `/`, `/terms`, `/privacy`, `/docs` e `noindex, nofollow` + `Disallow` em `/app/*`, `/dashboard/*`, `/api/*`.
- Incluir em `/terms` o enquadramento como ferramenta matemática parametrizada sob CVM 19/20 e em `/privacy` os direitos LGPD (portabilidade, Direito ao Esquecimento, sanitização de 0 bytes PII na IA) e retenção legal de 6 meses dos registros de conexão (Art. 15 da Lei 12.965/2014 — Marco Civil).
- Preservar todos os ativos da Story 1.1 (`logo.svg`, `favicon.png`, `og-image.png`), `/healthz` no `nginx.conf`, porta `8080` e rotas autenticadas do `GoRouter`.

**Never:**
- Carregar `flutter_bootstrap.js` ou `CanvasKit` na rota raiz `/`, `/terms` ou `/privacy`.
- Mencionar a marca legada *"Graham Select"* como nome da plataforma (preservar *"Benjamin Graham"* apenas na metodologia).
- Alterar `helm/graham-select/templates/ingress.yaml`, `frontend/Dockerfile` ou migrations Liquibase.
- Antecipar lógica de aceite/persistência OAuth2 da Story 1.4.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Acesso à Landing Page `/` | `GET /` no Nginx | Retorna `landing.html` (`200 OK`, `index, follow`) sem carregar Flutter Web; exibe proposta Kerno, previews e disclaimer CVM | N/A |
| Lote Founders Ativo | `GET /api/v1/founders/slots` -> `remainingSlots: 42, isLoteActive: true` | Banner Ouro Founder *"Seja um Membro Founder — Acesso Pro Vitalício Gratuito"* com contador `42/250` e CTA *"Garantir Minha Vaga de Founder Gratuitamente"* -> `/login` | Timeout/erro HTTP mantém fallback não-bloqueante com CTA para `/login` |
| Lote Founders Esgotado | `GET /api/v1/founders/slots` -> `remainingSlots: 0, isLoteActive: false` | Seção transiciona para *"Trial Pro de 90 dias gratuitos"* sem cartão de crédito e CTA para `/login` | N/A |
| Rotas Institucionais | `GET /terms`, `GET /privacy`, `GET /docs` | `/terms` e `/privacy` servem HTML estático (`200 OK`, `index, follow`) com CVM 19/20, LGPD e Marco Civil Art. 15; `/docs` acessível sem login no `GoRouter` | N/A |
| Isolamento SEO | `GET /robots.txt`, `GET /dashboard/*`, `GET /app/*` | `robots.txt` permite `/`, `/terms`, `/privacy`, `/docs` e bloqueia `/app/`, `/dashboard/`, `/api/`; Nginx retorna `X-Robots-Tag: noindex, nofollow` em `/app/*` e `/dashboard/*` | N/A |

</frozen-after-approval>

## Code Map

- `frontend/web/landing.html` (novo) -- Landing Page pública estática (`/`) com SEO `index, follow`, OpenGraph Kerno, CSS Light-First inline, previews (GPS de Aporte, Diagrama do Cerrado, Kerno AI), fetch assíncrono a `/api/v1/founders/slots` e disclaimers CVM 19/20.
- `frontend/web/terms.html` (novo) -- Página estática de Termos de Uso (`/terms`, `index, follow`) com enquadramento CVM 19/20.
- `frontend/web/privacy.html` (novo) -- Página estática de Política de Privacidade (`/privacy`, `index, follow`) com LGPD, sanitização IA e retenção de 6 meses (Art. 15 Marco Civil).
- `frontend/web/robots.txt` (novo) -- Diretivas `Allow` para `/`, `/terms`, `/privacy`, `/docs` e `Disallow` para `/app/`, `/dashboard/`, `/api/`.
- `frontend/nginx.conf` -- Mapear rotas exatas `= /`, `= /terms`, `= /privacy`, `= /robots.txt`, cabeçalhos `X-Robots-Tag` (`index, follow` em `/docs`; `noindex, nofollow` em `/app/` e `/dashboard/`) mantendo `/healthz` e fallback SPA `/index.html`.
- `frontend/lib/main.dart` (`L323-341`) -- Incluir `/docs` na allowlist não autenticada do `GoRouter.redirect`.
- `frontend/test/` -- Novos testes verificando conteúdo/SEO/CVM/LGPD das páginas estáticas, `robots.txt`, `nginx.conf` e roteamento público de `/docs` no `GoRouter`.

## Tasks & Acceptance

**Execution:**
- [x] `frontend/web/landing.html` -- Criar Landing Page estática Light-First Kerno com previews (GPS de Aporte, Diagrama do Cerrado, Kerno AI), contador dinâmico `/api/v1/founders/slots`, fallback resiliente e disclaimers CVM -- Garantir `FCP < 1.2s` e alta conversão.
- [x] `frontend/web/terms.html`, `frontend/web/privacy.html` -- Criar páginas institucionais estáticas com CVM 19/20, LGPD e retenção de 6 meses (Marco Civil Art. 15) -- Atender conformidade regulatória e SEO público.
- [x] `frontend/web/robots.txt`, `frontend/nginx.conf` -- Configurar `robots.txt`, rotas estáticas exatas (`/`, `/terms`, `/privacy`), `X-Robots-Tag` para rotas públicas vs `/app/*` e `/dashboard/*` -- Garantir isolamento SEO mantendo SPA e `/healthz`.
- [x] `frontend/lib/main.dart` -- Permitir acesso não autenticado à rota `/docs` no `GoRouter.redirect` -- Tornar `/docs` acessível a visitantes públicos.
- [x] `frontend/test/kerno_landing_seo_test.dart`, `frontend/test/main_router_public_docs_test.dart` -- Testar estrutura HTML, ausência de marca legada, meta tags/robots, disclaimers CVM/LGPD/Marco Civil, regras do `nginx.conf` e navegação pública para `/docs` -- Cobrir matriz I/O e regressões.

**Acceptance Criteria:**
- Given um visitante acessando `/` sem sessão autenticada, when a resposta HTTP é servida pelo Nginx, then `landing.html` é entregue diretamente sem carregar o bundle Flutter Web, exibindo narrativa Kerno, previews visuais, disclaimer CVM e banner dinâmico do Programa Founders.
- Given um crawler indexando `kerno.afsdigital.com.br`, when consulta `robots.txt` e os cabeçalhos/meta tags das rotas, then `/`, `/terms`, `/privacy` e `/docs` permitem indexação (`index, follow`) enquanto `/app/*`, `/dashboard/*` e `/api/*` aplicam bloqueio (`Disallow` / `noindex, nofollow`).

## Implementation Notes

## Spec Change Log

## Review Triage Log

| Origem | Achado | Veredicto | Rota | Evidência |
|---|---|---|---|---|
| verification-gap, blind-hunter | Script JS de `/api/v1/founders/slots` em `landing.html` testado apenas por substring estática | `medium` | `defer` | `kerno_landing_seo_test.dart` valida HTML/JS estático; `frontend/` não possui runner JS/Playwright para DOM sem acoplar `flutter test` ao Node. |
| verification-gap | `nginx.conf` e cabeçalhos `X-Robots-Tag` não validados contra processo `nginx` real | `medium` | `defer` | Daemon Docker local indisponível (`dockerDesktopLinuxEngine` ausente); validar `nginx -t` e smoke HTTP em CI/Docker. |
| verification-gap, blind-hunter | Botão voltar em `DocsScreen` (`docs_screen.dart:24`) chama `context.pop()` sem checar `context.canPop()` | `high` | `patch` | Acesso direto via `<a href="/docs">` inicia `GoRouter` com pilha vazia e lança `GoError` ao tocar em voltar. |
| blind-hunter, edge-case-hunter | `nginx.conf` exige `/terms` e `/privacy` sem barra final; `main.dart:330` libera `/docs/*` sem rota correspondente | `medium` | `patch` | `/terms/` e `/privacy/` caem em `index.html` redirecionando para `/login`; `/docs/foo` abre tela de erro do `GoRouter`. |
| blind-hunter | Rotas privadas SPA em `location /` do `nginx.conf` não recebem `X-Robots-Tag: noindex, nofollow` | `low` | `patch` | Rotas autenticadas além de `/app` e `/dashboard` herdam `location /` sem `noindex`; adicionar `X-Robots-Tag "noindex, nofollow" always` em `location /`. |
| blind-hunter | `<link rel="canonical">` em `web/index.html:24` aponta para `/` mesmo em `/docs` | `low` | `reject` | `index.html` é único para toda a SPA Flutter; alterar canonical por rota exigiria SSR ou mutação JS fora do escopo. |
| blind-hunter, edge-case-hunter | Fallback silencioso em `landing.html:1038-1051` mantém `250/250` em falha/timeout e não valida `Number.isFinite` / `totalSlots <= 0` | `medium` | `patch` | `.catch()` é no-op e payload inválido pode exibir `NaN/250` ou `250/250` falso quando o lote estiver esgotado. |
| blind-hunter | Usuários autenticados acessando `/` recebem `landing.html` em vez de `HomePage` | `false` | `reject` | Comportamento exigido pelo `<frozen-after-approval>`: `/` deve servir `landing.html` estática sem carregar Flutter Web. |
| blind-hunter | Ausência de `Cache-Control: no-cache` e headers de segurança extras em `nginx.conf` | `low` | `reject` | Nginx já serve assets com cache de 1 ano e páginas estáticas com ETag/Last-Modified; headers extras não foram solicitados. |
| blind-hunter | Ausência de `sitemap.xml` para rotas públicas | `low` | `reject` | Não solicitado na especificação; `robots.txt` e `index, follow` já orientam crawlers. |
| blind-hunter | `aria-live="polite"` no card inteiro e `.bar-track` sem `aria-hidden="true"` em `landing.html` | `low` | `patch` | Mover `aria-live="polite"` para `#founders-slots-value` e adicionar `aria-hidden="true"` em `.bar-track` evita leitura redundante em leitores de tela. |
| blind-hunter | `terms.html:1375` expõe identificador interno `FOUNDER_LIFETIME` ao usuário final | `low` | `patch` | Remover `(<code>FOUNDER_LIFETIME</code>)` do texto público dos Termos de Uso. |

## Verification

**Commands:**
- `cd frontend && flutter analyze && flutter test` -- expected: zero warnings/erros no `analyze` e todos os testes aprovados.

**Manual checks (if no CLI):**
- Validar sintaxe de `frontend/nginx.conf` (ou via container `nginx:1.27-alpine` se Docker disponível) e inspecionar responsividade de `landing.html`, `terms.html` e `privacy.html`.
