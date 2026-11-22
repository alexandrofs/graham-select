# Regulatory & Compliance Review (CVM & LGPD) — graham-select

## Overall verdict

A auditoria regulatória e de privacidade das especificações de UX do projeto **Kerno** (`DESIGN.md` e `EXPERIENCE.md`) atesta uma **maturidade de conformidade substancial e diferenciada no ecossistema de fintechs**, especialmente na blindagem de privacidade de Inteligência Artificial (**Sanitizador LGPD de IA com 0 PII para o Google Gemini**) e na postura matemática neutra do motor de cálculo (**Resoluções CVM nº 19 e nº 20**).

O par de documentos estabelece com clareza o enquadramento do aplicativo como uma **ferramenta tecnológica de triagem, simulação patrimonial e calculadora matemática parametrizada exclusivamente pelo próprio investidor**, evitando o exercício não autorizado de consultoria ou análise de valores mobiliários (CNPI/CVM). Adicionalmente, a regra de acessibilidade visual e auditiva no **Modo Privacidade** e a execução atômica do **Direito ao Esquecimento (`UserDataPurgeAdapter`)** demonstram estrita aderência aos princípios da LGPD (Lei 13.709/2018).

Contudo, para assegurar **blindagem jurídica integral perante a CVM, a ANPD e o Código de Defesa do Consumidor (CDC)**, foram identificados **2 apontamentos Críticos**, **3 de Alta severidade**, **4 de Média severidade** e **2 de Baixa severidade**. As correções necessárias concentram-se na obrigatoriedade do disclaimer legal na tela de **Ranking Graham (`/ranking`)**, na inclusão mandatória do **consentimento para Termos de Uso e Política de Privacidade no onboarding OAuth2 de 1 clique**, na eliminação do termo adjetivado *"Oportunidade"* em badges de ativos, e na especificação do **aviso de cotações com delay regulatório de 15 minutos da B3** durante o pregão.

---

## Findings by Severity

### Critical (2)

- **[critical]** **Ausência de Disclaimer Regulatório CVM na Tela de Ranking Graham (`/ranking`) e Risco de Interpretação como Carteira Recomendada** (`EXPERIENCE.md`, linhas 92 e 124–128; `DESIGN.md`, linhas 523–526 e 538–544).
  * *Risco:* A tela de `/ranking` ordena ações listadas na B3 com base na Margem de Segurança da Fórmula de Graham. A **Resolução CVM nº 20/2021** (art. 1º, § 1º e § 2º) estabelece que rankings e listas ordenadas de valores mobiliários sem aviso inequívoco de que se tratam de mero filtro quantitativo objetivo podem ser enquadrados como relatórios de análise ou carteiras recomendadas, expondo a plataforma a procedimentos sancionadores por exercício não registrado de análise de valores mobiliários. Embora o GPS de Aporte e o Kerno AI possuam o `{components.cvm-regulatory-footer}`, a rota `/ranking` e o widget `FinancialDataTable` não possuem previsão obrigatória de rodapé ou cabeçalho regulatório.
  * *Fix:* Tornar obrigatória a presença do `{components.cvm-regulatory-footer}` (ou banner fixo equivalente) no topo e na base de `/ranking`: *"Aviso Regulatório (CVM 20/2021): O Ranking Graham é uma ferramenta de triagem quantitativa estritamente baseada em dados públicos passados das demonstrações contábeis das empresas. Não constitui carteira recomendada, análise de valores mobiliários ou indicação de compra."*

- **[critical]** **Ausência de Mecanismo de Consentimento Explícito e Links de Termos de Uso e Política de Privacidade no Onboarding OAuth2 de 1 Clique** (`EXPERIENCE.md`, linhas 32–33, 79–85 e 316; `DESIGN.md`, linha 541).
  * *Risco:* A jornada UJ-5 (Camila) e a tabela de superfícies descrevem cadastro via *"CTA de 1 clique via Google Sign-In/OAuth2"*. A **LGPD (Lei 13.709/2018, art. 7º, I e art. 8º)** exige que o tratamento de dados pessoais, cadastrais e financeiros seja respaldado por consentimento inequívoco e transparente. Permitir a criação de conta sem exibir previamente o texto de concordância com links funcionais para Termos de Uso e Política de Privacidade torna a base de tratamento suscetível a autuações da ANPD por falta de transparência e vício de consentimento. Além disso, as rotas `/terms` e `/privacy` não constam na tabela de rotas do `GoRouter`.
  * *Fix:* Adicionar formalmente as rotas públicas `/terms` (Termos de Uso) e `/privacy` (Política de Privacidade) ao `GoRouter` e ao rodapé da Landing Page. No componente de botão do Google Sign-In da Landing Page e da `LoginScreen`, inserir texto legal obrigatório: *"Ao continuar, você concorda com os [Termos de Uso](/terms) e confirma ciência da [Política de Privacidade](/privacy)"*, registrando o aceite e a versão dos documentos no banco de dados com carimbo de data/hora (timestamp).

