---
baseline_commit: 158342e2645232fc3413b052550c7259dda590fa
story_id: "9.2"
story_key: "9-2-web-crawler-de-proventos-e-fatos-relevantes"
---

# Story 9.2: Web Crawler de Proventos e Fatos Relevantes

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como sistema (backend),
Eu quero buscar anúncios de dividendos, JCP, rendimentos e fatos relevantes diretamente nos canais oficiais e provedores de mercado (CVM, B3 e provedores complementares),
Para que a agenda de proventos (Data COM, Data EX, Data de Pagamento, Valor por Cota/Ação e tipo de provento) e os fatos relevantes das empresas em custódia e acompanhadas estejam sempre atualizados para os investidores do Graham-Select.

## Escopo e Limites (Boundaries)

- **O que ESTÁ incluído nesta história (Story 9.2):**
  1. Adição da biblioteca JSoup (`org.jsoup:jsoup:1.18.3`) no módulo `backend/api/pom.xml` para parsing seguro e estruturação de documentos HTML/XML oriundos de canais públicos e oficiais, preservando a pureza de domínio do módulo `common`.
  2. Criação do script de migração Liquibase `db/changelog/32-create-dividend-announcements-and-relevant-facts.yaml` e inclusão em `db.changelog-master.yaml`, criando as tabelas relacionais `dividend_announcements` e `relevant_facts` com campos padronizados, tipos enumerados, índices otimizados e restrição de unicidade composta em `(ticker, com_date, type, rate)` para garantia estrita de idempotência mesmo quando `payment_date` for indeterminado (`NULL`).
  3. Criação de modelos de domínio, entidades e registros imutáveis no módulo `common`:
     - Entidades: `DividendAnnouncement`, `RelevantFact`, enum `CorporateEventType` (`DIVIDENDO`, `JCP`, `RENDIMENTO`, `BONIFICACAO`, `DESDOBRAMENTO`).
     - Contratos de Portas: `DividendAnnouncementPort`, `RelevantFactPort`, `DividendCrawlerPort`, `RelevantFactCrawlerPort`.
     - Reutilização da porta existente: `CustodyTickerPort` (`afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort`) para descoberta de tickers ativos em custódia.
  4. Implementação de casos de uso desacoplados no módulo `common`:
     - `SyncCorporateEventsUseCase`: orquestra a coleta de proventos e fatos relevantes para todos os tickers ativos em custódia ou cadastrados no mestre de dados (`CustodyTickerPort`), executando a persistência com deduplicação e atualização de registros existentes.
     - `GetDividendScheduleUseCase`: consulta a agenda consolidada de proventos com filtros opcionais por ticker, intervalo de datas (`startDate`, `endDate`), status e cálculo de valor estimado para o usuário autenticado (`onlyMyTickers=true`), reaproveitando `GetCustodyPositionsUseCase` (`afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase`) para obter posições ativas.
     - `GetRelevantFactsUseCase`: consulta as divulgações corporativas e fatos relevantes filtrados por período e ativos da carteira.
  5. Implementação de adaptadores de crawler e integração de dados de mercado no módulo `api`:
     - `CvmOfficialCrawlerAdapter`: crawler resiliente baseado em `Jsoup` capaz de consultar e extrair eventos corporativos e fatos relevantes publicados em canais abertos e oficiais da CVM / B3.
     - `BrapiCorporateEventsAdapter`: adaptador estruturado utilizando o `RestClient` já configurado com fallback e query `/quote/{tickers}?dividends=true` para consumir o módulo de dividendos corporativos da API Brapi como fonte de dados de alta precisão.
     - `CompositeCorporateEventsCrawler`: orquestrador de fontes com estratégia de agregação, normalização e fallback transparente em caso de indisponibilidade de fontes externas, isolando falhas por ticker.
  6. Agendamento automático com execução distribuída segura:
     - Componente `CorporateEventsScheduler` com anotações `@Scheduled(cron = "${corporate-events.sync.cron:0 0 21 * * MON-FRI}")` e `@SchedulerLock(name = "CorporateEventsScheduler_syncCorporateEvents")` via ShedLock, garantindo execução única em ambientes clusterizados.
  7. Endpoints REST no módulo `api`:
     - `GET /api/v1/dividends/schedule`: Retorna a agenda de proventos, permitindo parâmetros `ticker`, `startDate`, `endDate`, `onlyMyTickers` (com extração do `userId` via token JWT) e calculando a estimativa de recebimento quando aplicável.
     - `GET /api/v1/dividends/facts`: Retorna a lista de fatos relevantes recentes, suportando filtros por ticker e ativos da carteira do investidor.
     - `POST /api/v1/admin/crawler/dividends/sync`: Disparo manual sob demanda restrito a administradores protegido por `@RequireAdmin` (`afsdigital.grahamselect.api.auth.infrastructure.security.RequireAdmin`) para forçar varredura e atualização imediata.
  8. Atualização do contrato OpenAPI 3.0 em `backend/api/src/main/resources/openapi.yaml` documentando todos os novos endpoints, parâmetros e schemas de resposta, garantindo compatibilidade com os controllers manuais sem conflitos de compilação no plugin openapi-generator.
  9. Interface Frontend Flutter:
     - Camada de Dados e Repositório: `DividendRemoteDataSource`, `DividendRepository` e `DividendRepositoryImpl`.
     - Gerenciamento de Estado: `DividendProvider` com suporte a estados de carregamento, sucesso, erro com retry, filtros e projeção de valores.
     - Página e Widgets de Apresentação: Tela **Agenda de Proventos e Fatos Relevantes** (`/dividends`), contendo seções para "Agenda de Proventos" (com datas COM, Pagamento, valor por cota e total previsto da carteira) e "Fatos Relevantes" (com tags de categoria, data de publicação e resumo).
     - Integração de Navegação: Adição da rota `/dividends` em `main.dart` e novo item de menu na barra lateral de navegação (`_Sidebar`) e barra inferior (`_BottomNavBar`) em `main_layout.dart` mantendo estrita sincronia de índices (índice 7 para `/dividends` e índice 8 para `/profile`).
  10. Conformidade estrita com `AGENTS.md` e `project-context.md`:
      - Registro de log `INFO` em todas as entradas de requisições de API, execuções de schedulers e coletas de crawlers.
      - Registro de log `ERROR` acompanhado de mensagem e stack trace completo (`log.error("...", e)`) em todos os blocos de captura de exceção.
      - Uso obrigatório de `ZoneOffset.UTC` em todas as manipulações de data e hora.
      - Garantia de 0 erros e 0 warnings no `flutter analyze` e 100% de sucesso nos testes unitários e de integração de frontend e backend com testes de crawler 100% herméticos (sem chamadas HTTP externas reais em build).

