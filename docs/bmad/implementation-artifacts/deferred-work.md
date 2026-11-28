# Deferred Work

Este arquivo registra itens de dívida técnica ou melhorias adiadas durante as revisões de código e sprints do projeto.

## Deferred from: code review of 4-1-market-data-indicator-integration (2026-06-06)

(Nenhum item pendente)

## Deferred from: code review of 4-2-premium-allocation-strategy-config.md (2026-06-08)

(Nenhum item pendente)

## Deferred from: code review of 4-3-graham-valuation-engine.md (2026-06-12)

- [RESOLVIDO em 2026-11-27] Quebra de ISP na GrahamRecommendationPort [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/GrahamRecommendationPort.java] — Segregada em `GrahamRecommendationReadPort` e `GrahamRecommendationWritePort`; `GrahamRecommendationReadAdapter` implementa apenas `GrahamRecommendationReadPort` sem lançar `UnsupportedOperationException`.
- [RESOLVIDO em 2026-11-27] Uso de Strings Mágicas em Filtros de Regra de Negócio [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java] — Substituído pelo enum `AllocationGoalType` (`TICKER`, `ASSET_CLASS`).
- Mapeamento de Datas como String em Eventos [backend/common/src/main/java/afsdigital/grahamselect/common/domain/entities/TopicConstants.java:1] — Os eventos do Kafka de recomendação usam String para trafegar timestamps de solicitação e conclusão em vez de tipos temporais nativos, conforme instruído na especificação.

## Deferred from: code review of 4-4-explainable-ai-reasoning-box.md (2026-06-13)

- [RESOLVIDO em 2026-11-27] Risco de Quebra em Tempo de Execução na Desserialização do JSON [frontend/lib/src/features/ranking/data/models/graham_recommendation_model.dart] — Conversão robusta via _parseDouble / _parseNullableDouble suportando num, int, double e String com testes unitários em graham_recommendation_model_test.dart.
- [RESOLVIDO em 2026-11-27] Rolagem Conflitante no Bottom Sheet [frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart:350] — Definido `enableDrag: false` no `showModalBottomSheet` para evitar competição de gesto com o `DraggableScrollableSheet` interno.

## Deferred from: code review of 5-1-wealth-income-goal-setup.md (2026-06-13)

(Nenhum item pendente)

## Deferred from: code review of 6-1-admin-observability-dashboard.md (2026-10-25)

- [RESOLVIDO em 2026-11-27] Validação de `sub` e `email` nulos na conversão OAuth2 [backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java] — Adicionada validação explícita de `sub` nulo/vazio lançando `BadCredentialsException` (com cobertura em `OAuth2UserProvisioningConverterTest.java`), além da validação pré-existente de `email`.
- [RESOLVIDO em 2026-11-27] Recuperação de concorrência dentro da mesma transação em `OAuth2UserProvisioningConverter.convert()` [backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java] — `convert()` utiliza `TransactionTemplate(PROPAGATION_REQUIRES_NEW)` isolado sem `@Transactional` externo.

## Deferred from: code review of 7-1-vinculo-de-novo-cpf-a-conta-matriz (2026-11-13)
- [RESOLVIDO em 2026-11-27] Implementar `Persistable<UUID>` em `FamilyProfileEntity` (`backend/api/src/main/java/afsdigital/grahamselect/api/family/infrastructure/persistence/jpa/entities/FamilyProfileEntity.java`) para evitar `SELECT` prévio antes de `INSERT` em `FamilyProfileJpaAdapter.save()`. Coberto com testes unitários em `FamilyProfileEntityTest.java`.

## Deferred from: code review of 9-2-web-crawler-de-proventos-e-fatos-relevantes.md (2026-11-15)

