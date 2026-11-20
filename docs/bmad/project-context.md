---
project_name: 'graham-select'
user_name: 'Alex'
date: '2026-11-20T19:08:55-03:00'
sections_completed: ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'quality_rules', 'workflow_rules', 'anti_patterns']
status: 'complete'
rule_count: 18
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

- **Frontend:** Flutter SDK `^3.11.0`, Dart 3
  - Dependências principais: `provider` (`^6.1.2`), `go_router` (`^14.3.0`), `http` (`^1.2.2`), `firebase_core` (`^3.11.0`), `file_picker` (`^8.1.4`)
- **Backend:** Java `21` (compatibilidade com JDK 21/22 configurada em `start-app.sh`), Spring Boot `3.4.13`
  - Arquitetura multi-módulo: `common`, `api`, `valuation-service`
- **Banco de Dados & Migrations:** MySQL `8.0.33` (TiDB Serverless) gerenciado exclusivamente via **Liquibase**
- **Mensageria:** Apache Kafka (Aiven)
- **Infraestrutura & Deploy:**
  - Local: Docker Compose (Kafka, MySQL)
  - Produção: Spring Boot Cloud Native Buildpacks (hospedado no Render.com)
- **Documentação de Arquitetura (C4):** Structurizr DSL (`docs/workspace.dsl`)

---

## Critical Implementation Rules

### Language-Specific Rules

- **Java 21:**
  - **Recursos Modernos:** Priorizar Records, Pattern Matching e estruturas imutáveis sempre que aplicável.
  - **Manipulação Temporal Mandatória:** Todas as operações temporais (datas e horas) DEVEM utilizar explicitamente `ZoneOffset.UTC`. Utilize `OffsetDateTime.now(ZoneOffset.UTC)` ou `LocalDateTime.now(ZoneOffset.UTC)` de forma consistente em todas as entidades, DTOs e cálculos para evitar divergências de fuso horário.
  - **Tratamento de Exceções & Logs:** Toda exceção capturada ou lançada DEVE registrar log em nível `ERROR` acompanhada do stack trace correspondente (`logger.error("Context message...", e)`). Nunca silencie exceções com blocos `catch` vazios.
- **Dart:**
  - **Tipagem Estrita & Null Safety:** Tipagem explícita em assinaturas de métodos e propriedades públicas; null safety estrito sem uso indiscriminado de force-unwrap (`!`).
  - **Tratamento Assíncrono:** Tratar chamadas assíncronas (`async`/`await`) com blocos `try/catch`, propagando erros de forma controlada na interface.

### Framework-Specific Rules

- **Spring Boot Multi-Módulo:**
  - **Divisão de Responsabilidades:**
    - `common`: DTOs compartilhados, modelos de domínio e utilitários de negócio reutilizáveis.
    - `api`: Endpoints REST públicos e autenticados, extração de token JWT, CORS, validação de payload e publicação de eventos para processamento assíncrono.
    - `valuation-service`: Processamento intensivo de dados consumidos exclusivamente via Apache Kafka; **não** deve expor endpoints HTTP de entrada para usuários.
  - **Observabilidade em Entradas:** Obrigatório registrar log em nível `INFO` em **todas** as entradas de métodos de endpoints de API e no recebimento de mensagens em consumers Kafka, incluindo metadados contextuais (`userId`, IDs de transação/ativo, status).
  - **Imutabilidade do Liquibase:** É **estritamente proibido** alterar retroativamente arquivos de migration já aplicados. Qualquer alteração de esquema ou correção em banco de dados deve ser realizada via **novas migrations incrementais e idempotentes**, aplicando `preConditions` ou `validCheckSum` quando pertinente.
- **Flutter:**
  - **Gerência de Estado:** Utilizar `provider` (`ChangeNotifierProvider`, `context.watch()`, `context.read()`) para desacoplar lógica de visualização.
  - **Roteamento:** Rotas centralizadas e declarativas via `go_router`.
  - **Estrutura por Features:** Código organizado por funcionalidade dentro de `lib/src/features/<feature_name>/{data,domain,presentation}`.

### Testing Rules

- **Validação Automatizada Mandatória:**
  - Após implementar qualquer funcionalidade ou correção de review, **é obrigatório** executar toda a suíte de testes:
    - Backend: `./mvnw test` (ou `mvn test`)
    - Frontend: `flutter test`
- **Análise Estática Rigorosa:**
  - O comando `flutter analyze` deve ser executado no diretório `frontend` e deve passar com **zero** erros e alertas de linting (conforme `analysis_options.yaml`).
- **Cobertura de Casos de Borda:**
  - Novas regras de negócio (cálculos financeiros, tributação/IRPF, consolidação de custódia, family holding) e novos widgets de interface devem vir acompanhados de seus respectivos testes unitários e de widget (`flutter_test`).

### Code Quality & Style Rules

- **Rastreabilidade Arquitetural (C4 Model):**
  - Todas as decisões e novos componentes/containers devem ser refletidos e mantidos atualizados em `docs/workspace.dsl` (Structurizr).
- **Tratamento de Exceções & Log Traces:**
  - Proibido engolir exceções. Todo bloco `catch` deve registrar contexto de falha detalhado e o stack trace correspondente no log `ERROR`.

### Development Workflow Rules

- **Branching Mandatório:** Sempre crie e trabalhe em uma branch isolada e sincronizada antes de implementar uma história (`git checkout -b feature/nome-da-historia main` ou a partir da branch base designada).
- **Commits:** Seguir rigorosamente o padrão *Conventional Commits* (ex: `feat: ...`, `fix: ...`, `chore: ...`).
- **Deploy Backend:** Não criar Dockerfiles manuais para os serviços Spring Boot; utilizar sempre os Buildpacks nativos (`./mvnw spring-boot:build-image -pl api`).
- **Abertura Autônoma de Pull Requests (PR):** Quando solicitado abrir/criar PR, o agente DEVE criá-lo de forma 100% autônoma via API do GitHub (`POST /repos/{owner}/{repo}/pulls`) usando credenciais obtidas com `git credential fill` caso a CLI `gh` não esteja no PATH, sem transferir a ação para o usuário via link web.

### Critical Don't-Miss Rules (Anti-Patterns & Gotchas)

- **Comunicação Assíncrona Estrita:** Jamais realizar chamadas HTTP síncronas entre `api` e `valuation-service`. A troca de dados para processamento financeiro deve ocorrer exclusivamente via tópicos do Apache Kafka.
- **Imutabilidade de Migrations (Liquibase):** Nunca modifique migrations existentes que já tenham sido executadas ou comitadas. Adicione novas migrations incrementais.
- **Multitenancy & JWT:** O `userId` deve ser extraído do token JWT (`jwt.getSubject()`) de forma estrita em todos os endpoints autenticados e propagado em todas as consultas SQL/JPA e eventos de mensageria para garantir isolamento absoluto por usuário.
- **Path Traversal em Uploads:** Qualquer endpoint de ingestão de arquivos (ex: planilhas B3) deve sanitizar o nome dos arquivos descartando caminhos relativos ou caracteres de escape (`../`).
- **CORS Centralizado:** Configurado via `CorsConfigurationSource` no Spring Security cobrindo `localhost:3000` e o domínio oficial no Render (`https://graham-select-frontend.onrender.com`).

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

Last Updated: 2026-11-20T19:08:55-03:00
