---
project_name: 'graham-select'
user_name: 'Alex'
date: '2026-03-08T15:24:29-03:00'
sections_completed: ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'quality_rules', 'workflow_rules', 'anti_patterns']
status: 'complete'
rule_count: 14
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

- **Frontend:** Flutter SDK `^3.11.0`, Dart
- **Backend:** Java `21`, Spring Boot `3.4.13` (Multi-módulo: `common`, `api`, `valuation-service`)
- **Mensageria:** Apache Kafka (Aiven)
- **Banco de Dados:** MySQL `8.0.33` (TiDB Serverless)
- **Infra/Deploy:** Docker Compose (Local), Spring Native Cloud Native Buildpacks (Produção no Render.com)
- **Arquitetura (C4):** Structurizr

## Critical Implementation Rules

### Language-Specific Rules

- **Java 21:** 
  - Priorizar recursos modernos da linguagem (Records, Pattern Matching) onde aplicável.
  - **Obrigatório:** Todas as operações temporais (datas e horas) DEVEM utilizar `ZoneOffset.UTC`. Use `OffsetDateTime.now(ZoneOffset.UTC)` ou `LocalDateTime.now(ZoneOffset.UTC)` de forma consistente para evitar bugs de fuso horário.
- **Dart:** Tipagem estrita é obrigatória. Null safety garantido em todas as implementações.

### Framework-Specific Rules

- **Spring Boot Multi-Módulo:** 
  - Regras de negócio compartilhadas e DTOs devem residir no módulo `common`.
  - O módulo `api` deve focar apenas na recepção de arquivos/dados e endpoints REST.
  - O módulo `valuation-service` não deve ter endpoints HTTP de entrada de usuário, consumindo apenas dados do Kafka.
- **Flutter:** Utilizar `provider` (`^6.1.2`) para gerência de estado e `go_router` (`^14.3.0`) para rotas.

### Testing Rules

- Lints rigorosos devem ser sempre respeitados no frontend (definidos em `analysis_options.yaml` via `flutter_lints`).

### Code Quality & Style Rules

- Todas as definições de arquitetura C4 devem ser mantidas e atualizadas em `docs/workspace.dsl` (Structurizr).

### Development Workflow Rules

- **Branching:** Novas funcionalidades devem ser desenvolvidas em branches nomeadas como `feature/nome-da-feature`.
- **Commits:** Seguir Conventional Commits rigorosamente (ex: `feat: minha nova feature`, `fix: correcao de bug`).
- **Deploy:** Nenhuma imagem Docker deve ser gerada via Dockerfile manual para o backend; utilizar sempre os comandos de Buildpacks nativos (`./mvnw spring-boot:build-image -pl api`).
- **Pull Requests (PR):** Ao receber comando para abrir/criar PR, crie-o diretamente e de forma autônoma via API do GitHub (`POST /repos/{owner}/{repo}/pulls`) usando credenciais obtidas de `git credential fill`, sem transferir a ação para o usuário via link manual.

### Critical Don't-Miss Rules

- **Comunicação Assíncrona:** Nunca realizar chamadas síncronas entre `api` e `valuation-service`. A comunicação **deve** fluir exclusivamente via Apache Kafka.
- **CORS:** O backend Spring Boot (porta 8080) deve sempre possuir CORS ativo permitindo chamadas do Frontend Web (porta 3000).

### Numeric Robustness & Contract Rules (Retro Epic 5)

- **Robustez numérica (obrigatório em todo cálculo financeiro, Java e Dart):** validar entradas com `Double.isFinite` / `.isFinite` antes de calcular; aplicar `clamp` em percentuais (0–100) e valores negativos de patrimônio/aporte; nunca multiplicar `0 × ∞` nem dividir por zero (checar denominador antes); resultados `NaN`/`Infinity` devem virar estado explícito (ex.: `isUnreachable`) e nunca chegar à UI ou ao JSON. Citar esta regra nas tasks de toda story com cálculo.
- **Paridade Java × Dart:** lógica duplicada nas duas linguagens (ex.: `TimeToGoalCalculator`) deve ter testes de paridade com vetores compartilhados em `backend/common/src/test/resources/goals/time-to-goal-parity-vectors.json`. Toda correção deve ser feita nas duas implementações e novo caso adicionado ao JSON.
- **Create-story — checagens obrigatórias:** (1) cruzar todos os cenários de exceção/circuit breaker do `epics.md` com as tasks da story; (2) validar contratos de API (formato objeto vs. array, nomes de campos, `totalValue` vs. `totalEquity`) contra o código/controllers existentes antes de marcar `ready-for-dev`.
---

