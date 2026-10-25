---
baseline_commit: 7739561ff5681a6cfdfd1772ca95ddee68a4626f
---

# Story 5.3: Simulador de Sensibilidade ("E se?")

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** investidor,
**I want** simular como um aumento (ou variação) no aporte mensal ou na rentabilidade esperada afetaria meu tempo para atingir a meta financeira, visualizando a comparação gráfica entre o cenário atual e o cenário simulado,
**So that** eu possa ajustar meu estilo de vida, elevar meus aportes ou calibrar minha estratégia de investimentos com clareza imediata do impacto em meses e anos economizados.

## Acceptance Criteria

1. **Given** que o usuário autenticado possui uma meta financeira ativa (`currentGoal != null`) e acessa a tela de metas e projeção (`GoalsPage`, rota `/metas`)
   **When** a tela é renderizada com a meta ativa carregada
   **Then** o sistema exibe o componente interativo **`WhatIfSimulatorCard`** ("Simulador de Sensibilidade — E se?") contendo:
   - Slider interativo de **"Aporte Mensal Simulado"** (inicializado com o `monthlyContribution` da meta ativa, variando de `R$ 0,00` até $\max(\text{monthlyContribution} \times 3, \text{R\$ } 10.000,00)$)
   - Slider interativo de **"Rendimento Esperado (% a.a.)"** (inicializado com o yield anual atual do portfólio `grossYieldPercentage` limitado a $\ge 0\%$, variando de `0,0%` até $\max(30,0\%, \text{grossYieldPercentage} \times 1,5)$)
   - Botão de ação rápida **"Resetar Simulação"** para restaurar os sliders aos valores originais da carteira/meta
   - Comparativo lado a lado entre **Trajetória Atual** (anos e meses) e **Cenário Simulado** (anos e meses) usando tipografia `GoogleFonts.jetBrainsMono`
   - **Gráfico de Projeção Comparativo (`GoalProjectionChart`)** exibindo a curva de evolução patrimonial da Trajetória Atual vs. Cenário Simulado ao longo do tempo até o Valor Alvo ($FV$).

2. **Given** que o usuário está visualizando o `WhatIfSimulatorCard` na tela de projeção (`GoalsPage`)
   **When** ele move o slider de **"Aporte Mensal"** ou o slider de **"Rendimento Esperado (%)"** para um cenário mais favorável que reduz o tempo até a meta ($n_{\text{simulado}} < n_{\text{atual}}$, com ambos os cenários alcançáveis e $PV < FV$)
   **Then** o sistema recalcula **instantaneamente** (em tempo real no cliente, $< 16\text{ms}$, sem bloqueio de rede) o novo Time-to-Goal via `TimeToGoalCalculator` e atualiza o gráfico de projeção (`GoalProjectionChart`)
   **And** exibe em destaque (cor `kEmerald` `#10B981`) o banner de impacto:
   **"Isso encurtaria sua jornada em Z meses!"** (onde $Z = n_{\text{atual}} - n_{\text{simulado}}$).

3. **Given** que o usuário move qualquer um dos sliders para um cenário menos favorável ($n_{\text{simulado}} > n_{\text{atual}}$, ambos alcançáveis)
   **When** o recálculo instantâneo é executado
   **Then** o sistema atualiza o contador simulado e o gráfico de projeção
   **And** exibe o alerta informativo (cor `kAmberGold` `#F59E0B`):
   **"Isso aumentaria sua jornada em Z meses."** (onde $Z = n_{\text{simulado}} - n_{\text{atual}}$).

4. **Given** que os valores simulados nos sliders produzem exatamente o mesmo número de meses da projeção original ($n_{\text{simulado}} == n_{\text{atual}}$)
   **When** o componente é exibido
   **Then** exibe a mensagem neutra orientativa:
   **"Ajuste os controles de aporte ou rentabilidade para simular o impacto no seu tempo até a meta."**

5. **Given** casos limites durante a simulação:
   - **Cenário A (Meta já atingida, $PV \ge FV$):** exibe mensagem em `kEmerald`: **"🎉 Sua meta já foi atingida! Use os controles para simular a evolução do seu patrimônio."**
   - **Cenário B (Cenário atual inalcançável, mas cenário simulado alcançável):** exibe em `kEmerald`: **"Com este cenário, sua meta passa a ser alcançável em X anos e Y meses!"**
   - **Cenário C (Cenário simulado inalcançável, ex: aporte R$ 0 e yield 0% com $PV < FV$):** trata sem exceções matemáticas (divisão por zero ou `NaN`/`Infinity`) e exibe em `kAmberGold`: **"Neste cenário simulado, a meta não seria alcançada. Aumente o aporte ou a rentabilidade esperada."**

