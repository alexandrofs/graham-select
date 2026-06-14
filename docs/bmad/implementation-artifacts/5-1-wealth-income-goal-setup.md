---
baseline_commit: 310100c521ed1bd296df02c00cd2a07cab605524
---

# Story 5.1: Configuração de Metas de Patrimônio e Renda

Status: done

## Story

**As a** investidor,
**I want** definir um objetivo financeiro (Valor de Patrimônio Alvo em R$ ou Renda Passiva Mensal Alvo em R$), com prazo estimado e aporte mensal pretendido,
**So that** o sistema salve minha meta de independência financeira no perfil e exiba um resumo imediato do quanto falta para atingi-la.

## Acceptance Criteria

1. **Given** que o usuário (autenticado) acessa a tela de "Minhas Metas" (rota `/metas`)
   **When** a tela carrega
   **Then** o sistema exibe um formulário com os campos:
   - **Tipo de Meta** (SegmentedButton): "Patrimônio Alvo" ou "Renda Mensal Alvo"
   - **Valor Alvo** (campo monetário com máscara R$): valor numérico positivo obrigatório
   - **Aporte Mensal Pretendido** (campo monetário R$): valor numérico positivo obrigatório
   - **Prazo Estimado** (campo em anos): número inteiro positivo (1–50), obrigatório
   - Botão primário **"Salvar Meta"** (FilledButton)
   **And** se o usuário já possui uma meta salva, os campos devem ser pré-preenchidos com os valores existentes

2. **Given** que o usuário preenche o formulário corretamente
   **When** ele clica em "Salvar Meta"
   **Then** o sistema valida os dados (todos os campos obrigatórios, valores positivos, prazo entre 1 e 50 anos)
   **And** chama `POST /api/v1/financial-goals` (criação) ou `PUT /api/v1/financial-goals/{goalId}` (atualização) com o payload
   **And** exibe um `SnackBar` de sucesso: "Meta salva com sucesso ✅"
   **And** atualiza a seção de resumo abaixo do formulário com os dados recém-salvos

3. **Given** que o usuário submeteu a meta com sucesso
   **When** a tela exibe o resumo da meta
   **Then** o sistema exibe um **card de resumo** com:
   - Tipo de meta selecionado
   - Valor Alvo formatado em R$
   - Aporte Mensal Pretendido em R$
   - Prazo Estimado em anos
   - Frase motivacional: **"Você precisa de R$ X para atingir sua meta."** (onde X = `valorAlvo - patrimonioAtualEstimado`)
   **And** o patrimônio atual estimado é obtido do endpoint `GET /api/v1/portfolio/summary` (campo `totalValue` existente da Story 3.1)
   **And** se o portfólio ainda não foi configurado, o patrimônio atual é tratado como R$ 0,00

4. **Given** que o usuário preenche o formulário com dados inválidos
   **When** ele clica em "Salvar Meta" ou sai de um campo obrigatório sem preenchê-lo
   **Then** o sistema exibe mensagens de erro inline abaixo de cada campo com problema:
   - Campo vazio: "Este campo é obrigatório"
   - Valor Alvo ≤ 0: "O valor deve ser maior que R$ 0,00"
   - Prazo fora do range: "O prazo deve estar entre 1 e 50 anos"
   **And** o botão "Salvar Meta" permanece habilitado para nova tentativa
   **And** nenhuma chamada à API é realizada enquanto houver erros de validação

5. **Given** que o backend recebe `POST /api/v1/financial-goals`
   **When** o payload é válido e o usuário está autenticado
   **Then** o sistema persiste a meta na tabela `financial_goals` com os campos: `user_id`, `goal_type` (`PATRIMONY_TARGET` ou `MONTHLY_INCOME_TARGET`), `target_value`, `monthly_contribution`, `estimated_years`, `created_at`, `updated_at`
   **And** retorna `201 Created` com o body `{ "data": { "id": "...", ... } }`
   **And** loga em nível `INFO`: `"Financial goal created for user {userId}, type={goalType}, targetValue={targetValue}"`

6. **Given** que o backend recebe `PUT /api/v1/financial-goals/{goalId}`
   **When** o `goalId` pertence ao usuário autenticado
   **Then** o sistema atualiza os campos da meta e atualiza `updated_at`
   **And** retorna `200 OK` com o body atualizado
   **And** se o `goalId` não pertencer ao usuário autenticado, retorna `403 Forbidden` (RFC 7807 ProblemDetail)

7. **Given** que o backend recebe `GET /api/v1/financial-goals`
   **When** o usuário está autenticado
   **Then** retorna a meta ativa do usuário (ou lista vazia `[]` se não houver meta)
   **And** o frontend usa este endpoint para pré-preencher o formulário no carregamento da tela
   **And** loga em nível `INFO`: `"Fetching financial goals for user {userId}"`

