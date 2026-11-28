# Epic 3 Context: Alocação Estratégica pelo Diagrama do Cerrado

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Estruturar a estratégia patrimonial de longo prazo do investidor através da hierarquia de 3 níveis do Diagrama do Cerrado (Macro Classes, Subclasses e Notas de Qualidade de 1 a 10), eliminando a paralisia de tela em branco por meio de templates pré-configurados e garantindo disciplina de risco via validação estrita de soma 100%, Teto Máximo de Risco e Quarentena de Aporte. Isso estabelece a base determinística e livre de viés editorial para que o GPS de Aporte e o Kerno AI operem com precisão matemática e conformidade regulatória CVM.

## Stories

- Story 3.1: Modelo de Dados Relacional e Migrations para o Diagrama do Cerrado
- Story 3.2: Motor de Domínio da Hierarquia do Cerrado, Aritmética de Metas e Redistribuição de Teto
- Story 3.3: API REST para Gestão de Metas de Macro Classes e Templates de Onboarding
- Story 3.4: Interface do Diagrama do Cerrado: Seletor de Templates e Trava de Metas
- Story 3.5: Gestão Visual de Notas de Qualidade, Slider 48dp, Assistente de 5 Critérios e Quarentena

## Requirements & Constraints

- **5 Macro Classes e Soma Estrita:** Suportar exatamente *Ações BR*, *FIIs*, *Renda Fixa / Tesouro*, *Internacional* e *Criptoativos*. A soma das metas percentuais deve totalizar impreterivelmente `100.00%`; qualquer divergência deve ser bloqueada na UI e rejeitada pela API com HTTP `422 Unprocessable Entity`.
- **Templates de Onboarding Sem Viés:** Disponibilizar 3 perfis (*Modelo Equilibrado Cerrado* com 1% Cripto, *Modelo Moderado Patrimonial* com 0% Cripto e 45% Renda Fixa, e *Modelo Acumulação Arrojada* com 5% Cripto) sem pré-seleção automática e com disclaimer CVM visível. Todos os ativos iniciam com Nota de Qualidade neutra padrão igual a `6`.
- **Cálculo Proporcional e Teto Máximo de Risco:** A meta bruta do ativo `i` na Macro Classe `C` segue `(Nota_i / soma das Notas elegíveis de C) * Meta_C%`. Ativos de risco emissor único (incluindo Criptoativos diretos e ETFs de Cripto) respeitam teto individual sobre o patrimônio total: notas `8–10` até `10%`, notas `5–7` até `6%`, e notas `1–4` entram em **Quarentena de Aporte** (`0%` para novos aportes, excluídas do denominador da classe sem forçar venda do patrimônio existente). Excedentes de teto são redistribuídos iterativamente de forma proporcional à nota entre os ativos elegíveis não-saturados da mesma Macro Classe até atingir `Meta_C%`.
- **Isenções de Teto:** Títulos públicos soberanos (*Tesouro Direto*) e ETFs de índices amplos (`is_diversified_broad_etf = true`, ex.: `BOVA11`, `IVVB11`, `WRLD11`) são isentos do teto individual, limitando-se apenas à meta da própria Macro Classe.
- **Aritmética Decimal Estrita:** Proibido uso de `float`/`double`. Todos os cálculos no domínio usam `BigDecimal` com `RoundingMode.HALF_EVEN` (escala 4 intermediária, escala 2 em saídas) e colunas `NUMERIC(5,2)` / `NUMERIC(19,4)` no banco.
- **Isolamento, Auditoria e Cache:** Isolamento estrito por `user_id` autenticado, registro de logs estruturados sem PII/tokens e invalidação imediata do cache de diagnóstico de IA (`portfolio_state_hash`) ao alterar metas ou notas.

## Technical Decisions

- **Arquitetura Hexagonal no Backend:** O motor matemático (`CerradoAllocationEngine`) reside como serviço de domínio puro e imutável em `backend/common` (`domain/cerrado`), desacoplado de frameworks web e persistência; controllers REST (`/api/v1/cerrado/templates`, `/api/v1/cerrado/preferences`, `/api/v1/cerrado/grades`) e adaptadores JPA (`CerradoPreferencesEntity`, `CerradoAssetGradeEntity`) ficam em `backend/api`.
- **Subclasses Analíticas:** O Nível 2 (Subclasses) utiliza enums fechados controlados no backend (com fallback `OUTROS_MANUAL`) para classificação analítica sem alterar a fórmula de peso intra-classe.
- **Migrations Liquibase Imutáveis:** Criar novos changesets incrementais e idempotentes (`db.changelog-2.0-cerrado.xml`) para `tb_cerrado_preferences` e `tb_cerrado_asset_grade` com índices por `user_id` e integridade referencial, sem jamais editar migrations já aplicadas e conferindo os nomes reais de tabelas/FKs existentes no changelog master.
- **Frontend Feature-First:** Módulo isolado em `frontend/lib/features/cerrado_diagram/` com `Provider` e `GoRouter` na rota `/allocation-strategy` (Aba 4: Diagrama).
- **Testes e LGPD:** Validar persistência, soma 100% e isolamento multi-tenant na suíte de integração `CerradoPreferencesIT`.

## UX & Interaction Patterns

- **Seletor de Templates (`CerradoTemplateSelectorCard`):** Exibir os 3 cartões sem pré-seleção automática e com disclaimer regulatório de ausência de suitability/recomendação.
- **Trava Visual de Soma 100%:** Ao editar percentuais das Macro Classes e a soma diferir de `100.00%`, exibir badge carmesim com a diferença exata faltante/excedente e desabilitar o botão salvar; ao atingir `100.00%`, habilitar o botão com destaque esmeralda.
- **Gestão de Notas (`CerradoQualityCard`):** Slider direto `1–10` com alvo de toque mínimo de `48x48dp` exibindo instantaneamente o Teto Máximo de Risco, sincronizado com o modal opcional *"Assistente de Autoavaliação"* de 5 critérios objetivos (*Governança*, *Lucro 5 anos*, *Endividamento*, *Vantagem Competitiva*, *Histórico de Proventos*, pontuados em `0`, `1` ou `2` cada, sem auditoria da plataforma).
- **Estado de Quarentena:** Notas `1–4` destacam a trilha do slider em âmbar e exibem badge acessível (cor + ícone + texto) *"Em Quarentena de Aporte — Novos Aportes = 0%"*. Contas `FREE` pós-trial operam em modo *read-only* preservando as notas configuradas.
- **Acessibilidade Intergeracional:** Numerais tabulares obrigatórios (`tabular-nums` / `FontFeature.tabularFigures()`), contraste mínimo `4.5:1` (`7.0:1` AAA em métricas/alertas) e suporte a `textScaler` até `2.0x` com reflow vertical (proibido `TextOverflow.ellipsis` em cifras e tickers).

## Cross-Story Dependencies

- **Internas:** Stories 3.1 (esquema/migrations) e 3.2 (motor de domínio) são pré-requisitos para a API REST da Story 3.3, que suporta as interfaces das Stories 3.4 e 3.5.
- **Externas:** Consome ativos e posições consolidadas da carteira (Epic 2); fornece metas efetivas, tetos e quarentenas como entrada do GPS de Aporte (Epic 4, incluindo flag `quarantine_corporate_event`) e do Kerno AI (Epic 5); novas tabelas devem ser incluídas no expurgo em cascata do `UserDataPurgeAdapter` e cobertas em `AccountDeletionIT` (Epic 5 / LGPD).