---

### High (3)

- **[high]** **Uso do Substantivo "Oportunidade" no Selo do Multiplicador de Graham (`{components.badge-graham-bonus}`)** (`DESIGN.md`, linhas 401, 478, 516; `EXPERIENCE.md`, linhas 118, 189).
  * *Risco:* O selo semântico renderiza o texto *"Oportunidade Graham + Cerrado"*. O termo "Oportunidade" possui carga semântica indutiva e promocional no mercado de capitais brasileiro, sugerindo juízo de valor e aproximação a um "call" de compra. Perante a CVM, ferramentas quantitativas parametrizadas pelo usuário devem manter neutralidade estrita e vocabulário técnico puramente descritivo.
  * *Fix:* Substituir o rótulo textual do badge por nomenclatura puramente descritiva e matemática, tais como: *"Margem Graham Positiva"*, *"Fator Graham Elegível (1.30x)"* ou *"Desconto Contábil Graham"*.

- **[high]** **Ausência de Aviso de Defasagem de Cotações Intradiárias (Delay de 15 Minutos da B3) Durante o Horário de Negociação** (`DESIGN.md`, linhas 390, 469, 486; `EXPERIENCE.md`, linhas 135, 162, 256).
  * *Risco:* As especificações cobrem com rigor o estado de fechamento de pregão (`"Cotações de fechamento (D-1)"`), mas omitem a política de cotações durante o horário de pregão aberto (10:00 às 17:00/18:00). A redistribuição de market data da B3 para plataformas não-corretoras de varejo tipicamente opera com atraso regulatório de no mínimo 15 minutos (delayed data). Não explicitar a defasagem temporal de 15 minutos durante o pregão pode induzir o investidor a erro no momento de emitir ordens na corretora, violando normas de transparência de dados de mercado da B3 e do Código de Defesa do Consumidor (CDC art. 6º, III).
  * *Fix:* Incluir na especificação de `wealth-hero-card`, `contribution-gps-card` e `FinancialDataTable` o carimbo de data/hora acompanhado do status intradiário: *"Cotações B3 com atraso de 15 min (Última atualização às HH:mm:ss)"* durante o pregão, alternando para *"Cotações de fechamento (D-1)"* após as 18:00.

- **[high]** **Exposição da Palavra "Recommendations" em Rota e Alias Técnico (`/graham-recommendations`)** (`EXPERIENCE.md`, linhas 70, 89; `DESIGN.md`, linhas 445, 543).
  * *Risco:* A tabela de migração brownfield mantém `/graham-recommendations` como rota ativa para o GPS de Aporte. Em auditorias regulatórias, inspeções de telemetria ou análise de requisições HTTP pelo usuário final, a presença explícita do termo em inglês "recommendations" colide frontalmente com a tese de defesa perante a CVM de que a plataforma jamais emite recomendações individualizadas.
  * *Fix:* Renomear a rota canônica no `GoRouter` para `/gps-aporte` ou `/alocacao-mensal`. Manter `/graham-recommendations` estritamente como redirecionamento de nível de rede/roteador interno sem menção em links visíveis, atributos ARIA, breadcrumbs ou tags de telemetria.

---

### Medium (4)

- **[medium]** **Omissão de Regra Visual e Aritmética para Lucro Líquido por Ação (LPA) ou Valor Patrimonial da Ação (VPA) Negativos** (`DESIGN.md`, linhas 486–487; `EXPERIENCE.md`, linhas 139, 253).
  * *Risco:* A fórmula clássica de Graham é dada por $VI = \sqrt{22.5 \times LPA \times VPA}$. Caso uma empresa reporte prejuízo contábil ($LPA < 0$) ou patrimônio líquido a descoberto ($VPA < 0$), o radicando torna-se negativo e a raiz quadrada é indefinida no conjunto dos números reais. A especificação não orienta o comportamento da UI no `{components.math-transparency-accordion}` ou no ranking para estes casos, gerando risco de cálculo anômalo, erro de interface ou quebra de transparência com o investidor.
  * *Fix:* Adicionar na especificação do `{components.math-transparency-accordion}` e no ranking: quando $LPA \le 0$ ou $VPA \le 0$, exibir badge neutro informativo: *"Fórmula de Graham Inaplicável (Empresa com Prejuízo ou PL Negativo)"*, mantendo o ativo elegível para cálculo do aporte exclusivamente pelos pesos do Diagrama do Cerrado, com multiplicador Graham fixado em `1.00x` (sem bônus).

