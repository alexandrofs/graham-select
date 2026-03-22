# Mapa de Cobertura de Requisitos (RTM)

Este documento mapeia os Requisitos Funcionais (FR) e Não Funcionais (NFR) descritos no PRD para os Épicos e User Stories definidos no planejamento técnico.

## 🔗 Requisitos Funcionais (FRs)

| ID | Requisito | Épico / Story Cobertura | Status |
|:---|:---|:---|:---:|
| **FR1** | Login via Google OAuth2 | Epic 1 / Story 1.1 | ✅ |
| **FR2** | Tier de Assinatura (Free/Premium) | Epic 1 / Story 1.2, 1.3 | ✅ |
| **FR3** | Multi-tenancy (Isolamento RLS) | Epic 1 / Story 1.3 | ✅ |
| **FR4** | Holding Familiar (Multi-CPF) | Epic 7 / Story 7.1 | ✅ |
| **FR5** | LGPD (Exclusão/Expurgo) | Epic 1 / Story 1.4 | ✅ |
| **FR6** | Upload de Arquivos B3 (Excel) | Epic 2 / Story 2.1 | ✅ |
| **FR7** | Processamento Background (Worker) | Epic 2 / Story 2.2 | ✅ |
| **FR8** | Status de Importação (Feedbacks) | Epic 2 / Story 2.3 | ✅ |
| **FR9** | Importação de Notas (PDF) | Epic 9 / Story 9.1 | ✅ |
| **FR10** | Ativos Internacionais & Cripto | Epic 9 / Story 9.2 | ✅ |
| **FR11** | Conector Open Finance | Epic 9 / Story 9.3 | ✅ |
| **FR12** | Visualização de Custódia (Grid) | Epic 3 / Story 3.1, 3.2 | ✅ |
| **FR13** | Evolução Balanço de Proventos | Epic 3 / Story 3.3 | ✅ |
| **FR14** | Tags Customizadas de Carteira | Epic 8 / Story 8.1 | ✅ |
| **FR15** | Filtros Dinâmicos por Tag/CPF | Epic 8 / Story 8.1 | ✅ |
| **FR16** | Cadastro Manual de Ativos | Epic 2 / Story 2.4 | ✅ |
| **FR17** | Trilha de Auditoria | Epic 2 / Story 2.5 | ✅ |
| **FR18** | API Integrada de Cotações | Epic 4 / Story 4.1 | ✅ |
| **FR19** | API de Fundamentos (P/L, LPA) | Epic 4 / Story 4.1 | ✅ |
| **FR20** | Definição de Metas de Alocação | Epic 4 / Story 4.2 | ✅ |
| **FR21** | Motor Algorítmico Graham | Epic 4 / Story 4.3 | ✅ |
| **FR22** | Explainable AI (Reasoning Box) | Epic 4 / Story 4.4 | ✅ |
| **FR23** | Configuração de Metas Longo Prazo | Epic 5 / Story 5.1 | ✅ |
| **FR24** | Time-to-Goal (Projeção Visual) | Epic 5 / Story 5.2 | ✅ |
| **FR25** | Notificações Push/WhatsApp | Epic 11 / Story 11.1, 11.2 | ✅ |
| **FR26** | Painel de Monitoramento Admin | Epic 6 / Story 6.1 | ✅ |
| **FR27** | Reprocessamento de Erros (DLQ) | Epic 6 / Story 6.2 | ✅ |

## ⚙️ Requisitos Não Funcionais (NFRs)

| ID | Requisito | Épico / Story / Design | Status |
|:---|:---|:---|:---:|
| **NFR1** | Resiliência (Kafka/Sync) | Epic 2 (Pipeline Assíncrono) | ✅ |
| **NFR2** | Latência Frontend (< 1s) | Design UX (Vite + Skeleton) | ✅ |
| **NFR3** | Latência Graham (< 2s) | Epic 4 (Valuation Algorithm) | ✅ |
| **NFR4** | Disponibilidade 99.9% | Infra Cloud (Multi-AZ) | ✅ |
| **NFR5** | LGPD Expurgo < 24h | Story 1.4 (Lógica de Deleção) | ✅ |
| **NFR6** | Segurança (Audit Logs) | Story 1.1 e 2.5 | ✅ |
| **NFR7** | Processamento Assíncrono | Epic 2 e Kafka Architecture | ✅ |
| **NFR8** | Caching de Cotações | Story 4.1 (Caffeine/Redis) | ✅ |

---
**Conclusão:** Cobertura de 100% (27 FRs + 8 NFRs) validada entre PRD e Planejamento de Épicos.
