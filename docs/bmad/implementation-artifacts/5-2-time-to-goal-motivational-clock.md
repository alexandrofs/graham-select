---
baseline_commit: 063e84e1159850bfb84542eee53526e707cdcf26
---
# Story 5.2: Relógio Motivacional Time-to-Goal (Componente UI Principal)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

**As a** investidor,
**I want** visualizar um componente interativo de "Relógio Motivacional Time-to-Goal" com contagem regressiva em anos e meses, baseado no meu patrimônio atual, taxa de rentabilidade (yield) e aporte mensal,
**So that** eu tenha clareza de quanto tempo falta para atingir minha independência financeira e mantenha a motivação e foco no longo prazo.

## Acceptance Criteria

1. **Given** que o usuário autenticado possui uma meta financeira ativa e dados de portfólio consolidados
   **When** ele acessa o dashboard central (`DashboardPage`) ou a tela de metas (`GoalsPage`)
   **Then** o sistema calcula a projeção matemática de tempo restante usando a fórmula de juros compostos com aportes mensais recorrentes:
   - $FV$: Valor Alvo (em R$) da meta ativa (`targetValue`). Se o tipo for `MONTHLY_INCOME_TARGET`, $FV = \frac{\text{targetMonthlyIncome} \times 12}{\text{annualYieldRate}}$ (caso `annualYieldRate > 0`; se yield for zero/indisponível, usa yield de referência padrão de 6.0% a.a. ou $0.06$).
   - $PV$: Patrimônio Total atual (`totalEquity` do `PortfolioSummary`, padrão R$ 0,00 se sem custódia).
   - $PMT$: Aporte Mensal pretendido (`monthlyContribution` da `FinancialGoal`).
   - $r$: Taxa de juros mensal baseada no yield atual ($r = \frac{\text{grossYieldPercentage} / 100}{12}$).
   - $n$: Número total de meses calculado por $n = \frac{\ln\left(\frac{FV \cdot r + PMT}{PV \cdot r + PMT}\right)}{\ln(1 + r)}$ quando $r > 0$, ou $n = \frac{FV - PV}{PMT}$ quando $r \le 0$ e $PMT > 0$.
   **And** decompõe $n$ em **Anos** ($n \div 12$) e **Meses** ($n \pmod{12}$).

2. **Given** que o cálculo da projeção Time-to-Goal foi realizado
   **When** o componente `MotivationalClockCard` é renderizado na interface
   **Then** exibe em destaque:
   - Título e ícone de relógio/cronômetro (`Icons.timer_outlined` / `Icons.hourglass_bottom_rounded`)
   - Contador visual em destaque: **[X anos e Y meses]** (usando tipografia `JetBrains Mono` ou `GoogleFonts.jetBrainsMono`)
   - Data estimada de conclusão (ex: *"Previsão: Outubro de 2038"*)
   - Barra de progresso visual de patrimônio com porcentagem atingida ($\min(100\%, \frac{PV}{FV} \times 100)$), utilizando gradiente das cores `kEmerald` (`#10B981`) e `kAmberGold` (`#F59E0B`)
   - Valores contextuais: Patrimônio Atual ($PV$), Valor Alvo ($FV$), Aporte Mensal ($PMT$) e Taxa de Yield anual considerada
   **And** inclui uma animação suave de "tick" (pulso sutil de iluminação/glow dourado a cada 1 segundo ou ciclo suave de animação) reforçando a progressão temporal contínua da jornada de investimento.

3. **Given** que o usuário já atingiu ou superou sua meta ($PV \ge FV$)
   **When** o `MotivationalClockCard` é renderizado
   **Then** o relógio exibe o status de conquista:
   - Contador: **"0 anos e 0 meses"**
   - Frase em destaque em `kEmerald` (`#10B981`): **"🎉 Parabéns! Você atingiu sua independência financeira!"**
   - Barra de progresso preenchida em 100%
   - O indicador de "tick" permanece em modo comemorativo estável.

4. **Given** que o usuário configurou aporte mensal R$ 0,00 e o yield da carteira é 0% (ou a meta é inalcançável sem aportes adicionais e sem rendimentos, $PV < FV$)
   **When** o cálculo é executado
   **Then** o sistema trata o caso limite sem quebras (evita divisão por zero ou $\ln$ de número negativo)
   **And** exibe no `MotivationalClockCard` o aviso: *"Defina um aporte mensal ou adicione ativos com rendimento para calcular a projeção."*

5. **Given** que o usuário ainda NÃO configurou uma meta financeira (`currentGoal == null`)
   **When** ele acessa o dashboard central (`DashboardPage`)
   **Then** o `MotivationalClockCard` é renderizado em estado *Empty State / Call-to-Action*:
   - Mensagem: *"Defina sua Meta Financeira para desbloquear o Relógio Time-to-Goal"*
   - Botão de ação: *"Configurar Minha Meta"* que navega diretamente para a rota `/metas`
   - O dashboard não quebra e mantém o layout harmonioso.