- **[medium]** **Promessa de Expurgo Total sem Menção à Retenção Legal Obrigatória de Registros de Acesso (Marco Civil da Internet)** (`EXPERIENCE.md`, linhas 231–235).
  * *Risco:* A descrição do fluxo de Direito ao Esquecimento informa que "tudo será permanentemente removido pelo `UserDataPurgeAdapter`". Entretanto, o Marco Civil da Internet (Lei 12.965/2014, art. 15) obriga a guarda dos registros de acesso a aplicações de internet (IP, data e hora) sob sigilo pelo período mínimo de 6 meses. Prometer a eliminação absoluta de 100% dos dados técnicos sem a ressalva da retenção legal cria vulnerabilidade de conformidade entre a LGPD e o Marco Civil.
  * *Fix:* Detalhar na microcópia do modal de exclusão e na Política de Privacidade: *"Seus dados cadastrais, patrimoniais, carteiras e notas serão excluídos permanentemente. Conforme exigido pelo art. 15 da Lei Federal nº 12.965/2014 (Marco Civil da Internet), os registros de conexão e acesso à aplicação serão armazenados de forma criptografada sob sigilo estrito pelo prazo legal de 6 meses, sendo expurgados automaticamente após esse período."*

- **[medium]** **Falta de Declaração de Guarda e Consentimento Específico para Dados de Menores e Dependentes no Multi-CPF Familiar (LGPD Art. 14)** (`EXPERIENCE.md`, linhas 36, 97, 243–250; `DESIGN.md`, linha 521).
  * *Risco:* O módulo de Holding Familiar Multi-CPF permite gerenciar carteiras de dependentes (como filhos menores de idade). O artigo 14 da LGPD estipula que o tratamento de dados pessoais de crianças e adolescentes deve ser realizado com o consentimento específico e em destaque concedido por pelo menos um dos pais ou responsável legal. A especificação atual não prevê confirmação de representação legal ao vincular um novo CPF.
  * *Fix:* No formulário de cadastro de novo CPF na página `FamilyProfilesPage` ou no sheet `{components.b3-ingestion-crud-sheet}`, incluir caixa de seleção obrigatória quando o dependente for menor: *"Declaro que sou pai, mãe ou representante legal deste titular e autorizo a gestão consolidada dos dados patrimoniais sob as disposições da LGPD."*

- **[medium]** **Ausência de Opção de Portabilidade de Dados (Exportação Estruturada) Precedendo o Expurgo Definitivo (LGPD Art. 18, V)** (`EXPERIENCE.md`, linhas 231–235).
  * *Risco:* O artigo 18, inciso V da LGPD assegura ao titular o direito à portabilidade de seus dados a outro fornecedor em formato interoperável. Apresentar apenas a opção de exclusão irreversível ("hard-delete") sem permitir que o usuário baixe seus dados históricos consolidados (ativos, notas, aportes) em formato aberto (CSV/JSON) contraria os princípios de transparência e autodeterminação informativa.
  * *Fix:* Disponibilizar no painel de Conta & LGPD o botão *"Exportar Meus Dados (JSON/CSV)"*, permitindo ao usuário efetuar o download completo de sua base antes de prosseguir com a exclusão de conta.

---

### Low (2)

- **[low]** **Expansão do Guardrail Léxico CVM no Pós-Processamento do Kerno AI** (`EXPERIENCE.md`, linhas 110, 229).
  * *Risco:* O filtro regex pós-geração atual (`compr[aeo]|vend[aeo]|adquir[ai]|alien[ae]|garantid[oa]|recomenda`) bloqueia com eficácia verbos diretos de compra e venda. Porém, formulações persuasivas indiretas como *"ativo imperdível"*, *"sobreponderar posição"*, *"subponderar setor"* ou *"sugiro alocar"* podem transpassar a validação e soar como aconselhamento financeiro regulado.
  * *Fix:* Enriquecer a lista de radicais banidos no backend para conter: `sobreponder|subponder|imperdivel|lucro certo|rentabilidade garantida|sugestao de compra|melhor papel`.

