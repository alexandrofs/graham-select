#!/bin/bash

# Cores para o output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Graham Select - Inicializador Local ===${NC}"

# 0. Carregar variáveis de ambiente do .env.local
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env.local"

if [ -f "$ENV_FILE" ]; then
  echo -e "${GREEN}[0/4] Carregando variáveis de .env.local...${NC}"
  # Exporta cada linha não-comentada e não-vazia
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  echo -e "${YELLOW}  API_BASE_URL=${API_BASE_URL:-'(não definida, usando default)'}${NC}"
else
  echo -e "${YELLOW}[AVISO] Arquivo .env.local não encontrado. Usando valores padrão.${NC}"
  echo -e "${YELLOW}  Crie o arquivo baseado em .env.local.example${NC}"
fi

# 1. Subir Infraestrutura Docker
echo -e "${GREEN}[1/4] Iniciando infraestrutura (MySQL & Kafka)...${NC}"
docker-compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}Erro ao subir os containers Docker. Verifique se o Docker Desktop está rodando.${NC}"
    exit 1
fi

# 2. Instalar Módulo Common
echo -e "${GREEN}[2/4] Instalando módulo common...${NC}"
cd backend && mvn clean install -pl common -am -DskipTests
cd ..

# 3. Iniciar Serviços Backend e Frontend em Paralelo
echo -e "${GREEN}[3/4] Iniciando Serviços Backend e Frontend...${NC}"

# Função para matar processos nas portas do backend
kill_backend_ports() {
    local pids
    pids=$(lsof -ti tcp:8080; lsof -ti tcp:8081) 2>/dev/null
    if [ -n "$pids" ]; then
        echo -e "${YELLOW}  Liberando portas 8080/8081 (PIDs: $(echo $pids | tr '\n' ' '))...${NC}"
        echo "$pids" | xargs kill -9 2>/dev/null
        sleep 1
    fi
}

# Função para finalizar processos ao sair
cleanup() {
    echo -e "\n${RED}Finalizando serviços...${NC}"
    kill $API_PID $VALUATION_PID $FRONT_PID 2>/dev/null
    docker-compose down
    kill_backend_ports
    exit
}

trap cleanup SIGINT

# Liberar portas antes de subir (evita conflito com processos órfãos)
kill_backend_ports

echo -e "${BLUE}Iniciando API Service na porta 8080...${NC}"
cd backend && mvn spring-boot:run -pl api -Dspring-boot.run.profiles=local > ../api.log 2>&1 &
API_PID=$!

echo -e "${BLUE}Iniciando Valuation Service na porta 8081...${NC}"
cd backend && mvn spring-boot:run -pl valuation-service -Dspring-boot.run.profiles=local > ../valuation.log 2>&1 &
VALUATION_PID=$!

echo -e "${BLUE}Iniciando Frontend (Flutter Web)...${NC}"
cd frontend && flutter run -d chrome --web-port 3000 \
  --dart-define=API_BASE_URL="${API_BASE_URL:-'http://localhost:8080/api/v1'}" \
  --dart-define=GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-''}" \
  --dart-define=FIREBASE_API_KEY="${FIREBASE_API_KEY:-'dummy-api-key'}" \
  --dart-define=FIREBASE_AUTH_DOMAIN="${FIREBASE_AUTH_DOMAIN:-'dummy-auth-domain'}" \
  --dart-define=FIREBASE_PROJECT_ID="${FIREBASE_PROJECT_ID:-'dummy-project-id'}" \
  --dart-define=FIREBASE_STORAGE_BUCKET="${FIREBASE_STORAGE_BUCKET:-'dummy-storage-bucket'}" \
  --dart-define=FIREBASE_MESSAGING_SENDER_ID="${FIREBASE_MESSAGING_SENDER_ID:-'dummy-sender-id'}" \
  --dart-define=FIREBASE_APP_ID="${FIREBASE_APP_ID:-'dummy-app-id'}" \
  --dart-define=FIREBASE_MEASUREMENT_ID="${FIREBASE_MEASUREMENT_ID:-'dummy-measurement-id'}" > ../frontend.log 2>&1 &
FRONT_PID=$!


# 4. Monitoramento
echo -e "${GREEN}[4/4] Ambiente subindo!${NC}"
echo -e "Logs disponíveis em: ${BLUE}api.log${NC}, ${BLUE}valuation.log${NC} e ${BLUE}frontend.log${NC}"
echo -e "Pressione ${RED}CTRL+C${NC} para parar todos os serviços."

# Aguarda os processos
wait
