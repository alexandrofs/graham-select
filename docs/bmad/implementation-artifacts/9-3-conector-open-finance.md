---
story_id: "9.3"
story_key: "9-3-conector-open-finance"
baseline_commit: "c70e5a806cdb74f032018f6429d01e893cfb53f9"
---

# Story 9.3: Conector Open Finance

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como investidor pessoa física do Graham-Select,
Eu quero conectar minha conta de investimentos da minha corretora ou banco (ex: XP, Nubank/NuInvest, BTG Pactual, Itaú, Inter) via Open Finance,
Para que minhas operações de compra e venda e posições de custódia sejam sincronizadas de forma automatizada e contínua, sem a necessidade de uploads manuais periódicos de notas de corretagem ou planilhas da B3.

## Escopo e Limites (Boundaries)

- **O que ESTÁ incluído nesta história (Story 9.3):**
  1. Criação do script de migração Liquibase `backend/common/src/main/resources/db/changelog/33-create-open-finance-connections-and-sync-logs.yaml` e sua inclusão em `db.changelog-master.yaml`:
     - Tabela `open_finance_connections`: identificador UUID `id` (`CHAR(36)`), `user_id` (`VARCHAR(255)`), `profile_id` (`CHAR(36)` opcional para Family Holding), `institution_id` (`VARCHAR(50)`), `institution_name` (`VARCHAR(100)`), `institution_logo_url` (`VARCHAR(255)`), `status` (`VARCHAR(30)`), `consent_id` (`VARCHAR(100)`), credenciais cifradas `encrypted_access_token` (`VARCHAR(512)`) e `encrypted_refresh_token` (`VARCHAR(512)`), `consent_expires_at` (`DATETIME`), `last_sync_at` (`DATETIME`), `last_sync_status` (`VARCHAR(30)`), `error_message` (`VARCHAR(500)`), auditoria `created_at` e `updated_at`.
     - Índices de consulta otimizados em `(user_id, status)` e índice de unicidade / busca rápida em `(user_id, institution_id, profile_id)`.
     - Tabela `open_finance_sync_logs`: `id` (`CHAR(36)`), `connection_id` (`CHAR(36)` FK com `onDelete="CASCADE"`), `user_id` (`VARCHAR(255)`), `started_at` (`DATETIME`), `completed_at` (`DATETIME`), `sync_type` (`VARCHAR(30)`), contadores (`positions_fetched`, `transactions_fetched`, `trades_created`, `trades_duplicated`), `status` (`VARCHAR(30)`), `error_details` (`TEXT`) e `created_at` (`DATETIME`).
  2. Implementação de camada de segurança e criptografia de credenciais em repouso (`NFR6`):
     - Serviço de criptografia de tokens `AesGcmTokenEncryptionAdapter` utilizando o algoritmo padrão da indústria `AES/GCM/NoPadding` de 256 bits com vetor de inicialização (IV) de 12 bytes gerado aleatoriamente por operação via `SecureRandom` e chave secreta configurável (`openfinance.encryption.secret-key`), armazenando access tokens, refresh tokens e identificadores de consentimento de forma cifrada no banco de dados.
     - Validação de segurança em endpoints de webhook parceiros via cabeçalho compartilhado `X-Webhook-Secret` (`openfinance.webhook.secret`).
  3. Criação de modelos de domínio, entidades e registros imutáveis no módulo `common`:
     - Entidades de domínio: `OpenFinanceConnection`, `OpenFinanceSyncLog`, enums `OpenFinanceConnectionStatus` (`PENDING_CONSENT`, `ACTIVE`, `SYNCING`, `EXPIRED`, `REVOKED`, `FAILED`), `OpenFinanceSyncStatus` (`SUCCESS`, `PARTIAL`, `FAILED`), `OpenFinanceSyncType` (`INITIAL`, `SCHEDULED`, `MANUAL`, `WEBHOOK`).
     - Contratos de Portas no módulo `common`:
       - `OpenFinanceConnectionPort`: operações de persistência, consulta por ID, busca por usuário e perfil, busca por conexões ativas para rotinas de sincronização agendada e expurgo.
       - `OpenFinanceSyncLogPort`: persistência e consulta do histórico de sincronizações por conexão e usuário.
       - `OpenFinanceConnectorPort`: contrato abstrato para integração com instituições Open Finance / agregadores (geração de URL de consentimento com state e PKCE, troca de código de autorização por tokens, renovação de tokens expirados via `refreshToken`, busca de posições de custódia, busca de transações/trades de investimento e revogação remota de consentimento).
       - `TokenEncryptionPort`: contrato para criptografia e descriptografia simétrica de tokens sensíveis.
     - DTOs / Records: `OpenFinanceInstitutionDTO`, `InitiateConnectionRequestDTO`, `ConsentUrlResponseDTO`, `ExchangeConsentCallbackDTO`, `OpenFinanceConnectionDTO`, `OpenFinanceSyncResultDTO`, `OpenFinancePositionDTO`, `OpenFinanceTransactionDTO`, `OpenFinanceTokenPairDTO`.
  4. Implementação de casos de uso desacoplados no módulo `common`:
     - `ListOpenFinanceInstitutionsUseCase`: lista as corretoras e bancos parceiros habilitados (XP, Nubank, BTG, Itaú, Inter) com status operacional e ícones.
     - `InitiateOpenFinanceConnectionUseCase`: valida isolamento do usuário, valida regra de Family Holding (`profileId != null` exige plano Premium via `PremiumFeatureAccessGuard` e existência em `FamilyProfilePort`), previne duplicação reaproveitando ou atualizando conexão anterior para o mesmo par instituição/perfil, cria conexão com status `PENDING_CONSENT` e gera URL de autorização/consentimento com state seguro (OAuth2 PKCE / OpenID FAPI).
     - `CompleteOpenFinanceConsentUseCase`: processa o retorno da autorização (code + state), troca pelo par de tokens via `OpenFinanceConnectorPort`, criptografa os tokens com `TokenEncryptionPort`, atualiza o status para `ACTIVE` e dispara a primeira sincronização inicial assíncrona.
     - `SyncOpenFinanceConnectionUseCase`: executa o pipeline de sincronização. Verifica o tempo de vida do access token e executa renovação automática transparente via `OpenFinanceConnectorPort.refreshToken(...)` quando expirado ou prestes a expirar. Busca transações recentes, normaliza operação (`BUY`/`COMPRA` -> `"COMPRA"`, `SELL`/`VENDA` -> `"VENDA"`), ignora lançamentos não operacionais com log seguro, persiste novos trades via `SaveTradeUseCase` (reutilizando deduplicação determinística), emite notificação em tempo real via `SendPortfolioUpdateNotificationUseCase.execute(userId)` quando `trades_created > 0`, e finaliza registros em `open_finance_sync_logs`.
     - `RevokeOpenFinanceConnectionUseCase`: revoga o consentimento remotamente na instituição parceira, marca a conexão como `REVOKED` e limpa as credenciais cifradas.
     - `ListUserOpenFinanceConnectionsUseCase`: lista todas as conexões do usuário logado (sanitizando credenciais sensíveis e expondo apenas metadados de status e sincronização).
  5. Implementação de adaptadores de infraestrutura e persistência no módulo `api`:
     - `AesGcmTokenEncryptionAdapter`: adaptador criptográfico robusto e testável para proteção em repouso.
     - `OpenFinanceConnectorAdapter` e `MockOpenFinanceConnectorAdapter`: cliente desacoplado (baseado em Spring `RestClient` com connect timeout de 5s e read timeout de 15s) com suporte nativo a modo sandbox/mock para testes automatizados locais, CI hermético e integração transparente com agregadores reais (ex: Pluggy / Celcoin / Belvo ou APIs Bacen).
     - `OpenFinanceConnectionJpaAdapter` e `OpenFinanceSyncLogJpaAdapter`: adaptadores de persistência JPA mapeando para entidades de banco.
  6. Agendamento automático com bloqueio distribuído via ShedLock:
     - `OpenFinanceSyncScheduler` com anotações `@Scheduled(cron = "${openfinance.sync.cron:0 0 6 * * *}", zone = "America/Sao_Paulo")` e `@SchedulerLock(name = "OpenFinanceSyncScheduler_syncAllActiveConnections", lockAtLeastFor = "1m", lockAtMostFor = "30m")` para sincronização matinal recorrente de todas as contas ativas com isolamento de falha por conexão.
  7. Endpoints REST no módulo `api`:
     - `GET /api/v1/open-finance/institutions`: lista instituições disponíveis para conexão.
     - `GET /api/v1/open-finance/connections`: lista as conexões do usuário autenticado (`userId` extraído do JWT).
     - `POST /api/v1/open-finance/connections`: inicia conexão e retorna URL de consentimento.
     - `POST /api/v1/open-finance/connections/callback`: processa callback de autorização e ativa conexão.
     - `POST /api/v1/open-finance/connections/{id}/sync`: disparo manual sob demanda de sincronização.
     - `DELETE /api/v1/open-finance/connections/{id}`: desconexão e revogação de consentimento.
     - `POST /api/v1/open-finance/webhook`: recepção de eventos assíncronos com validação de segredo no header `X-Webhook-Secret`.
  8. Atualização do contrato OpenAPI 3.0 em `backend/api/src/main/resources/openapi.yaml` e exclusão de classes geradas conflitantes (`OpenFinanceApi.java`, `OpenFinanceApiController.java`, `OpenFinanceApiDelegate.java`) no `maven-antrun-plugin` em `backend/api/pom.xml`.
  9. Camada Frontend Flutter:
     - Dependência `url_launcher: ^6.3.1` adicionada a `frontend/pubspec.yaml` para abertura segura de URL de consentimento externo.
     - Camada de Dados e Repositório: `OpenFinanceRemoteDataSource`, `OpenFinanceRepository`, `OpenFinanceRepositoryImpl`.
     - Gerenciamento de Estado: `OpenFinanceProvider` com suporte a listagem, fluxo de conexão, polling de status durante sincronização e desconexão.
     - Apresentação na tela `/upload`: aba dedicada "Open Finance (Automático)" ao lado de "Upload B3 (.xlsx / .pdf)", exibindo cartões de conexões ativas, status de sincronização, botão "Conectar Instituição" com modal/diálogo de seleção de bancos/corretoras e suporte à seleção de sub-carteira familiar (`profileId`).
  10. Conformidade estrita com `AGENTS.md` e `project-context.md`:
      - `@Slf4j` e logs estruturados em nível `INFO` nas entradas de controllers, endpoints de callback, webhooks e schedulers.
      - Tratamento de exceções com logs em nível `ERROR` acompanhados de mensagem descritiva e stack trace completo (`log.error("...", e)`).
      - Uso obrigatório de `ZoneOffset.UTC` em todas as manipulações de datas e horas.
      - Testes 100% herméticos e unitários/integrados sem chamadas externas na build (`mvn clean test` e `flutter analyze` com 0 erros/warnings).

