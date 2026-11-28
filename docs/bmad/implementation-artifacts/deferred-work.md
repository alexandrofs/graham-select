# Deferred Work

Este arquivo registra itens de dívida técnica ou melhorias adiadas durante as revisões de código e sprints do projeto.

## Deferred from: code review of 4-1-market-data-indicator-integration (2026-06-06)

(Nenhum item pendente)

## Deferred from: code review of 4-2-premium-allocation-strategy-config.md (2026-06-08)

(Nenhum item pendente)

## Deferred from: code review of 4-3-graham-valuation-engine.md (2026-06-12)

- Quebra de ISP na GrahamRecommendationPort [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java:1] — A interface unifica leitura e escrita, obrigando adapters de leitura a lançar UnsupportedOperationException nos métodos de escrita.
- Uso de Strings Mágicas em Filtros de Regra de Negócio [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java:1] — Utiliza a String "TICKER" hardcoded no usecase em vez de um enum de tipo de meta.
- Mapeamento de Datas como String em Eventos [backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java:1] — Os eventos do Kafka de recomendação usam String para trafegar timestamps de solicitação e conclusão em vez de tipos temporais nativos, conforme instruído na especificação.

## Deferred from: code review of 4-4-explainable-ai-reasoning-box.md (2026-06-13)

- [RESOLVIDO em 2026-11-27] Risco de Quebra em Tempo de Execução na Desserialização do JSON [frontend/lib/src/features/ranking/data/models/graham_recommendation_model.dart] — Conversão robusta via _parseDouble / _parseNullableDouble suportando num, int, double e String com testes unitários em graham_recommendation_model_test.dart.
- Rolagem Conflitante no Bottom Sheet [frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart:1] — Conflito potencial de gestos entre o SingleChildScrollView interno e a folha deslizável nativa.

## Deferred from: code review of 5-1-wealth-income-goal-setup.md (2026-06-13)

(Nenhum item pendente)

## Deferred from: code review of 6-1-admin-observability-dashboard.md (2026-10-25)

- Validação de `sub` e `email` nulos na conversão OAuth2 [backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java:34] — Comportamento pré-existente da Story 1.1 na extração de claims do JWT.
- Recuperação de concorrência dentro da mesma transação em `OAuth2UserProvisioningConverter.convert()` [backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java:72] — Bloco catch pré-existente da Story 1.1 dentro de método anotado com `@Transactional`.

## Deferred from: code review of 7-1-vinculo-de-novo-cpf-a-conta-matriz (2026-11-13)
- [RESOLVIDO em 2026-11-27] Implementar `Persistable<UUID>` em `FamilyProfileEntity` (`backend/api/src/main/java/afsdigital/grahamselect/api/family/infrastructure/persistence/jpa/entities/FamilyProfileEntity.java`) para evitar `SELECT` prévio antes de `INSERT` em `FamilyProfileJpaAdapter.save()`. Coberto com testes unitários em `FamilyProfileEntityTest.java`.

## Deferred from: code review of 9-2-web-crawler-de-proventos-e-fatos-relevantes.md (2026-11-15)

- Endpoints Ideais e Protocolo Rad/ENET da CVM [backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/CvmOfficialCrawlerAdapter.java:467] — A extração avançada de formulários e autenticação na sessão do sistema CVM Rad/ENET requer engenharia reversa de protocolo governamental além do escopo da história 9.2, onde Brapi opera como fonte primária robusta.
- Consulta de Agenda com Expressão COALESCE Sem Índice Funcional e Sem Paginação [backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/jpa/repositories/DividendAnnouncementJpaRepository.java:1131] — Em grande escala de dados, `COALESCE(paymentDate, comDate)` impede uso de índices B-Tree padrão e ausência de `Pageable` pode onerar memória.

## Deferred from: code review of 9-3-conector-open-finance.md (2026-11-17)

