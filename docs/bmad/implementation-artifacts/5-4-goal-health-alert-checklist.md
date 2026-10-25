---
baseline_commit: 1a33194ee5d7a55e68ab78ffc1e952eeb0b26837
---

# Story 5.4: Checklist de Saúde da Meta

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** investidor,
**I want** receber alertas visuais e um checklist diagnóstico de saúde da minha meta financeira caso meu ritmo atual de aportes ou a rentabilidade da minha carteira estejam me afastando do prazo estimado,
**So that** eu não tenha surpresas negativas no futuro e saiba exatamente quanto aumentar no aporte mensal ou qual rentabilidade buscar para voltar ao trilho.

## Acceptance Criteria

1. **Given** que o usuário autenticado possui uma meta financeira ativa (`currentGoal != null`) cujo tempo projetado (`projectedTotalMonths`) ultrapassa o prazo estimado pelo usuário (`estimatedYears * 12` meses) ou é inalcançável (`isUnreachable == true`)
   **When** o dashboard principal (`DashboardPage`, rota `/dashboard`) ou a tela de metas (`GoalsPage`, rota `/metas`) é carregado
   **Then** o sistema exibe o componente **`GoalHealthChecklistCard`** ("Checklist de Saúde da Meta") contendo:
   - **Badge de Atenção (`Key('goal_health_status_badge')`)** na cor semântica de alerta **Warning `#EF4444`** (`const Color(0xFFEF4444)`) com ícone `Icons.warning_amber_rounded` e texto **"Atenção: Fora do Trilho"**
   - **Checklist de Diagnóstico** avaliando os 3 pilares da meta (Prazo Estimado, Ritmo de Aportes Mensais e Rentabilidade da Carteira)
   - **Painel de Ações Recomendadas (`Key('goal_health_warning_banner')`)** destacado em `#EF4444` sugerindo ações prescritivas e calculadas matematicamente:
     - **"Aumente seu aporte em R$ X para voltar ao trilho"** (onde $X$ é o aporte mensal adicional necessário $\Delta PMT$ para atingir o valor alvo exatamente dentro de `estimatedYears * 12` meses mantendo a rentabilidade atual)
     - **"Revise seu yield alvo para Y% a.a. para atingir a meta no prazo estimado"** (onde $Y$ é a taxa anual necessária mantendo o aporte mensal atual).

2. **Given** que o usuário possui uma meta financeira ativa (`currentGoal != null`) e a projeção atual está dentro do prazo estimado (`!isUnreachable && projectedTotalMonths <= estimatedYears * 12` e `!isAchieved`)
   **When** o `DashboardPage` ou a `GoalsPage` é carregado
   **Then** o componente **`GoalHealthChecklistCard`** exibe:
   - **Badge de Saúde (`Key('goal_health_status_badge')`)** na cor `kEmerald` (`#10B981`) com ícone `Icons.check_circle_outline` e texto **"No Trilho"**
   - Todos os 3 itens do checklist marcados como saudáveis (`Icons.check_circle_rounded` em `#10B981`)
   - Mensagem de reforço positivo: **"Mantenha a consistência dos seus aportes mensais para seguir no trilho."** (sem exibir o banner de alerta `#EF4444`).

3. **Given** que o usuário já atingiu ou superou o valor alvo da meta (`isAchieved == true`, $PV \ge FV$)
   **When** o `GoalHealthChecklistCard` é renderizado
   **Then** exibe o badge **"Meta Concluída"** em `kEmerald` (`#10B981`), todos os itens do checklist marcados como concluídos e a mensagem **"Parabéns! Sua meta financeira já foi atingida."**.

4. **Given** que o usuário ainda NÃO possui meta financeira configurada (`currentGoal == null`)
   **When** o `DashboardPage` ou a `GoalsPage` é carregado
   **Then** o `GoalHealthChecklistCard` não é exibido (evitando poluição visual duplicada com o Empty State do `MotivationalClockCard` no Dashboard e com o formulário vazio na `GoalsPage`).