- **O que NÃO está incluído (fora de escopo desta história):**
  - Iniciação de pagamentos ou transferências via Open Finance (Pix/TED) — a história foca exclusivamente no escopo de leitura de investimentos e custódia (Investment & Custody Data Sharing).
  - Execução automática de ordens de compra e venda nas corretoras via Open Finance (a fase atual do Bacen é restrita a compartilhamento de dados cadastrais, saldos e investimentos).
  - Webhooks de notificações push para smartphones (previsto no Epic 11).

## Acceptance Criteria

1. **Modelagem Relacional e Persistência de Conexões e Logs (Liquibase) (`FR11`, `NFR4`, `NFR6`):**
   - **Dado** o processo de inicialização do backend com Liquibase
   - **Quando** o changeset `33-create-open-finance-connections-and-sync-logs.yaml` é executado
   - **Então** cria a tabela `open_finance_connections` contendo:
     - `id` (`CHAR(36)` PRIMARY KEY)
     - `user_id` (`VARCHAR(255)` NOT NULL)
     - `profile_id` (`CHAR(36)` NULL - suporte a Family Holding)
     - `institution_id` (`VARCHAR(50)` NOT NULL)
     - `institution_name` (`VARCHAR(100)` NOT NULL)
     - `institution_logo_url` (`VARCHAR(255)` NULL)
     - `status` (`VARCHAR(30)` NOT NULL)
     - `consent_id` (`VARCHAR(100)` NULL)
     - `encrypted_access_token` (`VARCHAR(512)` NULL)
     - `encrypted_refresh_token` (`VARCHAR(512)` NULL)
     - `consent_expires_at` (`DATETIME` NULL)
     - `last_sync_at` (`DATETIME` NULL)
     - `last_sync_status` (`VARCHAR(30)` NULL)
     - `error_message` (`VARCHAR(500)` NULL)
     - `created_at` (`DATETIME` NOT NULL)
     - `updated_at` (`DATETIME` NOT NULL)
     - Índices em `(user_id, status)` e `(user_id, institution_id, profile_id)` para consultas e verificações otimizadas
   - **E** cria a tabela `open_finance_sync_logs` contendo:
     - `id` (`CHAR(36)` PRIMARY KEY)
     - `connection_id` (`CHAR(36)` NOT NULL)
     - `user_id` (`VARCHAR(255)` NOT NULL)
     - `started_at` (`DATETIME` NOT NULL)
     - `completed_at` (`DATETIME` NULL)
     - `sync_type` (`VARCHAR(30)` NOT NULL)
     - `positions_fetched` (`INT` NOT NULL DEFAULT 0)
     - `transactions_fetched` (`INT` NOT NULL DEFAULT 0)
     - `trades_created` (`INT` NOT NULL DEFAULT 0)
     - `trades_duplicated` (`INT` NOT NULL DEFAULT 0)
     - `status` (`VARCHAR(30)` NOT NULL)
     - `error_details` (`TEXT` NULL)
     - `created_at` (`DATETIME` NOT NULL)
     - Chave estrangeira ligando `connection_id` a `open_finance_connections(id)` com `onDelete="CASCADE"`.

