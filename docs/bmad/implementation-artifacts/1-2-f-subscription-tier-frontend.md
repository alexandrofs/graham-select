# História 1.2-F: Interface de Gerenciamento de Nível de Assinatura

Status: ready-for-dev

## História

Como usuário da plataforma,
Eu quero ver meu nível de assinatura atual (Trial/Gratuito/Premium) na minha página de perfil,
Para que eu saiba quais funcionalidades estão disponíveis para mim e quando meu período de teste (trial) expira.

## Critérios de Aceite

1. **Página de Perfil:** O aplicativo deve possuir uma tela/página acessível via menu de navegação que representa o "Perfil" ou "Assinatura" do usuário. [ ]
2. **Consumo de API:** O frontend (Flutter) deve consumir o endpoint `GET /api/v1/users/me` (desenvolvido na história 1.2 de backend) para buscar os dados de assinatura. [ ]
3. **Exibição do Tier Atual:** A interface deve destacar, de forma clara (ex: com um badge, selo ou card visual), o tier atual do usuário (`TRIAL`, `FREE` ou `PREMIUM`). [ ]
4. **Alerta do Prazo de Trial:** Se o usuário estiver no tier `TRIAL`, a interface deve calcular e exibir a contagem de dias restantes até a expiração com base no campo `trialEndsAt`. [ ]
5. **Comparativo (Diferenciais da Assinatura):** A interface deve exibir um card ou modal informando as permissões de cada tier, seguindo a paleta de cores Navy Blue, Emerald, e Amber Gold detalhada nos guias de UX do epics.md. [ ]
6. **Estados e Tratamento de Erro:** A tela deve ter tratamentos adequados de Skeleton (estado Loading), erro (Retry) e dados populados de forma fluída (transição < 300ms, conforme NFR2). [ ]

## Tarefas / Subtarefas

- [ ] Frontend: Camada de Dados (Data/Domain)
  - [ ] Criar modelo `UserProfile` e mapeadores de parsing de JSON.
  - [ ] Criar a chamada HTTP para `GET /api/v1/users/me` dentro do Repository/DataSource adequado.
- [ ] Frontend: State Management (Domain/Application)
  - [ ] Adicionar a lógica de busca do Perfil usando o gerenciador de estado (Provider/Riverpod/BLoC).
- [ ] Frontend: Apresentação (UI)
  - [ ] Criar a `ProfileScreen` (ou refinar se já existir apenas um esqueleto).
  - [ ] Construir o componente do badge de "Trial / Gratuito / Premium".
  - [ ] Implementar a lógica condicional na UI que contabiliza os dias faltantes baseada na data `trialEndsAt`.
  - [ ] Adicionar skeleton screens enquanto os dados são carregados.
- [ ] Frontend: Testes
  - [ ] Testes unitários para o modelo `UserProfile`.
  - [ ] Widget mockando a resposta para prever todos os 3 Tiers possíveis na UI.

## Notas de Avaliação Analítica (Mary)

- **Simultaniedade:** Os engenheiros de Frontend podem iniciar essa tarefa utilizando o formato MOCK de um JSON localmente, sem precisarem esperar o endpoint de Backend da história 1.2 ficar 100% pronto na branch `develop`.
- **UX Strategy:** O tempo restante de Trial é fundamental para incentivar a conversão. Capriche visualmente nesse countdown sem torná-lo intrusivo.