5. **Given** que o backend recebe `GET /api/v1/financial-goals/health`
   **When** o usuário está autenticado com token JWT válido
   **Then** o backend busca a meta ativa (`FinancialGoal`) e o resumo do portfólio (`PortfolioSummary`), executa `EvaluateGoalHealthUseCase` e retorna `200 OK` com `GoalHealthAssessmentDto`:
   ```json
   {
     "data": {
       "status": "OFF_TRACK",
       "isOnTrack": false,
       "estimatedTargetMonths": 120,
       "projectedTotalMonths": 142,
       "monthsDelay": 22,
       "additionalMonthlyContributionNeeded": 685.00,
       "requiredMonthlyContribution": 2685.00,
       "requiredAnnualYieldPercentage": 12.85,
       "actionSuggestions": [
         "Aumente seu aporte em R$ 685,00 para voltar ao trilho",
         "Revise seu yield alvo para 12,85% a.a. para atingir a meta no prazo estimado"
       ]
     }
   }
   ```
   **And** se o usuário não possuir meta ativa, retorna `200 OK` com `{ "data": null }` (usando `Collections.singletonMap("data", null)`)
   **And** registra log `INFO` na entrada do controller (`"Received request to evaluate financial goal health for user {}"`) e no use case (`"Evaluating financial goal health for user {}"`), e log `ERROR` com stack trace em qualquer captura/lançamento de exceção (`AGENTS.md`)
   **And** utiliza estritamente `ZoneOffset.UTC` e `BigDecimal`.

6. **Given** que a implementação está concluída
   **When** a suíte de validação é executada
   **Then** `mvn clean test -pl common` e `mvn clean test -pl api` passam com 100% de sucesso
   **And** `flutter analyze` retorna zero issues e `flutter test` passa integralmente sem regressões nos testes existentes (`dashboard_page_test.dart`, `goals_page_test.dart`, `motivational_clock_card_test.dart`, `what_if_simulator_card_test.dart`, `goals_provider_test.dart`).

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domain + Application)

- [x] **Task 1: Criar entidades `GoalHealthStatus` e `GoalHealthAssessment` e evoluir `TimeToGoalCalculator`** (AC: 1, 2, 3, 5)
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalHealthStatus.java`:
    ```java
    package afsdigital.grahamselect.goals.domain.entities;

    public enum GoalHealthStatus {
        ON_TRACK,
        OFF_TRACK,
        ACHIEVED
    }
    ```
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalHealthAssessment.java`:
    ```java
    @Builder
    @Getter
    public class GoalHealthAssessment {
        private GoalHealthStatus status;
        private boolean isOnTrack;
        private int estimatedTargetMonths;
        private int projectedTotalMonths;
        private int monthsDelay;
        private BigDecimal additionalMonthlyContributionNeeded;
        private BigDecimal requiredMonthlyContribution;
        private BigDecimal requiredAnnualYieldPercentage;
        private List<String> actionSuggestions;
    }
    ```
  - [x] Em `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`:
    - Adicionar método `evaluateHealth(...)`:
      ```java
      public GoalHealthAssessment evaluateHealth(
          GoalType goalType,
          BigDecimal rawTargetValue,
          int estimatedYears,
          BigDecimal currentEquity,
          BigDecimal monthlyContribution,
          BigDecimal annualYieldPercentage,
          OffsetDateTime referenceDate
      )
      ```
    - Calcular `GoalProjection projection = calculate(goalType, rawTargetValue, currentEquity, monthlyContribution, annualYieldPercentage, referenceDate)`.
    - Definir `int estimatedTargetMonths = Math.max(1, estimatedYears) * 12`.
    - **Caso 1 (`projection.isAchieved()`):**
      - `status = GoalHealthStatus.ACHIEVED`, `isOnTrack = true`, `monthsDelay = 0`
      - `additionalMonthlyContributionNeeded = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)`
      - `requiredMonthlyContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)`
      - `requiredAnnualYieldPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)`
      - `actionSuggestions = List.of("Parabéns! Sua meta financeira já foi atingida.")`
    - **Caso 2 (`!projection.isUnreachable() && projection.getTotalMonths() <= estimatedTargetMonths`):**
      - `status = GoalHealthStatus.ON_TRACK`, `isOnTrack = true`, `monthsDelay = 0`
      - `additionalMonthlyContributionNeeded = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)`
      - `requiredMonthlyContribution = pmt.setScale(2, RoundingMode.HALF_UP)`
      - `requiredAnnualYieldPercentage = annualYieldPct.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)`
      - `actionSuggestions = List.of("Mantenha a consistência dos seus aportes mensais para seguir no trilho.")`
    - **Caso 3 (`projection.isUnreachable() || projection.getTotalMonths() > estimatedTargetMonths` — `OFF_TRACK`):**
      - `status = GoalHealthStatus.OFF_TRACK`, `isOnTrack = false`
      - `monthsDelay = projection.isUnreachable() ? -1 : (projection.getTotalMonths() - estimatedTargetMonths)`
      - Calcular `requiredMonthlyContribution` ($PMT_{\text{req}}$) para atingir `FV = projection.getTargetValue()` partindo de $PV$ em $N = \text{estimatedTargetMonths}$ meses com taxa mensal $r = \frac{\text{annualYieldPct}}{12 \times 100}$:
        - Se $r > 0$:
          $F = (1 + r)^N$. Se $PV \cdot F \ge FV$, $PMT_{\text{req}} = 0$; caso contrário, $PMT_{\text{req}} = \frac{(FV - PV \cdot F) \cdot r}{F - 1}$.
        - Se $r \le 0$:
          $PMT_{\text{req}} = \frac{FV - PV}{N}$.
      - Calcular $\Delta PMT = \max(1.0, \lceil (PMT_{\text{req}} - PMT_{\text{atual}}) - 10^{-6} \rceil)$ (subtraindo $10^{-6}$ antes de `Math.ceil` para evitar que ruído de ponto flutuante IEEE 754 transforme um delta exato como `200.0` em `201.0`, e garantindo mínimo de `1.00` quando `OFF_TRACK`, com `.setScale(2, RoundingMode.HALF_UP)`), e `requiredMonthlyContribution = pmt.add(additionalMonthlyContributionNeeded).setScale(2, RoundingMode.HALF_UP)`.
      - Calcular `requiredAnnualYieldPercentage` via busca binária (60 iterações no intervalo $[0.0, 200.0]$ % a.a.) para encontrar a rentabilidade anual que leva $PV$ com aportes $PMT_{\text{atual}}$ até $FV$ em $N = \text{estimatedTargetMonths}$ meses, usando o limite superior `high` arredondado com `RoundingMode.CEILING` em 2 casas decimais (garantindo que aplicar `requiredAnnualYieldPercentage` em `calculate(...)` resulte em `totalMonths <= estimatedTargetMonths`; se $PV == 0$ e $PMT_{\text{atual}} == 0$, usar `BigDecimal.valueOf(12.00).setScale(2, RoundingMode.HALF_UP)` como referência mínima).
      - Montar `actionSuggestions` formatadas deterministicamente com `DecimalFormat` e `DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"))` (nunca depender do `Locale.getDefault()` da JVM):
        - `"Aumente seu aporte em R$ %s para voltar ao trilho"` (ex: `"Aumente seu aporte em R$ 685,00 para voltar ao trilho"`)
        - `"Revise seu yield alvo para %s%% a.a. para atingir a meta no prazo estimado"` (ex: `"Revise seu yield alvo para 12,85% a.a. para atingir a meta no prazo estimado"`)