6. **Given** que o backend recebe `POST /api/v1/financial-goals/projection/simulate` com payload JSON `{ "simulatedMonthlyContribution": 3500.00, "simulatedAnnualYieldPercentage": 12.00 }`
   **When** o usuário está autenticado com token JWT válido
   **Then** o backend busca a meta ativa (`FinancialGoal`) e o resumo do portfólio (`PortfolioSummary`), executa `SimulateGoalProjectionUseCase` e retorna `200 OK` com `GoalSimulationDto`:
   ```json
   {
     "data": {
       "baselineProjection": {
         "targetValue": 1000000.00,
         "currentEquity": 150000.00,
         "monthlyContribution": 2000.00,
         "annualYieldPercentage": 10.50,
         "totalMonths": 142,
         "years": 11,
         "months": 10,
         "targetDate": "2038-08-25T10:00:00Z",
         "progressPercentage": 15.00,
         "isAchieved": false,
         "isUnreachable": false
       },
       "simulatedProjection": {
         "targetValue": 1000000.00,
         "currentEquity": 150000.00,
         "monthlyContribution": 3500.00,
         "annualYieldPercentage": 12.00,
         "totalMonths": 106,
         "years": 8,
         "months": 10,
         "targetDate": "2035-08-25T10:00:00Z",
         "progressPercentage": 15.00,
         "isAchieved": false,
         "isUnreachable": false
       },
       "monthsDifference": 36
     }
   }
   ```
   **And** se o usuário não possuir meta ativa, retorna `200 OK` com `{ "data": null }` (usando `Collections.singletonMap("data", null)`)
   **And** registra log `INFO` na entrada do controller (`"Received request to simulate time-to-goal projection for user {}"`) e no use case (`"Simulating time-to-goal sensitivity for user {}"`), e log `ERROR` com stack trace em qualquer captura/lançamento de exceção (`AGENTS.md`)
   **And** utiliza estritamente `ZoneOffset.UTC` e `BigDecimal`.

7. **Given** que a implementação está concluída
   **When** a suíte de validação é executada
   **Then** `mvn clean test -pl common` e `mvn clean test -pl api` passam com 100% de sucesso
   **And** `flutter analyze` retorna zero issues e `flutter test` passa integralmente sem regressões nos testes existentes (`goals_page_test.dart`, `motivational_clock_card_test.dart`, `goals_provider_test.dart`).

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domain + Application)

