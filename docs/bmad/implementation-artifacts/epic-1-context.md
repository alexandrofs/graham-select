# Epic 1 Context: Rebranding Kerno, Vitrine Pública & Programa Founders

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Transicionar integralmente a identidade pública e autenticada da plataforma para a marca **Kerno** (*"O núcleo inteligente do seu dinheiro"*) operando sob o domínio oficial `kerno.afsdigital.com.br`, lançar a nova Landing Page pública de alta conversão com isolamento SEO e destaque do Programa Founders, estruturar o controle atômico transacional do lote inicial de vagas vitalícias de Membros Founders com fallback automático para Trial Pro de 90 dias e camada Free não-destrutiva, e viabilizar o onboarding em 1 clique via Google OAuth2 com aceite transparente aos Termos de Uso e à Política de Privacidade.

## Stories

- Story 1.1: Identidade Visual Institucional Kerno e Migração de Metadados Públicos
- Story 1.2: Infraestrutura de Controle Transacional do Lote de Membros Founders
- Story 1.3: Landing Page Pública de Alta Conversão com Destaque do Programa Founders
- Story 1.4: Onboarding com Google OAuth2, Aceite de Termos e Concessão de Camada Comercial

## Requirements & Constraints

- **Identidade de marca unificada:** Exibir o nome **Kerno**, logotipo, paleta cromática institucional, favicon, ícones de aplicativo (iOS e Android), títulos de página, metadados SEO/PWA/OpenGraph, textos institucionais e templates de notificação visíveis ao usuário final sem menções ao nome legado *"Graham Select"* como nome da plataforma. Referências a *"Benjamin Graham"* permanecem exclusivamente como denominação técnica da metodologia da Margem de Segurança de Graham. Metadados públicos, OpenGraph e URLs canônicas devem refletir *"Kerno — O núcleo inteligente do seu dinheiro"* e `https://kerno.afsdigital.com.br` (migração direta sem exigência de redirecionamento do domínio legado).
- **Preservação de infraestrutura interna:** Identificadores internos de infraestrutura legada (como o diretório do chart `helm/graham-select/`, nomes de pods/serviços Kubernetes e recursos internos de banco) devem permanecer intactos para evitar *downtime* ou recriação destrutiva em produção.
- **Landing Page leve e isolamento SEO:** A vitrine pública raiz `/` deve ser servida como HTML estático/SSR leve (desacoplada do bundle Flutter Web) com *First Contentful Paint* `< 1.2s`, layout responsivo Mobile e Desktop, narrativa *"Do caos das planilhas ao núcleo inteligente das suas finanças"*, demonstração visual do GPS de Aporte, Diagrama do Cerrado e Kerno AI, destaque do Programa Founders e disclaimer regulatório CVM visível no rodapé e próximo às demonstrações. Apenas rotas públicas institucionais (`/`, `/terms`, `/privacy`, `/docs`) devem ser indexáveis (`index, follow` via `robots.txt` e meta tags); rotas autenticadas e de API (`/app/*`, `/dashboard/*`, `/api/*`) devem aplicar estritamente `noindex, nofollow`.
- **Programa Founders e camadas comerciais:** O lote inicial de vagas (teto configurável via variável de ambiente, padrão `250`) deve persistir o saldo remanescente em banco e decrementá-lo de forma transacional e atômica, eliminando condições de corrida. Novos cadastros realizados enquanto houver vagas recebem imediatamente acesso Pro gratuito vitalício (`FOUNDER_LIFETIME`) sem condição adicional de ativação em 30 dias. Após o esgotamento das vagas, novos cadastros recebem automaticamente Trial Pro de 90 dias (`PRO_TRIAL`) sem cartão de crédito, com transição não-destrutiva para a camada `FREE` após expiração (sem gateway de pagamento ou IAP neste ciclo; CTAs de upgrade pós-trial exibem apenas tela informativa / lista de espera).
- **Conformidade CVM e LGPD:** O fluxo de autenticação exibe links diretos e consentimento explícito aos Termos de Uso (`/terms`, enquadrando a plataforma como ferramenta matemática parametrizada sob CVM 19/20) e à Política de Privacidade (`/privacy`, detalhando anonimização na IA, Direito ao Esquecimento LGPD e retenção legal de 6 meses de registros de conexão pelo Art. 15 do Marco Civil da Internet).
- **Regras transversais de engenharia:** Registrar logs estruturados em todas as entradas de API (sem PII/tokens) e logs `ERROR` com stack trace em exceções; propagar cabeçalho `X-Correlation-ID`; criar migrations Liquibase exclusivamente incrementais e idempotentes (nunca modificar migrations já aplicadas); espelhar novas variáveis/propriedades em `application.yml`, chart Helm (`helm/graham-select/values.yaml`, `configmap.yaml`, `secret.yaml`) e `.github/workflows/deploy.yml`; padronizar datas/timestamps em UTC ISO-8601.