6. **Given** que o backend recebe `GET /api/v1/financial-goals/projection`
   **When** o usuário está autenticado com token JWT válido
   **Then** o backend busca a meta ativa do usuário (`FinancialGoal`) e os dados de patrimônio/yield consolidados (`PortfolioSummary`), calcula a projeção e retorna `200 OK` com o DTO `GoalProjectionDto`:
   ```json
   {
     "data": {
       "targetValue": 1000000.00,
       "currentEquity": 150000.00,
       "monthlyContribution": 2000.00,
       "annualYieldPercentage": 10.50,
       "totalMonths": 142,
       "years": 11,
       "months": 10,
       "targetDate": "2038-04-16T15:00:00Z",
       "progressPercentage": 15.00,
       "isAchieved": false,
       "isUnreachable": false
     }
   }
   ```
   **And** se o usuário não possuir meta cadastrada, retorna `200 OK` com `{ "data": null }`
   **And** registra log em nível `INFO`: `"Received request to get time-to-goal projection for user {userId}"` na entrada do controller e `"Calculating time-to-goal projection for user {userId}"` no use case
   **And** todas as operações temporais no backend utilizam `ZoneOffset.UTC`.

7. **Given** que a feature está implementada
   **When** o agente executa a suíte de validação
   **Then** `mvn clean test -pl common` e `mvn clean test -pl api` executam com 100% de sucesso
   **And** `flutter analyze` executa com zero warnings e zero errors (respeitando todas as regras de linter do `analysis_options.yaml`)
   **And** os testes de unidade e widgets em `frontend/test/features/goals/` cobrem os cálculos matemáticos do `TimeToGoalCalculator`, o estado do `GoalsProvider` e a renderização do `MotivationalClockCard` (incluindo estados normal, meta atingida, inalcançável e empty state).

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domain + Application)

- [x] **Task 1: Criar modelo de domínio `GoalProjection` e serviço `TimeToGoalCalculator`** (AC: 1, 3, 4)
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalProjection.java`:
    ```java
    @Builder
    @Getter
    public class GoalProjection {
        private BigDecimal targetValue;
        private BigDecimal currentEquity;
        private BigDecimal monthlyContribution;
        private BigDecimal annualYieldPercentage;
        private int totalMonths;
        private int years;
        private int months;
        private OffsetDateTime targetDate;
        private BigDecimal progressPercentage;
        private boolean isAchieved;
        private boolean isUnreachable;
    }
    ```
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`:
    - Implementar método puro: `public GoalProjection calculate(GoalType goalType, BigDecimal rawTargetValue, BigDecimal currentEquity, BigDecimal monthlyContribution, BigDecimal annualYieldPercentage, OffsetDateTime referenceDate)` e sobrecarga `public GoalProjection calculate(BigDecimal targetValue, BigDecimal currentEquity, BigDecimal monthlyContribution, BigDecimal annualYieldPercentage, OffsetDateTime referenceDate)` (assumindo `GoalType.PATRIMONY_TARGET`).
    - Resolver $FV$ (`effectiveTargetValue`): se `goalType == GoalType.MONTHLY_INCOME_TARGET`, calcular $FV = \frac{\text{rawTargetValue} \times 12}{\text{annualYieldRate}}$ (usando `annualYieldPercentage / 100` se `> 0`, ou `0.06` como taxa de referência padrão caso yield seja `<= 0` ou nulo), com `RoundingMode.HALF_UP` e 2 casas decimais.
    - Tratar casos:
      1. $PV \ge FV$: `isAchieved = true`, `isUnreachable = false`, `totalMonths = 0`, `years = 0`, `months = 0`, `progressPercentage = 100.00`, `targetDate = referenceDate`.
      2. $r > 0$ (onde $r = \frac{\text{annualYield}}{12 \times 100}$) e $PV \cdot r + PMT > 0$: calcular $n = \frac{\ln\left(\frac{FV \cdot r + PMT}{PV \cdot r + PMT}\right)}{\ln(1 + r)}$, arredondando para cima (`Math.ceil`).
      3. $r \le 0$ e $PMT > 0$: calcular $n = \frac{FV - PV}{PMT}$ (`Math.ceil`).
      4. $r \le 0$ e $PMT \le 0$ com $PV < FV$ (ou $PV \cdot r + PMT \le 0$): `isUnreachable = true`, `isAchieved = false`, `totalMonths = -1`, `years = -1`, `months = -1`, `targetDate = null`.
    - Calcular `targetDate = referenceDate.plusMonths(totalMonths)` quando alcançável.
    - Calcular `progressPercentage = (PV / FV) * 100` (limitado entre `0.00` e `100.00`, escala de 2 casas decimais com `RoundingMode.HALF_UP`).

