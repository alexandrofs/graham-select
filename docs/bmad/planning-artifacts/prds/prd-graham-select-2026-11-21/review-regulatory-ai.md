# Regulatory & AI Compliance Review — Kerno (ex-Graham Select 2.0)

## Perspective
Auditor especialista em conformidade regulatória perante a Comissão de Valores Mobiliários (Resoluções CVM nº 19/2020 e nº 20/2020), Lei Geral de Proteção de Dados (LGPD — Lei nº 13.709/2018 / Resolução CD/ANPD nº 19/2024 sobre transferência internacional) e segurança em sistemas de Inteligência Artificial aplicada ao setor financeiro.

## Executive Verdict
O PRD do Kerno apresenta um dos modelos de conformidade mais rigorosos e bem desenhados observados em projetos de tecnologia financeira. O isolamento estrito entre dados pessoais e motores de inteligência artificial, o posicionamento legal como calculadora matemática parametrizada pelo investidor e os mecanismos de mitigação contra alucinações e recomendações imperativas de investimento estão completamente blindados por arquitetura e testes automatizados. Veredito: **Conforme / Aprovado sem ressalvas impeditivas**.

---

## Pillar Assessment

### 1. Enquadramento CVM (Resoluções nº 19 e nº 20)
- **Status:** Conforme.
- **Racional:**
  - A plataforma não emite relatórios de análise de valores mobiliários nem realiza consultoria personalizada de investimentos. O algoritmo do GPS de Aporte apenas executa simulações matemáticas calculadas a partir de parâmetros que o próprio usuário define (metas de Macro Classes e Notas de Qualidade 1–10).
  - O ponto de partida nos Templates de Alocação inicializa todos os ativos com uma Nota de Qualidade neutra padrão igual a `6`, eliminando qualquer suspeita de curadoria ou viés editorial por parte do Kerno.
  - O validador léxico pós-geração do Kerno AI opera com limites contextuais de modo imperativo financeiro, bloqueando expressões como "compre", "aporte em", "reduza" ou "lucro garantido", acompanhado de aviso regulatório fixo em todas as interfaces.

### 2. Proteção de Dados Pessoais (LGPD) e Anonimização
- **Status:** Conforme.
- **Racional:**
  - O **Sanitizador LGPD de IA** (`FR-10`) remove 100% de nomes, CPFs, e-mails, telefones, números de conta e valores monetários absolutos em `R$` antes de qualquer requisição externa ao LLM.
  - A sanitização estende-se a ativos manuais: textos livres que não passem nas allowlists de tickers da B3 (`^[A-Z][A-Z0-9]{3}[0-9]{1,2}$`), catálogos internacionais ou tabelas oficiais do Tesouro Direto são substituídos por identificadores canônicos anonimizados (`RENDA_FIXA_MANUAL_1`, `INTERNACIONAL_MANUAL_1`, `CRIPTO_MANUAL_1`), neutralizando potenciais injeções de prompt ou vazamento de PII em descrições de ativos.
  - A integração de produção utiliza **Google Cloud Vertex AI / Gemini Enterprise** sob contrato empresarial com cláusula de **Zero Data Retention (ZDR)**, DPA e proibição explícita de uso de dados para treinamento de modelos de terceiros.
  - O direito ao esquecimento e autodeterminação informativa é cumprido integralmente pelo `UserDataPurgeAdapter` (cancelamento de jobs assíncronos e hard-delete de todas as tabelas e caches), coberto de ponta a ponta pelo teste de integração `AccountDeletionIT` (`FR-16`).

### 3. Integridade e Ancoragem Numérica do LLM
- **Status:** Conforme.
- **Racional:**
  - A arquitetura adota a metodologia *Deterministic-First*: toda a matemática é processada antecipadamente em `BigDecimal` no backend Java. O LLM atua estritamente como redator e sintetizador das conclusões analíticas.
  - O validador pós-geração confere cada número e percentual citado pelo LLM contra o snapshot de entrada com tolerância estrita de `±0,1 p.p.` e tolerância zero para tickers inventados.
  - Caso haja qualquer violação de ancoragem numérica ou barreira regulatória, o sistema degrada graciosamente para o diagnóstico quantitativo determinístico, sem realizar retentativas síncronas que pudessem inflar custos ou expor o usuário a novas alucinações.

---

## Detailed Findings
- Nenhum achado crítico, alto ou médio identificado nesta revisão.