- **O que NÃO está incluído (fora de escopo desta história):**
  - Conexão e sincronização bancária via Open Finance (previsto na Story 9.3).
  - Envio de mensagens ativas e notificações via WhatsApp ou push (previsto no Epic 11).
  - Execução de ordens automáticas de reinvestimento de proventos.

## Acceptance Criteria

1. **Modelagem Relacional e Persistência de Proventos e Fatos Relevantes (Liquibase) (FR10, FR13):**
   - **Dado** o processo de inicialização do backend com Liquibase
   - **Quando** o changeset `32-create-dividend-announcements-and-relevant-facts.yaml` é executado
   - **Então** cria a tabela `dividend_announcements` contendo:
     - `id` (VARCHAR(36) PRIMARY KEY), `ticker` (VARCHAR(20) NOT NULL), `type` (VARCHAR(30) NOT NULL),
     - `rate` (DECIMAL(16,6) NOT NULL - valor bruto por cota/ação), `net_value` (DECIMAL(16,6) NULL),
     - `approved_date` (DATE NULL), `com_date` (DATE NOT NULL), `ex_date` (DATE NULL),
     - `payment_date` (DATE NULL), `source` (VARCHAR(50) NOT NULL), `status` (VARCHAR(30) NOT NULL),
     - `created_at` (TIMESTAMP NOT NULL), `updated_at` (TIMESTAMP NOT NULL)
     - Restrição de unicidade composta estrita em `(ticker, com_date, type, rate)` (evitando dependência de `payment_date`, que pode ser inicialmente `NULL`)
   - **E** cria a tabela `relevant_facts` contendo:
     - `id` (VARCHAR(36) PRIMARY KEY), `ticker` (VARCHAR(20) NOT NULL), `title` (VARCHAR(255) NOT NULL),
     - `summary` (TEXT NULL), `source_url` (VARCHAR(500) NULL), `category` (VARCHAR(50) NOT NULL),
     - `publication_date` (TIMESTAMP NOT NULL), `created_at` (TIMESTAMP NOT NULL)
     - Índice em `(ticker, publication_date)` para consultas cronológicas rápidas.

2. **Web Crawler Resiliente e Multi-Fonte (CVM / Canais Oficiais / Brapi Fallback) (FR10):**
   - **Dado** a lista de tickers ativos retornada por `CustodyTickerPort.findDistinctActiveTickers()`
   - **Quando** o motor de coleta (`CompositeCorporateEventsCrawler`) executa a varredura
   - **Então** aciona a extração de eventos corporativos e fatos relevantes nos canais oficiais e estruturados
   - **E** para cada provento identificado, normaliza o tipo (`DIVIDENDO`, `JCP`, `RENDIMENTO`, `BONIFICACAO`, `DESDOBRAMENTO`), formata valores numéricos para `BigDecimal` e datas para `LocalDate` em UTC
   - **E** calcula o `netValue` bruto e líquido com precisão:
     - Se `type == JCP`: aplica a alíquota padrão de 15% de IRRF para pessoa física: $\text{netValue} = \text{rate} \times 0.85$
     - Se `type == DIVIDENDO` ou `RENDIMENTO` (FII isento): $\text{netValue} = \text{rate}$
   - **E** em caso de indisponibilidade, timeout ou resposta inesperada de uma fonte externa (ex: erro 403, 500 ou HTML malformado), ativa o fallback transparente sem abortar o processamento dos demais tickers e registra log `WARN` contendo o ticker e a causa
   - **E** garante que os eventos corporativos já existentes na base sejam atualizados deterministicamente (especialmente `payment_date` e `status`) sem gerar duplicidades.

3. **Agendamento Automatizado e Bloqueio Distribuído (Scheduler & ShedLock):**
   - **Dado** o agendamento configurado via `${corporate-events.sync.cron:0 0 21 * * MON-FRI}`
   - **Quando** o cron dispara no horário programado (dias úteis às 21:00 no fuso de Brasília)
   - **Então** o componente `CorporateEventsScheduler` obtém o lock distribuído na tabela `shedlock` (`name = "CorporateEventsScheduler_syncCorporateEvents"`, `lockAtLeastFor = "1m"`, `lockAtMostFor = "15m"`)
   - **E** delega a sincronização para `SyncCorporateEventsUseCase.execute()`
   - **E** ao término da execução, registra log `INFO` com métricas consolidadas: total de tickers consultados, novos proventos inseridos, proventos atualizados e total de fatos relevantes registrados.

