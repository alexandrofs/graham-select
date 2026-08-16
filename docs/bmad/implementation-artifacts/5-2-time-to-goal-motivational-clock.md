# Story 5.2: Relógio Motivacional Time-to-Goal (Componente UI Principal)

Status: ready-for-dev

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
   **And** registra log em nível `INFO`: `"Calculating time-to-goal projection for user {userId}"` na entrada do controller
   **And** todas as operações temporais no backend utilizam `ZoneOffset.UTC`.

7. **Given** que a feature está implementada
   **When** o agente executa a suíte de validação
   **Then** `mvn clean test -pl common` e `mvn clean test -pl api` executam com 100% de sucesso
   **And** `flutter analyze` executa com zero warnings e zero errors (respeitando todas as regras de linter do `analysis_options.yaml`)
   **And** os testes de unidade e widgets em `frontend/test/features/goals/` cobrem os cálculos matemáticos do `TimeToGoalCalculator`, o estado do `GoalsProvider` e a renderização do `MotivationalClockCard` (incluindo estados normal, meta atingida, inalcançável e empty state).

---

## Tasks / Subtasks

### Backend — Módulo `common` (Domain + Application)

- [ ] **Task 1: Criar modelo de domínio `GoalProjection` e serviço `TimeToGoalCalculator`** (AC: 1, 3, 4)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/entities/GoalProjection.java`:
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
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculator.java`:
    - Implementar método puro: `public GoalProjection calculate(BigDecimal targetValue, BigDecimal currentEquity, BigDecimal monthlyContribution, BigDecimal annualYieldPercentage, OffsetDateTime referenceDate)`
    - Tratar casos:
      1. $PV \ge FV$: `isAchieved = true`, `totalMonths = 0`, `years = 0`, `months = 0`, `progressPercentage = 100.00`, `targetDate = referenceDate`.
      2. $r > 0$ (onde $r = \frac{\text{annualYield}}{12 \times 100}$): calcular $n = \frac{\ln\left(\frac{FV \cdot r + PMT}{PV \cdot r + PMT}\right)}{\ln(1 + r)}$, arredondando para cima (`Math.ceil`).
      3. $r \le 0$ e $PMT > 0$: calcular $n = \frac{FV - PV}{PMT}$ (`Math.ceil`).
      4. $r \le 0$ e $PMT \le 0$ com $PV < FV$: `isUnreachable = true`, `totalMonths = -1`, `years = -1`, `months = -1`, `targetDate = null`.
    - Calcular `targetDate = referenceDate.plusMonths(totalMonths)`.
    - Calcular `progressPercentage = (PV / FV) * 100` (limitado entre 0 e 100, escala de 2 casas decimais).