- [x] **Task 2: Criar `GoalHealthAssessmentDto` e `EvaluateGoalHealthUseCase`** (AC: 5)
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalHealthAssessmentDto.java`:
    ```java
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GoalHealthAssessmentDto(
        String status,
        boolean isOnTrack,
        int estimatedTargetMonths,
        int projectedTotalMonths,
        int monthsDelay,
        BigDecimal additionalMonthlyContributionNeeded,
        BigDecimal requiredMonthlyContribution,
        BigDecimal requiredAnnualYieldPercentage,
        List<String> actionSuggestions
    ) {}
    ```
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/EvaluateGoalHealthUseCase.java`:
    - POJO com `@Slf4j` e `@RequiredArgsConstructor` (sem anotações Spring).
    - Injetar `FinancialGoalRepository`, `GetPortfolioSummaryUseCase` e `TimeToGoalCalculator`.
    - Implementar `execute(String userId): Optional<GoalHealthAssessmentDto>`.
    - Registrar `log.info("Evaluating financial goal health for user {}", userId)`.
    - Se o usuário não possuir meta ativa, retornar `Optional.empty()`.
    - Capturar qualquer `Exception`, registrar `log.error("Error evaluating financial goal health for user {}", userId, ex)` e relançar (`AGENTS.md`).

---

### Backend — Módulo `api` (Web + Configuration)

- [x] **Task 3: Expor endpoint `GET /api/v1/financial-goals/health`** (AC: 5)
  - [x] Em `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java`:
    - Registrar bean `@Bean public EvaluateGoalHealthUseCase evaluateGoalHealthUseCase(...)`.
  - [x] Em `backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java`:
    - Injetar `EvaluateGoalHealthUseCase`.
    - Adicionar endpoint:
      ```java
      @GetMapping("/health")
      public ResponseEntity<?> getGoalHealth(@AuthenticationPrincipal Jwt jwt) {
          String userId = jwt.getSubject();
          log.info("Received request to evaluate financial goal health for user {}", userId);
          Optional<GoalHealthAssessmentDto> result = evaluateGoalHealthUseCase.execute(userId);
          return ResponseEntity.ok(Collections.singletonMap("data", result.orElse(null)));
      }
      ```

---

### Frontend Flutter — Feature `goals` & `dashboard`

