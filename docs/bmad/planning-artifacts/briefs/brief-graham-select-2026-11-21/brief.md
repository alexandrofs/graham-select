---
title: "Product Brief: Graham Select 2.0 (Modernização, App Mobile & Diagrama do Cerrado)"
status: "draft"
created: "2026-11-21"
updated: "2026-11-21"
author: "Alex"
---

# Product Brief: Graham Select 2.0

## Executive Summary

O **Graham Select 2.0** representa o salto de maturidade da plataforma de inteligência patrimonial Graham Select. Nascido para eliminar o atrito de planilhas e a ansiedade de aportes às cegas com base nos fundamentos de Benjamin Graham, o produto agora expande sua proposta de valor para dois novos horizontes: **acessibilidade multiplataforma com experiência de uso de ponta** e **rebalanceamento patrimonial ativo guiado pelo Diagrama do Cerrado**.

A versão 2.0 entrega um frontend web e mobile unificado, inspirado no padrão de clareza, elegância e acessibilidade cognitiva do Nubank — perfeitamente equilibrado para acolher jovens investidores dinâmicos e investidores mais maduros que exigem legibilidade, sobriedade e hierarquia direta. Em paralelo ao poder analítico da web, é introduzido o **MVP Mobile**, concebido como um companheiro de bolso instantâneo: login seguro, dashboard da carteira consolidada e a lista de sugestões de aporte calculada em segundos. Ao adotar o Diagrama do Cerrado, o Graham Select resolve a maior dúvida do investidor de longo prazo: *onde colocar exatamente o dinheiro do novo aporte este mês sem girar patrimônio e sem pagar imposto de venda à toa*.

---

## The Problem

1. **Atrito Cognitivo e Ruído Visual:** Plataformas tradicionais de investimento (e interfaces web sobrecarregadas) distanciam dois públicos cruciais: jovens que esperam interfaces minimalistas e rápidas, e investidores experientes ou maduros que se irritam com densidade visual excessiva, fontes diminutas e fluxos labirínticos.
2. **A Paralisia do Rebalanceamento:** Mesmo com a carteira consolidada via B3, investidores sofrem ao decidir onde colocar o novo dinheiro do mês. Rebalanceamentos acadêmicos tradicionais mandam "vender o que subiu para comprar o que caiu", gerando eventos tributários indesejados (DARF) e desfazendo posições em empresas excelentes. 
3. **Falta de Presença Mobile de Bolso:** No momento em que o investidor está na corretora ou recebendo o salário, abrir o computador para consultar planilhas ou sistemas web pesados adiciona atrito. O usuário precisa de uma visão sintetizada na palma da mão em menos de 10 segundos.
4. **Landing Page Desconectada da Nova Identidade:** A página de entrada atual reflete a versão inicial focada apenas no ranking quantitativo simples, sem demonstrar a sofisticação do acompanhamento de carteira, a inteligência de alocação e o futuro app móvel.

---

## The Solution

O Graham Select 2.0 se estrutura em quatro pilares de evolução:

1. **Design System Inspirado no Nubank (Intergeracional):**
   - Redesenho completo da identidade visual: fundos limpos com contraste WCAG AA/AAA, tipografia generosa e escalável, cartões de métricas fluidos e redução drástica de ruído visual.
   - Navegação direta que respeita a clareza para públicos maduros (tabelas legíveis, números destacados, sem termos crípticos) e encanta jovens com micro-interações responsivas e visual moderno.

2. **Mecanismo de Alocação & Rebalanceamento por Diagrama do Cerrado:**
   - Implementação da filosofia de *Buy & Hold* por novos aportes: o usuário define as porcentagens-alvo por classe (Ações BR, FIIs, Renda Fixa, Exterior, etc.) e notas de qualidade/risco para os ativos.
   - A cada mês, o investidor apenas informa o valor que deseja aportar. O algoritmo calcula a distância de cada ativo em relação à meta e aponta onde aplicar o dinheiro para equilibrar o risco, respeitando tetos de exposição máxima [ASSUMPTION: os critérios padrão de notas de risco e governança virão pré-configurados com sugestões de boas práticas, permitindo ajuste pelo usuário].

3. **MVP Mobile de Alto Desempenho (Flutter Multi-Surface):**
   - Entrega de compilação mobile dedicada e fluida contendo:
     - **Autenticação Biométrica / Login Rápido** (com persistência de sessão segura).
     - **Dashboard Patrimonial:** Patrimônio total, rentabilidade histórica e distribuição por classes.
     - **Central de Sugestões de Aporte:** O "GPS do Aporte" em destaque, dizendo onde alocar os recursos do mês.
   - [ASSUMPTION: Arquitetura em camadas e modularizada em `frontend/lib/src/features/` para suportar nativamente a inclusão do futuro módulo de **Controle de Gastos e Orçamento** sem refatorações estruturais].

