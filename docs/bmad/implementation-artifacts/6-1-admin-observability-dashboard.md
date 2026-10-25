---
baseline_commit: 6b5d65a71c2f5416281c82edb84036da109fc96b
---
# Story 6.1: Painel de Monitoramento Admin

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** administrador do sistema,
**I want** visualizar métricas agregadas de uso (número de usuários, volume de uploads, taxa de erro de parse),
**So that** eu identifique problemas técnicos ou gargalos de infraestrutura proativamente.

## Acceptance Criteria

1. **AC1 - Exibição das Métricas e Gráficos de Observabilidade em `/admin/dashboard` (FR26):**
   - **Given** que o usuário autenticado possui a permissão de administrador (via flag `role: ADMIN` presente na claim `role`/`roles` do JWT **ou** persistida na coluna `role = 'ADMIN'` da tabela `users` conforme Decisão Arquitetural D5)
   - **When** ele acessa a rota `/admin/dashboard` no frontend (que consome `GET /api/v1/admin/observability`)
   - **Then** o sistema exibe gráficos e indicadores consolidados de:
     1. **Usuários Ativos (24h):** Total de usuários ativos nas últimas 24 horas (`activeUsers24h`), total de usuários cadastrados (`totalUsers`) e distribuição por tier (`TRIAL`, `FREE`, `PREMIUM`) / série temporal recente para visualização gráfica;
     2. **Sucesso vs Erro de Upload (Kafka):** Volume total de importações B3 (`totalUploads` deduplicadas por `correlationId`), contagem e proporção gráfica de `successCount` (`SUCESSO`/`COMPLETED`), `partialCount` (`PARCIAL`/`COMPLETED_WITH_ERRORS`), `errorCount` (`ERRO`/`FAILED`), `processingCount` (`PROCESSANDO`/`RECEIVED`/`PROCESSING`) e taxa percentual de erro (`errorRatePercentage`);
     3. **Latência Média do Motor Graham:** Latência média em milissegundos (`averageLatencyMs`), percentil P95 (`p95LatencyMs`), meta de SLA de 2000ms (conforme NFR3: `slaTargetMs = 2000`), status de conformidade do SLA (`withinSla`) e pontos de histórico de latência (`latencyHistory`) para renderização gráfica.

2. **AC2 - Anonimização Obrigatória de Dados Financeiros e Pessoais Sensíveis (FR26 / NFR5):**
   - **Given** que o administrador visualiza o painel `/admin/dashboard` e a listagem/tracing recente de importações (`recentUploads`)
   - **When** os dados são retornados pela API `GET /api/v1/admin/observability` e renderizados na tela
   - **Then** nenhum dado pessoal sensível (e-mail, nome completo, CPF, `googleSub` ou identificador interno em texto claro) nem dados financeiros individuais (saldo de carteira, posições por ticker ou patrimônio individual) são expostos em texto claro
   - **And** os identificadores de usuários nas trilhas de auditoria/importação aparecem estritamente anonimizados no formato mascarado `usr_***xxxx` (onde `xxxx` são 4 caracteres hexadecimais determinísticos derivados de hash SHA-256 do `userId`, garantindo anonimização inclusive para IDs numéricos curtos como `1` ou `42`), o nome de arquivo aparece sanitizado (`extrato_b3_***.xlsx`) e o valor de portfólio aparece mascarado (`maskedPortfolioValue: "***"`).

3. **AC3 - Controle de Acesso Baseado em Papel (`ADMIN` vs `USER`) no Backend e Frontend:**
   - **Given** que um usuário autenticado comum (`role: USER`, sem claim `ADMIN` no JWT e sem `role = 'ADMIN'` no banco) tenta acessar `GET /api/v1/admin/observability`
   - **When** a requisição é processada pelo Spring Security (`@RequireAdmin` / `AdminAccessGuard`)
   - **Then** o backend rejeita a chamada com status HTTP `403 Forbidden` no formato padrão RFC 7807 (`application/problem+json`, `type: "urn:problem-type:access-denied"`) e registra log de aviso/erro
   - **And** caso um usuário sem perfil `ADMIN` acesse `/admin/dashboard` no Flutter Web, a tela exibe um estado claro de **"Acesso Restrito a Administradores"** (ou trata o `403 Forbidden` graciosamente sem quebrar a aplicação), além de exibir o atalho de navegação para o Painel Admin apenas para usuários com `role == 'ADMIN'`.

4. **AC4 - Contrato REST, Precisão Numérica, UTC e Observabilidade (`AGENTS.md` & `project-context.md`):**
   - **Given** o endpoint `GET /api/v1/admin/observability` (com envelope padrão `{ "data": { ... } }` conforme Decisão Arquitetural D7) e o campo `role` adicionado ao schema `UserProfile` em `backend/api/src/main/resources/openapi.yaml` (`/api/v1/users/me`)
   - **When** o endpoint é invocado
   - **Then** todas as porcentagens e métricas decimais utilizam `BigDecimal` com `RoundingMode.HALF_UP`, todas as janelas temporais (ex.: últimas 24h) e timestamps utilizam `ZoneOffset.UTC`
   - **And** o controller e o service/use case registram logs estruturados (`INFO` na entrada da API com contexto do requisitante e `ERROR` com stack trace em blocos `catch`), mantendo todos os nomes dos campos JSON em `camelCase`.

5. **AC5 - Qualidade, Análise Estática e Testes Automatizados (`AGENTS.md`):**
   - **Given** a implementação completa no backend e frontend
   - **When** a suíte de validação é executada
   - **Then** todos os testes unitários e de integração do backend (`mvn clean test`) e os testes e análise estática do frontend (`flutter analyze` e `flutter test`) passam com zero erros ou avisos e sem regressão nos testes existentes.

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domínio, Persistência de Usuário e Migração Liquibase)