- [x] **Task 2: Criar DTO `GoalProjectionDto`** (AC: 6)
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalProjectionDto.java`:
    - Record Java:
      ```java
      @JsonInclude(JsonInclude.Include.NON_NULL)
      public record GoalProjectionDto(
          BigDecimal targetValue,
          BigDecimal currentEquity,
          BigDecimal monthlyContribution,
          BigDecimal annualYieldPercentage,
          int totalMonths,
          int years,
          int months,
          OffsetDateTime targetDate,
          BigDecimal progressPercentage,
          boolean isAchieved,
          boolean isUnreachable
      ) {}
      ```

- [x] **Task 3: Criar Use Case `CalculateGoalProjectionUseCase`** (AC: 1, 6)
  - [x] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/CalculateGoalProjectionUseCase.java`:
    - Injetar `FinancialGoalRepository`, `GetPortfolioSummaryUseCase` (já existente em `afsdigital.grahamselect.common.portfolio.application.usecase.GetPortfolioSummaryUseCase`) e `TimeToGoalCalculator`.
    - Métodos:
      - `execute(String userId): Optional<GoalProjectionDto>` — busca `PortfolioSummaryDTO` via `getPortfolioSummaryUseCase.execute(userId)` (tratando `totalEquity` e `grossYieldPercentage` nulos como `BigDecimal.ZERO`) e delega para `execute(userId, totalEquity, grossYieldPercentage)`.
      - `execute(String userId, BigDecimal currentEquity, BigDecimal grossYieldPercentage): Optional<GoalProjectionDto>` — registra `log.info("Calculating time-to-goal projection for user {}", userId)`, busca meta ativa com `financialGoalRepository.findByUserId(userId)`.
    - Se não houver meta: retornar `Optional.empty()`.
    - Se houver meta: chamar `timeToGoalCalculator.calculate(goal.getGoalType(), goal.getTargetValue(), currentEquity, goal.getMonthlyContribution(), grossYieldPercentage, OffsetDateTime.now(ZoneOffset.UTC))` e mapear para `GoalProjectionDto`.
    - Em caso de exceção inesperada, capturar, registrar `log.error("Error calculating time-to-goal projection for user {}", userId, ex)` (conforme regra do `AGENTS.md`) e relançar.

---

### Backend — Módulo `api` (Web & Integration)

- [x] **Task 4: Atualizar `FinancialGoalsController` e `GoalsServiceConfiguration` com endpoint de projeção** (AC: 6)
  - [x] Em `GoalsServiceConfiguration.java`:
    - Registrar beans `TimeToGoalCalculator` e `CalculateGoalProjectionUseCase(financialGoalRepository, getPortfolioSummaryUseCase, timeToGoalCalculator)`.
  - [x] No `FinancialGoalsController.java`:
    - Injetar `CalculateGoalProjectionUseCase`.
    - Adicionar endpoint `GET /api/v1/financial-goals/projection`:
      ```java
      @GetMapping("/projection")
      public ResponseEntity<?> getProjection(@AuthenticationPrincipal Jwt jwt) {
          String userId = jwt.getSubject();
          log.info("Received request to get time-to-goal projection for user {}", userId);
          Optional<GoalProjectionDto> result = calculateGoalProjectionUseCase.execute(userId);
          return ResponseEntity.ok(Collections.singletonMap("data", result.orElse(null)));
      }
      ```
    - **ATENÇÃO CRÍTICA:** Usar `Collections.singletonMap("data", result.orElse(null))` e **NUNCA** `Map.of("data", null)`, pois `Map.of` lança `NullPointerException` quando o valor é `null` (caso de usuário sem meta cadastrada).

---

### Frontend Flutter — Feature `goals` & `dashboard`

- [x] **Task 5: Criar entidade de domínio `GoalProjection` e serviço `TimeToGoalCalculator` (Dart)** (AC: 1, 3, 4)
  - [x] Criar `frontend/lib/src/features/goals/domain/entities/goal_projection.dart`:
    ```dart
    class GoalProjection {
      final double targetValue;
      final double currentEquity;
      final double monthlyContribution;
      final double annualYieldPercentage;
      final int totalMonths;
      final int years;
      final int months;
      final DateTime? targetDate;
      final double progressPercentage;
      final bool isAchieved;
      final bool isUnreachable;

      const GoalProjection({
        required this.targetValue,
        required this.currentEquity,
        required this.monthlyContribution,
        required this.annualYieldPercentage,
        required this.totalMonths,
        required this.years,
        required this.months,
        this.targetDate,
        required this.progressPercentage,
        required this.isAchieved,
        required this.isUnreachable,
      });
    }
    ```
  - [x] Criar `frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart`:
    - Implementar classe estática/serviço `TimeToGoalCalculator.calculate(...)`:
      - Suporte à conversão de `MONTHLY_INCOME_TARGET` e `PATRIMONY_TARGET`.
      - Fórmulas de juros compostos com precisão numérica.
      - Retornar instância de `GoalProjection`.

- [x] **Task 6: Criar `GoalProjectionModel` e atualizar DataSource/Repository** (AC: 6)
  - [x] Criar `frontend/lib/src/features/goals/data/models/goal_projection_model.dart` com `fromJson` e `toEntity`.
  - [x] Em `GoalsRemoteDatasource`: adicionar `Future<GoalProjectionModel?> fetchProjection()`.
  - [x] Em `GoalsRepository` e `GoalsRepositoryImpl`: adicionar `Future<GoalProjection?> fetchProjection()`.