- [x] **Task 4: Criar entidade `GoalHealthAssessment` e expandir `TimeToGoalCalculator` (Dart)** (AC: 1, 2, 3)
  - [x] Criar `frontend/lib/src/features/goals/domain/entities/goal_health_assessment.dart`:
    ```dart
    enum GoalHealthStatus { onTrack, offTrack, achieved }

    class GoalHealthChecklistItem {
      final String title;
      final String description;
      final bool isHealthy;

      const GoalHealthChecklistItem({
        required this.title,
        required this.description,
        required this.isHealthy,
      });
    }

    class GoalHealthAssessment {
      final GoalHealthStatus status;
      final bool isOnTrack;
      final int estimatedTargetMonths;
      final int projectedTotalMonths;
      final int monthsDelay;
      final double additionalMonthlyContributionNeeded;
      final double requiredMonthlyContribution;
      final double requiredAnnualYieldPercentage;
      final List<GoalHealthChecklistItem> checklistItems;
      final List<String> actionSuggestions;

      const GoalHealthAssessment({
        required this.status,
        required this.isOnTrack,
        required this.estimatedTargetMonths,
        required this.projectedTotalMonths,
        required this.monthsDelay,
        required this.additionalMonthlyContributionNeeded,
        required this.requiredMonthlyContribution,
        required this.requiredAnnualYieldPercentage,
        required this.checklistItems,
        required this.actionSuggestions,
      });
    }
    ```
  - [x] Em `frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart`:
    - Adicionar método estático `TimeToGoalCalculator.evaluateHealth(...)`:
      ```dart
      static GoalHealthAssessment evaluateHealth({
        String goalType = 'PATRIMONY_TARGET',
        required double targetValue,
        required int estimatedYears,
        required double currentEquity,
        required double monthlyContribution,
        required double annualYieldPercentage,
        DateTime? referenceDate,
      })
      ```
    - Validar `.isFinite` em todos os parâmetros `double` (`targetValue`, `currentEquity`, `monthlyContribution`, `annualYieldPercentage`) no início do método para evitar `UnsupportedError: Infinity or NaN toInt`.
    - Calcular `GoalProjection projection = calculate(...)` e `int estimatedTargetMonths = math.max(1, estimatedYears) * 12`.
    - Aplicar a mesma matemática exata do backend para `additionalMonthlyContributionNeeded` ($\Delta PMT = \max(1.0, ((PMT_{\text{req}} - PMT_{\text{atual}}) - 10^{-6}).\text{ceilToDouble}())$) e `requiredAnnualYieldPercentage` (busca binária em $[0.0, 200.0]$ arredondando `high` para cima em 2 casas decimais: `(high * 100.0).ceil() / 100.0`).
    - Gerar os 3 itens de `checklistItems`:
      1. **Prazo Estimado** (`isHealthy: isOnTrack`, detalhando prazo desejado vs. projetado)
      2. **Ritmo de Aportes Mensais** (`isHealthy: isOnTrack`; quando `offTrack`, incluir na descrição a orientação concisa com valor inteiro sem centavos se aplicável, ex: `"Aumente seu aporte em R\$ 200 para voltar ao trilho"`, garantindo compatibilidade literal com o texto do épico)
      3. **Rentabilidade / Yield Alvo** (`isHealthy: isOnTrack`, detalhando rentabilidade atual vs. necessária)
    - Gerar `actionSuggestions` quando `status == GoalHealthStatus.offTrack`:
      - Sugestão 1: `"Aumente seu aporte em R\$ X,XX para voltar ao trilho"` (formatado em moeda `pt_BR` com 2 casas decimais, ex: `"Aumente seu aporte em R\$ 200,00 para voltar ao trilho"`).
      - Sugestão 2: `"Revise seu yield alvo para Y,YY% a.a. para atingir a meta no prazo estimado"`.