4. **Endpoint REST de Consulta da Agenda de Proventos (`GET /api/v1/dividends/schedule`) (FR13):**
   - **Dado** um investidor autenticado com token JWT válido
   - **Quando** envia requisição `GET /api/v1/dividends/schedule` podendo fornecer parâmetros opcionais `ticker`, `startDate`, `endDate`, `status` e `onlyMyTickers=true`
   - **Então** o sistema filtra a agenda de proventos conforme os critérios estabelecidos
   - **E** caso `onlyMyTickers=true` seja informado, invoca `GetCustodyPositionsUseCase.execute(userId)` para identificar as posições ativas da custódia do usuário autenticado (`userId` extraído do JWT)
   - **E** para cada provento da carteira com saldo positivo, calcula e retorna o campo `estimatedTotal = position.quantity * dividend.rate` e `estimatedNetTotal = position.quantity * dividend.netValue`, permitindo que o investidor saiba exatamente quanto irá receber
   - **E** responde com status HTTP `200 OK` contendo lista ordenada cronologicamente por `payment_date` ascendente (ou `com_date` quando `payment_date` for indeterminado).

5. **Endpoint REST de Consulta de Fatos Relevantes (`GET /api/v1/dividends/facts`):**
   - **Dado** um usuário autenticado
   - **Quando** envia requisição `GET /api/v1/dividends/facts` com parâmetros opcionais `ticker`, `limit` (padrão 20) e `onlyMyTickers=true`
   - **Então** retorna a relação de fatos relevantes e comunicados de mercado ordenados por `publication_date` descendente
   - **E** retorna HTTP `200 OK` contendo a listagem com `id`, `ticker`, `title`, `summary`, `category`, `publicationDate` e `sourceUrl`.

6. **Endpoint Administrativo de Disparo Manual sob Demanda (`POST /api/v1/admin/crawler/dividends/sync`):**
   - **Dado** um usuário autenticado
   - **Quando** envia requisição `POST /api/v1/admin/crawler/dividends/sync`
   - **Então** a autorização é verificada através da anotação `@RequireAdmin` (`@PreAuthorize("@adminAccessGuard.canAccessAdmin()")`)
   - **E** se o usuário possuir o privilégio administrativo, o sistema dispara a execução de sincronização
   - **E** responde com HTTP `200 OK` ou `202 Accepted` informando o status do disparo e sumário de sincronização
   - **E** caso um usuário não-admin tente invocar o endpoint, rejeita imediatamente com HTTP `403 Forbidden` conforme padrão RFC 7807.

7. **Interface Frontend Flutter - Tela de Agenda de Proventos e Fatos Relevantes (`/dividends`):**
   - **Dado** que o usuário navega para a rota `/dividends` (através do novo item no menu lateral e inferior "Agenda de Proventos" `Key('nav_dividends_button')`)
   - **Quando** a página é renderizada
   - **Então** exibe:
     1. **Barra Superior / Controles:** Título "Agenda de Proventos & Fatos Relevantes", switch/chip de filtro "Meus Ativos" (`Key('dividends_only_my_assets_toggle')`), seletor de intervalo de data e botão de recarregar (`Key('dividends_refresh_button')`);
     2. **Card de Resumo de Proventos Futuros:** Card de destaque exibindo o total estimado de proventos a receber no mês corrente / próximos 30 dias com base na custódia ativa (`Key('dividends_estimated_total_card')`);
     3. **Aba "Agenda de Proventos":** Lista ou tabela com cards visuais de proventos (`Key('dividends_schedule_list')`), exibindo ticker, badge do tipo de provento (`DIVIDENDO`, `JCP`, `RENDIMENTO`), Data COM, Data de Pagamento, Valor por cota (ex: `R$ 0,75`), e se o usuário tiver custódia, o valor total projetado (ex: `100 cotas = R$ 75,00`);
     4. **Aba "Fatos Relevantes":** Lista de comunicados corporativos (`Key('relevant_facts_list')`), exibindo ticker, título, data de divulgação formatada, badge de categoria e link/botão para visualizar o documento oficial na fonte;
     5. **Estados de Interface:** Skeleton screen animado durante carregamento (`Key('dividends_skeleton')`), estado vazio ilustrado quando nenhum evento corporativo for encontrado (`Key('dividends_empty_state')`), e mensagem com botão de tentar novamente em caso de falha de conexão.
   - **E** a integração no `MainLayout` atualiza sincronizadamente o `_Sidebar` (NavigationRail) e o `_BottomNavBar` (NavigationBar), associando `/dividends` ao índice 7 e `/profile` ao índice 8, sem disparar `RangeError`.

8. **Observabilidade, Logs Estruturados e Conformidade com Diretrizes (`AGENTS.md` & `project-context.md`):**
   - **Dado** o processamento de crawlers, agendamentos e chamadas REST
   - **Então** todas as entradas de endpoints HTTP e execuções do scheduler DEVEM ser registradas em nível `INFO` com dados contextuais sanitizados (`userId`, `ticker`, `source`)
   - **E** toda exceção lançada ou capturada DEVE ser logada em nível `ERROR` com mensagem descritiva e stack trace completo (`log.error("...", exception)`)
   - **E** todas as operações com datas e horários DEVEM utilizar estritamente `ZoneOffset.UTC`
   - **E** todos os testes automatizados de crawling (`CvmOfficialCrawlerAdapterTest`, `BrapiCorporateEventsAdapterTest`) DEVEM ser 100% herméticos e offline, utilizando mocks ou fixtures locais sem executar conexões HTTP externas reais durante `mvn clean test`
   - **E** a suíte de testes de backend (`mvn clean test`) e frontend (`flutter test`, `flutter analyze`) DEVE passar com 100% de sucesso sem warnings ou regressões.