- [x] **Task 1: Criar `UserRole`, Migração Liquibase `25-add-role-to-users.yaml` e Evoluir `User` / `UserRepository`** (AC: #1, #3, #4)
  - [x] 1.1 Criar enum `UserRole` (`USER`, `ADMIN`) no pacote real de entidades de usuário:
    `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/UserRole.java`.
  - [x] 1.2 Criar migration Liquibase `backend/common/src/main/resources/db/changelog/25-add-role-to-users.yaml` adicionando a coluna `role VARCHAR(20) NOT NULL DEFAULT 'USER'` na tabela `users`, e registrar no final de `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (logo após `24-create-shedlock-table.yaml`).
  - [x] 1.3 Atualizar a entidade JPA `User` (`backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java`):
    - Adicionar:
      ```java
      @Enumerated(EnumType.STRING)
      @Column(name = "role", nullable = false, length = 20)
      @Builder.Default
      private UserRole role = UserRole.USER;
      ```
    - Adicionar `@PrePersist` (ou garantir no `@PrePersist` existente) que se `this.role == null`, atribua `this.role = UserRole.USER;` para que qualquer `new User(...)` ou builder em testes legados nunca persista `null` na coluna `NOT NULL`.
  - [x] 1.4 Adicionar métodos de consulta agregada em `UserRepository` (`backend/common/src/main/java/afsdigital/grahamselect/common/user/infrastructure/persistence/UserRepository.java`) e `IngestionAuditRepository` (`backend/common/src/main/java/afsdigital/grahamselect/common/ingestion/infrastructure/persistence/jpa/repositories/IngestionAuditRepository.java`):
    - Em `UserRepository`:
      - `long countByTier(SubscriptionTier tier);`
      - `@Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since OR u.updatedAt >= :since") long countActiveUsersSince(@Param("since") LocalDateTime since);`
      - `@Query("SELECT u.id FROM User u WHERE u.createdAt >= :since OR u.updatedAt >= :since") List<Long> findActiveUserIdsSince(@Param("since") LocalDateTime since);`
    - Em `IngestionAuditRepository`:
      - `List<IngestionAudit> findTop20ByOrderByUploadDateDesc();`
      - `@Query("SELECT DISTINCT a.userId FROM IngestionAudit a WHERE a.uploadDate >= :since") List<String> findDistinctUserIdsSince(@Param("since") LocalDateTime since);`

---

### Backend — Módulo `api` (Segurança, Contrato OpenAPI e Endpoint `/api/v1/admin/observability`)

- [x] **Task 2: Suporte a `ROLE_ADMIN` no Spring Security (`OAuth2UserProvisioningConverter`, `@RequireAdmin` e `AdminAccessGuard`)** (AC: #1, #3)
  - [x] 2.1 Atualizar `OAuth2UserProvisioningConverter` (`backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java`):
    - Extrair se o JWT possui claim de admin: verificar `jwt.getClaimAsString("role")` igual a `"ADMIN"` (ou `"ROLE_ADMIN"`) ou lista `jwt.getClaimAsStringList("roles")` contendo `"ADMIN"` / `"ROLE_ADMIN"`.
    - **GUARDRAIL CRÍTICO DE NÃO-REGRESSÃO (`UserAutoProvisioningExtendedIT`):**
      Em `UserAutoProvisioningExtendedIT.shouldNotUpdateIfNameUnchanged`, o teste faz dois logins consecutivos com o mesmo nome e valida o comportamento de usuário existente. Portanto, no `.map(existingUser -> ...)`, **só chame `userRepository.save(existingUser)` se o `name` mudou OU se o JWT trouxe claim `ADMIN` e `existingUser.getRole() != UserRole.ADMIN`**.
    - Se o usuário no banco tiver `user.getRole() == UserRole.ADMIN` **OU** o JWT contiver a claim de `ADMIN`, construa as authorities contendo **ambas** `ROLE_USER` e `ROLE_ADMIN`:
      ```java
      Collection<GrantedAuthority> authorities = isAdmin
              ? Set.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
              : Set.of(new SimpleGrantedAuthority("ROLE_USER"));
      ```
      *(Nunca remova `ROLE_USER`, pois `UserAutoProvisioningExtendedIT.shouldGrantRoleUser` verifica `.contains("ROLE_USER")`!)*.
  - [x] 2.2 Criar anotação `@RequireAdmin` e componente `AdminAccessGuard` em `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/` (seguindo exatamente o padrão de `@RequirePremium` e `PremiumFeatureAccessGuard`):
    - `@RequireAdmin` anotada com `@PreAuthorize("@adminAccessGuard.canAccessAdmin()")`.
    - `AdminAccessGuard.canAccessAdmin()` verifica o `Authentication` do `SecurityContextHolder`:
      1. Se `authentication == null || !authentication.isAuthenticated()`, lança `new AccessDeniedException("Admin privileges required.")`.
      2. Se `authentication.getAuthorities()` já contiver `"ROLE_ADMIN"`, retorna `true`.
      3. Se `authentication.getPrincipal() instanceof Jwt jwt`, verifica se `jwt.getClaimAsString("role")` é `"ADMIN"`/`"ROLE_ADMIN"` ou `jwt.getClaimAsStringList("roles")` contém `"ADMIN"`/`"ROLE_ADMIN"`; se sim, retorna `true`.
      4. Faz parse de `authentication.getName()` com `Long.parseLong(authentication.getName())` (ou busca por `googleSub` caso `getName()` não seja numérico em mocks de teste) no `UserRepository`: se `user.getRole() == UserRole.ADMIN`, retorna `true`.
      5. Caso contrário, registra `log.warn("Access denied to admin resource for principal: {}", authentication.getName())` e lança `new org.springframework.security.access.AccessDeniedException("Access restricted to system administrators.")` (que o `PremiumFeatureAccessDeniedHandler` já converte automaticamente para HTTP `403 Forbidden` com `application/problem+json` e `type: "urn:problem-type:access-denied"`).

- [x] **Task 3: Atualizar `UserProfile` no OpenAPI e Implementar `AdminObservabilityService` e `AdminObservabilityController`** (AC: #1, #2, #3, #4)
  - [x] 3.1 Atualizar `backend/api/src/main/resources/openapi.yaml` e `UserController` (`backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java`):
    - No schema `UserProfile` em `openapi.yaml`, adicionar:
      ```yaml
      role:
        type: string
        enum: [USER, ADMIN]
        example: "USER"
      ```
    - Em `UserController.mapToUserProfile(User user)`, mapear o campo `role`:
      - Se a autenticação atual possuir `ROLE_ADMIN` ou `user.getRole() == UserRole.ADMIN`, definir `profile.setRole(UserProfile.RoleEnum.ADMIN)`, caso contrário `profile.setRole(UserProfile.RoleEnum.USER)`.
  - [x] 3.2 Criar DTOs e `AdminObservabilityService` em `backend/api/src/main/java/afsdigital/grahamselect/api/admin/`:
    - Pacotes:
      - `afsdigital.grahamselect.api.admin.application.dto.*` (records Java 21 imutáveis em `camelCase`):
        - `AdminObservabilityDto(AdminUserMetricsDto userMetrics, AdminUploadMetricsDto uploadMetrics, AdminGrahamLatencyMetricsDto grahamLatencyMetrics, List<AnonymizedUploadTraceDto> recentUploads, OffsetDateTime generatedAt)`
        - `AdminUserMetricsDto(long activeUsers24h, long totalUsers, long trialUsers, long freeUsers, long premiumUsers)`
        - `AdminUploadMetricsDto(long totalUploads, long successCount, long partialCount, long errorCount, long processingCount, BigDecimal errorRatePercentage)`
        - `AdminGrahamLatencyMetricsDto(BigDecimal averageLatencyMs, BigDecimal p95LatencyMs, long slaTargetMs, boolean withinSla, List<AdminLatencyPointDto> latencyHistory)`
        - `AdminLatencyPointDto(String label, BigDecimal latencyMs)`
        - `AnonymizedUploadTraceDto(String anonymizedUserId, String sanitizedFileName, String status, int processedLines, int errorLines, String maskedPortfolioValue, OffsetDateTime uploadTimestamp)`
      - `afsdigital.grahamselect.api.admin.application.service.AdminObservabilityService`:
        - **Cálculo de `userMetrics` (em `ZoneOffset.UTC`):**
          - `LocalDateTime since24h = LocalDateTime.now(ZoneOffset.UTC).minusHours(24);`
          - Unir o conjunto de IDs de usuários ativos em `users` (`createdAt >= since24h OR updatedAt >= since24h`) com `distinct userId` de `IngestionAuditRepository` e `B3ImportStatusJpaRepository` nas últimas 24h, garantindo contagem precisa mesmo para usuários que só realizaram operações sem relogar.
          - Contar `totalUsers`, `trialUsers`, `freeUsers`, `premiumUsers`.
        - **Cálculo de `uploadMetrics` (Deduplicado por `correlationId`):**
          - Consultar `B3ImportStatusJpaRepository.findAll()` e `IngestionAuditRepository.findAll()`.
          - Consolidar por `correlationId` (usando `Map<String, ...>` onde `B3ImportStatusEntity` e `IngestionAudit` com o mesmo `correlationId` representam **1 único upload**, evitando contagem duplicada!).
          - Mapear status:
            - `COMPLETED` / `SUCESSO` → `successCount`
            - `COMPLETED_WITH_ERRORS` / `PARCIAL` → `partialCount`
            - `FAILED` / `ERRO` → `errorCount`
            - `RECEIVED` / `PROCESSING` / `PROCESSANDO` → `processingCount`
          - Calcular `errorRatePercentage`: se `totalUploads == 0`, `BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)`; senão `(errorCount * 100) / totalUploads` com `BigDecimal` e `RoundingMode.HALF_UP` (escala 2).
        - **Cálculo de `grahamLatencyMetrics` (Micrometer `MeterRegistry`):**
          - Injetar `io.micrometer.core.instrument.MeterRegistry`.
          - Buscar timers de endpoints de valuation/recomendações (`http.server.requests` com tag `uri` contendo `graham-recommendations` ou `ranked-companies`, ou timer customizado `graham.valuation.latency`).
          - Se houver medições (`count > 0`), extrair `mean(TimeUnit.MILLISECONDS)` e estimar P95 (`max` ou percentil) convertendo para `BigDecimal` com escala 2 (`RoundingMode.HALF_UP`).
          - Se ainda não houver medições registradas no `MeterRegistry` (ex.: recém-iniciado), retornar valores base determinísticos realistas de referência operacional (ex.: `averageLatencyMs = 145.00`, `p95LatencyMs = 320.00`, `slaTargetMs = 2000L`, `withinSla = true`, e 6 pontos em `latencyHistory`), garantindo que o gráfico de **Latência Média do Motor Graham** sempre tenha série válida para renderização.
        - **Construção de `recentUploads` (Anonimização Estrita LGPD / FR26):**
          - Ordenar os uploads consolidados por data decrescente (máximo 10 itens).
          - Para cada item, gerar `anonymizedUserId` via hash SHA-256 curto do `userId`: `"usr_***" + primeiros 4 caracteres hexadecimais do SHA-256(userId)` (ex.: `usr_***9f8a`), garantindo que mesmo um `userId = "1"` jamais seja reversível ou exposto.
          - Sanitizar `fileName` para `"extrato_b3_***." + extensao` (ex.: `"extrato_b3_***.xlsx"`), nunca expondo nomes de arquivos que possam conter o nome ou CPF do investidor.
          - Definir `maskedPortfolioValue = "***"`.
          - Converter `uploadDate`/`createdAt` para `OffsetDateTime` em `ZoneOffset.UTC`.
        - **Logs (`AGENTS.md`):** Registrar `log.info` no início/fim da coleta e `log.error("Failed to collect admin observability metrics", ex)` em qualquer bloco `catch`.
  - [x] 3.3 Criar `AdminObservabilityController` em `backend/api/src/main/java/afsdigital/grahamselect/api/admin/web/AdminObservabilityController.java`:
    - Anotado com `@RestController`, `@RequestMapping("/api/v1/admin/observability")`, `@RequiredArgsConstructor`, `@Slf4j`.
    - Método `@GetMapping` protegido por `@RequireAdmin`:
      - Registrar `log.info("Received request to get admin observability metrics from principal {}", authentication != null ? authentication.getName() : "anonymous")`.
      - Invocar `adminObservabilityService.getObservabilityMetrics()` dentro de `try / catch`, logando `log.error` com stack trace em caso de erro inesperado.
      - Retornar `ResponseEntity.ok(Map.of("data", metrics))` (seguindo o envelope `{ "data": ... }` padrão do projeto em `FinancialGoalsController`, `PortfolioController`, etc.).

---

### Frontend Flutter — Feature `admin`, `profile` & Navegação

- [x] **Task 4: Atualizar `UserProfile` e Implementar Feature `admin` (`frontend/lib/src/features/admin/`)** (AC: #1, #2, #3, #5)
  - [x] 4.1 Atualizar `UserProfile` em `frontend/lib/src/features/profile/data/models/user_profile_model.dart`:
    - Adicionar `final String role;` com valor default `'USER'` no construtor (`this.role = 'USER'`), em `fromJson` (`role: json['role'] as String? ?? 'USER'`), em `toJson` (`'role': role`), no `operator ==` e no `hashCode`.
    - **GUARDRAIL:** Manter `this.role = 'USER'` opcional no construtor garante 100% de compatibilidade retroativa com `user_profile_model_test.dart` e `profile_page_test.dart`.
  - [x] 4.2 Criar camadas de domínio, dados e provider em `frontend/lib/src/features/admin/` (seguindo exatamente a arquitetura `src/features/` + `ChangeNotifier` + `ApiClient` usada em `goals` e `allocation`):
    - `frontend/lib/src/features/admin/domain/entities/admin_observability.dart`:
      - Entidades `AdminObservability`, `AdminUserMetrics`, `AdminUploadMetrics`, `AdminGrahamLatencyMetrics`, `AdminLatencyPoint`, `AnonymizedUploadTrace`.
    - `frontend/lib/src/features/admin/data/models/admin_observability_model.dart`:
      - `fromJson` suportando leitura tanto de envelope `{ "data": { ... } }` quanto de objeto direto, com validações `.isFinite` e defaults seguros.
    - `frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart`:
      - Recebe `ApiClient`. Chama `apiClient.get('/api/v1/admin/observability')`.
      - Trata `DioException` com `e.response?.statusCode == 403` lançando uma exceção tipada `AdminAccessDeniedException`.
    - `frontend/lib/src/features/admin/domain/repositories/admin_repository.dart` e `frontend/lib/src/features/admin/data/repositories/admin_repository_impl.dart`.
    - `frontend/lib/src/features/admin/presentation/providers/admin_observability_provider.dart`:
      - `enum AdminObservabilityStatus { initial, loading, loaded, accessDenied, error }`
      - `class AdminObservabilityProvider extends ChangeNotifier` com método `Future<void> loadMetrics()`.
  - [x] 4.3 Criar a página `AdminDashboardPage` (`frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart`) e widgets em `frontend/lib/src/features/admin/presentation/widgets/`:
    - **Skeleton Screen (`Key('admin_dashboard_skeleton')`):** Exibido durante `AdminObservabilityStatus.loading` (seguindo diretriz de UX: nunca usar apenas `CircularProgressIndicator` isolado).
    - **Estado de Acesso Restrito (`Key('admin_access_denied_state')`):** Exibido quando `status == AdminObservabilityStatus.accessDenied` (ou quando `ProfileProvider` indica `role != 'ADMIN'` e a API rejeita com 403), com ícone `Icons.lock_person_outlined`, título **"Acesso Restrito a Administradores"** e botão para voltar ao `/dashboard`.
    - **Seção 1 — Card + Gráfico "Usuários Ativos (24h)" (`Key('admin_active_users_card')`):**
      - Exibe `activeUsers24h` em destaque, `totalUsers` cadastrados e gráfico de barras horizontal/vertical proporcional (`LayoutBuilder` + `FractionallySizedBox`) da distribuição por tier (`TRIAL`, `FREE`, `PREMIUM`).
    - **Seção 2 — Card + Gráfico "Sucesso vs Erro de Upload (Kafka)" (`Key('admin_upload_metrics_card')`):**
      - Exibe `totalUploads`, badge de taxa de falha (`errorRatePercentage%`) e gráfico comparativo / barra segmentada com cores semânticas do Design System:
        - Sucesso (`#10B981`): `successCount`
        - Parcial (`#F59E0B`): `partialCount`
        - Erro (`#EF4444`): `errorCount`
        - Processando (`#0D9488`): `processingCount`
    - **Seção 3 — Card + Gráfico "Latência Média do Motor Graham" (`Key('admin_graham_latency_card')`):**
      - Exibe `averageLatencyMs` (ms), `p95LatencyMs` (ms), meta `SLA < 2000 ms`, badge de status (`withinSla ? "SLA OK (< 2000ms)" : "Acima do SLA"`) e gráfico de barras/linha (`latencyHistory`) com linha de referência do SLA.
    - **Seção 4 — Tabela/Lista de Tracing "Importações Recentes Anonimizadas" (`Key('admin_anonymized_uploads_card')`):**
      - Badge explícito **"Dados Anonimizados (LGPD / NFR5)"**.
      - Lista `recentUploads` exibindo `anonymizedUserId` (`usr_***xxxx`), `sanitizedFileName` (`extrato_b3_***.xlsx`), `status`, linhas processadas/com erro e `maskedPortfolioValue` (`***`).
  - [x] 4.4 Registrar rota `/admin/dashboard` e atalho de navegação para Admin:
    - Em `frontend/lib/main.dart`:
      - Instanciar `AdminRemoteDatasource`, `AdminRepositoryImpl` e registrar `ChangeNotifierProvider(create: (_) => AdminObservabilityProvider(repository: adminRepository))` no `MultiProvider`.
      - Adicionar `GoRoute(path: '/admin/dashboard', builder: (context, state) => const AdminDashboardPage())` dentro do `ShellRoute`.
    - Em `frontend/lib/src/core/widgets/main_layout.dart` (na `_TopBar`) e/ou em `frontend/lib/src/features/profile/presentation/pages/profile_page.dart`:
      - Usar `context.watch<ProfileProvider?>()` (com leitura segura `Provider.of<ProfileProvider?>(context)` para não quebrar testes de widget que montam `MainLayout` sem `ProfileProvider`!) para exibir um botão/chip **"Painel Admin"** (`Key('admin_dashboard_nav_button')`) que navega para `context.go('/admin/dashboard')` quando `profile?.role == 'ADMIN'`.
      - **GUARDRAIL:** Não adicionar um 9º item fixo ao `NavigationRail` / `NavigationBar` para não alterar os índices 0..7 nem quebrar testes de layout existentes.

---

### Testes Automatizados e Validação Final

- [x] **Task 5: Testes Unitários, de Integração e de Widget (Backend + Frontend)** (AC: #1, #2, #3, #4, #5)
  - [x] 5.1 Criar testes unitários de backend em `backend/api/src/test/java/afsdigital/grahamselect/api/admin/`:
    - `AdminObservabilityServiceTest`:
      - Valida cálculo de usuários ativos nas últimas 24h em UTC e distribuição por tier.
      - Valida deduplicação por `correlationId` entre `B3ImportStatusEntity` e `IngestionAudit`, cálculo de `errorRatePercentage` com `BigDecimal` (`RoundingMode.HALF_UP`).
      - Valida leitura de latência do `MeterRegistry` (com `SimpleMeterRegistry` contendo `Timer` populado e com `SimpleMeterRegistry` vazio em modo fallback).
      - Valida anonimização estrita (`usr_***xxxx` sem expor ID real/email/nome, `extrato_b3_***.xlsx` e `maskedPortfolioValue == "***"`).
    - `AdminAccessGuardTest`:
      - Valida permissão concedida via authority `ROLE_ADMIN`, via claim JWT `role: "ADMIN"` e via `user.getRole() == UserRole.ADMIN` no banco.
      - Valida lançamento de `AccessDeniedException` para usuário comum (`UserRole.USER`).
  - [x] 5.2 Criar teste de integração `AdminObservabilityControllerIT` em `backend/api/src/test/java/afsdigital/grahamselect/api/admin/web/AdminObservabilityControllerIT.java` (estendendo `BaseRepositoryIT` com `@AutoConfigureMockMvc`, seguindo o padrão de `FeatureRestrictionIT`):
    - `shouldReturn200WithMetricsAndAnonymizedDataWhenUserHasAdminRoleInJwt`
    - `shouldReturn200WhenUserHasAdminRoleInDatabase`
    - `shouldReturn403ForbiddenProblemDetailWhenUserIsRegularUser` (validando `status = 403`, `application/problem+json`, `type = "urn:problem-type:access-denied"`)
    - `shouldReturn401UnauthorizedWhenUnauthenticated`
  - [x] 5.3 Criar testes unitários e de widget no Flutter em `frontend/test/features/admin/`:
    - `frontend/test/features/admin/data/models/admin_observability_model_test.dart`: valida parse JSON do envelope `{ "data": ... }`, campos de métricas e traces anonimizados.
    - `frontend/test/features/admin/presentation/providers/admin_observability_provider_test.dart`: valida transições `initial -> loading -> loaded`, `accessDenied` (403) e `error`.
    - `frontend/test/features/admin/presentation/pages/admin_dashboard_page_test.dart`:
      - Valida exibição do Skeleton Screen durante `loading`.
      - Valida renderização dos 3 gráficos/indicadores exigidos pelo AC (**"Usuários Ativos (24h)"**, **"Sucesso vs Erro de Upload (Kafka)"**, **"Latência Média do Motor Graham"**) e da tabela anonimizada (`usr_***`, `***`, badge LGPD).
      - Valida exibição da tela de **"Acesso Restrito a Administradores"** quando o estado é `accessDenied`.
  - [x] 5.4 Executar validação completa conforme `AGENTS.md`:
    - `mvn clean test` no diretório `backend`
    - `flutter analyze` e `flutter test` no diretório `frontend`

### Review Findings

- [x] [Review][Patch] Adicionar logs estruturados (`log.info` na entrada e `log.error` com stack trace em exceções) e guard de null-safety em `UserController` [`backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java:29-101`]
- [x] [Review][Patch] Limitar `activeUsers24h` a `totalUsers` quando `totalUsers > 0` e preservar metadados não-nulos e status terminal ao consolidar `B3ImportStatusEntity` e `IngestionAudit` por `correlationId` [`backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminObservabilityService.java:115-184`]
- [x] [Review][Patch] Proteger `notifyListeners()` contra chamadas após `dispose()` em `AdminObservabilityProvider` [`frontend/lib/src/features/admin/presentation/providers/admin_observability_provider.dart:28-64`]
- [x] [Review][Patch] Tratar estado vazio de `latencyHistory` em `AdminGrahamLatencyCard` e adicionar teste de widget para visibilidade condicional de `admin_dashboard_nav_button` no `MainLayout` [`frontend/lib/src/features/admin/presentation/widgets/admin_metric_charts.dart:493-506`, `frontend/test/features/admin/presentation/pages/admin_dashboard_page_test.dart:151-206`]
- [x] [Review][Defer] Validação adicional para claims `sub` e `email` ausentes no `OAuth2UserProvisioningConverter` [`backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java:34-66`] — deferred, pre-existing
- [x] [Review][Defer] Isolamento transacional (`REQUIRES_NEW`) no fallback de concorrência em `OAuth2UserProvisioningConverter.convert()` [`backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java:72-80`] — deferred, pre-existing

---

## Dev Notes

### 1. Contexto Arquitetural e Alinhamento de Requisitos
- **Épico 6:** Administração & Operações (Saúde do Sistema).
- **FR26 (PRD):** *"Administradores do sistema podem visualizar o volume e as taxas de falha (tracing) das importações de arquivos B3 de forma anonimizada."*
- **Decisão Arquitetural D5 (RBAC) + BDD do Épico:**
  - `architecture.md` (D5): *"Roles: `USER` (padrão) e `ADMIN` (configurado manualmente no banco)."*
  - `epics.md` (Story 6.1): *"Given que o usuário possui a flag `role: ADMIN` no JWT"*.
  - **Solução Unificada:** Suportar **ambas** as origens (claim `role: "ADMIN"` / `roles: ["ADMIN"]` no JWT **OU** coluna `users.role = 'ADMIN'` no banco de dados) no `OAuth2UserProvisioningConverter` e no `AdminAccessGuard`.
- **NFR3 (SLA do Motor Graham):** Latência alvo `< 2.000ms` (`slaTargetMs = 2000`).
- **NFR5 / LGPD (Anonimização):** Como a entidade `User` no projeto utiliza `Long id` auto-incrementável (`1, 2, 3...`), mascarar apenas com substring exporia o ID se ele tivesse menos de 4 dígitos. Por isso, gere sempre um sufixo determinístico de 4 caracteres hexadecimais a partir de `MessageDigest.getInstance("SHA-256")` do `userId` (`"usr_***" + sha256Hex.substring(0, 4)`), garantindo formato uniforme `usr_***xxxx` e irreversibilidade total.

### 2. Análise de Arquivos Existentes que Serão Modificados (UPDATE Files Analysis)

1. **`backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`**
   - *Estado atual:* Possui changelogs `01` a `24-create-shedlock-table.yaml`.
   - *O que muda:* Inclusão de `db/changelog/25-add-role-to-users.yaml`.
   - *O que preservar:* Ordem exata dos changelogs existentes.

2. **`backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java`**
   - *Estado atual:* Entidade JPA mapeada para a tabela `users` com `id` (`Long`), `googleSub`, `email`, `fullName`, `tier` (`SubscriptionTier`), `trialEndsAt`, `investorProfile` (`InvestorProfile`), `createdAt`, `updatedAt`.
   - *O que muda:* Adição do campo `role` (`UserRole`) com `@Builder.Default private UserRole role = UserRole.USER;` e fallback em `@PrePersist`.
   - *O que preservar:* Todos os builders existentes (`User.builder()...build()`) usados em dezenas de testes (`FeatureRestrictionIT`, `UserAutoProvisioningIT`, `UserAutoProvisioningExtendedIT`, `UserProfileIT`, `AccountDeletionIT`) devem continuar funcionando sem exigir `.role(...)` explicitamente.

3. **`backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java`**
   - *Estado atual:* Faz lookup por `googleSub`, atualiza `fullName` apenas quando `name != null && !name.equals(existingUser.getFullName())`, cria novo usuário com `SubscriptionTier.TRIAL` se não existir, e retorna `JwtAuthenticationToken(jwt, Set.of(new SimpleGrantedAuthority("ROLE_USER")), user.getId().toString())`.
   - *O que muda:* Inclusão de `ROLE_ADMIN` nas authorities quando `user.getRole() == UserRole.ADMIN` ou quando o JWT contiver claim `role: "ADMIN"` / `roles: ["ADMIN"]`.
   - *O que preservar:*
     - `token.getName()` **DEVE** continuar sendo `user.getId().toString()` (numérico).
     - `token.getAuthorities()` **DEVE** continuar contendo `"ROLE_USER"`.
     - Não fazer `userRepository.save(existingUser)` desnecessário quando nem `fullName` nem `role` mudaram (`UserAutoProvisioningExtendedIT.shouldNotUpdateIfNameUnchanged`).

4. **`backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java` & `backend/api/src/main/resources/openapi.yaml`**
   - *Estado atual:* `UserController` implementa `UsersApiDelegate` gerado a partir de `openapi.yaml`.
   - *O que muda:* Adição do campo `role` (`USER`, `ADMIN`) ao schema `UserProfile` em `openapi.yaml` e preenchimento em `UserController.mapToUserProfile`.
   - *O que preservar:* Todos os campos existentes de `/api/v1/users/me` (`id`, `email`, `fullName`, `tier`, `investorProfile`, `trialEndsAt`, `daysRemaining`).

5. **`frontend/lib/src/features/profile/data/models/user_profile_model.dart`**
   - *Estado atual:* Possui `id`, `email`, `fullName`, `tier`, `investorProfile`, `trialEndsAt`, `daysRemaining`.
   - *O que muda:* Adição de `final String role;` com default `'USER'`.
   - *O que preservar:* Assinatura compatível com chamadas posicionais/nomeadas existentes nos testes (`UserProfile(id: 1, email: ..., fullName: ..., tier: ...)`).

6. **`frontend/lib/main.dart` & `frontend/lib/src/core/widgets/main_layout.dart`**
   - *Estado atual:* `main.dart` configura `GoRouter` e `MultiProvider` usando `Provider` / `ChangeNotifierProvider` e caminhos em `lib/src/features/...`. `MainLayout` possui 8 itens no `NavigationRail` / `NavigationBar` e `_TopBar`.
   - *O que muda:* Adição do `AdminObservabilityProvider`, da rota `/admin/dashboard` no `ShellRoute` e de botão condicional na `_TopBar` (`Key('admin_dashboard_nav_button')`) quando `ProfileProvider` tiver `profile?.role == 'ADMIN'`.
   - *O que preservar:* Usar `Provider.of<ProfileProvider?>(context)` (nullable) na `_TopBar` para nunca lançar `ProviderNotFoundException` em testes de widget que instanciam `MainLayout` sem `ProfileProvider`.

### 3. Guardrails de Arquitetura e Código (`project-context.md` & `AGENTS.md`)
- **Estrutura de Diretórios Frontend:** Todos os arquivos de código do Flutter ficam dentro de `frontend/lib/src/...` (ex.: `frontend/lib/src/features/admin/...`) e os testes ficam em `frontend/test/features/admin/...`. **NÃO** crie diretórios em `frontend/lib/features/` fora de `src/`.
- **Gerência de Estado no Frontend:** O projeto utiliza `provider` (`ChangeNotifier` + `ChangeNotifierProvider` / `Consumer`), **NÃO** `flutter_bloc` / `Cubit` (que não existe no `pubspec.yaml`).
- **Gráficos Nativos sem Dependências Externas:** Renderizar os gráficos usando widgets nativos do Flutter (`LayoutBuilder`, `FractionallySizedBox`, `CustomPaint`, `Container`, `Row`/`Column`), sem adicionar pacotes novos ao `pubspec.yaml`.
- **Deprecação do Flutter 3.11+:** Nunca usar `.withOpacity(...)`; usar sempre `.withValues(alpha: ...)` ou `.withAlpha(...)` para manter `flutter analyze` com zero warnings.
- **Regras Mandatórias (`AGENTS.md`):**
  1. Abrir branch atualizada antes de implementar (`git checkout -b feature/6-1-admin-observability-dashboard main`).
  2. Logs `log.info` em todas as entradas de APIs e `log.error` com stack trace em todas as capturas/lançamentos de exceções.
  3. Usar `BigDecimal` com `RoundingMode.HALF_UP` e datas/horários em `ZoneOffset.UTC`.
  4. Executar todos os testes de backend (`mvn clean test`) e frontend (`flutter analyze` e `flutter test`).

### 4. Estrutura Completa de Arquivos — Novos e Modificados

```text
NOVOS ARQUIVOS (BACKEND):
backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/UserRole.java
backend/common/src/main/resources/db/changelog/25-add-role-to-users.yaml
backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/RequireAdmin.java
backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/AdminAccessGuard.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminObservabilityDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminUserMetricsDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminUploadMetricsDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminGrahamLatencyMetricsDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminLatencyPointDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AnonymizedUploadTraceDto.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminObservabilityService.java
backend/api/src/main/java/afsdigital/grahamselect/api/admin/web/AdminObservabilityController.java
backend/api/src/test/java/afsdigital/grahamselect/api/admin/application/service/AdminObservabilityServiceTest.java
backend/api/src/test/java/afsdigital/grahamselect/api/admin/infrastructure/security/AdminAccessGuardTest.java
backend/api/src/test/java/afsdigital/grahamselect/api/admin/web/AdminObservabilityControllerIT.java

MODIFICADOS (BACKEND):
backend/common/src/main/resources/db/changelog/db.changelog-master.yaml
backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java
backend/common/src/main/java/afsdigital/grahamselect/common/user/infrastructure/persistence/UserRepository.java
backend/common/src/main/java/afsdigital/grahamselect/common/ingestion/infrastructure/persistence/jpa/repositories/IngestionAuditRepository.java
backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java
backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java
backend/api/src/main/resources/openapi.yaml

NOVOS ARQUIVOS (FRONTEND):
frontend/lib/src/features/admin/domain/entities/admin_observability.dart
frontend/lib/src/features/admin/domain/repositories/admin_repository.dart
frontend/lib/src/features/admin/data/models/admin_observability_model.dart
frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart
frontend/lib/src/features/admin/data/repositories/admin_repository_impl.dart
frontend/lib/src/features/admin/presentation/providers/admin_observability_provider.dart
frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart
frontend/lib/src/features/admin/presentation/widgets/admin_metric_charts.dart
frontend/test/features/admin/data/models/admin_observability_model_test.dart
frontend/test/features/admin/presentation/providers/admin_observability_provider_test.dart
frontend/test/features/admin/presentation/pages/admin_dashboard_page_test.dart

MODIFICADOS (FRONTEND):
frontend/lib/src/features/profile/data/models/user_profile_model.dart
frontend/lib/src/core/widgets/main_layout.dart
frontend/lib/main.dart
```

### References

- [Source: `docs/bmad/planning-artifacts/epics.md` — Epic 6, Story 6.1: Painel de Monitoramento Admin]
- [Source: `docs/bmad/planning-artifacts/prd.md` — FR26, NFR3, NFR5]
- [Source: `docs/bmad/planning-artifacts/architecture.md` — D5 RBAC (`USER`, `ADMIN`), D7 API Design, D8 Error Handling RFC 7807, D12 Estrutura Flutter `lib/src/features/`, D14 Observabilidade Actuator + Micrometer]
- [Source: `docs/bmad/planning-artifacts/ux-design-specification.md` — Paleta de Cores, Skeleton Screens, Dashboard Layout]
- [Source: `docs/bmad/project-context.md` — Regras de BigDecimal, UTC, camelCase, Provider e Testes]
- [Source: `AGENTS.md` — Regras de Branch, Validação com Testes/Analyze e Logs Obrigatórios]

## Dev Agent Record

### Agent Model Used

Gemini 4 Argon (Medium)

### Debug Log References

- `mvn test` (com JDK 21/22): 100% dos testes unitários e de integração passando nos módulos `common`, `api-service` e `valuation-service`.
- `flutter analyze` & `flutter test`: 0 issues estáticos e 168 testes passando sem regressões.

### Completion Notes List

- Ultimate context engine analysis completed - comprehensive developer guide created and validated against real codebase structure.
- Implementado `UserRole` (`USER`, `ADMIN`), migração Liquibase `25-add-role-to-users.yaml`, evolução da entidade `User` com fallback `@PrePersist` e queries agregadas em `UserRepository` e `IngestionAuditRepository`.
- Implementado suporte completo a RBAC `ADMIN` via JWT claims (`role`/`roles`) e banco de dados no `OAuth2UserProvisioningConverter`, anotação `@RequireAdmin` e `AdminAccessGuard` com respostas RFC 7807 (`403 Forbidden`).
- Implementado endpoint `GET /api/v1/admin/observability` (`AdminObservabilityController` e `AdminObservabilityService`) com métricas de usuários ativos (24h UTC), deduplicação de uploads B3 por `correlationId`, latência do motor Graham via `MeterRegistry` (SLA < 2000ms) e anonimização estrita SHA-256 (`usr_***xxxx`, `extrato_b3_***.xlsx`, `***`).
- Implementada feature Flutter `admin` (`/admin/dashboard`) com Skeleton Screen, 3 cards gráficos nativos, listagem de tracing anonimizado LGPD, tratamento de `403 Forbidden` ("Acesso Restrito a Administradores") e botão condicional na TopBar para perfis `ADMIN`.

### File List

- `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/UserRole.java` (new)
- `backend/common/src/main/resources/db/changelog/25-add-role-to-users.yaml` (new)
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml` (modified)
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/domain/entities/User.java` (modified)
- `backend/common/src/main/java/afsdigital/grahamselect/common/user/infrastructure/persistence/UserRepository.java` (modified)
- `backend/common/src/main/java/afsdigital/grahamselect/common/ingestion/infrastructure/persistence/jpa/repositories/IngestionAuditRepository.java` (modified)
- `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/OAuth2UserProvisioningConverter.java` (modified)
- `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/RequireAdmin.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/AdminAccessGuard.java` (new)
- `backend/api/src/main/resources/openapi.yaml` (modified)
- `backend/api/src/main/java/afsdigital/grahamselect/api/user/web/UserController.java` (modified)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminObservabilityDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminUserMetricsDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminUploadMetricsDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminGrahamLatencyMetricsDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AdminLatencyPointDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/dto/AnonymizedUploadTraceDto.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/application/service/AdminObservabilityService.java` (new)
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/web/AdminObservabilityController.java` (new)
- `backend/api/src/test/java/afsdigital/grahamselect/api/admin/application/service/AdminObservabilityServiceTest.java` (new)
- `backend/api/src/test/java/afsdigital/grahamselect/api/admin/infrastructure/security/AdminAccessGuardTest.java` (new)
- `backend/api/src/test/java/afsdigital/grahamselect/api/admin/web/AdminObservabilityControllerIT.java` (new)
- `frontend/lib/src/features/profile/data/models/user_profile_model.dart` (modified)
- `frontend/lib/src/features/admin/domain/entities/admin_observability.dart` (new)
- `frontend/lib/src/features/admin/domain/repositories/admin_repository.dart` (new)
- `frontend/lib/src/features/admin/data/models/admin_observability_model.dart` (new)
- `frontend/lib/src/features/admin/data/datasources/admin_remote_datasource.dart` (new)
- `frontend/lib/src/features/admin/data/repositories/admin_repository_impl.dart` (new)
- `frontend/lib/src/features/admin/presentation/providers/admin_observability_provider.dart` (new)
- `frontend/lib/src/features/admin/presentation/widgets/admin_metric_charts.dart` (new)
- `frontend/lib/src/features/admin/presentation/pages/admin_dashboard_page.dart` (new)
- `frontend/lib/src/core/widgets/main_layout.dart` (modified)
- `frontend/lib/main.dart` (modified)
- `frontend/test/features/admin/data/models/admin_observability_model_test.dart` (new)
- `frontend/test/features/admin/presentation/providers/admin_observability_provider_test.dart` (new)
- `frontend/test/features/admin/presentation/pages/admin_dashboard_page_test.dart` (new)

## Change Log

- 2026-10-25: Implementação completa da Story 6.1 (Painel de Monitoramento Admin `/admin/dashboard` e API `GET /api/v1/admin/observability` com RBAC `ADMIN` e anonimização LGPD).
- 2026-10-25: Code Review adversarial concluído (Blind Hunter, Edge Case Hunter, Acceptance Auditor). Aplicados 4 patches de revisão (`UserController` logs/null-safety, `AdminObservabilityService` merge de metadados/status na consolidação e teto de `activeUsers24h`, `AdminObservabilityProvider` guard pós-dispose, `AdminGrahamLatencyCard` fallback de histórico vazio + teste de widget para `MainLayout`). Status atualizado para `done`.
