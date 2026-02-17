# Cenários de Teste E2E - Tela de Upload

| ID | Cenário | Descrição | Passos | Resultado Esperado | Status |
|---|---|---|---|---|---|
| CT-01 | Renderização Inicial | Verificar elementos da tela de upload | 1. Acessar `/upload` | Título "Upload de Dados Financeiros", Card de seleção de arquivo visível | [x] |
| CT-02 | Navegação Home -> Upload | Verificar navegação via botão | 1. Acessar Home<br>2. Clicar "Fazer Upload de Dados" | Redirecionado para `/upload` | [x] |
| CT-03 | Upload Sucesso | Fluxo feliz de upload | 1. Selecionar arquivo `statusinvest-busca-avancada-33.csv`<br>2. Clicar "Fazer Upload"<br>3. Aguardar | Mensagem de sucesso e botão "Enviar Outro Arquivo" | [x] |
| CT-04 | Upload Erro Servidor | Fluxo de erro do backend | 1. Selecionar arquivo<br>2. Simular erro 500<br>3. Clicar Upload | Mensagem de erro e botão "Tentar Novamente" | [x] |
| CT-05 | Reset Após Sucesso | Reiniciar fluxo após sucesso | 1. Executar CT-03<br>2. Clicar "Enviar Outro Arquivo" | Tela volta ao estado inicial | [x] |
| CT-06 | Reset Após Erro | Reiniciar fluxo após erro | 1. Executar CT-04<br>2. Clicar "Tentar Novamente" | Tela volta ao estado inicial | [x] |