- Endpoints Ideais e Protocolo Rad/ENET da CVM [backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/CvmOfficialCrawlerAdapter.java:467] — A extração avançada de formulários e autenticação na sessão do sistema CVM Rad/ENET requer engenharia reversa de protocolo governamental além do escopo da história 9.2, onde Brapi opera como fonte primária robusta.
- [RESOLVIDO em 2026-11-28] Consulta de Agenda com Expressão COALESCE Sem Índice Funcional e Sem Paginação [backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/jpa/repositories/DividendAnnouncementJpaRepository.java] — Reescrita cláusula de data sem `COALESCE` no `WHERE` para uso de índices B-Tree, adicionada migration `42-add-dividend-schedule-indexes.yaml` e paginação limitada (`PageRequest.of(0, 500)`) em `DividendAnnouncementJpaAdapter` com cobertura em `DividendAnnouncementJpaAdapterTest.java`.

## Deferred from: code review of 9-3-conector-open-finance.md (2026-11-17)

- Integração com Agregador Real de Produção vs Mock Sandbox [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/OpenFinanceConnectorAdapter.java:180] — A integração com agregadores reais (Pluggy, Celcoin ou APIs Bacen FAPI) em modo não-mock requer credenciamento formal e contratação de parceiro de Open Finance, mantendo-se o MockOpenFinanceConnectorAdapter como driver ativo de testes/sandbox.
- Reconciliação direta de custódia vs derivação por transações de trades [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java:88] — No Graham-Select a custódia consolidada é derivada reativamente do razão de transações de compra/venda pelo valuation-service. A reconciliação forçada de saldo estático fica diferida como evolução de auditoria de custódia.
- Processamento sequencial em thread única de conexões ativas no Scheduler em alta escala [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/spring/OpenFinanceSyncScheduler.java:36] — Para a escala atual o agendador sequencial atende com folga dentro da janela do ShedLock. Paginação com pool de workers concorrentes fica adiada para cenário de milhares de conexões ativas.

## Deferred from: code review of 10-1-relatorio-de-bens-e-direitos-informe-anual.md (2026-11-19)

- [RESOLVIDO em 2026-11-28] Redesenho de navegação inferior móvel com mais de 5 destinos [frontend/lib/src/core/widgets/main_layout.dart] — `_BottomNavBar` consolidado para 4 destinos principais (5 com feature flag de Gastos) via `StatefulNavigationShell` na Story 2.3.
- [RESOLVIDO em 2026-11-27] Otimização de consulta em lote para CNPJs de empresas no relatório de IRPF [backend/common/src/main/java/afsdigital/grahamselect/common/tax/application/usecase/GetTaxAssetDeclarationReportUseCase.java] — Adicionado `CompanyRepository.findAllByTickerIn` (implementado em `CompanyJpaAdapter` e `CompanyRepositoryImpl`) e consulta em lote única antes do loop.

## Deferred from: code review of 11-2-web-push-para-zona-de-compra-graham.md (2026-11-20)

- Processamento assíncrono desacoplado para disparo em massa de Web Push na avaliação diária [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/EvaluateGrahamBuyZoneAlertsUseCase.java:82] — Atualmente o agendador executa as notificações de forma síncrona dentro da janela do ShedLock de 15 minutos, satisfazendo a demanda atual. Evoluir para fila assíncrona ou pool de threads quando a base de assinantes crescer.
- [RESOLVIDO em 2026-11-27] Caching de token JWT VAPID assinado por origin [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java] — Implementado cache em `ConcurrentHashMap` por origin com expiração de 12h (margem de renovação de 5 min), coberto em `VapidWebPushGatewayAdapterTest.java`.
- [RESOLVIDO em 2026-11-27] Validação antecipada do par de chaves VAPID no startup do adapter [backend/api/src/main/java/afsdigital/grahamselect/api/notifications/infrastructure/gateway/VapidWebPushGatewayAdapter.java] — Validação criptográfica ECDSA P-256 entre `publicKey` e `privateKey` no construtor, coberta em `VapidWebPushGatewayAdapterTest.java`.
- [RESOLVIDO em 2026-11-27] Desativação de assinatura por endpoint físico em caso de troca de usuário no mesmo browser compartilhado [backend/common/src/main/java/afsdigital/grahamselect/common/notifications/application/usecase/RegisterWebPushSubscriptionUseCase.java] — Desativação automática de assinaturas ativas de outros usuários para o mesmo `endpoint`, coberta em `RegisterWebPushSubscriptionUseCaseTest.java`.
- [RESOLVIDO em 2026-11-27] Suporte a atributo tag para agrupamento no Service Worker push-sw.js [frontend/web/push-sw.js] — Adicionado `tag` por `data.tag` / `data.ticker` com `renotify: true`.