8. **Given** que a implementação está concluída
   **When** o agente de dev verifica a estrutura
   **Then** a feature Flutter está em `frontend/lib/src/features/goals/`
   **And** o backend está no package `afsdigital.grahamselect.{módulo}.goals`
   **And** existe migration Liquibase `21-create-financial-goals-table.yaml` (numerada após a última migration existente `20-create-graham-recommendations-table.yaml`)

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domain + Application)

- [ ] **Task 1: Criar entidade de domínio `FinancialGoal`** (AC: 5, 6, 7)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/FinancialGoal.java`
  - [ ] Usar Lombok `@Builder`, `@Getter`, sem anotações Spring
  - [ ] Campos:
    ```java
    UUID id;
    String userId;
    GoalType goalType;          // enum: PATRIMONY_TARGET, MONTHLY_INCOME_TARGET
    BigDecimal targetValue;     // obrigatório, > 0
    BigDecimal monthlyContribution; // obrigatório, > 0
    int estimatedYears;         // 1–50
    OffsetDateTime createdAt;   // ZoneOffset.UTC obrigatório
    OffsetDateTime updatedAt;   // ZoneOffset.UTC obrigatório
    ```
  - [ ] Criar enum `GoalType` no mesmo package: `PATRIMONY_TARGET, MONTHLY_INCOME_TARGET`

- [ ] **Task 2: Criar Port interface `FinancialGoalRepository`** (AC: 5, 6, 7)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/repository/FinancialGoalRepository.java`
  - [ ] Métodos:
    ```java
    Optional<FinancialGoal> findByUserId(String userId);
    FinancialGoal save(FinancialGoal goal);
    Optional<FinancialGoal> findByIdAndUserId(UUID id, String userId);
    ```

- [ ] **Task 3: Criar DTO `FinancialGoalDto`** (AC: 5, 6, 7)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/FinancialGoalDto.java`
  - [ ] Record Java com campos: `id` (UUID), `goalType` (String), `targetValue` (BigDecimal), `monthlyContribution` (BigDecimal), `estimatedYears` (int), `createdAt`, `updatedAt`
  - [ ] Anotação `@JsonInclude(JsonInclude.Include.NON_NULL)`

- [ ] **Task 4: Criar DTO de request `CreateFinancialGoalRequest`** (AC: 5, 6)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/CreateFinancialGoalRequest.java`
  - [ ] Record com validações Bean Validation:
    ```java
    @NotNull String goalType;       // "PATRIMONY_TARGET" ou "MONTHLY_INCOME_TARGET"
    @NotNull @Positive BigDecimal targetValue;
    @NotNull @Positive BigDecimal monthlyContribution;
    @Min(1) @Max(50) int estimatedYears;
    ```

- [ ] **Task 5: Criar Use Case `CreateFinancialGoalUseCase`** (AC: 5)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/CreateFinancialGoalUseCase.java`
  - [ ] POJO com método `execute(String userId, CreateFinancialGoalRequest request): FinancialGoalDto`
  - [ ] Usar `@Slf4j` + `@RequiredArgsConstructor` (Lombok)
  - [ ] Log INFO: `"Creating financial goal for user {}, type={}, targetValue={}"` na entrada
  - [ ] Construir `FinancialGoal` com `id = UUID.randomUUID()`, `createdAt = OffsetDateTime.now(ZoneOffset.UTC)`, `updatedAt = OffsetDateTime.now(ZoneOffset.UTC)`
  - [ ] Chamar `repository.save(goal)` e retornar DTO mapeado

- [ ] **Task 6: Criar Use Case `UpdateFinancialGoalUseCase`** (AC: 6)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/UpdateFinancialGoalUseCase.java`
  - [ ] POJO com método `execute(UUID goalId, String userId, CreateFinancialGoalRequest request): FinancialGoalDto`
  - [ ] Log INFO: `"Updating financial goal {} for user {}"` na entrada
  - [ ] Verificar existência via `repository.findByIdAndUserId(goalId, userId)` — lançar `FinancialGoalNotFoundException` se não encontrado ou não pertencer ao usuário
  - [ ] Atualizar os campos e `updatedAt = OffsetDateTime.now(ZoneOffset.UTC)`
  - [ ] Chamar `repository.save(updatedGoal)` e retornar DTO

- [ ] **Task 7: Criar Use Case `GetFinancialGoalUseCase`** (AC: 7)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/GetFinancialGoalUseCase.java`
  - [ ] Método `execute(String userId): Optional<FinancialGoalDto>`
  - [ ] Log INFO: `"Fetching financial goals for user {}"` na entrada
  - [ ] Chamar `repository.findByUserId(userId)` e mapear para DTO

- [ ] **Task 8: Criar exceção de domínio `FinancialGoalNotFoundException`** (AC: 6)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/service/exceptions/FinancialGoalNotFoundException.java`
  - [ ] Estender `RuntimeException`

---

### Backend — Módulo `api` (Infrastructure + REST)

