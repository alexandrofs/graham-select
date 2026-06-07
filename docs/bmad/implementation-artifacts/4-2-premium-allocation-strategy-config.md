# Story 4.2: Configuração de Metas de Alocação (Tier Premium)

Status: ready-for-dev

## Story

**As a** investidor Premium,
**I want** definir uma meta percentual de alocação para cada classe de ativos (ex: 50% Ações, 30% FIIs) e para ativos específicos (ex: 10% PETR4),
**So that** o Motor de Graham (história 4.3) saiba onde eu quero chegar ao calcular recomendações de aporte personalizadas.

## Acceptance Criteria

1. **Given** que o usuário possui tier Premium ou Trial ativo
   **When** ele acessa "Configurações de Estratégia" (nova tela/rota)
   **Then** o sistema exibe o formulário de configuração de metas de alocação com estado atual (se já existir)
   **And** somente usuários Premium/Trial podem acessar — Gratuito recebe erro 403 via `@RequirePremium`

2. **Given** que o usuário define metas por classe (ex: Ações 50%, FIIs 30%, Renda Fixa 20%)
   **When** ele salva a configuração
   **Then** o backend valida que a **soma de todas as metas de classe fecha em exatamente 100%**
   **And** retorna erro 400 (RFC 7807 ProblemDetail) com mensagem clara se a soma for diferente de 100%
   **And** persiste o plano de alocação por usuário com isolamento de tenant (filtro por `userId`)

3. **Given** que o usuário define metas por ativo específico (ex: PETR4 10%, VALE3 8%)
   **When** ele salva a configuração
   **Then** o backend persiste essas metas adicionalmente ao plano por classe
   **And** NÃO valida soma para ativos específicos (metas granulares são complementares, não obrigatoriamente somam 100%)

4. **Given** que o usuário já possui metas salvas
   **When** ele acessa "Configurações de Estratégia"
   **Then** o sistema carrega e exibe as metas existentes para edição
   **And** o histórico anterior é substituído pelo novo plano salvo (upsert, não append)

5. **Given** que o formulário de metas está sendo preenchido
   **When** a soma das metas de classe diverge de 100%
   **Then** o frontend exibe indicador visual em tempo real mostrando a soma atual e o saldo restante (ex: "70% alocados, 30% restantes")

6. **Given** que o usuário tenta acessar a tela sem autenticação ou com tier Gratuito
   **When** o sistema avalia o acesso
   **Then** retorna HTTP 403 para chamadas de API e exibe tela/mensagem de "Funcionalidade Premium" no frontend

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domain + Application — POJOs puros, sem Spring)

- [ ] **Task 1: Criar entidade de domínio `AllocationGoal`** (AC: 2, 3, 4)
  - [ ] Criar `AllocationGoal` em `common/src/main/java/afsdigital/grahamselect/valuation/domain/entities/AllocationGoal.java`
  - [ ] Campos: `id` (UUID), `userId` (String), `goalType` (String: `"ASSET_CLASS"` | `"TICKER"`), `targetKey` (String — nome da classe ou ticker), `targetPercentage` (BigDecimal — ex: 50.00)
  - [ ] Usar Java record ou classe com Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
  - [ ] **SEM** anotações Spring ou JPA nesta camada (POJO puro)

- [ ] **Task 2: Criar Port `AllocationGoalPort`** (AC: 2, 3, 4)
  - [ ] Criar interface `AllocationGoalPort` em `common/src/main/java/afsdigital/grahamselect/valuation/application/repository/AllocationGoalPort.java`
  - [ ] Métodos:
    - `void saveAllocationGoals(String userId, List<AllocationGoalDto> goals)` — salva/substitui (upsert) todas as metas do usuário
    - `List<AllocationGoalDto> findByUserId(String userId)` — carrega metas existentes

- [ ] **Task 3: Criar DTO `AllocationGoalDto`** (AC: 2, 3, 4)
  - [ ] Criar record `AllocationGoalDto` em `common/src/main/java/afsdigital/grahamselect/valuation/application/dto/AllocationGoalDto.java`
  - [ ] Campos: `goalType` (String: `"ASSET_CLASS"` | `"TICKER"`), `targetKey` (String), `targetPercentage` (BigDecimal)