## Tasks / Subtasks

- [x] **Task 1: Dependências, Modelos de Domínio e Portas no Módulo `common` e `api`** (AC: 1, 2, 8)
  - [x] 1.1 Adicionar dependência `org.jsoup:jsoup:1.18.3` em `backend/api/pom.xml` (mantendo `common` livre de dependências de scraping web).
  - [x] 1.2 Compilar com `mvn test-compile -pl common,api` para validar compatibilidade do classpath.
  - [x] 1.3 Criar enum `CorporateEventType` (`DIVIDENDO`, `JCP`, `RENDIMENTO`, `BONIFICACAO`, `DESDOBRAMENTO`) em `afsdigital.grahamselect.common.dividends.domain.entities`.
  - [x] 1.4 Criar entidades de domínio imutáveis `DividendAnnouncement` e `RelevantFact` com validações de dados e métodos auxiliares em `afsdigital.grahamselect.common.dividends.domain.entities`.
  - [x] 1.5 Definir interfaces de portas no módulo `common` em `afsdigital.grahamselect.common.dividends.application.repository`:
    - `DividendAnnouncementPort`: métodos de consulta por ticker, intervalo de datas e persistência idempotente (`saveAll`, `findByTickersAndDateRange`, `findByTickerAndComDateAndTypeAndRate`, `exists`).
    - `RelevantFactPort`: métodos de consulta e persistência de fatos relevantes (`saveAll`, `findByTickersAndDateRange`, `existsByTickerAndPublicationDate`).
    - `DividendCrawlerPort` e `RelevantFactCrawlerPort`: contratos abstratos para coleta de dados em fontes externas.
  - [x] 1.6 Criar DTOs de transporte no módulo `common`: `DividendScheduleDTO`, `RelevantFactDTO`, `CorporateEventSyncSummaryDTO`.

- [x] **Task 2: Migração Liquibase e Persistência JPA no `common` e `api`** (AC: 1, 8)
  - [x] 2.1 Criar arquivo de migração `backend/common/src/main/resources/db/changelog/32-create-dividend-announcements-and-relevant-facts.yaml` com restrição de unicidade composta em `(ticker, com_date, type, rate)`.
  - [x] 2.2 Adicionar inclusão do changeset 32 no `db.changelog-master.yaml`.
  - [x] 2.3 Criar entidades JPA `DividendAnnouncementEntity` e `RelevantFactEntity` mapeando colunas, tipos e índices.
  - [x] 2.4 Criar repositórios Spring Data JPA `DividendAnnouncementJpaRepository` e `RelevantFactJpaRepository`.
  - [x] 2.5 Implementar adaptadores `DividendAnnouncementJpaAdapter` e `RelevantFactJpaAdapter` implementando as portas do domínio com lógica de merge/update quando o registro já existir.
  - [x] 2.6 Criar testes unitários e de repositório para persistência, consulta e deduplicação de proventos.

- [x] **Task 3: Motores de Crawling, Ingestão e Fallback Hermético no Módulo `api`** (AC: 2, 8)
  - [x] 3.1 Implementar `BrapiCorporateEventsAdapter`: consome endpoint `/quote/{tickers}?dividends=true` da API Brapi com tratamento resiliente e mapeamento para o domínio.
  - [x] 3.2 Implementar `CvmOfficialCrawlerAdapter`: crawler baseado em `Jsoup` para consulta de informes corporativos e fatos relevantes de canais oficiais e públicos com timeouts e sanitização de dados.
  - [x] 3.3 Implementar `CompositeCorporateEventsCrawler`: orquestra as coletas, agrega resultados, aplica deduplicação de anúncios e gerencia fallback com tolerância a falhas por ticker.
  - [x] 3.4 Configurar propriedades de crawler em `backend/api/src/main/resources/application.yml` (`crawler.user-agent`, `crawler.timeout-ms`, `corporate-events.sync.cron`).
  - [x] 3.5 Criar testes unitários 100% herméticos para os crawlers com mocks de responses HTML e JSON estáticos locais, sem dependência de rede externa.

- [x] **Task 4: Scheduler Automático com ShedLock e Casos de Uso de Aplicação** (AC: 2, 3, 4, 5, 8)
  - [x] 4.1 Criar caso de uso `SyncCorporateEventsUseCase` no módulo `common`:
    - Obtém lista de tickers rastreados via `CustodyTickerPort.findDistinctActiveTickers()`.
    - Executa coleta multi-fonte via `CompositeCorporateEventsCrawler`.
    - Persiste proventos e fatos relevantes garantindo idempotência e sem duplicações.
    - Retorna sumário consolidado de execução.
  - [x] 4.2 Criar caso de uso `GetDividendScheduleUseCase`:
    - Consulta proventos por ticker e período.
    - Injeta `GetCustodyPositionsUseCase`: se `userId` for informado e `onlyMyTickers=true`, cruza com posições ativas de custódia e calcula o valor projetado total (`estimatedTotal = quantity * rate`) e líquido (`estimatedNetTotal = quantity * netValue`).
  - [x] 4.3 Criar caso de uso `GetRelevantFactsUseCase` para consulta de divulgações corporativas.
  - [x] 4.4 Implementar `CorporateEventsScheduler` com anotações `@Scheduled` e `@SchedulerLock` para execução periódica às 21:00 (dias úteis).
  - [x] 4.5 Registrar beans de serviço e use cases em classe de configuração Spring (`CorporateEventsConfiguration`).
  - [x] 4.6 Criar testes unitários exaustivos para `SyncCorporateEventsUseCaseTest` e `GetDividendScheduleUseCaseTest`.