- [x] **Task 7: Atualizar `GoalsProvider` com projeção e recálculo reativo** (AC: 1, 5)
  - [x] Adicionar no `GoalsProvider`:
    - `GoalProjection? _projection;`
    - `GoalProjection? get projection => _projection;`
    - Método `Future<void> loadProjection({double? currentEquity, double? grossYieldPercentage})` que busca da API via `repository.fetchProjection()` e, caso `currentGoal != null` e o backend retorne nulo (ex: em mocks de teste ou modo offline/reativo), recalcula localmente com `TimeToGoalCalculator.calculate(...)`.
    - Método síncrono auxiliar `void recalculateLocalProjection({required double currentEquity, required double grossYieldPercentage})` para recálculo instantâneo quando o `PortfolioProvider` atualizar os dados.
    - No método `loadGoal()` e `saveGoal(...)`: após carregar/salvar `_currentGoal`, atualizar `_projection`.

- [x] **Task 8: Criar componente `MotivationalClockCard`** (AC: 2, 3, 4, 5)
  - [x] Criar `frontend/lib/src/features/goals/presentation/widgets/motivational_clock_card.dart`:
    - `StatefulWidget` com `SingleTickerProviderStateMixin` para gerenciar a animação de pulso/tick (`static bool isTesting = false;` + detecção automática de `TestWidgetsFlutterBinding` via `WidgetsBinding.instance.runtimeType.toString().contains('Test')` para **NÃO** chamar `_pulseController.repeat()` durante `flutter test`, evitando timeout em `pumpAndSettle()`).
    - Props: `final FinancialGoal? goal`, `final GoalProjection? projection`, `final VoidCallback? onConfigureGoal`.
    - **Header:**
      - Ícone de relógio com pulso suave (`Icons.timer_outlined` / `Icons.hourglass_bottom_rounded` pulsando glow dourado a cada 1s).
      - Título *"Tempo Restante para Independência"* em `headlineSmall` / `titleLarge` com tom `kNavyBlue` (`#1B2A4A`) ou branco sobre card escuro.
      - Badge de status (ex: "Em Progresso", "Meta Atingida 🎉", "Ação Necessária", "Sem Meta").
    - **Display Central do Cronômetro:**
      - Container estilizado com bordas arredondadas (16dp), fundo escuro elegante (`#111827`) ou tom navy suave com borda sutil em `kAmberGold` (`#F59E0B`).
      - Tipografia `GoogleFonts.jetBrainsMono`:
        - Se normal: **"[X anos e Y meses]"** (ex: `"11 anos e 10 meses"`, destaque em tamanho 28–34 sp, cor `kAmberGold` ou `Colors.white`).
        - Se $PV \ge FV$ (`isAchieved == true`): Contador **"0 anos e 0 meses"** e frase em destaque em `kEmerald` (`#10B981`): **"🎉 Parabéns! Você atingiu sua independência financeira!"**.
        - Se inalcançável (`isUnreachable == true`): Aviso claro: *"Defina um aporte mensal ou adicione ativos com rendimento para calcular a projeção."*
    - **Barra de Progresso Visual:**
      - Barra com altura de 10dp, cantos arredondados, cor base suave e preenchimento com gradiente `kEmerald` (`#10B981`) e `kAmberGold` (`#F59E0B`).
      - Texto de porcentagem: **"XX,X% alcançado"** e valores formatados em uma única string: *"R$ PV de R$ FV"* (para não colidir com seletores `find.text('R$ ...')` existentes no `goals_page_test.dart`).
    - **Rodapé de Contexto:**
      - Linha com data prevista: *"Previsão: [Mês] de [Ano]"* (usando `DateFormat("MMMM 'de' yyyy", 'pt_BR')` com inicial maiúscula).
      - Chips/Badges informativos: *"Aporte: R$ PMT/mês"* e *"Yield: Y% a.a."*.
    - **Estado Vazio (Sem Meta — `goal == null`):**
      - Renderizar card convidativo com mensagem: *"Defina sua Meta Financeira para desbloquear o Relógio Time-to-Goal"* e botão de ação *"Configurar Minha Meta"* que aciona `onConfigureGoal` (ou navega para `/metas` via `GoRouter`).
    - **Regras de Estilo:**
      - Utilizar `withAlpha((alpha * 255).round())` ou `withValues(alpha: ...)` (**NÃO** usar `withOpacity()`).
      - Tipografia e tokens consistentes com o Design System.

- [x] **Task 9: Integrar `MotivationalClockCard` no `DashboardPage` e na `GoalsPage`** (AC: 1, 2, 5)
  - [x] Em `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`:
    - Usar leitura defensiva de `GoalsProvider?` (`Provider.of<GoalsProvider?>(context)` / `Provider.of<GoalsProvider?>(context, listen: false)`) para que testes legados que não injetem `GoalsProvider` na árvore não quebrem com `ProviderNotFoundException`, além de atualizar `dashboard_page_test.dart`.
    - No `initState` (dentro do `addPostFrameCallback`): se `goalsProvider != null`, chamar `goalsProvider.loadGoal()` e `goalsProvider.loadProjection()`.
    - Inserir o widget `MotivationalClockCard` de forma proeminente no Dashboard (logo abaixo dos KPI Cards).
    - Caso `goalsProvider.projection == null` mas `goalsProvider.currentGoal != null`, calcular projeção em tempo real via `TimeToGoalCalculator.calculate(...)` com os dados do `PortfolioProvider` (`summary?.totalEquity ?? 0.0` e `summary?.grossYieldPercentage ?? 0.0`).
  - [x] Em `frontend/lib/src/features/goals/presentation/pages/goals_page.dart`:
    - Quando `goalsProvider.currentGoal != null`, renderizar o `MotivationalClockCard` logo acima do `GoalSummaryCard`, calculando/atualizando a projeção com `currentPatrimony` e `grossYieldPercentage` do `PortfolioProvider`.

