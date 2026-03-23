# Cenários de Teste E2E - Gerenciamento de Assinatura (Story 1.2-F)

Estes cenários devem ser automatizados (ex: usando Flutter `integration_test` ou Playwright) assim que a interface do usuário for implementada.

| ID | Cenário | Descrição | Passos | Resultado Esperado |
|---|---|---|---|---|
| CT-PRO-01 | Exibição de Tier TRIAL | Validar que usuário em trial vê o badge correto e dias restantes. | 1. Autenticar como usuário Trial.<br>2. Navegar para a tela de Perfil. | Badge "TRIAL" visível. Texto informando os dias restantes (ex: "29 dias restantes"). |
| CT-PRO-02 | Exibição de Tier FREE | Validar que usuário com trial expirado vê o badge FREE. | 1. Autenticar com usuário cujo `trialEndsAt` é passado.<br>2. Abrir Perfil. | Badge "FREE" ou "GRATUITO" visível. |
| CT-PRO-03 | Navegação para Perfil | Validar acesso à tela via menu. | 1. Estar na Home.<br>2. Clicar no ícone de perfil/avatar. | Redirecionado para `/profile` ou similar. |
| CT-PRO-04 | Atualização de Tier | Validar que a UI reflete mudanças após upgrade (Simulado). | 1. Estar no perfil (TRIAL).<br>2. Simular mudança para PREMIUM no backend.<br>3. Atualizar tela. | Badge muda para "PREMIUM". |
| CT-PRO-05 | Erro de Carregamento | Validar tratamento de erro na API. | 1. Abrir Perfil.<br>2. Simular erro 500 no endpoint `/users/me`. | Exibição de mensagem de erro e botão de "Tentar Novamente". |

## Notas Técnica
- **Locators Sugeridos**:
  - `Key('subscription_tier_badge')`
  - `Key('trial_countdown_text')`
  - `Key('profile_error_retry_button')`
