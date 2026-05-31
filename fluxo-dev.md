# 🚀 Fluxo de Desenvolvimento Graham Select (BMAD + AGENTS)

Este guia estabelece o processo de desenvolvimento rigoroso, eficiente e padronizado para a implementação de histórias no projeto **Graham Select**, alinhando os fluxos dos agentes BMAD com as diretrizes de integridade e qualidade do repositório (`AGENTS.md`).

---

## 📋 Pré-requisitos & Regras de Ouro

> [!IMPORTANT]
> **Antes de Iniciar qualquer Tarefa:**
> 1. **Sempre abra uma branch atualizada** a partir da branch base designada (geralmente `main`), garantindo que seu código esteja perfeitamente sincronizado com o repositório remoto.
> 2. **Sempre valide a implementação com testes e análise estática** locais antes de avançar de fase e antes de abrir o Pull Request (PR).
> 3. **Siga rigorosamente** todas as etapas descritas abaixo. Atalhos geram problemas de integração e falhas no pipeline.

---

## 🔄 Fluxo de Desenvolvimento Passo a Passo

### 📂 Fase 1: Setup & Preparação da Branch
1. Certifique-se de que a branch principal está atualizada:
   ```bash
   git checkout main && git pull
   ```
2. Crie uma branch específica para a funcionalidade/história:
   ```bash
   git checkout -b feature/[ID_DA_HISTORIA]-[NOME_BREVE]
   # Exemplo: git checkout -b feature/3.4-implementar-valuation-service
   ```

---

### 📝 Fase 2: Planejamento & Especificação
3. Execute o fluxo BMAD para criação e detalhamento da história:
   ```bash
   /create-story
   # Ou execute a ferramenta correspondente ao fluxo: bmad-bmm-create-story
   ```
4. **Verificação Crítica:** Antes de prosseguir, confirme se a história foi devidamente atualizada com status `in_progress` no arquivo `sprint-status.yaml`.

---

### 💻 Fase 3: Desenvolvimento & Implementação
5. Inicie o fluxo BMAD para desenvolvimento guiado da história:
   ```bash
   /dev-story
   # Ou execute a ferramenta correspondente ao fluxo: bmad-bmm-dev-story
   ```
6. Realize a codificação focando em soluções limpas, modulares e coerentes com a arquitetura definida.

---

### 🧪 Fase 4: Validação de Qualidade Local (Rigorosa)
7. Execute a validação estática e compilação do projeto para garantir a ausência de regressões:
   
   * **Backend (Java / Maven):**
     ```bash
     cd backend
     mvn clean compile     # Garante que compila sem erros
     mvn test              # Executa todos os testes unitários e de integração
     cd ..
     ```
   
   * **Frontend (Flutter):**
     ```bash
     cd frontend
     flutter pub get
     flutter analyze       # Validação estática obrigatória (0 erros/warnings recomendados)
     flutter test          # Executa todos os testes do aplicativo
     cd ..
     ```

8. Efetue o commit e envie as alterações locais para o repositório remoto:
   ```bash
   git add .
   git commit -m "feat([ID_DA_HISTORIA]): implementa funcionalidade base"
   git push origin HEAD
   ```

---

### 🔍 Fase 5: Revisão de Código (Code Review)
9. Submeta a implementação ao fluxo BMAD de revisão de código para identificar problemas ocultos ou otimizações:
   ```bash
   /code-review
   # Ou execute a ferramenta correspondente ao fluxo: bmad-bmm-code-review
   ```
10. **Ação Corretiva:** Corrija meticulosamente todos os *findings* apontados no Code Review.
11. **Revalidação:** Após as correções, repita a **Fase 4** (compilar, rodar testes e análise estática) para assegurar que as correções não quebraram nada.
12. Faça o push das correções:
    ```bash
    git add .
    git commit -m "refactor([ID_DA_HISTORIA]): corrige apontamentos do code review"
    git push origin HEAD
    ```

---

### 🤖 Fase 6: Automação & Cobertura de Testes
13. Execute o fluxo BMAD focado em expandir e consolidar testes automatizados:
    ```bash
    /testarch-automate
    # Ou execute a ferramenta correspondente ao fluxo: bmad-tea-testarch-automate
    ```
14. Rode novamente toda a suíte de testes locais (`mvn test` e `flutter test`) para assegurar 100% de estabilidade.
15. Envie as últimas atualizações de testes:
    ```bash
    git add .
    git commit -m "test([ID_DA_HISTORIA]): expande cobertura de testes automatizados"
    git push origin HEAD
    ```

---

### 🏁 Fase 7: Conclusão & Integração
16. Atualize o status da história para `done` no arquivo `sprint-status.yaml`.
17. Abra a Pull Request (PR) na plataforma de controle de versão (ex: GitHub).
    > [!TIP]
    > Certifique-se de que a descrição da PR contém um resumo claro do que foi implementado, os testes que foram executados com sucesso e referências ao ID da história.