---

### Testes & Validação

- [x] **Task 10: Testes Unitários de Backend — `TimeToGoalCalculatorTest` & `CalculateGoalProjectionUseCaseTest`** (AC: 1, 3, 4)
  - [x] Criar `backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java`:
    - Testar cálculo com rentabilidade positiva e aportes ($PV < FV, r > 0, PMT > 0$).
    - Testar conversão de meta do tipo `MONTHLY_INCOME_TARGET` (com yield positivo e com yield zero usando 6% a.a. de referência).
    - Testar caso $PV \ge FV \implies$ 0 anos, 0 meses, `isAchieved = true`, `progressPercentage = 100.00`.
    - Testar caso sem rendimento ($r = 0, PMT > 0$) $\implies n = \lceil (FV - PV) / PMT \rceil$.
    - Testar caso inalcançável ($r = 0, PMT = 0, PV < FV$) $\implies isUnreachable = true, totalMonths = -1, targetDate = null$.
  - [x] Criar `backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/CalculateGoalProjectionUseCaseTest.java`:
    - Testar retorno de `Optional.empty()` quando usuário não possui meta.
    - Testar cálculo integrado com `GetPortfolioSummaryUseCase` e `FinancialGoalRepository` usando `ZoneOffset.UTC`.

- [x] **Task 11: Testes de Controller Backend — `FinancialGoalsControllerTest`** (AC: 6)
  - [x] Atualizar `backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java`:
    - Adicionar `@MockBean private CalculateGoalProjectionUseCase calculateGoalProjectionUseCase;`.
    - Testar `GET /api/v1/financial-goals/projection` com meta existente $\implies$ retorna `200 OK` com payload preenchido (`$.data.years`, `$.data.months`, `$.data.isAchieved`, etc.).
    - Testar `GET /api/v1/financial-goals/projection` sem meta $\implies$ retorna `200 OK` com `$.data` nulo (`{"data": null}` sem `NullPointerException`).

- [x] **Task 12: Testes Unitários e de Widget no Flutter** (AC: 1, 2, 3, 4, 5)
  - [x] Criar `frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart`:
    - Testar fórmulas de patrimônio alvo (`PATRIMONY_TARGET`) e renda mensal alvo (`MONTHLY_INCOME_TARGET`).
    - Testar decomposição em anos e meses, meta atingida ($PV \ge FV$) e meta inalcançável ($r \le 0, PMT \le 0$).
  - [x] Criar `frontend/test/features/goals/presentation/widgets/motivational_clock_card_test.dart`:
    - Testar renderização de anos e meses formatados (`"X anos e Y meses"`), data prevista e barra de progresso.
    - Testar estado de meta atingida (100% de progresso, `"0 anos e 0 meses"` e mensagem `"🎉 Parabéns! Você atingiu sua independência financeira!"`).
    - Testar empty state com mensagem e botão `"Configurar Minha Meta"` quando `goal == null`.
    - Testar estado inalcançável com mensagem de orientação quando `isUnreachable == true`.
  - [x] Atualizar `frontend/test/features/goals/presentation/pages/goals_page_test.dart` (implementar `fetchProjection()` no `ManualMockGoalsRepository` e validar presença do `MotivationalClockCard`) e `frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart` para garantir compatibilidade total e zero regressões.

- [x] **Task 13: Execução e Validação Final** (AC: 7)
  - [x] Executar `mvn clean test -pl common`
  - [x] Executar `mvn clean test -pl api`
  - [x] Executar `flutter analyze`
  - [x] Executar `flutter test`

### Review Findings

- [x] [Review][Patch] Evitar log ERROR duplicado na delegação de `CalculateGoalProjectionUseCase.execute(String userId)` [`CalculateGoalProjectionUseCase.java:26-41`]
- [x] [Review][Patch] Garantir clamp de valores negativos e limite seguro de meses (`MAX_SUPPORTED_MONTHS = 12000`) em `TimeToGoalCalculator` (Java e Dart) para evitar `Infinity`/`NaN`/`DateTimeException` [`TimeToGoalCalculator.java:41-125`, `time_to_goal_calculator.dart:15-105`]
- [x] [Review][Patch] Remover chamada concorrente redundante de `loadProjection()` no `initState` de `DashboardPage` e sincronizar `recalculateLocalProjection` com `PortfolioProvider` em `DashboardPage` e `GoalsPage` [`dashboard_page.dart:26-42`, `goals_page.dart:76-90`]
- [x] [Review][Patch] Garantir que `MotivationalClockCard` exiba Empty State apenas quando `goal == null` e calcule fallback quando `projection == null` [`motivational_clock_card.dart:91-105`]
- [x] [Review][Patch] Tornar `GoalProjectionModel.fromJson` e `GoalsRemoteDatasource.fetchProjection` resilientes a tipos numéricos (`num?.toInt()`) e datas malformadas (`DateTime.tryParse`) [`goal_projection_model.dart:30-44`, `goals_remote_datasource.dart:40-50`]
- [x] [Review][Patch] Limpar flag estática `MotivationalClockCard.isTesting = false` em `tearDownAll` nos testes de widget [`motivational_clock_card_test.dart:14-16`]

