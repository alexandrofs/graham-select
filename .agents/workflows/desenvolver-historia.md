---
description: Desenvolver uma história completa do Graham Select utilizando os fluxos BMAD e validações locais (Maven, Flutter) fornecendo apenas o ID da história.
---

# 🚀 Workflow: Desenvolver História (Graham Select)

Este workflow automatiza e padroniza o desenvolvimento de ponta a ponta para qualquer história/funcionalidade no **Graham Select**, alinhando as melhores práticas do BMAD com as regras rígidas do `AGENTS.md`.

---

## 📥 Como Invocar

Para executar este fluxo, basta fornecer o identificador da história. 
> *Exemplo de comando:* `/desenvolver-historia para a história 3.4`

---

## 📝 Passo a Passo de Execução

### Fase 1: Preparação do Ambiente & Branch
- [ ] **Sincronizar**: Atualizar a branch principal e garantir que não há conflitos locais pendentes:
    ```bash
    git checkout main && git pull
    ```
- [ ] **Criar Branch**: Criar e acessar a branch da funcionalidade baseada no ID fornecido:
    ```bash
    git checkout -b feature/<ID_DA_HISTORIA>-<descricao-curta>
    ```

### Fase 2: Planejamento (BMAD)
- [ ] **Criar História**: Executar o fluxo BMAD para gerar o arquivo Markdown de especificação da história:
    ```bash
    bmad-bmm-create-story
    ```
- [ ] **Sprint Status**: Abrir o arquivo `sprint-status.yaml` e atualizar o status da história correspondente para `in_progress`.

### Fase 3: Desenvolvimento (BMAD)
- [ ] **Implementar**: Iniciar a codificação executando o fluxo BMAD de desenvolvimento:
    ```bash
    bmad-bmm-dev-story
    ```
- [ ] **Padrões de Arquitetura**: Codificar com foco em beans explícitos no backend e estruturas limpas no Flutter.

### Fase 4: Validação Rigorosa de Qualidade (MANDATÓRIO)
- [ ] **Backend (Java/Maven)**:
    ```bash
    cd backend && mvn clean compile && mvn test && cd ..
    ```
- [ ] **Frontend (Flutter)**:
    ```bash
    cd frontend && flutter pub get && flutter analyze && flutter test && cd ..
    ```
- [ ] **Commit Inicial**: Se as builds passarem sem erros/warnings, fazer o push inicial:
    ```bash
    git add .
    git commit -m "feat(<ID_DA_HISTORIA>): implementa funcionalidade base"
    git push origin HEAD
    ```

### Fase 5: Code Review & Ajustes
- [ ] **Code Review**: Executar o fluxo BMAD para inspecionar vulnerabilidades ou desvios arquiteturais:
    ```bash
    bmad-bmm-code-review
    ```
- [ ] **Ajustes**: Corrigir rigorosamente todas as observações encontradas pelo Code Review.
- [ ] **Revalidar**: Executar novamente os testes e lints locais (Fase 4) para garantir estabilidade.
- [ ] **Commit Review**: Enviar as correções de review para a branch remota:
    ```bash
    git add .
    git commit -m "refactor(<ID_DA_HISTORIA>): corrige apontamentos do code review"
    git push origin HEAD
    ```

### Fase 6: Automação & Cobertura de Testes
- [ ] **Adicionar Testes**: Expandir a suíte de testes automáticos executando:
    ```bash
    bmad-tea-testarch-automate
    ```
- [ ] **Revalidação Final**: Garantir que todos os testes antigos e novos continuam passando com sucesso.
- [ ] **Commit Testes**: Enviar os testes adicionais:
    ```bash
    git add .
    git commit -m "test(<ID_DA_HISTORIA>): expande cobertura de testes automatizados"
    git push origin HEAD
    ```

### Fase 7: Conclusão & Integração
- [ ] **Finalizar Sprint**: Mudar o status da história correspondente para `done` no arquivo `sprint-status.yaml`.
- [ ] **Pull Request**: Abrir o PR detalhando os testes rodados e decisões de implementação, e solicitar revisão humana.