- [x] **Task 5: Endpoints REST, Segurança e Atualização do OpenAPI** (AC: 4, 5, 6, 8)
  - [x] 5.1 Atualizar `backend/api/src/main/resources/openapi.yaml` documentando as rotas `/dividends/schedule`, `/dividends/facts` e `/admin/crawler/dividends/sync`.
  - [x] 5.2 Criar controller `DividendController` em `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/web/DividendController.java` expondo `/api/v1/dividends/schedule` e `/api/v1/dividends/facts`.
  - [x] 5.3 Criar controller administrativo `AdminDividendCrawlerController` expondo `POST /api/v1/admin/crawler/dividends/sync` protegido pela anotação `@RequireAdmin`.
  - [x] 5.4 Adicionar logs de entrada `INFO` com dados de contexto e logs de nível `ERROR` com stack trace em exceções conforme `AGENTS.md`.
  - [x] 5.5 Criar testes de integração MockMvc para `DividendControllerTest` e `AdminDividendCrawlerControllerTest` validando cenários de sucesso, filtros de data, cálculo de estimativa e permissões de acesso.

- [x] **Task 6: Frontend Flutter - Camada de Domínio, Dados e Estado** (AC: 7)
  - [x] 6.1 Criar entidades de domínio em `frontend/lib/src/features/dividends/domain/entities/`:
    - `DividendAnnouncement`: ticker, type, rate, netValue, comDate, paymentDate, status, estimatedTotal, estimatedNetTotal.
    - `RelevantFact`: ticker, title, summary, category, publicationDate, sourceUrl.
  - [x] 6.2 Criar contratos de repositório em `frontend/lib/src/features/dividends/domain/repositories/dividend_repository.dart`.
  - [x] 6.3 Implementar `DividendRemoteDataSource` utilizando o `ApiClient` Dio existente.
  - [x] 6.4 Implementar `DividendRepositoryImpl` com mapeamento para modelos `DividendAnnouncementModel` e `RelevantFactModel`.
  - [x] 6.5 Implementar `DividendProvider` com suporte a `ChangeNotifier`, métodos para carregar agenda, carregar fatos relevantes, alternar filtro "Meus Ativos" e recarregar dados.
  - [x] 6.6 Adicionar `DividendRemoteDataSource`, `DividendRepositoryImpl` e `DividendProvider` na árvore de injeção de dependências em `frontend/lib/main.dart`.
  - [x] 6.7 Criar testes unitários para `DividendProviderTest` e `DividendRepositoryImplTest`.

- [x] **Task 7: Frontend Flutter - Interface de Usuário da Agenda de Proventos e Navegação** (AC: 7)
  - [x] 7.1 Criar tela principal `DividendsPage` em `frontend/lib/src/features/dividends/presentation/pages/dividends_page.dart`.
  - [x] 7.2 Implementar card de destaque com resumo de proventos previstos para os próximos 30 dias (`Key('dividends_estimated_total_card')`).
  - [x] 7.3 Implementar lista responsiva com cards de proventos futuros com badges visuais por tipo de provento (`Key('dividends_schedule_list')`).
  - [x] 7.4 Implementar aba de fatos relevantes com links diretos para leitura dos comunicados oficiais (`Key('relevant_facts_list')`).
  - [x] 7.5 Implementar estados de Skeleton Screen animado e estado vazio ilustrado com mensagens claras ao usuário.
  - [x] 7.6 Adicionar rota `/dividends` no `GoRouter` dentro do `ShellRoute` em `frontend/lib/main.dart`.
  - [x] 7.7 Atualizar sincronizadamente `_Sidebar` (NavigationRail) e `_BottomNavBar` (NavigationBar) em `main_layout.dart`:
    - Inserir o destino "Agenda de Proventos" no índice 7 (associando a `/dividends`).
    - Deslocar "Meu Perfil" para o índice 8 (associando a `/profile`).
    - Atualizar ambos os métodos `getSelectedIndex()` para garantir paridade total entre desktop e mobile.
  - [x] 7.8 Criar testes de widget em `frontend/test/features/dividends/presentation/pages/dividends_page_test.dart` validando renderização de lista, filtros, estados de carregamento e navegação.

- [x] **Task 8: Validação Geral de Qualidade, Logs e Análise Estática** (AC: 8)
  - [x] 8.1 Executar suíte completa de testes no backend: `mvn clean test` em todos os módulos (`common`, `api`, `valuation-service`).
  - [x] 8.2 Executar `flutter analyze` garantindo 0 erros e 0 warnings.
  - [x] 8.3 Executar `flutter test` garantindo 100% de sucesso nos testes de frontend.
  - [x] 8.4 Validar conformidade de todos os novos logs de API, schedulers e captura de exceções com as regras de `AGENTS.md`.

## Dev Notes

### Arquitetura e Padrões Obrigatórios

1. **Multi-Módulo Spring Boot e Clean Architecture:**
   - O módulo `common` deve conter os modelos de domínio (`DividendAnnouncement`, `RelevantFact`), enums (`CorporateEventType`), interfaces de repositório (`DividendAnnouncementPort`, `RelevantFactPort`, `DividendCrawlerPort`, `RelevantFactCrawlerPort`) e casos de uso de aplicação (`SyncCorporateEventsUseCase`, `GetDividendScheduleUseCase`, `GetRelevantFactsUseCase`).
   - O módulo `common` NÃO deve conter dependências de web scraping (como JSoup) ou clientes HTTP concretos.
   - O módulo `api` deve conter os adaptadores de persistência JPA, dependência `org.jsoup:jsoup:1.18.3`, adaptadores de crawler (`CvmOfficialCrawlerAdapter`, `BrapiCorporateEventsAdapter`), componentes de agendamento (`CorporateEventsScheduler`), controllers REST e configurações Spring.
   - O módulo `valuation-service` não necessita de alterações diretas nesta história.