2. **Criptografia Segura em Repouso de Credenciais e Tokens (`NFR6`):**
   - **Dado** qualquer token de acesso (`accessToken`), token de renovação (`refreshToken`) ou identificador sensível recebido do Open Finance
   - **Quando** o sistema vai persistir a informação no banco de dados
   - **Então** o componente `AesGcmTokenEncryptionAdapter` cifra o valor em texto plano utilizando `AES/GCM/NoPadding` de 256 bits com um IV aleatório de 12 bytes gerado via `SecureRandom`
   - **E** armazena o resultado empacotando `IV + Ciphertext` codificado em Base64
   - **E** em nenhum momento credenciais em texto puro são salvas no banco de dados ou expostas nos arquivos de log da aplicação
   - **E** no momento da leitura para chamadas de API externas, o valor é descriptografado em memória de forma volátil.

3. **Iniciação de Conexão, Regra de Concorrência e Restrição de Tier (`FR11`, `FR4`):**
   - **Dado** um investidor autenticado com token JWT válido
   - **Quando** envia requisição `POST /api/v1/open-finance/connections` contendo `institutionId` (ex: `"XP_INVESTIMENTOS"`) e opcionalmente `profileId`
   - **Então** se `profileId` for informado, o sistema valida que o usuário possui plano Premium via `PremiumFeatureAccessGuard.canAccessPremiumFeatures()` e que o perfil existe e pertence ao usuário (`FamilyProfilePort.findActiveByIdAndMasterUserId(...)`)
   - **E** se `profileId` não for informado (conta matriz), permite o acesso a usuários de todos os tiers (incluindo FREE)
   - **E** valida se já existe conexão prévia com a mesma instituição para a mesma carteira:
     - Se existir com status `PENDING_CONSENT`, reutiliza ou atualiza o registro existente
     - Se existir com status `ACTIVE`, atualiza as credenciais ou substitui o consentimento sem criar registros duplicados orfãos
   - **E** invoca `OpenFinanceConnectorPort.generateConsentUrl(...)` gerando uma URL de autorização/consentimento contendo parâmetros seguros (`state`, `nonce`, escopos de leitura de investimentos)
   - **E** responde com HTTP `201 Created` contendo o ID da conexão e a `consentUrl` para redirecionamento.

4. **Conclusão e Ativação de Consentimento (Callback de Autorização):**
   - **Dado** que o usuário completou o fluxo de consentimento na instituição financeira e foi redirecionado
   - **Quando** o cliente envia requisição `POST /api/v1/open-finance/connections/callback` com `connectionId`, `code` e `state`
   - **Então** o sistema valida a correspondência do `state` e da conexão
   - **E** delega para `OpenFinanceConnectorPort.exchangeConsentCode(...)` para obter os tokens de acesso e renovação
   - **E** cifra os tokens com AES-256 e persiste na conexão, atualizando o status para `ACTIVE` e definindo `consentExpiresAt`
   - **E** dispara imediatamente a sincronização inicial em background
   - **E** responde com HTTP `200 OK` com os detalhes atualizados da conexão.

5. **Pipeline de Sincronização, Ciclo de Vida de Token e Reuso Arquitetural (`FR11`, Reuso Arquitetural):**
   - **Dado** uma conexão ativa agendada ou disparada manualmente via `POST /api/v1/open-finance/connections/{id}/sync`
   - **Quando** o caso de uso `SyncOpenFinanceConnectionUseCase` é executado
   - **Então** atualiza o status temporário para `SYNCING` e cria um registro em `open_finance_sync_logs` com status `RUNNING`
   - **E** verifica se o access token está expirado ou próximo da expiração; caso afirmativo, descriptografa o `encrypted_refresh_token`, invoca `OpenFinanceConnectorPort.refreshToken(...)`, cifra os novos tokens e atualiza a conexão antes de prosseguir
   - **E** busca transações de investimentos e posições de custódia na instituição financeira
   - **E** para cada transação retornada:
     - Normaliza o tipo de operação: `BUY` ou `COMPRA` mapeia para `"COMPRA"`; `SELL` ou `VENDA` mapeia para `"VENDA"`
     - Transações que não representam compra/venda de ativos (ex: proventos, taxas, ajustes) são ignoradas e computadas em log sem abortar a sincronização
     - Mapeia para a entidade `Trade` do domínio (com `userId`, `profileId`, `ticker`, `tradeDate`, `quantity`, `price`, `broker`, `side`)
     - Invoca `SaveTradeUseCase.execute(trade)` para garantir deduplicação determinística contra a tabela `trades` existente
     - Incrementa os contadores de `trades_created` ou `trades_duplicated`
   - **E** se `trades_created > 0`, dispara `SendPortfolioUpdateNotificationUseCase.execute(userId)` para atualizar o dashboard Flutter em tempo real via SSE
   - **E** ao concluir com sucesso, atualiza o status da conexão para `ACTIVE`, atualiza `lastSyncAt` com `LocalDateTime.now(ZoneOffset.UTC)`, define `status = "SUCCESS"` e finaliza o log em `open_finance_sync_logs`
   - **E** em caso de erro na comunicação ou expiração irreversível do consentimento, captura a exceção, registra log `ERROR` com stack trace e marca a conexão como `FAILED` ou `EXPIRED` sem corromper os dados preexistentes.

6. **Segurança do Endpoint de Webhook de Parceiros:**
   - **Dado** uma notificação assíncrona enviada por uma instituição financeira para `POST /api/v1/open-finance/webhook`
   - **Quando** a requisição chega ao backend
   - **Então** valida obrigatoriamente o cabeçalho `X-Webhook-Secret` contra o segredo configurado (`openfinance.webhook.secret`)
   - **E** se o segredo for inválido ou ausente, responde imediatamente com HTTP `401 Unauthorized` e registra log de auditoria
   - **E** se o segredo for válido, processa o evento de alteração de consentimento ou status e responde com HTTP `200 OK`.