- [x] **Task 5: Criar componente `GoalHealthChecklistCard` e integrar em `DashboardPage` e `GoalsPage`** (AC: 1, 2, 3, 4)
  - [x] Criar `frontend/lib/src/features/goals/presentation/widgets/goal_health_checklist_card.dart`:
    - Props:
      - `required FinancialGoal goal`
      - `required double currentEquity`
      - `required double annualYieldPercentage`
      - `GoalHealthAssessment? assessment` (opcional; se nulo, calcula via `TimeToGoalCalculator.evaluateHealth(...)`)
    - Cores mandatórias:
      - `kWarningRed = Color(0xFFEF4444)` (cor exigida pelo AC: `Warning #EF4444`)
      - `kEmerald = Color(0xFF10B981)`
      - `kNavyBlue = Color(0xFF1B2A4A)`
    - Estrutura visual:
      - **Header:**
        - Ícone contextual (`Icons.health_and_safety_outlined` ou `Icons.fact_check_outlined`)
        - Título: **"Checklist de Saúde da Meta"**
        - **Badge de Status (`Key('goal_health_status_badge')`)**:
          - Quando `offTrack`: fundo `kWarningRed.withAlpha(30)`, borda `kWarningRed`, ícone `Icons.warning_amber_rounded` (`color: kWarningRed`) e texto **"Atenção: Fora do Trilho"** em `kWarningRed`.
          - Quando `onTrack`: fundo `kEmerald.withAlpha(30)`, borda `kEmerald`, ícone `Icons.check_circle_outline` (`color: kEmerald`) e texto **"No Trilho"** em `kEmerald`.
          - Quando `achieved`: fundo `kEmerald.withAlpha(30)`, borda `kEmerald`, ícone `Icons.verified_rounded` (`color: kEmerald`) e texto **"Meta Concluída"** em `kEmerald`.
      - **Lista de Itens do Checklist:**
        - Renderizar cada `GoalHealthChecklistItem` com ícone de check verde (`#10B981`) se `isHealthy` ou ícone de alerta vermelho (`#EF4444`) se `!isHealthy`, título em negrito e descrição clara.
      - **Banner de Ações Recomendadas (`Key('goal_health_warning_banner')`):**
        - Quando `status == GoalHealthStatus.offTrack`: exibir container destacado com borda e fundo suave em `kWarningRed` (`#EF4444`), listando as sugestões acionáveis (`actionSuggestions`), garantindo a presença explícita das expressões **"Aumente seu aporte em R$ ... para voltar ao trilho"** e **"Revise seu yield alvo"**.
        - Quando `onTrack` ou `achieved`: exibir mensagem positiva em `kEmerald` (`#10B981`).
      - **Guardrail de Testes (`goals_page_test.dart`):**
        - **NUNCA** renderizar um widget `Text` cujo texto seja exclusivamente um valor monetário isolado como `'R\$ 200.000,00'` ou `'R\$ 300.000,00'`, para preservar o `findsNWidgets(2)` em `goals_page_test.dart`.
  - [x] Em `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`:
    - Quando `goalsProvider != null && currentGoal != null`, renderizar `GoalHealthChecklistCard` logo abaixo do `MotivationalClockCard`:
      ```dart
      if (currentGoal != null) ...[
        const SizedBox(height: 24),
        GoalHealthChecklistCard(
          goal: currentGoal,
          currentEquity: provider.summary?.totalEquity ?? 0.0,
          annualYieldPercentage: provider.summary?.grossYieldPercentage ?? 0.0,
        ),
      ],
      ```
  - [x] Em `frontend/lib/src/features/goals/presentation/pages/goals_page.dart`:
    - Quando `goalsProvider.currentGoal != null`, renderizar `GoalHealthChecklistCard` abaixo do `MotivationalClockCard` (e acima do `WhatIfSimulatorCard`).

---

### Testes & Validação

- [x] **Task 6: Testes Unitários e de Controller no Backend** (AC: 1, 2, 3, 5, 6)
  - [x] Atualizar `backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java`:
    - Testar `evaluateHealth(...)` quando a projeção ultrapassa `estimatedYears * 12` (`OFF_TRACK`), validando `monthsDelay > 0`, `additionalMonthlyContributionNeeded > 0`, `requiredAnnualYieldPercentage > annualYieldPercentage` e sugestões de ação.
    - Testar `evaluateHealth(...)` quando o aporte adicional calculado (`requiredMonthlyContribution`) é aplicado de volta no `calculate(...)`, confirmando que o novo `totalMonths <= estimatedTargetMonths`.
    - Testar `evaluateHealth(...)` quando a meta está dentro do prazo (`ON_TRACK`), quando já foi atingida (`ACHIEVED`) e quando é inalcançável (`OFF_TRACK` com `monthsDelay == -1`).
  - [x] Criar `backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/EvaluateGoalHealthUseCaseTest.java`:
    - Testar avaliação bem-sucedida com dados do `GetPortfolioSummaryUseCase` e `FinancialGoalRepository`.
    - Testar retorno `Optional.empty()` quando usuário não possui meta.
    - Testar captura, log `ERROR` e propagação de exceção inesperada.
  - [x] Atualizar `backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java`:
    - Adicionar `@MockBean private EvaluateGoalHealthUseCase evaluateGoalHealthUseCase;`.
    - Testar `GET /api/v1/financial-goals/health` retornando `200 OK` com `status = "OFF_TRACK"`, `isOnTrack = false`, `additionalMonthlyContributionNeeded` e `actionSuggestions`.
    - Testar `GET /api/v1/financial-goals/health` retornando `200 OK` com `{ "data": null }` quando o usuário não possui meta ativa.

