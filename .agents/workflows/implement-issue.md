---
description: Implementar uma Issue completa (Planejamento, Desenvolvimento, Testes e PR)
---

# Workflow de Implementação de Issue (Padrão Graham Select)

Este workflow descreve o processo completo para resolver uma issue no projeto, garantindo consistência, qualidade e alinhamento com os padrões estabelecidos (ex: configuração via YAML, beans explícitos).

## 1. Análise e Planejamento
Antes de escrever qualquer código, entenda o problema e planeje a solução.

- [ ] **Compreensão**: Leia a issue cuidadosamente. Identifique os requisitos e o escopo.
- [ ] **Pesquisa**: Verifique se já existem padrões similares no projeto (ex: Consumers, Controllers, Repositories).
- [ ] **Brainstorming**: Crie os artefatos de planejamento na pasta brain:
    - `task.md`: Checklist detalhado das tarefas (Planejamento, Implementação, Verificação).
    - `implementation_plan.md`: Plano técnico (Arquitetura, Mudanças de Arquivos, Testes).
- [ ] **Aprovação**: Solicite revisão do plano ao usuário antes de começar a codificar.

## 2. Preparação do Ambiente
- [ ] **Main**: Faça checkout da `main` (ou branch base).
    ```bash
    git checkout main
    ```
- [ ] **Main**: Faça pull e garanta que não há arquivos alterados e não commitados.
- [ ] **Branch**: Crie uma branch descritiva a partir da `main` (ou branch base).
    ```bash
    git checkout -b feature/<nome-descritivo-da-issue>
    ```
- [ ] **Limpeza**: Garanta que o ambiente está limpo e os testes atuais passam.
    ```bash
    mvn clean test
    ```

## 3. Desenvolvimento (Ciclo TDD/Iterativo)
Siga os padrões do projeto:
- **Configuração**: Prefira `application.yml` para configurações (evite `@Configuration` complexas desnecessárias).
- **Injeção de Dependência**: Declare beans explicitamente se o `@ComponentScan` for restrito no módulo.
- **Logs**: Use Slf4j para logs estruturados e claros.

- [ ] **Testes Primeiro (Opcional/Recomendado)**: Crie testes falhando para a funcionalidade (Unitários ou Integração).
- [ ] **Implementação**: Escreva o código para fazer os testes passarem.
- [ ] **Refatoração**: Melhore o código mantendo os testes verdes.

## 4. Verificação e Qualidade
Garanta que a nova feature funciona e não quebrou nada existente.

- [ ] **Testes Unitários**: Valide a lógica de negócio isolada.
- [ ] **Testes de Integração**: Use Testcontainers (`@EmbeddedKafka`, `@Container MySQL`) para validar o fluxo real.
- [ ] **Testes de Contexto**: Verifique se o contexto do Spring sobe sem erros (`ContextTest`).
- [ ] **Build Completo**:
    ```bash
    // turbo
    mvn clean install
    ```

## 5. Documentação e Entrega
- [ ] **Walkthrough**: Crie/Atualize o `walkthrough.md` documentando o que foi feito, decisões tomadas e como testar.
- [ ] **Revisão Própria**: Releia seu código (Self-Code-Review).
- [ ] **Push e PR**:
    ```bash
    git push origin feature/<nome-descritivo-da-issue>
    ```
    - Abra a Pull Request.
    - Responda aos comentários da revisão (se houver) e faça commits de fix.

## Dicas do Projeto
- **Kafka**: Use `spring.json.use.type.headers: false` no `application.yml` para evitar problemas de deserialização de classes entre serviços.
- **Transações**: Lembre-se de `@Transactional` nos serviços que alteram o banco de dados.
- **Lombok**: Use `@RequiredArgsConstructor` para injeção de dependência via construtor.