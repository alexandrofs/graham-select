# Story 4.4: Reasoning Box — Explainable AI (Componente `ReasoningBox`)

Status: ready-for-dev

## Story

**As a** investidor,
**I want** visualizar o passo-a-passo matemático da recomendação de compra,
**So that** eu entenda por que o sistema está me sugerindo aquele aporte específico, com transparência total do raciocínio do Filtro de Graham.

## Acceptance Criteria

1. **Given** que o sistema gerou uma ou mais recomendações de compra
   **When** o usuário clica em "Por que comprar?" em qualquer card de recomendação na tela `GrahamRecommendationsPage`
   **Then** o componente `ReasoningBox` exibe um painel expandível (bottom sheet ou inline) com:
   - O ticker em destaque com o score composto de recomendação
   - Preço atual (R$) vs Valor Intrínseco calculado pela Fórmula de Graham
   - Margem de Segurança (%) com destaque visual em Amber Gold (`#F59E0B`) quando positivo
   - Alocação Atual (%) vs Meta de Alocação (%) com barra de progresso e gap em percentual
   - Explicação em linguagem acessível: "Este ativo está X% abaixo do valor intrínseco calculado pelo Filtro de Graham"

2. **Given** que o `ReasoningBox` está exibindo os dados de uma recomendação
   **When** o usuário lê o painel de raciocínio
   **Then** os indicadores fundamentalistas que alimentaram o cálculo são exibidos:
   - Fórmula de Graham aplicada: `√(22,5 × LPA × VPA)` = Valor Intrínseco
   - Valores de LPA (Lucro Por Ação) e VPA (Valor Patrimonial por Ação) utilizados
   - Score composto: `(marginOfSafety × 0,6) + (max(allocationGap, 0) × 0,4)` com valores individuais exibidos

3. **Given** que o backend retorna as recomendações via `GET /api/v1/graham-recommendations`
   **When** o frontend recebe a resposta
   **Then** o payload já existente (`ticker`, `currentPrice`, `intrinsicValue`, `marginOfSafety`, `currentAllocationPct`, `targetAllocationPct`, `allocationGap`, `recommendationScore`) é suficiente para renderizar o `ReasoningBox`
   **And** o endpoint backend retorna campos adicionais `epsUsed` (LPA usado) e `bvpsUsed` (VPA usado) para exibição no painel de raciocínio
   **And** se `epsUsed`/`bvpsUsed` forem nulos no retorno do backend, o painel exibe "Dados de LPA/VPA não disponíveis" sem travar

4. **Given** que a Fórmula de Graham é exibida no `ReasoningBox`
   **When** o usuário examina os cálculos
   **Then** o `ReasoningBox` exibe a fórmula como passo-a-passo:
   - Passo 1: "LPA (Lucro Por Ação): R$ X,XX"
   - Passo 2: "VPA (Valor Patrimonial por Ação): R$ X,XX"
   - Passo 3: "Valor Intrínseco Graham = √(22,5 × LPA × VPA) = R$ X,XX"
   - Passo 4: "Preço Atual: R$ X,XX"
   - Passo 5: "Margem de Segurança: (Intrínseco/Preço - 1) = X,XX%"
   **And** os passos são exibidos sequencialmente com separadores visuais

5. **Given** que o `ReasoningBox` está aberto
   **When** o usuário toca/clica fora do painel (dismiss)
   **Then** o painel fecha com animação suave (fade out 250ms)
   **And** o card de recomendação retorna ao estado normal sem perda de dados

6. **Given** que o usuário não tem metas de alocação definidas para um ativo
   **When** o `ReasoningBox` é exibido para esse ativo
   **Then** a seção de alocação exibe "Sem meta de alocação definida para este ativo"
   **And** o score composto é exibido usando apenas `marginOfSafety × 0,6` (sem componente de alocação)
   **And** o painel não lança erro nem trava

7. **Given** que o `ReasoningBox` é implementado
   **When** o agente de dev verifica a implementação
   **Then** o componente `ReasoningBox` deve ser um widget Flutter reutilizável criado em `frontend/lib/src/features/ranking/presentation/widgets/reasoning_box.dart`
   **And** o widget aceita um `GrahamRecommendation` como parâmetro de entrada
   **And** o widget pode ser invocado de qualquer tela que exiba recomendações (não acoplado a `GrahamRecommendationsPage`)

---

## Tasks / Subtasks

