---
title: 'Correção do Erro de Importação de Planilha B3'
slug: 'correcao-erro-importacao-planilha-b3'
created: '2026-06-01T00:40:22-03:00'
status: 'ready-for-dev'
stepsCompleted: [1, 2, 3, 4]
tech_stack: ["Java 21", "Spring Boot 3.4.13", "FastExcel", "Apache Kafka", "JUnit 5", "AssertJ"]
files_to_modify: ["backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/FastExcelB3TradeRowParser.java", "backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/parser/FastExcelB3TradeRowParserTest.java"]
code_patterns: ["FastExcel Streaming Reader", "DDD Adapter Pattern", "UTC Temporal Standards"]
test_patterns: ["JUnit 5 Integration and Unit tests", "AssertJ fluent assertions", "FastExcel Mocking"]
---

# Tech-Spec: Correção do Erro de Importação de Planilha B3

**Created:** 2026-06-01T00:40:22-03:00

## Overview

### Problem Statement

Ao realizar a ingestão de planilhas de negociação da B3 (.xlsx), ocorrem falhas no parser devido a formatações numéricas e de data inconsistentes geradas por diferentes corretoras ou exportações da B3 (como células numéricas formatadas como String, presença de caracteres como 'R$' e separadores de milhar incorretos, ou datas contendo horas/espaços). Isso resulta em rejeição de linhas válidas da planilha, impedindo o onboarding fluido e a consolidação de carteira do investidor.

### Solution

Tornar o parser `FastExcelB3TradeRowParser` altamente resiliente por meio de uma extração flexível: realizar a limpeza de strings monetárias e fallbacks de parseamento numérico para `BigDecimal` e tratamento flexível de datas (remoção de espaços e suporte a formatos adicionais com hora embutida), garantindo que as operações sejam extraídas com sucesso, independentemente da formatação textual da célula.

### Scope

**In Scope:**
- Refatoração do `FastExcelB3TradeRowParser` para converter de forma resiliente as colunas QUANTIDADE e PRECO, mesmo quando formatadas como texto ou contendo formatações de moeda ('R$', pontos e vírgulas).
- Robustez no parsing de DATA para tratar espaços em branco e tolerar representações com hora/minuto embutidos.
- Expansão dos testes unitários em `FastExcelB3TradeRowParserTest` com casos reais de inputs desformatados que agora devem ser aceitos.

**Out of Scope:**
- Alterações no fluxo assíncrono do Kafka ou no tratamento do tópico DLQ.
- Mudanças visuais no frontend (a tela de upload e tratamento de status já operam corretamente).

## Context for Development

### Codebase Patterns

- **FastExcel Streaming API**: O parser utiliza o padrão streaming do FastExcel para leitura em baixo memory footprint, o que deve ser estritamente preservado para evitar vulnerabilidade OOM (OutOfMemoryError) em grandes planilhas.
- **Isolamento de Negócio (DDD)**: O parser no módulo `api` deve produzir o DTO puramente agnóstico `B3TradeRow` que reside no módulo `common` sem misturar tipos de bibliotecas de terceiros como `Row` ou `Cell` no domínio core.
- **Tratamento UTC Obrigatório**: Datas devem ser tratadas de forma neutra de fuso horário local e persistidas com referência UTC estrita.

### Files to Reference