- [ ] **Task 4: Criar Use Case `SaveAllocationGoalsUseCase`** (AC: 2, 3, 5)
  - [ ] Criar `SaveAllocationGoalsUseCase` em `common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/SaveAllocationGoalsUseCase.java` como POJO (sem `@Component`)
  - [ ] Método `execute(String userId, List<AllocationGoalDto> goals)` — sem anotações Spring
  - [ ] **Validação obrigatória**: somar os `targetPercentage` onde `goalType = "ASSET_CLASS"` e verificar se a soma == 100.00 (com tolerância de 0.01 para precisão de ponto flutuante)
  - [ ] Se soma ≠ 100%: lançar exceção de domínio `AllocationGoalValidationException` com mensagem clara
  - [ ] Delega persistência para `AllocationGoalPort.saveAllocationGoals(userId, goals)`
  - [ ] Lombok `@RequiredArgsConstructor` + `@Slf4j`
  - [ ] Log INFO na entrada: `"Saving {} allocation goals for user {}"`

- [ ] **Task 5: Criar Use Case `GetAllocationGoalsUseCase`** (AC: 4)
  - [ ] Criar `GetAllocationGoalsUseCase` em `common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetAllocationGoalsUseCase.java` como POJO (sem `@Component`)
  - [ ] Método `execute(String userId)` → retorna `List<AllocationGoalDto>`
  - [ ] Log INFO na entrada: `"Fetching allocation goals for user {}"`

- [ ] **Task 6: Criar exceção de domínio `AllocationGoalValidationException`** (AC: 2)
  - [ ] Criar em `common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/exceptions/AllocationGoalValidationException.java`
  - [ ] Extends `RuntimeException` — **sem** anotações Spring

---

### Backend — Módulo `api` (Infrastructure + Web)

- [ ] **Task 7: Criar migration Liquibase para tabela `allocation_goals`** (AC: 2, 3, 4)
  - [ ] Criar arquivo `backend/common/src/main/resources/db/changelog/19-create-allocation-goals-table.yaml`
  - [ ] Schema da tabela `allocation_goals` (tipo UUID igual ao padrão `trades` e `manual_trade_audits`):
    ```yaml
    columns:
      - id: UUID PRIMARY KEY NOT NULL
      - user_id: VARCHAR(255) NOT NULL
      - goal_type: VARCHAR(20) NOT NULL  # 'ASSET_CLASS' | 'TICKER'
      - target_key: VARCHAR(20) NOT NULL  # nome da classe ou ticker (ex: 'ACOES', 'PETR4')
      - target_percentage: DECIMAL(8,4) NOT NULL
      - created_at: DATETIME NOT NULL
      - updated_at: DATETIME NOT NULL
    - Unique constraint: uq_allocation_goals_user_type_key (user_id, goal_type, target_key)
    - Index: idx_allocation_goals_user_id (user_id)
    ```
  - [ ] Incluir o arquivo no `db.changelog-master.yaml` (próxima entrada após `18-fix-manual-trade-audits-uuid-columns.yaml`)

- [ ] **Task 8: Criar entidade JPA `AllocationGoalEntity`** (AC: 2, 3, 4)
  - [ ] Criar em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/AllocationGoalEntity.java`
  - [ ] Anotações: `@Entity @Table(name = "allocation_goals") @Data @Builder @NoArgsConstructor @AllArgsConstructor`
  - [ ] Campo `id`: `UUID` com `@Id` + `@JdbcTypeCode(SqlTypes.VARCHAR)` — **padrão obrigatório do projeto** (ver `TradeEntity` e `ManualTradeAuditEntity`)
  - [ ] Demais campos: `userId` (String), `goalType` (String), `targetKey` (String), `targetPercentage` (BigDecimal), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime)

- [ ] **Task 9: Criar JPA Repository `AllocationGoalJpaRepository`** (AC: 2, 3, 4)
  - [ ] Criar interface `AllocationGoalJpaRepository` em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repositories/AllocationGoalJpaRepository.java`
  - [ ] Extends `JpaRepository<AllocationGoalEntity, UUID>` — ID é UUID, **não** Long
  - [ ] Método: `List<AllocationGoalEntity> findByUserId(String userId)`
  - [ ] Método: `void deleteByUserId(String userId)`