- [ ] **Task 9: Criar migration Liquibase `21-create-financial-goals-table.yaml`** (AC: 5, 8)
  - [ ] Criar `backend/common/src/main/resources/db/changelog/21-create-financial-goals-table.yaml`
  - [ ] Estrutura da tabela:
    ```yaml
    tableName: financial_goals
    columns:
      - id: VARCHAR(36) NOT NULL PRIMARY KEY
      - user_id: VARCHAR(255) NOT NULL (index: idx_financial_goals_user_id)
      - goal_type: VARCHAR(50) NOT NULL  # 'PATRIMONY_TARGET' | 'MONTHLY_INCOME_TARGET'
      - target_value: DECIMAL(18,2) NOT NULL
      - monthly_contribution: DECIMAL(18,2) NOT NULL
      - estimated_years: INT NOT NULL
      - created_at: DATETIME NOT NULL
      - updated_at: DATETIME NOT NULL
    constraints:
      - unique: uq_financial_goals_user_id (uma meta ativa por usuário — simplificação MVP)
    ```
  - [ ] Adicionar ao `db.changelog-master.yaml` **após** `20-create-graham-recommendations-table.yaml`

- [ ] **Task 10: Criar entidade JPA `FinancialGoalEntity` no módulo `api`** (AC: 5, 6, 7)
  - [ ] Criar `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/persistence/jpa/entities/FinancialGoalEntity.java`
  - [ ] Anotações: `@Entity @Table(name = "financial_goals") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
  - [ ] Campo `id` com `@Id @JdbcTypeCode(SqlTypes.VARCHAR) private UUID id;` (padrão do módulo `api`)
  - [ ] Campos mapeados com `@Column(name = "snake_case")`

- [ ] **Task 11: Criar `FinancialGoalJpaRepository`** (AC: 5, 6, 7)
  - [ ] Criar `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/persistence/jpa/repository/FinancialGoalJpaRepository.java`
  - [ ] Estender `JpaRepository<FinancialGoalEntity, UUID>`
  - [ ] Métodos: `Optional<FinancialGoalEntity> findByUserId(String userId);` e `Optional<FinancialGoalEntity> findByIdAndUserId(UUID id, String userId);`

- [ ] **Task 12: Criar `FinancialGoalRepositoryImpl` (adapter)** (AC: 5, 6, 7)
  - [ ] Criar `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/persistence/FinancialGoalRepositoryImpl.java`
  - [ ] Implementar port `FinancialGoalRepository` do `common`
  - [ ] Mapear JPA entity ↔ domain entity (mapeamento manual ou via método estático)
  - [ ] **ATENÇÃO:** `goalType` deve ser convertido entre `String` (DB) e `GoalType` enum (domain)

- [ ] **Task 13: Criar `FinancialGoalsController`** (AC: 5, 6, 7)
  - [ ] Criar `backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java`
  - [ ] Endpoints:
    ```
    POST   /api/v1/financial-goals       → 201 Created, body: { "data": FinancialGoalDto }
    PUT    /api/v1/financial-goals/{id}  → 200 OK,     body: { "data": FinancialGoalDto }
    GET    /api/v1/financial-goals       → 200 OK,     body: { "data": FinancialGoalDto | null }
    ```
  - [ ] Log INFO no início de cada handler: `"Received request to [create|update|get] financial goal for user {}"` — obrigatório pelo AGENTS.md
  - [ ] Extrair `userId` do JWT: `jwt.getSubject()` via `@AuthenticationPrincipal Jwt jwt`
  - [ ] Usar `@Valid` na request body do POST e PUT
  - [ ] Tratar `FinancialGoalNotFoundException` → delegar para `@ControllerAdvice` global existente (mapeará para `403 Forbidden` via ProblemDetail)

- [ ] **Task 14: Criar `@Configuration` para wiring dos beans** (AC: 5, 6, 7)
  - [ ] Criar `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java`
  - [ ] Declarar beans: `CreateFinancialGoalUseCase`, `UpdateFinancialGoalUseCase`, `GetFinancialGoalUseCase`

---

### Frontend Flutter — Feature `goals`

- [ ] **Task 15: Criar estrutura da feature `goals`** (AC: 1, 8)
  - [ ] Criar diretórios:
    ```
    frontend/lib/src/features/goals/
    ├── data/
    │   ├── datasources/
    │   │   └── goals_remote_datasource.dart
    │   ├── models/
    │   │   └── financial_goal_model.dart
    │   └── repositories/
    │       └── goals_repository_impl.dart
    ├── domain/
    │   ├── entities/
    │   │   └── financial_goal.dart
    │   ├── repositories/
    │   │   └── goals_repository.dart
    │   └── usecases/
    │       └── save_financial_goal_usecase.dart
    └── presentation/
        ├── pages/
        │   └── goals_page.dart
        ├── providers/
        │   └── goals_provider.dart
        └── widgets/
            └── goal_summary_card.dart
    ```

- [ ] **Task 16: Criar entidade de domínio `FinancialGoal` (Dart)** (AC: 1, 3)
  - [ ] Criar `frontend/lib/src/features/goals/domain/entities/financial_goal.dart`
  - [ ] Campos com null safety:
    ```dart
    final String? id;
    final String goalType;        // 'PATRIMONY_TARGET' | 'MONTHLY_INCOME_TARGET'
    final double targetValue;
    final double monthlyContribution;
    final int estimatedYears;
    final DateTime? createdAt;
    final DateTime? updatedAt;
    ```
  - [ ] Construtor `const`, `copyWith`, e `GoalType` como enum Dart:
    ```dart
    enum GoalType { patrimonyTarget, monthlyIncomeTarget }
    ```

- [ ] **Task 17: Criar `FinancialGoalModel` (data layer)** (AC: 5, 6, 7)
  - [ ] Criar `frontend/lib/src/features/goals/data/models/financial_goal_model.dart`
  - [ ] Implementar `fromJson(Map<String, dynamic> json)` com parsing de todos os campos
  - [ ] Implementar `toJson()` para serializar o request payload
  - [ ] Implementar `toEntity()` retornando `FinancialGoal`

- [ ] **Task 18: Criar `GoalsRemoteDatasource`** (AC: 5, 6, 7)
  - [ ] Criar `frontend/lib/src/features/goals/data/datasources/goals_remote_datasource.dart`
  - [ ] Usar `Dio` (instância do `core/api/`)
  - [ ] Métodos:
    - `Future<FinancialGoalModel> createGoal(Map<String, dynamic> payload)` → `POST /api/v1/financial-goals`
    - `Future<FinancialGoalModel> updateGoal(String id, Map<String, dynamic> payload)` → `PUT /api/v1/financial-goals/{id}`
    - `Future<FinancialGoalModel?> fetchGoal()` → `GET /api/v1/financial-goals` (retorna null se lista vazia)
  - [ ] Try-catch em cada método; relançar como `Exception` com mensagem amigável

- [ ] **Task 19: Criar `GoalsRepository` interface e `GoalsRepositoryImpl`** (AC: 5, 6, 7)
  - [ ] Criar interface `frontend/lib/src/features/goals/domain/repositories/goals_repository.dart` com os mesmos métodos
  - [ ] Criar `frontend/lib/src/features/goals/data/repositories/goals_repository_impl.dart` delegando para `GoalsRemoteDatasource`

- [ ] **Task 20: Criar `GoalsProvider`** (AC: 1, 2, 3, 4)
  - [ ] Criar `frontend/lib/src/features/goals/presentation/providers/goals_provider.dart`
  - [ ] Estender `ChangeNotifier`
  - [ ] Estados (seguir padrão `PortfolioProvider`):
    ```dart
    enum GoalsStatus { initial, loading, success, error }
    ```
  - [ ] Campos de estado: `_status`, `_errorMessage`, `_currentGoal` (FinancialGoal?)
  - [ ] Método `loadGoal()` → chama `repository.fetchGoal()` e popula `_currentGoal`
  - [ ] Método `saveGoal({...})` → detecta se é criação ou update via `_currentGoal?.id`, chama o método correto, atualiza estado
  - [ ] Getters: `status`, `errorMessage`, `currentGoal`

- [ ] **Task 21: Criar `GoalSummaryCard` widget** (AC: 3)
  - [ ] Criar `frontend/lib/src/features/goals/presentation/widgets/goal_summary_card.dart`
  - [ ] Parâmetros: `required FinancialGoal goal`, `required double currentPatrimony`
  - [ ] Layout (Card com padding 16dp):
    - Header: ícone 🎯 + título do tipo de meta em `titleLarge`
    - Linha: "Valor Alvo: **R$ X.XXX,XX**" (JetBrains Mono para o valor)
    - Linha: "Aporte Mensal: **R$ X.XXX,XX**"
    - Linha: "Prazo Estimado: **X anos**"
    - **Divider**
    - **Frase motivacional** em destaque (bodyLarge bold, cor Emerald `#10B981`):
      - Se `targetValue > currentPatrimony`: `"Você precisa de R$ X para atingir sua meta."` (X = targetValue - currentPatrimony)
      - Se `currentPatrimony >= targetValue`: `"🎉 Parabéns! Você já atingiu sua meta de patrimônio!"`
    - Se `goal.goalType == 'MONTHLY_INCOME_TARGET'`: frase: `"Meta: gerar R$ X/mês de renda passiva."`
  - [ ] Paleta de cores: usar `const Color(0xFF10B981)` (Emerald) para sucesso, `const Color(0xFFF59E0B)` (AmberGold) para destaque de falta
  - [ ] **NÃO usar** `withOpacity()` — usar `withAlpha((0.X * 255).round())` (linting Flutter 3.11+)