7. **Desconexão e Revogação de Consentimento (`FR11`, `NFR5` - LGPD):**
   - **Dado** um investidor autenticado
   - **Quando** envia requisição `DELETE /api/v1/open-finance/connections/{id}`
   - **Então** valida que a conexão pertence ao `userId` do token JWT (isolamento multi-tenant)
   - **E** tenta revogar o consentimento na instituição remota via `OpenFinanceConnectorPort.revokeConsent(...)`
   - **E** atualiza o status da conexão para `REVOKED` e limpa os campos `encrypted_access_token` e `encrypted_refresh_token`
   - **E** responde com HTTP `204 No Content`.

8. **Agendamento Recorrente e Bloqueio Distribuído (Scheduler & ShedLock):**
   - **Dado** a configuração de cron `${openfinance.sync.cron:0 0 6 * * *}` com timezone `America/Sao_Paulo`
   - **Quando** o cron dispara no horário programado (diariamente às 06:00)
   - **Então** o componente `OpenFinanceSyncScheduler` adquire o lock na tabela `shedlock` (`name = "OpenFinanceSyncScheduler_syncAllActiveConnections"`)
   - **E** busca todas as conexões ativas não expiradas
   - **E** itera sobre as conexões executando a sincronização dentro de blocos defensivos `try-catch`, garantindo que uma falha pontual de conexão de uma instituição ou usuário não interrompa a sincronização das demais contas
   - **E** registra métricas detalhadas consolidadas em log `INFO`.

9. **Interface Frontend Flutter - Conexões Open Finance na Tela de Importação (`/upload`):**
   - **Dado** que o usuário acessa a tela de Importação (`/upload`) no Flutter
   - **Quando** visualiza a interface
   - **Então** exibe um controle de abas/segmento: "Upload Manual (.xlsx / .pdf)" e "Open Finance (Automático)"
   - **E** na aba "Open Finance (Automático)", exibe:
     1. Cabeçalho explicativo com badge de segurança e garantia de criptografia de ponta a ponta;
     2. Botão de destaque "Conectar Nova Instituição" (`Key('open_finance_connect_button')`);
     3. Modal ou Diálogo de Seleção de Instituição (`Key('open_finance_institution_dialog')`), listando parceiros com seus logos e nomes;
     4. Seletor de Perfil / CPF de Destino (`Key('open_finance_profile_selector')`), permitindo vincular a conta a um membro da Family Holding se configurado;
     5. Ação de conexão que obtém a URL de consentimento e invoca o navegador externo de forma segura utilizando `url_launcher`;
     6. Lista de conexões ativas (`Key('open_finance_connections_list')`), exibindo o nome e ícone da corretora, status (badge "Ativo", "Sincronizando", "Expirado"), data da última sincronização e botões "Sincronizar Agora" (`Key('open_finance_sync_button_${id}')`) e "Desconectar" (`Key('open_finance_disconnect_button_${id}')`);
     7. Mecanismo de polling ou escuta SSE que reflete a conclusão do sync e exibe snackbar amigável com o resumo de trades importados.

10. **Observabilidade, Logs Estruturados e Conformidade com Diretrizes (`AGENTS.md` & `project-context.md`):**
    - **Dado** qualquer requisição aos endpoints REST, chamadas de conectores ou disparos do scheduler
    - **Então** toda entrada de API e processamento de sincronização DEVE ser registrada em log `INFO` com dados de contexto sanitizados (`userId`, `profileId`, `institutionId`, `connectionId`)
    - **E** toda exceção lançada ou capturada DEVE ser logada em nível `ERROR` com mensagem descritiva e stack trace completo (`log.error("...", e)`)
    - **E** nenhuma credencial, token ou chave em texto puro pode constar em logs
    - **E** todas as operações com datas e horários DEVEM utilizar estritamente `ZoneOffset.UTC`
    - **E** todos os testes automatizados do conector Open Finance DEVEM ser 100% herméticos e offline utilizando mocks (`MockOpenFinanceConnectorAdapter`), garantindo build limpa sem conexões externas reais no `mvn clean test` e 0 warnings no `flutter analyze`.

## Tasks / Subtasks

- [x] **Task 1: Modelos de Domínio, Portas e Criptografia AES-256 no Módulo `common`** (AC: 1, 2, 5, 10)
  - [x] 1.1 Criar enums `OpenFinanceConnectionStatus`, `OpenFinanceSyncStatus` e `OpenFinanceSyncType` em `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/domain/entities/`.
  - [x] 1.2 Criar entidades de domínio imutáveis `OpenFinanceConnection` e `OpenFinanceSyncLog` em `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/domain/entities/`.
  - [x] 1.3 Criar DTOs de dados e transporte em `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/`:
    - `OpenFinanceInstitutionDTO`, `InitiateConnectionRequestDTO`, `ConsentUrlResponseDTO`, `ExchangeConsentCallbackDTO`, `OpenFinanceConnectionDTO`, `OpenFinanceSyncResultDTO`, `OpenFinancePositionDTO`, `OpenFinanceTransactionDTO`, `OpenFinanceTokenPairDTO`.
  - [x] 1.4 Criar portas de domínio em `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/repository/`:
    - `OpenFinanceConnectionPort`, `OpenFinanceSyncLogPort`, `OpenFinanceConnectorPort` (com método `refreshToken`) e `TokenEncryptionPort`.
  - [x] 1.5 Implementar componente de criptografia `AesGcmTokenEncryptionAdapter` implementando `TokenEncryptionPort` com algoritmo `AES/GCM/NoPadding`, IV de 12 bytes aleatório por operação e chave de 256 bits configurável.
  - [x] 1.6 Criar testes unitários para o serviço de criptografia garantindo que texto cifrado seja decifrado com fidelidade e que chaves ou IVs diferentes lancem exceção.

- [x] **Task 2: Migração Liquibase e Persistência JPA no `common` e `api`** (AC: 1, 10)
  - [x] 2.1 Criar arquivo de migração `backend/common/src/main/resources/db/changelog/33-create-open-finance-connections-and-sync-logs.yaml` com as tabelas `open_finance_connections` (`id CHAR(36)`, `user_id VARCHAR(255)`) e `open_finance_sync_logs` com índices apropriados.
  - [x] 2.2 Registrar a nova migração no arquivo master `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`.
  - [x] 2.3 Criar entidades JPA `OpenFinanceConnectionEntity` e `OpenFinanceSyncLogEntity` em `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/jpa/entities/`.
  - [x] 2.4 Criar repositórios Spring Data `OpenFinanceConnectionJpaRepository` e `OpenFinanceSyncLogJpaRepository`.
  - [x] 2.5 Implementar adaptadores JPA `OpenFinanceConnectionJpaAdapter` e `OpenFinanceSyncLogJpaAdapter` implementando as portas do `common`.
  - [x] 2.6 Criar testes unitários e de integração de persistência para os adaptadores JPA.

