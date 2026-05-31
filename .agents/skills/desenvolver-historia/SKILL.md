---
name: desenvolver-historia
description: Executa o fluxo de desenvolvimento de ponta a ponta de uma história do Graham Select (setup, planejar, dev, testar backend/frontend, code review e PR) baseando-se apenas no identificador ou nome da história fornecido.
---

# 🛠️ Skill: Desenvolver História (Graham Select)

Esta habilidade guia o Agente de IA na execução automatizada, rigorosa e padronizada do desenvolvimento de qualquer história no repositório **Graham Select**, alinhando as melhores práticas do BMAD às regras rígidas de integridade do repositório (`AGENTS.md`).

---

## 📥 Parâmetros de Entrada

A skill aceita e espera o parâmetro:
- **`ID_DA_HISTORIA`**: O identificador ou nome curto da história/tarefa (ex: `3.4`, `3.4-implementar-valuation-service`).

---

## 🎯 Instruções de Execução para o Agente de IA

Sempre que esta skill for acionada, você (o Agente) deve seguir rigorosamente as 7 fases programáticas abaixo de forma autônoma e proativa.

---

### 📂 Fase 1: Setup & Preparação da Branch
1. **Limpeza do Workspace**: Garanta que não há alterações pendentes que possam conflitar.
2. **Atualização da Base**:
   ```bash
   git checkout main && git pull
   ```
3. **Criação da Branch**: Deduza um nome curto e descritivo com base no `ID_DA_HISTORIA` e crie a branch correspondente:
   ```bash
   git checkout -b feature/[ID_DA_HISTORIA]-[DESCRICAO_CURTA]
   ```
   *Exemplo: `git checkout -b feature/3.4-implementar-valuation`*

---

### 📝 Fase 2: Planejamento & Especificação da História
4. **Criação da História**: Execute o fluxo BMAD para gerar o arquivo de especificação da história:
   ```bash
   # Use o workflow correspondente:
   bmad-bmm-create-story
   ```
5. **Atualização do Sprint**: Modifique o arquivo `sprint-status.yaml`, definindo o status da história como `in_progress`.

---

### 💻 Fase 3: Desenvolvimento da Funcionalidade
6. **Implementação Guiada**: Execute o fluxo BMAD de desenvolvimento:
   ```bash
   bmad-bmm-dev-story
   ```
7. Prossiga com a codificação necessária, mantendo os padrões arquiteturais de injeção de dependência e beans explícitos no backend e padrões adequados no Flutter.

---

### 🧪 Fase 4: Validação de Qualidade & Compilação Local (Obrigatório)
8. **Validação do Backend (Java/Maven)**:
   Navegue até a pasta do backend e execute a build completa com testes:
   ```bash
   cd backend && mvn clean compile && mvn test && cd ..
   ```
9. **Validação do Frontend (Flutter)**:
   Navegue até a pasta do frontend e execute a análise estática e os testes do app:
   ```bash
   cd frontend && flutter pub get && flutter analyze && flutter test && cd ..
   ```
   > [!WARNING]
   > Se qualquer uma das etapas falhar (erros de compilação, lints do analyze ou testes falhando), você **deve** corrigir os problemas antes de avançar para o Code Review.
10. **Commit Base**:
    ```bash
    git add .
    git commit -m "feat([ID_DA_HISTORIA]): implementa funcionalidade base"
    git push origin HEAD
    ```

---

### 🔍 Fase 5: Code Review & Ajustes Rigorosos
11. **Execução do Review**: Acione o fluxo BMAD de Code Review:
    ```bash
    bmad-bmm-code-review
    ```
12. **Correção de Apontamentos**: Leia os findings levantados pelo review e faça as correções diretamente no código.
13. **Revalidação de Testes**: Após ajustar, rode **novamente** toda a bateria de validação local do backend e frontend (Fase 4).
14. **Push das Correções**:
    ```bash
    git add .
    git commit -m "refactor([ID_DA_HISTORIA]): corrige apontamentos do code review"
    git push origin HEAD
    ```

---

### 🤖 Fase 6: Automação de Testes Adicionais
15. **Expansão de Testes**: Use o fluxo BMAD para adicionar testes de arquitetura e cobertura:
    ```bash
    bmad-tea-testarch-automate
    ```
16. Valide novamente o projeto localmente para assegurar 100% de estabilidade e suba as atualizações:
    ```bash
    git add .
    git commit -m "test([ID_DA_HISTORIA]): expande cobertura de testes automatizados"
    git push origin HEAD
    ```

---

### 🏁 Fase 7: Entrega & Fechamento
17. **Atualização da Sprint**: No arquivo `sprint-status.yaml`, atualize o status da história para `done`.
18. **Abertura do Pull Request**: Abra um PR detalhando o que foi feito, as decisões de design, os resultados das validações e referenciando a história.