- [ ] **Task 22: Criar `GoalsPage`** (AC: 1, 2, 3, 4)
  - [ ] Criar `frontend/lib/src/features/goals/presentation/pages/goals_page.dart`
  - [ ] Estrutura geral:
    ```
    Scaffold
    └── SingleChildScrollView
        └── Column (padding: lg=24)
            ├── Text("Minhas Metas", style: headlineMedium)
            ├── [Formulário de Meta]   ← seção editável
            ├── SizedBox(height: 24)
            └── [GoalSummaryCard]      ← visível apenas após salvar
    ```
  - [ ] **Formulário:**
    - `SegmentedButton<String>` para tipo de meta: "Patrimônio Alvo" / "Renda Mensal Alvo"
    - `TextFormField` para Valor Alvo (máscara R$, `keyboardType: TextInputType.number`)
    - `TextFormField` para Aporte Mensal (máscara R$)
    - `TextFormField` para Prazo (em anos, `keyboardType: TextInputType.number`)
    - `FilledButton("Salvar Meta")` full-width
  - [ ] Usar `GlobalKey<FormState>` para validação
  - [ ] Regras de validação inline (AC 4): campo vazio, valor ≤ 0, prazo fora do range (1–50)
  - [ ] No `initState`: chamar `context.read<GoalsProvider>().loadGoal()` e pré-preencher campos se `currentGoal != null`
  - [ ] Ao submeter com sucesso: exibir `SnackBar` verde com `"Meta salva com sucesso ✅"`
  - [ ] Ao submeter com erro: exibir `SnackBar` vermelho com a mensagem de erro do provider
  - [ ] Após salvar: chamar `context.read<PortfolioProvider>().loadSummary()` para obter `totalValue` atualizado para o `GoalSummaryCard`
  - [ ] Skeleton screen (shimmer) durante `GoalsStatus.loading`