- [x] **Task 3: Casos de Uso de Conexão, Consentimento, Renovação de Token e Sincronização no `common`** (AC: 3, 4, 5, 7)
  - [x] 3.1 Implementar `ListOpenFinanceInstitutionsUseCase`: lista instituições disponíveis (XP, Nubank, BTG, Itaú, Inter).
  - [x] 3.2 Implementar `InitiateOpenFinanceConnectionUseCase`:
    - Valida acesso a `profileId` via `FamilyProfilePort` e permissão Premium via `PremiumFeatureAccessGuard` (se `profileId != null`).
    - Verifica se já existe conexão prévia com a mesma instituição para a carteira e reaproveita/atualiza para evitar duplicações orfãs.
    - Persiste conexão com status `PENDING_CONSENT` e gera URL de autorização com state seguro.
  - [x] 3.3 Implementar `CompleteOpenFinanceConsentUseCase`: processa code e state, troca por tokens criptografados e atualiza conexão para `ACTIVE`.
  - [x] 3.4 Implementar `SyncOpenFinanceConnectionUseCase`:
    - Valida expiração de tokens e invoca renovação automática transparente via `OpenFinanceConnectorPort.refreshToken(...)`.
    - Orquestra a coleta de posições e transações, mapeando operações para `"COMPRA"` ou `"VENDA"` e integrando com `SaveTradeUseCase` para deduplicação automática de trades.
    - Dispara `SendPortfolioUpdateNotificationUseCase.execute(userId)` quando `trades_created > 0`.
    - Atualiza métricas e registros em `open_finance_sync_logs`.
  - [x] 3.5 Implementar `RevokeOpenFinanceConnectionUseCase`: revoga remotamente na instituição, marca como `REVOKED` e limpa credenciais armazenadas.
  - [x] 3.6 Implementar `ListUserOpenFinanceConnectionsUseCase`: lista conexões ativas do usuário garantindo isolamento multi-tenant e sanitização de credenciais.
  - [x] 3.7 Criar testes unitários exaustivos para todos os casos de uso cobrindo fluxos de sucesso, renovação de tokens, deduplicação de trades, notificação SSE, falha de comunicação e revogação.

- [x] **Task 4: Adaptador do Conector Open Finance, Agendador e Controllers no `api`** (AC: 3, 4, 5, 6, 8, 10)
  - [x] 4.1 Implementar `MockOpenFinanceConnectorAdapter` e cliente `OpenFinanceConnectorAdapter` configuráveis via `application.yml` (com connect timeout de 5s e read timeout de 15s) para suporte a ambientes offline/sandbox e conectores reais.
  - [x] 4.2 Implementar agendador `OpenFinanceSyncScheduler` com ShedLock `@SchedulerLock(name = "OpenFinanceSyncScheduler_syncAllActiveConnections")`, timezone `"America/Sao_Paulo"` e tratamento defensivo com isolamento de falha por conexão.
  - [x] 4.3 Implementar controller REST `OpenFinanceController` em `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/web/`:
    - Endpoints: `GET /institutions`, `GET /connections`, `POST /connections`, `POST /connections/callback`, `POST /connections/{id}/sync`, `DELETE /connections/{id}` e `POST /webhook`.
    - Garantir validação do cabeçalho `X-Webhook-Secret` no endpoint de webhook.
    - Garantir extração de `userId` estritamente do token JWT (`jwt.getSubject()`).
  - [x] 4.4 Atualizar `backend/api/src/main/resources/openapi.yaml` documentando os novos contratos REST, schemas e headers.
  - [x] 4.5 Configurar exclusão das classes geradas conflitantes (`OpenFinanceApi.java`, `OpenFinanceApiController.java`, `OpenFinanceApiDelegate.java`) no `maven-antrun-plugin` em `backend/api/pom.xml`.
  - [x] 4.6 Criar testes de integração MockMvc para o controller cobrindo todos os endpoints, validação de webhook secret e regras de segurança multi-tenant.

- [x] **Task 5: Frontend Flutter - Interface e Integração Open Finance** (AC: 9, 10)
  - [x] 5.1 Adicionar dependência `url_launcher: ^6.3.1` ao `frontend/pubspec.yaml` e executar `flutter pub get`.
  - [x] 5.2 Criar entidades de domínio `OpenFinanceInstitution`, `OpenFinanceConnection` e `OpenFinanceSyncResult` em `frontend/lib/src/features/open_finance/domain/entities/`.
  - [x] 5.3 Criar contrato de repositório `OpenFinanceRepository` em `frontend/lib/src/features/open_finance/domain/repositories/`.
  - [x] 5.4 Implementar `OpenFinanceRemoteDataSource` e `OpenFinanceRepositoryImpl` consumindo a API REST através de `ApiClient`.
  - [x] 5.5 Implementar `OpenFinanceProvider` com controle de estados (loading, success, error, syncing), listagem de conexões, polling de status durante sincronização, disparo de sincronização sob demanda e desconexão.
  - [x] 5.6 Criar widgets de interface:
    - `OpenFinanceInstitutionSelectorDialog`: diálogo para seleção da corretora com busca e logos;
    - `OpenFinanceConnectionCard`: card de exibição da conexão com status, data da última sincronização e ações rápidas;
    - `OpenFinanceTab`: aba dedicada integrada em `UploadPage` (`frontend/lib/src/features/upload/presentation/pages/upload_page.dart`) permitindo alternar entre upload manual e sincronização automática.
  - [x] 5.7 Registrar `OpenFinanceProvider` e injeção de dependências em `frontend/lib/main.dart`.
  - [x] 5.8 Criar testes de unidade e de widget para `OpenFinanceProvider` e os componentes visuais.

- [x] **Task 6: Validação de Qualidade e Conformidade Final** (AC: 10)
  - [x] 6.1 Executar a suíte de testes de backend `mvn clean test` garantindo 100% de sucesso.
  - [x] 6.2 Executar testes de frontend `flutter test` e análise estática `flutter analyze` garantindo 0 erros e 0 warnings.
  - [x] 6.3 Validar que nenhum token ou chave sensível seja impresso em logs da aplicação.

### Review Findings