- Integração com Agregador Real de Produção vs Mock Sandbox [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/OpenFinanceConnectorAdapter.java:180] — A integração com agregadores reais (Pluggy, Celcoin ou APIs Bacen FAPI) em modo não-mock requer credenciamento formal e contratação de parceiro de Open Finance, mantendo-se o MockOpenFinanceConnectorAdapter como driver ativo de testes/sandbox.
- Reconciliação direta de custódia vs derivação por transações de trades [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java:88] — No Graham-Select a custódia consolidada é derivada reativamente do razão de transações de compra/venda pelo valuation-service. A reconciliação forçada de saldo estático fica diferida como evolução de auditoria de custódia.
- Processamento sequencial em thread única de conexões ativas no Scheduler em alta escala [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/spring/OpenFinanceSyncScheduler.java:36] — Para a escala atual o agendador sequencial atende com folga dentro da janela do ShedLock. Paginação com pool de workers concorrentes fica adiada para cenário de milhares de conexões ativas.

## Deferred from: code review of 10-1-relatorio-de-bens-e-direitos-informe-anual.md (2026-11-19)

- Redesenho de navegação inferior móvel com mais de 5 destinos [frontend/lib/src/core/widgets/main_layout.dart:237] — A barra inferior de navegação (`_BottomNavBar`) possui 10 destinos, ultrapassando a recomendação de 3-5 do Material Design. Deve ser abordado em um refator dedicado de UX (ex: menu 'Mais'/Drawer).
- Otimização de consulta em lote para CNPJs de empresas no relatório de IRPF [backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxAssetDeclarationReportUseCase.java:72] — A busca de dados de empresas utiliza cache em memória por ticker único; substituição por `findAllByTickerIn` pode ser implementada futuramente caso a carteira contenha centenas de ativos distintos.

## Deferred from: code review of 11-2-web-push-para-zona-de-compra-graham.md (2026-11-20)

- Processamento assíncrono desacoplado para disparo em massa de Web Push na avaliação diária [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateGrahamBuyZoneAlertsUseCase.java:82] — Atualmente o agendador executa as notificações de forma síncrona dentro da janela do ShedLock de 15 minutos, satisfazendo a demanda atual. Evoluir para fila assíncrona ou pool de threads quando a base de assinantes crescer.
- Caching de token JWT VAPID assinado por origin [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java:80] — O token VAPID é recalculado a cada envio; adicionar cache por origin com expiração de 12h reduz overhead computacional de assinatura ECDSA em execuções de lote volumosas.
- Validação antecipada do par de chaves VAPID no startup do adapter [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java:45] — Validar matematicamente a compatibilidade entre chave pública e privada P-256 durante a inicialização do bean evita falhas silenciosas 401 em runtime.
- Desativação de assinatura por endpoint físico em caso de troca de usuário no mesmo browser compartilhado [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/RegisterWebPushSubscriptionUseCase.java:40] — O hash é calculado por `userId + "|" + endpoint` conforme AC 2. Em máquinas compartilhadas onde múltiplos usuários usam o mesmo perfil de navegador, considerar desativar registros de outros usuários vinculados ao mesmo endpoint na nova subscrição.
- Suporte a atributo tag para agrupamento no Service Worker push-sw.js [frontend/web/push-sw.js:20] — Adicionar atributo `tag` com base no ticker no payload de notificação para agrupar e evitar acúmulo de múltiplos popups na bandeja do sistema operacional.

## Deferred from: code review of 10-2-calculo-de-lucro-realizado-e-isencao.md (2026-11-20)

- Mecanismo de download de arquivos CSV no Flutter Web via launchUrl(dataURI) [frontend/lib/src/features/tax/presentation/providers/tax_provider.dart:140] — O disparo de download via dataURI no Flutter Web/Desktop pode apresentar comportamentos variados entre browsers ou desconsiderar Content-Disposition; padronização com package dedicado de file saver adiada por seguir padrão já existente na Story 10.1.
- Calendário dinâmico de feriados bancários móveis para vencimento de DARF [backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/utils/DarfDueDateCalculator.java:11] — Feriados móveis (Sexta-feira Santa, Carnaval, Corpus Christi) diferidos para implementação em serviço compartilhado de calendário fiscal/bancário; 31/12 incluído como feriado bancário fixo (Resolução CMN/Bacen 4.880/2020).