- [ ] **Task 23: Registrar provider e rota** (AC: 1, 8)
  - [ ] Adicionar `GoalsProvider` no `MultiProvider` da `main.dart` (ou onde estão registrados os demais providers)
  - [ ] Registrar rota `/metas` no `GoRouter` apontando para `GoalsPage`
  - [ ] Adicionar item na `NavigationRail`/`NavigationBar` com ícone `Icons.flag_outlined` e label "Metas"

---

### Testes

- [ ] **Task 24: Testes unitários do backend — `CreateFinancialGoalUseCaseTest`** (AC: 5)
  - [ ] Criar `backend/common/src/test/java/.../goals/application/usecase/CreateFinancialGoalUseCaseTest.java`
  - [ ] Testar: payload válido → `repository.save()` chamado com valores corretos, retorna DTO com `id` e `createdAt` preenchidos
  - [ ] Testar: `goalType` inválido → exceção adequada
  - [ ] Testar: `estimatedYears` fora do range → exceção de validação (via Bean Validation no controller, mas se o use case também validar, testar aqui)
  - [ ] Usar Mockito para mockar `FinancialGoalRepository`

- [ ] **Task 25: Testes unitários do backend — `UpdateFinancialGoalUseCaseTest`** (AC: 6)
  - [ ] Criar `backend/common/src/test/java/.../goals/application/usecase/UpdateFinancialGoalUseCaseTest.java`
  - [ ] Testar: update de meta existente → `repository.save()` com `updatedAt` atualizado
  - [ ] Testar: `goalId` não pertence ao usuário → `FinancialGoalNotFoundException` lançada

- [ ] **Task 26: Testes de widget Flutter — `GoalsPageTest`** (AC: 1, 2, 4)
  - [ ] Criar `frontend/test/features/goals/presentation/pages/goals_page_test.dart`
  - [ ] Testar: tela carrega com formulário vazio (sem meta prévia)
  - [ ] Testar: formulário pré-preenchido quando `GoalsProvider.currentGoal` está carregado
  - [ ] Testar: validação — submeter com campos vazios → mensagens de erro exibidas
  - [ ] Testar: submissão bem-sucedida → SnackBar de sucesso visível
  - [ ] Usar `mockito` / `mocktail` para mockar `GoalsProvider`

- [ ] **Task 27: Teste de widget Flutter — `GoalSummaryCardTest`** (AC: 3)
  - [ ] Criar `frontend/test/features/goals/presentation/widgets/goal_summary_card_test.dart`
  - [ ] Testar: patrimônio atual < valor alvo → frase "Você precisa de R$ X..."
  - [ ] Testar: patrimônio atual ≥ valor alvo → frase de parabéns com emoji 🎉
  - [ ] Testar: `goalType == MONTHLY_INCOME_TARGET` → frase de renda mensal

- [ ] **Task 28: Executar validação final** (AC: todos)
  - [ ] `mvn clean test -pl common` → zero falhas
  - [ ] `mvn clean test -pl api` → zero falhas
  - [ ] `flutter analyze` → zero lints
  - [ ] `flutter test test/features/goals/` → todos os testes passam

### Review Findings