---

## Dev Notes

### 1. Racional Matemático Detalhado (Juros Compostos com Aportes Mensais)

Para uma meta de patrimônio $FV$, com patrimônio inicial $PV$, aporte mensal fixo no final de cada período $PMT$, e taxa mensal $r = \frac{\text{yield anual \%}}{100 \times 12}$:

A evolução patrimonial após $n$ meses é dada por:
$$FV = PV \cdot (1 + r)^n + PMT \cdot \left[ \frac{(1 + r)^n - 1}{r} \right]$$

Isolando o tempo $n$ (em meses):
$$FV = PV \cdot (1 + r)^n + \frac{PMT}{r} \cdot (1 + r)^n - \frac{PMT}{r}$$
$$FV + \frac{PMT}{r} = (1 + r)^n \cdot \left( PV + \frac{PMT}{r} \right)$$
$$(1 + r)^n = \frac{FV + \frac{PMT}{r}}{PV + \frac{PMT}{r}} = \frac{FV \cdot r + PMT}{PV \cdot r + PMT}$$
$$n = \frac{\ln\left(\frac{FV \cdot r + PMT}{PV \cdot r + PMT}\right)}{\ln(1 + r)}$$

**Casos Especiais:**
1. **$PV \ge FV$ (Meta Atingida):**
   - $n = 0$ meses $\implies 0$ anos e $0$ meses. `isAchieved = true`, `isUnreachable = false`. Progresso = $100.00\%$.
2. **$r > 0$ e $PV \cdot r + PMT > 0$:**
   - $n = \lceil \text{fórmula acima} \rceil$.
   - $\text{Anos} = \lfloor n / 12 \rfloor$, $\text{Meses} = n \pmod{12}$.
3. **$r \le 0$ e $PMT > 0$ (Sem rendimento, apenas aportes lineares):**
   - $n = \lceil \frac{FV - PV}{PMT} \rceil$.
4. **$r \le 0$, $PMT \le 0$ e $PV < FV$ (Inalcançável):**
   - $n = -1$, $\text{Anos} = -1$, $\text{Meses} = -1$, `targetDate = null`, `isUnreachable = true`.
5. **Meta de Renda Passiva Mensal (`MONTHLY_INCOME_TARGET` com alvo mensal $I_{\text{target}}$):**
   - Se $\text{annualYieldPercentage} > 0$, o patrimônio necessário para gerar renda perpétua mensal de $I_{\text{target}}$ é $FV = \frac{I_{\text{target}} \times 12}{\text{annualYieldPercentage} / 100}$.
   - Se $\text{annualYieldPercentage} \le 0$, adota-se uma taxa de referência padrão de mercado de $6.0\%$ ao ano ($\text{annualYieldRate} = 0.06$) para estimar o patrimônio alvo correspondente: $FV = \frac{I_{\text{target}} \times 12}{0.06}$.

### 2. Análise de Arquivos Existentes a Modificar (UPDATE Files Analysis)

- **`backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java`**
  - *Estado atual:* Possui endpoints `POST /api/v1/financial-goals`, `PUT /api/v1/financial-goals/{id}` e `GET /api/v1/financial-goals` (que retorna lista de metas em `Map.of("data", List)`). Já importa `java.util.Collections`.
  - *O que muda:* Injeção de `CalculateGoalProjectionUseCase` e adição do endpoint `@GetMapping("/projection")` retornando `ResponseEntity.ok(Collections.singletonMap("data", result.orElse(null)))`.
  - *O que preservar:* Assinaturas e contratos dos endpoints existentes (`POST`, `PUT`, `GET`). **NUNCA** usar `Map.of("data", null)` pois `Map.of` lança `NullPointerException` com valores nulos.
- **`backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java`**
  - *Estado atual:* Registra os beans `CreateFinancialGoalUseCase`, `UpdateFinancialGoalUseCase` e `GetFinancialGoalUseCase`.
  - *O que muda:* Adição dos beans `TimeToGoalCalculator` e `CalculateGoalProjectionUseCase` (injetando `FinancialGoalRepository`, `GetPortfolioSummaryUseCase` já provido por `PortfolioSummaryConfiguration`, e `TimeToGoalCalculator`).
  - *O que preservar:* Beans existentes intactos.
- **`backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java`**
  - *Estado atual:* Teste `@AutoConfigureMockMvc` que estende `BaseRepositoryIT` e declara `@MockBean` para `CreateFinancialGoalUseCase`, `UpdateFinancialGoalUseCase` e `GetFinancialGoalUseCase`.
  - *O que muda:* Como o `FinancialGoalsController` passará a exigir `CalculateGoalProjectionUseCase` no construtor, é **obrigatório** adicionar `@MockBean private CalculateGoalProjectionUseCase calculateGoalProjectionUseCase;` nesta classe de teste, além dos novos métodos de teste para `/projection`.
