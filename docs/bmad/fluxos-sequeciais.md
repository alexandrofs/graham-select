Rode os fluxos abaixo em sequencia para a historia 3.3:

- bmad-bmm-create-story
- Antes de iniciar o proximo fluxo, verifique se a historia foi atualizada no sprint-status.yaml
- bmad-bmm-dev-story
- Antes de iniciar o fluxo de code review, garanta que as builds estao passando
- bmad-bmm-code-review
- Corrija os findings do code review
- Atualizar o sprint-status.yaml com o status de "done"
- bmad-tea-testarch-automate

Regras: 
- Siga todos os passos dos fluxos BMAD solicitados, rigorosamente.
- Garanta que as builds estejam passando antes de abrir a PR