- [x] [Review][Patch] Erro conceitual de cálculo na meta de Renda Mensal Alvo no Frontend [frontend/lib/src/features/goals/presentation/widgets/goal_summary_card.dart:29-30] — aplicado
- [x] [Review][Patch] Formato de resposta do endpoint GET violando especificação (Objeto vs. Array) [backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java:270-276] — aplicado
- [x] [Review][Patch] Uso incorreto do campo de patrimônio acumulado no Frontend [frontend/lib/src/features/goals/presentation/pages/goals_page.dart:1456] — dismissed, totalValue é erro na especificação (campo correto é totalEquity)
- [x] [Review][Patch] Violação de ZoneOffset.UTC nos testes unitários do backend [backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/CreateFinancialGoalUseCaseTest.java:708-709] — aplicado
- [x] [Review][Patch] Risco de erro 500 (Unique Key Violation) na criação de meta [backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/CreateFinancialGoalUseCase.java:38] — aplicado
- [x] [Review][Patch] Conversão insegura de Enum no repository JPA do backend [backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/persistence/FinancialGoalRepositoryImpl.java:43] — aplicado
- [x] [Review][Patch] Log de nível ERROR para exceções de negócio no GlobalExceptionHandler [backend/api/src/main/java/afsdigital/grahamselect/api/common/web/GlobalExceptionHandler.java:30] — dismissed, logs de nível ERROR em handlers são exigidos por AGENTS.md
- [x] [Review][Patch] Ausência de tela de Shimmer no carregamento da tela de metas no Frontend [frontend/lib/src/features/goals/presentation/pages/goals_page.dart:1448-1454] — aplicado
- [x] [Review][Patch] Validação fraca do goalType na requisição do Backend [backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/CreateFinancialGoalRequest.java:15] — aplicado
- [x] [Review][Patch] Ausência de log de nível ERROR nos lançamentos de exceções no Use Case [backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/UpdateFinancialGoalUseCase.java:504-506] — aplicado
- [x] [Review][Patch] Ausência de log de sucesso concluído na criação de meta [backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/CreateFinancialGoalUseCase.java:400-401] — aplicado
- [x] [Review][Defer] Falta de controle de múltiplos cliques (Debounce) no formulário do Frontend [frontend/lib/src/features/goals/presentation/pages/goals_page.dart:1456] — deferred, pre-existing

---

## Dev Notes

### 1. Contexto Arquitetural — O que Já Existe (REUTILIZAR)

**Épicos 1–4 estabeleceram toda a infraestrutura. Esta é a PRIMEIRA história do Épico 5:**

```
Infraestrutura existente pós-Epic 4:
backend/
├── common/
│   ├── upload/... (padrão de use cases estabelecido)
│   └── valuation/
│       ├── domain/entities/GrahamRecommendation.java    ← padrão de entidade de domínio
│       └── application/dto/GrahamRecommendationDto.java ← padrão de DTO (record Java)
├── api/
│   ├── config/SecurityConfig.java                      ← JWT auth já configurado
│   ├── valuation/web/GrahamRecommendationsController.java ← padrão de controller REST
│   └── common/infrastructure/spring/...               ← padrão @Configuration
└── common/src/main/resources/db/changelog/
    └── 20-create-graham-recommendations-table.yaml     ← ÚLTIMA migration (próxima = 21)

frontend/
├── core/
│   └── api/ (Dio client configurado com JWT interceptor)
├── features/
│   ├── portfolio/presentation/providers/portfolio_provider.dart ← PADRÃO de provider
│   ├── ranking/presentation/pages/graham_recommendations_page.dart
│   └── allocation/ (padrão de feature completa a seguir)
└── pubspec.yaml (verificar se máscara monetária está disponível)
```

### 2. Padrão de Provider Flutter — CRÍTICO

**Seguir exatamente o padrão do `PortfolioProvider` (história 3.1):**
```dart
// ✅ Padrão estabelecido — OBRIGATÓRIO seguir
enum GoalsStatus { initial, loading, success, error }

class GoalsProvider extends ChangeNotifier {
  final GoalsRepository repository;
  GoalsProvider(this.repository);

  GoalsStatus _status = GoalsStatus.initial;
  GoalsStatus get status => _status;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  FinancialGoal? _currentGoal;
  FinancialGoal? get currentGoal => _currentGoal;

  Future<void> loadGoal() async {
    _status = GoalsStatus.loading;
    _errorMessage = null;
    notifyListeners();
    try {
      _currentGoal = await repository.fetchGoal();
      _status = GoalsStatus.success;
    } catch (e) {
      _status = GoalsStatus.error;
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
    }
    notifyListeners();
  }
  // ... saveGoal() seguindo mesmo padrão
}
```

### 3. Padrão de Controller Backend — CRÍTICO