- **`frontend/lib/src/features/goals/domain/repositories/goals_repository.dart`, `goals_repository_impl.dart`, `goals_remote_datasource.dart`, `goals_provider.dart`**
  - *Estado atual:* Suportam `fetchGoal()`, `createGoal()` e `updateGoal()`.
  - *O que muda:* Adição de `fetchProjection()` na cadeia DataSource → Repository → Provider, além do cálculo local com `TimeToGoalCalculator` em `GoalsProvider`.
  - *O que preservar:* Em `frontend/test/features/goals/presentation/pages/goals_page_test.dart`, a classe `ManualMockGoalsRepository implements GoalsRepository` **DEVE** ser atualizada para implementar `@override Future<GoalProjection?> fetchProjection()`, caso contrário o teste quebrará em tempo de compilação.
- **`frontend/lib/src/features/goals/presentation/pages/goals_page.dart`**
  - *Estado atual:* Exibe o formulário de metas e, quando `goalsProvider.currentGoal != null`, exibe o `GoalSummaryCard`. Possui `GoalsPage.isTesting` para desativar o `repeat()` do `AnimationController` em testes.
  - *O que muda:* Exibição do `MotivationalClockCard` acima do `GoalSummaryCard` quando houver meta ativa, e recálculo da projeção ao carregar/salvar meta.
  - *O que preservar:* Os testes existentes em `goals_page_test.dart` verificam `expect(find.text('R\$ 200.000,00'), findsNWidgets(2))` e `expect(find.text('R\$ 300.000,00'), findsNWidgets(2))`. Portanto, no `MotivationalClockCard`, os textos de patrimônio e alvo devem ser combinados (ex: `'R\$ 150.000,00 de R\$ 200.000,00'`) e não renderizados como um `Text('R\$ 200.000,00')` isolado, preservando a contagem exata dos widgets nos testes existentes!
- **`frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`**
  - *Estado atual:* Carrega `ProfileProvider` e `PortfolioProvider` no `initState`, e renderiza KPI Cards, `AssetAllocationDonutChart` e `FinancialDataTable`.
  - *O que muda:* Carregamento de `GoalsProvider` (`loadGoal()` e `loadProjection()`) e exibição do `MotivationalClockCard` abaixo da seção de KPIs.
  - *O que preservar:* Em `dashboard_page_test.dart`, o `MultiProvider` original injeta apenas `PortfolioProvider` e `ProfileProvider`. Usar `Provider.of<GoalsProvider?>(context)` de forma segura no `DashboardPage` e atualizar `dashboard_page_test.dart` evita `ProviderNotFoundException`.

### 3. Inteligência da História Anterior (Story 5.1) e Histórico Git

- **Aprendizados da Story 5.1 (`5-1-wealth-income-goal-setup.md`):**
  - No `PortfolioSummary` (tanto no backend `PortfolioSummaryDTO` quanto no frontend `PortfolioSummary`), o campo de patrimônio chama-se **`totalEquity`** (e a rentabilidade bruta chama-se **`grossYieldPercentage`**) — nunca usar `totalValue`.
  - Nos testes de widget Flutter (`goals_page_test.dart`), `tester.pumpAndSettle()` é utilizado extensivamente. Qualquer `AnimationController.repeat()` ativo durante testes de widget trava o `pumpAndSettle()` por timeout. Por isso, em `MotivationalClockCard`, verifique `final isTest = MotivationalClockCard.isTesting || GoalsPage.isTesting || WidgetsBinding.instance.runtimeType.toString().contains('Test');` antes de iniciar `_pulseController.repeat(reverse: true)`.
  - No backend, logs de nível `INFO` na entrada dos controllers e use cases e logs `ERROR` nas capturas/lançamentos de exceções são estritamente auditados no code review (`AGENTS.md`).
  - Todas as datas no backend e nos testes unitários devem usar explicitamente `OffsetDateTime.now(ZoneOffset.UTC)`.

### 4. Design e Micro-Animação do Relógio Motivacional (Flutter)

O componente `MotivationalClockCard` deve seguir rigorosamente os padrões visuais e de design tokens do projeto:

```dart
// Cores mandatórias (UX Design Specification):
const Color kNavyBlue = Color(0xFF1B2A4A);   // Backgrounds, títulos
const Color kEmerald = Color(0xFF10B981);    // Sucesso, progresso, meta atingida
const Color kAmberGold = Color(0xFFF59E0B);  // Destaque, tempo, relógio, motivação
const Color kCardDarkBg = Color(0xFF111827); // Fundo escuro premium para o relógio

// Animação de "Tick" Suave:
// Utilizar AnimationController com duração de 1000ms repetindo em loop (repeat)
// apenas quando fora do ambiente de teste (!isTest) e quando a meta ainda estiver em progresso.
```

### 5. Regras Mandatórias de Linter e Arquitetura (AGENTS.md & Project Context)

- **Java 21 / Spring Boot:**
  - Todas as datas/horas com `OffsetDateTime.now(ZoneOffset.UTC)`.
  - Valores monetários exclusivamente em `BigDecimal`.
  - `log.info(...)` na entrada de todo endpoint REST.
  - `log.error(...)` na captura e lançamento de exceções.
  - Multi-tenancy: Extração estrita de `userId` via `jwt.getSubject()`.