- [x] **Task 7: Testes Unitários e de Widget no Flutter** (AC: 1, 2, 3, 4, 6)
  - [x] Atualizar `frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart`:
    - Testar `TimeToGoalCalculator.evaluateHealth(...)` para os estados `offTrack`, `onTrack`, `achieved` e meta inalcançável.
    - Verificar que somar `additionalMonthlyContributionNeeded` ao aporte atual faz `TimeToGoalCalculator.calculate(...)` atingir a meta em $\le \text{estimatedTargetMonths}$.
  - [x] Criar `frontend/test/features/goals/presentation/widgets/goal_health_checklist_card_test.dart`:
    - Testar estado `offTrack` (projeção ultrapassa prazo estimado): verifica renderização do badge de atenção com cor `Color(0xFFEF4444)` (`#EF4444`), texto `"Atenção: Fora do Trilho"`, itens de checklist em alerta e sugestões `"Aumente seu aporte em ... para voltar ao trilho"` e `"Revise seu yield alvo"`.
    - Testar caso exato em que faltam R$ 200 de aporte mensal: verifica sugestão `"Aumente seu aporte em R$ 200,00 para voltar ao trilho"` (e compatibilidade textual).
    - Testar estado `onTrack` (projeção dentro do prazo): verifica badge `"No Trilho"` em `#10B981` e ausência de banner de erro.
    - Testar estado `achieved` ($PV \ge FV$): verifica badge `"Meta Concluída"`.
  - [x] Atualizar `frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart` e `frontend/test/features/goals/presentation/pages/goals_page_test.dart`:
    - Verificar renderização do `GoalHealthChecklistCard` quando há meta ativa no `DashboardPage` e na `GoalsPage`, sem quebrar nenhum teste existente.

- [x] **Task 8: Execução de Validação Completa** (AC: 6)
  - [x] Executar `mvn clean test -pl common`
  - [x] Executar `mvn clean test -pl api`
  - [x] Executar `flutter analyze`
  - [x] Executar `flutter test`

### Review Findings

- [x] [Review][Patch] Proteger cálculo de `compoundFactor` e `factor` contra `0.0 * Infinity` (`NaN`) no backend [`backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java:258-341`]
- [x] [Review][Patch] Proteger cálculo de `compoundFactor` e `factor` contra `0.0 * double.infinity` (`NaN`) no frontend [`frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart:410-510`]
- [x] [Review][Patch] Normalizar interpolação de `$safeEstimatedYears` nas descrições dos itens do checklist [`frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart:275-451`]

---

## Dev Notes

### 1. Racional Matemático do Diagnóstico de Saúde da Meta (`evaluateHealth`)

Dado um objetivo financeiro com valor alvo efetivo $FV$, patrimônio atual $PV$, aporte mensal configurado $PMT_{\text{atual}}$, rentabilidade anual atual $\text{yield}_{\text{atual}}$ (% a.a.), e prazo estimado pelo usuário $Y_{\text{est}}$ anos ($N = 12 \times Y_{\text{est}}$ meses):

1. **Verificação de Prazo (On-Track vs. Off-Track):**
   - Calcula-se a projeção atual via `TimeToGoalCalculator.calculate(...)`, obtendo $n_{\text{proj}} = \text{projection.totalMonths}$.
   - Se $PV \ge FV$ (`projection.isAchieved`): status = `ACHIEVED` (`isOnTrack = true`).
   - Se `!projection.isUnreachable` e $n_{\text{proj}} \le N$: status = `ON_TRACK` (`isOnTrack = true`, `monthsDelay = 0`).
   - Se `projection.isUnreachable` ou $n_{\text{proj}} > N$: status = `OFF_TRACK` (`isOnTrack = false`, `monthsDelay = isUnreachable ? -1 : n_proj - N`).

2. **Cálculo do Aporte Adicional Necessário ($\Delta PMT$) quando `OFF_TRACK`:**
   Para atingir $FV$ em exatos $N$ meses mantendo a taxa mensal atual $r = \frac{\text{yield}_{\text{atual}}}{12 \times 100}$:
   $$FV = PV \cdot (1 + r)^N + PMT_{\text{req}} \cdot \frac{(1 + r)^N - 1}{r}$$
   Logo:
   - Se $r > 0$:
     $$PMT_{\text{req}} = \max\left(0, \frac{(FV - PV \cdot (1 + r)^N) \cdot r}{(1 + r)^N - 1}\right)$$
   - Se $r \le 0$:
     $$PMT_{\text{req}} = \max\left(0, \frac{FV - PV}{N}\right)$$
   - O incremento mensal necessário é:
     $$\Delta PMT = \max\left(1.0, \lceil (PMT_{\text{req}} - PMT_{\text{atual}}) - 10^{-6} \rceil\right)$$
     *(Nota: Subtrair $10^{-6}$ antes do `Math.ceil` / `.ceilToDouble()` evita que ruído IEEE 754 em deltas exatos como `200.00000000000006` vire `201.0`, enquanto o arredondamento para cima garante que aplicar $PMT_{\text{atual}} + \Delta PMT$ mantenha o tempo projetado $\le N$ meses.)*