- [x] **Task 1: Evoluir `TimeToGoalCalculator` e criar modelo `GoalSimulationResult`** (AC: 2, 3, 5, 6)
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalSimulationResult.java`:
    ```java
    @Builder
    @Getter
    public class GoalSimulationResult {
        private GoalProjection baselineProjection;
        private GoalProjection simulatedProjection;
        private int monthsDifference;
    }
    ```
  - [x] Em `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`:
    - Adicionar método `simulate(...)`:
      ```java
      public GoalSimulationResult simulate(
          GoalType goalType,
          BigDecimal rawTargetValue,
          BigDecimal currentEquity,
          BigDecimal baselineMonthlyContribution,
          BigDecimal baselineAnnualYieldPercentage,
          BigDecimal simulatedMonthlyContribution,
          BigDecimal simulatedAnnualYieldPercentage,
          OffsetDateTime referenceDate
      )
      ```
    - Calcular `baselineProjection` e `simulatedProjection` reutilizando `calculate(...)`:
      - `baselineProjection = calculate(goalType, rawTargetValue, currentEquity, baselineMonthlyContribution, baselineAnnualYieldPercentage, referenceDate)`
      - `simulatedProjection = calculate(GoalType.PATRIMONY_TARGET, baselineProjection.getTargetValue(), currentEquity, simulatedMonthlyContribution, simulatedAnnualYieldPercentage, referenceDate)`
      - **Regra canônica para `GoalType.MONTHLY_INCOME_TARGET`:** A projeção simulada **DEVE** passar explicitamente `GoalType.PATRIMONY_TARGET` e `baselineProjection.getTargetValue()` como valor alvo fixo ($FV$). Isso garante que o montante patrimonial alvo não mude ao mover o slider de rentabilidade, mantendo ambas as curvas rumo exatamente à mesma linha de chegada ($FV$) e assegurando que aumentar a rentabilidade sempre reduza ou mantenha o tempo de acumulação.
    - Calcular `monthsDifference`:
      - Se `!baselineProjection.isUnreachable() && !simulatedProjection.isUnreachable()`: `monthsDifference = baselineProjection.getTotalMonths() - simulatedProjection.getTotalMonths()` (positivo = meses economizados; negativo = meses adicionais).
      - Caso contrário: `monthsDifference = 0`.

- [x] **Task 2: Criar DTOs `SimulateGoalProjectionRequest` e `GoalSimulationDto` e Use Case `SimulateGoalProjectionUseCase`** (AC: 6)
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/SimulateGoalProjectionRequest.java`:
    ```java
    public record SimulateGoalProjectionRequest(
        @NotNull @PositiveOrZero BigDecimal simulatedMonthlyContribution,
        @NotNull @PositiveOrZero BigDecimal simulatedAnnualYieldPercentage
    ) {}
    ```
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalSimulationDto.java`:
    ```java
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GoalSimulationDto(
        GoalProjectionDto baselineProjection,
        GoalProjectionDto simulatedProjection,
        int monthsDifference
    ) {}
    ```
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/SimulateGoalProjectionUseCase.java`:
    - POJO com `@Slf4j` e `@RequiredArgsConstructor` (sem anotações Spring).
    - Injetar `FinancialGoalRepository`, `GetPortfolioSummaryUseCase` e `TimeToGoalCalculator`.
    - Implementar `execute(String userId, SimulateGoalProjectionRequest request): Optional<GoalSimulationDto>`.
    - Registrar `log.info("Simulating time-to-goal sensitivity for user {}", userId)`.
    - Se o usuário não tiver meta cadastrada, retornar `Optional.empty()`.
    - Capturar qualquer `Exception`, registrar `log.error("Error simulating time-to-goal sensitivity for user {}", userId, ex)` e relançar.

---

### Backend — Módulo `api` (Web + Configuration)

- [x] **Task 3: Expor endpoint `POST /api/v1/financial-goals/projection/simulate`** (AC: 6)
  - [x] Em `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java`:
    - Registrar bean `@Bean public SimulateGoalProjectionUseCase simulateGoalProjectionUseCase(...)`.
  - [x] Em `backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java`:
    - Injetar `SimulateGoalProjectionUseCase`.
    - Adicionar endpoint:
      ```java
      @PostMapping("/projection/simulate")
      public ResponseEntity<?> simulateProjection(
              @AuthenticationPrincipal Jwt jwt,
              @Valid @RequestBody SimulateGoalProjectionRequest request) {
          String userId = jwt.getSubject();
          log.info("Received request to simulate time-to-goal projection for user {}", userId);
          Optional<GoalSimulationDto> result = simulateGoalProjectionUseCase.execute(userId, request);
          return ResponseEntity.ok(Collections.singletonMap("data", result.orElse(null)));
      }
      ```

---

### Frontend Flutter — Feature `goals`