### Backend — Módulo `common` (DTO extension)

- [ ] **Task 1: Adicionar campos `epsUsed` e `bvpsUsed` ao `GrahamRecommendationDto`** (AC: 3)
  - [ ] Modificar record em `common/src/main/java/afsdigital/grahamselect/valuation/application/dto/GrahamRecommendationDto.java`
  - [ ] Adicionar campos: `epsUsed` (BigDecimal, nullable — `@JsonInclude(NON_NULL)`), `bvpsUsed` (BigDecimal, nullable — `@JsonInclude(NON_NULL)`)
  - [ ] **NÃO remover** os campos existentes: `ticker`, `currentPrice`, `intrinsicValue`, `marginOfSafety`, `currentAllocationPct`, `targetAllocationPct`, `allocationGap`, `recommendationScore`
  - [ ] Manter record Java (não converter para class)

- [ ] **Task 2: Adicionar campos `epsUsed` e `bvpsUsed` à entidade `GrahamRecommendation`** (AC: 3)
  - [ ] Modificar `common/src/main/java/afsdigital/grahamselect/valuation/domain/entities/GrahamRecommendation.java`
  - [ ] Adicionar campos: `epsUsed` (BigDecimal, nullable), `bvpsUsed` (BigDecimal, nullable)
  - [ ] Usar `@Builder.Default` com `null` como padrão para compatibilidade retroativa

---

### Backend — Módulo `valuation-service` (migração e população de novos campos)

- [ ] **Task 3: Criar migration Liquibase para adicionar colunas à tabela `graham_recommendations`** (AC: 3)
  - [ ] Criar `backend/common/src/main/resources/db/changelog/21-add-eps-bvps-to-graham-recommendations.yaml`
  - [ ] Adicionar colunas:
    ```yaml
    columns:
      - eps_used: DECIMAL(18,4) NULL  # LPA usado no cálculo
      - bvps_used: DECIMAL(18,4) NULL # VPA usado no cálculo
    ```
  - [ ] Incluir no `db.changelog-master.yaml` após a entrada `20-create-graham-recommendations-table.yaml`
  - [ ] As colunas devem ser `NULL` (nullable) para retrocompatibilidade com dados existentes

- [ ] **Task 4: Atualizar `GrahamRecommendationEntity` no `valuation-service` com novos campos** (AC: 3)
  - [ ] Modificar `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java`
  - [ ] Adicionar campos: `epsUsed` (BigDecimal, `@Column(name = "eps_used")`), `bvpsUsed` (BigDecimal, `@Column(name = "bvps_used")`)
  - [ ] Manter campos como nullable: sem `@Column(nullable = false)`

- [ ] **Task 5: Verificar fonte dos dados de LPA/VPA no `valuation-service`** (AC: 3, 4)
  - [ ] Verificar se `RankedCompany` ou `IntrinsicValueEntity` já carrega `eps` e `bvps`
  - [ ] Verificar estrutura de `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/IntrinsicValueEntity.java`
  - [ ] Verificar se o `RankingReadAdapter.findTop20BestRanked()` já retorna dados de LPA/VPA no objeto `RankedCompany` (ou equivalente)
  - [ ] **Se LPA/VPA disponível**: popular `epsUsed` e `bvpsUsed` na `GrahamRecommendation` dentro de `GenerateGrahamRecommendationsUseCase.execute()`
  - [ ] **Se LPA/VPA não disponível no ranking**: buscar via query direta na tabela `company_intrinsic_value` ou equivalente para o ticker

- [ ] **Task 6: Atualizar `GenerateGrahamRecommendationsUseCase` para popular `epsUsed`/`bvpsUsed`** (AC: 3, 4)
  - [ ] Modificar `common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java`
  - [ ] Ao construir `GrahamRecommendation` via builder, incluir `epsUsed` e `bvpsUsed` extraídos dos dados fundamentalistas
  - [ ] Manter a lógica de filtragem e score existente (NÃO alterar o algoritmo)
  - [ ] Log INFO: a assinatura de log já existente `"Generated {} recommendations for user {}"` é suficiente

- [ ] **Task 7: Atualizar `GrahamRecommendationRepositoryImpl` no `valuation-service` para persistir novos campos** (AC: 3)
  - [ ] Modificar `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/GrahamRecommendationRepositoryImpl.java`
  - [ ] No mapeamento domain entity → JPA entity, incluir `epsUsed` e `bvpsUsed`
  - [ ] Tratar null safety: se `epsUsed == null`, não incluir na entity (entity field fica null)