2. **Reaproveitamento de Componentes Existentes:**
   - **Tickers Ativos:** Reutilizar a interface `CustodyTickerPort` (`afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort`), que já expõe `List<String> findDistinctActiveTickers()`.
   - **Custódia do Usuário:** Injetar `GetCustodyPositionsUseCase` (`afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase`) no `GetDividendScheduleUseCase`. Ao filtrar por `onlyMyTickers=true`, chamar `getCustodyPositionsUseCase.execute(userId)` para extrair a quantidade por ativo (`CustodyPositionDTO.quantity()`).
   - **Brapi Market Data:** Aproveitar a configuração existente do `RestClient` da Brapi configurado em `BrapiMarketDataAdapter`. Utilizar a chamada para `/quote/{tickers}?dividends=true` com o token já injetado via `@Value("${brapi.token:}")`.
   - **Autorização Administrativa:** Proteger os endpoints administrativos com a anotação existente `@RequireAdmin` (`afsdigital.grahamselect.api.auth.infrastructure.security.RequireAdmin`).

3. **Deduplicação, Idempotência e Unicidade Relacional:**
   - Na publicação inicial de proventos, a `payment_date` frequentemente é nula ("a definir"). Portanto, a restrição de unicidade na tabela `dividend_announcements` DEVE ser composta por `(ticker, com_date, type, rate)`.
   - Ao executar a sincronização, o repositório/adaptador verifica se já existe registro com essa chave composta:
     - Se NÃO existir: persiste o novo anúncio.
     - Se JÁ existir: atualiza os campos mutáveis (`payment_date`, `status`, `net_value`, `updated_at`).

4. **Regras de Negócio e Cálculo de Projeção:**
   - **JCP (Juros sobre Capital Próprio):** Retenção padrão de 15% de imposto de renda retido na fonte (IRRF) para pessoas físicas:
     $$\text{netValue} = \text{rate} \times 0.85$$
   - **Dividendos e Rendimentos de FIIs:** Isenção de IRRF para pessoas físicas:
     $$\text{netValue} = \text{rate}$$
   - **Projeção de Carteira (`estimatedTotal` e `estimatedNetTotal`):**
     $$\text{estimatedTotal} = \text{quantity} \times \text{rate}$$
     $$\text{estimatedNetTotal} = \text{quantity} \times \text{netValue}$$

5. **Crawler Resiliente e Testes Herméticos:**
   - Headers amigáveis no crawler: `User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 GrahamSelectCrawler/1.0`.
   - Isolamento de falhas: O crawler nunca deve interromper a varredura global em caso de falha em um ticker específico; registra log `WARN` e prossegue com os demais.
   - **Testes Unitários:** Todos os testes de crawler DEVEM utilizar mocks ou arquivos estáticos de teste em `src/test/resources` (ex: HTMLs simulados da CVM e JSONs simulados da Brapi). É estritamente proibido fazer chamadas HTTP externas reais durante a execução do Maven.

6. **Frontend Navigation Sync (`MainLayout`):**
   - Ambos os componentes `_Sidebar` e `_BottomNavBar` em `frontend/lib/src/core/widgets/main_layout.dart` devem ser mantidos estritamente alinhados:
     - Índice 0: `/dashboard`
     - Índice 1: `/evolution`
     - Índice 2: `/ranking`
     - Índice 3: `/allocation-strategy`
     - Índice 4: `/graham-recommendations`
     - Índice 5: `/upload`
     - Índice 6: `/metas`
     - Índice 7: `/dividends` (Agenda de Proventos)
     - Índice 8: `/profile` (Meu Perfil)
   - Atualizar tanto os `destinations` quanto os blocos `switch (index)` e a lógica `getSelectedIndex()`.

7. **Regras Transversais de Agentes (`AGENTS.md` e `project-context.md`):**
   - **UTC Obrigatório:** Toda e qualquer data/hora deve utilizar estritamente `ZoneOffset.UTC`.
   - **Logs Padronizados:** Registrar `INFO` na entrada de APIs e execuções do crawler, e `ERROR` com mensagem descritiva e stack trace completo (`log.error("...", e)`) em todos os blocos `catch`.
   - **Branch:** Antes da implementação, abrir uma nova branch (`feature/9-2-web-crawler-de-proventos-e-fatos-relevantes`).

### Project Structure Notes

- **Backend:**
  - `backend/api/pom.xml`: Adicionar dependência `org.jsoup:jsoup:1.18.3`.
  - `backend/common/src/main/resources/db/changelog/32-create-dividend-announcements-and-relevant-facts.yaml`: Migração Liquibase.
  - `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`: Inclusão do changeset 32.
  - `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/domain/entities/`: `DividendAnnouncement`, `RelevantFact`, `CorporateEventType`.
  - `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/repository/`: `DividendAnnouncementPort`, `RelevantFactPort`, `DividendCrawlerPort`, `RelevantFactCrawlerPort`.
  - `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/dto/`: `DividendScheduleDTO`, `RelevantFactDTO`, `CorporateEventSyncSummaryDTO`.
  - `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/usecase/`: `SyncCorporateEventsUseCase`, `GetDividendScheduleUseCase`, `GetRelevantFactsUseCase`.
  - `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/`: `CvmOfficialCrawlerAdapter`, `BrapiCorporateEventsAdapter`, `CompositeCorporateEventsCrawler`.
  - `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/`: `DividendAnnouncementJpaAdapter`, `RelevantFactJpaAdapter`, entidades JPA e repositórios.
  - `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/`: `CorporateEventsScheduler`, `CorporateEventsConfiguration`.
  - `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/web/`: `DividendController`.
  - `backend/api/src/main/java/afsdigital/grahamselect/api/admin/web/`: `AdminDividendCrawlerController`.
  - `backend/api/src/main/resources/openapi.yaml`: Atualização com contratos REST.
  - `backend/api/src/main/resources/application.yml`: Configurações de cron e timeouts.
