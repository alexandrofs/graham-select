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

### Critical Don't-Miss Rules

- **Comunicação Assíncrona:** Nunca realizar chamadas síncronas entre `api` e `valuation-service`. A comunicação **deve** fluir exclusivamente via Apache Kafka.
- **CORS:** O backend Spring Boot (porta 8080) deve sempre possuir CORS ativo permitindo chamadas do Frontend Web (porta 3000).

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

Last Updated: 2026-03-08T15:24:29-03:00
