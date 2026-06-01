# Regras para AI Agents

## Desenvolvimento de Histórias
- **Sempre abra uma branch atualizada antes de implementar uma história.**
  (Exemplo: `git checkout -b feature/nome-da-historia main` ou a partir da branch base designada, garantindo que o código esteja sincronizado).
- **Sempre valide a implementação com testes e análise estática.**
  (Após implementar uma história ou aplicar correções de review, deve-se rodar todos os testes de backend e frontend, além do `flutter analyze`, para garantir a integridade do projeto).
- **Sempre adicione logs em todas as entradas de APIs, consumo de tópicos e logs de nível ERROR nos lançamentos e capturas de exceções.**
  (Essencial para garantir a rastreabilidade e observabilidade do sistema. Toda entrada de requisição de API ou processamento de mensagens de tópicos/filas deve ser logada com suas informações contextuais relevantes. Exceções devem ser capturadas e logadas adequadamente com o stack trace correspondente no nível ERROR).