- **Frontend:**
  - `frontend/lib/src/features/dividends/domain/entities/`: `dividend_announcement.dart`, `relevant_fact.dart`.
  - `frontend/lib/src/features/dividends/domain/repositories/`: `dividend_repository.dart`.
  - `frontend/lib/src/features/dividends/data/datasources/`: `dividend_remote_data_source.dart`.
  - `frontend/lib/src/features/dividends/data/models/`: `dividend_announcement_model.dart`, `relevant_fact_model.dart`.
  - `frontend/lib/src/features/dividends/data/repositories/`: `dividend_repository_impl.dart`.
  - `frontend/lib/src/features/dividends/presentation/providers/`: `dividend_provider.dart`.
  - `frontend/lib/src/features/dividends/presentation/pages/dividends_page.dart`.
  - `frontend/lib/src/features/dividends/presentation/widgets/`: `dividend_schedule_card.dart`, `relevant_fact_card.dart`, `dividend_summary_header.dart`.
  - `frontend/lib/src/core/widgets/main_layout.dart`: Destino sincronizado no `_Sidebar` e `_BottomNavBar` (índice 7).
  - `frontend/lib/main.dart`: Rota `/dividends` no `ShellRoute` e registro de providers.

### References

- PRD: [docs/bmad/planning-artifacts/prd.md#2-data-ingestion--integration-a-b3--beyond](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/prd.md) (Requisitos FR10, FR13)
- Epics: [docs/bmad/planning-artifacts/epics.md#epic-9-expansao-de-ingestao-de-dados-post-mvp](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/epics.md)
- Arquitetura: [docs/bmad/planning-artifacts/architecture.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/planning-artifacts/architecture.md)
- Regras de Agentes de IA: [AGENTS.md](file:///c:/Users/afssi/projects/graham-select/AGENTS.md)
- Contexto do Projeto: [docs/bmad/project-context.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/project-context.md)
- Custody Ticker Port: [backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/CustodyTickerPort.java](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/valuation/application/repository/CustodyTickerPort.java)
- Get Custody Positions Use Case: [backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java](file:///c:/Users/afssi/projects/graham-select/backend/common/src/main/java/afsdigital/grahamselect/common/portfolio/application/usecase/GetCustodyPositionsUseCase.java)
- Require Admin Guard: [backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/RequireAdmin.java](file:///c:/Users/afssi/projects/graham-select/backend/api/src/main/java/afsdigital/grahamselect/api/auth/infrastructure/security/RequireAdmin.java)
- Story 4.1 (Market Data & Schedulers): [docs/bmad/implementation-artifacts/4-1-market-data-indicator-integration.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/implementation-artifacts/4-1-market-data-indicator-integration.md)
- Story 3.3 (Histórico de Proventos e Aportes): [docs/bmad/implementation-artifacts/3-3-dividend-contribution-history.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/implementation-artifacts/3-3-dividend-contribution-history.md)
- Story 9.1 (Importação de Nota de Corretagem PDF): [docs/bmad/implementation-artifacts/9-1-importacao-de-nota-de-corretagem-pdf.md](file:///c:/Users/afssi/projects/graham-select/docs/bmad/implementation-artifacts/9-1-importacao-de-nota-de-corretagem-pdf.md)

## Dev Agent Record

### Agent Model Used

Gemini 3.8 Flash (Medium)

### Debug Log References

- Resolução de conflito de classes geradas pelo OpenAPI Generator: adicionada exclusão explícita de `DividendsApi*.java` no `maven-antrun-plugin` em `backend/api/pom.xml`.
- Ajuste de responsividade no header de `DividendsPage`: aplicação de `Expanded` no bloco de títulos prevenindo `RenderFlex overflow` em telas de viewport reduzida.
- Testes herméticos de MockMvc com autenticação JWT: adição de resolver dedicado para `@AuthenticationPrincipal Jwt` em testes standalone de controllers.

### Completion Notes List

- Adicionada biblioteca JSoup `1.18.3` em `backend/api/pom.xml` mantendo `common` puro de domínios.
- Criada migração Liquibase `32-create-dividend-announcements-and-relevant-facts.yaml` com restrição de unicidade composta `(ticker, com_date, type, rate)`.
- Implementados modelos de domínio `CorporateEventType`, `DividendAnnouncement` e `RelevantFact` no módulo `common` com cálculo automático de `netValue` (alíquota de 15% de IRRF para JCP e isenção para dividendos/rendimentos).
- Implementadas portas `DividendAnnouncementPort`, `RelevantFactPort`, `DividendCrawlerPort` e `RelevantFactCrawlerPort`.
- Implementados adaptadores JPA idempotentes `DividendAnnouncementJpaAdapter` e `RelevantFactJpaAdapter`.
- Implementados crawlers herméticos `BrapiCorporateEventsAdapter`, `CvmOfficialCrawlerAdapter` e o agregador resiliente `CompositeCorporateEventsCrawler` com fallback transparente por ticker.
- Implementado agendamento distribuído `CorporateEventsScheduler` com ShedLock `@SchedulerLock(name = "CorporateEventsScheduler_syncCorporateEvents")` rodando dias úteis às 21:00.
- Implementados casos de uso `SyncCorporateEventsUseCase`, `GetDividendScheduleUseCase` (calculando projeções com base na custódia) e `GetRelevantFactsUseCase`.
- Implementados controllers REST `DividendController` (`/api/v1/dividends/schedule`, `/api/v1/dividends/facts`) e administrativo `AdminDividendCrawlerController` (`POST /api/v1/admin/crawler/dividends/sync` protegido por `@RequireAdmin`).
- Atualizado contrato OpenAPI 3.0 com todos os novos schemas e rotas.
- Criada camada completa no Frontend Flutter: entidades, datasources, modelos, repositórios e `DividendProvider`.
- Criada tela completa `DividendsPage` (`/dividends`) com card de previsão para 30 dias (`Key('dividends_estimated_total_card')`), filtro "Meus Ativos" (`Key('dividends_only_my_assets_toggle')`), abas de Proventos e Fatos Relevantes, skeleton animado e empty states.
- Atualizado sincronizadamente `main_layout.dart` (`_Sidebar` e `_BottomNavBar`) com item de navegação no índice 7 e `/profile` no índice 8.
- 100% de sucesso em testes no backend (`mvn clean test`) e frontend (`flutter test`, `flutter analyze` com 0 erros e 0 warnings).

### File List

- `backend/api/pom.xml`
- `backend/api/src/main/resources/application.yml`
- `backend/api/src/main/resources/openapi.yaml`
- `backend/common/src/main/resources/db/changelog/32-create-dividend-announcements-and-relevant-facts.yaml`
- `backend/common/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/domain/entities/CorporateEventType.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/domain/entities/DividendAnnouncement.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/domain/entities/RelevantFact.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/repository/DividendAnnouncementPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/repository/RelevantFactPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/repository/DividendCrawlerPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/repository/RelevantFactCrawlerPort.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/dto/DividendScheduleDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/dto/RelevantFactDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/dto/CorporateEventSyncSummaryDTO.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/usecase/SyncCorporateEventsUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/usecase/GetDividendScheduleUseCase.java`
- `backend/common/src/main/java/afsdigital/grahamselect/common/dividends/application/usecase/GetRelevantFactsUseCase.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/dividends/domain/entities/DividendAnnouncementTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/dividends/application/usecase/SyncCorporateEventsUseCaseTest.java`
- `backend/common/src/test/java/afsdigital/grahamselect/common/dividends/application/usecase/GetDividendScheduleUseCaseTest.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/jpa/entities/DividendAnnouncementEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/jpa/entities/RelevantFactEntity.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/jpa/repositories/DividendAnnouncementJpaRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/jpa/repositories/RelevantFactJpaRepository.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/DividendAnnouncementJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/RelevantFactJpaAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/BrapiCorporateEventsAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/CvmOfficialCrawlerAdapter.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/CompositeCorporateEventsCrawler.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsScheduler.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsConfiguration.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/dividends/web/DividendController.java`
- `backend/api/src/main/java/afsdigital/grahamselect/api/admin/web/AdminDividendCrawlerController.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/DividendAnnouncementJpaAdapterTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/dividends/infrastructure/persistence/RelevantFactJpaAdapterTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/BrapiCorporateEventsAdapterTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/CvmOfficialCrawlerAdapterTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/dividends/infrastructure/crawler/CompositeCorporateEventsCrawlerTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/dividends/infrastructure/spring/CorporateEventsSchedulerTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/dividends/web/DividendControllerTest.java`
- `backend/api/src/test/java/afsdigital/grahamselect/api/admin/web/AdminDividendCrawlerControllerTest.java`
- `frontend/lib/src/features/dividends/domain/entities/dividend_announcement.dart`
- `frontend/lib/src/features/dividends/domain/entities/relevant_fact.dart`
- `frontend/lib/src/features/dividends/domain/repositories/dividend_repository.dart`
- `frontend/lib/src/features/dividends/data/models/dividend_announcement_model.dart`
- `frontend/lib/src/features/dividends/data/models/relevant_fact_model.dart`
- `frontend/lib/src/features/dividends/data/datasources/dividend_remote_data_source.dart`
- `frontend/lib/src/features/dividends/data/repositories/dividend_repository_impl.dart`
- `frontend/lib/src/features/dividends/presentation/providers/dividend_provider.dart`
- `frontend/lib/src/features/dividends/presentation/widgets/dividend_summary_header.dart`
- `frontend/lib/src/features/dividends/presentation/widgets/dividend_schedule_card.dart`
- `frontend/lib/src/features/dividends/presentation/widgets/relevant_fact_card.dart`
- `frontend/lib/src/features/dividends/presentation/pages/dividends_page.dart`
- `frontend/lib/src/core/widgets/main_layout.dart`
- `frontend/lib/main.dart`
- `frontend/test/features/dividends/presentation/providers/dividend_provider_test.dart`
- `frontend/test/features/dividends/data/repositories/dividend_repository_impl_test.dart`
- `frontend/test/features/dividends/presentation/pages/dividends_page_test.dart`
- `docs/bmad/implementation-artifacts/sprint-status.yaml`
- `docs/bmad/implementation-artifacts/9-2-web-crawler-de-proventos-e-fatos-relevantes.md`

### Change Log

- **2026-11-15**: Implementação completa da Story 9.2: Web Crawler de Proventos e Fatos Relevantes, cobrindo persistência relacional com Liquibase changeset 32, portas e use cases no `common`, crawlers herméticos (Brapi + CVM) no `api`, agendamento com ShedLock, endpoints REST (`/schedule`, `/facts`, `/sync`), interface Flutter responsiva (`/dividends`), sincronização da barra de navegação no `MainLayout` e validação com 100% de cobertura nos testes e análise estática.