- [x] **Task 4: Expandir `TimeToGoalCalculator` (Dart) com simulação e geração de pontos da curva de evolução** (AC: 1, 2, 3, 4, 5)
  - [x] Criar `frontend/lib/src/features/goals/domain/entities/goal_simulation_result.dart`:
    ```dart
    import 'goal_projection.dart';

    class ProjectionCurvePoint {
      final int monthIndex;
      final double equity;

      const ProjectionCurvePoint({
        required this.monthIndex,
        required this.equity,
      });
    }

    class GoalSimulationResult {
      final GoalProjection baselineProjection;
      final GoalProjection simulatedProjection;
      final int monthsDifference;
      final List<ProjectionCurvePoint> baselineCurve;
      final List<ProjectionCurvePoint> simulatedCurve;

      const GoalSimulationResult({
        required this.baselineProjection,
        required this.simulatedProjection,
        required this.monthsDifference,
        required this.baselineCurve,
        required this.simulatedCurve,
      });
    }
    ```
  - [x] Em `frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart`:
    - Adicionar método estático `TimeToGoalCalculator.simulate(...)`:
      ```dart
      static GoalSimulationResult simulate({
        String goalType = 'PATRIMONY_TARGET',
        required double targetValue,
        required double currentEquity,
        required double baselineMonthlyContribution,
        required double baselineAnnualYieldPercentage,
        required double simulatedMonthlyContribution,
        required double simulatedAnnualYieldPercentage,
        DateTime? referenceDate,
      })
      ```
    - Calcular `baselineProjection` via `calculate(...)`.
    - Calcular `simulatedProjection` usando `goalType: 'PATRIMONY_TARGET'` e `targetValue: baselineProjection.targetValue` (garantindo que o $FV$ alvo seja idêntico entre base e simulado para comparação justa de tempo e curvas).
    - Calcular `monthsDifference`:
      - Se `!baselineProjection.isUnreachable && !simulatedProjection.isUnreachable`: `baselineProjection.totalMonths - simulatedProjection.totalMonths`.
      - Caso contrário: `0`.
    - Gerar `baselineCurve` e `simulatedCurve` via helper `generateEvolutionCurve(...)`:
      - Determinar horizonte $H$ em meses:
        - Se ambos alcançáveis: $H = \max(\text{baselineProjection.totalMonths}, \text{simulatedProjection.totalMonths})$.
        - Se apenas um alcançável: $H = \text{alcançável.totalMonths}$ (ou `120` se `0`).
        - Se nenhum alcançável ou $H \le 0$: $H = 120$ meses (10 anos padrão para visualização).
      - Gerar até ~20 pontos uniformemente espaçados de $m = 0$ até $m = H$ (incluindo $m = 0$ e $m = H$), calculando o patrimônio acumulado no mês $m$:
        - Se $r > 0$: $E(m) = PV \cdot (1 + r)^m + PMT \cdot \frac{(1 + r)^m - 1}{r}$.
        - Se $r \le 0$: $E(m) = PV + PMT \cdot m$.
        - Limitar $E(m)$ a $\le FV \times 1.25$ no gráfico para evitar distorção visual exponencial quando uma curva é muito mais rápida que a outra.

- [x] **Task 5: Criar widget `GoalProjectionChart` (Gráfico de Projeção CustomPaint)** (AC: 1, 2)
  - [x] Criar `frontend/lib/src/features/goals/presentation/widgets/goal_projection_chart.dart`:
    - Usar `CustomPaint` nativo do Flutter (seguindo o padrão arquitetural de `AssetAllocationDonutChart` — **sem adicionar pacotes externos** no `pubspec.yaml`).
    - Desenhar:
      - Linhas de grade horizontais sutis.
      - Linha horizontal tracejada de **Meta Alvo ($FV$)** no topo em `kAmberGold` (`#F59E0B`).
      - Curva da **Trajetória Atual** em azul-marinho/slate (`kNavyBlue` `#1B2A4A` com alpha 0.55).
      - Curva do **Cenário Simulado ("E se?")** em verde-esmeralda (`kEmerald` `#10B981`, espessura 3.0) com preenchimento gradiente suave abaixo da curva.
      - Legenda clara no topo do gráfico: `"Trajetória Atual"`, `"Cenário Simulado"` e `"Meta Alvo"`.
      - Eixo X indicando início (`"Hoje"`) e horizonte final em anos/meses.