3. **Cálculo do Yield Anual Necessário ($\text{yield}_{\text{req}}$) quando `OFF_TRACK`:**
   Mantendo o aporte mensal atual $PMT_{\text{atual}}$, buscamos a taxa anual $y \in [0\%, 200\%]$ tal que o montante acumulado em $N$ meses iguale $FV$:
   - Se $PV + PMT_{\text{atual}} \cdot N \ge FV$: $\text{yield}_{\text{req}} = 0\%$.
   - Se $PV == 0$ e $PMT_{\text{atual}} == 0$: nenhum yield multiplica zero; usar fallback de referência `12.00%`.
   - Caso contrário, executar 60 iterações de busca binária (bisseção) em `low = 0.0`, `high = 200.0` (onde $r(y) = \frac{y}{1200}$ e $E(y) = PV(1+r)^N + PMT_{\text{atual}}\frac{(1+r)^N - 1}{r}$), arredondando `high` para cima em 2 casas decimais (`RoundingMode.CEILING` / `(high * 100.0).ceil() / 100.0`) para garantir que a projeção com esse yield atinja a meta em $\le N$ meses.

### 2. Análise de Arquivos Existentes a Modificar (UPDATE Files Analysis)

- **`backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`**
  - *Estado atual:* Possui `calculate(...)` e `simulate(...)`.
  - *O que muda:* Adição do método `evaluateHealth(...)` que reutiliza `calculate(...)` e calcula `GoalHealthAssessment`.
  - *O que preservar:* Comportamento e assinaturas exatas de `calculate(...)` e `simulate(...)`.

- **`backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java` & `GoalsServiceConfiguration.java` & `FinancialGoalsControllerTest.java`**
  - *Estado atual:* Expõe `POST`, `PUT`, `GET`, `GET /projection` e `POST /projection/simulate`.
  - *O que muda:* Injeção de `EvaluateGoalHealthUseCase` e novo endpoint `GET /api/v1/financial-goals/health`.
  - *O que preservar:* **OBRIGATÓRIO** adicionar `@MockBean private EvaluateGoalHealthUseCase evaluateGoalHealthUseCase;` em `FinancialGoalsControllerTest.java` para que o contexto do `@AutoConfigureMockMvc` não falhe nos testes existentes. Usar `Collections.singletonMap("data", result.orElse(null))` e nunca `Map.of("data", null)`.

- **`frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart` & `dashboard_page_test.dart`**
  - *Estado atual:* Renderiza `MotivationalClockCard` quando `goalsProvider != null` (inclusive em Empty State quando `currentGoal == null`).
  - *O que muda:* Renderizar `GoalHealthChecklistCard` logo abaixo do `MotivationalClockCard` **somente quando `currentGoal != null`**.
  - *O que preservar:* Quando `goalsProvider == null` ou `currentGoal == null`, não exibir `GoalHealthChecklistCard`, mantendo intactos os 3 testes existentes em `dashboard_page_test.dart`.

- **`frontend/lib/src/features/goals/presentation/pages/goals_page.dart` & `goals_page_test.dart`**
  - *Estado atual:* Quando `goalsProvider.currentGoal != null`, exibe `MotivationalClockCard`, `WhatIfSimulatorCard` e `GoalSummaryCard`.
  - *O que muda:* Inclusão do `GoalHealthChecklistCard` abaixo do `MotivationalClockCard` quando `goalsProvider.currentGoal != null`.
  - *O que preservar:* Em `goals_page_test.dart`, existem asserções estritas `expect(find.text('R\$ 200.000,00'), findsNWidgets(2))` e `expect(find.text('R\$ 300.000,00'), findsNWidgets(2))`. Portanto, dentro do `GoalHealthChecklistCard`, **NUNCA** renderizar um widget `Text` contendo apenas um valor monetário isolado (sempre embutir em frases completas).

### 3. Inteligência das Histórias Anteriores (Stories 5.1, 5.2 e 5.3)