---

### Backend — Módulo `api` (leitura e exposição via REST)

- [ ] **Task 8: Atualizar `GrahamRecommendationEntity` no módulo `api` com novos campos** (AC: 3)
  - [ ] Modificar `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java`
  - [ ] Adicionar campos: `epsUsed` (BigDecimal, `@Column(name = "eps_used")`), `bvpsUsed` (BigDecimal, `@Column(name = "bvps_used")`)
  - [ ] Manter os campos como nullable

- [ ] **Task 9: Atualizar `GrahamRecommendationReadAdapter` no `api` para mapear novos campos** (AC: 3)
  - [ ] Modificar `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/GrahamRecommendationReadAdapter.java`
  - [ ] No mapeamento JPA entity → domain entity (`GrahamRecommendation`), incluir `epsUsed` e `bvpsUsed`

- [ ] **Task 10: Atualizar `GetGrahamRecommendationsUseCase` para mapear `epsUsed`/`bvpsUsed` no DTO** (AC: 3)
  - [ ] Verificar `common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetGrahamRecommendationsUseCase.java`
  - [ ] No mapeamento domain entity → `GrahamRecommendationDto`, incluir os novos campos
  - [ ] Campos nullable no DTO: Jackson serializa apenas se não-nulos (`@JsonInclude(NON_NULL)` já configurado no DTO)

- [ ] **Task 11: Confirmar que `GrahamRecommendationsController` não precisa de alteração** (AC: 3)
  - [ ] Verificar `backend/api/src/main/java/afsdigital/grahamselect/api/valuation/web/GrahamRecommendationsController.java`
  - [ ] O endpoint GET retorna `List<GrahamRecommendationDto>` — se o DTO foi atualizado, o controller herda automaticamente os novos campos
  - [ ] Log INFO existente no controller é suficiente para observabilidade

---

### Frontend Flutter — Componente `ReasoningBox` e integração

- [ ] **Task 12: Atualizar entidade `GrahamRecommendation` com campos opcionais de LPA/VPA** (AC: 3, 7)
  - [ ] Modificar `frontend/lib/src/features/ranking/domain/entities/graham_recommendation.dart`
  - [ ] Adicionar campos: `epsUsed` (double?, nullable), `bvpsUsed` (double?, nullable)
  - [ ] Manter null safety obrigatório do Dart

- [ ] **Task 13: Atualizar `GrahamRecommendationModel` para desserializar novos campos** (AC: 3)
  - [ ] Modificar `frontend/lib/src/features/ranking/data/models/graham_recommendation_model.dart`
  - [ ] Adicionar `epsUsed: json['epsUsed'] as double?` e `bvpsUsed: json['bvpsUsed'] as double?` no `fromJson`
  - [ ] Atualizar `toEntity()` para passar os campos para `GrahamRecommendation`