- [x] **Task 6: Criar componente `WhatIfSimulatorCard` e integrar na `GoalsPage`** (AC: 1, 2, 3, 4, 5)
  - [x] Criar `frontend/lib/src/features/goals/presentation/widgets/what_if_simulator_card.dart`:
    - Props:
      - `required FinancialGoal goal`
      - `required double currentEquity`
      - `required double baselineYieldPercentage`
    - Estado interno:
      - `late double _simulatedContribution;`
      - `late double _simulatedYield;`
    - Em `initState` e `didUpdateWidget`:
      - Sincronizar os valores base caso `oldWidget.goal != widget.goal || oldWidget.baselineYieldPercentage != widget.baselineYieldPercentage`:
        - `_simulatedContribution = widget.goal.monthlyContribution;`
        - `_simulatedYield = math.max(0.0, widget.baselineYieldPercentage);`
      - **Guardrail crítico de `Slider`:** sempre calcular `min` e `max` dinâmicos seguros (`maxContribution = math.max(widget.goal.monthlyContribution * 3.0, 10000.0)` e `maxYield = math.max(30.0, math.max(0.0, widget.baselineYieldPercentage) * 1.5)`) e aplicar `.clamp(min, max)` no `value` passado ao `Slider` para jamais disparar `AssertionError` caso a meta ou o yield da carteira tenha valores extremos ou negativos!
      - Ao mover o slider de aporte, arredondar para dezenas ou unidades inteiras (`(newValue).roundToDouble()`) e ao mover o slider de yield, arredondar para 1 casa decimal (`(newValue * 10).round() / 10.0`), evitando ruído de ponto flutuante e garantindo estabilidade ao clicar em **"Resetar Simulação"**.
    - Layout do Card:
      - **Header:** Ícone `Icons.tune_rounded` em `kAmberGold`, título **"Simulador de Sensibilidade (\"E se?\")"** e `TextButton.icon` **"Resetar Simulação"** (visível/habilitado para restaurar os parâmetros originais).
      - **Banner de Impacto Dinâmico:**
        - Quando `monthsDifference > 0` (cenário simulado reduz o tempo): container destacado com fundo `kEmerald.withAlpha(25)`, ícone `Icons.rocket_launch_outlined` e texto em destaque:
          **"Isso encurtaria sua jornada em ${result.monthsDifference} meses!"**
        - Quando `monthsDifference < 0` (cenário simulado aumenta o tempo): container com fundo `kAmberGold.withAlpha(25)`, ícone `Icons.schedule_outlined` e texto:
          **"Isso aumentaria sua jornada em ${result.monthsDifference.abs()} meses."**
        - Tratamento completo dos casos especiais (AC 4 e AC 5).
      - **Painel Comparativo de Tempo:**
        - Dois mini-cards lado a lado:
          - **"Ritmo Atual"**: `${baseline.years} anos e ${baseline.months} meses` (ou `"Inalcançável"` / `"0 anos e 0 meses"`)
          - **"Cenário Simulado"**: `${simulated.years} anos e ${simulated.months} meses` (ou `"Inalcançável"` / `"0 anos e 0 meses"`)
      - **Controles Sliders:**
        - **Slider 1 — Aporte Mensal (`Key('what_if_contribution_slider')`):**
          - Label superior: `"Aporte Mensal"` e valor formatado como `"Simulado: R\$ X.XXX,XX/mês"` (**ATENÇÃO CRÍTICA:** nunca renderizar apenas `'R\$ 200.000,00'` ou `'R\$ 300.000,00'` isolado em um `Text`, para não interferir nos `findsNWidgets(2)` de `goals_page_test.dart`!).
        - **Slider 2 — Rendimento Esperado (% a.a.) (`Key('what_if_yield_slider')`):**
          - Label superior: `"Rendimento Esperado (%)"` e valor formatado como `"Simulado: XX,X% a.a."`.
      - **Gráfico de Projeção (`GoalProjectionChart`):**
        - Exibido abaixo dos sliders, atualizando em tempo real a cada movimento de slider.
  - [x] Em `frontend/lib/src/features/goals/presentation/pages/goals_page.dart`:
    - Quando `goalsProvider.currentGoal != null`, renderizar o `WhatIfSimulatorCard` abaixo do `MotivationalClockCard` (e acima ou abaixo do `GoalSummaryCard`), passando `goal: goalsProvider.currentGoal!`, `currentEquity: currentPatrimony`, `baselineYieldPercentage: grossYieldPercentage`.

---

### Testes & Validação

- [x] **Task 7: Testes Unitários e de Controller no Backend** (AC: 2, 3, 5, 6, 7)
  - [x] Atualizar `backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java`:
    - Testar `simulate(...)` quando aumento de aporte ou yield reduz o tempo (`monthsDifference > 0`).
    - Testar `simulate(...)` quando redução de aporte ou yield aumenta o tempo (`monthsDifference < 0`).
    - Testar `simulate(...)` com `GoalType.MONTHLY_INCOME_TARGET` e casos inalcançáveis.
  - [x] Criar `backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/SimulateGoalProjectionUseCaseTest.java`:
    - Testar simulação bem-sucedida com dados do `GetPortfolioSummaryUseCase` e `FinancialGoalRepository`.
    - Testar retorno `Optional.empty()` quando usuário não possui meta.
    - Testar log e propagação de erro quando ocorre falha inesperada.
  - [x] Atualizar `backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java`:
    - Adicionar `@MockBean private SimulateGoalProjectionUseCase simulateGoalProjectionUseCase;`.
    - Testar `POST /api/v1/financial-goals/projection/simulate` retornando `200 OK` com `baselineProjection`, `simulatedProjection` e `monthsDifference`.
    - Testar `POST /api/v1/financial-goals/projection/simulate` retornando `200 OK` com `{ "data": null }` quando o usuário não possui meta ativa.
    - Testar `POST /api/v1/financial-goals/projection/simulate` retornando `400 Bad Request` quando valores simulados são negativos ou nulos.

