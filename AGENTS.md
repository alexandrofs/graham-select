# Regras para AI Agents

## Desenvolvimento de Histórias
- **Sempre abra uma branch atualizada antes de implementar uma história.**
  (Exemplo: `git checkout -b feature/nome-da-historia main` ou a partir da branch base designada, garantindo que o código esteja sincronizado).
- **Sempre valide a implementação com testes e análise estática.**
  (Após implementar uma história ou aplicar correções de review, deve-se rodar todos os testes de backend e frontend, além do `flutter analyze`, para garantir a integridade do projeto).
- **Sempre adicione logs em todas as entradas de APIs, consumo de tópicos e logs de nível ERROR nos lançamentos e capturas de exceções.**
  (Essencial para garantir a rastreabilidade e observabilidade do sistema. Toda entrada de requisição de API ou processamento de mensagens de tópicos/filas deve ser logada com suas informações contextuais relevantes. Exceções devem ser capturadas e logadas adequadamente com o stack trace correspondente no nível ERROR).
- **Imutabilidade de Migrations Liquibase:**
  (É estritamente proibido alterar retroativamente arquivos de migração Liquibase já aplicados. Quaisquer correções ou alterações em estruturas de dados existentes devem ser feitas exclusivamente via novas migrations incrementais e idempotentes, aplicando `preConditions` ou `validCheckSum` quando pertinente).
- **Conformidade LGPD e Expurgo de Dados Pessoais (Direito ao Esquecimento / FR5):**
  (Sempre que uma nova entidade ou tabela armazenar dados atrelados ao usuário ou identificadores pessoais/PII — como telefones, emails, credenciais ou históricos —, o adaptador de expurgo de dados (`UserDataPurgeAdapter`) DEVE ser atualizado e coberto por testes no `AccountDeletionIT`, garantindo que o fluxo de deleção de conta remova integralmente todos os registros associados ao usuário).
- **Mapeamento de Configurações de Produção no Helm e CI/CD:**
  (Sempre que novas variáveis de ambiente ou propriedades forem adicionadas em `application.yml` ou consumidas pelo frontend/backend, elas DEVEM ser obrigatoriamente refletidas no chart Helm (`helm/graham-select/values.yaml`, `configmap.yaml` e/ou `secret.yaml`) e na pipeline de implantação (`.github/workflows/deploy.yml`), assegurando paridade entre desenvolvimento e produção).
- **Abertura Automática de Pull Requests (PR):**
  (Quando o usuário solicitar a abertura de um PR, como "abra um pr" ou "crie um pr", o agente DEVE criar o Pull Request de forma 100% autônoma via API do GitHub, sem transferir o trabalho para o usuário nem se limitar a enviar link web. Se a CLI `gh` não estiver no PATH, obtenha o token do Git Credential Manager com `"protocol=https`nhost=github.com`n`n" | git credential fill` e dispare a requisição `POST https://api.github.com/repos/{owner}/{repo}/pulls` com cabeçalho de autenticação Bearer e corpo formatado em UTF-8).