- [ ] **Task 14: Criar widget `ReasoningBox`** (AC: 1, 2, 3, 4, 5, 6, 7) — **TAREFA PRINCIPAL**
  - [ ] Criar `frontend/lib/src/features/ranking/presentation/widgets/reasoning_box.dart`
  - [ ] Widget stateless: `class ReasoningBox extends StatelessWidget`
  - [ ] **Parâmetros:**
    ```dart
    const ReasoningBox({
      super.key,
      required this.recommendation, // GrahamRecommendation
    });
    ```
  - [ ] **Layout do painel** (usar `Container` com padding, `Column`, separadores):
    - **Header:** Ticker em `titleLarge` bold + Score badge com `primary.withOpacity(0.1)` background
    - **Seção: Análise de Preço**
      - Título: "Análise de Valor de Mercado" (subtítulo section)
      - Passo 1: "LPA (Lucro Por Ação): R$ X,XX" — se `epsUsed == null`, exibir "LPA: Dado não disponível"
      - Passo 2: "VPA (Valor Patrimonial por Ação): R$ X,XX" — se `bvpsUsed == null`, exibir "VPA: Dado não disponível"
      - Passo 3: "Valor Intrínseco (Graham): √(22,5 × LPA × VPA) = R$ X,XX" (usar `intrinsicValue` do objeto)
      - Passo 4: "Preço Atual: R$ X,XX" (usar `currentPrice`)
      - **Badge de Margem de Segurança:** `+X,XX% abaixo do valor intrínseco` em **Amber Gold `#F59E0B`** (positivo) ou `#EF4444` (negativo)
    - **Divisor visual** (`Divider`)
    - **Seção: Análise de Alocação**
      - Título: "Análise de Alocação na Carteira"
      - Se `targetAllocationPct == 0`: exibir "Sem meta de alocação definida para este ativo"
      - Se `targetAllocationPct > 0`:
        - "Alocação Atual: X,X%"
        - "Meta de Alocação: X,X%"
        - `LinearProgressIndicator` mostrando (current/target), capped at 1.0
        - "Gap: falta X,X% para atingir a meta" ou "Meta atingida ✓" ou "Sobre-alocado em X,X%"
    - **Divisor visual** (`Divider`)
    - **Seção: Score Composto**
      - Título: "Como é Calculado o Score"
      - "Componente Graham (60%): marginOfSafety × 0,6 = X,XX"
      - "Componente Alocação (40%): gap × 0,4 = X,XX" (exibir 0 se sem meta)
      - "**Score Final: X,XX**" em destaque
      - Microcopy: "Quanto maior o score, maior a prioridade de aporte sugerida."
  - [ ] **Paleta de cores obrigatória:**
    - Amber Gold `const Color(0xFFF59E0B)` para margem positiva
    - Emerald `const Color(0xFF10B981)` para meta atingida
    - `#EF4444` para negativo/perigo
    - Navy Blue `const Color(0xFF1B2A4A)` para header
  - [ ] **Tipografia:**
    - Valores financeiros em `JetBrains Mono` (monospace) via `fontFamily: 'JetBrainsMono'`
    - Verificar se a fonte está configurada em `pubspec.yaml` — se não, usar `TextStyle(fontFeatures: [FontFeature.tabularFigures()])`
    - Headers de seção em `titleMedium` bold
    - Valores em `bodyLarge`
    - Microcopy em `labelSmall` com `Colors.grey`
  - [ ] **Acessibilidade:**
    - Envolver cada valor financeiro em `Semantics(label: 'Preço atual: R$ X,XX', child: ...)`
    - Score deve ter semântica: `Semantics(label: 'Score de recomendação: X,XX', child: ...)`

- [ ] **Task 15: Integrar `ReasoningBox` na `GrahamRecommendationsPage` via `showModalBottomSheet`** (AC: 1, 5)
  - [ ] Modificar `frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart`
  - [ ] No método `_buildRecommendationCard`, adicionar botão "Por que comprar?" (`TextButton` com ícone `Icons.info_outline`)
  - [ ] O botão deve ficar posicionado no rodapé de cada card, alinhado à direita
  - [ ] Ao clicar, invocar:
    ```dart
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.6,
        minChildSize: 0.4,
        maxChildSize: 0.9,
        builder: (_, controller) => SingleChildScrollView(
          controller: controller,
          padding: const EdgeInsets.all(24),
          child: ReasoningBox(recommendation: rec),
        ),
      ),
    );
    ```
  - [ ] **NÃO alterar** a lógica de `fetchRecommendations()`, `triggerCalculation()` ou estados do provider
  - [ ] Importar `reasoning_box.dart` no topo do arquivo

---

### Testes

- [ ] **Task 16: Teste unitário do `ReasoningBox` widget** (AC: 1, 2, 4, 6)
  - [ ] Criar `frontend/test/features/ranking/presentation/widgets/reasoning_box_test.dart`
  - [ ] Testar: `ReasoningBox` com `epsUsed` e `bvpsUsed` preenchidos → exibe LPA, VPA, fórmula e margem de segurança
  - [ ] Testar: `ReasoningBox` com `epsUsed == null` e `bvpsUsed == null` → exibe "Dado não disponível" sem travar
  - [ ] Testar: `ReasoningBox` com `targetAllocationPct == 0` → exibe "Sem meta de alocação definida"
  - [ ] Testar: `ReasoningBox` com `marginOfSafety > 0` → badge de margem em Amber Gold (`#F59E0B`)
  - [ ] Testar: `ReasoningBox` com `marginOfSafety <= 0` → badge em vermelho
  - [ ] Testar: botão "Por que comprar?" no card → abre o bottom sheet com `ReasoningBox`