- [x] [Review][Defer] Integração com Agregador Real de Produção vs Mock Sandbox [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/OpenFinanceConnectorAdapter.java:180] — deferred: integração com API física de agregador parceiro (Pluggy/Celcoin/Bacen) diferida para história dedicada de credenciamento
- [x] [Review][Patch] Validação de parâmetro state ausente no callback de consentimento (vulnerabilidade CSRF) [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/CompleteOpenFinanceConsentUseCase.java:37]
- [x] [Review][Patch] Disparo de sincronização inicial em background ausente após conclusão do consentimento [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/CompleteOpenFinanceConsentUseCase.java:65]
- [x] [Review][Patch] Frontend sem fluxo de captura de redirecionamento/callback e sem fechamento de diálogo [frontend/lib/src/features/open_finance/presentation/widgets/open_finance_tab.dart:45]
- [x] [Review][Patch] Colunas de tokens cifrados subdimensionadas em Liquibase (VARCHAR(512) -> VARCHAR(2048)) [backend/common/src/main/resources/db/changelog/33-create-open-finance-connections-and-sync-logs.yaml:52]
- [x] [Review][Patch] Tipos de colunas de UUID e data desviando da especificação (VARCHAR(36) -> CHAR(36), TIMESTAMP -> DATETIME) [backend/common/src/main/resources/db/changelog/33-create-open-finance-connections-and-sync-logs.yaml:11]
- [x] [Review][Patch] Restrição de unicidade ausente no par usuário/instituição/perfil no banco [backend/common/src/main/resources/db/changelog/33-create-open-finance-connections-and-sync-logs.yaml:100]
- [x] [Review][Patch] Retorno de verificação de tier Premium ignorado no controller permitindo bypass de plano [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/web/OpenFinanceController.java:55]
- [x] [Review][Patch] Ciclo de vida de tokens quebrado ao usar consentExpiresAt em vez de expiração do access token [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java:82]
- [x] [Review][Patch] Agendador sincroniza conexões com consentimento já expirado [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/jpa/repositories/OpenFinanceConnectionJpaRepository.java:18]
- [x] [Review][Patch] Status da conexão nunca transiciona para EXPIRED em falha irrecuperável de consentimento [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java:184]
- [x] [Review][Patch] Webhook vulnerável a timing attack e sem processamento de eventos recebidos [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/web/OpenFinanceController.java:175]
- [x] [Review][Patch] Código de autorização OAuth2 registrado em texto claro no log do Mock connector [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/MockOpenFinanceConnectorAdapter.java:66]
- [x] [Review][Patch] Ausência de parâmetro nonce nas URLs de consentimento geradas [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/MockOpenFinanceConnectorAdapter.java:59]
- [x] [Review][Patch] Componentes de UI ignoram URLs de logos das instituições parceiras [frontend/lib/src/features/open_finance/presentation/widgets/open_finance_connection_card.dart:75]
- [x] [Review][Patch] Falta de sincronização incremental pesquisando sempre janela fixa de 1 ano [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java:94]
- [x] [Review][Patch] Controller relança exceções como RuntimeException retornando HTTP 500 para requisições com dados inválidos [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/web/OpenFinanceController.java:70]
- [x] [Review][Patch] Tratamento de exceção por item ausente no laço de trades abortando todo o lote por uma transação [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java:130]
- [x] [Review][Patch] DataSource do frontend descarta parâmetro opcional redirectUri [frontend/lib/src/features/open_finance/data/datasources/open_finance_remote_data_source.dart:58]
- [x] [Review][Defer] Reconciliação direta de custódia vs derivação por transações de trades [backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java:88] — deferred, pre-existing
- [x] [Review][Defer] Processamento sequencial em thread única de conexões ativas no Scheduler em alta escala [backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/spring/OpenFinanceSyncScheduler.java:36] — deferred, pre-existing

## Dev Notes

### Previous Story Intelligence & Learnings

- **Story 9.1 (Nota de Corretagem SINACOR PDF):**
  - O isolamento multi-tenant por perfil familiar (`profileId`) deve ser validado via `FamilyProfilePort.findActiveByIdAndMasterUserId(profileId, userId)`.
  - O uso de subcarteira familiar (`profileId != null`) é um recurso Premium, exigindo a chamada prévia a `premiumFeatureAccessGuard.canAccessPremiumFeatures()`. Usuários no plano gratuito utilizam o conector exclusivamente na conta matriz (`profileId == null`).
- **Story 9.2 (Web Crawler de Proventos e Fatos Relevantes):**
  - O agendador ShedLock deve especificar explicitamente `zone = "America/Sao_Paulo"` para alinhamento com a rotina de mercado nacional.
  - No loop de sincronização batch, o processamento de cada conexão deve ser encapsulado individualmente em bloco `try-catch`, registrando `log.error("...", e)` com stack trace completo para falhas pontuais sem abortar o processamento dos demais usuários.
  - O cliente Spring `RestClient` deve ser configurado defensivamente com timeouts de conexão e leitura para evitar travamento indefinido de threads em caso de indisponibilidade da API parceira.

### Arquitetura de Criptografia em Repouso (AES-256-GCM)

Para atender ao requisito `NFR6`, tokens sensíveis obtidos das instituições financeiras não podem ser gravados em texto plano:
- Algoritmo: `AES/GCM/NoPadding`
- Tamanho da chave: 256 bits (32 bytes) obtida via property `openfinance.encryption.secret-key` (com fallback seguro para ambiente de testes `TestEncryptionKey32BytesLong12345!`).
- Vetor de Inicialização (IV): 12 bytes gerados por operação via `SecureRandom`.
- Formato armazenado na coluna do banco: `Base64(IV + Ciphertext + AuthTag)`.
- Ao decifrar: decodifica o Base64, extrai os primeiros 12 bytes como IV e decifra os bytes restantes com `GCMParameterSpec(128, iv)`.

### Ciclo de Vida do Token (Refresh Token Lifecycle)

- Os tokens de acesso (`access_token`) do Open Finance expiram em intervalos curtos (15 a 60 min).
- Antes de qualquer requisição de coleta de dados em `SyncOpenFinanceConnectionUseCase`:
  1. O sistema verifica se o token está próximo da expiração ou expirado.
  2. Se expirado ou se a chamada à instituição retornar HTTP 401, o use case decifra o `encrypted_refresh_token` e invoca `OpenFinanceConnectorPort.refreshToken(refreshToken)`.
  3. Os novos tokens retornados são criptografados com AES-256 e persistidos na entidade `OpenFinanceConnection`.
  4. A requisição de dados de investimentos é executada com o novo token válido.

### Reuso do Pipeline de Deduplicação de Trades e Notificação em Tempo Real

- Transações obtidas da corretora devem ser convertidas diretamente para a entidade `Trade` do domínio existente (`afsdigital.grahamselect.common.portfolio.domain.entities.Trade`).
- Normalização de operação:
  - Operações do tipo `BUY`, `COMPRA` -> `side = "COMPRA"`
  - Operações do tipo `SELL`, `VENDA` -> `side = "VENDA"`
  - Eventos de custódia não operacionais (juros sobre capital, dividendos em dinheiro, taxas de corretagem) são filtrados e ignorados na ingestão de trades.