- [ ] **Task 10: Criar adapter `AllocationGoalJpaAdapter`** (AC: 2, 3, 4)
  - [ ] Criar `AllocationGoalJpaAdapter` em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java`
  - [ ] Implementa `AllocationGoalPort`
  - [ ] `saveAllocationGoals`: **primeiro `deleteByUserId(userId)`**, depois salva todas as novas metas via `saveAll()` (upsert via delete+insert — abordagem simples e segura para MVP)
  - [ ] Ao construir cada `AllocationGoalEntity` para insert: **gerar `UUID.randomUUID()`** para o campo `id` (o banco não gera automaticamente — padrão do projeto)
  - [ ] `findByUserId`: busca e mapeia `AllocationGoalEntity` → `AllocationGoalDto`
  - [ ] Setar `createdAt` e `updatedAt` com `LocalDateTime.now(ZoneOffset.UTC)` (**obrigatório**)

- [ ] **Task 11: Criar Controller `AllocationStrategyController`** (AC: 1, 2, 3, 4, 6)
  - [ ] Criar em `api/src/main/java/afsdigital/grahamselect/api/valuation/web/AllocationStrategyController.java`
  - [ ] `@RestController @RequestMapping("/api/v1/allocation-strategy") @RequiredArgsConstructor @Slf4j`
  - [ ] **Anotar a classe com `@RequirePremium`** — garante acesso apenas para Premium/Trial em todos os endpoints
  - [ ] **POST `/api/v1/allocation-strategy`**: salva/atualiza metas
    - `@PostMapping` + `@AuthenticationPrincipal Jwt jwt`
    - Log INFO na entrada: `"Received request to save allocation strategy for user {}"`
    - `String userId = jwt.getSubject()`
    - Delega para `saveAllocationGoalsUseCase.execute(userId, goals)`
    - Captura `AllocationGoalValidationException` e relança como `ResponseStatusException(HttpStatus.BAD_REQUEST, message)` OU deixa o `@ControllerAdvice` global tratar
    - Retorna HTTP 204 (no content) em sucesso
  - [ ] **GET `/api/v1/allocation-strategy`**: carrega metas existentes
    - `@GetMapping` + `@AuthenticationPrincipal Jwt jwt`
    - Log INFO na entrada: `"Received request to get allocation strategy for user {}"`
    - Retorna `List<AllocationGoalDto>` com HTTP 200
  - [ ] **Request/Response body**: `List<AllocationGoalDto>` serializado como JSON

- [ ] **Task 12: Tratar `AllocationGoalValidationException` no `@ControllerAdvice` global** (AC: 2)
  - [ ] Localizar o `@ControllerAdvice` global do projeto (verificar se já existe em `api/src/main/java/...`)
  - [ ] Adicionar handler para `AllocationGoalValidationException` → retorna `ProblemDetail` com status 400 e mensagem da exceção
  - [ ] Log ERROR na captura: `"Allocation goal validation failed: {}"`

- [ ] **Task 13: Registrar beans em `@Configuration`** (AC: 1, 2, 3, 4)
  - [ ] Criar `AllocationStrategyConfiguration` em `api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/spring/AllocationStrategyConfiguration.java`
  - [ ] Beans: `AllocationGoalJpaAdapter`, `SaveAllocationGoalsUseCase`, `GetAllocationGoalsUseCase`
  - [ ] Padrão do projeto: Use Cases como POJOs wired via `@Bean` (ver `PortfolioSummaryConfiguration.java` como referência)

---

### Frontend Flutter

- [ ] **Task 14: Criar entidade `AllocationGoal` no domain** (AC: 1, 2, 3)
  - [ ] Criar `frontend/lib/src/features/allocation/domain/entities/allocation_goal.dart`
  - [ ] Campos: `goalType` (String: `'ASSET_CLASS'` | `'TICKER'`), `targetKey` (String), `targetPercentage` (double)
  - [ ] Tipagem estrita Dart com null safety garantido

- [ ] **Task 15: Criar repository interface `AllocationRepository`** (AC: 1, 2, 3, 4)
  - [ ] Criar `frontend/lib/src/features/allocation/domain/repositories/allocation_repository.dart`
  - [ ] Métodos:
    - `Future<List<AllocationGoal>> getGoals()`
    - `Future<void> saveGoals(List<AllocationGoal> goals)`

- [ ] **Task 16: Criar datasource `AllocationRemoteDataSource`** (AC: 1, 2, 3, 4)
  - [ ] Criar `frontend/lib/src/features/allocation/data/datasources/allocation_remote_data_source.dart`
  - [ ] `GET /api/v1/allocation-strategy` → retorna `List<AllocationGoal>`
  - [ ] `POST /api/v1/allocation-strategy` → envia `List<AllocationGoal>`
  - [ ] Usar cliente Dio existente do projeto (`core/api/`)
  - [ ] Passar `Bearer {token}` no header Authorization
  - [ ] Lançar `Exception` com mensagem clara em caso de erro HTTP

- [ ] **Task 17: Criar `AllocationRepositoryImpl`** (AC: 1, 2, 3, 4)
  - [ ] Criar `frontend/lib/src/features/allocation/data/repositories/allocation_repository_impl.dart`
  - [ ] Injeta `AllocationRemoteDataSource` + `AuthRepository`
  - [ ] Segue o mesmo padrão de `PortfolioRepositoryImpl` (buscar token, delegar ao datasource)

- [ ] **Task 18: Criar `AllocationProvider`** (AC: 1, 2, 3, 4, 5)
  - [ ] Criar `frontend/lib/src/features/allocation/presentation/providers/allocation_provider.dart`
  - [ ] Extends `ChangeNotifier` — padrão do projeto (ver `PortfolioProvider`)
  - [ ] Estados: `AllocationStatus { initial, loading, success, error }`
  - [ ] Propriedades:
    - `List<AllocationGoal> goals` — metas atuais
    - `AllocationStatus status`
    - `String? errorMessage`
    - `double get classGoalSum` — soma das metas `ASSET_CLASS` (para indicador de 100%)
  - [ ] Métodos: `loadGoals()`, `saveGoals(List<AllocationGoal> goals)`, `resetStatus()`

- [ ] **Task 19: Criar tela `AllocationStrategyPage`** (AC: 1, 2, 3, 4, 5, 6)
  - [ ] Criar `frontend/lib/src/features/allocation/presentation/pages/allocation_strategy_page.dart`
  - [ ] Deve:
    - Exibir lista de metas por classe com campos de percentual (campo numérico com máscara %)
    - Exibir lista de metas por ativo específico (ticker + percentual)
    - Mostrar **indicador de soma em tempo real**: `"X% alocados em classes, Y% restantes"` — cor verde quando = 100%, amarelo/vermelho quando ≠ 100%
    - Botão "Salvar Estratégia" (FilledButton) habilitado apenas quando a soma de classes = 100%
    - Skeleton Screen (shimmer) durante loading
    - Exibir `SnackBar` de sucesso ou erro após salvar
  - [ ] Tela acessível via rota `/allocation-strategy`
  - [ ] Se usuário Gratuito: exibir card "Funcionalidade Premium — Faça upgrade" em vez do formulário (tratar erro 403 da API)

- [ ] **Task 20: Registrar rota `/allocation-strategy` no GoRouter** (AC: 1)
  - [ ] Localizar o arquivo de configuração de rotas (`core/routing/` ou similar)
  - [ ] Adicionar rota para `AllocationStrategyPage`
  - [ ] Adicionar link de navegação na sidebar/NavigationRail (item "Estratégia" ou "Metas de Alocação")

---

### Testes

- [ ] **Task 21: Testes unitários de `SaveAllocationGoalsUseCase`** (AC: 2, 3, 5)
  - [ ] Criar `common/src/test/java/.../valuation/application/usecase/SaveAllocationGoalsUseCaseTest.java`
  - [ ] Testar: soma = 100% → salva com sucesso
  - [ ] Testar: soma < 100% → lança `AllocationGoalValidationException`
  - [ ] Testar: soma > 100% → lança `AllocationGoalValidationException`
  - [ ] Testar: metas de TICKER sem validação de soma → salva com sucesso mesmo com soma ≠ 100%
  - [ ] Testar: metas mistas (ASSET_CLASS + TICKER) → valida apenas ASSET_CLASS
  - [ ] Usar Mockito para mockar `AllocationGoalPort`

- [ ] **Task 22: Testes do Controller `AllocationStrategyController`** (AC: 1, 6)
  - [ ] Criar `api/src/test/java/.../valuation/web/AllocationStrategyControllerTest.java`
  - [ ] Usar `@WebMvcTest` com Spring Security mock
  - [ ] Testar: usuário Premium → POST retorna 204
  - [ ] Testar: usuário Gratuito → retorna 403
  - [ ] Testar: soma de metas inválida → retorna 400 com ProblemDetail

- [ ] **Task 23: Widget tests de `AllocationStrategyPage`** (AC: 5)
  - [ ] Criar `frontend/test/features/allocation/presentation/pages/allocation_strategy_page_test.dart`
  - [ ] Testar: indicador de soma em tempo real (0%, 70%, 100%)
  - [ ] Testar: botão "Salvar" habilitado só quando soma = 100%
  - [ ] Testar: Skeleton Screen durante loading

- [ ] **Task 24: Executar `mvn clean test -pl common` e `mvn clean test -pl api`**
  - [ ] Confirmar que todos os testes passam sem falha

- [ ] **Task 25: Executar `flutter analyze` e testes de widget**
  - [ ] Confirmar zero lints e testes passando

---

## Dev Notes

### 1. Contexto Arquitetural — Por que esta história é o fundamento do Motor Graham

Esta história persiste o **plano de alocação desejado pelo usuário**, que é o input essencial para a história 4.3 (`valuation-service`). Sem essas metas salvas, o motor não tem como saber onde o usuário quer chegar.

**Fluxo de dados desta história:**
```
Flutter (AllocationStrategyPage)
    → POST /api/v1/allocation-strategy (AllocationStrategyController)
        → SaveAllocationGoalsUseCase (validação soma = 100%)
            → AllocationGoalPort.saveAllocationGoals()
                → AllocationGoalJpaAdapter (delete + insert na tabela allocation_goals)