- [ ] **Task 2: Criar DTO `GoalProjectionDto`** (AC: 6)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/dto/GoalProjectionDto.java`:
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

- [ ] **Task 3: Criar Use Case `CalculateGoalProjectionUseCase`** (AC: 1, 6)
  - [ ] Criar `backend/common/src/main/java/afsdigital/grahamselect/goals/application/usecase/CalculateGoalProjectionUseCase.java`:
    - Injetar `FinancialGoalRepository` e serviço/porta de consulta de portfólio (ou receber os dados consolidados de custódia/sumário).
    - Método `execute(String userId, BigDecimal currentEquity, BigDecimal grossYieldPercentage): Optional<GoalProjectionDto>`
    - Usar `@Slf4j` e `@RequiredArgsConstructor`.
    - Buscar meta ativa com `financialGoalRepository.findByUserId(userId)`.
    - Se não houver meta: retornar `Optional.empty()`.
    - Se houver meta: converter valores, se `goalType == MONTHLY_INCOME_TARGET`, calcular $FV$ correspondente; chamar `TimeToGoalCalculator.calculate(...)` com `OffsetDateTime.now(ZoneOffset.UTC)` e mapear para `GoalProjectionDto`.

---

### Backend — Módulo `api` (Web & Integration)

- [ ] **Task 4: Atualizar `FinancialGoalsController` com endpoint de projeção** (AC: 6)
  - [ ] No `FinancialGoalsController.java`:
    - Adicionar endpoint `GET /api/v1/financial-goals/projection`:
      ```java
      @GetMapping("/projection")
      public ResponseEntity<?> getProjection(@AuthenticationPrincipal Jwt jwt) {
          String userId = jwt.getSubject();
          log.info("Received request to get time-to-goal projection for user {}", userId);
          // Obter dados consolidados do portfólio do usuário (totalEquity e grossYieldPercentage)
          // Executar CalculateGoalProjectionUseCase
          // Retornar ResponseEntity.ok(Map.of("data", projectionDtoOrNull));
      }
      ```
    - Injetar use cases necessários e registrar bean no `GoalsServiceConfiguration.java`.

---

### Frontend Flutter — Feature `goals` & `dashboard`

- [ ] **Task 5: Criar entidade de domínio `GoalProjection` e serviço `TimeToGoalCalculator` (Dart)** (AC: 1, 3, 4)
  - [ ] Criar `frontend/lib/src/features/goals/domain/entities/goal_projection.dart`:
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
  - [ ] Criar `frontend/lib/src/features/goals/domain/services/time_to_goal_calculator.dart`:
    - Implementar classe estática/serviço `TimeToGoalCalculator.calculate(...)`:
      - Suporte à conversão de `MONTHLY_INCOME_TARGET` e `PATRIMONY_TARGET`.
      - Fórmulas de juros compostos com precisão numérica.
      - Retornar instância de `GoalProjection`.

- [ ] **Task 6: Criar `GoalProjectionModel` e atualizar DataSource/Repository** (AC: 6)
  - [ ] Criar `frontend/lib/src/features/goals/data/models/goal_projection_model.dart` com `fromJson` e `toEntity`.
  - [ ] Em `GoalsRemoteDatasource`: adicionar `Future<GoalProjectionModel?> fetchProjection()`.
  - [ ] Em `GoalsRepository` e `GoalsRepositoryImpl`: adicionar `Future<GoalProjection?> fetchProjection()`.

- [ ] **Task 7: Atualizar `GoalsProvider` com projeção e recálculo reativo** (AC: 1, 5)
  - [ ] Adicionar no `GoalsProvider`:
    - `GoalProjection? _projection;`
    - `GoalProjection? get projection => _projection;`
    - Método `Future<void> loadProjection({double? currentEquity, double? grossYieldPercentage})` que busca da API e atualiza `_projection` (ou recalcula localmente com `TimeToGoalCalculator` para resposta instantânea).
    - No método `saveGoal(...)`: após salvar, recalcular/atualizar `_projection`.

- [ ] **Task 8: Criar componente `MotivationalClockCard`** (AC: 2, 3, 4, 5)
  - [ ] Criar `frontend/lib/src/features/goals/presentation/widgets/motivational_clock_card.dart`:
    - `StatefulWidget` com `SingleTickerProviderStateMixin` para gerenciar a animação de pulso/tick.
    - Props: `final FinancialGoal? goal`, `final GoalProjection? projection`, `final VoidCallback? onConfigureGoal`.
    - **Header:**
      - Ícone de relógio com pulso suave (`Icons.hourglass_top_rounded` alternando ou pulsando glow dourado).
      - Título *"Tempo Restante para Independência"* em `headlineSmall` / `titleLarge` com tom `kNavyBlue` (`#1B2A4A`).
      - Badge de status (ex: "No Plano", "Meta Atingida 🎉", "Aguardando Meta").
    - **Display Central do Cronômetro:**
      - Container estilizado com bordas arredondadas (16dp), fundo escuro elegante ou tom navy suave com borda sutil em `kAmberGold` (`#F59E0B`).
      - Tipografia `GoogleFonts.jetBrainsMono`:
        - Se normal: **"X anos e Y meses"** (Destaque em tamanho 28–34 sp, cor `kAmberGold` ou `Colors.white`).
        - Se $PV \ge FV$: **"Meta Atingida! 🎉"** em cor `kEmerald` (`#10B981`).
        - Se inalcançável: Mensagem de orientação clara.
    - **Barra de Progresso Visual:**
      - `LinearProgressIndicator` ou custom painter com altura de 10dp, cantos arredondados, cor base cinza suave e preenchimento com `kEmerald` / `kAmberGold`.
      - Texto de porcentagem: **"XX.X% alcançado"** e valores formatados: *"R$ PV de R$ FV"*.
    - **Rodapé de Contexto:**
      - Linha com data prevista: *"Previsão de conclusão: Mês/Ano"*.
      - Chips/Badges informativos: *"Aporte: R$ PMT/mês"* e *"Yield: Y% a.a."*.
    - **Estado Vazio (Sem Meta):**
      - Renderizar card convidativo com mensagem inspiradora e botão primário *"Configurar Meta"* que navega para `/metas`.
    - **Regras de Estilo:**
      - Utilizar `withAlpha((alpha * 255).round())` (NÃO usar `withOpacity()`).
      - Tipografia e tokens consistentes com o Design System.

- [ ] **Task 9: Integrar `MotivationalClockCard` no `DashboardPage` e na `GoalsPage`** (AC: 1, 2, 5)
  - [ ] Em `frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart`:
    - No `initState`: chamar `context.read<GoalsProvider>().loadGoal()` e `context.read<GoalsProvider>().loadProjection()`.
    - Inserir o widget `MotivationalClockCard` de forma proeminente no topo do conteúdo (logo após as boas-vindas ou entre os KPI Cards e Gráficos).
    - Ouvir reativamente `GoalsProvider` e `PortfolioProvider`.
  - [ ] Em `frontend/lib/src/features/goals/presentation/pages/goals_page.dart`:
    - Atualizar a exibição da meta salva para renderizar o `MotivationalClockCard` logo acima ou abaixo do `GoalSummaryCard`, proporcionando feedback imediato ao salvar ou carregar a meta.

---

### Testes & Validação