- [x] **Task 8: Testes Unitários e de Widget no Flutter** (AC: 1, 2, 3, 4, 5, 7)
  - [x] Atualizar `frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart`:
    - Testar `TimeToGoalCalculator.simulate(...)`: cálculo de `monthsDifference` positivo, negativo e zero, consistência com `MONTHLY_INCOME_TARGET` e geração de pontos de `baselineCurve` e `simulatedCurve`.
  - [x] Criar `frontend/test/features/goals/presentation/widgets/what_if_simulator_card_test.dart`:
    - Testar renderização inicial com valores da meta ativa e do portfólio (exibindo mensagem neutra orientativa do AC 4).
    - Testar interação no slider de **"Aporte Mensal"** (`Key('what_if_contribution_slider')`): ao aumentar o aporte, recalcula o Time-to-Goal, atualiza o `GoalProjectionChart` e exibe `"Isso encurtaria sua jornada em Z meses!"` (AC 2).
    - Testar interação no slider de **"Rendimento Esperado (%)"** (`Key('what_if_yield_slider')`): ao aumentar a rentabilidade, recalcula e exibe `"Isso encurtaria sua jornada em Z meses!"` (AC 2).
    - Testar botão **"Resetar Simulação"** voltando ao estado inicial.
    - Testar caso de redução de aporte exibindo `"Isso aumentaria sua jornada em Z meses."` (AC 3).
    - Testar casos limites (AC 5): meta já atingida ($PV \ge FV$), meta originalmente inalcançável passando a ser alcançável na simulação, e cenário simulado inalcançável.
  - [x] Atualizar `frontend/test/features/goals/presentation/pages/goals_page_test.dart`:
    - Verificar que `WhatIfSimulatorCard` é renderizado quando existe uma meta salva e que todos os testes anteriores continuam passando sem regressões.

- [x] **Task 9: Execução de Validação Completa** (AC: 7)
  - [x] Executar `mvn clean test -pl common`
  - [x] Executar `mvn clean test -pl api`
  - [x] Executar `flutter analyze`
  - [x] Executar `flutter test`

### Review Findings

- [x] [Review][Patch] Preservar precisão original de `baselineYieldPercentage` em `_resetValues()` e validar `.isFinite` em `monthlyContribution` para evitar divergência de arredondamento na inicialização e no botão de reset [`frontend/lib/src/features/goals/presentation/widgets/what_if_simulator_card.dart:50`]
- [x] [Review][Patch] Garantir limite finito seguro (`1e12` em vez de `double.maxFinite`) em `TimeToGoalCalculator.generateEvolutionCurve` e guardrail `.isFinite` em `_GoalProjectionChartPainter.paint` [`frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart:227`, `frontend/lib/src/features/goals/presentation/widgets/goal_projection_chart.dart:165`]

---

## Dev Notes

### 1. Racional Matemático da Simulação e da Curva de Projeção

A simulação compara duas trajetórias partindo do mesmo Patrimônio Inicial ($PV$) rumo ao mesmo Valor Alvo efetivo ($FV$):

1. **Trajetória Base (Atual):**
   - Aporte mensal $PMT_{\text{base}} = \text{goal.monthlyContribution}$
   - Taxa mensal $r_{\text{base}} = \frac{\text{grossYieldPercentage}}{12 \times 100}$
   - Tempo estimado $n_{\text{base}}$ meses (calculado por `TimeToGoalCalculator.calculate`).

2. **Trajetória Simulada ("E se?"):**
   - Aporte mensal $PMT_{\text{sim}}$ (definido pelo slider de Aporte Mensal)
   - Taxa mensal $r_{\text{sim}} = \frac{\text{simulatedAnnualYieldPercentage}}{12 \times 100}$ (definido pelo slider de Rendimento Esperado)
   - Tempo estimado $n_{\text{sim}}$ meses.

3. **Diferença de Tempo ($\Delta n$):**
   $$\Delta n = n_{\text{base}} - n_{\text{sim}}$$
   - Se $\Delta n > 0$: o usuário economiza $\Delta n$ meses $\implies$ **"Isso encurtaria sua jornada em $\Delta n$ meses!"**
   - Se $\Delta n < 0$: a jornada fica $|\Delta n|$ meses mais longa $\implies$ **"Isso aumentaria sua jornada em $|\Delta n|$ meses."**