**Seguir exatamente o padrão do `GrahamRecommendationsController`:**
```java
// ✅ Log INFO obrigatório na entrada de TODA API (AGENTS.md rule)
@RestController
@RequestMapping("/api/v1/financial-goals")
@RequiredArgsConstructor
@Slf4j
public class FinancialGoalsController {

    private final CreateFinancialGoalUseCase createUseCase;
    private final UpdateFinancialGoalUseCase updateUseCase;
    private final GetFinancialGoalUseCase getUseCase;

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateFinancialGoalRequest request) {
        String userId = jwt.getSubject();
        log.info("Received request to create financial goal for user {}", userId); // OBRIGATÓRIO
        FinancialGoalDto result = createUseCase.execute(userId, request);
        return ResponseEntity.status(201).body(Map.of("data", result));
    }
    // ... PUT e GET seguindo o mesmo padrão
}
```

### 4. Regras de Tempo e Valores Monetários — CRÍTICO

```java
// ✅ OBRIGATÓRIO: ZoneOffset.UTC em TODAS as operações temporais
OffsetDateTime.now(ZoneOffset.UTC)  // não usar LocalDateTime.now()

// ✅ OBRIGATÓRIO: BigDecimal para valores monetários (NUNCA double/float no Java)
BigDecimal targetValue;
BigDecimal monthlyContribution;
```

```dart
// ✅ Dart: double para valores monetários (padrão Flutter existente)
final double targetValue;

// ✅ NÃO usar withOpacity() — Flutter 3.11+ deprecou
// ❌ ERRADO:
color: Colors.green.withOpacity(0.1)
// ✅ CORRETO:
color: Colors.green.withAlpha((0.1 * 255).round())
```

### 5. Estrutura do Banco de Dados — Constraint de Unicidade

**Decisão de MVP:** A tabela `financial_goals` tem constraint `UNIQUE(user_id)` — cada usuário tem UMA meta ativa. Isso simplifica a lógica do frontend (não há listagem de múltiplas metas). O endpoint `GET /api/v1/financial-goals` retorna a meta do usuário (singular), mas o response body é um array `{ "data": [...] }` por consistência com o padrão de API do projeto — o frontend pega o primeiro elemento.

**Se no futuro (Story 5.x) for necessário múltiplas metas simultâneas, a constraint pode ser removida via nova migration.**

### 6. Máscara Monetária (Flutter) — Verificar Dependência

**Verificar se `pubspec.yaml` já possui um pacote de máscara (ex: `intl`, `flutter_masked_text2`, ou similar):**
```bash
grep -i 'masked\|mask\|intl\|money' frontend/pubspec.yaml
```

- Se `intl` já está incluído (é provável — usado em formatação de datas nos épicos anteriores), usar `NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$')` para formatar e criar controller manual para máscara
- Se não houver pacote de máscara, implementar via `TextInputFormatter` custom ou adicionar `flutter_masked_text2: ^0.8.3` ao `pubspec.yaml`

### 7. Integração com `PortfolioProvider` para Patrimônio Atual

**`GoalSummaryCard` precisa do patrimônio atual do usuário (para calcular "falta R$ X"):**

```dart
// Em goals_page.dart — consumir o PortfolioProvider existente (da Story 3.1):
final portfolioProvider = context.watch<PortfolioProvider>();
final currentPatrimony = portfolioProvider.summary?.totalValue ?? 0.0;

// Após salvar meta, recarregar summary para valor atualizado:
await context.read<GoalsProvider>().saveGoal(...);
await context.read<PortfolioProvider>().loadSummary();
```

**NÃO criar novo endpoint para patrimônio — reutilizar `GET /api/v1/portfolio/summary` já existente.**

### 8. Estrutura de Arquivos — Novos e Modificados

```
NOVOS (Backend):
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/FinancialGoal.java
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalType.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/repository/FinancialGoalRepository.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/FinancialGoalDto.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/CreateFinancialGoalRequest.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/CreateFinancialGoalUseCase.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/UpdateFinancialGoalUseCase.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/GetFinancialGoalUseCase.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/service/exceptions/FinancialGoalNotFoundException.java
backend/common/src/main/resources/db/changelog/21-create-financial-goals-table.yaml
backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/persistence/jpa/entities/FinancialGoalEntity.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/persistence/jpa/repository/FinancialGoalJpaRepository.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/persistence/FinancialGoalRepositoryImpl.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java
backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/CreateFinancialGoalUseCaseTest.java
backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/UpdateFinancialGoalUseCaseTest.java

NOVOS (Frontend):
frontend/lib/src/features/goals/domain/entities/financial_goal.dart
frontend/lib/src/features/goals/domain/repositories/goals_repository.dart
frontend/lib/src/features/goals/domain/usecases/save_financial_goal_usecase.dart (opcional)
frontend/lib/src/features/goals/data/models/financial_goal_model.dart
frontend/lib/src/features/goals/data/datasources/goals_remote_datasource.dart
frontend/lib/src/features/goals/data/repositories/goals_repository_impl.dart
frontend/lib/src/features/goals/presentation/providers/goals_provider.dart
frontend/lib/src/features/goals/presentation/pages/goals_page.dart
frontend/lib/src/features/goals/presentation/widgets/goal_summary_card.dart
frontend/test/features/goals/presentation/pages/goals_page_test.dart
frontend/test/features/goals/presentation/widgets/goal_summary_card_test.dart

MODIFICADOS:
backend/common/src/main/resources/db/changelog/db.changelog-master.yaml  (adicionar entry 21)
frontend/lib/main.dart                                                     (registrar GoalsProvider)
frontend/lib/src/core/routing/app_router.dart                              (adicionar rota /metas)
frontend/lib/src/features/home/presentation/pages/home_page.dart           (ou equivalente — add nav item)
```