- [ ] **Task 10: Testes Unitários de Backend — `TimeToGoalCalculatorTest` & `CalculateGoalProjectionUseCaseTest`** (AC: 1, 3, 4)
  - [ ] Criar `backend/common/src/test/java/afsdigital/grahamselect/goals/domain/services/TimeToGoalCalculatorTest.java`:
    - Testar cálculo com rentabilidade positiva e aportes ($PV < FV, r > 0, PMT > 0$).
    - Testar caso $PV \ge FV \implies$ 0 anos, 0 meses, `isAchieved = true`.
    - Testar caso sem rendimento ($r = 0, PMT > 0$) $\implies n = (FV - PV) / PMT$.
    - Testar caso inalcançável ($r = 0, PMT = 0, PV < FV$) $\implies isUnreachable = true$.
  - [ ] Criar `backend/common/src/test/java/afsdigital/grahamselect/goals/application/usecase/CalculateGoalProjectionUseCaseTest.java`.

- [ ] **Task 11: Testes de Controller Backend — `FinancialGoalsControllerProjectionTest`** (AC: 6)
  - [ ] Atualizar `backend/api/src/test/java/afsdigital/grahamselect/api/goals/web/FinancialGoalsControllerTest.java`:
    - Testar `GET /api/v1/financial-goals/projection` com meta existente $\implies$ retorna `200 OK` com payload preenchido.
    - Testar `GET /api/v1/financial-goals/projection` sem meta $\implies$ retorna `200 OK` com `data: null`.

- [ ] **Task 12: Testes Unitários e de Widget no Flutter** (AC: 1, 2, 3, 4, 5)
  - [ ] Criar `frontend/test/features/goals/domain/services/time_to_goal_calculator_test.dart`:
    - Testar fórmulas de patrimônio alvo e renda mensal alvo.
    - Testar decomposição em anos e meses.
  - [ ] Criar `frontend/test/features/goals/presentation/widgets/motivational_clock_card_test.dart`:
    - Testar renderização de anos e meses formatados.
    - Testar estado de meta atingida (100% de progresso e mensagem de parabéns).
    - Testar empty state com botão de ação quando `goal == null`.
    - Testar estado inalcançável/orientação quando sem aporte e sem yield.
  - [ ] Atualizar testes de `DashboardPage` e `GoalsPage` para garantir compatibilidade e ausência de regressões.

- [ ] **Task 13: Execução e Validação Final** (AC: 7)
  - [ ] Executar `mvn clean test -pl common`
  - [ ] Executar `mvn clean test -pl api`
  - [ ] Executar `flutter analyze`
  - [ ] Executar `flutter test`

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
   - $n = 0$ meses $\implies 0$ anos e $0$ meses. `isAchieved = true`. Progresso = $100\%$.
2. **$r > 0$ e $PV \cdot r + PMT > 0$:**
   - $n = \lceil \text{fórmula acima} \rceil$.
   - $\text{Anos} = \lfloor n / 12 \rfloor$, $\text{Meses} = n \pmod{12}$.
3. **$r \le 0$ e $PMT > 0$ (Sem rendimento, apenas aportes lineares):**
   - $n = \lceil \frac{FV - PV}{PMT} \rceil$.
4. **$r \le 0$, $PMT \le 0$ e $PV < FV$ (Inalcançável):**
   - $n = -1$, `isUnreachable = true`.
5. **Meta de Renda Passiva Mensal (`MONTHLY_INCOME_TARGET` com alvo $I_{\text{target}}$):**
   - Se $r > 0$, o patrimônio necessário para gerar renda perpétua de $I_{\text{target}}$ é $FV = \frac{I_{\text{target}}}{r}$.
   - Se $r \le 0$, adota-se uma taxa de referência padrão de mercado de $6.0\%$ ao ano ($r = 0.005$) para estimar o patrimônio alvo correspondente: $FV = \frac{I_{\text{target}} \times 12}{0.06}$.

### 2. Design e Micro-Animação do Relógio Motivacional (Flutter)

O componente `MotivationalClockCard` deve seguir rigorosamente os padrões visuais e de design tokens do projeto:

```dart
// Cores mandatórias (UX Design Specification):
const Color kNavyBlue = Color(0xFF1B2A4A);   // Backgrounds, títulos
const Color kEmerald = Color(0xFF10B981);    // Sucesso, progresso, meta atingida
const Color kAmberGold = Color(0xFFF59E0B);  // Destaque, tempo, relógio, motivação
const Color kCardDarkBg = Color(0xFF111827); // Fundo escuro premium para o relógio

// Animação de "Tick" Suave:
// Utilizar AnimationController com duração de 1000ms repetindo em loop (repeat)
// para animar a opacidade ou o glow de um ícone de pulso/segundos.
// ATENÇÃO: Desabilitar animações contínuas durante testes de widget (ou verificar tickerProvider).
```

### 3. Regras Mandatórias de Linter e Arquitetura (AGENTS.md & Project Context)

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

### 4. Estrutura de Arquivos a Criar e Modificar

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List