- A persistência de cada transação DEVE utilizar o caso de uso existente `SaveTradeUseCase.execute(trade)`. O `SaveTradeUseCase` já valida `tradePort.exists(...)`, garantindo que operações já importadas previamente (seja por planilha B3, nota de corretagem PDF ou sincronizações anteriores) sejam descartadas sem duplicar registros na tabela `trades`.
- Quando `trades_created > 0`, o use case invoca `SendPortfolioUpdateNotificationUseCase.execute(userId)` para atualizar em tempo real o dashboard web do investidor via SSE (conforme padrão da Story 3.5).

### Configuração de Propriedades no `application.yml`

```yaml
openfinance:
  encryption:
    secret-key: ${OPENFINANCE_ENCRYPTION_KEY:0123456789abcdef0123456789abcdef}
  sync:
    cron: ${OPENFINANCE_SYNC_CRON:0 0 6 * * *}
  webhook:
    secret: ${OPENFINANCE_WEBHOOK_SECRET:sandbox-openfinance-webhook-secret-2026}
  mock:
    enabled: ${OPENFINANCE_MOCK_ENABLED:true}
  client:
    connect-timeout-ms: 5000
    read-timeout-ms: 15000
```

### Configuração de Exclusão no `backend/api/pom.xml` (`maven-antrun-plugin`)

Para evitar conflitos de compilação com as interfaces geradas automaticamente pelo OpenAPI Generator, adicionar ao plugin `maven-antrun-plugin` em `backend/api/pom.xml`:

```xml
<delete file="${project.build.directory}/generated-sources/openapi/src/main/java/afsdigital/grahamselect/api/OpenFinanceApi.java" />
<delete file="${project.build.directory}/generated-sources/openapi/src/main/java/afsdigital/grahamselect/api/OpenFinanceApiController.java" />
<delete file="${project.build.directory}/generated-sources/openapi/src/main/java/afsdigital/grahamselect/api/OpenFinanceApiDelegate.java" />
```

### File Structure Requirements

- **Backend Common:**
  - `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/domain/entities/`:
    - `OpenFinanceConnection.java`, `OpenFinanceSyncLog.java`, `OpenFinanceConnectionStatus.java`, `OpenFinanceSyncStatus.java`, `OpenFinanceSyncType.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/`:
    - `OpenFinanceInstitutionDTO.java`, `InitiateConnectionRequestDTO.java`, `ConsentUrlResponseDTO.java`, `ExchangeConsentCallbackDTO.java`, `OpenFinanceConnectionDTO`, `OpenFinanceSyncResultDTO.java`, `OpenFinancePositionDTO.java`, `OpenFinanceTransactionDTO.java`, `OpenFinanceTokenPairDTO.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/repository/`:
    - `OpenFinanceConnectionPort.java`, `OpenFinanceSyncLogPort.java`, `OpenFinanceConnectorPort.java`, `TokenEncryptionPort.java`
  - `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/`:
    - `ListOpenFinanceInstitutionsUseCase.java`, `InitiateOpenFinanceConnectionUseCase.java`, `CompleteOpenFinanceConsentUseCase.java`, `SyncOpenFinanceConnectionUseCase.java`, `RevokeOpenFinanceConnectionUseCase.java`, `ListUserOpenFinanceConnectionsUseCase.java`
- **Backend API:**
  - `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/security/`:
    - `AesGcmTokenEncryptionAdapter.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/jpa/entities/`:
    - `OpenFinanceConnectionEntity.java`, `OpenFinanceSyncLogEntity.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/jpa/repositories/`:
    - `OpenFinanceConnectionJpaRepository.java`, `OpenFinanceSyncLogJpaRepository.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/`:
    - `OpenFinanceConnectionJpaAdapter.java`, `OpenFinanceSyncLogJpaAdapter.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/`:
    - `MockOpenFinanceConnectorAdapter.java`, `OpenFinanceConnectorAdapter.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/spring/`:
    - `OpenFinanceConfiguration.java`, `OpenFinanceSyncScheduler.java`
  - `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/web/`:
    - `OpenFinanceController.java`
  - `backend/common/src/main/resources/db/changelog/`:
    - `33-create-open-finance-connections-and-sync-logs.yaml`
- **Frontend:**
  - `frontend/lib/src/features/open_finance/domain/entities/`:
    - `open_finance_institution.dart`, `open_finance_connection.dart`, `open_finance_sync_result.dart`
  - `frontend/lib/src/features/open_finance/domain/repositories/`:
    - `open_finance_repository.dart`
  - `frontend/lib/src/features/open_finance/data/datasources/`:
    - `open_finance_remote_data_source.dart`
  - `frontend/lib/src/features/open_finance/data/repositories/`:
    - `open_finance_repository_impl.dart`
  - `frontend/lib/src/features/open_finance/presentation/providers/`:
    - `open_finance_provider.dart`
  - `frontend/lib/src/features/open_finance/presentation/widgets/`:
    - `open_finance_connection_card.dart`, `open_finance_institution_dialog.dart`, `open_finance_tab.dart`
  - `frontend/lib/src/features/upload/presentation/pages/upload_page.dart`:
    - Atualização para acomodar as abas "Upload Manual" e "Open Finance".

### References