| File | Purpose |
| ---- | ------- |
| [FastExcelB3TradeRowParser.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/FastExcelB3TradeRowParser.java) | Realiza o parse das linhas da planilha B3 usando FastExcel |
| [FastExcelB3TradeRowParserTest.java](file:///Users/alexandrofs/projects/graham-select/backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/parser/FastExcelB3TradeRowParserTest.java) | Testes unitários do parser da planilha |

### Technical Decisions

- **Higienização de Strings Monetárias**: Implementação do helper `parseNumericCell` para encapsular a tentativa do `row.getCellAsNumber` e o fallback textual, limpando pontuações de moeda PT-BR (removendo `R$`, espaços em branco e pontos de milhar, além de substituir a vírgula decimal por ponto) antes do parseamento para `BigDecimal`.
- **Parsing de Datas Flexível**: O método `parseTradeDate` será estendido para remover espaços extras e separar a string de data caso contenha hora embutida (utilizando split espacial), permitindo que formatos complexos como `dd/MM/yyyy HH:mm:ss` sejam suportados nativamente como `dd/MM/yyyy`.

## Implementation Plan

### Tasks

- [ ] **Task 1: Implementação de helpers resilientes no parser**
  - **File:** `backend/api/src/main/java/afsdigital/grahamselect/api/upload/infrastructure/parser/FastExcelB3TradeRowParser.java`
  - **Action:**
    - Implementar o método utilitário `parseNumericCell(Row row, Integer index)` para parsear e higienizar células numéricas de quantidade e preço (com tratamento de fallback de String, limpeza de `R$`, espaços e pontos, e substituição de vírgula).
    - Refatorar a extração de `quantity` e `price` em `toTradeRow` para utilizar `parseNumericCell`.
    - Atualizar o parseador de data `parseTradeDate` para fazer trim na string e remover o horário embutido (caso haja espaço) antes de efetuar o `LocalDate.parse`.
  - **Notes:** Garantir que nenhuma exceção seja lançada para formatos válidos representados textualmente (ex: "35,50", " 1.200 ", "15/05/2026 14:00").

- [ ] **Task 2: Atualização dos Testes Unitários do Parser**
  - **File:** `backend/api/src/test/java/afsdigital/grahamselect/api/upload/infrastructure/parser/FastExcelB3TradeRowParserTest.java`
  - **Action:**
    - Adicionar testes de sucesso contendo células numéricas representadas como Strings com formatação brasileira e símbolos monetários (ex: "R$ 35,50", "1.000", " 100 ").
    - Adicionar testes de sucesso contendo células de data formatadas como String com espaços adicionais e com hora embutida (ex: " 15/05/2026 ", "15/05/2026 14:30:00").
    - Validar que esses casos não geram falhas no parser e chamam corretamente o `successConsumer`.
  - **Notes:** Executar os testes localmente com Maven para garantir o build verde.

### Acceptance Criteria

- [ ] **AC 1: Resiliência em Valores Monetários e Numéricos**
  - **Given** uma planilha B3 contendo a quantidade formatada como texto com espaços e o preço como string contendo "R$ " e vírgula decimal (ex: `"100"` e `"R$ 35,50"`),
  - **When** o parser processa a linha,
  - **Then** a linha é convertida com sucesso em um `B3TradeRow` com quantidade `100` e preço `35.50` sem lançar exceções.

- [ ] **AC 2: Tolerância em Strings de Datas**
  - **Given** uma planilha B3 contendo a data formatada como string com espaços invisíveis ou contendo horas (ex: `" 15/05/2026 "` ou `"15/05/2026 14:30"`),
  - **When** o parser processa a data,
  - **Then** ela é extraída com sucesso como `2026-05-15` (LocalDate) sem falhar.

- [ ] **AC 3: Isolamento de Erros por Linha**
  - **Given** uma linha da planilha que seja puramente corrompida (como quantidade `"abc"` ou data inválida `"xyz"`),
  - **When** o parser processa a linha,
  - **Then** a falha é detectada individualmente e encaminhada para o `failureConsumer` preservando a execução e o parsing das demais linhas corretas.

## Additional Context

### Dependencies

- fastexcel-reader (módulo api e common)

### Testing Strategy

- **Testes Unitários**: Rodar `mvn test -pl api -Dtest=FastExcelB3TradeRowParserTest` para certificar que a classe de parser atende todos os casos.
- **Testes de Integração Backend**: Rodar os testes de integração do Spring Kafka para garantir que a resiliência no parser impede que registros válidos caiam na fila DLQ.
- **Verificação Manual**: Realizar o upload de uma planilha Excel modificada com campos em String e formatações de moeda na interface local e validar que a importação do lote completa com sucesso no dashboard.

### Notes

Nenhum.