## Deferred from: retrospective of Epic 5 (2026-11-20)

- Debounce do formulário de metas financeiras [frontend/lib/src/features/goals/presentation/] — Defer da Story 5.1 sem dono; **Dono: Elena (Junior Dev)**. Prioridade baixa; registrado no backlog para tratamento em refator de UX.

## Deferred from: code review of spec-1-1-identidade-visual-institucional-kerno (2026-11-23)

- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: [RESOLVIDO em 2026-11-27] Isolar `app.upload.storage-path` em diretório temporário nos testes (`application-test.yml`) e adicionar `uploads/` ao `.gitignore`.
  evidence: `BrokerageNoteUploadControllerTest` isolado com `app.upload.storage-path: ./target/test-uploads` em `application-test.yml` e `uploads/`, `test-uploads/` adicionados a `.gitignore`.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: Exibir marca e logotipo Kerno na barra superior (`_TopBar`) em viewports mobile (`< 900px`) em `frontend/lib/src/core/widgets/main_layout.dart`.
  evidence: Quando `isMobile` é verdadeiro em `main_layout.dart:13-33`, `_Sidebar` é omitido e `_TopBar` (`L260-310`) inicia diretamente com `Spacer()`; o redesenho multi-superfície mobile está previsto na Story 2.3.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: Adicionar `mipmap-anydpi-v26/ic_launcher.xml` adaptativo para Android 8+ e `badge` monocromático transparente para Web Push em `frontend/web/push-sw.js:16`.
  evidence: `frontend/android/app/src/main/res/` não possui camada adaptativa XML (gerando quadro branco em torno de `ic_launcher.png` com cantos transparentes) e `push-sw.js:16` referencia `/icons/Icon-192.png` preenchido.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: [RESOLVIDO em 2026-11-27] Auditar a permissão incondicional de `http://localhost:*`, `http://127.0.0.1:*` e `http://10.0.2.2:*` em `SecurityConfig.corsConfigurationSource` sob profile `prod`.
  evidence: `SecurityConfig.java` ajustado para utilizar exclusivamente `${app.security.cors.allowed-origins}`, removendo padrões genéricos de loopback hardcoded em runtime.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: [RESOLVIDO em 2026-11-25] Espelhar `CRAWLER_USER_AGENT` no chart Helm e remover a string padrão duplicada de `@Value` em `CorporateEventsConfiguration.java:31`.
  evidence: Declarado em `helm/graham-select/values.yaml:46` e `helm/graham-select/templates/configmap.yaml:42`.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: [RESOLVIDO em 2026-11-27] Substituir o placeholder `Icons.account_circle` por `assets/icons/google_logo.png` em `login_screen.dart` e tornar o email do rodapé de `home_page.dart` selecionável ou link `mailto:`.
  evidence: `login_screen.dart` atualizado para exibir `assets/icons/google_logo.png` e coberto por teste em `login_screen_test.dart`.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: Atualizar ou desativar o workflow legado `.github/workflows/frontend-ci.yml` (`vars.API_BASE_URL` e `RENDER_FRONTEND_DEPLOY_HOOK`).
  evidence: `.github/workflows/frontend-ci.yml:72-121` continua referenciando build Pack e deploy hook legado no Render apesar do deploy oficial em OKE em `.github/workflows/deploy.yml`.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: Executar `helm lint helm/graham-select` e `helm template test-graham helm/graham-select --debug` em ambiente Linux/CI com `helm` instalado.
  evidence: Achado `maybe-false` (`medium` potencial): o CLI `helm` e o daemon Docker não estão disponíveis neste ambiente Windows; a execução em Linux/CI encerrará a verificação de sintaxe Helm/Go templates.

- source_spec: `docs/bmad/implementation-artifacts/spec-1-2-infraestrutura-de-controle-transacional-do-lote-de-membros-founders.md`
  summary: Configurar execução automática dos testes de integração H2 (`*IT.java`) no pipeline de CI (`.github/workflows/deploy.yml` / `maven-failsafe-plugin`).
  evidence: Lacuna pré-existente do repositório confirmada pelo `verification-gap` (`mvn clean test` executa apenas `*Test.java` e pula os 30 `*IT.java`).