Flutter → GET /api/v1/allocation-strategy
    → GetAllocationGoalsUseCase
        → AllocationGoalPort.findByUserId()
            → AllocationGoalJpaAdapter → DB
```

**Na história 4.3**, o `valuation-service` consumirá as metas via query direta no banco (read-only) para calcular qual ativo está abaixo da meta de alocação, combinando com o Filtro de Graham.

---

### 2. Código Existente — O Que NÃO Alterar e O Que Reusar

#### `RequirePremium.java` — **USAR, NÃO MODIFICAR**
```
api/src/.../api/auth/infrastructure/security/RequirePremium.java
```
Anotação existente que usa `@PreAuthorize("@premiumFeatureAccessGuard.canAccessPremiumFeatures()")`. Usar `@RequirePremium` na classe `AllocationStrategyController` — garante que todos os endpoints exijam Premium/Trial.

#### `PremiumFeatureAccessGuard.java` — **NÃO MODIFICAR**
```
api/src/.../api/auth/infrastructure/security/PremiumFeatureAccessGuard.java
```
Já verifica `SubscriptionTier.PREMIUM` e `SubscriptionTier.TRIAL` — funciona como está.

#### `PortfolioSummaryConfiguration.java` — **REFERÊNCIA DE PADRÃO**
```
api/src/.../api/portfolio/infrastructure/spring/PortfolioSummaryConfiguration.java
```
Padrão exato para wiring de Use Cases via `@Bean` em `@Configuration`. Seguir este padrão na `AllocationStrategyConfiguration`.

#### `MarketDataServiceConfiguration.java` — **REFERÊNCIA DE PADRÃO**
```
api/src/.../api/valuation/infrastructure/spring/MarketDataServiceConfiguration.java
```
Ver como os beans de `valuation` são registrados — mesma estrutura de packages.

#### `PortfolioProvider` — **REFERÊNCIA DE PADRÃO FLUTTER**
```
frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart
```
Padrão exato de `ChangeNotifier` com estados enum, `notifyListeners()`, e separação de estados. Seguir fielmente para `AllocationProvider`.

#### `PortfolioRepositoryImpl` — **REFERÊNCIA DE PADRÃO FLUTTER**
```
frontend/lib/src/features/portfolio/data/repositories/portfolio_repository_impl.dart
```
Padrão de buscar token via `AuthRepository` e delegar ao datasource — seguir para `AllocationRepositoryImpl`.

#### `TradeEntity.java` — **REFERÊNCIA DE PADRÃO JPA**
```
api/src/.../api/portfolio/infrastructure/persistence/jpa/entities/TradeEntity.java
```
Padrão de entidade JPA do projeto. **Diferença importante**: `AllocationGoalEntity` usa `Long` como ID (não UUID), então NÃO usar `@JdbcTypeCode(SqlTypes.VARCHAR)`.

---

### 3. Estrutura de Arquivos Esperada (NOVOS arquivos)

```
backend/
├── common/
│   └── src/
│       ├── main/java/afsdigital/grahamselect/
│       │   └── valuation/
│       │       ├── domain/entities/
│       │       │   └── AllocationGoal.java              ← NOVO
│       │       └── application/
│       │           ├── dto/
│       │           │   └── AllocationGoalDto.java       ← NOVO (record)
│       │           ├── repository/
│       │           │   └── AllocationGoalPort.java      ← NOVO (interface)
│       │           └── usecase/
│       │               ├── SaveAllocationGoalsUseCase.java ← NOVO (POJO)
│       │               ├── GetAllocationGoalsUseCase.java  ← NOVO (POJO)
│       │               └── exceptions/
│       │                   └── AllocationGoalValidationException.java ← NOVO
│       └── resources/db/changelog/
│           └── 19-create-allocation-goals-table.yaml   ← NOVO
│
└── api/
    └── src/main/java/afsdigital/grahamselect/api/
        └── valuation/
            ├── infrastructure/
            │   ├── persistence/
            │   │   ├── jpa/
            │   │   │   ├── entities/
            │   │   │   │   └── AllocationGoalEntity.java ← NOVO
            │   │   │   └── repositories/
            │   │   │       └── AllocationGoalJpaRepository.java ← NOVO
            │   │   └── AllocationGoalJpaAdapter.java    ← NOVO
            │   └── spring/
            │       └── AllocationStrategyConfiguration.java ← NOVO
            └── web/
                └── AllocationStrategyController.java    ← NOVO