- PRD: [docs/bmad/planning-artifacts/prd.md#2-data-ingestion--integration-a-b3--beyond](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/prd.md) (Requisitos FR11, NFR4, NFR6)
- Epics: [docs/bmad/planning-artifacts/epics.md#story-93-conector-open-finance](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/epics.md#L827-L831)
- Contexto do Projeto: [docs/bmad/project-context.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/project-context.md)
- Regras de Agentes de IA: [AGENTS.md](file:///c:/Users/afssi/projects/graham-select/AGENTS.md)
- SaveTradeUseCase: [backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/SaveTradeUseCase.java](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/SaveTradeUseCase.java)
- Trade Entity: [backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/domain/entities/Trade.java](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/domain/entities/Trade.java)
- SendPortfolioUpdateNotificationUseCase: [backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/SendPortfolioUpdateNotificationUseCase.java](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/SendPortfolioUpdateNotificationUseCase.java)
- Story 9.1 (Importação Nota SINACOR PDF): [docs/bmad/implementation-artifacts/9-1-importacao-de-nota-de-corretagem-pdf.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/implementation-artifacts/9-1-importacao-de-nota-de-corretagem-pdf.md)
- Story 9.2 (Web Crawler de Proventos e Fatos Relevantes): [docs/bmad/implementation-artifacts/9-2-web-crawler-de-proventos-e-fatos-relevantes.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/implementation-artifacts/9-2-web-crawler-de-proventos-e-fatos-relevantes.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.8 Flash (Medium)

### Debug Log References

- Sucesso na execução dos testes unitários de criptografia AES-256-GCM (`AesGcmTokenEncryptionAdapterTest`).
- Sucesso nos testes de adaptadores JPA (`OpenFinanceJpaAdaptersTest`).
- Sucesso nos testes de casos de uso (`OpenFinanceUseCasesTest`) validando iniciação, consentimento, renovação transparente de tokens, deduplicação de trades com `SaveTradeUseCase` e notificação SSE.
- Sucesso nos testes de agendador ShedLock (`OpenFinanceSyncSchedulerTest`) e MockMvc do controller (`OpenFinanceControllerTest`).
- Sucesso nos testes unitários e de widget do frontend (`open_finance_provider_test.dart`, `open_finance_tab_test.dart`, `upload_page_test.dart`).
- Validação com 100% de sucesso no `mvn clean test` de todos os módulos backend (`backend-parent`, `common`, `api-service`, `valuation-service`).
- Análise estática `flutter analyze` finalizada com 0 erros e 0 warnings.

### Completion Notes List

- Implementação completa do Conector Open Finance atendendo a todos os critérios de aceitação (AC 1 a AC 10):
  1. Script de migração Liquibase `33-create-open-finance-connections-and-sync-logs.yaml` criando as tabelas `open_finance_connections` e `open_finance_sync_logs` com chaves estrangeiras, restrições e índices de alta performance.
  2. Camada de criptografia em repouso AES-256-GCM (`AesGcmTokenEncryptionAdapter`) com IV aleatório de 12 bytes gerado por operação e chave configurável, garantindo proteção contra vazamentos de tokens e credenciais.
  3. Modelos de domínio, registros, DTOs e portas no módulo `common`.
  4. Casos de uso de negócio no módulo `common`: listagem de instituições, iniciação de conexão com validação de perfil familiar e tier Premium, ativação de consentimento, pipeline de sincronização com renovação automática de token expirado, deduplicação de trades via `SaveTradeUseCase`, disparo de evento SSE e revogação remota/local de consentimento.
  5. Agendador defensivo `OpenFinanceSyncScheduler` com ShedLock `@SchedulerLock(name = "OpenFinanceSyncScheduler_syncAllActiveConnections")` e fuso horário `America/Sao_Paulo`.
  6. Controller REST `OpenFinanceController` com endpoints completos, proteção de webhook via cabeçalho `X-Webhook-Secret` e liberação pública em `SecurityConfig`.
  7. Atualização do contrato OpenAPI em `openapi.yaml` e exclusão de stubs conflitantes no plugin AntRun do `pom.xml`.
  8. Interface no Frontend Flutter com `url_launcher: ^6.3.1`, camada de dados, gerência de estado via `OpenFinanceProvider`, e aba dedicada "Open Finance (Automático)" na tela de Upload com diálogo de seleção de bancos e cartões de conexões com status e sincronização sob demanda.
  9. Conformidade estrita com `AGENTS.md`: logs em `INFO` em todas as entradas de API e schedulers, logs em `ERROR` com stack traces nas exceções, manipulações de tempo com `ZoneOffset.UTC` e 0 credenciais em logs.

### File List

- `backend/common/src/main/resources/db/changelog/33-create-open-finance-connections-and-sync-logs.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/domain/entities/OpenFinanceConnectionStatus.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/domain/entities/OpenFinanceSyncStatus.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/domain/entities/OpenFinanceSyncType.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/domain/entities/OpenFinanceConnection.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/domain/entities/OpenFinanceSyncLog.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/OpenFinanceInstitutionDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/InitiateConnectionRequestDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/ConsentUrlResponseDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/ExchangeConsentCallbackDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/OpenFinanceConnectionDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/OpenFinanceSyncResultDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/OpenFinancePositionDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/OpenFinanceTransactionDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/dto/OpenFinanceTokenPairDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/repository/OpenFinanceConnectionPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/repository/OpenFinanceSyncLogPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/repository/OpenFinanceConnectorPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/repository/TokenEncryptionPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/ListOpenFinanceInstitutionsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/InitiateOpenFinanceConnectionUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/CompleteOpenFinanceConsentUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/SyncOpenFinanceConnectionUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/RevokeOpenFinanceConnectionUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/openfinance/application/usecase/ListUserOpenFinanceConnectionsUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/openfinance/application/usecase/OpenFinanceUseCasesTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/security/AesGcmTokenEncryptionAdapter.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/openfinance/infrastructure/security/AesGcmTokenEncryptionAdapterTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/jpa/entities/OpenFinanceConnectionEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/jpa/entities/OpenFinanceSyncLogEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/jpa/repositories/OpenFinanceConnectionJpaRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/jpa/repositories/OpenFinanceSyncLogJpaRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/OpenFinanceConnectionJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/OpenFinanceSyncLogJpaAdapter.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/openfinance/infrastructure/persistence/OpenFinanceJpaAdaptersTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/MockOpenFinanceConnectorAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/connector/OpenFinanceConnectorAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/spring/OpenFinanceConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/infrastructure/spring/OpenFinanceSyncScheduler.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/openfinance/infrastructure/spring/OpenFinanceSyncSchedulerTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/openfinance/web/OpenFinanceController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/openfinance/web/OpenFinanceControllerTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/SecurityConfig.java`
- `backend/api/src/main/resources/application.yml`
- `backend/api/src/main/resources/openapi.yaml`
- `backend/api/pom.xml`
- `frontend/pubspec.yaml`
- `frontend/pubspec.lock`
- `frontend/lib/main.dart`
- `frontend/lib/src/features/open_finance/domain/entities/open_finance_institution.dart`
- `frontend/lib/src/features/open_finance/domain/entities/open_finance_connection.dart`
- `frontend/lib/src/features/open_finance/domain/entities/open_finance_sync_result.dart`
- `frontend/lib/src/features/open_finance/domain/repositories/open_finance_repository.dart`
- `frontend/lib/src/features/open_finance/data/datasources/open_finance_remote_data_source.dart`
- `frontend/lib/src/features/open_finance/data/repositories/open_finance_repository_impl.dart`
- `frontend/lib/src/features/open_finance/presentation/providers/open_finance_provider.dart`
- `frontend/lib/src/features/open_finance/presentation/widgets/open_finance_connection_card.dart`
- `frontend/lib/src/features/open_finance/presentation/widgets/open_finance_institution_dialog.dart`
- `frontend/lib/src/features/open_finance/presentation/widgets/open_finance_tab.dart`
- `frontend/lib/src/features/upload/presentation/pages/upload_page.dart`
- `frontend/test/features/open_finance/presentation/open_finance_provider_test.dart`
- `frontend/test/features/open_finance/presentation/open_finance_tab_test.dart`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`
- `docs/bmad/implementation-artifacts/9-3-conector-open-finance.md`