- **[low]** **Transparência Auditável do Contador de Vagas do Programa Founders (Código de Defesa do Consumidor Art. 37)** (`EXPERIENCE.md`, linhas 32, 315, 320; `DESIGN.md`, linha 404).
  * *Risco:* Estratégias de escassez limitada ("primeiras 250 vagas vitalícias") estão sujeitas a escrutínio pelo CDC contra práticas abusivas ou publicidade enganosa. Caso o contador seja estático ou não corresponda à realidade dos bancos de dados, pode gerar reclamações perante órgãos de defesa do consumidor (Procon).
  * *Fix:* Garantir que o valor exibido na Landing Page seja derivado em tempo real de consulta auditável ao backend (`/api/v1/founders/slots`), transitando automaticamente e de forma transparente para o Trial Pro de 90 dias assim que o lote for integralmente atingido.

---

## Compliance Matrix

| Domínio de Conformidade | Norma / Regulamento | Status de Conformidade | Avaliação & Recomendações de Ação |
|---|---|---|---|
| **Segregação de Análise e Consultoria** | Resoluções CVM nº 19/2021 e nº 20/2021 | **Parcial** | O GPS de Aporte e o Kerno AI contam com disclaimers claros e vocabulário sem imperativos. **Ajuste Mandatório:** Tornar obrigatório o disclaimer `{components.cvm-regulatory-footer}` no Ranking Graham (`/ranking`) e na Landing Page pública; renomear a rota `/graham-recommendations` para `/gps-aporte`. |
| **Neutralidade da Metodologia Graham** | CVM 20/2021 & Princípios de Engenharia Financeira | **Parcial** | A memória de cálculo no `{components.math-transparency-accordion}` é determinística e transparente. **Ajuste Mandatório:** Substituir o rótulo *"Oportunidade Graham"* por *"Margem Graham Positiva"*; adicionar tratamento explícito para empresas com $LPA \le 0$ ou $VPA \le 0$ (fórmula inaplicável). |
| **Transparência de Cotações de Mercado** | B3 Market Data Policy & CDC Art. 6º, III | **Parcial** | O status de fechamento (`D-1`) está bem documentado e implementado visualmente. **Ajuste Mandatório:** Especificar o aviso de defasagem intradiária de 15 minutos da B3 (*"Cotações com atraso de 15 min"*) durante o horário de pregão aberto. |
| **Consentimento e Base Legal LGPD** | LGPD (Lei 13.709/2018, Arts. 7º e 8º) | **Não Conforme (Parcial)** | O isolamento no fluxo de IA é exemplar. **Ajuste Mandatório:** Implementar consentimento prévio/concomitante aos Termos de Uso e Política de Privacidade no login de 1 clique Google OAuth2, e criar as rotas públicas `/terms` e `/privacy`. |
| **Proteção de Dados em Inteligência Artificial** | LGPD (Princípio da Finalidade e Minimização) | **Conforme (Compliant)** | O **Sanitizador LGPD de IA** (0 PII, anonimização de identificadores manuais para `RENDA_FIXA_MANUAL_1` e *Local UI Binding*) atende aos mais elevados padrões internacionais de privacidade em arquiteturas de IA generativa. |
| **Direito ao Esquecimento e Retenção Legal** | LGPD Art. 18, VI & Marco Civil da Internet Art. 15 | **Parcial** | O expurgo atômico (`UserDataPurgeAdapter`) é tecnicamente impecável. **Ajuste Mandatório:** Esclarecer na microcópia do modal a retenção obrigatória de registros de conexão por 6 meses conforme a Lei 12.965/2014; disponibilizar botão de exportação prévia (portabilidade). |
| **Proteção de Dados de Menores e Dependentes** | LGPD Art. 14 (Crianças e Adolescentes) | **Parcial** | O seletor Multi-CPF atende ergonomicamente à família. **Ajuste Mandatório:** Exigir declaração expressa de representação/guarda legal ao cadastrar CPFs de dependentes menores de idade. |
| **Acessibilidade e Não-Exposição no Transporte** | WCAG 2.1 AA/AAA & CDC Art. 6º | **Conforme (Compliant)** | Excelente blindagem: contraste AAA nos números patrimoniais, `FLAG_SECURE` contra captura de tela, leitura audível do sintetizador bloqueada no Modo Privacidade e áreas de toque com piso mínimo de 48x48dp. |