- **Flutter / Dart:**
  - Null safety estrito.
  - **NÃO USAR** `withOpacity()`; usar `withAlpha((alpha * 255).round())` ou `withValues(alpha: ...)`.
  - Não utilizar `surfaceVariant` (deprecado), utilizar `surfaceContainerHighest`.
  - Verificar `if (!mounted) return;` antes de interações com `BuildContext` pós chamadas assíncronas.

### 6. Estrutura de Arquivos a Criar e Modificar

```
NOVOS ARQUIVOS:
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalProjection.java
backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalProjectionDto.java
backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/CalculateGoalProjectionUseCase.java
backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java
backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/CalculateGoalProjectionUseCaseTest.java

frontend/lib/src/features/goals/domain/entities/goal_projection.dart
frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart
frontend/lib/src/features/goals/data/models/goal_projection_model.dart
frontend/lib/src/features/goals/presentation/widgets/motivational_clock_card.dart
frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart
frontend/test/features/goals/presentation/widgets/motivational_clock_card_test.dart

ARQUIVOS A MODIFICAR:
backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java
backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java
backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java

frontend/lib/src/features/goals/data/datasources/goals_remote_datasource.dart
frontend/lib/src/features/goals/domain/repositories/goals_repository.dart
frontend/lib/src/features/goals/data/repositories/goals_repository_impl.dart
frontend/lib/src/features/goals/presentation/providers/goals_provider.dart
frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart
frontend/lib/src/features/goals/presentation/pages/goals_page.dart
frontend/test/features/goals/presentation/pages/goals_page_test.dart
frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart
```

---

## References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 5.2: Relógio Motivacional Time-to-Goal]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 5: Metas & Projeção Financeira]
- [Source: docs/bmad/planning-artifacts/prd.md#FR24]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Visual Design Foundation]
- [Source: docs/bmad/planning-artifacts/architecture.md#Clean Architecture — Padrão do Projeto]
- [Source: docs/bmad/project-context.md]
- [Source: docs/bmad/implementation-artifacts/5-1-wealth-income-goal-setup.md]
- [Source: frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart]
- [Source: frontend/lib/src/features/goals/presentation/providers/goals_provider.dart]
- [Source: backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java]

---

## Dev Agent Record

### Agent Model Used

Gemini 4 Argon (Medium)

### Debug Log References

- `mvn clean test -pl common`: 75 tests run, 0 failures, 0 errors.
- `mvn clean test -pl api`: All tests passed including `FinancialGoalsControllerTest`.
- `flutter analyze`: No issues found!
- `flutter test`: 145 tests passed, 0 failures.

### Completion Notes List

- Implementados `GoalProjection`, `TimeToGoalCalculator`, `GoalProjectionDto` e `CalculateGoalProjectionUseCase` no módulo `common` do backend, aplicando juros compostos com aportes recorrentes, conversão de metas `MONTHLY_INCOME_TARGET`, tratamento de casos limites (meta atingida e inalcançável), logs `INFO`/`ERROR` e uso estrito de `ZoneOffset.UTC`.
- Exposto o endpoint `GET /api/v1/financial-goals/projection` no `FinancialGoalsController` e configurados os beans em `GoalsServiceConfiguration`.
- Implementados no Flutter `GoalProjection`, `TimeToGoalCalculator`, `GoalProjectionModel`, `GoalsRemoteDatasource.fetchProjection`, `GoalsRepository.fetchProjection`, `GoalsProvider` (com suporte a recálculo reativo local) e o componente visual `MotivationalClockCard` integrado na `DashboardPage` e `GoalsPage`.
- Adicionados testes unitários e de widget completos para backend e frontend validando todos os critérios de aceite sem regressões.

### File List

- `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalProjection.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalProjectionDto.java`
- `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/CalculateGoalProjectionUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/CalculateGoalProjectionUseCaseTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/goals/infrastructure/spring/GoalsServiceConfiguration.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java`
- `frontend/lib/src/features/goals/domain/entities/goal_projection.dart`
- `frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart`
- `frontend/lib/src/features/goals/data/models/goal_projection_model.dart`
- `frontend/lib/src/features/goals/data/datasources/goals_remote_datasource.dart`
- `frontend/lib/src/features/goals/domain/repositories/goals_repository.dart`
- `frontend/lib/src/features/goals/data/repositories/goals_repository_impl.dart`
- `frontend/lib/src/features/goals/presentation/providers/goals_provider.dart`
- `frontend/lib/src/features/goals/presentation/widgets/motivational_clock_card.dart`
- `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`
- `frontend/lib/src/features/goals/presentation/pages/goals_page.dart`
- `frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart`
- `frontend/test/features/goals/presentation/providers/goals_provider_test.dart`
- `frontend/test/features/goals/presentation/widgets/motivational_clock_card_test.dart`
- `frontend/test/features/goals/presentation/pages/goals_page_test.dart`
- `frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart`
- `docs/bmad/implementation-artifacts/5-2-time-to-goal-motivational-clock.md`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-10-25: Implementação completa da Story 5.2 (Relógio Motivacional Time-to-Goal no backend e frontend Flutter) com cobertura de testes unitários, integração e widgets.