- [ ] **Task 17: Teste de integração para novos campos no `GrahamRecommendationModel`** (AC: 3)
  - [ ] Modificar `frontend/test/features/ranking/presentation/pages/graham_recommendations_page_test.dart` (existente)
  - [ ] Adicionar cenário: JSON com `epsUsed` e `bvpsUsed` → `toEntity()` popula campos corretamente
  - [ ] Adicionar cenário: JSON sem `epsUsed`/`bvpsUsed` → `toEntity()` popula campos como `null`

- [ ] **Task 18: Atualizar testes de backend para novos campos na entidade** (AC: 3)
  - [ ] Atualizar `GenerateGrahamRecommendationsUseCaseTest.java` (existente) para verificar que `epsUsed`/`bvpsUsed` são populados quando disponíveis
  - [ ] Adicionar cenário: LPA/VPA indisponível → `epsUsed == null`, `bvpsUsed == null` na recomendação gerada

- [ ] **Task 19: Executar validação final** (AC: todos)
  - [ ] `mvn clean test -pl common` — confirmar zero falhas
  - [ ] `mvn clean test -pl api` — confirmar zero falhas
  - [ ] `mvn clean test -pl valuation-service` — confirmar zero falhas
  - [ ] `flutter analyze` — confirmar zero lints
  - [ ] `flutter test test/features/ranking/` — confirmar todos os testes passam

---

## Dev Notes

### 1. Contexto Arquitetural — O que Já Existe (REUTILIZAR)

**História 4.3 já implementou toda a infraestrutura necessária:**

```
Estrutura existente pós-4.3:
backend/
├── common/
│   └── valuation/
│       ├── domain/entities/GrahamRecommendation.java        ← MODIFICAR (adicionar epsUsed/bvpsUsed)
│       ├── application/dto/GrahamRecommendationDto.java     ← MODIFICAR (adicionar campos)
│       └── application/usecase/
│           ├── GenerateGrahamRecommendationsUseCase.java    ← MODIFICAR (popular novos campos)
│           └── GetGrahamRecommendationsUseCase.java         ← VERIFICAR se precisa ajuste
├── valuation-service/
│   └── infrastructure/persistence/
│       ├── jpa/entities/GrahamRecommendationEntity.java     ← MODIFICAR (adicionar colunas)
│       └── GrahamRecommendationRepositoryImpl.java          ← MODIFICAR (mapear novos campos)
└── api/
    └── valuation/
        ├── infrastructure/persistence/
        │   ├── jpa/entities/GrahamRecommendationEntity.java  ← MODIFICAR (adicionar colunas)
        │   └── GrahamRecommendationReadAdapter.java          ← MODIFICAR (mapear novos campos)
        └── web/GrahamRecommendationsController.java          ← NÃO ALTERAR

frontend/
└── features/ranking/
    ├── domain/entities/graham_recommendation.dart            ← MODIFICAR (nullable fields)
    ├── data/models/graham_recommendation_model.dart          ← MODIFICAR (fromJson)
    ├── presentation/
    │   ├── pages/graham_recommendations_page.dart            ← MODIFICAR (adicionar botão)
    │   └── widgets/                                         ← CRIAR reasoning_box.dart
    └── presentation/providers/graham_recommendation_provider.dart ← NÃO ALTERAR
```

### 2. Fonte de Dados LPA/VPA — Investigação Necessária

**Antes de implementar Task 5 e 6, o dev agent DEVE:**

1. Verificar `RankedCompany` domain entity (ou DTO equivalente) no módulo `common`:
   - Localização: `common/src/main/java/afsdigital/grahamselect/valuation/domain/entities/` ou `application/dto/`
   - Se já tem `eps` e `bvps` → usar diretamente
   
2. Verificar `RankingReadAdapter` no `valuation-service`:
   - Localização: `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/RankingReadAdapter.java`
   - A query SQL de ranking já busca EPS/BVPS? Verificar o SELECT

3. Verificar tabela/entidade de intrinsic value:
   - `backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/IntrinsicValueEntity.java`
   - Verificar se possui colunas `eps`, `bvps` ou similar

**Se os dados NÃO estiverem disponíveis no ranking atual:**
   - Criar query adicional que busca `eps`/`bvps` por ticker na tabela de intrinsic values
   - Passar como parâmetro adicional ao `GenerateGrahamRecommendationsUseCase`
   - **NÃO refatorar** o algoritmo principal — apenas adicionar dados complementares

### 3. Padrão de IDs — CRÍTICO (Diferença entre módulos)

