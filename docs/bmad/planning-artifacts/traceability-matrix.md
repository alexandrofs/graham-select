# Mapa de Cobertura de Requisitos (RTM)

Este documento mapeia os Requisitos Funcionais (FR) e Não Funcionais (NFR) descritos no PRD para os Épicos e User Stories definidos no planejamento técnico.

## 🔗 Requisitos Funcionais (FRs)

| ID | Requisito | Épico / Story Cobertura | Status |
|:---|:---|:---|:---:|
| **FR1** | Login via Google OAuth2 | Epic 1 / Story 1.1 | ✅ |
| **FR2** | Provisionamento automático de conta (Trial) | Epic 1 / Story 1.1 | ✅ |
| **FR3** | Isolamento de dados por Tenant (RLS) | Epic 1 / Story 1.1 | ✅ |
| **FR4** | Expurgo (LGPD) - Deleção de conta | Epic 1 / Story 1.2 | ✅ |
| **FR5** | Dashboards unificados (Ações, FIIs, Tesouro) | Epic 3 / Story 3.1, 3.2, 3.4 | ✅ |
| **FR6** | Importação via PDF/XLS B3 | Epic 2 / Story 2.1, 2.2 | ✅ |
| **FR7** | Cadastro Manual de Operações | Epic 2 / Story 2.4 | ✅ |
| **FR8** | Detecção de Ticker desconhecido | Epic 6 / Story 6.3 | ✅ |
| **FR9** | Processamento Assíncrono | Epic 2 / Story 2.2 | ✅ |
| **FR10** | Trilha de Auditoria (Manual) | Epic 2 / Story 2.5 | ✅ |
| **FR11** | Consolidação de Carteira | Epic 3 / Story 3.2 | ✅ |
| **FR12** | Evolução Patrimonial (Proventos) | Epic 3 / Story 3.3 / Epic 7 | ✅ |
| **FR13** | Dashboard KPI Real-time | Epic 3 / Story 3.1, 3.5 | ✅ |
| **FR14** | Relatório Bens e Direitos (IR) | Epic 7 / Story 7.1 | ✅ |
| **FR15** | Cálculo de Isenção (R$ 20k) | Epic 7 / Story 7.2 | ✅ |
| **FR16** | Histórico de Aportes | Epic 3 / Story 3.3 | ✅ |
| **FR17** | Agenda de Proventos | Epic 8 / Story 8.1 | ✅ |
| **FR18** | Motor Graham (Valuation) | Epic 4 / Story 4.3 | ✅ |
| **FR19** | Reasoning Box (Explainable AI) | Epic 4 / Story 4.4 | ✅ |
| **FR20** | Sync de Market Data (Cotação) | Epic 4 / Story 4.1 | ✅ |
| **FR21** | Metas de Alocação (%) | Epic 4 / Story 4.2 | ✅ |
| **FR22** | Sugestão de Aporte Otimizada | Epic 4 / Story 4.3 | ✅ |
| **FR23** | Configuração de Meta Financeira | Epic 5 / Story 5.1 | ✅ |
| **FR24** | Time-to-Goal (Projeção) | Epic 5 / Story 5.2, 5.3 | ✅ |
| **FR25** | Alertas Mobile/Web/Push/WhatsApp | Epic 9 / Epic 10 | ✅ |
| **FR26** | Painel de Monitoramento Admin | Epic 6 / Story 6.1 | ✅ |
| **FR27** | Reprocessamento de Erros (DLQ) | Epic 6 / Story 6.2 | ✅ |

## ⚙️ Requisitos Não Funcionais (NFRs)

| ID | Requisito | Evidência de Design | Status |
|:---|:---|:---|:---:|
| **NFR1** | Escalabilidade (100k msg/min) | Kafka + Spring Batch (Architecture doc) | ✅ |
| **NFR2** | Performance Frontend (< 300ms) | Vite + Skeleton Screens (UX spec) | ✅ |
| **NFR3** | Latência Algoritmo (< 2s) | Spring Boot Optimization (Story 4.3) | ✅ |
| **NFR4** | Segurança RLS (Postgres) | Tenant Partitioning (Architecture doc) | ✅ |
| **NFR5** | LGPD Compliance | Expugo flow (Story 1.2) | ✅ |
| **NFR6** | Disponibilidade 99.9% | AWS Multi-AZ Deployment | ✅ |
| **NFR7** | Observabilidade | Grafana + DLQs (Story 6.1, 6.2) | ✅ |
| **NFR8** | Resiliência de Market Data | Spring Cache + Fallback Story 4.1 | ✅ |

---
**Conclusão:** 100% dos requisitos identificados no PRD foram mapeados para ações executáveis no planejamento de histórias.