frontend/
└── lib/src/features/
    └── allocation/                                      ← NOVA FEATURE
        ├── domain/
        │   ├── entities/
        │   │   └── allocation_goal.dart                 ← NOVO
        │   └── repositories/
        │       └── allocation_repository.dart           ← NOVO
        ├── data/
        │   ├── datasources/
        │   │   └── allocation_remote_data_source.dart   ← NOVO
        │   └── repositories/
        │       └── allocation_repository_impl.dart      ← NOVO
        └── presentation/
            ├── providers/
            │   └── allocation_provider.dart             ← NOVO
            └── pages/
                └── allocation_strategy_page.dart        ← NOVO
```

---

### 4. Estrutura de Packages — Regras Obrigatórias

```
// OBRIGATÓRIO: Domain/Application no common, SEM Spring
// common/src/main/java/afsdigital/grahamselect/valuation/...

// OBRIGATÓRIO: Infrastructure no api, COM Spring
// api/src/main/java/afsdigital/grahamselect/api/valuation/...

// NUNCA: importar org.springframework.* em classes de domain/ ou application/
```

A entidade de domínio `AllocationGoal` fica em:
`common/.../valuation/domain/entities/`

Os Use Cases ficam em:
`common/.../valuation/application/usecase/`

Os adapters JPA ficam em:
`api/.../api/valuation/infrastructure/persistence/`

---

### 5. Schema do Banco de Dados

```sql
CREATE TABLE allocation_goals (
    id          CHAR(36) NOT NULL PRIMARY KEY,  -- UUID como VARCHAR (padrão do projeto)
    user_id     VARCHAR(255) NOT NULL,
    goal_type   VARCHAR(20) NOT NULL,            -- 'ASSET_CLASS' | 'TICKER'
    target_key  VARCHAR(20) NOT NULL,            -- ex: 'ACOES', 'FIIS', 'PETR4'
    target_percentage DECIMAL(8,4) NOT NULL,    -- ex: 50.0000
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    CONSTRAINT uq_allocation_goals_user_type_key UNIQUE (user_id, goal_type, target_key),
    INDEX idx_allocation_goals_user_id (user_id)
);
```

**Nota**: O constraint UNIQUE em `(user_id, goal_type, target_key)` garante que não haja duplicação de metas para o mesmo usuário+tipo+chave. A abordagem de `deleteByUserId + saveAll` no adapter é a implementação mais simples e segura. O UUID do `id` é gerado via `UUID.randomUUID()` no adapter — **o banco não auto-gera** (padrão observado em `TradeEntity` e `ManualTradeAuditEntity`).

---

### 6. Validação de Soma de Classes — Lógica Exata

```java
// Em SaveAllocationGoalsUseCase.execute():
BigDecimal classSum = goals.stream()
    .filter(g -> "ASSET_CLASS".equals(g.goalType()))
    .map(AllocationGoalDto::targetPercentage)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal tolerance = new BigDecimal("0.01");