## Security Checklist

Antes de dar como finalizado qualquer desenvolvimento backend ou de infraestrutura, valide:
1. **CORS Production Domain:** Domínios locais e o domínio oficial do Render (`https://graham-select-frontend.onrender.com`) devem estar explicitamente configurados em um bean centralizado de `CorsConfigurationSource` no `SecurityConfig.java`. Evite padrões dinâmicos ou `*` combinados com `allowCredentials(true)` em produção.
2. **JWT Extraction & Multitenancy:** O `userId` deve ser extraído do token JWT (`jwt.getSubject()`) de forma estrita em todos os endpoints não públicos. Cada query ou persistência de dados de carteira, trades ou auditoria deve incluir o `userId` no filtro para garantir o isolamento total por usuário.
3. **Path Traversal Protection:** Qualquer funcionalidade de recebimento de arquivos (como uploads de extratos B3) deve filtrar o nome do arquivo descartando caminhos relativos ou caracteres de escape (`../`) para prevenir ataques de Path Traversal.
4. **SSE Preflight Validation:** Certifique-se de que endpoints de streaming de eventos (como SSE `/stream`) tratam corretamente requisições Preflight `OPTIONS` de forma anônima antes da requisição real autenticada.

---

## Deploy & Production Gate Checklist (Smoke Tests)

Após qualquer deploy em produção (Render.com), o Project Lead e o agente DEV devem validar manualmente estes 5 pontos críticos em `graham-select-frontend.onrender.com`:
1. **Status Ao Vivo (SSE):** O dashboard exibe o badge pulsante verde "● Ao vivo" no canto superior indicando sucesso na conexão de streaming de eventos em tempo real.
2. **KPI Dashboard Cards:** Os cards de patrimônio, rentabilidade e dividendos carregam e exibem dados reais (com Skeleton Screen animado durante o fetching).
3. **Upload de Extrato B3:** A funcionalidade de upload aceita planilhas `.xlsx` e dispara o pipeline assíncrono Kafka.
4. **Operações Manuais (Reactive Update):** A inserção manual de um trade (Compra/Venda) deve atualizar o dashboard e a tabela de custódia instantaneamente via SSE (sem necessidade de dar refresh no browser).
5. **Session Persistency & Auth:** Efetuar logout e login novamente via Google OAuth2 garantindo fluxo correto de renovação e armazenamento do JWT.

---

## Autonomous / YOLO Mode Governance

Quando agentes AI operam de forma autônoma (sem validação humana história a história):
1. **Rastreabilidade Obrigatória:** Nenhuma história de usuário (`story.md`) pode ter seu status alterado para `done` sem que **todas** as caixas de tarefas `[ ]` internas estejam marcadas como `[x]` e a seção `File List` liste fisicamente todos os caminhos dos arquivos criados e editados.
2. **Testes Estáticos & Compilação:** É obrigatório rodar localmente `mvn clean test` no backend e `flutter analyze` + testes de widget no frontend a cada história concluída.
3. **Gate Humano de Deploy:** Modificações que envolvam CORS, portas, banco de dados ou ambiente de produção exigem aprovação manual e Smoke Test do Project Lead antes do merge da branch na `main`.

4. **Coesão de Escopo (Fatiamento Flexível):** histórias devem ter foco funcional único e claro. Use ~20-25 arquivos alterados como sinal de alerta para avaliar a divisão em fatias menores (heurística, sem rigidez excessiva).
5. **Critérios do YOLO Mode:** o modo autônomo/YOLO é permitido apenas para histórias de documentação, setup e correções de baixo risco. Features críticas (autenticação, ingestão de dados, cálculos financeiros, CORS/produção) exigem validação humana e E2E antes de `done`.
---

## Usage Guidelines

**For AI Agents:**

- Read this file before implementing any code
- Follow ALL rules exactly as documented
- When in doubt, prefer the more restrictive option
- Update this file if new patterns emerge

**For Humans:**

- Keep this file lean and focused on agent needs
- Update when technology stack changes
- Review quarterly for outdated rules
- Remove rules that become obvious over time

Last Updated: 2026-05-31T16:46:00-03:00
