# Persona
Carregue o agente 'bmad-agent-dev' para assumir a implementação do código seguindo rigorosamente as diretrizes e o fluxo abaixo.

# Objetivo
Desenvolver a história extraída da 'Descrição da tarefa', garantindo que o projeto compile e que 100% dos testes passem antes de realizar o commit e finalizar a execução.

# Dados de Entrada
- ID da Issue: {ISSUE_ID}
- Descrição da Tarefa:
{TASK_DESCRIPTION}

---

# Validações Pré-Execução (Condições de Parada / HALT)
Antes de executar qualquer desenvolvimento:
1. Extraia a `STORY_KEY` da descrição da tarefa (utilizando subagente se necessário).
2. Carregue os comentários da issue no GitHub.
3. Crie uma branch para desenvolvimento baseada na `STORY_KEY` (ex: `feature/{STORY_KEY}-descricao`).
4. Verifique o status da história:
   - Se **não for possível extrair a `STORY_KEY`** OU se o status for diferente de `'Ready For Development'` e `'In Progress'`:
     - Faça um comentário na issue no GitHub explicando exatamente o motivo da impossibilidade de execução.
     - Responda apenas com a palavra/chave **DESENVOLVIMENTO_BLOQUEADO**.
     - **Interrompa a execução imediatamente (HALT).**

---

# Passo a Passo de Execução

1. **Identificação:** Com a `STORY_KEY` validada, execute o fluxo `DS (DEV Story)` passando a chave como parâmetro.
2. **Ciclo de Verificação de Status:**
   - Verifique o status atual da história após a execução:
     - Se o status for `done`, altere-o para `review`.
     - Se o status for `review`, avance diretamente para o **Passo 3**.
     - Se o status for diferente de `review` ou `done`, repita o fluxo `DS` (limite máximo de **2 tentativas**). Se persistir, encerre a execução, comente na issue o erro e responda **BLOQUEADO**.
3. **Validação de Build e Testes Locais:**
   - Execute o build do projeto e rode toda a suíte de testes locais.
   - Certifique-se de que não há regressões e que todos os critérios de aceite estão cobertos.
4. **Commit e push:**
   - Realize o commit das alterações seguindo o padrão Conventional Commits e referenciando a `{STORY_KEY}` (ex: `feat({STORY_KEY}): implementa critérios de aceite`).
   - Realize um push para o repositorio remoto
5. **Finalização e Feedback:**
   - Se houver observações relevantes, débitos técnicos ou detalhes de implementação, adicione um comentário descritivo na issue correspondente no GitHub.

---

# Diretrizes Técnicas de Execução
1. **Desenvolvimento Guiado por BDD e TDD:** Siga o ciclo estrito Red-Green-Refactor (crie os testes que falham, implemente o código mínimo para passar e refatore).
2. **Cobertura de Critérios de Aceite:** Crie/atualize testes unitários e de integração para 100% dos critérios de aceite da história.
3. **Respeito à Arquitetura:** Mantenha os padrões arquiteturais, convenções de código do repositório, tipagem estática e guardrails existentes.
4. **Isolamento de PR:** Não abra Pull Request; a tarefa passará por auditoria interna antes do envio externo.