```java
// No valuation-service: ID é String (UUID como VARCHAR)
String id = UUID.randomUUID().toString(); // ✅ CORRETO no valuation-service

// No api: ID é UUID com @JdbcTypeCode
@Id
@JdbcTypeCode(SqlTypes.VARCHAR)
private UUID id; // ✅ CORRETO no api
```

**Seguir o padrão do módulo onde o código reside.**

### 4. Regra de Dependência de Módulos

```
common/ → sem deps externas (POJOs puros)
api/ → depende de common/ (via Maven)
valuation-service/ → depende de common/ (via Maven)
api/ e valuation-service/ → NÃO se dependem mutuamente
```

**As entidades `GrahamRecommendationEntity` existem duplicadas em `api/` e `valuation-service/` — esse é o design correto.**

### 5. Design do `ReasoningBox` — Especificação Visual

```dart
// Paleta de cores obrigatória (definida no UX Design Spec e epics.md)
const Color navyBlue = Color(0xFF1B2A4A);
const Color emerald = Color(0xFF10B981);
const Color amberGold = Color(0xFFF59E0B);  // ← "destaca em Emerald Gold" (AC da história)
const Color errorRed = Color(0xFFEF4444);
const Color infoPrimary = Color(0xFF3B82F6);
```

**AC 1 usa "Amber Gold (#F59E0B)" para destacar a vantagem competitiva — manter fiel ao epics.md.**

**Layout do passo-a-passo (inspirado na UX spec "Caixa de Raciocínio"):**
```
┌─────────────────────────────────────────────────┐
│  PETR4                          Score: 0.87     │
├─────────────────────────────────────────────────┤
│  📊 Análise de Valor de Mercado                  │
│  Passo 1: LPA (Lucro Por Ação): R$ 4,52        │
│  Passo 2: VPA (Valor Patrimonial): R$ 31,80     │
│  Passo 3: VI Graham = √(22,5 × 4,52 × 31,80)  │
│           = R$ 56,87                            │
│  Passo 4: Preço Atual: R$ 38,20                │
│  ┌──────────────────────────────────────────┐  │
│  │  Margem de Segurança: +48,9% 🟡          │  │
│  └──────────────────────────────────────────┘  │
├─────────────────────────────────────────────────┤
│  📈 Análise de Alocação na Carteira              │
│  Atual: 3,2%   Meta: 8,0%                      │
│  ████████░░░░░░░░░░░░░░░░░  (40% da meta)      │
│  Falta 4,8% para atingir a meta                │
├─────────────────────────────────────────────────┤
│  🎯 Como é Calculado o Score                    │
│  Componente Graham (60%): 0,489 × 0,6 = 0,293  │
│  Componente Alocação (40%): 0,048 × 0,4 = 0,019│
│  Score Final: 0,87                              │
│  (Quanto maior, maior a prioridade de aporte)  │
└─────────────────────────────────────────────────┘
```

### 6. Checklist de Linting Flutter (AGENTS.md)

O `analysis_options.yaml` usa `flutter_lints`. Problemas comuns a evitar:

```dart
// ❌ ERRADO — withOpacity está deprecado em Flutter 3.11+
color: Colors.amber.withOpacity(0.1)

// ✅ CORRETO — usar withValues ou extension method
color: Colors.amber.withAlpha((0.1 * 255).round())
// OU
color: Color.fromARGB((0.1 * 255).round(), 245, 158, 11)

// ❌ ERRADO — surfaceVariant está deprecado
Theme.of(context).colorScheme.surfaceVariant

// ✅ CORRETO
Theme.of(context).colorScheme.surfaceContainerHighest
```

**Verificar o código existente em `graham_recommendations_page.dart` linhas 319 e 252** — há usos de `withOpacity` e `surfaceVariant` que podem gerar lint warnings. Esta história pode corrigi-los como parte da refatoração.

### 7. Regras de Logging Obrigatórias (AGENTS.md)

Esta história modifica poucos endpoints backend. Os logs existentes são suficientes:
```java
// GrahamRecommendationsController — já existe
log.info("Received request to get Graham recommendations for user {}", userId);

// GenerateGrahamRecommendationsUseCase — já existe
log.info("Generating Graham recommendations for user {}", userId);
log.info("Generated {} recommendations for user {}", recommendations.size(), userId);
```

**NÃO é necessário adicionar novos logs — apenas garantir que os existentes permanecem inalterados.**