4. **Nova Landing Page Focada em Conversão:**
   - Reformulação da página inicial com storytelling moderno, demonstrando a dor de quem tem dinheiro espalhado e a solução instantânea: "Do upload B3 ao GPS de Aporte em minutos".
   - Demonstração visual do aplicativo mobile, metodologia Graham + Diagrama do Cerrado e CTA direto para teste gratuito ou login.

---

## What Makes This Different

- **Rebalanceamento Sem Venda Forçada (*Tax-Efficient Buy & Hold*):** Ao contrário de robôs de investimento tradicionais que forçam vendas e rebalanceamentos fiscais caros, a combinação Graham + Cerrado educa e orienta o aporte novo, potencializando juros compostos.
- **União do "O que Comprar" com o "Onde Alocar":** O mercado se divide entre apps de carteira (que só mostram gráficos passivos) e casas de análise (que recomendam ações isoladas). O Graham Select junta o filtro de Graham (segurança de preço/valor) com a bússola do Cerrado (disciplina de risco da sua própria carteira).
- **Usabilidade Acessível Real:** Interfaces bancárias e de corretoras são ou simplistas demais ou densas como terminais Bloomberg. Nosso diferencial de UX é a simplicidade acolhedora com profundidade matemática sob o capô.

---

## Who This Serves

- **O "CFO" da Família (Investidor Maduro / 40+ anos):**
  - *Cenário:* Cuida do dinheiro da casa, preza por segurança, detesta perder tempo decifrando interfaces confusas com letras miúdas.
  - *Sucesso:* Abre o app no celular no dia do pagamento, visualiza o patrimônio familiar equilibrado e em 15 segundos sabe exatamente qual ativo comprar para balancear a carteira sem ansiedade.
- **O Investidor Jovem / Digital Native (20 a 35 anos):**
  - *Cenário:* Começando a acumular patrimônio, acostumado à experiência fluida do Nubank/Inter, busca método confiável para não cair em modismos de redes sociais.
  - *Sucesso:* Entende a regra do jogo através de um visual limpo, adota o hábito mensal de aportes e acompanha a evolução patrimonial com sensação de progresso real.

---

## Scope

### In Scope (Graham Select 2.0)
- **Design System & Reestilização:** Novo tema global no Flutter (paleta contemporânea inspirada no Nubank, tipografia aprimorada, contraste alto, componentes modulares).
- **MVP Mobile (iOS & Android):**
  - Fluxo de autenticação rápida/segura.
  - Dashboard consolidado com métricas-chave de patrimônio e alocação.
  - Tela/Seção de Sugestões de Aporte baseada no Diagrama do Cerrado.
  - Estrutura de navegação modular preparada para novas abas (ex: Controle de Gastos).
- **Módulo Diagrama do Cerrado (Backend & Frontend):**
  - Configuração de metas patrimoniais por classes e pesos/notas por ativos.
  - Motor de cálculo de aporte inteligente (distância para a meta + tetos de exposição).
  - Integração com as oportunidades detectadas pela fórmula de Graham.
- **Nova Landing Page:** Redesenho completo da `home_page.dart` e assets visuais com comunicação clara da proposta de valor, previews interativos e alta conversão.

### Out of Scope (Parked for Next Cycles)
- **Controle de Gastos e Orçamento Pessoal:** Não entra no MVP Mobile do dia 1; apenas a fundação arquitetural de dados/navegação será deixada pronta.
- **Execução Automática de Ordens (Home Broker Integration):** O app sugere e direciona; a execução da compra física permanece na corretora do usuário.
- **Open Finance Bancário Automático para Despesas:** Conexão direta com contas correntes para rastrear gastos bancários será tratada no futuro ciclo do módulo de gastos.

---

## Success Criteria

1. **Adoção Mobile:** Mais de 65% dos acessos semanais de checagem pré-aporte ocorrendo via dispositivo móvel nos primeiros 60 dias de lançamento do app.
2. **Eficiência de Decisão (Time-to-Action):** Usuário leva menos de 30 segundos entre abrir o app e identificar a recomendação de aporte ideal.
3. **Conversão da Nova Landing Page:** Aumento de pelo menos 25% na taxa de conversão de visitantes únicos em contas criadas / usuários ativos.
4. **Satisfação de Usabilidade (CSAT/NPS):** Avaliação de facilidade e clareza visual superior a 85% positiva tanto na faixa etária < 35 anos quanto > 50 anos.

---

## Vision

Transformar o Graham Select no ecossistema financeiro definitivo de construção de patrimônio familiar no Brasil. Nos próximos 2 anos, a plataforma unirá o **cuidado com a saída de capital** (controle de gastos e fluxo de caixa simplificado no celular) com o **direcionamento inteligente da entrada de capital** (Graham + Cerrado), tudo protegido por uma IA que atua como o consultor financeiro de bolso do investidor, blindando seu patrimônio contra o efeito manada e a inflação.