BigDecimal expected = new BigDecimal("100.00");
if (classSum.subtract(expected).abs().compareTo(tolerance) > 0) {
    throw new AllocationGoalValidationException(
        "A soma das metas de classe deve ser 100%. Soma atual: " + classSum + "%"
    );
}
```

---

### 7. Regras de Logging Obrigatórias (AGENTS.md)

```java
// AllocationStrategyController — entrada de API (INFO obrigatório)
log.info("Received request to save allocation strategy for user {}", userId);
log.info("Received request to get allocation strategy for user {}", userId);

// SaveAllocationGoalsUseCase — entrada do use case (INFO)
log.info("Saving {} allocation goals for user {}", goals.size(), userId);

// GetAllocationGoalsUseCase — entrada do use case (INFO)
log.info("Fetching allocation goals for user {}", userId);

// @ControllerAdvice — captura de exceção de validação (ERROR)
log.error("Allocation goal validation failed: {}", exception.getMessage());
```

---

### 8. Regras Temporais (project-context.md — OBRIGATÓRIO)

```java
// ✅ CORRETO — usar ZoneOffset.UTC em todas as operações temporais
LocalDateTime.now(ZoneOffset.UTC)  // para createdAt e updatedAt no adapter

// ❌ ERRADO — nunca sem timezone
LocalDateTime.now()  // BUG — timezone pode variar entre ambientes
```

---

### 9. Regras Monetárias — BigDecimal Obrigatório

```java
// ✅ CORRETO — BigDecimal para percentuais financeiros
BigDecimal targetPercentage = new BigDecimal("50.00");