- source_spec: `docs/bmad/implementation-artifacts/spec-1-3-landing-page-publica-alta-conversao-programa-founders.md`
  summary: Add automated DOM/browser test for `landing.html` `/api/v1/founders/slots` state transitions and error fallback.
  evidence: `frontend/test/kerno_landing_seo_test.dart` only asserts static HTML/JS substrings; live DOM execution requires Playwright or Node DOM harness in CI.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-3-landing-page-publica-alta-conversao-programa-founders.md`
  summary: Validate `frontend/nginx.conf` syntax (`nginx -t`) and live HTTP status/X-Robots-Tag headers in Docker/CI.
  evidence: Local Docker Desktop daemon was not running during Story 1.3 build (`dockerDesktopLinuxEngine` unavailable).

- source_spec: `docs/bmad/implementation-artifacts/spec-1-4-onboarding-google-oauth2-termos-camada-comercial.md`
  summary: [RESOLVIDO em 2026-11-25] Resetar `ProfileProvider` em logout ou troca de usuário no `MultiProvider` raiz (`frontend/lib/main.dart`) para evitar exibição momentânea de perfil anterior.
  evidence: `ProfileProvider.reset()` integrado no listener `authStateChanges` em `frontend/lib/main.dart:289-296`.

- source_spec: `docs/bmad/implementation-artifacts/spec-2-1-fundacao-design-system-intergeracional.md`
  summary: [RESOLVIDO em 2026-11-27] Adicionar teste unitário cobrindo KernoThemeData.lerp entre tema Light e Dark e fallback não-KernoThemeData.
  evidence: Teste implementado em `frontend/test/core/theme/kerno_design_system_test.dart` validando interpolação em t=0, t=1, t=0.5 e fallback para nulo/não-KernoThemeData.
  summary: Adicionar teste unitário cobrindo KernoThemeData.lerp entre tema Light e Dark e fallback não-KernoThemeData.
  evidence: verification-gap identificou que KernoThemeData.lerp (kerno_tokens.dart:434-468) não possui chamada direta em frontend/test/.

- source_spec: `docs/bmad/implementation-artifacts/spec-2-4-dashboard-patrimonial-com-modo-privacidade-e-protecao-de-snapshot.md`
  summary: Aplicar PrivacyProvider (máscara R$ •••••• e Semantics) nas demais rotas autenticadas fora do Dashboard/Portfólio (goals, tax).
  evidence: PrivacyToggleButton está no _TopBar global, mas telas fora do Dashboard (ex. goal_summary_card.dart, tax_declaration_page.dart) ainda formatam R$ sem consultar PrivacyProvider.
- source_spec: `docs/bmad/implementation-artifacts/spec-2-4-dashboard-patrimonial-com-modo-privacidade-e-protecao-de-snapshot.md`
  summary: Adicionar teste E2E/integrado validando wiring raiz de PrivacyProvider.load() em GrahamSelectApp autenticado e preload antes do primeiro frame.
  evidence: Testes atuais injetam PrivacyProvider diretamente nos widgets; GrahamSelectApp exige harness completo autenticado.

- source_spec: `docs/bmad/implementation-artifacts/spec-2-5-adaptacao-visual-ingestao-b3-crud-multi-classe-holding-familiar.md`
  summary: Implementar filtro global Multi-CPF (`MultiCpfSelector` na `_TopBar`, `profileId` nos endpoints `/api/v1/portfolios/*`), badge `"Pausado no Free — Upgrade Pro"` e consentimento de representação legal para dependentes menores (LGPD Art. 14) em `FamilyProfilesPage` e `FamilyProfileController`, além de validação de expurgo Multi-CPF em `AccountDeletionIT`.
  evidence: Fatiado da Story 2.5 no gate de tokens para manter a spec principal focada em Ingestão B3 (com descarte de PII) e CRUD Manual Multi-Classe dentro do limite de 1600 tokens.
