# Addendum: Graham Select 2.0

Este adendo preserva detalhes técnicos, regras de negócio e contextos de produto que apoiam a transição do Product Brief para o PRD, Arquitetura e Histórias de Desenvolvimento.

---

## 1. Regras do Mecanismo Diagrama do Cerrado

### 1.1 Premissas Conceituais (Buy & Hold Raiz)
- **Não vender para rebalancear:** Ao contrário de modelos de markowitz ou rebalanceamento fixo semestral com giro de ativos, o Diagrama do Cerrado foca 100% em canalizar o **novo aporte financeiro** para ativos que ficaram para trás em relação à sua meta.
- **Hierarquia de Alocação:**
  1. *Nível 1 (Macro Classes):* Alocação percentual desejada entre classes (ex: 40% Ações Brasil, 30% FIIs, 20% Renda Fixa / Tesouro, 10% Internacional).
  2. *Nível 2 (Subclasses ou Segmentos):* Dentro de Ações: Exposição balanceada entre setores defensivos e de crescimento. Dentro de FIIs: Tijolo vs. Papel.
  3. *Nível 3 (Ativo Individual & Nota de Qualidade/Risco):* Cada ativo recebe uma pontuação (1 a 10) baseada em fundamentos (governança, histórico de lucros, endividamento, etc.). A nota determina o teto percentual que aquele ativo pode atingir dentro da sua classe.

### 1.2 Algoritmo de Sugestão de Aporte
- **Input:** Saldo disponível para aporte este mês ($ R\$ X $).
- **Cálculo da Distância da Meta:**
  $$\Delta_i = (\text{Patrimônio Total} + X) \times \text{Meta}_i\% - \text{Valor Atual}_i$$
- **Priorização:**
  - Identificar a classe de ativo mais defasada ($\Delta$ positivo mais distante).
  - Dentro da classe escolhida, identificar os ativos de maior qualidade cuja exposição ainda não atingiu o teto de risco.
  - Se um ativo da classe com boa nota também figurar na lista de oportunidades com margem de segurança de Benjamin Graham, ganha multiplicador de prioridade.
- **Output para a UI:** "Recomendamos alocar R$ A em [Ativo 1] e R$ B em [Ativo 2] para aproximar sua carteira do equilíbrio ideal".

---

## 2. Diretrizes de UX/UI inspiradas no Nubank (Intergeracional)

- **Cores & Contraste:** 
  - Fundo limpo, paleta com tons sóbrios, cinzas quentes/frios equilibrados e uma cor de acento vibrante e confiável (roxo/azul sofisticado e verde de crescimento/aportes).
  - Relação de contraste mínima WCAG 2.1 AA (4.5:1 para texto normal, 3:1 para texto grande e componentes gráficos).
- **Tipografia:**
  - Escala tipográfica generosa com suporte nativo ao Dynamic Type / text scaling do sistema operacional do smartphone para pessoas maduras ou com dificuldades visuais.
  - Fontes com alta legibilidade em números e dados financeiros (ex: Google Fonts Inter ou Plus Jakarta Sans).
- **Cards e Informação Gradual:**
  - Valores patrimoniais visíveis com opção de ocultar ("olho" / toggle de privacidade).
  - Cards arredondados com hierarquia suave de sombras ou bordas sutis, sem poluição de dezenas de tabelas em tela pequena.
  - Feed de ações simples: "Seu patrimônio", "Sua meta", "Seu aporte do mês".

---

## 3. Preparação Arquitetural para o Módulo de Controle de Gastos (Futuro)

- **Desacoplamento em Clean Architecture:**
  - Criar o subdomínio `features/expenses` isolado no Flutter e backend.
  - O MVP Mobile deve ter navegação inferior (`BottomNavigationBar`) já configurada com rotas que permitam habilitar a aba "Gastos" via feature toggle ou atualização sem quebrar a navegação existente de "Carteira" e "Aportes".
- **Modelo de Dados Unificado:**
  - O fluxo de caixa (Entradas - Saídas = Disponível para Aporte) alimentará automaticamente a caixa de entrada do Diagrama do Cerrado em etapas futuras.