// ❌ ERRADO — nunca float/double para dados financeiros
double targetPercentage = 50.0;  // viola regra da arquitetura
```

---

### 10. Tratamento de 403 no Frontend

```dart
// Em AllocationRemoteDataSource:
// Se a API retornar 403, lançar Exception com mensagem específica
if (response.statusCode == 403) {
  throw Exception('Funcionalidade exclusiva para usuários Premium. Faça upgrade do seu plano.');
}

// Em AllocationStrategyPage:
// Capturar a Exception e exibir card informativo em vez do formulário
// NÃO redirecionar para outra tela — mostrar inline o upgrade call-to-action
```

---

### 11. Registro do Provider no sistema de DI (Provider)

O `AllocationProvider` deve ser registrado como `ChangeNotifierProvider` no ponto de entrada do app ou na configuração de rotas. Verificar onde os outros providers são registrados (ex: `PortfolioProvider`) para seguir o mesmo padrão do projeto.

---

### 12. Isolamento de Tenant — Regra Crítica

**TODA** query e persistência de `allocation_goals` DEVE filtrar por `userId`:
- `AllocationGoalJpaAdapter.saveAllocationGoals()`: deleta e insere com `userId` explícito
- `AllocationGoalJpaAdapter.findByUserId()`: filtra por `userId` do token JWT

```java
// O userId vem SEMPRE do JWT no controller:
String userId = jwt.getSubject();
// NUNCA aceitar userId como parâmetro de query da requisição HTTP
```

---

### 13. Classes de Ativos — Referência de Chaves

Para o MVP, as classes sugeridas (valor de `target_key` quando `goal_type = 'ASSET_CLASS'`) são:
- `"ACOES"` — Ações
- `"FIIS"` — Fundos Imobiliários
- `"RENDA_FIXA"` — Renda Fixa
- `"OUTROS"` — Outros

O frontend deve oferecer essas opções via dropdown/selector. O backend NÃO valida os nomes das classes (aceita qualquer String) — a responsabilidade é do frontend.

---

### 14. Dependência com história 4.3

O `valuation-service` (história 4.3) consumirá as metas via **query direta no banco** na tabela `allocation_goals`. A história 4.3 precisará:
1. Criar acesso de leitura à tabela `allocation_goals` dentro do `valuation-service`
2. Cruzar as metas do usuário com o score de Graham para gerar recomendações personalizadas

Esta história (4.2) **NÃO precisa emitir eventos Kafka** — persistência direta em MySQL é suficiente e o `valuation-service` lerá do banco conforme precisar.

---

### 15. Learnings da história 4.1 (aplicar aqui)

- **`@Value` em beans manuais**: Injetar `@Value` em campos de `@Configuration` (como na `MarketDataServiceConfiguration`) é o padrão validado — seguir
- **Timezone no LocalDateTime**: Sempre `LocalDateTime.now(ZoneOffset.UTC)` — conforme corrigido na 4.1
- **Padrão de `@Configuration` com `@Bean`**: Preferir wiring explícito via `@Bean` a usar `@Component` nos Use Cases — conforme padrão do projeto
- **Varredura de componentes**: Verificar se o package `afsdigital.grahamselect.api.valuation` está coberto pelo `@SpringBootApplication` scan — estava OK na 4.1

---

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 4.2]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 4: Market Data & Motor de Recomendação]
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean Architecture — Padrão do Projeto]
- [Source: docs/bmad/planning-artifacts/architecture.md#D5 Autorização — RBAC simples com Google Auth]
- [Source: docs/bmad/planning-artifacts/architecture.md#Padrões de Naming]
- [Source: docs/bmad/planning-artifacts/architecture.md#D2 Migrations — Liquibase]
- [Source: docs/bmad/planning-artifacts/architecture.md#D8 Error Handling — RFC 7807]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Component Strategy]
- [Source: docs/bmad/planning-artifacts/prd.md#FR20]
- [Source: docs/bmad/project-context.md#Critical Implementation Rules]
- [Source: docs/bmad/project-context.md#Security Checklist]
- [Source: docs/bmad/implementation-artifacts/4-1-market-data-indicator-integration.md#Dev Notes]
- [Source: backend/api/src/main/java/.../auth/infrastructure/security/RequirePremium.java]
- [Source: backend/api/src/main/java/.../auth/infrastructure/security/PremiumFeatureAccessGuard.java]
- [Source: backend/api/src/main/java/.../portfolio/infrastructure/spring/PortfolioSummaryConfiguration.java]
- [Source: backend/api/src/main/java/.../portfolio/infrastructure/persistence/jpa/entities/TradeEntity.java]
- [Source: backend/api/src/main/java/.../portfolio/web/PortfolioController.java]
- [Source: backend/common/src/main/resources/db/changelog/db.changelog-master.yaml]
- [Source: frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart]
- [Source: frontend/lib/src/features/portfolio/data/repositories/portfolio_repository_impl.dart]
- [Source: AGENTS.md — Regras de logging obrigatório]

---

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.6

### Debug Log References

N/A

### Completion Notes List

N/A — ready-for-dev

### File List

N/A — a ser preenchido pelo agente de desenvolvimento
