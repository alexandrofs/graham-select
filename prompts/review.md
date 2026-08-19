# Persona
Carregue o agente 'bmad-agent-dev' para assumir a revisão do código (Code Review) seguindo rigorosamente as diretrizes e o fluxo abaixo.

# Objetivo
Revisar a história extraída da 'Descrição da tarefa', aplicar os patches necessários, garantir que o projeto compile e que 100% dos testes passem antes de enviar as alterações e abrir o Pull Request.

# Dados de Entrada
- ID da Issue: {ISSUE_ID}
- Descrição da Tarefa:
{TASK_DESCRIPTION}

---

# Validações Pré-Execução (Condições de Parada / HALT)
Antes de executar qualquer ação de revisão:
1. Extraia a `STORY_KEY` da descrição da tarefa (utilizando subagente se necessário).
2. Verifique o status da história:
   - Se **não for possível extrair a `STORY_KEY`** OU se o status for diferente de `'review'` / `'In Review'`:
     - Faça um comentário na issue no GitHub explicando que a história não está pronta para Code Review.
     - Responda apenas com a palavra **BLOQUEADO**.
     - **Interrompa a execução imediatamente (HALT).**

---

# Passo a Passo de Execução

1. **Execução do Review:** Com a `STORY_KEY` validada, execute o fluxo `CR (Code Review)` passando a chave como parâmetro.
2. **Ciclo de Verificação de Status:**
   - Verifique o status atual da história após a execução do fluxo:
     - Se o status for `done`, avance diretamente para o **Passo 3**.
     - Se o status for diferente de `done`, repita o fluxo `CR` (limite máximo de **2 tentativas**). Se persistir, encerre a execução, comente o erro na issue e responda **BLOQUEADO**.
3. **Validação de Build e Testes Locais:**
   - Execute o build do projeto e rode toda a suíte de testes locais.
   - Certifique-se de que não há quebras, regressões e que todos os critérios de aceite e padrões arquiteturais estão atendidos.
4. **Commit Condicional e Push:**
   - Verifique se houve aplicação de patches ou modificação de arquivos:
     - Se houver alterações: realize o commit seguindo Conventional Commits (ex: `review({STORY_KEY}): aplica correcoes de auditoria e code review`) e faça o `git push` para a branch correspondente.
     - Se não houver alterações (código 100% aprovado sem patches): apenas garanta que a branch local está sincronizada remotamente.
5. **Abertura de Pull Request:**
   - Abra o Pull Request (caso ainda não exista) com as seguintes informações:
     - **Título:** `review({STORY_KEY}): {TASK_TITLE ou STORY_KEY}`
     - **Descrição:** `{TASK_DESCRIPTION}`
6. **Finalização e Feedback:**
   - Comente na issue do GitHub resumindo os achados da auditoria, as correções aplicadas e o link do Pull Request gerado.
   - Mover a issue para o status 'For Review'

---

# Diretrizes Técnicas de Execução
1. **Execução Totalmente Autônoma:** Conduza a revisão de ponta a ponta sem interrupções interativas ou pausas desnecessárias.
2. **Auditoria de Critérios de Aceite e Arquitetura:** Verifique rigorosamente o código contra os critérios de aceite da história, padrões de arquitetura (AD-4, AD-6, AD-8, AD-9) e qualidade técnica.
3. **Aplicação Obrigatória de Patches:** Aplique imediatamente todos os patches (`patch`) para sanar falhas, inconsistências e vulnerabilidades encontradas durante a auditoria.
4. **Política Zero Deferred Work:** É estritamente proibido classificar achados como `defer` ou postergar correções. Todos os problemas identificados devem ser corrigidos na própria sessão de review.
5. **Validação de Testes e Regressões:** Execute a suíte de testes locais após cada correção para assegurar que 100% dos testes passam sem novas regressões.