# História 1.3: Restrição de Funcionalidades por Tier

Status: ready-for-dev

## História

**Como** sistema,
**Eu quero** controlar o acesso a funcionalidades Premium baseado no tier (nível de assinatura) do usuário,
**Para que** apenas assinantes Premium (ou usuários no período de Trial) acessem recursos avançados, como o motor de recomendações de IA (Filtro de Graham) e a definição de metas de alocação.

## Contexto

O sistema possui três níveis de assinatura: `TRIAL`, `FREE` (Gratuito) e `PREMIUM`. 
- `TRIAL`: Usuários recém-cadastrados começam aqui com validade de 30 dias. Possuem acesso a todas as funcionalidades Premium.
- `FREE`: Após o fim do Trial, o usuário é rebaixado caso não assine o plano pago. Perde acesso a features avançadas.
- `PREMIUM`: Usuários pagantes com acesso irrestrito.

Esta história foca na proteção de endpoints específicos na camada de API (Backend) e na adequação de possíveis respostas padronizadas para que o Frontend possa exibir a mensagem "Upgrade Required".

## Critérios de Aceite

1. **Acesso Negado (Tier Gratuito e Trial Expirado):**
   - **Dado** que o usuário possui o nível "Gratuito" (FREE) ou um "Trial" expirado,
   - **Quando** ele tenta acessar um endpoint protegido (ex: Motor de Recomendação - FR21, ou Metas de Alocação - FR20),
   - **Então** o sistema deve bloquear a requisição e retornar um erro HTTP adequado (ex: 403 Forbidden ou 402 Payment Required).
   - **E** a resposta deve seguir o formato RFC 7807 (Problem Detail) com uma mensagem clara de que a funcionalidade exige upgrade.

2. **Acesso Permitido (Trial Válido):**
   - **Dado** que o usuário possui o nível "Trial" dentro do prazo de validade (`trialEndsAt` > agora),
   - **Quando** ele acessa as funcionalidades Premium,
   - **Então** o sistema permite a execução normalmente.

3. **Acesso Permitido (Premium):**
   - **Dado** que o usuário possui o nível "Premium",
   - **Quando** ele acessa qualquer funcionalidade (incluindo as Premium),
   - **Então** o sistema permite a execução sem restrições.

4. **Expiração Automática de Trial no Acesso:**
   - **Dado** que o usuário está no nível "Trial" mas seu `trialEndsAt` já passou da data/hora atual (UTC),
   - **Quando** ele tentar usar o sistema,
   - **Então** a autorização para a feature Premium deve ser negada, e o sistema deve promover a mudança do status dele para `FREE` de forma preguiçosa (lazy update) ou reavaliar on-the-fly.

## Tarefas / Subtarefas

- [ ] **Backend: Infraestrutura de Segurança Autorizativa**
  - [ ] Criar um mecanismo de proteção para rotas Premium (ex: uma anotação customizada `@RequirePremium` ou configuração de segurança via expressões do Spring Security).
  - [ ] Implementar a lógica de checagem do Tier do usuário atual logado (extraindo dados do contexto e valendo-se das regras definidas de Trial expirado).
- [ ] **Backend: Handler de Exceções**
  - [ ] Ajustar o `ControllerAdvice` global para tratar as exceções de acesso negado relacionadas ao tier.
  - [ ] Retornar o formato padronizado (RFC 7807 Problem Detail) com um tipo/código customizado (e.g. `urn:problem-type:upgrade-required`).
- [ ] **Backend: Verificação (Testes)**
  - [ ] Implementar testes de integração (`FeatureRestrictionIT`) simulando usuários nos três níveis: FREE, TRIAL válido e PREMIUM.
  - [ ] Simular um usuário com TRIAL expirado tentando acessar a rota restrita e validar que ocorre a negação.
  - [ ] Validar o formato da resposta no padrão RFC 7807 para o caso de acesso negado.

## Notas de Desenvolvimento

- **Segurança (Spring Security):** Use as abstrações do Spring Boot (ex: `MethodSecurity` com `@PreAuthorize`) para injetar a checagem diretamente nos métodos do Controler ou Use Case.
- Como as regras podem envolver o rebaixamento ("downgrade") lazy do usuário de TRIAL para FREE, defina a melhor abordagem: ou um cron job diário (mais complexo na infra), ou a checagem no filtro de segurança interceptando requisições e disparando o update no banco caso detecte a expiração.
- **Integração Frontend (UX):** As requisições barradas com o erro correto (e.g. 403 / Upgrade Required) servirão de gatilho para o Flutter (`Frontend`) exibir telas ou modais de assinatura. Portanto, o códido de erro da resposta deve estar estritamente documentado para os desenvolvedores mobile construírem os cenários da História 1.3 de UI.