- **Não usar `withOpacity()`:** O Flutter 3.11+ com `flutter_lints` acusa warning/info de deprecação em `withOpacity`. Usar sempre `.withAlpha((alpha * 255).round())` ou `.withValues(alpha: ...)`.
- **Guardrails `.isFinite` em Dart:** Sempre validar `.isFinite` antes de chamar `.ceil()`, `.round()` ou formatar números para evitar `UnsupportedError: Infinity or NaN toInt`.
- **Conformidade com `AGENTS.md`:**
  - Sempre abrir branch atualizada antes de implementar (`git checkout -b feature/5-4-goal-health-alert-checklist main`).
  - Logs `INFO` obrigatórios na entrada de todas as APIs/use cases e `ERROR` com stack trace nas capturas/lançamentos de exceções.
  - Rodar `mvn clean test`, `flutter analyze` e `flutter test` antes de concluir.

### 4. Estrutura de Arquivos — Novos e Modificados

```
NOVOS ARQUIVOS:
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalHealthStatus.java
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalHealthAssessment.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalHealthAssessmentDto.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/EvaluateGoalHealthUseCase.java
backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/EvaluateGoalHealthUseCaseTest.java

frontend/lib/src/features/goals/domain/entities/goal_health_assessment.dart
frontend/lib/src/features/goals/presentation/widgets/goal_health_checklist_card.dart
frontend/test/features/goals/presentation/widgets/goal_health_checklist_card_test.dart

ARQUIVOS MODIFICADOS:
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java
backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java
backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java

frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart
frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart
frontend/lib/src/features/goals/presentation/pages/goals_page.dart
frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart
frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart
frontend/test/features/goals/presentation/pages/goals_page_test.dart
```

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 5.4: Checklist de Saúde da Meta]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 5: Metas & Projeção Financeira (Time-to-Goal)]
- [Source: docs/bmad/planning-artifacts/prd.md#FR24]
- [Source: docs/bmad/planning-artifacts/prd.md#FR25]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Visual Design Foundation]
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean Architecture — Padrão do Projeto]
- [Source: docs/bmad/project-context.md]
- [Source: docs/bmad/implementation-artifacts/5-3-what-if-sensitivity-simulator.md]
- [Source: frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart]
- [Source: frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart]
- [Source: frontend/lib/src/features/goals/presentation/pages/goals_page.dart]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java]
- [Source: backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java]

---

## Dev Agent Record

### Agent Model Used

Gemini 4 Argon (Medium)

### Debug Log References

### Completion Notes List

- Ultimate context engine analysis completed - comprehensive developer guide created
- Implementadas entidades de domínio `GoalHealthStatus` e `GoalHealthAssessment`, DTO `GoalHealthAssessmentDto`, `EvaluateGoalHealthUseCase` e evolução do `TimeToGoalCalculator.evaluateHealth(...)` no backend (`common`) com logs `INFO` e `ERROR` (`AGENTS.md`), precisão `BigDecimal` e datas em `ZoneOffset.UTC`.
- Exposto o endpoint `GET /api/v1/financial-goals/health` no `FinancialGoalsController` e configurado bean no `GoalsServiceConfiguration`.
- Implementado diagnóstico `TimeToGoalCalculator.evaluateHealth(...)` em Dart, entidade `GoalHealthAssessment` e widget `GoalHealthChecklistCard` com suporte aos estados `offTrack` (alerta `#EF4444`), `onTrack` e `achieved` (`#10B981`), integrado em `DashboardPage` e `GoalsPage`.
- Adicionados testes unitários e de widget completos no backend e frontend; `mvn clean test -pl common`, `mvn clean test -pl api`, `flutter analyze` e `flutter test` executados com 100% de sucesso.

### File List

- `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalHealthStatus.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalHealthAssessment.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalHealthAssessmentDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/EvaluateGoalHealthUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/EvaluateGoalHealthUseCaseTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java`
- `frontend/lib/src/features/goals/domain/entities/goal_health_assessment.dart`
- `frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart`
- `frontend/lib/src/features/goals/presentation/widgets/goal_health_checklist_card.dart`
- `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`
- `frontend/lib/src/features/goals/presentation/pages/goals_page.dart`
- `frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart`
- `frontend/test/features/goals/presentation/widgets/goal_health_checklist_card_test.dart`
- `frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart`
- `frontend/test/features/goals/presentation/pages/goals_page_test.dart`
- `docs/bmad/implementation-artifacts/5-4-goal-health-alert-checklist.md`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-25: Implementação completa da Story 5.4 (Checklist de Saúde da Meta) no backend (`common`, `api`) e frontend Flutter (`goals`, `dashboard`), com cobertura de testes unitários, de integração web e de widgets.
- 2026-10-25: Code Review adversário em 3 camadas (`Blind Hunter`, `Edge Case Hunter`, `Acceptance Auditor`) concluído, patches preventivos aplicados em `TimeToGoalCalculator` (Java e Dart), testes e análise estática 100% aprovados; status atualizado para `done`.