### 8. Regras Temporais e Financeiras (project-context.md)

```java
// ✅ Manter uso de BigDecimal para epsUsed e bvpsUsed
BigDecimal epsUsed = rankedCompany.getEps();   // nunca double/float
BigDecimal bvpsUsed = rankedCompany.getBvps(); // nunca double/float

// ✅ Timezone UTC obrigatório (não afeta esta história diretamente)
```

```dart
// ✅ Dart: double? para nullable (null safety obrigatório)
final double? epsUsed;
final double? bvpsUsed;

// ❌ ERRADO — nunca usar late sem garantia
late double epsUsed; // risco de LateInitializationError
```

### 9. Isolamento de Tenant — Não Impactado

Esta história não cria novos endpoints nem queries com userId. O endpoint `GET /api/v1/graham-recommendations` já filtra por userId do JWT. **Nenhuma query adicional precisa de filtro de usuário nesta história.**

### 10. Dívida Técnica da 4.3 — Oportunidade de Melhoria (opcional)

O `deferred-work.md` lista 3 itens deferidos da história 4.3. Esta história **não é obrigada** a corrigi-los, mas se o dev agent tocar em `GrahamRecommendationPort.java` ou `GenerateGrahamRecommendationsUseCase.java` durante a implementação, pode oportunisticamente:

- **Strings Mágicas**: Considerar criar enum `GoalType { TICKER, ASSET_CLASS }` em vez de `"TICKER"` hardcoded
- **ISP na Port**: Considerar dividir `GrahamRecommendationPort` em `GrahamRecommendationWritePort` e `GrahamRecommendationReadPort`
- **NÃO é obrigatório** — apenas se o dev agent julgar que o custo de mudança é baixo e sem risco de regressão

### 11. Delay Fixo de 3 Segundos — Issue Conhecida (NÃO CORRIGIR AQUI)

O `GrahamRecommendationProvider.triggerCalculation()` usa `Future.delayed(const Duration(seconds: 3))` antes de fazer o reload. Esta é uma limitação conhecida registrada na história 4.3 como `[Review][Decision]`. **NÃO alterar este comportamento nesta história** — a solução definitiva exige polling inteligente ou WebSocket.

### 12. Estrutura de Arquivos — Novos e Modificados

```
NOVOS:
frontend/lib/src/features/ranking/presentation/widgets/reasoning_box.dart
frontend/test/features/ranking/presentation/widgets/reasoning_box_test.dart
backend/common/src/main/resources/db/changelog/21-add-eps-bvps-to-graham-recommendations.yaml

MODIFICADOS:
backend/common/src/main/java/afsdigital/grahamselect/valuation/domain/entities/GrahamRecommendation.java
backend/common/src/main/java/afsdigital/grahamselect/valuation/application/dto/GrahamRecommendationDto.java
backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GenerateGrahamRecommendationsUseCase.java
backend/common/src/main/java/afsdigital/grahamselect/valuation/application/usecase/GetGrahamRecommendationsUseCase.java (verificar)
backend/common/src/main/resources/db/changelog/db.changelog-master.yaml
backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java
backend/valuation-service/src/main/java/afsdigital/grahamselect/valuation/infrastructure/persistence/GrahamRecommendationRepositoryImpl.java
backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/entities/GrahamRecommendationEntity.java
backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/GrahamRecommendationReadAdapter.java
frontend/lib/src/features/ranking/domain/entities/graham_recommendation.dart
frontend/lib/src/features/ranking/data/models/graham_recommendation_model.dart
frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart
frontend/test/features/ranking/presentation/pages/graham_recommendations_page_test.dart
backend/common/src/test/java/.../application/usecase/GenerateGrahamRecommendationsUseCaseTest.java
```

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 4.4]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 4: Market Data & Motor de Recomendação]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Custom Components — ReasoningBox]
- [Source: docs/bmad/planning-artifacts/architecture.md#D12 - Estrutura Flutter]
- [Source: docs/bmad/planning-artifacts/architecture.md#Regras Obrigatórias para Agentes de IA]
- [Source: docs/bmad/implementation-artifacts/4-3-graham-valuation-engine.md#Dev Notes]
- [Source: docs/bmad/implementation-artifacts/deferred-work.md]
- [Source: docs/bmad/project-context.md]
- [Source: frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart]
- [Source: frontend/lib/src/features/ranking/domain/entities/graham_recommendation.dart]