### 9. Regras de Segurança e Isolamento de Tenant

```java
// ✅ OBRIGATÓRIO: SEMPRE filtrar por userId extraído do JWT
String userId = jwt.getSubject();  // extração obrigatória em cada endpoint

// ✅ PUT: verificar ownership antes de update
repository.findByIdAndUserId(goalId, userId)
    .orElseThrow(() -> new FinancialGoalNotFoundException(...));
    // → ControllerAdvice mapeia para 403 Forbidden

// ✅ GET: retorna apenas meta DO usuário autenticado
repository.findByUserId(userId);
```

**Nenhuma query deve acessar metas de outros usuários — isolamento mandatório (NFR4).**

### 10. Checklist de Linting Flutter (AGENTS.md)

```dart
// ❌ ERRADO — withOpacity está deprecado no Flutter 3.11+
color: emeraldColor.withOpacity(0.1)

// ✅ CORRETO
color: Color.fromARGB((0.1 * 255).round(), 16, 185, 129)

// ❌ ERRADO — surfaceVariant deprecado
Theme.of(context).colorScheme.surfaceVariant

// ✅ CORRETO
Theme.of(context).colorScheme.surfaceContainerHighest

// ❌ ERRADO — BuildContext across async gap sem mounted check
await saveGoal();
ScaffoldMessenger.of(context).showSnackBar(...); // pode crashar

// ✅ CORRETO
await saveGoal();
if (!mounted) return;
ScaffoldMessenger.of(context).showSnackBar(...);
```

### 11. Padrão de Migration Liquibase

**Seguir o estilo das migrations anteriores do projeto:**
```yaml
# 21-create-financial-goals-table.yaml
databaseChangeLog:
  - changeSet:
      id: 21-create-financial-goals-table
      author: graham-select-dev
      changes:
        - createTable:
            tableName: financial_goals
            columns:
              - column:
                  name: id
                  type: VARCHAR(36)
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: user_id
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
              # ... demais campos
        - createIndex:
            indexName: idx_financial_goals_user_id
            tableName: financial_goals
            columns:
              - column:
                  name: user_id
        - addUniqueConstraint:
            tableName: financial_goals
            columnNames: user_id
            constraintName: uq_financial_goals_user_id
```

### 12. Paleta de Cores Obrigatória (UX Design Spec)

```dart
// Design tokens do projeto — OBRIGATÓRIO usar
const Color kNavyBlue = Color(0xFF1B2A4A);   // backgrounds, headers
const Color kEmerald = Color(0xFF10B981);    // sucesso, metas atingidas
const Color kAmberGold = Color(0xFFF59E0B); // destaque, progresso, falta de meta
const Color kErrorRed = Color(0xFFEF4444);  // erro, valores negativos

// GoalSummaryCard — uso correto das cores:
// "Falta R$ X" → kAmberGold (destaque de atenção)
// "Meta atingida! 🎉" → kEmerald (celebração)
// Botão "Salvar Meta" → FilledButton com kEmerald como primary
```

### 13. Inteligência da História Anterior (4.4)

**Learnings críticos da Story 4.4 (Reasoning Box) aplicáveis aqui:**
- Pattern de `@Configuration` para wiring de use cases está consolidado — seguir o mesmo padrão em `GoalsServiceConfiguration.java`
- O padrão de `@JdbcTypeCode(SqlTypes.VARCHAR)` para UUID no módulo `api` é mandatório
- A entidade JPA e o domain entity SÃO DIFERENTES e DEVEM ser mapeados manualmente (sem usar a JPA entity diretamente no domain)
- Campos monetários no backend: sempre `BigDecimal` (nunca `double`)
- `@JsonInclude(JsonInclude.Include.NON_NULL)` nos DTOs de response

---

## References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 5.1]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 5: Metas & Projeção Financeira]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Design System Foundation]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Form Patterns]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Feedback Patterns]
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean Architecture — Padrão do Projeto]
- [Source: docs/bmad/planning-artifacts/architecture.md#Regras Obrigatórias para Agentes de IA]
- [Source: docs/bmad/planning-artifacts/architecture.md#D12 - Estrutura Flutter]
- [Source: docs/bmad/project-context.md]
- [Source: docs/bmad/implementation-artifacts/4-4-explainable-ai-reasoning-box.md#Dev Notes]
- [Source: frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart]
- [Source: backend/common/src/main/resources/db/changelog/20-create-graham-recommendations-table.yaml]
