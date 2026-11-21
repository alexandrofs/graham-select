# Addendum: Kerno (ex-Graham Select 2.0)

Este adendo registra especificações técnicas, premissas algorítmicas, alternativas descartadas e pontos de handoff para o PRD e a Arquitetura.

---

## 1. Especificação Técnica: Diagrama do Cerrado

### 1.1 Estrutura de Alocação e Templates Iniciais
- **Templates Pré-configurados (Decisão Confirmada):** Para eliminar a fricção de "tela em branco", o sistema entregará perfis padrão editáveis (ex.: 40% Ações BR, 30% FIIs, 20% Renda Fixa / Tesouro, 10% Internacional) e notas iniciais de governança para os principais ativos da B3.
- **Hierarquia de 3 Níveis:**
  1. *Nível 1 (Macro Classes):* Meta percentual alvo por classe de ativos.
  2. *Nível 2 (Subclasses / Segmentos):* Ações (setores defensivos vs. crescimento) e FIIs (Tijolo vs. Papel).
  3. *Nível 3 (Ativo Individual & Nota 1–10):* Pontuação baseada em fundamentos (governança, histórico de lucros, endividamento etc.) que define o peso/teto máximo do ativo dentro da classe (*[A definir no PRD: mapeamento exato da nota 1–10 para peso proporcional vs. faixas fixas de teto]*).

### 1.2 Algoritmo de Sugestão de Aporte
- **Entrada:** Valor disponível para aporte no mês (`R$ X`).
- **Cálculo de Defasagem por Classe/Ativo:**

$$\Delta_i = (\text{Patrimônio Total} + X) \times \text{Meta}_i\% - \text{Valor Atual}_i$$

- **Regras de Priorização e Distribuição:**
  - Classes ou ativos com $\Delta_i \le 0$ (acima da meta) são ignorados no aporte, mantendo a disciplina de não vender posições vencedoras.
  - A priorização ordena as classes de ativos com maior defasagem positiva (*[A definir no PRD: ordenação por gap percentual relativo vs. gap absoluto em R$]*).
  - **Regra de Transbordamento (*Spillover*):** Caso `R$ X` exceda o $\Delta_i$ da classe/ativo mais defasado ou atinja o teto de exposição de risco do ativo líder, o saldo remanescente transborda automaticamente para o próximo ativo/classe elegível.
  - **Integração com Benjamin Graham:** Aplicável exclusivamente à classe de Ações; ativos elegíveis que também apresentem margem de segurança pelo filtro de Graham recebem um multiplicador de prioridade (*[A definir no PRD: fator de ponderação do multiplicador]*).
- **Saída para a UI:** Distribuição objetiva do aporte (ex.: *"Aloque R$ A em [Ativo 1] e R$ B em [Ativo 2] para aproximar sua carteira do equilíbrio ideal"*).

---

## 2. Diretrizes Técnicas de Design System (Intergeracional)

- **Contraste e Paleta:** Relação de contraste mínima **WCAG 2.1 AA** (4.5:1 para texto normal; 3:1 para texto grande e componentes gráficos), com fundos limpos, escala neutra equilibrada e cor de acento institucional + verde semântico para aportes/crescimento.
- **Tipografia e Acessibilidade:** Suporte nativo ao *Dynamic Type* / `textScaler` do sistema operacional (Android/iOS) para leitores maduros; fontes com numerais tabulares de alta legibilidade (`Inter` ou `Plus Jakarta Sans` via `google_fonts`).
- **Hierarquia Visual:** Cards arredondados com bordas sutis em vez de tabelas densas em telas pequenas, com privacidade de valores (*toggle* de ocultação) no topo do dashboard.

---

## 3. Âncoras Arquiteturais Mobile & Expansões Futuras

- **Segurança & Sessão (Decisão Confirmada):** Reuso de Google Sign-In / OAuth2 + JWT com armazenamento seguro de tokens via `flutter_secure_storage` e desbloqueio biométrico nativo (FaceID / Digital).
- **Modularidade:** Estruturar os módulos isolados `features/expenses` e `features/ai_assistant` no Flutter e seus respectivos contextos/pacotes no backend.
- **Contrato de Navegação:** `BottomNavigationBar` controlada por *feature flags*, iniciando com *Carteira* e *Aportes* e preparada para habilitar *Gastos* e *Kerno AI* sem refatoração de rotas.
- **Integração de Fluxo de Caixa (Futuro):** O saldo livre mensal ($\text{Entradas} - \text{Saídas}$) alimentará diretamente o campo `R$ X` do motor do Diagrama do Cerrado.

---

## 4. Decisão de Rebranding & Racional de Alternativas Descartadas

- **Nome Escolhido:** **Kerno** (ex-*Graham Select*).
  - *Racional:* Nome neutro (não limita o produto à bolsa nem a uma única metodologia), curto (5 letras, 2 sílabas), fácil pronúncia intergeracional, sem colisão relevante no mercado financeiro brasileiro e com forte sinergia para IA (*Kerno AI*, *Kerno Insights*).
- **Alternativa Finalista Descartada:** **Aura**.
  - *Motivo do descarte:* Levantamento de mercado apontou alta saturação no Brasil em finanças e IA (*Aura Financeira*, *Aura Lembretes* no WhatsApp, *GetAura* e *Aura Finance* na Play Store), elevando riscos de confusão de marca, disputa de SEO e colisão no INPI.
- **Outras Alternativas Avaliadas e Não Selecionadas:** *Lumina*, *Plano*, *Nexus*, *Valora*, *Synap*, *Aloca*, *Cerrado Invest* (estas duas últimas descartadas por restringirem o escopo a investimentos).