## Technical Decisions

- **Segurança e endpoints públicos:** Reutilizar autenticação Google OAuth2 + JWT com filtro em `/api/v1/*`, mantendo públicos sem autenticação apenas `/api/v1/public/*` e o endpoint de consulta de cota `GET /api/v1/founders/slots` retornando `{ "totalSlots": 250, "remainingSlots": X, "isLoteActive": true/false }`.
- **Persistência e atomicidade:** Introduzir migration incremental Liquibase para adicionar `commercial_plan` (`FOUNDER_LIFETIME`, `PRO_TRIAL`, `FREE`) e `trial_expires_at` em `tb_user` e criar `tb_founder_quota` com trava/decremento atômico condicional (`WHERE founder_slots_remaining > 0`).
- **CORS e domínio oficial:** Configurar `https://kerno.afsdigital.com.br` no bean centralizado `CorsConfigurationSource` do Spring Security sem wildcards combinados com `allowCredentials(true)`.
- **Estrutura cliente e segurança mobile:** Organizar o frontend Flutter por feature (`features/founders` para fluxos do Programa Founders e superfícies públicas integradas) e persistir credenciais mobile exclusivamente via `flutter_secure_storage`.

## UX & Interaction Patterns

- **Identidade Light-First e paleta Kerno:** Canvas base `#F8FAFC` (Dark `#0B0F19`), superfície elevada `#FFFFFF` (Dark `#111827`), superfície sutil `#F1F5F9` (Dark `#1E293B`), tinta primária `#0F172A` (`17.4:1` AAA), marca primária Kerno Indigo `#4F46E5` (hover `#4338CA`, deep `#3730A3`, Dark `#818CF8`), Ouro Founder `#92400E` sobre `#FEF3C7` com borda `#F59E0B` (Dark `#FDE68A` sobre `#78350F`).
- **Tipografia e acessibilidade:** `Plus Jakarta Sans` para títulos/display e `Inter` com `tabular-nums` para métricas e corpo; contraste mínimo WCAG 2.1 AA (`4.5:1` texto, `7:1` AAA para cifras principais), alvos de toque mínimos de `48x48dp`, semântica por tríade (cor + ícone + rótulo textual) e escalonamento nativo `1.0x`–`2.0x` sem `TextOverflow.ellipsis` em valores financeiros ou tickers.
- **Sinais comerciais e linguagem regulatória:** Selo `badge-founder` (pílula Ouro Founder *"Founder"* / *"Membro Founder — Vitalício"*) no cabeçalho autenticado para Membros Founders e pill *"Trial Pro: 90 dias restantes"* para contas em trial. Toda copy pública e autenticada deve permanecer estritamente descritiva e matemática, vedando verbos imperativos de compra/venda ou promessas de rentabilidade.
- **Superfícies públicas:** Alinhar integralmente `/`, `/login`, `/docs`, `/terms` e `/privacy` à marca Kerno, com botão de autenticação Google OAuth2 acompanhado de aviso claro de aceite aos Termos de Uso e Política de Privacidade.

## Cross-Story Dependencies

- A identidade visual, ativos de marca (logotipo, favicons, ícones) e metadados de domínio `kerno.afsdigital.com.br` definidos na Story 1.1 são base direta para a Landing Page (Story 1.3), o fluxo de login/onboarding (Story 1.4) e todas as telas dos épicos subsequentes.
- A infraestrutura de banco, decremento atômico de cota e endpoint público `/api/v1/founders/slots` da Story 1.2 são pré-requisitos funcionais para o contador/banner dinâmico da Landing Page (Story 1.3) e para a concessão do status `FOUNDER_LIFETIME` / `PRO_TRIAL` no cadastro OAuth2 (Story 1.4).
- As rotas institucionais e links de conformidade (`/terms`, `/privacy`) entregues junto à vitrine pública (Story 1.3) são referenciados pelo consentimento obrigatório de login na Story 1.4.
- Os campos comerciais em `tb_user` e o selo Founder integram-se ao Header Global do Épico 2 e precisam estar cobertos pelo expurgo LGPD (`UserDataPurgeAdapter` / `AccountDeletionIT`) no Épico 5.
