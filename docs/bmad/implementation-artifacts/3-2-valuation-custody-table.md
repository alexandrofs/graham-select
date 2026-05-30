# Story 3.2: Tabela de Custódia Atualizada (Componente `FinancialDataTable`)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a investidor,
I want ver a lista de todos os ativos que possuo hoje,
so that eu acompanhe o preço médio e a valorização de cada papel individualmente.

## Acceptance Criteria

1. **Given** que o usuário acessa a aba "Portfólio"
   **When** o sistema consolida as operações manuais e de upload
   **Then** agrupa ativos por Ticker e exibe: Quantidade, Preço Médio, Cotação Atual (D-0 ou Cache), Valor de Mercado e % de Ganho/Perda

2. **Given** que a tabela de custódia está exibida
   **When** o usuário clica no cabeçalho de qualquer coluna
   **Then** a tabela ordena os dados pela coluna selecionada (ascendente/descendente alternando)

3. **Given** que as cotações foram buscadas com sucesso
   **When** a tabela é renderizada
   **Then** exibe o indicador "Atualizado em [HH:mm]" abaixo ou acima da tabela

4. **Given** que a API de cotações falhou ou o dado está em cache antigo
   **When** a tabela é renderizada
   **Then** exibe o indicador "Preço em Cache" em Amber Gold (#F59E0B) com ícone de aviso

5. **Given** que o usuário ainda não possui operações cadastradas
   **When** acessa a aba "Portfólio"
   **Then** exibe um empty state com ilustração, texto "Nenhum ativo encontrado" e CTA "Faça upload do seu arquivo B3 para começar" → [Fazer Upload]

6. **Given** que a tabela está carregando dados
   **When** a requisição está em andamento
   **Then** exibe Skeleton Screens imitando o layout da tabela (não spinner)

## Tasks / Subtasks

### Backend (Spring Boot)

- [x] Criar DTO `CustodyPositionDTO` no módulo `common` (AC: 1)
  - [x] Campos: `ticker`, `quantity` (BigDecimal), `averagePrice` (BigDecimal), `currentPrice` (BigDecimal), `marketValue` (BigDecimal), `gainLossPercentage` (BigDecimal), `priceSource` (enum: LIVE / CACHE), `priceUpdatedAt` (Instant)
- [x] Implementar `GetCustodyPositionsUseCase` no módulo `common` (AC: 1, 3, 4)
  - [x] Agrupar trades por Ticker (mesmo algoritmo da história 3.1: `COMPRA` soma, `VENDA` subtrai)
  - [x] Calcular Preço Médio Ponderado: `sum(qty * price) / sum(qty)` para posições com `side=COMPRA`
  - [x] Buscar cotação atual via `StockPricePort.findLatestByCompanyIds(...)` (reutilizar padrão da 3.1)
  - [x] Calcular `gainLossPercentage = ((currentPrice - averagePrice) / averagePrice) * 100`
  - [x] Calcular `marketValue = quantity * currentPrice`
  - [x] Definir `priceSource = LIVE` se `StockPrice.createdAt` for do dia atual, senão `CACHE`
  - [x] Filtrar apenas posições com `quantity > 0` (excluir zeradas / vendidas)
  - [x] Excluir trades do tipo `DIVIDENDO` do cálculo de posições
- [x] Adicionar endpoint `GET /api/v1/portfolios/custody` no `PortfolioController` (AC: 1)
  - [x] Retornar `{ "data": [ ...CustodyPositionDTO ], "meta": { "total": N, "priceUpdatedAt": "..." } }`
  - [x] Garantir isolamento por `userId` extraído do JWT
- [x] Implementar Testes Unitários para `GetCustodyPositionsUseCase` (AC: 1, 3, 4)
  - [x] Cenário: carteira mista (COMPRA + VENDA), posição zerada deve ser excluída
  - [x] Cenário: cotação não encontrada → `currentPrice = averagePrice`, `priceSource = CACHE`
  - [x] Cenário: DIVIDENDO não afeta posição
- [x] Implementar Teste de Integração para o Controller (AC: 1)

### Frontend (Flutter)

- [x] Criar `CustodyPositionModel` em `lib/src/features/portfolio/data/models/` (AC: 1)
  - [x] Campos: `ticker`, `quantity`, `averagePrice`, `currentPrice`, `marketValue`, `gainLossPercentage`, `priceSource` (String), `priceUpdatedAt` (DateTime?)
  - [x] `factory CustodyPositionModel.fromJson(Map<String, dynamic> json)`
- [x] Adicionar método `getCustodyPositions()` em `PortfolioRemoteDataSource` (AC: 1)
  - [x] Endpoint: `GET $baseUrl/portfolios/custody`
  - [x] Reutilizar o padrão de autenticação existente (`Authorization: Bearer $token`)
  - [x] **ATENÇÃO:** O projeto usa `package:http` (não Dio) neste datasource — manter consistência
- [x] Criar entidade `CustodyPosition` em `lib/src/features/portfolio/domain/entities/` (AC: 1)
- [x] Criar interface `getCustodyPositions()` no `PortfolioRepository` em `domain/repositories/`
- [x] Implementar `getCustodyPositions()` no `PortfolioRepositoryImpl` em `data/repositories/`
- [x] Estender `PortfolioProvider` para gerenciar estado da custódia (AC: 1, 2, 3, 4, 5, 6)
  - [x] Adicionar `List<CustodyPosition> _custodyPositions`
  - [x] Adicionar `PortfolioStatus _custodyStatus`
  - [x] Adicionar `String? _custodyError`
  - [x] Método `Future<void> loadCustodyPositions()`
  - [x] Adicionar campo de ordenação: `String _sortColumn`, `bool _sortAscending`
  - [x] Método `void sortBy(String column)` — alterna ascending/descending
- [x] Implementar componente `FinancialDataTable` em `lib/src/features/portfolio/presentation/widgets/` (AC: 1, 2, 3, 4, 5, 6)
  - [x] **Variante compact** por padrão (lista de ativos)
  - [x] Colunas: Ticker | Qtd | Preço Médio | Cotação Atual | Valor de Mercado | Ganho/Perda %
  - [x] Cabeçalhos clicáveis com ícone de seta (`▲`/`▼`) para ordenação
  - [x] Valores monetários formatados com `NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$')`
  - [x] Coluna "Ganho/Perda %" com cor semântica: positivo → Emerald (#10B981), negativo → Red, neutro → branco
  - [x] **Estado loading:** `SkeletonTableWidget` — skeleton de 5 linhas imitando o layout da tabela
  - [x] **Estado empty:** Ilustração + "Nenhum ativo encontrado" + botão "Fazer Upload" (navegar para `UploadScreen`)
  - [x] **Estado error:** `AnomalyAlert` inline com mensagem de erro e botão "Tentar novamente"
  - [x] Indicador de atualização: linha abaixo da tabela com texto "Atualizado em HH:mm" ou "Preço em Cache" (Amber Gold)
  - [x] Acessibilidade: `Semantics` wrapper em cada célula com `aria-label` descritivo; headers com `scope=col`
- [x] Integrar `FinancialDataTable` na `HomeScreen` ou tela de portfólio (AC: 1)
  - [x] Adicionar como segunda seção após os `PortfolioKpiCard`
  - [x] Chamar `portfolioProvider.loadCustodyPositions()` no `initState`
- [x] Rodar `flutter analyze` e garantir conformidade com `analysis_options.yaml`

## Dev Notes

### Contexto Crítico do Projeto

- **Stack Backend:** Java 21 + Spring Boot 3.4.13 + Maven Multi-Module (`common`, `api`, `valuation-service`)
- **Stack Frontend:** Flutter + Provider + GoRouter + `package:http` (NÃO Dio neste datasource)
- **Banco:** MySQL com Liquibase para migrations
- **Auth:** Google Sign-In via Firebase; JWT validado pelo Spring como `spring-security-oauth2-resource-server`; `userId = jwt.getSubject()`

### Padrões Obrigatórios (Architecture Rules)

1. ⛔ **NUNCA** importar Spring/JPA em classes de `domain/` ou `application/usecase/`
2. ✅ **SEMPRE** criar Use Case como POJO com método `execute()` — sem anotações Spring
3. ✅ **SEMPRE** filtrar por `userId` em todas as queries — isolamento mandatório
4. ⛔ **NUNCA** usar `float`/`double` para valores monetários — usar `BigDecimal`
5. ✅ **SEMPRE** criar teste unitário para nova lógica de negócio
6. ✅ **SEMPRE** retornar erros no formato RFC 7807 ProblemDetail (`spring.mvc.problemdetails.enabled=true`)

### Reutilização da História 3.1 (Portfolio KPI Dashboard)

A Story 3.1 criou a fundação que esta história DEVE reutilizar:

- **`TradePort.findAllByUserId(String userId)`** → port de acesso a trades — **reutilizar exatamente este método**
- **`StockPricePort.findLatestByCompanyIds(List<String> companyIds)`** → busca em batch de cotações — **reutilizar**
- **`CompanyRepository.findByTicker(String ticker)`** → mapeamento ticker→empresa — **reutilizar**
- **`GetPortfolioSummaryUseCase`** → referência para o padrão de cálculo de posições (agrupar por ticker, somar COMPRA, subtrair VENDA, excluir DIVIDENDO)
- **`PortfolioProvider`** → **estender** (não criar um novo) com os estados de custódia
- **`PortfolioRemoteDataSource`** → **adicionar método** `getCustodyPositions()` neste arquivo existente

### Cálculo de Preço Médio Ponderado (Regra de Negócio)

```
averagePrice = sum(quantity_i * price_i para side=COMPRA) / sum(quantity_i para side=COMPRA)
```
> **Importante:** O preço médio considera APENAS trades de `COMPRA`. Trades de `VENDA` reduzem a `quantity` mas o preço médio deve ser recalculado apenas sobre as compras restantes. Para simplificação no MVP, calcular como a média ponderada global de todas as compras para aquele ticker.

### Indicador de Cache (NFR8)

- `priceSource = LIVE` se `StockPrice.createdAt` for `>= LocalDate.now().atStartOfDay()` em UTC
- `priceSource = CACHE` caso contrário (dado antigo ou API externa falhou com graceful degradation)
- Frontend: exibir "Preço em Cache" em Amber Gold (`#F59E0B`) com ícone `Icons.warning_amber_rounded`

### Design System (obrigatório)

```dart
// Cores (do tema existente em core/theme/)
final emeraldGreen = Color(0xFF10B981);   // Ganho positivo
final navyBlue     = Color(0xFF1B2A4A);   // Neutro/base
final amberGold    = Color(0xFFF59E0B);   // Aviso / Cache
final errorRed     = Color(0xFFEF4444);   // Perda negativa

// Tipografia (DM Sans/Inter/JetBrains Mono já configurados)
// Valores monetários → JetBrains Mono (monospace para alinhamento)
// Headers da tabela → DM Sans SemiBold
// Microcopy (status, legenda) → Inter Regular 12px
```

### Formatação de Dados (Flutter)

```dart
// Importação necessária
import 'package:intl/intl.dart';

final currencyFormatter = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
final percentFormatter  = NumberFormat('+##0.00%;-##0.00%', 'pt_BR');
final quantityFormatter = NumberFormat('#,##0', 'pt_BR');
final timeFormatter     = DateFormat('HH:mm', 'pt_BR');
```

### Estrutura de Arquivos a Criar/Modificar

**Backend (`common` module):**
```
backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/
├── application/
│   ├── dto/
│   │   └── CustodyPositionDTO.java              [NEW]
│   └── usecase/
│       └── GetCustodyPositionsUseCase.java       [NEW]
```

**Backend (`api` module):**
```
backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/
├── web/
│   └── PortfolioController.java                 [MODIFY — adicionar endpoint]
└── infrastructure/spring/
    └── PortfolioServiceConfiguration.java       [MODIFY — adicionar bean GetCustodyPositionsUseCase]
```

**Frontend:**
```
frontend/lib/src/features/portfolio/
├── data/
│   ├── datasources/
│   │   └── portfolio_remote_data_source.dart    [MODIFY — adicionar getCustodyPositions()]
│   ├── models/
│   │   └── custody_position_model.dart          [NEW]
│   └── repositories/
│       └── portfolio_repository_impl.dart       [MODIFY — implementar getCustodyPositions()]
├── domain/
│   ├── entities/
│   │   └── custody_position.dart                [NEW]
│   └── repositories/
│       └── portfolio_repository.dart            [MODIFY — declarar getCustodyPositions()]
└── presentation/
    ├── providers/
    │   └── portfolio_provider.dart              [MODIFY — adicionar estados de custódia]
    ├── widgets/
    │   └── financial_data_table.dart            [NEW]
    └── pages/
        └── home_screen.dart (ou equivalente)    [MODIFY — integrar FinancialDataTable]
```

**Testes:**
```
backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/
└── application/usecase/
    └── GetCustodyPositionsUseCaseTest.java      [NEW]
```

### Lições da História 3.1

- **Cotação pode estar ausente** (empresa não cadastrada no sistema): tratar com `log.warn()` no backend e usar `averagePrice` como fallback de `currentPrice` → indicar `priceSource = CACHE`
- **Isolamento de userId** é mandatório em todas as queries — o método `TradePort.findAllByUserId(userId)` já garante isso
- **Skeleton Screens** devem imitar o layout final. Para a tabela, criar `SkeletonTableWidget` com 5 linhas de `ShimmerEffect`
- **Provider pattern:** estados `initial`, `loading`, `success`, `error` — manter consistente com `PortfolioStatus` já existente
- **`package:http`** é usado no `portfolio_remote_data_source.dart` existente (não Dio). Manter consistência.
- **`flutter analyze`** deve passar sem warnings antes de considerar a implementação concluída

### Commits Recentes Relevantes

- `e61deb3` — `feat: implement portfolio summary feature...` → criou toda a fundação da feature portfolio (3.1)
- `96a11c1` — `test: implement dashboard, portfolio, and performance testing suite...` → referência para padrões de teste
- `aaa3f0d` — `fix(frontend): remove unused import and avoid print in performance tests` → prática: sem `print()` em código de produção/teste

### Project Structure Notes

- **Backend:** Seguir Clean Architecture rigorosa (`domain` ← `application` ← `infrastructure`). Use Case como POJO com `execute()`.
- **Package root backend:** `afsdigital.grahamselect.common.portfolio` (módulo `common`) para Use Case e DTO; `afsdigital.grahamselect.api.portfolio` (módulo `api`) para Controller e adaptadores JPA.
- **Frontend:** Feature-first em `lib/src/features/portfolio/`. Estrutura de camadas: `data/`, `domain/`, `presentation/`.
- **Naming conventions:**
  - Java: `PascalCase` (classes), `camelCase` (métodos/vars), `snake_case` (DB, Kafka topics)
  - Dart: `PascalCase` (classes), `snake_case.dart` (arquivos), `camelCase` (vars/métodos), `kCamelCase` (constantes)
  - JSON: `camelCase`
  - Datas: ISO 8601 `"2026-03-12T08:00:00Z"`
  - Monetários (Java): `BigDecimal` serializado como `String` no JSON (Jackson)

### Resposta da API (Formato Obrigatório)

```json
{
  "data": [
    {
      "ticker": "ITUB4",
      "quantity": "100",
      "averagePrice": "28.50",
      "currentPrice": "32.10",
      "marketValue": "3210.00",
      "gainLossPercentage": "12.63",
      "priceSource": "LIVE",
      "priceUpdatedAt": "2026-05-30T14:00:00Z"
    }
  ],
  "meta": {
    "total": 1,
    "priceUpdatedAt": "2026-05-30T14:00:00Z"
  }
}
```

### References

- [Source: docs/bmad/planning-artifacts/epics.md#Story 3.2]
- [Source: docs/bmad/planning-artifacts/epics.md#Epic 3]
- [Source: docs/bmad/planning-artifacts/architecture.md#D7 API Design]
- [Source: docs/bmad/planning-artifacts/architecture.md#D12 Estrutura Flutter]
- [Source: docs/bmad/planning-artifacts/architecture.md#Regras Obrigatórias para Agentes de IA]
- [Source: docs/bmad/planning-artifacts/architecture.md#Padrões de Implementação]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#FinancialDataTable]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Responsive Design & Accessibility]
- [Source: docs/bmad/planning-artifacts/ux-design-specification.md#Loading States]
- [Source: docs/bmad/implementation-artifacts/3-1-portfolio-kpi-dashboard.md]
- [Source: backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetPortfolioSummaryUseCase.java]
- [Source: backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/PortfolioController.java]
- [Source: backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/persistence/jpa/entities/TradeEntity.java]
- [Source: frontend/lib/src/features/portfolio/data/datasources/portfolio_remote_data_source.dart]
- [Source: frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart]

## Dev Agent Record

### Agent Model Used

Gemini 3.5 Flash — dev-story workflow

### Debug Log References

### Completion Notes List

- Implementação completa e robusta do backend Java e frontend Flutter para a tabela de custódia.
- Criadas as classes CustodyPositionDTO, GetCustodyPositionsUseCase no backend common, e adicionado endpoint REST /api/v1/portfolios/custody no api-service.
- Criadas as classes CustodyPosition (entidade), CustodyPositionModel (model com parsing seguro), métodos correspondentes em Repository/DataSource, e estados de custódia com ordenação local no PortfolioProvider.
- Desenvolvido o componente visual de alta fidelidade FinancialDataTable com Shimmer skeleton, Empty state moderno de ilustração e CTA que navega para Upload, AnomalyAlert de erro, formatação de JetBrains Mono e acessibilidade via Semantics.
- Integrado na página de Dashboard do portfólio de forma fluida.
- Resolvido um problema crítico de compilação do Lombok com JDK 23 do terminal do sandbox atualizando a versão do Lombok para 1.18.34 e adicionando a flag -proc:full ao maven-compiler-plugin de common e do pom-parent.

### File List

- backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/dto/CustodyPositionDTO.java
- backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java
- backend/common/src/test/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCaseTest.java
- backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/infrastructure/spring/PortfolioSummaryConfiguration.java
- backend/api/src/main/java/afsdigital/grahamselect/api/portfolio/web/PortfolioController.java
- backend/api/src/test/java/afsdigital/grahamselect/api/portfolio/web/PortfolioControllerIT.java
- backend/pom.xml
- backend/common/pom.xml
- frontend/lib/src/features/portfolio/domain/entities/custody_position.dart
- frontend/lib/src/features/portfolio/data/models/custody_position_model.dart
- frontend/lib/src/features/portfolio/domain/repositories/portfolio_repository.dart
- frontend/lib/src/features/portfolio/data/repositories/portfolio_repository_impl.dart
- frontend/lib/src/features/portfolio/data/datasources/portfolio_remote_data_source.dart
- frontend/lib/src/features/portfolio/presentation/providers/portfolio_provider.dart
- frontend/lib/src/features/portfolio/presentation/widgets/financial_data_table.dart
- frontend/lib/src/features/dashboard/presentation/pages/dashboard_page.dart
- frontend/test/features/dashboard/presentation/pages/dashboard_page_test.dart
- frontend/test/features/dashboard/presentation/pages/dashboard_page_test.mocks.dart
- docs/bmad/implementation-artifacts/3-2-valuation-custody-table.md

### Review Follow-ups (AI)

- [ ] [AI-Review][HIGH] Violação de Clean Architecture: `GetCustodyPositionsUseCase` em `common` importa diretamente de `valuation` (`CompanyRepository`, `StockPricePort`, `StockPrice`). Débito arquitetural pré-existente da história 3.1. Solução: mover as interfaces `CompanyRepository` e `StockPricePort` para o módulo `common` para eliminar a dependência cruzada entre módulos Maven.