4. **Pontos da Curva para o Gráfico (`ProjectionCurvePoint`):**
   Para cada instante $t \in [0, H]$ (em meses):
   $$E(t) = \begin{cases} PV \cdot (1 + r)^t + PMT \cdot \frac{(1 + r)^t - 1}{r}, & \text{se } r > 0 \\ PV + PMT \cdot t, & \text{se } r \le 0 \end{cases}$$

### 2. Análise de Arquivos Existentes a Modificar (UPDATE Files Analysis)

- **`backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`**
  - *Estado atual:* Possui métodos `calculate(...)` com suporte a `GoalType.PATRIMONY_TARGET` e `GoalType.MONTHLY_INCOME_TARGET`, tratando $PV \ge FV$, $r > 0$, $r \le 0$ e metas inalcançáveis (`MAX_SUPPORTED_MONTHS = 12000`).
  - *O que muda:* Adição do método `simulate(...)` que calcula `baselineProjection`, `simulatedProjection` (mantendo o `effectiveTargetValue` de `baselineProjection` como alvo fixo para comparação consistente) e `monthsDifference`.
  - *O que preservar:* Assinaturas e comportamento exato dos métodos `calculate(...)` existentes.

- **`backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java` & `GoalsServiceConfiguration.java` & `FinancialGoalsControllerTest.java`**
  - *Estado atual:* Expõe `POST /api/v1/financial-goals`, `PUT /api/v1/financial-goals/{id}`, `GET /api/v1/financial-goals` e `GET /api/v1/financial-goals/projection`.
  - *O que muda:* Injeção de `SimulateGoalProjectionUseCase` e novo endpoint `POST /api/v1/financial-goals/projection/simulate`.
  - *O que preservar:* **OBRIGATÓRIO** declarar `@MockBean private SimulateGoalProjectionUseCase simulateGoalProjectionUseCase;` em `FinancialGoalsControllerTest.java` para que o contexto Spring do `@AutoConfigureMockMvc` suba sem `NoSuchBeanDefinitionException`. Usar `Collections.singletonMap("data", result.orElse(null))` e nunca `Map.of("data", null)`.

- **`frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart`**
  - *Estado atual:* Possui `TimeToGoalCalculator.calculate(...)`.
  - *O que muda:* Adição de `TimeToGoalCalculator.simulate(...)` e `TimeToGoalCalculator.generateEvolutionCurve(...)`.
  - *O que preservar:* Comportamento idêntico de `calculate(...)` testado em `time_to_goal_calculator_test.dart`.

- **`frontend/lib/src/features/goals/presentation/pages/goals_page.dart` & `frontend/test/features/goals/presentation/pages/goals_page_test.dart`**
  - *Estado atual:* Quando `goalsProvider.currentGoal != null`, renderiza `MotivationalClockCard` e `GoalSummaryCard`.
  - *O que muda:* Inclusão do `WhatIfSimulatorCard` abaixo do `MotivationalClockCard` quando `goalsProvider.currentGoal != null`.
  - *O que preservar:*
    1. Em `goals_page_test.dart`, existem asserções estritas `expect(find.text('R\$ 200.000,00'), findsNWidgets(2))` e `expect(find.text('R\$ 300.000,00'), findsNWidgets(2))`. Portanto, dentro de `WhatIfSimulatorCard` e `GoalProjectionChart`, **NUNCA** renderize um widget `Text` cujo conteúdo seja apenas o valor monetário isolado (como `'R\$ 200.000,00'`, `'R\$ 300.000,00'`, `'R\$ 1.000,00'` ou `'R\$ 1.500,00'`). Sempre inclua prefixo ou sufixo na mesma string (ex: `'Simulado: R\$ 1.500,00/mês'`, `'Atual: R\$ 1.500,00/mês'`, `'Alvo: R\$ 300.000,00'`).
    2. Não iniciar `AnimationController.repeat()` sem checar ambiente de teste (`GoalsPage.isTesting` / `WidgetsBinding.instance.runtimeType.toString().contains('Test')`), pois `goals_page_test.dart` usa `tester.pumpAndSettle()`.

### 3. Inteligência das Histórias Anteriores (Stories 5.1 e 5.2)

