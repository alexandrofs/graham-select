#!/bin/bash
# user-tier-api-test.sh - Validação do endpoint /users/me
# Story 1.2: Gerenciamento de Nível de Assinatura

# Configuração
API_URL=${1:-"http://localhost:8080/api/v1"}
echo "Iniciando testes de sistema para API de Usuário em: $API_URL"

# 1. Testar Acesso Sem Token (401 Unauthorized)
echo -n "Testando acesso sem autorização... "
status_code=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/users/me")

if [ "$status_code" -eq 401 ]; then
    echo "PASS ✅ (401 Unauthorized esperado)"
else
    echo "FAIL ❌ (Esperado 401, recebido $status_code)"
fi

# 2. Testar Com Token Inválido
echo -n "Testando token inválido... "
status_code=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer invalid_token" \
  "$API_URL/users/me")

if [ "$status_code" -eq 401 ]; then
    echo "PASS ✅ (401 Unauthorized esperado)"
else
    echo "FAIL ❌ (Esperado 401, recebido $status_code)"
fi

# 3. Nota sobre Teste de Sucesso
echo "--------------------------------------------------------"
echo "Nota: O teste de sucesso (200 OK) requer um Google ID Token válido."
echo "Para executar manualmente com um token, use:"
echo "curl -H \"Authorization: Bearer <TOKEN>\" $API_URL/users/me"
echo "--------------------------------------------------------"

echo "Testes de API finalizados."