## Deferred from: code review of 10-2-calculo-de-lucro-realizado-e-isencao.md (2026-11-20)

- Mecanismo de download de arquivos CSV no Flutter Web via launchUrl(dataURI) [frontend/lib/src/features/tax/presentation/providers/tax_provider.dart:140] — O disparo de download via dataURI no Flutter Web/Desktop pode apresentar comportamentos variados entre browsers ou desconsiderar Content-Disposition; padronização com package dedicado de file saver adiada por seguir padrão já existente na Story 10.1.
- [RESOLVIDO em 2026-11-27] Calendário dinâmico de feriados bancários móveis para vencimento de DARF [backend/common/src/main/java/afsdigital/grahamselect/common/tax/domain/utils/DarfDueDateCalculator.java] — Calculados dinamicamente via algoritmo de Páscoa (Segunda/Terça de Carnaval, Sexta-feira Santa e Corpus Christi), coberto em `DarfDueDateCalculatorTest.java`.

## Deferred from: retrospective of Epic 5 (2026-11-20)

- [RESOLVIDO em 2026-11-28] Debounce do formulário de metas financeiras [frontend/lib/src/features/goals/presentation/pages/goals_page.dart] — Adicionado debounce de 300ms (`_formDebounceTimer`) nos campos do formulário de metas com cancelamento em `dispose()`.

## Deferred from: code review of spec-1-1-identidade-visual-institucional-kerno (2026-11-23)

- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: [RESOLVIDO em 2026-11-27] Isolar `app.upload.storage-path` em diretório temporário nos testes (`application-test.yml`) e adicionar `uploads/` ao `.gitignore`.
  evidence: `BrokerageNoteUploadControllerTest` isolado com `app.upload.storage-path: ./target/test-uploads` em `application-test.yml` e `uploads/`, `test-uploads/` adicionados a `.gitignore`.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: [RESOLVIDO em 2026-11-28] Exibir marca e logotipo Kerno na barra superior (`_TopBar`) em viewports mobile (`< 900px`) em `frontend/lib/src/core/widgets/main_layout.dart`.
  evidence: `_TopBar` exibe `topbar_mobile_brand` com `assets/images/logo.png` e texto `Kerno` quando `isCompact` é verdadeiro, coberto em `frontend/test/core/widgets/main_layout_test.dart`.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: [RESOLVIDO em 2026-11-28] Adicionar `mipmap-anydpi-v26/ic_launcher.xml` adaptativo para Android 8+ e `badge` monocromático transparente para Web Push em `frontend/web/push-sw.js:17`.
  evidence: Criados `frontend/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `values/ic_launcher_background.xml` e `frontend/web/icons/badge-monochrome.png` referenciado em `push-sw.js` e validado em `privacy_root_wiring_test.dart`.
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
  summary: [RESOLVIDO em 2026-11-28] Atualizar ou desativar o workflow legado `.github/workflows/frontend-ci.yml` (`vars.API_BASE_URL` e `RENDER_FRONTEND_DEPLOY_HOOK`).
  evidence: Job legado `build-web` (Pack + `RENDER_FRONTEND_DEPLOY_HOOK`) removido de `.github/workflows/frontend-ci.yml`, mantendo apenas validação `flutter analyze` e `flutter test --coverage`.
- source_spec: `docs/bmad/implementation-artifacts/spec-1-1-identidade-visual-institucional-kerno.md`
  summary: [RESOLVIDO em 2026-11-28] Executar `helm lint helm/graham-select` e `helm template test-graham helm/graham-select --debug` em ambiente Linux/CI com `helm` instalado.
  evidence: Configurado e executado automaticamente no job `validate` de `.github/workflows/deploy.yml:58-63`.

- source_spec: `docs/bmad/implementation-artifacts/spec-1-2-infraestrutura-de-controle-transacional-do-lote-de-membros-founders.md`
  summary: [RESOLVIDO em 2026-11-28] Configurar execução automática dos testes de integração H2 (`*IT.java`) no pipeline de CI (`.github/workflows/deploy.yml`).
  evidence: Configurado e executado automaticamente no job `validate` de `.github/workflows/deploy.yml:40` (`mvn test -pl api "-Dtest=*IT"`).

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

- source_spec: `docs/bmad/implementation-artifacts/spec-2-4-dashboard-patrimonial-com-modo-privacidade-e-protecao-de-snapshot.md`
  summary: [RESOLVIDO em 2026-11-27] Aplicar PrivacyProvider (máscara R$ •••••• e Semantics) nas demais rotas autenticadas fora do Dashboard/Portfólio (goals, tax).
  evidence: `PrivacyProvider` integrado em `goal_summary_card.dart`, `what_if_simulator_card.dart`, `tax_declaration_page.dart`, `tax_item_card.dart`, `tax_summary_card.dart` e `monthly_summary_card.dart`, com teste em `goal_summary_card_test.dart`.
- source_spec: `docs/bmad/implementation-artifacts/spec-2-4-dashboard-patrimonial-com-modo-privacidade-e-protecao-de-snapshot.md`
  summary: [RESOLVIDO em 2026-11-28] Adicionar teste E2E/integrado validando wiring raiz de PrivacyProvider.load() em GrahamSelectApp autenticado e preload antes do primeiro frame.
  evidence: Coberto em `frontend/test/core/security/privacy_root_wiring_test.dart` validando `GrahamSelectApp` com `FlutterSecureStorage` pré-populado.

- source_spec: `docs/bmad/implementation-artifacts/spec-2-5-adaptacao-visual-ingestao-b3-crud-multi-classe-holding-familiar.md`
  summary: Implementar filtro global Multi-CPF (`MultiCpfSelector` na `_TopBar`, `profileId` nos endpoints `/api/v1/portfolios/*`), badge `"Pausado no Free — Upgrade Pro"` e consentimento de representação legal para dependentes menores (LGPD Art. 14) em `FamilyProfilesPage` e `FamilyProfileController`, além de validação de expurgo Multi-CPF em `AccountDeletionIT`.
  evidence: Fatiado da Story 2.5 no gate de tokens para manter a spec principal focada em Ingestão B3 (com descarte de PII) e CRUD Manual Multi-Classe dentro do limite de 1600 tokens.

- source_spec: `docs/bmad/implementation-artifacts/spec-2-5-adaptacao-visual-ingestao-b3-crud-multi-classe-holding-familiar.md`
  summary: [RESOLVIDO em 2026-11-27] Classificar ETFs de ações brasileiras terminados em `11` (ex.: `BOVA11`, `SMAL11`) fora de `FIIs` em `GetCustodyPositionsUseCase.inferAssetClass`.
  evidence: Adicionados `BRAZILIAN_EQUITY_ETF_TICKERS` (`Ações`) e `FIXED_INCOME_ETF_TICKERS` (`Renda Fixa`) antes da regra `t.endsWith("11") -> "FIIs"`, coberto em `GetCustodyPositionsUseCaseTest.java`.
- source_spec: `docs/bmad/implementation-artifacts/spec-2-5-adaptacao-visual-ingestao-b3-crud-multi-classe-holding-familiar.md`
  summary: [RESOLVIDO em 2026-11-27] Substituir `@SneakyThrows` e publicação de `ValuationRequestedEvent` dentro de `@Transactional` por logging `ERROR` explícito e outbox/after-commit nos use cases de trade manual.
  evidence: Extraído `ManualTradeUseCaseSupport` com logging `ERROR` explícito em falha de serialização e `TransactionSynchronization.afterCommit()` nos use cases `CreateManualTradeUseCase`, `UpdateManualTradeUseCase` e `DeleteManualTradeUseCase`.