- **Não usar `withOpacity()`:** O Flutter 3.11+ com `flutter_lints: ^6.0.0` falha no `flutter analyze` se `withOpacity` for usado. Usar sempre `.withAlpha((alpha * 255).round())` ou `.withValues(alpha: ...)`.
- **Segurança de `Slider` no Flutter:** Se o usuário cadastrar uma meta com aporte `R$ 25.000,00` e o `Slider` tiver `max: 10000.0` fixo, o Flutter lançará `AssertionError: value >= min && value <= max`. Sempre calcule `final double maxContribution = math.max(widget.goal.monthlyContribution * 3.0, 10000.0);` e faça `_simulatedContribution.clamp(0.0, maxContribution)` antes de passar ao `Slider`.
- **Conformidade com `AGENTS.md`:**
  - Sempre abrir branch atualizada antes de implementar (`git checkout -b feature/5-3-what-if-sensitivity-simulator main` se em main, ou manter na branch de trabalho designada).
  - Logs `INFO` obrigatórios na entrada de todas as APIs/use cases e `ERROR` com stack trace nas capturas/lançamentos de exceções.
  - Rodar `mvn clean test`, `flutter analyze` e `flutter test` antes de concluir.

### 4. Estrutura de Arquivos — Novos e Modificados

```
NOVOS ARQUIVOS:
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalSimulationResult.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/SimulateGoalProjectionRequest.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalSimulationDto.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/SimulateGoalProjectionUseCase.java
backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/SimulateGoalProjectionUseCaseTest.java

frontend/lib/src/features/goals/domain/entities/goal_simulation_result.dart
frontend/lib/src/features/goals/presentation/widgets/goal_projection_chart.dart
frontend/lib/src/features/goals/presentation/widgets/what_if_simulator_card.dart
frontend/test/features/goals/presentation/widgets/what_if_simulator_card_test.dart

ARQUIVOS MODIFICADOS:
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java
backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java
backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java

frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart
frontend/lib/src/features/goals/presentation/pages/goals_page.dart
frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart
frontend/test/features/goals/presentation/pages/goals_page_test.dart
```

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 5.3: Simulador de Sensibilidade ("E se?")]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 5: Metas & Projeção Financeira (Time-to-Goal)]
- [Source: docs/bmad/planning-artifacts/prd.md#FR24]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Visual Design Foundation]
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean Architecture — Padrão do Projeto]
- [Source: docs/bmad/project-context.md]
- [Source: docs/bmad/implementation-artifacts/5-2-time-to-goal-motivational-clock.md]
- [Source: frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart]
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
- Implementado `GoalSimulationResult` e método `simulate(...)` em `TimeToGoalCalculator` (Java e Dart), mantendo valor alvo efetivo fixo para metas de renda mensal (`MONTHLY_INCOME_TARGET`) e gerando curvas de evolução patrimonial.
- Criados `SimulateGoalProjectionRequest`, `GoalSimulationDto`, `SimulateGoalProjectionUseCase` e endpoint `POST /api/v1/financial-goals/projection/simulate` com logs `INFO` na entrada e `ERROR` com stack trace em exceções, utilizando `ZoneOffset.UTC` e `BigDecimal`.
- Criados widgets `GoalProjectionChart` (via `CustomPaint` nativo) e `WhatIfSimulatorCard` integrados à `GoalsPage` com recálculo instantâneo em tempo real no cliente e cobertura completa de testes unitários e de widget.

### File List

- `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalSimulationResult.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/SimulateGoalProjectionRequest.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalSimulationDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/SimulateGoalProjectionUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/SimulateGoalProjectionUseCaseTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java`
- `frontend/lib/src/features/goals/domain/entities/goal_simulation_result.dart`
- `frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart`
- `frontend/lib/src/features/goals/presentation/widgets/goal_projection_chart.dart`
- `frontend/lib/src/features/goals/presentation/widgets/what_if_simulator_card.dart`
- `frontend/lib/src/features/goals/presentation/pages/goals_page.dart`
- `frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart`
- `frontend/test/features/goals/presentation/widgets/what_if_simulator_card_test.dart`
- `frontend/test/features/goals/presentation/pages/goals_page_test.dart`
- `docs/bmad/implementation-artifacts/5-3-what-if-sensitivity-simulator.md`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-25: Implementação completa da Story 5.3 — Simulador de Sensibilidade ("E se?") no backend e frontend com testes automatizados.
- 2026-10-25: Code review adversarial concluído (Blind Hunter, Edge Case Hunter, Acceptance Auditor). Aplicados patches de precisão inicial no `_resetValues()` do `WhatIfSimulatorCard` e guardrails `.isFinite` em `TimeToGoalCalculator.generateEvolutionCurve` e `GoalProjectionChart`. Todos os critérios de aceitação (AC 1–7), regras do `AGENTS.md` e suítes de testes (`mvn test`, `flutter analyze`, `flutter test`) validados com sucesso. Status atualizado para `done`.
